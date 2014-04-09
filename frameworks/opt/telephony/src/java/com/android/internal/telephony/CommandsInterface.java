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

import com.mediatek.common.telephony.gsm.PBEntry;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import android.telephony.SmsParameters;

import android.os.Message;
import android.os.Handler;

/**
 * {@hide}
 */
public interface CommandsInterface {
    enum RadioState {
        RADIO_OFF(0),         /* Radio explictly powered off (eg CFUN=0) */
        RADIO_UNAVAILABLE(0), /* Radio unavailable (eg, resetting or not booted) */
        SIM_NOT_READY(1),     /* Radio is on, but the SIM interface is not ready */
        SIM_LOCKED_OR_ABSENT(1),  /* SIM PIN locked, PUK required, network
                                     personalization, or SIM absent */
        SIM_READY(1),         /* Radio is on and SIM interface is available */
        RUIM_NOT_READY(2),    /* Radio is on, but the RUIM interface is not ready */
        RUIM_READY(2),        /* Radio is on and the RUIM interface is available */
        RUIM_LOCKED_OR_ABSENT(2), /* RUIM PIN locked, PUK required, network
                                     personalization locked, or RUIM absent */
        NV_NOT_READY(3),      /* Radio is on, but the NV interface is not available */
        NV_READY(3),          /* Radio is on and the NV interface is available */
        RADIO_ON;          /* Radio is on */

        public boolean isOn() /* and available...*/ {
            return this == SIM_NOT_READY
                    || this == SIM_LOCKED_OR_ABSENT
                    || this == SIM_READY
                    || this == RUIM_NOT_READY
                    || this == RUIM_READY
                    || this == RUIM_LOCKED_OR_ABSENT
                    || this == NV_NOT_READY
                    || this == NV_READY;
        }
        private int stateType;

        private RadioState() {
        }

        private RadioState (int type) {
            stateType = type;
        }

        public int getType() {
            return stateType;
        }

        public boolean isAvailable() {
            return this != RADIO_UNAVAILABLE;
        }

        public boolean isSIMReady() {
            return this == SIM_READY;
        }

        public boolean isRUIMReady() {
            return this == RUIM_READY;
        }

        public boolean isNVReady() {
            return this == NV_READY;
        }

        public boolean isGsm() {
            if (BaseCommands.getLteOnCdmaModeStatic() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                return false;
            } else {
                return this == SIM_NOT_READY
                        || this == SIM_LOCKED_OR_ABSENT
                        || this == SIM_READY;
            }
        }

        public boolean isCdma() {
            if (BaseCommands.getLteOnCdmaModeStatic() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                return true;
            } else {
                return this ==  RUIM_NOT_READY
                        || this == RUIM_READY
                        || this == RUIM_LOCKED_OR_ABSENT
                        || this == NV_NOT_READY
                        || this == NV_READY;
            }
        }
    }

    //***** Constants

    // Used as parameter to dial() and setCLIR() below
    static final int CLIR_DEFAULT = 0;      // "use subscription default value"
    static final int CLIR_INVOCATION = 1;   // (restrict CLI presentation)
    static final int CLIR_SUPPRESSION = 2;  // (allow CLI presentation)


    // Used as parameters for call forward methods below
    static final int CF_ACTION_DISABLE          = 0;
    static final int CF_ACTION_ENABLE           = 1;
//  static final int CF_ACTION_UNUSED           = 2;
    static final int CF_ACTION_REGISTRATION     = 3;
    static final int CF_ACTION_ERASURE          = 4;

    static final int CF_REASON_UNCONDITIONAL    = 0;
    static final int CF_REASON_BUSY             = 1;
    static final int CF_REASON_NO_REPLY         = 2;
    static final int CF_REASON_NOT_REACHABLE    = 3;
    static final int CF_REASON_ALL              = 4;
    static final int CF_REASON_ALL_CONDITIONAL  = 5;

    // Used for call barring methods below
    static final String CB_FACILITY_BAOC         = "AO";
    static final String CB_FACILITY_BAOIC        = "OI";
    static final String CB_FACILITY_BAOICxH      = "OX";
    static final String CB_FACILITY_BAIC         = "AI";
    static final String CB_FACILITY_BAICr        = "IR";
    static final String CB_FACILITY_BA_ALL       = "AB";
    static final String CB_FACILITY_BA_MO        = "AG";
    static final String CB_FACILITY_BA_MT        = "AC";
    static final String CB_FACILITY_BA_SIM       = "SC";
    static final String CB_FACILITY_BA_FD        = "FD";


    // Used for various supp services apis
    // See 27.007 +CCFC or +CLCK
    static final int SERVICE_CLASS_NONE     = 0; // no user input
    static final int SERVICE_CLASS_VOICE    = (1 << 0);
    static final int SERVICE_CLASS_DATA     = (1 << 1); //synonym for 16+32+64+128
    static final int SERVICE_CLASS_FAX      = (1 << 2);
    static final int SERVICE_CLASS_SMS      = (1 << 3);
    static final int SERVICE_CLASS_DATA_SYNC = (1 << 4);
    static final int SERVICE_CLASS_DATA_ASYNC = (1 << 5);
    static final int SERVICE_CLASS_PACKET   = (1 << 6);
    static final int SERVICE_CLASS_PAD      = (1 << 7);

    //MTK-START [mtk04070][111118][ALPS00093395]MTK added
    static final int SERVICE_CLASS_LINE2    = (1 << 8); // Add for Line2
    static final int SERVICE_CLASS_VIDEO    = (1 << 9); // Add for VT support
    static final int SERVICE_CLASS_MAX      = (1 << 9); // Max SERVICE_CLASS value
    //MTK-END [mtk04070][111118][ALPS00093395]MTK added

    // Numeric representation of string values returned
    // by messages sent to setOnUSSD handler
    static final int USSD_MODE_NOTIFY       = 0;
    static final int USSD_MODE_REQUEST      = 1;
    //MTK-START [mtk04070][111118][ALPS00093395]MTK added
    static final int USSD_SESSION_END               = 2;
    static final int USSD_HANDLED_BY_STK            = 3;
    static final int USSD_OPERATION_NOT_SUPPORTED   = 4;
    static final int USSD_NETWORK_TIMEOUT           = 5;
    //MTK-END [mtk04070][111118][ALPS00093395]MTK added

    // SIM Refresh results, passed up from RIL.
    static final int SIM_REFRESH_FILE_UPDATED   = 0;  // Single file updated
    static final int SIM_REFRESH_INIT           = 1;  // SIM initialized; reload all
    static final int SIM_REFRESH_RESET          = 2;  // SIM reset; may be locked

    // GSM SMS fail cause for acknowledgeLastIncomingSMS. From TS 23.040, 9.2.3.22.
    static final int GSM_SMS_FAIL_CAUSE_MEMORY_CAPACITY_EXCEEDED    = 0xD3;
    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    static final int GSM_SMS_FAIL_CAUSE_USIM_APP_TOOLKIT_BUSY       = 0xD4;
    static final int GSM_SMS_FAIL_CAUSE_USIM_DATA_DOWNLOAD_ERROR    = 0xD5;
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    static final int GSM_SMS_FAIL_CAUSE_UNSPECIFIED_ERROR           = 0xFF;

    // CDMA SMS fail cause for acknowledgeLastIncomingCdmaSms.  From TS N.S0005, 6.5.2.125.
    static final int CDMA_SMS_FAIL_CAUSE_INVALID_TELESERVICE_ID     = 4;
    static final int CDMA_SMS_FAIL_CAUSE_RESOURCE_SHORTAGE          = 35;
    static final int CDMA_SMS_FAIL_CAUSE_OTHER_TERMINAL_PROBLEM     = 39;
    static final int CDMA_SMS_FAIL_CAUSE_ENCODING_PROBLEM           = 96;

    //MTK-START [mtk04070][111118][ALPS00093395]MTK added
    //MTK AT CMD +ESMLCK
    static final int CAT_NETWOEK                = 0;
    static final int CAT_NETOWRK_SUBSET         = 1;
    static final int CAT_SERVICE_PROVIDER       = 2;
    static final int CAT_CORPORATE              = 3;
    static final int CAT_SIM                    = 4;

    static final int OP_UNLOCK                  = 0;
    static final int OP_LOCK                    = 1;
    static final int OP_ADD                     = 2;
    static final int OP_REMOVE                  = 3;
    static final int OP_PERMANENT_UNLOCK        = 4;
    //MTK-END [mtk04070][111118][ALPS00093395]MTK added

    //***** Methods

    RadioState getRadioState();
    RadioState getSimState();
    RadioState getRuimState();
    RadioState getNvState();

    void getVoiceRadioTechnology(Message result);
    /**
     * Fires on any RadioState transition
     * Always fires immediately as well
     *
     * do not attempt to calculate transitions by storing getRadioState() values
     * on previous invocations of this notification. Instead, use the other
     * registration methods
     */
    void registerForRadioStateChanged(Handler h, int what, Object obj);
    void unregisterForRadioStateChanged(Handler h);

    void registerForVoiceRadioTechChanged(Handler h, int what, Object obj);
    void unregisterForVoiceRadioTechChanged(Handler h);

    /**
     * Fires on any transition into RadioState.isOn()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    void registerForOn(Handler h, int what, Object obj);
    void unregisterForOn(Handler h);

    /**
     * Fires on any transition out of RadioState.isAvailable()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    void registerForAvailable(Handler h, int what, Object obj);
    void unregisterForAvailable(Handler h);

    /**
     * Fires on any transition into !RadioState.isAvailable()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    void registerForNotAvailable(Handler h, int what, Object obj);
    void unregisterForNotAvailable(Handler h);

    /**
     * Fires on any transition into RADIO_OFF or !RadioState.isAvailable()
     * Fires immediately if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    void registerForOffOrNotAvailable(Handler h, int what, Object obj);
    void unregisterForOffOrNotAvailable(Handler h);

    /**
     * Fires on any transition into SIM_READY
     * Fires immediately if if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    void registerForSIMReady(Handler h, int what, Object obj);
    void unregisterForSIMReady(Handler h);

    /** Any transition into SIM_LOCKED_OR_ABSENT */
    void registerForSIMLockedOrAbsent(Handler h, int what, Object obj);
    void unregisterForSIMLockedOrAbsent(Handler h);

    void registerForIccStatusChanged(Handler h, int what, Object obj);
    void unregisterForIccStatusChanged(Handler h);

    void registerForCallStateChanged(Handler h, int what, Object obj);
    void unregisterForCallStateChanged(Handler h);
    void registerForVoiceNetworkStateChanged(Handler h, int what, Object obj);
    void unregisterForVoiceNetworkStateChanged(Handler h);
    void registerForDataNetworkStateChanged(Handler h, int what, Object obj);
    void unregisterForDataNetworkStateChanged(Handler h);

    void registerForNVReady(Handler h, int what, Object obj);
    void unregisterForNVReady(Handler h);
    void registerForRUIMLockedOrAbsent(Handler h, int what, Object obj);
    void unregisterForRUIMLockedOrAbsent(Handler h);

    /** InCall voice privacy notifications */
    void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj);
    void unregisterForInCallVoicePrivacyOn(Handler h);
    void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj);
    void unregisterForInCallVoicePrivacyOff(Handler h);

    /**
     * Fires on any transition into RUIM_READY
     * Fires immediately if if currently in that state
     * In general, actions should be idempotent. State may change
     * before event is received.
     */
    void registerForRUIMReady(Handler h, int what, Object obj);
    void unregisterForRUIMReady(Handler h);

    /**
     * unlike the register* methods, there's only one new 3GPP format SMS handler.
     * if you need to unregister, you should also tell the radio to stop
     * sending SMS's to you (via AT+CNMI)
     *
     * AsyncResult.result is a String containing the SMS PDU
     */
    void setOnNewGsmSms(Handler h, int what, Object obj);
    void unSetOnNewGsmSms(Handler h);

    /**
     * unlike the register* methods, there's only one new 3GPP2 format SMS handler.
     * if you need to unregister, you should also tell the radio to stop
     * sending SMS's to you (via AT+CNMI)
     *
     * AsyncResult.result is a String containing the SMS PDU
     */
    void setOnNewCdmaSms(Handler h, int what, Object obj);
    void unSetOnNewCdmaSms(Handler h);

    /**
     * Set the handler for SMS Cell Broadcast messages.
     *
     * AsyncResult.result is a byte array containing the SMS-CB PDU
     */
    void setOnNewGsmBroadcastSms(Handler h, int what, Object obj);
    void unSetOnNewGsmBroadcastSms(Handler h);

   /**
     * Register for NEW_SMS_ON_SIM unsolicited message
     *
     * AsyncResult.result is an int array containing the index of new SMS
     */
    void setOnSmsOnSim(Handler h, int what, Object obj);
    void unSetOnSmsOnSim(Handler h);

    /**
     * Register for NEW_SMS_STATUS_REPORT unsolicited message
     *
     * AsyncResult.result is a String containing the status report PDU
     */
    void setOnSmsStatus(Handler h, int what, Object obj);
    void unSetOnSmsStatus(Handler h);

    /**
     * unlike the register* methods, there's only one NITZ time handler
     *
     * AsyncResult.result is an Object[]
     * ((Object[])AsyncResult.result)[0] is a String containing the NITZ time string
     * ((Object[])AsyncResult.result)[1] is a Long containing the milliseconds since boot as
     *                                   returned by elapsedRealtime() when this NITZ time
     *                                   was posted.
     *
     * Please note that the delivery of this message may be delayed several
     * seconds on system startup
     */
    void setOnNITZTime(Handler h, int what, Object obj);
    void unSetOnNITZTime(Handler h);

    /**
     * unlike the register* methods, there's only one USSD notify handler
     *
     * Represents the arrival of a USSD "notify" message, which may
     * or may not have been triggered by a previous USSD send
     *
     * AsyncResult.result is a String[]
     * ((String[])(AsyncResult.result))[0] contains status code
     *      "0"   USSD-Notify -- text in ((const char **)data)[1]
     *      "1"   USSD-Request -- text in ((const char **)data)[1]
     *      "2"   Session terminated by network
     *      "3"   other local client (eg, SIM Toolkit) has responded
     *      "4"   Operation not supported
     *      "5"   Network timeout
     *
     * ((String[])(AsyncResult.result))[1] contains the USSD message
     * The numeric representations of these are in USSD_MODE_*
     */

    void setOnUSSD(Handler h, int what, Object obj);
    void unSetOnUSSD(Handler h);

    /**
     * unlike the register* methods, there's only one signal strength handler
     * AsyncResult.result is an int[2]
     * response.obj.result[0] is received signal strength (0-31, 99)
     * response.obj.result[1] is  bit error rate (0-7, 99)
     * as defined in TS 27.007 8.5
     */

    void setOnSignalStrengthUpdate(Handler h, int what, Object obj);
    void unSetOnSignalStrengthUpdate(Handler h);

    /**
     * Sets the handler for SIM/RUIM SMS storage full unsolicited message.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnIccSmsFull(Handler h, int what, Object obj);
    void unSetOnIccSmsFull(Handler h);

    /**
     * Sets the handler for SIM Refresh notifications.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForIccRefresh(Handler h, int what, Object obj);
    void unregisterForIccRefresh(Handler h);

    void setOnIccRefresh(Handler h, int what, Object obj);
    void unsetOnIccRefresh(Handler h);

    /**
     * Sets the handler for RING notifications.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnCallRing(Handler h, int what, Object obj);
    void unSetOnCallRing(Handler h);

    /**
     * Sets the handler for RESTRICTED_STATE changed notification,
     * eg, for Domain Specific Access Control
     * unlike the register* methods, there's only one signal strength handler
     *
     * AsyncResult.result is an int[1]
     * response.obj.result[0] is a bitmask of RIL_RESTRICTED_STATE_* values
     */

    void setOnRestrictedStateChanged(Handler h, int what, Object obj);
    void unSetOnRestrictedStateChanged(Handler h);

    /**
     * Sets the handler for Supplementary Service Notifications.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnSuppServiceNotification(Handler h, int what, Object obj);
    void unSetOnSuppServiceNotification(Handler h);

    /**
     * Sets the handler for Session End Notifications for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnCatSessionEnd(Handler h, int what, Object obj);
    void unSetOnCatSessionEnd(Handler h);

    /**
     * Sets the handler for Proactive Commands for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnCatProactiveCmd(Handler h, int what, Object obj);
    void unSetOnCatProactiveCmd(Handler h);

    /**
     * Sets the handler for Event Notifications for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnCatEvent(Handler h, int what, Object obj);
    void unSetOnCatEvent(Handler h);

    /**
     * Sets the handler for Call Set Up Notifications for CAT.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnCatCallSetUp(Handler h, int what, Object obj);
    void unSetOnCatCallSetUp(Handler h);

    /**
     * Enables/disbables supplementary service related notifications from
     * the network.
     *
     * @param enable true to enable notifications, false to disable.
     * @param result Message to be posted when command completes.
     */
    void setSuppServiceNotifications(boolean enable, Message result);
    //void unSetSuppServiceNotifications(Handler h);

    /**
     * Sets the handler for Event Notifications for CDMA Display Info.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForDisplayInfo(Handler h, int what, Object obj);
    void unregisterForDisplayInfo(Handler h);

    /**
     * Sets the handler for Event Notifications for CallWaiting Info.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForCallWaitingInfo(Handler h, int what, Object obj);
    void unregisterForCallWaitingInfo(Handler h);

    /**
     * Sets the handler for Event Notifications for Signal Info.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSignalInfo(Handler h, int what, Object obj);
    void unregisterForSignalInfo(Handler h);

    /**
     * Registers the handler for CDMA number information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForNumberInfo(Handler h, int what, Object obj);
    void unregisterForNumberInfo(Handler h);

    /**
     * Registers the handler for CDMA redirected number Information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForRedirectedNumberInfo(Handler h, int what, Object obj);
    void unregisterForRedirectedNumberInfo(Handler h);

    /**
     * Registers the handler for CDMA line control information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForLineControlInfo(Handler h, int what, Object obj);
    void unregisterForLineControlInfo(Handler h);

    /**
     * Registers the handler for CDMA T53 CLIR information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerFoT53ClirlInfo(Handler h, int what, Object obj);
    void unregisterForT53ClirInfo(Handler h);

    /**
     * Registers the handler for CDMA T53 audio control information record
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForT53AudioControlInfo(Handler h, int what, Object obj);
    void unregisterForT53AudioControlInfo(Handler h);

    /**
     * Fires on if Modem enters Emergency Callback mode
     */
    void setEmergencyCallbackMode(Handler h, int what, Object obj);

     /**
      * Fires on any CDMA OTA provision status change
      */
     void registerForCdmaOtaProvision(Handler h,int what, Object obj);
     void unregisterForCdmaOtaProvision(Handler h);

     /**
      * Registers the handler when out-band ringback tone is needed.<p>
      *
      *  Messages received from this:
      *  Message.obj will be an AsyncResult
      *  AsyncResult.userObj = obj
      *  AsyncResult.result = boolean. <p>
      */
     void registerForRingbackTone(Handler h, int what, Object obj);
     void unregisterForRingbackTone(Handler h);

     /**
      * Registers the handler when mute/unmute need to be resent to get
      * uplink audio during a call.<p>
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForResendIncallMute(Handler h, int what, Object obj);
     void unregisterForResendIncallMute(Handler h);

     /**
      * Registers the handler for when Cdma subscription changed events
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForCdmaSubscriptionChanged(Handler h, int what, Object obj);
     void unregisterForCdmaSubscriptionChanged(Handler h);

     /**
      * Registers the handler for when Cdma prl changed events
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForCdmaPrlChanged(Handler h, int what, Object obj);
     void unregisterForCdmaPrlChanged(Handler h);

     /**
      * Registers the handler for when Cdma prl changed events
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      *
      */
     void registerForExitEmergencyCallbackMode(Handler h, int what, Object obj);
     void unregisterForExitEmergencyCallbackMode(Handler h);

     /**
      * Registers the handler for RIL_UNSOL_RIL_CONNECT events.
      *
      * When ril connects or disconnects a message is sent to the registrant
      * which contains an AsyncResult, ar, in msg.obj. The ar.result is an
      * Integer which is the version of the ril or -1 if the ril disconnected.
      *
      * @param h Handler for notification message.
      * @param what User-defined message code.
      * @param obj User object.
      */
     void registerForRilConnected(Handler h, int what, Object obj);
     void unregisterForRilConnected(Handler h);

    /**
     * Supply the ICC PIN to the ICC card
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPin(String pin, Message result);

    /**
     * Supply the PIN for the app with this AID on the ICC card
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPinForApp(String pin, String aid, Message result);

    /**
     * Supply the ICC PUK and newPin to the ICC card
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPuk(String puk, String newPin, Message result);

    /**
     * Supply the PUK, new pin for the app with this AID on the ICC card
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPukForApp(String puk, String newPin, String aid, Message result);

    /**
     * Supply the ICC PIN2 to the ICC card
     * Only called following operation where ICC_PIN2 was
     * returned as a a failure from a previous operation
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPin2(String pin2, Message result);

    /**
     * Supply the PIN2 for the app with this AID on the ICC card
     * Only called following operation where ICC_PIN2 was
     * returned as a a failure from a previous operation
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPin2ForApp(String pin2, String aid, Message result);

    /**
     * Supply the SIM PUK2 to the SIM card
     * Only called following operation where SIM_PUK2 was
     * returned as a a failure from a previous operation
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPuk2(String puk2, String newPin2, Message result);

    /**
     * Supply the PUK2, newPin2 for the app with this AID on the ICC card
     * Only called following operation where SIM_PUK2 was
     * returned as a a failure from a previous operation
     *
     *  AID (Application ID), See ETSI 102.221 8.1 and 101.220 4
     *
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  This exception is CommandException with an error of PASSWORD_INCORRECT
     *  if the password is incorrect
     *
     * ar.exception and ar.result are null on success
     */

    void supplyIccPuk2ForApp(String puk2, String newPin2, String aid, Message result);

    void changeIccPin(String oldPin, String newPin, Message result);
    void changeIccPinForApp(String oldPin, String newPin, String aidPtr, Message result);
    void changeIccPin2(String oldPin2, String newPin2, Message result);
    void changeIccPin2ForApp(String oldPin2, String newPin2, String aidPtr, Message result);

    void changeBarringPassword(String facility, String oldPwd, String newPwd, Message result);

    void supplyNetworkDepersonalization(String netpin, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of DriverCall
     *      The ar.result List is sorted by DriverCall.index
     */
    void getCurrentCalls (Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of DataCallState
     *  @deprecated Do not use.
     */
    @Deprecated
    void getPDPContextList(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result contains a List of DataCallState
     */
    void getDataCallList(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void dial (String address, int clirMode, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void dial(String address, int clirMode, UUSInfo uusInfo, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMSI on success
     */
    void getIMSI(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMSI on success
     */
    void getIMSIForApp(String aid, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMEI on success
     */
    void getIMEI(Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is String containing IMEISV on success
     */
    void getIMEISV(Message result);

    /**
     * Hang up one individual connection.
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     *  3GPP 22.030 6.5.5
     *  "Releases a specific active call X"
     */
    void hangupConnection (int gsmIndex, Message result);

    /**
     * 3GPP 22.030 6.5.5
     *  "Releases all held calls or sets User Determined User Busy (UDUB)
     *   for a waiting call."
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void hangupWaitingOrBackground (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Releases all active calls (if any exist) and accepts
     *  the other (held or waiting) call."
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void hangupForegroundResumeBackground (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Places all active calls (if any exist) on hold and accepts
     *  the other (held or waiting) call."
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void switchWaitingOrHoldingAndActive (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Adds a held call to the conversation"
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void conference (Message result);

    /**
     * Set preferred Voice Privacy (VP).
     *
     * @param enable true is enhanced and false is normal VP
     * @param result is a callback message
     */
    void setPreferredVoicePrivacy(boolean enable, Message result);

    /**
     * Get currently set preferred Voice Privacy (VP) mode.
     *
     * @param result is a callback message
     */
    void getPreferredVoicePrivacy(Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Places all active calls on hold except call X with which
     *  communication shall be supported."
     */
    void separateConnection (int gsmIndex, Message result);

    /**
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void acceptCall (Message result);

    /**
     *  also known as UDUB
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void rejectCall (Message result);

    /**
     * 3GPP 22.030 6.5.5
     * "Connects the two calls and disconnects the subscriber from both calls"
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void explicitCallTransfer (Message result);

    /**
     * cause code returned as int[0] in Message.obj.response
     * Returns integer cause code defined in TS 24.008
     * Annex H or closest approximation.
     * Most significant codes:
     * - Any defined in 22.001 F.4 (for generating busy/congestion)
     * - Cause 68: ACM >= ACMMax
     */
    void getLastCallFailCause (Message result);


    /**
     * Reason for last PDP context deactivate or failure to activate
     * cause code returned as int[0] in Message.obj.response
     * returns an integer cause code defined in TS 24.008
     * section 6.1.3.1.3 or close approximation
     * @deprecated Do not use.
     */
    @Deprecated
    void getLastPdpFailCause (Message result);

    /**
     * The preferred new alternative to getLastPdpFailCause
     * that is also CDMA-compatible.
     */
    void getLastDataCallFailCause (Message result);

    void setMute (boolean enableMute, Message response);

    void getMute (Message response);

    /**
     * response.obj is an AsyncResult
     * response.obj.result is an int[2]
     * response.obj.result[0] is received signal strength (0-31, 99)
     * response.obj.result[1] is  bit error rate (0-7, 99)
     * as defined in TS 27.007 8.5
     */
    void getSignalStrength (Message response);


    /**
     * response.obj.result is an int[3]
     * response.obj.result[0] is registration state 0-5 from TS 27.007 7.2
     * response.obj.result[1] is LAC if registered or -1 if not
     * response.obj.result[2] is CID if registered or -1 if not
     * valid LAC and CIDs are 0x0000 - 0xffff
     *
     * Please note that registration state 4 ("unknown") is treated
     * as "out of service" above
     */
    void getVoiceRegistrationState (Message response);

    /**
     * response.obj.result is an int[3]
     * response.obj.result[0] is registration state 0-5 from TS 27.007 7.2
     * response.obj.result[1] is LAC if registered or -1 if not
     * response.obj.result[2] is CID if registered or -1 if not
     * valid LAC and CIDs are 0x0000 - 0xffff
     *
     * Please note that registration state 4 ("unknown") is treated
     * as "out of service" above
     */
    void getDataRegistrationState (Message response);

    /**
     * response.obj.result is a String[3]
     * response.obj.result[0] is long alpha or null if unregistered
     * response.obj.result[1] is short alpha or null if unregistered
     * response.obj.result[2] is numeric or null if unregistered
     */
    void getOperator(Message response);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void sendDtmf(char c, Message result);


    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void startDtmf(char c, Message result);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void stopDtmf(Message result);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void sendBurstDtmf(String dtmfString, int on, int off, Message result);

    /**
     * smscPDU is smsc address in PDU form GSM BCD format prefixed
     *      by a length byte (as expected by TS 27.005) or NULL for default SMSC
     * pdu is SMS in PDU format as an ASCII hex string
     *      less the SMSC address
     */
    void sendSMS (String smscPDU, String pdu, Message response);

    /**
     * @param pdu is CDMA-SMS in internal pseudo-PDU format
     * @param response sent when operation completes
     */
    void sendCdmaSms(byte[] pdu, Message response);

    /**
     * Deletes the specified SMS record from SIM memory (EF_SMS).
     *
     * @param index index of the SMS record to delete
     * @param response sent when operation completes
     */
    void deleteSmsOnSim(int index, Message response);

    /**
     * Deletes the specified SMS record from RUIM memory (EF_SMS in DF_CDMA).
     *
     * @param index index of the SMS record to delete
     * @param response sent when operation completes
     */
    void deleteSmsOnRuim(int index, Message response);

    /**
     * Writes an SMS message to SIM memory (EF_SMS).
     *
     * @param status status of message on SIM.  One of:
     *                  SmsManger.STATUS_ON_ICC_READ
     *                  SmsManger.STATUS_ON_ICC_UNREAD
     *                  SmsManger.STATUS_ON_ICC_SENT
     *                  SmsManger.STATUS_ON_ICC_UNSENT
     * @param pdu message PDU, as hex string
     * @param response sent when operation completes.
     *                  response.obj will be an AsyncResult, and will indicate
     *                  any error that may have occurred (eg, out of memory).
     */
    void writeSmsToSim(int status, String smsc, String pdu, Message response);

    void writeSmsToRuim(int status, String pdu, Message response);

    void setRadioPower(boolean on, Message response);
//MTK-START [mtk80601][111212][ALPS00093395]IPO feature
    void setRadioPowerOn(Message response);
//MTK-END [mtk80601][111212][ALPS00093395]IPO feature
    void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message response);

    void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message response);

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * Acknowledge successful or failed receipt of last incoming SMS,
     * including acknowledgement TPDU to send as the RP-User-Data element
     * of the RP-ACK or RP-ERROR PDU.
     *
     * @param success true to send RP-ACK, false to send RP-ERROR
     * @param ackPdu the acknowledgement TPDU in hexadecimal format
     * @param response sent when operation completes.
     */
    void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message response);
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3


    /**
     * parameters equivalent to 27.007 AT+CRSM command
     * response.obj will be an AsyncResult
     * response.obj.result will be an IccIoResult on success
     */
    void iccIO (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, Message response);

    /**
     * parameters equivalent to 27.007 AT+CRSM command
     * response.obj will be an AsyncResult
     * response.obj.userObj will be a IccIoResult on success
     */
    void iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message response);

    // NFC SEEK start
    void iccExchangeAPDU(int cla, int command, int channel, int p1, int p2,
            int p3, String data, Message response);
    void iccOpenChannel(String AID, Message response);
    void iccCloseChannel(int channel, Message response);
    void iccGetATR(Message result);
    void iccOpenChannelWithSw(String AID, Message result);
    // NFC SEEK end

    /**
     * (AsyncResult)response.obj).result is an int[] with element [0] set to
     * 1 for "CLIP is provisioned", and 0 for "CLIP is not provisioned".
     *
     * @param response is callback message
     */

    void queryCLIP(Message response);

    /**
     * response.obj will be a an int[2]
     *
     * response.obj[0] will be TS 27.007 +CLIR parameter 'n'
     *  0 presentation indicator is used according to the subscription of the CLIR service
     *  1 CLIR invocation
     *  2 CLIR suppression
     *
     * response.obj[1] will be TS 27.007 +CLIR parameter 'm'
     *  0 CLIR not provisioned
     *  1 CLIR provisioned in permanent mode
     *  2 unknown (e.g. no network, etc.)
     *  3 CLIR temporary mode presentation restricted
     *  4 CLIR temporary mode presentation allowed
     */

    void getCLIR(Message response);

    /**
     * clirMode is one of the CLIR_* constants above
     *
     * response.obj is null
     */

    void setCLIR(int clirMode, Message response);

    /**
     * (AsyncResult)response.obj).result is an int[] with element [0] set to
     * 0 for disabled, 1 for enabled.
     *
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */

    void queryCallWaiting(int serviceClass, Message response);

    /**
     * @param enable is true to enable, false to disable
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */

    void setCallWaiting(boolean enable, int serviceClass, Message response);

    /**
     * @param action is one of CF_ACTION_*
     * @param cfReason is one of CF_REASON_*
     * @param serviceClass is a sum of SERVICE_CLASSS_*
     */
    void setCallForward(int action, int cfReason, int serviceClass,
                String number, int timeSeconds, Message response);

    /**
     * cfReason is one of CF_REASON_*
     *
     * ((AsyncResult)response.obj).result will be an array of
     * CallForwardInfo's
     *
     * An array of length 0 means "disabled for all codes"
     */
    void queryCallForwardStatus(int cfReason, int serviceClass,
            String number, Message response);

    void setNetworkSelectionModeAutomatic(Message response);

    void setNetworkSelectionModeManual(String operatorNumeric, Message response);

    /**
     * Queries whether the current network selection mode is automatic
     * or manual
     *
     * ((AsyncResult)response.obj).result  is an int[] with element [0] being
     * a 0 for automatic selection and a 1 for manual selection
     */

    void getNetworkSelectionMode(Message response);

    /**
     * Queries the currently available networks
     *
     * ((AsyncResult)response.obj).result  is a List of NetworkInfo objects
     */
    void getAvailableNetworks(Message response);

    void getBasebandVersion (Message response);


    /**
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled service classes (sum of SERVICE_CLASS_*)
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */

    void queryFacilityLock (String facility, String password, int serviceClass,
        Message response);

    /**
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled service classes (sum of SERVICE_CLASS_*) for the
     * application with appId.
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param appId is application Id or null if none
     * @param response is callback message
     */

    void queryFacilityLockForApp(String facility, String password, int serviceClass, String appId,
        Message response);

    /**
     * @param facility one of CB_FACILTY_*
     * @param lockState true means lock, false means unlock
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param response is callback message
     */
    void setFacilityLock (String facility, boolean lockState, String password,
        int serviceClass, Message response);

    /**
     * Set the facility lock for the app with this AID on the ICC card.
     *
     * @param facility one of CB_FACILTY_*
     * @param lockState true means lock, false means unlock
     * @param password password or "" if not required
     * @param serviceClass is a sum of SERVICE_CLASS_*
     * @param appId is application Id or null if none
     * @param response is callback message
     */
    void setFacilityLockForApp(String facility, boolean lockState, String password,
        int serviceClass, String appId, Message response);

    void sendUSSD (String ussdString, Message response);

    /**
     * Cancels a pending USSD session if one exists.
     * @param response callback message
     */
    void cancelPendingUssd (Message response);

    void resetRadio(Message result);

    /**
     * Assign a specified band for RF configuration.
     *
     * @param bandMode one of BM_*_BAND
     * @param response is callback message
     */
    void setBandMode (int bandMode, Message response);

    /**
     * Query the list of band mode supported by RF.
     *
     * @param response is callback message
     *        ((AsyncResult)response.obj).result  is an int[] with every
     *        element representing one avialable BM_*_BAND
     */
    void queryAvailableBandMode (Message response);

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * Set the current preferred network type. This will be the last
     * networkType that was passed to setPreferredNetworkType.
     */
    void setCurrentPreferredNetworkType();
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

    /**
     *  Requests to set the preferred network type for searching and registering
     * (CS/PS domain, RAT, and operation mode)
     * @param networkType one of  NT_*_TYPE
     * @param response is callback message
     */
    void setPreferredNetworkType(int networkType , Message response);

     /**
     *  Query the preferred network type setting
     *
     * @param response is callback message to report one of  NT_*_TYPE
     */
    void getPreferredNetworkType(Message response);

    /**
     * Query neighboring cell ids
     *
     * @param response s callback message to cell ids
     */
    void getNeighboringCids(Message response);

    /**
     * Request to enable/disable network state change notifications when
     * location information (lac and/or cid) has changed.
     *
     * @param enable true to enable, false to disable
     * @param response callback message
     */
    void setLocationUpdates(boolean enable, Message response);

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
     * Indicates whether there is storage available for new SMS messages.
     * @param available true if storage is available
     * @param result callback message
     */
    void reportSmsMemoryStatus(boolean available, Message result);

    /**
     * Indicates to the vendor ril that StkService is running
     * and is ready to receive RIL_UNSOL_STK_XXXX commands.
     *
     * @param result callback message
     */
    void reportStkServiceIsRunning(Message result);

    void invokeOemRilRequestRaw(byte[] data, Message response);

    void invokeOemRilRequestStrings(String[] strings, Message response);


    /**
     * Send TERMINAL RESPONSE to the SIM, after processing a proactive command
     * sent by the SIM.
     *
     * @param contents  String containing SAT/USAT response in hexadecimal
     *                  format starting with first byte of response data. See
     *                  TS 102 223 for details.
     * @param response  Callback message
     */
    public void sendTerminalResponse(String contents, Message response);

    /**
     * Send ENVELOPE to the SIM, after processing a proactive command sent by
     * the SIM.
     *
     * @param contents  String containing SAT/USAT response in hexadecimal
     *                  format starting with command tag. See TS 102 223 for
     *                  details.
     * @param response  Callback message
     */
    public void sendEnvelope(String contents, Message response);

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * Send ENVELOPE to the SIM, such as an SMS-PP data download envelope
     * for a SIM data download message. This method has one difference
     * from {@link #sendEnvelope}: The SW1 and SW2 status bytes from the UICC response
     * are returned along with the response data.
     *
     * response.obj will be an AsyncResult
     * response.obj.result will be an IccIoResult on success
     *
     * @param contents  String containing SAT/USAT response in hexadecimal
     *                  format starting with command tag. See TS 102 223 for
     *                  details.
     * @param response  Callback message
     */
    public void sendEnvelopeWithStatus(String contents, Message response);
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3


    /**
     * Accept or reject the call setup request from SIM.
     *
     * @param accept   true if the call is to be accepted, false otherwise.
     * @param response Callback message
     */
    //MTK-START [mtk04070][111118][ALPS00093395]Add a parameter - resCode
    public void handleCallSetupRequestFromSim(boolean acceresCodept, int resCode, Message response);
    //MTK-END [mtk04070][111118][ALPS00093395]Add a parameter - resCode

    /**
     * Activate or deactivate cell broadcast SMS for GSM.
     *
     * @param activate
     *            true = activate, false = deactivate
     * @param result Callback message is empty on completion
     */
    public void setGsmBroadcastActivation(boolean activate, Message result);

    /**
     * Configure cell broadcast SMS for GSM.
     *
     * @param response Callback message is empty on completion
     */
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response);

    /**
     * Query the current configuration of cell broadcast SMS of GSM.
     *
     * @param response
     *        Callback message contains the configuration from the modem
     *        on completion
     */
    public void getGsmBroadcastConfig(Message response);

    //***** new Methods for CDMA support

    /**
     * Request the device ESN / MEID / IMEI / IMEISV.
     * "response" is const char **
     *   [0] is IMEI if GSM subscription is available
     *   [1] is IMEISV if GSM subscription is available
     *   [2] is ESN if CDMA subscription is available
     *   [3] is MEID if CDMA subscription is available
     */
    public void getDeviceIdentity(Message response);

    /**
     * Request the device MDN / H_SID / H_NID / MIN.
     * "response" is const char **
     *   [0] is MDN if CDMA subscription is available
     *   [1] is a comma separated list of H_SID (Home SID) in decimal format
     *       if CDMA subscription is available
     *   [2] is a comma separated list of H_NID (Home NID) in decimal format
     *       if CDMA subscription is available
     *   [3] is MIN (10 digits, MIN2+MIN1) if CDMA subscription is available
     */
    public void getCDMASubscription(Message response);

    /**
     * Send Flash Code.
     * "response" is is NULL
     *   [0] is a FLASH string
     */
    public void sendCDMAFeatureCode(String FeatureCode, Message response);

    /** Set the Phone type created */
    void setPhoneType(int phoneType);

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
    void setCdmaSubscriptionSource(int cdmaSubscriptionType, Message response);

    /**
     *  Requests to get the CDMA subscription srouce
     * @param response is callback message
     */
    void getCdmaSubscriptionSource(Message response);

    /**
     *  Set the TTY mode
     *
     * @param ttyMode one of the following:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param response is callback message
     */
    void setTTYMode(int ttyMode, Message response);

    /**
     *  Query the TTY mode
     * (AsyncResult)response.obj).result is an int[] with element [0] set to
     * tty mode:
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_OFF}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_FULL}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_HCO}
     * - {@link com.android.internal.telephony.Phone#TTY_MODE_VCO}
     * @param response is callback message
     */
    void queryTTYMode(Message response);

    /**
     * Setup a packet data connection On successful completion, the result
     * message will return a {@link DataCallState} object containing the connection
     * information.
     *
     * @param radioTechnology
     *            indicates whether to setup connection on radio technology CDMA
     *            (0) or GSM/UMTS (1)
     * @param profile
     *            Profile Number or NULL to indicate default profile
     * @param apn
     *            the APN to connect to if radio technology is GSM/UMTS.
     *            Otherwise null for CDMA.
     * @param user
     *            the username for APN, or NULL
     * @param password
     *            the password for APN, or NULL
     * @param authType
     *            the PAP / CHAP auth type. Values is one of SETUP_DATA_AUTH_*
     * @param protocol
     *            one of the PDP_type values in TS 27.007 section 10.1.1.
     *            For example, "IP", "IPV6", "IPV4V6", or "PPP".
     * @param result
     *            Callback message
     */
    public void setupDataCall(String radioTechnology, String profile,
            String apn, String user, String password, String authType,
            String protocol, Message result);

    /**
     * Deactivate packet data connection
     *
     * @param cid
     *            The connection ID
     * @param reason
     *            Data disconnect reason.
     * @param result
     *            Callback message is empty on completion
     */
    public void deactivateDataCall(int cid, int reason, Message result);

    /**
     * Activate or deactivate cell broadcast SMS for CDMA.
     *
     * @param activate
     *            true = activate, false = deactivate
     * @param result
     *            Callback message is empty on completion
     */
    public void setCdmaBroadcastActivation(boolean activate, Message result);

    /**
     * Configure cdma cell broadcast SMS.
     *
     * @param result
     *            Callback message is empty on completion
     */
    // TODO: Change the configValuesArray to a RIL_BroadcastSMSConfig
    public void setCdmaBroadcastConfig(int[] configValuesArray, Message result);

    /**
     * Query the current configuration of cdma cell broadcast SMS.
     *
     * @param result
     *            Callback message contains the configuration from the modem on completion
     */
    public void getCdmaBroadcastConfig(Message result);

    /**
     *  Requests the radio's system selection module to exit emergency callback mode.
     *  This function should only be called from CDMAPHone.java.
     *
     * @param response callback message
     */
    public void exitEmergencyCallbackMode(Message response);

    /**
     * Request the status of the ICC and UICC cards.
     *
     * @param result
     *          Callback message containing {@link IccCardStatus} structure for the card.
     */
    public void getIccCardStatus(Message result);

    /**
     * Return if the current radio is LTE on CDMA. This
     * is a tri-state return value as for a period of time
     * the mode may be unknown.
     *
     * @return {@link Phone#LTE_ON_CDMA_UNKNOWN}, {@link Phone#LTE_ON_CDMA_FALSE}
     * or {@link Phone#LTE_ON_CDMA_TRUE}
     */
    public int getLteOnCdmaMode();

    /**
     * Request the ISIM application on the UICC to perform the AKA
     * challenge/response algorithm for IMS authentication. The nonce string
     * and challenge response are Base64 encoded Strings.
     *
     * @param nonce the nonce string to pass with the ISIM authentication request
     * @param response a callback message with the String response in the obj field
     */
    public void requestIsimAuthentication(String nonce, Message response);

    /**
     * Notifiy that we are testing an emergency call
     */
    public void testingEmergencyCall();

    //MTK-START [mtk04070][111118][ALPS00093395]MTK proprietary methods
    //Add by mtk80372 for Barcode Number
    void registerForSN(Handler h, int what ,Object obj);
    //Add by mtk80372 for Barcode Number
    void unregisterForSN(Handler h);

    /** Call Forwarding Flag notifications */
    void registerForCallForwardingInfo(Handler h, int what, Object obj);
    void unregisterForCallForwardingInfo(Handler h);

    /** Call Related SuppSvc notifications */
    void  setOnCallRelatedSuppSvc(Handler h, int what, Object obj);
    void  unSetOnCallRelatedSuppSvc(Handler h);

    /**
     * Sets the handler for PHB ready notification
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForPhbReady(Handler h, int what, Object obj);
    void unregisterForPhbReady(Handler h);

    /**
     * Sets the handler for SMS ready notification
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSmsReady(Handler h, int what, Object obj);
    void unregisterForSmsReady(Handler h);

    /**
     * Sets the handler for ME SMS storage full unsolicited message.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setOnMeSmsFull(Handler h, int what, Object obj);
    void unSetOnMeSmsFull(Handler h);

    void changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result);

    /* vt start */
    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void vtDial (String address, int clirMode, Message result);

    /**
     *  returned message
     *  retMsg.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     *
     * CLIR_DEFAULT     == on "use subscription default value"
     * CLIR_SUPPRESSION == on "CLIR suppression" (allow CLI presentation)
     * CLIR_INVOCATION  == on "CLIR invocation" (restrict CLI presentation)
     */
    void vtDial (String address, int clirMode, UUSInfo uusInfo, Message result);

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void voiceAccept(int callId, Message result);
    /* vt end */

    /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void hangupAll (Message result);

     /**
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
    */
    void hangupAllEx (Message result);

     /**
     * result.obj = AsyncResult ar     
     *	 ar.exception carries exception on failure
     *	 ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of CCM
     * the value will be 3 bytes of hexadecimal format, 
     * ex: "00001E" indicates decimal value 30
     */
    void getCurrentCallMeter (Message result);

     /**
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of ACM
     * the value will be 3 bytes of hexadecimal format, 
     * ex: "00001E" indicates decimal value 30
     */
    void getAccumulatedCallMeter (Message result);

     /**
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of ACMMax
     * the value will be 3 bytes of hexadecimal format, 
     * ex: "00001E" indicates decimal value 30
     */
    void getAccumulatedCallMeterMaximum (Message result);

     /**
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[2]
     * ar.result[0] contain the value of currency, ex: "GBP"
     * ar.result[1] contain the value of ppu, ex: "2.66"
     */
    void getPpuAndCurrency (Message result);

     /**
     * result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * @param acmmax is the maximum value for ACM. ex: "00001E"
     * @param pin2 is necessary parameter.
     */
    void setAccumulatedCallMeterMaximum (String acmmax, String pin2, Message result);

     /**
     *result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * @param pin2 is necessary parameter.     
     */
    void resetAccumulatedCallMeter (String pin2, Message result);        

     /**
     *result.obj = AsyncResult ar	  
     *	ar.exception carries exception on failure
     *	ar.userObject contains the orignal value of result.obj
     *
     * @param currency is value of "currency". ex: "GBP"
     * @param ppu is the value of "price per unit". ex: "2.66"
     * @param pin2 is necessary parameter.
     */
    void setPpuAndCurrency (String currency, String ppu, String pin2, Message result);   

    void setRadioPowerOff(Message response);
    void setRadioMode(int mode, Message response);

    void setGprsConnType(int mode, Message response);    

    /*Add by mtk80372 for Barcode number*/
    void getMobileRevisionAndIMEI(int type, Message result);
    void setGprsTransferType(int mode, Message response);

    /**
     * response.obj will be a an int[2]
     *
     * response.obj[0] will be TS 27.007 +COLP parameter 'n'
     *  0 COLP disabled
     *  1 COLP enabled
     *
     * response.obj[1] will be TS 27.007 +COLP parameter 'm'
     *  0 COLP not provisioned
     *  1 COLP provisioned
     *  2 unknown (e.g. no network, etc.)
     */
    void getCOLP(Message response);

    /**
     * enable is true for enable / false for disable COLP (ONLY affect TE not NW)
     *
     * response.obj is null
     */
    void setCOLP(boolean enable, Message response);

    /**
     * response.obj will be a an int[1]
     *
     * response.obj[0] will be proprietary +COLR parameter 'n'
     *  0 COLR not provisioned
     *  1 COLR provisioned
     *  2 unknown (e.g. no network, etc.)
     */
    void getCOLR(Message response);

    /**
     * select network with special act type manualy
     *
     * act is a string indicate the network type. "0" for gsm, "2" for UTRAN
     */
    void setNetworkSelectionModeManualWithAct(String operatorNumeric, String act, Message response);

    void queryNetworkLock (int categrory, Message response);


    void setNetworkLock (int catagory, int lockop, String password,
        String data_imsi, String gid1, String gid2, Message response);

    /**
     * Get SMS SIM Card memory's total and used number
     *
     * @param result callback message
     */
    void getSmsSimMemoryStatus(Message result);

    void setScri(boolean forceRelease, Message response);
    //[New R8 modem FD]	
    void setFDMode(int mode, int parameter1, int parameter2, Message response);	

    public void setScriResult(Handler h, int what, Object obj);

    public void unSetScriResult(Handler h);

    public void setGprsDetach(Handler h, int what, Object obj);
    
    public void unSetGprsDetach(Handler h);

    /* Add by mtk01411 for Multiple PDP Contexts */
    public void setupDataCall(String radioTechnology, String profile, String apn,
            String user, String password, String authType, String protocol, String requestCid, Message result); 


    /**
     * Request the information of the given storage type
     *
     * @param type
     *          the type of the storage, refer to PHB_XDN defined in the RilConstants
     * @param response
     *          Callback message
     *          response.obj.result is an int[4]
     *          response.obj.result[0] is number of current used entries
     *          response.obj.result[1] is number of total entries in the storage
     *          response.obj.result[2] is maximum supported length of the number
     *          response.obj.result[3] is maximum supported length of the alphaId
     */
    public void queryPhbStorageInfo(int type, Message response);

    /**
     * Request update a PHB entry using the given {@link PhbEntry}
     *
     * @param entry a PHB entry strucutre {@link PhbEntry}
     *          when one of the following occurs, it means delete the entry.
     *          1. entry.number is NULL
     *          2. entry.number is empty and entry.ton = 0x91
     *          3. entry.alphaId is NULL
     *          4. both entry.number and entry.alphaId are empty.
     * @param result
     *          Callback message containing if the action is success or not.
     */
    public void writePhbEntry(PhbEntry entry, Message result);

    /**
     * Request read PHB entries from the given storage
     * @param type 
     *          the type of the storage, refer to PHB_* defined in the RilConstants
     * @param bIndex
     *          the begin index of the entries to be read
     * @param eIndex
     *          the end index of the entries to be read, note that the (eIndex - bIndex +1)
     *          should not exceed the value RilConstants.PHB_MAX_ENTRY
     *
     * @param response
     *          Callback message containing an array of {@link PhbEntry} structure.
     */
    public void ReadPhbEntry(int type, int bIndex, int eIndex, Message response);

    /**
     * Send BT SIM profile
     * @param nAction 
     *          the type of the action
     *          0: Connect 
     *          1: Disconnect
     *          2: Power On
     *          3: Power Off
     *          4: Reset
     *          5: APDU
     * @param nType 
     *          Indicate which transport protocol is the preferred one
     *          0x00 : T=0
     *          0x01 : T=1
     * @param strData
     *          Only be used when action is APDU transfer 
     * @param response
     *          Callback message containing response structure.
     */
     public void sendBTSIMProfile(int nAction, int nType, String strData, Message response);

    /**
     * Query the IccId of the ICC or UICC card.
     *
     * @param response
     *          Callback message containing a IccId String for the card.
     */
    public void queryIccId(Message result);

    /**
     * unlike the register* methods, there's only one Neighboring cell info handler
     *
     * AsyncResult.result is an Object[]
     * ((Object[])AsyncResult.result)[0] is a String containing the RAT
     * ((Object[])AsyncResult.result)[1] is a String containing the neighboring cell info raw data
     *
     * Please note that the delivery of this message may be delayed several
     * seconds on system startup
     */
    void registerForNeighboringInfo(Handler h, int what, Object obj);
    void unregisterForNeighboringInfo(Handler h);

    /**
     * unlike the register* methods, there's only one Network info handler
     *
     * AsyncResult.result is an Object[]
     * ((Object[])AsyncResult.result)[0] is a String containing the type
     * ((Object[])AsyncResult.result)[1] is a String contain the network info raw data
     *
     * Please note that the delivery of this message may be delayed several
     * seconds on system startup
     */
    void registerForNetworkInfo(Handler h, int what, Object obj);
    void unregisterForNetworkInfo(Handler h);

    /**
     * used to register to +ECPI URC for call state change.
     *
     * msg.obj is an AsyncResult
     * ar.result is a String[]
     * Please see handleCallProgressInfo() for detail information of result
     */
    void registerForCallProgressInfo(Handler h, int what, Object obj);
    void unregisterForCallProgressInfo(Handler h);

    /**
     * used to register to +ESPEECH URC for audio on/off.
     *
     * msg.obj is an AsyncResult
     * ar.result is a String[]
     * String[0] is on_off
     * String[1] is rat
     * String[2] is irho_on_off
     */
    void registerForSpeechInfo(Handler h, int what, Object obj);
    void unregisterForSpeechInfo(Handler h);

    /* vt start */
    /**
     * used to register to +EVTSTATUS URC for VT status.
     *
     * msg.obj is an AsyncResult
     * ar.result is a int[]
     * String[0] is on_off
     */
    void registerForVtStatusInfo(Handler h, int what, Object obj);
    void unregisterForVtStatusInfo(Handler h);

    /**
     * used to register to +CRING: VIDEO URC for MT VT call.
     *
     * msg.obj is an AsyncResult
     */
    void registerForVtRingInfo(Handler h, int what, Object obj);
    void unregisterForVtRingInfo(Handler h);
    /* vt end */

    /**
     * used to register SIM inserted status change
     *
     * msg.obj is an AsyncResult
     * ar.result is a String[]
     */
    void registerForSimInsertedStatus(Handler h, int what, Object obj);
    void unregisterForSimInsertedStatus(Handler h);

    /**
     * used to register SIM missing status change
     *
     * msg.obj is an AsyncResult
     * ar.result is a String[]
     */
    void registerForSimMissing(Handler h, int what, Object obj);
    void unregisterForSimMissing(Handler h);

	void registerForSimRecovery(Handler h, int what, Object obj);
    void unregisterForSimRecovery(Handler h);
	
    public void registerForVirtualSimOn(Handler h, int what, Object obj);
    public void unregisterForVirtualSimOn(Handler h);

    public void registerForVirtualSimOff(Handler h, int what, Object obj);
    public void unregisterForVirtualSimOff(Handler h);
	
    /**
     * Request 2G context authentication for SIM/USIM
     */
    public void doSimAuthentication (String strRand, Message response);

    /**
     * Request 3G context authentication for USIM
     */
    public void doUSimAuthentication (String strRand, String strAutn,  Message response);   

    /**
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void forceReleaseCall(int index, Message result);

    /**
     * used to register to +EAIC URC for call state change.
     *
     * msg.obj is an AsyncResult
     * ar.result is a String[]
     */
    void setOnIncomingCallIndication(Handler h, int what, Object obj);
    void unsetOnIncomingCallIndication(Handler h);

    /**
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void setCallIndication(int mode, int callId, int seqNumber, Message result);

    /**
     *
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *  ar.result is null on success and failure
     */
    void replaceVtCall(int index, Message result);

    void get3GCapabilitySIM(Message response);
    void set3GCapabilitySIM(int simId, Message response);
    
    void getPOLCapabilty(Message response);
    void getCurrentPOLList(Message response);
    void setPOLEntry(int index, String numeric, int nAct, Message response);
    /* UPB start */
    void queryUPBCapability(Message response);
    void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal, String tonForNum, Message response);
    void deleteUPBEntry(int entryType, int adnIndex, int entryIndex, Message response);
    void readUPBGasList(int startIndex, int endIndex, Message response);
    void readUPBGrpEntry(int adnIndex, Message response);
    void writeUPBGrpEntry(int adnIndex, int[] grpIds, Message response);
    /* UPB end */
    //MTK-END [mtk04070][111118][ALPS00093395]MTK proprietary methods
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /**
     * unlike the register* methods, there's only one new SMS handler
     * if you need to unregister, you should also tell the radio to stop
     * sending SMS's to you (via AT+CNMI)
     *
     * AsyncResult.result is a String containing the SMS PDU
     */
    void setOnNewSMS(Handler h, int what, Object obj);
    void unSetOnNewSMS(Handler h);
    void setPreferredNetworkTypeRIL(int NetworkType);

    void setSimRecoveryOn(int Type, Message response);
    void getSimRecoveryOn(Message response);

    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    //MTK-START [mtkXXXXX][120208][APLS00109092] Replace "RIL_UNSOL_SIM_MISSING in RIL.java" with "acively query SIM missing status"
    /**
     * prior modem could notify +ESIMs: 0,0, and trigger RIL_UNSOL_SIM_MISSING
     * however, current modem disable this feature
     * now, add this feature to notify RIL_UNSOL_SIM_MISSING of RIL
     */
    void detectSimMissing(Message result);
    void notifySimMissing();
    //MTK-END [mtkXXXXX][120208][APLS00109092] Replace "RIL_UNSOL_SIM_MISSING in RIL.java" with "acively query SIM missing status"

    void registerForPsNetworkStateChanged(Handler h, int what, Object obj);
    void unregisterForPsNetworkStateChanged(Handler h);   

    /**
     * Sets the handler for event notifications for SIM plug-out event.
     * 
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSimPlugOut(Handler h, int what, Object obj);

    /**
     * Unregister the handler for event notifications for SIM plug-out event.
     * 
     * @param h Handler for notification message.
     */
    void unregisterForSimPlugOut(Handler h);

    /**
     * Sets the handler for event notifications for SIM plug-in event.
     * 
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void registerForSimPlugIn(Handler h, int what, Object obj);

    /**
     * Unregister the handler for event notifications for SIM plug-in event.
     * 
     * @param h Handler for notification message.
     */
    void unregisterForSimPlugIn(Handler h);

   //ALPS00248788 BEGIN
   /**
     * Sets the handler for Invalid SIM unsolicited message.
     * Unlike the register* methods, there's only one notification handler
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    void setInvalidSimInfo(Handler h, int what, Object obj);
    void unSetInvalidSimInfo(Handler h);
    void setTRM(int mode, Message result);
   //ALPS00248788 END

    // ALPS00302698 ENS
    void registerForEfCspPlmnModeBitChanged(Handler h, int what, Object obj);
    void unregisterForEfCspPlmnModeBitChanged(Handler h);	

    public void registerForIMEILock(Handler h, int what, Object obj);
    public void unregisterForIMEILock(Handler h);	

    String lookupOperatorName(String numeric, boolean desireLongName);

    //MTK-START [mtk80950][120410][ALPS00266631] check whether download calibration data or not
    void getCalibrationData(Message result);
    //MTK-END [mtk80950][120410][ALPS00266631] check whether download calibration data or not

    // ADD BY via start
    void getNitzTime (Message result);
    void queryUimInsertedStatus(Message result);
    void notifyUimInsertedStatus(int status);	
    // ADD BY via end
    void getPhoneBookStringsLength(Message result);
    void getPhoneBookMemStorage(Message result);
    void setPhoneBookMemStorage(String storage, String password, Message result);
    void readPhoneBookEntryExt(int index1, int index2, Message result);
    void writePhoneBookEntryExt(PBEntry entry, Message result);

    /*
    * Get sms parameters from EFsmsp
    */
    void getSmsParameters(Message response);
    
    /*
    * Set sms parameters into EFsmsp
    */
    void setSmsParameters(SmsParameters params, Message response);

    // MVNO-API
    void setPhoneComponent(Phone phone);

    // get Available network informaitons API
    void registerForGetAvailableNetworksDone(Handler h, int what, Object obj);
    void unregisterForGetAvailableNetworksDone(Handler h);
    boolean isGettingAvailableNetworks();
    
    void setEtws(int mode, Message result);
    
    void setOnEtwsNotification(Handler h, int what, Object obj);
    void unSetOnEtwsNotification(Handler h);
    
    void setCellBroadcastChannelConfigInfo(String config, int cb_set_type, Message response);
    void setCellBroadcastLanguageConfigInfo(String config, Message response);
    void queryCellBroadcastConfigInfo(Message response);
    void setAllCbLanguageOn(Message response);
    void detachPS(Message response);
}
