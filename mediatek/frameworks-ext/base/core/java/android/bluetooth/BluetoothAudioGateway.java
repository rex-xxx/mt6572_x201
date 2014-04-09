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

/*****************************************************************************
*==========================================================
* 			HISTORY
* Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
*------------------------------------------------------------------------------
* $Revision: $
* $Modtime: $
* $Log: $
 *
 * 09 23 2010 sh.lai
 * [ALPS00021182] [Need Patch] [Volunteer Patch] Can not connect audio to headset when call is on hold.
 * Volunteer Patch ALPS00021182 : Can not connect audio to headset when call is on hold.
 * [Cause]  BluetoothAudiogateway.java always report remote SCO connection accepted abthough it is an outgoing SCO connection. 
 * [Solution]  Report SCO connected instead of SCO accepted when it is an outgoing SCO connection.
 *
 * 09 22 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integrate bluetooth code from //ALPS_SW_PERSONAL/sh.lai/10YW1040OF_CB/ into //ALPS_SW/TRUNK/ALPS/.
 *
 * 09 10 2010 sh.lai
 * NULL
 * 1. Fix CR ALPS00125139 : [Gemini][Call]During a Call ,unpair&disconnect the Bluetooth headset,there is a JE
 * 2. Format HFG debug log with prefix "[BT][HFG]".
 *
 * 08 20 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integrate BT solution into Android 2.2
 *
 * 08 17 2010 sh.lai
 * NULL
 * Integration change.
 *
 * 05 26 2010 yufeng.chu
 * [ALPS00007206][HFP, OBEX, OPP] Add $Log in source file 
 * .
*
*------------------------------------------------------------------------------
* Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
*==========================================================
****************************************************************************/

package android.bluetooth;

import java.lang.Thread;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
//import android.net.LocalSocket;

/**
 * Listens for incoming RFCOMM connection for the headset / handsfree service.
 *
 * TODO: Use the new generic BluetoothSocket class instead of this legacy code
 *
 * @hide
 */
public final class BluetoothAudioGateway /*extends HeadsetBase*/ {
    private static final String TAG = "BT Audio Gateway";
    private static final boolean DBG = true;

    private int mNativeData;
    static { classInitNative(); }

    /* in */
    private int mHandsfreeAgRfcommChannel = -1;
    private int mHeadsetAgRfcommChannel   = -1;

    /* out - written by native code */
    private String mConnectingHeadsetAddress;
    private int mConnectingHeadsetRfcommChannel; /* -1 when not connected */
    private int mConnectingHeadsetSocketFd;
    private String mConnectingHandsfreeAddress;
    private int mConnectingHandsfreeRfcommChannel; /* -1 when not connected */
    private int mConnectingHandsfreeSocketFd;
    /*  */
    private int mTimeoutRemainingMs; /* in/out */

    private final BluetoothAdapter mAdapter;

    /***********************************/
    /*           member for HeadsetBase            */
    /***********************************/
    private static int sAtInputCount = 0;  /* TODO: Consider not using a static variable */
    protected AtParser mAtParser;
    // keep the current active device
    private BluetoothDevice mHeadsetDevice;
    private int mDirection;
    private boolean mIsConnected;
    private boolean mIsStartEvent;
    private boolean mOutgoingSCO;
    private boolean mSCOConnected;
    WakeLock mAtWakeLock;
    private long mConnectTimestamp;
    

    // keep the AT cmd received before HandsFree starts to handle AT cmd
    private String[] mAtBuf = new String[5];
    private int mBufCount;

    public static final int DEFAULT_HF_AG_CHANNEL = 10;
    public static final int DEFAULT_HS_AG_CHANNEL = 11;

    /***********************************/
    /*      const member for HeadsetBase        */
    /***********************************/
    public static final int DIRECTION_INCOMING = 1;
    public static final int DIRECTION_OUTGOING = 2;
    /***********************************/
    /*      const member for events                 */
    /***********************************/
    /* Sometimes we have to wakeup listen thread to do something */
    public static final int EVENT_HF_STARTED = 1;   // handle pending AT CMD received before HF started
    public static final int EVENT_HF_SHUTDOWN = 2;   // trigger listen thread to leave
    
    
    public BluetoothAudioGateway(BluetoothAdapter adapter) {
        this(null, adapter, DEFAULT_HF_AG_CHANNEL, DEFAULT_HS_AG_CHANNEL);
        log("BluetoothAudioGateway(1)");        
    }

    public BluetoothAudioGateway(PowerManager pm, BluetoothAdapter adapter) {
        this(pm, adapter, DEFAULT_HF_AG_CHANNEL, DEFAULT_HS_AG_CHANNEL);
        log("BluetoothAudioGateway(1)");        
    }

    public BluetoothAudioGateway(PowerManager pm, BluetoothAdapter adapter, int handsfreeAgRfcommChannel,
                int headsetAgRfcommChannel) {
        log("BluetoothAudioGateway(2)");
        mAdapter = adapter;
        mHandsfreeAgRfcommChannel = handsfreeAgRfcommChannel;
        mHeadsetAgRfcommChannel = headsetAgRfcommChannel;
        /* SH : Create and keep the AtParser */
        mAtParser = new AtParser();
        /* SH : init mHeadsetDevice to null */
        mHeadsetDevice = null;
        /* SH : Init to incoming connection */
        mDirection = DIRECTION_INCOMING;
        mIsConnected = false;
        mIsStartEvent = false;
        mOutgoingSCO = false;
        mSCOConnected = false;
        mBufCount = 0;
        // For power management
        if ( pm != null ) {
            mAtWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioGateway");
            mAtWakeLock.setReferenceCounted(false);
        }
        initializeNativeDataNative();
    }

    private Thread mConnectThead;
    private volatile boolean mInterrupted;
    private static final int SELECT_WAIT_TIMEOUT = 1000;

    private Handler mCallback;

    public class IncomingConnectionInfo {
        public BluetoothAdapter mAdapter;
        public BluetoothDevice mRemoteDevice;
        public int mSocketFd;
        public int mRfcommChan;
        IncomingConnectionInfo(BluetoothAdapter adapter, BluetoothDevice remoteDevice,
                int socketFd, int rfcommChan) {
            mAdapter = adapter;
            mRemoteDevice = remoteDevice;
            mSocketFd = socketFd;
            mRfcommChan = rfcommChan;
        }
    }

    /* SH : Message ID */
    public static final int MSG_INCOMING_HEADSET_CONNECTION   = 100;
    public static final int MSG_INCOMING_HANDSFREE_CONNECTION = 101;
    public static final int RFCOMM_CONNECTED                                        = 1;
    public static final int RFCOMM_ERROR                                                 = 2;
    public static final int RFCOMM_DISCONNECTED                                  = 3;
    public static final int SCO_ACCEPTED                                                   = 4;
    public static final int SCO_CONNECTED                                                = 5;
    public static final int SCO_CLOSED                                                        = 6;
    /*
    private LocalSocketImpl impl;
    private LocalSocketAddress localAddress;
    */
    public synchronized boolean start(Handler callback) {
        log("[API] start");
        if (mConnectThead == null) {
            mCallback = callback;
            mConnectThead = new Thread(TAG) {
                    public void run() {
                        log("Audiogateway listening thread starting");
                        while (!mInterrupted) {
                            mConnectingHeadsetRfcommChannel = -1;
                            mConnectingHandsfreeRfcommChannel = -1;
                            if( waitForHandsfreeIndicationNative() == false ) {
                                        logInfo("select thread was interrupted (2), exiting");
                                        mInterrupted = true;
                                    }
                                }
                        if (DBG) log("Connect Thread finished");
                    }
                };

            if (setUpListeningSocketsNative() == false) {
                logErr("Could not set up listening socket, exiting");
                return false;
            }

            mInterrupted = false;
            mConnectThead.start();
        }

        return true;
    }

    public void stop() {
        synchronized (this){
            log("[API] stop");
            if (mConnectThead == null) {
                log("mConnectThead == null");
                return;
            }
            if (DBG) log("stopping Connect Thread");
            mInterrupted = true;
            setEventNative(EVENT_HF_SHUTDOWN);
            mConnectThead.interrupt();
        } // synchronized
        if (DBG) log("waiting for thread to terminate");
        try {
            mConnectThead.join();
        } catch (InterruptedException e) {
            logWarn( "Interrupted waiting for Connect Thread to join");
        }
        synchronized (this){
            mConnectThead = null;
            mCallback = null;        
            tearDownListeningSocketsNative();
        } // synchronized
    }

    protected void finalize() throws Throwable {
        try {
            cleanupNativeDataNative();
        } finally {
            super.finalize();
        }
    }

    /*******************************************************************/
    /*                           Replacement Interface of HeadsetBase                                   */
    /*******************************************************************/
    private synchronized void acquireWakeLock(WakeLock lock) {
        log("[API] acquireWakeLock");
        if (!lock.isHeld()) {
            lock.acquire();
        }
    }

    private synchronized void releaseWakeLock(WakeLock lock) {
        log("[API] releaseWakeLock");
        if (lock.isHeld()) {
            lock.release();
        }
    }    
    protected void handleInput(String input) {
        log("[API] handleInput("+input+")");
        if ( mAtWakeLock != null ) {
            acquireWakeLock(mAtWakeLock);
        }

        //synchronized(HeadsetBase.class) {
            if (sAtInputCount == Integer.MAX_VALUE) {
                sAtInputCount = 0;
            } else {
                sAtInputCount++;
            }
        //}

        AtCommandResult result = mAtParser.process(input);
        sendURC(result.toString());
        if ( mAtWakeLock != null ) {
            releaseWakeLock(mAtWakeLock);
        }
    }

    
    public synchronized void disconnect()
    {
        log("[API] disconnect");
        disconnectNative();
    }
    public synchronized int waitForAsyncConnect(BluetoothDevice device, int timeout_ms, int type)
    {
        String address;
        int ret;
        address = device.getAddress();
        log("[API] waitForAsyncConnect("+address+","+Integer.toString(type)+")");
        ret = waitForAsyncConnectNative(address, timeout_ms, type);
        log("waitForAsyncConnectNative returns "+String.valueOf(ret));
        if(ret > 0)
        {
            // Success
            mHeadsetDevice = device;
            mDirection = DIRECTION_OUTGOING;
        }
        return ret;
    }
    public void startEventThread()
    {
        log("[API] startEventThread");
        mIsStartEvent = true;
        setEventNative(EVENT_HF_STARTED);
    }
    public BluetoothDevice getRemoteDevice()
    {
        log("[API] getRemoteDevice");
        return mHeadsetDevice;
    }
    public AtParser getAtParser()
    {
        log("[API] getAtParser");
        return mAtParser;
    }
    public int getDirection()
    {
        log("[API] getDirection : dir="+Integer.toString(mDirection));
        return mDirection;
    }
    public boolean isConnected()
    {
        log("[API] isConnected : "+String.valueOf(mIsConnected));
        return mIsConnected;
    }
    public synchronized boolean sendURC(String urc)
    {
        log("[API] sendURC("+urc+")");
        if (urc.length() > 0) {
            return sendURCNative(urc);
        }
        return true;
    }

    public long getConnectTimestamp() {
        log("[API] getConnectTimestamp");
        return mConnectTimestamp;
    }
    
    public static int getAtInputCount() {
        log("[API] getAtInputCount : "+Integer.toString(sAtInputCount));
        return sAtInputCount;
    }    
    /*******************************************************************/
    /*                           Replacement Interface of ScoSocket                                       */
    /*******************************************************************/
    public synchronized boolean isSCOConnected()
    {
        return mSCOConnected;
    }
    public synchronized boolean accept()
    {
        log("[API] accept");
        return true;
        //return acceptNative();
    }
    public synchronized boolean connect()
    {
        log("[API] connect");
        if(connectNative() == true)
        {
            mOutgoingSCO = true;
            return true;
        }
        return false;
    }
    public synchronized void close()
    {
        log("[API] close");
        closeNative();
    }
    /*******************************************************************/
    /*                                         Additional Interface                                                  */
    /*******************************************************************/
    // 1 for success and -1 for failed
    public synchronized int acceptConnection()
    {
        log("[API] acceptConnection");
        return acceptConnectionNative();
    }
    public synchronized void rejectConnection()
    {
        log("[API] rejectConnection");
        rejectConnectionNative();
    }
    /*******************************************************************/
    /*                                     JAVA event callback                                                      */
    /*******************************************************************/    
    private void sendMsg(int msgID, Object obj)
    {
        Message msg;
        log("[API] sendMsg : msg_id="+Integer.toString(msgID));
        msg = mCallback.obtainMessage(msgID);
        if(obj != null)
        {
            msg.obj = obj;
        }
        msg.sendToTarget();
    }
    
    // callback when get incoming request from stack
    // type : 0 for unknown, 1 for headset, 2 for handsfree
    private synchronized void onConnectRequest(String address, int type)
    {
        log("[API] onConnectRequest("+address+","+Integer.toString(type)+")");
        mDirection = DIRECTION_INCOMING;
        mHeadsetDevice = mAdapter.getRemoteDevice(address);
        sendMsg(type==1?MSG_INCOMING_HEADSET_CONNECTION:MSG_INCOMING_HANDSFREE_CONNECTION,
                        new IncomingConnectionInfo(
                                mAdapter,
                                mHeadsetDevice,
                                type, // Use type to replace the socket fd
                                0 //Seems not used by HeadsetBase, assign 0
                                ));
    }
    /* When connected indication is received */
    /* address : BT address of remote */
    /* type : 0 for unknown, 1 for headset, 2 for handsfree */
    private synchronized void onConnected(String address, int type)
    {
        log("[API] onConnected("+address+","+Integer.toString(type)+")");
        // Keep the connected timestamp
        mConnectTimestamp = System.currentTimeMillis();
        mIsConnected = true;
        // Send RFCOMM_CONNECTED no matter it is outgoing or incoming
        sendMsg(RFCOMM_CONNECTED, null);
    }
    /* When sco is disconnected indication is received. It might be remote disconnect or connect failed */
    private synchronized void onDisconnected(String address, int type)
    {
        int dir = mDirection;
        log("[API] onDisconnected("+address+","+Integer.toString(type)+")"); 
        /* SH : Always keep mDirection as incoming except waitForAsyncConnect is called and return success */        
        mDirection = DIRECTION_INCOMING;
        mIsStartEvent = false;
        mBufCount = 0;
        mHeadsetDevice = null;
        if(mIsConnected)
        {
            mIsConnected = false;
            sendMsg(RFCOMM_DISCONNECTED, null);
        }
        else if (dir == DIRECTION_OUTGOING)
        {
            // Report connect failed
            sendMsg(RFCOMM_ERROR, null);
        }
        else
        {}        
    }
    /* When sco is connected indication is received */
    /* address : BT address of remote */
    /* type : 0 for unknown, 1 for headset, 2 for handsfree */
    private synchronized void onSCOConnected()
    {
        log("[API] onSCOConnected"); 
        mSCOConnected = true;
        if(mOutgoingSCO == true)
        {
            mOutgoingSCO = false;
            sendMsg(SCO_CONNECTED, mHeadsetDevice);
        }
        else
        {
            sendMsg(SCO_ACCEPTED, mHeadsetDevice);
        }
    }
    /* When sco is disconnected indication is received. It might be remote disconnect or connect failed */
    private synchronized void onSCODisconnected()
    {
        log("[API] onSCODisconnected"); 
        mOutgoingSCO = false;
        mSCOConnected = false;
        sendMsg(SCO_CLOSED,mHeadsetDevice);
    }
    /* When AT CMD packet is received */
    private void onPacketReceived(String atCmd)
    {
        log("[API] onPacketReceived("+atCmd+") : mIsStartEvent="+String.valueOf(mIsStartEvent));
        log("mBufCount="+String.valueOf(mBufCount));
        if( mIsStartEvent == false )
        {
            if(mBufCount < mAtBuf.length)
            {
                mAtBuf[mBufCount++] = atCmd;
            }
            else
            {
                log("[ERR] Run out of AtBuf capacity");
            }
        }
        else
        {
            handleInput(atCmd);
        }
    }

    private void onEvent(int event)
    {
        log("[API] onEvent("+String.valueOf(event)+")");
        switch(event)
        {
        case EVENT_HF_STARTED:
            // HF has started, handle pending AT cmd
            if(mBufCount > 0)
            {
                while(mBufCount > 0)
                {
                    handleInput(mAtBuf[--mBufCount]);
                }
            }
            break;
        case EVENT_HF_SHUTDOWN:
            //mInterrupted = true;
            // do nothing, only wakeup the listening thread
            break;
        default:
            log("[ERR] unknown event : "+String.valueOf(event));
            break;
        }
    }
    /*******************************************************************/
    /*                             Native function Interface                                                      */
    /*******************************************************************/
    private static native void classInitNative();
    private native void initializeNativeDataNative();
    private native void cleanupNativeDataNative();
    /* SH : replace waitForHandsfreeConnectNative with waitForHandsfreeIndication */
    //private native boolean waitForHandsfreeConnectNative(int timeoutMs);
    private native boolean waitForHandsfreeIndicationNative();
    /* SH : create the mailbox of HFP (bthfp) */
    private native boolean setUpListeningSocketsNative();
    /* SH : close the mailbox of HFP (bthfp) */
    private native void tearDownListeningSocketsNative();
    /***************************************************************/
    /* SH : native interface to replace the functionality provided by headsetbase */
    /***************************************************************/
    /* return immediatly and wait connected indication. Have to add timeout mechanism in 
                 BluetoothAudioGateway. */
    /* Returns 1 when an async connect is complete, 0 on timeout, and -1 on error. */              
    private native int waitForAsyncConnectNative(String address, int timeout_ms, int type);
    /* SH : Return immediatly and return RFCOMM_DISCONNECTED to BluetoothHeadsetService  */
    private native void disconnectNative();
    /* SH : Send an URC to headset */
    private native boolean sendURCNative(String urc);
    /***************************************************************/
    /* SH : native interface to replace the functionality provided by ScoSocket    */
    /***************************************************************/
    private native boolean connectNative();
    //private native boolean acceptNative();
    private native void closeNative();
    /*******************************************************************/
    /*                                   Additional Native Interface                                             */
    /*******************************************************************/
    private native int acceptConnectionNative();
    private native void rejectConnectionNative(); 
    private native void setEventNative(int evt);

    private static void log(String msg) {
        Log.d(TAG, "[BT][HFG]"+msg);
    }
    private static void logInfo(String msg) {
        Log.i(TAG, "[BT][HFG]"+msg);
    }
    private static void logWarn(String msg) {
        Log.w(TAG, "[BT][HFG]"+msg);
    }
    private static void logErr(String msg) {
        Log.e(TAG, "[BT][HFG]"+msg);
    }
}
