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
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.settings.CallSettings;
import com.mediatek.settings.MultipleSimActivity;
import com.mediatek.settings.NoNetworkPopUpService;

import java.util.List;
/**
 * "Networks" settings UI for the Phone app.
 */
public class NetworkSetting extends PreferenceActivity {

    private static final String LOG_TAG = "phone";
    private static final boolean DBG = true;

    private static final int EVENT_AUTO_SELECT_DONE = 300;
    private static final int EVENT_SERVICE_STATE_CHANGED = 400;

    //dialog ids
    private static final int DIALOG_NETWORK_MENU_SELECT = 200;
    private static final int DIALOG_NETWORK_AUTO_SELECT = 300;
    private static final int DIALOG_DISCONNECT_DATA_CONNECTION = 500;

    //String keys for preference lookup
    private static final String BUTTON_SELECT_MANUAL = "button_manual_select_key";
    private static final String BUTTON_AUTO_SELECT_KEY = "button_auto_select_key";


    Phone mPhone;
    protected boolean mIsForeground = false;

    /** message for network selection */
    String mNetworkSelectMsg;

    //preference objects
    private Preference mManuSelect;
    private Preference mAutoSelect;

    /// M: the values is for Gemini @{
    private TextView mNoServiceMsg;
    private CheckBox mShowAlwaysCheck;
    private TextView mShowAlwaysTitle;

    private String mTitleName = null;
    protected boolean mIsResignSuccess = false;
    private GeminiPhone mGeminiPhone;
    private int mSimId = 0;
    private static final int SIM_CARD_UNDEFINED = -1;
    private static final int MENU_CANCEL = 100;
    private boolean mAirplaneModeEnabled;
    private int mDualSimMode = -1;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                log("ACTION_AIRPLANE_MODE_CHANGED" + " ||mAirplaneModeEnabled:" + mAirplaneModeEnabled);
                setScreenEnabled(true);
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                setScreenEnabled(true);
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap {
                Log.d(LOG_TAG, "ACTION_SIM_INFO_UPDATE received");
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(NetworkSetting.this);
                if (temp.size() == 0) {
                    Log.d(LOG_TAG, "Activity finished");
                    CallSettings.goToMobileNetworkSettings(NetworkSetting.this);
                } else if (temp.size() == 1) {
                    if (temp.get(0).mSlot != mSimId) {
                        Log.d(LOG_TAG, "temp.size()=" + temp.size() + "Activity finished");
                        CallSettings.goToMobileNetworkSettings(NetworkSetting.this);
                    }
                }
                ///@}
            }
        }
    };
    ///@}
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_AUTO_SELECT_DONE:
                if (DBG) {
                    log("hideProgressPanel");
                }
                /// M: dismiss all dialogs when auto select done @{
                NetworkSetting.this.removeDialog(DIALOG_NETWORK_AUTO_SELECT);
                /// @}
                    
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    if (DBG) {
                        log("automatic network selection: failed!");
                    }
                    displayNetworkSelectionFailed(ar.exception);
                } else {
                    if (DBG) {
                        log("automatic network selection: succeeded!");
                    }
                    displayNetworkSelectionSucceeded();
                }
                break;
            /// M: add state changed @{
            case EVENT_SERVICE_STATE_CHANGED:
                Log.d(LOG_TAG, "EVENT_SERVICE_STATE_CHANGED");                        
                setScreenEnabled(true);
                break;
            /// @}
            default:
                break;
            }
            return;
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAutoSelect) {
            selectNetworkAutomatic();
        } else if (preference == mManuSelect) {
            if (CallSettings.isMultipleSim() && !PhoneFactory.isDualTalkMode()) {
                long dataConnectionId = Settings.System.getLong(getContentResolver(),
                    Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                log("dataConnectionId = " + dataConnectionId);
                if (dataConnectionId != Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                    int slot = SIMInfo.getSlotById(this, dataConnectionId);
                    log("slot = " + mSimId);
                    if (slot != mSimId) {
                        // show dialog to user, whether disconnect data connection
                        showDialog(DIALOG_DISCONNECT_DATA_CONNECTION);
                        return true;
                    }
                }
            }
            showDialog(DIALOG_NETWORK_MENU_SELECT);
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.network_setting);
        addPreferencesFromResource(R.xml.carrier_select);
        mPhone = PhoneGlobals.getPhone();

        mManuSelect = getPreferenceScreen().findPreference(BUTTON_SELECT_MANUAL);
        mAutoSelect = getPreferenceScreen().findPreference(BUTTON_AUTO_SELECT_KEY);
        mTitleName = getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME);
        if (mTitleName != null) {
            setTitle(mTitleName);
        }

        mNoServiceMsg = (TextView)findViewById(R.id.message);
        mShowAlwaysCheck = (CheckBox)findViewById(R.id.show_always);
        if (mShowAlwaysCheck != null) {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            mShowAlwaysCheck.setChecked(sp.getBoolean(NoNetworkPopUpService.NO_SERVICE_KEY, false));
            mShowAlwaysCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isCheck) {
                    SharedPreferences.Editor editor = sp.edit(); 
                    editor.putBoolean(NoNetworkPopUpService.NO_SERVICE_KEY, isCheck);
                    editor.commit();
                }
            });
        }
        mShowAlwaysTitle = (TextView)findViewById(R.id.show_always_title);

        geminiPhoneInit();

        if (isNoService()) {
            if (CallSettings.isMultipleSim()) {
                SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getDefault();
                if (simInfoWrapper != null) {
                    SIMInfo simInfo = simInfoWrapper.getSimInfoBySlot(mSimId);
                    if (simInfo != null) {
                        setTitle(getResources().getString(R.string.no_service_msg_title_gemini, simInfo.mDisplayName));
                        mNoServiceMsg.setText(getResources().getString(
                                R.string.no_service_msg_gemini, simInfo.mDisplayName));
                    }
                }
            } else {
                setTitle(getResources().getString(R.string.no_service_msg_title));
                mNoServiceMsg.setText(getResources().getString(R.string.no_service_msg));
            }
        } else {
            mNoServiceMsg.setVisibility(View.GONE);
            mShowAlwaysCheck.setVisibility(View.GONE);
            mShowAlwaysTitle.setVisibility(View.GONE);
        }

        if (DBG) {
            log("It's a GeminiPhone ? = " + CallSettings.isMultipleSim() + "SIM_ID = " + mSimId);
        }
        /// M: receive network change broadcast to sync with network @{
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        if (CallSettings.isMultipleSim()) {
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        /// @}
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_NETWORK_AUTO_SELECT:
            dialog = new ProgressDialog(this);
            ((ProgressDialog)dialog).setMessage(getResources().getString(R.string.register_automatically));
            ((ProgressDialog)dialog).setCancelable(false);
            ((ProgressDialog)dialog).setIndeterminate(true);
            break;
        case DIALOG_NETWORK_MENU_SELECT:
            dialog = new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.manual_select_dialog_msg))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.phone", "com.mediatek.settings.NetworkSettingList");
                        intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
            break;
            case DIALOG_DISCONNECT_DATA_CONNECTION:
                dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.disable_data_connection_msg)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectivityManager cm =
                                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                            if (cm != null) {
                                cm.setMobileDataEnabled(false);
                            }
                            showDialog(DIALOG_NETWORK_MENU_SELECT);
                        }
                    })
                    .setNegativeButton(com.android.internal.R.string.no, null)
                    .create();
                break;
            default:
                break;
        }
        /// M: add a log to debug the dialog to show
        log("[onCreateDialog] create dialog id is " + id);
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isNoService()) {
            menu.add(Menu.NONE, MENU_CANCEL, 0, R.string.cancel)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return super.onCreateOptionsMenu(menu);
    } 

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_CANCEL:
            finish();
            break;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        /// M: when reassign error enable network settings to reassign twice @{
        mIsResignSuccess = false;
        setScreenEnabled(true);
        /// @}
        String status;

        if ((ex != null && ex instanceof CommandException) &&
                ((CommandException)ex).getCommandError() == CommandException.Error.ILLEGAL_SIM_OR_ME) {
            status = getResources().getString(R.string.not_allowed);
        } else {
            status = getResources().getString(R.string.connect_later);
        }

        final PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(
                NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionSucceeded() {
        /// M: when reassign success disable network settings to avoid reassign twice
        mIsResignSuccess = true;
        setScreenEnabled(false);
        /// @}

        String status = getResources().getString(R.string.registration_done);

        final PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(
                NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 3000);
    }

    private void selectNetworkAutomatic() {
        if (DBG) {
            log("select network automatically...");
        }
        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_AUTO_SELECT);
        }

        Message msg = mHandler.obtainMessage(EVENT_AUTO_SELECT_DONE);
        /// M: to avoid start two same activity @{
        if (!CallSettings.isMultipleSim()) {
            mPhone.setNetworkSelectionModeAutomatic(msg);
        } else {
            mGeminiPhone.setNetworkSelectionModeAutomaticGemini(msg, mSimId);
        }
        /// @}
    }
    
    @Override    
    protected void onResume() {
        super.onResume();
        mIsForeground = true;
        /// M: to avoid start two same activity @{
        mPhoneStateReceiver.registerIntent(); 
        registerReceiver(mReceiver, mIntentFilter);
        mAirplaneModeEnabled = (Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1);
        if (CallSettings.isMultipleSim()) {
            mDualSimMode = android.provider.Settings.System.getInt(
                    getContentResolver(), android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
            Log.d(LOG_TAG, "NetworkSettings.onResume(), mDualSimMode=" + mDualSimMode);
        }
        setScreenEnabled(true);
        /// @}
    } 
    
    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;        
        /// M: to avoid start two same activity @{
        mPhoneStateReceiver.unregisterIntent();
        unregisterReceiver(mReceiver);
        /// @}
    }
    
    /// M: when airplane mode, radio off, dualsimmode == 0 disable the feature
    private void setScreenEnabled(boolean flag) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isCallStateIdle;
        /* M: add dual talk, @ { */
        if (CallSettings.isMultipleSim() && PhoneFactory.isDualTalkMode()) {
            isCallStateIdle = telephonyManager.getCallStateGemini(mSimId) == TelephonyManager.CALL_STATE_IDLE;
        } else {
            isCallStateIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        }
        /* @ } */
        getPreferenceScreen().setEnabled(flag && !mIsResignSuccess && !isRadioPoweroff() && isCallStateIdle 
            && (!mAirplaneModeEnabled) && (mDualSimMode != 0));
    }
    
    /// M: add for support gemini phone
    private void geminiPhoneInit() {
        if (CallSettings.isMultipleSim()) {
            Intent it = getIntent();
            mSimId = it.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, SIM_CARD_UNDEFINED);
            mGeminiPhone = (GeminiPhone) PhoneGlobals.getPhone();
        } 
    }

    private boolean isNoService() {
        return getIntent().getBooleanExtra(NoNetworkPopUpService.NO_SERVICE, false);
    }

    /// in single or gemini phone, is radio off or not
    private boolean isRadioPoweroff() {
        boolean isPoweroff = false; 
        if (CallSettings.isMultipleSim()) {
            ServiceState serviceState = mPhoneStateReceiver.getServiceStateGemini(mSimId);
            isPoweroff = serviceState.getState() == ServiceState.STATE_POWER_OFF;
        } else {
            ServiceState serviceState = mPhoneStateReceiver.getServiceState();
            isPoweroff = serviceState.getState() == ServiceState.STATE_POWER_OFF;
        }
        Log.d(LOG_TAG, "isRadioPoweroff=" + isPoweroff);        
        return isPoweroff;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }
}
