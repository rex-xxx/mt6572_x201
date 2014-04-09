
package com.mediatek.common.telephony;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public interface ISmsManagerExt {

    public static final int STATUS_ON_ICC_FREE = 0;

    public static final int STATUS_ON_ICC_READ = 1;

    public static final int STATUS_ON_ICC_UNREAD = 3;

    public static final int STATUS_ON_ICC_SENT = 5;

    public static final int STATUS_ON_ICC_UNSENT = 7;

    // SMS send failure result codes

    /** Generic failure cause */
    public static final int RESULT_ERROR_GENERIC_FAILURE = 1;
    /** Failed because radio was explicitly turned off */
    public static final int RESULT_ERROR_RADIO_OFF = 2;
    /** Failed because no pdu provided */
    public static final int RESULT_ERROR_NULL_PDU = 3;
    /** Failed because service is currently unavailable */
    public static final int RESULT_ERROR_NO_SERVICE = 4;
    /** Failed because we reached the sending queue limit. {@hide} */
    public static final int RESULT_ERROR_LIMIT_EXCEEDED = 5;
    /** Failed because FDN is enabled. {@hide} */
    public static final int RESULT_ERROR_FDN_CHECK_FAILURE = 6;

    public static final int RESULT_ERROR_SIM_MEM_FULL = 7;

    public static final int RESULT_ERROR_SUCCESS = 0;

    public static final int RESULT_ERROR_INVALID_ADDRESS = 8;

    public static final String EXTRA_PARAMS_VALIDITY_PERIOD = "validity_period";

    public static final String EXTRA_PARAMS_ENCODING_TYPE = "encoding_type";

    public static final int VALIDITY_PERIOD_NO_DURATION = -1;

    public static final int VALIDITY_PERIOD_ONE_HOUR = 11; // (VP + 1) * 5 = 60
    // Mins

    public static final int VALIDITY_PERIOD_SIX_HOURS = 71; // (VP + 1) * 5 = 6
    // * 60 Mins

    public static final int VALIDITY_PERIOD_TWELVE_HOURS = 143; // (VP + 1) * 5
    // = 12 * 60
    // Mins

    public static final int VALIDITY_PERIOD_ONE_DAY = 167; // 12 + (VP - 143) *
    // 30 Mins = 24 Hours

    public static final int VALIDITY_PERIOD_MAX_DURATION = 255; // (VP - 192)

    // Weeks

    public ArrayList<String> divideMessage(String text);

    public void sendTextMessage(String destinationAddress,
            String scAddress, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, int slotId);

    public void sendMultipartTextMessage(String destinationAddress,
            String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, int slotId);

    public void sendDataMessage(String destinationAddress,
            String scAddress, short destinationPort, byte[] data,
            PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId);

    public void sendDataMessage(String destinationAddress,
            String scAddress, short destinationPort, short originalPort,
            byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent, int slotId);

    public boolean deleteMessageFromIcc(int messageIndex, int slotId);

    public boolean deleteAllMessagesFromIcc(int slotId);

    public boolean updateMessageOnIcc(int messageIndex, int newStatus,
            byte[] pdu, int slotId);

    // should provide a more simple method that doesn't need param pdu

    public ArrayList<SmsMessage> getAllMessagesFromIcc(int slotId);

    public boolean isSmsReady(int slotId);

    public IccSmsStorageStatus getIccSmsStorageStatus(int slotId);

    // SmsMemoryStatus --> IccSmsStorageStatus
    // getSmsSimMemoryStatusGemini --> getIccSmsStorageStatusGemini

    public void sendTextMessageWithExtraParams(String destAddr,
            String scAddr, String text, Bundle extraParams,
            PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId);

    // should move relative constants from SmsManager.java to another file

    public void sendMultipartTextMessageWithExtraParams(String destAddr,
            String scAddr, ArrayList<String> parts, Bundle extraParams,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, int slotId);

    public int getValidityPeriodFromIccCard(int slotId);

    // readValidityPeriod --> getValidityPeriodFromIccCard

    public boolean setValidityPeroidToIccCard(int validityPeriod, int slotId);

    // updateValidityPeriod --> setValidityPeroidToIccCard

    public int copySmsPduToIcc(byte[] smsc, byte[] pdu, int status, int slotId);
}
