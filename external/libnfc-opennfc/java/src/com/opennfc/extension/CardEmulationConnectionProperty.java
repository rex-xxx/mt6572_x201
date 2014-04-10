/*
 * Copyright (c) 2010 Inside Secure, All Rights Reserved.
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

/**
 * The connection properties are constant values describing the features of a
 * connection.
 * 
 * <p>
 * A connection property may be:
 * <ul>
 * <li>a protocol supported,</li>
 * <li>the format of the tag, or</li>
 * <li>the physical type of the tag.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The card properties are used to register a card detection event handler with
 * {@link com.opennfc.extension.CardEmulationManager#createCardEmulation
 * CardEmulationManager.createCardEmulation()}.
 * </p>
 * <p>
 * The connection properties are also used in the card emulation API and in the
 * Virtual Tag API. They are needed to specify the type of card emulation
 * requested. The method
 * {@link com.opennfc.extension.CardEmulationManager#checkConnectionProperty
 * CardEmulationManager.checkConnectionProperty()} checks if a property is
 * supported by the NFC Controller for the card emulation functions.
 * </p>
 * 
 * @since Open NFC 4.0
 */
public enum CardEmulationConnectionProperty {

	/** Communicate with protocol ISO 14443 part 4 type A. */
	ISO_14443_4_A(ConstantAutogen.W_PROP_ISO_14443_4_A),

	/** Communicate with protocol ISO 14443 part 4 type B */
	ISO_14443_4_B(ConstantAutogen.W_PROP_ISO_14443_4_B);

	/**
	 * The identifier of the property
	 * 
	 * @hide
	 */
	private int identifier;

	/**
	 * private constructor to prevent instantiation.
	 * 
	 * @hide
	 */
	private CardEmulationConnectionProperty(int identifier) {
		this.identifier = identifier;
	}

	/**
	 * Returns the property value.
	 * 
	 * @return the property value.
	 * 
	 * @hide
	 */
	public int getValue() {
		return this.identifier;
	}

	/**
	 * Returns the connection property corresponding to an identifier.
	 * 
	 * @param identifier
	 *            the property identifier.
	 * 
	 * @return the connection property.
	 * 
	 * @hide
	 **/
	public static CardEmulationConnectionProperty getConnectionProperty(
			int identifier) {

		switch (identifier) {
		case ConstantAutogen.W_PROP_ISO_14443_4_A:
			return ISO_14443_4_A;
		case ConstantAutogen.W_PROP_ISO_14443_4_B:
			return ISO_14443_4_B;
		}

		throw new InternalError("Wrong property identifier");
	}
}
