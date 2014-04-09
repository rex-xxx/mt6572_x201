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

package com.mediatek.MediatekDM;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;

public class DmScomoNotification implements DmScomoUpdateListener {
    public static final int NOTIFICATION = 11;

    private Context mContext;
    private NotificationManager mNotificationManager;

    private PendingIntent mDownloadDetailPendingIntent;

    public void onScomoUpdated() {
        DmScomoState scomoState = DmService.getInstance().getScomoState();
        int state = scomoState.state;
        Log.i(TAG.Scomo, "notification onUpdate: " + state);
        if (state == DmScomoState.DOWNLOADING) {
            CharSequence tickerText = mContext.getText(R.string.downloading_scomo);
            String text = "";
            if (scomoState.totalSize == 0) {
                text = "NaN";
            } else {
                text = scomoState.currentSize * 100 / scomoState.totalSize + "%";
            }
            CharSequence tvText = mContext.getResources().getString(R.string.downloading,
                    scomoState.getName());
            notifyDownloadNotification(tickerText, text, tvText, scomoState.currentSize,
                    scomoState.totalSize);
        } else if (state == DmScomoState.DOWNLOADING_STARTED || state == DmScomoState.RESUMED) {
            CharSequence tickerText = mContext.getText(R.string.downloading_scomo);
            CharSequence contentText = scomoState.currentSize / 1024 + "KB/" + scomoState.totalSize
                    / 1024 + "KB";
            CharSequence contentTitle = mContext.getText(R.string.connecting);
            notifyDownloadNotification(tickerText, contentTitle, contentText,
                    scomoState.currentSize,
                    scomoState.totalSize);
        } else if (state == DmScomoState.PAUSED) {
            CharSequence tickerText = mContext.getText(R.string.scomo_downloading_paused);
            CharSequence contentText = scomoState.currentSize / 1024 + "KB/" + scomoState.totalSize
                    / 1024 + "KB";
            CharSequence contentTitle = mContext.getText(R.string.scomo_downloading_paused);
            notifyDownloadNotification(tickerText, contentTitle, contentText,
                    scomoState.currentSize,
                    scomoState.totalSize);
        } else if (state == DmScomoState.CONFIRM_UPDATE) {
            CharSequence tickerText = mContext.getText(R.string.confirm_update_scomo);
            CharSequence contentText = mContext.getText(R.string.confirm_update_scomo);
            CharSequence contentTitle = mContext.getText(R.string.click_for_detail);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
        } else if (state == DmScomoState.DOWNLOAD_FAILED) {
            // CharSequence tickerText =
            // mContext.getText(R.string.scomo_downloading_failed);
            // CharSequence contentText =
            // mContext.getText(R.string.downloading_scomo);
            // CharSequence contentTitle =
            // mContext.getText(R.string.scomo_downloading_failed);
            // notifyConfirmNotification(tickerText, contentTitle, contentText,
            // state);

            Intent intent = new Intent(mContext, DmScomoConfirmActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("action", state);
            mContext.startActivity(intent);
        } else if (state == DmScomoState.INSTALL_FAILED) {
            CharSequence tickerText = mContext.getText(R.string.scomo_install_failed);
            CharSequence contentText = mContext.getText(R.string.software_update);
            CharSequence contentTitle = mContext.getText(R.string.scomo_install_failed);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
        } else if (state == DmScomoState.INSTALL_OK) {
            CharSequence tickerText = mContext.getText(R.string.scomo_install_ok);
            CharSequence contentText = mContext.getText(R.string.software_update);
            CharSequence contentTitle = mContext.getText(R.string.scomo_install_ok);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
        } else if (state == DmScomoState.CONFIRM_DOWNLOAD) {
            CharSequence tickerText = mContext.getText(R.string.new_scomo_available);
            CharSequence contentText = mContext.getString(R.string.new_scomo_available);
            CharSequence contentTitle = scomoState.getName();
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
        } else if (state == DmScomoState.IDLE || state == DmScomoState.ABORTED) {
            Log.d(TAG.Scomo, "state is idle or aborted");
            mNotificationManager.cancel(NOTIFICATION);
        } else if (state == DmScomoState.GENERIC_ERROR) {
            CharSequence tickerText = mContext.getText(R.string.scomo_failed);
            CharSequence contentText = mContext.getText(R.string.software_update);
            CharSequence contentTitle = mContext.getText(R.string.scomo_failed);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
        } else if (state == DmScomoState.INSTALLING) {
            Intent intent = new Intent(mContext, DmScomoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
            Notification.Builder notifyBuilder = new Notification.Builder(mContext)
                    .setOngoing(true)
                    .setSmallIcon(
                            R.drawable.stat_download_waiting)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(
                            pendingIntent)
                    .setTicker(mContext.getText(R.string.installing_scomo))
                    .setContentTitle(
                            mContext.getText(R.string.installing_scomo))
                    .setContentText(
                            mContext.getResources().getString(R.string.installing,
                                    scomoState.getName()));
            mNotificationManager.notify(NOTIFICATION, notifyBuilder.getNotification());
        } else if (state == DmScomoState.UPDATING) {
        }
    }

    private void notifyDownloadNotification(CharSequence tickerText, CharSequence contentTitle,
            CharSequence contentText, int currentSize, int totalSize) {
        int progress = (int) ((float) currentSize / (float) totalSize * 100);
        Notification.Builder notifyBuilder = new Notification.Builder(mContext).setOngoing(true)
                .setSmallIcon(
                        R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                .setContentIntent(
                        mDownloadDetailPendingIntent).setTicker(tickerText)
                .setContentTitle(contentTitle).setContentText(
                        contentText).setProgress(100, progress, false);
        mNotificationManager.notify(NOTIFICATION, notifyBuilder.getNotification());
    }

    private void notifyConfirmNotification(CharSequence tickerText, CharSequence contentTitle,
            CharSequence contentText, int state) {
        Intent intent = new Intent(mContext, DmScomoConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("action", state);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder notifyBuilder = new Notification.Builder(mContext).setAutoCancel(true)
                .setSmallIcon(
                        R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setTicker(tickerText).setContentTitle(contentTitle).setContentText(contentText);
        mNotificationManager.notify(NOTIFICATION, notifyBuilder.getNotification());
    }

    public DmScomoNotification(Context context) {
        this.mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, DmScomoDownloadDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mDownloadDetailPendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
