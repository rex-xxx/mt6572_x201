/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.phone;

import android.bluetooth.AtCommandHandler;
import android.bluetooth.AtCommandResult;
import android.bluetooth.AtParser;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
/* MTK Modified : Begin */
//import android.bluetooth.HeadsetBase;
import android.bluetooth.BluetoothAudioGateway;
/* MTK Modified : End */
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import com.mediatek.telephony.TelephonyManagerEx;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.CallManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import com.android.internal.telephony.CallStateException;

import android.provider.CallLog.Calls;
import java.util.HashMap;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.phone.vt.VTCallUtils;

/** M: GEMINI support @{ */
import com.android.internal.telephony.gemini.MTKCallManager;
/** @} */

///M: DAUL TALK SUPPORT @{ 
import com.mediatek.phone.DualTalkUtils;
/// @}



/**
 * Bluetooth headset manager for the Phone app.
 * @hide
 */
public class BluetoothHandsfree {
    private static final String TAG = "Bluetooth HS/HF";
    private static final boolean DBG = true;//(PhoneApp.DBG_LEVEL >= 1)
            //&& (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = true;//(PhoneApp.DBG_LEVEL >= 2);  // even more logging
    private static final boolean bPassPTS = false;

    public static final int TYPE_UNKNOWN           = 0;
    public static final int TYPE_HEADSET           = 1;
    public static final int TYPE_HANDSFREE         = 2;

    /** The singleton instance. */
    private static BluetoothHandsfree sInstance;

    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    /// M: UT placeholder declaration @{
    private final CallManager mCM;
    /// @}
    
    /** M: GEMINI support @{ */
    private final MTKCallManager mMCM;
    private static final String GEMINI_SIM_NUM = "persist.gemini.sim_num";
    /** @} */

    private BluetoothA2dp mA2dp;

    private BluetoothDevice mA2dpDevice;
    private int mA2dpState; ///M: STATE_DISCONNECTED(0), STATE_CONNECTING(1), STATE_CONNECTED(2), STATE_DISCONNECTING(3)
    /** M: A2DP state refactoring @{ */
    private int mA2dpPlayingState; ///M: STATE_PLAYING (10) / STATE_NOT_PLAYING (11)
    /** @} */
    private boolean mPendingAudioState;
    private int mAudioState;

    private ServiceState mServiceState;
        /* MTK Modified : Begin */
    //private HeadsetBase mHeadset;  // null when not connected
    /// M: UT placeholder declaration @{
    private BluetoothAudioGateway mHeadset;  // null when not connected
    /// @}
    private BluetoothDevice mConnectedAudioDevice;
    /* MTK Modified : End */
    private BluetoothHeadset mBluetoothHeadset;
    private int mHeadsetType;   // TYPE_UNKNOWN when not connected
    private boolean mAudioPossible;
    /* MTK Modified : Begin */
    //private BluetoothSocket mConnectedSco;

    //private IncomingScoAcceptThread mIncomingScoThread = null;
    //private ScoSocketConnectThread mConnectScoThread = null;
    //private SignalScoCloseThread mSignalScoCloseThread = null;
    /* mAudioConnected means the audioManager is set to SCO and SCO on/off intent is broadcasted */
    /* mAudioConnected is not implied the SCO is currently on or off */
    private boolean mAudioConnected = false;
    private boolean mSCOConnecting = false; //Indicate if SCO connecting is ongoing
    /* MTK Modified : End */

    /// M: UT placeholder declaration @{
    private AudioManager mAudioManager;
    /// @}
    private PowerManager mPowerManager;

    private boolean mPendingSco;  // waiting for a2dp sink to suspend before establishing SCO
    private boolean mA2dpSuspended;
    private boolean mUserWantsAudio;
    private WakeLock mStartCallWakeLock;  // held while waiting for the intent to start call
    private WakeLock mStartVoiceRecognitionWakeLock;  // held while waiting for voice recognition

    // AT command state
    private static final int GSM_MAX_CONNECTIONS = 6;  // Max connections allowed by GSM
    private static final int CDMA_MAX_CONNECTIONS = 2;  // Max connections allowed by CDMA

    private long mBgndEarliestConnectionTime = 0;
    private boolean mClip = false;  // Calling Line Information Presentation
    private boolean mIndicatorsEnabled = false;
    private boolean mCmee = false;  // Extended Error reporting
    private long[] mClccTimestamps; // Timestamps associated with each clcc index
    private boolean[] mClccUsed;     // Is this clcc index in use
    private boolean mWaitingForCallStart;
    private boolean mWaitingForVoiceRecognition;
    private boolean mWaitingForOutCallStart = false;
    // do not connect audio until service connection is established
    // for 3-way supported devices, this is after AT+CHLD
    // for non-3-way supported devices, this is after AT+CMER (see spec)
    private boolean mServiceConnectionEstablished;

    private final BluetoothPhoneState mBluetoothPhoneState;  // for CIND and CIEV updates
    private final BluetoothAtPhonebook mPhonebook;
    private PhoneConstants.State mPhoneState = PhoneConstants.State.IDLE;
    CdmaPhoneCallState.PhoneCallState mCdmaThreeWayCallState =
                                            CdmaPhoneCallState.PhoneCallState.IDLE;

    private DebugThread mDebugThread;
    private int mScoGain = Integer.MIN_VALUE;

    private static Intent sVoiceCommandIntent;

    // Audio parameters
    private static final String HEADSET_NREC = "bt_headset_nrec";
    private static final String HEADSET_NAME = "bt_headset_name";

    /// M:[ALPS00462855] @{
    private static final String[] IOT1_DEVICES = {"00:0C:55:E2:FC:9E"};
    /// @}

    private int mRemoteBrsf = 0;
    private int mLocalBrsf = 0;

    // CDMA specific flag used in context with BT devices having display capabilities
    // to show which Caller is active. This state might not be always true as in CDMA
    // networks if a caller drops off no update is provided to the Phone.
    // This flag is just used as a toggle to provide a update to the BT device to specify
    // which caller is active.
    private boolean mCdmaIsSecondCallActive = false;
    private boolean mCdmaCallsSwapped = false;

    /** M: BT HFP in Dual Talk @{ */
    private boolean mIsLimitDTCall = true;
    /** @} */

    /* Constants from Bluetooth Specification Hands-Free profile version 1.5 */
    private static final int BRSF_AG_THREE_WAY_CALLING = 1 << 0;
    private static final int BRSF_AG_EC_NR = 1 << 1;
    private static final int BRSF_AG_VOICE_RECOG = 1 << 2;
    private static final int BRSF_AG_IN_BAND_RING = 1 << 3;
    private static final int BRSF_AG_VOICE_TAG_NUMBE = 1 << 4;
    private static final int BRSF_AG_REJECT_CALL = 1 << 5;
    private static final int BRSF_AG_ENHANCED_CALL_STATUS = 1 <<  6;
    private static final int BRSF_AG_ENHANCED_CALL_CONTROL = 1 << 7;
    private static final int BRSF_AG_ENHANCED_ERR_RESULT_CODES = 1 << 8;

    private static final int BRSF_HF_EC_NR = 1 << 0;
    private static final int BRSF_HF_CW_THREE_WAY_CALLING = 1 << 1;
    private static final int BRSF_HF_CLIP = 1 << 2;
    private static final int BRSF_HF_VOICE_REG_ACT = 1 << 3;
    private static final int BRSF_HF_REMOTE_VOL_CONTROL = 1 << 4;
    private static final int BRSF_HF_ENHANCED_CALL_STATUS = 1 <<  5;
    private static final int BRSF_HF_ENHANCED_CALL_CONTROL = 1 << 6;

    // VirtualCall - true if Virtual Call is active, false otherwise
    private boolean mVirtualCallStarted = false;

    // Voice Recognition - true if Voice Recognition is active, false otherwise
    private boolean mVoiceRecognitionStarted;

    /// M: UT placeholder declaration @{
    private HandsfreeMessageHandler mHandler;
    /// @}

    /// M: To handle switch SCO error problem @{
    private boolean mIsProcAudioOff = false;
    private boolean mIsPendingAudioOn = false;
    /// @}

    public static String typeToString(int type) {
        switch (type) {
        case TYPE_UNKNOWN:
            return "unknown";
        case TYPE_HEADSET:
            return "headset";
        case TYPE_HANDSFREE:
            return "handsfree";
        }
        return null;
    }

    /** M: GEMINI support @{ */
    public static int getSimCount(){
        int simCount = 1;
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            //Default value is 2. Compatible with previous platform when this property does not exist.
            String value = SystemProperties.get(GEMINI_SIM_NUM, "2");
            simCount = Integer.parseInt(value);
        }else{
            return simCount; //For non-Gemini project, the SIM num is 1.
        }
        return simCount;
    }
    /** @} */

    /**
     * Initialize the singleton BluetoothHandsfree instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     * MTK Modified: init() is now called by BluetoothHeadsetService & HeadsetPhoneService
     */
    /* package */ static BluetoothHandsfree init(Context context, CallManager cm) {
        synchronized (BluetoothHandsfree.class) {
            if (sInstance == null) {
                sInstance = new BluetoothHandsfree(context, cm);
            } else {
            	/* MTK Modified Begin */
            	//Log.wtf(TAG, "init() called multiple times!  sInstance = " + sInstance);
            	Log.d(TAG, "init() called multiple times!  sInstance = " + sInstance);
            	/* MTK Modified Begin */
            }
            return sInstance;
        }
    }

    /// M: UT placeholder addedmethod BluetoothHandsfree

    /** Private constructor; @see init() */
    private BluetoothHandsfree(Context context, CallManager cm) {
        /// M: UT placeholder assignment @{
        mCM = cm;
        /// @}
        mMCM = MTKCallManager.getInstance();
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean bluetoothCapable = (mAdapter != null);
        mHeadset = null;
        mConnectedAudioDevice = null;
        mHeadsetType = TYPE_UNKNOWN; // nothing connected yet
        if (bluetoothCapable) {
            mAdapter.getProfileProxy(mContext, mProfileListener,
                                     BluetoothProfile.A2DP);
        }
        mA2dpState = BluetoothA2dp.STATE_DISCONNECTED;
        /** M: A2DP state refactoring @{ */
        mA2dpPlayingState = BluetoothA2dp.STATE_NOT_PLAYING;
        /** @} */
        mA2dpDevice = null;
        mA2dpSuspended = false;

        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mStartCallWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                       TAG + ":StartCall");
        mStartCallWakeLock.setReferenceCounted(false);
        mStartVoiceRecognitionWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                       TAG + ":VoiceRecognition");
        mStartVoiceRecognitionWakeLock.setReferenceCounted(false);

        mLocalBrsf = BRSF_AG_THREE_WAY_CALLING |
                     BRSF_AG_EC_NR |
                     BRSF_AG_REJECT_CALL |
                     BRSF_AG_ENHANCED_CALL_STATUS;

        if (sVoiceCommandIntent == null) {
            sVoiceCommandIntent = new Intent(Intent.ACTION_VOICE_COMMAND);
            sVoiceCommandIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (mContext.getPackageManager().resolveActivity(sVoiceCommandIntent, 0) != null &&
                BluetoothHeadset.isBluetoothVoiceDialingEnabled(mContext)) {
            mLocalBrsf |= BRSF_AG_VOICE_RECOG;
        }

        HandlerThread thread = new HandlerThread("BluetoothHandsfreeHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mHandler = new HandsfreeMessageHandler(looper);
        mPhonebook = new BluetoothAtPhonebook(mContext, this);        
        mBluetoothPhoneState = new BluetoothPhoneState();
        mUserWantsAudio = true;
        mVirtualCallStarted = false;
        mVoiceRecognitionStarted = false;
        /// M: UT placeholder assignment @{
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        /// @}
        cdmaSetSecondCallState(false);

        if (bluetoothCapable) {
            resetAtState();
        }

    }

    /**
     * A thread that runs in the background waiting for a Sco Server Socket to
     * accept a connection. Even after a connection has been accepted, the Sco Server
     * continues to listen for new connections.
     */
    //private class IncomingScoAcceptThread extends Thread{
    //    private final BluetoothServerSocket mIncomingServerSocket;
    //    private BluetoothSocket mIncomingSco;
    //    private boolean stopped = false;

    //    public IncomingScoAcceptThread() {
    //        BluetoothServerSocket serverSocket = null;
    //        try {
    //            serverSocket = BluetoothAdapter.listenUsingScoOn();
    //        } catch (IOException e) {
    //            Log.e(TAG, "Could not create BluetoothServerSocket");
    //            stopped = true;
    //        }
    //        mIncomingServerSocket = serverSocket;
    //    }

    //    @Override
    //    public void run() {
    //        while (!stopped) {
    //            try {
    //                mIncomingSco = mIncomingServerSocket.accept();
    //            } catch (IOException e) {
    //                Log.e(TAG, "BluetoothServerSocket could not accept connection");
    //            }

    //            if (mIncomingSco != null) {
    //                connectSco();
    //            }
    //        }
    //    }

    //    private void connectSco() {
    //        synchronized (BluetoothHandsfree.this) {
    //            if (!Thread.interrupted() && isHeadsetConnected() &&
    //                (mAudioPossible || allowAudioAnytime()) &&
    //                mConnectedSco == null) {
    //                Log.i(TAG, "Routing audio for incoming SCO connection");
    //                mConnectedSco = mIncomingSco;
    //                mAudioManager.setBluetoothScoOn(true);
    //                setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTED,
    //                    mHeadset.getRemoteDevice());

    //                if (mSignalScoCloseThread == null) {
    //                    mSignalScoCloseThread = new SignalScoCloseThread();
    //                    mSignalScoCloseThread.setName("SignalScoCloseThread");
    //                    mSignalScoCloseThread.start();
    //                }
    //            } else {
    //                Log.i(TAG, "Rejecting incoming SCO connection");
    //                try {
    //                    mIncomingSco.close();
    //                }catch (IOException e) {
    //                    Log.e(TAG, "Error when closing incoming Sco socket");
    //                }
    //                mIncomingSco = null;
    //            }
    //        }
    //    }

        // must be called with BluetoothHandsfree locked
    //    void shutdown() {
    //        try {
    //            mIncomingServerSocket.close();
    //        } catch (IOException e) {
    //            Log.w(TAG, "Error when closing server socket");
    //        }
    //        stopped = true;
    //        interrupt();
    //    }
    //}

    /**
     * A thread that runs in the background waiting for a Sco Socket to
     * connect.Once the socket is connected, this thread shall be
     * shutdown.
     */
    //private class ScoSocketConnectThread extends Thread{
    //    private BluetoothSocket mOutgoingSco;

    //    public ScoSocketConnectThread(BluetoothDevice device) {
    //        try {
    //            mOutgoingSco = device.createScoSocket();
    //        } catch (IOException e) {
    //            Log.w(TAG, "Could not create BluetoothSocket");
    //            failedScoConnect();
    //        }
    //    }

    //    @Override
    //    public void run() {
    //        try {
    //            mOutgoingSco.connect();
    //        }catch (IOException connectException) {
    //            Log.e(TAG, "BluetoothSocket could not connect");
    //            mOutgoingSco = null;
    //            failedScoConnect();
    //        }

    //        if (mOutgoingSco != null) {
    //            connectSco();
    //        }
    //    }

    //    private void connectSco() {
    //        synchronized (BluetoothHandsfree.this) {
    //            if (!Thread.interrupted() && isHeadsetConnected() && mConnectedSco == null) {
    //                if (VDBG) log("Routing audio for outgoing SCO conection");
    //                mConnectedSco = mOutgoingSco;
    //                mAudioManager.setBluetoothScoOn(true);

    //                setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTED,
    //                  mHeadset.getRemoteDevice());

    //                if (mSignalScoCloseThread == null) {
    //                    mSignalScoCloseThread = new SignalScoCloseThread();
    //                    mSignalScoCloseThread.setName("SignalScoCloseThread");
    //                    mSignalScoCloseThread.start();
    //                }
    //            } else {
    //                if (VDBG) log("Rejecting new connected outgoing SCO socket");
    //                try {
    //                    mOutgoingSco.close();
    //                }catch (IOException e) {
    //                    Log.e(TAG, "Error when closing Sco socket");
    //                }
    //                mOutgoingSco = null;
    //                failedScoConnect();
    //            }
    //        }
    //    }

    /// M: [ALPS00438629][ALPS00389674] @{
    private void failedScoConnect(BluetoothDevice device) {
        // Wait for couple of secs before sending AUDIO_STATE_DISCONNECTED,
        // since an incoming SCO connection can happen immediately with
        // certain headsets.
        Message msg = Message.obtain(mHandler, SCO_AUDIO_STATE);
        msg.obj = device;
        mHandler.sendMessageDelayed(msg, 2000);
    }
     /// @}

        // must be called with BluetoothHandsfree locked
    //    void shutdown() {
    //        closeConnectedSco();

            // sync with isInterrupted() check in failedScoConnect method
            // see explanation there
    //        synchronized (ScoSocketConnectThread.this) {
    //            interrupt();
    //        }
    //    }
    //}

    /*
     * Signals when a Sco connection has been closed
     */
    //private class SignalScoCloseThread extends Thread{
    //    private boolean stopped = false;

    //    @Override
    //    public void run() {
    //        while (!stopped) {
    //            BluetoothSocket connectedSco = null;
    //            synchronized (BluetoothHandsfree.this) {
    //                connectedSco = mConnectedSco;
    //            }
    //            if (connectedSco != null) {
    //                byte b[] = new byte[1];
    //                InputStream inStream = null;
    //                try {
    //                    inStream = connectedSco.getInputStream();
    //                } catch (IOException e) {}

    //                if (inStream != null) {
    //                    try {
                            // inStream.read is a blocking call that won't ever
                            // return anything, but will throw an exception if the
                            // connection is closed
    //                        int ret = inStream.read(b, 0, 1);
    //                    }catch (IOException connectException) {
                            // call a message to close this thread and turn off audio
                            // we can't call audioOff directly because then
                            // the thread would try to close itself
    //                        Message msg = Message.obtain(mHandler, SCO_CLOSED);
    //                        mHandler.sendMessage(msg);
    //                        break;
    //                    }
    //                }
    //            }
    //        }
    //    }

        // must be called with BluetoothHandsfree locked
    //    void shutdown() {
    //        stopped = true;
    //        closeConnectedSco();
    //        interrupt();
    //    }
    //}

    //private void connectScoThread(){
        // Sync with setting mConnectScoThread to null to assure the validity of
        // the condition
    //    synchronized (ScoSocketConnectThread.class) {
    //        if (mConnectScoThread == null) {
    //            BluetoothDevice device = mHeadset.getRemoteDevice();
    //            if (getAudioState(device) == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
    //                setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTING, device);
    //            }

    //            mConnectScoThread = new ScoSocketConnectThread(mHeadset.getRemoteDevice());
    //            mConnectScoThread.setName("HandsfreeScoSocketConnectThread");

    //            mConnectScoThread.start();
    //        }
    //    }
    //}

    //private void resetConnectScoThread() {
        // Sync with if (mConnectScoThread == null) check
    //    synchronized (ScoSocketConnectThread.class) {
    //        mConnectScoThread = null;
    //    }
    //}

    // must be called with BluetoothHandsfree locked
    private void closeConnectedSco() {
        if (/*mConnectedSco != null*/ mAudioConnected == true) {
            /*
            try {
                mConnectedSco.close();
            } catch (IOException e) {
                Log.e(TAG, "Error when closing Sco socket");
            }
            */
            BluetoothDevice device = null;
            log("closeConnectedSco : mAudioConnected="+String.valueOf(mAudioConnected));
            if(/*isAudioOn()*/ isHeadsetConnected()){
                log("headset is connected");
                device = mHeadset.getRemoteDevice();
                if(isAudioOn()){
                    mHeadset.close();
                   //M: [ALPS00558353] to prevent from switching audio path too fast
                    mIsProcAudioOff = true;                    
                }
            }else{
                device = mConnectedAudioDevice;
            }
            log("mAudioManager.setBluetoothScoOn(false)");
            mAudioManager.setBluetoothScoOn(false);
            if(device != null){
                setAudioState(BluetoothHeadset.STATE_AUDIO_DISCONNECTED, device);
            }else{
                logErr("audio device is null when close audio");
            }
            mAudioConnected = false;
        }
    }

    /* package */ synchronized void onBluetoothEnabled() {
        /* Bluez has a bug where it will always accept and then orphan
         * incoming SCO connections, regardless of whether we have a listening
         * SCO socket. So the best thing to do is always run a listening socket
         * while bluetooth is on so that at least we can disconnect it
         * immediately when we don't want it.
         */
        log("[API] onBluetoothEnabled");
        /* MTK Removed : Begin */
        /*
        if (mIncomingScoThread == null) {
            mIncomingScoThread = new IncomingScoAcceptThread();
            mIncomingScoThread.setName("incomingScoAcceptThread");
            mIncomingScoThread.start();
        }
        */
        /* MTK Removed : End */
    }

    /* package */ synchronized void onBluetoothDisabled() {
        // Close off the SCO sockets
        audioOff();
        //M:[ALPS00592016] since for the disabled case, the scoket will be disconnected soon, thus the disconnect indication will not be notified and we will always think that sco is disconnecting.
        // so let's reset the flag and consider the sco disconnection will definiitely be done after disable.
        mIsProcAudioOff = false;
        /* MTK Removed : Begin */
        /*
        if (mIncomingScoThread != null) {
            mIncomingScoThread.shutdown();
            mIncomingScoThread = null;
        }
        */
        /* MTK Removed : End */
    }

    private boolean isHeadsetConnected() {
        /// M: UT placeholder reference @{
        if (mHeadset == null || mHeadsetType == TYPE_UNKNOWN) {
            return false;
        }
        /// @}
        return mHeadset.isConnected();
    }
    /* MTK Modified : Begin */
    /* package */ synchronized void connectHeadset(/*HeadsetBase*/BluetoothAudioGateway headset, int headsetType) {
    /* MTK Modified : End */
        /// M: UT placeholder assignment @{
        mHeadset = headset;
        /// @}
        mHeadsetType = headsetType;
        if (mHeadsetType == TYPE_HEADSET) {
            initializeHeadsetAtParser();
        } else {
            initializeHandsfreeAtParser();
        }

        /* Prevent SCO connected / disconnected ind not received when HFP deactivating */
        mSCOConnecting = false;

        // Headset vendor-specific commands
        registerAllVendorSpecificCommands();

        headset.startEventThread();
        configAudioParameters();

        if (inDebug()) {
            startDebug();
        }

        if (isIncallAudio()) {
            audioOn();
        } else if ( mCM.getFirstActiveRingingCall().isRinging()) {
            // need to update HS with RING when single ringing call exist
            mBluetoothPhoneState.ring();
        }
    }

    /* returns true if there is some kind of in-call audio we may wish to route
     * bluetooth to */
    private boolean isIncallAudio() {
        Call.State state = mCM.getActiveFgCallState();

        //M: [ALPS00578169]
        log("isIncallAudio: state = "+ state);
        return (state == Call.State.ACTIVE || state == Call.State.ALERTING || state == Call.State.DIALING);
    }

    /* package */ synchronized void disconnectHeadset() {
        audioOff();

        // No need to check if isVirtualCallInProgress()
        // terminateScoUsingVirtualVoiceCall() does the check
        terminateScoUsingVirtualVoiceCall();

        mHeadsetType = TYPE_UNKNOWN;
        stopDebug();
        resetAtState();
    }

    /* package */ synchronized void resetAtState() {
        mClip = false;
        mIndicatorsEnabled = false;
        mServiceConnectionEstablished = false;
        mCmee = false;
        mClccTimestamps = new long[GSM_MAX_CONNECTIONS];
        mClccUsed = new boolean[GSM_MAX_CONNECTIONS];
        for (int i = 0; i < GSM_MAX_CONNECTIONS; i++) {
            mClccUsed[i] = false;
        }
        mRemoteBrsf = 0;
        mPhonebook.resetAtState();
    }

    /* package */ /*HeadsetBase*/ BluetoothAudioGateway getHeadset() {
        /// M: UT placeholder reference @{
        return mHeadset;
        /// @}
    }

    private void configAudioParameters() {
        String name = mHeadset.getRemoteDevice().getName();
        if (name == null) {
            name = "<unknown>";
        }
        mAudioManager.setParameters(HEADSET_NAME+"="+name+";"+HEADSET_NREC+"=on");
    }


    /** Represents the data that we send in a +CIND or +CIEV command to the HF
     */
    private class BluetoothPhoneState {
        // 0: no service
        // 1: service
        private int mService;

        // 0: no active call
        // 1: active call (where active means audio is routed - not held call)
        private int mCall;

        // 0: not in call setup
        // 1: incoming call setup
        // 2: outgoing call setup
        // 3: remote party being alerted in an outgoing call setup
        private int mCallsetup;

        // 0: no calls held
        // 1: held call and active call
        // 2: held call only
        private int mCallheld;

        // cellular signal strength of AG: 0-5
        private int mSignal;

        // cellular signal strength in CSQ rssi scale
        private int mRssi;  // for CSQ

        // 0: roaming not active (home)
        // 1: roaming active
        private int mRoam;

        // battery charge of AG: 0-5
        private int mBattchg;

        // 0: not registered
        // 1: registered, home network
        // 5: registered, roaming
        private int mStat;  // for CREG

        private String mRingingNumber;  // Context for in-progress RING's
        private int    mRingingType;
        private boolean mIgnoreRing = false;
        private boolean mStopRing = false;

        /** M: Modified for [ALPS00451276]
         ** Deal with the "hanging up Active call & picking up Waiting call" scenario @{ */
        private HashMap<Call, Call.State> mCallStates = new HashMap<Call, Call.State>();
        /** @}*/
        
        // current or last call start timestamp
        private long mCallStartTime = 0;
        // time window to reconnect remotely-disconnected SCO
        // in mili-seconds
        private static final int RETRY_SCO_TIME_WINDOW = 1000;

        private static final int SERVICE_STATE_CHANGED = 1;
        private static final int PRECISE_CALL_STATE_CHANGED = 2;
        private static final int RING = 3;
        private static final int PHONE_CDMA_CALL_WAITING = 4;

        /** M: [ALPS00448197] 
         ** M: [Rose][JB2][Free Test][Call]The voice of playing song isn't transfered via Bluetooth headset @{ */
        private static final int PHONE_DISCONNECT = 6; 
        /** @}*/
        
        // SH : Merge Yufeng's modification
        private static final int PHONE_INCOMING_RING = 7;
        ///M: [ALPS00440217] For CDMA Ringing @{
        private static final int PHONE_INCOMING_CDMA_RING = 9;
        /// @}

        /** M: VT Call related messages */
        private static final int PHONE_VT_RING_INFO = 13;
        /** @}*/

        ///M: DAUL TALK SUPPORT @{ 
        private DualTalkUtils mDualTalk;
        /// @}
        
        private Handler mStateChangeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                log("mStateChangeHandler.handleMessage(): msg = " + msg.what);

                switch(msg.what) {
                // SH : Merge Yufeng's modification
                case PHONE_INCOMING_RING:
                case RING:
                case PHONE_VT_RING_INFO:
                    AtCommandResult result = ring();
                    if (result != null) {
                        sendURC(result.toString());
                    }
                    break;
                ///M: [ALPS00440217] For CDMA Ringing @{ 
                case PHONE_INCOMING_CDMA_RING:
                    log("CDMA phone call, make the ring");
                    AtCommandResult cdmaResult = cdmaRing();
                    if (cdmaResult != null) {
                        sendURC(cdmaResult.toString());
                    }
                    break;
                /// @}
                case SERVICE_STATE_CHANGED:
                    /** M: GEMINI support @{ */
                    if (FeatureOption.MTK_GEMINI_SUPPORT == true) {

                        AsyncResult asResult = (AsyncResult)msg.obj;
                        int iSimId = (Integer)(asResult.userObj);
                        if (mPhonebook.getDefaultSIM() == iSimId) {
                            ServiceState state = (ServiceState) asResult.result;
                            updateServiceState(sendUpdate(), state);
                        }                        
                    }
                    /** @} */
                    else {
                        ServiceState state = (ServiceState) ((AsyncResult) msg.obj).result;
                        updateServiceState(sendUpdate(), state);
                    }                                      
                    break;                  
                case PRECISE_CALL_STATE_CHANGED:
                case PHONE_CDMA_CALL_WAITING:
                case PHONE_DISCONNECT:
                    {
                        Connection connection = null;
                        if (((AsyncResult) msg.obj).result instanceof Connection) {
                            connection = (Connection) ((AsyncResult) msg.obj).result;
                        }
                        handlePreciseCallStateChange(sendUpdate(), connection);
                        break;
                    }
                }
            }
        };

        /// M: UT placeholder addedmethod BluetoothPhoneState

        private BluetoothPhoneState() {
            // init members
            // TODO May consider to repalce the default phone's state and signal
            //      by CallManagter's state and signal
            updateServiceState(false, mCM.getDefaultPhone().getServiceState());
            handlePreciseCallStateChange(false, null);
            mBattchg = 5;  // There is currently no API to get battery level
                           // on demand, so set to 5 and wait for an update
            mSignal = asuToSignal(mCM.getDefaultPhone().getSignalStrength());

            ///M: DAUL TALK SUPPORT @{ 
            if (DualTalkUtils.isSupportDualTalk() && mDualTalk == null) {
                mDualTalk = DualTalkUtils.getInstance();
                if(mDualTalk != null)
                {
                    if (VDBG) log("get mDualTalk instance succeed");
                }
                else 
                {
                    if (VDBG) logErr("get mDualTalk instance failed");
                }
            }
            /// @}

            // register for updates
            registerPhoneEvents(true);
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED);
            filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);
            mContext.registerReceiver(mStateReceiver, filter);
        }

        /** M: Modified for [ALPS00451276]
         ** Deal with the "hanging up Active call & picking up Waiting call" scenario @{ */
        private void setPrevCallState(Call callObj)
        {
            Call.State state = null;
            
            if(callObj == null)
                return;
            
            state = callObj.getState();
            
            log("setPrevCallState: Call=" + callObj);
            mCallStates.put(callObj, callObj.getState());
        }
        
        private Call.State getPrevCallState(Call callObj)
        {
            Call.State state = mCallStates.get(callObj);
            
            log("getPrevCallState: PrevState=" + state + ", Current Call=" + callObj);
            return state;
        }
        /** @}*/
        
        private void updateBtPhoneStateAfterRadioTechnologyChange() {
            if(VDBG) log( "updateBtPhoneStateAfterRadioTechnologyChange...");

            registerPhoneEvents(false);
            //Register all events new to the new active phone
            registerPhoneEvents(true);
        }

        private boolean sendUpdate() {
            return isHeadsetConnected() && mHeadsetType == TYPE_HANDSFREE && mIndicatorsEnabled
                   && mServiceConnectionEstablished;
        }

        private boolean sendClipUpdate() {
            return isHeadsetConnected() && mHeadsetType == TYPE_HANDSFREE && mClip &&
                   mServiceConnectionEstablished;
        }

        private boolean sendRingUpdate() {
                boolean bIsHeadsetConnected = isHeadsetConnected();
                boolean bisRinging = mCM.getFirstActiveRingingCall().isRinging();

                log( "sendRingUpdate(): bIsHeadsetConnected = "+bIsHeadsetConnected
                                + ",mIgnoreRing = "+ mIgnoreRing
                                + ",mStopRing = "+ mStopRing
                                + ",bisRinging = "+bisRinging
                                + ",mHeadsetType = "+ mHeadsetType
                                + ",mServiceConnectionEstablished = "+ mServiceConnectionEstablished
                                );

            if (bIsHeadsetConnected && !mIgnoreRing && !mStopRing && bisRinging) {
                if (mHeadsetType == TYPE_HANDSFREE) {
                    return mServiceConnectionEstablished ? true : false;
                }
                return true;
            }
            return false;
        }

        private void stopRing() {
            mStopRing = true;
        }

        /* convert [0,31] ASU signal strength to the [0,5] expected by
         * bluetooth devices. Scale is similar to status bar policy
         */
        private int gsmAsuToSignal(SignalStrength signalStrength) {
            int asu = signalStrength.getGsmSignalStrength();
            if      (asu >= 16) return 5;
            else if (asu >= 8)  return 4;
            else if (asu >= 4)  return 3;
            else if (asu >= 2)  return 2;
            else if (asu >= 1)  return 1;
            else                return 0;
        }

        /**
         * Convert the cdma / evdo db levels to appropriate icon level.
         * The scale is similar to the one used in status bar policy.
         *
         * @param signalStrength
         * @return the icon level
         */
        private int cdmaDbmEcioToSignal(SignalStrength signalStrength) {
            int levelDbm = 0;
            int levelEcio = 0;
            int cdmaIconLevel = 0;
            int evdoIconLevel = 0;
            int cdmaDbm = signalStrength.getCdmaDbm();
            int cdmaEcio = signalStrength.getCdmaEcio();

            if (cdmaDbm >= -75) levelDbm = 4;
            else if (cdmaDbm >= -85) levelDbm = 3;
            else if (cdmaDbm >= -95) levelDbm = 2;
            else if (cdmaDbm >= -100) levelDbm = 1;
            else levelDbm = 0;

            // Ec/Io are in dB*10
            if (cdmaEcio >= -90) levelEcio = 4;
            else if (cdmaEcio >= -110) levelEcio = 3;
            else if (cdmaEcio >= -130) levelEcio = 2;
            else if (cdmaEcio >= -150) levelEcio = 1;
            else levelEcio = 0;

            cdmaIconLevel = (levelDbm < levelEcio) ? levelDbm : levelEcio;

            if (mServiceState != null &&
                  (mServiceState.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                   mServiceState.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_A)) {
                  int evdoEcio = signalStrength.getEvdoEcio();
                  int evdoSnr = signalStrength.getEvdoSnr();
                  int levelEvdoEcio = 0;
                  int levelEvdoSnr = 0;

                  // Ec/Io are in dB*10
                  if (evdoEcio >= -650) levelEvdoEcio = 4;
                  else if (evdoEcio >= -750) levelEvdoEcio = 3;
                  else if (evdoEcio >= -900) levelEvdoEcio = 2;
                  else if (evdoEcio >= -1050) levelEvdoEcio = 1;
                  else levelEvdoEcio = 0;

                  if (evdoSnr > 7) levelEvdoSnr = 4;
                  else if (evdoSnr > 5) levelEvdoSnr = 3;
                  else if (evdoSnr > 3) levelEvdoSnr = 2;
                  else if (evdoSnr > 1) levelEvdoSnr = 1;
                  else levelEvdoSnr = 0;

                  evdoIconLevel = (levelEvdoEcio < levelEvdoSnr) ? levelEvdoEcio : levelEvdoSnr;
            }
            // TODO(): There is a bug open regarding what should be sent.
            return (cdmaIconLevel > evdoIconLevel) ?  cdmaIconLevel : evdoIconLevel;

        }


        private int asuToSignal(SignalStrength signalStrength) {
            if (signalStrength.isGsm()) {
                return gsmAsuToSignal(signalStrength);
            } else {
                return cdmaDbmEcioToSignal(signalStrength);
            }
        }


        /* convert [0,5] signal strength to a rssi signal strength for CSQ
         * which is [0,31]. Despite the same scale, this is not the same value
         * as ASU.
         */
        private int signalToRssi(int signal) {
            // using C4A suggested values
            switch (signal) {
            case 0: return 0;
            case 1: return 4;
            case 2: return 8;
            case 3: return 13;
            case 4: return 19;
            case 5: return 31;
            }
            return 0;
        }


        private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String intentAction = intent.getAction();
                
                log("[API] mStateReceiver.onReceive("+intentAction+")");

                if (intentAction.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    Message msg = mHandler.obtainMessage(BATTERY_CHANGED, intent);
                    mHandler.sendMessage(msg);
                } else if (intentAction.equals(
                            TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED)) {
                    Message msg = mHandler.obtainMessage(SIGNAL_STRENGTH_CHANGED,
                                                                    intent);
                    mHandler.sendMessage(msg);
                } else if (intentAction.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                    
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                    int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // We are only concerned about Connected sinks to suspend and resume
                    // them. We can safely ignore SINK_STATE_CHANGE for other devices.
                    if (device == null || (mA2dpDevice != null && !device.equals(mA2dpDevice))) {
                        log("device="+device+", mA2dpDevice="+mA2dpDevice);
                        return;
                    }

                    synchronized (BluetoothHandsfree.this) {
                        mA2dpState = state;
                        log("A2DP ACTION_CONNECTION_STATE_CHANGED:" + oldState + "->" + state);
                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            /// M: A2DP refactoring. 
                            /// M: mA2dpPlayingState is set to STATE_NOT_PLAYING when A2DP is disconnected @{
                            mA2dpPlayingState = BluetoothA2dp.STATE_NOT_PLAYING;
                            /// @}
                            mA2dpDevice = null;
                        } else {
                            mA2dpDevice = device;
                        }
                        
                        ///M: A2DP is disconnected while we are waiting for A2DP to be suspended
                        if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                            if (mA2dpSuspended && mPendingSco) {
                                    /// M: A2DP state refactoring @{
                                    processPendingSCO();
                                    /// @}
                                    //connectScoThread();
                                    mPendingSco = false;
                            }
                            /** M: [[ALPS00445419]] 
                             ** M: DT][BlueTooth]The ringtone of BT headset is not normal
                             ** A2DP is disconnected, so mA2dpSuspended can be reset to false @{ */
                            mA2dpSuspended = false;
                            /** @} */
                        }
                    }
                } 
                /** M: A2DP state refactoring @{ */
                else if(intentAction.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)){
                    
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                    int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (device == null || (mA2dpDevice != null && !device.equals(mA2dpDevice))) {
                        log("device="+device+", mA2dpDevice="+mA2dpDevice);
                        return;
                    }
                    
                    synchronized (BluetoothHandsfree.this) {
                        mA2dpPlayingState = state;
                        log("A2DP ACTION_PLAYING_STATE_CHANGED:" + oldState + "->" + state);
                        
                        if (state == BluetoothA2dp.STATE_NOT_PLAYING) {
                            if (mA2dpSuspended && mPendingSco) {
                                    processPendingSCO();
                                    mPendingSco = false;
                            }
                        }
                        else if(mA2dpPlayingState == BluetoothA2dp.STATE_PLAYING)
                        {
                            /** M: [[ALPS00445419]] 
                             ** M: DT][BlueTooth]The ringtone of BT headset is not normal
                             ** M: For some BT headset, A2DP will enter STATE_PLAYING after STATE_CONNECTED
                             ** M: Pre-Condition: HFP SLC connected, Phone ringing  Event: A2DP starts to play
                             ** M: Process: Just Suspend A2DP @{ */
                        
                            if(isA2dpMultiProfile() && sendUpdate())
                            {
                                if ((mA2dpSuspended == false) && (mCall == 0) && (mCallsetup == 1)) //Phone is ringing
                                {
                                    if (DBG) log("suspending A2DP stream for incoming call [Pre-Condition: HFP SLC connected, Phone ringing, Event: A2DP starts to play]");
                                    mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
                                }
                            }
                            /** @}*/
                        }
                    }
                    
                } 
                /** @} */
                else if (intent.getAction().
                           equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY)) {
                    mPhonebook.handleAccessPermissionResult(intent);
                }
            }
        };
        
        /** M: A2DP state refactoring @{ */
        private void processPendingSCO()
        {
            mHandler.removeMessages(MESSAGE_CHECK_PENDING_SCO);
            if (DBG) log("A2DP suspended, completing SCO");
            if(mHeadset.connect() == true) {
                /// M: [ALPS00389674] @{
                BluetoothDevice remoteDevice = mHeadset.getRemoteDevice();
                log("onReceive: device="+remoteDevice+" state="+getAudioState(remoteDevice));
                if(BluetoothHeadset.STATE_AUDIO_DISCONNECTED == getAudioState(remoteDevice)){
                  log("onReceive: device="+remoteDevice+" from state= STATE_AUDIO_DISCONNECTED to state = STATE_AUDIO_CONNECTING");
                    setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTING, remoteDevice);
                    mSCOConnecting = true;
                }   
                /// @}
            }
        }
        /** @} */
        
        private synchronized void updateBatteryState(Intent intent) {
            int batteryLevel = intent.getIntExtra("level", -1);
            int scale = intent.getIntExtra("scale", -1);
            log("[API] updateBatteryState");
            if (batteryLevel == -1 || scale == -1) {
                return;  // ignore
            }
            batteryLevel = batteryLevel * 5 / scale;
            if (mBattchg != batteryLevel) {
                mBattchg = batteryLevel;
                if (sendUpdate()) {
                    sendURC("+CIEV: 7," + mBattchg);
                }
            }
        }

        private synchronized void updateSignalState(Intent intent) {
            // NOTE this function is called by the BroadcastReceiver mStateReceiver after intent
            // ACTION_SIGNAL_STRENGTH_CHANGED and by the DebugThread mDebugThread
            if (!isHeadsetConnected()) {
                return;
            }

            SignalStrength signalStrength = SignalStrength.newFromBundle(intent.getExtras());
            int signal;
            log("[API] updateSignalState");
            if (signalStrength != null) {
                signal = asuToSignal(signalStrength);
                mRssi = signalToRssi(signal);  // no unsolicited CSQ
                if (signal != mSignal) {
                    mSignal = signal;
                    if (sendUpdate()) {
                        sendURC("+CIEV: 5," + mSignal);
                    }
                }
            } else {
                logErr("Signal Strength null");
            }
        }

        private synchronized void updateServiceState(boolean sendUpdate, ServiceState state) {
            int service = state.getState() == ServiceState.STATE_IN_SERVICE ? 1 : 0;
            int roam = state.getRoaming() ? 1 : 0;
            int stat;
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            mServiceState = state;          
            log("[API] updateServiceState service=" + service + ", mService=" + mService +
                                          ", roam=" + roam + ", mRoam=" + mRoam);
            if (service == 0) {
                stat = 0;
            } else {
                stat = (roam == 1) ? 5 : 1;
            }

            if (service != mService) {
                mService = service;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 1," + mService);
                }
            }
            if (roam != mRoam) {
                mRoam = roam;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 6," + mRoam);
                }
            }
            if (stat != mStat) {
                mStat = stat;
                if (sendUpdate) {
                    result.addResponse(toCregString());
                }
            }

            sendURC(result.toString());
        }

        private synchronized void handlePreciseCallStateChange(boolean sendUpdate,
                Connection connection) {
            int call = 0;
            int callsetup = 0;
            int callheld = 0;
            int prevCallsetup = mCallsetup;
            int prevCall = mCall;
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            
            log("[handlePreciseCallStateChange]");
            
            /** M: BT HFP For More Info @{ */
            Call.State foregroundCallState;
            Call.State backgroundCallState;
            Call.State ringingCallState;
            /** @} */
            
            Call foregroundCall = mCM.getActiveFgCall();
            Call backgroundCall = mCM.getFirstActiveBgCall();
            Call ringingCall = mCM.getFirstActiveRingingCall();
            
            /** M: Get the list of all foreground calls (from all Phones) @{ */
            List<Call> fgCalls = mCM.getForegroundCalls();
            /** @} */
            
            /** M: BT HFP Phone State Info @{ */
            log(String.format("## handlePreciseCallStateChange() - mCallsetup = %d, mCall = %d, mCallheld = %d ##", 
                    mCallsetup, mCall, mCallheld));
            
            log(String.format("   >>>handlePreciseCallStateChange() - Connection info: %s",
                    connection));
            /** @} */

            /** M: BT HFP in Dual Talk @{ */
            if (sendUpdate && FeatureOption.MTK_DT_SUPPORT == true && mIsLimitDTCall) {
                restrictDualTalkStatus(false);
            }
            /** @} */

            // This function will get called when the Precise Call State
            // {@link Call.State} changes. Hence, we might get this update
            // even if the {@link Phone.state} is same as before.
            // Check for the same.

            PhoneConstants.State newState = mCM.getState();
            
            /** M: BT HFP Phone State Info @{ */
            log(String.format("   >>>handlePreciseCallStateChange() - PhoneState: %d -> %d", mPhoneState.ordinal(), newState.ordinal()));
            /** @} */
            
            if (newState != mPhoneState) {
                mPhoneState = newState;
                switch (mPhoneState) {
                case IDLE:
                    mUserWantsAudio = true;  // out of call - reset state
                    audioOff();
                    break;
                default:
                    callStarted();
                }
            }

            /* Check the call state of foreground/ringing/background call*/
            /** M: BT HFP For More Info @{ */
            foregroundCallState = foregroundCall.getState();
            switch(foregroundCallState) {
            /** @} */
            case ACTIVE:
                call = 1;
                mAudioPossible = true;
                break;
            case DIALING:
                callsetup = 2;
                mAudioPossible = true;
                // We also need to send a Call started indication
                // for cases where the 2nd MO was initiated was
                // from a *BT hands free* and is waiting for a
                // +BLND: OK response
                // There is a special case handling of the same case
                // for CDMA below
                if (mCM.getFgPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                    callStarted();
                }
                /** M: [ALPS00448138] [Rose][JB2][Free Test][Call]It will change to Bluetooth mode after the screen light(5/5).
                 ** Do not send audioOn request for duplicate DIALING state
                 * @{ */
                if(callsetup != mCallsetup)
                {
                    audioOn();
                }
                /** @} */
                break;
            case ALERTING:
                callsetup = 3;
                // Open the SCO channel for the outgoing call.
                mCallStartTime = System.currentTimeMillis();
                
                /** M: [ALPS00448138] [Rose][JB2][Free Test][Call]It will change to Bluetooth mode after the screen light(5/5).
                 ** For ALERTING state, there is no need to call audioOn()
                 ** We did it in DIALING state
                 * @{ */
                //audioOn();
                /** @} */
                
                mAudioPossible = true;
                break;
            case DISCONNECTING:
                // This is a transient state, we don't want to send
                // any AT commands during this state.
                call = mCall;
                callsetup = mCallsetup;
                callheld = mCallheld;
                break;
            default:
                mAudioPossible = false;
            }

            /** M: BT HFP For More Info @{ */
            ringingCallState = ringingCall.getState();
            switch(ringingCallState) {
            /** @} */
            case INCOMING:
            case WAITING:
                callsetup = 1;
                break;
            case DISCONNECTING:
                // This is a transient state, we don't want to send
                // any AT commands during this state.
                call = mCall;
                callsetup = mCallsetup;
                callheld = mCallheld;
                break;
            }

            /** M: BT HFP For More Info @{ */
            backgroundCallState = backgroundCall.getState();
            switch(backgroundCallState) {
            /** @} */
            case HOLDING:
                if (call == 1) {
                    callheld = 1;
                } else {
                    call = 1;
                    callheld = 2;
                }
                break;
            case DISCONNECTING:
                // This is a transient state, we don't want to send
                // any AT commands during this state.
                call = mCall;
                callsetup = mCallsetup;
                callheld = mCallheld;
                break;
            }

            /** M: BT HFP Phone State Info @{ */
            log(String.format("handlePreciseCallStateChange: Tag fgCall %d", foregroundCallState.ordinal()));
            log(String.format("handlePreciseCallStateChange: Tag bgCall %d", backgroundCallState.ordinal()));
            log(String.format("handlePreciseCallStateChange: Tag ringingCall %d", ringingCallState.ordinal()));
            /** @} */
            
            /** M: Modified for [ALPS00451276]
             ** Deal with the "hanging up Active call & picking up Waiting call" scenario
             ** We may miss the call disconnecting step. For example:
             **
             **   (Fg: Active      , Fg: Idle   |Ring: Waiting)
             ** ->(Fg: Disconnected, Fg: Active |Ring: Idle   )
             *
             **   (Fg: Active        |Ring: Waiting, X) 
             ** ->(Fg: Disconnecting |Ring: Waiting, X) 
             ** ->(Fg: Active        |Ring: Idle   , X)
             ** @{ */
            //call status is still "In Call" , but this may just be a disconnected call followed by a connected call
            if (call == 1 && mCall == call) {
                // Get the previous state of the current active call
                Call.State prevState = getPrevCallState(foregroundCall);

                // Check if the active foreground call has just been picked up
                // If yes, this must mean the previous Active foreground call has been disconnected

                // @ If there was a previous held call (mCallheld != 0), don't fake the previous "Not In Call" state
                // The call should be not be changed and still active (call = 1)                
                if ((mCallheld == 0) &&
                    (foregroundCallState == Call.State.ACTIVE) &&
                    (prevState == Call.State.IDLE || prevState == Call.State.DISCONNECTED || prevState == Call.State.DISCONNECTING)) {
                    mCall = 0; // fake the previous "Not In Call" state
                    if (sendUpdate) {
                        result.addResponse("+CIEV: 2," + mCall);
                    }
                }
            }
            /** @} */
            
            if (mCall != call) {
                if (call == 1) {
                    // This means that a call has transitioned from NOT ACTIVE to ACTIVE.
                    // Switch on audio.
                    mCallStartTime = System.currentTimeMillis();
                    
                    /** M: [ALPS00448138] [Rose][JB2][Free Test][Call]It will change to Bluetooth mode after the screen light(5/5).
                     ** For the outgoing call case (DIALING/ALERTING), there is no need to call audioOn() again
                     * @{ */
                    if(prevCallsetup != 2 && prevCallsetup != 3)
                    {
                        audioOn();
                    }
                    /** @} */
                }
                mCall = call;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 2," + mCall);
                    /// M:[ALPS00462855] @{
                    if(0 == mCall
                    && Call.State.INCOMING == ringingCallState){
                        if(isMatchIOT1List()){
                            log("append CIEV: 3,1 !");
                            result.addResponse("+CIEV: 3," + 1);
                }
            }
                    /// @}
                }
            }
            
            if (mCallsetup != callsetup) {
                mCallsetup = callsetup;
                if (sendUpdate) {
                    /** M: Modified to avoid unneeded +CIEV: 3, 0 @{ */
                    // If mCall = 0, send CIEV
                    //   mCall = 1, mCallsetup = 1, send CIEV after CCWA, if 3 way supported.
                    //   mCall = 1, mCallsetup = 0, send CIEV, if 3 way is supported
                    //   mCall = 1, mCallsetup = 2 / 3 -> send CIEV, if 3 way is supported

                    if (prevCall != 1 || (mCallsetup != 1 && (mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0))
                    {
                        result.addResponse("+CIEV: 3," + mCallsetup);
                    }
                    /** @} */
                }
                
                /** M: [ALPS00303068]
                 ** M: If sending RING without suspending A2DP, the ring tone in SE MW600 will be strange 
                 ** M: Pre-Condition: HFP SLC connected, Event: Phone ringing
                 ** M: Process: Suspend A2DP -> Send "+CIEV: 3,1" -> Send "RING" @{ */
                if(isA2dpMultiProfile() && isA2dpConnected() && sendUpdate)
                {
                    if ((mA2dpSuspended == false) && (mCall == 0) && (mCallsetup == 1)) //Phone ringing
                    {
                        if (DBG) log("suspending A2DP stream for incoming call [Pre-Condition: HFP SLC connected, Event: Phone ringing]");
                        mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);                                    
                    }
                }
                /** @} */
            }

            if (mCM.getDefaultPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            	PhoneGlobals app = PhoneGlobals.getInstance();
                if (app.cdmaPhoneCallState != null) {
                    CdmaPhoneCallState.PhoneCallState currCdmaThreeWayCallState =
                            app.cdmaPhoneCallState.getCurrentCallState();
                    CdmaPhoneCallState.PhoneCallState prevCdmaThreeWayCallState =
                        app.cdmaPhoneCallState.getPreviousCallState();

                    log("CDMA call state: " + currCdmaThreeWayCallState + " prev state:" +
                        prevCdmaThreeWayCallState);
                    callheld = getCdmaCallHeldStatus(currCdmaThreeWayCallState,
                                                     prevCdmaThreeWayCallState);

                    if (mCdmaThreeWayCallState != currCdmaThreeWayCallState) {
                        // In CDMA, the network does not provide any feedback
                        // to the phone when the 2nd MO call goes through the
                        // stages of DIALING > ALERTING -> ACTIVE we fake the
                        // sequence
                        if ((currCdmaThreeWayCallState ==
                                CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                                    && app.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                            mAudioPossible = true;
                            if (sendUpdate) {
                                if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
                                    result.addResponse("+CIEV: 3,2");
                                    // Mimic putting the call on hold
                                    result.addResponse("+CIEV: 4,1");
                                    mCallheld = callheld;
                                    result.addResponse("+CIEV: 3,3");
                                    result.addResponse("+CIEV: 3,0");
                                }
                            }
                            // We also need to send a Call started indication
                            // for cases where the 2nd MO was initiated was
                            // from a *BT hands free* and is waiting for a
                            // +BLND: OK response
                            callStarted();
                        }

                        // In CDMA, the network does not provide any feedback to
                        // the phone when a user merges a 3way call or swaps
                        // between two calls we need to send a CIEV response
                        // indicating that a call state got changed which should
                        // trigger a CLCC update request from the BT client.
                        if (currCdmaThreeWayCallState ==
                                CdmaPhoneCallState.PhoneCallState.CONF_CALL &&
                                prevCdmaThreeWayCallState ==
                                  CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                            mAudioPossible = true;
                            if (sendUpdate) {
                                if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
                                    result.addResponse("+CIEV: 2,1");
                                    result.addResponse("+CIEV: 3,0");
                                }
                            }
                        }
                    }
                    mCdmaThreeWayCallState = currCdmaThreeWayCallState;
                }
            }

            boolean callsSwitched;

            if (mCM.getDefaultPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA &&
                mCdmaThreeWayCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                callsSwitched = mCdmaCallsSwapped;
            } else {
                callsSwitched =
                    (callheld == 1 && ! (backgroundCall.getEarliestConnectTime() ==
                        mBgndEarliestConnectionTime));
                mBgndEarliestConnectionTime = backgroundCall.getEarliestConnectTime();
            }


            if (mCallheld != callheld || callsSwitched) {
                mCallheld = callheld;
                if (sendUpdate) {
                    result.addResponse("+CIEV: 4," + mCallheld);
                }
            }

            if (callsetup == 1 && callsetup != prevCallsetup) {
                // new incoming call
                String number = null;
                int type = 128;
                // find incoming phone number and type
                if (connection == null) {
                    connection = ringingCall.getEarliestConnection();
                    if (connection == null) {
                        logErr("Could not get a handle on Connection object for new " +
                              "incoming call");
                    }
                }
                if (connection != null) {
                    number = connection.getAddress();
                    if (number != null) {
                        type = PhoneNumberUtils.toaFromString(number);
                    }
                }
                if (number == null) {
                    number = "";
                }
                if ((call != 0 || callheld != 0) && sendUpdate) {
                    // call waiting
                    if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
                        result.addResponse("+CCWA: \"" + number + "\"," + type);
                        result.addResponse("+CIEV: 3," + callsetup);
                    }
                } else {
                    // regular new incoming call
                    mRingingNumber = number;
                    mRingingType = type;
                    mIgnoreRing = false;
                    mStopRing = false;

                    if ((mLocalBrsf & BRSF_AG_IN_BAND_RING) != 0x0) {
                        mCallStartTime = System.currentTimeMillis();
                        audioOn();
                    }
                    /// M: [ALPS00440217] For CDMA Ringing
                    /// This is a workaround for VIA CDMA not sending RIL_UNSOL_CALL_RING 
                    /// For DualTalk, this code may not work GSM(I) -> CDMA(I). The retrieved phone type may be GSM. @{
                    if(ringingCall != null)
                    { 
                        if (ringingCall.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                            AtCommandResult cdmaResult = cdmaRing();
                                if (cdmaResult != null) {
                                    result.addResult(cdmaRing()); //Add RING/CLIP command to result
                                }
                        }
                    }
                    /** @} */
                    // SH : Merge Yufeng's modification
                    //result.addResult(ring());
                    ///////////////////////////////////
                }
            }
            
            /** M: Save current call states for future use */
            //Currently, we just deal with foreground call
            
            for(Call callObj : fgCalls)
            {
                setPrevCallState(callObj);
            }
            /** @} */
            
            sendURC(result.toString());
            
            /** M: [ALPS00404593]
             ** M: If we resume A2DP before sending +CIEV:3,0 for call setup failure, Jabra STREET2 v2.9.0
             ** M: will reject A2DP start request
             ** M: Pre-Condition: A2DP is suspended  Event: Incoming call stops ringing
             ** M: Process: Send "+CIEV: 3,0" -> Resume A2DP @{ */
            if(prevCallsetup == 1)//Only when previous callsetup = 1
            {
                if (mA2dpSuspended && (mCall == 0) && (mCallsetup == 0))  //Incoming call stops ("+CIEV: 3,1" -> "+CIEV: 3,0")
                {
                    if (isA2dpMultiProfile())
                    {
                        if (DBG) log("resuming A2DP stream [Pre-Condition: A2DP is suspended  Event: Ringing stops]");
                        mA2dp.resumeSink(mA2dpDevice);
                    }
                    mA2dpSuspended = false;
                }   
            }
            /** @} */
            
            log("[[handlePreciseCallStateChange]]");
        }

        private int getCdmaCallHeldStatus(CdmaPhoneCallState.PhoneCallState currState,
                                  CdmaPhoneCallState.PhoneCallState prevState) {
            int callheld;
            log("[API] getCdmaCallHeldStatus");
            // Update the Call held information
            if (currState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                if (prevState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                    callheld = 0; //0: no calls held, as now *both* the caller are active
                } else {
                    callheld = 1; //1: held call and active call, as on answering a
                            // Call Waiting, one of the caller *is* put on hold
                }
            } else if (currState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                callheld = 1; //1: held call and active call, as on make a 3 Way Call
                        // the first caller *is* put on hold
            } else {
                callheld = 0; //0: no calls held as this is a SINGLE_ACTIVE call
            }
            return callheld;
        }


        private AtCommandResult ring() {
            log("[API] ring");
            if (sendRingUpdate()) {
                AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
                result.addResponse("RING");
                if (sendClipUpdate()) {
                    result.addResponse("+CLIP: \"" + mRingingNumber + "\"," + mRingingType);
                }
                // SH : Merge Yufeng's modification
                //Message msg = mStateChangeHandler.obtainMessage(RING);
                //mStateChangeHandler.sendMessageDelayed(msg, 3000);
                //////////////////////////////
                return result;
            }
            return null;
        }

        /// M: [ALPS00440217] For CDMA Ringing
        /// This is a workaround for VIA CDMA not sending RIL_UNSOL_CALL_RING @{
        private AtCommandResult cdmaRing() {
            log("[API] cdmaRing");
            if (sendRingUpdate()) {
                AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
                result.addResponse("RING");
                if (sendClipUpdate()) {
                    result.addResponse("+CLIP: \"" + mRingingNumber + "\"," + mRingingType);
                }

                Message msg = mStateChangeHandler.obtainMessage(PHONE_INCOMING_CDMA_RING);
                mStateChangeHandler.sendMessageDelayed(msg, 3000);
                return result;
            }
            return null;
        }
        /** @} */

        private synchronized String toCregString() {
            return new String("+CREG: 1," + mStat);
        }

        private synchronized void updateCallHeld() {
            if (mCallheld != 0) {
                mCallheld = 0;
                sendURC("+CIEV: 4,0");
            }
        }

        private synchronized AtCommandResult toCindResult() {
            AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
            int call, call_setup;

            // Handsfree carkits expect that +CIND is properly responded to.
            // Hence we ensure that a proper response is sent for the virtual call too.
            if (isVirtualCallInProgress()) {
                call = 1;
                call_setup = 0;
            } else {
                // regular phone call
                call = mCall;
                call_setup = mCallsetup;
            }
            
            /** M: [ALPS00366302]
             ** M: [DT][BlueTooth]The ringtone of BT headset is not normal 
             ** M: Pre-Condition: Phone ringing, Event: AT+CIND?
             ** M: Process: Suspend A2DP -> Send "+CIND" @{ */
            if(isA2dpMultiProfile() && isA2dpConnected())
            {
                if ((mA2dpSuspended == false) && (mCall == 0) && (mCallsetup == 1)) //Phone ringing
                {
                	if (DBG) log("suspending A2DP stream for incoming call [Pre-Condition: Phone ringing, Event: HFP reconnects, AT+CIND?]");
                	mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
                }
            }
            /** @}*/
          
            mSignal = asuToSignal(mCM.getDefaultPhone().getSignalStrength());
            String status = "+CIND: " + mService + "," + call + "," + call_setup + "," +
                            mCallheld + "," + mSignal + "," + mRoam + "," + mBattchg;
            /// M: ALPS00383650 @{
            if(0 == mService){
                status = "+CIND: " + mService + "," + call + "," + call_setup + "," +
                                mCallheld + "," + 0 + "," + mRoam + "," + mBattchg;
                log("[toCindResult] mService="+ mService +" mSignal="+ mSignal +" ["+status+"]");
            }
            /// @}                            
            result.addResponse(status);
            return result;
        }

        private synchronized AtCommandResult toCsqResult() {
            AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
            String status = "+CSQ: " + mRssi + ",99";
            result.addResponse(status);
            return result;
        }


        private synchronized AtCommandResult getCindTestResult() {
            return new AtCommandResult("+CIND: (\"service\",(0-1))," + "(\"call\",(0-1))," +
                        "(\"callsetup\",(0-3)),(\"callheld\",(0-2)),(\"signal\",(0-5))," +
                        "(\"roam\",(0-1)),(\"battchg\",(0-5))");
        }

        private synchronized void ignoreRing() {
            mCallsetup = 0;
            mIgnoreRing = true;
            if (sendUpdate()) {
                sendURC("+CIEV: 3," + mCallsetup);
            }
        }

        /* SH Added additional function */
        private void registerPhoneEvents(boolean register) {
            log("registerPhoneEvents(" + String.valueOf(register) + ")");
            if (register) {
                /** M: GEMINI support @{ */
                if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                    
                    int simNum = getSimCount();

                    for(int simID = PhoneConstants.GEMINI_SIM_1; simID<(PhoneConstants.GEMINI_SIM_1 + simNum); simID++)
                    {
                        mMCM.registerForServiceStateChangedGemini(mStateChangeHandler, SERVICE_STATE_CHANGED, simID, simID);
                        mMCM.registerForPreciseCallStateChangedGemini(mStateChangeHandler, PRECISE_CALL_STATE_CHANGED, null, simID);
                        mMCM.registerForIncomingRingGemini(mStateChangeHandler, PHONE_INCOMING_RING, null, simID);
                        mMCM.registerForCallWaitingGemini(mStateChangeHandler, PHONE_CDMA_CALL_WAITING, null, simID);

                    if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                            mMCM.registerForVtRingInfoGemini(mStateChangeHandler, PHONE_VT_RING_INFO, null, simID);
                    }
                    /** M: [ALPS00448197] 
                     ** M: [Rose][JB2][Free Test][Call]The voice of playing song isn't transfered via Bluetooth headset,
                     ** No any sound after the incoming call is ended by remote side(5/5).
                     **
                     ** In some case, PRECISE_CALL_STATE_CHANGED message will not be sent 
                     ** before Phone app call a certain API even the call is disconnected
                     ** We need to register PHONE_DISCONNECT message to get notified right when the call is disconnected @{ */
                         mMCM.registerForDisconnectGemini(mStateChangeHandler, PHONE_DISCONNECT, null, simID);
                    /** @}*/
                    }
                    
                }
                /** @{ */
                else {
                    mCM.registerForServiceStateChanged(mStateChangeHandler,SERVICE_STATE_CHANGED, null);
                    mCM.registerForPreciseCallStateChanged(mStateChangeHandler,PRECISE_CALL_STATE_CHANGED, null);
                    mCM.registerForIncomingRing(mStateChangeHandler,PHONE_INCOMING_RING, null);
                    mCM.registerForCallWaiting(mStateChangeHandler,PHONE_CDMA_CALL_WAITING, null);
                    if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                        mCM.registerForVtRingInfo(mStateChangeHandler,PHONE_VT_RING_INFO, null);
                    }
                    
                    /** M: [ALPS00448197] 
                     ** M: [Rose][JB2][Free Test][Call]The voice of playing song isn't transfered via Bluetooth headset,
                     ** No any sound after the incoming call is ended by remote side(5/5). @{ */
                    mCM.registerForDisconnect(mStateChangeHandler, PHONE_DISCONNECT, null);
                    /** @}*/
                }
                // ////////////////////////////////////////////////
            } else {
                // Unregister all events from the old obsolete phone
                /** M: GEMINI support @{ */
                if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                    
                    int simNum = getSimCount();
                    
                    for(int simID = PhoneConstants.GEMINI_SIM_1; simID<(PhoneConstants.GEMINI_SIM_1 + simNum); simID++)
                    {
                        mMCM.unregisterForServiceStateChangedGemini(mStateChangeHandler, simID);                    
                        mMCM.unregisterForPreciseCallStateChangedGemini(mStateChangeHandler, simID);
                        mMCM.unregisterForIncomingRingGemini(mStateChangeHandler, simID);
                        mMCM.unregisterForCallWaitingGemini(mStateChangeHandler, simID);

                    if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                            mMCM.unregisterForVtRingInfoGemini(mStateChangeHandler, simID);
                    }

                    /** M: [ALPS00448197] 
                     ** M: [Rose][JB2][Free Test][Call]The voice of playing song isn't transfered via Bluetooth headset,
                     ** No any sound after the incoming call is ended by remote side(5/5). @{ */
                        mMCM.unregisterForDisconnectGemini(mStateChangeHandler, simID);
                    /** @}*/
                    }
                }
                /** @{ */
                else {
                    mCM.unregisterForServiceStateChanged(mStateChangeHandler);
                    mCM.unregisterForPreciseCallStateChanged(mStateChangeHandler);
                    mCM.unregisterForIncomingRing(mStateChangeHandler);
                    mCM.unregisterForCallWaiting(mStateChangeHandler);
                    if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                        mCM.unregisterForVtRingInfo(mStateChangeHandler);
                    }

                    /** M: [ALPS00448197] 
                     ** M: [Rose][JB2][Free Test][Call]The voice of playing song isn't transfered via Bluetooth headset,
                     ** No any sound after the incoming call is ended by remote side(5/5). @{ */
                    mCM.unregisterForDisconnect(mStateChangeHandler);
                    /** @}*/
                }
            }
        }

        private void scoClosed() {
            // sync on mUserWantsAudio change
            synchronized(BluetoothHandsfree.this) {
                if (mUserWantsAudio &&
                    System.currentTimeMillis() - mCallStartTime < RETRY_SCO_TIME_WINDOW) {
                    Message msg = mHandler.obtainMessage(SCO_CONNECTION_CHECK);
                    mHandler.sendMessage(msg);
                }
            }
        }
        
        /** M: BT HFP in Dual Talk @{ */
        ///For now, we only check the call swapping from Phone app
        private boolean isCallSwapping()
        {
            return PhoneUtils.getPhoneSwapStatus();
        }
                
        private void restrictDualTalkStatus(boolean mIsNewHFPConnection) {
            if (VDBG) log("restrictDualTalkStatus(" + mIsNewHFPConnection + ")");
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            Call foregroundCall = mCM.getActiveFgCall();
            Call backgroundCall = mCM.getFirstActiveBgCall();            
            Call ringingCall = mCM.getFirstActiveRingingCall();
            List<Call> ringingCalls = mCM.getRingingCalls();
            List<Call> backgroundCalls = mCM.getBackgroundCalls();
            boolean hasOutgoingCall = false;
            boolean hasRingingCall = false;
            boolean hasHoldingCall = false;

            ///M: foreground call could only be O or A and O/A are conflict with each other, so check the foreground call with DIALING || ALERTING could make sure if there is outgoing call
            ///   so check the foreground call with DIALING || ALERTING could make sure if there is outgoing call
            if (foregroundCall.getState() == Call.State.DIALING || foregroundCall.getState() == Call.State.ALERTING) {
                hasOutgoingCall = true;
            }

            ///Make sure the ringing call is in INCOMING/WAITING states
            if (ringingCall.getState().isRinging()) {
                hasRingingCall = true;
            }

            if (backgroundCall.getState() == Call.State.HOLDING) {
                hasHoldingCall = true;
            }

            if (hasRingingCall) {
                if (hasOutgoingCall) { // M: check (I, O)
                    if (mCallsetup > 0 && !mIsNewHFPConnection) {
                        mCallsetup = 0;
                        result.addResponse("+CIEV: 3,0");
                        sendURC(result.toString());
                    }

                    try {
                        if (VDBG) log("hangup outgoing call when (I, O)");
                        mCM.hangupActiveCall(foregroundCall);  // M: hangup Outgoing call
                    } catch(CallStateException e) {
                        logErr("mCM.hangupActiveCall failed");
                    }
                    return;
                }

                for (Call rCall : ringingCalls) {	// M: check (I, I)
                    ///Make sure rCall is in INCOMING/WAITING states
                    if (rCall != ringingCall && rCall.getState().isRinging()) {
                        if (mIsNewHFPConnection == true) {

                            if(mDualTalk != null)
                            {
                                if (VDBG) log("hangup background incoming call when (I, I)");
                                PhoneUtils.hangupRingingCall(mDualTalk.getSecondActiveRingCall());  // M: hangup background-Incoming call    
                            }
                            else
                            {
                                if (VDBG) log("fail to hangup background incoming call when (I, I), mDualTalk == null");
                            }

                        } else {
                            if (VDBG) log("hangup new incoming call when (I, I)");

                            if(ringingCall.getEarliestConnection().getCreateTime() > rCall.getEarliestConnection().getCreateTime())
                            {
                                PhoneUtils.hangupRingingCall(ringingCall);  // M: hangup new-Incoming call
                            }
                            else
                            {
                                PhoneUtils.hangupRingingCall(rCall);  // M: hangup new-Incoming call
                            }
                      	}
                        return;
                    }
                }
            }

            if (hasHoldingCall) {
                for (Call hCall : backgroundCalls) {  // M: check (H, H)
                    if (hCall != backgroundCall && hCall.getState() == Call.State.HOLDING ) {

                        if (VDBG) log("hangup hold call which hold first when (H, H)");

                        /// [ALPS00513091] if Phone is swapping (A, H), don't restrict the transient state (H, H). It'll transit to (H, A) soon
                        if(isCallSwapping()) //Check if phone is swaaping call
                        {
                            if (VDBG) log("Don't restrict (H, H) now for call is swapping");
                            return;
                        }

                        if(backgroundCall.getEarliestConnection().getCreateTime() > hCall.getEarliestConnection().getCreateTime())
                        {
                            PhoneUtils.hangupHoldingCall(hCall);
                        }
                        else
                        {
                            PhoneUtils.hangupHoldingCall(backgroundCall);
                        }

                        return;
                    }
                }
            }
        }
        /** @} */
        
        /// M:[ALPS00462855] @{
        private boolean isMatchIOT1List(){
            boolean isMatch = false;
            if(null != mConnectedAudioDevice){
                String curMacAddr = mConnectedAudioDevice.getAddress();
                for(String macAddr : IOT1_DEVICES){
                    if(curMacAddr.equals(macAddr)){
                        isMatch = true;
                        break;
                    }
                }
            }
            return isMatch;
        }
        /// @}
    };

    //private static final int SCO_CLOSED = 3;
    private static final int CHECK_CALL_STARTED = 4;
    private static final int CHECK_VOICE_RECOGNITION_STARTED = 5;
    private static final int MESSAGE_CHECK_PENDING_SCO = 6;
    private static final int SCO_AUDIO_STATE = 7;
    private static final int SCO_CONNECTION_CHECK = 8;
    private static final int BATTERY_CHANGED = 9;
    private static final int SIGNAL_STRENGTH_CHANGED = 10;
    private static final int CHECK_OUT_CALL_STARTED = 11;

    /* MTK Added : Begin */
    public synchronized void handleSCOEvent(int evt, BluetoothDevice device)
    {
        log("[HANDLER] handleSCOEvent("+evt+")");
        mConnectedAudioDevice = device;
        switch (evt) {
        case BluetoothAudioGateway.SCO_ACCEPTED:
            //if (msg.arg1 == ScoSocket.STATE_CONNECTED) {
            if (isHeadsetConnected() && (mAudioPossible || allowAudioAnytime()) &&
                mAudioConnected == false
                /*mConnectedSco == null*/) {
                logInfo( "Routing audio for incoming SCO connection");
                //mConnectedSco = (ScoSocket)msg.obj;
                mAudioConnected = true;
                log("mAudioManager.setBluetoothScoOn(true)");
                mAudioManager.setBluetoothScoOn(true);
                setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTED,
                        /*mHeadset.getRemoteDevice()*/device);
            } else {
                logInfo( "Rejecting incoming SCO connection");
                //((ScoSocket)msg.obj).close();
                mHeadset.close();
                //M: [ALPS00558353] to prevent from switching audio path too fast
                mIsProcAudioOff = true;                
            }
            //} // else error trying to accept, try again
            /* SH comment :  if the SCO is connected, why it also start to listen */
            /*
            mIncomingSco = createScoSocket();
            mIncomingSco.accept();
            */
            break;
        case BluetoothAudioGateway.SCO_CONNECTED:
            if (/*msg.arg1 == ScoSocket.STATE_CONNECTED && */isHeadsetConnected() &&
                mAudioConnected == false
                /*mConnectedSco == null*/) {
                if (VDBG) log("Routing audio for outgoing SCO conection");
                //mConnectedSco = (ScoSocket)msg.obj;
                mAudioConnected = true;
                log("mAudioManager.setBluetoothScoOn(true)");
                mAudioManager.setBluetoothScoOn(true);
                setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTED,
                      /*mHeadset.getRemoteDevice()*/device);
            }// else if (msg.arg1 == ScoSocket.STATE_CONNECTED) {
                //if (VDBG) log("Rejecting new connected outgoing SCO socket");
                /* SH : Comment */
                /* If HeadsetBase is not connected (no HF or HS SLC), then close the SCO */
                //((ScoSocket)msg.obj).close();
                //mOutgoingSco.close();
                
            //}
            //mOutgoingSco = null;
            mSCOConnecting = false;
            break;
        case BluetoothAudioGateway.SCO_CLOSED:
            /// M: [ALPS00389674] @{
            log("handleSCOEvent: SCO_CLOSED mSCOConnecting="+mSCOConnecting);
            if(!mSCOConnecting){
                /** [ALPS00380073] Move A2DP resumeSink to the time receiving SCO_CLOSED
                 * if the interval between remove sco(+CIEV:2,0) and A2DP start is too slow, 
                 * Jabra STREET2 will response "BAD_STATE" to A2DP start
                 */            
                if (VDBG) log("case SCO_CLOSED, mA2dpState: " + mA2dpState + ", mA2dpSuspended: " + mA2dpSuspended);            
                if (mA2dpSuspended) {
                    if (isA2dpMultiProfile()) {
                        if (DBG) log("resuming A2DP stream [Pre-Condition: A2DP is suspended  Event: SCO Disconnected]");
                            mA2dp.resumeSink(mA2dpDevice);
                    }
                    mA2dpSuspended = false;
                }

                //M: [ALPS00558353] to prevent from switching audio path too fast
                doAudioOff();

                // notify mBluetoothPhoneState that the SCO channel has closed
                mBluetoothPhoneState.scoClosed();
                //} else if (mOutgoingSco == (ScoSocket)msg.obj) {
                //    mOutgoingSco = null;
                //} else if (mIncomingSco == (ScoSocket)msg.obj) {
                //    mIncomingSco = null;
                //}
                mConnectedAudioDevice = null;
                /// M: To handle switch SCO error problem @{
                mIsProcAudioOff = false;
                if (mIsPendingAudioOn == true) {
                    log("[API] process the pedding userWantsAudioOn when audio off is completed");
                    mIsPendingAudioOn = false;
                    audioOn();
                }
                /// @}
            }else{
                /// M: [ALPS00438629] In some cases, RFCOMM will be disconnected
                ///    right after SCO connection failed, and mHeadset in BluetoothAudioGateway may be null .
                ///    Therefore, we just use the current BluetoothDevice object passed by SCO_CLOSED @{
                failedScoConnect(device);
                /// @}
            }
            /// @}
            break;
        default:
            log("Unsupported SCO message "+evt);
            break;
        }
    }
    /* MTK Added : End */

    private final class HandsfreeMessageHandler extends Handler {
        private HandsfreeMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            log("[API] mHandler.handleMessage("+String.valueOf(msg.what)+")");
            switch (msg.what) {
            case CHECK_CALL_STARTED:
                synchronized (BluetoothHandsfree.this) {
                    // synchronized
                    // Protect test/change of mWaitingForCallStart
                    if (mWaitingForCallStart) {
                        mWaitingForCallStart = false;
                        logErr("Timeout waiting for call to start");
                        sendURC("ERROR");
                        if (mStartCallWakeLock.isHeld()) {
                            mStartCallWakeLock.release();
                        }
                    }
                }
                break;
            case CHECK_OUT_CALL_STARTED:
                mWaitingForOutCallStart = false;
                break;
            case CHECK_VOICE_RECOGNITION_STARTED:
                synchronized (BluetoothHandsfree.this) {
                    // synchronized
                    // Protect test/change of mWaitingForVoiceRecognition
                    if (mWaitingForVoiceRecognition) {
                        mWaitingForVoiceRecognition = false;
                        logErr("Timeout waiting for voice recognition to start");
                        sendURC("ERROR");
                    }
                }
                break;
            case MESSAGE_CHECK_PENDING_SCO:
                // synchronized
                // Protect test/change of mPendingSco
                synchronized (BluetoothHandsfree.this) {
                    if (mPendingSco && isA2dpMultiProfile()) {
                        logWarn("Timeout suspending A2DP for SCO (" +
                                "mA2dpState = " + mA2dpState + ", mA2dpPlayingState = "+ mA2dpPlayingState +
                                "). Starting SCO anyway");
                        if (mHeadset.connect() == true) {
                            /// M: [ALPS00389674] @{
                            BluetoothDevice device = mHeadset.getRemoteDevice();
                            log("handleMessage: device="+device+" state="+getAudioState(device));
                            if(BluetoothHeadset.STATE_AUDIO_DISCONNECTED == getAudioState(device)){
                         	      log("handleMessage: device="+device+" from state= STATE_AUDIO_DISCONNECTED to state = STATE_AUDIO_CONNECTING");
                                setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTING, device);
                                mSCOConnecting = true;
                            }
                            /// @}
                        }
                    }
                    mPendingSco = false;
                }
                break;
                case SCO_AUDIO_STATE:
                BluetoothDevice device = (BluetoothDevice) msg.obj;
                if (getAudioState(device) == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
                    setAudioState(BluetoothHeadset.STATE_AUDIO_DISCONNECTED, device);
                    /// M: [ALPS00389674] @{
                    mSCOConnecting = false;
                    /// @}
                }
                break;
            case SCO_CONNECTION_CHECK:
                synchronized (mBluetoothPhoneState) {
                    // synchronized on mCall change
                    if (mBluetoothPhoneState.mCall == 1) {
                        // Sometimes, the SCO channel is torn down by HF with no reason.
                        // Because we are still in active call, reconnect SCO.
                        // audioOn does nothing if the SCO is already on.
                        audioOn();
                    }
                }
                break;
            case BATTERY_CHANGED:
                mBluetoothPhoneState.updateBatteryState((Intent) msg.obj);
                break;
            case SIGNAL_STRENGTH_CHANGED:
                mBluetoothPhoneState.updateSignalState((Intent) msg.obj);
                break;
            }
        }
    }

    private synchronized void setAudioState(int state, BluetoothDevice device) {
        if (VDBG) log("setAudioState(" + state + ")");
        if (mBluetoothHeadset == null) {
            mAdapter.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET);
            mPendingAudioState = true;
            mAudioState = state;
            return;
        }
        mBluetoothHeadset.setAudioState(device, state);
    }

    private synchronized int getAudioState(BluetoothDevice device) {
        if (mBluetoothHeadset == null) return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
        return mBluetoothHeadset.getAudioState(device);
    }

    private BluetoothProfile.ServiceListener mProfileListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
                synchronized(BluetoothHandsfree.this) {
                    if (mPendingAudioState) {
                        mBluetoothHeadset.setAudioState(mHeadset.getRemoteDevice(), mAudioState);
                        mPendingAudioState = false;
                    }
                }
            } else if (profile == BluetoothProfile.A2DP) {
                mA2dp = (BluetoothA2dp) proxy;
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            } else if (profile == BluetoothProfile.A2DP) {
                mA2dp = null;
            }
        }
    };

    /*
     * Put the AT command, company ID, arguments, and device in an Intent and broadcast it.
     */
    private void broadcastVendorSpecificEventIntent(String command,
                                                    int companyId,
                                                    int commandType,
                                                    Object[] arguments,
                                                    BluetoothDevice device) {
        if (VDBG) log("broadcastVendorSpecificEventIntent(" + command + ")");
        Intent intent =
                new Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, command);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,
                        commandType);
        // assert: all elements of args are Serializable
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arguments);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);

        intent.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
            + "." + Integer.toString(companyId));

        mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
    }

    void updateBtHandsfreeAfterRadioTechnologyChange() {
        if (VDBG) Log.d(TAG, "updateBtHandsfreeAfterRadioTechnologyChange...");

        mBluetoothPhoneState.updateBtPhoneStateAfterRadioTechnologyChange();
    }

    /** Request to establish SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     * Returns false if the user has requested audio off, or if there
     * is some other immediate problem that will prevent BT audio.
     */
    /* package */ synchronized boolean audioOn() {
        if (VDBG) log("audioOn()");
        
        /** M: ALPS00448138 
         * To handle switch SCO error problem @{ **/
        mIsPendingAudioOn = false;
        if (mIsProcAudioOff == true) {
            log("[API] audioOn() called when audio off is in progress");
            mIsPendingAudioOn = true;
            return false;
        }
        /** @} */
        
        if (!isHeadsetConnected()) {
            if (DBG) log("audioOn(): headset is not connected!");
            return false;
        }
        if (mHeadsetType == TYPE_HANDSFREE && !mServiceConnectionEstablished) {
            if (DBG) log("audioOn(): service connection not yet established!");
            return false;
        }

        if(mAudioConnected == true) {
            if (DBG) log("audioOn(): audio is already connected");
            return true;
        }

        if (!mUserWantsAudio) {
            if (DBG) log("audioOn(): user requested no audio, ignoring");
            return false;
        }

        if(mSCOConnecting == true) {
            if (DBG) log("audioOn(): audio connection is on going");
            return true;
        }

        if (mPendingSco) {
            if (DBG) log("audioOn(): SCO already pending");
            return true;
        }

        //mA2dpSuspended = false;
        mPendingSco = false;
        if (mA2dpSuspended == false) {
            if (isA2dpMultiProfile() && mA2dpPlayingState == BluetoothA2dp.STATE_PLAYING) {
                if (DBG) log("suspending A2DP stream [Pre-Condition: A2DP streaming, Event: Turn SCO On]");
                /// M: BT HFP more info @{
                if (DBG) log("Current mA2dpPlayingState ="+mA2dpPlayingState+", waiting for A2DP streaming to stop");
                /// @}
                mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
                
                /** Use a timer to make sure if A2DP streaming does not stop in time, 
                 *  HFP will continue the SCO connection
                 */
                if (mA2dpSuspended) {
                    mPendingSco = true;
                    Message msg = mHandler.obtainMessage(MESSAGE_CHECK_PENDING_SCO);
                    mHandler.sendMessageDelayed(msg, 2000);
                } else {
                    logWarn("Could not suspend A2DP stream for SCO, going ahead with SCO");
                }
            }

            /// M: [ALPS00248346] force to suspend a2dp (Do not wait for A2DP state change) @{
            else if (isA2dpMultiProfile() && mA2dpPlayingState == BluetoothA2dp.STATE_NOT_PLAYING)
            {
                if (DBG) log("suspending A2DP stream [Pre-Condition: A2DP not streaming, Event: Turn SCO On]");
                /// M: BT HFP more info @{
                if (DBG) log("Current mA2dpPlayingState ="+mA2dpPlayingState+", A2DP streaming is already stopped");
                /// @}
                mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
            }
            /// @}
        }

        if (!mPendingSco) {
            //connectScoThread();
            if(mHeadset.connect()==true) {
                /// M: [ALPS00389674] @{
                BluetoothDevice device = mHeadset.getRemoteDevice();
                log("audioOn: device="+device+" state="+getAudioState(device));
                if(BluetoothHeadset.STATE_AUDIO_DISCONNECTED == getAudioState(device)){
                    log("audioOn: device="+device+" from state= STATE_AUDIO_DISCONNECTED to state = STATE_AUDIO_CONNECTING");
                    setAudioState(BluetoothHeadset.STATE_AUDIO_CONNECTING, device);
                    mSCOConnecting = true;
                }
                /// @}
                return true;
            }else {
                return false;
            }
        }

        return true;
    }

    /** Used to indicate the user requested BT audio on.
     *  This will establish SCO (BT audio), even if the user requested it off
     *  previously on this call.
     */
    /* package */ synchronized void userWantsAudioOn() {
        log("[API] userWantsAudioOn");
        mUserWantsAudio = true;

        audioOn();
    }
    /** Used to indicate the user requested BT audio off.
     *  This will prevent us from establishing BT audio again during this call
     *  if audioOn() is called.
     */
    /* package */ synchronized void userWantsAudioOff() {
        log("[API] userWantsAudioOff");
        mUserWantsAudio = false;

        audioOff();
    }

    /** Request to disconnect SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     */
    /* package */ synchronized void audioOff() {
        log("audioOff(): mPendingSco: " + mPendingSco +
            ", mA2dpState: " + mA2dpState +
            ", mA2dpSuspended: " + mA2dpSuspended);

        //M: [ALPS00558353] to prevent from switching audio path too fast @{
        mIsPendingAudioOn = false;
        if (mIsProcAudioOff == true) {
            log("[API] audio off is processing");
            return;
        }
        /// @}
        doAudioOff();
    }

    //M: [ALPS00558353] to prevent from switching audio path too fast @{
    /** Request to disconnect SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     */
    /* package */ synchronized void doAudioOff() {
        log("doAudioOff(): mPendingSco: " + mPendingSco +
            ", mA2dpState: " + mA2dpState +
            ", mA2dpSuspended: " + mA2dpSuspended);

        mPendingSco = false;
        mHandler.removeMessages(MESSAGE_CHECK_PENDING_SCO);

        closeConnectedSco();    // Should be closed already, but just in case
    }

    /* package */ boolean isAudioOn() {
        boolean ret;
        /// M: UT placeholder reference @{
        ret = (mHeadset != null && mHeadset.isSCOConnected() && mAudioConnected == true);
        //// @}
        log("[API] isAudioOn : "+String.valueOf(ret));
        return ret;
    }

    private boolean isA2dpMultiProfile() {
        boolean ret;
        /// M: UT placeholder reference @{
        ret = (mA2dp != null && mHeadset != null && mA2dpDevice != null
            /* Stop streaming no matter a2dp is the same device with HFP or not */
            /* && mA2dpDevice.equals(mHeadset.getRemoteDevice()) */
                 );
        /// @}
        log("[API] isA2dpMultiProfile : "+String.valueOf(ret));
        if(mA2dpDevice == null)
            log("mA2dpDevice is null");
        if(mA2dp == null)
            log("mA2dp is null");
        return ret;
    }

    /** M: BT HFP Refactoring @{ */
    private boolean isA2dpConnected() {
        log("isA2dpConnected(): mA2dpState = "+ mA2dpState);
        return (mA2dpState == BluetoothA2dp.STATE_CONNECTED);
    }
    /** @}*/
    
    /* package */ void ignoreRing() {
        log("[API] ignoreRing");
        mBluetoothPhoneState.ignoreRing();
    }

    private void sendURC(String urc) {
        log("sendURC: Tag sendURC " + urc);
        if (isHeadsetConnected() && urc.length() > 0) {
            mHeadset.sendURC(urc);
        }
    }

    /** helper to redial last dialled number */
    private AtCommandResult redial() {
        /* Modified for supporting VT call : BEGIN */
    	Object isVTCall = null;
    	HashMap<String, Object> rets = new HashMap<String, Object>();
        String number = mPhonebook.getLastDialledNumber(rets);
        /* Modified for supporting VT call : END */
        log("[API] redial : number="+number);

        // If it is dialing now. just return error
        if(mWaitingForOutCallStart == true || mBluetoothPhoneState.mCallsetup == 2 || mBluetoothPhoneState.mCallsetup == 3){
            log("mWaitingForOutCallStart="+String.valueOf(mWaitingForOutCallStart)+", mBluetoothPhoneState.mCallsetup="+mBluetoothPhoneState.mCallsetup);
            return new AtCommandResult(AtCommandResult.ERROR);
        }
        
        if (number == null) {
            // spec seems to suggest sending ERROR if we dont have a
            // number to redial
            if (VDBG) log("Bluetooth redial requested (+BLDN), but no previous " +
                  "outgoing calls found. Ignoring");
            return new AtCommandResult(AtCommandResult.ERROR);
        }
        // Outgoing call initiated by the handsfree device
        // Send terminateScoUsingVirtualVoiceCall
        terminateScoUsingVirtualVoiceCall();
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                Uri.fromParts(Constants.SCHEME_TEL, number, null));
        intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
        intent.putExtra(Constants.EXTRA_SLOT_ID, mPhonebook.getDefaultSIM());
        intent.putExtra(Constants.EXTRA_IS_FORBIDE_DIALOG, true);
        intent.putExtra(Constants.EXTRA_INTERNATIONAL_DIAL_OPTION, Constants.INTERNATIONAL_DIAL_OPTION_IGNORE);

        /* Modified for supporting VT call : BEGIN */
        isVTCall = rets.get(Calls.VTCALL);
        if(isVTCall != null){
            if( isVTCall instanceof Integer && ((Integer)isVTCall).intValue() == 1 ){
                intent.putExtra("is_vt_call", true);
                log("[VT] isVTCall=="+((Integer)isVTCall).intValue());
            }else{
                log("[VT] isVTCall is 0 or not Integer type");
            }
        }else{
            log("[VT] isVTCall == null");
        }
        /* Modified for supporting VT call : END */
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        // We do not immediately respond OK, wait until we get a phone state
        // update. If we return OK now and the handsfree immeidately requests
        // our phone state it will say we are not in call yet which confuses
        // some devices
        expectCallStart();
        return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing
    }

    /** Build the +CLCC result
     *  The complexity arises from the fact that we need to maintain the same
     *  CLCC index even as a call moves between states. */
    private synchronized AtCommandResult gsmGetClccResult() {
        // Collect all known connections
        Connection[] clccConnections = new Connection[GSM_MAX_CONNECTIONS];  // indexed by CLCC index
        LinkedList<Connection> newConnections = new LinkedList<Connection>();
        LinkedList<Connection> connections = new LinkedList<Connection>();

        Call foregroundCall = mCM.getActiveFgCall();
        Call backgroundCall = mCM.getFirstActiveBgCall();
        Call ringingCall = mCM.getFirstActiveRingingCall();

        if (ringingCall.getState().isAlive()) {
            connections.addAll(ringingCall.getConnections());
        }
        if (foregroundCall.getState().isAlive()) {
            connections.addAll(foregroundCall.getConnections());
        }
        if (backgroundCall.getState().isAlive()) {
            connections.addAll(backgroundCall.getConnections());
        }

        // Mark connections that we already known about
        boolean clccUsed[] = new boolean[GSM_MAX_CONNECTIONS];
        for (int i = 0; i < GSM_MAX_CONNECTIONS; i++) {
            clccUsed[i] = mClccUsed[i];
            mClccUsed[i] = false;
        }
        for (Connection c : connections) {
            boolean found = false;
            long timestamp = c.getCreateTime();
            for (int i = 0; i < GSM_MAX_CONNECTIONS; i++) {
                if (clccUsed[i] && timestamp == mClccTimestamps[i]) {
                    mClccUsed[i] = true;
                    found = true;
                    clccConnections[i] = c;
                    break;
                }
            }
            if (!found) {
                newConnections.add(c);
            }
        }

        // Find a CLCC index for new connections
        while (!newConnections.isEmpty()) {
            // Find lowest empty index
            int i = 0;
            while (mClccUsed[i]) i++;
            // Find earliest connection
            long earliestTimestamp = newConnections.get(0).getCreateTime();
            Connection earliestConnection = newConnections.get(0);
            for (int j = 0; j < newConnections.size(); j++) {
                long timestamp = newConnections.get(j).getCreateTime();
                if (timestamp < earliestTimestamp) {
                    earliestTimestamp = timestamp;
                    earliestConnection = newConnections.get(j);
                }
            }

            // update
            mClccUsed[i] = true;
            mClccTimestamps[i] = earliestTimestamp;
            clccConnections[i] = earliestConnection;
            newConnections.remove(earliestConnection);
        }

        // Build CLCC
        AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
        for (int i = 0; i < clccConnections.length; i++) {
            if (mClccUsed[i]) {
                String clccEntry = connectionToClccEntry(i, clccConnections[i]);
                if (clccEntry != null) {
                    result.addResponse(clccEntry);
                }
            }
        }

        return result;
    }

    /** Convert a Connection object into a single +CLCC result */
    private String connectionToClccEntry(int index, Connection c) {
        int state;
        log("[API] connectionToClccEntry("+String.valueOf(index)+")");
        switch (c.getState()) {
        case ACTIVE:
            state = 0;
            break;
        case HOLDING:
            state = 1;
            break;
        case DIALING:
            state = 2;
            break;
        case ALERTING:
            state = 3;
            break;
        case INCOMING:
            state = 4;
            break;
        case WAITING:
            state = 5;
            break;
        default:
            return null;  // bad state
        }

        int mpty = 0;
        Call call = c.getCall();
        if (call != null) {
            mpty = call.isMultiparty() ? 1 : 0;
        }

        int direction = c.isIncoming() ? 1 : 0;

        String number = c.getAddress();
        int type = -1;
        if (number != null) {
            type = PhoneNumberUtils.toaFromString(number);
        }

        String result = "+CLCC: " + (index + 1) + "," + direction + "," + state + ",0," + mpty;
        if (number != null) {
            result += ",\"" + number + "\"," + type;
        }
        return result;
    }

    /** Build the +CLCC result for CDMA
     *  The complexity arises from the fact that we need to maintain the same
     *  CLCC index even as a call moves between states. */
    private synchronized AtCommandResult cdmaGetClccResult() {
        // In CDMA at one time a user can have only two live/active connections
        Connection[] clccConnections = new Connection[CDMA_MAX_CONNECTIONS];// indexed by CLCC index
        Call foregroundCall = mCM.getActiveFgCall();
        Call ringingCall = mCM.getFirstActiveRingingCall();

        Call.State ringingCallState = ringingCall.getState();
        // If the Ringing Call state is INCOMING, that means this is the very first call
        // hence there should not be any Foreground Call
        if (ringingCallState == Call.State.INCOMING) {
            if (VDBG) log("Filling clccConnections[0] for INCOMING state");
            clccConnections[0] = ringingCall.getLatestConnection();
        } else if (foregroundCall.getState().isAlive()) {
            // Getting Foreground Call connection based on Call state
            if (ringingCall.isRinging()) {
                if (VDBG) log("Filling clccConnections[0] & [1] for CALL WAITING state");
                clccConnections[0] = foregroundCall.getEarliestConnection();
                clccConnections[1] = ringingCall.getLatestConnection();
            } else {
                if (foregroundCall.getConnections().size() <= 1) {
                    // Single call scenario
                    if (VDBG) log("Filling clccConnections[0] with ForgroundCall latest connection");
                    clccConnections[0] = foregroundCall.getLatestConnection();
                } else {
                    // Multiple Call scenario. This would be true for both
                    // CONF_CALL and THRWAY_ACTIVE state
                    if (VDBG) log("Filling clccConnections[0] & [1] with ForgroundCall connections");
                    clccConnections[0] = foregroundCall.getEarliestConnection();
                    clccConnections[1] = foregroundCall.getLatestConnection();
                }
            }
        }

        // Update the mCdmaIsSecondCallActive flag based on the Phone call state
        if (PhoneGlobals.getInstance().cdmaPhoneCallState.getCurrentCallState()
                == CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE) {
            cdmaSetSecondCallState(false);
        } else if (PhoneGlobals.getInstance().cdmaPhoneCallState.getCurrentCallState()
                == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
            cdmaSetSecondCallState(true);
        }

        // Build CLCC
        AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
        for (int i = 0; (i < clccConnections.length) && (clccConnections[i] != null); i++) {
            String clccEntry = cdmaConnectionToClccEntry(i, clccConnections[i]);
            if (clccEntry != null) {
                result.addResponse(clccEntry);
            }
        }

        return result;
    }

    /** Convert a Connection object into a single +CLCC result for CDMA phones */
    private String cdmaConnectionToClccEntry(int index, Connection c) {
        int state;
        PhoneGlobals app = PhoneGlobals.getInstance();
        CdmaPhoneCallState.PhoneCallState currCdmaCallState =
                app.cdmaPhoneCallState.getCurrentCallState();
        CdmaPhoneCallState.PhoneCallState prevCdmaCallState =
                app.cdmaPhoneCallState.getPreviousCallState();

        log("[API] cdmaConnectionToClccEntry : index="+String.valueOf(index));
        if ((prevCdmaCallState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                && (currCdmaCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL)) {
            // If the current state is reached after merging two calls
            // we set the state of all the connections as ACTIVE
            state = 0;
        } else {
            switch (c.getState()) {
            case ACTIVE:
                // For CDMA since both the connections are set as active by FW after accepting
                // a Call waiting or making a 3 way call, we need to set the state specifically
                // to ACTIVE/HOLDING based on the mCdmaIsSecondCallActive flag. This way the
                // CLCC result will allow BT devices to enable the swap or merge options
                if (index == 0) { // For the 1st active connection
                    state = mCdmaIsSecondCallActive ? 1 : 0;
                } else { // for the 2nd active connection
                    state = mCdmaIsSecondCallActive ? 0 : 1;
                }
                break;
            case HOLDING:
                state = 1;
                break;
            case DIALING:
                state = 2;
                break;
            case ALERTING:
                state = 3;
                break;
            case INCOMING:
                state = 4;
                break;
            case WAITING:
                state = 5;
                break;
            default:
                return null;  // bad state
            }
        }

        int mpty = 0;
        if (currCdmaCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
            if (prevCdmaCallState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                // If the current state is reached after merging two calls
                // we set the multiparty call true.
                mpty = 1;
            } else {
                // CALL_CONF state is not from merging two calls, but from
                // accepting the second call. In this case first will be on
                // hold in most cases but in some cases its already merged.
                // However, we will follow the common case and the test case
                // as per Bluetooth SIG PTS
                mpty = 0;
            }
        } else {
            mpty = 0;
        }

        int direction = c.isIncoming() ? 1 : 0;

        String number = c.getAddress();
        int type = -1;
        if (number != null) {
            type = PhoneNumberUtils.toaFromString(number);
        }

        String result = "+CLCC: " + (index + 1) + "," + direction + "," + state + ",0," + mpty;
        if (number != null) {
            result += ",\"" + number + "\"," + type;
        }
        return result;
    }

    /*
     * Register a vendor-specific command.
     * @param commandName the name of the command.  For example, if the expected
     * incoming command is <code>AT+FOO=bar,baz</code>, the value of this should be
     * <code>"+FOO"</code>.
     * @param companyId the Bluetooth SIG Company Identifier
     * @param parser the AtParser on which to register the command
     */
    private void registerVendorSpecificCommand(String commandName,
                                               int companyId,
                                               AtParser parser) {
        parser.register(commandName,
                        new VendorSpecificCommandHandler(commandName, companyId));
    }

    /*
     * Register all vendor-specific commands here.
     */
    private void registerAllVendorSpecificCommands() {
        AtParser parser = mHeadset.getAtParser();

        // Plantronics-specific headset events go here
        registerVendorSpecificCommand("+XEVENT",
                                      BluetoothAssignedNumbers.PLANTRONICS,
                                      parser);
    }

    /**
     * Register AT Command handlers to implement the Headset profile
     */
    private void initializeHeadsetAtParser() {
        if (VDBG) log("Registering Headset AT commands");
        AtParser parser = mHeadset.getAtParser();
        // Headsets usually only have one button, which is meant to cause the
        // HS to send us AT+CKPD=200 or AT+CKPD.
        parser.register("+CKPD", new AtCommandHandler() {
            private AtCommandResult headsetButtonPress() {
                if (mCM.getFirstActiveRingingCall().isRinging()) {
                    // Answer the call
                    mBluetoothPhoneState.stopRing();
                    sendURC("OK");
                    PhoneUtils.answerCall(mCM.getFirstActiveRingingCall());
                    // If in-band ring tone is supported, SCO connection will already
                    // be up and the following call will just return.
                    audioOn();
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);
                } else if (mCM.hasActiveFgCall()) {
                    if (!isAudioOn()) {
                        // Transfer audio from AG to HS
                        audioOn();
                    } else {
                        if (mHeadset.getDirection() == /*HeadsetBase*/BluetoothAudioGateway.DIRECTION_INCOMING &&
                          (System.currentTimeMillis() - mHeadset.getConnectTimestamp()) < 5000) {
                            // Headset made a recent ACL connection to us - and
                            // made a mandatory AT+CKPD request to connect
                            // audio which races with our automatic audio
                            // setup.  ignore
                        } else {
                            // Hang up the call
                            audioOff();
                            PhoneUtils.hangup(PhoneGlobals.getInstance().mCM);
                        }
                    }
                    return new AtCommandResult(AtCommandResult.OK);
                } else {
                    // No current call - redial last number
                    return redial();
                }
            }
            @Override
            public AtCommandResult handleActionCommand() {
                return headsetButtonPress();
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                return headsetButtonPress();
            }
        });
    }

    /**
     * Register AT Command handlers to implement the Handsfree profile
     */
    private void initializeHandsfreeAtParser() {
        if (VDBG) log("Registering Handsfree AT commands");
        AtParser parser = mHeadset.getAtParser();
        final Phone phone = mCM.getDefaultPhone();

        // Answer
        parser.register('A', new AtCommandHandler() {
            @Override
            public AtCommandResult handleBasicCommand(String args) {
                sendURC("OK");
                mBluetoothPhoneState.stopRing();
                if(FeatureOption.MTK_VT3G324M_SUPPORT == true &&
                    VTCallUtils.isVTRinging()) {
                    try{
                    	PhoneGlobals.getInstance().touchAnswerVTCall();
                    }catch(Exception ex){
                        logErr("Answer VT call cause exception : " + ex.toString());
                    }
                }else {
                    PhoneUtils.answerCall(mCM.getFirstActiveRingingCall());
                }
                return new AtCommandResult(AtCommandResult.UNSOLICITED);
            }
        });
        parser.register('D', new AtCommandHandler() {
            @Override
            public AtCommandResult handleBasicCommand(String args) {
                String phoneNumber = null;
                log("Handle ATD"+args);
                if(mWaitingForOutCallStart == true || mBluetoothPhoneState.mCallsetup == 2 || mBluetoothPhoneState.mCallsetup == 3){
                    log("mWaitingForOutCallStart="+String.valueOf(mWaitingForOutCallStart)+", mBluetoothPhoneState.mCallsetup="+mBluetoothPhoneState.mCallsetup);
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
                if (args.length() > 0) {
                        // Remove trailing ';'
                        if (args.charAt(args.length() - 1) == ';') {
                            args = args.substring(0, args.length() - 1);
                        }
                    if (args.charAt(0) == '>') {
                        args = args.substring(1);
                        log("ATD memory dial : "+args);
                        try{
                            phoneNumber = getPhoneNumberByIndex(Integer.parseInt(args), true);
                        } catch(NumberFormatException e )
                        {
                            log("[ERR] wrong memory index : "+args);
                        }
                    } else {
                        phoneNumber = args;
                    }
                    if(phoneNumber != null)
                    {
                        phoneNumber = phoneNumber.replaceAll("-", "");
                        log("phoneNumber = "+phoneNumber);
                        // Send terminateScoUsingVirtualVoiceCall
                        terminateScoUsingVirtualVoiceCall();

                        phoneNumber = PhoneNumberUtils.convertPreDial(phoneNumber);

                        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                Uri.fromParts(Constants.SCHEME_TEL, phoneNumber, null));
                        intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
                        intent.putExtra(Constants.EXTRA_SLOT_ID, mPhonebook.getDefaultSIM());  
                        intent.putExtra(Constants.EXTRA_IS_FORBIDE_DIALOG, true);
                        intent.putExtra(Constants.EXTRA_INTERNATIONAL_DIAL_OPTION, Constants.INTERNATIONAL_DIAL_OPTION_IGNORE);

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);

                        expectCallStart();
                        return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing
                    } else {
                        log("phoneNumber is null");
                    }
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
        });

        // Hang-up command
        parser.register("+CHUP", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                sendURC("OK");
                if (isVirtualCallInProgress()) {
                    terminateScoUsingVirtualVoiceCall();
                } else {
                    if (mCM.hasActiveFgCall()) {
                        /** M: Fix: PTS issues [ALPS00445352] [ALPS00432603]
                         **    Fix: [ALPS00369668][IOT][Bluetooth]Cannot answer the second call via Moto H375 begin
                         **    Determine how AT+CHUP will Deal with the 1A1W case @{ */

                        if(BluetoothHandsfree.bPassPTS){
                            /// M: If BT headset supports BRSF_HF_CW_THREE_WAY_CALLING, we should just hangup the Active call (1A1W) -> (X,1I)
                            if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) != 0x0) {
                                // For headset supporting BRSF_HF_CW_THREE_WAY_CALLING, don't to accept other calls after active call is hangedup
                                log("BRSF_HF_CW_THREE_WAY_CALLING is supported: (1A1W) -> (X,1I)");
                                try {
                                    Call fgCall = mCM.getActiveFgCall();
                                    if(fgCall != null){
                                        mCM.hangupActiveCall(fgCall);
                                    }else{
                                        logWarn("mCM.getActiveFgCall return null");
                                    }
                                }catch(CallStateException e){
                                    logErr("mCM.hangupActiveCall failed");
                                }      
                            /// M: Else, we should pick up Waiting call after hanging up Active call (1A1W) -> (X,1A)                      
                            } else {
                                log("BRSF_HF_CW_THREE_WAY_CALLING is Not supported: (1A1W) -> (X,1A) ");
                                PhoneUtils.hangupActiveCall(mCM.getActiveFgCall());
                            }
                        /** @} */
                       }else{
                            log("PhoneUtils.hangupActiveCall(mCM.getActiveFgCall())");
                            PhoneUtils.hangupActiveCall(mCM.getActiveFgCall());
                       }

                    } else if (mCM.hasActiveRingingCall()) {
                        PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
                    } else if (mCM.hasActiveBgCall()) {
                        PhoneUtils.hangupHoldingCall(mCM.getFirstActiveBgCall());
                    }
                }
                return new AtCommandResult(AtCommandResult.UNSOLICITED);
            }
        });

        // Bluetooth Retrieve Supported Features command
        parser.register("+BRSF", new AtCommandHandler() {
            private AtCommandResult sendBRSF() {
                log("sendBRSF : "+String.valueOf(mLocalBrsf));
                return new AtCommandResult("+BRSF: " + mLocalBrsf);
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                /** M: BT HFP in Dual Talk 
                 ** M: We do check here for new HFP connection @{ */
                if (FeatureOption.MTK_DT_SUPPORT == true && mIsLimitDTCall) {
                    mBluetoothPhoneState.restrictDualTalkStatus(true);
                }
                /** @} */

                // AT+BRSF=<handsfree supported features bitmap>
                // Handsfree is telling us which features it supports. We
                // send the features we support
                log("handleSetCommand : length="+String.valueOf(args.length)+", mrsf="+String.valueOf(args[0]));
                if (args.length == 1 && (args[0] instanceof Integer)) {
                    mRemoteBrsf = (Integer) args[0];
                } else {
                    logWarn("HF didn't sent BRSF assuming 0");
                }
                return sendBRSF();
            }
            @Override
            public AtCommandResult handleActionCommand() {
                // This seems to be out of spec, but lets do the nice thing
                log("handleActionCommand");
                return sendBRSF();
            }
            @Override
            public AtCommandResult handleReadCommand() {
                // This seems to be out of spec, but lets do the nice thing
                log("handleReadCommand");
                return sendBRSF();
            }
        });

        // Call waiting notification on/off
        parser.register("+CCWA", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Seems to be out of spec, but lets return nicely
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleReadCommand() {
                // Call waiting is always on
                return new AtCommandResult("+CCWA: 1");
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+CCWA=<n>
                // Handsfree is trying to enable/disable call waiting. We
                // cannot disable in the current implementation.
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                // Request for range of supported CCWA paramters
                return new AtCommandResult("+CCWA: (\"n\",(1))");
            }
        });

        // Mobile Equipment Event Reporting enable/disable command
        // Of the full 3GPP syntax paramters (mode, keyp, disp, ind, bfr) we
        // only support paramter ind (disable/enable evert reporting using
        // +CDEV)
        parser.register("+CMER", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult(
                        "+CMER: 3,0,0," + (mIndicatorsEnabled ? "1" : "0"));
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (args.length < 4) {
                    // This is a syntax error
                    return new AtCommandResult(AtCommandResult.ERROR);
                } else if (args[0].equals(3) && args[1].equals(0) &&
                           args[2].equals(0)) {
                    boolean valid = false;
                    if (args[3].equals(0)) {
                        mIndicatorsEnabled = false;
                        valid = true;
                    } else if (args[3].equals(1)) {
                        mIndicatorsEnabled = true;
                        valid = true;
                    }
                    if (valid) {
                        if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) == 0x0) {
                            mServiceConnectionEstablished = true;
                            sendURC("OK");  // send immediately, then initiate audio
                            if (isIncallAudio()) {
                                audioOn();
                            } else if (mCM.getFirstActiveRingingCall().isRinging()) {
                                // need to update HS with RING cmd when single
                                // ringing call exist
                                mBluetoothPhoneState.ring();
                            }
                            // only send OK once
                            return new AtCommandResult(AtCommandResult.UNSOLICITED);
                        } else {
                            return new AtCommandResult(AtCommandResult.OK);
                        }
                    }
                }
                return reportCmeError(BluetoothCmeError.OPERATION_NOT_SUPPORTED);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return new AtCommandResult("+CMER: (3),(0),(0),(0-1)");
            }
        });

        // Mobile Equipment Error Reporting enable/disable
        parser.register("+CMEE", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // out of spec, assume they want to enable
                mCmee = true;
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult("+CMEE: " + (mCmee ? "1" : "0"));
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+CMEE=<n>
                if (args.length == 0) {
                    // <n> ommitted - default to 0
                    mCmee = false;
                    return new AtCommandResult(AtCommandResult.OK);
                } else if (!(args[0] instanceof Integer)) {
                    // Syntax error
                    return new AtCommandResult(AtCommandResult.ERROR);
                } else {
                    mCmee = ((Integer)args[0] == 1);
                    return new AtCommandResult(AtCommandResult.OK);
                }
            }
            @Override
            public AtCommandResult handleTestCommand() {
                // Probably not required but spec, but no harm done
                return new AtCommandResult("+CMEE: (0-1)");
            }
        });

        // Bluetooth Last Dialled Number
        parser.register("+BLDN", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                return redial();
            }
        });

        // Indicator Update command
        parser.register("+CIND", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return mBluetoothPhoneState.toCindResult();
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return mBluetoothPhoneState.getCindTestResult();
            }
        });

        // Query Signal Quality (legacy)
        parser.register("+CSQ", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                return mBluetoothPhoneState.toCsqResult();
            }
        });

        // Query network registration state
        parser.register("+CREG", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult(mBluetoothPhoneState.toCregString());
            }
        });

        // Send DTMF. I don't know if we are also expected to play the DTMF tone
        // locally, right now we don't
        parser.register("+VTS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (args.length >= 1) {
                    char c;
                    if (args[0] instanceof Integer) {
                        c = ((Integer) args[0]).toString().charAt(0);
                    } else {
                        c = ((String) args[0]).charAt(0);
                    }
                    if (isValidDtmf(c)) {
                        mCM.sendDtmf(c);
                        return new AtCommandResult(AtCommandResult.OK);
                    }
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
            private boolean isValidDtmf(char c) {
                switch (c) {
                case '#':
                case '*':
                    return true;
                default:
                    if (Character.digit(c, 14) != -1) {
                        return true;  // 0-9 and A-D
                    }
                    return false;
                }
            }
        });

        // List calls
        parser.register("+CLCC", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                int phoneType = phone.getPhoneType();
                // Handsfree carkits expect that +CLCC is properly responded to.
                // Hence we ensure that a proper response is sent for the virtual call too.
                if (isVirtualCallInProgress()) {
                    String number = phone.getLine1Number();
                    AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
                    String args;
                    if (number == null) {
                        args = "+CLCC: 1,0,0,0,0,\"\",0";
                    }
                    else
                    {
                        args = "+CLCC: 1,0,0,0,0,\"" + number + "\"," +
                                  PhoneNumberUtils.toaFromString(number);
                    }
                    result.addResponse(args);
                    return result;
                }
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    return cdmaGetClccResult();
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    return gsmGetClccResult();
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        });

        // Call Hold and Multiparty Handling command
        parser.register("+CHLD", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                int phoneType = phone.getPhoneType();
                Call ringingCall = mCM.getFirstActiveRingingCall();
                Call backgroundCall = mCM.getFirstActiveBgCall();

                if (args.length >= 1) {
                    if (args[0].equals(0)) {
                        boolean result;
                        if (ringingCall!= null && ringingCall.isRinging()) {
                            result = PhoneUtils.hangupRingingCall(ringingCall);
                        } else {
                            result = PhoneUtils.hangupHoldingCall(backgroundCall);
                        }
                        if (result) {
                            return new AtCommandResult(AtCommandResult.OK);
                        } else {
                            return new AtCommandResult(AtCommandResult.ERROR);
                        }
                    } else if (args[0].equals(1)) {
                        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                            if (ringingCall!= null && ringingCall.isRinging()) {
                                // Hangup the active call and then answer call waiting call.
                                if (VDBG) log("CHLD:1 Callwaiting Answer call");
                                PhoneUtils.hangupRingingAndActive(phone);
                            } else {
                                // If there is no Call waiting then just hangup
                                // the active call. In CDMA this mean that the complete
                                // call session would be ended
                                if (VDBG) log("CHLD:1 Hangup Call");
                                PhoneUtils.hangup(PhoneGlobals.getInstance().mCM);
                            }
                            return new AtCommandResult(AtCommandResult.OK);
                        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                            /* MTK Modified : Begin */
                            boolean ret;
                            // Hangup active call, answer held call
                            if(mCM.hasActiveFgCall())
                            {
                                if(ringingCall != null && ringingCall.getState().isAlive()) {
                                    /// M: UT placeholder reference @{
                                    /* 1A1W */
                                    ret = PhoneUtils.answerAndEndActive(mCM, ringingCall);
                                    /// @}
                                } else{
                                    /* 1A1H && 1A0H */
                                    ret = PhoneUtils.hangupActiveCall(mCM.getActiveFgCall());
                                }
                            }
                            else
                            {
                                if(backgroundCall != null && backgroundCall.getState().isAlive())
                                {
                                    /* 0A1H */
                                    PhoneUtils.switchHoldingAndActive(backgroundCall);
                                    ret = true;
                                }
                                else
                                {
                                    ret = false;
                                }
                            }

                            if(ret)
                            {
                                return new AtCommandResult(AtCommandResult.OK);
                            }
                            else
                            {
                                return new AtCommandResult(AtCommandResult.ERROR);
                            }
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }
                    } else if (args[0].equals(2)) {
                        sendURC("OK");
                        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                            // For CDMA, the way we switch to a new incoming call is by
                            // calling PhoneUtils.answerCall(). switchAndHoldActive() won't
                            // properly update the call state within telephony.
                            // If the Phone state is already in CONF_CALL then we simply send
                            // a flash cmd by calling switchHoldingAndActive()
                            if (ringingCall.isRinging()) {
                                if (VDBG) log("CHLD:2 Callwaiting Answer call");
                                PhoneUtils.answerCall(ringingCall);
                                PhoneUtils.setMute(false);
                                // Setting the second callers state flag to TRUE (i.e. active)
                                cdmaSetSecondCallState(true);
                            } else if (PhoneGlobals.getInstance().cdmaPhoneCallState
                                    .getCurrentCallState()
                                    == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                                if (VDBG) log("CHLD:2 Swap Calls");
                                PhoneUtils.switchHoldingAndActive(backgroundCall);
                                // Toggle the second callers active state flag
                                cdmaSwapSecondCallState();
                            }
                        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                        ///M: use phone app / phone utils interfaces to simplified the code (phone app will hlep to handle things like confliction between voice / VT call) since dual code or not, the handle is identical@{
                            if (ringingCall != null && ringingCall.isRinging()) {  // M: (IAH), (IA, H), (I, AH), (IH, A), (I, A), (I, H), (IH), (IA)
                                PhoneGlobals.getInstance().phoneMgr.answerRingingCall();
                            }
                            else{ // M: (AH), (A, H)
                                PhoneUtils.switchHoldingAndActive(backgroundCall);
                            }
                       ///M: @}
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }
                        return new AtCommandResult(AtCommandResult.UNSOLICITED);
                    } else if (args[0].equals(3)) {
                        sendURC("OK");
                        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                            CdmaPhoneCallState.PhoneCallState state =
                                PhoneGlobals.getInstance().cdmaPhoneCallState.getCurrentCallState();
                            // For CDMA, we need to check if the call is in THRWAY_ACTIVE state
                            if (state == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                                if (VDBG) log("CHLD:3 Merge Calls");
                                /** M: BT HFP in Dual Talk @{ */
                                if (FeatureOption.MTK_DT_SUPPORT == true) {
                                    Phone fgPhone = mCM.getActiveFgCall().getPhone();
                                    Phone bgPhone = mCM.getFirstActiveBgCall().getPhone();
                                    boolean sameChannel = (fgPhone == bgPhone);
                                    if (sameChannel) {
                                        PhoneUtils.mergeCalls();
                                    } else { // M: we could not merge calls in different SIM
                                        return new AtCommandResult(AtCommandResult.ERROR);
                                  	}
                                } else {
                                    PhoneUtils.mergeCalls();
                                }
                                /** @} */
                            } else if (state == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                                // State is CONF_CALL already and we are getting a merge call
                                // This can happen when CONF_CALL was entered from a Call Waiting
                                mBluetoothPhoneState.updateCallHeld();
                            }
                        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                            if (mCM.hasActiveFgCall() && mCM.hasActiveBgCall()) {
                                /** M: BT HFP in Dual Talk @{ */
                                if (FeatureOption.MTK_DT_SUPPORT == true) {
                                    Phone fgPhone = mCM.getActiveFgCall().getPhone();
                                    Phone bgPhone = mCM.getFirstActiveBgCall().getPhone();
                                    boolean sameChannel = (fgPhone == bgPhone);
                                    if (sameChannel) {
                                        PhoneUtils.mergeCalls();
                                    } else { // M: we could not merge calls in different SIM
                                        return new AtCommandResult(AtCommandResult.ERROR);
                                  	}
                                } else {
                                    PhoneUtils.mergeCalls();
                                }
                                /** @} */
                            }
                        } else {
                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
                        }
                        return new AtCommandResult(AtCommandResult.UNSOLICITED);
                    }
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                mServiceConnectionEstablished = true;
                sendURC("+CHLD: (0,1,2,3)");
                sendURC("OK");  // send reply first, then connect audio
                if (isIncallAudio()) {
                    audioOn();
                } else if (mCM.getFirstActiveRingingCall().isRinging()) {
                    // need to update HS with RING when single ringing call exist
                    mBluetoothPhoneState.ring();
                }
                // already replied
                return new AtCommandResult(AtCommandResult.UNSOLICITED);
            }
        });

        // Get Network operator name
        parser.register("+COPS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                String operatorName = phone.getServiceState().getOperatorAlphaLong();
                if (operatorName != null) {
                    if (operatorName.length() > 16) {
                        operatorName = operatorName.substring(0, 16);
                    }
                    return new AtCommandResult(
                            "+COPS: 0,0,\"" + operatorName + "\"");
                } else {
                    return new AtCommandResult(
                            "+COPS: 0");
                }
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // Handsfree only supports AT+COPS=3,0
                if (args.length != 2 || !(args[0] instanceof Integer)
                    || !(args[1] instanceof Integer)) {
                    // syntax error
                    return new AtCommandResult(AtCommandResult.ERROR);
                } else if ((Integer)args[0] != 3 || (Integer)args[1] != 0) {
                    return reportCmeError(BluetoothCmeError.OPERATION_NOT_SUPPORTED);
                } else {
                    return new AtCommandResult(AtCommandResult.OK);
                }
            }
            @Override
            public AtCommandResult handleTestCommand() {
                // Out of spec, but lets be friendly
                return new AtCommandResult("+COPS: (3),(0)");
            }
        });

        // Mobile PIN
        // AT+CPIN is not in the handsfree spec (although it is in 3GPP)
        parser.register("+CPIN", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                return new AtCommandResult("+CPIN: READY");
            }
        });

        // Bluetooth Response and Hold
        // Only supported on PDC (Japan) and CDMA networks.
        parser.register("+BTRH", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                // Replying with just OK indicates no response and hold
                // features in use now
                return new AtCommandResult(AtCommandResult.OK);
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // Neeed PDC or CDMA
                return new AtCommandResult(AtCommandResult.ERROR);
            }
        });

        // Request International Mobile Subscriber Identity (IMSI)
        // Not in bluetooth handset spec
        parser.register("+CIMI", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // AT+CIMI
                String imsi = phone.getSubscriberId();
                if (imsi == null || imsi.length() == 0) {
                    return reportCmeError(BluetoothCmeError.SIM_FAILURE);
                } else {
                    return new AtCommandResult(imsi);
                }
            }
        });

        // Calling Line Identification Presentation
        parser.register("+CLIP", new AtCommandHandler() {
            @Override
            public AtCommandResult handleReadCommand() {
                // Currently assumes the network is provisioned for CLIP
                return new AtCommandResult("+CLIP: " + (mClip ? "1" : "0") + ",1");
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+CLIP=<n>
                if (args.length >= 1 && (args[0].equals(0) || args[0].equals(1))) {
                    mClip = args[0].equals(1);
                    return new AtCommandResult(AtCommandResult.OK);
                } else {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return new AtCommandResult("+CLIP: (0-1)");
            }
        });

        // AT+CGSN - Returns the device IMEI number.
        parser.register("+CGSN", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Get the IMEI of the device.
                // phone will not be NULL at this point.
                return new AtCommandResult("+CGSN: " + phone.getDeviceId());
            }
        });

        // AT+CGMM - Query Model Information
        parser.register("+CGMM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Return the Model Information.
                String model = SystemProperties.get("ro.product.model");
                if (model != null) {
                    return new AtCommandResult("+CGMM: " + model);
                } else {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
            }
        });

        // AT+CGMI - Query Manufacturer Information
        parser.register("+CGMI", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                // Return the Model Information.
                String manuf = SystemProperties.get("ro.product.manufacturer");
                if (manuf != null) {
                    return new AtCommandResult("+CGMI: " + manuf);
                } else {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
            }
        });

        // Noise Reduction and Echo Cancellation control
        parser.register("+NREC", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (args[0].equals(0)) {
                    mAudioManager.setParameters(HEADSET_NREC+"=off");
                    return new AtCommandResult(AtCommandResult.OK);
                } else if (args[0].equals(1)) {
                    mAudioManager.setParameters(HEADSET_NREC+"=on");
                    return new AtCommandResult(AtCommandResult.OK);
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
        });

        // Voice recognition (dialing)
        parser.register("+BVRA", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                if (!BluetoothHeadset.isBluetoothVoiceDialingEnabled(mContext)) {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
                if (args.length >= 1 && args[0].equals(1)) {
                    synchronized (BluetoothHandsfree.this) {
                        if (!isVoiceRecognitionInProgress() &&
                            !isCellularCallInProgress() &&
                            !isVirtualCallInProgress()) {
                            try {
                                mContext.startActivity(sVoiceCommandIntent);
                            } catch (ActivityNotFoundException e) {
                                return new AtCommandResult(AtCommandResult.ERROR);
                            }
                            expectVoiceRecognition();
                        }
                    }
                    return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing yet
                } else if (args.length >= 1 && args[0].equals(0)) {
                    if (isVoiceRecognitionInProgress()) {
                        audioOff();
                    }
                    return new AtCommandResult(AtCommandResult.OK);
                }
                return new AtCommandResult(AtCommandResult.ERROR);
            }
            @Override
            public AtCommandResult handleTestCommand() {
                return new AtCommandResult("+BVRA: (0-1)");
            }
        });

        // Retrieve Subscriber Number
        parser.register("+CNUM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                String number;
                /** M: GEMINI support @{ */
                if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                    //M: GEMINI API should be put in TelephonyManagerEx
                    number = TelephonyManagerEx.getDefault().getLine1Number(mPhonebook.getDefaultSIM());
                    log("+CNUM:"+ number);
                    
                } 
                /** @} */
                else {
                    number = phone.getLine1Number();  
                    log("+CNUM:"+ number);               
                } 
                if (number == null) {
                    return new AtCommandResult(AtCommandResult.OK);
                }
                return new AtCommandResult("+CNUM: ,\"" + number + "\"," +
                        PhoneNumberUtils.toaFromString(number) + ",,4");
            }
        });

        // Microphone Gain
        parser.register("+VGM", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+VGM=<gain>    in range [0,15]
                // Headset/Handsfree is reporting its current gain setting
                return new AtCommandResult(AtCommandResult.OK);
            }
        });

        // Speaker Gain
        parser.register("+VGS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                // AT+VGS=<gain>    in range [0,15]
                if (args.length != 1 || !(args[0] instanceof Integer)) {
                    return new AtCommandResult(AtCommandResult.ERROR);
                }
                mScoGain = (Integer) args[0];
                int flag =  mAudioManager.isBluetoothScoOn() ? AudioManager.FLAG_SHOW_UI:0;

                mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, mScoGain, flag);
                return new AtCommandResult(AtCommandResult.OK);
            }
        });

        // Phone activity status
        parser.register("+CPAS", new AtCommandHandler() {
            @Override
            public AtCommandResult handleActionCommand() {
                int status = 0;
                /// M: UT placeholder reference @{
                switch (mCM.getState()) {
                /// @}
                case IDLE:
                    status = 0;
                    break;
                case RINGING:
                    status = 3;
                    break;
                case OFFHOOK:
                    status = 4;
                    break;
                }
                return new AtCommandResult("+CPAS: " + status);
            }
        });

        mPhonebook.register(parser);
    }

    public void sendScoGainUpdate(int gain) {
        if (mScoGain != gain && (mRemoteBrsf & BRSF_HF_REMOTE_VOL_CONTROL) != 0x0) {
            sendURC("+VGS:" + gain);
            mScoGain = gain;
        }
    }

    public AtCommandResult reportCmeError(int error) {
        if (mCmee) {
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            result.addResponse("+CME ERROR: " + error);
            return result;
        } else {
            return new AtCommandResult(AtCommandResult.ERROR);
        }
    }

    private static final int START_CALL_TIMEOUT = 10000;  // ms
    private static final int START_OUT_CALL_TIMEOUT = 2000;  // ms

    private synchronized void expectCallStart() {
        mWaitingForCallStart = true;
        Message msg = Message.obtain(mHandler, CHECK_CALL_STARTED);
        mHandler.sendMessageDelayed(msg, START_CALL_TIMEOUT);
        if (!mStartCallWakeLock.isHeld()) {
            mStartCallWakeLock.acquire(START_CALL_TIMEOUT);
        }
        // Start 2 seconds to wait outgoing call started
        mWaitingForOutCallStart = true;
        msg = Message.obtain(mHandler, CHECK_OUT_CALL_STARTED);
        mHandler.sendMessageDelayed(msg, START_OUT_CALL_TIMEOUT);
    }

    private synchronized void callStarted() {
        if (mWaitingForCallStart) {
            mWaitingForCallStart = false;
            sendURC("OK");
            if (mStartCallWakeLock.isHeld()) {
                mStartCallWakeLock.release();
            }
        }
        // Remove the timer for waiting outgoing call started
        mHandler.removeMessages(CHECK_OUT_CALL_STARTED);
        mWaitingForOutCallStart = false;
    }

    private static final int START_VOICE_RECOGNITION_TIMEOUT = 5000;  // ms

    private synchronized void expectVoiceRecognition() {
        mWaitingForVoiceRecognition = true;
        Message msg = Message.obtain(mHandler, CHECK_VOICE_RECOGNITION_STARTED);
        mHandler.sendMessageDelayed(msg, START_VOICE_RECOGNITION_TIMEOUT);
        if (!mStartVoiceRecognitionWakeLock.isHeld()) {
            mStartVoiceRecognitionWakeLock.acquire(START_VOICE_RECOGNITION_TIMEOUT);
        }
    }

    /* package */ synchronized boolean startVoiceRecognition() {

        if  ((isCellularCallInProgress()) ||
             (isVirtualCallInProgress()) ||
             mVoiceRecognitionStarted) {
            Log.e(TAG, "startVoiceRecognition: Call in progress");
            return false;
        }

        mVoiceRecognitionStarted = true;

        if (mWaitingForVoiceRecognition) {
            // HF initiated
            mWaitingForVoiceRecognition = false;
            sendURC("OK");
        } else {
            // AG initiated
            sendURC("+BVRA: 1");
        }
        boolean ret = audioOn();
        if (ret == false) {
            mVoiceRecognitionStarted = false;
        }
        if (mStartVoiceRecognitionWakeLock.isHeld()) {
            mStartVoiceRecognitionWakeLock.release();
        }
        return ret;
    }

    /* package */ synchronized boolean stopVoiceRecognition() {

        if (!isVoiceRecognitionInProgress()) {
            return false;
        }

        mVoiceRecognitionStarted = false;

        sendURC("+BVRA: 0");
        audioOff();
        return true;
    }

    // Voice Recognition in Progress
    private boolean isVoiceRecognitionInProgress() {
        return (mVoiceRecognitionStarted || mWaitingForVoiceRecognition);
    }

    /*
     * This class broadcasts vendor-specific commands + arguments to interested receivers.
     */
    private class VendorSpecificCommandHandler extends AtCommandHandler {

        private String mCommandName;

        private int mCompanyId;

        private VendorSpecificCommandHandler(String commandName, int companyId) {
            mCommandName = commandName;
            mCompanyId = companyId;
        }

        @Override
        public AtCommandResult handleReadCommand() {
            return new AtCommandResult(AtCommandResult.ERROR);
        }

        @Override
        public AtCommandResult handleTestCommand() {
            return new AtCommandResult(AtCommandResult.ERROR);
        }

        @Override
        public AtCommandResult handleActionCommand() {
            return new AtCommandResult(AtCommandResult.ERROR);
        }

        @Override
        public AtCommandResult handleSetCommand(Object[] arguments) {
            broadcastVendorSpecificEventIntent(mCommandName,
                                               mCompanyId,
                                               BluetoothHeadset.AT_CMD_TYPE_SET,
                                               arguments,
                                               mHeadset.getRemoteDevice());
            return new AtCommandResult(AtCommandResult.OK);
        }
    }

    private boolean inDebug() {
        return DBG && SystemProperties.getBoolean(DebugThread.DEBUG_HANDSFREE, false);
    }

    private boolean allowAudioAnytime() {
        return inDebug() && SystemProperties.getBoolean(DebugThread.DEBUG_HANDSFREE_AUDIO_ANYTIME,
                false);
    }

    private void startDebug() {
        if (DBG && mDebugThread == null) {
            mDebugThread = new DebugThread();
            mDebugThread.start();
        }
    }

    private void stopDebug() {
        if (mDebugThread != null) {
            mDebugThread.interrupt();
            mDebugThread = null;
        }
    }

    // VirtualCall SCO support
    //

    // Cellular call in progress
    private boolean isCellularCallInProgress() {
        if (mCM.hasActiveFgCall() || mCM.hasActiveRingingCall()) return true;
        return false;
    }

    // Virtual Call in Progress
    private boolean isVirtualCallInProgress() {
        return mVirtualCallStarted;
    }

    void setVirtualCallInProgress(boolean state) {
        mVirtualCallStarted = state;
    }

    //NOTE: Currently the VirtualCall API does not allow the application to initiate a call
    // transfer. Call transfer may be initiated from the handsfree device and this is handled by
    // the VirtualCall API
    synchronized boolean initiateScoUsingVirtualVoiceCall() {
        if (DBG) log("initiateScoUsingVirtualVoiceCall: Received");
        // 1. Check if the SCO state is idle
        if (isCellularCallInProgress() || isVoiceRecognitionInProgress()) {
            Log.e(TAG, "initiateScoUsingVirtualVoiceCall: Call in progress");
            return false;
        }

        // 2. Perform outgoing call setup procedure
        if (mBluetoothPhoneState.sendUpdate() && !isVirtualCallInProgress()) {
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            // outgoing call
            result.addResponse("+CIEV: 3,2");
            result.addResponse("+CIEV: 2,1");
            result.addResponse("+CIEV: 3,0");
            sendURC(result.toString());
            if (DBG) Log.d(TAG, "initiateScoUsingVirtualVoiceCall: Sent Call-setup procedure");
        }

        mVirtualCallStarted = true;

        // 3. Open the Audio Connection
        if (audioOn() == false) {
            log("initiateScoUsingVirtualVoiceCall: audioON failed");
            terminateScoUsingVirtualVoiceCall();
            return false;
        }

        mAudioPossible = true;

        // Done
        if (DBG) log("initiateScoUsingVirtualVoiceCall: Done");
        return true;
    }

    synchronized boolean terminateScoUsingVirtualVoiceCall() {
        if (DBG) log("terminateScoUsingVirtualVoiceCall: Received");

        if (!isVirtualCallInProgress()) {
            return false;
        }

        // 1. Release audio connection
        audioOff();

        // 2. terminate call-setup
        if (mBluetoothPhoneState.sendUpdate()) {
            AtCommandResult result = new AtCommandResult(AtCommandResult.UNSOLICITED);
            // outgoing call
            result.addResponse("+CIEV: 2,0");
            sendURC(result.toString());
            if (DBG) log("terminateScoUsingVirtualVoiceCall: Sent Call-setup procedure");
        }
        mVirtualCallStarted = false;
        mAudioPossible = false;

        // Done
        if (DBG) log("terminateScoUsingVirtualVoiceCall: Done");
        return true;
    }


    /** Debug thread to read debug properties - runs when debug.bt.hfp is true
     *  at the time a bluetooth handsfree device is connected. Debug properties
     *  are polled and mock updates sent every 1 second */
    private class DebugThread extends Thread {
        /** Turns on/off handsfree profile debugging mode */
        static final String DEBUG_HANDSFREE = "debug.bt.hfp";

        /** Mock battery level change - use 0 to 5 */
        static final String DEBUG_HANDSFREE_BATTERY = "debug.bt.hfp.battery";

        /** Mock no cellular service when false */
        static final String DEBUG_HANDSFREE_SERVICE = "debug.bt.hfp.service";

        /** Mock cellular roaming when true */
        static final String DEBUG_HANDSFREE_ROAM = "debug.bt.hfp.roam";

        /** false to true transition will force an audio (SCO) connection to
         *  be established. true to false will force audio to be disconnected
         */
        static final String DEBUG_HANDSFREE_AUDIO = "debug.bt.hfp.audio";

        /** true allows incoming SCO connection out of call.
         */
        static final String DEBUG_HANDSFREE_AUDIO_ANYTIME = "debug.bt.hfp.audio_anytime";

        /** Mock signal strength change in ASU - use 0 to 31 */
        static final String DEBUG_HANDSFREE_SIGNAL = "debug.bt.hfp.signal";

        /** Debug AT+CLCC: print +CLCC result */
        static final String DEBUG_HANDSFREE_CLCC = "debug.bt.hfp.clcc";

        /** Debug AT+BSIR - Send In Band Ringtones Unsolicited AT command.
         * debug.bt.unsol.inband = 0 => AT+BSIR = 0 sent by the AG
         * debug.bt.unsol.inband = 1 => AT+BSIR = 0 sent by the AG
         * Other values are ignored.
         */

        static final String DEBUG_UNSOL_INBAND_RINGTONE =
            "debug.bt.unsol.inband";

        @Override
        public void run() {
            boolean oldService = true;
            boolean oldRoam = false;
            boolean oldAudio = false;

            while (!isInterrupted() && inDebug()) {
                int batteryLevel = SystemProperties.getInt(DEBUG_HANDSFREE_BATTERY, -1);
                if (batteryLevel >= 0 && batteryLevel <= 5) {
                    Intent intent = new Intent();
                    intent.putExtra("level", batteryLevel);
                    intent.putExtra("scale", 5);
                    mBluetoothPhoneState.updateBatteryState(intent);
                }

                boolean serviceStateChanged = false;
                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_SERVICE, true) != oldService) {
                    oldService = !oldService;
                    serviceStateChanged = true;
                }
                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_ROAM, false) != oldRoam) {
                    oldRoam = !oldRoam;
                    serviceStateChanged = true;
                }
                if (serviceStateChanged) {
                    Bundle b = new Bundle();
                    b.putInt("state", oldService ? 0 : 1);
                    b.putBoolean("roaming", oldRoam);
                    mBluetoothPhoneState.updateServiceState(true, ServiceState.newFromBundle(b));
                }

                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_AUDIO, false) != oldAudio) {
                    oldAudio = !oldAudio;
                    if (oldAudio) {
                        audioOn();
                    } else {
                        audioOff();
                    }
                }

                int signalLevel = SystemProperties.getInt(DEBUG_HANDSFREE_SIGNAL, -1);
                if (signalLevel >= 0 && signalLevel <= 31) {
                    SignalStrength signalStrength = new SignalStrength(signalLevel, -1, -1, -1,
                            -1, -1, -1, true);
                    Intent intent = new Intent();
                    Bundle data = new Bundle();
                    signalStrength.fillInNotifierBundle(data);
                    intent.putExtras(data);
                    mBluetoothPhoneState.updateSignalState(intent);
                }

                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_CLCC, false)) {
                    log(gsmGetClccResult().toString());
                }
                try {
                    sleep(1000);  // 1 second
                } catch (InterruptedException e) {
                    break;
                }

                int inBandRing =
                    SystemProperties.getInt(DEBUG_UNSOL_INBAND_RINGTONE, -1);
                if (inBandRing == 0 || inBandRing == 1) {
                    AtCommandResult result =
                        new AtCommandResult(AtCommandResult.UNSOLICITED);
                    result.addResponse("+BSIR: " + inBandRing);
                    sendURC(result.toString());
                }
            }
        }
    }

    public void cdmaSwapSecondCallState() {
        if (VDBG) log("cdmaSetSecondCallState: Toggling mCdmaIsSecondCallActive");
        mCdmaIsSecondCallActive = !mCdmaIsSecondCallActive;
        mCdmaCallsSwapped = true;
    }

    public void cdmaSetSecondCallState(boolean state) {
        if (VDBG) log("cdmaSetSecondCallState: Setting mCdmaIsSecondCallActive to " + state);
        mCdmaIsSecondCallActive = state;

        if (!mCdmaIsSecondCallActive) {
            mCdmaCallsSwapped = false;
        }
    }

    private static void log(String msg) {
        Log.d(TAG, "[BT][HFG] " + msg);
    }
    private static void logErr(String msg) {
        Log.e(TAG, "[BT][HFG] " + msg);
    }
    private static void logInfo(String msg) {
        Log.i(TAG, "[BT][HFG] " + msg);
    }
    private static void logWarn(String msg) {
        Log.w(TAG, "[BT][HFG] " + msg);
    }

    /* SH added additional functions */
    static final int CONTACTS_ID_COLUMN_INDEX = 0;
    static final int CONTACTS_NAME_COLUMN_INDEX = 1;
    
    private String getPhoneNumberByIndex(final int index, boolean orderByName) {
        log("getPhoneNumberByIndex("+index+", "+String.valueOf(orderByName)+")");
        if (index < 1) {
            logErr("getPhoneNumberByIndex : Invalid index value : "+index);            
            return null;
        }
        String phoneNumber = null;
        final Uri myUri = Contacts.CONTENT_URI;
        Cursor contactCursor = null;
        //String selection = null;
        long contactId = -1;
        int numberColIndex = -1;
        final String order = orderByName ? Contacts.DISPLAY_NAME : Contacts._ID;
        String[] projection = new String[] {
            Contacts._ID,   // 0                
            Contacts.DISPLAY_NAME, // 1
        };
        ContentResolver resolver = mContext.getContentResolver();
        
        try {
            contactCursor = resolver.query(myUri, projection, Contacts.IN_VISIBLE_GROUP + "=1",
                null, order);
            if (contactCursor != null && index <= contactCursor.getCount()) {
                log("contactCursor.getCount() = "+contactCursor.getCount());
                contactCursor.moveToPosition(index - 1);
                contactId = contactCursor.getLong(CONTACTS_ID_COLUMN_INDEX);
                log("Query startPointId = " + contactId);
                phoneNumber = querySuperPrimaryPhone(contactId);
            }
        } catch (Exception e) {
            log("[ERR] query Phone number failed");
        } finally {
            if (contactCursor != null) {
                log("close contactCursor");
                contactCursor.close();
            }
            else
            {
                log("[ERR] contactCursor is null");
            }
        }
        log("getPhoneNumberByIndex return "+((phoneNumber!=null)?phoneNumber:"null"));
        return phoneNumber;
    }

    private String querySuperPrimaryPhone(long contactId) {
        Cursor c = null;
        String phone = null;
        log("querySuperPrimaryPhone : id="+contactId);
        try {
            Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
            Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);

            log("baseUri="+baseUri.toString());
            log("dataUri="+dataUri.toString());
            /*
            c = mContext.getContentResolver().query(dataUri,
                    new String[] {CommonDataKinds.Phone.NUMBER},
                    Data.MIMETYPE + "=" + CommonDataKinds.Phone.MIMETYPE +
                        " AND " + Data.IS_SUPER_PRIMARY + "=1",
                    null, null);
            */
            /*
            c = mContext.getContentResolver().query(dataUri,
                    new String[] {CommonDataKinds.Phone.NUMBER, Data.IS_SUPER_PRIMARY},
                    Data.MIMETYPE + "=" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    null, Data.IS_SUPER_PRIMARY+" DESC");
            */
            c = mContext.getContentResolver().query(dataUri,
                    new String[] {CommonDataKinds.Phone.NUMBER, Data.IS_SUPER_PRIMARY},
                    Data.MIMETYPE + "=?", new String[] {CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
                    Data.IS_SUPER_PRIMARY+" DESC");

            if(c != null)
                log("c.getCount() = "+c.getCount());
            
            if (c != null) {
                 if(c.moveToFirst())
                {
                    // Just return the first one.
                    log("get first column");
                    phone = c.getString(0);
                }
                else
                {
                    log("moveToFirst failed");
                }
            }
        } finally {
            if (c != null) {
                c.close();
                log("close cursor c");
            }
            else
            {
                log("cursor c is null");
            }
        }
        log("querySuperPrimaryPhone : "+((phone!=null)?phone:"null"));
        return phone;
    }

    /// M: UT placeholder addedmethod autogenerate
}
