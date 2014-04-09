/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ngin3d.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Ngin3d;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.presentation.PresentationEngine;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * A view that can display Ngin3d stage contents.
 */
public class StageView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = "StageView";
    private static final float STEREO3D_EYE_DISTANCE = 200;

    protected final Stage mStage;
    private Text mTextFPS;
    private final PresentationEngine mPresentationEngine;
    private Resources mResources;
    private String mCacheDir;
    private boolean mShowFPS;
    private Thread mGLThread;
    private static float sPixelDensity = -1f;

    public StageView(Context context) {
        this(context, (Stage) null);
    }

    public StageView(Context context, AttributeSet attrs) {
        this(context, attrs, new Stage(), false);
    }

    public StageView(Context context, AttributeSet attrs, boolean antiAlias) {
        this(context, attrs, new Stage(), antiAlias);
    }

    /**
     * Initialize this object with android context and Stage class object.
     *
     * @param context android context
     * @param stage   Stage class object
     */
    public StageView(Context context, Stage stage) {
        this(context, null, stage, false);
    }

    /**
     * Initialize this object with android context and Stage class object.
     *
     * @param context android context
     * @param stage   Stage class object
     * @param antiAlias   enable anti-aliasing if true
     */
    public StageView(Context context, Stage stage, boolean antiAlias) {
        this(context, null, stage, antiAlias);
    }

    private StageView(Context context, AttributeSet attrs, Stage stage, boolean antiAlias) {
        super(context, attrs);
        if (stage == null) {
            mStage = new Stage(AndroidUiHandler.create());
        } else {
            mStage = stage;
        }

        setEGLContextClientVersion(2);
        if (antiAlias) {
            setEGLConfigChooser(new MultisampleConfigChooser());
        } else {
            setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        }

        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);

        setRenderer(this);
        mResources = context.getResources();
        mPresentationEngine = Ngin3d.createPresentationEngine(mStage);

        if (context.getCacheDir() != null) {
            mCacheDir = context.getCacheDir().getAbsolutePath();
        }
        mShowFPS = SystemProperties.getBoolean("ngin3d.showfps", Ngin3d.showFPS());

        // Add text to show FPS
        if (mShowFPS) {
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            setupFPSText();
        } else {
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    private void setupFPSText() {
        mTextFPS = new Text("");
        mTextFPS.setAnchorPoint(new Point(1.f, 0.f));
        mTextFPS.setPosition(new Point(0, 0));
        mTextFPS.setTextColor(Color.YELLOW);
        mStage.add(mTextFPS);
    }

    public void runInGLThread(Runnable runnable) {
        if (Thread.currentThread() == mGLThread) {
            runnable.run();
        } else {
            queueEvent(runnable);
        }
    }

    public static float dpToPixel(Context context, float dp) {
        synchronized (StageView.class) {
            if (sPixelDensity < 0) {
                DisplayMetrics metrics = new DisplayMetrics();
                ((Activity) context).getWindowManager()
                    .getDefaultDisplay().getMetrics(metrics);
                sPixelDensity =  metrics.density;
            }
            return sPixelDensity * dp;
        }
    }

    public static int dpToPixel(Context context, int dp) {
        return (int)(dpToPixel(context, (float) dp) + .5f);
    }

    /**
     * This method can change the cache path where binary shaders are stored.
     * Must invoke the method in the constructor to apply the new cache directory or
     * default cache path (application's cache directory) will be used.
     *
     * @param cacheDir cache directory that binary
     */
    public void setCacheDir(String cacheDir) {
        mCacheDir = cacheDir;
    }

    /**
     * Get the original stage object of this class
     *
     * @return original stage object
     */
    public final Stage getStage() {
        return mStage;
    }


    /**
     * Get FPS
     *
     * @return FPS value
     */
    public double getFPS() {
        return mPresentationEngine.getFPS();
    }

    /**
     * Get the presentation engine of this object
     *
     * @return presentation engine
     */
    public PresentationEngine getPresentationEngine() {
        return mPresentationEngine;
    }

    /**
     * Enable/disable stereoscopic 3d display mode
     *
     * @param enable true if you want to enable stereo 3d display mode
     */
    public void enableStereoscopic3D(boolean enable) {
        mStage.setStereo3D(enable, STEREO3D_EYE_DISTANCE);
    }

    /**
     * Enable/disable stereoscopic 3d display mode
     *
     * @param enable       true if you want to enable stereo 3d display mode
     * @param eyesDistance the distance between two eyes
     */
    public void enableStereoscopic3D(boolean enable, float eyesDistance) {
        mStage.setStereo3D(enable, eyesDistance);
    }

    /**
     * Get the the screen shot of current render frame
     *
     * @return A Bitmap object representing the render frame
     */
    public Bitmap getScreenShot() {
        FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
            public Object call() {
                return mPresentationEngine.getScreenShot();
            }
        });

        runInGLThread(task);
        Object obj = null;
        try {
            obj = task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (obj instanceof Bitmap) {
            return (Bitmap) obj;
        }
        return null;
    }

    private double mLastFps = -1;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.v(TAG, "onSurfaceCreated()");

        // Increase the priority of the render thread
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        mGLThread = Thread.currentThread();

        final int w = getWidth();
        final int h = getHeight();
        boolean enableNDB = SystemProperties.getBoolean("ngin3d.enableNDB", false);

        // Set render callback so that the presentation engine can trigger renders.
        mPresentationEngine.setRenderCallback(new PresentationEngine.RenderCallback() {
            public void requestRender() {
                StageView.this.requestRender();
            }
        });

        mPresentationEngine.initialize(w, h, mResources, mCacheDir, enableNDB);

        synchronized (mSurfaceReadyLock) {
            mSurfaceReadyLock.notifyAll();
        }

        if (Ngin3d.DEBUG) {
            mPresentationEngine.dump();
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.v(TAG, "onSurfaceChanged(width = " + width + ", height = " + height + ")");

        gl.glViewport(0, 0, width, height);
        /** The actors' position might be normalized values and the real position values depend on Width/Height.
         *  We need make position property dirty to recalculate correct position of actors after surface changed
         */
        mStage.touchProperty("position");

        mPresentationEngine.resize(width, height);

        if (mShowFPS) {
            mTextFPS.setPosition(new Point(width, 0));  // show it at right-bottom corner
        } else {
            requestRender();
        }
    }

    public static final long INVALID_TIME = -1;
    private long mFirstOnDrawFrameTime = INVALID_TIME;

    public long getFirstOnDrawTime() {
        return mFirstOnDrawFrameTime;
    }

    public void onDrawFrame(GL10 gl) {
        if (mFirstOnDrawFrameTime == INVALID_TIME) {
            mFirstOnDrawFrameTime = SystemClock.uptimeMillis();
            Log.d(TAG, "onDrawFrame() invoked @" + mFirstOnDrawFrameTime);
        }

        if (mShowFPS) {
            mTextFPS.setText(String.format("FPS: %.2f", mPresentationEngine.getFPS()));
            mPresentationEngine.render();
        } else {
            if (mPresentationEngine.render()) {
                requestRender();
            }
        }
    }

    @Override
    public void onPause() {
        // pause rendering and animations
        pauseRendering();
        // Uninitialize presentation engine before GLSurface paused the rendering thread.
        FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
            public Void call() {
                mPresentationEngine.uninitialize();
                return null;
            }
        });
        runInGLThread(task);
        super.onPause();
    }

    public void onResume() {
        Log.d(TAG, "onResume from activity");

        super.onResume();
        // resume rendering and animations
        resumeRendering();
    }

    /**
     * Pause the rendering
     */
    public void pauseRendering() {
        if (mShowFPS) {
            setRenderMode(RENDERMODE_WHEN_DIRTY);
        }

        mPresentationEngine.pauseRendering();
    }

    /**
     * Resume the rendering
     */
    public void resumeRendering() {
        // adjust all timelines by current tick time
        mPresentationEngine.resumeRendering();

        if (mShowFPS) {
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        } else {
            requestRender();
        }
    }

    public Boolean isSurfaceReady() {
        return mPresentationEngine.isReady();
    }

    private final Object mSurfaceReadyLock = new Object();

    public void waitSurfaceReady() {
        synchronized (mSurfaceReadyLock) {
            while (!isSurfaceReady()) {
                try {
                    mSurfaceReadyLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
