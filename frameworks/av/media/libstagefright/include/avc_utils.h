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

#ifndef AVC_UTILS_H_

#define AVC_UTILS_H_

#include <media/stagefright/foundation/ABuffer.h>
#include <utils/Errors.h>

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_S3D_SUPPORT
#include <media/stagefright/MetaData.h>
#endif
#endif
namespace android {

struct ABitReader;

#ifndef ANDROID_DEFAULT_CODE 
struct SPSInfo {
	int32_t width;
	int32_t height;
	uint32_t profile;
	uint32_t level;
} ;

#ifdef MTK_S3D_SUPPORT
struct SEIInfo {
	uint32_t payload_type;
	void *pvalue;
};
#endif

signed parseSE(ABitReader *br);
status_t FindAVCSPSInfo(
		uint8_t *seqParamSet, size_t size, struct SPSInfo *pSPSInfo);


#endif
enum {
    kAVCProfileBaseline      = 0x42,
    kAVCProfileMain          = 0x4d,
    kAVCProfileExtended      = 0x58,
    kAVCProfileHigh          = 0x64,
    kAVCProfileHigh10        = 0x6e,
    kAVCProfileHigh422       = 0x7a,
    kAVCProfileHigh444       = 0xf4,
    kAVCProfileCAVLC444Intra = 0x2c
};

void FindAVCDimensions(
        const sp<ABuffer> &seqParamSet, int32_t *width, int32_t *height);

unsigned parseUE(ABitReader *br);

status_t getNextNALUnit(
        const uint8_t **_data, size_t *_size,
        const uint8_t **nalStart, size_t *nalSize,
        bool startCodeFollows = false);

struct MetaData;
sp<MetaData> MakeAVCCodecSpecificData(const sp<ABuffer> &accessUnit);
void MakeAVCCodecSpecificData2(const sp<ABuffer> &accessUnit, sp<MetaData> meta); 

bool IsIDR(const sp<ABuffer> &accessUnit);
bool IsAVCReferenceFrame(const sp<ABuffer> &accessUnit);

const char *AVCProfileToString(uint8_t profile);

sp<MetaData> MakeAACCodecSpecificData(
        unsigned profile, unsigned sampling_freq_index,
        unsigned channel_configuration);

// Given an MPEG4 video VOL-header chunk (starting with 0x00 0x00 0x01 0x2?)
// parse it and fill in dimensions, returns true iff successful.
bool ExtractDimensionsFromVOLHeader(
        const uint8_t *data, size_t size, int32_t *width, int32_t *height);

bool GetMPEGAudioFrameSize(
        uint32_t header, size_t *frame_size,
        int *out_sampling_rate = NULL, int *out_channels = NULL,
        int *out_bitrate = NULL, int *out_num_samples = NULL);

#ifndef ANDROID_DEFAULT_CODE
struct MPEG4Info {
    int progressive;
    int cpcf;
};
int findVOLHeader(const uint8_t *start, int length);
int decodeVOLHeader(const uint8_t *vol, size_t size, struct MPEG4Info* s);
int decodeShortHeader(const uint8_t *vol, size_t size, struct MPEG4Info* s);
#ifdef MTK_S3D_SUPPORT
status_t ParseSpecificSEI(const uint8_t* sei, uint32_t size, struct SEIInfo *s);
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE
}  // namespace android

#endif  // AVC_UTILS_H_
