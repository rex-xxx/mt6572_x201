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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;

public class DrmService extends Service {
    private static final String TAG = "DrmService";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart");

        Resources res = this.getResources();
        int saveRightsResult = intent.getIntExtra("saveRights-result", OmaDrmClient.ERROR_UNKNOWN);
        String text = (saveRightsResult == OmaDrmClient.ERROR_NONE) ?
            res.getString(com.mediatek.internal.R.string.drm_license_install_success) :
            res.getString(com.mediatek.internal.R.string.drm_license_install_fail);

        NotificationManager nm =
            (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =
            new Notification(com.mediatek.internal.R.drawable.drm_stat_notify_wappush,
                    text, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        Intent i = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);
        notification.setLatestEventInfo(this, text, text, pendingIntent);

        nm.notify(0, notification);

        stopSelf();
    }
}

