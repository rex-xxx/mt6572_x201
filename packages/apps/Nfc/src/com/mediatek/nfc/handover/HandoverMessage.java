package com.mediatek.nfc.handover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

public class HandoverMessage {

    // Carrier Power State (Inactive)(00)
    public static final byte CARRIER_POWER_STATE_INACTIVE = 0;
    // Carrier Power State (Active)(01)
    public static final byte CARRIER_POWER_STATE_ACTIVE = 1;
    // Carrier Power State (Activatiing)(10)
    public static final byte CARRIER_POWER_STATE_ACTIVATING = 2;
    // Carrier Power State (Unknow)(11)
    public static final byte CARRIER_POWER_STATE_UNKNOWN = 3;

	// Error record reason record not match
    public static final byte ERROR_RECORD_REASON_NOT_MATCH = 0x20;
	// Error record reason record Invalid
	public static final byte ERROR_RECORD_REASON_INVALID = 0x21;
	// Error record reason connection reject
	public static final byte ERROR_RECORD_REASON_CONNECTION_REJECCT = 0x22;	
	// Error record reason record other
	public static final byte ERROR_RECORD_REASON_OTHER = 0x2F;//47

	// Specific record type Handover Request collision
	public static final byte SPECIFIC_RECORD_TYPE_HANDOVER_REQUEST_COLLISION = 0x0;	    
    
    // Collision resolution record type
    public static final byte[] COLLISION_RESOLUTION_RECORD_TYPE = {
	    (byte) 0x63, (byte) 0x72 };
    // Error record type , err
    public static final byte[] ERROR_RECORD_TYPE = {
	    (byte) 0x65, (byte) 0x72 , (byte) 0x72 };		
    // MTK specific record type ,Specific
    public static final byte[] SPECIFIC_RECORD_TYPE = {
	    (byte) 0x53, (byte) 0x70 , (byte) 0x65, (byte) 0x63 , (byte) 0x69, (byte) 0x66, (byte) 0x69, (byte) 0x63 };		        
    // Octet Stream
    public static final byte[] OCTET_STREAM = "application/octet-stream"
	    .getBytes();
    private static final byte[] EMPTY_BYTES = new byte[0];

    public byte[] mRequesterRandom;

    /**
     * Handover Select
     * 
     */
    public static class HandoverSelect {

	private HandoverSelect(byte version, HandoverCarrier[] carriers,
		byte[][] auxiliaryData) {
	    mVersion = version;
	    mCarriers = carriers;
	    mAuxiliaryData = auxiliaryData;
	}

	// Handover carrier
	private HandoverCarrier[] mCarriers;
	// Handover NDEF Version
	private byte mVersion;
	// Auxiliary Data
	private byte[][] mAuxiliaryData;

	/**
	 * @return the AuxiliaryData
	 */
	public byte[][] getAuxiliaryData() {
	    return mAuxiliaryData;
	}

	public HandoverCarrier[] getCarriers() {
	    return mCarriers;
	}

	public byte getVersion() {
	    return mVersion;
	}
    }

    /**
     * Handover Request
     * 
     */
    public static class HandoverRequest {

	/**
	 * Constructor
	 * 
	 * @param version
	 * @param crn
	 *            Collision Resolution Number
	 * @param carriers
	 * @param auxiliary
	 */
	private HandoverRequest(byte version, byte[] crn,
		HandoverCarrier[] carriers, byte[][] auxiliary) {
	    mVersion = version;
	    mCRN = crn;
	    mCarriers = carriers;
	    mAuxiliaryData = auxiliary;
	}

	// Handover carrier
	private HandoverCarrier[] mCarriers;
	// Handover NDEF Version
	private byte mVersion;
	// Collision Resolution Number
	private byte[] mCRN;
	// Auxiliary Data
	private byte[][] mAuxiliaryData;

	/**
	 * @return the AuxiliaryData
	 */
	public byte[][] getAuxiliaryData() {
	    return mAuxiliaryData;
	}

	public HandoverCarrier[] getCarriers() {
	    return mCarriers;
	}

	public byte getVersion() {
	    return mVersion;
	}

	public byte[] getCollisionResolutionNumber() {
	    return mCRN;
	}
    }

    /**
     * Handover carrier
     */
    public static class HandoverCarrier {

	private HandoverCarrier(short cps, int format, String protocol, byte[] data) {
	    mCPS = cps;
	    mFormatType = format;
	    mProtocolType = protocol;
	    mData = data;
	}

	// record type
	private int mFormatType;
	// protocol type
	private String mProtocolType;
	// carrier raw
	private byte[] mData;

	public static final int HANDOVER_CARRIER_RECORD = 1;
	public static final int HANDOVER_CARRIER_CONFIGURATION_RECORD = 2;

	private short mCPS;

	public short getCPS() {
	    return mCPS;
	}

	public int getFormat() {
	    return mFormatType;
	}

	public String getProtocol() {
	    return mProtocolType;
	}

	byte[] getData() {
	    return mData;
	}
    }

    /**
     * parse ndef data to handover carrier
     * 
     * @param message
     * @return
     * @throws FormatException
     */
    public static HandoverSelect tryParseSelect(NdefMessage message)
	    throws FormatException {

	if (message == null)
	    return null;

	NdefRecord[] records = message.getRecords();
	if (records == null || records.length < 2) {
	    return null;
	}

	if (records[0].getTnf() != NdefRecord.TNF_WELL_KNOWN
		|| !Arrays.equals(NdefRecord.RTD_HANDOVER_SELECT,
			records[0].getType())) {
	    return null;
	}

	byte[] hsPayload = records[0].getPayload();
	byte version = hsPayload[0];
	byte[] acMessageBytes = new byte[hsPayload.length - 1];
	System.arraycopy(hsPayload, 1, acMessageBytes, 0, hsPayload.length - 1);
	hsPayload = null;

	NdefMessage ac = new NdefMessage(acMessageBytes);
	NdefRecord[] acItems = ac.getRecords();

	int i = 0;
	byte[] acPayload = null;
	short cps = 0;
	int idLen = 0;
	byte[] id = null;

	// Auxiliary data reference n
	int auxiliaryDataCount = 0;
	int cursor = 0;
	byte[][] auxiliaryData = null;

	ArrayList<HandoverCarrier> carrierItems = new ArrayList<HandoverMessage.HandoverCarrier>();
	HandoverCarrier item = null;
	for (NdefRecord acRecord : acItems) {

	    if (acRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
		continue;
	    }

	    if (!Arrays.equals(NdefRecord.RTD_ALTERNATIVE_CARRIER,
		    acRecord.getType())) {
		continue;
	    }

	    cursor = 0;
	    acPayload = acRecord.getPayload();
	    cps = (short) (acPayload[cursor++] & 0xFF);
	    idLen = acPayload[cursor++] & 0xFF;
	    id = new byte[idLen];

	    System.arraycopy(acPayload, cursor, id, 0, idLen);
	    cursor += idLen;

	    // Parse Auxiliary Data Reference
	    auxiliaryDataCount = acPayload[cursor++] & 0xFF;
	    if (auxiliaryDataCount > 0) {
		auxiliaryData = new byte[auxiliaryDataCount][];
		int len = 0;
		int j = 0;
		byte[] auxiliaryID = null;
		for (i = 0; i < auxiliaryDataCount; i++) {
		    len = acPayload[cursor++] & 0xFF;
		    auxiliaryID = new byte[len];
		    System.arraycopy(acPayload, cursor, auxiliaryID, 0, len);
		    cursor += len;

		    for (j = 1; i < records.length; j++) {
			if (Arrays.equals(auxiliaryID, records[j].getId())) {
			    auxiliaryData[i] = records[j].getPayload();
			    break;
			}
		    }
		}
	    }

	    for (i = 1; i < records.length; i++) {

		if (Arrays.equals(id, records[i].getId())) {

		    switch (records[i].getTnf()) {
		    case NdefRecord.TNF_WELL_KNOWN: // case HC

			if (!Arrays.equals(NdefRecord.RTD_HANDOVER_CARRIER,
				records[i].getType())) {
			    continue;
			}

			item = new HandoverCarrier(cps,
				HandoverCarrier.HANDOVER_CARRIER_RECORD, "",
				records[i].getPayload());

			carrierItems.add(item);
			item = null;

			break;

		    case NdefRecord.TNF_MIME_MEDIA: // case CCR
		    	Log.d("[HandoverMessage]", "case CCR");
			item = new HandoverCarrier(
				cps,
				HandoverCarrier.HANDOVER_CARRIER_CONFIGURATION_RECORD,
				new String(records[i].getType()), records[i]
					.getPayload());

			carrierItems.add(item);
			item = null;

			break;
		    default:
			break;
		    }
		}
	    }
	}

	if (carrierItems.size() > 0) {
	    return new HandoverSelect(version,
		    carrierItems.toArray(new HandoverCarrier[carrierItems
			    .size()]), auxiliaryData);
	}

	return null;
    }

    /**
     * parse ndef data to handover carrier
     * 
     * @param message
     * @return
     * @throws FormatException
     */
    public static HandoverRequest tryParseRequest(NdefMessage message)
	    throws FormatException {

	if (message == null)
	    return null;

	NdefRecord[] records = message.getRecords();
	if (records == null || records.length < 2) {
	    return null;
	}

	if (records[0].getTnf() != NdefRecord.TNF_WELL_KNOWN
		|| !Arrays.equals(NdefRecord.RTD_HANDOVER_REQUEST,
			records[0].getType())) {
	    return null;
	}

	byte[] hrPayload = records[0].getPayload();
	byte version = hrPayload[0];
	byte[] acMessageBytes = new byte[hrPayload.length - 1];
	System.arraycopy(hrPayload, 1, acMessageBytes, 0, hrPayload.length - 1);

	NdefMessage ac = new NdefMessage(acMessageBytes);
	NdefRecord[] acItems = ac.getRecords();
	int i = 0;

	// Alternative Carrier Record Payload
	byte[] acPayload = null;
	// Carrier Power State
	short cps = 0;

	// Carrier data reference
	int idLen = 0;
	byte[] id = null;

	// Auxiliary data reference n
	int auxiliaryDataCount = 0;
	int cursor = 0;
	byte[][] auxiliaryData = null;

	ArrayList<HandoverCarrier> carrierItems = new ArrayList<HandoverMessage.HandoverCarrier>();
	HandoverCarrier item = null;
	byte[] crn = null;

	for (NdefRecord acRecord : acItems) {

	    if (acRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
		continue;
	    }

	    if (Arrays.equals(COLLISION_RESOLUTION_RECORD_TYPE,
		    acRecord.getType())) {
		crn = acRecord.getPayload();
		continue;
	    }

	    if (!Arrays.equals(NdefRecord.RTD_ALTERNATIVE_CARRIER,
		    acRecord.getType())) {
		continue;
	    }

	    cursor = 0;
	    acPayload = acRecord.getPayload();
	    cps = (short) (acPayload[cursor++] & 0xFF);
	    idLen = acPayload[cursor++] & 0xFF;
	    id = new byte[idLen];

	    System.arraycopy(acPayload, cursor, id, 0, idLen);
	    cursor += idLen;

	    // Parse Auxiliary Data Reference
	    auxiliaryDataCount = acPayload[cursor++] & 0xFF;
	    if (auxiliaryDataCount > 0) {
		auxiliaryData = new byte[auxiliaryDataCount][];
		int len = 0;
		int j = 0;
		byte[] auxiliaryID = null;
		for (i = 0; i < auxiliaryDataCount; i++) {
		    len = acPayload[cursor++] & 0xFF;
		    auxiliaryID = new byte[len];
		    System.arraycopy(acPayload, cursor, auxiliaryID, 0, len);
		    cursor += len;

		    for (j = 1; i < records.length; j++) {
			if (Arrays.equals(auxiliaryID, records[j].getId())) {
			    auxiliaryData[i] = records[j].getPayload();
			    break;
			}
		    }
		}
	    }

	    for (i = 1; i < records.length; i++) {
		if (Arrays.equals(id, records[i].getId())) {
		    switch (records[i].getTnf()) {
		    case NdefRecord.TNF_WELL_KNOWN: // case HC

			if (!Arrays.equals(NdefRecord.RTD_HANDOVER_CARRIER,
				records[i].getType())) {
			    continue;
			}

			item = new HandoverCarrier(cps,
				HandoverCarrier.HANDOVER_CARRIER_RECORD, "",
				records[i].getPayload());

			carrierItems.add(item);
			item = null;

			break;

		    case NdefRecord.TNF_MIME_MEDIA: // case CCR

			item = new HandoverCarrier(
				cps,
				HandoverCarrier.HANDOVER_CARRIER_CONFIGURATION_RECORD,
				new String(records[i].getType()), records[i]
					.getPayload());

			carrierItems.add(item);
			item = null;

			break;
		    default:
			break;
		    }
		}
	    }
	}

	if (carrierItems.size() > 0) {
	    return new HandoverRequest(version, crn,
		    carrierItems.toArray(new HandoverCarrier[carrierItems
			    .size()]), auxiliaryData);
	}

	return null;
    }

    /**
     * AC
     */
    private static class AlternativeCarrier {

	byte CPS;
	CarrierData data;
	byte[] id;
	byte[][] auxiliary;
	byte[][] auxiliaryID;
	int auxiliaryTotalBytes;
    }

    // Alternative Carrier Records
    private ArrayList<AlternativeCarrier> mAcItems = new ArrayList<HandoverMessage.AlternativeCarrier>();
    // Record ID
    private int mCurrentRecordID = 1;

    /**
     * Append Alternative Carrier with auxiliary data into Handover Message
     * 
     * @param cps
     * @param data
     * @param auxiliary
     */
    public void appendAlternativeCarrier(byte cps, CarrierData data,
	    byte[][] auxiliary) {
	AlternativeCarrier ac = new AlternativeCarrier();
	ac.CPS = cps;
	ac.data = data;
	ac.id = Integer.toString(mCurrentRecordID++).getBytes();
	if (auxiliary == null || auxiliary.length == 0) {
	} else {
	    byte[][] auxiliaryIDs = new byte[auxiliary.length][];
	    int totalBytes = 0;
	    for (int i = 0; i < auxiliary.length; i++) {
		auxiliaryIDs[i] = Integer.toString(mCurrentRecordID++)
			.getBytes();
		totalBytes += (1 + auxiliaryIDs[i].length);
	    }

	    ac.auxiliary = auxiliary;
	    ac.auxiliaryID = auxiliaryIDs;
	    ac.auxiliaryTotalBytes = totalBytes;
	}

	mAcItems.add(ac);
    }

    /**
     * Append Alternative Carrier into Handover Message
     * 
     * @param cps
     * @param data
     */
    public void appendAlternativeCarrier(byte cps, CarrierData data) {
	appendAlternativeCarrier(cps, data, null);
    }

    /**
     * Create handover Request Message (Hr)
     * 
     * @return
     */
    public NdefMessage createHandoverRequestMessage() {

	NdefMessage nestedMessage = new NdefMessage(
		createCollisionResolutionRecord(),
		createAlternaiveCarrierRecords());

	byte[] nestedPayload = nestedMessage.toByteArray();
	byte[] hrPayload = new byte[nestedPayload.length + 1];
	hrPayload[0] = 0x12; // connection handover v1.2
	System.arraycopy(nestedPayload, 0, hrPayload, 1, nestedPayload.length);

	NdefRecord hrRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		NdefRecord.RTD_HANDOVER_REQUEST, EMPTY_BYTES, hrPayload);

	return new NdefMessage(hrRecord, createCarrierDataRecords());
    }

    /**
     * create Auxiliary record
     * 
     * @param ac
     * @return
     */
    private NdefRecord createAuxiliaryData(byte[] id, byte[] payload) {
	return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, OCTET_STREAM, id,
		payload);
    }

    /**
     * create handover record
     * 
     * @return
     */
    private NdefRecord createCarrierDataRecord(AlternativeCarrier ac) {

	CarrierData data = ac.data;
	return new NdefRecord(data.getTnf(), data.getType(), ac.id,
		data.getPayload());
    }

    /**
     * create carrier records
     * 
     * @return
     */
    private NdefRecord[] createCarrierDataRecords() {

	ArrayList<NdefRecord> records = new ArrayList<NdefRecord>();
	for (AlternativeCarrier ac : mAcItems) {
	    records.add(createCarrierDataRecord(ac));
	    if (ac.auxiliaryID == null) {

	    } else {
		for (int i = 0; i < ac.auxiliaryID.length; i++) {
		    records.add(createAuxiliaryData(ac.auxiliaryID[i],
			    ac.auxiliary[i]));
		}
	    }
	}

	return records.toArray(new NdefRecord[records.size()]);
    }

    /**
     * create a collision resolution record
     * 
     * @return
     */
    private NdefRecord createCollisionResolutionRecord() {

	byte[] random = new byte[2];
	new Random().nextBytes(random);

    mRequesterRandom = random;
    
	return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		COLLISION_RESOLUTION_RECORD_TYPE, EMPTY_BYTES, random);
    }

    /**
     * create a Error record
     *		3.3.3 Error record
     *		The error Record is used in the Handover Select Record to indicate that the Handover Selector 
     *		failed to successfully process the most recently received handover Request message
     * 
     * @return
     */
    private NdefRecord createErrorRecord(byte reason,byte data) {

	byte[] err = new byte[2];
	//new Random().nextBytes(random);
	err[0] = reason;
	err[1] = data;
	return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		ERROR_RECORD_TYPE, EMPTY_BYTES, err);
    }

    /**
     * create a Specific  record
     *		
     *		The Specific Record is used in the Handover Select Record to indicate specific case happen when
     *       process the most recently received handover Request message
     * 
     * @return
     */
    private NdefRecord createSpecificRecord(byte reason,byte[] arrayData) {

	byte[] specArray = new byte[arrayData.length +1];
	//new Random().nextBytes(random);
	specArray[0] = reason;
    System.arraycopy(arrayData, 0, specArray, 1, arrayData.length );
	return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		SPECIFIC_RECORD_TYPE, EMPTY_BYTES, specArray);
    }


    /**
     * create alternative carriers
     * 
     * @return
     */
    private NdefRecord[] createAlternaiveCarrierRecords() {

	NdefRecord[] records = new NdefRecord[mAcItems.size()];
	int counter = 0;
	for (AlternativeCarrier ac : mAcItems) {
	    records[counter++] = createAlternaiveCarrierRecord(ac);
	}

	return records;
    }

    /**
     * Create Alternative Carrier Record
     * 
     * @param ac
     * @return
     */
    private NdefRecord createAlternaiveCarrierRecord(AlternativeCarrier ac) {

	/****************************************************************************
	 * Payload size = Carrier Data Reference Length + Carrier Data Reference
	 * + Auxiliary Count + (Auxiliary Length + Auxiliary Data) * m
	 ***************************************************************************/
	int size = ac.id.length + 3 + ac.auxiliaryTotalBytes;
	byte[] payload = new byte[size];
	payload[0] = (byte) (ac.CPS & 0x03);
	payload[1] = (byte) ac.id.length;
	System.arraycopy(ac.id, 0, payload, 2, ac.id.length);

	if (ac.auxiliaryID == null) {
	} else {
	    int cursor = 2 + ac.id.length;
	    payload[cursor++] = (byte) ac.auxiliaryID.length;
	    for (byte[] id : ac.auxiliaryID) {
		payload[cursor++] = (byte) id.length;
		System.arraycopy(id, 0, payload, cursor, id.length);
		cursor += id.length;
	    }
	}

	return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		NdefRecord.RTD_ALTERNATIVE_CARRIER, EMPTY_BYTES, payload);

    }

    /**
     * Create handover Request Message (Hs)
     * 
     * @return
     */
    public NdefMessage createHandoverSelectMessage() {
	NdefMessage nestedMessage = new NdefMessage(
		createAlternaiveCarrierRecords());

	byte[] nestedPayload = nestedMessage.toByteArray();
	byte[] hsPayload = new byte[nestedPayload.length + 1];
	hsPayload[0] = 0x12; // connection handover v1.2
	System.arraycopy(nestedPayload, 0, hsPayload, 1, nestedPayload.length);

	NdefRecord hsRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		NdefRecord.RTD_HANDOVER_SELECT, EMPTY_BYTES, hsPayload);

	return new NdefMessage(hsRecord, createCarrierDataRecords());
    }

	/**
	 * Create Error handover Select Message
	 * 
	 * @return
	 */
	public NdefMessage createErrorHandoverSelectMessage(byte reason,byte data) {
	NdefMessage nestedMessage = new NdefMessage(
		createErrorRecord(reason,data));

	byte[] nestedPayload = nestedMessage.toByteArray();
	byte[] hsPayload = new byte[nestedPayload.length + 1];
	hsPayload[0] = 0x12; // connection handover v1.2
	System.arraycopy(nestedPayload, 0, hsPayload, 1, nestedPayload.length);

	NdefRecord hsRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		NdefRecord.RTD_HANDOVER_SELECT, EMPTY_BYTES, hsPayload);

	return new NdefMessage(hsRecord);
	}

    /**
     * Create Error handover Select Message
     * 
     * @return
     */
    public NdefMessage createMtkSpecificHandoverSelectMessage(byte reason,byte[] arrayData) {
    NdefMessage nestedMessage = new NdefMessage(
        createSpecificRecord(reason,arrayData));

    byte[] nestedPayload = nestedMessage.toByteArray();
    byte[] hsPayload = new byte[nestedPayload.length + 1];
    hsPayload[0] = 0x12; // connection handover v1.2
    System.arraycopy(nestedPayload, 0, hsPayload, 1, nestedPayload.length);

    NdefRecord hsRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
        NdefRecord.RTD_HANDOVER_SELECT, EMPTY_BYTES, hsPayload);

    return new NdefMessage(hsRecord);
    }



	
}
