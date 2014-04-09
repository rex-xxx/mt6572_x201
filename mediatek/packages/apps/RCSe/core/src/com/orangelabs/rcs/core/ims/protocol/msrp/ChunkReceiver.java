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

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chunks receiver
 * 
 * @author jexa7410
 */
public class ChunkReceiver extends Thread {
	/**
	 * MSRP connection
	 */
	private MsrpConnection connection;
	
	/**
	 * MSRP input stream
	 */
	private InputStream stream;
	
	/**
	 * Termination flag
	 */
	private boolean terminated = false;
	
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param connection MSRP connection
	 * @param stream TCP input stream
	 */
	public ChunkReceiver(MsrpConnection connection, InputStream stream) {
		this.connection = connection;
		this.stream = stream;
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
	 * Terminate the receiver
	 */
	public void terminate() {
		terminated = true;
		try {
			interrupt();
		} catch(Exception e) {}
		if (logger.isActivated()) {
			logger.debug("Receiver is terminated");
		}
	}
	
	/**
	 * Background processing
	 */
	public void run() {
		try {
			if (logger.isActivated()) {
				logger.debug("Receiver is started");
			}

			// Background processing
			while(!terminated) {
				StringBuffer trace = new StringBuffer();

				// Read first line of a new data chunk
				StringBuffer line = readLine();
				if (line.length() == 0) {
					if (logger.isActivated()) {
						logger.debug("End of stream");
					}					
					return;
				}
				
				if (MsrpConnection.MSRP_TRACE_ENABLED) {
					trace.append(line);
					trace.append(MsrpConstants.NEW_LINE);
				}

				if (logger.isActivated()) {
					logger.debug("Read a new chunk");
				}
				
				// Check the MSRP tag
				String[] firstLineTags = line.toString().split(" ");
				if ((firstLineTags.length < 3) || !firstLineTags[0].equals(MsrpConstants.MSRP_PROTOCOL)) {
					if (logger.isActivated()) {
						logger.debug("Not a MSRP message");
					}
					return;
				}

				// Get the transaction ID from the first line
				String txId = firstLineTags[1];
				if (logger.isActivated()) {
					logger.debug("Transaction-ID: " + txId);
				}	
				String end = MsrpConstants.END_MSRP_MSG + txId;

				// Get response code or method name from the first line
				int responseCode = -1;
				String method = null;
				try {
					responseCode = Integer.parseInt(firstLineTags[2]);
					if (logger.isActivated()) {
						logger.debug("Response: " + responseCode);
					}	
				} catch(NumberFormatException e) {
					method = firstLineTags[2];
					if (logger.isActivated()) {
						logger.debug("Method: " + method);
					}	
				}

				// Data chunk
				byte[] data = null;

				// Read next lines
				Hashtable<String, String> headers = new Hashtable<String, String>();
				char continuationFlag = '\0';
				long totalSize = 0;
				while(continuationFlag == '\0') {
					line = readLine();
					if (MsrpConnection.MSRP_TRACE_ENABLED) {
						trace.append(line);
						trace.append(MsrpConstants.NEW_LINE);
					}
					
					// Test if there is a new line separating headers from the data
					if (line.length() == 0) {
						// Read data
						String byteRange = headers.get(MsrpConstants.HEADER_BYTE_RANGE);
						int chunkSize = 0;
						if (byteRange != null) {
							chunkSize = MsrpUtils.getChunkSize(byteRange);
							totalSize = MsrpUtils.getTotalSize(byteRange);
						}
							
						if (logger.isActivated()) {
							logger.debug("Read data (" + chunkSize + ")");
						}	
						
						if (chunkSize > 0) {
							// Use Byte-Range value to read directly the block of data
							data = readChunkedData(chunkSize);
							
							if (MsrpConnection.MSRP_TRACE_ENABLED) {
								trace.append(new String(data));
								trace.append(MsrpConstants.NEW_LINE);
							}
						} else {
							// Read until terminating header is found
							StringBuffer buffer = new StringBuffer();
							StringBuffer dataline;
							boolean endchunk = false; 
							while((!endchunk) && (buffer.length() < MsrpConstants.CHUNK_MAX_SIZE)) {
								dataline = readLine();
								if ((dataline.length()-1 == end.length()) && (dataline.toString().startsWith(end))) {
									continuationFlag = dataline.charAt(dataline.length()-1);
									if (logger.isActivated()) {
										logger.debug("Continuous flag: " + continuationFlag);
									}
									endchunk = true;
								} else {
									if (buffer.length() > 0) {
										buffer.append(MsrpConstants.NEW_LINE);
									}
									buffer.append(dataline);
								}								
							}
							/**
							 * M: Modified to resolve the mess code. @{
							 */
                            data = buffer.toString().getBytes("ISO-8859-1");
                            /**
                             * @}
                             */
							totalSize = data.length;
							
							if (MsrpConnection.MSRP_TRACE_ENABLED) {
								trace.append(new String(data));
								trace.append(MsrpConstants.NEW_LINE);
								trace.append(end);
								trace.append(continuationFlag);
							}
						}
						if (logger.isActivated()) {
							logger.debug("Data: " + data.length);
						}						
					} else
					if (line.toString().startsWith(end)) {
						continuationFlag = line.charAt(line.length()-1);
						if (logger.isActivated()) {
							logger.debug("Continuous flag: " + continuationFlag);
						}						
					} else {
						// It's an header
						int index = line.indexOf(":");
						String headerName = line.substring(0, index).trim();
						String headerValue = line.substring(index+1).trim();
						
						// Add the header in the list
						headers.put(headerName, headerValue);
						if (logger.isActivated()) {
							logger.debug("Header: " + headerName);
						}						
					}
				}
				
				// Process the received MSRP message
				if (responseCode != -1) {
					// Process MSRP response
					if (MsrpConnection.MSRP_TRACE_ENABLED) {
						System.out.println("<<< Receive MSRP response:\n" +  trace);
					}
					connection.getSession().receiveMsrpResponse(responseCode, txId, headers);
				} else {
					// Process MSRP request
					if (method.toString().equals(MsrpConstants.METHOD_SEND)) {
						// Process a SEND request
						if (MsrpConnection.MSRP_TRACE_ENABLED) {
							System.out.println("<<< Receive MSRP SEND request:\n" +  trace);
						}
						connection.getSession().receiveMsrpSend(txId, headers, continuationFlag, data, totalSize);
					} else 
					if (method.toString().equals(MsrpConstants.METHOD_REPORT)) {
						// Process a REPORT request
						if (MsrpConnection.MSRP_TRACE_ENABLED) {
							System.out.println("<<< Receive MSRP REPORT request:\n" +  trace);
						}
						connection.getSession().receiveMsrpReport(txId, headers);					
					} else {
						// Unknown request
						if (logger.isActivated()) {
							logger.debug("Unknown request received: " + method);
						}
					}
				}
			}
		} catch(Exception e) {
			if (terminated) { 
				if (logger.isActivated()) {
					logger.debug("Chunk receiver thread terminated");
				}
			} else {
				if (logger.isActivated()) {
					logger.error("Chunk receiver has failed", e);
				}
				
				// Notify the session listener that an error has occured
				connection.getSession().getMsrpEventListener().msrpTransferError(e.getMessage());
			}
			terminated = true;
		}
	}

	/**
	 * Read line
	 * 
	 * @return String
	 * @throws IOException
	 */
	private StringBuffer readLine() throws IOException {
		StringBuffer line = new StringBuffer();
		int previous = -1;
		int current = -1;
        /** M: Catch read ssl socket exception @{ */
        try {
            while ((current = stream.read()) != -1) {
                line.append((char) current);
                if ((previous == MsrpConstants.CHAR_LF) && (current == MsrpConstants.CHAR_CR)) {
                    return line.delete(line.length() - 2, line.length());
                }
                previous = current;
            }
        } catch (IOException e) {
            if(logger.isActivated()){
                logger.error("Read stream failed:" + e.getMessage());
            }
        }
        /** @} */
		
		return line;			
	}
		
	/**
	 * Read chunked data
	 *
	 * @param chunkSize Chunk size
	 * @return Data
	 * @throws IOException
	 */
	private byte[] readChunkedData(int chunkSize) throws IOException {
		// Read data until chunk size is reached
		byte[] result = null;
		result = new byte[chunkSize];
		int nbRead = 0;
		int nbData = -1;
		while((nbRead < chunkSize) && ((nbData = stream.read(result, nbRead, chunkSize-nbRead)) != -1)) {
			nbRead += nbData;
		}		
		stream.read(); // Read LF
		stream.read(); // Read CR
		return result;
	}
}
