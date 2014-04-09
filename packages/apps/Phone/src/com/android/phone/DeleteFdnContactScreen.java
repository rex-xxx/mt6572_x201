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
 * Copyright (C) 2006 The Android Open Source Project
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

import static android.view.Window.PROGRESS_VISIBILITY_OFF;
import static android.view.Window.PROGRESS_VISIBILITY_ON;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.settings.CallSettings;
import com.mediatek.xlog.Xlog;

/**
 * Activity to let the user delete an FDN contact.
 */
public class DeleteFdnContactScreen extends Activity {
    private static final String LOG_TAG = PhoneGlobals.LOG_TAG;
    private static final boolean DBG = true;

    private static final String INTENT_EXTRA_INDEX = "index";
    private static final String INTENT_EXTRA_NAME = "name";
    private static final String INTENT_EXTRA_NUMBER = "number";

    private static final int PIN2_REQUEST_CODE = 100;
    private static final int GET_PIN_RETRY_EMPTY = -1;
    private final BroadcastReceiver mReceiver = new DeleteFdnContactScreenBroadcastReceiver();

    private String mIndex;
    private String mName;
    private String mNumber;
    private String mPin2;
    
    private int mSimId;

    protected QueryHandler mQueryHandler;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        resolveIntent();

        authenticatePin2();

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.delete_fdn_contact_screen);
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int retryNumber = getPin2RetryNumber();
        if (retryNumber == 0) {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (DBG) {
            log("onActivityResult");
        }

        switch (requestCode) {
            case PIN2_REQUEST_CODE:
                Bundle extras = (intent != null) ? intent.getExtras() : null;
                if (extras != null) {
                    mPin2 = extras.getString("pin2");
                showStatus(getResources()
                        .getText(R.string.deleting_fdn_contact));
                    deleteContact();
                } else {
                    // if they cancelled, then we just cancel too.
                    if (DBG) {
                        log("onActivityResult: CANCELLED");
                    }
                    displayProgress(false);
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void resolveIntent() {
        Intent intent = getIntent();

        mIndex = intent.getStringExtra(INTENT_EXTRA_INDEX);
        mName =  intent.getStringExtra(INTENT_EXTRA_NAME);
        mNumber =  intent.getStringExtra(INTENT_EXTRA_NUMBER);
        mSimId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
    }

    private void deleteContact() {
        StringBuilder buf = new StringBuilder();
        if (TextUtils.isEmpty(mName)) {
            buf.append("number='");
        } else {
            buf.append("tag='");
            buf.append(mName);
            buf.append("' AND number='");
        }
        buf.append(mNumber);
        buf.append("' AND pin2='");
        buf.append(mPin2);
        buf.append("'");

        Uri uri = getContentURI();

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startDelete(0, null, uri, buf.toString(), null);
        displayProgress(true);
    }
    
    private Uri getContentURI() {
        String fdnUri = "content://icc/fdn";
        if (CallSettings.isMultipleSim()) {
            switch (mSimId) {
                case PhoneConstants.GEMINI_SIM_1:
                    fdnUri = "content://icc/fdn1";
                break;
                case PhoneConstants.GEMINI_SIM_2:
                    fdnUri = "content://icc/fdn2";
                break;
                case PhoneConstants.GEMINI_SIM_3:
                    fdnUri = "content://icc/fdn3";
                break;
                case PhoneConstants.GEMINI_SIM_4:
                    fdnUri = "content://icc/fdn4";
                break;
                default:
                    log("error as mSimId = " + mSimId);    
                break;
            }
        }
        return Uri.parse(fdnUri);
    }

    private void authenticatePin2() {
        Intent intent = new Intent();
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        intent.setClass(this, GetPin2Screen.class);
        startActivityForResult(intent, PIN2_REQUEST_CODE);
    }

    private void displayProgress(boolean flag) {
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                flag ? PROGRESS_VISIBILITY_ON : PROGRESS_VISIBILITY_OFF);
    }

    // Replace the status field with a toast to make things appear similar
    // to the rest of the settings.  Removed the useless status field.
    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private int getPin2RetryNumber() {
        String pinStr = "gsm.sim.retry.pin2";
        if (CallSettings.isMultipleSim()) {
            switch (mSimId) {
                case PhoneConstants.GEMINI_SIM_1:
                    pinStr = "gsm.sim.retry.pin2";
                break;
                case PhoneConstants.GEMINI_SIM_2:
                    pinStr = "gsm.sim.retry.pin2.2";
                break;
                case PhoneConstants.GEMINI_SIM_3:
                    pinStr = "gsm.sim.retry.pin2.3";
                break;
                case PhoneConstants.GEMINI_SIM_4:
                    pinStr = "gsm.sim.retry.pin2.4";
                break;
                default:
                    log("error without correct ");
                break;
            }    
        }
        
        if (mSimId == PhoneConstants.GEMINI_SIM_2) {
            return SystemProperties.getInt("gsm.sim.retry.pin2.2", GET_PIN_RETRY_EMPTY);
        }
        return SystemProperties.getInt("gsm.sim.retry.pin2",
                GET_PIN_RETRY_EMPTY);
    }

    private String getRetryPin2() {
        int retryCount = getPin2RetryNumber();
        switch (retryCount) {
        case GET_PIN_RETRY_EMPTY:
            return " ";
        case 1:
            return getString(R.string.one_retry_left);
        default:
            return getString(R.string.retries_left,retryCount);
        }
    }

    private void handleResult(EditFdnContactScreen.Operate op, int errorCode) {
        /*  1= Ok
         *  0= unknown error code
         * -1= number length too long
         * -2= name length too long
         * -3= Storage is full
         * -4= Phone book is not ready
         * -5= Pin2 error
         */
        if (DBG) {
            log("handleResult: [errorcode=" + errorCode + "]");
        }
        switch(errorCode) {
        case IccProvider.ERROR_ICC_PROVIDER_UNKNOWN:
            if (DBG) {
                log("handleResult: Error,unknown error code!");
            }
            showStatus(getString(R.string.fdn_errorcode_unknown_info));
            break;
        case IccProvider.ERROR_ICC_PROVIDER_NUMBER_TOO_LONG:
            if (DBG) {
                log("handleResult: Error,Contact number's length is too long !");
            }
            showStatus(getString(R.string.fdn_errorcode_number_info));
            break;
        case IccProvider.ERROR_ICC_PROVIDER_TEXT_TOO_LONG:
            if (DBG) {
                log("handleResult: Error,Contact name's length is too long !");
            }
            showStatus(getString(R.string.fdn_errorcode_name_info));
            break;
        case IccProvider.ERROR_ICC_PROVIDER_STORAGE_FULL:
            if (DBG) {
                log("handleResult: Error,storage is full !");
            }
            showStatus(getString(R.string.fdn_errorcode_storage_info));
            break;
        case IccProvider.ERROR_ICC_PROVIDER_NOT_READY:
            if (DBG) {
                log("handleResult: Error,Phone book is not ready !");
            }
            showStatus(getString(R.string.fdn_errorcode_phb_info));
            break;
        case IccProvider.ERROR_ICC_PROVIDER_PASSWORD_ERROR:
            if (DBG) {
                log("handleResult: Error,invalid pin2 !");
            }
            handlePin2Error();
            break;
        default:
            if (DBG) {
                log("handleResult: Error,system return unknown error code!");
            }
            break;
        }

        if (errorCode >= IccProvider.ERROR_ICC_PROVIDER_NO_ERROR) {
            showStatus(getResources().getText(R.string.fdn_contact_deleted));
        }
        int retryNumber = getPin2RetryNumber();
        if (retryNumber != 0) {
            finishThisActivity();
        }
    }
    
    private void handlePin2Error() {
        int retryNumber = getPin2RetryNumber();
        if (DBG) {
            log("handleResult: retryNumber=" + retryNumber);
        }
        if (retryNumber == 0) {
            if (DBG) {
                log("handleResult: pin2 retry= 0 ,pin2 is locked!");
            }
            AlertDialog a = new AlertDialog.Builder(this).setPositiveButton(
                    R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).setMessage(R.string.puk2_requested).create();
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            a.show();
        } else {
            showStatus(getString(R.string.fdn_errorcode_pin2_info) + "\n" + getRetryPin2());
        }
    }
    
    private void finishThisActivity() {
        mHandler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 2000);
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
        }

        protected void onInsertComplete(int token, Object cookie, Uri uri) {
        }

        protected void onUpdateComplete(int token, Object cookie, int result) {
        }

        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (DBG) {
                log("onDeleteComplete");
            }
            displayProgress(false);
            handleResult(EditFdnContactScreen.Operate.DELETE,result);
        }

    }

    private void log(String msg) {
        Xlog.d(LOG_TAG, "[DeleteFdnContact] " + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private class DeleteFdnContactScreenBroadcastReceiver extends
            BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    }
}
