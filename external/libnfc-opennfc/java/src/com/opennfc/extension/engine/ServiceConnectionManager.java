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

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;



/**
 * The class to control the connection to a client application.
 * The ServiceConnectionManager object is created for each application utilizing OpenNFCExtensions when the connection to the 
 * OpenNFCService is established.
 * The purpose of the class is to provide opportunity to close (free) the resources that are not freed if the application is dead
 * or if they are not freed at the moment when OpenNFCExtManager.close() is called by the application. 
 *
 */
final class ServiceConnectionManager implements IBinder.DeathRecipient {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	private static final String TAG = "ServiceConnectionManager";
	
	private int pid;
	
	/** a table of all ServiceConnectionManager objects: key is pid of the calling application */
	static private Hashtable<Integer, ServiceConnectionManager> registry = new Hashtable<Integer, ServiceConnectionManager>();

	/** a set of the registered listeners for the application */
	private HashSet<ServiceConnectionListener> listeners = new HashSet<ServiceConnectionListener>();
	
	ServiceConnectionManager() {
		this.pid = getPid();
		if (DEBUG) {
			Log.d(TAG, "ServiceConnection(): pid = " + pid);
		}
		
		registry.put(getPid(), this);
	}

	static ServiceConnectionManager getConnection() {
		return registry.get(getPid());
	}
	
	void close() {
		if (DEBUG) {
			Log.d(TAG, "close()");
		}
		for (ServiceConnectionListener listener : listeners) {
			listener.closedConnectionNotification();
			listeners.remove(listener);
		}
		registry.remove(pid);
	}

	static Integer getPid() {
		return new Integer(Binder.getCallingPid());
	}
	
	static void addListener(ServiceConnectionListener listener) {
		ServiceConnectionManager manager = getConnection();
		manager.listeners.add(listener);
	}

	static void removeListener(ServiceConnectionListener listener) {
		ServiceConnectionManager manager = getConnection();
		manager.listeners.remove(listener);
	}
	
	@Override
	public void binderDied() {
		if (DEBUG) {
			Log.d(TAG, "binderDied()");
		}
		close();
	}
	
	static interface ServiceConnectionListener {
		public void closedConnectionNotification();
	}
}
