package com.mediatek.providers.settings.ext;

import android.content.Context;
/**
 * Interface that defines all methods which are implemented in DatabaseHelper about Settings Provider
 */
 
 /** {@hide} */
public interface IDatabaseHelperExt
{
    // get the name's string value resource
    public String getResStr(Context context, String name, int defResId);
    // get the name's boolean value resource
    public String getResBoolean(Context context, String name, int defResId);
    // get the name's Integer value resource
    public String getResInteger(Context context, String name, int defResId);
    // get the name's Fraction value resource
    public String getResFraction(Context context, String name, int defResId, int defBase);
}
