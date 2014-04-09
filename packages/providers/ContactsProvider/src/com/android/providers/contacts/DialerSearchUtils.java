package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;

import com.android.internal.telephony.CallerInfo;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;

public class DialerSearchUtils {
    private static final String TAG = "DialerSearchUtils";
    public static String computeNormalizedNumber(String number) {
        String normalizedNumber = null;
        if (number != null) {
            normalizedNumber = PhoneNumberUtils.getStrippedReversed(number);
        }
        return normalizedNumber;
    }
    
    public static String stripSpecialCharInNumberForDialerSearch(String number) {
        if (number == null)
            return null;
        int len = number.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = number.charAt(i);
            if (PhoneNumberUtils.isNonSeparator(c)) {
                sb.append(c);
            } else if (c == ' ' || c == '-' || c == '(' || c == ')') {
                // strip blank and hyphen
            } else {
                /*
                 * Bug fix by Mediatek begin 
                 * CR ID: ALPS00293790 
                 * Description:
                 * Original Code: break;
                 */
                continue;
                /*
                 * Bug fix by Mediatek end
                 */
            }
        }
        return sb.toString();
    }

    public static Cursor queryPhoneLookupByNumber(SQLiteDatabase db, ContactsDatabaseHelper dbHelper,
            String number, String[] projection, String selection, String[] selectionArgs,
            String groupBy, String having, String sortOrder, String limit) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String numberE164 = PhoneNumberUtils.formatNumberToE164(number,
                dbHelper.getCurrentCountryIso());
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
        dbHelper.buildPhoneLookupAndContactQuery(qb, normalizedNumber, numberE164);
        qb.setStrict(true);
        boolean foundResult = false;
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);
        try {
            if (c.getCount() > 0) {
                foundResult = true;
                Cursor exactCursor = findNumberExactMatchedCursor(c, normalizedNumber);
                if (exactCursor != null) {
                    Log.i(TAG, "queryPhoneLookupByNumber: has found the exact number match Contact!");
                    c.close();
                    return exactCursor;
                }
                return c;
            } else {
                qb = new SQLiteQueryBuilder();
                //dbHelper.buildMinimalPhoneLookupAndContactQuery(qb, normalizedNumber);
                // use the raw number instead of the normalized number because
                // phone_number_compare_loose in SQLite works only with non-normalized
                // numbers
                dbHelper.buildFallbackPhoneLookupAndContactQuery(qb, number);
                qb.setStrict(true);
            }
        } finally {
            if (!foundResult) {
                // We'll be returning a different cursor, so close this one.
                c.close();
            }
        }
        return qb.query(db, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);
    }

    // Reference the com.android.phone.CallNotifier.CALL_TYPE_SIP
    // the Call Type of SIP Call
    private static final int CALL_TYPE_SIP = -2;

    public static void updateDialerSearchAfterInsertedCalls(SQLiteDatabase db, ContentValues values, long id,
            ContactsDatabaseHelper dbHelper, Context context) {
        Log.i(TAG, "updateDialerSearch");
        String strInsNumber = values.getAsString(Calls.NUMBER);
        Log.d(TAG, "[insert] get default insert number:" + strInsNumber);

        try {
            db.beginTransaction();
            // Get all same call log id from calls table
            Cursor allCallLogCursorOfSameNum = db.query(Tables.CALLS, new String[] { Calls._ID, Calls.DATE }, " CASE WHEN "
                    + Calls.SIM_ID + "=" + CALL_TYPE_SIP + " THEN " + Calls.NUMBER + "='" + strInsNumber + "'"
                    + " ELSE PHONE_NUMBERS_EQUAL(" + Calls.NUMBER + ", '" + strInsNumber + "') END", null, null, null,
                    "_id DESC", null);

            long updateRowID = -1;
            long latestRowID = -1;
            long allCalledTimes = -1;
            StringBuilder noNamebuilder = new StringBuilder();
            // rowId is new callLog ID, and latestRowID is old callLog ID for the same number.
            if (allCallLogCursorOfSameNum != null && allCallLogCursorOfSameNum.moveToFirst()) {
                allCalledTimes = allCallLogCursorOfSameNum.getCount();
                if (allCallLogCursorOfSameNum.moveToNext()) {
                    latestRowID = allCallLogCursorOfSameNum.getLong(0);
                    noNamebuilder.append(latestRowID);
                    while (allCalledTimes > 2 && allCallLogCursorOfSameNum.moveToNext()) {
                        noNamebuilder.append(",");
                        noNamebuilder.append(allCallLogCursorOfSameNum.getInt(0));
                    }
                }
                allCallLogCursorOfSameNum.close();
                allCallLogCursorOfSameNum = null;
            }

            // Get data_id and raw_contact_id information about contacts
            boolean bIsUriNumber = PhoneNumberUtils.isUriNumber(strInsNumber);
            Cursor nameCursor = null;
            String normalizedNumber = strInsNumber;
            boolean numberCheckFlag = false;
            long dataId = -1;
            long rawContactId = -1;
            boolean bSpecialNumber = (strInsNumber.equals(CallerInfo.UNKNOWN_NUMBER)
                    || strInsNumber.equals(CallerInfo.PRIVATE_NUMBER) || strInsNumber.equals(CallerInfo.PAYPHONE_NUMBER));
            Log.d(TAG, "bIsUriNumber:" + bIsUriNumber + "|bSpecialNumber:" + bSpecialNumber);
            if (bIsUriNumber) {
                // Get internet call number contact information
                nameCursor = db.query(Views.DATA, new String[] { Data._ID, Data.RAW_CONTACT_ID }, Data.DATA1 + "='"
                        + strInsNumber + "' AND " + Data.MIMETYPE + "='" + SipAddress.CONTENT_ITEM_TYPE + "'", null, null,
                        null, null);
            } else {
                // Get non-internet call number contact information
                // Do not strip the special number. Otherwise, UI would not get the right value.
                if (!bSpecialNumber) {
                    normalizedNumber = DialerSearchUtils.stripSpecialCharInNumberForDialerSearch(strInsNumber);
                }
                /*
                 * Use key "lookup" to get right data_id and raw_contact_id. The former one which uses "normalizedNumber" to
                 * search phone_lookup table would cause to get the dirty data.
                 *
                 * The previous code is: nameCursor = getContext().getContentResolver().query(
                 * Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(strInsNumber)), new String[]
                 * {PhoneLookupColumns.DATA_ID, PhoneLookupColumns.RAW_CONTACT_ID}, null, null, null);
                 */
                nameCursor = queryPhoneLookupByNumber(db, dbHelper, strInsNumber, new String[] { PhoneLookupColumns.DATA_ID,
                        PhoneLookupColumns.RAW_CONTACT_ID, Phone.NUMBER}, null, null, null, null, null, null);
            }
            ContentValues contactsValues = new ContentValues();
            if ((!bSpecialNumber) && (null != nameCursor) && (nameCursor.moveToFirst())) {
                numberCheckFlag = true;
                dataId = nameCursor.getLong(0);
                rawContactId = nameCursor.getLong(1);
                // Put the data_id and raw_contact_id into copiedValues to insert
                contactsValues.put(Calls.DATA_ID, dataId);
                contactsValues.put(Calls.RAW_CONTACT_ID, rawContactId);
            }
            if (null != nameCursor) {
                nameCursor.close();
            }

            // rowId is new callLog ID, and latestRowID is old callLog ID for the same number.
            Log.d(TAG, "insert into calls table");
            long rowId = id;
            if (contactsValues.size() > 0) {
                getDatabaseModifier(db, Tables.CALLS, context)
                        .update(Tables.CALLS, contactsValues, "Calls._ID =" + id, null);
            }
            Log.d(TAG, "inserted into calls table. new rowId:" + rowId + "|dataId:" + dataId + "|rawContactId"
                    + rawContactId);

            if (FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
                if (updateRowID == -1) {
                    updateRowID = rowId;
                }
                Log.d(TAG, "[insert] insert updateRowID:" + updateRowID + " latestRowID:" + latestRowID + " rowId:" + rowId);
            }
            if (rowId > 0 && FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
                ContentValues updateNameLookupValues = new ContentValues();
                updateNameLookupValues.put(DialerSearchLookupColumns.CALL_LOG_ID, rowId);
                if (numberCheckFlag) {
                    /*
                     * update old NO Name CallLog records that share the same number with the new inserted one, if exist.
                     * String updateNoNameCallLogStmt = Calls.DATA_ID + " IS NULL " + " AND PHONE_NUMBERS_EQUAL(" +
                     * Calls.NUMBER + ",'" + number + "') ";
                     *
                     * update All CallLog records that share the same number with the new inserted one, if exist.
                     */
                    if (noNamebuilder != null && noNamebuilder.length() > 0) {
                        // update NO Name CallLog records of the inserted CallLog
                        Log.d(TAG, "[insert]updated calls record. number:" + strInsNumber + " data_id:" + dataId
                                + " raw_contact_id:" + rawContactId);
                        ContentValues updateNoNameCallLogValues = new ContentValues();
                        updateNoNameCallLogValues.put(Calls.RAW_CONTACT_ID, rawContactId);
                        updateNoNameCallLogValues.put(Calls.DATA_ID, dataId);
                        int updateNoNameCallLogCount = db.update(Tables.CALLS, updateNoNameCallLogValues, Calls._ID
                                + " IN (" + noNamebuilder.toString() + ")", null);
                        Log.d(TAG, "[insert]updated NO Name CallLog records of the inserted CallLog. Count:"
                                + updateNoNameCallLogCount);

                        // delete No Name CallLog records in dialer search table, if exists.
                        Log.d(TAG, "[insert] delete No Name CallLog records:" + noNamebuilder.toString() + " Except:"
                                + latestRowID);
                        String deleteNoNameCallLogInDs = "(" + DialerSearchLookupColumns.CALL_LOG_ID + " IN ("
                                + noNamebuilder.toString() + ") " + "AND " + DialerSearchLookupColumns.NAME_TYPE + " = "
                                + DialerSearchLookupType.NO_NAME_CALL_LOG + " AND "
                                + DialerSearchLookupColumns.RAW_CONTACT_ID + " < 0 " + " AND "
                                + DialerSearchLookupColumns.DATA_ID + " < 0 )";
                        int deleteNoNameCallLogCount = db.delete(Tables.DIALER_SEARCH, deleteNoNameCallLogInDs, null);
                        Log.d(TAG, "[insert] deleted No Name CallLog records in dialer search table. Count:"
                                + deleteNoNameCallLogCount);
                    }

                    // update dialer search table.
                    Log.d(TAG, "[insert]query dialer_search. ");
                    String updateNameCallLogStmt = "(" + DialerSearchLookupColumns.RAW_CONTACT_ID + " = " + rawContactId
                            + " AND " + DialerSearchLookupColumns.NAME_TYPE + " = 11)" + " OR ("
                            + DialerSearchLookupColumns.DATA_ID + " = " + dataId + " AND "
                            + DialerSearchLookupColumns.NAME_TYPE + " = 8)";
                    int updateDialerSearchCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValues,
                            updateNameCallLogStmt, null);
                    Log.d(TAG, "[insert]update dialer_search table. updateDialerSearchCount:" + updateDialerSearchCount);

                    // if the new a call log with new contact id but the same number
                    // change the original call_log_id as
                    updateNameLookupValues.put(DialerSearchLookupColumns.CALL_LOG_ID, 0);
                    int upDialCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValues,
                            DialerSearchLookupColumns.CALL_LOG_ID + " = " + latestRowID, null);
                    Log.d(TAG, "[insert]update dialer_search table. updateDialerSearchCount:" + upDialCount);
                } else {
                    Log.d(TAG, "[insert]cursor nameCursor donot have data.");
                    if (allCalledTimes > 1) {
                        // if (latestRowID != -1) {
                        Log.d(TAG, "[insert] update NO NAME RECORD.");
                        updateNameLookupValues.put(DialerSearchLookupColumns.DATA_ID, -updateRowID);
                        updateNameLookupValues.put(DialerSearchLookupColumns.RAW_CONTACT_ID, -updateRowID);
                        updateNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME, normalizedNumber);
                        updateNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE, normalizedNumber);
                        int updateDialerSearchCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValues,
                                DialerSearchLookupColumns.CALL_LOG_ID + " = " + latestRowID, null);
                        Log.d(TAG, "[insert]update dialer_search table. updateDialerSearchCount:" + updateDialerSearchCount);
                    } else if (allCalledTimes == 1) {
                        Log.d(TAG, "[insert]**nameLookupCursor is null");
                        ContentValues insertNameLookupValues = new ContentValues();
                        insertNameLookupValues.put(DialerSearchLookupColumns.CALL_LOG_ID, updateRowID);
                        insertNameLookupValues.put(DialerSearchLookupColumns.NAME_TYPE,
                                DialerSearchLookupType.NO_NAME_CALL_LOG);
                        insertNameLookupValues.put(DialerSearchLookupColumns.DATA_ID, -updateRowID);
                        insertNameLookupValues.put(DialerSearchLookupColumns.RAW_CONTACT_ID, -updateRowID);
                        insertNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME, normalizedNumber);
                        insertNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE, normalizedNumber);
                        long insertDialerSearch = db.insert(Tables.DIALER_SEARCH, null, insertNameLookupValues);
                        Log.d(TAG, "[insert]insert dialer_search table. insertDialerSearch:" + insertDialerSearch);
                    }
                }
            }
            if (rowId > 0) {
                notifyDialerSearchChange(context);
                // retUri = ContentUris.withAppendedId(uri, rowId);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static int updateDialerSearchBeforeDeleteCalls(SQLiteDatabase db, Context context, String selection,
            String[] selectionArgs) {
        SQLiteStatement updateForCallLogUpdated = null;
        SQLiteStatement updateForNoNameCallLogDeleted = null;
        SQLiteStatement deleteForCallLogDeleted = null;
        int count = 0;
        if (FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
            /*
             * update name_lookup for usage of dialer search:
             */
            if (selection == null) { // delete all call logs
                Log.d(TAG, "[delete] Selection is null, delete all Call logs.");
                int deleteCount = db.delete(Tables.DIALER_SEARCH, DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND "
                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " <=0 ", null);
                Log.d(TAG, "[delete] delete from Dialer_Search Count: " + deleteCount);
                ContentValues updateNameLookupValue = new ContentValues();
                updateNameLookupValue.put(DialerSearchLookupColumns.CALL_LOG_ID, 0);
                int updateCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValue,
                        DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " >0 ", null);
                Log.d(TAG, "[delete] update from Dialer_Search Count: " + updateCount);
                // count = getDatabaseModifier(db).delete(Tables.CALLS,
                // selectionBuilder.build(), selectionArgs);
            } else {
                db.beginTransaction();
                try {
                    Log.d(TAG, "[delete] delete calls selection: " + selection);
                    Cursor delCursor = db.query(true, Tables.CALLS, new String[] { Calls._ID, Calls.NUMBER,
                            Calls.RAW_CONTACT_ID, Calls.DATA_ID }, selection, selectionArgs, "data_id, _id", null, null,
                            null);
                    Cursor allCallLogs = db.query(true, Tables.CALLS, new String[] { Calls._ID, Calls.DATA_ID }, null, null,
                            null, null, null, null);
                    int allCount = allCallLogs == null ? 0 : allCallLogs.getCount();
                    if (delCursor != null && delCursor.getCount() == allCount) {
                        int deleteCount = db.delete(Tables.DIALER_SEARCH, DialerSearchLookupColumns.CALL_LOG_ID
                                + " > 0 AND " + DialerSearchLookupColumns.RAW_CONTACT_ID + " <=0 ", null);
                        Log.d(TAG, "[delete] delete from Dialer_Search Count: " + deleteCount);
                        ContentValues updateNameLookupValue = new ContentValues();
                        updateNameLookupValue.put(DialerSearchLookupColumns.CALL_LOG_ID, 0);
                        int updateCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValue,
                                DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND "
                                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " >0 ", null);
                        Log.d(TAG, "[delete] update from Dialer_Search Count: " + updateCount);
                        // count = getDatabaseModifier(db).delete(Tables.CALLS,
                        // selectionBuilder.build(), selectionArgs);
                    } else if (delCursor != null && delCursor.getCount() > 0) {

                        db.execSQL("DROP TABLE IF EXISTS delCallLog");
                        if (selectionArgs != null && selectionArgs.length > 0) {
                            db.execSQL(" CREATE TEMP TABLE delCallLog AS SELECT " + "_id, number, data_id, raw_contact_id"
                                    + " FROM calls WHERE " + selection, selectionArgs);
                        } else {
                            db.execSQL(" CREATE TEMP TABLE delCallLog AS SELECT " + "_id, number, data_id, raw_contact_id"
                                    + " FROM calls WHERE " + selection);
                        }
                        // count = getDatabaseModifier(db).delete(Tables.CALLS,
                        // selectionBuilder.build(), selectionArgs);

                        String queryStr = "SELECT "
                                + "delCallLog._id as _id, "
                                + "delCallLog.number as delNumber, "
                                + "delCallLog.data_id as delDataId, "
                                + "delCallLog.raw_contact_id as delRawId, "
                                + "calls._id as newId, "
                                + "calls.number as newNumber, "
                                + "calls.data_id as newDataId, "
                                + "calls.raw_contact_id as newRawId "
                                + " FROM delCallLog "
                                + " LEFT JOIN calls "
                                + " on case when delCallLog.data_id is null then PHONE_NUMBERS_EQUAL(delCallLog.number, calls.number) "
                                + " else delCallLog.data_id = calls.data_id "
                                + " end and delCallLog._id != calls._id GROUP BY delCallLog._id";
                        Cursor updateCursor = db.rawQuery(queryStr, null);
                        if (updateCursor != null) {
                            while (updateCursor.moveToNext()) {
                                long delCallId = updateCursor.getLong(0);
                                long delDataId = updateCursor.getLong(2);
                                long newCallId = updateCursor.getLong(4);
                                if (delDataId > 0) {
                                    if (updateForCallLogUpdated == null) {
                                        updateForCallLogUpdated = db.compileStatement(" UPDATE " + Tables.DIALER_SEARCH
                                                + " SET " + DialerSearchLookupColumns.CALL_LOG_ID + "=? " + " WHERE "
                                                + DialerSearchLookupColumns.CALL_LOG_ID + "=? ");
                                    }
                                    // named call log
                                    if (newCallId != delCallId && newCallId > 0) {
                                        updateForCallLogUpdated.bindLong(1, newCallId);
                                        updateForCallLogUpdated.bindLong(2, delCallId);
                                    } else if (newCallId <= 0) {
                                        updateForCallLogUpdated.bindLong(1, 0);
                                        updateForCallLogUpdated.bindLong(2, delCallId);
                                    }
                                    updateForCallLogUpdated.execute();
                                } else {
                                    // no name call log
                                    if (newCallId > 0) {
                                        // update new call log
                                        if (newCallId != delCallId) {
                                            if (updateForNoNameCallLogDeleted == null) {
                                                updateForNoNameCallLogDeleted = db.compileStatement(" UPDATE "
                                                        + Tables.DIALER_SEARCH + " SET " + DialerSearchLookupColumns.DATA_ID
                                                        + "=?, " + DialerSearchLookupColumns.RAW_CONTACT_ID + "=?, "
                                                        + DialerSearchLookupColumns.CALL_LOG_ID + "=? " + " WHERE "
                                                        + DialerSearchLookupColumns.CALL_LOG_ID + "=? ");
                                            }
                                            updateForNoNameCallLogDeleted.bindLong(1, -newCallId);
                                            updateForNoNameCallLogDeleted.bindLong(2, -newCallId);
                                            updateForNoNameCallLogDeleted.bindLong(3, newCallId);
                                            updateForNoNameCallLogDeleted.bindLong(4, delCallId);
                                            updateForNoNameCallLogDeleted.execute();
                                        }
                                    } else {
                                        if (deleteForCallLogDeleted == null) {
                                            deleteForCallLogDeleted = db.compileStatement("DELETE FROM "
                                                    + Tables.DIALER_SEARCH + " WHERE "
                                                    + DialerSearchLookupColumns.CALL_LOG_ID + " =? " + " AND "
                                                    + DialerSearchLookupColumns.NAME_TYPE + " = "
                                                    + DialerSearchLookupType.PHONE_EXACT);
                                        }
                                        // delete from dialer search table
                                        deleteForCallLogDeleted.bindLong(1, delCallId);
                                        deleteForCallLogDeleted.execute();
                                    }
                                }
                            }
                            updateCursor.close();
                        }
                        db.execSQL("DROP TABLE IF EXISTS delCallLog");
                    }
                    if (delCursor != null) {
                        delCursor.close();
                    }
                    if (allCallLogs != null)
                        allCallLogs.close();
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } else {
            // count = getDatabaseModifier(db).delete(Tables.CALLS,
            // selectionBuilder.build(), selectionArgs);
        }
        Log.d(TAG, "[delete] delete Calls. count: " + count);
        // if (count > 0) {
        notifyDialerSearchChange(context);
        // }
        return count;
    }

    private static void notifyDialerSearchChange(Context context) {
        context.getContentResolver().notifyChange(Uri.parse("content://com.android.contacts.dialer_search/callLog/"), null,
                false);
    }

    private static DatabaseModifier getDatabaseModifier(SQLiteDatabase db, String tableName, Context context) {
        return new DbModifierWithNotification(tableName, db, context);
    }

    /**
     * To find out the exactly number which has a same normalizedNumber with the input number.
     * @param cursor
     *        origin Cursor
     * @param normalizedNumber
     *        the input number's normalizedNumber
     * @return
     *        the exactly number matched contacts cursor
     */
    public static Cursor findNumberExactMatchedCursor(Cursor cursor, String normalizedNumber) {
        /**
         * M: [ALPS00565568]we need to find out the right "Phone.NUMBER" in case there are more than record in the cursor.
         * the cursor which would be handled must contain "Phone.NUMBER" field && count > 1 && normalizedNumber is valid.
         * otherwise, we would do nothing and return null.
         */
        if (cursor == null || cursor.getColumnIndex(Phone.NUMBER) < 0 || cursor.getCount() <= 1
                || TextUtils.isEmpty(normalizedNumber)) {
            Log.i(TAG, "findNumberExactMatchedCursor: did not match the filter rule!");
            return null;
        }
        String data1 = null;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            data1 = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
            data1 = PhoneNumberUtils.normalizeNumber(data1);
            if (normalizedNumber.equals(data1)) {
                MatrixCursor exactCursor = matrixCursorFromCursorRow(cursor, cursor.getPosition());
                cursor.close();
                return exactCursor;
            }
            cursor.moveToNext();
        }
        return null;
    }

    /**
     * Copy one cursor row to another cursor which has the same value.
     *
     * @param cursor
     *            the src cursor
     * @param index
     *            the row to convert
     * @return the new cursor with a single row
     */
    private static MatrixCursor matrixCursorFromCursorRow(Cursor cursor, int index) {
        MatrixCursor newCursor = new MatrixCursor(cursor.getColumnNames(), 1);
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        if (-1 < index && index < cursor.getCount()) {
            cursor.moveToPosition(index);
        }
        for (int i = 0; i < numColumns; i++) {
            data[i] = cursor.getString(i);
        }
        newCursor.addRow(data);
        return newCursor;
    }
}
