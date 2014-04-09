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

import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class BluetoothOpp {

	public static class Client extends BluetoothProxy {

		public Client( Context context ){

			super(context, Client.class);
		}
	}
	
	public static class Server extends BluetoothProxy {

		public Server( Context context ){

			super(context, Server.class);
		}
	}
}

class BluetoothProxy implements BluetoothProfileManager.BluetoothProfileBehavior {

	private static final String TAG = "OPP fw";

	protected Context mContext;

	protected IBluetoothOpp mService;

	protected ServiceConnection mConnection = new ServiceConnection(){

		public void onServiceConnected( ComponentName className, IBinder service ){

			mService = IBluetoothOpp.Stub.asInterface(service);
		}
		public void onServiceDisconnected( ComponentName className ){

			mService = null;
		}
	};

	public BluetoothProxy( Context context, Class<?> clazz ){

		this.mContext = context;
		if( !context.bindService( new Intent(clazz.getName().replaceAll( "\\$", "." )), this.mConnection, Context.BIND_AUTO_CREATE ) ){

			Log.e( TAG, "Could not bind to [" + clazz.getName() + "] Service" );
		}
	}

	public synchronized void close(){

		try {
			if( this.mService != null ){

				this.mService = null;
			}

			if( this.mConnection != null ){

				this.mContext.unbindService( this.mConnection );
				this.mConnection = null;
			}
		}
		catch( Exception ex ){

			Log.e( TAG, "Exception occurred in close(): " + ex );
		}
	}

	private boolean isServiceReady(){

		if( this.mService == null ){

			Log.e( TAG, "mService is null: " + Log.getStackTraceString(new Throwable()) );
			return false;
		}
		return true;
	}

	public boolean connect( BluetoothDevice device ){

		// no connect support
		return false;
	}
	
	public boolean disconnect( BluetoothDevice device ){

		if( this.isServiceReady() ){

			try {
				this.mService.disconnect( device );
				return true;
			}
			catch( RemoteException ex ){

				Log.e( TAG, "Exception occurred in disconnect(): " + ex );
			}
		}
		return false;
	}

	public int getState(BluetoothDevice device){

		Set<BluetoothDevice> remotDevices = null;

		if( this.isServiceReady() ){

			remotDevices = getConnectedDevices();
			
			if(device == null || remotDevices == null || !remotDevices.contains(device))
            return BluetoothProfileManager.STATE_DISCONNECTED;

			try {
				return mService.getState();
			}
			catch( RemoteException ex ){

				Log.e( TAG, "Exception occurred in getState(): " + ex );
			}
		}
		return BluetoothProfileManager.STATE_UNKNOWN;
	}

	public Set<BluetoothDevice> getConnectedDevices() {

		if( this.isServiceReady() ){

			try {
				HashSet<BluetoothDevice> devices = new HashSet<BluetoothDevice>();
				devices.add( mService.getConnectedDevice() );
				return devices;
			}
			catch( RemoteException ex ){

				Log.e( TAG, "Exception occurred in getState(): " + ex );
			}
		}
		return null;
	}

}



