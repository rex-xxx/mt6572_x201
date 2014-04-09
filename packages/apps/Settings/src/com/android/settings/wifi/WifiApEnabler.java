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

package com.android.settings.wifi;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.TetherSettings;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.wifi.hotspot.HotspotSwitchPreference;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

public class WifiApEnabler extends Fragment
        implements CompoundButton.OnCheckedChangeListener,
                   Preference.OnPreferenceChangeListener {
    static final String TAG = "WifiApEnabler";
    private final Context mContext;
    private CharSequence mOriginalSummary;

    private WifiManager mWifiManager;
    private IntentFilter mIntentFilter;
    private TetherSettings mTetherSettings;
    private static final int WIFI_IPV4 = 0x0f;
    private static final int WIFI_IPV6 = 0xf0;

    ConnectivityManager mCm;
    private String[] mWifiRegexs;

    /// M: @{
    private static final String WIFI_SWITCH_SETTINGS = "wifi_tether_settings";
    private static final int INVALID             = -1;
    private static final int WIFI_TETHERING      = 0;
    IWifiExt mExt;
    private Switch mSwitch;
    private Preference mWpsConnectPre;
    private Preference mBandwidthPre;
    private boolean mStateMachineEvent;

    private HotspotSwitchPreference mSwitchPreference;
    private int mTetherChoice = INVALID;
    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;
    /// @}

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiApStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED));
            } else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED.equals(action)) {
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                if (available != null && active != null && errored != null) {
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        updateTetherStateForIpv6(available.toArray(), active.toArray(), errored.toArray());
                    } else {
                        updateTetherState(available.toArray(), active.toArray(), errored.toArray());
                    }
                }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                enableWifiCheckBox();
            }

        }
    };

    public WifiApEnabler(Context context, Switch switch_, Preference wps, Preference bandwith) {
        mContext = context;
        mSwitch = switch_;
        mWpsConnectPre = wps;
        mBandwidthPre = bandwith;
        init(context);
    }

    public WifiApEnabler(Context context, HotspotSwitchPreference preference) {
        mContext = context;
        mSwitchPreference = preference;
        mOriginalSummary = mSwitchPreference.getSummary();
        init(context);
    }

    public void init(Context context) {
        /// M: WifiManager memory leak @{
        //mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        /// @}
        mCm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = mCm.getTetherableWifiRegexs();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        mProvisionApp = mContext.getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);

        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(this);
        } else {
            mSwitchPreference.setOnPreferenceChangeListener(this);
        }

        enableWifiCheckBox();
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);

        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(null);
        } else {
            mSwitchPreference.setOnPreferenceChangeListener(null);
        }
    }

    private void enableWifiCheckBox() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (!isAirplaneMode) {
            setSwitchEnabled(true);
        } else {
            if (mSwitch == null) {
                mSwitchPreference.setSummary(mOriginalSummary);
            }
            setSwitchEnabled(false);
        }
    }

    public void setSoftapEnabled(boolean enable) {
        final ContentResolver cr = mContext.getContentResolver();
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = mWifiManager.getWifiState();
        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                    (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            mWifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
        }

        if (mWifiManager.setWifiApEnabled(null, enable)) {
            /* Disable here, enabled on receiving success broadcast */
            setSwitchEnabled(false);
        } else {
            if (mSwitch == null) {
                mSwitchPreference.setSummary(R.string.wifi_error);
            }
        }

        /**
         *  If needed, restore Wifi on tether disable
         */
        if (!enable) {
            int wifiSavedState = 0;
            try {
                wifiSavedState = Settings.Global.getInt(cr, Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                Xlog.d(TAG, "SettingNotFoundException");
            }
            if (wifiSavedState == 1) {
                mWifiManager.setWifiEnabled(true);
                Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 0);
            }
        }
    }

    public void updateConfigSummary(WifiConfiguration wifiConfig) {
        String s = com.mediatek.custom.CustomProperties.getString(com.mediatek.custom.CustomProperties.MODULE_WLAN, 
                    com.mediatek.custom.CustomProperties.SSID, 
                    mContext.getString(com.android.internal.R.string.wifi_tether_configure_ssid_default));
        if (mSwitch == null) {
            mSwitchPreference.setSummary(String.format(mContext.getString(R.string.wifi_tether_enabled_subtext),
                    (wifiConfig == null) ? s : wifiConfig.SSID));
        }
    }

    private void updateTetherStateForIpv6(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        int wifiErrorIpv4 = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        int wifiErrorIpv6 = ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR;
        for (Object o : available) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    if (wifiErrorIpv4 == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        wifiErrorIpv4 = (mCm.getLastTetherError(s) & WIFI_IPV4);
                    }
                    if (wifiErrorIpv6 == ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR) {
                        wifiErrorIpv6 = (mCm.getLastTetherError(s) & WIFI_IPV6);
                    }
                }
            }
        }

        for (Object o : tethered) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    wifiTethered = true;
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        if (wifiErrorIpv6 == ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR) {
                            wifiErrorIpv6 = (mCm.getLastTetherError(s) & WIFI_IPV6);
                        }
                    }
                }
            }
        }

        for (Object o: errored) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    wifiErrored = true;
                }
            }
        }

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
            String s = mContext.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
            String tetheringActive = String.format(
                mContext.getString(R.string.wifi_tether_enabled_subtext),
                (wifiConfig == null) ? s : wifiConfig.SSID);

            if (mTetherSettings != null && mSwitch == null) {
                mSwitchPreference.setSummary(tetheringActive + 
                    mTetherSettings.getIPV6String(wifiErrorIpv4, wifiErrorIpv6));
            }
        } else if (wifiErrored) {
            if (mSwitch == null) {
                mSwitchPreference.setSummary(R.string.wifi_error);
            }
        }
    }


    /**
     * set the TetherSettings.
     * @param TetherSettings
     * @return void.
     */
    public void setTetherSettings(TetherSettings tetherSettings) {
        mTetherSettings = tetherSettings;
    }

    private void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        for (Object o : tethered) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    wifiTethered = true;
                }
            }
        }
        for (Object o: errored) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    wifiErrored = true;
                }
            }
        }

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
        } else if (wifiErrored) {
            if (mSwitch == null) {
                mSwitchPreference.setSummary(R.string.wifi_error);
            }
        }
    }

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                setSwitchEnabled(false);
                setPreferenceState(false);
                setStartTime(false);
                if (mSwitch == null) {
                    mSwitchPreference.setSummary(R.string.wifi_tether_starting);
                }
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                /**
                 * Summary on enable is handled by tether
                 * broadcast notice
                 */
                setSwitchChecked(true);
                setSwitchEnabled(true);
                setPreferenceState(true);
                setStartTime(true);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                setSwitchEnabled(false);
                setPreferenceState(false);
                if (mSwitch == null) {
                    Xlog.d(TAG, "wifi_stopping");
                    mSwitchPreference.setSummary(R.string.wifi_tether_stopping);
                }
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                setSwitchChecked(false);
                setSwitchEnabled(true);
                setPreferenceState(false);
                if (mSwitch == null) {
                    mSwitchPreference.setSummary(mOriginalSummary);
                }
                enableWifiCheckBox();
                break;
            default:
                enableWifiCheckBox();
                break;
        }
    }
    private void setSwitchChecked(boolean checked) {
        mStateMachineEvent = true;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        } else {
            mSwitchPreference.setChecked(checked);
        }
        mStateMachineEvent = false;
    }
    private void setSwitchEnabled(boolean enabled) {
        if (mSwitch != null) {
            mSwitch.setEnabled(enabled);
        } else {
            mSwitchPreference.setEnabled(enabled);
        }
    }
    private void setPreferenceState(boolean enabled) {
        if (mSwitch != null) {
            mBandwidthPre.setEnabled(enabled);
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            if (wifiConfig != null && WifiApDialog.getSecurityTypeIndex(wifiConfig) == 1) {
                Xlog.d(TAG, "security is wpa psk, disable wps connect preference");
                enabled = false;
            }
            mWpsConnectPre.setEnabled(enabled);
        }
    }
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference.getKey().equals(WIFI_SWITCH_SETTINGS)) {
            boolean isChecked =  (Boolean) value;
            Xlog.d(TAG,"onPreferenceChange, isChecked:" + isChecked);
            if (isChecked) {
                startProvisioningIfNecessary(WIFI_TETHERING);
            } else {
                setSoftapEnabled(false);
            }
        }
        return true;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }
        Xlog.d(TAG,"onCheckedChanged, isChecked:" + isChecked);
        if (isChecked) {
            startProvisioningIfNecessary(WIFI_TETHERING);
        } else {
            setSoftapEnabled(false);
        }
    }
    boolean isProvisioningNeeded() {
        return mProvisionApp.length == 2;
    }
    private void startProvisioningIfNecessary(int choice) {
        mTetherChoice = choice;
        if (isProvisioningNeeded()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
            getActivity().startActivityForResult(intent, PROVISION_REQUEST);
            Xlog.d(TAG,"startProvisioningIfNecessary, startActivityForResult");
        } else {
            startTethering();
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PROVISION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                startTethering();
            } 
        }
    }
    private void startTethering() {
        if (mTetherChoice == WIFI_TETHERING) {
            Xlog.d(TAG,"startTethering, setSoftapEnabled");
            setSoftapEnabled(true);
        }
    }
    private void setStartTime(boolean enable) {
        long startTime = Settings.System.getLong(mContext.getContentResolver(),Settings.System.WIFI_HOTSPOT_START_TIME,
                            Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME);
        if (enable) {
            if (startTime == Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME) {
                Settings.System.putLong(mContext.getContentResolver(),Settings.System.WIFI_HOTSPOT_START_TIME,
                         System.currentTimeMillis());
                Xlog.d(TAG,"enable value: " + System.currentTimeMillis());
            }
        } else {
            long newValue = Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME;
            Xlog.d(TAG,"disable value: " + newValue);
            Settings.System.putLong(mContext.getContentResolver(),Settings.System.WIFI_HOTSPOT_START_TIME, newValue);
        }
    }

}
