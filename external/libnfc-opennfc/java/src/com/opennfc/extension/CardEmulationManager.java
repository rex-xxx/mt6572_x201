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

package com.opennfc.extension;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.os.RemoteException;
import android.util.Log;
import com.opennfc.extension.engine.IOpenNfcExtCardEmulation;
import com.opennfc.extension.engine.IOpenNfcExtService;

/**
 * The Manager to provide access to CardEmulation via Open NFC Extension
 * service. Make sure to call {@link #startCardEmulationMode
 * startCardEmulationMode()} before doing card emulation operation,
 * startCardEmulationMode disables the NFC read mode.
 * 
 * {@link #stopCardEmulationMode stopCardEmulationMode()} is called to re-enable
 * NFC read mode before exiting the card emulation application.
 * <p>
 * Even if multiple virtual tag / Card emulation applications are launched
 * simultaneously, please still call {@link #startCardEmulationMode
 * startCardEmulationMode()} per application before doing CardEmulation
 * operation, and call {@link #stopCardEmulationMode stopCardEmulationMode()}
 * before exiting the current Card Emulation application if user want that the
 * device re-switches to Read mode automatically after exiting all the virtual
 * tag / Card emulation applications. These steps allow Service aware of the
 * number of cardEmulation/VirtualTag applications which are running.
 * <p>
 * A user application must include a permission
 * <b>org.opennfc.permission.CARD_EMULATION</b> in its AndroidManifest.xml to
 * have access to CARD EMULATION API:
 * <table border=2>
 * <tr>
 * <td>
 * &lt;uses-permission android:name="org.opennfc.permission.CARD_EMULATION"/&gt</td>
 * </tr>
 * </table>
 */
public final class CardEmulationManager {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = "CardEmulationManager";

	private IOpenNfcExtCardEmulation ceInterface = null;

	List<CardEmulation> cardEmulationList = new ArrayList<CardEmulation>();

	// private boolean cardEmulationEnabled = false;
	private IOpenNfcExtService service = null;
	private int appHandle;
	private String appHandleString;
	private List<String> modeEnabledAppList = new ArrayList<String>();

	CardEmulationManager(IOpenNfcExtCardEmulation ceInterface,
			IOpenNfcExtService service) throws RemoteException {
		this.ceInterface = ceInterface;
		this.service = service;
		this.appHandle = this.service.getAppHandleForCeVt();
		this.appHandleString = String.valueOf(this.appHandle);
		if (DEBUG) {
			Log.i(TAG, "CardEmulation: appHandle = " + this.appHandle);
		}
	}

	/**
	 * Creates a new instance of a card emulation.
	 * <p/>
	 * The emulation will then start only when {@link CardEmulation#start
	 * CardEmulation.start()} is called.
	 * 
	 * @param cardType
	 *            the type of the card to be created:
	 *            <ul>
	 *            <li>{@link CardEmulationConnectionProperty#ISO_14443_4_A} for
	 *            a card of type ISO 14443-4 A, or</li> <li>
	 *            {@link CardEmulationConnectionProperty#ISO_14443_4_B} for a
	 *            card of type ISO 14443-4 B.</li>
	 *            </ul>
	 * @param identifier
	 *            the card identifier. For a tag of type A, the length of the
	 *            identifier may be 4, 7 or 10 bytes. For a tag of type B, the
	 *            length of the identifier shall be 4.
	 * 
	 * @return the card emulation instance.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>cardType</code> or <code>identifier</code> is null.
	 * @throws IllegalArgumentException
	 *             if <code>cardType</code> is unknown or the
	 *             <code>identifier</code> length is not compliant with the card
	 *             type.
	 * @throws CardEmulationException
	 *             Connection to Service error.
	 *             {@link CardEmulationException#CE_ERROR_SERVICE}
	 */
	public CardEmulation createCardEmulation(
			CardEmulationConnectionProperty cardType, byte[] identifier)
			throws CardEmulationException {

		Log.i(TAG, "createCardEmulation in");
		if ((cardType == null) || (identifier == null)) {
			throw new IllegalArgumentException("cardType or identifier = null");
		}
		if (cardType == CardEmulationConnectionProperty.ISO_14443_4_A) {
			if ((identifier.length != 4) && (identifier.length != 7)
					&& (identifier.length != 10)) {
				throw new IllegalArgumentException(
						"the identifier length is not compliant with the card type");
			}
		} else if (cardType == CardEmulationConnectionProperty.ISO_14443_4_B) {
			if (identifier.length != 4) {
				throw new IllegalArgumentException(
						"the identifier length is not compliant with the card type");
			}
		} else {
			throw new IllegalArgumentException("Unsupported cardType");
		}

		Log.i(TAG, "create new instance CardEmulation");

		int indice = -1;
		try {
			indice = this.ceInterface.getNextIndice();
		} catch (RemoteException e) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_SERVICE);
		}
		CardEmulation ce = new CardEmulation(this.ceInterface, cardType,
				identifier, 0, indice);
		cardEmulationList.add(ce);
		return ce;
	}

	/**
	 * Creates a new instance of a card emulation with a random identity.
	 * <p/>
	 * The emulation will then start only when {@link CardEmulation#start
	 * CardEmulation.start()} is called.
	 * 
	 * @param cardType
	 *            the type of the card to be created:
	 *            <ul>
	 *            <li>{@link CardEmulationConnectionProperty#ISO_14443_4_A} for
	 *            a card of type ISO 14443-4 A, or</li> <li>
	 *            {@link CardEmulationConnectionProperty#ISO_14443_4_B} for a
	 *            card of type ISO 14443-4 B.</li>
	 *            </ul>
	 * @param Length
	 *            length of randomIdentifier
	 * 
	 * @return the card emulation instance.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>cardType</code> or <code>identifier</code> is null.
	 * @throws IllegalArgumentException
	 *             if <code>cardType</code> is unknown or the
	 *             <code>identifier</code> length is not compliant with the card
	 *             type.
	 * @throws CardEmulationException
	 *             Connection to Service error.
	 *             {@link CardEmulationException#CE_ERROR_SERVICE}
	 */
	public CardEmulation createCardEmulation(
			CardEmulationConnectionProperty cardType, int randomIdentifierLength)
			throws CardEmulationException {
		if (cardType == null) {
			throw new IllegalArgumentException("cardType or identifier = null");
		}
		if (cardType == CardEmulationConnectionProperty.ISO_14443_4_A) {
			if ((randomIdentifierLength != 4) && (randomIdentifierLength != 7)
					&& (randomIdentifierLength != 10)) {
				throw new IllegalArgumentException(
						"the identifier length is not compliant with the card type");
			}
			if (randomIdentifierLength != 4) {
				throw new IllegalArgumentException(
						"the identifier length is valid but not supported by the current implementation");
			}
		} else if (cardType == CardEmulationConnectionProperty.ISO_14443_4_B) {
			if (randomIdentifierLength != 4) {
				throw new IllegalArgumentException(
						"the identifier length is not compliant with the card type");
			}
		} else {
			throw new IllegalArgumentException("Unsupported cardType");
		}

		int indice = -1;
		try {
			indice = this.ceInterface.getNextIndice();
		} catch (RemoteException e) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_SERVICE);
		}

		CardEmulation ce = new CardEmulation(this.ceInterface, cardType, null,
				randomIdentifierLength, indice);
		cardEmulationList.add(ce);

		return ce;
	}

	/**
	 * Checks if the NFC Controller supports a connection property for the card
	 * emulation function.
	 * 
	 * @param property
	 *            the connection property to be checked.
	 * 
	 * @throws IllegalArgumentException
	 *             if property in null.
	 * @throws CardEmulationException
	 *             if can not check Connection Property.
	 *             {@link CardEmulationException#CE_ERROR_CHECK_PROPERTY}
	 * 
	 * @return true if the type of connection is supported by the NFC
	 *         Controller, false if this type of connection is not supported.
	 */
	public boolean checkConnectionProperty(
			CardEmulationConnectionProperty property)
			throws CardEmulationException {
		boolean result = false;
		if (property == null) {
			throw new IllegalArgumentException("property = null");
		}

		try {
			result = this.ceInterface.readerIsPropertySupported(property
					.getValue());
		} catch (RemoteException e) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_CHECK_PROPERTY);
		}

		return result;
	}

	/**
	 * Start the CardEmulationMode. This method MUST be called before starting
	 * the card emulation.
	 * 
	 * @param activity
	 *            The current activity to disable NFC
	 * 
	 * @throws CardEmulationException
	 *             if error happens during starting card emulation.
	 *             {@link CardEmulationException#CE_ERROR_SET_CEMODE}
	 * @throws CardEmulationException
	 *             if communication error with service.
	 *             {@link OpenNfcException#SERVICE_COMMUNICATION_FAILED}
	 * 
	 * @return true if startCardEmulationMode succeeds, false if
	 *         startCardEmulationMode fails.
	 */
	public boolean startCardEmulationMode(Activity activity)
			throws CardEmulationException {
		boolean result = true;
		try {
			modeEnabledAppList = this.service.getModeEnabledAppList();
		} catch (RemoteException e) {
			throw new CardEmulationException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		if (DEBUG) {
			Log.i(TAG, "startCardEmulationMode: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		if (modeEnabledAppList.size() < 1) {
			// Disable NFC to make card emulation avaiable
			/* @comment */NfcAdapter.getDefaultAdapter(activity).disable();
			try {
				Thread.sleep(2000);
			} catch (Exception e) {

			}
			try {
				result = this.ceInterface.setCardEmulationMode(true);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_SET_CEMODE);
			}

			if (result == false) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_SET_CEMODE);
			}
		} else {
			Log.i(TAG,
					"Other card emulation or Virtual Tag applications has already started the CardEmulationMode.");
		}

		if (result) {
			try {
				insertToServerList(appHandleString, modeEnabledAppList);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						OpenNfcException.SERVICE_COMMUNICATION_FAILED);
			}
		}

		if (DEBUG) {
			Log.i(TAG,
					"startCardEmulationMode Exit: modeEnabledAppList.size = "
							+ modeEnabledAppList.size());
		}
		return result;
	}

	/**
	 * Stop the CardEmulationMode.
	 * 
	 * @param activity
	 *            The current activity to Re-enable NFC
	 * 
	 * @throws CardEmulationException
	 *             if error happens during stopping card emulation.
	 *             {@link CardEmulationException#CE_ERROR_SET_CEMODE}
	 * @throws CardEmulationException
	 *             if communication error with service.
	 *             {@link OpenNfcException#SERVICE_COMMUNICATION_FAILED}
	 * 
	 * @return true if stopCardEmulationMode succeeds, false if
	 *         stopCardEmulationMode fails.
	 */
	public boolean stopCardEmulationMode(Activity activity)
			throws CardEmulationException {
		boolean result = true;
		try {
			modeEnabledAppList = this.service.getModeEnabledAppList();
		} catch (RemoteException e) {
			throw new CardEmulationException(
					OpenNfcException.SERVICE_COMMUNICATION_FAILED);
		}
		if (DEBUG) {
			Log.i(TAG, "stopCardEmulationMode: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		if ((modeEnabledAppList.size() == 1)
				&& (modeEnabledAppList.get(0).equalsIgnoreCase(appHandleString))) {
			try {
				result = this.ceInterface.setCardEmulationMode(false);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_SET_CEMODE);
			}
			// Re-enable NFC to make card emulation avaiable
			/* @comment */NfcAdapter.getDefaultAdapter(activity).enable();
			if (result == false) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_SET_CEMODE);
			}
		} else {
			Log.i(TAG,
					"Other card emulation or Virtual Tag applications is on, can not completely stop CardEmulationMode.");
		}

		if (result) {
			try {
				removeFromServerList(appHandleString, modeEnabledAppList);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						OpenNfcException.SERVICE_COMMUNICATION_FAILED);
			}
		}

		if (DEBUG) {
			Log.i(TAG, "stopCardEmulationMode Exit: modeEnabledAppList.size = "
					+ modeEnabledAppList.size());
		}
		return result;
	}

	private synchronized void insertToServerList(String appHandleString,
			List<String> l) throws RemoteException {
		if (l.contains(appHandleString) == false) {
			l.add(appHandleString);
		}
		this.service.setModeEnabledAppList(l);
	}

	private synchronized void removeFromServerList(String appHandleString,
			List<String> l) throws RemoteException {
		l.remove(appHandleString);
		this.service.setModeEnabledAppList(l);
	}

}