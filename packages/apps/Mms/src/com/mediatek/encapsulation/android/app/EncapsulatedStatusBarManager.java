
package com.mediatek.encapsulation.android.app;

import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.IBinder;
import android.view.View;
import android.app.StatusBarManager;

import com.mediatek.encapsulation.EncapsulationConstant;

/**
 * Allows an app to control the status bar.
 * 
 * @hide
 */
public class EncapsulatedStatusBarManager {

    /** M: MTK ADD */
    private static StatusBarManager mStatusBarManager;

    // private static final String TAG = "EncapsulatedStorageManager";

    /** M: MTK ADD */
    public EncapsulatedStatusBarManager(Context context) {
        if (null == mStatusBarManager && null != context) {
            mStatusBarManager = (StatusBarManager) context
                    .getSystemService(Context.STATUS_BAR_SERVICE);
        }
    }

    /** M: Support "SystemUI SIM indicator" feature. @{ */
    /** M: MTK ADD */
    public void showSIMIndicator(ComponentName componentName, String businessType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mStatusBarManager.showSimIndicator(componentName, businessType);
        } else {
            /** M:can't complete this branch,need to access private methods */
        }
    }

    /** M: MTK ADD */
    public void hideSIMIndicator(ComponentName componentName) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mStatusBarManager.hideSimIndicator(componentName);
        } else {
            /** M:can't complete this branch,need to access private methods */
        }
    }

    /** }@ */
}
