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

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.test.ActivityUnitTestCase;

import com.mediatek.engineermode.tvout.TVOutEntry;

public class TvOutTest extends ActivityUnitTestCase<TVOutEntry> {

    private static final String TVOUT_ENABLE = "tvout_en_disable";
    private static final String CHINESE_TEST_PATT = "chinese_test_patter";
    private static final String KEY_TV_SYSTEM = "tv_system";
    private Context mContext;
    private Instrumentation mInstrumentation;
    private Intent mIntent;
    private PreferenceActivity mActivity;

    public TvOutTest() {
        super(TVOutEntry.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getContext();
        mIntent = new Intent(Intent.ACTION_MAIN);
        mIntent.setComponent(new ComponentName(mContext, TVOutEntry.class
            .getName()));
        mActivity = startActivity(mIntent, null, null);
    }

    public void test01_Precondition() {
        testConditions();
    }

    public void test02_TvOutOnOff() {
        testConditions();
        final CheckBoxPreference haveTvoutCheck =
            (CheckBoxPreference) mActivity.findPreference(TVOUT_ENABLE);
        assertNotNull(haveTvoutCheck);
        haveTvoutCheck.getOnPreferenceChangeListener().onPreferenceChange(
            haveTvoutCheck, !haveTvoutCheck.isChecked());
        EmOperate.runOnUiThread(mInstrumentation, mActivity, new Runnable() {

            public void run() {
                haveTvoutCheck.setChecked(!haveTvoutCheck.isChecked());
            }
        });
        haveTvoutCheck.getOnPreferenceChangeListener().onPreferenceChange(
            haveTvoutCheck, !haveTvoutCheck.isChecked());
        EmOperate.runOnUiThread(mInstrumentation, mActivity, new Runnable() {

            public void run() {
                haveTvoutCheck.setChecked(!haveTvoutCheck.isChecked());
            }
        });
    }

    public void test03_TvSystemTest() {
        testConditions();
        final ListPreference tVSystem =
            (ListPreference) mActivity.findPreference(KEY_TV_SYSTEM);
        assertNotNull(tVSystem);
        if (tVSystem.getSummary() == null) {
            tVSystem.getOnPreferenceChangeListener().onPreferenceChange(
                tVSystem, "PAL");
            assertEquals(tVSystem.getSummary(), "PAL");
            tVSystem.getOnPreferenceChangeListener().onPreferenceChange(
                tVSystem, "NTSC");
            assertEquals(tVSystem.getSummary(), "NTSC");
        } else if (tVSystem.getSummary().equals("NTSC")) {
            tVSystem.getOnPreferenceChangeListener().onPreferenceChange(
                tVSystem, "PAL");
            assertEquals(tVSystem.getSummary(), "PAL");
            tVSystem.getOnPreferenceChangeListener().onPreferenceChange(
                tVSystem, "NTSC");
            assertEquals(tVSystem.getSummary(), "NTSC");
        } else {
            tVSystem.getOnPreferenceChangeListener().onPreferenceChange(
                tVSystem, "NTSC");
            assertEquals(tVSystem.getSummary(), "NTSC");
            tVSystem.getOnPreferenceChangeListener().onPreferenceChange(
                tVSystem, "PAL");
            assertEquals(tVSystem.getSummary(), "PAL");
        }
    }

    public void test04_TestPatter() {
        testConditions();
        final Preference chineseTestPatter =
            mActivity.findPreference(CHINESE_TEST_PATT);
        assertNotNull(chineseTestPatter);
        mActivity.onPreferenceTreeClick(mActivity.getPreferenceScreen(),
            chineseTestPatter);
    }

    public void testConditions() {
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
        assertNotNull(mActivity);
        assertNotNull(mIntent);
    }
}