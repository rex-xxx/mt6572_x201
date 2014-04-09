/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.google.common.collect.Lists;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.storage.StorageManagerEx;

import java.util.ArrayList;
import java.util.List;
/**
 * Panel showing storage usage on disk for known {@link StorageVolume} returned
 * by {@link StorageManager}. Calculates and displays usage of data types.
 */
public class Memory extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "MemorySettings";

    private static final String TAG_CONFIRM_CLEAR_CACHE = "confirmClearCache";

    private static final int DLG_CONFIRM_UNMOUNT = 1;
    private static final int DLG_ERROR_UNMOUNT = 2;
    private static final int DLG_CONFIRM_MOUNT = 3;

    public static final int H_UNMOUNT_ERROR = 11;

    private static final String DEFAULT_WRITE_CATEGORY_KEY = "memory_select";

    private Resources mResources;

    // The mountToggle Preference that has last been clicked.
    // Assumes no two successive unmount event on 2 different volumes are performed before the first
    // one's preference is disabled
    private static Preference sLastClickedMountToggle;
    private static String sClickedMountPoint;

    // whether the current unmounting device is USB.
    private boolean mIsUnmountingUsb = false;

    // Access using getMountService()
    private IMountService mMountService;

    private StorageManager mStorageManager;

    private UsbManager mUsbManager;

    private ArrayList<StorageVolumePreferenceCategory> mCategories = Lists.newArrayList();

    private RadioButtonPreference[] mStorageWritePathGroup;
    private String mDefaultWritePath;
    private PreferenceCategory mDefaultWriteCategory;
    private RadioButtonPreference mDeafultWritePathPref;
    private boolean[] mDefaultWritePathAdded;

    private static final String USB_STORAGE_PATH = "/mnt/usbotg";

    private static final String KEY_APK_INSTALLER = "apk_installer";

    private static final String KEY_APP_INSTALL_LOCATION = "app_install_location";

    // App installation location. Default is ask the user.
    private static final int APP_INSTALL_AUTO = 0;
    private static final int APP_INSTALL_DEVICE = 1;
    private static final int APP_INSTALL_SDCARD = 2;
    private static final String APP_INSTALL_DEVICE_ID = "device";
    private static final String APP_INSTALL_SDCARD_ID = "sdcard";
    private static final String APP_INSTALL_AUTO_ID = "auto";

    // dynamic swap sd card
    private static final String ACTION_DYNAMIC_SD_SWAP = "com.mediatek.SD_SWAP";
    private static final String SD_EXIST = "SD_EXIST";
    private static final int SD_INDEX = 1;

    private static final int ORDER_PHONE_STORAGE = -3;
    private static final int ORDER_SD_CARD = -2;
    private static final int ORDER_USB_OTG = -1;

    private ListPreference mInstallLocation;

    private StorageVolumePreferenceCategory mVolumePrefCategory;

    private Handler mUiHandler;
    private static final int MESSAGE_DELAY_TIME = 200;
    private boolean mIsRemovableVolume;
    private static final String EXTERNAL_STORAGE_PATH = "/storage/sdcard1";

    BroadcastReceiver mDynSwapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Receive dynamic sd swap broadcast");

            StorageVolume[] newVolumes = mStorageManager.getVolumeList();
            for (StorageVolume volume : newVolumes) {
                // update the storageVolumePreferenceCategory group
                for (StorageVolumePreferenceCategory category : mCategories) {
                    if(volume != null && category.getStorageVolume() != null) {
                        if (volume.getPath().equals(
                                category.getStorageVolume().getPath())) {
                            category.setStorageVolume(volume);
                            category.updateStorageVolumePrefCategory();
                        }
                    }
                }

                // update the default write disk group
                for (RadioButtonPreference pref : mStorageWritePathGroup) {
                    if (volume.getPath().equals(pref.getPath())) {
                        pref.setTitle(volume.getDescription(getActivity()));
                    }
                }
            }

            dynamicShowDefaultWriteCategory();

            //update the Install location preference
            resetInstallLocation(intent, newVolumes);
        }
    };

    private boolean mDefaultWriteDiskCategoryIsPresent = true;
    private boolean mIsApkInstallerExist = false;
    private boolean mIsInstLocSupport = false;
    private Preference mApkInstallePref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mStorageManager = StorageManager.from(context);
        mStorageManager.registerListener(mStorageListener);

        addPreferencesFromResource(R.xml.device_info_memory);

        mUiHandler = new Handler();

        mDefaultWriteCategory = (PreferenceCategory) findPreference(DEFAULT_WRITE_CATEGORY_KEY);
        mResources = getResources();

        if (!(FeatureOption.MTK_SHARED_SDCARD && FeatureOption.MTK_2SDCARD_SWAP)) {
            Log.d(TAG, "not sd + swap, add internal storage");
            addCategory(StorageVolumePreferenceCategory.buildForInternal(context));
        }

        // get the provided path list
        String[] mPathList = mStorageManager.getVolumePaths();
        StorageVolume[] volumes = mStorageManager.getVolumeList();

        // take off the absence sd card path,and mVolumePathList is the
        // available path list
        List<String> mVolumePathList = new ArrayList<String>();
        List<StorageVolume> storageVolumes = new ArrayList<StorageVolume>();

        for (int i = 0; i < mPathList.length; i++) {
            Log.d(TAG, "Volume: " + volumes[i].getDescription(getActivity()) + " ,state is: " + 
                    mStorageManager.getVolumeState(mPathList[i]) + " ,emulated is: " + volumes[i].isEmulated()
                    + ", path is " + volumes[i].getPath());

            if (!"not_present".equals(mStorageManager.getVolumeState(mPathList[i]))) {
                mVolumePathList.add(mPathList[i]);
                storageVolumes.add(volumes[i]);
            }
        }

        int length = storageVolumes.size();
        Log.d(TAG, "default path group length = " + length);

        mStorageWritePathGroup = new RadioButtonPreference[length];
        mDefaultWritePathAdded = new boolean[length];
        
        for (int i = 0; i < storageVolumes.size(); i++) {
            StorageVolume volume = storageVolumes.get(i);
            mStorageWritePathGroup[i] = new RadioButtonPreference(getActivity());
            mStorageWritePathGroup[i].setKey(mVolumePathList.get(i));
            mStorageWritePathGroup[i].setTitle(volume
                    .getDescription(getActivity()));
            mStorageWritePathGroup[i].setPath(mVolumePathList.get(i));
            mStorageWritePathGroup[i].setOnPreferenceChangeListener(this);
            if (FeatureOption.MTK_SHARED_SDCARD
                    && FeatureOption.MTK_2SDCARD_SWAP) {
                Log.d(TAG, "share + swap, add emulated category");
                addCategory(StorageVolumePreferenceCategory.buildForPhysical(
                        context, volume));
            } else if (!volume.isEmulated()) {
                Log.d(TAG, "no share + swap, add non-emulated category");
                addCategory(StorageVolumePreferenceCategory.buildForPhysical(
                        context, volume));
            }
        }

        setHasOptionsMenu(true);

        //Init Apk Installer preference when needed.
        initApkInstallerPreference();

        //Init Install Location preference when needed.
        initInstallLocationPreference(volumes);

        //Register a broadcast receiver for dynamic sd swap
        if (FeatureOption.MTK_2SDCARD_SWAP) {
            IntentFilter mFilter = new IntentFilter();
            mFilter.addAction(ACTION_DYNAMIC_SD_SWAP);
            getActivity().registerReceiver(mDynSwapReceiver, mFilter);
        }
    }

    /**
     * M: add the handler to pop-up dialog when unmount error
     */
    private Handler mUnMountErrorHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case H_UNMOUNT_ERROR:
                showDialogInner(DLG_ERROR_UNMOUNT);
                break;
            default:
                break;
            }
        }
    };

    private void addCategory(StorageVolumePreferenceCategory category) {
        mCategories.add(category);
        getPreferenceScreen().addPreference(category);
        category.init();
    }

    private boolean isMassStorageEnabled() {
        // Mass storage is enabled if primary volume supports it
        final StorageVolume[] volumes = mStorageManager.getVolumeList();
        final StorageVolume primary = StorageManager.getPrimaryVolume(volumes);
        return primary != null && primary.allowMassStorage();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_STATE);
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        //Remove DLG_CONFIRM_UNMOUNT Dialog when current state is UMS
        dynamicUpdateUnmountDlg();

        // update the preferred_install_location preference
        Log.d(TAG, "Dynamic Update Install Location in OnResume()");
        dynamicUpdateInstallLocation();

        // update the APK Installer preference
        dynamicUpdateAPKInstaller();

        dynamicShowDefaultWriteCategory();

        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onResume();
        }

    }

    private void dynamicUpdateUnmountDlg() {
        Log.d(TAG, "dynamicUpdateUnmountDlg()");

        for (int i = 0; i < mStorageWritePathGroup.length; i++) {
            String writePath = mStorageWritePathGroup[i].getPath();
            String volumeState = mStorageManager.getVolumeState(writePath);
            Log.d(TAG, "Path " + writePath + " volume state is " + volumeState);
            if (Environment.MEDIA_SHARED.equals(volumeState)) {
                Log.d(TAG, "current status is UMS");
                removeDialog(DLG_CONFIRM_UNMOUNT);
                return;
            }
        }
        Log.d(TAG, "current status is MTP");
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState,
                String newState) {
            Log.i(TAG, "onStorageStateChanged");

            mUiHandler.removeCallbacks(mUpdateRunnable);
            mUiHandler.postDelayed(mUpdateRunnable, MESSAGE_DELAY_TIME);

            Log.i(TAG, "Received storage state changed notification that " + path +
                    " changed state from " + oldState + " to " + newState);
            for (StorageVolumePreferenceCategory category : mCategories) {
                final StorageVolume volume = category.getStorageVolume();
                if (volume != null && path.equals(volume.getPath())) {
                    category.onStorageStateChanged();
                    break;
                }
            }

            // update the preferred_install_location preference
            Log.d(TAG, "Dynamic Update Install Location in storage listener");
            dynamicUpdateInstallLocation();

            // update the APK installer preference
            dynamicUpdateAPKInstaller();

        }
    };

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mMediaScannerReceiver);

        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onPause();
        }
    }

    @Override
    public void onDestroy() {
        mUiHandler.removeCallbacks(mUpdateRunnable);
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }

        if (FeatureOption.MTK_2SDCARD_SWAP) {
            getActivity().unregisterReceiver(mDynSwapReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.storage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem usb = menu.findItem(R.id.storage_usb);
        usb.setVisible(!isMassStorageEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.storage_usb:
                if (getActivity() instanceof PreferenceActivity) {
                    ((PreferenceActivity) getActivity()).startPreferencePanel(
                            UsbSettings.class.getCanonicalName(),
                            null,
                            R.string.storage_title_usb, null,
                            this, 0);
                } else {
                    startFragment(this, UsbSettings.class.getCanonicalName(), -1, null);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private synchronized IMountService getMountService() {
       if (mMountService == null) {
           IBinder service = ServiceManager.getService("mount");
           if (service != null) {
               mMountService = IMountService.Stub.asInterface(service);
           } else {
               Log.e(TAG, "Can't get mount service");
           }
       }
       return mMountService;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (StorageVolumePreferenceCategory.KEY_CACHE.equals(preference.getKey())) {
            ConfirmClearCacheFragment.show(this);
            return true;
        }

        for (StorageVolumePreferenceCategory category : mCategories) {
            Intent intent = category.intentForClick(preference);
            try {
                if (intent != null) {
                    // Don't go across app boundary if monkey is running
                    if (!Utils.isMonkeyRunning()) {
                        startActivity(intent);
                    }
                    return true;
               }
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.launch_error,
                        Toast.LENGTH_SHORT).show();
            }

            final StorageVolume volume = category.getStorageVolume();
            if (volume != null && category.mountToggleClicked(preference)) {
                sLastClickedMountToggle = preference;
                mIsRemovableVolume = volume.isRemovable();
                Log.d(TAG, "onPreferenceTreeClick, mIsRemovableVolume is " + mIsRemovableVolume);
                sClickedMountPoint = volume.getPath();
                mIsUnmountingUsb = sClickedMountPoint.equals(USB_STORAGE_PATH);
                String state = mStorageManager.getVolumeState(volume.getPath());
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    unmount();
                } else {
                    mount();
                }
                return true;
            }
        }
        
        return false;
    }

    private final BroadcastReceiver mMediaScannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_STATE)) {
               boolean isUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
               String usbFunction = mUsbManager.getDefaultFunction();
               for (StorageVolumePreferenceCategory category : mCategories) {
                   category.onUsbStateChanged(isUsbConnected, usbFunction);
               }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                for (StorageVolumePreferenceCategory category : mCategories) {
                    category.onMediaScannerFinished();
                }
            }
        }
    };

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DLG_CONFIRM_UNMOUNT:
            return new AlertDialog.Builder(getActivity())
                    .setTitle(
                            mIsUnmountingUsb ? R.string.dlg_confirm_unmount_usb_title
                                    : R.string.dlg_confirm_unmount_title)
                    .setPositiveButton(R.string.dlg_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    doUnmount();
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(
                            mIsUnmountingUsb ? R.string.dlg_confirm_unmount_usb_text
                                    : R.string.dlg_confirm_unmount_text)
                    .create();
        case DLG_ERROR_UNMOUNT:
            return new AlertDialog.Builder(getActivity())
                    .setTitle(
                            mIsUnmountingUsb ? R.string.dlg_error_unmount_usb_title
                                    : R.string.dlg_error_unmount_title)
                    .setNeutralButton(R.string.dlg_ok, null)
                    .setMessage(
                            mIsUnmountingUsb ? R.string.dlg_error_unmount_usb_text
                                    : R.string.dlg_error_unmount_text).create();
        case DLG_CONFIRM_MOUNT:
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_mount_external_sd_title)
                    .setPositiveButton(R.string.dlg_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    doMount();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                    .setMessage(R.string.dlg_mount_external_sd_summary)
                    .create();
        }
        return null;
    }

    private void doUnmount() {
        // Present a toast here
        if (mIsUnmountingUsb) {
            Toast.makeText(getActivity(), R.string.unmount_usb_inform_text,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.unmount_inform_text,
                    Toast.LENGTH_SHORT).show();
        }
        final IMountService mountService = getMountService();

        sLastClickedMountToggle.setEnabled(false);
        sLastClickedMountToggle.setTitle(mResources
                .getString(R.string.sd_ejecting_title));
        sLastClickedMountToggle.setSummary(mResources
                .getString(R.string.sd_ejecting_summary));

        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "unmountVolume" + sClickedMountPoint);
                try {
                    mountService.unmountVolume(sClickedMountPoint, true, false);
                } catch (RemoteException e) {
                    // Informative dialog to user that unmount failed.
                    mUnMountErrorHandler.sendEmptyMessage(H_UNMOUNT_ERROR);
                }
            }
        }.start();

    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    private boolean hasAppsAccessingStorage() throws RemoteException {
        IMountService mountService = getMountService();
        int stUsers[] = mountService.getStorageUsers(sClickedMountPoint);
        if (stUsers != null && stUsers.length > 0) {
            return true;
        }
        // TODO FIXME Parameterize with mountPoint and uncomment.
        // On HC-MR2, no apps can be installed on sd and the emulated internal storage is not
        // removable: application cannot interfere with unmount
        /*
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationInfo> list = am.getRunningExternalApplications();
        if (list != null && list.size() > 0) {
            return true;
        }
        */
        // Better safe than sorry. Assume the storage is used to ask for confirmation.
        return true;
    }

    private void unmount() {
        // Check if external media is in use.
        try {
            if (hasAppsAccessingStorage()) {
                // Present dialog to user
                showDialogInner(DLG_CONFIRM_UNMOUNT);
            } else {
                doUnmount();
            }
        } catch (RemoteException e) {
            // Very unlikely. But present an error dialog anyway
            Log.e(TAG, "Is MountService running?");
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void mount() {
        // in sd swap, mount the external sd card ,pop up a dialog to prompt
        // user
        // mount the external sd card will cause dynamic sd swap
        if (FeatureOption.MTK_2SDCARD_SWAP
                && sClickedMountPoint.equals(EXTERNAL_STORAGE_PATH) && mIsRemovableVolume) {
            showDialogInner(DLG_CONFIRM_MOUNT);
        } else {
            doMount();
        }
    }

    private void doMount() {
        final IMountService mountService = getMountService();
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mountService != null) {
                        Log.d(TAG, "mountVolume" + sClickedMountPoint);
                        mountService.mountVolume(sClickedMountPoint);
                    } else {
                        Log.e(TAG, "Mount service is null, can't mount");
                    }
                } catch (RemoteException e) {
                    // Not much can be done
                }
            }

        }.start();

    }

    private void onCacheCleared() {
        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onCacheCleared();
        }
    }

    private static class ClearCacheObserver extends IPackageDataObserver.Stub {
        private final Memory mTarget;
        private int mRemaining;

        public ClearCacheObserver(Memory target, int remaining) {
            mTarget = target;
            mRemaining = remaining;
        }

        @Override
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            synchronized (this) {
                if (--mRemaining == 0) {
                    mTarget.onCacheCleared();
                }
            }
        }
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(Memory parent) {
            if (!parent.isAdded()) {
                return;
            }

            final ConfirmClearCacheFragment dialog = new ConfirmClearCacheFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_CLEAR_CACHE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Memory target = (Memory) getTargetFragment();
                    final PackageManager pm = context.getPackageManager();
                    final List<PackageInfo> infos = pm.getInstalledPackages(0);
                    final ClearCacheObserver observer = new ClearCacheObserver(
                            target, infos.size());
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, observer);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference != null && preference instanceof RadioButtonPreference) {
            if (mDeafultWritePathPref != null) {
                mDeafultWritePathPref.setChecked(false);
            }
            StorageManagerEx.setDefaultPath(preference.getKey());
            mDeafultWritePathPref = (RadioButtonPreference) preference;
            return true;
        }
        return false;
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        public void run() {
            dynamicShowDefaultWriteCategory();
        }
    };

    private void dynamicShowDefaultWriteCategory() {
        Log.d(TAG, "dynamicShowDefaultWriteCategory()");
        String externalStoragePath = StorageManagerEx.getExternalStoragePath();
        Log.d(TAG, "external storage path= " + externalStoragePath);

        for (int i = 0; i < mStorageWritePathGroup.length; i++) {
            String writePath = mStorageWritePathGroup[i].getPath();
            String volumeState = mStorageManager.getVolumeState(writePath);
            Log.d(TAG, "Path " + writePath + " volume state is " + volumeState);

            //set default write path pref order
            if(writePath.equals(externalStoragePath)) {
                Log.d(TAG, "set the pref sd card order");
                mStorageWritePathGroup[i].setOrder(ORDER_SD_CARD);
            } else if(writePath.equals(USB_STORAGE_PATH)) {
                Log.d(TAG, "set the pref usb otg order");
                mStorageWritePathGroup[i].setOrder(ORDER_USB_OTG);
            } else {
                Log.d(TAG, "set the pref phone storage order");
                mStorageWritePathGroup[i].setOrder(ORDER_PHONE_STORAGE);
            }

            if (Environment.MEDIA_MOUNTED.equals(volumeState)) {
                if (!mDefaultWritePathAdded[i]) {
                    mDefaultWriteCategory
                            .addPreference(mStorageWritePathGroup[i]);
                    mDefaultWritePathAdded[i] = true;
                }
            } else {
                if (mDefaultWritePathAdded[i]) {
                    mStorageWritePathGroup[i].setChecked(false);
                    mDefaultWriteCategory
                            .removePreference(mStorageWritePathGroup[i]);
                    mDefaultWritePathAdded[i] = false;
                }
            }
        }
        int numberOfWriteDisk = mDefaultWriteCategory.getPreferenceCount();
        Log.d(TAG, "numberOfWriteDisk : " + numberOfWriteDisk);
        if (mDefaultWriteDiskCategoryIsPresent && numberOfWriteDisk == 0) {
            getPreferenceScreen().removePreference(mDefaultWriteCategory);
            mDefaultWriteDiskCategoryIsPresent = false;
        } else if (!mDefaultWriteDiskCategoryIsPresent && numberOfWriteDisk > 0) {
            getPreferenceScreen().addPreference(mDefaultWriteCategory);
            mDefaultWriteDiskCategoryIsPresent = true;
        }

        mDefaultWritePath = StorageManagerEx.getDefaultPath();
        Log.d(TAG, "get default path" + mDefaultWritePath);
        for (int i = 0; i < mStorageWritePathGroup.length; i++) {
            if (mStorageWritePathGroup[i].getPath().equals(mDefaultWritePath)) {
                mStorageWritePathGroup[i].setChecked(true);
                mDeafultWritePathPref = mStorageWritePathGroup[i];
            } else {
                mStorageWritePathGroup[i].setChecked(false);
            }
        }
    }

    /**
     *M : Init APK installer preference when create.
     */
    private void initApkInstallerPreference() {
        mIsApkInstallerExist = isPkgInstalled("com.mediatek.apkinstaller");
        if (mIsApkInstallerExist)  {
            mApkInstallePref = findPreference(KEY_APK_INSTALLER);
            Intent intent = new Intent();
            intent.setClassName("com.mediatek.apkinstaller",
                    "com.mediatek.apkinstaller.APKInstaller");
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            mApkInstallePref.setIntent(intent);
        } else {
            getPreferenceScreen().removePreference(
                            findPreference(KEY_APK_INSTALLER));
        }
    }

    /**
     * dynamic update apk installer preference
     */
    public void dynamicUpdateAPKInstaller() {
        if (!mIsApkInstallerExist) { return; }

        boolean flag = false;
        Log.d(TAG, "dynamicUpdateAPKInstaller()");
        for (int i = 0; i < mStorageWritePathGroup.length; i++) {
            String writePath = mStorageWritePathGroup[i].getPath();
            String volumeState = mStorageManager.getVolumeState(writePath);
            Log.d(TAG, "Path " + writePath + " volume state is " + volumeState);
            flag = flag || Environment.MEDIA_MOUNTED.equals(volumeState);
        }

        mApkInstallePref.setEnabled(flag);
    }

    /**
     *M: Judge packageName apk is installed or not
     */
    private boolean isPkgInstalled(String packageName) {
        if (packageName != null) {
            PackageManager pm = getPackageManager();
            try {
                pm.getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            Log.i(TAG, "the package name cannot be null!!");
            // disable the preference ? and change summary?
            return false;
        }
        return true;
    }

    protected void handleUpdateAppInstallLocation(final String value) {
        if (APP_INSTALL_DEVICE_ID.equals(value)) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.DEFAULT_INSTALL_LOCATION,
                    APP_INSTALL_DEVICE);
        } else if (APP_INSTALL_SDCARD_ID.equals(value)) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.DEFAULT_INSTALL_LOCATION,
                    APP_INSTALL_SDCARD);
        } else if (APP_INSTALL_AUTO_ID.equals(value)) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.DEFAULT_INSTALL_LOCATION, APP_INSTALL_AUTO);
        } else {
            // Should not happen, default to prompt...
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.DEFAULT_INSTALL_LOCATION, APP_INSTALL_AUTO);
        }
        mInstallLocation.setValue(value);
    }

    private String getAppInstallLocation() {
        int selectedLocation = Settings.Global.getInt(getContentResolver(),
                Settings.Global.DEFAULT_INSTALL_LOCATION, APP_INSTALL_AUTO);
        if (selectedLocation == APP_INSTALL_DEVICE) {
            return APP_INSTALL_DEVICE_ID;
        } else if (selectedLocation == APP_INSTALL_SDCARD) {
            return APP_INSTALL_SDCARD_ID;
        } else if (selectedLocation == APP_INSTALL_AUTO) {
            return APP_INSTALL_AUTO_ID;
        } else {
            // Default value, should not happen.
            return APP_INSTALL_AUTO_ID;
        }
    }

    /**
     *M : Init install location preference when create.
     */
    private void initInstallLocationPreference(StorageVolume[] volumes) {
        mIsInstLocSupport = (Settings.Global.getInt(getContentResolver(),
                Settings.Global.SET_INSTALL_LOCATION, 0) != 0);
        if (mIsInstLocSupport) {
            mInstallLocation = (ListPreference) findPreference(KEY_APP_INSTALL_LOCATION);
            String whereStr = "";
            for (int i = 0; i < volumes.length; i++) {
                if (volumes[i].getPath().equals(
                        Environment.getLegacyExternalStorageDirectory().getPath())) {
                    whereStr = volumes[i].getDescription(getActivity());
                    break;
                }
            }
            CharSequence[] entries = mInstallLocation.getEntries();
            entries[SD_INDEX] = whereStr;
            mInstallLocation.setEntries(entries);
            mInstallLocation.setValue(getAppInstallLocation());
            mInstallLocation
                    .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            String value = (String) newValue;
                            handleUpdateAppInstallLocation(value);
                            return false;
                        }
                    });
        } else {
            getPreferenceScreen().removePreference(
                    findPreference(KEY_APP_INSTALL_LOCATION));
        }
    }

    /**
     *M : Dynamic reset install location preference when receive
     *    SD dynamic SWAP broadcast.
     */
    private void resetInstallLocation(Intent intent, StorageVolume[] newVolumes) {
        if (!mIsInstLocSupport) { return; }

        Log.d(TAG, "resetInstallLocation()");
        // update the preferred install location
        boolean isExternalSD = intent.getBooleanExtra(SD_EXIST, false);
        mInstallLocation.setEnabled(isExternalSD);

        // reset the install location entries
        if (isExternalSD) {
            // get the SD description
            String sdDescription = "";
            for (int i = 0; i < newVolumes.length; i++) {
                if (newVolumes[i].getPath()
                        .equals(Environment.getLegacyExternalStorageDirectory()
                                .getPath())) {
                    sdDescription = newVolumes[i]
                            .getDescription(getActivity());
                    break;
                }
            }
            CharSequence[] entries = mInstallLocation.getEntries();
            entries[SD_INDEX] = sdDescription;
            mInstallLocation.setEntries(entries);
        }
    }

    /**
     *M : Dynamic update install location preference
     */
    public void dynamicUpdateInstallLocation() {
        if (!mIsInstLocSupport) { return; }

        Log.d(TAG, "dynamicUpdateInstallLocation()");
        for (int i = 0; i < mStorageWritePathGroup.length; i++) {
            String writePath = mStorageWritePathGroup[i].getPath();
            String volumeState = mStorageManager.getVolumeState(writePath);
            Log.d(TAG, "Path " + writePath + " volume state is " + volumeState);

            if (Environment.MEDIA_SHARED.equals(volumeState)) {
                Log.d(TAG, "current status is UMS");
                mInstallLocation.setEnabled(false);
                return;
            }
        }
        Log.d(TAG, "current status is not UMS");
        mInstallLocation.setEnabled(true);
        // when open the 2SDCARD SWAP feature and the external sd card is not
        // mounted, change the install location selection
        if (FeatureOption.MTK_2SDCARD_SWAP) {
            if (!Utils.isExSdcardInserted()) {
                Log.d(TAG,
                        "2SDCARD_SWAP feature , the external sd card is not mounted");
                mInstallLocation.setEnabled(false);
            }
        }
    }
}
