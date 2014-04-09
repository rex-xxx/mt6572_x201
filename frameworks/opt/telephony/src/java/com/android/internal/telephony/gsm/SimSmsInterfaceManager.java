/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.IntRangeManager;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.IntRangeManager;
import com.android.internal.telephony.SmsConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.telephony.SmsManager.STATUS_ON_ICC_FREE;

// mtk added by mtk80589 in 2011.11.16
import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_SUCCESS;
import static android.telephony.SmsManager.RESULT_ERROR_SIM_MEM_FULL;
import static android.telephony.SmsManager.RESULT_ERROR_INVALID_ADDRESS;

import static android.telephony.SmsManager.STATUS_ON_ICC_READ;
import static android.telephony.SmsManager.STATUS_ON_ICC_UNREAD;
import static android.telephony.SmsManager.STATUS_ON_ICC_SENT;
import static android.telephony.SmsManager.STATUS_ON_ICC_UNSENT;

import com.mediatek.common.telephony.IccSmsStorageStatus;
import android.telephony.SimSmsInsertStatus;
import android.telephony.SmsParameters;
// mtk added end
/* MTK start: icc file handler will be null while plug-out sim card */
import com.android.internal.telephony.IccFileHandler;
/* MTK end: icc file handler will be null while plug-out sim card */

/**
 * SimSmsInterfaceManager to provide an inter-process communication to
 * access Sms in Sim.
 */
public class SimSmsInterfaceManager extends IccSmsInterfaceManager {
    static final String LOG_TAG = "GSM";
    static final boolean DBG = true;

    private final Object mLock = new Object();
    private final Object mSimSmsLock = new Object();
    private final Object mDeleteLock = new Object();
    private final Object mSmsParamsLock = new Object();
    private boolean mSuccess;
    private List<SmsRawData> mSms;
    private HashMap<Integer, HashSet<String>> mCellBroadcastSubscriptions =
            new HashMap<Integer, HashSet<String>>();

    private CellBroadcastRangeManager mCellBroadcastRangeManager =
            new CellBroadcastRangeManager();

    private static final int EVENT_LOAD_DONE = 1;
    private static final int EVENT_UPDATE_DONE = 2;
    private static final int EVENT_SET_BROADCAST_ACTIVATION_DONE = 3;
    private static final int EVENT_SET_BROADCAST_CONFIG_DONE = 4;
    private static final int EVENT_SIM_SMS_DELETE_DONE = 5;
    
    // message id for ETWS
    private static final int EVENT_SET_ETWS_CONFIG_DONE = 100;

    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;

    // mtk added for SIM SMS storage by mtk80589 in 2011.11.16
    private IccSmsStorageStatus mSimMemStatus;
    private static final int EVENT_GET_SMS_SIM_MEM_STATUS_DONE = 103;
    // mtk added end
    // mtk added for text message inserting by mtk80589 in 2011.11.16
    private static final int EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE = 104;
    private boolean mInsertMessageSuccess;
    private final Object mSimInsertLock = new Object();
    private SimSmsInsertStatus smsInsertRet = new SimSmsInsertStatus(RESULT_ERROR_SUCCESS, "");
    private static int sConcatenatedRef = 456;
    private static final String INDEXT_SPLITOR = ",";
    // mtk added end
    // mtk added for raw message inserting by mtk80589 in 2011.11.16
    private SimSmsInsertStatus smsInsertRet2 = new SimSmsInsertStatus(RESULT_ERROR_SUCCESS, "");
    // mtk added end

    // mtk added for EFsmsp read/write by mtk80589 in 2011.11.16
    private static final int EVENT_GET_SMS_PARAMS = 105;
    private static final int EVENT_SET_SMS_PARAMS = 106;
    private SmsParameters mSmsParams = null;
    private boolean mSmsParamsSuccess = false;
    // mtk added end

    // mtk added for single sms record loading by mtk80589 in 2011.11.16
    private static final int EVENT_LOAD_ONE_RECORD_DONE = 107;
    private SmsRawData mSmsRawData = null;
    // mtk added end

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {
                case EVENT_SIM_SMS_DELETE_DONE:
                    ar = (AsyncResult) msg.obj;
                    synchronized (mDeleteLock) {
                        mSuccess = (ar.exception == null);
                        mDeleteLock.notifyAll();
                    }
                    break;
                case EVENT_UPDATE_DONE:
                    ar = (AsyncResult) msg.obj;
                    synchronized (mLock) {
                        mSuccess = (ar.exception == null);
                        if (mSuccess == true) {
                            try {
                                int index = ((int[])ar.result)[0];
                                smsInsertRet2.indexInIcc += (index + INDEXT_SPLITOR);
                                log("[insertRaw save one pdu in index " + index);
                            } catch (ClassCastException e) {
                                e.printStackTrace();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            log("[insertRaw fail to insert raw into ICC");
                            smsInsertRet2.indexInIcc += ("-1" + INDEXT_SPLITOR);
                        }
                        mLock.notifyAll();
                    }

                    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                    if (ar.exception != null) {
                        CommandException e = (CommandException)ar.exception;
                        if (DBG) {
                            log("Cannot update SMS " + e.getCommandError());
                        }

                        if (e.getCommandError() == CommandException.Error.SIM_MEM_FULL) {
                            mDispatcher.handleIccFull();
                        }
                    }
                    // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                    break;
                case EVENT_LOAD_DONE:
                    ar = (AsyncResult)msg.obj;
                    synchronized (mSimSmsLock) {
                        if (ar.exception == null) {
                            mSms  = buildValidRawData((ArrayList<byte[]>) ar.result);
                        } else {
                            if(DBG) log("Cannot load Sms records");
                            if (mSms != null)
                                mSms.clear();
                        }
                        mSimSmsLock.notifyAll();
                    }
                    break;
                case EVENT_SET_BROADCAST_ACTIVATION_DONE:
                case EVENT_SET_BROADCAST_CONFIG_DONE:
                case EVENT_SET_ETWS_CONFIG_DONE:
                    ar = (AsyncResult) msg.obj;
                    synchronized (mLock) {
                        mSuccess = (ar.exception == null);
                        mLock.notifyAll();
                    }
                    break;
                // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                case EVENT_GET_SMS_SIM_MEM_STATUS_DONE:
                    ar = (AsyncResult)msg.obj;
          
                    synchronized (mLock) {
                        if (ar.exception == null) {
                            mSuccess = true;

                            if (mSimMemStatus == null) {
                                mSimMemStatus = new IccSmsStorageStatus();
                            }

                            IccSmsStorageStatus tmpStatus = (IccSmsStorageStatus) ar.result;

                            mSimMemStatus.mUsed = tmpStatus.mUsed;
                            mSimMemStatus.mTotal = tmpStatus.mTotal;
                        } else {
                            if (DBG)
                                log("Cannot Get Sms SIM Memory Status from SIM");
                        }
                        mLock.notifyAll();
                    }
                    break;
                    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                case EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE:
                    ar = (AsyncResult)msg.obj;
                    synchronized (mSimInsertLock) {
                        mInsertMessageSuccess = (ar.exception == null);
                        if (mInsertMessageSuccess == true) {
                            try {
                                int index = ((int[]) ar.result)[0];
                                smsInsertRet.indexInIcc += (index + INDEXT_SPLITOR);
                                log("[insertText save one pdu in index " + index);
                            } catch (ClassCastException e) {
                                e.printStackTrace();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            log("[insertText fail to insert sms into ICC");
                            smsInsertRet.indexInIcc += ("-1" + INDEXT_SPLITOR);
                        }

                        mSimInsertLock.notifyAll();
                    }
                    break;
                case EVENT_GET_SMS_PARAMS:
                    ar = (AsyncResult)msg.obj;
                    synchronized (mSmsParamsLock) {
                        if (ar.exception == null) {
                            try {
                                mSmsParams = (SmsParameters) ar.result;
                            } catch (ClassCastException e) {
                                log("[EFsmsp fail to get sms params ClassCastException");
                                e.printStackTrace();
                            } catch (Exception ex) {
                                log("[EFsmsp fail to get sms params Exception");
                                ex.printStackTrace();
                            }
                        } else {
                            log("[EFsmsp fail to get sms params");
                            mSmsParams = null;
                        }

                        mSmsParamsLock.notifyAll();
                    }
                    break;
                case EVENT_SET_SMS_PARAMS:
                    ar = (AsyncResult)msg.obj;
                    synchronized (mSmsParamsLock) {
                        if (ar.exception == null) {
                            mSmsParamsSuccess = true;
                        } else {
                            log("[EFsmsp fail to set sms params");
                            mSmsParamsSuccess = false;
                        }

                        mSmsParamsLock.notifyAll();
                    }
                    break;
                case EVENT_LOAD_ONE_RECORD_DONE:
                    ar = (AsyncResult)msg.obj;
                    synchronized (mLock) {
                        if (ar.exception == null) {
                            try {
                                // mSmsRawData = (SmsRawData)ar.result;
                                byte[] rawData = (byte[]) ar.result;
                                if (rawData[0] == STATUS_ON_ICC_FREE) {
                                    log("sms raw data status is FREE");
                                    mSmsRawData = null;
                                } else {
                                    mSmsRawData = new SmsRawData(rawData);
                                }
                            } catch (ClassCastException e) {
                                log("fail to get sms raw data ClassCastException");
                                e.printStackTrace();
                                mSmsRawData = null;
                            }
                        } else {
                            log("fail to get sms raw data rild");
                            mSmsRawData = null;
                        }
                        mLock.notifyAll();
                    }
                    break;
            }
        }
    };

    public SimSmsInterfaceManager(GSMPhone phone, SMSDispatcher dispatcher) {
        super(phone);
        mDispatcher = dispatcher;
        
        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mediatek.dm.LAWMO_WIPE");
        mContext.registerReceiver(mSmsWipeReceiver, filter);
        // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    }

    public void dispose() {
    }

    @Override
    protected void finalize() {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            Log.e(LOG_TAG, "Error while finalizing:", throwable);
        }
        if(DBG) Log.d(LOG_TAG, "SimSmsInterfaceManager finalized");
    }

    /**
     * Update the specified message on the SIM.
     *
     * @param index record index of message to update
     * @param status new message status (STATUS_ON_ICC_READ,
     *                  STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *                  STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu the raw PDU to store
     * @return success or not
     *
     */
    public boolean
    updateMessageOnIccEf(int index, int status, byte[] pdu) {
        if (DBG) log("updateMessageOnIccEf: index=" + index +
                " status=" + status + " ==> " +
                "("+ Arrays.toString(pdu) + ")");
        enforceReceiveAndSend("Updating message on SIM");

        if (status == STATUS_ON_ICC_FREE) {
            synchronized (mDeleteLock) {
                mSuccess = false;

                Message response = mHandler.obtainMessage(EVENT_SIM_SMS_DELETE_DONE);
                mPhone.mCM.deleteSmsOnSim(index, response);

                try {
                    mDeleteLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to delete by index");
                }
            }
        } else {
            synchronized (mLock) {
                mSuccess = false;

                Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE);
                byte[] record = makeSmsRecordData(status, pdu);
                /* MTK start: icc file handler will be null while plug-out sim card */
                IccFileHandler fh = mPhone.getIccFileHandler();
                if (fh != null) {
                    mPhone.getIccFileHandler().updateEFLinearFixed(
                            IccConstants.EF_SMS,
                            index, record, null, response);

                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        log("interrupted while trying to update by index");
                    }
                }
                /* MTK end: icc file handler will be null while plug-out sim card */
            }
        }

        return mSuccess;
    }

    /**
     * Copy a raw SMS PDU to the SIM.
     *
     * @param pdu the raw PDU to store
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @return success or not
     *
     */
    public boolean copyMessageToIccEf(int status, byte[] pdu, byte[] smsc) {
        if (DBG) log("copyMessageToIccEf: status=" + status + " ==> " +
                "pdu=("+ Arrays.toString(pdu) +
                "), smsm=(" + Arrays.toString(smsc) +")");
        enforceReceiveAndSend("Copying message to SIM");
        synchronized (mLock) {
            mSuccess = false;
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE);

            mPhone.mCM.writeSmsToSim(status, IccUtils.bytesToHexString(smsc),
                    IccUtils.bytesToHexString(pdu), response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
        }
        return mSuccess;
    }

    /**
     * Retrieves all messages currently stored on ICC.
     *
     * @return list of SmsRawData of all sms on ICC
     */
    public List<SmsRawData> getAllMessagesFromIccEf() {
        if (DBG) log("getAllMessagesFromEF");

        Context context = mPhone.getContext();

        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Reading messages from SIM");
        synchronized (mSimSmsLock) {
            /* MTK start: icc file handler will be null while plug-out sim card */
            IccFileHandler fh = mPhone.getIccFileHandler();
            if (fh != null) {
                Message response = mHandler.obtainMessage(EVENT_LOAD_DONE);
                mPhone.getIccFileHandler().loadEFLinearFixedAll(IccConstants.EF_SMS, response);
    
                try {
                    mSimSmsLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to load from the SIM");
                }
            }
            /* MTK end: icc file handler will be null while plug-out sim card */
        }
        return mSms;
    }

    public boolean enableCellBroadcast(int messageIdentifier) {
        return enableCellBroadcastRange(messageIdentifier, messageIdentifier);
    }

    public boolean disableCellBroadcast(int messageIdentifier) {
        return disableCellBroadcastRange(messageIdentifier, messageIdentifier);
    }

    public boolean activateCellBroadcastSms(boolean activate)
    {
        Log.d(LOG_TAG, "activateCellBroadcastSms activate : "+activate);
        return setCellBroadcastActivation(activate);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId) {
        if (DBG) log("enableCellBroadcastRange");

        Context context = mPhone.getContext();

        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Enabling cell broadcast SMS");

        String client = context.getPackageManager().getNameForUid(
                Binder.getCallingUid());

        if (!mCellBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
            log("Failed to add cell broadcast subscription for MID range " + startMessageId
                    + " to " + endMessageId + " from client " + client);
            return false;
        }

        if (DBG)
            log("Added cell broadcast subscription for MID range " + startMessageId
                    + " to " + endMessageId + " from client " + client);

        setCellBroadcastActivation(!mCellBroadcastRangeManager.isEmpty());

        return true;
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId) {
        if (DBG) log("disableCellBroadcastRange");

        Context context = mPhone.getContext();

        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Disabling cell broadcast SMS");

        String client = context.getPackageManager().getNameForUid(
                Binder.getCallingUid());

        if (!mCellBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
            log("Failed to remove cell broadcast subscription for MID range " + startMessageId
                    + " to " + endMessageId + " from client " + client);
            return false;
        }

        if (DBG)
            log("Removed cell broadcast subscription for MID range " + startMessageId
                    + " to " + endMessageId + " from client " + client);

        setCellBroadcastActivation(!mCellBroadcastRangeManager.isEmpty());

        return true;
    }

    class CellBroadcastRangeManager extends IntRangeManager {
        private ArrayList<SmsBroadcastConfigInfo> mConfigList =
                new ArrayList<SmsBroadcastConfigInfo>();

        /**
         * Called when the list of enabled ranges has changed. This will be
         * followed by zero or more calls to {@link #addRange} followed by
         * a call to {@link #finishUpdate}.
         */
        protected void startUpdate() {
            mConfigList.clear();
        }

        /**
         * Called after {@link #startUpdate} to indicate a range of enabled
         * values.
         * @param startId the first id included in the range
         * @param endId the last id included in the range
         */
        protected void addRange(int startId, int endId, boolean selected) {
            mConfigList.add(new SmsBroadcastConfigInfo(startId, endId,
                        SMS_CB_CODE_SCHEME_MIN, SMS_CB_CODE_SCHEME_MAX, selected));
        }

        /**
         * Called to indicate the end of a range update started by the
         * previous call to {@link #startUpdate}.
         * @return true if successful, false otherwise
         */
        protected boolean finishUpdate() {
            if (mConfigList.isEmpty()) {
                return true;
            } else {
                SmsBroadcastConfigInfo[] configs =
                        mConfigList.toArray(new SmsBroadcastConfigInfo[mConfigList.size()]);
                return setCellBroadcastConfig(configs);
            }
        }
    }

    private boolean setCellBroadcastConfig(SmsBroadcastConfigInfo[] configs) {
        if (DBG)
            log("Calling setGsmBroadcastConfig with " + configs.length + " configurations");

        synchronized (mLock) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_CONFIG_DONE);

            mSuccess = false;
            mPhone.mCM.setGsmBroadcastConfig(configs, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast config");
            }
        }

        return mSuccess;
    }

    private boolean setCellBroadcastActivation(boolean activate) {
        if (DBG)
            log("Calling setCellBroadcastActivation(" + activate + ')');

        synchronized (mLock) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_ACTIVATION_DONE);

            mSuccess = false;
            mPhone.mCM.setGsmBroadcastActivation(activate, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast activation");
            }
        }
        if (!activate && mSuccess) {
            mCellBroadcastRangeManager.clearAllRanges();
        }
        return mSuccess;
    }

    @Override
    protected void log(String msg) {
        Log.d(LOG_TAG, "[SimSmsInterfaceManager] " + msg);
    }
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /**
     * Copy a text SMS to the ICC.
     *
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @return success or not
     *
     */
    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp) {
        if(DBG) {
            log("sc address: " + scAddress + " address: " + address + " message count: " + text.size() +
              " status: " + status + " timestamp: " + timestamp);
        }
        enforceReceiveAndSend("Copying message to SIM");

        IccSmsStorageStatus memStatus;

        memStatus = getSmsSimMemoryStatusInternal();

        if (memStatus == null) {
            log("Fail to get SIM memory status");
            return RESULT_ERROR_GENERIC_FAILURE;
        } else {
            if (memStatus.getUnused() < text.size()) {
                log("SIM memory is not enough");
                return RESULT_ERROR_SIM_MEM_FULL;
            }
        }

        return mDispatcher.copyTextMessageToIccCard(scAddress, address, text, status, timestamp);
    }

    /**
     * Get SMS SIM Card memory's total and used number
     *
     * @return <code>IccSmsStorageStatus</code> object
     */
    public IccSmsStorageStatus getSmsSimMemoryStatus() {
        if (DBG)
            log("getSmsSimMemoryStatus");

        enforceReceiveAndSend("Get SMS SIM Card Memory Status from SIM");
        return getSmsSimMemoryStatusInternal();
    }

    private IccSmsStorageStatus getSmsSimMemoryStatusInternal() {
        log("getSmsSimMemoryStatusInternal");

        synchronized (mLock) {
            mSuccess = false;

            Message response = mHandler.obtainMessage(EVENT_GET_SMS_SIM_MEM_STATUS_DONE);

            mPhone.mCM.getSmsSimMemoryStatus(response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get SMS SIM Card Memory Status from SIM");
            }
        }

        if (mSuccess) {
            return mSimMemStatus;
        } else {
            return null;
        }
    }

    // this receiver is used to receive WIPE intent
    private BroadcastReceiver mSmsWipeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "[xj Receive intent");
            if (intent.getAction().equals("com.mediatek.dm.LAWMO_WIPE")) {
                Log.d(LOG_TAG, "[xj Receive wipe intent");
                Thread t = new Thread() {
                    public void run() {
                        //GeminiSmsManager.deleteMessageFromIccGemini(-1, Phone.GEMINI_SIM_1);
                        Log.d(LOG_TAG, "[xj delete message on sim " + mPhone.getMySimId());
                        Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE);
                        mPhone.mCM.deleteSmsOnSim(-1, response);
                    }
                };
                t.start();
            }
        }
    };
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    
    /**
     * Insert a text SMS to the ICC.
     *
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @return SimSmsInsertStatus
     *
     */
    public SimSmsInsertStatus insertTextMessageToIccCard(String scAddress, String address,
            List<String> text, int status, long timestamp) {
        // impl
        enforceReceiveAndSend("[insertText insert message into SIM");

        int msgCount = text.size();
        boolean isDeliverPdu = true;

        log("[insertText scAddr=" + scAddress + ", addr=" + address + ", msgCount=" + msgCount
                + ", status=" + status + ", timestamp=" + timestamp);

        smsInsertRet.indexInIcc = "";

        IccSmsStorageStatus memStatus = getSmsSimMemoryStatusInternal();
        if (memStatus != null) {
            int unused = memStatus.getUnused();
            if (unused < msgCount) {
                log("[insertText SIM mem is not enough [" + unused + "/" + msgCount + "]");
                smsInsertRet.insertStatus = RESULT_ERROR_SIM_MEM_FULL;
                return smsInsertRet;
            }
        } else {
            log("[insertText fail to get SIM mem status");
            smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
            return smsInsertRet;
        }

        if (checkPhoneNumberInternal(scAddress) == false) {
            log("[insertText invalid sc address");
            scAddress = null;
        }

        if (checkPhoneNumberInternal(address) == false) {
            log("[insertText invalid address");
            smsInsertRet.insertStatus = RESULT_ERROR_INVALID_ADDRESS;
            return smsInsertRet;
        }

        if (status == STATUS_ON_ICC_READ || status == STATUS_ON_ICC_UNREAD) {
            log("[insertText to encode delivery pdu");
            isDeliverPdu = true;
        } else if (status == STATUS_ON_ICC_SENT || status == STATUS_ON_ICC_UNSENT) {
            log("[insertText to encode submit pdu");
            isDeliverPdu = false;
        } else {
            log("[insertText invalid status " + status);
            smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
            return smsInsertRet;
        }
        log("[insertText params check pass");

        int encoding = SmsConstants.ENCODING_UNKNOWN;
        TextEncodingDetails details[] = new TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; ++i) {
            details[i] = SmsMessage.calculateLength(text.get(i), false);
            if (encoding != details[i].codeUnitSize &&
                (encoding == SmsConstants.ENCODING_UNKNOWN || encoding == SmsConstants.ENCODING_7BIT)) {
                // use the USC2 if only one message is that coding style
                encoding = details[i].codeUnitSize;
            }
        }

        log("[insertText create & insert pdu start...");
        for (int i = 0; i < msgCount; ++i) {
            if (mInsertMessageSuccess == false && i > 0) {
                log("[insertText last message insert fail");
                smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
                return smsInsertRet;
            }

            int singleShiftId = -1;
            int lockingShiftId = -1;
            int language = details[i].shiftLangId;
            int encoding_detail = encoding;

            if (encoding == SmsConstants.ENCODING_7BIT) {
                if (details[i].languageTable > 0 && details[i].languageShiftTable > 0) {
                    singleShiftId = details[i].languageTable;
                    lockingShiftId = details[i].languageShiftTable;
                    encoding_detail = SmsMessage.ENCODING_7BIT_LOCKING_SINGLE;
                } else if (details[i].languageShiftTable > 0) {
                    lockingShiftId = details[i].languageShiftTable;
                    encoding_detail = SmsMessage.ENCODING_7BIT_LOCKING;
                } else if (details[i].languageTable > 0) {
                    singleShiftId = details[i].languageTable;
                    encoding_detail = SmsMessage.ENCODING_7BIT_SINGLE;
                }
            }

            byte[] smsHeader = null;
            if (msgCount > 1) {
                log("[insertText create pdu header for concat-message");
                smsHeader = SmsHeader.getSubmitPduHeaderWithLang(-1, (getNextConcatRef() & 0xff),
                        (i + 1), msgCount, singleShiftId, lockingShiftId);
            }

            if (isDeliverPdu) {
                SmsMessage.DeliverPdu pdu = SmsMessage.getDeliverPduWithLang(scAddress, address,
                    text.get(i), smsHeader, timestamp, encoding_detail, language);
                if(pdu != null) {
                    mPhone.mCM.writeSmsToSim(status, IccUtils.bytesToHexString(pdu.encodedScAddress),
                        IccUtils.bytesToHexString(pdu.encodedMessage), mHandler.obtainMessage(EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE));
                } else {
                    log("[insertText fail to create deliver pdu");
                    smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
                    return smsInsertRet;
                }
            } else {
                SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPduWithLang(scAddress, address,
                        text.get(i), false, smsHeader, encoding_detail, language);
                if (pdu != null) {
                    mPhone.mCM.writeSmsToSim(status, IccUtils.bytesToHexString(pdu.encodedScAddress),
                        IccUtils.bytesToHexString(pdu.encodedMessage), mHandler.obtainMessage(EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE));
                } else {
                    log("[insertText fail to create submit pdu");
                    smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
                    return smsInsertRet;
                }
            }

            synchronized (mSimInsertLock) {
                try {
                    log("[insertText wait until the pdu be wrote into the SIM");
                    mSimInsertLock.wait();
                } catch (InterruptedException e) {
                    log("[insertText fail to insert pdu");
                    smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
                    return smsInsertRet;
                }
            }
        } // end loop for pdu creation & insertion
        log("[insertText create & insert pdu end");

        if (mInsertMessageSuccess == true) {
            log("[insertText all messages inserted");
            smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
            return smsInsertRet;
        }

        log("[insertText pdu insert fail");
        smsInsertRet.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
        return smsInsertRet;
    }

    private static int getNextConcatRef() {
        return sConcatenatedRef++;
    }

    private static boolean checkPhoneNumberCharacter(char c) {
        return (c >= '0' && c <= '9') || (c == '*') || (c == '+')
                || (c == '#') || (c == 'N') || (c == ' ') || (c == '-');
    }

    private static boolean checkPhoneNumberInternal(String number) {
        if (number == null) {
            return true;
        }

        for (int i = 0, n = number.length(); i < n; ++i) {
            if (checkPhoneNumberCharacter(number.charAt(i))) {
                continue;
            } else {
                return false;
            }
        }

        return true;
    }

    public SimSmsInsertStatus insertRawMessageToIccCard(int status, byte[] pdu, byte[] smsc) {
        // impl
        enforceReceiveAndSend("[insertRaw insert message into SIM");
        synchronized (mLock) {
            mSuccess = false;
            smsInsertRet2.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
            smsInsertRet2.indexInIcc = "";
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE);

            mPhone.mCM.writeSmsToSim(status, IccUtils.bytesToHexString(smsc),
                    IccUtils.bytesToHexString(pdu), response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("[insertRaw interrupted while trying to update by index");
            }
        }

        if (mSuccess == true) {
            log("[insertRaw message inserted");
            smsInsertRet2.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
            return smsInsertRet2;
        }

        log("[insertRaw pdu insert fail");
        smsInsertRet2.insertStatus = RESULT_ERROR_GENERIC_FAILURE;
        return smsInsertRet2;
    }

    /*
     * Get sms parameters from EFsmsp, such as the validity period & its format,
     * protocol identifier and decode char set value
     */
    public SmsParameters getSmsParameters() {
        // impl
        synchronized (mSmsParamsLock) {
            Message response = mHandler.obtainMessage(EVENT_GET_SMS_PARAMS);
            mPhone.mCM.getSmsParameters(response);

            try {
                mSmsParamsLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get sms params");
            }
        }

        return mSmsParams;
    }

    /*
     * Save sms parameters into EFsmsp
     */
    public boolean setSmsParameters(SmsParameters params) {
        // impl
        mSmsParamsSuccess = false;
        synchronized (mSmsParamsLock) {
            Message response = mHandler.obtainMessage(EVENT_SET_SMS_PARAMS);
            mPhone.mCM.setSmsParameters(params, response);

            try {
                mSmsParamsLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get sms params");
            }
        }
        return mSmsParamsSuccess;
    }

    /**
     * Retrieves message currently stored on ICC by index.
     * 
     * @return SmsRawData of sms on ICC
     */
    public SmsRawData getMessageFromIccEf(int index) {
        log("getMessagesFromIccEf");

        Context context = mPhone.getContext();
        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Reading messages from SIM");

        return getMessageFromIccEfInternal(index);
    }

    private SmsRawData getMessageFromIccEfInternal(int index) {
        log("getMessagesFromIccEfInternal");
        mSmsRawData = null;
        synchronized (mLock) {
            /* MTK start: icc file handler will be null while plug-out sim card */
            IccFileHandler fh = mPhone.getIccFileHandler();
            if (fh != null) {
                Message response = mHandler.obtainMessage(EVENT_LOAD_ONE_RECORD_DONE);
                mPhone.getIccFileHandler().loadEFLinearFixed(IccConstants.EF_SMS, index, response);
    
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to load from the SIM");
                }
            }
            /* MTK end: icc file handler will be null while plug-out sim card */
        }

        return mSmsRawData;
    }
    
    public boolean setEtwsConfig(int mode) {
        if (DBG)
            log("Calling setEtwsConfig(" + mode + ')');

        synchronized (mLock) {
            Message response = mHandler.obtainMessage(EVENT_SET_ETWS_CONFIG_DONE);

            mSuccess = false;
            mPhone.mCM.setEtws(mode, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set ETWS config");
            }
        }

        return mSuccess;
    }
}
