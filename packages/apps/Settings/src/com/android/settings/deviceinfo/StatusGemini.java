/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.gemini.SimListEntrance;
import com.mediatek.settings.ext.IStatusGeminiExt;
import com.mediatek.xlog.Xlog;

import java.lang.ref.WeakReference;
import java.util.List;

public class StatusGemini extends PreferenceActivity {

    private static final String KEY_WIFI_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SIM_STATUS = "sim_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_UP_TIME = "up_time";
    private static final String KEY_SLOT_STATUS = "slot_status";

    private static final String CDMA = "CDMA";
    private static final String WIMAX_ADDRESS = "net.wimax.mac.address";

    private Resources mRes;
    private static String sUnknown;

    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    private Preference mUptime;

    private static final int EVENT_UPDATE_STATS = 500;

    private Handler mHandler;

    private GeminiPhone mGeminiPhone = null;

    private static final String TAG = "Gemini_Aboutphone";
    IStatusGeminiExt mExt;

    private static class MyHandler extends Handler {

        private WeakReference<StatusGemini> mStatus;

        public MyHandler(StatusGemini activity) {
            mStatus = new WeakReference<StatusGemini>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            StatusGemini status = mStatus.get();
            if (status == null) {
                return;
            }
            if (EVENT_UPDATE_STATS == msg.what) {
                status.updateTimes();
                sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {

                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);

                mBatteryLevel.setSummary(getString(R.string.battery_level,
                        level * 100 / scale));

                int plugType = intent.getIntExtra("plugged", 0);
                int status = intent.getIntExtra("status",
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                String statusString;
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    statusString = getString(R.string.battery_info_status_charging);
                    if (plugType > 0) {
                        statusString = statusString
                                + " "
                                + getString((plugType == BatteryManager.BATTERY_PLUGGED_AC) ? 
                                          R.string.battery_info_status_charging_ac
                                        : R.string.battery_info_status_charging_usb);
                    }
                } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    statusString = getString(R.string.battery_info_status_discharging);
                } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    statusString = getString(R.string.battery_info_status_not_charging);
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    statusString = getString(R.string.battery_info_status_full);
                } else {
                    statusString = getString(R.string.battery_info_status_unknown);
                }
                mBatteryStatus.setSummary(statusString);
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.device_info_status_gemini);
        Xlog.d(TAG, "Enter StatusGemini onCreate function.");
        mExt = Utils.getStatusGeminiExtPlugin(this);
        mHandler = new MyHandler(this);
        mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();

        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);
        mUptime = findPreference(KEY_UP_TIME);

        // setSimListEntrance();

        mRes = getResources();
        sUnknown = mRes.getString(R.string.device_info_default);

        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean sIsWifiOnly = false;
        if (cm != null) {
            sIsWifiOnly = (!cm
                    .isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
            Xlog.d(TAG, "sIsWifiOnly=" + sIsWifiOnly);
        }

        if (!sIsWifiOnly) {
            setSimListEntrance();

            String serial = Build.SERIAL;
            if (serial != null && !serial.equals("")) {
                setSummaryText(KEY_SERIAL_NUMBER, serial);
            } else {
                Preference pref = findPreference(KEY_SERIAL_NUMBER);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        } else {
            Preference simStatus = findPreference(KEY_SIM_STATUS);
            getPreferenceScreen().removePreference(simStatus);

            Preference slotStatus = findPreference(KEY_SLOT_STATUS);
            getPreferenceScreen().removePreference(slotStatus);

        }

        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    private void setSimListEntrance() {
        Preference simStatus = findPreference(KEY_SIM_STATUS);
        // M: plug in method
        mExt.initUI(getPreferenceScreen(), simStatus);
        if (simStatus != null) {
            List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
            int mSimNum = simList.size();
            Xlog.d(TAG, "sim num " + mSimNum);
            if (mSimNum == 0) {
                simStatus.setEnabled(false);
            } else if (mSimNum == 1) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        "com.android.settings.deviceinfo.SimStatusGemini");
                intent.putExtra("slotid", simList.get(0).mSlot);
                simStatus.setIntent(intent);

            } else if (mSimNum > 1) {
                Intent intent = new Intent(
                        "com.android.settings.SIM_LIST_ENTRANCE_ACTIVITY");
                intent.putExtra("title", simStatus.getTitle());
                intent.putExtra("type", SimListEntrance.SIM_STATUS_INDEX);
                simStatus.setIntent(intent);
            }

        }
    }

    private void setSummaryText(String preference, String text) {
        Xlog.d(TAG, "set " + preference + " with text=" + text);
        if (TextUtils.isEmpty(text)) {
            text = this.getResources().getString(R.string.device_info_default);
        }
        PreferenceScreen parent = (PreferenceScreen) findPreference(KEY_SLOT_STATUS);
        // some preferences may be missing
        Preference p = parent.findPreference(preference);
        if (p == null) {
            Xlog.d(TAG, KEY_SLOT_STATUS + " not find preference " + preference);
            p = this.findPreference(preference);
            if (p != null) {

                p.setSummary(text);
            }
        } else {
            p.setSummary(text);
        }
    }

    private void setWimaxStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = null;
        if (cm != null) {
            ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        }

        if (ni == null) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_WIMAX_MAC_ADDRESS);
            if (ps != null) {
                root.removePreference(ps);
            }
        } else {
            Preference wimaxMacAddressPref = findPreference(KEY_WIMAX_MAC_ADDRESS);
            String macAddress = SystemProperties.get(WIMAX_ADDRESS,
                    getString(R.string.status_unavailable));
            wimaxMacAddressPref.setSummary(macAddress);
        }
    }

    private void setWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = null;
        if (wifiManager != null) {
            wifiInfo = wifiManager.getConnectionInfo();
        }

        Preference wifiMacAddressPref = findPreference(KEY_WIFI_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        if (wifiMacAddressPref != null) {
            wifiMacAddressPref
                    .setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                            : getString(R.string.status_unavailable));
        }

        Preference wifiIpAddressPref = findPreference(KEY_WIFI_IP_ADDRESS);
        String ipAddress = Utils.getDefaultIpAddresses(this);
        if (ipAddress != null) {
            wifiIpAddressPref.setSummary(ipAddress);
        } else {
            wifiIpAddressPref
                    .setSummary(getString(R.string.status_unavailable));
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        Preference btAddressPref = findPreference(KEY_BT_ADDRESS);

        if (bluetooth == null) {
            // device not BT capable
            if (btAddressPref != null) {
                getPreferenceScreen().removePreference(btAddressPref);
            }
        } else {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress()
                    : null;
            if (btAddressPref != null) {
                btAddressPref.setSummary(!TextUtils.isEmpty(address) ? address
                        : getString(R.string.status_unavailable));
            }
        }
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int) (t % 60);
        int m = (int) ((t / 60) % 60);
        int h = (int) ((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }
}
