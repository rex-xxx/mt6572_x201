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

package com.mediatek.internal.telephony.cat;

import static com.android.internal.telephony.cat.CatService.MSG_ID_CONN_MGR_TIMEOUT;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Message;
import android.os.SystemProperties;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cat.CatResponseMessage;

import com.mediatek.common.featureoption.FeatureOption;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.common.telephony.IBipManagerExt;
import com.mediatek.common.telephony.OtherAddress;
import com.mediatek.common.telephony.TransportProtocol;
import com.mediatek.common.telephony.BipUtils;
import com.mediatek.common.telephony.ErrorValue;
import com.mediatek.common.telephony.ChannelStatus;
import com.mediatek.common.telephony.BearerDesc;

public class BipManager implements IBipManagerExt {
    private static BipManager instance1 = null;
    private static BipManager instance2 = null;
    private static BipManager instance3 = null;
    private static BipManager instance4 = null;

    private Handler mHandler = null;
    private CatCmdMessage mCurrentCmd = null;

    private Context mContext = null;
    //private Phone mPhone = null;
    private ConnectivityManager mConnMgr = null;

    BearerDesc mBearerDesc = null;
    int mBufferSize = 0;
    OtherAddress mLocalAddress = null;
    TransportProtocol mTransportProtocol = null;
    OtherAddress mDataDestinationAddress = null;
    int mLinkMode = 0;
    boolean mAutoReconnected = false;

    String mApn = null;
    String mLogin = null;
    String mPassword = null;

    final int NETWORK_TYPE = ConnectivityManager.TYPE_MOBILE;

    private int mChannelStatus = BipUtils.CHANNEL_STATUS_UNKNOWN;
    private int mChannelId = 1;
    private Channel mChannel = null;
    private ChannelStatus mChannelStatusDataObject = null;
    private boolean isParamsValid = false;
    private int mSimId = -1;

    private static final int CONN_MGR_TIMEOUT = 30 * 1000;
    private boolean isConnMgrIntentTimeout = false;
    private BipChannelManager mBipChannelManager = null;
    private boolean mIsOpenInProgress = false;

    public BipManager(Context context, Handler handler, int sim_id) {
        CatLog.d("[BIP]", "Construct BipManager");

        if (context == null) {
            CatLog.d("[BIP]", "Fail to construct BipManager");
        }

        mContext = context;
        mSimId = sim_id;
        CatLog.d("[BIP]", "sim id: " + sim_id + ", mSimId: " + mSimId);
        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mHandler = handler;
        mBipChannelManager = new BipChannelManager();

        if(sim_id == PhoneConstants.GEMINI_SIM_1 && instance1 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 1");
            instance1 = this;
        } else if(sim_id == PhoneConstants.GEMINI_SIM_2 && instance2 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 2");
            instance2 = this;
        } else if(sim_id == PhoneConstants.GEMINI_SIM_3 && instance3 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 3");
            instance3 = this;
        } else if(sim_id == PhoneConstants.GEMINI_SIM_4 && instance4 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 3");
            instance4 = this;
        }

        IntentFilter connFilter = new IntentFilter();
        connFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION_IMMEDIATE);
        mContext.registerReceiver(mNetworkConnReceiver, connFilter);
    }

    public void getInstance(Context context, Handler handler, int simId) {
        if(simId == PhoneConstants.GEMINI_SIM_1 && instance1 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 1");
            instance1 = new BipManager(context, handler, simId);
        } else if(simId == PhoneConstants.GEMINI_SIM_2 && instance2 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 2");
            instance2 = new BipManager(context, handler, simId);
        } else if(simId == PhoneConstants.GEMINI_SIM_3 && instance3 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 3");
            instance3 = new BipManager(context, handler, simId);
        } else if(simId == PhoneConstants.GEMINI_SIM_4 && instance4 == null) {
            CatLog.d("[BIP]", "Construct instance for sim 3");
            instance4 = new BipManager(context, handler, simId);
        } else {
            CatLog.d("[BIP]", "Bip instance was generated. sim id: " + simId);
        }
    }

    private int getDataConnectionFromSetting() {
        int currentDataConnectionSimId = -1;

        if(FeatureOption.MTK_GEMINI_ENHANCEMENT == true){
            long currentDataConnectionMultiSimId =  Settings.System.getLong(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            currentDataConnectionSimId = SIMInfo.getSlotById(mContext, currentDataConnectionMultiSimId);
        }else{
            currentDataConnectionSimId =  Settings.System.getInt(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT) - 1;            
        }

        CatLog.d("[BIP]", "Default Data Setting value=" + currentDataConnectionSimId);

        return currentDataConnectionSimId;
    }

    public void openChannel(CatCmdMessage cmdMsg, Message response) {
        int result = PhoneConstants.APN_TYPE_NOT_AVAILABLE;
        CatLog.d("[BIP]", "BM-openChannel: enter");
        int ret = ErrorValue.NO_ERROR;
        Channel channel = null;
        
        CatLog.d("[BIP]", "BM-openChannel: init channel status object");

        isConnMgrIntentTimeout = false;

        mChannelId = mBipChannelManager.acquireChannelId(cmdMsg.mTransportProtocol.protocolType);
        if(0 == mChannelId) {
            CatLog.d("[BIP]", "BM-openChannel: acquire channel id = 0");            
            response.arg1 = ErrorValue.BIP_ERROR;
            response.obj = cmdMsg;
            mCurrentCmd = cmdMsg;
            mHandler.sendMessage(response);
            return;
        }
        cmdMsg.mChannelStatusData = new ChannelStatus(mChannelId, ChannelStatus.CHANNEL_STATUS_NO_LINK, ChannelStatus.CHANNEL_STATUS_INFO_NO_FURTHER_INFO);
        mCurrentCmd = cmdMsg;

        mBearerDesc = cmdMsg.mBearerDesc;
        if(cmdMsg.mBearerDesc != null) {
        CatLog.d("[BIP]", "BM-openChannel: bearer type " + cmdMsg.mBearerDesc.bearerType);
        } else {
            CatLog.d("[BIP]", "BM-openChannel: bearer type is null");
        }

        mBufferSize = cmdMsg.mBufferSize;
        CatLog.d("[BIP]", "BM-openChannel: buffer size " + cmdMsg.mBufferSize);

        mLocalAddress = cmdMsg.mLocalAddress;
        if(cmdMsg.mLocalAddress != null) {
            CatLog.d("[BIP]", "BM-openChannel: local address " + cmdMsg.mLocalAddress.address.toString());
        } else {
            CatLog.d("[BIP]", "BM-openChannel: local address is null");
        }

        mTransportProtocol = cmdMsg.mTransportProtocol;
        if (cmdMsg.mTransportProtocol != null) {
            CatLog.d("[BIP]", "BM-openChannel: transport protocol type/port "
                    + cmdMsg.mTransportProtocol.protocolType + "/" + cmdMsg.mTransportProtocol.portNumber);
        } else {
            CatLog.d("[BIP]", "BM-openChannel: transport protocol is null");
        }

        mDataDestinationAddress = cmdMsg.mDataDestinationAddress;
        if(cmdMsg.mDataDestinationAddress != null) {
            CatLog.d("[BIP]", "BM-openChannel: dest address " + cmdMsg.mDataDestinationAddress.address.toString());
        } else {
            CatLog.d("[BIP]", "BM-openChannel: dest address is null");
        }

        mApn = (cmdMsg.mApn == null) ? "TestGp.rs" : cmdMsg.mApn;
        if (cmdMsg.mApn != null) {
            CatLog.d("[BIP]", "BM-openChannel: apn " + cmdMsg.mApn);
        } else {
            CatLog.d("[BIP]", "BM-openChannel: apn default TestGp.rs");
            mCurrentCmd.mApn = mApn;
        }

        mLogin = cmdMsg.mLogin;
        CatLog.d("[BIP]", "BM-openChannel: login " + cmdMsg.mLogin);
        mPassword = cmdMsg.mPwd;
        CatLog.d("[BIP]", "BM-openChannel: password " + cmdMsg.mPwd);

        mLinkMode = ((cmdMsg.getCmdQualifier() & 0x01) == 1) ?
                BipUtils.LINK_ESTABLISHMENT_MODE_IMMEDIATE : BipUtils.LINK_ESTABLISHMENT_MODE_ONDEMMAND;

        CatLog.d("[BIP]", "BM-openChannel: mLinkMode " + cmdMsg.getCmdQualifier());

        mAutoReconnected = ((cmdMsg.getCmdQualifier() & 0x02) == 0) ? false : true;

        //if(mBearerDesc.bearerType == BipUtils.BEARER_TYPE_GPRS) {
        //  CatLog.d("[BIP]", "BM-openChannel: Set QoS params");
        //  SystemProperties.set(BipUtils.KEY_QOS_PRECEDENCE, String.valueOf(mBearerDesc.precedence));
        //  SystemProperties.set(BipUtils.KEY_QOS_DELAY, String.valueOf(mBearerDesc.delay));
        //  SystemProperties.set(BipUtils.KEY_QOS_RELIABILITY, String.valueOf(mBearerDesc.reliability));
        //  SystemProperties.set(BipUtils.KEY_QOS_PEAK, String.valueOf(mBearerDesc.peak));
        //  SystemProperties.set(BipUtils.KEY_QOS_MEAN, String.valueOf(mBearerDesc.mean));
        //}

        setApnParams(mApn, mLogin, mPassword);
        SystemProperties.set("gsm.stk.bip", "1");

        // Wait for APN is ready. This is a tempoarily solution

        CatLog.d("[BIP]", "BM-openChannel: call startUsingNetworkFeature:" + mSimId);

        if(BipUtils.TRANSPORT_PROTOCOL_SERVER == mTransportProtocol.protocolType) {
            ret = establishLink();
            
            if (ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
                CatLog.d("[BIP]", "BM-openChannel: channel is activated");                
                channel = mBipChannelManager.getChannel(mChannelId);                
                cmdMsg.mChannelStatusData.mChannelStatus = channel.mChannelStatusData.mChannelStatus;
            } else {
                CatLog.d("[BIP]", "BM-openChannel: channel is un-activated");
                cmdMsg.mChannelStatusData.mChannelStatus = BipUtils.TCP_STATUS_CLOSE;
            }
            
            response.arg1 = ret;
            response.obj = mCurrentCmd;
            mHandler.sendMessage(response);            
        } else {
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                if (getDataConnectionFromSetting() == mSimId) {
                    CatLog.d("[BIP]", "Start to establish data connection" + mSimId);
                    result = mConnMgr.startUsingNetworkFeatureGemini(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL, mSimId);
                }
            }else{
                // result = mConnMgr.startUsingNetworkFeatureGemini(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL,mPhone.getMySimId());
                result = mConnMgr.startUsingNetworkFeature(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL);
            }

            if(result == PhoneConstants.APN_ALREADY_ACTIVE) {
                CatLog.d("[BIP]", "BM-openChannel: APN already active");
                if (requestRouteToHost() == false) {
                    CatLog.d("[BIP]", "BM-openChannel: Fail - requestRouteToHost");
                    ret = ErrorValue.NETWORK_CURRENTLY_UNABLE_TO_PROCESS_COMMAND;
                }
                isParamsValid = true;
                mIsOpenInProgress = true;
                CatLog.d("[BIP]", "BM-openChannel: establish data channel");
                ret = establishLink();
                
                if (ret != ErrorValue.WAIT_OPEN_COMPLETED) {
                    if (ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
                        CatLog.d("[BIP]", "BM-openChannel: channel is activated");
                        updateCurrentChannelStatus(ChannelStatus.CHANNEL_STATUS_LINK);
                    } else {
                        CatLog.d("[BIP]", "BM-openChannel: channel is un-activated");
                        updateCurrentChannelStatus(ChannelStatus.CHANNEL_STATUS_NO_LINK);
                    }
                    if(true == mIsOpenInProgress) {
                        mIsOpenInProgress = false;
                        response.arg1 = ret;
                        response.obj = mCurrentCmd;
                        mHandler.sendMessage(response);
                    }
                }                
            } else if(result == PhoneConstants.APN_REQUEST_STARTED) {
                CatLog.d("[BIP]", "BM-openChannel: APN request started");
                isParamsValid = true;
                mIsOpenInProgress = true;
                Message timerMsg = mHandler.obtainMessage(MSG_ID_CONN_MGR_TIMEOUT);
                timerMsg.obj = cmdMsg;
                mHandler.sendMessageDelayed(timerMsg, CONN_MGR_TIMEOUT);
            } else {
                CatLog.d("[BIP]", "BM-openChannel: startUsingNetworkFeature FAIL");
                ret = ErrorValue.NETWORK_CURRENTLY_UNABLE_TO_PROCESS_COMMAND;
                cmdMsg.mChannelStatusData.mChannelStatus = ChannelStatus.CHANNEL_STATUS_NO_LINK;

                response.arg1 = ret;
                response.obj = mCurrentCmd;
                mHandler.sendMessage(response);
            }
        }
        CatLog.d("[BIP]", "BM-openChannel: exit");
    }

    public void closeChannel(CatCmdMessage cmdMsg, Message response) {
        CatLog.d("[BIP]", "BM-closeChannel: enter");

        Channel lChannel = null;              
        int cId = cmdMsg.mCloseCid;
        
        response.arg1 = ErrorValue.NO_ERROR;
        
        if(0 > cId || BipChannelManager.MAXCHANNELID < cId){
            CatLog.d("[BIP]", "BM-closeChannel: channel id is wrong");
            response.arg1 = ErrorValue.CHANNEL_ID_NOT_VALID;
        } else {
            try {
                if(BipUtils.CHANNEL_STATUS_UNKNOWN == mBipChannelManager.getBipChannelStatus(cId))
                    response.arg1 = ErrorValue.CHANNEL_ID_NOT_VALID;
                else if(BipUtils.CHANNEL_STATUS_CLOSE == mBipChannelManager.getBipChannelStatus(cId))
                    response.arg1 = ErrorValue.CHANNEL_ALREADY_CLOSED;
                else {
                    lChannel = mBipChannelManager.getChannel(cId);
                    if(null == lChannel) {
                        CatLog.d("[BIP]", "BM-closeChannel: channel has already been closed");
                        response.arg1 = ErrorValue.CHANNEL_ID_NOT_VALID;
                    } else { //null != lChannel             
                        //mConnMgr.stopUsingNetworkFeature(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL);
                        TcpServerChannel tcpSerCh = null;
                        if(BipUtils.TRANSPORT_PROTOCOL_SERVER == lChannel.mProtocolType) {
                            if(lChannel instanceof TcpServerChannel) {
                                tcpSerCh = (TcpServerChannel)lChannel;
                                tcpSerCh.setCloseBackToTcpListen(cmdMsg.mCloseBackToTcpListen);                        
                            }
                        } else {
                            CatLog.d("[BIP]", "BM-closeChannel: stop data connection");                    
                            if(FeatureOption.MTK_GEMINI_SUPPORT == true)
                            {
                                CatLog.d("[BIP]", "stopUsingNetworkFeature getDataConnectionFromSetting  ==" + mSimId);
                                mConnMgr.stopUsingNetworkFeatureGemini(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL,mSimId);
                            }
                            else
                            {
                                mConnMgr.stopUsingNetworkFeature(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL);
                            }
                        }
                        response.arg1 = lChannel.closeChannel();
                        if(BipUtils.TRANSPORT_PROTOCOL_SERVER == lChannel.mProtocolType) {
                            if(null != tcpSerCh && false == tcpSerCh.isCloseBackToTcpListen()) {
                                mBipChannelManager.removeChannel(cId);
                            }
                        } else {
                            mBipChannelManager.removeChannel(cId);
                        }
                            
                        mChannel = null;
                        mChannelStatus = BipUtils.CHANNEL_STATUS_CLOSE;
                    }
                }
            }catch (IndexOutOfBoundsException e){
                CatLog.e("[BIP]", "BM-closeChannel: IndexOutOfBoundsException cid="+cId); 
                response.arg1 = ErrorValue.CHANNEL_ID_NOT_VALID;
            }
        }
        isParamsValid = false;

        response.obj = cmdMsg;
        mHandler.sendMessage(response);
        CatLog.d("[BIP]", "BM-closeChannel: exit");
    }

    public void closeChannel(Message response) {
        
        CatCmdMessage cmdMsg = ((CatService)mHandler).getCmdMessage();
        
        CatLog.d("[BIP]", "new closeChannel, mCloseCid: " + cmdMsg.mCloseCid);
         
        closeChannel(cmdMsg, response);
    }
    
    public void receiveData(CatCmdMessage cmdMsg, Message response) {
        int requestCount = cmdMsg.mChannelDataLength;
        ReceiveDataResult result = new ReceiveDataResult();
        Channel lChannel = null;        
        int cId = cmdMsg.mReceiveDataCid;

        lChannel = mBipChannelManager.getChannel(cId);
        CatLog.d("[BIP]", "BM-receiveData: receiveData enter");

        if(null == lChannel) {
            CatLog.e("[BIP]", "lChannel is null cid="+cId);        
            response.arg1 = ErrorValue.BIP_ERROR;
            response.obj = cmdMsg;
            mHandler.sendMessage(response);        
            return;            
        }
        if (lChannel.mChannelStatus == BipUtils.CHANNEL_STATUS_OPEN
                || lChannel.mChannelStatus == BipUtils.CHANNEL_STATUS_SERVER_CLOSE) {
            if (requestCount > BipUtils.MAX_APDU_SIZE) {
                CatLog.d("[BIP]", "BM-receiveData: Modify channel data length to MAX_APDU_SIZE");
                requestCount = BipUtils.MAX_APDU_SIZE;
            }
            Thread recvThread = new Thread(new RecvDataRunnable(requestCount, result, cmdMsg, response));
            recvThread.start();
        } else {
            // response ResultCode.BIP_ERROR
            CatLog.d("[BIP]", "BM-receiveData: Channel status is invalid " + mChannelStatus);
            response.arg1 = ErrorValue.BIP_ERROR;
            response.obj = cmdMsg;
            mHandler.sendMessage(response);
        }
    }

    public void receiveData(Message response) {
        CatLog.d("[BIP]", "new receiveData");
        CatCmdMessage cmdMsg = ((CatService)mHandler).getCmdMessage();
        receiveData(cmdMsg, response);
    }
    
    public void sendData(CatCmdMessage cmdMsg, Message response) 
    {
        CatLog.d("[BIP]", "sendData: Enter");
/*
        int cId = cmdMsg.mSendDataCid;
        Channel channel = BipChannelManager.getChannel(cId); 

        if(null == channel) {
            CatLog.e("[BIP]", "sendData: channel null");            
            ret = ErrorValue.CHANNEL_ID_NOT_VALID; 
            cmdMsg.mChannelStatusData.isActivated = false;
            response.arg1 = ret;                    
            response.obj = cmdMsg;
            mHandler.sendMessage(response);                                
        }
        
        if(BipUtils.LINK_ESTABLISHMENT_MODE_ONDEMMAND == channel.mLinkMode) {
            int result = PhoneConstants.APN_TYPE_NOT_AVAILABLE;
            int ret = ErrorValue.NO_ERROR;
            
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                if (getDataConnectionFromSetting() == mSimId) {
                    CatLog.d("[BIP]", "Start to establish data connection" + mSimId);
                    result = mConnMgr.startUsingNetworkFeatureGemini(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL, mSimId);
                }
            }else{
                // result = mConnMgr.startUsingNetworkFeatureGemini(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL,mPhone.getMySimId());
                result = mConnMgr.startUsingNetworkFeature(NETWORK_TYPE, Phone.FEATURE_ENABLE_SUPL);
            }
            if(result == PhoneConstants.APN_ALREADY_ACTIVE) {
                CatLog.d("[BIP]", "BM-openChannel: APN already active");
                if (requestRouteToHost() == false) {
                    CatLog.d("[BIP]", "BM-openChannel: Fail - requestRouteToHost");
                    ret = ErrorValue.NETWORK_CURRENTLY_UNABLE_TO_PROCESS_COMMAND;                    
                    cmdMsg.mChannelStatusData.isActivated = false;
                    response.arg1 = ret;                    
                    response.obj = cmdMsg;
                    mHandler.sendMessage(response);                    
                }
            } else if(result == PhoneConstants.APN_REQUEST_STARTED) {
                CatLog.d("[BIP]", "BM-openChannel: APN request started");
                mIsSendInProgress = true;
                Message timerMsg = mHandler.obtainMessage(MSG_ID_CONN_MGR_TIMEOUT);
                timerMsg.obj = cmdMsg;
                mHandler.sendMessageDelayed(timerMsg, CONN_MGR_TIMEOUT);
            } else {
                CatLog.d("[BIP]", "BM-openChannel: startUsingNetworkFeature FAIL");
                ret = ErrorValue.NETWORK_CURRENTLY_UNABLE_TO_PROCESS_COMMAND;
                cmdMsg.mChannelStatusData.isActivated = false;

                response.arg1 = ret;
                response.obj = cmdMsg;
                mHandler.sendMessage(response);
            }
            
        }
*/        
        Thread rt = new Thread(new SendDataThread(cmdMsg, response));
        rt.start();
        CatLog.d("[BIP]", "sendData: Leave");
    }

    public void sendData(Message response) {
        CatLog.d("[BIP]", "new sendData: Enter");
        CatCmdMessage cmdMsg = ((CatService)mHandler).getCmdMessage();
        sendData(cmdMsg, response);
    }

    public void getChannelStatus(Message response) {
        CatLog.d("[BIP]", "new getChannelStatus");

        CatCmdMessage cmdMsg = ((CatService)mHandler).getCmdMessage();
        getChannelStatus(cmdMsg, response);
    }

    public void openChannel(Message response) {
        CatLog.d("[BIP]", "new openChannel");
        CatCmdMessage cmdMsg = ((CatService)mHandler).getCmdMessage();
        openChannel(cmdMsg, response);
    }
    
    protected class SendDataThread implements Runnable 
    {
        CatCmdMessage cmdMsg;
        Message response;

        SendDataThread(CatCmdMessage Msg,Message resp)
        {
            CatLog.d("[BIP]", "SendDataThread Init");
            cmdMsg = Msg;
            response = resp;
        }

        @Override
        public void run() 
        {
            CatLog.d("[BIP]", "SendDataThread Run Enter");
            int ret = ErrorValue.NO_ERROR;

            byte[] buffer = cmdMsg.mChannelData;
            int mode = cmdMsg.mSendMode;
            Channel lChannel = null;
            int cId = cmdMsg.mSendDataCid;

            lChannel = mBipChannelManager.getChannel(cId);
            do {
                if(null == lChannel) {//if(mChannelId != cmdMsg.mSendDataCid)
                    CatLog.d("[BIP]", "SendDataThread Run mChannelId != cmdMsg.mSendDataCid");
                    ret = ErrorValue.CHANNEL_ID_NOT_VALID;
                    break;
                }
                
                if(lChannel.mChannelStatus == BipUtils.CHANNEL_STATUS_OPEN)
                {
                    CatLog.d("[BIP]", "SendDataThread Run mChannel.sendData");
                    ret = lChannel.sendData(buffer, mode);
                    response.arg2 = lChannel.getTxAvailBufferSize();
                }
                else
                {
                    CatLog.d("[BIP]", "SendDataThread Run CHANNEL_ID_NOT_VALID");
                    ret = ErrorValue.CHANNEL_ID_NOT_VALID;
                }            
            }while(false);
            response.arg1 = ret;
            response.obj = cmdMsg;
            CatLog.d("[BIP]", "SendDataThread Run mHandler.sendMessage(response);");
            mHandler.sendMessage(response);
        }
    }

    public void getChannelStatus(CatCmdMessage cmdMsg, Message response) {
        int ret = ErrorValue.NO_ERROR;
        int cId = 1; 
        List<ChannelStatus> csList = new ArrayList<ChannelStatus>();

        try {
            while(cId <= mBipChannelManager.MAXCHANNELID){
                if(true == mBipChannelManager.isChannelIdOccupied(cId)) {
                    CatLog.d("[BIP]", "getChannelStatus: cId:"+cId);                
                    csList.add(mBipChannelManager.getChannel(cId).mChannelStatusData);
                }
                cId++;
            }
        } catch (NullPointerException ne) {
            CatLog.e("[BIP]", "getChannelStatus: NE");   
            ne.printStackTrace();             
        }
        cmdMsg.mChannelStatusList = csList;
        response.arg1 = ret;
        response.obj = cmdMsg;
        mHandler.sendMessage(response);
    }

    private void updateCurrentChannelStatus(int status){  
        try {
            mBipChannelManager.updateChannelStatus(mChannelId, status);
            mCurrentCmd.mChannelStatusData.mChannelStatus = status;        
        } catch (NullPointerException ne) {
            CatLog.e("[BIP]", "updateCurrentChannelStatus id:"+mChannelId+" is null");
            ne.printStackTrace();                         
        }
    }
    private boolean requestRouteToHost() {
        CatLog.d("[BIP]", "requestRouteToHost");
        byte[] addressBytes = null;
        if (mDataDestinationAddress != null) {
            addressBytes = mDataDestinationAddress.address.getAddress();
        } else {
            CatLog.d("[BIP]", "mDataDestinationAddress is null");
            return false;
        }
        int addr = 0;
        addr = ((addressBytes[3] & 0xFF) << 24)
                | ((addressBytes[2] & 0xFF) << 16)
                | ((addressBytes[1] & 0xFF) << 8)
                | (addressBytes[0] & 0xFF);

        return mConnMgr.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_SUPL, addr);
    }

    private boolean checkNetworkInfo(NetworkInfo nwInfo, NetworkInfo.State exState) {
        if (nwInfo == null) {
            return false;
        }

        int type = nwInfo.getType();
        NetworkInfo.State state = nwInfo.getState();
        CatLog.d("[BIP]", "network type is " + ((type == ConnectivityManager.TYPE_MOBILE) ? "MOBILE" : "WIFI"));
        CatLog.d("[BIP]", "network state is " + state);

        if (type == ConnectivityManager.TYPE_MOBILE && state == exState) {
            return true;
        }

        return false;
    }

    private int establishLink() {
        int ret = ErrorValue.NO_ERROR;
        Channel lChannel = null;
        
        if(true == FeatureOption.MTK_BIP_SCWS && mTransportProtocol.protocolType == BipUtils.TRANSPORT_PROTOCOL_SERVER){
            
            CatLog.d("[BIP]", "BM-establishLink: establish a TCPServer link");
            try {
            lChannel = new TcpServerChannel(mChannelId, mLinkMode, mTransportProtocol.protocolType,
                    mTransportProtocol.portNumber, mBufferSize, ((CatService)mHandler), this);
            }catch (NullPointerException ne){
                CatLog.e("[BIP]", "BM-establishLink: NE,new TCP server channel fail.");
                ne.printStackTrace();                             
                return ErrorValue.BIP_ERROR;
            }
            ret = lChannel.openChannel(mCurrentCmd);
            if (ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {                
                mChannelStatus = BipUtils.CHANNEL_STATUS_OPEN;
                mBipChannelManager.addChannel(mChannelId, lChannel);                
            } else {
                mBipChannelManager.releaseChannelId(mChannelId,BipUtils.TRANSPORT_PROTOCOL_SERVER);
                mChannelStatus = BipUtils.CHANNEL_STATUS_ERROR;
            }            
        }
        else if (true == FeatureOption.MTK_BIP_SCWS && mTransportProtocol.protocolType == BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE) {
            CatLog.d("[BIP]", "BM-establishLink: establish a TCP link");
            try {
            lChannel = new TcpChannel(mChannelId, mLinkMode,
                    mTransportProtocol.protocolType, mDataDestinationAddress.address,
                    mTransportProtocol.portNumber, mBufferSize, ((CatService)mHandler), this);
            }catch (NullPointerException ne){
                CatLog.e("[BIP]", "BM-establishLink: NE,new TCP client channel fail.");
                ne.printStackTrace();                             
                if(null == mDataDestinationAddress)
                    return ErrorValue.MISSING_DATA;
                else
                    return ErrorValue.BIP_ERROR;
            }
            ret = lChannel.openChannel(mCurrentCmd);
            if (ret != ErrorValue.WAIT_OPEN_COMPLETED) {
                if(ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
                    mChannelStatus = BipUtils.CHANNEL_STATUS_OPEN;
                    mBipChannelManager.addChannel(mChannelId, lChannel);                                
                } else {
                    mBipChannelManager.releaseChannelId(mChannelId,BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE);
                    mChannelStatus = BipUtils.CHANNEL_STATUS_ERROR;
                }    
            }
        } else if (mTransportProtocol.protocolType == BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE) {
            // establish upd link
            CatLog.d("[BIP]", "BM-establishLink: establish a UDP link");
            try {
            lChannel = new UdpChannel(mChannelId, mLinkMode, mTransportProtocol.protocolType,
                    mDataDestinationAddress.address, mTransportProtocol.portNumber, mBufferSize,
                    ((CatService)mHandler), this);
            }catch (NullPointerException ne){
                CatLog.e("[BIP]", "BM-establishLink: NE,new UDP client channel fail.");
                ne.printStackTrace();                             
                return ErrorValue.BIP_ERROR;
            }
            ret = lChannel.openChannel(mCurrentCmd);
            if (ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
                mChannelStatus = BipUtils.CHANNEL_STATUS_OPEN;
                mBipChannelManager.addChannel(mChannelId, lChannel);                                                
            } else {
                mBipChannelManager.releaseChannelId(mChannelId,BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE);
                mChannelStatus = BipUtils.CHANNEL_STATUS_ERROR;
            }
        } else {
            CatLog.d("[BIP]", "BM-establishLink: unsupported channel type");
            ret = ErrorValue.UNSUPPORTED_TRANSPORT_PROTOCOL_TYPE;
            mChannelStatus = BipUtils.CHANNEL_STATUS_ERROR;
        }

        CatLog.d("[BIP]", "BM-establishLink: ret:" + ret);
        return ret;
    }

    private void setApnParams(String apn, String user, String pwd) {
        CatLog.d("[BIP]", "BM-setApnParams: enter");
        if (apn == null) {
            CatLog.d("[BIP]", "BM-setApnParams: No apn parameters");
            return;
        }

        Uri uri = null;
        String numeric = null;
        String mcc = null;
        String mnc = null;
        String apnType = "supl";

        /*
         * M for telephony provider enhancement
         */
        if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            CatLog.d("[BIP]", "BM-setApnParams: URI use telephony provider enhancement");
            if(mSimId == PhoneConstants.GEMINI_SIM_1) {
                uri = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
            } else if(mSimId == PhoneConstants.GEMINI_SIM_2) {
                uri = Telephony.Carriers.SIM2Carriers.CONTENT_URI;
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2);
            } else if(mSimId == PhoneConstants.GEMINI_SIM_3) {
                uri = Telephony.Carriers.SIM3Carriers.CONTENT_URI;
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_3);
            } else if(mSimId == PhoneConstants.GEMINI_SIM_4) {
                uri = Telephony.Carriers.SIM4Carriers.CONTENT_URI;
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_4);
            } else {
                CatLog.d("[BIP]", "BM-setApnParams: invalid sim id");
            }
        } else {
            CatLog.d("[BIP]", "BM-setApnParams: URI use normal single card");
            uri = Telephony.Carriers.CONTENT_URI;
            numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
        }

        if (uri == null) {
            CatLog.e("[BIP]", "BM-setApnParams: Invalid uri");
        }

        if (numeric != null && numeric.length() >= 4) {
            Cursor cursor = null;
            mcc = numeric.substring(0, 3);
            mnc = numeric.substring(3);
            CatLog.d("[BIP]", "BM-setApnParams: mcc = " + mcc + ", mnc = " + mnc);
            String selection = "name = 'BIP' and numeric = '" + mcc + mnc + "'";

            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                    cursor = mContext.getContentResolver().query(
                            Telephony.Carriers.SIM1Carriers.CONTENT_URI, null, selection, null, null);
                } else if(mSimId == PhoneConstants.GEMINI_SIM_2) {
                    cursor = mContext.getContentResolver().query(
                            Telephony.Carriers.SIM2Carriers.CONTENT_URI, null, selection, null, null); 
                } else if(mSimId == PhoneConstants.GEMINI_SIM_3) {
                    cursor = mContext.getContentResolver().query(
                            Telephony.Carriers.SIM3Carriers.CONTENT_URI, null, selection, null, null);
                } else if(mSimId == PhoneConstants.GEMINI_SIM_4) {
                    cursor = mContext.getContentResolver().query(
                            Telephony.Carriers.SIM4Carriers.CONTENT_URI, null, selection, null, null);
                }
            } else {
                cursor = mContext.getContentResolver().query(
                        Telephony.Carriers.CONTENT_URI, null, selection, null, null);
            }

            if (cursor != null) {
                ContentValues values = new ContentValues();
                values.put(Telephony.Carriers.NAME, "BIP");
                values.put(Telephony.Carriers.APN, apn);
                values.put(Telephony.Carriers.USER, user);
                values.put(Telephony.Carriers.PASSWORD, pwd);
                values.put(Telephony.Carriers.TYPE, apnType);
                values.put(Telephony.Carriers.MCC, mcc);
                values.put(Telephony.Carriers.MNC, mnc);
                values.put(Telephony.Carriers.NUMERIC, mcc + mnc);

                if (cursor.getCount() == 0) {
                    // int updateResult = mContext.getContentResolver().update(
                    // uri, values, selection, selectionArgs);
                    CatLog.d("[BIP]", "BM-setApnParams: insert one record");
                    Uri newRow = mContext.getContentResolver().insert(uri, values);
                    if (newRow != null) {
                        CatLog.d("[BIP]", "insert a new record into db");
                    } else {
                        CatLog.d("[BIP]", "Fail to insert apn params into db");
                    }
                } else {
                    CatLog.d("[BIP]", "BM-setApnParams: update one record");
                    mContext.getContentResolver().update(uri, values, selection, null);
                }

                cursor.close();
            }
            // cursor.close();
        }

        CatLog.d("[BIP]", "BM-setApnParams: exit");
    }

    public int getChannelId() {
        CatLog.d("[BIP]", "BM-getChannelId: channel id is " + mChannelId);
        return mChannelId;
    }

    public int getFreeChannelId(){
        return mBipChannelManager.getFreeChannelId();
    }

    public void openChannelCompleted(int ret, Channel lChannel){
        CatLog.d("[BIP]", "BM-openChannelCompleted: ret: " + ret);

        if(ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
            mCurrentCmd.mBufferSize = mBufferSize;
        }      
        if(ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
            mChannelStatus = BipUtils.CHANNEL_STATUS_OPEN;
            mBipChannelManager.addChannel(mChannelId, lChannel);                                
        } else {
            mBipChannelManager.releaseChannelId(mChannelId,BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE);
            mChannelStatus = BipUtils.CHANNEL_STATUS_ERROR;
        }    
        mCurrentCmd.mChannelStatusData = lChannel.mChannelStatusData;

        if(true == mIsOpenInProgress && false == isConnMgrIntentTimeout) {
            mIsOpenInProgress = false;
            Message response = mHandler.obtainMessage(CatService.MSG_ID_OPEN_CHANNEL_DONE, ret, 0, mCurrentCmd);
            response.arg1 = ret;
            response.obj = mCurrentCmd;
            mHandler.sendMessage(response);
        }
    }

    public BipChannelManager getBipChannelManager(){
        return mBipChannelManager;
    }
    protected class ConnectivityChangeThread implements Runnable 
    {
        Intent intent;

        ConnectivityChangeThread(Intent in)
        {
            CatLog.d("[BIP]", "ConnectivityChangeThread Init");
            intent = in;
        }

        @Override
        public void run() 
        {
            CatLog.d("[BIP]", "ConnectivityChangeThread Enter");
            CatLog.d("[BIP]", "Connectivity changed");
            int ret = ErrorValue.NO_ERROR;
            Message response = null;

            NetworkInfo info = (NetworkInfo)intent.getExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            if (info == null) {
                return;
            }

            int type = info.getType();
            NetworkInfo.State state = info.getState();
            CatLog.d("[BIP]", "network type is " + type);
            CatLog.d("[BIP]", "network state is " + state);

            if (type == ConnectivityManager.TYPE_MOBILE_SUPL) {
                if (state == NetworkInfo.State.CONNECTED) {
                    if(true == mIsOpenInProgress){
                    if (requestRouteToHost() == false) {
                            CatLog.e("[BIP]", "Fail - requestRouteToHost");
                        ret = ErrorValue.NETWORK_CURRENTLY_UNABLE_TO_PROCESS_COMMAND;
                    }
                    ret = establishLink();
                        if (ret != ErrorValue.WAIT_OPEN_COMPLETED) {                    
                            if (ret == ErrorValue.NO_ERROR || ret == ErrorValue.COMMAND_PERFORMED_WITH_MODIFICATION) {
                                CatLog.d("[BIP]", "channel is activated");       
                                updateCurrentChannelStatus(ChannelStatus.CHANNEL_STATUS_LINK);
                            } else {
                                CatLog.d("[BIP]", "channel is un-activated");
                                updateCurrentChannelStatus(ChannelStatus.CHANNEL_STATUS_NO_LINK);
                            }
                            mIsOpenInProgress = false;
                            response = mHandler.obtainMessage(CatService.MSG_ID_OPEN_CHANNEL_DONE, ret, 0, mCurrentCmd);
                            mHandler.sendMessage(response);
                        }
                    } else {
                        CatLog.e("[BIP]", "Error in channel state.");
                        for(int i=1; i <= BipChannelManager.MAXCHANNELID; i++){
                            Channel channel = mBipChannelManager.getChannel(i);
                            if(null != channel)
                                CatLog.e("[BIP]", ">cid:"+i+",protocolType:"+channel.mProtocolType);
                        }
                    }
                } else if (state == NetworkInfo.State.DISCONNECTED) {
                    CatLog.d("[BIP]", "network state - disconnected");

                    if (true == mIsOpenInProgress && mChannelStatus != BipUtils.CHANNEL_STATUS_OPEN) {
                        Channel channel = mBipChannelManager.getChannel(mChannelId);
                        ret = ErrorValue.NETWORK_CURRENTLY_UNABLE_TO_PROCESS_COMMAND;

                        if(null != channel) {
                            channel.closeChannel();
                            mBipChannelManager.removeChannel(mChannelId);                            
                        } else {
                            mBipChannelManager.releaseChannelId(mChannelId, mTransportProtocol.protocolType);
                        }
                        
                        mChannelStatus = BipUtils.CHANNEL_STATUS_CLOSE;
                        mCurrentCmd.mChannelStatusData.mChannelStatus = ChannelStatus.CHANNEL_STATUS_NO_LINK;
                        mIsOpenInProgress = false;
                        response = mHandler.obtainMessage(CatService.MSG_ID_OPEN_CHANNEL_DONE, ret, 0, mCurrentCmd);
                        mHandler.sendMessage(response);
                    } else {
                        int i = 0;                                          
                        ArrayList<Byte> alByte = new ArrayList<Byte>();
                        byte[] additionalInfo = null;
                        CatLog.d("[BIP]", "this is a drop link");
                        mChannelStatus = BipUtils.CHANNEL_STATUS_CLOSE;
                        mCurrentCmd.mChannelStatusData.mChannelStatus = ChannelStatus.CHANNEL_STATUS_NO_LINK;

                        CatResponseMessage resMsg = new CatResponseMessage(CatService.EVENT_LIST_ELEMENT_CHANNEL_STATUS);

                        for(i = 1; i<=BipChannelManager.MAXCHANNELID; i++) {
                            if(true == mBipChannelManager.isChannelIdOccupied(i)) {
                                try {
                                Channel channel = mBipChannelManager.getChannel(i);                                  
                                    CatLog.d("[BIP]", "channel protocolType:"+channel.mProtocolType);
                                    if(BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == channel.mProtocolType || 
                                        BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == channel.mProtocolType){
                                    channel.closeChannel();
                                    mBipChannelManager.removeChannel(i);
                                    alByte.add((byte)0xB8);//additionalInfo[firstIdx] = (byte) 0xB8; // Channel status
                                    alByte.add((byte)0x02);//additionalInfo[firstIdx+1] = 0x02;
                                    alByte.add((byte)(channel.mChannelId | ChannelStatus.CHANNEL_STATUS_NO_LINK));//additionalInfo[firstIdx+2] = (byte) (channel.mChannelId | ChannelStatus.CHANNEL_STATUS_NO_LINK);
                                    alByte.add((byte)ChannelStatus.CHANNEL_STATUS_INFO_LINK_DROPED);//additionalInfo[firstIdx+3] = ChannelStatus.CHANNEL_STATUS_INFO_LINK_DROPED;
                                }
                                }catch (NullPointerException ne){
                                    CatLog.e("[BIP]", "NE,channel null");
                                    ne.printStackTrace();                             
                                }
                            }
                        }
                        if(alByte.size() > 0) {
                            additionalInfo = new byte[alByte.size()];
                            for(i = 0; i < additionalInfo.length; i++)
                                additionalInfo[i] = alByte.get(i);

                            resMsg.setSourceId(0x82);
                            resMsg.setDestinationId(0x81);
                            resMsg.setAdditionalInfo(additionalInfo);
                            resMsg.setOneShot(false);
                            CatLog.d("[BIP]", "onEventDownload: for channel status");
                        ((CatService)mHandler).onEventDownload(resMsg);
                        } else {
                            CatLog.d("[BIP]", "onEventDownload: No client channels are opened.");                        
                        }
                    }
                }
            }
        }
    }

    private BroadcastReceiver mNetworkConnReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION_IMMEDIATE)
                    && ((mIsOpenInProgress == true && isConnMgrIntentTimeout == false) || (true == mBipChannelManager.isClientChannelOpened()))) {//if apn state == APN_REQUEST_STARTED & not timeout occured
                CatLog.d("[BIP]", "Connectivity changed onReceive Enter");
                mHandler.removeMessages(MSG_ID_CONN_MGR_TIMEOUT);

                Thread rt = new Thread(new ConnectivityChangeThread(intent));
                rt.start();
                CatLog.d("[BIP]", "Connectivity changed onReceive Leave");
            }
        }
    };

    public void setConnMgrTimeoutFlag(boolean flag) {
        isConnMgrIntentTimeout = flag;
    }
    public void setOpenInProgressFlag(boolean flag){
        mIsOpenInProgress = flag;
    }
    private class RecvDataRunnable implements Runnable {
        int requestDataSize;
        ReceiveDataResult result;
        CatCmdMessage cmdMsg;
        Message response;
      
        public RecvDataRunnable(int size, ReceiveDataResult result, CatCmdMessage cmdMsg, Message response) {
            this.requestDataSize = size;
            this.result = result;
            this.cmdMsg = cmdMsg;
            this.response = response;
        }

        public void run() {
            Channel lChannel = null;
            int errCode = ErrorValue.NO_ERROR;
            
            CatLog.d("[BIP]", "BM-receiveData: start to receive data");
            lChannel = mBipChannelManager.getChannel(cmdMsg.mReceiveDataCid);
            if(null == lChannel)
                errCode = ErrorValue.BIP_ERROR;
            else
                errCode = lChannel.receiveData(requestDataSize, result);

            cmdMsg.mChannelData = result.buffer;
            cmdMsg.mRemainingDataLength = result.remainingCount;
            response.arg1 = errCode;
            response.obj = cmdMsg;
            mHandler.sendMessage(response);
            CatLog.d("[BIP]", "BM-receiveData: end to receive data. Result code = " + errCode);
        }
    }
}

class ReceiveDataResult {
    public byte[] buffer = null;
    public int requestCount = 0;
    public int remainingCount = 0;
}
