package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

public interface ISettingsMiscExt {

    /**
     * 
     * @param type
     * @param apn name to query
     * @param numeric
     * @return if the specified apn could be edited.
     */
     boolean isAllowEditPresetApn(String type, String apn, String numeric);
    
    /**
     * 
     * @param apnList list
     * @param count apn count
     * @param array
     * @return default apn.
     */
     Preference getApnPref(PreferenceGroup apnList, int count, int[] array);

    /**
     * remove tethering apn setting in not orange load.
     * @param prefSc
     * @param preference
     */
    void removeTetherApnSettings(PreferenceScreen prefSc, Preference preference);

    /**
     * 
     * @param ctx
     * @return if wifi toggle button could be disabled. 
     */
    boolean isWifiToggleCouldDisabled(Context ctx); 

    /**
     * 
     * @param ctx
     * @return tether wifi string.
     */
    String getTetherWifiSSID(Context ctx);
    
    /**
     * set screen timeout preference title
     * @param pref the screen timeout preference
     */
    void setTimeoutPrefTitle(Preference pref);
    
    
    /**
     * data usage background data restrict summary
     * @param: default string.
     * @param: tag string.
     * @return summary string.
     */
    String getDataUsageBackgroundStrByTag(String defStr, String tag);


    /**
     * get the upated tabspec
     * @param fragment activity
     * @param tab tag info
     * @param tab widget as the parent
     * @param tab title string
     * @return updated tabspec
     */
    TabSpec DataUsageUpdateTabInfo(Activity activity, String tag, TabSpec tab, TabWidget tabWidget, String title);

    /**
     * get the sim text with sim indicator
     * @param origin textview
     * @return void
     */
    void DataUsageUpdateSimText(int simColor, TextView title);

}
