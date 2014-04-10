/**
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

import java.util.HashMap;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.opennfc.extension.engine.IOpenNfcExtService;
import com.opennfc.extension.engine.IOpenNfcExtCardEmulation;
import com.opennfc.extension.engine.IOpenNfcExtVirtualTag;

/**
 * OpenNFCExtManager is a class to provide access to OpenNFC Extensions Service.
 * <p>OpenNFCExtManager should be used as an entry point for an application to access any Open NFC Extension API.
 * A user application should call {@link OpenNFCExtManager#getManager(Context, OpenNFCExtServiceConnectionListener)}
 * to initialize access to OpenNFC Extensions:
 *  <table border=2>
 *  <tr><td>
 *  &nbsp;&nbsp;&nbsp;OpenNFCExtManager onfcExtManager = OpenNFCExtManager.getManager(this, this);<br>
 *  </td></tr>
 *  </table>
 * 
 * The application should implement the {@link OpenNFCExtManager.OpenNFCExtServiceConnectionListener}
 * interface to get a notification when the link to the Open NFC Extensions Service is established. 
 * After that it can access Open NFC Extensions:
 *  <table border=2>
 *  <tr><td>
 *  &nbsp;&nbsp;&nbsp;public void onOpenNFCExtServiceConnected() {<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String fwVersion = onfcExtManager.getFirmwareVersion();<br>
 *  &nbsp;&nbsp;&nbsp;}<br>
 *  </td></tr>
 *  </table>
 * <p>Any application that requires Open NFC Extensions should declare the NFC Extensions library in its AndroidManifest.xml:
 *  <table border=2>
 *  <tr><td>
 *  &lt;application .... &gt;<br>
 *  &nbsp;&nbsp;&nbsp;...<br>
 *  &nbsp;&nbsp;&nbsp;&lt;uses-library android:name="NfcExt" /&gt;<br>
 *  &nbsp;&nbsp;&nbsp;...<br>
 *  &lt;/application&gt
 *  </td></tr>
 *  </table>
 *  
 * <p><p>A user application has to include special permissions in its 
 * AndroidManifest.xml to have access to some of the Open NFC Extensions API.
 * F.e. if the application needs to utilize Secure Element API it must declare <b>org.opennfc.permission.SECURE_ELEMENT</b>
 * in AndroidManifest.xml:
 *  <table border=2>
 *  <tr><td>
 *  &lt;uses-permission android:name="org.opennfc.permission.SECURE_ELEMENT"/&gt *  </td></tr>
 *  </table>
 * 
 * <p>A user application <b>MUST</b> call 
 * {@link #close()} in onDestroy() method of the application 
 * to correctly close the connection to the OpenNFC Extensions Service.
 * 
 */ 
final public class OpenNFCExtManager {
	
	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	private static final String TAG = OpenNFCExtManager.class.getSimpleName();

	private IOpenNfcExtService service = null;
	private Context context = null;

	private SecureElementManager secureElementManager = null;
	private FirmwareUpdateManager firmwareUpdateManager = null;
	private CardEmulationManager cardEmulationManager = null;
	private VirtualTagManager virtualTagManager = null;
	private RoutingTableManager routingTableManager = null;
	
	private OpenNFCExtServiceConnectionListener serviceConnectionListener = null;

    private final Binder token;
    
	static final private String API_VERSION = "1.0";
	
	static private HashMap<Context, OpenNFCExtManager> managers = 
			new HashMap<Context, OpenNFCExtManager>();
	
	private OpenNFCExtManager(Context context, OpenNFCExtServiceConnectionListener serviceConnectionListener) {
		if (DEBUG) {
			Log.i(TAG, "OpenNFCExtManager(): context=" + context);
		}
		this.context = context;
		this.serviceConnectionListener = serviceConnectionListener;
		this.token = new Binder();
		context.bindService(new Intent(IOpenNfcExtService.class.getName()),
				serviceConnection, Context.BIND_AUTO_CREATE);		
	}

	/**
	 * Get an OpenNFCExtManager instance for the provided context
	 * @param context application context
	 * @param serviceConnectionListener listener to receive onServiceConnected() notification
	 * @return the OpenNFCExtManager instance
	 */
	public static OpenNFCExtManager getManager(Context context, 
			OpenNFCExtServiceConnectionListener serviceConnectionListener) {
		if (DEBUG) {
			Log.i(TAG, "getManager(): context=" + context);
		}
		OpenNFCExtManager manager = managers.get(context);
		if (manager == null) { 
			manager = new OpenNFCExtManager(context, serviceConnectionListener);
			managers.put(context, manager);
			if (DEBUG) {
				Log.i(TAG, "a new OpenNFCExtManager is created.");
			}
		}
		return manager;
	}

	/**
	 * Close connection to the Open NFC Extensions service
	 * Should be called by the user application's onDestroy()
	 */
	public void close() {
		if (DEBUG) {
			Log.i(TAG, "close()");
		}
		try {
			service.close(token);
		} catch (RemoteException ex) {
			Log.e(TAG, "Can't register close() for OpenNFCExt Service");
		}
		context.unbindService(serviceConnection);
	}
	
	/**
	 * Get the Manager to access Secure Element API
	 * @return the Secure Element Manager instance
	 * @throws RemoteException if can't connect to the Open NFC Extensions service
	 */
	public SecureElementManager getSecureElementManager() throws RemoteException {
		if (service == null) {
			return null;
		}
		if (secureElementManager == null) {
			secureElementManager = new SecureElementManager(service.getSecureElementInterface());
		}
		return secureElementManager; 
	}
	
	/**
	 * Get the Manager to access Firmware Update API
	 * @return the FirmwareUpdateManager instance
	 * @throws RemoteException if can't connect to the Open NFC Extensions service
	 */
	public FirmwareUpdateManager getFirmwareUpdateManager() throws RemoteException {
		if (service == null) {
			return null;
		}
		if (firmwareUpdateManager == null) {
			firmwareUpdateManager = new FirmwareUpdateManager(service.getFirmwareUpdateInterface());
		}
		return firmwareUpdateManager; 
	}
	
	/**
	 * Get the Manager to access Card Emulation API
	 * @return the Card Emulation Manager instance
	 * @throws RemoteException if can't connect to the Open NFC Extensions service
	 */
	public CardEmulationManager getCardEmulationManager() throws RemoteException {
		if (service == null) {
			return null;
		}
		if (cardEmulationManager == null) {
			IOpenNfcExtCardEmulation ice = service.getCardEmulationInterface();
			cardEmulationManager = new CardEmulationManager(ice, service);
		}
		return cardEmulationManager;
	}
	
	/**
	 * Get the Manager to access Virtual Tag API
	 * @return the Virtual Tag Manager instance
	 * @throws RemoteException if can't connect to the Open NFC Extensions service
	 */
	public VirtualTagManager getVirtualTagManager() throws RemoteException {
		if (service == null) {
			return null;
		}
		if (virtualTagManager == null) {
			IOpenNfcExtVirtualTag ivt = service.getVirtualTagInterface();
			virtualTagManager = new VirtualTagManager(ivt, service);
		}
		return virtualTagManager;
	}

	/** 
	 * Get the Manager to access Routing Table API
	 * @return the RoutingTableManager instance
	 * @throws RemoteException if can't connect to the Open NFC Extensions service
	 */
	public RoutingTableManager getRoutingTableManager() throws RemoteException {
		if (service == null) {
			return null;
		}
		if (routingTableManager == null) {
			routingTableManager = new RoutingTableManager(service.getRoutingTableInterface(), 
					getSecureElementManager());
		}
		return routingTableManager; 
	}
	
	/**
	 * Get the version of the Open NFC Extensions API 
	 * @return  the version of the Open NFC Extensions API
	 */
	public String getAPIVersion()  {
		return API_VERSION;
	}

	/**
	 * Get the version of the NFC Controller firmware, including the build number 
	 * @return  the version of the NFC Controller firmware
	 * @throws OpenNfcException if can't retrieve the firmware version
	 */
	public String getFirmwareVersion() throws OpenNfcException {
		if (service == null) {
			return null;
		}
		try {
			return service.getStringProperty(ConstantAutogen.W_NFCC_PROP_FIRMWARE_VERSION);
		} catch (RemoteException ex) {
			Log.e(TAG, "getFirmwareVersion() fails: " + ex);
			throw new OpenNfcException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
	
	/**
	 * Get the version of the NFC Controller firmware loader
	 * @return the version of the NFC Controller firmware loader
	 * @throws OpenNfcException if can't retrieve the firmware version
	 */
	public String getLoaderVersion() throws OpenNfcException {
		if (service == null) {
			return null;
		}
		try {
			return service.getStringProperty(ConstantAutogen.W_NFCC_PROP_LOADER_VERSION);
		} catch (RemoteException ex) {
			Log.e(TAG, "getLoaderVersion() fails: " + ex);
			throw new OpenNfcException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	
	/**
	 * Get the version of the Open NFC library
	 * 
	 * @return the version of the Open NFC library
	 * @throws OpenNfcException
	 *             if can't retrieve the firmware version
	 */
	public String getLibraryVersion() throws OpenNfcException {
		if (service == null) {
			return null;
		}
		try {
			return service.getStringProperty(ConstantAutogen.W_NFCC_PROP_LIBRARY_VERSION);
		} catch (RemoteException ex) {
			throw new OpenNfcException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	/**
	 * Get the current RF Activity state for reader, card emulation and peer to peer modes
	 * 
	 * @return the current RF Activity state
	 * @throws OpenNfcException
	 *             if can't retrieve the the current RF Activity state
	 */
	public RFActivity getRFActivity() throws OpenNfcException {
		if (service == null) {
			return null;
		}
		try {
			return service.getRFActivity();
		} catch (RemoteException ex) {
			throw new OpenNfcException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
	
	/**
	 * Gets the current RF Lock state for reader and card emulation modes
	 * @return the current RF Lock state
	 * @throws OpenNfcException if can't retrieve the current RF Lock state
	 */
	public RFLock getRFLock() throws OpenNfcException {
		if (service == null) {
			return null;
		}
		try {
			return service.getRFLock();
		} catch (RemoteException ex) {
			throw new OpenNfcException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	/**
	 * Sets the current RF Lock state for reader and card emulation modes
	 * @throws OpenNfcException if the RF Lock state can't be changed
	 */
	public void setRFLock(RFLock rfLock) throws OpenNfcException {
		if (service != null) {
			try {
				int status = service.setRFLock(rfLock);
				if (status != 0) {
					throw new OpenNfcException(status);
				}
			} catch (RemoteException ex) {
				throw new OpenNfcException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
			}
			return;
		}
	}
	
	/**
	 * Interface for an application to get notification about established connection 
	 *   to the Open NFC Extensions service
	 *
	 */
	public interface OpenNFCExtServiceConnectionListener {
		/**
		 * Callback to provide notification about established connection 
		 *   to the Open NFC Extensions service
		 */
		void onOpenNFCExtServiceConnected();
	}
	
	final private ServiceConnection serviceConnection = new ServiceConnection()	{

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			service = IOpenNfcExtService.Stub.asInterface(binder);
			if (DEBUG) {
				Log.d(TAG, "onServiceConnected(): binder=" + binder);
			}
			try {
				service.open(token);
			} catch (RemoteException ex) {
				Log.e(TAG, "Can't register open() for OpenNFCExt Service");
			}
			if (serviceConnectionListener != null) {
				serviceConnectionListener.onOpenNFCExtServiceConnected();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}
		
	};
}
