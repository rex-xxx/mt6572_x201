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

/*
 *
 */

package com.android.internal.telephony;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.net.ConnectivityManager;
import android.net.sip.SipManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import java.util.List;
import android.os.UserHandle;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.ITelephonyExt;
import com.android.internal.telephony.gemini.GeminiPhone;

public class DefaultSIMSettings {
    private static final String LOG_TAG = "PHONE";

    private static int[] mInsertSimState;
    /* SIM inserted status constants */
    private static final int STATUS_NO_SIM_INSERTED = 0x00;
    private static final int STATUS_SIM1_INSERTED = 0x01;    
    private static final int STATUS_SIM2_INSERTED = 0x02;
    private static final int STATUS_SIM3_INSERTED = 0x04;
    private static final int STATUS_SIM4_INSERTED = 0x08; 
    private static final int STATUS_DUAL_SIM_INSERTED = STATUS_SIM1_INSERTED | STATUS_SIM2_INSERTED;
    protected static final int STATUS_TRIPLE_SIM_INSERTED = STATUS_SIM1_INSERTED | STATUS_SIM2_INSERTED | STATUS_SIM3_INSERTED;            
    protected static final int STATUS_QUAD_SIM_INSERTED = STATUS_SIM1_INSERTED | STATUS_SIM2_INSERTED | STATUS_SIM3_INSERTED | STATUS_SIM4_INSERTED;            

    public static final String ACTION_ON_SIM_DETECTED = "ACTION_ON_SIM_DETECTED";

    public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";
    public static final String INTENT_KEY_SIM_COUNT = "simCount";
    public static final String INTENT_KEY_NEW_SIM_SLOT = "newSIMSlot";
    public static final String INTENT_KEY_NEW_SIM_STATUS = "newSIMStatus";
    public static final String EXTRA_VALUE_NEW_SIM = "NEW";
    public static final String EXTRA_VALUE_REMOVE_SIM = "REMOVE";
    public static final String EXTRA_VALUE_SWAP_SIM = "SWAP";

    static private String[] DEFAULTSIMSETTING_PROPERTY_ICC_OPERATOR_DEFAULT_NAME = {
        TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME,
        TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2,
        TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME_3,
        TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME_4
    };

    synchronized public static void onAllIccidQueryComplete(Context context, Phone phone,
        String iccid1, String iccid2, String iccid3, String iccid4, boolean is3GSwitched)
    {
        logd("onAllIccidQueryComplete start");
        iccid1 = iccid1 == null ? "" : iccid1;
        iccid2 = iccid2 == null ? "" : iccid2;
        iccid3 = iccid3 == null ? "" : iccid3;
        iccid4 = iccid4 == null ? "" : iccid4;
        String[] iccId = {iccid1, iccid2, iccid3, iccid4};
        boolean[] isSIMInserted = {!"".equals(iccId[0]),!"".equals(iccId[1]),
                                      !"".equals(iccId[2]),!"".equals(iccId[3])};        
        ContentResolver contentResolver = context.getContentResolver();
        String[] oldIccIdInSlot = new String[PhoneConstants.GEMINI_SIM_NUM];
        ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));

        ITelephonyExt telephonyExt = null;
        try {
            telephonyExt = MediatekClassFactory.createInstance(ITelephonyExt.class);
        } catch (Exception e){
            e.printStackTrace();
        }
        
        /*
         *  int[] mInsertSimState maintains all slots' SIM inserted status currently, 
         *  it may contain 4 kinds of values:
         *    SIM_NOT_INSERT : no SIM inserted in slot i now
         *    SIM_CHANGED    : a valid SIM insert in slot i and is different SIM from last time
         *                     it will later become SIM_NEW or SIM_REPOSITION during update procedure
         *    SIM_NOT_CHANGE : a valid SIM insert in slot i and is the same SIM as last time
         *    SIM_NEW        : a valid SIM insert in slot i and is a new SIM
         *    SIM_REPOSITION    : a valid SIM insert in slot i and is inserted in different slot last time
         *    positive integer #: index to distinguish SIM cards with the same IccId
         */
        int index = 0;
        int SIM_NOT_CHANGE = 0;
        int SIM_CHANGED = -1;
        int SIM_NEW = -2;
        int SIM_REPOSITION = -3;
        int SIM_NOT_INSERT = -99;
        mInsertSimState = new int[PhoneConstants.GEMINI_SIM_NUM];

        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            mInsertSimState[i] = SIM_NOT_CHANGE;
        }
        // not insert and identical IccId states are indicated
        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            if (iccId[i] == null || iccId[i].equals("")) {
                mInsertSimState[i] = SIM_NOT_INSERT;
                continue;
            }
            index = 2;
            for (int j=i+1; j<PhoneConstants.GEMINI_SIM_NUM; j++) {
                if (mInsertSimState[j] == SIM_NOT_CHANGE && iccId[i].equals(iccId[j])) {
                    // SIM i adn SIM j has equal IccId, they are invalid SIMs
                    mInsertSimState[i] = 1;
                    mInsertSimState[j] = index;
                    index++;
                }
            }
        }
        for(int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            SIMInfo oldSimInfo = SIMInfo.getSIMInfoBySlot(context, i);
            if (oldSimInfo != null) {
                oldIccIdInSlot[i] = oldSimInfo.mICCId;
                logd("old IccId In Slot" + i + " is " + oldIccIdInSlot[i] + " oldSimId:" + oldSimInfo.mSimId); 
                // slot_i has different SIM states is indicated
                if (mInsertSimState[i] == SIM_NOT_CHANGE && !iccId[i].equals(oldIccIdInSlot[i])) {
                    mInsertSimState[i] = SIM_CHANGED;
                }
                if(mInsertSimState[i] != SIM_NOT_CHANGE) {
                    ContentValues value = new ContentValues(1);
                    value.put(SimInfo.SLOT, -1);
                    contentResolver.update(ContentUris.withAppendedId(SimInfo.CONTENT_URI, oldSimInfo.mSimId), 
                            value, null, null);
                    logd("reset Slot" + i + " to -1, iccId[ " + i + "]= " + iccId[i]); 
                }
            } else {
                if (mInsertSimState[i] == SIM_NOT_CHANGE) {
                    // no SIM inserted last time, but there is one SIM inserted now
                    mInsertSimState[i] = SIM_CHANGED;
                }
                oldIccIdInSlot[i] = "";
                logd("No sim in slot " + i + " for last time"); 
            }
        }
        
        //check if the Inserted sim is new
        int nNewCardCount = 0;
        int nNewSIMStatus = 0;
        
        for(int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            if (mInsertSimState[i] == SIM_NOT_INSERT) {
                logd("No SIM inserted in Slot " + i + ", set the slot for Removed SIM to NONE " );
            } else {
                logd("iccId[ " + i + "] : " + iccId[i] + ", oldIccIdInSlot[" + i + "] : " + oldIccIdInSlot[i]);
                if (mInsertSimState[i] > 0) {
                    //some special SIM cards have no invalid ICCID. so we add a suffix to prove that we can show two SIM cards 
                    //even if two SIMs of that kind are inserted at the same time.  So this kind of SIM will be always treated as a new SIM.
                    SIMInfo.insertICCId(context, iccId[i] + Integer.toString(mInsertSimState[i]), i); 
                    logd("special SIM with invalid ICCID is inserted in slot" + i );
                } else if (mInsertSimState[i] == SIM_CHANGED) {
                    SIMInfo.insertICCId(context, iccId[i], i); 
                }
                
                if(isNewSimInserted(iccId[i], oldIccIdInSlot, PhoneConstants.GEMINI_SIM_NUM)) {
                    //one new card inserted into slot1
                    nNewCardCount++;
                    switch (i) {
                        case PhoneConstants.GEMINI_SIM_1:
                            nNewSIMStatus |= STATUS_SIM1_INSERTED;
                            break;
                        case PhoneConstants.GEMINI_SIM_2:
                            nNewSIMStatus |= STATUS_SIM2_INSERTED;
                            break;
                        case PhoneConstants.GEMINI_SIM_3:
                            nNewSIMStatus |= STATUS_SIM3_INSERTED;
                            break;
                        case PhoneConstants.GEMINI_SIM_4:
                            nNewSIMStatus |= STATUS_SIM4_INSERTED;
                            break;
                    }
                    // new SIM card now assign to be SIM_NEW state
                    mInsertSimState[i] = SIM_NEW;
                }
            }
        }
        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            if (mInsertSimState[i] == SIM_CHANGED) {
                mInsertSimState[i] = SIM_REPOSITION;
            }
        }
        long[] simIdForSlot = {-3, -3, -3, -3};
        List<SIMInfo> simInfos = SIMInfo.getInsertedSIMList(context);
        int nSIMCount = (simInfos == null ? 0 : simInfos.size());
        logd("nSIMCount="+nSIMCount);
        logd("[DefaultSimSetting] getAllIccIdsDone ");
        for (int i=0; i<nSIMCount; i++) {
            SIMInfo temp = simInfos.get(i);
            simIdForSlot[temp.mSlot] = temp.mSimId;
            logd("getAllIccIdsDone simIdForSlot [" + i + "] = " + simIdForSlot[i] );
            
            }           
        
        if (nNewCardCount > 0 ) {   
            logd("onAllIccidQueryComplete New SIM detected. " ); 
            setColorForNewSIM(simInfos, context);
            int airplaneMode = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
            if (airplaneMode > 0) {
                setDefaultNameForAllNewSIM(simInfos, context);
            } else {
                setDefaultNameIfImsiReadyOrLocked(simInfos, context);
            }

        }  

        //if (FeatureOption.MTK_GEMINI_ENHANCEMENT) {
        if (!FeatureOption.MTK_BSP_PACKAGE) {
            //get all default SIM setting
            long oldVTDefaultSIM = Settings.System.getLong(contentResolver, Settings.System.VIDEO_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            long oldVoiceCallDefaultSIM =  Settings.System.getLong(contentResolver, 
                    Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            long oldSmsDefaultSIM = Settings.System.getLong(contentResolver, Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET); 
            long oldGprsDefaultSIM = Settings.System.getLong(contentResolver, Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET); 

            if (FeatureOption.MTK_VT3G324M_SUPPORT) {            
                long nVTDefSIM = Settings.System.DEFAULT_SIM_NOT_SET;
                int n3gSIMSlot = get3GSimId(); 
                if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                    if(isSIMInserted[n3gSIMSlot]) {
                        nVTDefSIM = simIdForSlot[n3gSIMSlot];
                    }   
                } else {
                    if(isSIMInserted[PhoneConstants.GEMINI_SIM_1]){
                        nVTDefSIM = simIdForSlot[PhoneConstants.GEMINI_SIM_1];
                    }
                }

                Settings.System.putLong(contentResolver, Settings.System.VIDEO_CALL_SIM_SETTING, nVTDefSIM);
            }

            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
            if (nSIMCount > 1) {
                if (oldVoiceCallDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET) {
                    Settings.System.putLong(contentResolver, Settings.System.VOICE_CALL_SIM_SETTING, 
                                               Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK);
                }

                if (oldSmsDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET) {
                    logd(SystemProperties.get("ro.operator.optr"));
                    if ("OP01".equals(SystemProperties.get("ro.operator.optr"))) {
                        Settings.System.putLong(contentResolver, Settings.System.SMS_SIM_SETTING, 
                                                Settings.System.SMS_SIM_SETTING_AUTO);
                    } else {
                        Settings.System.putLong(contentResolver, Settings.System.SMS_SIM_SETTING, 
                                                Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK);
                    }
                }
                
                if (oldGprsDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET) {
                    if (telephonyExt.isDefaultDataOn()) {
                        if (is3GSwitched)
                            connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_2);
                        else
                            connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_1);
                    } else {
                        connectivityManager.setMobileDataEnabled(false);
                    }
                }
            } else if (nSIMCount == 1) {
                long simId = simInfos.get(0).mSimId;
                int isVoipEnabled = Settings.System.getInt(contentResolver, Settings.System.ENABLE_INTERNET_CALL, 0);
                if (!(SipManager.isVoipSupported(context) && isVoipEnabled != 0) ||
                    (oldVoiceCallDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET))
                {
                    Settings.System.putLong(contentResolver, Settings.System.VOICE_CALL_SIM_SETTING, simId);
                }

                Settings.System.putLong(contentResolver, Settings.System.SMS_SIM_SETTING, simId);

                // [mtk02772]
                // TODO: Data connection has different behavior between Gemini and non-Gemini,
                //       This part should be extracted to itself from SIM framework
                if (FeatureOption.MTK_GEMINI_SUPPORT == true) {   
                    if (oldGprsDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET) {
                        if (telephonyExt.isDefaultDataOn()) {
                            connectivityManager.setMobileDataEnabledGemini(simInfos.get(0).mSlot);
                        } else {
                            connectivityManager.setMobileDataEnabled(false);
                        }
                    }
                } else {
                    if(oldGprsDefaultSIM == Settings.System.DEFAULT_SIM_NOT_SET){
                        Settings.System.putLong(contentResolver, Settings.System.GPRS_CONNECTION_SIM_SETTING, simId);
                    }   
                }
            }

            boolean hasSIMRemoved = false;
            if(PhoneConstants.GEMINI_SIM_NUM == 1) {
                hasSIMRemoved = (iccid1.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1].equals(""));
            } else if(PhoneConstants.GEMINI_SIM_NUM == 2){
                hasSIMRemoved = ((iccid1.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1].equals("")) ||   
                    (iccid2.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_2].equals("")));
                logd("onAllIccidQueryComplete. handling SIM detect dialog [" + iccid1 + ", " + iccid2 + "] ["  
                    + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1] + ", " + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_2] 
                    + "]" + ", " + nNewCardCount + ", " + hasSIMRemoved + ", " + nSIMCount + "]");
            } else if(PhoneConstants.GEMINI_SIM_NUM == 3) {
                hasSIMRemoved = ((iccid1.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1].equals("")) ||   
                    (iccid2.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_2].equals("")) ||   
                    (iccid3.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_3].equals("")));
                logd("onAllIccidQueryComplete. handling SIM detect dialog [" + iccid1 + ", " + iccid2 + ", " + iccid3 + "] ["  
                    + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1] + ", " + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_2] 
                    + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_3] + "]" + ", " + nNewCardCount + ", " + hasSIMRemoved + ", " + nSIMCount + "]");
            } else if(PhoneConstants.GEMINI_SIM_NUM == 4) {
                hasSIMRemoved = ((iccid1.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1].equals("")) ||   
                    (iccid2.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_2].equals("")) ||   
                    (iccid3.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_3].equals("")) ||   
                    (iccid4.equals("") && !oldIccIdInSlot[PhoneConstants.GEMINI_SIM_4].equals("")));
            logd("onAllIccidQueryComplete. handling SIM detect dialog [" + iccid1 + ", " + iccid2 + ", " + iccid3 + ", " + iccid4 + "] ["  
                + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_1] + ", " + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_2] 
                + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_3] + oldIccIdInSlot[PhoneConstants.GEMINI_SIM_4] + "]"
                + ", " + nNewCardCount + ", " + hasSIMRemoved + ", " + nSIMCount + "]");
            }

            long defSIM = Settings.System.DEFAULT_SIM_NOT_SET;
            if (nSIMCount > 1) {
                defSIM = Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK;  
            } else if(nSIMCount == 1) {
                defSIM = simInfos.get(0).mSimId;
            }

            if(isSIMRemoved(oldVoiceCallDefaultSIM,simIdForSlot,PhoneConstants.GEMINI_SIM_NUM)){
                Settings.System.putLong(contentResolver, Settings.System.VOICE_CALL_SIM_SETTING, defSIM);
            }

            if(isSIMRemoved(oldSmsDefaultSIM,simIdForSlot,PhoneConstants.GEMINI_SIM_NUM)){
                long defaultSmsSim = defSIM;
                logd(SystemProperties.get("ro.operator.optr"));
                if ("OP01".equals(SystemProperties.get("ro.operator.optr"))) {
                    if (nSIMCount > 1) {
                        defaultSmsSim = Settings.System.SMS_SIM_SETTING_AUTO;
                    } else if(nSIMCount == 1) {
                        defaultSmsSim = simInfos.get(0).mSimId;
                    }
                }
                Settings.System.putLong(contentResolver, Settings.System.SMS_SIM_SETTING, defaultSmsSim);
            }

            // [mtk02772]
            // TODO: Data connection has different behavior between Gemini and non-Gemini,
            //       This part should be extracted to itself from SIM framework
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) { 
                if(isSIMRemoved(oldGprsDefaultSIM,simIdForSlot,PhoneConstants.GEMINI_SIM_NUM)){
                    if (telephonyExt.isDefaultDataOn()) {
                        if (nSIMCount > 1) {
                            if (is3GSwitched)
                                connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_2);
                            else
                                connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_1);
                        } else {
                            if (nSIMCount > 0)
                                connectivityManager.setMobileDataEnabledGemini(simInfos.get(0).mSlot);
                        }
                    } else {
                        connectivityManager.setMobileDataEnabled(false);
                    }
                } else if (telephonyExt.isDefaultEnable3GSIMDataWhenNewSIMInserted()) {
                    if (oldGprsDefaultSIM > 0) {
                        //if (nNewCardCount > 0 || (!iccid1.equals("") && iccid1.equals(oldIccIdInSlot2)) || (!iccid2.equals("") && iccid2.equals(oldIccIdInSlot1))) {
                        if(nNewCardCount > 0) {
                            logd("onAllIccidQueryComplete. SIM swapped and data on, default switch to 3G SIM");
                            if (nSIMCount > 1) {
                                if (is3GSwitched)
                                    connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_2);
                                else
                                    connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_1);
                            } else {
                                if (nSIMCount > 0)
                                    connectivityManager.setMobileDataEnabledGemini(simInfos.get(0).mSlot);
                            }
                        }
                    } else {
                        if (nNewCardCount > 0 && nNewCardCount == nSIMCount) {
                            logd("onAllIccidQueryComplete. All SIM new, data off and default switch data to 3G SIM");
                            if (nSIMCount > 1) {
                                if (is3GSwitched)
                                    connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_2);
                                else
                                    connectivityManager.setMobileDataEnabledGemini(PhoneConstants.GEMINI_SIM_1);
                            } else {
                                if (nSIMCount > 0)
                                    connectivityManager.setMobileDataEnabledGemini(simInfos.get(0).mSlot);
                            }
                        }
                    }
                }
            } else { // non-Gemini 
                if(isSIMRemoved(oldGprsDefaultSIM,simIdForSlot,PhoneConstants.GEMINI_SIM_NUM)){
                    Settings.System.putLong(contentResolver, Settings.System.GPRS_CONNECTION_SIM_SETTING, 
                                                              Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER);  
                }
            }

            if (nNewCardCount == 0) {
                int i;
                if (!hasSIMRemoved) {
                    // no SIM is removed, no new SIM, just check if any SIM is repositioned
                    for (i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
                        if (mInsertSimState[i] == SIM_REPOSITION) {
                            logd("onAllIccidQueryComplete. SIM swapped");
                            onSIMDetected(context, EXTRA_VALUE_SWAP_SIM, nSIMCount, nNewSIMStatus);
                            break;
                        }
                    }
                    if (i == PhoneConstants.GEMINI_SIM_NUM) {
                        // no SIM is removed, no new SIM, no SIM is repositioned => all status remain unchanged
                        logd("onAllIccidQueryComplete. all SIM inserted into the same slot");
                    }
                } else {
                    // no new SIM, at least one SIM is removed, check if any SIM is repositioned first
                    for (i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
                        if (mInsertSimState[i] == SIM_REPOSITION) {
                            logd("onAllIccidQueryComplete. SIM swapped");
                            onSIMDetected(context, EXTRA_VALUE_SWAP_SIM, nSIMCount, nNewSIMStatus);
                            break;
                        }
                    }
                    if (i == PhoneConstants.GEMINI_SIM_NUM) {
                        // no new SIM, no SIM is repositioned => at least one SIM is removed
                        logd("onAllIccidQueryComplete No new SIM detected and Default SIM for some service has been removed[A]" );
                        onSIMDetected(context, EXTRA_VALUE_REMOVE_SIM, nSIMCount, nNewSIMStatus);
                    }
                }
            } else {
                logd("getAllIccIdsDone. New SIM detected.");
                onSIMDetected(context, EXTRA_VALUE_NEW_SIM, nSIMCount, nNewSIMStatus);
            }

            long gprsDefaultSIM = Settings.System.getLong(contentResolver, Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            if (gprsDefaultSIM != Settings.System.DEFAULT_SIM_NOT_SET && gprsDefaultSIM != Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                int slot = SIMInfo.getSlotById(context, gprsDefaultSIM);            
                if (slot != SimInfo.SLOT_NONE) {
                    //if (FeatureOption.MTK_GEMINI_SUPPORT)
                    //    ((GeminiPhone)phone).setGprsConnType(1, slot);
                    connectivityManager.setMobileDataEnabledGemini(slot);
                } else {
                    logd("onAllIccidQueryComplete: gprsDefaultSIM does not exist in slot then skip.");
                }
            }
        } else {
                ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
                int gprsDefaultSlot = Settings.System.getInt(contentResolver, Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT) - 1;
                logd("getAllIccIdsDone original data settings: " + gprsDefaultSlot);
                if (gprsDefaultSlot == Settings.System.DEFAULT_SIM_NOT_SET_INT) {
                    if (telephonyExt.isDefaultDataOn()){
                        int m3GSimId = get3GSimId();
                        if (isSIMInserted[m3GSimId])
                            connectivityManager.setMobileDataEnabledGemini(m3GSimId);                       
/*                      
                        if (is3GSwitched()) {
                            if (isSimInsert(Phone.GEMINI_SIM_2))
                                connectivityManager.setMobileDataEnabledGemini(Phone.GEMINI_SIM_2);
                            else if (isSimInsert(Phone.GEMINI_SIM_1))
                                connectivityManager.setMobileDataEnabledGemini(Phone.GEMINI_SIM_1);
                        } else {
                            if (isSimInsert(Phone.GEMINI_SIM_1))
                                connectivityManager.setMobileDataEnabledGemini(Phone.GEMINI_SIM_1);
                            else if (isSimInsert(Phone.GEMINI_SIM_2))
                                connectivityManager.setMobileDataEnabledGemini(Phone.GEMINI_SIM_2);
                        }
*/                        
                    } else {
                        connectivityManager.setMobileDataEnabled(false);
                    }
                } else if (gprsDefaultSlot >= PhoneConstants.GEMINI_SIM_1 && gprsDefaultSlot < PhoneConstants.GEMINI_SIM_NUM) {
                    if (isSIMInserted[gprsDefaultSlot]) { //data SIM slot has SIM inserted
                        logd("getAllIccIdsDone gprsDefaultSlot is inserted then enable it.");
                        connectivityManager.setMobileDataEnabledGemini(gprsDefaultSlot);
                    } else {
                        if (telephonyExt.isDefaultDataOn()){ //data SIM slot has no SIM and default data on
                            for (int i=PhoneConstants.GEMINI_SIM_1; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
                                if (isSIMInserted[i]) {
                                    logd("getAllIccIdsDone enabled data of valid SIM: " + i);
                                    connectivityManager.setMobileDataEnabledGemini(i);
                                }
                            }
                        } else { //data SIM slot has no SIM and default data off
                            connectivityManager.setMobileDataEnabled(false);
                        }
                    }
                } else {
                    connectivityManager.setMobileDataEnabled(false);
                }

                gprsDefaultSlot = Settings.System.getInt(contentResolver, Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT) - 1;
                logd("getAllIccIdsDone final data settings: " + gprsDefaultSlot);
                // if (gprsDefaultSlot == Phone.GEMINI_SIM_1 || gprsDefaultSlot == Phone.GEMINI_SIM_2)
                //    setGprsConnType(GeminiNetworkSubUtil.CONN_TYPE_ALWAYS, gprsDefaultSlot);
        }

        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
            logd("mInsertSimState[" + i + "] = " + mInsertSimState[i] );
        }
        
        SystemProperties.set(TelephonyProperties.PROPERTY_SIM_INFO_READY, "true");
        logd("onAllIccidQueryComplete PROPERTY_SIM_INFO_READY after set is " + SystemProperties.get(TelephonyProperties.PROPERTY_SIM_INFO_READY, null) );

        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        logd("broadCast intent ACTION_SIM_INFO_UPDATE");
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    private static void setDefaultNameForAllNewSIM(List<SIMInfo> simInfos, Context context){
        int nSIMCount = (simInfos == null ? 0 : simInfos.size());
        logd("setDefaultNameForAll nSIMCount  is " + nSIMCount);
        for (int i=0; i<nSIMCount; i++) {
            SIMInfo temp = simInfos.get(i);
            if (temp.mDisplayName == null){
                logd("setDefaultNameForAll set default name for slot" + temp.mSlot);
                SIMInfo.setDefaultName(context,temp.mSimId, null);
            }       
        }       
    }

    private static void setDefaultNameIfImsiReadyOrLocked(List<SIMInfo> simInfos, Context context){
        int nSIMCount = (simInfos == null ? 0 : simInfos.size());
        logd("setDefaultNameIfImsiReadyOrLocked nSIMCount  is " + nSIMCount);
        String operatorName = null;
        for (int i=0; i<nSIMCount; i++) {
            SIMInfo temp = simInfos.get(i);
            if (temp.mDisplayName == null){
                logd("setDefaultNameIfImsiReadyOrLocked the " + i + "th mDisplayName is null ");
                operatorName = SystemProperties.get(DEFAULTSIMSETTING_PROPERTY_ICC_OPERATOR_DEFAULT_NAME[temp.mSlot]);
                logd("setDefaultNameIfImsiReadyOrLocked operatorName  is " + operatorName);
                if( operatorName != null && !operatorName.equals("")) {                          
                    SIMInfo.setDefaultName(context,temp.mSimId, operatorName);
                }
            }       
        }       
    }
    
    private static void setColorForNewSIM(List<SIMInfo> simInfos, Context context) {
        int nSIMInsert = (simInfos == null ? 0 : simInfos.size());

        boolean isNeedChangeColor = false;
        int pivotSimColor = -1;
        logd("setColorForNewSIM nSIMInsert = " + nSIMInsert); 
        for(int i=0; i<nSIMInsert; i++) {
            SIMInfo pivotSimInfo = simInfos.get(i);
            if(pivotSimInfo != null) {
                do {
                    isNeedChangeColor = false;
                    logd("setColorForNewSIM i = " + i + " needchange = " + isNeedChangeColor + " slot " + pivotSimInfo.mSlot + " simId " + pivotSimInfo.mSimId + "trace 200"); 
                    
                    // set valid sim color to pivot sim
                    if(!(0 <= pivotSimInfo.mColor 
                        && pivotSimInfo.mColor < PhoneConstants.TOTAL_SIM_COLOR_COUNT)) {
                        pivotSimColor = (int)(pivotSimInfo.mSimId-1) %(PhoneConstants.TOTAL_SIM_COLOR_COUNT);
                    } else {
                        pivotSimColor = pivotSimInfo.mColor;
                    }

                    // make sure the color will be different with others for consistent UI
                    for(int j=0; j<i; j++) {
                        SIMInfo tmpSimInfo = simInfos.get(j);
                        if(tmpSimInfo != null
                            && 0 <= tmpSimInfo.mColor 
                            && tmpSimInfo.mColor < PhoneConstants.TOTAL_SIM_COLOR_COUNT 
                            && pivotSimColor == tmpSimInfo.mColor) {
                            pivotSimColor = (pivotSimColor+1) % PhoneConstants.TOTAL_SIM_COLOR_COUNT;
                            pivotSimInfo.mColor = pivotSimColor;
                            isNeedChangeColor = true;
                            logd("setColorForNewSIM pivot slot " + pivotSimInfo.mSlot + " pivot simId " + pivotSimInfo.mSimId + " pivot color " + pivotSimColor); 
                            break;
                        }
                    }
                } while (isNeedChangeColor == true);
                    
                ContentValues valueColor = new ContentValues(1);
                valueColor.put(SimInfo.COLOR, pivotSimColor);
                context.getContentResolver().update(ContentUris.withAppendedId(SimInfo.CONTENT_URI, pivotSimInfo.mSimId), 
                        valueColor, null, null);   
                logd("setColorForNewSIM slot " + pivotSimInfo.mSlot + " simId " + pivotSimInfo.mSimId + " color " + pivotSimColor + "trace 500"); 
            }
        }
    }

    private static boolean isNewSimInserted(String iccId, String[] oldIccId, int numSIM)
    {
        boolean isNewSim = true;
        for(int i=0; i<numSIM; i++) {
            if(iccId.equals(oldIccId[i])) {
                isNewSim = false;
                break;
            }
        }
        logd("isNewSimInserted : " + isNewSim);
        return isNewSim;
    }

    private static boolean isSIMRemoved(long defSimId,long[] curSim, int numSim){     
        // there is no default sim if defSIMId is less than zero
        if(defSimId <= 0) {
            return false;
        }
        
        boolean isDefaultSimRemoved = true;
        for(int i=0; i<numSim; i++)
        {
            if(defSimId == curSim[i]) {
                isDefaultSimRemoved = false;
                break; 
            }
        }
        return isDefaultSimRemoved;
    }

    private static void onSIMDetected(Context context, String detectStatus, int nSIMCount, int nNewSIMStatus) {
        Intent intent = new Intent(ACTION_ON_SIM_DETECTED);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMCount);
        intent.putExtra(INTENT_KEY_NEW_SIM_STATUS, nNewSIMStatus);
        intent.putExtra(INTENT_KEY_DETECT_STATUS, detectStatus);
        context.sendBroadcast(intent);
    }

    public static void broadCastNewSIMDetected(int nSIMCount, int nNewSIMSlot) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_DETECTED);
        intent.putExtra(INTENT_KEY_DETECT_STATUS, EXTRA_VALUE_NEW_SIM);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMCount);
        intent.putExtra(INTENT_KEY_NEW_SIM_SLOT, nNewSIMSlot);
        logd("broadCast intent ACTION_SIM_DETECTED [" + EXTRA_VALUE_NEW_SIM + ", " +  nSIMCount + ", " + nNewSIMSlot + "]");
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    public static void broadCastDefaultSIMRemoved(int nSIMCount) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_DETECTED);
        intent.putExtra(INTENT_KEY_DETECT_STATUS, EXTRA_VALUE_REMOVE_SIM);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMCount);
        logd("broadCast intent ACTION_SIM_DETECTED [" + EXTRA_VALUE_REMOVE_SIM + ", " +  nSIMCount + "]");
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }
    
    public static void broadCastSIMSwapped(int nSIMCount) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_DETECTED);
        intent.putExtra(INTENT_KEY_DETECT_STATUS, EXTRA_VALUE_SWAP_SIM);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMCount);
        logd("broadCast intent ACTION_SIM_DETECTED [" + EXTRA_VALUE_SWAP_SIM + ", " +  nSIMCount + "]");
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    public static void broadCastSIMInsertedStatus(int nSIMInsertStatus) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_INSERTED_STATUS);
        intent.putExtra(INTENT_KEY_SIM_COUNT, nSIMInsertStatus);
        logd("broadCast intent ACTION_SIM_INSERTED_STATUS " +  nSIMInsertStatus);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE, UserHandle.USER_ALL);
    }

    static int get3GSimId() {
        if(FeatureOption.MTK_GEMINI_3G_SWITCH){
            int simId = SystemProperties.getInt("gsm.3gswitch", 0); 
            if((simId > 0)&& (simId <= PhoneConstants.GEMINI_SIM_NUM)){
                return (simId -1); //Property value shall be 1~4,  convert to PhoneConstants.GEMINI_SIM_x
            }else{
                Log.w(LOG_TAG, "get3GSimId() got invalid property value:"+ simId);
            }
        } 
        return PhoneConstants.GEMINI_SIM_1;
    }

    private static void logd(String message) {
        Log.d(LOG_TAG, "[DefaultSIMSettings] " + message);
    }
}

