package com.mediatek.settings.ext;


import android.content.Context;
import android.preference.Preference; 


public class DefaultSimRoamingExt implements ISimRoamingExt {
    
    /**
     * get the text of warning msg
     * @param context 
     * @param res
     * @return the warnning message for dialog
     */
    public String getRoamingWarningMsg(Context context,int res) {
        return context.getString(res);
    }
    /**
     * Set the summary for the pref for default not to show
     * @param pref
     */
    public void setSummary(Preference pref) {
    }
    
    /**
     * show a dialog to user as default not to show anything
     * @param context
     */
    public void showDialog(Context context) {
        
    }

	/**
	* show Toast for succeeding to enable or disable pin lock
	*/
	public void showPinToast(boolean enable) {
        
	}
}
