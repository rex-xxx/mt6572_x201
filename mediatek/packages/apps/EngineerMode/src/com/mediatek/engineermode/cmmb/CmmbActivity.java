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

package com.mediatek.engineermode.cmmb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

/**
 * Description: a helper of TD CMMB module.
 * 
 * @author mtk54043
 * 
 */
public class CmmbActivity extends Activity {

    public static final String TAG = "EM_CMMB";

    public static final String CMMB_PREF = "cmmb_pref";
    public static final String CMMB_SAVEMFSFILE_KEY = "cmmb_savemfsfile_key";
    public static final String CMMB_MEMSETSPIBUF_KEY = "cmmb_memsetspibuf_key";
    public static final String CMMB_MTS_KEY = "cmmb_mts_key";
    public static final String CMMB_MBBMS30_KEY = "cmmb_mbbms30_key";
    public static final String TESTKEY = "MODE";
    public static final String FREQKEY = "FREQ";
    public static final String ON = "1";
    public static final String OFF = "0";
    public static final int INTERNALTESTVALUE = 0;
    public static final int EXTERNALTESTVALUE = 1;

    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmmb_activity);

        final SharedPreferences preferences = getSharedPreferences(CMMB_PREF, Context.MODE_WORLD_READABLE);

        View.OnClickListener listener = new View.OnClickListener() {

            public void onClick(View v) {
                Editor edit = preferences.edit();
                switch (v.getId()) {
                case R.id.cmmb_save_on_radio:
                    edit.putString(CMMB_SAVEMFSFILE_KEY, "1");
                    break;
                case R.id.cmmb_save_off_radio:
                    edit.putString(CMMB_SAVEMFSFILE_KEY, "0");
                    break;
                case R.id.cmmb_memset_on_radio:
                    edit.putString(CMMB_MEMSETSPIBUF_KEY, "1");
                    break;
                case R.id.cmmb_memset_off_radio:
                    edit.putString(CMMB_MEMSETSPIBUF_KEY, "0");
                    break;
                case R.id.cmmb_mts_on_radio:
                    edit.putString(CMMB_MTS_KEY, "1");
                    break;
                case R.id.cmmb_mts_off_radio:
                    edit.putString(CMMB_MTS_KEY, "0");
                    break;
                case R.id.cmmb_mbbms30_on_radio:
                    edit.putString(CMMB_MBBMS30_KEY, "1");
                    break;
                case R.id.cmmb_mbbms30_off_radio:
                    edit.putString(CMMB_MBBMS30_KEY, "0");
                    break;
                case R.id.cmmb_internal_ft:
                    Elog.i(TAG, "click internal btn");
                    Intent intentI = new Intent();
                    intentI.setComponent(new ComponentName("com.mediatek.cmmb.app", "com.mediatek.cmmb.app.FtmActivity"));
                    if (getPackageManager().resolveActivity(intentI, 0) != null) {
                        intentI.putExtra(TESTKEY, INTERNALTESTVALUE);
                        String freq = mEditText.getText().toString();
                        if (null != freq && !freq.equals("")) {
                            intentI.putExtra(FREQKEY, mEditText.getText().toString());
                        }
                        startActivity(intentI);
                    }
                    break;
                case R.id.cmmb_external_ft:
                    Elog.i(TAG, "click external btn");
                    Intent intentE = new Intent();
                    intentE.setComponent(new ComponentName("com.mediatek.cmmb.app", "com.mediatek.cmmb.app.FtmActivity"));
                    if (getPackageManager().resolveActivity(intentE, 0) != null) {
                        intentE.putExtra(TESTKEY, EXTERNALTESTVALUE);
                        startActivity(intentE);
                    }
                    break;
                default:
                    break;
                }
                edit.commit();
            }
        };

        RadioButton saveOnRadioBtn = (RadioButton) findViewById(R.id.cmmb_save_on_radio);
        RadioButton saveOffRadioBtn = (RadioButton) findViewById(R.id.cmmb_save_off_radio);

        RadioButton memsetOnRadioBtn = (RadioButton) findViewById(R.id.cmmb_memset_on_radio);
        RadioButton memsetOffRadioBtn = (RadioButton) findViewById(R.id.cmmb_memset_off_radio);

        RadioButton mtsOnRadioBtn = (RadioButton) findViewById(R.id.cmmb_mts_on_radio);
        RadioButton mtsOffRadioBtn = (RadioButton) findViewById(R.id.cmmb_mts_off_radio);
        RadioButton mbbms30OnRadioBtn = (RadioButton) findViewById(R.id.cmmb_mbbms30_on_radio);
        RadioButton mbbms30OffRadioBtn = (RadioButton) findViewById(R.id.cmmb_mbbms30_off_radio);

        Button startInternalBtn = (Button) findViewById(R.id.cmmb_internal_ft);
        Button startExternalBtn = (Button) findViewById(R.id.cmmb_external_ft);
        mEditText = (EditText) findViewById(R.id.cmmb_external_edit);

        startInternalBtn.setOnClickListener(listener);
        startExternalBtn.setOnClickListener(listener);

        saveOnRadioBtn.setOnClickListener(listener);
        saveOffRadioBtn.setOnClickListener(listener);

        memsetOnRadioBtn.setOnClickListener(listener);
        memsetOffRadioBtn.setOnClickListener(listener);

        mtsOnRadioBtn.setOnClickListener(listener);
        mtsOffRadioBtn.setOnClickListener(listener);
        mbbms30OnRadioBtn.setOnClickListener(listener);
        mbbms30OffRadioBtn.setOnClickListener(listener);

        String saveMfsFile = preferences.getString(CMMB_SAVEMFSFILE_KEY, OFF);
        if (ON.equals(saveMfsFile)) {
            saveOnRadioBtn.setChecked(true);
            saveOffRadioBtn.setChecked(false);
        } else {
            saveOnRadioBtn.setChecked(false);
            saveOffRadioBtn.setChecked(true);
        }

        String memsetSpibuf = preferences.getString(CMMB_MEMSETSPIBUF_KEY, OFF);
        if (ON.equals(memsetSpibuf)) {
            memsetOnRadioBtn.setChecked(true);
            memsetOffRadioBtn.setChecked(false);
        } else {
            memsetOnRadioBtn.setChecked(false);
            memsetOffRadioBtn.setChecked(true);
        }

        String mts = preferences.getString(CMMB_MTS_KEY, OFF);
        if (ON.equals(mts)) {
            mtsOnRadioBtn.setChecked(true);
            mtsOffRadioBtn.setChecked(false);
        } else {
            mtsOnRadioBtn.setChecked(false);
            mtsOffRadioBtn.setChecked(true);
        }
        String mbbms30 = preferences.getString(CMMB_MBBMS30_KEY, OFF);
        if (ON.equals(mbbms30)) {
            mbbms30OnRadioBtn.setChecked(true);
            mbbms30OffRadioBtn.setChecked(false);
        } else {
            mbbms30OnRadioBtn.setChecked(false);
            mbbms30OffRadioBtn.setChecked(true);
        }
    }
}
