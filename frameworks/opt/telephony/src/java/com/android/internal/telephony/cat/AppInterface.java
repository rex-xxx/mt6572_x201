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

package com.android.internal.telephony.cat;

/**
 * Interface for communication between STK App and CAT Telephony {@hide}
 */
public interface AppInterface {

    /*
     * Intent's actions which are broadcasted by the Telephony once a new CAT
     * proactive command, session end arrive.
     */
    public static final String CAT_CMD_ACTION =
            "android.intent.action.stk.command";
    public static final String CAT_SESSION_END_ACTION =
            "android.intent.action.stk.session_end";
    public static final String CAT_CMD_ACTION_2 =
            "android.intent.action.stk.command_2";
    public static final String CAT_SESSION_END_ACTION_2 =
            "android.intent.action.stk.session_end_2";

    //MTK-START [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card
    public static final String CAT_CMD_ACTION_3 =
        "android.intent.action.stk.command_3";
    public static final String CAT_SESSION_END_ACTION_3 =
        "android.intent.action.stk.session_end_3";
    public static final String CAT_CMD_ACTION_4 =
        "android.intent.action.stk.command_4";
    public static final String CAT_SESSION_END_ACTION_4 =
        "android.intent.action.stk.session_end_4";
    //MTK-END [mtk80950][121110][ALPS00XXXXXX] add for Gemini+ the 3rd card and the 4th card
    public static final String CLEAR_DISPLAY_TEXT_CMD =
            "android.intent.action.stk.clear_display_text";
    /*
     * Callback function from app to telephony to pass a result code and user's
     * input back to the ICC.
     */
    void onCmdResponse(CatResponseMessage resMsg);

    /*
     * Callback function from app to telephony to pass a envelope to the SIM.
     */
    void onEventDownload(CatResponseMessage resMsg);

    /*
     * Callback function from app to telephony to handle DB.
     */
    void onDBHandler(int sim_id);

    /*
     * Enumeration for representing "Type of Command" of proactive commands.
     * Those are the only commands which are supported by the Telephony. Any app
     * implementation should support those.
     */
    public static enum CommandType {
        DISPLAY_TEXT(0x21),
        GET_INKEY(0x22),
        GET_INPUT(0x23),
        LAUNCH_BROWSER(0x15),
        PLAY_TONE(0x20),
        REFRESH(0x01),
        SELECT_ITEM(0x24),
        SEND_SS(0x11),
        SEND_USSD(0x12),
        SEND_SMS(0x13),
        SEND_DTMF(0x14),
        SET_UP_EVENT_LIST(0x05),
        SET_UP_IDLE_MODE_TEXT(0x28),
        SET_UP_MENU(0x25),
        SET_UP_CALL(0x10),
        PROVIDE_LOCAL_INFORMATION(0x26),
                // Add By huibin
        MORE_TIME(0x02),
        POLL_INTERVAL(0x03),
        POLLING_OFF(0x04),
        TIMER_MANAGEMENT(0x27),
        PERFORM_CARD_APDU(0x30),
        POWER_ON_CARD(0x31),
        POWER_OFF_CARD(0x32),
        GET_READER_STATUS(0x33),
        RUN_AT_COMMAND(0x34),
        LANGUAGE_NOTIFICATION(0x35),
        OPEN_CHANNEL(0x40),
        CLOSE_CHANNEL(0x41),
        RECEIVE_DATA(0x42),
        SEND_DATA(0x43),
        GET_CHANNEL_STATUS(0x44),
        SERVICE_SEARCH(0x45),
        GET_SERVICE_INFORMATION(0x46),
        DECLARE_SERVICE(0x47),
        SET_FRAME(0x50),
        GET_FRAME_STATUS(0x51),
        RETRIEVE_MULTIMEDIA_MESSAGE(0x60),
        SUBMIT_MULTIMEDIA_MESSAGE(0x61),
        DISPLAY_MULTIMEDIA_MESSAGE(0x62),
        ACTIVATE(0x70);

        private int mValue;

        CommandType(int value) {
            mValue = value;
        }

        public int value() {
            return mValue;
        }

        /**
         * Create a CommandType object.
         * 
         * @param value Integer value to be converted to a CommandType object.
         * @return CommandType object whose "Type of Command" value is {@code
         *         value}. If no CommandType object has that value, null is
         *         returned.
         */
        public static CommandType fromInt(int value) {
            for (CommandType e : CommandType.values()) {
                if (e.mValue == value) {
                    return e;
                }
            }
            return null;
        }
    }

    // MTK-START [ALPS00092673] Orange feature merge back added by mtk80589 in
    // 2011.11.15
    /*
     * Detail description: This feature provides a interface to get menu title
     * string from EF_SUME
     */
    // MTK_OP03_PROTECT_START
    /**
     * get menu title from ef 0x6f54
     * 
     * @return STK menu title
     */
    //public String getMenuTitleFromEf();
    // MTK_OP03_PROTECT_END
    // MTK-END [ALPS00092673] Orange feature merge back added by mtk80589 in
    // 2011.11.15
}
