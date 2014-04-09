package com.mediatek.browser.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.webkit.WebSettings;

import com.mediatek.xlog.Xlog;

public class BrowserSmallFeatureEx extends ContextWrapper implements IBrowserSmallFeatureEx {
    private static final String TAG = "BrowserPluginEx";
    
    public BrowserSmallFeatureEx(Context context) {
        super(context);
    }

    @Override
    public void updatePreferenceItem(Preference pref, String value) {
        Xlog.i(TAG, "Enter: " + "updatePreferenceItem" + " --default implement");
        return;
    }

    @Override
    public void updatePreferenceItemAndSetListener(Preference pref, String value,
            OnPreferenceChangeListener onPreferenceChangeListener) {
        //
        Xlog.i(TAG, "Enter: " + "updatePreferenceItemAndSetListener" + " --default implement");
        return;
    }

    @Override
    public void setTextEncodingChoices(ListPreference e) {
        //
        Xlog.i(TAG, "Enter: " + "setTextEncodingChoices" + " --default implement");
        return;
        
    }

    @Override
    public boolean shouldDownloadPreference() {
        //
        Xlog.i(TAG, "Enter: " + "shouldDownloadPreference" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldProcessResultForFileManager() {
        Xlog.i(TAG, "Enter: " + "shouldProcessResultForFileManager" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldCheckUrlLengthLimit() {
        //
        Xlog.i(TAG, "Enter: " + "shouldCheckUrlLengthLimit" + " --default implement");
        return false;
    }

    @Override
    public String checkAndTrimUrl(/*Context context, */String url) {
        Xlog.i(TAG, "Enter: " + "checkAndTrimUrl" + " --default implement");
        return url;
    }

    @Override
    public String getCustomerHomepage() {
        //
        Xlog.i(TAG, "Enter: " + "getCustomerHomepage" + " --default implement");
        return null;
    }

    @Override
    public String getSearchEngine(SharedPreferences mPrefs) {
        //
        Xlog.i(TAG, "Enter: " + "getSearchEngine" + " --default implement");
        return null;
    }

    @Override
    public void setStandardFontFamily(WebSettings settings, String fontFamily) {
        //
        Xlog.i(TAG, "Enter: " + "setStandardFontFamily" + " --default implement");
        return;
        
    }

    @Override
    public boolean setIntentSearchEngineExtra(Intent intent, String string) {
        //
        Xlog.i(TAG, "Enter: " + "setIntentSearchEngineExtra" + " --default implement");
        return false;
    }

    @Override
    public CharSequence[] getPredefinedWebsites() {
        Xlog.i(TAG, "Enter: " + "getPredefinedWebsites" + " --default implement");
        return null;
    }

    @Override
    public boolean shouldSetNavigationBarTitle() {
        //
        Xlog.i(TAG, "Enter: " + "shouldSetNavigationBarTitle" + " --default implement");
        return false;
    }
/*
    @Override
    public boolean shouldChangeSelectionString() {
        Xlog.i(TAG, "Enter: " + "shouldChangeSelectionString" + " --default implement");
        return false;
    }
*/
    @Override
    public int addDefaultBookmarksForCustomer(IBrowserProvider2Ex mBrowserProvider2, SQLiteDatabase db, long id,
            int position) {
        //
        Xlog.i(TAG, "Enter: " + "addDefaultBookmarksForCustomer" + " --default implement");
        return 0;
    }

    @Override
    public boolean shouldCreateHistoryPageOptionMenu(Menu menu, MenuInflater inflater) {
        //
        Xlog.i(TAG, "Enter: " + "createHistoryPageOptionMenu" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldConfigHistoryPageMenuItem(Menu menu, boolean isNull, boolean isEmpty) {
        //
        Xlog.i(TAG, "Enter: " + "configHistoryPageMenuItem" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldCreateBookmarksOptionMenu(Menu menu, MenuInflater inflater) {
        //
        Xlog.i(TAG, "Enter: " + "createBookmarksOptionMenu" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldChangeBookmarkMenuManner() {
        Xlog.i(TAG, "Enter: " + "shouldChangeBookmarkMenuManner" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldTransferToWapBrowser() {
        Xlog.i(TAG, "Enter: " + "shouldTransferToWapBrowser" + " --default implement");
        return false;
    }

    @Override
    public String getOperatorUA(String defaultUA) {
        Xlog.i(TAG, "Enter: " + "getOperatorUA" + " --default implement");
        return null;
    }

    @Override
    public boolean shouldOnlyLandscape(SharedPreferences mPrefs) {
        Xlog.i(TAG, "Enter: " + "shouldOnlyLandscape" + " --default implement");
        return false;
    }

    @Override
    public boolean shouldLoadCustomerAdvancedXml() {
        Xlog.i(TAG, "Enter: " + "shouldLoadCustomerAdvancedXml" + " --default implement");
        return false;
    }
    
    @Override
    public boolean shouldOverrideFocusContent() {
        Xlog.i(TAG, "Enter: " + "shouldOverrideFocusContent" + " --default implement");
        return false;
    }
}
