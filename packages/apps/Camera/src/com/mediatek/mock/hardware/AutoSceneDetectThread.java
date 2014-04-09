package com.mediatek.mock.hardware;

import android.os.Handler;
import android.os.Message;

import com.android.camera.Log;

import java.util.ArrayList;
import java.util.Random;

public class AutoSceneDetectThread extends Thread {

    private static final String TAG = "AutoSceneDetectThread";
    private static final int DETECTINGTIME = 1000;
    private static final int SCENENUM = 9;
    private static final int MAGICNUM = 23;
    private Handler mHandler;
    private boolean mQuit = false;
    private Random mRandom = new Random();
    private ArrayList<Integer> mSupportedMode = new ArrayList<Integer>();

    public AutoSceneDetectThread(Handler handler) {
        mHandler = handler;
        // Current supported mode
        mSupportedMode.add(0);
        mSupportedMode.add(1);
        mSupportedMode.add(2);
        mSupportedMode.add(3);
        mSupportedMode.add(4);
        mSupportedMode.add(6);
        mSupportedMode.add(8);
    }

    public void quit() {
        mQuit = true;
        this.interrupt();
    }

    public void run() {
        int nextScheduleTime = DETECTINGTIME;
        while (true) {
            if (mQuit) {
                break;
            }
            int seed = mRandom.nextInt(100);
            seed %= MAGICNUM;
            if (seed > MAGICNUM / 2) {
                int scene = mRandom.nextInt(seed);
                scene %= SCENENUM;
                while (mSupportedMode.indexOf(scene) == -1) {
                    scene = mRandom.nextInt(seed);
                    scene %= SCENENUM;
                }
                nextScheduleTime = DETECTINGTIME * scene;
                Message msg = mHandler.obtainMessage(
                        MessageEnum.MTK_CAMERA_MSG_EXT_NOTIFY, MessageEnum.MTK_CAMERA_MSG_EXT_NOTIFY_ASD, scene);
                mHandler.sendMessageDelayed(msg, 100);
            } else {
                nextScheduleTime = DETECTINGTIME;
            }
            try {
                sleep(nextScheduleTime);
            } catch (InterruptedException e) {
                Log.i(TAG, "break from Idle");
            }
        }
    }
}
