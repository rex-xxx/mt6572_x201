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
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ThrottleManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.SettingsExtension;
import com.mediatek.phone.gemini.GeminiUtils;
import com.mediatek.settings.CallSettings;
import com.mediatek.settings.DefaultSimPreference;
import com.mediatek.settings.MultipleSimActivity;
import com.mediatek.settings.PreCheckForRunning;
import com.mediatek.settings.SimItem;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class MobileNetworkSettings extends PreferenceActivity
        implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener {
    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    private static final String BUTTON_DATA_ENABLED_KEY = "button_data_enabled_key";
    private static final String BUTTON_DATA_USAGE_KEY = "button_data_usage_key";
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";

    ///M: add for data connection under gemini
    private static final String KEY_DATA_CONN = "data_connection_setting";
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    private static final String ACTION_DATA_USAGE_DISABLED_DIALOG_OK =
            "com.mediatek.systemui.net.action.ACTION_DATA_USAGE_DISABLED_DIALOG_OK";
    private static final int PREFERRED_NETWORK_MODE = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    //UI objects
    private CheckBoxPreference mButtonDataRoam;
    private CheckBoxPreference mButtonDataEnabled;
    private Preference mLteDataServicePref;
    ///M: add for data conn feature @{
    private DefaultSimPreference mDataConnPref = null;
    ///@}
    private Preference mButtonDataUsage;
    private DataUsageListener mDataUsageListener;
    private static final String IFACE = "rmnet0"; //TODO: this will go away
    private Phone mPhone;
    private MyHandler mHandler;
    private boolean mOkClicked;
    private SettingsExtension mExtension;
    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    /// M: add for gemini support @{
    private static final String BUTTON_GSM_UMTS_OPTIONS = "gsm_umts_options_key";
    private static final String BUTTON_CDMA_OPTIONS = "cdma_options_key";
    private static final String BUTTON_APN = "button_apn_key";
    private static final String BUTTON_CARRIER_SEL = "button_carrier_sel_key";
    private static final String BUTTON_3G_SERVICE = "button_3g_service_key";
    private static final String BUTTON_PLMN_LIST = "button_plmn_key";
    private static final String BUTTON_2G_ONLY = "button_prefer_2g_key";
    private static final String BUTTON_NETWORK_MODE_EX_KEY = "button_network_mode_ex_key";
    private static final String BUTTON_NETWORK_MODE_KEY = "gsm_umts_preferred_network_mode_key";

    private int mSimId;
    private static final int PIN1_REQUEST_CODE = 302;

    private static final int DATA_ENABLE_ALERT_DIALOG = 100;
    private static final int DATA_DISABLE_ALERT_DIALOG = 200;
    private static final int ROAMING_DIALOG = 300;
    private static final int PROGRESS_DIALOG = 400;

    private Preference mButtonPreferredNetworkModeEx;
    private ListPreference mButtonPreferredNetworkMode;
    private Preference mPreference3GSwitch;
    private Preference mPLMNPreference;
    private CheckBoxPreference mButtonPreferredGSMOnly;
    private GeminiPhone mGeminiPhone;
    private PreferenceScreen mApnPref;
    private PreferenceScreen mCarrierSelPref;
    private PreCheckForRunning mPreCheckForRunning;

    private static final int MODEM_MASK_GPRS = 0x01;
    private static final int MODEM_MASK_EDGE = 0x02;
    private static final int MODEM_MASK_WCDMA = 0x04;
    private static final int MODEM_MASK_TDSCDMA = 0x08;
    private static final int MODEM_MASK_HSDPA = 0x10;
    private static final int MODEM_MASK_HSUPA = 0x20;
    private CellConnMgr mCellConnMgr;
    private TelephonyManager mTelephonyManager;
    private ConnectivityManager mConnService;

    private static final int DIALOG_GPRS_SWITCH_CONFIRM = 1;
    private int mDataSwitchMsgIndex = -1;
    private long mSelectGprsIndex = -1;


    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            log("onCallStateChanged ans state is " + state);
            switch(state) {
            case TelephonyManager.CALL_STATE_IDLE:
                setScreenEnabled();
                break;
            default:
                break;
            }
        }
    };

    private ContentObserver mGprsDefaultSIMObserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            setDataConnPref();
        }
    };

    ///@}
    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //Added by vend_am00015 2010-06-07
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                setScreenEnabled();
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED) && mIsChangeData) {
                Log.d(LOG_TAG, "catch data change!");
                PhoneConstants.DataState state = getMobileDataState(intent);
                String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                Log.d(LOG_TAG, "apnTypeList=" + apnTypeList);
                Log.d(LOG_TAG,"state=" + state);
                if (PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList) && 
                    (state == PhoneConstants.DataState.CONNECTED || 
                     state == PhoneConstants.DataState.DISCONNECTED)) {
                    mH.removeMessages(DATA_STATE_CHANGE_TIMEOUT);
                    removeDialog(PROGRESS_DIALOG);
                    mIsChangeData = false;
                    setDataConnPref();
                }
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                setScreenEnabled();
            } else if (TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY.equals(action)) {
                setNetworkOperator();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                Log.d(LOG_TAG,"indicator state changed");
                setDataConnPref();
                setScreenEnabled();
            } else if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                ///M: add for hot swap {
                Log.d(LOG_TAG, "ACTION_SIM_INFO_UPDATE received");
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(MobileNetworkSettings.this);
                if (simList != null) {
                    Log.d(LOG_TAG, "sim card number is: " + simList.size());
                    if (simList.size() > 0) {
                        setDataConnPref();
                        setScreenEnabled();
                    } else {
                        finish();
                    }
                }
                ///@}
            } else if (PhoneGlobals.NETWORK_MODE_CHANGE_RESPONSE.equals(action)) {
                if (!intent.getBooleanExtra(PhoneGlobals.NETWORK_MODE_CHANGE_RESPONSE, true)) {
                        Log.d(LOG_TAG,"network mode change failed! restore the old value.");
                        int oldMode = intent.getIntExtra(PhoneGlobals.OLD_NETWORK_MODE, 0);
                        Log.d(LOG_TAG,"oldMode = " + oldMode);
                        android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                                oldMode);
                }
            } else if (action.equals(TRANSACTION_START)) {
                Log.d(LOG_TAG,"start to send MMS");
                if (mDataConnPref.isEnabled()) {
                    mDataConnPref.setEnabled(false);
                }
                Dialog dlg = mDataConnPref.getDialog();
                if (dlg != null && dlg.isShowing()) {
                    Log.d(LOG_TAG, "MMS stopped dismiss GPRS selection dialog");
                    dlg.dismiss();
                }
            } else if (action.equals(TRANSACTION_STOP)) {
                Log.d(LOG_TAG,"send MMS is end");
                if (!mDataConnPref.isEnabled()) {
                    mDataConnPref.setEnabled(true);
                }
                Dialog dlg = mDataConnPref.getDialog();
                if (dlg != null && dlg.isShowing()) {
                    Log.d(LOG_TAG, "MMS stopped dismiss GPRS selection dialog");
                    dlg.dismiss();
                }
            } else if (action.equals(ACTION_DATA_USAGE_DISABLED_DIALOG_OK)) {
                Log.d(LOG_TAG, "deal with data limit broadcast");
                removeDialog(PROGRESS_DIALOG);
                mIsChangeData = false;
                setDataConnPref();
            }
        }
    };
    public static final int DATA_STATE_CHANGE_TIMEOUT = 2001;
    private boolean mIsChangeData = false;
    Handler mH = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == DATA_STATE_CHANGE_TIMEOUT) {
                removeDialog(PROGRESS_DIALOG);
                mIsChangeData = false;
                setDataConnPref();
            }
        }
    };
    /// @}
    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPhone.setDataRoamingEnabled(true);
            mOkClicked = true;
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (!mOkClicked) {
            mButtonDataRoam.setChecked(false);
        }
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        /// M: add for plmn prefer feature for CTA test & gemini rat mode @{       
        if (preference == mPLMNPreference) {
            if (CallSettings.isMultipleSim()) {
                Intent intent = new Intent(this, MultipleSimActivity.class);
                intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.plmn_list_setting_title);
                intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
                intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.mediatek.settings.PLMNListPreference");
                mPreCheckForRunning.checkToRun(intent, mSimId, 302);
                return true;
            } else {
                return false;
            }
        } else if (preference == mButtonPreferredNetworkModeEx) {
            CharSequence[] entries;
            CharSequence[] entriesValue; 
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "ListPreference");
            if ((getBaseBand(PhoneConstants.GEMINI_SIM_1) & MODEM_MASK_TDSCDMA) != 0) {
                intent.putExtra(MultipleSimActivity.INIT_ARRAY, R.array.gsm_umts_network_preferences_choices_cmcc);
                intent.putExtra(MultipleSimActivity.INIT_ARRAY_VALUE, R.array.gsm_umts_network_preferences_values_cmcc);
            } else {
                intent.putExtra(MultipleSimActivity.INIT_ARRAY, R.array.gsm_umts_network_preferences_choices);
                intent.putExtra(MultipleSimActivity.INIT_ARRAY_VALUE, R.array.gsm_umts_network_preferences_values);
            }
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.preferred_network_mode_title);
            intent.putExtra(MultipleSimActivity.LIST_TITLE, R.string.gsm_umts_network_preferences_title);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "NETWORK_MODE");

            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, PhoneConstants.GEMINI_SIM_1);
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, CallSettings.get3GSimCards(this));
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "preferred_network_mode_key@");
            mPreCheckForRunning.checkToRun(intent, mSimId, 302);
            return true;
        }
        /// @}       
        
        if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference)) {
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference)) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(), android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    PREFERRED_NETWORK_MODE);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            if (DBG) {
                log("onPreferenceTreeClick: preference == mButtonDataRoam.");
            }

            //normally called on the toggle click
            if (mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                showDialog(ROAMING_DIALOG);
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        } else if (preference == mButtonDataEnabled) {
            if (DBG) {
                log("onPreferenceTreeClick: preference == mButtonDataEnabled.");
            }
            ///M: change the interface definition for consistent_UI
            if (!mExtension.dataEnableReminder(mButtonDataEnabled.isChecked(), this)) {
                Log.d(LOG_TAG,"onPreferenceTreeClick: preference == mButtonDataEnabled.");
                if (mButtonDataEnabled.isChecked() && isSimLocked()) {
                    mCellConnMgr.handleCellConn(0, PIN1_REQUEST_CODE);    
                    Log.d(LOG_TAG,"Data enable check change request pin single card");
                    mButtonDataEnabled.setChecked(false);
                } else {
                    mIsChangeData = true;
                    NetworkInfo networkInfo = mConnService.getActiveNetworkInfo();
                    if (!(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI 
                        && networkInfo.isConnected())) {
                        showDialog(PROGRESS_DIALOG);
                    }
                    mConnService.setMobileDataEnabled(mButtonDataEnabled.isChecked());
                    mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
                    if (mButtonDataEnabled.isChecked() &&
                            isNeedtoShowRoamingMsg()) {
                        mExtension.showWarningDlg(this,R.string.data_conn_under_roaming_hint);
                    }
                    ///M: add for ATT requirement
                    mExtension.disableDataRoaming(mButtonDataRoam,mButtonDataEnabled.isChecked());
                }
            }
            return true;
        } else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                String imsi = mTelephonyManager.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        } else if (preference == mApnPref && CallSettings.isMultipleSim()) {
            //// M: for gemini support @{
            Intent it = new Intent();
            it.setAction("android.intent.action.MAIN");
            it.setClassName("com.android.phone", "com.mediatek.settings.MultipleSimActivity");
            it.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            it.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.apn_settings);
            it.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            it.putExtra(MultipleSimActivity.TARGET_CALSS, "com.android.settings.ApnSettings");
            mPreCheckForRunning.checkToRun(it, mSimId, 302);
            return true;
        } else if (preference == mCarrierSelPref && CallSettings.isMultipleSim()) {
            Intent it = new Intent();
            it.setAction("android.intent.action.MAIN");
            it.setClassName("com.android.phone", "com.mediatek.settings.MultipleSimActivity");
            it.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.networks);
            it.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            it.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "NETWORK_SEARCH");
            it.putExtra(MultipleSimActivity.TARGET_CALSS, "com.android.phone.NetworkSetting");
            mPreCheckForRunning.checkToRun(it, mSimId, 302);
            return true;
            //// @}
        } else if (preference == mDataConnPref) {
            ///M: consistent_UI use the data connection ui style same as sim management for single 
            ///   and gemini
            Log.d(LOG_TAG,"mDataConnPref is clicked");
            return true;

        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    //// M: is sim locked or not
    private boolean isSimLocked() {
        boolean isLocked = false;
        try {          
            ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (iTelephony != null) {
                isLocked = iTelephony.getSimIndicatorState() == PhoneConstants.SIM_INDICATOR_LOCKED;
            }
        } catch (android.os.RemoteException e) {
            Log.d(LOG_TAG, "[e = " + e + "]");
        }

        return isLocked;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.network_setting);

        mExtension = ExtensionManager.getInstance().getSettingsExtension();

        mPhone = PhoneGlobals.getPhone();
        /// M: for gemini phone
        if (CallSettings.isMultipleSim()) {
            mGeminiPhone = (GeminiPhone)mPhone;
        }
        mHandler = new MyHandler();
        /// M: for receivers sim lock gemini phone @{
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        ///M: add to receiver indicator intents@{
        /**modify for consistent_UI*/
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mIntentFilter.addAction(PhoneGlobals.NETWORK_MODE_CHANGE_RESPONSE); 
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        mIntentFilter.addAction(TRANSACTION_START);
        mIntentFilter.addAction(TRANSACTION_STOP);
        mIntentFilter.addAction(ACTION_DATA_USAGE_DISABLED_DIALOG_OK);
        ///@}
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mPreCheckForRunning = new PreCheckForRunning(this);
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
        if (list.size() == 1) {
            mPreCheckForRunning.mByPass = false;
            mSimId = list.get(0).mSlot;
        } else {
            mPreCheckForRunning.mByPass = true;
        }
        /// @}
        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        //M: add data connection for gemini sim project
        mDataConnPref = (DefaultSimPreference) prefSet.findPreference(KEY_DATA_CONN);
        mDataConnPref.setOnPreferenceChangeListener(this);
        mButtonDataEnabled = (CheckBoxPreference) prefSet.findPreference(BUTTON_DATA_ENABLED_KEY);
        mButtonDataRoam = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        mButtonDataRoam.setSummaryOn(mExtension.getRoamingSummary(this,R.string.roaming_enable));
        mButtonDataRoam.setSummaryOff(mExtension.getRoamingSummary(this,R.string.roaming_disable));
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonDataUsage = prefSet.findPreference(BUTTON_DATA_USAGE_KEY);
        /// M: support gemini phone, 3G switch, PLMN prefer @{     
        /// M:  remove this data connection by using  mDataConnPref,
        //      and data roaming under multi-sim 
        //      for consistent_UI, move to sim management {
        prefSet.removePreference(mButtonDataEnabled);
        if (CallSettings.isMultipleSim()) {
            prefSet.removePreference(mButtonDataRoam);    
        }
        /// @}
        mPreference3GSwitch = prefSet.findPreference(BUTTON_3G_SERVICE);
        mPLMNPreference = prefSet.findPreference(BUTTON_PLMN_LIST);
        /// @}   
        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);
        mButtonPreferredNetworkModeEx = prefSet.findPreference(BUTTON_NETWORK_MODE_EX_KEY);

        boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        if (getResources().getBoolean(R.bool.world_phone)) {
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            //Get the networkMode from Settings.System and displays it
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    PREFERRED_NETWORK_MODE);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
        } else {
            if (!isLteOnCdma) {
                prefSet.removePreference(mButtonPreferredNetworkMode);
            }
            int phoneType = mPhone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
                if (isLteOnCdma) {
                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                    mButtonPreferredNetworkMode.setEntries(
                            R.array.preferred_network_mode_choices_lte);
                    mButtonPreferredNetworkMode.setEntryValues(
                            R.array.preferred_network_mode_values_lte);
                    int settingsNetworkMode = android.provider.Settings.Global.getInt(
                            mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                            PREFERRED_NETWORK_MODE);
                    mButtonPreferredNetworkMode.setValue(
                            Integer.toString(settingsNetworkMode));
                }
                /// M: support for cdma @{
                if (!PhoneUtils.isSupportFeature("3G_SWITCH")) {
                    if (mPreference3GSwitch != null) {
                        prefSet.removePreference(mPreference3GSwitch);
                        mPreference3GSwitch = null;
                    }
                }
                prefSet.removePreference(mButtonPreferredNetworkModeEx);
                mApnPref = (PreferenceScreen) prefSet.findPreference(BUTTON_APN);
                mCarrierSelPref = (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SEL);
                /// @}
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
                /// M: support for operators @{
                mApnPref = (PreferenceScreen) prefSet.findPreference(BUTTON_APN);
                mButtonPreferredGSMOnly = (CheckBoxPreference) prefSet.findPreference(BUTTON_2G_ONLY);
                mButtonPreferredNetworkMode = (ListPreference)prefSet.findPreference(BUTTON_NETWORK_MODE_KEY);

                //Get the networkMode from Settings.System and displays it
                int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    PREFERRED_NETWORK_MODE);
                if (settingsNetworkMode > 2) {
                    settingsNetworkMode = PREFERRED_NETWORK_MODE;
                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        settingsNetworkMode);
                }
                mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));

                if (!PhoneUtils.isSupportFeature("3G_SWITCH")) {
                    prefSet.removePreference(mPreference3GSwitch);
                    if (getBaseBand(0) > MODEM_MASK_EDGE) {
                        if (CallSettings.isMultipleSim()) {
                            prefSet.removePreference(mButtonPreferredNetworkMode);
                        } else {
                            prefSet.removePreference(mButtonPreferredNetworkModeEx);
                        }
                    }
                } else {
                    prefSet.removePreference(mButtonPreferredNetworkModeEx);
                    prefSet.removePreference(mButtonPreferredNetworkMode);
                }

                mExtension.customizeFeatureForOperator(prefSet, mButtonPreferredNetworkModeEx, 
                    mButtonPreferredNetworkMode, mPreference3GSwitch, mButtonPreferredGSMOnly);

                if (mButtonPreferredNetworkMode != null) {
                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                    if ((getBaseBand(PhoneConstants.GEMINI_SIM_1) & MODEM_MASK_TDSCDMA) != 0) {
                        mButtonPreferredNetworkMode.setEntries(
                            getResources().getStringArray(R.array.gsm_umts_network_preferences_choices_cmcc));    
                        mButtonPreferredNetworkMode.setEntryValues(
                            getResources().getStringArray(R.array.gsm_umts_network_preferences_values_cmcc));    
                    }
                }
                mCarrierSelPref = (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SEL);
                /// @}
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        ThrottleManager tm = (ThrottleManager) getSystemService(Context.THROTTLE_SERVICE);
        mDataUsageListener = new DataUsageListener(this, mButtonDataUsage, prefSet);
        /// M: register receivers
        registerReceiver(mReceiver, mIntentFilter);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mExtension.removeNMMode(prefSet, mButtonPreferredNetworkMode, 
                mButtonPreferredGSMOnly, mButtonPreferredNetworkModeEx);
        setNetworkOperator();
        /// M: add unlock sim card receiver
        mCellConnMgr = new CellConnMgr();
        mCellConnMgr.register(this);
    }

    private void setNetworkOperator() {
        boolean isShowPlmn = false;
        if (CallSettings.isMultipleSim()) {
            List<SIMInfo> sims = SIMInfo.getInsertedSIMList(this);
            for (SIMInfo sim : sims) {
                isShowPlmn |= mGeminiPhone.isCspPlmnEnabled(sim.mSlot);
            }
        } else {
            isShowPlmn = mPhone.isCspPlmnEnabled();
        }
        mExtension.removeNMOp(getPreferenceScreen(), mCarrierSelPref, isShowPlmn);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        mAirplaneModeEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;
        mConnService = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnService != null) {
            mButtonDataEnabled.setChecked(mConnService.getMobileDataEnabled());
        } else {
            Log.d(LOG_TAG,"onResume, mConnService is null");
        }

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }
        mDataUsageListener.resume();
       
        //if the phone not idle state or airplane mode, then disable the preferenceScreen
        /// M: support for gemini phone
        if (CallSettings.isMultipleSim()) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), 
                    android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
            Log.d(LOG_TAG, "Settings.onResume(), mDualSimMode=" + mDualSimMode);
        }
        
        /// M: set RAT mode when on resume 
        if (mButtonPreferredNetworkMode != null) {
            mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());
        }
        
        ///M: add for data connection gemini and op01 only
        setDataConnPref();
        //Please make sure this is the last line!!
        setScreenEnabled();
        ///M: @{
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SIM_SETTING),
                false, mGprsDefaultSIMObserver);
        } else {
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Global.MOBILE_DATA),
                false, mGprsDefaultSIMObserver);
        }
        ///@}
    }
    private void setDataConnPref() {
        Log.d(LOG_TAG,"setDataConnPref");
        if (mDataConnPref != null) {
            mDataConnPref.setCellConnMgr(mCellConnMgr);
            boolean isEnabled = false;
            if (mConnService != null) {
                isEnabled = mConnService.getMobileDataEnabled();
            } else {
                Log.d(LOG_TAG,"mConnService is null");
            }
            long dataconnectionID = android.provider.Settings.System.getLong(getContentResolver(), 
                                    android.provider.Settings.System.GPRS_CONNECTION_SIM_SETTING,
                                    android.provider.Settings.System.DEFAULT_SIM_NOT_SET);
            List<SimItem> mSimItemListGprs = new ArrayList<SimItem>();
            List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
            Collections.sort(simList, new CallSettings.SIMInfoComparable());
            mSimItemListGprs.clear();
            SimItem simitem;
            int state = 0;
            int k = 0;
            TelephonyManagerEx mTelephonyManagerEx = TelephonyManagerEx.getDefault();
            for (SIMInfo siminfo: simList) {
                if (siminfo != null) {
                    simitem = new SimItem(siminfo);

                    try {    
                        ITelephony iTelephony = ITelephony.Stub.asInterface(
                                ServiceManager.getService(Context.TELEPHONY_SERVICE));
                        if (iTelephony != null) {      
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {    
                                state = iTelephony.getSimIndicatorStateGemini(siminfo.mSlot);
                            } else {
                                state = iTelephony.getSimIndicatorState();
                            }
                        }
                    } catch (android.os.RemoteException e) {
                        Log.d(LOG_TAG, "[e = " + e + "]");
                    }
  
                    simitem.mState = state;
                    Log.d(LOG_TAG, "state=" + simitem.mState);
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        if (siminfo.mSimId == dataconnectionID) {
                            mDataConnPref.setInitValue(k);
                            mDataConnPref.setSummary(siminfo.mDisplayName);
                        }        
                    } else {
                        Log.d(LOG_TAG,"-----isEnabled=" + isEnabled);
                        if (isEnabled) {
                            mDataConnPref.setInitValue(k);
                            mDataConnPref.setSummary(siminfo.mDisplayName);    
                        }
                    }
                    
                    mSimItemListGprs.add(simitem);
                }
                k++;
            }
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (dataconnectionID == android.provider.Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                    mDataConnPref.setInitValue(simList.size());
                    mDataConnPref.setSummary(R.string.service_3g_off);
                }    
            } else {
                if (!isEnabled) {
                    mDataConnPref.setInitValue(simList.size());
                    mDataConnPref.setSummary(R.string.service_3g_off);    
                }
            }
            simitem = new SimItem(null);
            if (isRadioOff()) {
                simitem.mState = PhoneConstants.SIM_INDICATOR_RADIOOFF;
            }
            mSimItemListGprs.add(simitem);  
            Log.d(LOG_TAG,"mSimItemListGprs=" + mSimItemListGprs.size());
            mDataConnPref.setInitData(mSimItemListGprs);   
        }
    }
    private boolean isRadioOff() {
        boolean isAllRadioOff = (Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                || (Settings.System.getInt(getContentResolver(),
                        Settings.System.DUAL_SIM_MODE_SETTING, -1) == 0);
        Log.d(LOG_TAG, "isAllRadioOff=" + isAllRadioOff);
        return isAllRadioOff;
    }
    /// M: show dialogs
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == PROGRESS_DIALOG) {
            dialog = new ProgressDialog(this);
            ((ProgressDialog)dialog).setMessage(getResources().getString(R.string.updating_settings));
            dialog.setCancelable(false);
        } else if (id == DATA_ENABLE_ALERT_DIALOG 
                || id == DATA_DISABLE_ALERT_DIALOG) {
            int message = (id == DATA_ENABLE_ALERT_DIALOG ?
                    R.string.networksettings_tips_data_enabled
                    : R.string.networksettings_tips_data_disabled);
            dialog = new AlertDialog.Builder(this)
                    .setMessage(getText(message))
                    .setTitle(com.android.internal.R.string.dialog_alert_title)
                    .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            mIsChangeData = true;
                            if (mConnService != null) {
                                showDialog(PROGRESS_DIALOG);
                                boolean isConnected = mConnService.getMobileDataEnabled();
                                mConnService.setMobileDataEnabled(!isConnected);
                                mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
                            }
                        }
                    })
                    .setNegativeButton(com.android.internal.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            if (getPreferenceScreen().findPreference(BUTTON_DATA_ENABLED_KEY)
                                    != null) {
                                Log.d(LOG_TAG,"setNegativeButton---restore mButtonDataEnabled to previous state");
                                mButtonDataEnabled.setChecked(!mButtonDataEnabled.isChecked());
                            } else {
                                setDataConnPref();
                            }       
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (getPreferenceScreen().findPreference(BUTTON_DATA_ENABLED_KEY)
                                    != null) {
                                Log.d(LOG_TAG,"restore mButtonDataEnabled to previous state");
                                mButtonDataEnabled.setChecked(!mButtonDataEnabled.isChecked());
                            } else {
                                setDataConnPref();
                            }     
                        }
                    })
                    .create();
        } else if (id == ROAMING_DIALOG) {
            dialog = new AlertDialog.Builder(this).setMessage(mExtension.getRoamingSummary(this,R.string.roaming_warning))
                    .setTitle(com.android.internal.R.string.dialog_alert_title)
                    .setIconAttribute(com.android.internal.R.attr.alertDialogIcon)
                    .setPositiveButton(com.android.internal.R.string.yes, this)
                    .setNegativeButton(com.android.internal.R.string.no, this)
                    .create();
            dialog.setOnDismissListener(this);
        } else if (id == DIALOG_GPRS_SWITCH_CONFIRM) {
            dialog = new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(android.R.string.dialog_alert_title)
            .setMessage(getResources().getString(mDataSwitchMsgIndex))
            .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSelectGprsIndex != -1) {
                            if ((mDataSwitchMsgIndex == R.string.gemini_3g_disable_warning_case0) 
                                || (mDataSwitchMsgIndex == R.string.gemini_3g_disable_warning_case2)) {
                                if (!enableDataRoaming(mSelectGprsIndex)) {
                                    mSelectGprsIndex = -1;
                                    return;
                                }
                            }
                            switchGprsDefautlSIM(mSelectGprsIndex);
                            mSelectGprsIndex = -1;
                        }
                    }
                })
            .setNegativeButton(com.android.internal.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setDataConnPref();
                    }
                })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    setDataConnPref();
                }
            })
            .create();
        }
        return dialog;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataUsageListener.pause();
        /// M: unregister Default SIM Observer 
        getContentResolver().unregisterContentObserver(mGprsDefaultSIMObserver);
    }
    /// M: add for support receiver & check sim lock
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mCellConnMgr.unregister();
        if (mPreCheckForRunning != null) {
            mPreCheckForRunning.deRegister();
        }
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
    }
    
    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE, PREFERRED_NETWORK_MODE);
            if (buttonNetworkMode != settingsNetworkMode) {
                /// M: when wait for network switch done show dialog    
                showDialog(PROGRESS_DIALOG);
                int modemNetworkMode;
                switch(buttonNetworkMode) {
                    case Phone.NT_MODE_GLOBAL:
                        modemNetworkMode = Phone.NT_MODE_GLOBAL;
                        break;
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                        modemNetworkMode = Phone.NT_MODE_EVDO_NO_CDMA;
                        break;
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                        modemNetworkMode = Phone.NT_MODE_CDMA_NO_EVDO;
                        break;
                    case Phone.NT_MODE_CDMA:
                        modemNetworkMode = Phone.NT_MODE_CDMA;
                        break;
                    case Phone.NT_MODE_GSM_UMTS:
                        modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                        break;
                    case Phone.NT_MODE_WCDMA_ONLY:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                        break;
                    case Phone.NT_MODE_GSM_ONLY:
                        modemNetworkMode = Phone.NT_MODE_GSM_ONLY;
                        break;
                    case Phone.NT_MODE_WCDMA_PREF:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        break;
                    default:
                        modemNetworkMode = Phone.PREFERRED_NT_MODE;
                }

                // If button has no valid selection && setting is LTE ONLY
                // mode, let the setting stay in LTE ONLY mode. UI is not
                // supported but LTE ONLY mode could be used in testing.
                if ((modemNetworkMode == Phone.PREFERRED_NT_MODE) &&
                    (settingsNetworkMode == Phone.NT_MODE_LTE_ONLY)) {
                    return true;
                }

                mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());

                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        buttonNetworkMode);
                /// M: support gemini Set the modem network mode
                if (CallSettings.isMultipleSim()) {
                    mGeminiPhone.setPreferredNetworkTypeGemini(modemNetworkMode, mHandler
                            .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), mSimId);
                } else {
                    mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                            .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
                }
            }
        } else if (preference == mDataConnPref) {
            /// M: @{
            long simId = ((Long) objValue).longValue();
            Log.d(LOG_TAG,"under click simId=" + simId);
            if (CallSettings.isMultipleSim()) {
                if (simId == 0) {
                    switchGprsDefautlSIM(0);
                    return true;
                }

                SIMInfo simInfo = getSimInfoById(simId);
                if (simInfo == null) {
                    return false;
                }
                boolean isInRoaming = mTelephonyManager.isNetworkRoamingGemini(simInfo.mSlot);
                mDataSwitchMsgIndex = -1;
                log("isInRoaming = " + isInRoaming);
                int slot3G = PhoneGlobals.getInstance().phoneMgr.get3GCapabilitySIM();
                boolean is3gOff = slot3G == -1;

                if (isInRoaming) {
                    boolean isRoamingDataAllowed = simInfo.mDataRoaming == SimInfo.DATA_ROAMING_ENABLE;
                    log("isRoamingDataAllowed = " + isRoamingDataAllowed);
                    if (isRoamingDataAllowed) {
                        if (simInfo.mSlot != slot3G && FeatureOption.MTK_GEMINI_3G_SWITCH) {
                            mDataSwitchMsgIndex = R.string.gemini_3g_disable_warning_case1;
                        }
                    } else if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                        if (simInfo.mSlot == slot3G) {
                            mDataSwitchMsgIndex = R.string.gemini_3g_disable_warning_case0;
                        } else {
                            mDataSwitchMsgIndex = R.string.gemini_3g_disable_warning_case2;
                        }
                    /// M: for alps00610011 @{
                    // when 3G switch do not open ,  we add this string.
                    //
                    // MTK add
                    } else {
                        mDataSwitchMsgIndex = R.string.gemini_3g_disable_warning_case0;
                    }
                    /// @}
                } else {
                    if (simInfo.mSlot != slot3G && FeatureOption.MTK_GEMINI_3G_SWITCH) {
                        mDataSwitchMsgIndex = R.string.gemini_3g_disable_warning_case1;
                    }
                }
                log("slot3G = " + slot3G);
                log("simInfo.mSlot = " + simInfo.mSlot);

                if (mDataSwitchMsgIndex == -1) {
                    switchGprsDefautlSIM(simId);
                } else {
                    mSelectGprsIndex = simId;
                    log("mSelectGprsIndex = " + mSelectGprsIndex);
                    removeDialog(DIALOG_GPRS_SWITCH_CONFIRM);
                    showDialog(DIALOG_GPRS_SWITCH_CONFIRM);
                }
            } else {
                dealWithDataConn(simId);
            }
            /// @}
        }
        return true;
    }
    /**
    *To turn on/off the data connection for single sim only (consistent_UI)
    *
    */
    private void dealWithDataConn(long simid) {
        boolean isDataEnabled = false;
        // simid = 0 means off is clicked
        if (simid != 0) {
            isDataEnabled = true;
        } 
        if (!mExtension.dataEnableReminder(isDataEnabled, this)) {
                Log.d(LOG_TAG,"onPreferenceTreeClick: preference == mButtonDataEnabled.");
                if (mConnService == null) {
                    Log.d(LOG_TAG,"mConnService is null");
                    return;
                }
                mConnService.setMobileDataEnabled(isDataEnabled);
                if (isDataEnabled &&
                    isNeedtoShowRoamingMsg()) {
                    mExtension.showWarningDlg(this,R.string.data_conn_under_roaming_hint);
                }
            }
    }
    /**
     * switch data connection default SIM
     * @param value: sim id of the new default SIM
     */
    private void switchGprsDefautlSIM(long simid) {
        if (simid < 0) {
            Log.d(LOG_TAG,"value=" + simid + " is an exceptions");
            return;
        }
        boolean isConnect = (simid > 0) ? true : false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            long gprsValue = Settings.System.getLong(getContentResolver(),
                Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            Log.d(LOG_TAG,"Current gprsValue=" + gprsValue + " and target value=" + simid);
            if (simid == gprsValue) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
            intent.putExtra("simid", simid);
            sendBroadcast(intent);
        } else {
            mConnService.setMobileDataEnabled(isConnect);
        }
        showDialog(PROGRESS_DIALOG);
        mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
        mIsChangeData = true;
    }
    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
                default:
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    log("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        PREFERRED_NETWORK_MODE);

                if (DBG) {
                    log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL) {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) { log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode);
                    }
                    /// M: add for wcdma prefer feature
                    if (modemNetworkMode == Phone.NT_MODE_GSM_UMTS) {
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        settingsNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                    }

                    // changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                    mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());
                } else if (modemNetworkMode == Phone.NT_MODE_LTE_ONLY) {
                    // LTE Only mode not yet supported on UI, but could be used for testing
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: lte only: no action");
                    }
                } else {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                    }
                    resetNetworkModeToDefault();
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
             /// M: when set network mode show wait dialog
            removeDialog(PROGRESS_DIALOG);
            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mButtonPreferredNetworkMode.getValue()).intValue();
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        networkMode);
            } else {
                 /// M: support gemini phone
                if (CallSettings.isMultipleSim()) {
                    mGeminiPhone.getPreferredNetworkTypeGemini(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE), mSimId);
                } else {
                    mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
                }
            }
        }

        private void resetNetworkModeToDefault() {
            //set the mButtonPreferredNetworkMode
            mButtonPreferredNetworkMode.setValue(Integer.toString(PREFERRED_NETWORK_MODE));
            //set the Settings.System
            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        PREFERRED_NETWORK_MODE);
            /// M: support gemini Set the Modem
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.setPreferredNetworkTypeGemini(PREFERRED_NETWORK_MODE,
                        obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), mSimId);
            } else {
                mPhone.setPreferredNetworkType(PREFERRED_NETWORK_MODE,
                        obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
    }
    ///M: add for AT&T
    private boolean isNeedtoShowRoamingMsg() {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isInRoaming = telMgr.isNetworkRoaming();
        boolean isRoamingEnabled = mPhone.getDataRoamingEnabled();
        Log.d(LOG_TAG,"***isInRoaming=" + isInRoaming + " isRoamingEnabled=" + isRoamingEnabled);
        return (isInRoaming && !isRoamingEnabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            }
            break;

        default:
            break;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /// M: get slot base band 
    private int getBaseBand(int slot) {
        int value = 0;
        String propertyKey = "gsm.baseband.capability";
        String capability = null;
        if (slot == 1) {
            propertyKey += "2";
        }
        capability = SystemProperties.get(propertyKey);
        if (capability == null || "".equals(capability)) {
            return value;
        }
        
        try {
            value = Integer.valueOf(capability, 16);
        } catch (NumberFormatException ne) {
            log("parse value of basband error");
        }
        return value;        
    }
    /// M: when receive data change broadcast get the extra    
    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }
    private void setScreenEnabled() {
        boolean isShouldEnabled = false;
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        ///M: add for hot swap {
        List<SIMInfo> sims = SIMInfo.getInsertedSIMList(this);
        boolean isHasSimCard = ((sims != null) && (sims.size() > 0));
        isShouldEnabled = isIdle && (!mAirplaneModeEnabled) && (mDualSimMode != 0) && isHasSimCard;
        ///@}
        getPreferenceScreen().setEnabled(isShouldEnabled);
        mExtension.disableDataRoaming(mButtonDataRoam,mButtonDataEnabled.isChecked());
        boolean isGeminiMode = CallSettings.isMultipleSim();
        boolean isSupport3GSwitch = PhoneUtils.isSupportFeature("3G_SWITCH");
        if (mPreference3GSwitch != null) {
            mPreference3GSwitch.setEnabled(isHasSimCard && isShouldEnabled);
        }
        if (mButtonPreferredNetworkMode != null) {
            boolean isNWModeEnabled = isShouldEnabled && CallSettings.isRadioOn(PhoneConstants.GEMINI_SIM_1);
            mButtonPreferredNetworkMode.setEnabled(isNWModeEnabled);
            if (!isNWModeEnabled) {
                Dialog dialog = mButtonPreferredNetworkMode.getDialog();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }

        if (mButtonPreferredNetworkModeEx != null) {
            boolean isNWModeEnabled = false;
            int[] slot3G = CallSettings.get3GSimCardSlots(this);
            if (slot3G != null) {
                for (int i = 0; i < slot3G.length; i++) {
                    if (CallSettings.isRadioOn(slot3G[i])) {
                        isNWModeEnabled = true;
                        log("slot " + slot3G[i] + " radio state is = " + isNWModeEnabled);
                    }
                }
            }
            isNWModeEnabled = isShouldEnabled && isNWModeEnabled;
            log("isNWModeEnabled = " + isNWModeEnabled);
            mButtonPreferredNetworkModeEx.setEnabled(isNWModeEnabled);
        }
        if (isShouldEnabled) {
            if (mConnService != null) {
                NetworkInfo networkInfo = mConnService.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                if (networkInfo != null) {
                    NetworkInfo.State state = networkInfo.getState();
                    log("mms state = " + state);
                    mDataConnPref.setEnabled(state != NetworkInfo.State.CONNECTING
                        && state != NetworkInfo.State.CONNECTED);
                }
            }
        }
    }

    /**
     * M: get simInfo by simId
     * @param simId: sim Id
     */
    private SIMInfo getSimInfoById(long simId) {
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        for (SIMInfo simInfo : simList) {
            if (simId == simInfo.mSimId) {
                return simInfo;
            }
        }
        return null;
    }

    /**
     * M: enable data roaming by simId
     * @param simId: sim Id
     */
    private boolean enableDataRoaming(long simId) {
        int slotId = SIMInfo.getSlotById(this, simId);
        log("enableDataRoaming with SimId=" + simId + ", slotId=" + slotId);
        if (GeminiUtils.isValidSlot(slotId)){
            try {
                ITelephony iTelephony = ITelephony.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE));
                if (iTelephony != null) {
                    iTelephony.setDataRoamingEnabledGemini(true, slotId);
                }
            } catch (RemoteException e) {
                log("iTelephony exception");
                return false;
            }
            SIMInfo.setDataRoaming(this, SimInfo.DATA_ROAMING_ENABLE, simId);
            return true;
        } else {
            log("enableDataRoaming error: slotId is not valid, the SIM card may be pulled out.");
            return false;
        }
    }
}
