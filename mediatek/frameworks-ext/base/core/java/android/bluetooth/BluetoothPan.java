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

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the APIs to control the Bluetooth Pan Profile.
 * <p>
 * BluetoothPan is a proxy object for controlling the Bluetooth Service via IPC. Use {@link BluetoothAdapter#getProfileProxy}
 * to get the BluetoothPan proxy object.
 * <p>
 * Each method is protected with its appropriate permission.
 *
 * @hide
 */
public final class BluetoothPan implements BluetoothProfile {
    private static final String TAG = "BluetoothPan";

    private static final boolean DBG = true;

    /**
     * Intent used to broadcast the change in connection state of the Pan profile.
     * <p>
     * This intent will have 4 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile.</li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device.</li>
     * <li> {@link #EXTRA_LOCAL_ROLE} - Which local role the remote device is bound to.</li>
     * </ul>
     * <p>
     * {@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of {@link #STATE_DISCONNECTED},
     * {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     * <p>
     * {@link #EXTRA_LOCAL_ROLE} can be one of {@link #LOCAL_NAP_ROLE} or {@link #LOCAL_PANU_ROLE}
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH} permission to receive.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
        "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Extra for {@link #ACTION_CONNECTION_STATE_CHANGED} intent The local role of the PAN profile that the remote device is
     * bound to. It can be one of {@link #LOCAL_NAP_ROLE} or {@link #LOCAL_PANU_ROLE}.
     */
    public static final String EXTRA_LOCAL_ROLE = "android.bluetooth.pan.extra.LOCAL_ROLE";

    /* MTKBT PAN Role Definition, it shall be referred to bluetooth_pan_struct.h "bt_pan_service_enum" */
    /**
     * The local device is acting as a Network Access Point.
     */
    public static final int LOCAL_NAP_ROLE = 0;

    /**
     * The local device is acting as a Ad-hoc.
     */
    public static final int LOCAL_GN_ROLE = 1;

    /**
     * The local device is acting as a PAN User.
     */
    public static final int LOCAL_PANU_ROLE = 2;

    /**
     * Return codes for the connect and disconnect Bluez / Dbus calls.
     *
     * @hide
     */
    public static final int PAN_DISCONNECT_FAILED_NOT_CONNECTED = 1000;

    /**
     * @hide
     */
    public static final int PAN_CONNECT_FAILED_ALREADY_CONNECTED = 1001;

    /**
     * @hide
     */
    public static final int PAN_CONNECT_FAILED_ATTEMPT_FAILED = 1002;

    /**
     * @hide
     */
    public static final int PAN_OPERATION_GENERIC_FAILURE = 1003;

    /**
     * @hide
     */
    public static final int PAN_OPERATION_SUCCESS = 1004;

    private ServiceListener mServiceListener;

    private BluetoothAdapter mAdapter;

    /* MTKBT: Use IBluetoothPan and bind to BluetoothPanService instead */

    /********************************
     * MTKBT PAN Definition *
     *********************************/

    public static final String DEVICE_ADDR = "device_addr";

    public static final String ACTION = "action";

    public static final String BT_PAN_GN_DEVICE_AUTHORIZE = "bt_pan_GN_device_authorize";

    public static final String BT_PAN_NAP_DEVICE_AUTHORIZE = "bt_pan_NAP_device_authorize";

    public static final String BT_PAN_GN_DEVICE_CONNECTED = "bt_pan_GN_device_connected";

    public static final String BT_PAN_NAP_DEVICE_CONNECTED = "bt_pan_NAP_device_connected";

    public static final String PAN_ROLE_SELECT_INTENT = "android.bluetooth.BluetoothPan.action.ROLE_SELECT";

    /**
     * A BluetoothPAN instance represents a control interface for PAN device.
     */
    private static IBluetoothPan sService;

    private final Context mContext;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            sService = IBluetoothPan.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected, mService: " + sService);
            if (sService == null) {
                Log.e(TAG, "Service connect failed!");
                return;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            sService = null;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    /**
     * Create a BluetoothPan proxy object for interacting with the local Bluetooth Service which handles the Pan profile
     */
    /* package */BluetoothPan(Context context, ServiceListener l) {
        Log.d(TAG, "Start bind to Bluetooth PAN Service, mService: " + sService);
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mServiceListener = l;
        if (sService == null) {
            if (!context.bindService(new Intent(IBluetoothPan.class.getName()), mConnection, Context.BIND_AUTO_CREATE)) {
                Log.e(TAG, "Could not bind to Bluetooth PAN Service");
            }
        }
        if (mServiceListener != null) {
            mServiceListener.onServiceConnected(BluetoothProfile.PAN, this);
        }
    }

    /**
     * Initiate connection to a profile of the remote bluetooth device.
     * <p>
     * This API returns false in scenarios like the profile on the device is already connected or Bluetooth is not turned on.
     * When this API returns true, it is guaranteed that connection state intent for the profile will be broadcasted with the
     * state. Users can get the connection state of the profile from this intent.
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    public boolean connect(BluetoothDevice device) {
        if (DBG) {
            log("connect(" + device + ")");
        }
        /* Since PANU is not supported, do not initiate connection here */
        return false;
        /*
         * if (mService != null) { try { mService.connect(device); } catch (RemoteException e) { Log.e(TAG, "Exception: " +
         * e); return false; } } else { Log.e(TAG, "mService is null"); return false; } return true;
         */
    }

    /* package */void close() {
        mServiceListener = null;
        if (mConnection != null) {
            mContext.unbindService(mConnection);
            mConnection = null;
        }
    }

    protected void finalize() {
        close();
    }


    /**
     * Initiate disconnection from a profile
     * <p>
     * This API will return false in scenarios like the profile on the Bluetooth device is not in connected state etc. When
     * this API returns, true, it is guaranteed that the connection state change intent will be broadcasted with the state.
     * Users can get the disconnection state of the profile from this intent.
     * <p>
     * If the disconnection is initiated by a remote device, the state will transition from {@link #STATE_CONNECTED} to
     * {@link #STATE_DISCONNECTED}. If the disconnect is initiated by the host (local) device the state will transition from
     * {@link #STATE_CONNECTED} to state {@link #STATE_DISCONNECTING} to state {@link #STATE_DISCONNECTED}. The transition to
     * {@link #STATE_DISCONNECTING} can be used to distinguish between the two scenarios.
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     * @hide
     */
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) {
            log("disconnect(" + device + ")");
        }
        if (sService != null) {
            try {
                sService.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception: " + e);
                return false;
            }
        } else {
            Log.e(TAG, "mService is null");
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) {
            log("getConnectedDevices()");
        }
        if (sService != null && isEnabled()) {
            try {
                return sService.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (sService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (DBG) {
            log("getDevicesMatchingStates()");
        }
        if (sService != null && isEnabled()) {
            try {
                return sService.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (sService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getConnectionState(BluetoothDevice device) {
        if (DBG) {
            log("getState(" + device + ")");
        }
        if (sService != null && isEnabled() && isValidDevice(device)) {
            try {
                return sService.getState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (sService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public void setBluetoothTethering(boolean value) {
        if (DBG) {
            log("setBluetoothTethering(" + value + ")");
        }
        if (sService == null) {
            Log.d(TAG, "Service is not ready");
            if (!mContext.bindService(new Intent(IBluetoothPan.class.getName()), mConnection, Context.BIND_AUTO_CREATE)) {
                Log.e(TAG, "Could not bind to Bluetooth Pan Service");
                return;
            }
            return;
        }
        try {
            sService.setBluetoothTethering(value);
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
    }

    public boolean isTetheringOn() {
        if (DBG) {
            log("isTetheringOn(), mService: " + sService);
        }
        if (sService == null) {
            Log.d(TAG, "Service is not ready");
            return false;
        }
        try {
            return sService.isTetheringOn();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    private boolean isEnabled() {
        if (mAdapter.getState() == BluetoothAdapter.STATE_ON) {
            return true;
        }
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        }

        if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
            return true;
        }
        return false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
