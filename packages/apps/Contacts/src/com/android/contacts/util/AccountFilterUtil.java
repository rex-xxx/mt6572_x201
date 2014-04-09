/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.list.AccountFilterActivity;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;

// The following lines are provided and maintained by Mediatek Inc.
// Description: for SIM name display
import java.util.List;

import com.android.contacts.ContactsApplication;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.model.AccountWithDataSetEx;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
// The previous lines are provided and maintained by Mediatek inc.

/**
 * Utility class for account filter manipulation.
 */
public class AccountFilterUtil {
    private static final String TAG = AccountFilterUtil.class.getSimpleName();

    /**
     * Find TextView with the id "account_filter_header" and set correct text for the account
     * filter header.
     *
     * @param filterContainer View containing TextView with id "account_filter_header"
     * @return true when header text is set in the call. You may use this for conditionally
     * showing or hiding this entire view.
     */
    public static boolean updateAccountFilterTitleForPeople(View filterContainer,
            ContactListFilter filter, boolean showTitleForAllAccounts) {
        return updateAccountFilterTitle(filterContainer, filter, showTitleForAllAccounts, false);
    }

    /**
     * Similar to {@link #updateAccountFilterTitleForPeople(View, ContactListFilter, boolean,
     * boolean)}, but for Phone UI.
     */
    public static boolean updateAccountFilterTitleForPhone(View filterContainer,
            ContactListFilter filter, boolean showTitleForAllAccounts) {
        return updateAccountFilterTitle(
                filterContainer, filter, showTitleForAllAccounts, true);
    }

    private static boolean updateAccountFilterTitle(View filterContainer,
            ContactListFilter filter, boolean showTitleForAllAccounts,
            boolean forPhone) {
        final Context context = filterContainer.getContext();
        final TextView headerTextView = (TextView)
                filterContainer.findViewById(R.id.account_filter_header);
        
        //MTK_THEMEMANAGER_APP
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            Resources res = context.getResources();
            int textColor = res.getThemeMainColor();
            if (textColor != 0) {
                headerTextView.setTextColor(textColor);
            }
        }
        //MTK_THEMEMANAGER_APP
        
        boolean textWasSet = false;
        if (filter != null) {
            // The following lines are provided and maintained by Mediatek Inc.
            // Description: for SIM name display
            String displayName = null;
            displayName = getAccountDisplayNameByAccount(filter.accountType, filter.accountName);
            if (null == displayName) {
            	if (AccountWithDataSetEx.isLocalPhone(filter.accountType)) {
            		displayName = context.getString(R.string.account_phone_only);
                } else {
                    displayName = filter.accountName;
                }
            }
            // The previous lines are provided and maintained by Mediatek inc.
            if (forPhone) {
                if (filter.filterType == ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS) {
                    if (showTitleForAllAccounts) {
                        headerTextView.setText(R.string.list_filter_phones);
                        textWasSet = true;
                    }
                } else if (filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT) {
                    // The following lines are provided and maintained by Mediatek Inc.
                    // Description: for SIM name display
                    // Keep previous code here.
                    // headerTextView.setText(context.getString(
                    // R.string.listAllContactsInAccount, filter.accountName));
                    headerTextView.setText(context.getString(
                            R.string.listAllContactsInAccount, displayName));
                    // The previous lines are provided and maintained by Mediatek inc.
                    textWasSet = true;
                } else if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
                    headerTextView.setText(R.string.listCustomView);
                    textWasSet = true;
                } else {
                    Log.w(TAG, "Filter type \"" + filter.filterType + "\" isn't expected.");
                }
            } else {
                if (filter.filterType == ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS) {
                    if (showTitleForAllAccounts) {
                        headerTextView.setText(R.string.list_filter_all_accounts);
                        textWasSet = true;
                    }
                } else if (filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT) {
                    // The following lines are provided and maintained by Mediatek Inc.
                    // Description: for SIM name display
                    // Keep previous code here.
                    // headerTextView.setText(context.getString(
                    // R.string.listAllContactsInAccount, filter.accountName));
                    headerTextView.setText(context.getString(
                            R.string.listAllContactsInAccount, displayName));
                    // The previous lines are provided and maintained by Mediatek inc.
                    textWasSet = true;
                } else if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
                    headerTextView.setText(R.string.listCustomView);
                    textWasSet = true;
                } else if (filter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
                    headerTextView.setText(R.string.listSingleContact);
                    textWasSet = true;
                } else {
                    Log.w(TAG, "Filter type \"" + filter.filterType + "\" isn't expected.");
                }
            }
        } else {
            Log.w(TAG, "Filter is null.");
        }
        return textWasSet;
    }

    /**
     * Launches account filter setting Activity using
     * {@link Activity#startActivityForResult(Intent, int)}.
     *
     * @param activity
     * @param requestCode requestCode for {@link Activity#startActivityForResult(Intent, int)}
     * @param currentFilter currently-selected filter, so that it can be displayed as activated.
     */
    public static void startAccountFilterActivityForResult(
            Activity activity, int requestCode, ContactListFilter currentFilter) {
        final Intent intent = new Intent(activity, AccountFilterActivity.class);
        intent.putExtra(AccountFilterActivity.KEY_EXTRA_CURRENT_FILTER, currentFilter);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Very similar to
     * {@link #startAccountFilterActivityForResult(Activity, int, ContactListFilter)}
     * but uses Fragment instead.
     */
    public static void startAccountFilterActivityForResult(
            Fragment fragment, int requestCode, ContactListFilter currentFilter) {
        final Activity activity = fragment.getActivity();
        if (activity != null) {
            final Intent intent = new Intent(activity, AccountFilterActivity.class);
            intent.putExtra(AccountFilterActivity.KEY_EXTRA_CURRENT_FILTER, currentFilter);
            fragment.startActivityForResult(intent, requestCode);
        } else {
            Log.w(TAG, "getActivity() returned null. Ignored");
        }
    }

    /**
     * Useful method to handle onActivityResult() for
     * {@link #startAccountFilterActivityForResult(Activity, int)} or
     * {@link #startAccountFilterActivityForResult(Fragment, int)}.
     *
     * This will update filter via a given ContactListFilterController.
     */
    public static void handleAccountFilterResult(
            ContactListFilterController filterController, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            final ContactListFilter filter = (ContactListFilter)
                    data.getParcelableExtra(AccountFilterActivity.KEY_EXTRA_CONTACT_LIST_FILTER);
            if (filter == null) {
                return;
            }
            if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
                filterController.selectCustomFilter();
            } else {
                filterController.setContactListFilter(filter, true);
            }
        }
    }

    // The following lines are provided and maintained by Mediatek Inc.
    // Description: for SIM name display
    public static String getAccountDisplayNameByAccount(String accountType, String accountName) {
        String accountDisplayName = null;
        if (null == accountType || null == accountName) {
            Log.w(TAG, "[getAccountDisplayNameByAccount] accountType or accountName is null, returned null.");
            return accountDisplayName;
        }

        ContactsApplication contactsApp = ContactsApplication.getInstance();
        if (null == contactsApp) {
            Log.w(TAG, "[getAccountDisplayNameByAccount] contactsApp is null, returned null.");
            return accountDisplayName;
        }
        List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(contactsApp)
                .getAccounts(true);

        if (null == accounts) {
            Log.w(TAG, "[getAccountDisplayNameByAccount] accounts is null, returned null.");
            return accountDisplayName;
        }
        for (AccountWithDataSet ads : accounts) {
            if (ads instanceof AccountWithDataSetEx) {
                if (accountType.equals(ads.type) && accountName.equals(ads.name))
                accountDisplayName = ((AccountWithDataSetEx) ads).getDisplayName();
            }
        }

        return accountDisplayName;
    }
    // The previous lines are provided and maintained by Mediatek inc.
}
