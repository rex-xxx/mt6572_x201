package com.mediatek.settings.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.mediatek.xlog.Xlog;

public class DefaultStatusGeminiExt implements IStatusGeminiExt {
    private static final String TAG = "DefaultStatusGeminiExt";

    /**
     * init the ui
     * @param screen
     * @param preference
     */
    public void initUI(PreferenceScreen screen, Preference preference) {
        Xlog.d(TAG,"default launched");
    }
}
