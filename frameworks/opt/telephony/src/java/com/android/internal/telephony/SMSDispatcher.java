/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.ContentObserver;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Sms.Intents;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SmsCbMessage;
import android.telephony.SmsMessage;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.util.EventLog;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.R;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.HexDump;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Random;
import java.util.List;

import static android.telephony.SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_LIMIT_EXCEEDED;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_NULL_PDU;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;

// MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
import android.app.NotificationManager;
import android.app.Notification;

/*
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.WapPushOverSms;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.telephony.ITelephony;
*/

import com.mediatek.common.featureoption.FeatureOption;

// MTK_OPTR_PROTECT_START
// import com.mediatek.dmagent.DMAgent;
// MTK_OPTR_PROTECT_END

// DM-Agent
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;

// for Netqin lib
import com.netqin.NqSmsFilter;
/*
import android.content.pm.ApplicationInfo;
*/
// MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.sms.IWapPushFwkExt;
import android.app.AlarmManager;
import android.os.SystemClock;
import android.app.ActivityManager;
import java.util.Iterator;
import com.mediatek.common.sms.IDupSmsFilterExt;


public abstract class SMSDispatcher extends Handler {
    static final String TAG = "SMS";    // accessed from inner class
    private static final String SEND_NEXT_MSG_EXTRA = "SendNextMsg";

    /** Default timeout for SMS sent query */
    private static final int DEFAULT_SMS_TIMEOUT = 6000;

    /** Permission required to receive SMS and SMS-CB messages. */
    public static final String RECEIVE_SMS_PERMISSION = "android.permission.RECEIVE_SMS";

    /** Permission required to receive ETWS and CMAS emergency broadcasts. */
    public static final String RECEIVE_EMERGENCY_BROADCAST_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

    /** Permission required to send SMS to short codes without user confirmation. */
    private static final String SEND_SMS_NO_CONFIRMATION_PERMISSION =
            "android.permission.SEND_SMS_NO_CONFIRMATION";

    /** Query projection for checking for duplicate message segments. */
    private static final String[] PDU_PROJECTION = new String[] {
            "pdu"
    };

    /** Query projection for combining concatenated message segments. */
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = new String[] {
            "pdu",
            "sequence",
            "destination_port"
    };
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    protected static final String[] RAW_PROJECTION = new String[] {
        "pdu",
        "sequence",
        "destination_port",
    };

    protected static final String[] CB_RAW_PROJECTION = new String[] {
        "pdu",
        "sequence",
    };
    // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    private static final int PDU_COLUMN = 0;
    private static final int SEQUENCE_COLUMN = 1;
    private static final int DESTINATION_PORT_COLUMN = 2;

    private static final int PREMIUM_RULE_USE_SIM = 1;
    private static final int PREMIUM_RULE_USE_NETWORK = 2;
    private static final int PREMIUM_RULE_USE_BOTH = 3;
    private final AtomicInteger mPremiumSmsRule = new AtomicInteger(PREMIUM_RULE_USE_SIM);
    private final SettingsObserver mSettingsObserver;

    /** New SMS received. */
    protected static final int EVENT_NEW_SMS = 1;

    /** SMS send complete. */
    protected static final int EVENT_SEND_SMS_COMPLETE = 2;

    /** Retry sending a previously failed SMS message */
    private static final int EVENT_SEND_RETRY = 3;

    /** Confirmation required for sending a large number of messages. */
    private static final int EVENT_SEND_LIMIT_REACHED_CONFIRMATION = 4;

    /** Send the user confirmed SMS */
    static final int EVENT_SEND_CONFIRMED_SMS = 5;  // accessed from inner class

    /** Don't send SMS (user did not confirm). */
    static final int EVENT_STOP_SENDING = 7;        // accessed from inner class

    /** Confirmation required for third-party apps sending to an SMS short code. */
    private static final int EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE = 8;

    /** Confirmation required for third-party apps sending to an SMS short code. */
    private static final int EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE = 9;

    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /** Status report received */
    // static final protected int EVENT_NEW_SMS_STATUS_REPORT = 8;

    /** New broadcast SMS */
    static final protected int EVENT_NEW_BROADCAST_SMS = 101;

    /** Activate/Inactivate Cell Broadcast complete */
    static final protected int EVENT_ACTIVATE_CB_COMPLETE = 102;

    /** Get Cell Broadcast Configuration complete */
    static final protected int EVENT_GET_CB_CONFIG_COMPLETE = 103;

    /** Set Cell Broadcast Configuration complete */
    static final protected int EVENT_SET_CB_CONFIG_COMPLETE = 104;

    /** Get Cell Broadcast Configuration complete */
    static final protected int EVENT_QUERY_CB_ACTIVATION_COMPLETE = 106;

    /** SMS subsystem in the modem is ready */
    static final protected int EVENT_SMS_READY = 107;

    /** reducted message handling */
    static final protected int EVENT_HANDLE_REDUCTED_MESSAGE = 108;
    static final protected int EVENT_REDUCTED_MESSAGE_TIMEOUT = 109;

    /** copy text message to the ICC card */
    static final protected int EVENT_COPY_TEXT_MESSAGE_DONE = 110;
    // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    protected final Phone mPhone;
    protected final Context mContext;
    protected final ContentResolver mResolver;
    protected final CommandsInterface mCm;
    protected final SmsStorageMonitor mStorageMonitor;
    protected final TelephonyManager mTelephonyManager;

    protected final WapPushOverSms mWapPush;

    protected static final Uri mRawUri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, "raw");
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    protected static final Uri mCbRawUri = Uri.withAppendedPath(Uri.parse("content://cb"), "cbraw");
    // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    /** Maximum number of times to retry sending a failed SMS. */
    private static final int MAX_SEND_RETRIES = 3;
    /** Delay before next send attempt on a failed SMS, in milliseconds. */
    private static final int SEND_RETRY_DELAY = 2000;
    /** single part SMS */
    private static final int SINGLE_PART_SMS = 1;
    /** Message sending queue limit */
    private static final int MO_MSG_QUEUE_LIMIT = 5;

    /**
     * Message reference for a CONCATENATED_8_BIT_REFERENCE or
     * CONCATENATED_16_BIT_REFERENCE message set.  Should be
     * incremented for each set of concatenated messages.
     * Static field shared by all dispatcher objects.
     */
    private static int sConcatenatedRef = new Random().nextInt(256);

    /** Outgoing message counter. Shared by all dispatchers. */
    private final SmsUsageMonitor mUsageMonitor;

    /** Number of outgoing SmsTrackers waiting for user confirmation. */
    private int mPendingTrackerCount;

    /**
     * This list is used to maintain the unsent Sms Tracker
     * we have this queue list to avoid we send a lot of SEND_SMS request to RIL
     * and block other commands.
     * So we only send the next SEND_SMS request after the previously request has been completed
     */
    protected ArrayList<SmsTracker> mSTrackersQueue = new ArrayList<SmsTracker>(MO_MSG_QUEUE_LIMIT);

    /** Wake lock to ensure device stays awake while dispatching the SMS intent. */
    private PowerManager.WakeLock mWakeLock;

    /**
     * Hold the wake lock for 5 seconds, which should be enough time for
     * any receiver(s) to grab its own wake lock.
     */
    private static final int WAKE_LOCK_TIMEOUT = 5000;

    /* Flags indicating whether the current device allows sms service */
    protected boolean mSmsCapable = true;
    protected boolean mSmsReceiveDisabled;
    protected boolean mSmsSendDisabled;

    protected int mRemainingMessages = -1;

    protected static int getNextConcatenatedRef() {
        sConcatenatedRef += 1;
        return sConcatenatedRef;
    }
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    // MTK_OPTR_PROTECT_START
    static final protected String DM_OA = "10654040";
    static final protected int DM_PORT = 16998;

    protected DMOperatorFile mDMOperatorFile = null;

    protected static boolean isDmLock = false;
    // MTK_OPTR_PROTECT_END

    /** Default checking period for SMS sent without user permit */
    private static final int DEFAULT_SMS_CHECK_PERIOD = 3600000;

    /** Default number of SMS sent in checking period without user permit */
    private static final int DEFAULT_SMS_MAX_COUNT = 100;

    // flag of storage status
    /** For FTA test only */
    protected boolean mStorageAvailable = true;

    protected int mSimId = PhoneConstants.GEMINI_SIM_1;

    protected boolean mSmsReady = false;

    // for copying text message to ICC card
    protected int messageCountNeedCopy = 0;
    protected Object mLock = new Object();
    protected boolean mSuccess = true;

    // for Netqin SMS checking
    private static SmsHeader.ConcatRef sConcatRef = null;
    private static boolean sRefuseSent = true;
    private static int sConcatMsgCount = 0;

    // for auto push service
    private static final int WAP_PUSH_NOTI_ID = 4999;
    private static final String ACTION_WAP_PUSH_NOTI_CANCEL = "com.mediatek.cu_wap_push_permission_cancel";
    private static final int DELAY_NOTI_TIME = 15 * 1000;
    private static final String PACKAGE_NAME_SETTINGS = "com.mediatek.gemini";

    protected static String PDU_SIZE = "pdu_size";
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    
    IWapPushFwkExt mWapPushFwkExt = null;

    protected IDupSmsFilterExt mDupSmsFilterExt = null;
    IConcatenatedSmsFwkExt mConcatenatedSmsFwkExt = null;
    
    protected ICellBroadcastFwkExt mCellBroadcastFwkExt = null;
    protected PendingIntent mEtwsAlarmIntent = null;
    protected static final String INTENT_ETWS_ALARM =
        "com.android.internal.telephony.etws";

    /**
     * Create a new SMS dispatcher.
     * @param phone the Phone to use
     * @param storageMonitor the SmsStorageMonitor to use
     * @param usageMonitor the SmsUsageMonitor to use
     */
    protected SMSDispatcher(PhoneBase phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor) {
        mPhone = phone;
        mWapPush = new WapPushOverSms(phone, this);
        mContext = phone.getContext();
        mResolver = mContext.getContentResolver();
        mCm = phone.mCM;
        mStorageMonitor = storageMonitor;
        mUsageMonitor = usageMonitor;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mSettingsObserver = new SettingsObserver(this, mPremiumSmsRule, mContext);
        mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.SMS_SHORT_CODE_RULE), false, mSettingsObserver);

        createWakelock();

        mSmsCapable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sms_capable);
        mSmsReceiveDisabled = !SystemProperties.getBoolean(
                                TelephonyProperties.PROPERTY_SMS_RECEIVE, mSmsCapable);
        mSmsSendDisabled = !SystemProperties.getBoolean(
                                TelephonyProperties.PROPERTY_SMS_SEND, mSmsCapable);
        Log.d(TAG, "SMSDispatcher: ctor mSmsCapable=" + mSmsCapable + " format=" + getFormat()
                + " mSmsReceiveDisabled=" + mSmsReceiveDisabled
                + " mSmsSendDisabled=" + mSmsSendDisabled);
        
        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        mCm.setOnNewSMS(this, EVENT_NEW_SMS, null);
        mCm.registerForSmsReady(this, EVENT_SMS_READY, null);

        mSimId = mPhone.getMySimId();

        // Register for device storage intents.  Use these to notify the RIL
        // that storage for SMS is or is not available.
        // TODO: Revisit this for a later release.  Storage reporting should
        // rely more on application indication.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_FULL);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_NOT_FULL);
        filter.addAction(INTENT_ETWS_ALARM);
        //filter.addAction(ACTION_WAP_PUSH_NOTI_CANCEL);
        //filter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        mContext.registerReceiver(mResultReceiver, filter);

        // MTK_OPTR_PROTECT_START
        // initialize DM operator info
        Log.d(TAG, "[DM initialize DM xml");
        mDMOperatorFile = DMOperatorFile.getInstance();
        mDMOperatorFile.initFromRes(mContext);
        mDMOperatorFile.dump();

        // register DM broadcast receiver
        IntentFilter dmFilter = new IntentFilter();
        dmFilter.addAction("com.mediatek.dm.LAWMO_LOCK");
        dmFilter.addAction("com.mediatek.dm.LAWMO_UNLOCK");
        mContext.registerReceiver(mDMLockReceiver, dmFilter);
        /*
        try {
            IBinder binder = ServiceManager.getService("DMAgent");
            DMAgent dmAgent = DMAgent.Stub.asInterface (binder);
            if(dmAgent != null) {
                isDmLock = dmAgent.isLockFlagSet();
            }
            Log.d(TAG, "DM is lock: " + isDmLock);
        } catch (RemoteException ex) {
            Log.d(TAG, "Fail to obtain DMAgent");
            ex.printStackTrace();
        }
        */
        // MTK_OPTR_PROTECT_END
        // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        mWapPushFwkExt = MediatekClassFactory.createInstance(IWapPushFwkExt.class, mContext, mSimId);
        if (mWapPushFwkExt != null) {
            String actualClassName = mWapPushFwkExt.getClass().getName();
            Log.d(TAG, "initial IWapPushFwkExt done, actual class name is " + actualClassName);
        } else {
            Log.d(TAG, "FAIL! initial IWapPushFwkExt");
        }

        mDupSmsFilterExt = MediatekClassFactory.createInstance(IDupSmsFilterExt.class, mContext, mSimId);
        if (mDupSmsFilterExt != null) {
            String actualClassName = mDupSmsFilterExt.getClass().getName();
            Log.d(TAG, "initial IDupSmsFilterExt done, actual class name is " + actualClassName);
        } else {
            Log.d(TAG, "FAIL! intial IDupSmsFilterExt");
        }

        mConcatenatedSmsFwkExt = new ConcatenatedSmsFwkExt(mContext, mSimId);
        
        mCellBroadcastFwkExt = new CellBroadcastFwkExt(phone);
    }

    /**
     * Observe the secure setting for updated premium sms determination rules
     */
    private static class SettingsObserver extends ContentObserver {
        private final AtomicInteger mPremiumSmsRule;
        private final Context mContext;
        SettingsObserver(Handler handler, AtomicInteger premiumSmsRule, Context context) {
            super(handler);
            mPremiumSmsRule = premiumSmsRule;
            mContext = context;
            onChange(false); // load initial value;
        }

        @Override
        public void onChange(boolean selfChange) {
            mPremiumSmsRule.set(Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.SMS_SHORT_CODE_RULE, PREMIUM_RULE_USE_SIM));
        }
    }

    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /** Unregister for incoming SMS events. */
    public void dispose() {
        mCm.unSetOnNewSMS(this);
        mCm.unSetOnSmsStatus(this);
        mCm.unregisterForOn(this);
    }
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    
    /**
     * The format of the message PDU in the associated broadcast intent.
     * This will be either "3gpp" for GSM/UMTS/LTE messages in 3GPP format
     * or "3gpp2" for CDMA/LTE messages in 3GPP2 format.
     *
     * Note: All applications which handle incoming SMS messages by processing the
     * SMS_RECEIVED_ACTION broadcast intent MUST pass the "format" extra from the intent
     * into the new methods in {@link android.telephony.SmsMessage} which take an
     * extra format parameter. This is required in order to correctly decode the PDU on
     * devices which require support for both 3GPP and 3GPP2 formats at the same time,
     * such as CDMA/LTE devices and GSM/CDMA world phones.
     *
     * @return the format of the message PDU
     */
    protected abstract String getFormat();

    @Override
    protected void finalize() throws Throwable {
        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        super.finalize();
        // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        Log.d(TAG, "SMSDispatcher finalized");
    }


    /* TODO: Need to figure out how to keep track of status report routing in a
     *       persistent manner. If the phone process restarts (reboot or crash),
     *       we will lose this list and any status reports that come in after
     *       will be dropped.
     */
    /** Sent messages awaiting a delivery status report. */
    protected final ArrayList<SmsTracker> deliveryPendingList = new ArrayList<SmsTracker>();

    /**
     * Handles events coming from the phone stack. Overridden from handler.
     *
     * @param msg the message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
        case EVENT_NEW_SMS:
            // A new SMS has been received by the device
            if (false) {
                Log.d(TAG, "New SMS Message Received");
            }

            SmsMessage sms;

            ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                Log.e(TAG, "Exception processing incoming SMS. Exception:" + ar.exception);
                return;
            }

            sms = (SmsMessage) ar.result;
            /*
            try {
                int result = dispatchMessage(sms.mWrappedSmsMessage);
                if (result != Activity.RESULT_OK) {
                    // RESULT_OK means that message was broadcast for app(s) to handle.
                    // Any other result, we should ack here.
                    boolean handled = (result == Intents.RESULT_SMS_HANDLED);
                    notifyAndAcknowledgeLastIncomingSms(handled, result, null);
                }
            } catch (RuntimeException ex) {
                Log.e(TAG, "Exception dispatching message", ex);
                notifyAndAcknowledgeLastIncomingSms(false, Intents.RESULT_SMS_GENERIC_ERROR, null);
            }*/
            handleNewSms(sms);

            break;

        case EVENT_SEND_SMS_COMPLETE:
            // An outbound SMS has been successfully transferred, or failed.
            handleSendComplete((AsyncResult) msg.obj);
            break;

        case EVENT_SEND_RETRY:
            sendSms((SmsTracker) msg.obj);
            break;

        case EVENT_SEND_LIMIT_REACHED_CONFIRMATION:
            handleReachSentLimit((SmsTracker)(msg.obj));
            break;

        case EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE:
            handleConfirmShortCode(false, (SmsTracker)(msg.obj));
            break;

        case EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE:
            handleConfirmShortCode(true, (SmsTracker)(msg.obj));
            break;

        case EVENT_SEND_CONFIRMED_SMS:
        {
            SmsTracker tracker = (SmsTracker) msg.obj;
            if (tracker.isMultipart()) {
                sendMultipartSms(tracker);
            } else {
                sendSms(tracker);
            }
            mPendingTrackerCount--;
            break;
        }

        case EVENT_STOP_SENDING:
        {
            SmsTracker tracker = (SmsTracker) msg.obj;
            if (tracker.mSentIntent != null) {
                try {
                    tracker.mSentIntent.send(RESULT_ERROR_LIMIT_EXCEEDED);
                } catch (CanceledException ex) {
                    Log.e(TAG, "failed to send RESULT_ERROR_LIMIT_EXCEEDED");
                }
            }
            mPendingTrackerCount--;
            break;
        }

        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        case EVENT_ACTIVATE_CB_COMPLETE:
        case EVENT_GET_CB_CONFIG_COMPLETE:
        case EVENT_SET_CB_CONFIG_COMPLETE:
            ar = (AsyncResult) msg.obj;
            AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
            ((Message) ar.userObj).sendToTarget();            
            break;
            
        case EVENT_QUERY_CB_ACTIVATION_COMPLETE:
            handleQueryCbActivation((AsyncResult) msg.obj);
            break;
            
        case EVENT_SMS_READY:
            Log.d(TAG, "SMS is ready, SIM: " + mSimId);
            mSmsReady = true;

            notifySmsReady(mSmsReady);
            break;
            
        case EVENT_COPY_TEXT_MESSAGE_DONE:
            ar = (AsyncResult)msg.obj;
            synchronized (mLock) {
                mSuccess = (ar.exception == null);

                if(mSuccess == true) {
                    Log.d(TAG, "[copyText success to copy one");
                    messageCountNeedCopy -= 1;
                } else {
                    Log.d(TAG, "[copyText fail to copy one");
                    messageCountNeedCopy = 0;
                }
                
                mLock.notifyAll();
            }
            break;
            
        case EVENT_HANDLE_REDUCTED_MESSAGE:
            handleDeductedMessage((SmsTracker)(msg.obj));
            break;
            
        case EVENT_REDUCTED_MESSAGE_TIMEOUT:
            SmsTracker tracker = (SmsTracker) msg.obj;

            if (tracker != null) {
                try {
                    if(tracker.mSentIntent != null) {
                        tracker.mSentIntent.send(RESULT_ERROR_LIMIT_EXCEEDED);
                    }
                } catch (CanceledException ex) {
                    Log.e(TAG, "failed to send back RESULT_ERROR_LIMIT_EXCEEDED");
                }
            }
            
            while(sConcatMsgCount > 0 && mPendingTrackerCount > 0) {
                sConcatMsgCount -= 1;
            }
            break;

        // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
            
        case IConcatenatedSmsFwkExt.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS:
            Log.d(TAG, "ConcatenatedSmsFwkExt: receive timeout message");
            if (msg.obj == null) {
                Log.d(TAG, "ConcatenatedSmsFwkExt: null TimerRecord in msg");
                return;
            }

            TimerRecord record = (TimerRecord) msg.obj;
            if (record == null) {
                Log.d(TAG, "ConcatenatedSmsFwkExt: null TimerRecord in msg 2");
                return;
            }
            Log.d(TAG,
                    "ConcatenatedSmsFwkExt: timer is expired, dispatch existed segments. refNumber = "
                            + record.refNumber);
            byte[][] pdus = mConcatenatedSmsFwkExt.queryExistedSegments(record);
            if (pdus != null && pdus.length > 0) {
                dispatchPdus(pdus);
            } else {
                Log.d(TAG, "ConcatenatedSmsFwkExt: no pdus to be dispatched");
            }
            Log.d(TAG, "ConcatenatedSmsFwkExt: delete segment(s), ref = "
                    + record.refNumber);
            mConcatenatedSmsFwkExt.deleteExistedSegments(record);
            break;
        }
    }

    private void createWakelock() {
        PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMSDispatcher");
        mWakeLock.setReferenceCounted(true);
    }

    /**
     * Grabs a wake lock and sends intent as an ordered broadcast.
     * The resultReceiver will check for errors and ACK/NACK back
     * to the RIL.
     *
     * @param intent intent to broadcast
     * @param permission Receivers are required to have this permission
     */
    public void dispatch(Intent intent, String permission) {
        // Hold a wake lock for WAKE_LOCK_TIMEOUT seconds, enough to give any
        // receivers time to take their own wake locks.
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        intent.putExtra("rTime", System.currentTimeMillis());
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        mContext.sendOrderedBroadcast(intent, permission, mResultReceiver,
                this, Activity.RESULT_OK, null, null);
    }

    /**
     * Grabs a wake lock and sends intent as an ordered broadcast.
     * Used for setting a custom result receiver for CDMA SCPD.
     *
     * @param intent intent to broadcast
     * @param permission Receivers are required to have this permission
     * @param resultReceiver the result receiver to use
     */
    public void dispatch(Intent intent, String permission, BroadcastReceiver resultReceiver) {
        // Hold a wake lock for WAKE_LOCK_TIMEOUT seconds, enough to give any
        // receivers time to take their own wake locks.
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        mContext.sendOrderedBroadcast(intent, permission, resultReceiver,
                this, Activity.RESULT_OK, null, null);
    }

    /**
     * Called when SMS send completes. Broadcasts a sentIntent on success.
     * On failure, either sets up retries or broadcasts a sentIntent with
     * the failure in the result code.
     *
     * @param ar AsyncResult passed into the message handler.  ar.result should
     *           an SmsResponse instance if send was successful.  ar.userObj
     *           should be an SmsTracker instance.
     */
    protected void handleSendComplete(AsyncResult ar) {
        SmsTracker tracker = (SmsTracker) ar.userObj;
        PendingIntent sentIntent = tracker.mSentIntent;

        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        int szPdu = 0;
        if(tracker != null) {
            HashMap map = tracker.mData;
            if(map != null) {
                int smscLength = (map.get("smsc") == null) ? 0 : (((byte[])map.get("smsc")).length);
                int pduLength = (map.get("pdu") == null) ? 0 : (((byte[])map.get("pdu")).length);
                szPdu = smscLength + pduLength;
            }
        }
        synchronized (mSTrackersQueue) {
            // remove the first tracker and send the next one if any
            Log.d(TAG, "Remove Tracker");
            SmsTracker tempTracker = (!mSTrackersQueue.isEmpty()) ? mSTrackersQueue.remove(0) : null;
            if(tempTracker != null && tempTracker.equals(tracker)) {
                Log.d(TAG, "[pdu size: " + szPdu);
            }

            if (!mSTrackersQueue.isEmpty()) {
                SmsTracker sendtracker = mSTrackersQueue.get(0);

                sendSms(sendtracker);
            }
        }
        // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

        if (ar.exception == null) {
            if (Config.DEBUG) {
                Log.d(TAG, "SMS send complete. Broadcasting "
                        + "intent: " + sentIntent);
            }

            if (tracker.mDeliveryIntent != null) {
                // Expecting a status report.  Add it to the list.
                int messageRef = ((SmsResponse)ar.result).messageRef;
                tracker.mMessageRef = messageRef;
                deliveryPendingList.add(tracker);
            }

            if (sentIntent != null) {
                try {
                    if (mRemainingMessages > -1) {
                        mRemainingMessages--;
                    }

                    if (mRemainingMessages == 0) {
                        Intent sendNext = new Intent();
                        sendNext.putExtra(SEND_NEXT_MSG_EXTRA, true);
                        sendNext.putExtra(PDU_SIZE, szPdu);
                        sentIntent.send(mContext, Activity.RESULT_OK, sendNext);
                    } else {
                        // sentIntent.send(Activity.RESULT_OK);
                        Intent fillIn = new Intent();
                        fillIn.putExtra(PDU_SIZE, szPdu);
                        sentIntent.send(mContext, Activity.RESULT_OK, fillIn);
                    }
                } catch (CanceledException ex) {
                    // MTK-START Add log for Canceled Exception
                    Log.d(TAG, "CanceledException happened when send sms success with sentIntent");
                    // MTK-END  Add log for Canceled Exception
                }
            // MTK-START Add log if caller does not contain send intent
            } else {
                Log.d(TAG, "Send sms success without sentIntent");
            }
            // MTK-END Add log if caller does not contain send intent
        } else {
            Log.d(TAG, "SMS send failed");
            
            // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
            // for ALPS00044719
            boolean isTestIccCard = false;
            try {
                ITelephony telephony = ITelephony.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE));
                if (telephony != null) {
                    isTestIccCard = telephony.isTestIccCard();
                }
            } catch (RemoteException ex) {
                // This shouldn't happen in the normal case
                Log.d(TAG, "SD-handleSendComplete: RemoteException: " + ex.getMessage());
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                Log.d(TAG, "SD-handleSendComplete: NullPointerException: " + ex.getMessage());
            }

            Log.d(TAG, "SD-handleSendComplete: SIM" + mSimId + " isTestIccCard " + isTestIccCard);
            // for ALPS00044719
            // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

            int ss = mPhone.getServiceState().getState();

            if (ss != ServiceState.STATE_IN_SERVICE) {
                Log.d(TAG, "handleSendComplete: No service");
                handleNotInService(ss, tracker.mSentIntent);
            /* MTK-START: No need to retry due to modem has already retry it */
//          } else if ((((CommandException)(ar.exception)).getCommandError()
//                  == CommandException.Error.SMS_FAIL_RETRY) &&
//                 tracker.mRetryCount < MAX_SEND_RETRIES) {
//              // Retry after a delay if needed.
//              // TODO: According to TS 23.040, 9.2.3.6, we should resend
//              //       with the same TP-MR as the failed message, and
//              //       TP-RD set to 1.  However, we don't have a means of
//              //       knowing the MR for the failed message (EF_SMSstatus
//              //       may or may not have the MR corresponding to this
//              //       message, depending on the failure).  Also, in some
//              //       implementations this retry is handled by the baseband.
//              tracker.mRetryCount++;
//              Message retryMsg = obtainMessage(EVENT_SEND_RETRY, tracker);
//              sendMessageDelayed(retryMsg, SEND_RETRY_DELAY);
            /* MTK-END: No need to retry due to modem has already retry it */
            } else if (tracker.mSentIntent != null) {
                int error = RESULT_ERROR_GENERIC_FAILURE;

                if (((CommandException)(ar.exception)).getCommandError()
                        == CommandException.Error.FDN_CHECK_FAILURE) {
                    error = RESULT_ERROR_FDN_CHECK_FAILURE;
                }
                // Done retrying; return an error to the app.
                try {
                    Intent fillIn = new Intent();
                    // add pdu size 
                    fillIn.putExtra(PDU_SIZE, szPdu);
                    if (ar.result != null) {
                        fillIn.putExtra("errorCode", ((SmsResponse)ar.result).errorCode);
                    }
                    if (mRemainingMessages > -1) {
                        mRemainingMessages--;
                    }

                    if (mRemainingMessages == 0) {
                        fillIn.putExtra(SEND_NEXT_MSG_EXTRA, true);
                    }

                    tracker.mSentIntent.send(mContext, error, fillIn);
                } catch (CanceledException ex) {
                    // MTK-START Add log for Canceled Exception
                    Log.d(TAG, "CanceledException happened when send sms fail with sentIntent");
                    // MTK-END  Add log for Canceled Exception
                }
            // MTK-START Add log if caller does not contain send intent
            } else {
                Log.d(TAG, "Send sms fail without sentIntent");
            }
            // MTK-END Add log if caller does not contain send intent
        }
    }

    /**
     * Handles outbound message when the phone is not in service.
     *
     * @param ss     Current service state.  Valid values are:
     *                  OUT_OF_SERVICE
     *                  EMERGENCY_ONLY
     *                  POWER_OFF
     * @param sentIntent the PendingIntent to send the error to
     */
    protected static void handleNotInService(int ss, PendingIntent sentIntent) {
        if (sentIntent != null) {
            try {
                if (ss == ServiceState.STATE_POWER_OFF) {
                    sentIntent.send(RESULT_ERROR_RADIO_OFF);
                } else {
                    sentIntent.send(RESULT_ERROR_NO_SERVICE);
                }
            } catch (CanceledException ex) {
                // MTK-START Add log for Canceled Exception
                Log.d(TAG, "CanceledException happened when send sms fail with sentIntent due to no service");
                // MTK-END  Add log for Canceled Exception
            }
        // MTK-START Add log if caller does not contain send intent
        } else {
            Log.d(TAG, "Send sms fail without sentIntent due to no service");
        }
        // MTK-END Add log if caller does not contain send intent
    }

    /**
     * Dispatches an incoming SMS messages.
     *
     * @param sms the incoming message from the phone
     * @return a result code from {@link Telephony.Sms.Intents}, or
     *         {@link Activity#RESULT_OK} if the message has been broadcast
     *         to applications
     */
    public abstract int dispatchMessage(SmsMessageBase sms);

    /**
     * Dispatch a normal incoming SMS. This is called from the format-specific
     * {@link #dispatchMessage(SmsMessageBase)} if no format-specific handling is required.
     *
     * @param sms
     * @return
     */
    protected int dispatchNormalMessage(SmsMessageBase sms) {
        SmsHeader smsHeader = sms.getUserDataHeader();

        // See if message is partial or port addressed.
        if ((smsHeader == null) || (smsHeader.concatRef == null)) {
            // Message is not partial (not part of concatenated sequence).
            byte[][] pdus = new byte[1][];
            pdus[0] = sms.getPdu();

            if (smsHeader != null && smsHeader.portAddrs != null) {
                /*
                if(isCuVersion() == true &&
                   (allowDispatchWapPush(mSimId) == false) &&
                   (isMmsWapPush(sms.getUserData()) == false)) {
                    // impl
                    Log.d(TAG, "don't dispatch push message");
                    return Intents.RESULT_SMS_HANDLED;
                }
                */
                boolean allowDispatch = mWapPushFwkExt.allowDispatchWapPush();
                boolean isMms = mWapPushFwkExt.isMmsWapPush(sms.getUserData());
                if (allowDispatch == false && isMms == false) {
                    Log.d(TAG, "don't dispatch push message");
                    return Intents.RESULT_SMS_HANDLED;
                }
                if (smsHeader.portAddrs.destPort == SmsHeader.PORT_WAP_PUSH) {
                    // GSM-style WAP indication
                    if (FeatureOption.MTK_WAPPUSH_SUPPORT) {
                        Log.d(TAG, "dispatch wap push pdu with addr & sc addr");
                        Bundle mBundle = new Bundle();
                        mBundle.putString(Telephony.WapPush.ADDR, sms.getOriginatingAddress());
                        mBundle.putString(Telephony.WapPush.SERVICE_ADDR, sms.getServiceCenterAddress());

                        return mWapPush.dispatchWapPdu(sms.getUserData(), mBundle);
                    } else {
                        Log.d(TAG, "dispatch wap push pdu");
                        return mWapPush.dispatchWapPdu(sms.getUserData());
                    }
                } else {
                    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                    // MTK_OPTR_PROTECT_START
                    if(mDMOperatorFile.searchMatchOp(sms.getOriginatingAddress(),smsHeader.portAddrs.destPort)) {
                        Log.d(TAG, "we receive a DM register SMS");
                        dispatchDmRegisterPdus(pdus);
                    }
                    else {
                    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                    // MTK_OPTR_PROTECT_END
                        // The message was sent to a port, so concoct a URI for it.
                        dispatchPortAddressedPdus(pdus, smsHeader.portAddrs.destPort);
                    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                    // MTK_OPTR_PROTECT_START
                    }
                    // MTK_OPTR_PROTECT_END
                    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                }
            } else {
                // Normal short and non-port-addressed message, dispatch it.
                dispatchPdus(pdus);
            }
            return Activity.RESULT_OK;
        } else {
            // Process the message part.
            SmsHeader.ConcatRef concatRef = smsHeader.concatRef;
            SmsHeader.PortAddrs portAddrs = smsHeader.portAddrs;
            return processMessagePart(sms.getPdu(), sms.getOriginatingAddress(),
                    concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount,
                    sms.getTimestampMillis(), (portAddrs != null ? portAddrs.destPort : -1), false);
        }
    }

    /**
     * If this is the last part send the parts out to the application, otherwise
     * the part is stored for later processing. Handles both 3GPP concatenated messages
     * as well as 3GPP2 format WAP push messages processed by
     * {@link com.android.internal.telephony.cdma.CdmaSMSDispatcher#processCdmaWapPdu}.
     *
     * @param pdu the message PDU, or the datagram portion of a CDMA WDP datagram segment
     * @param address the originating address
     * @param referenceNumber distinguishes concatenated messages from the same sender
     * @param sequenceNumber the order of this segment in the message
     *          (starting at 0 for CDMA WDP datagrams and 1 for concatenated messages).
     * @param messageCount the number of segments in the message
     * @param timestamp the service center timestamp in millis
     * @param destPort the destination port for the message, or -1 for no destination port
     * @param isCdmaWapPush true if pdu is a CDMA WDP datagram segment and not an SM PDU
     *
     * @return a result code from {@link Telephony.Sms.Intents}, or
     *         {@link Activity#RESULT_OK} if the message has been broadcast
     *         to applications
     */
    protected int processMessagePart(byte[] pdu, String address, int referenceNumber,
            int sequenceNumber, int messageCount, long timestamp, int destPort,
            boolean isCdmaWapPush) {
        byte[][] pdus = null;
        Cursor cursor = null;
        try {
            // used by several query selection arguments
            String refNumber = Integer.toString(referenceNumber);
            String seqNumber = Integer.toString(sequenceNumber);
            String simId = Integer.toString(mSimId);

            // Check for duplicate message segment
            cursor = mResolver.query(mRawUri, PDU_PROJECTION,
                    "address=? AND reference_number=? AND sequence=? AND sim_id=?",
                    new String[] {address, refNumber, seqNumber, simId}, null);

            // moveToNext() returns false if no duplicates were found
            if (cursor.moveToNext()) {
                Log.w(TAG, "Discarding duplicate message segment from address=" + address
                        + " refNumber=" + refNumber + " seqNumber=" + seqNumber);
                String oldPduString = cursor.getString(PDU_COLUMN);
                byte[] oldPdu = HexDump.hexStringToByteArray(oldPduString);
                if (!Arrays.equals(oldPdu, pdu)) {
                    Log.e(TAG, "Warning: dup message segment PDU of length " + pdu.length
                            + " is different from existing PDU of length " + oldPdu.length);
                }
                return Intents.RESULT_SMS_HANDLED;
            }
            cursor.close();
            
            // check whether the message is the first segment of one concatenated sms
            boolean isFirstSegment = mConcatenatedSmsFwkExt.isFirstConcatenatedSegment(address, referenceNumber);
            if(isCdmaWapPush == false && isFirstSegment == true) {
                Log.d(TAG, "ConcatenatedSmsFwkExt: the first segment, ref = " + referenceNumber);
                Log.d(TAG, "ConcatenatedSmsFwkExt: start a new timer");
                TimerRecord record = new TimerRecord(address, referenceNumber, messageCount);
                if(record == null) {
                    Log.d(TAG, "ConcatenatedSmsFwkExt: fail to new TimerRecord to start timer");
                }
                mConcatenatedSmsFwkExt.startTimer(this, record);
            }

            // not a dup, query for all other segments of this concatenated message
            String where = "address=? AND reference_number=?";
            String[] whereArgs = new String[] {address, refNumber};
            cursor = mResolver.query(mRawUri, PDU_SEQUENCE_PORT_PROJECTION, where, whereArgs, null);

            int cursorCount = cursor.getCount();
            if (cursorCount != messageCount - 1) {
                Log.d(TAG, "ConcatenatedSmsFwkExt: refresh timer, ref = " + referenceNumber);
                TimerRecord record = mConcatenatedSmsFwkExt.queryTimerRecord(address, referenceNumber);
                if(record == null) {
                    Log.d(TAG, "ConcatenatedSmsFwkExt: fail to get TimerRecord to refresh timer");
                }
                mConcatenatedSmsFwkExt.refreshTimer(this, record);
                
                // We don't have all the parts yet, store this one away
                ContentValues values = new ContentValues();
                values.put("date", timestamp);
                values.put("pdu", HexDump.toHexString(pdu));
                values.put("address", address);
                values.put("reference_number", referenceNumber);
                values.put("count", messageCount);
                values.put("sequence", sequenceNumber);
                values.put("sim_id", mSimId);
                if (destPort != -1) {
                    values.put("destination_port", destPort);
                }
                mResolver.insert(mRawUri, values);
                return Intents.RESULT_SMS_HANDLED;
            }
            
            // cancel the timer, because all segments are in place
            Log.d(TAG, "ConcatenatedSmsFwkExt: cancel timer, ref = " + referenceNumber);
            TimerRecord record = mConcatenatedSmsFwkExt.queryTimerRecord(address, referenceNumber);
            if(record == null) {
                Log.d(TAG, "ConcatenatedSmsFwkExt: fail to get TimerRecord to cancel timer");
            }
            mConcatenatedSmsFwkExt.cancelTimer(this, record);

            // All the parts are in place, deal with them
            pdus = new byte[messageCount][];
            for (int i = 0; i < cursorCount; i++) {
                cursor.moveToNext();
                int cursorSequence = cursor.getInt(SEQUENCE_COLUMN);
                // GSM sequence numbers start at 1; CDMA WDP datagram sequence numbers start at 0
                if (!isCdmaWapPush) {
                    cursorSequence--;
                }
                pdus[cursorSequence] = HexDump.hexStringToByteArray(
                        cursor.getString(PDU_COLUMN));

                // Read the destination port from the first segment (needed for CDMA WAP PDU).
                // It's not a bad idea to prefer the port from the first segment for 3GPP as well.
                if (cursorSequence == 0 && !cursor.isNull(DESTINATION_PORT_COLUMN)) {
                    destPort = cursor.getInt(DESTINATION_PORT_COLUMN);
                }
            }
            // This one isn't in the DB, so add it
            // GSM sequence numbers start at 1; CDMA WDP datagram sequence numbers start at 0
            if (isCdmaWapPush) {
                pdus[sequenceNumber] = pdu;
            } else {
                pdus[sequenceNumber - 1] = pdu;
            }

            // Remove the parts from the database
            mResolver.delete(mRawUri, where, whereArgs);
        } catch (SQLException e) {
            Log.e(TAG, "Can't access multipart SMS database", e);
            return Intents.RESULT_SMS_GENERIC_ERROR;
        } finally {
            if (cursor != null) cursor.close();
        }

        // Special handling for CDMA WDP datagrams
        if (isCdmaWapPush) {
            // Build up the data stream
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (int i = 0; i < messageCount; i++) {
                // reassemble the (WSP-)pdu
                output.write(pdus[i], 0, pdus[i].length);
            }
            byte[] datagram = output.toByteArray();

            // Dispatch the PDU to applications
            if (destPort == SmsHeader.PORT_WAP_PUSH) {
                // Handle the PUSH
                return mWapPush.dispatchWapPdu(datagram);
            } else {
                pdus = new byte[1][];
                pdus[0] = datagram;
                // The messages were sent to any other WAP port
                dispatchPortAddressedPdus(pdus, destPort);
                return Activity.RESULT_OK;
            }
        }

        // Dispatch the PDUs to applications
        if (destPort != -1) {
            if (destPort == SmsHeader.PORT_WAP_PUSH) {
                // Build up the data stream
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (int i = 0; i < messageCount; i++) {
                    SmsMessage msg = SmsMessage.createFromPdu(pdus[i], getFormat());
                    if (msg != null) {
                        byte[] data = msg.getUserData();
                        output.write(data, 0, data.length);
                    }
                }

                /*
                if(isCuVersion() == true &&
                    (allowDispatchWapPush(mSimId) == false) &&
                    (isMmsWapPush(output.toByteArray()) == false)) {
                    // impl
                    Log.d(TAG, "don't dispatch push message");
                    return Intents.RESULT_SMS_HANDLED;
                }
                */
                boolean allowDispatch = mWapPushFwkExt.allowDispatchWapPush();
                boolean isMms = mWapPushFwkExt.isMmsWapPush(output.toByteArray());
                if (allowDispatch == false && isMms == false) {
                    Log.d(TAG, "don't dispatch push message");
                    return Intents.RESULT_SMS_HANDLED;
                }

                // Handle the PUSH
                // return mWapPush.dispatchWapPdu(output.toByteArray());
                if (FeatureOption.MTK_WAPPUSH_SUPPORT) {
                    Log.d(TAG, "2 - dispatch wap push pdu with addr & sc addr");
                    SmsMessage sms = SmsMessage.createFromPdu(pdus[0], getFormat());
                    Bundle mBundle = new Bundle();
                    if (sms != null) {
                        mBundle.putString(Telephony.WapPush.ADDR, sms.getOriginatingAddress());
                        mBundle.putString(Telephony.WapPush.SERVICE_ADDR, sms.getServiceCenterAddress());
                    }

                    return mWapPush.dispatchWapPdu(output.toByteArray(), mBundle);
                } else {
                    Log.d(TAG, "2 - dispatch wap push pdu");
                    return mWapPush.dispatchWapPdu(output.toByteArray());
                }
            } else {
                // The messages were sent to a port, so concoct a URI for it
                dispatchPortAddressedPdus(pdus, destPort);
            }
        } else {
            // The messages were not sent to a port
            dispatchPdus(pdus);
        }
        return Activity.RESULT_OK;
    }

    /**
     * Dispatches standard PDUs to interested applications
     *
     * @param pdus The raw PDUs making up the message
     */
    protected void dispatchPdus(byte[][] pdus) {
        Intent intent = new Intent(Intents.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        dispatch(intent, RECEIVE_SMS_PERMISSION);
    }

    /**
     * Dispatches port addressed PDUs to interested applications
     *
     * @param pdus The raw PDUs making up the message
     * @param port The destination port of the messages
     */
    protected void dispatchPortAddressedPdus(byte[][] pdus, int port) {
        Uri uri = Uri.parse("sms://localhost:" + port);
        Intent intent = new Intent(Intents.DATA_SMS_RECEIVED_ACTION, uri);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        
        if (port == 8025 || port == 7275 || port == 7276){
            dispatch(intent, null);   //for AGPS only
            Log.d("MtkAgps","=========== SMSDispatcher: Send SMS For A-GPS SUPL NI ========");
        } else {
            dispatch(intent, RECEIVE_SMS_PERMISSION);
        }
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param destPort the port to deliver the message to
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  <code>RESULT_ERROR_NO_SERVICE</code><br>.
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    protected abstract void sendData(String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent);

    /**
     * Send a text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  <code>RESULT_ERROR_NO_SERVICE</code><br>.
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    protected abstract void sendText(String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent);

    /**
     * Calculate the number of septets needed to encode the message.
     *
     * @param messageBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @return TextEncodingDetails
     */
    protected abstract TextEncodingDetails calculateLength(CharSequence messageBody,
            boolean use7bitOnly);

    /**
     * Send a multi-part text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>
     *   <code>RESULT_ERROR_NO_SERVICE</code>.
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */
    protected void sendMultipartText(String destAddr, String scAddr,
            ArrayList<String> parts, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {

        int refNumber = getNextConcatenatedRef() & 0x00FF;
        int msgCount = parts.size();
        int encoding = SmsConstants.ENCODING_UNKNOWN;

        mRemainingMessages = msgCount;

        TextEncodingDetails[] encodingForParts = new TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; i++) {
            TextEncodingDetails details = calculateLength(parts.get(i), false);
            if (encoding != details.codeUnitSize && 
                (encoding == SmsConstants.ENCODING_UNKNOWN ||
                 encoding == SmsConstants.ENCODING_7BIT)) {
                encoding = details.codeUnitSize;
            }
            encodingForParts[i] = details;
        }

        for (int i = 0; i < msgCount; i++) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = refNumber;
            concatRef.seqNumber = i + 1;  // 1-based sequence
            concatRef.msgCount = msgCount;
            // TODO: We currently set this to true since our messaging app will never
            // send more than 255 parts (it converts the message to MMS well before that).
            // However, we should support 3rd party messaging apps that might need 16-bit
            // references
            // Note:  It's not sufficient to just flip this bit to true; it will have
            // ripple effects (several calculations assume 8-bit ref).
            concatRef.isEightBits = true;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;

            // Set the national language tables for 3GPP 7-bit encoding, if enabled.
            if (encoding == SmsConstants.ENCODING_7BIT) {
                smsHeader.languageTable = encodingForParts[i].languageTable;
                smsHeader.languageShiftTable = encodingForParts[i].languageShiftTable;
            }

            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i) {
                sentIntent = sentIntents.get(i);
            }

            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i) {
                deliveryIntent = deliveryIntents.get(i);
            }

            sendNewSubmitPdu(destAddr, scAddr, parts.get(i), smsHeader, encoding,
                    sentIntent, deliveryIntent, (i == (msgCount - 1)));
        }

    }

    /**
     * Create a new SubmitPdu and send it.
     */
    protected abstract void sendNewSubmitPdu(String destinationAddress, String scAddress,
            String message, SmsHeader smsHeader, int encoding,
            PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart);

    /**
     * Send a SMS
     *
     * @param smsc the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param pdu the raw PDU to send
     * @param sentIntent if not NULL this <code>Intent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *  <code>RESULT_ERROR_RADIO_OFF</code>
     *  <code>RESULT_ERROR_NULL_PDU</code>
     *  <code>RESULT_ERROR_NO_SERVICE</code>.
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>Intent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param destAddr the destination phone number (for short code confirmation)
     */
    protected void sendRawPdu(byte[] smsc, byte[] pdu, PendingIntent sentIntent,
            PendingIntent deliveryIntent, String destAddr) {
        if (mSmsSendDisabled) {
            if (sentIntent != null) {
                try {
                    sentIntent.send(RESULT_ERROR_NO_SERVICE);
                } catch (CanceledException ex) {}
            }
            Log.d(TAG, "Device does not support sending sms.");
            return;
        }

        if (pdu == null) {
            if (sentIntent != null) {
                try {
                    sentIntent.send(RESULT_ERROR_NULL_PDU);
                } catch (CanceledException ex) {}
            }
            return;
        }

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("smsc", smsc);
        map.put("pdu", pdu);

        // Get calling app package name via UID from Binder call
        PackageManager pm = mContext.getPackageManager();
        String[] packageNames = pm.getPackagesForUid(Binder.getCallingUid());

        if (packageNames == null || packageNames.length == 0) {
            // Refuse to send SMS if we can't get the calling package name.
            Log.e(TAG, "Can't get calling app package name: refusing to send SMS");
            if (sentIntent != null) {
                try {
                    sentIntent.send(RESULT_ERROR_GENERIC_FAILURE);
                } catch (CanceledException ex) {
                    Log.e(TAG, "failed to send error result");
                }
            }
            return;
        }

        // MTK-START
        /* Because it may have multiple apks use the same uid, ex. Mms.apk and omacp.apk, we need to 
         * exactly find the correct calling apk. We should use running process to check the correct
         * apk. If we could not find the process via pid, this apk may be killed. We will use the 
         * default behavior, find the first package name via uid. 
         */
        if (packageNames.length > 1)
        {
            int callingPid = Binder.getCallingPid();

            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List processList = am.getRunningAppProcesses();
            Iterator index = processList.iterator();
            while (index.hasNext())
            {
                ActivityManager.RunningAppProcessInfo processInfo = (ActivityManager.RunningAppProcessInfo)(index.next());
                if (callingPid == processInfo.pid)
                {
                    packageNames[0] = processInfo.processName;
                    break;
                }
            }
        }
        // MTK-END

        // Get package info via packagemanager
        PackageInfo appInfo = null;
        try {
            // XXX this is lossy- apps can share a UID
            appInfo = pm.getPackageInfo(packageNames[0], PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't get calling app package info: refusing to send SMS");
            if (sentIntent != null) {
                try {
                    sentIntent.send(RESULT_ERROR_GENERIC_FAILURE);
                } catch (CanceledException ex) {
                    Log.e(TAG, "failed to send error result");
                }
            }
            return;
        }

        // Strip non-digits from destination phone number before checking for short codes
        // and before displaying the number to the user if confirmation is required.
        SmsTracker tracker = new SmsTracker(map, sentIntent, deliveryIntent, appInfo,
                PhoneNumberUtils.extractNetworkPortion(destAddr));

        // checkDestination() returns true if the destination is not a premium short code or the
        // sending app is approved to send to short codes. Otherwise, a message is sent to our
        // handler with the SmsTracker to request user confirmation before sending.
        if (checkDestination(tracker)) {
            // check for excessive outgoing SMS usage by this app
            if (!mUsageMonitor.check(appInfo.packageName, SINGLE_PART_SMS)) {
                sendMessage(obtainMessage(EVENT_SEND_LIMIT_REACHED_CONFIRMATION, tracker));
                return;
            }

            int ss = mPhone.getServiceState().getState();

            if (ss != ServiceState.STATE_IN_SERVICE) {
                handleNotInService(ss, tracker.mSentIntent);
            } else {
                String appName = getAppNameByIntent(sentIntent);
                // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                if(FeatureOption.MTK_SMS_FILTER_SUPPORT == true) {
                    SmsMessage msg = createMessageFromSubmitPdu(smsc, pdu);
                    if(msg != null) {
                        boolean ret = checkSmsWithNqFilter(msg.getDestinationAddress(), msg.getMessageBody(), sentIntent);
                        if(ret == false) {
                            Log.d(TAG, "[NQ this message is safe");
                            if (mUsageMonitor.check(appName, SINGLE_PART_SMS)) {
                                sendSms(tracker);
                            } else {
                                sendMessage(obtainMessage(EVENT_SEND_LIMIT_REACHED_CONFIRMATION, tracker));
                            }
                        } else {
                            Log.d(TAG, "[NQ this message may deduct fees");
                        
                            SmsHeader.ConcatRef newConcatRef = null;
                            if(msg.getUserDataHeader() != null) {
                                newConcatRef = msg.getUserDataHeader().concatRef;
                            }
                        
                            if(newConcatRef != null) {
                                if(sConcatRef == null || sConcatRef.refNumber != newConcatRef.refNumber) {
                                    Log.d(TAG, "[NQ this is a new concatenated message, just update");
                                    sConcatRef = newConcatRef;
                                    //sConcatMsgCount = 1;
                                    sendMessage(obtainMessage(EVENT_HANDLE_REDUCTED_MESSAGE, tracker));
                                } else {
                                    Log.d(TAG, "[NQ this is the same concatenated message, keep previous operation");
                                    //mSTrackers.add(tracker);
                                    sConcatMsgCount += 1;
                                }
                            } else {
                                Log.d(TAG, "[NQ this is a non-concatenated message");
                                //sConcatMsgCount = 0;
                                sendMessage(obtainMessage(EVENT_HANDLE_REDUCTED_MESSAGE, tracker));
                            }
                        }
                    } else {
                        Log.d(TAG, "[NQ fail to create message from pdu");
                        if (mUsageMonitor.check(appName, SINGLE_PART_SMS)) {
                            sendSms(tracker);
                        } else {
                            sendMessage(obtainMessage(EVENT_SEND_LIMIT_REACHED_CONFIRMATION, tracker));
                        }
                    }
                } else {
                // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                    if (mUsageMonitor.check(appName, SINGLE_PART_SMS)) {
                        sendSms(tracker);
                    } else {
                        sendMessage(obtainMessage(EVENT_SEND_LIMIT_REACHED_CONFIRMATION, tracker));
                    }
                // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                }
                // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
            }
        }
    }

    /**
     * Check if destination is a potential premium short code and sender is not pre-approved to
     * send to short codes.
     *
     * @param tracker the tracker for the SMS to send
     * @return true if the destination is approved; false if user confirmation event was sent
     */
    boolean checkDestination(SmsTracker tracker) {
        if (mContext.checkCallingOrSelfPermission(SEND_SMS_NO_CONFIRMATION_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            return true;            // app is pre-approved to send to short codes
        } else {
            int rule = mPremiumSmsRule.get();
            int smsCategory = SmsUsageMonitor.CATEGORY_NOT_SHORT_CODE;
            if (rule == PREMIUM_RULE_USE_SIM || rule == PREMIUM_RULE_USE_BOTH) {
                String simCountryIso = mTelephonyManager.getSimCountryIso();
                if (simCountryIso == null || simCountryIso.length() != 2) {
                    Log.e(TAG, "Can't get SIM country Iso: trying network country Iso");
                    simCountryIso = mTelephonyManager.getNetworkCountryIso();
                }

                smsCategory = mUsageMonitor.checkDestination(tracker.mDestAddress, simCountryIso);
            }
            if (rule == PREMIUM_RULE_USE_NETWORK || rule == PREMIUM_RULE_USE_BOTH) {
                String networkCountryIso = mTelephonyManager.getNetworkCountryIso();
                if (networkCountryIso == null || networkCountryIso.length() != 2) {
                    Log.e(TAG, "Can't get Network country Iso: trying SIM country Iso");
                    networkCountryIso = mTelephonyManager.getSimCountryIso();
                }

                smsCategory = mUsageMonitor.mergeShortCodeCategories(smsCategory,
                        mUsageMonitor.checkDestination(tracker.mDestAddress, networkCountryIso));
            }

            if (smsCategory == SmsUsageMonitor.CATEGORY_NOT_SHORT_CODE
                    || smsCategory == SmsUsageMonitor.CATEGORY_FREE_SHORT_CODE
                    || smsCategory == SmsUsageMonitor.CATEGORY_STANDARD_SHORT_CODE) {
                return true;    // not a premium short code
            }

            // Wait for user confirmation unless the user has set permission to always allow/deny
            int premiumSmsPermission = mUsageMonitor.getPremiumSmsPermission(
                    tracker.mAppInfo.packageName);
            if (premiumSmsPermission == SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN) {
                // First time trying to send to premium SMS.
                premiumSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER;
            }

            switch (premiumSmsPermission) {
                case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW:
                    Log.d(TAG, "User approved this app to send to premium SMS");
                    return true;

                case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_NEVER_ALLOW:
                    Log.w(TAG, "User denied this app from sending to premium SMS");
                    sendMessage(obtainMessage(EVENT_STOP_SENDING, tracker));
                    return false;   // reject this message

                case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER:
                default:
                    int event;
                    if (smsCategory == SmsUsageMonitor.CATEGORY_POSSIBLE_PREMIUM_SHORT_CODE) {
                        event = EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE;
                    } else {
                        event = EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE;
                    }
                    sendMessage(obtainMessage(event, tracker));
                    return false;   // wait for user confirmation
            }
        }
    }

    /**
     * Deny sending an SMS if the outgoing queue limit is reached. Used when the message
     * must be confirmed by the user due to excessive usage or potential premium SMS detected.
     * @param tracker the SmsTracker for the message to send
     * @return true if the message was denied; false to continue with send confirmation
     */
    private boolean denyIfQueueLimitReached(SmsTracker tracker) {
        if (mPendingTrackerCount >= MO_MSG_QUEUE_LIMIT) {
            // Deny sending message when the queue limit is reached.
            try {
                tracker.mSentIntent.send(RESULT_ERROR_LIMIT_EXCEEDED);
            } catch (CanceledException ex) {
                Log.e(TAG, "failed to send back RESULT_ERROR_LIMIT_EXCEEDED");
            }
            return true;
        }
        mPendingTrackerCount++;
        return false;
    }

    /**
     * Returns the label for the specified app package name.
     * @param appPackage the package name of the app requesting to send an SMS
     * @return the label for the specified app, or the package name if getApplicationInfo() fails
     */
    private CharSequence getAppLabel(String appPackage) {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(appPackage, 0);
            return appInfo.loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager Name Not Found for package " + appPackage);
            return appPackage;  // fall back to package name if we can't get app label
        }
    }

    /**
     * Post an alert when SMS needs confirmation due to excessive usage.
     * @param tracker an SmsTracker for the current message.
     */
    protected void handleReachSentLimit(SmsTracker tracker) {
        if (denyIfQueueLimitReached(tracker)) {
            return;     // queue limit reached; error was returned to caller
        }

        CharSequence appLabel = getAppLabel(tracker.mAppInfo.packageName);
        Resources r = Resources.getSystem();
        Spanned messageText = Html.fromHtml(r.getString(R.string.sms_control_message, appLabel));

        ConfirmDialogListener listener = new ConfirmDialogListener(tracker, null);

        AlertDialog d = new AlertDialog.Builder(mContext)
                .setTitle(R.string.sms_control_title)
                .setIcon(R.drawable.stat_sys_warning)
                .setMessage(messageText)
                .setPositiveButton(r.getString(R.string.sms_control_yes), listener)
                .setNegativeButton(r.getString(R.string.sms_control_no), listener)
                .setOnCancelListener(listener)
                .create();

        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        d.show();
    }

    /**
     * Post an alert for user confirmation when sending to a potential short code.
     * @param isPremium true if the destination is known to be a premium short code
     * @param tracker the SmsTracker for the current message.
     */
    protected void handleConfirmShortCode(boolean isPremium, SmsTracker tracker) {
        if (denyIfQueueLimitReached(tracker)) {
            return;     // queue limit reached; error was returned to caller
        }

        int detailsId;
        if (isPremium) {
            detailsId = R.string.sms_premium_short_code_details;
        } else {
            detailsId = R.string.sms_short_code_details;
        }

        CharSequence appLabel = getAppLabel(tracker.mAppInfo.packageName);
        Resources r = Resources.getSystem();
        Spanned messageText = Html.fromHtml(r.getString(R.string.sms_short_code_confirm_message,
                appLabel, tracker.mDestAddress));

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.sms_short_code_confirmation_dialog, null);

        ConfirmDialogListener listener = new ConfirmDialogListener(tracker,
                (TextView)layout.findViewById(R.id.sms_short_code_remember_undo_instruction));


        TextView messageView = (TextView) layout.findViewById(R.id.sms_short_code_confirm_message);
        messageView.setText(messageText);

        ViewGroup detailsLayout = (ViewGroup) layout.findViewById(
                R.id.sms_short_code_detail_layout);
        TextView detailsView = (TextView) detailsLayout.findViewById(
                R.id.sms_short_code_detail_message);
        detailsView.setText(detailsId);

        CheckBox rememberChoice = (CheckBox) layout.findViewById(
                R.id.sms_short_code_remember_choice_checkbox);
        rememberChoice.setOnCheckedChangeListener(listener);

        AlertDialog d = new AlertDialog.Builder(mContext)
                .setView(layout)
                .setPositiveButton(r.getString(R.string.sms_short_code_confirm_allow), listener)
                .setNegativeButton(r.getString(R.string.sms_short_code_confirm_deny), listener)
                .setOnCancelListener(listener)
                .create();

        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        d.show();

        listener.setPositiveButton(d.getButton(DialogInterface.BUTTON_POSITIVE));
        listener.setNegativeButton(d.getButton(DialogInterface.BUTTON_NEGATIVE));
    }

    /**
     * Returns the premium SMS permission for the specified package. If the package has never
     * been seen before, the default {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ASK_USER}
     * will be returned.
     * @param packageName the name of the package to query permission
     * @return one of {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_UNKNOWN},
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ASK_USER},
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_NEVER_ALLOW}, or
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW}
     */
    public int getPremiumSmsPermission(String packageName) {
        return mUsageMonitor.getPremiumSmsPermission(packageName);
    }

    /**
     * Sets the premium SMS permission for the specified package and save the value asynchronously
     * to persistent storage.
     * @param packageName the name of the package to set permission
     * @param permission one of {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ASK_USER},
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_NEVER_ALLOW}, or
     *  {@link SmsUsageMonitor#PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW}
     */
    public void setPremiumSmsPermission(String packageName, int permission) {
        mUsageMonitor.setPremiumSmsPermission(packageName, permission);
    }

    protected static String getAppNameByIntent(PendingIntent intent) {
        Resources r = Resources.getSystem();
        return (intent != null) ? intent.getTargetPackage()
            : "Resource unusable";//r.getString(R.string.sms_control_default_app_name);
    }

    /**
     * Send the message along to the radio.
     *
     * @param tracker holds the SMS message to send
     */
    protected abstract void sendSms(SmsTracker tracker);

    /**
     * Send the multi-part SMS based on multipart Sms tracker
     *
     * @param tracker holds the multipart Sms tracker ready to be sent
     */
    private void sendMultipartSms(SmsTracker tracker) {
        ArrayList<String> parts;
        ArrayList<PendingIntent> sentIntents;
        ArrayList<PendingIntent> deliveryIntents;

        HashMap<String, Object> map = tracker.mData;

        String destinationAddress = (String) map.get("destination");
        String scAddress = (String) map.get("scaddress");

        parts = (ArrayList<String>) map.get("parts");
        sentIntents = (ArrayList<PendingIntent>) map.get("sentIntents");
        deliveryIntents = (ArrayList<PendingIntent>) map.get("deliveryIntents");

        // check if in service
        int ss = mPhone.getServiceState().getState();
        if (ss != ServiceState.STATE_IN_SERVICE) {
            for (int i = 0, count = parts.size(); i < count; i++) {
                PendingIntent sentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    sentIntent = sentIntents.get(i);
                }
                handleNotInService(ss, sentIntent);
            }
            return;
        }

        sendMultipartText(destinationAddress, scAddress, parts, sentIntents, deliveryIntents);
    }

    /**
     * Send an acknowledge message.
     * @param success indicates that last message was successfully received.
     * @param result result code indicating any error
     * @param response callback message sent when operation completes.
     */
    protected abstract void acknowledgeLastIncomingSms(boolean success,
            int result, Message response);

    /**
     * Notify interested apps if the framework has rejected an incoming SMS,
     * and send an acknowledge message to the network.
     * @param success indicates that last message was successfully received.
     * @param result result code indicating any error
     * @param response callback message sent when operation completes.
     */
    private void notifyAndAcknowledgeLastIncomingSms(boolean success,
            int result, Message response) {
        if (!success) {
            // broadcast SMS_REJECTED_ACTION intent
            Intent intent = new Intent(Intents.SMS_REJECTED_ACTION);
            intent.putExtra("result", result);
            mWakeLock.acquire(WAKE_LOCK_TIMEOUT);
            mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
        }
        acknowledgeLastIncomingSms(success, result, response);
    }

    /**
     * Keeps track of an SMS that has been sent to the RIL, until it has
     * successfully been sent, or we're done trying.
     *
     */
    protected static final class SmsTracker {
        // fields need to be public for derived SmsDispatchers
        public final HashMap<String, Object> mData;
        public int mRetryCount;
        public int mMessageRef;

        public final PendingIntent mSentIntent;
        public final PendingIntent mDeliveryIntent;

        public final PackageInfo mAppInfo;
        public final String mDestAddress;

        public SmsTracker(HashMap<String, Object> data, PendingIntent sentIntent,
                PendingIntent deliveryIntent, PackageInfo appInfo, String destAddr) {
            mData = data;
            mSentIntent = sentIntent;
            mDeliveryIntent = deliveryIntent;
            mRetryCount = 0;
            mAppInfo = appInfo;
            mDestAddress = destAddr;
        }

        /**
         * Returns whether this tracker holds a multi-part SMS.
         * @return true if the tracker holds a multi-part SMS; false otherwise
         */
        protected boolean isMultipart() {
            HashMap map = mData;
            return map.containsKey("parts");
        }
    }

    /**
     * Dialog listener for SMS confirmation dialog.
     */
    private final class ConfirmDialogListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
            CompoundButton.OnCheckedChangeListener {

        private final SmsTracker mTracker;
        private Button mPositiveButton;
        private Button mNegativeButton;
        private boolean mRememberChoice;    // default is unchecked
        private final TextView mRememberUndoInstruction;

        ConfirmDialogListener(SmsTracker tracker, TextView textView) {
            mTracker = tracker;
            mRememberUndoInstruction = textView;
        }

        void setPositiveButton(Button button) {
            mPositiveButton = button;
        }

        void setNegativeButton(Button button) {
            mNegativeButton = button;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Always set the SMS permission so that Settings will show a permission setting
            // for the app (it won't be shown until after the app tries to send to a short code).
            int newSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER;

            if (which == DialogInterface.BUTTON_POSITIVE) {
                Log.d(TAG, "CONFIRM sending SMS");
                // XXX this is lossy- apps can have more than one signature
                //EventLog.writeEvent(EventLogTags.SMS_SENT_BY_USER,
                //                    mTracker.mAppInfo.signatures[0].toCharsString());
                sendMessage(obtainMessage(EVENT_SEND_CONFIRMED_SMS, mTracker));
                if (mRememberChoice) {
                    newSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW;
                }
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                Log.d(TAG, "DENY sending SMS");
                // XXX this is lossy- apps can have more than one signature
                //EventLog.writeEvent(EventLogTags.SMS_DENIED_BY_USER,
                //                    mTracker.mAppInfo.signatures[0].toCharsString());
                sendMessage(obtainMessage(EVENT_STOP_SENDING, mTracker));
                if (mRememberChoice) {
                    newSmsPermission = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_NEVER_ALLOW;
                }
            }
            setPremiumSmsPermission(mTracker.mAppInfo.packageName, newSmsPermission);

            // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
            if(FeatureOption.MTK_SMS_FILTER_SUPPORT == true) {
                while(sConcatMsgCount > 0 && mPendingTrackerCount > 0) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Log.d(TAG, "[NQ continue sending " + sConcatMsgCount);
                        sendMessage(obtainMessage(EVENT_SEND_CONFIRMED_SMS, mTracker));
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        Log.d(TAG, "[NQ stop sending " + sConcatMsgCount);
                    }
                        
                    sConcatMsgCount -= 1;
                } // end while(sConcatMsgCount > 0 && mPendingTrackerCount > 0)
            }
            // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Log.d(TAG, "dialog dismissed: don't send SMS");
            sendMessage(obtainMessage(EVENT_STOP_SENDING, mTracker));
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(TAG, "remember this choice: " + isChecked);
            mRememberChoice = isChecked;
            if (isChecked) {
                mPositiveButton.setText(R.string.sms_short_code_confirm_always_allow);
                mNegativeButton.setText(R.string.sms_short_code_confirm_never_allow);
                if (mRememberUndoInstruction != null) {
                    mRememberUndoInstruction.
                            setText(R.string.sms_short_code_remember_undo_instruction);
                    mRememberUndoInstruction.setPadding(0,0,0,32);
                }
            } else {
                mPositiveButton.setText(R.string.sms_short_code_confirm_allow);
                mNegativeButton.setText(R.string.sms_short_code_confirm_deny);
                if (mRememberUndoInstruction != null) {
                    mRememberUndoInstruction.setText("");
                    mRememberUndoInstruction.setPadding(0,0,0,0);
                }
            }
        }
    }

    private final BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        boolean hasNotifiedForWapPushSetting = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
            if (intent.getAction().equals(ACTION_WAP_PUSH_NOTI_CANCEL)) {
                Log.d(TAG, "receive cancel intent");
                NotificationManager notiMgr = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notiMgr != null) {
                    Log.d(TAG, "cancel wap push setting notification");
                    notiMgr.cancel(WAP_PUSH_NOTI_ID);
                } else {
                    Log.d(TAG, "fail to create notiMgr by mContext");
                }
            } else if (intent.getAction().equals(INTENT_ETWS_ALARM)) {
                int etws_sim = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mPhone.getMySimId());
                Log.d(TAG, "receive EVENT_ETWS_ALARM " + etws_sim);
                if (etws_sim == mPhone.getMySimId()) {
                    mCellBroadcastFwkExt.closeEtwsChannel(new EtwsNotification());
                    stopEtwsAlarm();
                }
            } else {
            // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
                // Assume the intent is one of the SMS receive intents that
                // was sent as an ordered broadcast.  Check result and ACK.
                int rc = getResultCode();
                boolean success = (rc == Activity.RESULT_OK)
                                    || (rc == Intents.RESULT_SMS_HANDLED);

                long rTime = intent.getLongExtra("rTime", -1);
                if (rTime != -1) {
                    long curTime = System.currentTimeMillis();
                    Log.d(TAG, "CNMA elplased time: " + (curTime - rTime));
                    if ((curTime - rTime) / 1000 > 8) {
                        Log.d(TAG, "APP process too long");                
                    } else { 
                        // For a multi-part message, this only ACKs the last part.
                        // Previous parts were ACK'd as they were received.
                        acknowledgeLastIncomingSms(success, rc, null);
                    }
                }   
            // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
            }
            // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        }
    };

    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /**
     * Called when SimSmsInterfaceManager update SIM card fail due to SIM_FULL.
     */
    public void handleIccFull() {
        // broadcast SIM_FULL intent
        mStorageMonitor.handleIccFull();
    }

    /**
     * Called when a CB activation result is received.  
     *
     * @param ar AsyncResult passed into the message handler. 
     */
    protected void handleQueryCbActivation(AsyncResult ar) {
        Log.e(TAG, "didn't support cellBoradcast in the CDMA phone");
    }

    /**
     * Dispatches an incoming Cb SMS messages.
     *
     * @param smsPdu the pdu string of the incoming message from the phone
     */
    protected void dispatchCbMessage(String smsPdu) {
        Log.e(TAG, "didn't support cellBoradcast in the CDMA phone");
    }

    /**
     * If this is the last part send the parts out to the application, otherwise
     * the part is stored for later processing.
     *
     * NOTE: concatRef (naturally) needs to be non-null, but portAddrs can be null.
     * @return a result code from {@link Telephony.Sms.Intents}, or
     *         {@link Activity#RESULT_OK} if the message has been broadcast
     *         to applications
     */
    protected int processMessagePart(SmsMessageBase sms,
            SmsHeader.ConcatRef concatRef, SmsHeader.PortAddrs portAddrs) {
        // Lookup all other related parts
        StringBuilder where = new StringBuilder("reference_number =");
        where.append(concatRef.refNumber);
        where.append(" AND address = ?");
        where.append(" AND sim_id = " + mSimId);
        String[] whereArgs = new String[] {sms.getOriginatingAddress()};

        byte[][] pdus = null;
        Cursor cursor = null;
        try {
            cursor = mResolver.query(mRawUri, RAW_PROJECTION, where.toString(), whereArgs, null);
            if (cursor == null) {
                return Intents.RESULT_SMS_GENERIC_ERROR;
            }
            int cursorCount = cursor.getCount();

            // All the parts are in place, deal with them
            int pduColumn = cursor.getColumnIndex("pdu");
            int sequenceColumn = cursor.getColumnIndex("sequence");

            // for ALPS00007326
            // judeg if we have already received the same segment
            for (int i = 0; i < cursorCount; i++) {
                cursor.moveToNext();
                int cursorSequence = (int) cursor.getLong(sequenceColumn);
                if (cursorSequence == concatRef.seqNumber) {
                    Log.w(TAG, "Received Duplicate segment: " + cursorSequence);
                    return Intents.RESULT_SMS_HANDLED;
                }
            }
            cursor.moveToFirst();

            if (cursorCount != concatRef.msgCount - 1) {
                // We don't have all the parts yet, store this one away
                ContentValues values = new ContentValues();
                values.put("date", new Long(sms.getTimestampMillis()));
                values.put("pdu", HexDump.toHexString(sms.getPdu()));
                values.put("address", sms.getOriginatingAddress());
                values.put("reference_number", concatRef.refNumber);
                values.put("count", concatRef.msgCount);
                values.put("sequence", concatRef.seqNumber);
                values.put("sim_id", mSimId);
                if (portAddrs != null) {
                    values.put("destination_port", portAddrs.destPort);
                }
                mResolver.insert(mRawUri, values);
                return Intents.RESULT_SMS_HANDLED;
            }

            pdus = new byte[concatRef.msgCount][];
            for (int i = 0; i < cursorCount; i++) {

                int cursorSequence = (int) cursor.getLong(sequenceColumn);
                pdus[cursorSequence - 1] = HexDump.hexStringToByteArray(
                        cursor.getString(pduColumn));
                cursor.moveToNext();
            }
            // This one isn't in the DB, so add it
            pdus[concatRef.seqNumber - 1] = sms.getPdu();

            // Remove the parts from the database
            mResolver.delete(mRawUri, where.toString(), whereArgs);
        } catch (SQLException e) {
            Log.e(TAG, "Can't access multipart SMS database", e);
            // TODO: Would OUT_OF_MEMORY be more appropriate?
            return Intents.RESULT_SMS_GENERIC_ERROR;
        } finally {
            if (cursor != null) cursor.close();
        }

        /**
         * TODO(cleanup): The following code has duplicated logic with
         * the radio-specific dispatchMessage code, which is fragile,
         * in addition to being redundant.  Instead, if this method
         * maybe returned the reassembled message (or just contents),
         * the following code (which is not really related to
         * reconstruction) could be better consolidated.
         */

        // Dispatch the PDUs to applications
        if (portAddrs != null) {
            /*
            if((isCuVersion() == true) && (allowDispatchWapPush(mSimId) == false)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (int i = 0; i < concatRef.msgCount; i++) {
                    SmsMessage msg = SmsMessage.createFromPdu(pdus[i]);
                    if(msg != null) {
                        byte[] data = msg.getUserData();
                        output.write(data, 0, data.length);
                    }
                }
                boolean isMms = isMmsWapPush(output.toByteArray());
                if(isMms == false) {
                    Log.d(TAG, "don't dispatch push message");
                    return Intents.RESULT_SMS_HANDLED;
                }
            */
            boolean allowDispatch = mWapPushFwkExt.allowDispatchWapPush();
            if(allowDispatch == false) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (int i = 0; i < concatRef.msgCount; i++) {
                    SmsMessage msg = SmsMessage.createFromPdu(pdus[i]);
                    if(msg != null) {
                        byte[] data = msg.getUserData();
                        output.write(data, 0, data.length);
                    }
                }
                
                boolean isMms = mWapPushFwkExt.isMmsWapPush(output.toByteArray());
                if(isMms == false) {
                    Log.d(TAG, "don't dispatch push message");
                    return Intents.RESULT_SMS_HANDLED;
                }
            }
            if (portAddrs.destPort == SmsHeader.PORT_WAP_PUSH) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                //modified by mtk80611
                if(FeatureOption.MTK_WAPPUSH_SUPPORT){
                    Bundle mBundle = new Bundle();
                    SmsMessage msg = SmsMessage.createFromPdu(pdus[0]);
                    if(msg != null) {
                        byte[] data = msg.getUserData();
                        output.write(data,0,data.length);
    
                        mBundle.putString(Telephony.WapPush.ADDR, sms.getOriginatingAddress());
                        mBundle.putString(Telephony.WapPush.SERVICE_ADDR, sms.getServiceCenterAddress());
    
                        for(int i = 1;i<concatRef.msgCount;i++){
                            msg = SmsMessage.createFromPdu(pdus[i]);
                            if(msg != null) {
                                data = msg.getUserData();
                                output.write(data,0,data.length);
                            }
                        }
                        // Handle the PUSH
                        return mWapPush.dispatchWapPdu(output.toByteArray(),mBundle);
                    }
                } else {
                    for (int i = 0; i < concatRef.msgCount; i++) {
                        SmsMessage msg = SmsMessage.createFromPdu(pdus[i]);
                        if(msg != null) {
                            byte[] data = msg.getUserData();
                            output.write(data, 0, data.length);
                        }
                    }
                    return mWapPush.dispatchWapPdu(output.toByteArray());
                }
                // modified by mtk80611
            } else {
                // The messages were sent to a port, so concoct a URI for it
                SmsMessage msg = SmsMessage.createFromPdu(pdus[0]);
                // MTK_OPTR_PROTECT_START
                if (true && /* need a feature option */
                portAddrs.destPort == DM_PORT &&
                        msg != null &&
                        msg.getOriginatingAddress().equals(DM_OA)) {

                    dispatchDmRegisterPdus(pdus);
                } else {
                    // MTK_OPTR_PROTECT_END
                    dispatchPortAddressedPdus(pdus, portAddrs.destPort);
                    // MTK_OPTR_PROTECT_START
                }
                // MTK_OPTR_PROTECT_END
            }
        } else {
            // The messages were not sent to a port
            SmsMessage msg = SmsMessage.createFromPdu(pdus[0]);
            if (msg != null && msg.getMessageBody() == null) {
                Log.d(TAG, " We discard SMS with dcs 8 bit");
                return Intents.RESULT_SMS_GENERIC_ERROR;
            }
            dispatchPdus(pdus);
        }
        return Activity.RESULT_OK;
    }

    // MTK_OPTR_PROTECT_START
    private BroadcastReceiver mDMLockReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "[DM-Lock receive lock/unlock intent");
                if(intent.getAction().equals("com.mediatek.dm.LAWMO_LOCK")) {
                    Log.d(TAG, "[DM-Lock DM is locked now");
                    isDmLock = true;
                } else if(intent.getAction().equals("com.mediatek.dm.LAWMO_UNLOCK")) {
                    Log.d(TAG, "[DM-Lock DM is unlocked now");
                    isDmLock = false;
                }
            }
    };
    // MTK_OPTR_PROTECT_END

    protected boolean checkSmsWithNqFilter(String address, String text, PendingIntent sentIntent) {
        String pkgName = getAppNameByIntent(sentIntent);
        //String appName = mContext.getPackageManager().getApplicationLabel(mContext.getApplicationInfo()).toString();
        String appName = null;
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(pkgName, 0);
            appName = mContext.getPackageManager().getApplicationLabel(appInfo).toString();
        } catch (NameNotFoundException e) {
            appName = "Resource unusable";// Resources.getSystem().getString(R.string.sms_control_default_app_name);
        }

        Log.d(TAG, "[NQ address = " + address + ", text = " + text
                + ", pkgName = " + pkgName + ", appName = " + appName);

        boolean isDeductedMessage = false;
        try {
            isDeductedMessage = NqSmsFilter.getInstance(mContext).nqSmsFilter(address, text, pkgName, appName);
        } catch (Exception e) {
            Log.d(TAG, "[Nq Exception is thrown when call NqSmsFilter");
        }

        return isDeductedMessage;
    }

    private void handleDeductedMessage(SmsTracker tracker) {
        if (mPendingTrackerCount >= MO_MSG_QUEUE_LIMIT) {
            // Deny the sending when the queue limit is reached.
            try {
                tracker.mSentIntent.send(RESULT_ERROR_LIMIT_EXCEEDED);
            } catch (CanceledException ex) {
                Log.e(TAG, "failed to send back RESULT_ERROR_LIMIT_EXCEEDED");
            }
            return;
        }

        Resources r = Resources.getSystem();

        ConfirmDialogListener listener = new ConfirmDialogListener(tracker, null);

        AlertDialog dlg = new AlertDialog.Builder(mContext)
            .setTitle(r.getString(com.mediatek.internal.R.string.nq_sms_filter_title))
            .setMessage(r.getString(com.mediatek.internal.R.string.nq_sms_filter_message))
            .setPositiveButton(r.getString(com.mediatek.internal.R.string.nq_sms_filter_yes), listener)
            .setNegativeButton(r.getString(com.mediatek.internal.R.string.nq_sms_filter_no), listener)
            .create();

        dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dlg.setCancelable(false);
        dlg.show();
        
        //sendMessageDelayed ( obtainMessage(EVENT_REDUCTED_MESSAGE_TIMEOUT, tracker),
        //        DEFAULT_SMS_TIMEOUT);
    }

    private SmsMessage createMessageFromPdu(byte[] smsc, byte[] tpdu) {
        Log.d(TAG, "[NQ tpdu first byte is " + tpdu[0]);
        int tpduLen = tpdu.length;
        int smscLen = 1;
        if (smsc != null) {
            smscLen = smsc.length;
        } else {
            Log.d(TAG, "[NQ smsc is null");
        }
        byte[] msgPdu = new byte[smscLen + tpduLen];
        int curIndex = 0;
        try {
            if (smsc != null) {
                System.arraycopy(smsc, 0, msgPdu, curIndex, smscLen);
            } else {
                msgPdu[0] = 0;
            }
            curIndex += smscLen;
            System.arraycopy(tpdu, 0, msgPdu, curIndex, tpduLen);
            Log.d(TAG, "[NQ mti byte in msgPdu is " + msgPdu[1]);
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "[NQ out of bounds error when copy pdu data");
        }

        return SmsMessage.createFromPdu(msgPdu, getFormat());
    }

    // MTK_OPTR_PROTECT_START
    /**
     * Dispatches DM Register PDUs to DM APP
     *
     * @param pdus The raw PDUs making up the message
     */
    protected void dispatchDmRegisterPdus(byte[][] pdus) {
        Intent intent = new Intent(Intents.DM_REGISTER_SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", pdus);
        dispatch(intent, "android.permission.RECEIVE_DM_REGISTER_SMS");
    }
    // MTK_OPTR_PROTECT_END

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param destPort the port to deliver the message to
     * @param originalPort the port to deliver the message from
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    protected abstract void sendData(String destAddr, String scAddr, int destPort, int originalPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent);

    /**
     * Send a multi-part data based SMS.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param data an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param destPort the port to deliver the message to
     * @param data an array of data messages in order,
     *   comprise the original message     
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */
    protected abstract void sendMultipartData(
            String destAddr, String scAddr, int destPort,
            ArrayList<SmsRawData> data, ArrayList<PendingIntent> sentIntents, 
            ArrayList<PendingIntent> deliveryIntents);

    /**
     * Send a text based SMS to a specified application port.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param destPort the port to deliver the message to
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    protected abstract void sendText(String destAddr, String scAddr, String text, 
            int destPort,PendingIntent sentIntent, PendingIntent deliveryIntent);

    /**
     * Send a multi-part text based SMS to a specified application port.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param destPort the port to deliver the message to
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */
    protected abstract void sendMultipartText(String destAddr, String scAddr,
            ArrayList<String> parts, int destPort, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents);

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
    abstract public int copyTextMessageToIccCard(
            String scAddress, String address, List<String> text,
            int status, long timestamp);

    private void notifySmsReady(boolean isReady) {
        // broadcast SMS_STATE_CHANGED_ACTION intent
        Intent intent = new Intent(Intents.SMS_STATE_CHANGED_ACTION);
        intent.putExtra("ready", isReady);
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        mContext.sendBroadcast(intent);
    }

    /**
     * Check if a SmsTracker holds multi-part Sms
     *
     * @param tracker a SmsTracker could hold a multi-part Sms
     * @return true for tracker holds Multi-parts Sms
     */
    private boolean isMultipartTracker(SmsTracker tracker) {
        HashMap map = tracker.mData;
        return (map.get("parts") != null);
    }


    /**
     * Set the memory storage status of the SMS
     * This function is used for FTA test only
     * 
     * @param status false for storage full, true for storage available
     *
     */
    protected void setSmsMemoryStatus(boolean status) {
        if (status != mStorageAvailable) {
            mStorageAvailable = status;
            mCm.reportSmsMemoryStatus(status, null);
        }
    }

    protected boolean isSmsReady() {
        return mSmsReady;
    }
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    
    // MTK-START [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    /**
     * Send an SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param encodingType the encoding type of content of message(GSM 7-bit, Unicode or Automatic)
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    abstract protected void sendTextWithEncodingType(
            String destAddr,
            String scAddr,
            String text,
            int encodingType,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent);

    /**
     * Send a multi-part text based SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param encodingType the encoding type of content of message(GSM 7-bit, Unicode or Automatic)
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */
    abstract protected void sendMultipartTextWithEncodingType(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            int encodingType,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents);
    // MTK-END [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22

    /**
     * Send an SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    abstract public void sendTextWithExtraParams(
            String destAddr,
            String scAddr,
            String text,
            Bundle extraParams,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent);

    /**
     * Send a multi-part text based SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */
    abstract public void sendMultipartTextWithExtraParams(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            Bundle extraParams,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents);

    protected void dispatchMwiMessage(SmsMessageBase sms) {
        Log.d(TAG, "broadcast intent for MWI message");
        byte[][] pdus = new byte[1][];
        pdus[0] = sms.getPdu();

        Intent intent = new Intent(Intents.MWI_SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        dispatch(intent, RECEIVE_SMS_PERMISSION);
    }

    abstract protected SmsMessage createMessageFromSubmitPdu(byte[] smsc, byte[] tpdu);

    protected void handleNewSms(final SmsMessage sms) {
        if (sms == null) {
            Log.d(TAG, "handleNewSms: null sms");
            return;
        }

        Log.d(TAG, "handleNewSms: handle new sms in a new thread");
        Thread smsHandleThread = new Thread() {
            public void run() {
                try {
                    int result = dispatchMessage(sms.mWrappedSmsMessage);
                    if (result != Activity.RESULT_OK) {
                        // RESULT_OK means that message was broadcast for app(s) to handle.
                        // Any other result, we should ack here.
                        boolean handled = (result == Intents.RESULT_SMS_HANDLED);
                        notifyAndAcknowledgeLastIncomingSms(handled, result, null);
                    }
                } catch (RuntimeException ex) {
                    Log.e(TAG, "Exception dispatching message", ex);
                    notifyAndAcknowledgeLastIncomingSms(false, Intents.RESULT_SMS_GENERIC_ERROR, null);
                }
            }
        };
        smsHandleThread.start();
    }

    protected void dispatchBroadcastMessage(SmsCbMessage message) {
        if (message.isEmergencyMessage()) {
            Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
            intent.putExtra("message", message);
            Log.d(TAG, "Dispatching emergency SMS CB:" + message);
            dispatch(intent, RECEIVE_EMERGENCY_BROADCAST_PERMISSION);
        } else {
            Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
            intent.putExtra("message", message);
            Log.d(TAG, "Dispatching SMS CB");
            dispatch(intent, RECEIVE_SMS_PERMISSION);
        }
    }

    protected void startEtwsAlarm() {
        int delayInMs = 30 * 60 * 1000;
        AlarmManager am =
            (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        Log.d(TAG, "startEtwsAlarm");
        Intent intent = new Intent(INTENT_ETWS_ALARM);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mPhone.getMySimId());
        }
        mEtwsAlarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayInMs, mEtwsAlarmIntent);
    }
    
    protected void stopEtwsAlarm() {
        AlarmManager am =
            (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        Log.d(TAG, "stopEtwsAlarm");
        if (mEtwsAlarmIntent != null) {
            am.cancel(mEtwsAlarmIntent);
            mEtwsAlarmIntent = null;
        }
    }
}
