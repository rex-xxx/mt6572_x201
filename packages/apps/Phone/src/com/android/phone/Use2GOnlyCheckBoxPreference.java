/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
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

package com.android.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.telephony.Phone;

public class Use2GOnlyCheckBoxPreference extends CheckBoxPreference {
    private static final String LOG_TAG = "Use2GOnlyCheckBoxPreference";

    private Phone mPhone;
    private MyHandler mHandler;
    private ProgressDialog mPd = null;

    public Use2GOnlyCheckBoxPreference(Context context) {
        this(context, null);
    }

    public Use2GOnlyCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs,com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public Use2GOnlyCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPhone = PhoneGlobals.getPhone();
        mHandler = new MyHandler();
        
        int networkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE, -1);
        Log.i(LOG_TAG, " At the begining, networkMode=" + networkMode + " end");
        mPhone.getPreferredNetworkType(
                mHandler.obtainMessage(MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
    }

    private void showProgressDialog() {
        // TODO Auto-generated method stub
        mPd = new ProgressDialog(this.getContext());
        mPd.setMessage(this.getContext().getText(R.string.updating_settings));
        mPd.setCancelable(false);
        mPd.setIndeterminate(true);
        mPd.show();
    }

    @Override
    protected void  onClick() {
        super.onClick();

        showProgressDialog();
        int networkType = isChecked() ? Phone.NT_MODE_GSM_ONLY : Phone.NT_MODE_WCDMA_PREF;
        Log.i(LOG_TAG, "set preferred network type=" + networkType);
        android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE, networkType);
        mPhone.setPreferredNetworkType(networkType, mHandler
                .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
   }

    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
                default:
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int type = ((int[])ar.result)[0];
                /*if (type != Phone.NT_MODE_GSM_ONLY) {
                    // Allow only NT_MODE_GSM_ONLY or NT_MODE_WCDMA_PREF
                    type = Phone.NT_MODE_WCDMA_PREF;
                }*/
                Log.i(LOG_TAG, "get preferred network type=" + type);
                setChecked(type == Phone.NT_MODE_GSM_ONLY);
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE, type);
            } else {
                // Weird state, disable the setting
                Log.i(LOG_TAG, "get preferred network type, exception=" + ar.exception);
                setEnabled(false);
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (mPd != null && mPd.isShowing()) {
                mPd.dismiss();
            }

            if (ar.exception != null) {
                // Yikes, error, disable the setting
                setEnabled(false);
                // Set UI to current state
                Log.i(LOG_TAG, "set preferred network type, exception=" + ar.exception);
                mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            } else {
                Log.i(LOG_TAG, "set preferred network type done");
            }
        }
    }
}
