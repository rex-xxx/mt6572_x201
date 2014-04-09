/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.android.email.activity;

import com.android.email.AttachmentInfo;
import com.android.email.Email;
import com.android.email.R;
import com.android.email.activity.UiUtilities;
import com.android.emailcommon.utility.Utility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * M: Dialog for renaming the attachment file name
 */
public class RenameAttachmentFileDialog extends DialogFragment
        implements DialogInterface.OnClickListener, TextWatcher {
    private EditText mEditText;
    private AlertDialog mDialog;
    private AttachmentInfo mAttachmentInfo;
    private Callback mCallback;

    private static final String ATTACHMENT_FILE_RENAME_NEWNAME = "attachment_file_rename_newname";
    private static final String INVALID_FILENAME_PATTERN = ".*[/\\\\:*?\"<>|].*";

    public interface Callback {
        public void onOkayClicked(AttachmentInfo info);
    }

    /**
     * M: Creates a new dialog to edit the file name.
     * @param attachmentInfo the attachment info
     * @param callback the callback method
     * @return the rename dialog
     */
    public static RenameAttachmentFileDialog newInstance(
            AttachmentInfo attachmentInfo, Callback callback) {
        final RenameAttachmentFileDialog dialog = new RenameAttachmentFileDialog();
        dialog.mAttachmentInfo = attachmentInfo;
        dialog.mCallback = callback;
        return dialog;
    }

    /**
     * M: Is the file name valid
     * @param fileName the name of file
     * @return true if the filename valid, otherwise false.
     */
    public static boolean isFileNameValid(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        return !fileName.toString().matches(INVALID_FILENAME_PATTERN);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View view = getActivity().getLayoutInflater().inflate(R.layout.attachment_file_rename,
                null);

        mEditText = (EditText) view.findViewById(R.id.rename_edittext);
        if (savedInstanceState != null) {
            String newName =
                    savedInstanceState.getString(ATTACHMENT_FILE_RENAME_NEWNAME);
            if (newName != null) {
                mEditText.setText(newName);
            }
        } else if (mAttachmentInfo != null) {
            mEditText.setText(mAttachmentInfo.mName);
        }
        mEditText.setSelection(mEditText.length());
        mEditText.addTextChangedListener(this);
        UiUtilities.setupLengthFilter(mEditText, context, Email.EDITVIEW_MAX_LENGTH_1, true);
        builder.setTitle(getResources().getString(R.string.rename))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setView(view)
                .setNegativeButton(R.string.cancel_action, this)
                .setPositiveButton(R.string.okay_action, this);
        mDialog = builder.create();
        return mDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        if (mEditText.length() <= 0 || !isFileNameValid(mEditText.getText().toString())) {
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    // implements TextWatcher
    @Override
    public void afterTextChanged(Editable s) {
        if (s == null || s.toString().length() <= 0 || !isFileNameValid(s.toString())) {
            // characters not allowed
            if (!isFileNameValid(s.toString())) {
                Utility.showToastShortTime(getActivity(), R.string.invalid_char_prompt);
            }
            Button botton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (botton != null) {
                botton.setEnabled(false);
            }
        } else {
            Button botton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (botton != null) {
                botton.setEnabled(true);
            }
        }
    }

    // implements TextWatcher
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    // implements TextWatcher
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    // Saves contents during orientation change
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(
                ATTACHMENT_FILE_RENAME_NEWNAME, mEditText.getText().toString());
    }

    /**
     * Implements DialogInterface.OnClickListener
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.cancel();
                break;
            case DialogInterface.BUTTON_POSITIVE:
                mAttachmentInfo.mName = mEditText.getText().toString();
                mCallback.onOkayClicked(mAttachmentInfo);
                break;
        }
    }
}
