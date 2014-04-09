/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.orangelabs.rcs.core.ims.service.ImsServiceError;

/**
 * Chat error
 * 
 * @author jexa7410
 */
public class ChatError extends ImsServiceError {
	static final long serialVersionUID = 1L;
	
	/**
	 * Unexpected exception occurs in the module (e.g. internal exception)
	 */
	public final static int UNEXPECTED_EXCEPTION = 0x01;
	
	/**
	 * IM session initiation has failed (e.g. 408 timeout)
	 */
	public final static int SESSION_INITIATION_FAILED = 0x02;

	/**
	 * Session initiation has been declined (e.g. 486 Busy)
	 */
	public final static int SESSION_INITIATION_DECLINED = 0x03;	

	/**
	 * Session initiation has been cancelled (e.g. 487 Session terminated)
	 */
	public final static int SESSION_INITIATION_CANCELLED = 0x04;	
	
	/**
	 * Media session has failed (e.g. MSRP failure)
	 */
	public final static int MEDIA_SESSION_FAILED = 0x05;
	
	/**
	 * Subscription to conference package has failed
	 */
	public final static int SUBSCRIBE_CONFERENCE_FAILED = 0x06;
	
	/**
	 * Group chat session not found in the server 
	 */
	public final static int SESSION_NOT_FOUND = 0x07;
	
	/**
	 * Group chat restart has failed
	 */
	public final static int SESSION_RESTART_FAILED = 0x08;
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 */
	public ChatError(int code) {
		super(code);
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 * @param msg Detail message 
	 */
	public ChatError(int code, String msg) {
		super(code, msg);
	}
}
