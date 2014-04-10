/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opennfc.extension.engine;

import java.util.HashSet;
import java.util.Hashtable;

import com.opennfc.extension.engine.ServiceConnectionManager.ServiceConnectionListener;

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;



/**
 * The class to control the "handle" connection to a client application.
 * The object is intended to be used by any OpenNFC Extensions API that uses OpenNFC handles that should be closed.
 * The purpose of the class is to provide opportunity to close (free) OpenNFC handles that are not closed if the application is dead
 * or if they are not closed at the moment when OpenNFCExtManager.close() is called by the application. 
 * The OpenNFC Extensions API which works with OpenNFC handle should create a HandleConnection object providing the
 * HandleConnectionListener to receive a notification void closedConnectionNotification(int handle) to close
 * Open NFC handle.
 * When a new Open NFC handle is created void open(int handle) should be called.
 * After closing an Open NFC handle (except the body of the listener's notification method) 
 * void close(int handle) should be called.  
 */
final class HandleConnectionManager {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	private static final String TAG = "HandleConnectionManager";
	
	/** a hashtable of internal listeners registered for an API to get notifications about external close event:
	 * dead application or calling of OpenNFCExtManager.close()	 */
	static Hashtable<Integer, ServiceConnectionListenerImpl> listeners = new Hashtable<Integer, ServiceConnectionListenerImpl>();
	
	HandleConnectionListener extListener = null;
	
	HandleConnectionManager(HandleConnectionListener extListener) {
		this.extListener = extListener;
	}
	
	void open(int handle) {
		if (DEBUG) {
			Log.d(TAG, "open(): handle = " + handle);
		}
		ServiceConnectionListenerImpl listener = new ServiceConnectionListenerImpl(handle);
		listeners.put(new Integer(handle), listener);
		ServiceConnectionManager.addListener(listener);
	}
	
	ServiceConnectionListener removeConnectionListener(int handle) {
		return listeners.remove(new Integer(handle));
	}

	/**
	 * Closes the HandleConnection
	 * @param handle Open NFC handle
	 */
	void close(int handle) {
		if (DEBUG) {
			Log.d(TAG, "close(): handle = " + handle);
		}
		ServiceConnectionListener listener = removeConnectionListener(handle);
		ServiceConnectionManager.removeListener(listener);
	}
	
	interface HandleConnectionListener {
		public void closedConnectionNotification(int handle);
	}
	
	private class ServiceConnectionListenerImpl implements ServiceConnectionManager.ServiceConnectionListener {

		private int handle;
		
		ServiceConnectionListenerImpl(int handle) {
			this.handle = handle;
		}
		
		@Override
		public void closedConnectionNotification() {
			if (DEBUG) {
				Log.d(TAG, "closedConnectionNotification(): handle = " + this.handle);
			}
			if (extListener != null) {
				extListener.closedConnectionNotification(this.handle);
			}
			HandleConnectionManager.this.removeConnectionListener(this.handle);
		}
	}
}
