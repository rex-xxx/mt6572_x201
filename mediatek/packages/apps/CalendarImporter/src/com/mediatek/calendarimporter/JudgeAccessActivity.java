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
 */
package com.mediatek.calendarimporter;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.widget.Toast;

import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.calendarimporter.utils.Utils;

public class JudgeAccessActivity extends Activity {
    private static final String TAG = "JudgeAccessActivity";

    private static final String HAS_TRY_TO_ADD_ACCOUNT = "HasTryToAddAccount";
    private static final String DATA_URI = "DataUri";
    private boolean mHasTryToAddAccount = false;
    private Uri mDataUri;

    static final int DURATION = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataUri = getIntent().getData();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.hasExchangeOrGoogleAccount(this)) {
            LogUtils.d(TAG, "onResume,show SelectActivity... ");
            showSelectActivity(mDataUri);
        } else {
            if (mHasTryToAddAccount) {
                Intent intent = getIntent();
                intent.setClass(this, ShowPreviewActivity.class);
                intent.setData(mDataUri);
                LogUtils.d(TAG, "onResume,Show PreviewActivity... ");
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, R.string.no_access_toast, DURATION).show();
                Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                // intent.setData(uri);
                intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] { CalendarContract.AUTHORITY });
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                LogUtils.d(TAG, "onResume,Show Settings... ");
                startActivity(intent);
                mHasTryToAddAccount = true;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogUtils.d(TAG, "onSaveInstanceState  " + mDataUri);
        outState.putBoolean(HAS_TRY_TO_ADD_ACCOUNT, mHasTryToAddAccount);
        outState.putParcelable(DATA_URI, mDataUri);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mHasTryToAddAccount = savedInstanceState.getBoolean(HAS_TRY_TO_ADD_ACCOUNT);
        mDataUri = savedInstanceState.getParcelable(DATA_URI);
        LogUtils.d(TAG, "onRestoreInstanceState() mDataUri=" + mDataUri);
    }

    private void showSelectActivity(Uri uri) {
        Intent intent = getIntent();
        intent.setClass(this, HandleProgressActivity.class);
        startActivity(intent);
        finish();
    }

}
