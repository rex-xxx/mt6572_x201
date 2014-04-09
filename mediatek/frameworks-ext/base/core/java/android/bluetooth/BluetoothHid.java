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

package android.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BluetoothHid implements BluetoothProfileManager.BluetoothProfileBehavior {

    /* Server States */
    public static final int BT_HID_STATE_ACTIVE = 100;

    public static final int BT_HID_STATE_AUTHORIZING = 101;

    public static final int BT_HID_STATE_CONNECTED = 102;

    public static final int BT_HID_STATE_DISCONNECTED = 103;

    public static final int BT_HID_STATE_DISACTIVE = 104;

    public static final int BT_HID_STATE_CONNECTING = 105;

    public static final int BT_HID_STATE_DISCONNECTING = 106;

    public static final String BT_HID_DEVICE_CONNECT = "connected";

    public static final String BT_HID_DEVICE_CONNECTING = "connecting";

    public static final String BT_HID_DEVICE_DISCONNECT = "disconnect";

    public static final String BT_HID_DEVICE_DISCONNECTING = "disconnecting";

    public static final String BT_HID_DEVICE_UNPLUG = "unplug";

    public static final String BT_HID_DEVICE_UNPLUG_DISCONNECT = "unplug_disconnect";

    public static final String BT_HID_DEVICE_AUTHORIZE = "authorize";

    public static final String DEVICE_NAME = "device_name";

    public static final String DEVICE_ADDR = "device_addr";

    public static final String ACTION = "action";

    public static final int MBTEVT_HID_HOST_ENABLE_SUCCESS = 0;

    public static final int MBTEVT_HID_HOST_ENABLE_FAIL = 1;

    public static final int MBTEVT_HID_HOST_DISABLE_SUCCESS = 2;

    public static final int MBTEVT_HID_HOST_DISABLE_FAIL = 3;

    public static final int MBTEVT_HID_HOST_CONNECT_SUCCESS = 4;

    public static final int MBTEVT_HID_HOST_CONNECT_FAIL = 5;

    public static final int MBTEVT_HID_HOST_DISCONNECT_SUCCESS = 6;

    public static final int MBTEVT_HID_HOST_DISCONNECT_FAIL = 7;

    public static final int MBTEVT_HID_HOST_GET_DESC_SUCCESS = 8;

    public static final int MBTEVT_HID_HOST_GET_DESC_FAIL = 9;

    public static final int MBTEVT_HID_HOST_SEND_CONTROL_SUCCESS = 10;

    public static final int MBTEVT_HID_HOST_SEND_CONTROL_FAIL = 11;

    public static final int MBTEVT_HID_HOST_SET_REPORT_SUCCESS = 12;

    public static final int MBTEVT_HID_HOST_SET_REPORT_FAIL = 13;

    public static final int MBTEVT_HID_HOST_GET_REPORT_SUCCESS = 14;

    public static final int MBTEVT_HID_HOST_GET_REPORT_FAIL = 15;

    public static final int MBTEVT_HID_HOST_SET_PROTOCOL_SUCCESS = 16;

    public static final int MBTEVT_HID_HOST_SET_PROTOCOL_FAIL = 17;

    public static final int MBTEVT_HID_HOST_GET_PROTOCOL_SUCCESS = 18;

    public static final int MBTEVT_HID_HOST_GET_PROTOCOL_FAIL = 19;

    public static final int MBTEVT_HID_HOST_SET_IDLE_SUCCESS = 20;

    public static final int MBTEVT_HID_HOST_SET_IDLE_FAIL = 21;

    public static final int MBTEVT_HID_HOST_GET_IDLE_SUCCESS = 22;

    public static final int MBTEVT_HID_HOST_GET_IDLE_FAIL = 23;

    public static final int MBTEVT_HID_HOST_SEND_REPORT_SUCCESS = 24;

    public static final int MBTEVT_HID_HOST_SEND_REPORT_FAIL = 25;

    public static final int MBTEVT_HID_HOST_RECEIVE_UNPLUG = 26;

    public static final int MBTEVT_HID_HOST_RECEIVE_AUTHORIZE = 27;

    private static final String TAG = "BT_HID_JAVA";

    /**
     * A BluetoothHID instance represents a control interface for HID device.
     */
    private IBluetoothHid mService;

    private final Context mContext;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IBluetoothHid.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    public BluetoothHid(Context context) {
        mContext = context;
        if (!context.bindService(new Intent(IBluetoothHid.class.getName()), mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to Bluetooth HID Service");
        }
    }

    public boolean connect(BluetoothDevice device) {

        if (mService != null) {
            try {
                mService.connect(device);
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

    public boolean disconnect(BluetoothDevice device) {

        if (mService != null) {
            try {
                mService.disconnect(device);
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

    public int getState(BluetoothDevice device) {
        int ret = BT_HID_STATE_DISCONNECTED;

        if (mService != null) {
            try {
                ret = mService.getState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception: " + e);
            }

        } else {
            Log.e(TAG, "mService is null");
        }
        return ret;
    }

    public Set<BluetoothDevice> getConnectedDevices() {

        if (mService != null) {
            try {
                return Collections
                        .unmodifiableSet(new HashSet<BluetoothDevice>(Arrays.asList(mService.getCurrentDevices())));
            } catch (RemoteException e) {
                Log.e(TAG, "Exception: " + e);
            }

        } else {
            Log.e(TAG, "mService is null");
        }
        return null;
    }

    public synchronized void close() {
        // try {
        if (mService != null) {
            mService = null;
        }

        if (mConnection != null) {
            mContext.unbindService(mConnection);
            mConnection = null;
        }
        // } catch (Exception e) {
        // Log.e(TAG, "Exception occurred in close(): " + e);
        // }
    }
}
