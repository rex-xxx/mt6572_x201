package com.android.internal.policy.impl.keyguard;

import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.widget.QuickContactBadge;

import com.mediatek.xlog.Xlog;

public abstract class UnReadObserver extends ContentObserver {
    static final String TAG = "UnReadObserver";
    
    final LockScreenNewEventView mNewEventView;
    
    long mCreateTime;
    
    public UnReadObserver(Handler handler, LockScreenNewEventView newEventView, long createTime) {
        super(handler);
        mNewEventView = newEventView;
        mCreateTime = createTime;
    }
    
    public void onChange(boolean selfChange) {
        refreshUnReadNumber();
    }
    
    public abstract void refreshUnReadNumber();
    
    public final void upateNewEventNumber(final int unreadNumber) {
        if (mNewEventView != null) {
            mNewEventView.setNumber(unreadNumber);                    
        } else {
            Xlog.e(TAG, "mNewEventView is null");
        }
    }
    
    // When queryt base time changed, we need to reset new event number
    public void updateQueryBaseTime(long newBaseTime) {
        mCreateTime = newBaseTime;
        upateNewEventNumber(0);
    }

}
