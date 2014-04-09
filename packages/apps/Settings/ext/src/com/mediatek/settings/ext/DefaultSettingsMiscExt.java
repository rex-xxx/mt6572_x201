package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;

/* Dummy implmentation , do nothing */
public class DefaultSettingsMiscExt implements ISettingsMiscExt {
    
    private static final String TAG = "DefaultSettingsMiscExt";

    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_SINGLE = 2;
 
    
    public boolean isAllowEditPresetApn(String type, String apn, String numeric) {
        return true;
    }

    public Preference getApnPref(PreferenceGroup apnList, int count, int[] array) {
        return apnList.getPreference(0);
    }
    
    public void removeTetherApnSettings(PreferenceScreen prefSc, Preference preference) {
        prefSc.removePreference(preference);
    }

    public boolean isWifiToggleCouldDisabled(Context context) {
        return true;
    }

    public String getTetherWifiSSID(Context ctx) {
        return ctx.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
    }

    public void setTimeoutPrefTitle(Preference pref) {
    }
    
   
    public String getDataUsageBackgroundStrByTag(String defStr, String tag) {
        return defStr;
    }

	
    public TabSpec DataUsageUpdateTabInfo(Activity activity, String tag, TabSpec tab, TabWidget tabWidget, String title) {
        return tab;
    }

    public void DataUsageUpdateSimText(int simColor, TextView title) {
        return;
    }
}

