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
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.settings;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.android.phone.ChangeIccPinScreen;
import com.android.phone.EditPinPreference;
import com.android.phone.FdnList;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.List;
/**
 * FDN settings UI for the Phone app.
 * Rewritten to look and behave closer to the other preferences.
 */
public class FdnSetting2 extends PreferenceActivity
        implements EditPinPreference.OnPinEnteredListener, Preference.OnPreferenceClickListener {

    private Phone mPhone;

    private int mSimId;
    private int mRetryPin2Old;
    private int mRetryPin2New;
    /**
     * Events we handle.
     * The first is used for toggling FDN enable, the second for the PIN change.
     */
    private static final int EVENT_PIN2_ENTRY_COMPLETE = 100;
    private static final String LOG_TAG = "Settings/FdnSetting2";


    // String keys for preference lookup
    // We only care about the pin preferences here, the manage FDN contacts
    // Preference is handled solely in xml.
    private static final String BUTTON_FDN_ENABLE_KEY = "button_fdn_enable_key";
    private static final String BUTTON_CHANGE_PIN2_KEY = "button_change_pin2_key";
    private static final String BUTTON_FDN_LIST_KEY = "button_fdn_list_key";

    private EditPinPreference mButtonEnableFDN;
    private Preference mButtonChangePin2;
    private Preference mButtonFDNList;
    private boolean mFdnSupport = true;

    // size limits for the pin.
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    private static final int GET_SIM_RETRY_EMPTY = -1;
    
    private final BroadcastReceiver mReceiver = new FdnSetting2BroadcastReceiver();
    private int getRetryPuk2Count() {
        String puk2RetryStr;
        if (CallSettings.isMultipleSim()) {
            switch (mSimId) {
                case PhoneConstants.GEMINI_SIM_1:
                puk2RetryStr = "gsm.sim.retry.puk2";
                break;
                case PhoneConstants.GEMINI_SIM_2:
                puk2RetryStr = "gsm.sim.retry.puk2.2";
                break;
                case PhoneConstants.GEMINI_SIM_3:
                puk2RetryStr = "gsm.sim.retry.puk2.3";
                break;
                case PhoneConstants.GEMINI_SIM_4:
                puk2RetryStr = "gsm.sim.retry.puk2.4";
                break;
                default:
                Xlog.d(LOG_TAG,"Error happened mSimId=" + mSimId);
                puk2RetryStr = "gsm.sim.retry.puk2";
                break;
            }
        } else {
            puk2RetryStr = "gsm.sim.retry.puk2";
        }
        return SystemProperties.getInt(puk2RetryStr, GET_SIM_RETRY_EMPTY);
    }

    private int getRetryPin2Count() {
        String pin2RetryString;
        if (CallSettings.isMultipleSim()) {
            switch (mSimId) {
                case PhoneConstants.GEMINI_SIM_1:
                pin2RetryString = "gsm.sim.retry.pin2";
                break;
                case PhoneConstants.GEMINI_SIM_2:
                pin2RetryString = "gsm.sim.retry.pin2.2";
                break;
                case PhoneConstants.GEMINI_SIM_3:
                pin2RetryString = "gsm.sim.retry.pin2.3";
                break;
                case PhoneConstants.GEMINI_SIM_4:
                pin2RetryString = "gsm.sim.retry.pin2.4";
                break;
                default:
                Xlog.d(LOG_TAG,"Error happened mSimId=" + mSimId);
                pin2RetryString = "gsm.sim.retry.pin2";
                break;
            }
        } else {
            pin2RetryString = "gsm.sim.retry.pin2";
        }
        return SystemProperties.getInt(pin2RetryString,GET_SIM_RETRY_EMPTY);
    }

    private String getRetryPin2() {
        int retryCount = getRetryPin2Count();
        mRetryPin2New = retryCount;
        Xlog.d(LOG_TAG, "getRetryPin2 =" + retryCount);
        switch (retryCount) {
        case GET_SIM_RETRY_EMPTY:
            Xlog.d(LOG_TAG, "getRetryPin2,GET_SIM_RETRY_EMPTY");
            return " ";
        case 1:
            return "(" + getString(R.string.one_retry_left) + ")";
        default:
            return "(" + getString(R.string.retries_left,retryCount) + ")";
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        Xlog.i(LOG_TAG, preference.getKey());
        if (preference == mButtonChangePin2) {
            Intent intent = new Intent();
            intent.putExtra("pin2", true);
            if (mSimId >= 0) {
                intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            }
            intent.setClass(this, ChangeIccPinScreen.class);
            startActivity(intent);
        }
        
        if (preference == mButtonFDNList) {
            Xlog.i(LOG_TAG, "onPreferenceClick mButtonFDNList");
            if (!checkPhoneBookState()) {
                return false;
            }
            Intent intent = new Intent(this, FdnList.class);
            if (mSimId >= 0) {
                intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            }
            startActivity(intent);
        }
        return true;
    }

    /**
     * Delegate to the respective handlers.
     */
    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (preference == mButtonEnableFDN) {
            Xlog.d(LOG_TAG, "onPinEntered");
            toggleFDNEnable(positiveResult);
        }
    }

    private void resetFDNDialog(int strId) {
        if (strId != 0) {
            mButtonEnableFDN.setDialogMessage(getString(strId) + "\n"
                    + getString(R.string.enter_pin2_text) + "\n" 
                    + getRetryPin2());
        } else {
            Xlog.d(LOG_TAG, "resetFDNDialog 0");
            mButtonEnableFDN.setDialogMessage(getString(R.string.enter_pin2_text) + "\n" 
                    + getRetryPin2());
        }
    }

    /**
     * Attempt to toggle FDN activation.
     */
    private void toggleFDNEnable(boolean positiveResult) {
        Xlog.d(LOG_TAG, "toggleFDNEnable" + positiveResult);
    
        if (!positiveResult) {
                Xlog.d(LOG_TAG, "toggleFDNEnable positiveResult is false");
            resetFDNDialog(0);
            mRetryPin2Old = mRetryPin2New;
            Xlog.d(LOG_TAG, "toggleFDNEnable mRetryPin2Old=" + mRetryPin2Old);
            return;
        }

        // validate the pin first, before submitting it to the RIL for FDN enable.
        String password = mButtonEnableFDN.getText();
        if (validatePin(password, false)) {
            // get the relevant data for the icc call
            IccCard iccCard;
            if (CallSettings.isMultipleSim()) {
                iccCard = ((GeminiPhone)mPhone).getIccCardGemini(mSimId);
            } else {
                iccCard = mPhone.getIccCard();
            }
            boolean isEnabled = iccCard.getIccFdnEnabled();
            Message onComplete = mFDNHandler.obtainMessage(EVENT_PIN2_ENTRY_COMPLETE);

            // make fdn request
            iccCard.setIccFdnEnabled(!isEnabled, password, onComplete);
        } else {
            // throw up error if the pin is invalid.
            resetFDNDialog(R.string.invalidPin2);
            mButtonEnableFDN.setText("");
            mButtonEnableFDN.showPinDialog();
        }

        mButtonEnableFDN.setText("");
    }

    /**
     * Handler for asynchronous replies from the sim.
     */
    private Handler mFDNHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // when we are enabling FDN, either we are unsuccessful and display
                // a toast, or just update the UI.
            case EVENT_PIN2_ENTRY_COMPLETE:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    if (getRetryPin2Count() == 0) {
                        displayMessage(R.string.puk2_requested);
                        Xlog.d(LOG_TAG, "EVENT_PIN2_ENTRY_COMPLETE,puk2_requested");
                        updateFDNPreference();
                    } else {
                        resetFDNDialog(R.string.pin2_invalid);
                        mButtonEnableFDN.showPinDialog();
                    }
                } else {
                    updateEnableFDN();
                }
                Xlog.d(LOG_TAG, "EVENT_PIN2_ENTRY_COMPLETE");
                mRetryPin2Old = mRetryPin2New;
                Xlog.d(LOG_TAG, "EVENT_PIN2_ENTRY_COMPLETE mRetryPin2Old=" + mRetryPin2Old);
                break;
            default:
                break;
            }
        }
    };

    /**
     * Display a toast for message, like the rest of the settings.
     */
    private void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT)
            .show();
    }

    /**
     * Validate the pin entry.
     *
     * @param pin This is the pin to validate
     * @param isPuk Boolean indicating whether we are to treat
     * the pin input as a puk.
     */
    private boolean validatePin(String pin, boolean isPUK) {

        // for pin, we have 4-8 numbers, or puk, we use only 8.
        int pinMinimum = isPUK ? MAX_PIN_LENGTH : MIN_PIN_LENGTH;

        // check validity
        return !(pin == null || pin.length() < pinMinimum || pin.length() > MAX_PIN_LENGTH);
    }

    /**
     * Reflect the updated FDN state in the UI.
     */
    private void updateEnableFDN() {
        IccCard iccCard;
        if (CallSettings.isMultipleSim()) {
            iccCard = ((GeminiPhone)mPhone).getIccCardGemini(mSimId);
        } else {
            iccCard = mPhone.getIccCard();
        }
        if (iccCard.getIccFdnEnabled()) {
            Xlog.d(LOG_TAG, "updateEnableFDN is FdnEnabled=" + R.string.disable_fdn);
            mButtonEnableFDN.setTitle(R.string.enable_fdn_ok);
            mButtonEnableFDN.setSummary(R.string.fdn_enabled);
            mButtonEnableFDN.setDialogTitle(R.string.disable_fdn);
        } else {
            Xlog.d(LOG_TAG, "updateEnableFDN is not FdnEnabled=" + R.string.enable_fdn);
            mButtonEnableFDN.setTitle(R.string.disable_fdn_ok);
            mButtonEnableFDN.setSummary(R.string.fdn_disabled);
            mButtonEnableFDN.setDialogTitle(R.string.enable_fdn);
        }
        Xlog.d(LOG_TAG, "updateEnableFDN");        
        resetFDNDialog(0);
    }

    private void updateFDNPreference() {
        GeminiPhone dualPhone = null;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (mPhone instanceof GeminiPhone) {
                dualPhone = (GeminiPhone)mPhone;
            }
            if (!dualPhone.isRadioOnGemini(mSimId)) {
                mButtonEnableFDN.setEnabled(false);
                mButtonFDNList.setEnabled(false);
                mButtonChangePin2.setEnabled(false);
             } else {        
                if (getRetryPin2Count() == 0) {
                    mRetryPin2New = 0;
                    Xlog.d(LOG_TAG, "updateFDNPreference, mRetryPin2New=" + mRetryPin2New);
                    mButtonEnableFDN.setEnabled(false);
                    mButtonFDNList.setEnabled(false);
                    mButtonChangePin2.setTitle(R.string.unblock_pin2);

                    if (getRetryPuk2Count() == 0) {
                        mButtonChangePin2.setEnabled(false);
                        mButtonChangePin2.setSummary(R.string.sim_permanently_locked);
                    } else {
                        mButtonChangePin2.setEnabled(true);
                        mButtonChangePin2.setSummary(R.string.puk_requested);
                    }
                    updateEnableFDN();
                } else {
                    Xlog.d(LOG_TAG, "updateFDNPreference");
                    mButtonEnableFDN.setEnabled(mFdnSupport);
                    mButtonChangePin2.setEnabled(true);
                    mButtonChangePin2.setTitle(R.string.change_pin2);
                    mButtonChangePin2.setSummary(R.string.sum_fdn_change_pin);
                    mButtonFDNList.setEnabled(mFdnSupport);
                    updateEnableFDN();
                }
            }
        } else {
             ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
             Xlog.i(LOG_TAG,"phone is " + phone);
             try {
                if (null != phone && !phone.isRadioOn()) {
                    mButtonEnableFDN.setEnabled(false);
                    mButtonFDNList.setEnabled(false);
                    mButtonChangePin2.setEnabled(false);
                } else {        
                    if (getRetryPin2Count() == 0) {
                        mRetryPin2New = 0;
                        Xlog.d(LOG_TAG, "single updateFDNPreference, mRetryPin2New=" + mRetryPin2New);
                        mButtonEnableFDN.setEnabled(false);
                        mButtonFDNList.setEnabled(false);
                        mButtonChangePin2.setTitle(R.string.unblock_pin2);
                        if (getRetryPuk2Count() == 0) {
                            mButtonChangePin2.setEnabled(false);
                            mButtonChangePin2.setSummary(R.string.sim_permanently_locked);
                        } else {
                            mButtonChangePin2.setEnabled(true);
                            mButtonChangePin2.setSummary(R.string.puk_requested);
                        }
                        updateEnableFDN();
                    } else {
                        Xlog.d(LOG_TAG, "single updateFDNPreference");
                        mButtonEnableFDN.setEnabled(mFdnSupport);
                        mButtonChangePin2.setEnabled(true);
                        mButtonChangePin2.setTitle(R.string.change_pin2);
                        mButtonChangePin2.setSummary(R.string.sum_fdn_change_pin);
                        mButtonFDNList.setEnabled(mFdnSupport);
                        updateEnableFDN();
                    }
                }
            } catch (RemoteException e) {
                 Xlog.i(LOG_TAG, "single phone exception ");
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.fdn_setting2);

        mPhone = PhoneGlobals.getPhone();
        mSimId = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        Xlog.d(LOG_TAG, "onCreate,Sim Id " + mSimId);
        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonEnableFDN = (EditPinPreference) prefSet.findPreference(BUTTON_FDN_ENABLE_KEY);
        mButtonChangePin2 = prefSet.findPreference(BUTTON_CHANGE_PIN2_KEY);
        mButtonFDNList = prefSet.findPreference(BUTTON_FDN_LIST_KEY);
        //assign click listener and update state
        if (null != mButtonEnableFDN) {
            mButtonEnableFDN.setOnPinEnteredListener(this);
            mButtonEnableFDN.getEditText().addTextChangedListener(new TextWatcher() {
                CharSequence mTempStr;
                int mStartPos;
                int mEndPos;
                public void afterTextChanged(Editable s) {
                    mStartPos = mButtonEnableFDN.getEditText().getSelectionStart();
                    mEndPos = mButtonEnableFDN.getEditText().getSelectionEnd();
                    if (mTempStr.length() > MAX_PIN_LENGTH) {
                        s.delete(mStartPos - 1, mEndPos);
                        mButtonEnableFDN.getEditText().setText(s);
                        mButtonEnableFDN.getEditText().setSelection(s.length());
                    }
                }
                public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                    mTempStr = s;
                }
                public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                }
            });
        }
        if (null != mButtonChangePin2) {
            mButtonChangePin2.setOnPreferenceClickListener(this);        
        }
        if (null != mButtonFDNList) {
            mButtonFDNList.setOnPreferenceClickListener(this);
        }
        IntentFilter intentFilter =
            new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            intentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        ///M: add for hot swap {
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        ///@}

        registerReceiver(mReceiver, intentFilter);
        mButtonEnableFDN.initFdnModeData(mPhone, EditPinPreference.FDN_MODE_FLAG, mSimId);
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME)) {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }
        if (icicle != null) {
            updateFDNPreference();
        }
        mRetryPin2Old = getRetryPin2Count();
        Xlog.d(LOG_TAG, "onCreate,  mRetryPin2Old=" + mRetryPin2Old);
        checkPhoneBookStateExit();
        setFDNSupport();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPhone = PhoneGlobals.getPhone();
        
        Xlog.d(LOG_TAG, "onResume");
        updateFDNPreference();
        Dialog fdnDialog = mButtonEnableFDN.getDialog();

        Xlog.d(LOG_TAG, "onResume, mRetryPin2New= " + mRetryPin2New
                + " mRetryPin2Old=" + mRetryPin2Old);
      
        if (mRetryPin2New != mRetryPin2Old) {
            mRetryPin2Old = mRetryPin2New;
            Xlog.d(LOG_TAG, "onResume, fdnDialog= " + fdnDialog);
            if (fdnDialog != null) {
                Xlog.d(LOG_TAG, "onResume, fdnDialog.isShowing()=" + fdnDialog.isShowing());
            }
            Xlog.d(LOG_TAG, "onResume, second mRetryPin2New= " + mRetryPin2New
                    + " mRetryPin2Old=" + mRetryPin2Old);

            if (fdnDialog != null && fdnDialog.isShowing()) {
               Xlog.d(LOG_TAG, "onResume, isShowing");
               mButtonEnableFDN.getDialog().dismiss();
            }
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
    protected void onDestroy() {
        Xlog.d(LOG_TAG, "onDestroy");
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void setFDNSupport() {
        IccCard iccCard;
        if (CallSettings.isMultipleSim()) {
            iccCard = ((GeminiPhone)mPhone).getIccCardGemini(mSimId);
        } else {
            iccCard = mPhone.getIccCard();
        }
        mFdnSupport = iccCard.isFDNExist();
        mButtonEnableFDN.setEnabled(mFdnSupport);
        mButtonFDNList.setEnabled(mFdnSupport);
    }

    private void checkPhoneBookStateExit() {
        boolean isPhoneBookReady = false;
        if (CallSettings.isMultipleSim()) {
            isPhoneBookReady = ((GeminiPhone)mPhone).getIccCardGemini(mSimId).isPhbReady();
        } else {
            isPhoneBookReady = mPhone.getIccCard().isPhbReady();
        }
        if (!isPhoneBookReady) {
            showTipToast(getString(R.string.error_title),getString(R.string.fdn_phone_book_busy));
            finish();
        }
    }
    
    private boolean checkPhoneBookState() {
        boolean isPhoneBookReady = false;
        if (CallSettings.isMultipleSim()) {
            isPhoneBookReady = ((GeminiPhone)mPhone).getIccCardGemini(mSimId).isPhbReady();
        } else {
            isPhoneBookReady = mPhone.getIccCard().isPhbReady();
        }
        if (!isPhoneBookReady) {
            showTipToast(getString(R.string.error_title),getString(R.string.fdn_phone_book_busy));
        }
        return isPhoneBookReady;
    }
    
    public void showTipToast(String title, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private class FdnSetting2BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean bDisable = intent.getBooleanExtra("state", false);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    bDisable = bDisable ||
                    (Settings.System.getInt(context.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1) == 0);
                }

                 updateMenuEnableState(!bDisable);

            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                boolean bDisable = (Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                   || (intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1) == 0);
                
                updateMenuEnableState(!bDisable);             
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(context);
                // all sim card is pluged out {
                if (temp.size() == 0) {
                    Xlog.d(LOG_TAG, "Activity finished");
                    CallSettings.goToOthersSettings(FdnSetting2.this);
                } else if (CallSettings.isMultipleSim() && temp.size() == 1) {
                    if (temp.get(0).mSlot != mSimId) {
                        Xlog.d(LOG_TAG, "temp.size()=" + temp.size()
                                + "Activity finished");
                        CallSettings.goToOthersSettings(FdnSetting2.this);
                    }
                }
                ///@}
            }
        }
    }
    
    private void updateMenuEnableState(boolean bEnable) {
        mButtonEnableFDN.setEnabled(mFdnSupport && bEnable);
        mButtonFDNList.setEnabled(mFdnSupport &&  bEnable);
        mButtonChangePin2.setEnabled(bEnable);   
    }
}
