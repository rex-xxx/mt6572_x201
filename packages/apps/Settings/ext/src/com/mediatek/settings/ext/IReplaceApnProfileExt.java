package com.mediatek.settings.ext;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;


public interface IReplaceApnProfileExt {

/**
     * Check if the same name or id apn exists, if it exists, replace it  
     * @param uri to access database
     * @param apn profile apn
     * @param apnId profile apn id 
     * @param name profile carrier name
     * @param values new profile values to update
     * @param numeric selected numeric
     * returns the replaced profile id 
     */  
long replaceApn(Context context, Uri uri, String apn, String apnId, String name,
        ContentValues values, String numeric);

}
