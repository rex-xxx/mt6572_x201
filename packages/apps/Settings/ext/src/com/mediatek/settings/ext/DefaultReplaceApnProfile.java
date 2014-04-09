package com.mediatek.settings.ext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.mediatek.xlog.Xlog;

public class DefaultReplaceApnProfile implements IReplaceApnProfileExt {

    private static final String TAG = "DefaultReplaceApnProfileExt";
    // for omacp update result state
    // 0 stands for the apn with the apnId exists , not update it and not insert it again
    private static final long APN_EXIST = 0;
    // -1 stands for the apn inserted fail
    private static final long APN_NO_UPDATE = -1;;

    
    /**
     * Check if the same id apn exists, if it exists, replace it  
     * @param uri to access database
     * @param apn profile apn
     * @param apnId profile apn id 
     * @param name profile carrier name
     * @param values new profile values to update
     * @param numeric selected numeric
     * returns the replaced profile id 
     */  
    public long replaceApn(Context context, Uri uri, String apn, String apnId, String name,
            ContentValues values, String numeric) {
        long numReplaced = APN_NO_UPDATE;
        String where = "numeric=\"" + numeric  + "\"" + " and omacpid<>\'\'";
        
        Xlog.d(TAG,"name " + name + " numeric = " + numeric + " apnId = " + apnId);
        
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, 
                    new String[] { Telephony.Carriers._ID, Telephony.Carriers.OMACPID }, where, null,
                    Telephony.Carriers.DEFAULT_SORT_ORDER);
            
            if (cursor == null || cursor.getCount() == 0) {
                Xlog.d(TAG, "cursor is null , or cursor.getCount() == 0 return");
                return APN_NO_UPDATE;
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Xlog.d(TAG,"apnId " + apnId + " getApnId = " + cursor.getString(1));
                if (apnId.equals(cursor.getString(1))) {
                    numReplaced = APN_EXIST;
                    break;
                }
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return numReplaced;
    }

}
