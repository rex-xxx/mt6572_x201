/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.mediatek.MediatekDM.conn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.mediatek.MediatekDM.DmApplication;
import com.mediatek.MediatekDM.DmClient;
import com.mediatek.MediatekDM.DmCommonFunction;
import com.mediatek.MediatekDM.DmConst;
import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.DmService;
import com.mediatek.MediatekDM.data.IDmPersistentValues;
import com.mediatek.MediatekDM.ext.MTKConnectivity;
import com.mediatek.MediatekDM.ext.MTKOptions;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.option.Options;
import com.mediatek.MediatekDM.session.SessionEventQueue;
import com.mediatek.MediatekDM.util.ScreenLock;

import junit.framework.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DmDataConnection {
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;

    private ConnectivityReceiver mConnectivityReceiver = null;
    private ConnectivityManager mConnMgr;
    private Context mContext;
    private DmDatabase mDmDatabase;

    private int simId = -1;

    private static Handler clientHandler = null;
    private static Handler serviceHandler = null;

    private static DmDataConnection instance = null;

    // extended message handler
    private Handler userMsgHandler = null;

    public void setUserHandler(Handler hd) {
        userMsgHandler = hd;
    }

    private DmDataConnection(Context context) {
        mContext = context;
        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        // register the CONNECTIVITY_ACTION receiver
        mConnectivityReceiver = new ConnectivityReceiver();
        // init DmDatabase
        mDmDatabase = new DmDatabase(context);
        if (serviceHandler == null) {
            if (DmService.getServiceInstance() != null) {
                serviceHandler = DmService.getServiceInstance().mHandler;
            }
        }
        IntentFilter intent = new IntentFilter();
        intent.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intent.addAction(DmConst.IntentAction.NET_DETECT_TIMEOUT);
        mContext.registerReceiver(mConnectivityReceiver, intent);

    }

    public static DmDataConnection getInstance(Context context) {
        if (instance == null) {
            instance = new DmDataConnection(context);
        }
        return instance;
    }

    public int startDmDataConnectivity() throws IOException {
        Assert.assertFalse("startDmDataConnectivity MUST NOT be called in direct internet conn.",
                Options.UseDirectInternet);
        // if gemini is set
        int result = -1;
        simId = DmCommonFunction.getRegisteredSimId(mContext);
        if (simId == -1) {
            Log.e(TAG.Connection, "Get Register SIM ID error in start data connection");
            return result;
        }
        // for gemini
        if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
            // begin for connectity gemini
            if (mDmDatabase.DmApnReady(simId) == false) {
                Log.e(TAG.Connection, "Dm apn table is not ready!");
                return result;
            }
            result = beginDmDataConnectivityGemini(simId);
        } else {
            if (mDmDatabase.DmApnReady(GEMINI_SIM_1) == false) {
                Log.e(TAG.Connection, "Dm apn table is not ready!");
                return result;
            }
            result = beginDmDataConnectivity();
        }

        if (result == MTKPhone.APN_TYPE_NOT_AVAILABLE || result == MTKPhone.APN_REQUEST_FAILED) {
            Log.e(TAG.Connection, "start Dmdate Connectivity error");
        }

        // for test begin
        if (result == MTKPhone.APN_ALREADY_ACTIVE) {
            Log.i(TAG.Connection,
                    "DataConnection is already exist and send MSG_WAP_CONNECTION_SUCCESS to client");

            notifyHandlers(IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS);
        }

        // for test end
        return result;
    }

    public void stopDmDataConnectivity() {
        Assert.assertFalse("stopDmDataConnectivity MUST NOT be called in direct internet conn.",
                Options.UseDirectInternet);
        Log.v(TAG.Connection, "stopDmDataConnectivity");
        try {
            simId = DmCommonFunction.getRegisteredSimId(mContext);
            if (simId == -1) {
                Log.e(TAG.Connection, "Get Register SIM ID error in stop data connection");
                return;
            }
            if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
                // begin for connectity gemini
                endDmConnectivityGemini(simId);
            } else {
                endDmDataConnectivity();
            }
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.enableKeyguard(mContext);
        } finally {
            Log.v(TAG.Connection, "stopUsingNetworkFeature end");
        }
    }

    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                return;
            }
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d(TAG.Connection,
                        "ConnectivityReceiver Receive android.net.conn.CONNECTIVITY_CHANGE");
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    NetworkInfo info = (NetworkInfo) bundle
                            .get(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (info == null) {
                        Log.e(TAG.Connection, "[dm-conn]->Get NetworkInfo error");
                        return;
                    }
                    simId = DmCommonFunction.getRegisteredSimId(mContext);
                    if (simId == -1) {
                        Log.e(TAG.Connection,
                                "[dm-conn]->Get Register SIM ID error in connetivity receiver");
                        return;
                    }
                    int networkSimId = MTKConnectivity.getSimId(info);
                    int intentSimId = intent.getIntExtra(MTKConnectivity.EXTRA_SIM_ID, 0);
                    int networkType = info.getType();

                    if (intentSimId == simId && networkType == MTKConnectivity.TYPE_MOBILE_DM) {
                        Log.i(TAG.Connection, "[dm-conn]->type == " + info.getTypeName() + "("
                                + networkType + ")");
                        Log.i(TAG.Connection, "[dm-conn]->intent_sim_Id == " + intentSimId);
                        Log.i(TAG.Connection, "[dm-conn]->network_sim_Id == " + networkSimId);
                        Log.i(TAG.Connection, "[dm-conn]->registered_sim_Id == " + simId);

                        State state = info.getState();
                        if (state == State.CONNECTED) {
                            Log.i(TAG.Connection, "[dm-conn]->state == CONNECTED");
                            try {
                                ensureRouteToHost();

                                // store CONNECTED event.
                                DmApplication.getInstance().queueEvent(
                                        SessionEventQueue.EVENT_CONN_CONNECTED);
                                Log.i(TAG.Connection, ">>sending msg WAP_CONN_SUCCESS");
                                notifyHandlers(IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS);

                            } catch (Exception ex) {
                                Log.e(TAG.Connection, "[dm-conn]->ensureRouteToHost() failed:", ex);
                            }
                        } else if (state == State.CONNECTING) {
                            Log.i(TAG.Connection, "[dm-conn]->state == CONNECTING");
                            return;
                        } else if (state == State.DISCONNECTED) {
                            Log.i(TAG.Connection, "[dm-conn]->state == DISCONNECTED");

                            // store DISCONNECTED event.
                            DmApplication.getInstance().queueEvent(
                                    SessionEventQueue.EVENT_CONN_DISCONNECTED);
                            return;
                        }
                    }
                }
            } else if (intent.getAction().equalsIgnoreCase(DmConst.IntentAction.NET_DETECT_TIMEOUT)) {
                Log.i(TAG.Connection,
                        "[dm-conn]->action == com.mediatek.MediatekDM.NETDETECTTIMEOUT");

                Log.i(TAG.Connection, ">>>sending msg WAP_CONN_TIMEOUT");
                notifyHandlers(IDmPersistentValues.MSG_WAP_CONNECTION_TIMEOUT);
            }
        }
    }

    private int beginDmDataConnectivity() throws IOException {

        int result = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                MTKPhone.FEATURE_ENABLE_DM);

        Log.i(TAG.Connection, "[dm-conn]->startUsingNetworkFeature: result=" + result);

        if (result == MTKPhone.APN_ALREADY_ACTIVE) {
            Log.i(TAG.Connection, "[dm-conn]->APN_ALREADY_ACTIVE");
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.acquirePartialWakelock(mContext);
            ensureRouteToHost();
        } else if (result == MTKPhone.APN_REQUEST_STARTED) {
            Log.i(TAG.Connection, "[dm-conn]->APN_REQUEST_STARTED, waiting for intent.");
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.acquirePartialWakelock(mContext);
            // mContext.registerReceiver(mConnectivityReceiver, new
            // IntentFilter(
            // ConnectivityManager.CONNECTIVITY_ACTION));
            // mTelephonyManager=(TelephonyManager)
            // mContext.getSystemService(Service.TELEPHONY_SERVICE);
        } else if (result == MTKPhone.APN_REQUEST_FAILED) {
            Log.e(TAG.Connection, "[dm-conn]->APN_REQUEST_FAILED");
        } else {
            throw new IOException("[dm-conn]:Cannot establish DM data connectivity");
        }

        return result;
    }

    private void endDmDataConnectivity() {
        try {
            Log.v(TAG.Connection, "endDmDataConnectivity");

            if (mConnMgr != null) {
                mConnMgr.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                        MTKPhone.FEATURE_ENABLE_DM);
            }
        } finally {
            Log.v(TAG.Connection, "stopUsingNetworkFeature end");
        }
    }

    private int beginDmDataConnectivityGemini(int simId) throws IOException {

        int result = MTKConnectivity.startUsingNetworkFeatureGemini(mConnMgr,
                ConnectivityManager.TYPE_MOBILE,
                MTKPhone.FEATURE_ENABLE_DM, simId);

        Log.i(TAG.Connection, "startDmDataConnectivityGemini: simId = " + simId + "\t result="
                + result);

        if (result == MTKPhone.APN_ALREADY_ACTIVE) {
            Log.w(TAG.Connection, "The data connection is already exist, go ahead");
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.acquirePartialWakelock(mContext);
            ensureRouteToHost();
        } else if (result == MTKPhone.APN_REQUEST_STARTED) {
            Log.w(TAG.Connection,
                    "The new data connection is started register and waiting for the intent");
            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.acquirePartialWakelock(mContext);
            // mContext.registerReceiver(mConnectivityReceiver, new
            // IntentFilter(
            // ConnectivityManager.CONNECTIVITY_ACTION));
            // mContext.registerReceiver(mConnectivityReceiver, new
            // IntentFilter(DmConst.IntentAction.NET_DETECT_TIMEOUT));
            // mTelephonyManager=(TelephonyManager)
            // mContext.getSystemService(Service.TELEPHONY_SERVICE);
        } else if (result == MTKPhone.APN_REQUEST_FAILED) {
            Log.e(TAG.Connection, "startUsingnetworkfeature failed");
        } else {
            throw new IOException("Cannot establish Dm Data connectivity");
        }
        return result;
    }

    // add for gemini
    private void endDmConnectivityGemini(int simId) {
        try {
            Log.i(TAG.Connection, "endDmDataConnectivityGemini: simId = " + simId);

            if (mConnMgr != null) {
                MTKConnectivity.stopUsingNetworkFeatureGemini(mConnMgr,
                        ConnectivityManager.TYPE_MOBILE,
                        MTKPhone.FEATURE_ENABLE_DM, simId);
            }
        } finally {
            Log.v(TAG.Connection, "stopUsingNetworkFeature end");
        }
    }

    private void ensureRouteToHost() throws IOException {
        Log.v(TAG.Connection, "Begin ensureRouteToHost");
        // call getApnInfoFromSettings
        String proxyAddr = mDmDatabase.getApnProxyFromSettings();
        int inetAddr = lookupHost(proxyAddr);
        Log.i(TAG.Connection, "inetAddr = " + inetAddr);

        // get the addr form setting
        if (!mConnMgr.requestRouteToHost(MTKConnectivity.TYPE_MOBILE_DM, inetAddr)) {
            throw new IOException("Cannot establish route to proxy " + inetAddr);
        }

    }

    public static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                | (addrBytes[0] & 0xff);
        return addr;
    }

    private void notifyHandlers(int msgCode) {
        clientHandler = null;
        if (DmClient.getMdmClientInstance() != null) {
            clientHandler = DmClient.getMdmClientInstance().apnConnHandler;
        }
        // }
        if (serviceHandler == null) {
            if (DmService.getServiceInstance() != null) {
                serviceHandler = DmService.getServiceInstance().mHandler;
            }
        }
        if (clientHandler != null) {
            clientHandler.sendMessage(clientHandler.obtainMessage(msgCode));
        }
        if (serviceHandler != null) {
            serviceHandler.sendMessage(serviceHandler.obtainMessage(msgCode));
        }

        // extended message handler
        if (userMsgHandler != null) {
            userMsgHandler.sendMessage(userMsgHandler.obtainMessage(msgCode));
        }
    }

    private void destroyDataConnection() {
        mContext.unregisterReceiver(mConnectivityReceiver);
        mContext = null;
    }

    public static void destroyInstance() {
        instance.destroyDataConnection();
        serviceHandler = null;
        instance = null;
    }

}
