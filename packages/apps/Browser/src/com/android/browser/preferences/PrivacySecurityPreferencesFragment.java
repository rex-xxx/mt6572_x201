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
 * limitations under the License
 */

package com.android.browser.preferences;

import com.android.browser.PreferenceKeys;
import com.android.browser.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
/// M : HTML5 web notification
import android.preference.ListPreference;

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.mediatek.common.featureoption.FeatureOption;

public class PrivacySecurityPreferencesFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.privacy_security_preferences);

        Preference e = findPreference(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY);
        e.setOnPreferenceChangeListener(this);

        /// M : HTML5 web notification @{
        if (FeatureOption.MTK_WEB_NOTIFICATION_SUPPORT) {
            Preference notificationState = findPreference(PreferenceKeys.KEY_NOTIFICATION_STATE);
            if (notificationState != null) {
                notificationState.setOnPreferenceChangeListener(this);
                updateListPreferenceSummary((ListPreference)notificationState);
            }
        } else {
            PreferenceCategory webNotificationCategory =
                    (PreferenceCategory) findPreference(PreferenceKeys.KEY_WEB_NOTIFICATION);
            getPreferenceScreen().removePreference(webNotificationCategory);
        }
        /// @}

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (pref.getKey().equals(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY)
                && ((Boolean) objValue).booleanValue() == true) {
            // Need to tell the browser to remove the parent/child relationship
            // between tabs
            getActivity().setResult(Activity.RESULT_OK, (new Intent()).putExtra(Intent.EXTRA_TEXT,
                    pref.getKey()));
            return true;
        }

        /// M : HTML5 web notification @{
        if (FeatureOption.MTK_WEB_NOTIFICATION_SUPPORT) {
            if (pref.getKey().equals(PreferenceKeys.KEY_NOTIFICATION_STATE)) {
                ListPreference localListPreference = (ListPreference)pref;
                localListPreference.setValue((String)objValue);
                updateListPreferenceSummary(localListPreference);
                return true;
            }
        }
        /// @}

        return false;
    }

    /// M : HTML5 web notification @{
    void updateListPreferenceSummary(ListPreference listPreference) {
        listPreference.setSummary(listPreference.getEntry());
    }
    /// @}

}
