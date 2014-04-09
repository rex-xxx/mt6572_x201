package com.android.phone;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.view.MenuItem;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.settings.CallBarring;
import com.mediatek.settings.CallSettings;
import com.mediatek.settings.MultipleSimActivity;
import com.mediatek.settings.PreCheckForRunning;
import com.mediatek.settings.VoiceMailSetting;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class CallFeaturesSetting extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    //Used for phone's other modules
    // Dtmf tone types
    static final int DTMF_TONE_TYPE_NORMAL = 0;
    static final int DTMF_TONE_TYPE_LONG   = 1;
    public static final String HAC_KEY = "HACSetting";
    public static final String HAC_VAL_ON = "ON";
    public static final String HAC_VAL_OFF = "OFF";
    
 // intent action to bring up voice mail settings
    public static final String ACTION_ADD_VOICEMAIL =
        "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL";
    // intent action sent by this activity to a voice mail provider
    // to trigger its configuration UI
    public static final String ACTION_CONFIGURE_VOICEMAIL =
        "com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL";
    // Extra put in the return from VM provider config containing voicemail number to set
    public static final String VM_NUMBER_EXTRA = "com.android.phone.VoicemailNumber";
    // Extra put in the return from VM provider config containing call forwarding number to set
    public static final String FWD_NUMBER_EXTRA = "com.android.phone.ForwardingNumber";
    // Extra put in the return from VM provider config containing call forwarding number to set
    public static final String FWD_NUMBER_TIME_EXTRA = "com.android.phone.ForwardingNumberTime";
    // If the VM provider returns non null value in this extra we will force the user to
    // choose another VM provider
    public static final String SIGNOUT_EXTRA = "com.android.phone.Signout";

    private static final String LOG_TAG = "Settings/CallFeaturesSetting";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    
    private static final String BUTTON_CALL_FWD_KEY    = "button_cf_expand_key";
    private static final String BUTTON_CALL_BAR_KEY    = "button_cb_expand_key";
    private static final String BUTTON_CALL_ADDITIONAL_KEY    = "button_more_expand_key";
    private static final String BUTTON_CALL_VOICEMAIL_KEY    = "button_voicemail_key";
    private static final String BUTTON_IP_PREFIX_KEY = "button_ip_prefix_key";
    
    private Preference mButtonVoiceMail;
    private Preference mButtonCallFwd;
    private Preference mButtonCallBar;
    private Preference mButtonCallAdditional;
    private Preference mButtonIpPrefix;
    
    private int mSimId = PhoneConstants.GEMINI_SIM_1;
    private PreCheckForRunning mPreCfr = null;
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Xlog.d(LOG_TAG, "[action = " + intent.getAction() + "]");
            setScreenEnabled();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.voice_call_settings);
        addPreferencesFromResource(R.xml.gsm_umts_call_options);
        
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCallAdditional = prefSet.findPreference(BUTTON_CALL_ADDITIONAL_KEY);
        mButtonCallFwd =  prefSet.findPreference(BUTTON_CALL_FWD_KEY);
        mButtonCallBar = prefSet.findPreference(BUTTON_CALL_BAR_KEY);
        mButtonVoiceMail = prefSet.findPreference(BUTTON_CALL_VOICEMAIL_KEY);
        mButtonIpPrefix = prefSet.findPreference(BUTTON_IP_PREFIX_KEY);
        
        if (!PhoneUtils.isSupportFeature("IP_DIAL")) {
            prefSet.removePreference(mButtonIpPrefix);
            mButtonIpPrefix = null;
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
    public void onResume() {
        super.onResume();
        setScreenEnabled();
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
            if (preference == mButtonVoiceMail) {
                Intent intent = new Intent(this, VoiceMailSetting.class);
                intent.setAction(Intent.ACTION_MAIN);
                mPreCfr.checkToRun(intent, mSimId, 302);
                return true;
            } else if (preference == mButtonCallFwd) {
                Intent intent = new Intent(this, GsmUmtsCallForwardOptions.class);
                intent.setAction(Intent.ACTION_MAIN);
                mPreCfr.checkToRun(intent, mSimId, 302);
                return true;
            } else if (preference == mButtonCallBar) {
                Intent intent = new Intent(this, CallBarring.class);
                intent.setAction(Intent.ACTION_MAIN);
                mPreCfr.checkToRun(intent, mSimId, 302);
                return true;
            } else if (preference == mButtonCallAdditional) {
                Intent intent = new Intent(this, GsmUmtsAdditionalCallOptions.class);
                intent.setAction(Intent.ACTION_MAIN);
                mPreCfr.checkToRun(intent, mSimId, 302);
                return true;
            }
            return false;
        }
        
        if (preference == mButtonVoiceMail) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.voice_mail);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.mediatek.settings.VoiceMailSetting");
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        } else if (preference == mButtonCallFwd) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.labelCF);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.android.phone.GsmUmtsCallForwardOptions");
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        } else if (preference == mButtonCallBar) {
            Xlog.d(LOG_TAG, "onPreferenceTreeClick , call barring key , simId= " + mSimId);
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.lable_call_barring);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.mediatek.settings.CallBarring");
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        } else if (preference == mButtonCallAdditional) {
            Xlog.d(LOG_TAG, "onPreferenceTreeClick , call cost key , simId= " + mSimId);
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.additional_gsm_call_settings);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.android.phone.GsmUmtsAdditionalCallOptions");
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        } else if (mButtonIpPrefix == preference) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.ip_prefix_setting);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.mediatek.settings.IpPrefixPreference");
            this.startActivity(intent);
            return true;
        }
        
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
    }
    
    protected void onDestroy() {
        super.onDestroy();
        if (mPreCfr != null) {
            mPreCfr.deRegister();
        }
        unregisterReceiver(mReceiver);
    }

    /**
     * Obtain the setting for "vibrate when ringing" setting.
     *
     * Watch out: if the setting is missing in the device, this will try obtaining the old
     * "vibrate on ring" setting from AudioManager, and save the previous setting to the new one.
     */
    public static boolean getVibrateWhenRinging(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return false;
        }

        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0;
    }

    private void setScreenEnabled() {
        boolean airplaneOn = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;

        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
        if (list.size() == 0) {
            finish();
        } else if (list.size() == 1) {
            mSimId = list.get(0).mSlot;
            boolean isRadioOn = CallSettings.isRadioOn(mSimId) && !airplaneOn;            
            mButtonCallAdditional.setEnabled(isRadioOn);
            mButtonCallFwd.setEnabled(isRadioOn);
            mButtonCallBar.setEnabled(isRadioOn);
            mButtonVoiceMail.setEnabled(isRadioOn);
            mButtonIpPrefix.setEnabled(isRadioOn);
            mPreCfr.mByPass = false;
        } else {
            mPreCfr.mByPass = true;
        }
    }
}
