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

#ifndef RTSP_SOURCE_H_

#define RTSP_SOURCE_H_

#include "NuPlayerSource.h"

#include "ATSParser.h"

#include <media/stagefright/foundation/AHandlerReflector.h>

namespace android {

struct ALooper;
struct AnotherPacketSource;
struct MyHandler;

struct NuPlayer::RTSPSource : public NuPlayer::Source {
    RTSPSource(
            const char *url,
            const KeyedVector<String8, String8> *headers,
            bool uidValid = false,
            uid_t uid = 0);


    virtual void start();
    virtual void stop();

    virtual status_t feedMoreTSData();

    virtual status_t dequeueAccessUnit(bool audio, sp<ABuffer> *accessUnit);

    virtual status_t getDuration(int64_t *durationUs);
    virtual status_t seekTo(int64_t seekTimeUs);

    virtual uint32_t flags() const;
	
#ifndef ANDROID_DEFAULT_CODE
//    virtual status_t getBufferedRate(int *rate);

    virtual status_t start_withCheck();
    virtual void setParams(const sp<MetaData>& meta);
    virtual status_t play();
    virtual status_t pause();
	
	void setSDP(sp<RefBase> &sdp);
	sp<RefBase> msdp;
#endif

    void onMessageReceived(const sp<AMessage> &msg);

protected:
    virtual ~RTSPSource();

    virtual sp<MetaData> getFormatMeta(bool audio);

private:
    enum {
        kWhatNotify          = 'noti',
        kWhatDisconnect      = 'disc',
        kWhatPerformSeek     = 'seek',
#ifndef ANDROID_DEFAULT_CODE
        kWhatSendPlay        = 'play',
	kWhatSendPause	     = 'paus',
        kWhatBufferingUpdate = 'buff',
#endif
    };

    enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SEEKING,
    };

    enum Flags {
        // Don't log any URLs.
        kFlagIncognito = 1,
    };

    struct TrackInfo {
        sp<AnotherPacketSource> mSource;

        int32_t mTimeScale;
        uint32_t mRTPTime;
        int64_t mNormalPlaytimeUs;
        bool mNPTMappingValid;
    };

    AString mURL;
    KeyedVector<String8, String8> mExtraHeaders;
    bool mUIDValid;
    uid_t mUID;
    uint32_t mFlags;
    State mState;
    status_t mFinalResult;
    uint32_t mDisconnectReplyID;
    bool mStartingUp;

    sp<ALooper> mLooper;
    sp<AHandlerReflector<RTSPSource> > mReflector;
    sp<MyHandler> mHandler;

    Vector<TrackInfo> mTracks;
    sp<AnotherPacketSource> mAudioTrack;
    sp<AnotherPacketSource> mVideoTrack;

    sp<ATSParser> mTSParser;

    int32_t mSeekGeneration;

    sp<AnotherPacketSource> getSource(bool audio);

    void onConnected();
    void onDisconnected(const sp<AMessage> &msg);
    void finishDisconnectIfPossible();

    void performSeek(int64_t seekTimeUs);

    bool haveSufficientDataOnAllTracks();

#ifndef ANDROID_DEFAULT_CODE
    status_t getBufferedDurationUs(int64_t *durationUs);
    void onBufferingUpdate();
    void notifyAsyncDone(uint32_t notify, status_t err = OK);
    void notifyStartBuffering(bool bStart);
    bool mHungry;
    status_t mFinalResult2;
	bool removeSpecificHeaders(const String8 MyKey, KeyedVector<String8, String8> *headers, String8 *pMyHeader);
	int64_t mHighWaterMarkUs;

    //The following are for sync call
    // >>> 
    Mutex mLock;
    Condition mCondition;
    status_t mSyncCallResult;
    bool mSyncCallDone;
    void prepareSyncCall();
    void completeSyncCall(const sp<AMessage>& msg);
    status_t finishSyncCall();
    // <<<
   
    //The following are sync call method, using prepareSyncCall+finishSyncCall
    status_t preSeekSync(int64_t timeUs);
    int64_t mLastSeekCompletedTimeUs;

	//for bitrate adaptation
    size_t m_BufQueSize; //Whole Buffer queue size 
    size_t m_TargetTime;  // target protected time of buffer queue duration for interrupt-free playback 
    // mtk80902: standalone looper for MyHander
    sp<ALooper> mHandlerLooper;
    // mtk80902: ALPS00439792
    bool mPlayAsResume;
    // mtk80902: ALPS00451531
    bool mIsServerPlaying;
#endif
    DISALLOW_EVIL_CONSTRUCTORS(RTSPSource);
};

}  // namespace android

#endif  // RTSP_SOURCE_H_
