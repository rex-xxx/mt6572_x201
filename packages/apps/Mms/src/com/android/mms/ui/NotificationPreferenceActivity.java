/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mms.ui;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.view.Menu;
import android.view.MenuItem;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class NotificationPreferenceActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotificationPreferenceActivity";

    private static final boolean DEBUG = false;

    // Symbolic names for the keys used for preference lookup
    public static final String NOTIFICATION_MUTE = "pref_key_mute";

    public static final String NOTIFICATION_VIBRATE = "pref_key_vibrate";

    public static final String POPUP_NOTIFICATION = "pref_key_popup_notification";

    public static final String NOTIFICATION_ENABLED = "pref_key_enable_notifications";

    public static final String NOTIFICATION_RINGTONE = "pref_key_ringtone";

    public static final String AUTO_RETRIEVAL = "pref_key_mms_auto_retrieval";

    public static final String MUTE_START = "mute_start";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS = 1;

    private CheckBoxPreference mEnableNotificationsPref;

    private CheckBoxPreference mVibratePref;

    private CheckBoxPreference mPopupNotificationPref;

    private ListPreference mNotificaitonMute;

    private TelephonyManagerEx mTelephonyManager;

    private int mCurrentSimCount = 0;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
        setListPrefSummary();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MmsLog.d(TAG, "onCreate");
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.actionbar_notification_setting));
        actionBar.setDisplayHomeAsUpEnabled(true);
        setMessagePreferences();
    }

    private void setListPrefSummary() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        long mMuteStart = sp.getLong(MUTE_START, 0);
        int mMuteOrigin = Integer.parseInt(sp.getString(NOTIFICATION_MUTE, "0"));
        if (mMuteStart > 0 && mMuteOrigin > 0) {
            MmsLog.d(TAG, "thread mute timeout, reset to default.");
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            if ((mMuteOrigin * 3600 + mMuteStart / 1000) <= currentTime) {
                SharedPreferences.Editor editor =
                                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putLong(NotificationPreferenceActivity.MUTE_START, 0);
                editor.putString(NOTIFICATION_MUTE,"0");
                editor.commit();
            }
        }
        // For notificationMute;
        String notificationMute = sp.getString(NOTIFICATION_MUTE, "0");
        mNotificaitonMute.setSummary(MessageUtils.getVisualTextName(this, notificationMute, R.array.pref_mute_choices,
                R.array.pref_mute_values));
    }

    private void setMessagePreferences() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            MmsLog.d(TAG, "MTK_GEMINI_SUPPORT is true");
            mCurrentSimCount = SIMInfo.getInsertedSIMCount(this);
            MmsLog.d(TAG, "mCurrentSimCount is :" + mCurrentSimCount);
            addPreferencesFromResource(R.xml.notificationpreferences);
        } else {
            addPreferencesFromResource(R.xml.notificationpreferences);
        }
        mNotificaitonMute = (ListPreference) findPreference(NOTIFICATION_MUTE);
        mNotificaitonMute.setOnPreferenceChangeListener(this);
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        mVibratePref = (CheckBoxPreference) findPreference(NOTIFICATION_VIBRATE);
        mPopupNotificationPref = (CheckBoxPreference) findPreference(POPUP_NOTIFICATION);
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESTORE_DEFAULTS:
            restoreDefaultPreferences();
            return true;
        case android.R.id.home:
            // The user clicked on the Messaging icon in the action bar. Take them back from
            // wherever they came from
            finish();
            return true;
        default:
            break;
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreDefaultPreferences() {
        SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(NotificationPreferenceActivity.this).edit();
        editor.putBoolean(NOTIFICATION_ENABLED,true);
        editor.putString(NOTIFICATION_MUTE,"0");
        editor.putString(NOTIFICATION_RINGTONE, ChatPreferenceActivity.DEFAULT_RINGTONE);
        editor.putBoolean(NOTIFICATION_VIBRATE, true);
        editor.putBoolean(POPUP_NOTIFICATION, true);
        editor.apply();
        setPreferenceScreen(null);
        setMessagePreferences();
        setListPrefSummary();
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled = prefs.getBoolean(NotificationPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(NotificationPreferenceActivity.NOTIFICATION_ENABLED, enabled);
        editor.apply();
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        final String key = arg0.getKey();
        String notificationMute = (String) arg1;
        if (NOTIFICATION_MUTE.equals(key)) {
            CharSequence mMute = MessageUtils.getVisualTextName(this, notificationMute, R.array.pref_mute_choices,
                R.array.pref_mute_values);
            mNotificaitonMute.setSummary(mMute);
            MmsLog.d(TAG,"preference change: " + mMute.toString());
            if (notificationMute.equals("0")) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NotificationPreferenceActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong(MUTE_START, 0);
                editor.commit();
            } else {
                Long muteTime = System.currentTimeMillis();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(NotificationPreferenceActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong(MUTE_START, muteTime);
                editor.commit();
            }
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MmsLog.d(TAG, "onConfigurationChanged: newConfig = " + newConfig + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        this.getListView().clearScrapViewsIfNeeded();
    }

    public static boolean isNotificationEnable() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean enable = prefs.getBoolean(NotificationPreferenceActivity.NOTIFICATION_ENABLED, false);
        return enable;
    }

    public static boolean isPopupNotificationEnable() {
        if (!isNotificationEnable()) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean enable = prefs.getBoolean(NotificationPreferenceActivity.POPUP_NOTIFICATION, true);
        return enable;
    }
}
