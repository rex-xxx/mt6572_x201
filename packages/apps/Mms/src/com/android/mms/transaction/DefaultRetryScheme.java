/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.transaction;

import android.content.Context;
import android.util.Config;
import android.util.Log;

/// M: CMCC need change retry scheme, so move sDefaultRetryScheme to plugin
import com.android.mms.MmsConfig;

/**
 * Default retry scheme, based on specs.
 */
public class DefaultRetryScheme extends AbstractRetryScheme {
    private static final String TAG = "DefaultRetryScheme";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    /// M:  CMCC need change retry scheme, so move sDefaultRetryScheme to plugin
    /*
    private static final int[] sDefaultRetryScheme = {
        0, 1 * 60 * 1000, 5 * 60 * 1000, 10 * 60 * 1000, 30 * 60 * 1000};
    */
    private static int[] sRetryScheme = null;
    /// @}

    public DefaultRetryScheme(Context context, int retriedTimes) {
        super(retriedTimes);

        /// M:  CMCC need change retry scheme, so move sDefaultRetryScheme to plugin
        if (sRetryScheme == null) {
            sRetryScheme = MmsConfig.getMmsRetryScheme();
        }

        mRetriedTimes = mRetriedTimes < 0 ? 0 : mRetriedTimes;
        mRetriedTimes = mRetriedTimes >= sRetryScheme.length
                ? sRetryScheme.length - 1 : mRetriedTimes;
        /// @}
        // TODO Get retry scheme from preference.
    }

    @Override
    public int getRetryLimit() {
        /// M:  CMCC need change retry scheme, so move sDefaultRetryScheme to plugin
        Log.d(TAG, "getRetryLimit, " + sRetryScheme.length);
        return sRetryScheme.length;
        /// @}
    }

    @Override
    public long getWaitingInterval() {
        /// M:  CMCC need change retry scheme, so move sDefaultRetryScheme to plugin
        if (LOCAL_LOGV) {
            Log.v(TAG, "Next int: " + sRetryScheme[mRetriedTimes]);
        }
        return sRetryScheme[mRetriedTimes];
        /// @}
    }
}
