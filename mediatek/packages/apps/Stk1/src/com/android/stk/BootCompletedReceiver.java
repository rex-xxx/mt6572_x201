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

package com.android.stk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatService;
import android.provider.Settings.System;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.mediatek.common.featureoption.FeatureOption;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.android.internal.telephony.ITelephony;

/**
 * Boot completed receiver. used to reset the app install state every time the
 * device boots.
 *
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";
    public static final String EXTRA_VALUE_REMOVE_SIM = "REMOVE";
    private static boolean mHasBootComplete = false;

    private static final String LOGTAG = "Stk-BCR ";

    private boolean checkSimRadioState(Context context, int sim_id)
    {
        int dualSimMode = -1;
        boolean result = false;

        /* dualSimMode: 0 => both are off, 1 => SIM1 is on, 2 => SIM2 is on, 3 => both is on */
        dualSimMode = Settings.System.getInt(context.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1);

        CatLog.d(LOGTAG, "dualSimMode: " + dualSimMode + ", sim id: " + sim_id);
        switch (sim_id)
        {
            case PhoneConstants.GEMINI_SIM_1:
                if ((dualSimMode == 1) || (dualSimMode == 3))
                {
                    result = true;
                }
                break;
            case PhoneConstants.GEMINI_SIM_2:
                if ((dualSimMode == 2) || (dualSimMode == 3))
                {
                    result = true;
                }
                break;
        }
        return result;
    }

    boolean isOnFlightMode(Context context) {
        int mode = 0;

        /*For OP02 spec v4.1 start*/
        String optr = SystemProperties.get("ro.operator.optr");
        if (optr != null && "OP02".equals(optr)) {   
            CatLog.d(this, "[isOnFlightMode]working for OP02...");  
            return false; //For OP02, the stk install state can not be changed by flight mode.
        }
        //In Normal case, if flight mode is on, we should uninstall stk apk.
        try {
            mode = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
        } catch(SettingNotFoundException e) {
            CatLog.d(LOGTAG, "fail to get airlane mode");
            mode = 0;
        }
        
        CatLog.d(LOGTAG, "airlane mode is " + mode);
        return (mode != 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        StkAppInstaller appInstaller = StkAppInstaller.getInstance();
        StkAppService appService = StkAppService.getInstance();
        
        // make sure the app icon is removed every time the device boots.
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Bundle args = new Bundle();
            int[] op = new int[2];
            op[0] = StkAppService.OP_BOOT_COMPLETED;
            op[1] = StkAppService.STK_GEMINI_BROADCAST_ALL;
            args.putIntArray(StkAppService.OPCODE, op);
            context.startService(new Intent(context, StkAppService.class)
                    .putExtras(args));
            /* TODO: Gemini+ begine */
            if (FeatureOption.MTK_GEMINI_SUPPORT != true)
            {
                /* Hide icon of SIM2-SIM4 */
                appInstaller.unInstall(context, PhoneConstants.GEMINI_SIM_2);
                appInstaller.unInstall(context, PhoneConstants.GEMINI_SIM_3);
                appInstaller.unInstall(context, PhoneConstants.GEMINI_SIM_4);
            }
            else
            {
                if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT != true && FeatureOption.MTK_GEMINI_4SIM_SUPPORT != true)
                {
                    appInstaller.unInstall(context, PhoneConstants.GEMINI_SIM_3);
                }
                if (FeatureOption.MTK_GEMINI_4SIM_SUPPORT != true)
                {
                    appInstaller.unInstall(context, PhoneConstants.GEMINI_SIM_4);
                }
            }
            /* TODO: Gemini+ end */
            mHasBootComplete = true;
            CatLog.d(LOGTAG, "[ACTION_BOOT_COMPLETED]");
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            CatLog.d(LOGTAG, "get ACTION_SIM_STATE_CHANGED");

            int SIMID = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY,-1);
            String SIMStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            CatLog.d(LOGTAG, "[ACTION_SIM_STATE_CHANGED][simId] : " + SIMID);
            CatLog.d(LOGTAG, "[ACTION_SIM_STATE_CHANGED][SimStatus] : " + SIMStatus);
            if(SIMID >= PhoneConstants.GEMINI_SIM_1 && SIMID <= PhoneConstants.GEMINI_SIM_4){
                //deal with SIM1
                CatLog.d(LOGTAG, "[ACTION_SIM_STATE_CHANGED][Feature GEMINI]");
                Bundle bundle = new Bundle();
                bundle.putString("affinity", "com.android.stk");
                final Intent it = new Intent();
                it.putExtras(bundle);

                CatLog.d(LOGTAG, "isSetupMenuCalled[" + StkAppService.isSetupMenuCalled(SIMID) + "]");
                CatLog.d(LOGTAG, "mHasBootComplete[" + mHasBootComplete + "]");
                
                boolean bUnInstall = true;
                if ((StkAppService.isSetupMenuCalled(SIMID)) && (((IccCardConstants.INTENT_VALUE_ICC_READY).equals(SIMStatus))||((IccCardConstants.INTENT_VALUE_ICC_IMSI).equals(SIMStatus))||((IccCardConstants.INTENT_VALUE_ICC_LOADED).equals(SIMStatus)))) {
                    bUnInstall = false;
                }
                
                int app_state = appInstaller.getIsInstalled(SIMID);
                if (app_state == -1)
                {
                    CatLog.d(LOGTAG, "Initialize the app state in launcher");
                    appInstaller.install(context, SIMID);
                    if (checkSimRadioState(context, SIMID) != true || true == isOnFlightMode(context))
                    {
                        /* The SIM card is off so uninstall it */
                        appInstaller.unInstall(context, SIMID);
                    }                    
                }
                if ((((IccCardConstants.INTENT_VALUE_ICC_READY).equals(SIMStatus)) || ((IccCardConstants.INTENT_VALUE_ICC_LOADED).equals(SIMStatus)))
                    && mHasBootComplete && !CatService.getSaveNewSetUpMenuFlag(SIMID))
                {
                    /* 1. SIM ready and get intent boot_complete. It must be phone reboot. 
                       2. In the case, if we still not get SET_UP_MENU, the SIM card may not support SAT 
                    */
                    if (((IccCardConstants.INTENT_VALUE_ICC_LOADED).equals(SIMStatus)))
                    {
                        CatLog.d(LOGTAG, "Disable the STK of sim" + (SIMID+1) + " because still not receive SET_UP_MENU after boot up");
                        appService.StkAvailable(SIMID, StkAppService.STK_AVAIL_NOT_AVAILABLE);
                        appInstaller.unInstall(context, SIMID);
                        appService.setUserAccessState(false, SIMID);
                        bUnInstall = true;
                    }
                    else
                    {
                        /* Remove that SET_UP_MENU command from DB */
                        Bundle args = new Bundle();
                        int[] op = new int[2];
                        op[0] = StkAppService.OP_REMOVE_STM;
                        op[1] = SIMID;
                        args.putIntArray(StkAppService.OPCODE, op);
                        context.startService(new Intent(context, StkAppService.class).putExtras(args));
                    }
                }
                else {
                    if(null == appService && ((IccCardConstants.INTENT_VALUE_ICC_NOT_READY).equals(SIMStatus))) {
                        CatLog.d(LOGTAG, "null == appService && NOT_READY, start StkAppService.");                        
                        Bundle args = new Bundle();
                        int[] op = new int[2];
                        op[0] = StkAppService.OP_BOOT_COMPLETED;
                        op[1] = StkAppService.STK_GEMINI_BROADCAST_ALL;
                        args.putIntArray(StkAppService.OPCODE, op);
                        context.startService(new Intent(context, StkAppService.class)
                                .putExtras(args));                        
                    }
                }

                if (appService != null)
                {
                    int currentState = appService.StkQueryAvailable(SIMID);
                    CatLog.d(LOGTAG, "[ACTION_SIM_STATE_CHANGED][bUnInstall] : " + bUnInstall + ", currentState: " + currentState);
                    if (bUnInstall && app_state == StkAppInstaller.STK_INSTALLED) {
                        CatLog.d(LOGTAG, "ADD_RECENET_IGNORE");
                        it.setAction("android.intent.action.ADD_RECENET_IGNORE");
                        context.sendBroadcast(it);

                        CatLog.d(LOGTAG, "get ACTION_SIM_STATE_CHANGED - unInstall");
                        appService.StkAvailable(SIMID, StkAppService.STK_AVAIL_NOT_AVAILABLE);
                        /*For OP02 spec v4.1 start*/
                        String optr = SystemProperties.get("ro.operator.optr");
                        if (optr != null && "OP02".equals(optr)) {   
                            CatLog.d(this, "working for OP02...");                            
                            if((IccCardConstants.INTENT_VALUE_ICC_ABSENT).equals(SIMStatus) || checkSimRadioState(context, SIMID) == false){
                                CatLog.d(this, "unInstall it~~");                                                            
                                appInstaller.unInstall(context, SIMID);
                            }                            
                        }/*For OP02 spec v4.1 end*/
                        else {
                            appInstaller.unInstall(context, SIMID);
                        }                                                
                        appService.setUserAccessState(false, SIMID);
                    } else if (!bUnInstall && app_state == StkAppInstaller.STK_NOT_INSTALLED){
                        CatLog.d(LOGTAG, "REMOVE_RECENET_IGNORE");
                        it.setAction("android.intent.action.REMOVE_RECENET_IGNORE");
                        context.sendBroadcast(it);
                        
                        CatLog.d(LOGTAG, "get ACTION_SIM_STATE_CHANGED - install");
                        appInstaller.install(context, SIMID);
                        appService.StkAvailable(SIMID, StkAppService.STK_AVAIL_AVAILABLE);
                    }
                }
                else
                {
                    CatLog.d(LOGTAG, "get ACTION_SIM_STATE_CHANGED - StkAppService instance is null");
                }
            }
            CatLog.d(LOGTAG, "get ACTION_SIM_STATE_CHANGED  finish");
        } else if (action.equals(TelephonyIntents.ACTION_SIM_DETECTED))
        {
            /* SIM detected */
            /* TODO: To do experiment if it can work like CARD plug-in/plug-out and it is earilier than catserice's constructor or not. */
            CatLog.d(LOGTAG, "get SIM_DETECTED begin");
            
            String status = intent.getStringExtra(INTENT_KEY_DETECT_STATUS);
            CatLog.d(LOGTAG, "SIM_DETECTED, status: " + status);
            if ((EXTRA_VALUE_REMOVE_SIM).equals(status))
            {
                int i = 0;
                ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                if (iTel != null)
                {
                    boolean isSimInserted = true;
                    for (i = 0; i < StkAppService.STK_GEMINI_SIM_NUM; i++)
                    {
                        try {
                            isSimInserted = iTel.isSimInsert(i);
                            if (!isSimInserted)
                            {
                                /* Remove that SET_UP_MENU command from DB */
                                CatLog.d(LOGTAG, "SIM_DETECTED, removed sim: " + i);
                                Bundle args = new Bundle();
                                int[] op = new int[2];
                                op[0] = StkAppService.OP_REMOVE_STM;
                                op[1] = i;
                                args.putIntArray(StkAppService.OPCODE, op);
                                context.startService(new Intent(context, StkAppService.class).putExtras(args));
                            }
                        }
                        catch (Exception ex)
                        {
                            CatLog.d(LOGTAG, "Query Sim insert status fail");
                        }
                    }
                }
                else
                {
                    CatLog.d(LOGTAG, "ITelephony is null");
                }
            }
            CatLog.d(LOGTAG, "get SIM_DETECTED end");
        }
    }
}
