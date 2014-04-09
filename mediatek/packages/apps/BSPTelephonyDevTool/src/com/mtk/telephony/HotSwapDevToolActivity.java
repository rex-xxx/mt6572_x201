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

package com.mtk.telephony;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.common.featureoption.FeatureOption;
import com.android.internal.telephony.gemini.GeminiPhone;

public class HotSwapDevToolActivity extends Activity {
    private static final String TAG = "Hot_Swap_Dev";

    private static final String AT_CMD_SIM_PLUG_OUT = "AT+ESIMTEST=17";
    private static final String AT_CMD_SIM_PLUG_IN = "AT+ESIMTEST=18";
    private static final String AT_CMD_SIM_MISSING = "AT+ESIMTEST=65";
    private static final String AT_CMD_SIM_RECOVERY = "AT+ESIMTEST=66";

    private Phone mPhone;

    private Button mPlugOutSim1;
    private Button mPlugOutSim2;
    private Button mPlugInSim1;
    private Button mPlugInSim2;
    private Button mMissingSim1;
    private Button mMissingSim2;
    private Button mRecoverySim1;
    private Button mRecoverySim2;
 
    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mPlugOutSim1) {
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugOutSim2) {
                String cmdStr[] = {AT_CMD_SIM_PLUG_OUT, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugInSim1) {
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mPlugInSim2) {
                String cmdStr[] = {AT_CMD_SIM_PLUG_IN, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mMissingSim1) {
                String cmdStr[] = {AT_CMD_SIM_MISSING, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mMissingSim2) {
                String cmdStr[] = {AT_CMD_SIM_MISSING, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mRecoverySim1) {
                String cmdStr[] = {AT_CMD_SIM_RECOVERY, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_1);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            } else if (v == mRecoverySim2) {
                String cmdStr[] = {AT_CMD_SIM_RECOVERY, ""};
                if (FeatureOption.MTK_GEMINI_SUPPORT)
					((GeminiPhone)mPhone).invokeOemRilRequestStringsGemini(cmdStr, null, PhoneConstants.GEMINI_SIM_2);
                else
                    mPhone.invokeOemRilRequestStrings(cmdStr, null);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hot_swap_dev_tool);

        mPlugOutSim1 = (Button) findViewById(R.id.btn_plug_out_sim1);
        mPlugOutSim2 = (Button) findViewById(R.id.btn_plug_out_sim2);
        mPlugInSim1 = (Button) findViewById(R.id.btn_plug_in_sim1);
        mPlugInSim2 = (Button) findViewById(R.id.btn_plug_in_sim2);
        mMissingSim1 = (Button) findViewById(R.id.btn_missing_sim1);
        mMissingSim2 = (Button) findViewById(R.id.btn_missing_sim2);
        mRecoverySim1 = (Button) findViewById(R.id.btn_recovery_sim1);
        mRecoverySim2 = (Button) findViewById(R.id.btn_recovery_sim2);

        mPlugOutSim1.setOnClickListener(mOnClickListener);
        mPlugOutSim2.setOnClickListener(mOnClickListener);
        mPlugInSim1.setOnClickListener(mOnClickListener);
        mPlugInSim2.setOnClickListener(mOnClickListener);
        mMissingSim1.setOnClickListener(mOnClickListener);
        mMissingSim2.setOnClickListener(mOnClickListener);
        mRecoverySim1.setOnClickListener(mOnClickListener);
        mRecoverySim2.setOnClickListener(mOnClickListener);

        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            mPlugOutSim2.setVisibility(View.GONE);
            mPlugInSim2.setVisibility(View.GONE);
            mMissingSim2.setVisibility(View.GONE);
            mRecoverySim2.setVisibility(View.GONE);
        }

        mPhone = PhoneFactory.getDefaultPhone();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
    }
}