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

import com.android.mms.MmsConfig;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.Recycler;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.AcknowledgeInd;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.EncodedStringValue;
import android.database.sqlite.SqliteWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
/// M:
import static com.google.android.mms.pdu.PduHeaders.STATUS_EXPIRED;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.mms.MmsApp;
import com.android.mms.ui.MmsPreferenceActivity;
import com.android.mms.ui.NotificationPreferenceActivity;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.NotifyRespInd;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;

//for Plugin
import com.android.mms.MmsPluginManager;
import com.mediatek.mms.ext.IMmsDialogNotify;

/**
 * The RetrieveTransaction is responsible for retrieving multimedia
 * messages (M-Retrieve.conf) from the MMSC server.  It:
 *
 * <ul>
 * <li>Sends a GET request to the MMSC server.
 * <li>Retrieves the binary M-Retrieve.conf data and parses it.
 * <li>Persists the retrieve multimedia message.
 * <li>Determines whether an acknowledgement is required.
 * <li>Creates appropriate M-Acknowledge.ind and sends it to MMSC server.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 */
public class RetrieveTransaction extends Transaction implements Runnable {
    private static final String TAG = "RetrieveTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private final Uri mUri;
    private final String mContentLocation;
    private boolean mLocked;

    static final String[] PROJECTION = new String[] {
        Mms.CONTENT_LOCATION,
        Mms.LOCKED
    };

    // The indexes of the columns which must be consistent with above PROJECTION.
    static final int COLUMN_CONTENT_LOCATION      = 0;
    static final int COLUMN_LOCKED                = 1;

    /// M:Code analyze 001,new members,using for judging if the mms in mmsc is expired or not @{
    private boolean mExpiry = false;
    /// @{

    public RetrieveTransaction(Context context, int serviceId,
            TransactionSettings connectionSettings, String uri)
            throws MmsException {
        super(context, serviceId, connectionSettings);

        if (uri.startsWith("content://")) {
            mUri = Uri.parse(uri); // The Uri of the M-Notification.ind
            mId = mContentLocation = getContentLocation(context, mUri);
            if (LOCAL_LOGV) {
                Log.v(TAG, "X-Mms-Content-Location: " + mContentLocation);
            }
        } else {
            throw new IllegalArgumentException(
                    "Initializing from X-Mms-Content-Location is abandoned!");
        }

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /// M:Code analyze 002,changed to the same format with JB, @{
    private String getContentLocation(Context context, Uri uri)
            throws MmsException {
        /// @}
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "RetrieveTransaction: getContentLocation()");
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            uri, PROJECTION, null, null, null);
        mLocked = false;

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    // Get the locked flag from the M-Notification.ind so it can be transferred
                    // to the real message after the download.
                    mLocked = cursor.getInt(COLUMN_LOCKED) == 1;
                    return cursor.getString(COLUMN_CONTENT_LOCATION);
                }
            } finally {
                cursor.close();
            }
        }

        throw new MmsException("Cannot get X-Mms-Content-Location from: " + uri);
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.transaction.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this, "RetrieveTransaction").start();
    }

    public void run() {
        try {
            /// M:Code analyze 003, check expiry,this operation must be done before markState function, @{
            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(mContext).load(mUri);
            if (nInd.getExpiry() < System.currentTimeMillis()/1000L) {
                /// M:Code analyze 001,new members,using for judging if the mms in mmsc is expired or not @{
                mExpiry = true;
                /// @}
                MmsLog.d(MmsApp.TXN_TAG, "The message is expired!");
                try {
                     ///M: modify for cmcc, don't send STATUS_EXPIRED res in cmcc project @{
                    if (MmsConfig.isSendExpiredResIfNotificationExpired()) {
                        sendNotifyRespInd(STATUS_EXPIRED);
                    }
                    /// @}
                } catch (IOException e) { 
                    // we should run following func to delete expired notification,
                    // no matter what happen. so, catch throwable
                    MmsLog.e(MmsApp.TXN_TAG, Log.getStackTraceString(e));
                } catch (MmsException e) {
                    MmsLog.e(MmsApp.TXN_TAG, Log.getStackTraceString(e));
                }
            }
            /// @}
            
            // Change the downloading state of the M-Notification.ind.
            DownloadManager.getInstance().markState(
                    mUri, DownloadManager.STATE_DOWNLOADING);

            /// M:Code analyze 004, check expiry,if this notification expiry, we should not download
            /// message from network @{
            if (mExpiry) {
                mTransactionState.setState(TransactionState.SUCCESS);
                mTransactionState.setContentUri(mUri);
                return;
            }
            /// @}

            // Send GET request to MMSC and retrieve the response data.
            byte[] resp = getPdu(mContentLocation);

            // Parse M-Retrieve.conf
            RetrieveConf retrieveConf = (RetrieveConf) new PduParser(resp).parse();
            if (null == retrieveConf) {
                /// M:
                MmsLog.e(MmsApp.TXN_TAG, "RetrieveTransaction: run(): Invalid M-Retrieve.conf PDU!!!");
                throw new MmsException("Invalid M-Retrieve.conf PDU.");
            }

            Uri msgUri = null;
            if (isDuplicateMessage(mContext, retrieveConf)) {
                /// M:
                MmsLog.w(MmsApp.TXN_TAG, "RetrieveTransaction: run, DuplicateMessage");
                // Mark this transaction as failed to prevent duplicate
                // notification to user.
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mUri);
            } else {
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "RetrieveTransaction: run, Store M-Retrieve.conf into Inbox");
                // Store M-Retrieve.conf into Inbox
                PduPersister persister = PduPersister.getPduPersister(mContext);
                /// M: google jb.mr1 patch, group mms
                msgUri = persister.persist(retrieveConf, Inbox.CONTENT_URI, true,
                        MmsPreferenceActivity.getIsGroupMmsEnabled(mContext));

                /// M:Code analyze 005, modify the logic,set message size and sim id(geimini) @{
                int messageSize = resp.length;
                ContentValues values = new ContentValues();
                values.put(Mms.MESSAGE_SIZE, messageSize);
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    values.put(EncapsulatedTelephony.Mms.SIM_ID, mSimId);
                } else {
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, 0);
                    if (si == null) {
                        MmsLog.e(MmsApp.TXN_TAG, "si null");
                    } else {
                        MmsLog.e(MmsApp.TXN_TAG, "simid=" + si.getSimId());
                        values.put(EncapsulatedTelephony.Mms.SIM_ID, (int) si.getSimId());
                    }
                }
                SqliteWrapper.update(mContext, mContext.getContentResolver(), msgUri, values, null, null);
                /// @}

                // Use local time instead of PDU time
                /// M: Code analyze 006,we do this in framework pdu layer @{
                //ContentValues values = new ContentValues(1);
                //values.put(Mms.DATE, System.currentTimeMillis() / 1000L);
                //SqliteWrapper.update(mContext, mContext.getContentResolver(),
                //       msgUri, values, null, null);
                /// @}

                // The M-Retrieve.conf has been successfully downloaded.
                mTransactionState.setState(TransactionState.SUCCESS);
                mTransactionState.setContentUri(msgUri);
                // Remember the location the message was downloaded from.
                // Since it's not critical, it won't fail the transaction.
                // Copy over the locked flag from the M-Notification.ind in case
                // the user locked the message before activating the download.
                updateContentLocation(mContext, msgUri, mContentLocation, mLocked);
            }

            /// M:Code analyze 006, modify the logic,when notifId don't equals msgId,delete notification msg,
            /// if msgUri == null,that means the notification is duplicated with last one,delete it @{
            if (msgUri != null) {
                // Delete the corresponding M-Notification.ind.
                String notifId = mUri.getLastPathSegment();
                String msgId = msgUri.getLastPathSegment();
                if (!notifId.equals(msgId)) {
                    SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                         mUri, null, null);
                }
                // Have to delete messages over limit *after* the delete above. Otherwise,
                // it would be counted as part of the total.
                Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, msgUri);
                MmsWidgetProvider.notifyDatasetChanged(mContext);
            } else {
                // is Duplicate Message, delete notification
                SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                         mUri, null, null);
            }
            /// @}

            /// M:Code analyze 007,add the paragraph below, ring in time @{
            if (mTransactionState.getState() == TransactionState.SUCCESS) {
                /// M: ALPS00435876, pass threadId so that no notification when new msg comes in its owning conversation @{
                long threadId = MessagingNotification.getThreadId(mContext, msgUri);
                MessagingNotification.blockingUpdateNewMessageIndicator(mContext, threadId, false);
                /// @}
                MessagingNotification.updateDownloadFailedNotification(mContext);
            }
            /// @}

            // Send ACK to the Proxy-Relay to indicate we have fetched the
            // MM successfully.
            // Don't mark the transaction as failed if we failed to send it.
            sendAcknowledgeInd(retrieveConf);
            /// M:Code analyze 008, CMCC new sms dialog @{
            if (msgUri != null && NotificationPreferenceActivity.isPopupNotificationEnable()) {
                IMmsDialogNotify dialogPlugin = (IMmsDialogNotify)MmsPluginManager
                        .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
                dialogPlugin.notifyNewSmsDialog(msgUri);
            }
            /// @}
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mUri);
                Log.e(TAG, "Retrieval failed.");
            }
            notifyObservers();
        }
    }

    /// M:Code analyze 009, remove static,don't waste unnecessary memory,because static function
    /// waste more memory than non-static function
    private boolean isDuplicateMessage(Context context, RetrieveConf rc) {
        byte[] rawMessageId = rc.getMessageId();
        /// M:Code analyze 010,new two variables for adding query condition @{
        byte[] rawContentType = rc.getContentType();
        byte[] rawMessageClass = rc.getMessageClass();
        /// @}
        if (rawMessageId != null && rawContentType != null && rawMessageClass != null) {
            String messageId = new String(rawMessageId);
            /// M:Code analyze 010, modify the logic,adding query condition,if in gemini mode,
            /// add another more variable SimId for limiting query condition @{ */
            String contentType = new String(rawContentType);
            String messageClass = new String(rawMessageClass);
            String selection = "(" + Mms.MESSAGE_ID + " = ? AND "
                                   + Mms.MESSAGE_TYPE + " = ? AND "
                                   + Mms.CONTENT_TYPE + " = ? AND "
                                   + Mms.MESSAGE_CLASS + " = ?)";
            //each card has it's own mms.
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                selection += " AND " + EncapsulatedTelephony.Mms.SIM_ID + " = " + mSimId;
            }
            /// @}

            /// M:Code analyze 010,here we need add some judgment element, because some operators
            /// return read report in the form of common MMS with the same MESSAGE_ID and MESSAGE_TYPE
            /// as the MMS it belongs to. That result in incorrect judgments.so adding query condition @{
            String[] selectionArgs = new String[] { messageId,
                    String.valueOf(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF),
                    contentType, messageClass };
            /// @}
            Cursor cursor = SqliteWrapper.query(
                    context, context.getContentResolver(),
                    Mms.CONTENT_URI, new String[] { Mms._ID, Mms.SUBJECT, Mms.SUBJECT_CHARSET },
                    selection, selectionArgs, null);

            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        // A message with identical message ID and type found.
                        // Do some additional checks to be sure it's a duplicate.
                        return isDuplicateMessageExtra(cursor, rc);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }

    private static boolean isDuplicateMessageExtra(Cursor cursor, RetrieveConf rc) {
        // Compare message subjects, taking encoding into account
        EncodedStringValue encodedSubjectReceived = null;
        EncodedStringValue encodedSubjectStored = null;
        String subjectReceived = null;
        String subjectStored = null;
        String subject = null;

        encodedSubjectReceived = rc.getSubject();
        if (encodedSubjectReceived != null) {
            subjectReceived = encodedSubjectReceived.getString();
        }

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int subjectIdx = cursor.getColumnIndex(Mms.SUBJECT);
            int charsetIdx = cursor.getColumnIndex(Mms.SUBJECT_CHARSET);
            subject = cursor.getString(subjectIdx);
            int charset = cursor.getInt(charsetIdx);
            if (subject != null) {
                encodedSubjectStored = new EncodedStringValue(charset, PduPersister
                        .getBytes(subject));
            }
            if (encodedSubjectStored == null && encodedSubjectReceived == null) {
                // Both encoded subjects are null - return true
                return true;
            } else if (encodedSubjectStored != null && encodedSubjectReceived != null) {
                subjectStored = encodedSubjectStored.getString();
                if (!TextUtils.isEmpty(subjectStored) && !TextUtils.isEmpty(subjectReceived)) {
                    // Both decoded subjects are non-empty - compare them
                    return subjectStored.equals(subjectReceived);
                } else if (TextUtils.isEmpty(subjectStored) && TextUtils.isEmpty(subjectReceived)) {
                    // Both decoded subjects are "" - return true
                    return true;
                }
            }
        }

        return false;
    }

    private void sendAcknowledgeInd(RetrieveConf rc) throws MmsException, IOException {
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "RetrieveTransaction: sendAcknowledgeInd()");
        // Send M-Acknowledge.ind to MMSC if required.
        // If the Transaction-ID isn't set in the M-Retrieve.conf, it means
        // the MMS proxy-relay doesn't require an ACK.
        byte[] tranId = rc.getTransactionId();
        if (tranId != null) {
            // Create M-Acknowledge.ind
            AcknowledgeInd acknowledgeInd = new AcknowledgeInd(
                    PduHeaders.CURRENT_MMS_VERSION, tranId);

            // insert the 'from' address per spec
            /// M:Code analyze 011,add for gemini, modify the logic @{ */
            String lineNumber = null;
            // add for gemini
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                lineNumber = MessageUtils.getLocalNumberGemini(mSimId);
            } else {
                lineNumber = MessageUtils.getLocalNumber();
            }
            /// @}
            acknowledgeInd.setFrom(new EncodedStringValue(lineNumber));

            /// M:Code analyze 012,add for new feature,judge if it is allowed to send delivery report
            /// for acknowledgeInd transaction @{
            if (MmsConfig.getDeliveryReportAllowed()) {
                // X-Mms-Report-Allowed Optional
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean reportAllowed = true;
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    reportAllowed = prefs.getBoolean(Integer.toString(mSimId)+ "_" + 
                            MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT,
                            true);
                } else {
                    reportAllowed = prefs.getBoolean(MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT, true);
                }

                MmsLog.d(MmsApp.TXN_TAG, "reportAllowed: " + reportAllowed);
            
                try {
                    acknowledgeInd.setReportAllowed(reportAllowed ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
                } catch(InvalidHeaderValueException ihve) {
                    // do nothing here
                    MmsLog.e(MmsApp.TXN_TAG, "acknowledgeInd.setReportAllowed Failed !!");
                }
            }
            /// @}

            // Pack M-Acknowledge.ind and send it
            if(MmsConfig.getNotifyWapMMSC()) {
                sendPdu(new PduComposer(mContext, acknowledgeInd).make(), mContentLocation);
            } else {
                sendPdu(new PduComposer(mContext, acknowledgeInd).make());
            }
        }
    }

    private static void updateContentLocation(Context context, Uri uri,
                                              String contentLocation,
                                              boolean locked) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "RetrieveTransaction: updateContentLocation()");

        ContentValues values = new ContentValues(2);
        values.put(Mms.CONTENT_LOCATION, contentLocation);
        values.put(Mms.LOCKED, locked);     // preserve the state of the M-Notification.ind lock.
        SqliteWrapper.update(context, context.getContentResolver(),
                             uri, values, null, null);
    }

    @Override
    public int getType() {
        return RETRIEVE_TRANSACTION;
    }

    /// M:code analyze 013, a new constructor for gemini @{
    public RetrieveTransaction(Context context, int serviceId, int simId,
            TransactionSettings connectionSettings, String uri)
            throws MmsException {
        super(context, serviceId, connectionSettings);
        mSimId = simId;

        if (uri.startsWith("content://")) {
            mUri = Uri.parse(uri); // The Uri of the M-Notification.ind
            mContentLocation = getContentLocation(context, mUri);
            mId = mContentLocation;
            if (LOCAL_LOGV) {
                Log.v(TAG, "X-Mms-Content-Location: " + mContentLocation);
            }
        } else {
            throw new IllegalArgumentException(
                    "Initializing from X-Mms-Content-Location is abandoned!");
        }

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }
    /// @}

    /// M:Code analyze 014,add for getting the uri of current retrieveTransaction
    public Uri getRtrTrxnUri() {
        return mUri;
    }
    /// @}

    /// M:Code analyze 015,send expired in MM1_notification.Res if the notification expired
    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        MmsLog.d(MmsApp.TXN_TAG, "RetrieveTransaction: sendNotifyRespInd for expired.");
        NotificationInd notificationInd = (NotificationInd) PduPersister.getPduPersister(mContext).load(mUri);
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                notificationInd.getTransactionId(),
                status);

        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            sendPdu(new PduComposer(mContext, notifyRespInd).make(), mContentLocation);
        } else {
            sendPdu(new PduComposer(mContext, notifyRespInd).make());
        }
    }
    /// @}
}
