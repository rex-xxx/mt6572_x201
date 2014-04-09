/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
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

package com.android.exchange;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;

public class ExchangePreferences {

    // Preferences file
    public static final String PREFERENCES_FILE = "AndroidExchange.Main";

    // Preferences field names
    private static final String LOW_STORAGE = "isLowStorage";
    private static long sStorageOkSize = 0;
    private static final String REMOVE_STALE_MAILS = "isRemovedStaleMails";
    private static final String BAD_SYNC_KEY_MAILBOX_ID = "badSyncKeyMailboxId";
    /// M: the maximum low storage threshold value, which defined by MTK's framework team.
    private static final long MAX_LOW_STORAGE_THRESHOLD = 50 * 1024 * 1024;

    private static ExchangePreferences sPreferences;

    private final SharedPreferences mSharedPreferences;

    private ExchangePreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    /**
     * TODO need to think about what happens if this gets GCed along with the
     * Activity that initialized it. Do we lose ability to read Preferences in
     * further Activities? Maybe this should be stored in the Application
     * context.
     */
    public static synchronized ExchangePreferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = new ExchangePreferences(context);
        }
        return sPreferences;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return getPreferences(context).mSharedPreferences;
    }

    public void setLowStorage(boolean value) {
        mSharedPreferences.edit().putBoolean(LOW_STORAGE, value).commit();
    }

    public boolean getLowStorage() {
        return mSharedPreferences.getBoolean(LOW_STORAGE, false);
    }

    /**
     * This fuction is used to avoid Email can not recover from low_storage state. 
     * Because we could miss the DEVICE_STORAGE_OK broadcast in some unknown cases.
     */
    public void checkLowStorage() {
        /// M: Not checkout lowstorage when running testcase @{
        if (Configuration.IS_TEST) {
            return;
        }
        /// @}

        String storageDirectory = Environment.getDataDirectory().toString();
        StatFs stat = new StatFs(storageDirectory);
        long availableBlocks = stat.getAvailableBlocks();
        long blockSize = stat.getBlockSize();
        long blockCount = stat.getBlockCount();
        long remaining = availableBlocks * blockSize;

        /**
         * M: Because the DEVICE_STORAGE_LOW broadcast will be sent when left
         * storage less 10%, and was limited by 50M, so set sStorageOkSize and
         * the LOW_STORAGE flag follow with the system to avoid conflict with
         * System. @{
         */
        if (0 == sStorageOkSize) {
            long minStorageSize = blockCount * blockSize / 10;
            sStorageOkSize = minStorageSize > MAX_LOW_STORAGE_THRESHOLD ? MAX_LOW_STORAGE_THRESHOLD
                    : minStorageSize;
        }

        if (remaining > sStorageOkSize) {
            setLowStorage(false);
        } else {
            setLowStorage(true);
        }
        /** @} */
    }

    public void setRemovedStaleMails(boolean value) {
        mSharedPreferences.edit().putBoolean(REMOVE_STALE_MAILS, value).commit();
    }

    public boolean getRemovedStaleMails() {
        return mSharedPreferences.getBoolean(REMOVE_STALE_MAILS, false);
    }

    public void setBadSyncKeyMailboxId(long value) {
        mSharedPreferences.edit().putLong(BAD_SYNC_KEY_MAILBOX_ID, value).commit();
    }

    public long getBadSyncKeyMailboxId() {
        return mSharedPreferences.getLong(BAD_SYNC_KEY_MAILBOX_ID, -1);
    }
}
