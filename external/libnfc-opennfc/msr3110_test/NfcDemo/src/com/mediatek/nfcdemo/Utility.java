package com.mediatek.nfcdemo;

import android.util.Log;

public class Utility {
    
    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;

    //reader type
    public static final int TYPE_ID_FELICA = 1001;
    public static final int TYPE_ID_14443A = 1002;
    public static final int TYPE_ID_TYPE1 = 1003;
    public static final int TYPE_ID_15693 = 1004;
    public static final int TYPE_ID_P2P_I = 1005;
    public static final int TYPE_ID_P2P_T = 1006;

    //card mode
    public static final int CARD_TYPE_A = 3001;
    public static final int CARD_TYPE_B = 3002;
    public static final int CARD_TYPE_AB = 3003;

    //data length
    public static final int DATA_LENGTH_1 = 4000;
    public static final int DATA_LENGTH_2 = 4001;
    public static final int DATA_LENGTH_3 = 4002;
    public static final int DATA_LENGTH_4 = 4003;
    public static final int DATA_LENGTH_5 = 4004;

    //reset MSR3110
    public static final int INIT_NONE = 0;
    public static final int INIT_RESET = 1;

    //polling info
    public static final int POLLING_INFO_LENGTH_MAX = 0x19;
    public static final int POLLING_CATEGORY_READER = 0x00;
    public static final int POLLING_CATEGORY_CARD = 0x40;
    public static final int POLLING_CATEGORY_TARGET = 0x80;
    public static final int POLLING_LOOP_STATUS_TYPE_POSITION = 13;
    public static final int POLLING_LOOP_STATUS_FIELD_POSITION = 14;

    //error code
    public static final int I2C_SEND_ERROR = 1;
    public static final int I2C_RECEIVE_ERROR = 2;
    public static final int DEP_FAIL = 3;
    public static final int FIELD_OFF = 4;
    public static final int CHECKSUM_ERROR = 5;
    public static final int CRC_ERROR = 6;
    public static final int JNI_ERROR = 7;

    public static String getErrorMessage(int errorCode) {
        String message = "";
        switch( errorCode) {
            case I2C_SEND_ERROR:
                message = "I2C send error";
                break;
                
            case I2C_RECEIVE_ERROR:
                message = "I2C receive error";
                break;
                
            case DEP_FAIL:
                message = "DEP Fail";
                break;

            case FIELD_OFF:
                message = "FIELD OFF";
                break;
                
            case CHECKSUM_ERROR:
                message = "Checksum Error";
                break;
                
            case CRC_ERROR:
                message = "CRC Error";
                break;
                
            case JNI_ERROR:
                message = "JNI Error";
                break;
                
            default:
                message = "Others";
                break;
        }
        return message;
    }
    
    public static String parsePollingInfo(byte[] byteArray, int length) {
            
        byte [] A3Rsp = new byte[1];
        byte [] Len = new byte[1];
        byte [] Enable = new byte[1];
        byte [] RunMode = new byte[1];
        byte [] CurrentStatus = new byte[1];
        byte [] PollingMask = new byte[5];
        byte [] PollingLoopStatus = new byte[7];
        byte [] SWPInfo = new byte[5];
        byte [] CheckSum = new byte[2];
        
        System.arraycopy(byteArray, 0, A3Rsp, 0, 1);
        System.arraycopy(byteArray, 1, Len, 0, 1);
        System.arraycopy(byteArray, 3, Enable, 0, 1);
        System.arraycopy(byteArray, 4, RunMode, 0, 1);
        System.arraycopy(byteArray, 5, CurrentStatus, 0, 1);
        System.arraycopy(byteArray, 6, PollingMask, 0, 5);
        System.arraycopy(byteArray, 11, PollingLoopStatus, 0, 7);
        System.arraycopy(byteArray, 18, SWPInfo, 0, 5);
        System.arraycopy(byteArray, 23, CheckSum, 0, 2);
        
        String responseString = "-----Raw data ------\n" + 
                                Utility.binToHex(byteArray, length) + "\n" +
                                "--------------------\n" +
                                "A3Rsp: " + Utility.binToHex(A3Rsp, 1) + "\n" +
                                "Len: " + Utility.binToHex(Len, 1) + "\n" +
                                "Enable: " + Utility.binToHex(Enable, 1) + "\n" +
                                "RunMode: " + Utility.binToHex(RunMode, 1) + "\n" +
                                "Current Status: " + Utility.binToHex(CurrentStatus, 1) + "\n" +
                                "Polling Mask: " + Utility.binToHex(PollingMask, 5) + "\n" +
                                "PollingLoop Status: " + Utility.binToHex(PollingLoopStatus, 7) + "\n" +
                                "SWP Info: " + Utility.binToHex(SWPInfo, 5) + "\n" +
                                "Check Sum: " + Utility.binToHex(CheckSum, 2) + "\n" ;
        
        A3Rsp = null;
        Len = null;
        Enable = null;
        CurrentStatus = null;
        PollingLoopStatus = null;
        PollingMask = null;
        SWPInfo = null;
        CheckSum = null;
        return responseString;
    }

    public static String rawDataPollingInfo(byte[] byteArray, int length) {
         
        String responseString = "-----Raw data ------\n" + 
                                Utility.binToHex(byteArray, length) + "\n" +
                                "--------------------\n" ;
        return responseString;
    }

    public static int parsePollingLoopStatusType(byte[] byteArray) {
        //return Integer.toHexString(byteArray[ POLLING_LOOP_STATUS_TYPE_POSITION ] & 0xFF);
        return new Integer(byteArray[ POLLING_LOOP_STATUS_TYPE_POSITION ] & 0xFF);
    }

    public static int parsePollingLoopStatusField(byte[] byteArray) {
         return new Integer(byteArray[ POLLING_LOOP_STATUS_FIELD_POSITION ] & 0xFF);
         //return Integer.toHexString(byteArray[ POLLING_LOOP_STATUS_FIELD_POSITION ] & 0xFF);
    }

    public static byte[] hexToBin(String s){
        int len = s.length();
        byte[] data = new byte[len/2];
        if( len%2 != 0)
            return null;    
        for(int i = 0; i < len; i += 2 ) {
            data[i/2] = (byte)((Character.digit(s.charAt(i), 16) << 4)+ Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String binToHex(byte[] buf, int len){
        String ret = "";
        for(int i = 0; i < len; i++){
            ret = ret + "0123456789ABCDEF".charAt(0xf&buf[i]>>4) + "0123456789ABCDEF".charAt(buf[i]&0xf) + " ";
        }
        return ret;
    }     

    public static String formatTime(long ms){
        String timeString = "";
        Long minus = (ms/1000)/60;
        Long seconds = (ms/1000)%60;
        timeString = minus + " min " + seconds + " sec" + "\n (" +  ms + " milliseconds)";
        return timeString;
    }    

}


