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
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.MoveItemsParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

/**
 * M: Test the MoveItemsParser
 */
public class MoveItemsParserTests extends SyncAdapterTestCase<EmailSyncAdapter> {
    private static final String SERVER_ID = "10086";
    // These are the status values we return to callers
    private static final int STATUS_CODE_SUCCESS = 1;
    private static final int STATUS_CODE_REVERT = 2;
    private static final int STATUS_CODE_RETRY = 3;

    public void testMoveItemsParser() throws IOException {
        EasSyncService service = getTestService();
        String server_id;
        int status_code;
        boolean result;
        // Set up an input stream
        Serializer s1 = new Serializer();
        s1.start(Tags.MOVE_MOVE_ITEMS).tag(Tags.MOVE)
        .start(Tags.MOVE_RESPONSE).data(Tags.MOVE_STATUS, "3")
        .data(Tags.MOVE_DSTMSGID, "10086").tag(Tags.MOVE)
        .end().end().done();
        byte[] bytes = s1.toByteArray();
        ByteArrayInputStream bis1 = new ByteArrayInputStream(bytes);
        MoveItemsParser moveItemsParser = new MoveItemsParser(bis1, service);

        result = moveItemsParser.parse();
        assertFalse(result);

        server_id = moveItemsParser.getNewServerId();
        assertEquals(SERVER_ID, server_id);

        status_code = moveItemsParser.getStatusCode();
        assertEquals(STATUS_CODE_SUCCESS, status_code);

        Serializer s2 = new Serializer();
        s2.start(Tags.MOVE_MOVE_ITEMS).start(Tags.MOVE_RESPONSE).data(Tags.MOVE_STATUS, "7")
        .end().end().done();
        bytes = s2.toByteArray();
        ByteArrayInputStream bis2 = new ByteArrayInputStream(bytes);
        moveItemsParser = new MoveItemsParser(bis2, service);

        moveItemsParser.parse();
        status_code = moveItemsParser.getStatusCode();
        assertEquals(STATUS_CODE_RETRY, status_code);

        Serializer s3 = new Serializer();
        s3.start(Tags.MOVE_MOVE_ITEMS).start(Tags.MOVE_RESPONSE).data(Tags.MOVE_STATUS, "1")
        .end().end().done();
        bytes = s3.toByteArray();
        ByteArrayInputStream bis3 = new ByteArrayInputStream(bytes);
        moveItemsParser = new MoveItemsParser(bis3, service);

        moveItemsParser.parse();
        status_code = moveItemsParser.getStatusCode();
        assertEquals(STATUS_CODE_REVERT, status_code);
    }
}
