/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.os.SystemProperties;

/**
 * @hide
 */
public class PhoneConstants {

    //MTK-START [mtk04070][111117][ALPS00093395]Used in GeminiPhone for SIM1/SIM2 color
    public static final int TOTAL_SIM_COLOR_COUNT = 4;
    //MTK-END [mtk04070][111117][ALPS00093395]Used in GeminiPhone for SIM1/SIM2 color 

    public static final int GEMINI_SIM_NUM = SystemProperties.getInt(PhoneConstants.GEMINI_SIM_NUM_PROP, 2);
    
    /**
     * The phone state. One of the following:<p>
     * <ul>
     * <li>IDLE = no phone activity</li>
     * <li>RINGING = a phone call is ringing or call waiting.
     *  In the latter case, another call is active as well</li>
     * <li>OFFHOOK = The phone is off hook. At least one call
     * exists that is dialing, active or holding and no calls are
     * ringing or waiting.</li>
     * </ul>
     */
    public enum State {
        IDLE, RINGING, OFFHOOK;
    };

   /**
     * The state of a data connection.
     * <ul>
     * <li>CONNECTED = IP traffic should be available</li>
     * <li>CONNECTING = Currently setting up data connection</li>
     * <li>DISCONNECTED = IP not available</li>
     * <li>SUSPENDED = connection is created but IP traffic is
     *                 temperately not available. i.e. voice call is in place
     *                 in 2G network</li>
     * </ul>
     */
    public enum DataState {
        CONNECTED, CONNECTING, DISCONNECTED, SUSPENDED;
    };

    public static final String STATE_KEY = "state";

    // Radio Type
    public static final int PHONE_TYPE_NONE = RILConstants.NO_PHONE;
    public static final int PHONE_TYPE_GSM = RILConstants.GSM_PHONE;
    public static final int PHONE_TYPE_CDMA = RILConstants.CDMA_PHONE;
    public static final int PHONE_TYPE_SIP = RILConstants.SIP_PHONE;
    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    public static final int PHONE_TYPE_GEMINI = RILConstants.GEMINI_PHONE;
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    // Modes for LTE_ON_CDMA
    public static final int LTE_ON_CDMA_UNKNOWN = RILConstants.LTE_ON_CDMA_UNKNOWN;
    public static final int LTE_ON_CDMA_FALSE = RILConstants.LTE_ON_CDMA_FALSE;
    public static final int LTE_ON_CDMA_TRUE = RILConstants.LTE_ON_CDMA_TRUE;

    // Number presentation type for caller id display (From internal/Conneciton.java)
    public static int PRESENTATION_ALLOWED = 1;    // normal
    public static int PRESENTATION_RESTRICTED = 2; // block by user
    public static int PRESENTATION_UNKNOWN = 3;    // no specified or unknown by network
    public static int PRESENTATION_PAYPHONE = 4;   // show pay phone info


    public static final String PHONE_NAME_KEY = "phoneName";
    public static final String FAILURE_REASON_KEY = "reason";
    public static final String STATE_CHANGE_REASON_KEY = "reason";
    public static final String DATA_APN_TYPE_KEY = "apnType";
    public static final String DATA_APN_KEY = "apn";
    public static final String DATA_LINK_PROPERTIES_KEY = "linkProperties";
    public static final String DATA_LINK_CAPABILITIES_KEY = "linkCapabilities";

    public static final String DATA_IFACE_NAME_KEY = "iface";
    public static final String NETWORK_UNAVAILABLE_KEY = "networkUnvailable";
    public static final String DATA_NETWORK_ROAMING_KEY = "networkRoaming";
    public static final String PHONE_IN_ECM_STATE = "phoneinECMState";

    public static final String REASON_LINK_PROPERTIES_CHANGED = "linkPropertiesChanged";


    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    public static final String REASON_GPRS_ATTACHED_TIMEOUT = "gprsAttachedTimeout";
    /* Add by mtk01411 */
    public static final String REASON_ON_RADIO_AVAILABLE = "onRadioAvailable";
    public static final String REASON_ON_RECORDS_LOADED = "onRecordsLoaded";
    public static final String REASON_POLL_STATE_DONE = "pollStateDone";
    public static final String REASON_NO_SUCH_PDP = "noSuchPdp";
    public static final String REASON_PDP_NOT_ACTIVE = "pdpNotActive";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    /**
     * Return codes for <code>enableApnType()</code>
     */
    public static final int APN_ALREADY_ACTIVE     = 0;
    public static final int APN_REQUEST_STARTED    = 1;
    public static final int APN_TYPE_NOT_AVAILABLE = 2;
    public static final int APN_REQUEST_FAILED     = 3;
    public static final int APN_ALREADY_INACTIVE   = 4;
    
    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    /* Add by mtk01411 */
    public static final int APN_REQUEST_FAILED_DUE_TO_RADIO_OFF = 98;
    public static final int APN_TYPE_NOT_AVAILABLE_DUE_TO_RECORDS_NOT_LOADED = 99;
    public static final int APN_TYPE_DISABLE_ONGOING = 100;

    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    public static final String APN_TYPE_ALL = "*";
    /** APN type for default data traffic */
    public static final String APN_TYPE_DEFAULT = "default";
    /** APN type for MMS traffic */
    public static final String APN_TYPE_MMS = "mms";
    /** APN type for SUPL assisted GPS */
    public static final String APN_TYPE_SUPL = "supl";
    /** APN type for DUN traffic */
    public static final String APN_TYPE_DUN = "dun";
    /** APN type for HiPri traffic */
    public static final String APN_TYPE_HIPRI = "hipri";
    /** APN type for FOTA */
    public static final String APN_TYPE_FOTA = "fota";
    /** APN type for IMS */
    public static final String APN_TYPE_IMS = "ims";
    /** APN type for CBS */
    public static final String APN_TYPE_CBS = "cbs";

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    public static final String APN_TYPE_DM = "dm";
    public static final String APN_TYPE_WAP = "wap";
    public static final String APN_TYPE_NET = "net";
    public static final String APN_TYPE_CMMAIL = "cmmail";
    public static final String APN_TYPE_TETHERING = "tethering";
    public static final String APN_TYPE_RCSE = "rcse";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added
    
    // CFU query type
    public static final String CFU_QUERY_TYPE_PROP = "ril.cfu.querytype";
    public static final String CFU_QUERY_TYPE_DEF_VALUE = "0";
    
    /**
     * SIM ID for GEMINI
     */
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;
    public static final int GEMINI_SIM_3 = 2;
    public static final int GEMINI_SIM_4 = 3;	
    public static final int GEMINI_SIP_CALL = -1; //MTK added for SIP call
    public static final String GEMINI_SIM_ID_KEY = "simId";
    public static final String MULTI_SIM_ID_KEY = "simid";
    public static final String GEMINI_DEFAULT_SIM_PROP = "persist.radio.default_sim";
    public static final String GEMINI_SIM_NUM_PROP = "persist.gemini.sim_num";	

	  /** UNKNOWN, invalid value */
    public static final int SIM_INDICATOR_UNKNOWN = -1;
    /** ABSENT, no SIM/USIM card inserted for this phone */
    public static final int SIM_INDICATOR_ABSENT = 0;
    /** RADIOOFF,  has SIM/USIM inserted but not in use . */
    public static final int SIM_INDICATOR_RADIOOFF = 1;
    /** LOCKED,  has SIM/USIM inserted and the SIM/USIM has been locked. */
    public static final int SIM_INDICATOR_LOCKED = 2;
	  /** INVALID : has SIM/USIM inserted and not be locked but failed to register to the network. */
    public static final int SIM_INDICATOR_INVALID = 3; 
	  /** SEARCHING : has SIM/USIM inserted and SIM/USIM state is Ready and is searching for network. */
    public static final int SIM_INDICATOR_SEARCHING = 4; 
	  /** NORMAL = has SIM/USIM inserted and in normal service(not roaming and has no data connection). */
    public static final int SIM_INDICATOR_NORMAL = 5; 
    /** ROAMING : has SIM/USIM inserted and in roaming service(has no data connection). */	
    public static final int SIM_INDICATOR_ROAMING = 6; 
	  /** CONNECTED : has SIM/USIM inserted and in normal service(not roaming) and data connected. */
    public static final int SIM_INDICATOR_CONNECTED = 7; 
	  /** ROAMINGCONNECTED = has SIM/USIM inserted and in roaming service(not roaming) and data connected.*/
    public static final int SIM_INDICATOR_ROAMINGCONNECTED = 8;
    
    

}
