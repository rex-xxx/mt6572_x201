package com.mediatek.contacts.util;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;

import com.android.contacts.ContactsApplication;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.quickcontact.QuickContactActivity;
import com.mediatek.contacts.ExtensionManager;

public class SetIndicatorUtils {

    private static final String TAG = "SetIndicatorUtils";
    private static final String PEOPLEACTIVITY = PeopleActivity.class.getName();
    private static final String QUICKCONTACTACTIVITY = QuickContactActivity.class.getName();
    private static SetIndicatorUtils sInstance;
    private StatusBarManager mStatusBarMgr;
    private boolean mShowSimIndicator;
    private BroadcastReceiver mReceiver = new MyBroadcastReceiver();
    private static final String INDICATE_TYPE = "CONTACTS";

    // In PeopleActivity, if quickContact is show, quickContactIsShow = true,
    // PeopleActivity.onPause cannot hide the Indicator.
    private boolean mQuickContactIsShow = false;

    private SetIndicatorUtils() {
        if (mStatusBarMgr == null) {
            mStatusBarMgr = (StatusBarManager) ContactsApplication.getInstance().getSystemService(
                    Context.STATUS_BAR_SERVICE);
        }
    }

    public static SetIndicatorUtils getInstance() {
        if (sInstance == null) {
            sInstance = new SetIndicatorUtils();
        }
        return sInstance;
    }

    public void showIndicator(boolean visible, Activity activity) {
        Log.i(TAG, "showIndicator visible : " + visible);
        mShowSimIndicator = visible;
        if (visible) {
            ExtensionManager.getInstance().getContactAccountExtension().switchSimGuide(activity,
                    INDICATE_TYPE, ContactPluginDefault.COMMD_FOR_AppGuideExt);
        }
        setSimIndicatorVisibility(visible, activity);
    }

    private void setSimIndicatorVisibility(boolean visible, Activity activity) {
        ComponentName componentName = null;
        String className = null;
        if (null != activity) {
            componentName = activity.getComponentName();
            if (null != componentName) {
                className = componentName.getClassName();
            }
        } else {
            // Receive the intent
            Log.i(TAG, "set compentantName is null");
        }
        if (visible) {
            mStatusBarMgr.showSimIndicator(componentName, Settings.System.VOICE_CALL_SIM_SETTING);
            if (QUICKCONTACTACTIVITY.equals(className)) {
                mQuickContactIsShow = true;
            }
        } else {
            if (QUICKCONTACTACTIVITY.equals(className)) {
                mQuickContactIsShow = false;
            }
            if (mQuickContactIsShow && PEOPLEACTIVITY.equals(className)) {
                Log.d(TAG, " no hide PEOPLEACTIVITY=" + PEOPLEACTIVITY);
            } else {
                mStatusBarMgr.hideSimIndicator(componentName);
            }

        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "CallDetailBroadcastReceiver, onReceive action = " + action);

            if (Intent.ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED.equals(action)) {
                Log.i(TAG, "mShowSimIndicator : " + mShowSimIndicator);
                if (mShowSimIndicator) {
                    setSimIndicatorVisibility(true, null);
                }
            }
        }
    }

    public void registerReceiver(Activity activity) {
        Log.i(TAG, "registerReceiver activity : " + activity);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED);
        activity.registerReceiver(mReceiver, intentFilter);
    }

    public void unregisterReceiver(Activity activity) {
        Log.i(TAG, "unregisterReceiver activity : " + activity);
        if (null != mReceiver) {
            activity.unregisterReceiver(mReceiver);
        }
    }
}
