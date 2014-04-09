package com.mediatek.nfc.wps;

/** @hide */
public interface INfcWpsAppInternal{

    /** intent extra field, NFC Foreground Dispatch command */
    public static final String EXTRA_NFC_WPS_CMD =
            "com.mediatek.nfc.wps.extra.WPS_CMD";

    /** intent extra field, NFC Foreground Dispatch parcel*/
    public static final String EXTRA_NFC_WPS_CONFIGURATION_TOKEN =
            "com.mediatek.nfc.wps.extra.WPS_CONFIGURATION_TOKEN";
    public static final String EXTRA_NFC_WPS_PWD_TOKEN =
            "com.mediatek.nfc.wps.extra.WPS_PWD_TOKEN";


    /** NFC Foreground Dispatch command  */
    public static final int READ_CONFIGURATION_TOKEN_CMD     =0;
    public static final int WRITE_CONFIGURATION_TOKEN_CMD    =1;
    public static final int READ_PASSWORD_TOKEN_CMD          =2;
    public static final int WRITE_PASSWORD_TOKEN_CMD         =3;



    /** intent extra field, NFC Foreground Dispatch command */
    public static final String EXTRA_NFC_WPS_INTERNAL_CMD =
            "com.mediatek.nfc.wps.extra.WPS_INTERNAL_CMD";

    public static final int HANDOVER_REQUEST_CMD    =0x10;
    public static final int HANDOVER_SELECT_CMD     =0x11;    
    public static final int HANDOVER_FINISH_CMD     =0x12;

        
};





