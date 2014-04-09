package com.android.camera.actor;

import com.android.camera.Camera;
import com.android.camera.FeatureSwitcher;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.Storage;
import com.android.camera.manager.ModePicker;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

public class HdrActor extends PhotoActor {
    private static final String TAG = "HdrActor";

    private static final boolean LOG = Log.LOGV;
    private SaveRequest mOriginalSaveRequest;
    private byte[] mRawImageBuffer;

    public HdrActor(Camera context) {
        super(context);
        if (LOG) {
            Log.i(TAG, "HdrActor initialize");
        }
        mCameraCategory = new HdrCameraCategory();
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_HDR;
    }

    @Override
    public void onCameraOpenDone() {
        super.onCameraOpenDone();
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.i(TAG, "HdrActor onCameraParameterReady");
        }
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Hdr.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.pref_camera_hdr_title) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    class HdrCameraCategory extends CameraCategory {
        public void initializeFirstTime() {
            mCamera.showInfo(mCamera.getString(R.string.hdr_guide_capture), Camera.SHOW_INFO_LENGTH_LONG);
        }

        @Override
        public boolean supportContinuousShot() {
            return false;
        }

        public void ensureCaptureTempPath() {
            if (FeatureSwitcher.isHdrOriginalPictureSaved()) {
                mOriginalSaveRequest = mContext.preparePhotoRequest(Storage.FILE_TYPE_PANO, Storage.PICTURE_TYPE_JPG);
            } else {
                mOriginalSaveRequest = null;
            }
        }

        public boolean applySpecialCapture() {
            return false;
        }

        public void doOnPictureTaken() {
            if (LOG) {
                Log.v(TAG, "Hdr.doOnPictureTaken");
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
        public void onLeaveActor() {
            if (LOG) {
                Log.v(TAG, "HDR.onLeaveActor");
            }
            mCamera.restoreViewState();
        }
    }
}
