/* MidiFile.cpp
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "MidiFile"
#include "utils/Log.h"

#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <sched.h>
#include <utils/threads.h>
#include <libsonivox/eas_reverb.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#ifndef ANDROID_DEFAULT_CODE
#include <sys/resource.h>
#include <sys/time.h>
#include <cutils/xlog.h>
#endif
#ifdef HAVE_GETTID
static pid_t myTid() { return gettid(); }
#else
static pid_t myTid() { return getpid(); }
#endif
#include <system/audio.h>

#include "MidiFile.h"

// ----------------------------------------------------------------------------

namespace android {

// ----------------------------------------------------------------------------

// The midi engine buffers are a bit small (128 frames), so we batch them up
static const int NUM_BUFFERS = 4;

// TODO: Determine appropriate return codes
static status_t ERROR_NOT_OPEN = -1;
static status_t ERROR_OPEN_FAILED = -2;
static status_t ERROR_EAS_FAILURE = -3;
static status_t ERROR_ALLOCATE_FAILED = -4;

static const S_EAS_LIB_CONFIG* pLibConfig = NULL;

MidiFile::MidiFile() :
    mEasData(NULL), mEasHandle(NULL), mAudioBuffer(NULL),
    mPlayTime(-1), mDuration(-1), mState(EAS_STATE_ERROR),
    mStreamType(AUDIO_STREAM_MUSIC), mLoop(false), mExit(false),
    mPaused(false), mRender(false), mTid(-1)
{
    ALOGV("constructor");
#ifndef ANDROID_DEFAULT_CODE
    mMaxPlayTime = -1;
    mSeekNewBufCount = 0;
    mIsCurrentComplete = false; // OMA DRM v1 implementation
#ifdef MidiFile_Duration_thread
    mForceStopDuration = false;
//    mEasDataDuration = NULL;
    mIsReset = false;
    mGetDurationProcessing = false;
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

    mFileLocator.path = NULL;
    mFileLocator.fd = -1;
    mFileLocator.offset = 0;
    mFileLocator.length = 0;

    // get the library configuration and do sanity check
    if (pLibConfig == NULL)
        pLibConfig = EAS_Config();
    if ((pLibConfig == NULL) || (LIB_VERSION != pLibConfig->libVersion)) {
        ALOGE("EAS library/header mismatch");
        goto Failed;
    }

    // initialize EAS library
    if (EAS_Init(&mEasData) != EAS_SUCCESS) {
        ALOGE("EAS_Init failed");
        goto Failed;
    }

    // select reverb preset and enable
    EAS_SetParameter(mEasData, EAS_MODULE_REVERB, EAS_PARAM_REVERB_PRESET, EAS_PARAM_REVERB_CHAMBER);
    EAS_SetParameter(mEasData, EAS_MODULE_REVERB, EAS_PARAM_REVERB_BYPASS, EAS_FALSE);
#ifndef ANDROID_DEFAULT_CODE
    SXLOGD("MidiFile::constructor tid(%d), ptr(0x%x)", myTid(), this);
#endif
    // create playback thread
    {
#ifndef ANDROID_DEFAULT_CODE
        SXLOGV("render thread started");
#endif
        Mutex::Autolock l(mMutex);
        mThread = new MidiFileThread(this);
        mThread->run("midithread", ANDROID_PRIORITY_AUDIO);
        mCondition.wait(mMutex);
        ALOGV("thread started");
    }

    // indicate success
    if (mTid > 0) {
        ALOGV(" render thread(%d) started", mTid);
        mState = EAS_STATE_READY;
    }

Failed:
    return;
}

status_t MidiFile::initCheck()
{
    if (mState == EAS_STATE_ERROR) return ERROR_EAS_FAILURE;
    return NO_ERROR;
}

MidiFile::~MidiFile() {
    ALOGV("MidiFile destructor");
    release();
}

status_t MidiFile::setDataSource(
        const char* path, const KeyedVector<String8, String8> *) {
    ALOGV("MidiFile::setDataSource url=%s", path);
    Mutex::Autolock lock(mMutex);

    // file still open?
    if (mEasHandle) {
        reset_nosync();
    }

    // open file and set paused state
    mFileLocator.path = strdup(path);
    mFileLocator.fd = -1;
    mFileLocator.offset = 0;
    mFileLocator.length = 0;
    EAS_RESULT result = EAS_OpenFile(mEasData, &mFileLocator, &mEasHandle);
    if (result == EAS_SUCCESS) {
        updateState();
    }

    if (result != EAS_SUCCESS) {
        ALOGE("EAS_OpenFile failed: [%d]", (int)result);
        mState = EAS_STATE_ERROR;
        return ERROR_OPEN_FAILED;
    }

    mState = EAS_STATE_OPEN;
    mPlayTime = 0;
    return NO_ERROR;
}

status_t MidiFile::setDataSource(int fd, int64_t offset, int64_t length)
{
    ALOGV("MidiFile::setDataSource fd=%d", fd);
    Mutex::Autolock lock(mMutex);

    // file still open?
    if (mEasHandle) {
        reset_nosync();
    }

    // open file and set paused state
    mFileLocator.fd = dup(fd);
    mFileLocator.offset = offset;
    mFileLocator.length = length;
    EAS_RESULT result = EAS_OpenFile(mEasData, &mFileLocator, &mEasHandle);
#ifndef ANDROID_DEFAULT_CODE  
    if (result == EAS_SUCCESS) {
        updateState();
    }
#else
    updateState();
#endif

    if (result != EAS_SUCCESS) {
        ALOGE("EAS_OpenFile failed: [%d]", (int)result);
        mState = EAS_STATE_ERROR;
        return ERROR_OPEN_FAILED;
    }

    mState = EAS_STATE_OPEN;
    mPlayTime = 0;
    return NO_ERROR;
}

status_t MidiFile::prepare()
{
    ALOGV("MidiFile::prepare");
    Mutex::Autolock lock(mMutex);
    if (!mEasHandle) {
        return ERROR_NOT_OPEN;
    }
    EAS_RESULT result;
    if ((result = EAS_Prepare(mEasData, mEasHandle)) != EAS_SUCCESS) {
        ALOGE("EAS_Prepare failed: [%ld]", result);
        return ERROR_EAS_FAILURE;
    }
    updateState();

#ifndef ANDROID_DEFAULT_CODE 
#ifdef MTK_DRM_APP
    mIsCurrentComplete = false;
    EAS_ConsumeRights(mEasData);
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

    return NO_ERROR;
}

status_t MidiFile::prepareAsync()
{
    ALOGV("MidiFile::prepareAsync");
    status_t ret = prepare();
#ifndef ANDROID_DEFAULT_CODE    
#ifdef MidiFile_Duration_thread
    {
        mIsReset = false;
        mGetDurationProcessing = false;
        SXLOGV("+get duration thread started"); 
        Mutex::Autolock p(mgetDurationMutex);                
        createThreadEtc(getDurationThread, this, "midigetduration", ANDROID_PRIORITY_AUDIO);
        ALOGD("MidiFile::MidiFile  +mgetDurationCondition.wait"); 
        mgetDurationCondition.wait(mgetDurationMutex);
        ALOGD("MidiFile::MidiFile  -mgetDurationCondition.wait");             
     }
    // don't hold lock during callback
    if (ret != NO_ERROR) {
        sendEvent(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, ret);
    }
#endif
#else
    if (ret == NO_ERROR) {
        sendEvent(MEDIA_PREPARED);
    } else {
        sendEvent(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, ret);
    }
#endif
    return ret;
}

status_t MidiFile::start()
{
    ALOGV("MidiFile::start");
    Mutex::Autolock lock(mMutex);
    if (!mEasHandle) {
        return ERROR_NOT_OPEN;
    }
#ifndef ANDROID_DEFAULT_CODE
    EAS_RESULT result;
    if((mPlayTime == mDuration ||mPlayTime == mMaxPlayTime) && mState == EAS_STATE_STOPPED) 
    {        
        //Tina add for PhoneRintone Looping
        SXLOGV("MidiFile::start - EAS_Locate 0 in EAS_STATE_STOPPED state");
        if ((result = EAS_Locate(mEasData, mEasHandle, 0, false)) != EAS_SUCCESS)
        {
            SXLOGE("MidiFile::render EAS_Locate returned %ld", result);
        }
        EAS_GetLocation(mEasData, mEasHandle, &mPlayTime);   
	      mMaxPlayTime = -1;
    }
#endif
    // resuming after pause?
    if (mPaused) {
        if (EAS_Resume(mEasData, mEasHandle) != EAS_SUCCESS) {
            return ERROR_EAS_FAILURE;
        }
        mPaused = false;
        updateState();
    }

    mRender = true;

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
    EAS_GetLocation(mEasData, mEasHandle, &mPlayTime);
    if (mIsCurrentComplete && mPlayTime == 0) { // single recursive mode
        ALOGD("start, consumeRights");
        EAS_ConsumeRights(mEasData);
        mIsCurrentComplete = false;
    }
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

    // wake up render thread
    ALOGV("  wakeup render thread");
    mCondition.signal();
    return NO_ERROR;
}

status_t MidiFile::stop()
{
    ALOGV("MidiFile::stop");
    Mutex::Autolock lock(mMutex);
    if (!mEasHandle) {
        return ERROR_NOT_OPEN;
    }
    if (!mPaused && (mState != EAS_STATE_STOPPED)) {
        EAS_RESULT result = EAS_Pause(mEasData, mEasHandle);
        if (result != EAS_SUCCESS) {
            ALOGE("EAS_Pause returned error %ld", result);
            return ERROR_EAS_FAILURE;
        }
    }
    mPaused = false;
    return NO_ERROR;
}

status_t MidiFile::seekTo(int position)
{
    ALOGV("MidiFile::seekTo %d", position);
    // hold lock during EAS calls
    {
        Mutex::Autolock lock(mMutex);
        if (!mEasHandle) {
            return ERROR_NOT_OPEN;
        }
#ifndef ANDROID_DEFAULT_CODE
        //Tina add: Do Audio Track Ramp Down
        if(mAudioSink.get()!=NULL)
           mAudioSink->setVolume(0.0, 0.0);
#endif
        EAS_RESULT result;
        if ((result = EAS_Locate(mEasData, mEasHandle, position, false))
                != EAS_SUCCESS)
        {
            ALOGE("EAS_Locate returned %ld", result);
            return ERROR_EAS_FAILURE;
        }
        EAS_GetLocation(mEasData, mEasHandle, &mPlayTime);
#ifndef ANDROID_DEFAULT_CODE
        mSeekNewBufCount = 20;
#endif
    }
    sendEvent(MEDIA_SEEK_COMPLETE);
    return NO_ERROR;
}

status_t MidiFile::pause()
{
    ALOGV("MidiFile::pause");
    Mutex::Autolock lock(mMutex);
    if (!mEasHandle) {
        return ERROR_NOT_OPEN;
    }
    if ((mState == EAS_STATE_PAUSING) || (mState == EAS_STATE_PAUSED)) return NO_ERROR;
#ifndef ANDROID_DEFAULT_CODE
    if ((mState == EAS_STATE_PRESTOPPING) || (mState == EAS_STATE_STOPPING)|| (mState == EAS_STATE_STOPPED)) return NO_ERROR;
#endif
    if (EAS_Pause(mEasData, mEasHandle) != EAS_SUCCESS) {
        return ERROR_EAS_FAILURE;
    }
    mPaused = true;
    return NO_ERROR;
}

bool MidiFile::isPlaying()
{
    ALOGV("MidiFile::isPlaying, mState=%d", int(mState));
    if (!mEasHandle || mPaused) return false;
#ifndef ANDROID_DEFAULT_CODE

      usleep(10000);
#endif
    return (mState == EAS_STATE_PLAY);
}

status_t MidiFile::getCurrentPosition(int* position)
{
    ALOGV("MidiFile::getCurrentPosition");
    if (!mEasHandle) {
        ALOGE("getCurrentPosition(): file not open");
        return ERROR_NOT_OPEN;
    }
    if (mPlayTime < 0) {
        ALOGE("getCurrentPosition(): mPlayTime = %ld", mPlayTime);
        return ERROR_EAS_FAILURE;
    }
    *position = mPlayTime;
    return NO_ERROR;
}

status_t MidiFile::getDuration(int* duration)
{

    ALOGV("MidiFile::getDuration");
    {
        Mutex::Autolock lock(mMutex);
        if (!mEasHandle) return ERROR_NOT_OPEN;
        *duration = mDuration;
    }
#ifndef ANDROID_DEFAULT_CODE
    SXLOGV("MidiFile::getDuration, *duration = %ld, addr=0x%x, mDuration = %ld", *duration, duration, mDuration);
#endif
    // if no duration cached, get the duration
    // don't need a lock here because we spin up a new engine
    if (*duration < 0) {
        EAS_I32 temp;
        EAS_DATA_HANDLE easData = NULL;
        EAS_HANDLE easHandle = NULL;
        EAS_RESULT result = EAS_Init(&easData);
        if (result == EAS_SUCCESS) {
            result = EAS_OpenFile(easData, &mFileLocator, &easHandle);
        }
        if (result == EAS_SUCCESS) {
            result = EAS_Prepare(easData, easHandle);
        }
        if (result == EAS_SUCCESS) {
            result = EAS_ParseMetaData(easData, easHandle, &temp);
        }
        if (easHandle) {
            EAS_CloseFile(easData, easHandle);
        }
        if (easData) {
            EAS_Shutdown(easData);
        }

        if (result != EAS_SUCCESS) {
            return ERROR_EAS_FAILURE;
        }

        // cache successful result
        mDuration = *duration = int(temp);
    }

    return NO_ERROR;
}

status_t MidiFile::release()
{
    ALOGV("MidiFile::release");
    Mutex::Autolock l(mMutex);
    reset_nosync();

    // wait for render thread to exit
    mExit = true;
    mCondition.signal();

    // wait for thread to exit
    if (mAudioBuffer) {
        mCondition.wait(mMutex);
    }

    // release resources
    if (mEasData) {
        EAS_Shutdown(mEasData);
        mEasData = NULL;
    }
    return NO_ERROR;
}

status_t MidiFile::reset()
{
    ALOGV("MidiFile::reset");
    Mutex::Autolock lock(mMutex);
    return reset_nosync();
}

// call only with mutex held
status_t MidiFile::reset_nosync()
{
    ALOGV("MidiFile::reset_nosync");
#ifndef ANDROID_DEFAULT_CODE
#ifdef MidiFile_Duration_thread
    stopDurationLoop();
    SXLOGV("MidiFile::release mGetDurationProcessing =%d", mGetDurationProcessing);
    if(mGetDurationProcessing == true)
    {
       mIsReset = true;
       SXLOGV("MidiFile::release +mgetDurationCondition.wait");
	     Mutex::Autolock p(mgetDurationMutex);    	
       mgetDurationCondition.wait(mgetDurationMutex);      
       SXLOGV("MidiFile::release -mgetDurationCondition.wait");
    }
    mForceStopDuration = false;    
#endif 
    mSeekNewBufCount = 0;
#endif
    // close file
    if (mEasHandle) {
        EAS_CloseFile(mEasData, mEasHandle);
        mEasHandle = NULL;
    }
    if (mFileLocator.path) {
        free((void*)mFileLocator.path);
        mFileLocator.path = NULL;
    }
    if (mFileLocator.fd >= 0) {
        close(mFileLocator.fd);
    }
    mFileLocator.fd = -1;
    mFileLocator.offset = 0;
    mFileLocator.length = 0;

    mPlayTime = -1;
    mDuration = -1;
    mLoop = false;
    mPaused = false;
    mRender = false;
    return NO_ERROR;
}

status_t MidiFile::setLooping(int loop)
{
    ALOGV("MidiFile::setLooping");
    Mutex::Autolock lock(mMutex);
    if (!mEasHandle) {
        return ERROR_NOT_OPEN;
    }
    loop = loop ? -1 : 0;
    if (EAS_SetRepeat(mEasData, mEasHandle, loop) != EAS_SUCCESS) {
        return ERROR_EAS_FAILURE;
    }
    return NO_ERROR;
}

status_t MidiFile::createOutputTrack() {
    if (mAudioSink->open(pLibConfig->sampleRate, pLibConfig->numChannels,
            CHANNEL_MASK_USE_CHANNEL_ORDER, AUDIO_FORMAT_PCM_16_BIT, 2) != NO_ERROR) {
        ALOGE("mAudioSink open failed");
        return ERROR_OPEN_FAILED;
    }
    return NO_ERROR;
}

#ifndef ANDROID_DEFAULT_CODE
#ifdef MidiFile_Duration_thread
status_t MidiFile::stopDurationLoop()
{
    status_t result;
    SXLOGV("MidiFile::stopDurationLoop, tid(%d), ptr(0x%x)", myTid(), this);
    if(mGetDurationProcessing == true)
    {
        mForceStopDuration = true;
        SXLOGV("MidiFile::stopDurationLoop call EAS_SetForceStopDuration, tid(%d)", myTid());
        //result = EAS_SetForceStopDuration(mEasDataDuration, mForceStopDuration);
        result = EAS_SetForceStopDuration(mEasData, mForceStopDuration);
        
        if (result == EAS_SUCCESS) 
            return NO_ERROR;
        else
            return ERROR_EAS_FAILURE;
    }
    return NO_ERROR;
}

int MidiFile::getDurationThread(void* p) {

        SXLOGV("MidiFile::getDurationThread call getDurationLoop, tid(%d)", myTid());
        ((MidiFile*)p)->getDurationLoop();
        return EAS_SUCCESS;
}

void MidiFile::getDurationLoop() 
{
    status_t result;
    int tid = myTid();
    mGetDurationProcessing = true;
    //mgetDurationMutex.lock();
    // nothing to render, wait for client thread to wake us up
    {
        // signal prepare that we started
        Mutex::Autolock p(mgetDurationMutex);
        SXLOGV("MidiFile::getDurationLoop thread(%d) +signal", tid);
        mgetDurationCondition.signal();
        SXLOGV("MidiFile::getDurationLoop thread(%d) -signal", tid);
    }
    

    if (mDuration < 0) 
    {
      // if no duration cached, get the duration
      // don't need a lock here because we spin up a new engine
      {
         EAS_I32 temp;
         /*EAS_DATA_HANDLE easData = NULL;
         EAS_HANDLE easHandle = NULL;
         SXLOGV("MidiFile::getDurationLoop thread(%d) +EAS_Init", tid);
         result = EAS_Init(&easData);
         SXLOGV("MidiFile::getDurationLoop thread(%d) -EAS_Init", tid);
         mEasDataDuration = easData;
         if (result == EAS_SUCCESS) {
         //SXLOGV("MidiFile::getDurationLoop thread(%d) +EAS_OpenFile", tid);             
            result = EAS_OpenFile(easData, &mFileLocator, &easHandle);
            SXLOGV("MidiFile::getDurationLoop thread(%d) -EAS_OpenFile", tid);                
         }
         if (result == EAS_SUCCESS) {
            SXLOGV("MidiFile::getDurationLoop thread(%d) +EAS_Prepare start------", tid);
            result = EAS_Prepare(easData, easHandle);
            SXLOGV("MidiFile::getDurationLoop thread(%d) -EAS_Prepare end------", tid);
         }
         if (result == EAS_SUCCESS) {*/
            SXLOGV("MidiFile::getDurationLoop thread(%d) +EAS_ParseMetaData start------", tid);
            result = EAS_ParseMetaData(mEasData, mEasHandle, &temp);
            SXLOGV("MidiFile::getDurationLoop thread(%d) -EAS_ParseMetaData end: temp=%ld------", tid, temp);
         /*}
         if (easHandle) {
            EAS_CloseFile(easData, easHandle);
         }
         if (easData) {
            EAS_Shutdown(easData);
            mEasDataDuration = NULL;
         }*/
         if (result != EAS_SUCCESS) {
             SXLOGV("MidiFile::getDurationLoop thread(%d) ERROR: temp= %ld, duration = %ld------", tid, temp, mDuration);
             //mgetDurationMutex.unlock();  
             if(mForceStopDuration != false)
                mDuration = 0;
               //break;
          }
          else
          {
              // cache successful result
              mDuration = int(temp);
              SXLOGV("MidiFile::getDurationLoop thread(%d) SUCCESS: mDuration = %ld-------------------------------------------------------------------", tid, mDuration);
          }                
              mForceStopDuration = false;
        }
        SXLOGV("MidiFile::getDurationLoop thread(%d) GetDurationDone", tid);
        mGetDurationProcessing = false;

        if(mIsReset == false)
        {
            mGetDurationProcessing = false;
           // don't hold lock during callback
           if (result == EAS_SUCCESS) {
               ALOGD("MidiFile::getDurationLoop thread(%d) sendEvent(MEDIA_PREPARED)", tid);
               sendEvent(MEDIA_PREPARED, tid);
           } else {
               ALOGD("MidiFile::getDurationLoop thread(%d) sendEvent(MEDIA_ERROR)", tid);
               sendEvent(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, result);
           }
        }
        else
        {
            //don't sentEvent
            //signal to reset_nosync func
            Mutex::Autolock p(mgetDurationMutex);  
            SXLOGV("MidiFile::getDurationLoop thread(%d) +signal 2", tid);
            mgetDurationCondition.signal();
            SXLOGV("MidiFile::getDurationLoop thread(%d) -signal 2", tid);
        }        
    }
    else
    {        
        mGetDurationProcessing = false;
        if(mIsReset == false)
        {
            sendEvent(MEDIA_PREPARED);     
        }
        else
        {
            //don't sentEvent
            //signal to reset_nosync func
            Mutex::Autolock p(mgetDurationMutex);  
            SXLOGV("MidiFile::getDurationLoop thread(%d) +signal 3", tid);
            mgetDurationCondition.signal();
            SXLOGV("MidiFile::getDurationLoop thread(%d) -signal 3", tid);
        }        
    }
    //all the thread done
}
#endif
#endif
int MidiFile::render() {
    EAS_RESULT result = EAS_FAILURE;
    EAS_I32 count;
    int temp;
    bool audioStarted = false;
#ifndef ANDROID_DEFAULT_CODE    
#ifdef RTPM_PRIO_MIDIFILE
    int t_result = -1;         // if set prority false , force to set priority
    if(t_result == -1)
    {
       struct sched_param sched_p;
       sched_getparam(0, &sched_p);
       sched_p.sched_priority = RTPM_PRIO_MIDI_FILE;
       if(0 != sched_setscheduler(0, SCHED_RR, &sched_p)) 
       {
          SXLOGE("[%s] failed, errno: %d", __func__, errno);
       }
       else 
       {
          sched_getparam(0, &sched_p);
          SXLOGV("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
       }
    }
#endif
#endif
    ALOGV("MidiFile::render");

    // allocate render buffer
    mAudioBuffer = new EAS_PCM[pLibConfig->mixBufferSize * pLibConfig->numChannels * NUM_BUFFERS];
    if (!mAudioBuffer) {
        ALOGE("mAudioBuffer allocate failed");
        goto threadExit;
    }

    // signal main thread that we started
    {
        Mutex::Autolock l(mMutex);
        mTid = gettid();
        ALOGV("render thread(%d) signal", mTid);
        mCondition.signal();
    }

    while (1) {
        mMutex.lock();
#ifndef ANDROID_DEFAULT_CODE
        memset(mAudioBuffer, 0, pLibConfig->mixBufferSize * pLibConfig->numChannels * NUM_BUFFERS);
#endif
        // nothing to render, wait for client thread to wake us up
        while (!mRender && !mExit)
        {
            ALOGV("MidiFile::render - signal wait");
            mCondition.wait(mMutex);
            ALOGV("MidiFile::render - signal rx'd");
        }
        if (mExit) {
            mMutex.unlock();
            break;
        }
#ifndef ANDROID_DEFAULT_CODE
        if(mDuration > 0)
        {
           EAS_SetDuration(mEasData, mDuration);
        }
#endif
        // render midi data into the input buffer
        //ALOGV("MidiFile::render - rendering audio");
        int num_output = 0;
        EAS_PCM* p = mAudioBuffer;
        for (int i = 0; i < NUM_BUFFERS; i++) {
            result = EAS_Render(mEasData, p, pLibConfig->mixBufferSize, &count);
            if (result != EAS_SUCCESS) {
                ALOGE("EAS_Render returned %ld", result);
            }
            p += count * pLibConfig->numChannels;
            num_output += count * pLibConfig->numChannels * sizeof(EAS_PCM);
        }

        // update playback state and position
        // ALOGV("MidiFile::render - updating state");
        EAS_GetLocation(mEasData, mEasHandle, &mPlayTime);
        EAS_State(mEasData, mEasHandle, &mState);
#ifndef ANDROID_DEFAULT_CODE      
        if(mPlayTime > mMaxPlayTime)
    		   mMaxPlayTime = mPlayTime;
        if ((mState == EAS_STATE_STOPPED) || (mState == EAS_STATE_ERROR) ||(mState == EAS_STATE_PAUSED))
		       mRender = false;
#endif
        mMutex.unlock();

        // create audio output track if necessary
        if (!mAudioSink->ready()) {
            ALOGV("MidiFile::render - create output track");
            if (createOutputTrack() != NO_ERROR)
                goto threadExit;
        }
#ifndef ANDROID_DEFAULT_CODE
//Tina add: start ramp up right after seek 
        if(mSeekNewBufCount == 20)
        {
            // Audio Track Ramp Up
             mAudioSink->setVolume(1.0, 1.0);
        }
        if(mSeekNewBufCount > 0)
        {
            mSeekNewBufCount--; 
        }
#endif    

        // Write data to the audio hardware
#ifndef ANDROID_DEFAULT_CODE  
        if (audioStarted) {        
#endif       
        // ALOGV("MidiFile::render - writing to audio output");
        if ((temp = mAudioSink->write(mAudioBuffer, num_output)) < 0) {
            ALOGE("Error in writing:%d",temp);
#ifndef ANDROID_DEFAULT_CODE  
            goto threadExit;
#else
            return temp;
#endif
        }
#ifndef ANDROID_DEFAULT_CODE            
         }
#endif
        // start audio output if necessary
        if (!audioStarted) {
            //ALOGV("MidiFile::render - starting audio");
#ifndef ANDROID_DEFAULT_CODE  
            //Tina add condition to check if the state is correct to start audio sink
            //if 1st render result is error, don't start audio sink
            if ((mState != EAS_STATE_STOPPING) && (mState != EAS_STATE_STOPPED) && (mState != EAS_STATE_ERROR) )
            {
                SXLOGV("MidiFile::render - starting AudioSink");
#endif
            mAudioSink->start();
            audioStarted = true;
#ifndef ANDROID_DEFAULT_CODE            
            }
#endif
        }

        // still playing?
        if ((mState == EAS_STATE_STOPPED) || (mState == EAS_STATE_ERROR) ||
                (mState == EAS_STATE_PAUSED))
        {
#ifndef ANDROID_DEFAULT_CODE        	            
            //Tina 
            //1. add condition to check if the state is correct to start audio sink
            //if audio sink doesn't start, don't need to set stop
            //2. set mAudioSink->stop before sent event to ap
            if (audioStarted)
            {
                SXLOGV("MidiFile::render - stopping AudioSink");
                mAudioSink->stop();
                audioStarted = false;
                mRender = false;
            }
#endif
            switch(mState) {
            case EAS_STATE_STOPPED:
            {
                ALOGV("MidiFile::render - stopped");

#ifndef ANDROID_DEFAULT_CODE             
#ifdef MTK_DRM_APP
                mIsCurrentComplete = true;
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

                sendEvent(MEDIA_PLAYBACK_COMPLETE);
                break;
            }
            case EAS_STATE_ERROR:
            {
                ALOGE("MidiFile::render - error");
                sendEvent(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN);
                break;
            }
            case EAS_STATE_PAUSED:
                ALOGV("MidiFile::render - paused");
                break;
            default:
                break;
            }
#ifdef ANDROID_DEFAULT_CODE              
            mAudioSink->stop();
            audioStarted = false;
            mRender = false;
#endif
        }
    }
#ifndef ANDROID_DEFAULT_CODE
    SXLOGD("MidiFile::render - threadExit, mRender=%d, mExit=%d", mRender, mExit);
#endif
threadExit:
    mAudioSink.clear();
    if (mAudioBuffer) {
        delete [] mAudioBuffer;
        mAudioBuffer = NULL;
    }
    mMutex.lock();
    mTid = -1;
    mCondition.signal();
    mMutex.unlock();
    return result;
}

} // end namespace android
