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

package com.opennfc.extension;

import android.os.RemoteException;
import android.util.Log;
import com.opennfc.extension.engine.IOpenNfcExtSecureElement;

/**
 * The Manager to provide access to Secure Element via Open NFC Extensions
 * service.
 * <p>
 * A user application must include a permission
 * <b>org.opennfc.permission.SECURE_ELEMENT</b> in its AndroidManifest.xml to
 * have access to Secure Element API:
 * <table border=2>
 * <tr>
 * <td>
 * &lt;uses-permission android:name="org.opennfc.permission.SECURE_ELEMENT"/&gt
 * *</td>
 * </tr>
 * </table>
 */
public final class SecureElementManager {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = "SecureElementManager";

	private IOpenNfcExtSecureElement seInterface = null;

	SecureElementManager(IOpenNfcExtSecureElement seInterface) {
		this.seInterface = seInterface;
	}

	/**
	 * Gets all available Secure Elements
	 * 
	 * @return an array of available Secure Elements
	 * @throws SecureElementException
	 *             if can't get Secure Elements data
	 */
	public SecureElement[] getSecureElements() throws SecureElementException {
		SecureElement[] seList = new SecureElement[0];
		try {
			seList = seInterface.getSecureElements();
			for (int i = 0; i < seList.length; i++) {
				seList[i].setSecureElementManager(this);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "getSecureElements() fails: " + ex);
			throw new SecureElementException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		return seList;
	}

	/**
	 * Set Secure Element's policy
	 * 
	 * @param slot
	 *            the slot identifier in the range [0,
	 *            NUMBER_OF_SECURE_ELEMENTS]
	 * @param storageType
	 *            identifier of the storage type for the updated policy
	 * @param policy
	 *            new Secure Element's policy
	 * @throws SecureElementException
	 *             if policy can't be updated
	 */
	void setPolicy(int slot, int storageType, int policy)
			throws SecureElementException {
		int status = 0;

		try {
			status = seInterface.setPolicy(slot, storageType, policy);
		} catch (RemoteException ex) {
			Log.e(TAG, "setPolicy() fails: " + ex);
			throw new SecureElementException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		if (status != 0) {
			throw new SecureElementException(status);
		}
	}

	/**
	 * Get status of a SWP link
	 * 
	 * @param slot
	 *            the slot identifier in the range [0,
	 *            NUMBER_OF_SECURE_ELEMENTS]
	 * @return status of SWP link
	 * @throws SecureElementException
	 */
	int getSWPStatus(int slot) throws SecureElementException {
		int swpStatus;

		try {
			swpStatus = seInterface.getSWPStatus(slot);
		} catch (RemoteException ex) {
			Log.e(TAG, "getSWPStatus() fails: " + ex);
			throw new SecureElementException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		return swpStatus;
	}

	/**
	 * Activate the SWP line for the Secure Element
	 * 
	 * @throws SecureElementException
	 */
	void activateSWPLine(int slot) throws SecureElementException {
		int status = 0;

		try {
			status = seInterface.activateSWPLine(slot);
		} catch (RemoteException ex) {
			Log.e(TAG, "setPolicy() fails: " + ex);
			throw new SecureElementException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		if (status != 0) {
			throw new SecureElementException(status);
		}
	}

}