package com.mediatek.phone;

import android.provider.Telephony;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

/**
 * Contains utility functions for getting framework resource
 */
public class Utils {

    public static int getStatusResource(int state) {

        Log.i("Utils gemini", "!!!!!!!!!!!!!state is " + state);
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

    public static int getSimColorResource(int color) {

        if ((color >= 0) && (color <= 7)) {
            return Telephony.SIMBackgroundDarkRes[color];
        } else {
            return -1;
        }
    }

    static boolean sSupport3G = false;
    static final int TYPE_VOICECALL = 1;
    static final int TYPE_VIDEOCALL = 2;
    static final int TYPE_SMS = 3;
    static final int TYPE_GPRS = 4;

}
