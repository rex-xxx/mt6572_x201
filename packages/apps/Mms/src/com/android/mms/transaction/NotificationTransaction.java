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

import static com.android.mms.transaction.TransactionState.FAILED;
import static com.android.mms.transaction.TransactionState.INITIALIZED;
import static com.android.mms.transaction.TransactionState.SUCCESS;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
import static com.google.android.mms.pdu.PduHeaders.STATUS_DEFERRED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_RETRIEVED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_UNRECOGNIZED;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.Recycler;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.NotifyRespInd;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import android.database.sqlite.SqliteWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Mms.Inbox;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
/// M:
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.mms.ui.MmsPreferenceActivity;
import com.android.mms.ui.NotificationPreferenceActivity;
import com.android.mms.MmsPluginManager;
import com.google.android.mms.InvalidHeaderValueException;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.mediatek.mms.ext.IMmsDialogNotify;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;

import java.util.Date;

/**
 * The NotificationTransaction is responsible for handling multimedia
 * message notifications (M-Notification.ind).  It:
 *
 * <ul>
 * <li>Composes the notification response (M-NotifyResp.ind).
 * <li>Sends the notification response to the MMSC server.
 * <li>Stores the notification indication.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 *
 * NOTE: This MMS client handles all notifications with a <b>deferred
 * retrieval</b> response.  The transaction service, upon succesful
 * completion of this transaction, will trigger a retrieve transaction
 * in case the client is in immediate retrieve mode.
 */
public class NotificationTransaction extends Transaction implements Runnable {
    private static final String TAG = "NotificationTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private Uri mUri;
    private NotificationInd mNotificationInd;
    private String mContentLocation;

    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, String uriString) {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        /// M:Code analyze 001, Resolve the problem of MMS repeat receiving @{
        //mId = new String(mNotificationInd.getTransactionId());
        mId = new String(mNotificationInd.getContentLocation());
        /// @}
        mContentLocation = new String(mNotificationInd.getContentLocation());

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /**
     * This constructor is only used for test purposes.
     */
    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, NotificationInd ind) {
        super(context, serviceId, connectionSettings);

        try {
            // Save the pdu. If we can start downloading the real pdu immediately, don't allow
            // persist() to create a thread for the notificationInd because it causes UI jank.
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, Inbox.CONTENT_URI, !allowAutoDownload(),
                        MmsPreferenceActivity.getIsGroupMmsEnabled(context));
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        
        /// M:Code analyze 001, Resolve the problem of MMS repeat receiving @{
        //mId = new String(ind.getTransactionId());
        mId = new String(ind.getContentLocation());
        /// @}
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.mms.pdu.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this, "NotificationTransaction").start();
    }

    /// M: google JB.MR1 patch, group mms
    public boolean allowAutoDownload() {
        DownloadManager downloadManager = DownloadManager.getInstance();
        boolean autoDownload = false;
        boolean dataSuspended = false;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            autoDownload = downloadManager.isAuto(mSimId);
            EncapsulatedTelephonyManagerEx teleManager = EncapsulatedTelephonyManagerEx.getDefault();
            int datastate = teleManager.getDataState(SIMInfo.getSlotById(mContext, mSimId));
            dataSuspended = (datastate == TelephonyManager.DATA_SUSPENDED);
        } else {
            autoDownload = downloadManager.isAuto();
            dataSuspended = (MmsApp.getApplication().getTelephonyManager().getDataState()
                    == TelephonyManager.DATA_SUSPENDED);
        }
        return autoDownload && !dataSuspended;
    }

    public void run() {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "NotificationTransaction: run()");
        /*
        DownloadManager downloadManager = DownloadManager.getInstance();
        /// M:Code analyze 002, the logic is modified for gemini @{
        //boolean autoDownload = downloadManager.isAuto();
        //boolean dataSuspended = (MmsApp.getApplication().getTelephonyManager().getDataState() ==
        //        TelephonyManager.DATA_SUSPENDED);
        boolean autoDownload = false;
        boolean dataSuspended = false;
        // add for gemini
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            autoDownload = downloadManager.isAuto(mSimId);
            EncapsulatedTelephonyManagerEx teleManager = EncapsulatedTelephonyManagerEx.getDefault();
            int datastate = teleManager.getDataState(SIMInfo.getSlotById(mContext, mSimId));
            dataSuspended = (datastate == TelephonyManager.DATA_SUSPENDED);
        } else {
            autoDownload = downloadManager.isAuto();
            dataSuspended = (MmsApp.getApplication().getTelephonyManager().getDataState()
                    == TelephonyManager.DATA_SUSPENDED);
        }
        /// @}
        */
        boolean autoDownload = allowAutoDownload();
        DownloadManager downloadManager = DownloadManager.getInstance();
        try {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Notification transaction launched: " + this);
            }

            // By default, we set status to STATUS_DEFERRED because we
            // should response MMSC with STATUS_DEFERRED when we cannot
            // download a MM immediately.
            int status = STATUS_DEFERRED;

            /// M:Code analyze 003, add checking expiry state @{
            Date currentDate = new Date(System.currentTimeMillis());
            Date expiryDate = new Date(mNotificationInd.getExpiry() * 1000);
            MmsLog.d(MmsApp.TXN_TAG, "expiry time=" + expiryDate.toLocaleString()
                    + "\t current=" + currentDate.toLocaleString());
            /// @}
            // Don't try to download when data is suspended, as it will fail, so defer download
            if (!autoDownload) {
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "Not autoDownload! autoDonwload=" + autoDownload);
                       // + autoDownload + ", dataSuspended=" + dataSuspended);
                /// M:Code analyze 004, modify the logic for gemini,blockingUpdateNewMessageIndicator
                /// will make status bar showing message info and ring meantime @{
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED, mSimId);
                } else {
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, 0);
                    if (si == null) {
                        MmsLog.d(MmsApp.TXN_TAG, "si null");
                        downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED);
                    } else {
                        MmsLog.d(MmsApp.TXN_TAG, "simid=" + si.getSimId());
                        downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED, (int)si.getSimId());
                    }
                }
                /// M: ALPS00435876, pass threadId so that no notification when new msg comes in its owning conversation @{
                long threadId = MessagingNotification.getThreadId(mContext, mUri);
                MessagingNotification.blockingUpdateNewMessageIndicator(mContext, threadId, false);
                /// @}
                MessagingNotification.updateDownloadFailedNotification(mContext);
                /// @}
                sendNotifyRespInd(status);
                /// M:Code analyze 005, CMCC new sms dialog @{
                if (NotificationPreferenceActivity.isPopupNotificationEnable()) {
                    IMmsDialogNotify dialogPlugin = (IMmsDialogNotify)MmsPluginManager
                            .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
                    dialogPlugin.notifyNewSmsDialog(mUri);
                }
                /// @}
                return;
            }

            /// M:Code analyze 006, modify the logic for gemini @{
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING, mSimId);
            } else {
                SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, 0);
                if (si == null) {
                    MmsLog.d(MmsApp.TXN_TAG, "si null");
                    downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING);
                } else {
                    MmsLog.d(MmsApp.TXN_TAG, "simid=" + si.getSimId());
                    downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING, (int)si.getSimId());
                }
            }
            /// @}

            if (LOCAL_LOGV) {
                Log.v(TAG, "Content-Location: " + mContentLocation);
            }
            MmsLog.d(MmsApp.TXN_TAG, "Content-Location: " + mContentLocation);
            byte[] retrieveConfData = null;
            // We should catch exceptions here to response MMSC
            // with STATUS_DEFERRED.
            try {
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "NotificationTransaction: before getpdu");
                retrieveConfData = getPdu(mContentLocation);
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "NotificationTransaction: after getpdu");
            } catch (IOException e) {
                mTransactionState.setState(FAILED);
            }

            if (retrieveConfData != null) {
                GenericPdu pdu = new PduParser(retrieveConfData).parse();
                if ((pdu == null) || (pdu.getMessageType() != MESSAGE_TYPE_RETRIEVE_CONF)) {
                    Log.e(TAG, "Invalid M-RETRIEVE.CONF PDU. " +
                            (pdu != null ? "message type: " + pdu.getMessageType() : "null pdu"));
                    mTransactionState.setState(FAILED);
                    status = STATUS_UNRECOGNIZED;
                } else {
                    // Save the received PDU (must be a M-RETRIEVE.CONF).
                    PduPersister p = PduPersister.getPduPersister(mContext);
                    /// M: google jb.mr1 patch, group mms
                    Uri uri = p.persist(pdu, Inbox.CONTENT_URI, true,
                            MmsPreferenceActivity.getIsGroupMmsEnabled(mContext));
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "PDU Saved, Uri=" + uri + "\nDelete Notify Ind, Uri=" + mUri);
                    /// M:Code analyze 007, modify the logic,save message size and save simID if in 
                    /// gemini mode into database @{
                    int messageSize = retrieveConfData.length;
                    ContentValues values = new ContentValues();
                    values.put(Mms.MESSAGE_SIZE, messageSize);
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        values.put(EncapsulatedTelephony.Mms.SIM_ID, mSimId);
                    } else {
                        SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, 0);
                        if (si == null) {
                            MmsLog.d(MmsApp.TXN_TAG, "si null");
                        } else {
                            MmsLog.d(MmsApp.TXN_TAG, "simid=" + si.getSimId());
                            values.put(EncapsulatedTelephony.Mms.SIM_ID, (int)si.getSimId());
                        }
                    }
                    SqliteWrapper.update(mContext, mContext.getContentResolver(), uri, values, null, null);
                    /// @}

                    // Use local time instead of PDU time
                    /// M:Code analyze 008, we do this in framework pdu layer @{
                    //ContentValues values = new ContentValues(1);
                    //values.put(Mms.DATE, System.currentTimeMillis() / 1000L);
                    //SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    //        uri, values, null, null);
                    /// @}

                    // We have successfully downloaded the new MM. Delete the
                    // M-NotifyResp.ind from Inbox.
                    /// M:Code analyze 009,check is notifId equals to msgId,if no,delete M-NotifyResp.ind @{
                    String notifId = mUri.getLastPathSegment();
                    String msgId = uri.getLastPathSegment();
                    if (!notifId.equals(msgId)) {
                        SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                             mUri, null, null);
                        Log.v(TAG, "NotificationTransaction received new mms message: " + uri);
                        /// M: google jb.mr1 patch, Delete obsolete threads
                        SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                            Threads.OBSOLETE_THREADS_URI, null, null);
                    }
                    /// @}
                    // Notify observers with newly received MM.
                    mUri = uri;
                    status = STATUS_RETRIEVED;
                }
            /// M:Code analyze 010, add else to set status if retrieve failed @{
            }else{
                MmsLog.e(MmsApp.TXN_TAG, "retrieveConfData is null");
                mTransactionState.setState(FAILED);
                status = STATUS_UNRECOGNIZED;
            /// @}
            }

            // Check the status and update the result state of this Transaction.
            switch (status) {
                case STATUS_RETRIEVED:
                    mTransactionState.setState(SUCCESS);
                    break;
                case STATUS_DEFERRED:
                    // STATUS_DEFERRED, may be a failed immediate retrieval.
                    if (mTransactionState.getState() == INITIALIZED) {
                        mTransactionState.setState(SUCCESS);
                    }
                    break;
            }

            /// M:Code analyze 011, do more check @{
            if (mTransactionState.getState() == SUCCESS) {
                /// M: ALPS00435876, pass threadId so that no notification when new msg comes in its owning conversation @{
                long threadId = MessagingNotification.getThreadId(mContext, mUri);
                MessagingNotification.blockingUpdateNewMessageIndicator(mContext, threadId, false);
                /// @}
                MessagingNotification.updateDownloadFailedNotification(mContext);
                //Dialog mode
                if (NotificationPreferenceActivity.isPopupNotificationEnable()) {
                    IMmsDialogNotify dialogPlugin = (IMmsDialogNotify)MmsPluginManager
                            .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
                    dialogPlugin.notifyNewSmsDialog(mUri);
                }
            }
            // if the status is STATUS_UNRECOGNIZED, this happened when we don't get mms pdu from server,
            //this may be a server problem or network problem.
            //our policy is will retry later, so we must response a deferred status not this one.
            //otherwise the server may delete this mms, and when we retry it, it's another mms created by server to 
            //inform us that the old mms is not exist yet.
            if (status == STATUS_UNRECOGNIZED) {
                status = STATUS_DEFERRED;
            }
            /// @}
            sendNotifyRespInd(status);

            // Make sure this thread isn't over the limits in message count.
            Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
            MmsWidgetProvider.notifyDatasetChanged(mContext);
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
            /// M:Code analyze 012, check limit @{
            if (null != mUri){
                Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
            }
            /// @}
        } finally {
            mTransactionState.setContentUri(mUri);
            /// M:Code analyze 013, comment dataSuspend for ALPS00081452
            boolean isCheckAutoDownload = false;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                isCheckAutoDownload = downloadManager.isAuto(mSimId);
            } else {
                isCheckAutoDownload = downloadManager.isAuto();
            }
            if (!isCheckAutoDownload) {
                // Always mark the transaction successful for deferred
                // download since any error here doesn't make sense.
                mTransactionState.setState(SUCCESS);
            }
            if (mTransactionState.getState() != SUCCESS) {
                mTransactionState.setState(FAILED);
                /// M:Code analyze 014,change to Xlog @{
                MmsLog.w(MmsApp.TXN_TAG, "NotificationTransaction failed.");
                /// @}
            }
            notifyObservers();
        }
    }

    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "NotificationTransaction: sendNotifyRespInd()");
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                mNotificationInd.getTransactionId(),
                status);

        /// M:Code analyze 014, this paragraph below is using for judging if it is allowed
        /// to send delivery report,at present,we don't support delivery report in MMS @{
        if (MmsConfig.getDeliveryReportAllowed()) {
            // X-Mms-Report-Allowed Optional
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean reportAllowed = true;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                reportAllowed = prefs.getBoolean(Integer.toString(mSimId) + "_" +
                        MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT,
                        true);
            } else {
                reportAllowed = prefs.getBoolean(MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT, true);
            }

            MmsLog.d(MmsApp.TXN_TAG, "reportAllowed: " + reportAllowed);

            try {
                notifyRespInd.setReportAllowed(reportAllowed ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
            } catch (InvalidHeaderValueException ihve) {
                // do nothing here
                MmsLog.e(MmsApp.TXN_TAG, "notifyRespInd.setReportAllowed Failed !!");
            }
        }
        /// @}

        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            sendPdu(new PduComposer(mContext, notifyRespInd).make(), mContentLocation);
        } else {
            sendPdu(new PduComposer(mContext, notifyRespInd).make());
        }
    }

    @Override
    public int getType() {
        return NOTIFICATION_TRANSACTION;
    }

    /// M: new methods
    /// M:Code analyze 015, new constructors for gemini @{
    public NotificationTransaction(
            Context context, int serviceId, int simId,
            TransactionSettings connectionSettings, String uriString) {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        /// M:Code analyze 001, Resolve the problem of MMS repeat receiving @{
        //mId = new String(mNotificationInd.getTransactionId());
        mId = new String(mNotificationInd.getContentLocation());
        /// @}
        mSimId = simId;
        mContentLocation = new String(mNotificationInd.getContentLocation());

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }


    // add for gemini
    public NotificationTransaction(
            Context context, int serviceId, int simId,
            TransactionSettings connectionSettings, NotificationInd ind) {
        super(context, serviceId, connectionSettings);

        try {
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, Inbox.CONTENT_URI);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;

        /// M:Code analyze 001, Resolve the problem of MMS repeat receiving @{
        //mId = new String(ind.getTransactionId());
        mId = new String(ind.getContentLocation());
        /// @}
        mSimId = simId;
    }
    /// @}

    /// M:Code analyze 016, return current transaction's uri for gemini @{
    public Uri getNotTrxnUri(){
        return mUri;
    }
    /// @}
}
