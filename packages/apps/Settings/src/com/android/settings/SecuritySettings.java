/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ListView;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.gemini.SimListEntrance;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;



/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener {

    private static final String TAG = "SecuritySettings";
    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
            "biometric_weak_improve_matching";
    private static final String KEY_BIOMETRIC_WEAK_LIVELINESS = "biometric_weak_liveliness";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_DEVICE_ADMIN_CATEGORY = "device_admin_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SIM_LOCK_PREF = "sim_lock_pref";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_RESET_CREDENTIALS = "reset_credentials";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_TOGGLE_VERIFY_APPLICATIONS = "toggle_verify_applications";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    /**M: the delayMillis 500 for scrolling @ {*/
    private static final int DELAY_MILLIS = 500;
    /** @}*/

    DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;

    private CheckBoxPreference mBiometricWeakLiveliness;
    private CheckBoxPreference mVisiblePattern;

    private CheckBoxPreference mShowPassword;

    private Preference mResetCredentials;

    private CheckBoxPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;
    private CheckBoxPreference mToggleVerifyApps;
    private CheckBoxPreference mPowerButtonInstantlyLocks;
    private boolean mIsPrimary;

    /**M: the position of the "Unknown Sources" item in the listview @ {*/
    private int mUnknownSourcesPosition;
    /** @}*/

    /**M: the flag whether to scroll to Unknown sources @ {*/
    private boolean mScrollToUnknownSources;
    /** @}*/

    /**M: make sure scrolling action occurs after the binding action completed @ {*/
    private Handler mScrollHandler = new Handler();
    private Runnable mScrollRunner = new Runnable() {
  
        public void run() {
            ListView lstView = getListView();
            // the index of the listview start from 0, then mUnknownSourcesPosition - 1 as the index.
            lstView.setItemChecked(mUnknownSourcesPosition - 1, true);
            lstView.smoothScrollToPosition(mUnknownSourcesPosition - 1);
        }
    };
    /** @}*/
    ///M: add for SIM hot swap
    private BroadcastReceiver mSiminfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "receive action=" + action);
            if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                createPreferenceHierarchy();
                final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
                if (mBiometricWeakLiveliness != null) {
                    mBiometricWeakLiveliness.setChecked(
                            lockPatternUtils.isBiometricWeakLivelinessEnabled());
                }
                if (mVisiblePattern != null) {
                    mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
                }
                if (mPowerButtonInstantlyLocks != null) {
                    mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
                }

                if (mShowPassword != null) {
                    mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                            Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
                }

                KeyStore.State state = KeyStore.getInstance().state();
                Xlog.i(TAG, "KeyStore.State on receiver:" + state);
                if (mResetCredentials != null) {
                    mResetCredentials.setEnabled(state != KeyStore.State.UNINITIALIZED);
                }
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        
        /**M: get the action in the intent starting the activity @ {*/ 
        String action = getActivity().getIntent().getAction();
        if (android.provider.Settings.ACTION_SECURITY_SETTINGS.equals(action)) {
            mScrollToUnknownSources = true;
        }
        Xlog.i(TAG, "action :" + action);
        Xlog.i(TAG, "activity name :" + getActivity().getComponentName().getClassName());
        /** @}*/
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        int resid = 0;
        if (!mLockPatternUtils.isSecure()) {
            // if there are multiple users, disable "None" setting
            UserManager mUm = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = mUm.getUsers(true);
            final boolean singleUser = users.size() == 1;

            if (singleUser && mLockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (mLockPatternUtils.usingBiometricWeak() &&
                mLockPatternUtils.isBiometricWeakInstalled()) {
            resid = R.xml.security_settings_biometric_weak;
        } else if (mLockPatternUtils.usingVoiceWeak()) { //M: add for voice unlock
            resid = R.xml.security_settings_voice_weak;
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        addPreferencesFromResource(resid);
        ///M: only emmc project support the feature
        Xlog.d(TAG,"FeatureOption.MTK_EMMC_SUPPORT=" + FeatureOption.MTK_EMMC_SUPPORT);
        Xlog.d(TAG,"FeatureOption.MTK_CACHE_MERGE_SUPPORT=" + FeatureOption.MTK_CACHE_MERGE_SUPPORT);
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (FeatureOption.MTK_EMMC_SUPPORT && !FeatureOption.MTK_CACHE_MERGE_SUPPORT) {
            // Add options for device encryption
            DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (!mIsPrimary) {
                // Rename owner info settings
                Preference ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
                if (ownerInfoPref != null) {
                    ownerInfoPref.setTitle(R.string.user_info_settings_title);
                }
            }

            if (mIsPrimary) {
                switch (dpm.getStorageEncryptionStatus()) {
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                    // The device is currently encrypted.
                    addPreferencesFromResource(R.xml.security_settings_encrypted);
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                    // This device supports encryption but isn't encrypted.
                    addPreferencesFromResource(R.xml.security_settings_unencrypted);
                    break;
                }
            }
        }
        // lock after preference
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // biometric weak liveliness
        mBiometricWeakLiveliness =
                (CheckBoxPreference) root.findPreference(KEY_BIOMETRIC_WEAK_LIVELINESS);

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (CheckBoxPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);

        // don't display visible pattern if biometric and backup is not pattern
        if ((resid == R.xml.security_settings_biometric_weak || 
                resid == R.xml.security_settings_voice_weak) &&  /*M: add for voice unlock*/
                mLockPatternUtils.getKeyguardStoredPasswordQuality() !=
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mVisiblePattern != null) {
                securityCategory.removePreference(root.findPreference(KEY_VISIBLE_PATTERN));
            }
        }

        // Append the rest of the settings
        addPreferencesFromResource(R.xml.security_settings_misc);
      ///M: add by mtk to handle gemini feature
        if (!(Utils.isWifiOnly(getActivity()))) {
            // Do not display SIM lock for CDMA phone
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                TelephonyManager tm = TelephonyManager.getDefault();
                if (!mIsPrimary || !tm.hasIccCard()) {
                    root.removePreference(root.findPreference(KEY_SIM_LOCK));
                } else {
                // Disable SIM lock if sim card is missing or unknown
                if ((TelephonyManager.getDefault().getSimState() ==
                                 TelephonyManager.SIM_STATE_ABSENT) ||
                    (TelephonyManager.getDefault().getSimState() ==
                                 TelephonyManager.SIM_STATE_UNKNOWN)) {
                    root.findPreference(KEY_SIM_LOCK).setEnabled(false);
                    }
                }
            }
            if (root.findPreference(KEY_SIM_LOCK) != null) {
                Preference simLockPref = root.findPreference(KEY_SIM_LOCK_PREF);
                List<SIMInfo> simList = SIMInfo
                        .getInsertedSIMList(getActivity());
                int nSimNum = simList.size();
                if (nSimNum == 0) {
                    Xlog.d(TAG, "No sim found");
                    simLockPref.setEnabled(false);
                } else if (nSimNum == 1) {
                    SIMInfo simInfo = simList.get(0);
                    Xlog.d(TAG, "Only one Sim inserted");
                    if (simInfo != null) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.settings",
                                "com.android.settings.IccLockSettings");
                        int slot = SIMInfo.getSlotById(getActivity(),
                                simInfo.mSimId);
                        intent.putExtra("slotid", simInfo.mSlot);
                        simLockPref.setIntent(intent);
                    }
                } else if (nSimNum > 1) {
                    Xlog.d(TAG, "two Sims inserted");
                    simLockPref.getExtras().putInt("type",
                            SimListEntrance.PIN_SETTING_INDEX);
                    simLockPref.setFragment(SimListEntrance.class.getName());
                }
            }
        } else {
            root.removePreference(root.findPreference(KEY_SIM_LOCK));
        }
        // Show password
        mShowPassword = (CheckBoxPreference) root.findPreference(KEY_SHOW_PASSWORD);

        // Credential storage, only for primary user
        if (mIsPrimary) {
            mResetCredentials = root.findPreference(KEY_RESET_CREDENTIALS);
        } else {
            removePreference(KEY_CREDENTIALS_MANAGER);
        }

        mToggleAppInstallation = (CheckBoxPreference) findPreference(
                KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());

        // Package verification, only visible to primary user and if enabled
        mToggleVerifyApps = (CheckBoxPreference) findPreference(KEY_TOGGLE_VERIFY_APPLICATIONS);
        if (mIsPrimary && showVerifierSetting()) {
            if (isVerifierInstalled()) {
                mToggleVerifyApps.setChecked(isVerifyAppsEnabled());
            } else {
                mToggleVerifyApps.setChecked(false);
                mToggleVerifyApps.setEnabled(false);
            }
        } else {
            PreferenceGroup deviceAdminCategory= (PreferenceGroup)
                    root.findPreference(KEY_DEVICE_ADMIN_CATEGORY);
            if (deviceAdminCategory != null) {
                deviceAdminCategory.removePreference(mToggleVerifyApps);
            } else {
                mToggleVerifyApps.setEnabled(false);
            }
        }

        return root;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS,
                                enabled ? 1 : 0);
    }

    private boolean isVerifyAppsEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.PACKAGE_VERIFIER_ENABLE, 1) > 0;
    }

    private boolean isVerifierInstalled() {
        final PackageManager pm = getPackageManager();
        final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
        verification.setType(PACKAGE_MIME_TYPE);
        verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
        return (receivers.size() > 0) ? true : false;
    }

    private boolean showVerifierSetting() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1) > 0;
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps && which == DialogInterface.BUTTON_POSITIVE) {
            setNonMarketAppsAllowed(true);
            if (mToggleAppInstallation != null) {
                mToggleAppInstallation.setChecked(true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWarnInstallApps != null) {
            mWarnInstallApps.dismiss();
        }
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
        
        /**M: scroll the screen for "Unknown Sources" @ {*/ 
        if (mScrollToUnknownSources) {
            mScrollToUnknownSources = false;
            // reset the position as 0 and find the position of the "unknownSource"
            mUnknownSourcesPosition = 0;
            findPreferencePosition(KEY_TOGGLE_INSTALL_APPLICATIONS, getPreferenceScreen());
            Xlog.i(TAG, "find position unknown resources: " + (mUnknownSourcesPosition - 1));
            mScrollHandler.postDelayed(mScrollRunner, DELAY_MILLIS);
        }
        /** @}*/
        
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mBiometricWeakLiveliness != null) {
            mBiometricWeakLiveliness.setChecked(
                    lockPatternUtils.isBiometricWeakLivelinessEnabled());
        }
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
        }

        if (mShowPassword != null) {
            mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
        }

        KeyStore.State state = KeyStore.getInstance().state();
        Xlog.i(TAG, "KeyStore.State on resume:" + state);
        if (mResetCredentials != null) {
            mResetCredentials.setEnabled(state != KeyStore.State.UNINITIALIZED);
        }
        ///M: add for sim hot swap
        this.getActivity().registerReceiver(mSiminfoReceiver, 
                new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE));
    }

    ///M: add for sim hot swap
    @Override
    public void onPause() {
        super.onPause();
        this.getActivity().unregisterReceiver(mSiminfoReceiver);
    }
    /** M: mtk add for getting the count of preference in traversing the root PreferenceScreen 
        to find the specified preference,reference to PreferenceGroup.findPreference(). @{ */
    private Preference findPreferencePosition(CharSequence key, PreferenceGroup root) {
        if (key.equals(root.getKey())) {
            return root;
        } else {
            // if the PreferenceGroup is not preference to find,then count + 1.
            mUnknownSourcesPosition++;
        }
        final int preferenceCount = root.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = root.getPreference(i);
            final String curKey = preference.getKey();
            if (curKey != null && curKey.equals(key)) {
                return preference;
            }
            
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup group = (PreferenceGroup)preference;
                final Preference returnedPreference = findPreferencePosition(key, group);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            } else {
             // if the Preference is not preference to find,then count + 1.
                mUnknownSourcesPosition++;
            }
        }

        return null;
    }
    /** @ } */
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST, null, null)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and start improve.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                startBiometricWeakImprove();
            }
        } else if (KEY_BIOMETRIC_WEAK_LIVELINESS.equals(key)) {
            if (isToggled(preference)) {
                lockPatternUtils.setBiometricWeakLivelinessEnabled(true);
            } else {
                // In this case the user has just unchecked the checkbox, but this action requires
                // them to confirm their password.  We need to re-check the checkbox until
                // they've confirmed their password
                mBiometricWeakLiveliness.setChecked(true);
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(
                                CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, null)) {
                    // If this returns false, it means no password confirmation is required, so
                    // go ahead and uncheck it here.
                    // Note: currently a backup is required for biometric_weak so this code path
                    // can't be reached, but is here in case things change in the future
                    lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
                    mBiometricWeakLiveliness.setChecked(false);
                }
            }
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            lockPatternUtils.setPowerButtonInstantlyLocks(isToggled(preference));
        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else if (preference == mToggleAppInstallation) {
            if (mToggleAppInstallation.isChecked()) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
            } else {
                setNonMarketAppsAllowed(false);
            }
        } else if (KEY_TOGGLE_VERIFY_APPLICATIONS.equals(key)) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.PACKAGE_VERIFIER_ENABLE,
                    mToggleVerifyApps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            // Setting the mBiometricWeakLiveliness checked value to false is handled when onResume
            // is called by grabbing the value from lockPatternUtils.  We can't set it here
            // because mBiometricWeakLiveliness could be null
            return;
        }
        createPreferenceHierarchy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mLockAfter) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        }
        return true;
    }

    public void startBiometricWeakImprove() {
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }
    
    /**
     * M: handle configuration change event
     * @param configuration
     * @return
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        this.getListView().clearScrapViewsIfNeeded();
    }
}
