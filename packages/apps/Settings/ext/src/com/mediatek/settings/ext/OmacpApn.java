package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class OmacpApn {
    
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mSimId;
    private Uri mUri;
    private String mNumeric;

    private static final String TAG = "omacp";
    

    
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_SINGLE = 2;
    
    //  ArrayList<Map<String, String>> mApnList;
    
    /**
     * Standard projection for the interesting columns of a normal note.
     */
    protected static final String[] PROJECTION = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.SOURCE_TYPE, // 16
            Telephony.Carriers.OMACPID,//17
            Telephony.Carriers.NAPID,//18
            Telephony.Carriers.PROXYID,//19
    };

    protected static final int ID_INDEX = 0;
    protected static final int NAME_INDEX = 1;
    protected static final int APN_INDEX = 2;
    protected static final int PROXY_INDEX = 3;
    protected static final int PORT_INDEX = 4;
    protected static final int USER_INDEX = 5;
    protected static final int SERVER_INDEX = 6;
    protected static final int PASSWORD_INDEX = 7;
    protected static final int MMSC_INDEX = 8;
    protected static final int MCC_INDEX = 9;
    protected static final int MNC_INDEX = 10;
    protected static final int NUMERIC_INDEX = 11;
    protected static final int MMSPROXY_INDEX = 12;
    protected static final int MMSPORT_INDEX = 13;
    protected static final int AUTH_TYPE_INDEX = 14;
    protected static final int TYPE_INDEX = 15;
    protected static final int SOURCE_TYPE_INDEX = 16;
    protected static final int APN_ID_INDEX = 17;
    protected static final int NAP_ID_INDEX = 18;
    protected static final int PROXY_ID_INDEX = 19;   
    
    
    public OmacpApn(Context context, int simId) {
        
        mContentResolver = context.getContentResolver();
        mSimId = simId;
        
    }
    
    public OmacpApn(Context context, int simId, Uri uri, String numeric) {
        
        mContentResolver = context.getContentResolver();
        mSimId = simId;
        mUri = uri;
        mNumeric = numeric;
    }
    
//  /**
//   * Init Content URI & numeric
//   */
//  private void initState() {
//      
//      switch (mSimId) {
//          case SIM_CARD_1:
//              mUri = Telephony.Carriers.CONTENT_URI;
//              mWhere = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
//              break;
//
//          case SIM_CARD_2:
//              mUri = Telephony.Carriers.GeminiCarriers.CONTENT_URI;
//              mWhere = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1");
//              break;
//          
//          case SIM_CARD_SINGLE:
//              mUri = Telephony.Carriers.CONTENT_URI;
//              mWhere = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "");
//                break;
//      }
//
//  }
    
    /**
     * 
     * @return (_id, omacpid) pair sets which match numeric
     */
    public ArrayList<HashMap<String, String>> getExistOmacpId() {
        
        ArrayList<HashMap<String, String>> mOmacpIdSet = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> map = new HashMap<String, String>();
        
        
        String where = "numeric=\"" + mNumeric + "\"" + " and omacpid<>\'\'";

        Cursor cursor = mContentResolver.query(
                mUri, 
                new String[] {Telephony.Carriers._ID, Telephony.Carriers.OMACPID}, 
                where, 
                null, 
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                map.put(cursor.getString(1), cursor.getString(0));//(omacpid,_id) pair
                mOmacpIdSet.add(map);
                cursor.moveToNext();
            }// end of while
            cursor.close();
        }
        
        return mOmacpIdSet;

    }
    
    public ArrayList<Map<String, String>> query() {
        ArrayList<Map<String, String>> mApnList;
        String where = "numeric=\"" + mNumeric + "\"";

        Cursor cursor = mContentResolver.query(
                mUri, 
                PROJECTION, 
                where, 
                null, 
                Telephony.Carriers.DEFAULT_SORT_ORDER);
        

        HashMap<String, String> apnProp;
        mApnList = new ArrayList<Map<String, String>>();
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                apnProp = new HashMap<String, String>();
                
                apnProp.put(PROJECTION[NAME_INDEX], cursor.getString(NAME_INDEX));
                apnProp.put(PROJECTION[APN_INDEX], cursor.getString(APN_INDEX));
                apnProp.put(PROJECTION[PROXY_INDEX], cursor.getString(PROXY_INDEX));
                apnProp.put(PROJECTION[PORT_INDEX], cursor.getString(PORT_INDEX));
                apnProp.put(PROJECTION[USER_INDEX], cursor.getString(USER_INDEX));
                apnProp.put(PROJECTION[SERVER_INDEX], cursor.getString(SERVER_INDEX));
                apnProp.put(PROJECTION[PASSWORD_INDEX], cursor.getString(PASSWORD_INDEX));
                apnProp.put(PROJECTION[MMSPROXY_INDEX], cursor.getString(MMSPROXY_INDEX));
                apnProp.put(PROJECTION[MMSPORT_INDEX], cursor.getString(MMSPORT_INDEX));
                apnProp.put(PROJECTION[MMSC_INDEX], cursor.getString(MMSC_INDEX));
                apnProp.put(PROJECTION[MCC_INDEX], cursor.getString(MNC_INDEX));
                apnProp.put(PROJECTION[AUTH_TYPE_INDEX], cursor.getString(AUTH_TYPE_INDEX));
                apnProp.put(PROJECTION[TYPE_INDEX], cursor.getString(TYPE_INDEX));
    
                mApnList.add(apnProp);
                
                cursor.moveToNext();
            }// end of while
            cursor.close();
        }
        
        return mApnList;

    }
    
    /**
     *  According to OMA CP specification, 
     *  if one APN is inserted into database successfully,
     *  we can consider it as successful.
     * @param context
     * @param values
     * @return
     */
    public boolean bulkInsert(final Context context, ContentValues[] values) {

        boolean success = false;
        int rows = 0;

        Cursor cursor = null;
        try {
            rows = mContentResolver.bulkInsert(mUri, values);
            if (rows > 0) {
                success = true;
            }
        } catch (SQLException e) {
            Log.d(TAG, "Database operation: bulkInsert() failed!");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return success;

    }
    
    public long insert(final Context context, ContentValues values) {
        String id = null;
        Cursor cursor = null;
        try {
            Uri newRow = mContentResolver.insert(mUri, values);
            if (newRow != null) {
                Log.d(TAG, "uri = " + newRow);
                if (newRow.getPathSegments().size() == 2) {
                    id = newRow.getLastPathSegment();
//                  Log.d(TAG, "id = " + id);
                }
            }
        } catch (SQLException e) {
            Log.d(TAG, "insert SQLException happened!");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        } 
        if (id != null) {
            return Long.parseLong(id);
        } else {
            return -1;
        }
    }

    
    public int setCurrentApn(final Context context, final int id, final Uri preferedUri) {
        int rows = 0;
        ContentValues values = new ContentValues();
        values.put("apn_id", id);
        try {
            rows = mContentResolver.update(preferedUri, values, null, null);
        } catch (SQLException e) {
            Log.d(TAG, "setCurrentApn SQLException happened!");
        }
        return rows;
    } 

}
