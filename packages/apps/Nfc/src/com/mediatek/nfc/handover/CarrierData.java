package com.mediatek.nfc.handover;

import java.io.UnsupportedEncodingException;

import com.mediatek.nfc.handover.HandoverMessage.HandoverCarrier;

import android.nfc.NdefRecord;

/**
 * Carrier Data (include HC/CCR)
 * 
 */
public abstract class CarrierData {

    /**
     * HC
     */
    public static class HandoverCarrierRecord extends CarrierData {

	// Hc type name
	private static final byte[] NDEF_TYPE_NAME = "Hc".getBytes();
	// TNF(WKT)
	private static final short TNF = 0x1;

	/**
	 * Parse to HC
	 * 
	 * @param carrier
	 * @return
	 */
	public static HandoverCarrierRecord tryParse(HandoverCarrier carrier) {

	    if (carrier == null)
		return null;

	    if (HandoverCarrier.HANDOVER_CARRIER_RECORD != carrier.getFormat()) {
		return null;
	    }

	    byte[] raw = carrier.getData();
	    if (raw == null || raw.length < 3) {
		return null;
	    }

	    if (raw[1] + 2 != raw.length) {
		return null;
	    }

	    short carrierTypeFormat = (short) (raw[0] & 0xFF);
	    byte[] carrierType = new byte[raw[1]];
	    System.arraycopy(raw, 2, carrierType, 0, raw[1]);

	    HandoverCarrierRecord record = new HandoverCarrierRecord(
		    carrierType);
	    record.mCarrierTypeFormat = carrierTypeFormat;

	    return record;
	}

	/**
	 * Factory
	 * 
	 * @param carrierType
	 * @return
	 */
	public static HandoverCarrierRecord newInstance(String carrierType) {

	    try {
		byte[] byteCarrierType = carrierType.getBytes("US-ASCII");
		return new HandoverCarrierRecord(byteCarrierType);

	    } catch (UnsupportedEncodingException e) {
		return null;
	    }
	}

	// Default Carrier type format is MIME-Type
	private static final byte DEFAULT_CARRIER_TYPE_FORMAT = 0x02;

	// Carrier type
	private byte[] mCarrierType;
	// Carrier type format
	private short mCarrierTypeFormat;

	/**
	 * 
	 * @return
	 */
	public short getCarrierTypeFormat() {
	    return mCarrierTypeFormat;
	}

	/**
	 * Constructor
	 * 
	 * @param carrierType
	 */
	private HandoverCarrierRecord(byte[] carrierType) {
	    mCarrierType = carrierType;
	}

	@Override
	public byte[] getPayload() {

	    int size = mCarrierType.length + 2;
	    byte[] payload = new byte[size];

	    payload[0] = DEFAULT_CARRIER_TYPE_FORMAT;
	    payload[1] = (byte) mCarrierType.length;
	    System.arraycopy(mCarrierType, 0, payload, 2, mCarrierType.length);

	    return payload;
	}

	/**
	 * @return the carrierType
	 */
	public String getCarrierType() {
	    return new String(mCarrierType);
	}

	@Override
	public short getTnf() {
	    return TNF;
	}

	@Override
	public byte[] getType() {
	    return NDEF_TYPE_NAME;
	}
    }

    /**
     * Abstract CCR
     */
    public static abstract class CarrierConfigurationRecord extends CarrierData {

	// Carrier type (ex: "application/vnd.wfa.wsc" for WIFI)
	protected String mCarrierType;
    }

    /**
     * Get Ndef payload data
     * 
     * @return
     */
    public abstract byte[] getPayload();

    /**
     * Get TNF
     * 
     * @return
     */
    public short getTnf() {
	return NdefRecord.TNF_MIME_MEDIA;
    }

    /**
     * Get Type
     * 
     * @return
     */
    public abstract byte[] getType();

}
