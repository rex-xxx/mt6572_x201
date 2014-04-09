package com.mediatek.gemini;

import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.PhoneConstants;

import java.util.Comparator;
/**
 * Contains utility functions for getting framework resource
 */
public class GeminiUtils {

    private static final int COLORNUM = 7;

    static int getStatusResource(int state) {

        switch (state) {
        case PhoneConstants.SIM_INDICATOR_RADIOOFF:
            return com.mediatek.internal.R.drawable.sim_radio_off;
        case PhoneConstants.SIM_INDICATOR_LOCKED:
            return com.mediatek.internal.R.drawable.sim_locked;
        case PhoneConstants.SIM_INDICATOR_INVALID:
            return com.mediatek.internal.R.drawable.sim_invalid;
        case PhoneConstants.SIM_INDICATOR_SEARCHING:
            return com.mediatek.internal.R.drawable.sim_searching;
        case PhoneConstants.SIM_INDICATOR_ROAMING:
            return com.mediatek.internal.R.drawable.sim_roaming;
        case PhoneConstants.SIM_INDICATOR_CONNECTED:
            return com.mediatek.internal.R.drawable.sim_connected;
        case PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
            return com.mediatek.internal.R.drawable.sim_roaming_connected;
        default:
            return -1;
        }
    }

    static int getSimColorResource(int color) {

        if ((color >= 0) && (color <= COLORNUM)) {
            return Telephony.SIMBackgroundDarkRes[color];
        } else {
            return -1;
        }

    }

    static final int TYPE_VOICECALL = 1;
    static final int TYPE_VIDEOCALL = 2;
    static final int TYPE_SMS = 3;
    static final int TYPE_GPRS = 4;
    static final int INTERNET_CALL_COLOR = 8;
    static final int NO_COLOR = -1;
    static final int IMAGE_GRAY = 75;//30% of 0xff in transparent
    static final int ORIGINAL_IMAGE = 255;

    static int sG3SlotID = PhoneConstants.GEMINI_SIM_1;
    static final String EXTRA_SIMID = "simid";
    
    public static class SIMInfoComparable implements Comparator<SIMInfo> {

        @Override
        public int compare(SIMInfo sim1, SIMInfo sim2) {
            return sim1.mSlot - sim2.mSlot;
        }
    }
}
