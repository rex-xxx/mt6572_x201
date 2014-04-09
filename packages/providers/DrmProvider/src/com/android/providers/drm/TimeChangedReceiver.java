/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.providers.drm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;

// when user changes devices time, update the offset between device time and real world time.
public class TimeChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "DRM/TimeChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive : TIME_SET received.");
        if (FeatureOption.MTK_DRM_APP) {
            new UpdateTimeOffsetTask().execute(context);
        }
    }

    // to avoid block when time changed, move "update time offset" to AsyncTask
    private class UpdateTimeOffsetTask extends AsyncTask<Context, Void, Integer> {

        @Override
        protected Integer doInBackground(Context... params) {
            Log.d(TAG, "UpdateTimeOffsetTask : start to update time offset.");
            Context context = params[0];
            OmaDrmClient client = new OmaDrmClient(context);
            return OmaDrmHelper.updateOffset(client);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "UpdateTimeOffsetTask : update time offset finished in UpdateTimeOffsetTask: " + result.toString());
        }

    }
}
