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

#ifndef ES_QUEUE_H_

#define ES_QUEUE_H_

#include <media/stagefright/foundation/ABase.h>
#include <utils/Errors.h>
#include <utils/List.h>
#include <utils/RefBase.h>

namespace android {

struct ABuffer;
struct MetaData;

struct ElementaryStreamQueue {
    enum Mode {
        H264,
        AAC,
#ifndef ANDROID_DEFAULT_CODE
        PSLPCM,			  //mtk02420
        VORBIS_AUDIO,
        LPCM,          
        BDLPCM,           
        VC1_VIDEO,
#endif
        MPEG_AUDIO,
        MPEG_VIDEO,
        MPEG4_VIDEO,
        PCM_AUDIO,
    };

    enum Flags {
        // Data appended to the queue is always at access unit boundaries.
        kFlag_AlignedData = 1,
    };
    ElementaryStreamQueue(Mode mode, uint32_t flags = 0);

    status_t appendData(const void *data, size_t size, int64_t timeUs, bool fgInvalidPTS=false);
    void clear(bool clearFormat);

    sp<ABuffer> dequeueAccessUnit();

    sp<MetaData> getFormat();

#ifndef ANDROID_DEFAULT_CODE
    void setSeeking(bool h264UsePPs=false);

    #ifdef MTK_OGM_PLAYBACK_SUPPORT //for tablet only
    void setSearchSCOptimize(bool fgEnable);
    #endif
#endif
private:
    struct RangeInfo {
        int64_t mTimestampUs;
        size_t mLength;
        bool mInvalidTimestamp;
    };

    Mode mMode;
    uint32_t mFlags;

    sp<ABuffer> mBuffer;
    List<RangeInfo> mRangeInfos;
#ifndef ANDROID_DEFAULT_CODE
    List< sp<ABuffer> > accessUnits;//[qian] H264: a nal is a AU
    bool mSeeking;
    int64_t mAudioFrameDuration;
    int32_t mMP3Header;
    int8_t mVorbisStatus;
    struct PCM_Header{
	char sub_stream_id;
	char number_of_frame_header;
	int	reserved:7;
	int	audio_emphasis_flag:1;
	int number_of_audio_channel:3;
	int audio_sampling_frequency:3;
	int quantization_word_length:2;
    } ;
    #ifdef MTK_OGM_PLAYBACK_SUPPORT //for tablet only
    bool mfgSearchStartCodeOptimize;
    #endif
    bool mH264UsePPs;
    bool mfgFirstFrmAfterSeek; //start to send AU at I frame with valid PTS after seek
#endif
    sp<MetaData> mFormat;

    sp<ABuffer> dequeueAccessUnitH264();
    sp<ABuffer> dequeueAccessUnitAAC();
#ifndef ANDROID_DEFAULT_CODE
    sp<ABuffer> dequeueAccessUnitPSLPCM();
    sp<ABuffer> dequeueAccessUnitVORBISAudio();
    sp<ABuffer> dequeueAccessUnitLPCM();
    sp<ABuffer> dequeueAccessUnitBDLPCM();
    bool IsIFrame(uint8_t *nalStart, size_t nalSize);
    sp<ABuffer> dequeueAccessUnitVC1Video();
#endif
    sp<ABuffer> dequeueAccessUnitMPEGAudio();
    sp<ABuffer> dequeueAccessUnitMPEGVideo();
    sp<ABuffer> dequeueAccessUnitMPEG4Video();
    sp<ABuffer> dequeueAccessUnitPCMAudio();

    // consume a logical (compressed) access unit of size "size",
    // returns its timestamp in us (or -1 if no time information).
    int64_t fetchTimestamp(size_t size, bool* pfgInvalidPTS=NULL);

    DISALLOW_EVIL_CONSTRUCTORS(ElementaryStreamQueue);
};

}  // namespace android

#endif  // ES_QUEUE_H_
