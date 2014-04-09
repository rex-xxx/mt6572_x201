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

package com.mediatek.contacts.simcontact;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;

public class BootCmpReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCmpReceiver";
    private static final int DEFAULT_DUAL_SIM_MODE = (1 << SlotUtils.getSlotCount()) - 1;
    private static Context sContext = null;

    ///M: [Gemini+]TODO: should change to SIMRecords.xxx
    private static final String ACTION_SIM_FILE_CHANGED = "android.intent.action.sim.SIM_FILES_CHANGED";
    private static final String KEY_SLOT_ID = "SIM_ID";

    public void onReceive(Context context, Intent intent) {
        sContext = context;
        Log.i(TAG, "In onReceive ");
        final String action = intent.getAction();
        Log.i(TAG, "action is " + action);

        if (action.equals(TelephonyIntents.ACTION_PHB_STATE_CHANGED)) {
            processPhoneBookChanged(intent);
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            processAirplaneModeChanged(intent);
            ///M:[Gemini+] this is OK in triple sims?
        } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
            processDualSimModeChanged(intent);
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            processSimStateChanged(intent);
        } else if (action.equals(ACTION_SIM_FILE_CHANGED)) { // SIM REFERSH
            int slotId = intent.getIntExtra(KEY_SLOT_ID, SlotUtils.getSingleSlotId());
            Log.d(TAG, "[onReceive] ACTION_SIM_FILE_CHANGED has extra: " + intent.hasExtra(KEY_SLOT_ID)
                    + ", and the slot is: " + slotId);
            processSimFilesChanged(slotId);
            /*
             * } else if (action.equals(Intent.SIM_SETTINGS_INFO_CHANGED)) {
             * processSimInfoUpdateForSettingChanged(intent); }
             */
        } else if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
            processIpoShutDown();
        } else if (action.equals("android.intent.action.ACTION_PHONE_RESTART")) {
            processPhoneReset(intent);
        }
    }

    public void startSimService(int slotId, int workType) {
        Intent intent = null;
        intent = new Intent(sContext, StartSIMService.class);
        intent.putExtra(AbstractStartSIMService.SERVICE_SLOT_KEY, slotId);
        intent.putExtra(AbstractStartSIMService.SERVICE_WORK_TYPE, workType);
        Log.i(TAG, "[startSimService]slotId:" + slotId + "|workType:" + workType);
        sContext.startService(intent);
    }

    void processPhoneBookChanged(Intent intent) {
        Log.i(TAG, "processPhoneBookChanged");
        boolean phbReady = intent.getBooleanExtra("ready", false);
        int slotId = intent.getIntExtra("simId", -10);
        Log.i(TAG, "[processPhoneBookChanged]phbReady:" + phbReady + "|slotId:" + slotId);
        if (phbReady && slotId >= 0) {
            startSimService(slotId, AbstractStartSIMService.SERVICE_WORK_IMPORT);
            /*SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getSimWrapperInstanceUnCheck();
            if (simInfoWrapper != null) {
                simInfoWrapper.updateSimInfoCache();
            }*/
        }
    }
    
    void processAirplaneModeChanged(Intent intent) {
        Log.i(TAG, "processAirplaneModeChanged");
        boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.i(TAG, "[processAirplaneModeChanged]isAirplaneModeOn:" + isAirplaneModeOn);
        for (int slotId : SlotUtils.getAllSlotIds()) {
            startSimService(slotId, isAirplaneModeOn ? AbstractStartSIMService.SERVICE_WORK_REMOVE
                    : AbstractStartSIMService.SERVICE_WORK_IMPORT);
        }
    }
    
    /**
     * Dual Sim mode is only for Gemini Feature.
     * each bit of the type stands for the state of each icc card.
     * 1: the icc card is on
     * 0: the icc card is off
     * 
     * for example, 0010 means only slot 1 is on
     * 
     * @param intent
     * 
     * M: Dual sim is upgraded to support triple or more sims
     */
    void processDualSimModeChanged(Intent intent) {
        Log.i(TAG, "processDualSimModeChanged");
        // Intent.EXTRA_DUAL_SIM_MODE = "mode";
        int type = intent.getIntExtra("mode", -1);
        
        SharedPreferences prefs = sContext.getSharedPreferences(
                "sim_setting_preference", Context.MODE_PRIVATE);
        int prevType = prefs.getInt("dual_sim_mode", DEFAULT_DUAL_SIM_MODE);
        
        Log.i(TAG, "[processDualSimModeChanged]type:" + type + "|prevType:" + prevType);
        for (int slotId : SlotUtils.getAllSlotIds()) {
            ///M: e.g. type == 0100 means slot 2 card(the 3rd card) is on
            boolean isPrevOn = ((prevType & (1 << slotId)) != 0);
            boolean isCurrentOn = ((type & (1 << slotId)) != 0);
            if (isPrevOn ^ isCurrentOn) {
                int worktype = isCurrentOn ? AbstractStartSIMService.SERVICE_WORK_IMPORT
                        : AbstractStartSIMService.SERVICE_WORK_REMOVE;
                Log.i(TAG, "[processDualSimModeChanged] slot " + slotId + ", worktype is " + worktype);
                startSimService(slotId, worktype);
            }
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("dual_sim_mode", type);
        editor.commit();
    }
    
    void processSimStateChanged(Intent intent) {
        Log.i(TAG, "processSimStateChanged");
        String phoneName = intent.getStringExtra(PhoneConstants.PHONE_NAME_KEY);
        String iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);

        Log.i(TAG, "mPhoneName:" + phoneName + "|mIccStae:" + iccState
                + "|mySlotId:" + slotId);
        // Check SIM state, and start service to remove old sim data if sim
        // is not ready.
        /*if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(iccState)) {
            SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getSimWrapperInstanceUnCheck();
            if (simInfoWrapper != null) {
                simInfoWrapper.updateSimInfoCache();
            }
        }*/
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(iccState)
                || IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(iccState)
                || IccCardConstants.INTENT_VALUE_LOCKED_NETWORK.equals(iccState)) {
            startSimService(slotId, AbstractStartSIMService.SERVICE_WORK_REMOVE);
        }
        if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(iccState)
                && SimCardUtils.isPhoneBookReady(slotId)) {
            startSimService(slotId, AbstractStartSIMService.SERVICE_WORK_IMPORT);
            /*SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getSimWrapperInstanceUnCheck();
            if (simInfoWrapper != null) {
                simInfoWrapper.updateSimInfoCache();
            }*/
        }
    }

    void processSimFilesChanged(int slotId) {
        Log.i(TAG, "processSimStateChanged:" + slotId);
        if (SimCardUtils.isPhoneBookReady(slotId)) {
            startSimService(slotId, AbstractStartSIMService.SERVICE_WORK_IMPORT);
        }
    }
    
    /*void processSimInfoUpdateForSettingChanged(Intent intent) {
        Log.i(TAG, "processSimInfoUpdateForSettingChanged:" + intent.toString());
        SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getSimWrapperInstanceUnCheck();
        if (simInfoWrapper != null) {
            simInfoWrapper.updateSimInfoCache();
        } else {
            SIMInfoWrapper.getDefault();
        }
    }*/
    
    void processIpoShutDown() {
        for (int slotId : SlotUtils.getAllSlotIds()) {
            startSimService(slotId, AbstractStartSIMService.SERVICE_WORK_REMOVE);
        }
    }

    void processPhoneReset(Intent intent) {
        Log.i(TAG, "processPhoneReset");
        /*SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getSimWrapperInstanceUnCheck();
        if (simInfoWrapper != null) {
            simInfoWrapper.updateSimInfoCache();
        }*/
        if (SlotUtils.isGeminiEnabled()) {
            int slotId = intent.getIntExtra("SimId", -1);
            if (slotId != -1) {
                Log.i(TAG, "processPhoneReset" + slotId);
                startSimService(slotId, AbstractStartSIMService.SERVICE_WORK_IMPORT);
            }
        } else {
            Log.i(TAG, "processPhoneReset0");
            startSimService(SlotUtils.getSingleSlotId(), AbstractStartSIMService.SERVICE_WORK_IMPORT);
        }
    }
}
