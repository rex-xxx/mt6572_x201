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

package com.mediatek.todos.tests;

import com.mediatek.todos.LogUtils;
import com.mediatek.todos.TimeChangeReceiver;
import com.mediatek.todos.TestUtils.ListenerForReceiver;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;


public class TimeChangeReceiverTest extends InstrumentationTestCase {
    private String TAG = "TimeChangeReceiverTest";

    private TimeChangeReceiver mReceiver = null;
    private ListenerForReceiver mListener = null;

    private Instrumentation mInst = null;
    private Context mTargetContext = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();
        mTargetContext = mInst.getTargetContext();
        mReceiver = TimeChangeReceiver.registTimeChangeReceiver(mTargetContext);

        mListener = new ListenerForReceiver();
        mReceiver.addDateChangeListener(mListener);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
    }

    @Override
    protected void tearDown() throws Exception {
        mTargetContext.unregisterReceiver(mReceiver);
        super.tearDown();
    }

    /**
     * send Intent Actions, it shall receive them successfully
     */
    public void testTimeChangeReceiver() {
        LogUtils.d(TAG, "testTimeChangeReceiver");
        Intent[] intents = new Intent[4];
        intents[0] = new Intent();
        intents[0].setAction(Intent.ACTION_TIME_CHANGED);
        intents[1] = new Intent();
        intents[1].setAction(Intent.ACTION_TIMEZONE_CHANGED);
        intents[2] = new Intent();
        intents[2].setAction(Intent.ACTION_DATE_CHANGED);
        intents[3] = new Intent();
        intents[3].setAction(Intent.ACTION_TIME_TICK);

        for (final Intent intent : intents) {
            try {
                runTestOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mReceiver.onReceive(mTargetContext, intent);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (intent.getAction() == Intent.ACTION_TIME_TICK) {
                            assertEquals(ListenerForReceiver.TIME_CHANGE, mListener
                                    .getReceiverData());
                        } else {
                            assertEquals(ListenerForReceiver.DATE_CHANGE, mListener
                                    .getReceiverData());
                        }
                    }
                });
            } catch (Throwable e) {}
        }
    }
}
