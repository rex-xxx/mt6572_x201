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
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
package com.android.mms.util;

import com.android.mms.R;

public class MessageConsts {
    public static final int ACTION_SHARE = 0;
    public static final int[] shareIconArr = { R.drawable.ipmsg_take_a_photo,
            R.drawable.ipmsg_record_a_video, R.drawable.ipmsg_record_an_audio,
            R.drawable.ipmsg_share_contact, R.drawable.ipmsg_choose_a_photo,
            R.drawable.ipmsg_choose_a_video, R.drawable.ipmsg_choose_an_audio,
            R.drawable.ipmsg_share_calendar, R.drawable.ipmsg_add_slideshow};
    public static final int[] ipmsgShareIconArr = { R.drawable.ipmsg_take_a_photo,
            R.drawable.ipmsg_record_a_video, R.drawable.ipmsg_record_an_audio,
            R.drawable.ipmsg_draw_a_sketch, R.drawable.ipmsg_choose_a_photo,
            R.drawable.ipmsg_choose_a_video, R.drawable.ipmsg_choose_an_audio,
            R.drawable.ipmsg_share_location, R.drawable.ipmsg_share_contact,
            R.drawable.ipmsg_share_calendar, R.drawable.ipmsg_add_slideshow};
    public static final int[] emoticonIdList = { R.drawable.emo_small_01, R.drawable.emo_small_02,
            R.drawable.emo_small_03, R.drawable.emo_small_04, R.drawable.emo_small_05,
            R.drawable.emo_small_06, R.drawable.emo_small_07, R.drawable.emo_small_08,
            R.drawable.emo_small_09, R.drawable.emo_small_10, R.drawable.emo_small_11,
            R.drawable.emo_small_12, R.drawable.emo_small_13, R.drawable.emo_small_14,
            R.drawable.emo_small_15, R.drawable.emo_small_16, R.drawable.emo_small_17,
            R.drawable.emo_small_18, R.drawable.emo_small_19, R.drawable.emo_small_20,
            R.drawable.emo_small_21, R.drawable.emo_small_22, R.drawable.emo_small_23,
            R.drawable.emo_small_24, R.drawable.emo_small_25, R.drawable.emo_small_26,
            R.drawable.emo_small_27, R.drawable.emo_small_28, R.drawable.emo_small_29,
            R.drawable.emo_small_30, R.drawable.emo_small_31, R.drawable.emo_small_32,
            R.drawable.emo_small_33, R.drawable.emo_small_34, R.drawable.emo_small_35,
            R.drawable.emo_small_36, R.drawable.emo_small_37, R.drawable.emo_small_38,
            R.drawable.emo_small_39, R.drawable.emo_small_40, R.drawable.good, R.drawable.no,
            R.drawable.ok, R.drawable.victory, R.drawable.seduce, R.drawable.down, R.drawable.rain,
            R.drawable.lightning, R.drawable.sun, R.drawable.microphone, R.drawable.clock,
            R.drawable.email, R.drawable.candle, R.drawable.birthday_cake, R.drawable.gift,
            R.drawable.star, R.drawable.heart, R.drawable.brokenheart, R.drawable.bulb,
            R.drawable.music, R.drawable.shenma, R.drawable.fuyun, R.drawable.rice,
            R.drawable.roses, R.drawable.film, R.drawable.aeroplane, R.drawable.umbrella,
            R.drawable.caonima, R.drawable.penguin, R.drawable.pig };

    /// M: add for common emoticon panel. @{
    public static final int[] defaultIconArr = { R.drawable.emo_small_01, R.drawable.emo_small_02,
        R.drawable.emo_small_03, R.drawable.emo_small_04, R.drawable.emo_small_05,
        R.drawable.emo_small_06, R.drawable.emo_small_07, R.drawable.emo_small_08,
        R.drawable.emo_small_09, R.drawable.emo_small_10, R.drawable.emo_small_11,
        R.drawable.emo_small_12, R.drawable.emo_small_13, R.drawable.emo_small_14,
        R.drawable.emo_small_15, R.drawable.emo_small_16, R.drawable.emo_small_17,
        R.drawable.emo_small_18, R.drawable.emo_small_19, R.drawable.emo_small_20,
        R.drawable.emo_small_21, R.drawable.emo_small_22, R.drawable.emo_small_23,
        R.drawable.emo_small_24, R.drawable.emo_small_25, R.drawable.emo_small_26,
        R.drawable.emo_small_27, R.drawable.emo_small_28, R.drawable.emo_small_29,
        R.drawable.emo_small_30, R.drawable.emo_small_31, R.drawable.emo_small_32,
        R.drawable.emo_small_33, R.drawable.emo_small_34, R.drawable.emo_small_35,
        R.drawable.emo_small_36, R.drawable.emo_small_37, R.drawable.emo_small_38,
        R.drawable.emo_small_39, R.drawable.emo_small_40};

    public static final int[] giftIconArr = {R.drawable.good, R.drawable.no,
        R.drawable.ok, R.drawable.victory, R.drawable.seduce, R.drawable.down, R.drawable.rain,
        R.drawable.lightning, R.drawable.sun, R.drawable.microphone, R.drawable.clock,
        R.drawable.email, R.drawable.candle, R.drawable.birthday_cake, R.drawable.gift,
        R.drawable.star, R.drawable.heart, R.drawable.brokenheart, R.drawable.bulb,
        R.drawable.music, R.drawable.shenma, R.drawable.fuyun, R.drawable.rice,
        R.drawable.roses, R.drawable.film, R.drawable.aeroplane, R.drawable.umbrella,
        R.drawable.caonima, R.drawable.penguin, R.drawable.pig };
    /// @}
}
