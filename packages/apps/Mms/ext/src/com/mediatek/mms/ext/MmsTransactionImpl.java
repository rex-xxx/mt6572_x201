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

package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;

import com.mediatek.encapsulation.MmsLog;
import com.mediatek.mms.ext.IMmsTransaction;

/// M: ALPS00440523, set service to foreground @ {
import android.app.Service;
/// @}
/// M: ALPS00545779, for FT, restart pending receiving mms @ {
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
/// @}

/// M: ALPS00452618, set special HTTP retry handler for CMCC FT @
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
/// @}

public class MmsTransactionImpl extends ContextWrapper implements IMmsTransaction {
    private static final String TAG = "Mms/MmsTransactionImpl";
    private Context mContext = null;

    public MmsTransactionImpl(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Returns continuous server fail count.
     *
     * @return              Number of continuous server fail.
     */
    public int getMmsServerFailCount() {
        MmsLog.d(TAG, "getMmsServerFailCount");
        return 0;
    }

    /**
     * Set status code from server this time, if it is server fail need handled, will make
     * server fail count inscrease
     * @param value         Status code from server
     *
     */
    public void setMmsServerStatusCode(int code) {
        MmsLog.d(TAG, "setMmsServerStatusCode");
    }

    /**
     * Update connection if we meet same server error many times.
     *
     * @return              If it really update connection returns true, otherwise false.
     */
    public boolean updateConnection() {
        MmsLog.d(TAG, "updateConnection");
        return false;
    }

    public boolean isSyncStartPdpEnabled() {
        MmsLog.d(TAG, "isSyncStartPdpEnabled");
        return false;
    }

    /// M: ALPS00452618, set special HTTP retry handler for CMCC FT @
    /**
     * Get HTTP request retry handler
     *
     * @return              Return DefaultHttpRequestRetryHandler instance
     */
    public DefaultHttpRequestRetryHandler getHttpRequestRetryHandler() {
        MmsLog.d(TAG, "getHttpRequestRetryHandler");
        return new DefaultHttpRequestRetryHandler(1, true);
    }
    /// @}

    /// M: ALPS00440523, set service to foreground @
    /**
     * Set service to foreground
     *
     * @param service         Service that need to be foreground
     */
    public void startServiceForeground(Service service) {
        MmsLog.d(TAG, "startServiceForeground");
    }

    /**
     * Set service to foreground
     *
     * @param service         Service that need stop to be foreground
     */
    public void stopServiceForeground(Service service) {
        MmsLog.d(TAG, "stopServiceForeground");
    }

    /**
     * Check support auto restart incompleted mms transactions or not
     *
     * @return              If support return true, otherwise return false
     */
    public boolean isRestartPendingsEnabled() {
        MmsLog.d(TAG, "isRestartPendingsEnabled");
        return true;
    }
    /// @}

    /// M: ALPS00440523, set property @ {
    public void setSoSendTimeoutProperty() {
        MmsLog.d(TAG, "setSoSendTimeoutProperty");
    }
    /// @}

    /// M: ALPS00545779, for FT, restart pending receiving mms @ {
    /* On default,  only check failureType. If it is transient failed message then need restart*/
    public boolean isPendingMmsNeedRestart(Uri pduUri, int failureType) {
        MmsLog.d(TAG, "isPendingMmsNeedRestart, uri=" + pduUri);

        final int PDU_COLUMN_STATUS = 2;
        final String[] PDU_PROJECTION = new String[] {
            Mms.MESSAGE_BOX,
            Mms.MESSAGE_ID,
            Mms.STATUS,
        };
        Cursor c = null;
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            c = contentResolver.query(pduUri, PDU_PROJECTION, null, null, null);

            if ((c == null) || (c.getCount() != 1) || !c.moveToFirst()) {
                MmsLog.d(TAG, "Bad uri");
                return true;
            }

            int status = c.getInt(PDU_COLUMN_STATUS);
            MmsLog.v(TAG, "status" + status);

            /* This notification is not processed yet, so need restart*/
            if (status == 0) {
                return true;
            }
            /* DEFERRED_MASK is not set, it is auto download*/
            if ((status & 0x04) == 0) {
                return isTransientFailure(failureType);
            }
            /* Reach here means it is manully download*/
            return false;
        } catch (SQLiteException e) {
            MmsLog.e(TAG, "Catch a SQLiteException when query: ", e);
            return isTransientFailure(failureType);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static boolean isTransientFailure(int type) {
        MmsLog.d(TAG, "isTransientFailure, type=" + type);
        return (type < MmsSms.ERR_TYPE_GENERIC_PERMANENT);
    }
    /// @}
}

