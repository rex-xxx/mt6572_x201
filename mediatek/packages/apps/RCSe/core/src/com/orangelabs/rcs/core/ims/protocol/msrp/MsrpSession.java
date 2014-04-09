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

import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;

/**
 * MSRP session
 * 
 * @author jexa7410
 */
public class MsrpSession {
	/**
	 * Failure report option
	 */
	private boolean failureReportOption = false;

	/**
	 * Success report option
	 */
	private boolean successReportOption = false;

	/**
	 * MSRP connection
	 */
	private MsrpConnection connection = null;
	
	/**
	 * From path
	 */
	private String from = null;
	
	/**
	 * To path
	 */
	private String to = null;
	
	/**
	 * Cancel transfer flag
	 */
	private boolean cancelTransfer = false;

	/**
	 * Request transaction
	 */
	private RequestTransaction requestTransaction = null;

	/**
	 * Received chunks
	 */
	private DataChunks receivedChunks = new DataChunks();	
	
    /**
     * MSRP event listener
     */
    private MsrpEventListener msrpEventListener = null;

    /**
     * Random generator
     */
	private static Random random = new Random(System.currentTimeMillis());

	/**
	 * Report transaction
	 */
	private ReportTransaction reportTransaction = null;

    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 */
	public MsrpSession() {
	}
	
	/**
	 * Generate a unique ID for transaction
	 * 
	 * @return ID
	 */
	private static synchronized String generateTransactionId() {
		return Long.toHexString(random.nextLong());		
	}
	
	/**
	 * Is failure report requested
	 * 
	 * @return Boolean
	 */
	public boolean isFailureReportRequested() {
		return failureReportOption;
	}

	/**
	 * Set the failure report option
	 * 
	 * @param failureReportOption Boolean flag
	 */
	public void setFailureReportOption(boolean failureReportOption) {
		this.failureReportOption = failureReportOption;
	}

	/**
	 * Is success report requested
	 * 
	 * @return Boolean
	 */
	public boolean isSuccessReportRequested() {
		return successReportOption;
	}

	/**
	 * Set the success report option
	 * 
	 * @param successReportOption Boolean flag
	 */
	public void setSuccessReportOption(boolean successReportOption) {
		this.successReportOption = successReportOption;
	}	

	/**
	 * Set the MSRP connection
	 * 
	 * @param connection MSRP connection
	 */
	public void setConnection(MsrpConnection connection) {
		this.connection = connection;
	}

	/**
	 * Returns the MSRP connection
	 * 
	 * @return MSRP connection
	 */
	public MsrpConnection getConnection() {
		return connection;
	}
	
	/**
	 * Get the MSRP event listener
	 * 
	 * @return Listener 
	 */
	public MsrpEventListener getMsrpEventListener() {
		return msrpEventListener;
	}
	
	/**
	 * Add a MSRP event listener
	 * 
	 * @param listener Listener 
	 */
	public void addMsrpEventListener(MsrpEventListener listener) {
		this.msrpEventListener = listener;		
	}
	
	/**
	 * Returns the From path
	 * 
	 * @return From path
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Set the From path
	 * 
	 * @param from From path
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * Returns the To path
	 *  
	 * @return To path
	 */
	public String getTo() {
		return to;
	}

	/**
	 * Set the To path
	 * 
	 * @param to To path
	 */
	public void setTo(String to) {
		this.to = to;
	}

	/**
	 * Close the session
	 */
	public void close() {
		if (logger.isActivated()) {
			logger.debug("Close session");
		}

		// Cancel transfer
		cancelTransfer = true;

		// Close the connection
		if (connection != null) {
			connection.close();
		}
		
		// Unblock request transaction
		if (requestTransaction != null) {
			requestTransaction.terminate();
		}

		// Unblock report transaction
		if (reportTransaction != null) {
			reportTransaction.terminate();
		}
	}

	/**
	 * Send chunks
	 * 
	 * @param inputStream Input stream
	 * @param msgId Message ID
	 * @param contentType Content type to be sent
	 * @param totalSize Total size of content
	 * @throws MsrpException
	 */
	public void sendChunks(InputStream inputStream, String msgId, String contentType, long totalSize) throws MsrpException {
		if (logger.isActivated()) {
			logger.info("Send content (" + contentType + ")");
		}

		if (from == null) {
			throw new MsrpException("From not set");
		}
		
		if (to == null) {
			throw new MsrpException("To not set");
		}
		
		if (connection == null) {
			throw new MsrpException("No connection set");
		}
		
		// Send content over MSRP 
		try {
			byte data[] = new byte[MsrpConstants.CHUNK_MAX_SIZE];
			long firstByte = 1;
			long lastByte = 0;
			cancelTransfer = false;
			if (successReportOption) {
				reportTransaction = new ReportTransaction();
			} else {
				reportTransaction = null;
			}
			
			// Send data chunk by chunk
			for (int i = inputStream.read(data); (!cancelTransfer) & (i>-1); i=inputStream.read(data)) {
				// Update upper byte range
				lastByte += i;

				// Send a chunk
				sendMsrpSendRequest(generateTransactionId(), to, from, msgId, contentType, i, data, firstByte, lastByte, totalSize);

				// Update lower byte range
				firstByte += i;
				
				if (!cancelTransfer) {
					// Notify event listener
					msrpEventListener.msrpTransferProgress(lastByte, totalSize);
				}
			}
			
			if (cancelTransfer) {
				// Transfer has been aborted
				return;
			}

			// Test if waiting report is needed
			if (reportTransaction != null) {
				// Wait until all data have been reported
				long lastReport = reportTransaction.getReportedSize();
				while(reportTransaction.getReportedSize() < totalSize) {
					reportTransaction.waitReport();
					if (lastReport == reportTransaction.getReportedSize()) {
						// Timeout
						break;
					}					
					if (reportTransaction.getStatusCode() != 200) {
						// Error
						break;
					}					
					lastReport = reportTransaction.getReportedSize();
				}
				
	            // Notify event listener
				/**
		         * M: Modified to change && to ||. @{
		         */
	            if ((reportTransaction.getStatusCode() == 200) ||
	            		(reportTransaction.getReportedSize() == totalSize)) {
	                msrpEventListener.msrpDataTransfered(msgId);
	            } else {
	                msrpEventListener.msrpTransferError("report timeout");
	            }
	            /**
	             * @}
	             */
			} else {
	            // Notify event listener
                msrpEventListener.msrpDataTransfered(msgId);
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Send chunk failed", e);
			}
			throw new MsrpException(e.getMessage());
		}
	}

	/**
	 * Send empty chunk
	 * 
	 * @throws MsrpException
	 */
	public void sendEmptyChunk() throws MsrpException {
		if (logger.isActivated()) {
			logger.info("Send an empty chunk");
		}
		
		if (from == null) {
			throw new MsrpException("From not set");
		}
		
		if (to == null) {
			throw new MsrpException("To not set");
		}
		
		if (connection == null) {
			throw new MsrpException("No connection set");
		}
		
		// Send an empty chunk
		try {
			sendEmptyMsrpSendRequest(generateTransactionId(), to, from, generateTransactionId());
		} catch(MsrpException e) {
			throw e;
		} catch(Exception e) {
			throw new MsrpException(e.getMessage());
		}
	}

	/**
	 * Send MSRP SEND request
	 * 
	 * @param transactionId Transaction ID
	 * @param to To header
	 * @param from From header
	 * @param msgId Message ID header
	 * @param contentType Content type 
	 * @param dataSize Data chunk size
	 * @param data Data chunk
	 * @param firstByte First byte range
	 * @param lastByte Last byte range
	 * @param totalSize Total size
	 * @throws IOException 
	 * @throws MsrpException
	 */
	private void sendMsrpSendRequest(String txId, String to, String from, String msgId, String contentType,
			int dataSize, byte data[], long firstByte, long lastByte, long totalSize) throws MsrpException, IOException {
        /**
         * M: Added to fix issue: can not send display notification-add log @{
         */
        if (logger.isActivated()) {
            logger.debug("sendMsrpSendRequest(),txId(transactionid)=" + txId + ",contentType + "
                    + contentType + ", msgId = " + msgId + ",totalSize = " + totalSize);
        }
        /**
         * @}
         */

		boolean isLastChunk = (lastByte == totalSize);
		
		// Create request
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(4000);
		buffer.reset();
		buffer.write(MsrpConstants.MSRP_PROTOCOL.getBytes());
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write(txId.getBytes());
		buffer.write((" " + MsrpConstants.METHOD_SEND).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		String toHeader = MsrpConstants.HEADER_TO_PATH + ": " + to + MsrpConstants.NEW_LINE;
		buffer.write(toHeader.getBytes());
		String fromHeader = MsrpConstants.HEADER_FROM_PATH + ": " + from + MsrpConstants.NEW_LINE;
		buffer.write(fromHeader.getBytes());

        /**
         * M: Added to fix issue: can not send display notification. @{
         */
        /*
         * Message-ID is modified by Vodafone's server, and receiver's
         * imdn.Message-ID is the same with the sender. So we do not care
         * Message-ID. If assign msgId to MsrpConstants.HEADER_MESSAGE_ID then
         * it cause the failure of send display notification
         */

        // String msgIdHeader = MsrpConstants.HEADER_MESSAGE_ID + ": " + msgId +
        // MsrpConstants.NEW_LINE;
        String msgIdHeader = MsrpConstants.HEADER_MESSAGE_ID + ": " + ChatUtils.generateMessageId()
                + MsrpConstants.NEW_LINE;
        /**
         * @}
         */
        
		buffer.write(msgIdHeader.getBytes());
		
		// Write byte range
		String byteRange = MsrpConstants.HEADER_BYTE_RANGE + ": " + firstByte + "-" + lastByte + "/" + totalSize + MsrpConstants.NEW_LINE;
		buffer.write(byteRange.getBytes());
		
		// Write optional headers
		if (!failureReportOption) {
			String header = MsrpConstants.HEADER_FAILURE_REPORT + ": yes" + MsrpConstants.NEW_LINE;
			buffer.write(header.getBytes());
		}
		if (successReportOption) {
			String header = MsrpConstants.HEADER_SUCCESS_REPORT + ": yes" + MsrpConstants.NEW_LINE;
			buffer.write(header.getBytes());
		}

		// Write content type
		if (contentType != null) {
			String content = MsrpConstants.HEADER_CONTENT_TYPE + ": " + contentType + MsrpConstants.NEW_LINE; 
			buffer.write(content.getBytes());
		}		

		// Write data
		if (data != null) {
			buffer.write(MsrpConstants.NEW_LINE.getBytes());
			buffer.write(data, 0, dataSize);
			buffer.write(MsrpConstants.NEW_LINE.getBytes());
		}
		
		// Write end of request
		buffer.write(MsrpConstants.END_MSRP_MSG.getBytes());
		buffer.write(txId.getBytes());
		if (isLastChunk) {
			// '$' -> last chunk
			buffer.write(MsrpConstants.FLAG_LAST_CHUNK);
		} else {
			// '+' -> more chunk
			buffer.write(MsrpConstants.FLAG_MORE_CHUNK);
		}
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
		
		// Send chunk
		if (failureReportOption) {
			requestTransaction = new RequestTransaction();
			connection.sendChunk(buffer.toByteArray());
			buffer.close();
			requestTransaction.waitResponse();
			if (!requestTransaction.isResponseReceived()) {
				throw new MsrpException("timeout");
			}
		} else {
			connection.sendChunk(buffer.toByteArray());
			buffer.close();
		}
	}
	
	/**
	 * Send an empty MSRP SEND request
	 * 
	 * @param transactionId Transaction ID
	 * @param to To header
	 * @param from From header
	 * @param msgId Message ID header
	 * @throws MsrpException
	 * @throws IOException
	 */
	private void sendEmptyMsrpSendRequest(String txId, String to, String from, String msgId) throws MsrpException, IOException {
		// Create request
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(4000);
		buffer.reset();
		buffer.write(MsrpConstants.MSRP_PROTOCOL.getBytes());
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write(txId.getBytes());
		buffer.write((" " + MsrpConstants.METHOD_SEND).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		String toHeader = MsrpConstants.HEADER_TO_PATH + ": " + to + MsrpConstants.NEW_LINE;
		buffer.write(toHeader.getBytes());
		String fromHeader = MsrpConstants.HEADER_FROM_PATH + ": " + from + MsrpConstants.NEW_LINE;
		buffer.write(fromHeader.getBytes());
		String msgIdHeader = MsrpConstants.HEADER_MESSAGE_ID + ": " + msgId + MsrpConstants.NEW_LINE;
		buffer.write(msgIdHeader.getBytes());
		
		// Write end of request
		buffer.write(MsrpConstants.END_MSRP_MSG.getBytes());
		buffer.write(txId.getBytes());
		// '$' -> last chunk
		buffer.write(MsrpConstants.FLAG_LAST_CHUNK);
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
		
		// Send chunk
		requestTransaction = new RequestTransaction();
		connection.sendChunkImmediately(buffer.toByteArray());
		buffer.close();
		requestTransaction.waitResponse();
		if (!requestTransaction.isResponseReceived()) {
			throw new MsrpException("timeout");
		}
	}

	/**
	 * Send MSRP response
	 * 
	 * @param code Response code
	 * @param txId Transaction ID
	 * @param headers MSRP headers
	 * @throws IOException
	 */
	private void sendMsrpResponse(String code, String txId, Hashtable<String, String> headers) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(4000);
		buffer.write(MsrpConstants.MSRP_PROTOCOL .getBytes());
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write(txId.getBytes());
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write(code.getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
		
		buffer.write(MsrpConstants.HEADER_TO_PATH.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write((headers.get(MsrpConstants.HEADER_FROM_PATH)).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		buffer.write(MsrpConstants.HEADER_FROM_PATH.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write((headers.get(MsrpConstants.HEADER_TO_PATH)).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
			
		// Byte-range header may not be present
		if (headers.get(MsrpConstants.HEADER_BYTE_RANGE) != null) {
			buffer.write(MsrpConstants.HEADER_BYTE_RANGE.getBytes());
			buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
			buffer.write(MsrpConstants.CHAR_SP);
			buffer.write((headers.get(MsrpConstants.HEADER_BYTE_RANGE)).getBytes());
			buffer.write(MsrpConstants.NEW_LINE.getBytes());
		}
		
		buffer.write(MsrpConstants.END_MSRP_MSG.getBytes());
		buffer.write(txId.getBytes());
		buffer.write(MsrpConstants.FLAG_LAST_CHUNK);
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
		
		connection.sendChunk(buffer.toByteArray());
		buffer.close();
	}

	/**
	 * Send MSRP REPORT request
	 * 
	 * @param txId Transaction ID
	 * @param headers MSRP headers
	 * @throws MsrpException
	 * @throws IOException
	 */
	private void sendMsrpReportRequest(String txId, Hashtable<String, String> headers,
			long lastByte, long totalSize) throws MsrpException, IOException {
		// Create request
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(4000);
		buffer.reset();
		buffer.write(MsrpConstants.MSRP_PROTOCOL.getBytes());
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write(txId.getBytes());
		buffer.write((" " + MsrpConstants.METHOD_REPORT).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		buffer.write(MsrpConstants.HEADER_TO_PATH.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write((headers.get(MsrpConstants.HEADER_FROM_PATH)).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		buffer.write(MsrpConstants.HEADER_FROM_PATH.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write((headers.get(MsrpConstants.HEADER_TO_PATH)).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
		
		buffer.write(MsrpConstants.HEADER_MESSAGE_ID.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		buffer.write((headers.get(MsrpConstants.HEADER_MESSAGE_ID)).getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		buffer.write(MsrpConstants.HEADER_BYTE_RANGE.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		String byteRange = "1-" + lastByte + "/" + totalSize;
		buffer.write(byteRange.getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());
		
		buffer.write(MsrpConstants.HEADER_STATUS.getBytes());
		buffer.write(MsrpConstants.CHAR_DOUBLE_POINT);
		buffer.write(MsrpConstants.CHAR_SP);
		String status = "000 200 OK";
		buffer.write(status.getBytes());
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		buffer.write(MsrpConstants.END_MSRP_MSG.getBytes());
		buffer.write(txId.getBytes());
		buffer.write(MsrpConstants.FLAG_LAST_CHUNK);
		buffer.write(MsrpConstants.NEW_LINE.getBytes());

		// Send request
		requestTransaction = new RequestTransaction();
		connection.sendChunk(buffer.toByteArray());
		buffer.close();
	}
	
	/**
	 * Receive MSRP SEND request
	 * 
	 * @param txId Transaction ID
	 * @param headers Request headers
	 * @param flag Continuation flag
	 * @param data Received data
	 * @param totalSize Total size of the content
	 * @throws IOException
	 */
	public void receiveMsrpSend(String txId, Hashtable<String, String> headers, int flag, byte[] data, long totalSize) throws IOException {
		// Receive a SEND request
		if (logger.isActivated()) {
			logger.debug("SEND request received (flag=" + flag + ", transaction=" + txId + ")");
		}

		// Read message-ID
		String msgId = headers.get(MsrpConstants.HEADER_MESSAGE_ID);
		
		// Test if a failure report is needed
		boolean failureReportNeeded = true;
		String failureHeader = headers.get(MsrpConstants.HEADER_FAILURE_REPORT);
		if ((failureHeader != null) && failureHeader.equalsIgnoreCase("no")) {
			failureReportNeeded = false;
		}
		
		// Send MSRP response if requested
		if (failureReportNeeded) {
			sendMsrpResponse(MsrpConstants.RESPONSE_OK + " " + MsrpConstants.COMMENT_OK, txId, headers);
		}
		
		// Test if it's an empty chunk
		if (data == null) { 
			if (logger.isActivated()) {
				logger.debug("Empty chunk");
			}
			return;
		}
		
		// Save received data chunk if there is some
		receivedChunks.addChunk(data);
		
		// Check the continuation flag
		if (flag == MsrpConstants.FLAG_LAST_CHUNK) {
			// Transfer terminated
			if (logger.isActivated()) {
				logger.info("Transfer terminated");
			}

			// Read the received content
			byte[] dataContent = receivedChunks.getReceivedData();
			receivedChunks.resetCache();

			// Notify event listener
			String contentTypeHeader = headers.get(MsrpConstants.HEADER_CONTENT_TYPE);
			msrpEventListener.msrpDataReceived(msgId, dataContent, contentTypeHeader);

			// Test if a success report is needed
			boolean successReportNeeded = false;
			String reportHeader = headers.get(MsrpConstants.HEADER_SUCCESS_REPORT);
			if ((reportHeader != null) && reportHeader.equalsIgnoreCase("yes")) {
				successReportNeeded = true;
			}
			
			// Send MSRP report if requested
			if (successReportNeeded) {
				try {
					sendMsrpReportRequest(txId, headers, dataContent.length, totalSize);
				} catch(MsrpException e) {
					// Report failed
					if (logger.isActivated()) {
						logger.error("Can't send report", e);
					}
					
					// Notify event listener
					msrpEventListener.msrpTransferError("report timeout");			
				}
			}
		} else
		if (flag == MsrpConstants.FLAG_ABORT_CHUNK) {
			// Transfer aborted
			if (logger.isActivated()) {
				logger.info("Transfer aborted");
			}

			// Notify event listener
			msrpEventListener.msrpTransferAborted();			
		} else
		if (flag == MsrpConstants.FLAG_MORE_CHUNK) {
			// Transfer in progress
			if (logger.isActivated()) {
				logger.debug("Transfer in progress...");
			}

			// Notify event listener
			msrpEventListener.msrpTransferProgress(receivedChunks.getCurrentSize(), totalSize);
		}
	}

	/**
	 * Receive MSRP response
	 * 
	 * @param code Response code
	 * @param txId Transaction ID
	 * @param headers MSRP headers
	 */
	public void receiveMsrpResponse(int code, String txId, Hashtable<String, String> headers) {
		if (logger.isActivated()) {
			logger.info("Response received (code=" + code + ", transaction=" + txId + ")");
		}
		
		// Notify request transaction
		if (requestTransaction != null) {
			requestTransaction.notifyResponse(code, headers);
		}
		
        // Notify event listener
		if (code != 200) {
			msrpEventListener.msrpTransferError(code + " response received");
		}
	}
	
	/**
	 * Receive MSRP REPORT request
	 * 
	 * @param txId Transaction ID
	 * @param headers MSRP headers
	 * @throws IOException
	 */
	public void receiveMsrpReport(String txId, Hashtable<String, String> headers) throws IOException {
		if (logger.isActivated()) {
			logger.info("REPORT request received (transaction=" + txId + ")");
		}
		
		// Notify report transaction
		if (reportTransaction != null) {
			reportTransaction.notifyReport(headers);
		}
	}
}
