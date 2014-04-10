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

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import android.util.Log;

final class FirmwareUpdateAdapter extends IOpenNfcExtFirmwareUpdate.Stub {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	private static final String TAG = "FirmwareUpdateAdapter";

	private static FirmwareUpdateAdapter adapter = null;

	static synchronized FirmwareUpdateAdapter getAdapter() {
		if (adapter == null) {
			adapter = new FirmwareUpdateAdapter();
		}
		return adapter;
	}

	public int update(String fileName) {
		int status = 0;
		byte[] data = getBinaryData(fileName);
		if (data != null) {
			status = updateNative(data);
			if (DEBUG) {
				Log.d(TAG, "update(): status=" + Integer.toHexString(status));
			}
		}
		return status;
	}

	private byte[] getBinaryData(String fileName) {
		byte[] data = null;
		File file = new File(fileName);
		int length = (int) file.length();
		data = new byte[length];
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
			int readLength = in.read(data);
			if (DEBUG) {
				Log.d(TAG, "Read " + readLength + " bytes from " + fileName + " (length = " +  length + ")");
			}
		} catch (Exception ex) {
			Log.e(TAG, "Can't read file data", ex);
			data = null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
				}
			}
		}
		return data;
	}
	
	public String getVersion(String fileName) {
		String version = null;
		byte[] data = getBinaryData(fileName);
		if (data != null) {
			version = getVersionNative(data);
			if (DEBUG) {
				Log.d(TAG, "getVersion(): " + version);
			}
		}
		return version;
	}

	private native String getVersionNative(byte[] data);

	private synchronized native int updateNative(byte[] data);
}
