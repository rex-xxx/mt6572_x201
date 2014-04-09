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

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;   

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

/**
 * Pin2 entry screen.
 */
public class GetPin2Screen extends Activity implements TextView.OnEditorActionListener {
    private static final String LOG_TAG = "Settings/" + PhoneGlobals.LOG_TAG;
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    private static final int GET_PIN_RETRY_EMPTY = -1;
    private final BroadcastReceiver mReceiver = new GetPin2ScreenBroadcastReceiver();

    private EditText mPin2Field;
    private TextView mPin2Title;
    private TextView mPin2RetryLabel;
    private TextView mPin2InvalidInfoLabel;
    private Button mOkButton;
    
    private int mSimId;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSimId = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        setContentView(R.layout.get_pin2_screen);
       
        setupView();
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        if (mPin2Field != null) {
            mPin2Field.addTextChangedListener(new TextWatcher() {
                CharSequence mTempStr;
                int mStartPos;
                int mEndPos;
                public void afterTextChanged(Editable s) {
                    mStartPos = mPin2Field.getSelectionStart();
                    mEndPos =  mPin2Field.getSelectionEnd();
                    if (mTempStr.length() > MAX_PIN_LENGTH) {
                        s.delete(mStartPos - 1, mEndPos);
                        mPin2Field.setText(s);
                        mPin2Field.setSelection(s.length());
                    } else if (mTempStr.length() >= MIN_PIN_LENGTH) {
                        mPin2InvalidInfoLabel.setVisibility(View.GONE);
                    }
                }
                public void beforeTextChanged(CharSequence s, int start, int count,int after) {
                    mTempStr = s;
                }
                public void onTextChanged(CharSequence s, int start, int before,int count) {
                }
            });
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPin2Field.requestFocus();
        if (getPin2RetryNumber() == 0) {
            finish();
        } else {
            mPin2RetryLabel.setText(getRetryPin2());
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

    /**
     * Reflect the changes in the layout that force the user to open
     * the keyboard. 
     */
    private void setupView() {
        mPin2Field = (EditText) findViewById(R.id.pin);
        if (mPin2Field != null) {
            mPin2Field.setKeyListener(DigitsKeyListener.getInstance());
            //MTK allow the edit text have the cursor
            //mPin2Field.setMovementMethod(null);
            
            //mPin2Field.setOnClickListener(mClicked);
            
        }
        mPin2Field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        mPin2Title = (TextView) findViewById(R.id.get_pin2_title);
        if (mPin2Title != null) {
            mPin2Title.append(getString(R.string.pin_length_indicate));
        }
        mPin2RetryLabel = (TextView) findViewById(R.id.pin2_retry_info_label);
        if (mPin2RetryLabel != null) {
            mPin2RetryLabel.setText(getRetryPin2());
        }
        mPin2InvalidInfoLabel = (TextView) findViewById(R.id.pin2_invalid_info_label);
        mOkButton = (Button) findViewById(R.id.ok);
        mOkButton.setOnClickListener(mClicked);
        
    }
    private int getPin2RetryNumber() {
        String pin2RetryStr;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            switch (mSimId) {
                case PhoneConstants.GEMINI_SIM_1:
                    pin2RetryStr = "gsm.sim.retry.pin2";
                    break;
                case PhoneConstants.GEMINI_SIM_2:
                    pin2RetryStr = "gsm.sim.retry.pin2.2";
                    break;
                case PhoneConstants.GEMINI_SIM_3:
                    pin2RetryStr = "gsm.sim.retry.pin2.3";
                    break;
                case PhoneConstants.GEMINI_SIM_4:
                    pin2RetryStr = "gsm.sim.retry.pin2.4";
                    break;
                default:
                    Xlog.d(LOG_TAG,"Error happened mSimId=" + mSimId);
                    pin2RetryStr = "gsm.sim.retry.puk2";
                    break;    
            }
        } else {
            pin2RetryStr = "gsm.sim.retry.pin2";
        }
        return SystemProperties.getInt(pin2RetryStr, GET_PIN_RETRY_EMPTY);
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

    private String getPin2() {
        return mPin2Field.getText().toString();
    }

    private void returnResult() {
        Bundle map = new Bundle();
        map.putString("pin2", getPin2());

        Intent intent = getIntent();
        Uri uri = intent.getData();

        Intent action = new Intent();
        if (uri != null) {
            action.setAction(uri.toString());
        }
        setResult(RESULT_OK, action.putExtras(map));
        finish();
    }

    private boolean invalidatePin(String pin) {
        // check validity
        return (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH);
    }
    
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            mOkButton.performClick();
            return true;
        }
        return false;
    }


    private Button.OnClickListener mClicked = new Button.OnClickListener() {
        public void onClick(View v) {
            if (invalidatePin(mPin2Field.getText().toString())) {
                if (mPin2InvalidInfoLabel != null) {
                    mPin2InvalidInfoLabel.setVisibility(View.VISIBLE);
                }
                mPin2Field.setText("");
                return;
            }

            returnResult();
        }
    };

    private void log(String msg) {
        Xlog.d(LOG_TAG, "[GetPin2] " + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private class GetPin2ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    }
}
