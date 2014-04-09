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

package com.android.server.connectivity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.app.Instrumentation;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;

import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.INetworkStatsService;
import android.net.NetworkStateTracker;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import com.android.server.connectivity.Tethering;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.AndroidTestCase;

import java.util.ArrayList;

import android.util.Log;

/**
 * Test Wifi soft AP configuration
 */
public class TetheringTest extends AndroidTestCase {

    private WifiManager mWifiManager;
    private ConnectivityManager mConnManager;
    private WifiConfiguration mWifiConfig = null;
    private Tethering mTethering;
    private final String TAG = "TetheringTest";
    private final String IFACE_USB = "rndis0";
    private final String IFACE_WIFI = "ap0";
    private final String IFACE_BT   = "btn0";
    private final int DURATION = 10000;
    private Handler mHandler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.v(TAG, "start of tethering Test");
        HandlerThread handlerThread = new HandlerThread("TetheringTestThread");
        handlerThread.start();
        mHandler = new MyHandler(handlerThread.getLooper());
        assertNotNull(mHandler);
        
        IBinder nmBinder = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService nmService = INetworkManagementService.Stub.asInterface(nmBinder);
        assertNotNull(nmService);
        
        IBinder ssBinder = ServiceManager.getService(Context.NETWORK_STATS_SERVICE);
        INetworkStatsService ssService = INetworkStatsService.Stub.asInterface(ssBinder);
        assertNotNull(ssService);
        
        IConnectivityManager csService = IConnectivityManager.Stub
            .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
        assertNotNull(csService);
        
        mTethering = new Tethering(getContext(), nmService, ssService, csService, mHandler.getLooper());
        assertNotNull(mTethering);
        
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);
        mConnManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assertNotNull(mConnManager);
        assertTrue(mWifiManager.setWifiApEnabled(null, true));
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        if (mWifiConfig != null) {
            Log.v(TAG, "mWifiConfig is " + mWifiConfig.toString());
        } else {
            Log.v(TAG, "mWifiConfig is null.");
        }        
    }

    @Override
    protected void tearDown() throws Exception {
        Log.v(TAG, "end of tethering Test");
        mWifiManager.setWifiApEnabled(null, false);
        super.tearDown();
    }

    @LargeTest
    public void testUsbTetheringEnable() {
    //ignored; USB disconnection will cause test fail
	/*
        assertEquals(ConnectivityManager.TETHER_ERROR_NO_ERROR, mConnManager.setUsbTethering(true));
        try {
            Thread.sleep(DURATION);
        } catch (InterruptedException e) {
            Log.v(TAG, "exception " + e.getStackTrace());
            assertFalse(true);
        }
        assertEquals(ConnectivityManager.TETHER_ERROR_NO_ERROR, mConnManager.setUsbTethering(false));
	*/
    }

    @LargeTest
    public void testTetherbleInterfaces() {
        //assertTrue(mTethering.isUsb(IFACE_USB));
        assertTrue(mTethering.isWifi(IFACE_WIFI));
        assertTrue(mTethering.isBluetooth(IFACE_BT));
    }

   @LargeTest
    public void testGetLastTetherError() {
        assertEquals(ConnectivityManager.TETHER_ERROR_UNKNOWN_IFACE, mTethering.getLastTetherError("123RogerBigBig"));
    }

    @LargeTest
    public void testGetUpstreamIfaceTypes() {
        int numIface = mTethering.getUpstreamIfaceTypes().length;
        int values[] = mTethering.getUpstreamIfaceTypes();
        if(numIface == 4){
            assertEquals(ConnectivityManager.TYPE_MOBILE, values[0]);
            assertEquals(ConnectivityManager.TYPE_WIFI, values[1]);
            assertEquals(ConnectivityManager.TYPE_MOBILE_HIPRI, values[2]);
            assertEquals(ConnectivityManager.TYPE_BLUETOOTH, values[3]);
        }else if(numIface == 5){
            assertEquals(ConnectivityManager.TYPE_MOBILE, values[0]);
            assertEquals(ConnectivityManager.TYPE_WIFI, values[1]);
            assertEquals(ConnectivityManager.TYPE_MOBILE_HIPRI, values[2]);
            assertEquals(ConnectivityManager.TYPE_BLUETOOTH, values[3]);
            assertEquals(ConnectivityManager.TYPE_ETHERNET, values[4]);
        }else{
            assertTrue(false);
        }
    }

    @LargeTest
    public void testInterfaceAddRemove() {
        mTethering.interfaceAdded(IFACE_USB);

        Log.d(TAG, "Wait for tethering is up");
        try{
            Thread.sleep(DURATION/2);
        }catch(Exception e){
        }
        String[] tethered = mTethering.getTetheredIfaces();
        
        //assertEquals(1, tethered.length);
        //assertEquals(IFACE_USB, tethered[0]);

        mTethering.interfaceRemoved(IFACE_USB);
        tethered = mTethering.getTetheredIfaces();
        assertEquals(0, tethered.length);
    }
   
    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //NetworkInfo info;
            switch (msg.what) {
                /*case NetworkStateTracker.EVENT_STATE_CHANGED:
                    break;
                case NetworkStateTracker.EVENT_CONFIGURATION_CHANGED:
                    break;
                case EVENT_CLEAR_NET_TRANSITION_WAKELOCK:
                    break;
                case EVENT_RESTORE_DEFAULT_NETWORK:
                    break;
                case EVENT_INET_CONDITION_CHANGE:
                    break;
                case EVENT_INET_CONDITION_HOLD_END:
                    break;
                case EVENT_SET_NETWORK_PREFERENCE:
                    break;
                case EVENT_SET_MOBILE_DATA:
                    break;
                case EVENT_APPLY_GLOBAL_HTTP_PROXY:
                    break;
                case EVENT_SET_DEPENDENCY_MET:
                    break;
                case EVENT_RESTORE_DNS:
                    break;
                case EVENT_SEND_STICKY_BROADCAST_INTENT:
                    break;
                case EVENT_SET_POLICY_DATA_ENABLE:
                    break;
                case EVENT_NOTIFICATION_CHANGED: 
                    break;*/
                default:
                    break;
            }
        }
    }
}
