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

package com.mediatek.engineermode.digitalstandard;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

public class NetworkModeSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "NetworkModeSettings";
    private NetworkModePreference mNetworkMode;
    private static final String KEY_NETWORK_MODE_SETTING = "gsm_umts_preferred_network_mode_key";
    private static final int PREFERRED_NT_MODE = Phone.PREFERRED_NT_MODE;
    private Phone mGeminiPhone;

    public static final String OLD_NETWORK_MODE = "com.android.phone.OLD_NETWORK_MODE";
    public static final String NETWORK_MODE_CHANGE_BROADCAST = "com.android.phone.NETWORK_MODE_CHANGE";
    public static final String NETWORK_MODE_CHANGE_RESPONSE = "com.android.phone.NETWORK_MODE_CHANGE_RESPONSE";

    private static final int DIALOG_NETWORK_MODE_CHANGE = 1008;

    private final BroadcastReceiver mSimReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {

                int slotId = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
                int simStatus = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, -1);
                Elog.i(TAG, "receive notification of  " + "sim slot = " + slotId + " status = " + simStatus);

                if ((slotId == PhoneConstants.GEMINI_SIM_1) && (simStatus >= 0)) {
                    mNetworkMode.setStatus(simStatus);
                }
            } else if (action.equals(Utils.NETWORK_MODE_CHANGE_RESPONSE)) {

                if (intent.getBooleanExtra(Utils.NETWORK_MODE_CHANGE_RESPONSE, true)) {
                    Elog.d(TAG, "BroadcastReceiver: network mode change succeed!" + " set the new value.");
                    android.provider.Settings.Global.putInt(getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE, intent.getIntExtra(
                                    Utils.NEW_NETWORK_MODE, 0));
                } else {
                    Elog.d(TAG, "BroadcastReceiver: network mode change failed!" + " restore the old value.");
                    android.provider.Settings.Global
                            .putInt(getContentResolver(), android.provider.Settings.Global.PREFERRED_NETWORK_MODE, intent
                                    .getIntExtra(OLD_NETWORK_MODE, 0));
                }
                removeDialog(DIALOG_NETWORK_MODE_CHANGE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.network_mode_settings);

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        intentFilter.addAction(Utils.NETWORK_MODE_CHANGE_RESPONSE);
        mNetworkMode = (NetworkModePreference) findPreference(KEY_NETWORK_MODE_SETTING);
        mNetworkMode.setMultiple(false); // single card

        registerReceiver(mSimReceiver, intentFilter);
        mNetworkMode.setOnPreferenceChangeListener(this);

        // get a gemini phone instance
        mGeminiPhone = (Phone) PhoneFactory.getDefaultPhone();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSimReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int settingsNetworkMode = Settings.Global.getInt(getContentResolver(), Settings.Global.PREFERRED_NETWORK_MODE,
                PREFERRED_NT_MODE);

        if ((settingsNetworkMode >= 0) && (settingsNetworkMode <= 2)) {
            mNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        }

        boolean sim1Insert = mGeminiPhone.isSimInsert();
        boolean sim1Ready = false;

        // if(sim1Insert == true) {
        // sim1Ready = mGeminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_1);
        // }
        if (sim1Insert) {
            if (TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault().getSimState()) {
                sim1Ready = true;
            }
        }
        mNetworkMode.setEnabled(sim1Ready);
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {

        final String key = arg0.getKey();

        if (KEY_NETWORK_MODE_SETTING.equals(key)) {

            int oldNetworkMode = android.provider.Settings.Global.getInt(getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE, PREFERRED_NT_MODE);

            int newNetworkMode = Integer.valueOf((String) arg1).intValue();

            newNetworkMode = Utils.getNetworkMode(newNetworkMode);

            Settings.Global.putInt(getContentResolver(), Settings.Global.PREFERRED_NETWORK_MODE, newNetworkMode);

            if (newNetworkMode != oldNetworkMode) {

                Intent intent = new Intent(Utils.NETWORK_MODE_CHANGE_BROADCAST, null);
                intent.putExtra(Utils.OLD_NETWORK_MODE, oldNetworkMode);
                intent.putExtra(Utils.NETWORK_MODE_CHANGE_BROADCAST, newNetworkMode);
                intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
                showDialog(DIALOG_NETWORK_MODE_CHANGE);
                sendBroadcast(intent);
                Elog.i(TAG, "Send broadcast of " + Utils.NETWORK_MODE_CHANGE_BROADCAST);
            }
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        if (DIALOG_NETWORK_MODE_CHANGE == id) {
            dialog.setMessage(getResources().getString(R.string.gemini_data_connection_progress_message));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }
}
