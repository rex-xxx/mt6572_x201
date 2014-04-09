package com.android.camera.manager;

import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.Storage;
import com.android.camera.Util;

import java.util.Locale;

public class RemainingManager extends ViewManager implements Camera.Resumable,
        Camera.OnFullScreenChangedListener,
        Camera.OnParametersReadyListener {
    private static final String TAG = "RemainingManager";
    private static final boolean LOG = Log.LOGV;
    
    private static final int TYPE_COUNT = 0;
    private static final int TYPE_TIME = 1;
    private static final Long REMAIND_THRESHOLD = 100L;
    
    private static final int[] MATRIX_REMAINING_TYPE = new int[ModePicker.MODE_NUM_ICON];
    static {
        MATRIX_REMAINING_TYPE[ModePicker.MODE_PHOTO]        = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_HDR]          = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_FACE_BEAUTY]  = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_PANORAMA]     = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_MAV]          = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_ASD]          = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_SMILE_SHOT]   = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_BEST]         = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_EV]           = TYPE_COUNT;
        MATRIX_REMAINING_TYPE[ModePicker.MODE_VIDEO]        = TYPE_TIME;
    }
    
    private TextView mRemainingView;
    private OnScreenHint mStorageHint;
    private WorkerHandler mWorkerHandler;
    private int mType = TYPE_COUNT;
    private String mRemainingText;
    private boolean mResumed;
    private boolean mParametersReady;

    private static final int MSG_UPDATE_STORAGE = 0;
    private Long mAvaliableSpace;
    
    private static final int DELAY_MSG_MS = 1500;
    
    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_STORAGE:
                mAvaliableSpace = Storage.getAvailableSpace();
                removeMessages(MSG_UPDATE_STORAGE);
                sendEmptyMessageDelayed(MSG_UPDATE_STORAGE, DELAY_MSG_MS);
                break;
            default:
                break;
            }
        }
    }
    
    private Handler mMainHandler = new Handler();
    
    public RemainingManager(Camera context) {
        super(context);
        context.addResumable(this);
        context.addOnFullScreenChangedListener(this);
        context.addOnParametersReadyListener(this);
        //disable animation for cross with remaining.
        setAnimationEnabled(true, false);
    }
    
    @Override
    public void begin() {
        if (mWorkerHandler == null) {
            HandlerThread t = new HandlerThread("thumbnail-creation-thread");
            t.start();
            mWorkerHandler = new WorkerHandler(t.getLooper());
            mWorkerHandler.sendEmptyMessage(MSG_UPDATE_STORAGE);
        }
    }
    
    @Override
    public void resume() {
        if (LOG) {
            Log.v(TAG, "resume()");
        }
        mResumed = true;
        showHint();
    }
    
    @Override
    public void pause() {
        if (LOG) {
            Log.v(TAG, "pause()");
        }
        mResumed = false;
        if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }
    
    @Override
    public void finish() {
        if (mWorkerHandler != null) {
            mWorkerHandler.getLooper().quit();
        }
    }
    
    @Override
    protected View getView() {
        View view = inflate(R.layout.remaining);
        mRemainingView = (TextView)view.findViewById(R.id.remaining_view);
        return view;
    }
    
    @Override
    protected void onRefresh() {
        super.onRefresh();
        if (mRemainingView != null) {
            mRemainingView.setText(mRemainingText);
        }
    }
    
    public boolean showIfNeed() {
        if (mParametersReady) {
            if (mAvaliableSpace == null || mAvaliableSpace.longValue() < 0) {
                mAvaliableSpace = Storage.getAvailableSpace();
            }
            long leftSpace = mAvaliableSpace.longValue();
            mType = MATRIX_REMAINING_TYPE[getContext().getCurrentMode()];
            leftSpace = computeStorage(leftSpace);
            updateStorageHint(leftSpace);
            updateRemainingView(REMAIND_THRESHOLD, leftSpace);
        }
        if (LOG) {
            Log.v(TAG, "showIfNeed() return " + isShowing() + ", mParametersReady=" + mParametersReady);
        }
        return isShowing();
    }
    
    public boolean showAways() {
        if (mParametersReady) {
            if (mAvaliableSpace == null || mAvaliableSpace.longValue() < 0) {
                mAvaliableSpace = Storage.getAvailableSpace();
            }
            long leftSpace = mAvaliableSpace.longValue();
            mType = MATRIX_REMAINING_TYPE[getContext().getCurrentMode()];
            leftSpace = computeStorage(leftSpace);
            updateStorageHint(leftSpace);
            updateRemainingView(Long.MAX_VALUE, leftSpace);
        }
        if (LOG) {
            Log.v(TAG, "showAways() return " + isShowing() + ", mParametersReady=" + mParametersReady);
        }
        return isShowing();
    }
    
    public void clearAvaliableSpace() {
        if (LOG) {
            Log.v(TAG, "clearAvaliableSpace() mAvaliableSpace=" + mAvaliableSpace
                    + ", mParametersReady=" + mParametersReady);
        }
        mAvaliableSpace = null;
    }
    
    public void showHint() {
        if (LOG) {
            Log.v(TAG, "showHint() mAvaliableSpace=" + mAvaliableSpace + ", mParametersReady=" + mParametersReady);
        }
        if (mParametersReady) {
            mMainHandler.post(new Runnable() { //delay for after resume
               @Override
                public void run() {
                   if (mAvaliableSpace == null || mAvaliableSpace.longValue() < 0) {
                       mAvaliableSpace = Storage.getAvailableSpace();
                   }
                   long leftSpace = mAvaliableSpace.longValue();
                   leftSpace = computeStorage(leftSpace);
                   updateStorageHint(leftSpace);
                } 
            });
        }
    }
    
    private long computeStorage(long avaliableSpace) {
        if (avaliableSpace > Storage.LOW_STORAGE_THRESHOLD) {
            avaliableSpace = (avaliableSpace - Storage.LOW_STORAGE_THRESHOLD)
                    / (mType == TYPE_COUNT ? pictureSize() : videoFrameRate());
        } else if (avaliableSpace > 0) {
            avaliableSpace = 0;
        }
        Storage.setLeftSpace(avaliableSpace);
        if (LOG) {
            Log.v(TAG, "computeStorage(" + avaliableSpace + ") return " + avaliableSpace);
        }
        return avaliableSpace;
    }
    
    private void updateRemainingView(long threshold, long leftSpace) {
        boolean needShow = (mType == TYPE_COUNT) ? (leftSpace <= threshold) : true;
        if (needShow) { //only show remaining view in some cases
            if (leftSpace < 0) {
                mRemainingText = mType == TYPE_COUNT ? stringForCount(0) : stringForTime(0);
            } else {
                mRemainingText = mType == TYPE_COUNT ? stringForCount(leftSpace) : stringForTime(leftSpace);
            }
            super.show(); //show remaining view
        }
        if (LOG) {
            Log.v(TAG, "updateRemainingView(" + threshold + ", " + leftSpace + ") mType=" + mType);
        }
    }
    
    private void updateStorageHint(long leftSpace) {
        if (LOG) {
            Log.v(TAG, "updateStorageHint(" + leftSpace + ") isFullScreen=" + getContext().isFullScreen());
        }
        String message = null;
        if (leftSpace == Storage.UNAVAILABLE) {
            message = getContext().getString(R.string.no_storage);
        } else if (leftSpace == Storage.PREPARING) {
            message = getContext().getString(R.string.preparing_sd);
        } else if (leftSpace == Storage.UNKNOWN_SIZE) {
            message = getContext().getString(R.string.access_sd_fail);
        } else if (leftSpace <= 0) {
            message = getContext().getString(Util.getNotEnoughSpaceAlertMessageId());
        }
        if (message != null && getContext().isAcceptFloatingInfo()) {
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(getContext(), message);
            } else {
                mStorageHint.setText(message);
            }
            mStorageHint.show();
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }
    
    public long pictureSize() {
        String pictureFormat;
        switch (getContext().getCameraActor().getMode()) {
            case ModePicker.MODE_PANORAMA:
                pictureFormat = "autorama";
                break;
            case ModePicker.MODE_MAV:
                pictureFormat = "mav";
                break;
            default:
                Size size = getContext().getParameters().getPictureSize();
                String pictureSize = ((size.width > size.height) ? (size.width + "x" + size.height)
                        : (size.height + "x" + size.width));
                pictureFormat = pictureSize + "-" + "superfine";
                break;
        }
        long psize = Storage.getSize(pictureFormat);
        if (LOG) {
            Log.v(TAG, "pictureSize() pictureFormat=" + pictureFormat + " return " + psize);
        }
        return psize;
    }
    
    public long videoFrameRate() {
        CamcorderProfile profile = getContext().getProfile();
        long bytePerMs = ((profile.videoBitRate + profile.audioBitRate) >> 3) / 1000;
        return bytePerMs;
    }
    
    private static String stringForTime(final long millis) {
        final int totalSeconds = (int) millis / 1000;
        final int seconds = totalSeconds % 60;
        final int minutes = (totalSeconds / 60) % 60;
        final int hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds);
        }
    }
    
    private static String stringForCount(final long count) {
        return String.format("%d", count);
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        if (LOG) {
            Log.v(TAG, "onFullScreenChanged(" + full + ")");
        }
        if (full) {
            resume();
        } else {
            pause();
        }
    }

    @Override
    public void onCameraParameterReady() {
        mParametersReady = true;
    }
    
    private class Holder {
        long mThreshold;
        long mLeftSpace;
        
        public Holder() {}
        
        public Holder(long threshold, long leftSpace) {
            mThreshold = threshold;
            mLeftSpace = leftSpace;
        }
        
        @Override
        public String toString() {
            return new StringBuilder()
            .append("Holder(mThreshold=")
            .append(mThreshold)
            .append(", mLeftSpace=")
            .append(mLeftSpace)
            .append(")")
            .toString();
        }
    }
}