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
#define LOG_TAG "AMPEG4ElementaryAssembler"
#include <utils/Log.h>

#include "AMPEG4ElementaryAssembler.h"

#include "ARTPSource.h"
#include "ASessionDescription.h"

#include <media/stagefright/foundation/ABitReader.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/Utils.h>

#include <ctype.h>
#include <stdint.h>

namespace android {

static bool GetAttribute(const char *s, const char *key, AString *value) {
    value->clear();

    size_t keyLen = strlen(key);

    for (;;) {
        while (isspace(*s)) {
            ++s;
        }

        const char *colonPos = strchr(s, ';');

        size_t len =
            (colonPos == NULL) ? strlen(s) : colonPos - s;

        if (len >= keyLen + 1 && s[keyLen] == '='
                && !strncasecmp(s, key, keyLen)) {
            value->setTo(&s[keyLen + 1], len - keyLen - 1);
            return true;
        }

        if (colonPos == NULL) {
            return false;
        }

        s = colonPos + 1;
    }
}

static bool GetIntegerAttribute(
        const char *s, const char *key, unsigned *x) {
    *x = 0;

    AString val;
    if (!GetAttribute(s, key, &val)) {
        return false;
    }

    s = val.c_str();
    char *end;
    unsigned y = strtoul(s, &end, 10);

    if (end == s || *end != '\0') {
        return false;
    }

    *x = y;

    return true;
}

static bool GetSampleRateIndex(int32_t sampleRate, size_t *tableIndex) {
    static const int32_t kSampleRateTable[] = {
        96000, 88200, 64000, 48000, 44100, 32000,
        24000, 22050, 16000, 12000, 11025, 8000
    };
    const size_t kNumSampleRates =
        sizeof(kSampleRateTable) / sizeof(kSampleRateTable[0]);

    *tableIndex = 0;
    for (size_t index = 0; index < kNumSampleRates; ++index) {
        if (sampleRate == kSampleRateTable[index]) {
            *tableIndex = index;
            return true;
        }
    }

#ifndef ANDROID_DEFAULT_CODE
    ALOGW("unsupport sample rate %d", sampleRate);
    return true;
#endif
    return false;
}

// static
AMPEG4ElementaryAssembler::AMPEG4ElementaryAssembler(
        const sp<AMessage> &notify, const AString &desc, const AString &params)
    : mNotifyMsg(notify),
      mIsGeneric(false),
      mParams(params),
#ifndef ANDROID_DEFAULT_CODE 
      mIsAAC(false),
#endif // #ifndef ANDROID_DEFAULT_CODE
      mSizeLength(0),
      mIndexLength(0),
      mIndexDeltaLength(0),
      mCTSDeltaLength(0),
      mDTSDeltaLength(0),
      mRandomAccessIndication(false),
      mStreamStateIndication(0),
      mAuxiliaryDataSizeLength(0),
      mHasAUHeader(false),
      mChannelConfig(0),
      mSampleRateIndex(0),
      mAccessUnitRTPTime(0),
      mNextExpectedSeqNoValid(false),
      mNextExpectedSeqNo(0),
#ifndef ANDROID_DEFAULT_CODE
      mAccessUnitDamaged(false),
      mConstantSize(0) {
#else
      mAccessUnitDamaged(false) {
#endif
    mIsGeneric = !strncasecmp(desc.c_str(),"mpeg4-generic/", 14);

    if (mIsGeneric) {
        AString value;
        CHECK(GetAttribute(params.c_str(), "mode", &value));
#ifndef ANDROID_DEFAULT_CODE 
        if (!strcasecmp(value.c_str(), "AAC-lbr") ||
                !strcasecmp(value.c_str(), "AAC-hbr"))
            mIsAAC = true;
#endif // #ifndef ANDROID_DEFAULT_CODE

        if (!GetIntegerAttribute(params.c_str(), "sizeLength", &mSizeLength)) {
            mSizeLength = 0;
        }

        if (!GetIntegerAttribute(
                    params.c_str(), "indexLength", &mIndexLength)) {
            mIndexLength = 0;
        }

        if (!GetIntegerAttribute(
                    params.c_str(), "indexDeltaLength", &mIndexDeltaLength)) {
            mIndexDeltaLength = 0;
        }

        if (!GetIntegerAttribute(
                    params.c_str(), "CTSDeltaLength", &mCTSDeltaLength)) {
            mCTSDeltaLength = 0;
        }

        if (!GetIntegerAttribute(
                    params.c_str(), "DTSDeltaLength", &mDTSDeltaLength)) {
            mDTSDeltaLength = 0;
        }

        unsigned x;
        if (!GetIntegerAttribute(
                    params.c_str(), "randomAccessIndication", &x)) {
            mRandomAccessIndication = false;
        } else {
            CHECK(x == 0 || x == 1);
            mRandomAccessIndication = (x != 0);
        }

        if (!GetIntegerAttribute(
                    params.c_str(), "streamStateIndication",
                    &mStreamStateIndication)) {
            mStreamStateIndication = 0;
        }

        if (!GetIntegerAttribute(
                    params.c_str(), "auxiliaryDataSizeLength",
                    &mAuxiliaryDataSizeLength)) {
            mAuxiliaryDataSizeLength = 0;
        }

#ifndef ANDROID_DEFAULT_CODE
        if (!GetIntegerAttribute(
                    params.c_str(), "constantSize",
                    &mConstantSize)) {
            mConstantSize = 0;
        }

        if (mSizeLength == 0 && mConstantSize == 0) {
            ALOGI("signal AU/fragment mode");
        }
#endif

        mHasAUHeader =
            mSizeLength > 0
            || mIndexLength > 0
            || mIndexDeltaLength > 0
            || mCTSDeltaLength > 0
            || mDTSDeltaLength > 0
            || mRandomAccessIndication
            || mStreamStateIndication > 0;

        int32_t sampleRate, numChannels;
        ASessionDescription::ParseFormatDesc(
                desc.c_str(), &sampleRate, &numChannels);

        mChannelConfig = numChannels;
        CHECK(GetSampleRateIndex(sampleRate, &mSampleRateIndex));
    }
}

AMPEG4ElementaryAssembler::~AMPEG4ElementaryAssembler() {
}

struct AUHeader {
    unsigned mSize;
    unsigned mSerial;
};

ARTPAssembler::AssemblyStatus AMPEG4ElementaryAssembler::addPacket(
        const sp<ARTPSource> &source) {
    List<sp<ABuffer> > *queue = source->queue();

    if (queue->empty()) {
        return NOT_ENOUGH_DATA;
    }

    if (mNextExpectedSeqNoValid) {
        List<sp<ABuffer> >::iterator it = queue->begin();
        while (it != queue->end()) {
            if ((uint32_t)(*it)->int32Data() >= mNextExpectedSeqNo) {
                break;
            }

            it = queue->erase(it);
        }

        if (queue->empty()) {
            return NOT_ENOUGH_DATA;
        }
    }

    sp<ABuffer> buffer = *queue->begin();

    if (!mNextExpectedSeqNoValid) {
        mNextExpectedSeqNoValid = true;
        mNextExpectedSeqNo = (uint32_t)buffer->int32Data();
    } else if ((uint32_t)buffer->int32Data() != mNextExpectedSeqNo) {
        ALOGV("Not the sequence number I expected");

#ifndef ANDROID_DEFAULT_CODE 
        return getAssembleStatus(queue, mNextExpectedSeqNo);
#else
        return WRONG_SEQUENCE_NUMBER;
#endif // #ifndef ANDROID_DEFAULT_CODE
    }

    uint32_t rtpTime;
    CHECK(buffer->meta()->findInt32("rtp-time", (int32_t *)&rtpTime));

    if (mPackets.size() > 0 && rtpTime != mAccessUnitRTPTime) {
        submitAccessUnit();
    }
    mAccessUnitRTPTime = rtpTime;

    if (!mIsGeneric) {
        mPackets.push_back(buffer);
    } else {
        // hexdump(buffer->data(), buffer->size());

#ifndef ANDROID_DEFAULT_CODE 
        // return MALFORMED_PACKET instead of abort
        if (buffer->size() < 2) {
            ALOGV("Ignoring malformed buffer: (size = %d)", buffer->size());
            queue->erase(queue->begin());
            ++mNextExpectedSeqNo;
            return MALFORMED_PACKET;
        }
        unsigned AU_headers_length = U16_AT(buffer->data());  // in bits
        if (buffer->size() < 2 + (AU_headers_length + 7) / 8) {
            ALOGV("Ignoring malformed buffer: (size = %d, header length = %d)",
                    buffer->size(), AU_headers_length);
            queue->erase(queue->begin());
            ++mNextExpectedSeqNo;
            return MALFORMED_PACKET;
        }

        size_t totalSize = 0;
#else
        CHECK_GE(buffer->size(), 2u);
        unsigned AU_headers_length = U16_AT(buffer->data());  // in bits

        CHECK_GE(buffer->size(), 2 + (AU_headers_length + 7) / 8);
#endif // #ifndef ANDROID_DEFAULT_CODE

        List<AUHeader> headers;

        ABitReader bits(buffer->data() + 2, buffer->size() - 2);
        unsigned numBitsLeft = AU_headers_length;

        unsigned AU_serial = 0;
        for (;;) {
            if (numBitsLeft < mSizeLength) { break; }

            unsigned AU_size = bits.getBits(mSizeLength);
            numBitsLeft -= mSizeLength;

            size_t n = headers.empty() ? mIndexLength : mIndexDeltaLength;
            if (numBitsLeft < n) { break; }

            unsigned AU_index = bits.getBits(n);
            numBitsLeft -= n;

            if (headers.empty()) {
                AU_serial = AU_index;
            } else {
                AU_serial += 1 + AU_index;
            }

            if (mCTSDeltaLength > 0) {
                if (numBitsLeft < 1) {
                    break;
                }
                --numBitsLeft;
                if (bits.getBits(1)) {
                    if (numBitsLeft < mCTSDeltaLength) {
                        break;
                    }
                    bits.skipBits(mCTSDeltaLength);
                    numBitsLeft -= mCTSDeltaLength;
                }
            }

            if (mDTSDeltaLength > 0) {
                if (numBitsLeft < 1) {
                    break;
                }
                --numBitsLeft;
                if (bits.getBits(1)) {
                    if (numBitsLeft < mDTSDeltaLength) {
                        break;
                    }
                    bits.skipBits(mDTSDeltaLength);
                    numBitsLeft -= mDTSDeltaLength;
                }
            }

            if (mRandomAccessIndication) {
                if (numBitsLeft < 1) {
                    break;
                }
                bits.skipBits(1);
                --numBitsLeft;
            }

            if (mStreamStateIndication > 0) {
                if (numBitsLeft < mStreamStateIndication) {
                    break;
                }
                bits.skipBits(mStreamStateIndication);
            }

            AUHeader header;
            header.mSize = AU_size;
            header.mSerial = AU_serial;
            headers.push_back(header);
#ifndef ANDROID_DEFAULT_CODE 
            totalSize += AU_size;
#endif // #ifndef ANDROID_DEFAULT_CODE
        }

        size_t offset = 2 + (AU_headers_length + 7) / 8;

        if (mAuxiliaryDataSizeLength > 0) {
            ABitReader bits(buffer->data() + offset, buffer->size() - offset);

            unsigned auxSize = bits.getBits(mAuxiliaryDataSizeLength);

            offset += (mAuxiliaryDataSizeLength + auxSize + 7) / 8;
        }

#ifndef ANDROID_DEFAULT_CODE 
        if (mSizeLength == 0 && mConstantSize == 0) {
            totalSize = buffer->size() - offset;
            AUHeader header;
            header.mSize = totalSize;
            header.mSerial = 0;
            headers.clear();
            headers.push_back(header);
        }
        // early check, check size together
        if (offset + totalSize != buffer->size()) {
            ALOGV("Ignoring malformed buffer: AU total size %d + offset %d, "
                    "dose not match buffer size %d", totalSize, offset, 
                    buffer->size());
            queue->erase(queue->begin());
            ++mNextExpectedSeqNo;
            return MALFORMED_PACKET;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        for (List<AUHeader>::iterator it = headers.begin();
             it != headers.end(); ++it) {
            const AUHeader &header = *it;

#ifdef ANDROID_DEFAULT_CODE 
            // no need check again
            CHECK_LE(offset + header.mSize, buffer->size());
#endif // #ifdef ANDROID_DEFAULT_CODE

            sp<ABuffer> accessUnit = new ABuffer(header.mSize);
            memcpy(accessUnit->data(), buffer->data() + offset, header.mSize);

            offset += header.mSize;

            CopyTimes(accessUnit, buffer);
            mPackets.push_back(accessUnit);
        }

#ifdef ANDROID_DEFAULT_CODE 
        // no need check again
        CHECK_EQ(offset, buffer->size());
#endif // #ifdef ANDROID_DEFAULT_CODE
    }

    queue->erase(queue->begin());
    ++mNextExpectedSeqNo;

    return OK;
}

void AMPEG4ElementaryAssembler::submitAccessUnit() {
    CHECK(!mPackets.empty());

    ALOGV("Access unit complete (%d nal units)", mPackets.size());

    sp<ABuffer> accessUnit;

    if (mIsGeneric) {
        accessUnit = MakeADTSCompoundFromAACFrames(
                OMX_AUDIO_AACObjectLC - 1,
                mSampleRateIndex,
                mChannelConfig,
                mPackets);
    } else {
        accessUnit = MakeCompoundFromPackets(mPackets);
    }

#if 0
    printf(mAccessUnitDamaged ? "X" : ".");
    fflush(stdout);
#endif

    if (mAccessUnitDamaged) {
        accessUnit->meta()->setInt32("damaged", true);
    }

    mPackets.clear();
    mAccessUnitDamaged = false;

    sp<AMessage> msg = mNotifyMsg->dup();
    msg->setBuffer("access-unit", accessUnit);
    msg->post();
}

ARTPAssembler::AssemblyStatus AMPEG4ElementaryAssembler::assembleMore(
        const sp<ARTPSource> &source) {
    AssemblyStatus status = addPacket(source);
    if (status == MALFORMED_PACKET) {
        mAccessUnitDamaged = true;
    }
    return status;
}

void AMPEG4ElementaryAssembler::packetLost() {
    CHECK(mNextExpectedSeqNoValid);
    ALOGV("packetLost (expected %d)", mNextExpectedSeqNo);

    ++mNextExpectedSeqNo;

    mAccessUnitDamaged = true;
}

void AMPEG4ElementaryAssembler::onByeReceived() {
    sp<AMessage> msg = mNotifyMsg->dup();
    msg->setInt32("eos", true);
    msg->post();
}

#ifndef ANDROID_DEFAULT_CODE 
void AMPEG4ElementaryAssembler::evaluateDuration(const sp<ARTPSource>& source,
        const sp<ABuffer>& buffer) {
    if (!mIsAAC)
        return;
    
    size_t size = buffer->size();
    if (size < 2)
        return;

    unsigned AU_headers_length = U16_AT(buffer->data());  // in bits
    unsigned bitsPerHeader = mSizeLength + mIndexDeltaLength;

    if (mCTSDeltaLength > 0)
        bitsPerHeader += 1 + mCTSDeltaLength;

    if (mDTSDeltaLength > 0)
        bitsPerHeader += 1 + mDTSDeltaLength;

    if (mRandomAccessIndication)
        bitsPerHeader++;

    if (mStreamStateIndication > 0)
        bitsPerHeader += mStreamStateIndication;

    int32_t num = (AU_headers_length + bitsPerHeader - 1) / bitsPerHeader;
    // assume frameLength of AAC is 1024
    source->updateExpectedTimeoutUs((int32_t)(num * 1024));
    return;
}
#endif // #ifndef ANDROID_DEFAULT_CODE
}  // namespace android
