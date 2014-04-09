/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.dreams.basic;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;

public class PreviewStubActivity extends Activity implements
        TextureView.SurfaceTextureListener {

    private TextureView mTextureView;
    private HandlerThread mRendererHandlerThread;
    private Handler mRendererHandler;
    private ColorsGLRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        if (mRendererHandlerThread == null) {
            mRendererHandlerThread = new HandlerThread(getClass()
                    .getSimpleName());
            mRendererHandlerThread.start();
            mRendererHandler = new Handler(mRendererHandlerThread.getLooper());
        }
        setContentView(mTextureView);
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface,
            final int width, final int height) {
        Colors.LOG("onSurfaceTextureAvailable(%s, %d, %d)", surface, width,
                height);
        mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.stop();
                }
                mRenderer = new ColorsGLRenderer(surface, width, height);
                mRenderer.start();
            }
        });
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
            final int width, final int height) {
        Colors.LOG("onSurfaceTextureSizeChanged(%s, %d, %d)", surface, width,
                height);
        mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.setSize(width, height);
                }
            }
        });
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Colors.LOG("onSurfaceTextureDestroyed(%s)", surface);
        mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.stop();
                    mRenderer = null;
                }
                mRendererHandlerThread.quit();
            }
        });
        try {
            mRendererHandlerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Colors.LOG("onSurfaceTextureUpdated(%s)", surface);
    }
}
