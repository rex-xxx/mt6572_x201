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
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;

import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.ims.IsimRecords;
import com.android.internal.telephony.test.SimulatedRadioControl;
//MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
import com.android.internal.telephony.gsm.UsimServiceTable;
//MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

//MTK-START [mtk04070][111117][ALPS00093395]MTK used
import com.android.internal.telephony.gsm.GsmDataConnection;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
//MTK-END [mtk04070][111117][ALPS00093395]MTK used
import com.android.internal.telephony.PhoneConstants.*; // ????
import java.util.List;

/**
 * Internal interface used to control the phone; SDK developers cannot
 * obtain this interface.
 *
 * {@hide}
 *
 */
public interface Phone {

    /** used to enable additional debug messages */
    static final boolean DEBUG_PHONE = true;

    public enum DataActivityState {
        /**
         * The state of a data activity.
         * <ul>
         * <li>NONE = No traffic</li>
         * <li>DATAIN = Receiving IP ppp traffic</li>
         * <li>DATAOUT = Sending IP ppp traffic</li>
         * <li>DATAINANDOUT = Both receiving and sending IP ppp traffic</li>
         * <li>DORMANT = The data connection is still active,
                                     but physical link is down</li>
         * </ul>
         */
        NONE, DATAIN, DATAOUT, DATAINANDOUT, DORMANT;
    };

    enum SuppService {
      UNKNOWN, SWITCH, SEPARATE, TRANSFER, CONFERENCE, REJECT, HANGUP;
    };

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    enum IccServiceStatus {
        NOT_EXIST_IN_SIM,
	 NOT_EXIST_IN_USIM,
	 ACTIVATED,
	 INACTIVATED,
	 UNKNOWN;
    };

    enum IccService {
        CHV1_DISABLE_FUNCTION ,   //0
        SPN,                                       
        PNN,                                        //PLMN Network Name
        OPL,                                         //Operator PLMN List
        MWIS,                                      //Message Waiting Indication Status
        CFIS,                                        //Call Forwarding Indication Status   5
        SPDI,                                        //Service Provider Display Information
        EPLMN,                                      //Equivalent HPLMN
        UNSUPPORTED_SERVICE;                          //8		

        public int getIndex() {
            int nIndex = -1;
	     switch(this) {
		case CHV1_DISABLE_FUNCTION:
		 	nIndex = 0;
			break;
		case SPN:
			nIndex = 1;
			break;
		case PNN:
			nIndex = 2;
			break;
		case OPL:
			nIndex = 3;
			break;
		case MWIS:
			nIndex = 4;
			break;
		case CFIS:
			nIndex = 5;
			break;				
		case SPDI:
			nIndex = 6;
			break;			
		case EPLMN:
			nIndex = 7;
			break;		
		case UNSUPPORTED_SERVICE:
			nIndex = 8;
			break;	
		default:
			break;
	     }
            return nIndex;
        }
    };
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    /* Add by vendor: Multiple PDP Context for MobileDataStateTracker usage */ 
    static final String DISCONNECT_DATA_FLAG ="disconnectPdpFlag";
    /* vt start */
    static final String IS_VT_CALL = "isVtCall";
    /* vt end */
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added
	

    // "Features" accessible through the connectivity manager
    static final String FEATURE_ENABLE_MMS = "enableMMS";
    static final String FEATURE_ENABLE_SUPL = "enableSUPL";
    static final String FEATURE_ENABLE_DUN = "enableDUN";
    static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";
    static final String FEATURE_ENABLE_DUN_ALWAYS = "enableDUNAlways";
    static final String FEATURE_ENABLE_FOTA = "enableFOTA";
    static final String FEATURE_ENABLE_IMS = "enableIMS";
    static final String FEATURE_ENABLE_CBS = "enableCBS";

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    static final String FEATURE_ENABLE_DM = "enableDM";
    static final String FEATURE_ENABLE_WAP = "enableWAP";
    static final String FEATURE_ENABLE_NET = "enableNET";
    static final String FEATURE_ENABLE_CMMAIL = "enableCMMAIL";
    static final String FEATURE_ENABLE_RCSE = "enableRCSE";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added


    
    /* Return codes for enableDataConnectivity() and disableDataConnectivity() */
    static final int ENABLE_DATA_CONNECTIVITY_INVALID_SIM_ID = 0;
    static final int ENABLE_DATA_CONNECTIVITY_STARTED = 1;
    static final int ENABLE_DATA_CONNECTIVITY_SUCCESS = 2;
    static final int ENABLE_DATA_CONNECTIVITY_FAILED_THIS_SIM_STILL_DETACHING = 3;
    static final int ENABLE_DATA_CONNECTIVITY_INVALID_STATE = 4;
    static final int DISABLE_DATA_CONNECTIVITY_INVALID_SIM_ID = 5;
    static final int DISABLE_DATA_CONNECTIVITY_STARTED = 6;
    static final int DISABLE_DATA_CONNECTIVITY_SUCCESS = 7;
    static final int DISABLE_DATA_CONNECTIVITY_INVALID_STATE = 8;
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added


    /**
     * Optional reasons for disconnect and connect
     */
    static final String REASON_ROAMING_ON = "roamingOn";
    static final String REASON_ROAMING_OFF = "roamingOff";
    static final String REASON_DATA_DISABLED = "dataDisabled";
    static final String REASON_DATA_ENABLED = "dataEnabled";
    static final String REASON_DATA_ATTACHED = "dataAttached";
    static final String REASON_DATA_DETACHED = "dataDetached";
    static final String REASON_CDMA_DATA_ATTACHED = "cdmaDataAttached";
    static final String REASON_CDMA_DATA_DETACHED = "cdmaDataDetached";
    static final String REASON_APN_CHANGED = "apnChanged";
    static final String REASON_APN_SWITCHED = "apnSwitched";
    static final String REASON_APN_FAILED = "apnFailed";
    static final String REASON_RESTORE_DEFAULT_APN = "restoreDefaultApn";
    static final String REASON_RADIO_TURNED_OFF = "radioTurnedOff";
    static final String REASON_PDP_RESET = "pdpReset";
    static final String REASON_VOICE_CALL_ENDED = "2GVoiceCallEnded";
    static final String REASON_VOICE_CALL_STARTED = "2GVoiceCallStarted";
    static final String REASON_PS_RESTRICT_ENABLED = "psRestrictEnabled";
    static final String REASON_PS_RESTRICT_DISABLED = "psRestrictDisabled";
    static final String REASON_SIM_LOADED = "simLoaded";
    static final String REASON_NW_TYPE_CHANGED = "nwTypeChanged";
    static final String REASON_DATA_DEPENDENCY_MET = "dependencyMet";
    static final String REASON_DATA_DEPENDENCY_UNMET = "dependencyUnmet";
    static final String REASON_LINK_PROPERTIES_CHANGED = "linkPropertiesChanged";

    // Used for band mode selection methods
    static final int BM_UNSPECIFIED = 0; // selected by baseband automatically
    static final int BM_EURO_BAND   = 1; // GSM-900 / DCS-1800 / WCDMA-IMT-2000
    static final int BM_US_BAND     = 2; // GSM-850 / PCS-1900 / WCDMA-850 / WCDMA-PCS-1900
    static final int BM_JPN_BAND    = 3; // WCDMA-800 / WCDMA-IMT-2000
    static final int BM_AUS_BAND    = 4; // GSM-900 / DCS-1800 / WCDMA-850 / WCDMA-IMT-2000
    static final int BM_AUS2_BAND   = 5; // GSM-900 / DCS-1800 / WCDMA-850
    static final int BM_BOUNDARY    = 6; // upper band boundary

    // Used for preferred network type
    // Note NT_* substitute RILConstants.NETWORK_MODE_* above the Phone
    int NT_MODE_WCDMA_PREF   = RILConstants.NETWORK_MODE_WCDMA_PREF;
    int NT_MODE_GSM_ONLY     = RILConstants.NETWORK_MODE_GSM_ONLY;
    int NT_MODE_WCDMA_ONLY   = RILConstants.NETWORK_MODE_WCDMA_ONLY;
    int NT_MODE_GSM_UMTS     = RILConstants.NETWORK_MODE_GSM_UMTS;

    int NT_MODE_CDMA         = RILConstants.NETWORK_MODE_CDMA;

    int NT_MODE_CDMA_NO_EVDO = RILConstants.NETWORK_MODE_CDMA_NO_EVDO;
    int NT_MODE_EVDO_NO_CDMA = RILConstants.NETWORK_MODE_EVDO_NO_CDMA;
    int NT_MODE_GLOBAL       = RILConstants.NETWORK_MODE_GLOBAL;

    int NT_MODE_LTE_ONLY     = RILConstants.NETWORK_MODE_LTE_ONLY;
    int PREFERRED_NT_MODE    = RILConstants.PREFERRED_NETWORK_MODE;

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    int NT_MODE_GEMINI       = RILConstants.NETWORK_MODE_GEMINI;
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added


    // Used for CDMA roaming mode
    static final int CDMA_RM_HOME        = 0;  // Home Networks only, as defined in PRL
    static final int CDMA_RM_AFFILIATED  = 1;  // Roaming an Affiliated networks, as defined in PRL
    static final int CDMA_RM_ANY         = 2;  // Roaming on Any Network, as defined in PRL

    // Used for CDMA subscription mode
    static final int CDMA_SUBSCRIPTION_UNKNOWN  =-1; // Unknown
    static final int CDMA_SUBSCRIPTION_RUIM_SIM = 0; // RUIM/SIM (default)
    static final int CDMA_SUBSCRIPTION_NV       = 1; // NV -> non-volatile memory

    static final int PREFERRED_CDMA_SUBSCRIPTION = CDMA_SUBSCRIPTION_NV;

    static final int TTY_MODE_OFF = 0;
    static final int TTY_MODE_FULL = 1;
    static final int TTY_MODE_HCO = 2;
    static final int TTY_MODE_VCO = 3;

     /**
     * CDMA OTA PROVISION STATUS, the same as RIL_CDMA_OTA_Status in ril.h
     */

    public static final int CDMA_OTA_PROVISION_STATUS_SPL_UNLOCKED = 0;
    public static final int CDMA_OTA_PROVISION_STATUS_SPC_RETRIES_EXCEEDED = 1;
    public static final int CDMA_OTA_PROVISION_STATUS_A_KEY_EXCHANGED = 2;
    public static final int CDMA_OTA_PROVISION_STATUS_SSD_UPDATED = 3;
    public static final int CDMA_OTA_PROVISION_STATUS_NAM_DOWNLOADED = 4;
    public static final int CDMA_OTA_PROVISION_STATUS_MDN_DOWNLOADED = 5;
    public static final int CDMA_OTA_PROVISION_STATUS_IMSI_DOWNLOADED = 6;
    public static final int CDMA_OTA_PROVISION_STATUS_PRL_DOWNLOADED = 7;
    public static final int CDMA_OTA_PROVISION_STATUS_COMMITTED = 8;
    public static final int CDMA_OTA_PROVISION_STATUS_OTAPA_STARTED = 9;
    public static final int CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED = 10;
    public static final int CDMA_OTA_PROVISION_STATUS_OTAPA_ABORTED = 11;

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    public static final String GEMINI_DEFAULT_SIM_MODE = "persist.radio.default_sim_mode";

    public static final String GEMINI_GPRS_TRANSFER_TYPE = "gemini.gprs.transfer.type";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //via support start
    //for uim status
    static final int UIM_STATUS_NO_CARD_INSERTED = 0; 
    static final int UIM_STATUS_CARD_INSERTED = 1; 
    //via support end

    /**
     * Get the current ServiceState. Use
     * <code>registerForServiceStateChanged</code> to be informed of
     * updates.
     */
    ServiceState getServiceState();

    /**
     * Get the current CellLocation.
     */
    CellLocation getCellLocation();

    /**
     * @return all available cell information or null if none.
     */
    public List<CellInfo> getAllCellInfo();

    /**
     * Get the current for the default apn DataState. No change notification
     * exists at this interface -- use
     * {@link android.telephony.PhoneStateListener} instead.
     */
    DataState getDataConnectionState();

    /**
     * Get the current DataState. No change notification exists at this
     * interface -- use
     * {@link android.telephony.PhoneStateListener} instead.
     * @param apnType specify for which apn to get connection state info.
     */
    DataState getDataConnectionState(String apnType);

    /**
     * Get the current DataActivityState. No change notification exists at this
     * interface -- use
     * {@link android.telephony.TelephonyManager} instead.
     */
    DataActivityState getDataActivityState();

    /**
     * Gets the context for the phone, as set at initialization time.
     */
    Context getContext();

    /**
     * Disables the DNS check (i.e., allows "0.0.0.0").
     * Useful for lab testing environment.
     * @param b true disables the check, false enables.
     */
    void disableDnsCheck(boolean b);

    /**
     * Returns true if the DNS check is currently disabled.
     */
    boolean isDnsCheckDisabled();

    /**
     * Get current coarse-grained voice call state.
     * Use {@link #registerForPreciseCallStateChanged(Handler, int, Object)
     * registerForPreciseCallStateChanged()} for change notification. <p>
     * If the phone has an active call and call waiting occurs,
     * then the phone state is RINGING not OFFHOOK
     * <strong>Note:</strong>
     * This registration point provides notification of finer-grained
     * changes.<p>
     *
     */
    State getState();

    /**
     * Returns a string identifier for this phone interface for parties
     *  outside the phone app process.
     *  @return The string name.
     */
    String getPhoneName();

    /**
     * Return a numerical identifier for the phone radio interface.
     * @return PHONE_TYPE_XXX as defined above.
     */
    int getPhoneType();

    /**
     * Returns an array of string identifiers for the APN types serviced by the
     * currently active.
     *  @return The string array will always return at least one entry, Phone.APN_TYPE_DEFAULT.
     * TODO: Revisit if we always should return at least one entry.
     */
    String[] getActiveApnTypes();

    /**
     * Returns string for the active APN host.
     *  @return type as a string or null if none.
     */
    String getActiveApnHost(String apnType);

    /**
     * Return the LinkProperties for the named apn or null if not available
     */
    LinkProperties getLinkProperties(String apnType);

    /**
     * Return the LinkCapabilities
     */
    LinkCapabilities getLinkCapabilities(String apnType);

    /**
     * Get current signal strength. No change notification available on this
     * interface. Use <code>PhoneStateNotifier</code> or an equivalent.
     * An ASU is 0-31 or -1 if unknown (for GSM, dBm = -113 - 2 * asu).
     * The following special values are defined:</p>
     * <ul><li>0 means "-113 dBm or less".</li>
     * <li>31 means "-51 dBm or greater".</li></ul>
     *
     * @return Current signal strength as SignalStrength
     */
    SignalStrength getSignalStrength();

    /**
     * Notifies when a previously untracked non-ringing/waiting connection has appeared.
     * This is likely due to some other entity (eg, SIM card application) initiating a call.
     */
    void registerForUnknownConnection(Handler h, int what, Object obj);

    /**
     * Unregisters for unknown connection notifications.
     */
    void unregisterForUnknownConnection(Handler h);

    /**
     * Register for getting notifications for change in the Call State {@link Call.State}
     * This is called PreciseCallState because the call state is more precise than the
     * {@link Phone.State} which can be obtained using the {@link PhoneStateListener}
     *
     * Resulting events will have an AsyncResult in <code>Message.obj</code>.
     * AsyncResult.userData will be set to the obj argument here.
     * The <em>h</em> parameter is held only by a weak reference.
     */
    void registerForPreciseCallStateChanged(Handler h, int what, Object obj);

    /**
     * Unregisters for voice call state change notifications.
     * Extraneous calls are tolerated silently.
     */
    void unregisterForPreciseCallStateChanged(Handler h);


    /**
     * Notifies when a new ringing or waiting connection has appeared.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     *  Please check Connection.isRinging() to make sure the Connection
     *  has not dropped since this message was posted.
     *  If Connection.isRinging() is true, then
     *   Connection.getCall() == Phone.getRingingCall()
     */
    void registerForNewRingingConnection(Handler h, int what, Object obj);

    /**
     * Unregisters for new ringing connection notification.
     * Extraneous calls are tolerated silently
     */

    void unregisterForNewRingingConnection(Handler h);

    /**
     * Notifies when an incoming call rings.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     */
    void registerForIncomingRing(Handler h, int what, Object obj);

    /**
     * Unregisters for ring notification.
     * Extraneous calls are tolerated silently
     */

    void unregisterForIncomingRing(Handler h);

    /**
     * Notifies when out-band ringback tone is needed.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = boolean, true to start play ringback tone
     *                       and false to stop. <p>
     */
    void registerForRingbackTone(Handler h, int what, Object obj);

    /**
     * Unregisters for ringback tone notification.
     */

    void unregisterForRingbackTone(Handler h);

    /**
     * Registers the handler to reset the uplink mute state to get
     * uplink audio.
     */
    void registerForResendIncallMute(Handler h, int what, Object obj);

    /**
     * Unregisters for resend incall mute notifications.
     */
    void unregisterForResendIncallMute(Handler h);

    /**
     * Notifies when a voice connection has disconnected, either due to local
     * or remote hangup or error.
     *
     *  Messages received from this will have the following members:<p>
     *  <ul><li>Message.obj will be an AsyncResult</li>
     *  <li>AsyncResult.userObj = obj</li>
     *  <li>AsyncResult.result = a Connection object that is
     *  no longer connected.</li></ul>
     */
    void registerForDisconnect(Handler h, int what, Object obj);

    /**
     * Unregisters for voice disconnection notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForDisconnect(Handler h);


    /**
     * Register for notifications of initiation of a new MMI code request.
     * MMI codes for GSM are discussed in 3GPP TS 22.030.<p>
     *
     * Example: If Phone.dial is called with "*#31#", then the app will
     * be notified here.<p>
     *
     * The returned <code>Message.obj</code> will contain an AsyncResult.
     *
     * <code>obj.result</code> will be an "MmiCode" object.
     */
    void registerForMmiInitiate(Handler h, int what, Object obj);

    /**
     * Unregisters for new MMI initiate notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForMmiInitiate(Handler h);

    /**
     * Register for notifications that an MMI request has completed
     * its network activity and is in its final state. This may mean a state
     * of COMPLETE, FAILED, or CANCELLED.
     *
     * <code>Message.obj</code> will contain an AsyncResult.
     * <code>obj.result</code> will be an "MmiCode" object
     */
    void registerForMmiComplete(Handler h, int what, Object obj);

    /**
     * Unregisters for MMI complete notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForMmiComplete(Handler h);

    /**
     * Registration point for Ecm timer reset
     * @param h handler to notify
     * @param what user-defined message code
     * @param obj placed in Message.obj
     */
    public void registerForEcmTimerReset(Handler h, int what, Object obj);

    /**
     * Unregister for notification for Ecm timer reset
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForEcmTimerReset(Handler h);

    /**
     * Returns a list of MMI codes that are pending. (They have initiated
     * but have not yet completed).
     * Presently there is only ever one.
     * Use <code>registerForMmiInitiate</code>
     * and <code>registerForMmiComplete</code> for change notification.
     */
    public List<? extends MmiCode> getPendingMmiCodes();

    /**
     * Sends user response to a USSD REQUEST message.  An MmiCode instance
     * representing this response is sent to handlers registered with
     * registerForMmiInitiate.
     *
     * @param ussdMessge    Message to send in the response.
     */
    public void sendUssdResponse(String ussdMessge);

    /**
     * Register for ServiceState changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a ServiceState instance
     */
    void registerForServiceStateChanged(Handler h, int what, Object obj);

    /**
     * Unregisters for ServiceStateChange notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForServiceStateChanged(Handler h);

    /**
     * Register for Supplementary Service notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSuppServiceNotification(Handler h, int what, Object obj);

    /**
     * Unregisters for Supplementary Service notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForSuppServiceNotification(Handler h);

    /**
     * Register for notifications when a supplementary service attempt fails.
     * Message.obj will contain an AsyncResult.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSuppServiceFailed(Handler h, int what, Object obj);

    /**
     * Unregister for notifications when a supplementary service attempt fails.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForSuppServiceFailed(Handler h);

    /**
     * Register for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj);

    /**
     * Unegister for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForInCallVoicePrivacyOn(Handler h);

    /**
     * Register for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj);

    /**
     * Unegister for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForInCallVoicePrivacyOff(Handler h);

    /**
     * Register for notifications when CDMA OTA Provision status change
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForCdmaOtaStatusChange(Handler h, int what, Object obj);

    /**
     * Unegister for notifications when CDMA OTA Provision status change
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForCdmaOtaStatusChange(Handler h);

    /**
     * Registration point for subscription info ready
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj);

    /**
     * Unregister for notifications for subscription info
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSubscriptionInfoReady(Handler h);

    /**
     * Returns SIM record load state. Use
     * <code>getSimCard().registerForReady()</code> for change notification.
     *
     * @return true if records from the SIM have been loaded and are
     * available (if applicable). If not applicable to the underlying
     * technology, returns true as well.
     */
    boolean getIccRecordsLoaded();

    /**
     * Returns the ICC card interface for this phone, or null
     * if not applicable to underlying technology.
     */
    IccCard getIccCard();

    /**
     * Answers a ringing or waiting call. Active calls, if any, go on hold.
     * Answering occurs asynchronously, and final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException when no call is ringing or waiting
     */
    void acceptCall() throws CallStateException;

    /**
     * Reject (ignore) a ringing call. In GSM, this means UDUB
     * (User Determined User Busy). Reject occurs asynchronously,
     * and final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException when no call is ringing or waiting
     */
    void rejectCall() throws CallStateException;

    /**
     * Places any active calls on hold, and makes any held calls
     *  active. Switch occurs asynchronously and may fail.
     * Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if a call is ringing, waiting, or
     * dialing/alerting. In these cases, this operation may not be performed.
     */
    void switchHoldingAndActive() throws CallStateException;

    /**
     * Whether or not the phone can conference in the current phone
     * state--that is, one call holding and one call active.
     * @return true if the phone can conference; false otherwise.
     */
    boolean canConference();

    /**
     * Conferences holding and active. Conference occurs asynchronously
     * and may fail. Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if canConference() would return false.
     * In these cases, this operation may not be performed.
     */
    void conference() throws CallStateException;

    /**
     * Enable or disable enhanced Voice Privacy (VP). If enhanced VP is
     * disabled, normal VP is enabled.
     *
     * @param enable whether true or false to enable or disable.
     * @param onComplete a callback message when the action is completed.
     */
    void enableEnhancedVoicePrivacy(boolean enable, Message onComplete);

    /**
     * Get the currently set Voice Privacy (VP) mode.
     *
     * @param onComplete a callback message when the action is completed.
     */
    void getEnhancedVoicePrivacy(Message onComplete);

    /**
     * Whether or not the phone can do explicit call transfer in the current
     * phone state--that is, one call holding and one call active.
     * @return true if the phone can do explicit call transfer; false otherwise.
     */
    boolean canTransfer();

    /**
     * Connects the two calls and disconnects the subscriber from both calls
     * Explicit Call Transfer occurs asynchronously
     * and may fail. Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if canTransfer() would return false.
     * In these cases, this operation may not be performed.
     */
    void explicitCallTransfer() throws CallStateException;

    /**
     * Clears all DISCONNECTED connections from Call connection lists.
     * Calls that were in the DISCONNECTED state become idle. This occurs
     * synchronously.
     */
    void clearDisconnected();


    /**
     * Gets the foreground call object, which represents all connections that
     * are dialing or active (all connections
     * that have their audio path connected).<p>
     *
     * The foreground call is a singleton object. It is constant for the life
     * of this phone. It is never null.<p>
     *
     * The foreground call will only ever be in one of these states:
     * IDLE, ACTIVE, DIALING, ALERTING, or DISCONNECTED.
     *
     * State change notification is available via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     */
    Call getForegroundCall();

    /**
     * Gets the background call object, which represents all connections that
     * are holding (all connections that have been accepted or connected, but
     * do not have their audio path connected). <p>
     *
     * The background call is a singleton object. It is constant for the life
     * of this phone object . It is never null.<p>
     *
     * The background call will only ever be in one of these states:
     * IDLE, HOLDING or DISCONNECTED.
     *
     * State change notification is available via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     */
    Call getBackgroundCall();

    /**
     * Gets the ringing call object, which represents an incoming
     * connection (if present) that is pending answer/accept. (This connection
     * may be RINGING or WAITING, and there may be only one.)<p>

     * The ringing call is a singleton object. It is constant for the life
     * of this phone. It is never null.<p>
     *
     * The ringing call will only ever be in one of these states:
     * IDLE, INCOMING, WAITING or DISCONNECTED.
     *
     * State change notification is available via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     */
    Call getRingingCall();

    /**
     * Initiate a new voice connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     * possible because no more call slots exist or a call exists that is
     * dialing, alerting, ringing, or waiting.  Other errors are
     * handled asynchronously.
     */
    Connection dial(String dialString) throws CallStateException;

    /**
     * Initiate a new voice connection with supplementary User to User
     * Information. This happens asynchronously, so you cannot assume the audio
     * path is connected (or a call index has been assigned) until
     * PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     *                possible because no more call slots exist or a call exists
     *                that is dialing, alerting, ringing, or waiting. Other
     *                errors are handled asynchronously.
     */
    Connection dial(String dialString, UUSInfo uusInfo) throws CallStateException;

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.
     */
    boolean handlePinMmi(String dialString);

    /**
     * Handles in-call MMI commands. While in a call, or while receiving a
     * call, use this to execute MMI commands.
     * see 3GPP 20.030, section 6.5.5.1 for specs on the allowed MMI commands.
     *
     * @param command the MMI command to be executed.
     * @return true if the MMI command is executed.
     * @throws CallStateException
     */
    boolean handleInCallMmiCommands(String command) throws CallStateException;

    /**
     * Play a DTMF tone on the active call. Ignored if there is no active call.
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * silently ignored.
     */
    void sendDtmf(char c);

    /**
     * Start to paly a DTMF tone on the active call. Ignored if there is no active call
     * or there is a playing DTMF tone.
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * silently ignored.
     */
    void startDtmf(char c);

    /**
     * Stop the playing DTMF tone. Ignored if there is no playing DTMF
     * tone or no active call.
     */
    void stopDtmf();

    /**
     * send burst DTMF tone, it can send the string as single character or multiple character
     * ignore if there is no active call or not valid digits string.
     * Valid digit means only includes characters ISO-LATIN characters 0-9, *, #
     * The difference between sendDtmf and sendBurstDtmf is sendDtmf only sends one character,
     * this api can send single character and multiple character, also, this api has response
     * back to caller.
     *
     * @param dtmfString is string representing the dialing digit(s) in the active call
     * @param on the DTMF ON length in milliseconds, or 0 for default
     * @param off the DTMF OFF length in milliseconds, or 0 for default
     * @param onComplete is the callback message when the action is processed by BP
     *
     */
    void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete);

    /**
     * Sets the radio power on/off state (off is sometimes
     * called "airplane mode"). Current state can be gotten via
     * {@link #getServiceState()}.{@link
     * android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete.
     *
     * @param power true means "on", false means "off".
     */
    void setRadioPower(boolean power);
//MTK-START [mtk80601][111212][ALPS00093395]IPO feature
    /**
     * Sets the radio power on after power off for reset.
     */
    void setRadioPowerOn();
//MTK-END [mtk80601][111212][ALPS00093395]IPO feature

    /**
     * Get voice message waiting indicator status. No change notification
     * available on this interface. Use PhoneStateNotifier or similar instead.
     *
     * @return true if there is a voice message waiting
     */
    boolean getMessageWaitingIndicator();

    /**
     * Get voice call forwarding indicator status. No change notification
     * available on this interface. Use PhoneStateNotifier or similar instead.
     *
     * @return true if there is a voice call forwarding
     */
    boolean getCallForwardingIndicator();

    /**
     * Get the line 1 phone number (MSISDN). For CDMA phones, the MDN is returned
     * and {@link #getMsisdn()} will return the MSISDN on CDMA/LTE phones.<p>
     *
     * @return phone number. May return null if not
     * available or the SIM is not ready
     */
    String getLine1Number();

    /**
     * Returns the alpha tag associated with the msisdn number.
     * If there is no alpha tag associated or the record is not yet available,
     * returns a default localized string. <p>
     */
    String getLine1AlphaTag();

    /**
     * Sets the MSISDN phone number in the SIM card.
     *
     * @param alphaTag the alpha tag associated with the MSISDN phone number
     *        (see getMsisdnAlphaTag)
     * @param number the new MSISDN phone number to be set on the SIM.
     * @param onComplete a callback message when the action is completed.
     */
    void setLine1Number(String alphaTag, String number, Message onComplete);

    /**
     * Get the voice mail access phone number. Typically dialed when the
     * user holds the "1" key in the phone app. May return null if not
     * available or the SIM is not ready.<p>
     */
    String getVoiceMailNumber();

    /**
     * Returns unread voicemail count. This count is shown when the  voicemail
     * notification is expanded.<p>
     */
    int getVoiceMessageCount();

    /**
     * Returns the alpha tag associated with the voice mail number.
     * If there is no alpha tag associated or the record is not yet available,
     * returns a default localized string. <p>
     *
     * Please use this value instead of some other localized string when
     * showing a name for this number in the UI. For example, call log
     * entries should show this alpha tag. <p>
     *
     * Usage of this alpha tag in the UI is a common carrier requirement.
     */
    String getVoiceMailAlphaTag();

    /**
     * setVoiceMailNumber
     * sets the voicemail number in the SIM card.
     *
     * @param alphaTag the alpha tag associated with the voice mail number
     *        (see getVoiceMailAlphaTag)
     * @param voiceMailNumber the new voicemail number to be set on the SIM.
     * @param onComplete a callback message when the action is completed.
     */
    void setVoiceMailNumber(String alphaTag,
                            String voiceMailNumber,
                            Message onComplete);

    /**
     * getCallForwardingOptions
     * gets a call forwarding option. The return value of
     * ((AsyncResult)onComplete.obj) is an array of CallForwardInfo.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CallForwardInfo for details.
     */
    void getCallForwardingOption(int commandInterfaceCFReason,
                                  Message onComplete);

    /**
     * setCallForwardingOptions
     * sets a call forwarding option.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param commandInterfaceCFAction is one of the valid call forwarding
     *        CF_ACTIONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param dialingNumber is the target phone number to forward calls to
     * @param timerSeconds is used by CFNRy to indicate the timeout before
     *        forwarding is attempted.
     * @param onComplete a callback message when the action is completed.
     */
    void setCallForwardingOption(int commandInterfaceCFReason,
                                 int commandInterfaceCFAction,
                                 String dialingNumber,
                                 int timerSeconds,
                                 Message onComplete);

    /**
     * getOutgoingCallerIdDisplay
     * gets outgoing caller id display. The return value of
     * ((AsyncResult)onComplete.obj) is an array of int, with a length of 2.
     *
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CommandsInterface#getCLIR for details.
     */
    void getOutgoingCallerIdDisplay(Message onComplete);

    /**
     * setOutgoingCallerIdDisplay
     * sets a call forwarding option.
     *
     * @param commandInterfaceCLIRMode is one of the valid call CLIR
     *        modes, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface./code>
     * @param onComplete a callback message when the action is completed.
     */
    void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode,
                                    Message onComplete);

    /**
     * getCallWaiting
     * gets call waiting activation state. The return value of
     * ((AsyncResult)onComplete.obj) is an array of int, with a length of 1.
     *
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CommandsInterface#queryCallWaiting for details.
     */
    void getCallWaiting(Message onComplete);

    /**
     * setCallWaiting
     * sets a call forwarding option.
     *
     * @param enable is a boolean representing the state that you are
     *        requesting, true for enabled, false for disabled.
     * @param onComplete a callback message when the action is completed.
     */
    void setCallWaiting(boolean enable, Message onComplete);

    /**
     * Scan available networks. This method is asynchronous; .
     * On completion, <code>response.obj</code> is set to an AsyncResult with
     * one of the following members:.<p>
     *<ul>
     * <li><code>response.obj.result</code> will be a <code>List</code> of
     * <code>OperatorInfo</code> objects, or</li>
     * <li><code>response.obj.exception</code> will be set with an exception
     * on failure.</li>
     * </ul>
     */
    void getAvailableNetworks(Message response);

    /**
     * Switches network selection mode to "automatic", re-scanning and
     * re-selecting a network if appropriate.
     *
     * @param response The message to dispatch when the network selection
     * is complete.
     *
     * @see #selectNetworkManually(OperatorInfo, android.os.Message )
     */
    void setNetworkSelectionModeAutomatic(Message response);

    /**
     * Manually selects a network. <code>response</code> is
     * dispatched when this is complete.  <code>response.obj</code> will be
     * an AsyncResult, and <code>response.obj.exception</code> will be non-null
     * on failure.
     *
     * @see #setNetworkSelectionModeAutomatic(Message)
     */
    void selectNetworkManually(OperatorInfo network,
                            Message response);

    /**
     *  Requests to set the preferred network type for searching and registering
     * (CS/PS domain, RAT, and operation mode)
     * @param networkType one of  NT_*_TYPE
     * @param response is callback message
     */
    void setPreferredNetworkType(int networkType, Message response);

    /**
     *  Query the preferred network type setting
     *
     * @param response is callback message to report one of  NT_*_TYPE
     */
    void getPreferredNetworkType(Message response);

    /**
     * Gets the default SMSC address.
     *
     * @param result Callback message contains the SMSC address.
     */
    void getSmscAddress(Message result);

    /**
     * Sets the default SMSC address.
     *
     * @param address new SMSC address
     * @param result Callback message is empty on completion
     */
    void setSmscAddress(String address, Message result);

    /**
     * Query neighboring cell IDs.  <code>response</code> is dispatched when
     * this is complete.  <code>response.obj</code> will be an AsyncResult,
     * and <code>response.obj.exception</code> will be non-null on failure.
     * On success, <code>AsyncResult.result</code> will be a <code>String[]</code>
     * containing the neighboring cell IDs.  Index 0 will contain the count
     * of available cell IDs.  Cell IDs are in hexadecimal format.
     *
     * @param response callback message that is dispatched when the query
     * completes.
     */
    void getNeighboringCids(Message response);

    /**
     * Sets an event to be fired when the telephony system processes
     * a post-dial character on an outgoing call.<p>
     *
     * Messages of type <code>what</code> will be sent to <code>h</code>.
     * The <code>obj</code> field of these Message's will be instances of
     * <code>AsyncResult</code>. <code>Message.obj.result</code> will be
     * a Connection object.<p>
     *
     * Message.arg1 will be the post dial character being processed,
     * or 0 ('\0') if end of string.<p>
     *
     * If Connection.getPostDialState() == WAIT,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWaitChar()
     * Connection.proceedAfterWaitChar()} or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the post-dial
     * DTMF sequence.<p>
     *
     * If Connection.getPostDialState() == WILD,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWildChar
     * Connection.proceedAfterWildChar()}
     * or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the
     * post-dial DTMF sequence.<p>
     *
     * Only one post dial character handler may be set. <p>
     * Calling this method with "h" equal to null unsets this handler.<p>
     */
    void setOnPostDialCharacter(Handler h, int what, Object obj);


    /**
     * Mutes or unmutes the microphone for the active call. The microphone
     * is automatically unmuted if a call is answered, dialed, or resumed
     * from a holding state.
     *
     * @param muted true to mute the microphone,
     * false to activate the microphone.
     */

    void setMute(boolean muted);

    /**
     * Gets current mute status. Use
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}
     * as a change notifcation, although presently phone state changed is not
     * fired when setMute() is called.
     *
     * @return true is muting, false is unmuting
     */
    boolean getMute();

    /**
     * Enables or disables echo suppression.
     */
    void setEchoSuppressionEnabled(boolean enabled);

    /**
     * Invokes RIL_REQUEST_OEM_HOOK_RAW on RIL implementation.
     *
     * @param data The data for the request.
     * @param response <strong>On success</strong>,
     * (byte[])(((AsyncResult)response.obj).result)
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     *
     * @see #invokeOemRilRequestRaw(byte[], android.os.Message)
     */
    void invokeOemRilRequestRaw(byte[] data, Message response);

    /**
     * Invokes RIL_REQUEST_OEM_HOOK_Strings on RIL implementation.
     *
     * @param strings The strings to make available as the request data.
     * @param response <strong>On success</strong>, "response" bytes is
     * made available as:
     * (String[])(((AsyncResult)response.obj).result).
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     *
     * @see #invokeOemRilRequestStrings(java.lang.String[], android.os.Message)
     */
    void invokeOemRilRequestStrings(String[] strings, Message response);

    /**
     * Get the current active Data Call list
     *
     * @param response <strong>On success</strong>, "response" bytes is
     * made available as:
     * (String[])(((AsyncResult)response.obj).result).
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     */
    void getDataCallList(Message response);

    /**
     * Update the ServiceState CellLocation for current network registration.
     */
    void updateServiceLocation();

    /**
     * Enable location update notifications.
     */
    void enableLocationUpdates();

    /**
     * Disable location update notifications.
     */
    void disableLocationUpdates();

    /**
     * For unit tests; don't send notifications to "Phone"
     * mailbox registrants if true.
     */
    void setUnitTestMode(boolean f);

    /**
     * @return true If unit test mode is enabled
     */
    boolean getUnitTestMode();

    /**
     * Assign a specified band for RF configuration.
     *
     * @param bandMode one of BM_*_BAND
     * @param response is callback message
     */
    void setBandMode(int bandMode, Message response);

    /**
     * Query the list of band mode supported by RF.
     *
     * @param response is callback message
     *        ((AsyncResult)response.obj).result  is an int[] with every
     *        element representing one avialable BM_*_BAND
     */
    void queryAvailableBandMode(Message response);

    /**
     * @return true if enable data connection on roaming
     */
    boolean getDataRoamingEnabled();

    /**
     * @param enable set true if enable data connection on roaming
     */
    void setDataRoamingEnabled(boolean enable);

    /**
     *  Query the CDMA roaming preference setting
     *
     * @param response is callback message to report one of  CDMA_RM_*
     */
    void queryCdmaRoamingPreference(Message response);

    /**
     *  Requests to set the CDMA roaming preference
     * @param cdmaRoamingType one of  CDMA_RM_*
     * @param response is callback message
     */
    void setCdmaRoamingPreference(int cdmaRoamingType, Message response);

    /**
     *  Requests to set the CDMA subscription mode
     * @param cdmaSubscriptionType one of  CDMA_SUBSCRIPTION_*
     * @param response is callback message
     */
    void setCdmaSubscription(int cdmaSubscriptionType, Message response);

    /**
     * If this is a simulated phone interface, returns a SimulatedRadioControl.
     * @ return A SimulatedRadioControl if this is a simulated interface;
     * otherwise, null.
     */
    SimulatedRadioControl getSimulatedRadioControl();

    /**
     * Enables the specified APN type. Only works for "special" APN types,
     * i.e., not the default APN.
     * @param type The desired APN type. Cannot be {@link #APN_TYPE_DEFAULT}.
     * @return <code>APN_ALREADY_ACTIVE</code> if the current APN
     * services the requested type.<br/>
     * <code>APN_TYPE_NOT_AVAILABLE</code> if the carrier does not
     * support the requested APN.<br/>
     * <code>APN_REQUEST_STARTED</code> if the request has been initiated.<br/>
     * <code>APN_REQUEST_FAILED</code> if the request was invalid.<br/>
     * A <code>ACTION_ANY_DATA_CONNECTION_STATE_CHANGED</code> broadcast will
     * indicate connection state progress.
     */
    int enableApnType(String type);

    /**
     * Disables the specified APN type, and switches back to the default APN,
     * if necessary. Switching to the default APN will not happen if default
     * data traffic has been explicitly disabled via a call to {@link #disableDataConnectivity}.
     * <p/>Only works for "special" APN types,
     * i.e., not the default APN.
     * @param type The desired APN type. Cannot be {@link #APN_TYPE_DEFAULT}.
     * @return <code>APN_ALREADY_ACTIVE</code> if the default APN
     * is already active.<br/>
     * <code>APN_REQUEST_STARTED</code> if the request to switch to the default
     * APN has been initiated.<br/>
     * <code>APN_REQUEST_FAILED</code> if the request was invalid.<br/>
     * A <code>ACTION_ANY_DATA_CONNECTION_STATE_CHANGED</code> broadcast will
     * indicate connection state progress.
     */
    int disableApnType(String type);

    /**
     * Report on whether data connectivity is allowed.
     */
    boolean isDataConnectivityPossible();

    /**
     * Report on whether data connectivity is allowed for an APN.
     */
    boolean isDataConnectivityPossible(String apnType);

    /**
     * Retrieves the unique device ID, e.g., IMEI for GSM phones and MEID for CDMA phones.
     */
    String getDeviceId();

    /**
     * Retrieves the software version number for the device, e.g., IMEI/SV
     * for GSM phones.
     */
    String getDeviceSvn();

    /**
     * Retrieves the unique subscriber ID, e.g., IMSI for GSM phones.
     */
    String getSubscriberId();

    /**
     * Retrieves the serial number of the ICC, if applicable.
     */
    String getIccSerialNumber();

    /* CDMA support methods */

    /**
     * Retrieves the MIN for CDMA phones.
     */
    String getCdmaMin();

    /**
     * Check if subscription data has been assigned to mMin
     *
     * return true if MIN info is ready; false otherwise.
     */
    boolean isMinInfoReady();

    /**
     *  Retrieves PRL Version for CDMA phones
     */
    String getCdmaPrlVersion();

    /**
     * Retrieves the ESN for CDMA phones.
     */
    String getEsn();

    /**
     * Retrieves MEID for CDMA phones.
     */
    String getMeid();

    /**
     * Retrieves the MSISDN from the UICC. For GSM/UMTS phones, this is equivalent to
     * {@link #getLine1Number()}. For CDMA phones, {@link #getLine1Number()} returns
     * the MDN, so this method is provided to return the MSISDN on CDMA/LTE phones.
     */
    String getMsisdn();

    /**
     * Retrieves IMEI for phones. Returns null if IMEI is not set.
     */
    String getImei();

    /**
     * Retrieves the PhoneSubInfo of the Phone
     */
    public PhoneSubInfo getPhoneSubInfo();

    /**
     * Retrieves the IccSmsInterfaceManager of the Phone
     */
    public IccSmsInterfaceManager getIccSmsInterfaceManager();

    /**
     * Retrieves the IccPhoneBookInterfaceManager of the Phone
     */
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager();

    /**
     * setTTYMode
     * sets a TTY mode option.
     * @param ttyMode is a one of the following:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param onComplete a callback message when the action is completed
     */
    void setTTYMode(int ttyMode, Message onComplete);

    /**
     * queryTTYMode
     * query the status of the TTY mode
     *
     * @param onComplete a callback message when the action is completed.
     */
    void queryTTYMode(Message onComplete);

    /**
     * Activate or deactivate cell broadcast SMS.
     *
     * @param activate
     *            0 = activate, 1 = deactivate
     * @param response
     *            Callback message is empty on completion
     */
    void activateCellBroadcastSms(int activate, Message response);

    /**
     * Query the current configuration of cdma cell broadcast SMS.
     *
     * @param response
     *            Callback message is empty on completion
     */
    void getCellBroadcastSmsConfig(Message response);

    /**
     * Configure cell broadcast SMS.
     *
     * TODO: Change the configValuesArray to a RIL_BroadcastSMSConfig
     *
     * @param response
     *            Callback message is empty on completion
     */
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response);

    public void notifyDataActivity();

    /**
     * Returns the CDMA ERI icon index to display
     */
    public int getCdmaEriIconIndex();

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    public int getCdmaEriIconMode();

    /**
     * Returns the CDMA ERI text,
     */
    public String getCdmaEriText();

    /**
     * request to exit emergency call back mode
     * the caller should use setOnECMModeExitResponse
     * to receive the emergency callback mode exit response
     */
    void exitEmergencyCallbackMode();

    /**
     * this decides if the dial number is OTA(Over the air provision) number or not
     * @param dialStr is string representing the dialing digit(s)
     * @return  true means the dialStr is OTA number, and false means the dialStr is not OTA number
     */
    boolean isOtaSpNumber(String dialStr);

    /**
     * Returns true if OTA Service Provisioning needs to be performed.
     */
    boolean needsOtaServiceProvisioning();

    /**
     * Register for notifications when CDMA call waiting comes
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForCallWaiting(Handler h, int what, Object obj);

    /**
     * Unegister for notifications when CDMA Call waiting comes
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForCallWaiting(Handler h);


    /**
     * Register for signal information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */

    void registerForSignalInfo(Handler h, int what, Object obj) ;
    /**
     * Unregisters for signal information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForSignalInfo(Handler h);

    /**
     * Register for display information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForDisplayInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for display information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForDisplayInfo(Handler h) ;

    /**
     * Register for CDMA number information record notification from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaNumberInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForNumberInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for number information record notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForNumberInfo(Handler h);

    /**
     * Register for CDMA redirected number information record notification
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaRedirectingNumberInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForRedirectedNumberInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for redirected number information record notification.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForRedirectedNumberInfo(Handler h);

    /**
     * Register for CDMA line control information record notification
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaLineControlInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForLineControlInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for line control information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForLineControlInfo(Handler h);

    /**
     * Register for CDMA T53 CLIR information record notifications
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaT53ClirInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerFoT53ClirlInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for T53 CLIR information record notification
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForT53ClirInfo(Handler h);

    /**
     * Register for CDMA T53 audio control information record notifications
     * from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a CdmaInformationRecords.CdmaT53AudioControlInfoRec
     * instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForT53AudioControlInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for T53 audio control information record notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unregisterForT53AudioControlInfo(Handler h);

    /**
     * registers for exit emergency call back mode request response
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */

    void setOnEcbModeExitResponse(Handler h, int what, Object obj);

    /**
     * Unregisters for exit emergency call back mode request response
     *
     * @param h Handler to be removed from the registrant list.
     */
    void unsetOnEcbModeExitResponse(Handler h);

    /**
     * Return if the current radio is LTE on CDMA. This
     * is a tri-state return value as for a period of time
     * the mode may be unknown.
     *
     * @return {@link #LTE_ON_CDMA_UNKNOWN}, {@link #LTE_ON_CDMA_FALSE} or {@link #LTE_ON_CDMA_TRUE}
     */
    public int getLteOnCdmaMode();

    /**
     * TODO: Adding a function for each property is not good.
     * A fucntion of type getPhoneProp(propType) where propType is an
     * enum of GSM+CDMA+LTE props would be a better approach.
     *
     * Get "Restriction of menu options for manual PLMN selection" bit
     * status from EF_CSP data, this belongs to "Value Added Services Group".
     * @return true if this bit is set or EF_CSP data is unavailable,
     * false otherwise
     */
    boolean isCspPlmnEnabled();
    boolean isCspPlmnEnabled(int simId);

    /**
     * Return an interface to retrieve the ISIM records for IMS, if available.
     * @return the interface to retrieve the ISIM records, or null if not supported
     */
    IsimRecords getIsimRecords();

    /**
     * Request the ISIM application on the UICC to perform the AKA
     * challenge/response algorithm for IMS authentication. The nonce string
     * and challenge response are Base64 encoded Strings.
     *
     * @param nonce the nonce string to pass with the ISIM authentication request
     * @param response a callback message with the String response in the obj field
     */
    void requestIsimAuthentication(String nonce, Message response);

    /**
     * Sets the SIM voice message waiting indicator records.
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    void setVoiceMessageWaiting(int line, int countWaiting);

    /**
     * Gets the USIM service table from the UICC, if present and available.
     * @return an interface to the UsimServiceTable record, or null if not available
     */
    UsimServiceTable getUsimServiceTable();

    /**
     * Unregister from all events it registered for and dispose objects
     * created by this object.
     */
    void dispose();

    /**
     * Remove references to external object stored in this object.
     */
    void removeReferences();

    //MTK-START [mtk04070][111117][ALPS00093395]MTK proprietary methods
    /**
     * added by vend_am00002 for Multiple PDP Context
     * @param apnType
     */
    String getActiveApnType();

    /* Add by vendor for Multiple PDP Context */
    String getApnForType(String type);

    /**
     * Register for Supplementary Service CRSS notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppCrssNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCrssSuppServiceNotification(Handler h, int what, Object obj);

	/**
     * Unregisters for Supplementary Service CRSS notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCrssSuppServiceNotification(Handler h);

    /* vt start */
    /**
     * Initiate a new video connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     * possible because no more call slots exist or a call exists that is
     * dialing, alerting, ringing, or waiting.  Other errors are
     * handled asynchronously.
     */
    Connection vtDial(String dialString) throws CallStateException;

    /**
     * Initiate a new video connection with supplementary User to User
     * Information. This happens asynchronously, so you cannot assume the audio
     * path is connected (or a call index has been assigned) until
     * PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     *                possible because no more call slots exist or a call exists
     *                that is dialing, alerting, ringing, or waiting. Other
     *                errors are handled asynchronously.
     */
    Connection vtDial(String dialString, UUSInfo uusInfo) throws CallStateException;

    void voiceAccept() throws CallStateException;
    /* vt end */

    /**
     * Sets the radio power on/off state (off is sometimes
     * called "airplane mode"). Current state can be gotten via
     * {@link #getServiceState()}.{@link
     * android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete.
     * <p>
     *  If shutdown is true, will turn off the radio and SIM power.
     *   Used when shutdown the entire phone
     *
     * @param power true means "on", false means "off".
     * @param shutdown true means turn off entire phone
     */
	  void setRadioPower(boolean power, boolean shutdown);

    /**
     * Get the current active PDP context list
     *
     * @deprecated
     * @param response <strong>On success</strong>, "response" bytes is
     * made available as:
     * (String[])(((AsyncResult)response.obj).result).
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     */
    void getPdpContextList(Message response);

    /**
     * Returns the name of the network interface used by the specified APN type.
     */
    String getInterfaceName(String apnType);

    /**
     * Returns the IP address of the network interface used by the specified
     * APN type.
     */
    String getIpAddress(String apnType);

    /**
     * Returns the gateway for the network interface used by the specified APN
     * type.
     */
    String getGateway(String apnType);

    /**
     * Returns the DNS servers for the network interface used by the specified
     * APN type.
     */
    public String[] getDnsServers(String apnType);

    /**
     * Configure cell broadcast SMS.
     * @param chIdList
     *            Channel ID list, fill in the fromServiceId, toServiceId, and selected
     *            in the SmsBroadcastConfigInfo only
     * @param langList
     *            Channel ID list, fill in the fromCodeScheme, toCodeScheme, and selected
     *            in the SmsBroadcastConfigInfo only     
     * @param response     
     *            Callback message is empty on completion
     */
    public void setCellBroadcastSmsConfig(SmsBroadcastConfigInfo[] chIdList, 
            SmsBroadcastConfigInfo[] langList, Message response);

    /**
     * Query if the Cell broadcast is adtivated or not
     * @param response     
     *            Callback message is empty on completion
     */
    public void queryCellBroadcastSmsActivation(Message response);

    /**
     * getFacilityLock
     * gets Call Barring States. The return value of
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled serivice classes (sum of SERVICE_CLASS_*)
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void getFacilityLock(String facility, String password, Message onComplete);

    /**
     * setFacilityLock
     * sets Call Barring options.
     *
     * @param facility one of CB_FACILTY_*
     * @param enable true means lock, false means unlock
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void setFacilityLock(String facility, boolean enable, String password, Message onComplete);

    /**
     * changeBarringPassword
     * changes Call Barring related password.
     *
     * @param facility one of CB_FACILTY_*
     * @param oldPwd old password
     * @param newPwd new password
     * @param onComplete a callback message when the action is completed.
     */
    void changeBarringPassword(String facility, String oldPwd, String newPwd, Message onComplete);

    /**
     * changeBarringPassword
     * changes Call Barring related password.
     *
     * @param facility one of CB_FACILTY_*
     * @param oldPwd old password
     * @param newPwd new password
     * @param newCfm
     * @param onComplete a callback message when the action is completed.
     */
    void changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm, Message onComplete);

    /**
     * used to release all connections in the MS,
     * release all connections with one reqeust together, not seperated.
     */
    void hangupAll() throws CallStateException;

    /**
     * used to release all connections in the MS,
     * release all connections with one reqeust together, not seperated.
     */
    void hangupAllEx() throws CallStateException;

    /**
     * used to release all connections in the foregrond call.
     */
    void hangupActiveCall() throws CallStateException;

    /**
     * used to get CCM.
     *
     * result.obj = AsyncResult ar     
     *	 ar.exception carries exception on failure
     *	 ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of CCM
     * the value will be 3 bytes of hexadecimal format, 
     * ex: "00001E" indicates decimal value 30
     */
    void getCurrentCallMeter(Message result);

    /**
     * used to get ACM.
     *
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of ACM
     * the value will be 3 bytes of hexadecimal format, 
     * ex: "00001E" indicates decimal value 30
     */
    void getAccumulatedCallMeter(Message result);

    /**
     * used to get ACMMAX.
     *
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of ACMMax
     * the value will be 3 bytes of hexadecimal format, 
     * ex: "00001E" indicates decimal value 30
     */	
    void getAccumulatedCallMeterMaximum(Message result);

    /**
     * used to get price per unit and currency.
     *
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[2]
     * ar.result[0] contain the value of currency, ex: "GBP"
     * ar.result[1] contain the value of ppu, ex: "2.66"
     */
    void getPpuAndCurrency(Message result);

     /**
     * used to set ACMMax.
     *     
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * @param acmmax is the maximum value for ACM. ex: "00001E"
     * @param pin2 is necessary parameter.
     */	
    void setAccumulatedCallMeterMaximum(String acmmax, String pin2, Message result);

     /**
     * used to reset ACM.
     *          
     *result.obj = AsyncResult ar	 
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * @param pin2 is necessary parameter.	  
     */
    void resetAccumulatedCallMeter(String pin2, Message result);

     /**
     * used to set price per unit and currency.
     *          
     *result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * @param currency is value of "currency". ex: "GBP"
     * @param ppu is the value of "price per unit". ex: "2.66"
     * @param pin2 is necessary parameter.
     */  	 
    void setPpuAndCurrency(String currency, String ppu, String pin2, Message result);

    /**
     * Register for Neighboring cell info changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a String[ ] instance
     */
    void registerForNeighboringInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for Neighboring cell info changed notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForNeighboringInfo(Handler h);

    /**
     * Register for Network info changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a String[ ] instance
     */
    void registerForNetworkInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for Network info changed notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForNetworkInfo(Handler h);

    /**
     * Refresh Spn Display due to configuration change
     */
    void refreshSpnDisplay();

    /**
     * Request to get my SIM ID
     */
    int getMySimId();
 
    /**
     * Register for speech on/off indication.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a String[ ] instance
     */
    void registerForSpeechInfo(Handler h, int what, Object obj);

    /**
     * Unregister for speech on/off indication
     */
    void unregisterForSpeechInfo(Handler h);

    public void registerForVoiceCallIncomingIndication(Handler h, int what, Object obj);
    public void unregisterForVoiceCallIncomingIndication(Handler h);
    /**
     * Get last call fail cause
     */
    public int getLastCallFailCause();

    /* vt start */
    /**
     * Register for VT status indication.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a int[ ] instance
     */
    void registerForVtStatusInfo(Handler h, int what, Object obj);

    /**
     * Unregister for VT status indication
     */
    void unregisterForVtStatusInfo(Handler h);

    /**
     * Register for MT VT call indication.
     * Message.obj will contain an AsyncResult.
     */
    void registerForVtRingInfo(Handler h, int what, Object obj);

    /**
     * Unregister for MT VT call indication.
     */
    void unregisterForVtRingInfo(Handler h);

    /**
     * Register for call disconnect message when reject waiting vt/voice cal if active voice/vt call exists.
     * Message.obj will contain an AsyncResult.
     */
    public void registerForVtReplaceDisconnect(Handler h, int what, Object obj);

    /**
     * Unregister for call disconnect message.
     */
    public void unregisterForVtReplaceDisconnect(Handler h);
    /* vt end */

    /**
     * set GPRS transfer type: data prefer/call prefer
     */
    void setGprsTransferType(int type, Message response);    

    //Add by mtk80372 for Barcode Number
    /**
     * Request to get Barcode number.
     */
    void getMobileRevisionAndIMEI(int type,Message result);

    //Add by mtk80372 for Barcode Number
    String getSN();

    /**
    *Retrieves the IccServiceStatus for the specic SIM/USIM service 
    */
    IccServiceStatus getIccServiceStatus(IccService enService);

    /**
    *send BT SAP profile
    */
    void  sendBTSIMProfile(int nAction, int nType, String strData, Message response); 

    /**
     * Request 2G context authentication for SIM/USIM
     */
    void doSimAuthentication (String strRand, Message result); 

    /**
     * Request 3G context authentication for USIM
     */
    void doUSimAuthentication (String strRand, String strAutn, Message result); 

    /* vt start */
    /**
     * getVtCallForwardingOptions
     * gets a VT call forwarding option. The return value of
     * ((AsyncResult)onComplete.obj) is an array of CallForwardInfo.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CallForwardInfo for details.
     */
    void getVtCallForwardingOption(int commandInterfaceCFReason,
                                   Message onComplete);

    /**
     * setVtCallForwardingOptions
     * sets a VT call forwarding option.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param commandInterfaceCFAction is one of the valid call forwarding
     *        CF_ACTIONS, as defined in
     *       <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param dialingNumber is the target phone number to forward calls to
     * @param timerSeconds is used by CFNRy to indicate the timeout before
     *       forwarding is attempted.
     * @param onComplete a callback message when the action is completed.
     */
    void setVtCallForwardingOption(int commandInterfaceCFReason,
                                   int commandInterfaceCFAction,
                                   String dialingNumber,
                                   int timerSeconds,
                                   Message onComplete);

    /**
     * getVtCallWaiting
     * gets VT call waiting activation state. The return value of
     * ((AsyncResult)onComplete.obj) is an array of int, with a length of 1.
     *
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CommandsInterface#queryCallWaiting for details.
     */
    void getVtCallWaiting(Message onComplete);
    
    /**
     * setVtCallWaiting
     * sets VT call waiting state.
     *
     * @param enable is a boolean representing the state that you are
     *        requesting, true for enabled, false for disabled.
     * @param onComplete a callback message when the action is completed.
     */
    void setVtCallWaiting(boolean enable, Message onComplete);
    
    /**
     * getVtFacilityLock
     * gets VT Call Barring States. The return value of
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled serivice classes (sum of SERVICE_CLASS_*)
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void getVtFacilityLock(String facility, String password, Message onComplete);

    /**
     * setVtFacilityLock
     * sets VT Call Barring options.
     *
     * @param facility one of CB_FACILTY_*
     * @param enable true means lock, false means unlock
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void setVtFacilityLock(String facility, boolean enable, String password, Message onComplete);
    /* vt end */
    void updateSimIndicateState();
    int getSimIndicateState();

    /**
      *return true if the slot for this phone has SIM/USIM inserted even if airplane mode is on.
      */
    boolean isSimInsert();

    /* 3G Switch start */
    /**
     * get3GCapabilitySIM
     * get SIM with 3G capability.
     *
     * @return the id (slot) with 3G capability (Phone.GEMINI_SIM_ID_1 or Phone.GEMINI_SIM_ID_2).
     */
    int get3GCapabilitySIM();

    /**
     * set3GCapabilitySIM
     * set 3G capability to the SIM.
     *
     * @param the id (slot) of the SIM to have 3G capability.
     * @return the id (slot) with 3G capability (Phone.GEMINI_SIM_ID_1 or Phone.GEMINI_SIM_ID_2).
     */
    boolean set3GCapabilitySIM(int simId);
    /* 3G Switch end */

    void getPOLCapability(Message onComplete);
    void getPreferedOperatorList(Message onComplete);
    void setPOLEntry(NetworkInfoWithAcT networkWithAct, Message onComplete);
    
    void setCRO(int onoff, Message onComplete); //ALPS00279048
    // ALPS00302702 RAT balancing
    int getEfRatBalancing();
    int getEfRatBalancing(int simId);
    //MTK-END [mtk04070][111117][ALPS00093395]MTK proprietary methods

    // ALPS00294581
    void notifySimMissingStatus(boolean isSimInsert);

    // MVNO-API START
    String getSpNameInEfSpn();
    String getSpNameInEfSpn(int simId);

    String isOperatorMvnoForImsi();
    String isOperatorMvnoForImsi(int simId);

    String getFirstFullNameInEfPnn();
    String getFirstFullNameInEfPnn(int simId);

    String isOperatorMvnoForEfPnn();
    String isOperatorMvnoForEfPnn(int simId);

    boolean isIccCardProviderAsMvno();
    boolean isIccCardProviderAsMvno(int simId);
    // MVNO-API END

    //[New R8 modem FD]	
    public int setFDTimerValue(String newTimerValue[], Message onComplete);
    //[New R8 modem FD]
    public String[] getFDTimerValue();
}
