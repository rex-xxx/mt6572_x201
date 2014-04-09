package com.mediatek.nfc.addon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;

public class NfcSimStateObserver implements Handler.Callback {
    private static final String TAG = "NfcSimStateObserve";

    public interface Callback {
        public void onSimReady(int simId);
        public void onSimReadyTimeOut();
    }

    public boolean isSimReady(int simId) {
        if (simId == 0) {
            return mIsSim1Ready;
        } else if (simId == 1) {
            return mIsSim2Ready;
        }
        return false;
    }
    
    public NfcSimStateObserver(Context context, Callback callback, int msToWait) {
        mContext = context;
        mCallback = callback;
        mHandler = new Handler(this);
        mFilter = new IntentFilter(); 
        mFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, mFilter);
        Log.d(TAG, "wait for sim ready, msToWait = " + msToWait);
        mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, msToWait);
    }

    private static final int MSG_TIMEOUT = 1;
    private Context mContext;
    private Handler mHandler;
    private IntentFilter mFilter;
    private boolean mIsSim1Ready;
    private boolean mIsSim2Ready;
    private Callback mCallback;
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_TIMEOUT:
                Log.d(TAG, "handleMessage: MSG_TIMEOUT");
                mCallback.onSimReadyTimeOut();
                return true;
        }
        return false;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String iccState;
                int simId;
                iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                if (iccState == null) {
                    iccState = "NULL";
                }
                Log.d(TAG, "ACTION_SIM_STATE_CHANGED receiver with iccState = " + iccState + ", simId = " + simId);
                if (iccState.equals(IccCardConstants.INTENT_VALUE_ICC_READY)) {
                    mCallback.onSimReady(simId);
                }
            }
        }
    };

}

