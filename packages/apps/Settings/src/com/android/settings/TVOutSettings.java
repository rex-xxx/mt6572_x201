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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.tvout.ITVOUTNative;
import com.mediatek.xlog.Xlog;

public class TVOutSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "TV_OUT";
    private static final String PROFILE_TV_OUT_SETTINGS = "tv_out";

    private static final String KEY_TVOUT_ENABLE = "tvout_enable";
    private static final String KEY_TV_SYSTEM = "tv_system";

    private static boolean sIsFirst = true;
    private static boolean sIsFirstFire = true;
    private CheckBoxPreference mEnablePreference;
    private ListPreference mTVSystem;
    private ITVOUTNative mTvOut = null;
    private static boolean sNeedUserEnable = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tvout_settings);
        if (mTvOut == null) {
            mTvOut = MediatekClassFactory
                    .createInstance(ITVOUTNative.class);
        }
        mEnablePreference = (CheckBoxPreference) findPreference(KEY_TVOUT_ENABLE);
        mTVSystem = (ListPreference) findPreference(KEY_TV_SYSTEM);

        sNeedUserEnable = mTvOut.isShowButton();
        if (sNeedUserEnable) {
            mEnablePreference.setOnPreferenceChangeListener(this);
            Xlog.i(TAG, "[TVOUT] enable button ");
        }
        mTVSystem.setOnPreferenceChangeListener(this);

        if (sIsFirst) {
            Xlog.i(TAG, "First launch");
            mEnablePreference.setChecked(false);
            sIsFirst = false;
        }

        PreferenceScreen root = this.getPreferenceScreen();

        if (!sNeedUserEnable) {
            root.removePreference(mEnablePreference);
            Xlog.i(TAG, "[TVOUT] no enable button ,remove");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (sNeedUserEnable) {
            if (KEY_TVOUT_ENABLE.equals(key)) {
                if (objValue.equals(false)) {
                    mTvOut.enableTvOut(false);
                } else {
                    if (sIsFirstFire) {
                        String sys = mTVSystem.getValue();
                        Xlog.i(TAG, "First enable, system type is:" + sys);
                        if (sys.equals("NTSC")) {
                            mTvOut.setTvSystem(ITVOUTNative.NTSC);
                        } else {
                            mTvOut.setTvSystem(ITVOUTNative.PAL);
                        }
                        sIsFirstFire = false;
                    }
                    mTvOut.enableTvOut(true);
                }
            }
        }
        if (KEY_TV_SYSTEM.equals(key)) {
            SharedPreferences setting = getActivity().getSharedPreferences(
                    PROFILE_TV_OUT_SETTINGS, Context.MODE_PRIVATE);
            setting.edit().putString(KEY_TV_SYSTEM, objValue.toString())
                    .commit();
            if (objValue.equals("NTSC")) {
                mTvOut.setTvSystem(ITVOUTNative.NTSC);
            } else {
                mTvOut.setTvSystem(ITVOUTNative.PAL);
            }
            mTVSystem.setSummary(objValue.toString());
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        SharedPreferences setting = getActivity().getSharedPreferences(
                PROFILE_TV_OUT_SETTINGS, Context.MODE_PRIVATE);
        String tvSystem = setting.getString(KEY_TV_SYSTEM, "NTSC");
        Xlog.i(TAG, "onResume, tv system is:" + tvSystem);
        mTVSystem.setValue(tvSystem);
        mTVSystem.setSummary(tvSystem);
        super.onResume();
    }

}
