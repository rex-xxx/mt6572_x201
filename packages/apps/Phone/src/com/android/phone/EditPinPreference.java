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

package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.CallBarringChangePassword;
import com.mediatek.xlog.Xlog;

/**
 * Class similar to the com.android.settings.EditPinPreference
 * class, with a couple of modifications, including a different layout 
 * for the dialog.
 */
public class EditPinPreference extends EditTextPreference {
    /// M: add log, phone book, sim card values
    private static final String LOG_TAG = "Settings/EditPinPreference";
    public static final int FDN_MODE_FLAG = 10;
    private Phone mPhone;
    private int mMode;
    private int mSimId;
    private boolean mShouldHideButtons;
    
    public interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference preference, boolean positiveResult);
    }
    
    private OnPinEnteredListener mPinListener;

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        mPinListener = listener;
    }
    
    public EditPinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditPinPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    /**
     * Overridden to setup the correct dialog layout, as well as setting up 
     * other properties for the pin / puk entry field.
     */
    @Override
    protected View onCreateDialogView() {
        // set the dialog layout
        setDialogLayoutResource(R.layout.pref_dialog_editpin);
        
        View dialog = super.onCreateDialogView();
        
        /// M: set input type
        // set the transformation method and the key listener to ensure
        // correct input and presentation of the pin / puk.
        final EditText textfield = getEditText();
        //textfield.setTransformationMethod(PasswordTransformationMethod.getInstance());
        //textfield.setKeyListener(DigitsKeyListener.getInstance());
        textfield.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        
        if (this instanceof CallBarringChangePassword) {
            InputFilter filters[] = new InputFilter[1];
            filters[0] = new InputFilter.LengthFilter(4);
            textfield.setFilters(filters);
        }
        
        return dialog;
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        
        // If the layout does not contain an edittext, hide the buttons.
        mShouldHideButtons = (view.findViewById(android.R.id.edit) == null);
    }
    
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        
        // hide the buttons if we need to.
        if (mShouldHideButtons) {
            builder.setPositiveButton(null, this);
            builder.setNegativeButton(null, this);
        }
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mPinListener != null) {
            mPinListener.onPinEntered(this, positiveResult);
        }
    }
    
    /**
     * Externally visible method to bring up the dialog to 
     * for multi-step / multi-dialog requests (like changing 
     * the SIM pin). 
     */
    /// M: pop up reminder dialog
    public void showPinDialog() {
        showDialog(null);
    }

    /// M: pop up reminder dialog
    public void showTipDialog(String title,String msg) {
        Toast.makeText(this.getContext(), msg, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onClick() {
        if (this.getDialog() != null) {
            return ;    
        }    
        switch (mMode) {
        case FDN_MODE_FLAG:
            Xlog.i(LOG_TAG, "onClick, FDN_MODE_FLAG");
            handleFdnModeClick();
            break;
        default:
            showDialog(null);
            break;
        }
    }
    
    public void initFdnModeData(Phone phone,int mode, int simId) {
        mPhone = phone;
        mMode = mode;
        mSimId = simId;
    }
    
    /// M: support gemini phone & judge phone book is ready or  not
    private void handleFdnModeClick() {
        Xlog.i(LOG_TAG, "Enable or Disable the FDN state button is clicked");
        boolean isPhoneBookReady = false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            isPhoneBookReady = ((GeminiPhone)mPhone).getIccCardGemini(mSimId).isPhbReady();
        } else {
            isPhoneBookReady = mPhone.getIccCard().isPhbReady();
        }
        
        Xlog.i(LOG_TAG, "Phone book state from system is :" + isPhoneBookReady);
        if (!isPhoneBookReady) {
            Context context = this.getContext();
            showTipDialog(context.getString(R.string.error_title),context.getString(R.string.fdn_phone_book_busy));
        } else {
            showDialog(null);
        }
    }
}
