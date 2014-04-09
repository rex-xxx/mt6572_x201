/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.mock.hardware;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;

import android.hardware.Camera;
import android.hardware.Camera.AFDataCallback;
import android.hardware.Camera.ASDCallback;
import android.hardware.Camera.AUTORAMACallback;
import android.hardware.Camera.AUTORAMAMVCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ContinuousShotDone;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.MAVCallback;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.SmileCallback;
import android.hardware.Camera.ZSDPreviewDone;

import android.media.MediaActionSound;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.mediatek.camera.ICamera;
import com.mediatek.xlog.Xlog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.StringTokenizer;

public class MockCamera implements ICamera {
    private static final String TAG = "MockCamera";

    // These match the enums in frameworks/base/include/camera/Camera.h
    private static final int CAMERA_MSG_ERROR            = 0x001;
    private static final int CAMERA_MSG_SHUTTER          = 0x002;
    private static final int CAMERA_MSG_FOCUS            = 0x004;
    private static final int CAMERA_MSG_ZOOM             = 0x008;
    private static final int CAMERA_MSG_PREVIEW_FRAME    = 0x010;
    private static final int CAMERA_MSG_VIDEO_FRAME      = 0x020;
    private static final int CAMERA_MSG_POSTVIEW_FRAME   = 0x040;
    private static final int CAMERA_MSG_RAW_IMAGE        = 0x080;
    private static final int CAMERA_MSG_COMPRESSED_IMAGE = 0x100;
    private static final int CAMERA_MSG_RAW_IMAGE_NOTIFY = 0x200;
    private static final int CAMERA_MSG_PREVIEW_METADATA = 0x400;
    private static final int CAMERA_MSG_FOCUS_MOVE       = 0x800;

    //!++
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY   = 0x40000000;  //  extended notify message
    private static final int MTK_CAMERA_MSG_EXT_DATA     = 0x80000000;  //  extended data message
    //!--
    //!++
    //
    // Extended notify message (MTK_CAMERA_MSG_EXT_NOTIFY)
    // These match the enums in frameworks/base/include/camera/mtk/MtkCamera.h
    //
    // Smile Detection
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_SMILE_DETECT     = 0x00000001;
    //
    // Auto Scene Detection
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_ASD              = 0x00000002;
    //
    // Multi Angle View
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_MAV              = 0x00000003;
    //
    // Burst Shutter Callback
    //  ext2: 0:not the last one, 1:the last one
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_BURST_SHUTTER    = 0x00000004;
    //
    // End notify for Continuous shot
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_CONTINUOUS_END   = 0x00000006;
    //
    // Last preview frame showed when capture in ZSD mode
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_ZSD_PREVIEW_DONE = 0x00000007;
    // preview frame arrived
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_START_PREVIEW_DONE = 0x00000008;
    //
    //--------------------------------------------------------------------------
    //
    // Extended data message (MTK_CAMERA_MSG_EXT_DATA)
    // These match the enums in frameworks/base/include/camera/mtk/MtkCamera.h
    //
    // Auto Panorama
    //  int[0]: 0:mAUTORAMAMVCallback, 1:mAUTORAMACallback
    //  int[1~]:depends on
    private static final int MTK_CAMERA_MSG_EXT_DATA_AUTORAMA           = 0x00000001;
    //
    // AF Window Results
    private static final int MTK_CAMERA_MSG_EXT_DATA_AF                 = 0x00000002;
    //
    // Burst Shot (EV Shot)
    //  int[0]: the total shut count.
    //  int[1]: count-down shut number; 0: the last one shut.
    private static final int MTK_CAMERA_MSG_EXT_DATA_BURST_SHOT         = 0x00000003;
    //
    //!--

    private int mNativeContext; // accessed by native methods
    private EventHandler mEventHandler;
    private ShutterCallback mShutterCallback;
    private PictureCallback mRawImageCallback;
    private PictureCallback mJpegCallback;
    private PreviewCallback mPreviewCallback;
    private PictureCallback mPostviewCallback;
    private AutoFocusCallback mAutoFocusCallback;
    private AutoFocusMoveCallback mAutoFocusMoveCallback;
    private OnZoomChangeListener mZoomListener;
    private FaceDetectionListener mFaceListener;
    private ErrorCallback mErrorCallback;
    //!++
    private SmileCallback mSmileCallback;
    private MAVCallback mMAVCallback;
    //auto panorama
    private AUTORAMACallback mAUTORAMACallback;
    private AUTORAMAMVCallback mAUTORAMAMVCallback;
    private AutoRama mAutoRama;
    private Mav mMav;
    //ASD
    private ASDCallback mASDCallback;
    //AF Data
    private AFDataCallback mAFDataCallback;
    private boolean mStereo3DModeForCamera = false;
    // ZSD preview done
    private ZSDPreviewDone mPreviewDoneCallback;
    // Continuous shot done
    private ContinuousShotDone mCSDoneCallback;
    //!--    
    private boolean mOneShot;
    private boolean mWithBuffer;
    private boolean mFaceDetectionRunning = false;
    private Object mAutoFocusCallbackLock = new Object();
    private MockCameraSensor mCurrentSensor;
    private HashMap<String, String> mNativeParamters = new HashMap<String, String>();
    private CaptureThread mCapture;
    private AutoSceneDetectThread mSceneDetector;
    private String mCapturePath;
    private String mCaptureMode;
    private MediaActionSound mCameraSound;

    private Context mContext;

    /**
     * Broadcast Action:  A new picture is taken by the camera, and the entry of
     * the picture has been added to the media store.
     * {@link android.content.Intent#getData} is URI of the picture.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";

    /**
     * Broadcast Action:  A new video is recorded by the camera, and the entry
     * of the video has been added to the media store.
     * {@link android.content.Intent#getData} is URI of the video.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";

    /**
     * Hardware face detection. It does not use much CPU.
     */
    private static final int CAMERA_FACE_DETECTION_HW = 0;

    /**
     * Software face detection. It uses some CPU.
     */
    private static final int CAMERA_FACE_DETECTION_SW = 1;

    /**
     * Returns the number of physical cameras available on this device.
     */
    public static int getNumberOfCameras() {
        return 2;
    }

    /**
     * Returns the information about a particular camera.
     * If {@link #getNumberOfCameras()} returns N, the valid id is 0 to N-1.
     */
    public static void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        MockCameraSensor.getCameraInfo(cameraId, cameraInfo);
    }

    public static MockCamera open(int cameraId) {
        return new MockCamera(cameraId);
    }

    public static MockCamera open() {
        int numberOfCameras = getNumberOfCameras();
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                return new MockCamera(i);
            }
        }
        return null;
    }

    MockCamera(int cameraId) {
        mShutterCallback = null;
        mRawImageCallback = null;
        mJpegCallback = null;
        mPreviewCallback = null;
        mPostviewCallback = null;
        mZoomListener = null;

        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        native_mock_setup(new WeakReference<MockCamera>(this), cameraId);
    }

    MockCamera() {
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public Camera getInstance() {
        return null;
    }

    protected void finalize() {
        release();
    }

    private final void native_mock_setup(Object camera_this, int cameraId) {
        mCurrentSensor = new MockCameraSensor(cameraId);
        mCurrentSensor.open();
        mNativeParamters.clear();
        StringTokenizer tokenizer = new StringTokenizer(mCurrentSensor.defaultParameters(), ";");
        while (tokenizer.hasMoreElements()) {
            String kv = tokenizer.nextToken();
            int pos = kv.indexOf('=');
            if (pos == -1) {
                continue;
            }
            String k = kv.substring(0, pos);
            String v = kv.substring(pos + 1);
            mNativeParamters.put(k, v);
        }
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        mCameraSound.load(MediaActionSound.START_VIDEO_RECORDING);
        mCameraSound.load(MediaActionSound.STOP_VIDEO_RECORDING);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Log.i(TAG, "loading camera sound");
        }
        Log.i(TAG, mCurrentSensor.defaultParameters());
    }

    private final void native_mock_release() {
        mCurrentSensor.close();
        if (mCapture != null) {
            mCapture.quit();
            mCapture = null;
        }
        if (mAutoRama != null) {
            mAutoRama.quit();
            mAutoRama = null;
        }
        if (mMav != null) {
            mMav.stopMAV(0);
            mMav = null;
        }
        if (mSceneDetector != null) {
            mSceneDetector.quit();
            mSceneDetector = null;
        }
        if (mCameraSound != null) {
            mCameraSound.release();
            mCameraSound = null;
        }
    }

    public final void release() {
        native_mock_release();
        mFaceDetectionRunning = false;
    }

    public final void unlock() {
    }

    public final void lock() {
    }

    public final void reconnect() throws IOException {
    }

    public final void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        if (holder != null) {
            setPreviewDisplay(holder.getSurface());
        } else {
            setPreviewDisplay((Surface)null);
        }
    }

    private final void setPreviewDisplay(Surface surface) throws IOException {
    }

    public final void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException {
        mSurfaceTexture = surfaceTexture;
    }

    public final void startPreview() {
        Log.i(TAG, "startPreview()");
        Message msg = mEventHandler.obtainMessage(MTK_CAMERA_MSG_EXT_NOTIFY,
                MTK_CAMERA_MSG_EXT_NOTIFY_START_PREVIEW_DONE, 0);
        mEventHandler.sendMessageDelayed(msg, 2000);
    }
    // public native final void startPreview();

    /**
     * Stops capturing and drawing preview frames to the surface, and
     * resets the camera for a future call to {@link #startPreview()}.
     */
    public final void stopPreview() {
        _stopPreview();
        mFaceDetectionRunning = false;

        mShutterCallback = null;
        mRawImageCallback = null;
        mPostviewCallback = null;
        mJpegCallback = null;
        synchronized (mAutoFocusCallbackLock) {
            mAutoFocusCallback = null;
        }
        mAutoFocusMoveCallback = null;
    }

    private final void _stopPreview() {
        mEventHandler.removeMessages(MTK_CAMERA_MSG_EXT_NOTIFY);
    }

    /**
     * Return current preview state.
     *
     * FIXME: Unhide before release
     * @hide
     */
    public final boolean previewEnabled() {
        return true;
    }

    /**
     * <p>Installs a callback to be invoked for every preview frame in addition
     * to displaying them on the screen.  The callback will be repeatedly called
     * for as long as preview is active.  This method can be called at any time,
     * even while preview is live.  Any other preview callbacks are
     * overridden.</p>
     *
     * <p>If you are using the preview data to create video or still images,
     * strongly consider using {@link android.media.MediaActionSound} to
     * properly indicate image capture or recording start/stop to the user.</p>
     *
     * @param cb a callback object that receives a copy of each preview frame,
     *     or null to stop receiving callbacks.
     * @see android.media.MediaActionSound
     */
    public final void setPreviewCallback(PreviewCallback cb) {
        mPreviewCallback = cb;
        mOneShot = false;
        mWithBuffer = false;
        // Always use one-shot mode. We mock camera preview mode by
        // doing one-shot preview continuously.
        setHasPreviewCallback(cb != null, false);
    }

    /**
     * <p>Installs a callback to be invoked for the next preview frame in
     * addition to displaying it on the screen.  After one invocation, the
     * callback is cleared. This method can be called any time, even when
     * preview is live.  Any other preview callbacks are overridden.</p>
     *
     * <p>If you are using the preview data to create video or still images,
     * strongly consider using {@link android.media.MediaActionSound} to
     * properly indicate image capture or recording start/stop to the user.</p>
     *
     * @param cb a callback object that receives a copy of the next preview frame,
     *     or null to stop receiving callbacks.
     * @see android.media.MediaActionSound
     */
    public final void setOneShotPreviewCallback(PreviewCallback cb) {
        mPreviewCallback = cb;
        mOneShot = true;
        mWithBuffer = false;
        setHasPreviewCallback(cb != null, false);
    }

    private final void setHasPreviewCallback(boolean installed, boolean manualBuffer) {
        // TODO ;
    }

    public final void setPreviewCallbackWithBuffer(PreviewCallback cb) {
        mPreviewCallback = cb;
        mOneShot = false;
        mWithBuffer = true;
        setHasPreviewCallback(cb != null, true);
    }

    public final void addCallbackBuffer(byte[] callbackBuffer) {
        _addCallbackBuffer(callbackBuffer, CAMERA_MSG_PREVIEW_FRAME);
    }

    public final void addRawImageCallbackBuffer(byte[] callbackBuffer) {
        addCallbackBuffer(callbackBuffer, CAMERA_MSG_RAW_IMAGE);
    }

    private final void addCallbackBuffer(byte[] callbackBuffer, int msgType) {
        // CAMERA_MSG_VIDEO_FRAME may be allowed in the future.
        if (msgType != CAMERA_MSG_PREVIEW_FRAME &&
            msgType != CAMERA_MSG_RAW_IMAGE) {
            throw new IllegalArgumentException(
                            "Unsupported message type: " + msgType);
        }

        _addCallbackBuffer(callbackBuffer, msgType);
    }

    private final void _addCallbackBuffer(
                                byte[] callbackBuffer, int msgType) {
        // TODO implement Buffer feedback thread
    }

    private class EventHandler extends Handler {
        private MockCamera mCamera;

        public EventHandler(MockCamera c, Looper looper) {
            super(looper);
            mCamera = c;
        }

        @Override
        public void handleMessage(Message msg) {
            //!++
            // For debug 
            Xlog.i(TAG, "handleMessage: " + msg.what);
            //!--
            switch(msg.what) {
            case CAMERA_MSG_SHUTTER:
                if (mShutterCallback != null) {
                    mShutterCallback.onShutter();
                }
                return;

            case CAMERA_MSG_RAW_IMAGE:
                if (mRawImageCallback != null) {
                    mRawImageCallback.onPictureTaken((byte[])msg.obj, null);
                }
                return;

            case CAMERA_MSG_COMPRESSED_IMAGE:
                if (mJpegCallback != null) {
                    mJpegCallback.onPictureTaken((byte[])msg.obj, null);
                    if ("evbracketshot".equals(mCaptureMode)) {
                        mJpegCallback.onPictureTaken((byte[])msg.obj, null);
                        mJpegCallback.onPictureTaken((byte[])msg.obj, null);
                    }
                }
                return;

            case CAMERA_MSG_PREVIEW_FRAME:
                PreviewCallback pCb = mPreviewCallback;
                if (pCb != null) {
                    if (mOneShot) {
                        // Clear the callback variable before the callback
                        // in case the app calls setPreviewCallback from
                        // the callback function
                        mPreviewCallback = null;
                    } else if (!mWithBuffer) {
                        // We're faking the camera preview mode to prevent
                        // the app from being flooded with preview frames.
                        // Set to oneshot mode again.
                        setHasPreviewCallback(true, false);
                    }
                    pCb.onPreviewFrame((byte[])msg.obj, null);
                }
                return;

            case CAMERA_MSG_POSTVIEW_FRAME:
                if (mPostviewCallback != null) {
                    mPostviewCallback.onPictureTaken((byte[])msg.obj, null);
                }
                return;

            case CAMERA_MSG_FOCUS:
                AutoFocusCallback cb = null;
                synchronized (mAutoFocusCallbackLock) {
                    cb = mAutoFocusCallback;
                }
                if (cb != null) {
                    boolean success = msg.arg1 == 0 ? false : true;
                    cb.onAutoFocus(success, null);
                }
                return;

            case CAMERA_MSG_ZOOM:
                if (mZoomListener != null) {
                    mZoomListener.onZoomChange(msg.arg1, msg.arg2 != 0, null);
                }
                return;

            case CAMERA_MSG_PREVIEW_METADATA:
                if (mFaceListener != null) {
                    mFaceListener.onFaceDetection((Face[])msg.obj, null);
                }
                return;

            case CAMERA_MSG_ERROR :
                Log.e(TAG, "Error " + msg.arg1);
                if (mErrorCallback != null) {
                    mErrorCallback.onError(msg.arg1, null);
                }
                return;

            case CAMERA_MSG_FOCUS_MOVE:
                if (mAutoFocusMoveCallback != null) {
                    mAutoFocusMoveCallback.onAutoFocusMoving(msg.arg1 == 0 ? false : true, null);
                }
                return; 
            //!++
            case MTK_CAMERA_MSG_EXT_NOTIFY:
                switch(msg.arg1) {

                case MTK_CAMERA_MSG_EXT_NOTIFY_SMILE_DETECT:
                    if (mSmileCallback != null) {
                        mSmileCallback.onSmile();
                    }
                    break;

                case MTK_CAMERA_MSG_EXT_NOTIFY_ASD:
                    if (mASDCallback != null) {
                        mASDCallback.onDetecte(msg.arg2);
                    }
                    break;

                case MTK_CAMERA_MSG_EXT_NOTIFY_MAV:
                    if (mMAVCallback != null) {
                        mMAVCallback.onFrame();
                    }
                    break;

                case MTK_CAMERA_MSG_EXT_NOTIFY_CONTINUOUS_END:
                    if (mCSDoneCallback != null) {
                        mCSDoneCallback.onConinuousShotDone(msg.arg2);
                    }
                    break;
                case MTK_CAMERA_MSG_EXT_NOTIFY_ZSD_PREVIEW_DONE:
                    if (mPreviewDoneCallback != null) {
                        mPreviewDoneCallback.onPreviewDone();
                    }
                    break;
                case MTK_CAMERA_MSG_EXT_NOTIFY_START_PREVIEW_DONE:
                    Log.i(TAG, "preview frame arrived, sFrameReporter = " + sFrameReporter);
                    try {
                        Class clz = mSurfaceTexture.getClass();
                        Field field = clz.getDeclaredField("mEventHandler");
                        field.setAccessible(true);
                        Handler tempHandler = (Handler) field.get(mSurfaceTexture);
                        Class hCls = tempHandler.getClass();
                        tempHandler.handleMessage(null);
                    } catch (NoSuchFieldException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e2) {
                        e2.printStackTrace();
                    }
                    break;
                default:
                    Xlog.e(TAG, "Unknown MTK-extended notify message type " + msg.arg1);
                    break;
                }
                return;

            case MTK_CAMERA_MSG_EXT_DATA:
                switch(msg.arg1) {

                case MTK_CAMERA_MSG_EXT_DATA_AUTORAMA: {
                        byte[] byteArray = (byte[])msg.obj;

                        Xlog.i(TAG, "MTK_CAMERA_MSG_EXT_DATA_AUTORAMA: byteArray.length = " + byteArray.length);

                        IntBuffer intBuf = ByteBuffer.wrap(byteArray).asIntBuffer();
                        if  (0 == intBuf.get(0)) {
                            if (mAUTORAMAMVCallback != null) {
                                int x   = intBuf.get(1);
                                int y   = intBuf.get(2);
                                int dir = intBuf.get(3);
                                int xy  = ((0x0000FFFF & x) << 16) + (0x0000FFFF & y);
                                Xlog.i(TAG, "call mAUTORAMAMVCallback: " + mAUTORAMACallback
                                        + " dir:" + dir + " x:" + x + " y:" + y + " xy:" + xy);
                                mAUTORAMAMVCallback.onFrame(xy, dir);
                            }
                        } else {
                            Xlog.i(TAG, "call mAUTORAMACallback: " + mAUTORAMACallback);
                            if (mAUTORAMACallback != null) {
                                mAUTORAMACallback.onCapture();
                            }
                        }
                    }
                    break;
                case MTK_CAMERA_MSG_EXT_DATA_AF: {

                        byte[] byteArray = (byte[])msg.obj;

                        Xlog.i(TAG, "MTK_CAMERA_MSG_EXT_DATA_AF: byteArray.length = " + byteArray.length);
/*
                            IntBuffer intBuf = ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder()).asIntBuffer();
                            Log.i(TAG, "intBuf.limit() = " + intBuf.limit());
                            for (int i = 0; i < intBuf.limit(); i++)  
                            {
                                Log.i(TAG, "intBuf " + i + " = " + intBuf.get(i));
                            }
*/
                        if (mAFDataCallback != null) {
                            AFDataCallback afDatacb = mAFDataCallback;
                            afDatacb.onAFData((byte[])msg.obj, null);
                        }
                    }
                    break;
                default:
                    Xlog.e(TAG, "Unknown MTK-extended data message type " + msg.arg1);
                    break;
                }
                return;
            //!--
            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }

    private static void postEventFromNative(Object camera_ref,
                                            int what, int arg1, int arg2, Object obj)
    {
        MockCamera c = (MockCamera)((WeakReference)camera_ref).get();
        if (c == null) {
            return;
        }

        if (c.mEventHandler != null) {
            Message m = c.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            c.mEventHandler.sendMessage(m);
        }
    }

    public final void autoFocus(AutoFocusCallback cb) {
        synchronized (mAutoFocusCallbackLock) {
            mAutoFocusCallback = cb;
        }
        native_mock_autoFocus();
    }

    private final void native_mock_autoFocus() {
        mEventHandler.sendMessageDelayed(
                mEventHandler.obtainMessage(CAMERA_MSG_FOCUS, 1, 0), 300);
    }

    public final void cancelAutoFocus() {
        synchronized (mAutoFocusCallbackLock) {
            mAutoFocusCallback = null;
        }
        native_mock_cancelAutoFocus();
        mEventHandler.removeMessages(CAMERA_MSG_FOCUS);
    }
    private final void native_mock_cancelAutoFocus() {
    }

    public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
        mAutoFocusMoveCallback = cb;
        enableFocusMoveCallback((mAutoFocusMoveCallback != null) ? 1 : 0);
    }

    private void enableFocusMoveCallback(int enable) {
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw,
            PictureCallback jpeg) {
        takePicture(shutter, raw, null, jpeg);
    }

    private final void native_mock_takePicture(int msgType) {
        mEventHandler.removeMessages(MTK_CAMERA_MSG_EXT_NOTIFY);
        if (mCapture == null) {
            mCapture = new CaptureThread(mEventHandler, mContext, mCameraSound);
            mCapture.start();
        }
        
        int num = Integer.parseInt(mNativeParamters.get(KEY_BURST_SHOT_NUM));
        mCapture.setCaptureNum(num);
        mCapture.capture();
/*
        if (mShutterCallback != null) {
            mEventHandler.sendMessageDelayed(
                    mEventHandler.obtainMessage(CAMERA_MSG_SHUTTER, 1, 0), 300);
        }
        if (mRawImageCallback != null) {
            mEventHandler.sendMessageDelayed(
                    mEventHandler.obtainMessage(CAMERA_MSG_RAW_IMAGE, 1, 0), 600);
        }
        if (mJpegCallback != null) {
            mEventHandler.sendMessageDelayed(
                    mEventHandler.obtainMessage(CAMERA_MSG_COMPRESSED_IMAGE, 1, 0), 600);
        }
*/
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw,
            PictureCallback postview, PictureCallback jpeg) {
        mShutterCallback = shutter;
        mRawImageCallback = raw;
        mPostviewCallback = postview;
        mJpegCallback = jpeg;

        // If callback is not set, do not send me callbacks.
        int msgType = 0;
        if (mShutterCallback != null) {
            msgType |= CAMERA_MSG_SHUTTER;
        }
        if (mRawImageCallback != null) {
            msgType |= CAMERA_MSG_RAW_IMAGE;
        }
        if (mPostviewCallback != null) {
            msgType |= CAMERA_MSG_POSTVIEW_FRAME;
        }
        if (mJpegCallback != null) {
            msgType |= CAMERA_MSG_COMPRESSED_IMAGE;
        }

        native_mock_takePicture(msgType);
        mFaceDetectionRunning = false;
    }

    public final void startSmoothZoom(int value) {
    }

    public final void stopSmoothZoom() {
    }

    public final void setDisplayOrientation(int degrees) {
    }

    public final void setZoomChangeListener(OnZoomChangeListener listener) {
        mZoomListener = listener;
    }

    public final void setFaceDetectionListener(FaceDetectionListener listener) {
        mFaceListener = listener;
    }

    public final void startFaceDetection() {
        if (mFaceDetectionRunning) {
            throw new RuntimeException("Face detection is already running");
        }
        _startFaceDetection(CAMERA_FACE_DETECTION_HW);
        mFaceDetectionRunning = true;
    }

    /**
     * Stops the face detection.
     *
     * @see #startFaceDetection()
     */
    public final void stopFaceDetection() {
        _stopFaceDetection();
        mFaceDetectionRunning = false;
    }

    private final void _startFaceDetection(int type) {
    }
    private final void _stopFaceDetection() {
    }

    public static final int CAMERA_ERROR_UNKNOWN = 1;

    public static final int CAMERA_ERROR_SERVER_DIED = 100;

    public static final int CAMERA_ERROR_NO_MEMORY = 1000;

    public static final int CAMERA_ERROR_RESET = 1001;  

    public final void setErrorCallback(ErrorCallback cb) {
        mErrorCallback = cb;
    }

    public final void setSmileCallback(SmileCallback cb) {
        mSmileCallback = cb;
    }

    public void startSDPreview() {
        int delay = (int) (Math.random() * 6000);
        Log.i(TAG, "startSDPreview, smile delay = " + delay);
        mEventHandler.sendMessageDelayed(
                mEventHandler.obtainMessage(
                        MTK_CAMERA_MSG_EXT_NOTIFY, MTK_CAMERA_MSG_EXT_NOTIFY_SMILE_DETECT, 0),
                delay);
    }

    public void cancelSDPreview() {
        mEventHandler.removeMessages(MTK_CAMERA_MSG_EXT_NOTIFY);
    }

    public final void setMAVCallback(MAVCallback cb) {
        mMAVCallback = cb;
    }
    
    public final void setASDCallback(ASDCallback cb) {
        mASDCallback = cb;
    }

    public final void setAFDataCallback(AFDataCallback cb) {
        mAFDataCallback = cb;
    }

    public final void setAUTORAMACallback(AUTORAMACallback cb) {
        mAUTORAMACallback = cb;
    }

    /**
     * @hide
     */    
    public final void setAUTORAMAMVCallback(AUTORAMAMVCallback cb) {
        mAUTORAMAMVCallback = cb;
    }
    
   /**
    * 
    * @hide
    *
    * Start to capture number of images of panorama.
    */
    public final void startAUTORAMA(int num) {
        if (mAutoRama == null) {
            mAutoRama = new AutoRama(mEventHandler, mContext, mCameraSound);
            mAutoRama.start();
        }
        mAutoRama.setCapturePath(mCapturePath);
        mAutoRama.startAutoRama();
    }
    
    /**
    * @hide
    *
    * Stop auto panorama, if isMerge is 1, there will be a callback when merge is done
    */
    public void stopAUTORAMA(int isMerge) {
        mAutoRama.stopAutoRama(isMerge);
    }

    /**
     * @hide
     *
     * Start to capture number of images of MAV, max num is 10
     */
    public final void startMAV(int num) {
        if (mMav != null) {
            mMav.stopMAV(0);
        }
        mMav = new Mav(mEventHandler);
        mMav.setContext(mContext);
        mMav.setCapturePath(mCapturePath);
        mMav.startMAV(num);
        mMav.start();
    }

    /**
     * @hide
     *
     * Stop MAV, if isMerge is 1, there will be a callback when merge is done
     */
    public void stopMAV(int isMerge) {
        mMav.stopMAV(isMerge);
        mMav = null;
    }

    /**
     * @hide
     *
     * Cancel continuous shot
     */
    public void cancelContinuousShot() {
        mCapture.cancelCapture();
    }

    public void setContinuousShotSpeed(int speed) {
        ;
    }

//    public void slowdownContinuousShot() {
//        ;
//    }

    public void setPreviewDoneCallback(ZSDPreviewDone callback) {
        mPreviewDoneCallback = callback;
    }

    public void setCSDoneCallback(ContinuousShotDone callback) {
        mCSDoneCallback = callback;
    }

    private PictureCreator mPictureCreator;
    public interface PictureCreator {
        byte[] onPictureCreate(int type);
    }

    public void setPictureCreator(PictureCreator creator) {
        mPictureCreator = creator;
    }
    //!--

    private final void native_mock_setParameters(Parameters params) {
        String scene = mNativeParamters.get(KEY_SCENE_MODE);
        String newScene = params.getSceneMode();
        try {
            Class clz = params.getClass();
            Field field = clz.getDeclaredField("mMap");
            field.setAccessible(true);
            HashMap<String, String> map = (HashMap<String, String>) field.get(params);
            mNativeParamters.clear();
            mNativeParamters = (HashMap<String, String>)map.clone();
        } catch (NoSuchFieldException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        }
        if (!scene.equals(newScene)) {
            mNativeParamters.put(KEY_EXPOSURE_COMPENSATION,
                    MiniFeatureTable.pickItem(KEY_EXPOSURE_COMPENSATION, newScene));
            mNativeParamters.put(KEY_WHITE_BALANCE,
                    MiniFeatureTable.pickItem(KEY_WHITE_BALANCE, newScene));
            mNativeParamters.put(KEY_ISOSPEED_MODE,
                    MiniFeatureTable.pickItem(KEY_ISOSPEED_MODE, newScene));
            mNativeParamters.put(KEY_EXPOSURE_METER,
                    MiniFeatureTable.pickItem(KEY_EXPOSURE_METER, newScene));
            mNativeParamters.put(KEY_EDGE_MODE,
                    MiniFeatureTable.pickItem(KEY_EDGE_MODE, newScene));
            mNativeParamters.put(KEY_HUE_MODE,
                    MiniFeatureTable.pickItem(KEY_HUE_MODE, newScene));
            mNativeParamters.put(KEY_SATURATION_MODE,
                    MiniFeatureTable.pickItem(KEY_SATURATION_MODE, newScene));
            mNativeParamters.put(KEY_BRIGHTNESS_MODE,
                    MiniFeatureTable.pickItem(KEY_BRIGHTNESS_MODE, newScene));
            mNativeParamters.put(KEY_CONTRAST_MODE,
                    MiniFeatureTable.pickItem(KEY_CONTRAST_MODE, newScene));
        }
        mCapturePath = mNativeParamters.get(KEY_CAPTURE_PATH);
        mCaptureMode = mNativeParamters.get(KEY_CAPTURE_MODE);
        Log.e(TAG,"setParameters:mCaptureMode=" + mCaptureMode);
    }
    private final String native_mock_getParameters() {
        StringBuilder flattened = new StringBuilder();
        for (String k : mNativeParamters.keySet()) {
            flattened.append(k);
            flattened.append("=");
            flattened.append(mNativeParamters.get(k));
            flattened.append(";");
        }
        // chop off the extra semicolon at the end
        flattened.deleteCharAt(flattened.length() - 1);
        return flattened.toString();
    }

    public void setParameters(Parameters params) {
        native_mock_setParameters(params);
    }
  
    //!++
    /**
     * Determine if the target process is restricted to get the all preview size list
     * TRUE means the device is restricted
     * FALSE means the device is not restricted
     *
     * @hide
     */
    public static boolean isRestricted(int pid) {
        boolean ret = false;
        String f = "/proc/" + pid + "/cmdline";

        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        InputStreamReader inReader = new InputStreamReader(in);
        StringBuilder buffer = new StringBuilder();
        char buf[] = new char[1];
        try {
            while (inReader.read(buf) != -1) {
                buffer.append(buf[0]);
            }
            inReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (buffer.toString().contains("com.google.android.apps.unveil")) {
            ret = true;
        }
        return ret;
    }
    /**
     * Return the device's screen size
     * The format is like: "800x480"
     * The default screen size is "800x480"
     * 
     * @hide
     */
    public static String getScreenSize() {
        return android.os.SystemProperties.get(
            "persist.sys.screen.size", "800x480");
    }

    /**
     * @hide
     * 
     * value: stereo3d_mode:true or false
     */
    public void setStereo3DModeForCamera(boolean enable) {
        mStereo3DModeForCamera = enable;
    }

    public Parameters getParameters() {
        Parameters p = getEmptyParameters();
        String s = native_mock_getParameters();
        p.unflatten(s);
        //!++
        Log.i(TAG, "MockCamera getParameters =" + s);
        p.setStereo3DMode(mStereo3DModeForCamera);
        //!--
        return p;
    }

    public static Parameters getEmptyParameters() {
        return android.hardware.Camera.getEmptyParameters();
    }

    /**
     * Image size (width and height dimensions).
     */
    public class Size {
        /**
         * Sets the dimensions for pictures.
         *
         * @param w the photo width (pixels)
         * @param h the photo height (pixels)
         */
        public Size(int w, int h) {
            width = w;
            height = h;
        }
        /**
         * Compares {@code obj} to this size.
         *
         * @param obj the object to compare this size with.
         * @return {@code true} if the width and height of {@code obj} is the
         *         same as those of this size. {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            return width == s.width && height == s.height;
        }
        @Override
        public int hashCode() {
            return width * 32713 + height;
        }
        /** width of the picture */
        public int width;
        /** height of the picture */
        public int height;
    };

    /**
     * <p>The Area class is used for choosing specific metering and focus areas for
     * the camera to use when calculating auto-exposure, auto-white balance, and
     * auto-focus.</p>
     *
     * <p>To find out how many simultaneous areas a given camera supports, use
     * {@link Parameters#getMaxNumMeteringAreas()} and
     * {@link Parameters#getMaxNumFocusAreas()}. If metering or focusing area
     * selection is unsupported, these methods will return 0.</p>
     *
     * <p>Each Area consists of a rectangle specifying its bounds, and a weight
     * that determines its importance. The bounds are relative to the camera's
     * current field of view. The coordinates are mapped so that (-1000, -1000)
     * is always the top-left corner of the current field of view, and (1000,
     * 1000) is always the bottom-right corner of the current field of
     * view. Setting Areas with bounds outside that range is not allowed. Areas
     * with zero or negative width or height are not allowed.</p>
     *
     * <p>The weight must range from 1 to 1000, and represents a weight for
     * every pixel in the area. This means that a large metering area with
     * the same weight as a smaller area will have more effect in the
     * metering result.  Metering areas can overlap and the driver
     * will add the weights in the overlap region.</p>
     *
     * @see Parameters#setFocusAreas(List)
     * @see Parameters#getFocusAreas()
     * @see Parameters#getMaxNumFocusAreas()
     * @see Parameters#setMeteringAreas(List)
     * @see Parameters#getMeteringAreas()
     * @see Parameters#getMaxNumMeteringAreas()
     */
    public static class Area {
        /**
         * Create an area with specified rectangle and weight.
         *
         * @param rect the bounds of the area.
         * @param weight the weight of the area.
         */
        public Area(Rect rect, int weight) {
            this.rect = rect;
            this.weight = weight;
        }
        /**
         * Compares {@code obj} to this area.
         *
         * @param obj the object to compare this area with.
         * @return {@code true} if the rectangle and weight of {@code obj} is
         *         the same as those of this area. {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Area)) {
                return false;
            }
            Area a = (Area) obj;
            if (rect == null) {
                if (a.rect != null) {
                    return false;
                }
            } else {
                if (!rect.equals(a.rect)) {
                    return false;
                }
            }
            return weight == a.weight;
        }

        /**
         * Bounds of the area. (-1000, -1000) represents the top-left of the
         * camera field of view, and (1000, 1000) represents the bottom-right of
         * the field of view. Setting bounds outside that range is not
         * allowed. Bounds with zero or negative width or height are not
         * allowed.
         *
         * @see Parameters#getFocusAreas()
         * @see Parameters#getMeteringAreas()
         */
        public Rect rect;

        /**
         * Weight of the area. The weight must range from 1 to 1000, and
         * represents a weight for every pixel in the area. This means that a
         * large metering area with the same weight as a smaller area will have
         * more effect in the metering result.  Metering areas can overlap and
         * the driver will add the weights in the overlap region.
         *
         * @see Parameters#getFocusAreas()
         * @see Parameters#getMeteringAreas()
         */
        public int weight;
    }

//    public class Parameters {
        private static final String KEY_PREVIEW_SIZE = "preview-size";
        private static final String KEY_PREVIEW_FORMAT = "preview-format";
        private static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
        private static final String KEY_PREVIEW_FPS_RANGE = "preview-fps-range";
        private static final String KEY_PICTURE_SIZE = "picture-size";
        private static final String KEY_PICTURE_FORMAT = "picture-format";
        private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        private static final String KEY_JPEG_QUALITY = "jpeg-quality";
        private static final String KEY_ROTATION = "rotation";
        private static final String KEY_GPS_LATITUDE = "gps-latitude";
        private static final String KEY_GPS_LONGITUDE = "gps-longitude";
        private static final String KEY_GPS_ALTITUDE = "gps-altitude";
        private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
        private static final String KEY_WHITE_BALANCE = "whitebalance";
        private static final String KEY_EFFECT = "effect";
        private static final String KEY_ANTIBANDING = "antibanding";
        private static final String KEY_SCENE_MODE = "scene-mode";
        private static final String KEY_FLASH_MODE = "flash-mode";
        private static final String KEY_FOCUS_MODE = "focus-mode";
        private static final String KEY_FOCUS_AREAS = "focus-areas";
        private static final String KEY_MAX_NUM_FOCUS_AREAS = "max-num-focus-areas";
        private static final String KEY_FOCAL_LENGTH = "focal-length";
        private static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
        private static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
        private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
        private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
        private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
        private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        private static final String KEY_AUTO_EXPOSURE_LOCK = "auto-exposure-lock";
        private static final String KEY_AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK = "auto-whitebalance-lock";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
        private static final String KEY_METERING_AREAS = "metering-areas";
        private static final String KEY_MAX_NUM_METERING_AREAS = "max-num-metering-areas";
        private static final String KEY_ZOOM = "zoom";
        private static final String KEY_MAX_ZOOM = "max-zoom";
        private static final String KEY_ZOOM_RATIOS = "zoom-ratios";
        private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
        private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
        private static final String KEY_FOCUS_DISTANCES = "focus-distances";
        private static final String KEY_VIDEO_SIZE = "video-size";
        private static final String KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO =
                                            "preferred-preview-size-for-video";
        private static final String KEY_MAX_NUM_DETECTED_FACES_HW = "max-num-detected-faces-hw";
        private static final String KEY_MAX_NUM_DETECTED_FACES_SW = "max-num-detected-faces-sw";
        private static final String KEY_RECORDING_HINT = "recording-hint";
        private static final String KEY_VIDEO_SNAPSHOT_SUPPORTED = "video-snapshot-supported";
        private static final String KEY_VIDEO_STABILIZATION = "video-stabilization";
        private static final String KEY_VIDEO_STABILIZATION_SUPPORTED = "video-stabilization-supported";
        //!++
        private static final String KEY_FOCUS_METER = "focus-meter";
        private static final String KEY_ISOSPEED_MODE = "iso-speed";
        private static final String KEY_EXPOSURE = "exposure";
        private static final String KEY_EXPOSURE_METER = "exposure-meter";
        private static final String KEY_FD_MODE = "fd-mode";
        private static final String KEY_EDGE_MODE = "edge";
        private static final String KEY_HUE_MODE = "hue";
        private static final String KEY_SATURATION_MODE = "saturation";
        private static final String KEY_BRIGHTNESS_MODE = "brightness";
        private static final String KEY_CONTRAST_MODE = "contrast";
        private static final String KEY_CAMERA_MODE = "mtk-cam-mode";
        private static final String KEY_FPS_MODE = "fps-mode";
        private static final String KEY_DISP_X = "disp-x";
        private static final String KEY_DISP_Y = "disp-y";
        private static final String KEY_DISP_W = "disp-w";
        private static final String KEY_DISP_H = "disp-h";
        private static final String KEY_DISP_ROTATE = "disp-rotate";
        private static final String KEY_RAW_SAVE_MODE = "rawsave-mode";
        private static final String KEY_RAW_PATH = "rawfname";
        private static final String KEY_FOCUS_DRAW = "af-draw";
        private static final String KEY_FOCUS_ENG_MODE = "afeng-mode";
        private static final String KEY_FOCUS_ENG_STEP = "afeng-pos";
        private static final String KEY_CAPTURE_MODE = "cap-mode";
        private static final String KEY_CAPTURE_PATH = "capfname";
        private static final String KEY_BURST_SHOT_NUM = "burst-num";
        private static final String KEY_MATV_PREVIEW_DELAY = "tv-delay";
        private static final String KEY_PANORAMA_IDX = "pano-idx";
        private static final String KEY_PANORAMA_DIR = "pano-dir";
        private static final String KEY_SENSOR_DEV = "sensor-dev";
        private static final String KEY_EIS_MODE = "eis-mode";
        private static final String KEY_AFLAMP_MODE = "aflamp-mode";
        private static final String KEY_ZSD_MODE = "zsd-mode";
        private static final String KEY_CONTINUOUS_SPEED_MODE = "continuous-shot-speed";
        //
        private static final String ISO_SPEED_ENG = "iso-speed-eng";
        //
        private static final String KEY_ZSD_SUPPORTED = "zsd-supported";
        private static final String SUPPORTED_VALUES_SUFFIX = "-values";

        private static final String TRUE = "true";
        private static final String FALSE = "false";


        private static final String KEY_STEREO3D_PRE = "stereo3d-";
        private boolean mStereo3DMode = false;

        private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
        private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
        private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
        private static final String PIXEL_FORMAT_YUV420P = "yuv420p";
        private static final String PIXEL_FORMAT_RGB565 = "rgb565";
        private static final String PIXEL_FORMAT_JPEG = "jpeg";
        private static final String PIXEL_FORMAT_BAYER_RGGB = "bayer-rggb";

    
    public MediaActionSound getCameraSound() {
        return mCameraSound;
    }

    private static SurfaceTexture.OnFrameAvailableListener sFrameReporter;
    private SurfaceTexture mSurfaceTexture;
    public static void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
        sFrameReporter = listener;
    }
}
