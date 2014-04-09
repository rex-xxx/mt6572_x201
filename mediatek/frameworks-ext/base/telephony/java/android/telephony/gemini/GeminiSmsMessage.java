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

package android.telephony.gemini;

import android.os.Parcel;

import android.telephony.SmsMessage;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;

/**
 * Manages SMS operations such as sending data, text, and pdu SMS messages.
 * This class also contains and manages the data or operations relative to Gemini 
 * {@hide}
 */
public final class GeminiSmsMessage extends SmsMessage {

    private static final String LOG_TAG = "SMS";
    private int simId;

    public GeminiSmsMessage() {
        this(PhoneConstants.GEMINI_SIM_1);
    }

    public GeminiSmsMessage(int simId) {
        super();
        this.simId = simId;
    }

    public GeminiSmsMessage(SmsMessage sms, int simId) {
        if (sms != null) {
            mWrappedSmsMessage = sms.mWrappedSmsMessage;
        } else {
            mWrappedSmsMessage = null;
        }
        this.simId = simId;
    }

    /**
     * Create an SmsMessage from a raw PDU.
     * 
     * @param pdu raw data for creating short message
     * @param simId SIM ID
     */
    public static GeminiSmsMessage createFromPdu(byte[] pdu, int simId) {
        String format = GeminiSmsManager.getSmsFormat(simId);
        Log.d(LOG_TAG, "create SmsMessage from pdu with format " + format);
        SmsMessage sms = createFromPdu(pdu, format);
        if (sms != null) {
            return new GeminiSmsMessage(sms, simId);
        } else {
            Log.d(LOG_TAG, "fail to create SmsMessage from pdu");
            return null;
        }
    }

    /** @hide */
    public static GeminiSmsMessage createFromPdu(byte[] pdu, String format, int simId) {

        SmsMessage sms = createFromPdu(pdu, format);

        return new GeminiSmsMessage(sms, simId);
    }

    /**
     * TS 27.005 3.4.1 lines[0] and lines[1] are the two lines read from the
     * +CMT unsolicited response (PDU mode, of course) +CMT:
     * [&lt;alpha>],<length><CR><LF><pdu> Only public for debugging and for RIL
     * {@hide}
     */
    public static GeminiSmsMessage newFromCMT(String[] lines, int simId) {

        SmsMessage sms = newFromCMT(lines);

        return new GeminiSmsMessage(sms, simId);
    }

    /** @hide */
    protected static GeminiSmsMessage newFromCMTI(String line, int simId) {

        SmsMessage sms = newFromCMTI(line);

        return new GeminiSmsMessage(sms, simId);
    }

    /** @hide */
    public static GeminiSmsMessage newFromCDS(String line, int simId) {

        SmsMessage sms = newFromCDS(line);

        return new GeminiSmsMessage(sms, simId);
    }

    /** @hide */
    public static GeminiSmsMessage newFromParcel(Parcel p, int simId) {

        SmsMessage sms = newFromParcel(p);

        return new GeminiSmsMessage(sms, simId);
    }

    /**
     * Create an SmsMessage from an SMS EF record.
     *
     * @param index Index of SMS record. This should be index in ArrayList
     *              returned by SmsManager.getAllMessagesFromSim + 1.
     * @param data Record data.
     * @return An SmsMessage representing the record.
     *
     * @hide
     */
    public static GeminiSmsMessage createFromEfRecord(int index, byte[] data, int simId) {

        String format = GeminiSmsManager.getSmsFormat(simId);

        SmsMessage sms = createFromEfRecord(index, data, format);

        return sms == null ? null : new GeminiSmsMessage(sms, simId);
    }

    /**
     * @return SIM ID
     */
    public int getMessageSimId() {
        return simId;
    }

    /**
     * Returns the smsc raw data from the pdu
     *
     * @return the raw smsc for the message.
     * @hide
     */
    public byte[] getSmsc() {
        if (FeatureOption.EVDO_DT_VIA_SUPPORT == true) {
            String format = GeminiSmsManager.getSmsFormat(simId);
            return getSmsc(format);
        } else {
            return super.getSmsc();
        }
    }

    /**
     * Returns the smsc raw data from the pdu
     * 
     * @return the raw smsc for the message.
     * @hide
     */
    public byte[] getSmsc(String format) {
        Log.d(LOG_TAG, "call getSmsc with format: " + format);
        byte[] pdu = this.getPdu();
        if (format.equals(FORMAT_3GPP)) {
            if (pdu == null) {
                Log.d(LOG_TAG, "pdu is null");
                return null;
            }

            int smsc_len = (pdu[0] & 0xff) + 1;
            byte[] smsc = new byte[smsc_len];

            try {
                System.arraycopy(pdu, 0, smsc, 0, smsc.length);
                return smsc;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(LOG_TAG, "Out of boudns");
                return null;
            }
        } else if (format.equals(FORMAT_3GPP2)) {
            return null;
        }

        return super.getSmsc();
    }

    /**
     * Returns the tpdu from the pdu
     * 
     * @return the tpdu for the message.
     * @hide
     */
    public byte[] getTpdu() {
        if (FeatureOption.EVDO_DT_VIA_SUPPORT == true) {
            String format = GeminiSmsManager.getSmsFormat(simId);
            return getTpdu(format);
        } else {
            return super.getTpdu();
        }
    }

    /**
     * Returns the tpdu from the pdu
     * 
     * @return the tpdu for the message.
     * @hide
     */
    public byte[] getTpdu(String format) {
        Log.d(LOG_TAG, "call getTpdu with format: " + format);
        byte[] pdu = this.getPdu();
        if (format.equals(FORMAT_3GPP)) {
            if (pdu == null) {
                Log.d(LOG_TAG, "pdu is null");
                return null;
            }

            int smsc_len = (pdu[0] & 0xff) + 1;
            int tpdu_len = pdu.length - smsc_len;
            byte[] tpdu = new byte[tpdu_len];

            try {
                System.arraycopy(pdu, smsc_len, tpdu, 0, tpdu.length);
                return tpdu;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(LOG_TAG, "Out of boudns");
                return null;
            }
        } else if (format.equals(FORMAT_3GPP2)) {
            return pdu;
        }

        return super.getTpdu();
    }
}
