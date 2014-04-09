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

package com.android.exchange;

import android.app.Application;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.util.LogPrinter;

import com.android.emailcommon.Logging;

public class Exchange extends Application {
    public static final String TAG = "SpendTime";
    public static final int NO_BSK_MAILBOX = -1;
    /// M: The bad sync key mailbox id. At present just suppose at
     // most only 1 mailbox may occurs bad sync key at the same time
    public static long sBadSyncKeyMailboxId = NO_BSK_MAILBOX;

    // TODO Investigate whether this class is needed
    @Override
    public void onCreate() {
        long startTime = System.currentTimeMillis();
        Logging.d(TAG, "ExchangeApp onCreate() start time: " + startTime);
        super.onCreate();
        if(Build.TYPE.equals("eng")){
            Looper.myLooper().setMessageLogging(new LogPrinter(Log.DEBUG, "ExchangeApp"));
        }
        Logging.d(TAG, "ExchangeApp onCreate() end time: " + System.currentTimeMillis());
        Logging.d(TAG, "ExchangeApp onCreate() spend time: "
                + (System.currentTimeMillis() - startTime));

        /** M: This is to check if the bad sync key had ever happened
        and its recovery process was halted by Exchange process crash
        or device rebooting etc. @{ */
        ExchangePreferences pref = ExchangePreferences.getPreferences(this);
        sBadSyncKeyMailboxId = pref.getBadSyncKeyMailboxId();
        if (sBadSyncKeyMailboxId != NO_BSK_MAILBOX) {
            ExchangeService.alwaysLog("[BSK recovery] Unfinished Bad sync key recovery detected," +
                    " mailbox id: " + sBadSyncKeyMailboxId);
        }
        /** @} */
    }
}
