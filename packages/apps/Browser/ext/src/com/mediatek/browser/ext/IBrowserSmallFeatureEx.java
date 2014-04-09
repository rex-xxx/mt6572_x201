package com.mediatek.browser.ext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.webkit.WebSettings;

public interface IBrowserSmallFeatureEx {
    /**
     * OP01 Feature
     * Usage location: AccessibilityPreferencesFragment.java
     * update the preference which key is PREF_FONT_FAMILY.
     * @param pref                                the pref value to be set
     * @param fontFamily                          the pref value
     */
    void updatePreferenceItem(Preference pref, String fontFamily);

    /**
     * OP01 Feature
     * Usage location: AccessibilityPreferencesFragment.java
     * update the preference which key is PREF_FONT_FAMILY and set the onPreferenceChangeListener.
     * @param pref                                the pref value to be set
     * @param fontFamily                          the pref value
     * @param onPreferenceChangeListener          the listener
     */
    void updatePreferenceItemAndSetListener(Preference pref, String value,
            OnPreferenceChangeListener onPreferenceChangeListener);

    /**OP02 Feature
     * Usage location: AdvancedPreferencesFragment.java
     * set the TextEncodingChoices array and its values
     * @param e                                   the ListPreference to be set.
     */
    void setTextEncodingChoices(ListPreference e);

    /**
     * OP01 Feature
     * Usage location: AdvancedPreferencesFragment.java
     * Check whether need add download preference item.
     */
    boolean shouldDownloadPreference();

    /**
     * OP01 Feature
     * Usage location: AdvancedPreferencesFragment.java
     * Check whether need process the result which returned by APP FileManager.
     */
    boolean shouldProcessResultForFileManager();

    /**
     * OP01 Feature
     * Usage location: NavigationBarBase.java Tab.java
     * Check whether need process the Url length.
     */
    boolean shouldCheckUrlLengthLimit();

    /**
     * OP01 Feature
     * Usage location: Tab.java
     * Check whether need process the Url length and got the Url which has been trimed.
     * Return: the Url which has been trimed
     */
    String checkAndTrimUrl(/*Context context, */String url);

    /**
     * OP02 Feature
     * Usage location: BrowserSettings.java
     * Get the Customer HomePage
     * Return: the HomePage Url
     */
    String getCustomerHomepage();

    /**
     * OP01 Feature
     * Usage location: sitenavigation.java
     * Get the Customer predefined websites in site navigation page.
     * Return: the array resource of predefinedWebsites.
     */
    CharSequence[] getPredefinedWebsites();

    /**
     * OP01 Feature
     * Usage location: SearchEngineSettings.java BrowserSettings.java
     * Get the Customer defined the search engine.
     * Return: the search engine url.
     */
    String getSearchEngine(SharedPreferences mPrefs);

    /**
     * OP01 Feature
     * Usage location: BrowserSettings.java
     * set the StandardFontFamily.
     */
    void setStandardFontFamily(WebSettings settings, String fontFamily);

    /**
     * OP01 Feature
     * Usage location: SearchEngineSettings.java
     * set the search engine to the intent as extra.
     */
    boolean setIntentSearchEngineExtra(Intent intent, String string);

    /**
     * OP01 Feature
     * Usage location: BaseUi.java
     * check whether need set the title to the navigation bar.
     */
    boolean shouldSetNavigationBarTitle();

    /**
     * OP01 Feature
     * Usage location: BrowserProvider2.java
     * add the customer defined the bookmarks..
     * return the number of the bookmarks added.
     */
    int addDefaultBookmarksForCustomer(IBrowserProvider2Ex mBrowserProvider2,
            SQLiteDatabase db, long id, int position);

    /**
     * OP01 Feature
     * Usage location: BrowserHistoryPage.java
     * create the option menu of history page and the click events.
     */
    boolean shouldCreateHistoryPageOptionMenu(Menu menu, MenuInflater inflater);
    boolean shouldConfigHistoryPageMenuItem(Menu menu, boolean isNull, boolean isEmpty);

    /**
     * OP01 Feature
     * Usage location: BrowserBookmarksPage.java
     * create the option menu of bookmarks page and check whech need change the manner of the menu.
     */
    boolean shouldCreateBookmarksOptionMenu(Menu menu, MenuInflater inflater);
    boolean shouldChangeBookmarkMenuManner();

    /**
     * OP01 Feature
     * Usage location: IntentHandler.java
     * Check whether need transfer to wap browser to process wap page.
     */
    boolean shouldTransferToWapBrowser();

    /**
     * OP01/OP02 feature
     * get the Operator defined UA
     */
    String getOperatorUA(String defaultUA);

    /**
     * OP01 feature
     * Check whether only landscape launch
     */
    boolean shouldOnlyLandscape(SharedPreferences mPrefs);
    boolean shouldLoadCustomerAdvancedXml();
    
    boolean shouldOverrideFocusContent();
}