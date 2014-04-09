package com.mediatek.apn;

import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Telephony;

import com.android.internal.telephony.TelephonyProperties;

public class ApnUtils {
    public static final int SIM_CARD_SINGLE = -1;
    // M: code review for value to be -2
    public static final int SIM_CARD_UNDEFINED = -2;
    // preferred APN URI
    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    public static final String PREFERRED_APN_URI_GEMINI_SIM1 = "content://telephony/carriers_sim1/preferapn";
    public static final String PREFERRED_APN_URI_GEMINI_SIM2 = "content://telephony/carriers_sim2/preferapn";
     //M: add for GEMINI+
    public static final String PREFERRED_APN_URI_GEMINI_SIM3 = "content://telephony/carriers_sim3/preferapn";
    public static final String PREFERRED_APN_URI_GEMINI_SIM4 = "content://telephony/carriers_sim4/preferapn";
    
    
    /**
     * Standard projection for the interesting columns of a normal note.
     */
    public static final String[] PROJECTION = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.SOURCE_TYPE, // 16
            Telephony.Carriers.OMACPID,//17
            Telephony.Carriers.NAPID,//18
            Telephony.Carriers.PROXYID,//19
    };

    public static final int ID_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int APN_INDEX = 2;
    public static final int PROXY_INDEX = 3;
    public static final int PORT_INDEX = 4;
    public static final int USER_INDEX = 5;
    public static final int SERVER_INDEX = 6;
    public static final int PASSWORD_INDEX = 7;
    public static final int MMSC_INDEX = 8;
    public static final int MCC_INDEX = 9;
    public static final int MNC_INDEX = 10;
    public static final int NUMERIC_INDEX = 11;
    public static final int MMSPROXY_INDEX = 12;
    public static final int MMSPORT_INDEX = 13;
    public static final int AUTH_TYPE_INDEX = 14;
    public static final int TYPE_INDEX = 15;
    public static final int SOURCE_TYPE_INDEX = 16;
    public static final int APN_ID_INDEX = 17;
    public static final int NAP_ID_INDEX = 18;
    public static final int PROXY_ID_INDEX = 19;   
    
   
    // uri list for gemini
    public static final Uri URI_LIST[] = {
            Telephony.Carriers.SIM1Carriers.CONTENT_URI,
            Telephony.Carriers.SIM2Carriers.CONTENT_URI,
            Telephony.Carriers.SIM3Carriers.CONTENT_URI,
            Telephony.Carriers.SIM4Carriers.CONTENT_URI };
    
    // numeric list for gemini
    public static final String NUMERIC_LIST[] = {
            SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1"),
            SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1"),
            SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_3, "-1"),
            SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_4, "-1"), };
    
    // preferred list for gemini
    public static final Uri PREFERRED_URI_LIST[] = {
            Uri.parse(PREFERRED_APN_URI_GEMINI_SIM1),
            Uri.parse(PREFERRED_APN_URI_GEMINI_SIM2),
            Uri.parse(PREFERRED_APN_URI_GEMINI_SIM3),
            Uri.parse(PREFERRED_APN_URI_GEMINI_SIM4)

    };
    
    public static String checkNotSet(String value) {
        if (value == null || value.length() == 0) {
            return "";
        } else {
            return value;
        }
    }
}
