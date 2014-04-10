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

import java.util.Arrays;

import com.opennfc.extension.OpenNFCExtManager.OpenNFCExtServiceConnectionListener;
import com.opennfc.extension.engine.IOpenNfcExtRoutingTable;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * The Manager to provide access to a Routing Table via Open NFC Extensions service.
 * <p>
 * The routing table is an optional feature of the NFC Controller.
 * <p>
 * The routing table determines the routing target of commands coming from an external reader when the system is in card
 * emulation mode.
 * <p>
 * The identification of the applications to be routed (routing objects) can be done on several ways as described below and
 * depends on the specification of the object (e.g. proprietary technology).
 * <p>
 * - Technology-based Routing
 * <p>
 * - Protocol-based Routing, including proprietary technologies
 * <p>
 * - AID-based Routing
 * 
 * <p>
 * The routing table implementation allows performing the following operations:
 * 
 * <p>
 * - Specify a routing target when a SELECT is received with a specific AID
 * <p>
 * - Specify a routing target when a SELECT is received with an unknown AID or without any AID at all (implicit selection)
 * <p>
 * - Specify a routing target when an APDU (not a SELECT APDU) is received prior any reception of previous SELECT command.
 * </p>
 * <p><p>
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
 * After that it can get an instance of the RoutingTableManager and have access to the Open NFC Extension for the Routing Table API :
 *  <table border=2>
 *  <tr><td>
 *  &nbsp;&nbsp;&nbsp;public void onOpenNFCExtServiceConnected() {<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;RoutingTableManager rtManager = onfcExtManager.getRoutingTableManager();<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;boolean rtSupported = rtManager.isSupported();<br>
 *  &nbsp;&nbsp;&nbsp;}<br>
 *  </td></tr>
 *  </table>
 * 
 * <p>
 * A user application must include permissions <b>org.opennfc.permission.ROUTING_TABLE</b> and <b>org.opennfc.permission.SECURE_ELEMENT</b> 
 * in its AndroidManifest.xml to have access to the Routing Table API:
 * <table border=2>
 * <tr>
 * <td>&lt;uses-permission android:name="org.opennfc.permission.ROUTING_TABLE"/&gt</td>
 * </tr>
 * <tr>
 * <td>&lt;uses-permission android:name="org.opennfc.permission.SECURE_ELEMENT"/&gt</td>
 * </tr>
 * </table>
 * 
 * @see
 * <p>{@link OpenNFCExtManager}
 * <p>{@link RoutingTable}
 * <p>{@link RoutingTableEntry}
 */
public final class RoutingTableManager {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = RoutingTableManager.class.getSimpleName();

	private IOpenNfcExtRoutingTable rtInterface = null;
	
	/** provides access to the list of the Secure Elements which is integrated in the list of available 
	 * RoutingTableEntryTargets	 */
	private SecureElementManager seManager = null;
	
	/** the list of available RoutingTableEntryTargets	 */
	private RoutingTableEntryTarget[] targets = null;
	
	/** the NFC controller to be used as a RoutingTableEntryTarget */
	private RoutingTableEntryTarget nfcc = null;
	
	public RoutingTableEntryTarget getNFCCTarget() throws RoutingTableException {
		if (nfcc == null) {
			fillTargetsList();
		}
		return nfcc;
	}
	
	RoutingTableManager(IOpenNfcExtRoutingTable rtInterface, SecureElementManager seManager) {
		this.rtInterface = rtInterface;
		this.seManager = seManager;
	}
	
	/**
	 * Checks if the routing table functionality is supported
	 * @return true if the routing table functionality is supported
	 * @throws RoutingTableException if information can't be retrieved from the Service
	 */
	public boolean isSupported() throws RoutingTableException {
		try {
			return rtInterface.isSupported();
		} catch (RemoteException ex) {
			Log.e(TAG, "isSupported() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
	
	/**
	 * Creates a new empty routing table
	 * 
	 * @return new empty routing table
	 * @throws RoutingTableException
	 *             if the routing table can't be created
	 */
	public RoutingTable createTable() throws RoutingTableException {
		RoutingTable routingTable = null;
		try {
			int handle = rtInterface.create();
			if (handle != 0) {
				routingTable = new RoutingTable(handle, this);
			} else {
				throw new RoutingTableException(ConstantAutogen.W_ERROR_FEATURE_NOT_SUPPORTED);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "create() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		return routingTable;
	}
	
	/**
	 * Reads the routing table from the NFC controller
	 * 
	 * @return the routing table read from the NFC controller
	 * @throws RoutingTableException
	 *             if the routing table can't be read
	 */
	public RoutingTable readTable() throws RoutingTableException {
		RoutingTable routingTable = null;
		try {
			int handle = rtInterface.read();
			if (handle != 0) {
				routingTable = new RoutingTable(handle, this);
			} else {
				throw new RoutingTableException(ConstantAutogen.W_ERROR_FEATURE_NOT_SUPPORTED);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "readTable() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		return routingTable;
	}

	/**
	 * Checks whether the routing table is enabled
	 * 
	 * @return true if the routing table is enabled 
	 * @throws RoutingTableException
	 *             if the information can't be retrieved
	 */
	public boolean isEnabled() throws RoutingTableException {
		try {
			return rtInterface.isEnabled();
		} catch (RemoteException ex) {
			Log.e(TAG, "isEnabled() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	/**
	 * Disables or enables the routing table
	 * 
	 * @return true if the routing table is enabled 
	 * @param isEnabled true - to enable, false - to disable the routing table
	 * @throws RoutingTableException
	 *             if the operation can't be accomplished 
	 */
	public void enableTable(boolean isEnabled) throws RoutingTableException {
		try {
			int status = rtInterface.enable(isEnabled);
			if (status != 0) {
				throw new RoutingTableException(status);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "isEnabled() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	void applyTable(int handle) throws RoutingTableException {
		try {
			int status = rtInterface.apply(handle);
			if (DEBUG) {
				Log.d(TAG, "applyTable(): status=" + Integer.toHexString(status));
			}
			if (status != 0) {
				throw new RoutingTableException(status);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "applyTable() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}

	private synchronized void fillTargetsList() throws RoutingTableException {
		try {
			SecureElement[] seList = seManager.getSecureElements();
			this.targets = new RoutingTableEntryTarget[seList.length + 1];
			System.arraycopy(seList, 0, this.targets, 0, seList.length);
			
			/* adding NFCC as a target to the list of possible targets for a Routing Table Entry */
			int nfccId = seList.length;
			this.nfcc = new RoutingTableEntryTarget(seList.length, "NFC Controller") {
			};
			targets[nfccId] = this.nfcc;
			
		}  catch(SecureElementException ex) {
			Log.e(TAG, "Can't get list of secure elements");
			throw new RoutingTableException(ConstantAutogen.W_ERROR_BAD_PARAMETER);
		}
	}
	
	/**
	 * Returns the entries of a routing table
	 * @param handle Open NFC's handle for the routing table
	 * @return the array of the entries of the routing table
	 */
	RoutingTableEntry[] getTableEntries(int handle) throws RoutingTableException {
		RoutingTableEntry[] entries = null;
		if (targets == null) {
			fillTargetsList();
		}
		try {
			entries = rtInterface.getEntries(handle);
			for(RoutingTableEntry entry: entries) {
				int targetId = entry.getTargetId();
				int length = targets.length;
				if (targetId < length) {
					entry.setTarget(targets[entry.getTargetId()]);
				} else {
					Log.e(TAG, "Can't detect target for id=" + targetId + ")");
					throw new RoutingTableException(ConstantAutogen.W_ERROR_BAD_PARAMETER);
				}
			}
			return entries;
		} catch (RemoteException ex) {
			Log.e(TAG, "getTableEntries() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
	
	void modifyTable(int handle, int operation, int index, RoutingTableEntry entry) throws RoutingTableException {
		try {
			int status = rtInterface.modify(handle, operation, index, entry);
			if (DEBUG) {
				Log.d(TAG, "modifyTable(): status=" + Integer.toHexString(status));
			}
			if (status != 0) {
				throw new RoutingTableException(status);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "modifyTable() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
	
	void closeTable(int handle) throws RoutingTableException {
		try {
			rtInterface.close(handle);
		} catch (RemoteException ex) {
			Log.e(TAG, "closeTable() fails: " + ex);
			throw new RoutingTableException(OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
	}
}
