
package com.mediatek.encapsulation.android.provider;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import android.net.Uri;
import android.provider.Settings;

import com.mediatek.encapsulation.EncapsulationConstant;


/**
 * The Settings provider contains global system-level device preferences.
 */
public final class EncapsulatedSettings {

    /**
     * System settings, containing miscellaneous system preferences. This table
     * holds simple name/value pairs. There are convenience functions for
     * accessing individual settings entries.
     */
    public static final class System {

        /**
         * Persistent store for the system-wide default ringtone URI.
         * <p>
         * If you need to play the default ringtone at any given time, it is
         * recommended you give {@link #DEFAULT_RINGTONE_URI} to the media
         * player. It will resolve to the set default ringtone at the time of
         * playing.
         * 
         * @see #DEFAULT_RINGTONE_URI
         */
        public static final String RINGTONE = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.RINGTONE
                : "ringtone";

        /**
         * A {@link Uri} that will point to the current default ringtone at any
         * given time.
         * <p>
         * If the current default ringtone is in the DRM provider and the caller
         * does not have permission, the exception will be a
         * FileNotFoundException.
         */
        public static final Uri DEFAULT_RINGTONE_URI = Settings.System
                .getUriFor(EncapsulatedSettings.System.RINGTONE);

        /**
         * Whether Airplane Mode is on.
         */
        public static final String AIRPLANE_MODE_ON = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.AIRPLANE_MODE_ON
                : "airplane_mode_on";

        /**
         * Default SIM not set
         * 
         * @hide
         */
        /** M: MTK ADD */
        public static final long DEFAULT_SIM_NOT_SET = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.DEFAULT_SIM_NOT_SET
                : -5;

        /**
         * SMS default sim<br/>
         * <b>Values: sim ID</b><br/>
         * 
         * @hide
         */
        /** M: MTK ADD */
        public static final String SMS_SIM_SETTING = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.SMS_SIM_SETTING
                : "sms_sim_setting";

        /**
         * Voice call and sms setting as always ask
         * 
         * @hide
         */
        /** M: MTK ADD */
        public static final long DEFAULT_SIM_SETTING_ALWAYS_ASK = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                : -1;

        /**
         * sms setting as AUTO, only cmcc use,
         *
         * @hide
         */
        /** M: MTK ADD */
        public static final long SMS_SIM_SETTING_AUTO = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.SMS_SIM_SETTING_AUTO
                : -3;

        /**
         * GPRS connection setting as never
         * 
         * @hide
         */
        /** M: MTK ADD */
        public static final long GPRS_CONNECTION_SIM_SETTING_NEVER = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER
                : 0;

        /**
         * GPRS connection default sim<br/>
         * <b>Values: sim ID</b><br/>
         * 
         * @hide
         */
        /** M: MTK ADD */
        public static final String GPRS_CONNECTION_SIM_SETTING = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.System.GPRS_CONNECTION_SIM_SETTING
                : "gprs_connection_sim_setting";

        /**
         * Convenience function for retrieving a single system settings value as
         * a {@code long}. Note that internally setting values are always stored
         * as strings; this function converts the string to a {@code long} for
         * you. The default value will be returned if the setting is not defined
         * or not a {@code long}.
         * 
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         * @return The setting's current value, or 'def' if it is not defined or
         *         not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            return Settings.System.getLong(cr, name, def);
        }

        /**
         * Convenience function for retrieving a single system settings value as
         * an integer. Note that internally setting values are always stored as
         * strings; this function converts the string to an integer for you. The
         * default value will be returned if the setting is not defined or not
         * an integer.
         * 
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         * @return The setting's current value, or 'def' if it is not defined or
         *         not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return Settings.System.getInt(cr, name, def);

        }

    }

    /**
     * Secure system settings, containing system preferences that applications
     * can read but are not allowed to write. These are for preferences that the
     * user must explicitly modify through the system UI or specialized APIs for
     * those values, not modified directly by applications.
     */
    public static final class Global {
        /**
         * Convenience function for retrieving a single secure settings value as
         * an integer. Note that internally setting values are always stored as
         * strings; this function converts the string to an integer for you. The
         * default value will be returned if the setting is not defined or not
         * an integer.
         * 
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         * @return The setting's current value, or 'def' if it is not defined or
         *         not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return Settings.Global.getInt(cr, name, def);
        }

        /**
         * Whether the device has been provisioned (0 = false, 1 = true)
         */
        public static final String DEVICE_PROVISIONED = EncapsulationConstant.USE_MTK_PLATFORM ? Settings.Global.DEVICE_PROVISIONED
                : "device_provisioned";

    }
}
