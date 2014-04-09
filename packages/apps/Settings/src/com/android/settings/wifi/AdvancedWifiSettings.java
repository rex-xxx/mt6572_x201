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

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.xlog.Xlog;

public class AdvancedWifiSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "AdvancedWifiSettings";
    private static final String KEY_FREQUENCY_BAND = "frequency_band";
    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";
    private static final String KEY_SLEEP_POLICY = "sleep_policy";
    private static final String KEY_POOR_NETWORK_DETECTION = "wifi_poor_network_detection";
    private static final String KEY_SUSPEND_OPTIMIZATIONS = "suspend_optimizations";
    /// M: add for cmcc plug in: mac & ip address @{
    private Preference mMacAddressPref;
    private Preference mIpAddressPref;
    IWifiExt mExt;
    /// @}

    /// M: add for DHCPV6 @{
    private static final int NOT_FOUND_STRING = -1;
    private static final int ONLY_ONE_IP_ADDRESS = 1;
    private Preference mIpv6AddressPref;
    /// @}

    /// M: intent filter @{
    private IntentFilter mIntentFilter;
    private CheckBoxPreference mNotifyOpenNetworks;
    /// @}
    private WifiManager mWifiManager;

    /*
     *
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    mNotifyOpenNetworks.setEnabled(true);
                } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                    mNotifyOpenNetworks.setEnabled(false);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_advanced_settings);
        /// M: get plug in
        mExt = Utils.getWifiPlugin(getActivity());
        /// M: new intent filter
        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /// M: WifiManager memory leak @{
        //mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        /// @}

        /// M: init view @{
        mExt.initConnectView(getActivity(),getPreferenceScreen());
        mExt.initPreference(getContentResolver());
        addWifiInfoPreference();
        mExt.initNetworkInfoView(getPreferenceScreen());
        ///@}
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
        refreshWifiInfo();
        /// M: register receiver
        getActivity().registerReceiver(mReceiver, mIntentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        /// M: unregister receiver
        getActivity().unregisterReceiver(mReceiver);
    }

    private void initPreferences() {
        mNotifyOpenNetworks =
            (CheckBoxPreference) findPreference(KEY_NOTIFY_OPEN_NETWORKS);
        mNotifyOpenNetworks.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1);
        mNotifyOpenNetworks.setEnabled(mWifiManager.isWifiEnabled());

        CheckBoxPreference poorNetworkDetection =
            (CheckBoxPreference) findPreference(KEY_POOR_NETWORK_DETECTION);
        if (poorNetworkDetection != null) {
            /// M: disable poor Network Detection @{
                getPreferenceScreen().removePreference(poorNetworkDetection);
                Settings.Global.putInt(getContentResolver(), Settings.Global.WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED, 0);
            /// @}
        }

        CheckBoxPreference suspendOptimizations =
            (CheckBoxPreference) findPreference(KEY_SUSPEND_OPTIMIZATIONS);
        suspendOptimizations.setChecked(Global.getInt(getContentResolver(),
                Global.WIFI_SUSPEND_OPTIMIZATIONS_ENABLED, 1) == 1);

        ListPreference frequencyPref = (ListPreference) findPreference(KEY_FREQUENCY_BAND);

        if (mWifiManager.isDualBandSupported()) {
            frequencyPref.setOnPreferenceChangeListener(this);
            int value = mWifiManager.getFrequencyBand();
            if (value != -1) {
                frequencyPref.setValue(String.valueOf(value));
            } else {
                Xlog.e(TAG, "Failed to fetch frequency band");
            }
        } else {
            if (frequencyPref != null) {
                // null if it has already been removed before resume
                getPreferenceScreen().removePreference(frequencyPref);
            }
        }

        ListPreference sleepPolicyPref = (ListPreference) findPreference(KEY_SLEEP_POLICY);
        if (sleepPolicyPref != null) {
            if (Utils.isWifiOnly(getActivity())) {
                sleepPolicyPref.setEntries(R.array.wifi_sleep_policy_entries_wifi_only);
            }
            sleepPolicyPref.setOnPreferenceChangeListener(this);
            /// M: get sleep policy value @{
            int value = mExt.getSleepPolicy(getContentResolver());
            /// @}
            String stringValue = String.valueOf(value);
            sleepPolicyPref.setValue(stringValue);
            updateSleepPolicySummary(sleepPolicyPref, stringValue);
        }
    }

    private void updateSleepPolicySummary(Preference sleepPolicyPref, String value) {
        if (value != null) {
            String[] values = getResources().getStringArray(R.array.wifi_sleep_policy_values);
            final int summaryArrayResId = Utils.isWifiOnly(getActivity()) ?
                    R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries;
            String[] summaries = getResources().getStringArray(summaryArrayResId);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i])) {
                    if (i < summaries.length) {
                        sleepPolicyPref.setSummary(summaries[i]);
                        return;
                    }
                }
            }
        }

        sleepPolicyPref.setSummary("");
        Xlog.d(TAG, "Invalid sleep policy value: " + value);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();

        if (KEY_NOTIFY_OPEN_NETWORKS.equals(key)) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        } else if (KEY_POOR_NETWORK_DETECTION.equals(key)) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        } else if (KEY_SUSPEND_OPTIMIZATIONS.equals(key)) {
            Global.putInt(getContentResolver(),
                    Global.WIFI_SUSPEND_OPTIMIZATIONS_ENABLED,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_FREQUENCY_BAND.equals(key)) {
            try {
                mWifiManager.setFrequencyBand(Integer.parseInt((String) newValue), true);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_frequency_band_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (KEY_SLEEP_POLICY.equals(key)) {
            try {
                String stringValue = (String) newValue;
                Settings.Global.putInt(getContentResolver(), Settings.Global.WIFI_SLEEP_POLICY,
                        Integer.parseInt(stringValue));
                updateSleepPolicySummary(preference, stringValue);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_sleep_policy_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void refreshWifiInfo() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        /// M: refresh wifi mac & ip address preference @{
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        mMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ?
                macAddress : getActivity().getString(R.string.status_unavailable));

        String ipAddress = Utils.getWifiIpAddresses(getActivity());
        /** M @{
        mIpAddressPref.setSummary(ipAddress == null ?
                getActivity().getString(R.string.status_unavailable) : ipAddress);
        @} */
        Xlog.d(TAG, "refreshWifiInfo, the ipAddress is : " + ipAddress);
        if (FeatureOption.MTK_DHCPV6C_WIFI && ipAddress != null) {
            String[] ipAddresses = ipAddress.split(", ");
            int ipAddressesLength = ipAddresses.length;
            for (int i = 0; i < ipAddressesLength; i++) {
                if (ipAddresses[i].indexOf(":") == NOT_FOUND_STRING) {
                    mIpAddressPref.setSummary(ipAddresses[i] == null ?
                            getActivity().getString(R.string.status_unavailable) : ipAddresses[i]);
                    if (ipAddressesLength == ONLY_ONE_IP_ADDRESS) {
                        getPreferenceScreen().removePreference(mIpv6AddressPref);    
                    }
                } else  {
                    mIpv6AddressPref.setSummary(ipAddresses[i] == null ?
                            getActivity().getString(R.string.status_unavailable) : ipAddresses[i]);
                    if (ipAddressesLength == ONLY_ONE_IP_ADDRESS) {
                        getPreferenceScreen().removePreference(mIpAddressPref);    
                    }
                }
            }
            
        } else {
            mIpAddressPref.setSummary(ipAddress == null ?
                    getActivity().getString(R.string.status_unavailable) : ipAddress);
            if (FeatureOption.MTK_DHCPV6C_WIFI) {
                getPreferenceScreen().removePreference(mIpv6AddressPref);
            }
        }

        mExt.refreshNetworkInfoView();
        /// @}
    }
    /**
     * M: add wifi mac & ip address preference
     */
    private void addWifiInfoPreference() {
        mMacAddressPref = new Preference(getActivity());
        mMacAddressPref.setTitle(R.string.wifi_advanced_mac_address_title);
        getPreferenceScreen().addPreference(mMacAddressPref);

        mIpAddressPref = new Preference(getActivity());
        if (FeatureOption.MTK_DHCPV6C_WIFI) {
            mIpAddressPref.setTitle(R.string.wifi_advanced_ipv4_address_title);
            mIpv6AddressPref = new Preference(getActivity());
            mIpv6AddressPref.setTitle(R.string.wifi_advanced_ipv6_address_title);
            getPreferenceScreen().addPreference(mIpv6AddressPref);
        } else {
        mIpAddressPref.setTitle(R.string.wifi_advanced_ip_address_title);
        }
        getPreferenceScreen().addPreference(mIpAddressPref);
    }
}
