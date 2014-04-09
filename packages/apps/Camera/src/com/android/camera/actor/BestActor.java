package com.android.camera.actor;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.manager.ModePicker;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

public class BestActor extends PhotoActor {
    private static final String TAG = "BestActor";
    private static final boolean LOG = Log.LOGV;

    public BestActor(Camera context) {
        super(context);
        if (LOG) {
            Log.i(TAG, "BestActor initialize");
        }
        mCameraCategory = new BestCameraCategory();
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_BEST;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.i(TAG, "BestActor onCameraParameterReady");
        }
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Best.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.pref_camera_capturemode_entry_bestshot) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    class BestCameraCategory extends CameraCategory {
        public void initializeFirstTime() { }

        @Override
        public boolean supportContinuousShot() {
            return false;
        }

        public boolean applySpecialCapture() {
            return false;
        }

        public void doOnPictureTaken() {
            if (LOG) {
                Log.v(TAG, "BestActor.doOnPictureTaken");
            }
            // add animation
            super.animateCapture(mCamera);
        }

        public void animateCapture(Camera camera) { }
    }
}
