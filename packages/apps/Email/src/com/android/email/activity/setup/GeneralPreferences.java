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

package com.android.email.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.activity.UiUtilities;
import com.android.email.service.MailService;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;
import com.mediatek.email.emailvip.activity.VipListActivity;

public class GeneralPreferences extends PreferenceFragment implements OnPreferenceChangeListener  {

    private static final String PREFERENCE_CATEGORY_KEY = "category_general_preferences";

    private static final String PREFERENCE_KEY_AUTO_ADVANCE = "auto_advance";
    private static final String PREFERENCE_KEY_TEXT_ZOOM = "text_zoom";
    private static final String PREFERENCE_KEY_REPLY_ALL = Preferences.REPLY_ALL;
    private static final String PREFERENCE_KEY_CLEAR_TRUSTED_SENDERS = "clear_trusted_senders";
    /**M: Support for VIP settings @{*/
    private static final String PERFERENCE_KEY_VIPSETTINGS = "vip_settings";
    private static final String PERFERENCE_KEY_VIP_MEMBERS = "vip_members";
    /** @} */
    /// M: Support for auto-clear cache @{
    private static final String PERFERENCE_KEY_AUTO_CLEAR_CACHE = "auto_clear_cache";
    /// @}

    private Preferences mPreferences;
    private ListPreference mAutoAdvance;
    private ListPreference mTextZoom;
    /**M: Support for VIP settings @{*/
    private PreferenceCategory mVipCategory;
    private VipMemberPreference mVipMembers;
    private VipMemberCountObserver mCountObserver;
    private int mMemberCount;
    /** @} */
    /// M: Support for auto-clear cache @{
    private CheckBoxPreference mAutoClearCache;
    /// @}

    CharSequence[] mSizeSummaries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES_FILE);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_preferences);

        if (UiUtilities.useTwoPane(getActivity())) {
            // "Reply All" should only be shown on phones
            PreferenceCategory pc = (PreferenceCategory) findPreference(PREFERENCE_CATEGORY_KEY);
            pc.removePreference(findPreference(PREFERENCE_KEY_REPLY_ALL));
        }
    }

    @Override
    public void onResume() {
        loadSettings();
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (PREFERENCE_KEY_AUTO_ADVANCE.equals(key)) {
            mPreferences.setAutoAdvanceDirection(mAutoAdvance.findIndexOfValue((String) newValue));
            return true;
        } else if (PREFERENCE_KEY_TEXT_ZOOM.equals(key)) {
            mPreferences.setTextZoom(mTextZoom.findIndexOfValue((String) newValue));
            reloadDynamicSummaries();
            return true;
        /**
         * M: Support auto-clear cache @{
         */
        } else if (PERFERENCE_KEY_AUTO_CLEAR_CACHE.equals(key)) {
            if ((Boolean) newValue) {
                mPreferences.setAutoClearCache(true);
                MailService.actionClearOldAttachment(this.getActivity());
            } else {
                mPreferences.setAutoClearCache(false);
                MailService.actionCancelClearOldAttachment(this.getActivity());
            }
            return true;
        }
        /** @} */
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (getActivity() == null) {
            // Guard against monkeys.
            return false;
        }

        String key = preference.getKey();
        if (key.equals(PREFERENCE_KEY_CLEAR_TRUSTED_SENDERS)) {
            mPreferences.clearTrustedSenders();
            Toast.makeText(
                    getActivity(), R.string.trusted_senders_cleared, Toast.LENGTH_SHORT).show();
            return true;
        /**M: Support for VIP settings. Open VipListActivity @{*/
        } else if (PERFERENCE_KEY_VIP_MEMBERS.equals(key)) {
            final Intent vipActivityIntent = VipListActivity.createIntent(getActivity(),
                    Account.ACCOUNT_ID_COMBINED_VIEW);
            getActivity().startActivity(vipActivityIntent);
        }
        /** @} */

        return false;
    }

    private void loadSettings() {
        mPreferences = Preferences.getPreferences(getActivity());
        mAutoAdvance = (ListPreference) findPreference(PREFERENCE_KEY_AUTO_ADVANCE);
        mAutoAdvance.setValueIndex(mPreferences.getAutoAdvanceDirection());
        mAutoAdvance.setOnPreferenceChangeListener(this);

        mTextZoom = (ListPreference) findPreference(PREFERENCE_KEY_TEXT_ZOOM);
        mTextZoom.setValueIndex(mPreferences.getTextZoom());
        mTextZoom.setOnPreferenceChangeListener(this);

        /**
         * M: Support for auto-clear cache @{
         */
        mAutoClearCache = (CheckBoxPreference) findPreference(PERFERENCE_KEY_AUTO_CLEAR_CACHE);
        mAutoClearCache.setChecked(mPreferences.getAutoClearCache());
        mAutoClearCache.setOnPreferenceChangeListener(this);
        /** @} */

        reloadDynamicSummaries();
        /// M: Register Vip count observer
        registerVipCountObserver();
    }

    /**
     * Reload any preference summaries that are updated dynamically
     */
    private void reloadDynamicSummaries() {
        int textZoomIndex = mPreferences.getTextZoom();
        // Update summary - but only load the array once
        if (mSizeSummaries == null) {
            mSizeSummaries = getActivity().getResources()
                    .getTextArray(R.array.general_preference_text_zoom_summary_array);
        }
        CharSequence summary = null;
        if (textZoomIndex >= 0 && textZoomIndex < mSizeSummaries.length) {
            summary = mSizeSummaries[textZoomIndex];
        }
        mTextZoom.setSummary(summary);
    }

    /**M: Support for VIP settings @{*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mVipCategory = (PreferenceCategory)findPreference(PERFERENCE_KEY_VIPSETTINGS);
        mVipMembers = new VipMemberPreference(getActivity());
        mVipMembers.setOrder(0);
        mVipCategory.addPreference(mVipMembers);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStop() {
        unregisterVipCountObserver();
        super.onStop();
    }

    private void registerVipCountObserver() {
        Context context = getActivity();
        if (context != null) {
            mCountObserver = new VipMemberCountObserver(Utility.getMainThreadHandler());
            context.getContentResolver().registerContentObserver(VipMember.CONTENT_URI, true,
                    mCountObserver);
            updateVipMemberCount();
        }
    }

    private void unregisterVipCountObserver() {
        Context context = getActivity();
        if (context != null && mCountObserver != null) {
            context.getContentResolver().unregisterContentObserver(mCountObserver);
        }
    }

    private void updateVipMemberCount() {
        new EmailAsyncTask<Void, Void, Integer>(null) {
            private static final int ERROR_RESULT = -1;
            @Override
            protected Integer doInBackground(Void... params) {
                Context context = getActivity();
                if (context == null) {
                    return ERROR_RESULT;
                }
                return VipMember.countVipMembersWithAccountId(context,
                        Account.ACCOUNT_ID_COMBINED_VIEW);
            }

            @Override
            protected void onSuccess(Integer result) {
                if (result != ERROR_RESULT) {
                    mMemberCount = result;
                    mVipMembers.setCount(result);
                } else {
                    Logging.e("Failed to get the count of the VIP member");
                }
            }
        }.executeParallel();
    }

    private class VipMemberCountObserver extends ContentObserver {

        public VipMemberCountObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateVipMemberCount();
        }
    }

    private class VipMemberPreference extends Preference {
        private TextView mCountView;

        public VipMemberPreference(Context context) {
            super(context);
            setKey(PERFERENCE_KEY_VIP_MEMBERS);
            setTitle(R.string.vip_members);
            setWidgetLayoutResource(R.layout.vip_preference_widget_count);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            // Get the widget view of the member preference
            ViewGroup widgetFrame = (ViewGroup)view.findViewById(com.android.internal.R.id.widget_frame);
            mCountView = (TextView)widgetFrame.findViewById(R.id.vip_count);
            setCount(mMemberCount);
        }

        // Set the count of the VIP member
        public void setCount(int count) {
            if (mCountView != null) {
                mCountView.setText(getContext().getResources().getString(
                        R.string.vip_settings_member_count, count));
            }
        }
    }
    /** @{ */
}
