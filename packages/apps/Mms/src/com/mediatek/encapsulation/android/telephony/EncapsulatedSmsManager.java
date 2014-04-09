
package com.mediatek.encapsulation.android.telephony;

import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.app.PendingIntent;
import android.telephony.gemini.GeminiSmsManager;
import android.telephony.TelephonyManager;

import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.telephony.SmsManagerEx;
import java.util.ArrayList;
import java.util.List;

public class EncapsulatedSmsManager {
    /** M: MTK ADD */
    public static final int RESULT_ERROR_SUCCESS = 0;
    public static final int RESULT_ERROR_SIM_MEM_FULL = 7;
    public static final int VALIDITY_PERIOD_NO_DURATION = -1;
    public static final int VALIDITY_PERIOD_ONE_HOUR = 11; // (VP + 1) * 5 = 6 Mins
    public static final int VALIDITY_PERIOD_SIX_HOURS = 71; // (VP + 1) * 5 = 6 * 60 Mins
    public static final int VALIDITY_PERIOD_TWELVE_HOURS = 143; // (VP + 1) * 5 = 12 * 60 Mins
    public static final int VALIDITY_PERIOD_ONE_DAY = 167; // 12 + (VP - 143) * 30 Mins = 24 Hours
    public static final int VALIDITY_PERIOD_MAX_DURATION = 255; // (VP - 192) Weeks
    public static final String EXTRA_PARAMS_VALIDITY_PERIOD = "validity_period";

    /** M: MTK ADD */
    public static void setSmsMemoryStatus(boolean status) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            SmsManagerEx.getDefault().setSmsMemoryStatus(status);
        } else {
            /** M: Can not complete for this branch. */
            MmsLog.d("Encapsulation issue", "EncapsulatedSmsManager -- setSmsMemoryStatus()");
        }
    }

    /** M: MTK ADD */
    public static EncapsulatedSmsMemoryStatus getSmsSimMemoryStatus() {
        return EncapsulatedGeminiSmsManager.getSmsSimMemoryStatusGemini(0/* Phone.GEMINI_SIM_1 */);
    }

    /** M: MTK ADD */
    public static int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().copyTextMessageToIccCard(scAddress, address, text,
                    status, timestamp, 0);
        } else {
            return 0;
        }
    }

    /** M: MTK ADD */
    public static ArrayList<String> divideMessage(String text, int encodingType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().divideMessage(text, encodingType);
        } else {
            //Do not use encodingType to divide message as default implement.
            return SmsManager.getDefault().divideMessage(text);
        }
    }

    public static ArrayList<String> divideMessage(String text) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().divideMessage(text);
        } else {
            return null;
        }
    }

    /** M: MTK ADD */
    public static void sendMultipartTextMessageWithEncodingType(String destAddr, String scAddr,
            ArrayList<String> parts, int encodingType, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            SmsManagerEx.getDefault().sendMultipartTextMessageWithEncodingType(destAddr, scAddr,
                    parts, encodingType, 0, sentIntents, deliveryIntents);
        } else {
        }
    }

}
