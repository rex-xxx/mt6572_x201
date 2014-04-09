/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;/// M
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.policy.IKeyguardLayer;
import com.mediatek.common.policy.KeyguardLayerInfo;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.thememanager.ThemeManager;
import com.mediatek.xlog.Xlog;


import java.util.ArrayList;
import java.util.List;



public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_HDMI_SETTINGS = "hdmi_settings";
    private static final String KEY_TV_OUT = "tvout_settings";
    private static final String KEY_LANDSCAPE_LAUNCHER = "landscape_launcher";
    private static final String KEY_COLOR = "color";
    private static final String KEY_SCENES = "scenes";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_WIFI_DISPLAY = "wifi_display";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private DisplayManager mDisplayManager;

    private static final String DATA_STORE_NONE = "none";

    private CheckBoxPreference mAccelerometer;
    private WarnedListPreference mFontSizePref;
    private CheckBoxPreference mNotificationPulse;

    private final Configuration mCurConfig = new Configuration();
    
    private ListPreference mScreenTimeoutPreference;
    private ListPreference mLandscapeLauncher;
    private Preference mScreenSaverPreference;

    private WifiDisplayStatus mWifiDisplayStatus;
    private Preference mWifiDisplayPreference;

    private Preference mHDMISettings;
    private Preference mTVOut;
    private ISettingsMiscExt mExt;

    /// M: add for category @{
    private PreferenceCategory mDisplayPerCategory;
    private PreferenceCategory mDisplayDefCategory;
    private static final String DISPLAY_PERSONALIZE = "display_personalize";
    private static final String DISPLAY_DEFAULT = "display_default";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String CONTACT_STRING = "&";
    private static final int PARSER_STRING_LENGTH_ZERO = 0;
    private static final int PARSER_STRING_LENGTH_ONE = 1;
    private static final int PARSER_STRING_LENGTH_TWO = 2;
    Preference mColorPref;
    Preference mScencePref;
    Preference mWallpaperPref;
    /// @}

    /// M: add for lockScreen @{
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "lock_screen_notifications";
    private static final String INCOMING_INDICATOR_ON_LOCKSCREEN = "incoming_indicator_on_lockscreen";
    private static final String LOCK_SCREEN_STYLE_INTENT_PACKAGE = "com.android.settings";
    private static final String LOCK_SCREEN_STYLE_INTENT_NAME = "com.mediatek.lockscreensettings.LockScreenStyleSettings";
    private static final String  KEY_LOCK_SCREEN_STYLE = "lock_screen_style";
    public static final String CURRENT_KEYGURAD_LAYER_KEY = "mtk_current_keyguard_layer";
    private static final int DEFAULT_LOCK_SCREEN_NOTIFICATIONS = 1;
    private CheckBoxPreference mLockScreenNotifications;
    private Preference mLockScreenStylePref;
    /// @}

    private boolean mIsUpdateFont;
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateAccelerometerRotationCheckbox();
        }
    };
    
    private ContentObserver mScreenTimeoutObserver = new ContentObserver(new Handler()){
            @Override
            public void onChange(boolean selfChange) {
                Xlog.d(TAG,"mScreenTimeoutObserver omChanged");
                int value=Settings.System.getInt(
                        getContentResolver(), SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
                updateTimeoutPreference(value);
            }
      
        };   
    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context1, Intent intent) {
                Xlog.d(TAG,"package changed, update list");
                updateLandscapeList();
                /// M: add for lockScreen @{
                updateLockScreenStyle();
                /// @}
            }
        };  
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Xlog.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        mExt = Utils.getMiscPlugin(getActivity());
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        /// M: add for category @{
        mDisplayPerCategory = (PreferenceCategory) findPreference(DISPLAY_PERSONALIZE);
        mDisplayDefCategory = (PreferenceCategory) findPreference(DISPLAY_DEFAULT);
        ///@}

        mAccelerometer = (CheckBoxPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        if (RotationPolicy.isRotationLockToggleSupported(getActivity()) && mDisplayDefCategory != null 
                && mAccelerometer != null) {
            // If rotation lock is supported, then we do not provide this option in
            // Display settings.  However, is still available in Accessibility settings.
            /** M: @{ getPreferenceScreen().removePreference(mAccelerometer);  @} */
            mDisplayDefCategory.removePreference(mAccelerometer);
        }

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                    com.android.internal.R.bool.config_dreamsSupported) == false && mDisplayDefCategory != null) {
            /** M: @{ getPreferenceScreen().removePreference(mScreenSaverPreference);  @} */
            mDisplayDefCategory.removePreference(mScreenSaverPreference);
        }
        
        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        /**M: for fix bug ALPS00266723 @{*/
        final long currentTimeout = getTimoutValue();
        Xlog.d(TAG,"currentTimeout=" + currentTimeout);
        /**@}*/
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mExt.setTimeoutPrefTitle(mScreenTimeoutPreference);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        //M: for solve a bug
        updateFontSize(mFontSizePref);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);
        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed) == false 
                        && mDisplayDefCategory != null) {
            /** M: @{ getPreferenceScreen().removePreference(mNotificationPulse); @} */
            mDisplayDefCategory.removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }

        mLandscapeLauncher = (ListPreference)findPreference(KEY_LANDSCAPE_LAUNCHER);                    
        mLandscapeLauncher.setOnPreferenceChangeListener(this);

        mHDMISettings = findPreference(KEY_HDMI_SETTINGS);
        if (!FeatureOption.MTK_HDMI_SUPPORT && mHDMISettings != null && mDisplayDefCategory != null) {
            /** M: @{ getPreferenceScreen().removePreference(mHDMISettings);@} */
            mDisplayDefCategory.removePreference(mHDMISettings);
        }
        mTVOut = (Preference) findPreference(KEY_TV_OUT);
        if (!FeatureOption.MTK_TVOUT_SUPPORT && mTVOut != null && mDisplayDefCategory != null) {
            /** M: @{  getPreferenceScreen().removePreference(mTVOut);@} */
            mDisplayDefCategory.removePreference(mTVOut);
        }
        /** M:  @{
        Preference colorPref = findPreference(KEY_COLOR);
        Preference scencePref = findPreference(KEY_SCENES);
        @} */

        /// M: add for category @{
        mColorPref = findPreference(KEY_COLOR);
        mScencePref = findPreference(KEY_SCENES);
        mWallpaperPref = findPreference(KEY_WALLPAPER);
       /// @}
        if (!FeatureOption.MTK_THEMEMANAGER_APP && mDisplayPerCategory != null 
                                && mColorPref != null && mScencePref != null) {
            Xlog.d(TAG,
                    "remove color preference as FeatureOption.MTK_THEMEMANAGER_APP="
                            + FeatureOption.MTK_THEMEMANAGER_APP);
            /** M: @{ getPreferenceScreen().removePreference(colorPref);@} */
            /** M: @{ getPreferenceScreen().removePreference(scencePref);@} */
            mDisplayPerCategory.removePreference(mColorPref);
            mDisplayPerCategory.removePreference(mScencePref);            
        } else {            
            //query if there is the activity receive the intent, otherwise remove the related pref
            Intent intent = new Intent();
            ComponentName comName = new ComponentName("com.android.launcher", "com.android.launcher2.SceneChooser");
            intent.setComponent(comName);
            List<ResolveInfo> chooseScenceActivities = getPackageManager()
                    .queryIntentActivities(intent, 0);
            boolean chooseScenceActivityExist = chooseScenceActivities != null && chooseScenceActivities.size() != 0;
            if (!chooseScenceActivityExist) {
                Xlog.d(TAG, "SceneChooserActivity doesn't exist, remove Scence pref");
                mDisplayPerCategory.removePreference(mScencePref); 
            }
        }
        
        mDisplayManager = (DisplayManager)getActivity().getSystemService(
                Context.DISPLAY_SERVICE);
        mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        mWifiDisplayPreference = (Preference)findPreference(KEY_WIFI_DISPLAY);
        if (mWifiDisplayStatus.getFeatureState()
                == WifiDisplayStatus.FEATURE_STATE_UNAVAILABLE) {
            /** M: @{ getPreferenceScreen().removePreference(mWifiDisplayPreference);@} */
            mDisplayDefCategory.removePreference(mWifiDisplayPreference);
            mWifiDisplayPreference = null;
        }
      /// M: add for lockScreen @{
        mLockScreenNotifications = (CheckBoxPreference)findPreference(KEY_LOCK_SCREEN_NOTIFICATIONS);
        mLockScreenNotifications.setOnPreferenceChangeListener(this);
        mLockScreenStylePref = findPreference(KEY_LOCK_SCREEN_STYLE);
        updateLockScreenStyle();
      /// @}        
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Xlog.d(TAG,"onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        mCurConfig.updateFrom(newConfig);
    }

    private int getTimoutValue() {
        int currentValue = Settings.System.getInt(getActivity()
                .getContentResolver(), SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        Xlog.d(TAG, "getTimoutValue()---currentValue=" + currentValue);
        int bestMatch = 0;
        int timeout = 0;
        final CharSequence[] valuesTimeout = mScreenTimeoutPreference
                .getEntryValues();
        for (int i = 0; i < valuesTimeout.length; i++) {
            timeout = Integer.parseInt(valuesTimeout[i].toString());
            if (currentValue == timeout) {
                return currentValue;
            } else {
                if (currentValue > timeout) {
                    bestMatch = i;
                }
            }
        }
        Xlog.d(TAG, "getTimoutValue()---bestMatch=" + bestMatch);
        return Integer.parseInt(valuesTimeout[bestMatch].toString());

    }
    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.parseLong(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }
            ///M: to prevent index out of bounds @{
            if (entries.length != 0) {
                summary = preference.getContext().getString(
                        R.string.screen_timeout_summary, entries[best]);
            } else {
                summary = "";
            }
           ///M: @}

            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    /**
     *  Update font size from EM
     *  Add by mtk54043
     */    
        private void updateFontSize(ListPreference fontSizePreference) {
        Xlog.d(TAG, "update font size ");

        final CharSequence[] values = fontSizePreference.getEntryValues();

        float small = Settings.System.getFloat(getContentResolver(),
                Settings.System.FONT_SCALE_SMALL, -1);
        float large = Settings.System.getFloat(getContentResolver(),
                Settings.System.FONT_SCALE_LARGE, -1);
        float extraLarge = Settings.System.getFloat(getContentResolver(),
                Settings.System.FONT_SCALE_EXTRALARGE, -1);
        Xlog.d(TAG, "update font size small = " + small);
        Xlog.d(TAG, "update font size large = " + large);
        Xlog.d(TAG, "update font size extraLarge = " + extraLarge);        
        if (small != -1 || large != -1 || extraLarge != -1) {

            if (null != values[0] && small != -1) {
                values[0] = small + "";
                Xlog.d(TAG, "update font size : " + values[0]);
            }
            if (null != values[2] && large != -1) {
                values[2] = large + "";
                Xlog.d(TAG, "update font size : " + values[2]);
            }
            if (null != values[3] && extraLarge != -1) {
                values[3] = extraLarge + "";
                Xlog.d(TAG, "update font size : " + values[3]);
            }

            if (null != values) {
                fontSizePreference.setEntryValues(values);
            }

            mIsUpdateFont = true;
        }
    }

    ///M: modify by MTK for EM
    int floatToIndex(float val) {
        Xlog.w(TAG, "floatToIndex enter val = " + val); 
        if (mIsUpdateFont) {           
            final CharSequence[] indicesEntry = mFontSizePref.getEntryValues();
            Xlog.d(TAG, "current font size : " + val);
            for (int i = 0; i < indicesEntry.length; i++) {
                float thisVal = Float.parseFloat(indicesEntry[i].toString());
                if (val == thisVal) {
                    Xlog.d(TAG, "Select : " + i);
                    return i;
                }
            }                      
        } else {
            String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
            float lastVal = Float.parseFloat(indices[0]);
            for (int i = 1; i < indices.length; i++) {
                float thisVal = Float.parseFloat(indices[i]);
                if (val < (lastVal + (thisVal - lastVal) * .5f)) {
                    return i - 1;
                }
                lastVal = thisVal;
            }
            return indices.length - 1;
        }
        return 1;
    }
    
    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        Xlog.d(TAG,"readFontSizePreference index = " + index);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume of DisplaySettings");
        ///M: update the landscape list to remove or keep
        updateLandscapeList();
        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
        getContentResolver().registerContentObserver(Settings.System.getUriFor(SCREEN_OFF_TIMEOUT),
                false, mScreenTimeoutObserver);        
        
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mPackageReceiver, filter);

        if (mWifiDisplayPreference != null) {
            getActivity().registerReceiver(mReceiver, new IntentFilter(
                    DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED));
            mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        }

        updateState();

        /// M: add display new feature @{
        ContentResolver cr = getContentResolver();
        mColorPref.setSummary(ThemeManager.getThemeSummary(mColorPref.getContext()));
        mScencePref.setSummary(parseString(Settings.System.getString(cr, "current_scene_name")));
        mWallpaperPref.setSummary(parseString(Settings.System.getString(cr, Settings.System.CURRENT_WALLPAPER_NAME)));
        /// @}

        /**M: for fix bug not sync status bar when lock screen @{*/
        final int currentTimeout = getTimoutValue();
        updateTimeoutPreference(currentTimeout);
        /**@}*/

        /// M: add for lockScreen @{
        mLockScreenNotifications.setChecked(Settings.System.getInt(getContentResolver(),
                INCOMING_INDICATOR_ON_LOCKSCREEN, DEFAULT_LOCK_SCREEN_NOTIFICATIONS) == 1);
        updateLockScreenStyleSummary();
        /// @}
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
        getContentResolver().unregisterContentObserver(mScreenTimeoutObserver);        
        getActivity().unregisterReceiver(mPackageReceiver);
        
        if (mWifiDisplayPreference != null) {
            getActivity().unregisterReceiver(mReceiver);
        }
        
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
        updateWifiDisplaySummary();
    }

    /**M: for fix bug not sync status bar when lock screen @{*/ 
    private void updateTimeoutPreference(int currentTimeout) {
        Xlog.d(TAG,"currentTimeout=" + currentTimeout);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        updateTimeoutPreferenceDescription(currentTimeout);    
        AlertDialog dlg = (AlertDialog)mScreenTimeoutPreference.getDialog();
        if (dlg == null || !dlg.isShowing()) {
            return;
        }
        ListView listview = dlg.getListView();
        int checkedItem = mScreenTimeoutPreference.findIndexOfValue(
        mScreenTimeoutPreference.getValue());
        if (checkedItem > -1) {
            listview.setItemChecked(checkedItem, true);
            listview.setSelection(checkedItem);
        }
    }
    /**@}*/

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
        mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }
        
    private void updateWifiDisplaySummary() {
        if (mWifiDisplayPreference != null) {
            switch (mWifiDisplayStatus.getFeatureState()) {
                case WifiDisplayStatus.FEATURE_STATE_OFF:
                    mWifiDisplayPreference.setSummary(R.string.wifi_display_summary_off);
                    break;
                case WifiDisplayStatus.FEATURE_STATE_ON:
                    mWifiDisplayPreference.setSummary(R.string.wifi_display_summary_on);
                    break;
                case WifiDisplayStatus.FEATURE_STATE_DISABLED:
                default:
                    mWifiDisplayPreference.setSummary(R.string.wifi_display_summary_disabled);
                    break;
            }
        }
    }
        
  /// M: add display new featue @{
    public String parseString(final String decodeStr) {
        if (decodeStr == null) {
            Xlog.w(TAG, "parseString error as decodeStr is null");
            return getString(R.string.default_name);
    }
        String ret = decodeStr;
        String[] tokens = decodeStr.split(CONTACT_STRING);
        int tokenSize = tokens.length;
        if (tokenSize > PARSER_STRING_LENGTH_ONE) {
            PackageManager pm = getPackageManager();
            Resources resources;
            try {
                resources = pm.getResourcesForApplication(tokens[PARSER_STRING_LENGTH_ZERO]);
            } catch (PackageManager.NameNotFoundException e) {
                Xlog.w(TAG, "parseString can not find pakcage: " + tokens[PARSER_STRING_LENGTH_ZERO]);
                return ret;
            }
            int resId;
            try {
                resId = Integer.parseInt(tokens[PARSER_STRING_LENGTH_ONE]);
            } catch (NumberFormatException e) {
                Xlog.w(TAG, "Invalid format of propery string: " + tokens[PARSER_STRING_LENGTH_ONE]);
                return ret;
            }
            if (tokenSize == PARSER_STRING_LENGTH_TWO) {
                ret = resources.getString(resId);
            } else {
                ret = resources.getString(resId, tokens[PARSER_STRING_LENGTH_TWO]);
            }
        }

        Xlog.d(TAG, "parseString return string: " + ret);
        return ret;
    }
    ///@}

    private void updateAccelerometerRotationCheckbox() {
        if (getActivity() == null) return;

        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
        if (mLandscapeLauncher != null) {
            if (mAccelerometer.isChecked()) {
                mLandscapeLauncher.setEnabled(true);    
            } else {
                mLandscapeLauncher.setEnabled(false);    
            }    
        }
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            Xlog.d(TAG, "writeFontSizePreference font size =  " + Float.parseFloat(objValue.toString()));
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mAccelerometer) {
            RotationPolicy.setRotationLockForAccessibility(getActivity(),
                    !mAccelerometer.isChecked());
            if (mLandscapeLauncher != null) {
                if (mAccelerometer.isChecked()) {
                    mLandscapeLauncher.setEnabled(true);
                } else {
                    mLandscapeLauncher.setEnabled(false);
                }
            }
        } else if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_LIGHT_PULSE, value ? 1 : 0);
            return true;
        /// M: add for lockScreen @{
        } else if (preference == mLockScreenNotifications) {
            boolean value = mLockScreenNotifications.isChecked();
            Settings.System.putInt(getContentResolver(),
                                  INCOMING_INDICATOR_ON_LOCKSCREEN, value ? 1 : 0);
        }
        /// @}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        } else if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        } else if (KEY_LANDSCAPE_LAUNCHER.equals(key)) {
            Xlog.d(TAG, "select landscape launcher   " + objValue);
            if (mLandscapeLauncher != null) {
                mLandscapeLauncher.setValue((String)objValue);
                mLandscapeLauncher.setSummary(mLandscapeLauncher.getEntry());   
            }
            Settings.System.putString(getContentResolver(), Settings.System.LANDSCAPE_LAUNCHER,(String)objValue);
        }
        

        return true;
    }
    
    private void updateLandscapeList() {
        int appListSize = 0;

        Intent intent = new Intent(Intent.ACTION_ROTATED_MAIN);
        List<ResolveInfo> mLandscapeLauncherApps = getPackageManager()
                .queryIntentActivities(intent, 0);

        if (mLandscapeLauncherApps != null
                && mLandscapeLauncherApps.size() != 0) {
            Xlog.d(TAG, "mLandscapeLauncherApps.size()="
                    + mLandscapeLauncherApps.size());
            appListSize = mLandscapeLauncherApps.size();
            // If it is already added , this will do nothing.
            /** M: @{ getPreferenceScreen().addPreference(mLandscapeLauncher); @} */
            if (mDisplayDefCategory != null && mLandscapeLauncher != null) {
            mDisplayDefCategory.addPreference(mLandscapeLauncher);
            }
        } else {
            Xlog.d(TAG, "landscape launcher query return null or size 0 ");
            // There is no landscape launcher installed , remove the preference.
             /** M: @{ getPreferenceScreen().removePreference(mLandscapeLauncher); @} */
             if (mDisplayDefCategory != null && mLandscapeLauncher != null) {
             mDisplayDefCategory.removePreference(mLandscapeLauncher);
             }
            Settings.System.putString(getContentResolver(),
                    Settings.System.LANDSCAPE_LAUNCHER, DATA_STORE_NONE);
            return;
        }
        CharSequence[] appStrs = new CharSequence[appListSize + 1];
        CharSequence[] appValues = new CharSequence[appListSize + 1];
        appStrs[0] = getString(R.string.landscape_launcher_none);
        appValues[0] = DATA_STORE_NONE;
        String current = Settings.System.getString(getContentResolver(),
                Settings.System.LANDSCAPE_LAUNCHER);
        if (current == null) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.LANDSCAPE_LAUNCHER, DATA_STORE_NONE);
            current = DATA_STORE_NONE;
        }

        int i = 1;
        int setIdx = 0;
        if (mLandscapeLauncherApps != null) {

            PackageManager pm = getPackageManager();
            for (ResolveInfo info : mLandscapeLauncherApps) {
                Xlog.i(TAG, "resolve app : " + info.activityInfo.packageName
                        + " " + info.activityInfo.name);
                appStrs[i] = info.activityInfo.loadLabel(pm);
                appValues[i] = info.activityInfo.packageName + "/"
                        + info.activityInfo.name;
                if (current.equals(appValues[i])) {
                    setIdx = i;
                }
                ++i;
            }
        }
        if (setIdx == 0 && !current.equals(DATA_STORE_NONE)) {
            // Because current package maybe uninstalled, so no match found ,
            // set it back to None.
            Settings.System.putString(getContentResolver(),
                    Settings.System.LANDSCAPE_LAUNCHER, DATA_STORE_NONE);
        }
        mLandscapeLauncher.setEntries(appStrs);
        mLandscapeLauncher.setEntryValues(appValues);
        mLandscapeLauncher.setValueIndex(setIdx);
        mLandscapeLauncher.setSummary(appStrs[setIdx]);
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                mWifiDisplayStatus = (WifiDisplayStatus)intent.getParcelableExtra(
                        DisplayManager.EXTRA_WIFI_DISPLAY_STATUS);
                updateWifiDisplaySummary();
            }
        }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }
    
    /// M: add for lockScreen @{
    private void updateLockScreenStyle() {
        Intent intent = new Intent();
        ComponentName comName = new ComponentName(LOCK_SCREEN_STYLE_INTENT_PACKAGE, LOCK_SCREEN_STYLE_INTENT_NAME);
        intent.setComponent(comName);
        List<ResolveInfo> lockScreenStyleApps = getPackageManager()
                .queryIntentActivities(intent, 0);
        boolean hasPlugin = queryPluginKeyguardLayers();

        if (lockScreenStyleApps != null
                && lockScreenStyleApps.size() != 0 && hasPlugin) {
            Xlog.d(TAG, "lockScreenStyleApps.size()="
                    + lockScreenStyleApps.size());
            if (mDisplayPerCategory != null && mLockScreenStylePref != null) {
                mDisplayPerCategory.addPreference(mLockScreenStylePref);
            }
        } else {
            Xlog.d(TAG, "lock screen style query return null or size 0 ");
            // There is no lock screen style installed , remove the preference.
             if (mDisplayPerCategory != null && mLockScreenStylePref != null) {
                 mDisplayPerCategory.removePreference(mLockScreenStylePref);
             }
            return;
        }

        updateLockScreenStyleSummary();
 
    }
    
    /**
     * Get key guard layers from system, a key guard layer should implement IKeyguardLayer interface. Plugin app should make
     * sure the data is valid.
     */
    private boolean queryPluginKeyguardLayers() {
        boolean pluginLayers = false;
        KeyguardLayerInfo info = null;
        try {
            final PluginManager plugManager = PluginManager.<IKeyguardLayer> create(getActivity(), 
                    IKeyguardLayer.class.getName());
            final int pluginCount = plugManager.getPluginCount();
            Xlog.d(TAG, "getKeyguardLayers: pluginCount = " + pluginCount);
            if (pluginCount != 0) {
                Plugin<IKeyguardLayer> plugin;
                IKeyguardLayer keyguardLayer;
                for (int i = 0; i < pluginCount; i++) {
                    plugin = plugManager.getPlugin(i);
                    keyguardLayer = (IKeyguardLayer) plugin.createObject();
                    info = keyguardLayer.getKeyguardLayerInfo();
                    Xlog.d(TAG, "getKeyguardLayers: i = " + i + ",keyguardLayer = " + keyguardLayer + ",info = " + info);
                    if (info != null) {
                        pluginLayers  = true;
                        return pluginLayers;
                    }
                }
            }
        } catch (Exception e) {
            Xlog.e(TAG, "getPluginKeyguardLayers exception happens: e = " + e.getMessage());
            return false;
        }

        return pluginLayers;
    }
    
    private void updateLockScreenStyleSummary() {
        String lockScreenStyleSummary = parseString(Settings.System.getString(
                getContentResolver(), CURRENT_KEYGURAD_LAYER_KEY));
        if (lockScreenStyleSummary.equals("")) {
            Xlog.d(TAG, "lockScreenStyleSummary = " + lockScreenStyleSummary);
            mLockScreenStylePref.setSummary(R.string.default_name);
        } else {
            mLockScreenStylePref.setSummary(lockScreenStyleSummary);
        } 
        
    }
    /// @}
    
}

