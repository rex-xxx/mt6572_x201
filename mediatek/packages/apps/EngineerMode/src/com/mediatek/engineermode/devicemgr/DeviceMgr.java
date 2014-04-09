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

package com.mediatek.engineermode.devicemgr;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.mediatek.common.dm.DMAgent;
import com.mediatek.engineermode.R;
import com.mediatek.xlog.Xlog;

public class DeviceMgr extends PreferenceActivity
        implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_SMS_AUTO_REG = "sms_auto_reg";
    private static final String STR_DMAGENT = "DMAgent";
    private static final String STR_ENABLED = "Enabled";
    private static final String STR_DISABLED = "Disabled";
    private ListPreference mListPreferSmsAutoReg;
    private DMAgent mAgent;
    private static final String TAG = "EM/devmgr";

    /**
     * Called when the activity is first created.
     * @param savedInstanceState
     *            : the bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.devicemgr);

        IBinder binder = ServiceManager.getService(STR_DMAGENT);
        mAgent = DMAgent.Stub.asInterface(binder);

        mListPreferSmsAutoReg =
            (ListPreference) findPreference(KEY_SMS_AUTO_REG);

        mListPreferSmsAutoReg.setOnPreferenceChangeListener(this);
        final int savedCTA = getSavedCTA();
        final String summary = savedCTA == 1 ? STR_ENABLED : STR_DISABLED;
        mListPreferSmsAutoReg.setSummary(summary);
        mListPreferSmsAutoReg.setValue(String.valueOf(savedCTA));

    }

    /**
     * on preference changed
     * 
     * @param preference
     *            : selected preference
     * @param objValue
     *            : selected preference value
     * @return whether preference changed success
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {

        final String key = preference.getKey();

        if (KEY_SMS_AUTO_REG.equals(key)) {
            try {
                setSavedCTA((String) objValue);
            } catch (NumberFormatException e) {
                Xlog.e(TAG, "set exception. ", e);
            }
            final boolean isEnabled = getSavedCTA() == 1;
            mListPreferSmsAutoReg.setValue(isEnabled ? "1" : "0");
            final String summary = isEnabled ? STR_ENABLED : STR_DISABLED;
            mListPreferSmsAutoReg.setSummary(summary);
        }
        return false;
    }

    private int getSavedCTA() {
        if (mAgent == null) {
            Xlog.e(TAG, "get CTA failed, agent is null!");
            return 0;
        }
        int savedCTA = 0;
        try {
            byte[] cta = mAgent.readCTA();
            if (cta != null) {
                savedCTA = Integer.parseInt(new String(cta));
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "get cta cmcc switch failed, readCTA failed!");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            Xlog.e(TAG, "number format exception. ", e);
        }
        Xlog.i(TAG, "Get savedCTA = [" + savedCTA + "]");
        return savedCTA;
    }

    private void setSavedCTA(String cta) {
        if (mAgent == null) {
            Xlog.e(TAG, "save CTA switch value failed, agent is null!");
            return;
        }
        try {
            mAgent.writeCTA(cta.getBytes());
        } catch (RemoteException e) {
            Xlog.e(TAG, "save CTA switch failed, writeCTA failed!");
            e.printStackTrace();
        }
        Xlog.i(TAG, "save CTA [" + cta + "]");
    }
}
