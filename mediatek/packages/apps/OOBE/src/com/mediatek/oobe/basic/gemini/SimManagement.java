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

package com.mediatek.oobe.basic.gemini;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.telephony.TelephonyManager;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.oobe.ext.ISimManagementExt;
import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.SettingsPreferenceFragment;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SimItem {
    public boolean mIsSim = true;
    public String mName = null;
    public String mNumber = null;
    public int mDispalyNumberFormat = 0;
    public int mColor = -1;
    public int mSlot = -1;
    public long mSimID = -1;
    public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;

    // Constructor for not real sim
    public SimItem(String name, int color, long simID) {
        mName = name;
        mColor = color;
        mIsSim = false;
        mSimID = simID;
    }

    // constructor for sim
    public SimItem(SIMInfo siminfo) {
        mIsSim = true;
        mName = siminfo.mDisplayName;
        mNumber = siminfo.mNumber;
        mDispalyNumberFormat = siminfo.mDispalyNumberFormat;
        mColor = siminfo.mColor;
        mSlot = siminfo.mSlot;
        mSimID = siminfo.mSimId;
    }
}

public class SimManagement extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener,
        SimInfoEnablePreference.OnPreferenceClickCallback {

    private static final String TAG = "SimManagementSettings";
    private static final String KEY_SIM_INFO_CATEGORY = "sim_info";
    private static final String KEY_GENERAL_SETTINGS_CATEGORY = "general_settings";
    private static final String KEY_DEFAULT_SIM_SETTINGS_CATEGORY = "default_sim";
    private static final String KEY_SIM_CONTACTS_SETTINGS = "contacts_sim";
    private static final String KEY_VOICE_CALL_SIM_SETTING = "voice_call_sim_setting";

    private static final String KEY_VIDEO_CALL_SIM_SETTING = "video_call_sim_setting";
    private static final String KEY_SMS_SIM_SETTING = "sms_sim_setting";
    private static final String KEY_GPRS_SIM_SETTING = "gprs_sim_setting";

    // time out message event
    private static final int EVENT_DETACH_TIME_OUT = 2000;
    private static final int EVENT_ATTACH_TIME_OUT = 2001;
    // time out length
    private static final int DETACH_TIME_OUT_LENGTH = 10000;
    private static final int ATTACH_TIME_OUT_LENGTH = 30000;

    private static final int PIN1_REQUEST_CODE = 302;
    private static final int VEDIO_CALL_OFF = -1;

    private static int sProgressbarMaxSize;
    private static int sCurrentStep;

    private Map<Long, Integer> mSimIdToIndexMap;

    private static boolean sScreenEnable = true;
    private static boolean sAllSimRadioOff = false;
    private static boolean sHasSim = false;
    private static boolean sGprsTargSim = false;
    private static boolean sVTCallSupport = true;
    private static boolean mIsVoiceCapable = false;
    private static boolean sVoipAvailable = true;
    private boolean mIsSmsCapable = false;
    private boolean mIs3gOff = false;
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    private static final String MMS_TRANSACTION = "mms.transaction";

    private Map<Long, SIMInfo> mSimMap;

    private int mDualSimMode = 0;

    // private long SIM_ID_INVALID = -5;

    private long mVTTargetTemp;
    private ISimManagementExt mExt;

    private boolean mIsSlot1Insert = false;
    private boolean mIsSlot2Insert = false;

    private static final int VOICE_CALL_SIM_INDEX = 0;
    private static final int VIDEO_CALL_SIM_INDEX = 1;
    private static final int SMS_SIM_INDEX = 2;
    private static final int GPRS_SIM_INDEX = 3;

    private static final int TYPE_SIM_NAME = 0;
    private static final int TYPE_SIM_COLOR = 1;
    private static final int TYPE_SIM_NUMBER = 2;
    private static final int TYPE_SIM_NUMBER_FORMAT = 3;

    //SIM MODE
    private static final int SIM_GENIMI_MODE_SIM1 = 1;
    private static final int SIM_GENIMI_MODE_SIM2 = 2;
    private static final int SIM_GENIMI_MODE_DUAL = 3;
    
    private static final int DIALOG_ACTIVATE = 1000;
    private static final int DIALOG_DEACTIVATE = 1001;
    private static final int DIALOG_WAITING = 1004;
    private static final int DIALOG_NETWORK_MODE_CHANGE = 1005;
    private static final int DIALOG_3G_MODEM_SWITCHING = 1006;
    private static final int DIALOG_3G_MODEM_SWITCH_CONFIRM = 1007;
    private static final int DIALOG_GPRS_SWITCH_CONFIRM = 1008;

    private SimInfoEnablePreference mSlot1SimPref;
    private SimInfoEnablePreference mSlot2SimPref;

    private DefaultSimPreference mVoiceCallSimSetting;
    private DefaultSimPreference mVideoCallSimSetting;
    private DefaultSimPreference mSmsSimSetting;
    private DefaultSimPreference mGprsSimSetting;

    // private PreferenceScreen mSimAndContacts;

    private TelephonyManagerEx mTelephonyManagerEx;
    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;
    private StatusBarManager mStatusBarManager;

    private static ContentObserver sGprsDefaultSIMObserver;

    private static final int EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE = 1;

    private List<SimItem> mSimItemListVoice = new ArrayList<SimItem>();
    private List<SimItem> mSimItemListVideo = new ArrayList<SimItem>();
    private List<SimItem> mSimItemListSms = new ArrayList<SimItem>();
    private List<SimItem> mSimItemListGprs = new ArrayList<SimItem>();

    private boolean mIsSIMModeSwitching = false;
    private boolean mIsGprsSwitching = false;
    // private boolean mIsModemSwitching = false;

    private IntentFilter mIntentFilter;

    // -1: none; 0: radio on; 1 radio off; 2: data switching; 3: modem switch
    private static final int DIALOG_NONE = -1;
    private static final int DIALOG_RADIO_ON = 1100;
    private static final int DIALOG_RADIO_OFF = 1101;
    private static final int DIALOG_DATA_SWITCH = 1102;
    private static final int DIALOG_MODEN_SWITCH = 1103;
    private static final int DIALOG_NETWORK_MODE = 1104; 
    private int mIsShowDlg = DIALOG_NONE;
    private static final int ALL_RADIO_OFF = 0;

    private int[] mDataSwitchMsgStr = { 
            R.string.gemini_3g_disable_warning_case0, 
            R.string.gemini_3g_disable_warning_case1,
            R.string.gemini_3g_disable_warning_case2,
            R.string.gemini_3g_disable_warning_case3,
            R.string.gemini_3g_disable_warning_case4 };

    private static final int DATA_SWITCH_MSG_CASE0 = 0;
    private static final int DATA_SWITCH_MSG_CASE1 = 1;
    private static final int DATA_SWITCH_MSG_CASE2 = 2;
    private static final int DATA_SWITCH_MSG_CASE3 = 3;
    private static final int DATA_SWITCH_MSG_CASE4 = 4;
    
    private int mDataSwitchMsgIndex = -1;
    private CellConnMgr mCellConnMgr;

    private Runnable mServiceComplete = new Runnable() {
        @Override
        public void run() {
            //
        }
    };

    private Handler mDualSimModeChangedHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE:
                if (getActivity() == null) {
                    Xlog.i(TAG, "getActivity is null!");
                    return;
                }
                Xlog.i(TAG, "dual sim mode changed!");
                dealWithSwtichComplete();
                break;
            default:
                break;

            }
        }
    };

    private Messenger mMessenger = new Messenger(mDualSimModeChangedHander);
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { //MTK_CS_IGNORE_THIS_LINE

            String action = intent.getAction();
            Xlog.d(TAG, "receiver:" + action);
            if (action.equals(Intent.SIM_SETTINGS_INFO_CHANGED)) {

                long simid = intent.getLongExtra("simid", -1);
                int type = intent.getIntExtra("type", -1);
                Xlog.i(TAG, "receiver: Intent.SIM_SETTINGS_INFO_CHANGED");
                Xlog.i(TAG, "type is " + type + " simid is " + simid);
                updateSimInfo(simid, type);
                updateDefaultSimInfo(simid);

            } else if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                int slotId = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
                int simStatus = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, -1);
                Xlog.i(TAG, "slotid is " + slotId + "status is " + simStatus);

                if ((slotId >= 0) && (simStatus >= 0)) {
                    updateSimState(slotId, simStatus);
                    updateDefaultSimState(slotId, simStatus);
                }

            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                disableSimPrefs();
                
            } else if (action.equals(TelephonyIntents.ACTION_SIM_NAME_UPDATE)) {
                int slotid = intent.getIntExtra("simId", -1);

                if (slotid < 0) {
                    return;
                }

                SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(context, slotid);
                if (siminfo != null) {
                    long simID = siminfo.mSimId;
                    Xlog.i(TAG, "slotid is " + slotid);
                    updateSimInfo(simID, 0);
                    updateDefaultSimInfo(simID);
                }

            } else if (action.equals(TRANSACTION_START)) {
                Xlog.i(TAG, "receiver: TRANSACTION_START");
                sScreenEnable = false;
                mGprsSimSetting.setEnabled(!sAllSimRadioOff && sScreenEnable && sHasSim);
                Dialog dlg = mGprsSimSetting.getDialog();
                if (dlg != null) {
                    if (dlg.isShowing()) {
                        dlg.dismiss();
                    }
                }
            } else if (action.equals(TRANSACTION_STOP)) {
                Xlog.i(TAG, "receiver: TRANSACTION_STOP");
                sScreenEnable = true;
                mGprsSimSetting.setEnabled(!sAllSimRadioOff && sScreenEnable && sHasSim);
                Dialog dlg = mGprsSimSetting.getDialog();
                if (dlg != null) {
                    if (dlg.isShowing()) {
                        dlg.dismiss();
                    }
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {

                sAllSimRadioOff = intent.getBooleanExtra("state", false)
                        || (Settings.System.getInt(context.getContentResolver(),
                                Settings.System.DUAL_SIM_MODE_SETTING, -1) == 0);
                Xlog.i(TAG, "airplane mode changed to " + sAllSimRadioOff);
                initDefaultSimPreference();
            } else if (action.equals(GeminiPhone.EVENT_3G_SWITCH_DONE)) {

                Xlog.i(TAG, "receiver: GeminiPhone.EVENT_3G_SWITCH_DONE");

                if (mIsShowDlg == DIALOG_MODEN_SWITCH) {
                    mIsShowDlg = DIALOG_NONE;
                    if (isResumed()) {
                        removeDialog(DIALOG_3G_MODEM_SWITCHING);
                    }

                }
                // mIsModemSwitching = false;
                if (mStatusBarManager != null) {
                    mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);

                }

                updateVideoCallDefaultSIM();

            } else if (action.equals(GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED)) {

                Xlog.i(TAG, "receiver: GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED");

                boolean lockState = intent.getBooleanExtra(GeminiPhone.EXTRA_3G_SWITCH_LOCKED, false);
                mVideoCallSimSetting.setEnabled(!(mIs3gOff || lockState || (!sHasSim)));
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                // Get reason from this intent
                String reason = intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY);
                String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                PhoneConstants.DataState state = getMobileDataState(intent);

                int simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                Xlog.i(TAG, "mDataConnectionReceiver simId is : " + simId);
                Xlog.i(TAG, "mDataConnectionReceiver state is : " + state);
                Xlog.i(TAG, "mDataConnectionReceiver reason is : " + reason);
                Xlog.i(TAG, "mDataConnectionReceiver apn type is : " + apnTypeList);

                if (reason == null || (!PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList))) {
                    return;
                }

                if (reason.equals(Phone.REASON_DATA_ATTACHED) && (state == PhoneConstants.DataState.CONNECTED)
                        && (mIsGprsSwitching)) {
                    mTimeHandler.removeMessages(EVENT_ATTACH_TIME_OUT);

                    if (mIsShowDlg == DIALOG_DATA_SWITCH) {
                        mIsShowDlg = DIALOG_NONE;
                        if (isResumed()) {
                            removeDialog(DIALOG_WAITING);
                        }
                    }

                    updateGprsSettings();
                    mIsGprsSwitching = false;

                } else if (reason.equals(Phone.REASON_DATA_DETACHED) && (state == PhoneConstants.DataState.DISCONNECTED)
                        && (mIsGprsSwitching)) {

                    if (!sGprsTargSim) {
                        mTimeHandler.removeMessages(EVENT_DETACH_TIME_OUT);
                        if (mIsShowDlg == DIALOG_DATA_SWITCH) {
                            mIsShowDlg = DIALOG_NONE;
                            if (isResumed()) {
                                removeDialog(DIALOG_WAITING);
                            }
                        }
                        updateGprsSettings();
                        mIsGprsSwitching = false;
                    }

                }

            }
        }
    };

    protected void updateGprsSettings() {
        long dataconnectionID = Settings.System.getLong(getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING,
                Settings.System.DEFAULT_SIM_NOT_SET);
        Xlog.i(TAG, "dataconnectionID =" + dataconnectionID);

        if (dataconnectionID > 0) {
            Integer intIndex = mSimIdToIndexMap.get(dataconnectionID);
            if (intIndex == null) {
                return;
            }
            int index = intIndex.intValue();
            SIMInfo siminfo = mSimMap.get(dataconnectionID);
            if ((index >= 0) && (siminfo != null)) {

                mGprsSimSetting.setInitValue(index);
                mGprsSimSetting.setSummary(siminfo.mDisplayName);

            }
        } else if (dataconnectionID == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
            int nSim = mSimMap.size();
            mGprsSimSetting.setInitValue(nSim);
            mGprsSimSetting.setSummary(R.string.gemini_default_sim_never);

        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) { //MTK_CS_IGNORE_THIS_LINE
        super.onCreate(savedInstanceState);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mIsVoiceCapable = mTelephonyManager.isVoiceCapable();

        if ((!FeatureOption.MTK_VT3G324M_SUPPORT) || (!FeatureOption.MTK_GEMINI_3G_SWITCH) 
                || !mIsVoiceCapable) {
            sVTCallSupport = false;
        }
        Xlog.d(TAG, "sVTCallSupport = " + sVTCallSupport);
        int voipEnable = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.ENABLE_INTERNET_CALL, 0);
        sVoipAvailable = SipManager.isVoipSupported(getActivity()) && (voipEnable != 0);

        addPreferencesFromResource(R.xml.sim_management);
        // /M: plug in of sim management
        mExt = Utils.getSimManagmentExtPlugin(this.getActivity());
        
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);

        mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mIntentFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TRANSACTION_START);
        mIntentFilter.addAction(TRANSACTION_STOP);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_NAME_UPDATE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);

        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            mIntentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_DONE);
            mIntentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED);
        }

        mSimMap = new HashMap<Long, SIMInfo>();
        mSimIdToIndexMap = new HashMap<Long, Integer>();

        // mSimAndContacts = (PreferenceScreen)findPreference(KEY_SIM_CONTACTS_SETTINGS);
        mVoiceCallSimSetting = (DefaultSimPreference) findPreference(KEY_VOICE_CALL_SIM_SETTING);
        mSmsSimSetting = (DefaultSimPreference) findPreference(KEY_SMS_SIM_SETTING);
        mGprsSimSetting = (DefaultSimPreference) findPreference(KEY_GPRS_SIM_SETTING);
        mVideoCallSimSetting = (DefaultSimPreference) findPreference(KEY_VIDEO_CALL_SIM_SETTING);
        setIconForDefaultSimPref();

        mVoiceCallSimSetting.setType(GeminiUtils.TYPE_VOICECALL);
        mSmsSimSetting.setType(GeminiUtils.TYPE_SMS);
        mGprsSimSetting.setType(GeminiUtils.TYPE_GPRS);

        mVoiceCallSimSetting.setOnPreferenceChangeListener(this);
        mSmsSimSetting.setOnPreferenceChangeListener(this);
        mGprsSimSetting.setOnPreferenceChangeListener(this);

        if (mVideoCallSimSetting != null) {
            if (sVTCallSupport) {
                mVideoCallSimSetting.setType(GeminiUtils.TYPE_VIDEOCALL);
                mVideoCallSimSetting.setOnPreferenceChangeListener(this);
            } else {
                PreferenceGroup defaultSIMSettingsCategory = 
                        (PreferenceGroup) findPreference(KEY_DEFAULT_SIM_SETTINGS_CATEGORY);

                if (defaultSIMSettingsCategory != null) {
                    defaultSIMSettingsCategory.removePreference(mVideoCallSimSetting);
                }
            }

        }

        initSimMap();

        int nSimNum = mSimMap.size();
        if (nSimNum > 0) {
            sHasSim = true;
            try {
                if (mITelephony != null) {
                    mITelephony.registerForSimModeChange(mMessenger.getBinder(), EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE);

                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mITelephony exception");
                return;
            }

            getActivity().registerReceiver(mSimReceiver, mIntentFilter);
            addSimInfoPreference();
            if (sVTCallSupport && (!FeatureOption.MTK_GEMINI_3G_SWITCH) && (!mIsSlot1Insert)) {
                mVideoCallSimSetting.setEnabled(false);
            }

            mCellConnMgr = new CellConnMgr(mServiceComplete);

            mCellConnMgr.register(getActivity());

            mGprsSimSetting.setCellConnMgr(mCellConnMgr);

        } else {
            sHasSim = false;
            addNoSimIndicator();

            getPreferenceScreen().setEnabled(false);
            mVoiceCallSimSetting.setEnabled(true);

        }

        sGprsDefaultSIMObserver = new ContentObserver(new Handler()) {

            @Override
            public void onChange(boolean selfChange) {
                Xlog.i(TAG, "Gprs connection SIM changed");
                long dataconnectionID = Settings.System.getLong(getContentResolver(),
                        Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);

                updateDefaultSimValue(GeminiUtils.TYPE_GPRS, dataconnectionID);
            }
        };

        if (savedInstanceState != null) {
            Xlog.d(TAG, "saved instance not null ,means we need init default sim preference to avoid problems");
            initDefaultSimPreference();
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSimMap.size() > 0) {
            try {
                if (mITelephony != null) {
                    mITelephony.unregisterForSimModeChange(mMessenger.getBinder());
                }

            } catch (RemoteException e) {
                Xlog.e(TAG, "mITelephony exception");
                return;
            }
            getActivity().unregisterReceiver(mSimReceiver);
            mCellConnMgr.unregister();
        }

        mDualSimMode = Settings.System.getInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1);
        Xlog.i(TAG, "has attach msg = " + mTimeHandler.hasMessages(EVENT_ATTACH_TIME_OUT));
        Xlog.i(TAG, "has detach msg = " + mTimeHandler.hasMessages(EVENT_DETACH_TIME_OUT));
        Xlog.i(TAG, "has sim mode msg = " + mDualSimModeChangedHander.hasMessages(EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE));
        mTimeHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
        mTimeHandler.removeMessages(EVENT_DETACH_TIME_OUT);
        mDualSimModeChangedHander.removeMessages(EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE);
        Xlog.i(TAG, "onDestroy: mDualSimMode value is : " + mDualSimMode);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // TODO Auto-generated method stub
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onStart() {

        super.onStart();
        getListView().setItemsCanFocus(true);
    }

    @Override
    public void onResume() { //MTK_CS_IGNORE_THIS_LINE
        // TODO Auto-generated method stub
        super.onResume();
        int voipEnable = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.ENABLE_INTERNET_CALL, 0);
        sVoipAvailable = SipManager.isVoipSupported(getActivity()) && (voipEnable != 0);
        mIsSmsCapable = mTelephonyManager.isSmsCapable();

        if (mSimMap.size() >= 0) {

            ContentResolver resolver = getContentResolver();

            if ((resolver != null) && (sGprsDefaultSIMObserver != null)) {
                resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SIM_SETTING),
                        false, sGprsDefaultSIMObserver);
            }

            long voicecallID = Settings.System.getLong(getContentResolver(), Settings.System.VOICE_CALL_SIM_SETTING,
                    Settings.System.DEFAULT_SIM_NOT_SET);
            Xlog.i(TAG, "voicecallID =" + voicecallID);
            // long videocallID = Settings.System.getLong(getContentResolver(),
            // Settings.System.VIDEO_CALL_SIM_SETTING,Settings.System.DEFAULT_SIM_NOT_SET);
            // Xlog.i(TAG, "videocallID =" +videocallID);

            int videocallSlotID = VEDIO_CALL_OFF;

            if (!FeatureOption.MTK_GEMINI_3G_SWITCH) {
                videocallSlotID = PhoneConstants.GEMINI_SIM_1;
            } else {
                try {
                    if (mITelephony != null) {
                        videocallSlotID = mITelephony.get3GCapabilitySIM();
                        GeminiUtils.sG3SloteID = videocallSlotID;
                        Xlog.i(TAG, "videocallSlotID =" + videocallSlotID);

                    }
                } catch (RemoteException e) {
                    Xlog.e(TAG, "mITelephony exception");
                    return;
                }
            }

            long smsID = Settings.System.getLong(getContentResolver(), Settings.System.SMS_SIM_SETTING,
                    Settings.System.DEFAULT_SIM_NOT_SET);
            Xlog.i(TAG, "smsID =" + smsID);
            long dataconnectionID = Settings.System.getLong(getContentResolver(),
                    Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            Xlog.i(TAG, "dataconnectionID =" + dataconnectionID);

            int k = 0;
            List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
            for (SIMInfo siminfo : simList) {
                if (siminfo != null) {

                    if (siminfo.mSimId == voicecallID) {
                        mVoiceCallSimSetting.setInitValue(k);
                        mVoiceCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                    Xlog.i(TAG, "siminfo.mSlot  = " + siminfo.mSlot);
                    if (sVTCallSupport && (siminfo.mSlot == videocallSlotID)) {
                        Xlog.i(TAG, "set init video call" + k);
                        if (!FeatureOption.MTK_GEMINI_3G_SWITCH) {
                            mVideoCallSimSetting.setInitValue(0);
                        } else {
                            mVideoCallSimSetting.setInitValue(k);
                        }
                        mVideoCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                    if (siminfo.mSimId == smsID) {
                        mSmsSimSetting.setInitValue(k);
                        mSmsSimSetting.setSummary(siminfo.mDisplayName);
                    }
                    if (siminfo.mSimId == dataconnectionID) {
                        mGprsSimSetting.setInitValue(k);
                        mGprsSimSetting.setSummary(siminfo.mDisplayName);
                    }
                    String key = String.valueOf(siminfo.mSimId);

                    SimInfoEnablePreference simInfoPref = (SimInfoEnablePreference) findPreference(key);
                    if ((simInfoPref != null) && (mITelephony != null)) {
                        try {
                            boolean isRadioOn = mITelephony.isRadioOnGemini(siminfo.mSlot);
                            simInfoPref.setCheck(isRadioOn);
                            simInfoPref.setRadioOn(isRadioOn);
                        } catch (RemoteException e) {
                            Xlog.e(TAG, "mITelephony exception");
                            return;
                        }

                    }

                }
                k++;
            }

            int nSim = simList.size();
            if (voicecallID == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                mVoiceCallSimSetting.setInitValue(nSim);
                mVoiceCallSimSetting.setSummary(R.string.gemini_intenet_call);
            } else if (voicecallID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {

                mVoiceCallSimSetting.setInitValue(sVoipAvailable ? (nSim + 1) : nSim);
                mVoiceCallSimSetting.setSummary(R.string.gemini_default_sim_always_ask);
            } else if (voicecallID == Settings.System.DEFAULT_SIM_NOT_SET) {
                mVoiceCallSimSetting.setSummary(R.string.apn_not_set);
            }

            if (smsID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                mSmsSimSetting.setInitValue(nSim);
                mSmsSimSetting.setSummary(R.string.gemini_default_sim_always_ask);
            } else if (smsID == Settings.System.DEFAULT_SIM_NOT_SET) {
                mSmsSimSetting.setSummary(R.string.apn_not_set);
            } else {
                if (mExt.isNeedsetAutoItem() && (smsID == Settings.System.SMS_SIM_SETTING_AUTO)){
                    mExt.setPrefProperty(mSmsSimSetting, smsID);
                    mSmsSimSetting.setInitValue(nSim + 1); 
                 }
                 
             }

            if (dataconnectionID == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                mGprsSimSetting.setInitValue(nSim);
                mGprsSimSetting.setSummary(R.string.gemini_default_sim_never);
            } else if (dataconnectionID == Settings.System.DEFAULT_SIM_NOT_SET) {
                mGprsSimSetting.setSummary(R.string.apn_not_set);
            }

            if (sVTCallSupport) {

                if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                    if (videocallSlotID == VEDIO_CALL_OFF) {
                        mIs3gOff = true;
                        mVideoCallSimSetting.setSummary(R.string.gemini_default_sim_3g_off);
                    } else {
                        mIs3gOff = false;
                    }

                    try {
                        if (mITelephony != null) {

                            mVideoCallSimSetting.setEnabled(!(mIs3gOff || mITelephony.is3GSwitchLocked() || (!sHasSim)));
                            Xlog.i(TAG, "mITelephony.is3GSwitchLocked() is " + mITelephony.is3GSwitchLocked());

                        }
                    } catch (RemoteException e) {
                        Xlog.e(TAG, "mITelephony exception");
                        return;
                    }
                } else {
                    long videocallID = Settings.System.getLong(getContentResolver(), Settings.System.VIDEO_CALL_SIM_SETTING,
                            Settings.System.DEFAULT_SIM_NOT_SET);

                    if (videocallID == Settings.System.DEFAULT_SIM_NOT_SET) {
                        mVideoCallSimSetting.setSummary(R.string.apn_not_set);
                    }

                }

            }

            sScreenEnable = (Settings.System.getInt(this.getContentResolver(), MMS_TRANSACTION, 0) == 0) ? true : false;
            sAllSimRadioOff = (Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                    || (Settings.System.getInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1) == 0);

            // when there is an call, disable this item

            mGprsSimSetting.setEnabled(!sAllSimRadioOff && sScreenEnable && sHasSim);
            Xlog.i(TAG, "mGprsSimSetting.setEnabled = " + mGprsSimSetting.isEnabled() + " in onResume");
            Xlog.i(TAG, "sAllSimRadioOff = " + sAllSimRadioOff + " sScreenEnable = " + sScreenEnable + " sHasSim = "
                    + sHasSim);

        }

        // deal with the problem that dialog fragment could not be dismissed after onSaveInstanceState

        switch (mIsShowDlg) {
        case DIALOG_RADIO_OFF:
            showDialog(DIALOG_ACTIVATE);
            setCancelable(false);
            break;
        case DIALOG_RADIO_ON:
            showDialog(DIALOG_DEACTIVATE);
            setCancelable(false);
            break;
        case DIALOG_DATA_SWITCH:
            showDialog(DIALOG_WAITING);
            setCancelable(false);
            break;
        case DIALOG_MODEN_SWITCH:
            showDialog(DIALOG_3G_MODEM_SWITCHING);
            setCancelable(false);
            break;
        case DIALOG_NETWORK_MODE:
            showDialog(DIALOG_NETWORK_MODE_CHANGE);
            setCancelable(false);
            break;
        default:
            break;
        }
        initDefaultSimPreference();
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        // deal with the problem that dialog fragment could not be dismissed after onSaveInstanceState

        switch (mIsShowDlg) {
        case DIALOG_RADIO_OFF:
            removeDialog(DIALOG_ACTIVATE);
            break;
        case DIALOG_RADIO_ON:
            removeDialog(DIALOG_DEACTIVATE);
            break;
        case DIALOG_DATA_SWITCH:
            removeDialog(DIALOG_WAITING);
            break;
        case DIALOG_MODEN_SWITCH:
            removeDialog(DIALOG_3G_MODEM_SWITCHING);
            break;
        case DIALOG_NETWORK_MODE:
            removeDialog(DIALOG_NETWORK_MODE_CHANGE);
            break;
        default:
            break;
        }
        if (mSimMap.size() >= 0) {
            ContentResolver resolver = getContentResolver();

            if ((resolver != null) && (sGprsDefaultSIMObserver != null)) {
                resolver.unregisterContentObserver(sGprsDefaultSIMObserver);
            }
        }

    }

    private void updateDefaultSIMSummary(DefaultSimPreference pref, Long simid) {

        if (simid > 0) {
            SIMInfo siminfo = mSimMap.get(simid);

            if (siminfo != null) {
                pref.setSummary(siminfo.mDisplayName);
            }

        } else if (simid == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
            pref.setSummary(R.string.gemini_intenet_call);
        } else if (simid == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
            pref.setSummary(R.string.gemini_default_sim_always_ask);
        } else if (simid == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
            pref.setSummary(R.string.gemini_default_sim_never);
        } else if (simid == Settings.System.SMS_SIM_SETTING_AUTO) {
            mExt.updateDefaultSIMSummary(pref, simid);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) { //MTK_CS_IGNORE_THIS_LINE
        Xlog.i(TAG, "Enter onPreferenceChange function.");

        final String key = arg0.getKey();
        // TODO Auto-generated method stub
        if (KEY_VOICE_CALL_SIM_SETTING.equals(key)) {

            Settings.System.putLong(getContentResolver(), Settings.System.VOICE_CALL_SIM_SETTING, (Long) arg1);

            Intent intent = new Intent(Intent.ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED);
            intent.putExtra("simid", (Long) arg1);
            getActivity().sendBroadcast(intent);
            updateDefaultSIMSummary(mVoiceCallSimSetting, (Long) arg1);
        } else if (KEY_VIDEO_CALL_SIM_SETTING.equals(key)) {

            if (FeatureOption.MTK_GEMINI_3G_SWITCH) {

                mVTTargetTemp = mVideoCallSimSetting.getValue();
                showDialog(DIALOG_3G_MODEM_SWITCH_CONFIRM);
                setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        updateVideoCallDefaultSIM();
                    }
                });
            }

        } else if (KEY_SMS_SIM_SETTING.equals(key)) {

            Settings.System.putLong(getContentResolver(), Settings.System.SMS_SIM_SETTING, (Long) arg1);

            Intent intent = new Intent(Intent.ACTION_SMS_DEFAULT_SIM_CHANGED);

            intent.putExtra("simid", (Long) arg1);
            getActivity().sendBroadcast(intent);
            updateDefaultSIMSummary(mSmsSimSetting, (Long) arg1);

        } else if (KEY_GPRS_SIM_SETTING.equals(key)) {

            long value = ((Long) arg1).longValue();

            if (value == 0) {
                switchGprsDefautlSIM(value);
                return true;
            }

            SIMInfo siminfo = mSimMap.get(value);

            if (siminfo == null) {
                return false;
            }

            boolean isInRoaming = mTelephonyManagerEx.isNetworkRoaming(siminfo.mSlot);
            boolean mIsCU = false;
            mDataSwitchMsgIndex = -1;
            if (isInRoaming) {
                boolean isRoamingDataAllowed = (siminfo.mDataRoaming == SimInfo.DATA_ROAMING_ENABLE);
                if (isRoamingDataAllowed) {
                    if ((siminfo.mSlot != GeminiUtils.sG3SloteID) && (FeatureOption.MTK_GEMINI_3G_SWITCH)) {
                        mDataSwitchMsgIndex = mIsCU ? DATA_SWITCH_MSG_CASE2 : DATA_SWITCH_MSG_CASE1;
                    }
                } else {
                    if ((mIs3gOff) || (!mIs3gOff && (siminfo.mSlot == GeminiUtils.sG3SloteID))
                            || (!FeatureOption.MTK_GEMINI_3G_SWITCH)) {
                        mDataSwitchMsgIndex = 0;
                    } else if ((siminfo.mSlot != GeminiUtils.sG3SloteID) && (FeatureOption.MTK_GEMINI_3G_SWITCH)) {
                        mDataSwitchMsgIndex = mIsCU ? DATA_SWITCH_MSG_CASE4 : DATA_SWITCH_MSG_CASE3;
                    }
                }
            } else {
                if ((siminfo.mSlot != GeminiUtils.sG3SloteID) && (FeatureOption.MTK_GEMINI_3G_SWITCH)) {
                    mDataSwitchMsgIndex = mIsCU ? DATA_SWITCH_MSG_CASE2 : DATA_SWITCH_MSG_CASE1;
                }
            }

            if (mDataSwitchMsgIndex == -1) {

                switchGprsDefautlSIM(value);

            } else {
                showDialog(DIALOG_GPRS_SWITCH_CONFIRM);
                setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        updateGprsSettings();
                    }
                });
            }

        }
        return true;
    }

    private void showProgressDlg(boolean isActivating) {
        if (isActivating) {
            if (!getActivity().isFinishing()) {
                Xlog.i(TAG, "DIALOG_ACTIVATE");

                showDialog(DIALOG_ACTIVATE);
                mIsShowDlg = DIALOG_RADIO_ON;
                setCancelable(false);
            } else {
                Xlog.i(TAG, "Activity isFinishing, state error......");
            }
        } else {
            if (!getActivity().isFinishing()) {
                Xlog.i(TAG, "DIALOG_DEACTIVATE");

                showDialog(DIALOG_DEACTIVATE);
                mIsShowDlg = DIALOG_RADIO_OFF;
                setCancelable(false);
            } else {
                Xlog.i(TAG, "Activity isFinishing, state error......");
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDlg;
        switch (id) {
        case DIALOG_ACTIVATE:
            dialog.setMessage(getResources().getString(R.string.gemini_sim_mode_progress_activating_message));
            dialog.setIndeterminate(true);
            return dialog;

        case DIALOG_DEACTIVATE:
            dialog.setMessage(getResources().getString(R.string.gemini_sim_mode_progress_deactivating_message));
            dialog.setIndeterminate(true);
            return dialog;

        case DIALOG_WAITING:
            dialog.setMessage(getResources().getString(R.string.gemini_data_connection_progress_message));
            dialog.setIndeterminate(true);
            return dialog;

        case DIALOG_NETWORK_MODE_CHANGE:
            dialog.setMessage(getResources().getString(R.string.gemini_data_connection_progress_message));
            dialog.setIndeterminate(true);
            return dialog;

        case DIALOG_GPRS_SWITCH_CONFIRM:
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);

            if ((mDataSwitchMsgIndex >= DATA_SWITCH_MSG_CASE0) && (mDataSwitchMsgIndex <= DATA_SWITCH_MSG_CASE4)) {
                builder.setMessage(getResources().getString(mDataSwitchMsgStr[mDataSwitchMsgIndex]));

            }

            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    // TODO Auto-generated method stub
                    // use to judge whether the click is correctly done!

                    if ((mDataSwitchMsgIndex == DATA_SWITCH_MSG_CASE0)
                            || (mDataSwitchMsgIndex == DATA_SWITCH_MSG_CASE3) 
                            || (mDataSwitchMsgIndex == DATA_SWITCH_MSG_CASE4)) {
                        enableDataRoaming(mGprsSimSetting.getValue());
                    }
                    switchGprsDefautlSIM(mGprsSimSetting.getValue());

                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    // TODO Auto-generated method stub
                    // use to judge whether the click is correctly done!
                    updateGprsSettings();
                }
            });
            alertDlg = builder.create();

            return alertDlg;

        case DIALOG_3G_MODEM_SWITCH_CONFIRM:
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getResources().getString(R.string.gemini_3g_modem_switch_confirm_message));
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    // TODO Auto-generated method stub
                    // use to judge whether the click is correctly done!
                    switchVideoCallDefaultSIM(mVTTargetTemp);

                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    // TODO Auto-generated method stub
                    // use to judge whether the click is correctly done!
                    updateVideoCallDefaultSIM();
                }
            });

            alertDlg = builder.create();
            return alertDlg;

        case DIALOG_3G_MODEM_SWITCHING:

            dialog.setMessage(getResources().getString(R.string.gemini_3g_modem_switching_message));
            dialog.setIndeterminate(true);
            Window win = dialog.getWindow();
            WindowManager.LayoutParams lp = win.getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
            win.setAttributes(lp);
            return dialog;

        default:
            return null;
        }
    }

    private void addSimInfoPreference() {

        PreferenceGroup simInfoListCategory = 
                (PreferenceGroup) findPreference(KEY_SIM_INFO_CATEGORY);
        if (simInfoListCategory == null) {
            return;
        }
        List<SIMInfo> mSiminfoList = SIMInfo.getInsertedSIMList(getActivity());
        Collections.sort(mSiminfoList, new GeminiUtils.SIMInfoComparable());

        simInfoListCategory.removeAll();

        for (final SIMInfo siminfo : mSiminfoList) {

            if (siminfo == null) {
                break;
            }
            if (OOBEConstants.DEBUG) {
                Xlog.i(TAG, "siminfo.mDisplayName = " + siminfo.mDisplayName);
                Xlog.i(TAG, "siminfo.mNumber = " + siminfo.mNumber);
                Xlog.i(TAG, "siminfo.mSlot = " + siminfo.mSlot);
                Xlog.i(TAG, "siminfo.mColor = " + siminfo.mColor);
                Xlog.i(TAG, "siminfo.mDispalyNumberFormat = " + siminfo.mDispalyNumberFormat);
                Xlog.i(TAG, "siminfo.mSimId = " + siminfo.mSimId);
            }
            int status = -1;
            if(mITelephony != null) {
                try {
                    status = mITelephony.getSimIndicatorStateGemini(siminfo.mSlot);
                } catch (RemoteException exception) {
                  Xlog.i(TAG, "RemoteException  " + exception.getMessage());
                }
            }
            SimInfoEnablePreference simInfoPref = new SimInfoEnablePreference(getActivity(), siminfo.mDisplayName,
                    siminfo.mNumber, siminfo.mSlot, status, siminfo.mColor, siminfo.mDispalyNumberFormat, siminfo.mSimId);

            Xlog.i(TAG, "simid status is  " + status);

            if (simInfoPref != null) {
                simInfoPref.setClickCallback(this);
                if (mITelephony != null) {
                    try {
                        boolean isRadioOn = mITelephony.isRadioOnGemini(siminfo.mSlot);
                        simInfoPref.setCheck(isRadioOn);
                        simInfoPref.setRadioOn(isRadioOn);
                    } catch (RemoteException e) {
                        Xlog.e(TAG, "mITelephony exception");

                    }

                }
                if (siminfo.mSlot == PhoneConstants.GEMINI_SIM_1) {
                    mIsSlot1Insert = true;
                    mSlot1SimPref = simInfoPref;

                    simInfoPref.setCheckBoxClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            Xlog.i(TAG, "receive sim1 click intent!");
                            boolean bChecked = mSlot1SimPref.getCheck();
                            if (mIsSIMModeSwitching) {
                                Xlog.i(TAG, "mIsSIMModeSwitching == true");
                                mSlot1SimPref.setCheck(bChecked);
                                return;
                            } else {
                                mIsSIMModeSwitching = true;
                                Xlog.i(TAG, "set mIsSIMModeSwitching true");
                                mSlot1SimPref.setCheck(!bChecked);
                            }

                            dealSim1Change();

                        }
                    });

                } else if (siminfo.mSlot == PhoneConstants.GEMINI_SIM_2) {

                    mIsSlot2Insert = true;
                    mSlot2SimPref = simInfoPref;
                    // it will switch to airplane mode in the following case
                    simInfoPref.setCheckBoxClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Xlog.i(TAG, "receive sim2 click intent!");
                            boolean bChecked = mSlot2SimPref.getCheck();
                            if (mIsSIMModeSwitching) {
                                Xlog.i(TAG, "mIsSIMModeSwitching == true");
                                mSlot2SimPref.setCheck(bChecked);
                                return;
                            } else {
                                mIsSIMModeSwitching = true;
                                Xlog.i(TAG, "set mIsSIMModeSwitching true");
                                mSlot2SimPref.setCheck(!bChecked);
                            }

                            dealSim2Change();

                        }
                    });

                }
                simInfoListCategory.addPreference(simInfoPref);

            }

        }

    }

    private void addNoSimIndicator() {

        PreferenceGroup simInfoListCategory = 
                (PreferenceGroup) findPreference(KEY_SIM_INFO_CATEGORY);

        Preference pref = new Preference(getActivity());

        if (pref != null) {
            pref.setTitle(R.string.gemini_no_sim_indicator);
            simInfoListCategory.addPreference(pref);
        }

    }

    private void dealSim1Change() { //MTK_CS_IGNORE_THIS_LINE

        mDualSimMode = Settings.System.getInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1);

        Xlog.i(TAG, "dealSim1Change mDualSimMode value is : " + mDualSimMode);

        Intent intent;
        Xlog.i(TAG, "mIsSlot1Insert = " + mIsSlot1Insert + "; mIsSlot2Insert =" + mIsSlot2Insert);
        // see if it is airplane mode, if yes
        if (1 == Settings.System.getInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1)) {
            Xlog.i(TAG, "airplane mode is on");
            // two sim insert, change to sim1 only
            if (mIsSlot1Insert && mIsSlot2Insert) {

                Settings.System.putInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
                intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);
            // sim1 insert, change to sim1 only
            } else if (mIsSlot1Insert && !mIsSlot2Insert) {

                Settings.System.putInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
                intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);
            }
            return;
        }

        switch (mDualSimMode) {
        case 0: 
            Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
            intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, 1);
            getActivity().sendBroadcast(intent);
            showProgressDlg(true);
            break;
        case 1: 
            Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 0);
            intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, 0);
            getActivity().sendBroadcast(intent);
            showProgressDlg(false);
            break;
        case 2: 
            // two sim insert, change to dual sim mode
            if (mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 3);
                intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, SIM_GENIMI_MODE_DUAL);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);
            } else if (mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
                intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, 1);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);
            }
            break;
        case SIM_GENIMI_MODE_DUAL:
            // two sim insert, change to sim2 only
            if (mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 2);
                intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, SIM_GENIMI_MODE_SIM2);
                getActivity().sendBroadcast(intent);

                showProgressDlg(false);
            }
            break;
        default:
            Xlog.i(TAG, "dual sim mode error.");
            break;
        }
    }

    private void dealSim2Change() { //MTK_CS_IGNORE_THIS_LINE

        mDualSimMode = Settings.System.getInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1);

        Xlog.i(TAG, "dealSim2Change mDualSimMode value is : " + mDualSimMode);

        Intent intent;

        // see if it is airplane mode, if yes
        if (1 == Settings.System.getInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1)) {
            // two sim insert, change to sim2 only
            if (mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 2);
                intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                getActivity().sendBroadcast(intent);
                showProgressDlg(true);

            // sim2 insert, change to sim2 only
            } else if (!mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 2);
                intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);
            }

            return;
        }
        
        switch (mDualSimMode) {
        case 0: 
            Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 2);
            intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, 2);
            getActivity().sendBroadcast(intent);
            showProgressDlg(true);
            break;
        case SIM_GENIMI_MODE_SIM1: 

            // two sim insert, change to dual sim mode
            if (mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 3);
                intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, SIM_GENIMI_MODE_DUAL);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);

            // sim2 insert, change to sim2 only mode and sim1 can not be used any more
            } else if (!mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 2);
                intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, SIM_GENIMI_MODE_SIM2);
                getActivity().sendBroadcast(intent);

                showProgressDlg(true);
            }
            break;
        case SIM_GENIMI_MODE_SIM2: 

            Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 0);
            intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, 0);
            getActivity().sendBroadcast(intent);
            showProgressDlg(false);
            break;
        case SIM_GENIMI_MODE_DUAL: 
            // two sim insert, change to sim1 only
            if (mIsSlot1Insert && mIsSlot2Insert) {
                Settings.System.putInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
                intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, SIM_GENIMI_MODE_SIM1);
                getActivity().sendBroadcast(intent);

                showProgressDlg(false);
            }
            break;
        default:
            Xlog.i(TAG, "dual sim mode error.");
            break;
        }
    }

    private void dealWithSwtichComplete() {

        for (Long simid : mSimMap.keySet()) {
            SIMInfo siminfo = mSimMap.get(simid);

            SimInfoEnablePreference simInfoPref = (SimInfoEnablePreference) findPreference(String.valueOf(siminfo.mSimId));

            if ((simInfoPref != null) && (mITelephony != null)) {

                try {
                    boolean newState = mITelephony.isRadioOnGemini(siminfo.mSlot);
                    boolean oldState = simInfoPref.isRadioOn();
                    simInfoPref.setRadioOn(newState);

                    simInfoPref.setCheck(newState);
                    Xlog.i(TAG, "mIsSIMModeSwitching is " + mIsSIMModeSwitching + " newState is " + newState
                            + " oldState is " + oldState);
                } catch (RemoteException e) {
                    Xlog.e(TAG, "mITelephony exception");
                    return;
                }

            }

        }

        Xlog.i(TAG, "next will remove the progress dlg");

        if ((mIsShowDlg == DIALOG_RADIO_ON) || (mIsShowDlg == DIALOG_RADIO_OFF)) {
            if (isResumed()) {
                removeDialog((mIsShowDlg == DIALOG_RADIO_ON) ? DIALOG_ACTIVATE : DIALOG_DEACTIVATE);
            }
            mIsShowDlg = DIALOG_NONE;
        }

        // switch
        if (!mIsSIMModeSwitching) {
            Xlog.i(TAG, "mIsSIMModeSwitching value error......");
        }
        mIsSIMModeSwitching = false;
        Xlog.e(TAG, "mIsSIMModeSwitching is " + mIsSIMModeSwitching);

        sAllSimRadioOff = (Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                || (Settings.System.getInt(this.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1) == 0);

        mGprsSimSetting.setEnabled(!sAllSimRadioOff && sScreenEnable && sHasSim);

    }

    private void dealwithAttach() {
        mIsGprsSwitching = true;
        mTimeHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
        showDialog(DIALOG_WAITING);
        mIsShowDlg = DIALOG_DATA_SWITCH;
        setCancelable(false);

    }

    private void dealwithDetach() {
        mIsGprsSwitching = true;
        mTimeHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
        showDialog(DIALOG_WAITING);
        mIsShowDlg = DIALOG_DATA_SWITCH;
        setCancelable(false);
    }

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    private Handler mTimeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_DETACH_TIME_OUT:
                Xlog.i(TAG, "detach time out......");

                if (mIsShowDlg == DIALOG_DATA_SWITCH) {

                    mIsShowDlg = DIALOG_NONE;

                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                }
                updateGprsSettings();
                // Toast.makeText(mContext, getString(R.string.data_connection_detach_timeout_error_msg),
                // Toast.LENGTH_LONG).show();
                mIsGprsSwitching = false;
                break;
            case EVENT_ATTACH_TIME_OUT:
                Xlog.i(TAG, "attach time out......");

                if (mIsShowDlg == DIALOG_DATA_SWITCH) {

                    mIsShowDlg = DIALOG_NONE;

                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                }
                updateGprsSettings();

                // Toast.makeText(mContext, getString(R.string.data_connection_attach_timeout_error_msg),
                // Toast.LENGTH_LONG).show();
                mIsGprsSwitching = false;
                break;
            default:
                break;
            }
        }
    };

    protected void initDefaultSimPreference() {

        // initialize the default sim preferences
        mSimItemListVoice.clear();
        mSimItemListSms.clear();
        mSimItemListGprs.clear();

        mSimItemListVideo.clear();
        SimItem simitem;
        int k = 0;
        long voicecallID = getDataValue(Settings.System.VOICE_CALL_SIM_SETTING);
        long smsID = getDataValue(Settings.System.SMS_SIM_SETTING);
        long dataconnectionID = getDataValue(Settings.System.GPRS_CONNECTION_SIM_SETTING);
        int videocallSlotID = current3GSlotId();
        if ((sVTCallSupport)) {
            mVideoCallSimSetting.setSummary("");
        }
        mGprsSimSetting.setSummary("");
        List<SIMInfo> mSiminfoList = SIMInfo.getInsertedSIMList(getActivity());
        Collections.sort(mSiminfoList, new GeminiUtils.SIMInfoComparable());

        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo != null) {

                simitem = new SimItem(siminfo);
                int state = -1;
                if(mITelephony != null) {
                    try {
                        state = mITelephony.getSimIndicatorStateGemini(siminfo.mSlot);
                    } catch (RemoteException exception) {
                        Xlog.i(TAG, "RemoteException  " + exception.getMessage());
                    }
                }
                simitem.mState = state;

                if (siminfo.mSimId == voicecallID) {
                    if (mIsVoiceCapable) {
                        mVoiceCallSimSetting.setInitValue(k);
                        mVoiceCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                }
                if (siminfo.mSimId == smsID) {
                    mSmsSimSetting.setInitValue(k);
                    mSmsSimSetting.setSummary(siminfo.mDisplayName);
                }
                if (siminfo.mSimId == dataconnectionID) {
                    mGprsSimSetting.setInitValue(k);
                    mGprsSimSetting.setSummary(siminfo.mDisplayName);
                }
                if (sVTCallSupport) {
                    if (siminfo.mSlot == videocallSlotID) {
                        mVideoCallSimSetting.setInitValue(k);
                        mVideoCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                }
                mSimItemListVoice.add(simitem);
                mSimItemListSms.add(simitem);
                mSimItemListGprs.add(simitem);
                if (sVTCallSupport) {
                    // based on the new UI only 3g switch is available to show video call item
                    mSimItemListVideo.add(simitem);
                }
                mSimIdToIndexMap.put(Long.valueOf(siminfo.mSimId), k);

            }

            k++;

        }

        if (sVoipAvailable) {
            simitem = new SimItem(this.getString(R.string.gemini_intenet_call), 
                    GeminiUtils.INTERNET_CALL_COLOR,
                    Settings.System.VOICE_CALL_SIM_SETTING_INTERNET);
            mSimItemListVoice.add(simitem);
        }

        simitem = new SimItem(this.getString(R.string.gemini_default_sim_always_ask), -1,
                Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK);
        if (mSimItemListVoice.size() > 1) {
            mSimItemListVoice.add(simitem);    
        } else if (mSimItemListVoice.size() == 1 && sVoipAvailable) {
            Xlog.d(TAG,"no sim card inserted but internet call is on");
            mSimItemListVoice.add(simitem); 
        }
        if (mSimItemListSms.size() >1) {
            mSimItemListSms.add(simitem);    
        }

        if (mExt.isNeedsetAutoItem()) {
            if (mSimItemListSms.size() > 1) {
                simitem = new SimItem(mExt.getAutoString(),
                    GeminiUtils.NO_COLOR, Settings.System.SMS_SIM_SETTING_AUTO);
                mSimItemListSms.add(simitem);    
            }
        }
            
        simitem = new SimItem(this.getString(R.string.gemini_default_sim_never), -1,
                Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER);
        if (isRadioOff()) {
            simitem.mState = PhoneConstants.SIM_INDICATOR_RADIOOFF;
        }
        mSimItemListGprs.add(simitem);

        int nSim = mSiminfoList.size();
        if (voicecallID == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
            mVoiceCallSimSetting.setInitValue(nSim);
            mVoiceCallSimSetting.setSummary(R.string.gemini_intenet_call);
        } else if (voicecallID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
            mVoiceCallSimSetting.setInitValue(sVoipAvailable ? (nSim + 1) : nSim);
            mVoiceCallSimSetting.setSummary(R.string.gemini_default_sim_always_ask);
        } else if (voicecallID == Settings.System.DEFAULT_SIM_NOT_SET) {
            mVoiceCallSimSetting.setSummary(R.string.apn_not_set);
        }
        if (smsID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
            mSmsSimSetting.setInitValue(nSim);
            mSmsSimSetting.setSummary(R.string.gemini_default_sim_always_ask);
        } else if (smsID == Settings.System.DEFAULT_SIM_NOT_SET) {
            mSmsSimSetting.setSummary(R.string.apn_not_set);
        } else {
            if (mExt.isNeedsetAutoItem() && (smsID == Settings.System.SMS_SIM_SETTING_AUTO)){
                mExt.setPrefProperty(mSmsSimSetting, smsID);
                mSmsSimSetting.setInitValue(nSim + 1); 
             }
             
         }
        
        if (dataconnectionID == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
            mGprsSimSetting.setInitValue(nSim);
            mGprsSimSetting.setSummary(R.string.gemini_default_sim_3g_off);
        } else if (dataconnectionID == Settings.System.DEFAULT_SIM_NOT_SET) {
            mGprsSimSetting.setSummary(R.string.apn_not_set);
        }

        mGprsSimSetting.setData(mSimItemListGprs);
        mVoiceCallSimSetting.setData(mSimItemListVoice);
        mSmsSimSetting.setData(mSimItemListSms);
        mVideoCallSimSetting.setData(mSimItemListVideo);
        if (sVTCallSupport) {
            if (mSimItemListVideo.size() < 2) {
                mVideoCallSimSetting.setEnabled(false);    
            } else {
                mVideoCallSimSetting.setData(mSimItemListVideo);
                mVideoCallSimSetting.setEnabled(true);    
            }
        }
        if (mIsVoiceCapable) {
            if (mSimItemListVoice.size() < 2) {
                mVoiceCallSimSetting.setEnabled(false); 
            } else {
                mVoiceCallSimSetting.setEnabled(true); 
            }
        }
        if (mSimItemListSms.size() < 2) {
            mSmsSimSetting.setEnabled(false);
        } else {
            mSmsSimSetting.setEnabled(true);
        }

    }

    protected void updateSimInfo(long simID, int type) {
        SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(), simID);

        if (siminfo != null) {
            mSimMap.put(Long.valueOf(simID), siminfo);
            SimInfoEnablePreference pref = (SimInfoEnablePreference) findPreference(String.valueOf(simID));
            if (pref == null) {
                return;
            }
            switch (type) {
            case TYPE_SIM_NAME:
                pref.setName(siminfo.mDisplayName);
                return;
            case TYPE_SIM_COLOR:
                pref.setColor(siminfo.mColor);
                return;
            case TYPE_SIM_NUMBER:
                pref.setNumber(siminfo.mNumber);
                return;
            case TYPE_SIM_NUMBER_FORMAT:
                pref.setNumDisplayFormat(siminfo.mDispalyNumberFormat);
                return;
            default:
                break;
            }
        }
    }

    protected void updateSimState(int slotID, int state) {
        SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(getActivity(), slotID);

        if (siminfo != null) {

            SimInfoEnablePreference pref = (SimInfoEnablePreference) findPreference(String.valueOf(siminfo.mSimId));
            if (pref != null) {
                pref.setStatus(state);
                Xlog.i(TAG, "simid status of sim " + siminfo.mSimId + "is  " + state);
            }
        }
    }

    protected void updateDefaultSimState(int slotID, int state) {

        SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(getActivity(), slotID);

        if (siminfo != null) {

            Integer intIndex = mSimIdToIndexMap.get(Long.valueOf(siminfo.mSimId));
            if (intIndex == null) {
                return;
            }
            int index = intIndex.intValue();

            Xlog.i(TAG, "index is" + index);
            SimItem simitem = new SimItem(siminfo);
            simitem.mState = state;
            updateDefaultSimItemList(index, simitem, (slotID == PhoneConstants.GEMINI_SIM_1) ? true : false);

            Xlog.i(TAG, "simid status of sim " + siminfo.mSimId + "is  " + state);

        }

    }

    protected void updateDefaultSimInfo(long simID) {

        SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(), simID);

        if (siminfo != null) {

            Integer intIndex = mSimIdToIndexMap.get(siminfo.mSimId);
            if (intIndex == null) {
                return;
            }
            int index = intIndex.intValue();

            SimItem simitem = new SimItem(siminfo);
            int state = -1;
            try {
                state = mITelephony.getSimIndicatorStateGemini(siminfo.mSlot);
            } catch (RemoteException exception) {
                Xlog.i(TAG, "RemoteException  " + exception.getMessage());
            }

            simitem.mState = state;
            updateDefaultSimItemList(index, simitem, (siminfo.mSlot == PhoneConstants.GEMINI_SIM_1) ? true : false);

            Xlog.i(TAG, "simid status of sim " + siminfo.mSimId + "is  " + state);
        }
    }

    protected void updateDefaultSimItemList(int index, SimItem simitem, boolean slot3g) {
        if (index < mSimItemListVoice.size()) {
            mSimItemListVoice.set(index, simitem);
            mVoiceCallSimSetting.setData(mSimItemListVoice);
        }
        if (index < mSimItemListSms.size()) {
            mSimItemListSms.set(index, simitem);
            mSmsSimSetting.setData(mSimItemListSms);
        }
        if (index < mSimItemListGprs.size()) {
            mSimItemListGprs.set(index, simitem);
            mGprsSimSetting.setData(mSimItemListGprs);
        }
        if (sVTCallSupport) {
            if (!FeatureOption.MTK_GEMINI_3G_SWITCH) {
                if (slot3g) {
                    mSimItemListVideo.set(0, simitem);
                }

            } else {
                if (index < mSimItemListVideo.size()) {
                    mSimItemListVideo.set(index, simitem);
                }
            }

            mVideoCallSimSetting.setData(mSimItemListVideo);
        }
    }

    private void updateDefaultSimValue(int type, long simId) {

        if (simId < Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
            return;
        }

        if (simId == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {

            if (type == GeminiUtils.TYPE_GPRS) {
                mGprsSimSetting.setInitValue(mSimMap.size());
                mGprsSimSetting.setSummary(R.string.gemini_default_sim_never);
            }
        } else {

            Integer intIndex = mSimIdToIndexMap.get(simId);
            if (intIndex == null) {
                return;
            }
            int index = intIndex.intValue();

            if (index < 0) {
                return;
            }

            SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(), simId);

            if (siminfo == null) {
                return;
            }
            if (type == GeminiUtils.TYPE_GPRS) {

                mGprsSimSetting.setInitValue(index);
                mGprsSimSetting.setSummary(siminfo.mDisplayName);
            }
        }

    }

    /**
     * update video call default SIM value and summary
     */

    private void updateVideoCallDefaultSIM() {

        if (mITelephony != null) {

            try {
                int videocallSlotID = mITelephony.get3GCapabilitySIM();

                GeminiUtils.sG3SloteID = videocallSlotID;

                if (videocallSlotID < 0) {
                    return;
                }

                SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(getActivity(), videocallSlotID);

                if (siminfo != null) {

                    Integer intIndex = mSimIdToIndexMap.get(siminfo.mSimId);
                    if (intIndex == null) {
                        return;
                    }
                    int index = intIndex.intValue();

                    if ((index >= 0) && (siminfo != null)) {

                        mVideoCallSimSetting.setInitValue(index);
                        mVideoCallSimSetting.setSummary(siminfo.mDisplayName);

                    }
                } else {
                    mVideoCallSimSetting.setInitValue(-1);
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mITelephony exception");
                return;
            }

        }
    }

    private void initSimMap() {

        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
        Collections.sort(simList, new GeminiUtils.SIMInfoComparable());
        mSimMap.clear();
        Xlog.i(TAG, "sim number is " + simList.size());
        for (SIMInfo siminfo : simList) {
            mSimMap.put(Long.valueOf(siminfo.mSimId), siminfo);
        }
    }

    /**
     * Check if voip is supported and is enabled
     */
    private boolean isVoipAvailable() {
        int isInternetCallEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.ENABLE_INTERNET_CALL, 0);

        return (SipManager.isVoipSupported(getActivity())) && (isInternetCallEnabled != 0);

    }

    /**
     * switch data connection default SIM
     * 
     * @param value
     *            : sim id of the new default SIM
     */
    private void switchGprsDefautlSIM(long value) {

        if (value < 0) {
            return;
        }

        long gprsValue = Settings.System.getLong(getContentResolver(), 
                Settings.System.GPRS_CONNECTION_SIM_SETTING,
                Settings.System.DEFAULT_SIM_NOT_SET);
        if (value == gprsValue) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
        intent.putExtra("simid", value);

        sGprsTargSim = (value > 0) ? true : false;

        if (sGprsTargSim) {
            dealwithAttach();
        } else {
            dealwithDetach();

        }

        getActivity().sendBroadcast(intent);
    }

    private void enableDataRoaming(long value) {

        try {
            if (mITelephony != null) {
                mITelephony.setDataRoamingEnabledGemini(true, SIMInfo.getSlotById(getActivity(), value));

            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "mITelephony exception");
            return;
        }
        SIMInfo.setDataRoaming(getActivity(), SimInfo.DATA_ROAMING_ENABLE, value);

    }

    /**
     * switch 3g modem SIM
     * 
     * @param slotID
     */
    private void switchVideoCallDefaultSIM(long value) {
        Xlog.i(TAG, "switchVideoCallDefaultSIM to " + value);

        if (mITelephony != null) {

            SIMInfo siminfo = SIMInfo.getSIMInfoById(getActivity(), value);
            Xlog.i(TAG, "siminfo = " + siminfo);

            if (siminfo == null) {
                return;
            }

            try {

                Xlog.i(TAG, "sim slot  = " + siminfo.mSlot);
                if (mITelephony.set3GCapabilitySIM(siminfo.mSlot)) {
                    Xlog.i(TAG, "result is true");
                    // mIsModemSwitching = true;
                    if (mStatusBarManager != null) {
                        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
                    }

                    showDialog(DIALOG_3G_MODEM_SWITCHING);
                    mIsShowDlg = DIALOG_MODEN_SWITCH;
                    setCancelable(false);

                } else {
                    updateVideoCallDefaultSIM();
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mITelephony exception");
                return;
            }

        }
    }

    /**
     * init Progress Bar
     * @param maxSize int
     * @param curStep int
     */
    public static void initProgressBar(int maxSize, int curStep) {
        sProgressbarMaxSize = maxSize;
        sCurrentStep = curStep;
    }

    @Override
    public void onPreferenceClick(long simid) {
        Intent intent = new Intent(getActivity(), SimInfoEditor.class);
        intent.putExtra(OOBEConstants.OOBE_BASIC_STEP_TOTAL, sProgressbarMaxSize);
        intent.putExtra(OOBEConstants.OOBE_BASIC_STEP_INDEX, sCurrentStep);
        intent.putExtra(GeminiUtils.EXTRA_SIMID, simid);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        Xlog.i(TAG, " Start Sim info editor, simId=" + simid);
    }

    /**
     * disable sim preference for hot swap card
     */
    public void disableSimPrefs(){
        
        if(mSlot1SimPref != null){
            mSlot1SimPref.setEnabled(false);
        }
        if(mSlot2SimPref != null){
            mSlot2SimPref.setEnabled(false);
        }
        
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());

        Xlog.i("hotswapdbg", "sim number is "+simList.size());
        
        for (SIMInfo siminfo : simList) {
            Xlog.d("hotswapdbg", " " + siminfo.mSlot + " " + siminfo.mDisplayName );
            
            if(siminfo.mSlot == PhoneConstants.GEMINI_SIM_1) {
                if(mSlot1SimPref != null){
                    mSlot1SimPref.setEnabled(true);
                }
            }else if(siminfo.mSlot == PhoneConstants.GEMINI_SIM_2){
                if(mSlot2SimPref != null){
                    mSlot2SimPref.setEnabled(true);
                }
            }           
        }
        
        initDefaultSimPreference();

        int simNumber = simList.size();
        if (simNumber < 2) {
            
            //mVoiceCallSimSetting.setEnabled(false);
            //mVideoCallSimSetting.setEnabled(false);
            //mSmsSimSetting.setEnabled(false);
            //if not sim card, disable all default SIM settings
            if( mVoiceCallSimSetting.getDialog() != null ){
                mVoiceCallSimSetting.getDialog().dismiss();
            }
            if( mVideoCallSimSetting.getDialog() != null ){
                mVideoCallSimSetting.getDialog().dismiss();
            }
            if( mSmsSimSetting.getDialog() != null ){
                mSmsSimSetting.getDialog().dismiss();
            }

        }
        if (simNumber == 0) {
            mGprsSimSetting.setEnabled(false);
            if( mGprsSimSetting.getDialog() != null ){
                mGprsSimSetting.getDialog().dismiss();
            }

        }
        sHasSim = simNumber > 0;
    }
    /**
     * @return is airplane mode or all sim card is set on radio off
     * 
     */
    private boolean isRadioOff() {
        boolean isAllRadioOff = (Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                || (Settings.System.getInt(getContentResolver(),
                        Settings.System.DUAL_SIM_MODE_SETTING, -1) == ALL_RADIO_OFF);
        Xlog.d(TAG, "isAllRadioOff=" + isAllRadioOff);
        return isAllRadioOff;
    }
    ///M: set the icon for each preference voice/sms/video/data connection
    private void setIconForDefaultSimPref() {
        mVoiceCallSimSetting.setIcon(R.drawable.gemini_voice_call);
        mVideoCallSimSetting.setIcon(R.drawable.gemini_video_call);
        mSmsSimSetting.setIcon(R.drawable.gemini_sms);
        mGprsSimSetting.setIcon(R.drawable.gemini_data_connection);
    }
    private long getDataValue(String dataString) {
        return Settings.System.getLong(getContentResolver(), dataString,
                Settings.System.DEFAULT_SIM_NOT_SET);
    }
    private int current3GSlotId() {
        int slot3G = VEDIO_CALL_OFF;
        try {
            if (mITelephony != null) {
                slot3G = mITelephony.get3GCapabilitySIM();
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "mTelephony exception");
        }
        return slot3G;
    }

}
