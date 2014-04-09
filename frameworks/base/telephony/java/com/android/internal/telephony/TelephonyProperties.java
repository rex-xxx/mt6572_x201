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

/**
 * Contains a list of string constants used to get or set telephone properties
 * in the system. You can use {@link android.os.SystemProperties os.SystemProperties}
 * to get and set these values.
 * @hide
 */
public interface TelephonyProperties
{
    //****** Baseband and Radio Interface version

    //TODO T: property strings do not have to be gsm specific
    //        change gsm.*operator.*" properties to "operator.*" properties

    /**
     * Baseband version
     * Availability: property is available any time radio is on
     */
    static final String PROPERTY_BASEBAND_VERSION = "gsm.version.baseband";

    /** Radio Interface Layer (RIL) library implementation. */
    static final String PROPERTY_RIL_IMPL = "gsm.version.ril-impl";

    //****** Current Network

    /** Alpha name of current registered operator.<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_ALPHA = "gsm.operator.alpha";
    //TODO: most of these properties are generic, substitute gsm. with phone. bug 1856959

    /** Numeric name (MCC+MNC) of current registered operator.<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_NUMERIC = "gsm.operator.numeric";

    /** 'true' if the device is on a manually selected network
     *
     *  Availability: when registered to a network
     */
    static final String PROPERTY_OPERATOR_ISMANUAL = "operator.ismanual";

    /** 'true' if the device is considered roaming on this network for GSM
     *  purposes.
     *  Availability: when registered to a network
     */
    static final String PROPERTY_OPERATOR_ISROAMING = "gsm.operator.isroaming";

    /** The ISO country code equivalent of the current registered operator's
     *  MCC (Mobile Country Code)<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_ISO_COUNTRY = "gsm.operator.iso-country";

    /**
     * The contents of this property is the value of the kernel command line
     * product_type variable that corresponds to a product that supports LTE on CDMA.
     * {@see BaseCommands#getLteOnCdmaMode()}
     */
    static final String PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE = "telephony.lteOnCdmaProductType";

    /**
     * The contents of this property is the one of {@link Phone#LTE_ON_CDMA_TRUE} or
     * {@link Phone#LTE_ON_CDMA_FALSE}. If absent the value will assumed to be false
     * and the {@see #PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE} will be used to determine its
     * final value which could also be {@link Phone#LTE_ON_CDMA_FALSE}.
     * {@see BaseCommands#getLteOnCdmaMode()}
     */
    static final String PROPERTY_LTE_ON_CDMA_DEVICE = "telephony.lteOnCdmaDevice";

    static final String CURRENT_ACTIVE_PHONE = "gsm.current.phone-type";

    //****** SIM Card
    /**
     * One of <code>"UNKNOWN"</code> <code>"ABSENT"</code> <code>"PIN_REQUIRED"</code>
     * <code>"PUK_REQUIRED"</code> <code>"NETWORK_LOCKED"</code> or <code>"READY"</code>
     */
    static String PROPERTY_SIM_STATE = "gsm.sim.state";

    /** The MCC+MNC (mobile country code+mobile network code) of the
     *  provider of the SIM. 5 or 6 decimal digits.
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_NUMERIC = "gsm.sim.operator.numeric";

    /** PROPERTY_ICC_OPERATOR_ALPHA is also known as the SPN, or Service Provider Name.
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_ALPHA = "gsm.sim.operator.alpha";

    /** ISO country code equivalent for the SIM provider's country code*/
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY = "gsm.sim.operator.iso-country";

    /**
     * Indicates the available radio technology.  Values include: <code>"unknown"</code>,
     * <code>"GPRS"</code>, <code>"EDGE"</code> and <code>"UMTS"</code>.
     */
    static String PROPERTY_DATA_NETWORK_TYPE = "gsm.network.type";

    /** Indicate if phone is in emergency callback mode */
    static final String PROPERTY_INECM_MODE = "ril.cdma.inecmmode";

    /** Indicate the timer value for exiting emergency callback mode */
    static final String PROPERTY_ECM_EXIT_TIMER = "ro.cdma.ecmexittimer";

    /** The international dialing prefix conversion string */
    static final String PROPERTY_IDP_STRING = "ro.cdma.idpstring";

    /**
     * Defines the schema for the carrier specified OTASP number
     */
    static final String PROPERTY_OTASP_NUM_SCHEMA = "ro.cdma.otaspnumschema";

    /**
     * Disable all calls including Emergency call when it set to true.
     */
    static final String PROPERTY_DISABLE_CALL = "ro.telephony.disable-call";

    /**
     * Set to true for vendor RIL's that send multiple UNSOL_CALL_RING notifications.
     */
    static final String PROPERTY_RIL_SENDS_MULTIPLE_CALL_RING =
        "ro.telephony.call_ring.multiple";

    /**
     * The number of milliseconds between CALL_RING notifications.
     */
    static final String PROPERTY_CALL_RING_DELAY = "ro.telephony.call_ring.delay";

    /**
     * Track CDMA SMS message id numbers to ensure they increment
     * monotonically, regardless of reboots.
     */
    static final String PROPERTY_CDMA_MSG_ID = "persist.radio.cdma.msgid";

    /**
     * Property to override DEFAULT_WAKE_LOCK_TIMEOUT
     */
    static final String PROPERTY_WAKE_LOCK_TIMEOUT = "ro.ril.wake_lock_timeout";

    /**
     * Set to true to indicate that the modem needs to be reset
     * when there is a radio technology change.
     */
    static final String PROPERTY_RESET_ON_RADIO_TECH_CHANGE = "persist.radio.reset_on_switch";

    /**
     * Set to false to disable SMS receiving, default is
     * the value of config_sms_capable
     */
    static final String PROPERTY_SMS_RECEIVE = "telephony.sms.receive";

    /**
     * Set to false to disable SMS sending, default is
     * the value of config_sms_capable
     */
    static final String PROPERTY_SMS_SEND = "telephony.sms.send";

    /**
     * Set to true to indicate a test CSIM card is used in the device.
     * This property is for testing purpose only. This should not be defined
     * in commercial configuration.
     */
    static final String PROPERTY_TEST_CSIM = "persist.radio.test-csim";

    /**
     * Ignore RIL_UNSOL_NITZ_TIME_RECEIVED completely, used for debugging/testing.
     */
    static final String PROPERTY_IGNORE_NITZ = "telephony.test.ignore.nitz";

    //MTK-START [mtk04070][111118][ALPS00093395]MTK added
    static final String PROPERTY_OPERATOR_ALPHA_2 = "gsm.operator.alpha.2";    
    static final String PROPERTY_OPERATOR_NUMERIC_2 = "gsm.operator.numeric.2";    
    static final String PROPERTY_OPERATOR_ISMANUAL_2 = "operator.ismanual.2";    
    static final String PROPERTY_OPERATOR_ISROAMING_2 = "gsm.operator.isroaming.2";    
    static final String PROPERTY_OPERATOR_ISO_COUNTRY_2 = "gsm.operator.iso-country.2";    

    static final String PROPERTY_OPERATOR_ALPHA_3 = "gsm.operator.alpha.3";    
    static final String PROPERTY_OPERATOR_NUMERIC_3 = "gsm.operator.numeric.3";    
    static final String PROPERTY_OPERATOR_ISMANUAL_3 = "operator.ismanual.3";    
    static final String PROPERTY_OPERATOR_ISROAMING_3 = "gsm.operator.isroaming.3";    
    static final String PROPERTY_OPERATOR_ISO_COUNTRY_3 = "gsm.operator.iso-country.3";    
    static final String PROPERTY_OPERATOR_ALPHA_4 = "gsm.operator.alpha.4";    
    static final String PROPERTY_OPERATOR_NUMERIC_4 = "gsm.operator.numeric.4";    
    static final String PROPERTY_OPERATOR_ISMANUAL_4 = "operator.ismanual.4";    
    static final String PROPERTY_OPERATOR_ISROAMING_4 = "gsm.operator.isroaming.4";    
    static final String PROPERTY_OPERATOR_ISO_COUNTRY_4 = "gsm.operator.iso-country.4";    


    static String PROPERTY_SIM_STATE_2 = "gsm.sim.state.2";
    static String PROPERTY_SIM_STATE_3 = "gsm.sim.state.3";
    static String PROPERTY_SIM_STATE_4 = "gsm.sim.state.4";
    static String PROPERTY_ICC_OPERATOR_NUMERIC_2 = "gsm.sim.operator.numeric.2";
    static String PROPERTY_ICC_OPERATOR_NUMERIC_3 = "gsm.sim.operator.numeric.3";
    static String PROPERTY_ICC_OPERATOR_NUMERIC_4 = "gsm.sim.operator.numeric.4";
    static String PROPERTY_ICC_OPERATOR_ALPHA_2 = "gsm.sim.operator.alpha.2";
    static String PROPERTY_ICC_OPERATOR_ALPHA_3 = "gsm.sim.operator.alpha.3";
    static String PROPERTY_ICC_OPERATOR_ALPHA_4 = "gsm.sim.operator.alpha.4";
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2 = "gsm.sim.operator.iso-country.2";
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY_3 = "gsm.sim.operator.iso-country.3";
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY_4 = "gsm.sim.operator.iso-country.4";

    /** PROPERTY_ICC_OPERATOR_DEFAULT_NAME is the operator name for plmn which origins the SIM.
     *  Availablity: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME = "gsm.sim.operator.default-name";
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2 = "gsm.sim.operator.default-name.2";
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME_3 = "gsm.sim.operator.default-name.3";
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME_4 = "gsm.sim.operator.default-name.4";
    static String PROPERTY_DATA_NETWORK_TYPE_2 = "gsm.network.type.2";    
    static String PROPERTY_DATA_NETWORK_TYPE_3 = "gsm.network.type.3";    
    static String PROPERTY_DATA_NETWORK_TYPE_4 = "gsm.network.type.4";    
    
    /**
     * Indicate how many SIM cards are inserted
     */
    static final String PROPERTY_GSM_SIM_INSERTED = "gsm.sim.inserted";

    /**
    * Indicate CS network type
    */
    static final String PROPERTY_CS_NETWORK_TYPE = "gsm.cs.network.type";
	
    /**
    * Indicate CS network type
    */
    static final String PROPERTY_CS_NETWORK_TYPE_2 = "gsm.cs.network.type.2";	

    static final String PROPERTY_CS_NETWORK_TYPE_3 = "gsm.cs.network.type.3";	

    static final String PROPERTY_CS_NETWORK_TYPE_4 = "gsm.cs.network.type.4";	
	
    /**
    * Indicate whether the SIM info has been updated
    */
    static final String PROPERTY_SIM_INFO_READY = "gsm.siminfo.ready";
	
    /**
    * Indicate if Roaming Indicator needed for SIM/USIM in slot1
    */
    static final String PROPERTY_ROAMING_INDICATOR_NEEDED = "gsm.roaming.indicator.needed";
	
    /**
    * Indicate if Roaming Indicator needed for SIM/USIM in slot2
    */
    static final String PROPERTY_ROAMING_INDICATOR_NEEDED_2 = "gsm.roaming.indicator.needed.2";

    static final String PROPERTY_ROAMING_INDICATOR_NEEDED_3 = "gsm.roaming.indicator.needed.3";

    static final String PROPERTY_ROAMING_INDICATOR_NEEDED_4 = "gsm.roaming.indicator.needed.4";
	
    //MTK-END [mtk04070][111118][ALPS00093395]MTK added

    /**
    * Indicate Modem version for slot2
    */
    static final String PROPERTY_BASEBAND_VERSION_2 = "gsm.version.baseband.2";	

    /**
    * Indicate if chaneing to SIM locale is processing
    */
    static final String PROPERTY_SIM_LOCALE_SETTINGS = "gsm.sim.locale.waiting";
	
    /**
    * Add for query project name and MD flavor
    */
    static final String PROPERTY_PROJECT = "gsm.project.baseband";
    static final String PROPERTY_PROJECT_2 = "gsm.project.baseband.2";

}
