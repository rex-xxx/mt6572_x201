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

/**
 * This SecureElementException is thrown when an error is detected during access
 * to Open NFC Extension for Secure Element
 * 
 */
public final class SecureElementException extends OpenNfcException {

	static {
		// set specific messages for some error codes
		exceptionMsgs[ConstantAutogen.W_ERROR_PERSISTENT_DATA] = "Writing to persistent memory failed";
		exceptionMsgs[ConstantAutogen.W_ERROR_BAD_STATE] = "Secure Element set policy operation is already pending";
	}

	private SecureElementException() {
		super();
	}

	protected SecureElementException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new SecureElementException
	 * 
	 * @param message
	 *            the detail message for this exception
	 * @param throwable
	 *            the cause of this exception
	 */
	protected SecureElementException(final String message, Throwable throwable) {
		super(message, throwable);
	}

	protected SecureElementException(int error) {
		super(error);
	}
}