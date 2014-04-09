package com.android.internal.policy.impl.keyguard;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.CallLog.Calls;

import com.mediatek.xlog.Xlog;

public class MissCallUnReadObserver extends UnReadObserver {
    
    private static final String TAG = "MissCallUnReadObserver";
    
    public static final Uri MISS_CALL_URI = Calls.CONTENT_URI;
    private static final String[] MISS_CALL_PROJECTION = new String[] {Calls._ID, Calls.NEW, Calls.DATE};
    private static final String MISS_CALL_SELECTION = "(" + Calls.NEW + " = ? AND " +
            Calls.TYPE + " = ? AND " + Calls.IS_READ  + " = ? AND " + Calls.DATE + " >= ";
    private static final String[] MISS_CALL_SELECTION_ARGS = new String[] {"1", Integer.toString(Calls.MISSED_TYPE), Integer.toString(0)};
    
    public MissCallUnReadObserver(Handler handler, LockScreenNewEventView newEventView, long createTime) {
        super(handler, newEventView, createTime);
    }
    
    public void refreshUnReadNumber() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            public Integer doInBackground(Void... params) {
                Cursor cursor = mNewEventView.getContext().getContentResolver()
                        .query(MISS_CALL_URI, MISS_CALL_PROJECTION,
                                MISS_CALL_SELECTION + mCreateTime + " )", MISS_CALL_SELECTION_ARGS, null);
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
