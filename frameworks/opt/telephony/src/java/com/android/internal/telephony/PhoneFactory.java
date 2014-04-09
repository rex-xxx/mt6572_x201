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

import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.SystemProperties;

import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.UiccController;

//MTK-START [mtk04070][111117][ALPS00093395]MTK added
import android.os.SystemProperties;
import com.android.internal.telephony.gemini.*;
import com.mediatek.common.featureoption.FeatureOption;
//MTK-END [mtk04070][111117][ALPS00093395]MTK added

/**
 * {@hide}
 */
public class PhoneFactory {
    static final String LOG_TAG = "PHONE";
    static final int SOCKET_OPEN_RETRY_MILLIS = 2 * 1000;
    static final int SOCKET_OPEN_MAX_RETRY = 3;

    public static final int MODE_0_NONE = 0;
    public static final int MODE_1_WG_GEMINI = 1;
    public static final int MODE_2_TG_GEMINI = 2;
    public static final int MODE_3_FDD_SINGLE = 3;
    public static final int MODE_4_TDD_SINGLE = 4;
    public static final int MODE_5_WGNTG_DUALTALK = 5;
    public static final int MODE_6_TGNG_DUALTALK = 6;
    public static final int MODE_7_WGNG_DUALTALK = 7;
    public static final int MODE_8_GNG_DUALTALK = 8;

    //***** Class Variables

    static private Phone sProxyPhone = null;
    static private CommandsInterface sCommandsInterface = null;

    static private boolean sMadeDefaults = false;
    static private PhoneNotifier sPhoneNotifier;
    static private Looper sLooper;
    static private Context sContext;

    static private int sTelephonyMode = -1;
    static private int sFirstMD = -1;
    static private int sExternalMD = -1;

    static final int preferredCdmaSubscription =
                         CdmaSubscriptionSourceManager.PREFERRED_CDMA_SUBSCRIPTION;

    //***** Class Methods

    //MTK-START [mtk04070][111117][ALPS00093395]Use MTKPhoneFactory
    public static void makeDefaultPhones(Context context) {
        if (FeatureOption.MTK_GEMINI_SUPPORT == true){
            SystemProperties.set(Phone.GEMINI_DEFAULT_SIM_MODE, String.valueOf(RILConstants.NETWORK_MODE_GEMINI));
        }else{
            SystemProperties.set(Phone.GEMINI_DEFAULT_SIM_MODE, String.valueOf(RILConstants.NETWORK_MODE_WCDMA_PREF));
        }
        
        MTKPhoneFactory.makeDefaultPhone(context);
    }

    /*
     * This function returns the type of the phone, depending
     * on the network mode.
     *
     * @param network mode
     * @return Phone Type
     */
    public static int getPhoneType(int networkMode) {
		return MTKPhoneFactory.getPhoneType(networkMode);
    }

    public static Phone getDefaultPhone() {
		return MTKPhoneFactory.getDefaultPhone();
    }

    public static Phone getCdmaPhone() {
		return MTKPhoneFactory.getCdmaPhone();
    }

    public static Phone getGsmPhone() {
		return MTKPhoneFactory.getGsmPhone();
    }

    /**
     * Makes a {@link SipPhone} object.
     * @param sipUri the local SIP URI the phone runs on
     * @return the {@code SipPhone} object or null if the SIP URI is not valid
     */
    public static SipPhone makeSipPhone(String sipUri) {
        return MTKPhoneFactory.getSipPhone(sipUri);
    }

    public static boolean isDualTalkMode() {
        return FeatureOption.MTK_DT_SUPPORT;
    }

    public static boolean isFlightModePowerOffMD() {
        return FeatureOption.MTK_FLIGHT_MODE_POWER_OFF_MD;
    }

    public static boolean isRadioOffPowerOffMD() {
        return FeatureOption.MTK_RADIOOFF_POWER_OFF_MD;
    }

    public static int getTelephonyMode() {
        if (sTelephonyMode < 0)
            sTelephonyMode = FeatureOption.MTK_TELEPHONY_MODE; 
        return sTelephonyMode;
    }

    /**
     * Use to check if the first modem is FDD or TDD.
     * @return 1: first modem is FDD, 2: first modem is TDD
     */
    public static int getFirstMD() {
        int telephonyMode = getTelephonyMode();
        switch (telephonyMode) {
            case PhoneFactory.MODE_5_WGNTG_DUALTALK:
              if (sFirstMD < 0) {
                  sFirstMD = SystemProperties.getInt("ril.first.md", 0);
              }
              break;
            
        	case PhoneFactory.MODE_7_WGNG_DUALTALK:
        	case PhoneFactory.MODE_8_GNG_DUALTALK:
        	  sFirstMD = 1;
        	  break;
        	
        	case PhoneFactory.MODE_6_TGNG_DUALTALK:
        	  sFirstMD = 2;
        	  break;
        }    
        
        return sFirstMD;
    }

    public static int getExternalModemSlot() {
        if (sExternalMD < 0)
            sExternalMD = SystemProperties.getInt("ril.external.md", 0);
        return sExternalMD-1;
    }

    public static boolean isEVDODTSupport() {
        return FeatureOption.EVDO_DT_SUPPORT;
    }

}
