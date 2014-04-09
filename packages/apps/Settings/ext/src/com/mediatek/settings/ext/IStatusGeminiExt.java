package com.mediatek.settings.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;
public interface IStatusGeminiExt {
    /**
     * Initialize the preference for status
     * @param screen The screen of device info
     * @param preference The status preference 
     */
    void initUI(PreferenceScreen screen, Preference preference);

}
