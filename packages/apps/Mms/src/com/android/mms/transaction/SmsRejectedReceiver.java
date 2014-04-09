/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.mms.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import android.provider.Telephony;
import com.android.mms.ui.ConversationList;
/// M:
import android.widget.RemoteViews;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.mediatek.encapsulation.MmsLog;

/**
 * Receive Intent.SMS_REJECTED.  Handle notification that received SMS messages are being
 * rejected. This can happen when the device is out of storage.
 */
public class SmsRejectedReceiver extends BroadcastReceiver {

    public static final int SMS_REJECTED_NOTIFICATION_ID = 239;

    @Override
    public void onReceive(Context context, Intent intent) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "SmsRejectedReceiver: onReceive()  intent=" + intent);
        if (EncapsulatedSettings.Global.getInt(context.getContentResolver(),
                EncapsulatedSettings.Global.DEVICE_PROVISIONED, 0) == 1 &&
                Telephony.Sms.Intents.SMS_REJECTED_ACTION.equals(intent.getAction())) {

            int reason = intent.getIntExtra("result", -1);
            /// M:
            MmsLog.d(MmsApp.TXN_TAG, "Sms Rejected, reason=" + reason);
            boolean outOfMemory = reason == Telephony.Sms.Intents.RESULT_SMS_OUT_OF_MEMORY;
            if (!outOfMemory) {
                // Right now, the only user-level rejection we show to the user is out-of-memory.
                return;
            }

            NotificationManager nm = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent viewConvIntent = new Intent(context, ConversationList.class);
            viewConvIntent.setAction(Intent.ACTION_VIEW);
            viewConvIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            /// M:Code analyze 001, add for cmcc dir ui mode @{
            if (MmsConfig.getMmsDirMode()) {
                viewConvIntent.setClassName("com.android.mms", "com.android.mms.ui.FolderViewList");
            }
            /// @}
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, viewConvIntent, 0);

            Notification notification = new Notification();

            // TODO: need appropriate icons
            notification.icon = R.drawable.stat_sys_no_sim;
            int titleId;
            int bodyId;
            if (outOfMemory) {
                titleId = R.string.sms_full_title;
                bodyId = R.string.sms_full_body;
            } else {
                titleId = R.string.sms_rejected_title;
                bodyId = R.string.sms_rejected_body;
            }
            notification.tickerText = context.getString(titleId);
            notification.defaults = Notification.DEFAULT_ALL;
            /** M:
            notification.setLatestEventInfo(
                    context, context.getString(titleId),
                    context.getString(bodyId),
                    pendingIntent);
            */
            /// M:Code analyze 002, use customized notification layout instead of setLatestEventInfo,
            /// so the text can all be shown in the notification list@{
            RemoteViews contentView = new RemoteViews(context.getPackageName(),
                    R.layout.status_bar_sms_rejected);
            contentView.setImageViewResource(R.id.icon, R.drawable.stat_sys_no_sim);
            contentView.setTextViewText(R.id.title, context.getString(titleId));
            contentView.setTextViewText(R.id.text, context.getString(bodyId));
            notification.contentView = contentView;
            notification.contentIntent = pendingIntent;
            /// @}
            nm.notify(SMS_REJECTED_NOTIFICATION_ID, notification);
        }
    }

}
