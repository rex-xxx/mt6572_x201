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

import java.io.IOException;
import java.io.InputStream;

import android.test.suitebuilder.annotation.SmallTest;

import com.android.emailcommon.Logging;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.AccountSyncAdapter;
import com.android.exchange.adapter.Serializer;

/**
 * M: Test the AccountSyncAdapter
 */
@SmallTest
public class AccountSyncAdapterTests extends SyncAdapterTestCase<AccountSyncAdapter> {

    public void testAccountSyncAdapter() throws IOException {
        //get the test service
        EasSyncService mService = getTestService();
        AccountSyncAdapter mAccountSyncAdapter = new AccountSyncAdapter(mService);
        boolean outCome;

        mAccountSyncAdapter.cleanup();
        mAccountSyncAdapter.wipe();
        mAccountSyncAdapter.sendSyncOptions(0.0, new Serializer(), 0);

        outCome = mAccountSyncAdapter.isSyncable();
        assertEquals(true, outCome);
        outCome = mAccountSyncAdapter.sendLocalChanges(new Serializer());
        assertEquals(false, outCome);
        outCome = mAccountSyncAdapter.parse(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        });
        assertEquals(false, outCome);
        assertNull(mAccountSyncAdapter.getCollectionName());
    }
}

