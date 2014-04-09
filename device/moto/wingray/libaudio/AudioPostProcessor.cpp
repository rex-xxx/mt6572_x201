/*
** Copyright 2010, The Android Open-Source Project
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
#define LOG_TAG "AudioPostProcessor"
#include <fcntl.h>
#include <utils/Log.h>
#include "AudioHardware.h"
#include "AudioPostProcessor.h"
#include <sys/stat.h>
#include "mot_acoustics.h"
// hardware specific functions
extern uint16_t HC_CTO_AUDIO_MM_PARAMETER_TABLE[];
///////////////////////////////////
// Some logging #defines
#define ECNS_LOG_ENABLE_OFFSET 1 // 2nd word of the configuration buffer
#define ECNS_LOGGING_BITS 0xBFFF // 15 possible logpoints

#define MOT_LOG_DELIMITER_START  0xFEED
#define MOT_LOG_DELIMITER_END    0xF00D
#define BASIC_DOCK_PROP_VALUE    0

#define ECNSLOGPATH "/data/ecns"
#define DOCK_PROP_PATH "/sys/class/switch/dock/dock_prop"

#ifndef ARRAY_SIZE
#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))
#endif

//#define DEBUG_TIMING
#ifdef DEBUG_TIMING
struct timeval mtv1, mtv2, mtv3, mtv4, mtv5, mtv6, mtv7, mtv8;
#define GETTIMEOFDAY gettimeofday
#else
#define GETTIMEOFDAY(a,b)
#endif

namespace android_audio_legacy {

AudioPostProcessor::AudioPostProcessor() :
    mEcnsScratchBuf(0), mLogNumPoints(0),  mEcnsDlBuf(0), mEcnsDlBufSize(0), mEcnsThread(0)
{
    ALOGD("%s",__FUNCTION__);

    // One-time CTO Audio configuration
    mAudioMmEnvVar.cto_audio_mm_param_block_ptr              = HC_CTO_AUDIO_MM_PARAMETER_TABLE;
    mAudioMmEnvVar.cto_audio_mm_pcmlogging_buffer_block_ptr  = mPcmLoggingBuf;
    mAudioMmEnvVar.pcmlogging_buffer_block_size              = ARRAY_SIZE(mPcmLoggingBuf);
    mAudioMmEnvVar.cto_audio_mm_runtime_param_mem_ptr        = mRuntimeParam;
    mAudioMmEnvVar.cto_audio_mm_static_memory_block_ptr      = mStaticMem;
    mAudioMmEnvVar.cto_audio_mm_scratch_memory_block_ptr     = mScratchMem;
    mAudioMmEnvVar.accy = CTO_AUDIO_MM_ACCY_INVALID;
    mAudioMmEnvVar.sample_rate = CTO_AUDIO_MM_SAMPL_44100;

    mEcnsThread = new EcnsThread();
    // Initial conditions for EC/NS
    stopEcns();
}

AudioPostProcessor::~AudioPostProcessor()
{
    if (mEcnsRunning) {
        ALOGD("%s",__FUNCTION__);
        enableEcns(0);
    }
}

uint32_t AudioPostProcessor::convOutDevToCTO(uint32_t outDev)
{
    int32_t dock_prop = 0;
    // Only loudspeaker and audio docks are currently in this table
    switch (outDev) {
       case CPCAP_AUDIO_OUT_SPEAKER:
           return CTO_AUDIO_MM_ACCY_LOUDSPEAKER;
       case CPCAP_AUDIO_OUT_ANLG_DOCK_HEADSET:
           dock_prop = read_dock_prop(DOCK_PROP_PATH);
           if ((dock_prop < 0) || (dock_prop == BASIC_DOCK_PROP_VALUE)) {
               // Basic dock, or error getting the dock ID
               return CTO_AUDIO_MM_ACCY_INVALID;
	   }
           else // speaker dock
               return CTO_AUDIO_MM_ACCY_DOCK;
       default:
           return CTO_AUDIO_MM_ACCY_INVALID;
    }
}

uint32_t AudioPostProcessor::convRateToCto(uint32_t rate)
{
    switch (rate) {
        case 44100: // Most likely.
            return CTO_AUDIO_MM_SAMPL_44100;
        case 8000:
            return CTO_AUDIO_MM_SAMPL_8000;
        case 11025:
            return CTO_AUDIO_MM_SAMPL_11025;
        case 12000:
            return CTO_AUDIO_MM_SAMPL_12000;
        case 16000:
            return CTO_AUDIO_MM_SAMPL_16000;
        case 22050:
            return CTO_AUDIO_MM_SAMPL_22050;
        case 24000:
            return CTO_AUDIO_MM_SAMPL_24000;
        case 32000:
            return CTO_AUDIO_MM_SAMPL_32000;
        case 48000:
            return CTO_AUDIO_MM_SAMPL_48000;
        default:
            return CTO_AUDIO_MM_SAMPL_44100;
    }
}

void AudioPostProcessor::configMmAudio()
{
    if (mAudioMmEnvVar.accy != CTO_AUDIO_MM_ACCY_INVALID) {
        ALOGD("Configure CTO Audio MM processing");
        // fetch the corresponding runtime audio parameter
        api_cto_audio_mm_param_parser(&(mAudioMmEnvVar), (int16_t *)0, (int16_t *)0);
        // Initialize algorithm static memory
        api_cto_audio_mm_init(&(mAudioMmEnvVar), (int16_t *)0, (int16_t *)0);
    } else {
        ALOGD("CTO Audio MM processing is disabled.");
    }
}

void AudioPostProcessor::enableEcns(int value)
{
    if (mEcnsEnabled!=value) {
        ALOGD("enableEcns() new %08x old %08x)", value, mEcnsEnabled);
        mEcnsThread->broadcastReadCond();
        mEcnsThread->requestExitAndWait();
        stopEcns();
        cleanupEcns();
        mEcnsEnabled = value;
    }
}

void AudioPostProcessor::setAudioDev(struct cpcap_audio_stream *outDev,
                                     struct cpcap_audio_stream *inDev,
                                     bool is_bt, bool is_bt_ec, bool is_spdif)
{
    int32_t dock_prop = 0;
    uint32_t mm_accy = convOutDevToCTO(outDev->id);
    Mutex::Autolock lock(mMmLock);

    if (is_bt) {
        if (is_bt_ec)
            mEcnsMode = CTO_AUDIO_USECASE_NB_BLUETOOTH_WITH_ECNS;
        else
            mEcnsMode = CTO_AUDIO_USECASE_NB_BLUETOOTH_WITHOUT_ECNS;
    } else if (is_spdif) // May need a more complex check here for HDMI vs. others
        mEcnsMode = CTO_AUDIO_USECASE_NB_ACCY_1;
    else if (outDev->id==CPCAP_AUDIO_OUT_HEADSET && inDev->id==CPCAP_AUDIO_IN_MIC1)
        mEcnsMode = CTO_AUDIO_USECASE_NB_HEADSET_WITH_HANDSET_MIC;
    else if (outDev->id==CPCAP_AUDIO_OUT_HEADSET)
        mEcnsMode = CTO_AUDIO_USECASE_NB_HEADSET;
    else if (outDev->id==CPCAP_AUDIO_OUT_ANLG_DOCK_HEADSET) {
        dock_prop = read_dock_prop(DOCK_PROP_PATH);
        if ((dock_prop < 0) || (dock_prop == BASIC_DOCK_PROP_VALUE))
            // Basic dock, or error getting the dock ID
            mEcnsMode = CTO_AUDIO_USECASE_NB_ACCY_1;
        else
            // Speaker Dock
            mEcnsMode = CTO_AUDIO_USECASE_NB_DEDICATED_DOCK;
    }
    else
        mEcnsMode = CTO_AUDIO_USECASE_NB_SPKRPHONE;

    if (mEcnsEnabled) {
        // We may need to reset the EC/NS if the output device changed.
        stopEcns();
    }

    ALOGV("setAudioDev %d", outDev->id);
    if (mm_accy != mAudioMmEnvVar.accy) {
        mAudioMmEnvVar.accy = mm_accy;
        configMmAudio();
    }
}

// Setting the HW sampling rate may require reconfiguration of audio processing.
void AudioPostProcessor::setPlayAudioRate(int sampRate)
{
    uint32_t rate = convRateToCto(sampRate);
    Mutex::Autolock lock(mMmLock);

    ALOGD("AudioPostProcessor::setPlayAudioRate %d", sampRate);
    if (rate != mAudioMmEnvVar.sample_rate) {
        mAudioMmEnvVar.sample_rate = rate;
        configMmAudio();
    }
}

void AudioPostProcessor::doMmProcessing(void * buffer, int numSamples)
{
    Mutex::Autolock lock(mMmLock);

    if (mAudioMmEnvVar.accy != CTO_AUDIO_MM_ACCY_INVALID &&
        !mEcnsEnabled) {
        // Apply the CTO audio effects in-place.
        mAudioMmEnvVar.frame_size = numSamples;
        api_cto_audio_mm_main(&mAudioMmEnvVar, (int16_t *)buffer, (int16_t *)buffer);
    }
}

int AudioPostProcessor::getEcnsRate (void)
{
    return mEcnsRate;
}

void AudioPostProcessor::initEcns(int rate, int bytes)
{
    ALOGD("%s",__FUNCTION__);
    CTO_AUDIO_USECASES_CTRL mode;
    Mutex::Autolock lock(mEcnsBufLock);

    if (rate != 8000 && rate != 16000) {
        ALOGW("Invalid rate for EC/NS, disabling");
        mEcnsEnabled = 0;
        mEcnsRunning = 0;
        return;
    }
    mode = mEcnsMode;
    mEcnsRate = rate;
    if (mEcnsRate==16000) {
       // Offset to the 16K (WB) block in the coefficients file
       mode = CTO_AUDIO_USECASES_CTRL(mode + CTO_AUDIO_USECASE_WB_HANDSET);
    }
    ALOGD("%s for mode %d at %d size %d",__FUNCTION__, mode, mEcnsRate, bytes);
    mEcnsCtrl.framesize = bytes/2;
    mEcnsCtrl.micFlag = 0; // 0- one mic.  1- dual mic. 2- three mic.
    mEcnsCtrl.digital_mode = (rate == 8000) ? 0 : 1;  // 8K or 16K
    mEcnsCtrl.usecase = mode;
    mMemBlocks.staticMemory_1 = mStaticMemory_1;
    mMemBlocks.staticMemory_2 = NULL;
    mMemBlocks.mot_datalog = mMotDatalog;
    mMemBlocks.gainTableMemory = mParamTable;

    FILE * fp = fopen("/system/etc/voip_aud_params.bin", "r");
    if (fp) {
        if (fread(mParamTable, sizeof(mParamTable), 1, fp) < 1) {
            ALOGE("Cannot read VOIP parameter file.  Disabling EC/NS.");
            fclose(fp);
            mEcnsEnabled = 0;
            mEcnsRunning = 0;
            return;
        }
        fclose(fp);
    }
    else {
        ALOGE("Cannot open VOIP parameter file.  Disabling EC/NS.");
        mEcnsEnabled = 0;
        mEcnsRunning = 0;
        return;
    }

    mEcnsRunning = 1;
    mEcnsOutBuf = 0;
    mEcnsOutBufSize = 0;
    mEcnsOutBufReadOffset = 0;

    // Send setup parameters to the EC/NS module, init the module.
    API_MOT_SETUP(&mEcnsCtrl, &mMemBlocks);
    API_MOT_INIT(&mEcnsCtrl, &mMemBlocks);
}

void AudioPostProcessor::stopEcns (void)
{
    AutoMutex lock(mEcnsBufLock);
    if (mEcnsRunning) {
        ALOGD("%s",__FUNCTION__);
        mEcnsRunning = 0;
    }
}

void AudioPostProcessor::cleanupEcns(void)
{
    AutoMutex lock(mEcnsBufLock);
    mEcnsRate = 0;
    if (mEcnsScratchBuf) {
        free(mEcnsScratchBuf);
        mEcnsScratchBuf = 0;
    }
    mEcnsScratchBufSize = 0;
    mEcnsOutFd = -1;

    if (mEcnsDlBuf) {
       free(mEcnsDlBuf);
       mEcnsDlBuf = 0;
    }
    mEcnsDlBufSize = 0;
    // In case write() is blocked, set it free.
    mEcnsBufCond.signal();

    ecnsLogToFile();
}


// Returns: Bytes written (actually "to-be-written" by EC/NS thread).
int AudioPostProcessor::writeDownlinkEcns(int fd, void * buffer, bool stereo,
                                          int bytes, Mutex *fdLock)
{
    int written = 0;

    // write directly to pcm out driver if only NS is enabled
    if (!(mEcnsEnabled & AEC)) {
        if (fd >= 0) {
            fdLock->lock();
            ::write(fd, buffer, bytes);
            fdLock->unlock();
        }
        return bytes;
    }

    mEcnsBufLock.lock();
    if (mEcnsEnabled && !mEcnsRunning) {
        long usecs = 20*1000;
        // Give the read thread a chance to catch up.
        ALOGV("%s: delay %d msecs for ec/ns to start",__FUNCTION__, (int)(usecs/1000));
        mEcnsBufLock.unlock();
        usleep(usecs);
        mEcnsBufLock.lock();
        written = bytes;  // Pretend all data was consumed even if ecns isn't running
    }
    if (mEcnsRunning) {
        // Only run through here after initEcns has been done by read thread.
        mEcnsOutFd = fd;
        mEcnsOutBuf = buffer;
        mEcnsOutBufSize = bytes;
        mEcnsOutBufReadOffset = 0;
        mEcnsOutFdLockp = fdLock;
        mEcnsOutStereo = stereo;
        if (mEcnsBufCond.waitRelative(mEcnsBufLock, seconds(1)) != NO_ERROR) {
            ALOGE("%s: Capture thread is stalled.", __FUNCTION__);
        }
        if (mEcnsOutBufSize != 0)
            ALOGD("%s: Buffer not consumed", __FUNCTION__);
        else
            written = bytes;  // All data consumed
    }
    mEcnsBufLock.unlock();
    return written;
}

// Returns: Bytes read.
int AudioPostProcessor::read(int fd, void * buffer, int bytes, int rate)
{
    if (mEcnsEnabled) {
        return mEcnsThread->readData(fd, buffer, bytes, rate, this);
    }
    ssize_t ret;
    ret = ::read(fd, buffer, bytes);
    if (ret < 0)
        ALOGE("Error reading from audio in: %s", strerror(errno));
    return (int)ret;
}

// Returns: Bytes processed.
int AudioPostProcessor::applyUplinkEcns(void * buffer, int bytes, int rate)
{
    static int16 ul_gbuff2[160];
    int16_t *dl_buf;
    int16_t *ul_buf = (int16_t *)buffer;
    int dl_buf_bytes=0;
    // The write thread could have left us with one frame of data in the
    // driver when we started reading.
    static bool onetime;

    if (!mEcnsEnabled)
        return 0;

    ALOGV("%s %d bytes at %d Hz",__FUNCTION__, bytes, rate);
    if (mEcnsEnabled && !mEcnsRunning) {
        initEcns(rate, bytes);
        onetime=true;
    }

    // In case the rate switched..
    if (mEcnsEnabled && rate != mEcnsRate) {
        stopEcns();
        initEcns(rate, bytes);
        onetime=true;
    }

    if (!mEcnsRunning) {
        ALOGE("EC/NS failed to init, read returns.");
        if (mEcnsEnabled & AEC) {
            mEcnsBufCond.signal();
        }
        return -1;
    }

    // do not get downlink audio if only NS is enabled
    if (mEcnsEnabled & AEC) {
        mEcnsBufLock.lock();
        // Need a contiguous stereo playback buffer in the end.
        if (bytes*2 != mEcnsDlBufSize || !mEcnsDlBuf) {
            if (mEcnsDlBuf)
                free(mEcnsDlBuf);
            mEcnsDlBuf = (int16_t*)malloc(bytes*2);
            if (mEcnsDlBuf)
                mEcnsDlBufSize = bytes*2;
        }
        dl_buf = mEcnsDlBuf;
        if (!dl_buf) {
            mEcnsBufLock.unlock();
            return -1;
        }

        // Need to gather appropriate amount of downlink speech.
        // Take oldest scratch data first.  The scratch buffer holds fractions of buffers
        // that were too small for processing.
        if (mEcnsScratchBuf && mEcnsScratchBufSize) {
            dl_buf_bytes = mEcnsScratchBufSize > bytes ? bytes:mEcnsScratchBufSize;
            memcpy(dl_buf, mEcnsScratchBuf, dl_buf_bytes);
            //ALOGD("Took %d bytes from mEcnsScratchBuf", dl_buf_bytes);
            mEcnsScratchBufSize -= dl_buf_bytes;
            if (mEcnsScratchBufSize==0) {
                // This should always be true.
                free(mEcnsScratchBuf);
                mEcnsScratchBuf = 0;
                mEcnsScratchBufSize = 0;
            }
        }
        // Take fresh data from write thread second.
        if (dl_buf_bytes < bytes) {
            int bytes_to_copy = mEcnsOutBufSize - mEcnsOutBufReadOffset;
            bytes_to_copy = bytes_to_copy + dl_buf_bytes > bytes?
                          bytes-dl_buf_bytes:bytes_to_copy;
            if (bytes_to_copy) {
                memcpy((void *)((unsigned int)dl_buf+dl_buf_bytes),
                       (void *)((unsigned int)mEcnsOutBuf+mEcnsOutBufReadOffset),
                       bytes_to_copy);
                dl_buf_bytes += bytes_to_copy;
            }
            //ALOGD("Took %d bytes from mEcnsOutBuf.  Need %d more.", bytes_to_copy,
            //      bytes-dl_buf_bytes);
            mEcnsOutBufReadOffset += bytes_to_copy;
            if (mEcnsOutBufSize - mEcnsOutBufReadOffset < bytes) {
                // We've depleted the output buffer, it's smaller than one uplink "frame".
                // First take any unused data into scratch, then free the write thread.
                if (mEcnsScratchBuf) {
                    ALOGE("Memleak - coding error");
                    free(mEcnsScratchBuf);
                }
                if (mEcnsOutBufSize - mEcnsOutBufReadOffset > 0) {
                    if ((mEcnsScratchBuf=malloc(mEcnsOutBufSize - mEcnsOutBufReadOffset)) == 0) {
                        ALOGE("%s: Alloc failed, scratch data lost.",__FUNCTION__);
                    } else {
                        mEcnsScratchBufSize = mEcnsOutBufSize - mEcnsOutBufReadOffset;
                        //ALOGD("....store %d bytes into scratch buf %p",
                        //     mEcnsScratchBufSize, mEcnsScratchBuf);
                        memcpy(mEcnsScratchBuf,
                               (void *)((unsigned int)mEcnsOutBuf+mEcnsOutBufReadOffset),
                               mEcnsScratchBufSize);
                    }
                }
                mEcnsOutBuf = 0;
                mEcnsOutBufSize = 0;
                mEcnsOutBufReadOffset = 0;
                //ALOGD("Signal write thread - need data.");
                mEcnsBufCond.signal();
            }
        }

        ALOGV_IF(dl_buf_bytes < bytes, "%s:EC/NS Starved for downlink data. have %d need %d.",
             __FUNCTION__,dl_buf_bytes, bytes);

        mEcnsBufLock.unlock();
    } else {
        if (mEcnsDlBufSize < bytes * 2) {
           mEcnsDlBufSize = bytes * 2;
           mEcnsDlBuf = (int16_t *)realloc(mEcnsDlBuf, mEcnsDlBufSize);
        }
        dl_buf = mEcnsDlBuf;
    }

    // Pad downlink with zeroes as last resort.  We have to process the UL speech.
    if (dl_buf_bytes < bytes) {
        memset(&dl_buf[dl_buf_bytes/sizeof(int16_t)],
               0,
               bytes-dl_buf_bytes);
    }

    // Do Echo Cancellation
    GETTIMEOFDAY(&mtv4, NULL);
    API_MOT_LOG_RESET(&mEcnsCtrl, &mMemBlocks);
    if (mEcnsEnabled & AEC) {
        API_MOT_DOWNLINK(&mEcnsCtrl, &mMemBlocks, (int16*)dl_buf, (int16*)ul_buf, &(ul_gbuff2[0]));
    }
    API_MOT_UPLINK(&mEcnsCtrl, &mMemBlocks, (int16*)dl_buf, (int16*)ul_buf, &(ul_gbuff2[0]));

    // Playback the echo-cancelled speech to driver.
    // Include zero padding.  Our echo canceller needs a consistent path.
    if (mEcnsEnabled & AEC) {
        if (mEcnsOutStereo) {
            // Convert up to stereo, in place.
            for (int i = bytes/2-1; i >= 0; i--) {
                dl_buf[i*2] = dl_buf[i];
                dl_buf[i*2+1] = dl_buf[i];
            }
            dl_buf_bytes *= 2;
        }
        GETTIMEOFDAY(&mtv5, NULL);
        if (mEcnsOutFd != -1) {
            mEcnsOutFdLockp->lock();
            ::write(mEcnsOutFd, &dl_buf[0],
                    bytes*(mEcnsOutStereo?2:1));
            mEcnsOutFdLockp->unlock();
        }
    }
    // Do the CTO SuperAPI internal logging.
    // (Do this after writing output to avoid adding latency.)
    GETTIMEOFDAY(&mtv6, NULL);
    ecnsLogToRam(bytes);
    return bytes;
}
void AudioPostProcessor::ecnsLogToRam (int bytes)
{
    uint16_t *logp;
    int mode = mEcnsMode + (mEcnsRate==16000?CTO_AUDIO_USECASE_WB_HANDSET:0);
    uint16_t *audioProfile = &mParamTable[AUDIO_PROFILE_PARAMETER_BLOCK_WORD16_SIZE*mode];

    if (audioProfile[ECNS_LOG_ENABLE_OFFSET] & ECNS_LOGGING_BITS) {
        if (!mLogBuf[0]) {
            mLogNumPoints = 0;
            mLogOffset = 0;
            ALOGE("EC/NS AUDIO LOGGER CONFIGURATION:");
            ALOGE("log enable %04X",
                audioProfile[ECNS_LOG_ENABLE_OFFSET]);
            mkdir(ECNSLOGPATH, 00770);
            for (uint16_t i=1; i>0; i<<=1) {
                if (i&ECNS_LOGGING_BITS&audioProfile[ECNS_LOG_ENABLE_OFFSET]) {
                   mLogNumPoints++;
                }
            }
            ALOGE("Number of log points is %d.", mLogNumPoints);
            logp = mMotDatalog;
            mLogSize = 10*60*50*bytes;
            for (int i=0; i<mLogNumPoints; i++) {
                // Allocate 10 minutes of logging per point
                mLogBuf[i]=(char *)malloc(mLogSize);
                if (!mLogBuf[i]) {
                    ALOGE("%s: Memory allocation failed.", __FUNCTION__);
                    for (int j=0; j<i; j++) {
                        free(mLogBuf[j]);
                        mLogBuf[j]=0;
                    }
                    return;
                }
            }
        }
        if (mLogOffset+bytes > mLogSize)
            return;
        logp = mMotDatalog;
        for (int i=0; i<mLogNumPoints; i++) {
            if (mLogBuf[i]) {
                mLogPoint[i] = logp[1];
                memcpy(&mLogBuf[i][mLogOffset], &logp[4], logp[2]*sizeof(uint16_t));
                logp += 4+logp[2];
            } else {
                ALOGE("EC/NS logging enabled, but memory not allocated");
            }
        }
        mLogOffset += bytes;
    }
}

void AudioPostProcessor::ecnsLogToFile()
{
    if (mLogNumPoints && mLogOffset > 16000*2) {
        for (int i=0; i<mLogNumPoints; i++) {
            FILE * fp;
            char fname[80];
            sprintf(fname, ECNSLOGPATH"/log-0x%04X.pcm", mLogPoint[i]);
            fp = fopen((const char *)fname, "w");
            if (fp) {
                ALOGE("Writing %d bytes to %s", mLogOffset, fname);
                fwrite(mLogBuf[i], mLogOffset, 1, fp);
                fclose(fp);
            } else {
                ALOGE("Problem writing to %s", fname);
            }
        }
    }
    mLogOffset = 0;
}

int AudioPostProcessor::read_dock_prop(char const *path)
{
    int fd = -1;
    const size_t SIZE = 7;
    static int already_warned = -1;
    char buffer[SIZE];
    /* the docks come with a property id AC000 for basic docks
       and AC002 for speaker docks, numbers might change, keeping
       them for now.
     */
    unsigned long int basic_dock_prop = 0xAC000;
    unsigned long int spkr_dock_prop;

    buffer[SIZE - 1] = '\0';
    fd = open(path, O_RDONLY);
    if (fd >= 0) {
        int amt = ::read(fd, buffer, SIZE-1);
        if (amt != SIZE-1) {
	    ALOGE("Incomplete dock property read, cannot validate dock");
	    return -1;
        }
        spkr_dock_prop = strtoul(buffer, NULL, 16);
	if (spkr_dock_prop <= 0) {
	    ALOGE("dock property conversion error");
	    return -EINVAL;
        }
        close(fd);
        ALOGV("buffer = %s, spkr_dock_prop = 0x%lX", buffer, spkr_dock_prop);
        spkr_dock_prop = spkr_dock_prop ^ basic_dock_prop;
        ALOGV("dock_prop returned = %lX", spkr_dock_prop);
        return spkr_dock_prop;
    } else {
        if (already_warned == -1) {
            ALOGE("read_dock_prop failed to open %s\n", path);
            already_warned = 1;
        }
        return -errno;
    }
}
// ---------------------------------------------------------------------------------------------
// Echo Canceller thread
// Needed to isolate the EC/NS module from scheduling jitter of it's clients.
//
AudioPostProcessor::EcnsThread::EcnsThread() :
    mReadBuf(0), mIsRunning(0)
{
}

AudioPostProcessor::EcnsThread::~EcnsThread()
{
}

int AudioPostProcessor::EcnsThread::readData(int fd, void * buffer, int bytes, int rate,
                                             AudioPostProcessor * pp)
{
    ALOGV("%s: read %d bytes at %d rate", __FUNCTION__, bytes, rate);
    Mutex::Autolock lock(mEcnsReadLock);
    mProcessor = pp;
    mFd = fd;
    mClientBuf = buffer;
    mReadSize = bytes;
    mRate = rate;
    if (!mIsRunning) {
        ALOGD("Create (run) the ECNS thread");
        run("AudioPostProcessor::EcnsThread", ANDROID_PRIORITY_HIGHEST);
        mIsRunning = true;
    }
    mEcnsReadCond.signal();
    if (mEcnsReadCond.waitRelative(mEcnsReadLock, seconds(1)) != NO_ERROR) {
        ALOGE("%s: ECNS thread is stalled.", __FUNCTION__);
        mClientBuf = 0;
        return -1;
    }
    return bytes;
}

bool AudioPostProcessor::EcnsThread::threadLoop()
{
#ifdef DEBUG_TIMING
    int count = 0;
    int small_jitter = 0;
    int medium_jitter = 0;
    int large_jitter = 0;
#endif
    ssize_t ret1 = 0, ret2;
    struct timeval tv1, tv2;
    int  usecs;
    bool half_done = false;
    int ecnsStatus = 0;

    ALOGD("%s: Enter thread loop size %d rate %d", __FUNCTION__,
                                          mReadSize, mRate);

    mReadBuf = (int16_t *) malloc(mReadSize);
    if (!mReadBuf)
        goto error;

    while (!exitPending() && ecnsStatus != -1) {
        GETTIMEOFDAY(&mtv1, NULL);
        if (!half_done)
            ret1 = ::read(mFd, mReadBuf, mReadSize/2);
        if(exitPending())
            goto error;
        GETTIMEOFDAY(&mtv2, NULL);
        ret2 = ::read(mFd, (char *)mReadBuf+mReadSize/2, mReadSize/2);
        if(exitPending())
            goto error;
        if (ret1 <= 0 || ret2 <= 0) {
            ALOGE("%s: Problem reading.", __FUNCTION__);
            goto error;
        }
        GETTIMEOFDAY(&mtv3, NULL);
        mEcnsReadLock.lock();
        ecnsStatus = mProcessor->applyUplinkEcns(mReadBuf, mReadSize, mRate);

        // wait for client buffer if not ready
        if (!mClientBuf) {
            if(exitPending()) {
                mEcnsReadLock.unlock();
                goto error;
            }
            if (mEcnsReadCond.waitRelative(mEcnsReadLock, seconds(1)) != NO_ERROR) {
                ALOGE("%s: client stalled.", __FUNCTION__);
            }
        }
        if (mClientBuf && mReadSize) {
            // Give the buffer to the client.
            memcpy(mClientBuf, mReadBuf, mReadSize);
            // Avoid read overflow by reading before signaling the similar-priority read thread.
            ret1 = ::read(mFd, mReadBuf, mReadSize/2);
            half_done = true;
            GETTIMEOFDAY(&mtv7, NULL);
            mClientBuf = 0;
            mEcnsReadCond.signal();
        } else {
            half_done = false;
            ALOGV("%s: Read overflow (ECNS sanity preserved)", __FUNCTION__);
        }
        mEcnsReadLock.unlock();
        GETTIMEOFDAY(&mtv8, NULL);

#ifdef DEBUG_TIMING
	count++;
        tv1.tv_sec = mtv1.tv_sec;
        tv1.tv_usec = mtv1.tv_usec;
        tv2.tv_sec = mtv8.tv_sec;
        tv2.tv_usec = mtv8.tv_usec;
        // Compare first and last timestamps
        tv2.tv_sec -= tv1.tv_sec;
        if(tv2.tv_usec < tv1.tv_usec) {
            tv2.tv_sec--;
            tv2.tv_usec = 1000000 + tv2.tv_usec - tv1.tv_usec;
        } else {
            tv2.tv_usec = tv2.tv_usec - tv1.tv_usec;
        }
        usecs = tv2.tv_usec + tv2.tv_sec*1000000;
        if (usecs > 25000) {
            if (usecs > 30000)
                large_jitter++;
            else
                medium_jitter++;
            ALOGD("jitter: usecs = %d should be 20000", usecs);
            ALOGD("Point 1 (      start): %03d.%06d:", (int)mtv1.tv_sec, (int)mtv1.tv_usec);
            ALOGD("Point 2 (after read1): %03d.%06d:", (int)mtv2.tv_sec, (int)mtv2.tv_usec);
            ALOGD("Point 3 (after read2): %03d.%06d:", (int)mtv3.tv_sec, (int)mtv3.tv_usec);
            ALOGD("Point 4 (before ECNS): %03d.%06d:", (int)mtv4.tv_sec, (int)mtv4.tv_usec);
            ALOGD("Point 5 (after  ECNS): %03d.%06d:", (int)mtv5.tv_sec, (int)mtv5.tv_usec);
            ALOGD("Point 6 (after write): %03d.%06d:", (int)mtv6.tv_sec, (int)mtv6.tv_usec);
            ALOGD("Point 7 (before sgnl): %03d.%06d:", (int)mtv7.tv_sec, (int)mtv7.tv_usec);
            ALOGD("Point 8 (after unlck): %03d.%06d:", (int)mtv8.tv_sec, (int)mtv8.tv_usec);
        } else if ((usecs > 22000) || (usecs < 18000)) {
            small_jitter++;
            ALOGD("jitter: usecs = %d should be 20000", usecs);
        }
        if ((count % 500)== 0) {
            ALOGD("====================================== Statistics ===========================");
            ALOGD(" After %d seconds:", count/50);
            ALOGD(" Small jitters-  %d (%02.5f%%)", small_jitter, ((float)small_jitter)*100/count);
            ALOGD(" Medium jitters- %d (%02.5f%%)", medium_jitter, ((float)medium_jitter)*100/count);
            ALOGD(" Large jitters-  %d (%02.5f%%)", large_jitter, ((float)large_jitter)*100/count);
            ALOGD("=============================================================================");
        }
#endif
    }
error:
    ALOGD("%s: Exit thread loop, enabled = %d", __FUNCTION__,mProcessor->isEcnsEnabled());
    if (mReadBuf) {
        free (mReadBuf);
        mReadBuf = 0;
    }
    mIsRunning = false;
    return false;
}

} //namespace android
