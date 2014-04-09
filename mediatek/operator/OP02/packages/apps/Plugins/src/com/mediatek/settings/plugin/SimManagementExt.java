package com.mediatek.settings.plugin;

import android.content.Intent;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.mediatek.settings.ext.DefaultSimManagementExt;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;


public class SimManagementExt extends DefaultSimManagementExt {
    private static final String TAG = "SimManagementExt";
    private static final String KEY_3G_SERVICE_SETTING = "3g_service_settings";
    public void updateSimManagementPref(PreferenceGroup parent) {
        Xlog.d(TAG,"SimManagementExt---updateSimManagementPref()");
        PreferenceScreen pref3GService = null;
        if (parent != null) {
            pref3GService = (PreferenceScreen)parent.findPreference(KEY_3G_SERVICE_SETTING);
        }
        if (pref3GService != null) {
            Xlog.d(TAG,"FeatureOption.MTK_GEMINI_3G_SWITCH="+FeatureOption.MTK_GEMINI_3G_SWITCH);
            if (!FeatureOption.MTK_GEMINI_3G_SWITCH) {
                parent.removePreference(pref3GService);
            }
        }
    }
    public void updateSimEditorPref(PreferenceFragment pref) {
        return;
    }
    public void dealWithDataConnChanged(Intent intent, boolean isResumed) {
        return;
    }
    
    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
        Xlog.d(TAG, "showChangeDataConnDialog");
        
        return;
    }
    
    public void setToClosedSimSlot(int simSlot) {
        return;
    }
}
