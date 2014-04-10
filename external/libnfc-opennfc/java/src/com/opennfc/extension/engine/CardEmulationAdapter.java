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

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

final class CardEmulationAdapter extends IOpenNfcExtCardEmulation.Stub {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	private static final String TAG = "CardEmulationAdapter";
	private int accumulatedIndice = 0;
	private Map<Integer, IOpenNfcExtCardEmulationEventHandler> indice_listener = new HashMap<Integer, IOpenNfcExtCardEmulationEventHandler>();
	private Map<Integer, Integer> indice_handle = new HashMap<Integer, Integer>();

	IOpenNfcExtCardEmulationEventHandler listener = null;

	CardEmulationAdapter() {
		setCardEmulationAdapterObject();
	}

	public int getNextIndice() {
		int index = 0;
		synchronized (this) {
			index = accumulatedIndice++;
		}
		return index;
	}

	public void registerCEListener(
			IOpenNfcExtCardEmulationEventHandler listener, int indice) {

		indice_listener.put(indice, listener);
	}

	public void unRegisterCEListener(
			IOpenNfcExtCardEmulationEventHandler listener, int indice) {
		indice_listener.remove(indice);
	}

	public native void setCardEmulationAdapterObject();

	public native void closeCardEmulationAdapterObject();

	public native int emulOpenConnection(int cardType, byte[] identifier,
			int randomIdentifierLength, int cardEmulationIndice);

	public native boolean readerIsPropertySupported(int cardType);

	public native int stopCardEmulation(int cardEmulationHandle);

	public native int sendAnswer(int handle, byte[] response);

	public native boolean setCardEmulationMode(boolean enable);

	private void cardEmulEventCallback(int cardEmulationIndice, int event) {
		this.listener = indice_listener.get(cardEmulationIndice);
		try {
			this.listener.onEventReceived(event);
		} catch (Exception e) {
			Log.e(TAG, "RemoteException: cardEmulEventCallback error");
		}
	}

	private int getCardEmulationHandle(int cardEmulationIndice) {
		return this.indice_handle.get(cardEmulationIndice);
	}

	private void cardEmulCommandCallback(int cardEmulationIndice, byte[] command) {
		this.listener = indice_listener.get(cardEmulationIndice);

		try {
			this.listener.onCommandReceived(command);
		} catch (Exception e) {
			Log.e(TAG, "RemoteException: cardEmulCommandCallback error");
		}
	}

	public void setMappingIndiceHandle(int cardEmulationIndice,
			int cardEmulationHandle) {
		try {
			this.indice_handle.put(cardEmulationIndice, cardEmulationHandle);
		} catch (Exception e) {
			Log.e(TAG,
					"No validated card emulation handle found in server side");
		}
	}

}
