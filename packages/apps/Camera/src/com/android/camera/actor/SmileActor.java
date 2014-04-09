package com.android.camera.actor;

import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.SmileCallback;

import com.android.camera.Camera;
import com.android.camera.CameraHolder;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.manager.ModePicker;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

public class SmileActor extends PhotoActor {
    private static final String TAG = "SmileActor";

    private static final boolean LOG = Log.LOGV;
    private static final boolean SAVE_ORIGINAL_PICTURE = true;
    private static final int SMILESHOT_STANDBY = 0;
    private static final int SMILESHOT_INTERVAL = 1;
    private static final int SMILESHOT_IN_PROGRESS = 2;
    private SaveRequest mOriginalSaveRequest;
    private int mStatus = SMILESHOT_STANDBY;
    private final ActorSmileCallback mSmileCallback = new ActorSmileCallback();
    private static final int SMILE_SHOT_INTERVAL = 1500;

    private Runnable mDoSmileSnapRunnable = new Runnable() {
        @Override
        public void run() {
            if (LOG) {
                Log.i(TAG, "onShutterButtonClick(null), CameraState = " + mCamera.getCameraState());
            }
            if (mStatus != SMILESHOT_IN_PROGRESS
                    && mCamera.getCameraState() != Camera.STATE_SNAPSHOT_IN_PROGRESS) {
                mStatus = SMILESHOT_STANDBY;
                onShutterButtonClick(null);
            }
        }
    };

    public SmileActor(Camera context) {
        super(context);
        if (LOG) {
            Log.i(TAG, "SmileActor initialize");
        }
        mCameraCategory = new SmileCameraCategory();
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_SMILE_SHOT;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.i(TAG, "SmileActor onCameraParameterReady");
        }
        ensureFDState(true);
        if (mStatus != SMILESHOT_IN_PROGRESS
                && !mHandler.hasCallbacks(mDoSmileSnapRunnable)) {
            mHandler.post(mDoSmileSnapRunnable);
        }
    }

    @Override
    public void release() {
        super.release();
        mCamera.removeOnFullScreenChangedListener(mFullScreenChangedListener);
        mCameraCategory.doCancelCapture();
        ensureFDState(false);
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }

    @Override
    public boolean readyToCapture() {
        if (LOG) {
            Log.i(TAG, " readyToCapture? mStatus = " + String.valueOf(mStatus));
        }
        if (mStatus == SMILESHOT_STANDBY) {
            openSmileShutterMode();
            return false;
        }
        return true;
    }

    private void openSmileShutterMode() {
        if (LOG) {
            Log.i(TAG, "openSmileShutterMode ");
        }
        if (mCamera.getCameraDevice() == null) {
            Log.e(TAG, "CameraDevice is null, ignore");
            return;
        }
        mStatus = SMILESHOT_IN_PROGRESS;
        startSmileDetection(mSmileCallback);
    }

    @Override
    public boolean doSmileShutter() {
        if (LOG) {
            Log.i(TAG, "doSmileShutter mStatus = " + String.valueOf(mStatus));
        }
        if (mStatus == SMILESHOT_IN_PROGRESS) {
            // already in smile shutter mode, capture directly.
            capture();
            stopSmileDetection();
            return true;
        }
        return false;
    }

    private Camera.OnFullScreenChangedListener mFullScreenChangedListener = new Camera.OnFullScreenChangedListener() {

        @Override
        public void onFullScreenChanged(boolean full) {
            if (!full) {
                mHandler.removeCallbacks(mDoSmileSnapRunnable);
                stopSmileDetection();
            } else if (mStatus != SMILESHOT_IN_PROGRESS
                    && !mHandler.hasCallbacks(mDoSmileSnapRunnable)) {
                mHandler.post(mDoSmileSnapRunnable);
            }
        }
    };

    private final class ActorSmileCallback implements SmileCallback {
        public void onSmile() {
            if (mStatus != SMILESHOT_IN_PROGRESS) {
                if (LOG) {
                    Log.e(TAG, "Smile callback in error state, please check");
                }
                return;
            }
            if (LOG) {
                Log.i(TAG, "smile detected, mstat:" + mStatus);
            }
            if (!mCameraClosed) {
                capture();
                stopSmileDetection();
            }
        }
    }

    /**
     * The function is to ensure FD is stopped in front sensor except Smile
     * shot.
     */
    public void ensureFDState(boolean enable) {
        if (LOG) {
            Log.i(TAG, "ensureFDState enable=" + enable + "CameraState=" + mCamera.getCameraState());
        }
        if (mCamera.getCameraState() != Camera.STATE_IDLE) {
            return;
        }
        if (enable) {
            startFaceDetection();
        } else {
            CameraInfo info = CameraHolder.instance().getCameraInfo()[mCamera.getCameraId()];
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                stopFaceDetection();
            }
        }
    }

    public void handleSDcardUnmount() {
        if (mCamera.getCameraDevice() == null) {
            return;
        }
        if (mStatus == SMILESHOT_IN_PROGRESS) {
            stopSmileDetection();
        }
        mCamera.getCameraDevice().setSmileCallback(null);
    }

    public boolean isInShutterProgress() {
        return mStatus == SMILESHOT_IN_PROGRESS;
    }

    public void startSmileDetection(SmileCallback callback) {
        mCamera.getCameraDevice().setSmileCallback(callback);
        mCamera.getCameraDevice().startSDPreview();
    }

    public void stopSmileDetection() {
        mCamera.getCameraDevice().cancelSDPreview();
        mCamera.getCameraDevice().setSmileCallback(null);
        mStatus = SMILESHOT_STANDBY;
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Smile.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.pref_camera_capturemode_entry_smileshot) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    class SmileCameraCategory extends CameraCategory {
        @Override
        public void initializeFirstTime() {
            mCamera.showInfo(mCamera.getString(R.string.smileshot_guide_capture), Camera.SHOW_INFO_LENGTH_LONG);
            mCamera.addOnFullScreenChangedListener(mFullScreenChangedListener);
        }

        @Override
        public boolean supportContinuousShot() {
            return false;
        }

        @Override
        public boolean applySpecialCapture() {
            return false;
        }

        @Override
        public void doOnPictureTaken() {
            if (LOG) {
                Log.v(TAG, "doOnPictureTaken() mCamera.isFullScreen() = " + mCamera.isFullScreen()
                        + " mCamera.getCurrentMode() = " + mCamera.getCurrentMode());
            }
            if (mCamera.isFullScreen() && mCamera.getCurrentMode() == ModePicker.MODE_SMILE_SHOT) {
                if (mHandler.hasCallbacks(mDoSmileSnapRunnable)) {
                    mHandler.removeCallbacks(mDoSmileSnapRunnable);
                }
                mHandler.postDelayed(mDoSmileSnapRunnable, SMILE_SHOT_INTERVAL);
                mStatus = SMILESHOT_INTERVAL;
            }
        }

        @Override
        public boolean doCancelCapture() {
            if (LOG) {
                Log.v(TAG, "mCamera.getCameraDevice()=" + mCamera.getCameraDevice()
                        + " mStatus=" + mStatus);
            }
            mCamera.setSwipingEnabled(true);
            if (mCamera.getCameraDevice() == null) {
                return false;
            }
            if (mStatus == SMILESHOT_IN_PROGRESS) {
                stopSmileDetection();
            } else {
                mStatus = SMILESHOT_STANDBY;
            }
            return false;
        }

        @Override
        public void onLeaveActor() {
            mCamera.restoreViewState();

            mHandler.removeCallbacks(mDoSmileSnapRunnable);
            if (mStatus == SMILESHOT_IN_PROGRESS) {
                stopSmileDetection();
            }
        }
    }
}
