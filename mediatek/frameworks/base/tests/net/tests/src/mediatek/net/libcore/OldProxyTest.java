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

/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package libcore.java.net;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import junit.framework.TestCase;

public class OldProxyTest extends TestCase {

    private SocketAddress address = new InetSocketAddress("127.0.0.1", 1234);

    public void test_address() {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, address);
        assertEquals(address, proxy.address());

        try {
            new Proxy(Proxy.Type.SOCKS, null);
            fail("IllegalArgumentException was thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }
    }

    public void test_hashCode() {
        SocketAddress address1 = new InetSocketAddress("127.0.0.1", 1234);

        Proxy proxy1 = new Proxy(Proxy.Type.HTTP, address1);
        Proxy proxy2 = new Proxy(Proxy.Type.HTTP, address1);
        assertTrue(proxy1.hashCode() == proxy2.hashCode());

        new Proxy(Proxy.Type.SOCKS, address1);
        Proxy proxy4 = new Proxy(Proxy.Type.SOCKS, address1);
        assertTrue(proxy1.hashCode() == proxy2.hashCode());

        assertTrue(proxy1.hashCode() != proxy4.hashCode());

        SocketAddress address2 = new InetSocketAddress("127.0.0.1", 1235);

        Proxy proxy5 = new Proxy(Proxy.Type.SOCKS, address1);
        Proxy proxy6 = new Proxy(Proxy.Type.SOCKS, address2);
        assertTrue(proxy5.hashCode() != proxy6.hashCode());
    }

    public void test_type() {

        Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
        assertEquals(Proxy.Type.HTTP, proxy.type());

        proxy = new Proxy(Proxy.Type.SOCKS, address);
        assertEquals(Proxy.Type.SOCKS, proxy.type());

        proxy = Proxy.NO_PROXY;
        assertEquals(Proxy.Type.DIRECT, proxy.type());
    }
}
