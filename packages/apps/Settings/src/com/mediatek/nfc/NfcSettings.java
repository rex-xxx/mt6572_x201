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

package com.mediatek.nfc;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.mediatek.xlog.Xlog;

public class NfcSettings extends SettingsPreferenceFragment {
    private static final String TAG = "NfcSettings";

    private static final String KEY_NFC_TAG_RW = "nfc_rw_tag";
    private static final String KEY_NFC_P2P_MODE = "nfc_p2p_mode";
    private static final String KEY_ANDROID_BEAM = "nfc_android_beam";

    private MtkNfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    private CheckBoxPreference mNfcRwTagPref;
    private CheckBoxPreference mNfcP2pModePref;
    private Preference mAndroidBeam;
    private IntentFilter mIntentFilter;

    /**
     * The broadcast receiver is used to handle the nfc adapter state changed
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                updatePreferenceEnabledStatus();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.nfc_settings);

        Activity activity = getActivity();

        Switch mActionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders()
                    || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources()
                        .getDimensionPixelSize(
                                R.dimen.action_bar_switch_padding);
                mActionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(
                        ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(
                        mActionBarSwitch,
                        new ActionBar.LayoutParams(
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
                activity.getActionBar().setTitle(
                        R.string.nfc_quick_toggle_title);
            }
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        mNfcEnabler = new MtkNfcEnabler(activity, null, mActionBarSwitch,
                mNfcAdapter);

        mIntentFilter = new IntentFilter(
                NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        initPreferences();

    }

    /**
     * According to the key find the corresponding preference
     */
    private void initPreferences() {
        mNfcP2pModePref = (CheckBoxPreference) findPreference(KEY_NFC_P2P_MODE);
        mAndroidBeam = findPreference(KEY_ANDROID_BEAM);
        mNfcRwTagPref = (CheckBoxPreference) findPreference(KEY_NFC_TAG_RW);
    }

    private void updatePreferenceEnabledStatus() {
        if (mNfcAdapter.getAdapterState() == NfcAdapter.STATE_ON) {
            mNfcP2pModePref.setEnabled(true);
            mNfcRwTagPref.setEnabled(true);
        } else {
            mNfcP2pModePref.setEnabled(false);
            mNfcRwTagPref.setEnabled(false);
        }
    }

    /**
     * update the preference according to the status of NfcAdapter settings
     */
    private void updatePreferences() {
        // update Android beam summary
       
         if (mNfcAdapter.isNdefPushEnabled()) {
             mAndroidBeam.setSummary(R.string.android_beam_on_summary); 
         } 
         else {
             mAndroidBeam.setSummary(R.string.android_beam_off_summary); 
         }
        
        // update p2p mode and RW tag preference enabled status
        updatePreferenceEnabledStatus();
        // update p2p mode preference checked status
        mNfcP2pModePref
                .setChecked(mNfcAdapter.getModeFlag(NfcAdapter.MODE_P2P) == NfcAdapter.FLAG_ON);
        // update RW tag preference checked status
        mNfcRwTagPref.setChecked(mNfcAdapter
                .getModeFlag(NfcAdapter.MODE_READER) == NfcAdapter.FLAG_ON);

    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference.equals(mAndroidBeam)) {
            startFragment(this, "com.android.settings.nfc.AndroidBeam", 0, null);
        } else if (preference.equals(mNfcP2pModePref)) {
            Xlog.d(TAG, "p2p mode");
            int flag = mNfcP2pModePref.isChecked() ? NfcAdapter.FLAG_ON
                    : NfcAdapter.FLAG_OFF;
            mNfcAdapter.setModeFlag(NfcAdapter.MODE_P2P, flag);
        } else if (preference.equals(mNfcRwTagPref)) {
            Xlog.d(TAG, "tag rw mode");
            int flag = mNfcRwTagPref.isChecked() ? NfcAdapter.FLAG_ON
                    : NfcAdapter.FLAG_OFF;
            mNfcAdapter.setModeFlag(NfcAdapter.MODE_READER, flag);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onResume() {
        super.onResume();
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        updatePreferences();
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
    }
}

class NfcDescriptionPreference extends Preference {
    public NfcDescriptionPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    public NfcDescriptionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }
}
