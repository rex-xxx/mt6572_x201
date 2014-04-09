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
import com.android.exchange.adapter.SettingsParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

/**
 * M: Test the PingParser
 */
public class SettingsParserTests extends SyncAdapterTestCase<EmailSyncAdapter> {

    public void testSettingsParser() throws IOException {
        EasSyncService service = getTestService();

        Serializer s1 = new Serializer();
        s1.start(Tags.SETTINGS_SETTINGS).data(Tags.SETTINGS_STATUS, "1")
        .start(Tags.SETTINGS_DEVICE_INFORMATION).start(Tags.SETTINGS_SET)
        .data(Tags.SETTINGS_STATUS, "1").tag(Tags.SETTINGS_MODEL)
        .end().tag(Tags.SETTINGS_MODEL).end().tag(Tags.SETTINGS_MODEL).end().done();

        byte[] bytes = s1.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        SettingsParser settingsParser = new SettingsParser(bis, service);

        boolean result = settingsParser.parse();

        assertTrue(result);

        Serializer s2 = new Serializer();
        s2.start(Tags.SETTINGS_SETTINGS).data(Tags.SETTINGS_STATUS, "2")
        .end().done();

        bytes = s2.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        settingsParser = new SettingsParser(bais, service);

        result = settingsParser.parse();

        assertFalse(result);
    }
}
