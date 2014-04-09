
package com.mediatek.encapsulation.android.telephony;

import android.telephony.SmsMessage;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.telephony.SmsMessageEx;

public class EncapsulatedSmsMessageEx {

    public static byte[] getTpdu(SmsMessage msg, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsMessageEx.getDefault().getTpdu(msg, slotId);
        } else {
            return null;
        }
    }

    public static byte[] getSmsc(SmsMessage msg, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsMessageEx.getDefault().getSmsc(msg, slotId);
        } else {
            return null;
        }
    }

}
