/*
 * Copyright (C) 2010 Google Inc.
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

package com.android.systemui.usb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Slog;
import com.mediatek.xlog.SXlog;
import java.util.HashSet;
import android.os.SystemProperties;
import android.hardware.usb.UsbManager;

public class StorageNotification extends StorageEventListener {
    private static final String TAG = "StorageNotification";

    private static final boolean POP_UMS_ACTIVITY_ON_CONNECT = true;

    /**
     * Binder context for this service
     */
    private Context mContext;

    /**
     * The notification that is shown when a USB mass storage host
     * is connected.
     * <p>
     * This is lazily created, so use {@link #setUsbStorageNotification()}.
     */
    private Notification mUsbStorageNotification;

    /**
     * The notification that is shown when the following media events occur:
     *     - Media is being checked
     *     - Media is blank (or unknown filesystem)
     *     - Media is corrupt
     *     - Media is safe to unmount
     *     - Media is missing
     * <p>
     * This is lazily created, so use {@link #setMediaStorageNotification()}.
     */
    private Notification   mMediaStorageNotification;
    private Notification   mMediaStorageNotificationForExtStorage;
    private Notification   mMediaStorageNotificationForExtUsbOtg;
    private boolean        mUmsAvailable;
    private StorageManager mStorageManager;
    private HashSet        mUsbNotifications;
    private String         mLastState;
    private boolean        mLastConnected;
    private boolean        mAlarmBootOff = false;
    private boolean        mIsLastVisible = false;

    private static int notifyid = 0;
    private Handler        mAsyncEventHandler;

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

				if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
            		SXlog.d(TAG, "onReceive [ACTION_SHUTDOWN_IPO] - [Clear mUsbNotifications]");
					mUsbNotifications.clear();
				}

				if (action.equals("android.intent.action.normal.boot.done")) {
        			final boolean connected = mStorageManager.isUsbMassStorageConnected();
            		SXlog.d(TAG, "onReceive [ACTION_NORMAL_BOOT] - connected: " + connected);
					mAlarmBootOff = true;
        			onUsbMassStorageConnectionChanged(connected);
				}
		}
	};

    public StorageNotification(Context context) {
        mContext = context;

        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        final boolean connected = mStorageManager.isUsbMassStorageConnected();

		/* find any storage is unmountable. If yes, show it. */
        String st = "";
        String path = "";
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (int i=0; i<volumes.length; i++) {
            if (volumes[i].allowMassStorage() && !volumes[i].isEmulated()) {
				path = volumes[i].getPath();
				st = mStorageManager.getVolumeState(path);
            }
        }
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.normal.boot.done");
		filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
		mContext.registerReceiver(mIntentReceiver, filter);

        Slog.d(TAG, String.format( "Startup with UMS connection %s (media state %s)", mUmsAvailable, st));

        HandlerThread thr = new HandlerThread("SystemUI StorageNotification");
        thr.start();
        mAsyncEventHandler = new Handler(thr.getLooper());
        mUsbNotifications = new HashSet();
        mLastState = Environment.MEDIA_MOUNTED;
        mLastConnected = false;
		onUsbMassStorageConnectionChanged(connected);

        for (int i=0; i<volumes.length; i++) {
            String sharePath = volumes[i].getPath();
            String shareState = mStorageManager.getVolumeState(sharePath);
            Slog.d(TAG, "onStorageStateChanged - sharePath: " + sharePath + " shareState: " + shareState);
            if (shareState.equals(Environment.MEDIA_UNMOUNTABLE)) {
                onStorageStateChanged(sharePath, shareState, shareState);
            }
        }
    }

    /*
     * @override com.android.os.storage.StorageEventListener
     */
    @Override
    public void onUsbMassStorageConnectionChanged(final boolean connected) {
        mAsyncEventHandler.post(new Runnable() {
            @Override
            public void run() {
                onUsbMassStorageConnectionChangedAsync(connected);
            }
        });
    }

    private void onUsbMassStorageConnectionChangedAsync(boolean connected) {
        mUmsAvailable = connected;
        /*
         * Even though we may have a UMS host connected, we the SD card
         * may not be in a state for export.
         */

        String st = "";
        String path = "";
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (int i=0; i<volumes.length; i++) {
            if (volumes[i].allowMassStorage() && !volumes[i].isEmulated()) {
				path = volumes[i].getPath();
				st = mStorageManager.getVolumeState(path);
				break;
            }
        }
        Slog.i(TAG, String.format("UMS connection changed to %s (media state %s), (path %s)", connected, st, path));

        if (connected && (st.equals(
                Environment.MEDIA_REMOVED) || st.equals(Environment.MEDIA_CHECKING)|| st.equals(Environment.MEDIA_BAD_REMOVAL))) {
            /*
             * No card or card being checked = don't display
             */
            connected = false;
        }

        SXlog.d(TAG, "onUsbMassStorageConnectionChangedAsync - mLastState: " + mLastState + ", st: " + st + ", mLastConnected: " + mLastConnected+ ", connected: " + connected);
        if (!connected) {
            mUsbNotifications.clear();
            updateUsbMassStorageNotification(connected);
            SXlog.d(TAG, "onUsbMassStorageConnectionChangedAsync - Disconnect");
        } else {
            String mCurrentFunctions = SystemProperties.get("sys.usb.config", "none");
            if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_MASS_STORAGE)) {
                SXlog.d(TAG, "onUsbMassStorageConnectionChangedAsync - Connect - UMS");
                if (mLastState.equals(st) && mLastConnected == connected && !mAlarmBootOff) {
                    SXlog.d(TAG, "onUsbMassStorageConnectionChangedAsync - Connect - UMS - Ignore");
                    return;
                }
                updateUsbMassStorageNotification(connected);
            } else {
                SXlog.d(TAG, "onUsbMassStorageConnectionChangedAsync - Connect - MTP");
                updateUsbMassStorageNotification(false);
            }
        }
        mLastConnected = connected;
        SXlog.d(TAG, "onUsbMassStorageConnectionChangedAsync - mLastConnected: " + mLastConnected);
    }

    private static boolean containsFunction(String functions, String function) {
        int index = functions.indexOf(function);

        if (index < 0) return false;
        if (index > 0 && functions.charAt(index - 1) != ',') return false;
        int charAfter = index + function.length();
        if (charAfter < functions.length() && functions.charAt(charAfter) != ',') return false;
        return true;
    }

    /*
     * @override com.android.os.storage.StorageEventListener
     */
    @Override
    public void onStorageStateChanged(final String path, final String oldState, final String newState) {
        mAsyncEventHandler.post(new Runnable() {
            @Override
            public void run() {
                onStorageStateChangedAsync(path, oldState, newState);
            }
        });
    }

    private void onStorageStateChangedAsync(String path, String oldState, String newState) {
        Slog.i(TAG, String.format(
                "Media {%s} state changed from {%s} -> {%s}", path, oldState, newState));

        mLastState = newState;
        StorageVolume volume = null;

        StorageVolume[] Volumes = mStorageManager.getVolumeList();
        for(int i = 0; i < Volumes.length; i++){
           	if(Volumes[i].getPath().equals(path)) {
               volume = Volumes[i];
            	break;
            }
        }
		if (volume == null) {
			   Slog.e(TAG, String.format(
                "Can NOT find volume by name {%s}", path));
			   return;
		}

        if (newState.equals(Environment.MEDIA_SHARED)) {
            /*
             * Storage is now shared. Modify the UMS notification
             * for stopping UMS.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_SHARED]");
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            setUsbStorageNotification(
                    com.android.internal.R.string.usb_storage_stop_notification_title,
                    com.android.internal.R.string.usb_storage_stop_notification_message,
                    com.android.internal.R.drawable.stat_sys_warning, false, true, pi);

        } else if (newState.equals(Environment.MEDIA_CHECKING)) {
            /*
             * Storage is now checking. Update media notification and disable
             * UMS notification.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_CHECKING]");

			CharSequence title = Resources.getSystem().getString(com.android.internal.R.string.ext_media_checking_notification_title, volume.getDescription(mContext));
			CharSequence message = Resources.getSystem().getString(com.android.internal.R.string.ext_media_checking_notification_message);

            setMediaStorageNotification(
                    path,
                    title,
                    message,
                    com.android.internal.R.drawable.stat_notify_sdcard_prepare, true, false, false, null);
        } else if (newState.equals(Environment.MEDIA_MOUNTED)) {
            /*
             * Storage is now mounted. Dismiss any media notifications,
             * and enable UMS notification if connected.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_MOUNTED]");
            setMediaStorageNotification(path, 0, 0, 0, false, false, false, null);
            updateUsbMassStorageNotification(mUmsAvailable);

        } else if (newState.equals(Environment.MEDIA_UNMOUNTED)) {
            /*
             * Storage is now unmounted. We may have been unmounted
             * because the user is enabling/disabling UMS, in which case we don't
             * want to display the 'safe to unmount' notification.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTED]");
            if (!mStorageManager.isUsbMassStorageEnabled()) {
                SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTED]  !mStorageManager.isUsbMassStorageEnabled()");
                if (Environment.MEDIA_SHARED.equals(oldState)) {
                    /*
                     * The unmount was due to UMS being enabled. Dismiss any
                     * media notifications, and enable UMS notification if connected
                     */
                    SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTED]  MEDIA_SHARED");
                    setMediaStorageNotification(path, 0, 0, 0, false, false,  false, null);
                    //updateUsbMassStorageNotification(mUmsAvailable);
                } else {
                    /*
                     * Show safe to unmount media notification, and enable UMS
                     * notification if connected.
                     */
                    if (Environment.isExternalStorageRemovable()) {
 						CharSequence title = Resources.getSystem().getString(com.android.internal.R.string.ext_media_safe_unmount_notification_title, volume.getDescription(mContext));
						CharSequence message = Resources.getSystem().getString(com.android.internal.R.string.ext_media_safe_unmount_notification_message, volume.getDescription(mContext));

                        setMediaStorageNotification(
                                path,
                                title,
                                message,
                                com.android.internal.R.drawable.stat_notify_sdcard, true, false, true, null);
                    } else {
                    SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTED]  !isExternalStorageRemovable");
                        // This device does not have removable storage, so
                        // don't tell the user they can remove it.
                        setMediaStorageNotification(path, 0, 0, 0, false, false, false, null);
                    }
                    SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTED]  !MEDIA_SHARED");
                    //updateUsbMassStorageNotification(mUmsAvailable);
                }
            } else {
                /*
                 * The unmount was due to UMS being enabled. Dismiss any
                 * media notifications, and disable the UMS notification
                 */
                SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTED]  mStorageManager.isUsbMassStorageEnabled()");
                setMediaStorageNotification(path, 0, 0, 0, false, false, false, null);
            }
        } else if (newState.equals(Environment.MEDIA_NOFS)) {
            /*
             * Storage has no filesystem. Show blank media notification,
             * and enable UMS notification if connected.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_NOFS]");
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.internal.app.ExternalMediaFormatActivity.class);
            intent.putExtra("PATH", path);
            PendingIntent pi = PendingIntent.getActivity(mContext, notifyid++, intent, 0);

			CharSequence title = Resources.getSystem().getString(com.android.internal.R.string.ext_media_nofs_notification_title, volume.getDescription(mContext));
			CharSequence message = Resources.getSystem().getString(com.android.internal.R.string.ext_media_nofs_notification_message, volume.getDescription(mContext));

            setMediaStorageNotification(
                    path,
                    title,
                    message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, false, pi);
            updateUsbMassStorageNotification(mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_UNMOUNTABLE)) {
            /*
             * Storage is corrupt. Show corrupt media notification,
             * and enable UMS notification if connected.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_UNMOUNTABLE]");
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.internal.app.ExternalMediaFormatActivity.class);
            intent.putExtra("PATH", path);
            PendingIntent pi = PendingIntent.getActivity(mContext, notifyid++, intent, 0);

            int titleId = 0;
            int messageId = 0;
            boolean isDismissable = false;
            if("/mnt/usbotg".equals(path)) {
                pi = null;
                isDismissable = true;
            }

			CharSequence title = Resources.getSystem().getString(com.android.internal.R.string.ext_media_unmountable_notification_title, volume.getDescription(mContext));
			CharSequence message = Resources.getSystem().getString(com.android.internal.R.string.ext_media_unmountable_notification_message, volume.getDescription(mContext));

            setMediaStorageNotification(
                    path,
                    title,
                    message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, isDismissable, pi);
            updateUsbMassStorageNotification(mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_REMOVED)) {
            /*
             * Storage has been removed. Show nomedia media notification,
             * and disable UMS notification regardless of connection state.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_REMOVED]");

			CharSequence title = Resources.getSystem().getString(com.android.internal.R.string.ext_media_nomedia_notification_title, volume.getDescription(mContext));
			CharSequence message = Resources.getSystem().getString(com.android.internal.R.string.ext_media_nomedia_notification_message, volume.getDescription(mContext));

            setMediaStorageNotification(
                    path,
                    title,
                    message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb,
                    true, false, true, null);

            updateUsbMassStorageNotification(false);
        } else if (newState.equals(Environment.MEDIA_BAD_REMOVAL)) {
            /*
             * Storage has been removed unsafely. Show bad removal media notification,
             * and disable UMS notification regardless of connection state.
             */
            SXlog.d(TAG, "onStorageStateChangedAsync - [MEDIA_BAD_REMOVAL]");

			CharSequence title = Resources.getSystem().getString(com.android.internal.R.string.ext_media_badremoval_notification_title, volume.getDescription(mContext));
			CharSequence message = Resources.getSystem().getString(com.android.internal.R.string.ext_media_badremoval_notification_message, volume.getDescription(mContext));
            /* Add a notification sound for Badremoval */
            setMediaStorageNotification(
                    path,
                    title,
                    message,
                    com.android.internal.R.drawable.stat_sys_warning,
                    true, true, true, null);
            updateUsbMassStorageNotification(false);
        } else {
            Slog.w(TAG, String.format("Ignoring unknown state {%s}", newState));
        }
    }

    /*
    * Check how many storages can be shared on the device.
    * It seems that the device supported SHARED SD need to check.
    */
    boolean isAbleToShare() {
        int allowedShareNum = 0;
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        SXlog.d(TAG, "isAbleToShare - length:" + volumes.length);
        if (volumes != null) {
            for (int i=0; i<volumes.length; i++) {
                SXlog.d(TAG, "isAbleToShare - allowMassStorage:" + volumes[i].allowMassStorage() + "isEmulated:" + volumes[i].isEmulated());
                if (volumes[i].allowMassStorage() && !volumes[i].isEmulated()) {
                    String path = volumes[i].getPath();
                    String st = mStorageManager.getVolumeState(path);
                    if (st != null) {
                        SXlog.d(TAG, String.format("isAbleToShare - %s @ %s", path, st));
                        /* Only count the number of the storage can be shared */
                        if ( !st.equals(Environment.MEDIA_UNMOUNTABLE) && !st.equals(Environment.MEDIA_NOFS) &&
                            !st.equals(Environment.MEDIA_REMOVED) && !st.equals(Environment.MEDIA_BAD_REMOVAL) ) {
                            allowedShareNum++;
                        }
                    }
                }
            }
        }
        SXlog.d(TAG, "isAbleToShare - allowedShareNum:" + allowedShareNum);
        if (allowedShareNum == 0)
            return false;
        else
            return true;
    }

    /**
     * Update the state of the USB mass storage notification
     */
    void updateUsbMassStorageNotification(boolean available) {

        boolean isStorageCanShared = isAbleToShare();
        SXlog.d(TAG, "updateUsbMassStorageNotification - isStorageCanShared=" + isStorageCanShared + ",available=" + available);
        if( !mStorageManager.isUsbMassStorageEnabled() ) {
            /* Show "USB Connected" notification, if the system want it and there is more than one storage can be shared. */
            /* Like SHARED SD, there is an internal storage, but that can not be shared. So don't show notification. */
            if (available && isStorageCanShared) {
                SXlog.d(TAG, "updateUsbMassStorageNotification - [true]");
                Intent intent = new Intent();
                intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
                setUsbStorageNotification(
                        com.android.internal.R.string.usb_storage_notification_title,
                        com.android.internal.R.string.usb_storage_notification_message,
                        com.android.internal.R.drawable.stat_sys_data_usb,
                        false, true, pi);
            } else if(!available && !isStorageCanShared || !mUmsAvailable) {
                /* Cancel "USB Connected" notification, if the system want to cancel it and there is no storage can be shared. */
                /* Like SD hot-plug, remove the external SD card, but still one storage can be shared. So don't cancel the notification. */
                SXlog.d(TAG, "updateUsbMassStorageNotification - [false]");
                setUsbStorageNotification(0, 0, 0, false, false, null);
            } else {
                SXlog.d(TAG, "updateUsbMassStorageNotification - Cannot as your wish!");
            }
            mLastConnected = available;
        } else {
            SXlog.d(TAG, "updateUsbMassStorageNotification - UMS Enabled");
        }
    }

    /**
     * Sets the USB storage notification.
     */
    private synchronized void setUsbStorageNotification(int titleId, int messageId, int icon,
            boolean sound, boolean visible, PendingIntent pi) {

        SXlog.d(TAG, String.format("setUsbStorageNotification - visible: {%s}", visible));
        SXlog.d(TAG, "setUsbStorageNotification - mIsLastVisible: " + mIsLastVisible);
        if (!visible && mUsbStorageNotification == null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        if (visible) {
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(titleId);
            CharSequence message = r.getText(messageId);

            if (mUsbStorageNotification == null) {
                mUsbStorageNotification = new Notification();
                mUsbStorageNotification.icon = icon;
                mUsbStorageNotification.when = 0;
                mUsbStorageNotification.priority = Notification.PRIORITY_MIN;
            }

            if (sound) {
                mUsbStorageNotification.defaults |= Notification.DEFAULT_SOUND;
            } else {
                mUsbStorageNotification.defaults &= ~Notification.DEFAULT_SOUND;
            }

            mUsbStorageNotification.flags = Notification.FLAG_ONGOING_EVENT;

            mUsbStorageNotification.tickerText = title;

			String bootReason = SystemProperties.get("sys.boot.reason");
			boolean alarmBoot = (bootReason != null && bootReason.equals("1")) ? true : false;

           	SXlog.d(TAG, "setUsbStorageNotification - alarmBoot: " + alarmBoot);

			if (alarmBoot) {
                SXlog.d(TAG, "setUsbStorageNotification - [Show Notification After AlarmBoot]");
				return;
			}

           	SXlog.d(TAG, "setUsbStorageNotification - count of mUsbNotifications: " + mUsbNotifications.size());
            if (!mUsbNotifications.contains(title.toString())) {
                mUsbNotifications.clear();
                mUsbNotifications.add(title.toString());
                SXlog.d(TAG, String.format("setUsbStorageNotification - [Add] title: {%s} to HashSet", title.toString()));
            } else {
                SXlog.d(TAG, String.format("setUsbStorageNotification - [Hashset contains] visible: {%s}", visible));
                if (mIsLastVisible) {
                    SXlog.d(TAG, "setUsbStorageNotification - same and visible, return");
                    return;
                }
            }

            if (pi == null) {
                Intent intent = new Intent();
                pi = PendingIntent.getBroadcastAsUser(mContext, 0, intent, 0,
                        UserHandle.CURRENT);
            }

            mUsbStorageNotification.setLatestEventInfo(mContext, title, message, pi);
            final boolean adbOn = 1 == Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED,
                0);

            if (POP_UMS_ACTIVITY_ON_CONNECT && !adbOn) {
                // Pop up a full-screen alert to coach the user through enabling UMS. The average
                // user has attached the device to USB either to charge the phone (in which case
                // this is harmless) or transfer files, and in the latter case this alert saves
                // several steps (as well as subtly indicates that you shouldn't mix UMS with other
                // activities on the device).
                //
                // If ADB is enabled, however, we suppress this dialog (under the assumption that a
                // developer (a) knows how to enable UMS, and (b) is probably using USB to install
                // builds or use adb commands.
                if (Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.DEVICE_PROVISIONED, 0) == 0) {
                    SXlog.d(TAG, "Device not provisioned, skipping showing full screen UsbStorageActivity");
                    mUsbStorageNotification.fullScreenIntent = null;
                } else {
                    mUsbStorageNotification.fullScreenIntent = pi;
                }
            } else {
                mUsbStorageNotification.fullScreenIntent = null;
            }
        }

        final int notificationId = mUsbStorageNotification.icon;
        if (visible) {
            notificationManager.notifyAsUser(null, notificationId, mUsbStorageNotification,
                    UserHandle.ALL);
            mIsLastVisible = true;
        } else {
            notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
            mIsLastVisible = false;
        }
    }

    private synchronized boolean getMediaStorageNotificationDismissable() {
        if ((mMediaStorageNotification != null) &&
            ((mMediaStorageNotification.flags & Notification.FLAG_AUTO_CANCEL) ==
                    Notification.FLAG_AUTO_CANCEL))
            return true;

        return false;
    }

    /**
     * Sets the media storage notification.
     */
    private synchronized void setMediaStorageNotification(String path, int titleId, int messageId, int icon, boolean visible, boolean sound,
                                                          boolean dismissable, PendingIntent pi) {
		Resources r = Resources.getSystem();
		CharSequence title = null;
		CharSequence message = null;
		if (visible) {
		  title = r.getText(titleId);
		  message = r.getText(messageId);
		}
        setMediaStorageNotification(path, title, message, icon, visible, sound, dismissable, pi);
    }

    private synchronized void setMediaStorageNotification(String path, CharSequence title, CharSequence message, int icon, boolean visible,boolean sound,
                                                          boolean dismissable, PendingIntent pi) {
        SXlog.d(TAG, String.format("setMediaStorageNotification path:%s", path));

        Notification mediaStorageNotification = null;

        if ("/storage/sdcard0".equals(path) || "/storage/emulated/0".equals(path)) {
            if (mMediaStorageNotification == null) {
                mMediaStorageNotification = new Notification();
                mMediaStorageNotification.when = 0;
            }
            mediaStorageNotification = mMediaStorageNotification;
        } else if ("/storage/sdcard1".equals(path)) {
            if (mMediaStorageNotificationForExtStorage == null) {
                mMediaStorageNotificationForExtStorage = new Notification();
                mMediaStorageNotificationForExtStorage.when = 0;
            }
            mediaStorageNotification = mMediaStorageNotificationForExtStorage;
        } else {
            if (mMediaStorageNotificationForExtUsbOtg == null) {
                mMediaStorageNotificationForExtUsbOtg = new Notification();
                mMediaStorageNotificationForExtUsbOtg.when = 0;
            }
            mediaStorageNotification = mMediaStorageNotificationForExtUsbOtg;
        }


        if (!visible && mediaStorageNotification.icon == 0) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        if (mediaStorageNotification != null && visible) {
            /*
             * Dismiss the previous notification - we're about to
             * re-use it.
             */
            final int notificationId = mediaStorageNotification.icon;
            notificationManager.cancel(notificationId);
        }

        if (visible) {
            if (mediaStorageNotification == null) {
                mediaStorageNotification = new Notification();
                mediaStorageNotification.when = 0;
            }


        if (sound) {
            mediaStorageNotification.defaults |= Notification.DEFAULT_SOUND;
        } else {
            mediaStorageNotification.defaults &= ~Notification.DEFAULT_SOUND;
        }


            if (dismissable) {
                mediaStorageNotification.flags = Notification.FLAG_AUTO_CANCEL;
            } else {
                mediaStorageNotification.flags = Notification.FLAG_ONGOING_EVENT;
            }

            mediaStorageNotification.tickerText = title;
            if (pi == null) {
                Intent intent = new Intent();
                pi = PendingIntent.getBroadcastAsUser(mContext, 0, intent, 0,
                        UserHandle.CURRENT);
            }

            mediaStorageNotification.icon = icon;
            mediaStorageNotification.setLatestEventInfo(mContext, title, message, pi);
        }

        final int notificationId = mediaStorageNotification.icon;
        if (visible) {
             notificationManager.notifyAsUser(null, notificationId,
                    mediaStorageNotification, UserHandle.ALL);
        } else {
            notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
        }
    }
}
