package com.mediatek.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.view.MenuItem;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.OthersSettingsExtension;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class OthersSettings extends PreferenceActivity 
        implements Preference.OnPreferenceChangeListener {
    private static final String BUTTON_OTHERS_FDN_KEY     = "button_fdn_key";
    private static final String BUTTON_OTHERS_MINUTE_REMINDER_KEY    = "minute_reminder_key";
    private static final String BUTTON_OTHERS_DUAL_MIC_KEY = "dual_mic_key";
    private static final String BUTTON_TTY_KEY    = "button_tty_mode_key";
    private static final String BUTTON_INTER_KEY    = "international_dialing_key";
    
    private static final String LOG_TAG = "Settings/OthersSettings";
    private Preference mButtonFdn;
    private CheckBoxPreference mButtonMr;
    private CheckBoxPreference mButtonDualMic;
    private ListPreference mButtonTTY;
    private CheckBoxPreference mButtonInter;
    
    private static final int DEFAULT_INTER_DIALING_VALUE = 0;
    private static final int INTER_DIALING_ON = 1;
    private static final int INTER_DIALING_OFF = 0;
    
    private int mSimId = 0;
    PreCheckForRunning mPreCfr = null;
    
    private OthersSettingsExtension mExtension;
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d(LOG_TAG, "[action = " + action + "]");
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                setScreenEnabled();
            } else if (TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED.equals(action)) {
                setScreenEnabled();
            } else if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                setScreenEnabled();
            }
        }
    };
     
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.others_settings);
        
        mExtension = ExtensionManager.getInstance().getOthersSettingsExtension();

        mButtonFdn = findPreference(BUTTON_OTHERS_FDN_KEY);
        mButtonMr = (CheckBoxPreference)findPreference(BUTTON_OTHERS_MINUTE_REMINDER_KEY);
        mButtonDualMic = (CheckBoxPreference)findPreference(BUTTON_OTHERS_DUAL_MIC_KEY);
        mButtonInter = (CheckBoxPreference)findPreference(BUTTON_INTER_KEY);
        if (!PhoneUtils.isSupportFeature("DUAL_MIC")) {
            this.getPreferenceScreen().removePreference(mButtonDualMic);
        }
        mExtension.customizeCallRejectFeature(getPreferenceScreen(), findPreference("call_reject"));
        
        if (mButtonMr != null) {
            mButtonMr.setOnPreferenceChangeListener(this);
        }
        
        if (mButtonDualMic != null) {
            mButtonDualMic.setOnPreferenceChangeListener(this);
        }
        mButtonTTY = (ListPreference) findPreference(BUTTON_TTY_KEY);

        if (mButtonTTY != null) {
            if (PhoneUtils.isSupportFeature("TTY")) {
                mButtonTTY.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mButtonTTY);
                mButtonTTY = null;
            }
        }
        if (mButtonInter != null) {
            mButtonInter.setOnPreferenceChangeListener(this);
            int checkedStatus = Settings.System.getInt(getContentResolver(), 
                    Settings.System.INTER_DIAL_SETTING, DEFAULT_INTER_DIALING_VALUE); 
            mButtonInter.setChecked(checkedStatus != 0);
            Xlog.d(LOG_TAG, "onResume isChecked in DB:" + (checkedStatus != 0));
        }

        mPreCfr = new PreCheckForRunning(this);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        registerReceiver(mReceiver, intentFilter);
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!CallSettings.isMultipleSim()) {
            if (preference == mButtonFdn) {
                Intent intent = new Intent(this, FdnSetting2.class);
                mPreCfr.checkToRun(intent, mSimId, 302);
                return true;
            } else if (preference == mButtonTTY) {
                return true;
            }
            return false;
        }
        
        if (preference == mButtonFdn) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.fdn);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.mediatek.settings.FdnSetting2");
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        }
        
        return false;
    }
    
     public boolean onPreferenceChange(Preference preference, Object objValue) {
         if (preference == mButtonMr) {
             if (mButtonMr.isChecked()) {
                 Xlog.d("OthersSettings", "onPreferenceChange mButtonReminder turn on"); 
                 mButtonMr.setSummary(getString(R.string.minutereminder_turnon));
             } else {
                 Xlog.d("OthersSettings", "onPreferenceChange mButtonReminder turn off"); 
                 mButtonMr.setSummary(getString(R.string.minutereminder_turnoff));
             }
         } else if (preference == mButtonDualMic) {
             if (mButtonDualMic.isChecked()) {
                 Xlog.d(LOG_TAG, "onPreferenceChange mButtonDualmic turn on"); 
                 PhoneUtils.setDualMicMode("0");
             } else {
                 Xlog.d(LOG_TAG, "onPreferenceChange mButtonDualmic turn off"); 
                 PhoneUtils.setDualMicMode("1");
             }
         } else if (preference == mButtonTTY) {
             handleTTYChange(preference, objValue);
         } else if (preference == mButtonInter) {
             if ((Boolean)objValue) {
                 Settings.System.putInt(getContentResolver(), Settings.System.INTER_DIAL_SETTING, INTER_DIALING_ON); 
             } else {
                 Settings.System.putInt(getContentResolver(), Settings.System.INTER_DIAL_SETTING, INTER_DIALING_OFF); 
             }
             Xlog.d(LOG_TAG, "onPreferenceChange mButtonInter turn :" 
                    + Settings.System.getInt(getContentResolver(), Settings.System.INTER_DIAL_SETTING, -1));
         } 
         return true;
     }
     
    public void onResume() {
        super.onResume();
        setScreenEnabled();

        if (mButtonTTY != null) {
            int settingsTtyMode = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.PREFERRED_TTY_MODE,
                    Phone.TTY_MODE_OFF);
            mButtonTTY.setValue(Integer.toString(settingsTtyMode));
            updatePreferredTtyModeSummary(settingsTtyMode);
        }
    }
     
    protected void onDestroy() {
        super.onDestroy();
        if (mPreCfr != null) {
            mPreCfr.deRegister();
        }
        unregisterReceiver(mReceiver);
    }

    public static void goUpToTopLevelSetting(Activity activity) {
        Intent intent = new Intent(activity, OthersSettings.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
    
    private void handleTTYChange(Preference preference, Object objValue) {
        int buttonTtyMode;
        buttonTtyMode = Integer.valueOf((String) objValue).intValue();
        int settingsTtyMode = android.provider.Settings.Secure.getInt(
                getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_TTY_MODE, Phone.TTY_MODE_OFF);
        Xlog.d(LOG_TAG, "handleTTYChange: requesting set TTY mode enable (TTY) to" +
                Integer.toString(buttonTtyMode));

        if (buttonTtyMode != settingsTtyMode) {
            switch(buttonTtyMode) {
            case Phone.TTY_MODE_OFF:
            case Phone.TTY_MODE_FULL:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
                android.provider.Settings.Secure.putInt(getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_TTY_MODE, buttonTtyMode);
                break;
            default:
                buttonTtyMode = Phone.TTY_MODE_OFF;
            }

            mButtonTTY.setValue(Integer.toString(buttonTtyMode));
            updatePreferredTtyModeSummary(buttonTtyMode);
            Intent ttyModeChanged = new Intent(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
            ttyModeChanged.putExtra(TtyIntent.TTY_PREFFERED_MODE, buttonTtyMode);
            sendBroadcast(ttyModeChanged);
        }
    }
    
    private void updatePreferredTtyModeSummary(int ttyMode) {
        String [] txts = getResources().getStringArray(R.array.tty_mode_entries);
        switch(ttyMode) {
            case Phone.TTY_MODE_OFF:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
            case Phone.TTY_MODE_FULL:
                mButtonTTY.setSummary(txts[ttyMode]);
                break;
            default:
                mButtonTTY.setEnabled(false);
                mButtonTTY.setSummary(txts[Phone.TTY_MODE_OFF]);
        }
    }

    private void setScreenEnabled() {
        boolean airplaneModeOn = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;

        List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
        if (insertSim.size() == 0) {
            mButtonFdn.setEnabled(false);
        } else if (insertSim.size() == 1) {
            mPreCfr.mByPass = false;
            mSimId = insertSim.get(0).mSlot;
            boolean isRadioOn = CallSettings.isRadioOn(mSimId);            
            mButtonFdn.setEnabled(isRadioOn && !airplaneModeOn);
        } else {
            mPreCfr.mByPass = true;
        }
    }
}
