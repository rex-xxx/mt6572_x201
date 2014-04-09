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

package mediatek.system.cts;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.util.StringBuilderPrinter;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(Looper.class)
public class LooperTest extends AndroidTestCase {

    public static final long WAIT_TIME = 1000;

    private boolean mHasRun;

    private Looper mLooper = null;
    private boolean mHasQuit;
    private Handler mLoopHandler;

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "dump",
        args = {Printer.class, String.class}
    )
    public void testDump() {
        StringBuilderPrinter printer = new StringBuilderPrinter(new StringBuilder());
        final String prefix = "LooperTest";
        Looper.myLooper().dump(printer, prefix);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getMainLooper",
        args = {}
    )
    public void testGetMainLooper() {
        Looper looper = Looper.getMainLooper();
        assertNotNull(looper);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "loop",
        args = {}
    )
    public void testLoop() {
        MockRunnable run = new MockRunnable();

        Handler handler = new Handler();
        Message msg = Message.obtain(handler, run);
        handler.sendMessageAtTime(msg, 0);
        assertFalse(run.runCalled);
        Looper.loop();

        assertTrue(run.runCalled);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "myLooper",
        args = {}
    )
    public void testMyLooper() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                assertNull(Looper.myLooper());
                Looper.prepare();
                assertNotNull(Looper.myLooper());
            }
        });

        t.runTest(WAIT_TIME);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "myQueue",
        args = {}
    )
    public void testMyQueue() throws Throwable {
        MessageQueue mq = Looper.myQueue();
        assertNotNull(mq);

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                try {
                    assertNull(Looper.myQueue());
                    fail("should throw exception");
                } catch (Throwable e) {
                    // expected
                }
            }
        });

        t.runTest(WAIT_TIME);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "prepare",
        args = {}
    )
    public void testPrepare() throws Throwable {
        try {
            Looper.prepare();
            fail("should throw exception");
        } catch (RuntimeException e) {
            //expected
        }

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                try {
                    Looper.prepare();
                    fail("should throw exception");
                } catch (Throwable e) {
                    //expected
                }
            }
        });

        t.runTest(WAIT_TIME);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "prepareMainLooper",
        args = {}
    )
    
    public void testPrepareMainLooper() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                try {
                    Looper.prepareMainLooper();
                    fail("should throw exception because the main thread was already prepared");
                } catch (Throwable e) {
                    //expected
                }
            }
        });

        t.runTest(WAIT_TIME);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "quit",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getThread",
            args = {}
        )
    })
    public void testQuit() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                mHasQuit = false;
                Looper.prepare();
                mLoopHandler = new Handler();
                mLooper = Looper.myLooper();
                Looper.loop();
                mHasQuit = true;
            }
        });

        // Here doesn't call runTest() because we don't want to wait the runTest finish.
        // Just need to handle Looper#quit();
        t.start();
        Thread.sleep(WAIT_TIME);
        assertSame(t, mLooper.getThread());
        int time = 100;
        // Send message before Looper has quit.
        assertTrue(mLoopHandler.sendEmptyMessageAtTime(0, SystemClock.uptimeMillis() + time));
        Thread.sleep(WAIT_TIME);

        mLooper.quit();
        Thread.sleep(WAIT_TIME);
        // Send message after Looper has quit.
        assertFalse(mLoopHandler.sendEmptyMessageAtTime(1, SystemClock.uptimeMillis() + time));
        assertTrue(mHasQuit);

        t.joinAndCheck(WAIT_TIME);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setMessageLogging",
        args = {Printer.class}
    )
    public void testSetMessageLogging() throws Throwable {
        mHasRun = false;

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();
                MockPrinter mp = new MockPrinter();
                Looper.myLooper().setMessageLogging(mp);
                MockRunnable run = new MockRunnable();

                Handler handler = new Handler();
                Message msg = Message.obtain(handler, run);
                handler.sendMessageAtTime(msg, 0);

                Looper.loop();
                assertNotNull(mp.str);
                mHasRun = true;
            }
        });

        t.runTest(WAIT_TIME);

        assertTrue(mHasRun);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "toString",
        args = {}
    )
    public void testToString() {
        assertNotNull(Looper.myLooper().toString());
    }

    private Handler mHandler;
    boolean mThreadRunning;

    public void testSideEffect1() throws Throwable {

        mThreadRunning = true;

        new Thread(mRunnable0).start();
        Thread.sleep(WAIT_TIME);

        new Thread(mRunnable1).start();
        new Thread(mRunnable2).start();
        Thread.sleep(WAIT_TIME*10);

        mThreadRunning = false;
        MockRunnable run = new MockRunnable();
        Message msg = Message.obtain(mHandler, run);
        mHandler.sendMessageAtTime(msg, 0);
        Thread.sleep(WAIT_TIME*3);

    }

    Runnable mRunnable0 = new Runnable() {
        public void run() {
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }
    };


    Runnable mRunnable1 = new Runnable() {
        public void run() {
            while(mThreadRunning)
            {
                mHandler.sendEmptyMessage(0);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
                mHandler.sendEmptyMessage(1);
            }
        }
    };

    Runnable mRunnable2 = new Runnable() {
        public void run() {
            while(mThreadRunning)
            {
                mHandler.removeMessages(0);
            }
        }
    };

    class MockPrinter implements Printer {
        public String str;

        public void println(String x) {
            str = x;
        }
    }

    private class MockRunnable implements Runnable {
        public boolean runCalled = false;

        public void run() {
            runCalled = true;
            Looper.myLooper().quit();
        }

        public void stop() {
            Looper.myLooper().quit();
        }
    }
}

