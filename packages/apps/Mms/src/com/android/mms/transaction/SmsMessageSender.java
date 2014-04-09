/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.transaction;

import com.android.mms.LogTag;
import com.android.mms.ui.SmsPreferenceActivity;
import com.google.android.mms.MmsException;
import android.database.sqlite.SqliteWrapper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.util.Log;

/// M:
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.mms.MmsApp;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;


public class SmsMessageSender implements MessageSender {
    protected final Context mContext;
    protected final int mNumberOfDests;
    private final String[] mDests;
    protected final String mMessageText;
    protected final String mServiceCenter;
    protected final long mThreadId;
    protected long mTimestamp;
    private static final String TAG = "SmsMessageSender";

    // Default preference values
    private static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;

    private static final String[] SERVICE_CENTER_PROJECTION = new String[] {
        Sms.Conversations.REPLY_PATH_PRESENT,
        Sms.Conversations.SERVICE_CENTER,
    };

    private static final int COLUMN_REPLY_PATH_PRESENT = 0;
    private static final int COLUMN_SERVICE_CENTER     = 1;

    public SmsMessageSender(Context context, String[] dests, String msgText, long threadId) {
        mContext = context;
        mMessageText = msgText;
        if (dests != null) {
            mNumberOfDests = dests.length;
            mDests = new String[mNumberOfDests];
            System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
        } else {
            mNumberOfDests = 0;
            mDests = null;
        }
        mTimestamp = System.currentTimeMillis();
        mThreadId = threadId;
        mServiceCenter = getOutgoingServiceCenter(mThreadId);
    }

    public boolean sendMessage(long token) throws MmsException {
        // In order to send the message one by one, instead of sending now, the message will split,
        // and be put into the queue along with each destinations
        return queueMessage(token);
    }

    private boolean queueMessage(long token) throws MmsException {
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "queueMessage()");
        if ((mMessageText == null) || "".equals(mMessageText.trim()) || (mNumberOfDests == 0)) {
            // Don't try to send an empty message.
            throw new MmsException("Null message body or dest.");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        /// M:Code analyze 002, add for gemini to make sure if request delivery or not @{
        boolean requestDeliveryReport = false;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            requestDeliveryReport = prefs.getBoolean(Integer.toString(mSimId) + "_" +
                SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        } else {
            requestDeliveryReport = prefs.getBoolean(
                SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        }
        MmsLog.d(MmsApp.TXN_TAG, "SMS DR request=" + requestDeliveryReport);
        /// @}

        for (int i = 0; i < mNumberOfDests; i++) {
            try {
                if (LogTag.DEBUG_SEND) {
                    Log.v(TAG, "queueMessage mDests[i]: " + mDests[i] + " mThreadId: " + mThreadId);
                }
                /// M:Code analyze 003, add for gemini,add message to specific uri @{
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    EncapsulatedTelephony.Sms.addMessageToUri(mContext.getContentResolver(),
                        Uri.parse("content://sms/queued"), mDests[i],
                        mMessageText, null, mTimestamp,
                        true /* read */,
                        requestDeliveryReport,
                        mThreadId,
                        mSimId);
                } else {
                    EncapsulatedTelephony.Sms.addMessageToUri(mContext.getContentResolver(),
                        Uri.parse("content://sms/queued"), mDests[i],
                        mMessageText, null, mTimestamp,
                        true /* read */,
                        requestDeliveryReport,
                        mThreadId,
                        mSimId);
                }
                /// @}
            } catch (SQLiteException e) {
                if (LogTag.DEBUG_SEND) {
                    Log.e(TAG, "queueMessage SQLiteException", e);
                }
                SqliteWrapper.checkSQLiteException(mContext, e);
            }
        }
        // Notify the SmsReceiverService to send the message out
        /// M:Code analyze 004, send sms by sendBroadcast
        /// Support show SIM name in single SIM project @{
        Intent sentIt = new Intent(SmsReceiverService.ACTION_SEND_MESSAGE, null, mContext,
                SmsReceiver.class);
        sentIt.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, mSimId);
        mContext.sendBroadcast(sentIt);
        /// @}
        return false;
    }

    /**
     * Get the service center to use for a reply.
     *
     * The rule from TS 23.040 D.6 is that we send reply messages to
     * the service center of the message to which we're replying, but
     * only if we haven't already replied to that message and only if
     * <code>TP-Reply-Path</code> was set in that message.
     *
     * Therefore, return the service center from the most recent
     * message in the conversation, but only if it is a message from
     * the other party, and only if <code>TP-Reply-Path</code> is set.
     * Otherwise, return null.
     */
    private String getOutgoingServiceCenter(long threadId) {
        Cursor cursor = null;

        try {
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            Sms.CONTENT_URI, SERVICE_CENTER_PROJECTION,
                            /// M:Code analyze 005, modify where, add "AND" @{
                            "thread_id = " + threadId + " AND type = " + Sms.MESSAGE_TYPE_INBOX, null, "date DESC");
            /// @}

            if ((cursor == null) || !cursor.moveToFirst()) {
                return null;
            }

            boolean replyPathPresent = (1 == cursor.getInt(COLUMN_REPLY_PATH_PRESENT));
            return replyPathPresent ? cursor.getString(COLUMN_SERVICE_CENTER) : null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void log(String msg) {
        Log.d(LogTag.TAG, "[SmsMsgSender] " + msg);
    }

    /// M: new members and methods
    /// M:Code analyze 001,add for gemini to get sim info @{
    protected int mSimId = 0;
    /// @}
    public void setSimId(int simId) {
        mSimId = simId;
    }
}
