package com.mediatek.oobe.ext;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public interface IWifiSettingsExt {
    /**
     * Whether add forget menu item for the access point 
     * @param ssid SSID of the access point
     * @param ssid security of the access point
     */
    boolean shouldAddForgetMenu(String ssid, int security);

    /**
     * Whether add disconnect menu for the current selected access point
     */
    boolean shouldAddDisconnectMenu();
    /**
     * Whether access point catogory exist
     */
    boolean isCatogoryExist();
    /**
     * Initialize catogory for access points
     */
    void setCategory(PreferenceCategory trustPref, PreferenceCategory configedPref, 
            PreferenceCategory newPref);
    /**
     * Remove all prefereces in every catogory
     * @param screen The parent screen
     */
    void emptyCategory(PreferenceScreen screen);
    /**
     * Remove all prefereces in the screen
     * @param screen The parent screen
     */
    void emptyScreen(PreferenceScreen screen);
    /**
     * Whether the access point is tructed
     * @param ssid ssid of the access point
     * @param security security of the access point
     */
    boolean isTustAP(String ssid, int security);
    /**
     * Refresh the category
     * @param screen The parent screen
     */
    void refreshCategory(PreferenceScreen screen);
    /**
     * get count of access points in the screen
     * @param screen The parent screen
     */
    int getAccessPointsCount(PreferenceScreen screen);

}
