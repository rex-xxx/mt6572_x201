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
 * Copyright (C) 2011 Google Inc.
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

package com.mediatek.mockwebserver;

import java.net.Socket;
import java.util.List;
import javax.net.ssl.SSLSocket;

/**
 * An HTTP request that came into the mock web server.
 */
public final class RecordedRequest {
    private final String requestLine;
    private final String method;
    private final String path;
    private final List<String> headers;
    private final List<Integer> chunkSizes;
    private final int bodySize;
    private final byte[] body;
    private final int sequenceNumber;
    private final String sslProtocol;

    RecordedRequest(String requestLine, List<String> headers, List<Integer> chunkSizes,
            int bodySize, byte[] body, int sequenceNumber, Socket socket) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.chunkSizes = chunkSizes;
        this.bodySize = bodySize;
        this.body = body;
        this.sequenceNumber = sequenceNumber;

        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            sslProtocol = sslSocket.getSession().getProtocol();
        } else {
            sslProtocol = null;
        }

        if (requestLine != null) {
            int methodEnd = requestLine.indexOf(' ');
            int pathEnd = requestLine.indexOf(' ', methodEnd + 1);
            this.method = requestLine.substring(0, methodEnd);
            this.path = requestLine.substring(methodEnd + 1, pathEnd);
        } else {
            this.method = null;
            this.path = null;
        }
    }

    public String getRequestLine() {
        return requestLine;
    }

    public List<String> getHeaders() {
        return headers;
    }

    /**
     * Returns the sizes of the chunks of this request's body, or an empty list
     * if the request's body was empty or unchunked.
     */
    public List<Integer> getChunkSizes() {
        return chunkSizes;
    }

    /**
     * Returns the total size of the body of this POST request (before
     * truncation).
     */
    public int getBodySize() {
        return bodySize;
    }

    /**
     * Returns the body of this POST request. This may be truncated.
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Returns the index of this request on its HTTP connection. Since a single
     * HTTP connection may serve multiple requests, each request is assigned its
     * own sequence number.
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Returns the connection's SSL protocol like {@code TLSv1}, {@code SSLv3},
     * {@code NONE} or null if the connection doesn't use SSL.
     */
    public String getSslProtocol() {
        return sslProtocol;
    }

    @Override public String toString() {
        return requestLine;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
