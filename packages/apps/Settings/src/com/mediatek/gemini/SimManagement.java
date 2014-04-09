package com.mediatek.gemini;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.StatusBarManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SimItem {
    public boolean mIsSim = true;
    public String mName = null;
    public String mNumber = null;
    public int mDispalyNumberFormat = 0;
    public int mColor = -1;
    public int mSlot = -1;
    public long mSimID = -1;
    public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;

    /**
     * Construct of SimItem
     * 
     * @param name
     *            String name of sim card
     * @param color
     *            int color of sim card
     * @param simID
     *            long sim id of sim card
     */
    public SimItem(String name, int color, long simID) {
        mName = name;
        mColor = color;
        mIsSim = false;
        mSimID = simID;
    }

    /**
     * 
     * @param siminfo
     *            SIMInfo
     */
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

public class SimManagement extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        SimInfoEnablePreference.OnPreferenceClickCallback {

    private static final String TAG = "SimManagementSettings";
    // key of preference of sim managment
    private static final String KEY_SIM_INFO_CATEGORY = "sim_info";
    private static final String KEY_GENERAL_SETTINGS_CATEGORY = "general_settings";
    private static final String KEY_DEFAULT_SIM_SETTINGS_CATEGORY = "default_sim";
    private static final String KEY_VOICE_CALL_SIM_SETTING = "voice_call_sim_setting";
    private static final String KEY_VIDEO_CALL_SIM_SETTING = "video_call_sim_setting";
    private static final String KEY_SMS_SIM_SETTING = "sms_sim_setting";
    private static final String KEY_GPRS_SIM_SETTING = "gprs_sim_setting";
    private static final String KEY_SIM_CONTACTS_SETTINGS = "contacts_sim";
    private static final String KEY_3G_SERVICE_SETTING = "3g_service_settings";

    private static boolean sVTCallSupport = true;
    private static boolean sIsVoipAvailable = true;
    private boolean mIs3gOff = false;
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    private static final String ACTION_DATA_USAGE_DISABLED_DIALOG_OK =
            "com.mediatek.systemui.net.action.ACTION_DATA_USAGE_DISABLED_DIALOG_OK"; 
    private static final String CONFIRM_DIALOG_MSG_ID = "confirm_dialog_msg_id";
    private static final String PROGRESS_DIALOG_MSG_ID = "progress_dialog_msg_id";
    // which simid is selected when switch video call
    private long mSelectedVideoSimId;
    private boolean mIsSlot1Insert = false;
    private boolean mIsSlot2Insert = false;

    // time out length
    private static final int DETACH_DATA_CONN_TIME_OUT = 10000;// in ms
    private static final int ATTACH_DATA_CONN_TIME_OUT = 30000;// in ms
    private static final int DIALOG_NOT_REMOVE_TIME_OUT = 1000;// in ms
    private static final int SWITCH_3G_TIME_OUT = 60000;// in ms
    private static final int VIDEO_CALL_OFF = -1;
    
    // when sim radio switch complete receive msg with this id
    private static final int EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE = 1;
    // time out message event
    private static final int DATA_SWITCH_TIME_OUT_MSG = 2000;
    private static final int DIALOG_NOT_SHOW_SUCCESS_MSG = DATA_SWITCH_TIME_OUT_MSG + 1;
    private static final int DATA_3G_SWITCH_TIME_OUT_MSG = DATA_SWITCH_TIME_OUT_MSG + 2;
    // Dialog Id for different task
    private static final int PROGRESS_DIALOG = 1000;
    private static final int DIALOG_3G_MODEM_SWITCH_CONFIRM = PROGRESS_DIALOG + 1;
    private static final int DIALOG_GPRS_SWITCH_CONFIRM = PROGRESS_DIALOG + 2;
    // constant for current sim mode
    private static final int ALL_RADIO_OFF = 0;
    private static final int SIM_SLOT_1_RADIO_ON = 1;
    private static final int SIM_SLOT_2_RADIO_ON = 2;
    private static final int ALL_RADIO_ON = 3;

    private DefaultSimPreference mVoiceCallSimSetting;
    private DefaultSimPreference mVideoCallSimSetting;
    private DefaultSimPreference mSmsSimSetting;
    private DefaultSimPreference mGprsSimSetting;
    private PreferenceScreen mSimAndContacts;

    private TelephonyManager mTelephonyManager;
    private ITelephony mTelephony;
    // a list store the siminfo variable
    private List<SIMInfo> mSiminfoList = new ArrayList<SIMInfo>();
    

    private List<SimItem> mSimItemListVoice = new ArrayList<SimItem>();
    private List<SimItem> mSimItemListVideo = new ArrayList<SimItem>();
    private List<SimItem> mSimItemListSms = new ArrayList<SimItem>();
    private List<SimItem> mSimItemListGprs = new ArrayList<SimItem>();
    // to prevent click too fast to switch card 1 and 2 in radio on/off
    private boolean mIsSIMRadioSwitching = false;

    private IntentFilter mIntentFilter;
    private int mDataSwitchMsgIndex = -1;
    private CellConnMgr mCellConnMgr;
    private boolean mIsVoiceCapable = false;
    private boolean mIsSmsCapable = false;
    private boolean mIsDataConnectActing = false;
    private ISimManagementExt mExt;
    private int mSimNum;
    private long mSelectedGprsSimId;
    private boolean mRemoveProgDlg = false;
    private int mProDlgMsgId = -1;
    private boolean noNeedRestoreProgDlg = false;

    ///M: add for consistent_UI single sim to enable data connection
    private ConnectivityManager mConnService;

    private ContentObserver mGprsDefaultSIMObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDataConnPrefe();
        }
    };
    /*
     * Timeout handler to revent the dialog always showing if moderm not send
     * connected or disconnected intent
     */
    private Handler mTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (DATA_SWITCH_TIME_OUT_MSG == msg.what) {
                Xlog.i(TAG, "reveive time out msg...");
                removeProgDlg();
                mIsDataConnectActing = false;
                updateDataConnPrefe();
            } else if (DIALOG_NOT_SHOW_SUCCESS_MSG == msg.what) {
                Xlog.i(TAG, "handle abnormal progress dialog not showing");
                dealWithSwtichComplete();    
            } else if (DATA_3G_SWITCH_TIME_OUT_MSG == msg.what) {
                Xlog.d(TAG,"3G switch time out remove the progress dialog");
                removeProgDlg();
                setStatusBarEnableStatus(true);
                updateVideoCallDefaultSIM();
            } else if (EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE == msg.what) {
                Xlog.d(TAG, "dual sim mode changed");
                dealWithSwtichComplete();
            }
        }
    };

    // receive when sim card radio switch complete
    private Messenger mSwitchRadioStateMsg = new Messenger(mTimerHandler);


    // Receiver to handle different actions
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d(TAG, "mSimReceiver receive action=" + action);
            // Refresh the whole ui as below action all related to siminfo
            // updated
            if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)
                    || action.equals(Intent.SIM_SETTINGS_INFO_CHANGED)
                    || action.equals(TelephonyIntents.ACTION_SIM_NAME_UPDATE)
                    || action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)
                    || action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                getSimInfo();
                updatePreferenceUI();
                dealDlgOnAirplaneMode(action);
            } else if (action.equals(TRANSACTION_START)) {
                // Disable dataconnection pref to prohibit to switch data
                // connection
                if (mGprsSimSetting.isEnabled()) {
                    mGprsSimSetting.setEnabled(false);
                }
                Dialog dlg = mGprsSimSetting.getDialog();
                if (dlg != null && dlg.isShowing()) {
                    Xlog.d(TAG, "MMS starting dismiss GPRS selection dialog to prohbit data switch");
                    dlg.dismiss();
                }
            } else if (action.equals(TRANSACTION_STOP)) {
                /*
                 * if data connection is disable then able when mms transition
                 * stop if all radio is not set off
                 */
                if (!mGprsSimSetting.isEnabled()) {
                    mGprsSimSetting.setEnabled(!isRadioOff());
                }
                Dialog dlg = mGprsSimSetting.getDialog();
                if (dlg != null && dlg.isShowing()) {
                    Xlog.d(TAG, "MMS stopped dismiss GPRS selection dialog");
                    dlg.dismiss();
                }
            } else if (action.equals(GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED)) {
                boolean lockState = intent.getBooleanExtra(
                        GeminiPhone.EXTRA_3G_SWITCH_LOCKED, false);
                if (sVTCallSupport) {
                    mVideoCallSimSetting.setEnabled(!(mIs3gOff || lockState || mSimNum == 1));
                    Xlog.d(TAG, "mIs3gOff=" + mIs3gOff + " lockState="
                            + lockState);
                }
            } else if (action.equals(GeminiPhone.EVENT_3G_SWITCH_DONE)) {
                // remove the loading dialog when 3g service switch done
                mTimerHandler.removeMessages(DATA_3G_SWITCH_TIME_OUT_MSG);
                removeProgDlg();
                setStatusBarEnableStatus(true);
                updateVideoCallDefaultSIM();
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                PhoneConstants.DataState state = getMobileDataState(intent);
                Xlog.i(TAG, "slotId=" + slotId);
                Xlog.i(TAG, "state=" + state);
                Xlog.i(TAG, "apnTypeList=" + apnTypeList);
                /* {@M Auto open the other card's data connection. when current card is radio off */ 
                mExt.dealWithDataConnChanged(intent, isResumed());
                /*@}*/
                if ((state == PhoneConstants.DataState.CONNECTED) || 
                    (state == PhoneConstants.DataState.DISCONNECTED)) {
                    if ((PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList))) {
                        Xlog.d(TAG, "****the slot " + slotId + state + 
                                    " mIsDataConnectActing=" + mIsDataConnectActing);
                        if (mIsDataConnectActing) {
                            mTimerHandler.removeMessages(DATA_SWITCH_TIME_OUT_MSG);
                            removeProgDlg();
                            mIsDataConnectActing = false;   
                        }
                    }
                }

            } else if (action.equals(ACTION_DATA_USAGE_DISABLED_DIALOG_OK)) {
                if (mIsDataConnectActing) {
                    mIsDataConnectActing = false;
                    removeProgDlg();    
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sim_management);
        Xlog.d(TAG, "onCreate Sim Management");
        Xlog.d(TAG, "MTK_VT3G324M_SUPPORT=" + FeatureOption.MTK_VT3G324M_SUPPORT +
                    "MTK_GEMINI_3G_SWITCH=" + FeatureOption.MTK_GEMINI_3G_SWITCH);
        if ((!FeatureOption.MTK_VT3G324M_SUPPORT) || (!FeatureOption.MTK_GEMINI_3G_SWITCH)) {
            sVTCallSupport = false;
        }
        ///M: initilize connectivity manager for consistent_UI
        mConnService = ConnectivityManager.from(this.getActivity());
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));

        Xlog.d(TAG, "FeatureOption.MTK_GEMINI_3G_SWITCH="
                + FeatureOption.MTK_GEMINI_3G_SWITCH);
        PreferenceGroup parent = (PreferenceGroup) findPreference(KEY_GENERAL_SETTINGS_CATEGORY);
        // /M: plug in of sim management
        mExt = Utils.getSimManagmentExtPlugin(this.getActivity());
        mExt.updateSimManagementPref(parent);
        initIntentFilter();

        mVoiceCallSimSetting = (DefaultSimPreference) findPreference(KEY_VOICE_CALL_SIM_SETTING);
        mVideoCallSimSetting = (DefaultSimPreference) findPreference(KEY_VIDEO_CALL_SIM_SETTING);
        mSmsSimSetting = (DefaultSimPreference) findPreference(KEY_SMS_SIM_SETTING);
        mGprsSimSetting = (DefaultSimPreference) findPreference(KEY_GPRS_SIM_SETTING);
        /*M: set the icon for each preference (because of the onbinding of preference is called after the onresume,
              so before onresume it always get the icon with null) then set the icon in code rather xml@{*/
        setIconForDefaultSimPref();
        //*}/
        mSimAndContacts = (PreferenceScreen) findPreference(KEY_SIM_CONTACTS_SETTINGS);

        mVoiceCallSimSetting.setType(GeminiUtils.TYPE_VOICECALL);
        mVoiceCallSimSetting.setOnPreferenceChangeListener(this);
        mVideoCallSimSetting.setType(GeminiUtils.TYPE_VIDEOCALL);
        mVideoCallSimSetting.setOnPreferenceChangeListener(this);
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            removeDefaultSimPref();
        }
        mSmsSimSetting.setType(GeminiUtils.TYPE_SMS);
        mSmsSimSetting.setOnPreferenceChangeListener(this);
        mGprsSimSetting.setType(GeminiUtils.TYPE_GPRS);
        mGprsSimSetting.setOnPreferenceChangeListener(this);
        mCellConnMgr = new CellConnMgr();
        mCellConnMgr.register(getActivity());
        mGprsSimSetting.setCellConnMgr(mCellConnMgr);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            try {
                if (mTelephony != null) {
                    mTelephony.registerForSimModeChange(mSwitchRadioStateMsg
                        .getBinder(), EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE);
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mTelephony exception");
                return;
            }    
        }
        getActivity().registerReceiver(mSimReceiver, mIntentFilter);
        //handler the process is killed as dialog showing
        if (savedInstanceState != null) {
            mDataSwitchMsgIndex = savedInstanceState.getInt(CONFIRM_DIALOG_MSG_ID,-1);
            mProDlgMsgId = savedInstanceState.getInt(PROGRESS_DIALOG_MSG_ID,-1);
            // if the value !=-1 means there is dialog showing
            if (mProDlgMsgId != -1) {
                //Progress dialog no need to restore so remove on resume()
                Xlog.d(TAG,"mProDlgMsgId != -1 to remove dialog");
                noNeedRestoreProgDlg = true;
            }
            Xlog.d(TAG,"onrestore the dailog msg id with mDataSwitchMsgIndex = " + 
                        mDataSwitchMsgIndex + " mProDlgMsgId = " + mProDlgMsgId);
        }
    }
    ///M: set the icon for each preference voice/sms/video/data connection
    private void setIconForDefaultSimPref() {
        mVoiceCallSimSetting.setIcon(R.drawable.gemini_voice_call);
        mVideoCallSimSetting.setIcon(R.drawable.gemini_video_call);
        mSmsSimSetting.setIcon(R.drawable.gemini_sms);
        mGprsSimSetting.setIcon(R.drawable.gemini_data_connection);
    }
    private void removeDefaultSimPref() {
        PreferenceGroup defaultSettings = (PreferenceGroup) findPreference(KEY_DEFAULT_SIM_SETTINGS_CATEGORY); 

        if (defaultSettings != null) {
            Xlog.d(TAG,"group != null");
            defaultSettings.removePreference(mVideoCallSimSetting);
            defaultSettings.removePreference(mSmsSimSetting); 
        }
        PreferenceGroup generalSettings = (PreferenceGroup) findPreference(KEY_GENERAL_SETTINGS_CATEGORY);         
        if (generalSettings != null) {
            generalSettings.removePreference(findPreference("contacts_sim"));
        }
    }
    private void initIntentFilter() {
        Xlog.d(TAG, "initIntentFilter");
        mIntentFilter = new IntentFilter(
                TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mIntentFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        mIntentFilter
                .addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TRANSACTION_START);
        mIntentFilter.addAction(TRANSACTION_STOP);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_NAME_UPDATE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        mIntentFilter.addAction(ACTION_DATA_USAGE_DISABLED_DIALOG_OK);
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            mIntentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED);
            mIntentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_DONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(TAG, "onResume()");
        if (noNeedRestoreProgDlg) {
            //for CR ALPS00597595
            Xlog.d(TAG,"Unexpected is killed so restore the state but for progess dialog" +
            		" no need as the state has lost");
            removeDialog(PROGRESS_DIALOG);
            noNeedRestoreProgDlg = false;
        }
        mIsVoiceCapable = mTelephonyManager.isVoiceCapable();
        mIsSmsCapable = mTelephonyManager.isSmsCapable();
        sIsVoipAvailable = isVoipAvailable();
        Xlog.d(TAG, "mIsVoiceCapable=" + mIsVoiceCapable + " mIsSmsCapable="
                    + mIsSmsCapable + " sVTCallSupport=" + sVTCallSupport
                    + " sIsVoipAvailable=" + sIsVoipAvailable);
        // get new siminfo value
        getSimInfo();
        removeUnusedPref();
        updatePreferenceUI();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SIM_SETTING),
                                        false, mGprsDefaultSIMObserver);
        } else {
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Global.MOBILE_DATA),
                                        false, mGprsDefaultSIMObserver);
        }
        dealDialogOnResume();
        /* {@M Auto open the other card's data connection. when current card is radio off */
        mExt.dealWithDataConnChanged(null, isResumed());
        /*@}*/
    }

    private void dealDialogOnResume() {
        Xlog.d(TAG,"dealDialogOnResume");
        // if any progress dialog showing and the state is finished mRemoveProgDlg is true then
        if (mRemoveProgDlg) {
            Xlog.d(TAG,"on resume to remove dialog");    
            removeDialog(PROGRESS_DIALOG);
            mRemoveProgDlg = false;
        }
        Xlog.d(TAG,"mRemoveProgDlg = " + mRemoveProgDlg);   

        // if 3G switch confirm dialog is showing while current the radio is off then removed
        if (isRadioOff() && isDialogShowing(DIALOG_3G_MODEM_SWITCH_CONFIRM)) {
            removeDialog(DIALOG_3G_MODEM_SWITCH_CONFIRM);
        }
        if (isRadioOff() && isDialogShowing(DIALOG_GPRS_SWITCH_CONFIRM)) {
            removeDialog(DIALOG_GPRS_SWITCH_CONFIRM);    
        }
    }
    private void dealDlgOnAirplaneMode(String action) {
        if ((Intent.ACTION_AIRPLANE_MODE_CHANGED).equals(action) && isResumed() && isRadioOff()) {
            Xlog.d(TAG,"dealDlgOnAirplaneMode");
            if (isDialogShowing(DIALOG_3G_MODEM_SWITCH_CONFIRM)) {
                removeDialog(DIALOG_3G_MODEM_SWITCH_CONFIRM); 
            } else if (isDialogShowing(DIALOG_GPRS_SWITCH_CONFIRM)) {
                removeDialog(DIALOG_GPRS_SWITCH_CONFIRM);    
            }       
        }
    }
    private void removeUnusedPref() {
        Xlog.d(TAG, "removeUnusedPref()");
        if (!mIsVoiceCapable) {
            sVTCallSupport = false;
        }
        PreferenceGroup pref = (PreferenceGroup) findPreference(KEY_DEFAULT_SIM_SETTINGS_CATEGORY);
        if (!mIsVoiceCapable) {
            pref.removePreference(mVoiceCallSimSetting);
            pref.removePreference(mVideoCallSimSetting);
            if (!mIsSmsCapable) {
                pref.removePreference(mSmsSimSetting);
            }
        }
        if (!FeatureOption.MTK_GEMINI_SUPPORT && 
                !sIsVoipAvailable) {
            pref.removePreference(mVoiceCallSimSetting);
        }
        // if not support vtcall feature then remove this feature
        if (!sVTCallSupport) {
            Xlog.d(TAG, "Video call is " + sVTCallSupport + " remove the pref");
            pref.removePreference(mVideoCallSimSetting);
        }
    }

    private void getSimInfo() {
        Xlog.d(TAG, "getSimInfo()");
        mSiminfoList = SIMInfo.getInsertedSIMList(getActivity());
        mSimNum = mSiminfoList.size();
        Xlog.d(TAG,"total inserted sim card =" + mSimNum);
        Collections.sort(mSiminfoList, new GeminiUtils.SIMInfoComparable());
        // for debug purpose to show the actual sim information
        int slot;
        for (int i = 0; i < mSiminfoList.size(); i++) {
            Xlog.i(TAG, "siminfo.mDisplayName = " + mSiminfoList.get(i).mDisplayName);
            Xlog.i(TAG, "siminfo.mNumber = " + mSiminfoList.get(i).mNumber);
            slot = mSiminfoList.get(i).mSlot;
            Xlog.i(TAG, "siminfo.mSlot = " + slot);
            if (slot == PhoneConstants.GEMINI_SIM_1) {
                mIsSlot1Insert = true;    
            } else if (slot == PhoneConstants.GEMINI_SIM_2) {
                mIsSlot2Insert = true;
            }
            Xlog.i(TAG, "siminfo.mColor = " + mSiminfoList.get(i).mColor);
            Xlog.i(TAG, "siminfo.mDispalyNumberFormat = "
                    + mSiminfoList.get(i).mDispalyNumberFormat);
            Xlog.i(TAG, "siminfo.mSimId = " + mSiminfoList.get(i).mSimId);
        }
    }

    private void updatePreferenceUI() {
        // it will be called in onResume() and no need to call again if current is in background
        if (isResumed()) {
            Xlog.d(TAG, "updatePreferenceUI() and update UI");
            getPreferenceScreen().setEnabled(mSimNum > 0);
            initDefaultSimPreference();
            setPreferenceProperty();
            if (mSimNum > 0) {
                // there is sim card inserted so add sim card pref
                addSimInfoPreference();
            } else {
                setNoSimInfoUi();
            }    
        } else {
            Xlog.d(TAG,"on backgroud no need update preference");
        }
        
    }

    private void setNoSimInfoUi() {
        PreferenceGroup simInfoListCategory = (PreferenceGroup) findPreference(KEY_SIM_INFO_CATEGORY);
        simInfoListCategory.removeAll();
        Preference pref = new Preference(getActivity());
        if (pref != null) {
            pref.setTitle(R.string.gemini_no_sim_indicator);
            simInfoListCategory.addPreference(pref);
        }
        getPreferenceScreen().setEnabled(false);
        // for internet call to enable the voice call setting
        if (mIsVoiceCapable && isVoipAvailable()) {
            mVoiceCallSimSetting.setEnabled(true);
        } else {
            Xlog.d(TAG,"finish() sim management for sim hot swap as mSimNum = " + mSimNum);
            if ("tablet".equals(SystemProperties.get("ro.build.characteristics"))) {
                if (!getResources().getBoolean(
                        com.android.internal.R.bool.preferences_prefer_dual_pane)) {
                    Xlog.i(TAG, "[Tablet] It is single pane, so finish it!");
                    finish();
                } else {
                    Xlog.i(TAG, "[Tablet] It is multi pane, so do not finish it!");
                }
            } else {
                finish();
            }
        }
    }

    private void addSimInfoPreference() {
        Xlog.d(TAG, "addSimInfoPreference()");
        boolean isRadioOn;// sim card is trurned on
        PreferenceGroup simInfoListCategory = (PreferenceGroup) findPreference(KEY_SIM_INFO_CATEGORY);
        simInfoListCategory.removeAll();
        for (final SIMInfo siminfo : mSiminfoList) {
            Xlog.i(TAG, "siminfo.mDisplayName = " + siminfo.mDisplayName);
            Xlog.i(TAG, "siminfo.mNumber = " + siminfo.mNumber);
            Xlog.i(TAG, "siminfo.mSlot = " + siminfo.mSlot);
            Xlog.i(TAG, "siminfo.mColor = " + siminfo.mColor);
            Xlog.i(TAG, "siminfo.mDispalyNumberFormat = "
                    + siminfo.mDispalyNumberFormat);
            Xlog.i(TAG, "siminfo.mSimId = " + siminfo.mSimId);
            // get current status of slot
            int status = getSimIndicator(siminfo.mSlot);
            boolean isAirplaneModeOn = Settings.System.getInt(
                    getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1;
            final SimInfoEnablePreference simInfoPref = new SimInfoEnablePreference(
                    getActivity(), siminfo.mDisplayName, siminfo.mNumber,
                    siminfo.mSlot, status, siminfo.mColor,
                    siminfo.mDispalyNumberFormat, siminfo.mSimId,
                    isAirplaneModeOn);
            Xlog.i(TAG, "simid status is  " + status);
            if (simInfoPref != null) {
                simInfoPref.setClickCallback(this);
                if (mTelephony != null) {
                    try {
                        /**M: add for consistent_UI as single sim not support the function so
                              by using dual sim mode temp @{*/
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            isRadioOn = mTelephony.isRadioOnGemini(siminfo.mSlot);    
                        } else {
                            ///M: fix CR 423704 for single sim card as sim indicate state
                            /// radio off the sim indicator will not give the radio off at first time
                            Xlog.i(TAG, "addSimInfoPreference mIsSIMRadioSwitching = " + mIsSIMRadioSwitching +
                                        "sim status = " + status);
                            if (mIsSIMRadioSwitching) {
                                isRadioOn = false;
                                ///Once the sim indicator is off then able to get real radio state from API and 
                                ///reset the mIsSIMRadioSwitching to let radio state get from real API
                                if (status == PhoneConstants.SIM_INDICATOR_RADIOOFF) {
                                    isRadioOn = mTelephony.isRadioOn(); 
                                    mIsSIMRadioSwitching = false;
                                    Xlog.i(TAG, "radio off finish"); 
                                }
                            } else {
                                isRadioOn = mTelephony.isRadioOn();    
                            }
                        }
                        /**@}*/
                        simInfoPref.setCheck(isRadioOn);
                        Xlog.d(TAG, "sim card " + siminfo.mSlot
                                + " radio state is isRadioOn=" + isRadioOn);
                    } catch (RemoteException e) {
                        Xlog.e(TAG, "mTelephony exception");

                    }
                }
                simInfoPref.setCheckBoxClickListener(
                            new android.widget.CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                Xlog.i(TAG, "receive slot " + siminfo.mSlot
                                        + " switch is clicking! with isChecked=" + isChecked);
                                if (!mIsSIMRadioSwitching) {
                                    Xlog.d(TAG, "start to turn radio in " + isChecked);
                                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                        mIsSIMRadioSwitching = true;     
                                    } else {
                                        ///M: for Cr423704 if radio off then the switch state will be set false
                                        ///not get from isRadio(). mIsSIMRadioSwitching means different with gemini
                                        if (!isChecked) {
                                            mIsSIMRadioSwitching = true;     
                                        }    
                                    }
                                    /**@}*/
                                    simInfoPref.setCheck(isChecked);
                                    if (PhoneConstants.GEMINI_SIM_NUM > 2) {
                                        //for geimini plus
                                        switchGeminiPlusSimRadioState(siminfo.mSlot);
                                    } else {
                                        switchSimRadioState(siminfo.mSlot, isChecked);    
                                    }
                                } else {
                                    Xlog.d(TAG,"Click too fast it is switching " + "and set the switch to previous state");
                                    simInfoPref.setCheck(!isChecked);
                                }
                            }
                        });
                simInfoListCategory.addPreference(simInfoPref);
            }
        }
    }
    
    protected void initDefaultSimPreference() {
        Xlog.d(TAG, "initDefaultSimPreference()");
        mSimItemListVoice.clear();
        mSimItemListVideo.clear();
        mSimItemListSms.clear();
        mSimItemListGprs.clear();
        SimItem simitem;
        int k = 0;
        int simState = 0;
        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo != null) {
                simitem = new SimItem(siminfo);
                simState = getSimIndicator(siminfo.mSlot);
                simitem.mState = simState;
                /*M: due to the state is not sync with data base, it has delay so 
                 * to keep the sync if airplane or dual sim mode is radio off, then
                 * change to radio off event the state now is not real off
                 */
                if (isRadioOff()) {
                    Xlog.d(TAG,"Force the state to be radio off as airplane mode or dual sim mode");
                    simitem.mState = PhoneConstants.SIM_INDICATOR_RADIOOFF;    
                }
                mSimItemListVoice.add(simitem);
                mSimItemListSms.add(simitem);
                mSimItemListGprs.add(simitem);
                // only when vt call support then enable to add sim info into
                // video call pref
                if (sVTCallSupport) {
                    // based on the new UI only 3g switch is available to show video call item
                    mSimItemListVideo.add(simitem);
                }
            }
        }
        // Add internet call item
        if (sIsVoipAvailable) {
            Xlog.d(TAG, "set internet call item");
            simitem = new SimItem(this.getString(R.string.gemini_intenet_call),
                    GeminiUtils.INTERNET_CALL_COLOR,
                    Settings.System.VOICE_CALL_SIM_SETTING_INTERNET);
            mSimItemListVoice.add(simitem);
        }
        simitem = new SimItem(this
                .getString(R.string.gemini_default_sim_always_ask),
                GeminiUtils.NO_COLOR,
                Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK);
        if (mSimItemListVoice.size() > 1) {
            mSimItemListVoice.add(simitem);    
        } else if (mSimItemListVoice.size() == 1 && sIsVoipAvailable) {
            Xlog.d(TAG,"no sim card inserted but internet call is on");
            mSimItemListVoice.add(simitem); 
        }
        
        if (mSimItemListSms.size() > 1) {
            mSimItemListSms.add(simitem);    
        }


        if (mExt.isNeedsetAutoItem()) {
            if (mSimItemListSms.size() > 1) {
                simitem = new SimItem(mExt.getAutoString(),
                    GeminiUtils.NO_COLOR,
                    Settings.System.SMS_SIM_SETTING_AUTO);
                mSimItemListSms.add(simitem);    
            }
        }
        
        simitem = new SimItem(
                this.getString(R.string.gemini_default_sim_never),
                GeminiUtils.NO_COLOR,
                Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER);
        if (isRadioOff()) {
            simitem.mState = PhoneConstants.SIM_INDICATOR_RADIOOFF;
        }
        mSimItemListGprs.add(simitem);
        Xlog.d(TAG, "mSimItemListVoice.size=" + mSimItemListVoice.size()
                + " mSimItemListVideo.size=" + mSimItemListVideo.size()
                + " mSimItemListSms.size=" + mSimItemListSms.size()
                + " mSimItemListSms.size=" + mSimItemListGprs.size());
        // set adapter for each preference
        if (sVTCallSupport) {
            if (mSimItemListVideo.size() == 1) {
                mVideoCallSimSetting.setEnabled(false);    
            } else {
                mVideoCallSimSetting.setInitData(mSimItemListVideo);
                mVideoCallSimSetting.setEnabled(true);    
            }
        }
        if (mIsVoiceCapable) {
            if (mSimItemListVoice.size() == 1) {
                mVoiceCallSimSetting.setEnabled(false); 
            } else {
                mVoiceCallSimSetting.setInitData(mSimItemListVoice);
                mVoiceCallSimSetting.setEnabled(true); 
            }
        }
        if (mSimItemListSms.size() == 1) {
            mSmsSimSetting.setEnabled(false);
        } else {
            mSmsSimSetting.setInitData(mSimItemListSms);
            mSmsSimSetting.setEnabled(true);
        }
        /// Binding contacts disable if only one card inserted
        if (mSimNum == 1) {
            mSimAndContacts.setEnabled(false);
        } else if (mSimNum > 1) { 
            /// if more than one sim card inserted then enabled
            mSimAndContacts.setEnabled(true);
        }
        mGprsSimSetting.setInitData(mSimItemListGprs);
    }

    private int current3GSlotId() {
        int slot3G = VIDEO_CALL_OFF;
        try {
            if (mTelephony != null) {
                slot3G = mTelephony.get3GCapabilitySIM();
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "mTelephony exception");
        }
        return slot3G;
    }

    private void setPreferenceProperty() {
        long voicecallID = getDataValue(Settings.System.VOICE_CALL_SIM_SETTING);
        long smsID = getDataValue(Settings.System.SMS_SIM_SETTING);
        long dataconnectionID = getDataValue(Settings.System.GPRS_CONNECTION_SIM_SETTING);
        int videocallSlotID = current3GSlotId();
        Xlog.i(TAG, "voicecallID =" + voicecallID + " smsID =" + smsID
                + " dataconnectionID =" + dataconnectionID
                + " videocallSlotID =" + videocallSlotID);
        int pos = 0;
        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo != null) {
                if (siminfo.mSimId == voicecallID) {
                    if (mIsVoiceCapable) {
                        mVoiceCallSimSetting.setInitValue(pos);
                        mVoiceCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                }
                if (siminfo.mSimId == smsID) {
                    mSmsSimSetting.setInitValue(pos);
                    mSmsSimSetting.setSummary(siminfo.mDisplayName);
                }
                /**M: consistent_UI as the data connection of single sim use mConnService to 
                      get state of data connection @{*/
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    if (siminfo.mSimId == dataconnectionID) {
                        mGprsSimSetting.setInitValue(pos);
                        mGprsSimSetting.setSummary(siminfo.mDisplayName);
                    }    
                } else {
                    if (mConnService.getMobileDataEnabled()) {
                        mGprsSimSetting.setInitValue(pos);
                        mGprsSimSetting.setSummary(siminfo.mDisplayName);    
                    }
                }
                /**@}*/
                if ((sVTCallSupport)) {
                    if (siminfo.mSlot == videocallSlotID) {
                        mVideoCallSimSetting.setInitValue(pos);
                        mVideoCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                }
            }
            pos++;
        }
        int nSim = mSiminfoList.size();
        if (mIsVoiceCapable) {
            if (voicecallID == Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                mVoiceCallSimSetting.setInitValue(nSim);
                mVoiceCallSimSetting.setSummary(R.string.gemini_intenet_call);
            } else if (voicecallID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                mVoiceCallSimSetting.setInitValue(sIsVoipAvailable ? (nSim + 1)
                        : nSim);
                mVoiceCallSimSetting
                        .setSummary(R.string.gemini_default_sim_always_ask);
            } else if (voicecallID == Settings.System.DEFAULT_SIM_NOT_SET) {
                mVoiceCallSimSetting
                        .setInitValue((int) Settings.System.DEFAULT_SIM_NOT_SET);
                mVoiceCallSimSetting.setSummary(R.string.apn_not_set);
            }
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
        /**M: consistent_UI as the data connection of single sim use mConnService to 
                      get state of data connection @{*/
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (dataconnectionID == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                mGprsSimSetting.setInitValue(nSim);
                mGprsSimSetting.setSummary(R.string.gemini_default_sim_never);
            } else if (dataconnectionID == Settings.System.DEFAULT_SIM_NOT_SET) {
                mGprsSimSetting.setSummary(R.string.apn_not_set);
            }    
        } else {
            ///M: under single sim if the sim is data connection off then the initvalue should be off
            if (!mConnService.getMobileDataEnabled()) {
                mGprsSimSetting.setInitValue(nSim);
                mGprsSimSetting.setSummary(R.string.gemini_default_sim_never);   
            }
        }
        /**@}*/
        if (sVTCallSupport) {
            if (videocallSlotID == VIDEO_CALL_OFF) {
                mIs3gOff = true;
                mVideoCallSimSetting.setSummary(R.string.gemini_default_sim_3g_off);
            } else {
                mIs3gOff = false;
            }
            try {
                if (mTelephony != null) {
                    ///M: if only one card inserted also disable the video item
                    mVideoCallSimSetting.setEnabled(!(mIs3gOff || mTelephony.is3GSwitchLocked() || 
                                                      mSimNum == 1));
                    Xlog.i(TAG, "mIs3gOff=" + mIs3gOff);
                    Xlog.i(TAG, "mTelephony.is3GSwitchLocked() is "
                            + mTelephony.is3GSwitchLocked());
                    }
            } catch (RemoteException e) {
                    Xlog.e(TAG, "mTelephony exception");
                    return;
            }
        }
        mGprsSimSetting.setEnabled(isGPRSEnable());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (KEY_SIM_CONTACTS_SETTINGS.equals(preference.getKey())) {
            if (mSiminfoList.size() == 1) {
                SIMInfo siminfo = mSiminfoList.get(0);
                if (siminfo != null) {
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings",
                            "com.mediatek.gemini.GeminiSIMTetherInfo");
                    int slot = siminfo.mSlot;
                    Xlog.d(TAG, "Enter sim contanct of sim " + siminfo.mSlot);
                    if (slot >= 0) {
                        intent.putExtra("simid", siminfo.mSimId);
                        mSimAndContacts.setIntent(intent);
                    }
                }
            } else {
                // two sim cards inserted to lauch a simListEntrance
                // activity to select edit which sim card
                Bundle extras = new Bundle();
                extras.putInt("type",
                        SimListEntrance.SIM_CONTACTS_SETTING_INDEX);
                startFragment(this, SimListEntrance.class.getCanonicalName(),
                        -1, extras, R.string.gemini_contacts_sim_title);
                Xlog.i(TAG,"startFragment(this, " + "SimListEntrance.class.getCanonicalName(), -1, extras);");
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        final String key = arg0.getKey();
        Xlog.i(TAG, "Enter onPreferenceChange function with " + key);
        if (KEY_VOICE_CALL_SIM_SETTING.equals(key)) {
            Settings.System.putLong(getContentResolver(),
                    Settings.System.VOICE_CALL_SIM_SETTING, (Long) arg1);
            Intent intent = new Intent(
                    Intent.ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED);
            intent.putExtra("simid", (Long) arg1);
            getActivity().sendBroadcast(intent);
            Xlog.d(TAG, "send broadcast voice call change with simid="
                    + (Long) arg1);
            updateDefaultSIMSummary(mVoiceCallSimSetting, (Long) arg1);
        } else if (KEY_VIDEO_CALL_SIM_SETTING.equals(key)) {
            if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                mSelectedVideoSimId = (Long) arg1;
                showDialog(DIALOG_3G_MODEM_SWITCH_CONFIRM);
                setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        updateVideoCallDefaultSIM();
                    }
                });
            }
        } else if (KEY_SMS_SIM_SETTING.equals(key)) {
            Settings.System.putLong(getContentResolver(),
                    Settings.System.SMS_SIM_SETTING, (Long) arg1);
            Intent intent = new Intent(Intent.ACTION_SMS_DEFAULT_SIM_CHANGED);
            intent.putExtra("simid", (Long) arg1);
            getActivity().sendBroadcast(intent);
            Xlog.d(TAG, "send broadcast sms change with simid=" + (Long) arg1);
            updateDefaultSIMSummary(mSmsSimSetting, (Long) arg1);
        } else if (KEY_GPRS_SIM_SETTING.equals(key)) {
            long simid = ((Long) arg1).longValue();
            Xlog.d(TAG, "value=" + simid);
            ///M: only gemini need to show a dialop @{
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (simid == 0) {
                    ///M: turn off data connection no need to get whether to show connection dlg
                    mDataSwitchMsgIndex = -1;    
                } else {
                    mDataSwitchMsgIndex = dataSwitchConfirmDlgMsg(simid);    
                }
            }
            ///@}
            if (mDataSwitchMsgIndex == -1 || !FeatureOption.MTK_GEMINI_SUPPORT) {
                switchGprsDefautlSIM(simid);
            } else {
                mSelectedGprsSimId = simid;
                showDialog(DIALOG_GPRS_SWITCH_CONFIRM);
                setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        updateDataConnPrefe();
                    }
                });
            }
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Xlog.d(TAG, "OnPause()");
        getContentResolver().unregisterContentObserver(mGprsDefaultSIMObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Xlog.d(TAG, "onDestroy()");
        getActivity().unregisterReceiver(mSimReceiver);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            try {
                if (mTelephony != null) {
                    mTelephony.unregisterForSimModeChange(mSwitchRadioStateMsg.getBinder());
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mTelephony exception");
                return;
            }    
        }
        mCellConnMgr.unregister();
        //to remove the msg from handler since on destroyed
        mTimerHandler.removeMessages(DATA_SWITCH_TIME_OUT_MSG);
        mTimerHandler.removeMessages(DIALOG_NOT_SHOW_SUCCESS_MSG);
        mTimerHandler.removeMessages(DATA_3G_SWITCH_TIME_OUT_MSG);
        mTimerHandler.removeMessages(EVENT_DUAL_SIM_MODE_CHANGED_COMPLETE);
    }

    private void updateDefaultSIMSummary(DefaultSimPreference pref, Long simid) {
        Xlog.d(TAG, "updateDefaultSIMSummary() with simid=" + simid);
        if (simid > 0) {
            SIMInfo siminfo = getSIMInfoById(simid);
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

    /**
     * Get the corresponding siminfo by simid
     * 
     * @param simid
     *            is the sim card id
     */
    private SIMInfo getSIMInfoById(Long simid) {
        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo.mSimId == simid) {
                return siminfo;
            }
        }
        Xlog.d(TAG, "Error there is no correct siminfo found by simid " + simid);
        return null;
    }

    private int dataSwitchConfirmDlgMsg(long simid) {
        SIMInfo siminfo = findSIMInfoBySimId(simid);
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isInRoaming = telephonyManagerEx.isNetworkRoaming(siminfo.mSlot);
        boolean isRoamingDataAllowed = (siminfo.mDataRoaming == SimInfo.DATA_ROAMING_ENABLE);
        Xlog.d(TAG, "isInRoaming=" + isInRoaming + " isRoamingDataAllowed="
                + isRoamingDataAllowed);
        // by support 3G switch when data connection switch
        // and to a slot not current set 3G service
        if (isInRoaming) {
            if (!isRoamingDataAllowed) {
                if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                    if (siminfo.mSlot != current3GSlotId()) {
                        // under roaming but not abled and switch card is not 3G
                        // slot, \
                        // to pormpt user turn on roaming and how to modify to
                        // 3G service
                        return R.string.gemini_3g_disable_warning_case3;
                    } else {
                        // switch card is 3G slot but not able to roaming
                        // so only prompt to turn on roaming
                        return R.string.gemini_3g_disable_warning_case0;
                    }
                } else {
                    // no support 3G service so only prompt user to turn on
                    // roaming
                    return R.string.gemini_3g_disable_warning_case0;
                }
            } else {
                if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                    if (siminfo.mSlot != current3GSlotId()) {
                        // by support 3g switch and switched sim is not
                        // 3g slot to prompt user how to modify 3G service
                        return R.string.gemini_3g_disable_warning_case1;
                    }
                }
            }
        } else {
            if (FeatureOption.MTK_GEMINI_3G_SWITCH
                    && siminfo.mSlot != current3GSlotId()) {
                // not in roaming but switched sim is not 3G
                // slot so prompt user to modify 3G service
                return R.string.gemini_3g_disable_warning_case1;
            }

        }
        return -1;
    }

    private int findListPosBySimId(long simid) {
        int size = mSiminfoList.size();
        int pos = -1;
        // if only one sim card inserted the pos must be 0
        if (size == 1) {
            pos = 0;
        }
        // if two sim inserted then the pos would order as the slot sequence
        if (size > 1) {
            SIMInfo tempSIMInfo = findSIMInfoBySimId(simid);
            if (tempSIMInfo == null) {
                Xlog.d(TAG,
                        "Error can not find the sim id with related siminfo");
            } else {
                pos = tempSIMInfo.mSlot;
            }
        }
        Xlog.d(TAG, size + " sim card inserted and the sim is in pos with " + pos);
        return pos;
    }

    private SIMInfo findSIMInfoBySimId(long simid) {
        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo.mSimId == simid) {
                return siminfo;
            }
        }
        Xlog.d(TAG, "Error happend on findSIMInfoBySimId no siminfo find");
        return null;
    }

    private SIMInfo findSIMInofBySlotId(int mslot) {
        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo.mSlot == mslot) {
                return siminfo;
            }
        }
        Xlog.d(TAG, "Error happend on findSIMInofBySlotId no siminfo find");
        return null;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Xlog.d(TAG,"onCreateDialog() with id = " + id);
        Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDlg;
        switch (id) {
        case PROGRESS_DIALOG:
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getResources().getString(mProDlgMsgId));
            dialog.setIndeterminate(true);
            if (mProDlgMsgId == R.string.gemini_3g_modem_switching_message) {
                Xlog.d(TAG,"3G switch to dispatch home key");
                Window win = dialog.getWindow();
                WindowManager.LayoutParams lp = win.getAttributes();
                lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
                win.setAttributes(lp);
                setStatusBarEnableStatus(false);
            }
            return dialog;
        case DIALOG_GPRS_SWITCH_CONFIRM:
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getResources().getString(mDataSwitchMsgIndex));
            builder.setPositiveButton(android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int whichButton) {
                            if ((mDataSwitchMsgIndex == R.string.gemini_3g_disable_warning_case0)) {
                                enableDataRoaming(mSelectedGprsSimId);
                            }
                            switchGprsDefautlSIM(mSelectedGprsSimId);
                        }
                    });
            builder.setNegativeButton(android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int whichButton) {
                            updateDataConnPrefe();
                        }
                    });
            alertDlg = builder.create();
            return alertDlg;
        case DIALOG_3G_MODEM_SWITCH_CONFIRM:
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getResources().getString(
                    R.string.gemini_3g_modem_switch_confirm_message));
            builder.setPositiveButton(android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int whichButton) {
                            switchVideoCallDefaultSIM(mSelectedVideoSimId);
                        }
                    });
            builder.setNegativeButton(android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int whichButton) {
                            updateVideoCallDefaultSIM();
                        }
                    });
            alertDlg = builder.create();
            return alertDlg;
        default:
            return null;
        }
    }
    private int getSimIndicator(int slotId) {
        Xlog.d(TAG,"getSimIndicator---slotId=" + slotId);
        try {
            if (mTelephony != null) {
                return FeatureOption.MTK_GEMINI_SUPPORT ? 
                        mTelephony.getSimIndicatorStateGemini(slotId)
                      : mTelephony.getSimIndicatorState();
            } else {
                return PhoneConstants.SIM_INDICATOR_UNKNOWN;
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "RemoteException");
            return PhoneConstants.SIM_INDICATOR_UNKNOWN;
        } catch (NullPointerException ex) {
            Xlog.e(TAG, "NullPointerException");
            return PhoneConstants.SIM_INDICATOR_UNKNOWN;
        }
    }

    private void switchSimRadioState(int slot, boolean isChecked) {
        Xlog.d(TAG,"switchSimRadioState");
        int dualSimMode = Settings.System.getInt(this.getContentResolver(),
                Settings.System.DUAL_SIM_MODE_SETTING, -1);
        Xlog.i(TAG, "The current dual sim mode is " + dualSimMode);
        /* {@M Auto open the other card's data connection. when current card is radio off */ 
        mExt.setToClosedSimSlot(-1);
        /*@}*/
        int dualState = 0;
        boolean isRadioOn = false;
        switch (dualSimMode) {
        case ALL_RADIO_OFF:
            if (slot == PhoneConstants.GEMINI_SIM_1) {
                dualState = SIM_SLOT_1_RADIO_ON;
            } else if (slot == PhoneConstants.GEMINI_SIM_2) {
                dualState = SIM_SLOT_2_RADIO_ON;
            }
            Xlog.d(TAG, "Turning on only sim " + slot);
            isRadioOn = true;
            break;
        case SIM_SLOT_1_RADIO_ON:
            if (slot == PhoneConstants.GEMINI_SIM_1) {
                if (isChecked) {
                    Xlog.d(TAG,"try to turn on slot 1 again since it is already on");
                    dualState = dualSimMode;
                    isRadioOn = true; 
                } else {
                    dualState = ALL_RADIO_OFF;
                    isRadioOn = false;    
                }
                Xlog.d(TAG, "Turning off sim " + slot
                        + " and all sim radio is off");
            } else if (slot == PhoneConstants.GEMINI_SIM_2) {
                if (mIsSlot1Insert) {
                    dualState = ALL_RADIO_ON;
                    Xlog.d(TAG, "sim 0 was radio on and now turning on sim "
                            + slot);
                } else {
                    dualState = SIM_SLOT_2_RADIO_ON;
                    Xlog.d(TAG, "Turning on only sim " + slot);
                }
                isRadioOn = true;
            }
            break;
        case SIM_SLOT_2_RADIO_ON:
            if (slot == PhoneConstants.GEMINI_SIM_2) {
                if (isChecked) {
                    Xlog.d(TAG,"try to turn on slot 2 again since it is already on");
                    dualState = dualSimMode;
                    isRadioOn = true;     
                } else {
                    dualState = ALL_RADIO_OFF;
                    isRadioOn = false;   
                }
                Xlog.d(TAG, "Turning off sim " + slot
                        + " and all sim radio is off");
            } else if (slot == PhoneConstants.GEMINI_SIM_1) {
                if (mIsSlot2Insert) {
                    dualState = ALL_RADIO_ON;
                    Xlog.d(TAG, "sim 1 was radio on and now turning on sim " + slot);
                } else {
                    dualState = SIM_SLOT_1_RADIO_ON;
                    Xlog.d(TAG, "Turning on only sim " + slot);
                }
                isRadioOn = true;
            }
            break;
        case ALL_RADIO_ON:
            if (!isChecked) {
                if (slot == PhoneConstants.GEMINI_SIM_1) {
                    dualState = SIM_SLOT_2_RADIO_ON;
                    /* {@M Auto open the other card's data connection. when current card is radio off */ 
                    mExt.setToClosedSimSlot(PhoneConstants.GEMINI_SIM_1);
                    /*@}*/
                    } else if (slot == PhoneConstants.GEMINI_SIM_2) {
                        dualState = SIM_SLOT_1_RADIO_ON;
                        /* {@M Auto open the other card's data connection. when current card is radio off */ 
                        mExt.setToClosedSimSlot(PhoneConstants.GEMINI_SIM_2);
                        /*@}*/
                    }
                Xlog.d(TAG, "Turning off only sim " + slot);
                isRadioOn = false;    
            } else {
                Xlog.d(TAG,"try to turn on but actually they are all on");
                dualState = dualSimMode;
                isRadioOn = true;    
            }
            break;
        default:
            Xlog.d(TAG, "Error not correct values");
            return;
        }
        ///M: only gemini support to show a dialog, for single sim do not show this dlg @{
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int msgId = 0;
            if (isRadioOn) {
                msgId = R.string.gemini_sim_mode_progress_activating_message;
            } else {
                msgId = R.string.gemini_sim_mode_progress_deactivating_message;
            }
            showProgressDlg(msgId);
        }
        ///@}
        Xlog.d(TAG, "dualState=" + dualState + " isRadioOn=" + isRadioOn);
        Settings.System.putInt(this.getContentResolver(),
                Settings.System.DUAL_SIM_MODE_SETTING, dualState);
        Intent intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, dualState);
        getActivity().sendBroadcast(intent);
    }

     private int getInverseNumber(int num) {
        int constNum = 4;
        String inverseStr = Integer.toBinaryString(~num);
        String str = inverseStr.substring(inverseStr.length() - constNum);
        int inverseNum = Integer.parseInt(str , 2);
        Xlog.d(TAG,"inverseNum = " + inverseNum);
        return inverseNum;
    }

    private void switchGeminiPlusSimRadioState(int slot) {
        Xlog.d(TAG,"switchGeminiPlusSimRadioState");
        int dualSimMode = Settings.System.getInt(this.getContentResolver(),
                Settings.System.DUAL_SIM_MODE_SETTING, -1);
        int modeSlot = slot;
        int dualState;
        boolean isRadioOn = false;
        Xlog.i(TAG, "The current dual sim mode is " + dualSimMode + "with slot = " + slot);
        switch (slot) {
            case PhoneConstants.GEMINI_SIM_1:
            modeSlot = 1;//01
            break;
            case PhoneConstants.GEMINI_SIM_2:
            modeSlot = 2;//10
            break;
            case PhoneConstants.GEMINI_SIM_3:
            modeSlot = 4;//100
            break;
            case PhoneConstants.GEMINI_SIM_4:
            modeSlot = 8;//1000
            break;
            default:
            Xlog.d(TAG,"error of the slot = " + slot);
            break;
        }
        if ((dualSimMode & modeSlot) > 0) {
            dualState = dualSimMode & getInverseNumber(modeSlot);
            isRadioOn = false;
        } else {
            dualState = dualSimMode | modeSlot;
            isRadioOn = true;
        }
        ///M: only gemini support to show a dialog, for single sim do not show this dlg @{
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int msgId = 0;
            if (isRadioOn) {
                msgId = R.string.gemini_sim_mode_progress_activating_message;
            } else {
                msgId = R.string.gemini_sim_mode_progress_deactivating_message;
            }
            showProgressDlg(msgId);
        }
        ///@}
        Xlog.d(TAG, "dualState=" + dualState + " isRadioOn=" + isRadioOn);
        Settings.System.putInt(this.getContentResolver(),
                Settings.System.DUAL_SIM_MODE_SETTING, dualState);
        Intent intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, dualState);
        getActivity().sendBroadcast(intent);
    }

    private void dealWithSwtichComplete() {
        Xlog.d(TAG, "dealWithSwtichComplete()");
        Xlog.d(TAG, "mIsSIMModeSwitching is " + mIsSIMRadioSwitching);
        if (!mIsSIMRadioSwitching) {
            Xlog.i(TAG, "dual mode change by other not sim management");
        } else {
            if (!isDialogShowing(PROGRESS_DIALOG)) {
                Xlog.d(TAG,"Dialog is not show yet but dual sim modechange has sent msg");
                mTimerHandler.sendEmptyMessageDelayed(DIALOG_NOT_SHOW_SUCCESS_MSG, DIALOG_NOT_REMOVE_TIME_OUT);
            } else {
                removeProgDlg();
                mIsSIMRadioSwitching = false;    
            }
        }
        mGprsSimSetting.setEnabled(isGPRSEnable());
        /* {@M Auto open the other card's data connection. when current card is radio off */
        mExt.showChangeDataConnDialog(this, isResumed());
        /*@}*/
    }

    private void removeProgDlg() {
        Xlog.d(TAG,"removeProgDlg()");
        if (isResumed()) {
            Xlog.d(TAG,"Progress Dialog removed");
            removeDialog(PROGRESS_DIALOG);    
        } else {
            Xlog.d(TAG,"under onpause not enable to remove set flag as true");
            mRemoveProgDlg = true;
        }    
    }
    /**
     *show attach gprs dialog and revent time out to send a delay msg
     * 
     */
    private void showDataConnDialog(boolean isConnect) {
        long delaytime = 0;
        if (isConnect) {
            delaytime = ATTACH_DATA_CONN_TIME_OUT;
        } else {
            delaytime = DETACH_DATA_CONN_TIME_OUT;
        }
        mTimerHandler.sendEmptyMessageDelayed(DATA_SWITCH_TIME_OUT_MSG, delaytime);
        showProgressDlg(R.string.gemini_data_connection_progress_message);
        mIsDataConnectActing = true;
    }

    private void showProgressDlg(int dialogMsg) {
        Xlog.d(TAG,"showProgressDlg() with dialogMsg = " + dialogMsg);
        mProDlgMsgId = dialogMsg;
        showDialog(PROGRESS_DIALOG);
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

    /*
     * Update dataconnection prefe with new selected value and new sim name as
     * summary
     */
    private void updateDataConnPrefe() {
        long simid = Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            simid = Settings.System.getLong(getContentResolver(),
                    Settings.System.GPRS_CONNECTION_SIM_SETTING,
                    Settings.System.DEFAULT_SIM_NOT_SET);
        } else {
            if (mConnService.getMobileDataEnabled()) {
                if (mSiminfoList != null
                        && mSiminfoList.size() > 0) {
                    simid = mSiminfoList.get(0).mSimId;
                }
            }
        }

        Xlog.i(TAG, "Gprs connection SIM changed with simid is " + simid);
        if (simid == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
            // data connection is set to close so the
            // initvalue would be the last one which is size of mSiminfoList
            mGprsSimSetting.setInitValue(mSiminfoList.size());
            mGprsSimSetting.setSummary(R.string.gemini_default_sim_never);
        } else if (simid > Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
            // By usding the sim id to find corresponding
            // siminfo and position in gprs pref then update
            int pos = findListPosBySimId(simid);
            SIMInfo siminfo = findSIMInfoBySimId(simid);
            if (siminfo == null) {
                Xlog.d(TAG, "Error no correct siminfo get...");
                return;
            }
            mGprsSimSetting.setInitValue(pos);
            mGprsSimSetting.setSummary(siminfo.mDisplayName);
        } else {
            Xlog.d(TAG, "Error wrong simid need to check...");
        }
    }

    /**
     * update video call default SIM value and summary
     */

    private void updateVideoCallDefaultSIM() {
        Xlog.d(TAG, "updateVideoCallDefaultSIM()");
        if (mTelephony != null) {
            try {
                int slotId = mTelephony.get3GCapabilitySIM();
                Xlog.d(TAG, "updateVideoCallDefaultSIM()---slotId=" + slotId);
                if (slotId < 0) {
                    return;
                }
                SIMInfo siminfo = findSIMInofBySlotId(slotId);
                if (siminfo != null) {
                    int pos = findListPosBySimId(siminfo.mSimId);
                    if (pos >= 0) {
                        mVideoCallSimSetting.setInitValue(pos);
                        mVideoCallSimSetting.setSummary(siminfo.mDisplayName);
                    }
                } else {
                    Xlog.d(TAG, "mVideoCallSimSetting.setInitValue(-1)");
                    mVideoCallSimSetting.setInitValue(-1);
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mTelephony exception");
            }
        }
    }

    /**
     * Check if voip is supported and is enabled
     */
    private boolean isVoipAvailable() {
        int isInternetCallEnabled = android.provider.Settings.System.getInt(
                getContentResolver(),
                android.provider.Settings.System.ENABLE_INTERNET_CALL, 0);
        return (SipManager.isVoipSupported(getActivity()))
                && (isInternetCallEnabled != 0);

    }

    /**
     * switch data connection default SIM
     * 
     * @param value: sim id of the new default SIM
     */
    private void switchGprsDefautlSIM(long simid) {
        Xlog.d(TAG, "switchGprsDefautlSIM() with simid=" + simid);
        //M: solve CR ALPS00609944 when hot swap the target simId has been plug out so do nothing
        if (simid < 0 || simid > 0 && !isSimInsertedIn(simid)) {
            Xlog.d(TAG,"simid = " + simid + " not available anymore");
            return;
        }
        boolean isConnect = (simid > 0) ? true : false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            long curConSimId = Settings.System.getLong(getContentResolver(),
                Settings.System.GPRS_CONNECTION_SIM_SETTING,
                Settings.System.DEFAULT_SIM_NOT_SET);
            Xlog.d(TAG,"curConSimId=" + curConSimId);
            if (simid == curConSimId) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
            intent.putExtra("simid", simid);
            // simid>0 means one of sim card is selected
            // and <0 is close id which is -1 so mean disconnect
            getActivity().sendBroadcast(intent);   
            showDataConnDialog(isConnect); 
        } else {
            ///M: for consistent_UI only sinlge sim use connectivity set the state of data connection
            mConnService.setMobileDataEnabled(isConnect);
        }
    }

    private void enableDataRoaming(long value) {
        Xlog.d(TAG, "enableDataRoaming with SimId=" + value);
        if (isSimInsertedIn(value)) {
        try {
            if (mTelephony != null) {
                mTelephony.setDataRoamingEnabledGemini(true, SIMInfo
                        .getSlotById(getActivity(), value));
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "mTelephony exception");
            return;
        }
            SIMInfo.setDataRoaming(getActivity(), SimInfo.DATA_ROAMING_ENABLE, value);
        } else {
            Xlog.d(TAG,"sim Id " + value + " not inserted in phone do nothing");
        }
    }
    private boolean isSimInsertedIn(long simId) {
        for (SIMInfo siminfo : mSiminfoList) {
            if (siminfo.mSimId == simId) {
                return true;
            }    
        }
        Xlog.d(TAG,"simid = " + simId + " not inserted in phone");
        return false;
    }
    /**
     * switch video call prefer sim if 3G switch feature is enabled
     * 
     * @param slotID
     */
    private void switchVideoCallDefaultSIM(long simid) {
        Xlog.i(TAG, "switchVideoCallDefaultSIM to " + simid);
        if (mTelephony != null) {
            SIMInfo siminfo = findSIMInfoBySimId(simid);
            Xlog.i(TAG, "siminfo = " + siminfo);
            if (siminfo == null) {
                Xlog.d(TAG, "Error no corrent siminfo found");
                return;
            }
            try {
                Xlog.i(TAG, "set sim slot " + siminfo.mSlot
                        + " with 3G capability");
                if (mTelephony.set3GCapabilitySIM(siminfo.mSlot)) {
                    showProgressDlg(R.string.gemini_3g_modem_switching_message);
                    mTimerHandler.sendEmptyMessageDelayed(DATA_3G_SWITCH_TIME_OUT_MSG, SWITCH_3G_TIME_OUT);
                } else {
                    updateVideoCallDefaultSIM();
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "mTelephony exception");
                return;
            }

        }
    }

    /**
     * When switching modem, the status bar should be disabled
     * @param enabled
     */
    private void setStatusBarEnableStatus(boolean enabled) {
        Xlog.i(TAG, "setStatusBarEnableStatus(" + enabled + ")");
        StatusBarManager statusBarManager;
        statusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBarManager != null) {
            if (enabled) {
                statusBarManager.disable(StatusBarManager.DISABLE_NONE);
            } else {
                statusBarManager.disable(StatusBarManager.DISABLE_EXPAND |
                                         StatusBarManager.DISABLE_RECENT |
                                         StatusBarManager.DISABLE_HOME);
            }
        } else {
            Xlog.e(TAG, "Fail to get status bar instance");
        }
    }

    private long getDataValue(String dataString) {
        return Settings.System.getLong(getContentResolver(), dataString,
                Settings.System.DEFAULT_SIM_NOT_SET);
    }

    /**
     * Returns whether is in airplance or mms is under transaction
     * 
     * @return is airplane or mms is in transaction
     * 
     */
    private boolean isGPRSEnable() {
        boolean isMMSProcess = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            if (networkInfo != null) {
                NetworkInfo.State state = networkInfo.getState();
                Xlog.d(TAG,"mms state = " + state);
                isMMSProcess = (state == NetworkInfo.State.CONNECTING
                    || state == NetworkInfo.State.CONNECTED);
            }
        }
        boolean isRadioOff = isRadioOff();
        Xlog.d(TAG, "isMMSProcess=" + isMMSProcess + " isRadioOff="
                + isRadioOff);
        return !(isMMSProcess || isRadioOff);
    }
    /**
     * @return is airplane mode or all sim card is set on radio off
     * 
     */
    private boolean isRadioOff() {
        boolean isAllRadioOff = (Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1)
                || (Settings.System.getInt(getContentResolver(),
                        Settings.System.DUAL_SIM_MODE_SETTING, -1) == ALL_RADIO_OFF)
                || mSimNum == 0;
        Xlog.d(TAG, "isAllRadioOff=" + isAllRadioOff);
        return isAllRadioOff;
    }

    @Override
    public void onPreferenceClick(long simid) {
        Bundle extras = new Bundle();
        extras.putLong(GeminiUtils.EXTRA_SIMID, simid);
        startFragment(this, SimInfoEditor.class.getCanonicalName(), -1, extras,
                R.string.gemini_sim_info_title);
        Xlog.i(TAG, "startFragment " + SimInfoEditor.class.getCanonicalName());
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isDialogShowing(PROGRESS_DIALOG)) {
            outState.putInt(PROGRESS_DIALOG_MSG_ID,mProDlgMsgId);
        } else if (isDialogShowing(DIALOG_GPRS_SWITCH_CONFIRM)) {
            outState.putInt(CONFIRM_DIALOG_MSG_ID,mDataSwitchMsgIndex);
        }
    }
}
