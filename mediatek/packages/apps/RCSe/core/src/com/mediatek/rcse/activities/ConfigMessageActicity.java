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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcse.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;

import com.mediatek.rcse.api.Logger;

import com.orangelabs.rcs.R;

/**
 * This class defined as a transparent activity to display the auto
 * configuration message dialog
 */
public class ConfigMessageActicity extends Activity {

    private static final String TAG = "ConfigMessageActicity";
    public static final String CONFIG_DIALOG_TITLE = "title";
    public static final String CONFIG_DIALOG_MESSAGE = "message";
    public static final String CONFIG_DIALOG_ACCEPT_BUTTON = "accept_button";
    public static final String CONFIG_DIALOG_REJECT_BUTTON = "reject_button";
    public static final String CONFIG_MESSAGE_DIALOG_ACTION = "com.mediatek.rcse.activities.CONFIG_MESSAGE";
    private String mTitle = null;
    private String mMessage = null;
    private boolean mAccept = true;
    private boolean mReject = false;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        this.setContentView(R.layout.auto_config_layout);
        Intent intent = this.getIntent();
        if (intent != null) {
            mTitle = intent.getStringExtra(CONFIG_DIALOG_TITLE);
            mMessage = intent.getStringExtra(CONFIG_DIALOG_MESSAGE);
            mAccept = intent.getBooleanExtra(CONFIG_DIALOG_ACCEPT_BUTTON,true);
            mReject = intent.getBooleanExtra(CONFIG_DIALOG_REJECT_BUTTON, false);
            new ConfigMessageDialog().show(this.getFragmentManager(), TAG);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * This class defined to display the auto configuration message to user
     */
    public class ConfigMessageDialog extends DialogFragment {

        public ConfigMessageDialog() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder dialogBuilder =
                    new AlertDialog.Builder(ConfigMessageActicity.this,
                            AlertDialog.THEME_HOLO_LIGHT);
            if (mMessage != null) {
                dialogBuilder.setMessage(mMessage);
            } else {
                Logger.w(TAG, "onCreateDialog()-mMessage is null");
            }
            if (mTitle != null) {
                dialogBuilder.setTitle(mTitle);
            } else {
                Logger.w(TAG, "onCreateDialog()-mTitle is null");
            }
            if (mAccept) {
                dialogBuilder.setPositiveButton("Accept", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ConfigMessageActicity.this.finish();
                    }
                });
            } else {
                Logger.w(TAG, "onCreateDialog()-mAccept is null");
            }
            if (mReject) {
                dialogBuilder.setNegativeButton("Reject", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ConfigMessageActicity.this.finish();
                    }
                });
            } else {
                Logger.w(TAG, "onCreateDialog()-mReject is null");
            }
            AlertDialog dialog = dialogBuilder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            this.dismissAllowingStateLoss();
            ConfigMessageActicity.this.finish();
        }
    }
}
