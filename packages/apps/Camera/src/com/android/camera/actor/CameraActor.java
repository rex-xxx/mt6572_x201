package com.android.camera.actor;

import android.content.Intent;
import android.hardware.Camera.ASDCallback;
import android.hardware.Camera.AUTORAMACallback;
import android.hardware.Camera.AUTORAMAMVCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ContinuousShotDone;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.MAVCallback;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.SmileCallback;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View.OnClickListener;

import com.android.camera.Camera;
import com.android.camera.Camera.OnSingleTapUpListener;
import com.android.camera.FocusManager;
import com.android.camera.FocusManager.Listener;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

//just control capture flow, don't set parameters
public abstract class CameraActor {
    private static final String TAG = "CameraActor";
    protected final Camera mContext;
    protected FocusManager mFocusManager;

    public CameraActor(final Camera context) {
        mContext = context;
    }

    public Camera getContext() {
        return mContext;
    }

    // special callback
    // AutoFocusCallback getAutoFocusCallback(){ return null; }
    // AFDataCallback getAFDataCallback(){ return null; }
    public ASDCallback getASDCallback() {
        return null;
    }

    public AutoFocusMoveCallback getAutoFocusMoveCallback() {
        return null;
    }

    public ContinuousShotDone getContinuousShotDone() {
        return null;
    }

    public ErrorCallback getErrorCallback() {
        return null;
    }

    public MAVCallback getMAVCallback() {
        return null;
    }

    public OnZoomChangeListener getOnZoomChangeListener() {
        return null;
    }

    public PreviewCallback getPreviewCallback() {
        return null;
    }

    public SmileCallback getSmileCallback() {
        return null;
    }

    public FaceDetectionListener getFaceDetectionListener() {
        return null;
    }

    // should be checked
    public AUTORAMACallback getAUTORAMACallback() {
        return null;
    }

    public AUTORAMAMVCallback getAUTORAMAMVCallback() {
        return null;
    }

    // user action
    public boolean onUserInteraction() {
        return false;
    }

    public boolean onBackPressed() {
        return false;
    }

    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        return false;
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    }

    public void onMediaEject() {
    }
    
    public void onRestoreSettings() {
    }

    // public void onConfigurationChanged(Configuration newConfig){}
    // public void onCameraSwitched(int newCameraId){}//not recommended

    // shutter button callback
    public OnShutterButtonListener getVideoShutterButtonListener() {
        return null;
    }

    public OnShutterButtonListener getPhotoShutterButtonListener() {
        return null;
    }

    public OnSingleTapUpListener getonSingleTapUpListener() {
        return null;
    }

    public OnClickListener getPlayListener() {
        return null;
    }

    public OnClickListener getRetakeListener() {
        return null;
    }

    public OnClickListener getOkListener() {
        return null;
    }

    public OnClickListener getCancelListener() {
        return null;
    }

    public Listener getFocusManagerListener() {
        return null;
    }

    // camera life cycle
    public void onCameraOpenDone() {
    }// called in opening thread

    public void onCameraOpenFailed() {
    }

    public void onCameraDisabled() {
    }

    public void onCameraParameterReady(boolean startPreview) {
    }// may be called in opening thread
    
    public void stopPreview() {
        
    }
    
    public void onCameraClose() {
    }

    public void onEffectsDeactive() {
    }
    
    public boolean handleFocus() {
        return false;
    }

    public void release() {
    }
    
    public void onOrientationChanged(int orientation) {
    }

    public abstract int getMode();

    protected boolean isFromInternal() {
        final Intent intent = mContext.getIntent();
        final String action = intent.getAction();
        Log.i(TAG, "Check action = " + action);
        // menu helper ?
        return (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action));
    }

    public void onDisplayRotate() {
    }
}
