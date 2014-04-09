package com.mediatek.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.view.MenuItem;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.MobileNetworkSettings;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CallSettings extends PreferenceActivity {
    private static final String LOG_TAG = "Settings/CallSettings";
    public static final int MODEM_3G = 0x03;
    private static final String MD_CAPABILITY_SLOT_1 = "gsm.baseband.capability";
    private static final String MD_CAPABILITY_SLOT_2 = "gsm.baseband.capability2";
    private static final String MD_CAPABILITY_SLOT_3 = "gsm.baseband.capability3";
    private static final String MD_CAPABILITY_SLOT_4 = "gsm.baseband.capability4";

    Preference mVTSetting = null;
    Preference mVoiceSetting = null;
    Preference mSipCallSetting = null;
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d(LOG_TAG, "[action = " + action + "]");
            if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                setScreenEnabled();
            }
        }
    };
     
    public static class SIMInfoComparable implements Comparator<SIMInfo> {
        @Override
        public int compare(SIMInfo sim1, SIMInfo sim2) {
            return sim1.mSlot - sim2.mSlot;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.call_feature_setting);
        mVTSetting = this.findPreference("button_vedio_call_key");
        mVoiceSetting = this.findPreference("button_voice_call_key");
        
        boolean voipSupported = PhoneUtils.isVoipSupported();
        if (!voipSupported || PhoneFeatureConstants.FeatureOption.MTK_CTA_SUPPORT) {
            this.getPreferenceScreen().removePreference(findPreference("button_internet_call_key"));
        }

        //If this video telephony feature is not supported, remove the setting
        if (!FeatureOption.MTK_VT3G324M_SUPPORT) {
            getPreferenceScreen().removePreference(mVTSetting);
            mVTSetting = null;
        }

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE); 
        registerReceiver(mReceiver, intentFilter);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVTSetting) {
            Intent intent = new Intent();
            if (isOnlyVt()) {
                intent.setClass(this, VTAdvancedSetting.class);
            } else {
                intent.setClass(this, VTAdvancedSettingEx.class);
            }
            startActivity(intent);
            return true;
        }   
        return false;
    }
     
    @Override
    public void onResume() {
        super.onResume();
        setScreenEnabled();
    }
     
    public static boolean isMultipleSim() {
        return FeatureOption.MTK_GEMINI_SUPPORT;
    }
     
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public static boolean isRadioOn(int slot) {
        boolean isRadioOn = false;
        Phone phone = PhoneGlobals.getPhone();
        if (CallSettings.isMultipleSim() && phone instanceof GeminiPhone) {
            GeminiPhone dualPhone = (GeminiPhone) phone;
            isRadioOn = dualPhone.isRadioOnGemini(slot);
        } else {
            isRadioOn = phone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
        }
         
        return isRadioOn;
    }

    public static void goUpToTopLevelSetting(Activity activity) {
        Intent intent = new Intent(activity, CallSettings.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void goToOthersSettings(Activity activity) {
        Intent intent = new Intent(activity, OthersSettings.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static long[] get3GSimCards(Activity activity) {
        List<Long> simIds = new ArrayList<Long>(); 
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            int slot = PhoneGlobals.getInstance().phoneMgr.get3GCapabilitySIM();
            if (slot >= 0) {
                simIds.add(SIMInfo.getSIMInfoBySlot(activity, slot).mSimId);
            }
        } else {
            List<SIMInfo> siminfoList = SIMInfo.getInsertedSIMList(activity);
            for (SIMInfo simInfo : siminfoList) {
                int baseband = getBaseband(simInfo.mSlot);
                if (baseband > MODEM_3G) {
                    simIds.add(simInfo.mSimId);
                }
            }
        }
        long[] md3GIds  = new long[simIds.size()];
        for (int i = 0; i < simIds.size(); i++) {
            md3GIds[i] = simIds.get(i);
        }
        return md3GIds;
    }

    public static int[] get3GSimCardSlots(Activity activity) {
        List<Integer> simIds = new ArrayList<Integer>(); 
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            int slot = PhoneGlobals.getInstance().phoneMgr.get3GCapabilitySIM();
            if (slot >= 0) {
                simIds.add(slot);
            }
        } else {
            List<SIMInfo> siminfoList = SIMInfo.getInsertedSIMList(activity);
            for (SIMInfo simInfo : siminfoList) {
                int baseband = getBaseband(simInfo.mSlot);
                if (baseband > MODEM_3G) {
                    simIds.add(simInfo.mSlot);
                }
            }
        }
        int[] md3GIds = new int[simIds.size()];
        for (int i = 0; i < simIds.size(); i++) {
            md3GIds[i] = simIds.get(i);
        }
        return md3GIds;
    }

    public static int getBaseband(int slot) {
        String propertyKey = null;
        switch (slot) {
            case PhoneConstants.GEMINI_SIM_1:
                propertyKey = MD_CAPABILITY_SLOT_1;
                break;
            case PhoneConstants.GEMINI_SIM_2:
                propertyKey = MD_CAPABILITY_SLOT_2;
                break;
            case PhoneConstants.GEMINI_SIM_3:
                propertyKey = MD_CAPABILITY_SLOT_3;
                break;
            case PhoneConstants.GEMINI_SIM_4:
                propertyKey = MD_CAPABILITY_SLOT_4;
                break;
            default:
                break;
        }
        int baseband = 0;
        try {
            String capability = SystemProperties.get(propertyKey);
            if (capability != null) {
                baseband = Integer.parseInt(capability);
            }
        } catch (NumberFormatException e) {
            Xlog.i(LOG_TAG, "get base band error");
        }
        Xlog.i(LOG_TAG, "[slot = " + slot + "]");
        Xlog.i(LOG_TAG, "[propertyKey = " + propertyKey + "]");
        Xlog.i(LOG_TAG, "[baseband = " + baseband + "]");
        return baseband;
    }

    public static void goToMobileNetworkSettings(Activity activity) {
        Intent intent = new Intent(activity, MobileNetworkSettings.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    private void setScreenEnabled() {
        List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
        if (CallSettings.isMultipleSim()) {
            int[] simIds = CallSettings.get3GSimCardSlots(this);
            if (mVTSetting != null)  {
                mVTSetting.setEnabled(insertSim.size() > 0 && simIds.length > 0);
            }
            mVoiceSetting.setEnabled(insertSim.size() > 0);
         } else {
            boolean hasSimCard = TelephonyManager.getDefault().hasIccCard();
            if (mVTSetting != null)  {
                mVTSetting.setEnabled(hasSimCard);
            }
            mVoiceSetting.setEnabled(hasSimCard);
        }
    }

    private boolean isOnlyVt() {
        List<SIMInfo> siminfoList = SIMInfo.getInsertedSIMList(this);
        return siminfoList.size() == 1 && 
                CallSettings.getBaseband(siminfoList.get(0).mSlot) > MODEM_3G;
    }
}
