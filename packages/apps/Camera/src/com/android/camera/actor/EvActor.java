package com.android.camera.actor;

import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.camera.Camera;
import com.android.camera.Camera.Resumable;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.Storage;
import com.android.camera.manager.ModePicker;
import com.android.camera.manager.PickImageViewManager;
import com.android.camera.manager.PickImageViewManager.SelectedChangedListener;
import com.android.camera.manager.ShutterManager;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

import java.io.File;

public class EvActor extends PhotoActor {
    private static final String TAG = "EvActor";
    private static final boolean LOG = Log.LOGV;
    private static final int MAX_EV_NUM = 3;
    private static final int SLEEP_TIME_FOR_SHUTTER_SOUND = 140;

    private int mCurrentEVNum = 0;
    private PickImageViewManager mPickImageViewManager;
    private SaveRequest[] mSaveRequests;
    private SaveRequest mSaveRequest;

    private PictureCallback mEVJpegPictureMultiCallback = new PictureCallback() {

        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            Log.i(TAG, "EV picture taken mCameraClosed=" + mCameraClosed + " mCurrentEVNum=" + mCurrentEVNum);
            if (mCameraClosed) {
                return;
            }
            mCurrentEVNum++;
            saveEvPictureForMultiCallBack(data, mCurrentEVNum);
            if (MAX_EV_NUM == mCurrentEVNum) {
                multiCallBackfireEvSelector();
                mCurrentEVNum = 0;
            }
        }
    };

    private SelectedChangedListener mSelectedChangedListener = new SelectedChangedListener() {
        @Override
        public void onSelectedChanged(boolean selected) {
            if (LOG) {
                Log.v(TAG, "onSelectedChanged selected=" + selected);
            }
            if (mCameraClosed) {
                return;
            }
            if (0 < Storage.getLeftSpace()) {
                if (selected) {
                    mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_OK_CANCEL);
                } else {
                    mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_CANCEL);
                }
            }
        }

        @Override
        public void onDisplayFail() {
            if (LOG) {
                Log.v(TAG, "onDisplayFail, we don't delete the file for analyzing issue.");
            }
            if (!mCameraClosed) {
                restartPreview(true);
            }
        }
    };

    private OnClickListener mOkListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            if (LOG) {
                Log.v(TAG, "mOkListener.onClick()");
            }
            if (mCameraClosed) {
                return;
            }
            for (int i = 0; i < MAX_EV_NUM; i++) {
                if (saveOrDelete(i)) {
                    mSaveRequests[i].addRequest();
                }
            }
            restartPreview(true);
        }
    };

    private OnClickListener mCancelListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            if (LOG) {
                Log.v(TAG, "mCancelListener.onClick()");
            }
            if (mCameraClosed) {
                return;
            }
            clearRequest();
            restartPreview(true);
        }
    };

    private Resumable mResumable = new Resumable() {
        @Override
        public void resume() { }
        
        @Override
        public void pause() { }
        
        @Override
        public void finish() {
            if (LOG) {
                Log.v(TAG, "EV finish");
            }
            clearRequest();
        }
        
        @Override
        public void begin() { }
    };

    public EvActor(Camera context) {
        super(context);
        if (LOG) {
            Log.i(TAG, "EvActor initialize");
        }
        mCameraCategory = new EvCameraCategory();
        mCamera.addResumable(mResumable);
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_EV;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.i(TAG, "EvActor onCameraParameterReady");
        }
        setAspectRatio();
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }

    @Override
    public OnClickListener getOkListener() {
        return mOkListener;
    }

    @Override
    public OnClickListener getCancelListener() {
        return mCancelListener;
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Ev.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.pref_camera_capturemode_entry_evbracketshot) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    private void clearRequest() {
        if (LOG) {
            Log.v(TAG, "clearRequest mSaveRequests=" + mSaveRequests);
        }
        if (mSaveRequests != null) {
            for (int i = 0; i < mSaveRequests.length; i++) {
                if (mSaveRequests[i] != null && mSaveRequests[i].getTempFilePath() != null) {
                    new File(mSaveRequests[i].getTempFilePath()).delete();
                }
//                mSaveRequests[i] = null;
            }
            mSaveRequests = null;
        }
    }

    public boolean onBackPressed() {
        if (LOG) {
            Log.v(TAG, "onBackPressed() mPickImageViewManager.isShowing()="
                    + mPickImageViewManager.isShowing());
        }
        if (mPickImageViewManager.isShowing()) {
            mCancelListener.onClick(null);
            return true;
        } else {
            return super.onBackPressed();
        }
    }

    public void release() {
        if (LOG) {
            Log.v(TAG, "EV.release()");
        }
        super.release();
        if (mPickImageViewManager != null) {
            mPickImageViewManager.release();
        }
        mCamera.removeResumable(mResumable);
    }

    private void multiCallBackfireEvSelector() {
        if (mCameraClosed) {
            return;
        }
        mPickImageViewManager.show();
        mPickImageViewManager.setSaveRequests(mSaveRequests);
        mPickImageViewManager.displayImages();
        mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_OK_CANCEL);
        mCamera.setCameraState(Camera.STATE_PREVIEW_STOPPED);
        mCamera.setViewState(Camera.VIEW_STATE_PICKING);
        mCamera.setOrientation(true, OrientationEventListener.ORIENTATION_UNKNOWN);

        try {
            Log.e(TAG, "sleep 140ms for shuttersound");
            Thread.sleep(SLEEP_TIME_FOR_SHUTTER_SOUND); // sleep 140ms for shuttersound played out
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void restartPreview(boolean needStop) {
        if (LOG) {
            Log.v(TAG, "restartPreview camerastate=" + mCamera.getCameraState() + " needStop=" + needStop);
        }
        mPickImageViewManager.hide();
        mPickImageViewManager.setSaveRequests(null);
        mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
        mCamera.restoreViewState();
        mCamera.setOrientation(false, OrientationEventListener.ORIENTATION_UNKNOWN);
        if (mCamera.getCameraState() == Camera.STATE_PREVIEW_STOPPED) {
            super.restartPreview(true);
        }
    }

    private void saveEvPictureForMultiCallBack(byte[] data, int count) {
        if (mCameraClosed) {
            return;
        }
        mSaveRequest.setData(data);
        mSaveRequest.setIgnoreThumbnail(!mCamera.isNonePickIntent());
        mSaveRequest.saveSync();
        mSaveRequests[count - 1] = mSaveRequest;
        // prepare for next
        if (count < MAX_EV_NUM) {
            mSaveRequest = mCamera.preparePhotoRequest(Storage.FILE_TYPE_PANO, Storage.PICTURE_TYPE_JPG);
        }
    }

    private void setAspectRatio() {
        if (mPickImageViewManager != null && mCamera.getParameters() != null) {
            Size size = mCamera.getParameters().getPictureSize();
            int width = Math.max(size.width, size.height);
            int height = Math.min(size.width, size.height);
            mPickImageViewManager.setAspectRatio(((double)width) / height);
        }
    }

    private boolean saveOrDelete(int idx) {
        if (LOG) {
            Log.v(TAG, "saveOrDelete idx=" + idx);
        }
        boolean ret = true;
        if (!mPickImageViewManager.isSelected(idx)
                && mSaveRequests[idx].getTempFilePath() != null) {
            new File(mSaveRequests[idx].getTempFilePath()).delete();
            ret = false;
        }
        return ret;
    }

    class EvCameraCategory extends CameraCategory {
        public void initializeFirstTime() {
            mPickImageViewManager = new PickImageViewManager(mCamera,
                    mCamera.isImageCaptureIntent() ? 1 : 0);
            setAspectRatio();
            mPickImageViewManager.setSelectedChangedListener(mSelectedChangedListener);
        }

        @Override
        public boolean supportContinuousShot() {
            return false;
        }

        public void switchShutterButton() {
            if (mSaveRequests == null) {
                mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
            }
        }

        public boolean canshot() {
            return MAX_EV_NUM <= Storage.getLeftSpace();
        }

        public boolean applySpecialCapture() {
            return false;
        }

        public void ensureCaptureTempPath() {
            mSaveRequests = new SaveRequest[MAX_EV_NUM];
            mSaveRequest = mCamera.preparePhotoRequest(Storage.FILE_TYPE_PANO, Storage.PICTURE_TYPE_JPG);
        }

        public PictureCallback getJpegPictureCallback() {
            return mEVJpegPictureMultiCallback;
        }

        public void doOnPictureTaken() { }

        public void animateCapture(Camera camera) { }

        @Override
        public void onLeaveActor() {
            mCamera.restoreViewState();
            if (mCurrentEVNum > 0) {
                mCurrentEVNum = 0;
                new Thread() {
                    public void run() {
                        clearRequest();
                    }
                }.start();
            }
        }
    }
}
