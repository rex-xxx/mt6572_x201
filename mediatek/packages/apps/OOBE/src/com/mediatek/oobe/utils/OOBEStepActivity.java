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

package com.mediatek.oobe.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.oobe.R;
import com.mediatek.xlog.Xlog;

public class OOBEStepActivity extends Activity implements Button.OnClickListener {
    protected static final String TAG = OOBEConstants.TAG;
    protected LinearLayout mProgressbarLayout;
    protected Button mBackBtn;
    protected Button mNextBtn;
    protected int mTotalStep;
    protected int mStepIndex;
    protected boolean mLastStep = false;

    private String mStepSpecialTag = "";
    private TextView mSettingTitle;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main_frame);
        initLayout();
    }

    /**
     * this method must be called in sub-class onCreate() method
     */
    protected void initLayout() {
        mStepSpecialTag = getStepSpecialTag();
        mTotalStep = getIntent().getIntExtra(OOBEConstants.OOBE_BASIC_STEP_TOTAL, 1);
        mStepIndex = getIntent().getIntExtra(OOBEConstants.OOBE_BASIC_STEP_INDEX, 0);
        Xlog.i(TAG, mStepSpecialTag + "initLayout(), step index = " + mStepIndex + "/" + mTotalStep);

        mSettingTitle = (TextView) findViewById(R.id.settings_title);
        mProgressbarLayout = (LinearLayout) findViewById(R.id.progressbar_layout);
        mBackBtn = (Button) findViewById(R.id.panel_button_back);
        mNextBtn = (Button) findViewById(R.id.panel_button_next);

        if (mStepIndex == 1) {
            mBackBtn.setVisibility(View.INVISIBLE);
        }
        mBackBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);

        for (int i = 0; i < mTotalStep; i++) {
            ImageView child = (ImageView) mProgressbarLayout.getChildAt(i);
            if (i == mStepIndex - 1) {
                child.setImageResource(R.drawable.progress_radio_on);
            } else {
                child.setImageResource(R.drawable.progress_radio_off);
            }
            child.setVisibility(View.VISIBLE);
        }
        if (mTotalStep == mStepIndex) {
            Xlog.i(TAG, "Get to last settings step");
            mLastStep = true;
            mNextBtn.setText(R.string.oobe_btn_text_next);
        }
    }

    // private void initGesturerListener(){
    // //add for gesture begin
    // OOBEGesturer gestureListener = new OOBEGesturer(this, mTotalStep, mStepIndex);
    // mRootView = findViewById(R.id.oobe_root_view);
    // if(mRootView!=null && (mRootView instanceof OOBERootView)){
    // ((OOBERootView)mRootView).addGestureListener(gestureListener);
    // }else{
    // Xlog.d(TAG, getStepSpecialTag()+"root view is not OOBERootView, then no gesture for it");
    // }
    // //add for gesture end
    // }

    /**
     * If you want to implement your special layout in some step, like step title, inherit this and do it
     */
    protected void initSpecialLayout(int titleRes, int summaryRes) {
        mSettingTitle.setText(titleRes);
    }

    @Override
    public void onClick(View v) {
        if (v == mBackBtn) {
            onNextStep(false);
        } else if (v == mNextBtn) {
            onNextStep(true);
        } else {
            Xlog.d(TAG, getStepSpecialTag() + "Which button is clicked??");
        }
    }

    /**
     * onNextStep
     * 
     * @param isNext
     *            boolean
     */
    public void onNextStep(boolean isNext) {
        if (!isNext) {
            Xlog.i(TAG, getStepSpecialTag() + ">>Back to former settings, mStepIndex=" + mStepIndex);
            finishActivityByResultCode(OOBEConstants.RESULT_CODE_BACK);
        } else {
            Xlog.i(TAG, getStepSpecialTag() + ">>Forward to next settings");
            // if (mLastStep) {
            // finishActivityByResultCode(OOBEConstants.RESULT_CODE_FINISH);
            // } else {
            finishActivityByResultCode(OOBEConstants.RESULT_CODE_NEXT);
            // }
        }
    }

    protected void finishActivityByResultCode(int resultCode) {
        Intent intent = new Intent();
        setResult(resultCode, intent);
        finish();
        Xlog.i(TAG, "Finish " + getStepSpecialTag());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            Xlog.i(TAG, "Press back button to former settings");
            int firstRunFlag = Settings.System.getInt(getContentResolver(), "oobe_has_run", 0);

            Xlog.i(TAG, "Is first started?" + (firstRunFlag == 0));

            if ((firstRunFlag == 0)) {
                return true;// prevent default behavior
            }
            // }
            finishActivityByResultCode(OOBEConstants.RESULT_CODE_BACK);
            break;
        default:
            break;
        }

        return super.onKeyDown(keyCode, event);
    }

    protected String getStepSpecialTag() {
        return "OOBEStepActivity";
    }
}
