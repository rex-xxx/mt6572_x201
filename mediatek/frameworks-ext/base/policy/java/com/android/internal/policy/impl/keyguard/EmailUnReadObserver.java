package com.android.internal.policy.impl.keyguard;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import com.mediatek.xlog.Xlog;


public class EmailUnReadObserver extends UnReadObserver {
    
    private static final String TAG = "EmailUnReadObserver";
    
    private static final String EMAIL_AUTHORITY = "com.android.email.provider";
    private static final String EMAIL_NOTIFIER_AUTHORITY = "com.android.email.notifier";
    public static final Uri EMAIL_CONTENT_URI = Uri.parse("content://" + EMAIL_AUTHORITY);
    private static final Uri EMAIL_NOTIFIER_CONTENT_URI = Uri.parse("content://" + EMAIL_NOTIFIER_AUTHORITY);
    private static final Uri EMAIL_MESSAGE_CONTENT_URI = Uri.parse(EMAIL_CONTENT_URI + "/message");

    private interface MailboxColumns {
        public static final String ID = "_id";
        // The type (role) of this mailbox
        public static final String TYPE = "type";
    }

    private interface MessageColumns {
        public static final String ID = "_id";
        // Basic columns used in message list presentation
        // The name as shown to the user in a message list
        public static final String DISPLAY_NAME = "displayName";
        // The time (millis) as shown to the user in a message list [INDEX]
        public static final String TIMESTAMP = "timeStamp";
        // Message subject
        public static final String SUBJECT = "subject";
        // Boolean, unread = 0, read = 1 [INDEX]
        public static final String FLAG_READ = "flagRead";
        // Load state, see constants below (unloaded, partial, complete, deleted)
        public static final String FLAG_LOADED = "flagLoaded";
        // Boolean, unflagged = 0, flagged (favorite) = 1
        public static final String FLAG_FAVORITE = "flagFavorite";
        // Boolean, no attachment = 0, attachment = 1
        public static final String FLAG_ATTACHMENT = "flagAttachment";
        // Bit field for flags which we'll not be selecting on
        public static final String FLAGS = "flags";

        // References to other Email objects in the database
        // Foreign key to the Mailbox holding this message [INDEX]
        public static final String MAILBOX_KEY = "mailboxKey";
        // Foreign key to the Account holding this message
        public static final String ACCOUNT_KEY = "accountKey";
    }

    // Values used in mFlagRead
    private static final int UNREAD = 0;
    private static final int READ = 1;

    // Values used in mFlagLoaded
    private static final int FLAG_LOADED_UNLOADED = 0;
    private static final int FLAG_LOADED_COMPLETE = 1;
    private static final int FLAG_LOADED_PARTIAL = 2;
    private static final int FLAG_LOADED_DELETED = 3;
    // Value only used in IMAP Account 
    private static final int FLAG_LOADED_ENVELOPE = 4;

    /** No type specified */
    private static final int TYPE_NONE = -1;
    /** The "main" mailbox for the account, almost always referred to as "Inbox" */
    private static final int TYPE_INBOX = 0;


    private static final String FLAG_LOADED_SELECTION =
            MessageColumns.FLAG_LOADED + " IN ("
            +     FLAG_LOADED_PARTIAL + "," + FLAG_LOADED_COMPLETE + ","
            +     FLAG_LOADED_ENVELOPE + ")";

    /** Selection to retrieve all messages in "inbox" for any account */
    private static final String ALL_INBOX_SELECTION =
        MessageColumns.MAILBOX_KEY + " IN ("
        +     "SELECT " + MailboxColumns.ID + " FROM " + "Mailbox"
        +     " WHERE " + MailboxColumns.TYPE + " = " + TYPE_INBOX
        +     ")"
        + " AND " + FLAG_LOADED_SELECTION;

    private static final String MESSAGE_ID_SELECTION =
            MessageColumns.ID + ">?";

    /** Selection to retrieve unread messages in "inbox" for any account */
    private static final String ALL_UNREAD_SELECTION =
        MessageColumns.FLAG_READ + "=0 AND " + ALL_INBOX_SELECTION;

    private static final String ALL_LATEST_UNREAD_SELECTION = MessageColumns.FLAG_READ + "=0 AND "
            + ALL_INBOX_SELECTION + " AND " + MESSAGE_ID_SELECTION;

    private static final String[] CONTENT_PROJECTION = new String[] {
        "_id"};
    
    private long mLatestIdOnLock;
    
    public EmailUnReadObserver(Handler handler, LockScreenNewEventView newEventView, long createTime) {
        super(handler, newEventView, createTime);
        // Get the latest message id when start screen lock.
        mLatestIdOnLock = getFirstRowColumn(newEventView.getContext(), EMAIL_MESSAGE_CONTENT_URI,
                CONTENT_PROJECTION, ALL_UNREAD_SELECTION, null, MessageColumns.ID + " desc", 0, 0);
    }
    
    public void refreshUnReadNumber() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            public Integer doInBackground(Void... params) {
                Cursor cursor = mNewEventView.getContext().getContentResolver()
                        .query(EMAIL_MESSAGE_CONTENT_URI,
                                CONTENT_PROJECTION, ALL_LATEST_UNREAD_SELECTION,
                                new String[] {
                                    String.valueOf(mLatestIdOnLock)
                                }, MessageColumns.ID + " desc");
                int count = 0;
                if (cursor != null) {
                    try {
                        count = cursor.getCount();
                    } finally {
                        cursor.close();
                    }
                }
                Xlog.d(TAG, "refreshUnReadNumber count=" + count);
                return count;
            }

            @Override
            public void onPostExecute(Integer result) {
                upateNewEventNumber(result);
            }
        }.execute(null, null, null);
    }
    
    /**
     * @return a long in column {@code column} of the first result row, if the query returns at
     * least 1 row.  Otherwise returns {@code defaultValue}.
     */
    private static long getFirstRowColumn(Context context, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder,
            int column, long defaultValue) {
        Cursor c = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                sortOrder);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return c.getLong(column);
                }
            } finally {
                c.close();
            }
        }
        return defaultValue;
    }
}
