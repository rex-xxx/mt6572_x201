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

package com.orangelabs.rcs.platform.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.orangelabs.rcs.utils.logger.Logger;


/**
 * Android datagram server connection
 * 
 * @author jexa7410
 */
public class AndroidDatagramConnection implements DatagramConnection {
	/**
	 * Datagram connection
	 */
	private DatagramSocket connection = null; 
	
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
	/**
	 * Constructor
	 */
	public AndroidDatagramConnection() {
	}

	/**
	 * Open the datagram connection
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException {
        /**
         * M: Modified to resolve the issue that both the receiver and the
         * sender in a video sharing receive "terminated by the other side" when
         * the receiver accept a video sharing. @{
         */
        logger.debug("open() ");
        connection = new DatagramSocket();
        connection.setReuseAddress(true);
        /**
         * @}
         */
	}

	/**
	 * Open the datagram connection
	 * 
	 * @param port Local port
	 * @throws IOException
	 */
	public void open(int port) throws IOException {
	    /**
         * M: Modified to resolve the issue that both the receiver and the
         * sender in a video sharing receive "terminated by the other side" when
         * the receiver accept a video sharing. @{
         */
        logger.debug("open(), port = " + port);
        connection = new DatagramSocket(null);
        connection.setReuseAddress(true);
        InetSocketAddress localAddress = new InetSocketAddress(port);
        int localPort = localAddress.getPort();
        Logger.getLogger("AndroidDatagramConnection").debug("getPort() " + localPort);
        InetAddress address = localAddress.getAddress();
        if (address != null) {
            Logger.getLogger("AndroidDatagramConnection").debug(
                    "address " + address.getAddress());
        }
        Logger.getLogger("AndroidDatagramConnection").debug("address = " + address);
        connection.bind(localAddress);
        /**
         * @}
         */
	}

	/**
	 * Close the datagram connection
	 * 
	 * @throws IOException
	 */
    public void close() throws IOException {
        logger.debug("close socket");
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
	
	/**
	 * Receive data with a specific buffer size
	 * 
	 * @param bufferSize Buffer size 
	 * @return Byte array
	 * @throws IOException
	 */
	public byte[] receive(int bufferSize) throws IOException {
	    /**
         * M: Modified to resolve the native exception @{
         */
        logger.debug("receive(), bufferSize = " + bufferSize);
        if (connection != null && !connection.isClosed()) {
            byte[] buf = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            connection.receive(packet);

            int packetLength = packet.getLength();
            byte[] bytes = packet.getData();
            byte[] data = new byte[packetLength];
            System.arraycopy(bytes, 0, data, 0, packetLength);
            return data;
        } else {
            throw new IOException("Connection not openned");
        }
        /**
         * @}
         */
		
	}
	
	/**
	 * Receive data
	 * 
	 * @return Byte array
	 * @throws IOException
	 */
	public byte[] receive() throws IOException {
		return receive(DatagramConnection.DEFAULT_DATAGRAM_SIZE);
	}
	
	/**
	 * Send data
	 * 
	 * @param remoteAddr Remote address
	 * @param remotePort Remote port
	 * @param data Data as byte array
	 * @throws IOException
	 */
	public void send(String remoteAddr, int remotePort, byte[] data) throws IOException {
	    logger.debug("send(), remoteAddr = " + remoteAddr);
		if (data == null) {
			return;
		}
		/**
         * M: Modified to resolve the native exception @{
         */
		 if (connection != null && !connection.isClosed()) {
             InetAddress address = InetAddress.getByName(remoteAddr);
             DatagramPacket packet = new DatagramPacket(data, data.length, address, remotePort);
             connection.send(packet);
         } else {
             throw new IOException("Connection not openned");
         }
        /**
         * @}
         */
	}
	
	/**
	 * Returns the local address
	 * 
	 * @return Address
	 * @throws IOException
	 */
	public String getLocalAddress() throws IOException {
		if (connection != null) {
			return connection.getLocalAddress().getHostAddress();
		} else {
			throw new IOException("Connection not openned");
		}
	}

	/**
	 * Returns the local port
	 * 
	 * @return Port
	 * @throws IOException
	 */
	public int getLocalPort() throws IOException {
		if (connection != null) {
			return connection.getLocalPort();
		} else {
			throw new IOException("Connection not openned");
		}
	}
}
