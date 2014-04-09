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

package com.mediatek.wifi.hotspot;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.wifi.HotspotClient;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.mediatek.xlog.Xlog;

import java.util.List;

/*
 * Displays preferences for Tethering.
 */
public class TetherWifiSettings extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener,
        ButtonPreference.OnButtonClickCallback {
    private static final String TAG = "TetherWifiSettings";
    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String WIFI_AUTO_DISABLE = "wifi_auto_disable";
    private static final String WPS_CONNECT = "wps_connect";
    private static final String CONNECTED_CATEGORY = "connected_category";
    private static final String BLOCKED_CATEGORY = "blocked_category";
    private static final String BANDWIDTH = "bandwidth_usage";

    private static final int DIALOG_WPS_CONNECT = 1;
    private static final int DIALOG_AP_SETTINGS = 2;
    private static final int DIALOG_AP_CLIENT_DETAIL = 3;

    private static final int WIFI_AP_AUTO_CHANNEL_TEXT = R.string.wifi_tether_auto_channel_text;
    private static final int WIFI_AP_AUTO_CHANNEL_WIDTH_TEXT = R.string.wifi_tether_auto_channel_width_text;
    private static final int WIFI_AP_FIX_CHANNEL_WIDTH_TEXT = R.string.wifi_tether_fix_channel_width_text;
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private WifiApEnabler mWifiApEnabler;
    private ListPreference mWifiAutoDisable;
    private Preference mCreateNetwork;
    private Preference mWpsConnect;
    private Preference mBandwidth;

    private String[] mWifiRegexs;
    private String[] mSecurityType;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private IntentFilter mIntentFilter;

    private PreferenceCategory mConnectedCategory;
    private PreferenceCategory mBlockedCategory;

    private List<HotspotClient> mClientList;
    private View mDetailView;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
                Xlog.d(TAG,"receive action: " + action);
            if (WifiManager.WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION.equals(action)) {
                handleWifiApClientsChanged();
            } else if (WifiManager.WIFI_WPS_CHECK_PIN_FAIL_ACTION.equals(action)) {
                Toast.makeText(context, R.string.wifi_tether_wps_pin_error,Toast.LENGTH_LONG).show();
            } else if (WifiManager.WIFI_HOTSPOT_OVERLAP_ACTION.equals(action)) {
                Toast.makeText(context, R.string.wifi_wps_failed_overlap,Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.tether_wifi_prefs);

        final Activity activity = getActivity();

        mWifiAutoDisable = (ListPreference) findPreference(WIFI_AUTO_DISABLE);
        Preference wifiApSettings = findPreference(WIFI_AP_SSID_AND_SECURITY);
        mWpsConnect = findPreference(WPS_CONNECT);
        mWpsConnect.setEnabled(false);
        mBandwidth = findPreference(BANDWIDTH);
        mBandwidth.setEnabled(false);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        //mWifiRegexs = cm.getTetherableWifiRegexs();

        //final boolean wifiAvailable = mWifiRegexs.length != 0;

        Switch actionBarSwitch = new Switch(activity);
        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
        }

        mWifiApEnabler = new WifiApEnabler(activity, actionBarSwitch, mWpsConnect, mBandwidth);
        initWifiTethering();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_WPS_CHECK_PIN_FAIL_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_HOTSPOT_OVERLAP_ACTION);
        mConnectedCategory = (PreferenceCategory)findPreference(CONNECTED_CATEGORY);
        mBlockedCategory = (PreferenceCategory)findPreference(BLOCKED_CATEGORY);
        mDetailView = getActivity().getLayoutInflater().inflate(R.layout.wifi_ap_client_dialog, null);
    }

    private void initWifiTethering() {
        final Activity activity = getActivity();
        /// M: WifiManager memory leak , change context to getApplicationContext @{
        mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ///@}
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);

        if (mWifiConfig == null) {
            String s = com.mediatek.custom.CustomProperties.getString(com.mediatek.custom.CustomProperties.MODULE_WLAN, 
                        com.mediatek.custom.CustomProperties.SSID, 
                        activity.getString(com.android.internal.R.string.wifi_tether_configure_ssid_default));
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    s, mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            Xlog.d(TAG,"index = " + index);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    mWifiConfig.SSID,
                    mSecurityType[index]));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mWifiApEnabler != null) {
            mWifiApEnabler.resume();
        }
        if (mWifiAutoDisable != null) {
            mWifiAutoDisable.setOnPreferenceChangeListener(this);
            int value = System.getInt(getContentResolver(),System.WIFI_HOTSPOT_AUTO_DISABLE,
                                System.WIFI_HOTSPOT_AUTO_DISABLE_FOR_FIVE_MINS);
            mWifiAutoDisable.setValue(String.valueOf(value));
        }

        getActivity().registerReceiver(mReceiver, mIntentFilter);
        handleWifiApClientsChanged();
    }
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        if (mWifiApEnabler != null) {
            mWifiApEnabler.pause();
        }
        if (mWifiAutoDisable != null) {
            mWifiAutoDisable.setOnPreferenceChangeListener(null);
        }
    }
    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE 
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return mDialog;
        } else if (id == DIALOG_WPS_CONNECT) {
            Dialog d = new WifiApWpsDialog(getActivity());
            Xlog.d("mydialog","onCreateDialog, return dialog");
            return d;
        } else if (id == DIALOG_AP_CLIENT_DETAIL) {
            ViewParent parent = mDetailView.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mDetailView);
            }
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.wifi_ap_client_details_title)
            .setView(mDetailView)
            .setNegativeButton(com.android.internal.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            })
            .create();
            return dialog;
        }

        return null;
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        Xlog.d(TAG,"onPreferenceChange key=" + key);
        if (WIFI_AUTO_DISABLE.equals(key)) {
            System.putInt(getContentResolver(),
                    System.WIFI_HOTSPOT_AUTO_DISABLE, Integer.parseInt(((String) value)));
            Xlog.d(TAG,"onPreferenceChange auto disable value=" + Integer.parseInt(((String) value)));
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        } else if (preference == mWpsConnect) {
            showDialog(DIALOG_WPS_CONNECT);
        }
        if (preference instanceof ButtonPreference) {
            removeDialog(DIALOG_AP_CLIENT_DETAIL);
            final ButtonPreference client = (ButtonPreference) preference;

            ((TextView)mDetailView.findViewById(R.id.mac_address)).setText(client.getMacAddress());
            if (client.isBlocked()) {
                mDetailView.findViewById(R.id.ip_filed).setVisibility(View.GONE);
            } else {
                mDetailView.findViewById(R.id.ip_filed).setVisibility(View.VISIBLE);
                String ipAddr = mWifiManager.getClientIp(client.getMacAddress());
                Xlog.d(TAG,"connected client ip address is:" + ipAddr);
                ((TextView)mDetailView.findViewById(R.id.ip_address)).setText(ipAddr);
            }
            showDialog(DIALOG_AP_CLIENT_DETAIL);
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    private static String findIface(String[] ifaces, String[] regexes) {
        for (String iface : ifaces) {
            for (String regex : regexes) {
                if (iface.matches(regex)) {
                    return iface;
                }
            }
        }
        return null;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up
                 * else restart with new config
                 * TODO: update config on a running access point when framework support is added
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                } else {
                    mWifiManager.setWifiApConfiguration(mWifiConfig);
                }
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                if (index == 0) {
                    Toast.makeText(getActivity(), R.string.security_not_set,Toast.LENGTH_LONG).show();
                }
                mCreateNetwork.setSummary(String.format(getActivity().getString(CONFIG_SUBTEXT),
                        mWifiConfig.SSID,
                        mSecurityType[index]));
            }
        }
    }
    public void onClick(View v, HotspotClient client) {
        if (v.getId() == R.id.preference_button && client != null) {
            if (client.isBlocked) {
                Xlog.d(TAG,"onClick,client is blocked, unblock now");
                mWifiManager.unblockClient(client);
            } else {
                Xlog.d(TAG,"onClick,client isn't blocked, block now");
                mWifiManager.blockClient(client);
            }
            handleWifiApClientsChanged();
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDialog != null) {
            mDialog.closeSpinnerDialog();
        }
    }
    private void handleWifiApClientsChanged() {
        mConnectedCategory.removeAll();
        mBlockedCategory.removeAll();
        mClientList = mWifiManager.getHotspotClients();
        if (mClientList != null) {
            Xlog.d(TAG,"client number is " + mClientList.size());
            for (HotspotClient client : mClientList) {
                ButtonPreference preference = new ButtonPreference(getActivity(), client , this);
                preference.setMacAddress(client.deviceAddress);
                if (client.isBlocked) {
                    preference.setButtonText(getResources().getString(R.string.wifi_ap_client_unblock_title));
                    mBlockedCategory.addPreference(preference);
                    Xlog.d(TAG,"blocked client MAC is " + client.deviceAddress);
                } else {
                    preference.setButtonText(getResources().getString(R.string.wifi_ap_client_block_title));
                    mConnectedCategory.addPreference(preference);
                    Xlog.d(TAG,"connected client MAC is " + client.deviceAddress);
                }
            }
            if (mConnectedCategory.getPreferenceCount() == 0) {
                Preference preference = new Preference(getActivity());
                preference.setTitle(R.string.wifi_ap_no_connected);
                mConnectedCategory.addPreference(preference);
            }
            if (mBlockedCategory.getPreferenceCount() == 0) {
                Preference preference = new Preference(getActivity());
                preference.setTitle(R.string.wifi_ap_no_blocked);
                mBlockedCategory.addPreference(preference);
            }
        }
    }
}


