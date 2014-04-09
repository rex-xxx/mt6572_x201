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

package com.mediatek.cmmb.app;

import android.app.AlertDialog;
import android.app.Dialog;
//import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
//import android.os.SystemProperties;
import android.provider.Telephony;
//import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
//temp utility functions.
import android.telephony.TelephonyManager;
import android.util.Log;
//import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ITelephony;
import com.mediatek.common.featureoption.FeatureOption;
//import com.mediatek.mbbms.protocol.TelephonyUtils;
import com.mediatek.mbbms.service.CMMBServiceClient;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.List;

public class ModeSwitchManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "CMMB::ModeSwitchManager";

    private static final String CURR_MODE = "CURR_MODE";
    private static final String ISCMMBPREFERRED = "isCmmbPreferred";
    private static final String CUR_SIM = "cursim";
    private static final String CLOSE_DATA_CON = "datacon";
    
    private static final int MBBMS_MODE = CMMBServiceClient.CMMB_STATE_CONNECTED;    
    
    //activity specific data is kept by ModeSwitcher.
    private Context mContext;      
    private boolean mIsActivityStopped;
    private AlertDialog mDialog;    
    private ModeChangeHandler mOnModeChangeHandler;
    
    private static int sInForgroundNum;//it is a flag to help us to identify whether we are in forground now.
    private static ModeMonitor sModeMonitor;    
    private static class ModeMonitor {
        //global data is kept by ModeMonitor which is single instance.
        private SimItem curSim;
        private int mode;//the mode which the whole application is running in now. 
        private boolean isCmmbPreferred;        
        private int realMode;
        private List<SimItem> simList;    // CMCC sims
        private List<SimStateListerner> simListernerList;    // CMCC sims
        private List<SimItem> connectedSimList;    
        
                
        private Context appContext;
        private OnModeChangeListener onModeChangeListener;    
        //ensure showDialog is executed in main thread.
        private Handler mainHandler = new Handler(Looper.getMainLooper());

        class SimStateListerner extends PhoneStateListener {
            private SimItem mSim;
            private TelephonyManagerEx mTmgr;
            boolean mRegistered;
            public SimStateListerner(SimItem sim) {
                mSim = sim;
            }

            private Runnable signalValidator = new Runnable() {
                public void run() {
                    Log.d(TAG, "sim " + mSim.mSlot + "signal is recover");
                    
                    if (!connectedSimList.contains(mSim)) {
                        addConnectedSim(mSim);
                    }
                  
                    if (realMode == MBBMS_MODE) {
                        Log.w(TAG, "signalValidator: realMode is MBBMS_MODE already,ignore");
                        return;
                    }              
                    
                    realMode = MBBMS_MODE;
                    callOnModeChangeListener(realMode);
                }
              };
              
              class StateHdlr implements Runnable {
                    public ServiceState state;

                  public void run() {
                      Log.d(TAG, "stateChangeHdlr() sim " + mSim.mSlot + " state:" + state + " mRegistered:" + mRegistered);
                      if (!mRegistered) {
                          return;
                      }
                      switch (state.getState()) {
                      case ServiceState.STATE_IN_SERVICE:
                          if (realMode != MBBMS_MODE && isCMCC(state)) {
                              Log.d(TAG, "onServiceStateChanged() sim " + mSim.mSlot + "post Validator");
                              mainHandler.postDelayed(signalValidator, 30000);                        
                          } 
                          break;
                      default:
                          mainHandler.removeCallbacks(signalValidator);
                          if (connectedSimList.contains(mSim)) { 
                          connectedSimList.remove(mSim);
                          if (mSim == curSim) {
                              Log.d(TAG, "onServiceStateChanged() sim " + mSim.mSlot + "disconnect");
                              realMode = CMMBServiceClient.CMMB_STATE_DISCONNECTED;
                          }
                          
                          }
                          break;                
                      }
                  }                  
            }
              
            private StateHdlr stateChangeHdlr = new StateHdlr();
            
            public void onServiceStateChanged(ServiceState state) {
                Log.d(TAG, "onServiceStateChanged() sim " + mSim.mSlot + "state:" + state);
                if (mainHandler != null) {
                    stateChangeHdlr.state = state;
                    mainHandler.post(stateChangeHdlr);
                }
            }
            
            public void register() {
                if (mTmgr == null) {
                    mTmgr = new TelephonyManagerEx(appContext);
                    if (mTmgr == null) {
                        Log.e(TAG, "Create TelephonyManagerEx error!!!");
                        return;
                    }
                }
                mRegistered = true;
                mTmgr.listen(this, PhoneStateListener.LISTEN_SERVICE_STATE, mSim.mSlot);
            }
            
            public void unRegister() {
                if (mTmgr != null) {
                    mTmgr.listen(this, PhoneStateListener.LISTEN_NONE, mSim.mSlot);   
                }
                mainHandler.removeCallbacks(signalValidator);
                mainHandler.removeCallbacks(stateChangeHdlr);
                mRegistered = false;
            }

        }

        
        ModeMonitor(Context c,Bundle state) {
            Log.d(TAG, "ModeMonitor() state = " + state);
            
            appContext = c.getApplicationContext();//we don't use the activity's context directly to avoid memory leak;
            /*String sim_string = SettingActivity.getSelectedSimKey(appContext);
            Log.d(TAG, "sim_string = "+sim_string);
            
            if (sim_string != null) {
                if (sim_string.equals("SIM1")) {
                    simId = Phone.GEMINI_SIM_1;        
                } else if (sim_string.equals("SIM2")) {
                    simId = Phone.GEMINI_SIM_2;        
                }
            }*/
            
            initRealMode();

            if (state == null) {
                updateMode();
                if (connectedSimList.size() == 1) {
                    checkDataConnection(true);
                }
            } else {
                mode = state.getInt(CURR_MODE, realMode);
                isCmmbPreferred = state.getBoolean(ISCMMBPREFERRED, false);
                if (mode == MBBMS_MODE) {
                    int curSimId = state.getInt(CUR_SIM);
                    for (SimItem item : simList) {
                        if (item.mSlot == curSimId) {
                            curSim = item;
                        }
                    }

                    /* check data connection */
                    checkDataConnection(false);
                    mNeedCloseDataConnection = state.getBoolean(CLOSE_DATA_CON);
                }
            }

            Log.d(TAG, "ModeMonitor() mode = " + mode + " realMode = "
                    + realMode + " isCmmbPreferred = " + isCmmbPreferred);

        }

        private static final String CMCC_NUM0 = "46000";
        private static final String CMCC_NUM1 = "46002";
        private static final String CMCC_NUM2 = "46007";
        private boolean isSimAvailable(SimInfoRecord simInfoRecord) {
            return true;
            /* we cannot get operator info in airplane mode
            String opNum = getServiceState(simInfo.mSlot).getOperatorNumeric();
            if(opNum != null){
                return opNum.equals(CMCC_NUM);
            }
            
            return false;
            */
        }
        
        private boolean isCMCC(ServiceState state) {
            if (state == null) {
                return false;
            }
            
            String opNum = state.getOperatorNumeric();
            
            if (opNum == null) {
                return false;
            }
            
            //Just a temp solution for lab test
            return true;
            //return opNum.equals(CMCC_NUM0) || opNum.equals(CMCC_NUM1) || opNum.equals(CMCC_NUM2);
        }
        
        /*insert sim orderring by mSlot */
        private void addConnectedSim(SimItem sim) {
            if (!connectedSimList.isEmpty()) {
                int i = 0;
                for (SimItem item: connectedSimList) {
                    if (item.mSlot > sim.mSlot) {
                        connectedSimList.add(i, sim);
                        return;
                    }
                    i++;
                }
            }
            connectedSimList.add(sim);
        }
        
        private void initConnectedSimList() {
            connectedSimList = new ArrayList<SimItem>();
            if (simList != null) {
                for (SimItem sim: simList) {
                    ServiceState state = getServiceState(sim.mSlot);
                    if (state.getState() == ServiceState.STATE_IN_SERVICE && isCMCC(state)) {
                        addConnectedSim(sim);
                    }
                }
            }
            Log.d(TAG, "initConnectedSimList(): " + connectedSimList);
        }
         
        private void initSimList() {
            Log.d(TAG, "initSimList()");
            List<SimInfoRecord> simInfoRecordList = SimInfoManager.getInsertedSimInfoList(appContext);
            simList = new ArrayList<SimItem>();
            simListernerList = new ArrayList<SimStateListerner>();
            
            if (simInfoRecordList != null) {
                for (SimInfoRecord simInfoRecord : simInfoRecordList) {
                    if (isSimAvailable(simInfoRecord)) {
                        SimItem sim = new SimItem(simInfoRecord);
                        simList.add(sim);
                        simListernerList.add(new SimStateListerner(sim));
                    }
                }
            }            
            Log.d(TAG, "initSimList(): " + simList);
        }
        
        private void initRealMode() {
            initSimList();
            initConnectedSimList();
            if (simList.isEmpty()) {
                realMode = CMMBServiceClient.CMMB_STATE_NO_SIM;
            } else {
                if (connectedSimList.isEmpty()) {
                    realMode = CMMBServiceClient.CMMB_STATE_DISCONNECTED;
                    curSim = null;
                } else if (connectedSimList.size() == 1) {
                    realMode = MBBMS_MODE;
                    curSim = connectedSimList.get(0);
                } else {   
                    //TODO: is that right???
                    realMode = MBBMS_MODE;
                    curSim = connectedSimList.get(0);
                }
            }
            
            //listen sim state
            for (SimStateListerner l : simListernerList) {
                l.register();
            }
        }        

        private void deInit() {
            Log.d(TAG, "deInit() mode = " + mode);

            for (SimStateListerner l : simListernerList) {
                l.unRegister();
            }

            closeDataConnection();

            simList = null;
            curSim = null;
            connectedSimList = null;
            simListernerList = null;
        }

        private void updateMode() {
            Log.d(TAG, "updateMode()");
            mode = realMode;
        }   

        private void setCmmbIsPreferred() {
            Log.d(TAG, "setCmmbIsPreferred()");
            isCmmbPreferred = true;
        }    


        private ServiceState getServiceState(int simid) {
            Log.d(TAG, "getServiceState(): simid=" + simid);
        //  TelephonyManagerEx tmex = new TelephonyManagerEx(appContext);
            Bundle bd = null;
            try {
            ITelephony tmex = ITelephony.Stub.asInterface(android.os.ServiceManager.getService(Context.TELEPHONY_SERVICE));
            
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                Log.d(TAG, "getServiceState(): MTK_GEMINI_SUPPORT");
                bd = tmex.getServiceStateGemini(simid);
            } else {
                bd = tmex.getServiceState();
            }          
            } catch (RemoteException e) {
            	Log.d(TAG, "getServiceState error e:" + e.getMessage());
            }
            ServiceState state = ServiceState.newFromBundle(bd);
            
            Log.d(TAG, "getServiceState() = " + state);
            return state;
        }    
        
        private void setOnModeChangeListener(OnModeChangeListener l) {            
            onModeChangeListener = l;
        }

        private void callOnModeChangeListener(int mode) {
            if (onModeChangeListener != null) {
                onModeChangeListener.onModeChanged(mode);
            }
        }                    
        
        private void checkMode() {
            if (realMode != mode) {
                callOnModeChangeListener(realMode);                
            }               
        }        
        
        private boolean isFromMbbmsToCmmb() {
            return mode == MBBMS_MODE && realMode != MBBMS_MODE;
        }
        
        private boolean isFromCmmbToMbbms() {
            return mode != MBBMS_MODE && realMode == MBBMS_MODE;
        }       

        private boolean mNeedCloseDataConnection = false;
        private void checkDataConnection(boolean showMsg) {
            Log.d(TAG, "checkDataConnection()");
            ConnectivityManager cm = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean dataEnable = cm.getMobileDataEnabled();
            Log.i(TAG, "checkDataConnection() dataEnable=" + dataEnable + ", mSlot =" + curSim.mSlot);
            if (!dataEnable) {
                //open Data connection 
                mNeedCloseDataConnection = true;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
                    SimInfoRecord simInfoRecord = SimInfoManager.getSimInfoBySlot(appContext, curSim.mSlot);
                    intent.putExtra(PhoneConstants.MULTI_SIM_ID_KEY, simInfoRecord.mSimInfoId); 
                    appContext.sendBroadcast(intent);
                } else {
                      cm.setMobileDataEnabled(true);
                }
                /* Notify AP to show msg*/
                if (showMsg) {
                    Toast.makeText(appContext, R.string.datacon_hint, 1000).show();
                }
                
            }
        }
        
        private void closeDataConnection() {
            Log.d(TAG, "closeDataConnection() mNeedCloseDataConnection=" + mNeedCloseDataConnection);
            if (mNeedCloseDataConnection) {
                ConnectivityManager cm = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                cm.setMobileDataEnabled(false);
                
            }
        }

    }    

    private OnModeChangeListener mOnModeChangeListener = new OnModeChangeListener() {
        public void onModeChanged(int mode) {
            Log.d(TAG, "onModeChanged() mIsActivityStopped = " + mIsActivityStopped + " mode = " + sModeMonitor.mode
                + " realMode = " + sModeMonitor.realMode + " isCmmbPreferred = " + sModeMonitor.isCmmbPreferred);
            //now we do not do updateMode if activity is stopped.
            //if we allow to do it,then we need to care about the case that mode is changed after onSaveInstanceState 
            //is executed which is ensured to be executed before onStop.
            if (!mIsActivityStopped) {
                if (sModeMonitor.isFromMbbmsToCmmb()) {
                     Log.d(TAG, "onModeChanged() ignore");
                     /*sModeMonitor.updateMode();
                    if (!mOnModeChangeHandler.onEnterCmmbMode()) {                
                        goBackToMain(false);      
                    }
                    */
                } else if (sModeMonitor.isFromCmmbToMbbms() && !sModeMonitor.isCmmbPreferred) {
                    mOnModeChangeHandler.onDialogPrepared(); 
                    showSignalRecoverDialog(true);
                } else {
                    showSignalRecoverDialog(false);
                }
            }         
        }
    };    

    public ModeSwitchManager(Context c,ModeChangeHandler mainHandler,Bundle state) {
        mContext = c;    
        
        if (sModeMonitor == null) {
            sModeMonitor = new ModeMonitor(c,state);            
        }
        
        if (mainHandler == null) {
            mOnModeChangeHandler = new ModeChangeHandler();
        } else {
            mOnModeChangeHandler = mainHandler;
        }
    }    

    public void onSaveInstanceState(Bundle state) {
        state.putInt(CURR_MODE,sModeMonitor.mode);        
        state.putBoolean(ISCMMBPREFERRED,sModeMonitor.isCmmbPreferred);
        state.putInt(CUR_SIM, getCurSimID());
        state.putBoolean(CLOSE_DATA_CON, sModeMonitor.mNeedCloseDataConnection);
    }



    public static boolean isInForground() {
        return sInForgroundNum > 0;
    }
    
    public void onActivityStart() {
        mIsActivityStopped = false;
        sInForgroundNum++;
        sModeMonitor.setOnModeChangeListener(mOnModeChangeListener);
        if (sModeMonitor != null) {
            sModeMonitor.checkMode();
        }
    }
    
    public void onActivityStop() {
        mIsActivityStopped = true;
        sInForgroundNum--;
    }    
    
    //must be called when exiting the application.
    //TBD:we should make it to be called safely in any context just like ChannelListManager does.
    public void release() {    
        Log.d(TAG, "release");
        if (sInForgroundNum > 0) {
            Log.d(TAG, "do not release because there are still activities running in forground");
            return;
        }
        
        if (sModeMonitor != null) {
            sModeMonitor.deInit();
            sModeMonitor.setOnModeChangeListener(null);
            sModeMonitor = null;
        }
    }

    
    private void showSignalRecoverDialog(boolean toShow) {   
        Log.d(TAG, "showSignalRecoverDialog() toShow = " + toShow);
        
        if (toShow && mDialog == null) {      
            mDialog = new AlertDialog.Builder(mContext)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.mobile_tv)                
                .setMessage(R.string.gsm_signal_recover)  
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Log.d(TAG, "showSignalRecoverDialog() whichButton = " + whichButton);
                            selectSim();
                        }
                    })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "showSignalRecoverDialog() whichButton = " + whichButton);
                        sModeMonitor.setCmmbIsPreferred(); 
                    }
                    })                
                .create();

            mDialog.setOnCancelListener(new Dialog.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                        sModeMonitor.setCmmbIsPreferred();
                    }
            }); 
            
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(TAG, "showSignalRecoverDialog() onDismiss");
                    
                    mDialog = null;                    
                }
            });
            
            mDialog.show();
        } else if (!toShow && mDialog != null) {
            mDialog.dismiss();
        }
    }        

    private void goBackToMain(boolean simChanged) {
        
        Log.d(TAG, "goBackToMain()");
        Intent intent = new Intent();
        intent.setClass(mContext,MainScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle bundle = new Bundle();        
        bundle.putBoolean("simChanged", simChanged);    
        bundle.putBoolean("modeChanged", true);        
        intent.putExtras(bundle);
        mContext.startActivity(intent);
    }

    //temp utility functions.
    private int getSimType(int id) {
        Log.d(TAG, "getSimType() simId = " + id); 
    
        if (id != -1) {
           // TelephonyManager telephonyManager = 
             //  (TelephonyManager)sModeMonitor.appContext.getSystemService(Context.TELEPHONY_SERVICE);
              try {
              ITelephony telephonyManager = ITelephony.Stub.asInterface(android.os.ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (telephonyManager != null) {
                String simName;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    simName = telephonyManager.getIccCardTypeGemini(id);
                } else {
                    simName = telephonyManager.getIccCardType();
                }

                Log.d(TAG, "getSimType() simName = " + simName); 
                
                if (simName != null) {
                    if (simName.equalsIgnoreCase("USIM")) {
                        return CMMBServiceClient.SIMTYPE_3G;
                    }
                    if (simName.equalsIgnoreCase("SIM")) {
                        return CMMBServiceClient.SIMTYPE_2G;
                    }
                }
            }
          }catch (RemoteException e) {
                Log.d(TAG, "getSimType() error happened e:" + e.getMessage()); 
            }            
        }
        return CMMBServiceClient.SIMTYPE_NOSIM;
    }     
    
    public int getMode() {
        Log.d(TAG, "getMode() mode = " + sModeMonitor.mode);
        return sModeMonitor.mode;
    }           

    /*CMMBServiceClient.SIMTYPE_3G, CMMBServiceClient.SIMTYPE_2G*/
    public int getSimType() {
        if (isMbbmsMode()) {
            return getSimType(sModeMonitor.curSim.mSlot);
        } else {
            Log.w(TAG, "getSimType() in CMMB mode");
            return CMMBServiceClient.SIMTYPE_2G;
        }
    } 

    public int getCurSimID() {
        if (isMbbmsMode()) {
            return sModeMonitor.curSim.mSlot;
        } else {
            Log.w(TAG, "getCurSimID() in CMMB mode");
            return 0;
        }
    }

    public boolean isMbbmsMode() {
        return sModeMonitor.mode == MBBMS_MODE;
    }
    
    public boolean isMobileSignalAvailable() {
        return sModeMonitor.realMode == MBBMS_MODE;
    }
    
    public void setCurSim(SimItem sim) {
        sModeMonitor.curSim = sim;
    }


    private interface OnModeChangeListener {
        void onModeChanged(int mode);       
    }    

    
    public static class ModeChangeHandler {
        /*Mode manager is going to change current mode from mbbms to cmmb
         *if return false here,will go to MainScreen activity immediately
         */
        public boolean onEnterCmmbMode() {
            return false;
        }    
        
        /*Mobile network signal is recovered,a dialog will be showed up and ask user 
         *whether to goto mbbms mode or stay in cmmb mode.
         */
        public void onDialogPrepared() {}  
        
        /*indicate which mode is selected by user.
         *if return false here,will go to MainScreen activity immediately 
         */
        public boolean onModeSelected(boolean isMbbmsSelected) {
            return false;
        }          
    }
    
    public static class SimItem {
        public static final int DISPLAY_NONE = 0;
        public static final int DISPLAY_FIRST_FOUR = 1;
        public static final int DISPLAY_LAST_FOUR = 2;    

        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public long mSimID = -1;
        public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;


        //constructor for sim
        public SimItem(SimInfoRecord simInfoRecord) {
            mIsSim = true;
            mName = simInfoRecord.mDisplayName;
            mNumber = simInfoRecord.mNumber;
            mDispalyNumberFormat = simInfoRecord.mDispalyNumberFormat;
            mColor = simInfoRecord.mColor;
            mSlot = simInfoRecord.mSimSlotId;
            mSimID = simInfoRecord.mSimInfoId;
        }
        
        
        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return new String(
                    "mIsSim:" + mIsSim
                    + ", mName:" + mName
                    + ", mNumber:" + mNumber
                    + ", mDispalyNumberFormat:" + mDispalyNumberFormat
                    + ", mColor:" + mColor
                    + ", mSlot:" + mSlot
                    + ", mSimID:" + mSimID
                    );
        }


        static int getStatusResource(int state) {
            switch (state) {
            case PhoneConstants.SIM_INDICATOR_RADIOOFF:
                return com.mediatek.internal.R.drawable.sim_radio_off;
            case PhoneConstants.SIM_INDICATOR_LOCKED:
                return com.mediatek.internal.R.drawable.sim_locked;
            case PhoneConstants.SIM_INDICATOR_INVALID:
                return com.mediatek.internal.R.drawable.sim_invalid;
            case PhoneConstants.SIM_INDICATOR_SEARCHING:
                return com.mediatek.internal.R.drawable.sim_searching;
            case PhoneConstants.SIM_INDICATOR_ROAMING:
                return com.mediatek.internal.R.drawable.sim_roaming;
            case PhoneConstants.SIM_INDICATOR_CONNECTED:
                return com.mediatek.internal.R.drawable.sim_connected;
            case PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
                return com.mediatek.internal.R.drawable.sim_roaming_connected;
            default:
                return -1;
            }
        }
        
        static int getSimColorResource(int color) {
            
            if ((color >= 0) && (color <= 7)) {
                return Telephony.SIMBackgroundRes[color];
            } else {
                return -1;
            }

            
        }
    }

    public List<SimItem> getAvailableSims() {
        return sModeMonitor.connectedSimList;
    }

    private void selectSim() {
        List<ModeSwitchManager.SimItem> simList = getAvailableSims();

        if (simList == null || simList.isEmpty()) {
            Log.e(TAG, "should not reach here");
        } else if (simList.size() == 1) {
            sModeMonitor.updateMode();
            setCurSim(simList.get(0));
            sModeMonitor.checkDataConnection(true);
            if (!mOnModeChangeHandler.onModeSelected(true)) {                                
                goBackToMain(false);          
            } 
        } else {
            showSelectSimDialog();
        }

    }

    private AlertDialog mSelectSimDlg;
    private SelectSimListerner mSelSimListerner;
    private void showSelectSimDialog() {
        Log.d(TAG, "showSelectSimDialog()");
        
        if (mSelectSimDlg == null) {
            createSelectSimDialog(new SelectSimListerner() {
                public void onSelect() {
                    sModeMonitor.updateMode();
                    if (!mOnModeChangeHandler.onModeSelected(true)) {                                
                        goBackToMain(false);          
                    } 
                }
                
                public void onCancel() {
                    Log.d(TAG, "showSignalRecoverDialog() cancel");
                    sModeMonitor.setCmmbIsPreferred();
                }
                
                public void onDismiss() {
                    
                }
            }).show();
        }

    }
    
    public interface SelectSimListerner {
        void onSelect();
        void onCancel();
        void onDismiss();
    }
    
    public AlertDialog createSelectSimDialog(SelectSimListerner listerner) {
        Log.d(TAG, "createSelectSimDialog()");
        
        mSelSimListerner = listerner;
        List<ModeSwitchManager.SimItem> simList = getAvailableSims();
        SimListAdapter simListAdapter = new SimListAdapter(mContext, simList);      
        ListView simListView = new ListView(mContext);
        simListView.setAdapter(simListAdapter);
        simListAdapter.selectItem(false); //for OK
        simListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
               public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    Log.i(TAG,"positon is " + position);
                    
                    if (v != null) {
                        ((SimListAdapter)parent.getAdapter()).selectItem(position, true);
                        mSelectSimDlg.dismiss(); 
                        if (mSelSimListerner != null) {
                            mSelSimListerner.onSelect();
                        }
                    }
                }
        });
        simListView.setItemsCanFocus(false);

        mSelectSimDlg = new AlertDialog.Builder(mContext)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.mobile_tv)                             
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (mSelSimListerner != null) {
                    sModeMonitor.checkDataConnection(true);
                   // ((SimListAdapter)(mSelectSimDlg.getListView()).getAdapter())).selectItem(true);
                    mSelSimListerner.onSelect();
                } 
            }
        })
        .setView(simListView, 0, 0, 0, 0)
        .create();

        mSelectSimDlg.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "showSignalRecoverDialog() cancel");
                if (mSelSimListerner != null) {
                    mSelSimListerner.onCancel();
                }
            }
        });
        
        mSelectSimDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (mSelSimListerner != null) {
                            mSelSimListerner.onDismiss();
                        }
                        mSelSimListerner = null;
                        mSelectSimDlg = null;
                    }
        });
        
        return mSelectSimDlg;
    }
    /**
     * Operator type
     * @author mtk80357
     *
     */
/*
    public static enum OpIndex {
        OP_NONE,
        OP_CMCC,
        OP_CU,
        OP_ORANGE
    };
*/
    class SimListAdapter extends BaseAdapter {
        
        List<ModeSwitchManager.SimItem> mSimItemList;
        private LayoutInflater mInflater;
        private boolean mNeed3GText;
        private int mSelected;
        
        public SimListAdapter(Context c, List<ModeSwitchManager.SimItem> simItemList) {
            mInflater = LayoutInflater.from(c);
            mSimItemList = simItemList;
            mNeed3GText = false;
        }
        
        public int getCount() {
            return mSimItemList.size();
        }

        public Object getItem(int position) {
            return mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.sim_item, null);
                holder = new ViewHolder();
                setViewHolderId(holder,convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            ModeSwitchManager.SimItem simItem = (ModeSwitchManager.SimItem)getItem(position);
            setNameAndNum(holder.mTextName,holder.mTextNum, simItem);
            setText3G(holder.mText3G, simItem);
            setImageSim(holder.mImageSim, simItem);
            setImageStatus(holder.mImageStatus, simItem);
            setTextNumFormat(holder.mTextNumFormat, simItem);
            holder.mCkRadioOn.setChecked(mSelected == position);
            if ((simItem.mState == PhoneConstants.SIM_INDICATOR_RADIOOFF)) {
                    convertView.setEnabled(false);
                    holder.mTextName.setEnabled(false);
                    holder.mTextNum.setEnabled(false);
                    holder.mCkRadioOn.setEnabled(false);
            } else {
                    convertView.setEnabled(true);
                    holder.mTextName.setEnabled(true);
                    holder.mTextNum.setEnabled(true);
                    holder.mCkRadioOn.setEnabled(true);
            }
                    
            return convertView;
          }
        private void setTextNumFormat(TextView textNumFormat, ModeSwitchManager.SimItem simItem) {
            if (simItem.mIsSim) {
                if (simItem.mNumber != null) {
                    switch (simItem.mDispalyNumberFormat) {
                    case ModeSwitchManager.SimItem.DISPLAY_NONE: 
                        textNumFormat.setVisibility(View.GONE);
                        break;
                    case ModeSwitchManager.SimItem.DISPLAY_FIRST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mNumber.substring(0, 4));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    case ModeSwitchManager.SimItem.DISPLAY_LAST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mNumber.substring(simItem.mNumber.length() - 4));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    default:
                        break;
                    }           
                }
            }
            
        }
        private void setImageStatus(ImageView imageStatus, ModeSwitchManager.SimItem simItem) {
            int res = ModeSwitchManager.SimItem.getStatusResource(simItem.mState);
            if (res == -1) {
                imageStatus.setVisibility(View.GONE);
            } else {
                imageStatus.setVisibility(View.VISIBLE);
                imageStatus.setImageResource(res);
            }
        }
        private void setImageSim(RelativeLayout imageSim, ModeSwitchManager.SimItem simItem) {
            if (simItem.mIsSim) {
                int resColor = ModeSwitchManager.SimItem.getSimColorResource(simItem.mColor);
                if (resColor >= 0) {
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackgroundResource(resColor);
                }
            } else if (simItem.mColor == 8) {
                imageSim.setVisibility(View.VISIBLE);
                imageSim.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
            } else {
                imageSim.setVisibility(View.GONE);
            }
        }

        private void setText3G(TextView text3G, ModeSwitchManager.SimItem simItem) {
            if ((!mNeed3GText) || (simItem.mSlot != PhoneConstants.GEMINI_SIM_1) || simItem.mColor == 8) {
                text3G.setVisibility(View.GONE);
            } else {
                text3G.setVisibility(View.VISIBLE);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView)convertView.findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView)convertView.findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView)convertView.findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView)convertView.findViewById(R.id.simNumFormatSel);
            holder.mText3G = (TextView)convertView.findViewById(R.id.sim3gSel);
            holder.mCkRadioOn = (RadioButton)convertView.findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout)convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName,TextView textNum, ModeSwitchManager.SimItem simItem) {
            if (simItem.mName != null) {
                textName.setVisibility(View.VISIBLE);
                textName.setText(simItem.mName);
            } else {
                textName.setVisibility(View.GONE);
            }
            
            if ((simItem.mNumber != null) && (simItem.mNumber.length() != 0)) {
                textNum.setVisibility(View.VISIBLE);
                textNum.setText(simItem.mNumber);
            } else {
                textNum.setVisibility(View.GONE);
            }
        }
        class ViewHolder {
            TextView mTextName;
            TextView mTextNum;
            RelativeLayout mImageSim;
            ImageView mImageStatus;
            TextView mTextNumFormat;
            TextView mText3G;
            RadioButton mCkRadioOn;
            
        }
        
        public void selectItem(boolean checkDataCon) {
            selectItem(mSelected, checkDataCon);
        }

        public void selectItem(int pos, boolean checkDataCon) {
            Log.d(TAG, "SimListAdapter::selectItem: " + pos);
            setCurSim((ModeSwitchManager.SimItem)getItem(pos));
            if (checkDataCon) {
                sModeMonitor.checkDataConnection(true);
            }
        }

    }

}
