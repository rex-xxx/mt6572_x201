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

package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

/**
 * USB storage settings.
 */
public class UsbSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "UsbSettings";

    private static final String KEY_UMS = "usb_ums";
    private static final String KEY_MTP = "usb_mtp";
    private static final String KEY_PTP = "usb_ptp";
    private static final String KEY_CHARGE = "usb_charge";
    private static final String KEY_BICR = "usb_bicr";
    private static final int USB_CHARGING_PHLUGIN = 2;

    private UsbManager mUsbManager;
    private UsbPreference mMtp;
    private UsbPreference mPtp;
    private UsbPreference mUms;
    private UsbPreference mCharge;
    private UsbPreference mBicr;

    private boolean mUsbAccessoryMode;

    private boolean mUmsExist = true;
    private boolean mChargeExist = true;
    private boolean mBicrExist = true;
    private boolean mIsHwUsbConnected = false;
    private boolean mIsPcKnowMe = false;
    private int mPlugType = USB_CHARGING_PHLUGIN;
    private boolean mCanUpdateToggle = true;
    private String mCurrentToggles = "";

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            String currentFunction = getCurrentFunction();

            if (action.equals(UsbManager.ACTION_USB_STATE)) {
               mUsbAccessoryMode = intent.getBooleanExtra(UsbManager.USB_FUNCTION_ACCESSORY, false);
               Log.e(TAG, "UsbAccessoryMode " + mUsbAccessoryMode);                
               
               mIsHwUsbConnected = !intent.getBooleanExtra(
                        "USB_HW_DISCONNECTED", false);

                mIsPcKnowMe = intent.getBooleanExtra("USB_IS_PC_KNOW_ME", true);

                Log.d(TAG, "[ACTION_USB_STATE]" + ", mIsHwUsbConnected :"
                        + mIsHwUsbConnected + ", mIsPcKnowMe :" + mIsPcKnowMe);

                if (mIsHwUsbConnected) {
                    /// M: Before the current function set successfully,
                    /// M: Do not update UI because it will cause the UI flicker @{
                    if (mCurrentToggles.equals(currentFunction) ||
                                !mIsPcKnowMe && !mCurrentToggles.equals(UsbManager.USB_FUNCTION_CHARGING_ONLY)) {
                        mCanUpdateToggle = true;
                    }

                    if (mCanUpdateToggle) {
                        Log.d(TAG, "[Update Toggle - Other Functions]");
                        updateToggles(currentFunction);
                    }
                    /// M :@}
                } else if (!currentFunction.equals("charging")) {
                    Log.d(TAG, "[Finish Activity]");
                    finish();
                }
            }

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mPlugType = intent.getIntExtra("plugged", 0);
                Log.d(TAG, "[ACTION_BATTERY_CHANGED]" + ", mPlugType :"
                        + mPlugType);
                if (mPlugType == USB_CHARGING_PHLUGIN) {
                    /// M: Only when the current function is Charging,
                    /// M: receive the ACTION_BATTERY_CHANGED, and update UI
                    /// M: because when change the function and in this case
                    /// M: receive a ACTION_BATTERY_CHANGE, it will update UI
                    /// M: and the UI will display wrong @{
                    if (currentFunction.equals("charging")) {
                        Log.d(TAG, "[Update Toggle - USB Charging]");
                        updateToggles(currentFunction);
                    }
                    /// @}
                } else {
                    Log.d(TAG, "[Finish Activity - USB Charging Unplugged]");
                    finish();
                }
            }
        }
    };

    private String getCurrentFunction() {
        String functions = android.os.SystemProperties.get("sys.usb.config",
                "none");
        Log.d(TAG, "current function: " + functions);
        int commandIndex = functions.indexOf(',');
        if (commandIndex > 0) {
            return functions.substring(0, commandIndex);
        } else {
            return functions;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.usb_settings);
        root = getPreferenceScreen();

        mMtp = (UsbPreference) root.findPreference(KEY_MTP);
        mMtp.setOnPreferenceChangeListener(this);

        mPtp = (UsbPreference) root.findPreference(KEY_PTP);
        mPtp.setOnPreferenceChangeListener(this);

        mUms = (UsbPreference) root.findPreference(KEY_UMS);
        mUms.setOnPreferenceChangeListener(this);

        mCharge = (UsbPreference) root.findPreference(KEY_CHARGE);
        mCharge.setOnPreferenceChangeListener(this);

        mBicr = (UsbPreference) root.findPreference(KEY_BICR);
        mBicr.setOnPreferenceChangeListener(this);

        String config = android.os.SystemProperties.get(
                "ro.sys.usb.storage.type", "mtp");
        if (!config.equals(UsbManager.USB_FUNCTION_MTP + ","
                + UsbManager.USB_FUNCTION_MASS_STORAGE)) {
            root.removePreference(mUms);
            mUmsExist = false;
        }

        String chargeConfig = android.os.SystemProperties.get(
                "ro.sys.usb.charging.only", "no");
        Log.d(TAG, "ro.sys.usb.charging.only: " + chargeConfig);
        if (chargeConfig.equals("no")) {
            Log.d(TAG, "Usb Charge does not exist!");
            root.removePreference(mCharge);
            mChargeExist = false;
        }

        String bicrConfig = android.os.SystemProperties.get("ro.sys.usb.bicr",
                "no");
        Log.d(TAG, "ro.sys.usb.bicr: " + bicrConfig);
        if (bicrConfig.equals("no")) {
            Log.d(TAG, "Usb Bicr does not exist!");
            PreferenceCategory cdromCategory = (PreferenceCategory)findPreference("usb_connect_as_cdrom_category");
            root.removePreference(mBicr);
            root.removePreference(cdromCategory);

            mBicrExist = false;
        }

        return root;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mStateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these
        // settings
        // depend on others...
        createPreferenceHierarchy();

        // ACTION_USB_STATE is sticky so this will call updateToggles
        IntentFilter filter = new IntentFilter();

        filter.addAction(UsbManager.ACTION_USB_STATE);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);

        getActivity().registerReceiver(mStateReceiver, filter);
    }

    private void updateToggles(String function) {
        if (UsbManager.USB_FUNCTION_MTP.equals(function)) {
            mMtp.setChecked(true);
            mPtp.setChecked(false);
            if (mUmsExist) {
                mUms.setChecked(false);
            }
            if (mChargeExist) {
                mCharge.setChecked(false);
            }
            if (mBicrExist) {
                mBicr.setChecked(false);
            }
        } else if (UsbManager.USB_FUNCTION_PTP.equals(function)) {
            mMtp.setChecked(false);
            mPtp.setChecked(true);
            if (mUmsExist) {
                mUms.setChecked(false);
            }
            if (mChargeExist) {
                mCharge.setChecked(false);
            }
            if (mBicrExist) {
                mBicr.setChecked(false);
            }
        } else if (UsbManager.USB_FUNCTION_MASS_STORAGE.equals(function)) {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
            if (mUmsExist) {
                mUms.setChecked(true);
            }
            if (mChargeExist) {
                mCharge.setChecked(false);
            }
            if (mBicrExist) {
                mBicr.setChecked(false);
            }
        } else if (UsbManager.USB_FUNCTION_CHARGING_ONLY.equals(function)) {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
            if (mUmsExist) {
                mUms.setChecked(false);
            }
            if (mChargeExist) {
                mCharge.setChecked(true);
            }
            if (mBicrExist) {
                mBicr.setChecked(false);
            }
        } else if (UsbManager.USB_FUNCTION_BICR.equals(function)) {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
            if (mUmsExist) {
                mUms.setChecked(false);
            }
            if (mChargeExist) {
                mCharge.setChecked(false);
            }
            if (mBicrExist) {
                mBicr.setChecked(true);
            }
        } else {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
            if (mUmsExist) {
                mUms.setChecked(false);
            }
            if (mChargeExist) {
                mCharge.setChecked(false);
            }
            if (mBicrExist) {
                mBicr.setChecked(false);
            }
        }

        if (!mUsbAccessoryMode) {
            //Enable MTP and PTP switch while USB is not in Accessory Mode, otherwise disable it
            Log.e(TAG, "USB Normal Mode");
            mMtp.setEnabled(true);
            mPtp.setEnabled(true);
            if (mUmsExist) {
                mUms.setEnabled(true);
            }
            if (mChargeExist) {
                mCharge.setEnabled(true);
            }
            if (mBicrExist) {
                mBicr.setEnabled(true);
            }
        } else {
            Log.e(TAG, "USB Accessory Mode");
            mMtp.setEnabled(false);
            mPtp.setEnabled(false);
            if (mUmsExist) {
                mUms.setEnabled(false);
            }
            if (mChargeExist) {
                mCharge.setEnabled(false);
            }
            if (mBicrExist) {
                mBicr.setEnabled(false);
            }
        }
        /// M: update the mCurrentFunction
        mCurrentToggles = function;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Log.d(TAG, "onPreferenceChange");
        // Don't allow any changes to take effect as the USB host will be
        // disconnected, killing
        // the monkeys
        if (Utils.isMonkeyRunning()) {
            return true;
        }
        // temporary hack - using check boxes as radio buttons
        // don't allow unchecking them
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference checkBox = (CheckBoxPreference) preference;
            Log.d(TAG, "" + checkBox.getTitle() + checkBox.isChecked());
            if (!checkBox.isChecked()) {
                checkBox.setChecked(true);
                return true;
            }
        }
        if (preference == mMtp) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP, true);
            updateToggles(UsbManager.USB_FUNCTION_MTP);
        } else if (preference == mPtp) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP, true);
            updateToggles(UsbManager.USB_FUNCTION_PTP);
        } else if (preference == mUms) {
            mUsbManager.setCurrentFunction(
                    UsbManager.USB_FUNCTION_MASS_STORAGE, true);
            updateToggles(UsbManager.USB_FUNCTION_MASS_STORAGE);
        } else if (preference == mCharge) {
            mUsbManager.setCurrentFunction(
                    UsbManager.USB_FUNCTION_CHARGING_ONLY, true);
            updateToggles(UsbManager.USB_FUNCTION_CHARGING_ONLY);
        } else if (preference == mBicr) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_BICR, false);
            updateToggles(UsbManager.USB_FUNCTION_BICR);
        }
        /// M:set mCanUpdateToggle to false, and when it is
        /// M:false, do not update UI
        mCanUpdateToggle = false;
        return true;
    }
}
