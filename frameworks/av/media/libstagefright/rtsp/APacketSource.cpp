/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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
#define LOG_TAG "APacketSource"
#include <utils/Log.h>

#include "APacketSource.h"

#include "ARawAudioAssembler.h"
#include "ASessionDescription.h"

#include "avc_utils.h"

#include <ctype.h>

#include <media/stagefright/foundation/ABitReader.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/foundation/base64.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MetaData.h>
#include <utils/Vector.h>
#ifndef ANDROID_DEFAULT_CODE
#include <media/stagefright/MediaBuffer.h>

static int kMaxInputSizeAMR = 2000;

static int kWholeBufSize = 40000000; //40Mbytes
static int kTargetTime = 2000;  //ms

#endif // #ifndef ANDROID_DEFAULT_CODE

namespace android {

#ifndef ANDROID_DEFAULT_CODE 
static bool GetAttribute(const char *s, const char *key, AString *value, bool checkExist = false) {
#else
static bool GetAttribute(const char *s, const char *key, AString *value) {
#endif // #ifndef ANDROID_DEFAULT_CODE
    value->clear();

    size_t keyLen = strlen(key);

    for (;;) {
        while (isspace(*s)) {
            ++s;
        }

        const char *colonPos = strchr(s, ';');

        size_t len =
            (colonPos == NULL) ? strlen(s) : colonPos - s;

        if (len >= keyLen + 1 && s[keyLen] == '=' && !strncmp(s, key, keyLen)) {
            value->setTo(&s[keyLen + 1], len - keyLen - 1);
            return true;
        }

#ifndef ANDROID_DEFAULT_CODE 
        if (checkExist && len == keyLen && !strncmp(s, key, keyLen)) {
            value->setTo("1");
            return true;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE

        if (colonPos == NULL) {
            return false;
        }

        s = colonPos + 1;
    }
}

static sp<ABuffer> decodeHex(const AString &s) {
    if ((s.size() % 2) != 0) {
        return NULL;
    }

    size_t outLen = s.size() / 2;
    sp<ABuffer> buffer = new ABuffer(outLen);
    uint8_t *out = buffer->data();

    uint8_t accum = 0;
    for (size_t i = 0; i < s.size(); ++i) {
        char c = s.c_str()[i];
        unsigned value;
        if (c >= '0' && c <= '9') {
            value = c - '0';
        } else if (c >= 'a' && c <= 'f') {
            value = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            value = c - 'A' + 10;
        } else {
            return NULL;
        }

        accum = (accum << 4) | value;

        if (i & 1) {
            *out++ = accum;

            accum = 0;
        }
    }

    return buffer;
}

static sp<ABuffer> MakeAVCCodecSpecificData(
        const char *params, int32_t *width, int32_t *height) {
    *width = 0;
    *height = 0;

    AString val;
    if (!GetAttribute(params, "profile-level-id", &val)) {
#ifndef ANDROID_DEFAULT_CODE
        ALOGW("no profile-level-id is found");
        val.setTo("4DE01E");
#else
        return NULL;
#endif
    }

    sp<ABuffer> profileLevelID = decodeHex(val);
    CHECK(profileLevelID != NULL);
    CHECK_EQ(profileLevelID->size(), 3u);

    Vector<sp<ABuffer> > paramSets;

    size_t numSeqParameterSets = 0;
    size_t totalSeqParameterSetSize = 0;
    size_t numPicParameterSets = 0;
    size_t totalPicParameterSetSize = 0;

    if (!GetAttribute(params, "sprop-parameter-sets", &val)) {
        return NULL;
    }

    size_t start = 0;
    for (;;) {
        ssize_t commaPos = val.find(",", start);
        size_t end = (commaPos < 0) ? val.size() : commaPos;

        AString nalString(val, start, end - start);
        sp<ABuffer> nal = decodeBase64(nalString);
        CHECK(nal != NULL);
        CHECK_GT(nal->size(), 0u);
        CHECK_LE(nal->size(), 65535u);

        uint8_t nalType = nal->data()[0] & 0x1f;
        if (numSeqParameterSets == 0) {
            CHECK_EQ((unsigned)nalType, 7u);
        } else if (numPicParameterSets > 0) {
            CHECK_EQ((unsigned)nalType, 8u);
        }
        if (nalType == 7) {
            ++numSeqParameterSets;
            totalSeqParameterSetSize += nal->size();
        } else  {
            CHECK_EQ((unsigned)nalType, 8u);
            ++numPicParameterSets;
            totalPicParameterSetSize += nal->size();
        }

        paramSets.push(nal);

        if (commaPos < 0) {
            break;
        }

        start = commaPos + 1;
    }

    CHECK_LT(numSeqParameterSets, 32u);
    CHECK_LE(numPicParameterSets, 255u);

    size_t csdSize =
        1 + 3 + 1 + 1
        + 2 * numSeqParameterSets + totalSeqParameterSetSize
        + 1 + 2 * numPicParameterSets + totalPicParameterSetSize;

    sp<ABuffer> csd = new ABuffer(csdSize);
    uint8_t *out = csd->data();

    *out++ = 0x01;  // configurationVersion
    memcpy(out, profileLevelID->data(), 3);
    out += 3;
    *out++ = (0x3f << 2) | 1;  // lengthSize == 2 bytes
    *out++ = 0xe0 | numSeqParameterSets;

    for (size_t i = 0; i < numSeqParameterSets; ++i) {
        sp<ABuffer> nal = paramSets.editItemAt(i);

        *out++ = nal->size() >> 8;
        *out++ = nal->size() & 0xff;

        memcpy(out, nal->data(), nal->size());

        out += nal->size();

        if (i == 0) {
            FindAVCDimensions(nal, width, height);
            ALOGI("dimensions %dx%d", *width, *height);
        }
    }

    *out++ = numPicParameterSets;

    for (size_t i = 0; i < numPicParameterSets; ++i) {
        sp<ABuffer> nal = paramSets.editItemAt(i + numSeqParameterSets);

        *out++ = nal->size() >> 8;
        *out++ = nal->size() & 0xff;

        memcpy(out, nal->data(), nal->size());

        out += nal->size();
    }

    // hexdump(csd->data(), csd->size());

    return csd;
}

#ifndef ANDROID_DEFAULT_CODE 
static bool checkAACConfig(uint8_t *config) {
    int aacObjectType = config[0] >> 3;
    if ((aacObjectType != 2)    // AAC LC (Low Complexity) 
            && (aacObjectType != 4)      // AAC LTP (Long Term Prediction)
            && (aacObjectType != 5)      // SBR (Spectral Band Replication) 
            && (aacObjectType != 29))   // PS (Parametric Stereo)          
    {
        ALOGE ("[AAC capability error]Unsupported audio object type: (%d), , ignore audio track", aacObjectType);
        return false;
    }
    ALOGI("aacObjectType %d", aacObjectType);
    return true;
}
#endif // #ifndef ANDROID_DEFAULT_CODE

sp<ABuffer> MakeAACCodecSpecificData(const char *params) {
    AString val;
    CHECK(GetAttribute(params, "config", &val));

    sp<ABuffer> config = decodeHex(val);
    CHECK(config != NULL);
    CHECK_GE(config->size(), 4u);

    const uint8_t *data = config->data();
    uint32_t x = data[0] << 24 | data[1] << 16 | data[2] << 8 | data[3];
    x = (x >> 1) & 0xffff;

    static const uint8_t kStaticESDS[] = {
        0x03, 22,
        0x00, 0x00,     // ES_ID
        0x00,           // streamDependenceFlag, URL_Flag, OCRstreamFlag

        0x04, 17,
        0x40,                       // Audio ISO/IEC 14496-3
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,

        0x05, 2,
        // AudioSpecificInfo follows
    };

#ifndef ANDROID_DEFAULT_CODE
    if ((((x >> 8) & 0xff) >> 3) == 5) {
        CHECK_GE(config->size(), 6u);
        x = data[2] << 24 | data[3] << 16 | data[4] << 8 | data[5];
        x = (x >> 1);
        ALOGI("sbr detected %x", x);

        sp<ABuffer> csd = new ABuffer(sizeof(kStaticESDS) + 4);
        memcpy(csd->data(), kStaticESDS, sizeof(kStaticESDS));
        csd->data()[1] += 2;
        csd->data()[6] += 2;
        csd->data()[21] += 2;
        csd->data()[sizeof(kStaticESDS)] = (x >> 24) & 0xff;
        csd->data()[sizeof(kStaticESDS) + 1] = (x >> 16) & 0xff;
        csd->data()[sizeof(kStaticESDS) + 2] = (x >> 8) & 0xff;
        csd->data()[sizeof(kStaticESDS) + 3] = x & 0xff;

        return csd;
    }
#endif

    sp<ABuffer> csd = new ABuffer(sizeof(kStaticESDS) + 2);
    memcpy(csd->data(), kStaticESDS, sizeof(kStaticESDS));
    csd->data()[sizeof(kStaticESDS)] = (x >> 8) & 0xff;
    csd->data()[sizeof(kStaticESDS) + 1] = x & 0xff;

#ifndef ANDROID_DEFAULT_CODE 
    if (!checkAACConfig(csd->data() + sizeof(kStaticESDS))) {
        return NULL;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE
    // hexdump(csd->data(), csd->size());

    return csd;
}

// From mpeg4-generic configuration data.
sp<ABuffer> MakeAACCodecSpecificData2(const char *params) {
    AString val;
    unsigned long objectType;
    if (GetAttribute(params, "objectType", &val)) {
        const char *s = val.c_str();
        char *end;
        objectType = strtoul(s, &end, 10);
        CHECK(end > s && *end == '\0');
    } else {
        objectType = 0x40;  // Audio ISO/IEC 14496-3
    }

    CHECK(GetAttribute(params, "config", &val));

    sp<ABuffer> config = decodeHex(val);
    CHECK(config != NULL);

    // Make sure size fits into a single byte and doesn't have to
    // be encoded.
    CHECK_LT(20 + config->size(), 128u);

    const uint8_t *data = config->data();

    static const uint8_t kStaticESDS[] = {
        0x03, 22,
        0x00, 0x00,     // ES_ID
        0x00,           // streamDependenceFlag, URL_Flag, OCRstreamFlag

        0x04, 17,
        0x40,                       // Audio ISO/IEC 14496-3
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,

        0x05, 2,
        // AudioSpecificInfo follows
    };

    sp<ABuffer> csd = new ABuffer(sizeof(kStaticESDS) + config->size());
    uint8_t *dst = csd->data();
    *dst++ = 0x03;
    *dst++ = 20 + config->size();
    *dst++ = 0x00;  // ES_ID
    *dst++ = 0x00;
    *dst++ = 0x00;  // streamDependenceFlag, URL_Flag, OCRstreamFlag
    *dst++ = 0x04;
    *dst++ = 15 + config->size();
    *dst++ = objectType;
    for (int i = 0; i < 12; ++i) { *dst++ = 0x00; }
    *dst++ = 0x05;
    *dst++ = config->size();
    memcpy(dst, config->data(), config->size());

#ifndef ANDROID_DEFAULT_CODE 
    if (!checkAACConfig(config->data())) {
        return NULL;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE

    // hexdump(csd->data(), csd->size());

    return csd;
}

static size_t GetSizeWidth(size_t x) {
    size_t n = 1;
    while (x > 127) {
        ++n;
        x >>= 7;
    }
    return n;
}

static uint8_t *EncodeSize(uint8_t *dst, size_t x) {
    while (x > 127) {
        *dst++ = (x & 0x7f) | 0x80;
        x >>= 7;
    }
    *dst++ = x;
    return dst;
}

static bool ExtractDimensionsMPEG4Config(
        const sp<ABuffer> &config, int32_t *width, int32_t *height) {
    *width = 0;
    *height = 0;

    const uint8_t *ptr = config->data();
    size_t offset = 0;
    bool foundVOL = false;
    while (offset + 3 < config->size()) {
        if (memcmp("\x00\x00\x01", &ptr[offset], 3)
                || (ptr[offset + 3] & 0xf0) != 0x20) {
            ++offset;
            continue;
        }

        foundVOL = true;
        break;
    }

    if (!foundVOL) {
        return false;
    }

    return ExtractDimensionsFromVOLHeader(
            &ptr[offset], config->size() - offset, width, height);
}

static sp<ABuffer> MakeMPEG4VideoCodecSpecificData(
        const char *params, int32_t *width, int32_t *height) {
    *width = 0;
    *height = 0;

    AString val;
    CHECK(GetAttribute(params, "config", &val));

    sp<ABuffer> config = decodeHex(val);
    CHECK(config != NULL);

    if (!ExtractDimensionsMPEG4Config(config, width, height)) {
        return NULL;
    }

    ALOGI("VOL dimensions = %dx%d", *width, *height);

    size_t len1 = config->size() + GetSizeWidth(config->size()) + 1;
    size_t len2 = len1 + GetSizeWidth(len1) + 1 + 13;
    size_t len3 = len2 + GetSizeWidth(len2) + 1 + 3;

    sp<ABuffer> csd = new ABuffer(len3);
    uint8_t *dst = csd->data();
    *dst++ = 0x03;
    dst = EncodeSize(dst, len2 + 3);
    *dst++ = 0x00;  // ES_ID
    *dst++ = 0x00;
    *dst++ = 0x00;  // streamDependenceFlag, URL_Flag, OCRstreamFlag

    *dst++ = 0x04;
    dst = EncodeSize(dst, len1 + 13);
    *dst++ = 0x01;  // Video ISO/IEC 14496-2 Simple Profile
    for (size_t i = 0; i < 12; ++i) {
        *dst++ = 0x00;
    }

    *dst++ = 0x05;
    dst = EncodeSize(dst, config->size());
    memcpy(dst, config->data(), config->size());
    dst += config->size();

    // hexdump(csd->data(), csd->size());

    return csd;
}

#ifndef ANDROID_DEFAULT_CODE 
static bool checkVideoResolution(int32_t width, int32_t height) {
	return true;
}
#endif // #ifndef ANDROID_DEFAULT_CODE

APacketSource::APacketSource(
        const sp<ASessionDescription> &sessionDesc, size_t index)
    : mInitCheck(NO_INIT),
#ifndef ANDROID_DEFAULT_CODE
      mFormat(new MetaData),
      mEOSResult(OK),
      mWantsNALFragments(false),
      mAccessUnitTimeUs(-1),
      m_BufQueSize(kWholeBufSize), //40Mbytes
      m_TargetTime(kTargetTime), //ms
      m_uiNextAduSeqNum(-1),
      mIsAVC(false),
      mScanForIDR(true) {
#else
      mFormat(new MetaData) {
#endif // #ifndef ANDROID_DEFAULT_CODE
    unsigned long PT;
    AString desc;
    AString params;
    sessionDesc->getFormatType(index, &PT, &desc, &params);

    int64_t durationUs;
    if (sessionDesc->getDurationUs(&durationUs)) {
        mFormat->setInt64(kKeyDuration, durationUs);
    } else {
#ifndef ANDROID_DEFAULT_CODE 
        // set duration to 0 for live streaming
        mFormat->setInt64(kKeyDuration, 0ll);
#else
        mFormat->setInt64(kKeyDuration, 60 * 60 * 1000000ll);
#endif // #ifndef ANDROID_DEFAULT_CODE
    }

#ifndef ANDROID_DEFAULT_CODE 
    AString val;
#endif // #ifndef ANDROID_DEFAULT_CODE

    mInitCheck = OK;
    if (!strncmp(desc.c_str(), "H264/", 5)) {
#ifndef ANDROID_DEFAULT_CODE 
        mIsAVC = true;
#endif // #ifndef ANDROID_DEFAULT_CODE
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_AVC);

        int32_t width, height;
        if (!sessionDesc->getDimensions(index, PT, &width, &height)) {
            width = -1;
            height = -1;
        }

#ifndef ANDROID_DEFAULT_CODE
        ALOGI("width %d height %d", width, height);
#endif
        int32_t encWidth, encHeight;
        sp<ABuffer> codecSpecificData =
            MakeAVCCodecSpecificData(params.c_str(), &encWidth, &encHeight);

        if (codecSpecificData != NULL) {
#ifndef ANDROID_DEFAULT_CODE
            // always use enc width/height if available
            if (encWidth > 0 && encHeight > 0) {
#else
            if (width < 0) {
#endif
                // If no explicit width/height given in the sdp, use the dimensions
                // extracted from the first sequence parameter set.
                width = encWidth;
                height = encHeight;
            }

            mFormat->setData(
                    kKeyAVCC, 0,
                    codecSpecificData->data(), codecSpecificData->size());
        } else if (width < 0) {
#ifndef ANDROID_DEFAULT_CODE 
            ALOGE("[H264 capability error]Unsupported H264 video, bad params %s", params.c_str());
#endif // #ifndef ANDROID_DEFAULT_CODE
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }

#ifndef ANDROID_DEFAULT_CODE 
        if (!checkVideoResolution(width, height)) {
            ALOGE("[H264 capability error]Unsupported H264 video, width %d, height %d", width, height);
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        mFormat->setInt32(kKeyWidth, width);
        mFormat->setInt32(kKeyHeight, height);
    } else if (!strncmp(desc.c_str(), "H263-2000/", 10)
            || !strncmp(desc.c_str(), "H263-1998/", 10)) {
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_H263);

        int32_t width, height;
        if (!sessionDesc->getDimensions(index, PT, &width, &height)) {
#ifndef ANDROID_DEFAULT_CODE 
            ALOGE("[H263 capability error]Unsupported H263 video, no resolution info");
#endif // #ifndef ANDROID_DEFAULT_CODE
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }

#ifndef ANDROID_DEFAULT_CODE
        ALOGI("width %d height %d", width, height);
        if (!checkVideoResolution(width, height)) {
            ALOGE("[H263 capability error]Unsupported H263 video, width %d, height %d", width, height);
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        mFormat->setInt32(kKeyWidth, width);
        mFormat->setInt32(kKeyHeight, height);
    } else if (!strncmp(desc.c_str(), "MP4A-LATM/", 10)) {
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_AAC);

        int32_t sampleRate, numChannels;
        ASessionDescription::ParseFormatDesc(
                desc.c_str(), &sampleRate, &numChannels);

        mFormat->setInt32(kKeySampleRate, sampleRate);
        mFormat->setInt32(kKeyChannelCount, numChannels);

        sp<ABuffer> codecSpecificData =
            MakeAACCodecSpecificData(params.c_str());

#ifndef ANDROID_DEFAULT_CODE 
        if (codecSpecificData == NULL) {
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        mFormat->setData(
                kKeyESDS, 0,
                codecSpecificData->data(), codecSpecificData->size());
    } else if (!strncmp(desc.c_str(), "AMR/", 4)) {
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_AMR_NB);

        int32_t sampleRate, numChannels;
        ASessionDescription::ParseFormatDesc(
                desc.c_str(), &sampleRate, &numChannels);

#ifndef ANDROID_DEFAULT_CODE
        if (sampleRate != 8000) {
            ALOGW("bad AMR clock rate %d", sampleRate);
            sampleRate = 8000;
        }
#endif
        mFormat->setInt32(kKeySampleRate, sampleRate);
        mFormat->setInt32(kKeyChannelCount, numChannels);

        if (sampleRate != 8000 || numChannels != 1) {
#ifndef ANDROID_DEFAULT_CODE 
            ALOGE("[AMR capability error]Unsupported AMR audio, sample rate %d, channels %d", sampleRate, numChannels);
#endif // #ifndef ANDROID_DEFAULT_CODE
            mInitCheck = ERROR_UNSUPPORTED;
        }
#ifndef ANDROID_DEFAULT_CODE 
        AString value;
        bool valid = 
            (GetAttribute(params.c_str(), "octet-align", &value, true) && value == "1")
            && (!GetAttribute(params.c_str(), "crc", &value, true) || value == "0")
            && (!GetAttribute(params.c_str(), "interleaving", &value, true));
        if (!valid) {
            ALOGE("[AMR capability error]Unsupported AMR audio, params %s", params.c_str());
            mInitCheck = ERROR_UNSUPPORTED;
        }
        mFormat->setInt32(kKeyMaxInputSize, kMaxInputSizeAMR);
#endif
    } else if (!strncmp(desc.c_str(), "AMR-WB/", 7)) {
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_AMR_WB);

        int32_t sampleRate, numChannels;
        ASessionDescription::ParseFormatDesc(
                desc.c_str(), &sampleRate, &numChannels);

#ifndef ANDROID_DEFAULT_CODE
        if (sampleRate != 16000) {
            ALOGW("bad AMR clock rate %d", sampleRate);
            sampleRate = 16000;
        }
#endif
        mFormat->setInt32(kKeySampleRate, sampleRate);
        mFormat->setInt32(kKeyChannelCount, numChannels);

        if (sampleRate != 16000 || numChannels != 1) {
#ifndef ANDROID_DEFAULT_CODE 
            ALOGE("[AMR capability error]Unsupported AMR audio, sample rate %d, channels %d", sampleRate, numChannels);
#endif // #ifndef ANDROID_DEFAULT_CODE
            mInitCheck = ERROR_UNSUPPORTED;
        }
#ifndef ANDROID_DEFAULT_CODE 
        AString value;
        bool valid = 
            (GetAttribute(params.c_str(), "octet-align", &value, true) && value == "1")
            && (!GetAttribute(params.c_str(), "crc", &value, true) || value == "0")
            && (!GetAttribute(params.c_str(), "interleaving", &value, true));
        if (!valid) {
            ALOGE("[AMR capability error]Unsupported AMR audio, params %s", params.c_str());
            mInitCheck = ERROR_UNSUPPORTED;
        }
        mFormat->setInt32(kKeyMaxInputSize, kMaxInputSizeAMR);
#endif
#ifndef ANDROID_DEFAULT_CODE
    } else if (!strncmp(desc.c_str(), "MP4V-ES/", 8) ||
            (!strncmp(desc.c_str(), "mpeg4-generic/", 14) && GetAttribute(params.c_str(), "streamType", &val) 
             && !strcmp(val.c_str(), "4"))) {
#else
    } else if (!strncmp(desc.c_str(), "MP4V-ES/", 8)) {
#endif
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG4);

        int32_t width, height;
        if (!sessionDesc->getDimensions(index, PT, &width, &height)) {
            width = -1;
            height = -1;
        }

#ifndef ANDROID_DEFAULT_CODE
        ALOGI("width %d height %d", width, height);
#endif
        int32_t encWidth, encHeight;
        sp<ABuffer> codecSpecificData =
            MakeMPEG4VideoCodecSpecificData(
                    params.c_str(), &encWidth, &encHeight);

        if (codecSpecificData != NULL) {
#ifndef ANDROID_DEFAULT_CODE
	    mFormat->setData(kKeyMPEG4VOS, 0, 
			codecSpecificData->data(), codecSpecificData->size());
#endif
            mFormat->setData(
                    kKeyESDS, 0,
                    codecSpecificData->data(), codecSpecificData->size());

            if (width < 0) {
                width = encWidth;
                height = encHeight;
            }
#ifndef ANDROID_DEFAULT_CODE 
            // don't support bad config
        } else {
#else
        } else if (width < 0) {
#endif // #ifndef ANDROID_DEFAULT_CODE
#ifndef ANDROID_DEFAULT_CODE 
            ALOGE("[MPEG4 capability error]Unsupported MPEG4 video, params %s", params.c_str());
#endif // #ifndef ANDROID_DEFAULT_CODE
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }

#ifndef ANDROID_DEFAULT_CODE 
        if (!checkVideoResolution(width, height)) {
            ALOGE("[MPEG4 capability error]Unsupported MPEG4 video, width %d, height %d", width, height);
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        mFormat->setInt32(kKeyWidth, width);
        mFormat->setInt32(kKeyHeight, height);
    } else if (!strncasecmp(desc.c_str(), "mpeg4-generic/", 14)) {
        AString val;
        if (!GetAttribute(params.c_str(), "mode", &val)
                || (strcasecmp(val.c_str(), "AAC-lbr")
                    && strcasecmp(val.c_str(), "AAC-hbr"))) {
            mInitCheck = ERROR_UNSUPPORTED;
#ifndef ANDROID_DEFAULT_CODE 
            ALOGE("[RTSP capability error]Unsupported mpeg4-generic params %s", params.c_str());
#endif // #ifndef ANDROID_DEFAULT_CODE
            return;
        }

        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_AAC);

        int32_t sampleRate, numChannels;
        ASessionDescription::ParseFormatDesc(
                desc.c_str(), &sampleRate, &numChannels);

        mFormat->setInt32(kKeySampleRate, sampleRate);
        mFormat->setInt32(kKeyChannelCount, numChannels);
        mFormat->setInt32(kKeyIsADTS, true);

        sp<ABuffer> codecSpecificData =
            MakeAACCodecSpecificData2(params.c_str());

#ifndef ANDROID_DEFAULT_CODE 
        if (codecSpecificData == NULL) {
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        mFormat->setData(
                kKeyESDS, 0,
                codecSpecificData->data(), codecSpecificData->size());
    } else if (ARawAudioAssembler::Supports(desc.c_str())) {
        ARawAudioAssembler::MakeFormat(desc.c_str(), mFormat);
    } else {
#ifndef ANDROID_DEFAULT_CODE 
        ALOGE("[RTSP capability error]Unsupported mime %s", desc.c_str());
#endif // #ifndef ANDROID_DEFAULT_CODE
        mInitCheck = ERROR_UNSUPPORTED;
    }
}

APacketSource::~APacketSource() {
}

status_t APacketSource::initCheck() const {
    return mInitCheck;
}

sp<MetaData> APacketSource::getFormat() {
    return mFormat;
}

#ifndef ANDROID_DEFAULT_CODE 
status_t APacketSource::start(MetaData *params) {
    // support pv codec
    Mutex::Autolock autoLock(mLock);

    int32_t val;
    if (params && params->findInt32(kKeyWantsNALFragments, &val)
        && val != 0) {
        mWantsNALFragments = true;
    } else {
        mWantsNALFragments = false;
    }
    return OK;
}

status_t APacketSource::stop() {
    signalEOS(ERROR_END_OF_STREAM);
    return OK;
}

status_t APacketSource::read(
        MediaBuffer **out, const ReadOptions *) {
    *out = NULL;

    Mutex::Autolock autoLock(mLock);
    while (mEOSResult == OK && mBuffers.empty()) {
        mCondition.wait(mLock);
    }

    if (!mBuffers.empty()) {
        const sp<ABuffer> buffer = *mBuffers.begin();

		m_uiNextAduSeqNum = buffer->int32Data();
        int64_t timeUs;
        CHECK(buffer->meta()->findInt64("timeUs", &timeUs));

        MediaBuffer *mediaBuffer = new MediaBuffer(buffer);
        mediaBuffer->meta_data()->setInt64(kKeyTime, timeUs);

        *out = mediaBuffer;

        mBuffers.erase(mBuffers.begin());
        return OK;
    }

    return mEOSResult;
}

void APacketSource::queueAccessUnit(const sp<ABuffer> &buffer) {
    int32_t damaged;
    if (buffer->meta()->findInt32("damaged", &damaged) && damaged) {
        ALOGV("discarding damaged AU");
        return;
    }

    {
        Mutex::Autolock autoLock(mLock);
        if (mEOSResult == ERROR_END_OF_STREAM) {
            ALOGE("don't queue data after ERROR_END_OF_STREAM");
            return;
        }
    }

    if (mScanForIDR && mIsAVC) {
        // This pretty piece of code ensures that the first access unit
        // fed to the decoder after stream-start or seek is guaranteed to
        // be an IDR frame. This is to workaround limitations of a certain
        // hardware h.264 decoder that requires this to be the case.

        // only check the first byte of nal fragment
        // AAVCAssembler only send nal fragment now
        if ((buffer->data()[0] & 0x1f) != 5) {
            ALOGV("skipping AU while scanning for next IDR frame.");
            return;
        } 
        if (!mWantsNALFragments)
            CHECK(buffer->meta()->findInt64("timeUs", &mAccessUnitTimeUs));

        mScanForIDR = false;
    }

    // combine access units here for AVC
    if (mIsAVC && !mWantsNALFragments) {
        int64_t timeUs;
        CHECK(buffer->meta()->findInt64("timeUs", &timeUs));
        if (timeUs != mAccessUnitTimeUs) {
            size_t totalSize = 0;
            for (List<sp<ABuffer> >::iterator it = mNALUnits.begin();
                    it != mNALUnits.end(); ++it) {
                totalSize += 4 + (*it)->size();
            }

            sp<ABuffer> accessUnit = new ABuffer(totalSize);
            size_t offset = 0;
            for (List<sp<ABuffer> >::iterator it = mNALUnits.begin();
                    it != mNALUnits.end(); ++it) {
                memcpy(accessUnit->data() + offset, "\x00\x00\x00\x01", 4);
                offset += 4;

                sp<ABuffer> nal = *it;
                memcpy(accessUnit->data() + offset, nal->data(), nal->size());
                offset += nal->size();
            }

            accessUnit->setInt32Data((*mNALUnits.begin())->int32Data());
            accessUnit->meta()->setInt64("timeUs", mAccessUnitTimeUs);
            
            Mutex::Autolock autoLock(mLock);
            mBuffers.push_back(accessUnit);
            mCondition.signal();
            mNALUnits.clear();
        }
        mNALUnits.push_back(buffer);
        mAccessUnitTimeUs = timeUs;
        return;
    }

    Mutex::Autolock autoLock(mLock);
    int64_t timeUs;
    CHECK(buffer->meta()->findInt64("timeUs", &timeUs));
    if (mAccessUnitTimeUs != -1 && mAccessUnitTimeUs > timeUs) {
        ALOGW("discard late access unit %lld < %lld", timeUs, mAccessUnitTimeUs);
        return;
    }
    mAccessUnitTimeUs = timeUs;
    mBuffers.push_back(buffer);
    mCondition.signal();
}

void APacketSource::signalEOS(status_t result) {
    CHECK(result != OK);
	ALOGI("APacketSource::signalEOS,mBuffers.size=%d",mBuffers.size());
    Mutex::Autolock autoLock(mLock);
    mEOSResult = result;
    mCondition.signal();
}

void APacketSource::flushQueue() {
    Mutex::Autolock autoLock(mLock);
	ALOGI("APacketSource::flushQueue");
    mBuffers.clear();

    mScanForIDR = true;
    // reset eos
    mEOSResult = OK;
    mAccessUnitTimeUs = -1;
}

bool APacketSource::isAtEOS() {
    Mutex::Autolock autoLock(mLock);
    return mEOSResult == ERROR_END_OF_STREAM;
}

int64_t APacketSource::getQueueDurationUs(bool *eos) {
    Mutex::Autolock autoLock(mLock);

    *eos = (mEOSResult != OK);

    if (mBuffers.size() < 2) {
        return 0;
    }

    const sp<ABuffer> first = *mBuffers.begin();
    const sp<ABuffer> last = *--mBuffers.end();

    int64_t firstTimeUs;
    CHECK(first->meta()->findInt64("timeUs", &firstTimeUs));

    int64_t lastTimeUs;
    CHECK(last->meta()->findInt64("timeUs", &lastTimeUs));

    if (lastTimeUs < firstTimeUs) {
        ALOGE("Huh? Time moving backwards? %lld > %lld",
             firstTimeUs, lastTimeUs);

        return 0;
    }

    return lastTimeUs - firstTimeUs;
}

bool APacketSource::getNSN(int32_t* uiNextSeqNum){

    Mutex::Autolock autoLock(mLock);
    if(!mBuffers.empty()){
        if(m_uiNextAduSeqNum!= -1){		
            *uiNextSeqNum = m_uiNextAduSeqNum;
            return true;
        }
        *uiNextSeqNum = (*mBuffers.begin())->int32Data();
        return true;
    }
    return false;
}

size_t APacketSource::getFreeBufSpace(){
	
	Mutex::Autolock autoLock(mLock); //add lock ,in case of memorycorruption
	
    //size_t freeBufSpace = m_BufQueSize;
    size_t bufSizeUsed = 0;
    if(mBuffers.empty()){
        return m_BufQueSize;
    }

    List<sp<ABuffer> >::iterator it = mBuffers.begin();
    while (it != mBuffers.end()) {
        bufSizeUsed += (*it)->size();
        it++;	
    }
    if(bufSizeUsed >= m_BufQueSize)
        return 0;

    return 	m_BufQueSize - bufSizeUsed;	  
}
#endif

}  // namespace android
