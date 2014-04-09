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

package com.android.email.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.utility.Utility;

public class UiUtilities {
    private UiUtilities() {
    }

    /**
     * Formats the given size as a String in bytes, kB, MB or GB.  Ex: 12,315,000 = 11 MB
     */
    public static String formatSize(Context context, long size) {
        final Resources res = context.getResources();
        final long KB = 1024;
        final long MB = (KB * 1024);
        final long GB  = (MB * 1024);

        int resId;
        int value;

        if (size < KB) {
            resId = R.plurals.message_view_attachment_bytes;
            value = (int) size;
        } else if (size < MB) {
            resId = R.plurals.message_view_attachment_kilobytes;
            value = (int) (size / KB);
        } else if (size < GB) {
            resId = R.plurals.message_view_attachment_megabytes;
            value = (int) (size / MB);
        } else {
            resId = R.plurals.message_view_attachment_gigabytes;
            value = (int) (size / GB);
        }
        return res.getQuantityString(resId, value, value);
    }

    public static String getMessageCountForUi(Context context, int count,
            boolean replaceZeroWithBlank) {
        if (replaceZeroWithBlank && (count == 0)) {
            return "";
        } else if (count > 999) {
            return context.getString(R.string.more_than_999);
        } else {
            return Integer.toString(count);
        }
    }

    /** Generics version of {@link Activity#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(Activity parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /** Generics version of {@link View#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(View parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /**
     * Same as {@link Activity#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(Activity parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    /**
     * Same as {@link View#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(View parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    private static View checkView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("View doesn't exist");
        }
        return v;
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View v, int visibility) {
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(Activity parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Used by an {@link Fragment} to install itself to the host activity.
     *
     * @see FragmentInstallable
     */
    public static void installFragment(Fragment fragment) {
        final Activity a = fragment.getActivity();
        if (a instanceof FragmentInstallable) {
            ((FragmentInstallable) a).onInstallFragment(fragment);
        }
    }

    /**
     * Used by an {@link Fragment} to uninstall itself from the host activity.
     *
     * @see FragmentInstallable
     */
    public static void uninstallFragment(Fragment fragment) {
        final Activity a = fragment.getActivity();
        if (a instanceof FragmentInstallable) {
            ((FragmentInstallable) a).onUninstallFragment(fragment);
        }
    }

    private static int sDebugForcedPaneMode = 0;

    /**
     * Force 1-pane UI or 2-pane UI.
     *
     * @param paneMode Set 1 if 1-pane UI should be used.  Set 2 if 2-pane UI should be used.
     *        Set 0 to use the default UI.
     */
    static void setDebugPaneMode(int paneMode) {
        sDebugForcedPaneMode = paneMode;
    }

    /**
     * @return {@code true} if 2-pane UI should be used.  {@code false} otherwise.
     */
    public static boolean useTwoPane(Context context) {
        if (sDebugForcedPaneMode == 1) {
            return false;
        }
        if (sDebugForcedPaneMode == 2) {
            return true;
        }
        return context.getResources().getBoolean(R.bool.use_two_pane);
    }

    /**
     * Return whether to show search results in a split pane.
     */
    public static boolean showTwoPaneSearchResults(Context context) {
        return context.getResources().getBoolean(R.bool.show_two_pane_search_result);
    }

    /// M: Use to constraint the max word number that user can input.
    public static void setupLengthFilter(EditText inputText, final Context context, 
            final int maxLength , final boolean showToast) {
        InputFilter[] filters = inputText.getFilters();
        InputFilter[] contentFilters = new InputFilter[filters.length + 1];
        for (int i = 0; i < filters.length; i++) {
            contentFilters[i] = filters[i];
        }
        contentFilters[filters.length] = new InputFilter.LengthFilter(maxLength) {
            public CharSequence filter(CharSequence source, int start, int end,
                    Spanned dest, int dstart, int dend) {
                if (source != null && source.length() > 0
                        && (((dest == null ? 0 : dest.length()) + dstart - dend) == maxLength)) {
                    if (showToast) {
                        Toast.makeText(context,
                                context.getString(R.string.not_add_more_text),
                                Toast.LENGTH_SHORT).show();
                    }
                    return "";
                }
                return super.filter(source, start, end, dest, dstart, dend);
            }
        };
        inputText.setFilters(contentFilters);
    }

    /// M: Safely start a activity for result, toast if catch ActivityNotFoundException.
    public static void startRemoteActivityForResult(Activity fromActivity,
            Intent intent, int requestCode, boolean showToast) {
        try {
            fromActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            if (showToast) {
                Utility.showToast(fromActivity,
                        R.string.no_application_response);
            }
            Logging.w("startRemoteActivityForResult ActivityNotFoundException "
                    + e.toString());
        }
    }

    /// M: Safely start a activity, toast if catch ActivityNotFoundException.
    public static void startRemoteActivity(Context fromContext,
            Intent intent, boolean showToast) {
        try {
            fromContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            if (showToast) {
                Utility.showToast(fromContext,
                        R.string.no_application_response);
            }
            Logging.w("startRemoteActivity ActivityNotFoundException "
                    + e.toString());
        }
    }

    /// M:Safely start Contacts, toast if catch ActivityNotFoundException.
    public static void showContacts(Context context, ImageView photoView,
            Uri quickContactLookupUri, Address address) {
        if (quickContactLookupUri != null) {
            try {
                QuickContact.showQuickContact(context, photoView, quickContactLookupUri,
                        QuickContact.MODE_MEDIUM, null);
            } catch (ActivityNotFoundException e) {
                Utility.showToast(context, R.string.no_application_response);
                Logging.w("ShowQuickContact ActivityNotFoundException "
                        + e.toString());
            }
        } else {
            // No matching contact, ask user to create one
            final Uri mailUri = Uri.fromParts("mailto", address.getAddress(), null);
            final Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT,
                    mailUri);

            // Only provide personal name hint if we have one
            final String senderPersonal = address.getPersonal();
            if (!TextUtils.isEmpty(senderPersonal)) {
                intent.putExtra(ContactsContract.Intents.Insert.NAME, senderPersonal);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

            startRemoteActivity(context, intent, true);
        }
    }
    
    /**
     * if is Wifi Only
     * @param context Context
     * @return boolean
     */
    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }
}
