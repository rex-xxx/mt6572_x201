/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * See the License for the specific language governing permissions an
 * limitations under the License.
 */

package com.android.server.usb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Pair;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ActivityNotFoundException;
import android.util.Log;
import com.mediatek.xlog.SXlog;
import com.mediatek.common.featureoption.FeatureOption;

/**
 * UsbDeviceManager manages USB state in device mode.
 */
public class UsbDeviceManager {

    private static final String TAG = UsbDeviceManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String USB_STATE_MATCH =
            "DEVPATH=/devices/virtual/android_usb/android0";
    private static final String ACCESSORY_START_MATCH =
            "DEVPATH=/devices/virtual/misc/usb_accessory";
    //ALPS00428998
    private static final String MTP_STATE_MATCH =
            "DEVPATH=/devices/virtual/misc/mtp_usb";
    //ALPS00428998
    private static final String FUNCTIONS_PATH =
            "/sys/class/android_usb/android0/functions";
    private static final String STATE_PATH =
            "/sys/class/android_usb/android0/state";
    private static final String MASS_STORAGE_FILE_PATH =
            "/sys/class/android_usb/android0/f_mass_storage/lun/file";
    private static final String RNDIS_ETH_ADDR_PATH =
            "/sys/class/android_usb/android0/f_rndis/ethaddr";
    private static final String AUDIO_SOURCE_PCM_PATH =
            "/sys/class/android_usb/android0/f_audio_source/pcm";

    private static final String IPO_POWER_ON  = 
			"android.intent.action.ACTION_BOOT_IPO";
    private static final String IPO_POWER_OFF = 
			"android.intent.action.ACTION_SHUTDOWN_IPO";

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_ENABLE_ADB = 1;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_BOOT_COMPLETED = 4;
    private static final int MSG_USER_SWITCHED = 5;
    private static final int MSG_UPDATE_DISCONNECT_STATE = 6;

    private static final int AUDIO_MODE_NONE = 0;
    private static final int AUDIO_MODE_SOURCE = 1;

    // Delay for debouncing USB disconnects.
    // We often get rapid connect/disconnect events when enabling USB functions,
    // which need debouncing.
    private static final int UPDATE_DELAY = 1000;
    private static final int UPDATE_DELAY_BICR = 3000;

    //Extended to 45000 for waiting the behavior of XP MTP transfer canceling timeout.
    private static final int RNDIS_UPDATE_DELAY = 45000;

    private static final String BOOT_MODE_PROPERTY = "ro.bootmode";

    private UsbHandler mHandler;
    private boolean mBootCompleted;

    private final Object mLock = new Object();

    private final Context mContext;
    private final ContentResolver mContentResolver;
    @GuardedBy("mLock")
    private UsbSettingsManager mCurrentSettings;
    private NotificationManager mNotificationManager;
    private final boolean mHasUsbAccessory;
    private boolean mUseUsbNotification;
    private boolean mAdbEnabled;
    private boolean mAudioSourceEnabled;
    private Map<String, List<Pair<String, String>>> mOemModeMap;
    private String[] mAccessoryStrings;
    private UsbDebuggingManager mDebuggingManager;

    private boolean mAcmEnabled;
    private boolean mSettingUsbCharging;
    private boolean mSettingUsbBicr;
    /*
     * These 4 parameters are for built-in CD-ROM evolution(BICR evo).
     * @ What is BICR evo? The device only can show built-in CD-ROM, except receiving a specifc SCSI command from PC.
     *                     After that, user has ability to switch to other USB functions (UMS,MTP,PTP,Tethering.).
     *                     Charge only is excluded in this restriction.
     * 2. mIsUsbBicrEvo : True: this feature is on. Setting this flag in initial step.
     * 3. mIsPcKnowMe : True: the device has received the specific SCSI command. UsbDeviceManager can switch to others.
     * 4. mIsBicrSet : True: the device has done "sys.usb.bicr=no and sys.usb.config=none and bicr" once.
     * 5. mIsUserSwitch : True: the device is in the phase of switching USB functions A to B.
     */
    private boolean mIsUsbBicrEvo;
    private boolean mIsPcKnowMe;
    private boolean mIsBicrSet;
    private boolean mIsUserSwitch;
    private boolean mHwDisconnected;
    private boolean mBatteryChargingUnPlug;
    private String mSettingFunction;
    private String mUsbStorageType;
    private final ReentrantLock mAdbUpdateLock = new ReentrantLock();
    //ALPS00428998
    private boolean mMtpAskDisconnect;
    //ALPS00428998

    private class AdbSettingsObserver extends ContentObserver {
        public AdbSettingsObserver() {
            super(null);
        }
        @Override
        public void onChange(boolean selfChange) {
            boolean enable = (Settings.Global.getInt(mContentResolver,
                    Settings.Global.ADB_ENABLED, 0) > 0);
            mHandler.sendMessage(MSG_ENABLE_ADB, enable);
        }
    }

    /*
     * Listens for uevent messages from the kernel to monitor the USB state
     */
    private final UEventObserver mUEventObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (DEBUG) Slog.v(TAG, "USB UEVENT: " + event.toString());

            String state = event.get("USB_STATE");
            String accessory = event.get("ACCESSORY");            
			if (DEBUG) SXlog.d(TAG, "mUEventObserver - onUEvent - state: " + state);
            if (state != null) {
                mHandler.updateState(state);
            } else if ("START".equals(accessory)) {
                if (DEBUG) Slog.d(TAG, "got accessory start");
                startAccessoryMode();
            }
        }
    };

    public UsbDeviceManager(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        PackageManager pm = mContext.getPackageManager();

        mHwDisconnected = true;
        mSettingUsbCharging = false;
        mSettingUsbBicr = false;
        mIsUsbBicrEvo = false;
        mIsPcKnowMe = true;
        mIsUserSwitch = false;
        mBatteryChargingUnPlug = false;
        //ALPS00428998
        mMtpAskDisconnect = false;
        //ALPS00428998
        mHasUsbAccessory = pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY);
        initRndisAddress();

        String value = SystemProperties.get("persist.sys.usb.bicr_evo", "");
        if (value.equals("yes")) {
            Slog.d(TAG, "Enable BICR evolution!!");
            mIsUsbBicrEvo = true;
            mIsPcKnowMe = false;
        }

        readOemUsbOverrideConfig();

        // create a thread for our Handler
        HandlerThread thread = new HandlerThread("UsbDeviceManager",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mHandler = new UsbHandler(thread.getLooper());

        if (nativeIsStartRequested()) {
            if (DEBUG) Slog.d(TAG, "accessory attached at boot");
            startAccessoryMode();
        }

        if ("1".equals(SystemProperties.get("ro.adb.secure"))) {
            mDebuggingManager = new UsbDebuggingManager(context);
        }
    }

    public void setCurrentSettings(UsbSettingsManager settings) {
        synchronized (mLock) {
            mCurrentSettings = settings;
        }
    }

    private UsbSettingsManager getCurrentSettings() {
        synchronized (mLock) {
            return mCurrentSettings;
        }
    }

    public void systemReady() {
        if (DEBUG) Slog.d(TAG, "systemReady");

        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // We do not show the USB notification if any volume supports mass storage.
        // The legacy mass storage UI will be used instead.
        String config = SystemProperties.get("persist.sys.usb.config", UsbManager.USB_FUNCTION_MTP);
        config = removeFunction(config, UsbManager.USB_FUNCTION_ADB);
        config = removeFunction(config, UsbManager.USB_FUNCTION_ACM);
        mUsbStorageType = SystemProperties.get("ro.sys.usb.storage.type", UsbManager.USB_FUNCTION_MTP);

        SXlog.d(TAG, "systemReady - mUsbStorageType: " + mUsbStorageType + ", config: " + config);

        if (!containsFunction(mUsbStorageType, config)) {
            mUsbStorageType = config;
            SXlog.d(TAG, "systemReady - mUsbStorageType = config");
        }
        if (mUsbStorageType.equals(UsbManager.USB_FUNCTION_MASS_STORAGE)) {

            SXlog.d(TAG, "systemReady - UMS only");

            mUseUsbNotification = false;
			boolean massStorageSupported = false;
			final StorageManager storageManager = StorageManager.from(mContext);
			final StorageVolume[] volumes = storageManager.getVolumeList();

			for (int i=0; i<volumes.length; i++) {
				if (volumes[i].allowMassStorage()) {

					SXlog.d(TAG, "systemReady - massStorageSupported: " + massStorageSupported);

					massStorageSupported = true;
					break;
				}
			}

			mUseUsbNotification = !massStorageSupported;
        }
        else {
            SXlog.d(TAG, "systemReady - MTP(+UMS)");
            mUseUsbNotification = true;
        }
        if (containsFunction(config, UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
			mSettingUsbCharging = true;
		}

        // make sure the ADB_ENABLED setting value matches the current state
        Settings.Global.putInt(mContentResolver, Settings.Global.ADB_ENABLED, mAdbEnabled ? 1 : 0);

        mHandler.sendEmptyMessage(MSG_SYSTEM_READY);
    }

    private void startAccessoryMode() {
        mAccessoryStrings = nativeGetAccessoryStrings();
        boolean enableAudio = (nativeGetAudioMode() == AUDIO_MODE_SOURCE);
        // don't start accessory mode if our mandatory strings have not been set
        boolean enableAccessory = (mAccessoryStrings != null &&
                        mAccessoryStrings[UsbAccessory.MANUFACTURER_STRING] != null &&
                        mAccessoryStrings[UsbAccessory.MODEL_STRING] != null);
        String functions = null;

        if (enableAccessory && enableAudio) {
            functions = UsbManager.USB_FUNCTION_ACCESSORY + ","
                    + UsbManager.USB_FUNCTION_AUDIO_SOURCE;
        } else if (enableAccessory) {
            functions = UsbManager.USB_FUNCTION_ACCESSORY;
        } else if (enableAudio) {
            functions = UsbManager.USB_FUNCTION_AUDIO_SOURCE;
        }

        if (functions != null) {
            setCurrentFunctions(functions, false);
        }
    }

    private static void initRndisAddress() {
        // configure RNDIS ethernet address based on our serial number using the same algorithm
        // we had been previously using in kernel board files
        final int ETH_ALEN = 6;
        int address[] = new int[ETH_ALEN];
        // first byte is 0x02 to signify a locally administered address
        address[0] = 0x02;

        String serial = SystemProperties.get("ro.serialno", "1234567890ABCDEF");
        int serialLength = serial.length();
        // XOR the USB serial across the remaining 5 bytes
        for (int i = 0; i < serialLength; i++) {
            address[i % (ETH_ALEN - 1) + 1] ^= (int)serial.charAt(i);
        }
        String addrString = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
            address[0], address[1], address[2], address[3], address[4], address[5]);
        try {
            FileUtils.stringToFile(RNDIS_ETH_ADDR_PATH, addrString);
        } catch (IOException e) {
           Slog.e(TAG, "failed to write to " + RNDIS_ETH_ADDR_PATH);
        }
    }

     private static String addFunction(String functions, String function) {
         if ("none".equals(functions)) {
             return function;
         }

		if (DEBUG) SXlog.d(TAG, "Add " + function + " into " + functions);

        if (!containsFunction(functions, function)) {

            if ((function.equals(UsbManager.USB_FUNCTION_ADB) || function.equals(UsbManager.USB_FUNCTION_RNDIS) || function.equals(UsbManager.USB_FUNCTION_EEM))&& containsFunction(functions,UsbManager.USB_FUNCTION_ACM)) {
    	        functions = removeFunction(functions,UsbManager.USB_FUNCTION_ACM);
            }

            if (functions.length() > 0) {
                functions += ",";
            }
            functions += function;

            if ((function.equals(UsbManager.USB_FUNCTION_ADB) || function.equals(UsbManager.USB_FUNCTION_RNDIS) || function.equals(UsbManager.USB_FUNCTION_EEM)) && containsFunction(functions,UsbManager.USB_FUNCTION_ACM)) {
    	        functions = addFunction(functions,UsbManager.USB_FUNCTION_ACM);
            }
        }
        return functions;
    }

    private static String removeFunction(String functions, String function) {
        String[] split = functions.split(",");
        for (int i = 0; i < split.length; i++) {
            if (function.equals(split[i])) {
                split[i] = null;
            }
        }
        if (split.length == 1 && split[0] == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
         for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s != null) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(s);
            }
        }
        return builder.toString();
    }

    private static boolean containsFunction(String functions, String function) {
        int index = functions.indexOf(function);

        if (DEBUG) SXlog.d(TAG, "Does " + functions + " contain " + function + "? index: " + index);

        if (index < 0) return false;
        if (index > 0 && functions.charAt(index - 1) != ',') return false;
        int charAfter = index + function.length();
        if (charAfter < functions.length() && functions.charAt(charAfter) != ',') return false;
        return true;
    }

    private final class UsbHandler extends Handler {

        // current USB state
        private boolean mConnected;
        private boolean mConfigured;
        private String mCurrentFunctions;
        private String mDefaultFunctions;
        private UsbAccessory mCurrentAccessory;
        private int mUsbNotificationId;
        private boolean mAdbNotificationShown;
        private int mCurrentUser = UserHandle.USER_NULL;
        private int mPlugType;

        private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                    if (DEBUG) Slog.d(TAG, "boot completed");
                    mHandler.sendEmptyMessage(MSG_BOOT_COMPLETED);
                } else if(action.equals(IPO_POWER_ON)) {
                    SXlog.d(TAG, "onReceive - [IPO_POWER_ON] mDefaultFunctions: " + mDefaultFunctions +
                                 ", mSettingUsbCharging: " + mSettingUsbCharging + ", mSettingUsbBicr: " + mSettingUsbBicr);

                    if (mSettingUsbBicr) {
                        mSettingUsbBicr = false;
                        setCurrentFunctions(mDefaultFunctions, false);
                    }
                    if (mIsUsbBicrEvo) {
                        mIsPcKnowMe = false;
                        setCurrentFunctions(UsbManager.USB_FUNCTION_BICR, false);
                    } else {
                        mIsPcKnowMe = true;
                    }
                }
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    mPlugType = intent.getIntExtra("plugged", 0);
                    SXlog.d(TAG, "onReceive - BATTERY_CHANGED - mPlugType: " + mPlugType +
                                 ", mSettingUsbCharging: " + mSettingUsbCharging + ", mConnected: " + mConnected +
                                 ", mSettingUsbBicr: " + mSettingUsbBicr);
                    if (mPlugType == 0) {
                        mHwDisconnected = true;
                        if (mSettingUsbCharging) {
                            SXlog.d(TAG, "onReceive - BATTERY_CHANGED - [Update USB Notification] For [USB UNPLUGGED in USB Charging Mode] mSettingUsbCharging: " + mSettingUsbCharging + ", mPlugType: " + mPlugType);
                            updateUsbNotification();
                            updateAdbNotification();
                        }
                        if (mSettingUsbBicr) {
                            SXlog.d(TAG, "onReceive - BATTERY_CHANGED - [USB UNPLUGGED in USB BICR Mode]");
                            mSettingUsbBicr = false;
                            setCurrentFunctions(mDefaultFunctions, false);
                        }
                        if (!mSettingUsbCharging && !mSettingUsbBicr) {
                            if (mConnected) {
                                mBatteryChargingUnPlug = true;
                                removeMessages(MSG_UPDATE_STATE);
                                removeMessages(MSG_UPDATE_DISCONNECT_STATE);
                                Message msg = Message.obtain();
                                msg.what = MSG_UPDATE_DISCONNECT_STATE;
                                msg.arg1 = 0;
                                msg.arg2 = 0;
                                SXlog.d(TAG, "onReceive - BATTERY_CHANGED - [HW USB Disconnected] mHwDisconnected: " + mHwDisconnected + ", mConnected: " + mConnected + ", mConfigured: " + mConfigured);
                                sendMessageDelayed(msg, 1000);
                            }
                        }
                    } else {
                        mHwDisconnected = false;
                        if (mBatteryChargingUnPlug) {
                            SXlog.d(TAG, "onReceive - BATTERY_CHANGED - [IGNORE] mHwDisconnected: " + mHwDisconnected + ", mConnected: " + mConnected + ", mConfigured: " + mConfigured + ", mBatteryChargingUnPlug: " + mBatteryChargingUnPlug);
                            removeMessages(MSG_UPDATE_DISCONNECT_STATE);
                            mBatteryChargingUnPlug = false;
                        }
                        if (mSettingUsbCharging) {
                            if (mUsbNotificationId != com.mediatek.internal.R.string.usb_charging_notification_title) {
                                SXlog.d(TAG, "onReceive - BATTERY_CHANGED - [Update USB Notification] For [USB PLUGGED in USB Charging Mode] mSettingUsbCharging: " + mSettingUsbCharging + ", mPlugType: " + mPlugType);
                                updateUsbNotification();
                                updateAdbNotification();
                            }
                        }
                    }
                }
            }
        };

        private final BroadcastReceiver mUserSwitchedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                mHandler.obtainMessage(MSG_USER_SWITCHED, userId, 0).sendToTarget();
            }
        };

        public UsbHandler(Looper looper) {
            super(looper);
            try {
                // persist.sys.usb.config should never be unset.  But if it is, set it to "adb"
                // so we have a chance of debugging what happened.

                // if "persist.sys.usb.config" is empty or get property, set the default to mtp,adb
                //otherwise, the USB function switch menu would be disappear!!
                //ALPS00384287
                mDefaultFunctions = SystemProperties.get("persist.sys.usb.config", "mtp,adb");
                //ALPS00384287

                // Check if USB mode needs to be overridden depending on OEM specific bootmode.
                mDefaultFunctions = processOemUsbOverride(mDefaultFunctions);

                mAdbEnabled = containsFunction(mDefaultFunctions, UsbManager.USB_FUNCTION_ADB);
                mAcmEnabled = containsFunction(mDefaultFunctions, UsbManager.USB_FUNCTION_ACM);

                if (DEBUG) SXlog.d(TAG, "UsbHandler - mDefaultFunctions: " + mDefaultFunctions);

                if (mIsUsbBicrEvo) {
                    mDefaultFunctions = UsbManager.USB_FUNCTION_BICR;
                    if (mAdbEnabled)
                        mDefaultFunctions = addFunction(mDefaultFunctions, UsbManager.USB_FUNCTION_ADB);
                    else
                        mDefaultFunctions = removeFunction(mDefaultFunctions, UsbManager.USB_FUNCTION_ADB);
                }

                // sanity check the sys.usb.config system property
                // this may be necessary if we crashed while switching USB configurations
                String config = SystemProperties.get("sys.usb.config", "none");
                if (!config.equals(mDefaultFunctions)) {
                    Slog.w(TAG, "resetting config to persistent property: " + mDefaultFunctions);
                    SystemProperties.set("sys.usb.config", mDefaultFunctions);
                }

                mCurrentFunctions = mDefaultFunctions;
                mSettingFunction = mCurrentFunctions;
                String state = FileUtils.readTextFile(new File(STATE_PATH), 0, null).trim();
                updateState(state);

                // Upgrade step for previous versions that used persist.service.adb.enable
                String value = SystemProperties.get("persist.service.adb.enable", "");
                if (DEBUG) SXlog.d(TAG, "persist.service.adb.enable:" + value);
                if (value.length() > 0) {
                    char enable = value.charAt(0);
                    if (enable == '1') {
                        setAdbEnabled(true);
                    } else if (enable == '0') {
                        setAdbEnabled(false);
                    }
                    if(!mIsUsbBicrEvo) SystemProperties.set("persist.service.adb.enable", "");
                }

                value = SystemProperties.get("persist.service.acm.enable", "");
                if (DEBUG) SXlog.d(TAG, "persist.service.acm.enable:" + value);
                if (value.length() > 0) {
                    char enable = value.charAt(0);
                    if (enable == '1') {
                        setAcmEnabled(true);
                    } else if (enable == '0') {
                        setAcmEnabled(false);
                    }
                    SystemProperties.set("persist.service.acm.enable", "");
                }

                // register observer to listen for settings changes
                mContentResolver.registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),
                                false, new AdbSettingsObserver());

                // Watch for USB configuration changes
                mUEventObserver.startObserving(USB_STATE_MATCH);
                mUEventObserver.startObserving(ACCESSORY_START_MATCH);
                //ALPS00428998
                mUEventObserver.startObserving(MTP_STATE_MATCH);
                //ALPS00428998

                IntentFilter filter = new IntentFilter();

                if (FeatureOption.MTK_IPO_SUPPORT == true) {
                    filter.addAction(IPO_POWER_ON);
                    filter.addAction(IPO_POWER_OFF);
                }

                filter.addAction(Intent.ACTION_BOOT_COMPLETED);
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);

                mContext.registerReceiver(mIntentReceiver, filter);
                mContext.registerReceiver(
                        mUserSwitchedReceiver, new IntentFilter(Intent.ACTION_USER_SWITCHED));
            } catch (Exception e) {
                Slog.e(TAG, "Error initializing UsbHandler", e);
            }
        }

        public void sendMessage(int what, boolean arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = (arg ? 1 : 0);
            sendMessage(m);
        }

        public void sendMessage(int what, Object arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg;
            sendMessage(m);
        }

        public void sendMessage(int what, Object arg0, boolean arg1) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg0;
            m.arg1 = (arg1 ? 1 : 0);
            sendMessage(m);
        }

        public void updateState(String state) {
            int connected, configured;

            if (DEBUG) SXlog.d(TAG, "updateState - " + state);
            Message msg;

            if ("HWDISCONNECTED".equals(state)) {
                connected = 0;
                configured = 0;
                mHwDisconnected = true;
                msg = Message.obtain(this, MSG_UPDATE_DISCONNECT_STATE);
            } else if ("DISCONNECTED".equals(state)) {
                connected = 0;
                configured = 0;
                mHwDisconnected = false;
                mIsBicrSet = false;
                msg = Message.obtain(this, MSG_UPDATE_DISCONNECT_STATE);
            } else if ("CONNECTED".equals(state)) {
                connected = 1;
                configured = 0;
                mHwDisconnected = false;
                mIsBicrSet = false;
                msg = Message.obtain(this, MSG_UPDATE_STATE);
            } else if ("CONFIGURED".equals(state)) {
                connected = 1;
                configured = 1;
                mHwDisconnected = false;
                mIsBicrSet = false;
                msg = Message.obtain(this, MSG_UPDATE_STATE);
            } else if ("REZEROCMD".equals(state)) {
                /*When recieve REZEROCMD, it means that PC has installed a proper driver, so can switch to other USB function(UMS)*/
                Slog.w(TAG, "PC knows me");
                mIsPcKnowMe = true;
                setCurrentFunctions(UsbManager.USB_FUNCTION_MASS_STORAGE, false);
                return;
            } else if ("SHOWCDROMCMD".equals(state)) {
                /*When recieve REZEROCMD, it means that PC has installed a proper driver, so can switch to other USB function(UMS)*/
                Slog.w(TAG, "SHOW CD-ROM CMD");
                setCurrentFunctions(UsbManager.USB_FUNCTION_BICR, false);
                return;
            //ALPS00428998
            } else if ("MTPASKDISCONNECT".equals(state)) {
                /*When recieve MTPASKDISCONNECT, it means that PC has installed a proper driver, so can switch to other USB function(UMS)*/
                Slog.w(TAG, "MTPASKDISCONNECT");
                mMtpAskDisconnect = true;
                setCurrentFunctions(UsbManager.USB_FUNCTION_MTP, false);
                return;
            //ALPS00428998
            } else {
                Slog.e(TAG, "unknown state " + state);
                return;
            }
            removeMessages(MSG_UPDATE_STATE);
            removeMessages(MSG_UPDATE_DISCONNECT_STATE);

            msg.arg1 = connected;
            msg.arg2 = configured;
            // debounce disconnects to avoid problems bringing up USB tethering
            if (mHwDisconnected || mSettingUsbCharging) {
                SXlog.d(TAG, "updateState - UPDATE_DELAY  " + state + " mSettingFunction: " + mSettingFunction);
                if(mIsUsbBicrEvo && mIsPcKnowMe) {
                    sendMessageDelayed(msg, (connected == 0) ? UPDATE_DELAY_BICR : 0);
                    Slog.w(TAG, "Delay!!!!" + UPDATE_DELAY_BICR );
                } else {
                    sendMessageDelayed(msg, (connected == 0) ? UPDATE_DELAY : 0);
                }
            }
            else {
                SXlog.d(TAG, "updateState - RNDIS_UPDATE_DELAY  " + state + " mSettingFunction: " + mSettingFunction);
                sendMessageDelayed(msg, (connected == 0) ? RNDIS_UPDATE_DELAY : 0);
            }
        }

        private boolean waitForState(String state) {
            // wait for the transition to complete.
            // give up after 1 second.
            for (int i = 0; i < 40; i++) {
                // State transition is done when sys.usb.state is set to the new configuration
                if (state.equals(SystemProperties.get("sys.usb.state"))) return true;
                SystemClock.sleep(50);
            }
            Slog.e(TAG, "waitForState(" + state + ") FAILED");
            return false;
        }

        private boolean setUsbConfig(String config) {
            if (DEBUG) Slog.d(TAG, "setUsbConfig(" + config + ")");
            // set the new configuration
            if (DEBUG) SXlog.d(TAG, "setUsbConfig - config: " + config);

            SystemProperties.set("sys.usb.config", config);
            return waitForState(config);
        }

        private void setAdbEnabled(boolean enable) {
            if (DEBUG) Slog.d(TAG, "setAdbEnabled: " + enable);
            if (enable != mAdbEnabled) {
                mAdbEnabled = enable;
                // Due to the persist.sys.usb.config property trigger, changing adb state requires
                // switching to default function
                if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_BICR)) {
                    setEnabledFunctions(mCurrentFunctions, false);
                    updateAdbNotification();
                } else if (mIsUsbBicrEvo && mIsPcKnowMe) {
                    /* In BICR evo, the default function is always as bicr. So when enable or disable adb,*/
                    /* Use mCurrentFunctions to be the USB function that user wants to switch to.*/
                    if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_RNDIS) || containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_EEM)) {
                        setEnabledFunctions(UsbManager.USB_FUNCTION_MASS_STORAGE, false);
                    } else {
                        setEnabledFunctions(mCurrentFunctions, false);
                    }
                    updateAdbNotification();
                } else if (!mCurrentFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
					if (mDefaultFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                        mSettingUsbCharging = true;
                    }
                    setEnabledFunctions(mDefaultFunctions, true);
                    updateAdbNotification();
                } else if (mCurrentFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                    if (mAdbEnabled) {
                        SystemProperties.set("persist.service.adb.enable", "1");
                    } else {
                        SystemProperties.set("persist.service.adb.enable", "0");
                    }
                    return;
		}
                /* ADB setting should keep in mind when bicr evo is defined.*/
                /* Cuz after power on/off, the device should know adb is on/off.*/
                if(mIsUsbBicrEvo) {
                    if (mAdbEnabled) {
                        SystemProperties.set("persist.service.adb.enable", "1");
                    } else {
                        SystemProperties.set("persist.service.adb.enable", "0");
                    }
                }
                if (mDebuggingManager != null) {
                    mDebuggingManager.setAdbEnabled(mAdbEnabled);
                }
            }
        }

        private void setAcmEnabled(boolean enable) {
            if (DEBUG) Slog.d(TAG, "setAcmEnabled: " + enable);
            if (enable != mAcmEnabled) {
                mAcmEnabled = enable;
                // Due to the persist.sys.usb.config property trigger, changing adb state requires
                // switching to default function
                setEnabledFunctions(mDefaultFunctions, true);
            }
        }

        private void setEnabledFunctions(String functions, boolean makeDefault) {
            if (DEBUG) {
                SXlog.d(TAG, "setEnabledFunctions - functions: " + functions);
                SXlog.d(TAG, "setEnabledFunctions - mDefaultFunctions: " + mDefaultFunctions);
                SXlog.d(TAG, "setEnabledFunctions - mCurrentFunctions: " + mCurrentFunctions);
                SXlog.d(TAG, "setEnabledFunctions - mSettingFunction: " + mSettingFunction);
            }

            if (mCurrentFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                SXlog.d(TAG, "setEnabledFunctions - [Disable USB Charging]");
                SystemProperties.set("sys.usb.charging","no");
            }

            if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_BICR)) {
                if (mIsUsbBicrEvo) {
                    Log.w(TAG, "====mIsPcKnowMe:" + mIsPcKnowMe + ", mIsBicrSet:" + mIsBicrSet + ", mHwDisconnected:" + mHwDisconnected);
                    if (mIsBicrSet && mHwDisconnected) {
                        Slog.d(TAG, "Do not set sys.usb.bicr=no again!!!");
                    } else {
                        SXlog.d(TAG, "setEnabledFunctions1 - [Disable USB BICR]");
                        SystemProperties.set("sys.usb.bicr","no");
                    }
                } else {
                    SXlog.d(TAG, "setEnabledFunctions2 - [Disable USB BICR]");
                    SystemProperties.set("sys.usb.bicr","no");
                }
            }

            if (mIsUsbBicrEvo && functions != null) {
                if( containsFunction(functions, UsbManager.USB_FUNCTION_RNDIS) || containsFunction(functions, UsbManager.USB_FUNCTION_EEM) ) {
                    mDefaultFunctions = mSettingFunction;
                }
            }
            // Do not update persystent.sys.usb.config if the device is booted up
            // with OEM specific mode.
            if (functions != null && makeDefault && !needsOemUsbOverride() && !mIsUsbBicrEvo) {

                mSettingFunction = functions;
                if (mAdbEnabled && !mSettingUsbCharging) {
                    functions = addFunction(functions, UsbManager.USB_FUNCTION_ADB);
                } else {
                    functions = removeFunction(functions, UsbManager.USB_FUNCTION_ADB);
                }
                if (mAcmEnabled && !mSettingUsbCharging && !mSettingUsbBicr) {
                    functions = addFunction(functions, UsbManager.USB_FUNCTION_ACM);
                } else {
                    functions = removeFunction(functions, UsbManager.USB_FUNCTION_ACM);
                }
                SXlog.d(TAG, "setEnabledFunctions - functions: " + functions);
                if (!mDefaultFunctions.equals(functions) || containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_BICR) || containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_RNDIS)) {
                    if (!setUsbConfig("none")) {
                        Slog.e(TAG, "Failed to disable USB");
                        // revert to previous configuration if we fail
                        setUsbConfig(mCurrentFunctions);
                        return;
                    }
                    // setting this property will also change the current USB state
                    // via a property trigger
                    SystemProperties.set("persist.sys.usb.config", functions);

                    if (mSettingFunction.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                        if (mAdbEnabled) {
                            SystemProperties.set("persist.service.adb.enable", "1");
                        } else {
                            SystemProperties.set("persist.service.adb.enable", "0");
                        }
                    } else {
                        SystemProperties.set("persist.service.adb.enable", "");
                    }

                    if (waitForState(functions)) {
                        mCurrentFunctions = functions;
                        mDefaultFunctions = functions;
                    } else {
                        Slog.e(TAG, "Failed to switch persistent USB config to " + functions);
                        // revert to previous configuration if we fail
                        SystemProperties.set("persist.sys.usb.config", mDefaultFunctions);
                    }
                }
            } else {
                if (functions == null) {
                    functions = mDefaultFunctions;
                }

                // Override with bootmode specific usb mode if needed
                functions = processOemUsbOverride(functions);
                mSettingFunction = functions;

                if (mIsUsbBicrEvo && !mIsPcKnowMe && !mSettingUsbCharging) {
                    functions = UsbManager.USB_FUNCTION_BICR;
                    mSettingFunction = functions;
                }

                if (mAdbEnabled && !mSettingUsbCharging) {
                    functions = addFunction(functions, UsbManager.USB_FUNCTION_ADB);
                } else {
                    functions = removeFunction(functions, UsbManager.USB_FUNCTION_ADB);
                }
                if (mAcmEnabled && !mSettingUsbCharging && !mSettingUsbBicr) {
                    functions = addFunction(functions, UsbManager.USB_FUNCTION_ACM);
                } else {
                    functions = removeFunction(functions, UsbManager.USB_FUNCTION_ACM);
                }

                SXlog.d(TAG, "else setEnabledFunctions, functions: " + functions + ", mCurrentFunctions: " + mCurrentFunctions);
                if (!mCurrentFunctions.equals(functions) || containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_BICR) || (mIsUsbBicrEvo && !mIsPcKnowMe) || mMtpAskDisconnect) {
                    if (mIsUsbBicrEvo) {
                        Log.w(TAG, "====mIsPcKnowMe:" + mIsPcKnowMe + ", mIsBicrSet:" + mIsBicrSet + ", mHwDisconnected:" + mHwDisconnected);
                        if (mIsBicrSet && !mIsPcKnowMe && mHwDisconnected && mCurrentFunctions.equals(functions) && containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_BICR)) {
                            Slog.e(TAG, "Do not do setUsbConfig() again!!!");
                            return;
                        } else {
                            mIsBicrSet = true;
                        }
                    }
                    if (!setUsbConfig("none")) {
                        Slog.e(TAG, "Failed to disable USB");
                        // revert to previous configuration if we fail
                        setUsbConfig(mCurrentFunctions);
                        return;
                    }
                    if (setUsbConfig(functions)) {
                        mCurrentFunctions = functions;
                    } else {
                        Slog.e(TAG, "Failed to switch USB config to " + functions);
                        // revert to previous configuration if we fail
                        setUsbConfig(mCurrentFunctions);
                    }
                }
            }
        }

        private void updateCurrentAccessory() {
            if (!mHasUsbAccessory) return;

            if (mConfigured) {
                if (mAccessoryStrings != null) {
                    mCurrentAccessory = new UsbAccessory(mAccessoryStrings);
                    Slog.d(TAG, "entering USB accessory mode: " + mCurrentAccessory);
                    // defer accessoryAttached if system is not ready
                    if (mBootCompleted) {
                        getCurrentSettings().accessoryAttached(mCurrentAccessory);
                    } // else handle in mBootCompletedReceiver
                } else {
                    Slog.e(TAG, "nativeGetAccessoryStrings failed");
                }
            } else if (!mConnected) {
                // make sure accessory mode is off
                // and restore default functions
                Slog.d(TAG, "exited USB accessory mode");
				if (mDefaultFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
					mSettingUsbCharging = true;
					updateUsbState();
				} else {
					mSettingUsbCharging = false;
				}
                setEnabledFunctions(mDefaultFunctions, false);

                if (mCurrentAccessory != null) {
                    if (mBootCompleted) {
                        getCurrentSettings().accessoryDetached(mCurrentAccessory);
                    }
                    mCurrentAccessory = null;
                    mAccessoryStrings = null;
                }
            }
        }

        private void updateUsbState() {
            // send a sticky broadcast containing current USB state
            Intent intent = new Intent(UsbManager.ACTION_USB_STATE);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(UsbManager.USB_CONNECTED, mConnected);
            intent.putExtra(UsbManager.USB_CONFIGURED, mConfigured);
            intent.putExtra("USB_HW_DISCONNECTED", mHwDisconnected);
            intent.putExtra("USB_IS_PC_KNOW_ME", mIsPcKnowMe);

            if (mCurrentFunctions != null) {
                String[] functions = mCurrentFunctions.split(",");
                for (int i = 0; i < functions.length; i++) {
                    intent.putExtra(functions[i], true);
                }
            }

            SXlog.d(TAG, "updateUsbState - mConnected: " + mConnected + ", mConfigured: " + mConfigured + ", mHwDisconnected: " + mHwDisconnected + ", mCurrentFunctions: " + mCurrentFunctions);
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateAudioSourceFunction() {
            boolean enabled = containsFunction(mCurrentFunctions,
                    UsbManager.USB_FUNCTION_AUDIO_SOURCE);
            if (enabled != mAudioSourceEnabled) {
                // send a sticky broadcast containing current USB state
                Intent intent = new Intent(Intent.ACTION_USB_AUDIO_ACCESSORY_PLUG);
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                intent.putExtra("state", (enabled ? 1 : 0));
                if (enabled) {
                    try {
                        Scanner scanner = new Scanner(new File(AUDIO_SOURCE_PCM_PATH));
                        int card = scanner.nextInt();
                        int device = scanner.nextInt();
                        intent.putExtra("card", card);
                        intent.putExtra("device", device);
                    } catch (FileNotFoundException e) {
                        Slog.e(TAG, "could not open audio source PCM file", e);
                    }
                }
                mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                mAudioSourceEnabled = enabled;
            }
        }

        @Override
        public void handleMessage(Message msg) {

            if (DEBUG) Log.w(TAG, "handleMessage - " + msg.what);

            switch (msg.what) {
                case MSG_UPDATE_STATE:
                case MSG_UPDATE_DISCONNECT_STATE:
                    if (mBatteryChargingUnPlug) {
						mBatteryChargingUnPlug = false;
					}

                    mConnected = (msg.arg1 == 1);
                    mConfigured = (msg.arg2 == 1);
                    updateUsbNotification();
                    updateAdbNotification();
                    if (containsFunction(mCurrentFunctions,
                            UsbManager.USB_FUNCTION_ACCESSORY)) {
                        updateCurrentAccessory();
                    }

                    if (mIsUsbBicrEvo) {
                        if (mHwDisconnected) {
                            mIsPcKnowMe = false;
                        } else if (!mIsUserSwitch && !mConnected && !mSettingUsbCharging) {
                            mIsPcKnowMe = false;
                        }
                    }

                    if (!mConnected && !mSettingUsbCharging && !mSettingUsbBicr) {
                        // restore defaults when USB is disconnected
						if (mDefaultFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
							mSettingUsbCharging = true;
							updateUsbState();
						} else {
							mSettingUsbCharging = false;
						}

                        SXlog.d(TAG, "handleMessage - MSG_UPDATE_STATE/MSG_UPDATE_DISCONNECT_STATE - mConnected: " + mConnected +
                                     ", mSettingUsbCharging: " + mSettingUsbCharging +
                                     ", mSettingUsbBicr: " + mSettingUsbBicr);

                        setEnabledFunctions(mDefaultFunctions, false);
                    }
                    if (mBootCompleted) {
                        updateUsbState();
                        updateAudioSourceFunction();

                        SXlog.d(TAG, "handleMessage mConnected:" + mConnected + ",mConfigured:" + mConfigured +
                                   ", mHwDisconnected:" + mHwDisconnected);
                    }
                    break;
                case MSG_ENABLE_ADB:
                    setAdbEnabled(msg.arg1 == 1);
                    break;
                case MSG_SET_CURRENT_FUNCTIONS:
                    String functions = (String)msg.obj;
                    boolean makeDefault = (msg.arg1 == 1);

                    mSettingUsbCharging = false;
                    mSettingUsbBicr = false;

                    /* In BICR evo, it's hard to confirm that the current disconnect is caused by switching usb function or unplugging usb cable*/
                    /* So add a flag to know it*/
                    mIsUserSwitch = true;

                    if (functions != null && functions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                        mSettingUsbCharging = true;
                        updateUsbState();
                        SXlog.d(TAG, "handleMessage - MSG_SET_CURRENT_FUNCTION - CHARGING_ONLY - makeDefault: " + makeDefault);
                    } else if (functions != null && functions.equals(UsbManager.USB_FUNCTION_BICR)) {
                        mSettingUsbBicr = true;
                        SXlog.d(TAG, "handleMessage - MSG_SET_CURRENT_FUNCTION - BICR - makeDefault: " + makeDefault);
                    } else if (functions == null && mDefaultFunctions.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                        functions = mDefaultFunctions;
                        mSettingUsbCharging = true;
                        makeDefault = true;
                        updateUsbState();
                        SXlog.d(TAG, "handleMessage - MSG_SET_CURRENT_FUNCTION - [Tethering Off] USB_FUNCTION_CHARGING_ONLY - makeDefault: " + makeDefault);
                    }

                    setEnabledFunctions(functions, makeDefault);

                    mIsUserSwitch = false;
                    //ALPS00428998
                    if(mMtpAskDisconnect) mMtpAskDisconnect = false;
                    //ALPS00428998

                    SXlog.d(TAG, "handleMessage - MSG_SET_CURRENT_FUNCTION - functions: " + functions);
                    break;
                case MSG_SYSTEM_READY:
                    updateAudioSourceFunction();
                    break;
                case MSG_BOOT_COMPLETED:
                    mBootCompleted = true;

                    //ALPS00112030 modification
                    //update while System ready is too early that the "SystemUIService(  314): loading: class com.android.systemui.statusbar.phone.PhoneStatusBar" is not ready
                    updateUsbNotification();
                    updateAdbNotification();
                    //ALPS00112030 modification

                    if (mCurrentAccessory != null) {
                        getCurrentSettings().accessoryAttached(mCurrentAccessory);
                    }
                    if (mDebuggingManager != null) {
                        mDebuggingManager.setAdbEnabled(mAdbEnabled);
                    }
                    //ALPS00112030 modification
                    if(mBootCompleted)
                        updateUsbState();
                    //ALPS00112030 modification
                    break;
                case MSG_USER_SWITCHED: {
                    final boolean mtpActive =
                            containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_MTP)
                            || containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_PTP);
                    if (mtpActive && mCurrentUser != UserHandle.USER_NULL) {
                        Slog.v(TAG, "Current user switched; resetting USB host stack for MTP");
                        setUsbConfig("none");
                        setUsbConfig(mCurrentFunctions);
                    }
                    mCurrentUser = msg.arg1;
                    break;
                }
            }
        }

        public UsbAccessory getCurrentAccessory() {
            return mCurrentAccessory;
        }

        private void updateUsbNotification() {
            if(DEBUG) {
                SXlog.d(TAG, "updateUsbNotification - mNotificationManager: " + mNotificationManager);
                SXlog.d(TAG, "updateUsbNotification - mUseUsbNotification: " + mUseUsbNotification);
            }

            if (mNotificationManager == null || !mUseUsbNotification) return;
            int id = 0;
            Resources r = mContext.getResources();

            if(DEBUG) {
                SXlog.w(TAG, "updateUsbNotification - mConnected: " + mConnected);
                SXlog.w(TAG, "updateUsbNotification - mCurrentFunctions: " + mCurrentFunctions);
            }

            if (mConnected || (mSettingUsbCharging && mPlugType == 2)) {
                if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_MTP)) {
                    id = com.android.internal.R.string.usb_mtp_notification_title;
                    SXlog.d(TAG, "updateUsbNotification - containsFunction:  USB_FUNCTION_MTP");
                } else if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_PTP)) {
                    id = com.android.internal.R.string.usb_ptp_notification_title;
                    SXlog.d(TAG, "updateUsbNotification - containsFunction:  USB_FUNCTION_PTP");
                } else if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_MASS_STORAGE)) {
                    SXlog.d(TAG, "updateUsbNotification - containsFunction:  USB_FUNCTION_MASS_STORAGE - mUsbStorageType: " + mUsbStorageType);
                    if (mUsbStorageType.equals(UsbManager.USB_FUNCTION_MTP)) {
                        id = com.android.internal.R.string.usb_cd_installer_notification_title;
                    } else {
                        id = com.mediatek.internal.R.string.usb_ums_notification_title;
                    }
                } else if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_ACCESSORY)) {
                    id = com.android.internal.R.string.usb_accessory_notification_title;
                } else if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
            		if (mSettingUsbCharging && mPlugType == 2) {
						id = com.mediatek.internal.R.string.usb_charging_notification_title;
						SXlog.d(TAG, "updateUsbNotification - containsFunction:  USB_FUNCTION_CHARGING_ONLY");
					}
                } else if (containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_BICR)) {
                    id = com.android.internal.R.string.usb_cd_installer_notification_title;
                    SXlog.d(TAG, "updateUsbNotification - containsFunction:  USB_FUNCTION_BICR");
                } else {
                    // There is a different notification for USB tethering so we don't need one here
                    if (!containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_RNDIS) && !containsFunction(mCurrentFunctions, UsbManager.USB_FUNCTION_EEM)) {
                        Slog.e(TAG, "No known USB function in updateUsbNotification");
                    }
                }
            }
            //ALPS00112030 modification
            //update while System ready is too early that the "SystemUIService(  314): loading: class com.android.systemui.statusbar.phone.PhoneStatusBar" is not ready
            if (id != mUsbNotificationId && mBootCompleted) {
            //ALPS00112030 modification
                // clear notification if title needs changing
                if (mUsbNotificationId != 0) {
                    mNotificationManager.cancelAsUser(null, mUsbNotificationId,
                            UserHandle.ALL);
                    mUsbNotificationId = 0;
                }
                if (id != 0) {
                    CharSequence message = r.getText(
                            com.android.internal.R.string.usb_notification_message);
                    CharSequence title = r.getText(id);

                    Notification notification = new Notification();
                    notification.icon = com.android.internal.R.drawable.stat_sys_data_usb;
                    notification.when = 0;
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    notification.tickerText = title;
                    notification.defaults = 0; // please be quiet
                    notification.sound = null;
                    notification.vibrate = null;
                    notification.priority = Notification.PRIORITY_DEFAULT;

                    Intent intent = Intent.makeRestartActivityTask(
                            new ComponentName("com.android.settings",
                                    "com.android.settings.UsbSettings"));
                    PendingIntent pi = PendingIntent.getActivityAsUser(mContext, 0,
                            intent, 0, null, UserHandle.CURRENT);
                    notification.setLatestEventInfo(mContext, title, message, pi);
                    mNotificationManager.notifyAsUser(null, id, notification,
                            UserHandle.ALL);
                    mUsbNotificationId = id;
                }
            }
        }

        private void updateAdbNotification() {
            if (mNotificationManager == null) return;
            final int id = com.android.internal.R.string.adb_active_notification_title;

            mAdbUpdateLock.lock();

            if (mAdbEnabled && mConnected && !mSettingUsbCharging) {
                if ("0".equals(SystemProperties.get("persist.adb.notify"))) return;

                //ALPS00112030 modification
                //update while System ready is too early that the "SystemUIService(  314): loading: class com.android.systemui.statusbar.phone.PhoneStatusBar" is not ready
                if (mBootCompleted && !mAdbNotificationShown) {
                //ALPS00112030 modification
                    Resources r = mContext.getResources();
                    CharSequence title = r.getText(id);
                    CharSequence message = r.getText(
                            com.android.internal.R.string.adb_active_notification_message);

                    Notification notification = new Notification();
                    notification.icon = com.android.internal.R.drawable.stat_sys_adb;
                    notification.when = 0;
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    notification.tickerText = title;
                    notification.defaults = 0; // please be quiet
                    notification.sound = null;
                    notification.vibrate = null;
                    notification.priority = Notification.PRIORITY_LOW;

                    Intent intent = Intent.makeRestartActivityTask(
                            new ComponentName("com.android.settings",
                                    "com.android.settings.DevelopmentSettings"));
                    PendingIntent pi = PendingIntent.getActivityAsUser(mContext, 0,
                            intent, 0, null, UserHandle.CURRENT);
                    notification.setLatestEventInfo(mContext, title, message, pi);
                    mAdbNotificationShown = true;
                    mNotificationManager.notifyAsUser(null, id, notification,
                            UserHandle.ALL);
                }
            } else if (mAdbNotificationShown) {
                mAdbNotificationShown = false;
                mNotificationManager.cancelAsUser(null, id, UserHandle.ALL);
            }
            mAdbUpdateLock.unlock();
        }

        public void dump(FileDescriptor fd, PrintWriter pw) {
            pw.println("  USB Device State:");
            pw.println("    Current Functions: " + mCurrentFunctions);
            pw.println("    Default Functions: " + mDefaultFunctions);
            pw.println("    mConnected: " + mConnected);
            pw.println("    mConfigured: " + mConfigured);
            pw.println("    mCurrentAccessory: " + mCurrentAccessory);
            try {
                pw.println("    Kernel state: "
                        + FileUtils.readTextFile(new File(STATE_PATH), 0, null).trim());
                pw.println("    Kernel function list: "
                        + FileUtils.readTextFile(new File(FUNCTIONS_PATH), 0, null).trim());
                pw.println("    Mass storage backing file: "
                        + FileUtils.readTextFile(new File(MASS_STORAGE_FILE_PATH), 0, null).trim());
            } catch (IOException e) {
                pw.println("IOException: " + e);
            }
        }
    }

    /* returns the currently attached USB accessory */
    public UsbAccessory getCurrentAccessory() {
        return mHandler.getCurrentAccessory();
    }

    /* opens the currently attached USB accessory */
    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        UsbAccessory currentAccessory = mHandler.getCurrentAccessory();
        if (currentAccessory == null) {
            throw new IllegalArgumentException("no accessory attached");
        }
        if (!currentAccessory.equals(accessory)) {
            String error = accessory.toString()
                    + " does not match current accessory "
                    + currentAccessory;
            throw new IllegalArgumentException(error);
        }
        getCurrentSettings().checkPermission(accessory);
        return nativeOpenAccessory();
    }

    public void setCurrentFunctions(String functions, boolean makeDefault) {
        if (DEBUG) Slog.d(TAG, "setCurrentFunctions(" + functions + ") default: " + makeDefault);
        mHandler.sendMessage(MSG_SET_CURRENT_FUNCTIONS, functions, makeDefault);
    }

    public void setMassStorageBackingFile(String path) {
        if (path == null) path = "";
        try {
            FileUtils.stringToFile(MASS_STORAGE_FILE_PATH, path);
        } catch (IOException e) {
           Slog.e(TAG, "failed to write to " + MASS_STORAGE_FILE_PATH);
        }
    }

    private void readOemUsbOverrideConfig() {
        String[] configList = mContext.getResources().getStringArray(
            com.android.internal.R.array.config_oemUsbModeOverride);

        if (configList != null) {
            for (String config: configList) {
                String[] items = config.split(":");
                if (items.length == 3) {
                    if (mOemModeMap == null) {
                        mOemModeMap = new HashMap<String, List<Pair<String, String>>>();
                    }
                    List overrideList = mOemModeMap.get(items[0]);
                    if (overrideList == null) {
                        overrideList = new LinkedList<Pair<String, String>>();
                        mOemModeMap.put(items[0], overrideList);
                    }
                    overrideList.add(new Pair<String, String>(items[1], items[2]));
                }
            }
        }
    }

    private boolean needsOemUsbOverride() {
        if (mOemModeMap == null) return false;

        String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, "unknown");
        return (mOemModeMap.get(bootMode) != null) ? true : false;
    }

    private String processOemUsbOverride(String usbFunctions) {
        if ((usbFunctions == null) || (mOemModeMap == null)) return usbFunctions;

        String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, "unknown");

        List<Pair<String, String>> overrides = mOemModeMap.get(bootMode);
        if (overrides != null) {
            for (Pair<String, String> pair: overrides) {
                if (pair.first.equals(usbFunctions)) {
                    Slog.d(TAG, "OEM USB override: " + pair.first + " ==> " + pair.second);
                    return pair.second;
                }
            }
        }
        // return passed in functions as is.
        return usbFunctions;
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        if (mDebuggingManager != null) {
            mDebuggingManager.allowUsbDebugging(alwaysAllow, publicKey);
        }
    }

    public void denyUsbDebugging() {
        if (mDebuggingManager != null) {
            mDebuggingManager.denyUsbDebugging();
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw) {
        if (mHandler != null) {
            mHandler.dump(fd, pw);
        }
        if (mDebuggingManager != null) {
            mDebuggingManager.dump(fd, pw);
        }
    }

    private native String[] nativeGetAccessoryStrings();
    private native ParcelFileDescriptor nativeOpenAccessory();
    private native boolean nativeIsStartRequested();
    private native int nativeGetAudioMode();
}
