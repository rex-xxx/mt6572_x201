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

package com.mediatek.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.preference.Preference;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mediatek.xlog.Xlog;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class MtkNfcEnabler implements Preference.OnPreferenceChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private final Context mContext;
    private final NfcPreference mSwitchPreference;
    private final Switch mSwitchButton;
    private final NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;
    private boolean mUpdateSwitchPrefOnly;
    private boolean mUpdateSwitchButtonOnly;

    private static final String TAG = "MtkNfcEnabler";

    /**
     * The broadcast receiver is used to handle the nfc adapter state changed
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(
                        NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF));
            }
        }
    };

    public MtkNfcEnabler(Context context, NfcPreference switchpref,
            Switch switchButton, NfcAdapter adapter) {
        mContext = context;
        mSwitchPreference = switchpref;
        mSwitchButton = switchButton;
        mNfcAdapter = adapter;

        /*if (mNfcAdapter == null) {
            // NFC is not supported
            Xlog.d(TAG, "Nfc Adapter is null");
            if (mSwitchPreference != null) {
                mSwitchPreference.setEnabled(false);
            }
            mIntentFilter = null;
            return;
        }*/
        mIntentFilter = new IntentFilter(
                NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
    }

    /**
     * called in Fragment or Activity.onResume(), used to update button or register listener
     */
    public void resume() {
        Xlog.d(TAG, "Resume");
        if (mNfcAdapter == null) {
            return;
        }
        handleNfcStateChanged(mNfcAdapter.getAdapterState());
        mContext.registerReceiver(mReceiver, mIntentFilter);
        if (mSwitchPreference != null) {
            mSwitchPreference.setOnPreferenceChangeListener(this);
        }
        if (mSwitchButton != null) {
            mSwitchButton.setOnCheckedChangeListener(this);
        }
    }

    /**
     * called in Fragment or Activity.onPause()
     */
    public void pause() {
        Xlog.d(TAG, "Pause");
        if (mNfcAdapter == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        if (mSwitchPreference != null) {
            mSwitchPreference.setOnPreferenceChangeListener(null);
        }
        if (mSwitchButton != null) {
            mSwitchButton.setOnCheckedChangeListener(null);
        }
    }

    /**
     * set the switch button check status, before set checked, set a flag to true
     * and in onCheckChanged() according to the flag to decide just refresh UI or 
     * call framework to enable/disable NFC
     * @param checked the checked status
     */
    private void setSwitchButtonChecked(boolean checked) {
        if (checked != mSwitchButton.isChecked()) {
            mUpdateSwitchButtonOnly = true;
            mSwitchButton.setChecked(checked);
            mUpdateSwitchButtonOnly = false;
        }
    }

    /**
     * set the switch preference check status, before set checked, set a flag to true
     * and in onPreferenceChange() according to the flag to decide just refresh UI or 
     * call framework to enable/disable NFC
     * @param checked the checked status
     */
    private void setSwitchPrefChecked(boolean checked) {
        if (checked != mSwitchPreference.isChecked()) {
            mUpdateSwitchPrefOnly = true;
            mSwitchPreference.setChecked(checked);
            mUpdateSwitchPrefOnly = false;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        final boolean desiredState = (Boolean) value;
        Xlog.d(TAG, "onPreferenceChange " + desiredState);
        if (mSwitchPreference == null) {
            return false;
        }

        if (mUpdateSwitchPrefOnly) {
            return true;
        }
        mSwitchPreference.setEnabled(false);

        // Turn NFC on/off

        if (desiredState) {
            mNfcAdapter.setModeFlag(NfcAdapter.MODE_CARD, NfcAdapter.FLAG_ON);
            mNfcAdapter.enable();
        } else {
            mNfcAdapter.setModeFlag(NfcAdapter.MODE_CARD, NfcAdapter.FLAG_OFF);
            mNfcAdapter.disable();
        }
        return true;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Xlog.d(TAG, "onCheckedChanged " + isChecked);
        if (mSwitchButton != null && !mUpdateSwitchButtonOnly) {
            mSwitchButton.setEnabled(false);
            if (isChecked) {
                mNfcAdapter.setModeFlag(NfcAdapter.MODE_CARD, NfcAdapter.FLAG_ON);
                mNfcAdapter.enable();
            } else {
                mNfcAdapter.setModeFlag(NfcAdapter.MODE_CARD, NfcAdapter.FLAG_OFF);
                mNfcAdapter.disable();
            }
        }
    }

    /**
     * called when resume or receive the nfc adapter status changed
     * @param newState the current nfc adapter state
     */
    private void handleNfcStateChanged(int newState) {
        updateSwitch(newState);
        updateSwitchPref(newState);
    }

    /**
     * update the switchbutton according to the NFC state
     * @param state the current nfc adapter state
     */
    private void updateSwitch(int state) {
        if (mSwitchButton == null) {
            return;
        }
        switch (state) {
        case NfcAdapter.STATE_OFF:
            setSwitchButtonChecked(false);
            mSwitchButton.setEnabled(true);
            break;
        case NfcAdapter.STATE_ON:
            setSwitchButtonChecked(true);
            mSwitchButton.setEnabled(true);
            break;
        case NfcAdapter.STATE_TURNING_ON:
            setSwitchButtonChecked(true);
            mSwitchButton.setEnabled(false);
            break;
        case NfcAdapter.STATE_TURNING_OFF:
            setSwitchButtonChecked(false);
            mSwitchButton.setEnabled(false);
            break;
        default:
            setSwitchButtonChecked(false);
            break;
        }
    }

    /**
     * update the NfcPrefrence according to NFC state
     * @param state the current nfc adapter state
     */
    private void updateSwitchPref(int state) {
        if (mSwitchPreference == null) {
            return;
        }
        switch (state) {
        case NfcAdapter.STATE_OFF:
            setSwitchPrefChecked(false);
            mSwitchPreference.setEnabled(true);
            break;
        case NfcAdapter.STATE_ON:
            setSwitchPrefChecked(true);
            mSwitchPreference.setEnabled(true);
            break;
        case NfcAdapter.STATE_TURNING_ON:
            setSwitchPrefChecked(true);
            mSwitchPreference.setEnabled(false);
            break;
        case NfcAdapter.STATE_TURNING_OFF:
            setSwitchPrefChecked(false);
            mSwitchPreference.setEnabled(false);
            break;
        default:
            setSwitchPrefChecked(false);
            break;
        }
    }
}
