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

import com.opennfc.extension.engine.IOpenNfcExtFirmwareUpdate;
import android.os.RemoteException;
import android.util.Log;


/**
 * The Manager to provide access to Firmware Update via Open NFC Extensions service.
 * <p>It allows a user to update the firmware of the NFC controller and get the version of the firmware
 * that a binary file contains.</p> 
 * <p> A user application must include a permission <b>org.opennfc.permission.FIRMWARE_UPDATE</b> in its 
 * AndroidManifest.xml to have access to Firmware Update API:
 *  <table border=2>
 *  <tr><td>
 *  &lt;uses-permission android:name="org.opennfc.permission.FIRMWARE_UPDATE"/&gt *  </td></tr>
 *  </table>
 *
 */
public final class FirmwareUpdateManager {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	/** Tag use in debug */
	private static final String TAG = FirmwareUpdateManager.class.getSimpleName();

	private IOpenNfcExtFirmwareUpdate fwInterface = null;
	
	FirmwareUpdateManager(IOpenNfcExtFirmwareUpdate fwInterface) {
		this.fwInterface = fwInterface;
	}

	/**
	 * Update the firmware of the NFC controller from the provided binary file
	 * @param fileName the binary file which contains the NFC controller firmware
	 * @throws FirmwareUpdateException if the firmware can't be updated
	 */
	public void update(String fileName) throws FirmwareUpdateException {
		try {
			int status = fwInterface.update(fileName);
			if (DEBUG) {
				Log.d(TAG, "update(): status=" + Integer.toHexString(status));
			}
			if (status != 0) {
				throw new FirmwareUpdateException(status);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "update() fails: " + ex);
			throw new FirmwareUpdateException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	/**
	 * Read a binary file which contains the NFC controller firmware and retrieve the firmware name and version
	 * @param fileName the binary file which contains the NFC controller firmware
	 * @return the firmware name and version 
	 * @throws FirmwareUpdateException if the firmware version can't be retrieved from the file
	 */
	public String getVersion(String fileName) throws FirmwareUpdateException {
		
		try {
			return fwInterface.getVersion(fileName);
		} catch (RemoteException ex) {
			Log.e(TAG, "update() fails: " + ex);
			throw new FirmwareUpdateException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
}