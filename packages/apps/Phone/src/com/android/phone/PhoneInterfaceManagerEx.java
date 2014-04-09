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

package com.android.phone;


import android.os.AsyncResult;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.IccFileHandler;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.telephony.ITelephonyEx;


/**
 * Implementation of the ITelephony interface.
 */
public class PhoneInterfaceManagerEx extends ITelephonyEx.Stub {

    private static final String LOG_TAG = "PhoneInterfaceManagerEx";
    private static final boolean DBG = true;

    /** The singleton instance. */
    private static PhoneInterfaceManagerEx sInstance;

    PhoneGlobals mApp;
    Phone mPhone;
    CallManager mCM;
    MainThreadHandler mMainThreadHandler;

    // Query SIM phonebook Adn stroage info thread
    private QueryAdnInfoThread mAdnInfoThread = null;

    /* Query network lock start */

    // Verify network lock result.
    public static final int VERIFY_RESULT_PASS = 0;
    public static final int VERIFY_INCORRECT_PASSWORD = 1;
    public static final int VERIFY_RESULT_EXCEPTION = 2;

    // Total network lock count.
    public static final int NETWORK_LOCK_TOTAL_COUNT = 5;
    
    public static final String QUERY_SIMME_LOCK_RESULT = 
            "com.android.phone.QUERY_SIMME_LOCK_RESULT";

    public static final String SIMME_LOCK_LEFT_COUNT = 
            "com.android.phone.SIMME_LOCK_LEFT_COUNT";
    /* Query network lock end */

    /* SMS Center Address start*/
    private static final int CMD_HANDLE_GET_SCA = 11;
    private static final int CMD_GET_SCA_DONE = 12;
    private static final int CMD_HANDLE_SET_SCA = 13;
    private static final int CMD_SET_SCA_DONE = 14;
    /* SMS Center Address end*/

    /**
     * Initialize the singleton PhoneInterfaceManagerEx instance.
     * This is only done once, at startup, from PhoneGlobals.onCreate().
     */
    /* package */ static PhoneInterfaceManagerEx init(PhoneGlobals app, Phone phone) {
        synchronized (PhoneInterfaceManagerEx.class) {
            if (sInstance == null) {
                sInstance = new PhoneInterfaceManagerEx(app, phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private PhoneInterfaceManagerEx(PhoneGlobals app, Phone phone) {
        mApp = app;
        mPhone = phone;
        mCM = PhoneGlobals.getInstance().mCM;
        mMainThreadHandler = new MainThreadHandler();
        publish();
    }

    private void publish() {
        if (DBG) log("publish: " + this);

        ServiceManager.addService("phoneEx", this);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[PhoneIntfMgrEx] " + msg);
    }

    private void loge(String msg) {
        Log.e(LOG_TAG, "[PhoneIntfMgrEx] " + msg);
    }

    private class ScAddrGemini {
        public String scAddr;
        public int simId;

        public ScAddrGemini(String addr, int id) {
            this.scAddr = addr;
            if(id == PhoneConstants.GEMINI_SIM_1 ||
               id == PhoneConstants.GEMINI_SIM_2 ||
               id == PhoneConstants.GEMINI_SIM_3 ||
               id == PhoneConstants.GEMINI_SIM_4) {
                   simId = id;
               } else {
                   simId = PhoneConstants.GEMINI_SIM_1;
               }
        }
     }

    /**
     * A request object for use with {@link MainThreadHandler}. Requesters should wait() on the
     * request after sending. The main thread will notify the request when it is complete.
     */
    private static final class MainThreadRequest {
        /** The argument to use for the request */
        public Object argument;
        /** The result of the request that is run on the main thread */
        public Object result;

        public MainThreadRequest(Object argument) {
            this.argument = argument;
        }
    }

    /**
     * A handler that processes messages on the main thread in the phone process. Since many
     * of the Phone calls are not thread safe this is needed to shuttle the requests from the
     * inbound binder threads to the main thread in the phone process.  The Binder thread
     * may provide a {@link MainThreadRequest} object in the msg.obj field that they are waiting
     * on, which will be notified when the operation completes and will contain the result of the
     * request.
     *
     * <p>If a MainThreadRequest object is provided in the msg.obj field,
     * note that request.result must be set to something non-null for the calling thread to
     * unblock.
     */
    private final class MainThreadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MainThreadRequest request;
            Message onCompleted;
            AsyncResult ar;

            switch (msg.what) {
                case CMD_HANDLE_GET_SCA:
                    request = (MainThreadRequest)msg.obj;
                    onCompleted = obtainMessage(CMD_GET_SCA_DONE, request);

                    if(request.argument == null) {
                       // non-gemini
                    } else {
                       ScAddrGemini sca = (ScAddrGemini)request.argument;
                       int simId = sca.simId;

                       if(FeatureOption.MTK_GEMINI_SUPPORT) {
                           Log.d(LOG_TAG, "[sca get sc gemini");
                           ((GeminiPhone)mPhone).getSmscAddressGemini(onCompleted, simId);
                       } else  {
                           Log.d(LOG_TAG, "[sca get sc single");
                           mPhone.getSmscAddress(onCompleted);
                       }
        }
                    break;

                case CMD_GET_SCA_DONE:
                    ar = (AsyncResult)msg.obj;
                    request = (MainThreadRequest)ar.userObj;

                    if(ar.exception == null && ar.result != null) {
                            Log.d(LOG_TAG, "[sca get result");
                            request.result = ar.result;
                    } else {
                        Log.d(LOG_TAG, "[sca Fail to get sc address");
                            request.result = new String("");
                    }

                    synchronized(request) {
                            Log.d(LOG_TAG, "[sca notify sleep thread");
                            request.notifyAll();
                    }
                    break;

                case CMD_HANDLE_SET_SCA:
                    request = (MainThreadRequest)msg.obj;
                    onCompleted = obtainMessage(CMD_SET_SCA_DONE, request);

                    ScAddrGemini sca = (ScAddrGemini)request.argument;
                    if(sca.simId == -1) {
                            // non-gemini
                    } else {
                        if(FeatureOption.MTK_GEMINI_SUPPORT) {
                                    Log.d(LOG_TAG, "[sca set sc gemini");
                                ((GeminiPhone)mPhone).setSmscAddressGemini(sca.scAddr, onCompleted, sca.simId);
                        } else {
                            Log.d(LOG_TAG, "[sca set sc single");
                            mPhone.setSmscAddress(sca.scAddr, onCompleted);
                        }
                    }
                    break;

                case CMD_SET_SCA_DONE:
                    ar = (AsyncResult)msg.obj;
                    request = (MainThreadRequest)ar.userObj;
                    if(ar.exception != null) {
                        Log.d(LOG_TAG, "[sca Fail: set sc address");
                    } else {
                        Log.d(LOG_TAG, "[sca Done: set sc address");
                    }
                    request.result = new Object();
        
                    synchronized(request) {
                        request.notifyAll();
                    }
                    break;
            }
        }
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see sendRequestAsync
     */
    private Object sendRequest(int command, Object argument) {
        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }

        MainThreadRequest request = new MainThreadRequest(argument);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete
                }
            }
        }
        return request.result;
    }

    private static class UnlockSim extends Thread {

        private final IccCard mSimCard;

        private boolean mDone = false;
        private boolean mResult = false;

        // For replies from SimCard interface
        private Handler mHandler;

        private static final int QUERY_NETWORK_STATUS_COMPLETE = 100;
        private static final int SUPPLY_NETWORK_LOCK_COMPLETE = 101;

        private int mVerifyResult = -1;
        private int mSIMMELockRetryCount = -1;

        public UnlockSim(IccCard simCard) {
            mSimCard = simCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (UnlockSim.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case QUERY_NETWORK_STATUS_COMPLETE:
                                synchronized (UnlockSim.this) {
                                    int [] LockState = (int [])ar.result;
                                    if (ar.exception != null) { //Query exception occurs
                                        Log.d (LOG_TAG, "Query network lock fail");
                                        mResult = false;
                                        mDone = true;  
                                    }else{
                                        mSIMMELockRetryCount = LockState[2];
                                        Log.d (LOG_TAG, "[SIMQUERY] Category = " + LockState[0] 
                                            + " ,Network status =" + LockState[1] 
                                            + " ,Retry count = " + LockState[2]);
                                        
                                        mDone = true;
                                        mResult = true;
                                        UnlockSim.this.notifyAll();
                                    }
                                }
                                break;
                            case SUPPLY_NETWORK_LOCK_COMPLETE:
                                Log.d(LOG_TAG, "SUPPLY_NETWORK_LOCK_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    if ((ar.exception != null) &&
                                           (ar.exception instanceof CommandException)) {
                                        Log.d(LOG_TAG, "ar.exception " + ar.exception);
                                        if (((CommandException)ar.exception).getCommandError()
                                            == CommandException.Error.PASSWORD_INCORRECT) {
                                            mVerifyResult = VERIFY_INCORRECT_PASSWORD;
                                       } else {
                                            mVerifyResult = VERIFY_RESULT_EXCEPTION;
                                       }
                                    } else {
                                        mVerifyResult = VERIFY_RESULT_PASS;
                                    }
                                    mDone = true;
                                    UnlockSim.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                UnlockSim.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized Bundle queryNetworkLock(int category) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Log.d(LOG_TAG, "Enter queryNetworkLock");
            Message callback = Message.obtain(mHandler, QUERY_NETWORK_STATUS_COMPLETE);
            mSimCard.QueryIccNetworkLock(category,4,null,null,null,null,callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean(QUERY_SIMME_LOCK_RESULT, mResult);
            bundle.putInt(SIMME_LOCK_LEFT_COUNT, mSIMMELockRetryCount);
            
            Log.d(LOG_TAG, "done");
            return bundle;
        }

        synchronized int supplyNetworkLock(String strPasswd) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Log.d(LOG_TAG, "Enter supplyNetworkLock");
            Message callback = Message.obtain(mHandler, SUPPLY_NETWORK_LOCK_COMPLETE);
            mSimCard.supplyNetworkDepersonalization(strPasswd, callback);

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            
            Log.d(LOG_TAG, "done");
            return mVerifyResult;
        }      
    }


    public Bundle queryNetworkLock(int category, int simId) {
        final UnlockSim queryNetworkLockState;

        Log.d(LOG_TAG, "queryNetworkLock");        
        if(false == FeatureOption.MTK_GEMINI_SUPPORT) {
            queryNetworkLockState = new UnlockSim(mPhone.getIccCard());
        } else {
            queryNetworkLockState = new UnlockSim(((GeminiPhone)mPhone).getIccCardGemini(simId));
        }
        queryNetworkLockState.start();
        return queryNetworkLockState.queryNetworkLock(category);
    }

    public int supplyNetworkDepersonalization(String strPasswd, int simId) {
        final UnlockSim supplyNetworkLock;

        Log.d(LOG_TAG, "supplyNetworkDepersonalization");    
        if(false == FeatureOption.MTK_GEMINI_SUPPORT) {
            supplyNetworkLock = new UnlockSim(mPhone.getIccCard());
        } else {
            supplyNetworkLock = new UnlockSim(((GeminiPhone)mPhone).getIccCardGemini(simId));
        }
        supplyNetworkLock.start();
        return supplyNetworkLock.supplyNetworkLock(strPasswd);
    }  

   /**
    * This function is used to get SIM phonebook storage information
    * by sim id.
    *
    * @param simId Indicate which sim(slot) to query
    * @return int[] which incated the storage info
    *         int[0]; // # of remaining entries
    *         int[1]; // # of total entries
    *         int[2]; // # max length of number
    *         int[3]; // # max length of alpha id
    *
    */ 
    public int[] getAdnStorageInfo(int simId) {
        Log.d(LOG_TAG, "getAdnStorageInfo " + simId);
        if (mAdnInfoThread == null) {
            Log.d(LOG_TAG, "getAdnStorageInfo new thread ");
            mAdnInfoThread  = new QueryAdnInfoThread(simId,mPhone);
            mAdnInfoThread.start();
        } else {
            mAdnInfoThread.setSimId(simId);
            Log.d(LOG_TAG, "getAdnStorageInfo old thread ");
        }
        return mAdnInfoThread.GetAdnStorageInfo(); 
    }   

    private static class QueryAdnInfoThread extends Thread {
    
        private int mSimId;
        private boolean mDone = false;
        private int[] recordSize;
    
        private Handler mHandler;
            
        Phone myPhone;
        // For async handler to identify request type
        private static final int EVENT_QUERY_PHB_ADN_INFO = 100;
    
        public QueryAdnInfoThread(int simId, Phone myP) {
            mSimId = simId;
               
            myPhone = myP;
        }
        public void setSimId(int simId) {
            mSimId = simId;
            mDone = false;
        }
        
        @Override
        public void run() {
            Looper.prepare();
            synchronized (QueryAdnInfoThread.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                          
                        switch (msg.what) {
                            case EVENT_QUERY_PHB_ADN_INFO:
                                Log.d(LOG_TAG, "EVENT_QUERY_PHB_ADN_INFO");
                                synchronized (QueryAdnInfoThread.this) {
                                    mDone = true;
                                    int[] info = (int[]) (ar.result);
                                    if(info!=null){
                                        recordSize = new int[4];
                                        recordSize[0] = info[0]; // # of remaining entries
                                        recordSize[1] = info[1]; // # of total entries
                                        recordSize[2] = info[2]; // # max length of number
                                        recordSize[3] = info[3]; // # max length of alpha id
                                        Log.d(LOG_TAG,"recordSize[0]="+ recordSize[0]+",recordSize[1]="+ recordSize[1] +
                                                         "recordSize[2]="+ recordSize[2]+",recordSize[3]="+ recordSize[3]);
                                    }
                                    else {
                                        recordSize = new int[4];
                                        recordSize[0] = 0; // # of remaining entries
                                        recordSize[1] = 0; // # of total entries
                                        recordSize[2] = 0; // # max length of number
                                        recordSize[3] = 0; // # max length of alpha id                                           
                                    }
                                    QueryAdnInfoThread.this.notifyAll();
                                      
                                }
                                break;
                            }
                      }
                };
                QueryAdnInfoThread.this.notifyAll();
            }
            Looper.loop();
        }
    
        public int[] GetAdnStorageInfo() {   
            synchronized (QueryAdnInfoThread.this) { 
                while (mHandler == null) {
                    try {                
                        QueryAdnInfoThread.this.wait();
                          
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Message response = Message.obtain(mHandler, EVENT_QUERY_PHB_ADN_INFO);
                  
                // protected PhoneBase myPhone = (GeminiPhone)mPhone.getPhonebyId(mSimId);
                IccFileHandler filehandle = null;
                if (FeatureOption.MTK_GEMINI_SUPPORT == true){
                	filehandle = ((GeminiPhone)myPhone).getIccFileHandlerGemini(QueryAdnInfoThread.this.mSimId);
                }
                else {
                	filehandle =((PhoneProxy) myPhone).getIccFileHandler();
                }

                if (filehandle != null) {
                    filehandle.getPhbRecordInfo(response);
                } else {
                    Log.d(LOG_TAG, "GetAdnStorageInfo: filehandle is null.");
                    return null;
                }

                while (!mDone) {
                    try {
                        Log.d(LOG_TAG, "wait for done");
                        QueryAdnInfoThread.this.wait();                    
                    } catch (InterruptedException e) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                }
                Log.d(LOG_TAG, "done");
                return recordSize;
            }
        }
    }


   /**
    * This function is used to check if the SIM phonebook is ready
    * by default sim id.
    *
    * @return true if phone book is ready.  
    */ 
    public boolean isPhbReady(){
        return isPhbReadyExt(mPhone.getMySimId());
    }

   /**
    * This function is used to check if the SIM phonebook is ready
    * by sim id.
    *
    * @param simId Indicate which sim(slot) to query
    * @return true if phone book is ready. 
    * 
    */ 
    public boolean isPhbReadyExt(int simId){
        String strPhbReady = "false";
        if (PhoneConstants.GEMINI_SIM_2 == simId) {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.2", "false");
        } else if (PhoneConstants.GEMINI_SIM_3 == simId) {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.3", "false");
        } else if (PhoneConstants.GEMINI_SIM_4 == simId) {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.4", "false");
        } else {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready", "false");
        }   
        
        log("[isPhbReady] sim id:" + simId + ", isPhbReady: " + strPhbReady);
        
        return strPhbReady.equals("true");
    }

    /**
     * This function is used to check if the SIM phonebook is ready
     * by sim id.
     *
     * @param simId Indicate which sim(slot) to query
     * @return true if phone book is ready. 
     * 
     */ 
     public boolean isPhbReadyGemini(int simId){
         String strPhbReady = "false";
         if (PhoneConstants.GEMINI_SIM_2 == simId) {
             strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.2", "false");
         } else if (PhoneConstants.GEMINI_SIM_3 == simId) {
             strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.3", "false");
         } else if (PhoneConstants.GEMINI_SIM_4 == simId) {
             strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.4", "false");
         } else {
             strPhbReady = SystemProperties.get("gsm.sim.ril.phbready", "false");
         }   
         
         log("[isPhbReady] sim id:" + simId + ", isPhbReady: " + strPhbReady);
         
         return strPhbReady.equals("true");
     }

    /**
     * @return SMS default SIM.
     */
    public int getSmsDefaultSim() {
        if (FeatureOption.MTK_GEMINI_ENHANCEMENT == true) {
            return ((GeminiPhone)mPhone).getSmsDefaultSim();
        } else {
            return SystemProperties.getInt(PhoneConstants.GEMINI_DEFAULT_SIM_PROP, PhoneConstants.GEMINI_SIM_1);
        }
    }

    public String getScAddressGemini(int simId) {
            Log.d(LOG_TAG, "getScAddressGemini: enter");
        if(simId != PhoneConstants.GEMINI_SIM_1 &&
           simId != PhoneConstants.GEMINI_SIM_2 &&
           simId != PhoneConstants.GEMINI_SIM_3 &&
           simId != PhoneConstants.GEMINI_SIM_4)
        {
                Log.d(LOG_TAG, "[sca Invalid sim id");
                return null;
        }

        final ScAddrGemini addr = new ScAddrGemini(null, simId);

        Thread sender = new Thread() {
          public void run() {
                  try {
                  addr.scAddr = (String)sendRequest(CMD_HANDLE_GET_SCA, addr);
              } catch(RuntimeException e) {
                  Log.e(LOG_TAG, "[sca getScAddressGemini " + e);
              }
          }
        };
        sender.start();
        try {
          Log.d(LOG_TAG, "[sca thread join");
          sender.join();
        } catch(InterruptedException e) {
        Log.d(LOG_TAG, "[sca throw interrupted exception");
        }

        Log.d(LOG_TAG, "getScAddressGemini: exit with " + addr.scAddr);

        return addr.scAddr;
    }

    public void setScAddressGemini(String address, int simId) {
        Log.d(LOG_TAG, "setScAddressGemini: enter");
        if(simId != PhoneConstants.GEMINI_SIM_1 &&
           simId != PhoneConstants.GEMINI_SIM_2 &&
           simId != PhoneConstants.GEMINI_SIM_3 &&
           simId != PhoneConstants.GEMINI_SIM_4)
        {
                Log.d(LOG_TAG, "[sca Invalid sim id");
                return;
        }

        final ScAddrGemini addr = new ScAddrGemini(address, simId);

        Thread sender = new Thread() {
                public void run() {
                        try {
                        addr.scAddr = (String)sendRequest(CMD_HANDLE_SET_SCA, addr);
                } catch(RuntimeException e) {
            Log.e(LOG_TAG, "[sca setScAddressGemini " + e);
                }
                }
        };
        sender.start();
        try {
                Log.d(LOG_TAG, "[sca thread join");
                sender.join();
        } catch(InterruptedException e) {
           Log.d(LOG_TAG, "[sca throw interrupted exception");
        }

        Log.d(LOG_TAG, "setScAddressGemini: exit");
    }

    
/*********************************************************************/


}
