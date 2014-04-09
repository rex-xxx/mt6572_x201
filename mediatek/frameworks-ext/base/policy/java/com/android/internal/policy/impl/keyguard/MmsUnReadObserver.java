package com.android.internal.policy.impl.keyguard;

import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.widget.QuickContactBadge;

import com.google.android.mms.pdu.PduHeaders;

import com.mediatek.xlog.Xlog;

public class MmsUnReadObserver extends UnReadObserver {
    
    private static final String TAG = "MmsUnReadObserver";
    
    public static final Uri MMS_URI = Threads.CONTENT_URI;
    
    private static final Uri MMS_QUERY_URI = Uri.parse("content://mms/inbox");
    
    private static final String[] MMS_STATUS_PROJECTION = new String[] {
        Mms.DATE, Mms._ID};
    
    private static final String NEW_INCOMING_MM_CONSTRAINT = 
            "(" + Mms.READ + " = 0 "
            + " AND (" + Mms.MESSAGE_TYPE + " <> " + PduHeaders.MESSAGE_TYPE_DELIVERY_IND
            + " AND " + Mms.MESSAGE_TYPE + " <> " + PduHeaders.MESSAGE_TYPE_READ_ORIG_IND + ") AND "+ Mms.DATE + " >= ";
    
    private static final Uri SMS_QUERY_URI = Sms.CONTENT_URI;
    
    private static final String[] SMS_STATUS_PROJECTION = new String[] {
        Sms.DATE, Sms._ID };
    
    private static final String NEW_INCOMING_SM_CONSTRAINT =
            "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
            + " AND " + Sms.READ + " = 0 AND "+ Sms.DATE + " >= ";
    
    
    
    public MmsUnReadObserver(Handler handler, LockScreenNewEventView newEventView, long createTime) {
        super(handler, newEventView, createTime);
    }
    
    public void refreshUnReadNumber() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            public Integer doInBackground(Void... params) {
                ///M: Mms's database saves Mms received date as Second, so we need to pass second unit instead of millisecond
                long queryBaseTime = mCreateTime / 1000;
                Cursor cursor = mNewEventView.getContext().getContentResolver()
                        .query(MMS_QUERY_URI, MMS_STATUS_PROJECTION,
                                NEW_INCOMING_MM_CONSTRAINT + queryBaseTime + ")", null, null);
                int mmsCount = 0;
                if (cursor != null) {
                    try {
                        mmsCount = cursor.getCount();
                    } finally {
                        cursor.close();
                        cursor = null;
                    }
                }

                cursor = mNewEventView.getContext().getContentResolver()
                        .query(SMS_QUERY_URI, SMS_STATUS_PROJECTION,
                                NEW_INCOMING_SM_CONSTRAINT + mCreateTime + ")", null, null);
                int smsCount = 0;
                if (cursor != null) {
                    try {
                        smsCount = cursor.getCount();
                    } finally {
                        cursor.close();
                    }
                }
                Xlog.d(TAG, "refreshUnReadNumber mmsCount=" + mmsCount + ", smsCount=" + smsCount + ", mCreateTime=" + mCreateTime);
                return mmsCount + smsCount;
            }

            @Override
            public void onPostExecute(Integer result) {
                upateNewEventNumber(result);
            }
        }.execute(null, null, null);
    }
}
