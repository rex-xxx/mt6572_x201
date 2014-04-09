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
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.TextUtils;
import android.text.method.DialerKeyListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.settings.CallSettings;
import com.mediatek.xlog.Xlog;

/**
 * Activity to let the user add or edit an FDN contact.
 */
public class EditFdnContactScreen extends Activity {
    private static final String LOG_TAG = "Settings/" + PhoneGlobals.LOG_TAG;
    private static final boolean DBG = true;

    // Menu item codes
    private static final int MENU_IMPORT = 1;
    private static final int MENU_DELETE = 2;
    private static final int GET_PIN_RETRY_EMPTY = -1;
    private static final String INTENT_EXTRA_INDEX = "index";
    private static final String INTENT_EXTRA_NAME = "name";
    private static final String INTENT_EXTRA_NUMBER = "number";
    private static final String INTENT_EXTRA_ADD = "addcontact";    

    private static final int PIN2_REQUEST_CODE = 100;
    private final BroadcastReceiver mReceiver = new EditFdnContactScreenBroadcastReceiver();

    private String mIndex;
    private String mName;
    private String mNumber;
    private String mPin2;
    private boolean mAddContact;
    private QueryHandler mQueryHandler;
    
    private int mSimId;

    private EditText mNameField;
    private EditText mNumberField;
    private LinearLayout mPinFieldContainer;
    private Button mButton;

    private Handler mHandler = new Handler();
    private static final int PIN2_MAX = 3;

    /**
     * Constants used in importing from contacts
     */
    /** request code when invoking subactivity */
    private static final int CONTACTS_PICKER_CODE = 200;
    /** projection for phone number query */
    private static final String NUM_PROJECTION[] = {
        CommonDataKinds.Phone.DISPLAY_NAME, CommonDataKinds.Phone.NUMBER};
    /** static intent to invoke phone number picker */
    private static final Intent CONTACT_IMPORT_INTENT;
    static {
        CONTACT_IMPORT_INTENT = new Intent(Intent.ACTION_GET_CONTENT);
        CONTACT_IMPORT_INTENT
                .setType(CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
    }
    /** flag to track saving state */
    private boolean mDataBusy;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        resolveIntent();

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.edit_fdn_contact_screen);
        setupView();
        setTitle(mAddContact ? R.string.add_fdn_contact
                : R.string.edit_fdn_contact);

        mDataBusy = false;
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
        mNameField.requestFocus();
        int retryNumber = getPin2RetryNumber();
        if (retryNumber == 0) {
            finish();
        }
    }

    /**
     * We now want to bring up the pin request screen AFTER the contact
     * information is displayed, to help with user experience.
     * 
     * Also, process the results from the contact picker.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
        Intent intent) {
        if (DBG) {
            log("onActivityResult request:" + requestCode + " result:" + resultCode);
        }

        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
        case PIN2_REQUEST_CODE:
            mButton.setEnabled(false);
            Bundle extras = (intent != null) ? intent.getExtras() : null;
            if (extras != null) {
                mPin2 = extras.getString("pin2");
                if (mAddContact) {
                    addContact();
                } else {
                    updateContact();
                }
            } else if (resultCode != RESULT_OK) {
                // if they cancelled, then we just cancel too.
                if (DBG) {
                    log("onActivityResult: cancelled.");
                }
                finish();
            }
            break;
        // look for the data associated with this number, and update
        // the display with it.
        case CONTACTS_PICKER_CODE:
            if (resultCode != RESULT_OK) {
                if (DBG) {
                    log("onActivityResult: cancelled.");
                }
                return;
            }

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(intent.getData(),
                        NUM_PROJECTION, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    mNameField.setText(cursor.getString(0));
                    mNumberField.setText(cursor.getString(1).replaceAll("-", "").replaceAll(" ", ""));
                }
            } finally {
                if    (cursor != null) {
                    cursor.close();
                }
            }
            break;
        default :
            break;
        }
    }

    /**
     * Overridden to display the import and delete commands.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Resources r = getResources();

        // Added the icons to the context menu
        menu.add(0, MENU_IMPORT, 0,
                r.getString(R.string.importToFDNfromContacts)).setIcon(
                R.drawable.ic_menu_contact);
        menu.add(0, MENU_DELETE, 0, r.getString(R.string.menu_delete)).setIcon(
                android.R.drawable.ic_menu_delete);
        return true;
    }

    /**
     * Allow the menu to be opened ONLY if we're not busy.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        return mDataBusy ? false : result;
    }

    /**
     * Overridden to allow for handling of delete and import.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_IMPORT:
            try {
                startActivityForResult(CONTACT_IMPORT_INTENT, CONTACTS_PICKER_CODE);
            } catch (ActivityNotFoundException e) {
                Xlog.d(LOG_TAG, e.toString());
            }
            return true;
        case MENU_DELETE:
            deleteSelected();
            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resolveIntent() {
        Intent intent = getIntent();

        mIndex = intent.getStringExtra(INTENT_EXTRA_INDEX);
        Xlog.i(LOG_TAG,"mIndex is " + mIndex);
        mName = intent.getStringExtra(INTENT_EXTRA_NAME);
        mNumber = intent.getStringExtra(INTENT_EXTRA_NUMBER);
        mAddContact = intent.getBooleanExtra(INTENT_EXTRA_ADD, false);
        mSimId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY,  -1);
    }

    private static class FdnKeyListener extends DialerKeyListener {
        private static FdnKeyListener sKeyListener;
        /**
         * The characters that are used.
         * 
         * @see KeyEvent#getMatch
         * @see #getAcceptedChars
         */
        public static final char[] CHARACTERS = new char[] { '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', '#', '*', '+', ',', 'N'};

        @Override
        protected char[] getAcceptedChars() {
            return CHARACTERS;
        }

        // public CharSequence filter(CharSequence source, int start, int end,
        // Spanned dest , int dstart, int dend){
        // if (TextUtils.equals(source, "N")){
        // return "?";
        // }else if (TextUtils.equals(source, ",")){
        // return "P";
        // }
        //            
        // return super.filter(source, start, end, dest, dstart, dend);
        // }

        public static FdnKeyListener getInstance() {
            if (sKeyListener == null) {
                sKeyListener = new FdnKeyListener();
            }
            return sKeyListener;
        }

    }
    /**
     * We have multiple layouts, one to indicate that the user needs to open the
     * keyboard to enter information (if the keybord is hidden). So, we need to
     * make sure that the layout here matches that in the layout file.
     */
    private void setupView() {
        mNameField = (EditText) findViewById(R.id.fdn_name);
        /*
        if (mNameField != null) {
            mNameField.setOnFocusChangeListener(mOnFocusChangeHandler);
            mNameField.setOnClickListener(mClicked);
        }
        */

        mNumberField = (EditText) findViewById(R.id.fdn_number);
        if (mNumberField != null) {
            mNumberField.setKeyListener(FdnKeyListener.getInstance());
        /*
            mNumberField.setOnFocusChangeListener(mOnFocusChangeHandler);
            mNumberField.setOnClickListener(mClicked);
        */
        }

        if (!mAddContact) {
            if (mNameField != null) {
                mNameField.setText(mName);
            }
            if (mNumberField != null) {
                mNumberField.setText(mNumber);
            }
        }

        mButton = (Button) findViewById(R.id.button);
        if (mButton != null) {
            mButton.setOnClickListener(mClicked);
        }

        mPinFieldContainer = (LinearLayout) findViewById(R.id.pinc);

    }

    private String getNameFromTextField() {
        return mNameField.getText().toString();
    }

    private String getNumberFromTextField() {
        return mNumberField.getText().toString();
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

    /**
     * @param number
     *            is voice mail number
     * @return true if number length is less than 40-digit limit
     */
    private boolean isValidNumber(String number) {
             return (number.length() <= 40);
    }
    private boolean isValidChar(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+'
                || c == 'N' || c == 'n' || c == ',';
    }

    private void addContact() {
        if (DBG) {
            log("addContact");
        }


        Uri uri = getContentURI();

        ContentValues bundle = new ContentValues(3);
        bundle.put("tag", getNameFromTextField());
        bundle.put("number", getNumberFromTextField());
        bundle.put("pin2", mPin2);

        if (DBG) {
            log("[name = " + getNameFromTextField() + "]");
            log("[number = " + getNumberFromTextField() + "]");
        }

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startInsert(0, null, uri, bundle);
        displayProgress(true);
        showStatus(getResources().getText(R.string.adding_fdn_contact));
    }

    private void updateContact() {
        if (DBG) {
            log("updateContact");
        }

        Uri uri = getContentURI();

        ContentValues bundle = new ContentValues();
        bundle.put("index", mIndex);
        bundle.put("tag", mName);
        bundle.put("number", mNumber);
        bundle.put("newTag", getNameFromTextField());
        bundle.put("newNumber", getNumberFromTextField());
        bundle.put("pin2", mPin2);

        if (DBG) {
            log("[new name = " + getNameFromTextField() + "]");
            log("[new number = " + getNumberFromTextField() + "]");
        }

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startUpdate(0, null, uri, bundle, null, null);
        displayProgress(true);
        showStatus(getResources().getText(R.string.updating_fdn_contact));
    }

    /**
     * Handle the delete command, based upon the state of the Activity.
     */
    private void deleteSelected() {
        // delete ONLY if this is NOT a new contact.
        if (!mAddContact) {
            Intent intent = new Intent();
            intent.setClass(this, DeleteFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_INDEX, mIndex);
            intent.putExtra(INTENT_EXTRA_NAME, mName);
            intent.putExtra(INTENT_EXTRA_NUMBER, mNumber);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            startActivity(intent);
        }
        finish();
    }

    private void authenticatePin2() {
        Intent intent = new Intent();
        intent.setClass(this, GetPin2Screen.class);
        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        startActivityForResult(intent, PIN2_REQUEST_CODE);
    }

    private void displayProgress(boolean flag) {
        // indicate we are busy.
        mDataBusy = flag;
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                mDataBusy ? PROGRESS_VISIBILITY_ON : PROGRESS_VISIBILITY_OFF);
        // make sure we don't allow calls to save when we're
        // not ready for them.
        mButton.setClickable(!mDataBusy);
    }

    /**
     * Removed the status field, with preference to displaying a toast to match
     * the rest of settings UI.
     */
    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private int getPin2RetryNumber() {
        if (mSimId == PhoneConstants.GEMINI_SIM_2) {
            return SystemProperties.getInt("gsm.sim.retry.pin2.2", GET_PIN_RETRY_EMPTY);
        }
        return SystemProperties.getInt("gsm.sim.retry.pin2", GET_PIN_RETRY_EMPTY);
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

    public static enum Operate {
        INSERT,
        UPDATE,
        DELETE        
    };
    
    private void handleResult(Operate op,int errorCode) {
        /*  1= Ok
         *  0= unknown error code
         * -1= number length too long
         * -2= name length too long
         * -3= Storage is full
         * -4= Phone book is not ready
         * -5= Pin2 error
         */
        switch(errorCode) {
        //TODO     IccProvider.ERROR_ICC_PROVIDER_NO_ERROR
        case 1:
            if (op != Operate.DELETE) {
                if (DBG) {
                    log("handleResult: success!");
                }
                showStatus(getResources().getText(mAddContact ? R.string.fdn_contact_added : R.string.fdn_contact_updated));
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        finish();
                    }
                }, 2000);
            }
            break;
        //TODO     IccProvider.ERROR_ICC_PROVIDER_UNKNOWN:
        case 0:
            if (DBG) {
                log("handleResult: Error,unknown error code!");
            }
            showStatus(getString(R.string.fdn_errorcode_unknown_info));
            mButton.setEnabled(true);
            break;
        //TODO     IccProvider.ERROR_ICC_PROVIDER_NUMBER_TOO_LONG
        case -1:
            if (DBG) {
                log("handleResult: Error,Contact number's length is too long !");
            }
            showStatus(getString(R.string.fdn_errorcode_number_info));
            mButton.setEnabled(true);
            break;
        //TODO     IccProvider.ERROR_ICC_PROVIDER_TEXT_TOO_LONG
        case -2:
            if (DBG) {
                log("handleResult: Error,Contact name's length is too long !");
            }
            showStatus(getString(R.string.fdn_errorcode_name_info));
            mButton.setEnabled(true);
            break;
        //TODO     IccProvider.ERROR_ICC_PROVIDER_STORAGE_FULL
        case -3:
            if (DBG) {
                log("handleResult: Error,storage is full !");
            }
            showStatus(getString(R.string.fdn_errorcode_storage_info));
            mButton.setEnabled(true);
            break;
        //TODO     IccProvider.ERROR_ICC_PROVIDER_NOT_READY
        case -4:
            if (DBG) {
                log("handleResult: Error,Phone book is not ready !");
            }
            showStatus(getString(R.string.fdn_errorcode_phb_info));
            mButton.setEnabled(true);
            break;
        //TODO     IccProvider.ERROR_ICC_PROVIDER_PASSWORD_ERROR
        case -5:
            if (DBG) {
                log("handleResult: Error,invalid pin2 !");
            }
            handlePin2Error();
            mButton.setEnabled(true);
            break;
        default:
            if (DBG) {
                log("handleResult: Error,system return unknown error code!");
            }
            mButton.setEnabled(true);
            break;
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

    private View.OnClickListener mClicked = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPinFieldContainer.getVisibility() != View.VISIBLE) {
                return;
            }

        /*
            if (v == mNameField) {
                mNumberField.requestFocus();
            } else 
            if (v == mNumberField) {
                mButton.requestFocus();
            } else 
        */
            if (v == mButton) {
                // Autheticate the pin AFTER the contact information
                // is entered, and if we're not busy.
                if (!mDataBusy) {
                    if (isValidate(getNameFromTextField(), getNumberFromTextField())) {
                        authenticatePin2();
                    }
                }
            }
        }
    };

    private boolean isValidate(String name ,String number) {
        boolean isNameNull = TextUtils.isEmpty(name);
        boolean isNumberNull = TextUtils.isEmpty(number);
        if (isNameNull && isNumberNull) {
            showStatus(getResources().getText(R.string.fdn_contact_name_number_invalid));
            return false;
        }
        name = name.toUpperCase();
        number = number.toUpperCase();
//        if (TextUtils.isEmpty(name)){
//            return false;
//        }
//        if (TextUtils.isEmpty(number)){
//            showStatus(getResources().getText(R.string.fdn_contact_number_empty));
//            return false;
//        }
        int addCharIndex = number.indexOf('+');
        //Ex: +,  +123+324
        if (addCharIndex >= 0 && (number.length() == 1 || 
                number.indexOf('+', addCharIndex + 1) >= 0)) {
            showStatus(getResources().getText(R.string.fdn_contact_number_invalid));
            return false;
        }
        
        int pCharIndex = number.indexOf(','); 
        //Ex: P123, +P123 
        if (pCharIndex == 0 || (pCharIndex == 1 && addCharIndex == 0)) {
            showStatus(getResources().getText(R.string.fdn_contact_number_invalid));
            return false;
        }
        //Ex: (/)
        for (int i = number.length(); i-- > 0;) {
            if (!isValidChar(number.charAt(i))) {
                showStatus(getResources().getText(R.string.fdn_contact_number_invalid));
                return false;
            }
        }
        return true;
    }
    /*
        View.OnFocusChangeListener mOnFocusChangeHandler = new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    TextView textView = (TextView) v;
                    Selection.selectAll((Spannable) textView.getText());
                }
            }
        };
    */

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (DBG) {
                log("onInsertComplete");
            }
            displayProgress(false);
            log("== loyee ==  uri.toString() = " + uri.toString());
            String str = uri.toString();
            int result = 0;
            if (str.indexOf("error") == -1) {
                    result = 1;
            } else {
                str = str.replace("content://icc/error/", "");
                result = Integer.valueOf(str).intValue();
            }
            log("== loyee ==  result=" + result);
            handleResult(Operate.INSERT,result);
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (DBG) {
                log("onUpdateComplete");
            }
            displayProgress(false);
            handleResult(Operate.UPDATE,result);
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
        }
    }

    private void log(String msg) {
        Xlog.d(LOG_TAG, "[EditFdnContact] " + msg);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private class EditFdnContactScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    }
}
