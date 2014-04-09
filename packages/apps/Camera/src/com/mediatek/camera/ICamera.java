package com.mediatek.camera;

import android.content.Context;

import android.graphics.SurfaceTexture;

import android.hardware.Camera;
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

import java.io.IOException;

public interface ICamera {
    Camera getInstance();
    void addCallbackBuffer(byte[] callbackBuffer);
    void addRawImageCallbackBuffer(byte[] callbackBuffer);
    void autoFocus(AutoFocusCallback cb);
    void cancelAutoFocus();
    void cancelContinuousShot();
    void cancelSDPreview();
    void lock();
    Parameters getParameters();
    void release();
    void reconnect() throws IOException;
    void setASDCallback(ASDCallback cb);
    void setAutoFocusMoveCallback(AutoFocusMoveCallback cb);
    void setAUTORAMACallback(AUTORAMACallback cb);
    void setAUTORAMAMVCallback(AUTORAMAMVCallback cb);
    void setContext(Context context);
    void setCSDoneCallback(ContinuousShotDone callback);
    void setContinuousShotSpeed(int speed);
    void setDisplayOrientation(int degrees);
    void setErrorCallback(ErrorCallback cb);
    void setFaceDetectionListener(FaceDetectionListener listener);
    void setMAVCallback(MAVCallback cb);
    void setParameters(Parameters params);
    void setPreviewCallbackWithBuffer(PreviewCallback cb);
    void setPreviewDoneCallback(ZSDPreviewDone callback);
    void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException;
    void setSmileCallback(SmileCallback cb);
    void setZoomChangeListener(OnZoomChangeListener listener);
//    void slowdownContinuousShot();
    void startAUTORAMA(int num);
    void startFaceDetection();
    void startMAV(int num);
    void startPreview();
    void startSmoothZoom(int value);
    void startSDPreview();
    void stopAUTORAMA(int isMerge);
    void stopFaceDetection();
    void stopMAV(int isMerge);
    void stopPreview();
    void takePicture(ShutterCallback shutter, PictureCallback raw,
                            PictureCallback jpeg);
    void takePicture(ShutterCallback shutter, PictureCallback raw,
                            PictureCallback postview, PictureCallback jpeg);
    void unlock();
}
