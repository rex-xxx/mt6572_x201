package com.mediatek.nfc.handover;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.mediatek.nfc.handover.CarrierData.CarrierConfigurationRecord;
import com.mediatek.nfc.handover.HandoverMessage.HandoverCarrier;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

/**
 * Wifi CCR
 */
public class WifiCarrierConfiguration extends CarrierConfigurationRecord {

    static final String TAG = "WifiCarrierConfiguration";

	/**
	 * TLV
	 */
	public static class TLV {
		
		public short getTag() {
			return tag;
		}

		public byte[] getValue() {

			if (value == null || offset == 0 && size == value.length) {
				return value;
			}

			byte[] result = new byte[size];
			System.arraycopy(value, offset, result, 0, size);
			return result;
		}

		short tag;
		byte[] value;
		int offset;
		int size;
	}

	/**
	 * Password Token
	 */
	public static class PasswordToken {

		// Default Password
		private static final byte[] EMPTY_PASSWORD = { 0x30, 0x30, 0x30, 0x30,
				0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
				0x30, 0x30 };
		// Default Password ID Field
		private static final short DAFAULT_PASSWORD_ID_FIELD = 0x0010;
		// WPS Attribute TLV Header Size
		private static final int HEADER_SIZE = 4;
		// FIX length size
		private static final int FIX_FIELD_LENGTH = 22;
		// Min Password size (defined in WIFI Protected Using NFC)
		private static final int MIN_PASSWORD_LENGTH = 16;
		// Max Password size (defined in WIFI Protected Using NFC)
		private static final int MAX_PASSWORD_LENGTH = 32;
		// Public Key Hash
		private byte[] mPublicKeyHash;
		// Password ID
		private int mPasswordID = DAFAULT_PASSWORD_ID_FIELD;
		// Password
		private byte[] mPassword = EMPTY_PASSWORD;
		// TotalBytes Size
		private int mByteSize = FIX_FIELD_LENGTH;

		/**
		 * @return the PublicKeyHash
		 */
		public byte[] getPublicKeyHash() {
			return mPublicKeyHash;
		}

		/**
		 * @param PublicKeyHash
		 *            the PublicKeyHash to set
		 */
		public void setPublicKeyHash(byte[] publicKeyHash) {
			this.mPublicKeyHash = publicKeyHash;
		}

		/**
		 * @return the PasswordID
		 */
		public short getPasswordID() {
			return (short) mPasswordID;
		}

		/**
		 * @param PasswordID
		 *            the PasswordID to set
		 */
		public void setPasswordID(short passwordID) {

			int intPasswordID = passwordID & 0xFFFF;
			if (intPasswordID < 0x0010) {
				mPasswordID = DAFAULT_PASSWORD_ID_FIELD;
			} else {
				mPasswordID = intPasswordID;
			}
		}

		/**
		 * @return the Password
		 */
		public byte[] getPassword() {
			return mPassword;
		}

		/**
		 * @param mPassword
		 *            the mPassword to set
		 */
		public void setPassword(byte[] password) {
			if (password == null || password.length < MIN_PASSWORD_LENGTH
					|| password.length > MAX_PASSWORD_LENGTH) {
				mPassword = EMPTY_PASSWORD;
				mByteSize = FIX_FIELD_LENGTH + EMPTY_PASSWORD.length;
			} else {
				mPassword = password;
				mByteSize = FIX_FIELD_LENGTH + password.length;
			}
		}

		/**
		 * Measure the byte size
		 * 
		 * @return
		 */
		private int measureSize() {
			return mByteSize + HEADER_SIZE;
		}

		/**
		 * Dump data into buffer
		 * 
		 * @param buffer
		 */
		private void buildToBuffer(ByteBuffer buffer) {

			// Put Header
			buffer.putShort(WPS_ATTRIBUTE_TYPE_OUT_OF_BAND_DEVICE_PASSWORD);
			buffer.putShort((short) mByteSize);

			// Put Public Key Hash
			buffer.put(mPublicKeyHash);
			// Put Password Field ID
			buffer.putShort((short) mPasswordID);
			// Put Password
			buffer.put(mPassword);
		}

		/**
		 * parse credential block
		 * 
		 * @param buffer
		 * @param offset
		 * @param len
		 */
		private void parse(byte[] buffer, int offset, int len) {

			byte[] publicKeyHash = new byte[20];
			int passwordID = 0;
			byte[] password = null;
			int passwordLen = len - 22;

			System.arraycopy(buffer, offset, publicKeyHash, 0, 20);
			passwordID = buffer[20] & 0xFF;
			passwordID = (passwordID << 8) | (buffer[21] & 0xFF);

			password = new byte[passwordLen];
			System.arraycopy(buffer, offset + 22, password, 0, passwordLen);

			mByteSize = len;
			mPassword = password;
			mPasswordID = passwordID;
			mPublicKeyHash = publicKeyHash;
		}
	}

	/**
	 * Credential
	 */
	public static class Credential {

        static final String TAG = "Credential";

		// Default Authentication is 0x0020(WPA2Personal)
		private static final short DEFAULT_AUTHENTICATION_TYPE = 0x0020;
		// Default Encryption type is 0x0008 (AES)
		private static final short DEFAULT_ENCRYPTION_TYPE = 0x0008;
		// Default Network Index is 1 (Deprecated, always set to 1
		private static final byte DEFAULT_NETWORK_INDEX = 0x01;
		// Data fix size (4 * 6 + 2 * 2 + 1 + 4)
		private static final int DATA_FIX_SIZE = 5;

		// Network Index || 4 + 1
		private byte mNetworkIndex = DEFAULT_NETWORK_INDEX;
		// SSID || 4 + n
		private byte[] mSSID;
		// Authentication Type || 4 + 2
		private short mAuthenticationType = DEFAULT_AUTHENTICATION_TYPE;
		// Has Authentication Type
		private boolean mHasAuthenticationType;
		// Encryption Type || 4 + 2
		private short mEncryptionType = DEFAULT_ENCRYPTION_TYPE;
		// Has Encryption Type
		private boolean mHasEncryptionType;
		// Network Key || 4 + n
		private byte[] mNetworkKey;
		// Mac Address || 4 + n
		private byte[] mMacAddress;

		// delete able
		/*
		 * // vendor ID || 4 + 2 private byte[] mvendorID; // Group Owner IP ||
		 * 4 + 4 private byte[] mGOIP; // Group Client IP || 4 + 4 private
		 * byte[] mGCIP; // Max Height || 4 + 2 private byte[] mMaxHeight; //
		 * Max Width || 4 + 2 private byte[] mMaxWidth; // Group Owner Client
		 * table || 4 + 6*n private byte[][] mClienttable;
		 */

		// Crential Data bytes size
		private int mByteSize = DATA_FIX_SIZE;
		// Extension
		private ArrayList<TLV> mExtension;
		// Extension byte size
		private int mExtensionBytesSize;

		/**
		 * parse credential block
		 * 
		 * @param buffer
		 * @param offset
		 * @param len
		 */
		private void parse(byte[] buffer, int offset, int len) {

			int cursor = offset;
			short innerTag = 0;
			int innerLen = 0;
			while (cursor < offset + len) {

				innerTag = (short) (buffer[cursor++] & 0xFF);
				innerTag = (short) ((innerTag << 8) | (buffer[cursor++] & 0xFF));

				innerLen = buffer[cursor++] & 0xFF;
				innerLen = (innerLen << 8) | (buffer[cursor++] & 0xFF);

				byte[] innerTagArray = intToByteCountArray((int)innerTag,(byte)2);
				Log.d(TAG,
						"Into Credential.parse: parseCredentialTLV = \n (Tag:"
								+ Util.bytesToString(innerTagArray) + ", buffer:" + cursor + ",  Length:"
								+ innerLen + ")");
				// 2012/09/21 jacky fix this cursor from 'offset'
				parseCredentialTLV(innerTag, buffer, cursor, innerLen);
				cursor += innerLen;
			}

			mByteSize = len - mExtensionBytesSize;
		}

		/**
		 * Parse WIFI credential TLV
		 * 
		 * @param tag
		 * @param buffer
		 * @param offset
		 * @param len
		 * @param ccr
		 */
		private void parseCredentialTLV(short tag, byte[] buffer, int offset,
				int len) {

			// Log.d("[wifiConfig]", "Into parseCredentialTLV: buffer = " +
			// Util.bytesToString(buffer));

			switch (tag) {
			case WPS_ATTRIBUTE_TYPE_NETWORK_INDEX:
				mNetworkIndex = buffer[offset];
				break;
			case WPS_ATTRIBUTE_TYPE_SSID:
				// Log.d("[WiFiConfig]", "parseCrentialTLV - case SSID");
				mSSID = new byte[len];
				System.arraycopy(buffer, offset, mSSID, 0, len);
				Log.d(TAG, "SSID = " + Util.bytesToString(mSSID));

				break;
			case WPS_ATTRIBUTE_TYPE_AUTHENTICATION_TYPE:
				mAuthenticationType = (short) (buffer[offset] & 0xFF);
				mAuthenticationType = (short) ((mAuthenticationType << 8) | (buffer[offset + 1] & 0xFF));
				mHasAuthenticationType = true;
				break;
			case WPS_ATTRIBUTE_TYPE_ENCRYPTION_TYPE:
				mEncryptionType = (short) (buffer[offset] & 0xFF);
				mEncryptionType = (short) ((mEncryptionType << 8) | (buffer[offset + 1] & 0xFF));
				mHasEncryptionType = true;
				break;
			case WPS_ATTRIBUTE_TYPE_NETWORK_KEY:
				mNetworkKey = new byte[len];
				System.arraycopy(buffer, offset, mNetworkKey, 0, len);
                Log.d(TAG, "NetworkKey = " + Util.bytesToString(mNetworkKey));
				break;
			case WPS_ATTRIBUTE_TYPE_MAC_ADDRESS:
				mMacAddress = new byte[len];
				System.arraycopy(buffer, offset, mMacAddress, 0, len);
				break;
			default:
				// Put extension attributes
				byte[] value = new byte[len];
				System.arraycopy(buffer, offset, value, 0, len);
				addExtensionTLV(tag, value);
			}
		}

		/**
		 * Measure the byte size
		 * 
		 * @return
		 */
		private int measureSize() {
			return mExtensionBytesSize + mByteSize + 4;
		}

		/**
		 * Dump data into buffer
		 * 
		 * @param buffer
		 */
		private void buildToBuffer(ByteBuffer buffer) {

			// Credential TL (not include Value)
			buffer.putShort(WPS_ATTRIBUTE_TYPE_CREDENTIAL); // TAG
			buffer.putShort((short) (mByteSize + mExtensionBytesSize)); // Length

			// Network Index TLV
			buffer.putShort(WPS_ATTRIBUTE_TYPE_NETWORK_INDEX); // TAG
			buffer.putShort((short) 1); // Length
			buffer.put(mNetworkIndex); // 1 byte value

			// SSID TLV
			if (mSSID == null) {
			} else {
				buffer.putShort(WPS_ATTRIBUTE_TYPE_SSID); // Tag
				buffer.putShort((short) mSSID.length); // Length
				buffer.put(mSSID); // Value
			}

			// Authentication Type TLV
			if (mHasAuthenticationType) {
				buffer.putShort(WPS_ATTRIBUTE_TYPE_AUTHENTICATION_TYPE); // TAG
				buffer.putShort((short) 2); // Length
				buffer.putShort(mAuthenticationType); // Value
			}

			// Encryption Type TLV
			if (mHasEncryptionType) {
				buffer.putShort(WPS_ATTRIBUTE_TYPE_ENCRYPTION_TYPE); // TAG
				buffer.putShort((short) 2); // Length
				buffer.putShort(mEncryptionType); // Value
			}

			// Network Key TLV
			if (mNetworkKey == null) {
			} else {
				buffer.putShort(WPS_ATTRIBUTE_TYPE_NETWORK_KEY); // TAG
				buffer.putShort((short) mNetworkKey.length); // Length
				buffer.put(mNetworkKey); // Value
			}

			// MAC Address TLV
			if (mMacAddress == null) {
			} else {
				buffer.putShort(WPS_ATTRIBUTE_TYPE_MAC_ADDRESS); // TAG
				buffer.putShort((short) mMacAddress.length); // Length
				buffer.put(mMacAddress); // Value
			}

			// Extension TLV
			putExtension(buffer);
		}

		/**
		 * Add a extension TLV
		 * 
		 * @param tag
		 * @param value
		 */
		public void addExtensionTLV(short tag, byte[] value) {
			addExtensionTLV(tag, value, 0, value == null ? 0 : value.length);
		}

		/**
		 * Add a extension TLV
		 * 
		 * @param tag
		 * @param value
		 * @param offset
		 * @param len
		 */
		public void addExtensionTLV(short tag, byte[] value, int offset, int len) {

			TLV tlv = new TLV();
			tlv.tag = tag;
			tlv.value = value;
			tlv.offset = offset;
			tlv.size = len;

			if (mExtension == null) {
				mExtension = new ArrayList<WifiCarrierConfiguration.TLV>();
			}

			// Tag(2) + Length(2) + Value(n)
			mExtensionBytesSize += (4 + len);
			mExtension.add(tlv);
            Log.d(TAG, "WifiCCR [Credential] add TLV Tag:"+Integer.toHexString(tag)+"  len:"+len);
            Log.d(TAG, "                           value:"+ Util.bytesToString(value));
		}

		/**
		 * Get extensions fields
		 * 
		 * @return
		 */
		public TLV[] getExtensions() {
			if (mExtension.size() > 0) {
				return mExtension.toArray(new TLV[mExtension.size()]);
			}

			return null;
		}

		/**
		 * @return the mNetworkIndex
		 */
		public byte getNetworkIndex() {
			return mNetworkIndex;
		}

		/**
		 * @return the mAuthenticationType
		 */
		public short getAuthenticationType() {
			return mAuthenticationType;
		}

		/**
		 * @param AuthenticationType
		 *            the AuthenticationType to set
		 */
		public void setAuthenticationType(short authenticationType) {

			if (!mHasAuthenticationType) {
				mHasAuthenticationType = true;
				mByteSize += 6;
			}

			this.mAuthenticationType = authenticationType;
		}

		/**
		 * @return the EncryptionType
		 */
		public short getEncryptionType() {
			return mEncryptionType;
		}

		/**
		 * @param EncryptionType
		 *            the EncryptionType to set
		 */
		public void setEncryptionType(short encryptionType) {

			if (!mHasEncryptionType) {
				mHasEncryptionType = true;
				mByteSize += 6;
			}

			this.mEncryptionType = encryptionType;
		}

		/**
		 * @return the NetworkKey
		 */
		public String getNetworkKey() {
			return new String(mNetworkKey);
		}

		/**
		 * @param NetworkKey
		 *            the NetworkKey to set
		 */
		public void setNetworkKey(String networkKey) {

			byte[] networkKeyBytes = null;
			try {
				networkKeyBytes = networkKey.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				networkKeyBytes = null;
			}

			int orginalSize = ((mNetworkKey == null) ? 0
					: mNetworkKey.length + 4);
			int newSize = ((networkKeyBytes == null) ? 0
					: networkKeyBytes.length + 4);

			mByteSize -= orginalSize;
			if (newSize == 0) {
				mNetworkKey = null;
			} else {
				mNetworkKey = networkKeyBytes;
				mByteSize += newSize;
			}
		}

		/**
		 * @return the MacAddress
		 */
		public byte[] getMacAddress() {
			return mMacAddress;
		}

		/**
		 * @param mMacAddress
		 *            the MacAddress to set
		 */
		public void setMacAddress(byte[] macAddress) {

			int orginalSize = ((mMacAddress == null) ? 0
					: mMacAddress.length + 4);
			int newSize = ((macAddress == null) ? 0 : macAddress.length + 4);

			mByteSize -= orginalSize;
			if (newSize == 0) {
				mMacAddress = null;
			} else {
				mMacAddress = macAddress;
				mByteSize += newSize;
			}
		}

		/**
		 * @return the mSSID
		 */
		public String getSSID() {
			return new String(mSSID);
		}

		/**
		 * @param mSSID
		 *            the mSSID to set
		 * @throws UnsupportedEncodingException
		 */
		public void setSSID(String ssid) {

			byte[] newSSIDBytes = null;
			try {
				newSSIDBytes = ssid.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				newSSIDBytes = null;
			}

			int orginalSize = ((mSSID == null) ? 0 : mSSID.length + 4);
			int newSize = ((newSSIDBytes == null) ? 0 : newSSIDBytes.length + 4);

			mByteSize -= orginalSize;
			if (newSize == 0) {
				mSSID = null;
			} else {
				mSSID = newSSIDBytes;
				mByteSize += newSize;
			}
		}

		/**
		 * Put extension filed to payload
		 * 
		 * @param buffer
		 */
		private void putExtension(ByteBuffer buffer) {

			if (mExtension == null) {
				return;
			}

			for (TLV tlv : mExtension) {
				buffer.putShort(tlv.tag);
				buffer.putShort((short) tlv.value.length);
				buffer.put(tlv.value, tlv.offset, tlv.size);
			}
		}// end of putExtension

	}// end of Credential

	// Carrier type name for WI-FI
	public static final String TYPE = "application/vnd.wfa.wsc";
	// TAG VERSION
	public static final short WPS_ATTRIBUTE_TYPE_VERSION = 0x104A;
	// TAG CREDENTIAL
	public static final short WPS_ATTRIBUTE_TYPE_CREDENTIAL = 0x100E;
	// TAG NETWORK INDEX
	public static final short WPS_ATTRIBUTE_TYPE_NETWORK_INDEX = 0x1026;
	// TAG SSID
	public static final short WPS_ATTRIBUTE_TYPE_SSID = 0x1045;
	// TAG AUTHENTICATION TYPE
	public static final short WPS_ATTRIBUTE_TYPE_AUTHENTICATION_TYPE = 0x1003;
	// TAG ENCRYPTION TYPE
	public static final short WPS_ATTRIBUTE_TYPE_ENCRYPTION_TYPE = 0x100F;
	// TAG NETWORK KEY
	public static final short WPS_ATTRIBUTE_TYPE_NETWORK_KEY = 0x1027;
	// TAG MAC ADDRESS
	public static final short WPS_ATTRIBUTE_TYPE_MAC_ADDRESS = 0x1020;
	// TAG MAC ADDRESS
	public static final short WPS_ATTRIBUTE_TYPE_AP_CHANNEL = 0x1001;
	// Vendor Extension
	public static final short WPS_ATTRIBUTE_TYPE_VENDER_EXTENSION = 0x1049;
	// Out-of-Band Device Password
	public static final short WPS_ATTRIBUTE_TYPE_OUT_OF_BAND_DEVICE_PASSWORD = 0x102C;
	// Default Version
	private static final byte DEFAULT_WIFI_VERISON = 0x10;
	// Fix Fields length
	private static final int FIX_FIELDS_LENGTH = 5;
	// EMPTY Bytes
	private static final byte[] EMPTY_BYTES = new byte[0];
	// Version
	private byte mVersion = DEFAULT_WIFI_VERISON;
	// Crendential TLV
	private Credential mCredential;
	// Password Token TLV
	private PasswordToken mPasswordToken;
	// Extension
	private ArrayList<TLV> mExtension;
	// Extension byte size
	private int mExtensionBytesSize;

	/**
	 * Constructor
	 */
	private WifiCarrierConfiguration() {
		this.mCarrierType = TYPE;
	}

	/**
	 * Constructor with credential
	 * 
	 * @param credential
	 */
	public WifiCarrierConfiguration(Credential credential) {
		this();
		mCredential = credential;
	}

	public WifiCarrierConfiguration(PasswordToken passwordToken) {
		this();
		mPasswordToken = passwordToken;
	}

	/**
	 * Create NDEF Message (for PasswordToken or Configuration Token)
	 * 
	 * @return
	 */
	public NdefMessage createMessage() {
		return new NdefMessage(new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				TYPE.getBytes(), EMPTY_BYTES, getPayload()));
	}

	/**
	 * NDEF Message
	 * 
	 * @param message
	 * @return
	 */
	public static WifiCarrierConfiguration tryParse(NdefMessage message) {

		if (message == null) {
			return null;
		}

		NdefRecord[] records = message.getRecords();
		if (records == null || records.length != 1) {
			return null;
		}

		if (NdefRecord.TNF_MIME_MEDIA != records[0].getTnf()) {
			return null;
		}

		if (!Arrays.equals(TYPE.getBytes(), records[0].getType())) {
			return null;
		}

		byte[] raw = records[0].getPayload();
		if (raw == null || raw.length < 3) {
			return null;
		}

		int cursor = 0;
		short tag = 0;
		int len = 0;
		WifiCarrierConfiguration ccr = new WifiCarrierConfiguration();
		while (cursor < raw.length) {

			tag = (short) (raw[cursor++] & 0xFF);
			tag = (short) ((tag << 8) | (raw[cursor++] & 0xFF));

			len = raw[cursor++] & 0xFF;
			len = (len << 8) | (raw[cursor++] & 0xFF);

			parseTLV(tag, raw, cursor, len, ccr);
			cursor += len;
		}

		return ccr;
	}

	/**
	 * Parse handover carrier data into specific format
	 * 
	 * @param carrier
	 * @return
	 */
	public static WifiCarrierConfiguration tryParse(HandoverCarrier carrier) {

		if (carrier == null)
			return null;

		if (HandoverCarrier.HANDOVER_CARRIER_CONFIGURATION_RECORD != carrier
				.getFormat()) {
			return null;
		}

		// Only WIFI can pass
		if (!TYPE.equals(carrier.getProtocol())) {
			return null;
		}

		byte[] raw = carrier.getData();
		if (raw == null || raw.length < 3) {
			return null;
		}

		int cursor = 0;
		short tag = 0;
		int len = 0;
        Log.d(TAG,"tryParse parseTLV  carrier.getData()    raw= " + Util.bytesToString(raw) );
		WifiCarrierConfiguration ccr = new WifiCarrierConfiguration();
		while (cursor < raw.length) {

			tag = (short) (raw[cursor++] & 0xFF);
			tag = (short) ((tag << 8) | (raw[cursor++] & 0xFF));

			len = raw[cursor++] & 0xFF;
			len = (len << 8) | (raw[cursor++] & 0xFF);

			byte[] tagArray = intToByteCountArray((int)tag,(byte)2);
			
			Log.d(TAG,"tryParse parseTLV tag= " + Util.bytesToString(tagArray));
			Log.d(TAG,"                  cursor= " +cursor );
			Log.d(TAG,"                  len= "  + len );
			parseTLV(tag, raw, cursor, len, ccr);
			cursor += len;
		}

		return ccr;
	}

	/**
	 * Parse Wifi TLV
	 * 
	 * @param raw
	 */
	private static void parseTLV(short tag, byte[] buffer, int offset, int len,
			WifiCarrierConfiguration ccr) {

		switch (tag) {
		case WPS_ATTRIBUTE_TYPE_VERSION:

			ccr.mVersion = buffer[offset];
			break;

		case WPS_ATTRIBUTE_TYPE_CREDENTIAL:

			Log.d(TAG, "Into parTLV: case tag Credential");
			ccr.mCredential = new Credential();
			ccr.mCredential.parse(buffer, offset, len);
			break;

		case WPS_ATTRIBUTE_TYPE_OUT_OF_BAND_DEVICE_PASSWORD:

			ccr.mPasswordToken = new PasswordToken();
			ccr.mPasswordToken.parse(buffer, offset, len);
			break;

		default:

			// Put extension attributes
			byte[] value = new byte[len];
			System.arraycopy(buffer, offset, value, 0, len);
			ccr.addExtensionTLV(tag, value);
		}
	}

	/**
	 * Add a extension TLV
	 * 
	 * @param tag
	 * @param value
	 */
	public void addExtensionTLV(short tag, byte[] value) {
		addExtensionTLV(tag, value, 0, value == null ? 0 : value.length);
	}

	/**
	 * Add a extension TLV
	 * 
	 * @param tag
	 * @param value
	 * @param offset
	 * @param len
	 */
	public void addExtensionTLV(short tag, byte[] value, int offset, int len) {

		TLV tlv = new TLV();
		tlv.tag = tag;
		tlv.value = value;
		tlv.offset = offset;
		tlv.size = len;

		if (mExtension == null) {
			mExtension = new ArrayList<WifiCarrierConfiguration.TLV>();
		}

		// Tag(2) + Length(2) + Value(n)
		mExtensionBytesSize += (4 + len);
		mExtension.add(tlv);
        Log.d(TAG, "WifiCCR [Not in Credential]add TLV Tag:"+Integer.toHexString(tag)+"  len:"+len);
        Log.d(TAG, "                value:"+ Util.bytesToString(value));
	}

	/**
	 * @return the mVersion
	 */
	public byte getVersion() {
		return mVersion;
	}

	/**
	 * @param mVersion
	 *            the mVersion to set
	 */
	public void setVersion(byte mVersion) {
		this.mVersion = mVersion;
	}

	/**
	 * Put extension filed to payload
	 * 
	 * @param buffer
	 */
	private void putExtension(ByteBuffer buffer) {

		if (mExtension == null) {
			return;
		}

		for (TLV tlv : mExtension) {
			buffer.putShort(tlv.tag);
			buffer.putShort((short) tlv.value.length);
			buffer.put(tlv.value, tlv.offset, tlv.size);
		}
	}

	/**
	 * Create a payload
	 */
	@Override
	public byte[] getPayload() {

		int dataSize = mExtensionBytesSize + FIX_FIELDS_LENGTH;
		if (mCredential != null) {
			dataSize += mCredential.measureSize();
		} else if (mPasswordToken != null) {
			dataSize += mPasswordToken.measureSize();
		}

		// create a byte buffer
		ByteBuffer ccrPayload = ByteBuffer.allocate(dataSize);

		// Version TLV
		ccrPayload.putShort(WPS_ATTRIBUTE_TYPE_VERSION); // TAG
		ccrPayload.putShort((short) 1); // Length
		ccrPayload.put(mVersion); // 1 byte value only

		if (mCredential != null) {
			mCredential.buildToBuffer(ccrPayload);
		} else if (mPasswordToken != null) {
			mPasswordToken.buildToBuffer(ccrPayload);
		}

		// Extension TLV
		putExtension(ccrPayload);

		byte[] result = ccrPayload.array();
		return result;
	}

	@Override
	public byte[] getType() {
		return mCarrierType.getBytes();
	}

	/**
	 * Get extensions fields
	 * 
	 * @return
	 */
	public TLV[] getExtensions() {
		if (mExtension.size() > 0) {
			return mExtension.toArray(new TLV[mExtension.size()]);
		}

		return null;
	}

	/**
	 * @return the Credential
	 */
	public Credential getCredential() {
		return mCredential;
	}

	/**
	 * @return the PasswordToken
	 */
	public PasswordToken getPasswordToken() {
		return mPasswordToken;
	}
	
    static byte[] intToByteCountArray(int i,byte btyeCount)
    {
    	byte j;
    	if(btyeCount > 4)
    		return null;
    	
      byte[] result = new byte[btyeCount];

      for(j=0 ; j < btyeCount ; j++)
      {
    	  byte k = (byte) ((byte) j*8);
    	  result[j]= (byte)(i >> k);
      }
      //result[0] = (byte) (i >> 24);
      //result[1] = (byte) (i >> 16);
      //result[2] = (byte) (i >> 8);
      //result[3] = (byte) (i /*>> 0*/);

      return result;
    }
	
}
