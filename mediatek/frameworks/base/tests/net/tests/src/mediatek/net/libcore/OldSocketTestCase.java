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

import junit.framework.TestCase;

public abstract class OldSocketTestCase extends TestCase {

    public static final int SO_MULTICAST = 0;

    public static final int SO_MULTICAST_INTERFACE = 1;

    public static final int SO_LINGER = 2;

    public static final int SO_RCVBUF = 3;

    public static final int SO_TIMEOUT = 4;

    public static final int SO_SNDBUF = 5;

    public static final int TCP_NODELAY = 6;

    public static final int SO_KEEPALIVE = 7;

    public static final int SO_REUSEADDR = 8;

    public static final int SO_OOBINLINE = 9;

    public static final int IP_TOS = 10;

    public static final int SO_BROADCAST = 11;

    public static final int SO_USELOOPBACK = 12;

    private static final String osDoesNotSupportOperationString = "The socket does not support the operation";

    private static final String osDoesNotSupportOptionString = "The socket option is not supported";

    private static final String osDoesNotSupportOptionArgumentString = "The socket option arguments are invalid";

    /**
     * Answer whether the OS supports the given socket option.
     */
    public boolean getOptionIsSupported(int option) {
        switch (option) {
        case SO_RCVBUF:
        case SO_SNDBUF:
            return true;
        case SO_MULTICAST:
        case SO_MULTICAST_INTERFACE:
        case SO_LINGER:
            return true;
        case TCP_NODELAY:
        case SO_TIMEOUT:
            return true;
        case SO_KEEPALIVE:
        case SO_REUSEADDR:
            return true;
        case SO_OOBINLINE:
            return true;
        case IP_TOS:
            return true;
        case SO_BROADCAST:
            return true;
        case SO_USELOOPBACK:
            return true;
        }
        return false;
    }

    /**
     * If the exception is "socket does not support the operation" exception and
     * it is expected on the current platform, do nothing. Otherwise, fail the
     * test.
     */
    public void handleException(Exception e, int option) {
        if (!getOptionIsSupported(option)) {
            String message = e.getMessage();
            if (message != null
                    && (message.equals(osDoesNotSupportOperationString)
                            || message.equals(osDoesNotSupportOptionString) || message
                            .equals(osDoesNotSupportOptionArgumentString))) {
                /*
                 * This exception is the correct behavior for platforms which do
                 * not support the option
                 */
            } else {
                fail("Threw \""
                        + e
                        + "\" instead of correct exception for unsupported socket option: "
                        + getSocketOptionString(option));
            }
        } else {
            fail("Exception during test : " + e.getMessage());
        }
    }

    /**
     * This method should be called at the end of a socket option test's code
     * but before the exception catch statements. It throws a failure if the
     * option given is not supported on the current platform but the VM failed
     * to throw an exception. So, on platforms which do not support the option,
     * the execution should never get to this method.
     */
    public void ensureExceptionThrownIfOptionIsUnsupportedOnOS(int option) {
        if (!getOptionIsSupported(option)) {
            fail("Failed to throw exception for unsupported socket option: "
                    + getSocketOptionString(option));
        }
    }

    /**
     * Answer a string for the socket option given.
     */
    private String getSocketOptionString(int option) {
        switch (option) {
        case SO_MULTICAST:
            return "Multicast";
        case SO_LINGER:
            return "Linger";
        case SO_RCVBUF:
            return "Receive buffer size";
        case SO_TIMEOUT:
            return "Socket timeout";
        case SO_SNDBUF:
            return "Send buffer size";
        case TCP_NODELAY:
            return "TCP no delay";
        case SO_KEEPALIVE:
            return "Keepalive";
        case SO_REUSEADDR:
            return "Reuse address";
        case SO_OOBINLINE:
            return "out of band data inline";
        case IP_TOS:
            return "Traffic class";
        case SO_BROADCAST:
            return "broadcast";
        case SO_USELOOPBACK:
            return "loopback";
        }
        return "Unknown socket option";
    }

}
