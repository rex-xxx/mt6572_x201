/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.internal.telephony;

import android.util.Log;

import java.io.ByteArrayOutputStream;

public class MessageWaitingIndication {
    public static final String TAG = "MWI";

    public static final int ELT_ID_SPECIAL_SMS_MESSAGE_INDICATION = 0x01;

    public static final int MWI_TYPE_VOICEMAIL = 0x00;
    public static final int MWI_TYPE_FAX = 0x01;
    public static final int MWI_TYPE_EMAIL = 0x02;
    public static final int MWI_TYPE_EXT_VEDIO = 0x07;

    public static final int MWI_PROFILE_ID_1 = 0x00;
    public static final int MWI_PROFILE_ID_2 = 0x01;
    public static final int MWI_PROFILE_ID_3 = 0x02;
    public static final int MWI_PROFILE_ID_4 = 0x03;

    private boolean mwiDontStore = true;
    private int mwiType = -1;
    private int mwiCount = -1;
    private int mwiProfileId = -1;

    public MessageWaitingIndication(int octet1, int octet2) {
        Log.d(TAG, "read [" + octet1 + ", " + octet2 + "]");

        boolean dontStore = ((octet1 & 0x80) == 0);
        int type = ((octet1 & 0x03) != 0x03) ? (octet1 & 0x03) : (octet1 & 0x1f);
        int profileId = ((octet1 >> 5) & 0x03);
        int count = octet2;

        this.mwiDontStore = dontStore;
        this.mwiType = type;
        if (count > 255) {
            this.mwiCount = 255;
        } else if (count < 0) {
            this.mwiCount = -1;
        } else {
            this.mwiCount = count;
        }
        this.mwiProfileId = profileId;
    }

    private boolean isMwiAvailable() {
        boolean isAvailable = true;
        if (mwiType != MWI_TYPE_VOICEMAIL
                && mwiType != MWI_TYPE_FAX
                && mwiType != MWI_TYPE_EMAIL
                && mwiType != MWI_TYPE_EXT_VEDIO) {
            isAvailable = false;
            Log.e(TAG, "inavailable MWI type: " + mwiType);
        }

        if (mwiProfileId != MWI_PROFILE_ID_1
                && mwiProfileId != MWI_PROFILE_ID_2
                && mwiProfileId != MWI_PROFILE_ID_3
                && mwiProfileId != MWI_PROFILE_ID_4) {
            isAvailable = false;
            Log.e(TAG, "inavailable MWI profile ID: " + mwiProfileId);
        }

        return isAvailable;
    }

    public boolean isMwiDontStore() {
        return mwiDontStore;
    }

    public int getMwiType() {
        return mwiType;
    }

    public int getMwiCount() {
        return mwiCount;
    }

    public int getMwiProfileId() {
        return mwiProfileId;
    }

    public void toByteArray(ByteArrayOutputStream out) {
        out.write(ELT_ID_SPECIAL_SMS_MESSAGE_INDICATION);
        out.write(0x02); // this element contains 2 octets

        int octet1 = (mwiType | (mwiProfileId << 5) | (mwiDontStore ? 0x80 : 0));
        int octet2 = mwiCount;
        Log.d(TAG, "write [" + octet1 + ", " + octet2 + "]");
        out.write(octet1);
        out.write(octet2);
    }
}
