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

package com.opennfc.extension;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.os.RemoteException;
import android.util.Log;
import com.opennfc.extension.engine.IOpenNfcExtVirtualTag;
import com.opennfc.extension.engine.IOpenNfcExtService;

/**
 * The Manager to provide access to Virtual Tag via Open NFC Extensions service.
 * Make sure to call {@link #startVirtualTagMode startVirtualTagMode()} before
 * doing VirtuaTag operation, startVirtualTagMode disables the NFC read mode.
 * {@link #stopVirtualTagMode stopVirtualTagMode()} is called to re-enable NFC
 * read mode before exiting the virtual tag application.
 * 
 * <p>
 * Even if multiple virtual tag / Card emulation applications are launched
 * simultaneously, please still call {@link #startVirtualTagMode
 * startVirtualTagMode()} per application before doing VirtuaTag operation, and
 * still call {@link #stopVirtualTagMode stopVirtualTagMode()} before exiting
 * the current virtual tag application if user want that the device re-switches
 * to Read mode automatically after exiting all the virtual tag / Card emulation
 * applications. These steps allow Service aware of the number of
 * cardEmulation/VirtualTag applications which are running.
 * 
 * <p>
 * A user application must include a permission
 * <b>org.opennfc.permission.VIRTUAL_TAG</b> in its AndroidManifest.xml to have
 * access to VIRTUAL TAG API:
 * <table border=2>
 * <tr>
 * <td>
 * &lt;uses-permission android:name="org.opennfc.permission.VIRTUAL_TAG"/&gt</td>
 * </tr>
 * </table>
 */
public final class VirtualTagManager {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = "VirtualTagManager";

	private IOpenNfcExtVirtualTag vtInterface = null;

	List<VirtualTag> virtualTagList = new ArrayList<VirtualTag>();

	// private boolean virtualTagEnabled = false;
	private IOpenNfcExtService service = null;
	private int appHandle;
	private String appHandleString;
	private List<String> modeEnabledAppList = new ArrayList<String>();

	VirtualTagManager(IOpenNfcExtVirtualTag vtInterface,
			IOpenNfcExtService service) throws RemoteException {
		this.vtInterface = vtInterface;
		this.service = service;
		this.appHandle = this.service.getAppHandleForCeVt();
		this.appHandleString = String.valueOf(this.appHandle);
		if (DEBUG) {
			Log.i(TAG, "VirtualTag: appHandle = " + this.appHandle);
		}
	}

	/**
	 * Creates a new instance of a Virtual Tag.
	 * 
	 * @param cardType
	 *            the type of the card to be created (type
	 *            CardEmulationConnectionProperty):
	 *            <ul>
	 *            <li>ISO_14443_4_A for a card of type ISO 14443-4 A, or</li>
	 *            <li>ISO_14443_4_B for a card of type ISO 14443-4 B.</li>
	 *            </ul>
	 * @param Identifier
	 *            the card identifier. For a tag of type A, the length of the
	 *            identifier may be 4, 7 or 10 bytes. For a tag of type B, the
	 *            length of the identifier shall be 4.
	 * @param TagCapacity
	 *            The maximum length in bytes reserved for the messages stored
	 *            in the virtual tag. The virtual tag allocate in memory a
	 *            buffer of the specified size. The valid range for this value
	 *            is [0x0003, 0x80FC].
	 * @return a VirtualTag instance.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>cardType</code> or <code>identifier</code> is null.
	 * @throws IllegalArgumentException
	 *             if <code>cardType</code> is unknown or the
	 *             <code>identifier</code> length is not compliant with the card
	 *             type.
	 * @throws VirtualTagException
	 *             Connection to Service error.
	 *             {@link VirtualTagException#VT_ERROR_SERVICE}
	 */
	public VirtualTag createVirtualTagInstance(
			CardEmulationConnectionProperty cardType, byte[] identifier,
			int tagCapacity) throws VirtualTagException {

		if ((cardType == null) || (identifier == null)) {
			throw new IllegalArgumentException(
					"createVirtualTag: cardType or identifier = null");
		}
		if (cardType == CardEmulationConnectionProperty.ISO_14443_4_A) {
			if ((identifier.length != 4) && (identifier.length != 7)
					&& (identifier.length != 10)) {
				throw new IllegalArgumentException(
						"createVirtualTag: the identifier length is not compliant with the card type");
			}
		} else if (cardType == CardEmulationConnectionProperty.ISO_14443_4_B) {
			if (identifier.length != 4) {
				throw new IllegalArgumentException(
						"createVirtualTag: the identifier length is not compliant with the card type");
			}
		} else {
			throw new IllegalArgumentException(
					"createVirtualTag: Unsupported cardType");
		}

		Log.i(TAG, "create new instance VirtualTag");

		int indice = -1;
		try {
			indice = this.vtInterface.getNextIndice();
		} catch (RemoteException e) {
			throw new VirtualTagException(VirtualTagException.VT_ERROR_SERVICE);
		}

		VirtualTag vt = new VirtualTag(this.vtInterface, cardType, identifier,
				tagCapacity, indice);
		virtualTagList.add(vt);
		return vt;
	}

	/**
	 * Start the VirtualTag mode.
	 * 
	 * @param activity
	 *            the current activity to disable NFC
	 * 
	 * @throws VirtualTagException
	 *             if error happens during starting VirtualTag mode.
	 *             {@link VirtualTagException#VT_ERROR_SET_VTMODE}
	 * @throws VirtualTagException
	 *             if communication error with service.
	 *             {@link OpenNfcException#SERVICE_COMMUNICATION_FAILED}
	 * 
	 * @return true if startVirtualTagMode succeeds, false if
	 *         startVirtualTagMode fails.
	 */
	public boolean startVirtualTagMode(Activity activity)
			throws VirtualTagException {
		boolean result = true;
		try {
			modeEnabledAppList = this.service.getModeEnabledAppList();
		} catch (RemoteException e) {
			throw new VirtualTagException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		if (DEBUG) {
			Log.i(TAG, "startVirtualTagMode: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		if (modeEnabledAppList.size() < 1) {
			// Disable NFC to make card emulation avaiable
			/* @comment */NfcAdapter.getDefaultAdapter(activity).disable();
			try {
				Thread.sleep(2000);
			} catch (Exception e) {

			}

			try {
				result = this.vtInterface.setVirtualTagMode(true);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_SET_VTMODE);
			}

			if (result == false) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_SET_VTMODE);
			}

		} else {
			Log.i(TAG,
					"Other card emulation or Virtual Tag applications has already started the VirtualTagMode.");
		}

		if (result) {
			try {
				insertToServerList(appHandleString, modeEnabledAppList);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						OpenNfcException.SERVICE_COMMUNICATION_FAILED);
			}
		}

		if (DEBUG) {
			Log.i(TAG, "startVirtualTagMode Exit: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		return result;
	}

	/**
	 * Stop the VirtualTagMode.
	 * 
	 * @param activity
	 *            the current activity to Re-enable NFC
	 * 
	 * @throws VirtualTagException
	 *             if error happens during stopping VirtualTag mode.
	 *             {@link VirtualTagException#VT_ERROR_SET_VTMODE}
	 * @throws VirtualTagException
	 *             if communication error with service.
	 *             {@link OpenNfcException#SERVICE_COMMUNICATION_FAILED}
	 * 
	 * @return true if stopVirtualTagMode succeeds, false if stopVirtualTagMode
	 *         fails.
	 */
	public boolean stopVirtualTagMode(Activity activity)
			throws VirtualTagException {
		boolean result = true;
		try {
			modeEnabledAppList = this.service.getModeEnabledAppList();
		} catch (RemoteException e) {
			throw new VirtualTagException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		if (DEBUG) {
			Log.i(TAG, "stopVirtualTagMode: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		if ((modeEnabledAppList.size() == 1)
				&& (modeEnabledAppList.get(0).equalsIgnoreCase(appHandleString))) {
			try {
				result = this.vtInterface.setVirtualTagMode(false);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_SET_VTMODE);
			}

			// Re-enable NFC to make Virtual Tag avaiable
			/* @comment */NfcAdapter.getDefaultAdapter(activity).enable();

			if (result == false) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_SET_VTMODE);
			}
		} else {
			Log.i(TAG,
					"Other card emulation or Virtual Tag applications is on, can not completely stop VirtualTagMode .");
		}

		if (result) {
			try {
				removeFromServerList(appHandleString, modeEnabledAppList);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						OpenNfcException.SERVICE_COMMUNICATION_FAILED);
			}
		}

		if (DEBUG) {
			Log.i(TAG, "stopVirtualTagMode Exit: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		return result;
	}

	private synchronized void insertToServerList(String appHandleString,
			List<String> l) throws RemoteException {
		if (l.contains(appHandleString) == false) {
			l.add(appHandleString);
		}
		this.service.setModeEnabledAppList(l);
	}

	private synchronized void removeFromServerList(String appHandleString,
			List<String> l) throws RemoteException {
		l.remove(appHandleString);
		this.service.setModeEnabledAppList(l);
	}

}