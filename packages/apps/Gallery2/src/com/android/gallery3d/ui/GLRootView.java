/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import com.android.gallery3d.R;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MotionEventHelper;
import com.android.gallery3d.util.Profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.view.WindowManager;

import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekMMProfile;
import com.mediatek.gallery3d.util.MtkLog;

// The root component of all <code>GLView</code>s. The rendering is done in GL
// thread while the event handling is done in the main thread.  To synchronize
// the two threads, the entry points of this package need to synchronize on the
// <code>GLRootView</code> instance unless it can be proved that the rendering
// thread won't access the same thing as the method. The entry points include:
// (1) The public methods of HeadUpDisplay
// (2) The public methods of CameraHeadUpDisplay
// (3) The overridden methods in GLRootView.
public class GLRootView extends GLSurfaceView
        implements GLSurfaceView.Renderer, GLRoot {
    private static final String TAG = "Gallery2/GLRootView";

    private static final boolean DEBUG_FPS = false;
    private int mFrameCount = 0;
    private long mFrameCountingStart = 0;

    private static final boolean DEBUG_INVALIDATE = false;
    private int mInvalidateColor = 0;

    private static final boolean DEBUG_DRAWING_STAT = false;

    private static final boolean DEBUG_PROFILE = false;
    private static final boolean DEBUG_PROFILE_SLOW_ONLY = false;
    private final boolean DEBUG_PROFILE_PERFORMANCE = MtkLog.DBG_PERFORMANCE;

    private static final int FLAG_INITIALIZED = 1;
    private static final int FLAG_NEED_LAYOUT = 2;

    private GL11 mGL;
    private GLCanvas mCanvas;
    private GLView mContentView;

    private OrientationSource mOrientationSource;
    // mCompensation is the difference between the UI orientation on GLCanvas
    // and the framework orientation. See OrientationManager for details.
    private int mCompensation;
    // mCompensationMatrix maps the coordinates of touch events. It is kept sync
    // with mCompensation.
    private Matrix mCompensationMatrix = new Matrix();
    private int mDisplayRotation;

    private int mFlags = FLAG_NEED_LAYOUT;
    private volatile boolean mRenderRequested = false;

    private final GalleryEGLConfigChooser mEglConfigChooser =
            new GalleryEGLConfigChooser();

    private final ArrayList<CanvasAnimation> mAnimations =
            new ArrayList<CanvasAnimation>();

    private final ArrayDeque<OnGLIdleListener> mIdleListeners =
            new ArrayDeque<OnGLIdleListener>();

    private final IdleRunner mIdleRunner = new IdleRunner();

    private final ReentrantLock mRenderLock = new ReentrantLock();
    private final Condition mFreezeCondition =
            mRenderLock.newCondition();
    private boolean mFreeze;

    private long mLastDrawFinishTime;
    private boolean mInDownState = false;
    private boolean mFirstDraw = true;

    public GLRootView(Context context) {
        this(context, null);
    }

    public GLRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFlags |= FLAG_INITIALIZED;
        setBackgroundDrawable(null);
        setEGLConfigChooser(mEglConfigChooser);
        setRenderer(this);
        if (ApiHelper.USE_888_PIXEL_FORMAT) {
            getHolder().setFormat(PixelFormat.RGB_888);
        } else {
            getHolder().setFormat(PixelFormat.RGB_565);
        }

        // Uncomment this to enable gl error check.
        // setDebugFlags(DEBUG_CHECK_GL_ERROR);

        if (mIsStereoDisplaySupported) {
            initStereoDisplay();
        }
    }

    @Override
    public void registerLaunchedAnimation(CanvasAnimation animation) {
        // Register the newly launched animation so that we can set the start
        // time more precisely. (Usually, it takes much longer for first
        // rendering, so we set the animation start time as the time we
        // complete rendering)
        mAnimations.add(animation);
    }

    @Override
    public void addOnGLIdleListener(OnGLIdleListener listener) {
        synchronized (mIdleListeners) {
            mIdleListeners.addLast(listener);
            mIdleRunner.enable();
        }
    }
    @Override
    public void setContentPane(GLView content) {
        if (mContentView == content) return;
        if (mContentView != null) {
            if (mInDownState) {
                long now = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(
                        now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                mContentView.dispatchTouchEvent(cancelEvent);
                cancelEvent.recycle();
                mInDownState = false;
            }
            mContentView.detachFromRoot();
            BasicTexture.yieldAllTextures();
        }
        mContentView = content;
        if (content != null) {
            content.attachToRoot(this);
            requestLayoutContentPane();
        }
    }

    @Override
    public void requestRenderForced() {
        superRequestRender();
    }

    @Override
    public void requestRender() {
        if (DEBUG_INVALIDATE) {
            StackTraceElement e = Thread.currentThread().getStackTrace()[4];
            String caller = e.getFileName() + ":" + e.getLineNumber() + " ";
            Log.d(TAG, "invalidate: " + caller);
        }
        if (mRenderRequested) return;
        mRenderRequested = true;
        if (ApiHelper.HAS_POST_ON_ANIMATION) {
            postOnAnimation(mRequestRenderOnAnimationFrame);
        } else {
            super.requestRender();
        }
    }

    private Runnable mRequestRenderOnAnimationFrame = new Runnable() {
        @Override
        public void run() {
            superRequestRender();
        }
    };

    private void superRequestRender() {
        if (DEBUG_PROFILE_PERFORMANCE) {
            MediatekMMProfile.triggerGLRootViewRequest();
        }
        super.requestRender();
    }

    @Override
    public void requestLayoutContentPane() {
        mRenderLock.lock();
        try {
            if (mContentView == null || (mFlags & FLAG_NEED_LAYOUT) != 0) return;

            // "View" system will invoke onLayout() for initialization(bug ?), we
            // have to ignore it since the GLThread is not ready yet.
            if ((mFlags & FLAG_INITIALIZED) == 0) return;

            mFlags |= FLAG_NEED_LAYOUT;
            requestRender();
        } finally {
            mRenderLock.unlock();
        }
    }

    private void layoutContentPane() {
        mFlags &= ~FLAG_NEED_LAYOUT;

        int w = getWidth();
        int h = getHeight();
        int displayRotation = 0;
        int compensation = 0;

        // Get the new orientation values
        if (mOrientationSource != null) {
            displayRotation = mOrientationSource.getDisplayRotation();
            compensation = mOrientationSource.getCompensation();
        } else {
            displayRotation = 0;
            compensation = 0;
        }

        if (mCompensation != compensation) {
            mCompensation = compensation;
            if (mCompensation % 180 != 0) {
                mCompensationMatrix.setRotate(mCompensation);
                // move center to origin before rotation
                mCompensationMatrix.preTranslate(-w / 2, -h / 2);
                // align with the new origin after rotation
                mCompensationMatrix.postTranslate(h / 2, w / 2);
            } else {
                mCompensationMatrix.setRotate(mCompensation, w / 2, h / 2);
            }
        }
        mDisplayRotation = displayRotation;

        // Do the actual layout.
        if (mCompensation % 180 != 0) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        Log.i(TAG, "layout content pane " + w + "x" + h
                + " (compensation " + mCompensation + ")");
        if (mContentView != null && w != 0 && h != 0) {
            mContentView.layout(0, 0, w, h);
        }
        // Uncomment this to dump the view hierarchy.
        //mContentView.dumpTree("");
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        if (changed) requestLayoutContentPane();
    }

    /**
     * Called when the context is created, possibly after automatic destruction.
     */
    // This is a GLSurfaceView.Renderer callback
    @Override
    public void onSurfaceCreated(GL10 gl1, EGLConfig config) {
        GL11 gl = (GL11) gl1;
        if (mGL != null) {
            // The GL Object has changed
            Log.i(TAG, "GLObject has changed from " + mGL + " to " + gl);
        }
        mRenderLock.lock();
        try {
            mGL = gl;
            mCanvas = new GLCanvasImpl(gl);
            BasicTexture.invalidateAllTextures();
        } finally {
            mRenderLock.unlock();
        }

        if (DEBUG_FPS || DEBUG_PROFILE) {
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else {
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    /**
     * Called when the OpenGL surface is recreated without destroying the
     * context.
     */
    // This is a GLSurfaceView.Renderer callback
    @Override
    public void onSurfaceChanged(GL10 gl1, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: " + width + "x" + height
                + ", gl10: " + gl1.toString());
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        GalleryUtils.setRenderThread();
        if (DEBUG_PROFILE) {
            Log.d(TAG, "Start profiling");
            Profile.enable(20);  // take a sample every 20ms
        }
        GL11 gl = (GL11) gl1;
        Utils.assertTrue(mGL == gl);

        mCanvas.setSize(width, height);
    }

    private void outputFps() {
        long now = System.nanoTime();
        if (mFrameCountingStart == 0) {
            mFrameCountingStart = now;
        } else if ((now - mFrameCountingStart) > 1000000000) {
            Log.d(TAG, "fps: " + (double) mFrameCount
                    * 1000000000 / (now - mFrameCountingStart));
            mFrameCountingStart = now;
            mFrameCount = 0;
        }
        ++mFrameCount;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (DEBUG_PROFILE_PERFORMANCE) {
            MediatekMMProfile.startProfileOnDrawFrame();
        }
        AnimationTime.update();
        long t0;
        if (DEBUG_PROFILE_SLOW_ONLY) {
            Profile.hold();
            t0 = System.nanoTime();
        }
        mRenderLock.lock();

        while (mFreeze) {
            mFreezeCondition.awaitUninterruptibly();
        }

        try {
            //added for stereo photo display
            boolean updateStereoMode = prepareForStereo();

            onDrawFrameLocked(gl);

            //added for stereo photo display
            finishForStereo(gl, updateStereoMode);
        } finally {
            mRenderLock.unlock();
        }

        // We put a black cover View in front of the SurfaceView and hide it
        // after the first draw. This prevents the SurfaceView being transparent
        // before the first draw.
        if (mFirstDraw) {
            mFirstDraw = false;
            post(new Runnable() {
                    @Override
                    public void run() {
                        View root = getRootView();
                        View cover = root.findViewById(R.id.gl_root_cover);
                        cover.setVisibility(GONE);
                    }
                });
        }

        if (DEBUG_PROFILE_SLOW_ONLY) {
            long t = System.nanoTime();
            long durationInMs = (t - mLastDrawFinishTime) / 1000000;
            long durationDrawInMs = (t - t0) / 1000000;
            mLastDrawFinishTime = t;

            if (durationInMs > 34) {  // 34ms -> we skipped at least 2 frames
                Log.v(TAG, "----- SLOW (" + durationDrawInMs + "/" +
                        durationInMs + ") -----");
                Profile.commit();
            } else {
                Profile.drop();
            }
        }
        if (DEBUG_PROFILE_PERFORMANCE) {
            MediatekMMProfile.stopProfileOnDrawFrame();
        }
    }

    private void onDrawFrameLocked(GL10 gl) {
        if (DEBUG_FPS) outputFps();

        // release the unbound textures and deleted buffers.
        mCanvas.deleteRecycledResources();

        // reset texture upload limit
        UploadedTexture.resetUploadLimit();

        mRenderRequested = false;

        if ((mOrientationSource != null
                && mDisplayRotation != mOrientationSource.getDisplayRotation())
                || (mFlags & FLAG_NEED_LAYOUT) != 0) {
            layoutContentPane();
        }

        mCanvas.save(GLCanvas.SAVE_FLAG_ALL);
        rotateCanvas(-mCompensation);
        if (mContentView != null) {
           //added for stereo display feature
           transformForStereo();

           mContentView.render(mCanvas);
        } else {
            // M: ALPS00438164 even if mContentView is null, should need clear buffer 
            // avoid draw unknown texture when Gallery exit quickly
            Log.i(TAG, "mContentView == null, glClear buffer");
            mGL.glClear(GL10.GL_COLOR_BUFFER_BIT);
        }
        mCanvas.restore();

        if (!mAnimations.isEmpty()) {
            long now = AnimationTime.get();
            for (int i = 0, n = mAnimations.size(); i < n; i++) {
                mAnimations.get(i).setStartTime(now);
            }
            mAnimations.clear();
        }

        if (UploadedTexture.uploadLimitReached()) {
            requestRender();
        }

        synchronized (mIdleListeners) {
            if (!mIdleListeners.isEmpty()) mIdleRunner.enable();
        }

        if (DEBUG_INVALIDATE) {
            mCanvas.fillRect(10, 10, 5, 5, mInvalidateColor);
            mInvalidateColor = ~mInvalidateColor;
        }

        if (DEBUG_DRAWING_STAT) {
            mCanvas.dumpStatisticsAndClear();
        }
    }

    private void rotateCanvas(int degrees) {
        if (degrees == 0) return;
        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;
        mCanvas.translate(cx, cy);
        mCanvas.rotate(degrees, 0, 0, 1);
        if (degrees % 180 != 0) {
            mCanvas.translate(-cy, -cx);
        } else {
            mCanvas.translate(-cx, -cy);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            mInDownState = false;
        } else if (!mInDownState && action != MotionEvent.ACTION_DOWN) {
            return false;
        }

        if (mCompensation != 0) {
            event = MotionEventHelper.transformEvent(event, mCompensationMatrix);
        }

        mRenderLock.lock();
        try {
            // If this has been detached from root, we don't need to handle event
            boolean handled = mContentView != null
                    && mContentView.dispatchTouchEvent(event);
            if (action == MotionEvent.ACTION_DOWN && handled) {
                mInDownState = true;
            }
            return handled;
        } finally {
            mRenderLock.unlock();
        }
    }

    private class IdleRunner implements Runnable {
        // true if the idle runner is in the queue
        private boolean mActive = false;

        @Override
        public void run() {
            OnGLIdleListener listener;
            synchronized (mIdleListeners) {
                mActive = false;
                if (mIdleListeners.isEmpty()) return;
                listener = mIdleListeners.removeFirst();
            }
            mRenderLock.lock();
            boolean keepInQueue;
            try {
                keepInQueue = listener.onGLIdle(mCanvas, mRenderRequested);
            } finally {
                mRenderLock.unlock();
            }

            synchronized (mIdleListeners) {
                if (keepInQueue) mIdleListeners.addLast(listener);
                if (!mRenderRequested && !mIdleListeners.isEmpty()) enable();
            }
        }

        public void enable() {
            // Who gets the flag can add it to the queue
            if (mActive) return;
            mActive = true;
            queueEvent(this);
        }
    }

    @Override
    public void lockRenderThread() {
        mRenderLock.lock();
    }

    @Override
    public void unlockRenderThread() {
        mRenderLock.unlock();
    }

    @Override
    public void onPause() {
        unfreeze();
        super.onPause();
        if (DEBUG_PROFILE) {
            Log.d(TAG, "Stop profiling");
            Profile.disableAll();
            Profile.dumpToFile("/sdcard/gallery.prof");
            Profile.reset();
        }
    }

    @Override
    public void setOrientationSource(OrientationSource source) {
        mOrientationSource = source;
    }

    @Override
    public int getDisplayRotation() {
        return mDisplayRotation;
    }

    @Override
    public int getCompensation() {
        return mCompensation;
    }

    @Override
    public Matrix getCompensationMatrix() {
        return mCompensationMatrix;
    }

    @Override
    public void freeze() {
        mRenderLock.lock();
        mFreeze = true;
        mRenderLock.unlock();
    }

    @Override
    public void unfreeze() {
        mRenderLock.lock();
        mFreeze = false;
        mFreezeCondition.signalAll();
        mRenderLock.unlock();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setLightsOutMode(boolean enabled) {
        if (!ApiHelper.HAS_SET_SYSTEM_UI_VISIBILITY) return;

        int flags = 0;
        if (enabled) {
            flags = STATUS_BAR_HIDDEN;
            if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
                flags |= (SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
        setSystemUiVisibility(flags);
    }

    // We need to unfreeze in the following methods and in onPause().
    // These methods will wait on GLThread. If we have freezed the GLRootView,
    // the GLThread will wait on main thread to call unfreeze and cause dead
    // lock.
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        unfreeze();
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        unfreeze();
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        unfreeze();
        super.surfaceDestroyed(holder);
    }

    @Override
    protected void onDetachedFromWindow() {
        unfreeze();
        super.onDetachedFromWindow();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            unfreeze();
        } finally {
            super.finalize();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////

    //ADDED for stereo photo display
    private static final boolean mIsStereoDisplaySupported = 
                        MediatekFeature.isStereoDisplaySupported();

    private boolean mStereoMode = false;
    private boolean mSurfaceStereoMode = false;

    private static final int STEREO_DRAW_LEFT_PASS = 0;
    private static final int STEREO_DRAW_RIGHT_PASS = 1;

    private static class StereoPass {
        public StereoPass(float s, float t) {
            scale = s;
            transf = t;
        }
        public float scale;
        public float transf;
    }

    private StereoPass mStereoPass[] = new StereoPass[2];
    private StereoPass mCurrSP = null;
    private int mStereoPassId;

    private void initStereoDisplay(){
        mStereoPass[STEREO_DRAW_LEFT_PASS] = new StereoPass(0.5f,0.0f);
        mStereoPass[STEREO_DRAW_RIGHT_PASS] = new StereoPass(0.5f,0.5f);
        mStereoPassId = MediatekFeature.STEREO_DISPLAY_INVALID_PASS;
    }

    @Override
    public void setStereoMode(boolean mode, boolean force) {
        if (mIsStereoDisplaySupported) {
            if (mStereoMode == mode) {
                return;
            }
            mStereoMode = mode;
            //operate Surface view to render stereo should be
            //performed each time onDrawFrame() is called.
            //Or there may be a chance that surface attributes
            //are modified during frame is drawing
        } else {
            mStereoMode = false;
        }
    }

    @Override
    public boolean getStereoMode() {
        return mSurfaceStereoMode;//mStereoMode;
    }

    @Override
    public int getStereoPassId() {
        return mStereoPassId;
    }

    private void setSurfaceStereoMode(boolean surfaceStereoMode) {
        int stereoLayout = 0;
        if (surfaceStereoMode) {
            stereoLayout = WindowManager.LayoutParams.LAYOUT3D_SIDE_BY_SIDE;
        } else {
            stereoLayout = WindowManager.LayoutParams.LAYOUT3D_DISABLED;
        }
        //call SurfaceView.set3DLayout() to enter the right mode
        Log.i(TAG,"setSurfaceStereoMode:call set3DLayout(stereoLayout="+stereoLayout+")");
        set3DLayout(stereoLayout);
    }

    private void prepareForLeftPath() {
        //set new viewport and projection matrix on left
        mCanvas.setSize(0, 0, getWidth()/2, getHeight());
        //mClipRect.set(0, 0, getWidth()/2, getHeight());
        //set current pass parameters
        mCurrSP = mStereoPass[STEREO_DRAW_LEFT_PASS];
        //set current pass id
        mStereoPassId = 
            MediatekFeature.STEREO_DISPLAY_LEFT_PASS;
    }

    private void prepareForRightPath() {
        //set new viewport and projection matrix on right
        mCanvas.setSize(getWidth()/2, 0, getWidth()/2,getHeight());
        //mClipRect.set(getWidth()/2, 0, getWidth(), getHeight());
        //set current pass parameters
        mCurrSP = mStereoPass[STEREO_DRAW_RIGHT_PASS];
        //set current pass id
        mStereoPassId = 
            MediatekFeature.STEREO_DISPLAY_RIGHT_PASS;
    }

    private void resetStereoPath() {
        //reset canvas mode to normal mode
        mCanvas.setSize(getWidth(), getHeight());
        //reset glScissor clip
        //mClipRect.set(0, 0, getWidth(), getHeight());
    }

    private boolean prepareForStereo() {
        if (!mIsStereoDisplaySupported) return false;

        //added for stereo photo display
        boolean updateStereoMode = false;
        //first, we should check the correct stereo mode
        if (mSurfaceStereoMode != mStereoMode) {
            mSurfaceStereoMode = mStereoMode;
            //then we need to set the stereo mode of surface
            updateStereoMode = true;
            //we delay the actual updating process until drawing finished,
            //this is workaround to make screen flash less frequently
        }

        //if stereo photo should be display
        //we have to draw twice! once left, and once right
        //now, left
        if (mSurfaceStereoMode) {
            prepareForLeftPath();
        }
        return updateStereoMode;
    }

    private void finishForStereo(GL10 gl, boolean updateStereoMode) {
        if (!mIsStereoDisplaySupported) return;
        //now, right
        if (mSurfaceStereoMode) {
            prepareForRightPath();
            //draw right screen content
            onDrawFrameLocked(gl);
            resetStereoPath();
        }
        //finally, update stereo mode if needed
        if (updateStereoMode) {
            setSurfaceStereoMode(mSurfaceStereoMode);
        }
    }

    private void transformForStereo() {
        if (mIsStereoDisplaySupported && mSurfaceStereoMode) {
            mCanvas.translate((float)getWidth() * 
                              mCurrSP.transf, 0.0f, 0.0f);
            mCanvas.scale(mCurrSP.scale,1.0f,1.0f);
        }
    }
}
