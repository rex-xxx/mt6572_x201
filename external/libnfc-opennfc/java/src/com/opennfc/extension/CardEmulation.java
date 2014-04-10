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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.opennfc.extension.engine.IOpenNfcExtCardEmulation;
import com.opennfc.extension.engine.IOpenNfcExtCardEmulationEventHandler;

/**
 * The class provides access to card emulation functionalities for a user
 * application.
 * <p/>
 * 
 * After starting the card emulation mode, a client application calls the method
 * {@link #start start()}, then an external reader may start to interact with
 * the simulated card.
 * </p>
 * 
 * <p>
 * <b>Answering to the Reader</b>
 * </p>
 * <p>
 * When a command is received and analyzed, the answer can be sent back to the
 * reader using the method {@link #sendResponse sendResponse()}.
 * </p>
 * 
 * <p>
 * <b>Exclusivity</b>
 * </p>
 * <p>
 * Only one card of a given type can be emulated at the same time. If several
 * applications try to simultaneously emulate the same card type, the first
 * application obtaining the connection has the exclusive use of the service.
 * The other application are rejected.
 * 
 * <p>
 * <b>Stopping the Card Emulation</b>
 * </p>
 * <p>
 * The card emulation is stopped by calling the method {@link #stop stop()}.
 * 
 * 
 */
public final class CardEmulation implements Parcelable {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = CardEmulation.class.getSimpleName();

	CardEmulationConnectionProperty cardType;
	byte[] identifier;
	private int randomIdentifierLength = 0;
	private int cardEmulationHandle = 0;
	private int cardEmulationIndice = -1;
	private int eventCode;
	private byte[] commandCode;
	private CardEmulationEventHandler clientListener = null;

	private IOpenNfcExtCardEmulationEventHandler listener = new IOpenNfcExtCardEmulationEventHandler.Stub() {
		public void onEventReceived(int event) {
			// At application side, function may be called in a callback,
			// Note: opennfc stack does not allow synchronized functions in the
			// callback
			eventCode = event;
			Thread t = new Thread() {
				public void run() {
					// Looper.prepare();
					clientListener.onEventReceived(eventCode);
					// Looper.loop();
				}
			};
			t.start();
		}

		public void onCommandReceived(byte[] command) {
			// At application side, function may be called in a callback,
			// Note: opennfc stack does not allow synchrone functions in the
			// callback
			commandCode = command.clone();
			Thread t = new Thread() {
				public void run() {
					// Looper.prepare();
					clientListener.onCommandReceived(commandCode);
					// Looper.loop();
				}
			};
			t.start();
		}
	};

	private IOpenNfcExtCardEmulation ceInterface = null;

	/**
	 * Construct CardEmulation instance
	 * 
	 * @param ceInterface
	 *            cardEmulation adaptor interface
	 * @param cardType
	 *            should be ISO 14443-4 A or ISO 14443-4 B
	 * @param identifier
	 *            ID for an emulated card
	 * @param randomIdentifierLength
	 *            for the case when identifier is not set
	 * @param cardEmulationIndice
	 *            Different instance of card emulator
	 */
	CardEmulation(IOpenNfcExtCardEmulation ceInterface,
			CardEmulationConnectionProperty cardType, byte[] identifier,
			int randomIdentifierLength, int cardEmulationIndice) {
		this.ceInterface = ceInterface;
		this.cardType = cardType;
		this.identifier = identifier;
		this.randomIdentifierLength = randomIdentifierLength;
		this.cardEmulationIndice = cardEmulationIndice;
	}

	CardEmulation(Parcel parcel) {
		cardType = CardEmulationConnectionProperty.getConnectionProperty(parcel
				.readInt());
		identifier = readByteArray(parcel);
		randomIdentifierLength = parcel.readInt();
		cardEmulationIndice = parcel.readInt();
	}

	/**
	 * Starts the emulation of the card.
	 * 
	 * @param clientListener
	 *            a {@link CardEmulationEventHandler} whose
	 *            {@link CardEmulationEventHandler#onEventReceived
	 *            onEventReceived()} method will be called for each event
	 *            received or
	 *            {@link CardEmulationEventHandler#onCommandReceived
	 *            onCommandReceived()} method will be called for each command
	 *            received.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>clientListener</code> is null.
	 * @throws CardEmulationException
	 *             when error on registering remote listener.
	 *             {@link CardEmulationException#CE_ERROR_REGISTER_LISTENER}
	 * @throws CardEmulationException
	 *             if the emulation is already started.
	 *             {@link CardEmulationException#CE_ERROR_CE_ALREADY_STARTED}
	 * @throws CardEmulationException
	 *             in case of NFC error.
	 *             {@link CardEmulationException#CE_ERROR_SERVICE}
	 */
	public void start(CardEmulationEventHandler clientListener)
			throws CardEmulationException {
		if (DEBUG) {
			Log.i(TAG, "start CardEmulation");
		}

		if (clientListener == null) {
			throw new IllegalArgumentException("clientListener is null");
		}

		if (this.cardEmulationHandle != 0) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_CE_ALREADY_STARTED);
		}

		this.clientListener = clientListener;
		try {
			if (DEBUG) {
				Log.i(TAG, "registerCEListener");
			}
			this.ceInterface.registerCEListener(listener, cardEmulationIndice);
		} catch (RemoteException e) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_REGISTER_LISTENER);
		}

		synchronized (this) {
			try {
				if (DEBUG) {
					Log.i(TAG, "emulOpenConnection");
				}
				this.cardEmulationHandle = this.ceInterface.emulOpenConnection(
						cardType.getValue(), identifier,
						randomIdentifierLength, cardEmulationIndice);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_OPEN_CONNECTION);
			}

		}

		if (this.cardEmulationHandle == 0) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_OPEN_CONNECTION);
		}

		try {
			if (DEBUG) {
				Log.i(TAG, "setMappingIndiceHandle with cardEmulationHandle: "
						+ this.cardEmulationHandle);
			}
			this.ceInterface.setMappingIndiceHandle(cardEmulationIndice,
					cardEmulationHandle);
		} catch (RemoteException e) {
			throw new CardEmulationException(
					CardEmulationException.CE_ERROR_SERVICE);
		}
	}

	/**
	 * Stop the emulation of the card.
	 * 
	 * @throws CardEmulationException
	 *             If card emulation can not be stopped. If the emulation is
	 *             already stopped, this method does nothing.
	 *             {@link CardEmulationException#CE_ERROR_STOP}
	 */
	public void stop() throws CardEmulationException {
		int result = -1;
		if (this.cardEmulationHandle == 0)
			return;
		synchronized (this) {
			try {
				result = this.ceInterface
						.stopCardEmulation(cardEmulationHandle);
				if (result != 0)
					throw new CardEmulationException(
							CardEmulationException.CE_ERROR_STOP);
				this.ceInterface.unRegisterCEListener(listener,
						cardEmulationIndice);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_STOP);
			}
		}
		this.cardEmulationHandle = 0;
	}

	/**
	 * Send the response of the card emulation to the reader.
	 * 
	 * @param response
	 *            the response data to be sent to the reader.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>response</code> is null.
	 * @throws CardEmulationException
	 *             if the card emulation is stopped or in case of NFC error.
	 *             {@link CardEmulationException#CE_ERROR_CE_ALREADY_STOPPED}
	 * @throws CardEmulationException
	 *             if answer can not be sent.
	 *             {@link CardEmulationException#CE_ERROR_SEND_ANSWER}
	 */
	public void sendResponse(byte[] response) throws CardEmulationException {
		int result = -1;
		if (response == null) {
			throw new IllegalArgumentException("response = null");
		}
		synchronized (this) {
			if (this.cardEmulationHandle == 0) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_CE_ALREADY_STOPPED);
			}

			try {
				result = this.ceInterface.sendAnswer(cardEmulationHandle,
						response);
				if (result != 0)
					throw new CardEmulationException(
							CardEmulationException.CE_ERROR_SEND_ANSWER);
				Log.i(TAG, "answer length is: " + response.length);
			} catch (RemoteException e) {
				throw new CardEmulationException(
						CardEmulationException.CE_ERROR_SEND_ANSWER);
			}

		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(cardType.getValue());
		writeByteArray(dest, identifier);
		dest.writeInt(randomIdentifierLength);
		dest.writeInt(cardEmulationIndice);
	}

	/* @hide */public static final Parcelable.Creator<CardEmulation> CREATOR = new Parcelable.Creator<CardEmulation>() {

		public CardEmulation createFromParcel(Parcel source) {
			return new CardEmulation(source);
		}

		@Override
		public CardEmulation[] newArray(int size) {
			return new CardEmulation[size];
		}
	};

	private static void writeByteArray(Parcel parcel, byte[] array) {
		if (array == null) {
			parcel.writeInt(-1);
			return;
		}

		parcel.writeInt(array.length);
		for (int i = 0; i < array.length; i++)
			parcel.writeByte(array[i]);
	}

	private static byte[] readByteArray(Parcel parcel) {
		int length = parcel.readInt();

		if (length < 0) {
			return null;
		}

		byte[] array = new byte[length];
		for (int i = 0; i < length; i++)
			array[i] = parcel.readByte();

		return array;
	}
}