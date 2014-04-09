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

package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

import com.mediatek.common.telephony.OtherAddress;
import com.mediatek.common.telephony.TransportProtocol;
import com.mediatek.common.telephony.BipUtils;
import com.mediatek.common.telephony.ChannelStatus;
import com.mediatek.common.telephony.BearerDesc;

/**
 * Class used to pass CAT messages from telephony to application. Application
 * should call getXXX() to get commands's specific values.
 */
public class CatCmdMessage implements Parcelable {
    // members
    CommandDetails mCmdDet;
    private TextMessage mTextMsg;
    private Menu mMenu;
    private Input mInput;
    private BrowserSettings mBrowserSettings = null;
    private ToneSettings mToneSettings = null;
    private CallSettings mCallSettings = null;

    // Add by Huibin Mao MTK80229
    // ICS Migration start
    // parameters for BIP
    public BearerDesc mBearerDesc = null;
    public int mBufferSize = 0;
    public OtherAddress mLocalAddress = null;
    public TransportProtocol mTransportProtocol = null;
    public OtherAddress mDataDestinationAddress = null;

    public String mApn = null;
    public String mLogin = null;
    public String mPwd = null;

    public int mChannelDataLength = 0;
    public int mRemainingDataLength = 0;
    public byte[] mChannelData = null;

    public ChannelStatus mChannelStatusData = null;

    public int mCloseCid = 0;
    public int mSendDataCid = 0;
    public int mReceiveDataCid = 0;
    public boolean mCloseBackToTcpListen = false;
    public int mSendMode = 0;
    public List<ChannelStatus> mChannelStatusList = null;
    // ICS Migration end

    /*
     * Container for Launch Browser command settings.
     */
    public class BrowserSettings {
        public String url;
        public LaunchBrowserMode mode;
    }

    /*
     * Container for Call Setup command settings.
     */
    public class CallSettings {
        public TextMessage confirmMsg;
        public TextMessage callMsg;
    }

    CatCmdMessage(CommandParams cmdParams) {
        mCmdDet = cmdParams.cmdDet;
        switch (getCmdType()) {
            case SET_UP_MENU:
            case SELECT_ITEM:
                mMenu = ((SelectItemParams) cmdParams).menu;
                break;
            case DISPLAY_TEXT:
            case SET_UP_IDLE_MODE_TEXT:
            case SEND_DTMF:
            case SEND_SMS:
            case SEND_SS:
            case SEND_USSD:
                // Add by Huibin Mao MTK80229
                // ICS Migration start
            case REFRESH:
                // ICS Migration end
                mTextMsg = ((DisplayTextParams) cmdParams).textMsg;
                break;
            case GET_INPUT:
            case GET_INKEY:
                mInput = ((GetInputParams) cmdParams).input;
                break;
            case LAUNCH_BROWSER:
                mTextMsg = ((LaunchBrowserParams) cmdParams).confirmMsg;
                mBrowserSettings = new BrowserSettings();
                mBrowserSettings.url = ((LaunchBrowserParams) cmdParams).url;
                mBrowserSettings.mode = ((LaunchBrowserParams) cmdParams).mode;
                break;
            case PLAY_TONE:
                PlayToneParams params = (PlayToneParams) cmdParams;
                mToneSettings = params.settings;
                mTextMsg = params.textMsg;
                break;
            case SET_UP_CALL:
                mCallSettings = new CallSettings();
                mCallSettings.confirmMsg = ((CallSetupParams) cmdParams).confirmMsg;
                mCallSettings.callMsg = ((CallSetupParams) cmdParams).callMsg;
                break;
            // Add by Huibin Mao MTK80229
            // ICS Migration start
            case OPEN_CHANNEL:
                mBearerDesc = ((OpenChannelParams) cmdParams).bearerDesc;
                mBufferSize = ((OpenChannelParams) cmdParams).bufferSize;
                mLocalAddress = ((OpenChannelParams) cmdParams).localAddress;
                mTransportProtocol = ((OpenChannelParams) cmdParams).transportProtocol;
                mDataDestinationAddress = ((OpenChannelParams) cmdParams).dataDestinationAddress;
                mTextMsg = ((OpenChannelParams) cmdParams).textMsg;

                if (mBearerDesc != null) {
                    if (mBearerDesc.bearerType == BipUtils.BEARER_TYPE_GPRS) {
                        mApn = ((OpenChannelParams) cmdParams).gprsParams.accessPointName;
                        mLogin = ((OpenChannelParams) cmdParams).gprsParams.userLogin;
                        mPwd = ((OpenChannelParams) cmdParams).gprsParams.userPwd;
                    }
                } else {
                    CatLog.d("[BIP]", "Invalid BearerDesc object");
                }
                break;
            case CLOSE_CHANNEL:
                mTextMsg = ((CloseChannelParams) cmdParams).textMsg;
                mCloseCid = ((CloseChannelParams) cmdParams).mCloseCid;
                mCloseBackToTcpListen = ((CloseChannelParams) cmdParams).mBackToTcpListen;
                break;
            case RECEIVE_DATA:
                mTextMsg = ((ReceiveDataParams) cmdParams).textMsg;
                mChannelDataLength = ((ReceiveDataParams) cmdParams).channelDataLength;
                mReceiveDataCid = ((ReceiveDataParams) cmdParams).mReceiveDataCid;                
                break;
            case SEND_DATA:
                mTextMsg = ((SendDataParams) cmdParams).textMsg;
                mChannelData = ((SendDataParams) cmdParams).channelData;
                mSendDataCid = ((SendDataParams) cmdParams).mSendDataCid;
                mSendMode = ((SendDataParams) cmdParams).mSendMode;
                break;
            case GET_CHANNEL_STATUS:
                mTextMsg = ((GetChannelStatusParams) cmdParams).textMsg;
                break;
            // ICS Migration end
        }
    }

    public CatCmdMessage(Parcel in) {
        mCmdDet = in.readParcelable(null);
        mTextMsg = in.readParcelable(null);
        mMenu = in.readParcelable(null);
        mInput = in.readParcelable(null);
        switch (getCmdType()) {
            case LAUNCH_BROWSER:
                mBrowserSettings = new BrowserSettings();
                mBrowserSettings.url = in.readString();
                mBrowserSettings.mode = LaunchBrowserMode.values()[in.readInt()];
                break;
            case PLAY_TONE:
                mToneSettings = in.readParcelable(null);
                break;
            case SET_UP_CALL:
                mCallSettings = new CallSettings();
                mCallSettings.confirmMsg = in.readParcelable(null);
                mCallSettings.callMsg = in.readParcelable(null);
                break;
            // Add by Huibin Mao MTK80229
            // ICS Migration start
            case OPEN_CHANNEL:
                mBearerDesc = in.readParcelable(null);
                break;
            // ICS Migration end
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mCmdDet, 0);
        dest.writeParcelable(mTextMsg, 0);
        dest.writeParcelable(mMenu, 0);
        dest.writeParcelable(mInput, 0);
        switch (getCmdType()) {
            case LAUNCH_BROWSER:
                dest.writeString(mBrowserSettings.url);
                dest.writeInt(mBrowserSettings.mode.ordinal());
                break;
            case PLAY_TONE:
                dest.writeParcelable(mToneSettings, 0);
                break;
            case SET_UP_CALL:
                dest.writeParcelable(mCallSettings.confirmMsg, 0);
                dest.writeParcelable(mCallSettings.callMsg, 0);
                break;
            // Add by Huibin Mao MTK80229
            // ICS Migration start
            case OPEN_CHANNEL:
                dest.writeParcelable(mBearerDesc, 0);
                break;
            // ICS Migration end
        }
    }

    public static final Parcelable.Creator<CatCmdMessage> CREATOR = new Parcelable.Creator<CatCmdMessage>() {
        public CatCmdMessage createFromParcel(Parcel in) {
            return new CatCmdMessage(in);
        }

        public CatCmdMessage[] newArray(int size) {
            return new CatCmdMessage[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    // Add by Huibin Mao MTK80229
    // ICS Migration start
    /* external API to be used by application */
    public int getCmdQualifier() {
        return mCmdDet.commandQualifier;
    }

    // ICS Migration end

    public AppInterface.CommandType getCmdType() {
        return AppInterface.CommandType.fromInt(mCmdDet.typeOfCommand);
    }

    public Menu getMenu() {
        return mMenu;
    }

    public Input geInput() {
        return mInput;
    }

    public TextMessage geTextMessage() {
        return mTextMsg;
    }

    public BrowserSettings getBrowserSettings() {
        return mBrowserSettings;
    }

    public ToneSettings getToneSettings() {
        return mToneSettings;
    }

    public CallSettings getCallSettings() {
        return mCallSettings;
    }

    public BearerDesc getBearerDesc() {
        return mBearerDesc;
    }
}
