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
 * The class to contain RF lock state that is provided to a user application 
 *
 */
public final class RFLock implements Parcelable {

	/* Reader RF state */ 
	private boolean readerState;
	
	/* Card Emulation RF state */ 
	private boolean cardState;
	  
	/**
	 * Constructs RF Lock instance
	 * @param readerState the reader RF lock state
	 * @param cardState the card RF lock state
	 */
	public RFLock(boolean readerState, boolean cardState) {
		this.readerState = readerState; 
		this.cardState = cardState; 
	}

	/**
	 * Get the reader RF lock state
	 * @return the reader RF lock state
	 */
	public boolean getReaderState() {
		return readerState;
	}

	/**
	 * Get the card emulation RF lock state
	 * @return the card emulation RF lock state
	 */
	public boolean getCardState() {
		return cardState;
	}

	@Override
	public String toString() {
		return "RFLock [readerState=" + readerState + ", cardState=" + cardState + "]";
	}
	
	static private String getStringValue(boolean value) {
		return value ? "ON" : "OFF"; 
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(readerState ? 1 : 0);
		dest.writeInt(cardState ? 1 : 0);
	}
	
    public static final Parcelable.Creator<RFLock> CREATOR =
            new Parcelable.Creator<RFLock>() {
    	
        public RFLock createFromParcel(Parcel dest) {
	        	boolean readerState = dest.readInt() != 0;
	        	boolean cardState = dest.readInt() != 0;
	        	return new RFLock(readerState, cardState);
	    }
        
        @Override
        public RFLock[] newArray(int size) {
            return new RFLock[size];
        }
    };
	
}