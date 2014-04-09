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

package com.mediatek.engineermode.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.test.SingleLaunchActivityTestCase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.engineermode.cpustress.ApMcu;
import com.mediatek.engineermode.cpustress.ClockSwitch;
import com.mediatek.engineermode.cpustress.CpuStressTest;
import com.mediatek.engineermode.cpustress.SwVideoCodec;
import com.mediatek.engineermode.memory.Memory;
import com.mediatek.engineermode.R;

import java.io.File;

public class CpuStressTestTest extends
        SingleLaunchActivityTestCase<CpuStressTest> {

    private static final String TAG = "EMTest/CpustressTest";
    private static final String FILE_THERMAL_ETC = "/etc/.tp/.ht120.mtc";
    private static final int ITEM_TEST_COUNT = 4;
    private static Solo sSolo = null;
    private static Activity sActivity = null;
    private static Context sContext = null;
    private static Instrumentation sInst = null;
    private static ListView sListView = null;
    private static boolean sHaveThermalEtc = false;
    private static boolean sInit = false;
    private static boolean sFinished = false;

    public CpuStressTestTest() {
        super("com.mediatek.engineermode", CpuStressTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sInit = true;
        if (null == sInst) {
            sInst = getInstrumentation();
        }
        if (null == sContext) {
            sContext = sInst.getTargetContext();
        }
        if (null == sActivity) {
            sActivity = getActivity();
            if (sActivity.getClass() != CpuStressTest.class) {
                sActivity.finish();
                sActivity = launchActivity("com.mediatek.engineermode",
                        CpuStressTest.class, null);
            }
        }
        if (null == sSolo) {
            sSolo = new Solo(sInst, sActivity);
        }
        sHaveThermalEtc = new File(FILE_THERMAL_ETC).exists();
    }

    @Override
    protected void tearDown() throws Exception {
        if (sFinished) {
            sSolo.finishOpenedActivities();
        }
        super.tearDown();
    }

    public void test01_Precondition() {
        assertNotNull(sInst);
        assertNotNull(sContext);
        assertNotNull(sActivity);
        assertNotNull(sSolo);
        if (null == sListView) {
            sListView = (ListView) sActivity
                    .findViewById(R.id.listview_hqa_cpu_main);
        }
        assertNotNull(sListView);
    }

    public void test02_TestList() {
        int itemCount = sListView.getAdapter().getCount();
        assertEquals(ITEM_TEST_COUNT, itemCount);
        sSolo.clickOnText(sListView.getAdapter().getItem(0).toString());
        sSolo.sleep(EmOperate.TIME_MID);
    }

    public void test03_TestRadio() {
        RadioGroup rg = (RadioGroup) sActivity
                .findViewById(R.id.hqa_cpu_main_radiogroup);
        int count = rg.getChildCount();
        assertEquals(6, count);
        for (int i = 5; i >= 0; i--) {
            sSolo.clickOnRadioButton(i);
        }
    }

    public void test04_TestThermal() {
        CheckBox cbThermal = (CheckBox) sActivity
                .findViewById(R.id.hqa_cpu_main_checkbox);
        assertEquals(sHaveThermalEtc, cbThermal.isEnabled());
        if (sHaveThermalEtc) {
            sSolo.clickOnCheckBox(0);
            sSolo.sleep(EmOperate.TIME_LONG);
            sSolo.clickOnCheckBox(0);
            sSolo.sleep(EmOperate.TIME_LONG);
            sSolo.clickOnCheckBox(0);
        }
    }

    public void test05_TestRuntime() {
        sSolo.clickOnRadioButton(1);
        sSolo.clickOnText(sListView.getAdapter().getItem(0).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(6);
        sSolo.goBack();
        RadioGroup rg = (RadioGroup) sActivity
                .findViewById(R.id.hqa_cpu_main_radiogroup);
        int count = rg.getChildCount();
        for (int i = 0; i < count; i++) {
            RadioButton rb = (RadioButton) rg.getChildAt(i);
            assertFalse(rb.isEnabled());
        }
        CheckBox cbThermal = (CheckBox) sActivity
                .findViewById(R.id.hqa_cpu_main_checkbox);
        assertFalse(cbThermal.isEnabled());
    }

    public void test06_TestStop() {
        sSolo.clickOnText(sListView.getAdapter().getItem(0).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(6);
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.goBack();
        RadioGroup rg = (RadioGroup) sActivity
                .findViewById(R.id.hqa_cpu_main_radiogroup);
        int count = rg.getChildCount();
        for (int i = 0; i < count; i++) {
            RadioButton rb = (RadioButton) rg.getChildAt(i);
            assertTrue(rb.isEnabled());
        }
        CheckBox cbThermal = (CheckBox) sActivity
                .findViewById(R.id.hqa_cpu_main_checkbox);
        assertTrue(cbThermal.isEnabled());
        sSolo.clickOnText(sListView.getAdapter().getItem(2).toString());
        sSolo.clickOnRadioButton(0);
    }

    public void test07_ApMcuSingle() {
        sSolo.clickOnRadioButton(2);
        sSolo.clickOnText(sListView.getAdapter().getItem(0).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        for (int i = 0; i < 6; i++) {
            sSolo.clickOnCheckBox(i);
        }
        sSolo.clickOnButton(6);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        assertFalse(TextUtils.isEmpty(sSolo.getText(4).getText()));
        assertTrue(TextUtils.isEmpty(sSolo.getText(5).getText()));
        assertFalse(TextUtils.isEmpty(sSolo.getText(9).getText()));
        assertTrue(TextUtils.isEmpty(sSolo.getText(10).getText()));
        assertFalse(TextUtils.isEmpty(sSolo.getText(33).getText()));
        sSolo.clickOnButton(6);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        sSolo.goBack();
    }

    public void test08_SwCodecSingle() {
        sSolo.clickOnText(sListView.getAdapter().getItem(1).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(0);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        assertFalse(TextUtils.isEmpty(sSolo.getText(3).getText()));
        assertTrue(TextUtils.isEmpty(sSolo.getText(4).getText()));
        assertFalse(TextUtils.isEmpty(sSolo.getText(7).getText()));
        sSolo.clickOnButton(0);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        sSolo.goBack();
    }

    public void test09_ApMcuQuad() {
        sSolo.clickOnRadioButton(5);
        sSolo.clickOnText(sListView.getAdapter().getItem(0).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(6);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        for (int i = 4; i < 8; i++) {
            assertFalse(TextUtils.isEmpty(sSolo.getText(i).getText()));
        }
        assertFalse(TextUtils.isEmpty(sSolo.getText(33).getText()));
        sSolo.clickOnButton(6);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        sSolo.goBack();
    }

    public void test10_SwCodecDual() {
        sSolo.clickOnText(sListView.getAdapter().getItem(1).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(0);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        for (int i = 3; i <= 7; i++) {
            assertFalse(TextUtils.isEmpty(sSolo.getText(i).getText()));
        }
        sSolo.clickOnButton(0);
        sSolo.sleep(EmOperate.TIME_SUPER_LONG);
        sSolo.goBack();
    }

    public void test11_TestClockSwitch() {
        sSolo.clickOnText(sListView.getAdapter().getItem(2).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnCheckBox(0);
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnCheckBox(0);
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(3);
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(4);
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.clickOnButton(4);
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.goBack();
    }
    
    public void test13_Help() {
        sSolo.clickOnText(sListView.getAdapter().getItem(3).toString());
        sSolo.sleep(EmOperate.TIME_LONG);
        sSolo.goBack();
    }

    public void test13_TestRestore() {
        sFinished = true;
        sSolo.clickOnRadioButton(0);
        if (sHaveThermalEtc) {
            sSolo.clickOnCheckBox(0);
        }
    }
}
