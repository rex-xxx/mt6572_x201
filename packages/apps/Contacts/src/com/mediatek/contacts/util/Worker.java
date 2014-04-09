package com.mediatek.contacts.util;

import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

public class Worker extends HandlerThread {

    private static final String TAG = "Worker";
    private static final Worker INSTANCE = new Worker();
    private Handler mHandler;
    private Looper mLooper = null;
    
    private Worker() {
        super("Worker_Thread");
    }
    
    public static Worker getWorkerInstance() {
        return INSTANCE;
    }
    
    public void prepair() {
        if (mLooper == null) {
            INSTANCE.start();
            mLooper = getLooper();
            mHandler = new MyHandler(mLooper);
        } else {
            Log.d(TAG, "donothing!");
        }
    }
    
    public void postJob(Runnable r) {
        mHandler.post(r);
    }
    
    class MyHandler extends Handler {
        
        public MyHandler(Looper loop) {
            super(loop);
        }
        
//        public void handleMessage(Message msg) {
//            Log.d(TAG, "handleMessage");
//        }
    }
    
    public static class WrapperCloseCursor implements Runnable {
        Cursor mCursor;
        //public static String TAG = "WrapperCloseCursor";
        
        public WrapperCloseCursor(Cursor c) {
            mCursor = c;
        }
        
        public void run() {
            Log.d(TAG, "current thread = " + Thread.currentThread());
            if (mCursor != null) {
                Log.d(TAG, "try to close cursor = " + mCursor);
                mCursor.close();
                Log.d(TAG, "finished to close cursor = " + mCursor);
            }
        }
    }
}

