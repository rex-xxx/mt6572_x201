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

package com.android.mms.transaction;

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_READ_ORIG_IND;

import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.ui.MmsPreferenceActivity;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import android.database.sqlite.SqliteWrapper;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.util.Log;
/// M:
import android.provider.Telephony.MmsSms.PendingMessages;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.mms.MmsApp;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.WapPush;
import com.mediatek.mms.ext.IMmsTransaction;
/**
 * Receives Intent.WAP_PUSH_RECEIVED_ACTION intents and starts the
 * TransactionService by passing the push-data to it.
 */
public class PushReceiver extends BroadcastReceiver {
    private static final String TAG = "PushReceiver";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    /// M:Code analyze 001, add a lock
    private PowerManager.WakeLock mWakeLock;
    private class ReceivePushTask extends AsyncTask<Intent,Void,Void> {
        private Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];
            /// M:
            MmsLog.d(MmsApp.TXN_TAG, "do In Background, slotId=" + intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, 0));
            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData);
            int type = 0;
            try {
                GenericPdu pdu = parser.parse();

                if (null == pdu) {
                    Log.e(TAG, "Invalid PUSH data");
                    return null;
                }

                PduPersister p = PduPersister.getPduPersister(mContext);
                ContentResolver cr = mContext.getContentResolver();
                type = pdu.getMessageType();
                long threadId = -1;
                switch (type) {
                    case MESSAGE_TYPE_DELIVERY_IND:
                        /// M:
                        MmsLog.d(MmsApp.TXN_TAG, "type=MESSAGE_TYPE_DELIVERY_IND");
                        /// M: fall through
                    case MESSAGE_TYPE_READ_ORIG_IND: {
                        /** M: print log @{ */
                        if (type == MESSAGE_TYPE_READ_ORIG_IND) {
                            MmsLog.d(MmsApp.TXN_TAG, "type=MESSAGE_TYPE_READ_ORIG_IND");
                        }
                        /** @} */
                        threadId = findThreadId(mContext, pdu, type);
                        if (threadId == -1) {
                            // The associated SendReq isn't found, therefore skip
                            // processing this PDU.
                            break;
                        }

                        /// M: google jb.mr1 patch, group mms
                        Uri uri = p.persist(pdu, Inbox.CONTENT_URI, true,
                                MmsPreferenceActivity.getIsGroupMmsEnabled(mContext));
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        ContentValues values = new ContentValues(1);
                        values.put(Mms.THREAD_ID, threadId);
                        SqliteWrapper.update(mContext, cr, uri, values, null, null);
                        break;
                    }
                    case MESSAGE_TYPE_NOTIFICATION_IND: {
                        /// M:
                        MmsLog.d(MmsApp.TXN_TAG, "type=MESSAGE_TYPE_NOTIFICATION_IND");
                        NotificationInd nInd = (NotificationInd) pdu;

                        if (MmsConfig.getTransIdEnabled()) {
                            byte [] contentLocation = nInd.getContentLocation();
                            if ('=' == contentLocation[contentLocation.length - 1]) {
                                byte [] transactionId = nInd.getTransactionId();
                                byte [] contentLocationWithId = new byte [contentLocation.length
                                                                          + transactionId.length];
                                System.arraycopy(contentLocation, 0, contentLocationWithId,
                                        0, contentLocation.length);
                                System.arraycopy(transactionId, 0, contentLocationWithId,
                                        contentLocation.length, transactionId.length);
                                nInd.setContentLocation(contentLocationWithId);
                            }
                        }

                        if (!isDuplicateNotification(mContext, nInd)) {
                            /// M: google jb.mr1 patch, group mms
                            // Save the pdu. If we can start downloading the real pdu immediately,
                            // don't allow persist() to create a thread for the notificationInd
                            // because it causes UI jank.
                            Uri uri = p.persist(pdu, Inbox.CONTENT_URI,
                                    true,
                                    MmsPreferenceActivity.getIsGroupMmsEnabled(mContext));
                            /// M:Code analyze 002, add for gemini,update pdu and pending messages @{
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                // update pdu
                                ContentValues values = new ContentValues(2);
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext,
                                        intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
                                if (null == si) {
                                    MmsLog.e(MmsApp.TXN_TAG, "PushReceiver:SIMInfo is null for slot "
                                            + intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
                                    break;
                                }
                                values.put(EncapsulatedTelephony.Mms.SIM_ID, si.getSimId());
                                values.put(WapPush.SERVICE_ADDR, intent.getStringExtra(WapPush.SERVICE_ADDR));
                                SqliteWrapper.update(mContext, cr, uri, values, null, null);
                                MmsLog.d(MmsApp.TXN_TAG, "save notification slotId="
                                        + intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, 0)
                                        + "\tsimId=" + si.getSimId() 
                                        + "\tsc=" + intent.getStringExtra(WapPush.SERVICE_ADDR)
                                        + "\taddr=" + intent.getStringExtra(WapPush.ADDR));

                                // update pending messages
                                long msgId = 0;
                                Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                                                                uri, new String[] {Mms._ID}, null, null, null);
                                if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
                                    try {
                                        msgId = cursor.getLong(0);
                                        MmsLog.d(MmsApp.TXN_TAG, "msg id = " + msgId);
                                    } finally {
                                        cursor.close();
                                    }
                                }

                                Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
                                uriBuilder.appendQueryParameter("protocol", "mms");
                                uriBuilder.appendQueryParameter("message", String.valueOf(msgId));
                                Cursor pendingCs = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                                        uriBuilder.build(), null, null, null, null);
                                if (pendingCs != null) {
                                    try {
                                        if ((pendingCs.getCount() == 1) && pendingCs.moveToFirst()) {
                                            ContentValues valuesforPending = new ContentValues();
                                            valuesforPending.put(EncapsulatedTelephony.MmsSms.PendingMessages.SIM_ID, si.getSimId());
                                            int columnIndex = pendingCs.getColumnIndexOrThrow(PendingMessages._ID);
                                            long id = pendingCs.getLong(columnIndex);
                                            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                                            PendingMessages.CONTENT_URI,
                                                            valuesforPending, PendingMessages._ID + "=" + id, null);
                                        } else {
                                            MmsLog.w(MmsApp.TXN_TAG, "can not find message to set pending sim id, msgId="
                                                    + msgId);
                                        }
                                    } finally {
                                        pendingCs.close();
                                    }
                                }
                            } else {
                                ContentValues value = new ContentValues(1);
                                /// M: Add sim id to mms notification
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, 0);
                                if (null != si) {
                                    MmsLog.d(MmsApp.TXN_TAG, "single mms notifiy simid=" + si.getSimId());
                                    value.put(EncapsulatedTelephony.Mms.SIM_ID, si.getSimId());
                                }
                                /// @}
                                value.put(WapPush.SERVICE_ADDR, intent.getStringExtra(WapPush.SERVICE_ADDR));
                                SqliteWrapper.update(mContext, cr, uri, value, null, null);
                                MmsLog.d(MmsApp.TXN_TAG, "save notification," 
                                        + "\tsc=" + intent.getStringExtra(WapPush.SERVICE_ADDR)
                                        + "\taddr=" + intent.getStringExtra(WapPush.ADDR));
                            }
                            /// @}
                            // Start service to finish the notification transaction.
                            Intent svc = new Intent(mContext, TransactionService.class);
                            svc.putExtra(TransactionBundle.URI, uri.toString());
                            svc.putExtra(TransactionBundle.TRANSACTION_TYPE,
                                    Transaction.NOTIFICATION_TRANSACTION);
                            /// M:Code analyze 003, add for gemini,transfer the simId to TransactionService @{
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext,
                                        intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
                                if (null == si) {
                                    MmsLog.e(MmsApp.TXN_TAG, "PushReceiver: SIMInfo is null for slot "
                                            + intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
                                    break;
                                }
                                int simId = (int)si.getSimId();
                                svc.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, simId);
                                //svc.putExtra(Phone.GEMINI_SIM_ID_KEY, intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0));
                            }
                            /// @}
                            mContext.startService(svc);
                        } else if (LOCAL_LOGV) {
                            Log.v(TAG, "Skip downloading duplicate message: "
                                    + new String(nInd.getContentLocation()));
                        }
                        break;
                    }
                    default:
                        Log.e(TAG, "Received unrecognized PDU.");
                }
            } catch (MmsException e) {
                Log.e(TAG, "Failed to save the data from PUSH: type=" + type, e);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unexpected RuntimeException.", e);
            /// M:Code analyze 004,unknown, add finally @{
            }  finally {
                raisePriority(mContext, false);
                MmsLog.d(MmsApp.TXN_TAG, "Normal priority");
            /// @}
            }

            if (LOCAL_LOGV) {
                Log.v(TAG, "PUSH Intent processed.");
            }

            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION)
                && EncapsulatedContentType.MMS_MESSAGE.equals(intent.getType())) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Received PUSH Intent: " + intent);
            }

            /// M:Code analyze 004,unknown, raise priority @{
            MmsLog.d(MmsApp.TXN_TAG, "raise priority");
            raisePriority(context, true);
            /// @}

            /// M:Code analyze 005, Hold a wake lock for 5 seconds, enough to give any
            /// services we start time to take their own wake locks. @{
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                            "MMS PushReceiver");
            mWakeLock.acquire(5000);
            /// @}
            IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                      .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
           if ( mmsTransactionPlugin != null && mmsTransactionPlugin.isSyncStartPdpEnabled()) {
               intent.setClass(context, MmsPushReceiveService.class);            
               context.startService(intent);
           } else {
        	   new ReceivePushTask(context).execute(intent);
           }
        }
    }

    private static long findThreadId(Context context, GenericPdu pdu, int type) {
        String messageId;

        if (type == MESSAGE_TYPE_DELIVERY_IND) {
            messageId = new String(((DeliveryInd) pdu).getMessageId());
        } else {
            messageId = new String(((ReadOrigInd) pdu).getMessageId());
        }

        StringBuilder sb = new StringBuilder('(');
        sb.append(Mms.MESSAGE_ID);
        sb.append('=');
        sb.append(DatabaseUtils.sqlEscapeString(messageId));
        sb.append(" AND ");
        sb.append(Mms.MESSAGE_TYPE);
        sb.append('=');
        sb.append(PduHeaders.MESSAGE_TYPE_SEND_REQ);
        // TODO ContentResolver.query() appends closing ')' to the selection argument
        // sb.append(')');

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            Mms.CONTENT_URI, new String[] { Mms.THREAD_ID },
                            sb.toString(), null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return -1;
    }

    private static boolean isDuplicateNotification(
            Context context, NotificationInd nInd) {
        byte[] rawLocation = nInd.getContentLocation();
        if (rawLocation != null) {
            String location = new String(rawLocation);
            String selection = Mms.CONTENT_LOCATION + " = ?";
            String[] selectionArgs = new String[] { location };
            Cursor cursor = SqliteWrapper.query(
                    context, context.getContentResolver(),
                    Mms.CONTENT_URI, new String[] { Mms._ID },
                    selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        /// M:Code analyze 006,add log if duplicate @{
                        cursor.moveToFirst();
                        MmsLog.d(MmsApp.TXN_TAG, "duplicate, location=" + location + ", id=" + cursor.getLong(0));
                        /// @}
                        // We already received the same notification before.
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }

    /// M:Code analyze 004,unknown, new method @{
    private void raisePriority(Context context, boolean raise) {
        final Intent it = new Intent();
        it.setAction("android.intent.action.BOOST_DOWNLOADING");
        it.putExtra("package_name", "com.android.mms");
        it.putExtra("enabled", raise);
        context.sendBroadcast(it);
    }
    /// @}
}
