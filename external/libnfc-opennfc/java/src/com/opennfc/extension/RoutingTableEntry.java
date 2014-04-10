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
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * The class to represent a Routing Table.
 * The routing table determines the routing target of commands coming from an external reader when the system is in card
 * emulation mode.
 * @see
 * <p>{@link RoutingTableManager}
 * <p>{@link RoutingTable}
 */
public final class RoutingTableEntry implements Parcelable {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	/** Tag use in debug */
	private static final String TAG = RoutingTableEntry.class.getSimpleName();

	/** The type of the routing entry */
	private EntryType  entryType; 
	private ValidityFlag[]  validityFlags; 
	private RoutingTableEntryTarget target;
	private int targetId; 
	private byte aid[]; 

	/** The type of the routing entry */
	public enum EntryType {
		/** This type of entry is used to specify a target for a specific AID  */
		AID_ENTRY (ConstantAutogen.W_ROUTING_TABLE_AID_ENTRY),
		/** This type of entry is used to specify a default target for SELECT without AID or 
		 with an AID not present in the table */
		AID_TARGET (ConstantAutogen.W_ROUTING_TABLE_DEFAULT_AID_TARGET),
		/** Policy for ISO 14443 B */
		APDU_TARGET (ConstantAutogen.W_ROUTING_TABLE_DEFAULT_APDU_TARGET);
		
		private final int intValue;
		
		private EntryType (int intValue) {
			this.intValue = intValue;
		}
		
		int getIntValue() {
			return intValue;
		}
		
		static private EntryType getEntryType(int intValue) {
			for (EntryType entryType: EntryType.values()) {
				if (entryType.intValue == intValue) {
					return entryType;
				}
			}
			return null;
		}
	}

	/** The routing entry validity flag */
	public enum ValidityFlag {
		/** When set, indicates the routing entry is valid in battery OFF mode  */
		BATTERY_OFF (ConstantAutogen.W_ROUTING_TABLE_POWERSTATE_BATT_OFF),
		/** When set, indicates the routing entry is valid in phone OFF mode */ 
		PHONE_OFF (ConstantAutogen.W_ROUTING_TABLE_POWERSTATE_PHONE_OFF),
		/** When set, indicates the routing entry is valid in phone ON mode */ 
		PHONE_ON (ConstantAutogen.W_ROUTING_TABLE_POWERSTATE_PHONE_ON);
		
		private final int intValue;
		
		private ValidityFlag (int intValue) {
			this.intValue = intValue;
		}
		
		/**
		 * Create an array with ValidityFlag enumerations based on the integer validity flag value in
		 * the OpenNFC format
		 * @param value integer validity flag value
		 * @return an array with ValidityFlag enumerations
		 */
		private static ValidityFlag[] getValidityFlags(int value) {
			
			ValidityFlag[] flags = new ValidityFlag[0];
			if (value != 0) {
				ArrayList<ValidityFlag> list = new ArrayList<ValidityFlag>();
				for (ValidityFlag vFlag: ValidityFlag.values()) {
					if ((vFlag.intValue & value) != 0) {
						list.add(vFlag);
					}
				}
				flags = (ValidityFlag[]) list.toArray(flags);
			}
			return flags;
		}

		private int getIntValue() {
			return intValue;
		}

		/**
		 * Get the string description of provided validity flags
		 * @param vFlags the array of validity flags
		 * @return the string description of validity flags
		 */
		static public String getDescription(ValidityFlag[] vFlags) {
			String outString = null;
			if ((vFlags == null) || (vFlags.length == 0)) {
				outString = "NONE";
			} else {
				StringBuilder strBuilder = new StringBuilder("[ ");
				for (int i=0; i<vFlags.length; i++) {
					strBuilder.append(vFlags[i].toString() + " ");
				}
				strBuilder.append("]");
				outString = strBuilder.toString();
			}
			return outString;
		}
		
		/**
		 * Get an integer value used by Open NFC for provided set of policy validity flags
		 * @param vFlags an array of validity flags
		 * @return the integer value that can be used for Open NFC 
		 */
		static int getIntValue(ValidityFlag[] vFlags) {
			int value = 0;
			if (vFlags != null) {
				for (int i=0; i<vFlags.length; i++) {
					value |= vFlags[i].getIntValue();
				}
			}
			return value;
		}
	}
	/**
	 * Creates an entry for a Routing Table
	 * @param entryType the type of the routing entry
	 * @param validityFlags the routing entry validity flag
	 * @param target the destination for the routing (can be a SecureElement or the OpenNFCExtManager.NFCC)
	 * @param aid the AID value
	 */
	public RoutingTableEntry(EntryType entryType, ValidityFlag[] validityFlags, RoutingTableEntryTarget target, byte aid[]) {
		this.entryType = entryType;
		this.validityFlags = validityFlags;
		this.target = target;
		this.targetId = target.getTargetId();
		this.aid = aid;
	}

	private RoutingTableEntry(int entryTypeValue, int validityFlagValue, int targetId, byte aid[]) {
		this.entryType = EntryType.getEntryType(entryTypeValue);
		this.validityFlags = ValidityFlag.getValidityFlags(validityFlagValue);
		this.targetId = targetId;
		this.aid = aid;
	}

	public EntryType getEntryType() {
		return entryType;
	}

	public ValidityFlag[] getValidityFlags() {
		return validityFlags;
	}

	public RoutingTableEntryTarget getTarget() {
		return target;
	}

	public byte[] getAid() {
		return aid;
	}

	public int getTargetId() {
		return targetId;
	}

	void setTarget(RoutingTableEntryTarget target) {
		this.target = target;
	}

	private int getEntryTypeValue() {
		return this.entryType.getIntValue();
	}

	private int getValidityFlagsValue() {
		return ValidityFlag.getIntValue(this.validityFlags);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public String toString() {
		return "RoutingTableEntry [entryType=" + entryType + ", validityFlags=" + Arrays.toString(validityFlags)
				+ ", target=" + target + ", aid=" + Utils.toHexadecimal(aid) + "]";
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(getEntryTypeValue());
		dest.writeInt(getValidityFlagsValue());
		dest.writeInt(targetId);
		dest.writeByteArray(aid);
	}

    public static final Parcelable.Creator<RoutingTableEntry> CREATOR =
            new Parcelable.Creator<RoutingTableEntry>() {
    	
        public RoutingTableEntry createFromParcel(Parcel dest) {
        	int entryType = dest.readInt();
        	int vFlags = dest.readInt();
        	int targetId = dest.readInt();
        	byte[] aid = dest.createByteArray();
    		return new RoutingTableEntry(entryType, vFlags, targetId, aid);
	    }
        
        @Override
        public RoutingTableEntry[] newArray(int size) {
            return new RoutingTableEntry[size];
        }
    };
}