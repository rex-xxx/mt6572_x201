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

package com.android.internal.telephony.gsm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.net.LinkProperties.CompareResult;
import android.net.wifi.WifiManager;
import android.net.NetworkConfig;
import android.net.NetworkUtils;
import android.net.ProxyProperties;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import com.android.internal.telephony.ApnContext;
import com.android.internal.telephony.ApnSetting;
import com.android.internal.telephony.DataCallState;
import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.DataConnection.FailCause;
import com.android.internal.telephony.DataConnection.UpdateLinkPropertyResult;
import com.android.internal.telephony.DataConnectionAc;
import com.android.internal.telephony.DataConnectionTracker;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccRecords;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.AsyncChannel;

//[New R8 modem FD]
import com.android.internal.telephony.gsm.FDModeType;
import com.android.internal.telephony.gsm.FDTimer;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//MTK-START [mtk04070][111205][ALPS00093395]MTK added
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiNetworkSubUtil;
import com.android.internal.telephony.gemini.GeminiPhone;
import android.content.BroadcastReceiver;
import com.mediatek.common.featureoption.FeatureOption;
import static android.provider.Settings.System.GPRS_CONNECTION_SETTING;
import static android.provider.Settings.System.GPRS_CONNECTION_SETTING_DEFAULT;
import android.provider.Telephony.SIMInfo;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.ITetheringExt;
import com.mediatek.common.telephony.IApnSetting;
import com.mediatek.common.telephony.IGsmDCTExt;
//MTK-END [mtk04070][111205][ALPS00093395]MTK added


/**
 * {@hide}
 */
public final class GsmDataConnectionTracker extends DataConnectionTracker {
    protected final String LOG_TAG = "GSM";

    private static final int MSG_RESTART_RADIO_OFF_DONE = 999;
    private static final int MSG_RESTART_RADIO_ON_DONE = 998;
    /**
     * Handles changes to the APN db.
     */
    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver () {
            super(mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean selfChange) {
            sendMessage(obtainMessage(DctConstants.EVENT_APN_CHANGED));
        }
    }

    //***** Instance Variables

    private boolean mReregisterOnReconnectFailure = false;


    //***** Constants

    private static final int POLL_PDP_MILLIS = 5 * 1000;

    private static final String INTENT_RECONNECT_ALARM =
        "com.android.internal.telephony.gprs-reconnect";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_TYPE = "reconnect_alarm_extra_type";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_RETRY_COUNT =
        "reconnect_alaram_extra_retry_count";

    private static final String INTENT_DATA_STALL_ALARM =
        "com.android.internal.telephony.gprs-data-stall";

    static final Uri PREFERAPN_NO_UPDATE_URI =
                        Uri.parse("content://telephony/carriers/preferapn_no_update");
    static final String APN_ID = "apn_id";
    private boolean canSetPreferApn = false;

    //MTK-BEGIN
    private ApnSetting mPreferredTetheringApn = null;
    private boolean canSetPreferTetheringApn = false;
    private ArrayList<String> mWaitingApnList = new ArrayList<String>();
    private int mLogSimId = 0;
    static final Uri CONTENT_URI_GEMINI[]
            = {Telephony.Carriers.SIM1Carriers.CONTENT_URI,
            Telephony.Carriers.SIM2Carriers.CONTENT_URI,
            Telephony.Carriers.SIM3Carriers.CONTENT_URI,
            Telephony.Carriers.SIM4Carriers.CONTENT_URI};
    static final Uri PREFERAPN_NO_UPDATE_URI_GEMINI[] 
            = {Uri.parse("content://telephony/carriers_sim1/preferapn_no_update"),
            Uri.parse("content://telephony/carriers_sim2/preferapn_no_update"),
            Uri.parse("content://telephony/carriers_sim3/preferapn_no_update"),
            Uri.parse("content://telephony/carriers_sim4/preferapn_no_update")};
    //MTK-END

    private static final boolean DATA_STALL_SUSPECTED = true;
    private static final boolean DATA_STALL_NOT_SUSPECTED = false;

    public static final String PROPERTY_RIL_TEST_SIM[] = {
        "gsm.sim.ril.testsim",
        "gsm.sim.ril.testsim.2",
        "gsm.sim.ril.testsim.3",
        "gsm.sim.ril.testsim.4",
    };

    @Override
    protected void onActionIntentReconnectAlarm(Intent intent) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int reconnect_for_simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
            logd("GPRS reconnect alarm triggered by simId=" + reconnect_for_simId + ". Previous state was " + getState());  
            if (reconnect_for_simId != mGsmPhone.getMySimId()) {
                return;
            }
        } else {
            logd("GPRS reconnect alarm. Previous state was " + getState());
        }
        String reason = intent.getStringExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON);
        int connectionId = intent.getIntExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE, -1);
        int retryCount = intent.getIntExtra(INTENT_RECONNECT_ALARM_EXTRA_RETRY_COUNT, 0);

        DataConnectionAc dcac= mDataConnectionAsyncChannels.get(connectionId);

        if (DBG) {
            log("onActionIntentReconnectAlarm: mState=" + mState + " reason=" + reason +
                    " connectionId=" + connectionId + " retryCount=" + retryCount + " dcac=" + dcac
                    + " mDataConnectionAsyncChannels=" + mDataConnectionAsyncChannels);
        }

        if (dcac != null) {
            for (ApnContext apnContext : dcac.getApnListSync()) {
                apnContext.setReason(reason);
                apnContext.setRetryCount(retryCount);
                DctConstants.State apnContextState = apnContext.getState();
                if (DBG) {
                    log("onActionIntentReconnectAlarm: apnContext state=" + apnContextState);
                }
                if ((apnContextState == DctConstants.State.FAILED)
                        || (apnContextState == DctConstants.State.IDLE)) {
                    if (DBG) {
                        log("onActionIntentReconnectAlarm: state is FAILED|IDLE, disassociate");
                    }
                    apnContext.setDataConnectionAc(null);
                    apnContext.setDataConnection(null);
                    apnContext.setState(DctConstants.State.IDLE);
                } else {
                    if (DBG) {
                        log("onActionIntentReconnectAlarm: keep associated");
                    }
                }
                sendMessage(obtainMessage(DctConstants.EVENT_TRY_SETUP_DATA, apnContext));
            }
            // Alram had expired. Clear pending intent recorded on the DataConnection.
            dcac.setReconnectIntentSync(null);
        }
    }

    /** Watches for changes to the APN db. */
    private ApnChangeObserver mApnObserver;

    //MTK-START [mtk04070][111205][ALPS00093395]MTK added
    private GSMPhone mGsmPhone;
    /// M: SCRI and Fast Dormancy Feature @}
    //Add for SCRI, Fast Dormancy
    private ScriManager mScriManager;
    protected boolean scriPollEnabled = false;
    protected long scriTxPkts=0, scriRxPkts=0;
    //[New R8 modem FD]
    protected FDTimer mFDTimer;

    private boolean mIsUmtsMode = false;
    private boolean mIsCallPrefer = false;

    //[ALPS00098656][mtk04070]Disable Fast Dormancy when in Tethered mode
    private boolean mIsTetheredMode = false;
    /// @}
    //MTK-END [mtk04070][111205][ALPS00093395]MTK added

    private static final int PDP_CONNECTION_POOL_SIZE = 3;

    private ITetheringExt mTetheringExt;
    private IGsmDCTExt mGsmDCTExt;

    //***** Constructor

    public GsmDataConnectionTracker(PhoneBase p) {
        super(p);
        if (DBG) log("GsmDCT.constructor");
        //MTK-START [mtk04070][111205][ALPS00093395]MTK added
        mGsmPhone = (GSMPhone)p;
        mLogSimId = mGsmPhone.getMySimId() + 1;
        //MTK-END [mtk04070][111205][ALPS00093395]MTK added

        p.mCM.registerForAvailable (this, DctConstants.EVENT_RADIO_AVAILABLE, null);
        p.mCM.registerForOffOrNotAvailable(this, DctConstants.EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        p.mCM.registerForDataNetworkStateChanged (this, DctConstants.EVENT_DATA_STATE_CHANGED, null);
        p.getCallTracker().registerForVoiceCallEnded (this, DctConstants.EVENT_VOICE_CALL_ENDED, null);
        p.getCallTracker().registerForVoiceCallStarted (this, DctConstants.EVENT_VOICE_CALL_STARTED, null);
        p.getServiceStateTracker().registerForDataConnectionAttached(this,
                DctConstants.EVENT_DATA_CONNECTION_ATTACHED, null);
        p.getServiceStateTracker().registerForDataConnectionDetached(this,
                DctConstants.EVENT_DATA_CONNECTION_DETACHED, null);
        p.getServiceStateTracker().registerForRoamingOn(this, DctConstants.EVENT_ROAMING_ON, null);
        p.getServiceStateTracker().registerForRoamingOff(this, DctConstants.EVENT_ROAMING_OFF, null);
        p.getServiceStateTracker().registerForPsRestrictedEnabled(this,
                DctConstants.EVENT_PS_RESTRICT_ENABLED, null);
        p.getServiceStateTracker().registerForPsRestrictedDisabled(this,
                DctConstants.EVENT_PS_RESTRICT_DISABLED, null);
        mGsmPhone.mSST.registerForPsRegistrants(this, 
                DctConstants.EVENT_PS_RAT_CHANGED, null);
        p.mCM.registerForGetAvailableNetworksDone(this, DctConstants.EVENT_GET_AVAILABLE_NETWORK_DONE, null);

        //[New R8 modem FD]
        mFDTimer = new FDTimer(p);
        

        mDataConnectionTracker = this;

        mApnObserver = new ApnChangeObserver();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Uri geminiUri = CONTENT_URI_GEMINI[mPhone.getMySimId()];
            p.getContext().getContentResolver().registerContentObserver(geminiUri, true, mApnObserver);
        } else {
            p.getContext().getContentResolver().registerContentObserver(
                Telephony.Carriers.CONTENT_URI, true, mApnObserver);
        }

        // MTK START
        try{
            mTetheringExt = MediatekClassFactory.createInstance(ITetheringExt.class, p.getContext(), mPhone.getMySimId());
            mGsmDCTExt = MediatekClassFactory.createInstance(IGsmDCTExt.class, p.getContext());
        } catch (Exception e){
            e.printStackTrace();
        }
        // MTK END

        initApnContextsAndDataConnection();
        broadcastMessenger();


        /// M: SCRI and Fast Dormancy Feature @{
        //MTK-START [mtk04070][111205][ALPS00093395]MTK added
        //Register for handling SCRI events
        registerSCRIEvent(mGsmPhone);
        //MTK-END [mtk04070][111205][ALPS00093395]MTK added
        /// @}
    }

    @Override
    public void dispose() {
        if (DBG) log("GsmDCT.dispose");
        cleanUpAllConnections(true, null);

        super.dispose();

        //Unregister for all events
        mPhone.mCM.unregisterForAvailable(this);
        mPhone.mCM.unregisterForOffOrNotAvailable(this);
        IccRecords r = mIccRecords.get();
        if (r != null) { r.unregisterForRecordsLoaded(this);}
        mPhone.mCM.unregisterForDataNetworkStateChanged(this);
        mPhone.getCallTracker().unregisterForVoiceCallEnded(this);
        mPhone.getCallTracker().unregisterForVoiceCallStarted(this);
        mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(this);
        mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(this);
        mPhone.getServiceStateTracker().unregisterForRoamingOn(this);
        mPhone.getServiceStateTracker().unregisterForRoamingOff(this);
        mPhone.getServiceStateTracker().unregisterForPsRestrictedEnabled(this);
        mPhone.getServiceStateTracker().unregisterForPsRestrictedDisabled(this);
        mGsmPhone.mSST.unregisterForPsRegistrants(this);

        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            if (i != mGsmPhone.getMySimId() && mGsmPhone.getPeerPhones(i) != null) {
                mGsmPhone.getPeerPhones(i).getCallTracker().unregisterForVoiceCallEnded(this);
                mGsmPhone.getPeerPhones(i).getCallTracker().unregisterForVoiceCallStarted(this);
            }
        }

        mPhone.getContext().getContentResolver().unregisterContentObserver(this.mApnObserver);
        mApnContexts.clear();
        //MTK
        mPhone.mCM.unSetGprsDetach(this);
        mPhone.mCM.unregisterForGetAvailableNetworksDone(this);

        destroyDataConnections();
    }

    @Override
    public boolean isApnTypeActive(String type) {
        ApnContext apnContext = mApnContexts.get(type);
        if (apnContext == null) return false;

        return (apnContext.getDataConnection() != null);
    }

    @Override
    protected boolean isDataPossible(String apnType) {
        log("apnType = " + apnType);
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext == null) {
            return false;
        }
        boolean apnContextIsEnabled = apnContext.isEnabled();
        DctConstants.State apnContextState = apnContext.getState();
        boolean apnTypePossible = !(apnContextIsEnabled &&
                (apnContextState == DctConstants.State.FAILED));
        boolean dataAllowed = isDataAllowed();
        boolean possible = dataAllowed && apnTypePossible;

        if (DBG) {
            log(String.format("isDataPossible(%s): possible=%b isDataAllowed=%b " +
                    "apnTypePossible=%b apnContextisEnabled=%b apnContextState()=%s",
                    apnType, possible, dataAllowed, apnTypePossible,
                    apnContextIsEnabled, apnContextState));
        }
        return possible;
    }

    @Override
    protected void finalize() {
        if(DBG) log("finalize");
    }

    @Override
    protected String getActionIntentReconnectAlarm() {
        return INTENT_RECONNECT_ALARM;
    }

    @Override
    protected String getActionIntentDataStallAlarm() {
        return INTENT_DATA_STALL_ALARM;
    }

    private ApnContext addApnContext(String type) {
        ApnContext apnContext = new ApnContext(type, LOG_TAG);
        apnContext.setDependencyMet(false);
        mApnContexts.put(type, apnContext);
        return apnContext;
    }

    protected void initApnContextsAndDataConnection() {
        boolean defaultEnabled = SystemProperties.getBoolean(DEFALUT_DATA_ON_BOOT_PROP, true);
        // Load device network attributes from resources
        String[] networkConfigStrings = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.networkAttributes);
        for (String networkConfigString : networkConfigStrings) {
            NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
            ApnContext apnContext = null;

            switch (networkConfig.type) {
            case ConnectivityManager.TYPE_MOBILE:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_DEFAULT);
                apnContext.setEnabled(defaultEnabled);
                break;
            case ConnectivityManager.TYPE_MOBILE_MMS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_MMS);
                break;
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_SUPL);
                break;
            case ConnectivityManager.TYPE_MOBILE_DUN:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_DUN);
                break;
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_HIPRI);
                ApnContext defaultContext = mApnContexts.get(PhoneConstants.APN_TYPE_DEFAULT);
                if (defaultContext != null) {
                    applyNewState(apnContext, apnContext.isEnabled(),
                            defaultContext.getDependencyMet());
                } else {
                    // the default will set the hipri dep-met when it is created
                }
                continue;
            case ConnectivityManager.TYPE_MOBILE_FOTA:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_FOTA);
                break;
            case ConnectivityManager.TYPE_MOBILE_IMS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_IMS);
                break;
            case ConnectivityManager.TYPE_MOBILE_CBS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_CBS);
                break;
            case ConnectivityManager.TYPE_MOBILE_DM:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_DM);
                break;
            case ConnectivityManager.TYPE_MOBILE_NET:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_NET);
                break;
            case ConnectivityManager.TYPE_MOBILE_WAP:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_WAP);
                break;
            case ConnectivityManager.TYPE_MOBILE_CMMAIL:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_CMMAIL);
                break;
            case ConnectivityManager.TYPE_MOBILE_RCSE:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_RCSE);
                break;
            default:
                // skip unknown types
                continue;
            }
            if (apnContext != null) {
                // set the prop, but also apply the newly set enabled and dependency values
                onSetDependencyMet(apnContext.getApnType(), networkConfig.dependencyMet);
            }
        }
    }

    @Override
    protected LinkProperties getLinkProperties(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            DataConnectionAc dcac = apnContext.getDataConnectionAc();
            if (dcac != null) {
                if (DBG) log("return link properites for " + apnType);
                return dcac.getLinkPropertiesSync();
            }
        }
        if (DBG) log("return new LinkProperties");
        return new LinkProperties();
    }

    @Override
    protected LinkCapabilities getLinkCapabilities(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext!=null) {
            DataConnectionAc dataConnectionAc = apnContext.getDataConnectionAc();
            if (dataConnectionAc != null) {
                if (DBG) log("get active pdp is not null, return link Capabilities for " + apnType);
                return dataConnectionAc.getLinkCapabilitiesSync();
            }
        }
        if (DBG) log("return new LinkCapabilities");
        return new LinkCapabilities();
    }

    @Override
    // Return all active apn types
    public String[] getActiveApnTypes() {
        if (DBG) log("get all active apn types");
        ArrayList<String> result = new ArrayList<String>();

        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.isReady()) {
                result.add(apnContext.getApnType());
            }
        }

        return (String[])result.toArray(new String[0]);
    }

    @Override
    // Return active apn of specific apn type
    public String getActiveApnString(String apnType) {
        if (DBG) log( "get active apn string for type:" + apnType);
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            ApnSetting apnSetting = apnContext.getApnSetting();
            if (apnSetting != null) {
                return apnSetting.apn;
            }
        }
        return null;
    }

    @Override
    public boolean isApnTypeEnabled(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext == null) {
            return false;
        }
        return apnContext.isEnabled();
    }

    @Override
    protected void setState(DctConstants.State s) {
        if (DBG) log("setState should not be used in GSM" + s);
    }

    // Return state of specific apn type
    @Override
    public DctConstants.State getState(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.getState();
        }
        return DctConstants.State.FAILED;
    }

    // Return state of overall
    public DctConstants.State getOverallState() {
        boolean isConnecting = false;
        boolean isFailed = true; // All enabled Apns should be FAILED.
        boolean isAnyEnabled = false;
        StringBuilder builder = new StringBuilder();
        for (ApnContext apnContext : mApnContexts.values()) {
        	if (apnContext != null) {
        		builder.append(apnContext.toString() + ", ");
        	}
        }
        if (DBG) log( "overall state is " + builder);
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.isEnabled()) {
                isAnyEnabled = true;
                switch (apnContext.getState()) {
                case CONNECTED:
                case DISCONNECTING:
                    if (DBG) log("overall state is CONNECTED");
                    return DctConstants.State.CONNECTED;
                case CONNECTING:
                case INITING:
                    isConnecting = true;
                    isFailed = false;
                    break;
                case IDLE:
                case SCANNING:
                    isFailed = false;
                    break;
                }
            }
        }

        if (!isAnyEnabled) { // Nothing enabled. return IDLE.
            if (DBG) log( "overall state is IDLE");
            return DctConstants.State.IDLE;
        }

        if (isConnecting) {
            if (DBG) log( "overall state is CONNECTING");
            return DctConstants.State.CONNECTING;
        } else if (!isFailed) {
            if (DBG) log( "overall state is IDLE");
            return DctConstants.State.IDLE;
        } else {
            if (DBG) log( "overall state is FAILED");
            return DctConstants.State.FAILED;
        }
    }

    /**
     * Ensure that we are connected to an APN of the specified type.
     *
     * @param type the APN type
     * @return Success is indicated by {@code PhoneConstants.APN_ALREADY_ACTIVE} or
     *         {@code PhoneConstants.APN_REQUEST_STARTED}. In the latter case, a
     *         broadcast will be sent by the ConnectivityManager when a
     *         connection to the APN has been established.
     */
    @Override
    public synchronized int enableApnType(String apnType) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int simId = mPhone.getMySimId();
            ConnectivityManager connectivityManager = (ConnectivityManager)mPhone.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null && connectivityManager.getMobileDataEnabledGemini(simId)) {
                if (SystemProperties.getInt(PROPERTY_RIL_TEST_SIM[simId], 0) == 1) {
                    log("enableApnType SIM" + (simId+1) + " is a test SIM and data is enabled on it, do PS attach");
                    mPhone.mCM.setGprsConnType(GeminiNetworkSubUtil.CONN_TYPE_ALWAYS, null);
                } else {
                    log("enableApnType SIM" + (simId+1) + " is not a test SIM");
                }
            }
        }

        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext == null || !isApnTypeAvailable(apnType)) {
            if (DBG) log("enableApnType: " + apnType + " is type not available");
            return PhoneConstants.APN_TYPE_NOT_AVAILABLE;
        }

        // If already active, return
        if (DBG) log("enableApnType: " + apnType + " mState(" + apnContext.getState() + ")");

        if (apnContext.getState() == DctConstants.State.CONNECTED) {
            if (DBG) log("enableApnType: return APN_ALREADY_ACTIVE");
            return PhoneConstants.APN_ALREADY_ACTIVE;
        }
        setEnabled(apnTypeToId(apnType), true);
        if (DBG) {
            log("enableApnType: new apn request for type " + apnType +
                    " return APN_REQUEST_STARTED");
        }
        return PhoneConstants.APN_REQUEST_STARTED;
    }

    // A new APN has gone active and needs to send events to catch up with the
    // current condition
    private void notifyApnIdUpToCurrent(String reason, ApnContext apnContext, String type) {
        switch (apnContext.getState()) {
            case IDLE:
            case INITING:
                break;
            case CONNECTING:
            case SCANNING:
                mPhone.notifyDataConnection(reason, type, PhoneConstants.DataState.CONNECTING);
                break;
            case CONNECTED:
            case DISCONNECTING:
                mPhone.notifyDataConnection(reason, type, PhoneConstants.DataState.CONNECTING);
                mPhone.notifyDataConnection(reason, type, PhoneConstants.DataState.CONNECTED);
                break;
        }
    }

    @Override
    public synchronized int disableApnType(String type) {
        if (DBG) log("disableApnType:" + type);
        ApnContext apnContext = mApnContexts.get(type);

        if (apnContext != null) {
            setEnabled(apnTypeToId(type), false);
            if (apnContext.getState() != DctConstants.State.IDLE && apnContext.getState()
                    != DctConstants.State.FAILED) {
                if (DBG) log("diableApnType: return APN_REQUEST_STARTED");
                return PhoneConstants.APN_REQUEST_STARTED;
            } else {
                if (DBG) log("disableApnType: return APN_ALREADY_INACTIVE");
                return PhoneConstants.APN_ALREADY_INACTIVE;
            }

        } else {
            if (DBG) {
                log("disableApnType: no apn context was found, return APN_REQUEST_FAILED");
            }
            return PhoneConstants.APN_REQUEST_FAILED;
        }
    }

    @Override
    protected boolean isApnTypeAvailable(String type) {
        if (type.equals(PhoneConstants.APN_TYPE_DUN) && fetchDunApn() != null) {
            return true;
        }

        if (mAllApns != null) {
            for (ApnSetting apn : mAllApns) {
                if (apn.canHandleType(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Report on whether data connectivity is enabled for any APN.
     * @return {@code false} if data connectivity has been explicitly disabled,
     * {@code true} otherwise.
     */
    @Override
    public boolean getAnyDataEnabled() {
        synchronized (mDataEnabledLock) {
            mUserDataEnabled = Settings.Global.getInt(
                mPhone.getContext().getContentResolver(), Settings.Global.MOBILE_DATA, 1) == 1;

            if (!(mInternalDataEnabled && mUserDataEnabled && sPolicyDataEnabled)) return false;
            for (ApnContext apnContext : mApnContexts.values()) {
                // Make sure we dont have a context that going down
                // and is explicitly disabled.
                if (isDataAllowed(apnContext)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isDataAllowed(ApnContext apnContext) {
        return apnContext.isReady() && isDataAllowed();
    }

    //****** Called from ServiceStateTracker
    /**
     * Invoked when ServiceStateTracker observes a transition from GPRS
     * attach to detach.
     */
    protected void onDataConnectionDetached() {
        /*
         * We presently believe it is unnecessary to tear down the PDP context
         * when GPRS detaches, but we should stop the network polling.
         */
        if (DBG) log ("onDataConnectionDetached: stop polling and notify detached");
        stopNetStatPoll();
        stopDataStallAlarm();

        notifyDataConnection(Phone.REASON_DATA_DETACHED);

        /// M: SCRI and Fast Dormancy Feature @{ 
        //MTK-START [mtk04070][111205][ALPS00093395]Stop SCRI polling
        /* Add by MTK03594 */
        if (DBG) log ("onDataConnectionDetached: stopScriPoll()");
        stopScriPoll();
        //MTK-END [mtk04070][111205][ALPS00093395]Stop SCRI polling
        /// @}
    }

    private void onDataConnectionAttached() {
        if (DBG) log("onDataConnectionAttached");
        if (getOverallState() == DctConstants.State.CONNECTED) {
            if (DBG) log("onDataConnectionAttached: start polling notify attached");
            startNetStatPoll();
            startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
            notifyDataConnection(Phone.REASON_DATA_ATTACHED);
        } else {
            // update APN availability so that APN can be enabled.
            notifyOffApnsOfAvailability(Phone.REASON_DATA_ATTACHED);
        }
        mAutoAttachOnCreation = true;
        setupDataOnReadyApns(Phone.REASON_DATA_ATTACHED);
    }

    @Override
    protected boolean isDataAllowed() {
        final boolean internalDataEnabled;
        synchronized (mDataEnabledLock) {
            internalDataEnabled = mInternalDataEnabled;
        }

        int gprsState = mPhone.getServiceStateTracker().getCurrentDataConnectionState();
        boolean desiredPowerState = mPhone.getServiceStateTracker().getDesiredPowerState();
        IccRecords r = mIccRecords.get();
        boolean recordsLoaded = (r != null) ? r.getRecordsLoaded() : false;
        boolean isPeerPhoneIdle = true;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            for (int i=PhoneConstants.GEMINI_SIM_1; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
                if (i != mGsmPhone.getMySimId() && mGsmPhone.getPeerPhones(i) != null && 
                        mGsmPhone.getPeerPhones(i).getState() != PhoneConstants.State.IDLE) {
                    isPeerPhoneIdle = false;
                    break;
                }
            }
        }

        boolean allowed =
                    (gprsState == ServiceState.STATE_IN_SERVICE || mAutoAttachOnCreation) &&
                    recordsLoaded &&
                    (mPhone.getState() == PhoneConstants.State.IDLE ||
                     mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) &&
                    internalDataEnabled &&
                    (!mPhone.getServiceState().getRoaming() || getDataOnRoamingEnabled()) &&
                    // TODO: confirm if need to modify it in Gemini+
                    // !mIsPsRestricted &&
                    desiredPowerState &&
                    !mPhone.mCM.isGettingAvailableNetworks() &&
                    isPeerPhoneIdle;
        if (!allowed && DBG) {
            String reason = "";
            if (!((gprsState == ServiceState.STATE_IN_SERVICE) || mAutoAttachOnCreation)) {
                reason += " - gprs= " + gprsState;
            }
            if (!recordsLoaded) reason += " - SIM not loaded";
            if (mPhone.getState() != PhoneConstants.State.IDLE &&
                    !mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                reason += " - PhoneState= " + mPhone.getState();
                reason += " - Concurrent voice and data not allowed";
            }
            if (!internalDataEnabled) reason += " - mInternalDataEnabled= false";
            if (mPhone.getServiceState().getRoaming() && !getDataOnRoamingEnabled()) {
                reason += " - Roaming and data roaming not enabled";
            }
            if (mIsPsRestricted) reason += " - mIsPsRestricted= true";
            if (!desiredPowerState) reason += " - desiredPowerState= false";
            if (mPhone.mCM.isGettingAvailableNetworks()) reason += " - querying available network";
            if (!isPeerPhoneIdle) reason += " - Peer phone is not IDLE";
            if (DBG) log("isDataAllowed: not allowed due to" + reason);
        }
        return allowed;
    }

    private void setupDataOnReadyApns(String reason) {
        // Stop reconnect alarms on all data connections pending
        // retry. Reset ApnContext state to IDLE.
        for (DataConnectionAc dcac : mDataConnectionAsyncChannels.values()) {
            if (dcac.getReconnectIntentSync() != null) {
                cancelReconnectAlarm(dcac);
            }
            // update retry config for existing calls to match up
            // ones for the new RAT.
            if (dcac.dataConnection != null) {
                Collection<ApnContext> apns = dcac.getApnListSync();

                boolean hasDefault = false;
                for (ApnContext apnContext : apns) {
                    if (apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT)) {
                        hasDefault = true;
                        break;
                    }
                }
                configureRetry(dcac.dataConnection, hasDefault, 0);
            }
        }

        // Be sure retry counts for Apncontexts and DC's are sync'd.
        // When DCT/ApnContexts are refactored and we cleanup retrying
        // this won't be needed.
        resetAllRetryCounts();

        // Only check for default APN state
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.getState() == DctConstants.State.FAILED) {
                // By this time, alarms for all failed Apns
                // should be stopped if any.
                // Make sure to set the state back to IDLE
                // so that setup data can happen.
                apnContext.setState(DctConstants.State.IDLE);
            }
            if (apnContext.isReady()) {
                if (apnContext.getState() == DctConstants.State.IDLE ||
                        apnContext.getState() == DctConstants.State.SCANNING) {
                    apnContext.setReason(reason);
                    trySetupData(apnContext);
                }
            }
        }
    }

    private boolean trySetupData(String reason, String type) {
        if (DBG) {
            log("trySetupData: " + type + " due to " + (reason == null ? "(unspecified)" : reason)
                    + " isPsRestricted=" + mIsPsRestricted);
        }

        if (type == null) {
            type = PhoneConstants.APN_TYPE_DEFAULT;
        }

        ApnContext apnContext = mApnContexts.get(type);

        if (apnContext == null ){
            if (DBG) log("trySetupData new apn context for type:" + type);
            apnContext = new ApnContext(type, LOG_TAG);
            mApnContexts.put(type, apnContext);
        }
        apnContext.setReason(reason);

        return trySetupData(apnContext);
    }

    private boolean trySetupData(ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        if (DBG) {
            log("trySetupData for type:" + apnType +
                    " due to " + apnContext.getReason());
            log("trySetupData with mIsPsRestricted=" + mIsPsRestricted);
        }

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            apnContext.setState(DctConstants.State.CONNECTED);
            mPhone.notifyDataConnection(apnContext.getReason(), apnType);

            log("trySetupData: (fix?) We're on the simulator; assuming data is connected");
            return true;
        }
        //MTK begin
        if(FeatureOption.MTK_GEMINI_SUPPORT && PhoneConstants.APN_TYPE_DEFAULT.equals(apnType)){
            int gprsDefaultSIM = getDataConnectionFromSetting();
            GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
			
            logd("gprsDefaultSIM:" + gprsDefaultSIM);
            if(gprsDefaultSIM != mGsmPhone.getMySimId()){
                  logd("The setting is off(1)");
                  return false;
            }else if(gprsDefaultSIM < 0){
               logd("The setting is off(2)");
               return false;
            } else if ( mGeminiPhone != null && mGeminiPhone.isGprsDetachingOrDetached(mGsmPhone.getMySimId()) &&
                    !TextUtils.equals(apnContext.getReason(), Phone.REASON_DATA_ATTACHED)) {
                logd("trySetupData: detaching or detached state.");
                return false;
            }
         }
        //MTK end

        if (apnContext.getState() == DctConstants.State.DISCONNECTING) {
            if (DBG) logd("trySetupData:" + apnContext.getApnType() + " is DISCONNECTING, trun on reactive flag.");
            apnContext.setReactive(true);
        }

        boolean desiredPowerState = mPhone.getServiceStateTracker().getDesiredPowerState();
        boolean anyDataEnabled = (FeatureOption.MTK_BSP_PACKAGE || 
                !isDataAllowedAsOff(apnType))? getAnyDataEnabled() : isNotDefaultTypeDataEnabled();

        if ((apnContext.getState() == DctConstants.State.IDLE || 
                apnContext.getState() == DctConstants.State.SCANNING) &&
                isDataAllowed(apnContext) && anyDataEnabled && !isEmergency()) {

            if (apnContext.getState() == DctConstants.State.IDLE) {
                ArrayList<ApnSetting> waitingApns = buildWaitingApns(apnType);
                if (waitingApns.isEmpty()) {
                    if (DBG) log("trySetupData: No APN found");
                    notifyNoData(GsmDataConnection.FailCause.MISSING_UNKNOWN_APN, apnContext);
                    notifyOffApnsOfAvailability(apnContext.getReason());
                    return false;
                } else {
                    apnContext.setWaitingApns(waitingApns);
                    if (DBG) {
                        log ("trySetupData: Create from mAllApns : " + apnListToString(mAllApns));
                    }
                }
            }

            if (DBG) {
                log ("Setup watingApns : " + apnListToString(apnContext.getWaitingApns()));
            }
            // apnContext.setReason(apnContext.getReason());
            boolean retValue = setupData(apnContext);
            notifyOffApnsOfAvailability(apnContext.getReason());
            return retValue;
        } else {
            // TODO: check the condition.
            if (DBG) log ("try setup data but not executed [" + mInternalDataEnabled + "," + mUserDataEnabled + "," + sPolicyDataEnabled + "]");
            if (!apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT)
                && (apnContext.getState() == DctConstants.State.IDLE
                    || apnContext.getState() == DctConstants.State.SCANNING))
                mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnType);
            notifyOffApnsOfAvailability(apnContext.getReason());
            return false;
        }
    }

    @Override
    // Disabled apn's still need avail/unavail notificiations - send them out
    protected void notifyOffApnsOfAvailability(String reason) {
        for (ApnContext apnContext : mApnContexts.values()) {
            if (!apnContext.isReady()) {
                if (DBG) log("notifyOffApnOfAvailability type:" + apnContext.getApnType());
                mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(),
                                            apnContext.getApnType(),
                                            PhoneConstants.DataState.DISCONNECTED);
            } else {
                if (DBG) {
                    log("notifyOffApnsOfAvailability skipped apn due to isReady==false: " +
                            apnContext.toString());
                }
            }
        }
    }

    /**
     * If tearDown is true, this only tears down a CONNECTED session. Presently,
     * there is no mechanism for abandoning an INITING/CONNECTING session,
     * but would likely involve cancelling pending async requests or
     * setting a flag or new state to ignore them when they came in
     * @param tearDown true if the underlying GsmDataConnection should be
     * disconnected.
     * @param reason reason for the clean up.
     */
    protected void cleanUpAllConnections(boolean tearDown, String reason) {
        if (DBG) log("cleanUpAllConnections: tearDown=" + tearDown + " reason=" + reason);

        for (ApnContext apnContext : mApnContexts.values()) {
            apnContext.setReason(reason);
            cleanUpConnection(tearDown, apnContext);
        }

        stopNetStatPoll();
        stopDataStallAlarm();

        // TODO: Do we need mRequestedApnType?
        mRequestedApnType = PhoneConstants.APN_TYPE_DEFAULT;
    }

    /**
     * Cleanup all connections.
     *
     * TODO: Cleanup only a specified connection passed as a parameter.
     *       Also, make sure when you clean up a conn, if it is last apply
     *       logic as though it is cleanupAllConnections
     *
     * @param tearDown true if the underlying DataConnection should be disconnected.
     * @param reason for the clean up.
     */

    @Override
    protected void onCleanUpAllConnections(String cause) {
        cleanUpAllConnections(true, cause);
    }

    private void cleanUpConnection(boolean tearDown, ApnContext apnContext) {

        if (apnContext == null) {
            if (DBG) log("cleanUpConnection: apn context is null");
            return;
        }

        DataConnectionAc dcac = apnContext.getDataConnectionAc();
        if (DBG) {
            log("cleanUpConnection: E tearDown=" + tearDown + " reason=" + apnContext.getReason() +
                    " apnContext=" + apnContext);
        }
        if (tearDown) {
            if (apnContext.isDisconnected()) {
                // The request is tearDown and but ApnContext is not connected.
                // If apnContext is not enabled anymore, break the linkage to the DCAC/DC.
                apnContext.setState(DctConstants.State.IDLE);
                if (!apnContext.isReady()) {
                    apnContext.setDataConnection(null);
                    apnContext.setDataConnectionAc(null);
                }
                // If original state is FAILED, we should notify data possible again since data is disabled.
                mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            } else {
                // Connection is still there. Try to clean up.
                if (dcac != null) {
                    if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
                        boolean disconnectAll = false;
                        if (PhoneConstants.APN_TYPE_DUN.equals(apnContext.getApnType())) {
                            ApnSetting dunSetting = fetchDunApn();
                            if (dunSetting != null &&
                                    dunSetting.equals(apnContext.getApnSetting())) {
                                if (DBG) log("tearing down dedicated DUN connection");
                                // we need to tear it down - we brought it up just for dun and
                                // other people are camped on it and now dun is done.  We need
                                // to stop using it and let the normal apn list get used to find
                                // connections for the remaining desired connections
                                disconnectAll = true;
                            }
                        }
                        if (DBG) {
                            log("cleanUpConnection: tearing down" + (disconnectAll ? " all" :""));
                        }
                        Message msg = obtainMessage(DctConstants.EVENT_DISCONNECT_DONE, apnContext);
                        if (disconnectAll) {
                            apnContext.getDataConnection().tearDownAll(apnContext.getReason(), msg);
                        } else {
                            apnContext.getDataConnection().tearDown(apnContext.getReason(), msg);
                        }
                        apnContext.setState(DctConstants.State.DISCONNECTING);
                    }
                } else {
                    // apn is connected but no reference to dcac.
                    // Should not be happen, but reset the state in case.
                    apnContext.setState(DctConstants.State.IDLE);
                    mPhone.notifyDataConnection(apnContext.getReason(),
                                                apnContext.getApnType());
                }
            }
        } else {
            // force clean up the data connection.
            if (dcac != null) dcac.resetSync();
            apnContext.setState(DctConstants.State.IDLE);
            mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            apnContext.setDataConnection(null);
            apnContext.setDataConnectionAc(null);
        }

        // make sure reconnection alarm is cleaned up if there is no ApnContext
        // associated to the connection.
        if (dcac != null) {
            Collection<ApnContext> apnList = dcac.getApnListSync();
            if (apnList.isEmpty()) {
                cancelReconnectAlarm(dcac);
            }
        }
        if (DBG) {
            log("cleanUpConnection: X tearDown=" + tearDown + " reason=" + apnContext.getReason() +
                    " apnContext=" + apnContext + " dc=" + apnContext.getDataConnection());
        }
    }

    /**
     * Cancels the alarm associated with DCAC.
     *
     * @param DataConnectionAc on which the alarm should be stopped.
     */
    private void cancelReconnectAlarm(DataConnectionAc dcac) {
        if (dcac == null) return;

        PendingIntent intent = dcac.getReconnectIntentSync();

        if (intent != null) {
                AlarmManager am =
                    (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
                am.cancel(intent);
                dcac.setReconnectIntentSync(null);
        }
    }

    /**
     * @param types comma delimited list of APN types
     * @return array of APN types
     */
    private String[] parseTypes(String types) {
        String[] result;
        // If unset, set to DEFAULT.
        if (types == null || types.equals("")) {
            result = new String[1];
            result[0] = PhoneConstants.APN_TYPE_ALL;
        } else {
            result = types.split(",");
        }
        return result;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        if (FeatureOption.MTK_MVNO_SUPPORT) {
            String spnString = mPhone.getSpNameInEfSpn();
            logd("createApnList spnString=" + spnString);
            if (spnString != null && !spnString.equals("")) {
                ArrayList<ApnSetting> spnResult = createApnListWithSPN(cursor, spnString);
                if (spnResult.size() > 0) {
                    logd("Has spnResult.");
                    return spnResult;
                }
            }
            String imsiString = mPhone.isOperatorMvnoForImsi();
            if (imsiString != null && !imsiString.equals("")) {
                ArrayList<ApnSetting> imsiResult = createApnListWithIMSI(cursor, imsiString);
                if (imsiResult.size() > 0) {
                    logd("Has imsiResult.");
                    return imsiResult;
                }
            }
            String pnnString = mPhone.isOperatorMvnoForEfPnn();
            if (pnnString != null && !pnnString.equals("")) {
                ArrayList<ApnSetting> pnnResult = createApnListWithPNN(cursor, pnnString);
                if (pnnResult.size() > 0) {
                    logd("Has pnnResult.");
                    return pnnResult;
                }
            }
        }

        ArrayList<ApnSetting> result = new ArrayList<ApnSetting>();
        if (cursor.moveToFirst()) {
            do {
                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    String strSPN = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.SPN));
                    if (strSPN != null && !strSPN.equals("")) {
                        continue;
                    }
                    String strIMSI = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.IMSI));
                    if (strIMSI != null && !strIMSI.equals("")) {
                        continue;
                    }
                    String strPNN = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PNN));
                    if (strPNN != null && !strPNN.equals("")) {
                        continue;
                    }
                }
                String[] types = parseTypes(
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                ApnSetting apn = new ApnSetting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                        types,
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.ROAMING_PROTOCOL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.CARRIER_ENABLED)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)));
                result.add(apn);
            } while (cursor.moveToNext());
        }
        if (DBG) log("createApnList: X result=" + result);
        return result;
    }

    private ArrayList<ApnSetting> createApnListWithSPN(Cursor cursor, String spn) {
        ArrayList<ApnSetting> result = new ArrayList<ApnSetting>();
        if (cursor.moveToFirst()) {
            do {
                if (spn != null && !spn.equals(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.SPN)))) {
                    continue;
                }
                String[] types = parseTypes(
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                ApnSetting apn = new ApnSetting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                        types,
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.ROAMING_PROTOCOL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.CARRIER_ENABLED)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)));
                result.add(apn);
            } while (cursor.moveToNext());
        }
        if (DBG) log("createApnList: X result=" + result);
        return result;
    }

    private ArrayList<ApnSetting> createApnListWithIMSI(Cursor cursor, String imsi) {
        ArrayList<ApnSetting> result = new ArrayList<ApnSetting>();
        if (cursor.moveToFirst()) {
            do {
                if (imsi != null && !imsi.equals(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.IMSI)))) {
                    continue;
                }
                String[] types = parseTypes(
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                ApnSetting apn = new ApnSetting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                        types,
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.ROAMING_PROTOCOL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.CARRIER_ENABLED)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)));
                result.add(apn);
            } while (cursor.moveToNext());
        }
        if (DBG) log("createApnList: X result=" + result);
        return result;
    }

    private ArrayList<ApnSetting> createApnListWithPNN(Cursor cursor, String pnn) {
        ArrayList<ApnSetting> result = new ArrayList<ApnSetting>();
        if (cursor.moveToFirst()) {
            do {
                if (pnn != null && !pnn.equals(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PNN)))) {
                    continue;
                }
                String[] types = parseTypes(
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                ApnSetting apn = new ApnSetting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                        NetworkUtils.trimV4AddrZeros(
                                cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                        types,
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.ROAMING_PROTOCOL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                Telephony.Carriers.CARRIER_ENABLED)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)));
                result.add(apn);
            } while (cursor.moveToNext());
        }
        if (DBG) log("createApnList: X result=" + result);
        return result;
    }

    private boolean dataConnectionNotInUse(DataConnectionAc dcac) {
        if (DBG) log("dataConnectionNotInUse: check if dcac is inuse dc=" + dcac.dataConnection);
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.getDataConnectionAc() == dcac) {
                if (DBG) log("dataConnectionNotInUse: in use by apnContext=" + apnContext);
                return false;
            }
        }
        // TODO: Fix retry handling so free DataConnections have empty apnlists.
        // Probably move retry handling into DataConnections and reduce complexity
        // of DCT.
        for (ApnContext apnContext : dcac.getApnListSync()) {
            if (DBG) {
                log("dataConnectionNotInUse: removing apnContext=" + apnContext);
            }
            dcac.removeApnContextSync(apnContext);
        }
        if (DBG) log("dataConnectionNotInUse: not in use return true");
        return true;
    }

    private GsmDataConnection findFreeDataConnection() {
        for (DataConnectionAc dcac : mDataConnectionAsyncChannels.values()) {
            if (dcac.isInactiveSync() && dataConnectionNotInUse(dcac)) {
                DataConnection dc = dcac.dataConnection;
                if (DBG) {
                    log("findFreeDataConnection: found free GsmDataConnection=" +
                        " dcac=" + dcac + " dc=" + dc);
                }
                return (GsmDataConnection) dc;
            }
        }
        log("findFreeDataConnection: NO free GsmDataConnection");
        return null;
    }

    protected GsmDataConnection findReadyDataConnection(ApnSetting apn) {
        if (apn == null) {
            return null;
        }
        if (DBG) {
            log("findReadyDataConnection: apn string <" + apn + ">" +
                    " dcacs.size=" + mDataConnectionAsyncChannels.size());
        }
        for (DataConnectionAc dcac : mDataConnectionAsyncChannels.values()) {
            ApnSetting apnSetting = dcac.getApnSettingSync();
            if (DBG) {
                log("findReadyDataConnection: dc apn string <" +
                         (apnSetting != null ? (apnSetting.toString()) : "null") + ">");
            }
            if ((apnSetting != null) && TextUtils.equals(apnSetting.toString(), apn.toString())) {
                DataConnection dc = dcac.dataConnection;
                if (DBG) {
                    log("findReadyDataConnection: found ready GsmDataConnection=" +
                        " dcac=" + dcac + " dc=" + dc);
                }
                return (GsmDataConnection) dc;
            }
        }
        return null;
    }


    private boolean setupData(ApnContext apnContext) {
        if (DBG) log("setupData: apnContext=" + apnContext);
        ApnSetting apn;
        GsmDataConnection dc;

        int profileId = getApnProfileID(apnContext.getApnType());
        apn = apnContext.getNextWaitingApn();
        if (apn == null) {
            if (DBG) log("setupData: return for no apn found!");
            return false;
        }


        dc = (GsmDataConnection) checkForConnectionForApnContext(apnContext);
        
        // M: check if the dc's APN setting is prefered APN for default connection.
        if (dc != null && PhoneConstants.APN_TYPE_DEFAULT.equals(apnContext.getApnType())) {
            ApnSetting dcApnSetting = dc.getApnSetting();
            if (dcApnSetting != null && !dcApnSetting.apn.equals(apn.apn)) {
                if (DBG) log("The existing DC is not using prefered APN.");
                dc = null;
            }
        }

        if (dc == null) {
            dc = findReadyDataConnection(apn);

            if (dc == null) {
                if (DBG) log("setupData: No ready GsmDataConnection found!");
                // TODO: When allocating you are mapping type to id. If more than 1 free,
                // then could findFreeDataConnection get the wrong one??
                dc = findFreeDataConnection();
            }

            if (dc == null) {
                dc = createDataConnection();
            }

            if (dc == null) {
                if (PhoneFactory.isDualTalkMode()) {
                    //M: in dual-talk project, we only have single pdp ability.
                    if (apnContext.getApnType() == PhoneConstants.APN_TYPE_DEFAULT)
                    {
                        if (DBG) log("setupData: No free GsmDataConnection found!");
                        return false;
                    }
                    else
                    {
                        ApnContext DisableapnContext = mApnContexts.get(PhoneConstants.APN_TYPE_DEFAULT);
                        clearWaitingApn();
                        cleanUpConnection(true, DisableapnContext);
                        //disableApnType(PhoneConstants.APN_TYPE_DEFAULT);
                        mWaitingApnList.add(apnContext.getApnType());
                        return true;
                    }
                } else {
                    if (DBG) log("setupData: No free GsmDataConnection found!");
                    return false;
                }
            }
        } else {
            apn = mDataConnectionAsyncChannels.get(dc.getDataConnectionId()).getApnSettingSync();
        }

        DataConnectionAc dcac = mDataConnectionAsyncChannels.get(dc.getDataConnectionId());
        dc.setProfileId( profileId );  //  assumed no connection sharing on profiled types

        int refCount = dcac.getRefCountSync();
        if (DBG) log("setupData: init dc and apnContext refCount=" + refCount);

        // configure retry count if no other Apn is using the same connection.
        if (refCount == 0) {
            configureRetry(dc, apn.canHandleType(PhoneConstants.APN_TYPE_DEFAULT),
                    apnContext.getRetryCount());
        }

        if (apnContext.getDataConnectionAc() != null && apnContext.getDataConnectionAc() != dcac) {
            if (DBG) log("setupData: dcac not null and not equal to assigned dcac.");
            apnContext.setDataConnectionAc(null);
        }

        apnContext.setDataConnectionAc(dcac);
        apnContext.setDataConnection(dc);

        apnContext.setApnSetting(apn);
        apnContext.setState(DctConstants.State.INITING);
        mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        // If reconnect alarm is active on this DataConnection, wait for the alarm being
        // fired so that we don't disruppt data retry pattern engaged.
        if (apnContext.getDataConnectionAc().getReconnectIntentSync() != null) {
            if (DBG) log("setupData: data reconnection pending");
            apnContext.setState(DctConstants.State.FAILED);
            if (PhoneConstants.APN_TYPE_MMS.equals(apnContext.getApnType())) {
                mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType(), PhoneConstants.DataState.CONNECTING);
            } else {
                mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            }
            return true;
        }

        if (apnContext.getApnType() == PhoneConstants.APN_TYPE_MMS) {
            mWaitingApnList.clear();
            
            /** 
             * M: if MMS's proxy IP address is the same as current connected interface 
             *    and their APN is not the same, try to disable the existed one.
             *    Then, setup MMS's interface.
             */
            for (ApnContext currApnCtx : mApnContexts.values()) {
                ApnSetting apnSetting = currApnCtx.getApnSetting();

                if (currApnCtx == apnContext)
                    continue;            
                if ((apnSetting != null) && !currApnCtx.isDisconnected() && 
                            !apnSetting.equals(apn) && (isSameProxy(apnSetting, apn) && !apnSetting.apn.equals(apn.apn))) {
                    if (DBG) logd("setupData: disable conflict APN " + currApnCtx.getApnType());
                    disableApnType(currApnCtx.getApnType());
                    mWaitingApnList.add(currApnCtx.getApnType());
                }
            }
        }

        Message msg = obtainMessage();
        msg.what = DctConstants.EVENT_DATA_SETUP_COMPLETE;
        msg.obj = apnContext;
        dc.bringUp(msg, apn);

        if (DBG) log("setupData: initing!");
        return true;
    }

    /**
     * Handles changes to the APN database.
     */
    private void onApnChanged() {
        DctConstants.State overallState = getOverallState();
        boolean isDisconnected = (overallState == DctConstants.State.IDLE ||
                overallState == DctConstants.State.FAILED);

        if (mPhone instanceof GSMPhone) {
            // The "current" may no longer be valid.  MMS depends on this to send properly. TBD
            // M: To prevent DB access in main thread.
            new Thread(new Runnable() {
                public void run() {
                    ((GSMPhone)mPhone).updateCurrentCarrierInProvider();
                }
            }).start();			
        }

        // TODO: It'd be nice to only do this if the changed entrie(s)
        // match the current operator.
        if (DBG) log("onApnChanged: createAllApnList and cleanUpAllConnections");
        ArrayList<ApnSetting> previous_allApns = mAllApns;
        ApnSetting previous_preferredApn = mPreferredApn;
        createAllApnList();
        boolean isSameApnSetting = false;
        if ((previous_allApns == null && mAllApns == null)){
            if (previous_preferredApn == null && mPreferredApn == null) {
                isSameApnSetting = true;
            } else if (previous_preferredApn != null && previous_preferredApn.equals(mPreferredApn)) {
                isSameApnSetting = true;
            }
        } else if (previous_allApns != null && mAllApns != null) {
            String pre_all_str = "";
            String all_str = "";
            for (ApnSetting s : previous_allApns) {
                pre_all_str += s.toString();
            }
            for (ApnSetting t : mAllApns) {
                all_str += t.toString();
            }
            if (pre_all_str.equals(all_str)) {
                if (previous_preferredApn == null && mPreferredApn == null) {
                    isSameApnSetting = true;
                } else if (previous_preferredApn != null && previous_preferredApn.equals(mPreferredApn)) {
                    isSameApnSetting = true;
                    //TODO MTK remove
                }
            }
        }
        
        if (isSameApnSetting) {
            if (DBG) log("onApnChanged: not changed.");
            return;
        }
        if (DBG) log("onApnChanged: previous_preferredApn " + previous_preferredApn);
        if (DBG) log("onApnChanged: mPreferredApn " + mPreferredApn);
        cleanUpAllConnections(!isDisconnected, Phone.REASON_APN_CHANGED);
        if (isDisconnected) {
            setupDataOnReadyApns(Phone.REASON_APN_CHANGED);
        }
    }

    /**
     * @param cid Connection id provided from RIL.
     * @return DataConnectionAc associated with specified cid.
     */
    private DataConnectionAc findDataConnectionAcByCid(int cid) {
        for (DataConnectionAc dcac : mDataConnectionAsyncChannels.values()) {
            if (dcac.getCidSync() == cid) {
                return dcac;
            }
        }
        return null;
    }

    /**
     * @param dcacs Collection of DataConnectionAc reported from RIL.
     * @return List of ApnContext which is connected, but is not present in
     *         data connection list reported from RIL.
     */
    private List<ApnContext> findApnContextToClean(Collection<DataConnectionAc> dcacs) {
        if (dcacs == null) return null;

        if (DBG) log("findApnContextToClean(ar): E dcacs=" + dcacs);

        ArrayList<ApnContext> list = new ArrayList<ApnContext>();
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.getState() == DctConstants.State.CONNECTED) {
                boolean found = false;
                for (DataConnectionAc dcac : dcacs) {
                    if (dcac == apnContext.getDataConnectionAc()) {
                        // ApnContext holds the ref to dcac present in data call list.
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // ApnContext does not have dcac reported in data call list.
                    // Fetch all the ApnContexts that map to this dcac which are in
                    // INITING state too.
                    if (DBG) log("findApnContextToClean(ar): Connected apn not found in the list (" +
                                 apnContext.toString() + ")");
                    if (apnContext.getDataConnectionAc() != null) {
                        list.addAll(apnContext.getDataConnectionAc().getApnListSync());
                    } else {
                        list.add(apnContext);
                    }
                }
            }
        }
        if (DBG) log("findApnContextToClean(ar): X list=" + list);
        return list;
    }

    /**
     * @param ar is the result of RIL_REQUEST_DATA_CALL_LIST
     * or RIL_UNSOL_DATA_CALL_LIST_CHANGED
     */
    private void onDataStateChanged (AsyncResult ar) {
        ArrayList<DataCallState> dataCallStates;

        if (DBG) log("onDataStateChanged(ar): E");
        dataCallStates = (ArrayList<DataCallState>)(ar.result);

        if (ar.exception != null) {
            // This is probably "radio not available" or something
            // of that sort. If so, the whole connection is going
            // to come down soon anyway
            if (DBG) log("onDataStateChanged(ar): exception; likely radio not available, ignore");
            //mtk03851: since we have exception, we should assune there is no any pdp context
            dataCallStates = new ArrayList<DataCallState>(0);
        }

        int size = dataCallStates.size();
        if (DBG) log("onDataStateChanged(ar): DataCallState size=" + size);

        boolean isAnyDataCallDormant = false;
        boolean isAnyDataCallActive = false;

        if (size == 0) {
            /** M: if current list is not null and we receive zero length list, 
             *  we will try to cleanup all pending connections
             */
            Collection<DataConnection> collection = mDataConnections.values();
            Iterator<DataConnection> iterator = collection.iterator();
            while (iterator.hasNext()) {
                DataConnection dataConnection = iterator.next();
                DataConnectionAc dataConnectionAc = mDataConnectionAsyncChannels.get(dataConnection.getDataConnectionId());
                // M: If current data connection reference count is greater than 0 and state should be "ACTIVATE" then tear down it.
                if (dataConnectionAc != null &&
                    dataConnectionAc.getRefCountSync() > 0 &&
                    dataConnectionAc.isActiveSync())
                {
                    logw("found unlinked DataConnection, to tear down it");
                    dataConnection.tearDownAll(Phone.REASON_DATA_DETACHED, null);
                }
            }
        }
        
        // Create a hash map to store the dataCallState of each DataConnectionAc
        HashMap<DataCallState, DataConnectionAc> dataCallStateToDcac;
        dataCallStateToDcac = new HashMap<DataCallState, DataConnectionAc>();
        for (DataCallState dataCallState : dataCallStates) {
            DataConnectionAc dcac = findDataConnectionAcByCid(dataCallState.cid);

            if (dcac != null) dataCallStateToDcac.put(dataCallState, dcac);
        }

        // A list of apns to cleanup, those that aren't in the list we know we have to cleanup
        List<ApnContext> apnsToCleanup = findApnContextToClean(dataCallStateToDcac.values());

        // Find which connections have changed state and send a notification or cleanup
        for (DataCallState newState : dataCallStates) {
            DataConnectionAc dcac = dataCallStateToDcac.get(newState);

            if (dcac == null) {
                loge("onDataStateChanged(ar): No associated DataConnection ignore");
                continue;
            }

            if (newState.active == DATA_CONNECTION_ACTIVE_PH_LINK_UP) isAnyDataCallActive = true;
            if (newState.active == DATA_CONNECTION_ACTIVE_PH_LINK_DOWN) isAnyDataCallDormant = true;

            // The list of apn's associated with this DataConnection
            Collection<ApnContext> apns = dcac.getApnListSync();

            // Find which ApnContexts of this DC are in the "Connected/Connecting" state.
            ArrayList<ApnContext> connectedApns = new ArrayList<ApnContext>();
            for (ApnContext apnContext : apns) {
                if (apnContext.getState() == DctConstants.State.CONNECTED ||
                       apnContext.getState() == DctConstants.State.CONNECTING ||
                       apnContext.getState() == DctConstants.State.INITING) {
                    connectedApns.add(apnContext);
                }
            }
            if (connectedApns.size() == 0) {
                if (DBG) log("onDataStateChanged(ar): no connected apns");
            } else {
                // Determine if the connection/apnContext should be cleaned up
                // or just a notification should be sent out.
                if (DBG) log("onDataStateChanged(ar): Found ConnId=" + newState.cid
                        + " newState=" + newState.toString());
                if (newState.active == 0) {
                    if (DBG) {
                        log("onDataStateChanged(ar): inactive, cleanup apns=" + connectedApns);
                    }
                    apnsToCleanup.addAll(connectedApns);
                } else {
                    // Its active so update the DataConnections link properties
                    UpdateLinkPropertyResult result =
                        dcac.updateLinkPropertiesDataCallStateSync(newState);
                    if (result.oldLp.equals(result.newLp)) {
                        if (DBG) log("onDataStateChanged(ar): no change");
                    } else {
                        if (result.oldLp.isIdenticalInterfaceName(result.newLp)) {
                            if (! result.oldLp.isIdenticalDnses(result.newLp) ||
                                    ! result.oldLp.isIdenticalRoutes(result.newLp) ||
                                    ! result.oldLp.isIdenticalHttpProxy(result.newLp) ||
                                    ! result.oldLp.isIdenticalAddresses(result.newLp)) {
                                // If the same address type was removed and added we need to cleanup
                                CompareResult<LinkAddress> car =
                                    result.oldLp.compareAddresses(result.newLp);
                                if (DBG) {
                                    log("onDataStateChanged: oldLp=" + result.oldLp +
                                            " newLp=" + result.newLp + " car=" + car);
                                }

                                // M: If LP changes always clean up connection instead of update LP
                                apnsToCleanup.addAll(connectedApns);

                                /*
                                boolean needToClean = false;
                                for (LinkAddress added : car.added) {
                                    for (LinkAddress removed : car.removed) {
                                        if (NetworkUtils.addressTypeMatches(removed.getAddress(),
                                                added.getAddress())) {
                                            needToClean = true;
                                            break;
                                        }
                                    }
                                }
                                if (needToClean) {
                                    if (DBG) {
                                        log("onDataStateChanged(ar): addr change, cleanup apns=" +
                                                connectedApns + " oldLp=" + result.oldLp +
                                                " newLp=" + result.newLp);
                                    }
                                    apnsToCleanup.addAll(connectedApns);
                                } else {
                                    if (DBG) log("onDataStateChanged(ar): simple change");
                                    for (ApnContext apnContext : connectedApns) {
                                         mPhone.notifyDataConnection(
                                                 PhoneConstants.REASON_LINK_PROPERTIES_CHANGED,
                                                 apnContext.getApnType());
                                    }
                                }
                                */
                            } else {
                                if (DBG) {
                                    log("onDataStateChanged(ar): no changes");
                                }
                            }
                        } else {
                            //the first time we setup data call, we encounter that the interface is changed
                            //but the old interface is null (not setup yet)
                            //we should ignore cleaning up apn in this case
                            if (result.oldLp.getInterfaceName() != null) {
                            if (DBG) {
                                log("onDataStateChanged(ar): interface change, cleanup apns="
                                        + connectedApns);
                            }
                                apnsToCleanup.addAll(connectedApns);
                            } else {
                                if (DBG) {
                                    log("onDataStateChanged(ar): interface change but no old interface, not to cleanup apns"
                                            + connectedApns);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAnyDataCallDormant && !isAnyDataCallActive) {
            // There is no way to indicate link activity per APN right now. So
            // Link Activity will be considered dormant only when all data calls
            // are dormant.
            // If a single data call is in dormant state and none of the data
            // calls are active broadcast overall link state as dormant.
            mActivity = DctConstants.Activity.DORMANT;
            if (DBG) {
                log("onDataStateChanged: Data Activity updated to DORMANT. stopNetStatePoll");
            }
            stopNetStatPoll();
        } else {
            mActivity = DctConstants.Activity.NONE;
            if (DBG) {
                log("onDataStateChanged: Data Activity updated to NONE. " +
                         "isAnyDataCallActive = " + isAnyDataCallActive +
                         " isAnyDataCallDormant = " + isAnyDataCallDormant);
            }
            if (isAnyDataCallActive) startNetStatPoll();
        }
        mPhone.notifyDataActivity();

        if (apnsToCleanup.size() != 0) {
            // Add an event log when the network drops PDP
            int cid = getCellLocationId();
            EventLog.writeEvent(EventLogTags.PDP_NETWORK_DROP, cid,
                                TelephonyManager.getDefault().getNetworkType());
        }

        // Cleanup those dropped connections
        if (DBG) log("onDataStateChange(ar): apnsToCleanup=" + apnsToCleanup);
        for (ApnContext apnContext : apnsToCleanup) {
            cleanUpConnection(true, apnContext);
        }

        if (DBG) log("onDataStateChanged(ar): X");
    }

    private void notifyDefaultData(ApnContext apnContext) {
        if (DBG) {
            log("notifyDefaultData: type=" + apnContext.getApnType()
                + ", reason:" + apnContext.getReason());
        }

        // M: If context is disabled and state is DISCONNECTING, 
        // should not change the state to confuse the following enableApnType() which returns PhoneConstants.APN_ALREADY_ACTIVE as CONNECTED
        if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
            apnContext.setState(DctConstants.State.CONNECTED);
        }

        // setState(DctConstants.State.CONNECTED);
        mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        startNetStatPoll();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
        // reset reconnect timer
        apnContext.setRetryCount(0);
    }

    // TODO: For multiple Active APNs not exactly sure how to do this.
    protected void gotoIdleAndNotifyDataConnection(String reason) {
        if (DBG) log("gotoIdleAndNotifyDataConnection: reason=" + reason);
        notifyDataConnection(reason);
        mActiveApn = null;
    }

    @Override
    protected void restartRadio() {
        if (DBG) log("restartRadio: ************TURN OFF RADIO**************");
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int simId = mPhone.getMySimId();
            int dualSimMode = Settings.System.getInt(mPhone.getContext().getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 0);
            if (DBG) log("restartRadio: dual sim mode: " + dualSimMode);
            cleanUpAllConnections(true, Phone.REASON_RADIO_TURNED_OFF);
            mPhone.getServiceStateTracker().powerOffRadioSafely(this);
            //for dual sim we need restart radio power manually
            log("Start to radio off [" + dualSimMode + ", " + (dualSimMode & ~(simId+1)) + "]");

            if (PhoneFactory.isDualTalkMode()) {
                // M: As Dual Talk mode, just set radio mode via current phone.
                mPhone.mCM.setRadioMode(GeminiNetworkSubUtil.MODE_FLIGHT_MODE, obtainMessage(MSG_RESTART_RADIO_OFF_DONE, dualSimMode, 0));                
            } else {
                //we should always set radio power through 3G protocol
                GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
                int sim3G = mGeminiPhone.get3GSimId();
                if (sim3G == simId) {
                    mPhone.mCM.setRadioMode((dualSimMode & ~(simId+1)), obtainMessage(MSG_RESTART_RADIO_OFF_DONE, dualSimMode, 0));
                } else {
                    log("set radio off through peer phone(" + sim3G + ") since current phone is 2G protocol");
                    if (mPhone instanceof GSMPhone) {
                        GSMPhone peerPhone = ((GSMPhone)mPhone).getPeerPhones(sim3G);
                       ((PhoneBase)peerPhone).mCM.setRadioMode((dualSimMode & ~(simId+1)), obtainMessage(MSG_RESTART_RADIO_OFF_DONE, dualSimMode, 0));
                    }
                }
            }
        } else {
            cleanUpAllConnections(true, Phone.REASON_RADIO_TURNED_OFF);
            mPhone.getServiceStateTracker().powerOffRadioSafely(this);
            /* Note: no need to call setRadioPower(true).  Assuming the desired
             * radio power state is still ON (as tracked by ServiceStateTracker),
             * ServiceStateTracker will call setRadioPower when it receives the
             * RADIO_STATE_CHANGED notification for the power off.  And if the
             * desired power state has changed in the interim, we don't want to
             * override it with an unconditional power on.
             */
        }

        int reset = Integer.parseInt(SystemProperties.get("net.ppp.reset-by-timeout", "0"));
        SystemProperties.set("net.ppp.reset-by-timeout", String.valueOf(reset+1));
    }

    /**
     * Returns true if the last fail cause is something that
     * seems like it deserves an error notification.
     * Transient errors are ignored
     */
    private boolean shouldPostNotification(GsmDataConnection.FailCause  cause) {
        return (cause != GsmDataConnection.FailCause.UNKNOWN);
    }

    /**
     * Return true if data connection need to be setup after disconnected due to
     * reason.
     *
     * @param reason the reason why data is disconnected
     * @return true if try setup data connection is need for this reason
     */
    private boolean retryAfterDisconnected(String reason) {
        boolean retry = true;

        if ( Phone.REASON_RADIO_TURNED_OFF.equals(reason) ) {
            retry = false;
        }
        return retry;
    }

    private void reconnectAfterFail(FailCause lastFailCauseCode,
                                    ApnContext apnContext, int retryOverride) {
        if (apnContext == null) {
            loge("reconnectAfterFail: apnContext == null, impossible");
            return;
        }
        if (DBG) {
            log("reconnectAfterFail: lastFailCause=" + lastFailCauseCode +
                    " retryOverride=" + retryOverride + " apnContext=" + apnContext);
        }
        if ((apnContext.getState() == DctConstants.State.FAILED) &&
            (apnContext.getDataConnection() != null)) {
            if (!apnContext.getDataConnection().isRetryNeeded()) {
                if (!apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT)) {
                    mPhone.notifyDataConnection(Phone.REASON_APN_FAILED, apnContext.getApnType());
                    return;
                }
                if (mReregisterOnReconnectFailure) {
                    // We've re-registerd once now just retry forever.
                    apnContext.getDataConnection().retryForeverUsingLastTimeout();
                } else {
                    // Try to Re-register to the network.
                    if (DBG) log("reconnectAfterFail: activate failed, Reregistering to network");
                    mReregisterOnReconnectFailure = true;
                    mPhone.getServiceStateTracker().reRegisterNetwork(null);
                    apnContext.setRetryCount(0);
                    return;
                }
            }

            // If retry needs to be backed off for specific case (determined by RIL/Modem)
            // use the specified timer instead of pre-configured retry pattern.
            int nextReconnectDelay = retryOverride;
            if (nextReconnectDelay < 0) {
                nextReconnectDelay = apnContext.getDataConnection().getRetryTimer();
                apnContext.getDataConnection().increaseRetryCount();
                if (DBG) {
                    log("reconnectAfterFail: increaseRetryCount=" +
                            apnContext.getDataConnection().getRetryCount() +
                            " nextReconnectDelay=" + nextReconnectDelay);
                }
            }
            startAlarmForReconnect(nextReconnectDelay, apnContext);

            if (!shouldPostNotification(lastFailCauseCode)) {
                if (DBG) {
                    log("reconnectAfterFail: NOT Posting GPRS Unavailable notification "
                                + "-- likely transient error");
                }
            } else {
                notifyNoData(lastFailCauseCode, apnContext);
            }
        }
    }

    private void startAlarmForReconnect(int delay, ApnContext apnContext) {

        DataConnectionAc dcac = apnContext.getDataConnectionAc();

        if ((dcac == null) || (dcac.dataConnection == null)) {
            // should not happen, but just in case.
            loge("startAlarmForReconnect: null dcac or dc.");
            return;
        }

        if(FeatureOption.MTK_GEMINI_SUPPORT){
            GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
	    if (mGeminiPhone != null &&
               mGeminiPhone.isGprsDetachingOrDetached(mGsmPhone.getMySimId())) {
               logw("Current SIM is not active, stop reconnect.");
               return;
	    }   
        }

        AlarmManager am =
            (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(INTENT_RECONNECT_ALARM + '.' + mGsmPhone.getMySimId() +
                                   dcac.dataConnection.getDataConnectionId());
        String reason = apnContext.getReason();
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON, reason);
        int connectionId = dcac.dataConnection.getDataConnectionId();
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE, connectionId);
        String apnType = apnContext.getApnType();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mGsmPhone.getMySimId());
        }
        // TODO: Until a real fix is created, which probably entails pushing
        // retires into the DC itself, this fix gets the retry count and
        // puts it in the reconnect alarm. When the reconnect alarm fires
        // onActionIntentReconnectAlarm is called which will use the value saved
        // here and save it in the ApnContext and send the EVENT_CONNECT message
        // which invokes setupData. Then setupData will use the value in the ApnContext
        // and to tell the DC to set the retry count in the retry manager.
        int retryCount = dcac.dataConnection.getRetryCount();
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_RETRY_COUNT, retryCount);

        if (DBG) {
            log("startAlarmForReconnect: next attempt in " + (delay / 1000) + "s" +
                    " reason='" + reason + "' connectionId=" + connectionId +
                    " retryCount=" + retryCount);
        }

        PendingIntent alarmIntent = PendingIntent.getBroadcast (mPhone.getContext(), 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

        dcac.setReconnectIntentSync(alarmIntent);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, alarmIntent);

    }

    private void notifyNoData(GsmDataConnection.FailCause lastFailCauseCode,
                              ApnContext apnContext) {
        if (DBG) log( "notifyNoData: type=" + apnContext.getApnType());
        apnContext.setState(DctConstants.State.FAILED);
        if (lastFailCauseCode.isPermanentFail()
            && (!apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT))) {
            mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
    }

    private void onRecordsLoaded() {
        if (DBG) log("onRecordsLoaded: createAllApnList");
        final int gprsDefaultSIM = getDataConnectionFromSetting();
        logd("onRecordsLoaded gprsDefaultSIM:" + gprsDefaultSIM);
     
        if(gprsDefaultSIM == mGsmPhone.getMySimId()){
           // mGsmPhone.setGprsConnType(GeminiNetworkSubUtil.CONN_TYPE_ALWAYS);
        }

        // M: register peer phone notifications
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
                if (i != mGsmPhone.getMySimId() && mGsmPhone.getPeerPhones(i) != null) {
                    mGsmPhone.getPeerPhones(i).getCallTracker().unregisterForVoiceCallEnded(this);
                    mGsmPhone.getPeerPhones(i).getCallTracker().unregisterForVoiceCallStarted(this);
                    mGsmPhone.getPeerPhones(i).getCallTracker().registerForVoiceCallEnded(this, DctConstants.EVENT_VOICE_CALL_ENDED_PEER, null);
                    mGsmPhone.getPeerPhones(i).getCallTracker().registerForVoiceCallStarted(this, DctConstants.EVENT_VOICE_CALL_STARTED_PEER, null);
                }
            }
        }
		
        // MTK Put the query to threads
        new Thread(new Runnable() {
            public void run() {
                syncRoamingSetting();
                createAllApnList();
                if (mPhone.mCM.getRadioState().isOn()) {
                    if (DBG) log("onRecordsLoaded: notifying data availability");
                    notifyOffApnsOfAvailability(Phone.REASON_SIM_LOADED);
                }
                if ((FeatureOption.MTK_GEMINI_SUPPORT && gprsDefaultSIM == mGsmPhone.getMySimId()) ||
                        (!FeatureOption.MTK_GEMINI_SUPPORT && mUserDataEnabled)) {
                    // Enable default APN context if default data is enabled.
                    enableApnType(PhoneConstants.APN_TYPE_DEFAULT);
                }
              // Need to re-schedule setup data request by sending message to prevent synchronization problem,
                // since we spawn thread here to process createAllApnList(). (ALPS00294899)
                sendMessage(obtainMessage(DctConstants.EVENT_TRY_SETUP_DATA, Phone.REASON_SIM_LOADED));                
            }
        }).start();

    }

    @Override
    protected void onSetDependencyMet(String apnType, boolean met) {
        // don't allow users to tweak hipri to work around default dependency not met
        if (PhoneConstants.APN_TYPE_HIPRI.equals(apnType)) return;

        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext == null) {
            loge("onSetDependencyMet: ApnContext not found in onSetDependencyMet(" +
                    apnType + ", " + met + ")");
            return;
        }
        applyNewState(apnContext, apnContext.isEnabled(), met);
        if (PhoneConstants.APN_TYPE_DEFAULT.equals(apnType)) {
            // tie actions on default to similar actions on HIPRI regarding dependencyMet
            apnContext = mApnContexts.get(PhoneConstants.APN_TYPE_HIPRI);
            if (apnContext != null) applyNewState(apnContext, apnContext.isEnabled(), met);
        }
    }

    private void applyNewState(ApnContext apnContext, boolean enabled, boolean met) {
        boolean cleanup = false;
        boolean trySetup = false;
        if (DBG) {
            log("applyNewState(" + apnContext.getApnType() + ", " + enabled +
                    "(" + apnContext.isEnabled() + "), " + met + "(" +
                    apnContext.getDependencyMet() +"))");
        }
        if (apnContext.isReady()) {
            if (enabled && met) {
                DctConstants.State state = apnContext.getState();
                if (state == DctConstants.State.CONNECTED || state == DctConstants.State.CONNECTING) {
                    return;
                } else {
                    apnContext.setReason(Phone.REASON_DATA_ENABLED);
                    trySetup = true;
                }
            } else if (!enabled) {
                cleanup = true;
                apnContext.setReason(Phone.REASON_DATA_DISABLED);
            } else {
                cleanup = true;
                apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_UNMET);
            }
        } else {
            if (enabled && met) {
                if (apnContext.isEnabled()) {
                    apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_MET);
                } else {
                    apnContext.setReason(Phone.REASON_DATA_ENABLED);
                }
                if (apnContext.getState() == DctConstants.State.FAILED) {
                    apnContext.setState(DctConstants.State.IDLE);
                }
                trySetup = true;
            }
        }
        apnContext.setEnabled(enabled);
        apnContext.setDependencyMet(met);
        if (cleanup) cleanUpConnection(true, apnContext);
        if (trySetup) trySetupData(apnContext);
    }

    private DataConnection checkForConnectionForApnContext(ApnContext apnContext) {
        // Loop through all apnContexts looking for one with a conn that satisfies this apnType
        String apnType = apnContext.getApnType();
        ApnSetting dunSetting = null;

        if (PhoneConstants.APN_TYPE_DUN.equals(apnType)) {
            dunSetting = fetchDunApn();
        }

        DataConnection potential = null;
        for (ApnContext c : mApnContexts.values()) {
            DataConnection conn = c.getDataConnection();
            if (conn != null) {
                ApnSetting apnSetting = c.getApnSetting();
                if (dunSetting != null) {
                    if (dunSetting.equals(apnSetting)) {
                        switch (c.getState()) {
                            case CONNECTED:
                                if (DBG) {
                                    log("checkForConnectionForApnContext: apnContext=" +
                                            apnContext + " found conn=" + conn);
                                }
                                return conn;
                            case CONNECTING:
                                potential = conn;
                        }
                    }
                } else if (apnSetting != null && apnSetting.canHandleType(apnType)) {
                    switch (c.getState()) {
                        case CONNECTED:
                            if (DBG) {
                                log("checkForConnectionForApnContext: apnContext=" + apnContext +
                                        " found conn=" + conn);
                            }
                            return conn;
                        case CONNECTING:
                            potential = conn;
                    }
                }
            }
        }
        if (potential != null) {
            if (DBG) {
                log("checkForConnectionForApnContext: apnContext=" + apnContext +
                    " found conn=" + potential);
            }
            return potential;
        }

        if (DBG) log("checkForConnectionForApnContext: apnContext=" + apnContext + " NO conn");
        return null;
    }

    @Override
    protected void onEnableApn(int apnId, int enabled) {
        ApnContext apnContext = mApnContexts.get(apnIdToType(apnId));
        if (apnContext == null) {
            loge("onEnableApn(" + apnId + ", " + enabled + "): NO ApnContext");
            return;
        }
        // TODO change our retry manager to use the appropriate numbers for the new APN
        if (DBG) log("onEnableApn: apnContext=" + apnContext + " call applyNewState");
        applyNewState(apnContext, enabled == DctConstants.ENABLED, apnContext.getDependencyMet());
    }

    @Override
    // TODO: We shouldnt need this.
    protected boolean onTrySetupData(String reason) {
        if (DBG) log("onTrySetupData: reason=" + reason);
        setupDataOnReadyApns(reason);
        return true;
    }

    protected boolean onTrySetupData(ApnContext apnContext) {
        if (DBG) log("onTrySetupData: apnContext=" + apnContext);
        return trySetupData(apnContext);
    }

    @Override
    protected void onRoamingOff() {
        if (DBG) log("onRoamingOff");

        if (mUserDataEnabled == false) return;

        if (getDataOnRoamingEnabled() == false) {
            notifyOffApnsOfAvailability(Phone.REASON_ROAMING_OFF);
            setupDataOnReadyApns(Phone.REASON_ROAMING_OFF);
        } else {
            notifyDataConnection(Phone.REASON_ROAMING_OFF);
        }
    }

    @Override
    protected void onRoamingOn() {
        if (mUserDataEnabled == false) return;

        if (getDataOnRoamingEnabled()) {
            if (DBG) log("onRoamingOn: setup data on roaming");
            setupDataOnReadyApns(Phone.REASON_ROAMING_ON);
            notifyDataConnection(Phone.REASON_ROAMING_ON);
        } else {
            if (DBG) log("onRoamingOn: Tear down data connection on roaming.");
            cleanUpAllConnections(true, Phone.REASON_ROAMING_ON);
            notifyOffApnsOfAvailability(Phone.REASON_ROAMING_ON);
        }
    }

    @Override
    protected void onRadioAvailable() {
        if (DBG) log("onRadioAvailable");
        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            // setState(DctConstants.State.CONNECTED);
            notifyDataConnection(null);

            log("onRadioAvailable: We're on the simulator; assuming data is connected");
        }

        IccRecords r = mIccRecords.get();
        if (r != null && r.getRecordsLoaded()) {
            notifyOffApnsOfAvailability(null);
        }

        if (getOverallState() != DctConstants.State.IDLE) {
            cleanUpConnection(true, null);
        }
    }

    @Override
    protected void onRadioOffOrNotAvailable() {
        // Make sure our reconnect delay starts at the initial value
        // next time the radio comes on

        resetAllRetryCounts();
        mReregisterOnReconnectFailure = false;

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            log("We're on the simulator; assuming radio off is meaningless");
        } else {
            if (DBG) log("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnections(false, Phone.REASON_RADIO_TURNED_OFF);
        }
        notifyOffApnsOfAvailability(null);
    }

    @Override
    protected void onDataSetupComplete(AsyncResult ar) {

        DataConnection.FailCause cause = DataConnection.FailCause.UNKNOWN;
        boolean handleError = false;
        ApnContext apnContext = null;

        if(ar.userObj instanceof ApnContext){
            apnContext = (ApnContext)ar.userObj;
        } else {
            throw new RuntimeException("onDataSetupComplete: No apnContext");
        }

        if (isDataSetupCompleteOk(ar)) {
            DataConnectionAc dcac = apnContext.getDataConnectionAc();

            if (RADIO_TESTS) {
                // Note: To change radio.test.onDSC.null.dcac from command line you need to
                // adb root and adb remount and from the command line you can only change the
                // value to 1 once. To change it a second time you can reboot or execute
                // adb shell stop and then adb shell start. The command line to set the value is:
                //   adb shell sqlite3 /data/data/com.android.providers.settings/databases/settings.db "insert into system (name,value) values ('radio.test.onDSC.null.dcac', '1');"
                ContentResolver cr = mPhone.getContext().getContentResolver();
                String radioTestProperty = "radio.test.onDSC.null.dcac";
                if (Settings.System.getInt(cr, radioTestProperty, 0) == 1) {
                    log("onDataSetupComplete: " + radioTestProperty +
                            " is true, set dcac to null and reset property to false");
                    dcac = null;
                    Settings.System.putInt(cr, radioTestProperty, 0);
                    log("onDataSetupComplete: " + radioTestProperty + "=" +
                            Settings.System.getInt(mPhone.getContext().getContentResolver(),
                                    radioTestProperty, -1));
                }
            }
            if (dcac == null) {
                log("onDataSetupComplete: no connection to DC, handle as error");
                cause = DataConnection.FailCause.CONNECTION_TO_DATACONNECTIONAC_BROKEN;
                handleError = true;
            } else {
                DataConnection dc = apnContext.getDataConnection();
                ApnSetting apn = apnContext.getApnSetting();
                if (DBG) {
                    log("onDataSetupComplete: success apn=" + (apn == null ? "unknown" : apn.apn));
                }
                if (apn != null && apn.proxy != null && apn.proxy.length() != 0) {
                    try {
                        String port = apn.port;
                        if (TextUtils.isEmpty(port)) port = "8080";
                        ProxyProperties proxy = new ProxyProperties(apn.proxy,
                                Integer.parseInt(port), null);
                        dcac.setLinkPropertiesHttpProxySync(proxy);
                    } catch (NumberFormatException e) {
                        loge("onDataSetupComplete: NumberFormatException making ProxyProperties (" +
                                apn.port + "): " + e);
                    }
                }

                // everything is setup
                if(TextUtils.equals(apnContext.getApnType(),PhoneConstants.APN_TYPE_DEFAULT)) {
                    SystemProperties.set("gsm.defaultpdpcontext.active", "true");
                    if (canSetPreferApn && mPreferredApn == null) {
                        if (DBG) log("onDataSetupComplete: PREFERED APN is null");
                        mPreferredApn = apn;
                        if (mPreferredApn != null) {
                            setPreferredApn(mPreferredApn.id);
                        }
                    }
                } else {
                    SystemProperties.set("gsm.defaultpdpcontext.active", "false");
                }

                mTetheringExt.onDataSetupComplete(apnContext);

                // Notify call start again if call is not IDLE and not concurrent
                if (((GsmCallTracker)mGsmPhone.getCallTracker()).state != PhoneConstants.State.IDLE &&
                        !mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                    if (DBG) log("onDataSetupComplete: In 2G phone call, notify data REASON_VOICE_CALL_STARTED");
                    notifyDataConnection(Phone.REASON_VOICE_CALL_STARTED);
                }
                notifyDefaultData(apnContext);
                
                /// M: SCRI and Fast Dormancy Feature @{
                //MTK-START [mtk04070][111205][ALPS00093395]Add for SCRI 
                /* Add by MTK03594 for SCRI feature */                                
                startScriPoll();                
                //MTK-END [mtk04070][111205][ALPS00093395]Add for SCRI
                /// @} 
            }
        } else {
            cause = (DataConnection.FailCause) (ar.result);
            if (DBG) {
                ApnSetting apn = apnContext.getApnSetting();
                log(String.format("onDataSetupComplete: error apn=%s cause=%s",
                        (apn == null ? "unknown" : apn.apn), cause));
            }
            if (cause.isEventLoggable()) {
                // Log this failure to the Event Logs.
                int cid = getCellLocationId();
                EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL,
                        cause.ordinal(), cid, TelephonyManager.getDefault().getNetworkType());
            }

            // Count permanent failures and remove the APN we just tried
            if (cause.isPermanentFail()) apnContext.decWaitingApnsPermFailCount();

            if (cause == DataConnection.FailCause.GMM_ERROR) {
                // stop retry for GMM_ERROR and let GPRS attach event to trigger
                apnContext.decWaitingApnsPermFailCount();
                log("onDataSetupComplete: GMM_ERROR, wait for GPRS attach to retry.");
            }

            apnContext.removeWaitingApn(apnContext.getApnSetting());
            if (DBG) {
                log(String.format("onDataSetupComplete: WaitingApns.size=%d" +
                        " WaitingApnsPermFailureCountDown=%d",
                        apnContext.getWaitingApns().size(),
                        apnContext.getWaitingApnsPermFailCount()));
            }
            handleError = true;
        }

        if (handleError) {
            // See if there are more APN's to try
            if (apnContext.getWaitingApns().isEmpty()) {
                if (apnContext.getWaitingApnsPermFailCount() == 0) {
                    if (DBG) {
                        log("onDataSetupComplete: All APN's had permanent failures, stop retrying");
                    }
                    apnContext.setState(DctConstants.State.FAILED);
                    mPhone.notifyDataConnection(Phone.REASON_APN_FAILED, apnContext.getApnType());

                    apnContext.setDataConnection(null);
                    apnContext.setDataConnectionAc(null);
                    // M: try to enable apn which is in waiting list
                    if (apnContext.getApnType() == PhoneConstants.APN_TYPE_MMS) {
                        enableWaitingApn();
                    }

                    if (PhoneFactory.isDualTalkMode()) {
                        if (apnContext.getApnType() != PhoneConstants.APN_TYPE_DEFAULT && mWaitingApnList.isEmpty()) {
                            // try to restore default
                            trySetupData(Phone.REASON_DATA_ENABLED, PhoneConstants.APN_TYPE_DEFAULT);
                        }
                    }
					
                    if (DBG) {
                        // log("onDataSetupComplete: permanent error apn=%s" + apnString );
                    }
                } else {
                    if (DBG) log("onDataSetupComplete: Not all permanent failures, retry");
                    // check to see if retry should be overridden for this failure.
                    int retryOverride = -1;
                    if (ar.exception instanceof DataConnection.CallSetupException) {
                        retryOverride =
                            ((DataConnection.CallSetupException)ar.exception).getRetryOverride();
                    }
                    if (retryOverride == RILConstants.MAX_INT) {
                        if (DBG) log("No retry is suggested.");
                    } else {
                        startDelayedRetry(cause, apnContext, retryOverride);
                    }
                }
            } else {
                if (DBG) log("onDataSetupComplete: Try next APN");
                apnContext.setState(DctConstants.State.SCANNING);
                // Wait a bit before trying the next APN, so that
                // we're not tying up the RIL command channel
                startAlarmForReconnect(APN_DELAY_MILLIS, apnContext);
            }
        }
    }

    /**
     * Called when EVENT_DISCONNECT_DONE is received.
     */
    @Override
    protected void onDisconnectDone(int connId, AsyncResult ar) {
        boolean enableApnRet = false;
        ApnContext apnContext = null;

        if (ar.userObj instanceof ApnContext) {
            apnContext = (ApnContext) ar.userObj;
        } else {
            loge("onDisconnectDone: Invalid ar in onDisconnectDone, ignore");
            return;
        }

        if(DBG) log("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=" + apnContext);

        // M: try to enable apn which is in waiting list. 
        // In dual-talk, due to we only have single pdp, 
        // we need to disconnect the existed interface and setup the new one.
        if(PhoneFactory.isDualTalkMode()) {
            enableApnRet = enableWaitingApn();
            if (enableApnRet) {
                if (apnContext.getApnType() == PhoneConstants.APN_TYPE_DEFAULT) {
                    // avoid default retry, ban retry
                    apnContext.setReason(Phone.REASON_RADIO_TURNED_OFF);
                    logd("onDisconnectoinDone: set reason to radio turned off to avoid retry.");
                }
            }
        } else {
            if (apnContext.getApnType() == PhoneConstants.APN_TYPE_MMS) {
                enableWaitingApn();
            }
        }


        apnContext.setState(DctConstants.State.IDLE);
        
        // M: handle connection reactive case. Try to setup it later after receive disconnect done event
        if (apnContext.isReactive() && apnContext.isReady()) {
            if(DBG) log("onDisconnectDone(): isReactive() == true, notify " + apnContext.getApnType() +" APN with state CONNECTING");
            mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType(), PhoneConstants.DataState.CONNECTING);
        } else {
            mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        }
        apnContext.setReactive(false);

        // if all data connection are gone, check whether Airplane mode request was
        // pending.
        if (isDisconnected()) {
            /// M: SCRI and Fast Dormancy Feature @{ 
            //Modify by mtk01411: Only all data connections are terminated, it is necessary to invoke the stopScriPoll()
            if(DBG) log("All data connections are terminated:stopScriPoll()");                    	
            stopScriPoll();
            /// @}        	
            if (mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
                // Radio will be turned off. No need to retry data setup
                apnContext.setApnSetting(null);
                apnContext.setDataConnection(null);
                apnContext.setDataConnectionAc(null);
                return;
            }
        }

        /// M: SCRI and Fast Dormancy Feature @{        
        //MTK-START [mtk04070][111205][ALPS00093395]Stop SCRI polling
        //Modify by mtk01411: Only all data connections are terminated, it is necessary to invoke the stopScriPoll()
        //stopScriPoll();
        //MTK-END [mtk04070][111205][ALPS00093395]Stop SCRI polling
        /// @}
        // If APN is still enabled, try to bring it back up automatically
        if (apnContext.isReady() && retryAfterDisconnected(apnContext.getReason())) {
            SystemProperties.set("gsm.defaultpdpcontext.active", "false");  // TODO - what the heck?  This shoudld go
            // Wait a bit before trying the next APN, so that
            // we're not tying up the RIL command channel.
            // This also helps in any external dependency to turn off the context.
            startAlarmForReconnect(APN_DELAY_MILLIS, apnContext);
        } else {
            apnContext.setApnSetting(null);
            apnContext.setDataConnection(null);
            apnContext.setDataConnectionAc(null);
        }

        if (PhoneFactory.isDualTalkMode()) {
            if (enableApnRet == false) {
                if (apnContext.getApnType() != PhoneConstants.APN_TYPE_DEFAULT) {
                    // try to restore default
                    trySetupData(Phone.REASON_DATA_ENABLED, PhoneConstants.APN_TYPE_DEFAULT);
                }
            }
        }
    }

    protected void onPollPdp() {
        if (getOverallState() == DctConstants.State.CONNECTED) {
            // only poll when connected
            mPhone.mCM.getDataCallList(this.obtainMessage(DctConstants.EVENT_DATA_STATE_CHANGED));
            sendMessageDelayed(obtainMessage(DctConstants.EVENT_POLL_PDP), POLL_PDP_MILLIS);
        }
    }

    @Override
    protected void onVoiceCallStarted() {
        if (DBG) log("onVoiceCallStarted");

        if (!mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            notifyDataConnection(Phone.REASON_VOICE_CALL_STARTED);            
        }
 
        if (isConnected() && ! mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            if (DBG) log("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
        }
    }

    @Override
    protected void onVoiceCallEnded() {
        if (DBG) log("onVoiceCallEnded");

        if (!mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            notifyDataConnection(Phone.REASON_VOICE_CALL_ENDED);
        }

        if (isConnected()) {
            if (!mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                startNetStatPoll();
                startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
            } else {
                // clean slate after call end.
                resetPollStats();
            }
        }
        // reset reconnect timer
        setupDataOnReadyApns(Phone.REASON_VOICE_CALL_ENDED);
    }
        
    protected void onVoiceCallEndedPeer() {
        if (DBG) log("onVoiceCallEndedPeer");

        notifyDataConnection(Phone.REASON_VOICE_CALL_ENDED);

        if (isConnected()) {
            startNetStatPoll();
            startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
        }
        // reset reconnect timer
        setupDataOnReadyApns(Phone.REASON_VOICE_CALL_ENDED);
    }

    @Override
    protected void onCleanUpConnection(boolean tearDown, int apnId, String reason) {
        if (DBG) log("onCleanUpConnection");
        ApnContext apnContext = mApnContexts.get(apnIdToType(apnId));
        if (apnContext != null) {
            apnContext.setReason(reason);
            cleanUpConnection(tearDown, apnContext);
        }
    }

    protected boolean isConnected() {
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.getState() ==DctConstants.State.CONNECTED) {
                // At least one context is connected, return true
                return true;
            }
        }
        // There are not any contexts connected, return false
        return false;
    }

    @Override
    public boolean isDisconnected() {
        for (ApnContext apnContext : mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                // At least one context was not disconnected return false
                return false;
            }
        }
        // All contexts were disconnected so return true
        return true;
    }

    @Override
    protected void notifyDataConnection(String reason) {
        if (DBG) log("notifyDataConnection: reason=" + reason);
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.isReady()) {
                if (DBG) log("notifyDataConnection: type:"+apnContext.getApnType());
                mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(),
                        apnContext.getApnType());
            }
        }
        notifyOffApnsOfAvailability(reason);
    }

    /**
     * Based on the sim operator numeric, create a list for all possible
     * Data Connections and setup the preferredApn.
     */
    private void createAllApnList() {
        mAllApns = new ArrayList<ApnSetting>();
        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";
        if (operator != null) {
            String selection = "numeric = '" + operator + "'";
            // query only enabled apn.
            // carrier_enabled : 1 means enabled apn, 0 disabled apn.
            selection += " and carrier_enabled = 1";
            if (DBG) log("createAllApnList: selection=" + selection);
            Cursor cursor = null;
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                Uri geminiUri = CONTENT_URI_GEMINI[mPhone.getMySimId()];
                cursor = mPhone.getContext().getContentResolver().query(geminiUri, null, selection, null, null);
            } else {
                cursor = mPhone.getContext().getContentResolver().query(
                    Telephony.Carriers.CONTENT_URI, null, selection, null, null);
            }

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    mAllApns = createApnList(cursor);
                }
                cursor.close();
            }
        }

        if (mAllApns.isEmpty()) {
            if (DBG) log("createAllApnList: No APN found for carrier: " + operator);
            mPreferredApn = null;
            // TODO: What is the right behaviour?
            //notifyNoData(GsmDataConnection.FailCause.MISSING_UNKNOWN_APN);
        } else {
            mPreferredApn = getPreferredApn();
            if (DBG) log("createAllApnList: mPreferredApn_XXX=" + mPreferredApn);
            if (r != null && (mPreferredApn == null || !mPreferredApn.numeric.equals(operator))) {
                Cursor cursor = mGsmDCTExt.getOptPreferredApn(r.getIMSI(),
                        r.getOperatorNumeric(), mPhone.getMySimId());
                if (cursor != null) {
                    ArrayList<ApnSetting> result = createApnList(cursor);
                    cursor.close();
                    if (result != null && result.size() > 0) {
                        mPreferredApn = result.get(0);
                    }
                }
            }
            if (mPreferredApn != null && !mPreferredApn.numeric.equals(operator)) {
                mPreferredApn = null;
                setPreferredApn(-1);
            }
            if (DBG) log("createAllApnList: mPreferredApn=" + mPreferredApn);
        }

        mTetheringExt.onCreateAllApnList(new ArrayList<IApnSetting>(mAllApns), operator);

        if (DBG) log("createAllApnList: X mAllApns=" + mAllApns);
    }

    /** Return the id for a new data connection */
    private GsmDataConnection createDataConnection() {
        if (DBG) log("createDataConnection E");

        RetryManager rm = new RetryManager();
        int id = mUniqueIdGenerator.getAndIncrement();

        // TODO: need to fix before dual talk enabled
        //if (id >= TelephonyManager.getMaxPdpNum(mPhone.getMySimId())) {
        //    loge("Max PDP count is "+ TelephonyManager.getMaxPdpNum(mPhone.getMySimId())+",but request " + (id + 1));
        //    return null;
        //}
        if (id >= PDP_CONNECTION_POOL_SIZE) {
            loge("Max PDP count is 3,but request " + (id + 1));
            return null;
        }
        GsmDataConnection conn = GsmDataConnection.makeDataConnection(mPhone, id, rm, this);
        mDataConnections.put(id, conn);
        DataConnectionAc dcac = new DataConnectionAc(conn, LOG_TAG);
        int status = dcac.fullyConnectSync(mPhone.getContext(), this, conn.getHandler());
        if (status == AsyncChannel.STATUS_SUCCESSFUL) {
            mDataConnectionAsyncChannels.put(dcac.dataConnection.getDataConnectionId(), dcac);
        } else {
            loge("createDataConnection: Could not connect to dcac.mDc=" + dcac.dataConnection +
                    " status=" + status);
        }

        // install reconnect intent filter for this data connection.
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_RECONNECT_ALARM + '.' + mGsmPhone.getMySimId() + id);
        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);

        if (DBG) log("createDataConnection() X id=" + id + " dc=" + conn);
        return conn;
    }

    private void configureRetry(DataConnection dc, boolean forDefault, int retryCount) {
        if (DBG) {
            log("configureRetry: forDefault=" + forDefault + " retryCount=" + retryCount +
                    " dc=" + dc);
        }
        if (dc == null) return;

        if (!dc.configureRetry(getReryConfig(forDefault))) {
            if (forDefault) {
                if (!dc.configureRetry(DEFAULT_DATA_RETRY_CONFIG)) {
                    // Should never happen, log an error and default to a simple linear sequence.
                    loge("configureRetry: Could not configure using " +
                            "DEFAULT_DATA_RETRY_CONFIG=" + DEFAULT_DATA_RETRY_CONFIG);
                    dc.configureRetry(20, 2000, 1000);
                }
            } else {
                if (!dc.configureRetry(SECONDARY_DATA_RETRY_CONFIG)) {
                    // Should never happen, log an error and default to a simple sequence.
                    loge("configureRetry: Could note configure using " +
                            "SECONDARY_DATA_RETRY_CONFIG=" + SECONDARY_DATA_RETRY_CONFIG);
                    dc.configureRetry("max_retries=3, 333, 333, 333");
                }
            }
        }
        dc.setRetryCount(retryCount);
    }

    private void destroyDataConnections() {
        if(mDataConnections != null) {
            if (DBG) log("destroyDataConnections: clear mDataConnectionList");
            mDataConnections.clear();
        } else {
            if (DBG) log("destroyDataConnections: mDataConnecitonList is empty, ignore");
        }
    }

    /**
     * Build a list of APNs to be used to create PDP's.
     *
     * @param requestedApnType
     * @return waitingApns list to be used to create PDP
     *          error when waitingApns.isEmpty()
     */
    private ArrayList<ApnSetting> buildWaitingApns(String requestedApnType) {
        ArrayList<ApnSetting> apnList = new ArrayList<ApnSetting>();

        if (requestedApnType.equals(PhoneConstants.APN_TYPE_DUN)) {
            ApnSetting dun = fetchDunApn();
            if (dun != null) {
                apnList.add(dun);
                if (DBG) log("buildWaitingApns: X added APN_TYPE_DUN apnList=" + apnList);
                return apnList;
            }
        }

        if (requestedApnType.equals(PhoneConstants.APN_TYPE_DM)) {
            ArrayList<ApnSetting> dm = fetchDMApn();
            return dm;
        }

        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";
        int radioTech = mPhone.getServiceState().getRilRadioTechnology();

        mTetheringExt.onBuildWaitingApns(requestedApnType, new ArrayList<IApnSetting>(apnList), operator);


        // This is a workaround for a bug (7305641) where we don't failover to other
        // suitable APNs if our preferred APN fails.  On prepaid ATT sims we need to
        // failover to a provisioning APN, but once we've used their default data
        // connection we are locked to it for life.  This change allows ATT devices
        // to say they don't want to use preferred at all.
        boolean usePreferred = true;
        try {
            usePreferred = ! mPhone.getContext().getResources().getBoolean(com.android.
                    internal.R.bool.config_dontPreferApn);
        } catch (Resources.NotFoundException e) {
            usePreferred = true;
        }

        if (usePreferred && canSetPreferApn && mPreferredApn != null &&
                mPreferredApn.canHandleType(requestedApnType)) {
            if (DBG) {
                log("buildWaitingApns: Preferred APN:" + operator + ":"
                        + mPreferredApn.numeric + ":" + mPreferredApn);
            }
            if (mPreferredApn.numeric.equals(operator)) {
                if (mPreferredApn.bearer == 0 || mPreferredApn.bearer == radioTech) {
                    apnList.add(mPreferredApn);
                    if (DBG) log("buildWaitingApns: X added preferred apnList=" + apnList);
                    return apnList;
                } else {
                    if (DBG) log("buildWaitingApns: no preferred APN");
                    setPreferredApn(-1);
                    mPreferredApn = null;
                }
            } else {
                if (DBG) log("buildWaitingApns: no preferred APN");
                setPreferredApn(-1);
                mPreferredApn = null;
            }
        }
        if (mAllApns != null) {
            for (ApnSetting apn : mAllApns) {
                if (apn.canHandleType(requestedApnType)) {
                    if (apn.bearer == 0 || apn.bearer == radioTech) {
                        if (DBG) log("apn info : " +apn.toString());
                        apnList.add(apn);
                    }
                }
            }
        } else {
            loge("mAllApns is empty!");
        }
        if (DBG) log("buildWaitingApns: X apnList=" + apnList);
        return apnList;
    }

    private String apnListToString (ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, size = apns.size(); i < size; i++) {
            result.append('[')
                  .append(apns.get(i).toString())
                  .append(']');
        }
        return result.toString();
    }

    private void startDelayedRetry(GsmDataConnection.FailCause cause,
                                   ApnContext apnContext, int retryOverride) {
        notifyNoData(cause, apnContext);
        reconnectAfterFail(cause, apnContext, retryOverride);
    }

    private void setPreferredApn(int pos) {
        if (!canSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }

        log("setPreferredApn: delete");
        ContentResolver resolver = mPhone.getContext().getContentResolver();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Uri geminiUri = PREFERAPN_NO_UPDATE_URI_GEMINI[mPhone.getMySimId()];
            resolver.delete(geminiUri, null, null);
        } else {
            resolver.delete(PREFERAPN_NO_UPDATE_URI, null, null);
        }

        if (pos >= 0) {
            log("setPreferredApn: insert");
            ContentValues values = new ContentValues();
            values.put(APN_ID, pos);
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                Uri geminiUri = PREFERAPN_NO_UPDATE_URI_GEMINI[mPhone.getMySimId()];
                resolver.insert(geminiUri, values);
            } else {
                resolver.insert(PREFERAPN_NO_UPDATE_URI, values);
            }
        }
    }

    private ApnSetting getPreferredApn() {
        if (mAllApns.isEmpty()) {
            log("getPreferredApn: X not found mAllApns.isEmpty");
            return null;
        }

        Uri queryPreferApnUri = PREFERAPN_NO_UPDATE_URI;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            queryPreferApnUri = PREFERAPN_NO_UPDATE_URI_GEMINI[mPhone.getMySimId()];
        }
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                queryPreferApnUri, new String[] { "_id", "name", "apn" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            canSetPreferApn = true;
            if (DBG) log("getPreferredApn: canSetPreferApn= " + canSetPreferApn + ",count " + cursor.getCount());
        } else {
            canSetPreferApn = false;
            if (DBG) log("getPreferredApn: canSetPreferApn= " + canSetPreferApn);
        }

        if (canSetPreferApn && cursor.getCount() > 0) {
            int pos;
            cursor.moveToFirst();
            pos = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
            for(ApnSetting p:mAllApns) {
                if (p.id == pos && p.canHandleType(mRequestedApnType)) {
                    log("getPreferredApn: X found apnSetting" + p);
                    cursor.close();
                    return p;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        log("getPreferredApn: X not found");
        return null;
    }

    @Override
    public void handleMessage (Message msg) {
        if (DBG) log("handleMessage msg=" + msg);

        if (!mPhone.mIsTheCurrentActivePhone || mIsDisposed) {
            loge("handleMessage: Ignore GSM msgs since GSM phone is inactive");
            return;
        }

        switch (msg.what) {
            case DctConstants.EVENT_RECORDS_LOADED:
                onRecordsLoaded();
                break;

            case DctConstants.EVENT_DATA_CONNECTION_DETACHED:
                onDataConnectionDetached();
                break;

            case DctConstants.EVENT_DATA_CONNECTION_ATTACHED:
                onDataConnectionAttached();
                break;

            case DctConstants.EVENT_DATA_STATE_CHANGED:
                onDataStateChanged((AsyncResult) msg.obj);
                break;

            case DctConstants.EVENT_POLL_PDP:
                onPollPdp();
                break;

            case DctConstants.EVENT_DO_RECOVERY:
                doRecovery();
                break;

            case DctConstants.EVENT_APN_CHANGED:
                onApnChanged();
                break;

            case DctConstants.EVENT_PS_RESTRICT_ENABLED:
                /**
                 * We don't need to explicitly to tear down the PDP context
                 * when PS restricted is enabled. The base band will deactive
                 * PDP context and notify us with PDP_CONTEXT_CHANGED.
                 * But we should stop the network polling and prevent reset PDP.
                 */
                if (DBG) log("EVENT_PS_RESTRICT_ENABLED " + mIsPsRestricted);
                stopNetStatPoll();
                stopDataStallAlarm();
                mIsPsRestricted = true;
                break;

            case DctConstants.EVENT_PS_RESTRICT_DISABLED:
                /**
                 * When PS restrict is removed, we need setup PDP connection if
                 * PDP connection is down.
                 */
                if (DBG) log("EVENT_PS_RESTRICT_DISABLED " + mIsPsRestricted);
                mIsPsRestricted  = false;

                /// M: SCRI and Fast Dormancy Feature @{
                //MTK-START [mtk04070][111205][ALPS00093395]Add for SCRI                
                startScriPoll();
                //MTK-END [mtk04070][111205][ALPS00093395]Add for SCRI
                /// @}
                
                if (isConnected()) {
                    startNetStatPoll();
                    startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
                } else {
                    // TODO: Should all PDN states be checked to fail?
                    if (mState ==DctConstants.State.FAILED) {
                        cleanUpAllConnections(false, Phone.REASON_PS_RESTRICT_ENABLED);
                        resetAllRetryCounts();
                        mReregisterOnReconnectFailure = false;
                    }
                    trySetupData(Phone.REASON_PS_RESTRICT_ENABLED, PhoneConstants.APN_TYPE_DEFAULT);
                }
                break;
            case DctConstants.EVENT_TRY_SETUP_DATA:
                if (msg.obj instanceof ApnContext) {
                    onTrySetupData((ApnContext)msg.obj);
                } else if (msg.obj instanceof String) {
                    onTrySetupData((String)msg.obj);
                } else {
                    loge("EVENT_TRY_SETUP request w/o apnContext or String");
                }
                break;

            case DctConstants.EVENT_CLEAN_UP_CONNECTION:
                boolean tearDown = (msg.arg1 == 0) ? false : true;
                if (DBG) log("EVENT_CLEAN_UP_CONNECTION tearDown=" + tearDown);
                if (msg.obj instanceof ApnContext) {
                    cleanUpConnection(tearDown, (ApnContext)msg.obj);
                } else {
                    loge("EVENT_CLEAN_UP_CONNECTION request w/o apn context");
                }
                break;

            /// M: SCRI and Fast Dormancy Feature @{    
            //MTK-START [mtk04070][111205][ALPS00093395]Add for SCRI                
            case DctConstants.EVENT_SCRI_RESULT:
                logd("[SCRI]EVENT_SCRI_RESULT");
                handleScriResult((AsyncResult) msg.obj);
                break;
  
            case DctConstants.EVENT_SCRI_RETRY_TIMER:
                logd("[SCRI]EVENT_SCRI_RETRY_TIMER");
                //[Begin]Solve [ALPS00239224]Failed to send MMS, mtk04070, 20120227.
                if (mScriManager.isDataTransmitting()) {
                    logd("[SCRI]Data is transmitting, cancel retry mechanism.");
                    mScriManager.mScriRetryCounter = 0;
                    mScriManager.setScriState(ScriManager.STATE_ACTIVATED);
                    mScriManager.setScriDataCount(true);
                    mScriManager.setPsSessionStatus(true);
                }
                else {
                    sendScriCmd(true);
                }	
                //[End]Solve [ALPS00239224]Failed to send MMS, mtk04070, 20120227.
                break;

            case DctConstants.EVENT_SCRI_CMD_RESULT:
                logd("[SCRI]EVENT_SCRI_CMD_RESULT");
                AsyncResult ar= (AsyncResult) msg.obj;
                if(ar.exception != null) {
                   logd("command error in +ESCRI");
                   mScriManager.setScriState(ScriManager.STATE_ACTIVATED);
                }
                break;
                
            case DctConstants.EVENT_NW_RAT_CHANGED:
                logd("[SCRI]EVENT_NW_RAT_CHANGED");
                Integer rat = (Integer) ((AsyncResult) msg.obj).result;                
                int result = mScriManager.getScriNwType(rat.intValue());
                
                switch(result){
                    case ScriManager.SCRI_3G:
                        /// M: Fast Dormancy:Fix InterRAT problem ALPS00364331 @{
                        logd("[SCRI] InterRAT to 3G, Set mIsUmtsMode as true before startScriPoll()");
                        mIsUmtsMode = true;
                        startScriPoll();
                        //mIsUmtsMode = true;
                        /// @}
                        break;
                    case ScriManager.SCRI_2G:
                        stopScriPoll();
                        mIsUmtsMode = false;
                        break;
                    case ScriManager.SCRI_NONE:
                        break;                    
                    }
                break;
            //MTK-END [mtk04070][111205][ALPS00093395]Add for SCRI                
            /// @}
            case MSG_RESTART_RADIO_OFF_DONE:
                logi("MSG_RESTART_RADIO_OFF_DONE");
                int simId = mPhone.getMySimId();
                if (PhoneFactory.isDualTalkMode()) {
                    // As Dual Talk mode, just set radio mode via current phone.
                    mPhone.mCM.setRadioMode(GeminiNetworkSubUtil.MODE_SIM1_ONLY, obtainMessage(MSG_RESTART_RADIO_ON_DONE));
                } else {
                    GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
                    int sim3G = mGeminiPhone.get3GSimId();
                    if (sim3G == simId) {
                        mPhone.mCM.setRadioMode(msg.arg1, obtainMessage(MSG_RESTART_RADIO_ON_DONE));
                    } else {
                        log("set radio on through peer phone(" + sim3G + ") since current phone is 2G protocol");
                        if (mPhone instanceof GSMPhone) {
                            GSMPhone peerPhone = ((GSMPhone)mPhone).getPeerPhones(sim3G);
                            ((PhoneBase)peerPhone).mCM.setRadioMode(msg.arg1, obtainMessage(MSG_RESTART_RADIO_ON_DONE));
                        }
                    }
                }
                break;
            case MSG_RESTART_RADIO_ON_DONE:
                logi("MSG_RESTART_RADIO_ON_DONE");
                break;
            case DctConstants.EVENT_PS_RAT_CHANGED:
                // RAT change is only nofity active APNs in GsmServiceStateTracker
                // Here notify "off" APNs for RAT change.
                logd("EVENT_PS_RAT_CHANGED");
                notifyOffApnsOfAvailability(Phone.REASON_NW_TYPE_CHANGED);
                break;
            case DctConstants.EVENT_GET_AVAILABLE_NETWORK_DONE:
                logd("EVENT_GET_AVAILABLE_NETWORK_DONE");
                setupDataOnReadyApns(Phone.REASON_PS_RESTRICT_DISABLED);
                break;
            case DctConstants.EVENT_VOICE_CALL_STARTED_PEER:
                logd("EVENT_VOICE_CALL_STARTED_PEER");
                onVoiceCallStarted();
                break;
            case DctConstants.EVENT_VOICE_CALL_ENDED_PEER:
                logd("EVENT_VOICE_CALL_ENDED_PEER");
                onVoiceCallEndedPeer();
                break;
            default:
                // handle the message in the super class DataConnectionTracker
                super.handleMessage(msg);
                break;
        }
    }

    protected int getApnProfileID(String apnType) {
        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IMS)) {
            return RILConstants.DATA_PROFILE_IMS;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_FOTA)) {
            return RILConstants.DATA_PROFILE_FOTA;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_CBS)) {
            return RILConstants.DATA_PROFILE_CBS;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_MMS)) {
            return RILConstants.DATA_PROFILE_MTK_MMS;
        } else {
            return RILConstants.DATA_PROFILE_DEFAULT;
        }
    }

    private int getCellLocationId() {
        int cid = -1;
        CellLocation loc = mPhone.getCellLocation();

        if (loc != null) {
            if (loc instanceof GsmCellLocation) {
                cid = ((GsmCellLocation)loc).getCid();
            } else if (loc instanceof CdmaCellLocation) {
                cid = ((CdmaCellLocation)loc).getBaseStationId();
            }
        }
        return cid;
    }

    @Override
    protected void onUpdateIcc() {
        if (mUiccController == null ) {
            return;
        }

        IccRecords newIccRecords = mUiccController.getIccRecords(UiccController.APP_FAM_3GPP);

        IccRecords r = mIccRecords.get();
        if (r != newIccRecords) {
            if (r != null) {
                log("Removing stale icc objects.");
                r.unregisterForRecordsLoaded(this);
                mIccRecords.set(null);
            }
            if (newIccRecords != null) {
                log("New records found");
                mIccRecords.set(newIccRecords);
                newIccRecords.registerForRecordsLoaded(
                        this, DctConstants.EVENT_RECORDS_LOADED, null);
            }
        }
    }

    @Override
    protected void log(String s) {
        //MTK-START [mtk04070][111205][ALPS00093395]Use logd
        logd(s);
        //MTK-END [mtk04070][111205][ALPS00093395]Use logd
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmDataConnectionTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mReregisterOnReconnectFailure=" + mReregisterOnReconnectFailure);
        pw.println(" canSetPreferApn=" + canSetPreferApn);
        pw.println(" mApnObserver=" + mApnObserver);
        pw.println(" getOverallState=" + getOverallState());
        pw.println(" mDataConnectionAsyncChannels=%s\n" + mDataConnectionAsyncChannels);
    }

    
    /// M: SCRI and Fast Dormancy Feature @{
    //MTK-START [mtk04070][111205][ALPS00093395]MTK proprietary methods/classes/receivers 
    void registerSCRIEvent(GSMPhone p) {
        if (FeatureOption.MTK_FD_SUPPORT) {
            mScriManager = new ScriManager();
            
            if(Settings.System.getInt(mGsmPhone.getContext().getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING, 0) == 1){
                mIsCallPrefer = true;
            }else{
                mIsCallPrefer = false;
            }
                                    
            mScriManager.reset();
            p.mCM.setScriResult(this, DctConstants.EVENT_SCRI_RESULT, null);  //Register with unsolicated result
            p.mSST.registerForRatRegistrants(this, DctConstants.EVENT_NW_RAT_CHANGED, null);

            IntentFilter filter = new IntentFilter();
            //Add for SCRI by MTK03594
            filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_GPRS_TRANSFER_TYPE);

            //[ALPS00098656][mtk04070]Disable Fast Dormancy when in Tethered mode
            filter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);

            // TODO: Why is this registering the phone as the receiver of the intent
            //       and not its own handler?
            p.getContext().registerReceiver(mIntentReceiverScri, filter, null, p);
               
            //[ALPS00098656][mtk04070]Disable Fast Dormancy when in Tethered mode
            /* Get current tethered mode */
            ConnectivityManager connMgr = (ConnectivityManager) mPhone.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if ((connMgr != null) && (connMgr.getTetheredIfaces() != null))
            { 
               mIsTetheredMode = (connMgr.getTetheredIfaces().length > 0);
               logd("[GsmDataConnectionTracker Constructor]mIsTetheredMode = " + mIsTetheredMode + "mChargingMode=" + mChargingMode);    
               //[New R8 modem FD]
               if((!mIsTetheredMode) && (!mChargingMode)) {
                   updateFDMDEnableStatus(true);		
               } else {
                   updateFDMDEnableStatus(false);	
               }  
            }
        }
    }
    
    //Add for SCRI design
    //Function to start SCRI polling service
    protected void startScriPoll(){        
        if(FeatureOption.MTK_FD_SUPPORT) {
            if(DBG) logd("[SCRI] startScriPoll (" + scriPollEnabled + "," + mScriManager.getScriState() + "," + mIsUmtsMode + ")");
            //[New R8 modem FD]
            int FD_MD_Enable_Mode = SystemProperties.getInt(PROPERTY_RIL_FD_MODE, 0);
            if(DBG) logd("[SCRI] startScriPoll:FD_MD_Enable_Mode:" + FD_MD_Enable_Mode);
            if(scriPollEnabled == false && mIsUmtsMode && isConnected() && FD_MD_Enable_Mode == 0) {
                if(mScriManager.getScriState() == ScriManager.STATE_NONE) {
                     scriPollEnabled = true;                     
                     mPollScriStat.run();
                     mScriManager.setPsSessionStatus(true);
                     mScriManager.setScriState(ScriManager.STATE_ACTIVATED);
               }
            }
       }
        
    }

    //Function to stop SCRI polling service
    protected void stopScriPoll()
    {                            
        if(FeatureOption.MTK_FD_SUPPORT && (mScriManager.getScriState() != ScriManager.STATE_NONE)) {
            if(DBG) logd("[SCRI]stopScriPoll");
            mScriManager.reset();
            scriPollEnabled = false;
            mScriManager.setScriState(ScriManager.STATE_NONE);
            mScriManager.setPsSessionStatus(false);
            mDataConnectionTracker.removeMessages(DctConstants.EVENT_SCRI_RETRY_TIMER);
            removeCallbacks(mPollScriStat);
        }
    }
    
    protected void handleScriResult(AsyncResult ar){
        Integer scriResult = (Integer)(ar.result);
        if(DBG) logd("[SCRI] handleScriResult :" + scriResult);
        
        if (ar.exception == null) {
            if (scriResult == ScriManager.SCRI_RESULT_REQ_SENT || 
                scriResult == ScriManager.SCRI_NO_PS_DATA_SESSION ||
                scriResult == ScriManager.SCRI_NOT_ALLOWED) {      //[ALPS00097617][mtk04070]Handle SCRI_NOT_ALLOWED event
                mScriManager.setScriState(ScriManager.STATE_ACTIVATED);
                mScriManager.setPsSessionStatus(false);
                if (scriResult == ScriManager.SCRI_RESULT_REQ_SENT || scriResult == ScriManager.SCRI_NO_PS_DATA_SESSION) {
                    if (mScriManager.mFirstESCRIRAUFollowOnProceed) {                 	
                        mScriManager.mFirstESCRIRAUFollowOnProceed = false;
                        logd("1st AT+ESCRI=1 for RAUFollowOnProceed is sent to modem successfully");
                    }    	
                }                	                
            }
            //Add by mtk01411 to handle RAU with FollowOnProceed and RRC connected scenario
            else if(scriResult == ScriManager.SCRI_RAU_ENABLED) {
                if(DBG) logd("[SCRI] RAU with FollowOnProceed: RRC in connected state,scriState=" + mScriManager.getScriState());	
            	  mScriManager.mPeriodicRAUFollowOnProceedEnable = true; 
            	  mScriManager.mFirstESCRIRAUFollowOnProceed = true; 
            
            }else{
                if (DBG) logd("[SCRI] mScriManager.retryCounter :" + mScriManager.mScriRetryCounter);
                mScriManager.setPsSessionStatus(false);
                if (mScriManager.mScriRetryCounter < ScriManager.SCRI_MAX_RETRY_COUNTER) {
                    mScriManager.setScriState(ScriManager.STATE_RETRY);

                    if(mIsScreenOn) {
                        mScriManager.mScriRetryTimer = mScriManager.mScriTriggerDataCounter * 1000;
                    } else {
                        mScriManager.mScriRetryTimer = mScriManager.mScriTriggerDataOffCounter * 1000;
                    }

                    if(mScriManager.mScriRetryTimer > ScriManager.SCRI_MAX_RETRY_TIMERS) {
                        mScriManager.mScriRetryTimer = ScriManager.SCRI_MAX_RETRY_TIMERS;
                    }

                    mScriManager.mScriRetryCounter++;
                    Message msg = mDataConnectionTracker.obtainMessage(DctConstants.EVENT_SCRI_RETRY_TIMER, null);
                    mDataConnectionTracker.sendMessageDelayed(msg, mScriManager.mScriRetryTimer);
                    logd("[SCRI] Retry counter = " + mScriManager.mScriRetryCounter + ", timeout = " + mScriManager.mScriRetryTimer);
                } else {
                    //No retry
                    mScriManager.mScriRetryCounter = 0;
                    mScriManager.setScriState(ScriManager.STATE_ACTIVATED);
                }
            }
        } else {
            mScriManager.setScriState(ScriManager.STATE_RETRY);
            Message msg = mDataConnectionTracker.obtainMessage(DctConstants.EVENT_SCRI_RETRY_TIMER, null);
            mDataConnectionTracker.sendMessageDelayed(msg, ScriManager.SCRI_MAX_RETRY_TIMERS);
        }
    }
    
    protected void sendScriCmd(boolean retry) {
        try{
            if((mScriManager.getPsSessionStatus() || retry) && 
               (mPhone.getState() == PhoneConstants.State.IDLE) && 
               mIsUmtsMode && 
               (!mIsTetheredMode) && (!mChargingMode))   //[ALPS00098656][mtk04070]Disable Fast Dormancy when in Tethered mode
            {
                logd("[SCRI] Send SCRI command:" + mIsCallPrefer + ":" + retry);
                if(!mIsScreenOn) {
                    mPhone.mCM.setScri(true, obtainMessage(DctConstants.EVENT_SCRI_CMD_RESULT));
                    mScriManager.setScriState(ScriManager.STATE_ACTIVIATING);
                }else{
                    boolean forceFlag = false;
                    
                    //Send SCRI with force flag when the data prefer is on and both sims are on
                    if(FeatureOption.MTK_GEMINI_SUPPORT && !mIsCallPrefer){
                        GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
                        for (int peerSimId=PhoneConstants.GEMINI_SIM_1; peerSimId<PhoneConstants.GEMINI_SIM_NUM; peerSimId++) {
                            if(peerSimId != mPhone.getMySimId() &&
                                    mGeminiPhone.isRadioOnGemini(peerSimId)){
                                forceFlag = true;
                                break;
                            }
                        }
                    }
                    
                    //Only for operator (not CMCC) have the chance to set forceFlag as true when SCREEN is ON 
                    forceFlag = mGsmDCTExt.getFDForceFlag(forceFlag);
                    
                    //Send SCRI with force flag as TRUE when receiving the RAU with FollowOnProceed (+ESCRI:6)
                    if (mScriManager.mFirstESCRIRAUFollowOnProceed) {
                        forceFlag = true;
                        logd("[SCRI]Screen ON: but RAUFollowOnProceed sets forceFlag as true");
                    }

                    logd("[SCRI]Screen ON: send AT+ESCRI with forceFlag=" + forceFlag);                    
                    mPhone.mCM.setScri(forceFlag, obtainMessage(DctConstants.EVENT_SCRI_CMD_RESULT));
                    mScriManager.setScriState(ScriManager.STATE_ACTIVIATING);
                                        
                }
            } else {
                logd("[SCRI] Ingore SCRI command due to (" + mScriManager.getPsSessionStatus() + ";" + mPhone.getState() + ";" + ")");
                logd("[SCRI] mIsUmtsMode = " + mIsUmtsMode);
                logd("[SCRI] mIsTetheredMode = " + mIsTetheredMode);
                    mScriManager.setScriState(ScriManager.STATE_ACTIVATED);                
                }            
        }catch(Exception e){
           e.printStackTrace();
        }
    }
    //ADD_END for SCRI
    
    BroadcastReceiver mIntentReceiverScri = new BroadcastReceiver ()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            /*
               Some actions are handled in DataConnectionTracker.java
            */
            if(action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)){  //add by MTK03594 for SCRI
                if (FeatureOption.MTK_GEMINI_SUPPORT && FeatureOption.MTK_FD_SUPPORT) {
                    //Check SIM2 state during data prefer condition
                }
            } else if(action.equals(TelephonyIntents.ACTION_GPRS_TRANSFER_TYPE)) {
                int gprsTransferType = intent.getIntExtra(Phone.GEMINI_GPRS_TRANSFER_TYPE, 0);
                logd("GPRS Transfer type:" + gprsTransferType);
                if(gprsTransferType == 1) {
                    mIsCallPrefer = true;
                } else {
                    mIsCallPrefer = false;
                }
            } else if(action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
            	  //[ALPS00098656][mtk04070]Disable Fast Dormancy when in Tethered mode
            	  logd("Received ConnectivityManager.ACTION_TETHER_STATE_CHANGED");
                ArrayList<String> active = intent.getStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER);            	
                mIsTetheredMode = ((active != null) && (active.size() > 0));
                logd("[TETHER_STATE_CHANGED]mIsTetheredMode = " + mIsTetheredMode + "mChargingMode=" + mChargingMode);
                //[New R8 modem FD]
                if((!mIsTetheredMode) && (!mChargingMode)) {
                    updateFDMDEnableStatus(true);		
                } else {
                    updateFDMDEnableStatus(false);	
                }
            }
        }/* End of onReceive */
    };

    private class ScriManager{
        protected static final boolean DBG = true;
        
        static public final int STATE_NONE=0;
        static public final int STATE_ACTIVIATING=1;
        static public final int STATE_ACTIVATED=2;
        static public final int STATE_RETRY=3;

        static public final int SCRI_NONE = 0;
        static public final int SCRI_3G = 1;
        static public final int SCRI_2G = 2;

        static public final int SCRI_RESULT_REQ_SENT = 0;
        static public final int SCRI_CS_SESSION_ONGOING = 1;
        static public final int SCRI_PS_SIGNALLING_ONGOING = 2;
        static public final int SCRI_NO_PS_DATA_SESSION = 3;
        static public final int SCRI_REQ_NOT_SENT = 4; 
        //[ALPS00097617][mtk04070]Refine handleScriResult function to handle SCRI_NOT_ALLOWED 
        static public final int SCRI_NOT_ALLOWED = 5; 
        //Add by mtk01411 to handle RAU with FollowOnProceed and RRC connected scenario
        static public final int SCRI_RAU_ENABLED = 6; 
        
        static public final int SCRI_MAX_RETRY_COUNTER = 3;
        static public final int SCRI_MAX_RETRY_TIMERS = 30 * 1000;
        
        public int mScriGuardTimer;
        public int mScriPollTimer;
        public int mScriTriggerDataCounter;
        public int mScriTriggerDataOffCounter;
        public int mScriRetryTimer;
        public int mScriRetryCounter;

        //Add by mtk01411 to handle RAU with FollowOnProceed and RRC connected scenario
        public boolean mPeriodicRAUFollowOnProceedEnable = false;
        public boolean mFirstESCRIRAUFollowOnProceed = false;
        private boolean mScriNeeded;
        private boolean mPsSession;
        private boolean mGuardTimerExpired;
        private int mScriState;
        private int mScriDataCounter;
        private int mScriAddCounter;
        private int mNwType;
        
        protected final String LOG_TAG = "GSM";
            
        public ScriManager(){
            mScriGuardTimer = 0;
            mScriPollTimer = 0;
            mScriDataCounter = 0;
            mScriRetryTimer = 0;            
            mScriAddCounter = 0;
            mScriTriggerDataCounter = 0;
            mScriTriggerDataOffCounter = 0;
            mScriRetryCounter = 0;
            mPsSession = false;
            
            mScriNeeded = false;
            mGuardTimerExpired = false;
            mScriState = STATE_NONE;

            mNwType = SCRI_NONE;
        }

        public void setScriTimer()
        {
            String  str = null;
	          Integer val = 0;  

            try {
                //Get scri guard timer
                str = SystemProperties.get("persist.radio.fd.guard.timer", "60");
				        val = Integer.parseInt(str);
                if(val < 5 || val > 3600) val = 60;
                mScriGuardTimer = val * 1000;            

                //Get scri poll timer
                str = SystemProperties.get("persist.radio.fd.poll.timer", "5");
				        val = Integer.parseInt(str);
                if(val <= 0 || val > 600) val = 5;
                mScriAddCounter = val;
                mScriPollTimer = val * 1000;

                //Get scri data counter for screen on
                str = SystemProperties.get("persist.radio.fd.counter", "20");
                val = Integer.parseInt(str);
                if(val < 5 || val > 3600) val = 20;
                mScriTriggerDataCounter = val;
            
                //Get scri data counter for screen off
                str = SystemProperties.get("persist.radio.fd.off.counter", "20");
                val = Integer.parseInt(str);
                if(val < 5 || val > 3600) val = 20;
                mScriTriggerDataOffCounter = val;

                //Get scri retry timer
                str = SystemProperties.get("persist.radio.fd.retry.timer", "20");
				        val = Integer.parseInt(str);
                if(val < 5 || val > 600) val = 20;
                mScriRetryTimer = val * 1000;
            
                if (DBG) Log.d(LOG_TAG, "[SCRI] init value (" + mScriGuardTimer + "," + mScriPollTimer + ","+ mScriTriggerDataCounter + "," + mScriTriggerDataOffCounter + "," + mScriRetryTimer + ")");
            } catch (Exception e) {
                        e.printStackTrace();
                        mScriGuardTimer = 60 * 1000;
                        mScriPollTimer = 5 * 1000;
                        mScriTriggerDataCounter = 20;
                        mScriTriggerDataOffCounter = 20;
                        mScriRetryTimer = 20 * 1000;
                        mScriAddCounter = 5;
            }/* End of try-catch */
        }

        public void reset(){
            mScriNeeded = false;
            mGuardTimerExpired = false;
            mPsSession = false;
            mScriRetryCounter = 0;
            mScriState = STATE_NONE;
            mScriDataCounter = 0;
            mScriAddCounter = mScriPollTimer/1000;
            setScriTimer();
        }
            
        public void setScriState(int scriState){
            mScriState = scriState;
        }

        public int getScriState(){
            return mScriState;
        }

        public void setPsSessionStatus(boolean hasPsSession) {
            if(hasPsSession) {
                mScriRetryCounter = 0;
            }
           mPsSession = hasPsSession;
        }

        public boolean getPsSessionStatus() {
           return mPsSession;
        }
        
        public void setScriDataCount(boolean reset){
            if(reset == false){
                mScriDataCounter+=mScriAddCounter;
            }else{
                mScriDataCounter = 0;
            }
            if(DBG) Log.d(LOG_TAG, "[SCRI]setScriDataCount:" + mScriDataCounter);
        }

        public boolean isPollTimerTrigger(boolean isScreenOn) {
            if (isScreenOn) {
               return mScriDataCounter >= mScriTriggerDataCounter;
            } else {
               return mScriDataCounter >= mScriTriggerDataOffCounter;
            }
        }

        public int getScriNwType(int networktype){
            if(DBG) Log.d(LOG_TAG, "[SCRI]getScriNwType:" + networktype);
            int nwType = 0;
            
            if(networktype >= TelephonyManager.NETWORK_TYPE_UMTS){
                nwType = SCRI_3G;
            }else if(networktype == TelephonyManager.NETWORK_TYPE_GPRS || networktype == TelephonyManager.NETWORK_TYPE_EDGE){
                nwType = SCRI_2G;
            }else{
                nwType = SCRI_NONE;
            }

            //Only consider 2G -> 3G & 3G -> 2G
            if(nwType != SCRI_NONE && mNwType != nwType)
            {
               mNwType = nwType;
            }else{
               nwType = SCRI_NONE;
            }
            
            return nwType;
        }

        public boolean isDataTransmitting() {
            long deltaTx, deltaRx;
            long preTxPkts = scriTxPkts, preRxPkts = scriRxPkts;

           if(PhoneFactory.isDualTalkMode()) {
               TxRxSum curTxRxSum = new TxRxSum();
   
               curTxRxSum.updateTxRxSum();
               scriTxPkts = curTxRxSum.txPkts;
               scriRxPkts = curTxRxSum.rxPkts;
           } else {
               scriTxPkts = TrafficStats.getMobileTxPackets();
               scriRxPkts = TrafficStats.getMobileRxPackets();
           }

            Log.d(LOG_TAG, "[SCRI]tx: " + preTxPkts + " ==> " + scriTxPkts);
            Log.d(LOG_TAG, "[SCRI]rx  " + preRxPkts + " ==> " + scriRxPkts);

            deltaTx = scriTxPkts - preTxPkts;
            deltaRx = scriRxPkts - preRxPkts;
            Log.d(LOG_TAG, "[SCRI]delta rx " + deltaRx + " tx " + deltaTx);

            return (deltaTx > 0 || deltaRx > 0);
        }
    }

    private Runnable mPollScriStat = new Runnable(){
        public void run() {
            boolean resetFlag = false;

            resetFlag = mScriManager.isDataTransmitting();

            //Add by mtk01411 to handle RAU with FollowOnProceed and RRC connected scenario
            if (mScriManager.mPeriodicRAUFollowOnProceedEnable) {
                logd("[SCRI] Detect RAU FollowOnProceed:Force to let resetFlag as true (regard PS session exist)");
                resetFlag = true;
                mScriManager.mPeriodicRAUFollowOnProceedEnable = false;	
            }

            if (mScriManager.getScriState() == ScriManager.STATE_ACTIVATED || mScriManager.getScriState() == ScriManager.STATE_RETRY) {
                logd("[SCRI]act:" + resetFlag);
            
                if (resetFlag){
                    mScriManager.setPsSessionStatus(true);
                    //Disable retry command due to data transfer
                    if (mScriManager.getScriState() == ScriManager.STATE_RETRY) {
                        mDataConnectionTracker.removeMessages(DctConstants.EVENT_SCRI_RETRY_TIMER);
                        mScriManager.setScriState(ScriManager.STATE_ACTIVATED);
                    }
                }
                
                mScriManager.setScriDataCount(resetFlag);
                if (mScriManager.isPollTimerTrigger(mIsScreenOn))
                {
                    mScriManager.setScriDataCount(true);      
                    sendScriCmd(false);
                }
            }

            logd("mPollScriStat");
            if (scriPollEnabled) {
               mDataConnectionTracker.postDelayed(this, mScriManager.mScriPollTimer);
           }
        }/* End of run() */

    };
    /// @}

    /* Add by vendor for Multiple PDP Context used in notify overall data state scenario */
    @Override
    public String getActiveApnType() {
        // TODO Auto-generated method stub
        /* Note by mtk01411: Currently, this API is invoked by DefaultPhoneNotifier::notifyDataConnection(sender, reason) */
        /* => Without specifying the apnType: In this case, it means that report the overall data state */
        /* Return the null for apnType to query overall data state */
        return null;
    }

    private boolean isCuImsi(String imsi){

        if(imsi != null){
           int mcc = Integer.parseInt(imsi.substring(0,3));
           int mnc = Integer.parseInt(imsi.substring(3,5));
                  
           logd("mcc mnc:" + mcc +":"+ mnc);

            if(mcc == 460 && mnc == 01){
               return true;
            }
            
            if (mcc == 001) {
                return true;
            }
       }

       return false;
  }

    private ArrayList<ApnSetting> fetchDMApn() {
        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";
        /* Add by mtk01411 */
        logd("fetchDMApn():operator=" + operator);
        if (operator != null) {
            String selection = "numeric = '" + operator + "'";
            Cursor dmCursor = null;
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (mPhone.getMySimId() == PhoneConstants.GEMINI_SIM_1) {
                    dmCursor = mPhone.getContext().getContentResolver().query(
                                   Telephony.Carriers.CONTENT_URI_DM, null, selection, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
                } else {
                    dmCursor = mPhone.getContext().getContentResolver().query(
                                   Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM, null, selection, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
                }

            } else {
                dmCursor = mPhone.getContext().getContentResolver().query(
                               Telephony.Carriers.CONTENT_URI_DM, null, selection, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
            }

            if (dmCursor != null) {
                try {
                    /* Add by mtk01411 */
                    logd("fetchDMApn(): dmCursor_count=" + Integer.toString(dmCursor.getCount()));
                    if (dmCursor.getCount() > 0) {
                        return createApnList(dmCursor);
                    }
                } finally {
                    if (dmCursor != null) {
                        dmCursor.close();
                    }
                }
            }
        }
        return new ArrayList<ApnSetting>();
    }
    
    protected void logd(String s) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.d(LOG_TAG, "[GDCT][simId" + mLogSimId + "]"+ s);
        } else {
            Log.d(LOG_TAG, "[GDCT] " + s);
        }
    }

    protected void logi(String s) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.i(LOG_TAG, "[GDCT][simId" + mLogSimId + "]"+ s);
        } else {
            Log.i(LOG_TAG, "[GDCT] " + s);
        }
    }

    protected void logw(String s) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.w(LOG_TAG, "[GDCT][simId" + mLogSimId + "]"+ s);
        } else {
            Log.w(LOG_TAG, "[GDCT] " + s);
        }
    }

    protected void loge(String s) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.e(LOG_TAG, "[GDCT][simId" + mLogSimId + "]"+ s);
        } else {
            Log.e(LOG_TAG, "[GDCT] " + s);
        }
    }

    public boolean isNotDefaultTypeDataEnabled() {
        synchronized (mDataEnabledLock) {
            if (!(mInternalDataEnabled && sPolicyDataEnabled))
                return false;
            for (ApnContext apnContext : mApnContexts.values()) {
                // Make sure we dont have a context that going down
                // and is explicitly disabled.
                if (!PhoneConstants.APN_TYPE_DEFAULT.equals(apnContext.getApnType()) && isDataAllowed(apnContext)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* Add by vendor for Multiple PDP Context */
    private boolean isSameProxy(ApnSetting apn1, ApnSetting apn2){
        if (apn1 == null || apn2 == null){
            return false;
        }
        String proxy1;
        if (apn1.canHandleType(PhoneConstants.APN_TYPE_MMS)){
            proxy1 = apn1.mmsProxy;
        }else{
            proxy1 = apn1.proxy;
        }
        String proxy2;
        if (apn2.canHandleType(PhoneConstants.APN_TYPE_MMS)){
            proxy2 = apn2.mmsProxy;
        }else{
            proxy2 = apn2.proxy;
        }
        /* Fix NULL Pointer Exception problem: proxy1 may be null */ 
        if (proxy1 != null && proxy2 != null && !proxy1.equals("") && !proxy2.equals(""))
            return proxy1.equalsIgnoreCase(proxy2);
        else {
            logd("isSameProxy():proxy1=" + proxy1 + ",proxy2=" + proxy2);
            return false;
        }
    }	

    private boolean enableWaitingApn() {
        boolean ret = false;
        Iterator<String> iterWaitingApn = mWaitingApnList.iterator();

        if (DBG) logd("Reconnect waiting APNs if have.");
        while (iterWaitingApn.hasNext()) {
             enableApnType(iterWaitingApn.next());
             ret = true;
        }
        mWaitingApnList.clear();            
        return ret;
    }

    private void clearWaitingApn() {
        Iterator<String> iterWaitingApn = mWaitingApnList.iterator();

        if (DBG) logd("Reconnect waiting APNs if have.");
        while (iterWaitingApn.hasNext()) {
             mPhone.notifyDataConnection(Phone.REASON_APN_FAILED, iterWaitingApn.next());
        }
        mWaitingApnList.clear();  
    }
    
    public void gprsDetachResetAPN() {
        // To ensure all context are reset since GPRS detached.
        for (ApnContext apnContext : mApnContexts.values()) {
            if (DBG) logd("Reset APN since GPRS detached [" + apnContext.getApnType() + "]");
            DataConnection dataConnection = apnContext.getDataConnection();
            if (dataConnection != null) {
                DctConstants.State state = apnContext.getState();
                if (state == DctConstants.State.CONNECTED || state == DctConstants.State.CONNECTING) {
                    Message msg = obtainMessage(DctConstants.EVENT_DISCONNECT_DONE, apnContext);
                    dataConnection.tearDown(Phone.REASON_DATA_DETACHED, msg);
                }
            }
            apnContext.setState(DctConstants.State.IDLE);
            apnContext.setApnSetting(null);
            apnContext.setDataConnection(null);
            apnContext.setDataConnectionAc(null);
        }
    }


    private boolean isDomesticRoaming() {
        boolean roaming = mPhone.getServiceState().getRoaming();
        String operatorNumeric = mPhone.getServiceState().getOperatorNumeric();
        String imsi = mGsmPhone.getSubscriberId();        
        boolean sameMcc = false;
        try {
            sameMcc = imsi.substring(0, 3).equals(operatorNumeric.substring(0, 3));
        } catch (Exception e) {
        }
        
        if (DBG) logd("getDataOnRoamingEnabled(): roaming=" + roaming + ", sameMcc=" + sameMcc);

        return (roaming && sameMcc);
    }

    @Override
    public boolean getDataOnRoamingEnabled() {
        return (super.getDataOnRoamingEnabled() ||
                (isDomesticRoaming() && mGsmDCTExt.isDomesticRoamingEnabled()));    
    }

    @Override
    protected boolean isDataAllowedAsOff(String apnType) {
        return mGsmDCTExt.isDataAllowedAsOff(apnType);
    }	

    public void abortDataConnection() {
        if (DBG) logd("abortDataConnection()");
        for (DataConnection dc : mDataConnections.values()) {
            dc.removeConnectMsg();
        }
    }
    //MTK-END [mtk04070][111205][ALPS00093395]MTK proprietary methods/classes/receivers

    //[New R8 modem FD]
    public int setFDTimerValue(String newTimerValue[], Message onComplete) {
        return mFDTimer.setFDTimerValue(newTimerValue, onComplete);
    }

    //[New R8 modem FD]
    public String[] getFDTimerValue() {
        return mFDTimer.getFDTimerValue();
    } 

    //[New R8 modem FD]
    public void updateFDMDEnableStatus(boolean enabled) {
        int FD_MD_Enable_Mode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        int FDSimID = SystemProperties.getInt("gsm.3gswitch", 1) == 2 ? PhoneConstants.GEMINI_SIM_2 : PhoneConstants.GEMINI_SIM_1;
        if (DBG) logd("updateFDMDEnableStatus():enabled=" + enabled + ",FD_MD_Enable_Mode=" + FD_MD_Enable_Mode + ", 3gSimID=" + FDSimID);
        if (FD_MD_Enable_Mode == 1 && FeatureOption.MTK_FD_SUPPORT && mPhone.getMySimId() == FDSimID) {
            if (enabled) { 
                mPhone.mCM.setFDMode(FDModeType.ENABLE_MD_FD.ordinal(), -1, -1, obtainMessage(DctConstants.EVENT_FD_MODE_SET));
            } else {
                mPhone.mCM.setFDMode(FDModeType.DISABLE_MD_FD.ordinal(), -1, -1, obtainMessage(DctConstants.EVENT_FD_MODE_SET)); 
            }            
        }			
    }	

    // M: For multiple SIM support to check is any connection active
    @Override
    public boolean isAllConnectionInactive() {
        boolean retValue = true;
        for (DataConnectionAc dcac : mDataConnectionAsyncChannels.values()) {
            if (!dcac.isInactiveSync()) {
                if (DBG) logd("Found active DC=" + dcac);
                retValue = false;
                break;
            }
        }
        if (DBG) logd("isAllConnectionInactive(): retValue=" + retValue);
        return retValue;		
    }
}
