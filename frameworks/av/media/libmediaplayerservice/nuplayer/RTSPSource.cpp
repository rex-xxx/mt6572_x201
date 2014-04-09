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
#define LOG_TAG "NuPlayer/RTSPSource"
#include <utils/Log.h>

#include "RTSPSource.h"

#include "AnotherPacketSource.h"
#include "MyHandler.h"

#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#ifndef ANDROID_DEFAULT_CODE
#include "GenericSource.h"
#include <ASessionDescription.h>
//for bitrate-adaptation
static int kWholeBufSize = 40000000; //40Mbytes
static int kTargetTime = 2000;  //ms
//Redefine to avoid overrided by other headers
#define LOG_TAG "NuPlayer/RTSPSource"

#endif

namespace android {

    // Defaultly, we're going to buffer at least 5 secs worth data on all tracks before
    // starting playback (both at startup and after a seek).
    //This value can be updated by AP-pass-in param
    static const int64_t kMinDurationUs =   5000000ll;
    static const int64_t kHighWaterMarkUs = 5000000ll;   //5 secs
    static const int64_t kLowWaterMarkUs =  1000000ll;     //1 secs

NuPlayer::RTSPSource::RTSPSource(
        const char *url,
        const KeyedVector<String8, String8> *headers,
        bool uidValid,
        uid_t uid)
    : mURL(url),
      mUIDValid(uidValid),
      mUID(uid),
      mFlags(0),
      mState(DISCONNECTED),
      mFinalResult(OK),
      mDisconnectReplyID(0),
      mStartingUp(true),
#ifndef ANDROID_DEFAULT_CODE
      mFinalResult2(OK),
      mHungry(true),
      mLastSeekCompletedTimeUs(-1),
      mSyncCallResult(OK),
      mSyncCallDone(false),
      mPlayAsResume(false),
      mIsServerPlaying(false),
      mHighWaterMarkUs(kHighWaterMarkUs),
#endif
      mSeekGeneration(0) {
    if (headers) {
#ifndef ANDROID_DEFAULT_CODE
		ALOGD("RTSP uri headers from AP:\n");
		for (int i = 0; i < headers->size(); i ++) {
			ALOGD("\t\t%s: %s", headers->keyAt(i).string(), headers->valueAt(i).string());
        } 
#endif		
        mExtraHeaders = *headers;

        ssize_t index =
            mExtraHeaders.indexOfKey(String8("x-hide-urls-from-log"));

        if (index >= 0) {
            mFlags |= kFlagIncognito;

            mExtraHeaders.removeItemsAt(index);
        }
    }
#ifndef ANDROID_DEFAULT_CODE
	//for bitrate adaptation
	m_BufQueSize = kWholeBufSize; //Whole Buffer queue size 
	m_TargetTime = kTargetTime;  // target protected time of buffer queue duration for interrupt-free playback 


	//parse rtsp buffering size from headers and remove useless headers
	//porting from AwesomePlayer
	String8 cacheSize;
	if (removeSpecificHeaders(String8("MTK-RTSP-CACHE-SIZE"), &mExtraHeaders, &cacheSize)) {
		mHighWaterMarkUs = atoi(cacheSize.string()) * 1000000ll;
	}
	ALOGI("RTSP cache size = %lld", mHighWaterMarkUs);
	removeSpecificHeaders(String8("MTK-HTTP-CACHE-SIZE"), &mExtraHeaders, &cacheSize);
	
#endif

}


NuPlayer::RTSPSource::~RTSPSource() {
#ifndef ANDROID_DEFAULT_CODE
    if (mHandlerLooper != NULL) {
	mHandlerLooper->stop();
    }
#endif
    if (mLooper != NULL) {
        mLooper->stop();
    }
}

void NuPlayer::RTSPSource::start() {
    ALOGV("start");
    if (mLooper == NULL) {
        mLooper = new ALooper;
        mLooper->setName("rtsp");
        mLooper->start();

        mReflector = new AHandlerReflector<RTSPSource>(this);
        mLooper->registerHandler(mReflector);
    }

    CHECK(mHandler == NULL);

    sp<AMessage> notify = new AMessage(kWhatNotify, mReflector->id());

    mHandler = new MyHandler(mURL.c_str(), notify, mUIDValid, mUID);
#ifndef ANDROID_DEFAULT_CODE
    if (msdp != NULL) {
	ALOGI("start,is sdp,mURL=%s",mURL.c_str());
	sp<ASessionDescription> sdp = (ASessionDescription*)msdp.get();
        status_t err = mHandler->setSessionDesc(sdp);
        if (err != OK)
            return;
    } 
// mtk80902: ALPS00450314 - min & max port
// pass into MyHandler
    mHandler->parseHeaders(&mExtraHeaders);

    //for bitrate adaptation
    //because myhandler need this info during setup, but Anotherpacket source will not be created until connect done
    //so myhandler can't get this buffer info from anotherpacketsource just like apacketsource
    //but myhandler should keep the same value with anotherpacketsource, so put the value setting here
    mHandler->setBufQueSize(m_BufQueSize);
    mHandler->setTargetTimeUs(m_TargetTime);
    // mtk80902: standalone looper for MyHandler
    if (mHandlerLooper == NULL) {
		mHandlerLooper = new ALooper;
		mHandlerLooper->setName("rtsp_handler");
		mHandlerLooper->start();
    }
    mHandlerLooper->registerHandler(mHandler);
#else
    mLooper->registerHandler(mHandler);
#endif

    CHECK_EQ(mState, (int)DISCONNECTED);
    mState = CONNECTING;

    mHandler->connect();
}

#ifndef ANDROID_DEFAULT_CODE
status_t NuPlayer::RTSPSource::start_withCheck() {
    if (!strncasecmp("http://", mURL.c_str(), 7) || 
		!strncasecmp("https://", mURL.c_str(), 8)) { 
		ALOGD("sdp over http. connect/sniff in RTSPSource now."); // sdp url
		sp<Source> source = new GenericSource(mURL.c_str(), &mExtraHeaders, mUIDValid, mUID);
		status_t err = source->initCheck();
		if (err != OK){
			ALOGW("sdp over http: connect/sniff fail err=%d",err);
			return err;
		}

		sp<AMessage> format = source->getFormat(false);
		if (format == NULL)
			return UNKNOWN_ERROR;
		AString newUrl;
		sp<RefBase> sdp;
		if (format->findString("rtsp-uri",&newUrl) &&
			format->findObject("rtsp-sdp",&sdp)) {
				//is sdp--need re-setDataSource
			setSDP(sdp);
			mURL = newUrl;
		} else {
			ALOGW("sdp over http: parse sdp fail.");
			return UNKNOWN_ERROR;
		}
    }
    start();
    return -EWOULDBLOCK;
}
#endif

void NuPlayer::RTSPSource::stop() {
#ifndef ANDROID_DEFAULT_CODE
// mtk80902: if sdp over http failed before start..
    if (mReflector == NULL)
	return;
#endif
    sp<AMessage> msg = new AMessage(kWhatDisconnect, mReflector->id());

    sp<AMessage> dummy;
    msg->postAndAwaitResponse(&dummy);
}

#ifndef ANDROID_DEFAULT_CODE
status_t NuPlayer::RTSPSource::play() {
    ALOGV("play");
    sp<AMessage> msg = new AMessage(kWhatSendPlay, mReflector->id());

    msg->post();
    return -EWOULDBLOCK;
}

status_t NuPlayer::RTSPSource::pause() {
    ALOGV("pause");
    sp<AMessage> msg = new AMessage(kWhatSendPause, mReflector->id());

    msg->post();
    return -EWOULDBLOCK;
}
#endif

status_t NuPlayer::RTSPSource::feedMoreTSData() {
    return mFinalResult;
}

sp<MetaData> NuPlayer::RTSPSource::getFormatMeta(bool audio) {
    sp<AnotherPacketSource> source = getSource(audio);

    if (source == NULL) {
        return NULL;
    }
#ifndef  ANDROID_DEFAULT_CODE 
//avoid codec consume data so fast which will cause double bufferring 
    sp<MetaData> meta = source->getFormat();
    if (audio) {
		meta->setInt32(kKeyInputBufferNum, 1);
    } else {
		meta->setInt32(kKeyRTSPSeekMode, 1);
		meta->setInt32(kKeyMaxQueueBuffer, 1);
		meta->setInt32(kKeyInputBufferNum, 4);
    }
    return meta;
#else
    return source->getFormat();
#endif
}

bool NuPlayer::RTSPSource::haveSufficientDataOnAllTracks() {
#ifndef ANDROID_DEFAULT_CODE
    return !mHungry;
#else

    status_t err;
    int64_t durationUs;
    if (mAudioTrack != NULL
            && (durationUs = mAudioTrack->getBufferedDurationUs(&err))
                    < kMinDurationUs
            && err == OK) {
        ALOGV("audio track doesn't have enough data yet. (%.2f secs buffered)",
              durationUs / 1E6);
        return false;
    }

    if (mVideoTrack != NULL
            && (durationUs = mVideoTrack->getBufferedDurationUs(&err))
                    < kMinDurationUs
            && err == OK) {
        ALOGV("video track doesn't have enough data yet. (%.2f secs buffered)",
              durationUs / 1E6);
        return false;
    }

    return true;
#endif
}

status_t NuPlayer::RTSPSource::dequeueAccessUnit(
        bool audio, sp<ABuffer> *accessUnit) {
#ifndef ANDROID_DEFAULT_CODE
    // restore default - add seeking judgement here
    // read discontinuity is very important, we dont 
    // want hungry state interrupt it.
    if (mHungry && mIsServerPlaying) {
        // ALOGD("%s RTSPSource hungry!", audio?"audio":"video");
        return -EWOULDBLOCK;
    }
#else
    if (mStartingUp) {
        if (!haveSufficientDataOnAllTracks()) {
            return -EWOULDBLOCK;
        }

        mStartingUp = false;
    }
#endif

    sp<AnotherPacketSource> source = getSource(audio);

    if (source == NULL) {
#ifndef ANDROID_DEFAULT_CODE
        ALOGD("%s source is null!", audio?"audio":"video");
#endif
        return -EWOULDBLOCK;
    }

#ifndef ANDROID_DEFAULT_CODE
    // mtk80902: ALPS00447701
    // error occurs while still receiving data
    if (mFinalResult != OK) {
        ALOGD("%s RTSPSource mFinalResult = %d", 
                audio?"audio":"video", mFinalResult);
		return mFinalResult;
    }
#endif
    status_t finalResult;
    if (!source->hasBufferAvailable(&finalResult)) {
        return finalResult == OK ? -EWOULDBLOCK : finalResult;
    }

    return source->dequeueAccessUnit(accessUnit);
}

sp<AnotherPacketSource> NuPlayer::RTSPSource::getSource(bool audio) {
    if (mTSParser != NULL) {
        sp<MediaSource> source = mTSParser->getSource(
                audio ? ATSParser::AUDIO : ATSParser::VIDEO);

        return static_cast<AnotherPacketSource *>(source.get());
    }

    return audio ? mAudioTrack : mVideoTrack;
}

status_t NuPlayer::RTSPSource::getDuration(int64_t *durationUs) {
    *durationUs = 0ll;

    int64_t audioDurationUs;
    if (mAudioTrack != NULL
            && mAudioTrack->getFormat()->findInt64(
                kKeyDuration, &audioDurationUs)
            && audioDurationUs > *durationUs) {
        *durationUs = audioDurationUs;
    }

    int64_t videoDurationUs;
    if (mVideoTrack != NULL
            && mVideoTrack->getFormat()->findInt64(
                kKeyDuration, &videoDurationUs)
            && videoDurationUs > *durationUs) {
        *durationUs = videoDurationUs;
    }

    ALOGV("getDuration = %lld", *durationUs);
    return OK;
}

status_t NuPlayer::RTSPSource::seekTo(int64_t seekTimeUs) {
#ifndef ANDROID_DEFAULT_CODE
    status_t err = preSeekSync(seekTimeUs);
    if (err != OK) {
		// mtk80902: ALPS00436651
		if (err == ALREADY_EXISTS)
		    ALOGW("ignore too frequent seeks");
		else if (err == INVALID_OPERATION)
		    ALOGW("live streaming or switching TCP or not connected, seek is invalid.");
	        return err; // here would never return EWOULDBLOCK
    }
    //TODO: flush source to avoid still using data before seek
#endif

    sp<AMessage> msg = new AMessage(kWhatPerformSeek, mReflector->id());
    msg->setInt32("generation", ++mSeekGeneration);
    msg->setInt64("timeUs", seekTimeUs);
#ifndef ANDROID_DEFAULT_CODE
    msg->post();
    return -EWOULDBLOCK;
#else
    msg->post(200000ll);
    return OK;
#endif
}

void NuPlayer::RTSPSource::performSeek(int64_t seekTimeUs) {
    if (mState != CONNECTED) {
        ALOGD("seek %lld not perform, state != CONNECTED", seekTimeUs);
#ifndef ANDROID_DEFAULT_CODE
		// add notify
		notifyAsyncDone(NuPlayer::Source::kWhatSeekDone);
#endif
        return;
    }
    mState = SEEKING;
    mHandler->seek(seekTimeUs);
#ifndef ANDROID_DEFAULT_CODE
	// restore default - here set is server playing = false
	// so mIsServerPlaying represents seeking now.
	mIsServerPlaying = false;
#endif
}

uint32_t NuPlayer::RTSPSource::flags() const {
    return FLAG_SEEKABLE;
}

#ifndef ANDROID_DEFAULT_CODE
status_t NuPlayer::RTSPSource::preSeekSync(int64_t timeUs) {
    Mutex::Autolock autoLock(mLock);
    if (mState != CONNECTED)
	return INVALID_OPERATION;

    bool tooEarly =
        mLastSeekCompletedTimeUs >= 0
            && ALooper::GetNowUs() < mLastSeekCompletedTimeUs + 500000ll;
#ifdef MTK_BSP_PACKAGE
	//cancel  ignore seek --do every seek for bsp package
	// because ignore seek and notify seek complete will cause progress return back
	tooEarly = false;
#endif

	if (tooEarly) {
	 ALOGD("seek %lld not perform, because tooEarly", timeUs);
	 return ALREADY_EXISTS;
	}

	prepareSyncCall();
	mHandler->preSeek(timeUs);
	status_t err = finishSyncCall();
	ALOGI("preSeek end err = %d", err);
	return err;
}
#endif

void NuPlayer::RTSPSource::onMessageReceived(const sp<AMessage> &msg) {
    if (msg->what() == kWhatDisconnect) {
        uint32_t replyID;
        CHECK(msg->senderAwaitsResponse(&replyID));

        mDisconnectReplyID = replyID;
        finishDisconnectIfPossible();
        return;
    } else if (msg->what() == kWhatPerformSeek) {
        int32_t generation;
        CHECK(msg->findInt32("generation", &generation));

        if (generation != mSeekGeneration) {
            // obsolete.
#ifndef ANDROID_DEFAULT_CODE
            // add notify
            notifyAsyncDone(NuPlayer::Source::kWhatSeekDone);
#endif
            return;
        }

        int64_t seekTimeUs;
        CHECK(msg->findInt64("timeUs", &seekTimeUs));

        performSeek(seekTimeUs);
        return;
    } 
#ifndef ANDROID_DEFAULT_CODE
    else if (msg->what() == kWhatBufferingUpdate) {
        onBufferingUpdate();
        return;
    } else if (msg->what() == kWhatSendPlay) {
        ALOGD("send play, mPlayAsResume = %d", mPlayAsResume);
		// mtk80902: ALPS00451531
		// for bad server who's PLAY resp is error if we
		// send PLAY without range when it playing.
		// if server is playing then we dont sen PLAY
		// once more, just notify complete.
		if (mIsServerPlaying) {
	            notifyAsyncDone(NuPlayer::Source::kWhatPlayDone);
		    mPlayAsResume = false;
		}
		else if (mHandler != NULL)
		// mtk80902: ALPS00439792
		// special case: pause -> seek -> resume
		// in this case resume's play will start at 0
		// -- updates: wont be here when seeking.
		    mHandler->play(mPlayAsResume);
	        return;
    }
    else if (msg->what() == kWhatSendPause) {
        ALOGD("send pause");
		mPlayAsResume = true;
		if (mHandler != NULL)
		    mHandler->sendPause();
        return;
    }
#endif

    CHECK_EQ(msg->what(), (int)kWhatNotify);

    int32_t what;
    CHECK(msg->findInt32("what", &what));

    switch (what) {
        case MyHandler::kWhatConnected:
            onConnected();
#ifndef ANDROID_DEFAULT_CODE
            {   status_t err = OK;
                msg->findInt32("result", &err);
                notifyAsyncDone(NuPlayer::Source::kWhatConnDone, err);
	    		(new AMessage(kWhatBufferingUpdate, mReflector->id()))->post();
            }
#endif
            break;

        case MyHandler::kWhatDisconnected:
            onDisconnected(msg);
#ifndef ANDROID_DEFAULT_CODE
            {   status_t err = OK;
                msg->findInt32("result", &err);
                notifyAsyncDone(NuPlayer::Source::kWhatConnDone, err);
            }
#endif
            break;

        case MyHandler::kWhatSeekDone:
        {
            mState = CONNECTED;
            mStartingUp = true;
#ifndef ANDROID_DEFAULT_CODE
		    mIsServerPlaying = true;
		    (new AMessage(kWhatBufferingUpdate, mReflector->id()))->post();
            status_t err = OK;
            msg->findInt32("result", &err);
            notifyAsyncDone(NuPlayer::Source::kWhatSeekDone, err);
#endif
            break;
        }

        case MyHandler::kWhatAccessUnit:
        {
            ALOGV("kWhatAccessUnit");
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));

            if (mTSParser == NULL) {
                CHECK_LT(trackIndex, mTracks.size());
            } else {
                CHECK_EQ(trackIndex, 0u);
            }

            sp<ABuffer> accessUnit;
            CHECK(msg->findBuffer("accessUnit", &accessUnit));

            int32_t damaged;
            if (accessUnit->meta()->findInt32("damaged", &damaged)
                    && damaged) {
                ALOGI("dropping damaged access unit.");
                break;
            }

            if (mTSParser != NULL) {
                size_t offset = 0;
                status_t err = OK;
                while (offset + 188 <= accessUnit->size()) {
                    err = mTSParser->feedTSPacket(
                            accessUnit->data() + offset, 188);
                    if (err != OK) {
                        break;
                    }

                    offset += 188;
                }

                if (offset < accessUnit->size()) {
                    err = ERROR_MALFORMED;
                }

                if (err != OK) {
                    sp<AnotherPacketSource> source = getSource(false /* audio */);
                    if (source != NULL) {
                        source->signalEOS(err);
                    }

                    source = getSource(true /* audio */);
                    if (source != NULL) {
                        source->signalEOS(err);
                    }
                }
                break;
            }

            TrackInfo *info = &mTracks.editItemAt(trackIndex);

            sp<AnotherPacketSource> source = info->mSource;
            if (source != NULL) {
                uint32_t rtpTime;
                CHECK(accessUnit->meta()->findInt32("rtp-time", (int32_t *)&rtpTime));

                if (!info->mNPTMappingValid) {
                    // This is a live stream, we didn't receive any normal
                    // playtime mapping. We won't map to npt time.
                    source->queueAccessUnit(accessUnit);
                    break;
                }

                int64_t nptUs =
                    ((double)rtpTime - (double)info->mRTPTime)
                        / info->mTimeScale
                        * 1000000ll
                        + info->mNormalPlaytimeUs;

                accessUnit->meta()->setInt64("timeUs", nptUs);

                source->queueAccessUnit(accessUnit);
            }
            break;
        }

        case MyHandler::kWhatEOS:
        {
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));
#ifdef ANDROID_DEFAULT_CODE
			// mtk80902: ALPS00434921
            CHECK_LT(trackIndex, mTracks.size());
#endif
            int32_t finalResult;
            CHECK(msg->findInt32("finalResult", &finalResult));
            CHECK_NE(finalResult, (status_t)OK);
#ifndef ANDROID_DEFAULT_CODE
			// mtk80902: ALPS00447701
			// error occurs while still receiving data
		    int32_t QuitRightNow;
		    CHECK(msg->findInt32("QuitRightNow", &QuitRightNow));
			// mtk80902: ALPS00453677 - pause - seek to the end - seek anywhere else
			// here mPlayAsResume presents pause state, means if pause then seek to
			// the end, this seek wont cause EOS.
		    if (QuitRightNow)// [alps00569610]
			mFinalResult2 = finalResult;
#endif

	        if (mTSParser != NULL) {
	            sp<AnotherPacketSource> source = getSource(false /* audio */);
	            if (source != NULL) {
	                source->signalEOS(finalResult);
	            }

	            source = getSource(true /* audio */);
	            if (source != NULL) {
	                source->signalEOS(finalResult);
	            }

	            return;
	        }

#ifndef ANDROID_DEFAULT_CODE
			// mtk80902: ALPS00434921
		    if (mTracks.size() == 0 || trackIndex >= mTracks.size()) {
				ALOGW("sth wrong that track index: %d while mTrack's size: %d",
					trackIndex, mTracks.size());
				break;
		    }
#endif
            TrackInfo *info = &mTracks.editItemAt(trackIndex);
            sp<AnotherPacketSource> source = info->mSource;
            if (source != NULL) {
                source->signalEOS(finalResult);
            }

            break;
        }

        case MyHandler::kWhatSeekDiscontinuity:
        {
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));
            CHECK_LT(trackIndex, mTracks.size());

            TrackInfo *info = &mTracks.editItemAt(trackIndex);
            sp<AnotherPacketSource> source = info->mSource;
            if (source != NULL) {
#ifndef ANDROID_DEFAULT_CODE
				source->queueDiscontinuity(ATSParser::DISCONTINUITY_FLUSH_SOURCE_ONLY, NULL);
#else
				source->queueDiscontinuity(ATSParser::DISCONTINUITY_SEEK, NULL);
#endif
            }

            break;
        }

        case MyHandler::kWhatNormalPlayTimeMapping:
        {
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));
            CHECK_LT(trackIndex, mTracks.size());

            uint32_t rtpTime;
            CHECK(msg->findInt32("rtpTime", (int32_t *)&rtpTime));

            int64_t nptUs;
            CHECK(msg->findInt64("nptUs", &nptUs));

            TrackInfo *info = &mTracks.editItemAt(trackIndex);
            info->mRTPTime = rtpTime;
            info->mNormalPlaytimeUs = nptUs;
            info->mNPTMappingValid = true;
            break;
        }
#ifndef ANDROID_DEFAULT_CODE
        case MyHandler::kWhatPreSeekDone:
        {
            completeSyncCall(msg);
            break;
        }
        case MyHandler::kWhatPlayDone:
        {
            status_t err = OK;
            msg->findInt32("result", &err);
            notifyAsyncDone(NuPlayer::Source::kWhatPlayDone, err);
		    mPlayAsResume = false;
		    mIsServerPlaying = true;
		    break;
        }
        case MyHandler::kWhatPauseDone:
        {
            status_t err = OK;
            msg->findInt32("result", &err);
            notifyAsyncDone(NuPlayer::Source::kWhatPauseDone, err);
		    mIsServerPlaying = false;
		    break;
        }
#endif
        default: 
        {
            ALOGD("Unhandled MyHandler notification '%c%c%c%c' .", 
                    what>>24, (char)((what>>16) & 0xff), (char)((what>>8) & 0xff), (char)(what & 0xff));
//            TRESPASS();
		}
    }
}

void NuPlayer::RTSPSource::onConnected() {
    ALOGV("onConnected");
    CHECK(mAudioTrack == NULL);
    CHECK(mVideoTrack == NULL);

    size_t numTracks = mHandler->countTracks();
    for (size_t i = 0; i < numTracks; ++i) {
        int32_t timeScale;
        sp<MetaData> format = mHandler->getTrackFormat(i, &timeScale);

        const char *mime;
        CHECK(format->findCString(kKeyMIMEType, &mime));

        if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG2TS)) {
            // Very special case for MPEG2 Transport Streams.
            CHECK_EQ(numTracks, 1u);

            mTSParser = new ATSParser;
            return;
        }

        bool isAudio = !strncasecmp(mime, "audio/", 6);
        bool isVideo = !strncasecmp(mime, "video/", 6);

        TrackInfo info;
        info.mTimeScale = timeScale;
        info.mRTPTime = 0;
        info.mNormalPlaytimeUs = 0ll;
        info.mNPTMappingValid = false;

        if ((isAudio && mAudioTrack == NULL)
                || (isVideo && mVideoTrack == NULL)) {
            sp<AnotherPacketSource> source = new AnotherPacketSource(format);
#ifndef ANDROID_DEFAULT_CODE
			//for bitrate adaptation, ARTPConnection need get the pointer of AnotherPacketSource
			//to get the buffer queue info during sendRR
			mHandler->setAnotherPacketSource(i,source);

			//set bufferQue size and target time to anotherpacketSource
			//which will be same to the buffer info send to server during setup
			source->setBufQueSize(m_BufQueSize);
			source->setTargetTime(m_TargetTime);
#endif
            if (isAudio) {
                mAudioTrack = source;
            } else {
                mVideoTrack = source;
            }

            info.mSource = source;
        }

        mTracks.push(info);
    }
#ifndef ANDROID_DEFAULT_CODE
    // mtk80902: ALPS00448589
    // porting from MtkRTSPController
    if (mMetaData == NULL)
		mMetaData = new MetaData;
    mMetaData->setInt32(kKeyServerTimeout, mHandler->getServerTimeout());
    AString val;
    sp<ASessionDescription> desc = mHandler->getSessionDesc();
    if (desc->findAttribute(0, "s=", &val)) {
		ALOGI("rtsp s=%s ", val.c_str());
		mMetaData->setCString(kKeyTitle, val.c_str());
    }

    if (desc->findAttribute(0, "i=", &val)) {
		ALOGI("rtsp i=%s ", val.c_str());
		mMetaData->setCString(kKeyAuthor, val.c_str());
    }
#endif
    mState = CONNECTED;
}

void NuPlayer::RTSPSource::onDisconnected(const sp<AMessage> &msg) {
    status_t err;
    CHECK(msg->findInt32("result", &err));
    CHECK_NE(err, (status_t)OK);

#ifndef ANDROID_DEFAULT_CODE
    mIsServerPlaying = false;
    mPlayAsResume = false;
    // mtk80902: ALPS00441415 - pause fail called, stop called again
    // this function cant be called twice..
    if (mState == DISCONNECTED)
		return;
    mHandlerLooper->unregisterHandler(mHandler->id());
#else
    mLooper->unregisterHandler(mHandler->id());
#endif
    mHandler.clear();

    mState = DISCONNECTED;
#ifndef ANDROID_DEFAULT_CODE
    mFinalResult2 = err;
#else
    mFinalResult = err;
#endif

    if (mDisconnectReplyID != 0) {
        finishDisconnectIfPossible();
    }
}

void NuPlayer::RTSPSource::finishDisconnectIfPossible() {
    if (mState != DISCONNECTED) {
        mHandler->disconnect();
        return;
    }

    (new AMessage)->postReply(mDisconnectReplyID);
    mDisconnectReplyID = 0;
}

#ifndef ANDROID_DEFAULT_CODE
status_t NuPlayer::RTSPSource::getBufferedDurationUs(int64_t *durationUs) {
    int64_t trackUs = 0;
    status_t err;
    if (mVideoTrack != NULL) {
       trackUs = mVideoTrack->getBufferedDurationUs(&err); 
       if (err == OK) {
           *durationUs = trackUs;
           ALOGV("video track buffered %.2f secs", trackUs / 1E6);
       } else {
           ALOGV("video track buffer status %d", err);
       }
    }

    if (mAudioTrack != NULL) {
        trackUs = mAudioTrack->getBufferedDurationUs(&err);
        if (err == OK) {
            if (trackUs < *durationUs) {
                *durationUs = trackUs;
            }
            ALOGV("audio track buffered %.f secs", trackUs / 1E6);
        } else {
           ALOGV("audio track buffer status %d", err);
        }
    }
    return err;
}


void NuPlayer::RTSPSource::onBufferingUpdate() {

    bool wasHungry = mHungry;
    int32_t rate; 
    int64_t DurationUs;
    //int64_t MaxDurationUs = mStartingUp ? kMinDurationUs : kHighWaterMarkUs;
	int64_t MaxDurationUs = mStartingUp ? mHighWaterMarkUs : mHighWaterMarkUs;

    if (OK == getBufferedDurationUs(&DurationUs)) {

        if (wasHungry) {
            mHungry = (DurationUs < MaxDurationUs);
        } else {
            mHungry = (DurationUs < kLowWaterMarkUs);
        }

    } else {
        mHungry = false;
		// mtk80902: google's damn loop check for status is really really stupid
		// in our case final status will come up earlier than hungry status
		// so that in NuPlayer acodec's notify cannot be feedback
		// mFinalResult2 works around for this.
	    if (mFinalResult2 != OK)
			mFinalResult = mFinalResult2;
    }

    // start or end buffering
    if (wasHungry != mHungry) {
        notifyStartBuffering(mHungry);
        if (mStartingUp && !mHungry) {
            //mStartingUp is only set again when seek
            mStartingUp = false;
        }
    }

    if (mHungry) {
        sp<AMessage> notify = mNotify->dup();
        notify->setInt32("what", NuPlayer::Source::kWhatBufferNotify);
        int32_t rate = 100.0 * (double)DurationUs / MaxDurationUs;
        notify->setInt32("bufRate", rate);
        notify->post();
    }
    (new AMessage(kWhatBufferingUpdate, mReflector->id()))->post(100000ll);
}

void NuPlayer::RTSPSource::notifyStartBuffering(bool bStart) {
    sp<AMessage> notifyBuffChanged = mNotify->dup();
    notifyBuffChanged->setInt32("what", NuPlayer::Source::kWhatBufferNotify);

    // bufRate = -1 means "start buffering"
    // bufRate > 100 means "end buffring"
    notifyBuffChanged->setInt32("bufRate", bStart ? -1 : 101);
    notifyBuffChanged->post();
}

void NuPlayer::RTSPSource::notifyAsyncDone(uint32_t notif, status_t err) {
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", notif);
    notify->setInt32("result", err);
    notify->post();
}

void NuPlayer::RTSPSource::prepareSyncCall() {
    mSyncCallResult = OK;
    mSyncCallDone = false;
}

status_t NuPlayer::RTSPSource::finishSyncCall() {
    while(mSyncCallDone == false) {
        mCondition.wait(mLock);
    }
    return mSyncCallResult;
}

void NuPlayer::RTSPSource::completeSyncCall(const sp<AMessage>& msg) {
    Mutex::Autolock autoLock(mLock);
    if (!msg->findInt32("result", &mSyncCallResult)) {
        ALOGW("no result found in completeSyncCall");
        mSyncCallResult = OK;
    }
    mSyncCallDone = true;
    mCondition.signal();
}

void  NuPlayer::RTSPSource::setSDP(sp<RefBase>& sdp){
	msdp = sdp;
}

void NuPlayer::RTSPSource::setParams(const sp<MetaData>& meta)
{
    if (mHandler != NULL)
		mHandler->setPacketSourceParams(meta);	
}

bool NuPlayer::RTSPSource::removeSpecificHeaders(const String8 MyKey, KeyedVector<String8, String8> *headers, String8 *pMyHeader) {
	ALOGD("removeSpecificHeaders %s", MyKey.string());
    *pMyHeader = "";
    if (headers != NULL) {
        ssize_t index;
        if ((index = headers->indexOfKey(MyKey)) >= 0) {
            *pMyHeader = headers->valueAt(index);
            headers->removeItemsAt(index);
           	ALOGD("special headers: %s = %s", MyKey.string(), pMyHeader->string());
            return true;
        }
    }
    return false;
}

#endif

}  // namespace android
