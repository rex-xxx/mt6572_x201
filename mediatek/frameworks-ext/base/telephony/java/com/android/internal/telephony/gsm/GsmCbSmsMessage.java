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

package com.android.internal.telephony.gsm;

import android.util.Log;

import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.SmsHeader;

import java.io.UnsupportedEncodingException;

public class GsmCbSmsMessage {
    static final String LOG_TAG = "GSM";

    protected int serialNumber;
    protected int messageID;
    protected int dcs;
    protected int totalPage;
    protected int pageIndex;

    private String messageBody;

    protected byte[] mPdu;
    protected SmsHeader userDataHeader;

    private int etwsWarningType;
    private byte[] etwsWarningSecurityInfo;

    public static final int CB_GS_Bit_Index = 14;
    public static final int CB_MsgCode_Bit_Index = 4;
    public static final int CB_UpdateNum_Bit_Index = 0;

    public static final int CB_ETWS_EmergencyAlert_Bit_Index = 13;
    public static final int CB_ETWS_Popup_Bit_Index = 13;

    public static final int GS_CELL_IMM = 0;
    public static final int GS_PLMN = 1;
    public static final int GS_LAC_SAC = 2;
    public static final int GS_CELL_NORMAL = 4;

    public static final int CB_DISPLAY_IMM = 0;
    public static final int CB_DISPLAY_NORMAL = 1;

    public static final int CB_ETWS_ID_BEGIN = 4352;
    public static final int CB_ETWS_ID_EARTHQUAKE = 4352;
    public static final int CB_ETWS_ID_TSUNAMI = 4353;
    public static final int CB_ETWS_ID_COMBINED = 4354;
    public static final int CB_ETWS_ID_TEST = 4355;
    public static final int CB_ETWS_ID_OTHERS = 4356;
    public static final int CB_ETWS_ID_END = 4359;

    public static final int CB_ETWS_WARNING_EARTHQUAKE = 0;
    public static final int CB_ETWS_WARNING_TSUNAMI = 1;
    public static final int CB_ETWS_WARNING_COMBINED = 2;
    public static final int CB_ETWS_WARNING_TEST = 3;
    public static final int CB_ETWS_WARNING_OTHERS = 4;

    /** User data text encoding code unit size */
    public static final int ENCODING_UNKNOWN = 0;
    public static final int ENCODING_7BIT = 1;
    public static final int ENCODING_8BIT = 2;
    public static final int ENCODING_16BIT = 3;

    public static boolean isDuplicateMessage(
            int oldSn, int oldMsgId, int oldLac, int oldCid,
            int newSn, int newMsgId, int newLac, int newCid) {
        int gsCode;
        boolean ret = false;

        gsCode = (oldSn >> CB_GS_Bit_Index) & 0x03;

        if (oldMsgId == newMsgId && oldSn == newSn) {
            if (oldSn == newSn) {
                switch (gsCode) {
                    case GS_CELL_IMM:
                    case GS_CELL_NORMAL:
                        ret = oldCid == newCid;
                        break;
                    case GS_PLMN:
                        ret = true;
                        break;
                    case GS_LAC_SAC:
                        ret = oldLac == newLac;
                        break;
                    default:
                        break;
                }
            }
        }

        return ret;
    }

    /**
     * Create an SmsMessage from a raw PDU.
     */
    public static GsmCbSmsMessage createFromPdu(byte[] pdu) {

        GsmCbSmsMessage msg = new GsmCbSmsMessage();

        msg.mPdu = pdu;

        // get serial number
        msg.serialNumber = ((pdu[0] & 0xff) << 8) | (pdu[1] & 0xff);

        // get message Identifier
        msg.messageID = ((pdu[2] & 0xff) << 8) | (pdu[3] & 0xff);

        if (msg.isETWSMessage()) {

            // get warning type
            msg.etwsWarningType = ((pdu[4] & 0xff) << 8) | (pdu[5] & 0xff);

            // get warning-Securify-information
            msg.etwsWarningSecurityInfo = new byte[50];
            int len = pdu.length - 6;
            System.arraycopy(pdu, 6, msg.etwsWarningSecurityInfo, 0, len < 50 ? len : 50);

            msg.messageBody = "This is a ETWS Message";

            /* TODO : we might need to parse etwsWarningSecurityInfo */
        } else {
            // get DCS
            msg.dcs = pdu[4];

            // get page index
            msg.pageIndex = (pdu[5] >> 4) & 0x0f;

            // get total page
            msg.totalPage = pdu[5] & 0x0f;

            if (msg.pageIndex == 0 || msg.totalPage == 0) {
                msg.pageIndex = 1;
                msg.totalPage = 1;
            }

            int encoding;
            if ((msg.dcs & 0xEC) == 0x48 || // bit7...4 : 010x bit3...0 : 10xx :
                    // UCS2 coding
                    msg.dcs == 0x11) { // UCS2; message preceded by language
                // indication

                encoding = ENCODING_16BIT;
            } else if ((msg.dcs & 0xEC) == 0x44 || // bit7...4 010x bit3...0 :
                    // 01xx : 8bit data
                    (msg.dcs & 0xF4) == 0xF4) { // bit7...4 1111 bit3...0 : x1xx
                // : 8bit data

                encoding = ENCODING_8BIT;
                Log.w(LOG_TAG, "CBSms newCB: we don't support this dcs: " + msg.dcs);
            } else if ((msg.dcs & 0xF0) == 0xE0) // defined by the WAP Forum
            {
                encoding = ENCODING_UNKNOWN;
                Log.w(LOG_TAG, "CBSms newCB: we don't support WAP dcs: " + msg.dcs);
            } else if ((msg.dcs & 0xF0) == 0x90) {
                encoding = ENCODING_UNKNOWN;
                Log.w(LOG_TAG, "CBSms newCB: we don't support UDH dcs: " + msg.dcs);
            } else if ((msg.dcs & 0xE0) == 0x60) {
                encoding = ENCODING_UNKNOWN;
                Log.w(LOG_TAG, "CBSms newCB: we don't support compressed dcs: " + msg.dcs);
            } else {
                encoding = ENCODING_7BIT;
            }

            int byteCount = pdu.length - 6;
            String text = null;
            if (byteCount < 0) {
                Log.e(LOG_TAG, "CBSms newCB: the length of the pdu is invalid: " + pdu);
            } else if (encoding == ENCODING_16BIT) {
                try {
                    text = new String(pdu, 6, byteCount, "utf-16");
                } catch (UnsupportedEncodingException ex) {
                    Log.e(LOG_TAG, "CBSms NewCB, implausible UnsupportedEncodingException", ex);
                    text = null;
                }
            } else if (encoding == ENCODING_8BIT) {
                try {
                    text = new String(pdu, 6, byteCount, "utf-8");
                } catch (UnsupportedEncodingException ex) {
                    Log.e(LOG_TAG, "CBSms NewCB, implausible UnsupportedEncodingException(utf-8)",
                            ex);
                    text = null;
                }
            } else if (encoding == ENCODING_7BIT) {
                int lengthSeptets;
                lengthSeptets = byteCount * 8 / 7;
                // text = GsmAlphabet.gsm7BitPackedToString(pdu, 6,
                // lengthSeptets, 0);
                text = GsmAlphabet.gsm7BitPackedToString(pdu, 6, lengthSeptets);
            } else {
                text = null;
            }

            // remove padding characters 0x0D
            if (text != null) {
                msg.messageBody = removeTailCharacters(text, '\r');
            } else {
                msg.messageBody = null;
            }
        }

        Log.d(LOG_TAG, "CBSms newCB: " + msg);
        return msg;
    }

    public static GsmCbSmsMessage newFromCBM(String smsPdu) {

        Log.d(LOG_TAG, "CBSms newFromCBM: " + smsPdu);

        byte[] pduData = IccUtils.hexStringToBytes(smsPdu);
        if (pduData == null) {
            return null;
        }

        return createFromPdu(pduData);
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public int getMessageID() {
        return messageID;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public int getDisplayMode() {
        return ((serialNumber >> CB_GS_Bit_Index) & 0x03) == 0 ?
                CB_DISPLAY_IMM : CB_DISPLAY_NORMAL;
    }

    public byte[] getPdu() {
        return mPdu;
    }

    /**
     * Return the user data header (UDH).
     * 
     * @hide
     */
    public SmsHeader getUserDataHeader() {
        return userDataHeader;
    }

    public int getWarningType() {
        return etwsWarningType;
    }

    public byte[] getWarningSecurityInfo() {
        return etwsWarningSecurityInfo;
    }

    public boolean isETWSMessage() {
        return (messageID >= CB_ETWS_ID_BEGIN) && (messageID <= CB_ETWS_ID_END);
    }

    public boolean isETWSNeedAlert() {
        return ((serialNumber >> CB_ETWS_EmergencyAlert_Bit_Index) & 0x01) == 1;
    }

    public boolean isETWSNeedPopup() {
        return ((serialNumber >> CB_ETWS_Popup_Bit_Index) & 0x01) == 1;
    }

    public String toString() {
        return "CB Msg: Serial number = " + serialNumber +
                ", message identifier = " + messageID +
                ", hasUDH = " + (userDataHeader != null) +
                ", totalPage = " + totalPage +
                ", pageIndex = " + pageIndex +
                ", message Body = " + messageBody;
    }

    private static String removeTailCharacters(String src, char c) {
        StringBuilder sb = new StringBuilder(src);

        int i = -1;
        for (i = src.length() - 1; i >= 0; i--) {

            if (sb.charAt(i) == c) {
                ;
            } else {
                break;
            }
        }
        return sb.substring(0, i + 1);
    }

}
