package com.mediatek.oobe.ext;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import com.mediatek.xlog.Xlog;
/* Dummy implmentation , do nothing */
public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";
    public DefaultWifiSettingsExt(Context context) {
        Xlog.d(TAG,"DefaultWifiSettingsExt");
    }

    public boolean shouldAddForgetMenu(String ssid, int security) {
        Xlog.d(TAG,"WifiSettingsExt, shouldAddMenuForget(),return true");
        return true;
    }

    public boolean shouldAddDisconnectMenu() {
        return false;
    }
    public boolean isCatogoryExist() {
        return false;
    }
    public void setCategory(PreferenceCategory trustPref, PreferenceCategory configedPref, 
            PreferenceCategory newPref) {
    }
    public void emptyCategory(PreferenceScreen screen) {
        screen.removeAll();
    }
    public void emptyScreen(PreferenceScreen screen) {
        screen.removeAll();
    }
    public boolean isTustAP(String ssid, int security) {
        return false;
    }
    public void refreshCategory(PreferenceScreen screen) {
    }
    public int getAccessPointsCount(PreferenceScreen screen) {
        return screen.getPreferenceCount();
    }
}
