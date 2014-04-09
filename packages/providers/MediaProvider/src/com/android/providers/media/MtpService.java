/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.media;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDatabase;
import android.mtp.MtpServer;
import android.mtp.MtpStorage;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.UEventObserver;
import android.provider.MediaStore;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.SXlog;

import java.io.File;
import java.util.HashMap;

public class MtpService extends Service {
    private static final String TAG = "MtpService";
    private static final boolean LOGD = true;

    // We restrict PTP to these subdirectories
    private static final String[] PTP_DIRECTORIES = new String[] {
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_PICTURES,
    };

    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    private static final String MTP_OPERATION_DEV_PATH =
            "DEVPATH=/devices/virtual/misc/mtp_usb";
    /// M: @}

    // Add for update Storage
    private boolean mIsSDExist = false;
    private static final String SD_EXIST = "SD_EXIST";
    private static final String ACTION_DYNAMIC_SD_SWAP = "com.mediatek.SD_SWAP";

    // Add for update Storage

    private void addStorageDevicesLocked() {
        if (mPtpMode) {
            // In PTP mode we support only primary storage
            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
            final String path = primary.getPath();
            if (path != null) {
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    addStorageLocked(mVolumeMap.get(path));
                }
            }
        } else {
            for (StorageVolume volume : mVolumeMap.values()) {
                addStorageLocked(volume);
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /// M: ALPS00120037, add log for support MTP debugging @{
            MtkLog.w(TAG, "ACTION_USER_PRESENT: BroadcastReceiver: onReceive: synchronized");

            final String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                synchronized (mBinder) {
                    /// M: Added Modification for ALPS00273682/ALPS00279547
                    MtkLog.w(TAG, "ACTION_USER_PRESENT: BroadcastReceiver: mMtpDisabled " + mMtpDisabled);

                    // Unhide the storage units when the user has unlocked the lockscreen
                    if (mMtpDisabled) {
                        addStorageDevicesLocked();
                        mMtpDisabled = false;
                    }
                }
            }
            /// M: Added Modification for ALPS00273682/ALPS00279547 @{
            if (!mMtpDisabled) {
                MtkLog.w(TAG, "The KeyGuard unlock has been received, ");
                unregisterReceiver(mReceiver);
            }
            /// M: @}
        }
    };

    /// M: Added for Storage Update @{
    private final BroadcastReceiver mLocaleChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MtkLog.w(TAG, "ACTION_LOCALE_CHANGED: BroadcastReceiver: onReceive: synchronized");

            final String action = intent.getAction();
            if (Intent.ACTION_LOCALE_CHANGED.equals(action) && !mMtpDisabled) {
                synchronized (mBinder) {
                    MtkLog.w(TAG, "ACTION_LOCALE_CHANGED : BroadcastReceiver: onReceive: synchronized");

                    StorageVolume[] volumes = mStorageManager.getVolumeList();
                    mVolumes = volumes;

                    for (int i = 0; i < mVolumes.length; i++) {
                        StorageVolume volume = mVolumes[i];
                        updateStorageLocked(volume);
                    }
                }

            }

        }
    };
    private final BroadcastReceiver mSDSwapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MtkLog.w(TAG, "ACTION_DYNAMIC_SD_SWAP: BroadcastReceiver: onReceive: synchronized");

            final String action = intent.getAction();
            boolean swapSD;
            if (ACTION_DYNAMIC_SD_SWAP.equals(action) && !mMtpDisabled) {
                synchronized (mBinder) {
                    mIsSDExist = intent.getBooleanExtra(SD_EXIST, false);

                    MtkLog.w(TAG, "ACTION_DYNAMIC_SD_SWAP : BroadcastReceiver: swapSD = " + mIsSDExist);

                    StorageVolume[] volumes = mStorageManager.getVolumeList();
                    mVolumes = volumes;

                    for (int i = 0; i < mVolumes.length; i++) {
                        StorageVolume volume = mVolumes[i];
                        updateStorageLocked(volume);
                    }
                }

            }

        }
    };
    /// M: @}

    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            synchronized (mBinder) {
                Log.d(TAG, "onStorageStateChanged " + path + " " + oldState + " -> " + newState);
                if (Environment.MEDIA_MOUNTED.equals(newState)) {
					//Modification for ALPS00365000, Scan mStorageMap for checking if there is the same storage under current StorageList
					int isExist = 0;
					for (MtpStorage storage : mStorageMap.values()) {
						MtkLog.w(TAG, "onStorageStateChanged storage.getPath() = " + storage.getPath());
						MtkLog.w(TAG, "onStorageStateChanged storage.getStorageId() = 0x" + Integer.toHexString(storage.getStorageId()));
						
						if(path.equals(storage.getPath()))
							isExist=1;
						MtkLog.w(TAG, "onStorageStateChanged, isExist = " + isExist);
					}
					if(isExist==0)
					//Modification for ALPS00365000, Scan mStorageMap for checking if there is the same storage under current StorageList
					volumeMountedLocked(path);
                } else if (Environment.MEDIA_MOUNTED.equals(oldState)) {
                    StorageVolume volume = mVolumeMap.remove(path);
                    if (volume != null) {
                        removeStorageLocked(volume);
                    }
                }
            }
        }
    };

    private MtpDatabase mDatabase;
    private MtpServer mServer;
    private StorageManager mStorageManager;
    /** Flag indicating if MTP is disabled due to keyguard */
    private boolean mMtpDisabled; // true if MTP is disabled due to secure keyguard
    private boolean mPtpMode;
    //ALPS00444854
	private boolean isUsbConfigured;
    //ALPS00444854
    private final HashMap<String, StorageVolume> mVolumeMap = new HashMap<String, StorageVolume>();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<String, MtpStorage>();
    private StorageVolume[] mVolumes;

    @Override
    public void onCreate() {
		//ALPS00525202
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        final KeyguardManager keyguardManager = (KeyguardManager) getSystemService(
                Context.KEYGUARD_SERVICE);

        if (LOGD) {
            Log.w(TAG, "updating state; keyguardManager.isKeyguardLocked()=" + keyguardManager.isKeyguardLocked() + ", keyguardManager.isKeyguardSecure()="
                    + keyguardManager.isKeyguardSecure());
        	}
        mMtpDisabled = (keyguardManager.isKeyguardLocked() && keyguardManager.isKeyguardSecure()) || !isCurrentUser;

		if(mMtpDisabled)	//if mtp disable because of secure keyguard
		{
			if (LOGD) {
				Log.w(TAG, "secure keyguard enable -> register for unlock (USER_PRESENT intent)");
				}
		//ALPS00525202
	        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
		//ALPS00525202
		}
		//ALPS00525202
        /// M: Added for Storage Update @{
        registerReceiver(mLocaleChangedReceiver, new IntentFilter(Intent.ACTION_LOCALE_CHANGED));

        if (FeatureOption.MTK_2SDCARD_SWAP) {
            registerReceiver(mSDSwapReceiver, new IntentFilter(ACTION_DYNAMIC_SD_SWAP));
        }
        /// M: @}

        mStorageManager = StorageManager.from(this);
        synchronized (mBinder) {
            updateDisabledStateLocked();
            mStorageManager.registerListener(mStorageEventListener);
            StorageVolume[] volumes = mStorageManager.getVolumeList();
            mVolumes = volumes;
            /// M: ALPS00241636, add log for support MTP debugging
            SXlog.d(TAG, "onCreate: volumes.length=" + volumes.length);
            for (int i = 0; i < volumes.length; i++) {
                String path = volumes[i].getPath();
                String state = mStorageManager.getVolumeState(path);
                /// M: ALPS00241636, add log for support MTP debugging @{
                SXlog.d(TAG, "onCreate: path of volumes[" + i + "]=" + path);
                SXlog.d(TAG, "onCreate: state of volumes[" + i + "]=" + state);
                /// M: @}
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    volumeMountedLocked(path);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (mBinder) {
            updateDisabledStateLocked();
		    //ALPS00449601
			isUsbConfigured = (intent == null ? false
				:intent.getBooleanExtra(UsbManager.USB_CONFIGURED, false));
		    //ALPS00449601

            mPtpMode = (intent == null ? false
                    : intent.getBooleanExtra(UsbManager.USB_FUNCTION_PTP, false));
            String[] subdirs = null;
            if (mPtpMode) {
                int count = PTP_DIRECTORIES.length;
                subdirs = new String[count];
                for (int i = 0; i < count; i++) {
                    File file =
                            Environment.getExternalStoragePublicDirectory(PTP_DIRECTORIES[i]);
                    // make sure this directory exists
                    file.mkdirs();
                    subdirs[i] = file.getPath();
                }
            }
            //ALPS00444854
            /*
            		final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
		      mDatabase = new MtpDatabase(this, MediaProvider.EXTERNAL_VOLUME,
			             	primary.getPath(), subdirs);
		      manageServiceLocked();
		       */

			if(isUsbConfigured)
            {
	            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
	            mDatabase = new MtpDatabase(this, MediaProvider.EXTERNAL_VOLUME,
	        	            primary.getPath(), subdirs);
	            manageServiceLocked();
			}
            //ALPS00444854

        }

        return START_STICKY;
    }

    private void updateDisabledStateLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        final KeyguardManager keyguardManager = (KeyguardManager) getSystemService(
                Context.KEYGUARD_SERVICE);
		//ALPS00525202
        if (LOGD) {
            Log.w(TAG, "updating state; keyguardManager.isKeyguardLocked()=" + keyguardManager.isKeyguardLocked() + ", keyguardManager.isKeyguardSecure()="
                    + keyguardManager.isKeyguardSecure());
        	}
		//ALPS00525202
        mMtpDisabled = (keyguardManager.isKeyguardLocked() && keyguardManager.isKeyguardSecure())
                || !isCurrentUser;
        if (LOGD) {
            Log.d(TAG, "updating state; isCurrentUser=" + isCurrentUser + ", mMtpLocked="
                    + mMtpDisabled);
        }
    }

    /**
     * Manage {@link #mServer}, creating only when running as the current user.
     */
    private void manageServiceLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        if (mServer == null && isCurrentUser) {
            Log.d(TAG, "starting MTP server in " + (mPtpMode ? "PTP mode" : "MTP mode"));
            mServer = new MtpServer(mDatabase, mPtpMode);
            if (!mMtpDisabled) {
                addStorageDevicesLocked();
            }
            mServer.start();
        } else if (mServer != null && !isCurrentUser) {
            Log.d(TAG, "no longer current user; shutting down MTP server");
            // Internally, kernel will close our FD, and server thread will
            // handle cleanup.
            mServer = null;
        }
    }

    @Override
    public void onDestroy() {
        /// M: Added Modification for ALPS00273682/ALPS00279547
        MtkLog.w(TAG, "onDestroy: mMtpDisabled " + mMtpDisabled);
        if (mMtpDisabled) {
            unregisterReceiver(mReceiver);
        }
        mStorageManager.unregisterListener(mStorageEventListener);
        /// M: Added for Storage Update @{
        unregisterReceiver(mLocaleChangedReceiver);
        if (FeatureOption.MTK_2SDCARD_SWAP) {
            unregisterReceiver(mSDSwapReceiver);
        }
        /// M: @}
    }

    private final IMtpService.Stub mBinder =
            new IMtpService.Stub() {
        public void sendObjectAdded(int objectHandle) {
            synchronized (mBinder) {
                /// M: ALPS00120037, add log for support MTP debugging
                // MtkLog.w(TAG, "mBinder: sendObjectAdded!!");
                if (mServer != null) {
                    mServer.sendObjectAdded(objectHandle);
                }
            }
        }

        public void sendObjectRemoved(int objectHandle) {
            synchronized (mBinder) {
                /// M: ALPS00120037, add log for support MTP debugging
                // MtkLog.w(TAG, "mBinder: sendObjectRemoved!!");
                if (mServer != null) {
                    mServer.sendObjectRemoved(objectHandle);
                }
            }
        }

        /// M: ALPS00289309, update Object @{
        public void sendObjectInfoChanged(int objectHandle) {
            synchronized (mBinder) {
                MtkLog.w(TAG, "mBinder: sendObjectInfoChanged, objectHandle = 0x" + Integer.toHexString(objectHandle));
                if (mServer != null) {
                    mServer.sendObjectInfoChanged(objectHandle);
                }
            }
        }
        /// M: @}

        /// M: Added for Storage Update @{
        public void sendStorageInfoChanged(MtpStorage storage) {
            synchronized (mBinder) {
                MtkLog.w(TAG, "mBinder: sendObjectInfoChanged, storage.getStorageId = 0x"
                        + Integer.toHexString(storage.getStorageId()));
                if (mServer != null) {
                    mServer.sendStorageInfoChanged(storage);
                }
            }
        }
        /// M: @}
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void volumeMountedLocked(String path) {
        // Add for update Storage
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        mVolumes = volumes;
        // Add for update Storage
        for (int i = 0; i < mVolumes.length; i++) {
            StorageVolume volume = mVolumes[i];
            if (volume.getPath().equals(path)) {
                mVolumeMap.put(path, volume);
                if (!mMtpDisabled) {
                    // In PTP mode we support only primary storage
                    if (volume.isPrimary() || !mPtpMode) {
                        addStorageLocked(volume);
                    }
                }
                break;
            }
        }
    }

    private void addStorageLocked(StorageVolume volume) {
        /// M: ALPS00332280 @{
        if (volume == null) {
            SXlog.e(TAG, "addStorageLocked: No storage was mounted!");
            return;
        }
        /// M: @}

        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        String path = storage.getPath();
        mStorageMap.put(path, storage);

        Log.d(TAG, "addStorageLocked " + storage.getStorageId() + " " + path);
        if (mDatabase != null) {
            /// M: ALPS00241636, add log for support MTP debugging
            SXlog.d(TAG, "addStorageLocked: add storage " + storage.getPath() + " into MtpDatabase");
            mDatabase.addStorage(storage);
        }
        if (mServer != null) {
            /// M: ALPS00241636, add log for support MTP debugging
            SXlog.d(TAG, "addStorageLocked: add storage " + storage.getPath() + " into MtpServer");
            mServer.addStorage(storage);
        }
    }

    /// M: Added for Storage Update @{
    private void updateStorageLocked(StorageVolume volume) {
        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        MtkLog.w(TAG, "updateStorageLocked " + storage.getStorageId() + " = " + storage.getStorageId());

        if (mServer != null) {
            SXlog.d(TAG, "updateStorageLocked: updateStorageLocked storage " + storage.getPath() + " into MtpServer");
            mServer.updateStorage(storage);
        }
    }
    /// M: @}

    private void removeStorageLocked(StorageVolume volume) {
        MtpStorage storage = mStorageMap.remove(volume.getPath());
        if (storage == null) {
            Log.e(TAG, "no MtpStorage for " + volume.getPath());
            return;
        }

        Log.d(TAG, "removeStorageLocked " + storage.getStorageId() + " " + storage.getPath());
        if (mDatabase != null) {
            mDatabase.removeStorage(storage);
        }
        if (mServer != null) {
            mServer.removeStorage(storage);
        }
    }

    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    private final UEventObserver mUEventObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            MtkLog.w(TAG, "USB UEVENT: " + event.toString());

            String mtp = event.get("MTP");

            if (mtp != null) {
                MtkLog.w(TAG, "mMtpSessionEnd: end the session");
                mServer.endSession();
            } else {
                MtkLog.w(TAG, "Not MTP string");
            }
        }
    };
    /// M: @}
}
