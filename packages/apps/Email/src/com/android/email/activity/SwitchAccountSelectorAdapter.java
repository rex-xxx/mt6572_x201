/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.android.email.R;
import com.android.email.ResourceHelper;
import com.android.email.data.ThrottlingCursorLoader;
import com.android.emailcommon.provider.Account;

/**
 * Account selector spinner.
 *
 */
public class SwitchAccountSelectorAdapter extends CursorAdapter {

    /** "account id" virtual column name for the matrix cursor */
    private static final String ACCOUNT_ID = "accountId";

    /** Projection for account database query */
    private static final String[] ACCOUNT_PROJECTION = new String[] {
        Account.ID,
        Account.DISPLAY_NAME,
        Account.EMAIL_ADDRESS,
    };

    private static final int ID = 0;
    private static final int EMAIL_ADDRESS = 1;
    private static final int DISPLAY_NAME = 2;

    /** Sort order.  Show the default account first. */
    private static final String ORDER_BY = Account.IS_DEFAULT + " desc, " + Account.RECORD_ID;

    @SuppressWarnings("hiding")
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final ResourceHelper mResourceHelper;

    /**
     * Returns a loader that can populate the account spinner.
     * @param context a context
     * @param accountId the ID of the currently viewed account
     */
    public static Loader<Cursor> createLoader(Context context) {
        return new AccountsLoader(context);
    }

    public SwitchAccountSelectorAdapter(Context context) {
        super(context, null, 0 /* no auto-requery */);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResourceHelper = ResourceHelper.getInstance(context);
    }

    /**
     * {@inheritDoc}
     *
     * The account selector view can contain one of four types of row data:
     * <ol>
     * <li>headers</li>
     * <li>accounts</li>
     * <li>recent mailboxes</li>
     * <li>"show all folders"</li>
     * </ol>
     * Headers are handled separately as they have a unique layout and cannot be interacted with.
     * Accounts, recent mailboxes and "show all folders" all have the same interaction model and
     * share a very similar layout. The single difference is that both accounts and recent
     * mailboxes display an unread count; whereas "show all folders" does not. To determine
     * if a particular row is "show all folders" verify that a) it's not an account row and
     * b) it's ID is {@link Mailbox#NO_MAILBOX}.
     *
     * TODO Use recycled views.  ({@link #getViewTypeCount} and {@link #getItemViewType})
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        View view = mInflater.inflate(R.layout.action_bar_spinner_dropdown, parent, false);
        final TextView displayNameView = (TextView) view.findViewById(R.id.display_name);
        final TextView emailAddressView = (TextView) view.findViewById(R.id.email_address);
        final View chipView = view.findViewById(R.id.color_chip);
        chipView.setVisibility(View.GONE);

        final String displayName = getDisplayName(position);
        final String emailAddress = getAccountEmailAddress(position);

        displayNameView.setText(displayName);
        emailAddressView.setVisibility(View.VISIBLE);
        emailAddressView.setText(emailAddress);
        return view;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return null; // we don't reuse views.  This method never gets called.
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // we don't reuse views.  This method never gets called.
    }

    /**
     * @return ID of the account for a row
     */
    public long getAccountId(int position) {
        final Cursor c = getCursor();
        c.moveToPosition(position);
        return c.getLong(ID);
    }

    /** Returns the account name extracted from the given cursor. */
    public String getDisplayName(int position) {
        final Cursor c = getCursor();
        c.moveToPosition(position);
        return c.getString(DISPLAY_NAME);
    }

    /** Returns the email address extracted from the given cursor. */
    public  String getAccountEmailAddress(int position) {
        final Cursor c = getCursor();
        c.moveToPosition(position);
        return c.getString(EMAIL_ADDRESS);
    }

    /**
     * Load the account list.  The resulting cursor contains
     * - Account info
     */
    static class AccountsLoader extends ThrottlingCursorLoader {
        private final Context mContext;
        private static final int ACCOUNT_LOADER_MIN_TIME_OUT = 1000;
        private static final int ACCOUNT_LOADER_MAX_TIME_OUT = 3000;

        AccountsLoader(Context context) {
            // Super class loads a regular account cursor, but we replace it in loadInBackground().
            super(context, Account.CONTENT_URI, ACCOUNT_PROJECTION, null, null,
                    ORDER_BY, ACCOUNT_LOADER_MIN_TIME_OUT, ACCOUNT_LOADER_MAX_TIME_OUT);
            mContext = context;
        }

        @Override
        public Cursor loadInBackground() {
            final Cursor accountsCursor = super.loadInBackground();
            return accountsCursor;
        }

    }
}
