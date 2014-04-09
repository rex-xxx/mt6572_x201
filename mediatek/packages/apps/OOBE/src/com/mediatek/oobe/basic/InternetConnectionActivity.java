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

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Button;

import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.OOBEStepPreferenceActivity;
import com.mediatek.oobe.utils.RadioButtonPreference;
import com.mediatek.xlog.Xlog;

public class InternetConnectionActivity extends OOBEStepPreferenceActivity implements Button.OnClickListener {
    private static final String TAG = OOBEConstants.TAG;
    private RadioButtonPreference mWifiNGprsPref;
    private RadioButtonPreference mWifiOnlyPref;
    private boolean mDataConnectionOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        Xlog.d(TAG, "OnCreate InternetConnectionActivity");
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.frame_layout);
        addPreferencesFromResource(R.xml.oobe_preference_internet_connection);
        // initLayout();
        initSpecialLayout();
    }

    protected void initSpecialLayout() {
        super.initSpecialLayout(R.string.oobe_title_internet_connection_setting, R.string.oobe_summary_internet_connection);
        mWifiNGprsPref = (RadioButtonPreference) findPreference("wifi_and_gprs");
        mWifiOnlyPref = (RadioButtonPreference) findPreference("wifi_only");
        mDataConnectionOpen = isDataConnection();
        if (mDataConnectionOpen) {
            mWifiNGprsPref.setChecked(true);
            mWifiOnlyPref.setChecked(false);
        } else {
            mWifiNGprsPref.setChecked(false);
            mWifiOnlyPref.setChecked(true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mWifiNGprsPref) {
            mWifiNGprsPref.setChecked(true);
            mWifiOnlyPref.setChecked(false);
            Xlog.i(TAG, "Enable mobile data");
            setDataConnection(true);
            mDataConnectionOpen = true;
        } else if (preference == mWifiOnlyPref) {
            mWifiNGprsPref.setChecked(false);
            mWifiOnlyPref.setChecked(true);
            Xlog.i(TAG, "Disable mobile data");
            setDataConnection(false);
            mDataConnectionOpen = false;
        } else {
            return false;
        }
        return true;
    }

    private void setDataConnection(boolean bEnable) {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        cm.setMobileDataEnabled(bEnable);

        if (isDataConnection()) {
            Xlog.i(TAG, "mobile data enabled");
        } else {
            Xlog.i(TAG, "mobile data disabled");
        }
    }

    private boolean isDataConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        return cm.getMobileDataEnabled();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }
}
