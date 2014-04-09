package com.mediatek.providers.settings.ext;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;


/**
 * Interface that defines all methos which are implemented in in DatabaseHelper about Settings Provider
 */
 
 /** {@hide} */
public class DefaultDatabaseHelperExt implements IDatabaseHelperExt
{
    private static final String TAG = "DefaultDatabaseHelperExt";

    /**
     * @param context Context
     * @param name String
     * @param defResId int
     * @return the value
     * get the string type
     */
    public String getResStr(Context context, String name, int defResId) {
        String res = context.getResources().getString(defResId);
        Log.d(TAG,"get name = " + name + " string value = " + res);
        return res;
    } 

    /**
     * @param context Context
     * @param name String
     * @param defResId int
     * @return the value
     * get the boolean type
     */
    public String getResBoolean(Context context, String name, int defResId) {
        String res = context.getResources().getBoolean(defResId) ? "1" : "0";
        Log.d(TAG,"get name = " + name + " boolean value = " + res);
        return res;
    }

    /**
     * @param context Context
     * @param name String
     * @param defResId int
     * @return the value
     * get the integer type
     */
    public String getResInteger(Context context, String name, int defResId) {
        String res = Integer.toString(context.getResources().getInteger(defResId));
        Log.d(TAG,"get name = " + name + " int value = " + res);
        return res;
    }

    /**
     * @param context Context
     * @param name String
     * @param defResId int
     * @param defBase int
     * @return the value
     * get the fraction type
     */
    public String getResFraction(Context context, String name, int defResId,int defBase) {
    	String res = Float.toString(context.getResources().getFraction(defResId, defBase, defBase));
        Log.d(TAG,"get name = " + name + " fraction value = " + res);
        return res;
    }
}
