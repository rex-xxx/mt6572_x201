/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.mms.transaction;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Telephony.Threads;

import com.android.mms.MmsApp;
import com.android.mms.ui.DialogModeActivity;
import com.android.mms.ui.ManageSimMessages;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.NotificationPreferenceActivity;
import com.android.mms.widget.MmsWidgetProvider;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.NewMessageAction;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;
import com.mediatek.encapsulation.MmsLog;

import java.util.List;


/**
 * NotificationReceiver receives some kinds of ip message notification
 * and notify the listeners.
 */
public class IpMessageReceiver extends BroadcastReceiver {
    private static final String TAG = "IpMessageReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        MmsLog.d(TAG, "onReceive: " +"Action = " + intent.getAction());
        new ReceiveNotificationTask(context).execute(intent);
    }

    private class ReceiveNotificationTask extends AsyncTask<Intent, Void, Void> {
        private Context mContext;
        ReceiveNotificationTask(Context context) {
            mContext = context;
        }
        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];
            String action = intent.getAction();
            if (NewMessageAction.ACTION_NEW_MESSAGE.equals(action)) {
                handleNewMessage(intent);
            } else if (IpMessageConsts.ACTION_GROUP_NOTIFICATION_CLICKED.equals(action)) {
                handleGroupNotificationClicked(intent);
            } else if (IpMessageConsts.ACTION_UPGRADE.equals(action)) {
                handleUpgrade();
            } else {
                MmsLog.w(TAG, "unknown notification type.");
            }

            return null;
        }

        private void handleNewMessage(Intent intent) {
            MmsLog.d(TAG, "handleNewMessage");
            long messageId = intent.getLongExtra(NewMessageAction.IP_MESSAGE_KEY, 0);
            if (messageId <= 0) {
                MmsLog.e(TAG, "get ip message failed.");
                return;
            }
            MmsLog.d(TAG, "new message id:" + messageId);
            IpMessage ipMessage = IpMessageUtils.getMessageManager(mContext).getIpMsgInfo(messageId);
            MmsLog.d(TAG, "\t id:" + ipMessage.getId());
            MmsLog.d(TAG, "\t ipdbid:" + ipMessage.getIpDbId());
            MmsLog.d(TAG, "\t from:" + ipMessage.getFrom());
            MmsLog.d(TAG, "\t to:" + ipMessage.getTo());
            MmsLog.d(TAG, "\t simId:" + ipMessage.getSimId());
            MmsLog.d(TAG, "\t type:" + ipMessage.getType());

            String from = IpMessageUtils.getContactManager(mContext).getNumberByMessageId(messageId);
            MmsLog.d(TAG, "\t displyFrom:" + from);
            long threadId = Threads.getOrCreateThreadId(mContext, from);
            MmsLog.d(TAG, "\t threadid:" + threadId);
            MessagingNotification.blockingUpdateNewMessageIndicator(mContext, threadId, false);
            if (NotificationPreferenceActivity.isPopupNotificationEnable()) {
                notifyNewIpMessageDialog(messageId);
            }
            MmsWidgetProvider.notifyDatasetChanged(mContext);
        }

        private void handleGroupNotificationClicked(Intent intent) {
            MmsLog.d(TAG, "handleGroupNotificationClicked");
            long threadId = intent.getLongExtra(RemoteActivities.KEY_THREAD_ID, 0);
            boolean isImportant = intent.getBooleanExtra(RemoteActivities.KEY_BOOLEAN, false);

            Intent it = new Intent(RemoteActivities.CHAT_DETAILS_BY_THREAD_ID);
            it.putExtra(RemoteActivities.KEY_THREAD_ID, threadId);
            it.putExtra(RemoteActivities.KEY_BOOLEAN, isImportant);
            IpMessageUtils.startRemoteActivity(mContext, it);
        }

        private void handleUpgrade() {
            MessagingNotification.cancelNotification(mContext, MessagingNotification.NOTIFICATION_ID);
            MessagingNotification.cancelNotification(mContext, MessagingNotification.CLASS_ZERO_NOTIFICATION_ID);
            MessagingNotification.cancelNotification(mContext, MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
            MessagingNotification.cancelNotification(mContext, MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
            MessagingNotification.cancelNotification(mContext, SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);
            MessagingNotification.cancelNotification(mContext, ManageSimMessages.SIM_FULL_NOTIFICATION_ID);
            CBMessagingNotification.cancelNotification(mContext, CBMessagingNotification.NOTIFICATION_ID);

            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

            List<RunningTaskInfo> rti = am.getRunningTasks(2);
            RunningTaskInfo infoFst = rti.get(0);
            String packageNameFst = infoFst.topActivity.getPackageName();
            String classNameFst = infoFst.topActivity.getClassName();

            RunningTaskInfo infoSnd = rti.get(1);
            String packageNameSnd = infoSnd.topActivity.getPackageName();
            String classNameSnd = infoSnd.topActivity.getClassName();

            MmsLog.d(MmsApp.TXN_TAG, "\t task_0 id:" + infoFst.id);
            MmsLog.d(MmsApp.TXN_TAG, "\t packageName_0:" + packageNameFst);
            MmsLog.d(MmsApp.TXN_TAG, "\t className_0:" + classNameFst);
            MmsLog.d(MmsApp.TXN_TAG, "\t task_1 id:" + infoSnd.id);
            MmsLog.d(MmsApp.TXN_TAG, "\t packageName_1:" + packageNameSnd);
            MmsLog.d(MmsApp.TXN_TAG, "\t className_1:" + classNameSnd);

            if (packageNameFst != null && packageNameFst.equals("com.android.mms")) {
                MmsLog.d(MmsApp.TXN_TAG, "need move " + packageNameSnd + " to front.");
                am.moveTaskToFront(infoSnd.id, 0);
            }

            MmsLog.d(MmsApp.TXN_TAG, "kill process");
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    //Dialog mode
    private void notifyNewIpMessageDialog(long id) {
        MmsLog.d(TAG, "notifyNewIpMessageDialog,id:" + id);

        if (MessageUtils.isHome()) {
            MmsLog.d(TAG, "at launcher");
            Context context = MmsApp.getApplication();
            Intent smsIntent = new Intent(context, DialogModeActivity.class);
            Uri smsUri = Uri.parse("content://sms/" + id);
            smsIntent.putExtra("com.android.mms.transaction.new_msg_uri", smsUri.toString());
            smsIntent.putExtra("ipmessage", true);
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(smsIntent);
        } else {
            MmsLog.d(TAG, "not at launcher");
        }
    }
}

