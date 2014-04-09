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

#ifndef A_SESSION_DESCRIPTION_H_

#define A_SESSION_DESCRIPTION_H_

#include <sys/types.h>

#include <media/stagefright/foundation/ABase.h>
#include <utils/KeyedVector.h>
#include <utils/RefBase.h>
#include <utils/Vector.h>
#ifndef ANDROID_DEFAULT_CODE 
#include <utils/String8.h>
#endif // #ifndef ANDROID_DEFAULT_CODE

namespace android {

struct AString;

struct ASessionDescription : public RefBase {
    ASessionDescription();

    bool setTo(const void *data, size_t size);
    bool isValid() const;

    // Actually, 1 + number of tracks, as index 0 is reserved for the
    // session description root-level attributes.
    size_t countTracks() const;
    void getFormat(size_t index, AString *value) const;

    void getFormatType(
            size_t index, unsigned long *PT,
            AString *desc, AString *params) const;

    bool getDimensions(
            size_t index, unsigned long PT,
            int32_t *width, int32_t *height) const;

    bool getDurationUs(int64_t *durationUs) const;

    static void ParseFormatDesc(
            const char *desc, int32_t *timescale, int32_t *numChannels);

    bool findAttribute(size_t index, const char *key, AString *value) const;

    // parses strings of the form
    //   npt      := npt-time "-" npt-time? | "-" npt-time
    //   npt-time := "now" | [0-9]+("." [0-9]*)?
    //
    // Returns true iff both "npt1" and "npt2" times were available,
    // i.e. we have a fixed duration, otherwise this is live streaming.
    static bool parseNTPRange(const char *s, float *npt1, float *npt2);
#ifndef ANDROID_DEFAULT_CODE 
    bool getBitrate(size_t index, int32_t* bitrate) const;
    status_t getSessionUrl(String8& uri) const;
#endif // #ifndef ANDROID_DEFAULT_CODE

protected:
    virtual ~ASessionDescription();

private:
    typedef KeyedVector<AString,AString> Attribs;

    bool mIsValid;
    Vector<Attribs> mTracks;
    Vector<AString> mFormats;

    bool parse(const void *data, size_t size);

    DISALLOW_EVIL_CONSTRUCTORS(ASessionDescription);
};

}  // namespace android

#endif  // A_SESSION_DESCRIPTION_H_
