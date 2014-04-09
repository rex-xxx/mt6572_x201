/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.exchange.service;

import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.emailcommon.Logging;
import com.android.exchange.Eas;
import com.android.exchange.ExchangePreferences;
import com.android.exchange.ExchangeService;

/**
 * The service that really handles broadcast intents on a worker thread.
 *
 * We make it a service, because:
 * <ul>
 *   <li>So that it's less likely for the process to get killed.
 *   <li>Even if it does, the Intent that have started it will be re-delivered by the system,
 *   and we can start the process again.  (Using {@link #setIntentRedelivery}).
 * </ul>
 */
public class ExchangeBroadcastProcessorService extends IntentService {
    // Action used for BroadcastReceiver entry point
    private static final String ACTION_BROADCAST = "broadcast_receiver";
    
    private ExchangePreferences mPref = null;

    public ExchangeBroadcastProcessorService() {
        // Class name will be the thread name.
        super(ExchangeBroadcastProcessorService.class.getName());
        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    private ExchangePreferences getExchangePreferences() {
        if (mPref == null) {
            mPref = ExchangePreferences.getPreferences(this);
        }
        return mPref;
    }

    /**
     * Entry point for {@link ExchangeBroadcastReceiver}.
     */
    public static void processBroadcastIntent(Context context, Intent broadcastIntent) {
        Intent i = new Intent(context, ExchangeBroadcastProcessorService.class);
        i.setAction(ACTION_BROADCAST);
        i.putExtra(Intent.EXTRA_INTENT, broadcastIntent);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Dispatch from entry point
        final String action = intent.getAction();
        if (ACTION_BROADCAST.equals(action)) {
            final Intent broadcastIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            final String broadcastAction = broadcastIntent.getAction();

            if (Intent.ACTION_BOOT_COMPLETED.equals(broadcastAction)) {
                onBootCompleted();
            } else if (AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION.equals(broadcastAction)) {
                if (Eas.USER_LOG) {
                    Log.d(Logging.LOG_TAG, "Login accounts changed; reconciling...");
                }
                ExchangeService.runAccountReconcilerSync(this);
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(broadcastAction)) {
                ExchangeService.alwaysLog("Receive STORAGE_LOW broadcast , " +
                        "and ExchangeService will stop work");
                getExchangePreferences().setLowStorage(true);
                stopExchangeService();
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(broadcastAction)) {
                ExchangeService.alwaysLog("Receive STORAGE_OK broadcast , " +
                        "and ExchangeService will start work");
                getExchangePreferences().setLowStorage(false);
                startExchangeService();
            }
       }
    }

    private void startExchangeService() {
        startService(new Intent(this, ExchangeService.class));
    }

    private void stopExchangeService() {
        stopService(new Intent(this, ExchangeService.class));
    }

    /**
     * Handles {@link Intent#ACTION_BOOT_COMPLETED}.  Called on a worker thread.
     */
    private void onBootCompleted() {
        startExchangeService();
    }
}
