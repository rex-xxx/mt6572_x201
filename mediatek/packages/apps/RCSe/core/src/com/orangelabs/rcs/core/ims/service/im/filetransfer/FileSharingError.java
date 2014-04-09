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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import com.orangelabs.rcs.core.ims.service.ImsServiceError;

/**
 * File transfer error
 * 
 * @author jexa7410
 */
public class FileSharingError extends ImsServiceError {
	static final long serialVersionUID = 1L;
	
	/**
	 * Unexpected exception occurs in the module (e.g. internal exception)
	 */
	public final static int UNEXPECTED_EXCEPTION = 0x01;
	
	/**
	 * Session initiation has failed (e.g. 408 timeout)
	 */
	public final static int SESSION_INITIATION_FAILED = 0x02;
	
	/**
	 * Session initiation has been declined (e.g. 603 Decline)
	 */
	public final static int SESSION_INITIATION_DECLINED = 0x03;	

	/**
	 * Session initiation has been cancelled (e.g. 487 Session terminated)
	 */
	public final static int SESSION_INITIATION_CANCELLED = 0x04;	
	
	/**
	 * Media transfer has failed (e.g. MSRP failure)
	 */
	public final static int MEDIA_TRANSFER_FAILED = 0x05;
	
	/**
	 * Unsupported media type (e.g. codec not supported)
	 */
	public final static int UNSUPPORTED_MEDIA_TYPE = 0x06;

	/**
	 * Media saving has failed (e.g. sdcard is not correctly mounted)
	 */
	public final static int MEDIA_SAVING_FAILED = 0x07;
	
    /**M:ALPS00507513. ADDED to reslove issue of wrong prompt in case of
     file transfer timeout@{ 
    **/  
	/**
     * Session initiation has been time out ( No response)
     */
	public final static int SESSION_INITIATION_TIMEOUT = 0x08;
    /**@}**/	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 */
	public FileSharingError(int code) {
		super(code);
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 * @param msg Detail message 
	 */
	public FileSharingError(int code, String msg) {
		super(code, msg);
	}
}
