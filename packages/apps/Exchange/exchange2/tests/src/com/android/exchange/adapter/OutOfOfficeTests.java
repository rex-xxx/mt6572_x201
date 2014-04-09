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


import java.io.IOException;

import android.accounts.Account;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.OofParams;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.provider.EmailContentSetupUtils;
import com.mediatek.exchange.outofoffice.OutOfOffice;

public class OutOfOfficeTests extends SyncAdapterTestCase<EmailSyncAdapter>{

    public void testSyncOof() throws IOException {
        mAccount = EmailContentSetupUtils.setupEasAccount("account", true, mProviderContext);
        EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, true,
                mProviderContext, Mailbox.TYPE_EAS_ACCOUNT_MAILBOX);
        Configuration.openTest();
        EasResponse.sCallback = new MockEasResponse();
        OofParams op = new OofParams(1, 0, 0L, 0L, 1, "hello");
        try {
            OofParams result = OutOfOffice.syncOof(mProviderContext, mAccount.mId, op, false);
            assertEquals(1, result.getmStatus());
            op = new OofParams(1, 1, 1351584000000L,
                    1354262400000L, 1, "hello");
            result = OutOfOffice.syncOof(mProviderContext, mAccount.mId, op, false);
            assertEquals(1, result.getmStatus());
            result = OutOfOffice.syncOof(mProviderContext, mAccount.mId, null, true);
            assertEquals(1, result.getmStatus());
            assertEquals(2, result.getOofState());
            assertEquals(1, result.getIsExternal());
            assertEquals(1351584000000L, result.getStartTimeInMillis());
            assertEquals(1354262400000L, result.getEndTimeInMillis());
            assertEquals("hello", result.getReplyMessage());
            op = new OofParams(1, 1, 1354233600000L,
                    1356825600000L, 0, "hello");
            result = OutOfOffice.syncOof(mProviderContext, mAccount.mId, op, false);
            assertEquals(1, result.getmStatus());
            result = OutOfOffice.syncOof(mProviderContext, mAccount.mId, null, true);
            assertEquals(0, result.getIsExternal());
            } finally {
                Configuration.shutDownTest();
                EasResponse.sCallback = null;
            }
    }
}
