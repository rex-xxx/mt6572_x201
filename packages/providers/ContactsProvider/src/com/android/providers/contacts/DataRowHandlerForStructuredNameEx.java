package com.android.providers.contacts;

import com.android.providers.contacts.aggregation.ContactAggregator;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DialerSearchLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.DataRowHandler.DataDeleteQuery;
import com.android.providers.contacts.DataRowHandler.DataUpdateQuery;
import com.mediatek.providers.contacts.ContactsFeatureConstants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;

public class DataRowHandlerForStructuredNameEx extends
		DataRowHandlerForStructuredName {
	private static final String TAG = "DataRowHandlerForStructuredNameEx";
	private static final boolean DBG = ContactsFeatureConstants.DBG_DIALER_SEARCH;
	private SQLiteStatement mDialerSearchNewRecordInsert;
	private SQLiteStatement mDialerSearchDelete;
	
    public DataRowHandlerForStructuredNameEx(Context context, ContactsDatabaseHelper dbHelper,
            ContactAggregator aggregator, NameSplitter splitter,
            NameLookupBuilder nameLookupBuilder) {
		super(context, dbHelper, aggregator, splitter, nameLookupBuilder);
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        int count = super.delete(db, txContext, c);
        if (FeatureOption.MTK_SEARCH_DB_SUPPORT) {
        	long dataId = c.getLong(DataDeleteQuery._ID);
			deleteNameForDialerSearch(db, dataId);
		}
        return count;
    }
    
    @Override
    protected void insertDialerSearchName(SQLiteDatabase db, long rawContactId, long dataId,
            ContentValues values) {
        if (FeatureOption.MTK_SEARCH_DB_SUPPORT) {
            String name = values.getAsString(StructuredName.DISPLAY_NAME);
            insertNameForDialerSearch(db, rawContactId, dataId, name);
        }
    }
    
    public void insertNameForDialerSearch(SQLiteDatabase db, long rawContactId, long dataId, String name) {
    	if (mDialerSearchNewRecordInsert == null) {
        	mDialerSearchNewRecordInsert = db.compileStatement(
        			"INSERT INTO " + Tables.DIALER_SEARCH + "(" +
        			DialerSearchLookupColumns.RAW_CONTACT_ID + "," +
        			DialerSearchLookupColumns.DATA_ID + "," +
        			DialerSearchLookupColumns.NORMALIZED_NAME + "," +
        			DialerSearchLookupColumns.NAME_TYPE + "," +
        			DialerSearchLookupColumns.CALL_LOG_ID + "," + 
        			DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + ")" +
        			" VALUES (?,?,?,?,?,?)");
    	}
    	if (name == null) {
    		return;
    	}
        long mCallLogId = 0;
		//Do not insert name now, update it later for both name and alternative name.
		mDialerSearchNewRecordInsert.bindLong(1, rawContactId);
		mDialerSearchNewRecordInsert.bindLong(2, dataId);
		mDialerSearchNewRecordInsert.bindNull(3);
		mDialerSearchNewRecordInsert.bindLong(4, DialerSearchLookupType.NAME_EXACT);
		mDialerSearchNewRecordInsert.bindLong(5, mCallLogId);
		mDialerSearchNewRecordInsert.bindNull(6);
		mDialerSearchNewRecordInsert.executeInsert();
		log("[insertNameForDialerSearch]insert name records into dialer search table.");
    }
 
    public void updateNameForDialerSearch(SQLiteDatabase db, long rawContactId, long dataId, String name) {
    }
    
    private void deleteNameForDialerSearch(SQLiteDatabase db, long dataId){
    	if (mDialerSearchDelete == null) {
    		mDialerSearchDelete = db.compileStatement(
        			"DELETE FROM " + Tables.DIALER_SEARCH + 
        			" WHERE " + DialerSearchLookupColumns.DATA_ID + "=?");
    	}
    	mDialerSearchDelete.bindLong(1, dataId);
    	mDialerSearchDelete.execute();
    	log("[deleteNameForDialerSearch]delete name records in dialer search table");	
    }
    
    private void log(String msg) {
    	if (DBG) {
    		Log.d(TAG, msg);
    	}
    }
}
