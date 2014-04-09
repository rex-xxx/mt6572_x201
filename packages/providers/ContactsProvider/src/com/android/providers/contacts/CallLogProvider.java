/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.providers.contacts;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Process;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DialerSearch;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;

import android.provider.ContactsContract.RawContacts;
import android.app.SearchManager;

import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import static com.android.providers.contacts.util.DbQueryUtils.checkForSupportedColumns;
import static com.android.providers.contacts.util.DbQueryUtils.getEqualityClause;
import static com.android.providers.contacts.util.DbQueryUtils.getInequalityClause;

import com.android.internal.telephony.CallerInfo;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.SearchIndexColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.util.SelectionBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.mediatek.providers.contacts.ContactsFeatureConstants;
import com.mediatek.providers.contacts.VvmUtils;
/**
 * Call log content provider.
 */
public class CallLogProvider extends ContentProvider {
    /** Selection clause to use to exclude voicemail records.  */
    private static final String EXCLUDE_VOICEMAIL_SELECTION = getInequalityClause(
            Calls.TYPE, Calls.VOICEMAIL_TYPE);

    private static final int CALLS = 1;

    private static final int CALLS_ID = 2;

    private static final int CALLS_FILTER = 3;

    private static final int CALLS_SEARCH_FILTER = 4;
    private static final int CALLS_JION_DATA_VIEW = 5;
    private static final int CALLS_JION_DATA_VIEW_ID = 6;
    private static final int SEARCH_SUGGESTIONS = 10001;
    private static final int SEARCH_SHORTCUT = 10002;
    private CallLogSearchSupport mCallLogSearchSupport;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls", CALLS);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/#", CALLS_ID);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/filter/*", CALLS_FILTER);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/search_filter/*", CALLS_SEARCH_FILTER);
        sURIMatcher.addURI(CallLog.AUTHORITY, "callsjoindataview", CALLS_JION_DATA_VIEW);
        sURIMatcher.addURI(CallLog.AUTHORITY, "callsjoindataview/#", CALLS_JION_DATA_VIEW_ID);
        sURIMatcher.addURI(CallLog.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGESTIONS);
        sURIMatcher.addURI(CallLog.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGESTIONS);
        sURIMatcher.addURI(CallLog.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SEARCH_SHORTCUT);
    }

    private static final HashMap<String, String> sCallsProjectionMap;
    static {

        // Calls projection map
        sCallsProjectionMap = new HashMap<String, String>();
        sCallsProjectionMap.put(Calls._ID, Tables.CALLS + "._id as " + Calls._ID);
//        sCallsProjectionMap.put(Calls._ID, Calls._ID);
        sCallsProjectionMap.put(Calls.NUMBER, Calls.NUMBER);
        sCallsProjectionMap.put(Calls.DATE, Calls.DATE);
        sCallsProjectionMap.put(Calls.DURATION, Calls.DURATION);
        sCallsProjectionMap.put(Calls.TYPE, Calls.TYPE);
        sCallsProjectionMap.put(Calls.NEW, Calls.NEW);
        sCallsProjectionMap.put(Calls.VOICEMAIL_URI, Calls.VOICEMAIL_URI);
        sCallsProjectionMap.put(Calls.IS_READ, Calls.IS_READ);
        sCallsProjectionMap.put(Calls.CACHED_NAME, Calls.CACHED_NAME);
        sCallsProjectionMap.put(Calls.CACHED_NUMBER_TYPE, Calls.CACHED_NUMBER_TYPE);
        sCallsProjectionMap.put(Calls.CACHED_NUMBER_LABEL, Calls.CACHED_NUMBER_LABEL);
        sCallsProjectionMap.put(Calls.COUNTRY_ISO, Calls.COUNTRY_ISO);
        sCallsProjectionMap.put(Calls.GEOCODED_LOCATION, Calls.GEOCODED_LOCATION);
        sCallsProjectionMap.put(Calls.CACHED_LOOKUP_URI, Calls.CACHED_LOOKUP_URI);
        sCallsProjectionMap.put(Calls.CACHED_MATCHED_NUMBER, Calls.CACHED_MATCHED_NUMBER);
        sCallsProjectionMap.put(Calls.CACHED_NORMALIZED_NUMBER, Calls.CACHED_NORMALIZED_NUMBER);
        sCallsProjectionMap.put(Calls.CACHED_PHOTO_ID, Calls.CACHED_PHOTO_ID);
        sCallsProjectionMap.put(Calls.CACHED_FORMATTED_NUMBER, Calls.CACHED_FORMATTED_NUMBER);

        /// M: @{
        sCallsProjectionMap.put(Calls.SIM_ID, Calls.SIM_ID);
        sCallsProjectionMap.put(Calls.VTCALL, Calls.VTCALL);
        sCallsProjectionMap.put(Calls.RAW_CONTACT_ID, Calls.RAW_CONTACT_ID);
        sCallsProjectionMap.put(Calls.DATA_ID, Calls.DATA_ID);
        sCallsProjectionMap.put(Calls.IP_PREFIX, Calls.IP_PREFIX);
        // @}
    }

    /// M: @{
    private static final String mstableCallsJoinData = Tables.CALLS + " LEFT JOIN " 
    + " (SELECT * FROM " +  Views.DATA + " WHERE " + Data._ID + " IN "
    + "(SELECT " +  Calls.DATA_ID + " FROM " + Tables.CALLS + ")) AS " + Views.DATA
            + " ON(" + Tables.CALLS + "." + Calls.DATA_ID + " = " + Views.DATA + "." + Data._ID + ")";

    // Must match the definition in CallLogQuery - begin.
    private static final String CALL_NUMBER_TYPE = "calllognumbertype";
    private static final String CALL_NUMBER_TYPE_ID = "calllognumbertypeid";
    // Must match the definition in CallLogQuery - end.

    private static final HashMap<String, String> sCallsJoinDataViewProjectionMap;
    static {
        // Calls Join view_data projection map
        sCallsJoinDataViewProjectionMap = new HashMap<String, String>();
        sCallsJoinDataViewProjectionMap.put(Calls._ID, Tables.CALLS + "._id as " + Calls._ID);
        sCallsJoinDataViewProjectionMap.put(Calls.NUMBER, Calls.NUMBER);
        sCallsJoinDataViewProjectionMap.put(Calls.DATE, Calls.DATE);
        sCallsJoinDataViewProjectionMap.put(Calls.DURATION, Calls.DURATION);
        sCallsJoinDataViewProjectionMap.put(Calls.TYPE, Calls.TYPE);
        sCallsJoinDataViewProjectionMap.put(Calls.VOICEMAIL_URI, Calls.VOICEMAIL_URI);
        sCallsJoinDataViewProjectionMap.put(Calls.COUNTRY_ISO, Calls.COUNTRY_ISO);
        sCallsJoinDataViewProjectionMap.put(Calls.GEOCODED_LOCATION, Calls.GEOCODED_LOCATION);
        sCallsJoinDataViewProjectionMap.put(Calls.IS_READ, Calls.IS_READ);

        sCallsJoinDataViewProjectionMap.put(Calls.SIM_ID, Calls.SIM_ID);
        sCallsJoinDataViewProjectionMap.put(Calls.VTCALL, Calls.VTCALL);
        sCallsJoinDataViewProjectionMap.put(Calls.RAW_CONTACT_ID, Tables.CALLS + "." + Calls.RAW_CONTACT_ID + " AS " + Calls.RAW_CONTACT_ID);
        sCallsJoinDataViewProjectionMap.put(Calls.DATA_ID, Calls.DATA_ID);

        sCallsJoinDataViewProjectionMap.put(Contacts.DISPLAY_NAME, 
                Views.DATA + "." + Contacts.DISPLAY_NAME + " AS " + Contacts.DISPLAY_NAME);
        sCallsJoinDataViewProjectionMap.put(CALL_NUMBER_TYPE_ID,
                Views.DATA + "." + Data.DATA2 + " AS " + CALL_NUMBER_TYPE_ID);
        sCallsJoinDataViewProjectionMap.put(CALL_NUMBER_TYPE,
                Views.DATA + "." + Data.DATA3 + " AS " + CALL_NUMBER_TYPE);
        sCallsJoinDataViewProjectionMap.put(Data.PHOTO_ID, Views.DATA + "." + Data.PHOTO_ID + " AS " + Data.PHOTO_ID);
        sCallsJoinDataViewProjectionMap.put(RawContacts.INDICATE_PHONE_SIM, RawContacts.INDICATE_PHONE_SIM);
        sCallsJoinDataViewProjectionMap.put(RawContacts.IS_SDN_CONTACT, RawContacts.IS_SDN_CONTACT);  // add by MTK
        sCallsJoinDataViewProjectionMap.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);
        sCallsJoinDataViewProjectionMap.put(Contacts.LOOKUP_KEY, Views.DATA + "." + Contacts.LOOKUP_KEY + " AS " + Contacts.LOOKUP_KEY);
        sCallsJoinDataViewProjectionMap.put(Data.PHOTO_URI, Views.DATA + "." + Data.PHOTO_URI + " AS " + Data.PHOTO_URI);
        sCallsJoinDataViewProjectionMap.put(Calls.IP_PREFIX, Calls.IP_PREFIX);
    }
    /// @}

    private ContactsDatabaseHelper mDbHelper;
    private DatabaseUtils.InsertHelper mCallsInserter;
    private boolean mUseStrictPhoneNumberComparation;
    private VoicemailPermissions mVoicemailPermissions;
    private CallLogInsertionHelper mCallLogInsertionHelper;

    @Override
    public boolean onCreate() {
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "CallLogProvider.onCreate start");
        }
        final Context context = getContext();
        mDbHelper = getDatabaseHelper(context);
        mUseStrictPhoneNumberComparation =
            context.getResources().getBoolean(
                    com.android.internal.R.bool.config_use_strict_phone_number_comparation);
        mVoicemailPermissions = new VoicemailPermissions(context);
        mCallLogInsertionHelper = createCallLogInsertionHelper(context);
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "CallLogProvider.onCreate finish");
        }
        
        /// M: @{
        mCallLogSearchSupport = new CallLogSearchSupport(this);
        //when calllog provider is created, notify the missed call number
        Log.i(TAG, "onCreate(), notifyNewCallsCount");
        notifyNewCallsCount(mDbHelper.getReadableDatabase(), context);
        /// @}
        return true;
    }

    @VisibleForTesting
    protected CallLogInsertionHelper createCallLogInsertionHelper(final Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    @VisibleForTesting
    protected ContactsDatabaseHelper getDatabaseHelper(final Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }
    
    /**
     * Gets the value of the "limit" URI query parameter.
     *
     * @return A string containing a non-negative integer, or <code>null</code> if
     *         the parameter is not set, or is set to an invalid value.
     */
    private String getLimit(Uri uri) {
        String limitParam = ContactsProvider2.getQueryParameter(uri, "limit");
        if (limitParam == null) {
            return null;
        }
        // make sure that the limit is a non-negative integer
        try {
            int l = Integer.parseInt(limitParam);
            if (l < 0) {
                Log.w(TAG, "Invalid limit parameter: " + limitParam);
                return null;
            }
            return String.valueOf(l);
        } catch (NumberFormatException ex) {
            Log.w(TAG, "Invalid limit parameter: " + limitParam);
            return null;
        }
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(Tables.CALLS);
        qb.setProjectionMap(sCallsProjectionMap);
        qb.setStrict(true);

        final SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder);

        final int match = sURIMatcher.match(uri);
        /// M: @{
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String groupBy = null;
        Log.i(TAG,"match == " +match);
        /// @}
        switch (match) {
            case CALLS:
                break;

            case CALLS_ID: {
                selectionBuilder.addClause(getEqualityClause(Calls._ID,
                        parseCallIdFromUri(uri)));
                break;
            }

            case CALLS_FILTER: {
                String phoneNumber = uri.getPathSegments().get(2);
                qb.appendWhere("PHONE_NUMBERS_EQUAL(number, ");
                qb.appendWhereEscapeString(phoneNumber);
                qb.appendWhere(mUseStrictPhoneNumberComparation ? ", 1)" : ", 0)");
                break;
            }
            /// M: @{
            case CALLS_SEARCH_FILTER: {
                String query = uri.getPathSegments().get(2);
                String nomalizeName = NameNormalizer.normalize(query);
                final String SNIPPET_CONTACT_ID = "snippet_contact_id";
                String table = Tables.CALLS
                + " LEFT JOIN " + Views.DATA 
                    + " ON (" + Views.DATA + "." + Data._ID
                        + "=" + Tables.CALLS + "." + Calls.DATA_ID + ")" 
                + " LEFT JOIN (SELECT " + SearchIndexColumns.CONTACT_ID + " AS " + SNIPPET_CONTACT_ID
                    + " FROM " + Tables.SEARCH_INDEX
                    + " WHERE " + SearchIndexColumns.NAME + " MATCH '*" + nomalizeName + "*') "
                    + " ON (" + SNIPPET_CONTACT_ID
                        + "=" + Views.DATA + "." + Data.CONTACT_ID + ")";

                qb.setTables(table);
                qb.setProjectionMap(sCallsJoinDataViewProjectionMap);

                StringBuilder sb = new StringBuilder();
                sb.append(Tables.CALLS + "." + Calls.NUMBER + " GLOB '*");
                sb.append(query);
                sb.append("*' OR (" + SNIPPET_CONTACT_ID + ">0 AND " + Tables.CALLS + "." + Calls.RAW_CONTACT_ID + ">0) ");
                qb.appendWhere(sb);
                groupBy = Tables.CALLS + "." + Calls._ID;

                Log.d(TAG,
                        " CallLogProvider.CALLS_SEARCH_FILTER, table=" + table + ", query=" + query + ", sb="
                                + sb.toString());
                break;
            }

            case CALLS_JION_DATA_VIEW: {
                qb.setTables(mstableCallsJoinData);
                qb.setProjectionMap(sCallsJoinDataViewProjectionMap);
                qb.setStrict(true);
                break;
            }

            case CALLS_JION_DATA_VIEW_ID: {
                qb.setTables(mstableCallsJoinData);
                qb.setProjectionMap(sCallsJoinDataViewProjectionMap);
                qb.setStrict(true);
                selectionBuilder.addClause(getEqualityClause(Tables.CALLS + "." +Calls._ID,
                        parseCallIdFromUri(uri)));
                break;
            }

            case SEARCH_SUGGESTIONS: {
                Log.d(TAG, "CallLogProvider.SEARCH_SUGGESTIONS");
                return mCallLogSearchSupport.handleSearchSuggestionsQuery(db, uri, getLimit(uri));
            }
            
            case SEARCH_SHORTCUT: {
                Log.d(TAG,"CallLogProvider.SEARCH_SHORTCUT. Uri:" + uri);
                String callId = uri.getLastPathSegment();
                String filter = uri.getQueryParameter(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
                return mCallLogSearchSupport.handleSearchShortcutRefresh(db, projection, callId, filter);
            }
            /// @}

            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
        Log.d(TAG, "   In call log providers,  selectionBuilder="+selectionBuilder.build());

        final int limit = getIntParam(uri, Calls.LIMIT_PARAM_KEY, 0);
        final int offset = getIntParam(uri, Calls.OFFSET_PARAM_KEY, 0);
        String limitClause = null;
        if (limit > 0) {
            limitClause = offset + "," + limit;
        }

        final Cursor c = qb.query(db, projection, selectionBuilder.build(), selectionArgs, groupBy, null, sortOrder,
                limitClause);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), CallLog.CONTENT_URI);
            Log.d(TAG, "query count == " +c.getCount());
        }
        return c;
    }

    /**
     * Gets an integer query parameter from a given uri.
     *
     * @param uri The uri to extract the query parameter from.
     * @param key The query parameter key.
     * @param defaultValue A default value to return if the query parameter does not exist.
     * @return The value from the query parameter in the Uri.  Or the default value if the parameter
     * does not exist in the uri.
     * @throws IllegalArgumentException when the value in the query parameter is not an integer.
     */
    private int getIntParam(Uri uri, String key, int defaultValue) {
        String valueString = uri.getQueryParameter(key);
        if (valueString == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            String msg = "Integer required for " + key + " parameter but value '" + valueString +
                    "' was found instead.";
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case CALLS:
                return Calls.CONTENT_TYPE;
            case CALLS_ID:
                return Calls.CONTENT_ITEM_TYPE;
            case CALLS_FILTER:
                return Calls.CONTENT_TYPE;
            /// M: @{
            case CALLS_SEARCH_FILTER:
                return Calls.CONTENT_TYPE;
            case SEARCH_SUGGESTIONS:
                return Calls.CONTENT_TYPE;
            /// @}
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /// M: @{
        int prio = Process.getThreadPriority(Process.myTid());
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
        Uri retUri = null;
        long lStart = System.currentTimeMillis();
        log("insert() + ===========");
        /// @}
        checkForSupportedColumns(sCallsProjectionMap, values);
        // Inserting a voicemail record through call_log requires the voicemail
        // permission and also requires the additional voicemail param set.
        if (hasVoicemailValue(values)) {
            checkIsAllowVoicemailRequest(uri);
            mVoicemailPermissions.checkCallerHasFullAccess();
        }
        if (mCallsInserter == null) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            mCallsInserter = new DatabaseUtils.InsertHelper(db, Tables.CALLS);
        }

        ContentValues copiedValues = new ContentValues(values);

        // Add the computed fields to the copied values.
        mCallLogInsertionHelper.addComputedValues(copiedValues);

        /**
         * M:
         *  
         * Original Android code 
         * long rowId = getDatabaseModifier(mCallsInserter).insert(copiedValues); 
         * if (rowId > 0) { 
         *     return ContentUris.withAppendedId(uri, rowId); 
         * } 
         * @{
         */

        log("[insert]uri: " + uri);
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
        } catch (SQLiteDiskIOException err) {
            err.printStackTrace();
            log("insert()- 1 =========== Time:" + (System.currentTimeMillis() - lStart));
            return null;
        }

        String strInsNumber = values.getAsString(Calls.NUMBER);
        log("[insert] get default insert number:" + strInsNumber);
        
        try {
            db.beginTransaction();
            // Get all same call log id from calls table 
            Cursor allCallLogCursorOfSameNum = db.query(Tables.CALLS,
                    new String[] { Calls._ID, Calls.DATE }, 
                    " CASE WHEN " + Calls.SIM_ID + "=" + CALL_TYPE_SIP 
                    + " THEN " + Calls.NUMBER + "='" + strInsNumber + "'"
                    + " ELSE PHONE_NUMBERS_EQUAL(" + Calls.NUMBER + ", '" + strInsNumber + "') END", 
                    null, null, null, "_id DESC", null);
    
            long updateRowID = -1;
            long latestRowID = -1;
            StringBuilder noNamebuilder = new StringBuilder();
            if (allCallLogCursorOfSameNum != null) {
                if (allCallLogCursorOfSameNum.moveToFirst()) {
                    latestRowID = allCallLogCursorOfSameNum.getLong(0);
                    noNamebuilder.append(latestRowID);
                }
                while (allCallLogCursorOfSameNum.moveToNext()) {
                    noNamebuilder.append(",");
                    noNamebuilder.append(allCallLogCursorOfSameNum.getInt(0));
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
                    || strInsNumber.equals(CallerInfo.PRIVATE_NUMBER)
                    || strInsNumber.equals(CallerInfo.PAYPHONE_NUMBER));
            log("bIsUriNumber:" + bIsUriNumber + "|bSpecialNumber:" + bSpecialNumber);
            if (bIsUriNumber) {
                // Get internet call number contact information
                nameCursor = db.query(Views.DATA, new String[] { Data._ID,
                        Data.RAW_CONTACT_ID }, Data.DATA1 + "='" + strInsNumber
                        + "' AND " + Data.MIMETYPE + "='" + SipAddress.CONTENT_ITEM_TYPE + "'", 
                        null, null, null, null);
            } else {
                // Get non-internet call number contact information
                //Do not strip the special number. Otherwise, UI would not get the right value.
                if (!bSpecialNumber) {
                    normalizedNumber = DialerSearchUtils.stripSpecialCharInNumberForDialerSearch(strInsNumber);
                }
                /*
                 * Use key "lookup" to get right data_id and raw_contact_id. 
                 * The former one which uses "normalizedNumber" to search 
                 * phone_lookup table would cause to get the dirty data.
                 * 
                 * The previous code is:
                 *   nameCursor = getContext().getContentResolver().query(
                 *           Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(strInsNumber)),
                 *           new String[] {PhoneLookupColumns.DATA_ID, PhoneLookupColumns.RAW_CONTACT_ID},
                 *           null, null, null);
                 */
                nameCursor = DialerSearchUtils.queryPhoneLookupByNumber(db, mDbHelper,
                        strInsNumber, new String[] {
                                PhoneLookupColumns.DATA_ID, PhoneLookupColumns.RAW_CONTACT_ID, Phone.NUMBER
                        }, null, null, null, null, null, null);
            }
            if ((!bSpecialNumber) && (null != nameCursor) && (nameCursor.moveToFirst())) {
                numberCheckFlag = true;
                dataId = nameCursor.getLong(0);
                rawContactId = nameCursor.getLong(1);
                // Put the data_id and raw_contact_id into copiedValues to insert
                copiedValues.put(Calls.DATA_ID, dataId);
                copiedValues.put(Calls.RAW_CONTACT_ID, rawContactId);
            }
            if (null != nameCursor) {
                nameCursor.close();
            }

            // rowId is new callLog ID, and latestRowID is old callLog ID for the same number.
            log("insert into calls table");
            long rowId = getDatabaseModifier(mCallsInserter).insert(copiedValues);
            log("inserted into calls table. new rowId:" + rowId + "|dataId:"
                    + dataId + "|rawContactId" + rawContactId);
    
            if (FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
                if (updateRowID == -1) {
                    updateRowID = rowId;
                }
                log("[insert] insert updateRowID:" + updateRowID + " latestRowID:" + latestRowID + " rowId:" + rowId);
            }
            if (rowId > 0 && FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
                ContentValues updateNameLookupValues = new ContentValues();
                updateNameLookupValues.put(DialerSearchLookupColumns.CALL_LOG_ID, rowId);
                if (numberCheckFlag) {
                    /*
                     * update old NO Name CallLog records that share the same
                     * number with the new inserted one, if exist. 
                     *     String updateNoNameCallLogStmt = Calls.DATA_ID + " IS NULL " +
                     *         " AND PHONE_NUMBERS_EQUAL(" + Calls.NUMBER + ",'" + number + "') "; 
                     *     
                     * update All CallLog records that share the same number with the new inserted 
                     * one, if exist.
                     */
                    if (noNamebuilder != null && noNamebuilder.length() > 0) {
                        // update NO Name CallLog records of the inserted CallLog
                        log("[insert]updated calls record. number:" + strInsNumber + " data_id:"
                                + dataId + " raw_contact_id:" + rawContactId);
                        ContentValues updateNoNameCallLogValues = new ContentValues();
                        updateNoNameCallLogValues.put(Calls.RAW_CONTACT_ID, rawContactId);
                        updateNoNameCallLogValues.put(Calls.DATA_ID, dataId);
                        int updateNoNameCallLogCount = db.update(Tables.CALLS, updateNoNameCallLogValues,
                                Calls._ID + " IN (" + noNamebuilder.toString() + ")", null);
                        log("[insert]updated NO Name CallLog records of the inserted CallLog. Count:" + updateNoNameCallLogCount);

                        // delete No Name CallLog records in dialer search table, if exists.
                        log("[insert] delete No Name CallLog records:" + noNamebuilder.toString() + " Except:" + latestRowID);
                        String deleteNoNameCallLogInDs = "("
                                + DialerSearchLookupColumns.CALL_LOG_ID + " IN (" + noNamebuilder.toString() + ") "
                                + "AND "
                                + DialerSearchLookupColumns.NAME_TYPE + " = " + DialerSearchLookupType.NO_NAME_CALL_LOG
                                + " AND "
                                + DialerSearchLookupColumns.RAW_CONTACT_ID + " < 0 " 
                                + " AND "
                                + DialerSearchLookupColumns.DATA_ID + " < 0 )";
                        int deleteNoNameCallLogCount = db.delete( Tables.DIALER_SEARCH,
                                deleteNoNameCallLogInDs, null);
                        log("[insert] deleted No Name CallLog records in dialer search table. Count:" + deleteNoNameCallLogCount);
                    }

                    //update dialer search table. 
                    log("[insert]query dialer_search. ");
                    String updateNameCallLogStmt = "(" + DialerSearchLookupColumns.RAW_CONTACT_ID + " = " + rawContactId
                            + " AND " + DialerSearchLookupColumns.NAME_TYPE + " = 11)" 
                            + " OR (" + DialerSearchLookupColumns.DATA_ID + " = " + dataId 
                            + " AND " + DialerSearchLookupColumns.NAME_TYPE + " = 8)";
                    int updateDialerSearchCount = db.update(Tables.DIALER_SEARCH,
                            updateNameLookupValues, updateNameCallLogStmt, null);
                    log("[insert]update dialer_search table. updateDialerSearchCount:"
                            + updateDialerSearchCount);

                    // if the new a call log with new contact id but the same number
                    // change the original call_log_id as 
                    updateNameLookupValues.put(DialerSearchLookupColumns.CALL_LOG_ID, 0);
                    int upDialCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValues,
                            DialerSearchLookupColumns.CALL_LOG_ID + " = " + latestRowID, null);
                    log("[insert]update dialer_search table. updateDialerSearchCount:" + upDialCount);
                } else {
                    log("[insert]cursor nameCursor donot have data.");
                    if (latestRowID != -1) {
                        log("[insert] update NO NAME RECORD.");
                        updateNameLookupValues.put(DialerSearchLookupColumns.DATA_ID, -updateRowID);
                        updateNameLookupValues.put(DialerSearchLookupColumns.RAW_CONTACT_ID, -updateRowID);
                        updateNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME, normalizedNumber);
                        updateNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE, normalizedNumber);
                        int updateDialerSearchCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValues, DialerSearchLookupColumns.CALL_LOG_ID + " = " + latestRowID, null);
                        log("[insert]update dialer_search table. updateDialerSearchCount:"+updateDialerSearchCount);                            
                    } else {
                        log("[insert]**nameLookupCursor is null");
                        ContentValues insertNameLookupValues = new ContentValues();
                        insertNameLookupValues.put(DialerSearchLookupColumns.CALL_LOG_ID, updateRowID);
                        insertNameLookupValues.put(DialerSearchLookupColumns.NAME_TYPE, DialerSearchLookupType.NO_NAME_CALL_LOG);
                        insertNameLookupValues.put(DialerSearchLookupColumns.DATA_ID, -updateRowID);
                        insertNameLookupValues.put(DialerSearchLookupColumns.RAW_CONTACT_ID, -updateRowID);
                        insertNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME, normalizedNumber);
                        insertNameLookupValues.put(DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE, normalizedNumber);
                        long insertDialerSearch = db.insert(Tables.DIALER_SEARCH, null, insertNameLookupValues);
                        log("[insert]insert dialer_search table. insertDialerSearch:"+insertDialerSearch);
                    }
                }
            }
            if (rowId > 0) {
                notifyDialerSearchChange();
                retUri = ContentUris.withAppendedId(uri, rowId);
                notifyNewCallsCount(db, getContext());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        Log.i(TAG, "insert()  =========== Uri:" + uri);
        Log.i(TAG, "insert()- =========== Time:" + (System.currentTimeMillis() - lStart));
        Process.setThreadPriority(prio);
        return retUri;
        /**
         * @}
         */
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        checkForSupportedColumns(sCallsProjectionMap, values);
        // Request that involves changing record type to voicemail requires the
        // voicemail param set in the uri.
        if (hasVoicemailValue(values)) {
            checkIsAllowVoicemailRequest(uri);
        }

        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder);

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
            case CALLS:
                break;

            case CALLS_ID:
                selectionBuilder.addClause(getEqualityClause(Calls._ID, parseCallIdFromUri(uri)));
                break;

            default:
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
        }

        /** M: to clean the unread icon in launcher after update. @ { */
        int count = getDatabaseModifier(db).update(Tables.CALLS, values, selectionBuilder.build(), selectionArgs);
        if (count > 0) {
            notifyNewCallsCount(db, getContext());
        }
        return count;
        /** @} */
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder);
        
        /**
         * M:
         * Original Android code 
         * final SQLiteDatabase db = mDbHelper.getWritableDatabase();
         * @{
         */
        
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
        } catch (SQLiteDiskIOException err) {
            err.printStackTrace();
            return 0;
        }
        
        /**
         * @}
         */
        
        final int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
        /**
         * M:
         * Original Android code 
         * return getDatabaseModifier(db).delete(Tables.CALLS,
         *       selectionBuilder.build(), selectionArgs);
         * @{
         */
        case CALLS: {
            int count = 0;
            if (FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
                /* 
                 * update name_lookup for usage of dialer search:
                 */
                if (selection == null) {    // delete all call logs
                    log("[delete] Selection is null, delete all Call logs.");
                    int deleteCount = db.delete(Tables.DIALER_SEARCH, 
                            DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND " 
                            + DialerSearchLookupColumns.RAW_CONTACT_ID + " <=0 " , null);
                    log("[delete] delete from Dialer_Search Count: " + deleteCount);
                    ContentValues updateNameLookupValue = new ContentValues();
                    updateNameLookupValue.put(DialerSearchLookupColumns.CALL_LOG_ID, 0);
                    int updateCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValue, 
                            DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND "
                            + DialerSearchLookupColumns.RAW_CONTACT_ID + " >0 ", null);
                    log("[delete] update from Dialer_Search Count: " + updateCount);
                    count = getDatabaseModifier(db).delete(Tables.CALLS,
                            selectionBuilder.build(), selectionArgs);
                } else {
                    db.beginTransaction();
                    try {
                        log("[delete] delete calls selection: "+selection);
                        Cursor delCursor = db.query(true, Tables.CALLS,
                                new String[] { Calls._ID, Calls.NUMBER, Calls.RAW_CONTACT_ID, Calls.DATA_ID },
                                selection, selectionArgs, "data_id, _id", null, null, null);
                        Cursor allCallLogs = db.query(true,
                                Tables.CALLS, new String[] { Calls._ID, Calls.DATA_ID},
                                null, null, null, null, null, null);
                        int allCount = allCallLogs == null? 0:allCallLogs.getCount();
                        if (delCursor != null && delCursor.getCount() == allCount) {
                            int deleteCount = db.delete(Tables.DIALER_SEARCH, 
                                    DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND " 
                                    + DialerSearchLookupColumns.RAW_CONTACT_ID + " <=0 " , null);
                            log("[delete] delete from Dialer_Search Count: " + deleteCount);
                            ContentValues updateNameLookupValue = new ContentValues();
                            updateNameLookupValue.put(DialerSearchLookupColumns.CALL_LOG_ID, 0);
                            int updateCount = db.update(Tables.DIALER_SEARCH, updateNameLookupValue, 
                                    DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND "
                                    + DialerSearchLookupColumns.RAW_CONTACT_ID + " >0 ", null);
                            log("[delete] update from Dialer_Search Count: " + updateCount);
                            count = getDatabaseModifier(db).delete(Tables.CALLS,
                                    selectionBuilder.build(), selectionArgs);
                        } else if (delCursor != null && delCursor.getCount() > 0) {

                            db.execSQL("DROP TABLE IF EXISTS delCallLog");
                            if (selectionArgs != null && selectionArgs.length > 0) {
                                db.execSQL(" CREATE TEMP TABLE delCallLog AS SELECT " +
                                        "_id, number, data_id, raw_contact_id" +
                                        " FROM calls WHERE " + selection, selectionArgs);
                            } else {
                                db.execSQL(" CREATE TEMP TABLE delCallLog AS SELECT " +
                                        "_id, number, data_id, raw_contact_id" +
                                        " FROM calls WHERE " + selection);
                            }
                            count = getDatabaseModifier(db).delete(Tables.CALLS,
                                    selectionBuilder.build(), selectionArgs);
                            
                            String queryStr = "SELECT " + 
                            "delCallLog._id as _id, " +
                            "delCallLog.number as delNumber, " +
                            "delCallLog.data_id as delDataId, " +
                            "delCallLog.raw_contact_id as delRawId, " +
                            "calls._id as newId, " +
                            "calls.number as newNumber, " +
                            "calls.data_id as newDataId, " +
                            "calls.raw_contact_id as newRawId " + 
                            " FROM delCallLog " + 
                            " LEFT JOIN calls " +
                            " on case when delCallLog.data_id is null then PHONE_NUMBERS_EQUAL(delCallLog.number, calls.number) " + 
                            " else delCallLog.data_id = calls.data_id " + 
                            " end and delCallLog._id != calls._id GROUP BY delCallLog._id";
                            Cursor updateCursor = db.rawQuery(queryStr, null);
                            if (updateCursor != null) {
                                while(updateCursor.moveToNext()) {
                                    long delCallId = updateCursor.getLong(0);
                                    long delDataId = updateCursor.getLong(2);
                                    long newCallId = updateCursor.getLong(4);
                                    if (delDataId > 0) {
                                        if (mUpdateForCallLogUpdated == null) {
                                            mUpdateForCallLogUpdated = db.compileStatement(
                                                    " UPDATE " + Tables.DIALER_SEARCH +
                                                    " SET " + DialerSearchLookupColumns.CALL_LOG_ID + "=? " +
                                                    " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + "=? ");
                                        } 
                                        // named call log
                                        if (newCallId != delCallId && newCallId > 0) {
                                            mUpdateForCallLogUpdated.bindLong(1, newCallId);
                                            mUpdateForCallLogUpdated.bindLong(2, delCallId);
                                        } else if (newCallId <= 0) {
                                            mUpdateForCallLogUpdated.bindLong(1, 0);
                                            mUpdateForCallLogUpdated.bindLong(2, delCallId);
                                        }
                                        mUpdateForCallLogUpdated.execute();
                                    } else {
                                        // no name call log
                                        if (newCallId > 0) {
                                            //update new call log
                                            if (newCallId != delCallId) {
                                                if (mUpdateForNoNameCallLogDeleted == null) {
                                                    mUpdateForNoNameCallLogDeleted = db.compileStatement(
                                                            " UPDATE " + Tables.DIALER_SEARCH +
                                                            " SET " + DialerSearchLookupColumns.DATA_ID + "=?, " +
                                                            DialerSearchLookupColumns.RAW_CONTACT_ID + "=?, " +
                                                            DialerSearchLookupColumns.CALL_LOG_ID + "=? " +
                                                            " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + "=? ");
                                                }
                                                mUpdateForNoNameCallLogDeleted.bindLong(1, -newCallId);
                                                mUpdateForNoNameCallLogDeleted.bindLong(2, -newCallId);
                                                mUpdateForNoNameCallLogDeleted.bindLong(3, newCallId);
                                                mUpdateForNoNameCallLogDeleted.bindLong(4, delCallId);
                                                mUpdateForNoNameCallLogDeleted.execute();
                                            }
                                        } else {
                                            if (mDeleteForCallLogDeleted == null) {
                                                mDeleteForCallLogDeleted = db.compileStatement(
                                                        "DELETE FROM " + Tables.DIALER_SEARCH + 
                                                        " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + " =? " + 
                                                        " AND " + DialerSearchLookupColumns.NAME_TYPE + " = " + DialerSearchLookupType.PHONE_EXACT);
                                            }
                                            //delete from dialer search table
                                            mDeleteForCallLogDeleted.bindLong(1, delCallId);
                                            mDeleteForCallLogDeleted.execute();
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
                count = getDatabaseModifier(db).delete(Tables.CALLS,
                    selectionBuilder.build(), selectionArgs);
            }
            log("[delete] delete Calls. count: " + count);
            if (count > 0) {
                notifyDialerSearchChange();
            }
            return count;
        }
        /*
         * @}
         */
        default:
            throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
        }
    }

    // Work around to let the test code override the context. getContext() is final so cannot be
    // overridden.
    protected Context context() {
        return getContext();
    }

    /**
     * Returns a {@link DatabaseModifier} that takes care of sending necessary notifications
     * after the operation is performed.
     */
    private DatabaseModifier getDatabaseModifier(SQLiteDatabase db) {
        return new DbModifierWithNotification(Tables.CALLS, db, context());
    }

    /**
     * Same as {@link #getDatabaseModifier(SQLiteDatabase)} but used for insert helper operations
     * only.
     */
    private DatabaseModifier getDatabaseModifier(DatabaseUtils.InsertHelper insertHelper) {
        return new DbModifierWithNotification(Tables.CALLS, insertHelper, context());
    }

    private boolean hasVoicemailValue(ContentValues values) {
        return values.containsKey(Calls.TYPE) &&
                values.getAsInteger(Calls.TYPE).equals(Calls.VOICEMAIL_TYPE);
    }

    /**
     * Checks if the supplied uri requests to include voicemails and take appropriate
     * action.
     * <p> If voicemail is requested, then check for voicemail permissions. Otherwise
     * modify the selection to restrict to non-voicemail entries only.
     */
    private void checkVoicemailPermissionAndAddRestriction(Uri uri,
            SelectionBuilder selectionBuilder) {
        if (isAllowVoicemailRequest(uri)) {
            mVoicemailPermissions.checkCallerHasFullAccess();
        } else {
            selectionBuilder.addClause(EXCLUDE_VOICEMAIL_SELECTION);
        }
    }

    /**
     * Determines if the supplied uri has the request to allow voicemails to be
     * included.
     */
    private boolean isAllowVoicemailRequest(Uri uri) {
        return uri.getBooleanQueryParameter(Calls.ALLOW_VOICEMAILS_PARAM_KEY, false);
    }

    /**
     * Checks to ensure that the given uri has allow_voicemail set. Used by
     * insert and update operations to check that ContentValues with voicemail
     * call type must use the voicemail uri.
     * @throws IllegalArgumentException if allow_voicemail is not set.
     */
    private void checkIsAllowVoicemailRequest(Uri uri) {
        if (!isAllowVoicemailRequest(uri)) {
            throw new IllegalArgumentException(
                    String.format("Uri %s cannot be used for voicemail record." +
                            " Please set '%s=true' in the uri.", uri,
                            Calls.ALLOW_VOICEMAILS_PARAM_KEY));
        }
    }

   /**
    * Parses the call Id from the given uri, assuming that this is a uri that
    * matches CALLS_ID. For other uri types the behaviour is undefined.
    * @throws IllegalArgumentException if the id included in the Uri is not a valid long value.
    */
    private long parseCallIdFromUri(Uri uri) {
        try {
            return Long.parseLong(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid call id in uri: " + uri, e);
        }
    }

    /// M: @{
    private static final String TAG = "CallLogProvider";
    private static final boolean DBG_DIALER_SEARCH = ContactsFeatureConstants.DBG_DIALER_SEARCH;

    // Reference the com.android.phone.CallNotifier.CALL_TYPE_SIP
    // the Call Type of SIP Call
    private static final int CALL_TYPE_SIP = -2;
    
    private SQLiteStatement mUpdateForCallLogUpdated;
    private SQLiteStatement mUpdateForNoNameCallLogDeleted;
    private SQLiteStatement mDeleteForCallLogDeleted;
    
    private void notifyDialerSearchChange() {
        getContext().getContentResolver().notifyChange(
                Uri.parse("content://com.android.contacts.dialer_search/callLog/"), null, false);
    }

    private void log(String msg) {
        if (DBG_DIALER_SEARCH)
            Log.d(TAG, " " + msg);
    }
    /// @}

    /** M: send new Calls broadcast to luancher to update unread icon @{ */
    public static final void notifyNewCallsCount(SQLiteDatabase db, Context context) {
        Cursor c = null;
        if (VvmUtils.isVvmEnabled()) {
            c = db.rawQuery("select count(_id) from calls where type in (3,4) AND new=1", null);
        } else {
            c = db.rawQuery("select count(_id) from calls where type=3 AND new=1", null);
        }

        int newCallsCount = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                newCallsCount = c.getInt(0);
            }
            c.close();
        }

        Log.i(TAG, "[notifyNewCallsCount] newCallsCount = " + newCallsCount);
        //send count=0 to clear the unread icon
        if (newCallsCount >= 0) {
            Intent newIntent = new Intent(Intent.MTK_ACTION_UNREAD_CHANGED);
            newIntent.putExtra(Intent.MTK_EXTRA_UNREAD_NUMBER, newCallsCount);
            newIntent.putExtra(Intent.MTK_EXTRA_UNREAD_COMPONENT, new ComponentName(Constants.CONTACTS_PACKAGE,
                    Constants.CONTACTS_DIALTACTS_ACTIVITY));
            context.sendBroadcast(newIntent);
            android.provider.Settings.System.putInt(context.getContentResolver(), Constants.CONTACTS_UNREAD_KEY, Integer
                    .valueOf(newCallsCount));
        }
    }
    /** @} */
}