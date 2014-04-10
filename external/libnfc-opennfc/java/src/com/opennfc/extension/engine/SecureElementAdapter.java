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
import com.opennfc.extension.SecureElement;

final class SecureElementAdapter extends IOpenNfcExtSecureElement.Stub {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	private static final String TAG = "SecureElementAdapter";

	private native int getSecureElementsNumber();

	public native SecureElement getSecureElement(int slot);

	public SecureElement[] getSecureElements() {
		SecureElement[] seList = null;
		int seNumber = getSecureElementsNumber();
		if (DEBUG) {
			Log.d(TAG, "seNumber = " + seNumber);
		}
		seList = new SecureElement[seNumber];
		for (int i = 0; i < seNumber; i++) {
			seList[i] = getSecureElement(i);
		}
		return seList;
	}

	public native int setPolicy(int slot, int storageType, int policy);

	public native int getSWPStatus(int slot);

	public native int activateSWPLine(int slot);

}
