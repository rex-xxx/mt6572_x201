/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.MediatekDM;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.data.IDmPersistentValues;
import com.mediatek.MediatekDM.util.DLProgressNotifier;

public class DmDownloadNotification {
    public static final int NOTIFICATION_NEW_VERSION = 1;
    public static final int NOTIFICATION_DOWNLOADING = 2;
    public static final int NOTIFICATION_DOWNLOAD_COMPLETED = 3;
    public static final int NOTIFICATION_NIA_RECEIVED = 4;

    public static final int NOTIFICATION_USERMODE_VISIBLE = 5;
    public static final int NOTIFICATION_USERMODE_INTERACT = 6;
    public static final int NOTIFICATION_NIA_START = 7;

    /**
     * Constructor
     * 
     * @param context Context object of android environment
     * @return
     */
    public DmDownloadNotification(Context context) {
        mNotificationContext = context;
        mNotificationManager = (NotificationManager) mNotificationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        listener = new NotificationListener();
        String opName = DmCommonFunction.getOperatorName();
        if (opName != null && opName.equalsIgnoreCase("cu")) {
            uiVisible = R.string.usermode_visible_cu;
        } else if (opName != null && opName.equalsIgnoreCase("cmcc")) {
            uiVisible = R.string.usermode_visible_cmcc;
        } else {
            uiVisible = R.string.usermode_visible_cu;
        }

    }

    public void showNiaNotification() {
        Log.w(TAG.Notification, "showNiaNotification begin");
        if (mNotificationType != NOTIFICATION_NIA_START) {
            clearDownloadNotification();
            mNotificationType = NOTIFICATION_NIA_START;
        }
        setNotificationType(R.drawable.stat_download_info, R.string.nia_info);
    }

    public void showUserModeNotification(int type) {
        if (mNotificationType != type) {
            clearDownloadNotification();
            mNotificationType = type;
        }
        setNotificationType(R.drawable.stat_download_info, uiVisible);
    }

    /**
     * For DmService to show the new version notification
     * 
     * @param null
     * @return null
     */
    public void showNewVersionNotification() {
        if (mNotificationType != NOTIFICATION_NEW_VERSION) {
            clearDownloadNotification();
            mNotificationType = NOTIFICATION_NEW_VERSION;
        }
        setNotificationType(R.drawable.stat_download_info,
                R.string.status_bar_notifications_new_version);
    }

    /**
     * For DmService to show the new download completed notification
     * 
     * @param null
     * @return null
     */
    public void showDownloadCompletedNotification() {
        Log.w(TAG.Notification, "showDownloadComletedNotification enter");
        if (mNotificationType != NOTIFICATION_DOWNLOAD_COMPLETED) {
            clearDownloadNotification();
            mNotificationType = NOTIFICATION_DOWNLOAD_COMPLETED;
        }
        setNotificationType(R.drawable.stat_download_complete,
                R.string.status_bar_notifications_download_completed);
    }

    /**
     * For DmService to clear the notification. When the notification is clicked
     * to start an activity, the notification should be cleared.
     * 
     * @param null
     * @return null
     */
    public void clearDownloadNotification() {
        Log.w(TAG.Notification, "clearDownloadNotification notification type is "
                + mNotificationType);
        mNotificationManager.cancel(mNotificationType);
        mNotification = null;

        if (mDLProgressNotifier != null) {
            mDLProgressNotifier.onFinish();
        }
    }

    private static int mNotificationType;
    private int mCurrentProgress = 0;
    private Context mNotificationContext;
    private static NotificationManager mNotificationManager;
    private static Notification mNotification;

    /**
     * Make the notification pending intent
     * 
     * @param null
     * @return the pending intent
     */
    private PendingIntent makeNotificationPendingIntent(int icon) {
        Log.w(TAG.Notification, "makeNotificationPendingIntent begin");
        Intent mNotificationIntent = new Intent(mNotificationContext, DmService.class);
        mNotificationIntent.setAction(DmConst.IntentAction.DM_DL_FOREGROUND);
        mNotificationIntent.putExtra("downloading", icon);
        PendingIntent mPendingIntent = PendingIntent.getService(mNotificationContext, 0,
                mNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    private PendingIntent makeNiaStartNotificationPendingIntent(int icon) {
        Log.w(TAG.Notification, "makeNiaStartPendingIntent begin");
        Intent mNotificationIntent = new Intent(mNotificationContext, DmNiInfoActivity.class);
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mNotificationIntent.putExtra("Type", DmConst.ServerMessage.TYPE_ALERT_1101);
        PendingIntent mPendingIntent = PendingIntent.getActivity(mNotificationContext, 0,
                mNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    private PendingIntent makeUserModeNotificationPendingIntent(int icon) {
        Log.w(TAG.Notification, "makeUserModeNotificationPendingIntent begin");
        Intent mNotificationIntent = new Intent(mNotificationContext, DmNiInfoActivity.class);
        mNotificationIntent.setAction("com.mediatek.MediatekDM.SHOWDIALOG");
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (mNotificationType == NOTIFICATION_USERMODE_VISIBLE) {
            mNotificationIntent.putExtra("Type", DmConst.ServerMessage.TYPE_UIMODE_VISIBLE);
        } else if (mNotificationType == NOTIFICATION_USERMODE_INTERACT) {
            mNotificationIntent.putExtra("Type", DmConst.ServerMessage.TYPE_UIMODE_INTERACT);
        }

        PendingIntent mPendingIntent = PendingIntent.getActivity(mNotificationContext, 0,
                mNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    /**
     * According the notification icon and string to set the notification type
     * 
     * @param icon , notification content
     * @return null
     */
    private void setNotificationType(int downloadIcon, int notificationString) {
        CharSequence notificationText = mNotificationContext.getText(notificationString);
        String notificationContent = mNotificationContext.getString(notificationString);
        // Set the icon, scrolling text and timestamp

        // if the typeId is for downing and set the progress bar
        if (mNotification == null) {
            mNotification = new Notification(downloadIcon, notificationContent,
                    System.currentTimeMillis());
        }

        if (downloadIcon == R.drawable.stat_download_waiting) {
            // do nothing
        } else {
            CharSequence title = "";
            PendingIntent pendingIntent = null;
            switch (mNotificationType) {
                case NOTIFICATION_NIA_START:
                    mNotification.flags = Notification.FLAG_AUTO_CANCEL;
                    title = mNotificationContext.getText(R.string.app_name);
                    pendingIntent = makeNiaStartNotificationPendingIntent(downloadIcon);
                    break;
                case NOTIFICATION_USERMODE_VISIBLE:
                case NOTIFICATION_USERMODE_INTERACT:
                    mNotification.flags = Notification.FLAG_AUTO_CANCEL;
                    title = mNotificationContext.getText(R.string.app_name);
                    pendingIntent = makeUserModeNotificationPendingIntent(downloadIcon);
                    break;
                case NOTIFICATION_NEW_VERSION:
                case NOTIFICATION_DOWNLOAD_COMPLETED:
                    mNotification.flags = Notification.FLAG_AUTO_CANCEL;
                    title = mNotificationContext.getText(R.string.status_bar_notifications_title);
                    pendingIntent = makeNotificationPendingIntent(downloadIcon);
                    break;
                default:
                    break;
            }
            mNotification.setLatestEventInfo(mNotificationContext, title, notificationText,
                    pendingIntent);
            mNotification.defaults = Notification.DEFAULT_ALL;
        }

        // Send the notification.
        showNotification();
    }

    /**
     * show notification
     * 
     * @param null
     * @return null
     */
    private void showNotification() {
        Log.v(TAG.Notification, "showNotification, id is " + mNotificationType);
        if (mNotificationManager == null) {
            Log.w(TAG.Notification, "showNotification mNotificationManager is null");
            return;
        }
        mNotificationManager.notify(mNotificationType, mNotification);
    }

    public void setFlag(int flag) {
        if (mNotification != null) {
            mNotification.flags = flag;
        }
    }

    public void setDmStatus(int status) {
        if (status == -1) {
            return;
        }
        dmStatus = status;
    }

    class NotificationListener implements FumoUpdateListener {
        public void onUpdate(Message msg) {
            Log.d(TAG.Notification, "[NotificationListener]:onupdate(" + msg);
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case IDmPersistentValues.MSG_NEWVERSIONDETECTED:
                    Log.d(TAG.Notification, "[NotificationListener]:new version detected!");
                    if (DmService.getServiceInstance() != null) {
                        int sessionInitor = DmService.getServiceInstance().getSessionInitor();
                        if (sessionInitor == IDmPersistentValues.SERVER) {
                            Log.d(TAG.Notification, "----server inited session, show notif----");
                            showNewVersionNotification();
                        } else if (sessionInitor == IDmPersistentValues.CLIENT_POLLING) {
                            Log.d(TAG.Notification, "----client polling session, show notif----");
                            showNewVersionNotification();
                        } else {
                            Log.d(TAG.Notification, "----user pull session, don't show notif----");
                        }
                    }
                    break;
                case IDmPersistentValues.MSG_DLPKGUPGRADE:
                    if (mNotificationType != NOTIFICATION_DOWNLOADING) {
                        clearDownloadNotification();
                        mNotificationType = NOTIFICATION_DOWNLOADING;
                    }
                    Log.d(TAG.Notification, "dmStatus is " + dmStatus);
                    if (dmStatus == IDmPersistentValues.STATE_DOWNLOADING
                            || dmStatus == IDmPersistentValues.STATE_RESUMEDOWNLOAD) {
                        if (mDLProgressNotifier == null) {
                            mDLProgressNotifier = new DLProgressNotifier(mNotificationContext,
                                    makeNotificationPendingIntent(R.drawable.stat_download_waiting));
                        }
                        mDLProgressNotifier.onProgressUpdate(msg.arg1, msg.arg2);
                    }
                    break;
                case IDmPersistentValues.MSG_DLPKGCOMPLETE:
                    showDownloadCompletedNotification();
                    break;
                case IDmPersistentValues.MSG_NIASESSION_START:
                    showNiaNotification();
                    break;
                case IDmPersistentValues.MSG_USERMODE_VISIBLE:
                    Log.w(TAG.Notification,
                            "DownloadNotification receive a message, message is MSG_USERMODE_VISIBLE");
                    showUserModeNotification(NOTIFICATION_USERMODE_VISIBLE);
                    break;
                case IDmPersistentValues.MSG_USERMODE_INTERACT:
                    showUserModeNotification(NOTIFICATION_USERMODE_INTERACT);
                    break;
                default:
                    break;
            }
            Log.d(TAG.Notification, "new mNotificationType is " + mNotificationType);
        }

        public void syncStatus(int status) {
            // TODO Auto-generated method stub
            if (status < 0) {
                return;
            }
            dmStatus = status;
        }

        public void syncDmSession(int status) {
            // TODO Auto-generated method stub

        }

    }

    public NotificationListener getListener() {
        return listener;
    }

    private static int dmStatus = -1;
    private NotificationListener listener = null;
    private static Integer uiVisible = null;

    // to show download progress in ICS style.
    private DLProgressNotifier mDLProgressNotifier = null;
}
