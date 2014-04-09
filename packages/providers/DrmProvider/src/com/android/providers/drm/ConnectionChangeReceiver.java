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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

// when connection is available, sync secure timer
public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "DRM/ConnectionChangeReceiver";
    private static final String INVALID_DEVICE_ID = "000000000000000";

    private static Thread sThread = null; // the thread is static so that only one thread is running to do SNTP sync
    private static final String NETWORK_TYPE_MOBILE_NET =
            Phone.FEATURE_ENABLE_NET;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "onReceive : CONNECTIVITY_CHANGE received.");
        if (FeatureOption.MTK_DRM_APP) {
            OmaDrmClient client = new OmaDrmClient(context);

            // first we check if the secure timer is valid or not
            boolean isValid = OmaDrmHelper.checkClock(client);
            if (isValid) {
                Log.d(TAG, "ConnectionChangeReceiver : Secure timer is already valid");
                return;
            }

            // launch the thread to do SNTP
            if (null != context) {
                ConnectionChangeReceiver.this.launchSNTP(context);
            }
        }
    }

    private void launchSNTP(Context context) {
        if (null == sThread || (null != sThread && !sThread.isAlive())) {
            Log.d(TAG, "SNTP : the thread is not running.");
            OmaDrmClient client = new OmaDrmClient(context);

            // firstly we validate the device id
            String id = OmaDrmHelper.loadDeviceId(client);
            Log.d(TAG, "SNTP : load device id: " + id);

            // get an empty device id: the device id was not saved yet
            if (id.isEmpty()) {
                Log.d(TAG, "SNTP : The device id is empty, try obtain it");
                id = BootCompletedReceiver.deviceId(context);
                Log.d(TAG, "SNTP : Obtained device id: " + id);

                // anyway, we need to save the device id (may be invalid value)
                // so that the secure timer can be saved
                int res = OmaDrmHelper.saveDeviceId(client, id);
            }

            // we already have a device id: no matter if it's empty or not
            if (id.equals(INVALID_DEVICE_ID)) {
                Log.w(TAG, "SNTP : The device id is an invalid value, but we continue processing.");
            }

            // launch SNTP
            if (null != context) {
                Log.d(TAG, "SNTP : launch the thread.");
                ConnectionChangeReceiver.this.launchSimpleThread(context, client);
            }
        } else {
            Log.d(TAG, "SNTP : the thread is already running.");
        }
    }

    private void launchSimpleThread(final Context context, final OmaDrmClient client) {
        // check if the network is usable
        final ConnectivityManager conManager =
            (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == conManager) {
            Log.e(TAG, "SNTP : invalid connectivity manager.");
            return;
        }

        NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
        if (null == networkInfo) {
            Log.e(TAG, "SNTP : invalid active network info.");
            return;
        }
        if (!networkInfo.isAvailable()) {
            Log.e(TAG, "SNTP : unavailable active network.");
            return;
        }

        final int type = networkInfo.getType();
        Log.d(TAG, "SNTP : active network type: " + type);

        sThread = new Thread(new Runnable() {
            public void run() {
                Log.d(TAG, "SNTP : the thread launched.");
                int result = checkRouteToHost(conManager, type);
                if (-1 != result) {
                    int oft = Ntp.sync(sHostList[result]);
                    Log.d(TAG, "SNTP: synchronization result, utc time offset: " + oft);
                    result = OmaDrmHelper.updateClock(client, oft);
                }
            }
        });
        sThread.start();
    }

    // modify these SNTP host servers, for different countries.
    private static String[] sHostList = new String[] {
        "hshh.org",
        "t1.hshh.org",
        "t2.hshh.org",
        "t3.hshh.org",
        "clock.via.net"
    };

    private int checkRouteToHost(ConnectivityManager conManager, int type) {
        Log.v(TAG, "==== check if there's available route to SNTP servers ====");

        int result = -1;
        if (conManager != null) {
            int size = sHostList.length;
            for (int i = 0; i < size; i++) {
                int address = 0;
                try {
                    Log.d(TAG, "get host address by name: [" + sHostList[i] + "].");
                    InetAddress addr = InetAddress.getByName(sHostList[i]);
                    address = ipToInt(addr.getHostAddress());
                } catch (UnknownHostException e) {
                    Log.e(TAG, "caught UnknownHostException");
                    continue;
                }

                Log.d(TAG, "request route for host: [" + sHostList[i] + "].");
                if (conManager.requestRouteToHost(type, address)) {
                    Log.d(TAG, "request route for host success.");
                    result = i;
                    break;
                }
                Log.d(TAG, "request route for host failed.");
            }
        }
        return result;
    }

    private int ipToInt(String ipAddress) {
        if (ipAddress == null) {
            return -1;
        }

        String[] addrArray = ipAddress.split("\\.");
        int size = addrArray.length;
        if (size != 4) {
            return -1;
        }

        int[] addrBytes = new int[size];
        try {
            for (int i = 0; i < size; i++) {
                addrBytes[i] = Integer.parseInt(addrArray[i]);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }

        Log.v(TAG, "ipToInt: a[0] = " + addrBytes[0]
                   + ", a[1] = " + addrBytes[1]
                   + ", a[2] = " + addrBytes[2]
                   + ", a[3] = " + addrBytes[3]);
        return ((addrBytes[3] & 0xff) << 24)
               | ((addrBytes[2] & 0xff) << 16)
               | ((addrBytes[1] & 0xff) << 8)
               | (addrBytes[0] & 0xff);
    }
}
