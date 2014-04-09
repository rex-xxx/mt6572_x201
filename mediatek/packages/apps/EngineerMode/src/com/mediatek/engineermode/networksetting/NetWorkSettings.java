/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.networksetting;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

public class NetWorkSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "EM/NetWorkSettings";
    private static final String IVSR_PREFERENCE_KEY = "ivsr_en_disable";
    private static final String GCRO_PREFERENCE_KEY = "23gcro_switch";
    private static final String GHOO_PREFERENCE_KEY = "23ghoo_switch";
    private static final int CRO_PARA3 = 3;
    private Phone mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final long ivsrCheckValue = Settings.System.getLong(getContentResolver(),
                Settings.System.IVSR_SETTING, Settings.System.IVSR_SETTING_DISABLE);

        final long croCheckValue = Settings.System.getLong(getContentResolver(),
                Settings.System.CRO_SETTING, Settings.System.CRO_SETTING_DISABLE);

        final long hooCheckValue = Settings.System.getLong(getContentResolver(),
                Settings.System.HOO_SETTING, Settings.System.HOO_SETTING_DISABLE);
        mPhone = (Phone) PhoneFactory.getDefaultPhone();

        addPreferencesFromResource(R.xml.network_setting);

        CheckBoxPreference mCheckBoxIvsr = (CheckBoxPreference) findPreference(IVSR_PREFERENCE_KEY);
        if (ivsrCheckValue == 0) {
            mCheckBoxIvsr.setChecked(false);
        } else {
            mCheckBoxIvsr.setChecked(true);
        }
        mCheckBoxIvsr.setOnPreferenceChangeListener(this);

        CheckBoxPreference mCheckBox23Gcro = (CheckBoxPreference) findPreference(GCRO_PREFERENCE_KEY);
        if (croCheckValue == 0) {
            mCheckBox23Gcro.setChecked(false);
        } else {
            mCheckBox23Gcro.setChecked(true);
        }
        mCheckBox23Gcro.setOnPreferenceChangeListener(this);

        CheckBoxPreference mCheckBox23Ghoo = (CheckBoxPreference) findPreference(GHOO_PREFERENCE_KEY);
        if (hooCheckValue == 0) {
            mCheckBox23Ghoo.setChecked(false);
        } else {
            mCheckBox23Ghoo.setChecked(true);
        }
        mCheckBox23Ghoo.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals(IVSR_PREFERENCE_KEY)) {
            Elog.i(TAG, "ivsr->onPreferenceChange(): " + newValue);
            if (newValue.equals(false)) {
                Settings.System.putLong(getContentResolver(), Settings.System.IVSR_SETTING,
                        Settings.System.IVSR_SETTING_DISABLE);
            } else {
                Settings.System.putLong(getContentResolver(), Settings.System.IVSR_SETTING,
                        Settings.System.IVSR_SETTING_ENABLE);
            }
        } else {
            int command = -1;
            if (preference.getKey().equals(GCRO_PREFERENCE_KEY)) {
                Elog.i(TAG, "cro->onPreferenceChange(): " + newValue);
                if (newValue.equals(false)) {
                    Settings.System.putLong(getContentResolver(), Settings.System.CRO_SETTING,
                            Settings.System.CRO_SETTING_DISABLE);
                    command = 0;
                } else {
                    Settings.System.putLong(getContentResolver(), Settings.System.CRO_SETTING,
                            Settings.System.CRO_SETTING_ENABLE);
                    command = 1;
                }
            } else if (preference.getKey().equals(GHOO_PREFERENCE_KEY)) {
                Elog.i(TAG, "hoo->onPreferenceChange(): " + newValue);
                if (newValue.equals(false)) {
                    Settings.System.putLong(getContentResolver(), Settings.System.HOO_SETTING,
                            Settings.System.HOO_SETTING_DISABLE);
                    command = 2;
                } else {
                    Settings.System.putLong(getContentResolver(), Settings.System.HOO_SETTING,
                            Settings.System.HOO_SETTING_ENABLE);
                    command = CRO_PARA3;
                }
            }
            if (mPhone != null && command != -1) {
                Elog.i(TAG, "send command " + command);
                mPhone.setCRO(command, null);
            }
        }
        return true;
    }

}
