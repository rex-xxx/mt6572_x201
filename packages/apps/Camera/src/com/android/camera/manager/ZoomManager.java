package com.android.camera.manager;

import android.view.View;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SettingChecker;
import com.android.gallery3d.ui.GestureRecognizer.Listener;

import java.text.DecimalFormat;
import java.util.List;

public class ZoomManager extends ViewManager implements Camera.OnFullScreenChangedListener,
        Camera.Resumable {
    private static final String TAG = "ZoomManager";
    private static final boolean LOG = Log.LOGV;
    private static final DecimalFormat FORMATOR = new DecimalFormat("#.0"); 
    
    private static final int UNKNOWN = -1;
    private static final int RATIO_FACTOR_RATE = 100;
    private static float mIgoreDistance;
    private Listener mGalleryGestureListener;
    private MyListener mGestureListener = new MyListener();
    private boolean mResumed;
    private boolean mDeviceSupport;
    private List<Integer> mZoomRatios;
    private int mLastZoomRatio = UNKNOWN;
    //for scale gesture
    private static final float ZERO = 1;
    private float mZoomIndexFactor = ZERO; //zoom rate from 1, mapping zoom gesture rate
    //for zooming behavior
    private boolean mIgnorGestureForZooming;
    
    public ZoomManager(Camera context) {
        super(context);
        context.addResumable(this);
        context.addOnFullScreenChangedListener(this);
    }
    
    @Override
    protected View getView() {
        return null;
    }
    
    @Override
    public void begin() {
    }
    
    @Override
    public void resume() {
        if (!mResumed && getContext().isFullScreen()) {
            Listener last = getContext().setGestureListener(mGestureListener);
            if (last != mGestureListener) {
                mGalleryGestureListener = last;
            }
            mResumed = true;
        }
        if (LOG) {
            Log.v(TAG, "resume() mGalleryGestureListener=" + mGalleryGestureListener);
        }
    }
    @Override
    public void pause() {
        if (LOG) {
            Log.v(TAG, "pause() mGalleryGestureListener=" + mGalleryGestureListener
                    + ", mResumed=" + mResumed);
        }
        if (mResumed) {
            getContext().setGestureListener(mGalleryGestureListener);
            mGalleryGestureListener = null;
            mResumed = false;
        }
    }
    
    @Override
    public void finish() {
    }
    
    public void resetZoom() {
        if (LOG) {
            Log.v(TAG, "resetZoom() mZoomRatios=" + mZoomRatios + ", mLastZoomRatio=" + mLastZoomRatio);
        }
        mZoomIndexFactor = ZERO;
        if (isValidZoomIndex(0)) {
            mLastZoomRatio = mZoomRatios.get(0);
        }
    }
    
    private void performZoom(int zoomIndex, boolean userAction) {
        if (LOG) {
            Log.v(TAG, "performZoom(" + zoomIndex + ", " + userAction + ") mResumed=" + mResumed
                    + ", mDeviceSupport=" + mDeviceSupport);
        }
        if (getContext().getCameraDevice() != null && mDeviceSupport && isValidZoomIndex(zoomIndex)) {
            startAsyncZoom(zoomIndex);
            int newRatio = mZoomRatios.get(zoomIndex);
            if (mLastZoomRatio != newRatio) {
                mLastZoomRatio = newRatio;//change last zoom value to new
            }
        }
        if (userAction) {
            float zoomRation = ((float)mLastZoomRatio) / RATIO_FACTOR_RATE;
            getContext().showInfo("x" + FORMATOR.format(zoomRation));
        }
    }
    
    private void startAsyncZoom(final int zoomValue) {
        if (LOG) {
            Log.v(TAG, "startAsyncZoom(" + zoomValue + ")");
        }
        getContext().lockRun(new Runnable() {
            @Override
             public void run() {
                // Set zoom parameters asynchronously
                if (getContext().getParameters() != null && getContext().getCameraDevice() != null) {
                    if (LOG) {
                        Log.v(TAG, "startAsyncZoom() parameters.zoom=" + getContext().getParameters().getZoom());
                    }
                    if (getContext().getParameters().isZoomSupported()
                            && getContext().getParameters().getZoom() != zoomValue) {
                       getContext().getCameraDevice().setParametersAsync(getContext(), zoomValue);
                    }
                } 
            }
        });
    }
    
    private class MyListener implements Listener {
        @Override
        public void onDown(float x, float y) {
            if (LOG) {
                Log.v(TAG, "onDown(" + x + ", " + y + ")");
            }
            if (mGalleryGestureListener != null) {
                mGalleryGestureListener.onDown(x, y);
            }
            // for a complete new gesture, reset status
            mIgnorGestureForZooming = false;
        }
    
        @Override
        public boolean onFling(float velocityX, float velocityY) {
            if (LOG) {
                Log.v(TAG, "onFling(" + velocityX + ", " + velocityY + ")");
            }
            if (shouldIgnoreCurrentGesture()) {
                return false;
            }
            if (mGalleryGestureListener != null) {
                return mGalleryGestureListener.onFling(velocityX, velocityY);
            }
            return false;
        }
    
        @Override
        public boolean onScroll(float dx, float dy, float totalX, float totalY) {
            if (LOG) {
                Log.v(TAG, "onScroll(" + dx + ", " + dy + ", " + totalX + ", " + totalY + ")");
            }
            if (shouldIgnoreCurrentGesture() || shouldIgnoreScrollGesture(totalX, totalY)) {
                return false;
            }
            if (mGalleryGestureListener != null) {
                return mGalleryGestureListener.onScroll(dx, dy, totalX, totalY);
            }
            return false;
        }
    
        @Override
        public boolean onSingleTapUp(float x, float y) {
            if (LOG) {
                Log.v(TAG, "onSingleTapUp(" + x + ", " + y + ")");
            }
            if (mGalleryGestureListener != null) {
                return mGalleryGestureListener.onSingleTapUp(x, y);
            }
            return false;
        }
    
        @Override
        public void onUp() {
            if (LOG) {
                Log.v(TAG, "onUp");
            }
            if (mGalleryGestureListener != null) {
                mGalleryGestureListener.onUp();
            }
        }
        
        @Override
        public boolean onDoubleTap(float x, float y) {
            if (LOG) {
                Log.v(TAG, "onDoubleTap(" + x + ", " + y + ") mZoomIndexFactor=" + mZoomIndexFactor
                        + ", isAppSupported()=" + isAppSupported() + ", isEnabled()=" + isEnabled());
            }
            if (!isAppSupported() || !isEnabled()) {
                return false;
            }
            int oldIndex = findZoomIndex(mLastZoomRatio);
            int zoomIndex = 0;
            if (oldIndex == 0) {
                zoomIndex = getMaxZoomIndex();
                mZoomIndexFactor = getMaxZoomIndexFactor();
            } else {
                mZoomIndexFactor = ZERO;
            }
            performZoom(zoomIndex, true);
            return true;
        }
    
        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (LOG) {
                Log.v(TAG, "onScale(" + focusX + ", " + focusY + ", " + scale + ") mZoomIndexFactor=" + mZoomIndexFactor
                        + ", isAppSupported()=" + isAppSupported() + ", isEnabled()=" + isEnabled());
            }
            if (!isAppSupported() || !isEnabled()) {
                return false;
            }
            if (Float.isNaN(scale) || Float.isInfinite(scale)) {
                return false;
            }
            mZoomIndexFactor *= scale;
            if (mZoomIndexFactor <= ZERO) {
                mZoomIndexFactor = ZERO;
            } else if (mZoomIndexFactor >= getMaxZoomIndexFactor()) {
                mZoomIndexFactor = getMaxZoomIndexFactor();
            }
            int zoomIndex = findZoomIndex(Math.round(mZoomIndexFactor * RATIO_FACTOR_RATE));
            performZoom(zoomIndex, true);
            if (LOG) {
                Log.v(TAG, "onScale() mZoomIndexFactor=" + mZoomIndexFactor);
            }
            return true;
        }
    
        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            if (LOG) {
                Log.v(TAG, "onScaleBegin(" + focusX + ", " + focusY + ")");
            }
            //remember that a zooming gesture has just ended
            mIgnorGestureForZooming = true;
            return true;
        }
    
        @Override
        public void onScaleEnd() {
            if (LOG) {
                Log.v(TAG, "onScaleEnd()");
            }
        }
    }

    public void setZoomParameter() {
        if (isAppSupported()) {
            mDeviceSupport = getContext().getParameters().isZoomSupported();
            mZoomRatios = getContext().getParameters().getZoomRatios();
            int index = getContext().getParameters().getZoom();
            int len = mZoomRatios.size();
            int curRatio = mZoomRatios.get(index);
            int maxRatio = mZoomRatios.get(len - 1);
            int minRatio = mZoomRatios.get(0);
            int finalIndex = index;
            if (mLastZoomRatio == UNKNOWN || mLastZoomRatio == curRatio) {
                mLastZoomRatio = curRatio;
                finalIndex = index;
            } else {
                finalIndex = findZoomIndex(mLastZoomRatio);
            }
            int newRatio = mZoomRatios.get(finalIndex);
            performZoom(finalIndex, newRatio != mLastZoomRatio);
            if (LOG) {
                Log.v(TAG, "onCameraParameterReady() index=" + index + ", len=" + len + ", maxRatio=" + maxRatio
                        + ", minRatio=" + minRatio + ", curRatio=" + curRatio + ", finalIndex=" + finalIndex
                        + ", newRatio=" + newRatio + ", mSupportZoom=" + mDeviceSupport
                        + ", mLastZoomRatio=" + mLastZoomRatio);
            }
        } else { //reset zoom if App limit zoom function.
            resetZoom();
            performZoom(0, false);
        }
    }
    
    private boolean isAppSupported() {
        boolean enable = SettingChecker.isZoomEnable(getContext().getCurrentMode());
        if (LOG) {
            Log.v(TAG, "isAppSupported() return " + enable);
        }
        return enable;
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        if (full) {
            resume();
        } else {
            pause();
        }
    }
    
    private int findZoomIndex(int zoomRatio) {
        int find = 0; //if not find, return 0
        if (mZoomRatios != null) {
            int len = mZoomRatios.size();
            if (len == 1) {
                find = 0;
            } else {
                int max = mZoomRatios.get(len - 1);
                int min = mZoomRatios.get(0);
                if (zoomRatio <= min) {
                    find = 0;
                } else if (zoomRatio >= max) {
                    find = len - 1;
                } else {
                    for (int i = 0; i < len - 1; i++) {
                        int cur = mZoomRatios.get(i);
                        int next = mZoomRatios.get(i + 1);
                        if (zoomRatio >= cur && zoomRatio < next) {
                            find = i;
                            break;
                        }
                    }
                }
            }
        }
        return find;
    }
    
    private boolean isValidZoomIndex(int zoomIndex) {
        boolean valid = false;
        if (mZoomRatios != null && zoomIndex >= 0 && zoomIndex < mZoomRatios.size()) {
            valid = true;
        }
        if (LOG) {
            Log.v(TAG, "isValidZoomIndex(" + zoomIndex + ") return " + valid);
        }
        return valid;
    }
    
    private int getMaxZoomIndex() {
        int index = UNKNOWN;
        if (mZoomRatios != null) {
            index = mZoomRatios.size() - 1;
        }
        return index;
    }
    
    private float getMaxZoomIndexFactor() {
        return (float)getMaxZoomRatio() / RATIO_FACTOR_RATE;
    }
    
    private int getMaxZoomRatio() {
        int ratio = UNKNOWN;
        if (mZoomRatios != null) {
            ratio = mZoomRatios.get(mZoomRatios.size() - 1);
        }
        return ratio;
    }

    private boolean shouldIgnoreScrollGesture(float totalX, float totalY) {
        if (LOG) {
            Log.v(TAG, "shouldIgnoreScrollGesture(" + totalX + " " + totalY + ") mIgoreDistance = " + mIgoreDistance);
        }
        if (mIgoreDistance == 0) {
            mIgoreDistance = getContext().getResources().getDimensionPixelSize(R.dimen.ignore_distance);
        }
        return Math.abs(totalX) < mIgoreDistance && Math.abs(totalY) < mIgoreDistance;
    }

    private boolean shouldIgnoreCurrentGesture() {
        return (isAppSupported() && isEnabled() && mIgnorGestureForZooming);
    }
}
