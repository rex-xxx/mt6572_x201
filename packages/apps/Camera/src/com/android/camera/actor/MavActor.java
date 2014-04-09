package com.android.camera.actor;

import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.MAVCallback;
import android.hardware.Camera.Parameters;
import android.media.MediaActionSound;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.android.camera.Camera;
import com.android.camera.CameraErrorCallback;
import com.android.camera.CameraHolder;
import com.android.camera.FileSaver.FileSaverListener;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.Storage;
import com.android.camera.Util;
import com.android.camera.actor.PhotoActor.CameraCategory;
import com.android.camera.manager.ModePicker;
import com.android.camera.manager.PanoramaViewManager;
import com.android.camera.manager.ShutterManager;
import com.android.camera.ui.ProgressIndicator;
import com.android.camera.ui.ShutterButton;

public class MavActor extends PhotoActor {
    private static final String TAG = "MavActor";
    private static final boolean LOG = true;

    protected final Handler mMavHandler = new MavHandler(mCamera.getMainLooper());
    private MAVCallback mMavCallback = new MavFrameCallback();

    private PanoramaViewManager mPanoramaView;
    private int mCaptureState;
    private boolean mStopProcess = false;
    private Object mLock = new Object();

    private static final int IDLE = 0;
    private static final int STARTED = 1;
    private static final int MERGING = 2;

    public static final int GUIDE_SHUTTER = 0;
    public static final int GUIDE_CAPTURE = 1;

    private static final int MSG_FINAL_IMAGE_READY = 1;
    private static final int MSG_CLEAR_SCREEN_DELAY = 2;
    private static final int MSG_LOCK_ORIENTATION = 3;

    private static final int NUM_MAV_CAPTURE = 25;
    private int mCurrentNum = 0;
    private boolean mStopping;
    private long mTimeTaken;
    private boolean mShowingCollimatedDrawable;
    private SaveRequest mSaveRequest;

    private Runnable mOnHardwareStop;
    private Runnable mRestartCaptureView;
    private boolean mShutterPressed;

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MavHandler extends Handler {
        public MavHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "handleMessage what= " + msg.what);
            }

            switch (msg.what) {
            case MSG_FINAL_IMAGE_READY:
                updateSavingHint(false, false);
                resetCapture();
                if (!mCameraClosed) {
                    mCameraCategory.animateCapture(mCamera);
                }
                break;
            case MSG_CLEAR_SCREEN_DELAY:
                mCamera.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case MSG_LOCK_ORIENTATION:
                lockOrientation();
                break;
            default:
                break;
            }
        }
    }

    public MavActor(Camera context) {
        super(context);
        if (LOG) {
            Log.v(TAG, "MavActor initialize");
        }
        mCameraCategory = new MavCategory();
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_MAV;
    }

    @Override
    public OnClickListener getOkListener() {
        return null;
    }

    @Override
    public OnClickListener getCancelListener() {
        return mCancelOnClickListener;
    }

    @Override
    public void onCameraOpenDone() {
        super.onCameraOpenDone();
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        mMavHandler.sendEmptyMessage(MSG_LOCK_ORIENTATION);
    }

    private FileSaverListener mFileSaverListener = new FileSaverListener() {
        @Override
        public void onFileSaved(SaveRequest request) {
            mMavHandler.sendEmptyMessage(MSG_FINAL_IMAGE_READY);
        }
    };

    private CameraErrorCallback mPanoramaErrorCallback = new CameraErrorCallback() {
        public void onError(int error, android.hardware.Camera camera) {
            super.onError(error, camera);
            if (error == android.hardware.Camera.CAMERA_ERROR_NO_MEMORY) {
                Util.showErrorAndFinish(mCamera, R.string.capture_memory_not_enough);
            } else if (error == android.hardware.Camera.CAMERA_ERROR_RESET) {
                if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS) {
                    showCaptureError();
                    stopCapture(false);
                }
            }
        }
    };

    private Runnable mFalseShutterCallback = new Runnable() {
        @Override
        public void run() {
            // simulate an onShutter event since it is not supported in this mode.
            mCamera.getFocusManager().resetTouchFocus();
            mCamera.getFocusManager().updateFocusUI();
        }
    };

    public boolean hasCaptured() {
        if (LOG) {
            Log.v(TAG, "hasCaptured mCaptureState =" + mCaptureState + " mCurrentNum: " + mCurrentNum);
        }
        return mCaptureState != IDLE && mCurrentNum > 0;
    }

    public View.OnClickListener mCancelOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            onKeyPressed(false);
        }
    };

    private Camera.OnFullScreenChangedListener mFullScreenChangedListener = new Camera.OnFullScreenChangedListener() {

        @Override
        public void onFullScreenChanged(boolean full) {
            if (LOG) {
                Log.v(TAG, "onFullScreenChanged full = " + full);
            }
            if (full) {
                // we should keep landscape orientation
                lockOrientation();
            }
        }
    };

    @Override
    public void onDisplayRotate() {
        if (LOG) {
            Log.v(TAG, "onDisplayRotate mCamera.isFullScreen() = " + mCamera.isFullScreen());
        }
        if (mCamera.isFullScreen()) {
            // need to relock orientation when display rotate 
            // used case: orientation = 90 and slide from gallery2 to camera
            mCamera.setOrientation(false, OrientationEventListener.ORIENTATION_UNKNOWN);
            lockOrientation();
        }
    }

    private boolean startCapture() {
        if (mCamera.getCameraDevice() != null && mCaptureState == IDLE && !mStopping) {
            mCaptureState = STARTED;
            mCurrentNum = 0;
            mShowingCollimatedDrawable = false;

            doStart();
            mPanoramaView.show();
            return true;
        } else {
            if (LOG) {
                Log.v(TAG, "start mCaptureState: " + mCaptureState);
            }
            return false;
        }
    }

    private void lockOrientation() {
        if (LOG) {
            Log.v(TAG, "lockOrientation mCamera.getCameraDisplayOrientation() =" + mCamera.getCameraDisplayOrientation()
                    + " mCamera.getOrietation() = " + mCamera.getOrietation());
        }
        //default landscape mode, or cameraInfo.orientation.
        if (mCamera.getCameraDisplayOrientation() == 0 || mCamera.getCameraDisplayOrientation() == 180) {
            mCamera.setOrientation(true, (mCamera.getOrietation() == 0 || mCamera.getOrietation() == 180)
                    ? OrientationEventListener.ORIENTATION_UNKNOWN : 0);
        } else {
            mCamera.setOrientation(true, (mCamera.getOrietation() == 90 || mCamera.getOrietation() == 270)
                    ? OrientationEventListener.ORIENTATION_UNKNOWN : 270);
        }
    }

    @Override
    public boolean handleFocus() {
        if (!mShutterPressed) {
            super.handleFocus();
        }
        return true;
    }

    private void stopAsync(final boolean isMerge) {
        if (LOG) {
            Log.v(TAG, "stopAsync mStopping: " + mStopping);
        }

        if (mStopping) {
            return;
        }

        mStopping = true;
        Thread stopThread = new Thread(new Runnable() {
            public void run() {
                doStop(isMerge);
                mOnHardwareStop = new Runnable() {
                    public void run() {
                        mStopping = false;
                        if (!isMerge) {
                            // if isMerge is true, onHardwareStopped
                            // will be called in onCapture.
                            onHardwareStopped(false);
                        }
                    }
                };
                mMavHandler.post(mOnHardwareStop);

                synchronized (mLock) {
                    mStopProcess = false;
                    mLock.notifyAll();
                }
            }
        });
        synchronized (mLock) {
            mStopProcess = true;
        }
        stopThread.start();
    }

    private void doStart() {
        if (LOG) {
            Log.v(TAG, "doStart");
        }
        mCamera.getCameraDevice().setMAVCallback(getMavCallback());
        mCamera.getCameraDevice().startMAV(NUM_MAV_CAPTURE);
    }

    private final class MavFrameCallback implements MAVCallback {

        public void onFrame() {
            if (LOG) {
                Log.v(TAG, "onFrame: " + mCurrentNum + ",mCaptureState: " + mCaptureState);
            }
            if (mCaptureState == IDLE) {
                return;
            }

            if (mCurrentNum == NUM_MAV_CAPTURE || mCaptureState == MERGING) {
                if (LOG) {
                    Log.v(TAG, "mav done");
                }
                mCaptureState = IDLE;
                onHardwareStopped(true);
            } else if (mCurrentNum >= 0 && mCurrentNum < NUM_MAV_CAPTURE) {
                mPanoramaView.setProgress((mCurrentNum + 1) * ProgressIndicator.BLOCK_NUMBERS / NUM_MAV_CAPTURE);
            } else {
                Log.w(TAG, "onFrame is called in abnormal state");
            }

            mCurrentNum++;
            if (mCurrentNum == NUM_MAV_CAPTURE) {
                stop(true);
            }
        }
    }

    public MAVCallback getMavCallback() {
        return mMavCallback;
    }

    @Override
    public void onMediaEject() {
        if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS) {
            stopCapture(false);
        }
    }

    public void onCameraClose() {
        mCameraClosed = true;
        safeStop();
        super.onCameraClose();
    }

    // do the stop sequence carefully in order not to cause driver crash.
    private void safeStop() {
        // maybe stop capture(stopAUTORAMA or stopMAV) is ongoing,then it is not allowed to stopPreview.
        CameraHolder holder = CameraHolder.instance();
        if (LOG) {
            Log.v(TAG, "check stopAsync thread state, if running,we must wait");
        }
        checkStopProcess();
        synchronized (holder) {
            stopPreview();
        }
        // Note: mCameraState will be changed in stopPreview and closeCamera
        stopCapture(false);
    }

    private void doStop(boolean isMerge) {
        if (LOG) {
            Log.v(TAG, "doStop isMerge " + isMerge);
        }

        if (mCamera.getCameraDevice() != null) {
            CameraHolder holder = CameraHolder.instance();
            synchronized (holder) {
                if (holder.isSameCameraDevice(mCamera.getCameraDevice())) {
                    // means that hw was shutdown
                    // and no need to call stop anymore.
                    mCamera.getCameraDevice().stopMAV(isMerge ? 1 : 0);
                } else {
                    Log.w(TAG, "doStop device is release? ");
                }
            }
        }
    }

    private void onHardwareStopped(boolean isMerge) {
        if (LOG) {
            Log.v(TAG, "onHardwareStopped isMerge: " + isMerge);
        }

        if (isMerge) {
            mCamera.getCameraDevice().setMAVCallback(null);
        }

        onCaptureDone(isMerge);
    }

    public void stop(boolean isMerge) {
        if (LOG) {
            Log.v(TAG, "stop mCaptureState: " + mCaptureState);
        }

        if (mCamera.getCameraDevice() != null && mCaptureState == STARTED) {
            mCaptureState = isMerge ? MERGING : IDLE;
            if (!isMerge) {
                mCamera.getCameraDevice().setMAVCallback(null);
            } else {
                onMergeStarted();
            }

            stopAsync(isMerge);
            mPanoramaView.resetController();
            mPanoramaView.hide();
        }
    }

    public void checkStopProcess() {
        while (mStopProcess) {
            waitLock();
        }
    }

    private void waitLock() {
        try {
            synchronized (mLock) {
                mLock.wait();
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException in waitLock");
        }
    }
    
    private void onCaptureDone(boolean isMerge) {
        if (LOG) {
            Log.v(TAG, "onCaptureDone isMerge " + isMerge + " mCameraState=" + mCamera.getCameraState());
        }

        if (isMerge) {
            mSaveRequest.addRequest();
            mSaveRequest.setListener(mFileSaverListener);
        } else {
            resetCapture();
        }
    }

    public void onMergeStarted() {
        if (!mCameraClosed) {
            updateSavingHint(true, false);
            mCamera.dismissInfo();
        }
    }

    public void onKeyPressed(boolean ok) {
        if (LOG) {
            Log.v(TAG, "onKeyPressed ok = " + ok + " state=" + mCamera.getCameraState());
        }
        if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS) {
            stopCapture(ok);
        }
    }

    private void stopCapture(boolean isMerge) {
        if (LOG) {
            Log.v(TAG, "stopCapture isMerge = " + isMerge);
        }

        // only do merge when already have captured images.
        if (!hasCaptured()) {
            isMerge = false;
        }
        stop(isMerge);
        if (mCameraClosed && mCaptureState != IDLE) {
            mCaptureState = IDLE;
            mCamera.setSwipingEnabled(true);
            mPanoramaView.resetController();
            mPanoramaView.hide();
            updateSavingHint(false, false);
            mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
//            mCamera.setOrientation(false, OrientationEventListener.ORIENTATION_UNKNOWN);
            mCamera.restoreViewState();
        }
    }

    private void resetCapture() {
        if (LOG) {
            Log.v(TAG, "resetCapture mCamera.getCameraState()=" + mCamera.getCameraState());
        }
        mShutterPressed = false;
        // if we need to wait for merge,unlockAeAwb must be called after we receive the last callback.
        // so if isMerge = true,we will do it later in onCaptureDone.
        if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS) {
            unlockAeAwb();
            mCamera.setCameraState(Camera.STATE_IDLE);
        }
//        mCamera.setOrientation(false, OrientationEventListener.ORIENTATION_UNKNOWN);
        mCamera.restoreViewState();
        mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
        mCamera.setSwipingEnabled(true);
        mCamera.keepScreenOnAwhile();
        showGuideString(GUIDE_SHUTTER);

        if (!mCameraClosed) {
            mCamera.getCameraDevice().setAutoFocusMoveCallback(getAutoFocusMoveCallback());
            startFaceDetection();
        }
    }

    private void unlockAeAwb() {
        if (mCamera.getCameraState() != Camera.STATE_PREVIEW_STOPPED) {
            mCamera.getFocusManager().setAeLock(false); // Unlock AE and AWB.
            mCamera.getFocusManager().setAwbLock(false);
            setFocusParameters();
            if (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mCamera.getFocusManager()
                    .getFocusMode())) {
                mCamera.getCameraDevice().cancelAutoFocus();
            }
        }
    }

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "MavActor.onShutterButtonLongPressed(" + button + ")");
        }
        mCamera.showInfo(mCamera.getString(R.string.mav_dialog_title) +
                mCamera.getString(R.string.camera_continuous_not_supported));
    }

    @Override
    public void onShutterButtonClick(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "MavActor.onShutterButtonClick(" + button + ")");
        }
        super.onShutterButtonClick(button);
        mSnapshotOnIdle = false;
    }

    public boolean capture() {
        if (LOG) {
            Log.v(TAG,"capture begin");
        }
        // If we are already in the middle of taking a snapshot then ignore.
        if (mCamera.getCameraDevice() == null
                || mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS) {
            return false;
        }
        // set path
        mSaveRequest = mContext.preparePhotoRequest(Storage.FILE_TYPE_PANO, Storage.PICTURE_TYPE_MPO);
        // lock awb
        mCamera.getFocusManager().setAwbLock(true);
        setFocusParameters();

        mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_CANCEL);

        if (!startCapture()) { // it is still busy.
            return false;
        }
        mCamera.setCameraState(Camera.STATE_SNAPSHOT_IN_PROGRESS);
        mCamera.setSwipingEnabled(false);
        mCamera.showRemaining();
        mCamera.setViewState(Camera.VIEW_STATE_PANORAMA_CAPTURE);
        stopFaceDetection();
        mCamera.getCameraDevice().setAutoFocusMoveCallback(null);
        mCamera.getFocusManager().clearFocusOnContinuous();

        showGuideString(GUIDE_CAPTURE);
        mCamera.keepScreenOnAwhile();
        mMavHandler.postDelayed(mFalseShutterCallback, 300);
        return true;
    }

    @Override
    public ErrorCallback getErrorCallback() {
        return mPanoramaErrorCallback;
    }

    public void showGuideString(int step) {
        int guideId = 0;
        switch (step) {
        case GUIDE_SHUTTER:
            guideId = R.string.mav_guide_shutter;
            break;
        case GUIDE_CAPTURE:
            guideId = R.string.mav_guide_move;
            break;
        default:
            break;
        }

        // show current guide
        if (guideId != 0) {
            mCamera.showInfo(mCamera.getString(guideId), Camera.SHOW_INFO_LENGTH_LONG);
        }
    }

    private void showCaptureError() {
        mCamera.dismissAlertDialog();
        if (!mCameraClosed) {
            final String dialogTitle = mCamera.getString(R.string.mav_dialog_title);
            final String dialogOk = mCamera.getString(R.string.dialog_ok);
            final String dialogMavFailedString = mCamera.getString(R.string.mav_dialog_save_failed);
            mCamera.showAlertDialog(dialogTitle, dialogMavFailedString, dialogOk, null, null, null);
        }
    }

    public void release() {
        super.release();
        mCamera.removeOnFullScreenChangedListener(mFullScreenChangedListener);
        if (mPanoramaView != null) {
	        mPanoramaView.release();
        }
    }

    class MavCategory extends CameraCategory {
        public void initializeFirstTime() {
            mCamera.addOnFullScreenChangedListener(mFullScreenChangedListener);
            showGuideString(GUIDE_SHUTTER);
            mPanoramaView = new PanoramaViewManager(mCamera, PanoramaViewManager.MAV_VIEW);
        }

        public void shutterPressed() {
            if (LOG) {
                Log.v(TAG,"MavCategory.shutterPressed");
            }
            overrideFocusMode(Parameters.FOCUS_MODE_AUTO);
            mShutterPressed = true;
            mCamera.getFocusManager().onShutterDown();
        }

        public void shutterUp() {
            if (LOG) {
                Log.v(TAG,"MavCategory.shutterUp");
            }
            mShutterPressed = false;
            mCamera.getFocusManager().onShutterUp();
        }
        public boolean supportContinuousShot() {
            return false;
        }

        public boolean skipFocus() {
            return false;
        }

        public void doShutter() {
            playSound(MediaActionSound.START_VIDEO_RECORDING);
            mCamera.setSwipingEnabled(false);
        }

        public boolean enableFD(Camera camera) {
            return false;
        }

        public boolean applySpecialCapture() {
            return false;
        }

        public void doOnPictureTaken() { }

        @Override
        public void onLeaveActor() {
            mShutterPressed = false;
            mCamera.setOrientation(false, OrientationEventListener.ORIENTATION_UNKNOWN);
            overrideFocusMode(null);
            mCamera.restoreViewState();
        }
    }
}
