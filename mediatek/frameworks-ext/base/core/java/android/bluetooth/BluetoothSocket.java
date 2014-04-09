/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.bluetooth;

import android.bluetooth.IBluetoothCallback;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.bluetooth.IBluetoothSocket;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.server.BluetoothSocketService;

import com.mediatek.common.featureoption.FeatureOption;



/**
 * A connected or connecting Bluetooth socket.
 *
 * <p>The interface for Bluetooth Sockets is similar to that of TCP sockets:
 * {@link java.net.Socket} and {@link java.net.ServerSocket}. On the server
 * side, use a {@link BluetoothServerSocket} to create a listening server
 * socket. When a connection is accepted by the {@link BluetoothServerSocket},
 * it will return a new {@link BluetoothSocket} to manage the connection.
 * On the client side, use a single {@link BluetoothSocket} to both initiate
 * an outgoing connection and to manage the connection.
 *
 * <p>The most common type of Bluetooth socket is RFCOMM, which is the type
 * supported by the Android APIs. RFCOMM is a connection-oriented, streaming
 * transport over Bluetooth. It is also known as the Serial Port Profile (SPP).
 *
 * <p>To create a {@link BluetoothSocket} for connecting to a known device, use
 * {@link BluetoothDevice#createRfcommSocketToServiceRecord
 * BluetoothDevice.createRfcommSocketToServiceRecord()}.
 * Then call {@link #connect()} to attempt a connection to the remote device.
 * This call will block until a connection is established or the connection
 * fails.
 *
 * <p>To create a {@link BluetoothSocket} as a server (or "host"), see the
 * {@link BluetoothServerSocket} documentation.
 *
 * <p>Once the socket is connected, whether initiated as a client or accepted
 * as a server, open the IO streams by calling {@link #getInputStream} and
 * {@link #getOutputStream} in order to retrieve {@link java.io.InputStream}
 * and {@link java.io.OutputStream} objects, respectively, which are
 * automatically connected to the socket.
 *
 * <p>{@link BluetoothSocket} is thread
 * safe. In particular, {@link #close} will always immediately abort ongoing
 * operations and close the socket.
 *
 * <p class="note"><strong>Note:</strong>
 * Requires the {@link android.Manifest.permission#BLUETOOTH} permission.
 *
 * {@see BluetoothServerSocket}
 * {@see java.io.InputStream}
 * {@see java.io.OutputStream}
 */
public final class BluetoothSocket implements Closeable, Parcelable {
    private static final String TAG = "BluetoothSocket";
    private static final String TAG_2 = "BluetoothSocket_MTK";

    /** @hide */
    public static final int MAX_RFCOMM_CHANNEL = 30;

    /** Keep TYPE_ fields in sync with BluetoothSocket.cpp */
    /*package*/ static final int TYPE_RFCOMM = 1;
    /*package*/ static final int TYPE_SCO = 2;
    /*package*/ static final int TYPE_L2CAP = 3;

    /*package*/ static final int EBADFD = 77;
    /*package*/ static final int EADDRINUSE = 98;

    private final int mType;  /* one of TYPE_RFCOMM etc */
    private final BluetoothDevice mDevice;    /* remote device */
    private final String mAddress;    /* remote address */
    private final boolean mAuth;
    private final boolean mEncrypt;
    private final BluetoothInputStream mInputStream;
    private final BluetoothOutputStream mOutputStream;
    private final SdpHelper mSdp;

    private IBluetoothSocket mService;
    //private final Context mContext;

    private int mPort;  /* RFCOMM channel or L2CAP psm */

    private enum SocketState {
        INIT,
        CONNECTED,
        CLOSED
    }

    /** prevents all native calls after destroyNative() */
    private SocketState mSocketState;

    /** protects mSocketState */
    private final ReentrantReadWriteLock mLock;

    /** used by native code only */
    private int mSocketData;

    private int mFdHandle;  /* The handle of this port. */

    /**
     * Construct a BluetoothSocket.
     * @param type    type of socket
     * @param fd      fd to use for connected socket, or -1 for a new socket
     * @param auth    require the remote device to be authenticated
     * @param encrypt require the connection to be encrypted
     * @param device  remote device that this socket can connect to
     * @param port    remote port
     * @param uuid    SDP uuid
     * @throws IOException On error, for example Bluetooth not available, or
     *                     insufficient privileges
     */
    /*package*/ BluetoothSocket(int type, int fd, boolean auth, boolean encrypt,
            BluetoothDevice device, int port, ParcelUuid uuid) throws IOException {
        if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
		Log.i(TAG_2, "[JSR82] Bluetooth Socket Constructor");
		Log.i(TAG_2, "[JSR82] type=" + type + " fd=" +  fd + " auth=" + auth + " encrypt=" + encrypt + " port=" + port);
        	if (type == BluetoothSocket.TYPE_RFCOMM && uuid == null && fd == -1) {
            	if (port < 0) {
                	throw new IOException("Invalid RFCOMM channel: " + port);
            	}
        	}
        	if (uuid == null) {
            	mPort = port;
            	mSdp = null;
        	} else {
            	mSdp = new SdpHelper(device, uuid);
            	mPort = -1;
        	}
        	mType = type;
        	mAuth = auth;
        	mEncrypt = encrypt;
        	mDevice = device;
        	if (device == null) {
            	mAddress = null;
        	} else {
            	mAddress = device.getAddress();
        	}

        	if (mService == null) {
	        	IBinder binder = ServiceManager.getService(BluetoothSocketService.BLUETOOTH_SOCKET_SERVICE);
	        	if (binder != null) 	{
	            	mService = IBluetoothSocket.Stub.asInterface(binder);
	        	} else {
		     	Log.i(TAG_2, "[JSR82] Bluetooth Socket service not available!");
	            	mService = null;
	        	}
        	}

        	if( mService == null ){

        		throw new IOException("[JSR82] BluetoothSocket: IBluetoothSocket is null");
        	}
        
        	if (fd == -1) {
            	try {
                	mFdHandle = mService.initSocket(type, auth, encrypt, port, new Binder());
                	if (-1 == mFdHandle)
                	{
                		throw new IOException("[JSR82] BluetoothSocket: initSocket() failed.");
                	}
            	} catch (RemoteException e) {
                	Log.e(TAG_2, "", e);
            	}
        	} else {
            	{
                	mFdHandle = fd;
            	}
        	}
        	mInputStream = new BluetoothInputStream(this);
        	mOutputStream = new BluetoothOutputStream(this);
        	mSocketState = SocketState.INIT;
        	mLock = new ReentrantReadWriteLock();
    	} else {
        	if (type == BluetoothSocket.TYPE_RFCOMM && uuid == null && fd == -1) {
        	    if (port < 1 || port > MAX_RFCOMM_CHANNEL) {
        	        throw new IOException("Invalid RFCOMM channel: " + port);
        	    }
        	}
        	if (uuid == null) {
        	    mPort = port;
        	    mSdp = null;
        	} else {
        	    mSdp = new SdpHelper(device, uuid);
        	    mPort = -1;
        	}
        	mType = type;
        	mAuth = auth;
        	mEncrypt = encrypt;
        	mDevice = device;
        	if (device == null) {
        	    mAddress = null;
        	} else {
        	    mAddress = device.getAddress();
        	}
        	if (fd == -1) {
        	    initSocketNative();
        	} else {
        	    initSocketFromFdNative(fd);
        	}
        	mInputStream = new BluetoothInputStream(this);
        	mOutputStream = new BluetoothOutputStream(this);
                mSocketState = SocketState.INIT;
        	mLock = new ReentrantReadWriteLock();
    	}
    }

    /**
     * Construct a BluetoothSocket from address. Used by native code.
     * @param type    type of socket
     * @param fd      fd to use for connected socket, or -1 for a new socket
     * @param auth    require the remote device to be authenticated
     * @param encrypt require the connection to be encrypted
     * @param address remote device that this socket can connect to
     * @param port    remote port
     * @throws IOException On error, for example Bluetooth not available, or
     *                     insufficient privileges
     */
    private BluetoothSocket(int type, int fd, boolean auth, boolean encrypt, String address,
            int port) throws IOException {
        this(type, fd, auth, encrypt, new BluetoothDevice(address), port, null);
        Log.i(TAG_2, "[JSR82] Constructor used by JNI.");
    }

    /** @hide */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Constructor called from {@link #CREATOR}
     */
    private BluetoothSocket(Parcel in) {
    	boolean bVarArray[] = new boolean[4];
	
			
    	Log.i(TAG_2, "[JSR82] Constructor_Parcel");
        mType = in.readInt();
        mDevice = BluetoothDevice.CREATOR.createFromParcel(in);
        mAddress = in.readString();
        in.readBooleanArray(bVarArray);
        mAuth = bVarArray[0];
        mEncrypt = bVarArray[1];
        mSocketState = bVarArray[2] ? SocketState.CLOSED : SocketState.CONNECTED;
        mPort = in.readInt();
        mInputStream = new BluetoothInputStream(this);
        mOutputStream = new BluetoothOutputStream(this);
        mSdp = null;
        mService = null;
        mLock = null;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<BluetoothSocket> CREATOR =
            new Parcelable.Creator<BluetoothSocket>() {
        public BluetoothSocket createFromParcel(Parcel in) {
            return new BluetoothSocket(in);
        }
        public BluetoothSocket[] newArray(int size) {
            return new BluetoothSocket[size];
        }
    };

    public void writeToParcel(Parcel out, int flags) {
        boolean closed = (mSocketState == SocketState.CLOSED);
        boolean bVarArray[] = {mAuth, mEncrypt, closed};
			
        out.writeInt(mType);
        mDevice.writeToParcel(out, flags);
        out.writeString(mAddress);
        out.writeBooleanArray(bVarArray);
        //mInputStream
        //mOutputStream
        //mSdp
        //mService
        out.writeInt(mPort);
        //mLock
    }

    /**
     * Attempt to connect to a remote device.
     * <p>This method will block until a connection is made or the connection
     * fails. If this method returns without an exception then this socket
     * is now connected.
     * <p>Creating new connections to
     * remote Bluetooth devices should not be attempted while device discovery
     * is in progress. Device discovery is a heavyweight procedure on the
     * Bluetooth adapter and will significantly slow a device connection.
     * Use {@link BluetoothAdapter#cancelDiscovery()} to cancel an ongoing
     * discovery. Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * {@link BluetoothAdapter#cancelDiscovery()} even if it
     * did not directly request a discovery, just to be sure.
     * <p>{@link #close} can be used to abort this call from another thread.
     * @throws IOException on error, for example connection failure
     */
    public void connect() throws IOException {
    	if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
		int connectResult = 0;

		mLock.readLock().lock();
		try {
		    if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");

            	    if (mSdp != null) {
                	Log.i(TAG_2, "[JSR82] connect: do SDP");
                	mPort = mSdp.doSdp();  // blocks
                	Log.i(TAG_2, "[JSR82] connect: do SDP done; mPort=" + mPort);
            	    }

            	    try {
                	if (1 > mPort)
                	{
					throw new IOException("[JSR82] connect: Invalid RFCOMM channel: " + mPort);
                	}
                	connectResult = mService.connect(mFdHandle, mAddress, mPort);  // blocks
					
                	if (-1 == connectResult)
                	{
                		throw new IOException("[JSR82] connect: Connection is not created (failed or aborted).");
                	}
            	    } catch (RemoteException e) {
                	Log.e(TAG_2, "", e);
            	    }

        	} finally {
            	    mLock.readLock().unlock();
        	}
      } else {
      		mLock.readLock().lock();
        	try {
            	if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");

            	if (mSdp != null) {
                	mPort = mSdp.doSdp();  // blocks
            	}

            	connectNative();  // blocks
                mSocketState = SocketState.CONNECTED;
        	} finally {
            	mLock.readLock().unlock();
        	}
      }
    }

    /**
     * Immediately close this socket, and release all associated resources.
     * <p>Causes blocked calls on this socket in other threads to immediately
     * throw an IOException.
     */
    public void close() throws IOException {
    	if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
        	int serviceResult = 0;

        	// abort blocking operations on the socket
        	Log.i(TAG_2, "[JSR82] close");
        	mLock.readLock().lock();
			Log.i(TAG_2, "[JSR82] readLock got.");
        	try {
            	if (mSocketState == SocketState.CLOSED) return;
            	if (mSdp != null) {
               		mSdp.cancel();
            	}
            	try {
                	serviceResult = mService.abort(mFdHandle);
                	if (-1 == serviceResult)
                	{
                		throw new IOException("[JSR82] close: abort() failed.");
                	}
            	} catch (RemoteException e) {
                	Log.e(TAG_2, "", e);
            	}
        	} finally {
            	mLock.readLock().unlock();
        	}

			Log.i(TAG_2, "[JSR82] Start to aquire writeLock.");
        	// all native calls are guaranteed to immediately return after
        	// abortNative(), so this lock should immediately acquire
        	mLock.writeLock().lock();
			Log.i(TAG_2, "[JSR82] writeLock got.");
        	try {
            	        try {
                	    serviceResult = mService.destroy(mFdHandle);
                            mSocketState = SocketState.CLOSED;
                	    if (-1 == serviceResult)
                	    {
                		throw new IOException("[JSR82] close: destroy() failed.");
                	    }
            	         } catch (RemoteException e) {
                	    Log.e(TAG_2, "", e);
            	         }
        	} finally {
            	mLock.writeLock().unlock();
        	}


	} else {
         	// abort blocking operations on the socket
        	mLock.readLock().lock();
        	try {
            	if (mSocketState == SocketState.CLOSED) return;
            	if (mSdp != null) {
                	mSdp.cancel();
            	}
            	abortNative();
        	} finally {
            	mLock.readLock().unlock();
        	}

        	// all native calls are guaranteed to immediately return after
        	// abortNative(), so this lock should immediately acquire
        	mLock.writeLock().lock();
        	try {
                     mSocketState = SocketState.CLOSED;
            	     destroyNative();
        	} finally {
            	mLock.writeLock().unlock();
        	}
        }
    }

    /**
     * Get the remote device this socket is connecting, or connected, to.
     * @return remote device
     */
    public BluetoothDevice getRemoteDevice() {
        return mDevice;
    }

    /**
     * Get the input stream associated with this socket.
     * <p>The input stream will be returned even if the socket is not yet
     * connected, but operations on that stream will throw IOException until
     * the associated socket is connected.
     * @return InputStream
     */
    public InputStream getInputStream() throws IOException {
        return mInputStream;
    }

    /**
     * Get the output stream associated with this socket.
     * <p>The output stream will be returned even if the socket is not yet
     * connected, but operations on that stream will throw IOException until
     * the associated socket is connected.
     * @return OutputStream
     */
    public OutputStream getOutputStream() throws IOException {
        return mOutputStream;
    }

    /**
     * Get the connection status of this socket, ie, whether there is an active connection with
     * remote device.
     * @return true if connected
     *         false if not connected
     */
    public boolean isConnected() {
        return (mSocketState == SocketState.CONNECTED);
    }

    /**
     * Currently returns unix errno instead of throwing IOException,
     * so that BluetoothAdapter can check the error code for EADDRINUSE
     */
    /*package*/ int bindListen() {
        if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
	    int serviceResult = 0;

	    mLock.readLock().lock();
	    try {
		if (mSocketState == SocketState.CLOSED) return EBADFD;
		Log.i(TAG_2, "[JSR82] bindListen");
		try {
		    serviceResult = mService.bindListen(mFdHandle);
		    if (-1 == serviceResult){
			Log.d(TAG_2, "[JSR82] bindListen: bindListen() failed.");
		    }
		    mPort = serviceResult;
		    return 0;
		} catch (RemoteException e) {
		    Log.e(TAG_2, "", e);
      	            return -1;
      	      	}
	    } finally {
		mLock.readLock().unlock();
      	    }
      	} else {
            mLock.readLock().lock();
	    try {
	        if (mSocketState == SocketState.CLOSED) return EBADFD;
		    return bindListenNative();
		} finally {
		    mLock.readLock().unlock();
		}
      	}
    }

    /*package*/ BluetoothSocket accept(int timeout) throws IOException {
        if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
	    BluetoothSocket bluetoothSocket = null;	 // This object is used to return to APP when connection is created.
	    int newFdHandle = 0;
	    String address;

	    Log.i(TAG_2, "[JSR82] accept().");
	    mLock.readLock().lock();
	    try {
		if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
		try {
		    newFdHandle = mService.accept(timeout, mFdHandle);
		    if (-1 != newFdHandle) {
                        mSocketState = SocketState.CONNECTED;
		    // Connection is created successfully.
		    Log.i(TAG_2, "[JSR82] accept: Connection is created successfully!");
		    address = mService.getAddr(mFdHandle);
		    if (address == null) {
			Log.d(TAG_2, "[JSR82] accept: BD_ADDR string is null.");
			throw new IOException("[JSR82] accept: getAddr() failed.");
		    }

		    bluetoothSocket = new BluetoothSocket(mType, mFdHandle, mAuth, mEncrypt, new BluetoothDevice(address), mPort/* channel */, null);
	            // The original BluetoothSocket object will use new FDHandle to listen for further connection
		    mFdHandle = newFdHandle;
		    return bluetoothSocket;
		    } else {
			Log.i(TAG_2, "[JSR82] accept: Connection is not created.");
			throw new IOException("[JSR82] accept: Connection is not created (failed or aborted).");
  	              	//return null;
  	            }
		} catch (RemoteException e) {
		    Log.e(TAG_2, "", e);
		    return null;
		}
	    } finally {
		    mLock.readLock().unlock();
	    }
	} else {
            mLock.readLock().lock();
            try {
                if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
                
                BluetoothSocket acceptedSocket = acceptNative(timeout);
                mSocketState = SocketState.CONNECTED;
                return acceptedSocket;
            } finally {
                mLock.readLock().unlock();
            }
  	}
    }
 
    /*package*/ int available() throws IOException {
    	 if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
    	    int serviceResult = 0;
    	
    	    mLock.readLock().lock();
    	    try {
    	        if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
    	        try {
    	            serviceResult = mService.available(mFdHandle);
    	            if (-1 == serviceResult)
    	            {
    	            	throw new IOException("[JSR82] available: available() failed.");
    	            }
    	            return serviceResult;
    	        } catch (RemoteException e) {
    	            Log.e(TAG_2, "", e);
    	            return -1;
    	        }
    	    } finally {
    	        mLock.readLock().unlock();
    	    }
    	  } else {
            mLock.readLock().lock();
            try {
                if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
                return availableNative();
            } finally {
                mLock.readLock().unlock();
            }
    	  }
    }
 
    /*package*/ int read(byte[] b, int offset, int length) throws IOException {
    	if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
	    int serviceResult = 0;    	

    	    mLock.readLock().lock();
	    try {
    	        if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
    	        try {
    	            serviceResult = mService.read(b, offset, length, mFdHandle);
    	            if (-1 == serviceResult)
    	            {
    	            	throw new IOException("[JSR82] read: read() failed.");
    	            }
    	            return serviceResult;
    	        } catch (RemoteException e) {
    	            Log.e(TAG_2, "", e);
    	            return 0;
    	        }
    	    } finally {
    	        mLock.readLock().unlock();
    	    }
    	} else {
    		mLock.readLock().lock();
			try {
				if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
				return readNative(b, offset, length);
			} finally {
				mLock.readLock().unlock();
       	 	}
    	} 
    }

    /*package*/ int write(byte[] b, int offset, int length) throws IOException {
        if(true == FeatureOption.MTK_BT_PROFILE_SPP) {  
	    int serviceResult = 0;

	    mLock.readLock().lock();
    	    try {
    	        if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
    	        try {
    	            serviceResult = mService.write(b, offset, length, mFdHandle);
    	            if (-1 == serviceResult)
    	            {
    	            	throw new IOException("[JSR82] write: write() failed.");
    	            }
    	            return serviceResult;
    	        } catch (RemoteException e) {
    	            Log.e(TAG_2, "", e);
    	            return 0;
    	        }
    	    } finally {
    	        mLock.readLock().unlock();
    	    }
    	  } else {
    	  	mLock.readLock().lock();
		try {
		    if (mSocketState == SocketState.CLOSED) throw new IOException("socket closed");
		        return writeNative(b, offset, length);
		} finally {
		    mLock.readLock().unlock();
		}
    	  }
    }

    /*package*/ void throwErrno(int errno) throws IOException {
        if(true == FeatureOption.MTK_BT_PROFILE_SPP) {  
	    	try {
	     	Log.i(TAG_2, "[JSR82] throwErrnoNative");
            	try {
                	// Exception could not be thrown across processes, so this exception will be got in BluetoothSocketService
                	mService.throwErrno(errno, mFdHandle);
                	throw new IOException("[JSR82] throwErrnoNative: errno=" + errno);
            	} catch (RemoteException e) {
                	Log.e(TAG_2, "", e);
                	return ;
           		}
        	} finally {
        	 	return ;
        	}
    	} else {
			throwErrnoNative(errno);
		}
    }



    /*
     * Get the channel number just registered with bindListen()
     */
    int getChannel()
    {
        if (mSocketState == SocketState.CLOSED) return -1;
        Log.i(TAG_2, "[JSR82] getChannel: " + mPort);
        return mPort;
    }

//////////////////////////////////////////////////////////////////////////////////
// Use AIDL interface to replace these APIs

    private native void initSocketNative() throws IOException;
    private native void initSocketFromFdNative(int fd) throws IOException;
    private native void connectNative() throws IOException;
    private native int bindListenNative();
    private native BluetoothSocket acceptNative(int timeout) throws IOException;
    private native int availableNative() throws IOException;
    private native int readNative(byte[] b, int offset, int length) throws IOException;
    private native int writeNative(byte[] b, int offset, int length) throws IOException;
    private native void abortNative() throws IOException;
    private native void destroyNative() throws IOException;
    /**
     * Throws an IOException for given posix errno. Done natively so we can
     * use strerr to convert to string error.
     */
    /*package*/ native void throwErrnoNative(int errno) throws IOException;
//////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper to perform blocking SDP lookup.
     */
    private static class SdpHelper extends IBluetoothCallback.Stub {
        private final IBluetooth service;
        private final ParcelUuid uuid;
        private final BluetoothDevice device;
        private int channel;
        private boolean canceled;
        public SdpHelper(BluetoothDevice device, ParcelUuid uuid) {
            service = BluetoothDevice.getService();
            this.device = device;
            this.uuid = uuid;
            canceled = false;
        }
        /**
         * Returns the RFCOMM channel for the UUID, or throws IOException
         * on failure.
         */
        public synchronized int doSdp() throws IOException {
            if (canceled) throw new IOException("Service discovery canceled");
            channel = -1;

            boolean inProgress = false;
            try {
                inProgress = service.fetchRemoteUuids(device.getAddress(), uuid, this);
            } catch (RemoteException e) {Log.e(TAG, "", e);}

            if (!inProgress) throw new IOException("Unable to start Service Discovery");

            try {
                /* 12 second timeout as a precaution - onRfcommChannelFound
                 * should always occur before the timeout */
                wait(12000);   // block

            } catch (InterruptedException e) {}

            if (canceled) throw new IOException("Service discovery canceled");
            if (channel < 1) throw new IOException("Service discovery failed");

            return channel;
        }
        /** Object cannot be re-used after calling cancel() */
        public synchronized void cancel() {
            if (!canceled) {
                canceled = true;
                channel = -1;
                notifyAll();  // unblock
            }
        }
        public synchronized void onRfcommChannelFound(int channel) {
            if (!canceled) {
            	if(true == FeatureOption.MTK_BT_PROFILE_SPP) {
                    Log.i(TAG_2, "[JSR82] SdpHelper::onRfcommChannelFound: channel=" + channel);
                }
                this.channel = channel;
                notifyAll();  // unblock
            }
        }

        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException  {

        }
    }
}
