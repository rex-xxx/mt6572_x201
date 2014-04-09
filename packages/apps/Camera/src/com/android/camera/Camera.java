package com.android.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.camera.FeatureSwitcher;
import com.android.camera.ParametersHelper;
import com.android.camera.actor.AsdActor;
import com.android.camera.actor.BestActor;
import com.android.camera.actor.CameraActor;
import com.android.camera.actor.EffectsRecorder;
import com.android.camera.actor.EvActor;
import com.android.camera.actor.FaceBeautyActor;
import com.android.camera.actor.HdrActor;
import com.android.camera.actor.MavActor;
import com.android.camera.actor.PanoramaActor;
import com.android.camera.actor.PhotoActor;
import com.android.camera.actor.SmileActor;
import com.android.camera.actor.VideoActor;
import com.android.camera.manager.IndicatorManager;
import com.android.camera.manager.InfoManager;
import com.android.camera.manager.MMProfileManager;
import com.android.camera.manager.ModePicker;
import com.android.camera.manager.OnScreenHint;
import com.android.camera.manager.PickerManager;
import com.android.camera.manager.RemainingManager;
import com.android.camera.manager.ReviewManager;
import com.android.camera.manager.RotateDialog;
import com.android.camera.manager.RotateProgress;
import com.android.camera.manager.SelfTimerManager;
import com.android.camera.manager.SettingManager;
import com.android.camera.manager.ShutterManager;
import com.android.camera.manager.ThumbnailManager;
import com.android.camera.manager.ViewManager;
import com.android.camera.manager.ZoomManager;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.PreviewFrameLayout;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;
import com.mediatek.camera.FrameworksClassFactory;
import com.mediatek.camera.ext.ExtensionHelper;
import com.mediatek.camcorder.CamcorderProfileEx;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * --ActivityBase
 * ----Camera //will control CameraActor, ModePicker, CameraPicker, SettingChecker, FocusManager, RemainingManager
 * --CameraActor
 * ------VideoActor
 * ------PhotoActor
 * --------NormalActor //contains continuous shot
 * --------HdrActor
 * --------FaceBeautyActor
 * --------AsdActor
 * --------SmileShotActor
 * --------PanaromaActor
 * --------MavActor
 */
public class Camera extends ActivityBase implements PreviewFrameLayout.OnSizeChangedListener,
        CameraScreenNail.FrameListener,
        VoiceManager.Listener {
    private static final String TAG = "Camera";
    private static final boolean LOG = Log.LOGV;
    
    public interface OnOrientationListener {
        void onOrientationChanged(int orientation);
    }
    public interface OnParametersReadyListener {
        void onCameraParameterReady();
    }
    public interface OnPreferenceReadyListener {
        void onPreferenceReady();
    }
    public interface Resumable {
        void begin();
        void resume();
        void pause();
        void finish();
    }
    public interface OnFullScreenChangedListener {
        void onFullScreenChanged(boolean full);
    }
    public interface OnSingleTapUpListener {
        void onSingleTapUp(View view, int x, int y);
    }
    
    public static final int UNKNOWN = -1;
    public static final int STATE_PREVIEW_STOPPED = 0;
    public static final int STATE_IDLE = 1;  // preview is active 
    public static final int STATE_FOCUSING = 2; // Focus is in progress. The exact focus state is in Focus.java.
    public static final int STATE_SNAPSHOT_IN_PROGRESS = 3;
    public static final int STATE_RECORDING_IN_PROGRESS = STATE_SNAPSHOT_IN_PROGRESS; //share the same value
    public static final int STATE_SWITCHING_CAMERA = 4;// Switching between cameras.
    private int mCameraState = STATE_PREVIEW_STOPPED;
    
    private boolean mCameraOpened;
    private CameraManager.CameraProxy mCameraDevice;
    private Parameters mInitialParams;
    private Parameters mParameters;
    private boolean mOpenCameraFail;
    private boolean mCameraDisabled;
    private CameraActor mCameraActor;
    private ConditionVariable mStartPreviewPrerequisiteReady = new ConditionVariable();
    private PreviewFrameLayout mPreviewFrameLayout;
    //we should cache preview width and height for onConfigurationChanged().
    private int mPreviewFrameWidth;
    private int mPreviewFrameHeight;
    private SurfaceTexture mSurfaceTexture;
    private CameraStartUpThread mCameraStartUpThread;
    private int mNumberOfCameras;
    private int mCameraId;
    // The activity is going to switch to the specified camera id. This is
    // needed because texture copy is done in GL thread. -1 means camera is not
    // switching.
    private int mPendingSwitchCameraId = UNKNOWN;
    private long mOnResumeTime;
    
    // The display rotation in degrees. This is only valid when mCameraState is
    // not PREVIEW_STOPPED.
    private int mDisplayRotation;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mCameraDisplayOrientation;
    // The value for UI components like indicators.
    private int mDisplayOrientation;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails. Ex: if the value
    // is 90, the UI components should be rotated 90 degrees counter-clockwise.
    private int mOrientationCompensation = 0;
    private MyOrientationEventListener mOrientationListener;
    
    private ModePicker mModePicker;
    private ShutterManager mShutterManager;
    private ThumbnailManager mThumbnailManager;
    private SettingManager mSettingManager;
    private IndicatorManager mIndicatorManager;
    private PickerManager mPickerManager;
    private RemainingManager mRemainingManager;
    private InfoManager mInfoManager;
    private ReviewManager mReviewManager;
    private ZoomManager mZoomManager;
    private FileSaver mFileSaver;
    private SettingChecker mSettingChecker;
    private VoiceManager mVoiceManager;
    private CameraSettings mCameraSettings;
    
    private ViewGroup mViewLayerBottom;
    private ViewGroup mViewLayerNormal;
    private ViewGroup mViewLayerTop;
    private ViewGroup mViewLayerShutter;
    private ViewGroup mViewLayerSetting;
    private ViewGroup mViewLayerOverlay;
    private FocusManager mFocusManager;
    private RotateLayout mFocusAreaIndicator;
    private ComboPreferences mPreferences;
    private PreferenceGroup mPreferenceGroup;
    private LocationManager mLocationManager;
    private RotateDialog mRotateDialog;
    private RotateProgress mRotateProgress;
    private OnScreenHint mRotateToast;
    private FaceView mFaceView;
    private LayoutInflater mInflater;
    private boolean mStereoMode;
    private String mFlashMode;
    private WfdManagerLocal mWfdLocal;
    
    private static final int MSG_CAMERA_OPEN_DONE = 1;
    private static final int MSG_CAMERA_PARAMETERS_READY = 2;
    private static final int MSG_CAMERA_PREFERENCE_READY = 3;
    private static final int MSG_CHECK_DISPLAY_ROTATION = 4;
    private static final int MSG_SWITCH_CAMERA = 5;
    private static final int MSG_SWITCH_CAMERA_START_ANIMATION = 6;
    private static final int MSG_CLEAR_SCREEN_DELAY = 7;
    private static final int MSG_SHOW_ONSCREEN_INDICATOR = 8;
    private static final int MSG_OPEN_CAMERA_FAIL = 9;
    private static final int MSG_OPEN_CAMERA_DISABLED = 10;
//    private static final int MSG_FIRST_FRAME_ARRIVED = 14;
    private static final int MSG_APPLY_PARAMETERS_WHEN_IDEL = 12;
    
    private static final int DELAY_MSG_SCREEN_SWITCH = 2 * 60 * 1000;
    private static final int DELAY_MSG_SHOW_ONSCREEN_VIEW = 3 * 1000;
    public static final int SHOW_INFO_LENGTH_LONG = 5 * 1000;

    private static String getMsgLabel(int msg) {
        switch (msg) {
            case MSG_CAMERA_OPEN_DONE: return "MSG_CAMERA_OPEN_DONE";
            case MSG_CAMERA_PARAMETERS_READY: return "MSG_CAMERA_PARAMETERS_READY";
            case MSG_CAMERA_PREFERENCE_READY: return "MSG_CAMERA_PREFERENCE_READY";
            case MSG_CHECK_DISPLAY_ROTATION: return "MSG_CHECK_DISPLAY_ROTATION";
            case MSG_SWITCH_CAMERA: return "MSG_SWITCH_CAMERA";
            case MSG_SWITCH_CAMERA_START_ANIMATION: return "MSG_SWITCH_CAMERA_START_ANIMATION";
            case MSG_CLEAR_SCREEN_DELAY: return "MSG_CLEAR_SCREEN_DELAY";
            case MSG_SHOW_ONSCREEN_INDICATOR: return "MSG_SHOW_ONSCREEN_INDICATOR";
            case MSG_OPEN_CAMERA_FAIL: return "MSG_OPEN_CAMERA_FAIL";
            case MSG_OPEN_CAMERA_DISABLED: return "MSG_OPEN_CAMERA_DISABLED";
//            case MSG_FIRST_FRAME_ARRIVED: return "MSG_FIRST_FRAME_ARRIVED";
            case MSG_APPLY_PARAMETERS_WHEN_IDEL: return "MSG_APPLY_PARAMETERS_WHEN_IDEL";
            default:
                break;
        }
        return "unknown message";
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "handleMessage(" + msg + ")");
                MMProfileManager.startProfileHandleMessage(getMsgLabel(msg.what));
            }
            switch(msg.what) {
            case MSG_CAMERA_OPEN_DONE:
                mCameraStartUpThread = null;
                break;
            case MSG_CAMERA_PARAMETERS_READY:
                notifyParametersReady();
                break;
            case MSG_CAMERA_PREFERENCE_READY:
                notifyPreferenceReady();
                break;
            case MSG_CHECK_DISPLAY_ROTATION:
                // Set the display orientation if display rotation has changed.
                // Sometimes this happens when the device is held upside
                // down and camera app is opened. Rotation animation will
                // take some time and the rotation value we have got may be
                // wrong. Framework does not have a callback for this now.
                if (Util.getDisplayRotation(Camera.this) != mDisplayRotation) {
                    setDisplayOrientation();
                    mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
                    mCameraActor.onDisplayRotate();
                }
                if (SystemClock.uptimeMillis() - mOnResumeTime < 5000) {
                    mMainHandler.sendEmptyMessageDelayed(MSG_CHECK_DISPLAY_ROTATION,
                            100);
                    if (LOG) {
                        MMProfileManager.triggersSendMessage(
                            getMsgLabel(MSG_CHECK_DISPLAY_ROTATION) + ", delayed 100");
                    }
                }
                notifyOrientationChanged();
                break;
            case MSG_SWITCH_CAMERA:
                switchCamera();
                break;
            case MSG_SWITCH_CAMERA_START_ANIMATION:
                mCameraScreenNail.animateSwitchCamera();
                break;
            case MSG_CLEAR_SCREEN_DELAY:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case MSG_SHOW_ONSCREEN_INDICATOR:
                doShowIndicator();
                break;
//            case MSG_FIRST_FRAME_ARRIVED:
//                doOnFirstFrameArrived();
//                break;
            case MSG_APPLY_PARAMETERS_WHEN_IDEL:
                applyParameters(false);
                break;
            case MSG_OPEN_CAMERA_FAIL:
                mCameraStartUpThread = null;
                mOpenCameraFail = true;
                Util.showErrorAndFinish(Camera.this, R.string.cannot_connect_camera);
                break;
            case MSG_OPEN_CAMERA_DISABLED:
                mCameraStartUpThread = null;
                mCameraDisabled = true;
                Util.showErrorAndFinish(Camera.this, R.string.camera_disabled);
                break;
            default:
                break;
            }
            if (LOG) {
                MMProfileManager.stopProfileHandleMessage();
            }
        };
    };
    
    /// M: open camera process functions @{
    private class CameraStartUpThread extends Thread {
        private boolean mCancel = false;
        
        @Override
        public void run() {
            MMProfileManager.startProfileCameraStartUp();
            //ensure current camera id is correct.
            int cameraId = getPreferredCameraId(mPreferences);
            if (mCameraId != cameraId) {
                Log.w(TAG, "CameraStartUpThread.run() camera id preference=" + cameraId + ", memory=" + mCameraId);
                CameraSettings.writePreferredCameraId(mPreferences, mCameraId);
            }
            int effectType = CameraSettings.readEffectType(mPreferences);
            Object effectParameter = CameraSettings.readEffectParameter(mPreferences);
            if (effectType != mLastEffectType || (effectParameter == null && mLastEffectParameter != null)
                    || (effectParameter != null && !effectParameter.equals(mLastEffectParameter))) {
                Log.w(TAG, "CameraStartUpThread.run() preference=(" + effectType + ", " + effectParameter
                        + ") memory=(" + mLastEffectType + ", " + mLastEffectParameter + ")");
                CameraSettings.writePreferredVideoEffect(mPreferences, mLastEffectType, mLastEffectParameter);
            }
            try {
                mCameraDevice = Util.openCamera(Camera.this, mCameraId);
                // M: added for mock camera
                prepareMockCamera();
                mCameraOpened = true;
            } catch (CameraHardwareException e) {
                mCameraActor.onCameraOpenFailed();
                mOpenCameraFail = true;
                mMainHandler.sendEmptyMessage(MSG_OPEN_CAMERA_FAIL);
                if (LOG) {
                    MMProfileManager.triggersSendMessage(
                        getMsgLabel(MSG_OPEN_CAMERA_FAIL));
                }
                MMProfileManager.stopProfileCameraStartUp();
                return;
            } catch (CameraDisabledException e) {
                mCameraActor.onCameraDisabled();
                mCameraDisabled = true;
                mMainHandler.sendEmptyMessage(MSG_OPEN_CAMERA_DISABLED);
                if (LOG) {
                    MMProfileManager.triggersSendMessage(
                        getMsgLabel(MSG_OPEN_CAMERA_DISABLED));
                }
                MMProfileManager.stopProfileCameraStartUp();
                return;
            }
            mInitialParams = CameraHolder.instance().getOriginalParameters();
            MMProfileManager.startProfileCameraParameterCopy();
            mParameters = mInitialParams.copy();
            ModeChecker.updateModeMatrix(Camera.this,mCameraId);
            MMProfileManager.stopProfileCameraParameterCopy();
            mCameraActor.onCameraOpenDone();
            MMProfileManager.startProfileCameraPreviewPreReadyBlock();
            mStartPreviewPrerequisiteReady.block();
            MMProfileManager.stopProfileCameraPreviewPreReadyBlock();
            if (mCancel) {
                MMProfileManager.stopProfileCameraStartUp();
                return;
            }
            initializeFocusManager();
            setDisplayOrientation();//should be set before initialize surface
            //updateSurfaceTexture();//should wait camera screennail.
            //setPreviewTextureAsync();
            initializeCameraPreferences();//should be rechecked
            clearDeviceCallbacks();
            applyDeviceCallbacks();
            clearViewCallbacks();
            applayViewCallbacks();
            applyParameters(true);
            mSettingChecker.applyParametersToUIImmediately();
            mOnResumeTime = SystemClock.uptimeMillis();
            mMainHandler.sendEmptyMessage(MSG_CHECK_DISPLAY_ROTATION);
            if (LOG) {
                MMProfileManager.triggersSendMessage(
                    getMsgLabel(MSG_CHECK_DISPLAY_ROTATION));
            }
            //send message to avoid onResume check
            mMainHandler.sendEmptyMessage(MSG_CAMERA_OPEN_DONE);
            if (LOG) {
                MMProfileManager.triggersSendMessage(
                    getMsgLabel(MSG_CAMERA_OPEN_DONE));
            }
            MMProfileManager.stopProfileCameraStartUp();
        }
        
        public void cancel() {
            mCancel = true;
        }
        
        public boolean isCanceled() {
            if (LOG) {
                Log.v(TAG, "isCanceled() return " + mCancel);
            }
            return mCancel;
        }
    }
    
    private void prepareMockCamera() {
        if (null == mCameraDevice) {
            return;
        }
        if (null != mCameraDevice.getCamera() &&
            FrameworksClassFactory.isMockCamera()) {
            // M: set context to create mock images
            mCameraDevice.getCamera().setContext(this);
        }
    }
    
    private void closeCamera() {
        if (LOG) {
            Log.v(TAG, "closeCamera() mCameraDevice=" + mCameraDevice);
        }
        if (mCameraDevice != null) {
            mCameraActor.onCameraClose();
            clearDeviceCallbacks();
            CameraHolder.instance().release();
            if (mFocusManager != null) {
                mFocusManager.onCameraReleased();
            }
            mCameraDevice = null;
            setCameraState(STATE_PREVIEW_STOPPED);
            mCameraOpened = false;
        }
    }
    
    private void waitCameraStartUpThread(boolean cancel) {
        if (LOG) {
            Log.v(TAG, "waitCameraStartUpThread(" + cancel + ") begin mCameraStartUpThread=" + mCameraStartUpThread);
        }
        try {
            if (mCameraStartUpThread != null) {
                if (cancel) {
                    mCameraStartUpThread.cancel();
                }
                mCameraStartUpThread.join();
                mCameraStartUpThread = null;
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "waitCameraStartUpThread()", e);
        }
        if (LOG) {
            Log.v(TAG, "waitCameraStartUpThread() end");
        }
    }
    
    private Size mLastPictureSize;
    private Size mLastPreviewSize;
    private String mLastZsdMode;
    private int mLastAudioBitRate = UNKNOWN;
    private int mLastVideoBitRate = UNKNOWN;
    private void applyParameters(boolean force) {
        if (cancelApplyParameters()) {
            return;
        }
        MMProfileManager.startProfileApplyParameters();
        lockRun(new Runnable() {
            @Override
            public void run() {
                //preview size changed, zsd changed, camera mode changed, open camera.
                mSettingChecker.applyPreferenceToParameters();
            }
        });
        //capability table will be used when value is applied.
        Size curPictureSize = mParameters.getPictureSize();
        Size curPreviewSize = mParameters.getPreviewSize();
        String curZsd = mParameters.getZSDMode();
        final boolean changedPreviewSize = !curPreviewSize.equals(mLastPreviewSize);
        final boolean changedPictureSize = !curPictureSize.equals(mLastPictureSize);
        final boolean changedZsd = curZsd == null ? mLastZsdMode != null : !curZsd.equals(mLastZsdMode);
        final boolean needRestart = changedZsd || changedPreviewSize || force;
        boolean vBRateChanged = false;
        boolean aBRateChanged = false;
        if (mProfile != null) {
            vBRateChanged = (mLastVideoBitRate == UNKNOWN ? true : mLastVideoBitRate != mProfile.videoBitRate);
            aBRateChanged = (mLastAudioBitRate == UNKNOWN ? true : mLastAudioBitRate != mProfile.audioBitRate);
        }
        if (cancelApplyParameters()) {
            MMProfileManager.stopProfileApplyParameters();
            return;
        }
        mCameraDevice.setDisplayOrientation(mCameraDisplayOrientation);
        if (needRestart) {
            mCameraActor.stopPreview();
            //videowallpaper launch camera, the preview size is not changed, not need copy texture.
            if (!isSwitchingCamera() && changedPreviewSize && mSurfaceTexture != null && !isVideoWallPaperIntent()) {
                mCameraScreenNail.copyOriginSizeTexture();
            } else {
                mCameraScreenNail.stopSwitchActorAnimation();
            }
            mCameraScreenNail.setDrawable(false);
            updateSurfaceTexture();
            setPreviewTextureAsync();
        }
        if (cancelApplyParameters()) {
            MMProfileManager.stopProfileApplyParameters();
            return;
        }
        lockRun(new Runnable() {
            @Override
            public void run() {
                setRotationToParameters(); //set rotation to parameters for face detection.
                setZoomParameter(); //maintain last ZoomValue to parameters after resume 
                applyParametersToServer();
                if (changedPreviewSize) {
                    // For picture: Zoom related settings will be changed for different preview
                    // sizes, so set and read the parameters to get latest values
                    // For video: Keep preview size up to date.
                    fetchParametersFromServer();
                }
            }
        });
        if (cancelApplyParameters()) {
            MMProfileManager.stopProfileApplyParameters();
            return;
        }
        //Zoom manager will be notified here.
        mMainHandler.sendEmptyMessage(MSG_CAMERA_PARAMETERS_READY);
        if (LOG) {
            MMProfileManager.triggersSendMessage(
                getMsgLabel(MSG_CAMERA_PARAMETERS_READY));
        }
        if (changedPictureSize || changedPreviewSize || force || vBRateChanged || aBRateChanged) {
            showRemainingAways();
        }
        mCameraActor.onCameraParameterReady(needRestart);
        if (LOG) {
            Log.v(TAG, "applyParameters(" + force + ") picturesize=" + SettingUtils.buildSize(curPictureSize)
                + " previewsize=" + SettingUtils.buildSize(curPreviewSize)
                + " oldPictureSize=" + SettingUtils.buildSize(mLastPictureSize)
                + " oldPreviewSize=" + SettingUtils.buildSize(mLastPreviewSize)
                + " changedPreviewSize=" + changedPreviewSize + ", changedPictureSize=" + changedPictureSize
                + " oldZsd=" + mLastZsdMode + ", curZsd=" + curZsd + ", changedZsd=" + changedZsd
                + " vBRateChanged=" + vBRateChanged + ", aBRateChanged=" + aBRateChanged);
        }
        mLastPictureSize = curPictureSize;
        mLastPreviewSize = curPreviewSize;
        mLastZsdMode = curZsd;
        if (mProfile != null) {
            mLastVideoBitRate = mProfile.videoBitRate;
            mLastAudioBitRate = mProfile.audioBitRate;
        }
        MMProfileManager.stopProfileApplyParameters();
    }
    
    private boolean cancelApplyParameters() {
        boolean cancel = (mCameraDevice == null || mParameters == null || mPaused
                || (mCameraStartUpThread != null && mCameraStartUpThread.isCanceled()));
        if (LOG && cancel) {
            Log.v(TAG, "cancelApplyParameters() mCameraDevice=" + mCameraDevice + ", " + "mParameters="
                    + mParameters + ", mPaused=" + mPaused + " return " + cancel);
        }
        return cancel;
    }
    
    protected void onAfterFullScreeChanged(final boolean full) {
        if (LOG) {
            Log.v(TAG, "onAfterFullScreeChanged(" + full + ")");
        }
        if (full) {
            /* M: MSG_CHECK_DISPLAY_ROTATION may get a wrong display rotation in this case:
             * Enter camera(protrait) --> change to landscape --> click thumbnail.
             * Then, when back to camera, mDisplayRotation will be 270 instead of 90.
             * So here we recheck it for this case. 
             * when CameraStartUpThread done, we can check display rotation.
             */
            if (mOnResumeTime != 0L) {
                mMainHandler.sendEmptyMessage(MSG_CHECK_DISPLAY_ROTATION);
            }
            if (LOG) {
                MMProfileManager.triggersSendMessage(
                    getMsgLabel(MSG_CHECK_DISPLAY_ROTATION));
            }
        } else {
            hideToast();
        }
        if (mSettingChecker != null) {
            lockRun(new Runnable() {
               @Override
                public void run() {
                   if (full) {
                       mSettingChecker.turnOnWhenShown();
                   } else {
                       mSettingChecker.turnOffWhenHide();
                   }
                } 
            });
        }
        notifyOnFullScreenChanged(full);
    }
    
    public void setCameraState(int state) {
        if (LOG) {
            Log.v(TAG, "setCameraState(" + state + ")");
        }
        mCameraState = state;
    }
    
    public int getCameraState() {
        return mCameraState;
    }
    
    public boolean isCameraClosed() {
        return !mCameraOpened;
    }
    
    public boolean isCameraIdle() {
        boolean idle = (mCameraState == STATE_IDLE) || ((mFocusManager != null)
                && mFocusManager.isFocusCompleted() && (mCameraState != STATE_SWITCHING_CAMERA));
        if (LOG) {
            Log.v(TAG, "isCameraIdle() mCameraState=" + mCameraState + ", return " + idle);
        }
        return idle;
    }
    
    /// @}
    
    /// M: for activity life cycle @{
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MMProfileManager.startProfileCameraOnCreate();

        parseIntent();
        
        //should be checked whether can be moved to opening thread @{
        mPreferences = new ComboPreferences(this);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mCameraId = getPreferredCameraId(mPreferences);
        mPreferences.setLocalId(this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        //@}
        
        //initial managers before new actor, so we can count views created by Camera.java
        initializeCommonManagers();
        if (isVideoCaptureIntent() || isVideoWallPaperIntent()) {
            mCameraActor = new VideoActor(this);
        } else {
            mCameraActor = new PhotoActor(this);
        }
        
        //start camera opening process
        mCameraStartUpThread = new CameraStartUpThread();
        mCameraStartUpThread.start();

        //for photo view loading camera folder
        ExtensionHelper.ensureCameraExtension(this);
        Storage.updateDefaultDirectory();

        //just set content view for preview
        MMProfileManager.startProfileCameraViewOperation();
        setContentView(R.layout.camera);
        MMProfileManager.stopProfileCameraViewOperation();
        //create camera screennail after content view inflated.
        createCameraScreenNail(isNonePickIntent());
        mCameraScreenNail.setFrameListener(this);
        
        //only initialize some thing for open
        initializeForOpeningProcess();
        MMProfileManager.startProfileCameraPreviewPreReadyOpen();
        mStartPreviewPrerequisiteReady.open();
        MMProfileManager.stopProfileCameraPreviewPreReadyOpen();
        
        //may be we can lazy this function.
        initializeAfterPreview();

        MMProfileManager.stopProfileCameraOnCreate();
    }
    
    private void initializeAfterPreview() {
        long start = System.currentTimeMillis();
        callResumableBegin();
        mModePicker.setCurrentMode(mCameraActor.getMode());
        //Here we don't use setViewState(VIEW_STATE_NORMAL) for that:
        //We will show remaining first time and setViewState() do more thing than we need.
        mShutterManager.show();
        mSettingManager.show();
        if (isNonePickIntent()) {
            //mModePicker.show(); //will do when parameters ready
            mThumbnailManager.show();
            ExtensionHelper.showAppGuide(this);
        }
        addIdleHandler();//why no flag to disable it after checked.
        long stop = System.currentTimeMillis();
        Log.v(TAG, "initializeAfterPreview() consume:" + (stop - start));
    }
    
    //Here should be lightweight functions!!!
    private void initializeCommonManagers() {
        mSettingChecker = new SettingChecker(this);
        mReviewManager = new ReviewManager(this);
        mShutterManager = new ShutterManager(this);
        mModePicker = new ModePicker(this);
        mSettingManager = new SettingManager(this);
        mThumbnailManager = new ThumbnailManager(this);
        
        mPickerManager = new PickerManager(this);
        mIndicatorManager = new IndicatorManager(Camera.this);
        mRemainingManager = new RemainingManager(this);
        mInfoManager = new InfoManager(this);
        
        mZoomManager = new ZoomManager(this);
        mFileSaver = new FileSaver(this);
        
        mRotateDialog = new RotateDialog(this);
        mRotateProgress = new RotateProgress(this);
        
        mVoiceManager = new VoiceManager(this);
        mWfdLocal = new WfdManagerLocal(this);
        
        recordCommonManagers();
        
        mModePicker.setListener(mModeChangedListener);
        mSettingManager.setListener(mSettingListener);
        mPickerManager.setListener(mPickerListener);
        mThumbnailManager.setFileSaver(mFileSaver);
        mVoiceManager.addListener(this);
        mWfdLocal.addListener(mWfdListener);
    }
    
    private void initializeForOpeningProcess() {
        MMProfileManager.startProfileInitOpeningProcess();

        mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
        
        MMProfileManager.startProfileCameraViewOperation();

        mViewLayerBottom = (ViewGroup) findViewById(R.id.view_layer_bottom);
        mViewLayerNormal = (ViewGroup) findViewById(R.id.view_layer_normal);
        mViewLayerTop = (ViewGroup) findViewById(R.id.view_layer_top);
        mViewLayerShutter = (ViewGroup) findViewById(R.id.view_layer_shutter);
        mViewLayerSetting = (ViewGroup) findViewById(R.id.view_layer_setting);
        mViewLayerOverlay = (ViewGroup) findViewById(R.id.view_layer_overlay);
        
        //for focus manager used
        mFocusAreaIndicator = (RotateLayout) findViewById(R.id.focus_indicator_rotate_layout);
        //for face detection
        if (mFaceView != null) {
            mFaceView.release();
        }
        mFaceView = (FaceView) findViewById(R.id.face_view);
        // startPreview needs this.
        mPreviewFrameLayout = (PreviewFrameLayout) findViewById(R.id.frame);
        // Set touch focus listener.
        setSingleTapUpListener(mPreviewFrameLayout);
        mPreviewFrameLayout.addOnLayoutChangeListener(this);
        mPreviewFrameLayout.setOnSizeChangedListener(this);
        
        // M: temp added for Mock Camera feature
        if (FrameworksClassFactory.isMockCamera()) {
            mPreviewFrameLayout.setBackgroundResource(R.drawable.mock_preview_rainbow);
        }

        MMProfileManager.stopProfileCameraViewOperation();
        
        //for other info.
        if (mLocationManager == null) {
            mLocationManager = new LocationManager(this, null);
        }
        if (mOrientationListener == null) {
            mOrientationListener = new MyOrientationEventListener(this);
        }
        if (LOG) {
            Log.v(TAG, "initializeForOpeningProcess() mNumberOfCameras=" + mNumberOfCameras);
        }
        MMProfileManager.stopProfileInitOpeningProcess();
    }
    
    private void updateFocusAndFace() {
        if (mFaceView != null) {
            mFaceView.clear();
            mFaceView.setVisibility(View.VISIBLE);
            mFaceView.setDisplayOrientation(mDisplayOrientation);
            CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
            mFaceView.setMirror(info.facing == CameraInfo.CAMERA_FACING_FRONT);
            mFaceView.resume();
        }
        if (mFocusManager != null) {
            mFocusManager.setFocusAreaIndicator(mFocusAreaIndicator);
        }
        if (mFocusManager != null && mFaceView != null) {
            mFocusManager.setFaceView(mFaceView);
        }
    }
    
    @Override
    protected void onResume() {
        if (LOG) {
            Log.v(TAG, "onResume() mForceFinishing=" + mForceFinishing + ", mOpenCameraFail=" + mOpenCameraFail
                    + ", mCameraDisabled=" + mCameraDisabled + ", mCameraState=" + mCameraState
                    + ", mCameraStartUpThread=" + mCameraStartUpThread);
        }
        super.onResume();
        MMProfileManager.startProfileCameraOnResume();
        if (mForceFinishing || mOpenCameraFail || mCameraDisabled) {
            return;
        }
        //For onResume()->onPause()->onResume() quickly case,
        //onFullScreenChanged(true) may be called after onPause() and before onResume().
        //So, force update app view.
        updateCameraAppViewIfNeed();
        if (mCameraState == STATE_PREVIEW_STOPPED && mCameraStartUpThread == null) {
            mCameraStartUpThread = new CameraStartUpThread();
            mCameraStartUpThread.start();
        }
        doOnResume();
        // M: patch for Picutre quality enhance
        Util.enterCameraPQMode();
        MMProfileManager.stopProfileCameraOnResume();
    }
    
    private void doOnResume() {
        long start = System.currentTimeMillis();
        mOrientationListener.enable();
        //Don't enable voice for video pick.
        if (isNonePickIntent() || isImageCaptureIntent()) {
            mVoiceManager.startUpdateVoiceState();
        }
        installIntentFilter();
        callResumableResume();
        checkViewManagerConfiguration();
        long stop = System.currentTimeMillis();
        if (LOG) {
            Log.v(TAG, "doOnResume() consume:" + (stop - start));
        }
    }
    
    private void clearFocusAndFace() {
        if (mFaceView != null) {
            mFaceView.clear();
        }
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
        }
    }
    
    @Override
    protected void onPause() {
        if (LOG) {
            Log.v(TAG, "onPause() mForceFinishing=" + mForceFinishing + ", mOpenCameraFail=" + mOpenCameraFail
                    + ", mCameraDisabled=" + mCameraDisabled);
        }
        MMProfileManager.startProfileCameraOnPause();
        super.onPause();
        MMProfileManager.stopProfileCameraOnPause();
        if (mPendingSwitchCameraId != UNKNOWN) {
            mPendingSwitchCameraId = UNKNOWN;
        }
        MMProfileManager.startProfileCameraOnPause();
        if (mForceFinishing || mOpenCameraFail || mCameraDisabled) {
            // M: patch for Picutre quality enhance
            Util.exitCameraPQMode();
            return;
        }
        waitCameraStartUpThread(true);
        closeCamera();
        releaseSurface();
        clearFocusAndFace();
        uninstallIntentFilter();
        callResumablePause();
        collapseViewManager(true);
        mOrientationListener.disable();
        mVoiceManager.stopUpdateVoiceState();
        mLocationManager.recordLocation(false);
        //when close camera, should reset mOnResumeTime
        mOnResumeTime = 0L;
        mMainHandler.removeCallbacksAndMessages(null);
        //actor will set screen on if needed, here reset it.
        resetScreenOn();
        //setLoadingAnimationVisible(true);
        // M: patch for Picutre quality enhance
        Util.exitCameraPQMode();
        MMProfileManager.stopProfileCameraOnPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MMProfileManager.startProfileCameraOnDestroy();
        //we finish worker thread when current activity destroyed.
        callResumableFinish();
        if (mFileSaver != null) {
            mFileSaver.finishAfterSaved();
        }
        SelfTimerManager.release();
        MMProfileManager.stopProfileCameraOnDestroy();
        if (LOG) {
            Log.v(TAG, "onDestroy() isChangingConfigurations()=" + isChangingConfigurations()
                    + ", mForceFinishing=" + mForceFinishing);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (LOG) {
            Log.v(TAG, "onBackPressed()");
        }
        if (mPaused || mForceFinishing) {
            return;
        }
        if (mOpenCameraFail || mCameraDisabled) {
            super.onBackPressed();
            return;
        }
        boolean handle = false;
        if (!mCameraActor.onBackPressed() && !collapseViewManager(false)) {
            super.onBackPressed();
            clearUserSettings();
        }
        Log.v(TAG, "onBackPressed() handle=" + handle);
    }
    
    private void clearUserSettings() {
        if (LOG) {
            Log.v(TAG, "clearUserSettings() isFinishing()=" + isFinishing());
        }
        if (mSettingChecker != null && isFinishing()) {
            mSettingChecker.resetSettings();
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        MMProfileManager.startProfileCameraOnConfigChange();
        super.onConfigurationChanged(newConfig);
        if (LOG) {
            Log.v(TAG, "onConfigurationChanged(" + newConfig + ")");
        }
        clearFocusAndFace();
        mRotateToast = null;
        
        MMProfileManager.startProfileCameraViewOperation();
        ViewGroup appRoot = (ViewGroup)findViewById(R.id.camera_app_root);
        appRoot.removeAllViews();
        getLayoutInflater().inflate(R.layout.preview_frame, appRoot, true);
        getLayoutInflater().inflate(R.layout.view_layers, appRoot, true);
        if (mViewLayerBottom != null) {
            mViewLayerBottom.removeAllViews();
        }
        if (mViewLayerNormal != null) {
            mViewLayerNormal.removeAllViews();
        }
        if (mViewLayerShutter != null) {
            mViewLayerShutter.removeAllViews();
        }
        if (mViewLayerSetting != null) {
            mViewLayerSetting.removeAllViews();
        }
        if (mViewLayerOverlay != null) {
            mViewLayerOverlay.removeAllViews();
        }
        MMProfileManager.stopProfileCameraViewOperation();

        // unlock orientation for new config
        setOrientation(false, OrientationEventListener.ORIENTATION_UNKNOWN);
        setDisplayOrientation();
        initializeForOpeningProcess();
        //Here we should update aspect ratio for reflate preview frame layout.
        setPreviewFrameLayoutAspectRatio();
        setLoadingAnimationVisible(false);
        updateFocusAndFace();
        reInflateViewManager();
        notifyOrientationChanged();
        MMProfileManager.stopProfileCameraOnConfigChange();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCameraActor.onActivityResult(requestCode, resultCode, data);
    }
    /// @}
    
    /// M: preview related logic @{
    private void setPreviewFrameLayoutAspectRatio() {
        // Set the preview frame aspect ratio according to the picture size.
        if (mPreviewFrameLayout != null && mParameters != null) {
            MMProfileManager.triggerSetPreviewAspectRatio();
            int width = 1;
            int height = 1;
            if (isVideoMode() && mProfile != null) {
                width = mProfile.videoFrameWidth;
                height = mProfile.videoFrameHeight;
            } else {
                //because we find full screen preview according to screen size,
                //so preview layout ratio should equals full screen preview ratio
                Size size = mParameters.getPreviewSize();
                width = size.width;
                height = size.height;
            }
            mPreviewFrameLayout.setAspectRatio((double) width / height);
            if (LOG) {
                Log.v(TAG, "setPreviewFrameLayoutAspectRatio() width=" + width + ", height=" + height);
            }
        }
    }
    
    // PreviewFrameLayout size has changed.
    @Override
    public void onSizeChanged(int width, int height) {
        if (mFocusManager != null) {
            mFocusManager.setPreviewSize(width, height);
        }
        mPreviewFrameWidth = width;
        mPreviewFrameHeight = height;
    }
    
    @Override
    public void onFirstFrameArrived() {
        if (LOG) {
            Log.v(TAG, "onFirstFrameArrived()");
        }
//        MMProfileManager.startProfileFirstFrameAvailable();
//        //Mark for some case onFristFrameArrived called from none-main thread.
//        if (Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
//            mMainHandler.sendEmptyMessage(MSG_FIRST_FRAME_ARRIVED);
//        } else {
//            doOnFirstFrameArrived();
//        }
    }
    
    private void doOnFirstFrameArrived() {
//        if (LOG) {
//            Log.v(TAG, "doOnFirstFrameArrived()");
//        }
//        setLoadingAnimationVisible(false);
//        MMProfileManager.stopProfileFirstFrameAvailable();
    }
    
    private void setLoadingAnimationVisible(boolean visible) {
        if (LOG) {
            Log.v(TAG, "setLoadingAnimationVisible(" + visible + ")");
        }
/*
        View view = findViewById(R.id.camera_app_loading_animation);
        if (view != null) {
            if (visible) {
                view.setVisibility(View.VISIBLE);
            } else {
                Util.fadeOut(view);
                view.setVisibility(View.GONE);
            }
        }
*/
    }
    
    @Override
    protected void onSingleTapUp(View view, int x, int y) {
        //Gallery use onSingleTapConfirmed() instead of onSingleTapUp().
        if (LOG) {
            Log.v(TAG, "onSingleTapUp(" + view + ", " + x + ", " + y + ")");
        }
        if (!mRotateDialog.isShowing() && !mRotateProgress.isShowing()) { //we do nothing for dialog is showing
            if (!mSettingManager.collapse(true)) {
                if (mCameraActor.getonSingleTapUpListener() != null) {
                    mCameraActor.getonSingleTapUpListener().onSingleTapUp(view, x, y);
                }
            }
        }
    }
    
    @Override
    protected void onSingleTapUpBorder(View view, int x, int y) {
        //Just collapse setting if touch border
        if (!mRotateDialog.isShowing() && !mRotateProgress.isShowing()) {
            mSettingManager.collapse(true);
        }
    }
    
    private void releaseSurface() {
        if (mSurfaceTexture != null) {
            mCameraScreenNail.releaseSurfaceTexture();
            mSurfaceTexture = null;
        }
    }
    
    private void updateSurfaceTexture() {
        MMProfileManager.startProfileUpdateSurfaceTexture();
        Size size = mParameters.getPreviewSize();
        if (mSurfaceTexture == null) {
            updateCameraScreenNailSize(UNKNOWN, UNKNOWN, size);
            mCameraScreenNail.acquireSurfaceTexture();
            mSurfaceTexture = mCameraScreenNail.getSurfaceTexture();
        } else {
            int oldWidth = mCameraScreenNail.getWidth();
            int oldHeight = mCameraScreenNail.getHeight();
            updateCameraScreenNailSize(oldWidth, oldHeight, size);
        }
        MMProfileManager.stopProfileUpdateSurfaceTexture();
    }
    
    private boolean mSurfaceTextureReady = true;
    public void setSurfaceTextureReady(boolean ready) {
        if (LOG) {
            Log.v(TAG, "setSurfaceTextureReady(" + ready + ") mSurfaceTextureReady=" + mSurfaceTextureReady);
        }
        mSurfaceTextureReady = ready;
    }
    
    public boolean getSurfaceTextureReady() {
        return mSurfaceTextureReady;
    }
    
    public void setPreviewTextureAsync() {
        if (LOG) {
            Log.v(TAG, "setPreviewTextureAsync() mSurfaceTextureReady=" + mSurfaceTextureReady
                    + ", mSurfaceTexture=" + mSurfaceTexture);
        }
        if (mCameraDevice != null && mSurfaceTexture != null && !effectsActive() && mSurfaceTextureReady) {
            MMProfileManager.triggerSetPreviewTexture();
            mCameraDevice.setPreviewTextureAsync(mSurfaceTexture);
        }
    }
    
    private void updateCameraScreenNailSize(int oldWidth, int oldHeight, Size size) {
        int width = size.width;
        int height = size.height;
        if (mCameraDisplayOrientation % 180 != 0) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        if (oldWidth != width || oldHeight != height) {
            mCameraScreenNail.setSize(width, height);
            notifyScreenNailChanged();
        }
        if (LOG) {
            Log.v(TAG, "updateCameraScreenNailSize(" + oldWidth + ", " + oldHeight
                    + ", " + SettingUtils.buildSize(size) + ")");
        }
    }
    
    private void setDisplayOrientation() {
        MMProfileManager.startProfileSetDisplayOrientation();
        mDisplayRotation = Util.getDisplayRotation(this);
        mDisplayOrientation = Util.getDisplayOrientation(
                mDisplayRotation, mCameraId);
        mCameraDisplayOrientation = Util.getDisplayOrientation(0, mCameraId);
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
        if (mFocusManager != null) {
            mFocusManager.setDisplayOrientation(mDisplayOrientation);
        }
        if (getGLRoot() != null) {
            getGLRoot().requestLayoutContentPane();
        }
        if (LOG) {
            Log.v(TAG, "setDisplayOrientation() mDisplayRotation=" + mDisplayRotation
                    + ", mCameraDisplayOrientation=" + mCameraDisplayOrientation
                    + ", mDisplayOrientation=" + mDisplayOrientation);
        }
        MMProfileManager.stopProfileSetDisplayOrientation();
    }
    /// @}
    
    /// M: for other class using API @{
    public FocusManager getFocusManager() {
        return mFocusManager;
    }
    
    public FaceView getFaceView() {
        return mFaceView;
    }
    
    public CameraManager.CameraProxy getCameraDevice() {
        return mCameraDevice;
    }
    
    //Note: please do not cache this preference for that:
    //we may re-fetch parameters from server.
    public Parameters getParameters() { //not recommended
        if (LOG) {
            Log.v(TAG, "getParameters() return " + mParameters);
        }
        return mParameters;
    }
    
    public ComboPreferences getPreferences() { //not recommended
        return mPreferences;
    }
    
    public PreferenceGroup getPreferenceGroup() { //not recommended
        return mPreferenceGroup;
    }
    
    public ListPreference getListPreference(int row) {
        return mSettingChecker.getListPreference(row);
    }
    
    public ListPreference getListPreference(String key) {
        int row = SettingUtils.index(SettingChecker.KEYS_FOR_SETTING, key);
        return getListPreference(row);
    }
    
    public FileSaver getFileSaver() {
        return mFileSaver;
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
    }
    
    public int getCurrentMode() {
        return mCameraActor.getMode();
    }
    public VoiceManager getVoiceManager() {
        return mVoiceManager;
    }
    public SettingManager getSettingManager() {
        return mSettingManager;
    }
    public SettingChecker getSettingChecker() {
        return mSettingChecker;
    }
    public RemainingManager getRemainingManager() {
        return mRemainingManager;
    }
    public ReviewManager getReviewManager() {
        return mReviewManager;
    }
    public ThumbnailManager getThumbnailManager() {
        return mThumbnailManager;
    }
    public ShutterManager getShutterManager() {
        return mShutterManager;
    }
    public WfdManagerLocal getWfdManagerLocal() {
        return mWfdLocal;
    }
    public PickerManager getPickerManager() {
        return mPickerManager;
    }
    public IndicatorManager getIndicatorManager() {
        return mIndicatorManager;
    }
    public ZoomManager getZoomManager() {
        return mZoomManager;
    }
    
    public CameraSettings getCameraSettings() {
        return mCameraSettings;
    }
    
    //activity
    public int getDisplayRotation() {
        return mDisplayRotation;
    }
    //camera
    public int getCameraDisplayOrientation() {
        return mCameraDisplayOrientation;
    }
    //sensor
    public int getOrietation() {
        return mOrientation;
    }
    //activity + sensor 
    public int getOrientationCompensation() {
        return mOrientationCompensation;
    }
    //activity + camera 
    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }
    
    public int getCameraCount() {
        return mNumberOfCameras;
    }
    public CameraActor getCameraActor() {
        return mCameraActor;
    }
    
    //should be called when starting preview
    public void setReviewOrientationCompensation(int orientationCompensation) {
        mReviewManager.setOrientationCompensation(orientationCompensation);
    }
    
    //should be called when showing review
    public void showReview(FileDescriptor fd) {
        if (LOG) {
            Log.v(TAG, "showReview(" + fd + ")");
        }
        setViewState(VIEW_STATE_REVIEW);
        mReviewManager.show(fd);
    }
    //should be called when showing review
    public void showReview(String filePath) {
        if (LOG) {
            Log.v(TAG, "showReview(" + filePath + ")");
        }
        setViewState(VIEW_STATE_REVIEW);
        mReviewManager.show(filePath);
    }
    public void showReview() {
        setViewState(VIEW_STATE_REVIEW);
        mReviewManager.show();
    }
    public void hideReview() {
        mReviewManager.hide();
        restoreViewState();
    }
    public void switchShutter(int type) {
        mShutterManager.switchShutter(type);
    }
    
    public int getPreviewFrameWidth() {
        if (LOG) {
            Log.v(TAG, "getPreviewFrameWidth() return " + mPreviewFrameHeight + ", real=" + mPreviewFrameLayout.getWidth());
        }
        return mPreviewFrameWidth;
    }

    public int getPreviewFrameHeight() {
        if (LOG) {
            Log.v(TAG, "getPreviewFrameHeight() return " + ", real=" + mPreviewFrameLayout.getHeight());
        }
        return mPreviewFrameHeight;
    }
    public  void showBorder(boolean show) {
        mPreviewFrameLayout.showBorder(show);
    }
    public int getCameraScreenNailWidth() {
        return mCameraScreenNail.getWidth();
    }
    
    public int getCameraScreenNailHeight() {
        return mCameraScreenNail.getHeight();
    }
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }
    
//    public View inflate(int layoutId) {
//        //mViewLayerNormal, mViewLayerBottom and mViewLayerTop are same ViewGroup.
//        //Here just use one to inflate child view.
//        return getLayoutInflater().inflate(layoutId, mViewLayerNormal, false);
//    }
    
    private ViewGroup getViewLayer(int layer) {
        ViewGroup viewLayer = null;
        switch (layer) {
        case ViewManager.VIEW_LAYER_BOTTOM:
            viewLayer = mViewLayerBottom;
            break;
        case ViewManager.VIEW_LAYER_NORMAL:
            viewLayer = mViewLayerNormal;
            break;
        case ViewManager.VIEW_LAYER_TOP:
            viewLayer = mViewLayerTop;
            break;
        case ViewManager.VIEW_LAYER_SHUTTER:
            viewLayer = mViewLayerShutter;
            break;
        case ViewManager.VIEW_LAYER_SETTING:
            viewLayer = mViewLayerSetting;
            break;
        case ViewManager.VIEW_LAYER_OVERLAY:
            viewLayer = mViewLayerOverlay;
            break;
        default:
            throw new RuntimeException("Wrong layer:" + layer);
        }
        if (LOG) {
            Log.v(TAG, "getViewLayer(" + layer + ") return " + viewLayer);
        }
        return viewLayer;
    }
    
    public View inflate(int layoutId, int layer) {
        //mViewLayerNormal, mViewLayerBottom and mViewLayerTop are same ViewGroup.
        //Here just use one to inflate child view.
        return getLayoutInflater().inflate(layoutId, getViewLayer(layer), false);
    }
    
    public void addView(View view, int layer) {
        ViewGroup group = getViewLayer(layer);
        if (group != null) {
            group.addView(view);
        }
    }
    
    public void removeView(View view, int layer) {
        ViewGroup group = getViewLayer(layer);
        if (group != null) {
            group.removeView(view);
        }
    }
    
    public void addView(View view) {
        addView(view, ViewManager.VIEW_LAYER_NORMAL);
    }
    
    public void removeView(View view) {
        removeView(view, ViewManager.VIEW_LAYER_NORMAL);
    }
    /// @}
    
    /// M: for listener and resumable convenient functions @{
    private List<OnFullScreenChangedListener> mFullScreenListeners = new CopyOnWriteArrayList<OnFullScreenChangedListener>();
    public boolean addOnFullScreenChangedListener(OnFullScreenChangedListener l) {
        if (!mFullScreenListeners.contains(l)) {
            return mFullScreenListeners.add(l);
        }
        return false;
    }
    
    public boolean removeOnFullScreenChangedListener(OnFullScreenChangedListener l) {
        return mFullScreenListeners.remove(l);
    }
    
    private void notifyOnFullScreenChanged(boolean full) {
        for (OnFullScreenChangedListener listener : mFullScreenListeners) {
            if (listener != null) {
                listener.onFullScreenChanged(full);
            }
        }
    }
    
    private List<OnPreferenceReadyListener> mPreferenceListeners = new CopyOnWriteArrayList<OnPreferenceReadyListener>();
    public boolean addOnPreferenceReadyListener(OnPreferenceReadyListener l) {
        if (!mPreferenceListeners.contains(l)) {
            return mPreferenceListeners.add(l);
        }
        return false;
    }

    private void notifyPreferenceReady() {
        for (OnPreferenceReadyListener listener : mPreferenceListeners) {
            if (listener != null) {
                listener.onPreferenceReady();
            }
        }
    }

    private List<OnParametersReadyListener> mParametersListeners = new CopyOnWriteArrayList<OnParametersReadyListener>();
    public boolean addOnParametersReadyListener(OnParametersReadyListener l) {
        if (!mParametersListeners.contains(l)) {
            return mParametersListeners.add(l);
        }
        return false;
    }
    private void notifyParametersReady() {
        if (isNonePickIntent()&& mCameraActor.getMode() != ModePicker.MODE_VIDEO) {
            mModePicker.show();
        }

        setPreviewFrameLayoutAspectRatio();
        mLocationManager.recordLocation(RecordLocationPreference.get(mPreferences,
                getContentResolver()));
        for (OnParametersReadyListener listener : mParametersListeners) {
            if (listener != null) {
                listener.onCameraParameterReady();
            }
        }
    }
    
    private List<ViewManager> mViewManagers = new CopyOnWriteArrayList<ViewManager>();
    private int mCommonManagerCount;
    private boolean[] mLastVisibles;
    private ViewManager[] mLastManagers;
    public boolean addViewManager(ViewManager viewManager) {
        if (!mViewManagers.contains(viewManager)) {
            return mViewManagers.add(viewManager);
        }
        return false;
    }
    
    public boolean removeViewManager(ViewManager viewManager) {
        return mViewManagers.remove(viewManager);
    }
    
    private void recordCommonManagers() {
        mCommonManagerCount = mViewManagers.size();
    }
    
    private void hideActorViews() {
        int size = mViewManagers.size();
        mLastVisibles = new boolean[size];
        mLastManagers = new ViewManager[size];
        for (int i = mCommonManagerCount - 1; i < size; i++) {
            ViewManager vm = mViewManagers.get(i);
            if (vm != null) {
                mLastManagers[i] = vm;
                mLastVisibles[i] = vm.isShowing();
            }
        }
        if (LOG) {
            Log.v(TAG, "hideActorViews() size=" + size + ", mCommonManagerCount=" + mCommonManagerCount);
        }
    }
    
    private void restoreActorViews() {
        if (mLastManagers == null) { return; }
        int size = mLastManagers.length;
        for (int i = mCommonManagerCount - 1; i < size; i++) {
            ViewManager vm = mLastManagers[i];
            if (vm != null && mViewManagers.contains(vm)) {
                if (!vm.isShowing() && mLastVisibles[i]) {
                    vm.show();
                }
            }
        }
        mLastManagers = null;
    }
    
    public boolean collapseViewManager(boolean force) {
        boolean handle = false;
        //hide dialog if it's showing.
        if (mRotateDialog.isShowing() && !force) {
            mRotateDialog.hide();
            handle = true;
        } else {
            for (ViewManager manager : mViewManagers) {
                handle = manager.collapse(force) || handle;
                if (!force && handle) {
                    break; //just collapse one sub list
                }
            }
        }
        if (LOG) {
            Log.v(TAG, "collapseViewManager(" + force + ") return " + handle);
        }
        return handle;
    }
    
    private void reInflateViewManager() {
        MMProfileManager.startProfileReInflateViewManager();
        for (ViewManager manager : mViewManagers) {
            manager.reInflate();
        }
        MMProfileManager.stopProfileReInflateViewManager();
    }

    private void checkViewManagerConfiguration() {
        for (ViewManager manager : mViewManagers) {
            manager.checkConfiguration();
        }
    }

    private List<Resumable> mResumables = new CopyOnWriteArrayList<Resumable>();
    public boolean addResumable(Resumable resumable) {
        if (!mResumables.contains(resumable)) {
            return mResumables.add(resumable);
        }
        return false;
    }
    
    public boolean removeResumable(Resumable resumable) {
        return mResumables.remove(resumable);
    }
    
    private void callResumableBegin() {
        for (Resumable resumable : mResumables) {
            resumable.begin();
        }
    }
    
    private void callResumableResume() {
        for (Resumable resumable : mResumables) {
            resumable.resume();
        }
    }
    
    private void callResumablePause() {
        for (Resumable resumable : mResumables) {
            resumable.pause();
        }
    }
    
    private void callResumableFinish() {
        for (Resumable resumable : mResumables) {
            resumable.finish();
        }
    }
    /// @}

    /// M: mode change logic @{
    private int mLastMode;
    private int mLastEffectType = EffectsRecorder.EFFECT_NONE;
    private Object mLastEffectParameter;
    private ModePicker.OnModeChangedListener mModeChangedListener = new ModePicker.OnModeChangedListener() {
        @Override
        public void onModeChanged(int newMode) {
            if (LOG) {
                Log.v(TAG, "onModeChanged(" + newMode + ") current mode = " + mCameraActor.getMode()
                        + ", state=" + mCameraState);
            }
            if (mCameraActor.getMode() != newMode) {
                releaseCameraActor();
                switch(newMode) {
                case ModePicker.MODE_PHOTO:
                    mCameraActor = new PhotoActor(Camera.this);
                    break;
                case ModePicker.MODE_HDR:
                    mCameraActor = new HdrActor(Camera.this);
                    break;
                case ModePicker.MODE_FACE_BEAUTY:
                    mCameraActor = new FaceBeautyActor(Camera.this);
                    break;
                case ModePicker.MODE_PANORAMA:
                    mCameraActor = new PanoramaActor(Camera.this);
                    break;
                case ModePicker.MODE_MAV:
                    mCameraActor = new MavActor(Camera.this);
                    break;
                case ModePicker.MODE_ASD:
                    mCameraActor = new AsdActor(Camera.this);
                    break;
                case ModePicker.MODE_SMILE_SHOT:
                    mCameraActor = new SmileActor(Camera.this);
                    break;
                case ModePicker.MODE_BEST:
                    mCameraActor = new BestActor(Camera.this);
                    break;
                case ModePicker.MODE_EV:
                    mCameraActor = new EvActor(Camera.this);
                    break;
                case ModePicker.MODE_VIDEO:
                    mCameraActor = new VideoActor(Camera.this);
                    break;
                case ModePicker.MODE_PHOTO_3D:
                    break;
                case ModePicker.MODE_VIDEO_3D:
                    break;
                case ModePicker.MODE_PHOTO_SGINLE_3D:
                    break;
                case ModePicker.MODE_PANORAMA_SINGLE_3D:
                    break;
                default:
                    mCameraActor = new PhotoActor(Camera.this);
                    break;
                }
                if (mPaused) { //startup thread will apply these things after onResume()
                    return;
                }
                //reset default focus modes.
                initializeFocusManager();
                clearDeviceCallbacks();
                applyDeviceCallbacks();
                clearViewCallbacks();
                applayViewCallbacks();
                notifyOrientationChanged();
                int oldCameraMode = SettingChecker.getCameraMode(mLastMode);
                int newCameraMode = SettingChecker.getCameraMode(mCameraActor.getMode());
                applyParameters(oldCameraMode != newCameraMode);
                mSettingChecker.applyParametersToUIImmediately();
            }
        }
    };
    
    private void releaseCameraActor() {
        if (LOG) {
            Log.v(TAG, "releaseCameraActor() mode=" + mCameraActor.getMode());
        }
        collapseViewManager(true);
        mCameraActor.release();
        mLastMode = mCameraActor.getMode();
    }
    
    private void applayViewCallbacks() {
        mShutterManager.setShutterListener(mPhotoShutterListener,
                mVideoShutterListener,
                mCameraActor.getOkListener(),
                mCameraActor.getCancelListener());
    }
    
    private void clearViewCallbacks() {
        mShutterManager.setShutterListener(null, null, null, null);
    }
    
    private void applyDeviceCallbacks() { //should be checked
        if (mCameraDevice != null) {
            mCameraDevice.setASDCallback(mCameraActor.getASDCallback());
            //mCameraDevice.setAutoFocusMoveCallback(mCameraActor.getAutoFocusMoveCallback());
            mCameraDevice.setCShotDoneCallback(mCameraActor.getContinuousShotDone());
            mCameraDevice.setErrorCallback(mCameraActor.getErrorCallback());
            mCameraDevice.setFaceDetectionListener(mCameraActor.getFaceDetectionListener());
            mCameraDevice.setMAVCallback(mCameraActor.getMAVCallback());
            //mCameraDevice.setPreviewDoneCallback(mCameraActor.getZSDPreviewDone());
            mCameraDevice.setSmileCallback(mCameraActor.getSmileCallback());
            //mCameraDevice.setZoomChangeListener(mCameraActor.getOnZoomChangeListener());
        }
    }
    
    private void clearDeviceCallbacks() {
        if (mCameraDevice != null) {
            mCameraDevice.setASDCallback(null);
            mCameraDevice.setAutoFocusMoveCallback(null);
            mCameraDevice.setCShotDoneCallback(null);
            mCameraDevice.setErrorCallback(null);
            mCameraDevice.setFaceDetectionListener(null);
            mCameraDevice.setMAVCallback(null);
            mCameraDevice.setPreviewDoneCallback(null);
            mCameraDevice.setSmileCallback(null);
            mCameraDevice.setZoomChangeListener(null);
        }
    }
    
    private OnShutterButtonListener mVideoShutterListener = new OnShutterButtonListener() {
        
        @Override
        public void onShutterButtonLongPressed(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Video.onShutterButtonLongPressed(" + button + ")");
            }
            mSettingManager.collapse(true);
            OnShutterButtonListener listener = mCameraActor.getVideoShutterButtonListener();
            if (listener != null) {
                listener.onShutterButtonLongPressed(button);
            }
        }
        
        @Override
        public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
            if (LOG) {
                Log.v(TAG, "Video.onShutterButtonFocus(" + button + ", " + pressed + ")");
            }
            mSettingManager.collapse(true);
            OnShutterButtonListener listener = mCameraActor.getVideoShutterButtonListener();
            if (listener != null) {
                listener.onShutterButtonFocus(button, pressed);
            }
        }
        
        @Override
        public void onShutterButtonClick(final ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Video.onShutterButtonClick(" + button + ") isFullScreen()=" + isFullScreen()
                        + ",mCameraOpened = " + mCameraOpened + ",mCameraStartUpThread =" + mCameraStartUpThread);
            }
            mSettingManager.collapse(true);
            // do not call after onpause or before onresume's mCameraStartUpThread start completely.
            if (isFullScreen() && mCameraOpened && (mCameraStartUpThread == null)) {
                OnShutterButtonListener listener = mCameraActor.getVideoShutterButtonListener();
                if (listener != null) {
                    listener.onShutterButtonClick(button);
                } else if (mModePicker.getModeIndex(mCameraActor.getMode()) != ModePicker.MODE_VIDEO) {
                    if (Storage.getLeftSpace() > 0) {
                        mModePicker.setCurrentMode(ModePicker.MODE_VIDEO);//new video mode
                        mModePicker.setEnabled(false);
                        mModePicker.hide();
                        setPreviewFrameLayoutAspectRatio();
                        //re-call onShutterButtonClick() to start recording.
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onShutterButtonClick(button);
                            }
                        });
                    }
                }
            }
        }
    };
    
    private OnShutterButtonListener mPhotoShutterListener = new OnShutterButtonListener() {
        
        @Override
        public void onShutterButtonLongPressed(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Photo.onShutterButtonLongPressed(" + button + ")");
            }
            mSettingManager.collapse(true);
            OnShutterButtonListener listener = mCameraActor.getPhotoShutterButtonListener();
            if (listener != null) {
                listener.onShutterButtonLongPressed(button);
            }
        }
        
        @Override
        public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
            if (LOG) {
                Log.v(TAG, "Photo.onShutterButtonFocus(" + button + ", " + pressed + ")");
            }
            mSettingManager.collapse(true);
            OnShutterButtonListener listener = mCameraActor.getPhotoShutterButtonListener();
            if (listener != null) {
                listener.onShutterButtonFocus(button, pressed);
            }
        }
        
        @Override
        public void onShutterButtonClick(ShutterButton button) {
            if (LOG) {
                Log.v(TAG, "Photo.onShutterButtonClick(" + button + ")isFullScreen()=" + isFullScreen());
            }
            if (isFullScreen()) {
                mSettingManager.collapse(true);
                OnShutterButtonListener listener = mCameraActor.getPhotoShutterButtonListener();
                if (listener != null) {
                    listener.onShutterButtonClick(button);
                }
            }
        }
    };
    
    public void backToLastMode() {
        if (LOG) {
            Log.v(TAG, "backToLastMode() mLastMode=" + mLastMode);
        }
        boolean visible = ModeChecker.getModePickerVisible(this, getCameraId(), mLastMode);
        if (!visible) { //if current camera not support current mode, change it to default.
            mLastMode = ModePicker.MODE_PHOTO;
        }
        //Here we wait startup thread if back late.
        waitCameraStartUpThread(false);
        mModePicker.setCurrentMode(mLastMode);
    }
    /// @}
    
    /// M: focus manager logic @{
    private void initializeFocusManager() {
        MMProfileManager.startProfileInitFocusManager();
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFocusManager.clearFocusOnContinuous();
                }
            });
            mFocusManager.release();
        }
        // Create FocusManager object. startPreview needs it.
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        String[] defaultFocusModes = SettingChecker.getModeDefaultFocusModes(this, mCameraActor.getMode());
        mFocusManager = new FocusManager(this, mPreferences, defaultFocusModes,
                mFocusAreaIndicator, mInitialParams, mCameraActor.getFocusManagerListener(),
                mirror, getMainLooper(), SettingChecker.getModeContinousFocusMode(mCameraActor.getMode()));
        mFocusManager.setPreviewSize(getPreviewFrameWidth(), getPreviewFrameHeight());
        mFocusManager.setDisplayOrientation(mDisplayOrientation);
        MMProfileManager.stopProfileInitFocusManager();
    }
    /// @}
    
    /// M: preference logic @{
    private int getPreferredCameraId(ComboPreferences preferences) {
        int intentCameraId = Util.getCameraFacingIntentExtras(this);
        if (intentCameraId != UNKNOWN) {
            // Testing purpose. Launch a specific camera through the intent
            // extras.
            return intentCameraId;
        } else {
            return CameraSettings.readPreferredCameraId(preferences);
        }
    }
    
    private void initializeCameraPreferences() {
        if (LOG) {
            Log.v(TAG, "initializeCameraPreferences() mPreferenceGroup=" + mPreferenceGroup);
        }
        MMProfileManager.startProfileInitPref();
        if (mPreferenceGroup == null) { //should be rechecked for switching camera case
            mCameraSettings = new CameraSettings(this, mInitialParams,
                    mCameraId, CameraHolder.instance().getCameraInfo());
            mPreferenceGroup = SettingChecker.filterUnsuportedPreference(mCameraSettings, 
                    mCameraSettings.getPreferenceGroup(R.xml.camera_preferences), mCameraId);
            limitSettingsByIntent();
        }
        mMainHandler.sendEmptyMessage(MSG_CAMERA_PREFERENCE_READY);
        if (LOG) {
            MMProfileManager.triggersSendMessage(
                getMsgLabel(MSG_CAMERA_PREFERENCE_READY));
        }
        MMProfileManager.stopProfileInitPref();
    }
    ///@

    /// M: setting related @{
    /* setRotation//for taken
     * Utils.setGpsParameters//for taken
     * setCapturePath//for taken
     */
    private void applyParameterForCapture(final SaveRequest request) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                // Set rotation and gps data. for picture taken
                int jpegRotation = Util.getJpegRotation(mCameraId, mOrientation);
                mParameters.setRotation(jpegRotation);
                request.setJpegRotation(jpegRotation);
                Location loc = mLocationManager.getCurrentLocation();
                Util.setGpsParameters(mParameters, loc);
                mParameters.setCapturePath(request.getTempFilePath());
                //Note: here doesn't fetch parameters from server
                applyParametersToServer();
            }
        });
    }
    
    public void applyContinousShot() {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mSettingChecker.enableContinuousShot();
                applyParametersToServer();
            }
        });
    }
    
    public void cancelContinuousShot() {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mSettingChecker.disableContinuousShot();
                applyParametersToServer();
            }
        });
    }
    
    public void applyParameterForFocus(final boolean setArea) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mSettingChecker.applyFocusCapabilities(setArea);
                //Note: here doesn't fetch parameters from server
                //set the focus mode to server
                applyParametersToServer();
            }
        });
    }
    
    public void applyContinousCallback() {
        if (LOG) {
            Log.v(TAG, "applyContinousCallback() mCameraDevice=" + mCameraDevice);
        }
        //Here set AutoFocusMoveCallback dynamically.
        if (mCameraDevice != null) {
            if (mFocusManager.getContinousFocusSupported()) {
                mCameraDevice.setAutoFocusMoveCallback(mCameraActor.getAutoFocusMoveCallback());
            } else {
                mCameraDevice.setAutoFocusMoveCallback(null);
            }
        }
    }
    
    public SaveRequest preparePhotoRequest() {
        return preparePhotoRequest(Storage.FILE_TYPE_PHOTO, Storage.PICTURE_TYPE_JPG);
    }
    
    public SaveRequest preparePhotoRequest(int type, int pictureType) {
        SaveRequest request = mFileSaver.preparePhotoRequest(type, pictureType);
        applyParameterForCapture(request);
        return request;
    }

    //resolution
    public SaveRequest prepareVideoRequest(int outputFileFormat, String resolution) {
        SaveRequest request = mFileSaver.prepareVideoRequest(outputFileFormat, resolution);
        return request;
    }
    
    //Fetch parameters from server for something changed by server.
    //Note: mCameraDevice.getParameters() consumes long time, please don't use it unnecessary.
    public Parameters fetchParametersFromServer() {
        if (LOG) {
            Log.v(TAG, "fetchParameterFromServer() mParameters=" + mParameters + ", mCameraDevice=" + mCameraDevice);
        }
        if (mCameraDevice != null) {
            mParameters = mCameraDevice.getParameters();
        }
        if (LOG) {
            Log.v(TAG, "fetchParameterFromServer() new mParameters=" + mParameters);
        }
        return mParameters;
    }
    
    //Apply parameters to server, so native camera can apply current settings.
    //Note: mCameraDevice.setParameters() consumes long time, please don't use it unnecessary.
    public void applyParametersToServer() {
        if (mCameraDevice != null && mParameters != null) {
            mCameraDevice.setParameters(mParameters);
        }
        if (LOG) {
            Log.v(TAG, "applyParameterToServer() mParameters=" + mParameters + ", mCameraDevice=" + mCameraDevice);
        }
    }
    
    public void lockRun(Runnable runnable) {
        if (LOG) {
            Log.v(TAG, "lockRun(" + runnable + ") mCameraDevice=" + mCameraDevice);
        }
        if (mCameraDevice != null) {
            mCameraDevice.lockParametersRun(runnable);
        }
    }
    
    public int getCameraId() {
        return mCameraId;
    }
    
    private SettingManager.SettingListener mSettingListener = new SettingManager.SettingListener() {
        @Override
        public void onSharedPreferenceChanged() {
            int effectType = CameraSettings.readEffectType(mPreferences);
            Object effectParameter = CameraSettings.readEffectParameter(mPreferences);
            boolean oldIsEffects = (mLastEffectType != EffectsRecorder.EFFECT_NONE);
            boolean newIsEffects = (effectType != EffectsRecorder.EFFECT_NONE);
            boolean changedParameter = !((effectParameter == null && mLastEffectParameter == null)
                    || (effectParameter != null && effectParameter.equals(mLastEffectParameter)));
            if (LOG) {
                Log.v(TAG, "onSharedPreferenceChanged() effectType=" + effectType + ", effectParameter=" + effectParameter
                        + ", oldIsEffects=" + oldIsEffects + ", newIsEffects=" + newIsEffects
                        + ", mLastEffectType=" + mLastEffectType + ", mLastEffectParameter=" + mLastEffectParameter);
            }
            mLastEffectType = effectType; //here we assign new type to mLastEffectType for other classes.
            mLastEffectParameter = effectParameter;
            if (oldIsEffects != newIsEffects) { //effects changed
                mSettingManager.collapse(true);
                if (newIsEffects) {
                    if (isNonePickIntent()) {
                        mModePicker.setCurrentMode(ModePicker.MODE_VIDEO);
                    } else {
                        applyParameters(false);
                    }
                } else {
                    if (isNonePickIntent()) {
                        mCameraActor.onEffectsDeactive();
                        backToLastMode();
                    } else {
                        mCameraActor.onEffectsDeactive();
                        applyParameters(false);
                    }
                }
            } else {
                if (changedParameter) {
                    mSettingManager.collapse(true);
                }
                applyParameters(false);
            }
        }
        
        @Override
        public void onRestorePreferencesClicked() {
            if (!mSettingManager.isEnabled()) {
                return;
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mLastEffectType = EffectsRecorder.EFFECT_NONE;
                    mLastEffectParameter = null;
                    mCameraActor.onRestoreSettings();
                    collapseViewManager(true);
                    CameraSettings.restorePreferences(Camera.this, mPreferences, mParameters);
                    mZoomManager.resetZoom();
                    //we should apply parameters if mode is default too.
                    if (ModePicker.MODE_PHOTO == mCameraActor.getMode() || !isNonePickIntent()) {
                        applyParameters(false);
                        mSettingChecker.applyParametersToUIImmediately();
                    } else {
                        mModePicker.setCurrentMode(ModePicker.MODE_PHOTO);
                    }
                }
            };
            showAlertDialog(null,
                    getString(R.string.confirm_restore_message),
                    getString(android.R.string.ok), runnable,
                    getString(android.R.string.cancel), null);
        }
    };

    public void notifyPreferenceChanged() {
        mSettingListener.onSharedPreferenceChanged();
        mSettingManager.refresh();
    }
    /// @}
    
    /// M: for photo info @{
    public String getSelfTimer() {
        String seflTimer = mSettingChecker.getSettingCurrentValue(SettingChecker.ROW_SETTING_SELF_TIMER);
        if (LOG) {
            Log.v(TAG, "getSelfTimer() return " + seflTimer);
        }
        return seflTimer;
    }
    /// @}
    
    /// M: for video info @{
    private int mQualityId;
    private CamcorderProfile mProfile;
    private int mTimelapseMs;
    
    public CamcorderProfile fetchProfile(int quality, int timelapseMs) {
        if (LOG) {
            Log.v(TAG, "fetchProfile(" + quality + ", " + timelapseMs + ")");
        }
        mTimelapseMs = timelapseMs;
        // TODO: This should be checked instead directly +1000.
        if (mTimelapseMs != 0) {
            quality += 1000;
        }
        mQualityId = quality;
        // use class factory to fetch the profile
        mProfile = FrameworksClassFactory.getMtkCamcorderProfile(mCameraId, quality);
        //revise video profile for operator.
        ExtensionHelper.getFeatureExtension().checkMMSVideoCodec(quality, mProfile);
        if (LOG) {
            Log.v(TAG, "fetchProfile() mProfile.videoFrameRate=" + mProfile.videoFrameRate
                    + ", mProfile.videoFrameWidth=" + mProfile.videoFrameWidth
                    + ", mProfile.videoFrameHeight=" + mProfile.videoFrameHeight
                    + ", mProfile.audioBitRate=" + mProfile.audioBitRate
                    + ", mProfile.videoBitRate=" + mProfile.videoBitRate
                    + ", mProfile.quality=" + mProfile.quality
                    + ", mProfile.duration=" + mProfile.duration);
        }
        //here we limit preview size according intent requirement.
        limitPreviewByIntent(mProfile);
        return mProfile;
    }
    
    public CamcorderProfile getProfile() {
        return mProfile;
    }
    
    public int getQualityId() {
        return mQualityId;
    }
    
    public int getTimelapseMs() {
        return mTimelapseMs;
    }
    
    public int getEffectType() {
        return mLastEffectType;
    }
    
    public boolean effectsActive() {
        boolean active = mLastEffectType != EffectsRecorder.EFFECT_NONE;
        if (LOG) {
            Log.v(TAG, "effectsActive() mLastEffectType=" + mLastEffectType + ", return " + active);
        }
        return active;
    }
    
    public Object getEffectParameter() {
        return mLastEffectParameter;
    }
    
    public boolean getMicrophone() {
        return CameraSettings.VIDEO_MICRIPHONE_ON.equals(mSettingChecker.getPreferenceValue(
                SettingChecker.ROW_SETTING_MICROPHONE));
    }
    
    public String getAudioMode() {
        return mSettingChecker.getPreferenceValue(SettingChecker.ROW_SETTING_AUDIO_MODE);
    }

    public void resetLiveEffect(boolean clear)  {
        if (LOG) {
            Log.v(TAG, "resetLiveEffect(" + clear + ")");
        }
        ComboPreferences.Editor editor = mPreferences.edit();
        editor.putString(CameraSettings.KEY_VIDEO_EFFECT,
                    getString(R.string.pref_video_effect_default));
        editor.apply();
        if (clear) {
            mLastEffectType = EffectsRecorder.EFFECT_NONE;
            mLastEffectParameter = null;
        }
        if (mSettingManager != null) {
            mSettingManager.collapse(true);
        }
    }
    /// @}
    
    /// M: orientation case @{
    /// M: for listener and resumable convenient functions @{
    private List<OnOrientationListener> mOrientationListeners = new CopyOnWriteArrayList<OnOrientationListener>();
    public boolean addOnOrientationListener(OnOrientationListener l) {
        if (!mOrientationListeners.contains(l)) {
            return mOrientationListeners.add(l);
        }
        return false;
    }
    
    public boolean removeOnOrientationListener(OnOrientationListener l) {
        return mOrientationListeners.remove(l);
    }
    
    private void notifyOrientationChanged() {
        if (LOG) {
            Log.v(TAG, "notifyOrientationChanged() mOrientationCompensation=" + mOrientationCompensation
                    + ", mDisplayRotation=" + mDisplayRotation);
        }
        MMProfileManager.startProfileNotifyOrientChanged();
        for (OnOrientationListener listener : mOrientationListeners) {
            if (listener != null) {
                listener.onOrientationChanged(mOrientationCompensation);
            }
        }
        MMProfileManager.stopProfileNotifyOrientChanged();
    }
    
    private class MyOrientationEventListener extends OrientationEventListener {
        private boolean mLock = false;
        private int mRestoreOrientation;
        
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        public void setLock(boolean lock) {
            mLock = lock;
            if (!mLock) {
                onOrientationChanged(mRestoreOrientation);
            }
        }

        public boolean getLock() {
            return mLock;
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            if (mLock) {
                //Use case: MAV mode
                //We need unlock orientation when orientation is 90 or 270 and than lock
                //So that we can use MAV in landscape
                if (mCameraActor != null
                        && (Util.roundOrientation(orientation, 90) == 90 || Util
                                .roundOrientation(orientation, 270) == 270)) {
                    mCameraActor.onDisplayRotate();
                }
                mRestoreOrientation = orientation;
                return;
            }
            if (Util.getDisplayRotation(Camera.this) != mDisplayRotation) {
                setDisplayOrientation();
            }
            MMProfileManager.startProfileCameraOnOrientChange();
            int newOrientation = Util.roundOrientation(orientation, mOrientation);
            if (mCameraActor != null && mOrientation != newOrientation) {
                mCameraActor.onOrientationChanged(newOrientation);
            }
            mOrientation = newOrientation;
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation =
                    (mOrientation + Util.getDisplayRotation(Camera.this)) % 360;
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                notifyOrientationChanged();
                if (isVideoWallPaperIntent() && !mReviewManager.isShowing() && mCameraState == STATE_IDLE) {
                    applyParameters(false); /// M: apply new preview size for orientation.
                }
                lockRun(new Runnable() {
                    @Override
                    public void run() {
                        setRotationToParameters();
                        applyParametersToServer();
                    }
                });
            }
            if (LOG) {
                Log.v(TAG, "onOrientationChanged(" + orientation + ") mOrientation=" + mOrientation
                        + ", mOrientationCompensation=" + mOrientationCompensation);
            }
            MMProfileManager.stopProfileCameraOnOrientChange();
        }
    }

    public void setOrientation(boolean lock, int orientation) {
        if (LOG) {
            Log.v(TAG, "setOrientation orientation=" + orientation
                    + " mOrientationListener.getLock()=" + mOrientationListener.getLock()
                    + " lock = " + lock);
        }
        if (mOrientationListener.getLock() == lock) {
            return;
        }
        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            mOrientationListener.onOrientationChanged(orientation);
        }
        mOrientationListener.setLock(lock);
    }

    public void setRotationToParameters() {
        int jpegRotation = UNKNOWN;
        if (mParameters != null) {
            jpegRotation = Util.getJpegRotation(mCameraId, mOrientation);
            mParameters.setRotation(jpegRotation);
        }
        if (LOG) {
            Log.v(TAG, "setRotationToParameters() mCameraId=" + mCameraId + ", mOrientation=" + mOrientation
                    + ", jpegRotation=" + jpegRotation);
        }
    }
    
    private void setZoomParameter() {
        if (mZoomManager != null) {
            mZoomManager.setZoomParameter();
        }
    }
    /// @}

    /// M: multiple camera and mode related @{
    private void switchCamera() {
        if (LOG) {
            Log.v(TAG, "switchCamera() begin id=" + mPendingSwitchCameraId + ", mPaused=" + mPaused);
        }
        if (mPaused || mOpenCameraFail || mCameraDisabled || mPendingSwitchCameraId == UNKNOWN) {
            return;
        }
        MMProfileManager.startProfileSwitchCamera();
        //Note: should disable sliding
        mCameraId = mPendingSwitchCameraId;
        mPickerManager.setCameraId(mCameraId);

        // from onPause
        closeCamera();
        collapseViewManager(true);
        clearFocusAndFace();
        
        // Restart the camera and initialize the UI. From onCreate.
        mPreferences.setLocalId(Camera.this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        
        //here set these variables null to initialize them again.
        mPreferenceGroup = null;
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
            mFocusManager.release();
            mFocusManager = null;
        }
        
        CameraStartUpThread cameraOpenThread = new CameraStartUpThread();
        cameraOpenThread.start();
        
        try {
            cameraOpenThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        // Start switch camera animation. Post a message because
        // onFrameAvailable from the old camera may already exist.
        mMainHandler.sendEmptyMessage(MSG_SWITCH_CAMERA_START_ANIMATION);
        if (LOG) {
            MMProfileManager.triggersSendMessage(
                getMsgLabel(MSG_SWITCH_CAMERA_START_ANIMATION));
        }
        //end switching camera
        mPendingSwitchCameraId = UNKNOWN;
        refreshModeRelated();
        restoreViewState();
        if (LOG) {
            Log.v(TAG, "switchCamera() end camera id=" + mCameraId);
        }
        MMProfileManager.stopProfileSwitchCamera();
    }
    
    public void animateCapture() {
        // video -> image will resize preview size, copy texture to avoid stretch preview frame.
        if (mModePicker.getModeIndex(mCameraActor.getMode()) == ModePicker.MODE_VIDEO) {
            mCameraScreenNail.copyOriginSizeTexture();
        }
        boolean rotationLocked = (1 != Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0));
        int rotation = 0;
        if (rotationLocked) {
            mCameraScreenNail.animateCapture(rotation);
        } else {
            rotation = (mOrientationCompensation - mDisplayRotation + 360) % 360;
            mCameraScreenNail.animateCapture(rotation);
        }
        if (LOG) {
            Log.v(TAG, "animateCapture() rotation=" + rotation + ", mDisplayRotation=" + mDisplayRotation
                    + ", mOrientationCompensation=" + mOrientationCompensation + ", rotationLocked=" + rotationLocked);
        }
    }
    
    public void requestRender() {
        mAppBridge.requestRender();
    }

    // Preview texture has been copied. Now camera can be released and the
    // animation can be started.
    @Override
    protected void onPreviewTextureCopied() {
        if (LOG) {
            Log.v(TAG, "onPreviewTextureCopied()");
        }
        mMainHandler.sendEmptyMessage(MSG_SWITCH_CAMERA);
        if (LOG) {
            MMProfileManager.triggersSendMessage(
                getMsgLabel(MSG_SWITCH_CAMERA));
        }
    }
    
    private PickerManager.PickerListener mPickerListener = new PickerManager.PickerListener() {
        @Override
        public boolean onCameraPicked(int cameraId) {
            if (LOG) {
                Log.v(TAG, "onCameraPicked(" + cameraId + ") mPaused=" + mPaused
                        + " mPendingSwitchCameraId=" + mPendingSwitchCameraId);
            }
            //Here we always return false for switchCamera will change preference after real switching.
            if (mPaused || mPendingSwitchCameraId != UNKNOWN) {
                return false;
            }
            // Disable all camera controls.
            setCameraState(STATE_SWITCHING_CAMERA);
            setViewState(VIEW_STATE_SWITCHING);
            // We need to keep a preview frame for the animation before
            // releasing the camera. This will trigger onPreviewTextureCopied.
            mCameraScreenNail.copyTexture();
            mPendingSwitchCameraId = cameraId;
            return false;
        }
    
        @Override
        public boolean onFlashPicked(String flashMode) {
            mFlashMode = flashMode;
            mMainHandler.sendEmptyMessage(MSG_APPLY_PARAMETERS_WHEN_IDEL);
            if (LOG) {
                MMProfileManager.triggersSendMessage(
                    getMsgLabel(MSG_APPLY_PARAMETERS_WHEN_IDEL));
            }
            return true;
        }
    
        @Override
        public boolean onStereoPicked(boolean stereoType) {
            mStereoMode = stereoType;
            refreshModeRelated();
            return true;
        }
    };
    
    public boolean isStereoMode() {
        return mStereoMode;
    }
    
    public boolean isSwitchingCamera() {
        if (LOG) {
            Log.v(TAG, "isSwitchingCamera() mCurrentViewState=" + mCurrentViewState
                    + ", mPendingSwitchCameraId=" + mPendingSwitchCameraId);
        }
        return mPendingSwitchCameraId != UNKNOWN;
    }
    
    private void refreshModeRelated() {
        //should update ModePicker mode firstly.
        mModePicker.refresh();
        mPickerManager.refresh();
        mShutterManager.refresh();
    }
    /// @}
    
    /// M: for screen on and user action@{
    @Override
    public void onUserInteraction() {
        if (!mCameraActor.onUserInteraction()) {
            super.onUserInteraction();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do not handle any key if the activity is paused.
        if (mPaused) {
            return true;
        }
        if (isFullScreen() && KeyEvent.KEYCODE_MENU == keyCode && event.getRepeatCount() == 0
                && mSettingManager.handleMenuEvent()) {
            return true;
        }
        if (!mCameraActor.onKeyDown(keyCode, event)) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPaused) {
            return true;
        }
        if (!mCameraActor.onKeyUp(keyCode, event)) {
            return super.onKeyUp(keyCode, event);
        }
        return true;
    }

    public void resetScreenOn() {
        if (LOG) {
            Log.v(TAG, "resetScreenOn()");
        }
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void keepScreenOnAwhile() {
        if (LOG) {
            Log.v(TAG, "keepScreenOnAwhile()");
        }
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_DELAY, DELAY_MSG_SCREEN_SWITCH);
        if (LOG) {
            MMProfileManager.triggersSendMessage(
                getMsgLabel(MSG_APPLY_PARAMETERS_WHEN_IDEL) +
                    ", delayed for " + DELAY_MSG_SCREEN_SWITCH);
        }
    }
    
    public void keepScreenOn() {
        if (LOG) {
            Log.v(TAG, "keepScreenOn()");
        }
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    /// @}
    
    /// M: for pick logic @{
    /**
     * An unpublished intent flag requesting to start recording straight away
     * and return as soon as recording is stopped.
     * TODO: consider publishing by moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    private static final String EXTRA_VIDEO_WALLPAPER_IDENTIFY = "identity";//String
    private static final String EXTRA_VIDEO_WALLPAPER_RATION = "ratio";//float
    private static final String EXTRA_VIDEO_WALLPAPER_IDENTIFY_VALUE = "com.mediatek.vlw";
    private static final String EXTRA_PHOTO_CROP_VALUE = "crop";
    private static final float WALLPAPER_DEFAULT_ASPECTIO = 1.2f;
    private static final int WALLPAPER_MIN_WIDTH = 300;//should be rechecked
    
    private static final int PICK_TYPE_NORMAL = 0;
    private static final int PICK_TYPE_PHOTO = 1;
    private static final int PICK_TYPE_VIDEO = 2;
    private static final int PICK_TYPE_WALLPAPER = 3;
    private int mPickType;
    private boolean mQuickCapture;
    private float mWallpaperAspectio;
    private Uri mSaveUri;
    private long mLimitedSize;
    private String mCropValue;
    private int mLimitedDuration;
    
    private void parseIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)) {
            mPickType = PICK_TYPE_PHOTO;
        } else if (EXTRA_VIDEO_WALLPAPER_IDENTIFY_VALUE.equals(
                intent.getStringExtra(EXTRA_VIDEO_WALLPAPER_IDENTIFY))) {
            mWallpaperAspectio = intent.getFloatExtra(EXTRA_VIDEO_WALLPAPER_RATION,
                    WALLPAPER_DEFAULT_ASPECTIO);
            intent.putExtra(EXTRA_QUICK_CAPTURE, true);
            mPickType = PICK_TYPE_WALLPAPER;
        } else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action)) {
            mPickType = PICK_TYPE_VIDEO;
        } else {
            mPickType = PICK_TYPE_NORMAL;
        }
        if (mPickType != PICK_TYPE_NORMAL) {
            mQuickCapture = intent.getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
            mSaveUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            mLimitedSize = intent.getLongExtra(MediaStore.EXTRA_SIZE_LIMIT, 0L);
            mCropValue = intent.getStringExtra(EXTRA_PHOTO_CROP_VALUE);
            mLimitedDuration = intent.getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0);
        }
        if (LOG) {
            Log.v(TAG, "parseIntent() mPickType=" + mPickType + ", mQuickCapture="
                    + mQuickCapture + ", mSaveUri=" + mSaveUri + ", mLimitedSize=" + mLimitedSize
                    + ", mCropValue=" + mCropValue + ", mLimitedDuration=" + mLimitedDuration);
        }
        if (false) {
            Log.v(TAG, "parseIntent() action=" + intent.getAction());
            Bundle extra = intent.getExtras();
            if (extra != null) {
                for (String key : extra.keySet()) {
                    Log.v(TAG, "parseIntent() extra[" + key + "]=" + extra.get(key));
                }
            }
            if (intent.getCategories() != null) {
                for (String key : intent.getCategories()) {
                    Log.v(TAG, "parseIntent() getCategories=" + key);
                }
            }
            Log.v(TAG, "parseIntent() data=" + intent.getData());
            Log.v(TAG, "parseIntent() flag=" + intent.getFlags());
            Log.v(TAG, "parseIntent() package=" + intent.getPackage());
        }
    }

    public boolean isNonePickIntent() {
        return PICK_TYPE_NORMAL == mPickType;
    }
    
    public boolean isImageCaptureIntent() {
        return PICK_TYPE_PHOTO == mPickType;
    }
    
    public boolean isVideoCaptureIntent() {
        return PICK_TYPE_VIDEO == mPickType;
    }
    
    public boolean isVideoWallPaperIntent() {
        return PICK_TYPE_WALLPAPER == mPickType;
    }
    
    public float getWallpaperPickAspectio() {
        return mWallpaperAspectio;
    }
    
    public boolean isQuickCapture() {
        return mQuickCapture;
    }
    
    public Uri getSaveUri() {
        return mSaveUri;
    }
    
    public long getLimitedSize() {
        return mLimitedSize;
    }

    public String getCropValue() {
        return mCropValue;
    }
    
    public int getLimitedDuration() {
        return mLimitedDuration;
    }
    
    private void limitPreviewByIntent(CamcorderProfile profile) {
        if (LOG) { //original size
            Log.v(TAG, "limitPreviewByIntent() videoFrameWidth=" + profile.videoFrameWidth
                    + ", videoFrameHeight=" + profile.videoFrameHeight + ", mDisplayRotation=" + mDisplayRotation
                    + ", mOrientationCompensation=" + mOrientationCompensation);
        }
        if (isVideoWallPaperIntent() && mParameters != null) {
            final List<Size> previewSizes = new ArrayList<Size>(mParameters.getSupportedPreviewSizes());
            final int orientation = mOrientationCompensation;
            Size minSize = previewSizes.get(previewSizes.size() - 1); //last one is biggest one.
            float minAspectio = (float)minSize.width / minSize.height;
            for (Iterator<Size> iter = previewSizes.iterator(); iter.hasNext();) {
                Size size = iter.next();
                if (size.width < WALLPAPER_MIN_WIDTH || size.height < WALLPAPER_MIN_WIDTH) {
                    continue;
                }
                float aspectRatio = (float)size.width / size.height;
                if (Math.abs(aspectRatio - mWallpaperAspectio)
                        <= Math.abs(minAspectio - mWallpaperAspectio)) {
                    minAspectio = aspectRatio;
                    minSize = size;
                }
            }
            if (mDisplayRotation == 0) { //always width > height
                if (orientation % 180 == 0) {
                    //profile.videoFrameWidth = minSize.height * minSize.height / minSize.width;
                    //profile.videoFrameHeight = minSize.height;
                    profile.videoFrameWidth = minSize.height;
                    profile.videoFrameHeight = minSize.width;
                } else {
                    profile.videoFrameWidth = minSize.width;
                    profile.videoFrameHeight = minSize.height;
                }
            } else {
                if (orientation % 180 == 0) {
                    profile.videoFrameWidth = minSize.width;
                    profile.videoFrameHeight = minSize.height;
                } else {
                    //profile.videoFrameWidth = minSize.height * minSize.height / minSize.width;
                    //profile.videoFrameHeight = minSize.height;
                    profile.videoFrameWidth = minSize.height;
                    profile.videoFrameHeight = minSize.width;
                }
            }
            if (LOG) { //original size
                Log.v(TAG, "limitPreviewByIntent() findSize=" + SettingUtils.buildSize(minSize));
            }
        }
        if (LOG) {
            Log.v(TAG, "limitPreviewByIntent() videoFrameWidth=" + profile.videoFrameWidth
                    + " videoFrameHeight=" + profile.videoFrameHeight + ", mDisplayRotation="
                    + mDisplayRotation + ", orientation=" + mOrientationCompensation);
        }
    }
    
    private void limitSettingsByIntent() {
        //here move some items from setting list
        if (isImageCaptureIntent()) {
            mCameraSettings.removePreferenceFromScreen(mPreferenceGroup, CameraSettings.KEY_CONTINUOUS_NUMBER,
                    SettingChecker.ROW_SETTING_CONTINUOUS);
        } else {
            Intent intent = getIntent();
            if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
                mCameraSettings.removePreferenceFromScreen(mPreferenceGroup, CameraSettings.KEY_VIDEO_QUALITY,
                        SettingChecker.ROW_SETTING_VIDEO_QUALITY);
            }
            if (isVideoWallPaperIntent()) {
                mCameraSettings.removePreferenceFromScreen(mPreferenceGroup, CameraSettings.KEY_VIDEO_QUALITY,
                        SettingChecker.ROW_SETTING_VIDEO_QUALITY);
                mCameraSettings.removePreferenceFromScreen(mPreferenceGroup, CameraSettings.KEY_VIDEO_EFFECT,
                        SettingChecker.ROW_SETTING_LIVE_EFFECT);
            }
        }
    }
    /// @}
    

    /// M: We will restart activity for changed default path. @{
    private boolean mForceFinishing; //forcing finish
    @Override
    protected void onRestart() {
        super.onRestart();
        if (isNonePickIntent() && isMountPointChanged()) {
            finish();
            mForceFinishing = true;
            startActivity(getIntent());
        }
        if (LOG) {
            Log.v(TAG, "onRestart() mForceFinishing=" + mForceFinishing);
        }
    }
    
    private boolean isMountPointChanged() {
        boolean changed = false;
        String mountPoint = Storage.getMountPoint();
        Storage.updateDefaultDirectory();
        if (!mountPoint.equals(Storage.getMountPoint())) {
            changed = true;
        }
        if (LOG) {
            Log.v(TAG, "isMountPointChanged() old=" + mountPoint + ", new="
                    + Storage.getMountPoint() + ", return " + changed);
        }
        return changed;
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOG) {
                Log.v(TAG, "mReceiver.onReceive(" + intent + ")");
            }
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                if (isSameStorage(intent)) {
                    Storage.setStorageReady(false);
                    mCameraActor.onMediaEject();
                }
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                if (isSameStorage(intent)) {
                    String internal = Storage.getInternalVolumePath();
                    if (internal != null) {
                        if(!Storage.updateDirectory(internal)) {
                            mRemainingManager.showHint();
                        }
                    } else {
                        mRemainingManager.clearAvaliableSpace();
                        mRemainingManager.showHint();
                    }
                }
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                if (isSameStorage(intent)) {
                    Storage.setStorageReady(true);
                    mRemainingManager.showHint();
                }
            } else if (action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                if (isSameStorage(intent)) {
                    mRemainingManager.showHint();
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                if (isSameStorage(intent.getData())) {
                    showToast(R.string.wait);
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                if (isSameStorage(intent.getData())) {
                    mRemainingManager.clearAvaliableSpace();
                    mRemainingManager.showHint();
                    mThumbnailManager.forceUpdate();
                }
            }
        }
    };
    
    private boolean isSameStorage(Intent intent) {
        StorageVolume storage = (StorageVolume)intent.getParcelableExtra(
                StorageVolume.EXTRA_STORAGE_VOLUME);
        boolean same = false;
        String mountPoint = null;
        String intentPath = null;
        if (storage != null) {
            mountPoint = Storage.getMountPoint();
            intentPath = storage.getPath();
            if (mountPoint != null && mountPoint.equals(intentPath)) {
                same = true;
            }
        }
        if (LOG) {
            Log.v(TAG, "isSameStorage() mountPoint=" + mountPoint + ", intentPath=" + intentPath
                    + ", return " + same);
        }
        return same;
    }
    
    private boolean isSameStorage(Uri uri) {
        boolean same = false;
        String mountPoint = null;
        String intentPath = null;
        if (uri != null) {
            mountPoint = Storage.getMountPoint();
            intentPath = uri.getPath();
            if (mountPoint != null && mountPoint.equals(intentPath)) {
                same = true;
            }
        }
        if (LOG) {
            Log.v(TAG, "isSameStorage(" + uri + ") mountPoint=" + mountPoint + ", intentPath=" + intentPath
                    + ", return " + same);
        }
        return same;
    }

    private void installIntentFilter() {
        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
    }
    
    private void uninstallIntentFilter() {
        unregisterReceiver(mReceiver);
    }
    /// @}
    
    /// M: for view state logic @{
    private static final int VIEW_STATE_NORMAL = UNKNOWN;
    public static final int VIEW_STATE_CAPTURE = 0;
    public static final int VIEW_STATE_RECORDING = 1;
    public static final int VIEW_STATE_CONTINIUOUS = 2;
    public static final int VIEW_STATE_SETTING = 3;
    public static final int VIEW_STATE_FOCUSING = 4;
    public static final int VIEW_STATE_SAVING = 5;
    public static final int VIEW_STATE_REVIEW = 6;
    private static final int VIEW_STATE_SWITCHING = 7;
    public static final int VIEW_STATE_PICKING = 8;
    public static final int VIEW_STATE_PANORAMA_CAPTURE = 9;
    public static final int VIEW_STATE_LEARNING_VIDEO_EFFECTS = 10;
    private int mCurrentViewState = VIEW_STATE_NORMAL;
    
    //Here just maintain view managers created by Camera.java
    public void setViewState(int state) {
        if (LOG) {
            Log.v(TAG, "setViewState(" + state + ") mCurrentViewState=" + mCurrentViewState
                    + ", mPendingSwitchCameraId=" + mPendingSwitchCameraId);
        }
        //Don't update view when switching camera.
        if (isSwitchingCamera()) {
            return;
        }
        mCurrentViewState = state;
        switch (state) {
        case VIEW_STATE_NORMAL:
            setViewManagerVisible(true);
            setViewManagerEnable(true);
            mShutterManager.setVideoShutterEnabled(true);
            mShutterManager.setEnabled(true);
            mSettingManager.setFileter(true);
            mSettingManager.setAnimationEnabled(true, true);
            if (!mMainHandler.hasMessages(MSG_SHOW_ONSCREEN_INDICATOR)) {
                showIndicator(0);
            } //else show message will restore picker/info/indicator/remaining
            //restoreActorViews();
            break;
        case VIEW_STATE_CAPTURE:
            mSettingManager.collapse(true);
            setViewManagerEnable(false);
            mShutterManager.setEnabled(false);
            break;
        case VIEW_STATE_CONTINIUOUS:
            mSettingManager.collapse(true);
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            mShutterManager.setVideoShutterEnabled(false);
            break;
        case VIEW_STATE_RECORDING:
            mSettingManager.collapse(true);
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            mZoomManager.setEnabled(true);
            break;
        case VIEW_STATE_SETTING:
            mModePicker.hide();
            mThumbnailManager.hide();
            mPickerManager.hide();
            setViewManagerEnable(false);
            mSettingManager.setEnabled(true);
            break;
        case VIEW_STATE_FOCUSING:
            setViewManagerEnable(false);
            break;
        case VIEW_STATE_SAVING:
            mShutterManager.setEnabled(false);
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            break;
        case VIEW_STATE_REVIEW:
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            break;
        case VIEW_STATE_SWITCHING:
            setViewManagerEnable(false);
            mShutterManager.setEnabled(false);
            //Hide indicator for not show indicator before remaining show.
            mIndicatorManager.hide();
            break;
        case VIEW_STATE_PICKING:
            mShutterManager.setEnabled(true);
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            break;
        case VIEW_STATE_PANORAMA_CAPTURE:
            mSettingManager.collapse(true);
            mShutterManager.setEnabled(true);
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            mThumbnailManager.hide();
            break;
        case VIEW_STATE_LEARNING_VIDEO_EFFECTS:
            mSettingManager.collapse(true);
            setViewManagerVisible(false);
            setViewManagerEnable(false);
            mZoomManager.setEnabled(false);
            break;
        default:
            break;
        }
    }
    
    private void setViewManagerVisible(boolean visible) {
        if (visible) {
            if (isNonePickIntent()) {
                mModePicker.show();
                mThumbnailManager.show();
            }
            mShutterManager.show();
            mSettingManager.show();
        } else {
            mModePicker.hide();
            mPickerManager.hide();
            mSettingManager.hide();
            //mThumbnailManager.hide();
        }
    }
    
    private void setViewManagerEnable(boolean enabled) {
        if (isNonePickIntent()) {
            mModePicker.setEnabled(enabled);
            mThumbnailManager.setEnabled(enabled);
        }
        mSettingManager.setEnabled(enabled);
        mPickerManager.setEnabled(enabled);
        mZoomManager.setEnabled(enabled);
    }
    
    public void restoreViewState() {
        if (LOG) {
            Log.v(TAG, "restoreViewState()");
        }
        setViewState(VIEW_STATE_NORMAL);
    }
    
    public boolean isNormalViewState() {
        if (LOG) {
            Log.v(TAG, "isNormalViewState() mCurrentViewState=" + mCurrentViewState);
        }
        return mCurrentViewState == VIEW_STATE_NORMAL;
    }
    
    public int getViewState() {
        return mCurrentViewState;
    }
    
    public void showInfo(final String text) {
        if (LOG) {
            Log.v(TAG, "showInfo(" + text + ")");
        }
        showInfo(text, DELAY_MSG_SHOW_ONSCREEN_VIEW);
    }
    
    public void showInfo(final String text, int showMs) {
        if (LOG) {
            Log.v(TAG, "showInfo(" + text + ", " + showMs + ")");
        }
        mMainHandler.removeMessages(MSG_SHOW_ONSCREEN_INDICATOR);
        doShowInfo(text, showMs);
    }
    
    public void dismissInfo() {
        if (LOG) {
            Log.v(TAG, "dismissInfo()");
        }
        mMainHandler.removeMessages(MSG_SHOW_ONSCREEN_INDICATOR);
        doShowIndicator();
    }

    private void doShowInfo(final String text, final int showMs) {
        if (LOG) {
            Log.v(TAG, "doShowInfo(" + text + ", " + showMs + ")");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIndicatorManager.hide();
                mPickerManager.hide();
                mRemainingManager.hide();
                mInfoManager.showText(text);
                showIndicator(showMs);
            }
        });
    }
    
    private void showRemainingAways() {
        if (LOG) {
            Log.v(TAG, "showRemainingAways()");
        }
        mMainHandler.removeMessages(MSG_SHOW_ONSCREEN_INDICATOR);
        doShowRemaining(true);
    }

    public void showRemaining() {
        if (LOG) {
            Log.v(TAG, "showRemaining()");
        }
        mMainHandler.removeMessages(MSG_SHOW_ONSCREEN_INDICATOR);
        doShowRemaining(false);
    }
    
    private void doShowRemaining(final boolean showAways) {
        if (LOG) {
            Log.v(TAG, "doShowRemaining(" + showAways + ")");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean remainingShown = false;
                if (showAways) {
                    remainingShown = mRemainingManager.showAways();
                } else {
                    remainingShown = mRemainingManager.showIfNeed();
                }
                if (remainingShown) {
                    mIndicatorManager.hide();
                    mInfoManager.hide();
                    if (isNormalViewState()) {
                        mPickerManager.show();
                    }
                    showIndicator(DELAY_MSG_SHOW_ONSCREEN_VIEW);
                }
            }
        });
    }
    
    private void showIndicator(int delayMs) {
        if (LOG) {
            Log.v(TAG, "showIndicator(" + delayMs + ")");
        }
        mMainHandler.removeMessages(MSG_SHOW_ONSCREEN_INDICATOR);
        if (delayMs > 0) {
            mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_ONSCREEN_INDICATOR, delayMs);
        } else {
            mMainHandler.sendEmptyMessage(MSG_SHOW_ONSCREEN_INDICATOR);
        }
    }
    
    private void doShowIndicator() {
        if (LOG) {
            Log.v(TAG, "doShowIndicator()");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInfoManager.hide();
                mRemainingManager.hide();
                if (isNormalViewState()) {
                    mPickerManager.show();
                }
                if (mCurrentViewState != VIEW_STATE_SAVING) {
                     mIndicatorManager.show();
                }
            }
        });
    }
    
    public void showAlertDialog(String title, String msg, String button1Text,
            final Runnable r1, String button2Text, final Runnable r2) {
        mRotateDialog.showAlertDialog(title, msg, button1Text, r1, button2Text, r2);
    }

    public void dismissAlertDialog() {
        mRotateDialog.hide();
    }
    
    public void showProgress(String msg) {
        setViewState(VIEW_STATE_SAVING);
        mRotateProgress.showProgress(msg);
    }
    
    public void dismissProgress() {
        mRotateProgress.hide();
        restoreViewState();
    }
    
    public boolean isShowingProgress() {
        return mRotateProgress.isShowing();
    }
    
    public void showToast(int stringId) {
        String message = getString(stringId);
        showToast(message);
    }
    
    public void showToast(String message) {
        if (LOG) {
            Log.v(TAG, "showToast(" + message + ")");
        }
        if (message != null && isAcceptFloatingInfo()) {
            if (mRotateToast == null) {
                mRotateToast = OnScreenHint.makeText(this, message);
            } else {
                mRotateToast.setText(message);
            }
            mRotateToast.showToast();
        }
    }
    
    public void hideToast() {
        if (LOG) {
            Log.v(TAG, "hideToast()");
        }
        if (mRotateToast != null) {
            mRotateToast.cancel();
        }
    }
    
    //View state may be changed for many reasons, here we use special flag to keep floating info.
    private boolean mAcceptFloatingInfo = true;
    public boolean isAcceptFloatingInfo() {
        boolean accepted = isFullScreen() && mAcceptFloatingInfo;
        if (LOG) {
            Log.v(TAG, "isAcceptFloatingInfo() isFullScreen()=" + isFullScreen() + ", mAcceptFloatingInfo=" +
                    mAcceptFloatingInfo + " return " + accepted);
        }
        return accepted;
    }
    
    public void setAcceptFloatingInfo(boolean accept) {
        if (LOG) {
            Log.v(TAG, "setAcceptFloatingInfo(" + accept + ")");
        }
        mAcceptFloatingInfo = accept;
        if (mAcceptFloatingInfo) {
            showIndicator(0);
        } else {
            mIndicatorManager.hide();
            mRemainingManager.hide();
            mInfoManager.hide();
            mPickerManager.hide();
        }
        //To maintain storage hint floating info
        mRemainingManager.showHint();
    }
    /// @}
    
    /// M: for other things @{
    private void addIdleHandler() {
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                Storage.ensureOSXCompatible();
                return false;
            }
        });
    }
    /// @}
    
    @Override
    public void setSwipingEnabled(boolean enabled) {
        super.setSwipingEnabled(enabled);
        //public this function to actor
    }
    
    public void setResultExAndFinish(int resultCode) {
        setResultEx(resultCode);
        finish();
        clearUserSettings();
    }

    public void setResultExAndFinish(int resultCode, Intent data) {
        setResultEx(resultCode, data);
        finish();
        clearUserSettings();
    }
    
    public boolean isVideoMode() {
        if (LOG) {
            Log.v(TAG, "isVideoMode() getCurrentMode()=" + getCurrentMode());
        }
        return (ModePicker.MODE_VIDEO == getCurrentMode() || ModePicker.MODE_VIDEO_3D == getCurrentMode());
    }
    
    private boolean isVoiceEnabled() {
        //Here we check current value, not preference value.
        boolean enabled = false;
        if (mSettingChecker != null && VoiceManager.VOICE_ON.equals(
                mSettingChecker.getSettingCurrentValue(SettingChecker.ROW_SETTING_VOICE))) {
            enabled = true;
        }
        return enabled;
    }

    @Override
    public void onUserGuideUpdated(final String userGuide) {
        if (LOG) {
            Log.v(TAG, "onUserGuideUpdated(" + userGuide + ")");
        }
        runOnUiThread(new Runnable() {
           @Override
            public void run() {
               //Don't show voice guide when in video mode
               if (!isVideoMode() && isVoiceEnabled()) {
                   showToast(userGuide);
               }
            } 
        });
    }

    @Override
    public void onVoiceValueUpdated(final String value) {
        if (LOG) {
            Log.v(TAG, "onVoiceValueUpdated(" + value + ")");
        }
        if (VoiceManager.VOICE_ON.equals(value)) {
            mVoiceManager.enableVoice();
        }
        //refresh voice control of camera setting.
        runOnUiThread(new Runnable() {
            @Override
             public void run() {
                if (mSettingManager != null) {
                    mSettingManager.refresh();
                }
            }
        });
    }
    
    @Override
    public void onVoiceCommandReceive(final int commandId) {
        if (LOG) {
            Log.v(TAG, "onVoiceCommandReceive(" + commandId + ")");
        }
        runOnUiThread(new Runnable() {
            @Override
             public void run() {
                if (VOICE_COMMAND_CAPTURE == commandId || VOICE_COMMAND_CHEESE == commandId) {
                    if (mShutterManager != null && !isVideoMode() && isVoiceEnabled()) {
                        mShutterManager.performPhotoShutter();
                    }
                }
            }
        });
    }

    private WfdManagerLocal.Listener mWfdListener = new WfdManagerLocal.Listener() {
        @Override
        public void onStateChanged(final boolean enabled) {
            if (LOG) {
                Log.v(TAG, "mWfdListener.onStateChanged(" + enabled + ")");
            }
            runOnUiThread(new Runnable() {
                @Override
                 public void run() {
                    if (mShutterManager != null) {
                        mShutterManager.refresh();
                    }
                }
            });
        }
    };
}
