package com.android.camera;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import com.android.camera.manager.MMProfileManager;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AppBridge;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.ui.GestureRecognizer.Listener;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.ScreenNail;

/**
 * Superclass of Camera and VideoCamera activities.
 */
public abstract class ActivityBase extends AbstractGalleryActivity
        implements View.OnLayoutChangeListener {

    private static final String TAG = "ActivityBase";
    private static final boolean LOG = Log.LOGV;
    private static final int CAMERA_APP_VIEW_TOGGLE_TIME = 100;  // milliseconds
    
    // The activity is paused. The classes that extend this class should set
    // mPaused the first thing in onResume/onPause.
    protected boolean mPaused;
    private HideCameraAppView mHideCameraAppView;
    private View mSingleTapArea;
    protected GalleryActionBar mActionBar;
    protected MyAppBridge mAppBridge;
    protected CameraScreenNail mCameraScreenNail; // This shows camera preview.
    // The view containing only camera related widgets like control panel,
    // indicator bar, focus indicator and etc.
    protected View mCameraAppView;
    protected boolean mShowCameraAppView = true;
    // mFullScreen used as a flag to indicate the real state of this view
    protected boolean mFullScreen = true;
    
    //just for test
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    
    @Override
    public void onCreate(Bundle icicle) {
        // M: enable screen shot by suggestion of planner
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.disableToggleStatusBar();
        // Set a theme with action bar. It is not specified in manifest because
        // we want to hide it by default. setTheme must happen before
        // setContentView.
        //
        // This must be set before we call super.onCreate(), where the window's
        // background is removed.
        setTheme(R.style.Theme_Gallery);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        //stop gallery from checking external storage
        mShouldCheckStorageState = false;
        super.onCreate(icicle);
    }
    
    public boolean isPanoramaActivity() {
        return false;
    }

    @Override
    protected void onResume() {
        mPaused = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mPaused = true;
        super.onPause();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // getActionBar() should be after setContentView
        mActionBar = new GalleryActionBar(this);
        mActionBar.hide();
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Prevent software keyboard or voice search from showing up.
        if (keyCode == KeyEvent.KEYCODE_SEARCH
                || keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.isLongPress()) { return true; }
        }

        return super.onKeyDown(keyCode, event);
    }

    protected void setResultEx(int resultCode) {
        mResultCodeForTesting = resultCode;
        setResult(resultCode);
    }

    public void setResultEx(int resultCode, Intent data) {
        mResultCodeForTesting = resultCode;
        mResultDataForTesting = data;
        setResult(resultCode, data);
    }

    public int getResultCode() {
        return mResultCodeForTesting;
    }

    public Intent getResultData() {
        return mResultDataForTesting;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void gotoGallery() {
        // Move the next picture with capture animation. "1" means next.
        mAppBridge.switchWithCaptureAnimation(1);
    }

    // Call this after setContentView.
    protected void createCameraScreenNail(boolean getPictures) {
        mCameraAppView = findViewById(R.id.camera_app_root);
        Bundle data = new Bundle();
        //String path = "/local/all/";
        // Intent mode does not show camera roll. Use 0 as a work around for
        // invalid bucket id.
        // TODO: add support of empty media set in gallery.
        //path += (getPictures ? MediaSetUtils.CAMERA_BUCKET_ID : "0");
        String path = getPictures ? Storage.getCameraScreenNailPath() : "/local/all/0";
        data.putString(PhotoPage.KEY_MEDIA_SET_PATH, path);
        data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path);

        // Send an AppBridge to gallery to enable the camera preview.
        mAppBridge = new MyAppBridge();
        data.putParcelable(PhotoPage.KEY_APP_BRIDGE, mAppBridge);
        getStateManager().startState(PhotoPage.class, data);
        mCameraScreenNail = mAppBridge.getCameraScreenNail();
        mAppBridge.setSwipingEnabled(FeatureSwitcher.isSlideEnabled());
    }

    private class HideCameraAppView implements Runnable {
        @Override
        public void run() {
            // We cannot set this as GONE because we want to receive the
            // onLayoutChange() callback even when we are invisible.
            mCameraAppView.setVisibility(View.INVISIBLE);
        }
    }

    protected void updateCameraAppView() {
        if (mShowCameraAppView) {
            mCameraAppView.setVisibility(View.VISIBLE);
            // The "transparent region" is not recomputed when a sibling of
            // SurfaceView changes visibility (unless it involves GONE). It's
            // been broken since 1.0. Call requestLayout to work around it.
            mCameraAppView.requestLayout();
            // withEndAction(null) prevents the pending end action
            // mHideCameraAppView from being executed.
            mCameraAppView.animate()
                    .setDuration(CAMERA_APP_VIEW_TOGGLE_TIME)
                    .withLayer().alpha(1).withEndAction(null);
        } else {
            mCameraAppView.animate()
                    .setDuration(CAMERA_APP_VIEW_TOGGLE_TIME)
                    .withLayer().alpha(0).withEndAction(mHideCameraAppView);
        }
    }

    protected void updateCameraAppViewIfNeed() {
        int visibility = (mCameraAppView == null ? View.VISIBLE : mCameraAppView.getVisibility());
        if (LOG) {
            Log.v(TAG, "updateCameraAppViewIfNeed() mShowCameraAppView=" + mShowCameraAppView
                    + ", visibility=" + visibility);
        }
        if ((mShowCameraAppView && View.VISIBLE != visibility)
                || (!mShowCameraAppView && View.VISIBLE == visibility)) {
            updateCameraAppView();
            onAfterFullScreeChanged(mShowCameraAppView);
        }
    }

    private void onFullScreenChanged(boolean full) {
        if (LOG) {
            Log.v(TAG, "onFullScreenChanged(" + full + ") mShowCameraAppView=" + mShowCameraAppView);
        }
        onFullScreenChanged(full, 0xFFFF);
    }
    
    private void onFullScreenChanged(boolean full, int type) {
        mFullScreen = full;
        //don't change camera app visibility for scale animation
        boolean scaleAnimation = (type & PhotoView.Listener.FULLSCREEN_TYPE_MINIMAL_SCALE) == 0;
        if (LOG) {
            Log.v(TAG, "onFullScreenChanged(" + full + ", " + type + ") mShowCameraAppView=" + mShowCameraAppView
                    + ", mPaused=" + mPaused + ", isFinishing()=" + isFinishing()
                    + ", scaleAnimation=" + scaleAnimation);
        }
        if (mShowCameraAppView == full || scaleAnimation) {
            return;
        }
        mShowCameraAppView = full;
        if (mPaused || isFinishing()) {
            return;
        }
        // Initialize the animation.
        if (mHideCameraAppView == null) {
            mHideCameraAppView = new HideCameraAppView();
            mCameraAppView.animate()
                .setInterpolator(new DecelerateInterpolator());
        }
        updateCameraAppView();
        onAfterFullScreeChanged(full);
    }
    
    protected abstract void onAfterFullScreeChanged(boolean full);
    public boolean isFullScreen() {
//        return mShowCameraAppView;
        return mFullScreen;
    }

    @Override
    public GalleryActionBar getGalleryActionBar() {
        return mActionBar;
    }

    // Preview frame layout has changed.
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (mAppBridge == null) { return; }

        if (left == oldLeft && top == oldTop && right == oldRight
                && bottom == oldBottom) {
            return;
        }

        int width = right - left;
        int height = bottom - top;
        if (LOG) {
            MMProfileManager.triggerProfileLayoutChange("onLayoutChange(left=" + left
                + ", top=" + top + ", right=" + right + ", bottom=" + bottom + ")");
        }
        if (Util.getDisplayRotation(this) % 180 == 0) {
            mCameraScreenNail.setPreviewFrameLayoutSize(width, height);
        } else {
            // Swap the width and height. Camera screen nail draw() is based on
            // natural orientation, not the view system orientation.
            mCameraScreenNail.setPreviewFrameLayoutSize(height, width);
        }

        // Find out the coordinates of the preview frame relative to GL
        // root view.
        View root = (View) getGLRoot();
        int[] rootLocation = new int[2];
        int[] viewLocation = new int[2];
        root.getLocationInWindow(rootLocation);
        v.getLocationInWindow(viewLocation);

        int l = viewLocation[0] - rootLocation[0];
        int t = viewLocation[1] - rootLocation[1];
        int r = l + width;
        int b = t + height;
//        // M: make sure the width is even, because the odd will make 
//        // Camera non-center in Gallery2.
//        if ((l + r) % 2 == 1) {
//            r--;
//        }
        Rect frame = new Rect(l, t, r, b);
        if (LOG) {
            MMProfileManager.triggerProfileLayoutChange(
            "call mAppBridge.setCameraRelativeFrame(" + frame.toString() + ")");
        }
        mAppBridge.setCameraRelativeFrame(frame);
        if (mOldCameraRelativeFrame != null && !frame.equals(mOldCameraRelativeFrame)) {
            // M: relative frame changed, 
            // make sure notifyScreenNailChanged is called
            notifyScreenNailChanged();
            Log.e(TAG, "onLayoutChange: notifyScreenNailChanged called");
        }
        if (LOG) {
            Log.v(TAG, "onLayoutChange() frame=" + frame + ", mOldCameraRelativeFrame=" + mOldCameraRelativeFrame);
        }
        mOldCameraRelativeFrame = frame;
    }
    private Rect mOldCameraRelativeFrame; // avoid changed

    protected void setSingleTapUpListener(View singleTapArea) {
        mSingleTapArea = singleTapArea;
    }

    private boolean onSingleTapUp(int x, int y) {
        // Ignore if listener is null or the camera control is invisible.
        if (mSingleTapArea == null || !mShowCameraAppView) { return false; }

        int[] relativeLocation = Util.getRelativeLocation((View) getGLRoot(),
                mSingleTapArea);
        x -= relativeLocation[0];
        y -= relativeLocation[1];
        if (x >= 0 && x < mSingleTapArea.getWidth() && y >= 0
                && y < mSingleTapArea.getHeight()) {
            onSingleTapUp(mSingleTapArea, x, y);
            return true;
        }
        onSingleTapUpBorder(mCameraAppView, x, y);
        return true;
    }

    protected void onSingleTapUp(View view, int x, int y) {
    }
    protected void onSingleTapUpBorder(View view, int x, int y) {
    }

    protected void setSwipingEnabled(boolean enabled) {
        mAppBridge.setSwipingEnabled(enabled);
    }

    protected void notifyScreenNailChanged() {
        mAppBridge.notifyScreenNailChanged();
    }
    
    public Listener setGestureListener(Listener listener) {
        Listener old = mAppBridge.setGestureListener(listener);
        if (LOG) {
            Log.v(TAG, "setGestureListener(" + listener + ") return " + old);
        }
        return old;
    }

    protected void onPreviewTextureCopied() {
    }

    //////////////////////////////////////////////////////////////////////////
    //  The is the communication interface between the Camera Application and
    //  the Gallery PhotoPage.
    //////////////////////////////////////////////////////////////////////////

    class MyAppBridge extends AppBridge implements CameraScreenNail.Listener {
        private CameraScreenNail mCameraScreenNail;
        private Server mServer;

        @Override
        public ScreenNail attachScreenNail() {
            if (mCameraScreenNail == null) {
                mCameraScreenNail = new CameraScreenNail(this);
            }
            return mCameraScreenNail;
        }

        @Override
        public void detachScreenNail() {
            mCameraScreenNail = null;
        }

        public CameraScreenNail getCameraScreenNail() {
            return mCameraScreenNail;
        }

        // Return true if the tap is consumed.
        @Override
        public boolean onSingleTapUp(int x, int y) {
            return ActivityBase.this.onSingleTapUp(x, y);
        }

        // This is used to notify that the screen nail will be drawn in full screen
        // or not in next draw() call.
        @Override
        public void onFullScreenChanged(boolean full) {
            ActivityBase.this.onFullScreenChanged(full);
        }

        @Override
        public void requestRender() {
            getGLRoot().requestRenderForced();
        }

        @Override
        public void onPreviewTextureCopied() {
            ActivityBase.this.onPreviewTextureCopied();
        }

        @Override
        public void setServer(Server s) {
            mServer = s;
        }

        @Override
        public boolean isPanorama() {
            return ActivityBase.this.isPanoramaActivity();
        }
        
        public boolean isStaticCamera() {
            return false;
        }

        private void setCameraRelativeFrame(Rect frame) {
            if (mServer != null) { mServer.setCameraRelativeFrame(frame); }
        }

        private void switchWithCaptureAnimation(int offset) {
            if (mServer != null) {
                if (mServer.switchWithCaptureAnimation(offset)) {
                    if (LOG) {
                        Log.v(TAG, "switchWithCaptureAnimation mFullScreen=" + mFullScreen);
                    }
                    mFullScreen = false; 
                }
            }
        }

        private void setSwipingEnabled(boolean enabled) {
            if (mServer != null) { mServer.setSwipingEnabled(enabled); }
        }

        private void notifyScreenNailChanged() {
            if (mServer != null) {
                MMProfileManager.triggerNotifyServerSelfChange();
                mServer.notifyScreenNailChanged();
            }
        }
        
        private Listener setGestureListener(Listener listener) {
            if (mServer != null) {
                return mServer.setGestureListener(listener);
            }
            return null;
        }
        
        @Override
        public void onFullScreenChanged(boolean full, int type) {
            ActivityBase.this.onFullScreenChanged(full, type);
        }
    }
}
