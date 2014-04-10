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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The class to contain RF activity state that is provided to a user application 
 *
 */
public final class RFActivity implements Parcelable {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;

	/* Reader RF state */ 
	private RFActivityState readerState;
	
	/* Card RF state */ 
	private RFActivityState cardState;
	  
	/* Peer to Peer RF state */ 
	private RFActivityState p2pState;
	
	/** The RF activity state for a particular mode: reader, card emulation or Peer to Peer */
	public enum RFActivityState {
		/** No Reader, card or p2p protocol is used */
		INACTIVE (ConstantAutogen.W_NFCC_RF_ACTIVITY_INACTIVE, "INACTIVE"),
		/** the detection sequence is performed */
		DETECTION (ConstantAutogen.W_NFCC_RF_ACTIVITY_DETECTION, "DETECTION"),
		/** Reader, card or p2p protocol is active */
		ACTIVE (ConstantAutogen.W_NFCC_RF_ACTIVITY_ACTIVE, "ACTIVE");

		private final int intValue;
		private final String description;

		
		private RFActivityState (int intValue, String description) {
			this.intValue = intValue;
			this.description = description;
		}
		
		int getIntValue() {
			return intValue;
		}

		public String toString() {
			return description;
		}
		
		static RFActivityState getRFActivityState(int intValue) {
			for (RFActivityState state: RFActivityState.values()) {
				if (state.intValue == intValue) {
					return state;
				}
			}
			return null;
		}
	}
	
	/**
	 * Construct RFActivity
	 */
	RFActivity(int readerState, int cardState, int p2pState) {
		this.readerState = RFActivityState.getRFActivityState(readerState); 
		this.cardState = RFActivityState.getRFActivityState(cardState); 
		this.p2pState = RFActivityState.getRFActivityState(p2pState); 
	}

	/**
	 * Get the reader RF state
	 * @return the reader RF state
	 */
	public RFActivityState getReaderState() {
		return readerState;
	}

	/**
	 * Get the card emulation RF state
	 * @return the card emulation RF state
	 */
	public RFActivityState getCardState() {
		return cardState;
	}

	/**
	 * Get the Peer to Peer RF state
	 * @return the Peer to Peer RF state
	 */
	public RFActivityState getP2pState() {
		return p2pState;
	}
	
	@Override
	public String toString() {
		return "RFActivity [readerState=" + readerState + ", cardState=" + cardState + ", p2pState=" + p2pState + "]";
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(readerState.getIntValue());
		dest.writeInt(cardState.getIntValue());
		dest.writeInt(p2pState.getIntValue());
	}
	
    public static final Parcelable.Creator<RFActivity> CREATOR =
            new Parcelable.Creator<RFActivity>() {
    	
        public RFActivity createFromParcel(Parcel dest) {
	        	int readerState = dest.readInt();
	        	int cardState = dest.readInt();
	        	int p2pState = dest.readInt();
	    		return new RFActivity(readerState, cardState, p2pState);
	    }
        
        @Override
        public RFActivity[] newArray(int size) {
            return new RFActivity[size];
        }
    };
	
}