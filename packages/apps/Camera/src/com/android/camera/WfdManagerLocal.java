package com.android.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;

//import com.mediatek.wfd.WfdManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WfdManagerLocal implements Camera.Resumable {
    private static final String TAG = "WfdManagerLocal";
    private static final boolean LOG = Log.LOGV;
    
    public interface Listener {
        void onStateChanged(boolean enabled);
    }

    private Camera mContext;
    private boolean mResumed;
    private DisplayManager mDisplayManager;
    private WifiDisplayStatus mWifiDisplayStatus;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (LOG) {
                Log.v(TAG, "onReceive(" + intent + ")");
            }
            String action = intent.getAction();
            if (action.equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                WifiDisplayStatus status = (WifiDisplayStatus)intent.getParcelableExtra(
                        DisplayManager.EXTRA_WIFI_DISPLAY_STATUS);
                mWifiDisplayStatus = status;
                notifyWfdStateChanged(isWfdEnabled());
            }
        };
    };
    
    public WfdManagerLocal(Camera context) {
        mContext = context;
        mContext.addResumable(this);
        mDisplayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
    }
    
    public synchronized boolean isWfdEnabled() {
        boolean enabled = false;
        int activeDisplayState = -1;
        if (mWifiDisplayStatus == null) {
            mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        }
        activeDisplayState = mWifiDisplayStatus.getActiveDisplayState();
        enabled = activeDisplayState == WifiDisplayStatus.DISPLAY_STATE_CONNECTED;
        if (LOG) {
            Log.v(TAG, "isWfdEnabled() mWifiDisplayStatus=" + mWifiDisplayStatus + ", return " + enabled);
        }
        return enabled;
    }
    
    private List<Listener> mListeners = new CopyOnWriteArrayList<Listener>();
    public boolean addListener(Listener l) {
        if (!mListeners.contains(l)) {
            return mListeners.add(l);
        }
        return false;
    }
    
    public boolean removeListener(Listener l) {
        return mListeners.remove(l);
    }
    
    private void notifyWfdStateChanged(boolean enabled) {
        if (LOG) {
            Log.v(TAG, "notifyWfdStateChanged(" + enabled + ")");
        }
        for (Listener listener : mListeners) {
            if (listener != null) {
                listener.onStateChanged(enabled);
            }
        }
    }
    
    @Override
    public void begin() {
    }

    @Override
    public void finish() {
    }

    @Override
    public void pause() {
        if (LOG) {
            Log.v(TAG, "pause() mResumed=" + mResumed);
        }
        if (mResumed) {
            mContext.unregisterReceiver(mReceiver);
            mWifiDisplayStatus = null;
            mResumed = false;
        }
    }

    @Override
    public void resume() {
        if (LOG) {
            Log.v(TAG, "resume() mResumed=" + mResumed);
        }
        if (!mResumed) {
            IntentFilter filter = new IntentFilter(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
            mWifiDisplayStatus = null;
            notifyWfdStateChanged(isWfdEnabled());
            mResumed = true;
        }
    }
}
