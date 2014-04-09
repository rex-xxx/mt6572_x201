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

import android.os.Handler;
import android.os.Message;

import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.IccUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

/**
 * Class used for queuing raw ril messages, decoding them into CommanParams
 * objects and sending the result back to the CAT Service.
 */
class RilMessageDecoder extends StateMachine {

    // constants
    private static final int CMD_START = 1;
    private static final int CMD_PARAMS_READY = 2;

    // members
    // private static RilMessageDecoder sInstance = null;
    private CommandParamsFactory mCmdParamsFactory = null;
    private RilMessage mCurrentRilMessage = null;
    private Handler mCaller = null;

    // Add by Huibin Mao Mtk80229
    // ICS Migration start
    private static RilMessageDecoder sInstanceSim1 = null; // mtk02374 GEMINI
    private static RilMessageDecoder sInstanceSim2 = null;
    //MTK-START [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card
    private static RilMessageDecoder sInstanceSim3 = null;
    private static RilMessageDecoder sInstanceSim4 = null;
    //MTK-END [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card

    /**
     * SIM ID for GEMINI
     */
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;
    // ICS Migration end
    //MTK-START [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card
    public static final int GEMINI_SIM_3 = 2;
    public static final int GEMINI_SIM_4 = 3;
    //MTK-END [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card

    // States
    private StateStart mStateStart = new StateStart();
    private StateCmdParamsReady mStateCmdParamsReady = new StateCmdParamsReady();

    /**
     * Get the singleton instance, constructing if necessary.
     * 
     * @param caller
     * @param fh
     * @return RilMesssageDecoder
     */
    public static synchronized RilMessageDecoder getInstance(Handler caller, IccFileHandler fh,
            int simId) {
        RilMessageDecoder tempInstance = null;

        if ((GEMINI_SIM_1 == simId && sInstanceSim1 == null) 
	    || (GEMINI_SIM_2 == simId && sInstanceSim2 == null)
	    || (GEMINI_SIM_3 == simId && sInstanceSim3 == null)
	    || (GEMINI_SIM_4 == simId && sInstanceSim4 == null)) {
            tempInstance = new RilMessageDecoder(caller, fh);
            tempInstance.start();
        }

//MTK-START [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card
        if(GEMINI_SIM_1 == simId) {
            sInstanceSim1 = tempInstance;
            return sInstanceSim1;
        } else if(GEMINI_SIM_2 == simId) {
            sInstanceSim2 = tempInstance;
            return sInstanceSim2;
        } else if(GEMINI_SIM_3 == simId) {
            sInstanceSim3 = tempInstance;
            return sInstanceSim3;
        } else { //GEMINI_SIM_4 == simId
            sInstanceSim4 = tempInstance;
            return sInstanceSim4;
        }
//MTK-END [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card
    }

    /**
     * Start decoding the message parameters, when complete
     * MSG_ID_RIL_MSG_DECODED will be returned to caller.
     * 
     * @param rilMsg
     */
    public void sendStartDecodingMessageParams(RilMessage rilMsg) {
        Message msg = obtainMessage(CMD_START);
        msg.obj = rilMsg;
        sendMessage(msg);
    }

    /**
     * The command parameters have been decoded.
     * 
     * @param resCode
     * @param cmdParams
     */
    public void sendMsgParamsDecoded(ResultCode resCode, CommandParams cmdParams) {
        Message msg = obtainMessage(RilMessageDecoder.CMD_PARAMS_READY);
        msg.arg1 = resCode.value();
        msg.obj = cmdParams;
        sendMessage(msg);
    }

    private void sendCmdForExecution(RilMessage rilMsg) {
        Message msg = mCaller.obtainMessage(CatService.MSG_ID_RIL_MSG_DECODED,
                new RilMessage(rilMsg));
        msg.sendToTarget();
    }

    private RilMessageDecoder(Handler caller, IccFileHandler fh) {
        super("RilMessageDecoder");

        addState(mStateStart);
        addState(mStateCmdParamsReady);
        setInitialState(mStateStart);

        mCaller = caller;

        // Add by Huibin Mao Mtk80229
        // ICS Migration start
        // mCmdParamsFactory = CommandParamsFactory.getInstance(this, fh);
        CatLog.d(this, "mCaller is " + mCaller.getClass().getName());
        mCmdParamsFactory = CommandParamsFactory.getInstance(this, fh, ((CatService) mCaller)
                .getContext());
        // ICS Migration end
    }

    private class StateStart extends State {
        @Override
        public boolean processMessage(Message msg) {
            if (msg.what == CMD_START) {
                if (decodeMessageParams((RilMessage) msg.obj)) {
                    transitionTo(mStateCmdParamsReady);
                }
            } else {
                CatLog.d(this, "StateStart unexpected expecting START=" +
                        CMD_START + " got " + msg.what);
            }
            return true;
        }
    }

    private class StateCmdParamsReady extends State {
        @Override
        public boolean processMessage(Message msg) {
            if (msg.what == CMD_PARAMS_READY) {
                mCurrentRilMessage.mResCode = ResultCode.fromInt(msg.arg1);
                mCurrentRilMessage.mData = msg.obj;
                sendCmdForExecution(mCurrentRilMessage);
                transitionTo(mStateStart);
            } else {
                CatLog.d(this, "StateCmdParamsReady expecting CMD_PARAMS_READY="
                        + CMD_PARAMS_READY + " got " + msg.what);
                deferMessage(msg);
            }
            return true;
        }
    }

    private boolean decodeMessageParams(RilMessage rilMsg) {
        boolean decodingStarted;

        mCurrentRilMessage = rilMsg;
        switch (rilMsg.mId) {
            case CatService.MSG_ID_SESSION_END:
            case CatService.MSG_ID_CALL_SETUP:
                mCurrentRilMessage.mResCode = ResultCode.OK;
                sendCmdForExecution(mCurrentRilMessage);
                decodingStarted = false;
                break;
            case CatService.MSG_ID_PROACTIVE_COMMAND:
            case CatService.MSG_ID_EVENT_NOTIFY:
            case CatService.MSG_ID_REFRESH:
                byte[] rawData = null;
                try {
                    rawData = IccUtils.hexStringToBytes((String) rilMsg.mData);
                } catch (Exception e) {
                    // zombie messages are dropped
                    CatLog.d(this, "decodeMessageParams dropping zombie messages");
                    decodingStarted = false;
                    break;
                }
                try {
                    // Start asynch parsing of the command parameters.
                    mCmdParamsFactory.make(BerTlv.decode(rawData));
                    decodingStarted = true;
                } catch (ResultException e) {
                    // send to Service for proper RIL communication.
                    // Add by Huibin Mao Mtk80229
                    // ICS Migration start
                    mCurrentRilMessage.mId = CatService.MSG_ID_SESSION_END;
                    // ICS Migration end
                    mCurrentRilMessage.mResCode = e.result();
                    sendCmdForExecution(mCurrentRilMessage);
                    decodingStarted = false;
                }
                break;
            default:
                decodingStarted = false;
                break;
        }
        return decodingStarted;
    }
}
