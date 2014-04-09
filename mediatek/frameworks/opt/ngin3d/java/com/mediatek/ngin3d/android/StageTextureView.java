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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Ngin3d;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.ngin3d.presentation.PresentationEngine;
import com.mediatek.ngin3d.utils.Ngin3dException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * A view that can display Ngin3d stage contents with TextureView.
 */
public class StageTextureView extends GLTextureView implements GLTextureView.Renderer {
    private static final String TAG = "StageTextureView";
    private static final float STEREO3D_EYE_DISTANCE = 200;
    private Text mTextFPS;
    private Resources mResources;
    private String mCacheDir;
    private String mLibDir;
    protected Stage mStage;

    public StageTextureView(Context context) {
        this(context, (Stage) null);
    }

    public StageTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, new Stage(), false);
    }

    public StageTextureView(Context context, AttributeSet attrs, boolean antiAlias) {
        this(context, attrs, new Stage(), antiAlias);
    }

    /**
     * Initialize this object with android context and Stage class object.
     *
     * @param context android context
     * @param stage   Stage class object
     */
    public StageTextureView(Context context, Stage stage) {
        this(context, null, stage, false);
    }


    /**
     * Initialize this object with android context and Stage class object.
     *
     * @param context android context
     * @param stage   Stage class object
     * @param antiAlias   enable anti-aliasing if true
     */
    public StageTextureView(Context context, Stage stage, boolean antiAlias) {
        this(context, null, stage, antiAlias);
    }

    private StageTextureView(Context context, AttributeSet attrs, Stage stage, boolean antiAlias) {
        super(context, attrs);

        mResources = context.getResources();
        if (context.getCacheDir() != null) {
            mCacheDir = context.getCacheDir().getAbsolutePath();
        }

        if (stage == null) {
            mStage = new Stage(AndroidUiHandler.create());
        } else {
            mStage = stage;
        }

        setEGLContextClientVersion(2);
        if (antiAlias) {
            setEGLConfigChooser(new MultisampleConfigChooser());
        }

        setRenderer(this);

        // Add text to show FPS
        if (mShowFPS) {
            setRenderMode(RENDERMODE_CONTINUOUSLY);
            setupFPSText();
        } else {
            setRenderMode(RENDERMODE_WHEN_DIRTY);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause from activity");
        // pause rendering and animations
        pauseRendering();
        super.onPause();
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow");
        makeTransparency();
        onPause();

        super.onDetachedFromWindow();
    }

    public void onResume() {
        Log.d(TAG, "onResume from activity");

        super.onResume();
        // resume rendering and animations
        resumeRendering();
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
     * Get the presentation engine of this object
     *
     * @return presentation engine
     */
    public PresentationEngine getPresentationEngine() {
        return mPresentationEngine;
    }

    /**
     * Get the the screen shot of current render frame in StageTextureView
     *
     * This method is deprecated, please use getBitmap() in TextureView
     * instead of getScreenShot().
     *
     * @return A Bitmap object representing the rendering texture
     */
    @Deprecated
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
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }

        if (obj instanceof Bitmap) {
            return (Bitmap) obj;
        }
        return null;
    }

    public double getFPS() {
        return mPresentationEngine.getFPS();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Log.d(TAG, "onVisibilityChanged, visibility is:" + visibility);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged, w: " + w + " h: " + h + " oldw: " + oldw + " oldh: " + oldh);
        if (mShowFPS) {
            mTextFPS.setPosition(new Point(w, 0));  // show it at right-bottom corner
        } else {
            requestRender();
        }
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");

        // Increase the priority of the render thread
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        boolean enableNDB = SystemProperties.getBoolean("ngin3d.enableNDB", false);
        mPresentationEngine = Ngin3d.createPresentationEngine(mStage);
        mPresentationEngine.setRenderCallback(new PresentationEngine.RenderCallback() {
            public void requestRender() {
                StageTextureView.this.requestRender();
            }
        });
        Log.d(TAG, "PresentationEngine initialize ");
        mPresentationEngine.initialize(getWidth(), getHeight(), mResources, mCacheDir, enableNDB, mLibDir);

        // if there are any paused animation, resume it
        resumeRendering();

        synchronized (mSurfaceReadyLock) {
            mSurfaceReadyLock.notifyAll();
        }

        if (Ngin3d.DEBUG) {
            mPresentationEngine.dump();
        }

        post(new Runnable() {
            public void run() {
                makeTransparency();
            }
        });

    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.v(TAG, "onSurfaceChanged(width = " + width + ", height = " + height + ")");

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

    public void onDrawFrame(GL10 gl) {
        if (mFirstRender == FirstRender.None) {
            Log.d(TAG, "onDrawFrame() invoked @" + SystemClock.uptimeMillis());
            mFirstRender = FirstRender.FirstRenderStarted;
            // Trigger next rendering or the widget will be disappeared if no request render anymore
            setRenderMode(GLTextureView.RENDERMODE_CONTINUOUSLY);
        } else if (mFirstRender == FirstRender.FirstRenderStarted) {
            if (!mShowFPS) {
                setRenderMode(GLTextureView.RENDERMODE_WHEN_DIRTY);
            }
            // Try to fix SurfaceTexture is black in initial state issue,
            // Show TextureView only after first rendering is completed.
            post(new Runnable() {
                public void run() {
                    Log.d(TAG, "setAlpha 1");
                    setAlpha(1);
                    mFirstRender = FirstRender.FirstRenderCompleted;
                }
            });
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

    public void onPaused() {
        if (mPresentationEngine != null) {
            mPresentationEngine.uninitialize();
            mPresentationEngine = null;
        }
    }

    private void setupFPSText() {
        mTextFPS = new Text("");
        mTextFPS.setAnchorPoint(new Point(1.f, 0.f));
        mTextFPS.setPosition(new Point(0, 0));
        mTextFPS.setTextColor(Color.YELLOW);
        mStage.add(mTextFPS);
    }

    /**
     * This method can change the cache path where binary shaders, animation cache and
     * symbolic library are stored.
     * Especially for widget (run on launcher process) and lockscreen 3D view(run on lockscreen process),
     * we have to set cache directory to launcher or lockscreen application's directory.
     * Only StageTextureView support this method since only this class can be used as widget and 3D lockscreen.
     * Must invoke the method in the constructor to apply the new cache directory or
     * default cache path (application's cache directory) will be used.
     *
     * @param context a context instance
     * @param cacheDir cache directory that binary
     */
    public void setCacheDir( Context context, String cacheDir) {
        if (context == null) {
            throw new Ngin3dException("The context can not be null");
        }
        mCacheDir = cacheDir;

        // Set keyframe animation cache directory
        AnimationLoader.setCacheDir(new File(mCacheDir));

        // Setup symbolic of JNI shared library
        mLibDir = mCacheDir + context.getPackageName();

        // If symbolic is created already, no need to create it again.
        File symbolic = new File(mLibDir + "/libja3m.so");
        if (symbolic.exists()) {
            return;
        }

        new File(mLibDir).mkdirs();
        try {
            // Create link
            Runtime.getRuntime().exec("ln -s /system/lib/libja3m.so " + mLibDir + "/libja3m.so");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure symbolic is created, if fail to create (ex: no permission),
        // set mLibDir to null and presentation engine will load shared library from default path.
        if (!symbolic.exists()) {
            mLibDir = null;
        }
    }

    /**
     * Pause the rendering
     */
    public void pauseRendering() {
        if (mShowFPS) {
            setRenderMode(RENDERMODE_WHEN_DIRTY);
        }

        if (mPresentationEngine != null) {
            mPresentationEngine.pauseRendering();
        }
    }

    /**
     * Resume the rendering
     */
    public void resumeRendering() {
        // adjust all timelines by current tick time
        if (mPresentationEngine != null) {
            mPresentationEngine.resumeRendering();

            requestRender();
            if (mShowFPS) {
                setRenderMode(RENDERMODE_CONTINUOUSLY);
            }
        }
    }

    public Boolean isSurfaceReady() {
        return mPresentationEngine != null && mPresentationEngine.isReady();
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
