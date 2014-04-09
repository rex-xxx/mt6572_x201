/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.MediatekDM.polling;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;

import com.mediatek.MediatekDM.util.FileLogger;

public class PollingService extends IntentService {
    private static final String TAG = "PollingService";

    public static final String ACTION = "com.mediatek.MediatekDM.polling_action";

    private static final String LOCK_NAME_STATIC = "com.mediatek.MediatekDM.polling_service";
    private static volatile PowerManager.WakeLock lockStatic = null;

    private BroadcastReceiver connReceiver = null;
    private boolean hasTriggered = false;

    public PollingService() {
        super("PollingService");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onStartCommand()");
        getLock(getApplicationContext()).acquire();
        FileLogger.getInstance(this).logMsg("+++wake lock acquired.");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Log.d(TAG, "===>onHandleIntent()");
            if (intent == null) {
                Log.e(TAG, "---intent is null.");
                return;
            }
            FileLogger.getInstance(this).logMsg("====>alarming.");

            String action = intent.getAction();
            FileLogger.getInstance(this).logMsg("polling service action=" + action);
            if (action != null) {
                try {
                    // wait 60s for network
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connMgr != null) {
                    NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
                    if (netInfo != null && netInfo.isConnected()) {
                        triggerPolling();
                        return;
                    }
                }

                // otherwise, wait for network ready
                FileLogger.getInstance(this).logMsg(">>wait for network ready...");
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                connReceiver = new ConnReceiver();
                this.registerReceiver(connReceiver, filter);
                Log.d(TAG, "++++++++connetivity receiver registered.");
            }

        } finally {
            getLock(this.getApplicationContext()).release();
            FileLogger.getInstance(this).logMsg("---wake lock released.");
            FileLogger.getInstance(this).logMsg(
                    "wake lock isHeld=" + getLock(this.getApplicationContext()).isHeld());
        }
    }

    private void triggerPolling() {
        // broadcast FUMO polling request...
        Log.d(TAG, "[[[[ trigger FUMO polling ]]]]");
        FileLogger.getInstance(this).logMsg("[[[[ trigger FUMO polling ]]]]");
        Intent fumoIntent = new Intent();
        fumoIntent.setAction("com.mediatek.MediatekDM.FUMO_CI");

        sendBroadcast(fumoIntent);

        // set next polling alarm
        PollingScheduler.getInstance(this).setNextAlarm();
    }

    private void unregisterReceiver() {
        if (connReceiver != null) {
            this.unregisterReceiver(connReceiver);
            Log.d(TAG, "--------connectivity receiver unregistered.");
        }
    }

    private synchronized boolean aquireTrigger() {
        if (!hasTriggered) {
            hasTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    private static synchronized PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);

            // reference counted Wake lock, to supply multi-requests.
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }

    private class ConnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                return;
            }

            String action = intent.getAction();
            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d(TAG, "[ConnReceiver]: got connectivity change event!");

                ConnectivityManager connMgr = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isConnected()) {
                    // cause there'll be more than 1 broadcasts coming at
                    // same time
                    if (aquireTrigger()) {
                        Log.i(TAG, "[ConnReceiver]: network connected, trigger polling.");

                        // trigger it.
                        triggerPolling();

                        // unregister self
                        unregisterReceiver();
                    } else {
                        Log.d(TAG, "[ConnReceiver]: already triggered, bypass.");
                    }
                }
            }
        }
    }

}
