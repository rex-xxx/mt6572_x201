package com.mediatek.hdmi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;

public class HDMIReceiver extends BroadcastReceiver {
    private static final String ACTION_IPO_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final String ACTION_IPO_BOOT = "android.intent.action.ACTION_BOOT_IPO";
    private static final String TAG = "hdmi";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent serviceIntent = new Intent(context, HDMILocalService.class);
        if (!FeatureOption.MTK_HDMI_SUPPORT) {
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.ACTION_BOOT_IPO".equals(action)) {
            Log.i(TAG, "HDMIReceiver >> get boot/IPO completed broadcast, "
                    + "start HDMI local service");
            Log.d(TAG, "  Bootup action=" + action);
            serviceIntent.putExtra("bootup_action", action);
            context.startService(serviceIntent);
        } else if (ACTION_IPO_SHUTDOWN.equals(action)) {
            Log.i(TAG,
                    "HDMIReceiver >> IPO shut down, stop HDMI local service, "
                            + "have no effect now");
        }
    }
}
