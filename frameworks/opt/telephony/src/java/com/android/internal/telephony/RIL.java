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
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.PowerManager.WakeLock;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccController;

import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.IccRefreshResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Vector;
import java.io.UnsupportedEncodingException;

//MTK-START [mtk04070][111121][ALPS00093395]MTK added
import android.provider.Settings; // ALPS00311194 WCDMA/GSM preferred for Single Card
import com.android.internal.telephony.gsm.SuppCrssNotification;
import com.android.internal.telephony.gsm.SpnOverride;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
import com.mediatek.common.featureoption.FeatureOption;
//MTK-END [mtk04070][111121][ALPS00093395]MTK added

import android.telephony.SmsParameters;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.common.telephony.IccSmsStorageStatus;
import com.mediatek.common.telephony.gsm.PBEntry;
import com.mediatek.common.telephony.gsm.PBMemStorage;
import com.mediatek.common.telephony.gsm.UsimPBMemInfo;
import com.android.internal.telephony.gsm.SIMRecords;
import com.android.internal.telephony.IccRecords;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.xlog.Xlog;
import com.android.internal.telephony.cat.CatService;

/**
 * {@hide}
 */
class RILRequest {
    static final String LOG_TAG = "RILJ";

    //***** Class Variables
    static int sNextSerial = 0;
    static Object sSerialMonitor = new Object();
    private static Object sPoolSync = new Object();
    private static RILRequest sPool = null;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 4;

    //***** Instance Variables
    int mSerial;
    int mRequest;
    Message mResult;
    Parcel mp;
    RILRequest mNext;

    /**
     * Retrieves a new RILRequest instance from the pool.
     *
     * @param request RIL_REQUEST_*
     * @param result sent when operation completes
     * @return a RILRequest instance from the pool.
     */
    static RILRequest obtain(int request, Message result) {
        RILRequest rr = null;

        synchronized(sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }

        if (rr == null) {
            rr = new RILRequest();
        }

        synchronized(sSerialMonitor) {
            rr.mSerial = sNextSerial++;
        }
        rr.mRequest = request;
        rr.mResult = result;
        rr.mp = Parcel.obtain();

        if (result != null && result.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }

        // first elements in any RIL Parcel
        rr.mp.writeInt(request);
        rr.mp.writeInt(rr.mSerial);

        return rr;
    }

    /**
     * Returns a RILRequest instance to the pool.
     *
     * Note: This should only be called once per use.
     */
    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                mResult = null;
            }
        }
    }

    private RILRequest() {
    }

    static void
    resetSerial() {
        synchronized(sSerialMonitor) {
            sNextSerial = 0;
        }
    }

    String
    serialString() {
        //Cheesy way to do %04d
        StringBuilder sb = new StringBuilder(8);
        String sn;

        sn = Integer.toString(mSerial);

        //sb.append("J[");
        sb.append('[');
        for (int i = 0, s = sn.length() ; i < 4 - s; i++) {
            sb.append('0');
        }

        sb.append(sn);
        sb.append(']');
        return sb.toString();
    }

    void
    onError(int error, Object ret) {
        CommandException ex;

        ex = CommandException.fromRilErrno(error);

        if (RIL.RILJ_LOGD) Log.d(LOG_TAG, serialString() + "< "
            + RIL.requestToString(mRequest)
            + " error: " + ex);

        if (mResult != null) {
            AsyncResult.forMessage(mResult, ret, ex);
            mResult.sendToTarget();
        }

        if (mp != null) {
            mp.recycle();
            mp = null;
        }
    }
}


/**
 * RIL implementation of the CommandsInterface.
 * FIXME public only for testing
 *
 * {@hide}
 */
public final class RIL extends BaseCommands implements CommandsInterface {
    static final String LOG_TAG = "RILJ";
    static final boolean RILJ_LOGD = true;
    static final boolean RILJ_LOGV = false; // STOP SHIP if true

    /**
     * Wake lock timeout should be longer than the longest timeout in
     * the vendor ril.
     */
    //MTK-START [mtk04070][111121][ALPS00093395]Replace 60000 with 5000
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 5000;
    //MTK-END [mtk04070][111121][ALPS00093395]Replace 60000 with 5000

    //***** Instance Variables

    LocalSocket mSocket;
    HandlerThread mSenderThread;
    RILSender mSender;
    Thread mReceiverThread;
    RILReceiver mReceiver;
    WakeLock mWakeLock;
    int mWakeLockTimeout;
    // The number of requests pending to be sent out, it increases before calling
    // EVENT_SEND and decreases while handling EVENT_SEND. It gets cleared while
    // WAKE_LOCK_TIMEOUT occurs.
    int mRequestMessagesPending;
    // The number of requests sent out but waiting for response. It increases while
    // sending request and decreases while handling response. It should match
    // mRequestList.size() unless there are requests no replied while
    // WAKE_LOCK_TIMEOUT occurs.
    int mRequestMessagesWaiting;

    //I'd rather this be LinkedList or something
    ArrayList<RILRequest> mRequestsList = new ArrayList<RILRequest>();

    Object     mLastNITZTimeInfo;

    // When we are testing emergency calls
    AtomicBoolean mTestingEmergencyCall = new AtomicBoolean(false);

    //***** Events

    static final int EVENT_SEND                 = 1;
    static final int EVENT_WAKE_LOCK_TIMEOUT    = 2;

    //***** Constants

    // match with constant in ril.cpp
    static final int RIL_MAX_COMMAND_BYTES = (8 * 1024);
    static final int RESPONSE_SOLICITED = 0;
    static final int RESPONSE_UNSOLICITED = 1;

    //MTK-START [mtk04070][111121][ALPS00093395]For supporting Gemini
    static String SOCKET_NAME_RIL_1 = "rild";
    static String SOCKET_NAME_RIL_2 = "rild2";
    //MTK-END [mtk04070][111121][ALPS00093395]For supporting Gemini
    //MTK-START For supporting Gemini+
    static String SOCKET_NAME_RIL_3 = "rild3";
    static String SOCKET_NAME_RIL_4 = "rild4";
    //MTK-END For supporting Gemini+

    static final int SOCKET_OPEN_RETRY_MILLIS = 4 * 1000;

    // The number of the required config values for broadcast SMS stored in the C struct
    // RIL_CDMA_BroadcastServiceInfo
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;

    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;

    //MTK-START [mtk04070][111121][ALPS00093395]MTK added
    //save the status of screen
    private boolean isScreenOn = true;

    /* Add for supporting Gemini */
    private int mySimId;

    //since we need to execute EFUN once to turn off radio state,
    //this should be a static member
    static private boolean mInitialRadioStateChange = false;//via: modify from true to false for debugging

    static private int mSimInsertedStatus;

    // radio temporarily unavailable state due to BT Carkit and WiMax connection
    static final int RADIO_TEMPSTATE_AVAILABLE = 0;     /* Radio available */
    static final int RADIO_TEMPSTATE_UNAVAILABLE = 1;           /* Radio unavailable temporarily */
    private int radioTemporarilyUnavailable = RADIO_TEMPSTATE_AVAILABLE; //mtk02514_socket
    SpnOverride mSpnOverride;
    private boolean mIs3GSwitch;

    /// M: Solve [ALPS00334963][Rose][6577JB][Free Test][Network]The SIM card network lost and the information of SIM card shows as "No service" in the unlock screen when we input many numbers ceaselessly until the call end automatically(3/3). @{
    /* DTMF request will be ignored when the count of requests reaches 32 */
    private int dtmfRequestCount = 0;
    private final int MAXIMUM_DTMF_REQUEST = 32;

    private int getAvailableNetworkRequests = 0;
    //MTK-END [mtk04070][111121][ALPS00093395]MTK added

    //MTK-START [mtk08470][130121][ALPS00445268] Block non-sequence DTMF input
    /* DTMF request will be ignored when duplicated sending */
    private final boolean DTMF_STATUS_START = true;
    private final boolean DTMF_STATUS_STOP = false;
    private boolean mDtmfStatus = DTMF_STATUS_STOP;
    private Vector mDtmfQueue = new Vector(MAXIMUM_DTMF_REQUEST);
    private RILRequest mPendingCHLDRequest = null;
    private boolean mIsSendChldRequest = false;
    //MTK-END [mtk08470][130121][ALPS00445268]MTK added
    
    private IServiceStateExt mServiceStateExt;

    private boolean mIsFirstResponse = true;
    
    private Object mStkPciObject = null;

    private PowerManager pm = null; /* ALPS00453878 */
	
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.i(LOG_TAG, "RIL received ACTION_SCREEN_ON");            
                sendScreenState(true);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(LOG_TAG, "RIL received ACTION_SCREEN_OFF");            
                sendScreenState(false);
            } else {
                Log.w(LOG_TAG, "RIL received unexpected Intent: " + intent.getAction());
            }
        }
    };

    class RILSender extends Handler implements Runnable {
        public RILSender(Looper looper) {
            super(looper);
        }

        // Only allocated once
        byte[] dataLength = new byte[4];

        //***** Runnable implementation
        public void
        run() {
            //setup if needed
        }


        //***** Handler implementation
        @Override public void
        handleMessage(Message msg) {
            RILRequest rr = (RILRequest)(msg.obj);
            RILRequest req = null;

            switch (msg.what) {
                case EVENT_SEND:
                    /**
                     * mRequestMessagePending++ already happened for every
                     * EVENT_SEND, thus we must make sure
                     * mRequestMessagePending-- happens once and only once
                     */
                    boolean alreadySubtracted = false;
                    try {
                        LocalSocket s;

                        s = mSocket;

                        //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
                        if (s == null || radioTemporarilyUnavailable != RADIO_TEMPSTATE_AVAILABLE) {
                            rr.onError(RADIO_NOT_AVAILABLE, null);
                            rr.release();
                            mRequestMessagesPending--;
                            alreadySubtracted = true;
                            return;
                        }

                        synchronized (mRequestsList) {
                            mRequestsList.add(rr);
                        }

                        mRequestMessagesPending--;
                        alreadySubtracted = true;
                        //MTK-END [mtk04070][111121][ALPS00093395]MTK modified

                        byte[] data;

                        data = rr.mp.marshall();
                        rr.mp.recycle();
                        rr.mp = null;

                        if (data.length > RIL_MAX_COMMAND_BYTES) {
                            throw new RuntimeException(
                                    "Parcel larger than max bytes allowed! "
                                                          + data.length);
                        }

                        // parcel length in big endian
                        dataLength[0] = dataLength[1] = 0;
                        dataLength[2] = (byte)((data.length >> 8) & 0xff);
                        dataLength[3] = (byte)((data.length) & 0xff);

                        //Log.v(LOG_TAG, "writing packet: " + data.length + " bytes");

                        s.getOutputStream().write(dataLength);
                        s.getOutputStream().write(data);
                    } catch (IOException ex) {
                        Log.e(LOG_TAG, "IOException", ex);
                        req = findAndRemoveRequestFromList(rr.mSerial);
                        // make sure this request has not already been handled,
                        // eg, if RILReceiver cleared the list.
                        if (req != null || !alreadySubtracted) {
                            rr.onError(RADIO_NOT_AVAILABLE, null);
                            rr.release();
                        }
                    } catch (RuntimeException exc) {
                        Log.e(LOG_TAG, "Uncaught exception ", exc);
                        req = findAndRemoveRequestFromList(rr.mSerial);
                        // make sure this request has not already been handled,
                        // eg, if RILReceiver cleared the list.
                        if (req != null || !alreadySubtracted) {
                            rr.onError(GENERIC_FAILURE, null);
                            rr.release();
                        }
                    } finally {
                        // Note: We are "Done" only if there are no outstanding
                        // requests or replies. Thus this code path will only release
                        // the wake lock on errors.
                        releaseWakeLockIfDone();
                    }

                    //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
                    if (!alreadySubtracted) {
                        mRequestMessagesPending--;
                    }
                    //MTK-END [mtk04070][111121][ALPS00093395]MTK modified

                    break;

                case EVENT_WAKE_LOCK_TIMEOUT:
                    // Haven't heard back from the last request.  Assume we're
                    // not getting a response and  release the wake lock.
                    // TODO should we clean up mRequestList and mRequestPending
                    synchronized (mWakeLock) {
                        if (mWakeLock.isHeld()) {
                            if (RILJ_LOGD) {
                                synchronized (mRequestsList) {
                                    int count = mRequestsList.size();
                                    Log.d(LOG_TAG, "WAKE_LOCK_TIMEOUT " +
                                        " mReqPending=" + mRequestMessagesPending +
                                        " mRequestList=" + count);

                                    for (int i = 0; i < count; i++) {
                                        rr = mRequestsList.get(i);
                                        Log.d(LOG_TAG, i + ": [" + rr.mSerial + "] " +
                                            requestToString(rr.mRequest));

                                    }
                                }
                            }
                            mWakeLock.release();
                        }
                    }
                    break;
            }
        }
    };

    /**
     * Reads in a single RIL message off the wire. A RIL message consists
     * of a 4-byte little-endian length and a subsequent series of bytes.
     * The final message (length header omitted) is read into
     * <code>buffer</code> and the length of the final message (less header)
     * is returned. A return value of -1 indicates end-of-stream.
     *
     * @param is non-null; Stream to read from
     * @param buffer Buffer to fill in. Must be as large as maximum
     * message size, or an ArrayOutOfBounds exception will be thrown.
     * @return Length of message less header, or -1 on end of stream.
     * @throws IOException
     */
    private static int readRilMessage(InputStream is, byte[] buffer)
            throws IOException {
        int countRead;
        int offset;
        int remaining;
        int messageLength;

        // First, read in the length of the message
        offset = 0;
        remaining = 4;
        do {
            countRead = is.read(buffer, offset, remaining);

            if (countRead < 0 ) {
                Log.e(LOG_TAG, "Hit EOS reading message length");
                return -1;
            }

            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);

        messageLength = ((buffer[0] & 0xff) << 24)
                | ((buffer[1] & 0xff) << 16)
                | ((buffer[2] & 0xff) << 8)
                | (buffer[3] & 0xff);

        // Then, re-use the buffer and read in the message itself
        offset = 0;
        remaining = messageLength;
        do {
            countRead = is.read(buffer, offset, remaining);

            if (countRead < 0 ) {
                Log.e(LOG_TAG, "Hit EOS reading message.  messageLength=" + messageLength
                        + " remaining=" + remaining);
                return -1;
            }

            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);

        return messageLength;
    }

    class RILReceiver implements Runnable {
        byte[] buffer;
        
        //MTK-START [mtk04070][111121][ALPS00093395]Refined for supporting Gemini
        int mySimId;

        RILReceiver() {
            this(PhoneConstants.GEMINI_SIM_1);
        }

        RILReceiver(int simId) {
            buffer = new byte[RIL_MAX_COMMAND_BYTES];
            mySimId = simId;
        }

        private String getRilSocketName(int simId){
            if (simId == PhoneConstants.GEMINI_SIM_2) {
                return SOCKET_NAME_RIL_2;			
            }else if (simId == PhoneConstants.GEMINI_SIM_3) {
                return SOCKET_NAME_RIL_3;
            }else if (simId == PhoneConstants.GEMINI_SIM_4) {
                return SOCKET_NAME_RIL_4;
            }else {
                return SOCKET_NAME_RIL_1;
            }	
        }			

        public void
        run() {
            int retryCount = 0;
            String socketRil = getRilSocketName(mySimId);

            try {
                for (;;) {
                LocalSocket s = null;
                LocalSocketAddress l;

                    socketRil = getRilSocketName(mySimId);
/* 3G switch start */
                    if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                        int m3GsimId = PhoneConstants.GEMINI_SIM_1;
                        m3GsimId =  get3GSimId();
						
                        if (m3GsimId >= PhoneConstants.GEMINI_SIM_2) {
                            if (mySimId == PhoneConstants.GEMINI_SIM_1){
                                socketRil = getRilSocketName(m3GsimId);
                            }								
                            else if(mySimId == m3GsimId){
                                socketRil = SOCKET_NAME_RIL_1;
                            }								
                            Log.i (LOG_TAG, "3G switched, switch sockets [" + mySimId + ", " + socketRil + "]");
                            mIs3GSwitch = true;
                        } else {
                            mIs3GSwitch = false;
                        }
                    }
/* 3G switch end */

                try {
                    s = new LocalSocket();
                    l = new LocalSocketAddress(socketRil,
                            LocalSocketAddress.Namespace.RESERVED);
                    s.connect(l);
                } catch (IOException ex){
                    try {
                        if (s != null) {
                            s.close();
                        }
                    } catch (IOException ex2) {
                        //ignore failure to close after failure to connect
                    }

                    // don't print an error message after the the first time
                    // or after the 8th time

                    if (retryCount == 16) {
                        Log.e (LOG_TAG,
                            "Couldn't find '" + socketRil
                            + "' socket after " + retryCount
                            + " times, continuing to retry silently");
                    } else if (retryCount > 0 && retryCount < 16) {
                        Log.i (LOG_TAG,
                            "Couldn't find '" + socketRil
                            + "' socket; retrying after timeout");
                    }

                    try {
                        Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                    } catch (InterruptedException er) {
                    }

                    retryCount++;
                    continue;
                }

                retryCount = 0;

                mSocket = s;
                Log.i(LOG_TAG, "Connected to '" + socketRil + "' socket");

                int length = 0;
                try {
                    InputStream is = mSocket.getInputStream();

                    for (;;) {
                        Parcel p;

                        length = readRilMessage(is, buffer);

                        if (length < 0) {
                            // End-of-stream reached
                            break;
                        }

                        p = Parcel.obtain();
                        p.unmarshall(buffer, 0, length);
                        p.setDataPosition(0);

                        //Log.v(LOG_TAG, "Read packet: " + length + " bytes");

                        processResponse(p);
                        p.recycle();
                    }
                } catch (java.io.IOException ex) {
                    Log.i(LOG_TAG, "'" + socketRil + "' socket closed",
                          ex);
                } catch (Throwable tr) {
                    Log.e(LOG_TAG, "Uncaught exception read length=" + length +
                        "Exception:" + tr.toString());
                }

                Log.i(LOG_TAG, "Disconnected from '" + socketRil
                      + "' socket");

                setRadioState (RadioState.RADIO_UNAVAILABLE);

                try {
                    mSocket.close();
                } catch (IOException ex) {
                }

                mSocket = null;
                RILRequest.resetSerial();

                // Clear request list on close
                synchronized (mRequestsList) {
                    for (int i = 0, sz = mRequestsList.size() ; i < sz ; i++) {
                        RILRequest rr = mRequestsList.get(i);
                        rr.onError(RADIO_NOT_AVAILABLE, null);
                        rr.release();
                    }

                    mRequestsList.clear();

                    // M: reset counter as radio is not available
                    getAvailableNetworkRequests = 0;
                }
            }} catch (Throwable tr) {
                Log.e(LOG_TAG,"Uncaught exception", tr);
            }
        //MTK-END [mtk04070][111121][ALPS00093395]Refined for supporting Gemini

            /* We're disconnected so we don't know the ril version */
            notifyRegistrantsRilConnectionChanged(-1);
        }
    }



    //***** Constructors
    //MTK-START [mtk04070][111121][ALPS00093395]Refined for supporting Gemini
    public RIL(Context context, int preferredNetworkType, int cdmaSubscription) {
       this (context, preferredNetworkType, cdmaSubscription, PhoneConstants.GEMINI_SIM_1);
    }

    public RIL(Context context, int preferredNetworkType, int cdmaSubscription, int simId) {
        super(context);
        mySimId = simId;
        if (RILJ_LOGD) {
            riljLog("RIL(context, preferredNetworkType=" + preferredNetworkType +
                    " cdmaSubscription=" + cdmaSubscription + ")");
        }
        mCdmaSubscription  = cdmaSubscription;
        mPreferredNetworkType = preferredNetworkType;
        mPhoneType = RILConstants.NO_PHONE;

        int telephonyMode = PhoneFactory.getTelephonyMode();
        if (telephonyMode == PhoneFactory.MODE_0_NONE) {
            if (FeatureOption.EVDO_DT_SUPPORT) {
                /*Set GSM as Phone1 and CDMA as Phone2 */
                SOCKET_NAME_RIL_2 = "rild-via";
            } else if (FeatureOption.MTK_DT_SUPPORT) {
                SOCKET_NAME_RIL_2 = "rild3";
            }
        } else {
            switch (telephonyMode) {
                case PhoneFactory.MODE_1_WG_GEMINI:
                case PhoneFactory.MODE_3_FDD_SINGLE:
                    SOCKET_NAME_RIL_1 = "rild";
                    SOCKET_NAME_RIL_2 = "rild2";
                    break;
                case PhoneFactory.MODE_2_TG_GEMINI:
                case PhoneFactory.MODE_4_TDD_SINGLE:
                    if(FeatureOption.MTK_ENABLE_MD2){
                        SOCKET_NAME_RIL_1 = "rild-md2";
                        SOCKET_NAME_RIL_2 = "rild2-md2";
                    }else{
                        SOCKET_NAME_RIL_1 = "rild";
                        SOCKET_NAME_RIL_2 = "rild2";                    
                    }
                    break;
                case PhoneFactory.MODE_5_WGNTG_DUALTALK:
                    if (PhoneFactory.getFirstMD() == 1) {
                        SOCKET_NAME_RIL_1 = "rild";
                        SOCKET_NAME_RIL_2 = "rild-md2";
                    } else {
                        SOCKET_NAME_RIL_1 = "rild-md2";
                        SOCKET_NAME_RIL_2 = "rild";
                    }
                    break;
                case PhoneFactory.MODE_7_WGNG_DUALTALK:
                case PhoneFactory.MODE_8_GNG_DUALTALK:
                    SOCKET_NAME_RIL_1 = "rild";
                    SOCKET_NAME_RIL_2 = "rild-md2";
                    break;
                case PhoneFactory.MODE_6_TGNG_DUALTALK:
                    SOCKET_NAME_RIL_1 = "rild-md2";
                    SOCKET_NAME_RIL_2 = "rild";
                    break;
            }
        }

        riljLog("SOCKET_NAME_RIL_1 = " + SOCKET_NAME_RIL_1);
        riljLog("SOCKET_NAME_RIL_2 = " + SOCKET_NAME_RIL_2);
		

        pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mWakeLock.setReferenceCounted(false);
        mWakeLockTimeout = SystemProperties.getInt(TelephonyProperties.PROPERTY_WAKE_LOCK_TIMEOUT,
                DEFAULT_WAKE_LOCK_TIMEOUT);
        mRequestMessagesPending = 0;
        mRequestMessagesWaiting = 0;

        /// M: Solve [ALPS00334963][Rose][6577JB][Free Test][Network]The SIM card network lost and the information of SIM card shows as "No service" in the unlock screen when we input many numbers ceaselessly until the call end automatically(3/3). @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        dtmfRequestCount = 0;

        if (simId == PhoneConstants.GEMINI_SIM_1) {
            mSenderThread = new HandlerThread("RILSender1");
        } else if(simId == PhoneConstants.GEMINI_SIM_2){
            mSenderThread = new HandlerThread("RILSender2");
        } else if(simId == PhoneConstants.GEMINI_SIM_3){
            mSenderThread = new HandlerThread("RILSender3");
        } else if(simId == PhoneConstants.GEMINI_SIM_4){
            mSenderThread = new HandlerThread("RILSender4");
        }
        mSenderThread.start();

        Looper looper = mSenderThread.getLooper();
        mSender = new RILSender(looper);

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false) {
            riljLog("Not starting RILReceiver: wifi-only");
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(mIntentReceiver, filter);
        }

        try{
            mServiceStateExt = MediatekClassFactory.createInstance(IServiceStateExt.class, context);
        } catch (Exception e){
            e.printStackTrace();
        }
        
        IntentFilter filterForStkCachedCommand = new IntentFilter();
        filterForStkCachedCommand.addAction(CatService.ACTION_CAT_INIT_DONE);
        context.registerReceiver(mCatServiceInitListener, filterForStkCachedCommand);
        Xlog.d(LOG_TAG, "[CachedStk register receiver to listen CAT init");
    }
    //MTK-END [mtk04070][111121][ALPS00093395]Refined for supporting Gemini

    public void startRilReceiver() {
        riljLog("Starting RILReceiver");
            mReceiver = new RILReceiver(mySimId);
            if (mySimId == PhoneConstants.GEMINI_SIM_1) {
           mReceiverThread = new Thread(mReceiver, "RILReceiver1");
            } else if(mySimId == PhoneConstants.GEMINI_SIM_2){
           mReceiverThread = new Thread(mReceiver, "RILReceiver2");
            } else if(mySimId == PhoneConstants.GEMINI_SIM_3){
           mReceiverThread = new Thread(mReceiver, "RILReceiver3");
            } else if(mySimId == PhoneConstants.GEMINI_SIM_4){
           mReceiverThread = new Thread(mReceiver, "RILReceiver4");
        }
        mReceiverThread.start();
    }

    //***** CommandsInterface implementation

    public void getVoiceRadioTechnology(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_VOICE_RADIO_TECH, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }


    @Override public void
    setOnNITZTime(Handler h, int what, Object obj) {
        super.setOnNITZTime(h, what, obj);

        // Send the last NITZ time if we have it
        if (mLastNITZTimeInfo != null) {
            mNITZTimeRegistrant
                .notifyRegistrant(
                    new AsyncResult (null, mLastNITZTimeInfo, null));
            mLastNITZTimeInfo = null;
        }
    }

    public void
    getIccCardStatus(Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_SIM_STATUS, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override public void
    supplyIccPin(String pin, Message result) {
        supplyIccPinForApp(pin, null, result);
    }

    @Override public void
    supplyIccPinForApp(String pin, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ENTER_SIM_PIN, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(2);
        rr.mp.writeString(pin);
        rr.mp.writeString(aid);

        send(rr);
    }

    @Override public void
    supplyIccPuk(String puk, String newPin, Message result) {
        supplyIccPukForApp(puk, newPin, null, result);
    }

    @Override public void
    supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ENTER_SIM_PUK, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(3);
        rr.mp.writeString(puk);
        rr.mp.writeString(newPin);
        rr.mp.writeString(aid);

        send(rr);
    }

    @Override public void
    supplyIccPin2(String pin, Message result) {
        supplyIccPin2ForApp(pin, null, result);
    }

    @Override public void
    supplyIccPin2ForApp(String pin, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ENTER_SIM_PIN2, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(2);
        rr.mp.writeString(pin);
        rr.mp.writeString(aid);

        send(rr);
    }

    @Override public void
    supplyIccPuk2(String puk2, String newPin2, Message result) {
        supplyIccPuk2ForApp(puk2, newPin2, null, result);
    }

    @Override public void
    supplyIccPuk2ForApp(String puk, String newPin2, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ENTER_SIM_PUK2, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(3);
        rr.mp.writeString(puk);
        rr.mp.writeString(newPin2);
        rr.mp.writeString(aid);

        send(rr);
    }

    @Override public void
    changeIccPin(String oldPin, String newPin, Message result) {
        changeIccPinForApp(oldPin, newPin, null, result);
    }

    @Override public void
    changeIccPinForApp(String oldPin, String newPin, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CHANGE_SIM_PIN, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(3);
        rr.mp.writeString(oldPin);
        rr.mp.writeString(newPin);
        rr.mp.writeString(aid);

        send(rr);
    }

    @Override public void
    changeIccPin2(String oldPin2, String newPin2, Message result) {
        changeIccPin2ForApp(oldPin2, newPin2, null, result);
    }

    @Override public void
    changeIccPin2ForApp(String oldPin2, String newPin2, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CHANGE_SIM_PIN2, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(3);
        rr.mp.writeString(oldPin2);
        rr.mp.writeString(newPin2);
        rr.mp.writeString(aid);

        send(rr);
    }

    public void
    changeBarringPassword(String facility, String oldPwd, String newPwd, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CHANGE_BARRING_PASSWORD, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(3);
        rr.mp.writeString(facility);
        rr.mp.writeString(oldPwd);
        rr.mp.writeString(newPwd);

        send(rr);
    }

    public void
    supplyNetworkDepersonalization(String netpin, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(1);
        rr.mp.writeString(netpin);

        send(rr);
    }

    public void
    getCurrentCalls (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_CURRENT_CALLS, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Deprecated public void
    getPDPContextList(Message result) {
        getDataCallList(result);
    }

    public void
    getDataCallList(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DATA_CALL_LIST, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    dial (String address, int clirMode, Message result) {
        dial(address, clirMode, null, result);
    }

    public void
    dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DIAL, result);

        rr.mp.writeString(address);
        rr.mp.writeInt(clirMode);
        rr.mp.writeInt(0); // UUS information is absent

        if (uusInfo == null) {
            rr.mp.writeInt(0); // UUS information is absent
        } else {
            rr.mp.writeInt(1); // UUS information is present
            rr.mp.writeInt(uusInfo.getType());
            rr.mp.writeInt(uusInfo.getDcs());
            rr.mp.writeByteArray(uusInfo.getUserData());
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getIMSI(Message result) {
        getIMSIForApp(null, result);
    }

    public void
    getIMSIForApp(String aid, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_IMSI, result);

        rr.mp.writeInt(1);
        rr.mp.writeString(aid);

        if (RILJ_LOGD) riljLog(rr.serialString() +
                              "> getIMSI: " + requestToString(rr.mRequest)
                              + " aid: " + aid);

        send(rr);
    }

    public void
    getIMEI(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_IMEI, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getIMEISV(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_IMEISV, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }


    public void
    hangupConnection (int gsmIndex, Message result) {
        if (RILJ_LOGD) riljLog("hangupConnection: gsmIndex=" + gsmIndex);

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " +
                gsmIndex);

        rr.mp.writeInt(1);
        rr.mp.writeInt(gsmIndex);

        send(rr);
    }

    public void
    hangupWaitingOrBackground (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    hangupForegroundResumeBackground (Message result) {
        RILRequest rr
                = RILRequest.obtain(
                        RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND,
                                        result);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    switchWaitingOrHoldingAndActive (Message result) {
        RILRequest rr
                = RILRequest.obtain(
                        RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE,
                                        result);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        handleChldRelatedRequest(rr);
    }

    public void
    conference (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_CONFERENCE, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        handleChldRelatedRequest(rr);
    }


    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE,
                result);

        rr.mp.writeInt(1);
        rr.mp.writeInt(enable ? 1:0);

        send(rr);
    }

    public void getPreferredVoicePrivacy(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE,
                result);
        send(rr);
    }

    public void
    separateConnection (int gsmIndex, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SEPARATE_CONNECTION, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + " " + gsmIndex);

        rr.mp.writeInt(1);
        rr.mp.writeInt(gsmIndex);

        handleChldRelatedRequest(rr);
    }

    public void
    acceptCall (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_ANSWER, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    rejectCall (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_UDUB, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    explicitCallTransfer (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_EXPLICIT_CALL_TRANSFER, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        handleChldRelatedRequest(rr);
    }

    public void
    getLastCallFailCause (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_LAST_CALL_FAIL_CAUSE, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * @deprecated
     */
    public void
    getLastPdpFailCause (Message result) {
        getLastDataCallFailCause (result);
    }

    /**
     * The preferred new alternative to getLastPdpFailCause
     */
    public void
    getLastDataCallFailCause (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    setMute (boolean enableMute, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_MUTE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + " " + enableMute);

        rr.mp.writeInt(1);
        rr.mp.writeInt(enableMute ? 1 : 0);

        send(rr);
    }

    public void
    getMute (Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_GET_MUTE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getSignalStrength (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SIGNAL_STRENGTH, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getNitzTime (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_GET_NITZ_TIME, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getVoiceRegistrationState (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_VOICE_REGISTRATION_STATE, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getDataRegistrationState (Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_DATA_REGISTRATION_STATE, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getOperator(Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_OPERATOR, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    sendDtmf(char c, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_DTMF, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeString(Character.toString(c));

        send(rr);
    }

    public void
    startDtmf(char c, Message result) {
        /// M: Solve [ALPS00334963][Rose][6577JB][Free Test][Network]The SIM card network lost and the information of SIM card shows as "No service" in the unlock screen when we input many numbers ceaselessly until the call end automatically(3/3). @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        synchronized (mDtmfQueue) {
            if (mIsSendChldRequest == false && mDtmfQueue.size() < MAXIMUM_DTMF_REQUEST) {
            //MTK-START [mtk08470][130121][ALPS00445268] Block non-sequence DTMF input
                if (mDtmfStatus == DTMF_STATUS_STOP) {
           RILRequest rr = RILRequest.obtain(RIL_REQUEST_DTMF_START, result);
                    
           rr.mp.writeString(Character.toString(c));
                    mDtmfStatus = DTMF_STATUS_START;
                    mDtmfQueue.addElement(rr);
                    if(mDtmfQueue.size() == 1) {
                        riljLog("send start dtmf");
                        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
           send(rr);
            }
                } else {
                    riljLog("DTMF status conflict, want to start DTMF when status is " + mDtmfStatus);	
            }
            //MTK-END [mtk08470][130121][ALPS00446908]MTK added
        }
        else {
               riljLog("reject start DTMF, mIsSendChldRequest = " + mIsSendChldRequest);
            }
        }
        /// @}
    }

    public void
    stopDtmf(Message result) {
        /// M: Solve [ALPS00334963][Rose][6577JB][Free Test][Network]The SIM card network lost and the information of SIM card shows as "No service" in the unlock screen when we input many numbers ceaselessly until the call end automatically(3/3). @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        synchronized (mDtmfQueue) {
            if (mIsSendChldRequest == false && mDtmfQueue.size() < MAXIMUM_DTMF_REQUEST) {
            //MTK-START [mtk08470][130121][ALPS00445268] Block non-sequence DTMF input
                if (mDtmfStatus == DTMF_STATUS_START) {
           RILRequest rr = RILRequest.obtain(RIL_REQUEST_DTMF_STOP, result);

                    mDtmfStatus = DTMF_STATUS_STOP;
                    mDtmfQueue.addElement(rr);
                    if(mDtmfQueue.size() == 1) {
                        riljLog("send stop dtmf");
                        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
           send(rr);
            }
                } else {
                    riljLog("DTMF status conflict, want to start DTMF when status is " + mDtmfStatus);	
            }
            //MTK-END [mtk08470][130121][ALPS00446908]MTK added
        }
        else {
               riljLog("reject stop DTMF, mIsSendChldRequest = " + mIsSendChldRequest);
            }
        }
        /// @}
    }

    public void
    sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_BURST_DTMF, result);

        rr.mp.writeInt(3);
        rr.mp.writeString(dtmfString);
        rr.mp.writeString(Integer.toString(on));
        rr.mp.writeString(Integer.toString(off));

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + dtmfString);

        send(rr);
    }

    public void
    sendSMS (String smscPDU, String pdu, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SEND_SMS, result);

        rr.mp.writeInt(2);
        rr.mp.writeString(smscPDU);
        rr.mp.writeString(pdu);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    sendCdmaSms(byte[] pdu, Message result) {
        int address_nbr_of_digits;
        int subaddr_nbr_of_digits;
        int bearerDataLength;
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);

        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_CDMA_SEND_SMS, result);

        try {
            rr.mp.writeInt(dis.readInt()); //teleServiceId
            rr.mp.writeByte((byte) dis.readInt()); //servicePresent
            rr.mp.writeInt(dis.readInt()); //serviceCategory
            rr.mp.writeInt(dis.read()); //address_digit_mode
            rr.mp.writeInt(dis.read()); //address_nbr_mode
            rr.mp.writeInt(dis.read()); //address_ton
            rr.mp.writeInt(dis.read()); //address_nbr_plan
            address_nbr_of_digits = (byte) dis.read();
            rr.mp.writeByte((byte) address_nbr_of_digits);
            for(int i=0; i < address_nbr_of_digits; i++){
                rr.mp.writeByte(dis.readByte()); // address_orig_bytes[i]
            }
            rr.mp.writeInt(dis.read()); //subaddressType
            rr.mp.writeByte((byte) dis.read()); //subaddr_odd
            subaddr_nbr_of_digits = (byte) dis.read();
            rr.mp.writeByte((byte) subaddr_nbr_of_digits);
            for(int i=0; i < subaddr_nbr_of_digits; i++){
                rr.mp.writeByte(dis.readByte()); //subaddr_orig_bytes[i]
            }

            bearerDataLength = dis.read();
            rr.mp.writeInt(bearerDataLength);
            for(int i=0; i < bearerDataLength; i++){
                rr.mp.writeByte(dis.readByte()); //bearerData[i]
            }
        }catch (IOException ex){
            if (RILJ_LOGD) riljLog("sendSmsCdma: conversion from input stream to object failed: "
                    + ex);
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void deleteSmsOnSim(int index, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DELETE_SMS_ON_SIM,
                response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(index);

        if (false) {
            if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                    + requestToString(rr.mRequest)
                    + " " + index);
        }

        send(rr);
    }

    public void deleteSmsOnRuim(int index, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM,
                response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(index);

        if (false) {
            if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                    + requestToString(rr.mRequest)
                    + " " + index);
        }

        send(rr);
    }

    public void writeSmsToSim(int status, String smsc, String pdu, Message response) {
        status = translateStatus(status);

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_WRITE_SMS_TO_SIM,
                response);

        rr.mp.writeInt(status);
        rr.mp.writeString(pdu);
        rr.mp.writeString(smsc);

        if (false) {
            if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                    + requestToString(rr.mRequest)
                    + " " + status);
        }

        send(rr);
    }

    public void writeSmsToRuim(int status, String pdu, Message response) {
        byte bytepdu[] = IccUtils.hexStringToBytes(pdu);
        if (RILJ_LOGD) riljLog("writeSmsToRuim() "+ " PDU: "+ new String(bytepdu));
        int address_nbr_of_digits;
        int subaddr_nbr_of_digits;
        int bearerDataLength;
        ByteArrayInputStream bais = new ByteArrayInputStream(bytepdu);
        DataInputStream dis = new DataInputStream(bais);

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM,
                response);

        status = translateStatus(status);
        rr.mp.writeInt(status);
        try {
            rr.mp.writeInt(dis.readInt()); //teleServiceId
            rr.mp.writeByte((byte) dis.readInt()); //servicePresent
            rr.mp.writeInt(dis.readInt()); //serviceCategory
            rr.mp.writeInt(dis.read()); //address_digit_mode
            rr.mp.writeInt(dis.read()); //address_nbr_mode
            rr.mp.writeInt(dis.read()); //address_ton
            rr.mp.writeInt(dis.read()); //address_nbr_plan
            address_nbr_of_digits = (byte) dis.read();
            rr.mp.writeByte((byte) address_nbr_of_digits);
            for(int i=0; i < address_nbr_of_digits; i++){
                rr.mp.writeByte(dis.readByte()); // address_orig_bytes[i]
            }
            rr.mp.writeInt(dis.read()); //subaddressType
            rr.mp.writeByte((byte) dis.read()); //subaddr_odd
            subaddr_nbr_of_digits = (byte) dis.read();
            rr.mp.writeByte((byte) subaddr_nbr_of_digits);
            for(int i=0; i < subaddr_nbr_of_digits; i++){
                rr.mp.writeByte(dis.readByte()); //subaddr_orig_bytes[i]
            }

            bearerDataLength = dis.read();
            rr.mp.writeInt(bearerDataLength);
            for(int i=0; i < bearerDataLength; i++){
                rr.mp.writeByte(dis.readByte()); //bearerData[i]
            }
        }catch (IOException ex){
            if (RILJ_LOGD) riljLog("writeSmsToRuim: conversion from input stream to object failed: "
                    + ex);
        }

        if (false) {
            if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                    + requestToString(rr.mRequest)
                    + " " + status);
        }

        send(rr);
    }

    /**
     *  Translates EF_SMS status bits to a status value compatible with
     *  SMS AT commands.  See TS 27.005 3.1.
     */
    private int translateStatus(int status) {
        switch(status & 0x7) {
            case SmsManager.STATUS_ON_ICC_READ:
                return 1;
            case SmsManager.STATUS_ON_ICC_UNREAD:
                return 0;
            case SmsManager.STATUS_ON_ICC_SENT:
                return 3;
            case SmsManager.STATUS_ON_ICC_UNSENT:
                return 2;
        }

        // Default to READ.
        return 1;
    }

    static int get3GSimId() {
        if(FeatureOption.MTK_GEMINI_3G_SWITCH){
            int simId = SystemProperties.getInt("gsm.3gswitch", 0); 
            if((simId > 0)&& (simId <= PhoneConstants.GEMINI_SIM_NUM)){
                return (simId -1); //Property value shall be 1~4,  convert to PhoneConstants.GEMINI_SIM_x
            }else{
                Log.w(LOG_TAG, "get3GSimId() got invalid property value:"+ simId);
            }
        } 

        return PhoneConstants.GEMINI_SIM_1; 					
    }


    //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
    /**
     * The preferred new alternative to setupDefaultPDP that is
     * CDMA-compatible.
     *
     */
    public void
    setupDataCall(String radioTechnology, String profile, String apn,
            String user, String password, String authType, String protocol,
            Message result) {
        /* [Note by mtk01411] In original Android2.1 release: MAX PDP Connection is 1 
        * request_cid is only allowed to set as "1" manually 
        */ 
        String request_cid = "1";
        setupDataCall(radioTechnology, profile, apn, user, password, authType, protocol, request_cid, result);
    }

    /**
     * The preferred new alternative to setupDefaultPDP that is
     * CDMA-compatible.
     *
     */
    public void
    setupDataCall(String radioTechnology, String profile, String apn,
            String user, String password, String authType, String protocol, String requestCid, 
            Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SETUP_DATA_CALL, result);

        /* [Note by mtk01411] Currently, MAX PDP Connection is 1: request_cid is only allowed to set as "1" manually
        * But for Multiple PDP Connecitons Feature:
        * PdpConnection.java connect() shoud invoke this 8-parms version's setupDataCall() directly
        * request_cid is obtained from the parameter paased into the setupDataCall() 
        */

        /* [Note by mtk01411] Change from 6 to 7: Due to add one cid field as the last one parameter */
        rr.mp.writeInt(8);
        rr.mp.writeString(radioTechnology);
        rr.mp.writeString(profile);
        rr.mp.writeString(apn);
        rr.mp.writeString(user);
        rr.mp.writeString(password);
        rr.mp.writeString(authType);
        /* [Add by mtk01411]
        * MAX PDP Connection =1:Only cid string "1" is allowed for RILD
        * MAX PDP Connection =2:Only cid string "1" or "2" is allowed for RILD
        * MAX PDP Connection =3:Only cid string "1" or "2" or "3" is allowed for our RILD 
        */
        rr.mp.writeString(protocol);
        rr.mp.writeString(requestCid); 

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                + requestToString(rr.mRequest) + " radioTech=" + radioTechnology + " profile="
                + profile + " apn=" + apn + " user=" + user + " password="
                + password + " authType=" + authType + " protocol=" + protocol + " requestCid=" + requestCid);

        send(rr);
    }

    public void
    deactivateDataCall(int cid, int reason, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_DEACTIVATE_DATA_CALL, result);

        rr.mp.writeInt(2);
        rr.mp.writeString(Integer.toString(cid));
        rr.mp.writeString(Integer.toString(reason));

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " +
                requestToString(rr.mRequest) + " " + cid + " " + reason);

        send(rr);
    }

    public void
    setRadioPower(boolean on, Message result) {
        //MTK-START [mtk03851][111217]
        //if radio is OFF set preferred NW type and cmda subscription
        if(mInitialRadioStateChange) {
            synchronized (mStateMonitor) {
                if (!mState.isOn()) {
                    RILRequest rrPnt = RILRequest.obtain(
                                   RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE, null);
                    int ratOnlyStart = 100;

                    rrPnt.mp.writeInt(1);
                    //ALPS00282643
                    if(FeatureOption.MTK_RAT_WCDMA_PREFERRED == false)
                        rrPnt.mp.writeInt(mPreferredNetworkType + ratOnlyStart);
                    else
                        rrPnt.mp.writeInt(mPreferredNetworkType);
                    if (RILJ_LOGD) riljLog(rrPnt.serialString() + "> "
                        + requestToString(rrPnt.mRequest) + " : " + mPreferredNetworkType);

                    send(rrPnt);

                    /* Temp marked
                    RILRequest rrCs = RILRequest.obtain(
                                   RIL_REQUEST_CDMA_SET_SUBSCRIPTION, null);
                    rrCs.mp.writeInt(1);
                    rrCs.mp.writeInt(mCdmaSubscription);
                    if (RILJ_LOGD) riljLog(rrCs.serialString() + "> "
                    + requestToString(rrCs.mRequest) + " : " + mCdmaSubscription);
                    send(rrCs);*/
                }
            }
        }
        //MTK-ENDED [mtk03851][111217]
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RADIO_POWER, result);

        rr.mp.writeInt(1);
        rr.mp.writeInt(on ? 1 : 0);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + (on ? " on" : " off"));
        }

        send(rr);
    }

    public void
    setSuppServiceNotifications(boolean enable, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION, result);

        rr.mp.writeInt(1);
        rr.mp.writeInt(enable ? 1 : 0);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SMS_ACKNOWLEDGE, result);

        rr.mp.writeInt(2);
        rr.mp.writeInt(success ? 1 : 0);
        rr.mp.writeInt(cause);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + success + " " + cause);

        send(rr);
    }

    public void
    acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE, result);

        rr.mp.writeInt(success ? 0 : 1); //RIL_CDMA_SMS_ErrorClass
        // cause code according to X.S004-550E
        rr.mp.writeInt(cause);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + success + " " + cause);

        send(rr);
    }

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    public void
    acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU, result);

        rr.mp.writeInt(2);
        rr.mp.writeString(success ? "1" : "0");
        rr.mp.writeString(ackPdu);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + ' ' + success + " [" + ackPdu + ']');

        send(rr);
    }
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

    public void
    iccIO (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, Message result) {
        iccIOForApp(command, fileid, path, p1, p2, p3, data, pin2, null, result);
    }

    public void
    iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SIM_IO, result);

        rr.mp.writeInt(command);
        rr.mp.writeInt(fileid);
        rr.mp.writeString(path);
        rr.mp.writeInt(p1);
        rr.mp.writeInt(p2);
        rr.mp.writeInt(p3);
        rr.mp.writeString(data);
        rr.mp.writeString(pin2);
        rr.mp.writeString(aid);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> iccIO: "
                + requestToString(rr.mRequest)
                + " 0x" + Integer.toHexString(command)
                + " 0x" + Integer.toHexString(fileid) + " "
                + " path: " + path + ","
                + p1 + "," + p2 + "," + p3
                + " aid: " + aid);

        send(rr);
    }

    public void
    getCLIR(Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_GET_CLIR, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    setCLIR(int clirMode, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_CLIR, result);

        // count ints
        rr.mp.writeInt(1);

        rr.mp.writeInt(clirMode);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + clirMode);

        send(rr);
    }

    public void
    queryCallWaiting(int serviceClass, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_QUERY_CALL_WAITING, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(serviceClass);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + serviceClass);

        send(rr);
    }

    public void
    setCallWaiting(boolean enable, int serviceClass, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_CALL_WAITING, response);

        rr.mp.writeInt(2);
        rr.mp.writeInt(enable ? 1 : 0);
        rr.mp.writeInt(serviceClass);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + enable + ", " + serviceClass);

        send(rr);
    }

    public void
    setNetworkSelectionModeAutomatic(Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC,
                                    response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    setNetworkSelectionModeManual(String operatorNumeric, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL,
                                    response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + operatorNumeric);

        rr.mp.writeString(operatorNumeric);

        send(rr);
    }

    public void
    getNetworkSelectionMode(Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE,
                                    response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getAvailableNetworks(Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_QUERY_AVAILABLE_NETWORKS,
                                    response);

        getAvailableNetworkRequests++;
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " requests: " + getAvailableNetworkRequests);

        send(rr);
    }

    public void
    setCallForward(int action, int cfReason, int serviceClass,
                String number, int timeSeconds, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_CALL_FORWARD, response);

        rr.mp.writeInt(action);
        rr.mp.writeInt(cfReason);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(PhoneNumberUtils.toaFromString(number));
        //MTK-START [mtk04070][111121][ALPS00093395]Check if number is null or not
        if (number != null) {
        rr.mp.writeString(number);
        } else {
            rr.mp.writeString("");
        }
        //MTK-END [mtk04070][111121][ALPS00093395]Check if number is null or not
        rr.mp.writeInt (timeSeconds);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + action + " " + cfReason + " " + serviceClass
                    + timeSeconds);

        send(rr);
    }

    public void
    queryCallForwardStatus(int cfReason, int serviceClass,
                String number, Message response) {
        RILRequest rr
            = RILRequest.obtain(RIL_REQUEST_QUERY_CALL_FORWARD_STATUS, response);

        rr.mp.writeInt(2); // 2 is for query action, not in used anyway
        rr.mp.writeInt(cfReason);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(PhoneNumberUtils.toaFromString(number));
        //MTK-START [mtk04070][111121][ALPS00093395]Check if number is null or not
        if (number != null) {
        rr.mp.writeString(number);
        } else {
            rr.mp.writeString("");
        }
        //MTK-END [mtk04070][111121][ALPS00093395]Check if number is null or not
        rr.mp.writeInt (0);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + cfReason + " " + serviceClass);

        send(rr);
    }

    public void
    queryCLIP(Message response) {
        RILRequest rr
            = RILRequest.obtain(RIL_REQUEST_QUERY_CLIP, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }


    public void
    getBasebandVersion (Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_BASEBAND_VERSION, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override
    public void
    queryFacilityLock(String facility, String password, int serviceClass,
                            Message response) {
        queryFacilityLockForApp(facility, password, serviceClass, null, response);
    }

    @Override
    public void
    queryFacilityLockForApp(String facility, String password, int serviceClass, String appId,
                            Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_QUERY_FACILITY_LOCK, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                                 + " [" + facility + " " + serviceClass
                                                 + " " + appId + "]");

        // count strings
        rr.mp.writeInt(4);

        rr.mp.writeString(facility);
        //MTK-START [mtk04070][111121][ALPS00093395]Check if password is null or not
        if (password != null) {
        rr.mp.writeString(password);
        } else {
            rr.mp.writeString("");
        }
        //MTK-END [mtk04070][111121][ALPS00093395]Check if password is null or not

        rr.mp.writeString(Integer.toString(serviceClass));
        rr.mp.writeString(appId);

        send(rr);
    }

    @Override
    public void
    setFacilityLock (String facility, boolean lockState, String password,
                        int serviceClass, Message response) {
        setFacilityLockForApp(facility, lockState, password, serviceClass, null, response);
    }

    @Override
    public void
    setFacilityLockForApp(String facility, boolean lockState, String password,
                        int serviceClass, String appId, Message response) {
        String lockString;
         RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_FACILITY_LOCK, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                                 + " [" + facility + " " + lockState
                                                 + " " + serviceClass + " " + appId + "]");

        // count strings
        rr.mp.writeInt(5);
        rr.mp.writeString(facility);
        lockString = (lockState)?"1":"0";
        rr.mp.writeString(lockString);
        //MTK-START [mtk04070][111121][ALPS00093395]Check if password is null or not
        if (null != password) {
        rr.mp.writeString(password);
        } else {
            rr.mp.writeString("");
        }
        //MTK-END [mtk04070][111121][ALPS00093395]Check if password is null or not
        rr.mp.writeString(Integer.toString(serviceClass));
        rr.mp.writeString(appId);

        send(rr);

    }

    public void
    sendUSSD (String ussdString, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SEND_USSD, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + " " + ussdString);

        rr.mp.writeString(ussdString);

        send(rr);
    }

    // inherited javadoc suffices
    public void cancelPendingUssd (Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_CANCEL_USSD, response);

        if (RILJ_LOGD) riljLog(rr.serialString()
                + "> " + requestToString(rr.mRequest));

        send(rr);
    }


    public void resetRadio(Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_RESET_RADIO, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_OEM_HOOK_RAW, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
               + "[" + IccUtils.bytesToHexString(data) + "]");

        rr.mp.writeByteArray(data);

        send(rr);

    }

    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_OEM_HOOK_STRINGS, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeStringArray(strings);

        send(rr);
    }

     /**
     * Assign a specified band for RF configuration.
     *
     * @param bandMode one of BM_*_BAND
     * @param response is callback message
     */
    public void setBandMode (int bandMode, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_BAND_MODE, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(bandMode);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                 + " " + bandMode);

        send(rr);
     }

    /**
     * Query the list of band mode supported by RF.
     *
     * @param response is callback message
     *        ((AsyncResult)response.obj).result  is an int[] with every
     *        element representing one avialable BM_*_BAND
     */
    public void queryAvailableBandMode (Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE,
                response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void sendTerminalResponse(String contents, Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeString(contents);
        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void sendEnvelope(String contents, Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeString(contents);
        send(rr);
    }

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * {@inheritDoc}
     */
    public void sendEnvelopeWithStatus(String contents, Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + '[' + contents + ']');

        rr.mp.writeString(contents);
        send(rr);
    }
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3


    //MTK-START [mtk04070][111121][ALPS00093395]Add a parameter - resCode
    /**
     * {@inheritDoc}
     */
    public void handleCallSetupRequestFromSim(
            boolean accept, int resCode, Message response) {

        RILRequest rr = RILRequest.obtain(
            RILConstants.RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM,
            response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        int[] param = new int[1];
		
        if (resCode == 0x21 || resCode == 0x20) {
            param[0] = resCode;
    	} else {
        param[0] = accept ? 1 : 0;
    	}

        rr.mp.writeIntArray(param);
        send(rr);
    }
    //MTK-END [mtk04070][111121][ALPS00093395]Add a parameter - resCode

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentPreferredNetworkType() {
        if (RILJ_LOGD) riljLog("setCurrentPreferredNetworkType: " + mPreferredNetworkType);
        setPreferredNetworkType(mPreferredNetworkType, null);
    }
    
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

    /**
     * {@inheritDoc}
     */
    public void setPreferredNetworkType(int networkType , Message response) {
    	int ratOnlyStart = 100;
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE, response);

        rr.mp.writeInt(1);
        //ALPS00282643
        if(FeatureOption.MTK_RAT_WCDMA_PREFERRED == false)
            rr.mp.writeInt(networkType + ratOnlyStart);
        else
        rr.mp.writeInt(networkType);

        mPreferredNetworkType = networkType;
       
        // ALPS00311194 WCDMA/GSM preferred for Single Card
        if (FeatureOption.MTK_GEMINI_SUPPORT == false)
            Settings.Global.putInt(mContext.getContentResolver(),Settings.Global.PREFERRED_NETWORK_MODE, networkType);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + networkType + " network preferred = " + FeatureOption.MTK_RAT_WCDMA_PREFERRED);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void getPreferredNetworkType(Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void getNeighboringCids(Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_GET_NEIGHBORING_CELL_IDS, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setLocationUpdates(boolean enable, Message response) {
        //MTK-START [mtk04070][111207][ALPS00093395]Consider screen on/off state
    	if (isScreenOn && false == enable) return;
        //MTK-END [mtk04070][111207][ALPS00093395]Consider screen on/off state
    	
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_LOCATION_UPDATES, response);
        rr.mp.writeInt(1);
        rr.mp.writeInt(enable ? 1 : 0);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                + requestToString(rr.mRequest) + ": " + enable);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void getSmscAddress(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_SMSC_ADDRESS, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setSmscAddress(String address, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_SMSC_ADDRESS, result);

        rr.mp.writeString(address);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + address);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void reportSmsMemoryStatus(boolean available, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_REPORT_SMS_MEMORY_STATUS, result);
        rr.mp.writeInt(1);
        rr.mp.writeInt(available ? 1 : 0);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                + requestToString(rr.mRequest) + ": " + available);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void reportStkServiceIsRunning(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void getGsmBroadcastConfig(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GSM_GET_BROADCAST_CONFIG, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GSM_SET_BROADCAST_CONFIG, response);

        int numOfConfig = config.length;
        rr.mp.writeInt(numOfConfig);

        for(int i = 0; i < numOfConfig; i++) {
            rr.mp.writeInt(config[i].getFromServiceId());
            rr.mp.writeInt(config[i].getToServiceId());
            rr.mp.writeInt(config[i].getFromCodeScheme());
            rr.mp.writeInt(config[i].getToCodeScheme());
            rr.mp.writeInt(config[i].isSelected() ? 1 : 0);
        }

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " with " + numOfConfig + " configs : ");
            for (int i = 0; i < numOfConfig; i++) {
                riljLog(config[i].toString());
            }
        }

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setGsmBroadcastActivation(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GSM_BROADCAST_ACTIVATION, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(activate ? 0 : 1);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    //***** Private Methods

    private void sendScreenState(boolean on) {
        //MTK-START [mtk04070][111207][ALPS00093395]Store screen on/off state
    	isScreenOn = on;
        //MTK-END [mtk04070][111207][ALPS00093395]Store screen on/off state
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SCREEN_STATE, null);
        rr.mp.writeInt(1);
        rr.mp.writeInt(on ? 1 : 0);

        if (RILJ_LOGD) riljLog(rr.serialString()
                + "> " + requestToString(rr.mRequest) + ": " + on);

        send(rr);
    }

    protected void
    onRadioAvailable() {
        // In case screen state was lost (due to process crash),
        // this ensures that the RIL knows the correct screen state.

        // TODO: Should query Power Manager and send the actual
        // screen state.  Just send true for now.
        /* ALPS00453878  START : Android default code suggest to set screen state according to the query actual screen state from PM */
        if(pm.isScreenOn() == false){
            Log.i(LOG_TAG, "onRadioAvailable screen state is OFF");        
            sendScreenState(false);
        }else{			
            Log.i(LOG_TAG, "onRadioAvailable screen state is ON");        
            sendScreenState(true);
        }
        /* ALPS00453878  END */		
   }

    private RadioState getRadioStateFromInt(int stateInt) {
        RadioState state;

        /* RIL_RadioState ril.h */
        switch(stateInt) {
            case 0: state = RadioState.RADIO_OFF; break;
            case 1: state = RadioState.RADIO_UNAVAILABLE; break;
            case 2: state = RadioState.SIM_NOT_READY; break;
            case 3: state = RadioState.SIM_LOCKED_OR_ABSENT; break;
            case 4: state = RadioState.SIM_READY; break;
            case 5: state = RadioState.RUIM_NOT_READY; break;
            case 6: state = RadioState.RUIM_READY; break;
            case 7: state = RadioState.RUIM_LOCKED_OR_ABSENT; break;
            case 8: state = RadioState.NV_NOT_READY; break;
            case 9: state = RadioState.NV_READY; break;

            default:
                throw new RuntimeException(
                            "Unrecognized RIL_RadioState: " + stateInt);
        }
        return state;
    }

    private void switchToRadioState(RadioState newState) {
        //setRadioState(newState);
        //MTK-START [mtk03851][111217]
        if (mInitialRadioStateChange) {
            int m3GsimId = PhoneConstants.GEMINI_SIM_1;
            if (FeatureOption.MTK_GEMINI_3G_SWITCH){
////                mIs3GSwitch = SystemProperties.getInt("gsm.3gswitch", 1) >= 2; 
                m3GsimId =  get3GSimId();
                mIs3GSwitch = (m3GsimId >= PhoneConstants.GEMINI_SIM_2); 
            }
////            if ((mIs3GSwitch && mySimId == PhoneConstants.GEMINI_SIM_2) || (!mIs3GSwitch && mySimId == PhoneConstants.GEMINI_SIM_1)) {
            if ((mIs3GSwitch && (mySimId == m3GsimId)) || (!mIs3GSwitch && (mySimId == PhoneConstants.GEMINI_SIM_1))) {
                if (newState.isOn()) {
                    /* If this is our first notification, make sure the radio
                     * is powered off.  This gets the radio into a known state,
                     * since it's possible for the phone proc to have restarted
                     * (eg, if it or the runtime crashed) without the RIL
                     * and/or radio knowing.
                     */
                    Log.i(LOG_TAG, "Radio ON @ init; reset to OFF");
                    if (FeatureOption.MTK_GEMINI_SUPPORT != true) {
                        setRadioPower(false, null);
                    } else {
                        setRadioMode(0, null);
                    }
                } else {
                    Log.i(LOG_TAG, "Radio OFF @ init 3G phone");
                    setRadioState(newState);
                }
                mInitialRadioStateChange = false;
                if (FeatureOption.MTK_VT3G324M_SUPPORT == false) {
                    disableVTCapability();
                }
            }else {
                Log.i(LOG_TAG, "Radio OFF @ init 2G phone");
                setRadioState(newState);
            }
        } else {
            setRadioState(newState);
        }
        //MTK-START [mtk03851][111217]
    }

    /**
     * Holds a PARTIAL_WAKE_LOCK whenever
     * a) There is outstanding RIL request sent to RIL deamon and no replied
     * b) There is a request pending to be sent out.
     *
     * There is a WAKE_LOCK_TIMEOUT to release the lock, though it shouldn't
     * happen often.
     */

    private void
    acquireWakeLock() {
        synchronized (mWakeLock) {
            mWakeLock.acquire();
            mRequestMessagesPending++;

            mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            Message msg = mSender.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
            mSender.sendMessageDelayed(msg, mWakeLockTimeout);
        }
    }

    private void
    releaseWakeLockIfDone() {
        synchronized (mWakeLock) {
            if (mWakeLock.isHeld() &&
                (mRequestMessagesPending == 0) &&
                //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
                (mRequestsList.size() == 0)) {
                //MTK-END [mtk04070][111121][ALPS00093395]MTK modified
                mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
                mWakeLock.release();
            }
        }
    }

    private void
    send(RILRequest rr) {
        Message msg;

        boolean show = (requestToString(rr.mRequest).compareTo("LAST_CALL_FAIL_CAUSE") == 0);
        if (show) riljLog("###send start");

        if (mSocket == null) {
           if (show) riljLog("###mSocket == null");
            rr.onError(RADIO_NOT_AVAILABLE, null);
            rr.release();
            return;
        }

        if (show) riljLog("###send 1");
        msg = mSender.obtainMessage(EVENT_SEND, rr);
        if (show) riljLog("###send 2");

        acquireWakeLock();
        if (show) riljLog("###send 3");

        msg.sendToTarget();
        if (show) riljLog("###send end");
    }

    private void
    processResponse (Parcel p) {
        int type;

        type = p.readInt();

        /* Solve [ALPS00308613]Receive incoming VT call causes EE, mtk04070, 20120628 */
        if (mIsFirstResponse && 
            ((type == RESPONSE_UNSOLICITED) || (type == RESPONSE_SOLICITED))) {
            if (FeatureOption.MTK_VT3G324M_SUPPORT == false) {
                Log.d(LOG_TAG,"FeatureOption.MTK_VT3G324M_SUPPORT == false");
                disableVTCapability();
            }
            mIsFirstResponse = false;
        }	

        if (type == RESPONSE_UNSOLICITED) {
            processUnsolicited (p);
        } else if (type == RESPONSE_SOLICITED) {
            processSolicited (p);
        }

        releaseWakeLockIfDone();
    }

    /**
     * Release each request in mReqeustsList then clear the list
     * @param error is the RIL_Errno sent back
     * @param loggable true means to print all requests in mRequestslist
     */
    private void clearRequestsList(int error, boolean loggable) {
        RILRequest rr;
        synchronized (mRequestsList) {
            int count = mRequestsList.size();
            if (RILJ_LOGD && loggable) {
                Log.d(LOG_TAG, "[clearRequestsList]WAKE_LOCK_TIMEOUT " +
                        " mReqPending=" + mRequestMessagesPending +
                        " mRequestList=" + count);
            }

            for (int i = 0; i < count ; i++) {
                rr = mRequestsList.get(i);
                if (RILJ_LOGD && loggable) {
                    Log.d(LOG_TAG, i + ": [" + rr.mSerial + "] " +
                            requestToString(rr.mRequest));
                }
                rr.onError(error, null);
                rr.release();
            }
            mRequestsList.clear();
            mRequestMessagesWaiting = 0;
        }
    }

    private RILRequest findAndRemoveRequestFromList(int serial) {
        synchronized (mRequestsList) {
            for (int i = 0, s = mRequestsList.size() ; i < s ; i++) {
                RILRequest rr = mRequestsList.get(i);

                if (rr.mSerial == serial) {
                    mRequestsList.remove(i);
                    if (mRequestMessagesWaiting > 0)
                        mRequestMessagesWaiting--;
                    return rr;
                }
            }
        }

        return null;
    }


    private void
    processSolicited (Parcel p) {
        int serial, error;
        boolean found = false;

        serial = p.readInt();
        error = p.readInt();

        RILRequest rr;

        rr = findAndRemoveRequestFromList(serial);

        if (rr == null) {
            Log.w(LOG_TAG, "Unexpected solicited response! sn: "
                            + serial + " error: " + error);
            return;
        }

        /// M: Solve [ALPS00334963][Rose][6577JB][Free Test][Network]The SIM card network lost and the information of SIM card shows as "No service" in the unlock screen when we input many numbers ceaselessly until the call end automatically(3/3). @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        if ((rr.mRequest == RIL_REQUEST_DTMF_START) || 
            (rr.mRequest == RIL_REQUEST_DTMF_STOP)) {
            synchronized (mDtmfQueue) {
                mDtmfQueue.remove(rr);
                riljLog("remove first item in dtmf queue done, size = " + mDtmfQueue.size());
                if(mDtmfQueue.size() > 0) {
                    RILRequest rr2 = (RILRequest)mDtmfQueue.get(0);
                    if (RILJ_LOGD) riljLog(rr2.serialString() + "> " + requestToString(rr2.mRequest));
                    send(rr2);
                } else {
                    if(mPendingCHLDRequest != null) {
                        riljLog("send pending switch request");
                        send(mPendingCHLDRequest);
                        mIsSendChldRequest = true;
                        mPendingCHLDRequest = null;
                    }
                }
            }
        }
        /// @}

        Object ret = null;

        if (rr.mRequest == RIL_REQUEST_QUERY_AVAILABLE_NETWORKS) {
            getAvailableNetworkRequests--;
            if (getAvailableNetworkRequests == 0) {
                mGetAvailableNetworkDoneRegistrant.notifyRegistrants();
            }
        }

        if (rr.mRequest == RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE ||
            rr.mRequest == RIL_REQUEST_CONFERENCE ||
            rr.mRequest == RIL_REQUEST_SEPARATE_CONNECTION ||
            rr.mRequest == RIL_REQUEST_EXPLICIT_CALL_TRANSFER) {
            riljLog("clear mIsSendChldRequest");
            mIsSendChldRequest = false;
        }
        
        if (error == 0 || p.dataAvail() > 0) {
            // either command succeeds or command fails but with data payload
            try {switch (rr.mRequest) {
            /*
 cat libs/telephony/ril_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: ret = \2(p); break;/'
             */

            case RIL_REQUEST_GET_SIM_STATUS: ret =  responseIccCardStatus(p); break;
            case RIL_REQUEST_DETECT_SIM_MISSING: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK2: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION: ret =  responseInts(p); break;
            case RIL_REQUEST_GET_CURRENT_CALLS: ret =  responseCallList(p); break;
            case RIL_REQUEST_DIAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMSI: ret =  responseString(p); break;
            case RIL_REQUEST_HANGUP: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: {
                if (mTestingEmergencyCall.getAndSet(false)) {
                    if (mEmergencyCallbackModeRegistrant != null) {
                        riljLog("testing emergency call, notify ECM Registrants");
                        mEmergencyCallbackModeRegistrant.notifyRegistrant();
                    }
                }
                ret =  responseVoid(p);
                break;
            }
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: {
                ret =  responseVoid(p); 
                break;
            }
            case RIL_REQUEST_CONFERENCE: {
                ret =  responseVoid(p); 
                break;
            }
            case RIL_REQUEST_UDUB: ret =  responseVoid(p); break;
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
            case RIL_REQUEST_SIGNAL_STRENGTH: ret =  responseSignalStrength(p); break;
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_OPERATOR: ret =  responseOperator(p); break;
            case RIL_REQUEST_RADIO_POWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: ret =  responseSMS(p); break;
            case RIL_REQUEST_SETUP_DATA_CALL: ret =  responseSetupDataCall(p); break;
            case RIL_REQUEST_SIM_IO: ret =  responseICC_IO(p); break;
            // NFC SEEK start
            case RIL_REQUEST_SIM_TRANSMIT_BASIC: ret =  responseICC_IO(p); break;
            case RIL_REQUEST_SIM_OPEN_CHANNEL: ret  = responseInts(p); break;
            case RIL_REQUEST_SIM_CLOSE_CHANNEL: ret  = responseVoid(p); break;
            case RIL_REQUEST_SIM_TRANSMIT_CHANNEL: ret = responseICC_IO(p); break;
            case RIL_REQUEST_SIM_GET_ATR: ret = responseString(p); break;
            case RIL_REQUEST_SIM_OPEN_CHANNEL_WITH_SW: ret  = responseICC_IO(p); break;
            // NFC SEEK end
            case RIL_REQUEST_SEND_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_CANCEL_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_CLIR: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CLIR: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: ret =  responseCallForward(p); break;
            case RIL_REQUEST_SET_CALL_FORWARD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_WAITING: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CALL_WAITING: ret =  responseVoid(p); break;
            case RIL_REQUEST_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMEI: ret =  responseString(p); break;
            case RIL_REQUEST_GET_IMEISV: ret =  responseString(p); break;
            case RIL_REQUEST_ANSWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : ret =  responseOperatorInfos(p); break;
            case RIL_REQUEST_DTMF_START: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF_STOP: ret =  responseVoid(p); break;
            case RIL_REQUEST_BASEBAND_VERSION: ret =  responseString(p); break;
            case RIL_REQUEST_SEPARATE_CONNECTION: {
                ret =  responseVoid(p); 
                break;
            }
            case RIL_REQUEST_SET_MUTE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_MUTE: ret =  responseInts(p); break;
            case RIL_REQUEST_QUERY_CLIP: ret =  responseInts(p); break;
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
            case RIL_REQUEST_DATA_CALL_LIST: ret =  responseDataCallList(p); break;
            case RIL_REQUEST_RESET_RADIO: ret =  responseVoid(p); break;
            case RIL_REQUEST_OEM_HOOK_RAW: ret =  responseRaw(p); break;
            case RIL_REQUEST_OEM_HOOK_STRINGS: ret =  responseStrings(p); break;
            case RIL_REQUEST_SCREEN_STATE: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_WRITE_SMS_TO_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_DELETE_SMS_ON_SIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_BAND_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_STK_GET_PROFILE: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SET_PROFILE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: {
                ret =  responseVoid(p); 
                break;
            }
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: ret =  responseGetPreferredNetworkType(p); break;
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: ret = responseCellList(p); break;
            case RIL_REQUEST_SET_LOCATION_UPDATES: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_TTY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_TTY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_FLASH: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BURST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: ret =  responseGmsBroadcastConfig(p); break;
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: ret =  responseCdmaBroadcastConfig(p); break;
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SUBSCRIPTION: ret =  responseStrings(p); break;
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEVICE_IDENTITY: ret =  responseStrings(p); break;
            case RIL_REQUEST_GET_SMSC_ADDRESS: ret = responseString(p); break;
            case RIL_REQUEST_SET_SMSC_ADDRESS: ret = responseVoid(p); break;
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: ret = responseVoid(p); break;
            // via support to add this RIL request
            case RIL_REQUEST_GET_NITZ_TIME: ret = responseGetNitzTime(p); break;
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: ret =  responseInts(p); break;
            case RIL_REQUEST_ISIM_AUTHENTICATION: ret =  responseString(p); break;
            case RIL_REQUEST_VOICE_RADIO_TECH: ret = responseInts(p); break;

            //MTK-START [mtk04070][111121][ALPS00093395]MTK added
            case RIL_REQUEST_EMERGENCY_DIAL: ret =  responseVoid(p); break;
            /* vt start */
            case RIL_REQUEST_VT_DIAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_VOICE_ACCEPT: ret = responseVoid(p); break;
            /* vt end */
            case RIL_REQUEST_HANGUP_ALL: ret =  responseVoid(p); break;		
            case RIL_REQUEST_HANGUP_ALL_EX: ret =  responseVoid(p); break;		
            case RIL_REQUEST_GET_CCM: ret =  responseString(p); break;
            case RIL_REQUEST_GET_ACM: ret =  responseString(p); break;
            case RIL_REQUEST_GET_ACMMAX: ret =  responseString(p); break;
            case RIL_REQUEST_GET_PPU_AND_CURRENCY: ret =  responseStrings(p); break;
            case RIL_REQUEST_SET_ACMMAX: ret =  responseVoid(p); break;
            case RIL_REQUEST_RESET_ACM: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_PPU_AND_CURRENCY: ret =  responseVoid(p); break;			
            case RIL_REQUEST_RADIO_POWEROFF: ret =  responseVoid(p); break;			
            case RIL_REQUEST_RADIO_POWERON: ret =  responseVoid(p); break;          
            case RIL_REQUEST_DUAL_SIM_MODE_SWITCH: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_GPRS_CONNECT_TYPE: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_GPRS_TRANSFER_TYPE: ret =  responseVoid(p); break;            
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT: ret =  responseVoid(p); break;			
            case RIL_REQUEST_GET_COLP: ret = responseInts(p); break;
            case RIL_REQUEST_SET_COLP: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_COLR: ret = responseInts(p); break;
            case RIL_REQUEST_QUERY_PHB_STORAGE_INFO: ret = responseInts(p); break;
            case RIL_REQUEST_WRITE_PHB_ENTRY: ret = responseVoid(p); break;
            case RIL_REQUEST_READ_PHB_ENTRY: ret = responsePhbEntries(p); break;
            case RIL_REQUEST_MOBILEREVISION_AND_IMEI: ret =  responseString(p); break;//Add by mtk80372 for Barcode Number
            case RIL_REQUEST_QUERY_SIM_NETWORK_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_SIM_NETWORK_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_SCRI: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_FD_MODE: ret = responseInts(p); break; //[New R8 modem FD]
            case RIL_REQUEST_BTSIM_CONNECT: ret = responseString(p); break;
            case RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF: ret = responseVoid(p); break;    
            case RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM: ret = responseString(p); break; 
            case RIL_REQUEST_BTSIM_TRANSFERAPDU: ret = responseString(p); break;
            case RIL_REQUEST_QUERY_ICCID: ret = responseString(p); break;
	    case RIL_REQUEST_SIM_AUTHENTICATION: ret =  responseString(p); break;
	    case RIL_REQUEST_USIM_AUTHENTICATION: ret =  responseString(p); break;
            case RIL_REQUEST_GET_SMS_SIM_MEM_STATUS: ret = responseSimSmsMemoryStatus(p); break;
            case RIL_REQUEST_FORCE_RELEASE_CALL: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_CALL_INDICATION: ret = responseVoid(p); break;
            case RIL_REQUEST_REPLACE_VT_CALL: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_3G_CAPABILITY: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_3G_CAPABILITY: ret =  responseInts(p); break;
            case RIL_REQUEST_GET_POL_CAPABILITY: ret = responseInts(p); break;
            case RIL_REQUEST_GET_POL_LIST: ret = responseNetworkInfoWithActs(p); break;
            case RIL_REQUEST_SET_POL_ENTRY: ret = responseVoid(p); break;
            case RIL_REQUEST_QUERY_UPB_CAPABILITY: ret = responseInts(p); break;
            case RIL_REQUEST_READ_UPB_GRP: ret = responseInts(p); break;
            case RIL_REQUEST_WRITE_UPB_GRP: ret = responseVoid(p); break;
            case RIL_REQUEST_EDIT_UPB_ENTRY: ret = responseVoid(p); break;
            case RIL_REQUEST_DELETE_UPB_ENTRY: ret = responseVoid(p); break;
            case RIL_REQUEST_READ_UPB_GAS_LIST: ret = responseStrings(p); break;
            case RIL_REQUEST_DISABLE_VT_CAPABILITY: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_SIM_RECOVERY_ON: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_SIM_RECOVERY_ON: ret = responseInts(p); break;
            //MTK-END [mtk04070][111121][ALPS00093395]MTK added

            case RIL_REQUEST_SET_TRM: ret = responseInts(p); break;//ALPS00248788

            //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU: ret = responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS: ret = responseICC_IO(p); break;
            //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

            //MTK-START [mtk80950][120410][ALPS00266631]check whether download calibration data or not
            case RIL_REQUEST_GET_CALIBRATION_DATA: ret = responseString(p); break;
            //MTK-END [mtk80950][120410][ALPS00266631]check whether download calibration data or not
            case RIL_REQUEST_GET_PHB_STRING_LENGTH: ret = responseInts(p); break;
            case RIL_REQUEST_GET_PHB_MEM_STORAGE : ret = responseGetPhbMemStorage(p); break;
            case RIL_REQUEST_SET_PHB_MEM_STORAGE : responseVoid(p); break;
            case RIL_REQUEST_READ_PHB_ENTRY_EXT: ret = responseReadPhbEntryExt(p); break;
            case RIL_REQUEST_WRITE_PHB_ENTRY_EXT: ret = responseVoid(p); break; 
	    case RIL_REQUEST_QUERY_UIM_INSERTED: ret = responseInts(p);break;
            
            // response for read/write EFsmsp
            case RIL_REQUEST_GET_SMS_PARAMS: ret = responseSmsParams(p); break;
            case RIL_REQUEST_SET_SMS_PARAMS: ret = responseVoid(p); break;
            
            // CB Extension
            case RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO:  ret = responseVoid(p); break;
            case RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_CB_CONFIG_INFO:          ret = responseCbConfig(p); break;
            case RIL_REQUEST_SET_ALL_CB_LANGUAGE_ON:      ret = responseVoid(p); break;
            // CB Extension

            case RIL_REQUEST_SET_ETWS: ret = responseVoid(p); break;
            case RIL_REQUEST_DETACH_PS: ret = responseVoid(p); break;
            
            default:
                throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
            //break;
            }} catch (Throwable tr) {
                // Exceptions here usually mean invalid RIL responses

                Log.w(LOG_TAG, rr.serialString() + "< "
                        + requestToString(rr.mRequest)
                        + " exception, possible invalid RIL response", tr);

                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, null, tr);
                    rr.mResult.sendToTarget();
                }
                rr.release();
                return;
            }
        }

        if (error != 0) {
            rr.onError(error, ret);
            rr.release();
            return;
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
            + " " + retToString(rr.mRequest, ret));

        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }

        rr.release();
    }

    private String
    retToString(int req, Object ret) {
        if (ret == null) return "";
        switch (req) {
            // Don't log these return values, for privacy's sake.
            case RIL_REQUEST_GET_IMSI:
            case RIL_REQUEST_GET_IMEI:
            case RIL_REQUEST_GET_IMEISV:
                if (!RILJ_LOGV) {
                    // If not versbose logging just return and don't display IMSI and IMEI, IMEISV
                    return "";
                }
        }

        StringBuilder sb;
        String s;
        int length;
        if (ret instanceof int[]){
            int[] intArray = (int[]) ret;
            length = intArray.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(intArray[i++]);
                while ( i < length) {
                    sb.append(", ").append(intArray[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else if (ret instanceof String[]) {
            String[] strings = (String[]) ret;
            length = strings.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(strings[i++]);
                while ( i < length) {
                    sb.append(", ").append(strings[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        }else if (req == RIL_REQUEST_GET_CURRENT_CALLS) {
            ArrayList<DriverCall> calls = (ArrayList<DriverCall>) ret;
            sb = new StringBuilder(" ");
            for (DriverCall dc : calls) {
                sb.append("[").append(dc).append("] ");
            }
            s = sb.toString();
        } else if (req == RIL_REQUEST_GET_NEIGHBORING_CELL_IDS) {
            ArrayList<NeighboringCellInfo> cells;
            cells = (ArrayList<NeighboringCellInfo>) ret;
            sb = new StringBuilder(" ");
            for (NeighboringCellInfo cell : cells) {
                sb.append(cell).append(" ");
            }
            s = sb.toString();
        } else {
            s = ret.toString();
        }
        return s;
    }

    private void
    processUnsolicited (Parcel p) {
        int response;
        Object ret;

        response = p.readInt();

        try {switch(response) {
/*
 cat libs/telephony/ril_unsol_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: \2(rr, p); break;/'
*/

            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED: ret =  responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED: ret =  responseStrings(p); break;//ALPS00283717
            case RIL_UNSOL_RESPONSE_NEW_SMS: ret =  responseString(p); break;
            case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT: ret =  responseString(p); break;
            case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM: ret =  responseInts(p); break;
            case RIL_UNSOL_ON_USSD: ret =  responseStrings(p); break;
            case RIL_UNSOL_NITZ_TIME_RECEIVED: ret =  responseString(p); break;
            case RIL_UNSOL_SIGNAL_STRENGTH: ret = responseSignalStrength(p); break;
            case RIL_UNSOL_DATA_CALL_LIST_CHANGED: ret = responseDataCallList(p);break;
            case RIL_UNSOL_SUPP_SVC_NOTIFICATION: ret = responseSuppServiceNotification(p); break;
            case RIL_UNSOL_STK_SESSION_END: ret = responseVoid(p); break;
            case RIL_UNSOL_STK_PROACTIVE_COMMAND: ret = responseString(p); break;
            case RIL_UNSOL_STK_EVENT_NOTIFY: ret = responseString(p); break;
            case RIL_UNSOL_STK_CALL_SETUP: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_SMS_STORAGE_FULL: ret =  responseVoid(p); break;
            case RIL_UNSOL_SIM_REFRESH: ret =  responseSimRefresh(p); break;
            case RIL_UNSOL_CALL_RING: ret =  responseCallRing(p); break;
            case RIL_UNSOL_RESTRICTED_STATE_CHANGED: ret = responseInts(p); break;
            case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED:  ret =  responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS:  ret =  responseCdmaSms(p); break;
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:  ret =  responseString(p); break;
            case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL:  ret =  responseVoid(p); break;
            case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
            case RIL_UNSOL_CDMA_CALL_WAITING: ret = responseCdmaCallWaiting(p); break;
            case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS: ret = responseInts(p); break;
            case RIL_UNSOL_CDMA_INFO_REC: ret = responseCdmaInformationRecord(p); break;
            case RIL_UNSOL_OEM_HOOK_RAW: ret = responseRaw(p); break;
            case RIL_UNSOL_RINGBACK_TONE: ret = responseInts(p); break;
            case RIL_UNSOL_RESEND_INCALL_MUTE: ret = responseVoid(p); break;
            case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED: ret = responseInts(p); break;
            case RIL_UNSOl_CDMA_PRL_CHANGED: ret = responseInts(p); break;
            case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
            case RIL_UNSOL_RIL_CONNECTED: ret = responseInts(p); break;
            case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED: ret =  responseInts(p); break;

            //MTK-START [mtk04070][111121][ALPS00093395]MTK added
            case RIL_UNSOL_NEIGHBORING_CELL_INFO: ret = responseStrings(p); break;			
            case RIL_UNSOL_NETWORK_INFO: ret = responseStrings(p); break;			
            case RIL_UNSOL_CALL_FORWARDING: ret = responseInts(p); break;
            case RIL_UNSOL_CRSS_NOTIFICATION: ret = responseCrssNotification(p); break;
            case RIL_UNSOL_CALL_PROGRESS_INFO: ret = responseStrings(p); break;			
            case RIL_UNSOL_PHB_READY_NOTIFICATION: ret = responseVoid(p); break;
            case RIL_UNSOL_SIM_INSERTED_STATUS: ret = responseInts(p); break;            
            case RIL_UNSOL_SIM_MISSING: ret = responseInts(p); break;   
            case RIL_UNSOL_SIM_RECOVERY: ret = responseInts(p); break;         
            case RIL_UNSOL_VIRTUAL_SIM_ON: ret = responseInts(p); break; 
            case RIL_UNSOL_VIRTUAL_SIM_OFF: ret = responseInts(p); break; 
            case RIL_UNSOL_SPEECH_INFO: ret = responseInts(p); break;			
            case RIL_UNSOL_RADIO_TEMPORARILY_UNAVAILABLE: ret = responseInts(p); break;	
            case RIL_UNSOL_ME_SMS_STORAGE_FULL: ret =  responseVoid(p); break;
            case RIL_UNSOL_SMS_READY_NOTIFICATION: ret = responseVoid(p); break;
            /* vt start */
            case RIL_UNSOL_VT_STATUS_INFO: ret = responseInts(p); break;
            case RIL_UNSOL_VT_RING_INFO: ret = responseVoid(p); break;
            /* vt end */
            case RIL_UNSOL_SCRI_RESULT: ret = responseInts(p); break;
            case RIL_UNSOL_GPRS_DETACH: ret = responseVoid(p); break;
            case RIL_UNSOL_INCOMING_CALL_INDICATION: ret = responseStrings(p); break;
            // ALPS00302698 ENS
            case RIL_UNSOL_EF_CSP_PLMN_MODE_BIT: ret = responseInts(p); break;
            //MTK-END [mtk04070][111121][ALPS00093395]MTK added
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED: ret =  responseVoid(p); break;
            case RIL_UNSOL_INVALID_SIM:  ret = responseStrings(p); break;
            case RIL_UNSOL_RESPONSE_ACMT: ret = responseInts(p); break;
            case RIL_UNSOL_IMEI_LOCK: ret = responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_PLUG_OUT: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_PLUG_IN: ret = responseInts(p); break;
            case RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION: ret = responseEtwsNotification(p); break;
            default:
                throw new RuntimeException("Unrecognized unsol response: " + response);
            //break; (implied)
        }} catch (Throwable tr) {
            Log.e(LOG_TAG, "Exception processing unsol response: " + response +
                "Exception:" + tr.toString());
            return;
        }

        switch(response) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
                /* has bonus radio state int */
                RadioState newState = getRadioStateFromInt(p.readInt());
                if (RILJ_LOGD) unsljLogMore(response, newState.toString());

                switchToRadioState(newState);
            break;
            case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED:
                if (RILJ_LOGD) unsljLog(response);

                mCallStateRegistrants
                    .notifyRegistrants(new AsyncResult(null, null, null));
            break;
            case RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED:
                //ALPS00283717
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                mVoiceNetworkStateRegistrants
					.notifyRegistrants(new AsyncResult (null, ret, null));
            break;
            case RIL_UNSOL_RESPONSE_NEW_SMS: {
                if (RILJ_LOGD) unsljLog(response);

                // FIXME this should move up a layer
                String a[] = new String[2];

                a[1] = (String)ret;

                SmsMessage sms;

                sms = SmsMessage.newFromCMT(a);
                if (mGsmSmsRegistrant != null) {
                    mGsmSmsRegistrant
                        .notifyRegistrant(new AsyncResult(null, sms, null));
                }
            break;
            }
            case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mSmsStatusRegistrant != null) {
                    mSmsStatusRegistrant.notifyRegistrant(
                            new AsyncResult(null, ret, null));
                }
            break;
            case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                int[] smsIndex = (int[])ret;

                if(smsIndex.length == 1) {
                    if (mSmsOnSimRegistrant != null) {
                        mSmsOnSimRegistrant.
                                notifyRegistrant(new AsyncResult(null, smsIndex, null));
                    }
                } else {
                    if (RILJ_LOGD) riljLog(" NEW_SMS_ON_SIM ERROR with wrong length "
                            + smsIndex.length);
                }
            break;
            case RIL_UNSOL_ON_USSD:
                String[] resp = (String[])ret;

                if (resp.length < 2) {
                    resp = new String[2];
                    resp[0] = ((String[])ret)[0];
                    resp[1] = null;
                }
                if (RILJ_LOGD) unsljLogMore(response, resp[0]);
                if (mUSSDRegistrant != null) {
                    mUSSDRegistrant.notifyRegistrant(
                        new AsyncResult (null, resp, null));
                }
            break;
            case RIL_UNSOL_NITZ_TIME_RECEIVED:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                // has bonus long containing milliseconds since boot that the NITZ
                // time was received
                long nitzReceiveTime = p.readLong();

                Object[] result = new Object[2];

                result[0] = ret;
                result[1] = Long.valueOf(nitzReceiveTime);

                boolean ignoreNitz = SystemProperties.getBoolean(
                        TelephonyProperties.PROPERTY_IGNORE_NITZ, false);

                if (ignoreNitz) {
                    if (RILJ_LOGD) riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
                    } else {
                    if (mNITZTimeRegistrant != null) {

                        mNITZTimeRegistrant
                            .notifyRegistrant(new AsyncResult (null, result, null));
                    } else {
                        // in case NITZ time registrant isnt registered yet
                        mLastNITZTimeInfo = result;
                    }
                }
            break;

            case RIL_UNSOL_SIGNAL_STRENGTH:
                // Note this is set to "verbose" because it happens
                // frequently
                if (RILJ_LOGV) unsljLogvRet(response, ret);

                if (mSignalStrengthRegistrant != null) {
                    mSignalStrengthRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
            break;
            case RIL_UNSOL_DATA_CALL_LIST_CHANGED:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                mDataNetworkStateRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
            break;

            case RIL_UNSOL_SUPP_SVC_NOTIFICATION:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mSsnRegistrant != null) {
                    mSsnRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_STK_SESSION_END:
                if (RILJ_LOGD) unsljLog(response);

                if (mCatSessionEndRegistrant != null) {
                    mCatSessionEndRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_STK_PROACTIVE_COMMAND:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCatProCmdRegistrant != null && mCatProCmdRegistrant.getHandler() != null) {
                    mCatProCmdRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                    try {
                        Xlog.d(LOG_TAG, "[CachedStk unregister mCatServiceInitListener");
                        mContext.unregisterReceiver(mCatServiceInitListener);
                    } catch(IllegalArgumentException e) {
                        Xlog.d(LOG_TAG, "[CachedStk mCatServiceInitListener has been unregistered");
                    } catch(Exception e2) {
                        Xlog.d(LOG_TAG, "[CachedStk uncaught exception");
                    }
                } else {
                    if (mCatProCmdRegistrant != null && mCatProCmdRegistrant.getHandler() == null)
                    {
                        Xlog.d(LOG_TAG, "[CachedStk register mCatServiceInitListener again");
                        IntentFilter filterForStkCachedCommand = new IntentFilter();
                        filterForStkCachedCommand.addAction(CatService.ACTION_CAT_INIT_DONE);
                        mContext.registerReceiver(mCatServiceInitListener, filterForStkCachedCommand);
                    }
                    Xlog.d(LOG_TAG, "[CachedStk cache proactive command");
                    mStkPciObject = ret;
                }
                break;

            case RIL_UNSOL_STK_EVENT_NOTIFY:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCatEventRegistrant != null) {
                    mCatEventRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_STK_CALL_SETUP:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCatCallSetUpRegistrant != null) {
                    mCatCallSetUpRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_SIM_SMS_STORAGE_FULL:
                if (RILJ_LOGD) unsljLog(response);

                if (mIccSmsFullRegistrant != null) {
                    mIccSmsFullRegistrant.notifyRegistrant();
                }
                break;

            case RIL_UNSOL_SIM_REFRESH:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mIccRefreshRegistrants != null) {
                    mIccRefreshRegistrants.notifyRegistrants(
                            new AsyncResult (null, ret, null));
                }

                //MTK-START [mtk04070][111121][ALPS00093395]MTK added
                // Sends REFRESH to StkService
                if (mCatProCmdRegistrant != null) {
                    mCatProCmdRegistrant.notifyRegistrant(
                    new AsyncResult (null, ret, null));
                }
                //MTK-END [mtk04070][111121][ALPS00093395]MTK added

                break;

            case RIL_UNSOL_CALL_RING:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mRingRegistrant != null) {
                    mRingRegistrant.notifyRegistrant(
                            new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_RESTRICTED_STATE_CHANGED:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mRestrictedStateRegistrant != null) {
                    mRestrictedStateRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED:
                if (RILJ_LOGD) unsljLog(response);

                if (mIccStatusChangedRegistrants != null) {
                    mIccStatusChangedRegistrants.notifyRegistrants();
                }
                break;

            case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS:
                if (RILJ_LOGD) unsljLog(response);

                SmsMessage sms = (SmsMessage) ret;

                if (mCdmaSmsRegistrant != null) {
                    mCdmaSmsRegistrant
                        .notifyRegistrant(new AsyncResult(null, sms, null));
                }
                break;

            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:
                if (RILJ_LOGD) unsljLog(response);

                if (mGsmBroadcastSmsRegistrant != null) {
                    mGsmBroadcastSmsRegistrant
                        .notifyRegistrant(new AsyncResult(null, ret, null));
                }
                break;

            case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL:
                if (RILJ_LOGD) unsljLog(response);

                if (mIccSmsFullRegistrant != null) {
                    mIccSmsFullRegistrant.notifyRegistrant();
                }
                break;

            case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE:
                if (RILJ_LOGD) unsljLog(response);

                if (mEmergencyCallbackModeRegistrant != null) {
                    mEmergencyCallbackModeRegistrant.notifyRegistrant();
                }
                break;

            case RIL_UNSOL_CDMA_CALL_WAITING:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCallWaitingInfoRegistrants != null) {
                    mCallWaitingInfoRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mOtaProvisionRegistrants != null) {
                    mOtaProvisionRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_CDMA_INFO_REC:
                ArrayList<CdmaInformationRecords> listInfoRecs;

                try {
                    listInfoRecs = (ArrayList<CdmaInformationRecords>)ret;
                } catch (ClassCastException e) {
                    Log.e(LOG_TAG, "Unexpected exception casting to listInfoRecs", e);
                    break;
                }

                for (CdmaInformationRecords rec : listInfoRecs) {
                    if (RILJ_LOGD) unsljLogRet(response, rec);
                    notifyRegistrantsCdmaInfoRec(rec);
                }
                break;

            case RIL_UNSOL_OEM_HOOK_RAW:
                if (RILJ_LOGD) unsljLogvRet(response, IccUtils.bytesToHexString((byte[])ret));
                if (mUnsolOemHookRawRegistrant != null) {
                    mUnsolOemHookRawRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
                }
                break;

            case RIL_UNSOL_RINGBACK_TONE:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mRingbackToneRegistrants != null) {
                    boolean playtone = (((int[])ret)[0] == 1);
                    mRingbackToneRegistrants.notifyRegistrants(
                                        new AsyncResult (null, playtone, null));
                }
                break;

            case RIL_UNSOL_RESEND_INCALL_MUTE:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mResendIncallMuteRegistrants != null) {
                    mResendIncallMuteRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mVoiceRadioTechChangedRegistrants != null) {
                    mVoiceRadioTechChangedRegistrants.notifyRegistrants(
                            new AsyncResult(null, ret, null));
                }
                break;

            case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCdmaSubscriptionChangedRegistrants != null) {
                    mCdmaSubscriptionChangedRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOl_CDMA_PRL_CHANGED:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCdmaPrlChangedRegistrants != null) {
                    mCdmaPrlChangedRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mExitEmergencyCallbackModeRegistrants != null) {
                    mExitEmergencyCallbackModeRegistrants.notifyRegistrants(
                                        new AsyncResult (null, null, null));
                }
                break;

            case RIL_UNSOL_RIL_CONNECTED: {
                if (RILJ_LOGD) unsljLogRet(response, ret);

                // Initial conditions
                if (FeatureOption.EVDO_DT_SUPPORT) {
                   setRadioState(RadioState.RADIO_OFF);				////Added for CDMA
                }
                setRadioPower(false, null);
                setPreferredNetworkType(mPreferredNetworkType, null);
                setCdmaSubscriptionSource(mCdmaSubscription, null);
                notifyRegistrantsRilConnectionChanged(((int[])ret)[0]);
                break;
             }

            //MTK-START [mtk04070][111121][ALPS00093395]MTK added    
            case RIL_UNSOL_CALL_FORWARDING:
                if (RILJ_LOGD) unsljLogRet(response, ret);
                
                if (mCallForwardingInfoRegistrants != null) {
                    boolean bCfuEnabled = (((int[])ret)[0] == 1);
                    boolean bIsLine1 = (((int[])ret)[1] == 1);
                    /* ONLY notify when Line1 is enabled */
                    if (bCfuEnabled && bIsLine1) {
                        mbWaitingForECFURegistrants = (mCallForwardingInfoRegistrants.size() == 0);
                        if (!mbWaitingForECFURegistrants) { 
                            mCallForwardingInfoRegistrants.notifyRegistrants();
                        }
                    }
                }
                break;
			
            case RIL_UNSOL_CRSS_NOTIFICATION:
                if (RILJ_LOGD) unsljLogRet(response, ret);

                if (mCallRelatedSuppSvcRegistrant != null) {
                    mCallRelatedSuppSvcRegistrant.notifyRegistrant(
                                        new AsyncResult (null, ret, null));
                }
                break;
                
            case RIL_UNSOL_NEIGHBORING_CELL_INFO: 
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mNeighboringInfoRegistrants != null) {
                    mNeighboringInfoRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }				
                break;			
				
            case RIL_UNSOL_NETWORK_INFO:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mNetworkInfoRegistrants != null) {
                    mNetworkInfoRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }					
				break;
		
            case RIL_UNSOL_CALL_PROGRESS_INFO:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mCallProgressInfoRegistrants != null) {
                    mCallProgressInfoRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }	
                break;		

            case RIL_UNSOL_PHB_READY_NOTIFICATION:
                if (RILJ_LOGD) unsljLog(response);
                if (mPhbReadyRegistrants != null) {
                    mPhbReadyRegistrants.notifyRegistrants();
                }
                break;

            case RIL_UNSOL_SPEECH_INFO:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mSpeechInfoRegistrants != null) {
                    mSpeechInfoRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }	
                break;	
            case RIL_UNSOL_RADIO_TEMPORARILY_UNAVAILABLE:
                if (RILJ_LOGD) unsljLogvRet(response, ret);

                radioTemporarilyUnavailable = ((int[])ret)[0];
                break;


            case RIL_UNSOL_SIM_INSERTED_STATUS:
                if (RILJ_LOGD) unsljLogRet(response, ret);
                mSimInsertedStatus = ((int[])ret)[0];
                if (mSimInsertedStatusRegistrants != null) {
                    mSimInsertedStatusRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;
            case RIL_UNSOL_SIM_MISSING:
            	if (RILJ_LOGD) unsljLogRet(response, ret);
            	if (mSimMissing != null) {
            		mSimMissing.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
            	break;
            case RIL_UNSOL_SIM_RECOVERY:
            	if (RILJ_LOGD) unsljLogRet(response, ret);
            	if (mSimRecovery != null) {
            		mSimRecovery.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
            	break;
            case RIL_UNSOL_VIRTUAL_SIM_ON:
            	if (RILJ_LOGD) unsljLogRet(response, ret);
            	if (mVirtualSimOn != null) {
            		mVirtualSimOn.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
            	break;
			      case RIL_UNSOL_VIRTUAL_SIM_OFF:
            	if (RILJ_LOGD) unsljLogRet(response, ret);
            	if (mVirtualSimOff != null) {
            		mVirtualSimOff.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
            break;
            case RIL_UNSOL_ME_SMS_STORAGE_FULL:
                if (RILJ_LOGD) unsljLog(response);

                if (mMeSmsFullRegistrant != null) {
                    mMeSmsFullRegistrant.notifyRegistrant();
                }
                break;
                
            case RIL_UNSOL_SMS_READY_NOTIFICATION:
                if (RILJ_LOGD) unsljLog(response);

                if (mSmsReadyRegistrants != null) {
                    mSmsReadyRegistrants.notifyRegistrants();
                }

                break;

            /* vt start */
            case RIL_UNSOL_VT_STATUS_INFO:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mVtStatusInfoRegistrants != null) {
                    mVtStatusInfoRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;

            case RIL_UNSOL_VT_RING_INFO:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mVtRingRegistrants != null) {
                    mVtRingRegistrants.notifyRegistrants(
                                        new AsyncResult (null, ret, null));
                }
                break;
            /* vt end */

            case RIL_UNSOL_SCRI_RESULT:
                Integer scriResult = (((int[])ret)[0]);

                riljLog("s:"+scriResult + ":" + (((int[])ret)[0]));
                
                if (RILJ_LOGD) unsljLogRet(response, ret);
                
                if (mScriResultRegistrant != null) {
                   mScriResultRegistrant.notifyRegistrant(
                                        new AsyncResult (null, scriResult, null));
                }
                break;

            case RIL_UNSOL_GPRS_DETACH:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mGprsDetachRegistrant != null) {
                    mGprsDetachRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
                }                
                break;
                
            case RIL_UNSOL_INCOMING_CALL_INDICATION:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mIncomingCallIndicationRegistrant != null) {
                    mIncomingCallIndicationRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
                }
                break;
            //ALPS00249116 START
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED:
                if (RILJ_LOGD) unsljLog(response);
                mPsNetworkStateRegistrants
                    .notifyRegistrants(new AsyncResult(null, null, null));
            break;
            //ALPS00249116 END

            case RIL_UNSOL_IMEI_LOCK:
                if (RILJ_LOGD) unsljLog(response);
                if (mImeiLockRegistrant != null) {
                    mImeiLockRegistrant.notifyRegistrants(new AsyncResult (null, null, null));
                }
                break;
	
            //ALPS00248788 START
            case RIL_UNSOL_INVALID_SIM:
                if (RILJ_LOGD) unsljLogvRet(response, ret);
                if (mInvalidSimInfoRegistrant != null) {
                   mInvalidSimInfoRegistrant.notifyRegistrant(new AsyncResult (null, ret, null));
                }
				break;
            //ALPS00248788 END	
            case RIL_UNSOL_SIM_PLUG_OUT:
                if(FeatureOption.MTK_SIM_HOT_SWAP == true){
                    if (RILJ_LOGD) unsljLogRet(response, ret);
                        mSimInsertedStatus = ((int[])ret)[0];
                    if (mSimPlugOutRegistrants!= null)
                        mSimPlugOutRegistrants.notifyRegistrants(new AsyncResult (null, ret, null));
                }
                break;
            case RIL_UNSOL_SIM_PLUG_IN:
                if(FeatureOption.MTK_SIM_HOT_SWAP == true){
                    if (RILJ_LOGD) unsljLogRet(response, ret);
                        mSimInsertedStatus = ((int[])ret)[0];
                    if (mSimPlugInRegistrants!= null)
                        mSimPlugInRegistrants.notifyRegistrants(new AsyncResult (null, ret, null));
                }
                break;
            // ALPS00297719 START
            case RIL_UNSOL_RESPONSE_ACMT:
                if (RILJ_LOGD) unsljLog(response);
                if (ret != null) {
                    int[] acmt = (int[]) ret;
                    if (acmt.length == 2) {
                        int error_type = Integer.valueOf(acmt[0]);
                        int error_cause = acmt[1];
                        if(mServiceStateExt.needBrodcastACMT(error_type,error_cause) == true)
                        {                        	  
                            Intent intent = new Intent(TelephonyIntents.ACTION_ACMT_NETWORK_SERVICE_STATUS_INDICATOR);
                            intent.putExtra("CauseCode", acmt[1]);
                            intent.putExtra("CauseType", acmt[0]);
                            mContext.sendBroadcast(intent);
                            riljLog("Broadcast for ACMT: com.VendorName.CauseCode " + acmt[1] + "," + acmt[0]);
                        }                        
                    }
                }
                break;			
            // ALPS00297719 END

            case RIL_UNSOL_EF_CSP_PLMN_MODE_BIT:
                // ALPS00302698 ENS
                if (RILJ_LOGD) unsljLog(response);
                if (mEfCspPlmnModeBitRegistrant != null) {
                    mEfCspPlmnModeBitRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
                }
                break;

            //MTK-START [MTK80515] [ALPS00368272]
            case RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED:
                if (RILJ_LOGD) unsljLog(response);
                if (ret != null) {
                    int[] emmrrs = (int[]) ret;
                    int ps_status = Integer.valueOf(emmrrs[0]);
                    if(mServiceStateExt.isBroadcastEmmrrsPsResume(ps_status)) {
                        riljLog("Broadcast for EMMRRS: android.intent.action.EMMRRS_PS_RESUME ");
                    }
                }
                break;  
            //MTK-END [MTK80515] [ALPS00368272]
            
            // MTK-START for ETWS by mtk80589
            case RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION:
                if (RILJ_LOGD) unsljLog(response);
                if(mEtwsNotificationRegistrant != null) {
                    mEtwsNotificationRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
                }
                break;
            // MTK-END for ETWS by mtk80589

            default:
                break;
            //MTK-END [mtk04070][111121][ALPS00093395]MTK added    
        }
    }

    /**
     * Notifiy all registrants that the ril has connected or disconnected.
     *
     * @param rilVer is the version of the ril or -1 if disconnected.
     */
    private void notifyRegistrantsRilConnectionChanged(int rilVer) {
        mRilVersion = rilVer;
        if (mRilConnectedRegistrants != null) {
            mRilConnectedRegistrants.notifyRegistrants(
                                new AsyncResult (null, new Integer(rilVer), null));
        }
    }

    private Object
    responseInts(Parcel p) {
        int numInts;
        int response[];

        numInts = p.readInt();

        response = new int[numInts];

        for (int i = 0 ; i < numInts ; i++) {
            response[i] = p.readInt();
        }

        return response;
    }


    private Object
    responseVoid(Parcel p) {
        return null;
    }

    private Object
    responseCallForward(Parcel p) {
        int numInfos;
        CallForwardInfo infos[];

        numInfos = p.readInt();

        infos = new CallForwardInfo[numInfos];

        for (int i = 0 ; i < numInfos ; i++) {
            infos[i] = new CallForwardInfo();

            infos[i].status = p.readInt();
            infos[i].reason = p.readInt();
            infos[i].serviceClass = p.readInt();
            infos[i].toa = p.readInt();
            infos[i].number = p.readString();
            infos[i].timeSeconds = p.readInt();
        }

        return infos;
    }

    private Object
    responseSuppServiceNotification(Parcel p) {
        SuppServiceNotification notification = new SuppServiceNotification();

        notification.notificationType = p.readInt();
        notification.code = p.readInt();
        notification.index = p.readInt();
        notification.type = p.readInt();
        notification.number = p.readString();

        return notification;
    }

    private Object
    responseCdmaSms(Parcel p) {
        SmsMessage sms;
        sms = SmsMessage.newFromParcel(p);

        return sms;
    }

    // via support to add this API
    private Object
    responseGetNitzTime(Parcel p) {
        Object[] result = new Object[2];
        String response;

        response = p.readString();
        long nitzReceiveTime = p.readLong();
        result[0] = response;
        result[1] = Long.valueOf(nitzReceiveTime);

        return result;
    }

    private Object
    responseString(Parcel p) {
        String response;

        response = p.readString();

        return response;
    }

    private Object
    responseStrings(Parcel p) {
        int num;
        String response[];

        response = p.readStringArray();

        if (false) {
            num = p.readInt();

            response = new String[num];
            for (int i = 0; i < num; i++) {
                response[i] = p.readString();
            }
        }

        return response;
    }

    private Object
    responseRaw(Parcel p) {
        int num;
        byte response[];

        response = p.createByteArray();

        return response;
    }

    private Object
    responseSMS(Parcel p) {
        int messageRef, errorCode;
        String ackPDU;

        messageRef = p.readInt();
        ackPDU = p.readString();
        errorCode = p.readInt();

        SmsResponse response = new SmsResponse(messageRef, ackPDU, errorCode);

        return response;
    }
    
    private Object
    responseSmsParams(Parcel p) {
        int format = p.readInt();
        int vp = p.readInt();
        int pid = p.readInt();
        int dcs = p.readInt();
        
        return new SmsParameters(format, vp, pid, dcs);
    }


    private Object
     responseICC_IO(Parcel p) {
        int sw1, sw2;
        byte data[] = null;
        Message ret;

        sw1 = p.readInt();
        sw2 = p.readInt();

        String s = p.readString();

        if (RILJ_LOGV) riljLog("< iccIO: "
                + " 0x" + Integer.toHexString(sw1)
                + " 0x" + Integer.toHexString(sw2) + " "
                + s);

        return new IccIoResult(sw1, sw2, s);
    }

    private Object
    responseIccCardStatus(Parcel p) {
        IccCardApplicationStatus appStatus;

        IccCardStatus cardStatus = new IccCardStatus();
        cardStatus.setCardState(p.readInt());
        cardStatus.setUniversalPinState(p.readInt());
        cardStatus.mGsmUmtsSubscriptionAppIndex = p.readInt();
        cardStatus.mCdmaSubscriptionAppIndex = p.readInt();
        cardStatus.mImsSubscriptionAppIndex = p.readInt();
        int numApplications = p.readInt();

        // limit to maximum allowed applications
        if (numApplications > IccCardStatus.CARD_MAX_APPS) {
            numApplications = IccCardStatus.CARD_MAX_APPS;
        }
        cardStatus.mApplications = new IccCardApplicationStatus[numApplications];
        for (int i = 0 ; i < numApplications ; i++) {
            appStatus = new IccCardApplicationStatus();
            appStatus.app_type       = appStatus.AppTypeFromRILInt(p.readInt());
            appStatus.app_state      = appStatus.AppStateFromRILInt(p.readInt());
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(p.readInt());
            appStatus.aid            = p.readString();
            appStatus.app_label      = p.readString();
            appStatus.pin1_replaced  = p.readInt();
            appStatus.pin1           = appStatus.PinStateFromRILInt(p.readInt());
            appStatus.pin2           = appStatus.PinStateFromRILInt(p.readInt());
            cardStatus.mApplications[i] = appStatus;
        }
        return cardStatus;
    }

    private Object
    responseSimRefresh(Parcel p) {
        IccRefreshResponse response = new IccRefreshResponse();

        int numInts = p.readInt();
        response.refreshResult = p.readInt();
        response.efId   = p.readInt();
        response.aid = p.readString();
        return response;
    }

    private Object
    responseCallList(Parcel p) {
        int num;
        int voiceSettings;
        ArrayList<DriverCall> response;
        DriverCall dc;

        num = p.readInt();
        response = new ArrayList<DriverCall>(num);

        if (RILJ_LOGV) {
            riljLog("responseCallList: num=" + num +
                    " mEmergencyCallbackModeRegistrant=" + mEmergencyCallbackModeRegistrant +
                    " mTestingEmergencyCall=" + mTestingEmergencyCall.get());
        }
        for (int i = 0 ; i < num ; i++) {
            dc = new DriverCall();

            dc.state = DriverCall.stateFromCLCC(p.readInt());
            dc.index = p.readInt();
            dc.TOA = p.readInt();
            dc.isMpty = (0 != p.readInt());
            dc.isMT = (0 != p.readInt());
            dc.als = p.readInt();
            voiceSettings = p.readInt();
            dc.isVoice = (0 == voiceSettings) ? false : true;
            dc.isVoicePrivacy = (0 != p.readInt());
            dc.number = p.readString();
            int np = p.readInt();
            dc.numberPresentation = DriverCall.presentationFromCLIP(np);
            dc.name = p.readString();
            dc.namePresentation = p.readInt();
            int uusInfoPresent = p.readInt();
            if (uusInfoPresent == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(p.readInt());
                dc.uusInfo.setDcs(p.readInt());
                byte[] userData = p.createByteArray();
                dc.uusInfo.setUserData(userData);
                riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d",
                                dc.uusInfo.getType(), dc.uusInfo.getDcs(),
                                dc.uusInfo.getUserData().length));
                riljLogv("Incoming UUS : data (string)="
                        + new String(dc.uusInfo.getUserData()));
                riljLogv("Incoming UUS : data (hex): "
                        + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
            } else {
                riljLogv("Incoming UUS : NOT present!");
            }

            // Make sure there's a leading + on addresses with a TOA of 145
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);

            response.add(dc);

            if (dc.isVoicePrivacy) {
                mVoicePrivacyOnRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is enabled");
            } else {
                mVoicePrivacyOffRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is disabled");
            }
        }

        Collections.sort(response);

        if ((num == 0) && mTestingEmergencyCall.getAndSet(false)) {
            if (mEmergencyCallbackModeRegistrant != null) {
                riljLog("responseCallList: call ended, testing emergency call," +
                            " notify ECM Registrants");
                mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
        }

        return response;
    }

    private DataCallState getDataCallState(Parcel p, int version) {
        DataCallState dataCall = new DataCallState();

        dataCall.version = version;
        if (version < 5) {
            dataCall.cid = p.readInt();
            dataCall.active = p.readInt();
            dataCall.type = p.readString();
            String addresses = p.readString();
            if (!TextUtils.isEmpty(addresses)) {
                dataCall.addresses = addresses.split(" ");
            }
        } else {
            dataCall.status = p.readInt();
            dataCall.suggestedRetryTime = p.readInt();
            dataCall.cid = p.readInt();
            dataCall.active = p.readInt();
            dataCall.type = p.readString();
            dataCall.ifname = p.readString();
            if ((dataCall.status == DataConnection.FailCause.NONE.getErrorCode()) &&
                    TextUtils.isEmpty(dataCall.ifname)) {
              throw new RuntimeException("getDataCallState, no ifname");
            }
            String addresses = p.readString();
            if (!TextUtils.isEmpty(addresses)) {
                dataCall.addresses = addresses.split(" ");
            }
            String dnses = p.readString();
            if (!TextUtils.isEmpty(dnses)) {
                dataCall.dnses = dnses.split(" ");
            }
            String gateways = p.readString();
            if (!TextUtils.isEmpty(gateways)) {
                dataCall.gateways = gateways.split(" ");
            }
        }
        return dataCall;
    }

    private Object
    responseDataCallList(Parcel p) {
        ArrayList<DataCallState> response;

        int ver = p.readInt();
        int num = p.readInt();
        riljLog("responseDataCallList ver=" + ver + " num=" + num);

        response = new ArrayList<DataCallState>(num);
        for (int i = 0; i < num; i++) {
            response.add(getDataCallState(p, ver));
        }

        return response;
    }

    private Object
    responseSetupDataCall(Parcel p) {
        int ver = p.readInt();
        int num = p.readInt();
        if (RILJ_LOGV) riljLog("responseSetupDataCall ver=" + ver + " num=" + num);

        DataCallState dataCall;

        if (ver < 5) {
            dataCall = new DataCallState();
            dataCall.version = ver;
            dataCall.cid = Integer.parseInt(p.readString());
            dataCall.ifname = p.readString();
            if (TextUtils.isEmpty(dataCall.ifname)) {
                throw new RuntimeException(
                        "RIL_REQUEST_SETUP_DATA_CALL response, no ifname");
            }
            String addresses = p.readString();
            if (!TextUtils.isEmpty(addresses)) {
              dataCall.addresses = addresses.split(" ");
            }
            if (num >= 4) {
                String dnses = p.readString();
                if (RILJ_LOGD) riljLog("responseSetupDataCall got dnses=" + dnses);
                if (!TextUtils.isEmpty(dnses)) {
                    dataCall.dnses = dnses.split(" ");
                }
            }
            if (num >= 5) {
                String gateways = p.readString();
                if (RILJ_LOGD) riljLog("responseSetupDataCall got gateways=" + gateways);
                if (!TextUtils.isEmpty(gateways)) {
                    dataCall.gateways = gateways.split(" ");
                }
            }
        } else {
            if (num != 1) {
                throw new RuntimeException(
                        "RIL_REQUEST_SETUP_DATA_CALL response expecting 1 RIL_Data_Call_response_v5"
                        + " got " + num);
            }
            dataCall = getDataCallState(p, ver);
        }

        return dataCall;
    }

    //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
    private Object
    responseOperatorInfos(Parcel p) {
        String strings[] = (String [])responseStrings(p);
        ArrayList<OperatorInfo> ret;

        if (strings.length % 5 != 0) {
            throw new RuntimeException(
                "RIL_REQUEST_QUERY_AVAILABLE_NETWORKS: invalid response. Got "
                + strings.length + " strings, expected multible of 5");
        }

        // ALPS00353868 START
        String lacStr = SystemProperties.get("gsm.cops.lac");
        boolean lacValid = false;
        int lacIndex=0;

        Log.d(LOG_TAG, "lacStr = " + lacStr+" lacStr.length="+lacStr.length()+" strings.length="+strings.length);
        if((lacStr.length() > 0) && (lacStr.length()%4 == 0) && ((lacStr.length()/4) == (strings.length/5 ))){		
            Log.d(LOG_TAG, "lacValid set to true");			
            lacValid = true;
        }			

        SystemProperties.set("gsm.cops.lac",""); //reset property						
        // ALPS00353868 END 		
   
        ret = new ArrayList<OperatorInfo>(strings.length / 5);

        for (int i = 0 ; i < strings.length ; i += 5) {
	     		
            /* ALPS00273663 handle UCS2 format name : prefix + hex string ex: "uCs2806F767C79D1" */	
            if((strings[i+0] != null) && (strings[i+0].startsWith("uCs2") == true))
            {        
                riljLog("responseOperatorInfos handling UCS2 format name");			        
			
                try{	
                    strings[i+0] = new String(hexStringToBytes(strings[i+0].substring(4)), "UTF-16");
                }catch(UnsupportedEncodingException ex){
                    riljLog("responseOperatorInfos UnsupportedEncodingException");
                }			
            }

            // ALPS00353868 START
            if((lacValid == true) && (strings[i+0] != null) && (mPhone!= null)){
                UiccController uiccController = UiccController.getInstance(mPhone.getMySimId());
                IccRecords iccRecords = uiccController.getIccRecords(UiccController.APP_FAM_3GPP);
                int lacValue = -1;
                String sEons = null;				
                String lac = lacStr.substring(lacIndex,lacIndex+4);	
                Log.d(LOG_TAG, "lacIndex="+lacIndex+" lacValue="+lacValue+" lac="+lac+" plmn numeric="+strings[i+2]+" plmn name"+strings[i+0]);							

                if(lac != ""){
                    lacValue = Integer.parseInt(lac, 16);
                    lacIndex += 4;
                    if(lacValue != 0xfffe){
                        sEons = iccRecords.getEonsIfExist(strings[i+2],lacValue,true);
                        if(sEons != null){						
                            strings[i+0] = sEons; 		
                            Log.d(LOG_TAG, "plmn name update to Eons: "+strings[i+0]);			
                        }
                    }else{
                        Log.d(LOG_TAG, "invalid lac ignored");			
                    }					
                }
            }
            // ALPS00353868 END
			
            if (strings[i+0] != null && (strings[i+0].equals("") || strings[i+0].equals(strings[i+2]))) {
                riljLog("lookup RIL responseOperatorInfos()");
                strings[i+0] = lookupOperatorName(strings[i+2], true);
                strings[i+1] = lookupOperatorName(strings[i+2], false);
            }
            //1 and 2 is 2g. above 2 is 3g
            String property_name = "gsm.baseband.capability";			
            if(mySimId > PhoneConstants.GEMINI_SIM_1){
                property_name = property_name + (mySimId+1) ;
            }				
			
            int basebandCapability = SystemProperties.getInt(property_name, 3); /* ALPS00352231 */			
            Log.d(LOG_TAG, "property_name="+property_name+",basebandCapability=" + basebandCapability);
            if( 3 < basebandCapability){
            	strings[i+0] = strings[i+0].concat(" " + strings[i+4]);
            	strings[i+1] = strings[i+1].concat(" " + strings[i+4]);
            }
	
            ret.add (
                new OperatorInfo(
                    strings[i+0],
                    strings[i+1],
                    strings[i+2],
                    strings[i+3]));
        }

        return ret;
    }
    //MTK-END [mtk04070][111121][ALPS00093395]MTK modified

   private Object
   responseCellList(Parcel p) {
       int num, rssi;
       String location;
       ArrayList<NeighboringCellInfo> response;
       NeighboringCellInfo cell;

       num = p.readInt();
       response = new ArrayList<NeighboringCellInfo>();

       // ALPS00269882 START
       // Get the radio access type
       int radioType = SystemProperties.getInt("gsm.enbr.rat", NETWORK_TYPE_GPRS);
       riljLog("gsm.enbr.rat=" + radioType);
       // ALPS00269882 END

       // Interpret the location based on radio access type
       if (radioType != NETWORK_TYPE_UNKNOWN) {
           for (int i = 0 ; i < num ; i++) {
               rssi = p.readInt();
               location = p.readString();
               cell = new NeighboringCellInfo(rssi, location, radioType);
               response.add(cell);
           }
       }
       return response;
    }

    private Object responseGetPreferredNetworkType(Parcel p) {
       int [] response = (int[]) responseInts(p);

       if (response.length >= 1) {
           // Since this is the response for getPreferredNetworkType
           // we'll assume that it should be the value we want the
           // vendor ril to take if we reestablish a connection to it.
           //mPreferredNetworkType = response[0];
       }
       return response;
    }

    private Object responseGmsBroadcastConfig(Parcel p) {
        int num;
        ArrayList<SmsBroadcastConfigInfo> response;
        SmsBroadcastConfigInfo info;

        num = p.readInt();
        response = new ArrayList<SmsBroadcastConfigInfo>(num);

        for (int i = 0; i < num; i++) {
            int fromId = p.readInt();
            int toId = p.readInt();
            int fromScheme = p.readInt();
            int toScheme = p.readInt();
            boolean selected = (p.readInt() == 1);

            info = new SmsBroadcastConfigInfo(fromId, toId, fromScheme,
                    toScheme, selected);
            response.add(info);
        }
        return response;
    }

    private Object
    responseCdmaBroadcastConfig(Parcel p) {
        int numServiceCategories;
        int response[];

        numServiceCategories = p.readInt();

        if (numServiceCategories == 0) {
            // TODO: The logic of providing default values should
            // not be done by this transport layer. And needs to
            // be done by the vendor ril or application logic.
            int numInts;
            numInts = CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES * CDMA_BSI_NO_OF_INTS_STRUCT + 1;
            response = new int[numInts];

            // Faking a default record for all possible records.
            response[0] = CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES;

            // Loop over CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES set 'english' as
            // default language and selection status to false for all.
            for (int i = 1; i < numInts; i += CDMA_BSI_NO_OF_INTS_STRUCT ) {
                response[i + 0] = i / CDMA_BSI_NO_OF_INTS_STRUCT;
                response[i + 1] = 1;
                response[i + 2] = 0;
            }
        } else {
            int numInts;
            numInts = (numServiceCategories * CDMA_BSI_NO_OF_INTS_STRUCT) + 1;
            response = new int[numInts];

            response[0] = numServiceCategories;
            for (int i = 1 ; i < numInts; i++) {
                 response[i] = p.readInt();
             }
        }

        return response;
    }

    private Object
    responseSignalStrength(Parcel p) {
        SignalStrength signalStrength = new SignalStrength(p);
        return signalStrength;
    }

    private ArrayList<CdmaInformationRecords>
    responseCdmaInformationRecord(Parcel p) {
        int numberOfInfoRecs;
        ArrayList<CdmaInformationRecords> response;

        /**
         * Loop through all of the information records unmarshalling them
         * and converting them to Java Objects.
         */
        numberOfInfoRecs = p.readInt();
        response = new ArrayList<CdmaInformationRecords>(numberOfInfoRecs);

        for (int i = 0; i < numberOfInfoRecs; i++) {
            CdmaInformationRecords InfoRec = new CdmaInformationRecords(p);
            response.add(InfoRec);
        }

        return response;
    }

    private Object
    responseCdmaCallWaiting(Parcel p) {
        CdmaCallWaitingNotification notification = new CdmaCallWaitingNotification();

        notification.number = p.readString();
        notification.numberPresentation = notification.presentationFromCLIP(p.readInt());
        notification.name = p.readString();
        notification.namePresentation = notification.numberPresentation;
        notification.isPresent = p.readInt();
        notification.signalType = p.readInt();
        notification.alertPitch = p.readInt();
        notification.signal = p.readInt();
        notification.numberType = p.readInt();
        notification.numberPlan = p.readInt();

        return notification;
    }

    private Object
    responseCallRing(Parcel p){
        char response[] = new char[4];

        response[0] = (char) p.readInt();    // isPresent
        response[1] = (char) p.readInt();    // signalType
        response[2] = (char) p.readInt();    // alertPitch
        response[3] = (char) p.readInt();    // signal

        return response;
    }

    private void
    notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {
        int response = RIL_UNSOL_CDMA_INFO_REC;
        if (infoRec.record instanceof CdmaInformationRecords.CdmaDisplayInfoRec) {
            if (mDisplayInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mDisplayInfoRegistrants.notifyRegistrants(
                        new AsyncResult (null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaSignalInfoRec) {
            if (mSignalInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mSignalInfoRegistrants.notifyRegistrants(
                        new AsyncResult (null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaNumberInfoRec) {
            if (mNumberInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mNumberInfoRegistrants.notifyRegistrants(
                        new AsyncResult (null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec) {
            if (mRedirNumInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mRedirNumInfoRegistrants.notifyRegistrants(
                        new AsyncResult (null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec) {
            if (mLineControlInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mLineControlInfoRegistrants.notifyRegistrants(
                        new AsyncResult (null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec) {
            if (mT53ClirInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mT53ClirInfoRegistrants.notifyRegistrants(
                        new AsyncResult (null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec) {
            if (mT53AudCntrlInfoRegistrants != null) {
               if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
               mT53AudCntrlInfoRegistrants.notifyRegistrants(
                       new AsyncResult (null, infoRec.record, null));
            }
        }
    }

    static String
    requestToString(int request) {
/*
 cat libs/telephony/ril_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{RIL_([^,]+),[^,]+,([^}]+).+/case RIL_\1: return "\1";/'
*/
        switch(request) {
            case RIL_REQUEST_GET_SIM_STATUS: return "GET_SIM_STATUS";
            case RIL_REQUEST_ENTER_SIM_PIN: return "ENTER_SIM_PIN";
            case RIL_REQUEST_ENTER_SIM_PUK: return "ENTER_SIM_PUK";
            case RIL_REQUEST_ENTER_SIM_PIN2: return "ENTER_SIM_PIN2";
            case RIL_REQUEST_ENTER_SIM_PUK2: return "ENTER_SIM_PUK2";
            case RIL_REQUEST_CHANGE_SIM_PIN: return "CHANGE_SIM_PIN";
            case RIL_REQUEST_CHANGE_SIM_PIN2: return "CHANGE_SIM_PIN2";
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION: return "ENTER_NETWORK_DEPERSONALIZATION";
            case RIL_REQUEST_GET_CURRENT_CALLS: return "GET_CURRENT_CALLS";
            case RIL_REQUEST_DIAL: return "DIAL";
            case RIL_REQUEST_GET_IMSI: return "GET_IMSI";
            case RIL_REQUEST_HANGUP: return "HANGUP";
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: return "HANGUP_WAITING_OR_BACKGROUND";
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case RIL_REQUEST_CONFERENCE: return "CONFERENCE";
            case RIL_REQUEST_UDUB: return "UDUB";
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: return "LAST_CALL_FAIL_CAUSE";
            case RIL_REQUEST_SIGNAL_STRENGTH: return "SIGNAL_STRENGTH";
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: return "VOICE_REGISTRATION_STATE";
            case RIL_REQUEST_DATA_REGISTRATION_STATE: return "DATA_REGISTRATION_STATE";
            case RIL_REQUEST_OPERATOR: return "OPERATOR";
            case RIL_REQUEST_RADIO_POWER: return "RADIO_POWER";
            case RIL_REQUEST_DTMF: return "DTMF";
            case RIL_REQUEST_SEND_SMS: return "SEND_SMS";
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: return "SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_SETUP_DATA_CALL: return "SETUP_DATA_CALL";
            case RIL_REQUEST_SIM_IO: return "SIM_IO";
            // NFC SEEK start
            case RIL_REQUEST_SIM_TRANSMIT_BASIC: return "SIM_TRANSMIT_BASIC";
            case RIL_REQUEST_SIM_OPEN_CHANNEL: return "SIM_OPEN_CHANNEL";
            case RIL_REQUEST_SIM_CLOSE_CHANNEL: return "SIM_CLOSE_CHANNEL";
            case RIL_REQUEST_SIM_TRANSMIT_CHANNEL: return "SIM_TRANSMIT_CHANNEL";
            case RIL_REQUEST_SIM_GET_ATR: return "SIM_GET_ATR";
            case RIL_REQUEST_SIM_OPEN_CHANNEL_WITH_SW: return "SIM_OPEN_CHANNEL_WITH_SW";
            // NFC SEEK end
            case RIL_REQUEST_SEND_USSD: return "SEND_USSD";
            case RIL_REQUEST_CANCEL_USSD: return "CANCEL_USSD";
            case RIL_REQUEST_GET_CLIR: return "GET_CLIR";
            case RIL_REQUEST_SET_CLIR: return "SET_CLIR";
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: return "QUERY_CALL_FORWARD_STATUS";
            case RIL_REQUEST_SET_CALL_FORWARD: return "SET_CALL_FORWARD";
            case RIL_REQUEST_QUERY_CALL_WAITING: return "QUERY_CALL_WAITING";
            case RIL_REQUEST_SET_CALL_WAITING: return "SET_CALL_WAITING";
            case RIL_REQUEST_SMS_ACKNOWLEDGE: return "SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GET_IMEI: return "GET_IMEI";
            case RIL_REQUEST_GET_IMEISV: return "GET_IMEISV";
            case RIL_REQUEST_ANSWER: return "ANSWER";
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: return "DEACTIVATE_DATA_CALL";
            case RIL_REQUEST_QUERY_FACILITY_LOCK: return "QUERY_FACILITY_LOCK";
            case RIL_REQUEST_SET_FACILITY_LOCK: return "SET_FACILITY_LOCK";
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: return "CHANGE_BARRING_PASSWORD";
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: return "QUERY_NETWORK_SELECTION_MODE";
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: return "SET_NETWORK_SELECTION_AUTOMATIC";
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: return "SET_NETWORK_SELECTION_MANUAL";
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : return "QUERY_AVAILABLE_NETWORKS ";
            case RIL_REQUEST_DTMF_START: return "DTMF_START";
            case RIL_REQUEST_DTMF_STOP: return "DTMF_STOP";
            case RIL_REQUEST_BASEBAND_VERSION: return "BASEBAND_VERSION";
            case RIL_REQUEST_SEPARATE_CONNECTION: return "SEPARATE_CONNECTION";
            case RIL_REQUEST_SET_MUTE: return "SET_MUTE";
            case RIL_REQUEST_GET_MUTE: return "GET_MUTE";
            case RIL_REQUEST_QUERY_CLIP: return "QUERY_CLIP";
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: return "LAST_DATA_CALL_FAIL_CAUSE";
            case RIL_REQUEST_DATA_CALL_LIST: return "DATA_CALL_LIST";
            case RIL_REQUEST_RESET_RADIO: return "RESET_RADIO";
            case RIL_REQUEST_OEM_HOOK_RAW: return "OEM_HOOK_RAW";
            case RIL_REQUEST_OEM_HOOK_STRINGS: return "OEM_HOOK_STRINGS";
            case RIL_REQUEST_SCREEN_STATE: return "SCREEN_STATE";
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: return "SET_SUPP_SVC_NOTIFICATION";
            case RIL_REQUEST_WRITE_SMS_TO_SIM: return "WRITE_SMS_TO_SIM";
            case RIL_REQUEST_DELETE_SMS_ON_SIM: return "DELETE_SMS_ON_SIM";
            case RIL_REQUEST_SET_BAND_MODE: return "SET_BAND_MODE";
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: return "QUERY_AVAILABLE_BAND_MODE";
            case RIL_REQUEST_STK_GET_PROFILE: return "REQUEST_STK_GET_PROFILE";
            case RIL_REQUEST_STK_SET_PROFILE: return "REQUEST_STK_SET_PROFILE";
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: return "REQUEST_EXPLICIT_CALL_TRANSFER";
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: return "REQUEST_GET_NEIGHBORING_CELL_IDS";
            case RIL_REQUEST_SET_LOCATION_UPDATES: return "REQUEST_SET_LOCATION_UPDATES";
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: return "RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE";
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
            case RIL_REQUEST_SET_TTY_MODE: return "RIL_REQUEST_SET_TTY_MODE";
            case RIL_REQUEST_QUERY_TTY_MODE: return "RIL_REQUEST_QUERY_TTY_MODE";
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: return "RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: return "RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_FLASH: return "RIL_REQUEST_CDMA_FLASH";
            case RIL_REQUEST_CDMA_BURST_DTMF: return "RIL_REQUEST_CDMA_BURST_DTMF";
            case RIL_REQUEST_CDMA_SEND_SMS: return "RIL_REQUEST_CDMA_SEND_SMS";
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: return "RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY";
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_SUBSCRIPTION: return "RIL_REQUEST_CDMA_SUBSCRIPTION";
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
            case RIL_REQUEST_DEVICE_IDENTITY: return "RIL_REQUEST_DEVICE_IDENTITY";
            case RIL_REQUEST_GET_SMSC_ADDRESS: return "RIL_REQUEST_GET_SMSC_ADDRESS";
            case RIL_REQUEST_SET_SMSC_ADDRESS: return "RIL_REQUEST_SET_SMSC_ADDRESS";
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
            // via support to add this RIL request
            case RIL_REQUEST_GET_NITZ_TIME: return "GET_NITZ_TIME";
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_ISIM_AUTHENTICATION: return "RIL_REQUEST_ISIM_AUTHENTICATION";
            case RIL_REQUEST_VOICE_RADIO_TECH: return "RIL_REQUEST_VOICE_RADIO_TECH";

            //MTK-START [mtk04070][111121][ALPS00093395]MTK added
            case RIL_REQUEST_EMERGENCY_DIAL: return "EMERGENCY_DIAL";
            /* vt start */
            case RIL_REQUEST_VT_DIAL: return "VT_DIAL";
            case RIL_REQUEST_VOICE_ACCEPT: return "VOICE_ACCEPT";
            /* vt end */
            case RIL_REQUEST_HANGUP_ALL: return "HANGUP_ALL";
            case RIL_REQUEST_HANGUP_ALL_EX: return "HANGUP_ALL_EX";
            case RIL_REQUEST_GET_CCM: return "GET_CCM";
            case RIL_REQUEST_GET_ACM: return "GET_ACM";
            case RIL_REQUEST_GET_ACMMAX: return "GET_ACMMAX";
            case RIL_REQUEST_GET_PPU_AND_CURRENCY: return "GET_PPU_AND_CURRENCY";
            case RIL_REQUEST_SET_ACMMAX: return "SET_ACMMAX";
            case RIL_REQUEST_RESET_ACM: return "RESET_ACM";
            case RIL_REQUEST_SET_PPU_AND_CURRENCY: return "SET_PPU_AND_CURRENCY";			
            case RIL_REQUEST_RADIO_POWEROFF: return "RADIO_POWEROFF";
            case RIL_REQUEST_RADIO_POWERON: return "RADIO_POWERON";			
            case RIL_REQUEST_DUAL_SIM_MODE_SWITCH: return "DUAL_SIM_MODE_SWITCH";            
            case RIL_REQUEST_SET_GPRS_CONNECT_TYPE: return "RIL_REQUEST_SET_GPRS_CONNECT_TYPE";
            case RIL_REQUEST_SET_GPRS_TRANSFER_TYPE: return "RIL_REQUEST_SET_GPRS_TRANSFER_TYPE";
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT: return "SET_NETWORK_SELECTION_MANUAL_WITH_ACT";			
            case RIL_REQUEST_GET_COLP: return "GET_COLP";
            case RIL_REQUEST_SET_COLP: return "SET_COLP";
            case RIL_REQUEST_GET_COLR: return "GET_COLR";   
            case RIL_REQUEST_QUERY_PHB_STORAGE_INFO: return "RIL_REQUEST_QUERY_PHB_STORAGE_INFO";
            case RIL_REQUEST_WRITE_PHB_ENTRY: return "RIL_REQUEST_WRITE_PHB_ENTRY";
            case RIL_REQUEST_READ_PHB_ENTRY: return "RIL_REQUEST_READ_PHB_ENTRY";
            case RIL_REQUEST_MOBILEREVISION_AND_IMEI: return "RIL_REQUEST_MOBILEREVISION_AND_IMEI";//Add by mtk80372 for Barcode Number
            case RIL_REQUEST_QUERY_SIM_NETWORK_LOCK: return "QUERY_SIM_NETWORK_LOCK";
            case RIL_REQUEST_SET_SIM_NETWORK_LOCK: return "SET_SIM_NETWORK_LOCK";
            case RIL_REQUEST_SET_SCRI: return "RIL_REQUEST_SET_SCRI";
            case RIL_REQUEST_SET_FD_MODE: return "RIL_REQUEST_SET_FD_MODE";	//[New R8 modem FD]		
            case RIL_REQUEST_BTSIM_CONNECT: return "RIL_REQUEST_BTSIM_CONNECT";
            case RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF: return "RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF";   
            case RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM: return "RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM"; 
            case RIL_REQUEST_BTSIM_TRANSFERAPDU: return "RIL_REQUEST_SEND_BTSIM_TRANSFERAPDU";  
            case RIL_REQUEST_QUERY_ICCID: return "RIL_REQUEST_QUERY_ICCID";
            case RIL_REQUEST_SIM_AUTHENTICATION: return "RIL_REQUEST_SIM_AUTHENTICATION";
            case RIL_REQUEST_USIM_AUTHENTICATION: return "RIL_REQUEST_USIM_AUTHENTICATION";
            case RIL_REQUEST_GET_SMS_SIM_MEM_STATUS: return "RIL_REQUEST_GET_SMS_SIM_MEM_STATUS";
            case RIL_REQUEST_FORCE_RELEASE_CALL: return "RIL_REQUEST_FORCE_RELEASE_CALL";
            case RIL_REQUEST_SET_CALL_INDICATION: return "RIL_REQUEST_SET_CALL_INDICATION";
            case RIL_REQUEST_REPLACE_VT_CALL: return "RIL_REQUEST_REPLACE_VT_CALL";
            case RIL_REQUEST_GET_3G_CAPABILITY: return "RIL_REQUEST_GET_3G_CAPABILITY";
            case RIL_REQUEST_SET_3G_CAPABILITY: return "RIL_REQUEST_SET_3G_CAPABILITY";
            case RIL_REQUEST_GET_POL_CAPABILITY: return "RIL_REQUEST_GET_POL_CAPABILITY";
            case RIL_REQUEST_GET_POL_LIST: return "RIL_REQUEST_GET_POL_LIST";
            case RIL_REQUEST_SET_POL_ENTRY: return "RIL_REQUEST_SET_POL_ENTRY";     
            case RIL_REQUEST_QUERY_UPB_CAPABILITY: return "RIL_REQUEST_QUERY_UPB_CAPABILITY";
            case RIL_REQUEST_EDIT_UPB_ENTRY: return "RIL_REQUEST_EDIT_UPB_ENTRY";
            case RIL_REQUEST_DELETE_UPB_ENTRY: return "RIL_REQUEST_DELETE_UPB_ENTRY";
            case RIL_REQUEST_READ_UPB_GAS_LIST: return "RIL_REQUEST_READ_UPB_GAS_LIST";
            case RIL_REQUEST_READ_UPB_GRP: return "RIL_REQUEST_READ_UPB_GRP";
            case RIL_REQUEST_WRITE_UPB_GRP: return "RIL_REQUEST_WRITE_UPB_GRP";
            case RIL_REQUEST_DISABLE_VT_CAPABILITY: return "RIL_REQUEST_DISABLE_VT_CAPABILITY";
            case RIL_REQUEST_SET_SIM_RECOVERY_ON: return "RIL_REQUEST_SET_SIM_RECOVERY_ON";
            case RIL_REQUEST_GET_SIM_RECOVERY_ON: return "RIL_REQUEST_GET_SIM_RECOVERY_ON";
            //MTK-END [mtk04070][111121][ALPS00093395]MTK added

            case RIL_REQUEST_SET_TRM: return "RIL_REQUEST_SET_TRM";
            
            //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU: return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS: return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
            //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
            case RIL_REQUEST_DETECT_SIM_MISSING: return "RIL_REQUEST_DETECT_SIM_MISSING";

            //MTK-START [mtk80950][120410][ALPS00266631]check whether download calibration data or not
            case RIL_REQUEST_GET_CALIBRATION_DATA: return "GET_CALIBRATION_DATA";
            //MTK-END [mtk80950][120410][ALPS00266631]check whether download calibration data or not
            case RIL_REQUEST_GET_PHB_STRING_LENGTH: return "RIL_REQUEST_GET_PHB_STRING_LENGTH";
            case RIL_REQUEST_GET_PHB_MEM_STORAGE: return "RIL_REQUEST_GET_PHB_MEM_STORAGE";
            case RIL_REQUEST_SET_PHB_MEM_STORAGE: return "RIL_REQUEST_SET_PHB_MEM_STORAGE";
            case RIL_REQUEST_READ_PHB_ENTRY_EXT: return "RIL_REQUEST_READ_PHB_ENTRY_EXT";
            case RIL_REQUEST_WRITE_PHB_ENTRY_EXT: return "RIL_REQUEST_WRITE_PHB_ENTRY_EXT";

	    case RIL_REQUEST_QUERY_UIM_INSERTED: return "RIL_REQUEST_QUERY_UIM_INSERTED";
            
            case RIL_REQUEST_GET_SMS_PARAMS: return "RIL_REQUEST_GET_SMS_PARAMS";
            case RIL_REQUEST_SET_SMS_PARAMS: return "RIL_REQUEST_SET_SMS_PARAMS";
            
            case RIL_REQUEST_SET_ETWS: return "SET_ETWS";
            case RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO: return "RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO";
            case RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO: return "RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO";
            case RIL_REQUEST_GET_CB_CONFIG_INFO: return "RIL_REQUEST_GET_CB_CONFIG_INFO";
            case RIL_REQUEST_SET_ALL_CB_LANGUAGE_ON: return "RIL_REQUEST_SET_ALL_CB_LANGUAGE_ON";

            case RIL_REQUEST_DETACH_PS: return "RIL_REQUEST_DETACH_PS";
            
            default: return "<unknown request>";
        }
    }

    static String
    responseToString(int request)
    {
/*
 cat libs/telephony/ril_unsol_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{RIL_([^,]+),[^,]+,([^}]+).+/case RIL_\1: return "\1";/'
*/
        switch(request) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED: return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED: return "UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_NEW_SMS: return "UNSOL_RESPONSE_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT: return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM: return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            case RIL_UNSOL_ON_USSD: return "UNSOL_ON_USSD";
            case RIL_UNSOL_ON_USSD_REQUEST: return "UNSOL_ON_USSD_REQUEST";
            case RIL_UNSOL_NITZ_TIME_RECEIVED: return "UNSOL_NITZ_TIME_RECEIVED";
            case RIL_UNSOL_SIGNAL_STRENGTH: return "UNSOL_SIGNAL_STRENGTH";
            case RIL_UNSOL_DATA_CALL_LIST_CHANGED: return "UNSOL_DATA_CALL_LIST_CHANGED";
            case RIL_UNSOL_SUPP_SVC_NOTIFICATION: return "UNSOL_SUPP_SVC_NOTIFICATION";
            case RIL_UNSOL_STK_SESSION_END: return "UNSOL_STK_SESSION_END";
            case RIL_UNSOL_STK_PROACTIVE_COMMAND: return "UNSOL_STK_PROACTIVE_COMMAND";
            case RIL_UNSOL_STK_EVENT_NOTIFY: return "UNSOL_STK_EVENT_NOTIFY";
            case RIL_UNSOL_STK_CALL_SETUP: return "UNSOL_STK_CALL_SETUP";
            case RIL_UNSOL_SIM_SMS_STORAGE_FULL: return "UNSOL_SIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_SIM_REFRESH: return "UNSOL_SIM_REFRESH";
            case RIL_UNSOL_CALL_RING: return "UNSOL_CALL_RING";
            case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED: return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS: return "UNSOL_RESPONSE_CDMA_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS: return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
            case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL: return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_RESTRICTED_STATE_CHANGED: return "UNSOL_RESTRICTED_STATE_CHANGED";
            case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE: return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_CDMA_CALL_WAITING: return "UNSOL_CDMA_CALL_WAITING";
            case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS: return "UNSOL_CDMA_OTA_PROVISION_STATUS";
            case RIL_UNSOL_CDMA_INFO_REC: return "UNSOL_CDMA_INFO_REC";
            case RIL_UNSOL_OEM_HOOK_RAW: return "UNSOL_OEM_HOOK_RAW";
            case RIL_UNSOL_RINGBACK_TONE: return "UNSOL_RINGBACK_TONG";
            case RIL_UNSOL_RESEND_INCALL_MUTE: return "UNSOL_RESEND_INCALL_MUTE";
            case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED: return "CDMA_SUBSCRIPTION_SOURCE_CHANGED";
            case RIL_UNSOl_CDMA_PRL_CHANGED: return "UNSOL_CDMA_PRL_CHANGED";
            case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE: return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_RIL_CONNECTED: return "UNSOL_RIL_CONNECTED";
            case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED: return "UNSOL_VOICE_RADIO_TECH_CHANGED";

            //MTK-START [mtk04070][111121][ALPS00093395]MTK added
            case RIL_UNSOL_NEIGHBORING_CELL_INFO: return "UNSOL_NEIGHBORING_CELL_INFO";
            case RIL_UNSOL_NETWORK_INFO: return "UNSOL_NETWORK_INFO";
            case RIL_UNSOL_CALL_FORWARDING: return "UNSOL_CALL_FORWARDING";
            case RIL_UNSOL_CALL_PROGRESS_INFO: return "UNSOL_CALL_PROGRESS_INFO";
            case RIL_UNSOL_PHB_READY_NOTIFICATION: return "UNSOL_PHB_READY_NOTIFICATION";
            case RIL_UNSOL_SIM_INSERTED_STATUS: return "UNSOL_SIM_INSERTED_STATUS";
            case RIL_UNSOL_SIM_MISSING: return "UNSOL_SIM_MISSING";
            case RIL_UNSOL_VIRTUAL_SIM_ON: return "UNSOL_VIRTUAL_SIM_ON";
            case RIL_UNSOL_VIRTUAL_SIM_OFF: return "UNSOL_VIRTUAL_SIM_ON_OFF";
            case RIL_UNSOL_SIM_RECOVERY: return "UNSOL_SIM_RECOVERY";
            case RIL_UNSOL_SPEECH_INFO: return "UNSOL_SPEECH_INFO";
            case RIL_UNSOL_RADIO_TEMPORARILY_UNAVAILABLE: return "UNSOL_RADIO_TEMPORARILY_UNAVAILABLE";
            case RIL_UNSOL_ME_SMS_STORAGE_FULL: return "RIL_UNSOL_ME_SMS_STORAGE_FULL";
            case RIL_UNSOL_SMS_READY_NOTIFICATION: return "RIL_UNSOL_SMS_READY_NOTIFICATION";
            /* vt start */
            case RIL_UNSOL_VT_STATUS_INFO: return "UNSOL_VT_STATUS_INFO";
            case RIL_UNSOL_VT_RING_INFO: return "UNSOL_VT_RING_INFO";
            /* vt end */
            case RIL_UNSOL_SCRI_RESULT: return "RIL_UNSOL_SCRI_RESULT";
            case RIL_UNSOL_INCOMING_CALL_INDICATION: return "UNSOL_INCOMING_CALL_INDICATION";
            case RIL_UNSOL_GPRS_DETACH: return "RIL_UNSOL_GPRS_DETACH";
            // ALPS00302698 ENS
            case RIL_UNSOL_EF_CSP_PLMN_MODE_BIT: return "RIL_UNSOL_EF_CSP_PLMN_MODE_BIT";
            //MTK-END [mtk04070][111121][ALPS00093395]MTK added
            case RIL_UNSOL_IMEI_LOCK: return "UNSOL_IMEI_LOCK";
            case RIL_UNSOL_RESPONSE_ACMT: return "UNSOL_ACMT_INFO";
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED: return "UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED: return "UNSOL_RESPONSE_MMRR_STATUS_CHANGED";
            case RIL_UNSOL_SIM_PLUG_OUT: return "UNSOL_SIM_PLUG_OUT";
            case RIL_UNSOL_SIM_PLUG_IN: return "UNSOL_SIM_PLUG_IN";

            case RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION: return "RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION";
            
            default: return "<unknown reponse>";
        }
    }

    //MTK-START [mtk04070][111121][ALPS00093395]Add mySimId log
    private void riljLog(String msg) {
        Log.d(LOG_TAG, " RIL(" + (mySimId+1) + ") :" + msg);
    }

    private void riljLogv(String msg) {
        Log.v(LOG_TAG, " RIL(" + (mySimId+1) + ") :" + msg);
    }

    private void unsljLog(int response) {
        riljLog("[UNSL" + mySimId + "]< " + responseToString(response));
    }

    private void unsljLogMore(int response, String more) {
        riljLog("[UNSL RIL]< " + responseToString(response) + " " + more);
    }

    private void unsljLogRet(int response, Object ret) {
        riljLog("[UNSL RIL]< " + responseToString(response) + " " + retToString(response, ret));
    }

    private void unsljLogvRet(int response, Object ret) {
        riljLogv("[UNSL RIL]< " + responseToString(response) + " " + retToString(response, ret));
    }
    //MTK-END [mtk04070][111121][ALPS00093395]Add mySimId log


    // ***** Methods for CDMA support
    public void
    getDeviceIdentity(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DEVICE_IDENTITY, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    // NFC SEEK start
    public void
    iccExchangeAPDU(int cla, int command, int channel, int p1, int p2, int p3,
            String data, Message result) {
        RILRequest rr;
        if (channel == 0) {
            rr = RILRequest.obtain(RIL_REQUEST_SIM_TRANSMIT_BASIC, result);
        } else {
            rr = RILRequest.obtain(RIL_REQUEST_SIM_TRANSMIT_CHANNEL, result);
        }

        rr.mp.writeInt(cla);
        rr.mp.writeInt(command);
        rr.mp.writeInt(channel);
        rr.mp.writeString(null);
        rr.mp.writeInt(p1);
        rr.mp.writeInt(p2);
        rr.mp.writeInt(p3);
        rr.mp.writeString(data);
        rr.mp.writeString(null);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> iccExchangeAPDU: " + requestToString(rr.mRequest)
                + " 0x" + Integer.toHexString(cla)
                + " 0x" + Integer.toHexString(command)
                + " 0x" + Integer.toHexString(channel) + " "
                + p1 + "," + p2 + "," + p3);

        send(rr);
    }

    public void
    iccOpenChannel(String AID, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SIM_OPEN_CHANNEL, result);

        rr.mp.writeString(AID);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> iccOpenChannel: " + requestToString(rr.mRequest)
                + " " + AID);

        send(rr);
    }

    public void
    iccCloseChannel(int channel, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SIM_CLOSE_CHANNEL, result);
    
        rr.mp.writeInt(1);
        rr.mp.writeInt(channel);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> iccCloseChannel: " + requestToString(rr.mRequest)
                + " " + channel);

        send(rr);
    }

    public void
    iccGetATR(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SIM_GET_ATR, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);   
    }

    public void
    iccOpenChannelWithSw(String AID, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SIM_OPEN_CHANNEL_WITH_SW, result);

        rr.mp.writeString(AID);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> iccOpenChannelWithSw: " + requestToString(rr.mRequest)
                + " " + AID);

        send(rr);
    }

    // NFC SEEK end

    public void
    getCDMASubscription(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_SUBSCRIPTION, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override
    public void setPhoneType(int phoneType) { // Called by CDMAPhone and GSMPhone constructor
        if (RILJ_LOGD) riljLog("setPhoneType=" + phoneType + " old value=" + mPhoneType);
        mPhoneType = phoneType;
    }

    /**
     * {@inheritDoc}
     */
    public void queryCdmaRoamingPreference(Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(cdmaRoamingType);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + cdmaRoamingType);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setCdmaSubscriptionSource(int cdmaSubscription , Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(cdmaSubscription);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + cdmaSubscription);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getCdmaSubscriptionSource(Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void queryTTYMode(Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_QUERY_TTY_MODE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void setTTYMode(int ttyMode, Message response) {
        RILRequest rr = RILRequest.obtain(
                RILConstants.RIL_REQUEST_SET_TTY_MODE, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(ttyMode);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + ttyMode);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void
    sendCDMAFeatureCode(String FeatureCode, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_FLASH, response);

        rr.mp.writeString(FeatureCode);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " : " + FeatureCode);

        send(rr);
    }

    public void getCdmaBroadcastConfig(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG, response);

        send(rr);
    }

    // TODO: Change the configValuesArray to a RIL_BroadcastSMSConfig
    public void setCdmaBroadcastConfig(int[] configValuesArray, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG, response);

        for(int i = 0; i < configValuesArray.length; i++) {
            rr.mp.writeInt(configValuesArray[i]);
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void setCdmaBroadcastActivation(boolean activate, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CDMA_BROADCAST_ACTIVATION, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(activate ? 0 :1);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void exitEmergencyCallbackMode(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void requestIsimAuthentication(String nonce, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ISIM_AUTHENTICATION, response);

        rr.mp.writeString(nonce);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /* (non-Javadoc)
     * @see com.android.internal.telephony.BaseCommands#testingEmergencyCall()
     */
    @Override
    public void testingEmergencyCall() {
        if (RILJ_LOGD) riljLog("testingEmergencyCall");
        mTestingEmergencyCall.set(true);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("RIL:");
        pw.println(" mSocket=" + mSocket);
        pw.println(" mSenderThread=" + mSenderThread);
        pw.println(" mSender=" + mSender);
        pw.println(" mReceiverThread=" + mReceiverThread);
        pw.println(" mReceiver=" + mReceiver);
        pw.println(" mWakeLock=" + mWakeLock);
        pw.println(" mWakeLockTimeout=" + mWakeLockTimeout);
        synchronized (mRequestsList) {
          pw.println(" mRequestMessagesPending=" + mRequestMessagesPending);
          pw.println(" mRequestMessagesWaiting=" + mRequestMessagesWaiting);
            int count = mRequestsList.size();
            pw.println(" mRequestList count=" + count);
            for (int i = 0; i < count; i++) {
                RILRequest rr = mRequestsList.get(i);
                pw.println("  [" + rr.mSerial + "] " + requestToString(rr.mRequest));
            }
        }
        pw.println(" mLastNITZTimeInfo=" + mLastNITZTimeInfo);
        pw.println(" mTestingEmergencyCall=" + mTestingEmergencyCall.get());
    }

    //MTK-START [mtk04070][111121][ALPS00093395]MTK proprietary methods
    public void
    changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CHANGE_BARRING_PASSWORD, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(4);
        rr.mp.writeString(facility);
        rr.mp.writeString(oldPwd);
        rr.mp.writeString(newPwd);
        rr.mp.writeString(newCfm);
        send(rr);
    }
   
    public void
    emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_EMERGENCY_DIAL, result);

        rr.mp.writeString(address);
        rr.mp.writeInt(clirMode);
        rr.mp.writeInt(0); // UUS information is absent

        if (uusInfo == null) {
            rr.mp.writeInt(0); // UUS information is absent
        } else {
            rr.mp.writeInt(1); // UUS information is present
            rr.mp.writeInt(uusInfo.getType());
            rr.mp.writeInt(uusInfo.getDcs());
            rr.mp.writeByteArray(uusInfo.getUserData());
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }


    /* vt start */
    public void
    vtDial (String address, int clirMode, Message result) {
        vtDial(address, clirMode, null, result);
    }


    public void
    vtDial (String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_VT_DIAL, result);

        rr.mp.writeString(address);
        rr.mp.writeInt(clirMode);
        rr.mp.writeInt(0); // UUS information is absent

        if (uusInfo == null) {
            rr.mp.writeInt(0); // UUS information is absent
        } else {
            rr.mp.writeInt(1); // UUS information is present
            rr.mp.writeInt(uusInfo.getType());
            rr.mp.writeInt(uusInfo.getDcs());
            rr.mp.writeByteArray(uusInfo.getUserData());
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    voiceAccept(int callId, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_VOICE_ACCEPT, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + callId);

        rr.mp.writeInt(1);
        rr.mp.writeInt(callId);

        send(rr);
    }
    /* vt end */

    public void
    hangupAll (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP_ALL,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    hangupAllEx (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP_ALL_EX,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getCurrentCallMeter (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_CCM,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getAccumulatedCallMeter (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_ACM,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getAccumulatedCallMeterMaximum (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_ACMMAX,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    getPpuAndCurrency (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_PPU_AND_CURRENCY,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }	

    public void
    setAccumulatedCallMeterMaximum (String acmmax, String pin2, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_ACMMAX,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
		
        rr.mp.writeInt(2);
        rr.mp.writeString(acmmax);
        rr.mp.writeString(pin2);

        send(rr);
    }	

    public void
    resetAccumulatedCallMeter (String pin2, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RESET_ACM,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeString(pin2);

        send(rr);
    }

    public void
    setPpuAndCurrency (String currency, String ppu, String pin2, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_PPU_AND_CURRENCY,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(3);
        rr.mp.writeString(currency);
        rr.mp.writeString(ppu);
        rr.mp.writeString(pin2);

        send(rr);
    }

    public void
    setRadioPowerOff(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RADIO_POWEROFF,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    setRadioPowerOn(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RADIO_POWERON,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    setRadioMode(int mode, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DUAL_SIM_MODE_SWITCH,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(1);
        rr.mp.writeInt(mode);

        send(rr);
    }

    public void
    setGprsConnType(int type, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_GPRS_CONNECT_TYPE,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(1);
        rr.mp.writeInt(type);

        send(rr);
    }

    public void
    setGprsTransferType(int type, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_GPRS_TRANSFER_TYPE,
                                        result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(1);
        rr.mp.writeInt(type);


        if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            Intent intent = new Intent(TelephonyIntents.ACTION_GPRS_TRANSFER_TYPE);
            intent.putExtra(Phone.GEMINI_GPRS_TRANSFER_TYPE, type);
            mContext.sendStickyBroadcast(intent);
            riljLog("Broadcast: ACTION_GPRS_CONNECTION_TYPE_SELECT");
        }
        


        send(rr);
    }

    /*Add by mtk80372 for Barcode number*/
    public void
    getMobileRevisionAndIMEI(int type,Message result){
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_MOBILEREVISION_AND_IMEI, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(1);
        rr.mp.writeInt(type);

        send(rr);
    }  

    // mtk00732 add for getCOLP
    public void
    getCOLP(Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_GET_COLP, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }
    
    // mtk00732 add for setCOLP
    public void
    setCOLP(boolean enable, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_COLP, result);

        // count ints
        rr.mp.writeInt(1);

        if (enable) {
            rr.mp.writeInt(1);
        } else {
            rr.mp.writeInt(0);
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + enable);

        send(rr);
    }

    // mtk00732 add for getCOLR
    public void
    getCOLR(Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_GET_COLR, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override
    public void
    setNetworkSelectionModeManualWithAct(String operatorNumeric, String act, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT,
                                    response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + operatorNumeric + "" + act);

        rr.mp.writeInt(2);
        rr.mp.writeString(operatorNumeric);
		rr.mp.writeString(act);

        send(rr);
    }

    public void 
    setScri(boolean forceRelease, Message response) {
        RILRequest rr
            = RILRequest.obtain(RIL_REQUEST_SET_SCRI, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mp.writeInt(1);
        rr.mp.writeInt(forceRelease ? 1 : 0);

        send(rr);
    }

    //[New R8 modem FD]
    public void
    setFDMode(int mode, int parameter1, int parameter2, Message response) {
        if (FeatureOption.MTK_FD_SUPPORT) {    
            RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_FD_MODE, response);
		
            //AT+EFD=<mode>[,<param1>[,<param2>]]
            //mode=0:disable modem Fast Dormancy; mode=1:enable modem Fast Dormancy
            //mode=3:inform modem the screen status; parameter1: screen on or off
            //mode=2:Fast Dormancy inactivity timer; parameter1:timer_id; parameter2:timer_value
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        
            if (mode == 0 || mode == 1) {
                rr.mp.writeInt(1);
                rr.mp.writeInt(mode);
            } else if (mode == 3) {
                rr.mp.writeInt(2);
                rr.mp.writeInt(mode);
                rr.mp.writeInt(parameter1);			
            } else if (mode == 2) {
                rr.mp.writeInt(3);
                rr.mp.writeInt(mode);
                rr.mp.writeInt(parameter1);
                rr.mp.writeInt(parameter2);
            }		
            send(rr);    
        }		
    }	

    public void
    queryNetworkLock (int category, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_QUERY_SIM_NETWORK_LOCK, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        riljLog("queryNetworkLock:" + category);

        rr.mp.writeInt(1);
        
        rr.mp.writeInt(category);
        
        send(rr);
    }

    public void
    setNetworkLock (int catagory, int lockop, String password,
                        String data_imsi, String gid1, String gid2, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_SIM_NETWORK_LOCK, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        riljLog("setNetworkLock:" + catagory + "," + lockop + "," + password +"," + data_imsi);

        rr.mp.writeInt(6);
        rr.mp.writeString(Integer.toString(catagory));
        rr.mp.writeString(Integer.toString(lockop));
        if (null != password) {
            rr.mp.writeString(password);
        } else {
            rr.mp.writeString("");
        }
        rr.mp.writeString(data_imsi);
        rr.mp.writeString(gid1);
        rr.mp.writeString(gid2);
        
        send(rr);

    }

    /**
     * {@inheritDoc}
     */
	public void getSmsSimMemoryStatus(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_SMS_SIM_MEM_STATUS, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void queryPhbStorageInfo(int type, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_QUERY_PHB_STORAGE_INFO, response);

        rr.mp.writeInt(1);
        rr.mp.writeInt(type);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) +": " +type);

        send(rr);
    }
    
    /**
     * {@inheritDoc}
     */
    public void writePhbEntry(PhbEntry entry, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_WRITE_PHB_ENTRY, result);

        rr.mp.writeInt(entry.type);
        rr.mp.writeInt(entry.index);
        rr.mp.writeString(entry.number);
        rr.mp.writeInt(entry.ton);
        rr.mp.writeString(entry.alphaId);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + entry);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void ReadPhbEntry(int type, int bIndex, int eIndex, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_READ_PHB_ENTRY, response);

        rr.mp.writeInt(3);
        rr.mp.writeInt(type);
        rr.mp.writeInt(bIndex);
        rr.mp.writeInt(eIndex);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " 
                + type + " begin: " + bIndex + " end: " + eIndex);

        send(rr);
    }

    /**
     * {@inheritDoc}
     */
    public void sendBTSIMProfile(int nAction, int nType, String strData, Message response) {
        if (RILJ_LOGD) riljLog( " sendBTSIMProfile nAction is " + nAction);
	 switch (nAction)	{
            case 0:
              requestConnectSIM(response);
		break;
	     case 1:
		 requestDisconnectOrPowerOffSIM(nAction, response);
		 break;
	     case 2:
		 requestPowerOnOrResetSIM(nAction, nType, response);
		 break;	
	     case 3:
		 requestDisconnectOrPowerOffSIM(nAction, response);
		 break;	
	     case 4:
		 requestPowerOnOrResetSIM(nAction, nType, response);
		 break;		 
	     case 5:
		 requestTransferApdu( nAction, nType, strData, response);
		 break;				 
	 }
    }

    public void
    doSimAuthentication (String strRand, Message response) {
         RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SIM_AUTHENTICATION, response);

        rr.mp.writeString(strRand);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + strRand );

        send(rr);
    }

    public void
    doUSimAuthentication (String strRand, String strAutn,  Message response) {
         RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_USIM_AUTHENTICATION, response);
        rr.mp.writeInt(2);
        rr.mp.writeString(strRand);
	 rr.mp.writeString(strAutn);	
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + strRand + " " + strAutn);

        send(rr);
    }

    //***** Private Methods
    /**
    * used only by sendBTSIMProfile 
    */
    private void requestConnectSIM(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_BTSIM_CONNECT, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }
	
    /**
    * used only by sendBTSIMProfile 
    */
    private void requestDisconnectOrPowerOffSIM(int nAction, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF, response);

         rr.mp.writeString(Integer.toString(nAction));

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + nAction );

        send(rr);
    }
	
    /**
    * used only by sendBTSIMProfile 
    */
    private void requestPowerOnOrResetSIM(int nAction, int nType, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM, response);

        rr.mp.writeInt(2);
        rr.mp.writeString(Integer.toString(nAction));
        rr.mp.writeString(Integer.toString(nType));

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " 
                + nAction + " nType: " + nType );

        send(rr);
    }
	
    /**
    * used only by sendBTSIMProfile 
    */
    private void requestTransferApdu(int nAction, int nType, String strData, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_BTSIM_TRANSFERAPDU, response);

        rr.mp.writeInt(3);
        rr.mp.writeString(Integer.toString(nAction));
        rr.mp.writeString(Integer.toString(nType));
        rr.mp.writeString(strData);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " 
                + nAction + " nType: " + nType + " data: " + strData);

        send(rr);
    }

    public void queryUimInsertedStatus(Message result){
	RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_QUERY_UIM_INSERTED, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void notifyUimInsertedStatus(int status) {
        if (mSimMissing != null) {
            if (RILJ_LOGD) riljLog("[RILJ, notifyUimInsertedStatus]");
	    int result[] = {-1} ;
	    result[0] = status;
            mSimInsertedStatusRegistrants.notifyRegistrants(
                    new AsyncResult (null, result, null));
        }
        else {
            if (RILJ_LOGD) riljLog("[RILJ, can not notifyUimInsertedStatus]");
        }
    }	

    public int getSimInsertedStatus() {
        return mSimInsertedStatus;
    }

    private Object
    responseCrssNotification(Parcel p) {
        SuppCrssNotification notification = new SuppCrssNotification();

        notification.code = p.readInt();
        notification.type = p.readInt();
        notification.number = p.readString();
        notification.alphaid = p.readString();
	notification.cli_validity = p.readInt();

        return notification;
    }

    public static byte[] hexStringToBytes(String s) {
        byte[] ret;

        if (s == null) return null;

        int len = s.length();
        ret = new byte[len/2];

        for (int i=0 ; i <len ; i+=2) {
            ret[i/2] = (byte) ((hexCharToInt(s.charAt(i)) << 4)
                                | hexCharToInt(s.charAt(i+1)));
        }

        return ret;
    }

    static int hexCharToInt(char c) {
         if (c >= '0' && c <= '9') return (c - '0');
         if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
         if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

         throw new RuntimeException ("invalid hex char '" + c + "'");
    }


    private Object
    responseOperator(Parcel p) {
        int num;
        String response[] = null;

        response = p.readStringArray();

        if (false) {
            num = p.readInt();

            response = new String[num];
            for (int i = 0; i < num; i++) {
                response[i] = p.readString();
            }
        }

        /* ALPS00273663 handle UCS2 format name : prefix + hex string ex: "uCs2806F767C79D1" */	
        if((response[0] != null) && (response[0].startsWith("uCs2") == true))
        {        
            riljLog("responseOperator handling UCS2 format name");			        
            try{	
                response[0] = new String(hexStringToBytes(response[0].substring(4)),"UTF-16");
            }catch(UnsupportedEncodingException ex){
                riljLog("responseOperatorInfos UnsupportedEncodingException");
            }			
        }
		
        if (response[0] != null && (response[0].equals("") || response[0].equals(response[2]))) {
            riljLog("lookup RIL responseOperator()");
            response[0] = lookupOperatorName(response[2], true);
            response[1] = lookupOperatorName(response[2], false);
        }

        return response;
    }

    private Object
    responseNetworkInfoWithActs(Parcel p) {
        String strings[] = (String [])responseStrings(p);
        ArrayList<NetworkInfoWithAcT> ret;

        if (strings.length % 4 != 0) {
            throw new RuntimeException(
                "RIL_REQUEST_GET_POL_LIST: invalid response. Got "
                + strings.length + " strings, expected multible of 5");
        }

        ret = new ArrayList<NetworkInfoWithAcT>(strings.length / 4);
        
        String strOperName = null;
        String strOperNumeric = null;
        int nAct = 0;
        int nIndex = 0;
        
        for (int i = 0 ; i < strings.length ; i += 4) { 
            strOperName = null;
            strOperNumeric = null;
            if (strings[i] != null) {
                nIndex = Integer.parseInt(strings[i]);
            }else {
                Log.d(LOG_TAG, "responseNetworkInfoWithActs: no invalid index. i is " + i );  
            }
            
            if (strings[i+1] != null ) {
                int format = Integer.parseInt(strings[i+1]); 
                switch (format){
                    case 0:
                    case 1:
                        strOperName = strings[i+2];
                        break;
                    case 2:
                        if (strings[i+2] != null) {
                            strOperNumeric = strings[i+2];
                            strOperName = lookupOperatorName(strings[i+2], true);
                        }
                        break;
                    default:
                        break;
                }
            }
            
            if (strings[i+3] != null) {
                nAct = Integer.parseInt(strings[i+3]);
            }else {
                Log.d(LOG_TAG, "responseNetworkInfoWithActs: no invalid Act. i is " + i );  
            }   
            if(strOperNumeric != null && !strOperNumeric.equals("?????")) {
                ret.add (
                    new NetworkInfoWithAcT(
                        strOperName,
                        strOperNumeric,
                        nAct,
                        nIndex));
            }else {
                Log.d(LOG_TAG, "responseNetworkInfoWithActs: invalid oper. i is " + i );   
            }
        }

        return ret;
    }  

    private Object
    responsePhbEntries(Parcel p) {
        int numerOfEntries;
        PhbEntry[] response;

        numerOfEntries = p.readInt();
        response = new PhbEntry[numerOfEntries];

        Log.d(LOG_TAG, "Number: " + numerOfEntries);

        for(int i=0; i<numerOfEntries; i++) {
            response[i] = new PhbEntry();
            response[i].type = p.readInt();
            response[i].index = p.readInt();
            response[i].number = p.readString();
            response[i].ton = p.readInt();
            response[i].alphaId = p.readString();
        }

        return response;
    }

    private Object responseSimSmsMemoryStatus(Parcel p) {
        IccSmsStorageStatus response;

        response = new IccSmsStorageStatus();
		response.mUsed = p.readInt();
		response.mTotal= p.readInt();

        return response;
    }

    private Object responseGetPhbMemStorage(Parcel p) {
        PBMemStorage response = PBMemStorage.createFromParcel(p);
        riljLog("responseGetPhbMemStorage:" +  response);
        return response;
    }
    private Object responseReadPhbEntryExt(Parcel p) {
        int numerOfEntries;
        PBEntry[] response;

        numerOfEntries = p.readInt();
        response = new PBEntry[numerOfEntries];

        Log.d(LOG_TAG, "responseReadPhbEntryExt Number: " + numerOfEntries);

        for (int i = 0; i < numerOfEntries; i++) {
            response[i] = new PBEntry();
            response[i].setIndex1(p.readInt());
            response[i].setNumber(p.readString());
            response[i].setType(p.readInt());
            response[i].setText(p.readString());
            response[i].setHidden(p.readInt());
            
            response[i].setGroup(p.readString());
            response[i].setAdnumber(p.readString());
            response[i].setAdtype(p.readInt());
            response[i].setSecondtext(p.readString());
            response[i].setEmail(p.readString());
            Log.d(LOG_TAG, "responseReadPhbEntryExt[" + i + "] " + response[i].toString());
        }

        return response;
    }
    
    private Object responseEtwsNotification(Parcel p) {
        EtwsNotification response = new EtwsNotification();
        
        response.warningType = p.readInt();
        response.messageId = p.readInt();
        response.serialNumber = p.readInt();
        response.plmnId = p.readString();
        response.securityInfo = p.readString();
        
        return response;
    }
    
    private Object responseCbConfig(Parcel p) {
        int mode            = p.readInt();
        String channels     = p.readString();
        String languages    = p.readString();
        boolean allOn       = (p.readInt() == 1) ? true : false;
        
        return new CellBroadcastConfigInfo(mode, channels, languages, allOn);
    }
    
    /**
     * {@inheritDoc}
     */
    public void queryIccId(Message result){
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_QUERY_ICCID, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);   
    }

    // MVNO-API START
    private String lookupVirtualOperatorNameByEfSpn(String numeric, boolean desireLongName) {
        // no consider long name or short name, because document is not full.
        mSpnOverride = SpnOverride.getInstance();
        return mSpnOverride.getSpnByEfSpn(numeric, mPhone.getSpNameInEfSpn());
    }

    private String lookupVirtualOperatorNameByImsi(String numeric, boolean desireLongName) {
        // no consider long name or short name, because document is not full.
        mSpnOverride = SpnOverride.getInstance();
        return mSpnOverride.getSpnByImsi(numeric, mPhone.getSubscriberId());
    }

    private String lookupVirtualOperatorNameByEfPnn(String numeric, boolean desireLongName) {
        // no consider long name or short name, because document is not full.
        mSpnOverride = SpnOverride.getInstance();
        return mSpnOverride.getSpnByEfPnn(numeric, mPhone.getFirstFullNameInEfPnn());
    }
    // MVNO-API END

    public String lookupOperatorName(String numeric, boolean desireLongName) {
        Context context = mContext;
        String operName = numeric;

        // MVNO-API
        String mvnoOperName = null;
        if (FeatureOption.MTK_MVNO_SUPPORT) {
            mvnoOperName = lookupVirtualOperatorNameByEfSpn(numeric, desireLongName); // determine by EF_SPN
            riljLog("the result of searching mvnoOperName by EF_SPN: " + mvnoOperName);

            if(mvnoOperName == null) // determine by IMSI
                mvnoOperName = lookupVirtualOperatorNameByImsi(numeric, desireLongName); 
            riljLog("the result of searching mvnoOperName by IMSI: " + mvnoOperName);

            if(mvnoOperName == null)
                mvnoOperName = lookupVirtualOperatorNameByEfPnn(numeric, desireLongName); 
            riljLog("the result of searching mvnoOperName by EF_PNN: " + mvnoOperName);

            if(mvnoOperName != null)
                operName = mvnoOperName;
        }        
        
        if (mvnoOperName == null && desireLongName) { // MVNO-API     
            if ((numeric.equals("46000")) || (numeric.equals("46002")) || (numeric.equals("46007"))) {
                operName = context.getText(com.mediatek.R.string.oper_long_46000).toString();
            } else if (numeric.equals("46001")) {
                operName = context.getText(com.mediatek.R.string.oper_long_46001).toString();
            } else if (numeric.equals("46003")) {
                operName = context.getText(com.mediatek.R.string.oper_long_46003).toString();					
            } else if (numeric.equals("46601")) {
                operName = context.getText(com.mediatek.R.string.oper_long_46601).toString();
            } else if (numeric.equals("46692")) {
                operName = context.getText(com.mediatek.R.string.oper_long_46692).toString();
            } else if (numeric.equals("46697")) {
                operName = context.getText(com.mediatek.R.string.oper_long_46697).toString();
            } else if (numeric.equals("99998")) {
                operName = context.getText(com.mediatek.R.string.oper_long_99998).toString();
            } else if (numeric.equals("99999")) {
                operName = context.getText(com.mediatek.R.string.oper_long_99999).toString();
            } else {
                // If can't found corresspoding operator in string resource, lookup from spn_conf.xml 
                mSpnOverride = SpnOverride.getInstance();
                if (mSpnOverride.containsCarrier(numeric)) {
                    operName = mSpnOverride.getSpn(numeric);
                } else {
                    riljLog("Can't find long operator name for " + numeric);
                }
            }
        }
        else if (mvnoOperName == null && desireLongName == false) // MVNO-API
        {
            if ((numeric.equals("46000")) || (numeric.equals("46002")) || (numeric.equals("46007"))) {
                operName = context.getText(com.mediatek.R.string.oper_short_46000).toString();
            } else if (numeric.equals("46001")) {
                operName = context.getText(com.mediatek.R.string.oper_short_46001).toString();
            } else if (numeric.equals("46003")) {
                operName = context.getText(com.mediatek.R.string.oper_short_46003).toString();				
            } else if (numeric.equals("46601")) {
                operName = context.getText(com.mediatek.R.string.oper_short_46601).toString();
            } else if (numeric.equals("46692")) {
                operName = context.getText(com.mediatek.R.string.oper_short_46692).toString();
            } else if (numeric.equals("46697")) {
                operName = context.getText(com.mediatek.R.string.oper_short_46697).toString();
            } else if (numeric.equals("99997")) {
                operName = context.getText(com.mediatek.R.string.oper_short_99997).toString();
            } else if (numeric.equals("99999")) {
                operName = context.getText(com.mediatek.R.string.oper_short_99999).toString();
            } else {
                riljLog("Can't find short operator name for " + numeric);
            }
        }
                
        return operName;
    }

    public void forceReleaseCall(int index, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_FORCE_RELEASE_CALL, result);

        rr.mp.writeInt(1);
        rr.mp.writeInt(index);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + index);

        send(rr);
    }

    public void setCallIndication(int mode, int callId, int seqNumber, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_CALL_INDICATION, result);

        rr.mp.writeInt(3);
        rr.mp.writeInt(mode);
        rr.mp.writeInt(callId);
        rr.mp.writeInt(seqNumber);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + mode + ", " + callId + ", " + seqNumber);

        send(rr);
    }

    public void replaceVtCall(int index, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_REPLACE_VT_CALL, result);

        rr.mp.writeInt(1);
        rr.mp.writeInt(index);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void get3GCapabilitySIM(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_3G_CAPABILITY, response);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void set3GCapabilitySIM(int simId, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_3G_CAPABILITY, response);
        rr.mp.writeInt(1);
        rr.mp.writeInt(simId);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void getPOLCapabilty(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_POL_CAPABILITY, response);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    } 
    
    public void getCurrentPOLList(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_POL_LIST, response);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);       
    }
    
    public void setPOLEntry(int index, String numeric, int nAct, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_POL_ENTRY, response);
        if(numeric == null || (numeric.length() == 0)){
            rr.mp.writeInt(1);
            rr.mp.writeString(Integer.toString(index));
        }else {
            rr.mp.writeInt(3);
            rr.mp.writeString(Integer.toString(index));
            rr.mp.writeString(numeric);
            rr.mp.writeString(Integer.toString(nAct));
        }
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);       
    }   

    public void queryUPBCapability(Message response){
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_QUERY_UPB_CAPABILITY, response);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal, String tonForNum, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_EDIT_UPB_ENTRY, response);
        if (entryType == 0){
            rr.mp.writeInt(5);
        }else {
            rr.mp.writeInt(4);
        }       
        rr.mp.writeString(Integer.toString(entryType));
        rr.mp.writeString(Integer.toString(adnIndex));
        rr.mp.writeString(Integer.toString(entryIndex));
        rr.mp.writeString(strVal);
        
        if (entryType == 0){
            rr.mp.writeString(tonForNum);
        }
        
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);   

    }

    public void deleteUPBEntry(int entryType, int adnIndex, int entryIndex, Message response){
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DELETE_UPB_ENTRY, response);
        rr.mp.writeInt(3);
        rr.mp.writeInt(entryType);
        rr.mp.writeInt(adnIndex);
        rr.mp.writeInt(entryIndex);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void readUPBGasList(int startIndex, int endIndex, Message response){
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_READ_UPB_GAS_LIST, response);
        rr.mp.writeInt(2);
        rr.mp.writeInt(startIndex);
        rr.mp.writeInt(endIndex);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);       
    }
    
    public void readUPBGrpEntry(int adnIndex, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_READ_UPB_GRP, response);
        rr.mp.writeInt(1);
        rr.mp.writeInt(adnIndex);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);  
    }

    public void writeUPBGrpEntry(int adnIndex, int[] grpIds, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_WRITE_UPB_GRP, response);
        int nLen = grpIds.length;
        rr.mp.writeInt(nLen + 1);
        rr.mp.writeInt(adnIndex);
        for(int i = 0; i < nLen; i++) {
            rr.mp.writeInt(grpIds[i]);
        }
        if (RILJ_LOGD) riljLog("writeUPBGrpEntry nLen is " + nLen);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);  

    }

    //MTK-START [mtk04070][120104][ALPS00109412]Solve "Disable modem VT capability if AP VT compile option is closed"
    //Merge from ALPS00096155
    private void disableVTCapability() {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DISABLE_VT_CAPABILITY, null);
        
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }	
    public void setPreferredNetworkTypeRIL(int NetworkType)
    {
  	if (RILJ_LOGD) riljLog("setPreferredNetworkTypeRIL NetworkType=" + NetworkType);
        mPreferredNetworkType=NetworkType;
    }
    //MTK-END [mtk04070][120104][ALPS00109412]Solve "Disable modem VT capability if AP VT compile option is closed"

    private static final int MSG_GET_DATA_CALL_LIST_DONE = 0;
    private Handler mDataListChangedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_GET_DATA_CALL_LIST_DONE) {
                if (RILJ_LOGD) riljLog("get MSG_GET_DATA_CALL_LIST_DONE, notify data call list changed");
                mDataNetworkStateRegistrants.notifyRegistrants((AsyncResult) msg.obj);
            }
        }
    };
    //MTK-END [mtk04070][111121][ALPS00093395]MTK proprietary methods

    //MTK-START [mtkXXXXX][120208][APLS00109092] Replace "RIL_UNSOL_SIM_MISSING in RIL.java" with "acively query SIM missing status"
    public void notifySimMissing() {
        if (mSimMissing != null) {
            if (RILJ_LOGD) riljLog("[RILJ, notifySimMissing]");
            mSimMissing.notifyRegistrants(new AsyncResult (null, null, null));
        }
        else {
            if (RILJ_LOGD) riljLog("[RILJ, can not notifySimMissing]");
        }
    }
    public void detectSimMissing(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DETECT_SIM_MISSING, result);

    
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }
    
    public void setSimRecoveryOn(int Type, Message response)
    {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_SIM_RECOVERY_ON, null);

        //ALPS00255599
        rr.mp.writeInt(1);		
        rr.mp.writeInt(Type);
		
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    
    public void getSimRecoveryOn(Message response)
    {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_SIM_RECOVERY_ON, null);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    //MTK-END [mtkXXXXX][120208][APLS00109092] Replace "RIL_UNSOL_SIM_MISSING in RIL.java" with "acively query SIM missing status"

    //ALPS00248788
    public void setTRM(int mode, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_TRM, null);

        rr.mp.writeInt(1);		
        rr.mp.writeInt(mode);
		
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);    
    }

    //MTK-START [mtk80950][120410][ALPS00266631]check whether download calibration data or not
    public void getCalibrationData(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_CALIBRATION_DATA, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

        send(rr);
    }
    //MTK-END [mtk80950][120410][ALPS00266631]check whether download calibration data or not 
    
    /**
     * at+cpbr=?
     * @return  <nlength><tlength><glength><slength><elength>
     */
    public void getPhoneBookStringsLength(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_PHB_STRING_LENGTH, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

        send(rr);
    }
    
    /**
     * at+cpbs?
     * @return  PBMemStorage :: +cpbs:<storage>,<used>,<total>
     */
    public void getPhoneBookMemStorage(Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_PHB_MEM_STORAGE, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

        send(rr);
    }
    
    /**
     * at+epin2=<p2>; at+cpbs=<storage> 
     * @return 
     */
    public void setPhoneBookMemStorage(String storage, String password, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_PHB_MEM_STORAGE, result);
        rr.mp.writeInt(2);      
        rr.mp.writeString(storage);
        rr.mp.writeString(password);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));

        send(rr);
    }
    
    /**
     * M at+cpbr=<index1>,<index2>
     * +CPBR:<indexn>,<number>,<type>,<text>,<hidden>,<group>,<adnumber>,<adtype>,<secondtext>,<email>
     */
    public void readPhoneBookEntryExt(int index1, int index2, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_READ_PHB_ENTRY_EXT, result);
        rr.mp.writeInt(2);
        rr.mp.writeInt(index1);
        rr.mp.writeInt(index2);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> :::" + requestToString(rr.mRequest));
        send(rr);
    }
    
    /**
     * M AT+CPBW=<index>,<number>,<type>,<text>,<hidden>,<group>,<adnumber>,<adtype>,<secondtext>,<email>
     */
    public void writePhoneBookEntryExt(PBEntry entry, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_WRITE_PHB_ENTRY_EXT, result);

        rr.mp.writeInt(entry.getIndex1());
        rr.mp.writeString(entry.getNumber());
        rr.mp.writeInt(entry.getType());
        rr.mp.writeString(entry.getText());
        rr.mp.writeInt(entry.getHidden());
        
        rr.mp.writeString(entry.getGroup());
        rr.mp.writeString(entry.getAdnumber());
        rr.mp.writeInt(entry.getAdtype());
        rr.mp.writeString(entry.getSecondtext());
        rr.mp.writeString(entry.getEmail());

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + ": " + entry);

        send(rr);
    }
    
    /**
     * {@inheritDoc}
     */
    public void getSmsParameters(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_SMS_PARAMS, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setSmsParameters(SmsParameters params, Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_SMS_PARAMS, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        
        rr.mp.writeInt(4);
        rr.mp.writeInt(params.format);
        rr.mp.writeInt(params.vp);
        rr.mp.writeInt(params.pid);
        rr.mp.writeInt(params.dcs);

        send(rr);
    }

    public boolean isGettingAvailableNetworks() {
        return (getAvailableNetworkRequests > 0);
    }
    
    public void setCellBroadcastChannelConfigInfo(String config, int cb_set_type, Message response) {
        RILRequest rr = RILRequest.obtain(
                RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO, 
                response);
        rr.mp.writeInt(2);
        rr.mp.writeString(config);
        rr.mp.writeString(Integer.toString(cb_set_type));
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    
    public void setCellBroadcastLanguageConfigInfo(String config, Message response) {
        RILRequest rr = RILRequest.obtain(
                RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO, 
                response);
        
        rr.mp.writeString(config);
        
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    
    public void queryCellBroadcastConfigInfo(Message response) {
        final long tid = Thread.currentThread().getId();
        Log.d(LOG_TAG, "[CB thread " + tid + " run queryCellBroadcastConfigInfo");
        RILRequest rr = RILRequest.obtain(
                RIL_REQUEST_GET_CB_CONFIG_INFO, 
                response);
                
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    
    public void setAllCbLanguageOn(Message response) {
        RILRequest rr = RILRequest.obtain(
                RIL_REQUEST_SET_ALL_CB_LANGUAGE_ON, 
                response);
                
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void setEtws(int mode, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_ETWS, result);

        rr.mp.writeInt(1);
        rr.mp.writeInt(mode);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + mode);

        send(rr);
    }

    public void detachPS(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_DETACH_PS, response);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    
    // this receiver is used to listen CatService init. After init is finished,
    // CatService will notify RIL to send cached stk command
    private BroadcastReceiver mCatServiceInitListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CatService.ACTION_CAT_INIT_DONE)) {
                int sim_id = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                if (RILJ_LOGD) riljLog("mCatServiceInitListener: sim_id: " + sim_id + ", mySimId: " + mySimId);
                if (mStkPciObject != null && sim_id == mySimId) {
                    Xlog.d(LOG_TAG, "[CachedStk send cached command to CatService");
                    mCatProCmdRegistrant.notifyRegistrant(
                            new AsyncResult (null, mStkPciObject, null));
                    mStkPciObject = null;
                }
            }
        }
    };

    /*
     * to protect modem status we need to avoid two case : 
     * 1. DTMF start -> CHLD request -> DTMF stop 
     * 2. CHLD request -> DTMF request
     */
    private void handleChldRelatedRequest(RILRequest rr) {
        synchronized (mDtmfQueue) { 
            int queueSize = mDtmfQueue.size();
            int i,j;
            if(queueSize > 0) {
                RILRequest rr2 = (RILRequest)mDtmfQueue.get(0);
                if(rr2.mRequest == RIL_REQUEST_DTMF_START) {
                    // need to send the STOP command
                    if (RILJ_LOGD) riljLog("DTMF queue isn't 0, first request is START, send stop dtmf and pending switch");
                    if(queueSize > 1) {
                        j = 2;
                    } else {
                        // need to create a new STOP command
                        j = 1;
                    }
                    if (RILJ_LOGD) riljLog("queue size  "+ mDtmfQueue.size());
                    
                    for(i = queueSize-1; i >= j; i--) {
                        mDtmfQueue.removeElementAt(i);
                    }
                    if (RILJ_LOGD) riljLog("queue size  after "+ mDtmfQueue.size());
                    if(mDtmfQueue.size() == 1) { // only start command, we need to add stop command
                        RILRequest rr3 = RILRequest.obtain(RIL_REQUEST_DTMF_STOP, null);
                        if (RILJ_LOGD) riljLog("add dummy stop dtmf request");
                        mDtmfStatus = DTMF_STATUS_STOP;
                        mDtmfQueue.addElement(rr3);
                    }
                } 
                else {
                    // first request is STOP, just remove it and send switch
                    if (RILJ_LOGD) riljLog("DTMF queue isn't 0, first request is STOP, penging switch");
                    j = 1;
                    for(i = queueSize; i >= j; i--) {
                        mDtmfQueue.removeElementAt(i);
                    }
                }
                mPendingCHLDRequest = rr;
            } else {
                if (RILJ_LOGD) riljLog("DTMF queue is 0, send switch Immediately");
                mIsSendChldRequest = true;
                send(rr);
            }
        }
    }
}
