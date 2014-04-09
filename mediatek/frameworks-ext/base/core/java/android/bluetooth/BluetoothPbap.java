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

package android.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;
/***** For profile manager *****/
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import android.bluetooth.BluetoothProfileManager;
import android.os.RemoteException;

/**
 * The Android Bluetooth API is not finalized, and *will* change. Use at your
 * own risk.
 *
 * Public API for controlling the Bluetooth Pbap Service. This includes
 * Bluetooth Phone book Access profile.
 * BluetoothPbap is a proxy object for controlling the Bluetooth Pbap
 * Service via IPC.
 *
 * Creating a BluetoothPbap object will create a binding with the
 * BluetoothPbap service. Users of this object should call close() when they
 * are finished with the BluetoothPbap, so that this proxy object can unbind
 * from the service.
 *
 * This BluetoothPbap object is not immediately bound to the
 * BluetoothPbap service. Use the ServiceListener interface to obtain a
 * notification when it is bound, this is especially important if you wish to
 * immediately call methods on BluetoothPbap after construction.
 *
 * Android only supports one connected Bluetooth Pce at a time.
 *
 * @hide
 */
public class BluetoothPbap implements BluetoothProfileManager.BluetoothProfileBehavior{

    private static final String TAG = "BluetoothPbap";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /** int extra for PBAP_STATE_CHANGED_ACTION */
    public static final String PBAP_STATE =
        "android.bluetooth.pbap.intent.PBAP_STATE";
    /** int extra for PBAP_STATE_CHANGED_ACTION */
    public static final String PBAP_PREVIOUS_STATE =
        "android.bluetooth.pbap.intent.PBAP_PREVIOUS_STATE";

    /** Indicates the state of a pbap connection state has changed.
     *  This intent will always contain PBAP_STATE, PBAP_PREVIOUS_STATE and
     *  BluetoothIntent.ADDRESS extras.
     */
    public static final String PBAP_STATE_CHANGED_ACTION =
        "android.bluetooth.pbap.intent.action.PBAP_STATE_CHANGED";

    private IBluetoothPbap mService;
    private final Context mContext;
    private ServiceListener mServiceListener;

    /** There was an error trying to obtain the state */
    public static final int STATE_ERROR        = -1;
    /** No client currently connected */
    public static final int STATE_DISCONNECTED = 0;
    /** Connection attempt in progress */
    public static final int STATE_CONNECTING   = 1;
    /** Client is currently connected */
    public static final int STATE_CONNECTED    = 2;

    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    /** Connection canceled before completion. */
    public static final int RESULT_CANCELED = 2;

    /**
     * An interface for notifying Bluetooth PCE IPC clients when they have
     * been connected to the BluetoothPbap service.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been
         * connected to the BluetoothPbap service. Clients must wait for
         * this callback before making IPC calls on the BluetoothPbap
         * service.
         */
        public void onServiceConnected(BluetoothPbap proxy);

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothPbap service. Clients must not
         * make IPC calls on the BluetoothPbap service after this callback.
         * This callback will currently only occur if the application hosting
         * the BluetoothPbap service, but may be called more often in future.
         */
        public void onServiceDisconnected();
    }

    /**
     * Create a BluetoothPbap proxy object.
     */
    public BluetoothPbap(Context context, ServiceListener l) {
        mContext = context;
        mServiceListener = l;
        if (!context.bindService(new Intent(IBluetoothPbap.class.getName()), mConnection, 0)) {
            Log.e(TAG, "Could not bind to Bluetooth Pbap Service");
        }
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Close the connection to the backing service.
     * Other public functions of BluetoothPbap will return default error
     * results once close() has been called. Multiple invocations of close()
     * are ok.
     */
    public synchronized void close() {
        if (mConnection != null) {
            mContext.unbindService(mConnection);
            mConnection = null;
        }
        mServiceListener = null;
    }

    /**
     * Get the current state of the BluetoothPbap service.
     * @return One of the STATE_ return codes, or STATE_ERROR if this proxy
     *         object is currently not connected to the Pbap service.
     */
    public int getState() {
        if (VDBG) log("getState()");
        if (mService != null) {
            try {
                return mService.getState();
            } catch (RemoteException e) {Log.e(TAG, e.toString());}
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        }
        return BluetoothPbap.STATE_ERROR;
    }

    /**
     * Get the currently connected remote Bluetooth device (PCE).
     * @return The remote Bluetooth device, or null if not in connected or
     *         connecting state, or if this proxy object is not connected to
     *         the Pbap service.
     */
    public BluetoothDevice getClient() {
        if (VDBG) log("getClient()");
        if (mService != null) {
            try {
                return mService.getClient();
            } catch (RemoteException e) {Log.e(TAG, e.toString());}
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        }
        return null;
    }

    /**
     * Returns true if the specified Bluetooth device is connected (does not
     * include connecting). Returns false if not connected, or if this proxy
     * object is not currently connected to the Pbap service.
     */
    public boolean isConnected(BluetoothDevice device) {
        if (VDBG) log("isConnected(" + device + ")");
        if (mService != null) {
            try {
                return mService.isConnected(device);
            } catch (RemoteException e) {Log.e(TAG, e.toString());}
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Disconnects the current Pbap client (PCE). Currently this call blocks,
     * it may soon be made asynchronous. Returns false if this proxy object is
     * not currently connected to the Pbap service.
     */
    public boolean disconnect() {
        if (VDBG) log("disconnect()");
        if (mService != null) {
            try {
                mService.disconnect();
                return true;
            } catch (RemoteException e) {Log.e(TAG, e.toString());}
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Check class bits for possible PBAP support.
     * This is a simple heuristic that tries to guess if a device with the
     * given class bits might support PBAP. It is not accurate for all
     * devices. It tries to err on the side of false positives.
     * @return True if this device might support PBAP.
     */
    public static boolean doesClassMatchSink(BluetoothClass btClass) {
        // TODO optimize the rule
        switch (btClass.getDeviceClass()) {
        case BluetoothClass.Device.COMPUTER_DESKTOP:
        case BluetoothClass.Device.COMPUTER_LAPTOP:
        case BluetoothClass.Device.COMPUTER_SERVER:
        case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
            return true;
        default:
            return false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (VDBG) log("Proxy object connected");
            mService = IBluetoothPbap.Stub.asInterface(service);
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothPbap.this);
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            if (VDBG) log("Proxy object disconnected");
            mService = null;
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected();
            }
        }
    };

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    /***** For profile manager *****/
    public int getState(BluetoothDevice device){
        int state;
        switch(getState()) {
        case STATE_DISCONNECTED:
            state = BluetoothProfileManager.STATE_ACTIVE;
        case STATE_CONNECTING:
            state = BluetoothProfileManager.STATE_CONNECTING;
        case STATE_CONNECTED:
            state = BluetoothProfileManager.STATE_CONNECTED;
            break;
        case STATE_ERROR:
        default:
            state = BluetoothProfileManager.STATE_UNKNOWN;
            break;
        }
        if (VDBG) log("getState("+device+")="+state);
        return state;
    }
    
    public Set<BluetoothDevice> getConnectedDevices(){
        if (VDBG) log("getConnectedDevices()");
	HashSet<BluetoothDevice> connSet = new HashSet<BluetoothDevice>();
        BluetoothDevice remDev = getClient();
        if (remDev != null)
        {
            connSet.add(remDev);
        }
        return connSet;
    }
    
    public boolean connect(BluetoothDevice device){
        if (VDBG) log("connect("+device+")");
        // PBAP only support, it will not initiate connection
        return false;
    }
    
    public boolean disconnect(BluetoothDevice device){
        if (VDBG) log("disconnect("+device+")");
        return disconnect();
    }
}
