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

import android.util.Log;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.SearchParams;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.Search;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.provider.EmailContentSetupUtils;

/**
 * M: Test the Search and SearchParser
 */
public class SearchTests extends SyncAdapterTestCase<EmailSyncAdapter> {

    public void testSearchMessages() throws IOException {
        // Create account and mailbox
        mAccount = EmailContentSetupUtils.setupEasAccount("account", true, mProviderContext);
        mMailbox = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_INBOX);
        Configuration.openTest();
        EasResponse.sCallback = new MockEasResponse();

        SearchParams searchParams = new SearchParams(mMailbox.mId, "Mediatek");
        int result;
        try {
            result = Search.searchMessages(mContext, mAccount.mId, searchParams, mMailbox.mId);
        } finally {
            Configuration.shutDownTest();
            EasResponse.sCallback = null;
        }
        assertEquals(10, result);
    }

    public void testSearchParser() throws IOException {
        // Get the test service
        EasSyncService service = getTestService();

        // Set up an input stream
        Serializer s = new Serializer();
        s.start(Tags.SEARCH_SEARCH).data(Tags.SEARCH_STATUS, "1").tag(Tags.SEARCH)
        .start(Tags.SEARCH_RESPONSE).start(Tags.SEARCH_STORE).tag(Tags.SEARCH)
        .data(Tags.SEARCH_STATUS, "1").data(Tags.SEARCH_TOTAL, "10").start(Tags.SEARCH_RESULT)
        .tag(Tags.SEARCH).data(Tags.SYNC_CLASS, "SearchTests").data(Tags.SYNC_COLLECTION_ID, "1")
        .data(Tags.SEARCH_LONG_ID, "12.0").tag(Tags.SEARCH_PROPERTIES)
        .end().end().end().end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Search search = new Search();
        Search.TestSearchParser tParser = search.new TestSearchParser(bis, service, null);

        boolean res = tParser.parse();
        int result = tParser.getResult();
        assertFalse(res);
        assertEquals(10, result);
    }
}
