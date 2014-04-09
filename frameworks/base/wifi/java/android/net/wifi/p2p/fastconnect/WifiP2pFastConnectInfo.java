/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net.wifi.p2p.fastconnect;

import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.WifiP2pService;
import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Slog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing fast connection info
 * @hide
 */
public class WifiP2pFastConnectInfo implements Parcelable {
	private static final String TAG = "WifiP2pFastConnectInfo";
	public int networkId = -1;
	public int venderId = -1;
	public String deviceAddress = "";
	public String ssid = "";
	public String authType = "";
	public String encrType = "";
	public String psk = "";
	public String goIpAddress = WifiP2pService.SERVER_ADDRESS;
	public String gcIpAddress = "192.168.49.2";
	/** 
   * @hide 
   */
  public int wfdDeviceType = WifiP2pWfdInfo.WFD_SOURCE;
	
	private static final Pattern connectCredentialPattern = Pattern.compile(
                "ssid=(DIRECT-[0-9a-zA-Z]{2}) " +
                "auth_type=(0x[0-9]{4}) " +
                "encr_type=(0x[0-9]{4}) " +
                "psk=([0-9a-zA-Z]{8,63})");
	/*private static final Pattern connectCredentialWithIpPattern = Pattern.compile(
			"(ssid=DIRECT-[0-9a-zA-Z]{2}-.) " +
	        "(auth_type=0x[0-9]{4}) " +
			"(encr_type=0x[0-9]{4}) " +
	        "(psk=[0-9a-f]{8,63}) " +
			"(gcIpAddress=\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})");*/
	
	
	public WifiP2pFastConnectInfo() {	
	}
	
	/*"ssid=DIRECT-W8-android auth_type=0x0020 encr_type=0x0008 psk=2182b2e50e53f260d04f3c7b25ef33c965a3291b9b36b455a82d77fd82ca15bc"*/
	public WifiP2pFastConnectInfo(String string) throws IllegalArgumentException {
		String[] tokens = string.split("[ \n]");
		Matcher match;
		boolean isMatch = false;
		
		Slog.d(TAG, "Cedential string is: " + string);
		if (tokens.length < 4) {
            throw new IllegalArgumentException("Malformed Credential String1");
        }
	
        match = connectCredentialPattern.matcher(string);
        if (!match.find()){
        	throw new IllegalArgumentException("Malformed Credential String2");
        }
        
        ssid = match.group(1);
        authType = match.group(2);
        encrType = match.group(3);
        psk = match.group(4);
	}
	
	/** copy constructor */
    public WifiP2pFastConnectInfo(WifiP2pFastConnectInfo source) {
        if (source != null) {
        	networkId = source.networkId;
        	venderId = source.venderId;
        	deviceAddress = source.deviceAddress;
        	ssid = source.ssid;
        	authType = source.authType;
        	encrType = source.encrType;
        	psk = source.psk;
        	gcIpAddress = source.gcIpAddress;
        	wfdDeviceType = source.wfdDeviceType;
        }
    }
	
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("networkId=").append(networkId);
		sbuf.append(" venderId=").append(venderId);
		sbuf.append(" deviceAddress=").append(deviceAddress);
		sbuf.append(" ssid=").append(ssid);
		sbuf.append(" auth_type=").append(authType);
		sbuf.append(" encr_type=").append(encrType);
		sbuf.append(" psk=").append(psk);
		sbuf.append(" gcIpAddress=").append(gcIpAddress);
		sbuf.append(" goIpAddress=").append(goIpAddress);
		sbuf.append(" wfdDeviceType=").append(wfdDeviceType);
		return sbuf.toString();
	}
	
	 /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeInt(networkId);
    	dest.writeInt(venderId);
    	dest.writeString(deviceAddress);
    	dest.writeString(ssid);
        dest.writeString(authType);
        dest.writeString(encrType);
        dest.writeString(psk);
        dest.writeString(gcIpAddress);
        dest.writeInt(wfdDeviceType);
    }
    
    /** Implement the Parcelable interface */
    public static final Creator<WifiP2pFastConnectInfo> CREATOR =
        new Creator<WifiP2pFastConnectInfo>() {
            public WifiP2pFastConnectInfo createFromParcel(Parcel in) {
                WifiP2pFastConnectInfo info = new WifiP2pFastConnectInfo();
                info.networkId = in.readInt();
                info.venderId = in.readInt();
                info.deviceAddress = in.readString();
                info.ssid = in.readString();
                info.authType = in.readString();
                info.encrType = in.readString();
                info.psk = in.readString();
                info.gcIpAddress = in.readString();
                info.wfdDeviceType = in.readInt();
                return info;
            }

            public WifiP2pFastConnectInfo[] newArray(int size) {
                return new WifiP2pFastConnectInfo[size];
            }
        };
	
	
}
