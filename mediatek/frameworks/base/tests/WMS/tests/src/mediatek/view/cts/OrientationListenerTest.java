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
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.cts.window;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

import android.content.Context;
import android.hardware.SensorManager;
import android.test.AndroidTestCase;
import android.view.OrientationListener;

/**
 * Test {@link OrientationListener}.
 */
@TestTargetClass(OrientationListener.class)
public class OrientationListenerTest extends AndroidTestCase {
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor OrientationListener#OrientationListener(Context).",
            method = "OrientationListener",
            args = {Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor OrientationListener#OrientationListener(Context, int).",
            method = "OrientationListener",
            args = {Context.class, int.class}
        )
    })
    public void testConstructor() {
        new MockOrientationListener(mContext);

        new MockOrientationListener(mContext, SensorManager.SENSOR_DELAY_UI);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link OrientationListener#enable()}. "
                    + "This method is simply called to make sure that no exception is thrown. "
                    + "The registeration of the listener can not be tested becuase there is no way "
                    + "to simulate sensor events on the emulator",
            method = "enable",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link OrientationListener#disable()}. "
                    + "This method is simply called to make sure that no exception is thrown. "
                    + "The registeration of the listener can not be tested becuase there is no way "
                    + "to simulate sensor events on the emulator",
            method = "disable",
            args = {}
        )
    })
    @ToBeFixed(explanation = "Can not simulate sensor events on the emulator.")
    public void testRegisterationOfOrientationListener() {
        // these methods are called to assure that no exception is thrown
        MockOrientationListener listener = new MockOrientationListener(mContext);
        listener.disable();
        listener.enable();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link OrientationListener#onAccuracyChanged(int, int)}.",
        method = "onAccuracyChanged",
        args = {int.class, int.class}
    )
    public void testOnAccuracyChanged() {
        // this method is called to assure that no exception is thrown
        new MockOrientationListener(mContext).onAccuracyChanged(SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        new MockOrientationListener(mContext).onAccuracyChanged(SensorManager.SENSOR_ORIENTATION,
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link OrientationListener#onSensorChanged(int , float[])}.",
        method = "onSensorChanged",
        args = {int.class, float[].class}
    )
    public void testOnSensorChanged() {
        // this method is called to assure that no exception is thrown
        MockOrientationListener listener = new MockOrientationListener(mContext);
        float[] mockData = new float[SensorManager.RAW_DATA_Z + 1];
        mockData[SensorManager.RAW_DATA_X] = 3.0f;
        mockData[SensorManager.RAW_DATA_Y] = 4.0f;
        mockData[SensorManager.RAW_DATA_Z] = 5.0f * 2.0f + 0.1f;
        new MockOrientationListener(mContext).onSensorChanged(SensorManager.SENSOR_ACCELEROMETER,
                mockData);

        mockData[SensorManager.RAW_DATA_X] = 4.0f;
        mockData[SensorManager.RAW_DATA_Y] = 4.0f;
        mockData[SensorManager.RAW_DATA_Z] = 5.0f * 2.0f;
        listener.reset();
        new MockOrientationListener(mContext).onSensorChanged(SensorManager.SENSOR_MAGNETIC_FIELD,
                mockData);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link OrientationListener#onOrientationChanged(int)}. "
                + "This test does not check callback of the "
                + "{@link OrientationListener#onOrientationChanged(int)} "
                + "because there is no way to simulate the sensor events on the emulator.",
        method = "onSensorChanged",
        args = {int.class, float[].class}
    )
    @ToBeFixed(explanation = "Can not simulate sensor events on the emulator.")
    public void testOnOrientationChanged() {
        MockOrientationListener listener = new MockOrientationListener(mContext);
        listener.enable();
        // TODO can not simulate sensor events on the emulator.
    }

    private class MockOrientationListener extends OrientationListener {
        private boolean mHasCalledOnOrientationChanged;

        public boolean hasCalledOnOrientationChanged() {
            return mHasCalledOnOrientationChanged;
        }

        public void reset() {
            mHasCalledOnOrientationChanged = false;
        }

        public MockOrientationListener(Context context) {
            super(context);
        }

        public MockOrientationListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            mHasCalledOnOrientationChanged = true;
        }
    }
}
