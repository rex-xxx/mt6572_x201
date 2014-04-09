package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import com.mediatek.xlog.Xlog;
/* Dummy implmentation , do nothing */
public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";


    public boolean shouldAddForgetMenu(String ssid, int security) {
        Xlog.d(TAG,"WifiSettingsExt, shouldAddMenuForget(),return true");
        return true;
    }
    public void registerPriorityObserver(ContentResolver contentResolver, Context context) {
    }
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
    }
    public void setLastConnectedConfig(WifiConfiguration config) {
    }
    public void setLastPriority(int priority) {
    }
    public void updatePriority() {
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
    public void adjustPriority() {
    }
    public void recordPriority(int selectPriority) { 
    }
    public void setNewPriority(WifiConfiguration config) {
    }
    public void updatePriorityAfterSubmit(WifiConfiguration config) {
    }
    public void disconnect(int networkId) {
    }
    public void updatePriorityAfterConnect(int networkId) {
    }
}
