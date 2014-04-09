package com.android.internal.policy.impl.keyguard;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.CalendarContract.CalendarAlerts;

import com.mediatek.xlog.Xlog;


public class CalendarUnReadObserver extends UnReadObserver {
    
    private static final String TAG = "CalendarUnReadObserver";
    
    public static final Uri CALENDAR_URL = CalendarAlerts.CONTENT_URI;
    
    private static final String ACTIVE_ALERTS_SELECTION = "(" + CalendarAlerts.STATE + "=? ) AND " + CalendarAlerts.RECEIVED_TIME + ">=";

    private static final String[] ACTIVE_ALERTS_SELECTION_ARGS = new String[] { Integer.toString(CalendarAlerts.STATE_FIRED) };

    static final String[] ALERT_PROJECTION = new String[] {
        CalendarAlerts._ID,                     // 0
        CalendarAlerts.EVENT_ID,                // 1
        CalendarAlerts.STATE,                   // 2
        CalendarAlerts.TITLE,                   // 3
        CalendarAlerts.EVENT_LOCATION,          // 4
        CalendarAlerts.SELF_ATTENDEE_STATUS,    // 5
        CalendarAlerts.ALL_DAY,                 // 6
        CalendarAlerts.ALARM_TIME,              // 7
        CalendarAlerts.MINUTES,                 // 8
        CalendarAlerts.BEGIN,                   // 9
        CalendarAlerts.END,                     // 10
        CalendarAlerts.DESCRIPTION,             // 11
    };
    
    public CalendarUnReadObserver(Handler handler, LockScreenNewEventView newEventView, long createTime) {
        super(handler, newEventView, createTime);
    }
    
    public void refreshUnReadNumber() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            public Integer doInBackground(Void... params) {
                Cursor cursor = mNewEventView.getContext().getContentResolver().query(
                        CalendarAlerts.CONTENT_URI, ALERT_PROJECTION, 
                        (ACTIVE_ALERTS_SELECTION + String.valueOf(mCreateTime)), 
                        ACTIVE_ALERTS_SELECTION_ARGS, null);
                int count = 0;
                if (cursor != null) {
                    try {
                        count = cursor.getCount();
                    } finally {
                        cursor.close();
                    }
                }
                Xlog.d(TAG, "refreshUnReadNumber count=" + count);
                return count;
            }

            @Override
            public void onPostExecute(Integer result) {
                upateNewEventNumber(result);
            }
        }.execute(null, null, null);
    }
}
