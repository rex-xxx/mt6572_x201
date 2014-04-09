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

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.AdnRecordCache;
import com.android.internal.telephony.AdnRecordLoader;
import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.IccRecords;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.IccVmFixedException;
import com.android.internal.telephony.IccVmNotSupportedException;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.IccRefreshResponse;
import com.android.internal.telephony.UiccCardApplication;
import com.android.internal.telephony.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.IccCardApplicationStatus.AppType;


import java.util.ArrayList;
//MTK-START [mtk80601][111215][ALPS00093395]
import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA_2;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2;
import android.provider.Telephony.Sms.Intents;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.content.ContentUris;
import android.content.ContentValues;
import android.provider.Settings;
import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.featureoption.FeatureOption;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gsm.SpnOverride; // MVNO-API
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.PhoneFactory;
//MTK-END [mtk80601][111215][ALPS00093395]

import static android.provider.Telephony.Intents.ACTION_REMOVE_IDLE_TEXT;
import static android.provider.Telephony.Intents.ACTION_REMOVE_IDLE_TEXT_2;

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.ITelephonyExt;
import com.mediatek.common.telephony.ISimInfoUpdate;
import com.android.internal.telephony.DefaultSIMSettings;

/**
 * {@hide}
 */
public class SIMRecords extends IccRecords {
    protected static final String LOG_TAG = "GSM";
    static public final String INTENT_KEY_SIM_COUNT = "simCount";
    private static final boolean CRASH_RIL = false;

    protected static final boolean DBG = true;

    private BroadcastReceiver mSimReceiver;
    // ***** Instance Variables

    VoiceMailConstants mVmConfig;


    SpnOverride mSpnOverride;

    // ***** Cached SIM State; cleared on channel close

    private boolean callForwardingEnabled;


    String cphsOnsl;
    String cphsOnss;
    private int iccIdQueryState = -1; // -1: init, 0: query error, 1: query successful
    private boolean hasQueryIccId;
    /**
     * States only used by getSpnFsm FSM
     */
    private Get_Spn_Fsm_State spnState;

    /** CPHS service information (See CPHS 4.2 B.3.1.1)
     *  It will be set in onSimReady if reading GET_CPHS_INFO successfully
     *  mCphsInfo[0] is CPHS Phase
     *  mCphsInfo[1] and mCphsInfo[2] is CPHS Service Table
     */
    private byte[] mCphsInfo = null;
    boolean mCspPlmnEnabled = true;
    private int efLanguageToLoad = 0;

    byte[] efMWIS = null;
    byte[] efCPHS_MWI =null;
    byte[] mEfCff = null;
    byte[] mEfCfis = null;
    byte[] mEfSST = null;
    byte[] mEfLI = null;
    byte[] mEfELP = null;

    int spnDisplayCondition;
    // Numeric network codes listed in TS 51.011 EF[SPDI]
    ArrayList<String> spdiNetworks = null;

    String pnnHomeName = null;
    UsimServiceTable mUsimServiceTable;

    static final String[] SIMRECORD_PROPERTY_RIL_PHB_READY  = {
        "gsm.sim.ril.phbready",
        "gsm.sim.ril.phbready.2",
        "gsm.sim.ril.phbready.3",
        "gsm.sim.ril.phbready.4"
    };  
    static final String PROPERTY_3G_SWITCH = "gsm.3gswitch"; 
    private boolean mPhbReady = false;
    private boolean mSIMInfoReady = false;

    private Phone mPhone;
    
//MTK-START [mtk80601][111215][ALPS00093395]
    public static class OperatorName {
        public String sFullName;
        public String sShortName;
    }

    /*Operator list recode
    * include numeric mcc mnc code 
    * and a range of LAC, the operator name index in PNN
    */
    public static class OplRecord {
        public String sPlmn;
        public int nMinLAC;
        public int nMaxLAC;
        public int nPnnIndex;
    }

    //Operator name listed in TS 51.011 EF[PNN] for plmn in operator list(EF[OPL])
    private ArrayList<OperatorName> mPnnNetworkNames = null;
    //Operator list in TS 51.011 EF[OPL]
    private ArrayList<OplRecord> mOperatorList = null;

    String mEfEcc = null;
    boolean bEccRequired = false;

    private String spNameInEfSpn = null; // MVNO-API
//MTK-END [mtk80601][111215][ALPS00093395]

    private boolean mIsWaitingLocale = false; //ALPS00288486
    // ***** Constants

    // Bitmasks for SPN display rules.
    static final int SPN_RULE_SHOW_SPN  = 0x01;
    static final int SPN_RULE_SHOW_PLMN = 0x02;

    // From TS 51.011 EF[SPDI] section
    static final int TAG_SPDI = 0xA3;
    static final int TAG_SPDI_PLMN_LIST = 0x80;

    // Full Name IEI from TS 24.008
    static final int TAG_FULL_NETWORK_NAME = 0x43;

    // Short Name IEI from TS 24.008
    static final int TAG_SHORT_NETWORK_NAME = 0x45;

    // active CFF from CPHS 4.2 B.4.5
    static final int CFF_UNCONDITIONAL_ACTIVE = 0x0a;
    static final int CFF_UNCONDITIONAL_DEACTIVE = 0x05;
    static final int CFF_LINE1_MASK = 0x0f;
    static final int CFF_LINE1_RESET = 0xf0;

    // CPHS Service Table (See CPHS 4.2 B.3.1)
    private static final int CPHS_SST_MBN_MASK = 0x30;
    private static final int CPHS_SST_MBN_ENABLED = 0x30;

    // ***** Event Constants

    private static final int EVENT_APP_READY = 1;
    private static final int EVENT_GET_IMSI_DONE = 3;
    private static final int EVENT_GET_ICCID_DONE = 4;
    private static final int EVENT_GET_MBI_DONE = 5;
    private static final int EVENT_GET_MBDN_DONE = 6;
    private static final int EVENT_GET_MWIS_DONE = 7;
    private static final int EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE = 8;
    protected static final int EVENT_GET_AD_DONE = 9; // Admin data on SIM
    protected static final int EVENT_GET_MSISDN_DONE = 10;
    private static final int EVENT_GET_CPHS_MAILBOX_DONE = 11;
    private static final int EVENT_GET_SPN_DONE = 12;
    private static final int EVENT_GET_SPDI_DONE = 13;
    private static final int EVENT_UPDATE_DONE = 14;
    private static final int EVENT_GET_PNN_DONE = 15;
    protected static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_SET_MBDN_DONE = 20;
    private static final int EVENT_SMS_ON_SIM = 21;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_CFF_DONE = 24;
    private static final int EVENT_SET_CPHS_MAILBOX_DONE = 25;
    private static final int EVENT_GET_INFO_CPHS_DONE = 26;
    private static final int EVENT_SET_MSISDN_DONE = 30;
    private static final int EVENT_SIM_REFRESH = 31;
    private static final int EVENT_GET_CFIS_DONE = 32;
    private static final int EVENT_GET_CSP_CPHS_DONE = 33;
    private static final int EVENT_RADIO_AVAILABLE = 41;
    private static final int EVENT_GET_LI_DONE = 42;
    private static final int EVENT_GET_ELP_DONE = 43;
    
    /*
      Detail description:
      This feature provides a interface to get menu title string from EF_SUME
    */
    private static final int EVENT_QUERY_MENU_TITLE_DONE = 53;
    private String mMenuTitleFromEf = null;

//MTK-START [mtk80601][111215][ALPS00093395]
    private static final int EVENT_GET_SIM_ECC_DONE = 102;
    private static final int EVENT_GET_USIM_ECC_DONE = 103;
    private static final int EVENT_GET_ALL_OPL_DONE = 104;
    private static final int EVENT_GET_CPHSONS_DONE = 105;
    private static final int EVENT_GET_SHORT_CPHSONS_DONE = 106;
    private static final int EVENT_QUERY_ICCID_DONE = 107;
    private static final int EVENT_RADIO_STATE_CHANGED = 201;
    private static final int EVENT_PHB_READY = 202;
    private static final int EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED = 203; // ALPS00302698 ENS
    private static final int EVENT_GET_RAT_DONE = 204; // ALPS00302702 RAT balancing
    private static final int EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP = 205;
    /*MTK proprietary end */

    private static final int[] simServiceNumber = {
        1, 17, 51, 52, 54, 55, 56, 0, 0
    };

    private static final int[] usimServiceNumber = {
        0, 19, 45, 46, 48, 49, 51, 71, 0
    };
//MTK-END [mtk80601][111215][ALPS00093395]
    // Lookup table for carriers known to produce SIMs which incorrectly indicate MNC length.

    private static final String[] MCCMNC_CODES_HAVING_3DIGITS_MNC = {
        "405025", "405026", "405027", "405028", "405029", "405030", "405031", "405032",
        "405033", "405034", "405035", "405036", "405037", "405038", "405039", "405040",
        "405041", "405042", "405043", "405044", "405045", "405046", "405047", "405750",
        "405751", "405752", "405753", "405754", "405755", "405756", "405799", "405800",
        "405801", "405802", "405803", "405804", "405805", "405806", "405807", "405808",
        "405809", "405810", "405811", "405812", "405813", "405814", "405815", "405816",
        "405817", "405818", "405819", "405820", "405821", "405822", "405823", "405824",
        "405825", "405826", "405827", "405828", "405829", "405830", "405831", "405832",
        "405833", "405834", "405835", "405836", "405837", "405838", "405839", "405840",
        "405841", "405842", "405843", "405844", "405845", "405846", "405847", "405848",
        "405849", "405850", "405851", "405852", "405853", "405875", "405876", "405877",
        "405878", "405879", "405880", "405881", "405882", "405883", "405884", "405885",
        "405886", "405908", "405909", "405910", "405911", "405912", "405913", "405914",
        "405915", "405916", "405917", "405918", "405919", "405920", "405921", "405922",
        "405923", "405924", "405925", "405926", "405927", "405928", "405929", "405930",
        "405931", "405932"
    };

    private static final String ACTION_SIM_FILES_CHANGED = "android.intent.action.sim.SIM_FILES_CHANGED";
    private static final String ACTION_SIM_FILES_CHANGED_2 = "android.intent.action.sim.SIM_FILES_CHANGED_2";
    private static final String KEY_SIM_ID = "SIM_ID";
    
    private static final String ACTION_RESET_MODEM = "android.intent.action.sim.ACTION_RESET_MODEM";

    // ALPS00302702 RAT balancing
    private boolean mEfRatLoaded = false;
    private byte[] mEfRat = null;
	
    private static final String[] LANGUAGE_CODE_FOR_LP = {
        "de", "en", "it", "fr", "es", "nl", "sv", "da", "pt", "fi", 
        "no", "el", "tr", "hu", "pl", "", 
        "cs", "he", "ar", "ru", "is", "", "", "", "", "", 
        "", "", "", "", "", ""
    };
    // ALPS00301018
    private boolean isValidMBI = false;

    private int mSimId;

    private ITelephonyExt mTelephonyExt;

    // ***** Constructor

    public SIMRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);

        mSimId = app.getMySimId();
        if(DBG) log("SIMRecords construct");

        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            mPhone = ((GeminiPhone)(PhoneFactory.getDefaultPhone())).getPhonebyId(mSimId);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        
        adnCache = new AdnRecordCache(mFh);
        ///M: Move UPBM code to here for phone restart event to contacts app.begin
        Intent intent = new Intent();
        intent.setAction("android.intent.action.ACTION_PHONE_RESTART");
        intent.putExtra("SimId", mSimId);
        mContext.sendBroadcast(intent);
        ///M: end
        mVmConfig = new VoiceMailConstants();
        //mSpnOverride = new SpnOverride();

        recordsRequested = false;  // No load request is made till SIM ready

        // recordsToLoad is set to 0 because no requests are made yet
        recordsToLoad = 0;

        cphsOnsl = null;
        cphsOnss = null;        
        hasQueryIccId = false;


        mCi.setOnSmsOnSim(this, EVENT_SMS_ON_SIM, null);
        mCi.registerForIccRefresh(this, EVENT_SIM_REFRESH, null);
        mCi.registerForPhbReady(this, EVENT_PHB_READY, null);
        mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);
        mCi.registerForAvailable(this, EVENT_RADIO_AVAILABLE, null);
        mCi.registerForEfCspPlmnModeBitChanged(this, EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED, null); 
        // Start off by setting empty state
        resetRecords();
        mParentApp.registerForReady(this, EVENT_APP_READY, null);

        mSimReceiver = new SIMBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mediatek.dm.LAWMO_WIPE");
        filter.addAction("action_pin_dismiss");
        filter.addAction("action_melock_dismiss");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        filter.addAction(ACTION_RESET_MODEM);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);  //ALPS00288486	
        mContext.registerReceiver(mSimReceiver, filter);

        IntentFilter phbFilter = new IntentFilter();
        phbFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        phbFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        phbFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        phbFilter.addAction(TelephonyIntents.ACTION_IVSR_NOTIFY);
        mContext.registerReceiver(mHandlePhbReadyReceiver, phbFilter);

        //ALPS00566446: Check if phb is ready or not, if phb was already ready, we won't wait for phb ready.
        if(isPhbReady()) {
            if(DBG) log("Phonebook is ready.");
            broadcastPhbStateChangedIntent(mPhbReady);
        }
        
        try{
            mTelephonyExt = MediatekClassFactory.createInstance(ITelephonyExt.class);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        //Unregister for all events
        if(DBG) log("dispose");
        mCi.unregisterForIccRefresh(this);
        mCi.unregisterForPhbReady(this);
        mCi.unregisterForRadioStateChanged(this);     
        mCi.unregisterForEfCspPlmnModeBitChanged(this); 
        mParentApp.unregisterForReady(this);
        mContext.unregisterReceiver(mSimReceiver);
        mContext.unregisterReceiver(mHandlePhbReadyReceiver);
        resetRecords();
        adnCache.reset();
        iccid = null;
        mImsi = null;
        super.dispose();
    }

    protected void finalize() {
        if(DBG) log("finalized");
    }

    protected void resetRecords() {
        //mImsi = null;  //[ALPS00127136]
        msisdn = null;
        voiceMailNum = null;
        countVoiceMessages = 0;
        mncLength = UNINITIALIZED;
        //iccid = null;
        // -1 means no EF_SPN found; treat accordingly.
        spnDisplayCondition = -1;
        efMWIS = null;
        efCPHS_MWI = null;
        spdiNetworks = null;
        pnnHomeName = null;
        isValidMBI = false; // ALPS00301018

        if (!mCi.getRadioState().isAvailable()) {
            /* could be BT SAP connection, SIM refresh rest, or something happend
             * which might cause the contacts in the SIM card be changed
             * so we need to reset the contacts */    
        //    adnCache.reset();
        }

        if (mSimId == PhoneConstants.GEMINI_SIM_1) {
            SystemProperties.set(PROPERTY_ICC_OPERATOR_NUMERIC, null);
            SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA, null);
            SystemProperties.set(PROPERTY_ICC_OPERATOR_ISO_COUNTRY, null);
            SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME, null);
        } else {
            SystemProperties.set(PROPERTY_ICC_OPERATOR_NUMERIC_2, null);
            SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA_2, null);
            SystemProperties.set(PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2, null);
            SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2, null);
        }

        // recordsRequested is set to false indicating that the SIM
        // read requests made so far are not valid. This is set to
        // true only when fresh set of read requests are made.
        recordsRequested = false;
    }


    //***** Public Methods

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIMSI() {
        log("getIMSI: " + mImsi); // MVNO-API
        return mImsi;
    }

    public String getMsisdnNumber() {
        return msisdn;
    }

    @Override
    public UsimServiceTable getUsimServiceTable() {
        return mUsimServiceTable;
    }

    /**
     * Set subscriber number to SIM record
     *
     * The subscriber number is stored in EF_MSISDN (TS 51.011)
     *
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param alphaTag alpha-tagging of the dailing nubmer (up to 10 characters)
     * @param number dailing nubmer (up to 20 digits)
     *        if the number starts with '+', then set to international TOA
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public void setMsisdnNumber(String alphaTag, String number,
            Message onComplete) {

        msisdn = number;
        msisdnTag = alphaTag;

        if(DBG) log("Set MSISDN: " + msisdnTag + " " + /*msisdn*/ "xxxxxxx");


        AdnRecord adn = new AdnRecord(msisdnTag, msisdn);

        new AdnRecordLoader(mFh).updateEF(adn, EF_MSISDN, EF_EXT1, 1, null,
                obtainMessage(EVENT_SET_MSISDN_DONE, onComplete));
    }

    public String getMsisdnAlphaTag() {
        return msisdnTag;
    }

    public String getVoiceMailNumber() {
        return voiceMailNum;
    }

    /**
     * Set voice mail number to SIM record
     *
     * The voice mail number can be stored either in EF_MBDN (TS 51.011) or
     * EF_MAILBOX_CPHS (CPHS 4.2)
     *
     * If EF_MBDN is available, store the voice mail number to EF_MBDN
     *
     * If EF_MAILBOX_CPHS is enabled, store the voice mail number to EF_CHPS
     *
     * So the voice mail number will be stored in both EFs if both are available
     *
     * Return error only if both EF_MBDN and EF_MAILBOX_CPHS fail.
     *
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param alphaTag alpha-tagging of the dailing nubmer (upto 10 characters)
     * @param voiceNumber dailing nubmer (upto 20 digits)
     *        if the number is start with '+', then set to international TOA
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public void setVoiceMailNumber(String alphaTag, String voiceNumber,
            Message onComplete) {
        if (isVoiceMailFixed) {
            AsyncResult.forMessage((onComplete)).exception =
                    new IccVmFixedException("Voicemail number is fixed by operator");
            onComplete.sendToTarget();
            return;
        }

        newVoiceMailNum = voiceNumber;
        newVoiceMailTag = alphaTag;

        AdnRecord adn = new AdnRecord(newVoiceMailTag, newVoiceMailNum);

        if (mailboxIndex != 0 && mailboxIndex != 0xff) {

            new AdnRecordLoader(mFh).updateEF(adn, EF_MBDN, EF_EXT6,
                    mailboxIndex, null,
                    obtainMessage(EVENT_SET_MBDN_DONE, onComplete));

        } else if (isCphsMailboxEnabled()) {

            new AdnRecordLoader(mFh).updateEF(adn, EF_MAILBOX_CPHS,
                    EF_EXT1, 1, null,
                    obtainMessage(EVENT_SET_CPHS_MAILBOX_DONE, onComplete));

        } else {
            AsyncResult.forMessage((onComplete)).exception =
                    new IccVmNotSupportedException("Update SIM voice mailbox error");
            onComplete.sendToTarget();
        }
    }

    public String getVoiceMailAlphaTag()
    {
        return voiceMailTag;
    }

    /**
     * Sets the SIM voice message waiting indicator records
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    public void
    setVoiceMessageWaiting(int line, int countWaiting) {
        if (line != 1) {
            // only profile 1 is supported
            return;
        }

        // range check
        if (countWaiting < 0) {
            countWaiting = -1;
        } else if (countWaiting > 0xff) {
            // TS 23.040 9.2.3.24.2
            // "The value 255 shall be taken to mean 255 or greater"
            countWaiting = 0xff;
        }

        countVoiceMessages = countWaiting;

        mRecordsEventsRegistrants.notifyResult(EVENT_MWI);

        try {
            if (efMWIS != null) {
                // TS 51.011 10.3.45

                // lsb of byte 0 is 'voicemail' status
                efMWIS[0] = (byte)((efMWIS[0] & 0xfe)
                                    | (countVoiceMessages == 0 ? 0 : 1));

                // byte 1 is the number of voice messages waiting
                if (countWaiting < 0) {
                    // The spec does not define what this should be
                    // if we don't know the count
                    efMWIS[1] = 0;
                } else {
                    efMWIS[1] = (byte) countWaiting;
                }

                mFh.updateEFLinearFixed(
                    EF_MWIS, 1, efMWIS, null,
                    obtainMessage (EVENT_UPDATE_DONE, EF_MWIS));
            }

            if (efCPHS_MWI != null) {
                    // Refer CPHS4_2.WW6 B4.2.3
                efCPHS_MWI[0] = (byte)((efCPHS_MWI[0] & 0xf0)
                            | (countVoiceMessages == 0 ? 0x5 : 0xa));

                mFh.updateEFTransparent(
                    EF_VOICE_MAIL_INDICATOR_CPHS, efCPHS_MWI,
                    obtainMessage (EVENT_UPDATE_DONE, EF_VOICE_MAIL_INDICATOR_CPHS));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            logw("Error saving voice mail state to SIM. Probably malformed SIM record", ex);
        }
    }

    // Validate data is !null and the MSP (Multiple Subscriber Profile)
    // byte is between 1 and 4. See ETSI TS 131 102 v11.3.0 section 4.2.64.
    private boolean validEfCfis(byte[] data) {
        return ((data != null) && (data[0] >= 1) && (data[0] <= 4));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getVoiceCallForwardingFlag() {
        return callForwardingEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVoiceCallForwardingFlag(int line, boolean enable) {

        if (line != 1) return; // only line 1 is supported

        callForwardingEnabled = enable;

        mRecordsEventsRegistrants.notifyResult(EVENT_CFI);

        // We don't update EF_CFU here because modem already done.
        /*
        try {
            if (validEfCfis(mEfCfis)) {
                // lsb is of byte 1 is voice status
                if (enable) {
                    mEfCfis[1] |= 1;
                } else {
                    mEfCfis[1] &= 0xfe;
                }

                log("setVoiceCallForwardingFlag: enable=" + enable
                        + " mEfCfis=" + IccUtils.bytesToHexString(mEfCfis));

                // TODO: Should really update other fields in EF_CFIS, eg,
                // dialing number.  We don't read or use it right now.

                mFh.updateEFLinearFixed(
                        EF_CFIS, 1, mEfCfis, null,
                        obtainMessage (EVENT_UPDATE_DONE, EF_CFIS));
            } else {
                log("setVoiceCallForwardingFlag: ignoring enable=" + enable
                        + " invalid mEfCfis=" + IccUtils.bytesToHexString(mEfCfis));
            }

            if (mEfCff != null) {
                if (enable) {
                    mEfCff[0] = (byte) ((mEfCff[0] & CFF_LINE1_RESET)
                            | CFF_UNCONDITIONAL_ACTIVE);
                } else {
                    mEfCff[0] = (byte) ((mEfCff[0] & CFF_LINE1_RESET)
                            | CFF_UNCONDITIONAL_DEACTIVE);
                }

                mFh.updateEFTransparent(
                        EF_CFF_CPHS, mEfCff,
                        obtainMessage (EVENT_UPDATE_DONE, EF_CFF_CPHS));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            logw("Error saving call fowarding flag to SIM. "
                            + "Probably malformed SIM record", ex);

        }
        */
    }

    /**
     * Called by STK Service when REFRESH is received.
     * @param fileChanged indicates whether any files changed
     * @param fileList if non-null, a list of EF files that changed
     */
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            // A future optimization would be to inspect fileList and
            // only reload those files that we care about.  For now,
            // just re-fetch all SIM records that we cache.
            fetchSimRecords();
        }
    }

    /**
     * {@inheritDoc}
    */ 
    @Override
    public String getOperatorNumeric() {
        if (mImsi == null) {
            log("getOperatorNumeric: IMSI == null");
            return null;
        }
        if (mncLength == UNINITIALIZED || mncLength == UNKNOWN) {
            log("getSIMOperatorNumeric: bad mncLength");
            return null;
        }

        // Length = length of MCC + length of MNC
        // length of mcc = 3 (TS 23.003 Section 2.2)
        return mImsi.substring(0, 3 + mncLength);
    }
	String getSIMOperatorNumeric() {
		if (mImsi == null || mncLength == UNINITIALIZED || mncLength == UNKNOWN) {
			return null;
		}
	
		// Length = length of MCC + length of MNC
		// length of mcc = 3 (TS 23.003 Section 2.2)
		return mImsi.substring(0, 3 + mncLength);
	}

    String getSIMCPHSOns() {
        if ( cphsOnsl != null ) {
            return cphsOnsl;
        } else {
            return cphsOnss;
        }
    }

    // ***** Overridden from Handler
    public void handleMessage(Message msg) {
        AsyncResult ar;
        AdnRecord adn;

        byte data[];

        boolean isRecordLoadResponse = false;

        if (mDestroyed.get()) {
            loge("Received message " + msg + "[" + msg.what + "] " +
                    " while being destroyed. Ignoring.");
            return;
        }

        try { switch (msg.what) {
            case EVENT_APP_READY:
                onReady();
                if(bEccRequired == false) {
                    fetchEccList();
                }
            break;
//MTK-START [mtk80601][111215][ALPS00093395]
            case EVENT_RADIO_STATE_CHANGED:
                if (DBG) log("handleMessage (EVENT_RADIO_STATE_CHANGED)");  
                if (bEccRequired == false ) {
                    if (mParentApp.getState() != AppState.APPSTATE_UNKNOWN) {
                        fetchEccList();
                    }
                }

                if(!hasQueryIccId){
                    mCi.queryIccId( obtainMessage(EVENT_QUERY_ICCID_DONE));
                    //hasQueryIccId = true; // disabled cause is that assuming first query is successful may be dangerous.
                }
            break;

            case EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP:
            case EVENT_QUERY_ICCID_DONE:
                if (DBG) log("handleMessage (EVENT_QUERY_ICCID_DONE)");

                ar = (AsyncResult)msg.obj; 

                if(hasQueryIccId) {
                    if (DBG) log("handleMessage (EVENT_QUERY_ICCID_DONE), Next EVENT_RADIO_STATE_CHANGED is fast than waiting EVENT_QUERY_ICCID_DONE.");
                    break;
                }

                int oldIccIdQueryState = iccIdQueryState;
                iccIdQueryState = (ar.exception == null) ? 1 : 0;

                boolean hasSIM = false;
                if (ar.exception == null && (ar.result != null) && !( ((String)ar.result).equals("") )) {
                    hasQueryIccId = true; // set true only when ICCID is legal
                    iccid = (String)ar.result;
                    if(msg.what == EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP) {
                        // TO DO MR1
//                        mParentApp.notifyIccIdForSimPlugIn(iccid);
                    }
                    hasSIM = true;
                    if (DBG) log("IccId = " + iccid);
                } else { 
                    iccIdQueryState = 0;
                    iccid = null;
                    hasSIM = false;
                    Log.e(LOG_TAG, "[SIMRecords] iccid error");
                }

                if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                    //boolean isSimInfoReady = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_SIM_INFO_READY, false);
                    boolean isSimInfoReady = (oldIccIdQueryState == iccIdQueryState); // FALSE case: -1 -> 0, 0 -> 1
                    Log.e(LOG_TAG, "[SIMRecords] is SIMInfo ready [" + isSimInfoReady + ", " + msg.what + "]");
                    if (!isSimInfoReady || msg.what == EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP) {
                        // ALPS00335578
                        String enCryState = SystemProperties.get("vold.decrypt");
                        if(enCryState == null || "".equals(enCryState) || "trigger_restart_framework".equals(enCryState)) {
                            DefaultSIMSettings.onAllIccidQueryComplete(mContext, mPhone, iccid, null, null, null, false);
                        }
                    } else {
                        if (DBG) log("SIM INFO has been ready.");
                    }
                }
            break;

//MTK-END [mtk80601][111215][ALPS00093395]

            /* IO events */
            case EVENT_GET_IMSI_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    loge("Exception querying IMSI, Exception:" + ar.exception);
                    break;
                }

                mImsi = (String) ar.result;

                // IMSI (MCC+MNC+MSIN) is at least 6 digits, but not more
                // than 15 (and usually 15).
                if (mImsi != null && (mImsi.length() < 6 || mImsi.length() > 15)) {
                    loge("invalid IMSI " + mImsi);
                    mImsi = null;
                }

                log("IMSI: " + /* imsi.substring(0, 6) +*/ "xxxxxxx");

                if (((mncLength == UNKNOWN) || (mncLength == 2)) &&
                        ((mImsi != null) && (mImsi.length() >= 6))) {
                    String mccmncCode = mImsi.substring(0, 6);
                    for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                        if (mccmnc.equals(mccmncCode)) {
                            mncLength = 3;
                            break;
                        }
                    }
                }

                if (mncLength == UNKNOWN) {
                    // the SIM has told us all it knows, but it didn't know the mnc length.
                    // guess using the mcc
                    try {
                        int mcc = Integer.parseInt(mImsi.substring(0,3));
                        mncLength = MccTable.smallestDigitsMccForMnc(mcc);
                    } catch (NumberFormatException e) {
                        mncLength = UNKNOWN;
                        loge("Corrupt IMSI!");
                    }
                }

                if (mncLength != UNKNOWN && mncLength != UNINITIALIZED) {
                    // finally have both the imsi and the mncLength and can parse the imsi properly
                    MccTable.updateMccMncConfiguration(mContext, mImsi.substring(0, 3 + mncLength));
                }
                mImsiReadyRegistrants.notifyRegistrants();
            break;

            case EVENT_GET_MBI_DONE:
                boolean isValidMbdn;
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[]) ar.result;

                isValidMbdn = false;
                if (ar.exception == null) {
                    // Refer TS 51.011 Section 10.3.44 for content details
                    log("EF_MBI: " + IccUtils.bytesToHexString(data));

                    // Voice mail record number stored first
                    mailboxIndex = (int)data[0] & 0xff;

                    // check if dailing numbe id valid
                    if (mailboxIndex != 0 && mailboxIndex != 0xff) {
                        log("Got valid mailbox number for MBDN");
                        isValidMbdn = true;
                        this.isValidMBI = true; // ALPS00301018
                    }
                }

                // one more record to load
                recordsToLoad += 1;

                if (isValidMbdn) {
                    // Note: MBDN was not included in NUM_OF_SIM_RECORDS_LOADED
                    new AdnRecordLoader(mFh).loadFromEF(EF_MBDN, EF_EXT6,
                            mailboxIndex, obtainMessage(EVENT_GET_MBDN_DONE));
                } else if(isCphsMailboxEnabled()) {
                    // If this EF not present, try mailbox as in CPHS standard
                    // CPHS (CPHS4_2.WW6) is a european standard.
                    new AdnRecordLoader(mFh).loadFromEF(EF_MAILBOX_CPHS,
                            EF_EXT1, 1,
                            obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                } else {
                	recordsToLoad -= 1;
                }

                break;
            case EVENT_GET_CPHS_MAILBOX_DONE:
            case EVENT_GET_MBDN_DONE:
                //Resetting the voice mail number and voice mail tag to null
                //as these should be updated from the data read from EF_MBDN.
                //If they are not reset, incase of invalid data/exception these
                //variables are retaining their previous values and are
                //causing invalid voice mailbox info display to user.
                voiceMailNum = null;
                voiceMailTag = null;
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {

                    log("Invalid or missing EF"
                        + ((msg.what == EVENT_GET_CPHS_MAILBOX_DONE) ? "[MAILBOX]" : "[MBDN]"));

                    // Bug #645770 fall back to CPHS
                    // FIXME should use SST to decide

                    if (msg.what == EVENT_GET_MBDN_DONE) {
                        //load CPHS on fail...
                        // FIXME right now, only load line1's CPHS voice mail entry

                        recordsToLoad += 1;
                        new AdnRecordLoader(mFh).loadFromEF(
                                EF_MAILBOX_CPHS, EF_EXT1, 1,
                                obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                    }
                    break;
                }

                adn = (AdnRecord)ar.result;

                log("VM: " + adn +
                        ((msg.what == EVENT_GET_CPHS_MAILBOX_DONE) ? " EF[MAILBOX]" : " EF[MBDN]"));

                if (adn.isEmpty() && msg.what == EVENT_GET_MBDN_DONE) {
                    // Bug #645770 fall back to CPHS
                    // FIXME should use SST to decide
                    // FIXME right now, only load line1's CPHS voice mail entry
                    recordsToLoad += 1;
                    new AdnRecordLoader(mFh).loadFromEF(
                            EF_MAILBOX_CPHS, EF_EXT1, 1,
                            obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));

                    break;
                }

                voiceMailNum = adn.getNumber();
                voiceMailTag = adn.getAlphaTag();
            break;

            case EVENT_GET_MSISDN_DONE:
                isRecordLoadResponse = false;

                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    log("Invalid or missing EF[MSISDN]");
                    break;
                }

                adn = (AdnRecord)ar.result;

                msisdn = adn.getNumber();
                msisdnTag = adn.getAlphaTag();

                log("MSISDN: " + /*msisdn*/ "xxxxxxx");
                setNumberForNewSIM();
            break;

            case EVENT_SET_MSISDN_DONE:
                isRecordLoadResponse = false;
                ar = (AsyncResult)msg.obj;

                if (ar.userObj != null) {
                    AsyncResult.forMessage(((Message) ar.userObj)).exception
                            = ar.exception;
                    ((Message) ar.userObj).sendToTarget();
                }
                break;

            case EVENT_GET_MWIS_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                log("EF_MWIS: " + IccUtils.bytesToHexString(data));

                efMWIS = data;

                if ((data[0] & 0xff) == 0xff) {
                    log("Uninitialized record MWIS");
                    break;
                }

                // Refer TS 51.011 Section 10.3.45 for the content description
                boolean voiceMailWaiting = ((data[0] & 0x01) != 0);
                countVoiceMessages = data[1] & 0xff;

                if (voiceMailWaiting && countVoiceMessages == 0) {
                    // Unknown count = -1
                    countVoiceMessages = -1;
                }

                mRecordsEventsRegistrants.notifyResult(EVENT_MWI);
            break;

            case EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                efCPHS_MWI = data;

                // Use this data if the EF[MWIS] exists and
                // has been loaded

                if (efMWIS == null || (efMWIS[0] & 01) == 0) {
                    int indicator = (int)(data[0] & 0xf);

                    // Refer CPHS4_2.WW6 B4.2.3
                    if (indicator == 0xA) {
                        // Unknown count = -1
                        countVoiceMessages = -1;
                    } else if (indicator == 0x5) {
                        countVoiceMessages = 0;
                    }

                    mRecordsEventsRegistrants.notifyResult(EVENT_MWI);
                }
            break;

            case EVENT_GET_ICCID_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                iccid = IccUtils.parseIccIdToString(data, 0, data.length);

                log("iccid: " + iccid);

            break;


            case EVENT_GET_AD_DONE:
                try {
                    isRecordLoadResponse = true;

                    ar = (AsyncResult)msg.obj;
                    data = (byte[])ar.result;

                    if (ar.exception != null) {
                        break;
                    }

                    log("EF_AD: " + IccUtils.bytesToHexString(data));

                    if (data.length < 3) {
                        log("Corrupt AD data on SIM");
                        break;
                    }

                    if (data.length == 3) {
                        log("MNC length not present in EF_AD");
                        break;
                    }

                    mncLength = (int)data[3] & 0xf;

                    if (mncLength == 0xf) {
                        mncLength = UNKNOWN;
                    }
                } finally {
                    if (((mncLength == UNINITIALIZED) || (mncLength == UNKNOWN) ||
                            (mncLength == 2)) && ((mImsi != null) && (mImsi.length() >= 6))) {
                        String mccmncCode = mImsi.substring(0, 6);
                        for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                            if (mccmnc.equals(mccmncCode)) {
                                mncLength = 3;
                                break;
                            }
                        }
                    }

                    if (mncLength == UNKNOWN || mncLength == UNINITIALIZED) {
                        if (mImsi != null) {
                            try {
                                int mcc = Integer.parseInt(mImsi.substring(0,3));

                                mncLength = MccTable.smallestDigitsMccForMnc(mcc);
                            } catch (NumberFormatException e) {
                                mncLength = UNKNOWN;
                                loge("Corrupt IMSI!");
                            }
                        } else {
                            // Indicate we got this info, but it didn't contain the length.
                            mncLength = UNKNOWN;

                            log("MNC length not present in EF_AD");
                        }
                    }
                    if (mImsi != null && mncLength != UNKNOWN) {
                        // finally have both imsi and the length of the mnc and can parse
                        // the imsi properly
                        MccTable.updateMccMncConfiguration(mContext,
                                mImsi.substring(0, 3 + mncLength));
                    }
                }
            break;

            case EVENT_GET_SPN_DONE:
                if (DBG) log("EF_SPN loaded and try to extract: "); // MVNO-API
                isRecordLoadResponse = true;
                ar = (AsyncResult) msg.obj;
//MTK-START [mtk80601][111215][ALPS00093395]
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    spnDisplayCondition = 0xff & data[0];

                    // [ALPS00121176], 255 means invalid SPN file
                    if (spnDisplayCondition == 255) {
                        spnDisplayCondition = -1;
                    }

                    spn = IccUtils.adnStringFieldToString(data, 1, data.length - 1);
                    spNameInEfSpn = spn; // MVNO-API
                    if(spNameInEfSpn != null && spNameInEfSpn.equals("")) {
                        if (DBG) log("set spNameInEfSpn to null because parsing result is empty");
                        spNameInEfSpn = null;
                    }

                    if (DBG) log("Load EF_SPN: " + spn
                            + " spnDisplayCondition: " + spnDisplayCondition);

                    if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA, spn);
                    } else {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA_2, spn);
                    }
                } else {
                    Log.e(LOG_TAG, "SIMRecords: read spn fail!");
                    // See TS 51.011 10.3.11.  Basically, default to
                    // show PLMN always, and SPN also if roaming.
                    spnDisplayCondition = -1;
                }
//MTK-END [mtk80601][111215][ALPS00093395]
            break;

            case EVENT_GET_CFF_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult) msg.obj;
                data = (byte[]) ar.result;

                if (ar.exception != null) {
                    break;
                }

                log("EF_CFF_CPHS: " + IccUtils.bytesToHexString(data));
                mEfCff = data;

                if (validEfCfis(mEfCfis) || !callForwardingEnabled) {
                    callForwardingEnabled =
                        ((data[0] & CFF_LINE1_MASK) == CFF_UNCONDITIONAL_ACTIVE);

                    mRecordsEventsRegistrants.notifyResult(EVENT_CFI);
                } else {
                    log("EVENT_GET_CFF_DONE: invalid mEfCfis="
                            + IccUtils.bytesToHexString(mEfCfis));
                }
                break;

            case EVENT_GET_SPDI_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                parseEfSpdi(data);
            break;

            case EVENT_UPDATE_DONE:
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    logw("update failed. ", ar.exception);
                }
            break;

            case EVENT_GET_PNN_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                //data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                parseEFpnn((ArrayList)ar.result);
               /*
                SimTlv tlv = new SimTlv(data, 0, data.length);

                for ( ; tlv.isValidObject() ; tlv.nextObject()) {
                    if (tlv.getTag() == TAG_FULL_NETWORK_NAME) {
                        pnnHomeName
                            = IccUtils.networkNameToString(
                                tlv.getData(), 0, tlv.getData().length);
                        break;
                    }
                }
                */
            break;

            case EVENT_GET_ALL_SMS_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                if (ar.exception != null)
                    break;

                handleSmses((ArrayList) ar.result);
                break;

            case EVENT_MARK_SMS_READ_DONE:
                Log.i("ENF", "marked read: sms " + msg.arg1);
                break;


            case EVENT_SMS_ON_SIM:
                isRecordLoadResponse = false;

                ar = (AsyncResult)msg.obj;

                int[] index = (int[])ar.result;

                if (ar.exception != null || index.length != 1) {
                    loge("Error on SMS_ON_SIM with exp "
                            + ar.exception + " length " + index.length);
                } else {
                    log("READ EF_SMS RECORD index=" + index[0]);
                    mFh.loadEFLinearFixed(EF_SMS,index[0],
                            obtainMessage(EVENT_GET_SMS_DONE));
                }
                break;

            case EVENT_GET_SMS_DONE:
                isRecordLoadResponse = false;
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    handleSms((byte[])ar.result);
                } else {
                    loge("Error on GET_SMS with exp " + ar.exception);
                }
                break;
            case EVENT_GET_SST_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                mUsimServiceTable = new UsimServiceTable(data);
                if (DBG) log("SST: " + mUsimServiceTable);
                mEfSST = data;
                fetchPnnAndOpl();
                fetchSpn();
            break;

            case EVENT_GET_INFO_CPHS_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    break;
                }

                mCphsInfo = (byte[])ar.result;

                if (DBG) log("iCPHS: " + IccUtils.bytesToHexString(mCphsInfo));

                // ALPS00301018
                if(this.isValidMBI == false && isCphsMailboxEnabled()) {
                    recordsToLoad += 1;
                    new AdnRecordLoader(mFh).loadFromEF(EF_MAILBOX_CPHS,
                                EF_EXT1, 1,
                                obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                }
            break;

            case EVENT_SET_MBDN_DONE:
                isRecordLoadResponse = false;
                ar = (AsyncResult)msg.obj;

                if (ar.exception == null) {
                    voiceMailNum = newVoiceMailNum;
                    voiceMailTag = newVoiceMailTag;
                }

                if (isCphsMailboxEnabled()) {
                    adn = new AdnRecord(voiceMailTag, voiceMailNum);
                    Message onCphsCompleted = (Message) ar.userObj;

                    /* write to cphs mailbox whenever it is available but
                    * we only need notify caller once if both updating are
                    * successful.
                    *
                    * so if set_mbdn successful, notify caller here and set
                    * onCphsCompleted to null
                    */
                    if (ar.exception == null && ar.userObj != null) {
                        AsyncResult.forMessage(((Message) ar.userObj)).exception
                                = null;
                        ((Message) ar.userObj).sendToTarget();

                        if (DBG) log("Callback with MBDN successful.");

                        onCphsCompleted = null;
                    }

                    new AdnRecordLoader(mFh).
                            updateEF(adn, EF_MAILBOX_CPHS, EF_EXT1, 1, null,
                            obtainMessage(EVENT_SET_CPHS_MAILBOX_DONE,
                                    onCphsCompleted));
                } else {
                    if (ar.userObj != null) {
                        AsyncResult.forMessage(((Message) ar.userObj)).exception
                                = ar.exception;
                        ((Message) ar.userObj).sendToTarget();
                    }
                }
                break;
            case EVENT_SET_CPHS_MAILBOX_DONE:
                isRecordLoadResponse = false;
                ar = (AsyncResult)msg.obj;
                if(ar.exception == null) {
                    voiceMailNum = newVoiceMailNum;
                    voiceMailTag = newVoiceMailTag;
                } else {
                    if (DBG) log("Set CPHS MailBox with exception: "
                            + ar.exception);
                }
                if (ar.userObj != null) {
                    if (DBG) log("Callback with CPHS MB successful.");
                    AsyncResult.forMessage(((Message) ar.userObj)).exception
                            = ar.exception;
                    ((Message) ar.userObj).sendToTarget();
                }
                break;
            case EVENT_SIM_REFRESH:
                isRecordLoadResponse = false;
                ar = (AsyncResult)msg.obj;
		if (DBG) log("Sim REFRESH with exception: " + ar.exception);
                if (ar.exception == null) {
                    handleSimRefresh((IccRefreshResponse)ar.result);
                }
                break;
            case EVENT_GET_CFIS_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception != null) {
                    break;
                }

                log("EF_CFIS: " + IccUtils.bytesToHexString(data));

                if (validEfCfis(data)) {
                    mEfCfis = data;

                    // Refer TS 51.011 Section 10.3.46 for the content description
                    callForwardingEnabled = ((data[1] & 0x01) != 0);
                    log("EF_CFIS: callFordwardingEnabled=" + callForwardingEnabled);

                    mRecordsEventsRegistrants.notifyResult(EVENT_CFI);
                } else {
                    log("EF_CFIS: invalid data=" + IccUtils.bytesToHexString(data));
                }
                break;

            case EVENT_GET_SIM_ECC_DONE:
                if (DBG) log("handleMessage (EVENT_GET_SIM_ECC_DONE)");

                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Log.e(LOG_TAG, "Get SIM ecc with exception: " + ar.exception);
                    break;
                }

                data = (byte[]) ar.result;
                for (int i = 0 ; i + 2 < data.length ; i += 3) {
                    String eccNum;
                    eccNum = IccUtils.bcdToString(data, i, 3);
                    //MTK-START [mtk04070][120104][ALPS00109412]Solve "While making any outgoing call with international prefix "+", the no. is dialling emergency number"
                    //Merge from ALPS00102099
                    if (eccNum != null && !eccNum.equals("") && i != 0 && !mEfEcc.equals("")) {
                        mEfEcc = mEfEcc + ",";
                    }
                    //MTK-END [mtk04070][120104][ALPS00109412]Solve "While making any outgoing call with international prefix "+", the no. is dialling emergency number"
                    mEfEcc = mEfEcc + eccNum ;
                }

                if (DBG) log("SIM mEfEcc is " + mEfEcc);
                if (PhoneConstants.GEMINI_SIM_2 == mSimId) {
                    SystemProperties.set("ril.ecclist2", mEfEcc);
                } else {
                    SystemProperties.set("ril.ecclist", mEfEcc);
                }
            break;

            case EVENT_GET_USIM_ECC_DONE:
                if (DBG) log("handleMessage (EVENT_GET_USIM_ECC_DONE)");

                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Log.e(LOG_TAG, "Get USIM ecc with exception: " + ar.exception);
                    break;
                }
   
                ArrayList eccRecords = (ArrayList) ar.result;
                int count = eccRecords.size();

                for (int i = 0; i < count; i++) {
                    data = (byte[]) eccRecords.get(i);
                    if (DBG) log("USIM EF_ECC record "+ count + ": " + IccUtils.bytesToHexString(data)); 
                    String eccNum;
                    eccNum = IccUtils.bcdToString(data, 0, 3);
                    if ( eccNum != null && !eccNum.equals("") ) {
                        if(!mEfEcc.equals("")) {
                            mEfEcc = mEfEcc + ",";
                        }
                        mEfEcc = mEfEcc + eccNum ;
                    }
                }

                if (DBG) log("USIM mEfEcc is " + mEfEcc);
                if (PhoneConstants.GEMINI_SIM_2 == mSimId) {
                    SystemProperties.set("ril.ecclist2", mEfEcc);
                } else {
                    SystemProperties.set("ril.ecclist", mEfEcc);
                }
            break;

            case EVENT_GET_CSP_CPHS_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    loge("Exception in fetching EF_CSP data " + ar.exception);
                    break;
                }

                data = (byte[])ar.result;

                log("EF_CSP: " + IccUtils.bytesToHexString(data));
                handleEfCspData(data);
                break;
//MTK-START [mtk80601][111215][ALPS00093395]
            case EVENT_GET_ALL_OPL_DONE:
                isRecordLoadResponse = true;

                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    break;
                }
                parseEFopl((ArrayList)ar.result);
                break;

            case EVENT_GET_CPHSONS_DONE:
                if (DBG) log("handleMessage (EVENT_GET_CPHSONS_DONE)");
                isRecordLoadResponse = true;

                ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    cphsOnsl = IccUtils.adnStringFieldToString(
                            data, 0, data.length);

                    if (DBG) log("Load EF_SPN_CPHS: " + cphsOnsl);
                }
                break;

            case EVENT_GET_SHORT_CPHSONS_DONE:
                if (DBG) log("handleMessage (EVENT_GET_SHORT_CPHSONS_DONE)");
                isRecordLoadResponse = true;

                ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    cphsOnss = IccUtils.adnStringFieldToString(
                            data, 0, data.length);

                    if (DBG) log("Load EF_SPN_SHORT_CPHS: " + cphsOnss);
                }
                break;

            case EVENT_PHB_READY:
                if (DBG) log("handleMessage (EVENT_PHB_READY)");
                fetchPhbRecords();
                mPhbReady = true;
                //No need to update system property because it has been updated in rill.
                broadcastPhbStateChangedIntent(mPhbReady);
                break;

            // ALPS00302698 ENS
            case EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED:
                ar = (AsyncResult) msg.obj;
                if(ar != null && ar.exception == null)  {
                    processEfCspPlmnModeBitUrc(((int[])ar.result)[0]);
                }
                break;

            // ALPS00302702 RAT balancing
            case EVENT_GET_RAT_DONE:
                if (DBG) log("handleMessage (EVENT_GET_RAT_DONE)");
                isRecordLoadResponse = true;

                ar = (AsyncResult) msg.obj;
                mEfRatLoaded = true;
                if (ar != null && ar.exception == null) {
                    mEfRat = ((byte[]) ar.result);
                    log("load EF_RAT complete: " + mEfRat[0]);
                    boradcastEfRatContentNotify(EF_RAT_FOR_OTHER_CASE);
                } else {
                    log("load EF_RAT fail");         
                    mEfRat = null;
                    if(mParentApp.getType() == AppType.APPTYPE_USIM) {
                        boradcastEfRatContentNotify(EF_RAT_NOT_EXIST_IN_USIM);
                    } else {
                        boradcastEfRatContentNotify(EF_RAT_FOR_OTHER_CASE);
                    }
                }
                break;
//MTK-END [mtk80601][111215][ALPS00093395]

            /*
              Detail description:
              This feature provides a interface to get menu title string from EF_SUME
            */
            case EVENT_QUERY_MENU_TITLE_DONE:
                Log.d(LOG_TAG, "[sume receive response message");
                isRecordLoadResponse = true;
                
                ar = (AsyncResult)msg.obj;
                if(ar != null && ar.exception == null) {
                    data = (byte[])ar.result;
                    if(data != null && data.length >= 2) {
                        int tag = data[0] & 0xff;
                        int len = data[1] & 0xff;
                        Log.d(LOG_TAG, "[sume tag = " + tag + ", len = " + len);
                        mMenuTitleFromEf = IccUtils.adnStringFieldToString(data, 2, len);
                        Log.d(LOG_TAG, "[sume menu title is " + mMenuTitleFromEf);
                    } 
                } else {
                    if(ar.exception != null) {
                        Log.d(LOG_TAG, "[sume exception in AsyncResult: " + ar.exception.getClass().getName());
                    } else {
                        Log.d(LOG_TAG, "[sume null AsyncResult");
                    }
                    mMenuTitleFromEf = null;
                }
                
                break;
            case EVENT_RADIO_AVAILABLE:
                if (mTelephonyExt.isSetLanguageBySIM()) {
                    fetchLanguageIndicator();
                }
            break;
            case EVENT_GET_LI_DONE:             
                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception == null) {
                   Log.d(LOG_TAG, "EF_LI: " +
                   IccUtils.bytesToHexString(data)); 
                   mEfLI = data;
                }
                onLanguageFileLoaded();
            break;
            case EVENT_GET_ELP_DONE:
                ar = (AsyncResult)msg.obj;
                data = (byte[])ar.result;

                if (ar.exception == null) {
                    Log.d(LOG_TAG, "EF_ELP: " +
                       IccUtils.bytesToHexString(data));
                    mEfELP = data;
                }
                onLanguageFileLoaded();        
                break;
            default:
                super.handleMessage(msg);   // IccRecords handles generic record load responses

        }}catch (RuntimeException exc) {
            // I don't want these exceptions to be fatal
            logw("Exception parsing SIM record", exc);
        } finally {
            // Count up record load responses even if they are fails
            if (isRecordLoadResponse) {
                onRecordLoaded();
            }
        }
    }
//MTK-START [mtk80601][111215][ALPS00093395]

    // ALPS00302698 ENS
    private void processEfCspPlmnModeBitUrc(int bit) {
        Log.d(LOG_TAG, "processEfCspPlmnModeBitUrc: bit = " + bit);
        if(bit == 0) {
            mCspPlmnEnabled = false;
        } else {
            mCspPlmnEnabled = true;
        }
////        phone.setNetworkSelectionModeAutomatic(null);
        Intent intent = new Intent(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY);
        intent.putExtra(TelephonyIntents.INTENT_KEY_PLMN_MODE_BIT, bit);
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        log("broadCast intent ACTION_EF_CSP_CONTENT_NOTIFY, INTENT_KEY_PLMN_MODE_BIT: " +  bit);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    private void getIccIdsDone(boolean hasSIM) {
        String oldIccIdInSlot = null;
        Log.d(LOG_TAG, "getIccIdsDone  " );
        SIMInfo oldSimInfo = SIMInfo.getSIMInfoBySlot(mContext, mSimId);
        if (oldSimInfo != null) {
            oldIccIdInSlot = oldSimInfo.mICCId;
            Log.d(LOG_TAG, "getIccIdsDone old IccId In Slot0 is " + oldIccIdInSlot);
            ContentValues value = new ContentValues(1);
            value.put(SimInfo.SLOT, -1);
            mContext.getContentResolver().update(ContentUris.withAppendedId(SimInfo.CONTENT_URI, oldSimInfo.mSimId), 
                        value, null, null);                 
        } else {
            Log.d(LOG_TAG, "getIccIdsDone No sim in slot0 for last time " );
        }
        
        //check if the Inserted sim is new
        int nNewCardCount = 0;
        if (iccid != null && !iccid.equals("")) {            
            SIMInfo.insertICCId(mContext, iccid, mSimId); 
            if (!iccid.equals(oldIccIdInSlot)){
                //one new card inserted into slot1
                nNewCardCount++;                      
            }       
        }

        long simIdForSlot = Settings.System.DEFAULT_SIM_NOT_SET;
        if(iccid != null){
            SIMInfo simInfo = SIMInfo.getSIMInfoByICCId(mContext, iccid);
            if(simInfo != null){
                simIdForSlot = simInfo.mSimId;
            }
        }
            
        //get all default SIM setting
        long oldVTDefaultSIM = Settings.System.getLong(mContext.getContentResolver(), Settings.System.VIDEO_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
        long oldVoiceCallDefaultSIM =  Settings.System.getLong(mContext.getContentResolver(), 
                                                           Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
        long oldSmsDefaultSIM = Settings.System.getLong(mContext.getContentResolver(), Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET); 
        long oldGprsDefaultSIM = Settings.System.getLong(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET); 

        boolean hasDefaultSIMRemoved = false;
        //To do: broadcast intent notify keyguard to popup message box about new card insertec.
        
        if(hasSIM) {
            if(FeatureOption.MTK_VT3G324M_SUPPORT) {
                Settings.System.putLong(mContext.getContentResolver(), Settings.System.VIDEO_CALL_SIM_SETTING, simIdForSlot);
            }
        }else if(FeatureOption.MTK_VT3G324M_SUPPORT){
            //VT is only can use SIM in slot1
            Settings.System.putLong(mContext.getContentResolver(), Settings.System.VIDEO_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
        }
        
        if(hasSIM) {  
            
            if(oldVoiceCallDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET){
                Settings.System.putLong(mContext.getContentResolver(), Settings.System.VOICE_CALL_SIM_SETTING, simIdForSlot);
            }

            if(oldSmsDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET){
                Settings.System.putLong(mContext.getContentResolver(), Settings.System.SMS_SIM_SETTING, simIdForSlot);
            }

            if(oldGprsDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET){
                Settings.System.putLong(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, simIdForSlot);
            }   
        } 
        
        if (nNewCardCount > 0 ) {   
            log("getIccIdsDone New SIM detected. " );
            setColorForNewSIM();
            //setDefaultNameIfImsiReadyOrLocked(simInfos);
            int airplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
            if (airplaneMode > 0) {
                setDefaultNameForNewSIM(null);
            }
        }

        
        if (isSIMRemoved(oldVoiceCallDefaultSIM,simIdForSlot)) {
            Settings.System.putLong(mContext.getContentResolver(), Settings.System.VOICE_CALL_SIM_SETTING, simIdForSlot);             
            hasDefaultSIMRemoved = true;
        }

        if (isSIMRemoved(oldSmsDefaultSIM,simIdForSlot)) {
            Settings.System.putLong(mContext.getContentResolver(), Settings.System.SMS_SIM_SETTING, simIdForSlot);
            hasDefaultSIMRemoved = true;
        }

        if (isSIMRemoved(oldGprsDefaultSIM,simIdForSlot)) {
        /*    if(nSIMCount > 1){
                defSIM = Settings.System.DEFAULT_SIM_NOT_SET;
            }*/
            Settings.System.putLong(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, 
                                                      Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER);  
            hasDefaultSIMRemoved = true;
        }

        Log.d(LOG_TAG, "getIccIdsDone set PROPERTY_SIM_INFO_READY to true. " );
        SystemProperties.set(TelephonyProperties.PROPERTY_SIM_INFO_READY, "true");
        Log.d(LOG_TAG, "getIccIdsDone  PROPERTY_SIM_INFO_READY after set is " + SystemProperties.get(TelephonyProperties.PROPERTY_SIM_INFO_READY, null) );
         
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        Log.d(LOG_TAG,"broadCast intent ACTION_SIM_INFO_UPDATE ");
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
	/*
        if (nNewCardCount > 0){
            log("getIccIdsDone. New SIM detected. ");
            broadCastNewSIMDetected(1);         
        }else if (hasDefaultSIMRemoved && hasSIM) {
            log("getIccIdsDone No new SIM detected and Default SIM for some service has been removed. " );
            broadCastDefaultSIMRemoved(1);
        } */     
	
    }

	private boolean isSIMRemoved(long defSIMId,long curSIM){     
        if (defSIMId <= 0) {
            return false;
        } else if (defSIMId != curSIM) {
            return true;
        } else {
            return false;
        }
    }

    private void broadCastNewSIMDetected(int nSIMCount) {
        Intent intent = new Intent(TelephonyIntents.ACTION_NEW_SIM_DETECTED);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMCount);
        log("broadCast intent ACTION_NEW_SIM_DETECTED " +  nSIMCount);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    private void broadCastDefaultSIMRemoved(int nSIMCount) {
        Intent intent = new Intent(TelephonyIntents.ACTION_DEFAULT_SIM_REMOVED);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMCount);
        log("broadCast intent ACTION_DEFAULT_SIM_REMOVED " +  nSIMCount);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    private void setColorForNewSIM() {
        //int[] colors = new int[8];
        SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(mContext, mSimId);
        int simColor = -1;
        if (simInfo!= null) {          
            simColor = simInfo.mColor;
            if(!(0 <= simColor && simColor <= 3)) {
                ContentValues valueColor1 = new ContentValues(1);
                simColor = 1;
                valueColor1.put(SimInfo.COLOR, simColor);
                mContext.getContentResolver().update(ContentUris.withAppendedId(SimInfo.CONTENT_URI, simInfo.mSimId), 
                        valueColor1, null, null);   
            }
            Log.d(LOG_TAG, "setColorForNewSIM SimInfo simColor is " + simColor);
        }
    }


    public void setDefaultNameForNewSIM(String strName){
        ISimInfoUpdate simInfoUpdate = null;
        try {
            simInfoUpdate = MediatekClassFactory.createInstance(ISimInfoUpdate.class, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        simInfoUpdate.setDefaultNameForNewSimAdp(mContext, strName, mSimId);
    }

    public void broadCastSetDefaultNameDone(){
        Intent intent = new Intent("android.intent.action.SIM_NAME_UPDATE");
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);        
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
        Log.d(LOG_TAG,"broadCast intent ACTION_SIM_NAME_UPDATE for sim " + mSimId);
    }

    private void setNumberForNewSIM(){
        SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(mContext, mSimId);
        if (simInfo!= null) {			
            String simNumber = simInfo.mNumber;
            Log.d(LOG_TAG, "setNumberForNewSIM SimInfo simNumber is " + simNumber);
            if (simNumber == null ) {
                SIMInfo.setNumber(mContext,msisdn, simInfo.mSimId);
                Intent intent = new Intent(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
                ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
                Log.d(LOG_TAG, "setNumberForNewSIM SimInfo simId is " + simInfo.mSimId + " number is " + msisdn);
            }			
        }
    }
//MTK-END [mtk80601][111215][ALPS00093395]	
    private void handleFileUpdate(int efid) {
        switch(efid) {
            case EF_MBDN:
                recordsToLoad++;
                new AdnRecordLoader(mFh).loadFromEF(EF_MBDN, EF_EXT6,
                        mailboxIndex, obtainMessage(EVENT_GET_MBDN_DONE));
                break;
            case EF_MAILBOX_CPHS:
                recordsToLoad++;
                new AdnRecordLoader(mFh).loadFromEF(EF_MAILBOX_CPHS, EF_EXT1,
                        1, obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                break;
            case EF_CSP_CPHS:
                recordsToLoad++;
                log("[CSP] SIM Refresh for EF_CSP_CPHS");
                mFh.loadEFTransparent(EF_CSP_CPHS,
                        obtainMessage(EVENT_GET_CSP_CPHS_DONE));
                break;
            case EF_ADN:            
            case EF_FDN:
            case EF_SDN:
            case EF_PBR:
            case EF_MSISDN:                
                // ALPS00523253: If the file update is related to PHB efid, set phb ready to false
                setPhbReady(false);
                adnCache.reset();                 
                break;
            default:
                // For now, fetch all records if this is not a
                // voicemail number.
                // TODO: Handle other cases, instead of fetching all.
                adnCache.reset();
                fetchSimRecords();
                break;
        }
    }

    private void handleSimRefresh(IccRefreshResponse refreshResponse){
        if (refreshResponse == null) {
            if (DBG) log("handleSimRefresh received without input");
            return;
        }

        if (refreshResponse.aid != null &&
                !refreshResponse.aid.equals(mParentApp.getAid())) {
            // This is for different app. Ignore.
            return;
        }

        switch (refreshResponse.refreshResult) {
            case CommandsInterface.SIM_REFRESH_FILE_UPDATED:
 		        if (DBG) log("handleSimRefresh with SIM_REFRESH_FILE_UPDATED");
                // result[1] contains the EFID of the updated file.
                //int efid = result[1];
                handleFileUpdate(refreshResponse.efId);
//MTK-START [mtk80601][111215][ALPS00093395]
                /*for (int i=1; i<result.length ; i++) {
                    if (result[i] == EF_ADN
                            || result[i] == EF_FDN
                            || result[i] == EF_MSISDN
                            || result[i] == EF_SDN) {
                        adnCache.reset();
                        fetchPhbRecords();
                        break;
                    }
                }*/
//MTK-END [mtk80601][111215][ALPS00093395]
                break;
            case IccRefreshResponse.REFRESH_RESULT_INIT:
                if (DBG) log("handleSimRefresh with SIM_REFRESH_INIT");
                // need to reload all files (that we care about)
                adnCache.reset();
                fetchSimRecords();
                break;
            case IccRefreshResponse.REFRESH_RESULT_RESET:
                if (DBG) log("handleSimRefresh with SIM_REFRESH_RESET");
                //mCi.setRadioPower(false, null);

                // [ALPS00385924][mtk02772]
                // Note: reset modem only can send to modem1 originally because only protocol1 can handle, 
                // so phone2 need to broadcast intent to Phone1 
                // but rild has already special handle for modem reset command,
                // it will transfer to protocol 1 channel if the command is sent to modem2.
                // Accordingly, send the modem reset command to corresponding phone directly
                //mCi.resetRadio(null);
                //adnCache.reset();

                if (!PhoneFactory.isDualTalkMode()) {
                    int SimIdFor3G = SystemProperties.getInt(PROPERTY_3G_SWITCH, 1)-1;

                    // [ALPS00432584][mtk02772]
                    // EPOF needs to sent to 3G Modem,
                    // if it already 3G switch, send EPOF directly
                    if(SimIdFor3G == mSimId) {
                        mCi.resetRadio(null);
                    } else {
                        // notify phone 1 to reset modem
                        Log.d(LOG_TAG, "notify phone 1 to reset modem");
                        Intent intent = new Intent(ACTION_RESET_MODEM);
                        mContext.sendBroadcast(intent);
                    }
                } else {
                    // reset modem directly
                    mCi.resetRadio(null);
                }

                adnCache.reset();

                /* Note: no need to call setRadioPower(true).  Assuming the desired
                * radio power state is still ON (as tracked by ServiceStateTracker),
                * ServiceStateTracker will call setRadioPower when it receives the
                * RADIO_STATE_CHANGED notification for the power off.  And if the
                * desired power state has changed in the interim, we don't want to
                * override it with an unconditional power on.
                */

                /*
                * MTK Note: we don't need to reset adnCache
                * and we will reset it in the onRadioOffOrNotAvailable()
                * because radio will be unavailable if SIM refresh reset occurs
                */
                break;
            default:
                // unknown refresh operation
                if (DBG) log("handleSimRefresh with unknown operation");
                break;
        }
        
        // notify apps that the files of SIM are changed
        if(refreshResponse.refreshResult == CommandsInterface.SIM_REFRESH_FILE_UPDATED ||
              refreshResponse.refreshResult == CommandsInterface.SIM_REFRESH_INIT) {
            Log.d(LOG_TAG, "notify apps that SIM files changed");
            Intent intent;
            intent = new Intent(ACTION_SIM_FILES_CHANGED);
            intent.putExtra(KEY_SIM_ID, mSimId);
            mContext.sendBroadcast(intent);
        }
        
        // notify stk app to clear the idle text
        if(refreshResponse.refreshResult == CommandsInterface.SIM_REFRESH_INIT ||
                refreshResponse.refreshResult == CommandsInterface.SIM_REFRESH_RESET) {
            // impl
            Log.d(LOG_TAG, "notify stk app to remove the idle text");
            Intent intent;
            intent = new Intent(ACTION_REMOVE_IDLE_TEXT);
            intent.putExtra(KEY_SIM_ID, mSimId);
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * Dispatch 3GPP format message. Overridden for CDMA/LTE phones by
     * {@link com.android.internal.telephony.cdma.CdmaLteUiccRecords}
     * to send messages to the secondary 3GPP format SMS dispatcher.
     */
    protected int dispatchGsmMessage(SmsMessageBase message) {
        mNewSmsRegistrants.notifyResult(message);
        return 0;
    }

    private void handleSms(byte[] ba) {
        if (ba[0] != 0)
            Log.d("ENF", "status : " + ba[0]);

        // 3GPP TS 51.011 v5.0.0 (20011-12)  10.5.3
        // 3 == "received by MS from network; message to be read"
        if (ba[0] == 3) {
            int n = ba.length;

            // Note: Data may include trailing FF's.  That's OK; message
            // should still parse correctly.
            byte[] pdu = new byte[n - 1];
            System.arraycopy(ba, 1, pdu, 0, n - 1);
            SmsMessage message = SmsMessage.createFromPdu(pdu);

            int result = dispatchGsmMessage(message);
/* TEMP
            if (result == Intents.RESULT_SMS_OUT_OF_MEMORY) {
                ((GSMPhone) phone).mSMS.notifyLastIncomingSms(result);
            }
*/

        }
    }


    private void handleSmses(ArrayList messages) {
        int count = messages.size();

        for (int i = 0; i < count; i++) {
            byte[] ba = (byte[]) messages.get(i);

            if (ba[0] != 0)
                Log.i("ENF", "status " + i + ": " + ba[0]);

            // 3GPP TS 51.011 v5.0.0 (20011-12)  10.5.3
            // 3 == "received by MS from network; message to be read"

            if (ba[0] == 3) {
                int n = ba.length;

                // Note: Data may include trailing FF's.  That's OK; message
                // should still parse correctly.
                byte[] pdu = new byte[n - 1];
                System.arraycopy(ba, 1, pdu, 0, n - 1);
                SmsMessage message = SmsMessage.createFromPdu(pdu);

                dispatchGsmMessage(message);

                // 3GPP TS 51.011 v5.0.0 (20011-12)  10.5.3
                // 1 == "received by MS from network; message read"

                ba[0] = 1;

                if (false) { // XXX writing seems to crash RdoServD
                    mFh.updateEFLinearFixed(EF_SMS,
                            i, ba, null, obtainMessage(EVENT_MARK_SMS_READ_DONE, i));
                }
            }
        }
    }

    protected void onRecordLoaded() {
        // One record loaded successfully or failed, In either case
        // we need to update the recordsToLoad count
        recordsToLoad -= 1;
        if (DBG) log("onRecordLoaded " + recordsToLoad + " requested: " + recordsRequested);

        if (recordsToLoad == 0 && recordsRequested == true) {
            onAllRecordsLoaded();
        } else if (recordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            recordsToLoad = 0;
        }
    }

    protected void onAllRecordsLoaded() {
        String operator = getOperatorNumeric();

        // Some fields require more than one SIM record to set

        if (mSimId== PhoneConstants.GEMINI_SIM_1) {
            SystemProperties.set(PROPERTY_ICC_OPERATOR_NUMERIC, operator);
        } else {
            SystemProperties.set(PROPERTY_ICC_OPERATOR_NUMERIC_2, operator);
        }

        if (mImsi != null) {
            String countryCode;
            try {
                countryCode = 
                    MccTable.countryCodeForMcc(Integer.parseInt(mImsi.substring(0,3)));
            } catch(NumberFormatException e) {
                countryCode = null;
                Log.e(LOG_TAG, "SIMRecords: Corrupt IMSI!");
            }
            if (mSimId== PhoneConstants.GEMINI_SIM_1) {            
                SystemProperties.set(PROPERTY_ICC_OPERATOR_ISO_COUNTRY, countryCode);
            } else {
                SystemProperties.set(PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2, countryCode);
            }
        }
        else {
            loge("onAllRecordsLoaded: imsi is NULL!");
        }

        setVoiceMailByCountry(operator);
        //setSpnFromConfig(operator);

        recordsLoadedRegistrants.notifyRegistrants(
            new AsyncResult(null, null, null));

        Log.d("SIM", "[SIMRecords] sim id = " + mSimId+ " imsi = " + mImsi + " operator = " + operator);

        if (operator != null) {
            String newName = null;
            if (operator.equals("46002") || operator.equals("46007") ) {         
                operator = "46000";
            }
            newName = mCi.lookupOperatorName(operator, true);

            if (mSimId== PhoneConstants.GEMINI_SIM_1) {
                SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME, newName);
            } else {
                SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2, newName);
            }

            //ALPS00288486
            /*for Gemini phone, check the other SIM display name*/
            boolean simLocaleProcessing = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_SIM_LOCALE_SETTINGS, false);
            if (simLocaleProcessing) {
                mIsWaitingLocale = true;
                log("wait for setting locale done from the other card");
            } else{
                setDefaultNameForNewSIM(newName);
            }
        } else {
            setDefaultNameForNewSIM(null);
        }
    }

    //***** Private methods

    private void setSpnFromConfig(String carrier) {
        if (mSpnOverride.containsCarrier(carrier)) {
            spn = mSpnOverride.getSpn(carrier);
        }
    }


    private void setVoiceMailByCountry (String spn) {
        if (mVmConfig.containsCarrier(spn)) {
            isVoiceMailFixed = true;
            voiceMailNum = mVmConfig.getVoiceMailNumber(spn);
            voiceMailTag = mVmConfig.getVoiceMailTag(spn);
        }
    }

    @Override
    public void onReady() {
        fetchSimRecords();
    }
//MTK-START [mtk80601][111215][ALPS00093395]
    private void fetchPhbRecords() {
        // FIXME should examine EF[MSISDN]'s capability configuration
        // to determine which is the voice/data/fax line
        new AdnRecordLoader(mFh).loadFromEF(EF_MSISDN, EF_EXT1, 1,
                        obtainMessage(EVENT_GET_MSISDN_DONE));
    }

    private void fetchEccList() {
        if (DBG) log("fetchEccList()"); 
        mEfEcc = "";
        bEccRequired = true;
        
        if(mParentApp.getType() == AppType.APPTYPE_USIM) {
            mFh.loadEFLinearFixedAll(EF_ECC, obtainMessage(EVENT_GET_USIM_ECC_DONE));
        } else {
            mFh.loadEFTransparent(EF_ECC, obtainMessage(EVENT_GET_SIM_ECC_DONE));
        }
    }

    // ALPS00267605 : PNN/OPL revision
    private void fetchPnnAndOpl() {
        if (DBG) log("fetchPnnAndOpl()"); 
        //boolean bPnnOplActive = false;
        boolean bPnnActive = false;
        boolean bOplActive = false;

        if (mEfSST != null) {  
            if (mParentApp.getType() == AppType.APPTYPE_USIM) {
                if (mEfSST.length >= 6) {
                    bPnnActive = ((mEfSST[5] & 0x10) == 0x10);
                    if(bPnnActive) {
                        bOplActive = ((mEfSST[5] & 0x20) == 0x20);
                    }
                }
            } else if (mEfSST.length >= 13) {
                bPnnActive = ((mEfSST[12] & 0x30) == 0x30);
                if (bPnnActive) {
                    bOplActive = ((mEfSST[12] & 0xC0) == 0xC0);
                }
            }
        }
        if (DBG) log("bPnnActive = " + bPnnActive + ", bOplActive = " + bOplActive);

        if (bPnnActive) {
            mFh.loadEFLinearFixedAll(EF_PNN, obtainMessage(EVENT_GET_PNN_DONE));
            recordsToLoad++;
            if(bOplActive) {
                mFh.loadEFLinearFixedAll(EF_OPL, obtainMessage(EVENT_GET_ALL_OPL_DONE));
                recordsToLoad++;
            }
        }
    }

    private void fetchSpn() {
        if (DBG) log("fetchSpn()");
        boolean bSpnActive = false;

        Phone.IccServiceStatus iccSerStatus =  getSIMServiceStatus(Phone.IccService.SPN);
        if (iccSerStatus == Phone.IccServiceStatus.ACTIVATED) {  
            spn = null;
            mFh.loadEFTransparent(EF_SPN,
                    obtainMessage(EVENT_GET_SPN_DONE));
            recordsToLoad++;
        } else {
            Log.i(LOG_TAG, "[SIMRecords] SPN service is not activated  " );
        }
    }

    private void fetchCPHSOns() {
        if (DBG) log("fetchCPHSOns()");
        cphsOnsl= null;
        cphsOnss= null;
        mFh.loadEFTransparent(EF_SPN_CPHS,
               obtainMessage(EVENT_GET_CPHSONS_DONE));
        recordsToLoad++;
        mFh.loadEFTransparent(
               EF_SPN_SHORT_CPHS, obtainMessage(EVENT_GET_SHORT_CPHSONS_DONE));
        recordsToLoad++;
    }
//MTK-END [mtk80601][111215][ALPS00093395]
    protected void fetchSimRecords() {
        recordsRequested = true;

        Log.v(LOG_TAG, "SIMRecords:fetchSimRecords " + recordsToLoad);

        mCi.getIMSI(obtainMessage(EVENT_GET_IMSI_DONE));
        recordsToLoad++;

        //iccFh.loadEFTransparent(EF_ICCID, obtainMessage(EVENT_GET_ICCID_DONE));
        //recordsToLoad++;

        // FIXME should examine EF[MSISDN]'s capability configuration
        // to determine which is the voice/data/fax line
        //new AdnRecordLoader(phone).loadFromEF(EF_MSISDN, EF_EXT1, 1,
                    //obtainMessage(EVENT_GET_MSISDN_DONE));
        //recordsToLoad++;

        // Record number is subscriber profile
        mFh.loadEFLinearFixed(EF_MBI, 1, obtainMessage(EVENT_GET_MBI_DONE));
        recordsToLoad++;

        mFh.loadEFTransparent(EF_AD, obtainMessage(EVENT_GET_AD_DONE));
        recordsToLoad++;

        // Record number is subscriber profile
        mFh.loadEFLinearFixed(EF_MWIS, 1, obtainMessage(EVENT_GET_MWIS_DONE));
        recordsToLoad++;


        // Also load CPHS-style voice mail indicator, which stores
        // the same info as EF[MWIS]. If both exist, both are updated
        // but the EF[MWIS] data is preferred
        // Please note this must be loaded after EF[MWIS]
        mFh.loadEFTransparent(
                EF_VOICE_MAIL_INDICATOR_CPHS,
                obtainMessage(EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE));
        recordsToLoad++;

        // Same goes for Call Forward Status indicator: fetch both
        // EF[CFIS] and CPHS-EF, with EF[CFIS] preferred.
        mFh.loadEFLinearFixed(EF_CFIS, 1, obtainMessage(EVENT_GET_CFIS_DONE));
        recordsToLoad++;
        mFh.loadEFTransparent(EF_CFF_CPHS, obtainMessage(EVENT_GET_CFF_DONE));
        recordsToLoad++;


        //getSpnFsm(true, null);

        mFh.loadEFTransparent(EF_SPDI, obtainMessage(EVENT_GET_SPDI_DONE));
        recordsToLoad++;

        //mFh.loadEFLinearFixed(EF_PNN, 1, obtainMessage(EVENT_GET_PNN_DONE));
        //recordsToLoad++;

        mFh.loadEFTransparent(EF_SST, obtainMessage(EVENT_GET_SST_DONE));
        recordsToLoad++;

        mFh.loadEFTransparent(EF_INFO_CPHS, obtainMessage(EVENT_GET_INFO_CPHS_DONE));
        recordsToLoad++;

        mFh.loadEFTransparent(EF_CSP_CPHS,obtainMessage(EVENT_GET_CSP_CPHS_DONE));
        recordsToLoad++;

        /*
          Detail description:
          This feature provides a interface to get menu title string from EF_SUME
        */
        if (mTelephonyExt != null) {
            if (mTelephonyExt.isSetLanguageBySIM()) {
                mFh.loadEFTransparent(EF_SUME, obtainMessage(EVENT_QUERY_MENU_TITLE_DONE)); 
                recordsToLoad++;
            }
        } else {
            loge("fetchSimRecords(): mTelephonyExt is null!!!");
        }

        fetchCPHSOns();

        // XXX should seek instead of examining them all
        if (false) { // XXX
            mFh.loadEFLinearFixedAll(EF_SMS, obtainMessage(EVENT_GET_ALL_SMS_DONE));
            recordsToLoad++;
        }

        if (CRASH_RIL) {
            String sms = "0107912160130310f20404d0110041007030208054832b0120"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "ffffffffffffffffffffffffffffff";
            byte[] ba = IccUtils.hexStringToBytes(sms);

            mFh.updateEFLinearFixed(EF_SMS, 1, ba, null,
                            obtainMessage(EVENT_MARK_SMS_READ_DONE, 1));
        }

        /*
        * Here, we assume that PHB is ready and try to read the entries.
        * If it is not, we will receive the event EVENT_PHB_READY later.
        * Then, we will ready the PHB entries again.
        */
        fetchPhbRecords();

		fetchRatBalancing();
    }

    /**
     * Returns the SpnDisplayRule based on settings on the SIM and the
     * specified plmn (currently-registered PLMN).  See TS 22.101 Annex A
     * and TS 51.011 10.3.11 for details.
     *
     * If the SPN is not found on the SIM or is empty, the rule is
     * always PLMN_ONLY.
     */
    @Override
    public int getDisplayRule(String plmn) {
        int rule;
        boolean bSpnActive = false;

        if (mEfSST != null && mParentApp != null) {  
            if (mParentApp.getType() == AppType.APPTYPE_USIM) {
                if (mEfSST.length >= 3 && (mEfSST[2] & 0x04) == 4) {
                    bSpnActive = true;
                    log("getDisplayRule USIM mEfSST is " + IccUtils.bytesToHexString(mEfSST) + " set bSpnActive to true");
                }
            } else if ((mEfSST.length >= 5) && (mEfSST[4] & 0x02) == 2) {
                bSpnActive = true;
                log("getDisplayRule SIM mEfSST is " + IccUtils.bytesToHexString(mEfSST) + " set bSpnActive to true");
            }
        }

        if (!bSpnActive || TextUtils.isEmpty(spn) || spn.equals("") || spnDisplayCondition == -1) {
            // No EF_SPN content was found on the SIM, or not yet loaded.  Just show ONS.
            rule = SPN_RULE_SHOW_PLMN;
        } else if (isOnMatchingPlmn(plmn)) {
            rule = SPN_RULE_SHOW_SPN;
            if ((spnDisplayCondition & 0x01) == 0x01) {
                // ONS required when registered to HPLMN or PLMN in EF_SPDI
                rule |= SPN_RULE_SHOW_PLMN;
            }
        } else {
            rule = SPN_RULE_SHOW_PLMN;
            if ((spnDisplayCondition & 0x02) == 0x00) {
                // SPN required if not registered to HPLMN or PLMN in EF_SPDI
                rule |= SPN_RULE_SHOW_SPN;
            }
        }
        return rule;
    }

    /**
     * Checks if plmn is HPLMN or on the spdiNetworks list.
     */
    private boolean isOnMatchingPlmn(String plmn) {
        if (plmn == null) return false;

        if (isHPlmn(plmn)) {
            return true;
        }

        if (spdiNetworks != null) {
            for (String spdiNet : spdiNetworks) {
                if (plmn.equals(spdiNet)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * States of Get SPN Finite State Machine which only used by getSpnFsm()
     */
    private enum Get_Spn_Fsm_State {
        IDLE,               // No initialized
        INIT,               // Start FSM
        READ_SPN_3GPP,      // Load EF_SPN firstly
        READ_SPN_CPHS,      // Load EF_SPN_CPHS secondly
        READ_SPN_SHORT_CPHS // Load EF_SPN_SHORT_CPHS last
    }

    /**
     * Finite State Machine to load Service Provider Name , which can be stored
     * in either EF_SPN (3GPP), EF_SPN_CPHS, or EF_SPN_SHORT_CPHS (CPHS4.2)
     *
     * After starting, FSM will search SPN EFs in order and stop after finding
     * the first valid SPN
     *
     * If the FSM gets restart while waiting for one of
     * SPN EFs results (i.e. a SIM refresh occurs after issuing
     * read EF_CPHS_SPN), it will re-initialize only after
     * receiving and discarding the unfinished SPN EF result.
     *
     * @param start set true only for initialize loading
     * @param ar the AsyncResult from loadEFTransparent
     *        ar.exception holds exception in error
     *        ar.result is byte[] for data in success
     */
    private void getSpnFsm(boolean start, AsyncResult ar) {
        byte[] data;

        if (start) {
            // Check previous state to see if there is outstanding
            // SPN read
            if(spnState == Get_Spn_Fsm_State.READ_SPN_3GPP ||
               spnState == Get_Spn_Fsm_State.READ_SPN_CPHS ||
               spnState == Get_Spn_Fsm_State.READ_SPN_SHORT_CPHS ||
               spnState == Get_Spn_Fsm_State.INIT) {
                // Set INIT then return so the INIT code
                // will run when the outstanding read done.
                spnState = Get_Spn_Fsm_State.INIT;
                return;
            } else {
            spnState = Get_Spn_Fsm_State.INIT;
        }
        }

        switch(spnState){
            case INIT:
                spn = null;

                mFh.loadEFTransparent(EF_SPN,
                        obtainMessage(EVENT_GET_SPN_DONE));
                recordsToLoad++;

                spnState = Get_Spn_Fsm_State.READ_SPN_3GPP;
                break;
            case READ_SPN_3GPP:
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    spnDisplayCondition = 0xff & data[0];

                    // [ALPS00121176], 255 means invalid SPN file
                    if (spnDisplayCondition == 255) {
                        spnDisplayCondition = -1;
                    }

                    spn = IccUtils.adnStringFieldToString(data, 1, data.length - 1);

                    if (DBG) log("Load EF_SPN: " + spn
                            + " spnDisplayCondition: " + spnDisplayCondition);
                    if (mSimId== PhoneConstants.GEMINI_SIM_1) {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA, spn);
                    } else {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA_2, spn);
                    }

                    spnState = Get_Spn_Fsm_State.IDLE;
                } else {
                    mFh.loadEFTransparent( EF_SPN_CPHS,
                            obtainMessage(EVENT_GET_SPN_DONE));
                    recordsToLoad++;

                    spnState = Get_Spn_Fsm_State.READ_SPN_CPHS;

                    // See TS 51.011 10.3.11.  Basically, default to
                    // show PLMN always, and SPN also if roaming.
                    spnDisplayCondition = -1;
                }
                break;
            case READ_SPN_CPHS:
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    spn = IccUtils.adnStringFieldToString(data, 0, data.length);

                    if (DBG) log("Load EF_SPN_CPHS: " + spn);
                    if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA, spn);
                    } else {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA_2, spn);
                    }

                    spnState = Get_Spn_Fsm_State.IDLE;
                } else {
                    mFh.loadEFTransparent(
                            EF_SPN_SHORT_CPHS, obtainMessage(EVENT_GET_SPN_DONE));
                    recordsToLoad++;

                    spnState = Get_Spn_Fsm_State.READ_SPN_SHORT_CPHS;
                }
                break;
            case READ_SPN_SHORT_CPHS:
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    spn = IccUtils.adnStringFieldToString(data, 0, data.length);

                    if (DBG) log("Load EF_SPN_SHORT_CPHS: " + spn);
                    if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA, spn);
                    } else {
                        SystemProperties.set(PROPERTY_ICC_OPERATOR_ALPHA_2, spn);
                    }
                }else {
                    if (DBG) log("No SPN loaded in either CHPS or 3GPP");
                }

                spnState = Get_Spn_Fsm_State.IDLE;
                break;
            default:
                spnState = Get_Spn_Fsm_State.IDLE;
        }
    }

    /**
     * Parse TS 51.011 EF[SPDI] record
     * This record contains the list of numeric network IDs that
     * are treated specially when determining SPN display
     */
    private void
    parseEfSpdi(byte[] data) {
        SimTlv tlv = new SimTlv(data, 0, data.length);

        byte[] plmnEntries = null;

        for ( ; tlv.isValidObject() ; tlv.nextObject()) {
            // Skip SPDI tag, if existant
            if (tlv.getTag() == TAG_SPDI) {
              tlv = new SimTlv(tlv.getData(), 0, tlv.getData().length);
            }
            // There should only be one TAG_SPDI_PLMN_LIST
            if (tlv.getTag() == TAG_SPDI_PLMN_LIST) {
                plmnEntries = tlv.getData();
                break;
            }
        }

        if (plmnEntries == null) {
            return;
        }

        spdiNetworks = new ArrayList<String>(plmnEntries.length / 3);

        for (int i = 0 ; i + 2 < plmnEntries.length ; i += 3) {
            String plmnCode;
            plmnCode = IccUtils.parsePlmnToString(plmnEntries, i, 3);

            // Valid operator codes are 5 or 6 digits
            if (plmnCode.length() >= 5) {
                log("EF_SPDI network: " + plmnCode);
                spdiNetworks.add(plmnCode);
            }
        }
    }

    /**
     * check to see if Mailbox Number is allocated and activated in CPHS SST
     */
    private boolean isCphsMailboxEnabled() {
        if (mCphsInfo == null)  return false;
        return ((mCphsInfo[1] & CPHS_SST_MBN_MASK) == CPHS_SST_MBN_ENABLED );
    }
//MTK-START [mtk80601][111215][ALPS00093395]
    /**
    *parse pnn list 
    */
    private void parseEFpnn(ArrayList messages) {
        int count = messages.size();
        if (DBG) log("parseEFpnn(): pnn has " + count + " records");

        mPnnNetworkNames = new ArrayList<OperatorName>(count);
        for (int i = 0; i < count; i++) {
            byte[] data = (byte[]) messages.get(i);
            if (DBG) log("parseEFpnn(): pnn record " + i + " content is " + IccUtils.bytesToHexString(data));

            SimTlv tlv = new SimTlv(data, 0, data.length);
            OperatorName opName = new OperatorName();
            for ( ; tlv.isValidObject(); tlv.nextObject()) {
                if (tlv.getTag() == TAG_FULL_NETWORK_NAME) {
                    opName.sFullName = IccUtils.networkNameToString(
                                tlv.getData(), 0, tlv.getData().length); 
                    if (DBG) log("parseEFpnn(): pnn sFullName is "  + opName.sFullName);
                } else if (tlv.getTag() == TAG_SHORT_NETWORK_NAME) {
                    opName.sShortName = IccUtils.networkNameToString(
                                tlv.getData(), 0, tlv.getData().length); 
                    if (DBG) log("parseEFpnn(): pnn sShortName is "  + opName.sShortName);
                }
            }

            mPnnNetworkNames.add(opName);
        }
    }

    /**
    *parse opl list 
    */
    private void parseEFopl(ArrayList messages) {
        int count = messages.size();
        if (DBG) log("parseEFopl(): opl has " + count + " records");

        mOperatorList= new ArrayList<OplRecord>(count);
        for (int i = 0; i < count; i++) {
            byte[] data = (byte[]) messages.get(i);
            if (DBG) log("parseEFopl(): opl record " + i + " content is " + IccUtils.bytesToHexString(data));

            OplRecord oplRec = new OplRecord();

            oplRec.sPlmn = IccUtils.parsePlmnToStringForEfOpl(data, 0, 3); // ALPS00316057
            if (DBG) log("parseEFopl(): opl sPlmn = " + oplRec.sPlmn);
 
            byte[] minLac = new byte[2];
            minLac[0] = data[3];
            minLac[1] = data[4];
            oplRec.nMinLAC = Integer.parseInt(IccUtils.bytesToHexString(minLac), 16);
            if (DBG) log("parseEFopl(): opl nMinLAC = " + oplRec.nMinLAC);

            byte[] maxLAC = new byte[2];
            maxLAC[0] = data[5];
            maxLAC[1] = data[6];
            oplRec.nMaxLAC = Integer.parseInt(IccUtils.bytesToHexString(maxLAC), 16);
            if (DBG) log("parseEFopl(): opl nMaxLAC = " + oplRec.nMaxLAC);

            oplRec.nPnnIndex = Integer.parseInt(IccUtils.bytesToHexString(data).substring(14), 16);
            if (DBG) log("parseEFopl(): opl nPnnIndex = " + oplRec.nPnnIndex);

            mOperatorList.add(oplRec);
        }
    }
//MTK-END [mtk80601][111215][ALPS00093395]
   
//MTK-START [mtk80601][111215][ALPS00093395]
    // ALPS00359372 for at&t testcase, mnc 2 should match 3 digits
    private boolean isMatchingPlmnForEfOpl(String simPlmn, String bcchPlmn) {
        if(simPlmn == null || simPlmn.equals("") || bcchPlmn == null || bcchPlmn.equals(""))
            return false;

        if (DBG) log("isMatchingPlmnForEfOpl(): simPlmn = " + simPlmn + ", bcchPlmn = " + bcchPlmn);

        /*  3GPP TS 23.122 Annex A (normative): HPLMN Matching Criteria
            For PCS1900 for North America, regulations mandate that a 3-digit MNC shall be used; 
            however during a transition period, a 2 digit MNC may be broadcast by the Network and, 
            in this case, the 3rd digit of the SIM is stored as 0 (this is the 0 suffix rule).     */
        int simPlmnLen = simPlmn.length();
        int bcchPlmnLen = bcchPlmn.length();
        if( simPlmnLen < 5 || bcchPlmnLen < 5 )
            return false;

        int i =0;
        for( i = 0; i < 5; i++) {
            if(simPlmn.charAt(i) == 'd')
                continue;
            if(simPlmn.charAt(i) != bcchPlmn.charAt(i))
                return false;
        }
        
        if(simPlmnLen == 6 && bcchPlmnLen == 6){
            if(simPlmn.charAt(5) == 'd' || simPlmn.charAt(5) == bcchPlmn.charAt(5)) {
                return true;
            }else{
                return false;
            }
        }else if(bcchPlmnLen == 6 && bcchPlmn.charAt(5) != '0'&& bcchPlmn.charAt(5) != 'd'){
            return false;
        }else if(simPlmnLen == 6 && simPlmn.charAt(5) != '0' && simPlmn.charAt(5) != 'd'){
            return false;
        }

        return true;
    }  

    // ALPS00267605 : PNN/OPL revision
    public String getEonsIfExist(String plmn, int nLac, boolean bLongNameRequired) {
        if (DBG) log("EONS getEonsIfExist: plmn is " + plmn + " nLac is " + nLac + " bLongNameRequired: " + bLongNameRequired);
        if(plmn == null || mPnnNetworkNames == null || mPnnNetworkNames.size() == 0) {
            return null;
        }

        int nPnnIndex = -1;  
        boolean isHPLMN = isHPlmn(plmn);

        if(mOperatorList == null) {
            // case for EF_PNN only
            if(isHPLMN) {
                if (DBG) log("EONS getEonsIfExist: Plmn is HPLMN, but no mOperatorList, return PNN's first record");
                nPnnIndex = 1;
            } else {
                if (DBG) log("EONS getEonsIfExist: Plmn is not HPLMN, and no mOperatorList, return null");
                return null;
            }
        } else {
            //search EF_OPL using plmn & nLac
            for (int i = 0; i < mOperatorList.size(); i++) {
                OplRecord oplRec = mOperatorList.get(i);
                if (DBG) log("EONS getEonsIfExist: record number is " + i + " sPlmn: " + oplRec.sPlmn + " nMinLAC: "
                             + oplRec.nMinLAC + " nMaxLAC: " + oplRec.nMaxLAC + " PnnIndex " + oplRec.nPnnIndex);

                // ALPS00316057
                //if((plmn.equals(oplRec.sPlmn) ||(!oplRec.sPlmn.equals("") && plmn.startsWith(oplRec.sPlmn))) &&
                if( isMatchingPlmnForEfOpl(oplRec.sPlmn, plmn) &&
                   ((oplRec.nMinLAC == 0 && oplRec.nMaxLAC == 0xfffe) || (oplRec.nMinLAC <= nLac && oplRec.nMaxLAC >= nLac))) {
                    if (DBG) log("EONS getEonsIfExist: find it in EF_OPL");
                    if (oplRec.nPnnIndex == 0) {
                        if (DBG) log("EONS getEonsIfExist: oplRec.nPnnIndex is 0 indicates that the name is to be taken from other sources");
                        return null;
                    }
                    nPnnIndex = oplRec.nPnnIndex;
                    break;
                }
            }
        }

        //ALPS00312727, 11603, add check (mOperatorList.size() == 1 
        if(nPnnIndex == -1 && isHPLMN&&(mOperatorList.size()==1)) {
            if (DBG) log("EONS getEonsIfExist: not find it in EF_OPL, but Plmn is HPLMN, return PNN's first record");
            nPnnIndex = 1;
        }
        else if(nPnnIndex > 1 && nPnnIndex > mPnnNetworkNames.size() && isHPLMN) {
            if (DBG) log("EONS getEonsIfExist: find it in EF_OPL, but index in EF_OPL > EF_PNN list length & Plmn is HPLMN, return PNN's first record");
            nPnnIndex = 1;
        }
        else if (nPnnIndex > 1 && nPnnIndex > mPnnNetworkNames.size() && !isHPLMN) {
            if (DBG) log("EONS getEonsIfExist: find it in EF_OPL, but index in EF_OPL > EF_PNN list length & Plmn is not HPLMN, return PNN's first record");
            nPnnIndex = -1;
        }

        String sEons = null;
        if(nPnnIndex >= 1) {
            OperatorName opName = mPnnNetworkNames.get(nPnnIndex - 1);
            if (bLongNameRequired) {
                if (opName.sFullName != null) {
                    sEons = new String(opName.sFullName);
                } else if (opName.sShortName != null) {
                    sEons = new String(opName.sShortName);
                }
            } else if (!bLongNameRequired ) {
                if (opName.sShortName != null) {
                    sEons = new String(opName.sShortName);                            
                } else if (opName.sFullName != null) {
                    sEons = new String(opName.sFullName);
                }               
            }
        }
        if (DBG) log("EONS getEonsIfExist: sEons is " + sEons);

        return sEons;

        /*int nPnnIndex = -1;  
        //check if the plmn is Hplmn, return the first record of pnn 
        if (isHPlmn(plmn)) {
            nPnnIndex = 1;
            if (DBG) log("EONS getEonsIfExist Plmn is hplmn");
        } else {
            //search the plmn from opl and if the LAC in the range of opl
            for (int i = 0; i < mOperatorList.size(); i++) {
                OplRecord oplRec = mOperatorList.get(i);
                //check if the plmn equals with the plmn in the operator list or starts with the plmn in the operator list(which include wild char 'D')
                if((plmn.equals(oplRec.sPlmn) ||(!oplRec.sPlmn.equals("") && plmn.startsWith(oplRec.sPlmn))) &&
                   ((oplRec.nMinLAC == 0 && oplRec.nMaxLAC == 0xfffe) || (oplRec.nMinLAC <= nLac && oplRec.nMaxLAC >= nLac))) {
                    nPnnIndex = oplRec.nPnnIndex;
                    break;
                }
                if (DBG) log("EONS getEonsIfExist record number is " + i + " sPlmn: " + oplRec.sPlmn + " nMinLAC: "
                             + oplRec.nMinLAC + " nMaxLAC: " + oplRec.nMaxLAC + " PnnIndex " + oplRec.nPnnIndex);
            }
            if (nPnnIndex == 0) {
                return null;    // not HPLMN and the index is 0 indicates that the name is to be taken from other sources   
            }
        }
        if (DBG) log("EONS getEonsIfExist Index of pnn is  " + nPnnIndex);

        String sEons = null;
        if (nPnnIndex >= 1) {
            OperatorName opName = mPnnNetworkNames.get(nPnnIndex - 1);
            if (bLongNameRequired) {
                if (opName.sFullName != null) {
                    sEons = new String(opName.sFullName);
                } else if (opName.sShortName != null) {
                    sEons = new String(opName.sShortName);
                }
            } else if (!bLongNameRequired ) {
                if (opName.sShortName != null) {
                    sEons = new String(opName.sShortName);                            
                } else if (opName.sFullName != null) {
                    sEons = new String(opName.sFullName);
                }               
            }
        }
        if (DBG) log("EONS getEonsIfExist sEons is " + sEons);
        return sEons;*/
    }

    public Phone.IccServiceStatus getSIMServiceStatus(Phone.IccService enService) {
        int nServiceNum = enService.getIndex();
        Phone.IccServiceStatus simServiceStatus = Phone.IccServiceStatus.UNKNOWN;
        if (DBG) log("getSIMServiceStatus enService is " + enService + " Service Index is " + nServiceNum);
 
        if (nServiceNum >= 0 && nServiceNum < Phone.IccService.UNSUPPORTED_SERVICE.getIndex() && mEfSST != null) {
            if (mParentApp.getType() == AppType.APPTYPE_USIM) {
                int nUSTIndex = usimServiceNumber[nServiceNum];
                if (nUSTIndex <= 0) {
                    simServiceStatus = Phone.IccServiceStatus.NOT_EXIST_IN_USIM;
                } else {
                    int nbyte = nUSTIndex /8;
                    int nbit = nUSTIndex % 8 ;
                    if(nbit == 0) {
                        nbit = 7;
                        nbyte--;
                    } else {
                        nbit--;
                    }
                    if (DBG) log("getSIMServiceStatus USIM nbyte: " + nbyte + " nbit: " + nbit);

                    if(mEfSST.length > nbyte && ((mEfSST[nbyte] & (0x1 << nbit)) > 0)) {
                        simServiceStatus = Phone.IccServiceStatus.ACTIVATED;  
                    } else {
                        simServiceStatus = Phone.IccServiceStatus.INACTIVATED;
                    }
                }
            } else {
                int nSSTIndex = simServiceNumber[nServiceNum];
		  if (nSSTIndex <= 0) {
		      simServiceStatus = Phone.IccServiceStatus.NOT_EXIST_IN_SIM;		      
		  } else {
	             int nbyte = nSSTIndex/4;
		      int nbit = nSSTIndex % 4;
		      if(nbit == 0) {
		          nbit = 3;
		          nbyte--;	  
		      } else {
		          nbit--;
		      }
		  
                    int nMask = (0x3 << (nbit*2)); 
		      Log.d(LOG_TAG, "getSIMServiceStatus SIM nbyte: " + nbyte + " nbit: " + nbit + " nMask: " + nMask);	
		      if( mEfSST.length > nbyte && ((mEfSST[nbyte] & nMask) == nMask)) {
		          simServiceStatus = Phone.IccServiceStatus.ACTIVATED;   
		      } else {
                        simServiceStatus = Phone.IccServiceStatus.INACTIVATED;
		      }
		  }
	     }            
	 }
    		
	 Log.d(LOG_TAG, "getSIMServiceStatus simServiceStatus: " + simServiceStatus);	
	 return simServiceStatus;
    }

    /*
    * Wipe all SIM contacts for DM   
    * Intent com.mediatek.dm.LAWMO_WIPE
    */
    private void wipeAllSIMContacts() {
        if (DBG) log("wipeAllSIMContacts");
        adnCache.reset();
        if (DBG) log("wipeAllSIMContacts after reset");
    }

    private final BroadcastReceiver mHandlePhbReadyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            log("mHandlePhbReadyReceiver Receive action " + action);
            if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                mContext.unregisterReceiver(mHandlePhbReadyReceiver);
                mSIMInfoReady = true;
                broadcastPhbStateChangedIntent(true);

                IntentFilter phbFilter = new IntentFilter();
                phbFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                phbFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
                phbFilter.addAction(TelephonyIntents.ACTION_IVSR_NOTIFY);
                mContext.registerReceiver(mHandlePhbReadyReceiver, phbFilter);
            }

            if (FeatureOption.MTK_FLIGHT_MODE_POWER_OFF_MD) {
                if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                    boolean enabled = intent.getBooleanExtra("state", false);
                    log("mHandlePhbReadyReceiver MTK_FLIGHT_MODE_POWER_OFF_MD flightmode = " + enabled);
                    if (enabled) {
                        SystemProperties.set(SIMRECORD_PROPERTY_RIL_PHB_READY[mParentApp.getMySimId()], "false"); 
                        mPhbReady = false;
                    }
                }
            }
            
            if(action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                log("mHandlePhbReadyReceiver ACTION_SHUTDOWN_IPO: reset PHB_READY");
                //ALPS00580231:Reset property when EPON.
                mPhbReady = false;
            }
            
            if(action.equals(TelephonyIntents.ACTION_IVSR_NOTIFY)) {
                int simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
                log("mHandlePhbReadyReceiver ACTION_IVSR_NOTIFY: reset SIM " + simId + " PHB_READY");
                SystemProperties.set(SIMRECORD_PROPERTY_RIL_PHB_READY[mParentApp.getMySimId()], "false");  
                if(simId == mParentApp.getMySimId()) {
                    mPhbReady = false;
                }
            }

        }
        
    };

    private class SIMBroadCastReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.mediatek.dm.LAWMO_WIPE")) {
               wipeAllSIMContacts();
            } else {
                if (action.equals("action_pin_dismiss")) {
                    int simId = intent.getIntExtra("simslot", PhoneConstants.GEMINI_SIM_1);
                    if (simId == mSimId){
                        Log.d(LOG_TAG, "SIMRecords receive pin dismiss intent for slot " + simId);   
                        setDefaultNameForNewSIM(null);
                   }
                } else if(action.equals("action_melock_dismiss")) {
                   int simId = intent.getIntExtra("simslot", /*PhoneConstants.GEMINI_SIM_1*/0);
                   if (simId == mSimId){
                       Log.d(LOG_TAG, "SIMRecords receive SIM ME lock dismiss intent for slot " + simId);   
                       setDefaultNameForNewSIM(null);
                   }            
                } else if(action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                   processShutdownIPO();
                   // ALPS00293301
                   SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME, null);
                   if(FeatureOption.MTK_GEMINI_SUPPORT)
                       SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2, null);

                   // ALPS00302698 ENS
                   Log.d(LOG_TAG, "wipeAllSIMContacts ACTION_SHUTDOWN_IPO: reset mCspPlmnEnabled");
                   mCspPlmnEnabled = true;

                   // ALPS00302702 RAT balancing 
                   if (mTelephonyExt.isSetLanguageBySIM()) {
                       mEfRatLoaded = false;
                       mEfRat = null;
                   }
                   
                   adnCache.reset(); 
                   Log.d(LOG_TAG, "wipeAllSIMContacts ACTION_SHUTDOWN_IPO");
                } else if(action.equals(ACTION_RESET_MODEM)) {
                    int SimIdFor3G = SystemProperties.getInt(PROPERTY_3G_SWITCH, 1)-1;
                    if(SimIdFor3G == mSimId) {
                        Log.d(LOG_TAG, "phone " + SimIdFor3G + " will reset modem");
                        mCi.resetRadio(null);
                    }
                } else if(action.equals(Intent.ACTION_LOCALE_CHANGED)) { //ALPS00288486
                   Log.d(LOG_TAG, "SIMBroadCastReceiver action = " + action + " mIsWaitingLocale " + mIsWaitingLocale);
                   SystemProperties.set(TelephonyProperties.PROPERTY_SIM_LOCALE_SETTINGS, "false");			   
                   if (mIsWaitingLocale){
                       setDefaultNameByLocale();
                   }
                }
            }
        }
    }
//MTK-END [mtk80601][111215][ALPS00093395]

    protected void log(String s) {
        Log.d(LOG_TAG, "[SIMRecords] [SIM" + mSimId + "]" + s);
    }

    protected void loge(String s) {
        Log.e(LOG_TAG, "[SIMRecords] [SIM" + mSimId + "]"  + s);
    }

    protected void logw(String s, Throwable tr) {
        Log.w(LOG_TAG, "[SIMRecords] [SIM" + mSimId + "]" + s, tr);
    }

    protected void logv(String s) {
        Log.v(LOG_TAG, "[SIMRecords] [SIM" + mSimId + "]"  + s);
    }

    /**
     * Return true if "Restriction of menu options for manual PLMN selection"
     * bit is set or EF_CSP data is unavailable, return false otherwise.
     */
    public boolean isCspPlmnEnabled() {
        log("isCspPlmnEnabled(), mCspPlmnEnabled = " + mCspPlmnEnabled);
        return mCspPlmnEnabled;
    }

    /**
     * Parse EF_CSP data and check if
     * "Restriction of menu options for manual PLMN selection" is
     * Enabled/Disabled
     *
     * @param data EF_CSP hex data.
     */
    private void handleEfCspData(byte[] data) {
        // As per spec CPHS4_2.WW6, CPHS B.4.7.1, EF_CSP contains CPHS defined
        // 18 bytes (i.e 9 service groups info) and additional data specific to
        // operator. The valueAddedServicesGroup is not part of standard
        // services. This is operator specific and can be programmed any where.
        // Normally this is programmed as 10th service after the standard
        // services.
        int usedCspGroups = data.length / 2;
        // This is the "Servive Group Number" of "Value Added Services Group".
        byte valueAddedServicesGroup = (byte)0xC0;

        mCspPlmnEnabled = true;
        for (int i = 0; i < usedCspGroups; i++) {
             if (data[2 * i] == valueAddedServicesGroup) {
                 log("[CSP] found ValueAddedServicesGroup, value " + data[(2 * i) + 1]);
				 // ALPS00302698 ENS : modem will provide CSP PLMN 
                 if ((data[(2 * i) + 1] & 0x80) == 0x80) {
                     // Bit 8 is for
                     // "Restriction of menu options for manual PLMN selection".
                     // Operator Selection menu should be enabled.
                     mCspPlmnEnabled = true;
                 } else {
                     mCspPlmnEnabled = false;
                     // Operator Selection menu should be disabled.
                     // Operator Selection Mode should be set to Automatic.
                     //log("[CSP] Set Automatic Network Selection");
                     //mNetworkSelectionModeAutomaticRegistrants.notifyRegistrants();
                 }
                 return;
             }
        }

        log("[CSP] Value Added Service Group (0xC0), not found!");
    }


    private void fetchLanguageIndicator() {
        log("fetchLanguageIndicator " );
        String l = SystemProperties.get("persist.sys.language");
        String c = SystemProperties.get("persist.sys.country");
        String oldSimLang = SystemProperties.get("persist.sys.simlanguage");
        if((null == l || 0 == l.length()) && (null == c || 0 == c.length()) 
                         && (null == oldSimLang || 0 == oldSimLang.length())) {
            mFh.loadEFTransparent( EF_LI,
                   obtainMessage(EVENT_GET_LI_DONE));
            efLanguageToLoad++;
            mFh.loadEFTransparent( EF_ELP,
                   obtainMessage(EVENT_GET_ELP_DONE));  
            efLanguageToLoad++;
        }
    }

    private void onLanguageFileLoaded() {
        efLanguageToLoad--;
        log("onLanguageFileLoaded efLanguageToLoad is " + efLanguageToLoad);
        if(efLanguageToLoad == 0){
            log("onLanguageFileLoaded all language file loaded");
            if(mEfLI != null || mEfELP != null) {
                setLanguageFromSIM();
            }else {
                log("onLanguageFileLoaded all language file are not exist!");
            }
        }
    }

    private void setLanguageFromSIM() {
        log("setLanguageFromSIM ");
        boolean bMatched = false;
       
        if ( mParentApp.getType() == AppType.APPTYPE_USIM ){                
            bMatched = getMatchedLocaleByLI(mEfLI);
        }else {
            bMatched = getMatchedLocaleByLP(mEfLI);
        }  
        if(!bMatched && mEfELP != null) {
            bMatched = getMatchedLocaleByLI(mEfELP);         
        }
        log("setLanguageFromSIM End");
    }

    private boolean getMatchedLocaleByLI(byte[] data) {
        boolean ret = false;
        if (data == null) {
            return ret;
        }
        int lenOfLI = data.length;
        String lang = null;
        for (int i = 0; i+2 <= lenOfLI; i+=2) {
            lang = IccUtils.parseLanguageIndicator(data, i, 2);
            log("USIM language in language indicator: i is " + i + " language is " + lang);
            if(lang == null || lang.equals("")){
                log("USIM language in language indicator: i is " + i + " language is empty");
                break;
            }
            lang = lang.toLowerCase();
            ret = matchLangToLocale(lang);

            if (ret) {
                break;
            }
        }   
        return ret;
    }

    private boolean getMatchedLocaleByLP(byte[] data) {
        boolean ret = false;
        if (data == null) {
            return ret;
        }       
        int lenOfLP = data.length;
        String lang = null;
        for (int i = 0; i < lenOfLP; i++) {
            int index = (int)mEfLI[0] & 0xff;
            if (0x00 <= index && index <= 0x0f ) {
                lang = LANGUAGE_CODE_FOR_LP[index];
            }else if (0x20 <= index && index <= 0x2f ) {
                lang = LANGUAGE_CODE_FOR_LP[index - 0x10];
            }
                
            log("SIM language in language preference: i is " + i + " language is " + lang);
            if(lang == null || lang.equals("")){
                log("SIM language in language preference: i is " + i + " language is empty");
                break;
            }

            ret = matchLangToLocale(lang);

            if (ret) {                      
                break;
            }
        }
        return ret;
    }
    
    private boolean matchLangToLocale(String lang) {
        boolean ret = false;
        String[] locals = mContext.getAssets().getLocales();
        int localsSize = locals.length;
        for (int i = 0 ; i < localsSize; i++ ) {
            String s = locals[i];
            int len = s.length();                        
            if (len == 5) {
                String language = s.substring(0, 2);                            
                log("Supported languages: the i" + i + " th is " + language);
                if(lang.equals(language)) {
                    ret = true;                 
/* TEMP
                    setSystemLocale(lang, s.substring(3, 5), true);
*/
                    MccTable.setSystemLocale(mContext,lang,s.substring(3, 5));
                    log("Matched! lang: " + lang + ", country is " + s.substring(3, 5));
                    break;
                }
            }
        }
        return ret;
    }

    /*
      Detail description:
      This feature provides a interface to get menu title string from EF_SUME
    */
    public String getMenuTitleFromEf() {
        return mMenuTitleFromEf;
    }

    public boolean isHPlmn(String plmn){
        //follow the behavior of modem, according to the length of plmn to compare mcc/mnc
        //ex: mccmnc: 334030 but plmn:33403 => still be HPLMN
        String mccmnc = getOperatorNumeric();
        if (plmn == null) return false;

        if(mccmnc == null || mccmnc.equals("")) {
            log("isHPlmn getOperatorNumeric error: " + mccmnc);
            return false;
        }

        if (plmn.equals(mccmnc)) {
            return true;
        }else{
            if(plmn.length() == 5 && mccmnc.length() == 6
                && plmn.equals(mccmnc.substring(0,5))){
                return true;              
            }  
        }
        return false;
    }

    // ALPS00302702 RAT balancing START
    private void fetchRatBalancing() {
        if (mTelephonyExt.isSetLanguageBySIM())
            return;
        log("support MTK_RAT_BALANCING");

        if(mParentApp.getType() == AppType.APPTYPE_USIM) {
            log("start loading EF_RAT");
            mFh.loadEFTransparent(EF_RAT, obtainMessage(EVENT_GET_RAT_DONE));
            recordsToLoad++;
        }
        else if(mParentApp.getType() == AppType.APPTYPE_SIM) {
            // broadcast & set no file
            log("loading EF_RAT fail, because of SIM");
            mEfRatLoaded = false;
            mEfRat = null;
            boradcastEfRatContentNotify(EF_RAT_FOR_OTHER_CASE);
        }
        else {
            log("loading EF_RAT fail, because of +EUSIM");
        }
    }

    private void boradcastEfRatContentNotify(int item) {
        // TO DO MR1
        
        if(mPhone.get3GCapabilitySIM() != mSimId) {
            log("not broadCast intent ACTION_EF_RAT_CONTENT_NOTIFY, simId: " + mSimId + ", 3GslotId: " + mPhone.get3GCapabilitySIM());
            return;
        }
        Intent intent = new Intent(TelephonyIntents.ACTION_EF_RAT_CONTENT_NOTIFY);
        intent.putExtra(TelephonyIntents.INTENT_KEY_EF_RAT_STATUS, item);
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        log("broadCast intent ACTION_EF_RAT_CONTENT_NOTIFY: item: " + item + ", simId: " +mSimId);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    public int getEfRatBalancing() {
        log("getEfRatBalancing: iccCardType = " + mParentApp.getType() + ", mEfRatLoaded = " + mEfRatLoaded + ", mEfRat is null = " + (mEfRat == null));
        
        if( (mParentApp.getType() == AppType.APPTYPE_USIM) && mEfRatLoaded && mEfRat == null) {   
            return EF_RAT_NOT_EXIST_IN_USIM; 
        }
        return EF_RAT_FOR_OTHER_CASE;
    }
    // ALPS00302702 RAT balancing END
    	
    private void setDefaultNameByLocale(){  //ALPS00288486
        SIMInfo simInfos = SIMInfo.getSIMInfoById(mContext, mSimId);
        String operator = getSIMOperatorNumeric();
        log("setDefaultNameByLocale() operator = " + operator); 
        if(operator != null) {
            String newName = null;
            if(operator.equals("46002") || operator.equals("46007") ) {		   
                operator = "46000";
            }
            newName = ((RIL)mCi).lookupOperatorName(operator, true);
            log("setDefaultNameByLocale() newName = " + operator); 
            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME, newName);
            } else {
                SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2, newName);
            }
            //the default name only can be set when it is null
            setDefaultNameForNewSIM(newName);				   
        }else{
            log("setDefaultNameByLocale() no operator name, but this intent should be received when operator is not null"); 
            setDefaultNameForNewSIM(null);				   
        }
        mIsWaitingLocale = false;
    }

    private void processShutdownIPO() {
        // reset icc id variable when ipo shutdown
        // ipo shutdown will make radio turn off, 
        // only needs to reset the variable which will not be reset in onRadioOffOrNotAvailable()
        hasQueryIccId = false;
        iccIdQueryState = -1;
        iccid = null;
        mImsi = null;
        spNameInEfSpn = null;
    }

    @Override
    public void onSimHotSwap(boolean isPlugIn) {

        // JB MR1 will dispose UiccCardApplication/IccRecords after sim absent.
        // onSimHotSwap will only be called when sim plug in
        hasQueryIccId = false;
        mCi.queryIccId( obtainMessage(EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP));

        if (!isPlugIn) {
            resetRecords();
            mImsi = null;
            adnCache.reset();
            iccid = null;
            if (mSimId == PhoneConstants.GEMINI_SIM_1)
                SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME, null);
            else
                SystemProperties.set(PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2, null);
        }
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Phone defaultPhone = PhoneFactory.getDefaultPhone();
            ((GeminiPhone)defaultPhone).onSimHotSwap(mSimId, isPlugIn);
        }
    }

    // MVNO-API START
    public String getSpNameInEfSpn() {
        if (DBG) log("getSpNameInEfSpn(): " + spNameInEfSpn);
        return spNameInEfSpn;
    }

    public String isOperatorMvnoForImsi() {
        if(FeatureOption.MTK_MVNO_SUPPORT == false) {
            if (DBG) log("isOperatorMvnoForImsi(): not MTK_MVNO_SUPPORT");
            return null;
        }

        SpnOverride spnOverride = SpnOverride.getInstance();
        String imsiPattern = spnOverride.isOperatorMvnoForImsi(getOperatorNumeric(), getIMSI());
        if (DBG) log("isOperatorMvnoForImsi(): " + imsiPattern);
        return imsiPattern;
    }


    public String getFirstFullNameInEfPnn() {
        if(mPnnNetworkNames == null || mPnnNetworkNames.size() == 0) {
            if (DBG) log("getFirstFullNameInEfPnn(): empty");
            return null;
        }

        OperatorName opName = mPnnNetworkNames.get(0);
        if (DBG) log("getFirstFullNameInEfPnn(): first fullname: " + opName.sFullName);
        if(opName.sFullName != null)
            return new String(opName.sFullName);
        return null;
    }

    public String isOperatorMvnoForEfPnn() {
        String MCCMNC= getOperatorNumeric();
        String PNN = getFirstFullNameInEfPnn();
        if (DBG) log("isOperatorMvnoForEfPnn(): mccmnc = " + MCCMNC + ", pnn = " + PNN);
        if(SpnOverride.getInstance().getSpnByEfPnn(MCCMNC, PNN) != null)
            return PNN;
        return null;
    }

    public boolean isIccCardProviderAsMvno() {
        String IMSI = getIMSI();
        String SPN = getSpNameInEfSpn();
        String PNN = getFirstFullNameInEfPnn();
        String MCCMNC= getOperatorNumeric();
        if (DBG) log("isIccCardProviderAsMvno(): imsi = " + IMSI + ", mccmnc = " + MCCMNC + ", spn = " + SPN);

        if(SpnOverride.getInstance().getSpnByEfSpn(MCCMNC, SPN) != null)
            return true;

        if(SpnOverride.getInstance().getSpnByImsi(MCCMNC, IMSI) != null)
            return true;

        if(SpnOverride.getInstance().getSpnByEfPnn(MCCMNC, PNN) != null)
            return true;

        return false;
    }
    // MVNO-API END

    public void broadcastPhbStateChangedIntent(boolean isReady) {
        log("broadcastPhbStateChangedIntent, mPhbReady " + mPhbReady + ", mSIMInfoReady " + mSIMInfoReady);
        if (mPhbReady && mSIMInfoReady) {
            Intent intent = new Intent(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
            intent.putExtra("ready", isReady);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mParentApp.getMySimId());
            if (DBG) log("Broadcasting intent ACTION_PHB_STATE_CHANGED " + isReady
                        + " sim id " + mParentApp.getMySimId());
            mContext.sendBroadcast(intent);
        }
    }

    public boolean isPhbReady() {
        if (DBG) log("isPhbReady(): cached mPhbReady = " + (mPhbReady ? "true" : "false"));
        String strPhbReady = null;
        strPhbReady = SystemProperties.get(SIMRECORD_PROPERTY_RIL_PHB_READY[mParentApp.getMySimId()], "false"); 
        
        if (strPhbReady.equals("true")){
            mPhbReady = true;
        } else {
            mPhbReady = false;
        }
        if (DBG) log("isPhbReady(): mPhbReady = " + (mPhbReady ? "true" : "false"));
        return mPhbReady;
    }

    public void setPhbReady(boolean isReady) {
        if (DBG) log("setPhbReady(): isReady = " + (isReady ? "true" : "false"));
        String strPhbReady = isReady ? "true" : "false";
        mPhbReady = isReady;
        SystemProperties.set(SIMRECORD_PROPERTY_RIL_PHB_READY[mParentApp.getMySimId()], strPhbReady); 
    }
}
