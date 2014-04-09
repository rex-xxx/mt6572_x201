package com.android.camera.manager;

import android.view.View;
import android.view.View.OnClickListener;

import com.android.camera.Camera;
import com.android.camera.Camera.OnFullScreenChangedListener;
import com.android.camera.Log;
import com.android.camera.ModeChecker;
import com.android.camera.R;
import com.android.camera.ui.ShutterButton;
import com.android.camera.ui.ShutterButton.OnShutterButtonListener;

public class ShutterManager extends ViewManager implements OnFullScreenChangedListener {
    private static final String TAG = "ShutterManager";
    private static final boolean LOG = Log.LOGV;

    public static final int SHUTTER_TYPE_PHOTO_VIDEO = 0;
    public static final int SHUTTER_TYPE_PHOTO = 1;
    public static final int SHUTTER_TYPE_VIDEO = 2;
    public static final int SHUTTER_TYPE_OK_CANCEL = 3;
    public static final int SHUTTER_TYPE_CANCEL = 4;
    public static final int SHUTTER_TYPE_CANCEL_VIDEO = 5;
    
    private int mShutterType = SHUTTER_TYPE_PHOTO_VIDEO;
    private ShutterButton mPhotoShutter;
    private ShutterButton mVideoShutter;
    private View mOkButton;
    private View mCancelButton;
    private OnShutterButtonListener mPhotoListener;
    private OnShutterButtonListener mVideoListener;
    private OnClickListener mOklistener;
    private OnClickListener mCancelListener;
    private boolean mPhotoShutterEnabled = true;
    private boolean mVideoShutterEnabled = true;
    private boolean mCancelButtonEnabled = true;
    private boolean mVideoShutterMasked;
    private boolean mFullScreen = true;
    
    public ShutterManager(Camera context) {
        super(context, VIEW_LAYER_SHUTTER);
        setFileter(false);
        context.addOnFullScreenChangedListener(this);
    }

    @Override
    protected View getView() {
        View view = null;
        int layoutId = R.layout.camera_shutter_photo_video;
        switch(mShutterType) {
        case SHUTTER_TYPE_PHOTO_VIDEO:
            layoutId = R.layout.camera_shutter_photo_video;
            break;
        case SHUTTER_TYPE_PHOTO:
            layoutId = R.layout.camera_shutter_photo;
            break;
        case SHUTTER_TYPE_VIDEO:
            layoutId = R.layout.camera_shutter_video;
            break;
        case SHUTTER_TYPE_OK_CANCEL:
            layoutId = R.layout.camera_shutter_ok_cancel;
            break;
        case SHUTTER_TYPE_CANCEL:
            layoutId = R.layout.camera_shutter_cancel;
            break;
        case SHUTTER_TYPE_CANCEL_VIDEO:
            layoutId = R.layout.camera_shutter_cancel_video;
            break;
        default:
            break;
        }
        view = inflate(layoutId);
        mPhotoShutter = (ShutterButton) view.findViewById(R.id.shutter_button_photo);
        mVideoShutter = (ShutterButton) view.findViewById(R.id.shutter_button_video);
        mOkButton = view.findViewById(R.id.btn_done);
        mCancelButton = view.findViewById(R.id.btn_cancel);
        applyListener();
        return view;
    }
    
    @Override
    protected void onRelease() {
        if (mPhotoShutter != null) {
            mPhotoShutter.setOnShutterButtonListener(null);
        }
        if (mVideoShutter != null) {
            mVideoShutter.setOnShutterButtonListener(null);
        }
        if (mOkButton != null) {
            mOkButton.setOnClickListener(null);
        }
        if (mCancelButton != null) {
            mCancelButton.setOnClickListener(null);
        }
        mPhotoShutter = null;
        mVideoShutter = null;
        mOkButton = null;
        mCancelButton = null;
    }

    private void applyListener() {
        if (mPhotoShutter != null) {
            mPhotoShutter.setOnShutterButtonListener(mPhotoListener);
        }
        if (mVideoShutter != null) {
            mVideoShutter.setOnShutterButtonListener(mVideoListener);
        }
        if (mOkButton != null) {
            mOkButton.setOnClickListener(mOklistener);
        }
        if (mCancelButton != null) {
            mCancelButton.setOnClickListener(mCancelListener);
        }
        if (LOG) {
            Log.v(TAG, "applyListener() mPhotoShutter=(" + mPhotoShutter + ", " + mPhotoListener
                    + "), mVideoShutter=(" + mVideoShutter + ", " + mVideoListener
                    + "), mOkButton=(" + mOkButton + ", " + mOklistener
                    + "), mCancelButton=(" + mCancelButton + ", " + mCancelListener
                    + ")");
        }
    }

    public void setShutterListener(OnShutterButtonListener photoListener,
            OnShutterButtonListener videoListener,
            OnClickListener okListener, OnClickListener cancelListener) {
        mPhotoListener = photoListener;
        mVideoListener = videoListener;
        mOklistener = okListener;
        mCancelListener = cancelListener;
        applyListener();
    }
    
    public void switchShutter(int type) {
        if (LOG) {
            Log.v(TAG, "switchShutterType(" + type + ") mShutterType=" + mShutterType);
        }
        if (mShutterType != type) {
            mShutterType = type;
            reInflate();
        }
    }
    
    public int getShutterType() {
        return mShutterType;
    }
    
    @Override
    public void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() mPhotoShutterEnabled=" + mPhotoShutterEnabled + ", mFullScreen=" + mFullScreen
                    + ", isEnabled()=" + isEnabled());
        }
        if (mVideoShutter != null) {
            boolean visible = ModeChecker.getModePickerVisible(getContext(), getContext().getCameraId(),
                    ModePicker.MODE_VIDEO);
            boolean enabled = mVideoShutterEnabled && isEnabled() && mFullScreen
                    && visible && !getContext().getWfdManagerLocal().isWfdEnabled();
            mVideoShutter.setEnabled(enabled);
            mVideoShutter.setClickable(enabled);
            if (mVideoShutterMasked) {
                mVideoShutter.setImageResource(R.drawable.btn_video_mask);
            } else {
                mVideoShutter.setImageResource(R.drawable.btn_video);
            }
        }
        if (mPhotoShutter != null) {
            boolean enabled = mPhotoShutterEnabled && isEnabled() && mFullScreen;
            mPhotoShutter.setEnabled(enabled);
            mPhotoShutter.setClickable(enabled);
        }
        if (mOkButton != null) {
            boolean enabled = isEnabled() && mFullScreen;
            mOkButton.setEnabled(enabled);
            mOkButton.setClickable(enabled);
        }
        if (mCancelButton != null) {
            boolean enabled = mCancelButtonEnabled && isEnabled() && mFullScreen;
            mCancelButton.setEnabled(enabled);
            mCancelButton.setClickable(enabled);
        }
    }
    
    public ShutterButton getPhotoShutter() {
        return mPhotoShutter;
    }
    
    public ShutterButton getVideoShutter() {
        return mVideoShutter;
    }
    
    public boolean performPhotoShutter() {
        boolean performed = false;
        if (mPhotoShutter != null && mPhotoShutter.isEnabled()) {
            mPhotoShutter.performClick();
            performed = true;
        }
        if (LOG) {
            Log.v(TAG, "performPhotoShutter() mPhotoShutter=" + mPhotoShutter + ", return " + mPhotoShutter);
        }
        return performed;
    }
    
    public void setPhotoShutterEnabled(boolean enabled) {
        if (LOG) {
            Log.v(TAG, "setPhotoShutterEnabled(" + enabled + ")");
        }
        mPhotoShutterEnabled = enabled;
        refresh();
    }
    
    public boolean isPhotoShutterEnabled() {
        if (LOG) {
            Log.v(TAG, "isPhotoShutterEnabled() return " + mPhotoShutterEnabled);
        }
        return mPhotoShutterEnabled;
    }
    
    public void setVideoShutterEnabled(boolean enabled) {
        if (LOG) {
            Log.v(TAG, "setVideoShutterEnabled(" + enabled + ")");
        }
        mVideoShutterEnabled = enabled;
        refresh();
    }
    
    public boolean isVideoShutterEnabled() {
        if (LOG) {
            Log.v(TAG, "isVideoShutterEnabled() return " + mVideoShutterEnabled);
        }
        return mVideoShutterEnabled;
    }
    
    public void setCancelButtonEnabled(boolean enabled) {
        if (LOG) {
            Log.v(TAG, "setCancelButtonEnabled(" + enabled + ")");
        }
        mCancelButtonEnabled = enabled;
        refresh();
    }
    
    public boolean isCancelButtonEnabled() {
        if (LOG) {
            Log.v(TAG, "isCancelButtonEnabled() return " + mCancelButtonEnabled);
        }
        return mCancelButtonEnabled;
    }
    
    public void setVideoShutterMask(boolean mask) {
        if (LOG) {
            Log.v(TAG, "setVideoShutterMask(" + mask + ")");
        }
        mVideoShutterMasked = mask;
        refresh();
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        refresh();
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        mFullScreen = full;
        refresh();
    }
}
