package com.mediatek.settings.ext;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.mediatek.xlog.Xlog;

public class DefaultWifiExt implements IWifiExt {
    private static final String TAG = "DefaultWifiExt";
    private Context mContext;

    public DefaultWifiExt(Context context) {
        mContext = context;
        Xlog.d(TAG,"DefaultWifiExt");
    }
    //wifi enabler
    public void registerAirplaneModeObserver(Switch tempSwitch) {
    }
    public void unRegisterAirplaneObserver() {
    }
    public boolean getSwitchState() {
        Xlog.d(TAG,"getSwitchState(), return true");
        return true;
    }
    public void initSwitchState(Switch tempSwitch) {
    }
    //wifi access point enabler
    public String getWifiApSsid() {
        return mContext.getString(
            com.android.internal.R.string.wifi_tether_configure_ssid_default);
    }
    //wifi config controller
    public boolean shouldAddForgetButton(String ssid, int security) {
        return true;
    }
    public void setAPNetworkId(int apNetworkId) {
    }
    public void setAPPriority(int apPriority) {
    }
    public View getPriorityView() {
        //view.findViewById(priorityId).setVisibility(View.GONE);
        return null;
    }
    public void setSecurityText(TextView view) {
    }
    public String getSecurityText(int security) {
        return mContext.getString(security);
    }

    public boolean shouldSetDisconnectButton() {
        return false;
    }
    public int getPriority() {
        return -1;
    }
    public void closeSpinnerDialog() {
    }
    public void setProxyText(TextView view) {
    }
//advanced wifi settings
    public void initConnectView(Activity activity,PreferenceScreen screen) {
    }

    public void initNetworkInfoView(PreferenceScreen screen) {
    }
    public void refreshNetworkInfoView() {
    }
    public void initPreference(ContentResolver contentResolver) {
    }
    public int getSleepPolicy(ContentResolver contentResolver) {
        return Global.getInt(contentResolver, Global.WIFI_SLEEP_POLICY, Global.WIFI_SLEEP_POLICY_NEVER);
    }

    //access point
    public int getApOrder(String currentSsid, int currentSecurity, String otherSsid, int otherSecurity) {
        Xlog.d(TAG,"getApOrder(),return 0");
        return 0;
    }
}
