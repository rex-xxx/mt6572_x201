package com.android.providers.contacts;

import com.android.providers.contacts.aggregation.ContactAggregator;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.mediatek.providers.contacts.ContactsFeatureConstants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;


public class DataRowHandlerForPhoneNumberEx extends
        DataRowHandlerForPhoneNumber {
    private static final String TAG = "DataRowHandlerForPhoneNumberEx";
    private static final boolean DBG = ContactsFeatureConstants.DBG_DIALER_SEARCH;
    private Context mContext;
    private ContactsDatabaseHelper mDbHelper;
    
    private SQLiteStatement mDialerSearchNumDelByCallLogIdDelete;;
    private SQLiteStatement mDialerSearchNewRecordInsert;
    private SQLiteStatement mCallsNewInsertDataIdUpdate;
    private SQLiteStatement mCallsGetLatestCallLogIdForOneContactQuery;
    private SQLiteStatement mCallsReplaceDataIdUpdate;
    private SQLiteStatement mDialerSearchCallLogIdUpdateByContactNumberUpdated;
    private SQLiteStatement mDialerSearchNoNameCallLogNumDataIdUpdate;
    private SQLiteStatement mDialerSearchContactNumDelete;

    public DataRowHandlerForPhoneNumberEx(Context context,
            ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator);
        mContext = context;
        mDbHelper = dbHelper;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        int result = super.delete(db, txContext, c);
        
        //SYNC calls table and dialer_search table AFTER delete data table.
        //This code can is called IFF IN Editing Contacts view, 
        //other cases are handled by raw_contacts trigger. 
        if (FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
            long dataId = c.getLong(DataDeleteQuery._ID);
            long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
            log("[delete] dataId: " + dataId + " || rawContactId: " + rawContactId);
            // For callLog, remove raw_contact_id and data_id
            updateCallsAndDialerSearchByContactNumberChanged(db, rawContactId, dataId);
        }
        return result;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext,
            long rawContactId, ContentValues values) {
        long dataId = super.insert(db, txContext, rawContactId, values);
        if (values.containsKey(Phone.NUMBER)) {
            String number = values.getAsString(Phone.NUMBER);
            if (FeatureOption.MTK_SEARCH_DB_SUPPORT) {
                String numberForDialerSearch = null;
                if (number != null) {
                    numberForDialerSearch = DialerSearchUtils
                            .stripSpecialCharInNumberForDialerSearch(number);
                }
                log("[insert] number:" + number + " || numberForDialerSearch: "
                        + numberForDialerSearch);
                log("[insert] rawContactId: " + rawContactId + "dataId: " + dataId);
                // update call Log record, and get the Latest call_log_id of the
                // inserted number
                int latestCallLogId = updateCallsInfoForNewInsertNumber(db,
                                numberForDialerSearch, rawContactId, dataId);
                log("[insert] latest call log id: " + latestCallLogId);
                // delete NO Name CALLLOG in dialer search table.
                if (latestCallLogId > 0) {
                    deleteDialerSearchNumByCallLogId(db, latestCallLogId);
                    log("[insert]delete no name call log. ");
                }
                // insert new data into dialer search table with latest call log
                // id.
                insertDialerSearchNewRecord(db, rawContactId, dataId, numberForDialerSearch,
                        DialerSearchLookupType.PHONE_EXACT, latestCallLogId);
                log("[insert] insert new data into dialer search table. ");

            }
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext,
            ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        boolean result = super.update(db, txContext, values, c, callerIsSyncAdapter);
        if (!result) {
            return false;
        }
        if (values.containsKey(Phone.NUMBER)) {
            String number = values.getAsString(Phone.NUMBER);
            long dataId = c.getLong(DataUpdateQuery._ID);
            long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
            // update calls table and dialer search table AFTER updating data table.
            if (FeatureOption.MTK_SEARCH_DB_SUPPORT == true) {
                log("[update]update: number: " + number + " || rawContactId: "
                        + rawContactId + " || dataId: " + dataId);
                // update calls table to clear raw_contact_id and data_id, if
                // the changing number or the changed number exists in call log.
                int mDeletedCallLogId = 0;

                // update records in calls table to no name callLog
                updateCallsAndDialerSearchByContactNumberChanged(db, rawContactId, dataId);

                String numberForDialerSearch = null;
                if (number != null) {
                    numberForDialerSearch = DialerSearchUtils
                            .stripSpecialCharInNumberForDialerSearch(number);
                }
                log("[update] number:" + number + " || numberForDialerSearch: "
                        + numberForDialerSearch);
                log("[update]Delete old records in dialer_search FOR its callLogId=0.");
                // update new number's callLog info(dataId & rawContactId) if exists
                int latestCallLogId = updateCallsInfoForNewInsertNumber(db, numberForDialerSearch,
                        rawContactId, dataId);
                log("[update] latest call log id: " + latestCallLogId);
                // delete NO Name CALLLOG in dialer search table.
                if (latestCallLogId > 0) {
                    deleteDialerSearchNumByCallLogId(db, latestCallLogId);
                    log("[update]delete no name call log for udpated number. ");
                }
                // insert new number into dialer search table with latest call log id.
                insertDialerSearchNewRecord(db, rawContactId, dataId, numberForDialerSearch,
                        DialerSearchLookupType.PHONE_EXACT, latestCallLogId);
                log("[update] insert new data into dialer search table. ");
            }
        }
        return result;
    }

    void deleteDialerSearchNumByCallLogId(SQLiteDatabase db, int latestCallLogId) {
        if (mDialerSearchNumDelByCallLogIdDelete == null) {
            mDialerSearchNumDelByCallLogIdDelete = db.compileStatement(
                    "DELETE FROM " + Tables.DIALER_SEARCH +
                            " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + " =? " +
                            " AND " + DialerSearchLookupColumns.NAME_TYPE + " = "
                            + DialerSearchLookupType.PHONE_EXACT);
        }
        mDialerSearchNumDelByCallLogIdDelete.bindLong(1, latestCallLogId);
        mDialerSearchNumDelByCallLogIdDelete.execute();
    }

    void insertDialerSearchNewRecord(SQLiteDatabase db, long rawContactId, long dataId, String number, int nameType,
            int lastCallLogId) {
        if (mDialerSearchNewRecordInsert == null) {
            mDialerSearchNewRecordInsert = db.compileStatement("INSERT INTO "
                    + Tables.DIALER_SEARCH + "("
                    + DialerSearchLookupColumns.RAW_CONTACT_ID + ","
                    + DialerSearchLookupColumns.DATA_ID + ","
                    + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                    + DialerSearchLookupColumns.NAME_TYPE + ","
                    + DialerSearchLookupColumns.CALL_LOG_ID + ","
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + ")"
                    + " VALUES (?,?,?,?,?,?)");
        }
        mDialerSearchNewRecordInsert.bindLong(1, rawContactId);
        mDialerSearchNewRecordInsert.bindLong(2, dataId);
        bindString(mDialerSearchNewRecordInsert, 3, number);
        mDialerSearchNewRecordInsert.bindLong(4, nameType);
        mDialerSearchNewRecordInsert.bindLong(5, lastCallLogId);
        bindString(mDialerSearchNewRecordInsert, 6, number);
        mDialerSearchNewRecordInsert.executeInsert();
    }
    
    int updateCallsInfoForNewInsertNumber(SQLiteDatabase db,
            String number, long rawContactId, long dataId) {
        if (mCallsNewInsertDataIdUpdate == null) {
            mCallsNewInsertDataIdUpdate = db.compileStatement(
                    "UPDATE " + Tables.CALLS + 
                    " SET " + Calls.DATA_ID + "=?, " + 
                    Calls.RAW_CONTACT_ID + "=? " + 
                    " WHERE PHONE_NUMBERS_EQUAL(" + Calls.NUMBER + ", ?) AND " + 
                    Calls.DATA_ID + " IS NULL ");
        }
        if (mCallsGetLatestCallLogIdForOneContactQuery == null) {
            mCallsGetLatestCallLogIdForOneContactQuery = db.compileStatement(
                    "SELECT " + Calls._ID + " FROM " + Tables.CALLS +
                    " WHERE " + Calls._ID + " = (" +
                    " SELECT MAX( " + Calls._ID + " ) " +
                    " FROM " + Tables.CALLS +
                    " WHERE " + Calls.DATA_ID + " =? )");
        }
        mCallsNewInsertDataIdUpdate.bindLong(1,dataId);
        mCallsNewInsertDataIdUpdate.bindLong(2,rawContactId);
        bindString(mCallsNewInsertDataIdUpdate,3,number);
        mCallsNewInsertDataIdUpdate.execute();
        int mCallLogId = 0;
        try{
            mCallsGetLatestCallLogIdForOneContactQuery.bindLong(1,dataId);
            mCallLogId = (int) mCallsGetLatestCallLogIdForOneContactQuery.simpleQueryForLong();
        } catch (android.database.sqlite.SQLiteDoneException e) {
            return 0;
        } catch (NullPointerException e){
            return 0;
        }
        return mCallLogId;
    }

    void updateCallsReplaceDataId(SQLiteDatabase db, long newDataId, long newContactId, long oldDataId) {
        if (mCallsReplaceDataIdUpdate == null) {
            mCallsReplaceDataIdUpdate = db.compileStatement("UPDATE " + Tables.CALLS
                    + " SET " + Calls.DATA_ID + "=?, " 
                    + Calls.RAW_CONTACT_ID + "=? " 
                    + " WHERE " + Calls.DATA_ID + " =? ");
        }
        if (newDataId > 0) {
            mCallsReplaceDataIdUpdate.bindLong(1, newDataId);
        } else {
            mCallsReplaceDataIdUpdate.bindNull(1);
        }
        if (newContactId > 0) {
            mCallsReplaceDataIdUpdate.bindLong(2, newContactId);
        } else {
            mCallsReplaceDataIdUpdate.bindNull(2);
        }
        mCallsReplaceDataIdUpdate.bindLong(3, oldDataId);
        mCallsReplaceDataIdUpdate.execute();
    }

    void updateDialerSearchCallLogIdByDataId(SQLiteDatabase db, long callLogId, long dataId) {
        if (mDialerSearchCallLogIdUpdateByContactNumberUpdated == null ) {
            mDialerSearchCallLogIdUpdateByContactNumberUpdated = db.compileStatement(
                    "UPDATE " + Tables.DIALER_SEARCH + 
                    " SET " + DialerSearchLookupColumns.CALL_LOG_ID + " =? " +
                    " WHERE " + DialerSearchLookupColumns.DATA_ID + " = ? ");
        }
        mDialerSearchCallLogIdUpdateByContactNumberUpdated.bindLong(1, callLogId);
        mDialerSearchCallLogIdUpdateByContactNumberUpdated.bindLong(2, dataId);
        mDialerSearchCallLogIdUpdateByContactNumberUpdated.execute();
    }
    
    void updateDialerSearchNoNameCallLog(SQLiteDatabase db, long dataId) {
        if (mDialerSearchNoNameCallLogNumDataIdUpdate == null) {
            mDialerSearchNoNameCallLogNumDataIdUpdate = db.compileStatement(
                    "UPDATE " + Tables.DIALER_SEARCH +
                            " SET " + NameLookupColumns.RAW_CONTACT_ID + " = -"
                                + DialerSearchLookupColumns.CALL_LOG_ID + "," +
                                NameLookupColumns.DATA_ID + " = -"
                                + DialerSearchLookupColumns.CALL_LOG_ID +
                            " WHERE " + DialerSearchLookupColumns.DATA_ID + " = ? AND " +
                            DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND " +
                            DialerSearchLookupColumns.NAME_TYPE + " = "
                            + DialerSearchLookupType.PHONE_EXACT);
        }
        mDialerSearchNoNameCallLogNumDataIdUpdate.bindLong(1, dataId);
        mDialerSearchNoNameCallLogNumDataIdUpdate.execute();
    }

    void deleteDialerSearchContactNum(SQLiteDatabase db, long dataId) {
        if (mDialerSearchContactNumDelete == null) {
            mDialerSearchContactNumDelete = db.compileStatement(
                    "DELETE FROM " + Tables.DIALER_SEARCH + 
                    " WHERE " + DialerSearchLookupColumns.DATA_ID + " =? ");
        }
        mDialerSearchContactNumDelete.bindLong(1, dataId);
        mDialerSearchContactNumDelete.execute();
    }

    void bindString(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }

    /**
     * Update calls and dialer_search when the contact number is changed
     * @param db
     * @param rawContactId
     * @param dataId
     */
    private void updateCallsAndDialerSearchByContactNumberChanged(SQLiteDatabase db,
            long rawContactId, long dataId) {
        String oldNumber = null;
        long callLogId = -1;
        long newDataId = -1;
        long newContactId = -1;

        log("[updateCallsAndDialerSearch] rawContactId:" + rawContactId + " dataId:" + dataId);

        // Get old number from table calls by raw_contact_id and data_id
        Cursor numberCallCursor = db.query(Tables.CALLS,
                new String[] {Calls._ID, Calls.NUMBER},
                Calls.DATA_ID + "=" + dataId + " AND " + Calls.RAW_CONTACT_ID + "=" + rawContactId,
                null, null, null, " _id DESC ", "1");
        try {
            if (null != numberCallCursor && numberCallCursor.moveToFirst()) {
                callLogId = numberCallCursor.getLong(0);
                oldNumber = numberCallCursor.getString(1);
            }
        } finally {
            if (numberCallCursor != null)
                numberCallCursor.close();
        }

        log("[updateCallsAndDialerSearch] callLogId:" + callLogId);
        if (callLogId > 0) {
            // Search the raw_contact_id and data_id according to the got old number
            if (null != oldNumber) {
                Cursor newContactIdCursor = DialerSearchUtils.queryPhoneLookupByNumber(db,
                        mDbHelper, oldNumber, new String[] {
                                PhoneLookupColumns.DATA_ID, PhoneLookupColumns.RAW_CONTACT_ID
                        }, PhoneLookupColumns.DATA_ID + "!=" + dataId, null, null, null, null, "1");

                if (newContactIdCursor != null) {
                    if (newContactIdCursor.moveToFirst()) {
                        newDataId = newContactIdCursor.getLong(0);
                        newContactId = newContactIdCursor.getLong(1);
                    }
                    newContactIdCursor.close();
                }
            }
            log("[updateCallsAndDialerSearch] callLogId:" + callLogId + " newDataId:" + newDataId
                    + " newContactId:" + newContactId);
            // Update dialer_search table:
            // Delete the old record and insert an new call log in dailer_search table
            if (newDataId > 0 && newContactId > 0) {
                // Update the Calls Table (data_id and raw_contact_id to new value)
                log("[updateCallsAndDialerSearch] Update Calls table(data_id to new data_id).");
                updateCallsReplaceDataId(db, newDataId, newContactId, dataId);

                // Update dialer_search table call_log_id by data_id
                log("[updateCallsAndDialerSearch] update dialer_search table.");
                updateDialerSearchCallLogIdByDataId(db, callLogId, newDataId);

                // Delete the number record in dialer search table
                log("[updateCallsAndDialerSearch] delete dialer_search table.");
                deleteDialerSearchContactNum(db, dataId);

            } else {
                // Update the Calls Table (data_id to null and raw_contact_id to null)
                log("[updateCallsAndDialerSearch] Update Calls table(data_id to null).");
                updateCallsReplaceDataId(db, 0, 0, dataId);
                // Update records in dialer search table to no name call if callLogId>0
                log("[updateCallsAndDialerSearch] Change old records in " +
                "dialer_search to NO NAME CALLLOG FOR its callLogId>0.");
                updateDialerSearchNoNameCallLog(db, dataId);
            }
        } else {
            // delete records in dialer search table if callLogId = 0
            log("[updateCallsAndDialerSearch] delete dialer_search table.");
            deleteDialerSearchContactNum(db, dataId);
        }
    }

    private void log(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}
