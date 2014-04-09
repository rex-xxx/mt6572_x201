/*
 * Copyright (C) 2010 The Android Open Source Project
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
#define LOG_TAG "NuPlayer"
#include <utils/Log.h>

#include "NuPlayer.h"

#include "HTTPLiveSource.h"
#include "NuPlayerDecoder.h"
#include "NuPlayerDriver.h"
#include "NuPlayerRenderer.h"
#include "NuPlayerSource.h"
#include "RTSPSource.h"
#include "StreamingSource.h"
#include "GenericSource.h"
#include "mp4/MP4Source.h"

#include "ATSParser.h"

#include <cutils/properties.h> // for property_get
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/ACodec.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MetaData.h>
#include <gui/ISurfaceTexture.h>

#include "avc_utils.h"

#include "ESDS.h"
#include <media/stagefright/Utils.h>

#ifndef ANDROID_DEFAULT_CODE
#include <media/stagefright/OMXCodec.h>
#endif

namespace android {

////////////////////////////////////////////////////////////////////////////////
#ifndef ANDROID_DEFAULT_CODE
static int64_t kRTSPEarlyEndTimeUs = 3000000ll; // 3secs
#endif // #ifndef ANDROID_DEFAULT_CODE

NuPlayer::NuPlayer()
    : mUIDValid(false),
      mVideoIsAVC(false),
      mAudioEOS(false),
      mVideoEOS(false),
      mScanSourcesPending(false),
      mScanSourcesGeneration(0),
      mPollDurationGeneration(0),
      mTimeDiscontinuityPending(false),
      mFlushingAudio(NONE),
      mFlushingVideo(NONE),
      mResetInProgress(false),
      mResetPostponed(false),
      mSkipRenderingAudioUntilMediaTimeUs(-1ll),
      mSkipRenderingVideoUntilMediaTimeUs(-1ll),
      mVideoLateByUs(0ll),
      mNumFramesTotal(0ll),
#ifndef ANDROID_DEFAULT_CODE
      mSeekTimeUs(-1),
      mPositionUs(-1),
      mPrepare(UNPREPARED),
      mPlayState(STOPPED),
      mVideoFirstRenderTimestamp(-1),
      mVideoWidth(320),
      mVideoHeight(240),
      mRenderer(NULL),
      mVideoDecoder(NULL),
      mAudioDecoder(NULL),
      mDataSourceType(SOURCE_Default),
      mFlags(0),
#endif
      mNumFramesDropped(0ll) ,
      mVideoScalingMode(NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW){
}

NuPlayer::~NuPlayer() {
    ALOGD("~NuPlayer");
}

void NuPlayer::setUID(uid_t uid) {
    mUIDValid = true;
    mUID = uid;
}

void NuPlayer::setDriver(const wp<NuPlayerDriver> &driver) {
    mDriver = driver;
}

void NuPlayer::setDataSource(const sp<IStreamSource> &source) {
    sp<AMessage> msg = new AMessage(kWhatSetDataSource, id());

    char prop[PROPERTY_VALUE_MAX];
    if (property_get("media.stagefright.use-mp4source", prop, NULL)
            && (!strcmp(prop, "1") || !strcasecmp(prop, "true"))) {
        msg->setObject("source", new MP4Source(source));
    } else {
        msg->setObject("source", new StreamingSource(source));
    }

    msg->post();
}

static bool IsHTTPLiveURL(const char *url) {
    if (!strncasecmp("http://", url, 7)
            || !strncasecmp("https://", url, 8)) {
        size_t len = strlen(url);
        if (len >= 5 && !strcasecmp(".m3u8", &url[len - 5])) {
            return true;
        }

        if (strstr(url,"m3u8")) {
            return true;
        }
    }

    return false;
}

void NuPlayer::setDataSource(
        const char *url, const KeyedVector<String8, String8> *headers) {
    sp<AMessage> msg = new AMessage(kWhatSetDataSource, id());

    sp<Source> source;
    if (IsHTTPLiveURL(url)) {
        source = new HTTPLiveSource(url, headers, mUIDValid, mUID);
#ifndef ANDROID_DEFAULT_CODE
        mDataSourceType = SOURCE_HttpLive;
#endif
    } else if (!strncasecmp(url, "rtsp://", 7)) {
        source = new RTSPSource(url, headers, mUIDValid, mUID);
#ifndef ANDROID_DEFAULT_CODE
        mDataSourceType = SOURCE_Rtsp;
    } else if (!strncasecmp("http://", url, 7)
            || !strncasecmp("https://", url, 8)) {
        size_t len = strlen(url);
        if ((len >= 5 && !strncasecmp(".smil", &url[len - 5], 5)) ||
            (len >= 4 && !strncasecmp(".sdp", &url[len - 4], 4))  ||
	    (strstr(url, ".sdp?"))) {
	    source = new RTSPSource(url, headers, mUIDValid, mUID);
	    mDataSourceType = SOURCE_Rtsp;
	}
#endif
    } else {
        source = new GenericSource(url, headers, mUIDValid, mUID);
    }
#ifndef ANDROID_DEFAULT_CODE
        sp<AMessage> notify = new AMessage(kWhatSourceNotify, id());
        source->setNotificationMessage(notify);
#endif

    msg->setObject("source", source);
    msg->post();
}

void NuPlayer::setDataSource(int fd, int64_t offset, int64_t length) {
    sp<AMessage> msg = new AMessage(kWhatSetDataSource, id());
    sp<Source> source = new GenericSource(fd, offset, length);
#ifndef ANDROID_DEFAULT_CODE
	mDataSourceType = SOURCE_Local;	

	status_t err = source->initCheck();
	if(err != OK){
		notifyListener(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
		ALOGW("setDataSource source init check fail err=%d",err);
		source = NULL;
		return;
	}
	
	sp<AMessage> format = source->getFormat(false);
	if(format.get()){
		AString newUrl;
		sp<RefBase> sdp;
		if(format->findString("rtsp-uri",&newUrl) &&
			format->findObject("rtsp-sdp",&sdp)){
			//is sdp--need re-setDataSource
			source = new RTSPSource(newUrl.c_str(),NULL, mUIDValid, mUID);
			((RTSPSource*)(source.get()))->setSDP(sdp);
			mDataSourceType = SOURCE_Rtsp;

			sp<AMessage> notify = new AMessage(kWhatSourceNotify, id());
        	source->setNotificationMessage(notify);
		}
	}	
				
#endif
    msg->setObject("source", source);
    msg->post();

}

void NuPlayer::setVideoSurfaceTexture(const sp<ISurfaceTexture> &surfaceTexture) {
    sp<AMessage> msg = new AMessage(kWhatSetVideoNativeWindow, id());

    ALOGD("setVideoSurfaceTexture(%p)", surfaceTexture.get());
    sp<SurfaceTextureClient> surfaceTextureClient(surfaceTexture != NULL ?
                new SurfaceTextureClient(surfaceTexture) : NULL);
    msg->setObject("native-window", new NativeWindowWrapper(surfaceTextureClient));
    msg->post();
}

void NuPlayer::setAudioSink(const sp<MediaPlayerBase::AudioSink> &sink) {
    sp<AMessage> msg = new AMessage(kWhatSetAudioSink, id());
    msg->setObject("sink", sink);
    msg->post();
}

void NuPlayer::start() {
    (new AMessage(kWhatStart, id()))->post();
}

void NuPlayer::pause() {
    (new AMessage(kWhatPause, id()))->post();
}

void NuPlayer::resume() {
    (new AMessage(kWhatResume, id()))->post();
}

void NuPlayer::resetAsync() {
    (new AMessage(kWhatReset, id()))->post();
}

#ifndef ANDROID_DEFAULT_CODE
void NuPlayer::prepareAsync() {
    (new AMessage(kWhatPrepare, id()))->post();
}
void NuPlayer::stop() {
    (new AMessage(kWhatStop, id()))->post();
}
#endif

void NuPlayer::seekToAsync(int64_t seekTimeUs) {
#ifndef ANDROID_DEFAULT_CODE
    CHECK(seekTimeUs != -1);
    Mutex::Autolock autoLock(mLock); 
    mSeekTimeUs = seekTimeUs;   //seek complete later
    mPositionUs = seekTimeUs;   // mtk80902: for kRTSPEarlyEndTimeUs vs seek to end 
    if (mRenderer != NULL) {
        if (mPlayState == PLAYING) {
            mRenderer->pause();
        }
    }
#endif
    sp<AMessage> msg = new AMessage(kWhatSeek, id());
    msg->setInt64("seekTimeUs", seekTimeUs);
    msg->post();
}

// static
bool NuPlayer::IsFlushingState(FlushStatus state, bool *needShutdown) {
    switch (state) {
        case FLUSHING_DECODER:
            if (needShutdown != NULL) {
                *needShutdown = false;
            }
            return true;

        case FLUSHING_DECODER_SHUTDOWN:
#ifndef ANDROID_DEFAULT_CODE
        case SHUTTING_DOWN_DECODER:
#endif
            if (needShutdown != NULL) {
                *needShutdown = true;
            }
            return true;

        default:
            return false;
    }
}

#ifndef ANDROID_DEFAULT_CODE
//static
bool NuPlayer::IsNeedFlush_WhenSeek(DataSourceType SourceType, bool *needShutdown) {
    bool needFlush = false;  //this is android default policy
    *needShutdown = false;
    if (SourceType == SOURCE_HttpLive) {
        needFlush = true;
        *needShutdown = true;
    } else if (SourceType == SOURCE_Local) {
        needFlush = true;
    } else if (SourceType == SOURCE_Rtsp) {
        needFlush = true;
	// mtk80902: add codec shutdown for rtsp
	// when streaming start with a seek, the init csd
	// will be flushed, seems MPEG4 is OK but H264 will
	// lost SPS/PPS (ALPS00448158)
//	*needShutdown = true;
    }
    return needFlush;
   
}

#endif

void NuPlayer::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatSetDataSource:
        {
            ALOGD("kWhatSetDataSource");

            CHECK(mSource == NULL);

            sp<RefBase> obj;
            CHECK(msg->findObject("source", &obj));

            mSource = static_cast<Source *>(obj.get());
            break;
        }

        case kWhatPollDuration:
        {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));

            if (generation != mPollDurationGeneration) {
                // stale
                break;
            }

            int64_t durationUs;
            if (mDriver != NULL && mSource->getDuration(&durationUs) == OK) {
                sp<NuPlayerDriver> driver = mDriver.promote();
                if (driver != NULL) {
                    driver->notifyDuration(durationUs);
                }
            }

            msg->post(1000000ll);  // poll again in a second.
            break;
        }

        case kWhatSetVideoNativeWindow:
        {
            ALOGD("kWhatSetVideoNativeWindow");

            sp<RefBase> obj;
            CHECK(msg->findObject("native-window", &obj));

            mNativeWindow = static_cast<NativeWindowWrapper *>(obj.get());
            ALOGD("\t\tnative window: %p", mNativeWindow->getNativeWindow().get());

#ifndef ANDROID_DEFAULT_CODE
            if (mVideoDecoder != NULL) {
                ALOGD("flush and shutdown decoder, reconfigure to use new surface");
                bool bWaitingFlush = flushAfterSeekIfNecessary();
                mTimeDiscontinuityPending = bWaitingFlush;
            }
#endif


            // XXX - ignore error from setVideoScalingMode for now
            setVideoScalingMode(mVideoScalingMode);
            break;
        }

        case kWhatSetAudioSink:
        {
            ALOGD("kWhatSetAudioSink");

            sp<RefBase> obj;
            CHECK(msg->findObject("sink", &obj));

            mAudioSink = static_cast<MediaPlayerBase::AudioSink *>(obj.get());
            ALOGD("\t\taudio sink: %p", mAudioSink.get());
            break;
        }
#ifndef ANDROID_DEFAULT_CODE
//#if 0  //debug: disable prepare
        case kWhatPrepare:
        {
            ALOGD("kWhatPrepare, source type = %d", (int)mDataSourceType);
            if (mPrepare == PREPARING)
                break;
            mPrepare = PREPARING;

            mVideoIsAVC = false;
            mAudioEOS = false;
            mVideoEOS = false;
            mSkipRenderingAudioUntilMediaTimeUs = -1;
            mSkipRenderingVideoUntilMediaTimeUs = -1;
            mVideoLateByUs = 0;
            mNumFramesTotal = 0;
            mNumFramesDropped = 0;

            if (mSource == NULL) {
                ALOGW("prepare error: source is not ready");
                CHECK(mDriver != NULL);
                finishPrepare(false);
                break;
            }

	    status_t err = mSource->start_withCheck();
	    if (err != OK) {
		//some source may be blocked, but will send Source::kWhatConnDone later
		if (err == -EWOULDBLOCK) {
		    break;
		}
                ALOGW("prepare error: source start fail with err %d", err);
		finishPrepare(false, err);
		break;
	    }
	    postScanSources();
	    break;
        }
        case kWhatStart:
        {
            ALOGD("kWhatStart");
            
            if (mPlayState == PLAYING) {
                break;
            }

            mRenderer = new Renderer(
                    mAudioSink,
                    new AMessage(kWhatRendererNotify, id()));

            looper()->registerHandler(mRenderer);          

            if (mDataSourceType == SOURCE_HttpLive) {    //diff-path: report-unsupport
                if ((mAudioDecoder == NULL) && (mVideoDecoder == NULL)) {
                    notifyListener(MEDIA_ERROR,  MEDIA_ERROR_TYPE_NOT_SUPPORTED, 0); 
                    break;
                }
            }
            
            if (mSource != NULL) {
                mSource->play();
            }

	    // the decoder Loaded->Executing
	    if (mAudioDecoder != NULL) {
		mAudioDecoder->initiateStart(); 
	    }
	    if (mVideoDecoder != NULL) {
		mVideoDecoder->initiateStart();
	    }

            mPlayState = PLAYING;
            break;
        }
#else
        case kWhatStart:
        {
            ALOGD("kWhatStart");

            mVideoIsAVC = false;
            mAudioEOS = false;
            mVideoEOS = false;
            mSkipRenderingAudioUntilMediaTimeUs = -1;
            mSkipRenderingVideoUntilMediaTimeUs = -1;
            mVideoLateByUs = 0;
            mNumFramesTotal = 0;
            mNumFramesDropped = 0;

            mSource->start();

            mRenderer = new Renderer(
                    mAudioSink,
                    new AMessage(kWhatRendererNotify, id()));

            looper()->registerHandler(mRenderer);

            postScanSources();
            break;
        }
#endif
        case kWhatScanSources:
        {
            ALOGD("kWhatScanSources");
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));
            if (generation != mScanSourcesGeneration) {
                // Drop obsolete msg.
                break;
            }

            mScanSourcesPending = false;
#ifndef ANDROID_DEFAULT_CODE
            bool needScanAgain = onScanSources();
            //TODO: to handle audio only file, finisPrepare should be sent
            if (needScanAgain) {     //scanning source is not completed, continue
                msg->post(100000ll);
                mScanSourcesPending = true;
            } else {
                ALOGD("scanning sources done ! haveAudio=%d, haveVideo=%d",
                        mAudioDecoder != NULL, mVideoDecoder != NULL);
                if (mPrepare == PREPARING)
                    finishPrepare(mAudioDecoder != NULL || mVideoDecoder != NULL);
            }
#else
            ALOGV("scanning sources haveAudio=%d, haveVideo=%d",
                 mAudioDecoder != NULL, mVideoDecoder != NULL);

            bool mHadAnySourcesBefore =
                (mAudioDecoder != NULL) || (mVideoDecoder != NULL);

            if (mNativeWindow != NULL) {
                instantiateDecoder(false, &mVideoDecoder);
            }

            if (mAudioSink != NULL) {
                instantiateDecoder(true, &mAudioDecoder);
            }

            if (!mHadAnySourcesBefore
                    && (mAudioDecoder != NULL || mVideoDecoder != NULL)) {
                // This is the first time we've found anything playable.

                uint32_t flags = mSource->flags();

                if (flags & Source::FLAG_DYNAMIC_DURATION) {
                    schedulePollDuration();
                }
            }

            status_t err;
            if ((err = mSource->feedMoreTSData()) != OK) {
                if (mAudioDecoder == NULL && mVideoDecoder == NULL) {
                    // We're not currently decoding anything (no audio or
                    // video tracks found) and we just ran out of input data.

                    if (err == ERROR_END_OF_STREAM) {
                        notifyListener(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                    } else {
                        notifyListener(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
                    }
                }
                break;
            }

            if ((mAudioDecoder == NULL && mAudioSink != NULL)
                    || (mVideoDecoder == NULL && mNativeWindow != NULL)) {
                msg->post(100000ll);
                mScanSourcesPending = true;
            //    ALOGV("scanning sources haveAudio=%d, haveVideo=%d",
            //       mAudioDecoder != NULL, mVideoDecoder != NULL);
            } else {
                ALOGV("scanning sources done ! haveAudio=%d, haveVideo=%d",
                    mAudioDecoder != NULL, mVideoDecoder != NULL);
            }
#endif
            break;
        }

        case kWhatVideoNotify:
        case kWhatAudioNotify:
        {
            bool audio = msg->what() == kWhatAudioNotify;

            sp<AMessage> codecRequest;
            CHECK(msg->findMessage("codec-request", &codecRequest));

            int32_t what;
            CHECK(codecRequest->findInt32("what", &what));

            if (what == ACodec::kWhatFillThisBuffer) {
                status_t err = feedDecoderInputData(
                        audio, codecRequest);

                if (err == -EWOULDBLOCK) {
                    if (mSource->feedMoreTSData() == OK) {
                        msg->post(10000ll);
                    }
                }
            } else if (what == ACodec::kWhatEOS) {
                int32_t err;
                CHECK(codecRequest->findInt32("err", &err));

                if (err == ERROR_END_OF_STREAM) {
                    ALOGD("got %s decoder EOS", audio ? "audio" : "video");
                } else {
                    ALOGD("got %s decoder EOS w/ error %d",
                         audio ? "audio" : "video",
                         err);
                }

                mRenderer->queueEOS(audio, err);
            } else if (what == ACodec::kWhatFlushCompleted) {
               bool needShutdown;

                if (audio) {
                    CHECK(IsFlushingState(mFlushingAudio, &needShutdown));
                    mFlushingAudio = FLUSHED;
                } else {
                    CHECK(IsFlushingState(mFlushingVideo, &needShutdown));
                    mFlushingVideo = FLUSHED;

                    mVideoLateByUs = 0;
                }

                ALOGD("decoder %s flush completed", audio ? "audio" : "video");

                if (needShutdown) {
                    ALOGD("initiating %s decoder shutdown",
                         audio ? "audio" : "video");

                    (audio ? mAudioDecoder : mVideoDecoder)->initiateShutdown();

                    if (audio) {
                        mFlushingAudio = SHUTTING_DOWN_DECODER;
                    } else {
                        mFlushingVideo = SHUTTING_DOWN_DECODER;
                    }
                }

                finishFlushIfPossible();
            } else if (what == ACodec::kWhatOutputFormatChanged) {
                if (audio) {
                    int32_t numChannels;
                    CHECK(codecRequest->findInt32("channel-count", &numChannels));

                    int32_t sampleRate;
                    CHECK(codecRequest->findInt32("sample-rate", &sampleRate));

                    ALOGI("Audio output format changed to %d Hz, %d channels",
                         sampleRate, numChannels);

                    mAudioSink->close();

                    audio_output_flags_t flags;
                    int64_t durationUs;
                    // FIXME: we should handle the case where the video decoder is created after
                    // we receive the format change indication. Current code will just make that
                    // we select deep buffer with video which should not be a problem as it should
                    // not prevent from keeping A/V sync.
                    if (mVideoDecoder == NULL &&
                            mSource->getDuration(&durationUs) == OK &&
                            durationUs > AUDIO_SINK_MIN_DEEP_BUFFER_DURATION_US) {
                        flags = AUDIO_OUTPUT_FLAG_DEEP_BUFFER;
                    } else {
                        flags = AUDIO_OUTPUT_FLAG_NONE;
                    }

                    int32_t channelMask;
                    if (!codecRequest->findInt32("channel-mask", &channelMask)) {
                        channelMask = CHANNEL_MASK_USE_CHANNEL_ORDER;
                    }

                    CHECK_EQ(mAudioSink->open(
                                sampleRate,
                                numChannels,
                                (audio_channel_mask_t)channelMask,
                                AUDIO_FORMAT_PCM_16_BIT,
                                8 /* bufferCount */,
                                NULL,
                                NULL,
                                flags),
                             (status_t)OK);
                    mAudioSink->start();

                    mRenderer->signalAudioSinkChanged();
                } else {
                    // video

                    int32_t width, height;
                    CHECK(codecRequest->findInt32("width", &width));
                    CHECK(codecRequest->findInt32("height", &height));
#ifndef ANDROID_DEFAULT_CODE
                    int32_t WRatio, HRatio;
                    if (!codecRequest->findInt32("width-ratio", &WRatio)) {
                        WRatio = 1;
                    }
                    if (!codecRequest->findInt32("height-ratio", &HRatio)) {
                        HRatio = 1;
                    }
                    width /= WRatio;
                    height /= HRatio;
#endif

                    int32_t cropLeft, cropTop, cropRight, cropBottom;
                    CHECK(codecRequest->findRect(
                                "crop",
                                &cropLeft, &cropTop, &cropRight, &cropBottom));

                    ALOGI("Video output format changed to %d x %d "
                         "(crop: %d x %d @ (%d, %d))",
                         width, height,
                         (cropRight - cropLeft + 1),
                         (cropBottom - cropTop + 1),
                         cropLeft, cropTop);

                    notifyListener(
                            MEDIA_SET_VIDEO_SIZE,
                            cropRight - cropLeft + 1,
                            cropBottom - cropTop + 1);
                }
            } else if (what == ACodec::kWhatShutdownCompleted) {
                ALOGD("%s shutdown completed", audio ? "audio" : "video");
                if (audio) {
                    looper()->unregisterHandler(mAudioDecoder->id());
                    mAudioDecoder.clear();

                    CHECK_EQ((int)mFlushingAudio, (int)SHUTTING_DOWN_DECODER);
                    mFlushingAudio = SHUT_DOWN;
                } else {
                    looper()->unregisterHandler(mVideoDecoder->id());
                    mVideoDecoder.clear();

                    CHECK_EQ((int)mFlushingVideo, (int)SHUTTING_DOWN_DECODER);
                    mFlushingVideo = SHUT_DOWN;
                }
                finishFlushIfPossible();
            } else if (what == ACodec::kWhatError) {
#ifndef ANDROID_DEFAULT_CODE
                if (!(IsFlushingState(audio ? mFlushingAudio : mFlushingVideo))) {
                    ALOGE("Received error from %s decoder.",
                            audio ? "audio" : "video");
                    
					sp<AMessage> msgSessionNotify;
					if (msg->findMessage("feedback-session", &msgSessionNotify)) {
					    int32_t bandwidth = 0;
					    msgSessionNotify->findInt32("bandwidth", &bandwidth);
					    ALOGD("@debug: %s decoder error happens in bandwidth = %d", 
					            audio ? "audio" : "video", bandwidth);
					    msgSessionNotify->post();

                        (audio ? mAudioDecoder : mVideoDecoder)->signalFillBufferToNul(); 
					} else {

						int32_t err;
						CHECK(codecRequest->findInt32("err", &err));
						// mtk80902: ALPS00490726 - in fact, prepare complete should
						// after acodec return onConfigureComponent done. if acodec 
						// notify error before it, we should judge wether a/v both
						// got errors.
						if (mRenderer != NULL)
						    mRenderer->queueEOS(audio, err);
						else {
						    (audio ? mAudioDecoder : mVideoDecoder)->initiateShutdown();

						    if (audio) {
						        mFlushingAudio = SHUTTING_DOWN_DECODER;
						        notifyListener(MEDIA_INFO, MEDIA_INFO_HAS_UNSUPPORT_AUDIO, 0);
							} else {
						        mFlushingVideo = SHUTTING_DOWN_DECODER;
						        notifyListener(MEDIA_INFO, MEDIA_INFO_HAS_UNSUPPORT_VIDEO, 0);
							}
						}
					
					}
                } else {
                    ALOGD("Ignore error from %s decoder when flushing", audio ? "audio" : "video");
                }
#else
                ALOGE("Received error from %s decoder, aborting playback.",
                     audio ? "audio" : "video");

                mRenderer->queueEOS(audio, UNKNOWN_ERROR);
#endif
            } 
            else if (what == ACodec::kWhatDrainThisBuffer) {
                renderBuffer(audio, codecRequest);
#ifndef ANDROID_DEFAULT_CODE
            }
            else if (what == ACodec::kWhatComponentAllocated) {
                int32_t quirks;
                CHECK(codecRequest->findInt32("quirks", &quirks));
                // mtk80902: must tell APacketSource quirks settings 
                // thru this way..
                ALOGD("Component Alloc: quirks (%u)", quirks);
                sp<MetaData> params = new MetaData;
                if (quirks & OMXCodec::kWantsNALFragments) {
                    params->setInt32(kKeyWantsNALFragments, true);
                    if (mSource != NULL)
                        mSource->setParams(params);
                }
#endif
            } else {
                ALOGD("Unhandled codec notification '%c%c%c%c' .", 
                        what>>24, (char)((what>>16) & 0xff), (char)((what>>8) & 0xff), (char)(what & 0xff));
            }

            break;
        }

        case kWhatRendererNotify:
        {
            int32_t what;
            CHECK(msg->findInt32("what", &what));

            if (what == Renderer::kWhatEOS) {
                int32_t audio;
                CHECK(msg->findInt32("audio", &audio));

                int32_t finalResult;
                CHECK(msg->findInt32("finalResult", &finalResult));

                if (audio) {
                    mAudioEOS = true;
                } else {
                    mVideoEOS = true;
                }

                if (finalResult == ERROR_END_OF_STREAM) {
                    ALOGD("reached %s EOS", audio ? "audio" : "video");
                } else {
                    ALOGE("%s track encountered an error (%d)",
                            audio ? "audio" : "video", finalResult);
#ifndef ANDROID_DEFAULT_CODE
                    // mtk80902: ALPS00436989
                    if (audio) {
                        notifyListener(MEDIA_INFO, MEDIA_INFO_HAS_UNSUPPORT_AUDIO, finalResult);
                    } else {
                        notifyListener(MEDIA_INFO, MEDIA_INFO_HAS_UNSUPPORT_VIDEO, finalResult);
                    }


#else
                    notifyListener(
                            MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, finalResult);
#endif
                }

                if ((mAudioEOS || mAudioDecoder == NULL)
                        && (mVideoEOS || mVideoDecoder == NULL)) {
#ifndef ANDROID_DEFAULT_CODE
                    // mtk80902: for ALPS00557536 - dont trigger AP's retry if it's codec's error?
                    if (finalResult == ERROR_END_OF_STREAM || finalResult == ERROR_CANNOT_CONNECT) {
                        // mtk80902: ALPS00434239 - porting from AwesomePlayer
                        int64_t durationUs;
                        if (mDataSourceType == SOURCE_Rtsp && mSource != NULL && 
                                mSource->getDuration(&durationUs) == OK && 
                                (durationUs == 0 || durationUs - mPositionUs > kRTSPEarlyEndTimeUs)) {
                            ALOGE("RTSP play end before duration %lld",	durationUs);
                            notifyListener(MEDIA_ERROR, MEDIA_ERROR_CANNOT_CONNECT_TO_SERVER, 0);
                            break;
                        }
                    }
#endif
                    notifyListener(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                }
            } else if (what == Renderer::kWhatPosition) {
                int64_t positionUs;
                CHECK(msg->findInt64("positionUs", &positionUs));
#ifndef ANDROID_DEFAULT_CODE
                // mtk80902: add earlyEndTime for rtsp
                // dont update when seeking
                if (mSeekTimeUs == -1)
                    mPositionUs = positionUs;
#endif

                CHECK(msg->findInt64("videoLateByUs", &mVideoLateByUs));

                if (mDriver != NULL) {
                    sp<NuPlayerDriver> driver = mDriver.promote();
                    if (driver != NULL) {
                        driver->notifyPosition(positionUs);
                        driver->notifyFrameStats(
                                mNumFramesTotal, mNumFramesDropped);
                    }
                }
            } else if (what == Renderer::kWhatFlushComplete) {
                CHECK_EQ(what, (int32_t)Renderer::kWhatFlushComplete);

                int32_t audio;
                CHECK(msg->findInt32("audio", &audio));

                ALOGD("renderer %s flush completed.", audio ? "audio" : "video");
            } else if (what == Renderer::kWhatVideoRenderingStart) {
                notifyListener(MEDIA_INFO, MEDIA_INFO_RENDERING_START, 0);
            }
            break;
        }
#ifndef ANDROID_DEFAULT_CODE
        case kWhatSourceNotify:
        {
            onSourceNotified(msg);
            break;
        }
	case kWhatStop:
	{
	// mtk80902: substitute of calling pause in NuPlayerDriver's stop
	// most for the rtsp
            ALOGD("kWhatStop, %d", (int32_t)mPlayState);
	    if (mPlayState == PAUSING || mPlayState == PAUSED)
		return;
	    mPlayState = PAUSED;
            CHECK(mRenderer != NULL);
            mRenderer->pause();
            break;
	}
#endif

        case kWhatMoreDataQueued:
        {
            break;
        }

        case kWhatReset:
        {
            ALOGD("kWhatReset");

            cancelPollDuration();

            if (mRenderer != NULL) {
                // There's an edge case where the renderer owns all output
                // buffers and is paused, therefore the decoder will not read
                // more input data and will never encounter the matching
                // discontinuity. To avoid this, we resume the renderer.

                if (mFlushingAudio == AWAITING_DISCONTINUITY
                        || mFlushingVideo == AWAITING_DISCONTINUITY) {
                    mRenderer->resume();
                }
            }

#ifndef ANDROID_DEFAULT_CODE
            // mtk80902: SHUT_DOWN also means not flushing
            if ((mFlushingVideo != NONE && mFlushingVideo != SHUT_DOWN) ||
                (mFlushingAudio != NONE && mFlushingAudio != SHUT_DOWN)) {
#else
            if (mFlushingAudio != NONE || mFlushingVideo != NONE) {
#endif
                // We're currently flushing, postpone the reset until that's
                // completed.

                ALOGD("postponing reset mFlushingAudio=%d, mFlushingVideo=%d",
                      mFlushingAudio, mFlushingVideo);

                mResetPostponed = true;
                break;
            }

            if (mAudioDecoder == NULL && mVideoDecoder == NULL) {
                finishReset();
                break;
            }

            mTimeDiscontinuityPending = true;

            if (mAudioDecoder != NULL) {
                flushDecoder(true /* audio */, true /* needShutdown */);
            }

            if (mVideoDecoder != NULL) {
                flushDecoder(false /* audio */, true /* needShutdown */);
            }

            mResetInProgress = true;
            break;
        }

        case kWhatSeek:
        {
            int64_t seekTimeUs;
            CHECK(msg->findInt64("seekTimeUs", &seekTimeUs));

            ALOGD("kWhatSeek seekTime(%.2f secs)", seekTimeUs / 1E6);
#ifndef ANDROID_DEFAULT_CODE
	    CHECK(mSource != NULL);
#endif
            status_t err = mSource->seekTo(seekTimeUs);
#ifndef ANDROID_DEFAULT_CODE
            if (err == -EWOULDBLOCK) {  // finish seek when receive Source::kWhatSeekDone
        		mSkipRenderingVideoUntilMediaTimeUs = seekTimeUs;
	        	mSkipRenderingAudioUntilMediaTimeUs = seekTimeUs;
                mTimeDiscontinuityPending = true;
                ALOGD("seek async, waiting Source seek done");
            } else if (err == OK) {

                bool bWaitingFlush = flushAfterSeekIfNecessary();

                mSkipRenderingAudioUntilMediaTimeUs = seekTimeUs;
                mSkipRenderingVideoUntilMediaTimeUs = seekTimeUs;
                if (bWaitingFlush) {
                    mTimeDiscontinuityPending = true;
                }
                //complete the seek until flush is done
                //if need no flush, complete seek now
                if (!bWaitingFlush) {
                    if (mRenderer != NULL) {    //resume render
                        if (mPlayState == PLAYING)
                            mRenderer->resume();
                    }
                    if (mDriver != NULL) {
                        sp<NuPlayerDriver> driver = mDriver.promote();
                        if (driver != NULL) {
                            driver->notifyPosition(mSeekTimeUs);
                            driver->notifySeekComplete();
                            ALOGI("seek(%.2f)  complete without flushed", mSeekTimeUs / 1E6);
                        }
                    }
                    mSeekTimeUs = -1;    
                } else {
                }

            } else {
                ALOGE("seek error %d", (int)err);
                // add notify seek complete
                if (mRenderer != NULL) {    //resume render
                    if (mPlayState == PLAYING)
                        mRenderer->resume();
                }
                if (mDriver != NULL) {
                    sp<NuPlayerDriver> driver = mDriver.promote();
                    if (driver != NULL) {
                        driver->notifyPosition(mSeekTimeUs);
                        driver->notifySeekComplete();
                        ALOGI("seek(%.2f)  complete without flushed", mSeekTimeUs / 1E6);
                    }
                }
                mSeekTimeUs = -1;    
            }
#else
            if (mDriver != NULL) {
                sp<NuPlayerDriver> driver = mDriver.promote();
                if (driver != NULL) {
                    driver->notifySeekComplete();
                }
            }
#endif
            break;
        }

        case kWhatPause:
        {
#ifndef ANDROID_DEFAULT_CODE
            ALOGD("kWhatPause, %d", (int32_t)mPlayState);
	    if (mPlayState == STOPPED || mPlayState == PAUSED || mPlayState == PLAYSENDING) {
		notifyListener(MEDIA_PAUSE_COMPLETE, INVALID_OPERATION, 0);
		break;
	    }
	    if (mPlayState == PAUSING) {
		notifyListener(MEDIA_PAUSE_COMPLETE, ALREADY_EXISTS, 0);
		break;
	    }
            mPlayState = PAUSING;
	    if (mSource->pause() == OK)
		mPlayState = PAUSED;
#endif
            CHECK(mRenderer != NULL);
            mRenderer->pause();
            break;
        }

        case kWhatResume:
        {
#ifndef ANDROID_DEFAULT_CODE
            ALOGD("kWhatResume, %d", (int32_t)mPlayState);
            if (mPlayState == PLAYING || mPlayState == PAUSING) {
		notifyListener(MEDIA_PLAY_COMPLETE, INVALID_OPERATION, 0);
		break;
	    }
            if (mPlayState == PLAYSENDING) {
		notifyListener(MEDIA_PLAY_COMPLETE, ALREADY_EXISTS, 0);
		break;
	    }
	// mtk80902: ALPS00451531 - bad server. response error
	// if received PLAY without range when playing
	// if play before seek complete then dont send PLAY cmd
	// just set PLAYING state
	    if (mSeekTimeUs != -1) {
		notifyListener(MEDIA_PLAY_COMPLETE, OK, 0);
		mPlayState = PLAYING;
		break;
	    }
	    mPlayState = PLAYSENDING;
	    if (mSource->play() == OK)
		mPlayState = PLAYING;
#endif
            CHECK(mRenderer != NULL);
            mRenderer->resume();
            break;
        }

        default:
            TRESPASS();
            break;
    }
}

#ifndef ANDROID_DEFAULT_CODE
bool NuPlayer::onScanSources() {
    bool needScanAgain = false;


    bool mHadAnySourcesBefore =
        (mAudioDecoder != NULL) || (mVideoDecoder != NULL);

    // mtk80902: for rtsp, if instantiateDecoder return EWOULDBLK
    // it means no track. no need to try again.
    status_t videoFmt = OK, audioFmt = OK;
    if (mNativeWindow != NULL) {
        videoFmt = instantiateDecoder(false, &mVideoDecoder);
    }

    if (mAudioSink != NULL) {
        audioFmt = instantiateDecoder(true, &mAudioDecoder);
    }

    if (!mHadAnySourcesBefore
            && (mAudioDecoder != NULL || mVideoDecoder != NULL)) {
        // This is the first time we've found anything playable.

        uint32_t flags = mSource->flags();

        if (flags & Source::FLAG_DYNAMIC_DURATION) {
            schedulePollDuration();
        }
    }

    status_t err;
    if ((err = mSource->feedMoreTSData()) != OK) {
        if (mAudioDecoder == NULL && mVideoDecoder == NULL) {
            // We're not currently decoding anything (no audio or
            // video tracks found) and we just ran out of input data.

            if (err == ERROR_END_OF_STREAM) {
                notifyListener(MEDIA_PLAYBACK_COMPLETE, 0, 0);
            } else {
                notifyListener(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
            }
        }
        return false;
    } 


    if (mDataSourceType == SOURCE_Rtsp) {
        ALOGD("audio sink: %p, audio decoder: %p, native window: %p, video decoder: %p",
                mAudioSink.get(), mAudioDecoder.get(), mNativeWindow.get(), mVideoDecoder.get());
        needScanAgain = ((mAudioSink != NULL) && (audioFmt == OK) && (mAudioDecoder == NULL))
            || ((mNativeWindow != NULL) && (videoFmt == OK) && (mVideoDecoder == NULL));
    } else if (mDataSourceType == SOURCE_HttpLive) {


	    needScanAgain = ((mAudioSink != NULL) && (mAudioDecoder == NULL))
	    	|| ((mNativeWindow != NULL) && (mVideoDecoder == NULL));

        if ((mAudioSink != NULL) && (mNativeWindow != NULL)) {
            int64_t bufferedAudio, bufferedVideo;
            mSource->getBufferedDuration(true, &bufferedAudio);
            mSource->getBufferedDuration(false, &bufferedVideo);
            ALOGD("audio buffered %.2f, video buffered %.2f", bufferedAudio / 1E6, bufferedVideo / 1E6);
            int64_t diff = bufferedVideo - bufferedAudio;
            if (diff > 5000000 || diff < -5000000) {
                needScanAgain = false;
            }
        }

    } else {
	needScanAgain = ((mAudioSink != NULL) && (mAudioDecoder == NULL))
		|| ((mNativeWindow != NULL) && (mVideoDecoder == NULL));
    }
    return needScanAgain;
}

void NuPlayer::finishPrepare(bool bSuccess, int err) {
    mPrepare = bSuccess?PREPARED:UNPREPARED;
    if (mDriver == NULL)
        return;
    sp<NuPlayerDriver> driver = mDriver.promote();
    if (driver != NULL) {
	// mtk80902: ALPS00439183
	if (bSuccess && mVideoDecoder == NULL) {
	    mVideoWidth = mVideoHeight = 0;
	    driver->notifyResolution(mVideoWidth, mVideoHeight);
	}
        driver->notifyPrepareComplete(bSuccess, err);
	if (mDataSourceType == SOURCE_Rtsp && bSuccess) 
	    notifyListener(MEDIA_INFO, MEDIA_INFO_CHECK_LIVE_STREAMING_COMPLETE, 0);
        ALOGD("complete prepare %s", bSuccess?"success":"fail");
    }
}

#endif

void NuPlayer::finishFlushIfPossible() {
#ifndef ANDROID_DEFAULT_CODE
    //If reset was postponed after one of the streams is flushed, complete it now
    if (mResetPostponed) {
        ALOGD("reset postponed, mFlushingVideo %d, mFlushingAudio %d", mFlushingVideo, mFlushingAudio);
        if ( (mAudioDecoder != NULL) &&
                (mFlushingAudio == NONE || mFlushingAudio == AWAITING_DISCONTINUITY) ) {
            flushDecoder(true /* audio */, true /* needShutdown */);
        }

        if ( (mVideoDecoder != NULL) &&
                (mFlushingVideo == NONE || mFlushingVideo == AWAITING_DISCONTINUITY) ) {
            flushDecoder(false /* video */, true /* needShutdown */);
        }
    }
    // mtk80902: ALPS00445127 - only one track's situation
    if (mAudioDecoder != NULL && mFlushingAudio != FLUSHED && mFlushingAudio != SHUT_DOWN) {
        ALOGD("not flushed, mFlushingAudio = %d", mFlushingAudio);
        return;
    }

    if (mVideoDecoder != NULL && mFlushingVideo != FLUSHED && mFlushingVideo != SHUT_DOWN) {
        ALOGD("not flushed, mFlushingVideo = %d", mFlushingVideo);
        return;
    }
#else

    if (mFlushingAudio != FLUSHED && mFlushingAudio != SHUT_DOWN) {
        ALOGD("not flushed, mFlushingAudio = %d", mFlushingAudio);
        return;
    }

    if (mFlushingVideo != FLUSHED && mFlushingVideo != SHUT_DOWN) {
        ALOGD("not flushed, mFlushingVideo = %d", mFlushingVideo);
        return;
    }
#endif

    ALOGD("both audio and video are flushed now.");

    if (mTimeDiscontinuityPending) {
    	if (mRenderer != NULL) {
	        mRenderer->signalTimeDiscontinuity();
	    } 
        mTimeDiscontinuityPending = false;
    } 

#ifndef ANDROID_DEFAULT_CODE
    if (isSeeking_l()) {
        if (mRenderer != NULL) {    //resume render
            if (mPlayState == PLAYING)
                mRenderer->resume();
        }
        if (mDriver != NULL) {
            sp<NuPlayerDriver> driver = mDriver.promote();
            if (driver != NULL) {
                driver->notifyPosition(mSeekTimeUs);
                driver->notifySeekComplete();
                ALOGI("seek(%.2f)  complete", mSeekTimeUs / 1E6);
            }
        }
        mSeekTimeUs = -1;
        // mtk80902: ALPS00445336
        // sometimes server's play resp is inaccurate and
        // if we insist on our seek pos, drop all data before it
        // player will buffering twice or more.
        mSkipRenderingVideoUntilMediaTimeUs = -1;
        mSkipRenderingAudioUntilMediaTimeUs = -1;
    }
#endif

    if (mAudioDecoder != NULL) {
        mAudioDecoder->signalResume();
    }

    if (mVideoDecoder != NULL) {
        mVideoDecoder->signalResume();
    }

    mFlushingAudio = NONE;
    mFlushingVideo = NONE;

    if (mResetInProgress) {
        ALOGD("reset completed");

        mResetInProgress = false;
        finishReset();
    } else if (mResetPostponed) {
        (new AMessage(kWhatReset, id()))->post();
        mResetPostponed = false;
    } else if (mAudioDecoder == NULL || mVideoDecoder == NULL) {
        ALOGD("Start scanning source after shutdown");
        postScanSources();
    }
}

void NuPlayer::finishReset() {
    CHECK(mAudioDecoder == NULL);
    CHECK(mVideoDecoder == NULL);

#ifndef ANDROID_DEFAULT_CODE
    mPlayState = STOPPED;
#endif
    ++mScanSourcesGeneration;
    mScanSourcesPending = false;

    if (mRenderer != NULL) {
        looper()->unregisterHandler(mRenderer->id());
        mRenderer.clear();
        mRenderer = NULL;
    }

    if (mSource != NULL) {
        mSource->stop();
        mSource.clear();
    }

    if (mDriver != NULL) {
        sp<NuPlayerDriver> driver = mDriver.promote();
        if (driver != NULL) {
            driver->notifyResetComplete();
        }
    }
}

void NuPlayer::postScanSources() {
    if (mScanSourcesPending) {
        return;
    }

    sp<AMessage> msg = new AMessage(kWhatScanSources, id());
    msg->setInt32("generation", mScanSourcesGeneration);
    msg->post();

    mScanSourcesPending = true;
}

status_t NuPlayer::instantiateDecoder(bool audio, sp<Decoder> *decoder) {
    if (*decoder != NULL) {
#ifndef ANDROID_DEFAULT_CODE
    ALOGD("%s decoder not NULL!", audio?"audio":"video");
#endif
        return OK;
    }

    sp<AMessage> format = mSource->getFormat(audio);

    if (format == NULL) {
#ifndef ANDROID_DEFAULT_CODE
    ALOGD("%s format is NULL!", audio?"audio":"video");
#endif
        return -EWOULDBLOCK;
    }

#ifndef ANDROID_DEFAULT_CODE
    if(!audio)
    {
        CHECK(format->findInt32("width", &mVideoWidth));
        CHECK(format->findInt32("height", &mVideoHeight));
        if (mDriver != NULL)
        {
            sp<NuPlayerDriver> driver = mDriver.promote();
            if (driver != NULL) 
                driver->notifyResolution(mVideoWidth, mVideoHeight);
        }

        ALOGD("instantiate Video decoder.");
    }

#endif

    if (!audio) {
        AString mime;
        CHECK(format->findString("mime", &mime));
        mVideoIsAVC = !strcasecmp(MEDIA_MIMETYPE_VIDEO_AVC, mime.c_str());
    }

    sp<AMessage> notify =
        new AMessage(audio ? kWhatAudioNotify : kWhatVideoNotify,
                     id());
#ifndef ANDROID_DEFAULT_CODE
    sp<AMessage> msg;
    if (format->findMessage("feedback-session", &msg)) {
        sp<AMessage> msgSessionNotify = msg->dup();
        notify->setMessage("feedback-session", msgSessionNotify);
    }
#endif

    *decoder = audio ? new Decoder(notify) :
                       new Decoder(notify, mNativeWindow);
    looper()->registerHandler(*decoder);

#ifndef ANDROID_DEFAULT_CODE
    CHECK(mFlushingAudio == NONE || mFlushingAudio == SHUT_DOWN); 
    CHECK(mFlushingAudio == NONE || mFlushingVideo == SHUT_DOWN); 
    (*decoder)->configure(format, (mPlayState == PLAYING || mPlayState == PAUSED));
    ALOGD("@debug: config decoder when mPlayState = %d", (int)mPlayState);
    //if preparing, don't let decoder autorun
#else
    (*decoder)->configure(format);
#endif

    int64_t durationUs;
    if (mDriver != NULL && mSource->getDuration(&durationUs) == OK) {
        sp<NuPlayerDriver> driver = mDriver.promote();
        if (driver != NULL) {
            driver->notifyDuration(durationUs);
        }
    }

    return OK;
}

status_t NuPlayer::feedDecoderInputData(bool audio, const sp<AMessage> &msg) {
    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    if ((audio && IsFlushingState(mFlushingAudio))
            || (!audio && IsFlushingState(mFlushingVideo))) {
#ifndef ANDROID_DEFAULT_CODE
        ALOGD("feed Decoder: %s is flushing.", audio?"audio":"video");
#endif
        reply->setInt32("err", INFO_DISCONTINUITY);
        reply->post();
        return OK;
    }

    sp<ABuffer> accessUnit;

    bool dropAccessUnit;
    do {
        status_t err = mSource->dequeueAccessUnit(audio, &accessUnit);

        if (err == -EWOULDBLOCK) {
            return err;
        } else if (err != OK) {
            if (err == INFO_DISCONTINUITY) {
                int32_t type;
                CHECK(accessUnit->meta()->findInt32("discontinuity", &type));
#ifndef ANDROID_DEFAULT_CODE
                if (type == ATSParser::DISCONTINUITY_HTTPLIVE_SEEK) {
                    //TODO: to get seekTime
                    sp<AMessage> extra;
                    if (accessUnit->meta()->findMessage("extra", &extra)
                            && extra != NULL) {

                    }
                    ALOGD("@debug: find discontinuity because http live seek");
                    return -EWOULDBLOCK;                 
                }
#endif

                bool formatChange =
                    (audio &&
                     (type & ATSParser::DISCONTINUITY_AUDIO_FORMAT))
                    || (!audio &&
                            (type & ATSParser::DISCONTINUITY_VIDEO_FORMAT));



                bool timeChange = (type & ATSParser::DISCONTINUITY_TIME) != 0;

                ALOGI("%s discontinuity (formatChange=%d, time=%d)",
                     audio ? "audio" : "video", formatChange, timeChange);

                if (audio) {
                    mSkipRenderingAudioUntilMediaTimeUs = -1;
                } else {
                    mSkipRenderingVideoUntilMediaTimeUs = -1;
                }

                if (timeChange) {
                    sp<AMessage> extra;
                    if (accessUnit->meta()->findMessage("extra", &extra)
                            && extra != NULL) {
                        int64_t resumeAtMediaTimeUs;
                        if (extra->findInt64(
                                    "resume-at-mediatimeUs", &resumeAtMediaTimeUs)) {
                            ALOGI("suppressing rendering of %s until %lld us",
                                    audio ? "audio" : "video", resumeAtMediaTimeUs);

                            if (audio) {
                                mSkipRenderingAudioUntilMediaTimeUs =
                                    resumeAtMediaTimeUs;
                            } else {
                                mSkipRenderingVideoUntilMediaTimeUs =
                                    resumeAtMediaTimeUs;
                            }
                        }
                    }
                }

                mTimeDiscontinuityPending =
                    mTimeDiscontinuityPending || timeChange;

                if (formatChange || timeChange) {
                     
                    ALOGD("flush decoder, formatChange = %d", formatChange);
                    flushDecoder(audio, formatChange);
                } else {
                    // This stream is unaffected by the discontinuity

                    if (audio) {
                        mFlushingAudio = FLUSHED;
                    } else {
                        mFlushingVideo = FLUSHED;
                    }

                    finishFlushIfPossible();

                    return -EWOULDBLOCK;
                }
            }

            reply->setInt32("err", err);
            reply->post();
            return OK;
        }

        if (!audio) {
            ++mNumFramesTotal;
        }

        dropAccessUnit = false;
        if (!audio
                && mVideoLateByUs > 100000ll
                && mVideoIsAVC
                && !IsAVCReferenceFrame(accessUnit)) {
            ALOGD("drop %lld / %lld", mNumFramesDropped, mNumFramesTotal);
            dropAccessUnit = true;
            ++mNumFramesDropped;
        }
    } while (dropAccessUnit);

    // ALOGV("returned a valid buffer of %s data", audio ? "audio" : "video");

#if 0
    int64_t mediaTimeUs;
    CHECK(accessUnit->meta()->findInt64("timeUs", &mediaTimeUs));
    ALOGV("feeding %s input buffer at media time %.2f secs",
         audio ? "audio" : "video",
         mediaTimeUs / 1E6);
#endif

    reply->setBuffer("buffer", accessUnit);
    reply->post();

    return OK;
}

void NuPlayer::renderBuffer(bool audio, const sp<AMessage> &msg) {
    // ALOGV("renderBuffer %s", audio ? "audio" : "video");

    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));
#ifndef ANDROID_DEFAULT_CODE
    {
        Mutex::Autolock autoLock(mLock);
        if (mSeekTimeUs != -1) {
            sp<ABuffer> buffer0;
            CHECK(msg->findBuffer("buffer", &buffer0));
            int64_t mediaTimeUs;
            CHECK(buffer0->meta()->findInt64("timeUs", &mediaTimeUs));
            ALOGD("seeking, %s buffer (%.2f) drop", 
                    audio ? "audio" : "video", mediaTimeUs / 1E6);
            reply->post();
            return;
        }
#if 0
        sp<ABuffer> buffer0;
        CHECK(msg->findBuffer("buffer", &buffer0));
        int64_t mediaTimeUs;
        CHECK(buffer0->meta()->findInt64("timeUs", &mediaTimeUs));
        ALOGD("render %s buffer (%.2f) ", 
                audio ? "audio" : "video", mediaTimeUs / 1E6);
#endif

    }
#endif
    if (IsFlushingState(audio ? mFlushingAudio : mFlushingVideo)) {
        // We're currently attempting to flush the decoder, in order
        // to complete this, the decoder wants all its buffers back,
        // so we don't want any output buffers it sent us (from before
        // we initiated the flush) to be stuck in the renderer's queue.

        ALOGD("we're still flushing the %s decoder, sending its output buffer"
             " right back.", audio ? "audio" : "video");
#if 0
        sp<ABuffer> buffer0;
        CHECK(msg->findBuffer("buffer", &buffer0));
        int64_t mediaTimeUs;
        CHECK(buffer0->meta()->findInt64("timeUs", &mediaTimeUs));
        ALOGD("\t\t buffer (%.2f) drop", mediaTimeUs / 1E6);

#endif

        reply->post();
        return;
    }

    sp<ABuffer> buffer;
    CHECK(msg->findBuffer("buffer", &buffer));

    int64_t &skipUntilMediaTimeUs =
        audio
            ? mSkipRenderingAudioUntilMediaTimeUs
            : mSkipRenderingVideoUntilMediaTimeUs;

#ifndef ANDROID_DEFAULT_CODE

    if ((skipUntilMediaTimeUs) >= 0) {
        int64_t mediaTimeUs;
        CHECK(buffer->meta()->findInt64("timeUs", &mediaTimeUs));

        if (mediaTimeUs < skipUntilMediaTimeUs) {
            ALOGD("dropping %s buffer at time %.2f s as requested(skipUntil %.2f).",
                 audio ? "audio" : "video",
                 mediaTimeUs / 1E6, skipUntilMediaTimeUs / 1E6);

            reply->post();
            return;
        }
        ALOGI("mediaTime > skipUntilMediaTimeUs ,skip done.mediaTimeUs = %.2f s, skiptime = %.2f", 
                                                         mediaTimeUs / 1E6, skipUntilMediaTimeUs / 1E6);
        skipUntilMediaTimeUs = -1;
    }
	
#else

    if (skipUntilMediaTimeUs >= 0) {
        int64_t mediaTimeUs;
        CHECK(buffer->meta()->findInt64("timeUs", &mediaTimeUs));

        if (mediaTimeUs < skipUntilMediaTimeUs) {
            ALOGV("dropping %s buffer at time %lld as requested.",
                 audio ? "audio" : "video",
                 mediaTimeUs);

            reply->post();
            return;
        }

        skipUntilMediaTimeUs = -1;
    }
#endif	

#ifndef ANDROID_DEFAULT_CODE
    if(audio)
    {
        int64_t mediaTimeUs;
        CHECK(buffer->meta()->findInt64("timeUs", &mediaTimeUs));
	 if (mediaTimeUs < mVideoFirstRenderTimestamp)
	 {
             ALOGE("Nuplayer::render audio is early than video drop  audio tp = %.2f sec, video first TP = %.2f sec", mediaTimeUs / 1E6, mVideoFirstRenderTimestamp / 1E6);
	      reply->post();
	      return;
	 }
    }

	int32_t flags;
	CHECK(msg->findInt32("flags", &flags));
	buffer->meta()->setInt32("flags", flags);
#endif

    mRenderer->queueBuffer(audio, buffer, reply);
}

void NuPlayer::notifyListener(int msg, int ext1, int ext2) {
    if (mDriver == NULL) {
        return;
    }

    sp<NuPlayerDriver> driver = mDriver.promote();

    if (driver == NULL) {
        return;
    }

#ifndef ANDROID_DEFAULT_CODE
    //try to report a more meaningful error
    if (msg == MEDIA_ERROR && ext1 == MEDIA_ERROR_UNKNOWN) {
        switch(ext2) {
            case ERROR_MALFORMED:
                ext1 = MEDIA_ERROR_BAD_FILE;
                break;
            case ERROR_CANNOT_CONNECT:
                ext1 = MEDIA_ERROR_CANNOT_CONNECT_TO_SERVER;
                break;
            case ERROR_UNSUPPORTED:
                ext1 = MEDIA_ERROR_TYPE_NOT_SUPPORTED;
                break;
            case ERROR_FORBIDDEN:
                ext1 = MEDIA_ERROR_INVALID_CONNECTION;
                break;
        }
    }

#endif
    driver->notifyListener(msg, ext1, ext2);
}

void NuPlayer::flushDecoder(bool audio, bool needShutdown) {
    ALOGD("++flushDecoder[%s], mFlushing %d, %d", audio?"audio":"video", mFlushingAudio, mFlushingVideo);
    if ((audio && mAudioDecoder == NULL) || (!audio && mVideoDecoder == NULL)) {
        ALOGI("flushDecoder %s without decoder present",
             audio ? "audio" : "video");
    }

    // Make sure we don't continue to scan sources until we finish flushing.
    ++mScanSourcesGeneration;
    mScanSourcesPending = false;

    (audio ? mAudioDecoder : mVideoDecoder)->signalFlush();
#ifndef ANDROID_DEFAULT_CODE
    if (mRenderer != NULL)
#endif
    mRenderer->flush(audio);

    FlushStatus newStatus =
        needShutdown ? FLUSHING_DECODER_SHUTDOWN : FLUSHING_DECODER;

    if (audio) {
        CHECK(mFlushingAudio == NONE
                || mFlushingAudio == AWAITING_DISCONTINUITY);

        mFlushingAudio = newStatus;

        if (mFlushingVideo == NONE) {
            mFlushingVideo = (mVideoDecoder != NULL)
                ? AWAITING_DISCONTINUITY
                : FLUSHED;
        }
    } else {
        CHECK(mFlushingVideo == NONE
                || mFlushingVideo == AWAITING_DISCONTINUITY);

        mFlushingVideo = newStatus;

#ifndef ANDROID_DEFAULT_CODE
                    mVideoFirstRenderTimestamp = -1;
#endif
 
        if (mFlushingAudio == NONE) {
            mFlushingAudio = (mAudioDecoder != NULL)
                ? AWAITING_DISCONTINUITY
                : FLUSHED;
        }
    }
    ALOGD("--flushDecoder[%s] end, mFlushing %d, %d", audio?"audio":"video", mFlushingAudio, mFlushingVideo);
}


sp<AMessage> NuPlayer::Source::getFormat(bool audio) {
    sp<MetaData> meta = getFormatMeta(audio);

    if (meta == NULL) {
        return NULL;
    }

    sp<AMessage> msg = new AMessage;

    if(convertMetaDataToMessage(meta, &msg) == OK) {
        return msg;
    }
    return NULL;
}

status_t NuPlayer::setVideoScalingMode(int32_t mode) {
    mVideoScalingMode = mode;
    if (mNativeWindow != NULL
            && mNativeWindow->getNativeWindow() != NULL) {
        status_t ret = native_window_set_scaling_mode(
                mNativeWindow->getNativeWindow().get(), mVideoScalingMode);
        if (ret != OK) {
            ALOGE("Failed to set scaling mode (%d): %s",
                -ret, strerror(-ret));
            return ret;
        }
    }
    return OK;
}

void NuPlayer::schedulePollDuration() {
    sp<AMessage> msg = new AMessage(kWhatPollDuration, id());
    msg->setInt32("generation", mPollDurationGeneration);
    msg->post();
}

void NuPlayer::cancelPollDuration() {
    ++mPollDurationGeneration;
}

#ifndef ANDROID_DEFAULT_CODE
void NuPlayer::onSourceNotified(const sp<AMessage> &msg) {
    CHECK(msg->what() == kWhatSourceNotify);
    ALOGV("messsage from source");
    int32_t what;
    CHECK(msg->findInt32("what", &what));
    switch (what) {
        case NuPlayer::Source::kWhatBufferNotify: 
        {
            int32_t rate;
            CHECK(msg->findInt32("bufRate", &rate));
            ALOGV("mFlags %d; mPlayState %d, buffering rate %d", 
		mFlags, mPlayState, rate);
	// mtk80902: ALPS00436540
	// porting flags from AwesomePlayer - buffering can be interrupted
	// by pause - send by plugging out earphone.
	// here rate <= 0 to set CACHE_UNDERRUN:
	// 1 reduce buffering 0% notify
	// 2 the first buffering has no rate with -1.. see RTSPSource.
	    if (!(mFlags & CACHE_UNDERRUN) && rate <= 0) { 
		mFlags |= CACHE_UNDERRUN;
		if (mPlayState == PLAYING) {
		    notifyListener(MEDIA_INFO, MEDIA_INFO_BUFFERING_START, 0);
		    if (mRenderer != NULL)
			mRenderer->pause();
		}
	    } else if ((mFlags & CACHE_UNDERRUN) && rate > 100) {
		if (mPlayState == PLAYING) {
		    notifyListener(MEDIA_BUFFERING_UPDATE, 100, 0);
		    notifyListener(MEDIA_INFO, MEDIA_INFO_BUFFERING_END, 0);
		    if (mRenderer != NULL)
			mRenderer->resume();
		}
		mFlags &= ~CACHE_UNDERRUN;
	    } else if (mPlayState == PLAYING && (mFlags & CACHE_UNDERRUN)) {
		notifyListener(MEDIA_BUFFERING_UPDATE, rate, 0);
	    }
            break;
        }
	case NuPlayer::Source::kWhatConnDone:
	{
        if (mPrepare == PREPARED) //TODO: this would would happen when MyHandler disconnect
            break;
	// mtk80902: mPrepare maybe UNPREPARED
	// if reset quickly after prepare. 
	//	CHECK_EQ((int)mPrepare, (int)PREPARING);
	    int32_t ret;
	    CHECK(msg->findInt32("result", &ret));
	    ALOGV("connect return: %d", ret);
	    if (ret == OK)
		postScanSources();
	    else {
                finishPrepare(false, ret);
	    }
	    break;
	}
        case NuPlayer::Source::kWhatSeekDone:
    {
        bool bWaitingFlush = flushAfterSeekIfNecessary();
            if (!bWaitingFlush && mSeekTimeUs != -1) {
                // restore default
                // result = ok means this seek has discontinuty
                // and should be completed by flush, otherwise
                // it's interrupted before send play, and should
                // be done here.
                //	int32_t ret;
                //	CHECK(msg->findInt32("result", &ret));
                //	if (ret != EINPROGRESS) {
	    if (mRenderer != NULL) {  //resume render
		if (mPlayState == PLAYING)
		    mRenderer->resume();
	    }
            if (mDriver != NULL) {
                sp<NuPlayerDriver> driver = mDriver.promote();
                if (driver != NULL) {
                    driver->notifyPosition(mSeekTimeUs);
                    driver->notifySeekComplete();
                    ALOGI("seek(%.2f)  complete without flushed", mSeekTimeUs / 1E6);
                }
            }
            mSeekTimeUs = -1;    
                mSkipRenderingVideoUntilMediaTimeUs = -1;
                mSkipRenderingAudioUntilMediaTimeUs = -1;
        } 

        break;
    }
	case NuPlayer::Source::kWhatPlayDone:
	{
	    int32_t ret;
	    CHECK(msg->findInt32("result", &ret));
	    notifyListener(MEDIA_PLAY_COMPLETE, ret, 0);
	    mPlayState = PLAYING;
	// mtk80902: ALPS00439792
	// special case: pause -> seek -> resume ->
	//  seek complete -> resume complete
	// in this case render cant resume in SeekDone
	    if (mRenderer != NULL)
		mRenderer->resume();
	    break;
	}
	case NuPlayer::Source::kWhatPauseDone:
	{
	    int32_t ret;
	    CHECK(msg->findInt32("result", &ret));
	    if(mPlayState != PAUSING)
		break;	
	    notifyListener(MEDIA_PAUSE_COMPLETE, ret, 0);
	    mPlayState = PAUSED;
	    if (mFlags & CACHE_UNDERRUN) {
		notifyListener(MEDIA_BUFFERING_UPDATE, 100, 0);
		notifyListener(MEDIA_INFO, MEDIA_INFO_BUFFERING_END, 0);
		mFlags &= ~CACHE_UNDERRUN;
	    }
	    break;
	}        

        default:
            TRESPASS();
            break;
    } 
}

bool NuPlayer::flushAfterSeekIfNecessary() {
    bool bWaitingFlush = false;
    bool needShutdown = false;
    bool needFlush = IsNeedFlush_WhenSeek(mDataSourceType, &needShutdown);
    if (needFlush) {
        if (mAudioDecoder == NULL) {
            ALOGD("audio is not there, reset the flush flag");
            mFlushingAudio = NONE;
        } else if ( mFlushingAudio == NONE || mFlushingAudio == AWAITING_DISCONTINUITY)  {
            flushDecoder(true /* audio */, needShutdown);
            bWaitingFlush = true;
        } else {
            //TODO: if there is many discontinuity, flush still is needed
            ALOGD("audio is already being flushed");
        }

        if (mVideoDecoder == NULL) {
            ALOGD("video is not there, reset the flush flag");
            mFlushingVideo = NONE;
        } else if (mFlushingVideo == NONE || mFlushingVideo == AWAITING_DISCONTINUITY) {
            flushDecoder(false /* video */, needShutdown);
            bWaitingFlush = true;
        } else {
            //TODO: if there is many discontinuity, flush still is needed
            ALOGD("video is already being flushed");
        }

    }

    return bWaitingFlush;

}

sp<MetaData> NuPlayer::getMetaData() const {
	return mSource->getMetaData();
}



#endif

}  // namespace android
