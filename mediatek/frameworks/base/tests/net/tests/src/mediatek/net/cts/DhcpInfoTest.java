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
 * Copyright (C) 2009 The Android Open Source Project
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

package mediatek.net.cts;

import android.net.DhcpInfo;
import android.test.AndroidTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(DhcpInfo.class)
public class DhcpInfoTest extends AndroidTestCase {

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test DhcpInfo's constructor.",
        method = "DhcpInfo",
        args = {}
    )
    public void testConstructor() {
        new DhcpInfo();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toString function.",
        method = "toString",
        args = {}
    )
    public void testToString() {
        String expectedDefault = "ipaddr 0.0.0.0 gateway 0.0.0.0 netmask 0.0.0.0 dns1 0.0.0.0 "
                + "dns2 0.0.0.0 DHCP server 0.0.0.0 lease 0 seconds";
        String STR_ADDR1 = "255.255.255.255";
        String STR_ADDR2 = "127.0.0.1";
        String STR_ADDR3 = "192.168.1.1";
        String STR_ADDR4 = "192.168.1.0";
        int leaseTime = 9999;
        String expected = "ipaddr " + STR_ADDR1 + " gateway " + STR_ADDR2 + " netmask "
                + STR_ADDR3 + " dns1 " + STR_ADDR4 + " dns2 " + STR_ADDR4 + " DHCP server "
                + STR_ADDR2 + " lease " + leaseTime + " seconds";

        DhcpInfo dhcpInfo = new DhcpInfo();

        // Test default string.
        assertEquals(expectedDefault, dhcpInfo.toString());

        dhcpInfo.ipAddress = ipToInteger(STR_ADDR1);
        dhcpInfo.gateway = ipToInteger(STR_ADDR2);
        dhcpInfo.netmask = ipToInteger(STR_ADDR3);
        dhcpInfo.dns1 = ipToInteger(STR_ADDR4);
        dhcpInfo.dns2 = ipToInteger(STR_ADDR4);
        dhcpInfo.serverAddress = ipToInteger(STR_ADDR2);
        dhcpInfo.leaseDuration = leaseTime;

        // Test with new values
        assertEquals(expected, dhcpInfo.toString());
    }

    private int ipToInteger(String ipString) {
        String ipSegs[] = ipString.split("[.]");
        int tmp = Integer.parseInt(ipSegs[3]) << 24 | Integer.parseInt(ipSegs[2]) << 16 |
            Integer.parseInt(ipSegs[1]) << 8 | Integer.parseInt(ipSegs[0]);
        return tmp;
    }
}
