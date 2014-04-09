package com.mediatek.email.provider.history;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.text.TextUtils;

import com.mediatek.email.provider.history.EmailAddressContent.AddressColumns;
import com.android.emailcommon.Logging;
import com.android.emailcommon.utility.Utility;

/**
 * M: Add EmailAddressHisProviders for query and insert
 */
public class EmailAddressHisProviders extends ContentProvider {
    private static final String TAG = "EmailAddressHisProvider";
    protected static final String DATABASE_NAME = "EmailAddressHisProviders.db";
    private static final int BASE_SHIFT = 12;
    private static final String DEFAULT_LOOKUP_LIMIT = "50";

    private static final int ADDRESS_BASE = 0;
    private static final int ADDRESS = ADDRESS_BASE;
    private static final int ADDRESS_FILTER = ADDRESS_BASE + 1;
    private static final int ADDRESS_ID = ADDRESS_BASE + 2;
    private static final int CONTACT_FILTER = ADDRESS_BASE + 3;
    private static final int EMAIL_FILTER = ADDRESS_BASE + 4;
    private static final int DIRECTORIES = ADDRESS_BASE + 5;

    public static final String MIME_ADDRESS = "vnd.android.cursor.dir/eah-address";
    private SQLiteDatabase mDb;

    private static final String[] TABLE_NAMES = { EmailAddress.TABLE_NAME };
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        UriMatcher matcher = sURIMatcher;
        // all address
        matcher.addURI(EmailAddressContent.AUTHORITY, "address", ADDRESS);
        // filter address
        matcher.addURI(EmailAddressContent.AUTHORITY, "address/filter/*", ADDRESS_FILTER);
        // special address
        matcher.addURI(EmailAddressContent.AUTHORITY, "address/#", ADDRESS_ID);
        matcher.addURI(EmailAddressContent.AUTHORITY, "contacts/filter/*", CONTACT_FILTER);
        matcher.addURI(EmailAddressContent.AUTHORITY, "data/emails/filter/*", EMAIL_FILTER);
        matcher.addURI(EmailAddressContent.AUTHORITY, "directories", DIRECTORIES);
    }

    synchronized SQLiteDatabase getDatabase(Context context) {
        if (mDb != null) {
            return mDb;
        }
        DatabaseHelper DbHelper = new DatabaseHelper(getContext(), DATABASE_NAME);
        mDb = DbHelper.getWritableDatabase();
        return mDb;
    }

    @Override
    public String getType(Uri uri) {
        int match = findMatch(uri, "getType");
        switch (match) {
            case ADDRESS:
                return MIME_ADDRESS;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = findMatch(uri, "insert");
        Context context = getContext();
        SQLiteDatabase db = getDatabase(context);
        int table = match >> BASE_SHIFT;
        Uri resultUri = null;
        String spliteString[] = { ",", ";" };
        String[] emailPart = null;
        String emailAddress = values.getAsString(AddressColumns.EMAIL_ADDRESS);
        if (TextUtils.isEmpty(emailAddress)) {
            return null;
        }
        emailAddress = emailAddress.trim();
        for (int i = 0; i < spliteString.length; i++) {
            emailPart = emailAddress.split(spliteString[i]);
            String dis_emailAddress = emailPart[0];
            Long longId;
            if (Utility.isValidEmailAddress(dis_emailAddress)) {
                if (!findExistingAddress(getContext(), dis_emailAddress)) {
                    try {
                        ContentValues cv = new ContentValues();
                        cv.put(AddressColumns.EMAIL_ADDRESS, dis_emailAddress);
                        switch (match) {
                            case ADDRESS:
                                longId = db.insert(TABLE_NAMES[table], AddressColumns.EMAIL_ADDRESS, cv);
                                resultUri = ContentUris.withAppendedId(uri, longId);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown URL " + uri);
                        }

                    } catch (SQLiteException e) {
                        throw e;
                    }
                } else {
                    resultUri = null;
                }

            } else {
                resultUri = null;
            }
        }

        return resultUri;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor c = null;
        int match = findMatch(uri, "query");
        Context context = getContext();
        SQLiteDatabase db = getDatabase(context);
        int table = match >> BASE_SHIFT;
        String id;
        String filter;
        try {
            switch (match) {
                case ADDRESS_FILTER:
                    filter = uri.getLastPathSegment();
                    if (filter == null || filter.length() < 1) {
                        return null;
                    }
                    selection = AddressColumns.EMAIL_ADDRESS + " like ? escape '\\'";
                    if (filter.contains("_")) {
                        filter = filter.replace("_", "\\_");
                    } else if (filter.contains("%")) {
                        filter = filter.replace("%", "\\%");
                    }
                    String[] selectionArg = { filter + "%" };
                    c = db.query(TABLE_NAMES[table], projection, selection, selectionArg, null,
                            null, null, null);
                    break;
                case ADDRESS_ID:
                    id = uri.getPathSegments().get(1);
                    c = db.query(TABLE_NAMES[table], projection, whereWithId(id, selection),
                            selectionArgs, null, null, sortOrder, null);
                    break;
                case ADDRESS:
                    c = db.query(TABLE_NAMES[table], projection, null, null, null, null, sortOrder,
                            null);
                    break;
                case CONTACT_FILTER:
                    return null;
                case EMAIL_FILTER:
                    filter = uri.getLastPathSegment();
                    if (filter == null || filter.length() < 1) {
                        return null;
                    }
                    // Enforce a limit on the number of lookup responses
                    String limitString = uri.getQueryParameter(ContactsContract.LIMIT_PARAM_KEY);
                    if (limitString == null) {
                        limitString = DEFAULT_LOOKUP_LIMIT;
                    }
                    String dbSelection = AddressColumns.EMAIL_ADDRESS + " like ?";
                    String[] dbSelectionArgs = { filter + "%" };
                    c = db.query(TABLE_NAMES[table], EmailAddress.CONTENT_PROJECTION, dbSelection,
                            dbSelectionArgs, null, null, null, limitString);
                    if (c == null) {
                        Logging.d(TAG, "EMAIL_FILTER return null becausse c is null.");
                        return null;
                    }
                    try {
                        return buildResultCursor(projection, c);
                    } finally {
                        c.close();
                    }
                case DIRECTORIES: {
                    android.accounts.Account[] accounts = AccountManager.get(getContext()).getAccounts();
                    MatrixCursor cursor = new MatrixCursor(projection);
                    if (accounts != null && accounts.length > 0) {
                        android.accounts.Account account = accounts[0];
                        Object[] row = new Object[projection.length];
                        for (int i = 0; i < projection.length; i++) {
                            String column = projection[i];
                            if (column.equals(Directory.ACCOUNT_NAME)) {
                                row[i] = account.name;
                            } else if (column.equals(Directory.ACCOUNT_TYPE)) {
                                row[i] = account.type;
                            } else if (column.equals(Directory.TYPE_RESOURCE_ID)) {
                                row[i] = com.android.email.R.string.app_name;
                            } else if (column.equals(Directory.DISPLAY_NAME)) {
                                row[i] = account.name;
                            } else if (column.equals(Directory.EXPORT_SUPPORT)) {
                                row[i] = Directory.EXPORT_SUPPORT_SAME_ACCOUNT_ONLY;
                            } else if (column.equals(Directory.SHORTCUT_SUPPORT)) {
                                row[i] = Directory.SHORTCUT_SUPPORT_NONE;
                            }
                        }
                        cursor.addRow(row);
                    }
                    return cursor;
                }
                default:
                    throw new UnsupportedOperationException("Unknown URI " + uri);
            }
        } catch (SQLiteException e) {
            throw e;
        } finally {
            if (c == null) {
                Logging.e(TAG, "Query returning null for uri: " +
                        uri + ", selection: "+ selection);
            }
        }
        return c;
    }

    /*package*/ Cursor buildResultCursor(String[] projection, Cursor addressCursor) {
        int displayNameIndex = -1;
        int emailIndex = -1;
        int idIndex = -1;
        int lookupIndex = -1;
        int displayNameSource = -1;

        for (int i = 0; i < projection.length; i++) {
            String column = projection[i];
            if (Contacts.DISPLAY_NAME.equals(column) ||
                    Contacts.DISPLAY_NAME_PRIMARY.equals(column)) {
                displayNameIndex = i;
            } else if (CommonDataKinds.Email.ADDRESS.equals(column)) {
                emailIndex = i;
            } else if (Contacts._ID.equals(column)) {
                idIndex = i;
            } else if (Contacts.LOOKUP_KEY.equals(column)) {
                lookupIndex = i;
            } else if (Contacts.DISPLAY_NAME_SOURCE.equals(column)) {
                displayNameSource = i;
            }
        }

        Object[] row = new Object[projection.length];
        MatrixCursor cursor = new MatrixCursor(projection);
        while (addressCursor.moveToNext()) {
            long id = addressCursor.getLong(0);
            String address = addressCursor.getString(1);
            if (displayNameIndex != -1) {
                row[displayNameIndex] = address.split("@")[0];
            }
            if (emailIndex != -1) {
                row[emailIndex] = address;
            }
            if (idIndex != -1) {
                row[idIndex] = id;
            }
            if (lookupIndex != -1) {
                row[lookupIndex] = ContentUris.withAppendedId(EmailAddress.CONTENT_URI, id);
            }
            if (displayNameSource != -1) {
                row[displayNameSource] = DisplayNameSources.STRUCTURED_NAME;
            }
            cursor.addRow(row);
        }
        return cursor;
    }

    private String whereWithId(String id, String selection) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("_id=");
        sb.append(id);
        if (selection != null) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = findMatch(uri, "delete");
        Context context = getContext();
        SQLiteDatabase db = getDatabase(context);
        int table = match >> BASE_SHIFT;
        int result = -1;
        String id = "0";
        switch (match) {
            case ADDRESS_ID:
                id = uri.getPathSegments().get(1);
                result = db.delete(TABLE_NAMES[table], whereWithId(id, selection), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private static int findMatch(Uri uri, String methodName) {
        int match = sURIMatcher.match(uri);
        if (match < 0) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        } else {
            Logging.v(TAG, methodName + ": uri=" + uri + ", match is " + match);
        }
        return match;
    }

    public static Boolean findExistingAddress(Context context, String allowAddress) {
        Boolean isExsit = false;
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(EmailAddress.CONTENT_URI, EmailAddress.CONTENT_PROJECTION, null,
                null, null);
        if (c == null) {
            return false;
        }
        try {
            while (c.moveToNext()) {
                if (allowAddress.equals(c.getString(1))) {
                    isExsit = true;
                    break;
                }
            }
        } finally {
            c.close();
        }
        return isExsit;
    }

}
