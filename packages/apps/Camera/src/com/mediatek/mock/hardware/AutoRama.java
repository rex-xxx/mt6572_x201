package com.mediatek.mock.hardware;

import android.content.Context;
import android.media.MediaActionSound;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.camera.R;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AutoRama extends Thread {

    private static final String TAG = "AutoRama";
    private static final int IDLE = 0;
    private static final int DIRECTION_DETERMING = 1;
    private static final int MOVING_FRAME = 2;
    private static final int SAVING_PICTURE = 3;
    private static final int CANCEL_CAPTURE = 4;
    private static final int FIRST_CAPTURE = 5;
    private static final int QUITING = 9;
    private static final int INTERVAL = 1200; //ms
    private Handler mReporter;
    private FrameDetector mFrameDemon = new FrameDetector();
    private int mStatus;
    private int mNextScheduleTime;
    private long mLastFrameTime;
    private MediaActionSound mCameraSound;
    private Context mContext;
    private String mCapturePath;

    public AutoRama(Handler handler, Context context, MediaActionSound sound) {
        mReporter = handler;
        mContext = context;
        mCameraSound = sound;
    }

    public synchronized void startAutoRama() {
        if (mStatus == IDLE) {
            Log.i(TAG, "startAutoRama");
            mStatus = FIRST_CAPTURE;
            this.interrupt();
        } else {
            Log.i(TAG, "startAutoRama in unsuspect status");
        }
    }

    public synchronized void stopAutoRama(int merge) {
        Log.i(TAG, "stopAutoRama");
        if (merge == 1) {
            mStatus = SAVING_PICTURE;
        } else {
            mStatus = CANCEL_CAPTURE;
        }
        this.interrupt();
    }

    public synchronized void quit() {
        Log.i(TAG, "quit autoRama");
        mStatus = QUITING;
        this.interrupt();
    }

    public void setHandler(Handler handler) {
        mReporter = handler;
    }

    public void setCapturePath(String path) {
        mCapturePath = path;
    }

    public void sendMsgToCamera(int[] intBuf) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(16);
        DataOutputStream dOutStream = new DataOutputStream(bout);
        try {
            dOutStream.writeInt(intBuf[0]);
            dOutStream.writeInt(intBuf[1]);
            dOutStream.writeInt(intBuf[2]);
            dOutStream.writeInt(intBuf[3]);
        } catch (IOException e) {
            Log.i(TAG, "write byte[] error");
        }
        Message msg = mReporter.obtainMessage(
                MessageEnum.MTK_CAMERA_MSG_EXT_DATA,
                MessageEnum.MTK_CAMERA_MSG_EXT_DATA_AUTORAMA, 0, bout.toByteArray());
        mReporter.sendMessage(msg);
    }

    public void run() {
        for (;;) {
            int[] intBuf = new int[4];
            synchronized (this) {
                mNextScheduleTime = 1000;
                switch(mStatus) {
                case IDLE:
                    mNextScheduleTime = 5000;
                    break;
                case DIRECTION_DETERMING:
                    mLastFrameTime = System.currentTimeMillis();

                    // Capture & keep first frame in buff
                    mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                    mFrameDemon.determinDirection(intBuf);
                    intBuf[0] = 0;
                    sendMsgToCamera(intBuf);
                    mStatus = MOVING_FRAME;
                    mNextScheduleTime = 100;
                    break;
                case MOVING_FRAME:
                    if (mReporter != null) {
                        int movingTime = (int) (System.currentTimeMillis() - mLastFrameTime);
                        if (movingTime < 0) {
                            movingTime = 0;
                        }
                        if (movingTime >= INTERVAL) {
                            mNextScheduleTime = 200;
                            intBuf[0] = 1;
                            sendMsgToCamera(intBuf);
                            mLastFrameTime = System.currentTimeMillis() + 200;
                            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                        } else {
                            mNextScheduleTime = 10;
                            intBuf[0] = 0;
                            mFrameDemon.getPosition(intBuf, movingTime);
                            sendMsgToCamera(intBuf);
                        }
                    }
                    break;
                case SAVING_PICTURE:
                    // Saving buff frame to disk.<Create a picture to capPath>
                    if (mContext != null && mCapturePath != null) {
                        byte[] data = onPictureCreate();
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(mCapturePath);
                            out.write(data);
                            Log.i(TAG, "flush pano pic to sdcard, path = " + mCapturePath);
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "Failed to write image", e);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to write image", e);
                        } finally {
                            try {
                                out.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Failed to close FileOutputStream", e);
                            }
                        }
                    }
                    intBuf[0] = 1;
                    sendMsgToCamera(intBuf);
                case CANCEL_CAPTURE:
                    // discard buff frame
                    mStatus = IDLE;
                    break;
                case QUITING:
                    mStatus = IDLE;
                    return;
                case FIRST_CAPTURE:
                    mNextScheduleTime = 1500;
                    intBuf[0] = 1;
                    sendMsgToCamera(intBuf);
                    mStatus = DIRECTION_DETERMING;
                    break;
                default:
                    break;
                }
            }
            try {
                Thread.sleep(mNextScheduleTime);
            } catch (InterruptedException e) {
                Log.i(TAG, "Loop into next job");
            }
        }
    }

    private static int sPictureCount = 0;
    private byte[] onPictureCreate() {
        if (null == mContext) {
            return null;
        }
        InputStream inputStream;
        sPictureCount++;
        sPictureCount %= 2;
        if (sPictureCount == 0) {
            inputStream = mContext.getResources().openRawResource(R.raw.blank);
        } else {
            inputStream = mContext.getResources().openRawResource(R.raw.test);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] b = new byte[1024];
        int size = -1;
        try {
            while ((size = inputStream.read(b)) != -1) {
                out.write(b, 0, size);
            }
        } catch (IOException ioe) {
            Log.i(TAG, "read blank.jpg in raw reault in error");
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.i(TAG, "close inputStream fail");
            }
        }
        return out.toByteArray();
    }

    private class FrameDetector {
        private static final int UNKNOWN = -1;
        private static final int LEFT = 1;
        private static final int RIGHT = 0;
        private static final int UP = 2;
        private static final int DOWN = 3;
        
        private static final int BUFF_WIDTH = 160;
        private static final int BUFF_HEIGHT = 120;
        private int mCurrentDirection = UNKNOWN;
        
        public boolean determinDirection(int[] position) {
            mCurrentDirection = ((int)(Math.random() * 100)) % 4;
            position[1] = 0;
            position[2] = 0;
            position[3] = mCurrentDirection;
            return true;
        }

        public void getPosition(int[] position, int movingTime) {
            if (position.length < 4) {
                return;
            }
            switch (mCurrentDirection) {
            case LEFT:
                position[1] = -((movingTime * BUFF_WIDTH) / INTERVAL);
                position[2] = 0;
                break;
            case RIGHT:
                position[1] = ((movingTime * BUFF_WIDTH) / INTERVAL);
                position[2] = 0;
                break;
            case UP:
                position[1] = 0;
                position[2] = -((movingTime * BUFF_HEIGHT) / INTERVAL);
                break;
            case DOWN:
                position[1] = 0;
                position[2] = ((movingTime * BUFF_HEIGHT) / INTERVAL);
                break;
            default:
                position[1] = 0;
                position[2] = 0;
                break;
            }
            position[3] = mCurrentDirection;
        }
    }
}
