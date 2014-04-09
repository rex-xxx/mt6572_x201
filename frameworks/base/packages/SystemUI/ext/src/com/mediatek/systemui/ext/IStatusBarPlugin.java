package com.mediatek.systemui.ext;

import android.content.res.Resources;

/**
 * M: the interface for Plug-in definition of Status bar.
 */
public interface IStatusBarPlugin {

    /** Signal strength interfaces. @{ */

    /**
     * Get the icon of signal strength according to these states.
     * 
     * @param roaming Whether at roaming state.
     * @param inetCondition The network condition state 0/1.
     * @param level The signal level value.
     * @param whether show SIM indicator or not.
     * @return the icon resource id of signal strength.
     */
    int getSignalStrengthIcon(boolean roaming, int inetCondition, int level, boolean showSimIndicator);

    /**
     * Get the description string of signal strength according to the level.
     * 
     * @param level The signal level value.
     * @return The description string of signal strength.
     */
    String getSignalStrengthDescription(int level);

    /**
     * Get the signal strength icon according to these state.
     * 
     * @param simColorId The SIM color index of GEMINI.
     * @param level The signal level value.
     * @param whether show SIM indicator or not.
     * @return The signal strength icon.
     */
    int getSignalStrengthIconGemini(int simColorId, int level, boolean showSimIndicator);

    /**
     * Get the signal strength icon according to these state.
     * 
     * @param simColorId The SIM color index of GEMINI.
     * @param type The type of signal, 0 - top part; 1 - bottom part.
     * @param level The signal level value.
     * @param whether show SIM indicator or not.
     * @return The signal strength icon.
     */
    int getSignalStrengthIconGemini(int simColorId, int type, int level, boolean showSimIndicator);

    /**
     * Get the signal strength icon when has not service or null.
     * 
     * @param slotId The slot index for GEMINI.
     * @return The signal strength icon when has not service or null.
     */
    int getSignalStrengthNullIconGemini(int slotId);

    /**
     * Get the signal strength icon when at searching state.
     * 
     * @param slotId The slot index for GEMINI.
     * @return The signal strength icon when at searching state.
     */
    int getSignalStrengthSearchingIconGemini(int slotId);

    /**
     * Get the signal indicator icon.
     * 
     * @param slotId The slot index for GEMINI.
     * @return The signal indicator icon.
     */
    int getSignalIndicatorIconGemini(int slotId);

    /** Signal strength interfaces. @} */

    /** Data connection interfaces. @{ */

    /**
     * Get the data connection type icon list.
     * 
     * @param roaming Whether at roaming state.
     * @param dataType The data connection type.
     * @return The data connection type icon list.
     */
    int[] getDataTypeIconListGemini(boolean roaming, DataType dataType);

    /**
     * Get the data connection type for HSPA.
     * 
     * @return Whether he data connection type 3G instead of H.
     */
    boolean isHspaDataDistinguishable();
    
    /**
     * Get the data connection network type icon.
     * 
     * @param networkType The network type.
     * @param simColorId The SIM color index of GEMINI.
     * @return The data connection network type icon.
     */
    int getDataNetworkTypeIconGemini(NetworkType networkType, int simColorId);

    /**
     * @param simColor The color of SIM.
     * @param showSimIndicator whether show SIM indicator or not.
     * @return Return the data connection activity icon list.
     */
    int[] getDataActivityIconList(int simColor, boolean showSimIndicator);

    /**
     * @return Return if data type icon always display once opened.
     */
    boolean supportDataTypeAlwaysDisplayWhileOn();

    /** Data connection interfaces. @} */

    /** WIFI interfaces. @{ */

    /**
     * @return Return if disable WIFI when at airplane mode.
     */
    boolean supportDisableWifiAtAirplaneMode();

    /** WIFI interfaces. @} */

    /** Resource interfaces. @{ */

    /**
     * @return Return the resources object of plug-in package.
     */
    Resources getPluginResources();

    /**
     * @return Return the string resource of 3g disabled warning.
     */
    String get3gDisabledWarningString();

    /** Resource interfaces. @} */

    /**
     * Get the mobile group should visible
     * 
     * @return true if mobile group should show
     */
    boolean getMobileGroupVisible();

}
