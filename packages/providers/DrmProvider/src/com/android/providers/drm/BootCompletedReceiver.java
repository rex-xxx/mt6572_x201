/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.providers.drm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

// read & save IMEI number of this device. for gemini, the IMEI of first SIM is retrieved.
// the IMEI is used as device id; other way of getting device id can be used.
// Note that the device id length is limited to 32 ASCII characters.
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "DRM/BootCompletedReceiver";
    private static final String INVALID_DEVICE_ID = "000000000000000";
    private static final String OLD_DEVICE_ID_FILE = "/data/data/com.android.providers.drm/files/id/id.dat";
    private static final int DEVICE_ID_LEN = 32;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive : BOOT_COMPLETED received.");
        if (FeatureOption.MTK_DRM_APP) {
            OmaDrmClient client = new OmaDrmClient(context);

            // first we get device id
            String id = OmaDrmHelper.loadDeviceId(client);
            Log.d(TAG, "BootCompletedReceiver : load device id: " + id);

            // get an empty device id: the device id was not saved yet
            if (id == null || id.isEmpty() || id.equals(INVALID_DEVICE_ID)) {
                Log.d(TAG, "BootCompletedReceiver : The device id is empty, try obtain it");
                id = deviceId(context);
                Log.d(TAG, "BootCompletedReceiver : Obtained device id: " + id);

                if (id.equals(INVALID_DEVICE_ID)) {
                    Log.w(TAG, "BootCompletedReceiver : Obtained device id is an invalid value");
                    return; // we do not save invalid device id, and do nothing else
                }

                // for a valid id we save it
                Log.d(TAG, "BootCompletedReceiver : save device id.");
                int res = OmaDrmHelper.saveDeviceId(client, id);
            }

            // not an empty device id, or it's already saved: other operations for secure timer.
            Log.d(TAG, "BootCompletedReceiver : load secure timer and update time base.");
            int result = OmaDrmHelper.loadClock(client);
            result = OmaDrmHelper.updateTimeBase(client);
        }
    }

    // you may modify this implementation to change the way you retrieve device id.
    // Note that the device id length is limited to 32 ASCII characters.
    // by default it returns an "invalid value" for device id if non of the method
    // can retrieve an valid one
    public static String deviceId(Context context) {
        // if invalid, 15 '0' digits are returned.
        String id = INVALID_DEVICE_ID;

        // for most of the cases, we use IMEI for device id. by default it's 15 digits
        // for MEID case, it is 14 digits/characters
        Log.v(TAG, "deviceId: try to get IMEI as device id");
        TelephonyManager tm =
            (TelephonyManager)(context.getSystemService(Context.TELEPHONY_SERVICE));
        if (null != tm) {
            String imei = tm.getDeviceId();

            // failed to get imei(null) or imei was not saved before
            if (imei == null || imei.isEmpty()) {
                Log.w(TAG, "deviceId: Invalid imei: " + imei);
            } else {
                id = imei;
            }
        } else {
            Log.w(TAG, "deviceId: Invalid TelephonyManager.");
        }

        // if we failed to get IMEI at boot-up time, for example, the boot-up timing
        //   issue after MOTA upgrade, we try /data/data/com.android.providers.drm/files/id/id.dat
        //   (the storage position for ICS ver.)
        if (id.equals(INVALID_DEVICE_ID)) {
            Log.v(TAG, "deviceId: try to check for old device id file " + OLD_DEVICE_ID_FILE);
            File f = new File(OLD_DEVICE_ID_FILE);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "deviceId: the old device id file is not found.");
                fis = null;
            }

            if (null != fis) {
                byte[] data = new byte[DEVICE_ID_LEN];
                for (byte element : data)
                    element = 0;

                try {
                    int result = fis.read(data);
                    // find the last byte which does not equals 0
                    int length = 0;
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] == 0) {
                            length = i;
                            break;
                        }
                    }
                    byte[] array = new byte[length];
                    for (int j = 0; j < array.length; j++) {
                        array[j] = data[j];
                    }

                    id = new String(array, Charset.forName("US-ASCII"));
                    fis.close();
                } catch (IOException e) {
                    Log.w(TAG, "deviceId: I/O error when reading old devicd id file.");
                }
            }
        }

        // now, in case there's no IMEI avaiable on device (some are wifi-only),
        // we may use wifi MAC address for an alternative method.
        // however we know that if wifi is closed, we can't get valid MAC value
        if (id.equals(INVALID_DEVICE_ID)) {
            Log.v(TAG, "deviceId: try to use mac address for device id.");
            WifiManager wm =
                (WifiManager)(context.getSystemService(Context.WIFI_SERVICE));
            if (null != wm) {
                WifiInfo info = wm.getConnectionInfo();
                String macAddr = (info == null) ? null : info.getMacAddress();
                if (macAddr == null || macAddr.isEmpty()) {
                    Log.w(TAG, "deviceId: Invalid mac address: " + macAddr);
                } else {
                    id = macAddr;
                }
            } else {
                Log.w(TAG, "deviceId: Invalid WifiManager.");
            }
        }

        // finally if non of those method does not work, the id may remains invalid value
        Log.v(TAG, "deviceId: result: " + id);
        return id;
    }
}

