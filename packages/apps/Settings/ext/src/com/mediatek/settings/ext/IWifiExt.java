package com.mediatek.settings.ext;
import android.app.Activity;
import android.content.ContentResolver;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

public interface IWifiExt {
    /**
     * Called when register AirplaneMode Observer for wifi enabler
     * @param context The parent context
     * @param switch_ The parent switch
     */
    void registerAirplaneModeObserver(Switch tempSwitch);
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
    void initSwitchState(Switch tempSwitch);
    /**
     * Get ssid for wifi access point enabler 
     * @param context The parent context 
     * @return ssid of wifi access point 
     */
    String getWifiApSsid();
    //wifi config controller
    /**
     * whether add forget button for wifi dialog 
     * @return whether add forget button 
     */
    boolean shouldAddForgetButton(String ssid, int security);
    /**
     * set network id for access point 
     * @param apNetworkId New network id
     */
    void setAPNetworkId(int apNetworkId);
    /**
     * Set priority for access point 
     * @param apPriority New priority
     */
    void setAPPriority(int apPriority);
    /**
     * Initiatlize priority UI 
     * @param context The parent context
     * @param view The view which need to be initiatlize
     */
    View getPriorityView();
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
     * get priority for current select access point
     */
    int getPriority();
    /**
     * close spinner dialog when rotate screen
     */
    void closeSpinnerDialog();
    /**
     * set proxy title
     */
    void setProxyText(TextView view);
    //advanced wifi settings
    /**
     * Initiatlize connect type in wifi advanced settings
     * @param screen The screen of wifi advanced settings
     * @param connectType Connect type is manual or auto
     * @param connectApType data connection change type from Cell to Wifi, manual or auto 
     * @param selectSsidType data connection change type among wifi access points
     * @param listner called when this preference has changed
     */
    void initConnectView(Activity activity,PreferenceScreen screen);
    /**
     * Initiatlize gatway & netmask info in wifi advanced settings
     * @param screen The screen of wifi advanced settings
     */
    void initNetworkInfoView(PreferenceScreen screen);
    /**
     * refresh gatway & netmask info in wifi advanced settings
     */
    void refreshNetworkInfoView();
    /**
     * Initiatlize preference for wifi advanced settings
     * @param contentResolver The parent content resolver
     */
    void initPreference(ContentResolver contentResolver);
    /**
     * Get sleep policy
     * @param contentResolver The parent content resolver
     * @return sleep policy
     */
    int getSleepPolicy(ContentResolver contentResolver);

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
