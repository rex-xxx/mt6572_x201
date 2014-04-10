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

import java.util.Hashtable;

/**
 * The OpenNfcException is a parent class for exceptions thrown by Open NFC Extensions
 *
 */
public class OpenNfcException extends Exception {

	/**
	 * The Exception to throw if there is no connection to Open NFC Extension Service 
	 */
	final static public String SERVICE_COMMUNICATION_FAILED = "Communication to service failed";
	/* @hide */ final static public String SERVICE_SE_FAILED = "Se service failed";	
	protected static String exceptionMsgs[] = {
		"W_SUCCESS", 							//  0x00000000
		"W_ERROR_VERSION_NOT_SUPPORTED", 		//  0x00000001
		"W_ERROR_ITEM_NOT_FOUND",	        	//  0x00000002
		"W_ERROR_BUFFER_TOO_SHORT"             , // 0x00000003
		"W_ERROR_PERSISTENT_DATA"              , // 0x00000004
		"W_ERROR_NO_EVENT"                     , // 0x00000005
		"W_ERROR_WAIT_CANCELLED"               , // 0x00000006
		"W_ERROR_UICC_COMMUNICATION"           , // 0x00000007
		"W_ERROR_BAD_HANDLE"                   , // 0x00000008
		"W_ERROR_EXCLUSIVE_REJECTED"           , // 0x00000009
		"W_ERROR_SHARE_REJECTED"               , // 0x0000000A
		"W_ERROR_BAD_PARAMETER"                , // 0x0000000B
		"W_ERROR_RF_PROTOCOL_NOT_SUPPORTED"    , // 0x0000000C
		"W_ERROR_CONNECTION_COMPATIBILITY"     , // 0x0000000D
		"W_ERROR_BUFFER_TOO_LARGE"             , // 0x0000000E
		"W_ERROR_INDEX_OUT_OF_RANGE"           , // 0x0000000F
		"W_ERROR_OUT_OF_RESOURCE"              , // 0x00000010
		"W_ERROR_BAD_TAG_FORMAT"               , // 0x00000011
		"W_ERROR_BAD_NDEF_FORMAT"              , // 0x00000012
		"W_ERROR_NDEF_UNKNOWN"                 , // 0x00000013
		"W_ERROR_LOCKED_TAG"                   , // 0x00000014
		"W_ERROR_TAG_FULL"                     , // 0x00000015
		"W_ERROR_CANCEL"                       , // 0x00000016
		"W_ERROR_TIMEOUT"                      , // 0x00000017
		"W_ERROR_TAG_DATA_INTEGRITY"           , // 0x00000018
		"W_ERROR_NFC_HAL_COMMUNICATION"        , // 0x00000019
		"W_ERROR_WRONG_RTD"                    , // 0x0000001A
		"W_ERROR_TAG_WRITE"                    , // 0x0000001B
		"W_ERROR_BAD_NFCC_MODE"                , // 0x0000001C
		"W_ERROR_TOO_MANY_HANDLERS"            , // 0x0000001D
		"W_ERROR_BAD_STATE"                    , // 0x0000001E
		"W_ERROR_BAD_FIRMWARE_FORMAT"          , // 0x0000001F
		"W_ERROR_BAD_FIRMWARE_SIGNATURE"       , // 0x00000020
		"W_ERROR_DURING_HARDWARE_BOOT"         , // 0x00000021
		"W_ERROR_DURING_FIRMWARE_BOOT"         , // 0x00000022
		"W_ERROR_FEATURE_NOT_SUPPORTED"        , // 0x00000023
		"W_ERROR_CLIENT_SERVER_PROTOCOL"       , // 0x00000024
		"W_ERROR_FUNCTION_NOT_SUPPORTED"       , // 0x00000025
		"W_ERROR_TAG_NOT_LOCKABLE"             , // 0x00000026
		"W_ERROR_ITEM_LOCKED"                  , // 0x00000027
		"W_ERROR_SYNC_OBJECT"                  , // 0x00000028
		"W_ERROR_RETRY"                        , // 0x00000029
		"W_ERROR_DRIVER"                       , // 0x0000002A
		"W_ERROR_MISSING_INFO"                 , // 0x0000002B
		"W_ERROR_P2P_CLIENT_REJECTED"          , // 0x0000002C
		"W_ERROR_NFCC_COMMUNICATION"           , // 0x0000002D
		"W_ERROR_RF_COMMUNICATION"             , // 0x0000002E
		"W_ERROR_BAD_FIRMWARE_VERSION"         , // 0x0000002F
		"W_ERROR_HETEROGENEOUS_DATA"           , // 0x00000030
		"W_ERROR_CLIENT_SERVER_COMMUNICATION"  , // 0x00000031
		"W_ERROR_SECURITY"                     , // 0x00000032
		"W_ERROR_OPERATION_PENDING"            , // 0x00000033
		"W_ERROR_PROGRAMMING"                  , // 0x00000034
	};
		
	protected OpenNfcException() {
		super();
	}
	
    /**
     * Creates a new OpenNfcException.
     *
     * @param  message  the exception message.
     **/
	protected OpenNfcException(final String message) {
        super(message);
    }

	/**
	 * Constructs a new OpenNfcException
	 * @param message the detail message for this exception
	 * @param throwable the cause of this exception
	 */
	protected OpenNfcException(final String message, Throwable throwable) {
        super(message, throwable);
    }
	
    /**
     * Creates a new OpenNfcException.
     *
     * @param  message  the exception message.
     **/
	protected static String getExceptionMessage(int error) {
    	return (error >= exceptionMsgs.length) ? 
			new String ("Error 0x" + Integer.toHexString(error)) :
    		exceptionMsgs[error];
    }
	
	/**
     * Creates a new OpenNfcException.
	 * @param error error code returned by Open NFC
	 */
			
	protected OpenNfcException(int error) {
		this(getExceptionMessage(error));
	}
    
}
