/*
 * Copyright (C) 2007 Google Inc.
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

import com.android.internal.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.StorageEventListener;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;

import java.util.List;
import com.mediatek.xlog.SXlog;
import com.mediatek.common.featureoption.FeatureOption;

/**
 * This activity is shown to the user for him/her to enable USB mass storage
 * on-demand (that is, when the USB cable is connected). It uses the alert
 * dialog style. It will be launched from a notification.
 */
public class UsbStorageActivity extends Activity
        implements View.OnClickListener, OnCancelListener {
    private static final String TAG = "UsbStorageActivity";

    private Button mMountButton;
    private Button mUnmountButton;
    private ProgressBar mProgressBar;
    private TextView mBanner;
    private TextView mMessage;
    private ImageView mIcon;
    private StorageManager mStorageManager = null;
    private static final int DLG_CONFIRM_KILL_STORAGE_USERS = 1;
    private static final int DLG_ERROR_SHARING = 2;
    static final boolean localLOGV = true;
    private boolean mDestroyed;
    private boolean mHasCheck = false;
    static private boolean mSettingUMS = false;
    private int mAllowedShareNum = 0;
    private int mSharedCount = 0;

    // UI thread
    private Handler mUIHandler;

    // thread for working with the storage services, which can be slow
    private Handler mAsyncStorageHandler;

    /** Used to detect when the USB cable is unplugged, so we can call finish() */
    private BroadcastReceiver mUsbStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_STATE)) {
                handleUsbStateChanged(intent);
            }
        }
    };

    /*
    * Check how many storages can be shared on the device.
    * It seems that the device supported SHARED SD need to check.
    */
    private boolean isSharable() {

        /* if (!FeatureOption.MTK_SHARED_SDCARD) {
			return true;
        } */

        StorageVolume[] volumes = mStorageManager.getVolumeList();
        int allowedShareNum = 0;
        if (volumes != null) {
            SXlog.d(TAG, "isSharable - length:" + volumes.length);
            for (int i=0; i<volumes.length; i++) {
                if (volumes[i].allowMassStorage() && !volumes[i].isEmulated()) {
                    String pth = volumes[i].getPath();
                    String st = mStorageManager.getVolumeState(pth);
                    if (st != null) {
                        SXlog.d(TAG, "isSharable - allowMassStorage:" + volumes[i].allowMassStorage() + ", isEmulated:" + volumes[i].isEmulated());
                        /* Only count the number of the storage can be shared */
                        if ( !st.equals(Environment.MEDIA_UNMOUNTABLE) && !st.equals(Environment.MEDIA_NOFS) &&
                            !st.equals(Environment.MEDIA_REMOVED) && !st.equals(Environment.MEDIA_BAD_REMOVAL) ) {
                            allowedShareNum++;
                        }
                    }
                }
            }
        }
        SXlog.d(TAG, "isSharable - allowedShareNum:" + allowedShareNum);
        if (allowedShareNum == 0)
            return false;
        else
            return true;
    }

    private StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            final boolean on = newState.equals(Environment.MEDIA_SHARED);
            SXlog.d(TAG, "onStorageStateChanged - on: " + on + ", mSettingUMS: " + mSettingUMS + ", path: " + path + ", oldState: " + oldState + ", newState: " + newState);

            if (mSettingUMS) {
                StorageVolume[] volumes = mStorageManager.getVolumeList();
                SXlog.d(TAG, "onStorageStateChanged - [UMS Enabled] volumes.length: " + volumes.length + ", path: " + path + ", volumes state: " + newState + ", mAllowedShareNum: " + mAllowedShareNum + ", mSharedCount: " + mSharedCount);

                if (FeatureOption.MTK_SHARED_SDCARD) {
                    if (on) {
                        mSharedCount++;
                        SXlog.d(TAG, "onStorageStateChanged - [SSD] mSharedCount: " + mSharedCount);

                        if (mAllowedShareNum == mSharedCount) {
                            SXlog.d(TAG, "onStorageStateChanged - [All Shared] mSharedCount: " + mSharedCount);
                            switchDisplay(on);
                        }
                    } else {
                        if (!isSharable()) {
                            finish();
                        }
                    }
                } else {
                    if (!isSharable()) {
                        finish();
                    } else {
                        boolean haveShared = false;
                        for (int i = 0; i < volumes.length; i++) {
                            if (Environment.MEDIA_SHARED.equals(mStorageManager.getVolumeState(volumes[i].getPath()))) {
                                haveShared = true;
                                break;
                            }
                        }
                        SXlog.d(TAG, "onStorageStateChanged - haveShared: " + haveShared);
                        switchDisplay(haveShared);
                    }
                }
            } else {
                SXlog.d(TAG, "onStorageStateChanged - [UMS Disable] mSettingUMS: " + mSettingUMS + ", on: " + on);
                switchDisplay(on);

                /*
                * When no storage can be shared, just finish the usb storage activity.
                * It happened the device with shared SD.
                * 1. Insert SD card.
                * 2. Launch Usb storage activiy.
                * 3. Remove SD card.
                * 4. Usb storage activity should be gone auto.
                */
                if (!isSharable()) {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            if (mStorageManager == null) {
                Log.w(TAG, "Failed to get StorageManager");
            }
        }

        mSettingUMS = mSettingUMS | mStorageManager.isUsbMassStorageEnabled();
        Log.w(TAG, "mSettingUMS=" + mSettingUMS);

        if (FeatureOption.MTK_SHARED_SDCARD) {
            StorageVolume[] volumes = mStorageManager.getVolumeList();
            if (volumes != null) {
                for (int i=0; i<volumes.length; i++) {
                    if (volumes[i].allowMassStorage() && !volumes[i].isEmulated()) {
                        String path = volumes[i].getPath();
                        String st = mStorageManager.getVolumeState(path);
                        if (st != null) {
                            /* Only count the number of the storage can be shared */
                            if ( !st.equals(Environment.MEDIA_UNMOUNTABLE) && !st.equals(Environment.MEDIA_NOFS) &&
                                !st.equals(Environment.MEDIA_REMOVED) && !st.equals(Environment.MEDIA_BAD_REMOVAL) ) {
                                mAllowedShareNum++;
                            }
                        }
                    }
                }
            }
        }

        mUIHandler = new Handler();

        HandlerThread thr = new HandlerThread("SystemUI UsbStorageActivity");
        thr.start();
        mAsyncStorageHandler = new Handler(thr.getLooper());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if (Environment.isExternalStorageRemovable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        setContentView(com.android.internal.R.layout.usb_storage_activity);

        mIcon = (ImageView) findViewById(com.android.internal.R.id.icon);
        mBanner = (TextView) findViewById(com.android.internal.R.id.banner);
        mMessage = (TextView) findViewById(com.android.internal.R.id.message);

        mMountButton = (Button) findViewById(com.android.internal.R.id.mount_button);
        mMountButton.setOnClickListener(this);
        mUnmountButton = (Button) findViewById(com.android.internal.R.id.unmount_button);
        mUnmountButton.setOnClickListener(this);
        mProgressBar = (ProgressBar) findViewById(com.android.internal.R.id.progress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAsyncStorageHandler.getLooper().quit();
        mDestroyed = true;
    }

    private void switchDisplay(final boolean usbStorageInUse) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                switchDisplayAsync(usbStorageInUse);
            }
        });
    }

    private void switchDisplayAsync(boolean usbStorageInUse) {
        if (usbStorageInUse) {
            SXlog.d(TAG, "switchDisplayAsync - [Mount] usbStorageInUse:  " + usbStorageInUse);
            mProgressBar.setVisibility(View.GONE);
            mUnmountButton.setVisibility(View.VISIBLE);
            mMountButton.setVisibility(View.GONE);
            mIcon.setImageResource(com.android.internal.R.drawable.usb_android_connected);
            mBanner.setText(com.android.internal.R.string.usb_storage_stop_title);
            mMessage.setText(com.android.internal.R.string.usb_storage_stop_message);
        } else {
            SXlog.d(TAG, "switchDisplayAsync - [Unmount] usbStorageInUse:  " + usbStorageInUse);
            mProgressBar.setVisibility(View.GONE);
            mUnmountButton.setVisibility(View.GONE);
            mMountButton.setVisibility(View.VISIBLE);
            mIcon.setImageResource(com.android.internal.R.drawable.usb_android);
            mBanner.setText(com.android.internal.R.string.usb_storage_title);
            mMessage.setText(com.android.internal.R.string.usb_storage_message);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHasCheck = false;
        mStorageManager.registerListener(mStorageListener);
        registerReceiver(mUsbStateReceiver, new IntentFilter(UsbManager.ACTION_USB_STATE));
        try {
            mAsyncStorageHandler.post(new Runnable() {
                @Override
                public void run() {
                    switchDisplay(mStorageManager.isUsbMassStorageEnabled());
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read UMS enable state", ex);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mUsbStateReceiver);
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
    }

    private void handleUsbStateChanged(Intent intent) {
        boolean connected = intent.getExtras().getBoolean(UsbManager.USB_CONNECTED);
        boolean isUMSmode = intent.getExtras().getBoolean(UsbManager.USB_FUNCTION_MASS_STORAGE);

        SXlog.d(TAG, "handleUsbStateChanged - connected:  " + connected + ", isUMSmode: " + isUMSmode);
        if (!connected || !isUMSmode) {
            /** If the USB cable was unplugged when UMS was enabled, set the UMS enable flag to false */
            if (mSettingUMS) {
                mSettingUMS = false;
                SXlog.d(TAG, "handleUsbStateChanged - [Unplug when UMS enabled] connected:  " + connected);
            }
            // It was disconnected from the plug, so finish
			if (FeatureOption.MTK_SHARED_SDCARD) {
				mSharedCount = 0;
			}
            finish();
        }
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
    	Log.i(TAG, "onCreateDialoge");
        switch (id) {

        case DLG_CONFIRM_KILL_STORAGE_USERS:
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_confirm_kill_storage_users_title)
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        	mHasCheck = false;
                            switchUsbMassStorage(true);
                        }})
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    		mHasCheck = false;
                    	}})
                    .setMessage(R.string.dlg_confirm_kill_storage_users_text)
                    .setOnCancelListener(this)
                    .create();
        case DLG_ERROR_SHARING:
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_error_title)
                    .setNeutralButton(R.string.dlg_ok, null)
                    .setMessage(R.string.usb_storage_error_message)
                    .setOnCancelListener(this)
                    .create();
        }
        return null;
    }

    private void scheduleShowDialog(final int id) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mDestroyed) {
                    removeDialog(id);
                    showDialog(id);
                }
            }
        });
    }

    private void switchUsbMassStorage(final boolean on) {
        // things to do on the UI thread
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                mUnmountButton.setVisibility(View.GONE);
                mMountButton.setVisibility(View.GONE);

                mProgressBar.setVisibility(View.VISIBLE);
                // will be hidden once USB mass storage kicks in (or fails)
            }
        });

        // things to do elsewhere
        mAsyncStorageHandler.post(new Runnable() {
            @Override
            public void run() {
				if (FeatureOption.MTK_SHARED_SDCARD) {
					mSharedCount = 0;
				}
                if (on) {
                    mSettingUMS = true;
                    mStorageManager.enableUsbMassStorage();
                } else {
                    mSettingUMS = false;
                    mStorageManager.disableUsbMassStorage();
                }
            }
        });
    }

    private void checkStorageUsers() {
        mAsyncStorageHandler.post(new Runnable() {
            @Override
            public void run() {
                checkStorageUsersAsync();
            }
        });
    }

    private void checkStorageUsersAsync() {
        IMountService ims = getMountService();
        if (ims == null) {
            // Display error dialog
            scheduleShowDialog(DLG_ERROR_SHARING);
        }
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        boolean showDialog = false;
        try {
            int[] stUsers = ims.getStorageUsers(extStoragePath);
            if (stUsers != null) {
                SXlog.d(TAG, "checkStorageUsersAsync - stUsers.length: " + stUsers.length);
            } else {
                SXlog.d(TAG, "checkStorageUsersAsync - [NO Storage Users]");
            }
            if (stUsers != null && stUsers.length > 0) {
                showDialog = true;
            } else {
                // List of applications on sdcard.
                ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
                List<ApplicationInfo> infoList = am.getRunningExternalApplications();
                if (infoList != null) {
                    SXlog.d(TAG, "checkStorageUsersAsync - infoList.size(): " + infoList.size());
                } else {
                    SXlog.d(TAG, "checkStorageUsersAsync - [NO EXT RUNNING APPS]");
                }
                if (infoList != null && infoList.size() > 0) {
                    showDialog = true;
                }
            }
        } catch (RemoteException e) {
            // Display error dialog
            scheduleShowDialog(DLG_ERROR_SHARING);
        }
        if (showDialog) {
            SXlog.d(TAG, "checkStorageUsersAsync - [SHOW DIALOG] showDialog: " + showDialog);
            // Display dialog to user
            scheduleShowDialog(DLG_CONFIRM_KILL_STORAGE_USERS);
        } else {
            SXlog.d(TAG, "checkStorageUsersAsync - [NO DIALOG] showDialog: " + showDialog);
            if (localLOGV) Log.i(TAG, "Enabling UMS");
            switchUsbMassStorage(true);
        }
    }

    public void onClick(View v) {
    	Log.i(TAG, "onClickaa"+ mHasCheck);
        if (v == mMountButton) {
           // Check for list of storage users and display dialog if needed.

            if(false == mHasCheck) {
                Log.i(TAG, "onClick"+ mHasCheck);
                mHasCheck = true;
                if (FeatureOption.MTK_SHARED_SDCARD) {
                    /*No storage can be shared. Change UI directedly.*/
                    if (mAllowedShareNum == 0)
                        switchDisplay(true);
                    else
                        checkStorageUsers();
                } else
                    checkStorageUsers();
            }
        } else if (v == mUnmountButton) {
            if (localLOGV) Log.i(TAG, "Disabling UMS");
            mHasCheck = false;
            switchUsbMassStorage(false);
        }
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }

}
