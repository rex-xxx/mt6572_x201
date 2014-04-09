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
 * BluetoothSocketService.java
 * 
 * This is the service for Bluetooth Socket APIs.
 * The APIs in BluetoothSocket.java uses the inerface through AIDL.
 * Below this service, JNI is the media to Mediatek's internal Bluetooth task.
 */

package android.server;

import android.bluetooth.IBluetoothSocket;
import android.bluetooth.BluetoothSocket;
import android.server.BluetoothService;
import android.bluetooth.BluetoothAdapter;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import java.io.IOException;
import android.util.Log;
import android.os.RemoteException;
import android.os.Binder;
import android.os.IBinder;

import java.util.HashMap;
import com.mediatek.xlog.Xlog;


public class BluetoothSocketService extends IBluetoothSocket.Stub
{
    private static final String TAG = "BTSocketService";
    private final Context mContext;
    private final BluetoothService mBluetoothService;
    private BluetoothAdapter mAdapter;
    public static final String BLUETOOTH_SOCKET_SERVICE = "bluetooth_socket";
    private static final String BLUETOOTH_ADMIN_PERM = android.Manifest.permission.BLUETOOTH_ADMIN;    
    
    private static class ServiceRecordClient {
        int pid;
        IBinder binder;
        IBinder.DeathRecipient death;
    }
    private final HashMap<Integer, ServiceRecordClient> mServiceRecordToPid;
    
    private class Reaper implements IBinder.DeathRecipient {
        int mPid;
        int mHandle;

        Reaper(int handle, int pid) {
            mPid = pid;
            mHandle = handle;
        }

        @Override
        public void binderDied() {
            synchronized (BluetoothSocketService.this) {
                log("Tracked app " + mPid + " died");
                ServiceRecordClient client = mServiceRecordToPid.get(mHandle);
				        if (client != null && mPid == client.pid) {
				            log("Removing service record " +
				                Integer.toHexString(mHandle) + " for pid " + mPid);
				
				            if (client.death != null) {
				                client.binder.unlinkToDeath(client.death, 0);
				            }				
				            mServiceRecordToPid.remove(mHandle);
				            
				            abortNative(mHandle);
				            destroyNative(mHandle);
				        }
            }
        }
    }
   
    
    public BluetoothSocketService(Context context, BluetoothService bluetoothService) 
    {
        // Service initialization
        log("[JSR82][Service] Initialization Constructor +++");
        
        mContext = context;

        mBluetoothService = bluetoothService;
        if (mBluetoothService == null) 
        {
            throw new RuntimeException("[JSR82][Service] This platform does not support Bluetooth.");
        }

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null)
        {
            log("[JSR82][Service] Bluetooth Adapter does not exist!!");
        }

        initNative();
        if (mBluetoothService.isEnabled())
        {
            log("[JSR82][Service] Bluetooth is not enabled!!");
        }
        log("[JSR82][Service] Initialization Constructor ---");
        
        mServiceRecordToPid = new HashMap<Integer, ServiceRecordClient>();
    }


    @Override
    protected void finalize() throws Throwable
    {
        try 
        {
            cleanupNative();
        } 
        finally 
        {
            super.finalize();
        }
    }


    /*
     * Interfaces of BluetoothSocket service
     */
    public int initSocket(int type, boolean auth, boolean encrypt, int port, IBinder b) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] initSocket");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result = initSocketNative(type, auth, encrypt, port);
        if (result >= 0)
        {
        	ServiceRecordClient client = new ServiceRecordClient();
	        client.pid = Binder.getCallingPid();
	        client.binder = b;
	        client.death = new Reaper(result, client.pid);
	        mServiceRecordToPid.put(new Integer(result), client);
	        try {
	            b.linkToDeath(client.death, 0);
	        } catch (RemoteException e) {
	            log(e.toString());
	            client.death = null;
	        }	
        }
        return result;
    }


    public int connect(int fdHandle, String sAddr, int channelNumber)
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] connect");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result = connectNative(fdHandle, sAddr, channelNumber);
        return result;
    }


    public int bindListen(int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] bindListen");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result = bindListenNative(fdHandle);
        return result;
    }


    public int accept(int timeout, int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] accept");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }

        int result = acceptNative(timeout, fdHandle);
        return result;
    }


    public int available(int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] available");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result =  availableNative(fdHandle);
        return result;
    }


    public int read(byte[] b, int offset, int length, int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result =  readNative(b, offset, length, fdHandle);
        return result;
    }


    public int write(byte[] b, int offset, int length, int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result = writeNative(b, offset, length, fdHandle);
        return result;
    }


    public int abort(int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] abort");
		//when Bluetooth state is STATE_OFF, HW is off and no request is expected in stack
        if (mAdapter.getState() == BluetoothAdapter.STATE_OFF ||
			mAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result = abortNative(fdHandle);
        return result;
    }


    public int destroy(int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] destroy");

		//when Bluetooth state is STATE_OFF, HW is off and no request is expected in stack
        if (mAdapter.getState() == BluetoothAdapter.STATE_OFF ||
			mAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        int result = destroyNative(fdHandle);
        mServiceRecordToPid.remove(fdHandle);
        return result;
    }


    public void throwErrno(int errno, int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] throwErrno");
        try
        {
            try
            {
                throwErrnoNative(errno, fdHandle);
            }
            catch (IOException e)
            {
                Log.e(TAG, "", e);
                return ;
            }
        }
        finally
        {
            return ;
        }
    }


    public String getAddr(int fdHandle) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] getAddr");
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return null;
        }
        return getAddrNative(fdHandle);
    }


    public int getRealServerChannel(int channelOriginal) 
    {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                    "Need BLUETOOTH_ADMIN permission");
        log("[JSR82][Service] getRealServerChannel. channelOriginal=" + channelOriginal);
        if (mAdapter.isEnabled() != true)
        {
            log("[JSR82][Service] ERROR! Bluetooth is not enabled!");
            return -1;
        }
        return getRealServerChannelNative(channelOriginal);
    }


    private static void log(String msg) 
    {
        Xlog.d(TAG, msg);
    }


    /*
     * Native function interface
     */
    private native void initNative();
    private native void cleanupNative();
    private native int initSocketNative(int type, boolean auth, boolean encrypt, int port);
    private native int connectNative(int fdHandle, String sAddr, int channelNumber);
    private native int bindListenNative(int fdHandle);
    private native int acceptNative(int timeout, int fdHandle);
    private native int availableNative(int fdHandle);
    private native int readNative(byte[] b, int offset, int length, int fdHandle);
    private native int writeNative(byte[] b, int offset, int length, int fdHandle);
    private native int abortNative(int fdHandle);
    private native int destroyNative(int fdHandle);
    private native void throwErrnoNative(int errno, int fdHandle)  throws IOException;
    private native String getAddrNative(int fdHandle);
    private native int getRealServerChannelNative(int channelOriginal);

} 


