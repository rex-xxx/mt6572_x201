package com.android.camera.actor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.filterpacks.videosink.MediaRecorderStopException;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaRecorder.HDRecordMode;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore.Video;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.camera.Camera;
import com.android.camera.Camera.OnSingleTapUpListener;
import com.android.camera.CameraErrorCallback;
import com.android.camera.CameraHolder;
import com.android.camera.FeatureSwitcher;
import com.android.camera.FileSaver;
import com.android.camera.FocusManager;
import com.android.camera.FocusManager.Listener;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.Storage;
import com.android.camera.Util;
import com.android.camera.WfdManagerLocal;
import com.android.camera.manager.MMProfileManager;
import com.android.camera.manager.ModePicker;
import com.android.camera.manager.RecordingView;
import com.android.camera.manager.ShutterManager;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

import com.mediatek.camera.FrameworksClassFactory;
import com.mediatek.mock.media.MockMediaRecorder;
import com.mediatek.media.MediaRecorderEx;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class VideoActor extends CameraActor implements EffectsRecorder.EffectsListener, MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener, FocusManager.Listener, EffectsRecorder.OnSurfaceStateChangeListener {
    private static final String TAG = "VideoActor";
    private static final boolean LOG = Log.LOGV;
    private Camera mVideoContext;
    private MediaRecorder mMediaRecorder;
    
    private CamcorderProfile mProfile;
    private ContentResolver mContentResolver;
    private Location mStartLocation;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private RecordingView mRecordingView;
    private SaveRequest mVideoSaveRequest;
    private String mVideoFilename = null;
    private String mCurrentVideoFilename;
    private String mVideoTempPath;
    private Thread mVideoSavingTask;
    private Uri mCurrentVideoUri;
    private CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private boolean mMediaRecorderRecording = false;
    private boolean mMediaRecoderRecordingPaused = false;
    private boolean mRecorderCameraReleased = true;
    private boolean mVideoCameraClosed = false;
    // Time Lapse parameters.
    private boolean mCaptureTimeLapse = false;
    private boolean mRecordAudio = false;
    private boolean mConnectApiReady = true;
    private boolean mStartRecordingFailed = false;
    private long mVideoRecordedDuration = 0;
    private long mRecordingStartTime;
    private long mFocusStartTime;
    private int mCurrentShowIndicator = 0;
    private int mStoppingAction = STOP_NORMAL;
    // The video duration limit. 0 menas no limit.
    private int mMaxVideoDurationInMs;
    // Default 0. If it is larger than 0, the camcorder is in time lapse mode.
    private int mTimeBetweenTimeLapseFrameCaptureMs = 0;
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private final Handler mHandler = new MainHandler();

    private static final int STOP_NORMAL = 1;
    private static final int STOP_RETURN = 2;
    private static final int STOP_RETURN_UNVALID = 3;
    private static final int STOP_SHOW_ALERT = 4;
    private static final int STOP_FAIL = 5;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final long INVALID_DURATION = -1l;
    private static final long FILE_ERROR = -12;
    private static final int MEDIA_RECORDER_INFO_RECORDING_SIZE = 895;
    private static final String[] PREF_CAMERA_VIDEO_HD_RECORDING_ENTRYVALUES = { "normal", "indoor" };

    // touch AE/AF
    private boolean mIsAutoFocusCallback = false;
    private boolean mSingleStartRecording = false;
    private boolean mSingleAutoModeSupported;
    private boolean mIsContinousFocusMode;
    private int mFocusState = 0;
    private final AutoFocusMoveCallback mAutoFocusMoveCallback = new AutoFocusMoveCallback();
    private static final int FOCUSING = 1;
    private static final int FOCUSED = 2;
    private static final int FOCUS_IDLE = 3;
    private static final int START_FOCUSING = -1;

    // Effect background
    private EffectsRecorder mEffectsRecorder;
    private Object mEffectParameter = null;
    private String mEffectUriFromGallery = null;
    private int mEffectType = EffectsRecorder.EFFECT_NONE;
    private int mEffectApplyTime = Integer.MAX_VALUE;
    private long mRequestedSizeLimit = 0;
    private boolean mEffectsDisplayResult;
    private boolean mResetEffect = true;
    private boolean mEffectsError = false;
    private boolean mNeedReLearningEffect = false;
    // We number the request code from 1000 to avoid collision with Gallery.
    private static final int REQUEST_EFFECT_BACKDROPPER = 1000;
    private static final String EFFECT_BG_FROM_GALLERY = "gallery";
    // private LearningView mLearningView;

    // Snapshot feature
    private Uri mSnapUri;
    private SaveRequest mPhotoSaveRequest;
    private boolean mStopVideoRecording = false;
    private static final int UPDATE_SNAP_UI = 15;

    //MMS progress bar
    private long mTotalSize = 0;

    //WFD
    private WfdManagerLocal mWfdManager;
    private boolean mWfdListenerEnabled = false;

    private OnShutterButtonListener mVideoShutterListener = new OnShutterButtonListener() {

        @Override
        public void onShutterButtonLongPressed(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Video.onShutterButtonLongPressed(" + button + ")");
            }
        }

        @Override
        public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
            if (LOG) {
                Log.v(TAG, "Video.onShutterButtonFocus(" + button + ", " + pressed + ")");
            }
            if (FeatureSwitcher.isContinuousFocusEnabledWhenTouch()) {
                if (!pressed) {
                    mVideoContext.getFocusManager().onShutterUp();
                }
            }
        }

        @Override
        public void onShutterButtonClick(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Video.onShutterButtonClick(" + button + ") mMediaRecorderRecording=" + mMediaRecorderRecording);
            }
            MMProfileManager.triggerVideoShutterClick();
            // Do not recording if there is not enough storage.
            if (Storage.getLeftSpace() <= 0) {
                backToLastModeIfNeed();
                return;
            }
            if (mVideoCameraClosed) {
                return;
            }
            if (mMediaRecorderRecording) {
                MMProfileManager.startProfileStopVideoRecording();
                onStopVideoRecordingAsync();
                MMProfileManager.stopProfileStopVideoRecording();
            } else {
                MMProfileManager.startProfileStartVideoRecording();
                if (FeatureSwitcher.isContinuousFocusEnabledWhenTouch()) {
                    shutterPressed();
                }
                startVideoRecording();
                MMProfileManager.stopProfileStartVideoRecording();
                // we should enable swiping when cannot record
                if (!mMediaRecorderRecording) {
                    mVideoContext.setSwipingEnabled(true);
                }
            }
        }
    };

    private void shutterPressed() {
        if ((mVideoContext.getParameters()!=null)&&(isSupported(Parameters.FOCUS_MODE_AUTO,mVideoContext.getParameters().getSupportedFocusModes()))) {
        mVideoContext.getFocusManager().overrideFocusMode(Parameters.FOCUS_MODE_AUTO);
        mVideoContext.getFocusManager().onShutterDown();
        }
    }
    private OnShutterButtonListener mPhotoShutterListener = new OnShutterButtonListener() {

        @Override
        public void onShutterButtonLongPressed(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Photo.onShutterButtonLongPressed(" + button + ")");
            }
        }

        @Override
        public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
            if (LOG) {
                Log.v(TAG, "Photo.onShutterButtonFocus(" + button + ", " + pressed + ")");
            }
        }

        @Override
        public void onShutterButtonClick(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Photo.onShutterButtonClick(" + button + ")");
            }
            if (mStopVideoRecording) {
                return;
            }
            if (LOG) {
                Log.v(TAG, "Video snapshot start");
            }
            mPhotoSaveRequest = mVideoContext.preparePhotoRequest();
            mVideoContext.getCameraDevice().takePicture(null, null, null,
                    new JpegPictureCallback(mPhotoSaveRequest.getLocation()));
            showVideoSnapshotUI(true);
        }
    };

    public VideoActor(Camera context) {
        super(context);
        mVideoContext = getContext();
        if (!mVideoContext.isNonePickIntent()) {
            mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_VIDEO);
        } else {
            mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_PHOTO_VIDEO);
        }
        mRecordingView = new RecordingView(mVideoContext);
        mRecordingView.setListener(mVideoPauseResumeListner);
        int mFrontCameraId = CameraHolder.instance().getFrontCameraId();
        if (!((mVideoContext.getCameraId() == mFrontCameraId) || effectActive())) {
            mVideoContext.getShutterManager().setPhotoShutterEnabled(FeatureSwitcher.isVssEnabled());
            if (LOG) {
                Log.v(TAG, "FeatureSwitcher.isVssEnabled()= " + FeatureSwitcher.isVssEnabled());
            }
        } else {
            mVideoContext.getShutterManager().setPhotoShutterEnabled(false);
            if (LOG) {
                Log.v(TAG, "mVideoContext.getShutterManager().setPhotoShutterEnabled(false)");
            }
        }
        mWfdManager = mVideoContext.getWfdManagerLocal();
        if (mWfdManager != null) {
            mWfdManager.addListener(mWfdListener);
        }
    }

    private WfdManagerLocal.Listener mWfdListener = new WfdManagerLocal.Listener() {
        @Override
        public void onStateChanged(boolean enabled) {
            if (LOG) {
                Log.v(TAG, "onStateChanged(" + enabled + ")");
            }
            mWfdListenerEnabled = enabled;
            if (enabled && mMediaRecorderRecording) {
                onStopVideoRecordingAsync();
            } else {
                if (LOG) {
                    Log.v(TAG, "mWfdListener, enabled = " + enabled + ",mMediaRecorderRecording = "
                            + mMediaRecorderRecording);
                }
            }
        }
    };

    @Override
    public int getMode() {
        return ModePicker.MODE_VIDEO;
    }

    private OnSingleTapUpListener mTapupListener = new OnSingleTapUpListener() {
        public void onSingleTapUp(View view, int x, int y) {
            String focusMode = mVideoContext.getFocusManager().getCurrentFocusMode(mVideoContext);
            if (focusMode == null || (Parameters.FOCUS_MODE_INFINITY.equals(focusMode))
                    || (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(focusMode))) {
                return;
            }
            if (mVideoCameraClosed || mVideoContext.getCameraDevice() == null
                    || (mVideoContext.getCameraState() == Camera.STATE_PREVIEW_STOPPED)
                    || (mVideoContext.getViewState() == Camera.VIEW_STATE_LEARNING_VIDEO_EFFECTS)) {
                return;
            }
            // Check if metering area or focus area is supported.
            if (!(mVideoContext.getFocusManager().getFocusAreaSupported())
                    && !(mVideoContext.getFocusManager().getMeteringAreaSupported())) {
                return;
            }
            if (LOG) {
                Log.v(TAG, "onSingleTapUp(" + x + ", " + y + ")" + ",focusMode = " + focusMode
                        + ",mVideoContext.getCameraState() = " + mVideoContext.getCameraState()
                        + "mVideoContext.getViewState() = " + mVideoContext.getViewState()
                        + ",mMediaRecorderRecording = " + mMediaRecorderRecording);
            }
            if (mMediaRecorderRecording) {
                setFocusState(START_FOCUSING);
            }
            mVideoContext.getFocusManager().onSingleTapUp(x, y);
        }
    };

    public OnSingleTapUpListener getonSingleTapUpListener() {
        return mTapupListener;
    }

    @Override
    public OnShutterButtonListener getVideoShutterButtonListener() {
        if (LOG) {
            Log.v(TAG, "getVideoShutterButtonListener" + mVideoShutterListener);
        }
        return mVideoShutterListener;
    }

    @Override
    public OnShutterButtonListener getPhotoShutterButtonListener() {
        if (LOG) {
            Log.v(TAG, "getPhotoShutterButtonListener" + mPhotoShutterListener);
        }
        return mPhotoShutterListener;
    }

    @Override
    public void onCameraParameterReady(boolean startPreview) {
        if (LOG) {
            Log.v(TAG, "onCameraParameterReady(" + startPreview + ") getCameraState()=" + mVideoContext.getCameraState());
        }
        mVideoCameraClosed = false;
        mProfile = mVideoContext.getProfile();
        Util.checkNotNull(mProfile);
        initVideoRecordingFirst();
        if (!updateEffectSelection() && startPreview) {
            startPreview();
        }
        restoreReviewIfNeed();
    }
    
    private void restoreReviewIfNeed() {
        if (mVideoContext.getReviewManager().isShowing()) {
            //reopen file descriptor and show it(don't change view state in this case).
            if (!mVideoContext.isNonePickIntent() && mVideoFileDescriptor == null) {
                Uri saveUri = mVideoContext.getSaveUri();
                if (saveUri != null) {
                    try {
                        mVideoFileDescriptor = mContentResolver.openFileDescriptor(saveUri, "rw");
                    } catch (java.io.FileNotFoundException ex) {
                        Log.e(TAG, "initializeNormalRecorder()", ex);
                    }
                }
            }
            mVideoContext.runOnUiThread(new Runnable() {
               @Override
                public void run() {
                   if (mVideoFileDescriptor != null) {
                       mVideoContext.getReviewManager().show(mVideoFileDescriptor.getFileDescriptor());
                   } else if (mCurrentVideoFilename != null) {
                       mVideoContext.getReviewManager().show(mCurrentVideoFilename);
                   }
                } 
            });
        }
        if (LOG) {
            Log.v(TAG, "restoreReviewIfNeed() review show=" + mVideoContext.getReviewManager().isShowing() +
                    " mVideoFileDescriptor=" + mVideoFileDescriptor + ", mCurrentVideoFilename=" + mCurrentVideoFilename);
        }
    }

    private void startPreview() {
        mVideoContext.runOnUiThread(new Runnable() {
            public void run() {
                mVideoContext.getFocusManager().resetTouchFocus();
            }
        });
        stopPreview();
        if (mVideoContext.getCameraState() != Camera.STATE_PREVIEW_STOPPED) {
            if (effectActive() && mEffectsRecorder != null) {
                mEffectsRecorder.release();
                mEffectsRecorder = null;
            }
        }
        if (mVideoContext.isNonePickIntent()) {
            mVideoContext.applyContinousCallback();
        }
        if (LOG) {
            Log.v(TAG, "startPreview()");
        }
        try {
            if (!effectActive()) {
                mVideoContext.getCameraDevice().startPreviewAsync();
                mVideoContext.setCameraState(Camera.STATE_IDLE);
                mVideoContext.getFocusManager().onPreviewStarted();
            } else {
                initializeEffectsPreview();
                mEffectsRecorder.startPreview();
            }
        } catch (Throwable ex) { // need,Throwable
            releaseVideoActor();
            Log.e(TAG, "startPreview()", ex);
            return;
        }
    }

    @Override
    public void stopPreview() {
        if (LOG) {
            Log.v(TAG, "stopPreview() mVideoContext.getCameraState()=" + mVideoContext.getCameraState());
        }
        if (mVideoContext.getCameraState() != Camera.STATE_PREVIEW_STOPPED) {
            mVideoContext.getCameraDevice().stopPreview();
            mVideoContext.setCameraState(Camera.STATE_PREVIEW_STOPPED);
        }
    }

    private void initVideoRecordingFirst() {
        mContentResolver = mVideoContext.getContentResolver();
        mIsContinousFocusMode = mVideoContext.getFocusManager().getContinousFocusSupported();
        mTimeBetweenTimeLapseFrameCaptureMs = mVideoContext.getTimelapseMs();
        mCaptureTimeLapse = (mTimeBetweenTimeLapseFrameCaptureMs != 0);
        mRecordAudio = mVideoContext.getMicrophone();
        mSingleAutoModeSupported = FeatureSwitcher.isContinuousFocusEnabledWhenTouch();
        int seconds = mVideoContext.getLimitedDuration();
        mMaxVideoDurationInMs = 1000 * seconds;
        mVideoContext.runOnUiThread(new Runnable() {
            // to keep screen on in main thread
            @Override
            public void run() {
                mVideoContext.keepScreenOnAwhile();
            }
        });
        if (LOG) {
            Log.v(TAG, "initVideoRecordingFirst,mIsContinousFocusMode =" + mIsContinousFocusMode
                    + ",mTimeBetweenTimeLapseFrameCaptureMs =" + mTimeBetweenTimeLapseFrameCaptureMs + ",mRecordAudio = "
                    + mRecordAudio + ",mSingleAutoModeSupported = " + mSingleAutoModeSupported + ",mMaxVideoDurationInMs ="
                    + mMaxVideoDurationInMs);
        }
    }

    private void initializeNormalRecorder() {
        if (LOG) {
            Log.v(TAG, "initializeNormalRecorder()");
        }
        getRequestedSizeLimit();
        // mMediaRecorder = new MediaRecorder();
        mMediaRecorder = FrameworksClassFactory.getMediaRecorder();

        mVideoContext.getCameraDevice().unlock();
        if (!FrameworksClassFactory.isMockCamera()) {
            mMediaRecorder.setCamera(mVideoContext.getCameraDevice().getCamera().getInstance());
        } else {
            ((MockMediaRecorder) mMediaRecorder).setContext(mVideoContext);
        }
        if (!mCaptureTimeLapse && mRecordAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);

        if (!mCaptureTimeLapse && mRecordAudio) {
            mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
            mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            if (FeatureSwitcher.isHdRecordingEnabled()) {
                MediaRecorderEx.setHDRecordMode(mMediaRecorder, getRecordMode(mVideoContext.getAudioMode()), true);
            }
        }
        mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
        if (mCaptureTimeLapse) {
            mMediaRecorder.setCaptureRate((1000 / (double) mTimeBetweenTimeLapseFrameCaptureMs));
        }
        // Set output file.Try Uri in the intent first. If it doesn't exist, use our own instead.
        if (mVideoFileDescriptor != null) {
            mMediaRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
        } else {
            FileSaver filesaver = mVideoContext.getFileSaver();
            mVideoSaveRequest = filesaver.prepareVideoRequest(mProfile.fileFormat,
                    Integer.toString(mProfile.videoFrameWidth) + "x" + Integer.toString(mProfile.videoFrameHeight));
            mMediaRecorder.setOutputFile(mVideoSaveRequest.getTempFilePath());
            mVideoFilename = mVideoSaveRequest.getTempFilePath();
        }

        // Get location from saverequest firstly, if it is null,then get it from location manager.
        Location loc = null;
        if (mVideoSaveRequest != null) {
            loc = mVideoSaveRequest.getLocation();
        } else {
            loc = mVideoContext.getLocationManager().getCurrentLocation();
        }
        if (loc != null) {
            mMediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
        }

        mStartLocation = mVideoContext.getLocationManager().getCurrentLocation();
        if (mStartLocation != null) {
            float latitue = (float) mStartLocation.getLatitude();
            float longitude = (float) mStartLocation.getLongitude();
            mMediaRecorder.setLocation(latitue, longitude);
        }

        // Set maximum file size.
        long maxFileSize = Storage.getAvailableSpace() - Storage.RECORD_LOW_STORAGE_THRESHOLD;
        if (mRequestedSizeLimit > 0 && mRequestedSizeLimit < maxFileSize) {
            maxFileSize = mRequestedSizeLimit;
        }
        try {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        } catch (RuntimeException exception) { // need,RuntimeException
            // We are going to ignore failure of setMaxFileSize here, as
            // a) The composer selected may simply not support it, or
            // b) The underlying media framework may not handle 64-bit range
            // on the size restriction.
            Log.w(TAG, "initializeNormalRecorder()", exception);
        }
        // Set output file.Try Uri in the intent first. If it doesn't exist, use our own instead.
        if (mVideoFileDescriptor != null) {
            mMediaRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
        } else {
            generateVideoFilename(mProfile.fileFormat);
            mMediaRecorder.setOutputFile(mVideoFilename);
        }

        // See android.hardware.Camera.Parameters.setRotation for documentation.
        // Note that mOrientation here is the device orientation, which is the opposite of what
        // activity.getWindowManager().getDefaultDisplay().getRotation() would return,which is the orientation the graphics
        // need to rotate in order to render correctly.
        int rotation = 0;
        mOrientation = mVideoContext.getOrietation();
        if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            CameraInfo info = CameraHolder.instance().getCameraInfo()[mVideoContext.getCameraId()];
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - mOrientation + 360) % 360;
            } else { // back-facing camera
                rotation = (info.orientation + mOrientation) % 360;
            }
        } else {
            // Get the right original orientation
            CameraInfo info = CameraHolder.instance().getCameraInfo()[mVideoContext.getCameraId()];
            rotation = info.orientation;
        }
        mMediaRecorder.setOrientationHint(rotation);
        mVideoContext.setReviewOrientationCompensation(mVideoContext.getOrientationCompensation());
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare failed", e);
            releaseMediaRecorder();
            throw new RuntimeException(e);
        }
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setOnCameraReleasedListener(this);
    }

    private void generateVideoFilename(int outputFileFormat) {
        String mDisplayName = "videorecorder";
        int mFileType = Storage.FILE_TYPE_VIDEO;
        // Used when emailing.
        String filename = mDisplayName + convertOutputFormatToFileExt(outputFileFormat);
        mVideoTempPath = Storage.getFileDirectory() + '/' + filename + ".tmp";
        mVideoFilename = mVideoTempPath;
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }

    private boolean effectActive() {
        return mVideoContext.getEffectType() != EffectsRecorder.EFFECT_NONE;
    }

    private void startVideoRecording() {
        if (LOG) {
            Log.v(TAG, "startVideoRecording()");
        }
        if (mSingleAutoModeSupported && mIsContinousFocusMode) {
            mSingleStartRecording = true;
            setAutoFocusMode();
        }
        mVideoContext.setSwipingEnabled(false);
        hideOtherSettings(true);
        if (mVideoContext.getLimitedSize() > 0) {
            if (!effectActive()) {
                mTotalSize = mVideoContext.getLimitedSize();
                mRecordingView.setTotalSize(mTotalSize);
                mRecordingView.setCurrentSize(0L);
            }
            mRecordingView.setRecordingSizeVisible(!effectActive());
        }
        mRecordingView.setRecordingIndicator(true);
        mRecordingView.setPauseResumeVisible(!effectActive());
        mRecordingView.show();
        mCurrentVideoUri = null;
        if (effectActive()) {
            initializeEffectsRecording();
            if (mEffectsRecorder == null) {
                Log.e(TAG, "Fail to initialize effect recorder.", new Throwable());
                return;
            }
        } else {
            initializeNormalRecorder();
            if (mMediaRecorder == null) {
                Log.e(TAG, "Fail to initialize media recorder.", new Throwable());
                return;
            }
        }
        pauseAudioPlayback();
        if (effectActive()) {
            mVideoContext.getShutterManager().setCancelButtonEnabled(false);
            startEffectRecording();
        } else {
            startNormalRecording();
        }

        if (mStartRecordingFailed) {
            if (LOG) {
                Log.v(TAG, "mStartRecordingFailed.");
            }
            mStartRecordingFailed = false;
            mVideoContext.showToast(R.string.video_recording_error);
            backToLastTheseCase();
        }
        // Parameters may have been changed by media recorder when recording starts.
        // To reduce latency, we do not update mParameters during zoom.
        // Keep this up-to-date now. Otherwise, we may revert the video size unexpectedly.
        mVideoContext.lockRun(new Runnable() {
            @Override
            public void run() {
                mVideoContext.fetchParametersFromServer();
            }
        });
        // add for camera pause feature
        mMediaRecoderRecordingPaused = false;
        mVideoRecordedDuration = 0;
        mRecorderCameraReleased = false;
        mStoppingAction = STOP_NORMAL;
        if (mWfdListenerEnabled) {
            if (!effectActive()) {
                releaseMediaRecorder();
            } else {
                releaseEffectsRecorder();
            }
            backToLastTheseCase();
        }
        mMediaRecorderRecording = true;
        mVideoContext.getShutterManager().setVideoShutterMask(true);
        mVideoContext.setCameraState(Camera.STATE_RECORDING_IN_PROGRESS);
        mRecordingStartTime = SystemClock.uptimeMillis();
        updateRecordingTime();
        if (!mCaptureTimeLapse) {
            // we just update recordingtime once,because it will be called in onInfo(xxx).update time to 00:00 or 00:30 .
            mHandler.removeMessages(UPDATE_RECORD_TIME);
        }
        mVideoContext.keepScreenOn();
    }

    private void startNormalRecording() {
        if (LOG) {
            Log.v(TAG, "startNormalRecording()");
        }
        try {
            mMediaRecorder.start(); // Recording is now started
        } catch (RuntimeException e) { // need Runtime
            Log.e(TAG, "Could not start media recorder. ", e);
            mStartRecordingFailed = true;
            releaseMediaRecorder();
            // If start fails, frameworks will not lock the camera for us.
            mVideoContext.getCameraDevice().lock();
        }
    }

    private void onStopVideoRecordingAsync() {
        if (LOG) {
            Log.v(TAG, "onStopVideoRecordingAsync");
        }
        mEffectsDisplayResult = true;
        // for snapshot
        mStopVideoRecording = true;
        stopVideoRecordingAsync();
    }

    private void pauseAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which should be public, but isn't.
        if (LOG) {
            Log.v(TAG, "pauseAudioPlayback()");
        }
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mVideoContext.sendBroadcast(i);
    }

    // from MediaRecorder.OnErrorListener
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
            stopVideoRecordingAsync();
        } else if (extra == MediaRecorder.MEDIA_RECORDER_ENCODER_ERROR) {
            onStopVideoRecordingAsync();
            mVideoContext.showAlertDialog(mVideoContext.getString(R.string.camera_error_title),
                    mVideoContext.getString(R.string.video_encoder_error), mVideoContext.getString(R.string.dialog_ok),
                    null, null, null);
        }
    }

    // from MediaRecorder.OnInfoListener
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            if (mMediaRecorderRecording) {
                onStopVideoRecordingAsync();
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            if (mMediaRecorderRecording) {
                onStopVideoRecordingAsync();
                mVideoContext.showToast(R.string.video_reach_size_limit);
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_CAMERA_RELEASE) {
            if (mVideoSavingTask != null) {
                synchronized (mVideoSavingTask) {
                    if (LOG) {
                        Log.v(TAG, "MediaRecorder camera released, notify job wait for camera release");
                    }
                    mRecorderCameraReleased = true;
                    mVideoSavingTask.notifyAll();
                }
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_START_TIMER) {
            if (!mCaptureTimeLapse) {
                mRecordingStartTime = SystemClock.uptimeMillis();
                // mVideoContext.getShutterManager().setVideoShutterEnabled(true);
                // used in effect record , it is always bigger than 1500, then times=2 is OK
                updateRecordingTime();
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_FPS_ADJUSTED
                || what == MediaRecorder.MEDIA_RECORDER_INFO_BITRATE_ADJUSTED) {
            mVideoContext.showToast(R.string.video_bad_performance_drop_quality);
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_WRITE_SLOW) {
            mVideoContext.showToast(R.string.video_bad_performance_auto_stop);
            stopVideoRecordingAsync();
        } else if (what == MEDIA_RECORDER_INFO_RECORDING_SIZE) {
            if (mTotalSize > 0 && (!effectActive())) {
                int progress = (int) (extra * 100 / mTotalSize);
                if (progress <= 100) {
                    if (LOG) {
                        Log.v(TAG, "MEDIA_RECORDER_INFO_RECORDING_SIZE,extra= " + extra + " progress= " + progress);
                    }
                    mRecordingView.setCurrentSize(extra);
                    mRecordingView.setSizeProgress(progress);
                }
            }
        }
    }

    private void hideOtherSettings(boolean hide) {
        if (hide) {
            mVideoContext.setViewState(Camera.VIEW_STATE_RECORDING);
        } else {
            mVideoContext.restoreViewState();
        }
    }

    private void updateRecordingTime() {
        if (!mMediaRecorderRecording) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;
        if (mMediaRecoderRecordingPaused) {
            delta = mVideoRecordedDuration;
        }
        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        long deltaAdjusted = delta;
        long targetNextUpdateDelay;
        if (!mCaptureTimeLapse) {
            mRecordingView.showTime(deltaAdjusted, false);
            targetNextUpdateDelay = 1000;
        } else {
            // The length of time lapse video is different from the length
            // of the actual wall clock time elapsed. Display the video length
            // only in format hh:mm:ss.dd, where dd are the centi seconds.
            mRecordingView.showTime(getTimeLapseVideoLength(delta), true);
            targetNextUpdateDelay = mTimeBetweenTimeLapseFrameCaptureMs;
        }
        mCurrentShowIndicator = 1 - mCurrentShowIndicator;
        if (mMediaRecoderRecordingPaused && 1 == mCurrentShowIndicator) {
            mRecordingView.setTimeVisible(false);
        } else {
            mRecordingView.setTimeVisible(true);
        }
        long actualNextUpdateDelay = 500;
        if (!mMediaRecoderRecordingPaused) {
            actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        }
        if (LOG) {
            Log.v(TAG, "updateRecordingTime(),actualNextUpdateDelay==" + actualNextUpdateDelay);
        }
        mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, actualNextUpdateDelay);
    }

    private void doReturnToCaller(boolean valid) {
        if (LOG) {
            Log.v(TAG, "doReturnToCaller(" + valid + ")");
        }
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = Activity.RESULT_OK;
            resultIntent.setData(mCurrentVideoUri);
            if (mVideoContext.isVideoWallPaperIntent()) {
                Util.setLastUri(mCurrentVideoUri);
            }
        } else {
            resultCode = Activity.RESULT_CANCELED;
        }
        mVideoContext.setResultExAndFinish(resultCode, resultIntent);
    }

    private long getTimeLapseVideoLength(long deltaMs) {
        // For better approximation calculate fractional number of frames captured.
        // This will update the video time at a higher resolution.
        double numberOfFrames = (double) deltaMs / mTimeBetweenTimeLapseFrameCaptureMs;
        return (long) (numberOfFrames / mProfile.videoFrameRate * 1000);
    }

    private void showAlert() {
        if (LOG) {
            Log.v(TAG, "showAlert()");
        }
        if (Storage.isStorageReady()) {
            FileDescriptor mFileDescriptor = null;
            if (mVideoFileDescriptor != null) {
                mFileDescriptor = mVideoFileDescriptor.getFileDescriptor();
                mVideoContext.showReview(mVideoFileDescriptor.getFileDescriptor());
            } else if (mCurrentVideoFilename != null) {
                mVideoContext.showReview(mCurrentVideoFilename);
            }
            mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_OK_CANCEL);
        }
    }

    // This Handler is used to post message back onto the main thread of the application
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "MainHandler.handleMessage(" + msg + ")");
            }
            switch (msg.what) {
            case UPDATE_RECORD_TIME:
                updateRecordingTime();
                break;

            case UPDATE_SNAP_UI:
                Util.broadcastNewPicture(mVideoContext, mSnapUri);
                showVideoSnapshotUI(false);
                break;

            default:
                break;
            }
        }
    }

    @Override
    public void onCameraClose() { // before camera close and mCameraDevice = null
        if (LOG) {
            Log.v(TAG, "onCameraClose()");
        }
        mHandler.removeMessages(UPDATE_RECORD_TIME);
        mHandler.removeMessages(UPDATE_SNAP_UI);
        // avoid performance is bad,the UPDATE_SNAP_UI msg had not been handleMessage,
        // it was removed,the red frame will show always.
        showVideoSnapshotUI(false); 
        mVideoCameraClosed = true;
        mSingleStartRecording = false;
        mIsAutoFocusCallback = false;
        mNeedReLearningEffect = false;
        mEffectsError = false;
        if (mVideoContext.getCameraDevice() != null) {
            mVideoContext.getCameraDevice().cancelAutoFocus();
        }
        if (mVideoContext.getFocusManager() != null) {
            mVideoContext.getFocusManager().onPreviewStopped();
        }
        stopVideoOnPause();
        if (mVideoContext.getCameraDevice() == null) {
            return;

        }
        releaseEffects();
        mVideoContext.resetScreenOn();
    }

    @Override
    public boolean onUserInteraction() {
        if (!mMediaRecorderRecording) {
            mVideoContext.keepScreenOnAwhile();
            return true;
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        if (LOG) {
            Log.v(TAG, "onBackPressed() isFinishing()=" + mVideoContext.isFinishing() + ", mVideoCameraClosed="
                    + mVideoCameraClosed + ", isVideoProcessing()=" + isVideoProcessing()
                    + ",mVideoContext.isShowingProgress() = " + mVideoContext.isShowingProgress());
        }
        if (mVideoCameraClosed || (mVideoContext.isShowingProgress()) || isVideoProcessing()) {
            return true;
        }
        if (mMediaRecorderRecording) {
            onStopVideoRecordingAsync();
            return true;
        }
        return super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do not handle any key if the activity is paused.
        if (mVideoCameraClosed) {
            return true;
        }
        switch (keyCode) {
        case KeyEvent.KEYCODE_CAMERA:
            if (event.getRepeatCount() == 0) {
                if (!(mVideoContext.getReviewManager().isShowing())) {
                    // || (mLearningView != null && mLearningView.isShowing()))) {
                    mVideoShutterListener.onShutterButtonClick(null);
                }
                return true;
            }
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            if (event.getRepeatCount() == 0) {
                if (!(mVideoContext.getReviewManager().isShowing())) {
                    // || (mLearningView != null && mLearningView.isShowing()))) {
                    mVideoShutterListener.onShutterButtonClick(null);
                }
                return true;
            }
            break;
        case KeyEvent.KEYCODE_MENU:
            if (mMediaRecorderRecording) {
                return true;
            }
            break;

        default:
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

/*    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_CAMERA:
            mVideoContext.getShutterManager().setVideoShutterEnabled(false);
            return true;
        default:
            break;
        }
        return super.onKeyUp(keyCode, event);
    }  */

    public void stopVideoOnPause() {
        if (LOG) {
            Log.v(TAG, "stopVideoOnPause()");
        }
        boolean effectsActive = effectActive();
        boolean videoSaving = false;
        if (mMediaRecorderRecording) {
            mEffectsDisplayResult = true;
            if (!mVideoContext.isNonePickIntent()) {
                if (!effectsActive) {
                    mStoppingAction = STOP_SHOW_ALERT;
                }
            }
            stopVideoRecordingAsync();
            videoSaving = isVideoProcessing();
        } else if (!effectsActive) {
            // always release media recorder. if video saving task is ongoing, let SavingTask do this job.
            releaseMediaRecorder();
        }
        if (effectsActive) {
            if (mVideoSavingTask != null) {
                waitForRecorder();
            }
        } else {
            if (videoSaving) {
                waitForRecorder();
            } else {
                // here if media recorder is stopping in videoSavingTask, do the job later.
                closeVideoFileDescriptor();
            }
        }
        if (LOG) {
            Log.v(TAG, "stopVideoOnPause() effectsActive=" + effectsActive + ", videoSaving=" + videoSaving
                    + ", mVideoSavingTask=" + mVideoSavingTask + ", mMediaRecorderRecording=" + mMediaRecorderRecording);
        }
    }

    private void pauseVideoRecording() {
        if (LOG) {
            Log.v(TAG, "pauseVideoRecording() mRecorderBusy=" + mRecorderBusy);
        }

        mRecordingView.setRecordingIndicator(false);
        if (mMediaRecorderRecording && !mMediaRecoderRecordingPaused) {
            try {
                MediaRecorderEx.pause(mMediaRecorder);
            } catch (IllegalStateException e) {
                Log.e("Camera", "Could not pause media recorder. ");
            }
            mVideoRecordedDuration = SystemClock.uptimeMillis() - mRecordingStartTime;
            mMediaRecoderRecordingPaused = true;
        }
    }

    private volatile boolean mRecorderBusy = false;

    private void stopVideoRecordingAsync() {
        if (LOG) {
            Log.v(TAG, "stopVideoRecordingAsync() mMediaRecorderRecording=" + mMediaRecorderRecording + ", mRecorderBusy="
                    + mRecorderBusy);
        }
        mVideoContext.setSwipingEnabled(true);
        mHandler.removeMessages(UPDATE_RECORD_TIME);
        mVideoContext.getShutterManager().setVideoShutterMask(false);
        if (isVideoProcessing()) {
            return;
        }
        if (mRecorderBusy) { // return for recorder is busy.
            return;
        }
        mRecorderBusy = true;
        mRecordingView.hide();
        if (mMediaRecorderRecording) {
            mVideoContext.getShutterManager().setVideoShutterEnabled(false);
            if (mStoppingAction != STOP_RETURN_UNVALID) {
                mVideoContext.showProgress(mVideoContext.getResources().getString(R.string.saving));
            }
            mVideoSavingTask = new SavingTask();
            mVideoSavingTask.start();
        } else {
            mRecorderBusy = false;
            if (!effectActive()) {
                releaseMediaRecorder();
            }
            if (mStoppingAction == STOP_RETURN_UNVALID) {
                doReturnToCaller(false);
            }
        }
    }

    private class SavingTask extends Thread {
        public void run() {
            if (LOG) {
                Log.v(TAG, "SavingTask.run() begin " + this + ", mEffectsActive=" + effectActive()
                        + ", mMediaRecorderRecording=" + mMediaRecorderRecording + ", mRecorderBusy=" + mRecorderBusy);
            }
            MMProfileManager.startProfileStoreVideo();
            boolean fail = false;
            // add saving thread to avoid blocking main thread
            boolean shouldAddToMediaStoreNow = false;
            if (mMediaRecorderRecording) {
                try {
                    if (effectActive()) {
                        // This is asynchronous, so we can't add to media store now because thumbnail
                        // may not be ready. In such case addVideoToMediaStore is called later
                        // through a callback from the MediaEncoderFilter to EffectsRecorder,
                        // and then to the VideoCamera.
                        mEffectsRecorder.stopRecording();
                    } else {
                        mMediaRecorder.setOnErrorListener(null);
                        mMediaRecorder.setOnInfoListener(null);
                        mMediaRecorder.stop();
                        mMediaRecorder.setOnCameraReleasedListener(null);
                        shouldAddToMediaStoreNow = true;
                    }
                    mCurrentVideoFilename = mVideoFilename;
                    if (LOG) {
                        Log.v(TAG, "Setting current video filename: " + mCurrentVideoFilename);
                    }
                } catch (RuntimeException e) { // need Runtime
                    Log.e(TAG, "stop fail", e);
                    fail = true;
                    if (mVideoFilename != null) {
                        deleteVideoFile(mVideoFilename);
                    }
                }
            }
            mMediaRecorderRecording = false;
            if (!mVideoCameraClosed) {
                mVideoContext.setCameraState(Camera.STATE_IDLE);
            }
            if (!mVideoContext.isNonePickIntent()) {
                if (!effectActive() && !fail && mStoppingAction != STOP_RETURN_UNVALID) {
                    if (mVideoContext.isQuickCapture()) {
                        mStoppingAction = STOP_RETURN;
                    } else {
                        mStoppingAction = STOP_SHOW_ALERT;
                    }
                }
            } else if (fail) {
                mStoppingAction = STOP_FAIL;
            }
            // always release media recorder
            if (!effectActive()) {
                releaseMediaRecorder();
            }
            if (shouldAddToMediaStoreNow) {
                addVideoToMediaStore();
            }
            synchronized (mVideoSavingTask) {
                if (!effectActive()) {
                    mVideoSavingTask.notifyAll();
                    mHandler.removeCallbacks(mVideoSavedRunnable);
                    mHandler.post(mVideoSavedRunnable);
                } else {
                    mRecorderBusy = false;
                }
            }
            if (LOG) {
                Log.v(TAG, "SavingTask.run() end " + this + ", mCurrentVideoUri=" + mCurrentVideoUri + ", mRecorderBusy="
                        + mRecorderBusy);
            }
            MMProfileManager.stopProfileStoreVideo();
        }
    }

    private Runnable mVideoSavedRunnable = new Runnable() {
        public void run() {
            if (LOG) {
                Log.v(TAG, "mVideoSavedRunnable.run() begin mVideoCameraClosed=" + mVideoCameraClosed + ", mStoppingAction="
                        + mStoppingAction + ", mEffectsDisplayResult=" + mEffectsDisplayResult + ", mFocusState="
                        + mFocusState + ", mSingleAutoModeSupported=" + mSingleAutoModeSupported + ", mRecorderBusy="
                        + mRecorderBusy);
            }
            hideOtherSettings(false);
            mVideoContext.dismissProgress();
            if (effectActive()) { // live effect will not call this flow
                mVideoContext.getShutterManager().setCancelButtonEnabled(true);
            }
            if (!mVideoCameraClosed) {
                // The orientation was fixed during video recording. Now make it
                // reflect the device orientation as video recording is stopped.
                mVideoContext.keepScreenOnAwhile();
            }
            mVideoContext.getShutterManager().setVideoShutterEnabled(true);
            int action = (mVideoCameraClosed && mStoppingAction != STOP_NORMAL && mStoppingAction != STOP_FAIL)
                    ? STOP_SHOW_ALERT
                    : mStoppingAction;
            switch (action) {
            case STOP_NORMAL:
                if (!mVideoCameraClosed && !effectActive()) {
                    mVideoContext.animateCapture();
                }
                break;
            case STOP_SHOW_ALERT:
                showAlert();
                break;
            case STOP_RETURN_UNVALID:
                doReturnToCaller(false);
                break;
            case STOP_RETURN:
                doReturnToCaller(true);
                break;
            default:
                break;
            }
            if (mVideoCameraClosed && !mEffectsDisplayResult) {
                closeVideoFileDescriptor();
            }
            // if the onAutoFocus had not back yet,changeFocusState()
            if (!mVideoCameraClosed
                    && ((mFocusState == START_FOCUSING) || (mFocusState == FOCUSING) || mSingleAutoModeSupported)) {
                changeFocusState();
            }
            backToLastModeIfNeed();
            mRecorderBusy = false;
            if (LOG) {
                Log.v(TAG, "mVideoSavedRunnable.run() end mRecorderBusy=" + mRecorderBusy);
            }
        };
    };

    private boolean isVideoProcessing() {
        return mVideoSavingTask != null && mVideoSavingTask.isAlive();
    }

    private void waitForRecorder() {
        synchronized (mVideoSavingTask) {
            if (!mRecorderCameraReleased) {
                try {
                    if (LOG) {
                        Log.v(TAG, "Wait for releasing camera done in MediaRecorder");
                    }
                    mVideoSavingTask.wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "Got notify from Media recorder()", e);
                }
            }
        }
    }

    // for pause/resume
    public OnClickListener mVideoPauseResumeListner = new OnClickListener() {
        public void onClick(View v) {
            if (LOG) {
                Log.v(TAG, "mVideoPauseResumeListner.onClick() mMediaRecoderRecordingPaused=" + mMediaRecoderRecordingPaused
                        + ",mRecorderBusy = " + mRecorderBusy + ",mMediaRecorderRecording = " + mMediaRecorderRecording);
            }
            // return for recorder is busy.or if the recording has already stopped because of some info,it will not response
            // the restart.
            if (mRecorderBusy || (!mMediaRecorderRecording)) {
                return;
            }
            mRecorderBusy = true;
            if (mMediaRecoderRecordingPaused) {
                mRecordingView.setRecordingIndicator(true);
                try {
                    mMediaRecorder.start();
                    mRecordingStartTime = SystemClock.uptimeMillis() - mVideoRecordedDuration;
                    mVideoRecordedDuration = 0;
                    mMediaRecoderRecordingPaused = false;
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Could not start media recorder. ", e);
                    mVideoContext.showToast(R.string.toast_video_recording_not_available);
                    releaseMediaRecorder();
                }
            } else {
                pauseVideoRecording();
            }
            mRecorderBusy = false;
            if (LOG) {
                Log.v(TAG, "mVideoPauseResumeListner.onClick() end. mRecorderBusy=" + mRecorderBusy);
            }
        }
    };

    private boolean backToLastModeIfNeed() {
        if (LOG) {
            Log.v(TAG, "backToLastModeIfNeed()");
        }
        boolean back = false;
        if (mVideoContext.isNonePickIntent() && !effectActive()) {
            releaseVideoActor();
            if (mWfdManager != null) {
                mWfdManager.removeListener(mWfdListener);
            }
            mVideoContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mVideoContext.getShutterManager().setPhotoShutterEnabled(true);
                    mVideoContext.backToLastMode();
                }
            });
            back = true;
        } else if (mVideoContext.isVideoCaptureIntent() || mVideoContext.isVideoWallPaperIntent()) {
            mVideoContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!effectActive() && !mVideoContext.getReviewManager().isShowing()) {
                        mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_VIDEO);
                    }
                }
            });
        }
        if (LOG) {
            Log.v(TAG, "backToLastModeIfNeed() return " + back);
        }
        return back;
    }

    private void releaseVideoActor() {
        if (LOG) {
            Log.v(TAG, "releaseVideoActor");
        }
        mVideoShutterListener = null;
        if (mVideoContext.getFocusManager() != null) {
            mVideoContext.getFocusManager().removeMessages();
        }
        mSingleStartRecording = false;
        mIsAutoFocusCallback = false;
        mFocusManager = null;
        mMediaRecoderRecordingPaused = false;
    }

    private long computeDuration() {
        long duration = getDuration();
     /*   if (duration > 0) {
            if (mCaptureTimeLapse) {
                duration = getTimeLapseVideoLength(duration);
            }
        }  */
        if (LOG) {
            Log.v(TAG, "computeDuration() return " + duration);
        }
        return duration;
    }

    private long getDuration() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mCurrentVideoFilename);
            return Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (IllegalArgumentException e) {
            return INVALID_DURATION;
        } catch (RuntimeException e) {
            return FILE_ERROR;
        } finally {
            retriever.release();
        }
    }

    private void releaseMediaRecorder() {
        if (LOG) {
            Log.v(TAG, "releaseMediaRecorder() mMediaRecorder=" + mMediaRecorder);
        }
        if (mMediaRecorder != null) {
            cleanupEmptyFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mRecorderCameraReleased = true;
        }
        mVideoFilename = null;
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    private void closeVideoFileDescriptor() {
        if (mVideoFileDescriptor != null) {
            try {
                mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd", e);
            }
            mVideoFileDescriptor = null;
        }
    }

    private void cleanupEmptyFile() {
        if (mVideoFilename != null) {
            File f = new File(mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                if (LOG) {
                    Log.v(TAG, "Empty video file deleted: " + mVideoFilename);
                }
                mVideoFilename = null;
            }
        }
    }

    private void deleteCurrentVideo() {
        // Remove the video and the uri if the uri is not passed in by intent.
        if (LOG) {
            Log.v(TAG, "deleteCurrentVideo() mCurrentVideoFilename=" + mCurrentVideoFilename);
        }
        if (mCurrentVideoFilename != null) {
            deleteVideoFile(mCurrentVideoFilename);
            mCurrentVideoFilename = null;
            if (mCurrentVideoUri != null) {
                mContentResolver.delete(mCurrentVideoUri, null, null);
                mCurrentVideoUri = null;
            }
        }
    }

    private void deleteVideoFile(String fileName) {
        File f = new File(fileName);
        if (!f.delete()) {
            if (LOG) {
                Log.v(TAG, "Could not delete " + fileName);
            }
        }
    }

    private final class AutoFocusCallback implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(boolean focused, android.hardware.Camera camera) {
            if (mVideoCameraClosed) {
                return;
            }
            if (LOG) {
                Log.v(TAG, "mAutoFocusTime = " + (System.currentTimeMillis() - mFocusStartTime) + "ms"
                        + ",mFocusManager.onAutoFocus(focused)");
            }
            setFocusState(FOCUSED);
            mVideoContext.getFocusManager().onAutoFocus(focused);
            mIsAutoFocusCallback = true;
        }
    }

    public void autoFocus() {
        mFocusStartTime = System.currentTimeMillis();
        if (LOG) {
            Log.v(TAG, "autoFocus");
        }
        mVideoContext.getCameraDevice().autoFocus(mAutoFocusCallback);
        setFocusState(FOCUSING);
    }

    public void cancelAutoFocus() {
        if (LOG) {
            Log.v(TAG, "cancelAutoFocus");
        }
        if (mVideoContext.getCameraDevice() != null) {
            mVideoContext.getCameraDevice().cancelAutoFocus();
        }
        setFocusState(FOCUS_IDLE);
        if (!(mSingleStartRecording && mSingleAutoModeSupported && mIsAutoFocusCallback)) {
            setFocusParameters();
        }
        mIsAutoFocusCallback = false;
    }

    public Listener getFocusManagerListener() {
        return this;
    }

    public boolean capture() {
        return false;
    }

    public void setFocusParameters() {
        mVideoContext.applyParameterForFocus(!mIsAutoFocusCallback);
    }

    public void startFaceDetection() {
    }

    public void stopFaceDetection() {
    }

    public void playSound(int soundId) {
    }

    public boolean readyToCapture() {
        return false;
    }

    public boolean doSmileShutter() {
        return false;
    }

    private void setAutoFocusMode() { // should be checked
        if (isSupported(Parameters.FOCUS_MODE_AUTO, mVideoContext.getParameters().getSupportedFocusModes())) {
            final String focusMode = Parameters.FOCUS_MODE_AUTO;
            mVideoContext.lockRun(new Runnable() {
                @Override
                public void run() {
                    mVideoContext.getParameters().setFocusMode(focusMode);
                    mVideoContext.applyParametersToServer();
                }
            });
        }
        if (LOG) {
            Log.v(TAG, "set focus mode is auto");
        }
    }

    private void changeFocusState() {
        if (LOG) {
            Log.v(TAG, "changeFocusState()");
        }
        if (mVideoContext.getCameraDevice() != null) {
            mVideoContext.getCameraDevice().cancelAutoFocus();
        }
        mSingleStartRecording = false;
        mIsAutoFocusCallback = false;
        mVideoContext.getFocusManager().resetTouchFocus();
        setFocusParameters();
        mVideoContext.getFocusManager().updateFocusUI();
    }

    private void setFocusState(int state) {
        if (LOG) {
            Log.v(TAG, "setFocusState(" + state + ") mMediaRecorderRecording=" + mMediaRecorderRecording
                    + ", mVideoCameraClosed=" + mVideoCameraClosed
                    + ",mVideoContext.getViewState() = " + mVideoContext.getViewState());
        }
        mFocusState = state;
        if (mMediaRecorderRecording || mVideoCameraClosed || (mVideoContext.getViewState() == Camera.VIEW_STATE_REVIEW)) {
            return;
        }
        switch (state) {
        case FOCUSING:
            mVideoContext.setViewState(Camera.VIEW_STATE_FOCUSING);
            break;
        case FOCUS_IDLE:
        case FOCUSED:
            hideOtherSettings(false);
            break;
        default:
            break;
        }
    }

    private final class AutoFocusMoveCallback implements android.hardware.Camera.AutoFocusMoveCallback {
        @Override
        public void onAutoFocusMoving(boolean moving, android.hardware.Camera camera) {
            mVideoContext.getFocusManager().onAutoFocusMoving(moving);
        }
    }

    public AutoFocusMoveCallback getAutoFocusMoveCallback() {
        return mAutoFocusMoveCallback;
    }

    public OnClickListener mReviewPlay = new OnClickListener() {
        public void onClick(View v) {
            startPlayVideoActivity();
        }
    };
    public OnClickListener mRetakeListener = new OnClickListener() {
        public void onClick(View v) {
            deleteCurrentVideo();
            mVideoContext.hideReview();
            if (effectActive()) {
                mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_CANCEL_VIDEO);
                mVideoContext.getShutterManager().setCancelButtonEnabled(true);
                mVideoContext.getShutterManager().setVideoShutterEnabled(true);
            if (mNeedReLearningEffect) {
                mNeedReLearningEffect = false;
                startPreview();
            }
            } else {
                mVideoContext.getShutterManager().setVideoShutterEnabled(true);
                mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_VIDEO);
            }
        }
    };
    public OnClickListener mOkListener = new OnClickListener() {
        public void onClick(View v) {
            doReturnToCaller(true);
        }
    };
    public OnClickListener mCancelListener = new OnClickListener() {
        public void onClick(View v) {
            if (LOG) {
                Log.v(TAG, "mCancelListener");
            }
            if (mVideoContext.getReviewManager().isShowing()) {
                mStoppingAction = STOP_RETURN_UNVALID;
                stopVideoRecordingAsync();
            } else {
                releaseEffects();
                // Write default effect out to shared prefs
                mVideoContext.resetLiveEffect(true);
                hideOtherSettings(false);
                if (!mVideoContext.isNonePickIntent()) {
                    // if click cancel when not none to none in MMS(pick),update the preference of videoactor.
                    mVideoContext.notifyPreferenceChanged();
                }
                backToLastModeIfNeed();
            }
        }
    };

    public OnClickListener getPlayListener() {
        return mReviewPlay;
    }

    public OnClickListener getRetakeListener() {
        return mRetakeListener;
    }

    public OnClickListener getOkListener() {
        return mOkListener;
    }

    public OnClickListener getCancelListener() {
        return mCancelListener;
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(mCurrentVideoUri, convertOutputFormatToMimeType(mProfile.fileFormat));
        try {
            mVideoContext.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't view video " + mCurrentVideoUri, ex);
        }
    }

    private int getRecordMode(String mode) {
        int audioMode = 0;
        if (mRecordAudio) {
            if (mode.equals(PREF_CAMERA_VIDEO_HD_RECORDING_ENTRYVALUES[0])) {
                audioMode = HDRecordMode.NORMAL;
            } else if (mode.equals(PREF_CAMERA_VIDEO_HD_RECORDING_ENTRYVALUES[1])) {
                audioMode = HDRecordMode.INDOOR;
            } else {
                audioMode = HDRecordMode.OUTDOOR;
            }
        } else {
            audioMode = HDRecordMode.NORMAL;
        }
        if (LOG) {
            Log.v(TAG, "getRecordMode(" + mode + ") return " + audioMode);
        }
        return audioMode;
    }

    public ErrorCallback getErrorCallback() {
        return mErrorCallback;
    }

    private static boolean isSupported(Object value, List<?> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private void getRequestedSizeLimit() {
        closeVideoFileDescriptor();
        if (!mVideoContext.isNonePickIntent()) {
            Uri saveUri = mVideoContext.getSaveUri();
            if (saveUri != null) {
                try {
                    mVideoFileDescriptor = mContentResolver.openFileDescriptor(saveUri, "rw");
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    Log.e(TAG, "initializeNormalRecorder()", ex);
                }
            }
            mRequestedSizeLimit = mVideoContext.getLimitedSize();
        }
    }

    // background Effects related
    private void startEffectRecording() {
        if (LOG) {
            Log.v(TAG, "startEffectRecording()");
        }
        try {
            mEffectsRecorder.startRecording();
        } catch (RuntimeException e) { // need Runtime
            Log.e(TAG, "Could not start effects recorder. ", e);
            mStartRecordingFailed = true;
            releaseEffectsRecorder();
        }
    }

    private void initializeEffectsRecording() {
        if (LOG) {
            Log.v(TAG, "initializeEffectsRecording");
        }
        getRequestedSizeLimit();
        mEffectsRecorder.setProfile(mProfile);
        mEffectsRecorder.setMuteAudio(!mRecordAudio);
        // important to set the capture rate to zero if not timelapsed, since the
        // effectsrecorder object does not get created again for each recording session
        if (mCaptureTimeLapse) {
            mEffectsRecorder.setCaptureRate((1000 / (double) mTimeBetweenTimeLapseFrameCaptureMs));
        } else {
            mEffectsRecorder.setCaptureRate(0);
        }

        // Set output file.Try Uri in the intent first. If it doesn't exist, use our own instead.
        if (mVideoFileDescriptor != null) {
            mEffectsRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
        } else {
            generateVideoFilename(mProfile.fileFormat);
            mEffectsRecorder.setOutputFile(mVideoFilename);
        }
        // Set maximum file size.
        long maxFileSize = Storage.getAvailableSpace() - Storage.LOW_STORAGE_THRESHOLD;
        if (mRequestedSizeLimit > 0 && mRequestedSizeLimit < maxFileSize) {
            maxFileSize = mRequestedSizeLimit;
        }
        mEffectsRecorder.setMaxFileSize(maxFileSize);
        mEffectsRecorder.setMaxDuration(mMaxVideoDurationInMs);
    }

    private boolean updateEffectSelection() {
        int previousEffectType = mEffectType;
        Object previousEffectParameter = mEffectParameter;
        mEffectType = mVideoContext.getEffectType();
        mEffectParameter = mVideoContext.getEffectParameter();
        if (LOG) {
            Log.v(TAG, "updateEffectSelection,mEffectType = " + mEffectType + ",mEffectParameter = " + mEffectParameter
                    + ",previousEffectType = " + previousEffectType + ",previousEffectParameter = "
                    + previousEffectParameter);
        }
        if (mEffectType == previousEffectType) {
            if (mEffectType == EffectsRecorder.EFFECT_NONE) {
                if (previousEffectParameter != null && previousEffectParameter.equals(EFFECT_BG_FROM_GALLERY)
                        && mEffectParameter == null) {
                    // when your video,back.
                    if ((mVideoContext.isNonePickIntent())) {
                        releaseEffects();
                        backToLastModeIfNeed();
                        return true;
                    } else {
                        mVideoContext.runOnUiThread(new Runnable() {
                            public void run() {
                                mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_VIDEO);
                            }
                        });
                    }
                }
                return false;
            }
            if ((mEffectParameter != null) && (mEffectParameter.equals(previousEffectParameter))) {
                if (LOG) {
                    Log.v(TAG, "mEffectParameter.equals(previousEffectParameter)");
                }
                return false;
            }
        }
        if (mEffectType == EffectsRecorder.EFFECT_NONE) {
            // Stop effects and return to normal preview
            mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_VIDEO);
            return false;
        }
        if (mEffectType == EffectsRecorder.EFFECT_BACKDROPPER && ((String) mEffectParameter).equals(EFFECT_BG_FROM_GALLERY)
                && ((previousEffectParameter == null) || (!previousEffectParameter.equals(EFFECT_BG_FROM_GALLERY)))) {
            // Request video from gallery to use for background
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setDataAndType(Video.Media.EXTERNAL_CONTENT_URI, "video/*");
            i.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            mVideoContext.startActivityForResult(i, REQUEST_EFFECT_BACKDROPPER);
            return true;
        }
        if (previousEffectType == EffectsRecorder.EFFECT_NONE) {
            // Stop regular preview and start effects.
            if (LOG) {
                Log.v(TAG, "previousEffectType == EffectsRecorder.EFFECT_NONE");
            }
            // when in MMS,liveeffect A,start recording,lock and then unlock the screen,not restudy.
            if (mVideoContext.getReviewManager().isShowing() && effectActive()) {
                mNeedReLearningEffect = true;
                return true;
            }
            startPreview();
        } else {
            // Switch currently running effect
            if (LOG) {
                Log.v(TAG, "mEffectsRecorder.setEffect(mEffectType, mEffectParameter");
            }
            mEffectsRecorder.setEffect(mEffectType, mEffectParameter);
            if (mEffectType == EffectsRecorder.EFFECT_GOOFY_FACE) {
                mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_CANCEL_VIDEO);
                mVideoContext.getShutterManager().setCancelButtonEnabled(true);
                mVideoContext.getShutterManager().setVideoShutterEnabled(true);
            }
        }
        return true;
    }

    private void initializeEffectsPreview() {
        if (LOG) {
            Log.v(TAG, "initializeEffectsPreview");
        }
        // If the mCameraDevice is null, then this activity is going to finish
        if (mVideoContext.getCameraDevice() == null) {
            return;
        }
        CameraInfo info = CameraHolder.instance().getCameraInfo()[(mVideoContext.getCameraId())];

        mEffectsDisplayResult = false;
        mEffectsRecorder = new EffectsRecorder(mVideoContext);

        // TODO: Confirm none of the following need to go to initializeEffectsRecording()
        // and none of these change even when the preview is not refreshed.
        mEffectsRecorder.setCameraDisplayOrientation(mVideoContext.getCameraDisplayOrientation());
        if (!FrameworksClassFactory.isMockCamera()) {
            mEffectsRecorder.setCamera(mVideoContext.getCameraDevice().getCamera().getInstance());
        }
        mEffectsRecorder.setCameraFacing(info.facing);
        mEffectsRecorder.setProfile(mProfile);
        mEffectsRecorder.setEffectsListener(this);
        mEffectsRecorder.setOnInfoListener(this);
        mEffectsRecorder.setOnCameraReleasedListener(this);
        mEffectsRecorder.setOnErrorListener(this);
        mEffectsRecorder.setSurfaceStateListener(this);

        // The input of effects recorder is affected by android.hardware.Camera.setDisplayOrientation.
        // Its value only compensates the camera orientation (no Display.getRotation). So the
        // orientation hint here should only consider sensor orientation.
        int orientation = 0;
        mOrientation = mVideoContext.getOrietation();
        if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = mOrientation;
        }
        mEffectsRecorder.setOrientationHint(orientation);
        mVideoContext.setReviewOrientationCompensation(mVideoContext.getOrientationCompensation());
        mEffectsRecorder.setPreviewSurfaceTexture(mVideoContext.getSurfaceTexture(),
                mVideoContext.getCameraScreenNailWidth(), mVideoContext.getCameraScreenNailHeight());

        if (mEffectType == EffectsRecorder.EFFECT_BACKDROPPER && 
                ((String) mEffectParameter).equals(EFFECT_BG_FROM_GALLERY)) {
            mEffectsRecorder.setEffect(mEffectType, mEffectUriFromGallery);
        } else {
            mEffectsRecorder.setEffect(mEffectType, mEffectParameter);
        }
    }

    @Override
    public synchronized void onEffectsError(Exception exception, String fileName) {
        // TODO: Eventually we may want to show the user an error dialog, and then restart
        // the camera and encoder gracefully. For now, we just delete the file and bail out.
        if (LOG) {
            Log.v(TAG, "onEffectsError", exception);
        }
        if (fileName != null && new File(fileName).exists()) {
            deleteVideoFile(fileName);
        }
        if (exception instanceof MediaRecorderStopException) {
            Log.w(TAG, "Problem recoding video file. Removing incomplete file.");
            // here we reset UI to start a new preview.
            updateEffectRecordingUI();
            return;
        }
        if (mEffectsError) {
            return;
        }
        if (!mVideoCameraClosed) {
            mEffectsError = true;
            Util.showErrorAndFinish(mVideoContext, R.string.video_live_effect_error);
        }
    }

    @Override
    public void onEffectsUpdate(int effectId, int effectMsg) {
        if (LOG) {
            Log.v(TAG, "onEffectsUpdate. Effect Message = " + effectMsg);
        }
        if (effectMsg == EffectsRecorder.EFFECT_MSG_EFFECTS_STOPPED) {
            // set mRecorderCameraReleased=true to avoid nobody notify mVideoSavingTask.
            if (mMediaRecorderRecording) {
                stopVideoRecordingAsync(); // stop recording for error
                updateEffectRecordingUI(); // reset ui for new preview
                if (mVideoSavingTask != null) {
                    synchronized (mVideoSavingTask) {
                        mRecorderCameraReleased = true; // release camera
                        mVideoSavingTask.notifyAll();
                    }
                }
                if (LOG) {
                    Log.v(TAG, "onEffectsUpdate() release mVideoSavingTask=" + mVideoSavingTask);
                }
            }
            mRecorderCameraReleased = true;
            // Effects have shut down. Hide learning message if any, and restart regular preview.
            mVideoContext.dismissInfo();
            hideOtherSettings(false);
            startPreview();
        } else if (effectMsg == EffectsRecorder.EFFECT_MSG_RECORDING_DONE) {
            updateEffectRecordingUI();
            // This follows the codepath from onStopVideoRecording.
            if (mEffectsDisplayResult) {
                addVideoToMediaStore();
                mVideoContext.dismissProgress();
                if (mVideoContext.isVideoCaptureIntent()) {
                    if (mVideoContext.isQuickCapture()) {
                        doReturnToCaller(true);
                    } else {
                        showAlert();
                    }
                }
            }
            mEffectsDisplayResult = false;
            // In onPause, these were not called if the effects were active. We
            // had to wait till the effects recording is complete to do this.
            if (mVideoCameraClosed) {
                closeVideoFileDescriptor();
            }
        } else if (effectMsg == EffectsRecorder.EFFECT_MSG_PREVIEW_RUNNING) {
            if (LOG) {
                Log.v(TAG, "effectMsg == EffectsRecorder.EFFECT_MSG_PREVIEW_RUNNING");
            }
            if ((mVideoContext.getCameraState() == Camera.STATE_PREVIEW_STOPPED) && (!mVideoContext.isCameraClosed())) {
                mVideoContext.setCameraState(Camera.STATE_IDLE);
                mVideoContext.getFocusManager().onPreviewStarted();
            }
            // Enable the shutter button once the preview is complete.
            if (mEffectType == EffectsRecorder.EFFECT_GOOFY_FACE) {
                mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_CANCEL_VIDEO);
                mVideoContext.getShutterManager().setCancelButtonEnabled(true);
                mVideoContext.getShutterManager().setVideoShutterEnabled(true);
            }
        } else if (effectId == EffectsRecorder.EFFECT_BACKDROPPER) {
            switch (effectMsg) {
            case EffectsRecorder.EFFECT_MSG_STARTED_LEARNING:
                if (LOG) {
                    Log.v(TAG, "msg is EffectsRecorder.EFFECT_MSG_STARTED_LEARNING");
                }
                mVideoContext.showInfo(mVideoContext.getString(R.string.bg_replacement_message), mEffectApplyTime);
                mVideoContext.switchShutter(ShutterManager.SHUTTER_TYPE_CANCEL_VIDEO);
                mVideoContext.getShutterManager().setCancelButtonEnabled(true);
                mVideoContext.getShutterManager().setVideoShutterEnabled(false);
                mVideoContext.setViewState(Camera.VIEW_STATE_LEARNING_VIDEO_EFFECTS);
                break;
            case EffectsRecorder.EFFECT_MSG_DONE_LEARNING:
                updateEffectRecordingUI();
                mVideoContext.getShutterManager().setVideoShutterEnabled(true);
                // Fall through to setVisibility(GONE)
            case EffectsRecorder.EFFECT_MSG_SWITCHING_EFFECT:
                mVideoContext.dismissInfo();
                hideOtherSettings(false);
                break;
            default:
                break;
            }
        }
        // In onPause, this was not called if the effects were active. We had to
        // wait till the effects completed to do this.
        if (mVideoCameraClosed) {
            if (LOG) {
                Log.v(TAG, "OnEffectsUpdate: closing effects if activity paused");
            }
            closeEffects(true);
        }
    }

    private void stopEffectVideoRecording() {
        if (LOG) {
            Log.v(TAG, "stopEffectVideoRecording");
        }
        mEffectsRecorder.stopRecording();
    }

    private void releaseEffectsRecorder() {
        if (LOG) {
            Log.v(TAG, "Releasing effects recorder.");
        }
        if (mEffectsRecorder != null) {
            cleanupEmptyFile();
            mEffectsRecorder.release();
            mEffectsRecorder = null;
        }
        mEffectType = EffectsRecorder.EFFECT_NONE;
        mVideoFilename = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_EFFECT_BACKDROPPER:
            if (resultCode == Activity.RESULT_OK) {
                // onActivityResult() runs before onResume(), so this parameter will be
                // seen by startPreview from onResume()
                mEffectUriFromGallery = data.getData().toString();
                if (LOG) {
                    Log.v(TAG, "Received URI from gallery: " + mEffectUriFromGallery);
                }
                mResetEffect = false;
            } else {
                mEffectUriFromGallery = null;
                if (LOG) {
                    Log.v(TAG, "No URI from gallery");
                }
                mResetEffect = true;
                releaseEffects();
                // Write default effect out to shared prefs
                mVideoContext.resetLiveEffect(false);
                mVideoContext.notifyPreferenceChanged();
                hideOtherSettings(false);
            }
            break;
        default:
            Log.e(TAG, "Unknown activity result sent to Camera!");
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showVideoSnapshotUI(boolean enabled) {
        if (!mVideoContext.isVideoCaptureIntent()) {
            mVideoContext.showBorder(enabled);
            mVideoContext.getZoomManager().setEnabled(!enabled);
            mVideoContext.getShutterManager().setPhotoShutterEnabled(!enabled);
            if (LOG) {
                Log.v(TAG, "showVideoSnapshotUI,enable shutter,enabled is " + enabled);
            }
        }
    }

    private final class JpegPictureCallback implements PictureCallback {
        public JpegPictureCallback(Location loc) {
        }

        @Override
        public void onPictureTaken(byte[] jpegData, android.hardware.Camera camera) {
            if (LOG) {
                Log.v(TAG, "onPictureTaken,storeImage");
            }
            mPhotoSaveRequest.setData(jpegData);
            mPhotoSaveRequest.addRequest();
            mHandler.sendEmptyMessage(UPDATE_SNAP_UI);
        }
    }

    // Closing the effects out. Will shut down the effects graph.
    private void closeEffects(boolean reset) {
        if (reset) {
            if (LOG) {
                Log.v(TAG, "Closing effects,mEffectsRecorder = " + mEffectsRecorder);
            }
            mEffectType = EffectsRecorder.EFFECT_NONE;
            if (mEffectsRecorder == null) {
                return;
            }
            // This call can handle the case where the camera is already released
            // after the recording has been stopped.
            mEffectsRecorder.release();
            mEffectsRecorder = null;
        }
    }

    public void updateEffectRecordingUI() {
        if (LOG) {
            Log.v(TAG, "updateEffectRecordingUI()");
        }
        if (!mVideoCameraClosed) {
            mVideoContext.keepScreenOnAwhile();
            mVideoContext.dismissProgress();
        }
        mVideoContext.getShutterManager().setVideoShutterEnabled(true);
        mVideoContext.getShutterManager().setCancelButtonEnabled(true);
    }

    public void onStateChange(boolean surfaceReadyForCamera) {
        if (LOG) {
            Log.v(TAG, "Effects report, surfaceTexture ready for camera = " + surfaceReadyForCamera + "effect is "
                    + effectActive() + ",mVideoCameraClosed = " + mVideoCameraClosed);
        }
        mConnectApiReady = surfaceReadyForCamera;
        mVideoContext.setSurfaceTextureReady(surfaceReadyForCamera);
        if (mConnectApiReady && !effectActive() && !mVideoCameraClosed && mVideoContext.getCameraDevice() != null) {
            mVideoContext.setPreviewTextureAsync();
        }
    }

    private void addVideoToMediaStore() {
        if (mVideoFileDescriptor == null) {
            FileSaver filesaver = mVideoContext.getFileSaver();
            mVideoSaveRequest = filesaver.prepareVideoRequest(mProfile.fileFormat,
                    Integer.toString(mProfile.videoFrameWidth) + "x" + Integer.toString(mProfile.videoFrameHeight));
            mVideoSaveRequest.setLocation(mStartLocation);
            mVideoSaveRequest.setTempPath(mVideoTempPath);
            if (Storage.isStorageReady()) {
                mVideoSaveRequest.setDuration(computeDuration());
            }
            mVideoSaveRequest.setIgnoreThumbnail(!mVideoContext.isNonePickIntent());

            mVideoSaveRequest.setListener(new FileSaver.FileSaverListener() {
                public void onFileSaved(SaveRequest request) {
                    if (LOG) {
                        Log.v(TAG, "onFileSaved,notify");
                    }
                    synchronized (mVideoSaveRequest) {
                        mVideoSaveRequest.notifyAll();
                    }
                }
            });
            mVideoSaveRequest.addRequest();
            try {
                if (LOG) {
                    Log.v(TAG, "Wait for URI when saving video done");
                }
                synchronized (mVideoSaveRequest) {
                    mVideoSaveRequest.wait();
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Got notify from onFileSaved", e);
            }
            mCurrentVideoUri = mVideoSaveRequest.getUri();
            mCurrentVideoFilename = mVideoSaveRequest.getFilePath();
            if (LOG) {
                Log.v(TAG, "Saving video,mCurrentVideoUri==" + mCurrentVideoUri + ",mCurrentVideoFilename="
                        + mCurrentVideoFilename);
            }
        }
    }

    @Override
    public void onMediaEject() {
        stopVideoRecordingAsync();
    }

    private void backToLastTheseCase() {
        mRecordingView.hide();
        mVideoContext.restoreViewState();
        backToLastModeIfNeed();
        return;
    }

    @Override
    public void onRestoreSettings() {
        releaseEffects();
        if (!mVideoContext.isNonePickIntent()) {
            // Write default effect out to shared prefs
            // mVideoContext.resetLiveEffect();
            hideOtherSettings(false);
        } else {
            mVideoContext.getShutterManager().setPhotoShutterEnabled(true);
        }
    }

    private void releaseEffects() {
        if (LOG) {
            Log.v(TAG, "releaseEffects()");
        }
        mVideoContext.dismissInfo();
        if (mVideoCameraClosed || mVideoContext.isNonePickIntent()) {
            if (mEffectsRecorder != null) {
                // Disconnect the camera from effects so that camera is ready to be released to the outside world.
                if (LOG) {
                    Log.v(TAG, "mEffectsRecorder.disconnectCamera()");
                }
                mEffectsRecorder.disconnectCamera();
            }
            if (mMediaRecorderRecording) {
                closeEffects(!effectActive());
            } else {
                closeEffects(true);
            }
            if (effectActive() && (mEffectsRecorder != null)) {
                // If the effects are active, make sure we tell the graph that the
                // surfacetexture is not valid anymore. Disconnect the graph from the display.
                if (LOG) {
                    Log.v(TAG, "mEffectsRecorder.disconnectDisplay()");
                }
                mEffectsRecorder.disconnectDisplay();
            }

        } else {
            if (mEffectsRecorder != null) {
                mEffectsRecorder.stopPreview();
            }
        }
    }

    public void onOrientationChanged(int orientation) {
        // TODO Auto-generated method stub
        // We keep the last known orientation. So if the user first orient
        // the camera then point the camera to floor or sky, we still have
        // the correct orientation.
        // The input of effects recorder is affected by
        // android.hardware.Camera.setDisplayOrientation. Its value only
        // compensates the camera orientation (no Display.getRotation).
        // So the orientation hint here should only consider sensor orientation.
        if (effectActive() && mEffectsRecorder != null) {
            mEffectsRecorder.setOrientationHint(orientation);
        }
    }

    public void onEffectsDeactive() { // from setting,not none to none
        releaseEffects();
        mVideoContext.getShutterManager().setPhotoShutterEnabled(true);
    }
}
