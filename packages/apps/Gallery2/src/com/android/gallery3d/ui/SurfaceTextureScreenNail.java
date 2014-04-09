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

package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;

import com.android.gallery3d.common.ApiHelper;

@TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
public abstract class SurfaceTextureScreenNail implements ScreenNail,
        SurfaceTexture.OnFrameAvailableListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/SurfaceTextureScreenNail";
    // This constant is not available in API level before 15, but it was just an
    // oversight.
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    protected ExtTexture mExtTexture;
    private SurfaceTexture mSurfaceTexture;
    private int mWidth, mHeight;
    private float[] mTransform = new float[16];
    private boolean mHasTexture = false;

    // M: debug info for draw preview.
    protected static final int INTERVALS = 60;
    protected int mDebugFlag = SystemProperties.getInt("cam.debug", 0);
    protected boolean mDebug = false;
    protected boolean mDebugLevel2 = false;
    protected int mDrawFrameCount = 0;
    protected int mRequestCount = 0;
    protected long mRequestStartTime = 0;
    protected long mDrawStartTime = 0;

    public SurfaceTextureScreenNail() {
    	mDebug = mDebugFlag > 0;
        mDebugLevel2 = mDebugFlag > 1;
    }

    private static int sMaxHightProrityFrameCount = 8;
    private int currentFrameCount = 0;
    private static HandlerThread sFrameListener = new HandlerThread("FrameListener");

    public void acquireSurfaceTexture() {
        mExtTexture = new ExtTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mExtTexture.setSize(mWidth, mHeight);
        if (!sFrameListener.isAlive()) {
            sFrameListener.start();
        }
//        mSurfaceTexture = new SurfaceTexture(mExtTexture.getId());
        mSurfaceTexture = new SurfaceTexture(mExtTexture.getId(), sFrameListener.getLooper());
        // initialize for the beginning of the looper
        initializePriority();

        setDefaultBufferSize(mSurfaceTexture, mWidth, mHeight);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        synchronized (this) {
            mHasTexture = true;
        }
    }

    @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private static void setDefaultBufferSize(SurfaceTexture st, int width, int height) {
        if (ApiHelper.HAS_SET_DEFALT_BUFFER_SIZE) {
            st.setDefaultBufferSize(width, height);
        }
    }

    @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void releaseSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(null);
        if (ApiHelper.HAS_RELEASE_SURFACE_TEXTURE) {
            st.release();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void releaseSurfaceTexture() {
        synchronized (this) {
            mHasTexture = false;
        }
        mExtTexture.recycle();
        mExtTexture = null;
        releaseSurfaceTexture(mSurfaceTexture);
        mSurfaceTexture = null;
    }

    public void fullHandlerCapacity() {
        Log.i(TAG,"fullHandlerCapacity:set urgent display");
        Process.setThreadPriority(sFrameListener.getThreadId(), Process.THREAD_PRIORITY_URGENT_DISPLAY);
    }

    public void normalHandlerCapacity() {
        Log.i(TAG,"normalHandlerCapacity:set normal");
        Process.setThreadPriority(sFrameListener.getThreadId(), Process.THREAD_PRIORITY_DEFAULT);
    }

    private void initializePriority() {
        fullHandlerCapacity();
        currentFrameCount = 0;
    }

    private void checkThreadPriority() {
        if (currentFrameCount == sMaxHightProrityFrameCount) {
            normalHandlerCapacity();
            currentFrameCount ++;
        } else if (currentFrameCount < sMaxHightProrityFrameCount) {
            currentFrameCount ++;
        }
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void resizeTexture() {
        if (mExtTexture != null) {
            mExtTexture.setSize(mWidth, mHeight);
            setDefaultBufferSize(mSurfaceTexture, mWidth, mHeight);
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        synchronized (this) {
            if (!mHasTexture) return;
            checkThreadPriority();
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTransform);

            // Flip vertically.
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            int cx = x + width / 2;
            int cy = y + height / 2;
            canvas.translate(cx, cy);
            canvas.scale(1, -1, 1);
            canvas.translate(-cx, -cy);
            updateTransformMatrix(mTransform);
            canvas.drawTexture(mExtTexture, mTransform, x, y, width, height);
            canvas.restore();

            if (mDebug) {
                if (mDebugLevel2) {
                    Log.d(TAG, "[Preview] GLCanvas drawing Frame");
                }
            	mDrawFrameCount++;
                if (mDrawFrameCount % INTERVALS == 0) {
                	long currentTime = System.currentTimeMillis();
                	int intervals = (int) (currentTime - mDrawStartTime);
                	Log.d(TAG, "[Preview] Drawing frame, fps = "
                			+ (mDrawFrameCount * 1000.0f) / intervals + " in last " + intervals + " millisecond.");
                	mDrawStartTime = currentTime;
                	mDrawFrameCount = 0;
                }
            }
        }
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        // M: ignore
        //throw new UnsupportedOperationException();
        Log.d(TAG, "draw(), fails!!!");
        return;
    }

    protected void updateTransformMatrix(float[] matrix) {}

    @Override
    abstract public void noDraw();

    @Override
    abstract public void recycle();

    @Override
    abstract public void onFrameAvailable(SurfaceTexture surfaceTexture);
}
