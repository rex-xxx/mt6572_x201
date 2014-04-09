/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.GalParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

/**
 * M: Test the GalParser
 */
public class GalParserTests extends SyncAdapterTestCase<EmailSyncAdapter> {

    public void testGalParser() throws IOException {
        EasSyncService service = getTestService();

        Serializer s = new Serializer();
        s.start(Tags.SEARCH_SEARCH).start(Tags.SEARCH_RESPONSE).start(Tags.SEARCH_STORE)
        .data(Tags.SEARCH_TOTAL,"50").tag(Tags.SEARCH_RANGE).start(Tags.SEARCH_RESULT)
        .start(Tags.SEARCH_PROPERTIES).data(Tags.GAL_DISPLAY_NAME, "james")
        .data(Tags.GAL_EMAIL_ADDRESS, "123@163.com")
        .data(Tags.GAL_PHONE, "13812345678").data(Tags.GAL_OFFICE, "2-1")
        .data(Tags.GAL_TITLE, "Engineer").data(Tags.GAL_COMPANY, "Google")
        .data(Tags.GAL_ALIAS, "pig").data(Tags.GAL_FIRST_NAME, "Feng")
        .data(Tags.GAL_LAST_NAME, "Gang").data(Tags.GAL_HOME_PHONE, "028-88888888")
        .data(Tags.GAL_MOBILE_PHONE, "13888888888").tag(Tags.GAL)
        .end().tag(Tags.SEARCH_AND).end().tag(Tags.SEARCH_AND).end()
        .tag(Tags.SEARCH_AND).end().tag(Tags.SEARCH_AND).end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        GalParser galParser = new GalParser(bis, service);
        boolean b = galParser.parse();

        assertTrue(b);
        assertEquals(50, galParser.getGalResult().total);

        String expectString = "2-1" + '\2' + "office" + '\1' + "Gang" + '\2' + "lastName"
                + '\1' + "Engineer" + '\2' + "title" + '\1' + "13888888888" + '\2' + "mobilePhone"
                + '\1' + "pig" + '\2' + "alias" + '\1' + "Google" + '\2' + "company"
                + '\1' + "123@163.com" + '\2' + "emailAddress" + '\1' + "Feng" + '\2' + "firstName"
                + '\1' + "13812345678" + '\2' + "workPhone" + '\1' + "james" + '\2' + "displayName"
                + '\1' + "028-88888888" + '\2' + "homePhone";
        String result = galParser.getGalResult().galData.get(0).toPackedString();
        // Remove all the test data
        galParser.getGalResult().galData.clear();

        assertEquals(expectString, result);

    }
}
