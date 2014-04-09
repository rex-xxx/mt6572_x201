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

/**
 * Thread class for executing a Runnable containing assertions in a separate thread.
 * Uncaught exceptions in the Runnable are rethrown in the context of the the thread
 * calling the <code>runTest()</code> method.
 */
public final class TestThread extends Thread {
    private Throwable mThrowable;
    private Runnable mTarget;

    public TestThread(Runnable target) {
        mTarget = target;
    }

    @Override
    public final void run() {
        try {
            mTarget.run();
        } catch (Throwable t) {
            mThrowable = t;
        }
    }

    /**
     * Run the target Runnable object and wait until the test finish or throw
     * out Exception if test fail.
     *
     * @param runTime
     * @throws Throwable
     */
    public void runTest(long runTime) throws Throwable {
        start();
        joinAndCheck(runTime);
    }

    /**
     * Get the Throwable object which is thrown when test running
     * @return  The Throwable object
     */
    public Throwable getThrowable() {
        return mThrowable;
    }

    /**
     * Set the Throwable object which is thrown when test running
     * @param t The Throwable object
     */
    public void setThrowable(Throwable t) {
        mThrowable = t;
    }

    /**
     * Wait for the test thread to complete and throw the stored exception if there is one.
     *
     * @param runTime The time to wait for the test thread to complete.
     * @throws Throwable
     */
    public void joinAndCheck(long runTime) throws Throwable {
        this.join(runTime);
        if (this.isAlive()) {
            this.interrupt();
            this.join(runTime);
            throw new Exception("Thread did not finish within allotted time.");
        }
        checkException();
    }

    /**
     * Check whether there is an exception when running Runnable object.
     * @throws Throwable
     */
    public void checkException() throws Throwable {
        if (mThrowable != null) {
            throw mThrowable;
        }
    }
}
