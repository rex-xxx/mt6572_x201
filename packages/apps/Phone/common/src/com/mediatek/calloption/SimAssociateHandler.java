package com.mediatek.calloption;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public final class SimAssociateHandler extends HandlerThread {

    private static final String TAG = "SimAssociateHandler";

    private static final int MSG_LOAD = 0;
    private static final int MSG_QUIT = 1;
    private static final int MSG_REFRESH = 2;
    private static final int MIN_MATCH = 7;

    public static final String ASSOCIATE_CHANGED = "com.android.contacts.associate_changed";

    private static SimAssociateHandler sMe;

    private boolean mLoading;
    private boolean mStarted;

    private boolean mCacheDirty;

    protected HashMap<String, ArrayList> mSimAssociationMaps = new HashMap<String, ArrayList>();

    protected Context mContext;

    protected MyHandler mMyHandler;
    ContactContentObserver mContactContentObserver;

    protected BroadcastReceiver mMyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ASSOCIATE_CHANGED.equals(action)) {
                log("RCV associated sim changed!");
                mMyHandler.sendEmptyMessage(MSG_REFRESH);
            }
        }
    };
    
    protected class ContactContentObserver extends ContentObserver {

        public ContactContentObserver() {
            super(new Handler());
        }
        
        public void onChange(boolean selfChange) {
            log("ContactContentObserver: ");
            if (!mCacheDirty) {
                mCacheDirty = true;
                log("ContactContentObserver: " + mCacheDirty);
            }
        }
    }

    private SimAssociateHandler(Context context) {
        super(TAG);
        mContext = context;
        IntentFilter filter = new IntentFilter(ASSOCIATE_CHANGED);
        mContactContentObserver = new ContactContentObserver();
        mContext.registerReceiver(mMyReceiver, filter);
        mContext.getContentResolver().registerContentObserver(
                Data.CONTENT_URI, true,
                mContactContentObserver);
        mCacheDirty = true;
    }

    void log(String msg) {
        Log.d(TAG, msg);
    }

    public static synchronized SimAssociateHandler getInstance(Context context) {
        if (sMe == null) {
            sMe = new SimAssociateHandler(context);
        }
        return sMe;
    }

    public void prepair() {
        start();
        mMyHandler = new MyHandler(getLooper());
    }

    public void relase() {
        if (mMyHandler != null) {
            mMyHandler.sendEmptyMessage(MSG_QUIT);
        }
        mContext.unregisterReceiver(mMyReceiver);
        mContext.getContentResolver().unregisterContentObserver(mContactContentObserver);
    }

    public ArrayList query(String number) {
        String strNumber = PhoneNumberUtils.stripSeparators(number);
        if ((null != strNumber) && (strNumber.length() > MIN_MATCH)) {
            strNumber = strNumber.substring(strNumber.length() - MIN_MATCH);
        }
        return mSimAssociationMaps.get(strNumber);
    }

    public void load() {
        synchronized (this) {
            log("load");
            if (mLoading || !mCacheDirty) {
                return;
            } else {
                mLoading = true;
                if (mMyHandler.hasMessages(MSG_LOAD)) {
                    log("duplicate MSG_LOAD");
                } else {
                    mMyHandler.sendEmptyMessage(MSG_LOAD);
                }
            }
        }
    }

    class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {

            String number = null;
            int id = -1;
            ArrayList associateSims = null;
            boolean exist = false;
            switch (msg.what) {
                case MSG_LOAD:
                    log("+MSG_LOAD");
                    Cursor cursor = mContext.getContentResolver().query(
                            Data.CONTENT_URI,
                            new String[] {
                                Data.DATA1, Data.SIM_ASSOCIATION_ID
                            },
                            Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                                    + "' AND (" + Data.SIM_ASSOCIATION_ID + ">0)", null, null);
                    mSimAssociationMaps.clear();
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            number = cursor.getString(0);
                            id = cursor.getInt(1);
                            log("number = " + number + " id = " + id);
                            String strNumber = PhoneNumberUtils.stripSeparators(number);
                            if ((null != strNumber) && (strNumber.length() > MIN_MATCH)) {
                                strNumber = strNumber.substring(strNumber.length() - MIN_MATCH);
                            }
                            log("strNumber = " + strNumber);
                            associateSims = mSimAssociationMaps.get(strNumber);
                            if (associateSims == null) {
                                associateSims = new ArrayList();
                                mSimAssociationMaps.put(strNumber, associateSims);
                            }
                            exist = false;
                            for (int i = 0; i < associateSims.size(); i++) {
                                if (((Integer)associateSims.get(i)).intValue() == id) {
                                    exist = true;
                                    break;
                                }
                            }
                            if (!exist) {
                                associateSims.add(Integer.valueOf(id));
                            }
                            log("associateSims = " + associateSims);
                        } while(cursor.moveToNext());
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    synchronized (this) {
                        mLoading = false;
                    }
                    //synchronized(this) {
                        mCacheDirty = false;
                    //}
                    log("-MSG_LOADs");
                    break;
                case MSG_QUIT:
                    log("MSG_EXIT");
                    getLooper().quit();
                    break;
                case MSG_REFRESH:
                    load();
                    break;
                default:
                    break;
            }
        }
    }
}
