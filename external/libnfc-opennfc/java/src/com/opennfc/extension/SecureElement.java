/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opennfc.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.opennfc.extension.SecureElementManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * The class to provide access to Secure Element functionality for a user
 * application
 * 
 */
public final class SecureElement extends RoutingTableEntryTarget implements
		Parcelable {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/** Tag use in debug */
	private static final String TAG = SecureElement.class.getSimpleName();

	private int mSeConnectionHandle = -1;

	private int mSeChannelHandle = -1;

	private Map<Integer, Integer> channelNumer_handle = new HashMap<Integer, Integer>();

	/**
	 * The Secure Element policy protocols: the card emulation protocols and the
	 * reader protocols allowed for the Secure Element
	 */
	public enum PolicyProtocol {
		/** Policy for IClass */
		ICLASS(ConstantAutogen.W_NFCC_PROTOCOL_CARD_ISO_15693_2, "ISO_15693_2"),
		/** Policy for ISO 14443 A */
		ISO_14443_A(ConstantAutogen.W_NFCC_PROTOCOL_CARD_ISO_14443_4_A,
				"ISO_14443_4_A"),
		/** Policy for ISO 14443 B */
		ISO_14443_B(ConstantAutogen.W_NFCC_PROTOCOL_CARD_ISO_14443_4_B,
				"ISO_14443_4_B"),
		/** Policy for Mifare */
		MIFARE(ConstantAutogen.W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC,
				"MIFARE_CLASSIC");

		private final int intValue;
		private final String description;

		private PolicyProtocol(int intValue, String description) {
			this.intValue = intValue;
			this.description = description;
		}

		/**
		 * Create an array with PolicyProtocol enumerations based on the integer
		 * policy value in the OpenNFC format
		 * 
		 * @param value
		 *            integer policy value
		 * @return an array with PolicyProtocol enumerations
		 */
		static PolicyProtocol[] getPolicyProtocols(int value) {

			PolicyProtocol[] protocols = new PolicyProtocol[0];
			if (value != 0) {
				ArrayList<PolicyProtocol> list = new ArrayList<PolicyProtocol>();
				for (PolicyProtocol policy : PolicyProtocol.values()) {
					if ((policy.intValue & value) != 0) {
						list.add(policy);
					}
				}
				protocols = (PolicyProtocol[]) list.toArray(protocols);
			}
			return protocols;
		}

		int getIntValue() {
			return intValue;
		}

		String getDescription() {
			return description;
		}

		/**
		 * Get the string description of provided policy protocols
		 * 
		 * @param policy
		 *            the array of policy protocols
		 * @return the string description of policy protocols
		 */
		static public String getDescription(PolicyProtocol[] policy) {
			String outString = null;
			if ((policy == null) || (policy.length == 0)) {
				outString = "NONE";
			} else {
				StringBuilder strBuilder = new StringBuilder("[ ");
				for (int i = 0; i < policy.length; i++) {
					strBuilder.append(policy[i].getDescription() + " ");
				}
				strBuilder.append("]");
				outString = strBuilder.toString();
			}
			return outString;
		}

		/**
		 * Get an integer value used by Open NFC for provided set of policy
		 * protocols
		 * 
		 * @param policy
		 *            an array of policy protocols
		 * @return the integer value that can be used for Open NFC
		 */
		static int getIntValue(PolicyProtocol[] policy) {
			int value = 0;
			if (policy != null) {
				for (int i = 0; i < policy.length; i++) {
					value |= policy[i].getIntValue();
				}
			}
			return value;
		}
	}

	/**
	 * The Secure Element policy storage types: identifies the Secure Element
	 * policy value(s) to modify
	 */
	public enum PolicyStorageType {
		/** persistent value storage */
		STORAGE_PERSISTENT(ConstantAutogen.W_NFCC_STORAGE_PERSISTENT),
		/** volatile value storage */
		STORAGE_VOLATILE(ConstantAutogen.W_NFCC_STORAGE_VOLATILE),
		/** persistent and volatile value storage */
		STORAGE_BOTH(ConstantAutogen.W_NFCC_STORAGE_PERSISTENT
				| ConstantAutogen.W_NFCC_STORAGE_VOLATILE);

		private final int intValue;

		int getIntValue() {
			return intValue;
		}

		private PolicyStorageType(int intValue) {
			this.intValue = intValue;
		}
	}

	/**
	 * Construct SecureElement
	 * 
	 * @param slotIdentifier
	 *            the slot identifier
	 * @param description
	 *            the Secure Element description
	 * @param supportedProtocols
	 *            the card emulation protocols and the reader protocols
	 *            supported by Secure Element for the RF interface
	 * @param capabilities
	 *            the Secure Element Capabilities
	 * @param volatilePolicy
	 *            the current volatile value of the card emulation protocols and
	 *            of the reader protocols allowed for the RF Interface of the
	 *            Secure Element
	 * @param persistentPolicy
	 *            the persistent value of the card emulation protocols and of
	 *            the reader protocols allowed for the RF Interface of the
	 *            Secure Element
	 */
	private SecureElement(int slotIdentifier, String description,
			int supportedProtocols, int capabilities, int volatilePolicy,
			int persistentPolicy) {
		super(slotIdentifier, description);

		this.slotIdentifier = slotIdentifier;
		this.description = description;
		this.supportedProtocols = supportedProtocols;
		this.capabilities = capabilities;
		this.volatilePolicy = volatilePolicy;
		this.persistentPolicy = persistentPolicy;
	}

	private int slotIdentifier;
	private String description;
	private int supportedProtocols;
	private int capabilities;
	private int volatilePolicy;
	private int persistentPolicy;
	private SecureElementManager manager;

	/**
	 * Get the slot identifier used by the Secure Element
	 * 
	 * @return a slot identifier
	 */
	int getSlotIdentifier() {
		return slotIdentifier;
	}

	/**
	 * Get the name of the Secure Element
	 * 
	 * @return the name of the Secure Element. See the specification of the NFC
	 *         Controller for the meaning of this value
	 */
	public String getName() {
		return description;
	}

	/*
	 * public int getSupportedProtocols() { return supportedProtocols; }
	 */
	/*
	 * public int getCapabilities() { return capabilities; }
	 */
	/**
	 * Get the current volatile value of the card emulation protocols and of the
	 * reader protocols allowed for the RF Interface of the Secure Element
	 * 
	 * @return the array of the protocols (zero-length array if no protocols are
	 *         allowed)
	 */
	public PolicyProtocol[] getVolatilePolicy() {
		return PolicyProtocol.getPolicyProtocols(volatilePolicy);
	}

	/**
	 * Get the persistent value of the card emulation protocols and of the
	 * reader protocols allowed for the RF Interface of the Secure Element
	 * 
	 * @return the array of the protocols (zero-length array if no protocols are
	 *         allowed)
	 */
	public PolicyProtocol[] getPersistentPolicy() {
		return PolicyProtocol.getPolicyProtocols(persistentPolicy);
	}

	private void storePolicy(PolicyStorageType storageType,
			PolicyProtocol[] policy) {
		int policyValue = PolicyProtocol.getIntValue(policy);
		switch (storageType) {
		case STORAGE_PERSISTENT:
			persistentPolicy = policyValue;
			break;
		case STORAGE_VOLATILE:
			volatilePolicy = policyValue;
			break;
		case STORAGE_BOTH:
			persistentPolicy = policyValue;
			volatilePolicy = policyValue;
			break;
		}
	}

	/**
	 * Set the Secure Element Manager that should be used to access the secure
	 * element Called by SecureElementManager after creating the SecureElement
	 * instance
	 * 
	 * @param manager
	 *            the Secure Element Manager instance
	 */
	void setSecureElementManager(SecureElementManager manager) {
		this.manager = manager;
	}

	/**
	 * Check if the Secure Element is a UICC
	 * 
	 * @return true, if the Secure Element is a UICC
	 */
	public boolean isUicc() {
		return ((capabilities & ConstantAutogen.W_SE_FLAG_UICC) != 0);
	}

	/**
	 * Check if provided policy protocol is meantime allowed for the RF
	 * Interface of the Secure Element
	 * 
	 * @param policy
	 *            policy protocol
	 * @return true if the protocol is meantime allowed
	 */
	public boolean isVolatilePolicySet(PolicyProtocol policy) {
		return (volatilePolicy & policy.getIntValue()) != 0;
	}

	/**
	 * Set policy protocols for the secure element
	 * 
	 * @param storageType
	 *            storage type to update the policy in
	 * @param policy
	 *            new set of allowed policy protocols (null or zero-length array
	 *            if no protocols are allowed)
	 * @throws SecureElementException
	 */
	public void setPolicy(PolicyStorageType storageType, PolicyProtocol[] policy)
			throws SecureElementException {

		manager.setPolicy(getSlotIdentifier(), storageType.getIntValue(),
				PolicyProtocol.getIntValue(policy));

		/* store new value */
		storePolicy(storageType, policy);
	}

	/**
	 * Gets the identifier which is used by a Routing Table to route data to
	 * this target
	 * 
	 * @return the target identifier
	 */
	int getTargetId() {
		return this.slotIdentifier;
	}

	/**
	 * The status of a SWP link to the Secure Element
	 * 
	 */
	public enum SWPStatus implements Parcelable {
		/** The slot is not connected to a SWP line */
		NO_CONNECTION(ConstantAutogen.W_SE_SWP_STATUS_NO_CONNECTION,
				"SE is not connected to SWP"),
		/** The Secure Element is not present */
		NO_SECURE_ELEMENT(ConstantAutogen.W_SE_SWP_STATUS_NO_SE,
				"SE is not present"),
		/** The SWP line is down */
		SWP_DOWN(ConstantAutogen.W_SE_SWP_STATUS_DOWN, "SWP line is down"),
		/** Error of the SWP link */
		SWP_LINK_ERROR(ConstantAutogen.W_SE_SWP_STATUS_ERROR, "SWP link error"),
		/** The SWP link is initializing */
		SWP_INITIALIZATION(ConstantAutogen.W_SE_SWP_STATUS_INITIALIZATION,
				"SWP link is initializing"),
		/** The SWP link is active */
		SWP_ACTIVE(ConstantAutogen.W_SE_SWP_STATUS_ACTIVE, "SWP link is active");

		private final int intValue;
		private final String description;

		private SWPStatus(int intValue, String description) {
			this.intValue = intValue;
			this.description = description;
		}

		private int getIntValue() {
			return intValue;
		}

		public String toString() {
			return description;
		}

		static SWPStatus getSWPStatus(int intValue) {
			for (SWPStatus swpStatus : SWPStatus.values()) {
				if (swpStatus.intValue == intValue) {
					return swpStatus;
				}
			}
			return null;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(getIntValue());
		}

		/* @hide */public static final Parcelable.Creator<SWPStatus> CREATOR = new Parcelable.Creator<SWPStatus>() {

			public SWPStatus createFromParcel(Parcel dest) {
				int intValue = dest.readInt();
				return getSWPStatus(intValue);
			}

			@Override
			public SWPStatus[] newArray(int size) {
				return new SWPStatus[size];
			}
		};
	}

	/**
	 * Get SWP status for the secure element
	 * 
	 * @return status of SWP line
	 */
	public SWPStatus getSWPStatus() throws SecureElementException {
		return SWPStatus
				.getSWPStatus(manager.getSWPStatus(getSlotIdentifier()));
	}

	/**
	 * Activate the SWP line for the Secure Element
	 * 
	 * @throws SecureElementException
	 */
	void activateSWPLine() throws SecureElementException {
		manager.activateSWPLine(getSlotIdentifier());
	}

	/**
	 * String representation
	 */
	@Override
	public String toString() {
		return "SecureElement [name=" + description + ", supportedProtocols=0x"
				+ Integer.toHexString(supportedProtocols) + ", capabilities=0x"
				+ Integer.toHexString(capabilities) + ", volatilePolicy="
				+ PolicyProtocol.getDescription(getVolatilePolicy())
				+ ", persistentPolicy="
				+ PolicyProtocol.getDescription(getPersistentPolicy()) + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(slotIdentifier);
		dest.writeString(description);
		dest.writeInt(supportedProtocols);
		dest.writeInt(capabilities);
		dest.writeInt(volatilePolicy);
		dest.writeInt(persistentPolicy);
	}

	public static final Parcelable.Creator<SecureElement> CREATOR = new Parcelable.Creator<SecureElement>() {

		public SecureElement createFromParcel(Parcel dest) {
			int slotIdentifier = dest.readInt();
			String description = dest.readString();
			int supportedProtocols = dest.readInt();
			int capabilities = dest.readInt();
			int volatilePolicy = dest.readInt();
			int persistentPolicy = dest.readInt();
			return new SecureElement(slotIdentifier, description,
					supportedProtocols, capabilities, volatilePolicy,
					persistentPolicy);
		}

		@Override
		public SecureElement[] newArray(int size) {
			return new SecureElement[size];
		}
	};
}