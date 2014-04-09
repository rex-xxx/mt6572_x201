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

package com.mediatek.oobe.advanced;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class AdvanceSettingsEntrance extends Activity {
    private static final String TAG = OOBEConstants.TAG;
    // finish advance settings, this application will exit
    public static final int REQUEST_CODE = 10;

    public static final String OOBE_STEP_TOTAL = "oobe_step_total";
    public static final String OOBE_STEP_INDEX = "oobe_step_index";
    public static final String OOBE_STEP_NEXT = "oobe_step_next";
    private ImageView mAdvanceSettingsIcon;
    private Button mBackBtn;
    private Button mNextBtn;

    // This list can be modified dynamically, which list all advanced setting steps
    private List<String> mActivityList;

    public static final String ADVANCE_SETTING_END_ACTIVITY = "com.mediatek.android.oobe.advanced.AdvanceSettingsEnd";
    private int mCurrentIndex = 0;
    private int mTotalStep = 0;// mActivityList.length;
    private boolean mIsGoToNextStep = true;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState
     *            Bundle
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advance_settings_entrance);
        mActivityList = new ArrayList<String>();
        // Maybe we have no SNS type in our phone, then SNS settings is not needed, remove it
        String[] allAdvancedSettingStep = { "com.mediatek.oobe.advanced.SyncSettings",
                "com.mediatek.oobe.advanced.AccountSettings", "com.mediatek.oobe.advanced.SNSSettings" };

        if (!isHasSNS()) {
            Xlog.d(TAG, "== No SNS type detected, remove SNS settings");
            for (int i = 0; i < allAdvancedSettingStep.length; i++) {
                if (OOBEConstants.ACTION_SNS_SETTING.equals(allAdvancedSettingStep[i])) {
                    continue;
                }
                mActivityList.add(allAdvancedSettingStep[i]);
            }
        } else {
            for (int i = 0; i < allAdvancedSettingStep.length; i++) {
                mActivityList.add(allAdvancedSettingStep[i]);
            }
        }

        mTotalStep = (mActivityList != null ? mActivityList.size() : 0);

        mAdvanceSettingsIcon = (ImageView) findViewById(R.id.advance_settings_entrance);
        mBackBtn = (Button) findViewById(R.id.panel_button_back);
        mNextBtn = (Button) findViewById(R.id.panel_button_next);

        mNextBtn.setText(R.string.oobe_btn_text_finish);

        mAdvanceSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentIndex = 0;
                goToNextSettings(true);
            }
        });
        mNextBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                quitAdvanceSettings();
            }
        });
        mBackBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(OOBEConstants.RESULT_CODE_BACK);
                finish();
            }
        });
    }

    /**
     * quit advance settings
     */
    public void quitAdvanceSettings() {
        Xlog.i(TAG, "Do not enter advance settings, just quit");
        setResult(OOBEConstants.RESULT_CODE_FINISH);
        finish();
    }

    private boolean isHasSNS() {
        AuthenticatorDescription[] mAuthDescs = AccountManager.get(this).getAuthenticatorTypes();
        if (mAuthDescs == null || mAuthDescs.length == 0) {
            return false;
        }
        for (int i = 0; i < mAuthDescs.length; i++) {
            String accountType = mAuthDescs[i].type;
            Xlog.d(TAG, "SNS account: " + accountType);
            if (!"com.android.exchange".equals(accountType) && !"com.android.email".equals(accountType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            Xlog.i(TAG, "AdvanceSettingsEntrance.onActivityResult, code=" + resultCode + "  current index=" + mCurrentIndex);
            switch (resultCode) {
            case OOBEConstants.RESULT_CODE_NEXT:
                goToNextSettings(true);
                break;
            case OOBEConstants.RESULT_CODE_BACK:
                goToNextSettings(false);
                break;
            case OOBEConstants.RESULT_CODE_FINISH:
                setResult(OOBEConstants.RESULT_CODE_FINISH);
                finish();
                break;
            default:
                Xlog.d(TAG, "Enter default branch, where am I? result code = " + resultCode + ", mCurrent Index="
                        + mCurrentIndex);
                finish();
                break;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(OOBEConstants.RESULT_CODE_BACK);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Activity flow go back to this main activity, decide which activity will be the next one
     */
    private void goToNextSettings(boolean isNextStep) {
        mIsGoToNextStep = isNextStep;
        if (isNextStep) {
            mCurrentIndex++;
        } else {
            mCurrentIndex--;
        }
        Xlog.v(TAG, "AdvanceSettingsEntrance.goToNextSettings(" + isNextStep + "), current index=" + mCurrentIndex);
        if (mCurrentIndex < 0) {
            Xlog.w(TAG, "AdvanceSettingsEntrance, mCurrentIndex=" + mCurrentIndex + ", finish OOBE now");
            setResult(OOBEConstants.RESULT_CODE_FINISH);
            finish();
            return;
        } else if(mCurrentIndex == 0) {
            Xlog.e(TAG, "Stay on advance settings entrance page");
            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
            return;
         }


        Intent intent = new Intent();
        if (mCurrentIndex > mTotalStep) {
            intent.putExtra(AdvanceSettingsEntrance.OOBE_STEP_TOTAL, mTotalStep);
            intent.putExtra(AdvanceSettingsEntrance.OOBE_STEP_INDEX, mTotalStep);
            intent.setClass(this, AdvanceSettingsEnd.class);
            startActivityForResult(intent, REQUEST_CODE);
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
            return;
        }
        String activityStr = mActivityList.get(mCurrentIndex - 1);
        int seperatorIndex = activityStr.indexOf("/");
        if (seperatorIndex > 0) {
            intent.setComponent(new ComponentName(activityStr.substring(0, seperatorIndex), activityStr
                    .substring(seperatorIndex + 1)));
        } else {
            intent.setAction(activityStr);
        }
        intent.putExtra(AdvanceSettingsEntrance.OOBE_STEP_TOTAL, mTotalStep);
        intent.putExtra(AdvanceSettingsEntrance.OOBE_STEP_INDEX, mCurrentIndex);
        startActivityForResult(intent, REQUEST_CODE);

        if (mIsGoToNextStep) {
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        } else {
            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        }
    }
}