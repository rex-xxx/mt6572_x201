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
import java.util.HashMap;
import java.util.Map;

import android.nfc.NdefMessage;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import com.opennfc.extension.engine.IOpenNfcExtVirtualTag;
import com.opennfc.extension.engine.IOpenNfcExtVirtualTagEventHandler;

/**
 * The Virtual Tag is a feature used to simulate a NFC tag with the local
 * device.
 * <p/>
 * 
 * When a virtualTag instance is created by VirtualTagManager, firstly use
 * virtualTagCreate {@link #virtualTagCreate virtualTagCreate()} to call
 * virtualTagCreate method in OpenNFC stack.
 * <p/>
 * The virtual tag is initially empty. The application may fill the tag with the
 * method {@link #writeMessage writeMessage()}. The method {@link #readMessage
 * readMessage()} is used by the application to read the content of the tag.
 * <p/>
 * The application calls the method {@link #start start()} to start the tag
 * simulation. Only one virtual tag of a given type can be simulated at the same
 * time. <strong>When the simulation is active, the application cannot read or
 * write the content of the tag.</strong>
 * <p/>
 * 
 * The application is notified of the following events occuring during the
 * simulation:
 * <ul>
 * <li>A remote reader selects the virtual tag.</li>
 * <li>A remote reader stops communicating with the tag without reading or
 * writing the tag content.</li>
 * <li>A remote reader stops communicating with the tag after reading the tag
 * content.</li>
 * <li>A remote reader stops communicating with the tag after writing the tag
 * content.</li>
 * </ul>
 * The application may subscribe to these events to decide to stop the tag
 * simulation.
 * <p/>
 * During the simulation, a remote reader may read or write the tag. The write
 * operations of a remote reader are allowed only if <code>isReadOnly</code> is
 * set to false when the simulation is started. Several remote readers may read
 * or write the tag during one simulation.
 * <p/>
 * The simulation is stopped with the {@link #stop} method. Then the virtual tag
 * is no longer visible from the remote readers. After stopping the tag, the
 * application may read new the content of the virtual tag using the method
 * {@link #readMessage readMessage()}. The application may also write a new
 * content or restart the simulation. Finally, if the {@link #close} method is
 * called, the current virtual tag is destroyed. {@link #virtualTagCreate} has
 * to be called to enable a new virtual tag.
 * <p/>
 * {@link #close close()} should be called to destory the virtual Tag
 * 
 * @since Open NFC 4.0
 */

public final class VirtualTag implements Parcelable {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = VirtualTag.class.getSimpleName();
	CardEmulationConnectionProperty cardType;
	byte[] identifier;
	private int tagCapacity;
	private int virtualTagHandle = 0;
	private int virtualTagIndice = -1;
	private int eventCode;
	private VirtualTagEventHandler clientListener = null;

	private IOpenNfcExtVirtualTagEventHandler listener = new IOpenNfcExtVirtualTagEventHandler.Stub() {
		public void onTagEventDetected(int event) {
			// at application side, functions like stop();readMessage();start()
			// are called in a callback, opennfc stack does not allow synchrone
			// functions in the callback
			eventCode = event;
			Thread t = new Thread() {
				public void run() {
					// Looper.prepare();
					clientListener.onTagEventDetected(eventCode);
					// Looper.loop();
				}
			};
			t.start();
		}
	};

	private IOpenNfcExtVirtualTag vtInterface = null;

	/**
	 * Construct VirtualTag instance, and create Virtual Tag
	 * 
	 * @param VirtualTagInterface
	 *            adaptor interface
	 * @param cardType
	 *            CardEmulationConnectionProperty type. should be ISO 14443-4 A
	 *            or ISO 14443-4 B
	 * @param identifier
	 *            ID for a virtual tag
	 * @param tagCapacity
	 * @param indice
	 *            Different instance of Virtual Tag
	 */
	VirtualTag(IOpenNfcExtVirtualTag vtInterface,
			CardEmulationConnectionProperty cardType, byte[] identifier,
			int tagCapacity, int virtualTagIndice) {
		this.vtInterface = vtInterface;
		this.cardType = cardType;
		this.identifier = identifier;
		this.tagCapacity = tagCapacity;
		this.virtualTagIndice = virtualTagIndice;
	}

	VirtualTag(Parcel parcel) {
		cardType = CardEmulationConnectionProperty.getConnectionProperty(parcel
				.readInt());
		identifier = readByteArray(parcel);
		tagCapacity = parcel.readInt();
		virtualTagIndice = parcel.readInt();
	}

	/**
	 * call virtualTagCreate in OpenNFC stack, must be called in the beginning.
	 * 
	 * @throws VirtualTagException
	 *             The virtual Tag has already created.
	 *             {@link VirtualTagException#VT_ERROR_VT_ALREADY_CREATED}
	 * @throws VirtualTagException
	 *             Creating virtual Tag fails.
	 *             {@link VirtualTagException#VT_ERROR_CREATE}
	 */
	public void virtualTagCreate() throws VirtualTagException {
		if (this.virtualTagHandle != 0) {
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_VT_ALREADY_CREATED);
		}

		synchronized (this) {
			try {
				this.virtualTagHandle = this.vtInterface.virtualTagCreate(
						this.cardType.getValue(), this.identifier,
						this.tagCapacity, this.virtualTagIndice);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_CREATE);
			}
		}

		if (this.virtualTagHandle == 0) {
			throw new VirtualTagException(VirtualTagException.VT_ERROR_CREATE);
		}
		if (DEBUG)
			Log.i(TAG, "VirtualTag started with virtualTagHandle = "
					+ virtualTagHandle);
	}

	/**
	 * Starts the simulation of the virtual tag.
	 * 
	 * @param eventListener
	 *            an instance of {@link VirtualTagEventHandler} whose
	 *            {@link VirtualTagEventHandler#onTagEventDetected
	 *            onTagEventDetected()} method will be called to notify the
	 *            virtual tag events.
	 * @param isReadOnly
	 *            set to true to set the virtual tag in read only for the remote
	 *            device. If this value is false, the remote device can write in
	 *            the virtual tag.
	 * 
	 * @throws IllegalArgumentException
	 *             if clientListener is null.
	 * @throws VirtualTagException
	 *             Virtual tag is null, please call createVirtualTag firstly.
	 *             {@link VirtualTagException#VT_ERROR_VT_ALREADY_DESTROYED}
	 * @throws VirtualTagException
	 *             if can not register remote listener.
	 *             {@link VirtualTagException#VT_ERROR_REGISTER_LISTENER}
	 * @throws VirtualTagException
	 *             if virtualTag Start error.
	 *             {@link VirtualTagException#VT_ERROR_START}
	 */
	public void start(VirtualTagEventHandler clientListener, boolean isReadOnly)
			throws VirtualTagException {
		int error = -1;
		if (DEBUG)
			Log.i(TAG, "start VirtualTag");
		if (clientListener == null) {
			throw new IllegalArgumentException(
					"VirtualTagEventHandler: clientListener is null");
		}

		if (this.virtualTagHandle == 0) {
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_VT_ALREADY_DESTROYED);
		}

		this.clientListener = clientListener;
		try {
			if (DEBUG)
				Log.i(TAG, "registerVTListener");
			this.vtInterface.registerVTListener(listener, virtualTagIndice);
		} catch (RemoteException e) {
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_REGISTER_LISTENER);
		}

		synchronized (this) {
			try {
				error = this.vtInterface.virtualTagStart(this.virtualTagHandle,
						this.virtualTagIndice, isReadOnly);
				if (error != 0)
					throw new VirtualTagException(
							VirtualTagException.VT_ERROR_START);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_START);
			}

		}
	}

	/**
	 * Stops the simulation of the virtual tag.
	 * 
	 * If the simulation is already stopped, this method does nothing.
	 * 
	 * @throws VirtualTagException
	 *             VirtualTag is null or already destroyed.
	 *             {@link VirtualTagException#VT_ERROR_VT_ALREADY_DESTROYED}
	 * @throws VirtualTagException
	 *             if virtual Tag stops with error.
	 *             {@link VirtualTagException#VT_ERROR_STOP}
	 */
	public void stop() throws VirtualTagException {
		int result = -1;
		this.clientListener = null;
		if (virtualTagHandle == 0) {
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_VT_ALREADY_DESTROYED);
		}

		synchronized (this) {
			try {
				result = this.vtInterface.stopVirtualTag(virtualTagHandle);
				if (result != 0)
					throw new VirtualTagException(
							VirtualTagException.VT_ERROR_STOP);
				this.vtInterface.unRegisterCEListener(listener,
						virtualTagIndice);
			} catch (RemoteException e) {
				throw new VirtualTagException(VirtualTagException.VT_ERROR_STOP);
			}
		}
	}

	/**
	 * Close the simulation of the virtual tag. Kill the virtual tag.
	 * 
	 * @throws VirtualTagException
	 *             VirtualTag is null or already destorird.
	 *             {@link VirtualTagException#VT_ERROR_VT_ALREADY_DESTROYED}
	 * @throws VirtualTagException
	 *             Destory virtual Tag with error.
	 *             {@link VirtualTagException#VT_ERROR_DESTORY}
	 */
	public void close() throws VirtualTagException {
		if (this.virtualTagHandle == 0)
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_VT_ALREADY_DESTROYED);
		synchronized (this) {
			try {
				this.vtInterface.closeVirtualTag(virtualTagHandle);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_DESTORY);
			}
		}
		this.virtualTagHandle = 0;
		this.clientListener = null;
	}

	/**
	 * Retrieve the message of this virtual tag
	 * 
	 * @return NdefMessage
	 * @throws VirtualTagException
	 *             Should call stop before this operation.
	 *             {@link VirtualTagException#VT_ERROR_SHOULD_CALL_STOP}
	 * @throws VirtualTagException
	 *             if virtual tag handle is null
	 *             {@link VirtualTagException#VT_ERROR_VT_ALREADY_DESTROYED}
	 * @throws VirtualTagException
	 *             if readMessage error or received NdefMessage is null.
	 *             {@link VirtualTagException#VT_ERROR_READ_MESSAGE}
	 */
	public NdefMessage readMessage() throws VirtualTagException {
		if (this.clientListener != null)
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_SHOULD_CALL_STOP);
		if (this.virtualTagHandle == 0)
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_VT_ALREADY_DESTROYED);
		NdefMessage ndefMessage = null;
		byte[] ndefData = null;
		synchronized (this) {
			try {
				ndefData = this.vtInterface.readNdefMessage(
						NdefType.W_NDEF_TNF_ANY_TYPE, virtualTagHandle);
				if (ndefData == null) {
					Log.e(TAG, "received ndefData is null");
					throw new VirtualTagException(
							VirtualTagException.VT_ERROR_READ_MESSAGE);
				}
				ndefMessage = new NdefMessage(ndefData);
			} catch (Exception e) {
				Log.e(TAG, "Construct NdefMessage error");
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_READ_MESSAGE);
			}
		}

		if (ndefMessage == null) {
			Log.e(TAG, "ndefMessage is null");
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_READ_MESSAGE);
		}
		return ndefMessage;

	}

	/**
	 * Write Ndef message to this virtual tag
	 * 
	 * @param NdefMessage
	 *            Ndef message to write
	 * @return operation result true(success)/false(failed)
	 * @throws VirtualTagException
	 *             Should call stop before this operation.
	 *             {@link VirtualTagException#VT_ERROR_SHOULD_CALL_STOP}
	 * @throws VirtualTagException
	 *             if virtual tag handle is null
	 *             {@link VirtualTagException#VT_ERROR_VT_ALREADY_DESTROYED}
	 * @throws IllegalArgumentException
	 *             if ndefMessage to write is null.
	 * @throws VirtualTagException
	 *             if writeMessage error.
	 *             {@link VirtualTagException#VT_ERROR_WRITE_MESSAGE}
	 */
	public boolean writeMessage(NdefMessage ndefMessage)
			throws VirtualTagException {

		if (this.clientListener != null)
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_SHOULD_CALL_STOP);

		if (this.virtualTagHandle == 0)
			throw new VirtualTagException(
					VirtualTagException.VT_ERROR_VT_ALREADY_DESTROYED);
		boolean result = false;
		byte[] ndefMessagedata = null;

		if (ndefMessage == null) {
			throw new IllegalArgumentException("NdefMessage to write is null");
		}

		synchronized (this) {
			try {
				ndefMessagedata = ndefMessage.toByteArray();
				result = this.vtInterface.writeNdefMessage(ndefMessagedata,
						virtualTagHandle);
			} catch (RemoteException e) {
				throw new VirtualTagException(
						VirtualTagException.VT_ERROR_WRITE_MESSAGE);
			}
		}

		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(cardType.getValue());
		writeByteArray(dest, identifier);
		dest.writeInt(tagCapacity);
		dest.writeInt(virtualTagIndice);
	}

	/* @hide */public static final Parcelable.Creator<VirtualTag> CREATOR = new Parcelable.Creator<VirtualTag>() {

		public VirtualTag createFromParcel(Parcel source) {
			return new VirtualTag(source);
		}

		@Override
		public VirtualTag[] newArray(int size) {
			return new VirtualTag[size];
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