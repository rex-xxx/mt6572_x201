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
 * This VirtualTagException is thrown when an error is detected during access to
 * Open NFC Extension for Virtual Tag
 * 
 */
public final class VirtualTagException extends OpenNfcException {

	final static public String VT_ERROR_SERVICE = "Service error.";
	final static public String VT_ERROR_CREATE = "Create VirtualTag error";
	final static public String VT_ERROR_VT_ALREADY_CREATED = "VirtualTag already created";
	final static public String VT_ERROR_VT_ALREADY_DESTROYED = "VirtualTag is null or already destroyed";
	final static public String VT_ERROR_SET_VTMODE = "can not set VirtualTag Mode";
	final static public String VT_ERROR_VTMODE_ALREADY_STARTED = "VirtualTag Mode already started";
	final static public String VT_ERROR_VTMODE_ALREADY_STOPPED = "VirtualTag Mode already stopped";
	final static public String VT_ERROR_REGISTER_LISTENER = "Can not register remote listener";
	final static public String VT_ERROR_START = "VirtualTag start error";
	final static public String VT_ERROR_STOP = "Stop VirtualTag error";
	final static public String VT_ERROR_DESTORY = "Can not destory VirtualTag";
	final static public String VT_ERROR_SHOULD_CALL_STOP = "Should call stop before this operation";
	final static public String VT_ERROR_READ_MESSAGE = "Received NdefMessage data is null or read Ndef Message error";
	final static public String VT_ERROR_WRITE_MESSAGE = "Write NdefMessage with error";

	static {
		// set specific messages for some error codes
	}

	private VirtualTagException() {
		super();
	}

	protected VirtualTagException(final String message) {
		super(message);
	}

	protected VirtualTagException(int error) {
		super(error);
	}

	public VirtualTagException(String message, Throwable cause) {
		super(message, cause);
	}
}