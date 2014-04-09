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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccFileHandler;

import java.util.Iterator;
import java.util.List;

import com.mediatek.common.telephony.OtherAddress;
import com.mediatek.common.telephony.TransportProtocol;
import com.mediatek.common.telephony.BipUtils;
import com.mediatek.common.telephony.BearerDesc;

/**
 * Factory class, used for decoding raw byte arrays, received from baseband,
 * into a CommandParams object.
 *
 */
class CommandParamsFactory extends Handler {
    private static CommandParamsFactory sInstance = null;
    private IconLoader mIconLoader;
    private CommandParams mCmdParams = null;
    private int mIconLoadState = LOAD_NO_ICON;
    private RilMessageDecoder mCaller = null;
    // Add by Huibin Mao Mtk80229
    // ICS Migration start
    private Context mContext;
    // used to mark the index of tlv object in a tlv list
    int tlvIndex = -1;
    // ICS Migration end

    // constants
    static final int MSG_ID_LOAD_ICON_DONE = 1;

    // loading icons state parameters.
    static final int LOAD_NO_ICON = 0;
    static final int LOAD_SINGLE_ICON = 1;
    static final int LOAD_MULTI_ICONS = 2;

    // Command Qualifier values for refresh command
    static final int REFRESH_NAA_INIT_AND_FULL_FILE_CHANGE = 0x00;
    static final int REFRESH_NAA_INIT_AND_FILE_CHANGE = 0x02;
    static final int REFRESH_NAA_INIT = 0x03;
    static final int REFRESH_UICC_RESET = 0x04;

    // Command Qualifier values for PLI command
    static final int DTTZ_SETTING = 0x03;
    static final int LANGUAGE_SETTING = 0x04;

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder caller,
            IccFileHandler fh) {
        if (sInstance != null) {
            return sInstance;
        }
        if (fh != null) {
            return new CommandParamsFactory(caller, fh);
        }
        return null;
    }

    // Add by Huibin Mao Mtk80229
    // ICS Migration start

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder caller,
            IccFileHandler fh, Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        if (fh != null && context != null) {
            return new CommandParamsFactory(caller, fh, context);
        }

        return null;
    }

    private CommandParamsFactory(RilMessageDecoder caller, IccFileHandler fh, Context context) {
        mCaller = caller;
        mIconLoader = IconLoader.getInstance(this, fh);
        mContext = context;
    }

    // ICS Migration end

    private CommandParamsFactory(RilMessageDecoder caller, IccFileHandler fh) {
        mCaller = caller;
        mIconLoader = IconLoader.getInstance(this, fh);
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> ctlvs) 
        throws ResultException {
    
        CommandDetails cmdDet = null;

        if (ctlvs != null) {
            // Search for the Command Details object.
            ComprehensionTlv ctlvCmdDet = searchForTag(
                    ComprehensionTlvTag.COMMAND_DETAILS, ctlvs);
            if (ctlvCmdDet != null) {
                try {
                    cmdDet = ValueParser.retrieveCommandDetails(ctlvCmdDet);
                } catch (ResultException e) {
                    CatLog.d(this, "Failed to procees command details");
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);                    
                }
            }
        }
        return cmdDet;
    }

    void make(BerTlv berTlv) {
        if (berTlv == null) {
            return;
        }
        // reset global state parameters.
        mCmdParams = null;
        mIconLoadState = LOAD_NO_ICON;
        // only proactive command messages are processed.
        if (berTlv.getTag() != BerTlv.BER_PROACTIVE_COMMAND_TAG) {
            CatLog.e(this, "CPF-make: Ununderstood proactive command tag");
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }
        boolean cmdPending = false;
        List<ComprehensionTlv> ctlvs = berTlv.getComprehensionTlvs();
        // process command dtails from the tlv list.
        CommandDetails cmdDet = null;
        try {
            cmdDet = processCommandDetails(ctlvs);
        } catch (ResultException e){
            CatLog.e(this,"CPF-make: Except to procees command details : " + e.result());
            sendCmdParams(e.result());
            return;
        }
        if (cmdDet == null) {
            CatLog.e(this, "CPF-make: No CommandDetails object");
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }

        // extract command type enumeration from the raw value stored inside
        // the Command Details object.
        AppInterface.CommandType cmdType = AppInterface.CommandType
                .fromInt(cmdDet.typeOfCommand);
        if (cmdType == null) {
            CatLog.d(this, "CPF-make: Command type can't be found");
            // Different from 2.3.5
            // This PROACTIVE COMMAND is presently not handled. Hence set
            // result code as BEYOND_TERMINAL_CAPABILITY in TR.
            mCmdParams = new CommandParams(cmdDet);
            // sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            return;
        }

        try {
            switch (cmdType) {
                case SET_UP_MENU:
                    cmdPending = processSelectItem(cmdDet, ctlvs);
                    break;
                case SELECT_ITEM:
                    cmdPending = processSelectItem(cmdDet, ctlvs);
                    break;
                case DISPLAY_TEXT:
                    cmdPending = processDisplayText(cmdDet, ctlvs);
                    break;
                case SET_UP_IDLE_MODE_TEXT:
                    cmdPending = processSetUpIdleModeText(cmdDet, ctlvs);
                    break;
                case GET_INKEY:
                    cmdPending = processGetInkey(cmdDet, ctlvs);
                    break;
                case GET_INPUT:
                    cmdPending = processGetInput(cmdDet, ctlvs);
                    break;
                case SEND_DTMF:
                case SEND_SMS:
                case SEND_SS:
                case SEND_USSD:
                    cmdPending = processEventNotify(cmdDet, ctlvs);
                    break;
                case SET_UP_CALL:
                    cmdPending = processSetupCall(cmdDet, ctlvs);
                    break;
                case REFRESH:
                    processRefresh(cmdDet, ctlvs);
                    cmdPending = false;
                    break;
                case LAUNCH_BROWSER:
                    cmdPending = processLaunchBrowser(cmdDet, ctlvs);
                    break;
                case PLAY_TONE:
                    cmdPending = processPlayTone(cmdDet, ctlvs);
                    break;
                // Add by Huibin Mao Mtk80229
                // ICS Migration start
                case SET_UP_EVENT_LIST:
                    cmdPending = processSetUpEventList(cmdDet, ctlvs);
                    break;
                /*
                 * case PROVIDE_LOCAL_INFORMATION: mCmdParams = new
                 * CommandParams(cmdDet); StkLog.d(this,
                 * "process ProvideLocalInformation"); break;
                 */
                case OPEN_CHANNEL:
                    cmdPending = processOpenChannel(cmdDet, ctlvs);
                    CatLog.d(this, "process OpenChannel");
                    break;
                case CLOSE_CHANNEL:
                    cmdPending = processCloseChannel(cmdDet, ctlvs);
                    CatLog.d(this, "process CloseChannel");
                    break;
                case SEND_DATA:
                    cmdPending = processSendData(cmdDet, ctlvs);
                    CatLog.d(this, "process SendData");
                    break;
                case RECEIVE_DATA:
                    cmdPending = processReceiveData(cmdDet, ctlvs);
                    CatLog.d(this, "process ReceiveData");
                    break;
                case GET_CHANNEL_STATUS:
                    cmdPending = processGetChannelStatus(cmdDet, ctlvs);
                    CatLog.d(this, "process GetChannelStatus");
                    break;
                // ICS Migration end

                case PROVIDE_LOCAL_INFORMATION:
                    cmdPending = processProvideLocalInfo(cmdDet, ctlvs);
                    CatLog.d(this, "process ProvideLocalInformation");
                    break;
                default:
                    // unsupported proactive commands
                    mCmdParams = new CommandParams(cmdDet);
                    CatLog.d(this, "CPF-make: default case");
                    // sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                    sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                    return;
            }
        } catch (ResultException e) {
            CatLog.d(this, "throw ResultException: " + e.result());
            mCmdParams = new CommandParams(cmdDet);
            sendCmdParams(e.result());
            return;
        }
        if (!cmdPending) {
            sendCmdParams(ResultCode.OK);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ID_LOAD_ICON_DONE:
                sendCmdParams(setIcons(msg.obj));
                break;
        }
    }

    private ResultCode setIcons(Object data) {
        Bitmap[] icons = null;
        int iconIndex = 0;

        if (data == null) {
            return ResultCode.PRFRMD_ICON_NOT_DISPLAYED;
        }
        switch (mIconLoadState) {
            case LOAD_SINGLE_ICON:
                mCmdParams.setIcon((Bitmap) data);
                break;
            case LOAD_MULTI_ICONS:
                icons = (Bitmap[]) data;
                // set each item icon.
                for (Bitmap icon : icons) {
                    mCmdParams.setIcon(icon);
                }
                break;
        }
        return ResultCode.OK;
    }

    private void sendCmdParams(ResultCode resCode) {
        mCaller.sendMsgParamsDecoded(resCode, mCmdParams);
    }

    /**
     * Search for a COMPREHENSION-TLV object with the given tag from a list
     * 
     * @param tag A tag to search for
     * @param ctlvs List of ComprehensionTlv objects used to search in
     * @return A ComprehensionTlv object that has the tag value of {@code tag}.
     *         If no object is found with the tag, null is returned.
     */
    private ComprehensionTlv searchForTag(ComprehensionTlvTag tag,
            List<ComprehensionTlv> ctlvs) {
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        return searchForNextTag(tag, iter);
    }

    /**
     * Search for the next COMPREHENSION-TLV object with the given tag from a
     * list iterated by {@code iter}. {@code iter} points to the object next to
     * the found object when this method returns. Used for searching the same
     * list for similar tags, usually item id.
     * 
     * @param tag A tag to search for
     * @param iter Iterator for ComprehensionTlv objects used for search
     * @return A ComprehensionTlv object that has the tag value of {@code tag}.
     *         If no object is found with the tag, null is returned.
     */
    private ComprehensionTlv searchForNextTag(ComprehensionTlvTag tag,
            Iterator<ComprehensionTlv> iter) {
        int tagValue = tag.value();
        while (iter.hasNext()) {
            ComprehensionTlv ctlv = iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }
        return null;
    }

    // Add by Huibin Mao Mtk80229
    // ICS Migration start
    private void resetTlvIndex() {
        tlvIndex = -1;
    }

    /**
     * Search for the next COMPREHENSION-TLV object with the given tag from a
     * list iterated by {@code iter}. {@code iter} points to the object next to
     * the found object when this method returns. Used for searching the same
     * list for similar tags, usually item id. At the same time, this method
     * will update a index to mark the position of the tlv object in the
     * comprehension- tlv.
     * 
     * @param tag A tag to search for
     * @param iter Iterator for ComprehensionTlv objects used for search
     * @return A ComprehensionTlv object that has the tag value of {@code tag}.
     *         If no object is found with the tag, null is returned.
     */
    private ComprehensionTlv searchForNextTagAndIndex(ComprehensionTlvTag tag,
            Iterator<ComprehensionTlv> iter) {
        if (tag == null || iter == null) {
            CatLog.d(this, "CPF-searchForNextTagAndIndex: Invalid params");
            return null;
        }

        int tagValue = tag.value();

        while (iter.hasNext()) {
            ++tlvIndex;
            ComprehensionTlv ctlv = iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }

        // tlvIndex = -1;
        return null;
    }

    /**
     * Search for a COMPREHENSION-TLV object with the given tag from a list and
     * provide the index of searched tlv object
     * 
     * @param tag A tag to search for
     * @param ctlvs List of ComprehensionTlv objects used to search in
     * @return A ComprehensionTlv object that has the tag value of {@code tag}.
     *         If no object is found with the tag, null is returned.
     */
    private ComprehensionTlv searchForTagAndIndex(ComprehensionTlvTag tag,
            List<ComprehensionTlv> ctlvs) {
        // tlvIndex = -1;
        resetTlvIndex();
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        return searchForNextTagAndIndex(tag, iter);
    }

    // ICS Migration end

    /**
     * Processes DISPLAY_TEXT proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processDisplayText(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs)
            throws ResultException {

        CatLog.d(this, "process DisplayText");

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING,
                ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        // If the tlv object doesn't exist or the it is a null object reply
        // with command not understood.
        if (textMsg.text == null) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }

        ctlv = searchForTag(ComprehensionTlvTag.IMMEDIATE_RESPONSE, ctlvs);
        if (ctlv != null) {
            textMsg.responseNeeded = false;
        }
        // parse icon identifier
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            try {
            iconId = ValueParser.retrieveIconId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveIconId ResultException: " + e.result());
            }	
            try {
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
            } catch (NullPointerException ne) {
                CatLog.e(this, "iconId is null.");
            }
        }
        // parse tone duration
        ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv != null) {
            try {
            textMsg.duration = ValueParser.retrieveDuration(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveDuration ResultException: " + e.result());
            }			
        }

        // Parse command qualifier parameters.
        textMsg.isHighPriority = (cmdDet.commandQualifier & 0x01) != 0;
        textMsg.userClear = (cmdDet.commandQualifier & 0x80) != 0;

        mCmdParams = new DisplayTextParams(cmdDet, textMsg);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes SET_UP_IDLE_MODE_TEXT proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processSetUpIdleModeText(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process SetUpIdleModeText");

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING,
                ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        // load icons only when text exist.
        // Add by Huibin Mao Mtk80229
        // ICS Migration start

        // if (textMsg.text != null) {
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        // }
        // ICS Migration end

        mCmdParams = new DisplayTextParams(cmdDet, textMsg);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes GET_INKEY proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processGetInkey(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process GetInkey");

        Input input = new Input();
        IconId iconId = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING,
                ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
        } else {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        // parse icon identifier
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            try {
            iconId = ValueParser.retrieveIconId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveIconId ResultException: " + e.result());
            }				
            // Add by Huibin Mao Mtk80229
            // ICS Migration start
            try {
            input.iconSelfExplanatory = iconId.selfExplanatory;
            } catch (NullPointerException ne) {
                CatLog.e(this, "iconId is null.");
            }			
            // ICS Migration end
        }

        // parse duration
        ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv != null) {
            try {
            input.duration = ValueParser.retrieveDuration(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveDuration ResultException: " + e.result());
            }						
        }

        input.minLen = 1;
        input.maxLen = 1;

        input.digitOnly = (cmdDet.commandQualifier & 0x01) == 0;
        input.ucs2 = (cmdDet.commandQualifier & 0x02) != 0;
        input.yesNo = (cmdDet.commandQualifier & 0x04) != 0;
        input.helpAvailable = (cmdDet.commandQualifier & 0x80) != 0;
        input.echo = true;

        mCmdParams = new GetInputParams(cmdDet, input);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes GET_INPUT proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processGetInput(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process GetInput");

        Input input = new Input();
        IconId iconId = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING,
                ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
        } else {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }

        ctlv = searchForTag(ComprehensionTlvTag.RESPONSE_LENGTH, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                // The maximum input lenght is 239, because the
                // maximum length of proactive command is 255
                input.minLen = rawValue[valueIndex] & 0xff;
                // Add by Huibin Mao Mtk80229
                // ICS Migration start
                if (input.minLen > 239) {
                    input.minLen = 239;
                }
                // ICS Migration end

                input.maxLen = rawValue[valueIndex + 1] & 0xff;
                // Add by Huibin Mao Mtk80229
                // ICS Migration start
                if (input.maxLen > 239) {
                    input.maxLen = 239;
                }
                // ICS Migration end
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } else {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }

        ctlv = searchForTag(ComprehensionTlvTag.DEFAULT_TEXT, ctlvs);
        if (ctlv != null) {
            try{
            input.defaultText = ValueParser.retrieveTextString(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveTextString ResultException: " + e.result());
            }
        }
        // parse icon identifier
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            try {
            iconId = ValueParser.retrieveIconId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveIconId ResultException: " + e.result());
            }
			
            // Add by Huibin Mao Mtk80229
            // ICS Migration start
            try {
            input.iconSelfExplanatory = iconId.selfExplanatory;
            } catch (NullPointerException ne) {
                CatLog.e(this, "iconId is null.");
            }
			
            // ICS Migration end
        }

        input.digitOnly = (cmdDet.commandQualifier & 0x01) == 0;
        input.ucs2 = (cmdDet.commandQualifier & 0x02) != 0;
        input.echo = (cmdDet.commandQualifier & 0x04) == 0;
        input.packed = (cmdDet.commandQualifier & 0x08) != 0;
        input.helpAvailable = (cmdDet.commandQualifier & 0x80) != 0;

        // Add by Huibin Mao Mtk80229
        // ICS Migration start
        /*
         * [20120613,mtk80601,ALPS300161] if ( input.ucs2 == true &&
         * input.digitOnly == false) { //input.minLen = (input.minLen / 2);
         * //input.maxLen = (input.maxLen / 2); // If the min/max input length
         * is 1, we should make sure the // user can input 1 16-bit character
         * input.minLen = (input.minLen == 1) ? 1 : (input.minLen / 2);
         * input.maxLen = (input.maxLen == 1) ? 1 : (input.maxLen / 2); }
         */
        // ICS Migration end

        mCmdParams = new GetInputParams(cmdDet, input);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes REFRESH proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     */
    private boolean processRefresh(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) {

        CatLog.d(this, "process Refresh");
        // Add by Huibin Mao Mtk80229
        // ICS Migration start
        TextMessage textMsg = new TextMessage();
        // ICS Migration end

        // REFRESH proactive command is rerouted by the baseband and handled by
        // the telephony layer. IDLE TEXT should be removed for a REFRESH
        // command
        // with "initialization" or "reset"
        switch (cmdDet.commandQualifier) {
            case REFRESH_NAA_INIT_AND_FULL_FILE_CHANGE:
            case REFRESH_NAA_INIT_AND_FILE_CHANGE:
            case REFRESH_NAA_INIT:
            case REFRESH_UICC_RESET:
                // Add by Huibin Mao Mtk80229
                // ICS Migration start
                textMsg.text = null;
                // ICS Migration end
                mCmdParams = new DisplayTextParams(cmdDet, textMsg);
                break;
        }
        return false;
    }

    /**
     * Processes SELECT_ITEM proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processSelectItem(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process SelectItem");

        Menu menu = new Menu();
        IconId titleIconId = null;
        ItemsIconId itemsIconId = null;
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID,
                ctlvs);
        if (ctlv != null) {
            try{
            menu.title = ValueParser.retrieveAlphaId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveAlphaId ResultException: " + e.result());
            }
            CatLog.d(this, "add AlphaId: " + menu.title);
        }

        while (true) {
            ctlv = searchForNextTag(ComprehensionTlvTag.ITEM, iter);
            if (ctlv != null) {
                Item item = ValueParser.retrieveItem(ctlv);
                CatLog.d(this, "add menu item: " + ((item == null)? "" : item.toString()));
                menu.items.add(item);
            } else {
                break;
            }
        }

        // We must have at least one menu item.
        if (menu.items.size() == 0) {
            CatLog.d(this, "no menu item");
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }

        // Add by Huibin Mao Mtk80229
        // ICS Migration start
        ctlv = searchForTag(ComprehensionTlvTag.NEXT_ACTION_INDICATOR, ctlvs);
        if (ctlv != null) {
            try{
            menu.nextActionIndicator = ValueParser.retrieveNextActionIndicator(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveNextActionIndicator ResultException: " + e.result());
            }
            try{
            if (menu.nextActionIndicator.length != menu.items.size()) {
                CatLog.d(this, "nextActionIndicator.length != number of menu items");
                menu.nextActionIndicator = null;
            }
            } catch (NullPointerException ne) {
                CatLog.e(this, "nextActionIndicator is null.");            
            }
        }
        // ICS Migration end

        ctlv = searchForTag(ComprehensionTlvTag.ITEM_ID, ctlvs);
        if (ctlv != null) {
            // CAT items are listed 1...n while list start at 0, need to
            // subtract one.
            try{
            menu.defaultItem = ValueParser.retrieveItemId(ctlv) - 1;
            } catch (ResultException e) {
                CatLog.e(this, "retrieveItemId ResultException: " + e.result());
            }
            CatLog.d(this, "default item: " + menu.defaultItem);
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            try{
            titleIconId = ValueParser.retrieveIconId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveIconId ResultException: " + e.result());
            }
            try{
            menu.titleIconSelfExplanatory = titleIconId.selfExplanatory;
            } catch (NullPointerException ne) {
                CatLog.e(this, "titleIconId is null.");
            }
        }

        ctlv = searchForTag(ComprehensionTlvTag.ITEM_ICON_ID_LIST, ctlvs);
        if (ctlv != null) {
            mIconLoadState = LOAD_MULTI_ICONS;
            try {
            itemsIconId = ValueParser.retrieveItemsIconId(ctlv);
            } catch (ResultException e){
                CatLog.e(this, "retrieveItemsIconId ResultException: " + e.result());
            }
            try {
            menu.itemsIconSelfExplanatory = itemsIconId.selfExplanatory;
            } catch (NullPointerException ne){
                CatLog.e(this, "itemsIconId is null.");
            }            
        }

        boolean presentTypeSpecified = (cmdDet.commandQualifier & 0x01) != 0;
        if (presentTypeSpecified) {
            if ((cmdDet.commandQualifier & 0x02) == 0) {
                menu.presentationType = PresentationType.DATA_VALUES;
            } else {
                menu.presentationType = PresentationType.NAVIGATION_OPTIONS;
            }
        }
        menu.softKeyPreferred = (cmdDet.commandQualifier & 0x04) != 0;
        menu.helpAvailable = (cmdDet.commandQualifier & 0x80) != 0;

        mCmdParams = new SelectItemParams(cmdDet, menu, titleIconId != null);

        // Load icons data if needed.
        switch (mIconLoadState) {
            case LOAD_NO_ICON:
                return false;
            case LOAD_SINGLE_ICON:
                // Add by Huibin Mao Mtk80229
                // ICS Migration start
                if (titleIconId != null && titleIconId.recordNumber > 0) {
                    mIconLoader.loadIcon(titleIconId.recordNumber, this
                            .obtainMessage(MSG_ID_LOAD_ICON_DONE));
                    break;
                } else {
                    return false;
                }
                // ICS Migration end
            case LOAD_MULTI_ICONS:
                // Add by Huibin Mao Mtk80229
                // ICS Migration start
                if (itemsIconId != null) {
                    int[] recordNumbers = itemsIconId.recordNumbers;
                    // Create a new array for all the icons (title and items).
                    recordNumbers = new int[itemsIconId.recordNumbers.length + 1];
                    if (titleIconId != null) {
                        recordNumbers[0] = titleIconId.recordNumber;
                    }
                    System.arraycopy(itemsIconId.recordNumbers, 0, recordNumbers,
                            1, itemsIconId.recordNumbers.length);
                    mIconLoader.loadIcons(recordNumbers, this
                            .obtainMessage(MSG_ID_LOAD_ICON_DONE));
                    break;
                } else {
                    return false;
                }
                // ICS Migration end
        }
        return true;
    }

    /**
     * Processes EVENT_NOTIFY message from baseband.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     */
    private boolean processEventNotify(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process EventNotify");

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID,
                ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        } else {
            // throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
            textMsg.text = null;
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        textMsg.responseNeeded = false;
        mCmdParams = new DisplayTextParams(cmdDet, textMsg);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes SET_UP_EVENT_LIST proactive command from the SIM card.
     * 
     * @param cmdDet Command Details object retrieved.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     */
    private boolean processSetUpEventList(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {
        //
        // ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.EVENT_LIST,
        // ctlvs);
        // if (ctlv != null) {
        // try {
        // byte[] rawValue = ctlv.getRawValue();
        // int valueIndex = ctlv.getValueIndex();
        // int valueLen = ctlv.getLength();
        //
        // } catch (IndexOutOfBoundsException e) {}
        // }
        // return true;

        CatLog.d(this, "process SetUpEventList");

        byte[] eventList;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.EVENT_LIST, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();

                eventList = new byte[valueLen];
                for (int index = 0; index < valueLen;) {
                    eventList[index] = rawValue[valueIndex];
                    CatLog.d(this, "CPF-processSetUpEventList: eventList[" + index + "] = "
                            + eventList[index]);
                    if (rawValue[valueIndex] == CatService.EVENT_LIST_ELEMENT_IDLE_SCREEN_AVAILABLE) {
                        CatLog.d(this, "CPF-processSetUpEventList: sent intent with idle = true");
                        Intent intent = new Intent(CatService.IDLE_SCREEN_INTENT_NAME);
                        intent.putExtra(CatService.IDLE_SCREEN_ENABLE_KEY, true);
                        mContext.sendBroadcast(intent);
                        // IWindowManager wm =
                        // IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
                        /*
                         * try { wm.setEventDownloadNeeded(true); } catch
                         * (RemoteException e) { CatLog.d(this,
                         * "Exception when set EventDownloadNeeded flag in WindowManager"
                         * ); } catch (NullPointerException e2) { StkLog.d(this,
                         * "wm is null"); }
                         */
                    } else if (rawValue[valueIndex] == CatService.EVENT_LIST_ELEMENT_USER_ACTIVITY) {
                        CatLog.d(this, "CPF-processSetUpEventList: sent intent for user activity");
                        Intent intent = new Intent(CatService.USER_ACTIVITY_INTENT_NAME);
                        intent.putExtra(CatService.USER_ACTIVITY_ENABLE_KEY, true);
                        mContext.sendBroadcast(intent);
                    }
                    index++;
                    valueIndex++;
                }
                mCmdParams = new SetupEventListParams(cmdDet, eventList);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }

        return false;
    }

    /**
     * Processes LAUNCH_BROWSER proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processLaunchBrowser(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process LaunchBrowser");

        TextMessage confirmMsg = new TextMessage();
        IconId iconId = null;
        String url = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.URL, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                if (valueLen > 0) {
                    url = GsmAlphabet.gsm8BitUnpackedToString(rawValue,
                            valueIndex, valueLen);
                } else {
                    url = null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }

        // parse alpha identifier.
        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            confirmMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }
        // parse icon identifier
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            confirmMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        // parse command qualifier value.
        LaunchBrowserMode mode;
        switch (cmdDet.commandQualifier) {
            case 0x00:
            default:
                mode = LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED;
                break;
            case 0x02:
                mode = LaunchBrowserMode.USE_EXISTING_BROWSER;
                break;
            case 0x03:
                mode = LaunchBrowserMode.LAUNCH_NEW_BROWSER;
                break;
        }

        mCmdParams = new LaunchBrowserParams(cmdDet, confirmMsg, url, mode);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes PLAY_TONE proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.t
     * @throws ResultException
     */
    private boolean processPlayTone(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {

        CatLog.d(this, "process PlayTone");

        Tone tone = null;
        TextMessage textMsg = new TextMessage();
        Duration duration = null;
        IconId iconId = null;

        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TONE, ctlvs);
        if (ctlv != null) {
            // Nothing to do for null objects.
            if (ctlv.getLength() > 0) {
                try {
                    byte[] rawValue = ctlv.getRawValue();
                    int valueIndex = ctlv.getValueIndex();
                    int toneVal = rawValue[valueIndex];
                    tone = Tone.fromInt(toneVal);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(
                            ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
        }
        // parse alpha identifier
        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            try {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveAlphaId ResultException: " + e.result());
            }												
        }
        // parse tone duration
        ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv != null) {
            try {
            duration = ValueParser.retrieveDuration(ctlv);
            } catch (ResultException e) {
                CatLog.e(this, "retrieveDuration ResultException: " + e.result());
            }									
        }
        // parse icon identifier
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        boolean vibrate = (cmdDet.commandQualifier & 0x01) != 0x00;

        textMsg.responseNeeded = false;
        mCmdParams = new PlayToneParams(cmdDet, textMsg, tone, duration, vibrate);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    /**
     * Processes SETUP_CALL proactive command from the SIM card.
     * 
     * @param cmdDet Command Details object retrieved from the proactive command
     *            object
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     */
    private boolean processSetupCall(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SetupCall");

        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        ComprehensionTlv ctlv = null;
        // User confirmation phase message.
        TextMessage confirmMsg = new TextMessage();
        // Call set up phase message.
        TextMessage callMsg = new TextMessage();
        IconId confirmIconId = null;
        IconId callIconId = null;

        // The structure of SET UP CALL
        // alpha id -> address -> icon id -> alpha id -> icon id
        // We use the index of alpha id to judge the type of alpha id:
        // confirm or call
        final int addrIndex = getAddrIndex(ctlvs);
        if (-1 == addrIndex) {
            CatLog.d(this, "fail to get ADDRESS data object");
            return false;
        }

        final int alpha1Index = getConfirmationAlphaIdIndex(ctlvs, addrIndex);
        final int alpha2Index = getCallingAlphaIdIndex(ctlvs, addrIndex);

        ctlv = getConfirmationAlphaId(ctlvs, addrIndex);
        if (ctlv != null) {
            confirmMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = getConfirmationIconId(ctlvs, alpha1Index, alpha2Index);
        if (ctlv != null) {
            confirmIconId = ValueParser.retrieveIconId(ctlv);
            confirmMsg.iconSelfExplanatory = confirmIconId.selfExplanatory;
        }

        ctlv = getCallingAlphaId(ctlvs, addrIndex);
        if (ctlv != null) {
            callMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = getCallingIconId(ctlvs, alpha2Index);
        if (ctlv != null) {
            callIconId = ValueParser.retrieveIconId(ctlv);
            callMsg.iconSelfExplanatory = callIconId.selfExplanatory;
        }

        mCmdParams = new CallSetupParams(cmdDet, confirmMsg, callMsg);

        if (confirmIconId != null || callIconId != null) {
            mIconLoadState = LOAD_MULTI_ICONS;
            int[] recordNumbers = new int[2];
            recordNumbers[0] = confirmIconId != null
                    ? confirmIconId.recordNumber : -1;
            recordNumbers[1] = callIconId != null ? callIconId.recordNumber
                    : -1;

            mIconLoader.loadIcons(recordNumbers, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }
        return false;
    }

    private boolean processProvideLocalInfo(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs)
            throws ResultException {
        CatLog.d(this, "process ProvideLocalInfo");
        switch (cmdDet.commandQualifier) {
            case DTTZ_SETTING:
                CatLog.d(this, "PLI [DTTZ_SETTING]");
                mCmdParams = new CommandParams(cmdDet);
                break;
            case LANGUAGE_SETTING:
                CatLog.d(this, "PLI [LANGUAGE_SETTING]");
                mCmdParams = new CommandParams(cmdDet);
                break;
            default:
                CatLog.d(this, "PLI[" + cmdDet.commandQualifier + "] Command Not Supported");
                mCmdParams = new CommandParams(cmdDet);
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
        }
        return false;
    }

    /**
     * Processes OPEN_CHANNEL proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processOpenChannel(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs)
            throws ResultException {

        CatLog.d(this, "enter: process OpenChannel");

        // Iterator for searching tlv
        Iterator<ComprehensionTlv> iter = null;
        ComprehensionTlv ctlv = null;
        // int tlvIndex = -1;

        BearerDesc bearerDesc = null;
        int bufferSize = 0;
        int linkMode = ((cmdDet.commandQualifier & 0x01) == 1)
                ? BipUtils.LINK_ESTABLISHMENT_MODE_IMMEDIATE
                : BipUtils.LINK_ESTABLISHMENT_MODE_ONDEMMAND;
        boolean isAutoReconnect = ((cmdDet.commandQualifier & 0x02) == 0) ? false : true;

        String accessPointName = null;
        OtherAddress localAddress = null;
        String userLogin = null;
        String userPwd = null;

        TransportProtocol transportProtocol = null;
        OtherAddress dataDestinationAddress = null;

        TextMessage confirmText = new TextMessage();
        IconId confirmIcon = null;

        // Two other address data objects may contain in one
        // OpenChannel data object. We can distinguish them
        // by their indices. The index of LocalAddress data
        // object should be less than the index of Transport-
        // Protocol data object and the index of DataDestination-
        // Address should be greater than the index of Trans-
        // port Protocol.
        int indexTransportProtocol = -1;

        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID,
                ctlvs);
        if (ctlv != null) {
            confirmText.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            confirmIcon = ValueParser.retrieveIconId(ctlv);
            confirmText.iconSelfExplanatory = confirmIcon.selfExplanatory;
        }

        // parse bearer description data object
        ctlv = searchForTag(ComprehensionTlvTag.BEARER_DESCRIPTION, ctlvs);
        if (ctlv != null) {
            bearerDesc = ValueParser.retrieveBearerDesc(ctlv);
            CatLog.d("[BIP]", "bearerDesc  \nbearer type: " + bearerDesc.bearerType
                    + "\nprecedence: " + bearerDesc.precedence
                    + "\ndelay: " + bearerDesc.delay
                    + "\nreliability: " + bearerDesc.reliability
                    + "\npeak: " + bearerDesc.peak
                    + "\nmean: " + bearerDesc.mean
                    + "\npdp type: " + bearerDesc.pdpType);
        } else {
            CatLog.d("[BIP]", "May Need BearerDescription object");
            //throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }

        // parse buffer size data object
        ctlv = searchForTag(ComprehensionTlvTag.BUFFER_SIZE, ctlvs);
        if (ctlv != null) {
            bufferSize = ValueParser.retrieveBufferSize(ctlv);
            CatLog.d("[BIP]", "buffer size: " + bufferSize);
        } else {
            CatLog.d("[BIP]", "Need BufferSize object");
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }

        // parse network access name data object
        ctlv = searchForTag(ComprehensionTlvTag.NETWORK_ACCESS_NAME, ctlvs);
        if (ctlv != null) {
            accessPointName = ValueParser.retrieveNetworkAccessName(ctlv);
            CatLog.d("[BIP]", "access point name: " + accessPointName);
        }

        // parse user login & password
        iter = ctlvs.iterator();
        ctlv = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, iter);
        if (ctlv != null) {
            userLogin = ValueParser.retrieveTextString(ctlv);
            CatLog.d("[BIP]", "user login: " + userLogin);
        }
        ctlv = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, iter);
        if (ctlv != null) {
            userPwd = ValueParser.retrieveTextString(ctlv);
            CatLog.d("[BIP]", "user password: " + userPwd);
        }

        // parse SIM/ME interface transport level & data destination address
        ctlv = searchForTagAndIndex(ComprehensionTlvTag.SIM_ME_INTERFACE_TRANSPORT_LEVEL, ctlvs);
        if (ctlv != null) {
            indexTransportProtocol = tlvIndex;
            CatLog.d("[BIP]", "CPF-processOpenChannel: indexTransportProtocol = "
                    + indexTransportProtocol);
            transportProtocol = ValueParser.retrieveTransportProtocol(ctlv);
            CatLog.d("[BIP]", "CPF-processOpenChannel: transport protocol(type/port): "
                    + transportProtocol.protocolType + "/" + transportProtocol.portNumber);
            if((BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == transportProtocol.protocolType)||
                (BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == transportProtocol.protocolType)){
                if(null == bearerDesc){
                    CatLog.d("[BIP]", "Need BearerDescription object");
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                }
            }
        } else {
            if(null == bearerDesc){
                CatLog.d("[BIP]", "BearerDescription & transportProtocol object are null");
                throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
            } else {
                CatLog.d("[BIP]", "transportProtocol object is null");            
                //There is on capability to handle this case, so throw it
                throw new ResultException(ResultCode.BIP_ERROR);                 
            }
        }

        if (transportProtocol != null) {
            CatLog.d("[BIP]", "CPF-processOpenChannel: transport protocol is existed");
            iter = ctlvs.iterator();
            resetTlvIndex();
            ctlv = searchForNextTagAndIndex(ComprehensionTlvTag.OTHER_ADDRESS, iter);
            if (ctlv != null) {
                if (tlvIndex < indexTransportProtocol) {
                    // this tlv is local address
                    CatLog.d("[BIP]", "CPF-processOpenChannel: get local address, index is "
                            + tlvIndex);
                    localAddress = ValueParser.retrieveOtherAddress(ctlv);

                    // we should also get destination address, because transport
                    // protocol object is existed
                    ctlv = searchForNextTagAndIndex(ComprehensionTlvTag.OTHER_ADDRESS, iter);
                    if (ctlv != null && tlvIndex > indexTransportProtocol) {
                        CatLog.d("[BIP]", "CPF-processOpenChannel: get dest address, index is "
                                + tlvIndex);
                        dataDestinationAddress = ValueParser.retrieveOtherAddress(ctlv);
                    } else {
                        CatLog.d("[BIP]", "CPF-processOpenChannel: missing dest address "
                                + tlvIndex
                                + "/" + indexTransportProtocol);
                        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                    }
                } else if (tlvIndex > indexTransportProtocol) {
                    CatLog.d("[BIP]",
                            "CPF-processOpenChannel: get dest address, but no local address");
                    dataDestinationAddress = ValueParser.retrieveOtherAddress(ctlv);
                } else {
                    CatLog.d("[BIP]", "CPF-processOpenChannel: Incorrect index");
                }
            } else {
                CatLog.d("[BIP]", "CPF-processOpenChannel: No other address object");
            }
            if(null == dataDestinationAddress){
                if(BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == transportProtocol.protocolType || 
                    BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == transportProtocol.protocolType){
                    CatLog.d("[BIP]", "BM-openChannel: dataDestinationAddress is null.");     
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);                
                }
            }//But if null == transportProtocol ??
        } else {
            // No transportProtocol, just retrieve LocalAddress data object
            CatLog.d("[BIP]", "CPF-processOpenChannel: No transport protocol object");
            //There is on capability to handle this case.
            throw new ResultException(ResultCode.BIP_ERROR);     
        /*    
            // No transportProtocol, just retrieve LocalAddress data object
            ctlv = searchForTag(ComprehensionTlvTag.OTHER_ADDRESS, ctlvs);
            if (ctlv != null) {
                localAddress = ValueParser.retrieveOtherAddress(ctlv);
            }
            */
        }

        // Undo: construct OpenChannelParams here
        if (bearerDesc != null) {
            if (bearerDesc.bearerType == BipUtils.BEARER_TYPE_GPRS) {
                mCmdParams = new OpenChannelParams(cmdDet, bearerDesc, bufferSize, localAddress,
                        transportProtocol, dataDestinationAddress,
                        accessPointName, userLogin, userPwd, confirmText);
            } else {
                CatLog.d("[BIP]", "Unsupport bearerType: " + bearerDesc.bearerType);
            }
        }

        mCmdParams = new OpenChannelParams(cmdDet, bearerDesc, bufferSize, localAddress,
                transportProtocol, dataDestinationAddress, accessPointName, userLogin, userPwd,
                confirmText);

        if (confirmIcon != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(confirmIcon.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }

        return false;
    }

    /**
     * Processes CLOSE_CHANNEL proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processCloseChannel(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs)
            throws ResultException {

        CatLog.d(this, "enter: process CloseChannel");

        ComprehensionTlv ctlv = null;

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;

        int channelId = 0;

        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            byte cidByte = ctlv.getRawValue()[ctlv.getValueIndex() + 1];
            channelId = cidByte & 0x0f;
            CatLog.d("[BIP]", "To close channel " + channelId);
        }
        boolean backToTcpListen = (1 == (cmdDet.commandQualifier & 0x01)) ? true:false;

        mCmdParams = new CloseChannelParams(cmdDet, channelId, textMsg, backToTcpListen);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }

        return false;
    }

    /**
     * Processes RECEIVE_DATA proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processReceiveData(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs)
            throws ResultException {

        CatLog.d(this, "enter: process ReceiveData");

        ComprehensionTlv ctlv = null;

        int channelDataLength = 0;

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        int channelId = 0;

        ctlv = searchForTag(ComprehensionTlvTag.CHANNEL_DATA_LENGTH, ctlvs);
        if (ctlv != null) {
            channelDataLength = ValueParser.retrieveChannelDataLength(ctlv);
            CatLog.d("[BIP]", "Channel data length: " + channelDataLength);
        }

        // mCmdParams = new ReceiveDataParams(cmdDet, channelDataLength,
        // textMsg);

        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            byte cidByte = ctlv.getRawValue()[ctlv.getValueIndex() + 1];
            channelId = cidByte & 0x0f;
            CatLog.d("[BIP]", "To Receive data: " + channelId);
        }

        mCmdParams = new ReceiveDataParams(cmdDet, channelDataLength, channelId, textMsg);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }

        return false;
    }

    /**
     * Processes SEND_DATA proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processSendData(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs)
            throws ResultException {

        CatLog.d(this, "enter: process SendData");

        ComprehensionTlv ctlv = null;

        byte[] channelData = null;

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        int channelId = 0;

        int sendMode = ((cmdDet.commandQualifier & 0x01) == 1)
                ? BipUtils.SEND_DATA_MODE_IMMEDIATE:BipUtils.SEND_DATA_MODE_STORED;

        ctlv = searchForTag(ComprehensionTlvTag.CHANNEL_DATA, ctlvs);
        if (ctlv != null) {
            channelData = ValueParser.retrieveChannelData(ctlv);
        }

        // mCmdParams = new SendDataParams(cmdDet, channelData, textMsg);

        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            byte cidByte = ctlv.getRawValue()[ctlv.getValueIndex() + 1];
            channelId = cidByte & 0x0f;
            CatLog.d("[BIP]", "To send data: " + channelId);
        }

        mCmdParams = new SendDataParams(cmdDet, channelData, channelId, textMsg, sendMode);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }

        return false;
    }

    /**
     * Processes GET_CHANNEL STATUS proactive command from the SIM card.
     * 
     * @param cmdDet Command Details container object.
     * @param ctlvs List of ComprehensionTlv objects following Command Details
     *            object and Device Identities object within the proactive
     *            command
     * @return true if the command is processing is pending and additional
     *         asynchronous processing is required.
     * @throws ResultException
     */
    private boolean processGetChannelStatus(CommandDetails cmdDet,
            List<ComprehensionTlv> ctlvs)
            throws ResultException {

        CatLog.d(this, "enter: process GetChannelStatus");

        ComprehensionTlv ctlv = null;

        TextMessage textMsg = new TextMessage();
        IconId iconId = null;

        ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }

        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }

        mCmdParams = new GetChannelStatusParams(cmdDet, textMsg);

        if (iconId != null) {
            mIconLoadState = LOAD_SINGLE_ICON;
            mIconLoader.loadIcon(iconId.recordNumber, this
                    .obtainMessage(MSG_ID_LOAD_ICON_DONE));
            return true;
        }

        return false;
    }

    /**
     * Get the index of ADDRESS data object.
     * 
     * @param list List of ComprehensionTlv
     * @return the index of ADDRESS data object.
     */
    private int getAddrIndex(final List<ComprehensionTlv> list) {
        int addrIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ADDRESS.value()) {
                return addrIndex;
            }
            ++addrIndex;
        } // end while

        return -1;
    }

    /**
     * Get the index of ALPHA_ID data object in confirmation phase data object.
     * 
     * @param list List of ComprehensionTlv
     * @param addrIndex The index of ADDRESS data object
     * @return the index of ALPHA_ID data object.
     */
    private int getConfirmationAlphaIdIndex(final List<ComprehensionTlv> list,
            final int addrIndex) {
        int alphaIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value()
                    && alphaIndex < addrIndex) {
                return alphaIndex;
            }
            ++alphaIndex;
        } // end while

        return -1;
    }

    /**
     * Get the index of ALPHA_ID data object in call phase data object.
     * 
     * @param list List of ComprehensionTlv
     * @param addrIndex The index of ADDRESS data object
     * @return the index of ALPHA_ID data object.
     */
    private int getCallingAlphaIdIndex(final List<ComprehensionTlv> list,
            final int addrIndex) {
        int alphaIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value()
                    && alphaIndex > addrIndex) {
                return alphaIndex;
            }
            ++alphaIndex;
        } // end while

        return -1;
    }

    /**
     * Get the ALPHA_ID data object in confirmation phase data object.
     * 
     * @param list List of ComprehensionTlv
     * @param addrIndex The index of ADDRESS data object
     * @return ALPHA_ID data object.
     */
    private ComprehensionTlv getConfirmationAlphaId(final List<ComprehensionTlv> list,
            final int addrIndex) {
        int alphaIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value()
                    && alphaIndex < addrIndex) {
                return temp;
            }
            ++alphaIndex;
        } // end while

        return null;
    }

    /**
     * Get the ALPHA_ID data object in call phase data object.
     * 
     * @param list List of ComprehensionTlv
     * @param addrIndex The index of ADDRESS data object
     * @return ALPHA_ID data object.
     */
    private ComprehensionTlv getCallingAlphaId(final List<ComprehensionTlv> list,
            final int addrIndex) {
        int alphaIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ALPHA_ID.value()
                    && alphaIndex > addrIndex) {
                return temp;
            }
            ++alphaIndex;
        } // end while

        return null;
    }

    /**
     * Get the ICON_ID data object in confirmation phase data object.
     * 
     * @param list List of ComprehensionTlv
     * @param alpha1Index The index of ALPHA_ID data object of confirmation
     *            phase
     * @param alpha2Index The index of ALPHA_ID data object of call phase
     * @return ICON_ID data object.
     */
    private ComprehensionTlv getConfirmationIconId(final List<ComprehensionTlv> list,
            final int alpha1Index,
            final int alpha2Index) {
        if (-1 == alpha1Index) {
            return null;
        }

        int iconIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ICON_ID.value()
                    && (-1 == alpha2Index || iconIndex < alpha2Index)) {
                return temp;
            }
            ++iconIndex;
        } // end while

        return null;
    }

    /**
     * Get the ICON_ID data object in call phase data object.
     * 
     * @param list List of ComprehensionTlv
     * @param alpha2Index The index of ALPHA_ID data object of call phase
     * @return ICON_ID data object.
     */
    private ComprehensionTlv getCallingIconId(final List<ComprehensionTlv> list,
            final int alpha2Index) {
        if (-1 == alpha2Index) {
            return null;
        }

        int iconIndex = 0;

        ComprehensionTlv temp = null;
        Iterator<ComprehensionTlv> iter = list.iterator();
        while (iter.hasNext()) {
            temp = iter.next();
            if (temp.getTag() == ComprehensionTlvTag.ICON_ID.value()
                    && iconIndex > alpha2Index) {
                return temp;
            }
            ++iconIndex;
        } // end while

        return null;
    }

}
