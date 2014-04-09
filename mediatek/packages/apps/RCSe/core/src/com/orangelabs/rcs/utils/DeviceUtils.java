/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.utils;

import java.util.UUID;

import android.content.Context;
import android.telephony.TelephonyManager;

/***
 * Device utility functions
 * 
 * @author jexa7410
 */
public class DeviceUtils {
	/**
	 * UUID
	 */
	private static UUID uuid = null;

	/**
	 * Returns unique UUID of the device
	 * 
	 * @param context Context 
	 * @return UUID
	 */
	public static UUID getDeviceUUID(Context context) {
		if (context == null) {
			return null;
		}
		
		if (uuid == null) {
			TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String id = tm.getDeviceId();
			if (id != null) { 
				uuid = UUID.nameUUIDFromBytes(id.getBytes());
			}
		}
		
		return uuid;
	}
}
