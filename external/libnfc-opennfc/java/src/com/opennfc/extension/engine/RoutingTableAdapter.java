/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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
 
import android.util.Log;
import com.opennfc.extension.RoutingTable;
import com.opennfc.extension.RoutingTableEntry;
 
final class RoutingTableAdapter extends IOpenNfcExtRoutingTable.Stub implements 
	HandleConnectionManager.HandleConnectionListener {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	private static final String TAG = "RoutingTableAdapter";
	
	private HandleConnectionManager handleManager = new HandleConnectionManager(this);
	
	private native int readTableHandle();

	private native int getTableEntryCount(int handle);

	private native RoutingTableEntry getTableEntry(int handle, int index);

	/**
	 * Creates a Routing Table object which corresponds to the Routing Table with provided Open NFC handle
	 * @param handle the Open NFC handle
	 * @param entries the Routing Table's entries
	 * @return a Routing Table
	 */
	private native RoutingTable readTable(int handle, RoutingTableEntry[] entries);

	/**
	 * Creates a new empty Routing Table that can be used by an application to add entries and to write it 
	 * to the NFC controller 
	 * @return Open NFC's handle for the new routing table
	 */
	public int create() {
		int handle = doCreate();
		if (handle != 0) {
			handleManager.open(handle);
		}
		return handle;
	}

	private native int doCreate();
	
	private static RoutingTableAdapter adapter = null;

	static synchronized RoutingTableAdapter getAdapter() {
		if (adapter == null) {
			adapter = new RoutingTableAdapter();
		}
		return adapter;
	}
	
	/**
	 * Reads the current Routing Table from the NFC controller
	 * @return Open NFC's handle for the current routing table
	 */
	public int read() {
		int handle = doRead();
		if (handle != 0) {
			handleManager.open(handle);
		}
		return handle;
	}

	public native int doRead();
	
	public RoutingTableEntry[] getEntries(int handle) {
		int entryCount = getTableEntryCount(handle);
		if (DEBUG) {
			Log.d(TAG, "entryCount = " + entryCount);
		}
		RoutingTableEntry[] entries = new RoutingTableEntry[entryCount];
		for (int i=0; i<entryCount; i++) {
			entries[i] = getTableEntry(handle, i);
		}
		return entries;
	}
	
	public native int modify(int handle, int operation, int index, RoutingTableEntry entry);
	
	/**
	 * Closes the table's handle
	 * @param handle Open NFC handle of the routing table
	 */
	public void close(int handle) {
		doClose(handle);
		handleManager.close(handle);
	}

	native void doClose(int handle);
	
	public native boolean isSupported();

	public native boolean isEnabled();
	
	public native int enable(boolean isEnabled);
	
	public int apply(int handle) {
//		return applyTable(handle);
		return 0;
	}
	
	public native int getSecureElementsNumber();

	
	@Override
	public void closedConnectionNotification(int handle) {
		if (DEBUG) {
			Log.d(TAG, "closedConnectionNotification(): handle = " + handle);
		}
		doClose(handle);
	}
}
