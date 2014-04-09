package com.mediatek.oobe.ext;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
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
    public void registerAirplaneModeObserver(SwitchPreference tempSwitch) {
    }
    public void unRegisterAirplaneObserver() {
    }
    public boolean getSwitchState() {
        Xlog.d(TAG,"getSwitchState(), return true");
        return true;
    }
    public void initSwitchState(SwitchPreference tempSwitch) {
    }

    //wifi config controller
    public boolean shouldAddForgetButton(String ssid, int security) {
        return true;
    }

    public void setSecurityText(TextView view) {
    }
    public String getSecurityText(int security) {
        return mContext.getString(security);
    }

    public boolean shouldSetDisconnectButton() {
        return false;
    }

    public void setProxyText(TextView view) {
    }

    //access point
    public int getApOrder(String currentSsid, int currentSecurity, String otherSsid, int otherSecurity) {
        Xlog.d(TAG,"getApOrder(),return 0");
        return 0;
    }
}
