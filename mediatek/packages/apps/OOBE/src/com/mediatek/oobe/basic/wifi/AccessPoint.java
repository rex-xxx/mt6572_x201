/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

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

package com.mediatek.oobe.basic.wifi;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.view.View;
import android.widget.ImageView;

import com.mediatek.oobe.ext.IWifiExt;
import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.xlog.Xlog;

class AccessPoint extends Preference {
    static final String TAG = "OOBE.AccessPoint";

    private static final String KEY_DETAILEDSTATE = "key_detailedstate";
    private static final String KEY_WIFIINFO = "key_wifiinfo";
    private static final String KEY_SCANRESULT = "key_scanresult";
    private static final String KEY_CONFIG = "key_config";

    private static final int[] STATE_SECURED = { R.attr.state_encrypted };
    private static final int[] STATE_NONE = {};

    /// M: OPEN AP & WFA test support @{
    private static final String KEY_PROP_WFA_TEST_SUPPORT = "persist.radio.wifi.wpa2wpaalone";
    private static final String KEY_PROP_WFA_TEST_VALUE = "true";
    private static String sWFATestFlag = null;
    /// @}

    /** These values are matched in string arrays -- changes must be kept in sync */
    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    /// M: security type @{
    static final int SECURITY_WPA_PSK = 3;
    static final int SECURITY_WPA2_PSK = 4;
    static final int SECURITY_EAP = 5;
    static final int SECURITY_WAPI_PSK = 6;
    static final int SECURITY_WAPI_CERT = 7;
    /// @}

    enum PskType {
        UNKNOWN, WPA, WPA2, WPA_WPA2
    }

    String mSsid;
    String mBssid;
    int mSecurity;
    int mNetworkId;
    boolean mWpsAvailable = false;

    PskType mPskType = PskType.UNKNOWN;

    private WifiConfiguration mConfig;
    /* package */ScanResult mScanResult;

    private int mRssi;
    private WifiInfo mInfo;
    private DetailedState mState;
    /// M: plug in
    static IWifiExt sExt = null;

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        /// M: support wapi psk/cert @{
        if (config.allowedKeyManagement.get(KeyMgmt.WAPI_PSK)) {
            return SECURITY_WAPI_PSK;
        }

        if (config.allowedKeyManagement.get(KeyMgmt.WAPI_CERT)) {
            return SECURITY_WAPI_CERT;
        }
        
        if (config.wepTxKeyIndex >= 0 && config.wepTxKeyIndex < config.wepKeys.length 
                && config.wepKeys[config.wepTxKeyIndex] != null) {
            return SECURITY_WEP;
        }
        ///@}
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WAPI-PSK")) {
            /// M:  WAPI_PSK
            return SECURITY_WAPI_PSK;
        } else if (result.capabilities.contains("WAPI-CERT")) {
            /// M: WAPI_CERT
            return SECURITY_WAPI_CERT;
        } else if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public String getSecurityString(boolean concise) { //MTK_CS_IGNORE_THIS_LINE
        Context context = getContext();
        switch (mSecurity) {
        case SECURITY_EAP:
            return concise ? context.getString(R.string.wifi_security_short_eap) : context
                    .getString(R.string.wifi_security_eap);
        case SECURITY_PSK:
            switch (mPskType) {
            case WPA:
                return concise ? context.getString(R.string.wifi_security_short_wpa) : context
                        .getString(R.string.wifi_security_wpa);
            case WPA2:
                return concise ? context.getString(R.string.wifi_security_short_wpa2) : context
                        .getString(R.string.wifi_security_wpa2);
            case WPA_WPA2:
                return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) : context
                        .getString(R.string.wifi_security_wpa_wpa2);
            case UNKNOWN:
            default:
                return concise ? context.getString(R.string.wifi_security_short_psk_generic) : context
                        .getString(R.string.wifi_security_psk_generic);
            }
        case SECURITY_WEP:
            return concise ? context.getString(R.string.wifi_security_short_wep) : context
                    .getString(R.string.wifi_security_wep);
            case SECURITY_WAPI_PSK:
                /// M:return WAPI_PSK string
                return context.getString(R.string.wifi_security_wapi_psk);
            case SECURITY_WAPI_CERT:
                /// M: return WAPI_CERT string
                return context.getString(R.string.wifi_security_wapi_certificate);
        case SECURITY_NONE:
        default:
            return concise ? "" : context.getString(R.string.wifi_security_none);
        }
    }

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            Xlog.w(TAG, "Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }

    AccessPoint(Context context, WifiConfiguration config) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        loadConfig(config);
        refresh();
        /// M: get plug in @{
        if (sExt == null) {
            sExt = Utils.getWifiPlugin(getContext());
        }
        /// @}
    }

    AccessPoint(Context context, ScanResult result) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        loadResult(result);
        refresh();
        /// M: get plug in @{
        if (sExt == null) {
            sExt = Utils.getWifiPlugin(getContext());
        }
        /// @}
    }

    AccessPoint(Context context, Bundle savedState) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);

        mConfig = savedState.getParcelable(KEY_CONFIG);
        if (mConfig != null) {
            loadConfig(mConfig);
        }
        mScanResult = (ScanResult) savedState.getParcelable(KEY_SCANRESULT);
        if (mScanResult != null) {
            loadResult(mScanResult);
        }
        mInfo = (WifiInfo) savedState.getParcelable(KEY_WIFIINFO);
        if (savedState.containsKey(KEY_DETAILEDSTATE)) {
            mState = DetailedState.valueOf(savedState.getString(KEY_DETAILEDSTATE));
        }
        update(mInfo, mState);
        /// M: get plug in @{
        if (sExt == null) {
            sExt = Utils.getWifiPlugin(getContext());
        }
        /// @}
    }

    public void saveWifiState(Bundle savedState) {
        savedState.putParcelable(KEY_CONFIG, mConfig);
        savedState.putParcelable(KEY_SCANRESULT, mScanResult);
        savedState.putParcelable(KEY_WIFIINFO, mInfo);
        if (mState != null) {
            savedState.putString(KEY_DETAILEDSTATE, mState.toString());
        }
    }

    private void loadConfig(WifiConfiguration config) {
        mSsid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
        mBssid = config.BSSID;
        mSecurity = getSecurity(config);
        mNetworkId = config.networkId;
        mRssi = Integer.MAX_VALUE;
        mConfig = config;
    }

    private void loadResult(ScanResult result) {
        mSsid = result.SSID;
        mBssid = result.BSSID;
        mSecurity = getSecurity(result);
        mWpsAvailable = mSecurity != SECURITY_EAP && result.capabilities.contains("WPS");
        if (mSecurity == SECURITY_PSK) {
            mPskType = getPskType(result);
        }
        mNetworkId = -1;
        mRssi = result.level;
        mScanResult = result;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ImageView signal = (ImageView) view.findViewById(R.id.signal);
        if (mRssi == Integer.MAX_VALUE) {
            signal.setImageDrawable(null);
        } else {
            signal.setImageLevel(getLevel());
            signal.setImageResource(R.drawable.wifi_signal);
            signal.setImageState((mSecurity != SECURITY_NONE) ? STATE_SECURED : STATE_NONE, true);
        }
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof AccessPoint)) {
            return 1;
        }
        AccessPoint other = (AccessPoint) preference;
        // Active one goes first.
        if (mInfo != other.mInfo) {
            return (mInfo != null) ? -1 : 1;
        }

        /// M: cmcc ap goes first @{
        int order = sExt.getApOrder(this.mSsid,this.mSecurity,other.mSsid,other.mSecurity);
        if (order != 0) {
            return order;
        }
        /// @}

        // Reachable one goes before unreachable one.
        if ((mRssi ^ other.mRssi) < 0) {
            return (mRssi != Integer.MAX_VALUE) ? -1 : 1;
        }
        // Configured one goes before unconfigured one.
        if ((mNetworkId ^ other.mNetworkId) < 0) {
            return (mNetworkId != -1) ? -1 : 1;
        }
        // Sort by signal strength.
        int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
        if (difference != 0) {
            return difference;
        }
        // Sort by mSsid.
        return mSsid.compareToIgnoreCase(other.mSsid);
    }

    boolean update(ScanResult result) {
        if (mSsid.equals(result.SSID) && mSecurity == getSecurity(result)) {
            if (WifiManager.compareSignalLevel(result.level, mRssi) > 0) {
                int oldLevel = getLevel();
                mRssi = result.level;
                if (getLevel() != oldLevel) {
                    notifyChanged();
                }
            }
            // This flag only comes from scans, is not easily saved in config
            if (mSecurity == SECURITY_PSK) {
                mPskType = getPskType(result);
            }
            refresh();
            return true;
        }
        return false;
    }

    void update(WifiInfo info, DetailedState state) {
        boolean reorder = false;
        if (info != null && mNetworkId != WifiConfiguration.INVALID_NETWORK_ID && mNetworkId == info.getNetworkId()) {
            reorder = (mInfo == null);
            mRssi = info.getRssi();
            mInfo = info;
            mState = state;
            refresh();
        } else if (mInfo != null) {
            reorder = true;
            mInfo = null;
            mState = null;
            refresh();
        }
        if (reorder) {
            notifyHierarchyChanged();
        }
    }

    int getLevel() {
        if (mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, 4); //MTK_CS_IGNORE_THIS_LINE
    }

    WifiConfiguration getConfig() {
        return mConfig;
    }

    WifiInfo getInfo() {
        return mInfo;
    }

    DetailedState getState() {
        return mState;
    }

    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    /** Updates the title and summary; may indirectly call notifyChanged() */
    private void refresh() {
        setTitle(mSsid);
        Context context = getContext();
        if (mState != null) { // This is the active connection
            setSummary(Summary.get(context, mState));
        } else if (mRssi == Integer.MAX_VALUE) { // Wifi out of range
            setSummary(context.getString(R.string.wifi_not_in_range));
        } else if (mConfig != null && mConfig.status == WifiConfiguration.Status.DISABLED) {
            switch (mConfig.disableReason) {
            case WifiConfiguration.DISABLED_AUTH_FAILURE:
                setSummary(context.getString(R.string.wifi_disabled_password_failure));
                break;
            case WifiConfiguration.DISABLED_DHCP_FAILURE:
            case WifiConfiguration.DISABLED_DNS_FAILURE:
                setSummary(context.getString(R.string.wifi_disabled_network_failure));
                break;
            case WifiConfiguration.DISABLED_UNKNOWN_REASON:
                setSummary(context.getString(R.string.wifi_disabled_generic));
                break;
            default:
                break;
            }
        } else { // In range, not disabled.
            StringBuilder summary = new StringBuilder();
            if (mConfig != null) { // Is saved network
                summary.append(context.getString(R.string.wifi_remembered));
            }

            if (mSecurity != SECURITY_NONE) {
                String securityStrFormat;
                if (summary.length() == 0) {
                    securityStrFormat = context.getString(R.string.wifi_secured_first_item);
                } else {
                    securityStrFormat = context.getString(R.string.wifi_secured_second_item);
                }
                summary.append(String.format(securityStrFormat, getSecurityString(true)));
            }

            if (mConfig == null && mWpsAvailable) { // Only list WPS available for unsaved networks
                if (summary.length() == 0) {
                    summary.append(context.getString(R.string.wifi_wps_available_first_item));
                } else {
                    summary.append(context.getString(R.string.wifi_wps_available_second_item));
                }
            }
            setSummary(summary.toString());
        }
    }

    /**
     * Generate and save a default wifiConfiguration with common values. Can only be called for unsecured networks.
     * 
     * @hide
     */
    protected void generateOpenNetworkConfig() {
        if (mSecurity != SECURITY_NONE) {
            throw new IllegalStateException();
        }
        if (mConfig != null) {
            return;
        }
        mConfig = new WifiConfiguration();
        mConfig.SSID = AccessPoint.convertToQuotedString(mSsid);
        /// M: set bssid to configuration
        mConfig.BSSID = mBssid;
        mConfig.allowedKeyManagement.set(KeyMgmt.NONE);
    }
    /**
     * M: support WFA test
     */
    public static boolean isWFATestSupported() {
        if (sWFATestFlag == null) {
            sWFATestFlag = SystemProperties.get(KEY_PROP_WFA_TEST_SUPPORT, "");
            Xlog.d(TAG, "isWFATestSupported(), sWFATestFlag=" + sWFATestFlag);
        }
        return KEY_PROP_WFA_TEST_VALUE.equals(sWFATestFlag);
    }
    /**
     * M: reset WFA Flag
     */
    public static void resetWFAFlag() {
        sWFATestFlag = null;
    }
}
