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

#ifndef A_MPEG4_ELEM_ASSEMBLER_H_

#define A_MPEG4_ELEM_ASSEMBLER_H_

#include "ARTPAssembler.h"

#include <media/stagefright/foundation/AString.h>

#include <utils/List.h>
#include <utils/RefBase.h>

#include <OMX_Audio.h>

namespace android {

struct ABuffer;
struct AMessage;

struct AMPEG4ElementaryAssembler : public ARTPAssembler {
    AMPEG4ElementaryAssembler(
            const sp<AMessage> &notify, const AString &desc,
            const AString &params);

protected:
    virtual ~AMPEG4ElementaryAssembler();

#ifndef ANDROID_DEFAULT_CODE 
    virtual void evaluateDuration(const sp<ARTPSource> &source, 
            const sp<ABuffer> &buffer);
#endif // #ifndef ANDROID_DEFAULT_CODE
    virtual AssemblyStatus assembleMore(const sp<ARTPSource> &source);
    virtual void onByeReceived();
    virtual void packetLost();

private:
    sp<AMessage> mNotifyMsg;
    bool mIsGeneric;
    AString mParams;

#ifndef ANDROID_DEFAULT_CODE 
    bool mIsAAC;
#endif // #ifndef ANDROID_DEFAULT_CODE
    unsigned mSizeLength;
    unsigned mIndexLength;
    unsigned mIndexDeltaLength;
    unsigned mCTSDeltaLength;
    unsigned mDTSDeltaLength;
    bool mRandomAccessIndication;
    unsigned mStreamStateIndication;
    unsigned mAuxiliaryDataSizeLength;
    bool mHasAUHeader;

    int32_t mChannelConfig;
    size_t mSampleRateIndex;

    uint32_t mAccessUnitRTPTime;
    bool mNextExpectedSeqNoValid;
    uint32_t mNextExpectedSeqNo;
    bool mAccessUnitDamaged;
    List<sp<ABuffer> > mPackets;
#ifndef ANDROID_DEFAULT_CODE
    unsigned mConstantSize;
#endif

    AssemblyStatus addPacket(const sp<ARTPSource> &source);
    void submitAccessUnit();

    DISALLOW_EVIL_CONSTRUCTORS(AMPEG4ElementaryAssembler);
#ifndef ANDROID_DEFAULT_CODE 
public:
    virtual void setNextExpectedSeqNo(uint32_t rtpSeq) {
        mNextExpectedSeqNo = rtpSeq;
        mNextExpectedSeqNoValid = true;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE
};

}  // namespace android

#endif  // A_MPEG4_ELEM_ASSEMBLER_H_
