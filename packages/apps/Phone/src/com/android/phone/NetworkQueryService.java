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
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Service;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

/**
 * Service code used to assist in querying the network for service
 * availability.   
 */
public class NetworkQueryService extends Service {
    // debug data
    private static final String LOG_TAG = "Settings/NetworkQuery";
    private static final boolean DBG = true;

    // static events
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100; 
    
    // static states indicating the query status of the service 
    private static final int QUERY_READY = -1;
    private static final int QUERY_IS_RUNNING = -2;
    
    // error statuses that will be retured in the callback.
    public static final int QUERY_OK = 0;
    public static final int QUERY_EXCEPTION = 1;
    
    /** state of the query service */
    private int mState;
    
    /** local handle to the phone object */
    private Phone mPhone;
    /// M: for gemini phone @{    
    private static final int EVENT_NETWORK_SCAN_COMPLETED_2 = 101;
    ///M: add for GEMINI+
    private static final int EVENT_NETWORK_SCAN_COMPLETED_3 = 102;
    private static final int EVENT_NETWORK_SCAN_COMPLETED_4 = 103;


    private GeminiPhone mGeminiPhone;
    private int mSimId = -1;
    /// @}  
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public INetworkQueryService getService() {
            return mBinder;
        }
    }
    private final IBinder mLocalBinder = new LocalBinder();

    /**
     * Local handler to receive the network query compete callback
     * from the RIL.
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // if the scan is complete, broadcast the results.
                // to all registerd callbacks.
                /// M: for gemini support @{
                case EVENT_NETWORK_SCAN_COMPLETED:
                    if (DBG) {
                        log("EVENT_NETWORK_SCAN_COMPLETED--mSimId:" + mSimId);
                    }
                    if (FeatureOption.MTK_GEMINI_SUPPORT && (mSimId != PhoneConstants.GEMINI_SIM_1)) {
                        if (DBG) {
                            log("SIM" + mSimId + " receives the query result of SIM1");
                        }
                        mState = QUERY_READY;
                        mGeminiPhone.getAvailableNetworksGemini(
                            mHandler.obtainMessage(getSimMsgType(mSimId)), mSimId);
                        mState = QUERY_IS_RUNNING;
                        return;
                    }
                    if (DBG) {
                        log("scan completed, broadcasting results");
                    }
                    broadcastQueryResults((AsyncResult) msg.obj);
                    break;
                case EVENT_NETWORK_SCAN_COMPLETED_2:
                    if (DBG) {
                        log("EVENT_NETWORK_SCAN_COMPLETED_2--mSimId:" + mSimId);
                    }
                    if (FeatureOption.MTK_GEMINI_SUPPORT && mSimId != PhoneConstants.GEMINI_SIM_2) {
                        if (DBG) {
                            log("SIM" + mSimId + " receives the query result of SIM2");
                        }
                        mState = QUERY_READY;
                        mGeminiPhone.getAvailableNetworksGemini(
                            mHandler.obtainMessage(getSimMsgType(mSimId)), mSimId);
                        mState = QUERY_IS_RUNNING; 
                        return;
                    }
                    if (DBG) log("scan completed, broadcasting results.");
                    broadcastQueryResults((AsyncResult) msg.obj);
                    break;
                case EVENT_NETWORK_SCAN_COMPLETED_3:
                    if (DBG) log("EVENT_NETWORK_SCAN_COMPLETED--mSimId:" + mSimId);
                    if (FeatureOption.MTK_GEMINI_SUPPORT && (mSimId != PhoneConstants.GEMINI_SIM_3)) {
                        if (DBG) log("SIM" + mSimId + " receives the query result of SIM3");
                        mState = QUERY_READY;
                        mGeminiPhone.getAvailableNetworksGemini(mHandler.obtainMessage(getSimMsgType(mSimId)), mSimId);
                        mState = QUERY_IS_RUNNING;
                        return;
                    }
                    if (DBG) log("scan completed, broadcasting results");
                    broadcastQueryResults((AsyncResult) msg.obj);
                    break;
                case EVENT_NETWORK_SCAN_COMPLETED_4:
                    if (DBG) log("EVENT_NETWORK_SCAN_COMPLETED--mSimId:" + mSimId);
                    if (FeatureOption.MTK_GEMINI_SUPPORT && (mSimId != PhoneConstants.GEMINI_SIM_4)) {
                        if (DBG) log("SIM" + mSimId + " receives the query result of SIM4");
                        mState = QUERY_READY;
                        mGeminiPhone.getAvailableNetworksGemini(mHandler.obtainMessage(getSimMsgType(mSimId)), mSimId);
                        mState = QUERY_IS_RUNNING;
                        return;
                    }
                    if (DBG) log("scan completed, broadcasting results");
                    broadcastQueryResults((AsyncResult) msg.obj);
                    break;
                /// @}
            }
        }
    };
    
    /** 
     * List of callback objects, also used to synchronize access to 
     * itself and to changes in state.
     */
    final RemoteCallbackList<INetworkQueryServiceCallback> mCallbacks =
        new RemoteCallbackList<INetworkQueryServiceCallback>();
    
    /**
     * Implementation of the INetworkQueryService interface.
     */
    private final INetworkQueryService.Stub mBinder = new INetworkQueryService.Stub() {
        
        /**
         * Starts a query with a INetworkQueryServiceCallback object if
         * one has not been started yet.  Ignore the new query request
         * if the query has been started already.  Either way, place the
         * callback object in the queue to be notified upon request 
         * completion.
         */
        public void startNetworkQuery(INetworkQueryServiceCallback cb) {
            if (cb != null) {
                // register the callback to the list of callbacks.
                synchronized (mCallbacks) {
                    mCallbacks.register(cb);
                    if (DBG) {
                        log("registering callback " + cb.getClass().toString());
                    }
                    
                    switch (mState) {
                        case QUERY_READY:
                            // TODO: we may want to install a timeout here in case we
                            // do not get a timely response from the RIL.
                            /// M: support gemini phone
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                int msgType = getSimMsgType(mSimId);
                                log("startNetworkQuery---msgType=" + msgType);
                                mGeminiPhone.getAvailableNetworksGemini(mHandler.obtainMessage(msgType), mSimId);
                            } else {
                                mPhone.getAvailableNetworks(mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED));
                            }
                            mState = QUERY_IS_RUNNING;
                            if (DBG) {
                                log("starting new query");
                            }
                            break;
                            
                        // do nothing if we're currently busy.
                        case QUERY_IS_RUNNING:
                            if (DBG) {
                                log("query already in progress");
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        
        /**
         * Stops a query with a INetworkQueryServiceCallback object as
         * a token.
         */
        public void stopNetworkQuery(INetworkQueryServiceCallback cb) {
            // currently we just unregister the callback, since there is 
            // no way to tell the RIL to terminate the query request.  
            // This means that the RIL may still be busy after the stop 
            // request was made, but the state tracking logic ensures 
            // that the delay will only last for 1 request even with 
            // repeated button presses in the NetworkSetting activity. 
            if (cb != null) {
                synchronized (mCallbacks) {
                    if (DBG) {
                        log("unregistering callback " + cb.getClass().toString());
                    }
                    mCallbacks.unregister(cb);
                }
            }            
        }
    };
    
    @Override
    public void onCreate() {
        mState = QUERY_READY;
        mPhone = PhoneFactory.getDefaultPhone();
        /// M: support gemini phone 
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
        }
    }
    
    /**
     * Required for service implementation.
     */
    /// M: start a new query
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DBG) {
            log("onStartCommand");
        }
        if (intent != null) {
            mSimId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        } else {
            mSimId = -1;
        }
        return Service.START_NOT_STICKY;
    }
    
    /**
     * Handle the bind request.
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Currently, return only the LocalBinder instance.  If we
        // end up requiring support for a remote binder, we will need to 
        // return mBinder as well, depending upon the intent.
        if (DBG) {
            log("binding service implementation");
        }
        return mLocalBinder;
    }
    private int getSimMsgType(int slotId) {
        int msgType = EVENT_NETWORK_SCAN_COMPLETED;
        switch (mSimId) {
            case PhoneConstants.GEMINI_SIM_1:
            msgType = EVENT_NETWORK_SCAN_COMPLETED;
            break;
            case PhoneConstants.GEMINI_SIM_2:
            msgType = EVENT_NETWORK_SCAN_COMPLETED_2;
            break;
            case PhoneConstants.GEMINI_SIM_3:
            msgType = EVENT_NETWORK_SCAN_COMPLETED_3;
            break;
            case PhoneConstants.GEMINI_SIM_4:
            msgType = EVENT_NETWORK_SCAN_COMPLETED_4;
            break;
            default:
            log("Error wrong sim id");
        }
        return msgType;
    }

    /**
     * Broadcast the results from the query to all registered callback
     * objects. 
     */
    private void broadcastQueryResults(AsyncResult ar) {
        // reset the state.
        synchronized (mCallbacks) {
            mState = QUERY_READY;
            
            // see if we need to do any work.
            if (ar == null) {
                if (DBG) {
                    log("AsyncResult is null.");
                }
                return;
            }
    
            // TODO: we may need greater accuracy here, but for now, just a
            // simple status integer will suffice.
            int exception = (ar.exception == null) ? QUERY_OK : QUERY_EXCEPTION;
            if (DBG) {
                log("AsyncResult has exception " + exception);
            }
            
            // Make the calls to all the registered callbacks.
            for (int i = (mCallbacks.beginBroadcast() - 1); i >= 0; i--) {
                INetworkQueryServiceCallback cb = mCallbacks.getBroadcastItem(i); 
                if (DBG) {
                    log("broadcasting results to " + cb.getClass().toString());
                }
                try {
                    cb.onQueryComplete((ArrayList<OperatorInfo>) ar.result, exception);
                } catch (RemoteException e) {
                    log("e = " + e);
                }
            }
            
            // finish up.
            mCallbacks.finishBroadcast();
        }
    }
    
    private static void log(String msg) {
        Xlog.d(LOG_TAG, msg);
    }    
}
