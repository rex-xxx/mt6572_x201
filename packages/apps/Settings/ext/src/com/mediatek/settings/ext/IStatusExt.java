package com.mediatek.settings.ext;

import android.content.IntentFilter;
import android.preference.Preference;



public interface IStatusExt {
    /**
     * Update the summpary of Preference from receiver
     * @param p operator_name with the key
     * @param name the operator name
     */
    void updateOpNameFromRec(Preference p, String name);
    /**
     * Update the summpary of Preference with the key "operator_name" 
     * @param p operator_name with the key
     * @param name the operator name
     */
    void updateServiceState(Preference p, String name);
    /**
     * add action for Intentfilter
     * @param intent the intent need to add action
     * @param action the action to add
     */
    void addAction(IntentFilter intent, String action);
}
