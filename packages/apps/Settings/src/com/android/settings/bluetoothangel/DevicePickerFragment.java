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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.ProgressCategory;

import com.android.settings.R;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class DevicePickerFragment extends DeviceListPreferenceFragment {

    private static final String TAG = "DevicePickerFragment";

    private boolean mNeedAuth;
    private String mLaunchPackage;
    private String mLaunchClass;
    private boolean mStartScanOnResume;

    private int mScanType;
    private IntentFilter mIntentFilter;

    private ProgressCategory mProgressCategory;
    private static final String KEY_BT_DEVICE_LIST = "bt_device_list";
    
    // / M: receive bt status change @{
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
            // BluetoothAdapter.ERROR);
            int state = mLocalAdapter.getBluetoothState();
            Log.d(TAG, "BluetoothAdapter state changed to" + state);
            handleStateChanged(state);
        }
    };

    // / @}

    @Override
    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.device_picker);

        Intent intent = getActivity().getIntent();
        mNeedAuth = intent.getBooleanExtra(
                BluetoothDevicePicker.EXTRA_NEED_AUTH, false);

        int filter = intent.getIntExtra(
                BluetoothDevicePicker.EXTRA_FILTER_TYPE,
                BluetoothDevicePicker.FILTER_TYPE_ALL);
        setFilter(filter);
        mLaunchPackage = intent
                .getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_PACKAGE);
        mLaunchClass = intent
                .getStringExtra(BluetoothDevicePicker.EXTRA_LAUNCH_CLASS);

        mScanType = LocalBluetoothAdapter.getInstance().getScanType(filter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(getString(R.string.device_picker));
        mStartScanOnResume = (savedInstanceState == null); // don't start scan
                                                           // after rotation
        mProgressCategory = (ProgressCategory) findPreference(KEY_BT_DEVICE_LIST);

        // / M: In onCreate(), register the bt state change receiver @{
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        // / @}
    }

    // / M: In onDestroy(), unregister the bt state change receiver @{
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    // / @}

    // / M: bt status turn to TURNING_OFF, finish the DevicePickerActivity @{
    void handleStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_TURNING_OFF) {
            Log.d(TAG, "Turning off Bt");
            getActivity().finish();
        }
    }

    // / @}

    @Override
    public void onResume() {
        super.onResume();
        // / M: before add device pref, firstly clear the screen
        mProgressCategory.setNoDeviceFoundAdded(false);
        removeAllDevices();
        addCachedDevices();
        if (mStartScanOnResume) {
            // / M: startScanning with the type 0 in order to search BR/EDR
            // device
            mLocalAdapter.startScanning(true, mScanType);
            mStartScanOnResume = false;
        }
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        mLocalAdapter.stopScanning();
        LocalBluetoothPreferences.persistSelectedDeviceInPicker(getActivity(),
                mSelectedDevice.getAddress());
        if ((btPreference.getCachedDevice().getBondState() == BluetoothDevice.BOND_BONDED)
                || !mNeedAuth) {
            sendDevicePickedIntent(mSelectedDevice);
            finish();
        } else {
            super.onDevicePreferenceClick(btPreference);
        }
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice,
            int bondState) {
        if (bondState == BluetoothDevice.BOND_BONDED) {
            BluetoothDevice device = cachedDevice.getDevice();
            if (device.equals(mSelectedDevice)) {
                sendDevicePickedIntent(device);
                finish();
            }
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);

        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            // / M: startScanning with the type 0 in order to search BR/EDR
            // device
            mLocalAdapter.startScanning(false, mScanType);
        }
    }

    private void sendDevicePickedIntent(BluetoothDevice device) {
        Intent intent = new Intent(BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        if (mLaunchPackage != null && mLaunchClass != null) {
            intent.setClassName(mLaunchPackage, mLaunchClass);
        }
        getActivity().sendBroadcast(intent);
    }
}
