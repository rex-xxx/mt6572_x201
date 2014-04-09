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
package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.util.Hashtable;

/**
 * Report transaction
 * 
 * @author jexa7410
 */
public class ReportTransaction extends Object {
    /**
     * MRSP report transaction timeout (in seconds)
     */
    private final static int TIMEOUT = 3600; // TODO: which value ?

    /**
     * Reported size
     */
    private long reportedSize = 0L;

    /**
     * Status code
     */
    private int statusCode = -1;
    
    /**
	 * Constructor
	 */
	public ReportTransaction() {
	}
	
	/**
	 * Notify report
	 * 
	 * @param headers MSRP headers
	 */
	public void notifyReport(Hashtable<String, String> headers) {
		synchronized(this) {
			// Get status code
			String status = headers.get(MsrpConstants.HEADER_STATUS);
			if ((status != null) && (status.startsWith("000 "))) {
				String[] parts = status.split(" "); 
				if (parts.length > 0) {
					try {
						statusCode = Integer.parseInt(parts[1]);
					} catch(NumberFormatException e) {
						statusCode = -1;
					}
				}
			}
			
			// Get reported size
			String byteRange = headers.get(MsrpConstants.HEADER_BYTE_RANGE);
			if (byteRange != null) {
				reportedSize = MsrpUtils.getChunkSize(byteRange);
			}

			// Unblock semaphore
			super.notify();
		}
	}
	
	/**
	 * Wait report
	 * 
	 * @return True if success else returns false
	 */
	public void waitReport() {
		synchronized(this) {
			try {
				// Wait semaphore
				super.wait(TIMEOUT * 1000);
			} catch(InterruptedException e) {
			    // Nothing to do
			}
		}
	}
	
	/**
	 * Terminate transaction
	 */
	public void terminate() {
		synchronized(this) {
			// Unblock semaphore
			super.notify();
		}
	}
	
	/**
	 * Returns the reported data size
	 * 
	 * @return Size in bytes
	 */
	public long getReportedSize() {
		return reportedSize;
	}

	/**
	 * Returns the status
	 * 
	 * @return Status or -1 in case of error
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
