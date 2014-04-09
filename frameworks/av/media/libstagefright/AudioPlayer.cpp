/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#ifndef ANDROID_DEFAULT_CODE // Demon Deng
// for INT64_MAX
#undef __STRICT_ANSI__
#define __STDINT_LIMITS
#define __STDC_LIMIT_MACROS
#include <stdint.h>
#include <cutils/xlog.h>
#include <media/AudioSystem.h>
#endif // #ifndef ANDROID_DEFAULT_CODE
#define LOG_TAG "AudioPlayer"
#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <media/AudioTrack.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/AudioPlayer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>

#include "include/AwesomePlayer.h"

#ifdef MMPROFILE_HTTP
#include <linux/mmprofile.h>
MMP_Event MMP_AUDIOBUFFER;
#endif

namespace android {

AudioPlayer::AudioPlayer(
        const sp<MediaPlayerBase::AudioSink> &audioSink,
        bool allowDeepBuffering,
        AwesomePlayer *observer)
    : mAudioTrack(NULL),
      mInputBuffer(NULL),
      mSampleRate(0),
      mLatencyUs(0),
      mFrameSize(0),
      mNumFramesPlayed(0),
      mNumFramesPlayedSysTimeUs(ALooper::GetNowUs()),
      mPositionTimeMediaUs(-1),
      mPositionTimeRealUs(-1),
      mSeeking(false),
      mReachedEOS(false),
      mFinalStatus(OK),
      mStarted(false),
      mIsFirstBuffer(false),
      mFirstBufferResult(OK),
      mFirstBuffer(NULL),
      mAudioSink(audioSink),
      mAllowDeepBuffering(allowDeepBuffering),
      mObserver(observer),
#ifdef ANDROID_DEFAULT_CODE
      mPinnedTimeUs(-1ll)
#else
      mPinnedTimeUs(-1ll),
      mLastBufferTimeUs(0),
      mLastBufferSize(0),
      mPadEnable(false),
      mSeekTimeUs(-1),
      mAVSyncLastRealTime(-1ll)
#endif
{
#ifdef  MMPROFILE_HTTP
    MMP_Event  MMP_Player = MMProfileFindEvent(MMP_RootEvent, "Playback");
    if(MMP_Player !=0){
	    MMP_AUDIOBUFFER= MMProfileRegisterEvent(MMP_Player, "AudioSinkCallback");
	    MMProfileEnableEvent(MMP_AUDIOBUFFER,1); 	
    } 
#endif	
}

AudioPlayer::~AudioPlayer() {
    if (mStarted) {
        reset();
    }
}

void AudioPlayer::setSource(const sp<MediaSource> &source) {
    CHECK(mSource == NULL);
    mSource = source;
}

status_t AudioPlayer::start(bool sourceAlreadyStarted) {
    CHECK(!mStarted);
    CHECK(mSource != NULL);

    status_t err;
    if (!sourceAlreadyStarted) {
        err = mSource->start();

        if (err != OK) {
            return err;
        }
    }

    // We allow an optional INFO_FORMAT_CHANGED at the very beginning
    // of playback, if there is one, getFormat below will retrieve the
    // updated format, if there isn't, we'll stash away the valid buffer
    // of data to be used on the first audio callback.

    CHECK(mFirstBuffer == NULL);

#ifndef ANDROID_DEFAULT_CODE
    bool wasSeeking = false;
#endif
    MediaSource::ReadOptions options;
    if (mSeeking) {
        options.setSeekTo(mSeekTimeUs);
        mSeeking = false;
#ifndef ANDROID_DEFAULT_CODE
        wasSeeking = true;
#endif
    }

    mFirstBufferResult = mSource->read(&mFirstBuffer, &options);
    if (mFirstBufferResult == INFO_FORMAT_CHANGED) {
        ALOGV("INFO_FORMAT_CHANGED!!!");

        CHECK(mFirstBuffer == NULL);
        mFirstBufferResult = OK;
        mIsFirstBuffer = false;
    } else {
        mIsFirstBuffer = true;
    }

    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    bool success = format->findCString(kKeyMIMEType, &mime);
    CHECK(success);
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    success = format->findInt32(kKeySampleRate, &mSampleRate);
    CHECK(success);

    int32_t numChannels, channelMask;
    success = format->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);

    if(!format->findInt32(kKeyChannelMask, &channelMask)) {
        // log only when there's a risk of ambiguity of channel mask selection
        ALOGI_IF(numChannels > 2,
                "source format didn't specify channel mask, using (%d) channel order", numChannels);
        channelMask = CHANNEL_MASK_USE_CHANNEL_ORDER;
    }

    if (mAudioSink.get() != NULL) {

        status_t err = mAudioSink->open(
                mSampleRate, numChannels, channelMask, AUDIO_FORMAT_PCM_16_BIT,
                DEFAULT_AUDIOSINK_BUFFERCOUNT,
                &AudioPlayer::AudioSinkCallback,
                this,
                (mAllowDeepBuffering ?
                            AUDIO_OUTPUT_FLAG_DEEP_BUFFER :
                            AUDIO_OUTPUT_FLAG_NONE));
        if (err != OK) {
            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
        mFrameSize = mAudioSink->frameSize();

        mAudioSink->start();
    } else {
        // playing to an AudioTrack, set up mask if necessary
        audio_channel_mask_t audioMask = channelMask == CHANNEL_MASK_USE_CHANNEL_ORDER ?
                audio_channel_out_mask_from_count(numChannels) : channelMask;
        if (0 == audioMask) {
            return BAD_VALUE;
        }

        mAudioTrack = new AudioTrack(
                AUDIO_STREAM_MUSIC, mSampleRate, AUDIO_FORMAT_PCM_16_BIT, audioMask,
                0, AUDIO_OUTPUT_FLAG_NONE, &AudioCallback, this, 0);

        if ((err = mAudioTrack->initCheck()) != OK) {
            delete mAudioTrack;
            mAudioTrack = NULL;

            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
        mFrameSize = mAudioTrack->frameSize();

        mAudioTrack->start();
    }

    mStarted = true;
    mPinnedTimeUs = -1ll;

#ifndef ANDROID_DEFAULT_CODE
      if (wasSeeking) {
          SXLOGI("start with seeking");
          mPositionTimeRealUs = 0;
          mPositionTimeMediaUs = mSeekTimeUs;
      }
#endif
    return OK;
}

void AudioPlayer::pause(bool playPendingSamples) {
    CHECK(mStarted);

    if (playPendingSamples) {
        if (mAudioSink.get() != NULL) {
            mAudioSink->stop();
        } else {
            mAudioTrack->stop();
        }

        mNumFramesPlayed = 0;
        mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();
#ifndef ANDROID_DEFAULT_CODE
        mAVSyncLastRealTime = -1ll;
#endif
    } else {
        if (mAudioSink.get() != NULL) {
            mAudioSink->pause();
        } else {
            mAudioTrack->pause();
        }

        mPinnedTimeUs = ALooper::GetNowUs();
    }
}

void AudioPlayer::resume() {
    CHECK(mStarted);

    if (mAudioSink.get() != NULL) {
        mAudioSink->start();
    } else {
        mAudioTrack->start();
    }
}

void AudioPlayer::reset() {
    CHECK(mStarted);

    if (mAudioSink.get() != NULL) {
        mAudioSink->stop();
        mAudioSink->close();
    } else {
        mAudioTrack->stop();

        delete mAudioTrack;
        mAudioTrack = NULL;
    }

    // Make sure to release any buffer we hold onto so that the
    // source is able to stop().

    if (mFirstBuffer != NULL) {
        mFirstBuffer->release();
        mFirstBuffer = NULL;
    }

    if (mInputBuffer != NULL) {
        ALOGV("AudioPlayer releasing input buffer.");

        mInputBuffer->release();
        mInputBuffer = NULL;
    }

    mSource->stop();

    // The following hack is necessary to ensure that the OMX
    // component is completely released by the time we may try
    // to instantiate it again.
    wp<MediaSource> tmp = mSource;
    mSource.clear();
    while (tmp.promote() != NULL) {
        usleep(1000);
    }
    IPCThreadState::self()->flushCommands();

    mNumFramesPlayed = 0;
    mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();
    mPositionTimeMediaUs = -1;
    mPositionTimeRealUs = -1;
#ifndef ANDROID_DEFAULT_CODE
    mAVSyncLastRealTime = -1ll;
#endif
    mSeeking = false;
    mReachedEOS = false;
    mFinalStatus = OK;
    mStarted = false;
}

// static
void AudioPlayer::AudioCallback(int event, void *user, void *info) {
    static_cast<AudioPlayer *>(user)->AudioCallback(event, info);
}

bool AudioPlayer::isSeeking() {
    Mutex::Autolock autoLock(mLock);
    return mSeeking;
}

bool AudioPlayer::reachedEOS(status_t *finalStatus) {
    *finalStatus = OK;

    Mutex::Autolock autoLock(mLock);
    *finalStatus = mFinalStatus;
    return mReachedEOS;
}

status_t AudioPlayer::setPlaybackRatePermille(int32_t ratePermille) {
    if (mAudioSink.get() != NULL) {
        return mAudioSink->setPlaybackRatePermille(ratePermille);
    } else if (mAudioTrack != NULL){
        return mAudioTrack->setSampleRate(ratePermille * mSampleRate / 1000);
    } else {
        return NO_INIT;
    }
}

// static
size_t AudioPlayer::AudioSinkCallback(
        MediaPlayerBase::AudioSink *audioSink,
        void *buffer, size_t size, void *cookie) {
    AudioPlayer *me = (AudioPlayer *)cookie;

#ifdef  MMPROFILE_HTTP
    MMProfileLogMetaStringEx(MMP_AUDIOBUFFER, MMProfileFlagPulse,(uint32_t)buffer, size,"AudioSinkCallback+");      
#endif	

    return me->fillBuffer(buffer, size);
}

void AudioPlayer::AudioCallback(int event, void *info) {
    if (event != AudioTrack::EVENT_MORE_DATA) {
        return;
    }

    AudioTrack::Buffer *buffer = (AudioTrack::Buffer *)info;
    size_t numBytesWritten = fillBuffer(buffer->raw, buffer->size);

    buffer->size = numBytesWritten;
}

uint32_t AudioPlayer::getNumFramesPendingPlayout() const {
    uint32_t numFramesPlayedOut;
    status_t err;

    if (mAudioSink != NULL) {
        err = mAudioSink->getPosition(&numFramesPlayedOut);
    } else {
        err = mAudioTrack->getPosition(&numFramesPlayedOut);
    }

    if (err != OK || mNumFramesPlayed < numFramesPlayedOut) {
        return 0;
    }

    // mNumFramesPlayed is the number of frames submitted
    // to the audio sink for playback, but not all of them
    // may have played out by now.
    return mNumFramesPlayed - numFramesPlayedOut;
}

#ifndef ANDROID_DEFAULT_CODE //weiguo.li
uint32_t AudioPlayer::getNumFramesPlayout() const {

    uint32_t numFramesPlayedOut=0;
    status_t err;

    if (mAudioSink != NULL) {
    	err = mAudioSink->getPosition(&numFramesPlayedOut);
    } else {
    	err = mAudioTrack->getPosition(&numFramesPlayedOut);
    }

    if (err != OK ) {
    	return 0;
    }
    SXLOGV("getNumFramesPlayout:: numFramesPlayedOut =%u",numFramesPlayedOut);
    //add latency only if two frames have been played out.
    //this accurate time is for camera burst shot . now audiosink callback will send mute
    // data to audiotrack if no data is available before eos is send out. if extra latency is add
    // more mute data will be send to audiotrack. the interval of burstshot will has  more
    // mute data.
    int64_t playedUs =(1000000ll * numFramesPlayedOut) / mSampleRate;
    uint32_t afLatency = 0;
    AudioSystem::getOutputLatency(&afLatency);
    if(playedUs > (2* afLatency))
        return numFramesPlayedOut;
    return 0;
}
#endif
	

size_t AudioPlayer::fillBuffer(void *data, size_t size) {
    if (mNumFramesPlayed == 0) {
        ALOGV("AudioCallback");
    }

    if (mReachedEOS) {
        return 0;
    }
#ifndef ANDROID_DEFAULT_CODE
    bool mNeedSkipFrames = false;
#endif
    bool postSeekComplete = false;
    bool postEOS = false;
    int64_t postEOSDelayUs = 0;

    size_t size_done = 0;
    size_t size_remaining = size;
    while (size_remaining > 0) {
        MediaSource::ReadOptions options;

        {
            Mutex::Autolock autoLock(mLock);

            if (mSeeking) {
                if (mIsFirstBuffer) {
#ifndef ANDROID_DEFAULT_CODE
					// sam for first frame err--->
					if (mFirstBufferResult != OK) {					
						mPositionTimeMediaUs = mSeekTimeUs;
						mSeeking = false;
						if (mObserver) {
							postSeekComplete = true;
						}
						
						if (mObserver && !mReachedEOS) {
							postEOS = true;;
						}
						SXLOGW("AudioPlayer::fillBuffer--first frame error(when seek)!!");
						mReachedEOS = true;
						mIsFirstBuffer = false;
						mFinalStatus = mFirstBufferResult;
						break;
					}
					// <---sam for first frame err
#endif  //#ifndef ANDROID_DEFAULT_CODE					
                    if (mFirstBuffer != NULL) {
                        mFirstBuffer->release();
                        mFirstBuffer = NULL;
                    }
                    mIsFirstBuffer = false;
                }

#ifndef ANDROID_DEFAULT_CODE
				if (mPadEnable) {
					mLastBufferSize = 0;//hai
					mLastBufferTimeUs = mSeekTimeUs;
				}
#endif
                options.setSeekTo(mSeekTimeUs);

                if (mInputBuffer != NULL) {
                    mInputBuffer->release();
                    mInputBuffer = NULL;
                }

                mSeeking = false;
                if (mObserver) {
                    postSeekComplete = true;
                }
#if 1
#ifndef ANDROID_DEFAULT_CODE
                // don't do this for RTSP
                if (mSeekTimeUs != INT64_MAX)
                {
                	sp<MetaData> format = mSource->getFormat();
#ifdef MTK_AUDIO_APE_SUPPORT                
                    const char *mime;
                    bool mimesuccess = format->findCString(kKeyApeFlag, &mime);
                    if((mimesuccess==true) 
                        && (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_APE)))
                        mNeedSkipFrames = true;
                    else
#endif                        
                    {
	                    const char *mime;
	                    bool mimesuccess = format->findCString(kKeyVorbisFlag, &mime);
	                    if((mimesuccess==true) 
	                        &&(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_VORBIS)))
	                        mNeedSkipFrames = true;
	                    else
	                        mNeedSkipFrames = false;
                   }
                }
#endif
#endif
            }
        }

        if (mInputBuffer == NULL) {
            status_t err;

            if (mIsFirstBuffer) {
                mInputBuffer = mFirstBuffer;
                mFirstBuffer = NULL;
                err = mFirstBufferResult;

                mIsFirstBuffer = false;
            } else {
                err = mSource->read(&mInputBuffer, &options);
#ifndef ANDROID_DEFAULT_CODE  
                if(mNeedSkipFrames) {              
                    Mutex::Autolock autoLock(mLock);//<-start Changqing
                    int64_t positionTimeMediaUS = mPositionTimeMediaUs;
                    struct timeval ts,te;
                    gettimeofday(&ts,NULL);
				       
                    do{
                        gettimeofday(&te,NULL);
                        if((te.tv_sec - ts.tv_sec) > 3) {
                            SXLOGD("accurate read costs much time");
					 	                mNeedSkipFrames = false;
                            break;
                        }
					  CHECK((err == OK && mInputBuffer != NULL)|| (err != OK && mInputBuffer == NULL));
					      if(err != OK)
					 	      mNeedSkipFrames = false;
					      else {
					 	           CHECK(mInputBuffer->meta_data()->findInt64(kKeyTime, &positionTimeMediaUS));
                                   //for ape extra high compress type, frame is more than 180s..
						           if(((mSeekTimeUs - positionTimeMediaUS) > 100000) && ((mSeekTimeUs - positionTimeMediaUS) < 200000000)) {
						               mInputBuffer->release();
  					                   mInputBuffer = NULL;
						               err = mSource->read(&mInputBuffer);
						           }else
  						               mNeedSkipFrames = false;
					           }
				    }while(mNeedSkipFrames); //->end Changqing              
				}
#endif
            }

            CHECK((err == OK && mInputBuffer != NULL)
                   || (err != OK && mInputBuffer == NULL));

            Mutex::Autolock autoLock(mLock);

            if (err != OK) {
                if (mObserver && !mReachedEOS) {
                    // We don't want to post EOS right away but only
                    // after all frames have actually been played out.

                    // These are the number of frames submitted to the
                    // AudioTrack that you haven't heard yet.
                    uint32_t numFramesPendingPlayout =
                        getNumFramesPendingPlayout();

                    // These are the number of frames we're going to
                    // submit to the AudioTrack by returning from this
                    // callback.
                    uint32_t numAdditionalFrames = size_done / mFrameSize;

                    numFramesPendingPlayout += numAdditionalFrames;

                    int64_t timeToCompletionUs =
                        (1000000ll * numFramesPendingPlayout) / mSampleRate;

                    ALOGV("total number of frames played: %lld (%lld us)",
                            (mNumFramesPlayed + numAdditionalFrames),
                            1000000ll * (mNumFramesPlayed + numAdditionalFrames)
                                / mSampleRate);

                    ALOGV("%d frames left to play, %lld us (%.2f secs)",
                         numFramesPendingPlayout,
                         timeToCompletionUs, timeToCompletionUs / 1E6);

                    postEOS = true;
                    if (mAudioSink->needsTrailingPadding()) {
#ifndef ANDROID_DEFAULT_CODE
                        uint32_t afLatency = 0;
                        if(getNumFramesPlayout() >0) {
                            if (AudioSystem::getOutputLatency(&afLatency) != NO_ERROR) {
        								        afLatency = mLatencyUs/3;
                            }
                        }
                    
                        postEOSDelayUs = timeToCompletionUs + 2*1000*afLatency;
                        SXLOGD("postEOSDelayUs =%lld ms",postEOSDelayUs/1000);
#else
                        postEOSDelayUs = timeToCompletionUs + mLatencyUs;
#endif
                    } else {
                        postEOSDelayUs = 0;
                    }
                }

                mReachedEOS = true;
                mFinalStatus = err;
                break;
            }

            if (mAudioSink != NULL) {
                mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
            } else {
                mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
            }

            CHECK(mInputBuffer->meta_data()->findInt64(
                        kKeyTime, &mPositionTimeMediaUs));
#ifndef ANDROID_DEFAULT_CODE
			if (mPadEnable)
			{//hai
				if (mPositionTimeMediaUs < mLastBufferTimeUs) {
                    // Demon, skip the frame if all samples are late
                    // INT64_MAX is used for seeking
                    if (mLastBufferTimeUs != INT64_MAX) {
                        size_t size = (mLastBufferTimeUs - mPositionTimeMediaUs)
                            *mSampleRate*mFrameSize/1000000 + mLastBufferSize;
                        if (size > mInputBuffer->range_length()) {
                            SXLOGW("AudioPlayer: drop late buffer %lld", 
                                    mPositionTimeMediaUs);
                            mInputBuffer->release();
                            mInputBuffer = NULL;
                            continue;
                        }
                    }
					mLastBufferTimeUs = mPositionTimeMediaUs;
                }
				size_t NeedLastBufferSize = (mPositionTimeMediaUs - mLastBufferTimeUs)*mSampleRate*mFrameSize/1000000;
				if (NeedLastBufferSize - mLastBufferSize > mLastBufferSize)
				{
					SXLOGD("mPositionTimeMediaUs=%lld, mLastBufferTimeUs=%lld, mSampleRate=%d, mFrameSize=%d", mPositionTimeMediaUs, mLastBufferTimeUs, mSampleRate, mFrameSize);
					SXLOGD("NeedLastBufferSize=%d, mLastBufferSize=%lld", NeedLastBufferSize, mLastBufferSize);
					int64_t tempTimeUs;
					size_t NeedPaddingSize = NeedLastBufferSize - mLastBufferSize;
					NeedPaddingSize = NeedPaddingSize - NeedPaddingSize % mFrameSize;
					if (NeedPaddingSize < 2*1024*1024)//max padding size is 2M
					{
						MediaBuffer *tempBuf = new MediaBuffer(NeedPaddingSize + mInputBuffer->range_length());
						if (tempBuf != NULL)
						{
							memset(tempBuf->data(), 0, NeedPaddingSize);
							memcpy(((char *)tempBuf->data()) + NeedPaddingSize, 
								    ((const char*)mInputBuffer->data()) + mInputBuffer->range_offset(),
								    mInputBuffer->range_length());
							tempTimeUs = mPositionTimeMediaUs;
							mPositionTimeMediaUs = mLastBufferTimeUs + mLastBufferSize * 1000000 / (mFrameSize * mSampleRate);
							mLastBufferSize = mInputBuffer->range_length();
							mLastBufferTimeUs = tempTimeUs;
							mInputBuffer->release();
							mInputBuffer = tempBuf;
						}
						else
						{
							SXLOGW("Malloc audio pad buffer failed!");
							mLastBufferSize = mInputBuffer->range_length();
							mLastBufferTimeUs = mPositionTimeMediaUs;
						}
					}
					else
					{
						SXLOGW("Too large audio padding size(%d)!!!", NeedPaddingSize);
						mLastBufferSize = mInputBuffer->range_length();
						mLastBufferTimeUs = mPositionTimeMediaUs;
					}
				}
				else
				{
					mLastBufferSize = mInputBuffer->range_length();
					mLastBufferTimeUs = mPositionTimeMediaUs;
				}
				
			}
#endif

            mPositionTimeRealUs =
                ((mNumFramesPlayed + size_done / mFrameSize) * 1000000)
                    / mSampleRate;

            ALOGV("buffer->size() = %d, "
                 "mPositionTimeMediaUs=%.2f mPositionTimeRealUs=%.2f",
                 mInputBuffer->range_length(),
                 mPositionTimeMediaUs / 1E6, mPositionTimeRealUs / 1E6);
        }

        if (mInputBuffer->range_length() == 0) {
            mInputBuffer->release();
            mInputBuffer = NULL;

            continue;
        }

        size_t copy = size_remaining;
        if (copy > mInputBuffer->range_length()) {
            copy = mInputBuffer->range_length();
        }

        memcpy((char *)data + size_done,
               (const char *)mInputBuffer->data() + mInputBuffer->range_offset(),
               copy);

        mInputBuffer->set_range(mInputBuffer->range_offset() + copy,
                                mInputBuffer->range_length() - copy);

        size_done += copy;
        size_remaining -= copy;
    }

    {
        Mutex::Autolock autoLock(mLock);
        mNumFramesPlayed += size_done / mFrameSize;
        mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();

        if (mReachedEOS) {
            mPinnedTimeUs = mNumFramesPlayedSysTimeUs;
        } else {
            mPinnedTimeUs = -1ll;
        }
    }

    if (postEOS) {
        mObserver->postAudioEOS(postEOSDelayUs);
    }

    if (postSeekComplete) {
        mObserver->postAudioSeekComplete();
    }

    return size_done;
}

int64_t AudioPlayer::getRealTimeUs() {
    Mutex::Autolock autoLock(mLock);
    return getRealTimeUsLocked();
}

int64_t AudioPlayer::getRealTimeUsLocked() const {
    CHECK(mStarted);
    CHECK_NE(mSampleRate, 0);
    int64_t result = -mLatencyUs + (mNumFramesPlayed * 1000000) / mSampleRate;

    // Compensate for large audio buffers, updates of mNumFramesPlayed
    // are less frequent, therefore to get a "smoother" notion of time we
    // compensate using system time.
    int64_t diffUs;
    if (mPinnedTimeUs >= 0ll) {
        diffUs = mPinnedTimeUs;
    } else {
        diffUs = ALooper::GetNowUs();
    }

    diffUs -= mNumFramesPlayedSysTimeUs;
#ifdef ANDROID_DEFAULT_CODE
    return result + diffUs;
#else
    uint32_t afLatency = 0;
    if (AudioSystem::getOutputLatency(&afLatency)!=NO_ERROR)
        afLatency = 0;

    if (diffUs<0ll){
        return (mAVSyncLastRealTime==-1ll?result:mAVSyncLastRealTime);
    }else if(diffUs>((int64_t)afLatency*1000)) {
        return (mAVSyncLastRealTime==-1ll?result:mAVSyncLastRealTime);
    }
    
    if (mAVSyncLastRealTime!=-1ll && mAVSyncLastRealTime>(result+diffUs)) {
        SXLOGV("Audio Time is back!");
        return mAVSyncLastRealTime;
    }
    
    mAVSyncLastRealTime = result + diffUs;
    return mAVSyncLastRealTime;
#endif
}

int64_t AudioPlayer::getMediaTimeUs() {
    Mutex::Autolock autoLock(mLock);

    if (mPositionTimeMediaUs < 0 || mPositionTimeRealUs < 0) {
        if (mSeeking || mReachedEOS) {
            return mSeekTimeUs;
        }

        return 0;
    }

    int64_t realTimeOffset = getRealTimeUsLocked() - mPositionTimeRealUs;
    if (realTimeOffset < 0) {
        realTimeOffset = 0;
    }

    return mPositionTimeMediaUs + realTimeOffset;
}

bool AudioPlayer::getMediaTimeMapping(
        int64_t *realtime_us, int64_t *mediatime_us) {
    Mutex::Autolock autoLock(mLock);

    *realtime_us = mPositionTimeRealUs;
    *mediatime_us = mPositionTimeMediaUs;

    return mPositionTimeRealUs != -1 && mPositionTimeMediaUs != -1;
}

status_t AudioPlayer::seekTo(int64_t time_us) {
    Mutex::Autolock autoLock(mLock);

    mSeeking = true;
    mPositionTimeRealUs = mPositionTimeMediaUs = -1;
    mReachedEOS = false;
    mSeekTimeUs = time_us;

    // Flush resets the number of played frames
    mNumFramesPlayed = 0;
    mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();
#ifndef ANDROID_DEFAULT_CODE
    mAVSyncLastRealTime = -1ll;
#endif
    if (mAudioSink != NULL) {
        mAudioSink->flush();
    } else {
        mAudioTrack->flush();
    }

    return OK;
}

#ifndef ANDROID_DEFAULT_CODE
void AudioPlayer::enableAudioPad()
{
	mPadEnable = true;
}
#endif
}
