/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
import com.android.mms.ui.ManageSimMessages;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import android.provider.Telephony;
/// M:
import android.util.Log;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;

/**
 * Receive Intent.SIM_FULL_ACTION.  Handle notification that SIM is full.
 */
public class SimFullReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (EncapsulatedSettings.Global.getInt(context.getContentResolver(),
            EncapsulatedSettings.Global.DEVICE_PROVISIONED, 0) == 1 &&
            Telephony.Sms.Intents.SIM_FULL_ACTION.equals(intent.getAction())) {

            NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            /// M:Code analyze 001,add a variable slotId for gemini mode @{
            Intent viewSimIntent;
            int slotId ;
            /// @}
            viewSimIntent = new Intent(context, ManageSimMessages.class);
            /// M:Code analyze 002,get the slot info for gemini @{
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                slotId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, EncapsulatedPhone.GEMINI_SIM_1);
                Log.d("SimFullReceiver", "slotId is " + slotId);
                viewSimIntent.putExtra("SlotId", slotId);
            }
            /// @}
            /// M:Code analyze 003, comment a line,don't show date to user @{
            //viewSimIntent.setAction(Intent.ACTION_VIEW);
            /// @}
            viewSimIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            /// M:Code analyze 004, change the last argument to update date @{
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, viewSimIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            /// @}

            Notification notification = new Notification();
            notification.icon = R.drawable.stat_sys_no_sim;
            /// M:Code analyze 005, add for gemini to get simId info @{
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(context, slotId);
                if (simInfo != null) {
                    notification.simId = simInfo.getSimId();
                    notification.simInfoType = 1;
                }
            }
            /// @}
            notification.tickerText = context.getString(R.string.sim_full_title);

            notification.defaults = Notification.DEFAULT_ALL;

            /// M:Code analyze 006, change the second argument @{
            notification.setLatestEventInfo(
                    context, notification.tickerText, 
                    context.getString(R.string.sim_full_body),
                    pendingIntent);
            /// @}
            nm.notify(ManageSimMessages.SIM_FULL_NOTIFICATION_ID, notification);
       }
    }

}
