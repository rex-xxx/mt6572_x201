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

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadRecInd;
import com.google.android.mms.pdu.EncodedStringValue;
import com.android.mms.ui.MessageUtils;

import android.content.Context;
import android.net.Uri;
import android.provider.Telephony.Mms.Sent;
import android.util.Log;

import java.io.IOException;

/// M:
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.provider.Telephony.Mms;
import com.android.mms.MmsApp;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;

/**
 * M: this class is modified a lot, be careful how it is used.
 * The ReadRecTransaction is responsible for sending read report
 * notifications (M-read-rec.ind) to clients that have requested them.
 * It:
 *
 * <ul>
 * <li>Loads the read report indication from storage (Outbox).
 * <li>Packs M-read-rec.ind and sends it.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 */
/// M:Code analyze 001,implements Runnable,using for start a thread to process some operation
public class ReadRecTransaction extends Transaction implements Runnable {
    private static final String TAG = "ReadRecTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private final Uri mReadReportURI;

    /// M:Code analyze 002, new members,using for the thread hander @{
    private Thread mThread;
    /// @}

    public ReadRecTransaction(Context context,
            int transId,
            TransactionSettings connectionSettings,
            String uri) {
        super(context, transId, connectionSettings);
        mReadReportURI = Uri.parse(uri);
        mId = uri;

        // Attach the transaction to the instance of RetryScheduler.
        /// M:Code analyze 007, currently read report only try once, so don't need this. @{
        //attach(RetryScheduler.getInstance(context));
        /// @}
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.Transaction#process()
     */
    @Override
    public void process() {
        /// M:Code analyze 003, do the work in a new thread @{
        mThread = new Thread(this);
        mThread.start();
        /// @}
    }

    @Override
    public int getType() {
        return READREC_TRANSACTION;
    }

    /// M: new methods
    /// M:Code analyze 004,a new constructor for gemini @{
    public ReadRecTransaction(Context context,
            int transId, int simId,
            TransactionSettings connectionSettings,
            String uri) {
        super(context, transId, connectionSettings);
        mReadReportURI = Uri.parse(uri);
        mId = uri;
        mSimId = simId;

        // Attach the transaction to the instance of RetryScheduler.
        // currently read report only try once, so don't need this.
        //attach(RetryScheduler.getInstance(context));
    }
    /// @}

    /// Code analyze 005,a thread for processing the read report flow @{
    public void run() {
        MmsLog.d(MmsApp.TXN_TAG, "ReadRecTransaction: process()");
        int readReportState = 0;
        try {
            String messageId = null;
            long msgId = 0;
            EncodedStringValue[] sender = new EncodedStringValue[1];
            Cursor cursor = null;
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                    mReadReportURI,
                    new String[] {Mms.MESSAGE_ID, Mms.READ_REPORT, Mms._ID},
                    null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        messageId = cursor.getString(0);
                        readReportState = cursor.getInt(1);
                        msgId = cursor.getLong(2);
                    }
                    //if curosr==null, this means the mms is deleted during processing.
                    //exception will happened. catched by out catch clause.
                    //so do not catch exception here.
                } finally {
                    cursor.close();
                }
            }
            MmsLog.d(MmsApp.TXN_TAG,"messageid:" + messageId + ",and readreport flag:" + readReportState);
            if (readReportState != 130) {
                Log.d("MmsLog","processed. ignore this:" + mId);
                return;//read report only try to send once.
            }
            cursor = null;
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                    Uri.parse("content://mms/" + msgId + "/addr"),
                    new String[] { Mms.Addr.ADDRESS, Mms.Addr.CHARSET},
                    Mms.Addr.TYPE + " = " + PduHeaders.FROM, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String address = cursor.getString(0);
                        int charSet = cursor.getInt(1);
                        MmsLog.d(MmsApp.TXN_TAG,"find address:" + address + ",charset:" + charSet);
                        sender[0] = new EncodedStringValue(charSet, PduPersister.getBytes(address));
                    }
                    //if cursor == null exception will catched by out catch clause.
                } finally {
                    cursor.close();
                }
            }
            ReadRecInd readRecInd = new ReadRecInd(
                    new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes()),
                    messageId.getBytes(),
                    PduHeaders.CURRENT_MMS_VERSION,
                    PduHeaders.READ_STATUS_READ,//always set read.
                    sender);

            readRecInd.setDate(System.currentTimeMillis() / 1000);
            byte[] postingData = new PduComposer(mContext, readRecInd).make();
            MmsLog.d(MmsApp.TXN_TAG,"before send read report pdu.");
            sendPdu(postingData);            
            MmsLog.d(MmsApp.TXN_TAG,"after send read report pdu.ok");
            mTransactionState.setState(TransactionState.SUCCESS);
        } catch (Throwable t) {
            MmsLog.e(MmsApp.TXN_TAG, Log.getStackTraceString(t));
        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                MmsLog.e(MmsApp.TXN_TAG, "send read report failed.uri:" + mId);
            }
            mTransactionState.setContentUri(mReadReportURI);//useless.
            //whether success or fail, update database, this read report will not send any more.
            ContentValues values = new ContentValues(1);
            values.put(Mms.READ_REPORT, PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ);
            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                 mReadReportURI, values, null, null);            
            notifyObservers();
        }        
    }
    /// @}

    /// M:Code analyze 006,return current transaction's uri, add for gemini @{
    public Uri getRrecTrxnUri() {
        return mReadReportURI;
    }
    /// @}
}
