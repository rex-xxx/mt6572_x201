package com.android.videoeditor.util;

public class MtkFeatureSwitcher {
    private static final String TAG = "MtkFeatureSwitcher";
    private static final boolean LOG = true;

    public MtkFeatureSwitcher() {
    }
    
    //enable cancel audio, transition and kenburns.
    public static boolean isCancelGeneratingEnabled() {
        boolean enable = true;
        if (LOG) MtkLog.v(TAG, "isCancelGeneratingEnabled() return " + enable);
        return enable;
    }
}
