
package com.mediatek.contacts.util;

public final class ErrorCause {

    public static final int NO_ERROR = 0;

    public static final int SIM_NUMBER_TOO_LONG = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_NUMBER_TOO_LONG; // -1
    public static final int SIM_NAME_TOO_LONG = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_TEXT_TOO_LONG; // -2
    public static final int SIM_STORAGE_FULL = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_STORAGE_FULL; // -3
    public static final int SIM_ICC_NOT_READY = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_NOT_READY; // -4
    public static final int SIM_PASSWORD_ERROR = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_PASSWORD_ERROR; // -5
    public static final int SIM_ANR_TOO_LONG = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_ANR_TOO_LONG; // -6
    public static final int SIM_GENERIC_FAILURE = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_GENERIC_FAILURE; // -10
    public static final int SIM_ADN_LIST_NOT_EXIT = 
        com.android.internal.telephony.IccProvider.ERROR_ICC_PROVIDER_ADN_LIST_NOT_EXIST; // -11

    public static final int ERROR_UNKNOWN = 1;

    public static final int USER_CANCEL = 2;

    public static final int SIM_NOT_READY = 3;

    public static final int USIM_GROUP_NAME_OUT_OF_BOUND = 4;

    public static final int USIM_GROUP_NUMBER_OUT_OF_BOUND = 5;

    public static final int ERROR_USIM_EMAIL_LOST = 6;
}
