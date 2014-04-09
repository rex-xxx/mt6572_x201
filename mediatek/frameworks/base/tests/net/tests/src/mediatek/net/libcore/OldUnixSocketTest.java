/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package libcore.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import junit.framework.TestCase;

public class OldUnixSocketTest extends TestCase {

    public void test_getInputStream() throws IOException {
        // Simple read/write test over the IO streams
        final ServerSocket pingServer = new ServerSocket(0);
        Socket pingClient = new Socket();

        try {
            pingClient.connect(new InetSocketAddress(
                    InetAddress.getLocalHost(), pingServer.getLocalPort()));

            Socket worker = pingServer.accept();
            pingServer.close();

            // Write some data to the server to provoke it
            OutputStream clientOut = pingClient.getOutputStream();
            clientOut.write(new byte[256]);
            InputStream in = worker.getInputStream();
            in.read();

            OutputStream out = worker.getOutputStream();
            out.write(new byte[42]);
            worker.close();
            InputStream clientIn = pingClient.getInputStream();
            clientIn.read(new byte[42]);

            try {
                int i = clientIn.read();
                fail("Should throw SocketException; got i=" + i);
            } catch (SocketException e) {
                // expected
            }
            clientIn.close();

            try {
                clientIn.read();
                fail("Should throw SocketException");
            } catch (SocketException e) {
                // expected
            }
            try {
                clientIn.read(new byte[5]);
                fail("Should throw SocketException");
            } catch (SocketException e) {
                // expected
            }
        } finally {
            pingClient.close();
            pingServer.close();
        }
    }

    public void test_connectLjava_net_SocketAddressI() throws Exception {
        // Now validate that we get a interrupted exception if we try to connect
        // to an address on which nobody is accepting connections and the
        // timeout expired
        Socket theSocket = new Socket();
        try {
            theSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(),
                    1), 200);
            fail("No interrupted exception when connecting to address nobody listening on with short timeout 200");
        } catch (ConnectException e) {
            // Expected
        }
        theSocket.close();
    }

    public void test_getOutputStream() throws Exception {
        // Regression test for HARMONY-2934
        // Port 0 is not allowed to be used in connect() on some platforms,
        // get a free port here
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();

        Socket socket = new Socket("127.0.0.1", port, false);
        OutputStream o = socket.getOutputStream();
        try {
            o.write(1);
        } catch (SocketException e) {
            // expected
        } finally {
            socket.close();
        }
    }
}
