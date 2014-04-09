package com.mediatek.mock.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.android.camera.Log;
import com.android.camera.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Mav extends Thread {

    private static final String TAG = "Mav";
    private static final int CAPTURE_INTERVAL = 100;
    private int mMavCaptureNum = 15;
    private int mCurrentNum = 0;
    private boolean mInCapture = false;
    private boolean mMerge;
    private Handler mHandler;
    private Context mContext;
    private String mCapturePath;

    public Mav(Handler handler) {
        mHandler = handler;
    }

    public synchronized void startMAV(int num) {
        Log.i(TAG, "startMav");
        mMavCaptureNum = num;
        mInCapture = true;
    }

    public synchronized void stopMAV(int merge) {
        Log.i(TAG, "stopMav");
        mInCapture = false;
        mMerge = merge > 0;
        this.interrupt();
    }

    public void run() {
        while (mInCapture && mCurrentNum < mMavCaptureNum) {
            try {
                sleep(CAPTURE_INTERVAL);
            } catch (InterruptedException e) {
                Log.i(TAG, "get Notify");
            }
            if (!mInCapture) {
                break;
            }
            if (mHandler != null) {
                sendFrameMsg();
            }
        }
        if (mMerge) {
            Log.i(TAG, "Save mpo file");
            if (mContext != null) {
                onPictureCreate();
            }
            sendFrameMsg();
            mMerge = false;
        } else {
            Log.i(TAG, "clear frame buff");
        }
    }

    private void onPictureCreate() {
        InputStream inputStream = mContext.getResources().openRawResource(R.raw.dsc00058);
        FileOutputStream out = null;
        byte[] b = new byte[1024];
        int size = -1;
        try {
            out = new FileOutputStream(mCapturePath);
            while ((size = inputStream.read(b)) != -1) {
                out.write(b, 0, size);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File: " + mCapturePath + " not found!");
        } catch (IOException ioe) {
            Log.i(TAG, "read blank.jpg in raw reault in error");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.i(TAG, "close inputStream fail");
            }
        }
    }

    public void sendFrameMsg() {
        Message msg = mHandler.obtainMessage(
                MessageEnum.MTK_CAMERA_MSG_EXT_NOTIFY,
                MessageEnum.MTK_CAMERA_MSG_EXT_NOTIFY_MAV, 0);
        mHandler.sendMessage(msg);
    }

    public void setCapturePath(String path) {
        mCapturePath = path;
    }

    public void setContext(Context context) {
        mContext = context;
    }
}
