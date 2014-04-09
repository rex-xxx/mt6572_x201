package com.mediatek.mock.hardware;

import android.os.Handler;

import com.android.camera.Log;

import java.util.Random;

public class FaceDetectionThread extends Thread {

    private static final String TAG = "FaceDetection";
//    private Handler mHandler;
    private Random mRandom = new Random();
    private boolean mStartDetect = false;
    private boolean mQuit = false;
    
    public FaceDetectionThread(Handler handler) {
//        mHandler = handler;
    }

    public void  startFaceDetection() {
        mStartDetect = true;
        this.interrupt();
    }

    public void stopFaceDetection() {
        mStartDetect = false;
        this.interrupt();
    }

    public void run() {
        int nextScheduleTime = 1000;
        while (true) {
            if (mQuit) {
                break;
            }
            if (mStartDetect) {
                int seed = mRandom.nextInt(100);
                seed %= 23;
            }
            try {
                sleep(nextScheduleTime);
            } catch (InterruptedException e) {
                Log.i(TAG, "break from Idle");
            }
        }
    }
}
