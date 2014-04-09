package com.android.camera.actor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ContinuousShotDone;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.ZSDPreviewDone;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.camera.Camera;
import com.android.camera.Camera.OnSingleTapUpListener;
import com.android.camera.CameraErrorCallback;
import com.android.camera.CameraHolder;
import com.android.camera.CameraSettings;
import com.android.camera.Exif;
import com.android.camera.FocusManager;
import com.android.camera.FocusManager.Listener;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.SettingChecker;
import com.android.camera.Storage;
import com.android.camera.Util;
import com.android.camera.manager.MMProfileManager;
import com.android.camera.manager.ModePicker;
import com.android.camera.manager.SelfTimerManager;
import com.android.camera.manager.SelfTimerManager.SelfTimerListener;
import com.android.camera.manager.ShutterManager;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;
import com.mediatek.camera.ext.ExtensionHelper;
import com.mediatek.camera.ext.IFeatureExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class PhotoActor extends CameraActor implements FocusManager.Listener, OnShutterButtonListener,
        ContinuousShotDone {
    private static final String TAG = "PhotoActor";
    private static final boolean LOG = Log.LOGV;
    private static final String TEMP_CROP_FILE_NAME = "crop-temp";
    private static final int START_PREVIEW_DONE = 100;
    private static final int BURST_SAVING_DONE = 101;
    private static final int IMAGE_PICK_SAVING_DONE = 102;
    private static final int PARAMETER_CHANGE_DONE = 103;

    private int mMaxCaptureNum = CameraSettings.SUPPORTED_SHOW_CONINUOUS_SHOT_NUMBER ? 
            Integer.parseInt(CameraSettings.DEFAULT_CAPTURE_NUM) : Integer.parseInt(CameraSettings.DEFAULT_CONINUOUS_CAPTURE_NUM);
    // We number the request code from 1000 to avoid collision with Gallery.
    private static final int REQUEST_CROP = 1000;
    private static final int IMAGE_DISPLAY_DURATION = 1200;
    private static final int THUMBNAIL_REFRESH_CONTINUOUS = 500;
    private static final int THUMBNAIL_REFRESH_NORMAL = 0;
    private static final boolean SKIP_FOCUS_ON_CAPTURE = true;
    protected Camera mCamera;
    protected SaveRequest mSaveRequest;
    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackFinishTime;
    public long mCaptureStartTime;
    protected boolean mCapturing = false;
    protected byte[] mJpegImageData;
    private boolean mZSDEnabled;
    private MediaActionSound mCameraSound;

    private RenderInCapture mRenderThread;
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private final AutoFocusMoveCallback mAutoFocusMoveCallback = new AutoFocusMoveCallback();

    private long mFocusStartTime;
    private long mShutterCallbackTime;
    private long mContinuousShotStartTime;
    protected long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    protected final Handler mHandler = new MainHandler();
    private static Object sFaceDetectionSync = new Object();
    protected static boolean sFaceDetectionStarted = false;
    protected boolean mCameraClosed = false;
    private boolean mSelftimerCounting = false;
    private boolean mSavingPictures = false;
    protected boolean mSnapshotOnIdle = false;
    protected CameraCategory mCameraCategory;
    private MemoryManager mMemoryManager = new MemoryManager();
    private boolean mContinuousShotPerformed = false;
    private boolean mIgnoreClick = false;
    private int mCurrentShotsNum = 0;
    private SoundPool mBurstSound;
    private int mSoundID;
    private int mStreamID;
    private Thread mWaitSavingDoneThread;
    private boolean mSupportContinuous = false;
    private static boolean sIsAutoFocusCallback = false;
    protected boolean mInitialized = false;
    private boolean mKeyHalfPressed = false;
    private boolean mCameraKeyLongPressed = false;

    public PhotoActor(Camera context) {
        super(context);
        mCamera = getContext();
        mCameraCategory = new CameraCategory();

        if (mCamera.isImageCaptureIntent()) {
            mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO);
        } else {
            mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
        }
    }

    protected class RenderInCapture extends Thread {
        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    Thread.sleep(60);
                    mCamera.requestRender();
                }
            } catch (InterruptedException e) {
                if (LOG) {
                    Log.i(TAG, "RenderInCapture exit");
                }
            }
        }
    }

    private class WaitSavingDoneThread extends Thread {
        @Override
        public void run() {
            mCamera.getFileSaver().waitDone();
            mCamera.getThumbnailManager().setRefreshInterval(THUMBNAIL_REFRESH_NORMAL);
            mHandler.sendEmptyMessage(BURST_SAVING_DONE);
        }
    }

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "msg id=" + msg.what);
            }
            switch (msg.what) {
            case START_PREVIEW_DONE:
                initializeAfterPreview();
                onPreviewStartDone();
//                startFaceDetection();
                break;
            case BURST_SAVING_DONE:
                updateSavingHint(false, false);
                onBurstSaveDone();
                break;
            case IMAGE_PICK_SAVING_DONE:
                updateSavingHint(false, false);
                onImagePickSaveDone();
                break;
            case PARAMETER_CHANGE_DONE:
                initializeAfterPreview();
                break;
            default:
                break;
            }
        }
    }

    private Runnable mDoSnapRunnable = new Runnable() {
        @Override
        public void run() {
            if (LOG) {
                Log.v(TAG, "mDoSnapRunnable.run()");
            }
            onShutterButtonClick(null);
        }
    };

    private OnClickListener mOkListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            if (LOG) {
                Log.v(TAG, "mOkListener.onClick()");
            }
            doAttach();
        }
    };
    private OnClickListener mCancelListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            if (LOG) {
                Log.v(TAG, "mCancelListener.onClick()");
            }
            doCancel();
        }
    };
    private OnClickListener mRetakeListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            if (LOG) {
                Log.v(TAG, "mRetakeListener.onClick()");
            }
            if (mCameraClosed) {
                return;
            }

            mCamera.hideReview();
            mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO);
            restartPreview(true);
        }
    };

    private ZSDPreviewDone mZSDPreviewDone = new ZSDPreviewDone() {
        public void onPreviewDone() {
            if (!mCamera.isImageCaptureIntent() && mZSDEnabled) {
                // camera need not wait this call to start capture animation.
                // so, interrupt thead only.
                interruptRenderThread();
            }
            mCamera.getCameraDevice().setPreviewDoneCallback(null);
        }
    };

    private Camera.OnSingleTapUpListener mOnSingleTapListener = new Camera.OnSingleTapUpListener() {

        @Override
        public void onSingleTapUp(View view, int x, int y) {
            if (LOG) {
                Log.v(TAG,"onSingleTapUp mCameraClosed=" + mCameraClosed
                        + "mCamera.getCameraDevice()=" + mCamera.getCameraDevice()
                        + "mCamera.getFocusManager()=" + mCamera.getFocusManager()
                        + "mCamera.getCameraState()=" + mCamera.getCameraState());
            }
            // getFocusAreaSupported and getMeteringAreaSupported will return true
            // when devices support both continuous and infinity focus mode.
            String focusMode = null;
            if (mCamera.getFocusManager() != null) {
                focusMode = mCamera.getFocusManager().getCurrentFocusMode(mCamera);
            }
            if (focusMode == null || Parameters.FOCUS_MODE_INFINITY.equals(focusMode)) {
                return;
            }
            if (mCameraClosed || mCamera.getCameraDevice() == null
                    || mCamera.getFocusManager() == null || !mInitialized
                    || mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS
                    || mCamera.getCameraState() == Camera.STATE_SWITCHING_CAMERA
                    || mCamera.getCameraState() == Camera.STATE_PREVIEW_STOPPED) {
                return;
            }

            // Check if metering area or focus area is supported.
            if (!mCamera.getFocusManager().getFocusAreaSupported()
                    && !mCamera.getFocusManager().getMeteringAreaSupported()) {
                return;
            }
            if (SelfTimerManager.getInstance(mCamera.getMainLooper()).isSelfTimerCounting()) {
                return;
            }
            mCamera.getFocusManager().onSingleTapUp(x, y);
        }
    };

    @Override
    public void onShutterButtonLongPressed(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Photo.onShutterButtonLongPressed(" + button + ")");
        }
        if (mCamera.isImageCaptureIntent() || !mSupportContinuous) {
            mCamera.showInfo(mCamera.getString(R.string.normal_camera_continuous_not_supported));
            return;
        }

        if (!isCameraPrepareDone()) {
            return;
        }
        Util.clearMemoryLimit();
        mMemoryManager.initMemory();
        mContinuousShotPerformed = true;
        mCurrentShotsNum = 0;
        mCamera.applyContinousShot();
        mCamera.getThumbnailManager().setRefreshInterval(THUMBNAIL_REFRESH_CONTINUOUS);
        mCamera.getFocusManager().clearFocusOnContinuous();
        mCamera.getFocusManager().doSnap();
//        mStreamID = mBurstSound.play(mSoundID, 1.0f, 1.0f, 1, -1, 1.0f);
    }

    private void cancelContinuousShot() {
        mCamera.getCameraDevice().cancelContinuousShot();
        mBurstSound.stop(mStreamID);
    }

    @Override
    public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
        if (LOG) {
            Log.v(TAG, "Photo.onShutterButtonFocus(" + button + ", " + pressed + ")"
                    + " mContinuousShotPerformed=" + mContinuousShotPerformed
                    + " mCameraClosed=" + mCameraClosed
                    + " camera.state=" + mCamera.getCameraState()
                    + " mCameraCategory.supportContinuousShot()=" + mCameraCategory.supportContinuousShot()
                    + " mCamera.isImageCaptureIntent()=" + mCamera.isImageCaptureIntent());
        }
        mCamera.collapseViewManager(true);
        // used for the actor except continuousshot
        if (mCamera.isImageCaptureIntent() || !mCameraCategory.supportContinuousShot()) {
            if (mCameraClosed || mCameraCategory.skipFocus()
                    || (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS)
                    || (mCamera.getCameraState() == Camera.STATE_PREVIEW_STOPPED)
                    || SelfTimerManager.getInstance(mCamera.getMainLooper()).isSelfTimerEnabled()) {
                return;
            }

            // Do not do focus if there is not enough storage.
            if (pressed && !canTakePicture()) {
                return;
            }

            if (pressed) {
                mCameraCategory.shutterPressed();
            } else {
                mCameraCategory.shutterUp();
            }
        } else {
            if (!pressed && mContinuousShotPerformed && !mCameraClosed) {
                if (LOG) {
                    Log.i(TAG, "Button up Msg received, start to Cancel continuous shot");
                }
                mContinuousShotPerformed = false;
                // press shutter button and hardkey at the same time and hardkey up
                if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS) {
                    cancelContinuousShot();
                    // avoid for scene of quickly press up before continuous sound is playing.
                    if (mCurrentShotsNum != 0) {
                        updateSavingHint(true, false);
                    }
                    mSavingPictures = true;
                } else {
//                    mBurstSound.stop(mStreamID);
                    mCamera.cancelContinuousShot();
                }
            }
            mIgnoreClick = false;
        }
    }

    @Override
    public void onShutterButtonClick(ShutterButton button) {
        if (LOG) {
            Log.v(TAG, "Photo.onShutterButtonClick(" + button + ")");
        }
        MMProfileManager.triggerPhotoShutterClick();
        if (!mContinuousShotPerformed && !mIgnoreClick && isCameraPrepareDone()) {
            if (SelfTimerManager.getInstance(mCamera.getMainLooper()).checkSelfTimerMode()) {
                mCamera.setSwipingEnabled(false);
                mCamera.setViewState(Camera.VIEW_STATE_CAPTURE);
                mSelftimerCounting = true;
                return;
            } else {
                mSelftimerCounting = false;
            }
            mCamera.getFocusManager().doSnap();
            mCameraCategory.doShutter();
        }
    }

    private FaceDetectionListener mFaceDetectionListener = new FaceDetectionListener() {
        @Override
        public void onFaceDetection(Face[] faces, android.hardware.Camera camera) {
            if (sFaceDetectionStarted) {
                mCamera.getFaceView().setFaces(faces);
            }
        }
    };

    private SelfTimerListener mSelfTimerListener = new SelfTimerListener() {

        @Override
        public void onTimerStart() { }

        @Override
        public void onTimerStop() { }

        @Override
        public void onTimerTimeout() {
            onShutterButtonClick(null);
        }
    };

    public boolean onUserInteraction() {
        mCamera.keepScreenOnAwhile();
        return true;
    }

    private ShutterCallback mShutterCallback = new ShutterCallback() {
        @Override
        public void onShutter() {
            if (LOG) {
                Log.v(TAG, "ShutterCallback onShutter mContinuousShotPerformed=" + mContinuousShotPerformed
                        + " mStreamID=" + mStreamID);
            }
            calculateShutterTime();
            // start play sound only at first time when continuous shot
            if (mContinuousShotPerformed && mStreamID == 0) {
                mContinuousShotStartTime = System.currentTimeMillis();
                mStreamID = mBurstSound.play(mSoundID, 1.0f, 1.0f, 1, -1, 1.0f);
            }
        }
    };

    protected void calculateShutterTime() {
        mShutterCallbackTime = System.currentTimeMillis();
        mShutterLag = mShutterCallbackTime - mCaptureStartTime;
        if (LOG) {
            Log.v(TAG, "mShutterLag = " + mShutterLag + "ms");
        }
    }

    private PictureCallback mPostViewPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            mPostViewPictureCallbackTime = System.currentTimeMillis();
            if (LOG) {
                Log.v(TAG, "mShutterToPostViewCallbackTime = "
                        + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                        + "ms");
            }
        }
    };

    private PictureCallback mRawPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] rawData,
                android.hardware.Camera camera) {
            if (LOG) {
                Log.v(TAG, "RawPictureCallback onPictureTaken rawData=" + rawData);
            }
            mRawPictureCallbackTime = System.currentTimeMillis();
            if (LOG) {
                Log.v(TAG, "mShutterToRawCallbackTime = "
                        + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
            }
        }
    };

    @Override
    public boolean capture() {
        long start = System.currentTimeMillis();
        if (LOG) {
            Log.v(TAG, "capture begin");
        }
        // If we are already in the middle of taking a snapshot then ignore.
        if (mCamera.getCameraDevice() == null
                || mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS
                || mCamera.getCameraState() == Camera.STATE_SWITCHING_CAMERA) {
            if (mCamera.getCameraDevice() == null) {
                mCamera.restoreViewState();
            }
            return false;
        }
        MMProfileManager.startProfileTakePicture();
        mCapturing = true;
        mCaptureStartTime = System.currentTimeMillis();
        mPostViewPictureCallbackTime = 0;
        mJpegImageData = null;
        mSaveRequest = mContext.preparePhotoRequest();
        if (mContinuousShotPerformed) {
            mStreamID = 0;
        }
        mCameraCategory.ensureCaptureTempPath();
        if(mZSDEnabled) {
            mCameraCategory.animateCapture(mCamera);
        }
        synchronized (sFaceDetectionSync) {
            if (!mCameraCategory.applySpecialCapture()) {
                mContext.getCameraDevice().takePicture(getShutterCallback(), mRawPictureCallback,
                        mPostViewPictureCallback, mCameraCategory.getJpegPictureCallback());
            }
            sFaceDetectionStarted = false;
        }
        if(!mZSDEnabled) {
            mCameraCategory.animateCapture(mCamera);
        }
        
        mCamera.setCameraState(Camera.STATE_SNAPSHOT_IN_PROGRESS);
        mCamera.setSwipingEnabled(false);
        mCamera.showRemaining();
        mCamera.setViewState(mContinuousShotPerformed ? Camera.VIEW_STATE_CONTINIUOUS : Camera.VIEW_STATE_CAPTURE);
        if (LOG) {
            Log.i(TAG, "Capture time = " + (System.currentTimeMillis() - start));
        }
        MMProfileManager.stopProfileTakePicture();
        return true;
    }

    public ContinuousShotDone getContinuousShotDone() {
        return this;
    }

    public ShutterCallback getShutterCallback() {
        return mShutterCallback;
    }

    public android.hardware.Camera.AutoFocusMoveCallback getAutoFocusMoveCallback() {
        if (LOG) {
            Log.v(TAG, "PhotoActor.getAutoFocusMoveCallback");
        }
        return mAutoFocusMoveCallback;
    }

    private PictureCallback mContinuousJpegPictureCallback = new PictureCallback() {

        public void onPictureTaken(final byte[] data, android.hardware.Camera camera) {
            if (LOG) {
                Log.w(TAG, "PhotoActor.ContinuousShot.onPictureTaken");
            }
            MMProfileManager.startProfileStorePicture();
            if (mContinuousShotPerformed && data == null) { // cannot allocate enough memory for picture, stop shot
                if (LOG) {
                    Log.w(TAG, "onPictureTaken(" + data + ") stop shot!");
                }
                onShutterButtonFocus(null, false);
                mIgnoreClick = true;
            } else if (mContinuousShotPerformed && !mCameraClosed) {
                if (!mCameraCategory.canshot()) {
                    onShutterButtonFocus(null, false);
                }
                setSaveRequest(data);
                mSaveRequest = mContext.getFileSaver().copyPhotoRequest(mSaveRequest);
                // Not adding number when capture canceled
                mCurrentShotsNum++;
                mMemoryManager.refresh(data.length);
                if (mCurrentShotsNum == mMaxCaptureNum || mMemoryManager.isNeedStopCapture()) {
                    onShutterButtonFocus(null, false);
                    mIgnoreClick = true;
                } else if (mMemoryManager.isNeedSlowDown(mCamera.getFileSaver().getWaitingDataSize())) {
                    mCamera.getCameraDevice().setContinuousShotSpeed(mMemoryManager.getSuitableContinuousShotSpeed());
                }
            } else {
                if (LOG) {
                    Log.i(TAG, "received onPictureTaken, but mCameraClosed=" + mCameraClosed
                            + " or mContinuousShotPerformed=" + mContinuousShotPerformed + ", ignore it");
                }
            }
            MMProfileManager.stopProfileStorePicture();
            if (LOG) {
                Log.i(TAG, "Continuous Shot, onPictureTaken: mCurrentShotsNum = " + mCurrentShotsNum
                        + " mContinuousShotPerformed = " + mContinuousShotPerformed);
                Log.i(TAG, "Continuous Shot, speed = " + mMemoryManager.getSuitableContinuousShotSpeed());
            }
        }
    };

    private PictureCallback mJpegPictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] jpegData,
                final android.hardware.Camera camera) {
            if (LOG) {
                Log.v(TAG, "JpegPictureCallback onPictureTaken jpegData=" + jpegData
                        + " mCameraClosed=" + mCameraClosed);
            }
            mKeyHalfPressed = false;
            if (mCameraClosed || jpegData == null) {
                mCamera.restoreViewState();
                mCamera.setSwipingEnabled(true);
                if (!mCameraClosed) {
                    //M: restart preview even we fail to captured a picture
                    restartPreview(false);
                }
                return;
            }

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mShutterToPictureDisplayedTime = mPostViewPictureCallbackTime
                        - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime = mJpegPictureCallbackTime
                        - mPostViewPictureCallbackTime;
            } else {
                mShutterToPictureDisplayedTime = mRawPictureCallbackTime
                        - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime = mJpegPictureCallbackTime
                        - mRawPictureCallbackTime;
            }
            if (LOG) {
                Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                        + mPictureDisplayedToJpegCallbackTime + "ms");
            }
            MMProfileManager.startProfileStorePicture();

            mCamera.getFocusManager().updateFocusUI(); // Ensure focus indicator is hidden.
            if (!mCamera.isImageCaptureIntent()) {
                IFeatureExtension previewFeature = ExtensionHelper.getFeatureExtension();
                if (previewFeature.isDelayRestartPreview()) {
                     long delay = IMAGE_DISPLAY_DURATION - mPictureDisplayedToJpegCallbackTime;
                     if (delay <= 0) {
                         restartPreview(true);
                     } else {
                         mHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (!mCameraClosed) {
                                    restartPreview(true);
                                }
                            }
                         }, delay);
                     }
                } else {
                    restartPreview(true);
                }
            }
            mCapturing = false;

            if (!mCamera.isImageCaptureIntent()) {
                // Calculate the width and the height of the jpeg.
                setSaveRequest(jpegData);
                mCameraCategory.doOnPictureTaken();
            } else {
                mJpegImageData = jpegData;
                if (!mCamera.isQuickCapture()) {
                    mCamera.showReview();
                    mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_OK_CANCEL);
                    mCamera.setViewState(Camera.VIEW_STATE_PICKING);
                } else {
                    doAttach();
                }
            }

            long now = System.currentTimeMillis();
            mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
            if (LOG) {
                Log.v(TAG, "mJpegCallbackFinishTime = " + mJpegCallbackFinishTime + "ms");
            }
            MMProfileManager.stopProfileStorePicture();
            mJpegPictureCallbackTime = 0;
        }
    };

    protected void restartPreview(boolean needStop) {
        sIsAutoFocusCallback = false;
        startPreview(needStop);
        mCamera.setCameraState(Camera.STATE_IDLE);
        mCamera.restoreViewState();
        mCamera.setSwipingEnabled(true);
        startFaceDetection();
    }

    private final class AutoFocusCallback implements
            android.hardware.Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(boolean focused, android.hardware.Camera camera) {
            if (mCameraClosed) {
                return;
            }

            mAutoFocusTime = System.currentTimeMillis() - mFocusStartTime;
            if (LOG) {
                Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");
            }
            if (!mSelftimerCounting && mCamera.getCameraState() == Camera.STATE_FOCUSING) {
                mCamera.setCameraState(Camera.STATE_IDLE);
                mCamera.restoreViewState();
            }
            mCamera.getFocusManager().onAutoFocus(focused);
            sIsAutoFocusCallback = true;
        }
    }

    private final class AutoFocusMoveCallback implements
            android.hardware.Camera.AutoFocusMoveCallback {
        @Override
        public void onAutoFocusMoving(boolean moving,
                android.hardware.Camera camera) {
            if (LOG) {
                Log.v(TAG, "onAutoFocusMoving");
            }
            mCamera.getFocusManager().onAutoFocusMoving(moving);
        }
    }

    @Override
    public void autoFocus() {
        if (LOG) {
            Log.v(TAG, "autoFocus");
        }
        mFocusStartTime = System.currentTimeMillis();
        mCamera.getCameraDevice().autoFocus(mAutoFocusCallback);
        mCamera.setCameraState(Camera.STATE_FOCUSING);
        mCamera.setViewState(Camera.VIEW_STATE_FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        if (LOG) {
            Log.i(TAG, "cancelAutoFocus");
        }
        mCamera.getCameraDevice().cancelAutoFocus();
        if (!mSelftimerCounting && mCamera.getCameraState() != Camera.STATE_SNAPSHOT_IN_PROGRESS) {
            mCamera.setCameraState(Camera.STATE_IDLE);
            mCamera.restoreViewState();
        }
        setFocusParameters();
    }

    public boolean doSmileShutter() {
        return false;
    }

    @Override
    public void playSound(int soundId) {
        mCameraSound.play(soundId);
    }

    @Override
    public void setFocusParameters() {
        if (LOG) {
            Log.i(TAG, "setFocusParameters sIsAutoFocusCallback =" + sIsAutoFocusCallback);
        }
        mCamera.applyParameterForFocus(!sIsAutoFocusCallback);
        sIsAutoFocusCallback = false;
    }

    public Listener getFocusManagerListener() {
        return this;
    }

    public void onCameraClose() {
        mCameraClosed = true;
        if (LOG) {
            Log.i(TAG, "onCameraClose mCameraClosed =" + mCameraClosed);
        }
        // remove message, it will restart after onresume->reopen->restartpreview
        mHandler.removeMessages(START_PREVIEW_DONE);
        mHandler.removeMessages(PARAMETER_CHANGE_DONE);

        resetPhotoActor();
        mCameraCategory.onLeaveActor();
        stopPreview();
    }

    @Override
    public void release() {
        // remove message, it will be unused after release
        mHandler.removeMessages(START_PREVIEW_DONE);
        mHandler.removeMessages(PARAMETER_CHANGE_DONE);

        if (mBurstSound != null) {
            mBurstSound.unload(mSoundID);
        }
//        stopFaceDetection();
        resetPhotoActor();
        mCameraCategory.onLeaveActor();
    }

    private boolean isImageCaptureIntent() {
        String action = mContext.getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action));
    }

    @Override
    public int getMode() {
        return ModePicker.MODE_PHOTO;
    }
    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return this;
    }
    @Override
    public FaceDetectionListener getFaceDetectionListener() {
        return mFaceDetectionListener;
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
    public OnClickListener getRetakeListener() {
        return mRetakeListener;
    }
    @Override
    public ErrorCallback getErrorCallback() {
        return new CameraErrorCallback();
    }

    @Override
    public OnSingleTapUpListener getonSingleTapUpListener() {
        return mOnSingleTapListener;
    }

    @Override
    public void onCameraOpenDone() {
        if (LOG) {
            Log.v(TAG, "onCameraOpenDone");
        }
        mCameraClosed = false;
        // avoid close camera in videoactor
        sFaceDetectionStarted = false;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        super.onCameraParameterReady(startPreview);
        if (LOG) {
            Log.v(TAG, "onCameraParameterReady startPreview=" + startPreview);
        }
        if (startPreview) {
            startPreview(true);
            mHandler.sendEmptyMessage(START_PREVIEW_DONE);
        } else {
            mHandler.sendEmptyMessage(PARAMETER_CHANGE_DONE);
        }
    }

    public void initializeAfterPreview() {
        if (LOG) {
            Log.v(TAG, "initializeAfterPreview mCamera.getCameraDevice()=" + mCamera.getCameraDevice());
        }
        if (mCameraClosed || mCamera.getCameraDevice() == null) {
            return;
        }
        // for auto focus moving callback
        sIsAutoFocusCallback = false;
        // for selfTimer
        String selfTimer = mCamera.getSelfTimer();
        SelfTimerManager.getInstance(mCamera.getMainLooper()).setSelfTimerDuration(selfTimer);
        SelfTimerManager.getInstance(mCamera.getMainLooper()).setTimerListener(mSelfTimerListener);
        // for ZSD
        mZSDEnabled = "on".equals(mCamera.getSettingChecker().getParameterValue(
                SettingChecker.ROW_SETTING_ZSD));
        // for continuous shot
        if (getMode() == ModePicker.MODE_PHOTO) {
            mMaxCaptureNum = Integer.valueOf(mCamera.getSettingChecker().getPreferenceValue(
                    SettingChecker.ROW_SETTING_CONTINUOUS));
        }
        mSupportContinuous = isSupportContinuousShot();
        // keep preview screen on in times( DELAY_MSG_SCREEN_SWITCH )
        mCamera.keepScreenOnAwhile();
        // for capture intent
        mCameraCategory.switchShutterButton();
        // face Detection
        if (isSupportFaceDetect()) {
            startFaceDetection();
        } else {
            stopFaceDetection();
        }
        // for LOG
        if (LOG) {
            Log.v(TAG, "selfTimer=" + selfTimer);
        }

        if (mInitialized) {
            return;
        }
        // The next steps will be excuted only at the first time.
        initializeFaceView();
        mCameraCategory.initializeFirstTime();
        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            // Not required, but reduces latency when playback is requested later.
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }

        mInitialized = true;
    }

    @Override
    public void stopPreview() {
        if (LOG) {
            Log.v(TAG, "stopPreview() mCamera.getCameraState()=" + mCamera.getCameraState());
        }
        if (mCamera.getCameraState() != Camera.STATE_PREVIEW_STOPPED
                && (!mZSDEnabled || (mZSDEnabled && mCamera.getCameraState() != Camera.STATE_SNAPSHOT_IN_PROGRESS))) {
            //stop preview may not be called in main thread,we need to synchronized "stopPreview" and "sFaceDetectionStarted = false"
            //Exception Case: touch focus between onCamearOpenDone and startPreview Done,press home key to exit camera and then enter immediately
            synchronized (sFaceDetectionSync) {
                if (mCamera.getCameraDevice() != null) {
                    mCamera.getCameraDevice().cancelAutoFocus(); // Reset the focus.
                    mCamera.getCameraDevice().stopPreview();
                }
                sFaceDetectionStarted = false;
            }
            mCamera.setCameraState(Camera.STATE_PREVIEW_STOPPED);
            if (mCamera.getFocusManager() != null) {
                mCamera.getFocusManager().onPreviewStopped();
            }
        }
    }

    public void startPreview(boolean needStop) {
        if (LOG) {
            Log.v(TAG, "PhotoActor.startPreview");
        }
        mCamera.runOnUiThread(new Runnable() {
            public void run() {
                mCamera.getFocusManager().resetTouchFocus();
            }
        });
        // continuous shot neednot stop preview after capture
        if (needStop) {
            stopPreview();
        }

        if (!mSnapshotOnIdle) {
            // If the focus mode is continuous autofocus, call cancelAutoFocus
            // to
            // resume it because it may have been paused by autoFocus call.
            if (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mCamera.getFocusManager()
                    .getFocusMode())) {
                mCamera.getCameraDevice().cancelAutoFocus();
            }
            mCamera.getFocusManager().setAeLock(false); // Unlock AE and AWB.
            mCamera.getFocusManager().setAwbLock(false);
        }
        setFocusParameters();
        mCamera.getCameraDevice().startPreviewAsync();
        mCamera.getFocusManager().onPreviewStarted();
        if (mSnapshotOnIdle) {
            mHandler.post(mDoSnapRunnable);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (LOG) {
            Log.v(TAG, "onBackPressed() isFinishing()=" + mCamera.isFinishing());
        }
        if (!isCameraIdle()) {
            if (mSelftimerCounting) {
                SelfTimerManager.getInstance(mCamera.getMainLooper()).breakTimer();
                mSelftimerCounting = false;
                mCamera.setCameraState(Camera.STATE_IDLE);
                mCamera.setSwipingEnabled(true);
                mCamera.restoreViewState();
            }
            if (mCamera.isImageCaptureIntent()
                    && mCamera.getShutterManager().getShutterType() == ShutterManager.SHUTTER_TYPE_OK_CANCEL) {
                mCancelListener.onClick(null);
            }
            // ignore backs while we're taking a picture
            return true;
        } else if (mCameraCategory.doCancelCapture()) {
            // just cancel smile searching state.
            return true;
        }
        return false;
    }

    public boolean isCameraPrepareDone() {
        // Copy from onshutterButonClick. Using for checking state before dosnap in ModeActor.
        if (LOG) {
            Log.i(TAG, "Check camera state in ModeActor, mCameraState=" + mCamera.getCameraState()
                    + " mCameraClosed=" + mCameraClosed);
        }
        int cameraState = mCamera.getCameraState();
        if (mCameraClosed
                || (cameraState == Camera.STATE_SWITCHING_CAMERA)
                || (cameraState == Camera.STATE_PREVIEW_STOPPED)) {
            return false;
        }

        // Do not take the picture if there is not enough storage.
        if (!mCameraCategory.canshot()) {
            if (LOG) {
                Log.i(TAG, "Not enough space or storage not ready.");
            }
            return false;
        }

        if (SelfTimerManager.getInstance(mCamera.getMainLooper()).isSelfTimerCounting()) {
            return false;
        }

        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        if ((mCamera.getFocusManager().isFocusingSnapOnFinish()
                || (cameraState == Camera.STATE_SNAPSHOT_IN_PROGRESS || mSavingPictures))
                && !mCamera.isImageCaptureIntent()) {
            mSnapshotOnIdle = true;
            return false;
        }

        if (isBusy()) {
            mCamera.showInfo(mCamera.getString(R.string.camera_saving_busy));
            return false;
        }

        mSnapshotOnIdle = false;
        return true;
    }

    private boolean canTakePicture() {
        return isCameraIdle() && (mCameraCategory.canshot());
    }

    private boolean isCameraIdle() {
        if (LOG) {
            Log.v(TAG, "mCamera.getCameraState()=" + mCamera.getCameraState()
                    + " mCamera.getFocusManager()=" + mCamera.getFocusManager());
        }
        return !mSelftimerCounting && ((mCamera.getCameraState() == Camera.STATE_IDLE)
                || ((mCamera.getFocusManager() != null)
                        && mCamera.getFocusManager().isFocusCompleted()
                        && (mCamera.getCameraState() != Camera.STATE_SWITCHING_CAMERA)));
    }

    public void enableCameraControls(boolean enable) {
    }

    private void doCancel() {
        mCamera.setResultExAndFinish(Activity.RESULT_CANCELED, new Intent());
    }

    private void doAttach() {
        if (mCameraClosed) {
            return;
        }

        byte[] data = mJpegImageData;
        // M: insert record into Media DB.
        setSaveRequest(data);
        Uri saveUri = mCamera.getSaveUri();
        String cropValue = mCamera.getCropValue();

        if (cropValue == null) {
            // First handle the no crop case -- just return the value. If the
            // caller specifies a "save uri" then write the data to its
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (saveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mCamera.getContentResolver().openOutputStream(saveUri);
                    if (outputStream != null) {
                        outputStream.write(data);
                        outputStream.close();
                    }

                    mCamera.setResultExAndFinish(Activity.RESULT_OK);
                } catch (IOException ex) {
                    if (LOG) {
                        Log.v(TAG, "IOException, when doAttach");
                    }
                    // ignore exception
                } finally {
                    Util.closeSilently(outputStream);
                }
            } else {
                int orientation = Exif.getOrientation(data);
                Bitmap bitmap = Util.makeBitmap(data, 50 * 1024);
                bitmap = Util.rotate(bitmap, orientation);
                mCamera.setResultExAndFinish(Activity.RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = mCamera.getFileStreamPath(TEMP_CROP_FILE_NAME);
                path.delete();
                tempStream = mCamera.openFileOutput(TEMP_CROP_FILE_NAME, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                mCamera.setResultExAndFinish(Activity.RESULT_CANCELED);
                return;
            } catch (IOException ex) {
                mCamera.setResultExAndFinish(Activity.RESULT_CANCELED);
                return;
            } finally {
                Util.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (cropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (saveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, saveUri);
            } else {
                newExtras.putBoolean("return-data", true);
            }

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            mCamera.startActivityForResult(cropIntent, REQUEST_CROP);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (LOG) {
            Log.v(TAG, "onKeyDown keyCode=" + keyCode);
        }
        switch (keyCode) {
        case KeyEvent.KEYCODE_FOCUS:
            if (mInitialized && mCamera.isFullScreen() && event.getRepeatCount() == 0) {
                // onShutterButtonFocus(true);
                mCamera.collapseViewManager(true);
                if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS
                        || SelfTimerManager.getInstance(mCamera.getMainLooper()).isSelfTimerEnabled()) {
                    return true;
                }

                // Do not do focus if there is not enough storage.
                if (!canTakePicture()) {
                    return true;
                }
                mKeyHalfPressed = true;
                mCamera.getFocusManager().onShutterDown();
            }
            return true;
        case KeyEvent.KEYCODE_CAMERA:
            if (mInitialized && mCamera.isFullScreen() && !mCameraKeyLongPressed && event.getRepeatCount() > 0) {
                // M: Delay capturing action to make sure orientation is in correct state.
                if (mCamera.getOrietation() == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return false;
                }
                onShutterButtonLongPressed(null);
                mCameraKeyLongPressed = true;
            }
            return true;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            mCamera.collapseViewManager(true);
            // If we get a dpad center event without any focused view, move
            // the focus to the shutter button and press it.
            if (mInitialized && event.getRepeatCount() == 0) {
                // Start auto-focus immediately to reduce shutter lag. After
                // the shutter button gets the focus, onShutterButtonFocus()
                // will be called again but it is fine.
                onShutterButtonFocus(null, true);
                if (mCamera.getShutterManager().getPhotoShutter().isInTouchMode()) {
                    mCamera.getShutterManager().getPhotoShutter().requestFocusFromTouch();
                } else {
                    mCamera.getShutterManager().getPhotoShutter().requestFocus();
                }
                mCamera.getShutterManager().getPhotoShutter().setPressed(true);
            }
            return true;
        default:
            break;
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (LOG) {
            Log.v(TAG, "onKeyUp keyCode=" + keyCode);
        }
        switch (keyCode) {
        case KeyEvent.KEYCODE_FOCUS:
            if (mInitialized) {
                onShutterButtonFocus(null, false);
                if (mCamera.getCameraState() == Camera.STATE_SNAPSHOT_IN_PROGRESS
                        || SelfTimerManager.getInstance(mCamera.getMainLooper()).isSelfTimerEnabled()) {
                    return true;
                }
                mKeyHalfPressed = false;
                mCamera.getFocusManager().onShutterUp();
            }
            return true;
        case KeyEvent.KEYCODE_CAMERA:
            if (mInitialized && !mCameraKeyLongPressed
                    && event.getRepeatCount() == 0
                    && mCamera.isFullScreen()) {
                // M: Delay capturing action to make sure orientation is in correct state.
                if (mCamera.getOrietation() == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return false;
                }
//                mCamera.setSwipingEnabled(false);
                onShutterButtonClick(null);
            }
            mCameraKeyLongPressed = false;
            return true;
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean handleFocus() {
        if (mKeyHalfPressed) {
            overrideFocusMode(Parameters.FOCUS_MODE_AUTO);
            return true;
        } else {
            overrideFocusMode(null);
            return false;
        }
    }

    protected void overrideFocusMode(String focusMode) {
        if (focusMode != null && !SettingChecker.isSupported(focusMode,
                mCamera.getParameters().getSupportedFocusModes())) {
            focusMode = Parameters.FOCUS_MODE_INFINITY;
        }
        if (!mCameraClosed && mCamera.getFocusManager() != null) {
            mCamera.getFocusManager().overrideFocusMode(focusMode);
        }
    }

    public void interruptRenderThread() {
        if (mRenderThread != null) {
            mRenderThread.interrupt();
            mRenderThread = null;
        }
    }

    public void startFaceDetection() {
        if (LOG) {
            Log.v(TAG, "startFaceDetection sFaceDetectionStarted=" + sFaceDetectionStarted);
        }
        synchronized (sFaceDetectionSync) {
            if (!isSupportFaceDetect() || sFaceDetectionStarted || !mCameraCategory.enableFD(mCamera)) {
                return;
            }
            if (mCamera.getCameraDevice() != null && mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
                sFaceDetectionStarted = true;
                initializeFaceView();
//                mCamera.getCameraDevice().setFaceDetectionListener(getFaceDetectionListener());
                mCamera.getCameraDevice().startFaceDetection();
            }
        }
    }

    public void stopFaceDetection() {
        if (LOG) {
            Log.v(TAG, "stopFaceDetection sFaceDetectionStarted=" + sFaceDetectionStarted);
        }
        synchronized (sFaceDetectionSync) {
            if (!sFaceDetectionStarted) {
                return;
            }
            if (mCamera.getCameraDevice() != null && mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
                sFaceDetectionStarted = false;
                mCamera.getCameraDevice().stopFaceDetection();
                if (mCamera.getFaceView() != null) {
                    mCamera.getFaceView().clear();
                }
            }
        }
    }

    private boolean isBusy() {
        return mCamera.getFileSaver().getWaitingCount() > 3;
    }

    public void initializeFaceView() {
        if (LOG) {
            Log.v(TAG, "initializeFaceView sFaceDetectionStarted=" + sFaceDetectionStarted);
        }
        if (!sFaceDetectionStarted) {
            return;
        }
        FaceView faceView = mCamera.getFaceView();
        faceView.clear();
        faceView.setVisibility(View.VISIBLE);
        faceView.setDisplayOrientation(mCamera.getDisplayOrientation());
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCamera.getCameraId()];
        faceView.setMirror(info.facing == CameraInfo.CAMERA_FACING_FRONT);
        faceView.resume();
        mCamera.getFocusManager().setFaceView(faceView);
    }

    private void setSaveRequest(byte[] jpegData) {
        mSaveRequest.setData(jpegData);
        mSaveRequest.addRequest();
    }

    public boolean readyToCapture() {
        return true;
    }

    protected void onPreviewStartDone() {
        if (mCameraClosed) {
            return;
        }
        mCamera.setCameraState(Camera.STATE_IDLE);
        if (!mCamera.isImageCaptureIntent() && mWaitSavingDoneThread != null && mWaitSavingDoneThread.isAlive()) {
            mSavingPictures = true;
        }
    }

    public void onConinuousShotDone(int capNum) {
        if (LOG) {
            Log.i(TAG, "onContinuousShotDone, pictures saved = " + capNum);
        }
        mWaitSavingDoneThread = new WaitSavingDoneThread();
        mWaitSavingDoneThread.start();
        updateSavingHint(true, true);
    }

    /**
     * <p>
     * onScreen hint for saving
     */
    protected void updateSavingHint(boolean bSaving, boolean shotDone) {
        if (LOG) {
            Log.i(TAG, "updateSavingHint, saving = " + bSaving + " shotDone = " + shotDone);
        }
        if (bSaving) {
            if (!shotDone) {
                mCamera.showProgress(mCamera.getString(R.string.saving));
            } else if (mCurrentShotsNum != 0) {
                // add mCurrentShotsNum!=0 check
                // to avoid for scene of quickly press up before continuous sound is playing.
                mCamera.showProgress(String.format(Locale.ENGLISH,
                        mCamera.getString(R.string.continuous_saving_pictures), mCurrentShotsNum));
            }
        } else {
            mCamera.dismissProgress();
            mCamera.setSwipingEnabled(true);
        }
    }

    private boolean isSupportContinuousShot() {
        List<String> supportedCaptureMode = mCamera.getParameters().getSupportedCaptureMode();
        return supportedCaptureMode == null ? false :
            supportedCaptureMode.indexOf(Parameters.CAPTURE_MODE_CONTINUOUS_SHOT) >= 0;
    }

    private boolean isSupportFaceDetect() {
        String faceDetection = mCamera.getSettingChecker().getSettingCurrentValue(
                SettingChecker.ROW_SETTING_CAMERA_FACE_DETECT);
        if (LOG) {
            Log.v(TAG, "isSupportFaceDetect faceDetection=" + faceDetection);
        }
        return "on".equals(faceDetection);
    }

    public void onBurstSaveDone() {
        if (!mCameraClosed && !mCamera.isImageCaptureIntent()) {
            mCamera.cancelContinuousShot();
            restartPreview(false);
        }
        mSavingPictures = false;
    }

    public void onImagePickSaveDone() {
        if (!mCameraClosed) {
            Intent intent = mCamera.getIntent();
            intent.setData(mSaveRequest.getUri());
            mCamera.setResultExAndFinish(Activity.RESULT_OK, intent);
        }
    }

    protected void resetPhotoActor() {
        sIsAutoFocusCallback = false;
        if (mSelftimerCounting) {
            SelfTimerManager.getInstance(mCamera.getMainLooper()).breakTimer();
            mSelftimerCounting = false;
        }
        mCamera.dismissInfo();
        mCamera.setSwipingEnabled(true);
    }

    protected class CameraCategory {
        public void initializeFirstTime() {
            mBurstSound = new SoundPool(10, AudioManager.STREAM_SYSTEM_ENFORCED, 0);
            mSoundID = mBurstSound.load("/system/media/audio/ui/camera_shutter.ogg", 1);
        }

        public void shutterPressed() {
            mCamera.getFocusManager().onShutterDown();
        }

        public void shutterUp() {
            mCamera.getFocusManager().onShutterUp();
        }
        public void switchShutterButton() {
            if (mCamera.isImageCaptureIntent()) {
                mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO);
            } else {
                mCamera.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
            }
        }

        public boolean canshot() {
            return 1 <= Storage.getLeftSpace();
        }

        public boolean supportContinuousShot() {
            return mSupportContinuous;
        }

        public boolean skipFocus() {
            return SKIP_FOCUS_ON_CAPTURE;
        }

        public void doShutter() { }

        public void ensureCaptureTempPath() { }

        public boolean applySpecialCapture() {
            // need override in single3d
            if (!mContinuousShotPerformed && mZSDEnabled && !mCamera.isImageCaptureIntent()) {
                mRenderThread = new RenderInCapture();
                mRenderThread.start();
                mCamera.getCameraDevice().setPreviewDoneCallback(mZSDPreviewDone);
            } else {
                mCamera.getCameraDevice().setPreviewDoneCallback(null);
            }
            return false;
        }

        public PictureCallback getJpegPictureCallback() {
            if (mContinuousShotPerformed) {
                return mContinuousJpegPictureCallback;
            } else {
                // Let onPictureTaken in Camera do work.
                return mJpegPictureCallback;
            }
        }

        public void doOnPictureTaken() {
            interruptRenderThread();
        }

        public void animateCapture(Camera camera) {
            if (!mContinuousShotPerformed) {
                if (!camera.isImageCaptureIntent()) {
                    // Start capture animation.
                    camera.animateCapture();
                }
            }
        }

        public boolean doCancelCapture() {
            return false;
        }

        public boolean enableFD(Camera camera) {
            CameraInfo info = CameraHolder.instance().getCameraInfo()[camera.getCameraId()];
            return true;//info.facing != CameraInfo.CAMERA_FACING_FRONT;
        }

        public void onLeaveActor() {
            if (LOG) {
                Log.v(TAG, "onLeaveActor mContinuousShotPerformed=" + mContinuousShotPerformed
                        + " mSavingPictures=" + mSavingPictures);
            }
            interruptRenderThread();
            if (mContinuousShotPerformed) {
                mContinuousShotPerformed = false;
                mSavingPictures = true;
                cancelContinuousShot();
            }
            if (mSavingPictures) {
                // avoid for received onFocus(press = false) and cannot receive onCsshutdone
                if (mWaitSavingDoneThread == null || !mWaitSavingDoneThread.isAlive()) {
                    mWaitSavingDoneThread = new WaitSavingDoneThread();
                    mWaitSavingDoneThread.start();
                    updateSavingHint(true, true);
                }
            } else {
                mCamera.restoreViewState();
            }
            if (mCamera.isImageCaptureIntent()) {
                mCamera.hideReview();
            }
        }
    }

    private class MemoryManager {
        /*
         * we assume that: sdcard speed: 2M/s, sensor speed: 1.5M * 5 1. init process memory: 24M 2. max memory: 64M + 24M 3.
         * min left memory: 10M 4. slow down memory for 1.5M * 40 - 2M * 8 picture: 44M
         * camera memory in 6589 is 128M, slow down memory could be 128*60%=80M
         */
        private static final long MIN_LEFT_MEMEORY = 10 * 1024 * 1024;
        private static final long SLOW_DOWN_LEFT_MEMORY = 80 * 1024 * 1024;
        private Runtime mRuntime = Runtime.getRuntime();
        private long mMaxMemory;
        private long mLeftStorage;
        private long mUsedStorage;

        public void initMemory() {
            mMaxMemory = mRuntime.maxMemory();
            mLeftStorage = getLeftStorage();
            mUsedStorage = 0;
            if (LOG) {
                Log.v(TAG, "initMemory() mMaxMemory=" + mMaxMemory);
            }
        }

        public void refresh(long currentPictureSize) {
            mUsedStorage += currentPictureSize;
        }

        // If memory used >= SLOW_DOWN_LEFT_MEMORY, slow the shot
        public boolean isNeedSlowDown(long dataSize) {
            boolean slow = false;
            if (dataSize >= SLOW_DOWN_LEFT_MEMORY) {
                slow = true;
            }
            if (LOG) {
                Log.v(TAG, "isNeedSlowDown(" + dataSize + ") return " + slow);
            }
            return slow;
        }

        public int getSuitableContinuousShotSpeed() {
            long timeduration = System.currentTimeMillis() - mContinuousShotStartTime;
            int speed = 0;
            int suitableSpeed = 0;
            if (mCurrentShotsNum > 0 && timeduration != 0) {
                speed = (int) (mCurrentShotsNum * 1000 / timeduration);
                suitableSpeed = speed * 3 / 5;
            }
            if (LOG) {
                Log.v(TAG, "getSuitableContinuousShotSpeed speed=" + speed
                        + "fps suitableSpeed=" + suitableSpeed + "fps");
            }
            return suitableSpeed;
        }

        // If memory free <= MIN_LEFT_MEMEORY, stop the shot
        public boolean isNeedStopCapture() {
            boolean stop = false;
            long total = mRuntime.totalMemory();
            long free = mRuntime.freeMemory();
            long realfree = mMaxMemory - (total - free);
            if (realfree <= MIN_LEFT_MEMEORY) {
                stop = true;
            } else if (mUsedStorage >= mLeftStorage) {
                stop = true;
            }
            if (LOG) {
                Log.v(TAG, "isNeedStopCapture() mMaxMemory=" + mMaxMemory + ", total=" + total + ", free=" + free
                        + ", real free=" + realfree + ", mUsedStorage=" + mUsedStorage + ", mLeftStorage=" + mLeftStorage
                        + ", return " + stop);
            }
            return stop;
        }

        private long getLeftStorage() {
            long pictureRemaining = Storage.getAvailableSpace() - Storage.LOW_STORAGE_THRESHOLD;
            return pictureRemaining;
        }

        public void logMemory(String title) {
            MemoryInfo mi = new MemoryInfo();
            android.os.Debug.getMemoryInfo(mi);
            String tagtitle = "logMemory() " + title;
            if (LOG) {
                Log.v(TAG, tagtitle + "         PrivateDirty    Pss     SharedDirty");
                Log.v(TAG, tagtitle + " dalvik: " + mi.dalvikPrivateDirty + ", " + mi.dalvikPss + ", "
                        + mi.dalvikSharedDirty + ".");
                Log.v(TAG, tagtitle + " native: " + mi.nativePrivateDirty + ", " + mi.nativePss + ", "
                        + mi.nativeSharedDirty + ".");
                Log.v(TAG, tagtitle + " other: " + mi.otherPrivateDirty + ", " + mi.otherPss + ", "
                        + mi.otherSharedDirty + ".");
                Log.v(TAG,
                        tagtitle + " total: " + mi.getTotalPrivateDirty() + ", " + mi.getTotalPss() + ", "
                                + mi.getTotalSharedDirty() + ".");
            }
        }
    }
}
