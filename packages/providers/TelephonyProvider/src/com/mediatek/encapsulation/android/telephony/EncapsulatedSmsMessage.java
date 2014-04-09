package com.mediatek.encapsulation.android.telephony;

import android.telephony.SmsMessage;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedSmsMessage {

    public static SmsHeader getUserDataHeader(SmsMessage sms) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sms.getUserDataHeader();
        } else {
            return sms.mWrappedSmsMessage.getUserDataHeader();
        }
    }

    public static String getDestinationAddress(SmsMessage sms) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sms.getDestinationAddress();
        } else {
            return sms.mWrappedSmsMessage.getDestinationAddress();
        }
    }

}
