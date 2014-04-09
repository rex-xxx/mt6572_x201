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

#ifndef A_PACKET_SOURCE_H_

#define A_PACKET_SOURCE_H_

#include <media/stagefright/foundation/ABase.h>
#ifndef ANDROID_DEFAULT_CODE
#include <media/stagefright/MediaSource.h>
#include <utils/threads.h>
#include <utils/List.h>
#endif
#include <media/stagefright/MetaData.h>
#include <utils/RefBase.h>

namespace android {

#ifndef ANDROID_DEFAULT_CODE
    struct ABuffer;
#endif
struct ASessionDescription;

#ifndef ANDROID_DEFAULT_CODE
struct APacketSource : public MediaSource {
#else
struct APacketSource : public RefBase {
#endif
    APacketSource(const sp<ASessionDescription> &sessionDesc, size_t index);

    status_t initCheck() const;

#ifndef ANDROID_DEFAULT_CODE
            virtual status_t start(MetaData *params = NULL);
            virtual status_t stop();
#endif
    virtual sp<MetaData> getFormat();

#ifndef ANDROID_DEFAULT_CODE
            virtual status_t read(
                    MediaBuffer **buffer, const ReadOptions *options = NULL);

            void queueAccessUnit(const sp<ABuffer> &buffer);
            void signalEOS(status_t result);

            void flushQueue();

            bool isAtEOS();

            size_t getBufQueSize(){return m_BufQueSize;} //get Whole Buffer queue size 
            size_t getTargetTime(){return m_TargetTime;}  //get target protected time of buffer queue duration for interrupt-free playback 
            bool getNSN(int32_t* uiNextSeqNum);
            size_t getFreeBufSpace();

            int64_t getQueueDurationUs(bool *eos);
#endif // #ifndef ANDROID_DEFAULT_CODE
protected:
    virtual ~APacketSource();

private:
    status_t mInitCheck;

    sp<MetaData> mFormat;

    DISALLOW_EVIL_CONSTRUCTORS(APacketSource);
#ifndef ANDROID_DEFAULT_CODE
    Mutex mLock;
    Condition mCondition;
    List<sp<ABuffer> > mBuffers;
    status_t mEOSResult;
    // for avc nals
    bool mWantsNALFragments;
    List<sp<ABuffer> > mNALUnits;
    int64_t mAccessUnitTimeUs;

    size_t m_BufQueSize; //Whole Buffer queue size 
    size_t m_TargetTime;  // target protected time of buffer queue duration for interrupt-free playback 
    int32_t m_uiNextAduSeqNum;

    bool mIsAVC;
    bool mScanForIDR;
#endif
};


}  // namespace android

#endif  // A_PACKET_SOURCE_H_
