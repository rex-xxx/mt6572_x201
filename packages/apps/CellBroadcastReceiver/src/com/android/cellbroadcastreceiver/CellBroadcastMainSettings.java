/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.cellbroadcastreceiver;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Telephony.SIMInfo;
import android.util.Log;
import com.android.cellbroadcastreceiver.CheckBoxAndSettingsPreference.OnEnableCBChangedListener;
import java.util.List;
/**
 * Settings activity for the cell broadcast receiver.
 */
public class CellBroadcastMainSettings extends PreferenceActivity 
                               implements OnEnableCBChangedListener{
    private static final String TAG = "[CMAS]CellBroadcastMainSettings";

    public static int sSlotId;
    public static int sReadySlotId = -1;

    private CheckBoxAndSettingsPreference mEnableCBCheckBox;
    private CheckBoxAndSettingsPreference mImminentCheckBox;
    private CheckBoxAndSettingsPreference mAmberCheckBox;
    private CheckBoxAndSettingsPreference mSpeechCheckBox;
    private BroadcastReceiver mEnableCBReceiver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        List<SIMInfo> listSimInfo;

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_main);

        mEnableCBCheckBox = (CheckBoxAndSettingsPreference)
            findPreference(CheckBoxAndSettingsPreference.KEY_ENABLE_CELLBROADCAST);
        mEnableCBCheckBox.setOnEnableCBChangedListener(this);
        mImminentCheckBox = (CheckBoxAndSettingsPreference)
            findPreference(CheckBoxAndSettingsPreference.KEY_ENABLE_CMAS_IMMINENT_ALERTS);
        mAmberCheckBox = (CheckBoxAndSettingsPreference)
            findPreference(CheckBoxAndSettingsPreference.KEY_ENABLE_CMAS_AMBER_ALERTS);
        mSpeechCheckBox = (CheckBoxAndSettingsPreference)
            findPreference(CheckBoxAndSettingsPreference.KEY_ENABLE_ALERT_SPEECH);

        listSimInfo = SIMInfo.getInsertedSIMList(this);
        if (listSimInfo == null || (listSimInfo != null && listSimInfo.isEmpty())) {
            Log.d(TAG, "there is no sim card");
            if (mEnableCBCheckBox != null) {
                mEnableCBCheckBox.setEnabled(false);
                setCheckBoxPreferenceEnable(false);
            }
            return;
        }
        Log.d(TAG, "listSimInfo.size " + listSimInfo.size());
        int slotId = listSimInfo.get(0).mSlot;
        
        if (mEnableCBCheckBox != null) {
            if (sReadySlotId != slotId) {
                mEnableCBCheckBox.setEnabled(false);

                if (mEnableCBReceiver == null) {
                    Log.d(TAG, "register sim_changed action for enable CB " + sReadySlotId);
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(CellBroadcastReceiver.SMS_STATE_CHANGED_ACTION);
                    mEnableCBReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            boolean isReady = intent.getBooleanExtra("ready", false);

                            Log.d(TAG, "onReceive : SMS_STATE_CHANGED_ACTION res " + isReady);
                            if (isReady) {
                                sReadySlotId = intent.getIntExtra("simId", 0);
                                refreshEnableCheckBox(true);
                                context.unregisterReceiver(mEnableCBReceiver);
                                mEnableCBReceiver = null;
                            }
                        }
                    };
                    registerReceiver(mEnableCBReceiver, filter);
                } else {
                    Log.d(TAG, "mEnableCBReceiver had been registered");
                }
                
            }
        }
        setCheckBoxPreferenceEnable(mEnableCBCheckBox.isChecked() && mEnableCBCheckBox.isEnabled());

    }

    public void onDestroy() {
        super.onDestroy();
        if (mEnableCBReceiver != null) {
            unregisterReceiver(mEnableCBReceiver);
        }
    }

    void refreshEnableCheckBox(boolean enable) {
        CheckBoxAndSettingsPreference checkbox = null;
        if (enable) {
            checkbox = (CheckBoxAndSettingsPreference) 
                findPreference(CheckBoxAndSettingsPreference.KEY_ENABLE_CELLBROADCAST);
            if (checkbox != null) {
                checkbox.setEnabled(true);
                setCheckBoxPreferenceEnable(true);
            }
            Log.d(TAG, "refreshEnableCheckBox true checkbox " + checkbox);
        } else {
            Log.d(TAG, "refreshEnableCheckBox false " + sReadySlotId);
        }
    }

    public void setCheckBoxPreferenceEnable(boolean enabled){
        mImminentCheckBox.setEnabled(enabled);
        mAmberCheckBox.setEnabled(enabled);
        mSpeechCheckBox.setEnabled(enabled);
    }

    @Override
    public void onEnableCBChanged(CheckBoxAndSettingsPreference preference) {
        Log.d(TAG, "onEnableCBChanged ");
        if(mEnableCBCheckBox.isChecked()){
            Log.d(TAG, "onEnableCBChanged true ");
            setCheckBoxPreferenceEnable(true);
        } else{
            Log.d(TAG, "onEnableCBChanged false");
            setCheckBoxPreferenceEnable(false);
        }
    }
}
