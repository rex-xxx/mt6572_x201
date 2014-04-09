package com.mediatek.telephony;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import android.telephony.PhoneNumberUtils;
import android.telephony.SimSmsInsertStatus;
import android.telephony.SmsManager;
import android.telephony.SmsMemoryStatus;
import android.telephony.SmsMessage;
import android.telephony.SmsParameters;
import android.telephony.TelephonyManager;

import android.telephony.gemini.GeminiSmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.content.Context;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.ITelephony;

import com.mediatek.common.telephony.ISmsManagerExt;
import com.mediatek.common.telephony.IccSmsStorageStatus;

/**
 * Manages SMS operations such as sending data, text, and PDU SMS messages.
 */
public class SmsManagerEx implements ISmsManagerExt {

    private static final String TAG = "SMS";

    private static final SmsManagerEx sInstance = new SmsManagerEx();
    private static int lastReceivedSmsSimId = PhoneConstants.GEMINI_SIM_1;

    private static final int TEST_MODE_CTA = 1;
    private static final int TEST_MODE_FTA = 2;
    private static final int TEST_MODE_IOT = 3;
    private static final int TEST_MODE_UNKNOWN = -1;
    private static final String TEST_MODE_PROPERTY_KEY = "gsm.gcf.testmode";
    private static final String TEST_MODE_PROPERTY_KEY2 = "gsm.gcf.testmode2";
    private int testMode = 0;

    private SmsManagerEx() {
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
    }
    
    /**
     * Gets the default instance of the SmsManagerEx
     * 
     * @return the default instance of the SmsManagerEx
     */
    public static SmsManagerEx getDefault() {
        return sInstance;
    }

    /**
     * Get the default SIM id
     */
    private int getDefaultSim() {
        return TelephonyManager.getDefault().getSmsDefaultSim();
    }

    /**
     * Divides a message text into several fragments, no bigger than the
     * maximum SMS message size.
     * 
     * @param text Original message. Must not be null.
     * @return <code>ArrayList</code> of strings in order to comprise the
     *         original message
     */
    public ArrayList<String> divideMessage(String text) {
        return SmsMessage.fragmentText(text);
    }

    /**
     * Sends a text based SMS.
     * 
     * @param destinationAddress Address to send the message to
     * @param scAddress Service center address or null to use the current
     *            default SMSC
     * @param text Body of the message to send
     * @param sentIntent If not NULL, this <code>PendingIntent</code> will be
     *            broadcasted when the message is sucessfully sent or failed. The
     *            result code will be
     *            <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *            <code>RESULT_ERROR_RADIO_OFF</code><br>
     *            <code>RESULT_ERROR_NULL_PDU</code><br>
     *            For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent
     *            may include the extra "errorCode" containing a radio
     *            technology specific value, generally only useful for
     *            troubleshooting.<br>
     *            The per-application based SMS control checks sentIntent. If
     *            sentIntent is NULL the caller will be checked against all
     *            unknown applications, which will cause smaller number of SMS to be
     *            sent in the checking period.
     * @param deliveryIntent If not NULL this <code>PendingIntent</code> is
     *            broadcast when the message is delivered to the recipient. The
     *            raw PDU of the status report is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     * @throws IllegalArgumentException If destinationAddress or text are empty
     */
    public void sendTextMessage(String destinationAddress, String scAddress,
            String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, int slotId) {
        // impl
        Log.d(TAG, "call sendTextMessage");
        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }

        String isms = getSmsServiceName(slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager
                    .getService(isms));
            if (iccISms != null) {
                iccISms.sendText(destinationAddress, scAddress, text,
                        sentIntent, deliveryIntent);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "sendTextMessage, RemoteException!");
        }
    }

    /**
     * Sends a multi-part text based SMS. The callee should have already divided
     * the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     * 
     * @param destinationAddress Address to send the message to
     * @param scAddress Service center address or null to use the current
     *            default SMSC
     * @param parts <code>ArrayList</code> of strings in order to
     *            comprise the original message
     * @param sentIntents If not null, an <code>ArrayList</code> of
     *            <code>PendingIntent</code>s (one for each message part) that
     *            will be broadcasted when the corresponding message part is
     *            sent. The result code will be
     *            <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *            <code>RESULT_ERROR_RADIO_OFF</code><br>
     *            <code>RESULT_ERROR_NULL_PDU</code><br>
     *            For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent
     *            may include the extra "errorCode" containing a radio
     *            technology specific value, generally only useful for
     *            troubleshooting.<br>
     *            The per-application based SMS control checks sentIntent. If
     *            sentIntent is NULL the caller will be checked against all
     *            unknown applicaitons, which will cause smaller number of SMS to be
     *            sent in the checking period.
     * @param deliveryIntents If not null, an <code>ArrayList</code> of
     *            <code>PendingIntent</code>s (one for each message part) will
     *            be broadcasted when the corresponding message part is
     *            delivered to the recipient. The raw PDU of the status report
     *            is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     * @throws IllegalArgumentException If destinationAddress or data are empty
     */
    public void sendMultipartTextMessage(String destinationAddress,
            String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, int slotId) {
        // impl
        Log.d(TAG, "call sendMultipartTextMessage");
        if (!isValidParameters(destinationAddress, parts, sentIntents)) {
            return;
        }

        String isms = getSmsServiceName(slotId);
        if (parts.size() > 1) {
            try {
                ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
                if (iccISms != null) {
                    iccISms.sendMultipartText(destinationAddress, scAddress, parts,
                            sentIntents, deliveryIntents);
                }
            } catch (RemoteException ex) {
                Log.d(TAG, "sendMultipartTextMessage, RemoteException!");
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
            sendTextMessage(destinationAddress, scAddress, text, sentIntent,
                            deliveryIntent, slotId);
        }
    }

    /**
     * Sends a data based SMS to specific application port.
     * 
     * @param destinationAddress Address to send the message to
     * @param scAddress Service center address or null to use the current
     *            default SMSC
     * @param destinationPort Port to deliver the message to
     * @param data Body of the message to send
     * @param sentIntent If not NULL this <code>PendingIntent</code> will be
     *            broadcasted when the message is sucessfully sent or failed. The
     *            result code will be
     *            <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *            <code>RESULT_ERROR_RADIO_OFF</code><br>
     *            <code>RESULT_ERROR_NULL_PDU</code><br>
     *            For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent
     *            may include the extra "errorCode" containing a radio
     *            technology specific value, generally only useful for
     *            troubleshooting.<br>
     *            The per-application based SMS control checks sentIntent. If
     *            sentIntent is NULL the caller will be checked against all
     *            unknown applicaitons, which will cause smaller number of SMS to be
     *            sent in the checking period.
     * @param deliveryIntent If not NULL this <code>PendingIntent</code> will be
     *            broadcasted when the message is delivered to the recipient. The
     *            raw PDU of the status report is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     * @throws IllegalArgumentException If destinationAddress or data are empty
     */
    public void sendDataMessage(String destinationAddress, String scAddress,
            short destinationPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent, int slotId) {
        // impl
        Log.d(TAG, "call sendDataMessage");
        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {

            return;
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        String isms = getSmsServiceName(slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                iccISms.sendData(destinationAddress, scAddress, destinationPort & 0xFFFF,
                        data, sentIntent, deliveryIntent);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "sendDataMessage, RemoteException!");
        }
    }

    /**
     * Sends a data based SMS to specific application port with the original port.
     * 
     * @param destinationAddress Address to send the message to
     * @param scAddress Service center address or null to use the current
     *            default SMSC
     * @param destinationPort Port to deliver the message to
     * @param originalPort Port to deliver the message from
     * @param data Body of the message to be sent
     * @param sentIntent If not NULL this <code>PendingIntent</code> will be
     *            broadcasted when the message is sucessfully sent, or failed. The
     *            result code will be
     *            <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *            <code>RESULT_ERROR_RADIO_OFF</code><br>
     *            <code>RESULT_ERROR_NULL_PDU</code><br>
     *            For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent
     *            may include the extra "errorCode" containing a radio
     *            technology specific value, generally only useful for
     *            troubleshooting.<br>
     *            The per-application based SMS control checks sentIntent. If
     *            sentIntent is NULL the caller will be checked against all
     *            unknown applicaitons, which will cause smaller number of SMS to be
     *            sent in the checking period.
     * @param deliveryIntent If not NULL this <code>PendingIntent</code> will be
     *            broadcasted when the message is delivered to the recipient. The
     *            raw PDU of the status report is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     * @throws IllegalArgumentException If destinationAddress or data are empty
     */
    public void sendDataMessage(String destinationAddress, String scAddress,
            short destinationPort, short originalPort, byte[] data,
            PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId) {
        // impl
        Log.d(TAG, "[xj send data with original port");
        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {

            return;
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }

        String isms = getSmsServiceName(slotId);
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
            Log.d(TAG, "sendDataMessage, RemoteException!");
        }
    }

    /**
     * Deletes specified message from the ICC. ICC (Integrated Circuit Card)
     * is the card of the device. For example, this can be the SIM or USIM for
     * GSM.
     * 
     * @param messageIndex Record index of the message on ICC
     * @param slotId SIM card the user would like to access
     * @return True for success
     * @hide
     */
    public boolean deleteMessageFromIcc(int messageIndex, int slotId) {
        // impl
        Log.d(TAG, "call deleteMessageFromIcc");
        boolean success = false;
        String isms = getSmsServiceName(slotId);

        byte[] pdu = new byte[IccConstants.SMS_RECORD_LENGTH - 1];
        Arrays.fill(pdu, (byte) 0xff);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                success = iccISms.updateMessageOnIccEf(messageIndex,
                        SmsManager.STATUS_ON_ICC_FREE, pdu);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "deleteMessageFromIcc, RemoteException!");
        }

        return success;
    }
    /**
     * Deletes all the messages from the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param slotId SIM card the user would like to access
     * @return True for success
     * @hide
     */
    public boolean deleteAllMessagesFromIcc(int slotId) {
        return deleteMessageFromIcc(-1, slotId);
    }

    /**
     * Updates the specified message on the ICC. ICC (Integrated Circuit Card) is
     * the card of the device. For example, this can be the SIM or USIM for GSM.
     * 
     * @param messageIndex Record index of message to be updated
     * @param newStatus New message status (STATUS_ON_ICC_READ,
     *            STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *            STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu Raw PDU to be stored
     * @param slotId SIM card the user would like to access
     * @return True for success
     * @hide
     */
    public boolean updateMessageOnIcc(int messageIndex, int newStatus,
            byte[] pdu, int slotId) {
        // impl
        Log.d(TAG, "call updateMessageOnIcc");
        boolean success = false;
        String isms = getSmsServiceName(slotId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                success = iccISms.updateMessageOnIccEf(messageIndex, newStatus, pdu);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "updateMessageOnIcc, RemoteException!");
        }

        return success;
    }

    // should provide a more simple method that doesn't need param pdu

    /**
     * Retrieves all messages currently stored on ICC. ICC (Integrated Circuit
     * Card) is the card of the device. For example, this can be the SIM or USIM
     * for GSM.
     * 
     * @param slotId SIM card the user would like to access
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects
     * @hide
     */
    public ArrayList<SmsMessage> getAllMessagesFromIcc(int slotId) {
        // impl
        Log.d(TAG, "call getAllMessagesFromIcc");
        String isms = getSmsServiceName(slotId);
        List<SmsRawData> records = null;

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEf();
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "getAllMessagesFromIcc, RemoteException!");
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
                ret = updateMessageOnIcc(index, SmsManager.STATUS_ON_ICC_READ, data, slotId);
                if (ret) {
                    Log.d(TAG, "update index[" + index + "] to STATUS_ON_ICC_READ");
                } else {
                    Log.d(TAG, "fail to update message status");
                }
            }
        }

        return createMessageListFromRawRecords(records, slotId);

    }

    /**
     * Judges if SMS subsystem in SIM card is ready or not.
     * 
     * @param slotId SIM card ID
     * @return True for success
     * @hide
     */
    public boolean isSmsReady(int slotId) {
        // impl
        Log.d(TAG, "call isSmsReady");
        boolean isReady = false;
        String isms = getSmsServiceName(slotId);

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
     * Gets SMS SIM card memory's total and used number.
     * 
     * @param slotId SIM card ID
     * @return <code>IccSmsStorageStatus</code> object
     * @hide
     */
    public IccSmsStorageStatus getIccSmsStorageStatus(int slotId) {
        // impl
        Log.d(TAG, "call getSmsSimMemoryStatus");
        String isms = getSmsServiceName(slotId);

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

    private static SimSmsInsertStatus insertRawMessageToIccCard(int status, byte[] pdu,
            byte[] smsc, int slotId) {
        // impl
        Log.d(TAG, "call insertRawMessageToIccCard");
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
     * Sends an SMS with specified encoding type.
     * 
     * @param destAddr Address to send the message to
     * @param scAddr SMSC to send the message through, or NULL for the
     *            default SMSC
     * @param text Body of the message to be sent
     * @param extraParams Extra parameters, such as validity period, encoding
     *            type
     * @param sentIntent If not NULL this <code>PendingIntent</code> will be
     *            broadcasted when the message is sucessfully sent, or failed.
     * @param deliveryIntent If not NULL this <code>PendingIntent</code> is
     *            broadcast when the message is delivered to the recipient. The
     *            raw PDU of the status report is in the extended data ("pdu").
     * @param slotId Identifier for SIM card slot
     */
    public void sendTextMessageWithExtraParams(String destAddr, String scAddr,
            String text, Bundle extraParams,
            PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId) {
        // impl
        Log.d(TAG, "call sendTextWithExtraParams");
        if (!isValidParameters(destAddr, text, sentIntent)) {
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
            Log.d(TAG, "fail to call sendTextWithExtraParams: " + e);
        }
    }

    // should move relative constants from SmsManager.java to another file

    /**
     * Sends a multi-part text based SMS with specified encoding type.
     * 
     * @param destAddr Address to send the message to
     * @param scAddr Service center address or null to use the current
     *            default SMSC
     * @param parts <code>ArrayList</code> of strings that, in order,
     *            comprise the original message
     * @param extraParams Extra parameters, such as validity period, encoding
     *            type
     * @param sentIntents If not null, an <code>ArrayList</code> of
     *            <code>PendingIntent</code>s (one for each message part) that
     *            will be broadcasted when the corresponding message part has been
     *            sent.
     * @param deliveryIntents If not null, an <code>ArrayList</code> of
     *            <code>PendingIntent</code>s (one for each message part) that
     *            will be broadcast when the corresponding message part has been
     *            delivered to the recipient. The raw PDU of the status report
     *            is in the extended data ("pdu").
     * @param slotId Identifier for SIM card slot
     */
    public void sendMultipartTextMessageWithExtraParams(String destAddr,
            String scAddr, ArrayList<String> parts, Bundle extraParams,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, int slotId) {
        // impl
        Log.d(TAG, "call sendMultipartTextWithExtraParams");
        if (!isValidParameters(destAddr, parts, sentIntents)) {
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
                Log.d(TAG, "fail to call sendMultipartTextWithExtraParams: " + e);
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

            sendTextMessageWithExtraParams(destAddr, scAddr, parts.get(0),
                    extraParams, sentIntent, deliveryIntent, slotId);
        }
    }

    private static SmsParameters getSmsParameters(int slotId) {
        Log.d(TAG, "[EFsmsp call getSmsParameters");
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

    private static boolean setSmsParameters(SmsParameters params, int slotId) {
        Log.d(TAG, "[EFsmsp call setSmsParameters");
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
     * Gets the validity period of the sms in the sim card.
     * @param slotId Identifier for SIM card slot
     * @return The validity period
     * @hide
     */
    public int getValidityPeriodFromIccCard(int slotId) {
        // impl
        SmsParameters smsParam = getSmsParameters(slotId);
        if (smsParam == null) {
            return 0;
        }
        return smsParam.vp;
    }

    /**
     * Sets the validity period of the sms in the sim card.
     * @param slotId, Identifier for SIM card slot
     * @return True if success
     * @hide
     */
    public boolean setValidityPeroidToIccCard(int validityPeriod, int slotId) {
        // impl
        SmsParameters smsParams = getSmsParameters(slotId);
        if (smsParams == null) {
            return false;
        }
        smsParams.vp = validityPeriod;
        return setSmsParameters(smsParams, slotId);
    }

    /**
     * Copies a raw SMS PDU to the ICC.
     * ICC (Integrated Circuit Card) is the card of the device.
     * For example, this can be the SIM or USIM for GSM.
     *
     * @param smsc The SMSC for this message, or NULL for the default SMSC
     * @param pdu The raw PDU to store
     * @param status Message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @param slotId SIM card the user would like to access
     * @return The index saved in the sim card
     * @hide
     */
    public int copySmsPduToIcc(byte[] smsc, byte[] pdu, int status, int slotId) {
        // impl
        SimSmsInsertStatus smsStatus = insertRawMessageToIccCard(status, pdu, smsc,
                slotId);
        if (smsStatus == null) {
            return -1;
        }
        int[] index = smsStatus.getIndex();

        if (index != null && index.length > 0) {
            return index[0];
        }

        return -1;
    }

    /**
     * Create a list of <code>SmsMessage</code>s from a list of RawSmsData
     * records returned by <code>getAllMessagesFromIcc()</code>.
     * 
     * @param records SMS EF records, returned by
     *            <code>getAllMessagesFromIcc</code>
     * @return <code>ArrayList</code> of <code>SmsMessage</code> objects.
     */
    private static ArrayList<SmsMessage> createMessageListFromRawRecords(List<SmsRawData> records,
            int slotId) {
        // impl
        Log.d(TAG, "call createMessageListFromRawRecords");
        ArrayList<SmsMessage> geminiMessages = null;
        if (records != null) {
            int count = records.size();
            geminiMessages = new ArrayList<SmsMessage>();

            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);

                if (data != null) {
                    GeminiSmsMessage geminiSms =
                            GeminiSmsMessage.createFromEfRecord(i + 1, data.getBytes(), slotId);
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
     * Gets the SMS service name by specific SIM ID.
     * 
     * @param slotId SIM ID
     * @return The SMS service name
     */
    private static String getSmsServiceName(int slotId) {
        if (slotId == PhoneConstants.GEMINI_SIM_1) {
            return "isms";
        } else if (slotId == PhoneConstants.GEMINI_SIM_2) {
            return "isms2";
        } else if (slotId == PhoneConstants.GEMINI_SIM_3) {
            return "isms3";
        } else if (slotId == PhoneConstants.GEMINI_SIM_4) {
            return "isms4";
        } else {
            return null;
        }
    }

    /**
     * Judges if the destination address is a valid SMS address or not, and if
     * the text is null or not.
     * 
     * @param destinationAddress The destination address to which the message be sent
     * @param text The content of shorm message
     * @param sentIntent will be broadcast if the address or the text is invalid
     * @return True for valid parameters
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
     * Judges if the destination address is a valid SMS address or not, and if
     * the text is null or not.
     * 
     * @param destinationAddress The destination address to which the message be sent
     * @param parts The content of shorm message
     * @param sentIntent will be broadcast if the address or the text is invalid
     * @return True for valid parameters
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
                    } catch (CanceledException ex) {
                    }
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
     * Judges if the input destination address is a valid SMS address or not.
     * 
     * @param da The input destination address
     * @return True for success
     */
    private static boolean isValidSmsDestinationAddress(String da) {
        // impl
        String encodeAddress = PhoneNumberUtils.extractNetworkPortion(da);
        if (encodeAddress == null) {
            return true;
        }

        int spaceCount = 0;
        for (int i = 0; i < da.length(); ++i) {
            if (da.charAt(i) == ' ' || da.charAt(i) == '-') {
                spaceCount++;
            }
        }

        return encodeAddress.length() == (da.length() - spaceCount);
    }

    /**
     * Set the memory storage status of the SMS
     * This function is used for FTA test only
     * 
     * @param status false for storage full, true for storage available
     * @param simId the sim card that user wants to access     
     *
     */
    private void setSmsMemoryStatus(boolean status, int simId) {
        // impl
        Log.d(TAG, "call setSmsMemoryStatus");
        String isms = getSmsServiceName(simId);

        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                iccISms.setSmsMemoryStatus(status);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "setSmsMemoryStatus, RemoteException ");
        }
    }
	

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
            setSmsMemoryStatus(status, lastReceivedSmsSimId);
        } else if(testMode == TEST_MODE_FTA) {
            setSmsMemoryStatus(status, lastReceivedSmsSimId);
        } else {
            //getDefault SIM,
            setSmsMemoryStatus(status, getDefaultSim());
        }
    }

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
                sendDataMessage(
                destinationAddress, scAddress, destinationPort, originalPort, data, 
                sentIntent, deliveryIntent, getDefaultSim());
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
     * @hide
     */
    public static int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp, int slotId) {
        Log.d(TAG, "call copyTextMessageToIccCard");
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
     * @hide
     */
    public static void sendTextMessageWithEncodingType(
            String destAddr,
            String scAddr,
            String text,
            int encodingType,
            int slotId,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "call sendTextMessageWithEncodingType, encoding = " + encodingType);

        if (!isValidParameters(destAddr, text, sentIntent)) {
            Log.d(TAG, "the parameters are invalid");
            return;
        }

        String isms = getSmsServiceName(slotId);
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
     * @hide
     */
    public static void sendMultipartTextMessageWithEncodingType(
            String destAddr,
            String scAddr,
            ArrayList<String> parts,
            int encodingType,
            int slotId,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        // impl
        Log.d(TAG, "call sendMultipartTextMessageWithEncodingType, encoding = " + encodingType);

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
            sendTextMessageWithEncodingType(destAddr, scAddr, text, encodingType,
                    slotId, sentIntent, deliveryIntent);
        }
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
}
