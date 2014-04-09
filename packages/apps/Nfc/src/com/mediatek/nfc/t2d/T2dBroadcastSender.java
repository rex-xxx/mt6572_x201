package com.mediatek.nfc.t2d;

import android.content.Context;
import android.content.Intent;

public class  T2dBroadcastSender {
	public static final String NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE =
            "mediatek.nfc.handover.intent.action.WFD_ACTIVE";

    public static final String EXTRA_NFC_WFD_MAC_ADDR =
            "mediatek.nfc.handover.intent.extra.WFD_MAC_ADDR";

    public static final String EXTRA_NFC_WFD_SSID =
            "mediatek.nfc.handover.intent.extra.WFD_SSID";
    
    public static final String EXTRA_NFC_WFD_NETWORK_KEY =
            "mediatek.nfc.handover.intent.extra.WFD_NETWORK_KEY";

    public static final String EXTRA_NFC_WFD_NETWORK_ID =
            "mediatek.nfc.handover.intent.extra.WFD_NETWORK_ID";

    public static final String EXTRA_NFC_WFD_AUTH_TYPE =
            "mediatek.nfc.handover.intent.extra.WFD_AUTH_TYPE";

    public static final String EXTRA_NFC_WFD_ENC_TYPE =
            "mediatek.nfc.handover.intent.extra.WFD_ENC_TYPE";    

    public static final String EXTRA_NFC_WFD_VENDOR_ID =
            "mediatek.nfc.handover.intent.extra.WFD_VENDOR_ID";

    public static final String EXTRA_NFC_WFD_GC_IP =
            "mediatek.nfc.handover.intent.extra.WFD_GC_IP";

    public static final String EXTRA_NFC_WFD_GO_IP =
            "mediatek.nfc.handover.intent.extra.WFD_GO_IP";

    public static final String EXTRA_NFC_WFD_MAX_HEIGHT =
            "mediatek.nfc.handover.intent.extra.WFD_MAX_HEIGHT";	

    public static final String EXTRA_NFC_WFD_MAX_WIDTH =
            "mediatek.nfc.handover.intent.extra.WFD_MAX_WIDTH";
			
	interface T2dConfig {
		byte[] getMacAddr();
		String getNetworkKey();
		String getSSID() ;
		byte getNetworkId();
		short getAuthType();
		short getEncType();
		byte[] getVendorId();
		byte[] getGcIp();
		byte[] getGoIp();
		byte[] getMaxHeight();
		byte[] getMaxWidth();
	}
 
	static class DtvConfig implements T2dConfig {
		byte[] macAddr = {0x02, 0x0c, 0x43, 0x35, (byte) 0xa1, (byte) 0xa2};
		String NetworkKey = "3DrTccTi";
		String SSID = "DIRECT-Z";
		byte NetworkId = 1;
		short AuthType = (short)0x0020;
		short EncType = (short)0x000c;
		byte[] vendorId = {1};
		byte[] gcIp = {(byte)0x64, (byte)0x31, (byte)0xa8, (byte)0xc0};
		byte[] goIp = {(byte)0x01, (byte)0x31, (byte)0xa8, (byte)0xc0};
		byte[] maxHeight = {0x04, 0x38};
		byte[] maxWidth = {0x07, (byte)0x80};
		
		public byte[] getMacAddr() { return macAddr; }
		public String getNetworkKey() { return NetworkKey;}
		public String getSSID() { return SSID; }
		public byte getNetworkId() {return NetworkId; }
		public short getAuthType() { return AuthType; }
		public short getEncType() { return EncType; }
		public byte[] getVendorId() { return vendorId; }
		public byte[] getGcIp() { return gcIp; }
		public byte[] getGoIp() { return goIp; }
		public byte[] getMaxHeight() { return maxHeight; }
		public byte[] getMaxWidth() { return maxWidth; }
	}
	
	 static class NbConfig implements T2dConfig {
		byte[] macAddr = {0x00, 0x0c, 0x43, 0x22, (byte) 0x66, (byte) 0xb2};
		String NetworkKey = "3DrTccTi";
		String SSID = "DIRECT-ZP";
		byte NetworkId = 1;
		short AuthType = (short)0x0020;
		short EncType = (short)0x000c;
		byte[] vendorId = {0x14, (byte)0xc3};
		byte[] gcIp = {0x01, 0x31, (byte)0xa8, (byte)0xc0};
		byte[] goIp = {0x64, 0x31, (byte)0xa8, (byte)0xc0};
		byte[] maxHeight = {0x04, 0x38};
		byte[] maxWidth = {0x07, (byte)0x80};    
		
		public byte[] getMacAddr() { return macAddr; }
		public String getNetworkKey() { return NetworkKey;}
		public String getSSID() { return SSID; }
		public byte getNetworkId() {return NetworkId; }
		public short getAuthType() { return AuthType; }
		public short getEncType() { return EncType; }
		public byte[] getVendorId() { return vendorId; }
		public byte[] getGcIp() { return gcIp; }
		public byte[] getGoIp() { return goIp; }
		public byte[] getMaxHeight() { return maxHeight; }
		public byte[] getMaxWidth() { return maxWidth; }
	}
	
	static private T2dConfig mT2dConfigs[] = { new DtvConfig(), new NbConfig() };
	
	static public void sendT2dBroadcast(int id, Context context){
		if (id < 0) {
			return;
		}
		
        Intent handoverIntent = new Intent(NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE);

		handoverIntent.putExtra(EXTRA_NFC_WFD_MAC_ADDR, mT2dConfigs[id].getMacAddr());
        handoverIntent.putExtra(EXTRA_NFC_WFD_SSID, mT2dConfigs[id].getSSID());
        handoverIntent.putExtra(EXTRA_NFC_WFD_NETWORK_KEY, mT2dConfigs[id].getNetworkKey());
        handoverIntent.putExtra(EXTRA_NFC_WFD_NETWORK_ID, mT2dConfigs[id].getNetworkId());
        handoverIntent.putExtra(EXTRA_NFC_WFD_AUTH_TYPE, mT2dConfigs[id].getAuthType());
        handoverIntent.putExtra(EXTRA_NFC_WFD_ENC_TYPE, mT2dConfigs[id].getEncType());
      
        handoverIntent.putExtra(EXTRA_NFC_WFD_VENDOR_ID, mT2dConfigs[id].getVendorId());

        handoverIntent.putExtra(EXTRA_NFC_WFD_GC_IP, mT2dConfigs[id].getGcIp());
        handoverIntent.putExtra(EXTRA_NFC_WFD_GO_IP, mT2dConfigs[id].getGoIp());
        handoverIntent.putExtra(EXTRA_NFC_WFD_MAX_HEIGHT, mT2dConfigs[id].getMaxHeight());
        handoverIntent.putExtra(EXTRA_NFC_WFD_MAX_WIDTH, mT2dConfigs[id].getMaxWidth());

        context.sendBroadcast(handoverIntent);
    }

}