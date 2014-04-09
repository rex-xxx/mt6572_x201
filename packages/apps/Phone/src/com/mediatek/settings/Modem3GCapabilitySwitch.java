package com.mediatek.settings;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneInterfaceManager;
import com.android.phone.R;

import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.SettingsExtension;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class Modem3GCapabilitySwitch extends PreferenceActivity 
        implements Preference.OnPreferenceChangeListener {

    public static final String SERVICE_LIST_KEY = "preferred_3g_service_key";
    public static final String NETWORK_MODE_KEY = "preferred_network_mode_key";

    private ServiceSelectList mServiceList = null;
    private ListPreference mNetworkMode = null;
    private static final boolean DBG = true;
    private static final String TAG = "Settings/Modem3GCapabilitySwitch";
    
    PhoneInterfaceManager mPhoneMgr = null;
    private GeminiPhone mGeminiPhone;
    private Phone mPhone;
    MyHandler mHandler;
    private StatusBarManager mStatusBarManager = null;
    private ProgressDialog mPD = null;
    private ModemSwitchReceiver mSlr;
    //As this activity may be destroyed and re-instance, give them a public "progress dialog"
    private ProgressDialog mPDSwitching = null;
    
    private static final int SIMID_3G_SERVICE_OFF = -1;
    private static final int SIMID_3G_SERVICE_NOT_SET = -2;

    private static final int SWITCH_3G_TIME_OUT_MSG = 1000;
    ///For 3G switch fail time out set 1 min
    private static final int SWITCH_3G_TIME_OUT_VALUE= 60000;


    
    private static int sInstanceFlag = 0;
    private int mInstanceIndex = 0;

    private SettingsExtension mExtension;

    private Handler mTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (SWITCH_3G_TIME_OUT_MSG == msg.what) {
                Xlog.d("TEST","3G switch time out remove the progress dialog");
                disSwitchProgressDialog();
                setStatusBarEnableStatus(true);
            }
        }
    }; 

    public Modem3GCapabilitySwitch() {
        mInstanceIndex = ++sInstanceFlag;
        Xlog.i(TAG, "Modem3GCapabilitySwitch(), instanceIndex=" + mInstanceIndex);
    }
    
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Xlog.d(TAG,"onCreate()");
        this.addPreferencesFromResource(R.xml.service_3g_setting);
        mServiceList = (ServiceSelectList)findPreference(SERVICE_LIST_KEY);
        mNetworkMode = (ListPreference)findPreference(NETWORK_MODE_KEY);
        mServiceList.setOnPreferenceChangeListener(this);
        mNetworkMode.setOnPreferenceChangeListener(this);
        mPhone = PhoneFactory.getDefaultPhone();
        if (CallSettings.isMultipleSim()) {
            mGeminiPhone = (GeminiPhone)mPhone;
        }
        mHandler = new MyHandler();
        mPhoneMgr = PhoneGlobals.getInstance().phoneMgr;

        mSlr = new ModemSwitchReceiver();
        IntentFilter intentFilter = new IntentFilter(GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED);
        intentFilter.addAction(GeminiPhone.EVENT_PRE_3G_SWITCH);
        intentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_DONE);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        registerReceiver(mSlr, intentFilter);
        /// M: For alps00604948 @{
        // get 3G Capability slot,then ,set network mode.
        int slot = mPhoneMgr.get3GCapabilitySIM();
        if (slot >= 0 && (CallSettings.getBaseband(slot) & NetworkEditor.MODEM_MASK_TDSCDMA) != 0) {
            mNetworkMode.setEntries(getResources().getTextArray(
                    R.array.umts_network_preferences_choices_cmcc));
        }
        /// @}

        mExtension = ExtensionManager.getInstance().getSettingsExtension();
        mExtension.removeNMOpFor3GSwitch(getPreferenceScreen(), mNetworkMode);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    protected void onResume() {
        super.onResume();
        long simId = SIMID_3G_SERVICE_NOT_SET;
        int slot = mPhoneMgr.get3GCapabilitySIM();
        if (slot == SIMID_3G_SERVICE_OFF) {
            simId = slot;
        } else {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, slot);
            simId = info != null ? info.mSimId : SIMID_3G_SERVICE_NOT_SET;
        }
        
        updateSummarys(simId);
        updateNetworkMode();
        updateItemStatus();
        update3GService();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateNetworkMode() {
        if (mNetworkMode == null) {
            return ;
        }
        int slot = mPhoneMgr.get3GCapabilitySIM();
        boolean enabled =  mPhoneMgr.is3GSwitchLocked();
        Xlog.d(TAG, "updateNetworkMode(), 3G capability slot=" + slot);
        if (!enabled && CallSettings.isMultipleSim() && slot != -1 && CallSettings.isRadioOn(slot)) {
            mNetworkMode.setEnabled(true);
            Xlog.d(TAG, "Try to get preferred network mode for slot " + slot);
            mGeminiPhone.getPreferredNetworkTypeGemini(
                    mHandler.obtainMessage(MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE), slot);
        } else {
            mNetworkMode.setEnabled(false);
            mNetworkMode.setSummary("");
        }
    }

    private void update3GService() {
        boolean enabled =  mPhoneMgr.is3GSwitchLocked();
        if (enabled) {
            return;
        }
        if (CallSettings.isMultipleSim()) {
            boolean isAirMode = Settings.System.getInt(
                    getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1;
            mServiceList.setEnabled(!isAirMode);   
            if (isAirMode) {
                mServiceList.dismissDialogs();
            }
        }
    }

    private void updateSummarys(long simId) {
        if (mServiceList == null) {
            return ;
        }
        Xlog.d(TAG, "updateSummarys(), simId=" + simId);
        if (simId == SIMID_3G_SERVICE_OFF) {
            //3G service is off
            mServiceList.setSummary(R.string.service_3g_off);
            if (mNetworkMode != null) {
                mNetworkMode.setEnabled(false);
                mNetworkMode.setSummary("");
            }
        } else if (simId == SIMID_3G_SERVICE_NOT_SET) {
            //Clear the summary
            mServiceList.setSummary("");
            mNetworkMode.setEnabled(false);
            mNetworkMode.setSummary("");
        } else {
            SIMInfo info = SIMInfo.getSIMInfoById(this, simId);
            if (info != null) {
                mServiceList.setSummary(info.mDisplayName);
                //if the 3G service slot is radio off, disable the network mode
                boolean isPowerOn = CallSettings.isRadioOn(info.mSlot);
                Xlog.d(TAG, "updateSummarys(), SIM " + simId + " power status is " + isPowerOn);
                mNetworkMode.setEnabled(isPowerOn);
                if (!isPowerOn) {
                    mNetworkMode.setSummary("");
                }
            }
        }
    }
    
    public void changeForNetworkMode(Object objValue) {
        mNetworkMode.setValue((String) objValue);
        int buttonNetworkMode;
        buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE, Phone.PREFERRED_NT_MODE);
        if (buttonNetworkMode != settingsNetworkMode) {
            showProgressDialog();
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
                    modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                    break;
            }
            
            updatePreferredNetworkModeSummary(buttonNetworkMode);

            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    buttonNetworkMode);
            //Set the modem network mode
            int slot = mPhoneMgr.get3GCapabilitySIM();
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.setPreferredNetworkTypeGemini(modemNetworkMode, 
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), slot);
            } else {
                mPhone.setPreferredNetworkType(modemNetworkMode, 
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mServiceList) {
            long simId = Long.valueOf(objValue.toString());
            //updateSummarys(simId);
            //updateNetworkMode();
            handleServiceSwitch(simId);
        } else if (preference == mNetworkMode) {
            changeForNetworkMode(objValue);
        }
        return true;
    }

    void showSwitchProgress() {
        if (mPDSwitching != null) {
            Xlog.d(TAG, "The progress dialog already exist, so exit!");
            return ;
        }
        
        mPDSwitching = new ProgressDialog(this);
        if (mPDSwitching != null) {
            mPDSwitching.setMessage(getResources().getString(R.string.modem_switching));
        }
        Xlog.d(TAG, "Create and show the progress dialog...");
        mPDSwitching.setCancelable(false);
        
        Window win = mPDSwitching.getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
        win.setAttributes(lp);
        mPDSwitching.show();
    }

    void clearAfterSwitch(Intent it) {
        long simId3G = SIMID_3G_SERVICE_NOT_SET;
        Xlog.d(TAG, "clearAfterSwitch(), remove switching dialog");
        disSwitchProgressDialog();
        setStatusBarEnableStatus(true);
        //the slot which supports 3g service after switch
        //then get the simid which inserted to the 3g slot
        int slot3G = it.getIntExtra(GeminiPhone.EXTRA_3G_SIM, SIMID_3G_SERVICE_NOT_SET);
        if (slot3G == SIMID_3G_SERVICE_OFF) {
            simId3G = SIMID_3G_SERVICE_OFF;
        } else {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, slot3G);
            if (info != null) {
                simId3G = info.mSimId;
            }
        }

        this.updateSummarys(simId3G);
        this.updateNetworkMode();
    }
    

    private void disSwitchProgressDialog() {
        Xlog.d(TAG, "disSwitchProgressDialog()");
        if (mPDSwitching != null && mPDSwitching.isShowing()) {
            Xlog.d(TAG, "disSwitchProgressDialog(), take effect");
            mPDSwitching.dismiss();
            mPDSwitching = null;
        } else {
            Xlog.d(TAG, "pdSwitching != null?" + (mPDSwitching != null));
        }
    }
    
    private void handleServiceSwitch(long simId) {
        if (mPhoneMgr.is3GSwitchLocked()) {
            Xlog.d(TAG, "Switch has been locked, return");
            return ;
        }
        Xlog.d(TAG, "handleServiceSwitch(" + simId + "), show switching dialog first");
        showSwitchProgress();
        setStatusBarEnableStatus(false);
        int slotId = -1;
        if (simId != -1) {
            SIMInfo info = SIMInfo.getSIMInfoById(this, simId);
            slotId = info == null ? -1 : info.mSlot;
        }
        if (mPhoneMgr.set3GCapabilitySIM(slotId)) {
            Xlog.d(TAG, "Receive ok for the switch, and starting the waiting...");
        } else {
            Xlog.d(TAG, "Receive error for the switch & Dismiss switching didalog");
            disSwitchProgressDialog();
            setStatusBarEnableStatus(true);
            //maybe: need update the ui if switch fail
            //this.updateSummarys(long simId);
            //this.updateNetworkMode();
        }
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
            Xlog.d(TAG, "[handleGetPreferredNetworkTypeResponse]");
            if (mPD != null && mPD.isShowing() 
                    && msg.arg2 == MESSAGE_SET_PREFERRED_NETWORK_TYPE) {
                mPD.dismiss();
            }
            
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        Phone.PREFERRED_NT_MODE);

                if (DBG) {
                    Xlog.d(TAG, "handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
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
                        Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) {
                            Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode);
                    }
                    if (modemNetworkMode == Phone.NT_MODE_GSM_UMTS) {
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        settingsNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                    }
                    mNetworkMode.setValue(Integer.toString(modemNetworkMode));
                    updatePreferredNetworkModeSummary(modemNetworkMode);
                    
                } else {
                    if (DBG) {
                        Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: else: reset to default");
                    }
                    resetNetworkModeToDefault();
                }
            } else {
                Xlog.i(TAG, "handleGetPreferredNetworkTypeResponse(), response exist exception");
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            Xlog.d(TAG, "-------------[handleSetPreferredNetworkTypeResponse]--------------");
            AsyncResult ar = (AsyncResult) msg.obj;
            
            int slot = mPhoneMgr.get3GCapabilitySIM();
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.getPreferredNetworkTypeGemini(
                        obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE, 1001, 
                        MESSAGE_SET_PREFERRED_NETWORK_TYPE, null), slot);
            } else {
                mPhone.getPreferredNetworkType(
                        obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE, 1001, 
                        MESSAGE_SET_PREFERRED_NETWORK_TYPE, null));
            }
        }

        private void resetNetworkModeToDefault() {
            //set the mNetworkMode
            mNetworkMode.setValue(Integer.toString(Phone.PREFERRED_NT_MODE));
            //set the Settings.System
            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        Phone.PREFERRED_NT_MODE);
            //Set the Modem
            int slot = mPhoneMgr.get3GCapabilitySIM();
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.setPreferredNetworkTypeGemini(Phone.PREFERRED_NT_MODE,
                            this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), slot);
            } else {
                mPhone.setPreferredNetworkType(Phone.PREFERRED_NT_MODE,
                        this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
    }
    
    private void updatePreferredNetworkModeSummary(int networkMode) {
        String value = mNetworkMode.getValue();
        switch(networkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                // TODO T: Make all of these strings come from res/values/strings.xml.
                mNetworkMode.setSummary(mNetworkMode.getEntry());
                //mNetworkMode.getEntry();
                break;
            case Phone.NT_MODE_GSM_ONLY:
                //mNetworkMode.setSummary("GSM only");
                mNetworkMode.setSummary(mNetworkMode.getEntry());
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                //mNetworkMode.setSummary("WCDMA only");
                mNetworkMode.setSummary(mNetworkMode.getEntry());
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mNetworkMode.setSummary("GSM/WCDMA");
                break;
            case Phone.NT_MODE_CDMA:
                mNetworkMode.setSummary("CDMA / EvDo");
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mNetworkMode.setSummary("CDMA only");
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mNetworkMode.setSummary("EvDo only");
                break;
            case Phone.NT_MODE_GLOBAL:
            default:
                mNetworkMode.setSummary("Global");
        }
    }
    
    private void showProgressDialog() {
        // TODO Auto-generated method stub
        mPD = new ProgressDialog(this);
        mPD.setMessage(getText(R.string.updating_settings));
        mPD.setCancelable(false);
        mPD.setIndeterminate(true);
        mPD.show();
    }
    
    private void updateItemStatus() {
        boolean enabled =  mPhoneMgr.is3GSwitchLocked();
        Xlog.d(TAG, "updateItemStatus(), is3GSwitchLocked()?" + enabled);
        if (this.mServiceList != null) {
            mServiceList.setEnabled(!enabled);
        }
        
        if (this.mNetworkMode != null) {
            //Two sim insert, set simA has 3G service, then power off and remove SimA
            //Power on, not switch 3G to SimB, return the slot which simA has been inserted
            //so check there is sim insert in the 3G service slot
            int cardSlot = mPhoneMgr.get3GCapabilitySIM();
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, cardSlot);
            mNetworkMode.setEnabled(!enabled && (info != null) && (CallSettings.isRadioOn(cardSlot)));
            if (!mNetworkMode.isEnabled()) {
                mNetworkMode.setSummary("");
            }
        }
    }
    
    protected void onDestroy() {
        super.onDestroy();
        Xlog.d(TAG, "Instance[" + mInstanceIndex + "]." + "onDestroy()");
        
        if (mPD != null && mPD.isShowing()) {
            mPD.dismiss();
        }
        if (mPD != null) {
            mPD = null;
        }
        disSwitchProgressDialog();
        
        if (mSlr != null) {
            unregisterReceiver(mSlr);
        }
        //restore status bar status after finish, to avoid unexpected event
        setStatusBarEnableStatus(true);
        mTimerHandler.removeMessages(SWITCH_3G_TIME_OUT_MSG);
    }
    
    /**
     * When switching modem, the status bar should be disabled
     * @param enabled
     */
    private void setStatusBarEnableStatus(boolean enabled) {
        Xlog.i(TAG, "setStatusBarEnableStatus(" + enabled + ")");
        if (mStatusBarManager == null) {
            mStatusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
        }
        if (mStatusBarManager != null) {
            if (enabled) {
                mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
            } else {
                mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND |
                                          StatusBarManager.DISABLE_RECENT |
                                          StatusBarManager.DISABLE_HOME);
            }
        } else {
            Xlog.e(TAG, "Fail to get status bar instance");
        }
    }
    
    class ModemSwitchReceiver extends BroadcastReceiver {
        
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED.equals(action)) {
                Xlog.d(TAG, "receives EVENT_3G_SWITCH_LOCK_CHANGED...");
                boolean bLocked = intent.getBooleanExtra(GeminiPhone.EXTRA_3G_SWITCH_LOCKED, false);
                updateItemStatus();
            } else if (GeminiPhone.EVENT_PRE_3G_SWITCH.equals(action)) {
                Xlog.d(TAG, "Starting the switch......@" + this);
                showSwitchProgress();
                showInstanceIndex("Receive starting switch broadcast");
                setStatusBarEnableStatus(false);
                mTimerHandler.sendEmptyMessageDelayed(SWITCH_3G_TIME_OUT_MSG, SWITCH_3G_TIME_OUT_VALUE);
                if (mNetworkMode.getDialog() != null) {
                    mNetworkMode.getDialog().dismiss();
                }
            } else if (GeminiPhone.EVENT_3G_SWITCH_DONE.equals(action)) {
                Xlog.d(TAG, "Done the switch......@" + this);
                showInstanceIndex("Receive switch done broadcast");
                clearAfterSwitch(intent);
                mTimerHandler.removeMessages(SWITCH_3G_TIME_OUT_MSG);
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)
                 || action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                boolean airplaneMode = intent.getBooleanExtra("state", false);
                Xlog.d(TAG, "airplaneMode new  state is [" + airplaneMode + "]");
                updateNetworkMode();
                update3GService();
                mServiceList.refreshList();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                Xlog.d(TAG, "ACTION_SIM_INFO_UPDATE received");
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(Modem3GCapabilitySwitch.this);
                if (temp.size() > 0) {
                    mServiceList.refreshList();
                    long simId = SIMID_3G_SERVICE_NOT_SET;
                    int slot = mPhoneMgr.get3GCapabilitySIM();
                    //update summary
                    if (slot == SIMID_3G_SERVICE_OFF) {
                        simId = slot;
                    } else {
                        SIMInfo info = SIMInfo.getSIMInfoBySlot(Modem3GCapabilitySwitch.this, slot);
                        simId = info != null ? info.mSimId : SIMID_3G_SERVICE_NOT_SET;
                    }
                    updateSummarys(simId);
                    updateNetworkMode();
                } else {
                    finish();
                } 
            }
        }
    }
    
    private void showInstanceIndex(String msg) {
        if (DBG) {
            Xlog.i(TAG, "Instance[" + mInstanceIndex + "]: " + msg);
        }
    }
}
