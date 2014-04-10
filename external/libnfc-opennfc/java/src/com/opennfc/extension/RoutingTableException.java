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
 * This RoutingTableException is thrown when an error is detected during utilizing Open NFC Extension for 
 *  access to the Routing Table
 *
 */
public final class RoutingTableException extends OpenNfcException  {

	
	static {
		// set specific messages for some error codes
		exceptionMsgs[ConstantAutogen.W_ERROR_FEATURE_NOT_SUPPORTED] = "Routing table is not supported"; 
	}
	
	private RoutingTableException() {
		super();
	}

	protected RoutingTableException(final String message) {
        super(message);
    }

	/**
	 * Constructs a new RoutingTableException
	 * @param message the detail message for this exception
	 * @param throwable the cause of this exception
	 */
	protected RoutingTableException(final String message, Throwable throwable) {
        super(message, throwable);
    }
	
	protected RoutingTableException(int error) {
        super(error);
    }
}
