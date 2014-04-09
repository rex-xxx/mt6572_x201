package com.mediatek.phone;

import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public final class Worker extends HandlerThread {

    private static final String TAG = "Worker";
    private Handler mHandler;
    private static final int MSG_QUIT = 100;
    private static final Worker INSTANCE = new Worker();
    private Looper mLooper;

    private Worker() {
        super("Worker_Thread");
    }

    /**
     * get worker singleton instance
     * @return Worker instance
     */
    public static Worker getWorkerInstance() {
        return INSTANCE;
    }

    /**
     * prepair
     */
    public void prepair() {
        if (mLooper == null) {
            INSTANCE.start();
            mLooper = getLooper();
            mHandler = new MyHandler(mLooper);
        } else {
            Log.d(TAG, "donothing!");
        }
    }

    /**
     * release
     */
    public void release() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_QUIT);
        }
    }

    /**
     * Post job
     * @param r runnable to post
     */
    public void postJob(Runnable r) {
        if (mHandler != null) {
            mHandler.post(r);
        } else {
            Log.d(TAG, "mHandler == null!");
        }
    }

    class MyHandler extends Handler {

        public MyHandler(Looper loop) {
            super(loop);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QUIT:
                    getLooper().quit();
                    break;
                default:
                    break;
            }
            Log.d(TAG, "handleMessage");
        }
    }

    public static class WrapperCloseCursor implements Runnable {
        Cursor mCursor;
        //public static String TAG = "WrapperCloseCursor";

        /**
         * Constructor function of WrapperCloseCursor
         */
        public WrapperCloseCursor(Cursor c) {
            mCursor = c;
        }

        /**
         * run function
         */
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
