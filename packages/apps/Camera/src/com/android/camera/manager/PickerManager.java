package com.android.camera.manager;

import android.view.View;

import com.android.camera.Camera;
import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;
import com.android.camera.Log;
import com.android.camera.ModeChecker;
import com.android.camera.R;
import com.android.camera.SettingChecker;
import com.android.camera.ui.PickerButton;
import com.android.camera.ui.PickerButton.Listener;

public class PickerManager extends ViewManager implements Listener, Camera.OnPreferenceReadyListener,
        Camera.OnParametersReadyListener {
    private static final String TAG = "PickerManager";
    private static final boolean LOG = Log.LOGV;
    
    public interface PickerListener {
        boolean onCameraPicked(int camerId);
        boolean onFlashPicked(String flashMode);
        boolean onStereoPicked(boolean stereoType);
    }
    
    private PickerButton mFlashPicker;
    private PickerButton mCameraPicker;
    private PickerButton mStereoPicker;
    private PickerListener mListener;
    private boolean mNeedUpdate;
    private boolean mPreferenceReady;
    
    public PickerManager(Camera context) {
        super(context);
        context.addOnPreferenceReadyListener(this);
        context.addOnParametersReadyListener(this);
    }
    
    @Override
    protected View getView() {
        View view = inflate(R.layout.onscreen_pickers);
        mFlashPicker = (PickerButton)view.findViewById(R.id.onscreen_flash_picker);
        mCameraPicker = (PickerButton)view.findViewById(R.id.onscreen_camera_picker);
        mStereoPicker = (PickerButton)view.findViewById(R.id.onscreen_stereo3d_picker);
        applyListeners();
        return view;
    }
    
    private void applyListeners() {
        if (mFlashPicker != null) {
            mFlashPicker.setListener(this);
        }
        if (mCameraPicker != null) {
            mCameraPicker.setListener(this);
        }
        if (mStereoPicker != null) {
            mStereoPicker.setListener(this);
        }
        if (LOG) {
            Log.v(TAG, "applyListeners() mFlashPicker=" + mFlashPicker + ", mCameraPicker=" + mCameraPicker
                    + ", mStereoPicker=" + mStereoPicker);
        }
    }
    
    private void clearListeners() {
        if (mFlashPicker != null) {
            mFlashPicker.setListener(null);
        }
        if (mCameraPicker != null) {
            mCameraPicker.setListener(null);
        }
        if (mStereoPicker != null) {
            mStereoPicker.setListener(null);
        }
    }
    
    public void setListener(PickerListener listener) {
        mListener = listener;
    }
    
    @Override
    public void onPreferenceReady() {
        if (LOG) {
            Log.v(TAG, "onPreferenceReady()");
        }
        mNeedUpdate = true;
        mPreferenceReady = true;
    }

    @Override
    public void onCameraParameterReady() {
        if (LOG) {
            Log.v(TAG, "onCameraParameterReady()");
        }
        if (mFlashPicker != null) {
            mFlashPicker.reloadPreference();
        }
        if (mCameraPicker != null) {
            mCameraPicker.reloadPreference();
        }
        if (mStereoPicker != null) {
            mStereoPicker.reloadPreference();
        }
        refresh();
    }
    
    @Override
    public boolean onPicked(PickerButton button, String key, String newValue) {
        boolean picked = false;
        if (mListener != null) {
            if (mCameraPicker == button) {
                picked = mListener.onCameraPicked(Integer.parseInt(newValue));
            } else if (mFlashPicker == button) {
                picked = mListener.onFlashPicked(newValue);
            } else if (mStereoPicker == button) {
                picked = mListener.onStereoPicked(CameraSettings.STEREO3D_ENABLE.endsWith(newValue) ? true : false);
            }
        }
        if (LOG) {
            Log.v(TAG, "onPicked(" + key + ", " + newValue + ") mListener=" + mListener + " return " + picked);
        }
        return picked;
    }

    public void setCameraId(int cameraId) {
        if (mCameraPicker != null) {
            mCameraPicker.setValue("" + cameraId);
        }
    }
    
    @Override
    public void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() mPreferenceReady=" + mPreferenceReady + ", mNeedUpdate=" + mNeedUpdate);
        }
        if (mPreferenceReady && mNeedUpdate) {
            mFlashPicker.initialize((IconListPreference)getContext().getListPreference(
                    SettingChecker.ROW_SETTING_FLASH));
            mCameraPicker.initialize((IconListPreference)getContext().getListPreference(
                    SettingChecker.ROW_SETTING_DUAL_CAMERA));
            mStereoPicker.initialize((IconListPreference)getContext().getListPreference(
                    SettingChecker.ROW_SETTING_STEREO_MODE));
            mNeedUpdate = false;
        }
        if (mFlashPicker != null) {
            mFlashPicker.updateView();
        }
        if (mCameraPicker != null) {
            boolean visible = ModeChecker.getCameraPickerVisible(getContext());
            mCameraPicker.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (mStereoPicker != null) {
            boolean visible = ModeChecker.getStereoPickerVisibile(getContext());
            mStereoPicker.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    protected void onRelease() {
        super.onRelease();
        mNeedUpdate = true;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mFlashPicker != null) {
            mFlashPicker.setEnabled(enabled);
            mFlashPicker.setClickable(enabled);
        }
        if (mCameraPicker != null) {
            mCameraPicker.setEnabled(enabled);
            mCameraPicker.setClickable(enabled);
        }
        if (mStereoPicker != null) {
            mStereoPicker.setEnabled(enabled);
            mStereoPicker.setClickable(enabled);
        }
    }
}