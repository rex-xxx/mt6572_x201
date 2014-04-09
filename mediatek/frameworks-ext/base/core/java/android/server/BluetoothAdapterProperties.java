/*
 * Copyright (C) 2010 The Android Open Source Project
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
 */

package android.server;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import android.bluetooth.BluetoothUuid;
import com.mediatek.common.featureoption.FeatureOption;

class BluetoothAdapterProperties {

    private static final String TAG = "BluetoothAdapterProperties";

    private final Map<String, String> mPropertiesMap;
    private final Context mContext;
    private final BluetoothService mService;

    BluetoothAdapterProperties(Context context, BluetoothService service) {
        mPropertiesMap = new HashMap<String, String>();
        mContext = context;
        mService = service;
    }

    synchronized String getProperty(String name) {
        if (mPropertiesMap.isEmpty() || (name != null) && name.equals("UUIDs")) {
            getAllProperties();
        }
        return mPropertiesMap.get(name);
    }

    synchronized String getObjectPath() {
		/*Notes: getAdapterPropertiesNative() @BluetoothService.java has been set as sync call in order to fix bug. */
		/*		And dead lock may happen when BT power on(or off) and ACL connection state is changed.*/
		/*		So this API should be processed specially*/
     //   return getProperty("ObjectPath");
     	String path = null;
     	if (!mPropertiesMap.containsKey("ObjectPath")){
			String adapterPath = mService.getAdapterPathNative();
			if (adapterPath != null) {
				path = adapterPath + "/dev_";
        	}
		} else {
			path = mPropertiesMap.get("ObjectPath");
		}
		Log.v(TAG, "getObjectPath():"+ path);
		return path;
    }

    synchronized void clear() {
        mPropertiesMap.clear();
    }

    synchronized boolean isEmpty() {
        return mPropertiesMap.isEmpty();
    }

    synchronized void setProperty(String name, String value) {
        mPropertiesMap.put(name, value);
    }

    synchronized void getAllProperties() {
        mContext.enforceCallingOrSelfPermission(
                BluetoothService.BLUETOOTH_PERM,
                "Need BLUETOOTH permission");
        mPropertiesMap.clear();

        String properties[] = (String[]) mService
                .getAdapterPropertiesNative();
        // The String Array consists of key-value pairs.
        if (properties == null) {
            Log.e(TAG, "*Error*: GetAdapterProperties returned NULL");
            return;
        }

        for (int i = 0; i < properties.length; i++) {
            String name = properties[i];
            String newValue = null;
            if (name == null) {
                Log.e(TAG, "Error:Adapter Property at index " + i + " is null");
                continue;
            }
            if (name.equals("Devices") || name.equals("UUIDs")) {
                StringBuilder str = new StringBuilder();
                int len = Integer.valueOf(properties[++i]);
                for (int j = 0; j < len; j++) {
                    str.append(properties[++i]);
                    str.append(",");
                }
                len = len + addDefaultService(name, str);
                if (len > 0) {
                    newValue = str.toString();
                }
            } else {
                newValue = properties[++i];
            }
            mPropertiesMap.put(name, newValue);
        }

        // Add adapter object path property.
        String adapterPath = mService.getAdapterPathNative();
        if (adapterPath != null) {
            mPropertiesMap.put("ObjectPath", adapterPath + "/dev_");
        }
    }
    
    private int addDefaultService(String name, StringBuilder value){
        int appendLen = 0;
        if (name == null || value == null || !name.equals("UUIDs")) {
            return 0;
        }
        if (FeatureOption.MTK_BT_PROFILE_HFP == true &&
			value.indexOf(BluetoothUuid.HSP_AG.toString()) == -1) {
            value.append(BluetoothUuid.HSP_AG.toString());
            value.append(",");
            appendLen++;
        }
        if (FeatureOption.MTK_BT_PROFILE_HFP == true &&
			value.indexOf(BluetoothUuid.Handsfree_AG.toString()) == -1) {
            value.append(BluetoothUuid.Handsfree_AG.toString());
            value.append(",");
            appendLen++;
        }
        if (FeatureOption.MTK_BT_PROFILE_A2DP == true &&
			value.indexOf(BluetoothUuid.AudioSource.toString()) == -1) {
            value.append(BluetoothUuid.AudioSource.toString());
            value.append(",");
            appendLen++;
        }
        return appendLen;
    }
}
