/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

//[New R8 modem FD]
package com.android.internal.telephony.gsm;
//package com.mediatek.common.telephony.gsm;

import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.FDModeType;
import com.android.internal.telephony.gsm.FDTimerType;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.featureoption.FeatureOption;
//import com.mediatek.common.telephony.gsm.FDModeType;
//import com.mediatek.common.telephony.gsm.FDTimerype;


public class FDTimer {
	
    private static int numberOfSupportedTypes;
    //Each element represents the timerValue of one specific type defined by FDTimerType
    //Time Unit:0.1 sec => {5sec, 15sec, 5sec, 15sec}
    private static String timerValue[] = {"50", "150", "50", "150"};
		
    protected PhoneBase mPhone;

    protected final String LOG_TAG = "FDTimer";
    protected static final String PROPERTY_RIL_FD_MODE = "ril.fd.mode";
    protected static final String PROPERTY_3G_SWITCH = "gsm.3gswitch";	
    private static final String STR_PROPERTY_FD_SCREEN_ON_TIMER = "persist.radio.fd.counter";
    private static final String STR_PROPERTY_FD_SCREEN_ON_R8_TIMER = "persist.radio.fd.r8.counter";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_TIMER = "persist.radio.fd.off.counter";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER = "persist.radio.fd.off.r8.counter";

    
		
    public FDTimer(PhoneBase p) {
        String timerStr[] = new String[4];		
        mPhone = p;
        //Read default value from the system property
        timerStr[0] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_TIMER, "5");
        timerValue[FDTimerType.ScreenOffLegacyFD.ordinal()] = Integer.toString((int)(Double.parseDouble(timerStr[0])*10));
        timerStr[1] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_TIMER, "15");
        timerValue[FDTimerType.ScreenOnLegacyFD.ordinal()] = Integer.toString((int)(Double.parseDouble(timerStr[1])*10));
        timerStr[2] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER, "5");
        timerValue[FDTimerType.ScreenOffR8FD.ordinal()] = Integer.toString((int)(Double.parseDouble(timerStr[2])*10));
        timerStr[3] = SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_R8_TIMER, "15");
        timerValue[FDTimerType.ScreenOnR8FD.ordinal()] = Integer.toString((int)(Double.parseDouble(timerStr[3])*10));
        Log.d(LOG_TAG,"Default FD timers=" + timerValue[0] + "," + timerValue[1] + "," + timerValue[2] + "," + timerValue[3]);		
    }		
    public int getNumberOfSupportedTypes() {
        return FDTimerType.SupportedTimerTypes.ordinal();
    }
    public int setFDTimerValue(String newTimerValue[], Message onComplete) {
        int FD_MD_Enable_Mode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        int FDSimID = SystemProperties.getInt("gsm.3gswitch", 1) == 2 ? PhoneConstants.GEMINI_SIM_2 : PhoneConstants.GEMINI_SIM_1; 
        if (FeatureOption.MTK_FD_SUPPORT && FD_MD_Enable_Mode == 1 && mPhone.getMySimId() == FDSimID) {
            for (int i=0; i < newTimerValue.length; i++) {
                timerValue[i] = newTimerValue[i];
            }
            // Invoke the setFDMode() to execute
            mPhone.mCM.setFDMode(FDModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FDTimerType.ScreenOffLegacyFD.ordinal(), Integer.parseInt(timerValue[FDTimerType.ScreenOffLegacyFD.ordinal()]), null);
            mPhone.mCM.setFDMode(FDModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FDTimerType.ScreenOnLegacyFD.ordinal(), Integer.parseInt(timerValue[FDTimerType.ScreenOnLegacyFD.ordinal()]), null);
            mPhone.mCM.setFDMode(FDModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FDTimerType.ScreenOffR8FD.ordinal(), Integer.parseInt(timerValue[FDTimerType.ScreenOffR8FD.ordinal()]), null);
            mPhone.mCM.setFDMode(FDModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FDTimerType.ScreenOnR8FD.ordinal(), Integer.parseInt(timerValue[FDTimerType.ScreenOnR8FD.ordinal()]), onComplete);
        }
        return 0;		
    }
    public String[] getDefaultTimerValue() {
        return null;
    }		

	public String[] getFDTimerValue() {
        return timerValue;
    }		

}

