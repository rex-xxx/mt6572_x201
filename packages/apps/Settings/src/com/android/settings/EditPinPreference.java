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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.mediatek.xlog.Xlog;

/**
 * TODO: Add a soft dialpad for PIN entry.
 */
class EditPinPreference extends EditTextPreference {

    // /M: add for new feature
    private AlertDialog mAlertDialog;
    private static final int MINPINNUM = 4;
    private static final int MAXPINNUM = 8;
    private static final String TAG = "EditTextPreference";

    interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference preference, boolean positiveResult);
    }

    private OnPinEnteredListener mPinListener;

    public EditPinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditPinPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        mPinListener = listener;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final EditText editText = getEditText();

        if (editText != null) {
            // /M: add for new feature
            editText.setSingleLine(true);
            /** M: add to deal with pin becoming plaintext when orientation changes.  @{ */
            //editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            //editText.setKeyListener(DigitsKeyListener.getInstance());
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            Xlog.d(TAG, "add deal with orientation change");
            /** @} */
            editText.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    if (mAlertDialog != null) {
                        if (s.length() >= MINPINNUM && s.length() <= MAXPINNUM) {
                            mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(true);
                        } else {
                            mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(false);
                        }
                    }
                }

                public void beforeTextChanged(CharSequence s, int start,
                        int count, int after) {

                }

                public void onTextChanged(CharSequence s, int start,
                        int before, int count) {

                }

            });
        }
    }

    public boolean isDialogOpen() {
        Dialog dialog = getDialog();
        return dialog != null && dialog.isShowing();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mPinListener != null) {
            mPinListener.onPinEntered(this, positiveResult);
        }
    }

    public void showPinDialog() {
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            showDialog(null);
        }
    }

    // /M: add by MTK to solve bug
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        Dialog dialog = getDialog();
        if (dialog != null) {
            Xlog.d(TAG, "showDialog");
            mAlertDialog = (AlertDialog) dialog;
            EditText editText = (EditText) mAlertDialog
                    .findViewById(android.R.id.edit);
            if (editText != null) {
                int length = editText.getText().toString().length();
                Xlog.d(TAG, "editText=" + editText.getText().toString()
                        + " length of text=" + length);
                if (length >= MINPINNUM && length <= MAXPINNUM) {
                    mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(true);
                } else {
                    mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(false);
                }
            }
        }
    }

}
