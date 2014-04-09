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

import com.android.camera.manager.MMProfileManager;

import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.RawTexture;
import com.android.gallery3d.ui.SurfaceTextureScreenNail;

/*
 * This is a ScreenNail which can displays camera preview.
 */
public class CameraScreenNail extends SurfaceTextureScreenNail {
    private static final String TAG = "CameraScreenNail";
    private static final int ANIM_NONE = 0;
    // Capture animation is about to start.
    private static final int ANIM_CAPTURE_START = 1;
    // Capture animation is running.
    private static final int ANIM_CAPTURE_RUNNING = 2;
    // Switch camera animation needs to copy texture.
    private static final int ANIM_SWITCH_COPY_TEXTURE = 3;
    // Switch camera animation shows the initial feedback by darkening the
    // preview.
    private static final int ANIM_SWITCH_DARK_PREVIEW = 4;
    // Switch camera animation is waiting for the first frame.
    private static final int ANIM_SWITCH_WAITING_FIRST_FRAME = 5;
    // Switch camera animation is about to start.
    private static final int ANIM_SWITCH_START = 6;
    // Switch camera animation is running.
    private static final int ANIM_SWITCH_RUNNING = 7;

    private boolean mVisible;
    // True if first onFrameAvailable has been called. If screen nail is drawn
    // too early, it will be all white.
    private boolean mFirstFrameArrived;
    private Listener mListener;
    private final float[] mTextureTransformMatrix = new float[16];

    // Animation.
    private CaptureAnimManager mCaptureAnimManager = new CaptureAnimManager();
    private SwitchAnimManager mSwitchAnimManager = new SwitchAnimManager();
    private int mAnimState = ANIM_NONE;
    private RawTexture mAnimTexture;
    // Some methods are called by GL thread and some are called by main thread.
    // This protects mAnimState, mVisible, and surface texture. This also makes
    // sure some code are atomic. For example, requestRender and setting
    // mAnimState.
    private Object mLock = new Object();
    private boolean mDrawable = true;
    private int mX;
    private int mY;
    private int mWidth;
    private int mHeight;
    private RawTexture mOriginSizeTexture;
    private int mSwitchActorState = ANIM_SIZE_CHANGE_NONE;
    private static final int ANIM_SIZE_CHANGE_NONE = 0;
    private static final int ANIM_SIZE_CHANGE_START = 1;
    private static final int ANIM_SIZE_CHANGE_RUNNING = 2;

    public interface Listener {
        void requestRender();
        // Preview has been copied to a texture.
        void onPreviewTextureCopied();
    }

    public CameraScreenNail(Listener listener) {
        mListener = listener;
    }

    @Override
    public void acquireSurfaceTexture() {
        synchronized (mLock) {
            mFirstFrameArrived = false;
            super.acquireSurfaceTexture();
            mAnimTexture = new RawTexture(getWidth(), getHeight(), true);
            mOriginSizeTexture = new RawTexture(getWidth(), getHeight(), true);
        }
    }

    @Override
    public void releaseSurfaceTexture() {
        synchronized (mLock) {
            super.releaseSurfaceTexture();
            mAnimState = ANIM_NONE; // stop the animation
        }
    }

    public void copyTexture() {
        synchronized (mLock) {
            mListener.requestRender();
            mAnimState = ANIM_SWITCH_COPY_TEXTURE;
        }
    }

    public void copyOriginSizeTexture() {
        synchronized (mLock) {
            //run copyOriginSizeTexture is a new start, 
            //mSwitchActorState whatever it is should be set ANIM_SIZE_CHANGE_START.
            if (mFirstFrameArrived) {
                mListener.requestRender();
                mSwitchActorState = ANIM_SIZE_CHANGE_START;
            }
        }
    }

    public void stopSwitchActorAnimation() {
        synchronized (mLock) {
            if (mSwitchActorState != ANIM_SIZE_CHANGE_NONE) {
                mSwitchActorState = ANIM_SIZE_CHANGE_NONE;
                mListener.requestRender();
            }
        }
    }

    public void animateSwitchCamera() {
        Log.v(TAG, "animateSwitchCamera");
        MMProfileManager.startProfileAnimateSwitchCamera();
        synchronized (mLock) {
            if (mAnimState == ANIM_SWITCH_DARK_PREVIEW) {
                // Do not request render here because camera has been just
                // started. We do not want to draw black frames.
                mAnimState = ANIM_SWITCH_WAITING_FIRST_FRAME;
            }
        }
    }

    public void animateCapture(int animOrientation) {
        MMProfileManager.startProfileAnimateCapture();
        synchronized (mLock) {   
            mCaptureAnimManager.setOrientation(animOrientation);
            mListener.requestRender();
            mAnimState = ANIM_CAPTURE_START;
        }
    }

    public void directDraw(GLCanvas canvas, int x, int y, int width, int height) {
        if (mSwitchActorState == ANIM_SIZE_CHANGE_RUNNING) {
            mOriginSizeTexture.draw(canvas, x, y, width, height);
        } else {
            super.draw(canvas, x, y, width, height);
        }
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        MMProfileManager.startProfileDrawScreenNail();
        synchronized (mLock) {
            if (!mVisible) {
                mVisible = true;
            }
            SurfaceTexture surfaceTexture = getSurfaceTexture();
            if (surfaceTexture == null || !mFirstFrameArrived) {
                MMProfileManager.stopProfileDrawScreenNail();
                return;
            }

            if (mAnimState == ANIM_NONE && mSwitchActorState == ANIM_SIZE_CHANGE_NONE) {
                super.draw(canvas, x, y, width, height);
                MMProfileManager.stopProfileDrawScreenNail();
                return;
            }

            switch (mAnimState) {
                case ANIM_SWITCH_COPY_TEXTURE:
                    copyPreviewTexture(canvas);
                    mSwitchAnimManager.setReviewDrawingSize(width, height);
                    mListener.onPreviewTextureCopied();
                    mAnimState = ANIM_SWITCH_DARK_PREVIEW;
                    // The texture is ready. Fall through to draw darkened
                    // preview.
                case ANIM_SWITCH_DARK_PREVIEW:
                case ANIM_SWITCH_WAITING_FIRST_FRAME:
                    // Consume the frame. If the buffers are full,
                    // onFrameAvailable will not be called. Animation state
                    // relies on onFrameAvailable.
                    surfaceTexture.updateTexImage();
                    mSwitchAnimManager.drawDarkPreview(canvas, x, y, width,
                            height, mAnimTexture);
                    return;
                case ANIM_SWITCH_START:
                    mSwitchAnimManager.startAnimation();
                    mAnimState = ANIM_SWITCH_RUNNING;
                    break;
                case ANIM_CAPTURE_START:
                    copyPreviewTexture(canvas);
                    mCaptureAnimManager.startAnimation(x, y, width, height);
                    mAnimState = ANIM_CAPTURE_RUNNING;
                    break;
            }

            if (mAnimState == ANIM_CAPTURE_RUNNING || mAnimState == ANIM_SWITCH_RUNNING) {
                boolean drawn;
                if (mAnimState == ANIM_CAPTURE_RUNNING) {
                    drawn = mCaptureAnimManager.drawAnimation(canvas, this, mAnimTexture);
                } else {
                    drawn = mSwitchAnimManager.drawAnimation(canvas, x, y,
                            width, height, this, mAnimTexture);
                }
                if (drawn) {
                    mListener.requestRender();
                } else {
                    // Continue to the normal draw procedure if the animation is
                    // not drawn.
                    if(mAnimState == ANIM_CAPTURE_RUNNING) {
                      MMProfileManager.stopProfileAnimateCapture();  
                    } else if(mAnimState == ANIM_SWITCH_RUNNING) {
                      MMProfileManager.stopProfileAnimateSwitchCamera();
                    }
                    mAnimState = ANIM_NONE;
                    // draw origin frame when size changed
                    if (mSwitchActorState == ANIM_SIZE_CHANGE_NONE) {
                        super.draw(canvas, x, y, width, height);
                    }
                }
            }

            switch (mSwitchActorState) {
                case ANIM_SIZE_CHANGE_START:
                    copyOriginSizePreviewTexture(canvas);
                    mX = x;
                    mY = y;
                    mWidth = width;
                    mHeight = height;
                    mSwitchActorState = ANIM_SIZE_CHANGE_RUNNING;
                    break;
                case ANIM_SIZE_CHANGE_RUNNING:
                    if (mDrawable && (mWidth != width || mHeight != height) && (mAnimState != ANIM_CAPTURE_RUNNING)) {
                        mSwitchActorState = ANIM_SIZE_CHANGE_NONE;
                        super.draw(canvas, x, y, width, height);
                    }
                    break;
            }
            if (mAnimState == ANIM_NONE && mSwitchActorState == ANIM_SIZE_CHANGE_RUNNING) {
                mOriginSizeTexture.draw(canvas, mX, mY, mWidth, mHeight);
            }
        } // mLock
        MMProfileManager.stopProfileDrawScreenNail();
    }

    private void copyPreviewTexture(GLCanvas canvas) {
        // For Mock Camera, do not copy texture, as SW OpenGL solution
        // does not support some function.
        if (com.mediatek.camera.FrameworksClassFactory.isMockCamera()) {
            return;
        }
        int width = mAnimTexture.getWidth();
        int height = mAnimTexture.getHeight();
        canvas.beginRenderTarget(mAnimTexture);
        // Flip preview texture vertically. OpenGL uses bottom left point
        // as the origin (0, 0).
        canvas.translate(0, height);
        canvas.scale(1, -1, 1);
        getSurfaceTexture().getTransformMatrix(mTextureTransformMatrix);
        canvas.drawTexture(mExtTexture,
                mTextureTransformMatrix, 0, 0, width, height);
        canvas.endRenderTarget();
    }

    private void copyOriginSizePreviewTexture(GLCanvas canvas) {
        int width = mOriginSizeTexture.getWidth();
        int height = mOriginSizeTexture.getHeight();
        canvas.beginRenderTarget(mOriginSizeTexture);
        // Flip preview texture vertically. OpenGL uses bottom left point
        // as the origin (0, 0).
        canvas.translate(0, height);
        canvas.scale(1, -1, 1);
        getSurfaceTexture().getTransformMatrix(mTextureTransformMatrix);
        canvas.drawTexture(mExtTexture,
                mTextureTransformMatrix, 0, 0, width, height);
        canvas.endRenderTarget();
    }

    @Override
    public void noDraw() {
        synchronized (mLock) {
            mVisible = false;
        }
    }

    @Override
    public void recycle() {
        synchronized (mLock) {
            mVisible = false;
        }
    }

    private long mLastFrameArriveTime;
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        MMProfileManager.triggerFrameAvailable();
        if (mDebugLevel2) {
            Log.d(TAG, "[Preview] onFrameAvailable");
        }
        setDrawable(true);
        if (!mFirstFrameArrived) {
            MMProfileManager.triggerFirstFrameAvailable();
            if (mFrameListener != null) {
                mFrameListener.onFirstFrameArrived();
            }
            if (!mVisible) {
                // We need to ask for re-render if the SurfaceTexture receives a new frame.
                MMProfileManager.triggerRequestRender();
                mListener.requestRender();
            }
            if (LOG) {
                Log.v(TAG, "onFrameAvailable is called(first time) " + this);
            }
        }
        synchronized (mLock) {
            if (mDebug && !mFirstFrameArrived) {
                mRequestStartTime = System.currentTimeMillis() - 1; // avoid divide by zero
                mLastFrameArriveTime = mRequestStartTime;
                mDrawStartTime = mRequestStartTime;
                mRequestCount = 0;
                mDrawFrameCount = 0;
            }
            mFirstFrameArrived = true;
            if (mVisible) {
                if (mAnimState == ANIM_SWITCH_WAITING_FIRST_FRAME) {
                    mAnimState = ANIM_SWITCH_START;
                }
                if (mDebug) {
                    long currentTime = 0;
                    if (mDebugLevel2) {
                        currentTime = System.currentTimeMillis();
                        int frameInterval = (int)(currentTime - mLastFrameArriveTime);
                        if (frameInterval > 50) {
                            Log.d(TAG, "[Preview] onFrameAvailable, request render interval too long = " + frameInterval);
                        }
                        mLastFrameArriveTime = currentTime;
                    }
                    mRequestCount++;
                    if (mRequestCount % INTERVALS == 0) {
                        if (!mDebugLevel2) {
                            currentTime = System.currentTimeMillis();
                        }
                        int intervals = (int) (currentTime - mRequestStartTime);
                        Log.d(TAG, "[Preview] Request render, fps = "
                                + (mRequestCount * 1000.0f) / intervals + " in last " + intervals + " millisecond.");
                        mRequestStartTime = currentTime;
                        mRequestCount = 0;
                    }
                }
                // We need to ask for re-render if the SurfaceTexture receives a new
                // frame.
                MMProfileManager.triggerRequestRender();
                mListener.requestRender();
            }
        }
    }

    // We need to keep track of the size of preview frame on the screen because
    // it's needed when we do switch-camera animation. See comments in
    // SwitchAnimManager.java. This is based on the natural orientation, not the
    // view system orientation.
    public void setPreviewFrameLayoutSize(int width, int height) {
        synchronized (mLock) {
            mSwitchAnimManager.setPreviewFrameLayoutSize(width, height);
        }
    }

    public boolean enableDebug() {
        return mDebug;
    }
    
    /// M: for loading animation @{
    private static final boolean LOG = Log.LOGV;
    public interface FrameListener {
        void onFirstFrameArrived();
    }
    
    private FrameListener mFrameListener;
    public void setFrameListener(FrameListener listener) {
        mFrameListener = listener;
    }
    /// @}

    public void setDrawable(boolean drawable) {
        Log.v(TAG, "setDrawable drawable = " + drawable);
        mDrawable = drawable;
    }
}
