
package com.mediatek.encapsulation.android.telephony.gemini;

import android.telephony.gemini.GeminiSmsManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.os.Bundle;
import android.app.PendingIntent;

import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.telephony.SmsManagerEx;
import java.util.List;
import java.util.ArrayList;

/// M: ALPS00510627, SMS Framewrok API refactoring, EncapsulatedGeminiSmsManager -> SmsManagerEx
public class EncapsulatedGeminiSmsManager {

    /** M: MTK ADD */
    public static ArrayList<SmsMessage> getAllMessagesFromIccGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().getAllMessagesFromIcc(simId);
        } else {
            /** M: Can not complete for this branch. */
            return SmsManager.getAllMessagesFromIcc();
        }
    }

    /** M: MTK ADD */
    public static boolean deleteMessageFromIccGemini(int messageIndex, int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().deleteMessageFromIcc(messageIndex, simId);
        } else {
            /** M: Can not complete for this branch. */
            return SmsManager.getDefault().deleteMessageFromIcc(messageIndex);
        }
    }
}