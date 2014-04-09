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

import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.util.RateController;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.internal.telephony.TelephonyIntents;

import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// M:
import android.content.ContentValues;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.util.DownloadManager;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
/// M: Pass status code to handle server error @{
import com.android.mms.MmsPluginManager;
import com.mediatek.mms.ext.IMmsTransaction;
/// @}


/**
 * The TransactionService of the MMS Client is responsible for handling requests
 * to initiate client-transactions sent from:
 * <ul>
 * <li>The Proxy-Relay (Through Push messages)</li>
 * <li>The composer/viewer activities of the MMS Client (Through intents)</li>
 * </ul>
 * The TransactionService runs locally in the same process as the application.
 * It contains a HandlerThread to which messages are posted from the
 * intent-receivers of this application.
 * <p/>
 * <b>IMPORTANT</b>: This is currently the only instance in the system in
 * which simultaneous connectivity to both the mobile data network and
 * a Wi-Fi network is allowed. This makes the code for handling network
 * connectivity somewhat different than it is in other applications. In
 * particular, we want to be able to send or receive MMS messages when
 * a Wi-Fi connection is active (which implies that there is no connection
 * to the mobile data network). This has two main consequences:
 * <ul>
 * <li>Testing for current network connectivity ({@link android.net.NetworkInfo#isConnected()} is
 * not sufficient. Instead, the correct test is for network availability
 * ({@link android.net.NetworkInfo#isAvailable()}).</li>
 * <li>If the mobile data network is not in the connected state, but it is available,
 * we must initiate setup of the mobile data connection, and defer handling
 * the MMS transaction until the connection is established.</li>
 * </ul>
 */
public class TransactionService extends Service implements Observer {
    private static final String TAG = "TransactionService";

    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is completed.
     */
    public static final String TRANSACTION_COMPLETED_ACTION =
            "android.intent.action.TRANSACTION_COMPLETED_ACTION";

    /**
     * Action for the Intent which is sent by Alarm service to launch
     * TransactionService.
     */
    public static final String ACTION_ONALARM = "android.intent.action.ACTION_ONALARM";

    /**
     * Used as extra key in notification intents broadcasted by the TransactionService
     * when a Transaction is completed (TRANSACTION_COMPLETED_ACTION intents).
     * Allowed values for this key are: TransactionState.INITIALIZED,
     * TransactionState.SUCCESS, TransactionState.FAILED.
     */
    public static final String STATE = "state";

    /**
     * Used as extra key in notification intents broadcasted by the TransactionService
     * when a Transaction is completed (TRANSACTION_COMPLETED_ACTION intents).
     * Allowed values for this key are any valid content uri.
     */
    public static final String STATE_URI = "uri";

    private static final int EVENT_TRANSACTION_REQUEST = 1;
    private static final int EVENT_CONTINUE_MMS_CONNECTIVITY = 3;
    private static final int EVENT_HANDLE_NEXT_PENDING_TRANSACTION = 4;
    private static final int EVENT_NEW_INTENT = 5;
    private static final int EVENT_QUIT = 100;

    private static final int TOAST_MSG_QUEUED = 1;
    private static final int TOAST_DOWNLOAD_LATER = 2;
    private static final int TOAST_NONE = -1;

    // How often to extend the use of the MMS APN while a transaction
    // is still being processed.
    /// M:Code analyze 001,for new feature,extending delay time of sending message,change time to 4 minutes @{
    private static final int APN_EXTENSION_WAIT = 8 * 30 * 1000;
    /// @}
    /// M: for fix bug ALPS00474890, wait for 10 seconds to scan pending mms.
    private static final int SCAN_PENDING_MMS_WAIT = 10 * 1000;

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private final ArrayList<Transaction> mProcessing  = new ArrayList<Transaction>();
    private final ArrayList<Transaction> mPending  = new ArrayList<Transaction>();
    private ConnectivityManager mConnMgr;
    private ConnectivityBroadcastReceiver mReceiver;

    private PowerManager.WakeLock mWakeLock;
    /// M: ALPS00442702, before start pdp need wait @{
    private static boolean mPdpWait = false;
    private final Object mPdpLock = new Object();
    /// @}

    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = null;

            if (msg.what == TOAST_MSG_QUEUED) {
                str = getString(R.string.message_queued);
            } else if (msg.what == TOAST_DOWNLOAD_LATER) {
                str = getString(R.string.download_later);
            }

            if (str != null) {
                Toast.makeText(TransactionService.this, str,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Creating TransactionService");
        }

        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "Creating Transaction Service");
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("TransactionService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mReceiver = new ConnectivityBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        /// M:Code analyze 014,add for bug,send a broadcast uing apn network connection without any delay @{
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION_IMMEDIATE);
        /// @}
        /// M:Code analyze 013,add for fix bug,for handling framework sticky intent issue @{
        mFWStickyIntent = registerReceiver(mReceiver, intentFilter);
        /// @}
        /// M:Code analyze 004,add for ALPS00081452 @{
        registerPhoneCallListener();
        /// @}

        /// M: ALPS00440523, set service to foreground @ {
        IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
        mmsTransactionPlugin.startServiceForeground(this);
        /// @}
        MmsLog.d(MmsApp.TXN_TAG, "Sticky Intent would be received:" + (mFWStickyIntent!=null?"true":"false"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
            msg.arg1 = startId;
            msg.obj = intent;
            mServiceHandler.sendMessage(msg);
            /// M:
            MmsLog.d(MmsApp.TXN_TAG, "onStartCommand");
        }
        return Service.START_NOT_STICKY;
    }

    public void onNewIntent(Intent intent, int serviceId) {
        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean noNetwork = !isNetworkAvailable();

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "onNewIntent: serviceId: " + serviceId + ": " + intent.getExtras() +
                    " intent=" + intent);
            Log.v(TAG, "    networkAvailable=" + !noNetwork);
        }

        /// M:Code analyze 008,unknown
        Uri uri = null;
        String str = intent.getStringExtra(TransactionBundle.URI);
        if (null != str) {
            MmsLog.d(MmsApp.TXN_TAG, "URI in Bundle.");
            uri = Uri.parse(str);
            if (null != uri) {
                mTriggerMsgId = ContentUris.parseId(uri);
                MmsLog.d(MmsApp.TXN_TAG, "Trigger Message ID = " + mTriggerMsgId);
            }
        }
        /// @}

        /// M:Code analyze 011,add for fix bug, the serviceId[startId] of each transaction got from framewrok is increasing by order.
        /// so the max service id is recorded. and must invoked as the last transaction finish.@{
        mMaxServiceId = (serviceId > mMaxServiceId)?serviceId:mMaxServiceId;
        /// @}

        /// M: Code analyze 015,add for new feature,change the logic for gemini
        if (ACTION_ONALARM.equals(intent.getAction()) || (intent.getExtras() == null)) {
            if (ACTION_ONALARM.equals(intent.getAction())) {
                MmsLog.d(MmsApp.TXN_TAG, "onNewIntent: ACTION_ONALARM");
            } else {
                MmsLog.d(MmsApp.TXN_TAG, "onNewIntent: Intent has no Extras data.");
            }
            scanPendingMessages(serviceId, noNetwork, -1, false);
        //add this case for read report.
        } else if (Transaction.READREC_TRANSACTION == intent.getIntExtra("type",-1)) {
            //specific process for read report.
            TransactionBundle args = null;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                args = new TransactionBundle(intent.getIntExtra("type",3),
                                             intent.getStringExtra("uri"));
                launchTransactionGemini(serviceId, intent.getIntExtra("simId",-1), args);
            } else {
                args = new TransactionBundle(intent.getExtras());
                launchTransaction(serviceId, args, noNetwork);
            }
            if (args != null) {
                MmsLog.d(MmsApp.TXN_TAG, "transaction type:"+args.getTransactionType()+",uri:"+args.getUri());
            }
        } else {
            // For launching NotificationTransaction and test purpose.
            TransactionBundle args = null;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                args = new TransactionBundle(intent.getIntExtra(TransactionBundle.TRANSACTION_TYPE, 0), 
                                             intent.getStringExtra(TransactionBundle.URI));
                // 1. for gemini, do not cear noNetwork param
                // 2. check URI
                if (null != intent.getStringExtra(TransactionBundle.URI)) {
                    int simId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
                    if (-1 != simId) {
                        launchTransactionGemini(serviceId, simId, args);
                    } else {
                        // for handling third party 
                        long connectSimId = EncapsulatedSettings.System.getLong(getContentResolver(),
                                EncapsulatedSettings.System.GPRS_CONNECTION_SIM_SETTING,
                                EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET); 
                        MmsLog.d(MmsApp.TXN_TAG, "onNewIntent. before launch transaction:  current data settings: "
                                + connectSimId);
                        if (EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET != connectSimId
                                && EncapsulatedSettings.System.GPRS_CONNECTION_SIM_SETTING_NEVER != connectSimId) {
                            launchTransactionGemini(serviceId, (int)connectSimId, args);
                        }
                    }
                }
            }else {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "onNewIntent: launch transaction...");
                }
                // For launching NotificationTransaction and test purpose.
                args = new TransactionBundle(intent.getExtras());
                launchTransaction(serviceId, args, noNetwork);
            }
        }
        /// @}
        return;
    }

    /// M:Code analyze 018,add for new feature,stop service @{
    private void stopSelfIfIdle(int startId) {
        /// M:Code analyze 004,add for ALPS00081452,TransactionService need keep alive to wait call end
        /// and process pending mms in db.@{
        if (mEnableCallbackIdle) {
            MmsLog.d(MmsApp.TXN_TAG, "need wait call end, no stop.");
            return;
        }
        /// @}
        synchronized (mProcessing) {
            /// M:Code analyze 007,mNeedWait is using avoid stop TransactionService incorrectly.
            if (mProcessing.isEmpty() && mPending.isEmpty() && mNeedWait == false) {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "stopSelfIfIdle: STOP!");
                }
                // Make sure we're no longer listening for connection state changes.
                /// M:Code analyze 016,add for fix bug,comment lines below,make we can still listening for connection state changes @{ */
                //if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                //    Log.v(TAG, "stopSelfIfIdle: unRegisterForConnectionStateChanges");
                //}
                //MmsSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());
                /// @}
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "stop TransactionService.");
                stopSelf(startId);
            }
        }
    }
    /// @}

    private static boolean isTransientFailure(int type) {
        return (type < MmsSms.ERR_TYPE_GENERIC_PERMANENT) && (type > MmsSms.NO_ERROR);
    }

    private boolean isNetworkAvailable() {
        NetworkInfo ni = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
        return (ni == null ? false : ni.isAvailable());
    }

    private int getTransactionType(int msgType) {
        switch (msgType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                return Transaction.RETRIEVE_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
                return Transaction.READREC_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                return Transaction.SEND_TRANSACTION;
            default:
                Log.w(TAG, "Unrecognized MESSAGE_TYPE: " + msgType);
                return -1;
        }
    }

    private void launchTransaction(int serviceId, TransactionBundle txnBundle, boolean noNetwork) {
        /// M: Code analyze 017,add for new feature,if transaction type is notification, we must let it check network 
        /// later(in it's run method).in NotificationTransaction's run method, will check auto download setting and try 
        /// to retrieve.if returned here, the notification can not be scanned, because it's error status is no error. @{ */
        if (noNetwork && (txnBundle.getTransactionType() != Transaction.NOTIFICATION_TRANSACTION)) {
            Log.w(TAG, "launchTransaction: no network error!");
            MmsSystemEventReceiver.registerForConnectionStateChanges(getApplicationContext());
            onNetworkUnavailable(serviceId, txnBundle.getTransactionType());
            return;
        }
        /// @}
        Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
        msg.arg1 = serviceId;
        msg.obj = txnBundle;

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "launchTransaction: sending message " + msg);
        }
        mServiceHandler.sendMessage(msg);
    }

    private void onNetworkUnavailable(int serviceId, int transactionType) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "onNetworkUnavailable: sid=" + serviceId + ", type=" + transactionType);
        }

        int toastType = TOAST_NONE;
        if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
            toastType = TOAST_DOWNLOAD_LATER;
        } else if (transactionType == Transaction.SEND_TRANSACTION) {
            toastType = TOAST_MSG_QUEUED;
        }
        if (toastType != TOAST_NONE) {
            mToastHandler.sendEmptyMessage(toastType);
        }
        /// M:Code analyze 019,add for new feature,stop service with a new different function
        stopSelfIfIdle(serviceId);
        /// @}
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Destroying TransactionService");
        }
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "Destroying Transaction Service");
        if (!mPending.isEmpty()) {
            Log.w(TAG, "TransactionService exiting with transaction still pending");
        }

        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            /// M:Code analyze 006,add for new feature,the variable is using to judge the current transaction can be added
            /// the mPending queue or not,only when the apn network state is under APN_REQUEST_STARTED,then it is right
            /// time to add the transaction into mPending queue in the case below,because current service is destroyed,
            /// variable should be set back to false @{
            mIsWaitingConxn = false;
            /// @}
            /// M:Code analyze 004,add for ALPS00081452,get telephony service and unregister phone call listener@{
            EncapsulatedTelephonyManagerEx teleManager = new EncapsulatedTelephonyManagerEx(this);
            teleManager.listen(mPhoneStateListener,
                            PhoneStateListener.LISTEN_NONE, EncapsulatedPhone.GEMINI_SIM_1);
            mPhoneStateListener = null;
            teleManager.listen(mPhoneStateListener2,
                            PhoneStateListener.LISTEN_NONE, EncapsulatedPhone.GEMINI_SIM_2);
            mPhoneStateListener2 = null;
            teleManager.listen(mPhoneStateListener3,
                            PhoneStateListener.LISTEN_NONE, EncapsulatedPhone.GEMINI_SIM_3);
            mPhoneStateListener3 = null;
            teleManager.listen(mPhoneStateListener4,
                            PhoneStateListener.LISTEN_NONE, EncapsulatedPhone.GEMINI_SIM_4);
            mPhoneStateListener4 = null;
        } else {
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }
        /// @}
        releaseWakeLock();

        unregisterReceiver(mReceiver);

        /// M: ALPS00440523, set service to foreground @ {
        IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
        mmsTransactionPlugin.stopServiceForeground(this);
        /// @}
        mServiceHandler.sendEmptyMessage(EVENT_QUIT);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handle status change of Transaction (The Observable).
     */
    public void update(Observable observable) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "Transaction Service update");
        Transaction transaction = (Transaction) observable;
        int serviceId = transaction.getServiceId();
        /// M:Code analyze 008,unkonw
        mTriggerMsgId = 0;
        /// @}
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "update transaction " + serviceId);
        }

        try {
            synchronized (mProcessing) {
                mProcessing.remove(transaction);
                if (mPending.size() > 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "update: handle next pending transaction...");
                    }
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "TransactionService: update: mPending.size()=" + mPending.size());
                    Message msg = mServiceHandler.obtainMessage(
                            EVENT_HANDLE_NEXT_PENDING_TRANSACTION,
                            transaction.getConnectionSettings());
                    /// M:Code analyze 020, add for new feature,set simID info for gemini @{ */
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        msg.arg2 = transaction.mSimId;
                    }
                    /// @}
                    mServiceHandler.sendMessage(msg);
                }
                /// M: Code analyze 021,add for new feature,change else to else if,when mProcessing queue is empty,
                /// means there is no any transaction is processing,end connectivity by different branch
                else if (0 == mProcessing.size()) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "update: endMmsConnectivity");
                    }

                    /** M: modify for gemini @{ */
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        endMmsConnectivityGemini(transaction.mSimId);
                        MmsLog.d(MmsApp.TXN_TAG, "update endMmsConnectivityGemini Param = " + transaction.mSimId);
                    } else {
                        MmsLog.d(MmsApp.TXN_TAG, "update call endMmsConnectivity");
                        endMmsConnectivity();
                    }
                    /// @}
                }
            }

            Intent intent = new Intent(TRANSACTION_COMPLETED_ACTION);
            TransactionState state = transaction.getState();
            int result = state.getState();
            intent.putExtra(STATE, result);

            switch (result) {
                case TransactionState.SUCCESS:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "update: result=SUCCESS");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction complete: " + serviceId);
                    }

                    intent.putExtra(STATE_URI, state.getContentUri());

                    // Notify user in the system-wide notification area.
                    switch (transaction.getType()) {
                        case Transaction.NOTIFICATION_TRANSACTION:
                        case Transaction.RETRIEVE_TRANSACTION:
                            // We're already in a non-UI thread called from
                            // NotificationTransacation.run(), so ok to block here.
                            /** M: notify previous here, comment two lines @{ */
                            /*
                            long threadId = MessagingNotification.getThreadId(
                                    this, state.getContentUri());
                            MessagingNotification.blockingUpdateNewMessageIndicator(this,
                                    threadId,
                                    false);
                            MessagingNotification.updateDownloadFailedNotification(this);
                            */
                            /** @} */
                            break;
                        case Transaction.SEND_TRANSACTION:
                            RateController.getInstance().update();
                            break;
                    }
                    break;
                case TransactionState.FAILED:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "update: result=FAILED");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction failed: " + serviceId);
                    }
                    break;
                default:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "update: result=default");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction state unknown: " +
                                serviceId + " " + result);
                    }
                    break;
            }

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "update: broadcast transaction result " + result);
            }
            // Broadcast the result of the transaction.
            sendBroadcast(intent);
        } finally {
            transaction.detach(this);
            MmsSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());
            /// M:Code analyze 011,add for fix bug,. some transaction may be not processed.
            /// here there is a precondition: the serviceId[startId] of each transaction got from framewrok is increasing
            /// by order.so the max service id is recorded. and must invoked as the last transaction finish. @{ */
            if (transaction.getServiceId() == mMaxServiceId) {
                stopSelfIfIdle(mMaxServiceId);
            }
            /// @}
        }
    }

    private synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
            mWakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    protected int beginMmsConnectivity() throws IOException {
        // Take a wake lock so we don't fall asleep before the message is downloaded.
        createWakeLock();

        /// M: ALPS00442702, before start pdp need wait @{
        synchronized (mPdpLock) {
            MmsLog.d(MmsApp.TXN_TAG, "beginMmsConnectivity check mPdpWait=" + mPdpWait);
            if (mPdpWait) {
                try {
                    MmsLog.d(MmsApp.TXN_TAG, "start wait");
                    mPdpLock.wait(1000);
                } catch (InterruptedException ex) {
                    MmsLog.e(MmsApp.TXN_TAG, "wait has been intrrupted", ex);
                } finally {
                    MmsLog.d(MmsApp.TXN_TAG, "after wait");
                    mPdpWait = false;
                }
            }
        }
        /// @}
        int result = mConnMgr.startUsingNetworkFeature(
                ConnectivityManager.TYPE_MOBILE, EncapsulatedPhone.FEATURE_ENABLE_MMS);
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "startUsingNetworkFeature: result=" + result);

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "beginMmsConnectivity: result=" + result);
        }

        switch (result) {
            case EncapsulatedPhone.APN_ALREADY_ACTIVE:
            case EncapsulatedPhone.APN_REQUEST_STARTED:
                acquireWakeLock();
                sendBroadcast(new Intent(TRANSACTION_START));
                /// M:Code analyze 003,set a type of EVENT_PENDING_TIME_OUT timer,time out is 3 minutes,that means
                /// this connection will last for 3 minutes if the network is created successfully
                setDataConnectionTimer(result);
                /// @}
                return result;
            /// M:Code analyze 022, add two cases @{ */
            case EncapsulatedPhone.APN_TYPE_NOT_AVAILABLE:
            case EncapsulatedPhone.APN_REQUEST_FAILED:
                return result;
            /// @}
        }

        throw new IOException("Cannot establish MMS connectivity");
    }

    protected void endMmsConnectivity() {
        try {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "endMmsConnectivity");
            }

            // cancel timer for renewal of lease
            mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
            if (mConnMgr != null) {
                mConnMgr.stopUsingNetworkFeature(
                        ConnectivityManager.TYPE_MOBILE,
                        EncapsulatedPhone.FEATURE_ENABLE_MMS);
                sendBroadcast(new Intent(TRANSACTION_STOP));
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "stopUsingNetworkFeature");
                /// M: Pass status code to handle server error @{
                IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
                /// M: ALPS00442702, before start pdp need wait @{
                if (mmsTransactionPlugin.isSyncStartPdpEnabled()) {
                    synchronized(mPdpLock) {
                        MmsLog.d(MmsApp.TXN_TAG, "stopUsingNetworkFeature set mPdpWait");
                        mPdpWait = true;
                    }
                }
                /// @}
                mmsTransactionPlugin.updateConnection();
                /// @}
            }
        } finally {
            releaseWakeLock();
            /// M:Code analyze 008,unkonw
            mTriggerMsgId = 0;
            /// @}
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        private String decodeMessage(Message msg) {
            if (msg.what == EVENT_QUIT) {
                return "EVENT_QUIT";
            } else if (msg.what == EVENT_CONTINUE_MMS_CONNECTIVITY) {
                return "EVENT_CONTINUE_MMS_CONNECTIVITY";
            } else if (msg.what == EVENT_TRANSACTION_REQUEST) {
                return "EVENT_TRANSACTION_REQUEST";
            } else if (msg.what == EVENT_HANDLE_NEXT_PENDING_TRANSACTION) {
                return "EVENT_HANDLE_NEXT_PENDING_TRANSACTION";
            } else if (msg.what == EVENT_NEW_INTENT) {
                return "EVENT_NEW_INTENT";
            }
            return "unknown message.what";
        }

        private String decodeTransactionType(int transactionType) {
            if (transactionType == Transaction.NOTIFICATION_TRANSACTION) {
                return "NOTIFICATION_TRANSACTION";
            } else if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
                return "RETRIEVE_TRANSACTION";
            } else if (transactionType == Transaction.SEND_TRANSACTION) {
                return "SEND_TRANSACTION";
            } else if (transactionType == Transaction.READREC_TRANSACTION) {
                return "READREC_TRANSACTION";
            }
            return "invalid transaction type";
        }

        /**
         * Handle incoming transaction requests.
         * The incoming requests are initiated by the MMSC Server or by the
         * MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "Handling incoming message: " + msg + " = " + decodeMessage(msg));
            }
            /// M:
            MmsLog.d(MmsApp.TXN_TAG, "handleMessage :" + msg);

            Transaction transaction = null;

            switch (msg.what) {
                case EVENT_NEW_INTENT:
                    onNewIntent((Intent)msg.obj, msg.arg1);
                    break;

                case EVENT_QUIT:
                    getLooper().quit();
                    return;

                case EVENT_CONTINUE_MMS_CONNECTIVITY:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_CONTINUE_MMS_CONNECTIVITY");
                    synchronized (mProcessing) {
                        if (mProcessing.isEmpty()) {
                            return;
                        }
                    }

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "handle EVENT_CONTINUE_MMS_CONNECTIVITY event...");
                    }

                    try {
                        /// M:Code analyze 023, add for gemini @{ */
                        int result = 0;
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), msg.arg2);
                            if (null == si) {
                                MmsLog.e(MmsApp.TXN_TAG, "TransactionService:SIMInfo is null for slot " + msg.arg2);
                                return;
                            }
                            int simId = (int)si.getSimId();
                            result = beginMmsConnectivityGemini(simId/*msg.arg2*/);
                        } else{
                            result = beginMmsConnectivity();
                        }
                        /// @}

                        if (result != EncapsulatedPhone.APN_ALREADY_ACTIVE) {
                            /// M:Code analyze 003,add for time out mechanism,currently,the APN state is APN_REQUEST_STARTED,
                            /// that means the timer is for last failed apn connection,so need to be removed @{
                            if (result == EncapsulatedPhone.APN_REQUEST_STARTED && mServiceHandler.hasMessages(EVENT_PENDING_TIME_OUT)) {
                                //the timer is not for this case, remove it.
                                mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                                MmsLog.d(MmsApp.TXN_TAG, "remove an invalid timer.");
                            }
                            /// @}
                            // Just wait for connectivity startup without
                            // any new request of APN switch.
                            return;
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Attempt to extend use of MMS connectivity failed");
                        return;
                    }

                    // Restart timer
                    /// M: Code analyze 024,add for new feature,modify this function for gemini,argument change @{
                    renewMmsConnectivity(0, msg.arg2);
                    /// @}
                    return;

                /// M:Code analyze 002,add for new feature,check Apn network is available or not before sending mms @{
                case EVENT_DATA_STATE_CHANGED:
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_DATA_STATE_CHANGED! slot=" + msg.arg2);

                    /// M:Code analyze 010,add for new feature,this member is used to ignore status message sent by framework
                    /// between time out happened and a new data connection request which need wait.@{
                    if (mIgnoreMsg == true) {
                        MmsLog.d(MmsApp.TXN_TAG, "between time out over and a new connection request, ignore msg.");
                        return;
                    }
                    /// @}

                    /// M:Code analyze 003,add for time out mechanism,check the connection is ok or not,judging if 
                    /// there is timer,if yes,but pending queue is null,remove this timer,or gemini normal get this message,
                    /// still remove this timer @{
                    if (mServiceHandler.hasMessages(EVENT_PENDING_TIME_OUT)) {
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            Transaction trxn = null;
                            synchronized (mProcessing) {
                                if (mPending.size() != 0) {
                                    trxn = mPending.get(0);
                                } else {
                                    MmsLog.d(MmsApp.TXN_TAG, "a timer is created but pending is null!");
                                }
                            }
                            /*state change but pending is null, this may happened.
                                this is just for framework abnormal case*/
                            if (trxn == null) {
                                MmsLog.d(MmsApp.TXN_TAG,
                                        "remove a timer which may be created by EVENT_CONTINUE_MMS_CONNECTIVITY");
                                mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                                //return;//since the pending is null, we can return. if not, it should ok too.

                            } else {
                                int slotId = SIMInfo.getSlotById(getApplicationContext(), trxn.mSimId);
                                if (slotId == msg.arg2) {
                                    mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                                    MmsLog.d(MmsApp.TXN_TAG, "gemini normal get msg, remove timer.");
                                }
                            }
                        } else {
                            mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                            MmsLog.d(MmsApp.TXN_TAG, "normal get msg, remove timer.");
                        }
                    }
                    /// @}

                    if (mConnMgr == null) {
                        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    }
                    if (mConnMgr == null) {
                        MmsLog.d(MmsApp.TXN_TAG, "mConnMgr == null ");
                        return;
                    }
                    NetworkInfo info = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                    if (info == null) {
                        MmsLog.d(MmsApp.TXN_TAG, "NetworkInfo == null.");
                        return;
                    }
                    int slotOfInfo = info.getSimId();

                    MmsLog.d(MmsApp.TXN_TAG, "Newwork info,reason: " + info.getReason() 
                                            + ",state:" + info.getState()
                                            + ",extra info:" + info.getExtraInfo()
                                            + ",slot:" + slotOfInfo);
                    //add for sync
                    int pendingSize = getPendingSize();

                    boolean isNotificationTransaction = false;
                    boolean autoDownload = false;
                    if (pendingSize != 0) {
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            autoDownload = DownloadManager.getInstance().isAuto(mPending.get(0).mSimId);
                        } else {
                            autoDownload = DownloadManager.getInstance().isAuto();
                        }
                        isNotificationTransaction = mPending.get(0) instanceof NotificationTransaction;
                    }

                    // Check connection state : connect or disconnect
                    if (!info.isConnected() && !(isNotificationTransaction && !autoDownload)) {
                        // add for gemini
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT 
                                && (ConnectivityManager.TYPE_MOBILE == info.getType()
                                    ||ConnectivityManager.TYPE_MOBILE_MMS == info.getType())) {
                            if (pendingSize != 0) {
                                //change for sync
                                Transaction trxn = null;
                                synchronized (mProcessing) {
                                    trxn = mPending.get(0);
                                }
                                int slotId = SIMInfo.getSlotById(getApplicationContext(), trxn.mSimId);
                                if (slotId != slotOfInfo) {
                                    return;
                                }
                            }
                        }

                        // check type and reason, 
                        if (ConnectivityManager.TYPE_MOBILE_MMS == info.getType() 
                            && EncapsulatedPhone.REASON_NO_SUCH_PDP.equals(info.getReason())) {
                            if (0 != pendingSize){
                                //add for sync
                                /// M: For CMCC FT here allow retry
                                if (MmsConfig.isAllowRetryForPermanentFail()) {
                                    setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                                } else {
                                    setTransactionFail(removePending(0), FAILE_TYPE_PERMANENT);
                                }
                                /// @}
                                return;
                            }
                        } else if (ConnectivityManager.TYPE_MOBILE_MMS == info.getType()
                            && NetworkInfo.State.DISCONNECTED == info.getState()) {
                            if (0 != pendingSize){
                                MmsLog.d(MmsApp.TXN_TAG,
                                        "setTransactionFail TEMPORARY because NetworkInfo.State.DISCONNECTED");
                                //add for sync
                                setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                                return;
                            }
                        } else if ((ConnectivityManager.TYPE_MOBILE_MMS == info.getType() 
                                && EncapsulatedPhone.REASON_APN_FAILED.equals(info.getReason())) 
                                || EncapsulatedPhone.REASON_RADIO_TURNED_OFF.equals(info.getReason())) {
                            if (0 != pendingSize){
                                //add for sync
                                setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                                return;
                            }
                            MmsLog.d(MmsApp.TXN_TAG, "No pending message.");
                        }
                        return;
                    } else {
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && pendingSize != 0) {
                            Transaction trxn = null;
                            synchronized (mProcessing) {
                                trxn = mPending.get(0);
                            }
                            int slotId = SIMInfo.getSlotById(getApplicationContext(), trxn.mSimId);
                            if (slotId != slotOfInfo) {
                                MmsLog.d(MmsApp.TXN_TAG, "the connected slot not the one needed.");
                                return;
                            }
                        }
                    }

                    if (EncapsulatedPhone.REASON_VOICE_CALL_ENDED.equals(info.getReason())){
                        if (0 != pendingSize){
                            Transaction trxn = null;
                            synchronized (mProcessing) {
                                trxn = mPending.get(0);
                            }
                            // add for gemini
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                processPendingTransactionGemini(transaction,trxn.getConnectionSettings(),trxn.mSimId);
                            } else {
                                processPendingTransaction(transaction, trxn.getConnectionSettings());
                            }
                        }
                    }

                    TransactionSettings settings = null;
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        int simId = 0;
                        List<SIMInfo> mListSimInfo;
                        mListSimInfo = SIMInfo.getInsertedSIMList(getApplicationContext());
                        if (mListSimInfo == null || mListSimInfo.size() == 0) {
                            return;
                        }

                        for (simId = 0; simId < mListSimInfo.size(); simId++) {
                            if (mListSimInfo.get(simId).getSlot() == slotOfInfo) {
                                settings = new TransactionSettings(TransactionService.this, info
                                        .getExtraInfo(), slotOfInfo);
                                break;
                            }
                        }
                        if (settings == null) {
                            return;
                        }
                    } else {
                        settings = new TransactionSettings(TransactionService.this, info.getExtraInfo());
                    }

                    // If this APN doesn't have an MMSC, wait for one that does.
                    if (TextUtils.isEmpty(settings.getMmscUrl())) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "   empty MMSC url, bail");
                        }
                        MmsLog.d(MmsApp.TXN_TAG, "empty MMSC url, bail");
                        if (0 != pendingSize){
                            setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                        }
                        return;
                    }

                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        int slotId;
                        int simId;
                        List<SIMInfo> mListSimInfo;
                        mListSimInfo = SIMInfo.getInsertedSIMList(getApplicationContext());
                        if (mListSimInfo == null || mListSimInfo.size() == 0) {
                            return;
                        }
                        int simNumber = mListSimInfo.size();
                        for (slotId = 0; slotId < simNumber; slotId++) {
                            if (mListSimInfo.get(slotId).getSlot() == slotOfInfo) {
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(),
                                        slotOfInfo);
                                if (null == si) {
                                    MmsLog.e(MmsApp.TXN_TAG,
                                            "TransactionService:SIMInfo is null for slot "
                                                    + slotOfInfo);
                                    return;
                                }
                                simId = (int) si.getSimId();
                                processPendingTransactionGemini(transaction, settings, simId/*
                                                                                             * msg.
                                                                                             * arg2
                                                                                             */);
                                break;
                            }
                        }
                    }else {
                        processPendingTransaction(transaction, settings);
                    }
                    return;
                    /// @}

                case EVENT_TRANSACTION_REQUEST:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_TRANSACTION_REQUEST");

                    int serviceId = msg.arg1;
                    try {
                        TransactionBundle args = (TransactionBundle) msg.obj;
                        TransactionSettings transactionSettings;

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "EVENT_TRANSACTION_REQUEST MmscUrl=" +
                                    args.getMmscUrl() + " proxy port: " + args.getProxyAddress());
                        }

                        // Set the connection settings for this transaction.
                        // If these have not been set in args, load the default settings.
                        String mmsc = args.getMmscUrl();
                        if (mmsc != null) {
                            transactionSettings = new TransactionSettings(
                                    mmsc, args.getProxyAddress(), args.getProxyPort());
                        } else {
                            /// M:Code analyze 025, add for gemini @{ */
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                // convert sim id to slot id
                                int slotId = SIMInfo.getSlotById(getApplicationContext(), msg.arg2);
                                transactionSettings = new TransactionSettings(
                                                    TransactionService.this, null, slotId/*msg.arg2*/);
                            } else {
                                transactionSettings = new TransactionSettings(
                                                    TransactionService.this, null);
                            }
                            /// @}
                        }

                        int transactionType = args.getTransactionType();

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "handle EVENT_TRANSACTION_REQUEST: transactionType=" +
                                    transactionType + " " + decodeTransactionType(transactionType));
                        }

                        // Create appropriate transaction
                        switch (transactionType) {
                            case Transaction.NOTIFICATION_TRANSACTION:
                                String uri = args.getUri();
                                /// M:
                                MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: NOTIFICATION_TRANSACTION, uri="+uri);
                                if (uri != null) {
                                    /// M:Code analyze 026, add for gemini @{
                                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                        transaction = new NotificationTransaction(
                                            TransactionService.this, serviceId, msg.arg2,
                                            transactionSettings, uri);
                                    } else {
                                        transaction = new NotificationTransaction(
                                            TransactionService.this, serviceId,
                                            transactionSettings, uri);
                                    }
                                    /// @}
                                } else {
                                    // Now it's only used for test purpose.
                                    byte[] pushData = args.getPushData();
                                    PduParser parser = new PduParser(pushData);
                                    GenericPdu ind = parser.parse();

                                    int type = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
                                    if ((ind != null) && (ind.getMessageType() == type)) {
                                        /// M:Code analyze 027 add for gemini @{
                                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                            transaction = new NotificationTransaction(
                                                TransactionService.this, serviceId, msg.arg2,
                                                transactionSettings, (NotificationInd) ind);
                                        } else {
                                            transaction = new NotificationTransaction(
                                                TransactionService.this, serviceId,
                                                transactionSettings, (NotificationInd) ind);
                                        }
                                        /// @}
                                    } else {
                                        /// M:
                                        MmsLog.e(MmsApp.TXN_TAG, "Invalid PUSH data.");
                                        transaction = null;
                                        return;
                                    }
                                }
                                break;
                            case Transaction.RETRIEVE_TRANSACTION:
                                /// M:
                                MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: RETRIEVE_TRANSACTION uri=" + args.getUri());
                                /// M:Code analyze 028 add for gemini @{
                                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                    transaction = new RetrieveTransaction(
                                        TransactionService.this, serviceId, msg.arg2,
                                        transactionSettings, args.getUri());
                                } else {
                                    transaction = new RetrieveTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                }
                                /// @}
                                break;
                            case Transaction.SEND_TRANSACTION:
                                /// M:
                                MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: SEND_TRANSACTION");
                                /// M:Code analyze 029 add for gemini @{
                                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                    transaction = new SendTransaction(
                                        TransactionService.this, serviceId, msg.arg2,
                                        transactionSettings, args.getUri());
                                } else {
                                    transaction = new SendTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                }
                                /// @}
                                break;
                            case Transaction.READREC_TRANSACTION:
                                /// M:
                                MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: READREC_TRANSACTION");
                                /// M:Code analyze 030 add for gemini @{
                                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                    transaction = new ReadRecTransaction(
                                        TransactionService.this, serviceId, msg.arg2,
                                        transactionSettings, args.getUri());
                                } else {
                                    transaction = new ReadRecTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                }
                                /// @}
                                break;
                            default:
                                /// M:
                                MmsLog.w(MmsApp.TXN_TAG, "Invalid transaction type: " + serviceId);
                                transaction = null;
                                return;
                        }

                        if (!processTransaction(transaction)) {
                            /** M: add for gemini @{ */
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && null != transaction) {
                                /// M:Code analyze 012,add for new feature,end network connectivity which is specific to the 
                                /// special slot.if current transaction is failed,need to set variable to the id corresponding
                                /// sim card @{
                                mSimIdForEnd = transaction.mSimId;
                                /// @}
                            }
                            /** @} */
                            transaction = null;
                            return;
                        }

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Started processing of incoming message: " + msg);
                        }
                    } catch (Exception ex) {
                        /// M:
                        MmsLog.e(MmsApp.TXN_TAG, "Exception occurred while handling message: " + msg, ex);

                        if (transaction != null) {
                            /// M:Code analyze 012,add for new feature,end network connectivity which is specific to the special slot.
                            /// if current transaction is null,need to set variable to the id corresponding sim card @{
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                mSimIdForEnd = transaction.mSimId;
                            }
                            /// @}
                            try {
                                transaction.detach(TransactionService.this);
                                /// M:Code analyze 031,change this sync. @{
                                synchronized (mProcessing) {
                                    if (mProcessing.contains(transaction)) {
                                        mProcessing.remove(transaction);
                                    }
                                }
                                /// @}
                            } catch (Throwable t) {
                                Log.e(TAG, "Unexpected Throwable.", t);
                            } finally {
                                // Set transaction to null to allow stopping the
                                // transaction service.
                                transaction = null;
                            }
                        }
                    } finally {
                        if (transaction == null) {
                            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                Log.v(TAG, "Transaction was null. Stopping self: " + serviceId);
                            }
                            /// M:
                            MmsLog.d(MmsApp.TXN_TAG, "finally call endMmsConnectivity");

                            /// M:Code analyze 032, judge variable canEnd is true or not,that is to say,only under the condition
                            /// canEnd is true,it is right time to end connectivity,add this for sync @{
                            boolean canEnd = false;
                            synchronized (mProcessing) {
                                canEnd = (mProcessing.size() == 0 && mPending.size() == 0);
                            }
                            /// @}

                            /// M:Code analyze 033,add for gemini,end the connectivity under the condition below @{
                            if (canEnd == true){
                                // add for gemini
                                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                    /// M:Code analyze 012,add for new feature,end network connectivity which is specific to 
                                    /// the special slot .
                                    endMmsConnectivityGemini(mSimIdForEnd);
                                } else {
                                    endMmsConnectivity();
                                }
                            }
                            /// @}
                            /// M:Code analyze 004,add for ALPS00081452,A is calling with B,then C send MMS to A and B,after hangup,
                            /// A and B download MMS fail,this paragraph below is for fixing this problem, will scan pending message
                            /// in database after hangup and try to download message again @{
                            stopSelfIfIdle(serviceId);
                            /// @}
                        }
                    }
                    return;
                case EVENT_HANDLE_NEXT_PENDING_TRANSACTION:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_HANDLE_NEXT_PENDING_TRANSACTION");
                    ///M:Code analyze 033, add for gemini @{
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        processPendingTransactionGemini(transaction, (TransactionSettings) msg.obj, msg.arg2);
                    } else {
                        processPendingTransaction(transaction, (TransactionSettings) msg.obj);
                    }
                    /// @}
                    return;
                /// M:Code analyze 003,add for time out mechanism,if time out,should set this transaction temporary failed.@{
                case EVENT_PENDING_TIME_OUT:
                    //make the pending transaction temporary failed.
                    int pendSize = getPendingSize();
                    if (0 != pendSize){
                        MmsLog.d(MmsApp.TXN_TAG, "a pending connection request time out, mark temporary failed.");
                      /// M:Code analyze 010,add for new feature,this member is used to ignore status message sent by framework
                        /// between time out happened and a new data connection request which need wait.timer is over,varialbe
                        /// should be set to true,duaring this time,status message should not be ignored @{
                        mIgnoreMsg = true;
                        setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                    }
                    return;
                    /// @}
                /// M:Code analyze 004,add for ALPS00081452,A is calling with B,then C send MMS to A and B,after hangup,A and B 
                /// download MMS fail,this paragraph below is for fixing this problem, will scan pending message in database 
                /// after hangup and try to download message again a@{
                case EVENT_SCAN_PENDING_MMS:
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_SCAN_PENDING_MMS");
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        for (int slotId = 0; slotId < EncapsulatedPhone.GEMINI_SIM_NUM; slotId++) {
                            MmsLog.d(MmsApp.TXN_TAG, "scan slot:"+slotId);
                            SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), slotId);
                            if (null != si) {
                                scanPendingMessages(1, false, (int) si.getSimId(), true);
                            }
                        }
                    } else {
                        scanPendingMessages(1, false, -1, true);
                    }
                    break;
                /// @}
                default:
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "handleMessage : default");
                    Log.w(TAG, "what=" + msg.what);
                    return;
            }
        }

        public void processPendingTransaction(Transaction transaction,
                                               TransactionSettings settings) {
            /// M:
            MmsLog.v(MmsApp.TXN_TAG, "processPendingTxn: transaction=" + transaction);
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "processPendingTxn: transaction=" + transaction);
            }

            int numProcessTransaction = 0;
            synchronized (mProcessing) {
                if (mPending.size() != 0) {
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "processPendingTransaction: mPending.size()=" + mPending.size());
                    transaction = mPending.remove(0);
                    /// M:Code analyze 007,avoid stop TransactionService incorrectly. @{
                    mNeedWait = true;
                    /// @}
                }
                numProcessTransaction = mProcessing.size();
            }

            if (transaction != null) {
                if (settings != null) {
                    transaction.setConnectionSettings(settings);
                }

                /*
                 * Process deferred transaction
                 */
                try {
                    int serviceId = transaction.getServiceId();

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processPendingTxn: process " + serviceId);
                    }

                    if (processTransaction(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Started deferred processing of transaction  "
                                    + transaction);
                        }
                    } else {
                        transaction = null;
                        /// M:Code analyze 004,add for ALPS00081452,stop transaction service @{
                        stopSelfIfIdle(serviceId);
                        /// @}
                    }
                } catch (IOException e) {
                    Log.w(TAG, e.getMessage(), e);
                }
            } else {
                if (numProcessTransaction == 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processPendingTxn: no more transaction, endMmsConnectivity");
                    }
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "processPendingTransaction:no more transaction, endMmsConnectivity");
                    endMmsConnectivity();
                }
            }
        }

        /**
         * Internal method to begin processing a transaction.
         * @param transaction the transaction. Must not be {@code null}.
         * @return {@code true} if process has begun or will begin. {@code false}
         * if the transaction should be discarded.
         * @throws IOException if connectivity for MMS traffic could not be
         * established.
         */
        private boolean processTransaction(Transaction transaction) throws IOException {
            /// M:
            MmsLog.v(MmsApp.TXN_TAG, "process Transaction");
            /// M:Code analyze 034,add for judging current connection need to be keeping alive or not @{
            int requestResult = EncapsulatedPhone.APN_REQUEST_FAILED;
            /// @}
            // Check if transaction already processing
            synchronized (mProcessing) {
                /// M:Code analyze 007,avoid stop TransactionService incorrectly. @{
                mNeedWait = false;
                /// @}
                for (Transaction t : mPending) {
                    if (t.isEquivalent(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Transaction already pending: " +
                                    transaction.getServiceId());
                        }
                        /// M:
                        MmsLog.d(MmsApp.TXN_TAG, "Process Transaction: already pending " + transaction.getServiceId());
                        return true;
                    }
                }
                for (Transaction t : mProcessing) {
                    if (t.isEquivalent(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Duplicated transaction: " + transaction.getServiceId());
                        }
                        /// M:
                        MmsLog.d(MmsApp.TXN_TAG, "Process Transaction: Duplicated transaction" + transaction.getServiceId());
                        return true;
                    }
                }

                /// M:Code analyze 035,if mProcessing queue's size is greater than 0,current transaction should be
                /// added into mPending queue,and also add for gemini @{ */
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && (mProcessing.size() > 0 || mIsWaitingConxn)) {
                    mPending.add(transaction);
                    MmsLog.d(MmsApp.TXN_TAG, "add to pending, Processing size=" + mProcessing.size() 
                        + ",is waiting conxn=" + mIsWaitingConxn);
                    return true;
                }
                /// @}
                ///M: add for cmcc, when in call, set transaction fail, but don't increase retryIndex @{
                if (MmsConfig.isRetainRetryIndexWhenInCall()) {
                    if (transaction instanceof SendTransaction
                            || transaction instanceof RetrieveTransaction){
                        boolean incall = isDuringCall();
                        boolean available = isNetworkAvailable();
                        if (!available && incall) {
                            MmsLog.d(MmsApp.TXN_TAG, "networkAvailabe: " + available +
                                    ", isDuringCall:" + incall);
                            setTransactionFail(transaction, FAILE_TYPE_RESTAIN_RETRY_INDEX);
                            return false;
                        }
                    }
                }
                /// @}

                /*
                * Make sure that the network connectivity necessary
                * for MMS traffic is enabled. If it is not, we need
                * to defer processing the transaction until
                * connectivity is established.
                */
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "processTransaction: call beginMmsConnectivity...");
                }
                /// M:Code analyze 036,try to establish network connection,and also modify for gemini @{
                int connectivityResult = 0;
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    connectivityResult = beginMmsConnectivityGemini(transaction.mSimId);
                } else {
                    connectivityResult = beginMmsConnectivity();
                }
                /// @}
                /// M:Code analyze 034,add for judging current connection need to be keeping alive or not @{
                requestResult = connectivityResult;
                /// @}
                /** @} */
                boolean autoDownload = false;
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    autoDownload = DownloadManager.getInstance().isAuto(transaction.mSimId);
                } else {
                    autoDownload = DownloadManager.getInstance().isAuto();
                }
                //if want to send Notification after Calling finished, you can change there, use
                //if (connectivityResult == EncapsulatedPhone.APN_REQUEST_STARTED)
                if ((connectivityResult == EncapsulatedPhone.APN_REQUEST_STARTED) && 
                        !(transaction instanceof NotificationTransaction && !autoDownload)) {
                    ///M: add for alps00425573 @{
                    /// M:Code analyze 006,add for new feature,the variable is using to judge the current transaction can be 
                    /// added the mPending queue or not,only when the apn network state is under APN_REQUEST_STARTED,
                    /// then it is right time to add the transaction into mPending queue @{
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        mPending.add(0,transaction);
                        mIsWaitingConxn = true;
                    } else {
                        mPending.add(transaction);
                    }
                    /// @}
                    /// @}
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processTransaction: connResult=APN_REQUEST_STARTED, " +
                                "defer transaction pending MMS connectivity");
                    }
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "mPending.size()=" + mPending.size());
                    return true;
                } 
                /// M:Code analyze 037,if connectivityResult is the state below,that means apn net work connection failed,
                /// so set current transaction failed also,add for gemini and open @{ */
                else if (connectivityResult == EncapsulatedPhone.APN_TYPE_NOT_AVAILABLE
                        ||connectivityResult == EncapsulatedPhone.APN_REQUEST_FAILED){
                    //add for read report, make it easy.
                    if (transaction instanceof ReadRecTransaction) {
                        setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                        return false;
                    }

                    if (transaction instanceof SendTransaction
                        || transaction instanceof RetrieveTransaction){
                        /// M:Code analyze 004,add for ALPS00081452,if failed becaused of call, moniter call end, and go on process
                        if (isDuringCall()) {
                            synchronized (mIdleLock) {
                                mEnableCallbackIdle = true;
                            }
                          ///M: modify for cmcc, when in call, set transaction fail, but don't increase retryIndex @{
                            MmsLog.d(MmsApp.TXN_TAG, "DuringCall, transation is " + transaction);
                            if (MmsConfig.isRetainRetryIndexWhenInCall()) {
                                setTransactionFail(transaction, FAILE_TYPE_RESTAIN_RETRY_INDEX);
                            } else {
                                setTransactionFail(transaction, FAILE_TYPE_TEMPORARY);
                            }
                            ///@}
                        } else {
                            /// M: For CMCC FT here allow retry
                            if (MmsConfig.isAllowRetryForPermanentFail()) {
                                setTransactionFail(transaction, FAILE_TYPE_TEMPORARY);
                            } else {
                                setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                            }
                            /// @}
                        }
                        return false;
                    }
                }
                /// @}
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "Adding Processing list: " + transaction);
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "Adding transaction to 'mProcessing' list: " + transaction);
                }
                mProcessing.add(transaction);
            }

            /// M:Code analyze 034,add for judging current connection need to be keeping alive or not,
            /// if yes,resent EVENT_CONTINUE_MMS_CONNECTIVITY message to connect again,and modify for gemini @{ */
            if (requestResult == EncapsulatedPhone.APN_ALREADY_ACTIVE) {
                MmsLog.d(MmsApp.TXN_TAG, "request ok, renew connection.");
                // Set a timer to keep renewing our "lease" on the MMS connection
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    int slotId = SIMInfo.getSlotById(getApplicationContext(), transaction.mSimId);
                    renewMmsConnectivity(0, slotId);
                } else {
                    renewMmsConnectivity(0, 0);
                }
            }
            /// @}

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "processTransaction: starting transaction " + transaction);
            }

            // Attach to transaction and process it
            transaction.attach(TransactionService.this);
            transaction.process();
            return true;
        }

        /// M:Code analyze 038,add for new feature,after apn network connection state changed to APN_REQUEST_STARTED,
        /// and invoke processPendingTransactionGemini to call beginMmsConnectivityGemini to connect again, then apn network
        /// state finally changed to APN_ALREADY_ACTIVE,and process relevant transaction,and add for gemini
        public void processPendingTransactionGemini(Transaction transaction,
                                               TransactionSettings settings, int simId) {
            MmsLog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini: transaction=" + transaction + " sim ID="+simId);

            int numProcessTransaction = 0;
            synchronized (mProcessing) {
                if (mPending.size() != 0) {
                    MmsLog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini: Pending size=" + mPending.size());
                    Transaction transactiontemp = null;
                    int pendingSize = mPending.size();
                    for (int i = 0; i < pendingSize; ++i){
                        transactiontemp = mPending.remove(0);
                        if (simId == transactiontemp.mSimId){
                            transaction = transactiontemp;
                            MmsLog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini, get transaction with same simId");
                            /// M:Code analyze 007,avoid stop TransactionService incorrectly. @{
                            mNeedWait = true;
                            /// @}
                            break;
                        }else{
                            mPending.add(transactiontemp);
                            MmsLog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini, diffrent simId, add to tail");
                        }
                    }
                    if (null == transaction) {
                        transaction = mPending.remove(0);
                        /// M:Code analyze 007,avoid stop TransactionService incorrectly. @{
                        mNeedWait = true;
                        /// @}
                        endMmsConnectivityGemini(simId);
                        MmsLog.d(MmsApp.TXN_TAG, "Another SIM:" + transaction.mSimId);
                    }
                }
                numProcessTransaction = mProcessing.size();
            }

            if (transaction != null) {
                if (settings != null) {
                    transaction.setConnectionSettings(settings);
                }

                /// M:Code analyze 006,add for new feature,the variable is using to judge the current transaction can be
                /// added the mPending queue or not,only when the apn network state is under APN_REQUEST_STARTED,then it is
                /// right time to add the  transaction into mPending queue in the case below,the current transaction will be
                /// handled immediately,so should not be added into mPending queue @{
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    mIsWaitingConxn = false;
                }
                /// @}

                try {
                    int serviceId = transaction.getServiceId();
                    MmsLog.d(MmsApp.TXN_TAG, "processPendingTxnGemini: process " + serviceId);

                    if (processTransaction(transaction)) {
                        MmsLog.d(MmsApp.TXN_TAG, "Started deferred processing of transaction  " + transaction);
                    } else {
                        transaction = null;
                        //change for 81452
                        stopSelfIfIdle(serviceId);
                    }
                } catch (IOException e) {
                    MmsLog.e(MmsApp.TXN_TAG, e.getMessage(), e);
                }
            } else {
                if (numProcessTransaction == 0) {
                    MmsLog.d(MmsApp.TXN_TAG, "processPendingTxnGemini:no more transaction, endMmsConnectivity");
                    endMmsConnectivityGemini(simId);
                }
            }
        }
    }
    /// @}

    /// M:Code analyze 039,add for new feature,aim to keep current connection alive,continue sending or receiving mms,
    /// modify method parameters,arg1 useless currently,arg2 slotId in gemini mode @{
    private void renewMmsConnectivity(int arg1, int arg2) {
        // Set a timer to keep renewing our "lease" on the MMS connection
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            mServiceHandler.sendMessageDelayed(
                                mServiceHandler.obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY, arg1, arg2),
                                APN_EXTENSION_WAIT);
        } else {
            mServiceHandler.sendMessageDelayed(
                                mServiceHandler.obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                                        APN_EXTENSION_WAIT);
        }
    }
    /// @}

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.w(TAG, "ConnectivityBroadcastReceiver.onReceive() action: " + action);
            }

            /// M:Code analyze 014,add for bug,send a broadcast uing apn network connection without any delay,
            /// the code below is using for checking that is the right action or not@{
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION_IMMEDIATE)) {
                return;
            }
            /// @}

            /// M:Code analyze 013,add for fix bug,for handling framework sticky intent issue @{
            if (mFWStickyIntent != null) {
                MmsLog.d(MmsApp.TXN_TAG, "get sticky intent" + mFWStickyIntent);
                mFWStickyIntent = null;
                return;
            }
            /// @}

            boolean noConnectivity =
               intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            NetworkInfo networkInfo = (NetworkInfo)
                intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            /*
             * If we are being informed that connectivity has been established
             * to allow MMS traffic, then proceed with processing the pending
             * transaction, if any.
             */

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "Handle ConnectivityBroadcastReceiver.onReceive(): " + networkInfo);
            }

            // Check availability of the mobile network.
            if ((networkInfo == null) || (networkInfo.getType() !=
                    ConnectivityManager.TYPE_MOBILE_MMS)) {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "   type is not TYPE_MOBILE_MMS, bail");
                }
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "ignore a none mobile mms status message.");

                /// M:Code analyze 040, we needn't this,comment it @{
                // This is a very specific fix to handle the case where the phone receives an
                // incoming call during the time we're trying to setup the mms connection.
                // When the call ends, restart the process of mms connectivity.
                //if (networkInfo != null &&
                //    Phone.REASON_VOICE_CALL_ENDED.equals(networkInfo.getReason())) {
                //    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                //      Log.v(TAG, "   reason is " + Phone.REASON_VOICE_CALL_ENDED +
                //            ", retrying mms connectivity");
                //    }
                //    renewMmsConnectivity();
                //}
                /// @}
                return;
            }

            /// M:Code analyze 041,comment the pragraph below, we do this in EVENT_DATA_STATE_CHANGED branch @{
            /*
            if (!networkInfo.isConnected()) {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "   TYPE_MOBILE_MMS not connected, bail");
                }
                return;
            }

            TransactionSettings settings = new TransactionSettings(
                    TransactionService.this, networkInfo.getExtraInfo());

            // If this APN doesn't have an MMSC, wait for one that does.
            if (TextUtils.isEmpty(settings.getMmscUrl())) {
                Log.v(TAG, "   empty MMSC url, bail");
                return;
            }

            renewMmsConnectivity();
            mServiceHandler.processPendingTransaction(null, settings);
            */
            /// @}

            /// M:Code analyze 042, we still keep EVENT_DATA_STATE_CHANGED case and use it. @{ */
            Message msg = mServiceHandler.obtainMessage(EVENT_DATA_STATE_CHANGED);
            msg.arg2 = intent.getIntExtra("simId", -1);
            mServiceHandler.sendMessage(msg);
            /// @}
        }
    };

    /// M: new members
    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is Start. add for gemini smart
     */
    public static final String TRANSACTION_START = "com.android.mms.transaction.START";

    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is Stop. add for gemini smart
     */
    public static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";

    /// M:Code analyze 002,add for new feature,check Apn network is available or not before sending mms @{
    private static final int EVENT_DATA_STATE_CHANGED = 2;
    /// M:@}

    /// M:Code analyze 003,add for time out mechanism
    private static final int EVENT_PENDING_TIME_OUT = 101;
    /// @}

    /// M:Code analyze 004,add for ALPS00081452,A is calling with B,then C send MMS to A and B,after hangup,A and B download
    /// MMS fail,this paragraph below is for fixing this problem, will scan pending message in database after hangup
    /// and try to download message again a@{ */
    private static final int EVENT_SCAN_PENDING_MMS = 102;
    private PhoneStateListener mPhoneStateListener;
    private PhoneStateListener mPhoneStateListener2;
    private PhoneStateListener mPhoneStateListener3;
    private PhoneStateListener mPhoneStateListener4;
    /// phone state in single mode, in gemini mode slot0 state
    private int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    /// phone state of slot1 in gemini mode
    private int mPhoneState2 = TelephonyManager.CALL_STATE_IDLE;
    private int mPhoneState3 = TelephonyManager.CALL_STATE_IDLE;
    private int mPhoneState4 = TelephonyManager.CALL_STATE_IDLE;
    private Object mPhoneStateLock = new Object();
    private int mLastIdleSlot = EncapsulatedPhone.GEMINI_SIM_1;
    private Object mIdleLock = new Object();
    private boolean mEnableCallbackIdle = false;
    /// @}

    /// M:Code analyze 005,add for new feature,two failed states of all transactions
    private static final int FAILE_TYPE_PERMANENT = 1;
    private static final int FAILE_TYPE_TEMPORARY = 2;
  ///M: modify for cmcc, when in call, set transaction fail, but don't increase retryIndex
    private static final int FAILE_TYPE_RESTAIN_RETRY_INDEX = 3;
    /// M:@}

    /// M:Code analyze 006,add for new feature,the variable is using to judge the current transaction can be added the mPending
    /// queue or not,only when the apn network state is under APN_REQUEST_STARTED condition,then it is right time to add the 
    /// transaction into mPending queue @{
    private boolean mIsWaitingConxn = false;
    /// M:@}

    /// M:Code analyze 007,avoid stop TransactionService incorrectly. @{
    private boolean mNeedWait = false;
    /// @}

    /// M:Code analyze 008,unkonw, @{
    private long mTriggerMsgId = 0;
    /// @}

    /// M:Code analyze 009,add for time out mechanism,and duration is 3 minutes @{
    private static final long REQUEST_CONNECTION_TIME_OUT_LENGTH = 3 * 60 * 1000;
    /// @}

    /// M:Code analyze 010,add for new feature,this member is used to ignore status message sent by framework
    /// between time out happened and a new data connection request which need wait.@{
    private boolean mIgnoreMsg = false;
    /// @}

    /// M:Code analyze 011,add for fix bug, the serviceId[startId] of each transaction got from framewrok is increasing by order.
    /// so the max service id is recorded. and must invoked as the last transaction finish.@{
    private int mMaxServiceId = Integer.MIN_VALUE;
    /// @}

    /// M:Code analyze 012,add for new feature,end network connectivity which is specific to the special slot @{
    private int mSimIdForEnd = 0;
    /// @}

    /// M:Code analyze 013,add for fix bug,for handling framework sticky intent issue @{
    private Intent mFWStickyIntent = null;
    /// @}

    /// M:Code analyze 043, this method is used to scan pending messages in database to re-process them one by one.
    /// startId: useless now.    * noNetwork: whether the network is ok.
    /// simId: for single mode use -1, for gemini mode use -1 means no filter.
    /// scanAll: control scan scope.@{
    private void scanPendingMessages(int startId, boolean noNetwork, int simId, boolean scanAll) {
        MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: startid=" + startId 
            + ", Request simId=" + simId+ ", noNetwork=" + noNetwork + "scanAll:" + scanAll);
        // Scan database to find all pending operations.
        Cursor cursor = PduPersister.getPduPersister(this).getPendingMessages(
                scanAll?Long.MAX_VALUE:SystemClock.elapsedRealtime());
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessages: Pending Message Size=" + count);
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "scanPendingMessages: cursor.count=" + count);
                }

                if (count == 0 && mTriggerMsgId == 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "scanPendingMessages: no pending messages. Stopping service.");
                    }
                    if (!scanAll) {
                        RetryScheduler.setRetryAlarm(this);
                    }
                    stopSelfIfIdle(startId);
                    return;
                }

                int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
                int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
                /*gemini specific*/
                int columnIndexOfSimId = cursor.getColumnIndexOrThrow(EncapsulatedTelephony.MmsSms.PendingMessages.SIM_ID);
                int columnIndexOfErrorType = cursor.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE);

                if (noNetwork) {
                    // Make sure we register for connection state changes.
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "scanPendingMessages: registerForConnectionStateChanges");
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: registerForConnectionStateChanges");
                    MmsSystemEventReceiver.registerForConnectionStateChanges(
                            getApplicationContext());
                }
                int msgType = 0;
                int transactionType = 0;
                /*gemini specific*/
                int pendingMsgSimId = 0;

                while (cursor.moveToNext()) {
                    msgType = cursor.getInt(columnIndexOfMsgType);
                    transactionType = getTransactionType(msgType);
                    if (noNetwork && (!EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT)/*only single card mode show toast*/) {
                        onNetworkUnavailable(startId, transactionType);
                        return;
                    }
                    /*gemini specific*/
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        pendingMsgSimId = cursor.getInt(columnIndexOfSimId);
                        MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: pendingMsgSimId=" + pendingMsgSimId);
                        if ((simId != -1) && (simId != pendingMsgSimId)) {
                            MmsLog.d(MmsApp.TXN_TAG, "Gemini mode, request only process simId:" + simId
                                    + ",current simId is:" + pendingMsgSimId);
                            continue;
                        }
                        if (MmsSms.ERR_TYPE_GENERIC_PERMANENT == cursor.getInt(columnIndexOfErrorType)) {
                            MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: Error type = Permanent, Continue!");
                            continue;
                        }
                        if (mTriggerMsgId == cursor.getLong(columnIndexOfMsgId)) {
                            MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: Message ID = Trigger message ID, Continue!");
                            continue;
                        }
                    }

                    switch (transactionType) {
                        case -1:
                            MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: transaction Type= -1");
                            break;
                        case Transaction.RETRIEVE_TRANSACTION:
                            MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: transaction Type= RETRIEVE");
                            // If it's a transiently failed transaction,
                            // we should retry it in spite of current
                            // downloading mode.
                            int failureType = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(
                                            PendingMessages.ERROR_TYPE));
                            /// M: ALPS00545779, for FT, restart pending receiving mms @ {
                            IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);

                            if (mmsTransactionPlugin == null) {
                                if (!isTransientFailure(failureType)) {
                                    MmsLog.d(MmsApp.TXN_TAG, cursor.getLong(columnIndexOfMsgId)
                                            + "this RETRIEVE not transient failure");
                                    break;
                                }
                            } else {
                                Uri mmsUri = ContentUris.withAppendedId(
                                        Mms.CONTENT_URI,
                                        cursor.getLong(columnIndexOfMsgId));
                                /* Old condition was !isTransientFailure(failureType))
                                                            Now this condition is moved to default MmsTransactionImpl for default*/
                                if (!mmsTransactionPlugin.isPendingMmsNeedRestart(mmsUri, failureType)) {
                                    MmsLog.d(MmsApp.TXN_TAG, cursor.getLong(columnIndexOfMsgId)
                                            + "this RETRIEVE not transient failure");
                                    break;
                                }
                            }
                            /// @}
                            // fall-through
                        default:
                            Uri uri = ContentUris.withAppendedId(
                                    Mms.CONTENT_URI,
                                    cursor.getLong(columnIndexOfMsgId));
                            MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessages: Pending Message uri=" + uri);

                            TransactionBundle args = new TransactionBundle(
                                    transactionType, uri.toString());
                            // FIXME: We use the same startId for all MMs.
                            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                                if (pendingMsgSimId > 0) {
                                    launchTransactionGemini(startId, pendingMsgSimId, args);
                                } else {
                                    // for handling third party
                                    long connectSimId = EncapsulatedSettings.System.getLong(getContentResolver(), EncapsulatedSettings.System.GPRS_CONNECTION_SIM_SETTING, EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET);
                                    MmsLog.v(MmsApp.TXN_TAG, "Scan Pending message:  current data settings: " + connectSimId);
                                    if (EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET != connectSimId && EncapsulatedSettings.System.GPRS_CONNECTION_SIM_SETTING_NEVER != connectSimId) {
                                        launchTransactionGemini(startId, (int)connectSimId, args);
                                    }
                                }
                            } else {
                                launchTransaction(startId, args, false);
                            }
                            break;
                    }
                }
            } finally {
                cursor.close();
            }
        } else {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "scanPendingMessages: no pending messages. Stopping service.");
            } 
            MmsLog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: no pending messages. Stopping service.");
            if (mTriggerMsgId == 0) {
                if (!scanAll) {
                    RetryScheduler.setRetryAlarm(this);
                }
                stopSelfIfIdle(startId);
            }
        }
    }
    /// @}

    /// M:Code analyze 044, set transaction failed,meantime,set some state belong to this transaction failed,and add for
    /// gemini and open @{
    private void setTransactionFail(Transaction txn, int failType) {
        MmsLog.v(MmsApp.TXN_TAG, "set Transaction Fail. fail Type=" + failType);

        /// M:Code analyze 006,add for new feature,the variable is using to judge the current transaction can be added 
        /// the mPending queue or not,only when the apn network state is under APN_REQUEST_STARTED,then it is right time
        /// to add the transaction into mPending queue in this case below,because the trsaction is already failed,
        /// so set it false,should not be added into mPending queue @{
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            mIsWaitingConxn = false;
        }
        /// @}
        long msgId = 0;
        Uri uri = null;
        if (txn instanceof SendTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Send");
            uri = ((SendTransaction)txn).getSendReqUri();
        } else if (txn instanceof NotificationTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Notification");
            uri = ((NotificationTransaction)txn).getNotTrxnUri();
        } else if (txn instanceof RetrieveTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Retrieve");
            uri = ((RetrieveTransaction)txn).getRtrTrxnUri();
        } else if (txn instanceof ReadRecTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :ReadRec");
            uri = ((ReadRecTransaction)txn).getRrecTrxnUri();
            // add this for read report.
            //if the read report is failed to open connection.mark it sent(129).i.e. only try to send once.
            //[or mark 128, this is another policy, this will resend next time into UI and out.]
            ContentValues values = new ContentValues(1);
            values.put(Mms.READ_REPORT, 129);
            SqliteWrapper.update(getApplicationContext(), getApplicationContext().getContentResolver(),
                                uri,
                                values,
                                null, null);
            txn.mTransactionState.setState(TransactionState.FAILED);
            txn.mTransactionState.setContentUri(uri);
            txn.attach(TransactionService.this);
            txn.notifyObservers();
            return;
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. type cann't be recognised");
        }

        if (null != uri) {
            txn.mTransactionState.setContentUri(uri);
            msgId = ContentUris.parseId(uri);
        } else {
            MmsLog.e(MmsApp.TXN_TAG, "set Transaction Fail. uri is null.");
            return;
        }

        if (txn instanceof NotificationTransaction) {
            DownloadManager downloadManager = DownloadManager.getInstance();
            boolean autoDownload = false;
            // add for gemini
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                autoDownload = downloadManager.isAuto(txn.mSimId);
            } else {
                autoDownload = downloadManager.isAuto();
            }

            if (!autoDownload) {
                txn.mTransactionState.setState(TransactionState.SUCCESS);
            } else {
                txn.mTransactionState.setState(TransactionState.FAILED);
            }
        } else {
            txn.mTransactionState.setState(TransactionState.FAILED);
        }

        txn.attach(TransactionService.this);
        MmsLog.d(MmsApp.TXN_TAG, "attach this transaction.");

        Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(getApplicationContext(), 
                                            getApplicationContext().getContentResolver(),
                                            uriBuilder.build(), 
                                            null, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    // Mark the failed message as unread.
                    ContentValues readValues = new ContentValues(1);
                    readValues.put(Mms.READ, 0);
                    SqliteWrapper.update(getApplicationContext(), getApplicationContext().getContentResolver(),
                                    uri, readValues, null, null);

                    DefaultRetryScheme scheme = new DefaultRetryScheme(getApplicationContext(), 100);

                    ContentValues values = null;
                    if (FAILE_TYPE_PERMANENT == failType) {
                        values = new ContentValues(2);
                        values.put(PendingMessages.ERROR_TYPE,  MmsSms.ERR_TYPE_GENERIC_PERMANENT);
                        values.put(PendingMessages.RETRY_INDEX, scheme.getRetryLimit());

                        int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                        long id = cursor.getLong(columnIndex);

                        SqliteWrapper.update(getApplicationContext(), 
                                            getApplicationContext().getContentResolver(),
                                            PendingMessages.CONTENT_URI,
                                            values, PendingMessages._ID + "=" + id, null);
                    }
                    ///M: add for cmcc, retry time not increase @{
                    else if (FAILE_TYPE_RESTAIN_RETRY_INDEX == failType) {
                        int retryIndex = cursor.getInt(cursor.getColumnIndexOrThrow(
                                PendingMessages.RETRY_INDEX)); // Count this time.
                        if (retryIndex > 0) {
                            retryIndex --;
                        }
                        MmsLog.d(MmsApp.TXN_TAG, "failType = 3, retryIndex = " + retryIndex);
                        values = new ContentValues(1);
                        values.put(PendingMessages.RETRY_INDEX, retryIndex);
                        int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                        long id = cursor.getLong(columnIndex);
                        SqliteWrapper.update(getApplicationContext(),
                                getApplicationContext().getContentResolver(),
                                PendingMessages.CONTENT_URI,
                                values, PendingMessages._ID + "=" + id, null);
                    }
                    ///@}
                }
            }finally {
                cursor.close();
            }
        }

        txn.notifyObservers();
    }
    /// @}

    /// M:Code analyze 045,add for gemini,using for sending EVENT_TRANSACTION_REQUEST message,similar with launchTransaction @{
    private void launchTransactionGemini(int serviceId, int simId, TransactionBundle txnBundle) {
        Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
        msg.arg1 = serviceId;
        msg.arg2 = simId;
        msg.obj = txnBundle;

        MmsLog.d(MmsApp.TXN_TAG, "launchTransactionGemini: sending message " + msg);
        mServiceHandler.sendMessage(msg);
    }
    /// @}

    /// M:Code analyze 046,add for gemini,using for establishing apn network connection,similar with beginMmsConnectivity @{
    protected int beginMmsConnectivityGemini(int simId) throws IOException {
        // Take a wake lock so we don't fall asleep before the message is downloaded.
        createWakeLock();

        /// M: ALPS00442702, before start pdp need wait @{
        synchronized (mPdpLock) {
            MmsLog.d(MmsApp.TXN_TAG, "beginMmsConnectivityGemini check mPdpWait=" + mPdpWait);
            if (mPdpWait) {
                try {
                    MmsLog.d(MmsApp.TXN_TAG, "start wait");
                    mPdpLock.wait(1000);
                } catch (InterruptedException ex) {
                    MmsLog.e(MmsApp.TXN_TAG, "wait has been intrrupted", ex);
                } finally {
                    MmsLog.d(MmsApp.TXN_TAG, "after wait");
                    mPdpWait = false;
                }
            }
        }
        /// @}
        // convert sim id to slot id
        int slotId = SIMInfo.getSlotById(getApplicationContext(), simId);

        int result = mConnMgr.startUsingNetworkFeatureGemini(
                ConnectivityManager.TYPE_MOBILE, EncapsulatedPhone.FEATURE_ENABLE_MMS, slotId);

        MmsLog.d(MmsApp.TXN_TAG, "beginMmsConnectivityGemini: simId=" + simId + "\t slotId=" + slotId + "\t result=" + result);

        switch (result) {
            case EncapsulatedPhone.APN_ALREADY_ACTIVE:
            case EncapsulatedPhone.APN_REQUEST_STARTED:
                acquireWakeLock();
                sendBroadcast(new Intent(TRANSACTION_START));
                //add this for time out mechanism
                setDataConnectionTimer(result);
                return result;
            case EncapsulatedPhone.APN_TYPE_NOT_AVAILABLE:
            case EncapsulatedPhone.APN_REQUEST_FAILED:
                return result;
            default:
                throw new IOException("Cannot establish MMS connectivity");
        }
    }
    /// @}

    /// M:Code analyze 047,add for gemini,end apn network connection,similar with endMmsConnectivity @{
    protected void endMmsConnectivityGemini(int simId) {
        try {
            // convert sim id to slot id
            int slotId = SIMInfo.getSlotById(getApplicationContext(), simId);

            MmsLog.d(MmsApp.TXN_TAG, "endMmsConnectivityGemini: slot id = " + slotId);

            // cancel timer for renewal of lease
            mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
            if (mConnMgr != null) {
                mConnMgr.stopUsingNetworkFeatureGemini(
                        ConnectivityManager.TYPE_MOBILE,
                        EncapsulatedPhone.FEATURE_ENABLE_MMS, slotId);
                sendBroadcast(new Intent(TRANSACTION_STOP));
                /// M:
                MmsLog.d(MmsApp.TXN_TAG, "stopUsingNetworkFeature");
                /// M: Pass status code to handle server error @{
                IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
                /// M: ALPS00442702, before start pdp need wait @{
                if (mmsTransactionPlugin.isSyncStartPdpEnabled()) {
                    synchronized(mPdpLock) {
                        MmsLog.d(MmsApp.TXN_TAG, "endMmsConnectivityGemini set mPdpWait");
                        mPdpWait = true;
                    }
                }
                /// @}
                mmsTransactionPlugin.updateConnection();
                /// @}
            }
        } finally {
            releaseWakeLock();
            mTriggerMsgId = 0;
        }
    }
    /// @}

    /// M:Code analyze 003,set a type of EVENT_PENDING_TIME_OUT timer,time out is 3 minutes
    private void setDataConnectionTimer(int result) {
        if (result == EncapsulatedPhone.APN_REQUEST_STARTED) {
            /// M:Code analyze 010,add for new feature,this member is used to ignore status message sent by framework
            /// between time out happened and a new data connection request which need wait,timer is just stated,variable
            /// should be set to false,duaring this time,status message should not be ignored @{
            mIgnoreMsg = false;
            /// @}
            if (!mServiceHandler.hasMessages(EVENT_PENDING_TIME_OUT)) {
                MmsLog.d(MmsApp.TXN_TAG, "a timer is created.");
                Message msg = mServiceHandler.obtainMessage(EVENT_PENDING_TIME_OUT);
                mServiceHandler.sendMessageDelayed(msg, REQUEST_CONNECTION_TIME_OUT_LENGTH);
            }
        }
    }
    /// @}

    /// M:Code analyze 048,using for get count of transactions in the ePending queue @{
    private int getPendingSize() {
        int pendingSize = 0;
        synchronized (mProcessing) {
            pendingSize = mPending.size();
        }
        return pendingSize;
    }
    /// @}

    /// M:Code analyze 049,using for remove the transaction in ePending queue @{
    private Transaction removePending(int index) {
        Transaction trxn = null;
        synchronized (mProcessing) {
            trxn = mPending.remove(index);
        }
        return trxn;
    }
    /// @}

    /// M:Code analyze 004,add for ALPS00081452,check whether the request data connection fail is caused by calling going on. @{
    private boolean isDuringCall() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            synchronized (mPhoneStateLock) {
                EncapsulatedTelephonyManagerEx teleManager = EncapsulatedTelephonyManagerEx.getDefault();
                mPhoneState = teleManager.getCallState(EncapsulatedPhone.GEMINI_SIM_1);
                mPhoneState2 = teleManager.getCallState(EncapsulatedPhone.GEMINI_SIM_2);
                mPhoneState3 = teleManager.getCallState(EncapsulatedPhone.GEMINI_SIM_3);
                mPhoneState4 = teleManager.getCallState(EncapsulatedPhone.GEMINI_SIM_4);
            }
            return (mPhoneState != TelephonyManager.CALL_STATE_IDLE)
                    || (mPhoneState2 != TelephonyManager.CALL_STATE_IDLE)
                    || (mPhoneState3 != TelephonyManager.CALL_STATE_IDLE)
                    || (mPhoneState4 != TelephonyManager.CALL_STATE_IDLE);
        } else {
            synchronized (mPhoneStateLock) {
                mPhoneState = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getCallState();
            }
            return mPhoneState != TelephonyManager.CALL_STATE_IDLE;
        }
    }
    /// @}


    /// M:Code analyze 004,add for ALPS00081452,A is calling with B,then C send MMS to A and B,after hangup,A and B
    /// download MMS fail,this function is created for registered call back to send EVENT_SCAN_PENDING_MMS message,
    /// then handleMessage will handle this message a@{
    private void callbackState() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                mPhoneState2 == TelephonyManager.CALL_STATE_IDLE &&
                mPhoneState3 == TelephonyManager.CALL_STATE_IDLE &&
                mPhoneState4 == TelephonyManager.CALL_STATE_IDLE) {
                synchronized (mIdleLock) {
                    if (mEnableCallbackIdle) {
                        Message msg = mServiceHandler.obtainMessage(EVENT_SCAN_PENDING_MMS);
                        mServiceHandler.sendMessageDelayed(msg, SCAN_PENDING_MMS_WAIT);
                        mEnableCallbackIdle = false;
                    }
                }
            }
        } else {
            if (mPhoneState == TelephonyManager.CALL_STATE_IDLE) {
                synchronized (mIdleLock) {
                    if (mEnableCallbackIdle) {
                        Message msg = mServiceHandler.obtainMessage(EVENT_SCAN_PENDING_MMS);
                        mServiceHandler.sendMessageDelayed(msg, SCAN_PENDING_MMS_WAIT);
                        mEnableCallbackIdle = false;
                    }
                }
            }
        }
    }
    /// @}

    /// M:Code analyze 004,add for ALPS00081452,register phone call listener @{
    private void registerPhoneCallListener() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState = state;
                    }
                    if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState2 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState3 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState4 == TelephonyManager.CALL_STATE_IDLE) {
                        mLastIdleSlot = EncapsulatedPhone.GEMINI_SIM_1;
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "get slot0 new state:"+state+",slot1 current state:"+mPhoneState2
                            +",slot2 current state:"+mPhoneState3+",slot3 current state:"+mPhoneState4
                            +",mEnableCallbackIdle:"+mEnableCallbackIdle+",mLastIdleSlot:"+mLastIdleSlot);
                    callbackState();
                }
            };
            mPhoneStateListener2 = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState2 = state;
                    }
                    if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState2 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState3 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState4 == TelephonyManager.CALL_STATE_IDLE) {
                        mLastIdleSlot = EncapsulatedPhone.GEMINI_SIM_2;
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "get slot1 new state:"+state+",slot0 current state:"+mPhoneState
                            +",slot2 current state:"+mPhoneState3+",slot3 current state:"+mPhoneState4
                            +",mEnableCallbackIdle:"+mEnableCallbackIdle+",mLastIdleSlot:"+mLastIdleSlot);
                    callbackState();
                }
            };
            mPhoneStateListener3 = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState3 = state;
                    }
                    if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState2 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState3 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState4 == TelephonyManager.CALL_STATE_IDLE) {
                        mLastIdleSlot = EncapsulatedPhone.GEMINI_SIM_3;
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "get slot2 new state:"+state+",slot0 current state:"+mPhoneState
                            +",slot1 current state:"+mPhoneState2+",slot3 current state:"+mPhoneState4
                            +",mEnableCallbackIdle:"+mEnableCallbackIdle+",mLastIdleSlot:"+mLastIdleSlot);
                    callbackState();
                }
            };
            mPhoneStateListener4 = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState4 = state;
                    }
                    if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState2 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState3 == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState4 == TelephonyManager.CALL_STATE_IDLE) {
                        mLastIdleSlot = EncapsulatedPhone.GEMINI_SIM_4;
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "get slot3 new state:"+state+",slot0 current state:"+mPhoneState
                            +",slot1 current state:"+mPhoneState2+",slot2 current state:"+mPhoneState3
                            +",mEnableCallbackIdle:"+mEnableCallbackIdle+",mLastIdleSlot:"+mLastIdleSlot);
                        callbackState();
                }
            };
            EncapsulatedTelephonyManagerEx teleManager = new EncapsulatedTelephonyManagerEx(this);
            teleManager.listen(mPhoneStateListener,
                            PhoneStateListener.LISTEN_CALL_STATE, EncapsulatedPhone.GEMINI_SIM_1);
            teleManager.listen(mPhoneStateListener2,
                            PhoneStateListener.LISTEN_CALL_STATE, EncapsulatedPhone.GEMINI_SIM_2);
            teleManager.listen(mPhoneStateListener3,
                            PhoneStateListener.LISTEN_CALL_STATE, EncapsulatedPhone.GEMINI_SIM_3);
            teleManager.listen(mPhoneStateListener4,
                            PhoneStateListener.LISTEN_CALL_STATE, EncapsulatedPhone.GEMINI_SIM_4);
        }else{
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState = state;
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "get new state:"+state+",mEnableCallbackIdle:"+mEnableCallbackIdle);
                    callbackState();
                }
            };
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }
    /// @}
}
