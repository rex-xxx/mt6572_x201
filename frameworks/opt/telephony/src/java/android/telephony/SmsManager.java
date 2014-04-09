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

package android.telephony;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;
import android.telephony.gemini.GeminiSmsManager;
import android.telephony.gemini.GeminiSmsMessage;
import android.telephony.SimSmsInsertStatus;
import android.telephony.SmsParameters;
import com.mediatek.common.telephony.IccSmsStorageStatus;
/*
 * TODO(code review): Curious question... Why are a lot of these
 * methods not declared as static, since they do not seem to require
 * any local object state?  Presumably this cannot be changed without
 * interfering with the API...
 */

/**
 * Manages SMS operations such as sending data, text, and pdu SMS messages.
 * Get this object by calling the static method SmsManager.getDefault().
 */
public final class SmsManager {
    /** Singleton object constructed during class initialization. */
    private static final SmsManager sInstance = new SmsManager();
    /* mtk added by mtk80589 in 2011.11.16 */
    private static final String TAG = "SMS";

    private static int lastReceivedSmsSimId = PhoneConstants.GEMINI_SIM_1;
    
    private static final int TEST_MODE_CTA = 1;
    private static final int TEST_MODE_FTA = 2;
    private static final int TEST_MODE_IOT = 3;
    private static final int TEST_MODE_UNKNOWN = -1;
    
    private static final String TEST_MODE_PROPERTY_KEY = "gsm.gcf.testmode";
    private static final String TEST_MODE_PROPERTY_KEY2 = "gsm.gcf.testmode2";
    
    private int testMode = 0;
    /* mtk added by mtk80589 in 2011.11.16 */

    /**
     * Send a text based SMS.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
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
    public void sendTextMessage(
            String destinationAddress, String scAddress, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        // mtk modified by mtk80589 in 2012.07.16
        GeminiSmsManager.sendTextMessageGemini(
                destinationAddress, scAddress, text, getDefaultSim(),
                sentIntent, deliveryIntent);
    }

    /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     *
     * @throws IllegalArgumentException if text is null
     */
    public ArrayList<String> divideMessage(String text) {
        if (null == text) {
            throw new IllegalArgumentException("text is null");
        }
        return SmsMessage.fragmentText(text);
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
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK</code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applications,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     *
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public void sendMultipartTextMessage(
            String destinationAddress, String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        // impl
        // mtk modified by mtk80589 in 2012.07.16
        GeminiSmsManager.sendMultipartTextMessageGemini(
                destinationAddress, scAddress, parts, getDefaultSim(),
                sentIntents, deliveryIntents);
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
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
     * @throws IllegalArgumentException if destinationAddress or data are empty
     */
    public void sendDataMessage(
            String destinationAddress, String scAddress, short destinationPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        // mtk modified by mtk80589 in 2012.07.16
        GeminiSmsManager.sendDataMessageGemini(
                destinationAddress, scAddress, destinationPort, data, getDefaultSim(),
                sentIntent, deliveryIntent);
    }

    /**
     * Get the default instance of the SmsManager
     *
     * @return the default instance of the SmsManager
     */
    public static SmsManager getDefault() {
        return sInstance;
    }

    private SmsManager() {
        /* mtk added by mtk80589 in 2011.11.16 */
        // get test mode from SystemProperties
        try {
            if(getDefaultSim() == PhoneConstants.GEMINI_SIM_1) {
                Log.d(TAG, "SM-constructor: get test mode from SIM 1");
                testMode = Integer.valueOf(SystemProperties.get(TEST_MODE_PROPERTY_KEY)).intValue();
            } else {
                Log.d(TAG, "SM-constructor: get test mode from SIM 2");
                // testMode = Integer.valueOf(SystemProperties.get(TEST_MODE_PROPERTY_KEY2)).intValue();
                testMode = Integer.valueOf(SystemProperties.get(TEST_MODE_PROPERTY_KEY)).intValue();
            }
        } catch(NumberFormatException e) {
            Log.d(TAG, "SM-constructor: invalid property value");
            testMode = TEST_MODE_UNKNOWN;
        }
        Log.d(TAG, "SM-constructor: test mode is " + testMode);
        /* mtk added by mtk80589 in 2011.11.16 */
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
     * @return true for success
     *
     * {@hide}
     */
    public boolean copyMessageToIcc(byte[] smsc, byte[] pdu, int status) {
        if (null == pdu) {
            throw new IllegalArgumentException("pdu is NULL");
        }
        // impl
        // mtk modified by mtk80589 in 2012.07.16
        return GeminiSmsManager.copyMessageToIccGemini(
                smsc, pdu, status, getDefaultSim());
    }

    /**
     * Delete the specified message from the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param messageIndex is the record index of the message on ICC
     * @return true for success
     *
     * {@hide}
     */
    public boolean
    deleteMessageFromIcc(int messageIndex) {
        // impl
        // mtk modified by mtk80589 in 2012.07.16
        return GeminiSmsManager.deleteMessageFromIccGemini(
                messageIndex, getDefaultSim());
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
     * @return true for success
     *
     * {@hide}
     */
    public boolean updateMessageOnIcc(int messageIndex, int newStatus, byte[] pdu) {
        // impl
        // mtk modified by mtk80589 in 2012.07.16
        return GeminiSmsManager.updateMessageOnIccGemini(
                messageIndex, newStatus, pdu, getDefaultSim());
    }

    /**
     * Retrieves all messages currently stored on ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects
     *
     * {@hide}
     */
    public static ArrayList<SmsMessage> getAllMessagesFromIcc() {
        List<SmsRawData> records = null;

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEf();
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return createMessageListFromRawRecords(records);
    }

    /**
     * Enable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier. Note that if two different clients enable the same
     * message identifier, they must both disable it for the device to stop
     * receiving those messages. All received messages will be broadcast in an
     * intent with the action "android.provider.Telephony.SMS_CB_RECEIVED".
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param messageIdentifier Message identifier as specified in TS 23.041
     * @return true if successful, false otherwise
     * @see #disableCellBroadcast(int)
     *
     * {@hide}
     */
    public boolean enableCellBroadcast(int messageIdentifier) {
        boolean success = false;

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                success = iccISms.enableCellBroadcast(messageIdentifier);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Disable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier. Note that if two different clients enable the same
     * message identifier, they must both disable it for the device to stop
     * receiving those messages.
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param messageIdentifier Message identifier as specified in TS 23.041
     * @return true if successful, false otherwise
     *
     * @see #enableCellBroadcast(int)
     *
     * {@hide}
     */
    public boolean disableCellBroadcast(int messageIdentifier) {
        boolean success = false;

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                success = iccISms.disableCellBroadcast(messageIdentifier);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Enable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier range. Note that if two different clients enable the same
     * message identifier, they must both disable it for the device to stop
     * receiving those messages. All received messages will be broadcast in an
     * intent with the action "android.provider.Telephony.SMS_CB_RECEIVED".
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param startMessageId first message identifier as specified in TS 23.041
     * @param endMessageId last message identifier as specified in TS 23.041
     * @return true if successful, false otherwise
     * @see #disableCellBroadcastRange(int, int)
     *
     * @throws IllegalArgumentException if endMessageId < startMessageId
     * {@hide}
     */
    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId) {
        boolean success = false;

        if (endMessageId < startMessageId) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                success = iccISms.enableCellBroadcastRange(startMessageId, endMessageId);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Disable reception of cell broadcast (SMS-CB) messages with the given
     * message identifier range. Note that if two different clients enable the same
     * message identifier, they must both disable it for the device to stop
     * receiving those messages.
     * Note: This call is blocking, callers may want to avoid calling it from
     * the main thread of an application.
     *
     * @param startMessageId first message identifier as specified in TS 23.041
     * @param endMessageId last message identifier as specified in TS 23.041
     * @return true if successful, false otherwise
     *
     * @see #enableCellBroadcastRange(int, int)
     *
     * @throws IllegalArgumentException if endMessageId < startMessageId
     * {@hide}
     */
    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId) {
        boolean success = false;

        if (endMessageId < startMessageId) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                success = iccISms.disableCellBroadcastRange(startMessageId, endMessageId);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return success;
    }

    /**
     * Create a list of <code>SmsMessage</code>s from a list of RawSmsData
     * records returned by <code>getAllMessagesFromIcc()</code>
     *
     * @param records SMS EF records, returned by
     *   <code>getAllMessagesFromIcc</code>
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects.
     */
    private static ArrayList<SmsMessage> createMessageListFromRawRecords(List<SmsRawData> records) {
        ArrayList<SmsMessage> messages = new ArrayList<SmsMessage>();
        if (records != null) {
            int count = records.size();
            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);
                // List contains all records, including "free" records (null)
                if (data != null) {
                    SmsMessage sms = SmsMessage.createFromEfRecord(i+1, data.getBytes());
                    if (sms != null) {
                        messages.add(sms);
                    }
                }
            }
        }
        return messages;
    }

    // see SmsMessage.getStatusOnIcc

    /** Free space (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_FREE      = 0;

    /** Received and read (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_READ      = 1;

    /** Received and unread (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_UNREAD    = 3;

    /** Stored and sent (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_SENT      = 5;

    /** Stored and unsent (TS 51.011 10.5.3 / 3GPP2 C.S0023 3.4.27). */
    static public final int STATUS_ON_ICC_UNSENT    = 7;

    // SMS send failure result codes

    /** Generic failure cause */
    static public final int RESULT_ERROR_GENERIC_FAILURE    = 1;
    /** Failed because radio was explicitly turned off */
    static public final int RESULT_ERROR_RADIO_OFF          = 2;
    /** Failed because no pdu provided */
    static public final int RESULT_ERROR_NULL_PDU           = 3;
    /** Failed because service is currently unavailable */
    static public final int RESULT_ERROR_NO_SERVICE         = 4;
    /** Failed because we reached the sending queue limit.  {@hide} */
    static public final int RESULT_ERROR_LIMIT_EXCEEDED     = 5;
    /** Failed because FDN is enabled. {@hide} */
    static public final int RESULT_ERROR_FDN_CHECK_FAILURE  = 6;

    // mtk added by mtk80589 in 2012.07.16
    /*
    * @hide
    */
    static public final int RESULT_ERROR_SIM_MEM_FULL = 7;
    /*
    * @hide
    */
    static public final int RESULT_ERROR_SUCCESS = 0;
    /*
    * @hide
    */
    static public final int RESULT_ERROR_INVALID_ADDRESS = 8;
    // mtk added end

    // mtk added by mtk80589 in 2012.07.16
    // for SMS validity period feature
    /**
    * @hide
    */
    public static final String EXTRA_PARAMS_VALIDITY_PERIOD = "validity_period";
    
    /**
    * @hide
    */
    public static final String EXTRA_PARAMS_ENCODING_TYPE = "encoding_type";
    
    /**
    * @hide
    */
    public static final int VALIDITY_PERIOD_NO_DURATION = -1;
    
    /**
    * @hide
    */
    public static final int VALIDITY_PERIOD_ONE_HOUR = 11; // (VP + 1) * 5 = 60 Mins
    
    /**
    * @hide
    */
    public static final int VALIDITY_PERIOD_SIX_HOURS = 71; // (VP + 1) * 5 = 6 * 60 Mins
    
    /**
    * @hide
    */
    public static final int VALIDITY_PERIOD_TWELVE_HOURS = 143; // (VP + 1) * 5 = 12 * 60 Mins
    
    /**
    * @hide
    */
    public static final int VALIDITY_PERIOD_ONE_DAY = 167; // 12 + (VP - 143) * 30 Mins = 24 Hours
    
    /**
    * @hide
    */
    public static final int VALIDITY_PERIOD_MAX_DURATION = 255; // (VP - 192) Weeks
    // mtk added by mtk80589 in 2012.07.16

    /**
     * Send a text based SMS to a specified application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param destinationPort the port to deliver the message to
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
    public void sendTextMessage( 
            String destinationAddress, String scAddress, String text,
            short destinationPort, PendingIntent sentIntent, 
            PendingIntent deliveryIntent) {
        // impl 
        GeminiSmsManager.sendTextMessageGemini(
                destinationAddress, scAddress, text, destinationPort, getDefaultSim(),
                sentIntent, deliveryIntent);
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
     * @throws IllegalArgumentException if destinationAddress or data are empty\
     */
    public void sendMultipartTextMessage(
            String destinationAddress, String scAddress, 
            ArrayList<String> parts, short destinationPort, 
            ArrayList<PendingIntent> sentIntents, 
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        GeminiSmsManager.sendMultipartTextMessageGemini(
                destinationAddress, scAddress, parts, destinationPort, getDefaultSim(),
                sentIntents, deliveryIntents);
    }

    // mtk added by mtk80589 in 2012.07.16
    // for DM registration feature
    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param originalPort the port to deliver the message from
     * @param data the body of the message to send
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
     *
     * @hide
     */
    public void sendDataMessage(
        String destinationAddress, String scAddress, short destinationPort, short originalPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl 
        GeminiSmsManager.sendDataMessageGemini(
                destinationAddress, scAddress, destinationPort, originalPort, data, getDefaultSim(),
                sentIntent, deliveryIntent);
    }

    // mtk added by mtk80589 in 2012.07.16
    /**
     * Send a multi-part data based SMS.  The callee should have already
     * divided the message into correctly sized parts
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param data the array of data messages body to send
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
    public void sendMultipartDataMessage(
            String destinationAddress, String scAddress, short destinationPort,
            byte[][] data, ArrayList<PendingIntent> sentIntents, 
            ArrayList<PendingIntent> deliveryIntents) {
        //getDefault SIM, 
        GeminiSmsManager.sendMultipartDataMessageGemini(
                destinationAddress, scAddress, destinationPort, data, getDefaultSim(),
                sentIntents, deliveryIntents);
    }
    // mtk added by mtk80589 in 2012.07.16
    /**
     * Copy a text SMS to the ICC.
     *
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @return success or not
     *
     * @hide
     */
    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp) {
        // impl 
        return GeminiSmsManager.copyTextMessageToIccCardGemini(scAddress, address, text, status, timestamp, getDefaultSim());
    }

    // mtk added by mtk80589 in 2012.07.16
    /**
     * Judge if SMS subsystem is ready or not
     * 
     * @return true for success
     *
     * @hide
     */
    public boolean isSmsReady() {
        // impl 
        return GeminiSmsManager.isSmsReadyGemini(getDefaultSim());    
    }

    // mtk added by mtk80589 in 2012.07.16
    /**
     * Set the memory storage status of the SMS
     * This function is used for FTA test only
     * 
     * @param status false for storage full, true for storage available
     *
     * @hide
     */
    public void setSmsMemoryStatus(boolean status) {
        boolean isTestIccCard = false;        
        
        try{
            ITelephony telephony = ITelephony.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (telephony != null && lastReceivedSmsSimId >= 0) {
                isTestIccCard = telephony.isTestIccCard();
            } 
        } catch(RemoteException ex) {
            // This shouldn't happen in the normal case
            Log.d(TAG, "setSmsMemoryStatus, remoteException: " + ex.getMessage());
        } catch (NullPointerException ex) {
            // This could happen before phone restarts due to crashing
            Log.d(TAG, "setSmsMemoryStatus, NullPointerException: " + ex.getMessage());
        }

        if (isTestIccCard) {
            /* for FTA test, we need to send the status to the
            * SIM card which receiving the last incoming SMS
            */
            GeminiSmsManager.setSmsMemoryStatusGemini(status, lastReceivedSmsSimId);
        } else if(testMode == TEST_MODE_FTA) {
            GeminiSmsManager.setSmsMemoryStatusGemini(status, lastReceivedSmsSimId);
        } else {
            //getDefault SIM, 
            GeminiSmsManager.setSmsMemoryStatusGemini(status, getDefaultSim());
        }
    }

    // mtk added by mtk80589 in 2012.07.16
    /**
     * Set the last Incoming Sms SIM Id
     * This function is used for FTA test only
     * 
     * @param simId the sim ID where the last incoming SMS comes from 
     *
     * @hide
     */
    public void setLastIncomingSmsSimId(int simId) {
        if (simId == PhoneConstants.GEMINI_SIM_1 || simId == PhoneConstants.GEMINI_SIM_2) {
            lastReceivedSmsSimId = simId;
        }
    }

    // mtk added by mtk80589 in 2012.07.16
    /**
     * Get SMS SIM Card memory's total and used number
     *
     * @return <code>IccSmsStorageStatus</code> object
     *
     * @hide
     */
    public IccSmsStorageStatus getSmsSimMemoryStatus() {
        return GeminiSmsManager.getSmsSimMemoryStatusGemini(getDefaultSim());
    }

    // mtk added by mtk80589 in 2012.07.16
    /**
     * Get the default SIM id
     */
    private int getDefaultSim() {
        return TelephonyManager.getDefault().getSmsDefaultSim();
    }

    // mtk added [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    /**
     * Send a text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param encodingType the encoding type of message(gsm 7-bit, unicode or automatic)
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
     * @hide
     */
    public void sendTextMessageWithEncodingType(
            String destAddr,
            String scAddr,
            String text,
            int encodingType,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        GeminiSmsManager.sendTextMessageWithEncodingTypeGemini(
                destAddr, scAddr, text, encodingType,
                getDefaultSim(), sentIntent, deliveryIntent);
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
     * @hide
     */
    public void sendMultipartTextMessageWithEncodingType(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            int encodingType,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        GeminiSmsManager.sendMultipartTextMessageWithEncodingTypeGemini(
                destAddr, scAddr, parts, encodingType,
                getDefaultSim(), sentIntents, deliveryIntents);
    }

    /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @param encodingType text encoding type(7-bit, 16-bit or automatic)
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @hide
     */
    public ArrayList<String> divideMessage(String text, int encodingType) {
        Log.d(TAG, "call divideMessage, encoding = " + encodingType);
        ArrayList<String> ret = SmsMessage.fragmentText(text, encodingType);
        Log.d(TAG, "divideMessage: size = " + ret.size());
        return ret;
    }
    // mtk added end

    // mtk added by mtk80589 in 2012.07.16
    /**
     * insert a text SMS to the ICC.
     *
     * @param scAddress Service center address
     * @param address   Destination address or original address
     * @param text      List of message text
     * @param status    message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *                  STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param timestamp Timestamp when service center receive the message
     * @return SimSmsInsertStatus
     * @hide
     */
    public SimSmsInsertStatus insertTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp) {
        return GeminiSmsManager.insertTextMessageToIccCardGemini(scAddress, address, text, status, timestamp, getDefaultSim());
    }
    
    /**
     * Copy a raw SMS PDU to the ICC.
     *
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param pdu the raw PDU to store
     * @param smsc encoded smsc service center
     * @return SimSmsInsertStatus
     * @hide
     */
    public SimSmsInsertStatus insertRawMessageToIccCard(int status, byte[] pdu, byte[] smsc) {
        //impl
        return GeminiSmsManager.insertRawMessageToIccCardGemini(status, pdu, smsc, getDefaultSim());
    }
    // mtk added end
    
    // mtk added by mtk80589 in 2012.07.16
    /**
     * Send an SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @hide
     */
    public void sendTextMessageWithExtraParams(
            String destAddr,
            String scAddr,
            String text,
            Bundle extraParams,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        //impl
        GeminiSmsManager.sendTextMessageWithExtraParamsGemini(destAddr, scAddr, text, extraParams,
                getDefaultSim(), sentIntent, deliveryIntent);
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
    public void sendMultipartTextMessageWithExtraParams(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            Bundle extraParams,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        //impl
        GeminiSmsManager.sendMultipartTextMessageWithExtraParamsGemini(
                destAddr, scAddr, parts, extraParams,
                getDefaultSim(), sentIntents, deliveryIntents);
    }
    // mtk added end
    
    // mtk added for LGE by mtk80589 in 2012.07.16
    /**
    * @hide
    */
    public SmsParameters getSmsParameters() {
        return GeminiSmsManager.getSmsParametersGemini(getDefaultSim());
    }
    
    /**
    * @hide
    */
    public boolean setSmsParameters(SmsParameters params) {
        return GeminiSmsManager.setSmsParametersGemini(params, getDefaultSim());
    }

    /**
    * @hide
    */
    public int getDefaultSubscription() {
        return getDefaultSim();
    }

    /**
    * @hide
    */
    public int getMaxEfSms() {
        IccSmsStorageStatus memStat = getSmsSimMemoryStatus();
        if(memStat != null) {
            return memStat.mTotal;
        }

        return -1;
    }

    /**
    * @hide
    */ 
    public int readValidityPeriod (){
        return GeminiSmsManager.readValidityPeriod (getDefaultSim());
    }

    /**
    * @hide
    */
    public boolean updateValidityPeriod(int validityperiod){
        return GeminiSmsManager.updateValidityPeriod (validityperiod, getDefaultSim());
    }

    /**
    * @hide
    */
    public int copySmsToIcc (byte[] smsc, byte[] pdu, int status){
        return GeminiSmsManager.copySmsToIcc (smsc, pdu, status, getDefaultSim());
    }
    
    /**
    * @hide
    */
    public boolean updateSmsOnSimReadStatus(int index, boolean read) {
        return GeminiSmsManager.updateSmsOnSimReadStatus(index, read, getDefaultSim());
    }
    
    /**
    * @hide
    */
    public boolean setEtwsConfig(int mode) {
        return GeminiSmsManager.setEtwsConfigGemini(mode, getDefaultSim());
    }
    // mtk added end
}
