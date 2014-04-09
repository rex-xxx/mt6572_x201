/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera.ASDCallback;
import android.hardware.Camera.AUTORAMACallback;
import android.hardware.Camera.AUTORAMAMVCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ContinuousShotDone;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.MAVCallback;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.SmileCallback;
import android.hardware.Camera.ZSDPreviewDone;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import static com.android.camera.Util.assertError;
import com.android.camera.manager.MMProfileManager;
import com.mediatek.camera.FrameworksClassFactory;
import com.mediatek.camera.ICamera;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static final boolean LOG = Log.LOGV;
    private static CameraManager sCameraManager = new CameraManager();

    // Thread progress signals
    private ConditionVariable mSig = new ConditionVariable();

    private Parameters mParameters;
    private IOException mReconnectException;

    private static final int RELEASE = 1;
    private static final int RECONNECT = 2;
    private static final int UNLOCK = 3;
    private static final int LOCK = 4;
    private static final int SET_PREVIEW_TEXTURE_ASYNC = 5;
    private static final int START_PREVIEW_ASYNC = 6;
    private static final int STOP_PREVIEW = 7;
    private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 8;
    private static final int ADD_CALLBACK_BUFFER = 9;
    private static final int AUTO_FOCUS = 10;
    private static final int CANCEL_AUTO_FOCUS = 11;
    private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 12;
    private static final int SET_DISPLAY_ORIENTATION = 13;
    private static final int SET_ZOOM_CHANGE_LISTENER = 14;
    private static final int SET_FACE_DETECTION_LISTENER = 15;
    private static final int START_FACE_DETECTION = 16;
    private static final int STOP_FACE_DETECTION = 17;
    private static final int SET_ERROR_CALLBACK = 18;
    private static final int SET_PARAMETERS = 19;
    private static final int GET_PARAMETERS = 20;
    private static final int SET_PARAMETERS_ASYNC = 21;
    private static final int WAIT_FOR_IDLE = 22;
    ///M: JB migration start @{
    private static final int START_SMOOTH_ZOOM = 100;
    private static final int SET_AUTORAMA_CALLBACK = 101;
    private static final int SET_AUTORAMA_MV_CALLBACK = 102;
    private static final int START_AUTORAMA = 103;
    private static final int STOP_AUTORAMA = 104;
    private static final int SET_MAV_CALLBACK = 105;
    private static final int START_MAV = 106;
    private static final int STOP_MAV = 107;
    private static final int SET_ASD_CALLBACK = 108;
    private static final int SET_SMILE_CALLBACK = 109;
    private static final int START_SD_PREVIEW = 110;
    private static final int CANCEL_SD_PREVIEW = 111;
    private static final int CANCEL_CONTINUOUS_SHOT = 112;
    private static final int SET_CONTINUOUS_SHOT_SPEED = 113;
    private static final int SET_PREVIEW_DONE_CALLBACK = 114;
    private static final int SET_CSHOT_DONE_CALLBACK = 115;
    private static final int ADD_RAW_IMAGE_CALLBACK_BUFFER = 116;
    /// @}

    private Handler mCameraHandler;
    private CameraProxy mCameraProxy;
    private ICamera mCamera;

    public static CameraManager instance() {
        return sCameraManager;
    }

    private CameraManager() {
        HandlerThread ht = new HandlerThread("Camera Handler Thread");
        ht.start();
        mCameraHandler = new CameraHandler(ht.getLooper());
    }

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            if (LOG) {
                Log.v(TAG, "handleMessage(" + msg + ")");
            }
            try {
                switch (msg.what) {
                    case RELEASE:
                        MMProfileManager.startProfileCameraRelease();
                        mCamera.release();
                        MMProfileManager.stopProfileCameraRelease();
                        mCamera = null;
                        mCameraProxy = null;
                        break;

                    case RECONNECT:
                        mReconnectException = null;
                        try {
                            mCamera.reconnect();
                        } catch (IOException ex) {
                            mReconnectException = ex;
                        }
                        break;

                    case UNLOCK:
                        mCamera.unlock();
                        break;

                    case LOCK:
                        mCamera.lock();
                        break;

                    case SET_PREVIEW_TEXTURE_ASYNC:
                        try {
                            mCamera.setPreviewTexture((SurfaceTexture) msg.obj);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return;  // no need to call mSig.open()

                    case START_PREVIEW_ASYNC:
                        MMProfileManager.startProfileStartPreview();
                        mCamera.startPreview();
                        MMProfileManager.stopProfileStartPreview();
                        return;  // no need to call mSig.open()

                    case STOP_PREVIEW:
                        MMProfileManager.startProfileStopPreview();
                        mCamera.stopPreview();
                        MMProfileManager.stopProfileStopPreview();
                        break;

                    case SET_PREVIEW_CALLBACK_WITH_BUFFER:
                        mCamera.setPreviewCallbackWithBuffer(
                            (PreviewCallback) msg.obj);
                        break;

                    case ADD_CALLBACK_BUFFER:
                        mCamera.addCallbackBuffer((byte[]) msg.obj);
                        break;

                    case AUTO_FOCUS:
                        mCamera.autoFocus((AutoFocusCallback) msg.obj);
                        break;

                    case CANCEL_AUTO_FOCUS:
                        mCamera.cancelAutoFocus();
                        break;

                    case SET_AUTO_FOCUS_MOVE_CALLBACK:
                        mCamera.setAutoFocusMoveCallback(
                            (AutoFocusMoveCallback) msg.obj);
                        break;

                    case SET_DISPLAY_ORIENTATION:
                        mCamera.setDisplayOrientation(msg.arg1);
                        break;

                    case SET_ZOOM_CHANGE_LISTENER:
                        mCamera.setZoomChangeListener(
                            (OnZoomChangeListener) msg.obj);
                        break;

                    case SET_FACE_DETECTION_LISTENER:
                        mCamera.setFaceDetectionListener(
                            (FaceDetectionListener) msg.obj);
                        break;

                    case START_FACE_DETECTION:
                        mCamera.startFaceDetection();
                        break;

                    case STOP_FACE_DETECTION:
                        mCamera.stopFaceDetection();
                        break;

                    case SET_ERROR_CALLBACK:
                        mCamera.setErrorCallback((ErrorCallback) msg.obj);
                        break;

                    case SET_PARAMETERS:
                        MMProfileManager.startProfileSetParameters();
                        mCamera.setParameters((Parameters) msg.obj);
                        MMProfileManager.stopProfileSetParameters();
                        break;

                    case GET_PARAMETERS:
                        MMProfileManager.startProfileGetParameters();
                        mParameters = mCamera.getParameters();
                        MMProfileManager.stopProfileGetParameters();
                        break;

                    case SET_PARAMETERS_ASYNC:
                        MMProfileManager.startProfileSetParameters();
                        mCamera.setParameters((Parameters) msg.obj);
                        MMProfileManager.stopProfileSetParameters();
                        return;  // no need to call mSig.open()

                    case WAIT_FOR_IDLE:
                        // do nothing
                        break;
                    case START_SMOOTH_ZOOM:
                        mCamera.startSmoothZoom(msg.arg1);
                        break;
                    case SET_AUTORAMA_CALLBACK:
                        mCamera.setAUTORAMACallback((AUTORAMACallback)msg.obj);
                        break;
                    case SET_AUTORAMA_MV_CALLBACK:
                        mCamera.setAUTORAMAMVCallback((AUTORAMAMVCallback)msg.obj);
                        break;
                    case START_AUTORAMA:
                        mCamera.startAUTORAMA(msg.arg1);
                        break;
                    case STOP_AUTORAMA:
                        mCamera.stopAUTORAMA(msg.arg1);
                        break;
                    case SET_MAV_CALLBACK:
                        mCamera.setMAVCallback((MAVCallback)msg.obj);
                        break;
                    case START_MAV:
                        mCamera.startMAV(msg.arg1);
                        break;
                    case STOP_MAV:
                        mCamera.stopMAV(msg.arg1);
                        break;
                    case SET_ASD_CALLBACK:
                        mCamera.setASDCallback((ASDCallback)msg.obj);
                        break;
                    case SET_SMILE_CALLBACK:
                        mCamera.setSmileCallback((SmileCallback)msg.obj);
                        break;
                    case START_SD_PREVIEW:
                        mCamera.startSDPreview();
                        break;
                    case CANCEL_SD_PREVIEW:
                        mCamera.cancelSDPreview();
                        break;
                    case CANCEL_CONTINUOUS_SHOT:
                        mCamera.cancelContinuousShot();
                        break;
                    case SET_CONTINUOUS_SHOT_SPEED:
                        mCamera.setContinuousShotSpeed(msg.arg1);
                        break;
                    case SET_PREVIEW_DONE_CALLBACK:
                        mCamera.setPreviewDoneCallback((ZSDPreviewDone)msg.obj);
                        break;
                    case SET_CSHOT_DONE_CALLBACK:
                        mCamera.setCSDoneCallback((ContinuousShotDone)msg.obj);
                        break;
                    case ADD_RAW_IMAGE_CALLBACK_BUFFER:
                        mCamera.addRawImageCallbackBuffer((byte[]) msg.obj);
                        break;
                    default:
                        break;
                }
            } catch (RuntimeException e) {
                if (msg.what != RELEASE && mCamera != null) {
                    try {
                        mCamera.release();
                    } catch (Exception ex) {
                        Log.e(TAG, "Fail to release the camera.");
                    }
                    mCamera = null;
                    mCameraProxy = null;
                }
                throw e;
            }
            openSig();
        }
    }

    // Open camera synchronously. This method is invoked in the context of a
    // background thread.
    CameraProxy cameraOpen(int cameraId) {
        // Cannot open camera in mCameraHandler, otherwise all camera events
        // will be routed to mCameraHandler looper, which in turn will call
        // event handler like Camera.onFaceDetection, which in turn will modify
        // UI and cause exception like this:
        // CalledFromWrongThreadException: Only the original thread that created
        // a view hierarchy can touch its views.
        MMProfileManager.startProfileCameraOpen();
        mCamera = FrameworksClassFactory.openCamera(cameraId);
        MMProfileManager.stopProfileCameraOpen();
        if (mCamera != null) {
            mCameraProxy = new CameraProxy();
            return mCameraProxy;
        } else {
            return null;
        }
    }

    public class CameraProxy {
        private CameraProxy() {
            assertError(mCamera != null);
        }

        public ICamera getCamera() {
            return mCamera;
        }

        public void release() {
            closeSig();
            mCameraHandler.sendEmptyMessage(RELEASE);
            blockSig();
        }

        public void reconnect() throws IOException {
            closeSig();
            mCameraHandler.sendEmptyMessage(RECONNECT);
            blockSig();
            if (mReconnectException != null) {
                throw mReconnectException;
            }
        }

        public void unlock() {
            closeSig();
            mCameraHandler.sendEmptyMessage(UNLOCK);
            blockSig();
        }

        public void lock() {
            closeSig();
            mCameraHandler.sendEmptyMessage(LOCK);
            blockSig();
        }

        public void setPreviewTextureAsync(final SurfaceTexture surfaceTexture) {
            mCameraHandler.obtainMessage(SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture).sendToTarget();
        }

        public void startPreviewAsync() {
            mCameraHandler.sendEmptyMessage(START_PREVIEW_ASYNC);
        }

        public void stopPreview() {
            closeSig();
            mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
            blockSig();
        }

        public void setPreviewCallbackWithBuffer(final PreviewCallback cb) {
            closeSig();
            mCameraHandler.obtainMessage(SET_PREVIEW_CALLBACK_WITH_BUFFER, cb).sendToTarget();
            blockSig();
        }

        public void addCallbackBuffer(byte[] callbackBuffer) {
            closeSig();
            mCameraHandler.obtainMessage(ADD_CALLBACK_BUFFER, callbackBuffer).sendToTarget();
            blockSig();
        }

        public void addRawImageCallbackBuffer(byte[] callbackBuffer) {
            closeSig();
            mCameraHandler.obtainMessage(ADD_RAW_IMAGE_CALLBACK_BUFFER, callbackBuffer).sendToTarget();
            blockSig();
        }

        public void autoFocus(AutoFocusCallback cb) {
            closeSig();
            mCameraHandler.obtainMessage(AUTO_FOCUS, cb).sendToTarget();
            blockSig();
        }

        public void cancelAutoFocus() {
            closeSig();
            mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);
            blockSig();
        }

        public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
            closeSig();
            mCameraHandler.obtainMessage(SET_AUTO_FOCUS_MOVE_CALLBACK, cb).sendToTarget();
            blockSig();
        }

        public void takePicture(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback postview, final PictureCallback jpeg) {
            closeSig();
            // Too many parameters, so use post for simplicity
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCamera.takePicture(shutter, raw, postview, jpeg);
                    openSig();
                }
            });
            blockSig();
        }

        public void setDisplayOrientation(int degrees) {
            closeSig();
            mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0)
                    .sendToTarget();
            blockSig();
        }

        public void setZoomChangeListener(OnZoomChangeListener listener) {
            closeSig();
            mCameraHandler.obtainMessage(SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
            blockSig();
        }

        public void setFaceDetectionListener(FaceDetectionListener listener) {
            closeSig();
            mCameraHandler.obtainMessage(SET_FACE_DETECTION_LISTENER, listener).sendToTarget();
            blockSig();
        }

        public void startFaceDetection() {
            closeSig();
            mCameraHandler.sendEmptyMessage(START_FACE_DETECTION);
            blockSig();
        }

        public void stopFaceDetection() {
            closeSig();
            mCameraHandler.sendEmptyMessage(STOP_FACE_DETECTION);
            blockSig();
        }

        public void setErrorCallback(ErrorCallback cb) {
            closeSig();
            mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
            blockSig();
        }

        public void setParameters(Parameters params) {
            closeSig();
            mCameraHandler.obtainMessage(SET_PARAMETERS, params).sendToTarget();
            blockSig();
        }
        
        public void setParametersAsync(Parameters params) {
            mCameraHandler.removeMessages(SET_PARAMETERS_ASYNC);
            mCameraHandler.obtainMessage(SET_PARAMETERS_ASYNC, params).sendToTarget();
        }
        
        public void setParametersAsync(final Camera context, final int zoomValue) {
            // Too many parameters, so use post for simplicity
            synchronized (CameraProxy.this) {
                if (mAsyncRunnable != null) {
                    mCameraHandler.removeCallbacks(mAsyncRunnable);
                }
                mAsyncRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (LOG) {
                            Log.v(TAG, "mAsyncRunnable.run(" + zoomValue + ") this="
                                    + mAsyncRunnable + ", mCamera=" + mCamera);
                        }
                        if (mCamera != null && mCameraProxy != null) {
                            if (!mCameraProxy.tryLockParametersRun(new Runnable() {
                                @Override
                                public void run() {
                                    MMProfileManager.startProfileSetParameters();
                                    //Here we use zoom value instead of parameters for that:
                                    //parameters may be different from current parameters.
                                    Parameters params = context.getParameters();
                                    if (mCamera != null && params != null) {
                                        params.setZoom(zoomValue);
                                        mCamera.setParameters(params);
                                    }
                                    MMProfileManager.stopProfileSetParameters();
                                 }
                             })) {
                                //Second async may changed the runnable,
                                //here we sync the new runnable and post it again.
                                synchronized (CameraProxy.this) {
                                    if (mAsyncRunnable != null) {
                                        mCameraHandler.removeCallbacks(mAsyncRunnable);
                                    }
                                    mCameraHandler.post(mAsyncRunnable);
                                    if (LOG) {
                                        Log.v(TAG, "mAsyncRunnable.post " + mAsyncRunnable);
                                    }
                                }
                            }
                        }
                    }
                };
                mCameraHandler.post(mAsyncRunnable);
                if (LOG) {
                    Log.v(TAG, "setParametersAsync(" + zoomValue + ") mAsyncRunnable=" + mAsyncRunnable);
                }
            }
        }

        public Parameters getParameters() {
            closeSig();
            mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
            blockSig();
            return mParameters;
        }

        public void waitForIdle() {
            closeSig();
            mCameraHandler.sendEmptyMessage(WAIT_FOR_IDLE);
            blockSig();
        }

        ///M: JB migration start @{
        public void startSmoothZoom(int zoomValue) {
            closeSig();
            mCameraHandler.obtainMessage(START_SMOOTH_ZOOM, zoomValue, 0).sendToTarget();
            blockSig();
        }

        public void setAUTORAMACallback(AUTORAMACallback autoramaCallback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_AUTORAMA_CALLBACK, autoramaCallback).sendToTarget();
            blockSig();
        }

        public void setAUTORAMAMVCallback(AUTORAMAMVCallback autoramamvCallback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_AUTORAMA_MV_CALLBACK, autoramamvCallback).sendToTarget();
            blockSig();
        }

        public void startAUTORAMA(int num) {
            closeSig();
            mCameraHandler.obtainMessage(START_AUTORAMA, num, 0).sendToTarget();
            blockSig();
        }

        public void stopAUTORAMA(int isMerge) {
            closeSig();
            mCameraHandler.obtainMessage(STOP_AUTORAMA, isMerge, 0).sendToTarget();
            blockSig();
        }

        public void setMAVCallback(MAVCallback mavCallback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_MAV_CALLBACK, mavCallback).sendToTarget();
            blockSig();
        }

        public void startMAV(int num) {
            closeSig();
            mCameraHandler.obtainMessage(START_MAV, num, 0).sendToTarget();
            blockSig();
        }

        public void stopMAV(int isMerge) {
            closeSig();
            mCameraHandler.obtainMessage(STOP_MAV, isMerge, 0).sendToTarget();
            blockSig();
        }

        public void setASDCallback(ASDCallback asdCallback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_ASD_CALLBACK, asdCallback).sendToTarget();
            blockSig();
        }

        public void setSmileCallback(SmileCallback smileCallback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_SMILE_CALLBACK, smileCallback).sendToTarget();
            blockSig();
        }

        public void startSDPreview() {
            closeSig();
            mCameraHandler.sendEmptyMessage(START_SD_PREVIEW);
            blockSig();
        }

        public void cancelSDPreview() {
            closeSig();
            mCameraHandler.sendEmptyMessage(CANCEL_SD_PREVIEW);
            blockSig();
        }

        public void cancelContinuousShot() {
            closeSig();
            mCameraHandler.sendEmptyMessage(CANCEL_CONTINUOUS_SHOT);
            blockSig();
        }

        public void setContinuousShotSpeed(int speed) {
            closeSig();
            mCameraHandler.obtainMessage(SET_CONTINUOUS_SHOT_SPEED, speed, 0).sendToTarget();
            blockSig();
        }

        public void setPreviewDoneCallback(ZSDPreviewDone callback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_PREVIEW_DONE_CALLBACK, callback).sendToTarget();
            blockSig();
        }

        public void setCShotDoneCallback(ContinuousShotDone callback) {
            closeSig();
            mCameraHandler.obtainMessage(SET_CSHOT_DONE_CALLBACK, callback).sendToTarget();
            blockSig();
        }
    
        ///M: lock parameter for ConcurrentModificationException. @{ 
        private Runnable mAsyncRunnable;
        private static final int ENGINE_ACCESS_MAX_TIMEOUT_MS = 500;
        private ReentrantLock mLock = new ReentrantLock();
        private void lockParameters() throws InterruptedException {
            if (LOG) {
                Log.v(TAG, "lockParameters: grabbing lock", new Throwable());
            }
            mLock.lock();
            if (LOG) {
                Log.v(TAG, "lockParameters: grabbed lock");
            }
        }

        private void unlockParameters() {
            if (LOG) {
                Log.v(TAG, "lockParameters: releasing lock");
            }
            mLock.unlock();
        }
        
        private boolean tryLockParameters(long timeoutMs) throws InterruptedException {
            if (LOG) {
                Log.d(TAG, "try lock: grabbing lock with timeout " + timeoutMs, new Throwable());
            }
            boolean acquireSem = mLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (LOG) {
                Log.d(TAG, "try lock: grabbed lock status " + acquireSem);
            }

            return acquireSem;
        }
        
        public void lockParametersRun(Runnable runnable) {
            boolean lockedParameters = false;
            try {
                lockParameters();
                lockedParameters = true;
                runnable.run();
            } catch (InterruptedException  ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                if (lockedParameters) {
                    unlockParameters();
                }
            }
        }
        
        public boolean tryLockParametersRun(Runnable runnable) {
            boolean lockedParameters = false;
            try {
                lockedParameters = tryLockParameters(ENGINE_ACCESS_MAX_TIMEOUT_MS);
                if (lockedParameters) {
                    runnable.run();
                }
            } catch (InterruptedException  ex) {
                Log.e(TAG, "tryLockParametersRun() not successfull.", ex);
            } finally {
                if (lockedParameters) {
                    unlockParameters();
                }
            }
            if (LOG) {
                Log.d(TAG, "tryLockParametersRun(" + runnable + ") return " + lockedParameters);
            }
            return lockedParameters;
        }
        /// @}
    }
    
    /// M: ConditionVariable may open multi thread, so here we use semphore to lock camera proxy.
    private Semaphore mSemphore = new Semaphore(1);
    private void closeSig() {
        if (LOG) {
            Log.v(TAG, "sginal: acquiring semphore");//, new Throwable());
        }
        try {
            mSemphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mSig.close();
        if (LOG) {
            Log.v(TAG, "sginal: acquired semphore");
        }
    }
    
    private void blockSig() {
        if (LOG) {
            Log.v(TAG, "sginal: blocking");
        }
        mSig.block();
        if (LOG) {
            Log.v(TAG, "sginal: released blocking");
        }
    }
    
    private void openSig() {
        if (LOG) {
            Log.v(TAG, "sginal: releasing semphore");
        }
        mSig.open();
        mSemphore.release();
        if (LOG) {
            Log.v(TAG, "sginal: released semphore");
        }
    }
    /// @}
}
