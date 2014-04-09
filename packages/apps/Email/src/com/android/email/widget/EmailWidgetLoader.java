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

package com.android.email.widget;

import com.android.email.R;
import com.android.email.data.ThrottlingCursorLoader;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.Mailbox;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * Loader for {@link EmailWidget}.
 *
 * This loader not only loads the messages, but also:
 * - The number of accounts.
 * - The message count shown in the widget header.
 *   It's currently just the same as the message count, but this will be updated to the unread
 *   counts for inboxes.
 */
class EmailWidgetLoader extends ThrottlingCursorLoader {
    private static final String SORT_TIMESTAMP_DESCENDING = MessageColumns.TIMESTAMP + " DESC";

    // The projection to be used by the WidgetLoader
    private static final String[] WIDGET_PROJECTION = new String[] {
            EmailContent.RECORD_ID, MessageColumns.DISPLAY_NAME, MessageColumns.TIMESTAMP,
            MessageColumns.SUBJECT, MessageColumns.FLAG_READ, MessageColumns.FLAG_FAVORITE,
            MessageColumns.FLAG_ATTACHMENT, MessageColumns.MAILBOX_KEY, MessageColumns.SNIPPET,
            MessageColumns.ACCOUNT_KEY, MessageColumns.FLAGS
            };
    private String[] mSpecialMailbox;
    public static final int WIDGET_COLUMN_ID = 0;
    public static final int WIDGET_COLUMN_DISPLAY_NAME = 1;
    public static final int WIDGET_COLUMN_TIMESTAMP = 2;
    public static final int WIDGET_COLUMN_SUBJECT = 3;
    public static final int WIDGET_COLUMN_FLAG_READ = 4;
    public static final int WIDGET_COLUMN_FLAG_FAVORITE = 5;
    public static final int WIDGET_COLUMN_FLAG_ATTACHMENT = 6;
    public static final int WIDGET_COLUMN_MAILBOX_KEY = 7;
    public static final int WIDGET_COLUMN_SNIPPET = 8;
    public static final int WIDGET_COLUMN_ACCOUNT_KEY = 9;
    public static final int WIDGET_COLUMN_FLAGS = 10;

    private long mAccountId;
    private long mMailboxId;

    /// M: SQL projections and selections for the loader mailbox id recovery
    private static final String MAILBOX_SELECTION = MailboxColumns.ID + "=?";
    private static final String[] MAILBOX_ID_PROJECTION = new String[] {MailboxColumns.ID};
    private static final String MAILBOX_DISPLAYNAME_SELECTION = Mailbox.ACCOUNT_KEY + "=? AND "
            + MailboxColumns.DISPLAY_NAME + "=?";

    /**
     * Cursor data specifically for use by the Email widget. Contains a cursor of messages in
     * addition to a message count and account name. The later elements were opportunistically
     * placed in this cursor. We could have defined multiple loaders for these items.
     */
    static class WidgetCursor extends CursorWrapper {
        private final int mMessageCount;
        private final String mAccountName;
        private final String mMailboxName;
        /// M: Flag to indicate if the cursor need to be reloaded
        private final boolean mNeedReload;

        public WidgetCursor(Cursor cursor, int messageCount, String accountName,
                String mailboxName, boolean needReload) {
            super(cursor);
            mMessageCount = messageCount;
            mAccountName = accountName;
            mMailboxName = mailboxName;
            mNeedReload = needReload;
        }

        /**
         * Gets the count to be shown on the widget header. If the currently viewed mailbox ID is
         * not {@link Mailbox#QUERY_ALL_FAVORITES}, it is the unread count, which is different from
         * number of records returned by {@link #getCount()}.
         */
        public int getMessageCount() {
            return mMessageCount;
        }
        /** Gets the display name of the account */
        public String getAccountName() {
            return mAccountName;
        }
        /** Gets the display name of the mailbox */
        public String getMailboxName() {
            return mMailboxName;
        }
        /** M: Get if the cursor need to be reload @{ */
        public boolean getNeedReload() {
            return mNeedReload;
        }
        /** @} */
    }

    private final Context mContext;
    /// M: The Id of the related widget
    private final int mWidgetId;

    EmailWidgetLoader(Context context, int widgetId) {
        super(context, Message.CONTENT_URI, WIDGET_PROJECTION, null,
                null, SORT_TIMESTAMP_DESCENDING);
        mContext = context;
        mWidgetId = widgetId;
        init();
    }

    // Initializing
    public void init() {
        // Initializing mailbox localized names
        mSpecialMailbox = mContext.getResources().getStringArray(R.array.mailbox_display_names);
        for (int i = 0; i < mSpecialMailbox.length; ++i) {
            if ("".equals(mSpecialMailbox[i])) {
                // there is no localized name, so use the display name from the server
                mSpecialMailbox[i] = null;
            }
        }
    }

    @Override
    public void onContentChanged() {
        Logging.d(EmailWidget.TAG,"EmailWidgetLoader onContentChanged()");
        super.onContentChanged();
    }

    @Override
    public Cursor loadInBackground() {
        Logging.d(EmailWidget.TAG,"EmailWidgetLoader loadInBackground()");
        final Cursor messagesCursor = super.loadInBackground();

        /// M: In case the messagesCursor is null though it almost never happen.
        if (null == messagesCursor) {
            Logging.d(EmailWidget.TAG,"EmailWidgetLoader loadInBackground() messagesCursor is null");
            return null;
        }

        // Reset the notification Uri to our Message table notifier URI
        /// M: Expand the ability , so that it can detect account change.
        messagesCursor.setNotificationUri(mContext.getContentResolver(), EmailContent.CONTENT_NOTIFIER_URI);

        final int messageCount;
        if (mMailboxId != Mailbox.QUERY_ALL_FAVORITES) {
            String selection = "(" + getSelection() + " ) AND " + MessageColumns.FLAG_READ + " = 0";
            messageCount = EmailContent.count(mContext, Message.CONTENT_URI, selection,
                    getSelectionArgs());
        } else {
            // Just use the number of all messages shown.
            messageCount = messagesCursor.getCount();
        }
        Account account = Account.restoreAccountWithId(mContext, mAccountId);
        final String accountName;
        if (account != null) {
            accountName = account.mDisplayName;
        } else {
            if (mAccountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
                int accountCount = EmailContent.count(mContext, Account.CONTENT_URI);
                Resources res = mContext.getResources();
                String countString =
                        res.getQuantityString(R.plurals.picker_combined_view_account_count,
                        accountCount, accountCount);
                accountName = res.getString(R.string.picker_combined_view_fmt, countString);
            } else {
                // TODO What to use here? "unknown"? Account is real, but, doesn't exist.
                accountName = null;
            }
        }
        String mailboxName;
        if (mMailboxId > 0) {
            Mailbox mailbox = Mailbox.restoreMailboxWithId(mContext, mMailboxId);
            if (mailbox != null) {
                String boxName = null;
                int type = mailbox.mType;
                if (type >= 0 && type < mSpecialMailbox.length ) {
                    boxName = mSpecialMailbox[type];
                }
                if (boxName == null || "".equalsIgnoreCase(boxName)) {
                    boxName = mailbox.mDisplayName;
                }
                mailboxName = boxName;
            } else {
                // TODO What use here? "unknown"? Mailbox is "real", but, doesn't exist.
                mailboxName = null;
            }
        } else {
            if (mMailboxId == Mailbox.QUERY_ALL_INBOXES) {
                mailboxName = mContext.getString(R.string.picker_mailbox_name_all_inbox);
            } else { // default to all unread for the account's inbox
                mailboxName = mContext.getString(R.string.picker_mailbox_name_all_unread);
            }
        }

        /** M: This is the recovery for the mailbox id after Exchange account maibox (_eas) arised
            bad sync key. In that case, all the mailboxes will be removed and resync. Therefore,
            mailbox id of this loader will be expired. We need to get the new mailbox id according
            to the mailbox name previously saved in the preferences. We only need to check if the
            situation had happend when the messages cursor is empty and not combined view @{ */
        boolean needReload = false;
        if (messagesCursor != null && messagesCursor.getCount() == 0 &&
                account != null && account.isEasAccount(mContext)) {
            long mailboxId = WidgetManager.loadMailboxIdPref(mContext, mWidgetId);
            Cursor cursor = mContext.getContentResolver().query(Mailbox.CONTENT_URI, null,
                    MAILBOX_SELECTION, new String[]{String.valueOf(mailboxId)}, null);
            boolean mailboxExist = false;
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        mailboxExist = true;
                    }
                } finally {
                    cursor.close();
                }
            }
            // The mailbox id can not be found in DB, then use previous saved
            // mailbox name to restore and save the new id, then restart the widget
            if (!mailboxExist) {
                mailboxName = WidgetManager.loadMailboxNamePref(mContext, mWidgetId);
                cursor = mContext.getContentResolver().query(Mailbox.CONTENT_URI,
                        MAILBOX_ID_PROJECTION, MAILBOX_DISPLAYNAME_SELECTION,
                        new String[]{String.valueOf(mAccountId), mailboxName}, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            WidgetManager.saveWidgetPrefs(mContext, mWidgetId, account.mId, cursor.getLong(0));
                            needReload = true;
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        /** @} */

        return new WidgetCursor(messagesCursor, messageCount, accountName, mailboxName, needReload);
    }

    /**
     * Stop any pending load, reset selection parameters, and start loading.
     *
     * Must be called from the UI thread
     *
     * @param accountId The ID of the account. May be {@link Account#ACCOUNT_ID_COMBINED_VIEW}.
     * @param mailboxId The mailbox to load; may either be a real mailbox or the pseudo mailbox
     *          {@link Mailbox#QUERY_ALL_INBOXES} or {@link Mailbox#QUERY_ALL_UNREAD}. If it's
     *          neither of these pseudo mailboxes, {@link Mailbox#QUERY_ALL_UNREAD} will be used.
     */
    void load(long accountId, long mailboxId) {
        Logging.d(EmailWidget.TAG, "EmailWidgetLoader load() mAccountId: " + accountId
                + ", mailboxId: " + mailboxId);
        reset();
        mAccountId = accountId;
        mMailboxId = mailboxId;
        setSelectionAndArgs();
        startLoading();
    }

    /** Sets the loader's selection and arguments depending upon the account and mailbox */
    private void setSelectionAndArgs() {
        if (mAccountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
            if (mMailboxId == Mailbox.QUERY_ALL_INBOXES) {
                setSelection(Message.ALL_INBOX_SELECTION);
            } else { // default to all unread
                setSelection(Message.ALL_UNREAD_SELECTION);
            }
            setSelectionArgs(null);
        } else {
            if (mMailboxId > 0L) {
                // Simple mailbox selection
                setSelection(
                    MessageColumns.ACCOUNT_KEY + "=? AND " +
                    MessageColumns.MAILBOX_KEY + "=?");
                setSelectionArgs(
                        new String[] { Long.toString(mAccountId), Long.toString(mMailboxId) });
            } else {
                if (mMailboxId == Mailbox.QUERY_ALL_INBOXES) {
                    setSelection(Message.PER_ACCOUNT_INBOX_SELECTION);
                } else { // default to all unread for the account's inbox
                    setSelection(Message.PER_ACCOUNT_UNREAD_SELECTION);
                }
                setSelectionArgs(new String[] { Long.toString(mAccountId) });
            }
        }
    }
}
