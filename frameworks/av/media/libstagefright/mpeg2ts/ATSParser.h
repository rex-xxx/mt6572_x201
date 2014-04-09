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

#ifndef A_TS_PARSER_H_

#define A_TS_PARSER_H_

#include <sys/types.h>

#include <media/stagefright/foundation/ABase.h>
#include <media/stagefright/foundation/AMessage.h>
#include <utils/KeyedVector.h>
#include <utils/Vector.h>
#include <utils/RefBase.h>

namespace android {

struct ABitReader;
struct ABuffer;
struct MediaSource;

struct ATSParser : public RefBase {
    enum DiscontinuityType {
        DISCONTINUITY_NONE              = 0,
        DISCONTINUITY_TIME              = 1,
        DISCONTINUITY_AUDIO_FORMAT      = 2,
        DISCONTINUITY_VIDEO_FORMAT      = 4,
        DISCONTINUITY_ABSOLUTE_TIME     = 8,

        DISCONTINUITY_SEEK              = DISCONTINUITY_TIME,

        // For legacy reasons this also implies a time discontinuity.
        DISCONTINUITY_FORMATCHANGE      =
            DISCONTINUITY_AUDIO_FORMAT
                | DISCONTINUITY_VIDEO_FORMAT
                | DISCONTINUITY_TIME,
#ifndef ANDROID_DEFAULT_CODE
        DISCONTINUITY_FLUSH_SOURCE_ONLY             = 0x80000000,
        DISCONTINUITY_FLUSH_SOURCE_AND_CLEAR_FORMAT       = DISCONTINUITY_FLUSH_SOURCE_ONLY | DISCONTINUITY_VIDEO_FORMAT | DISCONTINUITY_AUDIO_FORMAT,
            DISCONTINUITY_HTTPLIVE_SEEK = 0x40000000,
#endif
    };

    enum Flags {
        // The 90kHz clock (PTS/DTS) is absolute, i.e. PTS=0 corresponds to
        // a media time of 0.
        // If this flag is _not_ specified, the first PTS encountered in a
        // program of this stream will be assumed to correspond to media time 0
        // instead.
        TS_TIMESTAMPS_ARE_ABSOLUTE = 1,
        // Video PES packets contain exactly one (aligned) access unit.
        ALIGNED_VIDEO_DATA         = 2,
   #ifndef ANDROID_DEFAULT_CODE
   	 TS_SOURCE_IS_LOCAL              = 0x40000000,
        TS_SOURCE_IS_STREAMING          = 0x80000000,
   #endif
    };

    ATSParser(uint32_t flags = 0);

    status_t feedTSPacket(const void *data, size_t size);

#ifndef ANDROID_DEFAULT_CODE
    void signalDiscontinuity(
			DiscontinuityType type,const sp<AMessage> &extra = NULL);
#else
	void signalDiscontinuity(
			DiscontinuityType type, const sp<AMessage> &extra);
#endif

    void signalEOS(status_t finalResult);
#ifndef ANDROID_DEFAULT_CODE
    void setQueue(bool isQueue) ;
    int64_t getMaxPTS();
    bool firstPTSIsValid();
    bool findPAT(const void *data, size_t size);
    bool getDequeueState();
    void setDequeueState(bool needDequeuePES);
#endif

    enum SourceType {
        VIDEO,
        AUDIO
    };
    sp<MediaSource> getSource(SourceType type);

    bool PTSTimeDeltaEstablished();

    enum {
        // From ISO/IEC 13818-1: 2000 (E), Table 2-29
        STREAMTYPE_RESERVED             = 0x00,
        STREAMTYPE_MPEG1_VIDEO          = 0x01,
        STREAMTYPE_MPEG2_VIDEO          = 0x02,
        STREAMTYPE_MPEG1_AUDIO          = 0x03,
        STREAMTYPE_MPEG2_AUDIO          = 0x04,
        STREAMTYPE_MPEG2_AUDIO_ADTS     = 0x0f,
        STREAMTYPE_MPEG4_VIDEO          = 0x10,
        STREAMTYPE_H264                 = 0x1b,
#ifndef ANDROID_DEFAULT_CODE
        STREAMTYPE_AUDIO_PSLPCM            = 0xa0,
        STREAMTYPE_AUDIO_BDLPCM            = 0x80,
	STREAMTYPE_VC1_VIDEO          = 0xea,
#endif
        STREAMTYPE_PCM_AUDIO            = 0x83,
    };

protected:
    virtual ~ATSParser();

private:
    struct Program;
    struct Stream;
    struct PSISection;

    uint32_t mFlags;
    Vector<sp<Program> > mPrograms;

#ifndef ANDROID_DEFAULT_CODE
    bool mNeedDequeuePES;
#endif
    // Keyed by PID
    KeyedVector<unsigned, sp<PSISection> > mPSISections;

    int64_t mAbsoluteTimeAnchorUs;

    size_t mNumTSPacketsParsed;

    void parseProgramAssociationTable(ABitReader *br);
    void parseProgramMap(ABitReader *br);
    void parsePES(ABitReader *br);

    status_t parsePID(
        ABitReader *br, unsigned PID,
        unsigned continuity_counter,
        unsigned payload_unit_start_indicator);

    void parseAdaptationField(ABitReader *br, unsigned PID);
    status_t parseTS(ABitReader *br);

    void updatePCR(unsigned PID, uint64_t PCR, size_t byteOffsetFromStart);

    uint64_t mPCR[2];
    size_t mPCRBytes[2];
    int64_t mSystemTimeUs[2];
    size_t mNumPCRs;

    DISALLOW_EVIL_CONSTRUCTORS(ATSParser);
};

}  // namespace android

#endif  // A_TS_PARSER_H_
