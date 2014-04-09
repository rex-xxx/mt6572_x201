/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.ipmessage;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;

import com.mediatek.encapsulation.MmsLog;

/**
* Provide contact management related interface
*/
public class ContactManager extends ContextWrapper {
    public static final String TAG = "Mms/ipmsgContactManagerImpl";
    public Context mContext = null;

    public ContactManager(Context context) {
        super(context);
        mContext = context;
    }

    public String getNumberByEngineId(short engineId) {
        MmsLog.d(TAG, "getNumberByEngineId called");
        return "";
    }

    public String getNumberByMessageId(long messageId) {
        MmsLog.d(TAG, "getNumberByMessageId called");
        return "";
    }

    public String getNumberByThreadId(long threadId) {
        MmsLog.d(TAG, "getNumberByThreadId called");
        return "";
    }

    public short getContactIdByNumber(String number) {
        MmsLog.d(TAG, "getContactIdByNumber called");
        return 0;
    }

    public int getTypeByNumber(String number) {
        MmsLog.d(TAG, "getTypeByNumber called");
        return 0;
    }

    public int getStatusByNumber(String number) {
        MmsLog.d(TAG, "getStatusByNumber called");
        return 0;
    }

    public int getOnlineTimeByNumber(String number) {
        MmsLog.d(TAG, "getOnlineTimeByNumber called");
        return 0;
    }

    public String getNameByNumber(String number) {
        MmsLog.d(TAG, "getNameByNumber called");
        return "";
    }

    public String getNameByThreadId(long threadId) {
        MmsLog.d(TAG, "getNameByThreadId called");
        return "";
    }

    public String getSignatureByNumber(String number) {
        MmsLog.d(TAG, "getSignatureByNumber called");
        return "";
    }

    public Bitmap getAvatarByNumber(String number) {
        MmsLog.d(TAG, "getAvatarByNumber called");
        return null;
    }

    public Bitmap getAvatarByThreadId(long threadId) {
        MmsLog.d(TAG, "getAvatarByThreadId called");
        return null;
    }

    public boolean isIpMessageNumber(String number) {
        MmsLog.d(TAG, "isIpMessageNumber called");
        return false;
    }

    public boolean addContactToSpamList(int[] contactIds) {
        MmsLog.d(TAG, "addContactToSpamList called");
        return false;
    }

    public boolean deleteContactFromSpamList(int[] contactIds) {
        MmsLog.d(TAG, "deleteContactFromSpamList called");
        return false;
    }
}

