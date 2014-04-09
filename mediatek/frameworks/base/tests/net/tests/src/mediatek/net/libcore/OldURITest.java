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

import java.net.URI;
import java.net.URISyntaxException;
import junit.framework.TestCase;

public class OldURITest extends TestCase {

    String[] constructorTests = new String[] {
            "http://user@www.google.com:45/search?q=helpinfo#somefragment",
            // http with authority, query and fragment
            "ftp://ftp.is.co.za/rfc/rfc1808.txt", // ftp
            "gopher://spinaltap.micro.umn.edu/00/Weather/California/Los%20Angeles", // gopher
            "mailto:mduerst@ifi.unizh.ch", // mailto
            "news:comp.infosystems.www.servers.unix", // news
            "telnet://melvyl.ucop.edu/", // telnet
            "http://123.24.17.98/test", // IPv4 authority
            "http://www.google.com:80/test",// domain name authority
            "http://joe@[3ffe:2a00:100:7031::1]:80/test",
            // IPv6 authority, with userinfo and port
            "/relative", // relative starting with /
            "//relative", // relative starting with //
            "relative", // relative with no /
            "#fragment",// relative just with fragment
            "http://user@host:80", // UI, host,port
            "http://user@host", // ui, host
            "http://host", // host
            "http://host:80", // host,port
            "http://joe@:80", // ui, port (becomes registry-based)
            "file:///foo/bar", // empty authority, non empty path
            "ht?tp://hoe@host:80", // miscellaneous tests
            "mai/lto:hey?joe#man", "http://host/a%20path#frag",
            // path with an escaped octet for space char
            "http://host/a%E2%82%ACpath#frag",
            // path with escaped octet for unicode char, not USASCII
            "http://host/a\u20ACpath#frag",
            // path with unicode char, not USASCII equivalent to
            // = "http://host/a\u0080path#frag",
            "http://host%20name/", // escaped octets in host (becomes
            // registry based)
            "http://host\u00DFname/", // unicodechar in host (becomes
            // registry based)
            // equivalent to = "http://host\u00dfname/",
            "ht123-+tp://www.google.com:80/test", // legal chars in scheme
    };

    String[] constructorTestsInvalid = new String[] {
            "http:///a path#frag", // space char in path, not in escaped
            // octet form, with no host
            "http://host/a[path#frag", // an illegal char, not in escaped
            // octet form, should throw an
            // exception
            "http://host/a%path#frag", // invalid escape sequence in path
            "http://host/a%#frag", // incomplete escape sequence in path

            "http://host#a frag", // space char in fragment, not in
            // escaped octet form, no path
            "http://host/a#fr#ag", // illegal char in fragment
            "http:///path#fr%ag", // invalid escape sequence in fragment,
            // with no host
            "http://host/path#frag%", // incomplete escape sequence in
            // fragment

            "http://host/path?a query#frag", // space char in query, not
            // in escaped octet form
            "http://host?query%ag", // invalid escape sequence in query, no
            // path
            "http:///path?query%", // incomplete escape sequence in query,
            // with no host

            "mailto:user^name@fklkf.com" // invalid char in scheme specific part
    };

    public void test_createLjava_lang_String() {
        for (String s : constructorTests) {
            try {
                new URI(s);
            } catch (URISyntaxException e) {
                fail("Failed to construct URI for: " + s + " : " + e);
            }
        }

        for (String s : constructorTestsInvalid) {
            try {
                URI.create(s);
                fail("IllegalArgumentException expected but not received.");
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    public void test_relativizeLjava_net_URI() throws URISyntaxException {
        try {
            URI b = new URI("http://www.google.com/dir1/dir2");
            b.relativize(null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException expected) {
        }
    }

    public void test_resolveLjava_net_URI() throws URISyntaxException {
        try {
            URI b = new URI("http://www.test.com/dir");
            b.resolve((URI) null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException expected) {
        }
    }

    public void test_resolveLjava_lang_String() throws URISyntaxException {
        try {
            URI b = new URI("http://www.test.com/dir");
            b.resolve((String) null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException expected) {
        }

        try {
            URI b = new URI("http://www.test.com/dir");
            b.resolve("http://a/b/c/g?y/./x\n");
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException expected) {
        }
    }

    public void test_ConstructorLjava_lang_String() throws URISyntaxException {
        try {
            new URI(null);
            fail("NullPointerException was not thrown.");
        } catch(NullPointerException expected) {
        }
    }
}
