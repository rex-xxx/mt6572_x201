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

package com.mediatek.oobe.basic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.OOBEStepPreferenceActivity;
import com.mediatek.oobe.utils.RadioButtonPreference;
import com.mediatek.xlog.Xlog;

import java.util.HashMap;
import java.util.List;

public class KeyboardSetupActivity extends OOBEStepPreferenceActivity implements Button.OnClickListener {
    private static final String TAG = OOBEConstants.TAG;

    private static final String MTK_INPUT_METHOD = "com.mediatek.ime/.MtkIME";
    public static final String ACTION_INPUT_METHOD_SETTING = "com.mediatek.ime.INPUT_METHOD_SELECTION_WIZARD";

    private List<InputMethodInfo> mInputMethodProperties;
    private HashMap<String, CharSequence> mInputMethodMaps = new HashMap<String, CharSequence>();
    private String mLastInputMethodId;
    final TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
    private RadioButtonPreference mLastSelectedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Xlog.d(TAG, "OnCreate KeyboardSetupActivity");
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.frame_layout);
        addPreferencesFromResource(R.xml.oobe_preference_keyboard_setting);

        // initLayout();
        initSpecialLayout(R.string.oobe_title_keyboard_setting, R.string.oobe_summary_keyboard_setting);
        initInputMethods();
    }

    // protected void initSpecialLayout() {
    // settingTitle.setText(R.string.oobe_title_keyboard_setting);
    // settingSummary.setText(R.string.oobe_summary_keyboard_setting);
    // }

    private void initInputMethods() {
        mLastInputMethodId = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mInputMethodProperties = imm.getEnabledInputMethodList();
        int nCount = (mInputMethodProperties == null ? 0 : mInputMethodProperties.size());

        for (int i = 0; i < nCount; ++i) {
            final InputMethodInfo property = mInputMethodProperties.get(i);
            final String id = property.getId();
            CharSequence label = property.loadLabel(getPackageManager());
            mInputMethodMaps.put(id, label);
            if (OOBEConstants.DEBUG) {
                Xlog.v(OOBEConstants.TAG, "Input method " + i + ": " + id + ". And the label is " + label);
            }
            String summary = "";
            if (isMTKIme(id)) {
                summary = getResources().getString(R.string.oobe_ime_suggested);
            }

            RadioButtonPreference pref = new RadioButtonPreference(KeyboardSetupActivity.this, label.toString(), summary);
            pref.setNote(id);
            if (mLastInputMethodId.equals(id)) {
                pref.setChecked(true);
                mLastSelectedPref = pref;
            }
            getPreferenceScreen().addPreference(pref);
        }

        Xlog.v(OOBEConstants.TAG, "Default Input method: " + mLastInputMethodId);
    }

    private boolean isMTKIme(String imeString) {
        return imeString.equals(MTK_INPUT_METHOD);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof RadioButtonPreference) {
            selectIME((RadioButtonPreference) preference);
        }
        return true;
    }

    private void selectIME(RadioButtonPreference pref) {

        if (mLastSelectedPref != null) {
            if (mLastSelectedPref == pref) {
                return;
            }
            mLastSelectedPref.setChecked(false);
        }

        pref.setChecked(true);
        mLastSelectedPref = pref;

        // Must be system program to get the permission
        InputMethodManager imm2 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (this.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != 0) {
            // PERMISSION_GRANTED == 0
            Xlog.i(TAG, "myActivity requires permission " + android.Manifest.permission.WRITE_SECURE_SETTINGS);
        } else {
            Xlog.v(TAG, "Select IME is: " + pref.getNote());
            imm2.setInputMethod(null, pref.getNote());
            mLastInputMethodId = pref.getNote();
        }
        Xlog.v(TAG,
                "After selected: " + Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));

    }

    @Override
    public void onNextStep(boolean isNext) {
        if (!isNext) {
            Xlog.i(TAG, "Back to former settings");
            finishActivityByResultCode(OOBEConstants.RESULT_CODE_BACK);
        } else {
            Xlog.i(TAG, "Forward to next settings");
            if (mLastStep) {
                finishActivityByResultCode(OOBEConstants.RESULT_CODE_FINISH);
            } else {
                if (isMTKIme(mLastInputMethodId)) {
                    startMTKIMESetting();
                } else {
                    finishActivityByResultCode(OOBEConstants.RESULT_CODE_NEXT);
                }
            }
        }
    }

    private void startMTKIMESetting() {
        Intent intent = new Intent();
        intent.setAction(ACTION_INPUT_METHOD_SETTING);
        intent.putExtra(OOBEConstants.OOBE_BASIC_STEP_TOTAL, mTotalStep);
        intent.putExtra(OOBEConstants.OOBE_BASIC_STEP_INDEX, mStepIndex);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        OOBEConstants.logd("KeyboardSetupActivity onActivityResult: resultCode = " + resultCode);

        if (OOBEConstants.RESULT_CODE_BACK == resultCode) {
            Xlog.d(TAG, "back request");
        } else if (OOBEConstants.RESULT_CODE_NEXT == resultCode) {
            finishActivityByResultCode(OOBEConstants.RESULT_CODE_NEXT);
        }
    }

    @Override
    protected String getStepSpecialTag() {
        return "KeyboardSetupActivity";
    }
}
