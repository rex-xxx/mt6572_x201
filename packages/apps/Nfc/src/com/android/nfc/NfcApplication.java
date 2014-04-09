package com.android.nfc;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.os.Process;
import android.os.UserHandle;
import java.util.Iterator;
import java.util.List;


/// M: @ {
import android.util.Log;
import com.mediatek.nfc.addon.NfcSimStateObserver;
import com.android.nfc.NfcService;

/// }


public class NfcApplication extends Application {

    static final String TAG = "NfcApplication";
    static final String NFC_PROCESS = "com.android.nfc";

    NfcService mNfcService;

    public NfcApplication() {

    }

    @Override
    /// M: @{
    public void onCreate() {
        super.onCreate();
        createSimStateObserver();
    }

    private void doOnCreate() {
        boolean isMainProcess = false;
        // We start a service in a separate process to do
        // handover transfer. We don't want to instantiate an NfcService
        // object in those cases, hence check the name of the process
        // to determine whether we're the main NFC service, or the
        // handover process
        ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
        List processes = am.getRunningAppProcesses();
        Iterator i = processes.iterator();
        while (i.hasNext()) {
            RunningAppProcessInfo appInfo = (RunningAppProcessInfo)(i.next());
            if (appInfo.pid == Process.myPid()) {
                isMainProcess =  (NFC_PROCESS.equals(appInfo.processName));
                break;
            }
        }
        if (UserHandle.myUserId() == 0 && isMainProcess) {
            mNfcService = new NfcService(this);
        }
    }
    /// }

    /// M: @{
    NfcSimStateObserver.Callback mSimStateCallback = new NfcSimStateObserver.Callback() {
        public void onSimReady(int simId) {
            Log.d(TAG, "onSimReady, simId = " + simId);
            if (NfcService.getInstance() == null) {
                doOnCreate();
            }
        }

        public void onSimReadyTimeOut() {
            Log.d(TAG, "onSimReadyTimeOut");
            if (NfcService.getInstance() == null) {
                doOnCreate();
            }
        }
    };

    private NfcSimStateObserver mSimStateObserver;
    private void createSimStateObserver() {
        mSimStateObserver = new NfcSimStateObserver(this, mSimStateCallback, 6000);
    }
    /// }




    
}
