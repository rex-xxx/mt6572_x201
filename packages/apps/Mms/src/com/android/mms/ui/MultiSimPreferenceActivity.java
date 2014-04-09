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
 * Copyright (C) 2008 Esmertec AG.
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
package com.android.mms.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.android.mms.R;
import com.android.mms.ui.AdvancedCheckBoxPreference.GetSimInfo;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import java.util.List;
import java.util.ArrayList;
import com.mediatek.common.featureoption.FeatureOption;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.ui.SmsPreferenceActivity;
import com.android.mms.ui.MmsPreferenceActivity;
/** M:
 * MultiSimPreferenceActivity
 */
public class MultiSimPreferenceActivity extends PreferenceActivity implements GetSimInfo {
    private static final String TAG = "MultiSimPreferenceActivity";
    private int simCount;
    private int mSimCount;
    private List<SIMInfo> mListSimInfo;

    private ArrayList<AdvancedCheckBoxPreference> mSimPreferencesList = new ArrayList<AdvancedCheckBoxPreference>();
    private int mSim1CurrentId;
    private int mSim2CurrentId;
    private String mIntentPreference;

    private int mTitleId = 0;


    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mListSimInfo = SIMInfo.getInsertedSIMList(this);
        mSimCount = mListSimInfo.size();

        addPreferencesFromResource(R.xml.multicardselection);
        Intent intent = getIntent();
        mIntentPreference = intent.getStringExtra("preference");
        //translate key to SIM-related key;
        Log.i("MultiSimPreferenceActivity, getIntent:", intent.toString());
        Log.i("MultiSimPreferenceActivity, getpreference:", mIntentPreference);
        mTitleId = intent.getIntExtra("preferenceTitleId",0);
        if (mTitleId != 0) {
            setTitle(getString(mTitleId));
        }
        changeMultiCardKeyToSimRelated(mIntentPreference);
        IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        registerReceiver(mSimReceiver, filter);

    }

    protected void onResume() {
        super.onResume();
        mListSimInfo = SIMInfo.getInsertedSIMList(this);
        for (AdvancedCheckBoxPreference sim : mSimPreferencesList) {
            if (sim != null) {
                sim.setNotifyChange(this);
            }
        }
    }
    private boolean isChecked(String preference,int sim_id){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mIntentPreference.equals(SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE)) {
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE , false);
        } else if (mIntentPreference.equals(MmsPreferenceActivity.MMS_DELIVERY_REPORT_MODE)) {
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.MMS_DELIVERY_REPORT_MODE, false);
        } else if (mIntentPreference.equals(MmsPreferenceActivity.AUTO_RETRIEVAL)) {
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.AUTO_RETRIEVAL, true);
        } else if (mIntentPreference.equals(MmsPreferenceActivity.READ_REPORT_MODE)) {
            if (FeatureOption.EVDO_DT_SUPPORT && isUSimType((int)(mListSimInfo.get(sim_id)).getSlot())) {
                (mSimPreferencesList.get((mListSimInfo.get(sim_id)).getSlot())).setEnabled(false);
                return false;
            }
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.READ_REPORT_MODE, false);
        } else if (mIntentPreference.equals(MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING)) {
            if(prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.AUTO_RETRIEVAL, true) == false){
                (mSimPreferencesList.get((mListSimInfo.get(sim_id)).getSlot())).setEnabled(false);
            }
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING, false);
        } else if (mIntentPreference.equals(MmsPreferenceActivity.READ_REPORT_AUTO_REPLY)) {
            if (FeatureOption.EVDO_DT_SUPPORT && isUSimType((int)(mListSimInfo.get(sim_id)).getSlot())) {
                (mSimPreferencesList.get((mListSimInfo.get(sim_id)).getSlot())).setEnabled(false);
                return false;
            }
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.READ_REPORT_AUTO_REPLY, false);
        } else if (mIntentPreference.equals(MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT)) {
            return prefs.getBoolean(Long.toString((mListSimInfo.get(sim_id)).getSimId()) + "_" + MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT, false);
        }
        return true;
    }

    private void changeMultiCardKeyToSimRelated(String preference) {

        int i = 0;
        int j = 0;
        AdvancedCheckBoxPreference sim1 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim1");
        AdvancedCheckBoxPreference sim2 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim2");
        AdvancedCheckBoxPreference sim3 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim3");
        AdvancedCheckBoxPreference sim4 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim4");
        mSimPreferencesList.add(sim1);
        mSimPreferencesList.add(sim2);
        mSimPreferencesList.add(sim3);
        mSimPreferencesList.add(sim4);
        for (AdvancedCheckBoxPreference sim : mSimPreferencesList) {
            if (sim != null) {
                sim.init(this, i);
            }
            i++;
        }
        boolean isHasCardInthisSlot[] = new boolean[mSimPreferencesList.size()];
        for (SIMInfo simInfo: mListSimInfo) {
            if (simInfo.getSlot() < mSimPreferencesList.size()) {
                isHasCardInthisSlot[simInfo.getSlot()] = true;
            }
        }
        for (i = 0; i < mSimPreferencesList.size(); i++) {
            if ((!isHasCardInthisSlot[i]) && (mSimPreferencesList.get(i) != null)) {
                getPreferenceScreen().removePreference(mSimPreferencesList.get(i));
            }else{
                for(j = 0; j < mListSimInfo.size(); j++){
                    if(mListSimInfo.get(j).getSlot() == i){
                        boolean mchecked = false;
                        mchecked = isChecked(preference,j);
                        Log.d(TAG, "changeMultiCardKeyToSimRelated[preference]" +preference);
                        Log.d(TAG, "changeMultiCardKeyToSimRelated[SlotId]" +i);
                        Log.d(TAG, "changeMultiCardKeyToSimRelated[SimId]"+mListSimInfo.get(j).getSimId());
                        Log.d(TAG, "changeMultiCardKeyToSimRelated[checked]"+mchecked);
                        ((mSimPreferencesList.get(i))).setChecked(mchecked);
                    }
                }
            }
        }
    }

    public String getSimName(int id) {
        for (SIMInfo simInfo : mListSimInfo) {
            if (simInfo.getSlot() == id) {
                return simInfo.getDisplayName();
            }
        }
        return "";
    }

    public String getSimNumber(int id) {
        for (SIMInfo simInfo : mListSimInfo) {
            if (simInfo.getSlot() == id) {
                return simInfo.getNumber();
            }
        }
        return "";
    }

    public int getSimColor(int id) {
        for (SIMInfo simInfo : mListSimInfo) {
            if (simInfo.getSlot() == id) {
                return simInfo.getSimBackgroundRes();
            }
        }
        return 0;
    }

    public int getNumberFormat(int id) {
        for (SIMInfo simInfo : mListSimInfo) {
            if (simInfo.getSlot() == id) {
                return simInfo.getDispalyNumberFormat();
            }
        }
        return 0;
    }

    public int getSimStatus(int id) {
        EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
        //int slotId = SIMInfo.getSlotById(this,mListSimInfo.get(id).mSimId);
        int slotId = -1;
        for (SIMInfo simInfo : mListSimInfo) {
            if (simInfo.getSlot() == id) {
                slotId = simInfo.getSlot();
            }
        }
        if (slotId != -1) {
            try {
                return teleService.getSimIndicatorStateGemini(slotId);
            } catch (RemoteException e) {
                Log.e(TAG, "getSimIndicatorStateGemini is failed.\n" + e.toString());
                return -1;
            }
        }
        return -1;
    }

    public boolean is3G(int id) {
        // int slotId = SIMInfo.getSlotById(this, mListSimInfo.get(id).mSimId);
        int slotId = -1;
        for (SIMInfo simInfo : mListSimInfo) {
            if (simInfo.getSlot() == id) {
                slotId = simInfo.getSlot();
            }
        }
        Log.i(TAG, "SIMInfo.getSlotById id: " + id + " slotId: " + slotId);
        if (slotId == MessageUtils.get3GCapabilitySIM()) {
            return true;
        }
        return false;
    }

    public boolean isUSimType(int slot) {
        /** M: MTK Encapsulation ITelephony */
        // final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
        //         .getService(Context.TELEPHONY_SERVICE));
        EncapsulatedTelephonyService iTel = EncapsulatedTelephonyService.getInstance();
        if (iTel == null) {
            Log.d(TAG, "[isUIMType]: iTel = null");
            return false;
        }

        try {
            return iTel.getIccCardTypeGemini(slot).equals("UIM");
        } catch (RemoteException e) {
            Log.e(TAG, "[isUIMType]: " + String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (NullPointerException e) {
            Log.e(TAG, "[isUIMType]: " + String.format("%s: %s", e.toString(), e.getMessage()));
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSimReceiver != null) {
            unregisterReceiver(mSimReceiver);
        }
    }

    /// M: update sim state dynamically. @{
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                if (SIMInfo.getInsertedSIMCount(context) < 2) {
                     finish();
                }
            }
        }
    };

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int i = 0;
        int currentSlotId = -1;
        long currentSimId = -1;
        currentSlotId = mSimPreferencesList.indexOf(preference);
        for (SIMInfo simInfo: mListSimInfo) {
            if (simInfo.getSlot() == currentSlotId) {
                currentSimId = simInfo.getSimId();
                break;
            }
        }
        Log.d(TAG, "onPreferenceTreeClick[SlotId]" +currentSlotId);
        Log.d(TAG, "onPreferenceTreeClick[SimId]"+currentSimId);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (mIntentPreference.equals(SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE)) {
                editor.putBoolean(Long.toString(currentSimId) + "_" + SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                        ((AdvancedCheckBoxPreference) preference).isChecked());
        } else if (mIntentPreference.equals(MmsPreferenceActivity.MMS_DELIVERY_REPORT_MODE)) {
                editor.putBoolean(Long.toString(currentSimId) + "_" + MmsPreferenceActivity.MMS_DELIVERY_REPORT_MODE,
                        ((AdvancedCheckBoxPreference) preference).isChecked());
        } else if (mIntentPreference.equals(MmsPreferenceActivity.AUTO_RETRIEVAL)) {
            editor.putBoolean(Long.toString(currentSimId) + "_" + MmsPreferenceActivity.AUTO_RETRIEVAL,
                    ((AdvancedCheckBoxPreference) preference).isChecked());
        } else if (mIntentPreference.equals(MmsPreferenceActivity.READ_REPORT_MODE)) {
            editor.putBoolean(Long.toString(currentSimId) + "_" + MmsPreferenceActivity.READ_REPORT_MODE,
                    ((AdvancedCheckBoxPreference) preference).isChecked());
        } else if (mIntentPreference.equals(MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING)) {
            editor.putBoolean(Long.toString(currentSimId) + "_" + MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING,
                    ((AdvancedCheckBoxPreference) preference).isChecked());
        } else if (mIntentPreference.equals(MmsPreferenceActivity.READ_REPORT_AUTO_REPLY)) {
            editor.putBoolean(Long.toString(currentSimId) + "_" + MmsPreferenceActivity.READ_REPORT_AUTO_REPLY,
                    ((AdvancedCheckBoxPreference) preference).isChecked());
        } else if (mIntentPreference.equals(MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT)) {
            editor.putBoolean(Long.toString(currentSimId) + "_" + MmsPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT,
                    ((AdvancedCheckBoxPreference) preference).isChecked());
        }
        editor.apply();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
