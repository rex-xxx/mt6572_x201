
package com.mediatek.encapsulation.android.telephony.gemini;

import android.telephony.gemini.GeminiSmsManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.os.Bundle;
import android.app.PendingIntent;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.telephony.ISms;
import com.mediatek.common.telephony.IccSmsStorageStatus;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsMemoryStatus;
import com.mediatek.telephony.SmsManagerEx;

import java.util.List;
import java.util.ArrayList;

/// M: ALPS00510627, SMS Framewrok API refactoring, EncapsulatedGeminiSmsManager -> SmsManagerEx
public class EncapsulatedGeminiSmsManager {

    /** M: MTK ADD */
    public static int copyTextMessageToIccCardGemini(String scAddress, String address,
            List<String> text, int status, long timestamp, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().copyTextMessageToIccCard(scAddress, address, text,
                    status, timestamp, slotId);
        } else {
            return 0;
        }
    }

    /** M: MTK ADD */
    public static EncapsulatedSmsMemoryStatus getSmsSimMemoryStatusGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            IccSmsStorageStatus smsMemoryStatus
                    = SmsManagerEx.getDefault().getIccSmsStorageStatus(simId);
            if (smsMemoryStatus != null) {
                return new EncapsulatedSmsMemoryStatus(smsMemoryStatus);
            } else {
                return null;
            }
        } else {
            /** M: Can not complete for this branch. */
            return null;
        }
    }

    /** M: MTK ADD */
    public static int copyMessageToIccGemini(byte[] smsc, byte[] pdu, int status, int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().copySmsPduToIcc(smsc,pdu,status,simId);
        } else {
            /** M: Can not complete for this branch. */
            int result = SmsManager.getDefault().copyMessageToIcc(smsc, pdu, status) ? 0 : -1;
            return result;
        }
    }

    /** M: MTK ADD */
    public static void sendMultipartTextMessageWithExtraParamsGemini(String destAddr,
            String scAddr, ArrayList<String> parts, Bundle extraParams, int slotId,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
             SmsManagerEx.getDefault().sendMultipartTextMessageWithExtraParams(destAddr, scAddr,
                    parts, extraParams, sentIntents, deliveryIntents, slotId);
        } else {
        }

    }

    /** M: MTK ADD */
    public static void sendMultipartTextMessageWithEncodingTypeGemini(String destAddr,
            String scAddr, ArrayList<String> parts, int encodingType, int slotId,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
             SmsManagerEx.getDefault().sendMultipartTextMessageWithEncodingType(destAddr, scAddr,
                    parts, encodingType, slotId, sentIntents, deliveryIntents);
        } else {
        }
    }
}