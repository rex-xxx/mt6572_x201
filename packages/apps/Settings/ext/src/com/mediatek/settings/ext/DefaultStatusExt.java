package com.mediatek.settings.ext;

import android.content.IntentFilter;
import android.preference.Preference;

public class DefaultStatusExt implements IStatusExt {
    /**
     * update the operator name
     * @param p
     * @param name
     */
    public void updateOpNameFromRec(Preference p, String name){
        
    }
    
    /**
     * update the summar
     * @param p
     * @param name
     */
    public void updateServiceState(Preference p, String name) {
        p.setSummary(name);
    }
    
    /**
     * add the intent to the intentfilter
     * @param intent
     * @param action
     */
    public void addAction(IntentFilter intent, String action) {
        
    }
}
