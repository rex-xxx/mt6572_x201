package com.mediatek.nfc.handover;

import com.mediatek.nfc.handover.CarrierData.CarrierConfigurationRecord;
import com.mediatek.nfc.handover.HandoverMessage.HandoverCarrier;

import android.nfc.NdefRecord;

/**
 * BT CCR
 */
public class BTCarrierConfiguration extends CarrierConfigurationRecord {

    // Carrier type name for BT
	public static final String BT_CARRIER_TYPE = "application/vnd.bluetooth.ep.oob";
    // Bluetooth Handover Type
    //public static final byte[] TYPE = BT_CARRIER_TYPE.getBytes();

    // Mac Address
    private byte[] mMacAddress;

    //private static final byte[] ID_BYTE = new byte[]{'b'};


    /**
     * Constructor
     */
    private BTCarrierConfiguration() {
	this.mCarrierType = BT_CARRIER_TYPE;
    }

    public BTCarrierConfiguration(byte[] btMacAddress) {
	this();
	setMacAddress(btMacAddress);
    }

    public BTCarrierConfiguration(String strBtMacAddress) {
	this();
	setMacAddress(strBtMacAddress);
    }

   /**
     * Create NDEF Message (for PasswordToken or Configuration Token)
     * 
     * @return
     */
    /*
    public NdefMessage createMessage() {
	return new NdefMessage(new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
		TYPE.getBytes(),ID_BYTE, getPayload()));
    }
*/
    public void setMacAddress(byte[] macAddress) {
	if (macAddress == null) {

	} else if (macAddress.length != 6) {
	    this.mMacAddress = null;
	}

	// normal byte[] to reverse byte[]
	byte[] revAddrByte = new byte[6];
	for (int i = 0; i < 6; i++) {
	    revAddrByte[i] = macAddress[5 - i];
	}
	this.mMacAddress = revAddrByte;
    }

    public void setMacAddress(String strMacAddress) {
	if (strMacAddress == null) {

	} else if (strMacAddress.length() != 6) {
	    this.mMacAddress = null;
	}

	// normal string to reverse byte[]
	byte[] revAddrByte = addressToReverseBytes(strMacAddress);

	this.mMacAddress = revAddrByte;
    }

    public byte[] getMacAddress() {
	// reverse + reverse = normal
	byte[] revAddrByte = new byte[6];
	for (int i = 0; i < 6; i++) {
	    revAddrByte[i] = mMacAddress[5 - i];
	}
	return revAddrByte;
    }

    @Override
    public byte[] getPayload() {
	byte[] result = new byte[8];
	result[0] = 0;
	result[1] = (byte) result.length;
	System.arraycopy(mMacAddress, 0, result, 2, 6);
	// result is reversed byte[] of BT MAC Address, use to create btCCR in
	// HRM or HSM.
	return result;
    }

    public static BTCarrierConfiguration tryParse(HandoverCarrier carrier) {

	if (carrier == null)
	    return null;

	if (HandoverCarrier.HANDOVER_CARRIER_CONFIGURATION_RECORD != carrier
		.getFormat()) {
	    return null;
	}

	// Only BT can pass
	if (!BT_CARRIER_TYPE.equals(carrier.getProtocol())) {
	    return null;
	}

	byte[] raw = carrier.getData();
	if (raw == null || raw.length < 3) {
	    return null;
	}
	// Log.d("NfcFloat", "[raw] = "+Util.bytesToString(raw));

	BTCarrierConfiguration ccr = new BTCarrierConfiguration();
	byte[] macAddress = new byte[6];
	for (int i = 0; i < 6; i++) {
	    macAddress[i] = raw[7 - i];
	}
	ccr.setMacAddress(macAddress);

	return ccr;
    }

    @Override
    public short getTnf() {
	return NdefRecord.TNF_MIME_MEDIA;
    }

    @Override
    public byte[] getType() {
	return mCarrierType.getBytes();
    }

    /**
     * convert String address to reverse byte address
     * @param address
     * @return
     */
    private static byte[] addressToReverseBytes(String address) {
	String[] split = address.split(":");
	byte[] result = new byte[split.length];

	for (int i = 0; i < split.length; i++) {
	    // need to parse as int because parseByte() expects a signed byte
	    result[split.length - 1 - i] = (byte) Integer
		    .parseInt(split[i], 16);
	}

	return result;
    }// end of addressToReverseBytes

}// end of BTCarrierConfiguration
