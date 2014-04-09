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
 * Copyright (C) 2010 The Android Open Source Project
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
 */

package libcore.javax.net.ssl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;

/**
 * TestSSLSocketPair is a convenience class for other tests that want
 * a pair of connected and handshaked client and server SSLSockets for
 * testing.
 */
public final class TestSSLSocketPair {
    public final TestSSLContext c;
    public final SSLSocket server;
    public final SSLSocket client;

    private TestSSLSocketPair (TestSSLContext c,
                               SSLSocket server,
                               SSLSocket client) {
        this.c = c;
        this.server = server;
        this.client = client;
    }

    public void close() {
        c.close();
        try {
            server.close();
            client.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * based on test_SSLSocket_startHandshake
     */
    public static TestSSLSocketPair create () {
        TestSSLContext c = TestSSLContext.create();
        SSLSocket[] sockets = connect(c, null, null);
        return new TestSSLSocketPair(c, sockets[0], sockets[1]);
    }

    /**
     * Create a new connected server/client socket pair within a
     * existing SSLContext. Optionally specify clientCipherSuites to
     * allow forcing new SSLSession to test SSLSessionContext
     * caching. Optionally specify serverCipherSuites for testing
     * cipher suite negotiation.
     */
    public static SSLSocket[] connect (final TestSSLContext context,
                                       final String[] clientCipherSuites,
                                       final String[] serverCipherSuites) {
        try {
            final SSLSocket client = (SSLSocket)
                context.clientContext.getSocketFactory().createSocket(context.host, context.port);
            final SSLSocket server = (SSLSocket) context.serverSocket.accept();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            Future s = executor.submit(new Callable<Void>() {
                    public Void call() throws Exception {
                        if (serverCipherSuites != null) {
                            server.setEnabledCipherSuites(serverCipherSuites);
                        }
                        server.startHandshake();
                        return null;
                    }
                });
            Future c = executor.submit(new Callable<Void>() {
                    public Void call() throws Exception {
                        if (clientCipherSuites != null) {
                            client.setEnabledCipherSuites(clientCipherSuites);
                        }
                        client.startHandshake();
                        return null;
                    }
                });
            executor.shutdown();

            // catch client and server exceptions separately so we can
            // potentially log both.
            Exception serverException;
            try {
                s.get(30, TimeUnit.SECONDS);
                serverException = null;
            } catch (Exception e) {
                serverException = e;
                e.printStackTrace();
            }
            Exception clientException;
            try {
                c.get(30, TimeUnit.SECONDS);
                clientException = null;
            } catch (Exception e) {
                clientException = e;
                e.printStackTrace();
            }
            if (serverException != null) {
                throw serverException;
            }
            if (clientException != null) {
                throw clientException;
            }
            return new SSLSocket[] { server, client };
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

