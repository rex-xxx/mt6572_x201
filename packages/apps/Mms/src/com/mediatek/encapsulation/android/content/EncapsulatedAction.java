
package com.mediatek.encapsulation.android.content;

import android.content.Intent;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;

import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedAction {

    /**
     * <p>
     * Broadcast Action: The user has switched the default SIM option for sms.
     * The intent will have the following extra value:
     * </p>
     * <ul>
     * <li><em>simid</em> - A long value indicating which SIM card is set as the
     * default SIM</li>
     * </ul>
     * <p class="note">
     * This is a protected intent that can only be sent by the system.
     * 
     * @hide
     */
    /** M:MTK ADD */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_SMS_DEFAULT_SIM_CHANGED;

    /**
     * <p>
     * Broadcast Action: The user has modified the sim information. The intent
     * will have the following extra value:
     * </p>
     * <ul>
     * <li><em>simid</em> - An int value indicating which sim's information is
     * modified</li>
     * <li><em>type</em> - An int value indicating which kind of information is
     * modified</li>
     * </ul>
     * <p class="note">
     * This is a protected intent that can only be sent by the system.
     * 
     * @hide
     */
    /** M:MTK ADD */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String SIM_SETTINGS_INFO_CHANGED;
    static {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            SIM_SETTINGS_INFO_CHANGED = Intent.SIM_SETTINGS_INFO_CHANGED;
            ACTION_SMS_DEFAULT_SIM_CHANGED = Intent.ACTION_SMS_DEFAULT_SIM_CHANGED;
        } else {
            SIM_SETTINGS_INFO_CHANGED = "android.intent.action.SIM_SETTING_INFO_CHANGED";
            ACTION_SMS_DEFAULT_SIM_CHANGED = "android.intent.action.SMS_DEFAULT_SIM";
        }
    }

}
