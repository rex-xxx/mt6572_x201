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

package com.android.settings;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Toast;

import com.mediatek.drm.OmaDrmClient;
import com.mediatek.xlog.Xlog;

public class DrmSettings extends SettingsPreferenceFragment {
    private static final String TAG = "DrmSettings";
    private static final String DRM_RESET = "drm_settings";
    private static Preference sPreferenceReset;
    private static final int DIALOG_RESET = 1000;
    private static OmaDrmClient sClient;
    private Context mContext;
    private View mContentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.drm_settings);

        sPreferenceReset = findPreference(DRM_RESET);
        mContext = getActivity();
        sClient = new OmaDrmClient(mContext);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Builder builder = new AlertDialog.Builder(mContext);
        AlertDialog dialog;
        switch (id) {
        case DIALOG_RESET:
            builder.setMessage(getResources().getString(
                    R.string.drm_reset_dialog_msg));
            builder.setTitle(getResources().getString(
                    R.string.drm_settings_title));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            if (sClient != null) {
                                int result = sClient.removeAllRights();
                                if (result == sClient.ERROR_NONE) {
                                    Toast.makeText(mContext,
                                            R.string.drm_reset_toast_msg,
                                            Toast.LENGTH_SHORT).show();
                                    sPreferenceReset.setEnabled(false);
                                } else {
                                    Xlog.i(TAG, "removeAllRights fail!");
                                }
                                sClient = null;
                            }
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            dialog = builder.create();
            return dialog;
        default:
            return null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == sPreferenceReset) {
            showDialog(DIALOG_RESET);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sClient = null;
    }

}
