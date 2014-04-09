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

// save secure clock state & value to file when devices shuts down normally.
public class ShutDownReceiver extends BroadcastReceiver {
private static final String TAG = "DRM/ShutDownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive : ACTION_SHUTDOWN received.");
        if (FeatureOption.MTK_DRM_APP) {
            new SaveClockTask().execute(context);
        }
    }

    // to avoid block when power off, move "save clock" to AsyncTask
    private class SaveClockTask extends AsyncTask<Context, Void, Integer> {

        @Override
        protected Integer doInBackground(Context... params) {
            Log.d(TAG, "SaveClockTask : start to save clock in SaveClockTask.");
            Context context = params[0];
            OmaDrmClient client = new OmaDrmClient(context);
            return OmaDrmHelper.saveClock(client);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "SaveClockTask : save clock finished in SaveClockTask: "+ result.toString());
        }

    }
}
