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

package com.mediatek.StkSelection;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.android.internal.telephony.PhoneConstants;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class StkSelection extends Activity {
    /** Called when the activity is first created. */

    public static final String LOGTAG = "StkSelection ";
    public static boolean bSIM1Inserted = false;
    public static boolean bSIM2Inserted = false;
    public static boolean bSIM3Inserted = false;
    public static boolean bSIM4Inserted = false;
    public static String strTargetLoc = null;
    public static String strTargetClass = null;
 
    private static final int REQUEST_TYPE = 302;

    public static int mSlot = -1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "[onCreate]+");
        mCellMgr.register(this);
        //setContentView(R.layout.main);
        Log.d(LOGTAG, "[onCreate]-");
    }
    
    
//        setContentView(R.layout.main);

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(LOGTAG, "[onDestroy]+");
        super.onDestroy();
        mCellMgr.unregister();
        Log.d(LOGTAG, "[onDestroy]-");
    }

    private void launchStk(int sim_id, boolean multi)
    {
        String pName = "com.android.stk";
        String aName = null;

        if (multi) {
            pName = "com.android.phone";
            aName = "com.mediatek.phone.StkListEntrance";
        }
        else {
            switch (sim_id)
            {
                case PhoneConstants.GEMINI_SIM_1:
                    aName = "com.android.stk.StkLauncherActivity";
                    break;
                case PhoneConstants.GEMINI_SIM_2:
                    aName = "com.android.stk.StkLauncherActivityII";
                    break;
                case PhoneConstants.GEMINI_SIM_3:
                    aName = "com.android.stk.StkLauncherActivityIII";
                    break;
                case PhoneConstants.GEMINI_SIM_4:
                    aName = "com.android.stk.StkLauncherActivityIV";
                    break;
            }

            if (aName != null)
            {
                ComponentName cName = new ComponentName(pName, aName);
                int mode = 0;
                try {
                    mode = Settings.System.getInt(getApplicationContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON);
                    Log.d(LOGTAG, "mode: " + mode);
                } catch(SettingNotFoundException e) {
                    Log.d(LOGTAG, "fail to get property from Settings");
                }
                PackageManager pm = getApplicationContext().getPackageManager();
                if(mode != 0) { //sim1 card not ready
                    showTextToast(getString(R.string.airplane_mode_on));
                    return;
                } else if(pm.getComponentEnabledSetting(cName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED){//sim1 card ready but disable activity
                    showTextToast(getString(R.string.lable_sim_not_ready));
                    return;
                }
            }
        }

        Log.d(LOGTAG, "launchStk, sim_id: " + sim_id + ", multi: " + multi);

        if(pName != null && aName != null){
            Intent intent = new Intent();
            intent.setClassName(pName, aName);
            startActivity(intent);
        }
    }
    
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.d(LOGTAG, "[onResume]+");
        boolean multi = false;
        int sim_id = PhoneConstants.GEMINI_SIM_1;
        
        if(true == FeatureOption.MTK_GEMINI_4SIM_SUPPORT) {
            bSIM1Inserted = hasIccCard(0);
            bSIM2Inserted = hasIccCard(1);
            bSIM3Inserted = hasIccCard(2);
            bSIM4Inserted = hasIccCard(3);
            
            if((bSIM1Inserted == false) && (bSIM2Inserted == false) && (bSIM3Inserted == false) && (bSIM4Inserted == false)){//No SIM card inserted
                //Notify user no cards insert
                showTextToast(getString(R.string.no_sim_card_inserted));
                finish();
            } else if ((bSIM1Inserted == true) && (bSIM2Inserted == false) && (bSIM3Inserted == false) && (bSIM4Inserted == false)){//SIM1 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_1;
                launchStk(sim_id, multi);
            } else if ((bSIM1Inserted == false) && (bSIM2Inserted == true) && (bSIM3Inserted == false) && (bSIM4Inserted == false)){//SIM2 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_2;
                launchStk(sim_id, multi);   
            } else if ((bSIM1Inserted == false) && (bSIM2Inserted == false) && (bSIM3Inserted == true) && (bSIM4Inserted == false)){//SIM3 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_3;
                launchStk(sim_id, multi);    
            } else if ((bSIM1Inserted == false) && (bSIM2Inserted == false) && (bSIM3Inserted == false) && (bSIM4Inserted == true)){//SIM4 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_4;
                launchStk(sim_id, multi);
            } else { //more than one sim card inserted
                multi = true;
                launchStk(sim_id, multi);
            }       
        } else if (true == FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
            bSIM1Inserted = hasIccCard(0);
            bSIM2Inserted = hasIccCard(1);
            bSIM3Inserted = hasIccCard(2);
            
            if((bSIM1Inserted == false) && (bSIM2Inserted == false) && (bSIM3Inserted == false)){//No SIM card inserted
                //Notify user no cards insert
                showTextToast(getString(R.string.no_sim_card_inserted));
                finish();
            } else if ((bSIM1Inserted == true) && (bSIM2Inserted == false) && (bSIM3Inserted == false)){//SIM1 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_1;
                launchStk(sim_id, multi); 
            } else if ((bSIM1Inserted == false) && (bSIM2Inserted == true) && (bSIM3Inserted == false)){//SIM2 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_2;
                launchStk(sim_id, multi);    
            } else if ((bSIM1Inserted == false) && (bSIM2Inserted == false) && (bSIM3Inserted == true)){//SIM3 card inserted
                sim_id = PhoneConstants.GEMINI_SIM_3;
                launchStk(sim_id, multi);   
            } else { //more than one sim card inserted
                multi = true;
                launchStk(sim_id, multi);
            }
        } else if (false == FeatureOption.MTK_GEMINI_SUPPORT){//single card
            bSIM1Inserted = hasIccCard(0);
            if (bSIM1Inserted == false) {
                showTextToast(getString(R.string.no_sim_card_inserted));
                finish();
            }
            else {
                sim_id = PhoneConstants.GEMINI_SIM_1;
                launchStk(sim_id, multi);
            }
        }else{//gemini card
            bSIM1Inserted = hasIccCard(0);
            bSIM2Inserted = hasIccCard(1);
            if((bSIM1Inserted == false) && (bSIM2Inserted == false) ){//No SIM card inserted
                //Notify user no cards insert
                showTextToast(getString(R.string.no_sim_card_inserted));
                finish();
                return;
            }else if ((bSIM1Inserted == true) && (bSIM2Inserted == false)){//Only sim1 inserted
                sim_id = PhoneConstants.GEMINI_SIM_1;
                launchStk(sim_id, multi);
            }else if((bSIM1Inserted == false) && (bSIM2Inserted == true)){//Only sim2 inserted
                sim_id = PhoneConstants.GEMINI_SIM_2;
                launchStk(sim_id, multi);
            }else{//Both SIM inserted
                multi = true;
                launchStk(sim_id, multi);
            }
        }

        finish();
        Log.d(LOGTAG, "[onResume]-");
    }


    //deal with SIM status
    private Runnable serviceComplete = new Runnable() {
        public void run() {
            Log.d(LOGTAG, "serviceComplete run");
            int nRet = mCellMgr.getResult();
            Log.d(LOGTAG, "serviceComplete result = " + CellConnMgr.resultToString(nRet));
            if (mCellMgr.RESULT_ABORT == nRet) {
                finish();
                return;
            } else {
                finish();
                return;
            }
        }
    };

    private CellConnMgr mCellMgr = new CellConnMgr(serviceComplete);

    public static boolean hasIccCard(int slot) {

        boolean bRet = false;

        if (true == FeatureOption.MTK_GEMINI_SUPPORT) {
            try {
                final ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                if (null != iTelephony) {
                    bRet = iTelephony.isSimInsert(slot);
                }
            } catch (RemoteException ex) {
                Log.d(LOGTAG, "isSimInsert: fail");
                ex.printStackTrace();
            }
        } else {
            try {
                final ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                if (null != iTelephony) {
                    bRet = iTelephony.isSimInsert(0);
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }

        Log.d(LOGTAG, "isSimInsert: " + bRet);
        return bRet;
    }

   

    public static boolean IccCardReady(int slot) {

        boolean bRet = false;

        if (true == FeatureOption.MTK_GEMINI_SUPPORT) {
            bRet = (TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault().getSimStateGemini(slot));
        } else {
            bRet = (TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault().getSimState());
        }

        return bRet;
    }

    private void showTextToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg,
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
}
