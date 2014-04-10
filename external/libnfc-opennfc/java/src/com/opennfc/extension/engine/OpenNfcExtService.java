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

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.opennfc.extension.CardEmulation;
import com.opennfc.extension.OpenNfcException;
import com.opennfc.extension.RFActivity;
import com.opennfc.extension.RFLock;


/**
 * The Service that provides Open NFC Extensions functionality
 *
 */
/* @hide */ public final class OpenNfcExtService extends Service {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	private static final String TAG = "OpenNfcExtService";
	
	private SecureElementAdapter secureElementAdapter = null;
	
	private CardEmulationAdapter cardEmulationAdapter = null;
	
	private VirtualTagAdapter virtualTagAdapter = null;
	
	/** permission to execute active control operations for the NFC Controller (f.e. change RF Lock)  */
	private static final String NFC_CONTROLLER_PERMISSION = "org.opennfc.permission.NFC_CONTROLLER";
	
	/** permission to access Security Element API */
	private static final String SECURE_ELEMENT_PERMISSION = "org.opennfc.permission.SECURE_ELEMENT";

	/** permission to execute Firmware Update */
	private static final String FIRMWARE_UPDATE_PERMISSION = "org.opennfc.permission.FIRMWARE_UPDATE";

	/** permission to access Routing Table API */
	private static final String ROUTING_TABLE_PERMISSION = "org.opennfc.permission.ROUTING_TABLE";
	
	/** permission to execute Card Emulation */
	private static final String CARD_EMULATION_PERMISSION = "org.opennfc.permission.CARD_EMULATION";
	
	/** permission to execute Virtual Tag */
	private static final String VIRTUAL_TAG_PERMISSION = "org.opennfc.permission.VIRTUAL_TAG";
	
	private int appHandleForCeVt = 0;
	
	private List<String> modeEnabledAppList = new ArrayList<String>();
	
	private Object lockHandle = new Object();
	private Object lockList = new Object();
	
	static
	{
		System.loadLibrary("nfc_ext_jni");
	}
	
	private native void initialize();
	private native void terminate();
	
    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
        	Log.i(TAG, "onCreate()");
        }
        initialize();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) {
        	Log.i(TAG, "onBind()");
        }
        return serviceBinder;
    }
    
    public boolean onUnbind(Intent intent) {
        if (DEBUG) {
        	Log.i(TAG, "onUnbind()");
        }
    	return super.onUnbind(intent);
    }
    
    public void onDestroy() {
        if (DEBUG) {
        	Log.i(TAG, "onDestroy()");
        }
        
      // Destory when last service unbinded.
      if(cardEmulationAdapter != null)  
          cardEmulationAdapter.closeCardEmulationAdapterObject();
      if(virtualTagAdapter != null) 
    	  virtualTagAdapter.closeVirtualTagAdapterObject();
        terminate();
    	super.onDestroy();
    }

	private native String getStringProperty(int property);
    private native RFActivity getRFActivity();
    private native RFLock getRFLock();
    private native int setRFLock(boolean readerLock, boolean cardLock);
    
    private final IOpenNfcExtService.Stub serviceBinder = new IOpenNfcExtService.Stub() {
        public IOpenNfcExtSecureElement getSecureElementInterface() {
        	
        	chekPermission(SECURE_ELEMENT_PERMISSION);
        	if (secureElementAdapter == null) {
        		secureElementAdapter = new SecureElementAdapter();
        	}
        	return secureElementAdapter;
        }

        public IOpenNfcExtFirmwareUpdate getFirmwareUpdateInterface() {
        	chekPermission(FIRMWARE_UPDATE_PERMISSION);
        	return FirmwareUpdateAdapter.getAdapter();
        }

        
        public IOpenNfcExtRoutingTable getRoutingTableInterface() {
        	chekPermission(ROUTING_TABLE_PERMISSION);
        	return RoutingTableAdapter.getAdapter();
        }
        
        public String getStringProperty(int property) {
        	return OpenNfcExtService.this.getStringProperty(property);
        }
        
        public RFActivity getRFActivity() {
        	return OpenNfcExtService.this.getRFActivity();
        }
        
        public RFLock getRFLock() {
        	return OpenNfcExtService.this.getRFLock();
        }

        public IOpenNfcExtCardEmulation getCardEmulationInterface() {
        	chekPermission(CARD_EMULATION_PERMISSION);
        	synchronized(lockHandle) {
        	appHandleForCeVt++;}
        	Log.i(TAG, "getCardEmulationInterface - appHandleForCeVt :"+appHandleForCeVt);
        	if (cardEmulationAdapter == null) {
        		cardEmulationAdapter = new CardEmulationAdapter();
        	}
        	return cardEmulationAdapter;
        }
        
        public IOpenNfcExtVirtualTag getVirtualTagInterface() {
        	chekPermission(VIRTUAL_TAG_PERMISSION);
        	synchronized(lockHandle) {
        	appHandleForCeVt++;}
        	Log.i(TAG, "getVirtualTagInterface - appHandleForCeVt :"+appHandleForCeVt);
        	if (virtualTagAdapter == null) {
        		virtualTagAdapter = new VirtualTagAdapter();
        	}
        	return virtualTagAdapter;
        }
        
        public int getAppHandleForCeVt() {
    		Log.i(TAG, "getAppHandleForCeVt:"+appHandleForCeVt);
    		return appHandleForCeVt;
    	}
        
        public List<String> getModeEnabledAppList() {
    		Log.i(TAG, "getModeEnabledAppList length:"+modeEnabledAppList.size());
    		return modeEnabledAppList;
    	}
        
        public void  setModeEnabledAppList(List<String> l) {
        	synchronized (lockList) {
    		modeEnabledAppList = l;
    		Log.i(TAG, "setModeEnabledAppList length:"+modeEnabledAppList.size());
        	}
    	}
        
        public int setRFLock(RFLock rfLock) {
        	chekPermission(NFC_CONTROLLER_PERMISSION);
        	return OpenNfcExtService.this.setRFLock(rfLock.getReaderState(), rfLock.getCardState());
        }
        
    	public void open(IBinder binder) {
    		if (DEBUG) {
				Log.d(TAG, "open(): binder=" + binder + ", pid=" + Binder.getCallingPid());
			}
    		try {
    			binder.linkToDeath(new ServiceConnectionManager(), 0);
    		} catch (RemoteException ex) {
				Log.w(TAG, "open(): binder already dead");
    		}
    	}

    	public void close(IBinder binder) {
    		if (DEBUG) {
				Log.d(TAG, "close(): binder=" + binder + ", pid=" + Binder.getCallingPid());
			}
    		ServiceConnectionManager dlManager = ServiceConnectionManager.getConnection();
    		binder.unlinkToDeath(dlManager, 0);
    		dlManager.close();
    	}
    	
    };
    
	private void chekPermission(final String permission) {
		if (checkCallingPermission(permission) != PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException("Permission <" + permission + "> is not granted");
		}
	}
	
}
