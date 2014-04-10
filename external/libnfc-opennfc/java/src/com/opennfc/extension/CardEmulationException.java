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
 * This CardEmulationException is thrown when an error is detected during access
 * to Open NFC Extension for Card Emulation
 * 
 */
public final class CardEmulationException extends OpenNfcException {

	final static public String CE_ERROR_SERVICE = "Service error.";
	final static public String CE_ERROR_CHECK_PROPERTY = "can not checkConnectionProperty";
	final static public String CE_ERROR_SET_CEMODE = "can not set CardEmulation Mode";
	final static public String CE_ERROR_CEMODE_ALREADY_STARTED = "CardEmulation Mode already started by current or other card emulation/Virtual Tag applications";
	final static public String CE_ERROR_CEMODE_ALREADY_STOPPED = "CardEmulation Mode already stopped";
	final static public String CE_ERROR_CE_ALREADY_STARTED = "CardEmulation already started";
	final static public String CE_ERROR_CE_ALREADY_STOPPED = "CardEmulation already stopped";
	final static public String CE_ERROR_REGISTER_LISTENER = "Can not register remote listener";
	final static public String CE_ERROR_OPEN_CONNECTION = "CardEmulation OpenConnection error";
	final static public String CE_ERROR_STOP = "Stop card emulation error";
	final static public String CE_ERROR_SEND_ANSWER = "CardEmulation send Answer error";

	static {
		// set specific messages for some error codes
	}

	private CardEmulationException() {
		super();
	}

	protected CardEmulationException(final String message) {
		super(message);
	}

	protected CardEmulationException(int error) {
		super(error);
	}

	public CardEmulationException(String message, Throwable cause) {
		super(message, cause);
	}
}