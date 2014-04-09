package com.mediatek.contacts.calllog;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.contacts.R;

import java.util.Date;
import java.util.HashMap;

public class CallLogDateFormatHelper {

    private static final String TAG = "CallLogDateFormatHelper";
    protected static final long ONE_DAY_IN_MILLISECONDS = 86400000L;
    protected static final long MONTH_SHIFT_BIT = 8L;
    protected static final long YEAR_SHIFT_BIT = 16L;

    private static String sToday = "";
    private static String sYesterday = "";

    private static long sFormattedToday;
    private static long sFormattedYesterday;

    private static HashMap<Long, Long> sMapDate;
    //private static HashMap<Long, String> sMapDateToString;

    private static boolean sInitilized;

    private static long getFormattedDate(long milliSeconds) {
        if (!sInitilized) {
            sInitilized = true;
            refreshData();
        }
        Long ret = sMapDate.get(milliSeconds);

        if (null == ret) {
            Date date = new Date(milliSeconds);
            ret = (long)((date.getYear() << YEAR_SHIFT_BIT) 
                        + ((date.getMonth() + 1) << MONTH_SHIFT_BIT) + date.getDate());
            sMapDate.put(milliSeconds, ret);
        }

        return ret;
    }

    /** 
     * Today, Yesterday, MM/dd/yyyy
     * 
     * @param context to get resource
     * @param lDate to format date
     * @return string
     */


    public static String getFormatedDateText(Context context, final long lDate) {
        String retDate = null;
        if (lDate <= 0) {
            log("getSectionHeadText lDate:" + lDate);
            return retDate;
        }
        log("getSectionHeadText lDate:" + lDate);
        long lfmtdate = getFormattedDate(lDate);

        // Get string Today, Yesterday or not
        if (lfmtdate == sFormattedToday) {
            if ("".equals(sToday)) {
                sToday = context.getResources().getString(R.string.calllog_today);
            }
            retDate = sToday;
        } else if (lfmtdate == sFormattedYesterday) {
            if ("".equals(sYesterday)) {
                sYesterday = context.getResources().getString(R.string.calllog_yesterday);
            }
            retDate = sYesterday;
        } else {
            retDate = getDateString(context, lDate);
        }
        log("getFormatedDateText()  retDate===" + retDate);
        return retDate;
    }

    /**
     * judge if is the same day
     * 
     * @param firstDate the first date to diff
     * @param secondDate the second date to diff
     * @return boolean
     */
    public static boolean isSameDay(final long firstDate, final long secondDate) {
        boolean bRet = false;

        long first = getFormattedDate(firstDate);
        long second = getFormattedDate(secondDate);

        bRet = (first == second);
        return bRet;
    }

    private static void log(String log) {
        Log.i(TAG, log);
    }

    private static final int MAX_HASH_MAP_SIZE = 500;
    
    /** clear the date map*/
    public static void refreshData() {
        long curtime = System.currentTimeMillis();

        if (null == sMapDate) {
            sMapDate = new HashMap<Long, Long>();
        }
        //if (null == sMapDateToString) {
        //    sMapDateToString = new HashMap<Long, String>();
        //}
        sFormattedToday = getFormattedDate(curtime);
        sFormattedYesterday = getFormattedDate(curtime - ONE_DAY_IN_MILLISECONDS);

        sToday = "";
        sYesterday = "";
        if (sMapDate.size() > MAX_HASH_MAP_SIZE) {
            sMapDate.clear();
        }
        //if (sMapDateToString.size() > MAX_HASH_MAP_SIZE) {
        //    sMapDateToString.clear();
        //}
        sInitilized = true;
    }

    private static String getDateString(Context context, long milliSeconds) {
        String ret = null; // sMapDateToString.get(milliSeconds);

        //if (null == ret) {
        if (null != context) {
            ret = DateFormat.getDateFormat(context).format(milliSeconds).toString();
            //sMapDateToString.put(milliSeconds, ret);
        }

        return ret;
    }
}