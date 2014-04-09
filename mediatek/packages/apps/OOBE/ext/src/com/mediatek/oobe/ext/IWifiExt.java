package com.mediatek.oobe.ext;
import android.app.Activity;
import android.content.ContentResolver;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

public interface IWifiExt {
    /**
     * Called when register AirplaneMode Observer for wifi enabler
     * @param context The parent context
     * @param switch_ The parent switch
     */
    void registerAirplaneModeObserver(SwitchPreference tempSwitch);
    /**
     * Called when unregister AirplaneMode Observer for wifi enabler
     */
    void unRegisterAirplaneObserver();
    /**
     * Get state of switch of wifi enabler 
     * @return whether the switch is enable or disable
     */
    boolean getSwitchState();
    /**
     * Called when initialize state of switch of wifi enabler 
     * @param switch_ The parent switch 
     */
    void initSwitchState(SwitchPreference tempSwitch);

    /**
     * whether add forget button for wifi dialog 
     * @return whether add forget button 
     */
    boolean shouldAddForgetButton(String ssid, int security);

    /**
     * Set security text label 
     * @param context The parent context
     * @param view The view which contains security label
     */
    void setSecurityText(TextView view);
    /**
     * Get security text label 
     */
    String getSecurityText(int security);

    /**
     * Whether add disconnect button for wifi dialog
     * @return whether add disconnect button 
     */
    boolean shouldSetDisconnectButton();

    /**
     * set proxy title
     */
    void setProxyText(TextView view);

    //access point
    /**
     * Order access points
     * @param currentSsid The ssid of current access point
     * @param currentSecurity The security of current access point
     * @param otherSsid The ssid of other access point
     * @param otherSecurity The security of other access point
     * @return whether current access point is in front of another
     */
    int getApOrder(String currentSsid, int currentSecurity, String otherSsid, int otherSecurity);
}
