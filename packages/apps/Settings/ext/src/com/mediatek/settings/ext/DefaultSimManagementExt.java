package com.mediatek.settings.ext;

import android.content.Intent;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.DialogPreference;

import com.mediatek.xlog.Xlog;

public class DefaultSimManagementExt implements ISimManagementExt {
    private static final String TAG = "DefaultSimManagementExt";
    private static final String KEY_3G_SERVICE_SETTING = "3g_service_settings";
    private static final String KEY_SIM_STATUS = "status_info";
    /**
     * update the preference screen of sim management
     * @param parent parent preference
     */
    public void updateSimManagementPref(PreferenceGroup parent) {
        Xlog.d(TAG,"updateSimManagementPref()");
        PreferenceScreen pref3GService = null;
        PreferenceScreen prefWapPush = null;
        PreferenceScreen prefStatus = null;
        if (parent != null) {
            pref3GService = (PreferenceScreen)parent.findPreference(KEY_3G_SERVICE_SETTING);
            prefStatus = (PreferenceScreen)parent.findPreference(KEY_SIM_STATUS);
        }
        if (pref3GService != null) {
            Xlog.d(TAG,"updateSimManagementPref()---remove pref3GService");
            parent.removePreference(pref3GService);
        }
        if (prefStatus != null) {
            Xlog.d(TAG,"updateSimManagementPref()---remove prefStatus");
            parent.removePreference(prefStatus);
        }
    }

    public void updateSimEditorPref(PreferenceFragment pref) {
        return;
    }
    public void dealWithDataConnChanged(Intent intent, boolean isResumed) {
        return;
    }
    
    public void updateDefaultSIMSummary(DialogPreference pref, Long simId) {
        return;
    }

    public void setPrefProperty(DialogPreference pref, long simID) {
        return;
    }

    public String getAutoString() {
        return null;
    }

    public boolean isNeedsetAutoItem() {
        return false;
    }
    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
        Xlog.d(TAG, "showChangeDataConnDialog");
        
        return;
    }
    
    public void setToClosedSimSlot(int simSlot) {
        return;
    }
}
