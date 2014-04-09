/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.wifi.hotspot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.settings.R;
import com.mediatek.xlog.Xlog;

public class WifiApWpsDialog extends AlertDialog 
        implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String TAG = "WifiApWpsDialog";
    private static final int PUSH_BUTTON = 0;
    private static final int PIN_FROM_CLIENT = 1;

    private int mWpsMode = 0;
    private Context mContext;
    private View mView;
    private Spinner mWpsModeSpinner;
    private WifiManager mWifiManager;

    public WifiApWpsDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Xlog.d(TAG,"onCreate, return dialog");
        
        mView = getLayoutInflater().inflate(R.layout.wifi_ap_wps_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.wifi_ap_wps_dialog_title);

        mWpsModeSpinner = ((Spinner) mView.findViewById(R.id.wps_mode));
        mWpsModeSpinner.setOnItemSelectedListener(this);
        setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.wifi_connect), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.wifi_cancel), this);
        /// M: WifiManager memory leak , change context to getApplicationContext @{
        mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ///@}
        super.onCreate(savedInstanceState);
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            WpsInfo config = new WpsInfo();
            if (mWpsMode == PUSH_BUTTON) {
                config.setup = WpsInfo.PBC;
                config.BSSID = "any";
            } else if (mWpsMode == PIN_FROM_CLIENT) {
                config.setup = WpsInfo.DISPLAY;
                config.pin = ((EditText)mView.findViewById(R.id.pin_edit)).getText().toString();
            }
            mWifiManager.startApWps(config);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(mWpsModeSpinner)) {
            mWpsMode = position;
            if (mWpsMode == PUSH_BUTTON) {
                mView.findViewById(R.id.type_pin_field).setVisibility(View.GONE);
            } else if (mWpsMode == PIN_FROM_CLIENT) {
                mView.findViewById(R.id.type_pin_field).setVisibility(View.VISIBLE);
            }
        } 
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
