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

package mediatek.net.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import android.net.TrafficStats;
import android.os.Process;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@TestTargetClass(TrafficStats.class)
public class TrafficStatsTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(level = TestLevel.SUFFICIENT, method = "getMobileTxPackets"),
        @TestTargetNew(level = TestLevel.SUFFICIENT, method = "getMobileRxPackets"),
        @TestTargetNew(level = TestLevel.SUFFICIENT, method = "getMobileTxBytes"),
        @TestTargetNew(level = TestLevel.SUFFICIENT, method = "getMobileRxBytes")
    })
    public void testGetMobileStats() {
        // We can't assume a mobile network is even present in this test, so
        // we simply assert that a valid value is returned.

        assertTrue(TrafficStats.getMobileTxPackets() == TrafficStats.UNSUPPORTED ||
                   TrafficStats.getMobileTxPackets() >= 0);
        assertTrue(TrafficStats.getMobileRxPackets() == TrafficStats.UNSUPPORTED ||
                   TrafficStats.getMobileRxPackets() >= 0);
        assertTrue(TrafficStats.getMobileTxBytes() == TrafficStats.UNSUPPORTED ||
                   TrafficStats.getMobileTxBytes() >= 0);
        assertTrue(TrafficStats.getMobileRxBytes() == TrafficStats.UNSUPPORTED ||
                   TrafficStats.getMobileRxBytes() >= 0);
    }

    @TestTargets({
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getMobileTxPackets"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getMobileRxPackets"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getMobileTxBytes"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getMobileRxBytes"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getTotalTxPackets"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getTotalRxPackets"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getTotalTxBytes"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getTotalRxBytes"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getUidTxBytes"),
        @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "getUidRxBytes")
    })
    public void testTrafficStatsForLocalhost() throws IOException {
        long mobileTxPacketsBefore = TrafficStats.getTotalTxPackets();
        long mobileRxPacketsBefore = TrafficStats.getTotalRxPackets();
        long mobileTxBytesBefore = TrafficStats.getTotalTxBytes();
        long mobileRxBytesBefore = TrafficStats.getTotalRxBytes();
        long totalTxPacketsBefore = TrafficStats.getTotalTxPackets();
        long totalRxPacketsBefore = TrafficStats.getTotalRxPackets();
        long totalTxBytesBefore = TrafficStats.getTotalTxBytes();
        long totalRxBytesBefore = TrafficStats.getTotalRxBytes();
        long uidTxBytesBefore = TrafficStats.getUidTxBytes(Process.myUid());
        long uidRxBytesBefore = TrafficStats.getUidRxBytes(Process.myUid());

        // Transfer 1MB of data across an explicitly localhost socket.

        final ServerSocket server = new ServerSocket(0);
        new Thread("TrafficStatsTest.testTrafficStatsForLocalhost") {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("localhost", server.getLocalPort());
                    OutputStream out = socket.getOutputStream();
                    byte[] buf = new byte[1024];
                    for (int i = 0; i < 1024; i++) out.write(buf);
                    out.close();
                    socket.close();
                } catch (IOException e) {
                }
            }
        }.start();

        try {
            Socket socket = server.accept();
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[1024];
            int read = 0;
            while (read < 1048576) {
                int n = in.read(buf);
                assertTrue("Unexpected EOF", n > 0);
                read += n;
            }
        } finally {
            server.close();
        }

        // It's too fast to call getUidTxBytes function.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        long mobileTxPacketsAfter = TrafficStats.getTotalTxPackets();
        long mobileRxPacketsAfter = TrafficStats.getTotalRxPackets();
        long mobileTxBytesAfter = TrafficStats.getTotalTxBytes();
        long mobileRxBytesAfter = TrafficStats.getTotalRxBytes();
        long totalTxPacketsAfter = TrafficStats.getTotalTxPackets();
        long totalRxPacketsAfter = TrafficStats.getTotalRxPackets();
        long totalTxBytesAfter = TrafficStats.getTotalTxBytes();
        long totalRxBytesAfter = TrafficStats.getTotalRxBytes();
        long uidTxBytesAfter = TrafficStats.getUidTxBytes(Process.myUid());
        long uidRxBytesAfter = TrafficStats.getUidRxBytes(Process.myUid());

        // Localhost traffic should *not* count against mobile or total stats.
        // There might be some other traffic, but nowhere near 1MB.

        assertTrue("mtxp: " + mobileTxPacketsBefore + " -> " + mobileTxPacketsAfter,
               mobileTxPacketsAfter >= mobileTxPacketsBefore &&
               mobileTxPacketsAfter <= mobileTxPacketsBefore + 500);
        assertTrue("mrxp: " + mobileRxPacketsBefore + " -> " + mobileRxPacketsAfter,
               mobileRxPacketsAfter >= mobileRxPacketsBefore &&
               mobileRxPacketsAfter <= mobileRxPacketsBefore + 500);
        assertTrue("mtxb: " + mobileTxBytesBefore + " -> " + mobileTxBytesAfter,
               mobileTxBytesAfter >= mobileTxBytesBefore &&
               mobileTxBytesAfter <= mobileTxBytesBefore + 200000);
        assertTrue("mrxb: " + mobileRxBytesBefore + " -> " + mobileRxBytesAfter,
               mobileRxBytesAfter >= mobileRxBytesBefore &&
               mobileRxBytesAfter <= mobileRxBytesBefore + 200000);

        assertTrue("ttxp: " + totalTxPacketsBefore + " -> " + totalTxPacketsAfter,
               totalTxPacketsAfter >= totalTxPacketsBefore &&
               totalTxPacketsAfter <= totalTxPacketsBefore + 500);
        assertTrue("trxp: " + totalRxPacketsBefore + " -> " + totalRxPacketsAfter,
               totalRxPacketsAfter >= totalRxPacketsBefore &&
               totalRxPacketsAfter <= totalRxPacketsBefore + 500);
        assertTrue("ttxb: " + totalTxBytesBefore + " -> " + totalTxBytesAfter,
               totalTxBytesAfter >= totalTxBytesBefore &&
               totalTxBytesAfter <= totalTxBytesBefore + 200000);
        assertTrue("trxb: " + totalRxBytesBefore + " -> " + totalRxBytesAfter,
               totalRxBytesAfter >= totalRxBytesBefore &&
               totalRxBytesAfter <= totalRxBytesBefore + 200000);

        // Localhost traffic *does* count against per-UID stats.
        assertTrue("uidtxb: " + uidTxBytesBefore + " -> " + uidTxBytesAfter,
               uidTxBytesAfter >= uidTxBytesBefore + 1048576);
        assertTrue("uidrxb: " + uidRxBytesBefore + " -> " + uidRxBytesAfter,
               uidRxBytesAfter >= uidRxBytesBefore + 1048576);
    }
}
