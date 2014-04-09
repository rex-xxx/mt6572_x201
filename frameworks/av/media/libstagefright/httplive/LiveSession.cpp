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
#define LOG_TAG "LiveSession"
#include <utils/Log.h>

#include "include/LiveSession.h"

#include "LiveDataSource.h"

#include "include/M3UParser.h"
#include "include/HTTPBase.h"

#include <cutils/properties.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaErrors.h>

#include <ctype.h>
#include <openssl/aes.h>
#include <openssl/md5.h>

#ifndef ANDROID_DEFAULT_CODE
#define DUMP_PLAYLIST 1
#define DUMP_EACH_TS 0 
#define DUMP_PROFILE 0
#endif

namespace android {

LiveSession::LiveSession(uint32_t flags, bool uidValid, uid_t uid)
    : mFlags(flags),
      mUIDValid(uidValid),
      mUID(uid),
     mDataSource(new LiveDataSource),
      mHTTPDataSource(
              HTTPBase::Create(
                  (mFlags & kFlagIncognito)
                    ? HTTPBase::kFlagIncognito
                    : 0)),
      mPrevBandwidthIndex(-1),
      mLastPlaylistFetchTimeUs(-1),
      mSeqNumber(-1),
      mSeekTimeUs(-1),
      mNumRetries(0),
      mStartOfPlayback(true),
      mDurationUs(-1),
      mDurationFixed(false),
#ifndef ANDROID_DEFAULT_CODE
      mSeeking(false),
      mWasSeeking(false),
      mFirstSeqNumber(-1),
#else
      mSeekDone(false),
#endif
      mDisconnectPending(false),
      mMonitorQueueGeneration(0),
#ifndef ANDROID_DEFAULT_CODE
      mDiscontinuityNumber(0),
      mPolicy(BWPOLICY_MAX),
      mPolicyMaxBw(0),
      mLastUpdateBandwidthTimeUs(-1),
#endif
      mRefreshState(INITIAL_MINIMUM_RELOAD_DELAY) {


#ifndef ANDROID_DEFAULT_CODE
    char value[PROPERTY_VALUE_MAX];
    if (property_get("media.httplive.bw-change", value, "normal")) {
       ALOGD("-------httplive.bw-change = %s", value);
       if (!strcmp(value, "disable") || !strcmp(value, "0")) {
           mPolicy = BWPOLICY_DISABLE;
       } else if (!strcmp(value, "1") || !strcmp(value, "normal")) {
           mPolicy = BWPOLICY_MAX;
           mPolicyMaxBw = 0;
       } else if (strlen(value) > 4 && !strncmp(value, "max=", 4)) {
            mPolicy = BWPOLICY_MAX;
            char* end;
            mPolicyMaxBw = strtoul(value + 4, &end, 0);
            ALOGI("BWPOLICY_MAX, max = %ld", mPolicyMaxBw);
       } else if (!strcmp(value, "jump")) {
           mPolicy = BWPOLICY_JUMP;
       } else if (!strcmp(value, "random")) {
           mPolicy = BWPOLICY_RANDOM;
       }
    }

#endif
    if (mUIDValid) {
        mHTTPDataSource->setUID(mUID);
    }
}

LiveSession::~LiveSession() {
    ALOGD("~LiveSession");
}

sp<DataSource> LiveSession::getDataSource() {
    return mDataSource;
}

void LiveSession::connect(
        const char *url, const KeyedVector<String8, String8> *headers) {
    sp<AMessage> msg = new AMessage(kWhatConnect, id());
    msg->setString("url", url);

    if (headers != NULL) {
        msg->setPointer(
                "headers",
                new KeyedVector<String8, String8>(*headers));
    }

    msg->post();
}

void LiveSession::disconnect() {
    Mutex::Autolock autoLock(mLock);
    mDisconnectPending = true;

    mHTTPDataSource->disconnect();

    (new AMessage(kWhatDisconnect, id()))->post();
}

#ifndef ANDROID_DEFAULT_CODE
void LiveSession::seekTo(int64_t timeUs) {

    Mutex::Autolock autoLock(mLock);
    mSeeking = true;
    mHTTPDataSource->disconnect();

    sp<AMessage> msg = new AMessage(kWhatSeek, id());
    msg->setInt64("timeUs", timeUs);
    msg->post();

    while (mSeeking) {
        mCondition.wait(mLock);
        ALOGD("seek return %lld", mSeekTimeUs);
    }

}
#else
void LiveSession::seekTo(int64_t timeUs) {
    Mutex::Autolock autoLock(mLock);
    mSeekDone = false;
    sp<AMessage> msg = new AMessage(kWhatSeek, id());
    msg->setInt64("timeUs", timeUs);
    msg->post();

    while (!mSeekDone) {
        mCondition.wait(mLock);
    }
}
#endif

void LiveSession::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatConnect:
            onConnect(msg);
            break;

        case kWhatDisconnect:
            onDisconnect();
            break;

        case kWhatMonitorQueue:
        {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));

            if (generation != mMonitorQueueGeneration) {
                // Stale event
                break;
            }

            onMonitorQueue();
            break;
        }

        case kWhatSeek:
            onSeek(msg);
            break;

#ifndef ANDROID_DEFAULT_CODE
        case kWhatTellmeIfBandwidthInvalid:
            {
                int32_t bandwidth = 0;
                msg->findInt32("bandwidth", &bandwidth);
                ALOGD("@debug: kWhatTellmeIfBandwidthInvalid received, bandwidth = %d", bandwidth);
                if (bandwidth == 0) {
                    CHECK(mBandwidthItems.size() == 0);
                } else {
                    complainBandwidth(&mBandwidthItems, (unsigned long)bandwidth);
                    if (mPrevBandwidthIndex != -1 &&
                            mBandwidthItems.itemAt(mPrevBandwidthIndex).mBandwidth == bandwidth) {
                        mPrevBandwidthIndex = -1;
                    }
                }
                break;
            }
#endif
        default:
            TRESPASS();
            break;
    }
}

// static
int LiveSession::SortByBandwidth(const BandwidthItem *a, const BandwidthItem *b) {
    if (a->mBandwidth < b->mBandwidth) {
        return -1;
    } else if (a->mBandwidth == b->mBandwidth) {
        return 0;
    }

    return 1;
}

void LiveSession::onConnect(const sp<AMessage> &msg) {
    AString url;
    CHECK(msg->findString("url", &url));

    KeyedVector<String8, String8> *headers = NULL;
    if (!msg->findPointer("headers", (void **)&headers)) {
        mExtraHeaders.clear();
    } else {
        mExtraHeaders = *headers;
#ifndef ANDROID_DEFAULT_CODE
        String8 strStub;
//        TODO: tell Gallery3D that it's hl streaming, don't send these headers headers
        removeSpecificHeaders(String8("MTK-HTTP-CACHE-SIZE"), &mExtraHeaders, NULL);
        removeSpecificHeaders(String8("MTK-RTSP-CACHE-SIZE"), &mExtraHeaders, NULL);
#endif          

        delete headers;
        headers = NULL;
    }

    ALOGD("onConnect '%s'", url.c_str());

    mMasterURL = url;

    bool dummy;
    sp<M3UParser> playlist = fetchPlaylist(url.c_str(), &dummy);

    if (playlist == NULL) {
        ALOGE("unable to fetch master playlist '%s'.", url.c_str());

#ifndef ANDROID_DEFAULT_CODE
        mDataSource->queueEOS(ERROR_CANNOT_CONNECT);
#else
        mDataSource->queueEOS(ERROR_IO);
#endif
        return;
    }

    if (playlist->isVariantPlaylist()) {
        for (size_t i = 0; i < playlist->size(); ++i) {
            BandwidthItem item;

            sp<AMessage> meta;
            playlist->itemAt(i, &item.mURI, &meta);

            unsigned long bandwidth;
#ifndef ANDROID_DEFAULT_CODE
            item.mBandwidth = -1;
            item.mComplained = 0;
            if (meta != NULL) {
                meta->findInt32("bandwidth", (int32_t *)&item.mBandwidth);
            }
#else
            CHECK(meta->findInt32("bandwidth", (int32_t *)&item.mBandwidth));
#endif 
            mBandwidthItems.push(item);
        }

        CHECK_GT(mBandwidthItems.size(), 0u);

        mBandwidthItems.sort(SortByBandwidth);

#ifndef ANDROID_DEFAULT_CODE
        dumpBandwidthItems(&mBandwidthItems);
#endif
    }

    postMonitorQueue();
}

void LiveSession::onDisconnect() {
    ALOGI("onDisconnect");

    mDataSource->queueEOS(ERROR_END_OF_STREAM);

    Mutex::Autolock autoLock(mLock);
    mDisconnectPending = false;
}

status_t LiveSession::fetchFile(
        const char *url, sp<ABuffer> *out,
        int64_t range_offset, int64_t range_length) {
    *out = NULL;

    sp<DataSource> source;

    if (!strncasecmp(url, "file://", 7)) {
        source = new FileSource(url + 7);
    } else if (strncasecmp(url, "http://", 7)
            && strncasecmp(url, "https://", 8)) {
        ALOGE("unsupported file source %s", url);
        return ERROR_UNSUPPORTED;
    } else {
        {
            Mutex::Autolock autoLock(mLock);

            if (mDisconnectPending) {
                return ERROR_IO;
            }
        }

        KeyedVector<String8, String8> headers = mExtraHeaders;
        if (range_offset > 0 || range_length >= 0) {
            headers.add(
                    String8("Range"),
                    String8(
                        StringPrintf(
                            "bytes=%lld-%s",
                            range_offset,
                            range_length < 0
                                ? "" : StringPrintf("%lld", range_offset + range_length - 1).c_str()).c_str()));
        }

        status_t err = mHTTPDataSource->connect(
                url, mExtraHeaders.isEmpty() ? NULL : &headers);

        if (err != OK) {
            return err;
        }

        source = mHTTPDataSource;
    }

    off64_t size;
    status_t err = source->getSize(&size);

    if (err != OK) {
        size = 65536;
    }

    sp<ABuffer> buffer = new ABuffer(size);
    buffer->setRange(0, 0);

    for (;;) {
        size_t bufferRemaining = buffer->capacity() - buffer->size();

        if (bufferRemaining == 0) {
            bufferRemaining = 32768;

            ALOGV("increasing download buffer to %d bytes",
                 buffer->size() + bufferRemaining);

            sp<ABuffer> copy = new ABuffer(buffer->size() + bufferRemaining);
            memcpy(copy->data(), buffer->data(), buffer->size());
            copy->setRange(0, buffer->size());

            buffer = copy;
        }

        size_t maxBytesToRead = bufferRemaining;
        if (range_length >= 0) {
            int64_t bytesLeftInRange = range_length - buffer->size();
            if (bytesLeftInRange < maxBytesToRead) {
                maxBytesToRead = bytesLeftInRange;

                if (bytesLeftInRange == 0) {
                    break;
                }
            }
        }

        ssize_t n = source->readAt(
                buffer->size(), buffer->data() + buffer->size(),
                maxBytesToRead);
        ALOGD("readAt size (%lld)%d + %d (ret = %d)", size, (int)buffer->size(), (int)bufferRemaining, n);
        if (n < 0) {
            return n;
        }

        if (n == 0) {
            break;
        }

        buffer->setRange(0, buffer->size() + (size_t)n);
    }

    *out = buffer;

    return OK;
}

#ifndef ANDROID_DEFAULT_CODE
sp<M3UParser> LiveSession::fetchPlaylist(const char *url, bool *unchanged, status_t *fetchStatus) {
    ALOGV("fetchPlaylist '%s'", url);
    if (fetchStatus != NULL)
        *fetchStatus = OK;
#else
sp<M3UParser> LiveSession::fetchPlaylist(const char *url, bool *unchanged) {
    ALOGV("fetchPlaylist '%s'", url);

#endif
    *unchanged = false;

    sp<ABuffer> buffer;
    status_t err = fetchFile(url, &buffer);

    if (err != OK) {
#ifndef ANDROID_DEFAULT_CODE
        if (fetchStatus != NULL)
            *fetchStatus = err;
#endif
        return NULL;
    }
#if DUMP_PLAYLIST
    const int32_t nDumpSize = 1024 + 512;
    char dumpM3U8[nDumpSize];
    ALOGD("Playlist (size = %d) :\n", buffer->size());
    size_t dumpSize = (buffer->size() > (nDumpSize - 1)) ? (nDumpSize - 1) : buffer->size();
    memcpy(dumpM3U8, buffer->data(), dumpSize);
    dumpM3U8[dumpSize] = '\0';
    ALOGD("%s", dumpM3U8);
    ALOGD(" %s", ((buffer->size() < (nDumpSize - 1)) ? " " : "trunked because larger than dumpsize"));


    /*
    char DumpFileName[] = "/data/misc/dump_play.m3u8";
    FILE *PlayListFile = fopen(DumpFileName, "wb");
    if (PlayListFile != NULL) {
        CHECK_EQ(fwrite(buffer->data(), 1, buffer->size(), PlayListFile),
                buffer->size());
        fclose(PlayListFile);
    
    } else {
        ALOGE("error to create dump playlist file %s", DumpFileName);
    }
    */
#endif


    // MD5 functionality is not available on the simulator, treat all
    // playlists as changed.

#if defined(HAVE_ANDROID_OS)
    uint8_t hash[16];

    MD5_CTX m;
    MD5_Init(&m);
    MD5_Update(&m, buffer->data(), buffer->size());

    MD5_Final(hash, &m);

    if (mPlaylist != NULL && !memcmp(hash, mPlaylistHash, 16)) {
        // playlist unchanged

        if (mRefreshState != THIRD_UNCHANGED_RELOAD_ATTEMPT) {
            mRefreshState = (RefreshState)(mRefreshState + 1);
        }

        *unchanged = true;

        ALOGV("Playlist unchanged, refresh state is now %d",
             (int)mRefreshState);

        return NULL;
    }

    memcpy(mPlaylistHash, hash, sizeof(hash));

    mRefreshState = INITIAL_MINIMUM_RELOAD_DELAY;
#endif

    sp<M3UParser> playlist =
        new M3UParser(url, buffer->data(), buffer->size());

    if (playlist->initCheck() != OK) {
        ALOGE("failed to parse .m3u8 playlist");
#ifndef ANDROID_DEFAULT_CODE
        if (fetchStatus != NULL)
            *fetchStatus = ERROR_MALFORMED;
#endif

        return NULL;
    }

    return playlist;
}

int64_t LiveSession::getSegmentStartTimeUs(int32_t seqNumber) const {
    CHECK(mPlaylist != NULL);

    int32_t firstSeqNumberInPlaylist;
    if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                "media-sequence", &firstSeqNumberInPlaylist)) {
        firstSeqNumberInPlaylist = 0;
    }

    int32_t lastSeqNumberInPlaylist =
        firstSeqNumberInPlaylist + (int32_t)mPlaylist->size() - 1;

    CHECK_GE(seqNumber, firstSeqNumberInPlaylist);
    CHECK_LE(seqNumber, lastSeqNumberInPlaylist);

    int64_t segmentStartUs = 0ll;
    for (int32_t index = 0;
            index < seqNumber - firstSeqNumberInPlaylist; ++index) {
        sp<AMessage> itemMeta;
        CHECK(mPlaylist->itemAt(
                    index, NULL /* uri */, &itemMeta));

        int64_t itemDurationUs;
        CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

        segmentStartUs += itemDurationUs;
    }

    return segmentStartUs;
}

static double uniformRand() {
    return (double)rand() / RAND_MAX;
}

#ifndef ANDROID_DEFAULT_CODE
size_t LiveSession::getBandwidthIndex() {
    ssize_t index = 0;
    if (mBandwidthItems.size() == 0) {
        return 0;
    }
    
    switch (mPolicy) {
        case BWPOLICY_DISABLE:
            index = 0;
            break;
        case BWPOLICY_JUMP:
            {
                // There's a 50% chance to stay on the current bandwidth and
                // a 50% chance to switch to the next higher bandwidth (wrapping around
                // to lowest)
                const size_t kMinIndex = 0;

                if (mPrevBandwidthIndex < 0) {
                    index = kMinIndex;
                } else if (uniformRand() < 0.5) {
                    index = (size_t)mPrevBandwidthIndex;
                } else {
                    index = mPrevBandwidthIndex + 1;
                    if (index == mBandwidthItems.size()) {
                        index = kMinIndex;
                    }
                }
                break;
            }
        case BWPOLICY_RANDOM:
            {
                int32_t retries = 0;
                do {
                    index = uniformRand() * mBandwidthItems.size();
                    retries ++;
                } while (mBandwidthItems.itemAt(index).mComplained > 0 && retries < 20);
                break;
            }
        case BWPOLICY_MAX:
            {
                int32_t bandwidthBps;
                if (mHTTPDataSource != NULL
                        && mHTTPDataSource->estimateBandwidth(&bandwidthBps)) {
                   ALOGD("bandwidth estimated at %.2f kbps", bandwidthBps / 1024.0f);
                } else {
                    ALOGD("no bandwidth estimate.");
                    return 0;  // Pick the lowest bandwidth stream by default.
                }

                if (mPolicyMaxBw > 0 && bandwidthBps > mPolicyMaxBw) {
                    bandwidthBps = mPolicyMaxBw;
                }

                // Consider only 70% of the available bandwidth usable. (android default = 80%) 
                // TODO: this should be tuned by more convincing way
                bandwidthBps = (bandwidthBps * 7) / 10;

                // Pick the highest bandwidth stream below or equal to estimated bandwidth.

                for (index = mBandwidthItems.size() - 1; index >= 0; index --) {
                    if (mBandwidthItems.itemAt(index).mComplained < 1 
                            && mBandwidthItems.itemAt(index).mBandwidth < (size_t)bandwidthBps)
                        break;
                    ALOGD("@debug: ---path [%d] %d complained, bandwidth = %d vs %d", index, 
                            mBandwidthItems.itemAt(index).mComplained, 
                            mBandwidthItems.itemAt(index).mBandwidth, bandwidthBps);
                }
                if (index < 0) {
                    ALOGD("all bandwidth is complained, just using [0]");
                    dumpBandwidthItems(&mBandwidthItems);
                    index = 0;
                }
                break;
            }
        default:
            TRESPASS();
            break;
    }

    CHECK(index >= 0);
    return (size_t)index;
}

#else

size_t LiveSession::getBandwidthIndex() {

    if (mBandwidthItems.size() == 0) {
        return 0;
    }

#if 1
    int32_t bandwidthBps;
    if (mHTTPDataSource != NULL
            && mHTTPDataSource->estimateBandwidth(&bandwidthBps)) {
        ALOGV("bandwidth estimated at %.2f kbps", bandwidthBps / 1024.0f);
    } else {
        ALOGV("no bandwidth estimate.");
        return 0;  // Pick the lowest bandwidth stream by default.
    }

    char value[PROPERTY_VALUE_MAX];
    if (property_get("media.httplive.max-bw", value, NULL)) {
        char *end;
        long maxBw = strtoul(value, &end, 10);
        if (end > value && *end == '\0') {
            if (maxBw > 0 && bandwidthBps > maxBw) {
                ALOGV("bandwidth capped to %ld bps", maxBw);
                bandwidthBps = maxBw;
            }
        }
    }

    // Consider only 80% of the available bandwidth usable.
    bandwidthBps = (bandwidthBps * 8) / 10;

    // Pick the highest bandwidth stream below or equal to estimated bandwidth.

    size_t index = mBandwidthItems.size() - 1;
    while (index > 0 && mBandwidthItems.itemAt(index).mBandwidth
                            > (size_t)bandwidthBps) {
        --index;
    }
#elif 0
    // Change bandwidth at random()
    size_t index = uniformRand() * mBandwidthItems.size();
#elif 0
    // There's a 50% chance to stay on the current bandwidth and
    // a 50% chance to switch to the next higher bandwidth (wrapping around
    // to lowest)
    const size_t kMinIndex = 0;

    size_t index;
    if (mPrevBandwidthIndex < 0) {
        index = kMinIndex;
    } else if (uniformRand() < 0.5) {
        index = (size_t)mPrevBandwidthIndex;
    } else {
        index = mPrevBandwidthIndex + 1;
        if (index == mBandwidthItems.size()) {
            index = kMinIndex;
        }
    }
#elif 0
    // Pick the highest bandwidth stream below or equal to 1.2 Mbit/sec

    size_t index = mBandwidthItems.size() - 1;
    while (index > 0 && mBandwidthItems.itemAt(index).mBandwidth > 1200000) {
        --index;
    }
#else
    size_t index = mBandwidthItems.size() - 1;  // Highest bandwidth stream
#endif

    return index;
}

#endif

bool LiveSession::timeToRefreshPlaylist(int64_t nowUs) const {
    if (mPlaylist == NULL) {
        CHECK_EQ((int)mRefreshState, (int)INITIAL_MINIMUM_RELOAD_DELAY);
        return true;
    }

    int32_t targetDurationSecs;
    CHECK(mPlaylist->meta()->findInt32("target-duration", &targetDurationSecs));

    int64_t targetDurationUs = targetDurationSecs * 1000000ll;

    int64_t minPlaylistAgeUs;

    switch (mRefreshState) {
        case INITIAL_MINIMUM_RELOAD_DELAY:
        {
            size_t n = mPlaylist->size();
            if (n > 0) {
                sp<AMessage> itemMeta;
                CHECK(mPlaylist->itemAt(n - 1, NULL /* uri */, &itemMeta));

                int64_t itemDurationUs;
                CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

                minPlaylistAgeUs = itemDurationUs;
                break;
            }

            // fall through
        }

        case FIRST_UNCHANGED_RELOAD_ATTEMPT:
        {
            minPlaylistAgeUs = targetDurationUs / 2;
            break;
        }

        case SECOND_UNCHANGED_RELOAD_ATTEMPT:
        {
            minPlaylistAgeUs = (targetDurationUs * 3) / 2;
            break;
        }

        case THIRD_UNCHANGED_RELOAD_ATTEMPT:
        {
            minPlaylistAgeUs = targetDurationUs * 3;
            break;
        }

        default:
            TRESPASS();
            break;
    }

    return mLastPlaylistFetchTimeUs + minPlaylistAgeUs <= nowUs;
}
#ifndef ANDROID_DEFAULT_CODE

bool LiveSession::timeToRefreshBandwidth(int64_t nowUs) const {
    if ((mPrevBandwidthIndex < 0) || (mLastUpdateBandwidthTimeUs < -1))
        return true;

    return true;
//    return mLastUpdateBandwidthTimeUs + 10 * 1000000ll <= nowUs;
}
		


void LiveSession::onDownloadNext() {
    ALOGV("onDownloadNext, mSeqNumber=%d", mSeqNumber);
    size_t bandwidthIndex = mPrevBandwidthIndex;
    int64_t nowUs = ALooper::GetNowUs();
    if (timeToRefreshBandwidth(nowUs)) {
        mLastUpdateBandwidthTimeUs = nowUs;
        bandwidthIndex = getBandwidthIndex();
    }

    int iRepeat = 0;

rinse_repeat:
    iRepeat ++;
    ALOGD("\t\t repeat %d bandwidthIndex = %d", iRepeat - 1, bandwidthIndex);
    if ((ssize_t)bandwidthIndex < 0) {
        ALOGE("no more good bandwidthItem");
        dumpBandwidthItems(&mBandwidthItems);
        mDataSource->queueEOS(ERROR_MALFORMED);
        return;
    }
    nowUs = ALooper::GetNowUs();

    if (mLastPlaylistFetchTimeUs < 0
            || (ssize_t)bandwidthIndex != mPrevBandwidthIndex
            || (!mPlaylist->isComplete() && timeToRefreshPlaylist(nowUs))) {
        AString url;
        if (mBandwidthItems.size() > 0) {
            ALOGD("using [%u] bps (last = %d) items, bandwidth = %ld", 
                    bandwidthIndex, mPrevBandwidthIndex, mBandwidthItems.editItemAt(bandwidthIndex).mBandwidth);
            url = mBandwidthItems.editItemAt(bandwidthIndex).mURI;
        } else {
            url = mMasterURL;
            ALOGD("using MasterURL %s", mMasterURL.c_str());
        }


        if ((ssize_t)bandwidthIndex != mPrevBandwidthIndex) {
            // If we switch bandwidths, do not pay any heed to whether
            // playlists changed since the last time...
            mPlaylist.clear();
        }

        bool unchanged;
        ALOGV("m3uparser init by url = %s", url.c_str());
#ifndef ANDROID_DEFAULT_CODE
        status_t fetchStatus = OK;

        sp<M3UParser> playlist = fetchPlaylist(url.c_str(), &unchanged, &fetchStatus);
#else
        sp<M3UParser> playlist = fetchPlaylist(url.c_str(), &unchanged);
#endif
        if (playlist == NULL) {
            if (unchanged) {
                // We succeeded in fetching the playlist, but it was
                // unchanged from the last time we tried.
            }
            else if (!mSeeking) {

                ALOGE("failed to load playlist(bandwidthIndex = %u) at url '%s'", bandwidthIndex, url.c_str());
                mLastPlaylistFetchTimeUs = -1;
                
                if (alternateBandwidth(fetchStatus, &bandwidthIndex)) {
                    ALOGI("try to another bandwidthIndex = %u", bandwidthIndex);
                    goto rinse_repeat;
                } else {
                    ALOGE("no more bandwidthItems to retry");
                    mDataSource->queueEOS(ERROR_IO);
                    return;
                }

            } else {
                ALOGD("fetchPlaylist stopped due to seek");
                goto rinse_repeat;
            }
        } else {
            mPlaylist = playlist;
        }

        //get duration
        if (!mDurationFixed) {
            Mutex::Autolock autoLock(mLock);

            if (!mPlaylist->isComplete()) {
                ALOGD("playlist is not complete, duration = -1");
                mDurationUs = -1;
                mDurationFixed = true;
            } else {
                mDurationUs = 0;
                for (size_t i = 0; i < mPlaylist->size(); ++i) {
                    sp<AMessage> itemMeta;
                    CHECK(mPlaylist->itemAt(
                                i, NULL /* uri */, &itemMeta));

                    int64_t itemDurationUs;
                    CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

                    mDurationUs += itemDurationUs;
                }
                mDurationFixed = mPlaylist->isComplete();
                ALOGV("get duration from playlist , duration = %lld", mDurationUs);
            }
        }

        mLastPlaylistFetchTimeUs = ALooper::GetNowUs();
   }

    if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                "media-sequence", &mFirstSeqNumber)) {
        mFirstSeqNumber = 0;
    }

    bool explicitDiscontinuity = false;
    bool bandwidthChanged = false;


    int32_t lastSeqNumberInPlaylist =
        mFirstSeqNumber + (int32_t)mPlaylist->size() - 1;

    if (mSeqNumber < 0) {
        if (mPlaylist->isComplete()) {
            mSeqNumber = mFirstSeqNumber;
        } else {
            // If this is a live session, start 2 segments from the end.
            mSeqNumber = lastSeqNumberInPlaylist - 2;
            if (mSeqNumber < mFirstSeqNumber) {
                mSeqNumber = mFirstSeqNumber;
            }
        }
    }

    ALOGD("%d vs [%d, %d]", 
            mSeqNumber, mFirstSeqNumber, lastSeqNumberInPlaylist);

    if (mSeqNumber < mFirstSeqNumber
            || mSeqNumber > lastSeqNumberInPlaylist) {
        if (mPrevBandwidthIndex == -1) {
            ALOGD("new bandwidth has no alternate, just use new bandwidth");
            mSeqNumber = lastSeqNumberInPlaylist;
        } else if (mPrevBandwidthIndex != (ssize_t)bandwidthIndex) {
            // Go back to the previous bandwidth.

            if (mSeqNumber > lastSeqNumberInPlaylist) {
                ALOGI("new bandwidth does not have the sequence number (%d vs [%d, %d]) "
                        "we're looking for, switching back to previous bandwidth", 
                        mSeqNumber, mFirstSeqNumber, lastSeqNumberInPlaylist);

                mLastPlaylistFetchTimeUs = -1;

                bandwidthIndex = mPrevBandwidthIndex;
                goto rinse_repeat;
            } else {
                ALOGI("new bandwidth is quicker (%d vs [%d, %d]",
                        mSeqNumber, mFirstSeqNumber, lastSeqNumberInPlaylist);
            }
        }

        if (!mPlaylist->isComplete() && mNumRetries < kMaxNumRetries) {
            ALOGD("retrying %d", mNumRetries);
            ++mNumRetries;

            if (mSeqNumber > lastSeqNumberInPlaylist) {
                mLastPlaylistFetchTimeUs = -1;
                postMonitorQueue(3000000ll);
                return;
            }

            // we've missed the boat, let's start from the lowest sequence
            // number available and signal a discontinuity.

            ALOGI("We've missed the boat, restarting playback.");
            mSeqNumber = lastSeqNumberInPlaylist;
            explicitDiscontinuity = true;

            // fall through
        } else {
            ALOGE("Cannot find sequence number %d in playlist "
                 "(contains %d - %d)",
                 mSeqNumber, mFirstSeqNumber,
                 mFirstSeqNumber + mPlaylist->size() - 1);

            mDataSource->queueEOS(ERROR_END_OF_STREAM);
            return;
        }
    }

    mNumRetries = 0;

    AString uri;
    sp<AMessage> itemMeta;
    CHECK(mPlaylist->itemAt(
                mSeqNumber - mFirstSeqNumber,
                &uri,
                &itemMeta));

    int32_t val;
    if (itemMeta->findInt32("discontinuity", &val) && val != 0) 
    {    		
        explicitDiscontinuity = true;
    }

    int64_t range_offset, range_length;
    if (!itemMeta->findInt64("range-offset", &range_offset)
            || !itemMeta->findInt64("range-length", &range_length)) {
        range_offset = 0;
        range_length = -1;
    }


    sp<ABuffer> buffer;
    
    int64_t fetchStartTimeUs = ALooper::GetNowUs();
    status_t err = fetchFile(uri.c_str(), &buffer, range_offset, range_length);
    int64_t fetchEndTimeUs = ALooper::GetNowUs();
    ALOGI("fetchFile spend time = %.2f", (fetchEndTimeUs - fetchStartTimeUs) / 1E6);
    if (err != OK) {
        ALOGE("failed(err = %d) to fetch .ts segment at url '%s'", err, uri.c_str());
#ifndef ANDRIOD_DEFAULT_CODE
        {
            Mutex::Autolock autoLock(mLock);
            if (mSeeking) {
                ALOGD("fetchFile stooped due to seek");
                mDataSource->reset();
                return;
            }
        }
        if (err == ERROR_OUT_OF_RANGE) {
            if (!mPlaylist->isComplete()) {
                ALOGE("we may missed the boat, try to fresh playlist");
                mLastPlaylistFetchTimeUs = -1; 
            } else {

                ALOGE("remove bandwidth index [%d]", bandwidthIndex);
                mLastPlaylistFetchTimeUs = -1;
                mBandwidthItems.removeAt(bandwidthIndex);
                bandwidthIndex --; //getBandwidthIndex(); 
                if ((ssize_t)bandwidthIndex < 0 || mBandwidthItems.isEmpty()) {
                    ALOGE("no more bandwidthItems to retry");
                    mDataSource->queueEOS(ERROR_MALFORMED);
                    return;
                }
                mPrevBandwidthIndex = (mPrevBandwidthIndex > bandwidthIndex) ? 
                    bandwidthIndex : mPrevBandwidthIndex; //min()
                ALOGI("try to lower bandwidthIndex = %u", bandwidthIndex);
            }
            goto rinse_repeat;
        } else {
            ALOGE("fetch file error");
            mDataSource->queueEOS(ERROR_CANNOT_CONNECT);
            return;
        }
#else
        mDataSource->queueEOS(err);
        return;
#endif
    }

    if (mPlaylist->isVendorSource() && buffer->size() == 0) {
        //vendor source is using empty buffer to signal discontinuity
        explicitDiscontinuity = true;
        ALOGD("receive discontinuity fromvendor source");
    } else {

        CHECK(buffer != NULL);

        err = decryptBuffer(mSeqNumber - mFirstSeqNumber, buffer);

        if (err != OK) {
            ALOGE("decryptBuffer failed w/ error %d", err);

            mDataSource->queueEOS(err);
            return;
        }

        if (buffer->size() == 0 || buffer->data()[0] != 0x47) {
            // Not a transport stream???

            ALOGE("This doesn't look like a transport stream... size = %d", (int)buffer->size());

            mBandwidthItems.removeAt(bandwidthIndex);

            if (mBandwidthItems.isEmpty()) {
                mDataSource->queueEOS(ERROR_UNSUPPORTED);
                return;
            }

            ALOGI("Retrying with a different bandwidth stream.");

            mLastPlaylistFetchTimeUs = -1;
            bandwidthIndex = getBandwidthIndex();
            mPrevBandwidthIndex = bandwidthIndex;
            mSeqNumber = -1;

            goto rinse_repeat;
        }
    }

    if ((size_t)mPrevBandwidthIndex != bandwidthIndex) {
        bandwidthChanged = true;
    }

    if (mPrevBandwidthIndex < 0) {
        // Don't signal a bandwidth change at the very beginning of
        // playback.
        bandwidthChanged = false;
    }

    sp<ABuffer> specialBuffer = new ABuffer(188);

    if (mStartOfPlayback) {
        mWasSeeking = true;
        mStartOfPlayback = false;
    }

    
    uint8_t* p = specialBuffer->data(); 
    memset(p, 0, specialBuffer->size());

     //00->07: 00 [discontinuity_type] 00 00 00 00 00 00
    //08->11: 'm' 't' 'k' '\0', [first-timestamp]x8, 00, [bandwidth]x4, 00,
    if (mWasSeeking) {
        p[1] = 0;
        mDiscontinuityNumber ++;
    } else if (explicitDiscontinuity || bandwidthChanged) {
        //TODO: to avoid drop frame when explicitDiscontinuity or bandwidthChanged
        p[1] = 1;
        mDiscontinuityNumber ++;
    } else {
        p[1] = 2;
    }

    //'m' 't' 'k' 00
    p += 8;
    strcpy((char*)p, "mtk");
    p += 4;

    //[first-timestamp]x8 
    int64_t segmentStartTimeUs = 0;
    if (mPlaylist->isComplete() || mPlaylist->isEvent()) {
        segmentStartTimeUs = getSegmentStartTimeUs(mSeqNumber);
        ALOGD("@debug: timeUs_inplaylist = %lld", segmentStartTimeUs);
    }
    memcpy(p, &segmentStartTimeUs, sizeof(segmentStartTimeUs));
    p += sizeof(segmentStartTimeUs);
    CHECK(p[0] == 0); p ++;

    //[bandwidth]x4
    unsigned long current_bandwidth = 0;
    if (mBandwidthItems.size() > 0) {
        current_bandwidth = mBandwidthItems.editItemAt(bandwidthIndex).mBandwidth;
        ALOGD("@debug: current bandwidth = %ld", current_bandwidth);
    }
    memcpy(p, &current_bandwidth, sizeof(current_bandwidth));
    p += sizeof(current_bandwidth);
    CHECK(p[0] == 0); p ++;

    // type && 0x02: if 1, startTimeUs saved in buffer[2]
    if (mWasSeeking || explicitDiscontinuity || bandwidthChanged) {
        // Signal discontinuity.
        ALOGI("queueing discontinuity (seek=%d, explicit=%d, bandwidthChanged=%d)",
                mWasSeeking, explicitDiscontinuity, bandwidthChanged);
    }

    mDataSource->queueBuffer(specialBuffer);
    

    mWasSeeking = false;

    //queue the ts buffer
    mDataSource->queueBuffer(buffer);

#if DUMP_EACH_TS
    dumpTS(mSeqNumber, (char*)buffer->data(), buffer->size());
#endif

#if DUMP_PROFILE
    dumpProfile(mPlaylist, mSeqNumber);
#endif

    mPrevBandwidthIndex = bandwidthIndex;
    ++mSeqNumber;

    postMonitorQueue();
}

#else 

void LiveSession::onDownloadNext() {
    ALOGV("onDownloadNext, mSeqNumber=%d", mSeqNumber);
    size_t bandwidthIndex = getBandwidthIndex();

    int iRepeat = 0;
rinse_repeat:
    iRepeat ++;
    ALOGD("\t\t repeat %d", iRepeat - 1);
    int64_t nowUs = ALooper::GetNowUs();

    if (mLastPlaylistFetchTimeUs < 0
            || (ssize_t)bandwidthIndex != mPrevBandwidthIndex
            || (!mPlaylist->isComplete() && timeToRefreshPlaylist(nowUs))) {
        AString url;
        if (mBandwidthItems.size() > 0) {
            ALOGD("using [%u] bps items, bandwidth = %ld", bandwidthIndex, mBandwidthItems.editItemAt(bandwidthIndex).mBandwidth);
            url = mBandwidthItems.editItemAt(bandwidthIndex).mURI;
        } else {
            url = mMasterURL;
            ALOGD("using MasterURL %s", mMasterURL.c_str());
        }

        if ((ssize_t)bandwidthIndex != mPrevBandwidthIndex) {
            // If we switch bandwidths, do not pay any heed to whether
            // playlists changed since the last time...
            mPlaylist.clear();
        }

        bool unchanged;
        ALOGV("m3uparser init by url = %s", url.c_str());
        sp<M3UParser> playlist = fetchPlaylist(url.c_str(), &unchanged);
        if (playlist == NULL) {
            if (unchanged) {
                // We succeeded in fetching the playlist, but it was
                // unchanged from the last time we tried.
            } else {
               ALOGE("failed to load playlist at url '%s'", url.c_str());
                mDataSource->queueEOS(ERROR_IO);
                return;
            }
        } else {
            mPlaylist = playlist;
        }

        if (!mDurationFixed) {
            Mutex::Autolock autoLock(mLock);

            if (!mPlaylist->isComplete() && !mPlaylist->isEvent()) {
                ALOGD("playlist is not complete, duration = -1");
                mDurationUs = -1;
                mDurationFixed = true;
            } else {
                mDurationUs = 0;
                for (size_t i = 0; i < mPlaylist->size(); ++i) {
                    sp<AMessage> itemMeta;
                    CHECK(mPlaylist->itemAt(
                                i, NULL /* uri */, &itemMeta));

                    int64_t itemDurationUs;
                    CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

                    mDurationUs += itemDurationUs;
                }

                mDurationFixed = mPlaylist->isComplete();
                ALOGV("get duration from playlist , duration = %lld", mDurationUs);
            }
        }

        mLastPlaylistFetchTimeUs = ALooper::GetNowUs();
    }

    int32_t firstSeqNumberInPlaylist;
    if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                "media-sequence", &firstSeqNumberInPlaylist)) {
        firstSeqNumberInPlaylist = 0;
    }

    bool seekDiscontinuity = false;
    bool explicitDiscontinuity = false;
    bool bandwidthChanged = false;

    if (mSeekTimeUs >= 0) {
        if (mPlaylist->isComplete() || mPlaylist->isEvent()) {
            size_t index = 0;
            int64_t segmentStartUs = 0;
            while (index < mPlaylist->size()) {
                sp<AMessage> itemMeta;
                CHECK(mPlaylist->itemAt(
                            index, NULL /* uri */, &itemMeta));

                int64_t itemDurationUs;
                CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

                if (mSeekTimeUs < segmentStartUs + itemDurationUs) {
                    break;
                }

                segmentStartUs += itemDurationUs;
                ++index;
            }

            if (index < mPlaylist->size()) {
                int32_t newSeqNumber = firstSeqNumberInPlaylist + index;

                if (newSeqNumber != mSeqNumber) {
                    ALOGI("seeking to seq no %d", newSeqNumber);

                    mSeqNumber = newSeqNumber;

                    mDataSource->reset();

                    // reseting the data source will have had the
                    // side effect of discarding any previously queued
                    // bandwidth change discontinuity.
                    // Therefore we'll need to treat these seek
                    // discontinuities as involving a bandwidth change
                    // even if they aren't directly.
                    seekDiscontinuity = true;
                    bandwidthChanged = true;
                }
            }
        }

        mSeekTimeUs = -1;

        Mutex::Autolock autoLock(mLock);
        mSeekDone = true;
        mCondition.broadcast();
    }

    const int32_t lastSeqNumberInPlaylist =
        firstSeqNumberInPlaylist + (int32_t)mPlaylist->size() - 1;
    if (mSeqNumber < 0) {
        if (mPlaylist->isComplete()) {
            mSeqNumber = firstSeqNumberInPlaylist;
        } else {
            // If this is a live session, start 3 segments from the end.
            mSeqNumber = lastSeqNumberInPlaylist - 3;
            if (mSeqNumber < firstSeqNumberInPlaylist) {
                mSeqNumber = firstSeqNumberInPlaylist;
            }
        }
    }

    if (mSeqNumber < firstSeqNumberInPlaylist
            || mSeqNumber > lastSeqNumberInPlaylist) {
        if (mPrevBandwidthIndex != (ssize_t)bandwidthIndex) {
            // Go back to the previous bandwidth.

            ALOGI("new bandwidth does not have the sequence number (%d vs [%d, %d]) "
                 "we're looking for, switching back to previous bandwidth", 
                 mSeqNumber, firstSeqNumberInPlaylist, lastSeqNumberInPlaylist);

            mLastPlaylistFetchTimeUs = -1;
            bandwidthIndex = mPrevBandwidthIndex;
            goto rinse_repeat;
        }

        if (!mPlaylist->isComplete() && mNumRetries < kMaxNumRetries) {
            ALOGD("retrying %d", mNumRetries);
            ++mNumRetries;

            if (mSeqNumber > lastSeqNumberInPlaylist) {
                mLastPlaylistFetchTimeUs = -1;
                postMonitorQueue(3000000ll);
                return;
            }

            // we've missed the boat, let's start from the lowest sequence
            // number available and signal a discontinuity.

            ALOGI("We've missed the boat, restarting playback.");
            mSeqNumber = lastSeqNumberInPlaylist;
            explicitDiscontinuity = true;

            // fall through
        } else {
            ALOGE("Cannot find sequence number %d in playlist "
                 "(contains %d - %d)",
                 mSeqNumber, firstSeqNumberInPlaylist,
                 firstSeqNumberInPlaylist + mPlaylist->size() - 1);

            mDataSource->queueEOS(ERROR_END_OF_STREAM);
            return;
        }
    }

    mNumRetries = 0;

    AString uri;
    sp<AMessage> itemMeta;
    CHECK(mPlaylist->itemAt(
                mSeqNumber - firstSeqNumberInPlaylist,
                &uri,
                &itemMeta));

    int32_t val;
    if (itemMeta->findInt32("discontinuity", &val) && val != 0) {
        explicitDiscontinuity = true;
    }

    int64_t range_offset, range_length;
    if (!itemMeta->findInt64("range-offset", &range_offset)
            || !itemMeta->findInt64("range-length", &range_length)) {
        range_offset = 0;
        range_length = -1;
    }

    ALOGV("fetching segment %d from (%d .. %d)",
          mSeqNumber, firstSeqNumberInPlaylist, lastSeqNumberInPlaylist);

    sp<ABuffer> buffer;
    status_t err = fetchFile(uri.c_str(), &buffer, range_offset, range_length);
    if (err != OK) {
        ALOGE("failed(err = %d) to fetch .ts segment at url '%s'", err, uri.c_str());
        mDataSource->queueEOS(err);
        return;
    }

    CHECK(buffer != NULL);

    err = decryptBuffer(mSeqNumber - firstSeqNumberInPlaylist, buffer);

    if (err != OK) {
        ALOGE("decryptBuffer failed w/ error %d", err);

        mDataSource->queueEOS(err);
        return;
    }

    if (buffer->size() == 0 || buffer->data()[0] != 0x47) {
        // Not a transport stream???

        ALOGE("This doesn't look like a transport stream...");

        mBandwidthItems.removeAt(bandwidthIndex);

        if (mBandwidthItems.isEmpty()) {
            mDataSource->queueEOS(ERROR_UNSUPPORTED);
            return;
        }

        ALOGI("Retrying with a different bandwidth stream.");

        mLastPlaylistFetchTimeUs = -1;
        bandwidthIndex = getBandwidthIndex();
        mPrevBandwidthIndex = bandwidthIndex;
        mSeqNumber = -1;

        goto rinse_repeat;
    }

    if ((size_t)mPrevBandwidthIndex != bandwidthIndex) {
        bandwidthChanged = true;
    }

    if (mPrevBandwidthIndex < 0) {
        // Don't signal a bandwidth change at the very beginning of
        // playback.
        bandwidthChanged = false;
    }

    if (mStartOfPlayback) {
        seekDiscontinuity = true;
        mStartOfPlayback = false;
    }

    if (seekDiscontinuity || explicitDiscontinuity || bandwidthChanged) {
        // Signal discontinuity.

        ALOGI("queueing discontinuity (seek=%d, explicit=%d, bandwidthChanged=%d)",
             seekDiscontinuity, explicitDiscontinuity, bandwidthChanged);

        sp<ABuffer> tmp = new ABuffer(188);
        memset(tmp->data(), 0, tmp->size());

        // signal a 'hard' discontinuity for explicit or bandwidthChanged.
        uint8_t type = (explicitDiscontinuity || bandwidthChanged) ? 1 : 0;

        if (mPlaylist->isComplete() || mPlaylist->isEvent()) {
            // If this was a live event this made no sense since
            // we don't have access to all the segment before the current
            // one.
            int64_t segmentStartTimeUs = getSegmentStartTimeUs(mSeqNumber);
            memcpy(tmp->data() + 2, &segmentStartTimeUs, sizeof(segmentStartTimeUs));

            type |= 2;
        }

        tmp->data()[1] = type;

        mDataSource->queueBuffer(tmp);
    }

    mDataSource->queueBuffer(buffer);

    mPrevBandwidthIndex = bandwidthIndex;
    ++mSeqNumber;

    postMonitorQueue();
}

#endif

void LiveSession::onMonitorQueue() {
    if (
#ifndef ANDROID_DEFAULT_CODE
#else
            mSeekTimeUs >= 0 ||
#endif
             mDataSource->countQueuedBuffers() < kMaxNumQueuedFragments
        ) {
        onDownloadNext();
    } else {
        postMonitorQueue(1000000ll);
    }
}

status_t LiveSession::decryptBuffer(
        size_t playlistIndex, const sp<ABuffer> &buffer) {
    sp<AMessage> itemMeta;
    bool found = false;
    AString method;

    for (ssize_t i = playlistIndex; i >= 0; --i) {
        AString uri;
        CHECK(mPlaylist->itemAt(i, &uri, &itemMeta));

        if (itemMeta->findString("cipher-method", &method)) {
            found = true;
            break;
        }
    }

    if (!found) {
        method = "NONE";
    }

    if (method == "NONE") {
        return OK;
    } else if (!(method == "AES-128")) {
        ALOGE("Unsupported cipher method '%s'", method.c_str());
        return ERROR_UNSUPPORTED;
    }

    AString keyURI;
    if (!itemMeta->findString("cipher-uri", &keyURI)) {
        ALOGE("Missing key uri");
        return ERROR_MALFORMED;
    }

    ssize_t index = mAESKeyForURI.indexOfKey(keyURI);

    sp<ABuffer> key;
    if (index >= 0) {
        key = mAESKeyForURI.valueAt(index);
    } else {
        key = new ABuffer(16);

        sp<HTTPBase> keySource =
              HTTPBase::Create(
                  (mFlags & kFlagIncognito)
                    ? HTTPBase::kFlagIncognito
                    : 0);

        if (mUIDValid) {
            keySource->setUID(mUID);
        }

        status_t err =
            keySource->connect(
                    keyURI.c_str(),
                    mExtraHeaders.isEmpty() ? NULL : &mExtraHeaders);

        if (err == OK) {
            size_t offset = 0;
            while (offset < 16) {
                ssize_t n = keySource->readAt(
                        offset, key->data() + offset, 16 - offset);
                if (n <= 0) {
                    err = ERROR_IO;
                    break;
                }

                offset += n;
            }
        }

        if (err != OK) {
            ALOGE("failed to fetch cipher key from '%s'.", keyURI.c_str());
            return ERROR_IO;
        }

        mAESKeyForURI.add(keyURI, key);
    }

    AES_KEY aes_key;
    if (AES_set_decrypt_key(key->data(), 128, &aes_key) != 0) {
        ALOGE("failed to set AES decryption key.");
        return UNKNOWN_ERROR;
    }

    unsigned char aes_ivec[16];

    AString iv;
    if (itemMeta->findString("cipher-iv", &iv)) {
        if ((!iv.startsWith("0x") && !iv.startsWith("0X"))
                || iv.size() != 16 * 2 + 2) {
            ALOGE("malformed cipher IV '%s'.", iv.c_str());
            return ERROR_MALFORMED;
        }

        memset(aes_ivec, 0, sizeof(aes_ivec));
        for (size_t i = 0; i < 16; ++i) {
            char c1 = tolower(iv.c_str()[2 + 2 * i]);
            char c2 = tolower(iv.c_str()[3 + 2 * i]);
            if (!isxdigit(c1) || !isxdigit(c2)) {
                ALOGE("malformed cipher IV '%s'.", iv.c_str());
                return ERROR_MALFORMED;
            }
            uint8_t nibble1 = isdigit(c1) ? c1 - '0' : c1 - 'a' + 10;
            uint8_t nibble2 = isdigit(c2) ? c2 - '0' : c2 - 'a' + 10;

            aes_ivec[i] = nibble1 << 4 | nibble2;
        }
    } else {
        memset(aes_ivec, 0, sizeof(aes_ivec));
        aes_ivec[15] = mSeqNumber & 0xff;
        aes_ivec[14] = (mSeqNumber >> 8) & 0xff;
        aes_ivec[13] = (mSeqNumber >> 16) & 0xff;
        aes_ivec[12] = (mSeqNumber >> 24) & 0xff;
    }

    AES_cbc_encrypt(
            buffer->data(), buffer->data(), buffer->size(),
            &aes_key, aes_ivec, AES_DECRYPT);

    // hexdump(buffer->data(), buffer->size());

    size_t n = buffer->size();
    CHECK_GT(n, 0u);

    size_t pad = buffer->data()[n - 1];

    CHECK_GT(pad, 0u);
    CHECK_LE(pad, 16u);
    CHECK_GE((size_t)n, pad);
    for (size_t i = 0; i < pad; ++i) {
        CHECK_EQ((unsigned)buffer->data()[n - 1 - i], pad);
    }

    n -= pad;

    buffer->setRange(buffer->offset(), n);

    return OK;
}

void LiveSession::postMonitorQueue(int64_t delayUs) {
    sp<AMessage> msg = new AMessage(kWhatMonitorQueue, id());
    msg->setInt32("generation", ++mMonitorQueueGeneration);
    msg->post(delayUs);
}

void LiveSession::onSeek(const sp<AMessage> &msg) {
    int64_t timeUs;
    CHECK(msg->findInt64("timeUs", &timeUs));

#ifndef ANDROID_DEFAULT_CODE
    Mutex::Autolock autoLock(mLock);
#endif
    mSeekTimeUs = timeUs;
#ifndef ANDROID_DEFAULT_CODE
    if (mPlaylist != NULL && 
            (mPlaylist->isComplete() || mPlaylist->isEvent())) {
        size_t index = 0;
        int64_t segmentStartUs = 0;
        while (index < mPlaylist->size()) {
            sp<AMessage> itemMeta;
            CHECK(mPlaylist->itemAt(
                        index, NULL /* uri */, &itemMeta));

            int64_t itemDurationUs;
            CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

            if (mSeekTimeUs < segmentStartUs + itemDurationUs) {
                break;
            }

            segmentStartUs += itemDurationUs;
            ++index;
        }

        if (index < mPlaylist->size()) {
            int32_t newSeqNumber = mFirstSeqNumber + index;
            if (newSeqNumber == mSeqNumber) {
                ALOGD("Seek not required current seq %d", mSeqNumber);
                mSeekTimeUs = -1;
            } else {
                ALOGI("seeking to seq no %d", newSeqNumber);

                mSeqNumber = newSeqNumber;
                mDataSource->reset();
                mSeekTimeUs = segmentStartUs;
                ALOGD("Seeking to seq %d new seek time %0.2f secs", newSeqNumber, mSeekTimeUs/1E6);

            }
        } else {
             ALOGE("seek outrange index = %d, size = %d", index, mPlaylist->size());
		     mDataSource->reset();
             mDataSource->queueEOS(ERROR_END_OF_STREAM);
             mSeeking = false;
             mCondition.broadcast();
       	     return;
       	}

        mSeeking = false;
        mWasSeeking = true;
    } else {
        mSeekTimeUs = -1;
        if (mPlaylist != NULL) {
            ALOGI("Seeking Live Streams is not supported");
        } else {
            ALOGE("onSeek error: Playlist is NULL");
        }
        mSeeking = false;
    }
    mCondition.broadcast();

#endif
    postMonitorQueue();
}

status_t LiveSession::getDuration(int64_t *durationUs) const {
    Mutex::Autolock autoLock(mLock);
    *durationUs = mDurationUs;

    return OK;
}

bool LiveSession::isSeekable() const {
    int64_t durationUs;
    return getDuration(&durationUs) == OK && durationUs >= 0;
}

bool LiveSession::hasDynamicDuration() const {
    return !mDurationFixed;
}

#ifndef ANDROID_DEFAULT_CODE



void LiveSession::dumpBandwidthItems(Vector<BandwidthItem>* pItems) {
    ALOGD("dumping bandwidthItem * %d -------", pItems->size());
    for (size_t i = 0; i < pItems->size(); i ++) {
        ALOGD("\tBandwidthItem[%d], %d(%d complained), %s", i, 
                pItems->itemAt(i).mBandwidth, pItems->itemAt(i).mComplained, pItems->itemAt(i).mURI.c_str());
    }
}

void LiveSession::dumpTS(int32_t nSeq, char* pBuffer, size_t nBufSize) {
    AString strFileName = StringPrintf("/sdcard/dump/hls%d-%d.ts", mDiscontinuityNumber, nSeq);
    FILE* file = fopen(strFileName.c_str(), "wb");
    if (file != NULL) {
        fwrite(pBuffer, 1, nBufSize, file);
        fclose(file);

    } else {
        ALOGW("fail to dump file %s", strFileName.c_str());
    }

}

void LiveSession::dumpProfile(const sp<M3UParser> &playlist, int32_t seqnum) {

    //live session is not supported yet
    if (!playlist->isComplete())
        return;

    //calculate current timestamp

    size_t index = 0;
    int64_t currentUs = 0;

    CHECK(seqnum < playlist->size());
    for (int i = 0; i < seqnum; i ++) {
        sp<AMessage> itemMeta;
        CHECK(playlist->itemAt(
                    index, NULL /* uri */, &itemMeta));

        int64_t itemDurationUs;
        CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));
        currentUs += itemDurationUs;
    }
    ALOGD("[dump] av download %.2f", currentUs / 1E6);



}

bool LiveSession::alternateBandwidth(const status_t error, size_t *index) {
    int i = (int)(*index);
    mBandwidthItems.removeAt(i);

    if (mBandwidthItems.isEmpty()) {
        return false;
    }

    if ((mPrevBandwidthIndex >= 0) && (i != mPrevBandwidthIndex)) {
        *index = mPrevBandwidthIndex;
        return true;
    }

    if (error == ERROR_OUT_OF_RANGE) {//out_of_range reason: http status of 404 from server
        i = getBandwidthIndex();
    } else if (error == ERROR_MALFORMED) {
        i = getBandwidthIndex();
    } else {
        return false;
    }

    *index = i;
    return true;
    
    
}


bool LiveSession::removeSpecificHeaders(const String8 MyKey, KeyedVector<String8, String8> *headers, String8 *pMyHeader) {
    ALOGD("removeSpecificHeaders %s", MyKey.string());
    if (headers != NULL) {
        ssize_t index;
        if ((index = headers->indexOfKey(MyKey)) >= 0) {
            ALOGD("special headers: %s = %s", MyKey.string(), (headers->valueAt(index)).string());
            if (pMyHeader != NULL)
                *pMyHeader = headers->valueAt(index);
            headers->removeItemsAt(index);
            return true;
        }
    }
    return false;
}

void LiveSession::complainBandwidth(Vector<BandwidthItem>* bandwidthItems, unsigned long bandwidth) {
    size_t i = 0;
    if (bandwidthItems->size() == 0)
        return;
    for (i = 0; i < bandwidthItems->size(); i++) {
        if (bandwidth == bandwidthItems->itemAt(i).mBandwidth) {
            bandwidthItems->editItemAt(i).mComplained ++;
            ALOGD("@debug: bandwidth %d is complained %d", 
                    bandwidthItems->itemAt(i).mBandwidth, bandwidthItems->itemAt(i).mComplained);
            return;
        }
    }
    TRESPASS();
}

#endif


}  // namespace android

