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

package com.mediatek.engineermode;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

public class MobileDataPreferred extends PreferenceActivity {

    private static final String TAG = "EM/CallDataPreferred";
    private CheckBoxPreference mMobileDataPref;
    private static final String DATA_PREFER_KEY = "data_prefer_key";

    public static final int PCH_DATA_PREFER = 0;
    public static final int PCH_CALL_PREFER = 1;
    private static final int MOBILE_DATA_PREF_DIALOG = 10;

    private ITelephony mTelephony;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.mobile_data_prefer);
        addPreferencesFromResource(R.xml.gsm_umts_options);
        
        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        // / M: add mobile data prefer preference @{
        if (mTelephony == null) {
            mTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
        mMobileDataPref = (CheckBoxPreference)prefSet.findPreference(DATA_PREFER_KEY);
        //mMobileDataPref.setOnPreferenceClickListener(this);
        int pchFlag = Settings.System.getInt(this.getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING,
                        Settings.System.GPRS_TRANSFER_SETTING_DEFAULT);
        Xlog.v(TAG, "orgin value Settings.System.GPRS_TRANSFER_SETTING = " + pchFlag);
        
        mMobileDataPref.setChecked(pchFlag == 0 ? true : false);
        // / @}

    }

    // / M: show dialogs
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == MOBILE_DATA_PREF_DIALOG) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.pch_data_prefer_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Settings.System.putInt(getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING,0);
                                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                    if (mTelephony != null) {
                                        mTelephony.setGprsTransferTypeGemini(PCH_DATA_PREFER, PhoneConstants.GEMINI_SIM_1);
                                        mTelephony.setGprsTransferTypeGemini(PCH_DATA_PREFER, PhoneConstants.GEMINI_SIM_2);
                                    }
                                } else {
                                    if (mTelephony != null) {
                                        mTelephony.setGprsTransferType(PCH_DATA_PREFER);
                                    }
                                }
                            } catch (RemoteException e) {
                                Xlog.v(TAG, e.getMessage());
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mMobileDataPref.setChecked(false);
                        }
                    })
                    .create();
        }
        return dialog;
    }
    
    
    
    
    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation. Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

//        if (preference == mMobileDataPref) {
        if (mMobileDataPref.equals(preference)) {
            if (mMobileDataPref.isChecked()) {
                showDialog(MOBILE_DATA_PREF_DIALOG);
            } else {
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING, 1);
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        if (mTelephony != null) {
                             mTelephony.setGprsTransferTypeGemini(PCH_CALL_PREFER, PhoneConstants.GEMINI_SIM_1);
                             mTelephony.setGprsTransferTypeGemini(PCH_CALL_PREFER, PhoneConstants.GEMINI_SIM_2);
                        }
                    } else {
                        if (mTelephony != null) {
                            mTelephony.setGprsTransferType(PCH_CALL_PREFER);
                        }
                    }
                } catch (RemoteException e) {
                    Xlog.v(TAG, e.getMessage());
                }
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
