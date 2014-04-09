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

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.telephony.SmsMessage;
import android.telephony.SmsManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SimSmsInsertStatus;
import android.telephony.SmsParameters;

import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.ISms;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.telephony.IccSmsStorageStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages SMS operations such as sending data, text, and pdu SMS messages.
 */
public final class GeminiSmsManager {

    private static final String TAG = "SMS";

    private GeminiSmsManager() {}

    /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message 
     */
    public static ArrayList<String> divideMessage(String text) {
        return SmsMessage.fragmentText(text);
    }

    /**
     * Send a text based SMS.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param simId the sim card that user wants to access
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or text are empty
     */
    public static void sendTextMessageGemini(
            String destinationAddress, String scAddress, String text, int simId,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "call sendTextMessageGemini");
        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }

        String isms = getSmsServiceName(simId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                iccISms.sendText(destinationAddress, scAddress, text, sentIntent, deliveryIntent);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Send a text based SMS to a specified application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param destinationPort the port to deliver the message to
     * @param simId the sim card that user wants to access
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or text are empty
     */
    public static void sendTextMessageGemini( 
            String destinationAddress, String scAddress, String text,
            short destinationPort, int simId, PendingIntent sentIntent, 
            PendingIntent deliveryIntent) {
        // impl
        /*
        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }

        String isms = getSmsServiceName(simId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                iccISms.sendTextWithPort(destinationAddress, scAddress, text, 
                    destinationPort & 0xFFFF, sentIntent, deliveryIntent);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        */
        // don't support in ISms
    }

    /**
     * Send a multi-part text based SMS.  The callee should have already
     * divided the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param simId the sim card that user wants to access
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applicaitons,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public static void sendMultipartTextMessageGemini(
            String destinationAddress, String scAddress, ArrayList<String> parts, int simId,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        // impl
        Log.d(TAG, "call sendMultipartTextMessageGemini");
        if (!isValidParameters(destinationAddress, parts, sentIntents)) {
            return;
        }

        String isms = getSmsServiceName(simId);
        if (parts.size() > 1) {
            try {
                ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
                if (iccISms != null) {
                    iccISms.sendMultipartText(destinationAddress, scAddress, parts,
                            sentIntents, deliveryIntents);
                }
            } catch (RemoteException ex) {
                // ignore it
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }
            String text = (parts == null || parts.size() == 0) ? "" : parts.get(0);
            sendTextMessageGemini(destinationAddress, scAddress, text, simId,
                    sentIntent, deliveryIntent);
        }
    }

    /**
     * Send a multi-part text based SMS.  The callee should have already
     * divided the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param destinationPort the port to deliver the message to
     * @param simId the sim card that user wants to access
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applicaitons,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public static void sendMultipartTextMessageGemini(
            String destinationAddress, String scAddress, 
            ArrayList<String> parts, short destinationPort, int simId,
            ArrayList<PendingIntent> sentIntents, 
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        /*
        if (!isValidParameters(destinationAddress, parts, sentIntents)) {
            return;
        }

        String isms = getSmsServiceName(simId);
        if (parts.size() > 1) {
            try {
                ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
                if (iccISms != null) {
                    iccISms.sendMultipartTextWithPort(destinationAddress, scAddress, parts,
                            destinationPort, sentIntents, deliveryIntents);
                }
            } catch (RemoteException ex) {
                // ignore it
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }
            sendTextMessageGemini(destinationAddress, scAddress, parts.get(0),
                    destinationPort, simId, sentIntent, deliveryIntent);
        }
        */
        // don't support in ISms
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param data the body of the message to send
     * @param simId the sim card that user wants to access
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public static void sendDataMessageGemini(
            String destinationAddress, String scAddress, short destinationPort,
            byte[] data, int simId, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "call sendDataMessageGemini");
        if (!isValidParameters(destinationAddress, "send_data" , sentIntent)) {

            return;
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        String isms = getSmsServiceName(simId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                iccISms.sendData(destinationAddress, scAddress, destinationPort & 0xFFFF,
                        data, sentIntent, deliveryIntent);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Send a data based SMS to a specific application port with original port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param originalPort the port to deliver the message from
     * @param data the body of the message to send
     * @param simId the sim card that user wants to access
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public static void sendDataMessageGemini(
        String destinationAddress, String scAddress, short destinationPort, short originalPort,
        byte[] data, int simId, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "[xj send data with original port");
        if (!isValidParameters(destinationAddress, "send_data" , sentIntent)) {

            return;
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        String isms = getSmsServiceName(simId);
        try {
            Log.d(TAG, "[xj get sms service start");
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            Log.d(TAG, "[xj get sms service end");
            if (iccISms != null) {
                Log.d(TAG, "[xj send data start");
                iccISms.sendDataWithOriginalPort(destinationAddress, scAddress,
                        destinationPort & 0xFFFF,
                        originalPort & 0xFFFF, data, sentIntent, deliveryIntent);
                Log.d(TAG, "[xj send data end");
            }
        } catch (RemoteException ex) {
            // ignore it
        }
    }

    /**
     * Send a multi-part data based SMS.  The callee should have already
     * divided the message into correctly sized parts
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param data the array of data messages body to send
     * @param simId the sim card that user wants to access
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applicaitons,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public static void sendMultipartDataMessageGemini(
            String destinationAddress, String scAddress, short destinationPort,
            byte[][] data, int simId, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        /*
        Log.d(TAG, "sendMultipartDataMessageGemini");
        ArrayList<String> fake_text = new ArrayList<String>();
        fake_text.add("send_data1");
        if (!isValidParameters(destinationAddress, fake_text , sentIntents)) {

            return;
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        String isms = getSmsServiceName(simId);
        if (data.length > 1) {
            ArrayList<SmsRawData> list = new ArrayList<SmsRawData>(data.length);
            for (int i=0; i < data.length ;i++)
            {
                list.add(new SmsRawData(data[i]));
            }
            
            try {
                ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
                if (iccISms != null) {
                    iccISms.sendMultipartData(destinationAddress, scAddress, destinationPort, 
                        list, sentIntents, deliveryIntents);
                }
            } catch (RemoteException ex) {
                // ignore it
            }
        }
        else{
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }
            sendDataMessageGemini(destinationAddress, scAddress, destinationPort, 
                    data[0], simId, sentIntent, deliveryIntent);        
        }
        */
        // don't support in ISms
    }

    /**
     * Copy a raw SMS PDU to the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param smsc the SMSC for this message, or NULL for the default SMSC
     * @param pdu the raw PDU to store
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param simId the sim card that user wants to access
     * @return true for success
     *
     */
    public static boolean copyMessageToIccGemini(
            byte[] smsc, byte[] pdu, int status, int simId) {
        // impl
        Log.d(TAG, "call copyMessageToIccGemini");
        boolean success = false;

        String isms = getSmsServiceName(simId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                success = iccISms.copyMessageToIccEf(status, pdu, smsc);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Copy a text SMS to the ICC.
     *
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @param slotId Copy message into ICC card which is specified by this param
     * @return success or not
     *
     */
    public static int copyTextMessageToIccCardGemini(String scAddress, String address, List<String> text,
            int status, long timestamp, int slotId) {
        Log.d(TAG, "call copyTextMessageToIccCardGemini");
        int result = SmsManager.RESULT_ERROR_GENERIC_FAILURE;

        String isms = getSmsServiceName(slotId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                result = iccISms.copyTextMessageToIccCard(scAddress, address, text, status, timestamp);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return result;
    }

    /**
     * Delete the specified message from the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param messageIndex is the record index of the message on ICC
     * @param simId the sim card that user wants to access
     * @return true for success
     *
     */
    public static boolean
    deleteMessageFromIccGemini(int messageIndex, int simId) {
        // impl
        Log.d(TAG, "call deleteMessageFromIccGemini");
        boolean success = false;
        String isms = getSmsServiceName(simId);

        byte[] pdu = new byte[IccConstants.SMS_RECORD_LENGTH - 1];
        Arrays.fill(pdu, (byte) 0xff);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                success = iccISms.updateMessageOnIccEf(messageIndex,
                        SmsManager.STATUS_ON_ICC_FREE, pdu);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Update the specified message on the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param messageIndex record index of message to update
     * @param newStatus new message status (STATUS_ON_ICC_READ,
     *                  STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *                  STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu the raw PDU to store
     * @param simId the sim card that user wants to access
     * @return true for success
     *
     */
    public static boolean updateMessageOnIccGemini(int messageIndex,
            int newStatus, byte[] pdu, int simId) {
        // impl
        Log.d(TAG, "call updateMessageOnIccGemini");
        boolean success = false;
        String isms = getSmsServiceName(simId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                success = iccISms.updateMessageOnIccEf(messageIndex, newStatus, pdu);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Retrieves all messages currently stored on ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param simId the sim card that user wants to access
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects
     *
     */
    public static ArrayList<SmsMessage> getAllMessagesFromIccGemini(int simId) {
        // impl
        Log.d(TAG, "call getAllMessagesFromIccGemini");
        String isms = getSmsServiceName(simId);
        List<SmsRawData> records = null;

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEf();
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        int sz = 0;
        if (records != null) {
            sz = records.size();
        }
        for (int i = 0; i < sz; ++i) {
            byte[] data = null;
            SmsRawData record = records.get(i);
            if (record == null) {
                continue;
            } else {
                data = record.getBytes();
            }
            int index = i + 1;
            if ((data[0] & 0xff) == SmsManager.STATUS_ON_ICC_UNREAD) {
                Log.d(TAG, "index[" + index + "] is STATUS_ON_ICC_READ");
                boolean ret;
                ret = updateMessageOnIccGemini(index, SmsManager.STATUS_ON_ICC_READ, data, simId);
                if (ret == true) {
                    Log.d(TAG, "update index[" + index + "] to STATUS_ON_ICC_READ");
                } else {
                    Log.d(TAG, "fail to update message status");
                }
            }
        }

        return createMessageListFromRawRecords(records, simId);

    }

    /**
     * Set the memory storage status of the SMS
     * This function is used for FTA test only
     * 
     * @param status false for storage full, true for storage available
     * @param simId the sim card that user wants to access     
     *
     */
    public static void setSmsMemoryStatusGemini(boolean status, int simId) {
        // impl
        Log.d(TAG, "call setSmsMemoryStatusGemini");
        String isms = getSmsServiceName(simId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                iccISms.setSmsMemoryStatus(status);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

    }

    /**
     * Judge if SMS subsystem is ready or not
     *
     * @param simId the sim card ID
     *
     * @return true for success
     *
     */
    public static boolean isSmsReadyGemini(int simId) {
        // impl
        Log.d(TAG, "call isSmsReadyGemini");
        boolean isReady = false;
        String isms = getSmsServiceName(simId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                isReady = iccISms.isSmsReady();
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return isReady;
    }

    /**
     * Get SMS SIM Card memory's total and used number
     *
     * @param simId the sim card ID
     *
     * @return <code>IccSmsStorageStatus</code> object
     *
     */
    public static IccSmsStorageStatus getSmsSimMemoryStatusGemini(int simId) {
        // impl
        Log.d(TAG, "call getSmsSimMemoryStatusGemini");
        String isms = getSmsServiceName(simId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));

            if (iccISms != null) {
                return iccISms.getSmsSimMemoryStatus();
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return null;
    }

    /**
     * Create a list of <code>SmsMessage</code>s from a list of RawSmsData
     * records returned by <code>getAllMessagesFromIcc()</code>
     *
     *
     * @param records SMS EF records, returned by
     *   <code>getAllMessagesFromIcc</code>
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects.
     *
     */
    private static ArrayList<SmsMessage> 
    createMessageListFromRawRecords(List<SmsRawData> records, int simId) {
        // impl
        Log.d(TAG, "call createMessageListFromRawRecords");
        ArrayList<SmsMessage> geminiMessages = null;
        if (records != null) {
            int count = records.size();
            geminiMessages =  new ArrayList<SmsMessage>();
            
            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);
                
                if (data != null) {
                    GeminiSmsMessage geminiSms = 
                            GeminiSmsMessage.createFromEfRecord(i+1, data.getBytes(), simId);
                    if (geminiSms != null) {
                        geminiMessages.add(geminiSms);
                    }
                }
            }
            Log.d(TAG, "actual SIM sms count is " + geminiMessages.size());
        } else {
            Log.d(TAG, "fail to parse SIM sms, records is null");
        }

        return geminiMessages;
    }

    /**
     * Get the SMS service name by specific SIM ID
     * @simId SIM ID
     * @return the SMS service name
     */
    private static String getSmsServiceName(int simId) {
        if (simId == PhoneConstants.GEMINI_SIM_1) {
            return "isms";
        } else if (simId == PhoneConstants.GEMINI_SIM_2) {
            return "isms2";
        } else if (simId == PhoneConstants.GEMINI_SIM_3) {
            return "isms3";
        } else if (simId == PhoneConstants.GEMINI_SIM_4) {
            return "isms4";
        } else {
            return null;
        }
    }

    /**
     * Judge if the destination address is a valid SMS address or not, and if
     * the text is null or not
     * 
     * @destinationAddress the destination address to which the message be sent
     * @text the content of shorm message
     * @sentIntent will be broadcast if the address or the text is invalid
     * @return true for valid parameters
     */
    private static boolean isValidParameters(
            String destinationAddress, String text, PendingIntent sentIntent) {
        // impl
        ArrayList<PendingIntent> sentIntents = 
                new ArrayList<PendingIntent>();
        ArrayList<String> parts = 
                new ArrayList<String>();

        sentIntents.add(sentIntent);
        parts.add(text);

        // if (TextUtils.isEmpty(text)) {
        // throw new IllegalArgumentException("Invalid message body");
        // }

        return isValidParameters(destinationAddress, parts, sentIntents);
    }

    /**
     * Judge if the destination address is a valid SMS address or not, and if
     * the text is null or not
     * 
     * @destinationAddress the destination address to which the message be sent
     * @parts the content of shorm message
     * @sentIntent will be broadcast if the address or the text is invalid
     * @return true for valid parameters
     */
    private static boolean isValidParameters(
            String destinationAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents) {
        // impl
        if (parts == null || parts.size() == 0) {
            return true;
        }

        if (!isValidSmsDestinationAddress(destinationAddress)) {
            for (int i = 0; i < sentIntents.size(); i++) {
                PendingIntent sentIntent = sentIntents.get(i);
                if (sentIntent != null) {
                    try {
                        sentIntent.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                    } catch (CanceledException ex) {}
                }
            }

            Log.d(TAG, "Invalid destinationAddress: " + destinationAddress);
            return false;
        }

        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (parts == null || parts.size() < 1) {
            throw new IllegalArgumentException("Invalid message body");
        }

        return true;
    }

    /**
     * judge if the input destination address is a valid SMS address or not
     *
     * @param da the input destination address
     * @return true for success
     *
     */
    private static boolean isValidSmsDestinationAddress(String da) {
        // impl
        String encodeAddress = PhoneNumberUtils.extractNetworkPortion(da);
        if (encodeAddress == null)
            return true;

        int spaceCount = 0;
        for (int i = 0; i < da.length(); ++i) {
            if (da.charAt(i) == ' ' || da.charAt(i) == '-') {
                spaceCount++;
            }
        }

        return encodeAddress.length() == (da.length() - spaceCount);
    }

    // MTK-START [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    /**
     * Send a text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param encodingType the encoding type of message(gsm 7-bit, unicode or automatic)
     * @param slotId the sim card that user wants to access
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or text are empty
     */
    public static void sendTextMessageWithEncodingTypeGemini(
            String destAddr,
            String scAddr,
            String text,
            int encodingType,
            int slotId,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "call sendText, encoding = " + encodingType);

        if (!isValidParameters(destAddr, text, sentIntent)) {
            Log.d(TAG, "the parameters are invalid");
            return;
        }
        Log.d(TAG, "to get ISms");
        String isms = getSmsServiceName(slotId);
        Log.d(TAG, "isms = " + isms);
        Log.d(TAG, "isms = " + slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                Log.d(TAG, "call ISms interface to send text message");
                iccISms.sendTextWithEncodingType(destAddr, 
                    scAddr, text, encodingType, sentIntent, deliveryIntent);
            } else {
                Log.d(TAG, "iccISms is null");
            }
        } catch (RemoteException ex) {
            // ignore it
            Log.d(TAG, "fail to get ISms");
        }
    }

    /**
     * Send a multi-part text based SMS.  The callee should have already
     * divided the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param encodingType the encoding type of message(gsm 7-bit, unicode or automatic)
     * @param slotId the sim card that user wants to access
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applicaitons,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public static void sendMultipartTextMessageWithEncodingTypeGemini(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            int encodingType,
            int slotId,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        Log.d(TAG, "call sendMultipartText, encoding = " + encodingType);

        if (!isValidParameters(destAddr, parts, sentIntents)) {
            Log.d(TAG, "invalid parameters for multipart message");
            return;
        }

        String isms = getSmsServiceName(slotId);
        if (parts.size() > 1) {
            try {
                ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
                if (iccISms != null) {
                    Log.d(TAG, "call ISms.sendMultipartText");
                    iccISms.sendMultipartTextWithEncodingType(destAddr, scAddr, parts,
                            encodingType, sentIntents, deliveryIntents);
                }
            } catch (RemoteException ex) {
                // ignore it
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            Log.d(TAG, "get sentIntent: " + sentIntent);
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }
            Log.d(TAG, "send single message");
            if (parts != null) {
                Log.d(TAG, "parts.size = " + parts.size());
            }
            String text = (parts == null || parts.size() == 0) ? "" : parts.get(0);
            Log.d(TAG, "pass encoding type " + encodingType);
            sendTextMessageWithEncodingTypeGemini(destAddr, scAddr, text, encodingType,
                    slotId, sentIntent, deliveryIntent);
        }
    }

    /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @param encodingType text encoding type(7-bit, 16-bit or automatic)
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message 
     */
    public static ArrayList<String> divideMessage(String text, int encodingType) {
        Log.d(TAG, "call divideMessage, encoding = " + encodingType);
        ArrayList<String> ret = SmsMessage.fragmentText(text, encodingType);
        Log.d(TAG, "divideMessage: size = " + ret.size());
        return ret;
    }
    // MTK-END [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /**
     * Copy a text SMS to the ICC.
     *
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp timestamp when service center receive the message
     * @return SimSmsInsertStatus
     *
     */
    public static SimSmsInsertStatus insertTextMessageToIccCardGemini(String scAddress, String address, List<String> text,
            int status, long timestamp, int slotId) {
        // impl
        Log.d(TAG, "call insertTextMessageToIccCardGemini");
        SimSmsInsertStatus ret = null;

        String isms = getSmsServiceName(slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if(iccISms != null) {
                ret = iccISms.insertTextMessageToIccCard(scAddress, address, text, status, timestamp);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        Log.d(TAG, (ret != null) ? "[insertText " + ret.indexInIcc : "[insertText null");
        return ret;
    }
    
    public static SimSmsInsertStatus insertRawMessageToIccCardGemini(int status, byte[] pdu, byte[] smsc, int slotId) {
        // impl
        Log.d(TAG, "call insertRawMessageToIccCardGemini");
        SimSmsInsertStatus ret = null;

        String isms = getSmsServiceName(slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                ret = iccISms.insertRawMessageToIccCard(status, pdu, smsc);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        Log.d(TAG, (ret != null) ? "[insertRaw " + ret.indexInIcc : "[insertRaw null");
        return ret;
    }

    /**
     * Send an SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param slotId, identifier for SIM card slot
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @hide
     */
    public static void sendTextMessageWithExtraParamsGemini(
            String destAddr,
            String scAddr,
            String text,
            Bundle extraParams,
            int slotId,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "call sendTextWithExtraParamsGemini");
        if (isValidParameters(destAddr, text, sentIntent) == false) {
            return;
        }

        if (extraParams == null) {
            Log.d(TAG, "bundle is null");
            return;
        }

        String serviceName = getSmsServiceName(slotId);
        Log.d(TAG, "service name is " + serviceName);
        try {
            ISms service = ISms.Stub.asInterface(ServiceManager.getService(serviceName));
            if (service != null) {
                service.sendTextWithExtraParams(destAddr, scAddr, text, extraParams,
                        sentIntent, deliveryIntent);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "fail to call sendTextWithExtraParamsGemini: " + e);
        }
    }

    /**
     * Send a multi-part text based SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param slotId, identifier for SIM card slot
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     * @hide
     */
    public static void sendMultipartTextMessageWithExtraParamsGemini(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            Bundle extraParams,
            int slotId,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        Log.d(TAG, "call sendMultipartTextWithExtraParamsGemini");
        if (isValidParameters(destAddr, parts, sentIntents) == false) {
            return;
        }

        if (extraParams == null) {
            Log.d(TAG, "bundle is null");
            return;
        }

        String serviceName = getSmsServiceName(slotId);
        Log.d(TAG, "service name is " + serviceName);
        if (parts.size() > 1) {
            try {
                ISms service = ISms.Stub.asInterface(ServiceManager.getService(serviceName));
                if (service != null) {
                    service.sendMultipartTextWithExtraParams(destAddr, scAddr, parts, extraParams,
                            sentIntents, deliveryIntents);
                }
            } catch (RemoteException e) {
                Log.d(TAG, "fail to call sendMultipartTextWithExtraParamsGemini: " + e);
            }
        } else {
            PendingIntent sentIntent = null;
            PendingIntent deliveryIntent = null;
            if (sentIntents != null && sentIntents.size() > 0) {
                sentIntent = sentIntents.get(0);
            }
            if (deliveryIntents != null && deliveryIntents.size() > 0) {
                deliveryIntent = deliveryIntents.get(0);
            }

            sendTextMessageWithExtraParamsGemini(destAddr, scAddr, parts.get(0),
                    extraParams, slotId, sentIntent, deliveryIntent);
        }
    }

    /**
     * @hide
     */
    public static boolean enableCellBroadcastGemini(int msgId, int slotId) {
        return enableCellBroadcastRangeGemini(msgId, msgId, slotId);
    }

    /**
     * @hide
     */
    public static boolean disableCellBroadcastGemini(int msgId, int slotId) {
        return disableCellBroadcastRangeGemini(msgId, msgId, slotId);
    }

    /**
     * @hide
     */
    public static boolean enableCellBroadcastRangeGemini(int startMsgId, int endMsgId, int slotId) {
        Log.d(TAG, "enable CB range " + startMsgId + "-" + endMsgId + ", slot = " + slotId);
        boolean result = false;

        String serviceName = getSmsServiceName(slotId);
        try {
            ISms service = ISms.Stub.asInterface(ServiceManager.getService(serviceName));
            if (service != null) {
                result = service.enableCellBroadcastRange(startMsgId, endMsgId);
                Log.d(TAG, "enable CB range: " + result);
            } else {
                Log.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException e) {
            Log.d(TAG, "fail to enable CB range");
            result = false;
        }

        return result;
    }

    /**
     * @hide
     */
    public static boolean disableCellBroadcastRangeGemini(int startMsgId, int endMsgId, int slotId) {
        Log.d(TAG, "disable CB range " + startMsgId + "-" + endMsgId + ", slot = " + slotId);
        boolean result = false;

        String serviceName = getSmsServiceName(slotId);
        try {
            ISms service = ISms.Stub.asInterface(ServiceManager.getService(serviceName));
            if (service != null) {
                result = service.disableCellBroadcastRange(startMsgId, endMsgId);
                Log.d(TAG, "disable CB range: " + result);
            } else {
                Log.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException e) {
            Log.d(TAG, "fail to enable CB range");
            result = false;
        }

        return result;
    }

    /**
     * @hide
     */
    public static boolean activateCellBroadcastSms(boolean activate, int slotId) {
        Log.d(TAG, "activateCellBroadcastSms activate : " + activate + ", slot = " + slotId);
        boolean result = false;

        String serviceName = getSmsServiceName(slotId);
        try {
            ISms service = ISms.Stub.asInterface(ServiceManager.getService(serviceName));
            if (service != null) {
                result = service.activateCellBroadcastSms(activate);
            } else {
                Log.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException e) {
            Log.d(TAG, "fail to activate CB");
            result = false;
        }

        return result;
    }

    /**
     * @hide
     */
    protected static String getSmsFormat(int simId) {
        String isms = getSmsServiceName(simId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                return iccISms.getFormat();
            } else {
                return android.telephony.SmsMessage.FORMAT_3GPP;
            }
        } catch (RemoteException ex) {
            return android.telephony.SmsMessage.FORMAT_3GPP;
        }
    }

    /**
     * @hide
     */
    public static SmsParameters getSmsParametersGemini(int slotId) {
        Log.d(TAG, "[EFsmsp call getSmsParametersGemini");
        String svcName = getSmsServiceName(slotId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(svcName));
            if (iccISms != null) {
                Log.d(TAG, "[EFsmsp to get params from ef");
                return iccISms.getSmsParameters();
            } else {
                Log.d(TAG, "[EFsmsp fail to get service");
                return null;
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "[EFsmsp fail because of RemoteException");
        }

        Log.d(TAG, "[EFsmsp fail to get EFsmsp info");
        return null;
    }

    /**
     * @hide
     */
    public static boolean setSmsParametersGemini(SmsParameters params, int slotId) {
        Log.d(TAG, "[EFsmsp call setSmsParametersGemini");
        String svcName = getSmsServiceName(slotId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(svcName));
            if (iccISms != null) {
                Log.d(TAG, "[EFsmsp to set params into ef");
                return iccISms.setSmsParameters(params);
            } else {
                Log.d(TAG, "[EFsmsp fail to get service");
                return false;
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "[EFsmsp fail because of RemoteException");
        }

        return false;
    }

    /**
     * @hide
     */
    public static int readValidityPeriod(int subscription) {
        // impl
        SmsParameters smsParam = getSmsParametersGemini(subscription);
        if(smsParam != null) {
            return smsParam.vp;
        }

        return -1;
    }

    /**
     * @hide
     */
    public static boolean updateValidityPeriod(int validityperiod, int subscription) {
        // impl
        SmsParameters smsParams = getSmsParametersGemini(subscription);
        if(smsParams != null) {
            smsParams.vp = validityperiod;
            return setSmsParametersGemini(smsParams, subscription);
        }

        return false;
    }

    /**
     * @hide
     */
    public static int copySmsToIcc(byte[] smsc, byte[] pdu, int status, int subscription) {
        // impl
        SimSmsInsertStatus smsStatus = insertRawMessageToIccCardGemini(status, pdu, smsc,
                subscription);
        if(smsStatus != null) {
            int[] index = smsStatus.getIndex();

            if (index != null && index.length > 0) {
                return index[0];
            }
        }

        return -1;
    }

    public static int getMaxEfSmsGemini(int slotId) {
        IccSmsStorageStatus memStatus = GeminiSmsManager.getSmsSimMemoryStatusGemini(slotId);
        if(memStatus != null) {
            return memStatus.mTotal;
        }

        return -1;
    }

    public static boolean updateSmsOnSimReadStatus(int index, boolean read, int subscription) {
        Log.d(TAG, "call updateSmsOnSimReadStatus " + index);

        SmsRawData record = null;
        String svcName = getSmsServiceName(subscription);
        try {
            ISms smsSvc = ISms.Stub.asInterface(ServiceManager.getService(svcName));
            if (smsSvc != null) {
                record = smsSvc.getMessageFromIccEf(index);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        if (record != null) {
            byte[] rawData = record.getBytes();
            int status = rawData[0] & 0xff;
            Log.d(TAG, "sms status is " + status);
            if (status != SmsManager.STATUS_ON_ICC_UNREAD &&
                    status != SmsManager.STATUS_ON_ICC_READ) {
                Log.d(TAG, "non-delivery sms " + status);
                return false;
            } else {
                if ((status == SmsManager.STATUS_ON_ICC_UNREAD && read == false)
                        || (status == SmsManager.STATUS_ON_ICC_READ && read == true)) {
                    Log.d(TAG, "no need to update status");
                    return true;
                } else {
                    Log.d(TAG, "update sms status as " + read);
                    int newStatus = ((read == true) ? SmsManager.STATUS_ON_ICC_READ
                            : SmsManager.STATUS_ON_ICC_UNREAD);
                    return updateMessageOnIccGemini(index, newStatus, rawData, subscription);
                }
            }
        } // end if(record != null)

        Log.d(TAG, "record is null");
        return false;
    }
    
    public static boolean setEtwsConfigGemini(int mode, int slotId) {
        // impl
        Log.d(TAG, "call setEtwsConfigGemini");
        
        boolean ret = false;
        
        String isms = getSmsServiceName(slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                ret = iccISms.setEtwsConfig(mode);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        
        return ret;
    }
}
