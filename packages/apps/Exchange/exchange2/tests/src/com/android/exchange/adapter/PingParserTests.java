/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.android.exchange.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.android.exchange.EasSyncService;
import com.android.exchange.IllegalHeartbeatException;
import com.android.exchange.StaleFolderListException;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.PingParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

/**
 * M: Test the PingParser
 */
public class PingParserTests extends SyncAdapterTestCase<EmailSyncAdapter> {
    private static final int STATUS = 2;
    private static final String SERVER_ID = "TEST_SERVER_ID";

    public void testPingParser() throws IOException, StaleFolderListException, IllegalHeartbeatException {
        EasSyncService service = getTestService();

        Serializer s = new Serializer();
        s.start(Tags.PING_PING).data(Tags.PING_STATUS, "2").tag(Tags.PING_PING)
        .start(Tags.PING_FOLDERS).data(Tags.PING_FOLDER, SERVER_ID)
        .tag(Tags.PING_PING).end().end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        PingParser pingParser = new PingParser(bis, service);

        boolean result = pingParser.parse();

        assertTrue(result);
        assertEquals(STATUS, pingParser.getSyncStatus());
        assertEquals(SERVER_ID, pingParser.getSyncList().get(0));
        // Clear all of the test data
        pingParser.getSyncList().clear();
    }
}
