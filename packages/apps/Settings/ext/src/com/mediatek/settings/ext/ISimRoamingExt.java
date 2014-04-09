package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.Preference;

public interface ISimRoamingExt {
    
    /**
     * set the roaming warning msg
     * @param context
     * @param res the res to used
     * 
     */
    String getRoamingWarningMsg(Context context,int res);
    /**
     * Set the summary for the pref
     * @param pref preference to set
     * 
     */
    void setSummary(Preference pref);
    
    /**
     * show a waring dialog to customer if in roaming state
     * @param context
     */
    void showDialog(Context context);

	/**
	* show Toast for succeeding to enable or disable pin lock
	*/
	void showPinToast(boolean enable);
}
