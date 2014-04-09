package com.android.camera.actor;

import android.hardware.Camera.ASDCallback;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.manager.ModePicker;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

public class AsdActor extends PhotoActor {
    private static final String TAG = "AsdActor";

    private static final boolean SAVE_ORIGINAL_PICTURE = true;
    private static final boolean LOG = Log.LOGV;
    private SaveRequest mOriginalSaveRequest;

    public AsdActor(Camera context) {
        super(context);
        if (LOG) {
            Log.i(TAG, "AsdActor initialize");
        }
        mCameraCategory = new AsdCameraCategory();
        mCamera.getIndicatorManager().saveSceneMode();
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_ASD;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.i(TAG, "AsdActor onCameraParameterReady");
        }
    }

    private final ASDCallback mASDCaptureCallback = new ASDCallback() {
        public void onDetecte(int scene) {
            if (LOG) {
                Log.i(TAG, "AsdActor onDetecte scene=" + scene);
            }
            mCamera.getIndicatorManager().onDetectedSceneMode(scene);
        }
    };

    @Override
    public ASDCallback getASDCallback() {
        return mASDCaptureCallback;
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Asd.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.pref_camera_capturemode_entry_asd) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    class AsdCameraCategory extends CameraCategory {

        @Override
        public void initializeFirstTime() { }

        @Override
        public boolean supportContinuousShot() {
            return false;
        }

        @Override
        public boolean applySpecialCapture() {
            return false;
        }

        @Override
        public void doOnPictureTaken() { }

        @Override
        public void onLeaveActor() {
            mCamera.getIndicatorManager().restoreSceneMode();
            mCamera.restoreViewState();
        }
    }
}
