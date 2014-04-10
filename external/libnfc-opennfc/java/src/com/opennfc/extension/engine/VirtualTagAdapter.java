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

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

final class VirtualTagAdapter extends IOpenNfcExtVirtualTag.Stub {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	private static final String TAG = "VirtualTagAdapter";
	private int accumulatedIndice = 0;
	private Map<Integer, IOpenNfcExtVirtualTagEventHandler> indice_listener = new HashMap<Integer, IOpenNfcExtVirtualTagEventHandler>();
	private Map<Integer, Integer> indice_handle = new HashMap<Integer, Integer>();

	IOpenNfcExtVirtualTagEventHandler listener = null;

	VirtualTagAdapter() {
		setVirtualTagAdapterObject();
	}

	public int getNextIndice() {
		int index = 0;
		synchronized (this) {
			index = accumulatedIndice++;
		}
		return index;
	}

	public void registerVTListener(IOpenNfcExtVirtualTagEventHandler listener,
			int indice) {
		indice_listener.put(indice, listener);
	}

	public void unRegisterCEListener(
			IOpenNfcExtVirtualTagEventHandler listener, int indice) {
		indice_listener.remove(indice);
	}

	public native void setVirtualTagAdapterObject();

	public native void closeVirtualTagAdapterObject();

	public native int virtualTagCreate(int cardType, byte[] identifier,
			int tagCapacity, int virtualTagIndice);

	public native int virtualTagStart(int handle, int virtualTagIndice,
			boolean isReadOnly);

	public native int stopVirtualTag(int virtualTagHandle);

	public native void closeVirtualTag(int virtualTagHandle);

	public native boolean setVirtualTagMode(boolean enable);

	public native byte[] readNdefMessage(int ndefType, int virtualTagHandle);

	public native boolean writeNdefMessage(byte[] ndefMessagedata,
			int virtualTagHandle);

	private void virtualTagEventCallback(int virtualTagIndice, int event) {
		try {
			this.listener = indice_listener.get(virtualTagIndice);
			this.listener.onTagEventDetected(event);
		} catch (Exception e) {
			Log.e(TAG, "RemoteException: virtualTagEventCallback error");
		}
	}

}
