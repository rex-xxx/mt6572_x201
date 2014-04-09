/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef MPEG2_PS_EXTRACTOR_H_

#define MPEG2_PS_EXTRACTOR_H_

#include <media/stagefright/foundation/ABase.h>
#include <media/stagefright/MediaExtractor.h>
#include <utils/threads.h>
#include <utils/KeyedVector.h>

namespace android {

struct ABuffer;
struct AMessage;
struct Track;
struct String8;

struct MPEG2PSExtractor : public MediaExtractor {
    MPEG2PSExtractor(const sp<DataSource> &source);

    virtual size_t countTracks();
    virtual sp<MediaSource> getTrack(size_t index);
    virtual sp<MetaData> getTrackMetaData(size_t index, uint32_t flags);

    virtual sp<MetaData> getMetaData();

    virtual uint32_t flags() const;
#ifndef ANDROID_DEFAULT_CODE
	bool bisPlayable;
    virtual ~MPEG2PSExtractor();
#else
protected:
    virtual ~MPEG2PSExtractor();
#endif

private:
    struct Track;
    struct WrappedTrack;

    mutable Mutex mLock;
    sp<DataSource> mDataSource;

    off64_t mOffset;
    status_t mFinalResult;
    sp<ABuffer> mBuffer;
    KeyedVector<unsigned, sp<Track> > mTracks;
    bool mScanning;

    bool mProgramStreamMapValid;
    KeyedVector<unsigned, unsigned> mStreamTypeByESID;

    #ifndef ANDROID_DEFAULT_CODE
    int64_t mDurationUs;
    int64_t mSeekTimeUs;
    bool mSeeking;
    uint64_t mMaxcount;
    off64_t mSeekingOffset;
    off64_t mFileSize;
    off64_t mMinOffset;
    off64_t mMaxOffset;   	
    unsigned mSeekStreamID;
	off64_t mlastValidPESSCOffset; 
	bool mIsCrossChunk;
    bool mMPEG1Flag;
	bool mhasVTrack;
	bool mhasATrack;
	int64_t mSearchPTS;
    off64_t mSearchPTSOffset;
    off64_t mAverageByteRate;
    bool mSystemHeaderValid;
    bool mParseMaxTime;
    #endif

    #ifndef ANDROID_DEFAULT_CODE
    bool mNeedDequeuePES;
    #endif //ANDROID_DEFAULT_CODE

    #ifndef ANDROID_DEFAULT_CODE   //MTK Function
    void setDequeueState(bool needDequeuePES);
    bool getDequeueState();
    int64_t getMaxPTS();
    int64_t getMaxVideoPTS();
    void seekTo(int64_t seekTimeUs, unsigned StreamID);
    void parseMaxPTS();
    uint64_t getDurationUs();
    void init();
    bool getSeeking();
    void signalDiscontinuity();
	int findNextPES(const void* data,int length);
	int64_t getLastPESWithIFrame(off64_t end);
	int64_t getNextPESWithIFrame(off64_t begin);
    int64_t SearchPES(const void* data, int size);   
    int64_t SearchValidOffset(off64_t currentoffset);
    bool IsSeeminglyValidADTSHeader(const uint8_t *ptr, size_t size);
    bool IsSeeminglyValidMPEGAudioHeader(const uint8_t *ptr, size_t size); 
	//status_t getNextNALUnit_local(uint8_t **_data, size_t *_size, const uint8_t **nalStart, size_t *nalSize, bool startCodeFollows=false);
    #endif //ANDROID_DEFAULT_CODE

    status_t feedMore();

    status_t dequeueChunk();
    ssize_t dequeuePack();
    ssize_t dequeueSystemHeader();
    ssize_t dequeuePES();

    DISALLOW_EVIL_CONSTRUCTORS(MPEG2PSExtractor);
};

bool SniffMPEG2PS(
        const sp<DataSource> &source, String8 *mimeType, float *confidence,
        sp<AMessage> *);

}  // namespace android

#endif  // MPEG2_PS_EXTRACTOR_H_

