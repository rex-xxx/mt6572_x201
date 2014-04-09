package com.hissage.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.telephony.ServiceState;
import android.util.Log;

import com.hissage.config.NmsConfig;
import com.hissage.download.NmsDownloadManager;
import com.hissage.jni.engineadapter;
import com.hissage.message.smsmms.NmsSMSMMS;
import com.hissage.message.smsmms.NmsSmsMmsSync;
import com.hissage.message.smsmms.NmsThreadsObserver;
import com.hissage.notification.NmsNotificationManager;
import com.hissage.platfrom.NmsPlatformAdapter;
import com.hissage.pn.HpnsApplication;
import com.hissage.pn.hpnsReceiver;
import com.hissage.receiver.system.NmsSMSReceiver;
import com.hissage.util.data.NmsConsts.HissageTag;
import com.hissage.util.data.NmsConsts.NmsIntentStrId;
import com.hissage.util.log.NmsLog;
import com.hissage.util.preference.NmsPreferences;
import com.hissage.vcard.NmsContactObserver;

public class NmsService extends Service {

    private static final String TAG = "NmsService";
    public static volatile boolean bCEngineRunflag = false;
    public static final String pushEngineConnecd = "pushEngineConnecd";
    public static final String regAllReciverNow = "regAllReciverNow";
    private static NmsService mInstance = null;
    private static NmsSMSReceiver mSmsInter = null;
    private static BroadcastReceiver mServiceStateReceiver = null;
    private static boolean mNotifySimChanged = true;
    private ContentObserver mThreadsObserver = null;
    private Binder binder = new Binder();

    public static Context getInstance() {
        return HpnsApplication.mGlobalContext;
    }
    public static NmsService getService(){
        return mInstance;
    }

    public void addThreadObserver() {
        NmsLog.trace(TAG, "add a Thread observer. ");

        ContentResolver resolver = getContentResolver();
        Handler handler = NmsSmsMmsSync.getInstance(HpnsApplication.mGlobalContext);
        if (null == mThreadsObserver) {
            mThreadsObserver = new NmsThreadsObserver(handler);
        }
        resolver.registerContentObserver(Uri.parse("content://mms-sms/threads"), false,
                mThreadsObserver);
    }

    public void removeThreadObserver() {
        NmsLog.trace(TAG, "remove a Thread observer. ");
        if (null == mThreadsObserver) {
            return;
        }
        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mThreadsObserver);
    }

    public void addContactObserver() {
        NmsContactObserver.registerContentObserver(super.getApplicationContext(), true);
    }

    public void regSmsBroardCastReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(NmsSMSMMS.SMS_RECEIVED);
        filter.setPriority(2147483647);
        if (null == mSmsInter) {
            mSmsInter = NmsSMSReceiver.getInstance();
        }
        registerReceiver(mSmsInter, filter);
    }

    public void unRegSmsBroardCastReceiver() {
        if (null != mSmsInter) {
            unregisterReceiver(mSmsInter);
        }

    }

    private void addMmsTransactionRecver() {
        mmsreceiver = new mmsTransactionStateRecver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NmsIntentStrId.NMS_INTENT_MMS_TRANSACTION);
        registerReceiver(mmsreceiver, filter);
    }

    private void unRegServiceStateReceiver() {
        if (null != mServiceStateReceiver) {
            unregisterReceiver(mServiceStateReceiver);
            mServiceStateReceiver = null;
        }
    }

    private void regServiceStateReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
        mServiceStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("ANDROID_INFO", "Service state changed");
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    int state = bundle.getInt("state");
                    // int simId = bundle.getInt("simId");

                    NmsLog.trace(TAG, "Phone service state changed. state: " + state
                            + " mAirplaneMode: " + NmsConfig.mAirplaneMode + " mNotifySimChanged: "
                            + mNotifySimChanged);
                    switch (state) {
                    case ServiceState.STATE_IN_SERVICE: {
                        if (NmsConfig.mAirplaneMode) {
                            engineadapter.get().nmsSetAirplaneMode(
                                    (int) NmsPlatformAdapter.getInstance(NmsService.this)
                                            .getCurrentSimId(), 0);
                            Intent airplaneIntent = new Intent();
                            airplaneIntent.setAction(NmsIntentStrId.NMS_INTENT_AIRPLANE_MODE);
                            airplaneIntent.putExtra(NmsIntentStrId.NMS_AIRPLANE_MODE, false);
                            mInstance.sendBroadcast(airplaneIntent);
                        } else {
                            if (mNotifySimChanged) {
                                NmsLog.trace(TAG, "sim card status is changed, notify engine.");
                                int initEngine = engineadapter.msgtype.NMS_ENG_MSG_SIM_STATUS_CHANGED
                                        .ordinal();
                                engineadapter.get().nmsSendMsgToEngine(initEngine, null, 0);
                                mNotifySimChanged = false;
                            }
                        }
                        NmsConfig.mAirplaneMode = false;
                        break;
                    }
                    case ServiceState.STATE_OUT_OF_SERVICE: {
                        if (mNotifySimChanged) {
                            NmsLog.trace(TAG, "sim card status is changed, notify engine.");
                            int initEngine = engineadapter.msgtype.NMS_ENG_MSG_SIM_STATUS_CHANGED
                                    .ordinal();
                            engineadapter.get().nmsSendMsgToEngine(initEngine, null, 0);
                            mNotifySimChanged = false;
                        }
                        break;
                    }
                    case ServiceState.STATE_POWER_OFF: {
                        if (!NmsConfig.mAirplaneMode) {
                            engineadapter.get().nmsSetAirplaneMode(
                                    (int) NmsPlatformAdapter.getInstance(NmsService.this)
                                            .getCurrentSimId(), 1);
                            Intent airplaneIntent = new Intent();
                            airplaneIntent.setAction(NmsIntentStrId.NMS_INTENT_AIRPLANE_MODE);
                            airplaneIntent.putExtra(NmsIntentStrId.NMS_AIRPLANE_MODE, true);
                            mInstance.sendBroadcast(airplaneIntent);
                        }
                        NmsConfig.mAirplaneMode = true;
                        break;
                    }
                    default: {
                        NmsLog.trace(TAG, "Phone service state changed. state: " + state
                                + " but not handle");
                        break;
                    }
                    }
                }
            }
        };
        registerReceiver(mServiceStateReceiver, intentFilter);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mInstance == null) {
            mInstance = this;
        }



        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            // set handler
            @Override
            public void uncaughtException(Thread thread, final Throwable ex) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);

                StringBuilder sb = new StringBuilder();

                sb.append("Version code is ");
                sb.append(Build.VERSION.SDK_INT + "\n");// set android version
                sb.append("Model is ");
                sb.append(Build.BRAND + "-" + Build.MODEL + "\n");// set model
                                                                  // number
                sb.append(sw.toString());

                NmsLog.error(HissageTag.global, "Catch Exception:" + sb.toString());
                android.os.Process.killProcess(android.os.Process.myPid());
                // finish();

            }
        });

        addMmsTransactionRecver();

        NmsNotificationManager.getInstance(this).init();
        NmsDownloadManager.getInstance().init();

        activeC_Engine();

        hpnsReceiver.loadPn(this);

        NmsLog.error(TAG, "bootstart service on create new in 4-27.");
    }

    // this api for INIT_DONE of C_Engine only
    public static void RegAllReceiver() {
        
        mInstance.regSmsBroardCastReceiver();
        mInstance.addThreadObserver();
        mInstance.addContactObserver();
        mInstance.regServiceStateReceiver(); 
    }

    public static void unRegAllReciver() {
        if (mInstance == null)
            return;
        mInstance.unRegSmsBroardCastReceiver();
        mInstance.unRegServiceStateReceiver();
        NmsContactObserver.unregisterContentObserver(true);
    }

    public void activeC_Engine() {
        if (bCEngineRunflag)
            return;

        /*
        int initEngine = engineadapter.msgtype.NMS_ENG_MSG_INIT_REQ.ordinal();
        engineadapter.get().nmsSendMsgToEngine(initEngine, null, 0); */
        new Thread(new Runnable() {
            
            public void run() {
                engineadapter.get();
            }
        }).start() ;
        
        bCEngineRunflag = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // if (intent != null && intent.getBooleanExtra(activeHesine, true)) {
        // int activeHesine =
        // engineadapter.msgtype.NMS_ENG_MSG_ACTIVATE_REQ.ordinal();
        // engineadapter.get().nmsSendMsgToEngine(activeHesine, null, 0);
        // }
//        if (intent != null && intent.getBooleanExtra(pushEngineConnecd, false)) {
//            int connectNet = engineadapter.msgtype.NMS_ENG_MSG_CONNECT_REQ.ordinal();
//            engineadapter.get().nmsSendMsgToEngine(connectNet, null, 0);
//        }
        if (intent != null && intent.getBooleanExtra(regAllReciverNow, false)) {
            RegAllReceiver();
        }

        NmsLog.trace(TAG, "bootstart service on start command.");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the interface
        return binder;
    }
    @Override 
    public boolean onUnbind(Intent intent)  {
        return true;
    }

    @Override
    public void onDestroy() {
        unRegAllReciver();
        unregisterReceiver(mmsreceiver);
        Process.killProcess(Process.myPid());
        super.onDestroy();
    }

    private mmsTransactionStateRecver mmsreceiver = null;

    public class mmsTransactionStateRecver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            NmsLog.trace(TAG, "The mmsTransactionStateRecver action:" + action);

            if (action.equals(NmsIntentStrId.NMS_INTENT_MMS_TRANSACTION)) {
                // CommonUtils.showWarnMsgToNotificationBar(NmsService.this,
                // intent.getStringExtra(NmsIntentStrId.NMS_INTENT_MMS_TRANSACTION));
            }
        }
    }
}
