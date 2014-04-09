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

import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CellInfo;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.featureoption.FeatureOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * broadcast intents
 */
public class DefaultPhoneNotifier implements PhoneNotifier {

    static final String LOG_TAG = "GSM";
    private static final boolean DBG = true;
    private ITelephonyRegistry mRegistry;
    //MTK-START [mtk04070][111125][ALPS00093395]MTK added
    /* Add by vendor for Multiple PDP Context */
    private static final String TAG = "DefaultPhoneNotifier";
    //MTK-END [mtk04070][111125][ALPS00093395]MTK added
    private int mSimId;

    // M: For SIM indication state chage
    private DataState mDataState = DataState.DISCONNECTED;

    //MTK modified, make it public
    public DefaultPhoneNotifier() {
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                    "telephony.registry"));
    }

    public void notifyPhoneState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        String incomingNumber = "";
        if (ringingCall != null && ringingCall.getEarliestConnection() != null){
            incomingNumber = ringingCall.getEarliestConnection().getAddress();
        }
        try {
            mRegistry.notifyCallState(convertCallState(sender.getState()), incomingNumber);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyServiceState(Phone sender) {
        ServiceState ss = sender.getServiceState();
        if (ss == null) {
            ss = new ServiceState();
            ss.setStateOutOfService();
        }
        try {
            mRegistry.notifyServiceState(ss);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifySignalStrength(Phone sender) {
        try {
            mRegistry.notifySignalStrength(sender.getSignalStrength());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyMessageWaitingChanged(Phone sender) {
        try {
            mRegistry.notifyMessageWaitingChanged(sender.getMessageWaitingIndicator());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCallForwardingChanged(Phone sender) {
        try {
            mRegistry.notifyCallForwardingChanged(sender.getCallForwardingIndicator());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataActivity(Phone sender) {
        try {
            mRegistry.notifyDataActivity(convertDataActivityState(sender.getDataActivityState()));
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    //MTK-START [mtk04070][111125][ALPS00093395]MTK modified
    public void notifyDataConnection(Phone sender, String reason, String apnType,
            PhoneConstants.DataState state) {

        doNotifyDataConnection(sender, reason, apnType, state);
    }
    //MTK-END [mtk04070][111125][ALPS00093395]MTK modified

    private void doNotifyDataConnection(Phone sender, String reason, String apnType,
            PhoneConstants.DataState state) {
        // TODO
        // use apnType as the key to which connection we're talking about.
        // pass apnType back up to fetch particular for this one.
        TelephonyManager telephony = TelephonyManager.getDefault();
        LinkProperties linkProperties = null;
        LinkCapabilities linkCapabilities = null;
        boolean roaming = false;

        if (state == PhoneConstants.DataState.CONNECTED) {
            linkProperties = sender.getLinkProperties(apnType);
            linkCapabilities = sender.getLinkCapabilities(apnType);
        }
        ServiceState ss = sender.getServiceState();
        if (ss != null) roaming = ss.getRoaming();

        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            networkType = ((telephony!=null) ? telephony.getNetworkType() : TelephonyManager.NETWORK_TYPE_UNKNOWN);
        } else {
            networkType = ((telephony!=null) ? telephony.getNetworkTypeGemini(sender.getMySimId()) : TelephonyManager.NETWORK_TYPE_UNKNOWN);
        }
        log("doNotifyDataConnection " + "apnType=" + apnType + ",networkType=" + networkType + ", state=" + state);
        notifyDataStateChangeCallback(
                state.toString(),
                reason,
                sender.getActiveApnHost(apnType),
                apnType,
                sender.isDataConnectivityPossible(apnType),
                mSimId);
        try {
            mRegistry.notifyDataConnection(
                    convertDataState(state),
                    sender.isDataConnectivityPossible(apnType),
                    reason,
                    sender.getActiveApnHost(apnType),
                    apnType,
                    linkProperties,
                    linkCapabilities,
                    networkType,
                    roaming);
        } catch (RemoteException ex) {
            // system process is dead
        }

        // M: Update SIM indication state if data connection state change
        if (mDataState != sender.getDataConnectionState()) {
            mDataState = sender.getDataConnectionState();
            sender.updateSimIndicateState();
        }
    }

    public void notifyDataConnectionFailed(Phone sender, String reason, String apnType) {
        try {
            mRegistry.notifyDataConnectionFailed(reason, apnType);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCellLocation(Phone sender) {
        Bundle data = new Bundle();
        sender.getCellLocation().fillInNotifierBundle(data);
        try {
            mRegistry.notifyCellLocation(data);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCellInfo(Phone sender, List<CellInfo> cellInfo) {
        try {
            mRegistry.notifyCellInfo(cellInfo);
        } catch (RemoteException ex) {

        }
    }

    public void notifyOtaspChanged(Phone sender, int otaspMode) {
        try {
            mRegistry.notifyOtaspChanged(otaspMode);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    private void log(String s) {
        Log.d(LOG_TAG, "[PhoneNotifier] " + s);
    }

    /**
     * Convert the {@link State} enum into the TelephonyManager.CALL_STATE_* constants
     * for the public API.
     */
    public static int convertCallState(PhoneConstants.State state) {
        switch (state) {
            case RINGING:
                return TelephonyManager.CALL_STATE_RINGING;
            case OFFHOOK:
                return TelephonyManager.CALL_STATE_OFFHOOK;
            default:
                return TelephonyManager.CALL_STATE_IDLE;
        }
    }

    /**
     * Convert the TelephonyManager.CALL_STATE_* constants into the {@link State} enum
     * for the public API.
     */
    public static PhoneConstants.State convertCallState(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                return PhoneConstants.State.RINGING;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return PhoneConstants.State.OFFHOOK;
            default:
                return PhoneConstants.State.IDLE;
        }
    }

    /**
     * Convert the {@link DataState} enum into the TelephonyManager.DATA_* constants
     * for the public API.
     */
    public static int convertDataState(PhoneConstants.DataState state) {
        switch (state) {
            case CONNECTING:
                return TelephonyManager.DATA_CONNECTING;
            case CONNECTED:
                return TelephonyManager.DATA_CONNECTED;
            case SUSPENDED:
                return TelephonyManager.DATA_SUSPENDED;
            default:
                return TelephonyManager.DATA_DISCONNECTED;
        }
    }

    /**
     * Convert the TelephonyManager.DATA_* constants into {@link DataState} enum
     * for the public API.
     */
    public static PhoneConstants.DataState convertDataState(int state) {
        switch (state) {
            case TelephonyManager.DATA_CONNECTING:
                return PhoneConstants.DataState.CONNECTING;
            case TelephonyManager.DATA_CONNECTED:
                return PhoneConstants.DataState.CONNECTED;
            case TelephonyManager.DATA_SUSPENDED:
                return PhoneConstants.DataState.SUSPENDED;
            default:
                return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    /**
     * Convert the {@link DataState} enum into the TelephonyManager.DATA_* constants
     * for the public API.
     */
    public static int convertDataActivityState(Phone.DataActivityState state) {
        switch (state) {
            case DATAIN:
                return TelephonyManager.DATA_ACTIVITY_IN;
            case DATAOUT:
                return TelephonyManager.DATA_ACTIVITY_OUT;
            case DATAINANDOUT:
                return TelephonyManager.DATA_ACTIVITY_INOUT;
            case DORMANT:
                return TelephonyManager.DATA_ACTIVITY_DORMANT;
            default:
                return TelephonyManager.DATA_ACTIVITY_NONE;
        }
    }

    /**
     * Convert the TelephonyManager.DATA_* constants into the {@link DataState} enum
     * for the public API.
     */
    public static Phone.DataActivityState convertDataActivityState(int state) {
        switch (state) {
            case TelephonyManager.DATA_ACTIVITY_IN:
                return Phone.DataActivityState.DATAIN;
            case TelephonyManager.DATA_ACTIVITY_OUT:
                return Phone.DataActivityState.DATAOUT;
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                return Phone.DataActivityState.DATAINANDOUT;
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                return Phone.DataActivityState.DORMANT;
            default:
                return Phone.DataActivityState.NONE;
        }
    }

    //MTK-START [mtk04070][111125][ALPS00093395]MTK proprietary methods
    public DefaultPhoneNotifier(int simId) {
        mSimId = simId;
		
        if (simId == PhoneConstants.GEMINI_SIM_1) {
            mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                        "telephony.registry"));
        } else if(simId == PhoneConstants.GEMINI_SIM_2){
            mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                        "telephony.registry2"));
        } else if(simId == PhoneConstants.GEMINI_SIM_3){
            mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                        "telephony.registry3"));
        } else if(simId == PhoneConstants.GEMINI_SIM_4){
            mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                        "telephony.registry4"));
        }
    }

    public void notifyDataConnection(Phone sender, String reason, String apnType,
            boolean disconnectPdpFlag) {
        // TODO Auto-generated method stub
        TelephonyManager telephony = TelephonyManager.getDefault();
        try {
            /*
            mRegistry.notifyDataConnection(
                    convertDataState(sender.getDataConnectionState()),
                    sender.isDataConnectivityPossible(), reason,
                    sender.getActiveApn(),
                    sender.getActiveApnTypes(),
                    sender.getInterfaceName(null),
                    ((telephony!=null) ? telephony.getNetworkType() :
                    TelephonyManager.NETWORK_TYPE_UNKNOWN),
                    sender.getGateway(null));
            */
            /* getDataConnectionState(apnType) is newly added by vendor */
            /* If apnType is null: getDataConnectionState(null) will invoke DataConnectionTracker's getState() to obtain overall data state */
            Log.e(TAG, "apnType is " + apnType);
            int state = convertDataState(sender.getDataConnectionState(apnType));
            boolean isEnable = sender.isDataConnectivityPossible(apnType);
            String activeApnString = sender.getApnForType(apnType);
            //String[] activeApnTypes = sender.getActiveApnTypes();
            String[] activeApnTypes = apnType == null ? new String[]{} : new String[]{apnType};
            String interfaceName = sender.getInterfaceName(apnType);
            int networkType;

            if (FeatureOption.MTK_GEMINI_SUPPORT != true) {
                networkType = ((telephony!=null) ? telephony.getNetworkType() : TelephonyManager.NETWORK_TYPE_UNKNOWN);
            } else {
                networkType = ((telephony!=null) ? telephony.getNetworkTypeGemini(sender.getMySimId()) : TelephonyManager.NETWORK_TYPE_UNKNOWN);
            }

            /* Add by MTK03594 */
            /*Get gateway according to APN type*/
            String gateway = sender.getGateway(apnType);
            
            Log.e(TAG, "convertDataState(sender.getDataConnectionState(apnType) is " + state);
            Log.e(TAG, "sender.isDataConnectivityPossible(apnType) is " + isEnable);
            Log.e(TAG, "reason is " + reason);
            Log.e(TAG, "sender.getActiveApn() is " + activeApnString);
            Log.e(TAG, "sender.getActiveApnTypes() is " + (apnType == null ? null: activeApnTypes[0]));
            Log.e(TAG, "sender.getInterfaceName(apnType) is " + interfaceName);
            Log.e(TAG, "((telephony!=null) ? telephony.getNetworkType() : TelephonyManager.NETWORK_TYPE_UNKNOWN) is " + networkType);
            mRegistry.notifyAllDataConnection(state, isEnable, reason, activeApnString, activeApnTypes, interfaceName, networkType, disconnectPdpFlag, gateway);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private int networkStringToType(String networkType) {
        int ret = TelephonyManager.NETWORK_TYPE_UNKNOWN;

        if (networkType.equals("GPRS")) {
                ret = TelephonyManager.NETWORK_TYPE_GPRS;
        } else if (networkType.equals("EDGE")) {
                ret = TelephonyManager.NETWORK_TYPE_EDGE;
        } else if (networkType.equals("UMTS")) {
                ret = TelephonyManager.NETWORK_TYPE_UMTS;
        } else if (networkType.equals("HSDPA")) {
                ret = TelephonyManager.NETWORK_TYPE_HSDPA;
        } else if (networkType.equals("HSUPA")) {
                ret = TelephonyManager.NETWORK_TYPE_HSUPA;
        } else if (networkType.equals("HSPA")) {
                ret =TelephonyManager.NETWORK_TYPE_HSPA;
        }

        if (DBG) log("networkStringToType(): networkType=" + networkType + " ret=" + ret);
        return ret;
    }    

    public interface IDataStateChangedCallback {
        void onDataStateChanged(String state, String reason, String apnName,
            String apnType, boolean unavailable, int simId);
    }

    private ArrayList<IDataStateChangedCallback> mDataStateChangedCallbacks = new ArrayList<IDataStateChangedCallback>();

    private void notifyDataStateChangeCallback(String state, String reason, String apnName,
            String apnType, boolean unavailable, int simId)
    {
        Iterator<IDataStateChangedCallback> iterator = mDataStateChangedCallbacks.iterator();
        while (iterator.hasNext()) {
            IDataStateChangedCallback callback = iterator.next();
            callback.onDataStateChanged(state, reason, apnName, apnType, unavailable, simId);
        }
    }

    public void registerDataStateChangeCallback(IDataStateChangedCallback callback) {
        mDataStateChangedCallbacks.add(callback);
    }

    public void unregisterDataStateChangeCallback(IDataStateChangedCallback callback) {
        mDataStateChangedCallbacks.remove(callback);
    }
    //MTK-END [mtk04070][111125][ALPS00093395]MTK proprietary methods
}
