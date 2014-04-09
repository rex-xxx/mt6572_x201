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

#include "ARTPAssembler.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>

#include <stdint.h>

namespace android {

static int64_t getNowUs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);

    return (int64_t)tv.tv_usec + tv.tv_sec * 1000000ll;
}

ARTPAssembler::ARTPAssembler()
    : mFirstFailureTimeUs(-1) {
}

#ifndef ANDROID_DEFAULT_CODE 
void ARTPAssembler::onPacketReceived(const sp<ARTPSource> &source, bool flush) {
    if (flush)
        ALOGW("flush packet from ARTPSource to APacketSource");
#else
void ARTPAssembler::onPacketReceived(const sp<ARTPSource> &source) {
#endif // #ifndef ANDROID_DEFAULT_CODE
    AssemblyStatus status;
    for (;;) {
        status = assembleMore(source);

#ifndef ANDROID_DEFAULT_CODE 
        if (status == WRONG_SEQUENCE_NUMBER || status == LARGE_SEQUENCE_GAP) {
#else
        if (status == WRONG_SEQUENCE_NUMBER) {
#endif // #ifndef ANDROID_DEFAULT_CODE
#ifndef ANDROID_DEFAULT_CODE 
            if (flush || status == LARGE_SEQUENCE_GAP) {
                mFirstFailureTimeUs = -1;
                packetLost();
                continue;
            }
#endif // #ifndef ANDROID_DEFAULT_CODE
            if (mFirstFailureTimeUs >= 0) {
#ifndef ANDROID_DEFAULT_CODE 
                if (getNowUs() - mFirstFailureTimeUs > 100000ll) {
#else
                if (getNowUs() - mFirstFailureTimeUs > 10000ll) {
#endif
                    mFirstFailureTimeUs = -1;

                    // LOG(VERBOSE) << "waited too long for packet.";
                    packetLost();
                    continue;
                }
            } else {
                mFirstFailureTimeUs = getNowUs();
            }
            break;
        } else {
            mFirstFailureTimeUs = -1;

            if (status == NOT_ENOUGH_DATA) {
                break;
            }
        }
    }
}

// static
void ARTPAssembler::CopyTimes(const sp<ABuffer> &to, const sp<ABuffer> &from) {
    uint32_t rtpTime;
    CHECK(from->meta()->findInt32("rtp-time", (int32_t *)&rtpTime));

    to->meta()->setInt32("rtp-time", rtpTime);

    // Copy the seq number.
    to->setInt32Data(from->int32Data());
}

// static
sp<ABuffer> ARTPAssembler::MakeADTSCompoundFromAACFrames(
        unsigned profile,
        unsigned samplingFreqIndex,
        unsigned channelConfig,
        const List<sp<ABuffer> > &frames) {
    size_t totalSize = 0;
    for (List<sp<ABuffer> >::const_iterator it = frames.begin();
         it != frames.end(); ++it) {
        // Each frame is prefixed by a 7 byte ADTS header
        totalSize += (*it)->size() + 7;
    }

    sp<ABuffer> accessUnit = new ABuffer(totalSize);
    size_t offset = 0;
    for (List<sp<ABuffer> >::const_iterator it = frames.begin();
         it != frames.end(); ++it) {
        sp<ABuffer> nal = *it;
        uint8_t *dst = accessUnit->data() + offset;

        static const unsigned kADTSId = 0;
        static const unsigned kADTSLayer = 0;
        static const unsigned kADTSProtectionAbsent = 1;

        unsigned frameLength = nal->size() + 7;

        dst[0] = 0xff;

        dst[1] =
            0xf0 | (kADTSId << 3) | (kADTSLayer << 1) | kADTSProtectionAbsent;

        dst[2] = (profile << 6)
                | (samplingFreqIndex << 2)
                | (channelConfig >> 2);

        dst[3] = ((channelConfig & 3) << 6) | (frameLength >> 11);

        dst[4] = (frameLength >> 3) & 0xff;
        dst[5] = (frameLength & 7) << 5;
        dst[6] = 0x00;

        memcpy(dst + 7, nal->data(), nal->size());
        offset += nal->size() + 7;
    }

    CopyTimes(accessUnit, *frames.begin());

    return accessUnit;
}

// static
sp<ABuffer> ARTPAssembler::MakeCompoundFromPackets(
        const List<sp<ABuffer> > &packets) {
    size_t totalSize = 0;
    for (List<sp<ABuffer> >::const_iterator it = packets.begin();
         it != packets.end(); ++it) {
        totalSize += (*it)->size();
    }

    sp<ABuffer> accessUnit = new ABuffer(totalSize);
    size_t offset = 0;
    for (List<sp<ABuffer> >::const_iterator it = packets.begin();
         it != packets.end(); ++it) {
        sp<ABuffer> nal = *it;
        memcpy(accessUnit->data() + offset, nal->data(), nal->size());
        offset += nal->size();
    }

    CopyTimes(accessUnit, *packets.begin());

    return accessUnit;
}

#ifndef ANDROID_DEFAULT_CODE 
void ARTPAssembler::updatePacketReceived(const sp<ARTPSource> &source, 
        const sp<ABuffer> &buffer) {
    evaluateDuration(source, buffer);
}
#endif // #ifndef ANDROID_DEFAULT_CODE
}  // namespace android
