package com.android.settings.deviceinfo;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.common.featureoption.FeatureOption;

public class SoftwareUpdates extends PreferenceActivity {

    private static final String TAG = "SoftwareUpdates";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String KEY_DMSW_UPDATE = "software_update";
    private static final String KEY_SCOMO = "scomo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.device_info_software_updates);
        PreferenceGroup parentPreference = getPreferenceScreen();
        Utils.updatePreferenceToSpecificActivityOrRemove(this,
                parentPreference, KEY_SYSTEM_UPDATE_SETTINGS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        if (!FeatureOption.MTK_FOTA_ENTRY) {
            Log.i(TAG, "FeatureOption.MTK_FOTA_ENTRY="
                    + FeatureOption.MTK_FOTA_ENTRY + " removed!");
            parentPreference.removePreference(findPreference(KEY_DMSW_UPDATE));
        }
        if (!FeatureOption.MTK_SCOMO_ENTRY) {
            Log.i(TAG, "FeatureOption.MTK_SCOMO_ENTRY="
                    + FeatureOption.MTK_SCOMO_ENTRY + " removed!");
            parentPreference.removePreference(findPreference(KEY_SCOMO));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference.getKey().equals(KEY_DMSW_UPDATE)) {
            Intent i = new Intent();
            i.setAction("com.mediatek.DMSWUPDATE");
            sendBroadcast(i);
            Log.i(TAG, KEY_DMSW_UPDATE + " pressed with intent "
                    + i.getAction());
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
