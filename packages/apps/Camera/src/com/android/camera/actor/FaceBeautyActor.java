package com.android.camera.actor;

import android.hardware.Camera.CameraInfo;

import com.android.camera.Camera;
import com.android.camera.CameraHolder;
import com.android.camera.FeatureSwitcher;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.Storage;
import com.android.camera.manager.ModePicker;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

public class FaceBeautyActor extends PhotoActor {
    private static final String TAG = "FaceBeautyActor";

    private static final boolean LOG = Log.LOGV;
    private static final int SMILESHOT_STANDBY = 0;
    private static final int SMILESHOT_IN_PROGRESS = 1;
    private SaveRequest mOriginalSaveRequest;
    private int mStatus = SMILESHOT_STANDBY;

    public FaceBeautyActor(Camera context) {
        super(context);
        if (LOG) {
            Log.i(TAG, "FaceBeautyActor initialize");
        }
        mCameraCategory = new FaceBeautyCameraCategory();
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_FACE_BEAUTY;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.i(TAG, "FaceBeautyActor onCameraParameterReady");
        }
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }

    public void initializeFaceView() {
        super.initializeFaceView();
        mCamera.getFocusManager().enableFaceBeauty(true);
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "FaceBeauty.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.pref_camera_capturemode_enrty_fb) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    @Override
    public void release() {
        super.release();

        // font sensor will not support fd in other mode
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCamera.getCameraId()];
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            stopFaceDetection();
        }
    }

    class FaceBeautyCameraCategory extends CameraCategory {
        @Override
        public void initializeFirstTime() {
            // make sure fs is started
            if (!sFaceDetectionStarted) {
                startFaceDetection();
            }
        }

        @Override
        public boolean supportContinuousShot() {
            return false;
        }

        @Override
        public void ensureCaptureTempPath() {
            if (FeatureSwitcher.isFaceBeautyOriginalPictureSaved()) {
                mOriginalSaveRequest = mContext.preparePhotoRequest(Storage.FILE_TYPE_PANO, Storage.PICTURE_TYPE_JPG);
            } else {
                mOriginalSaveRequest = null;
            }
        }

        @Override
        public boolean applySpecialCapture() {
            return false;
        }

        @Override
        public void doOnPictureTaken() {
            if (LOG) {
                Log.v(TAG, "FaceBeauty.doOnPictureTaken");
            }
            // add animation
            super.animateCapture(mCamera);
            if (mOriginalSaveRequest == null) {
                return;
            }
            mOriginalSaveRequest.addRequest();
        }

        @Override
        public void animateCapture(Camera camera) { }

        @Override
        public boolean enableFD(Camera camera) {
            return true;
        }

        @Override
        public void onLeaveActor() {
            mCamera.getFocusManager().enableFaceBeauty(false);
            updateSavingHint(false, false);
        }
    }
}
