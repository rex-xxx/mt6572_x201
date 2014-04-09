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

package com.android.noisefield;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.service.wallpaper.WallpaperService;
import android.view.View;
import android.view.WindowManager;

import com.mediatek.xlog.Xlog;

public class PreviewStubActivity extends Activity {
    private static final String LOG_TAG = "PreviewStubActivity";

    public static final String CLASS_NAME = "CLASS_NAME";
    public static final String PACKAGE_NAME = "PACKAGE_NAME";

    private WallpaperManager mWallpaperManager;
    private WallpaperConnection mWallpaperConnection;
    private Intent mWallpaperIntent;
    private View mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.d(LOG_TAG, "onCreate() ");
        setContentView(R.layout.live_wallpaper_preview);
        mView = findViewById(R.id.set_wallpaper);

        Bundle extra = getIntent().getExtras();
        String pkg = extra.getString(PACKAGE_NAME);
        String cls = extra.getString(CLASS_NAME);

        mWallpaperIntent = new Intent();
        mWallpaperIntent.setAction(WallpaperService.SERVICE_INTERFACE);
        mWallpaperIntent.setComponent(new ComponentName(pkg, cls));
        Xlog.v(LOG_TAG, "mWallpaperIntent = " + mWallpaperIntent);

        mWallpaperManager = WallpaperManager.getInstance(this);
        mWallpaperConnection = new WallpaperConnection(mWallpaperIntent);
    }

    public void setLiveWallpaper(View v) {
        Xlog.d(LOG_TAG, "setLiveWallpaper() ");
        try {
            mWallpaperManager.getIWallpaperManager().setWallpaperComponent(
                    mWallpaperIntent.getComponent());
            mWallpaperManager.setWallpaperOffsetSteps(0.5f, 0.0f);
            mWallpaperManager.setWallpaperOffsets(v.getRootView().getWindowToken(), 0.5f, 0.0f);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(LOG_TAG, "onResume() ");
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            try {
                Xlog.v(LOG_TAG, "onResume() inner mWallpaperConnection = " + mWallpaperConnection);
                mWallpaperConnection.mEngine.setVisibility(true);
                Xlog.d(LOG_TAG, "onResume() setVisibility(true)");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Xlog.d(LOG_TAG, "onPause() ......");
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            try {
                mWallpaperConnection.mEngine.setVisibility(false);
                Xlog.d(LOG_TAG, "onPause() setVisibility(false)");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Xlog.d(LOG_TAG, "onAttachedToWindow() ");
        Handler handler = new Handler();
        mView.post(new Runnable() {
            public void run() {
                if (!mWallpaperConnection.connect()) {
                    Xlog
                            .d(LOG_TAG,
                                    "onAttachedToWindow() mWallpaperConnection.connect() fail, should not enter...");
                    mWallpaperConnection = null;
                }
            }
        });

    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
        }
        mWallpaperConnection = null;
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        final Intent mIntent;
        IWallpaperService mService;
        IWallpaperEngine mEngine;
        boolean mConnected;

        WallpaperConnection(Intent intent) {
            mIntent = intent;
        }

        public boolean connect() {
            synchronized (this) {
                if (!bindService(mIntent, this, Context.BIND_AUTO_CREATE)) {
                    return false;
                }
                Xlog.d(LOG_TAG, "connect() connecting...");
                mConnected = true;
                return true;
            }
        }

        public void disconnect() {
            synchronized (this) {
                mConnected = false;
                if (mEngine != null) {
                    try {
                        mEngine.destroy();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mEngine = null;
                }
                unbindService(this);
                mService = null;
                Xlog.d(LOG_TAG, "disconnect() successfully");
            }
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mWallpaperConnection == this) {
                Xlog.d(LOG_TAG, "connect() successfully");
                mService = IWallpaperService.Stub.asInterface(service);
                try {
                    final View view = mView;
                    final View root = view.getRootView();
                    mService.attach(this, view.getWindowToken(),
                            WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY,
                            true, root.getWidth(), root.getHeight());
                    Xlog.v(LOG_TAG, "onServiceConnected end");

                } catch (RemoteException e) {
                    Xlog.w(LOG_TAG, "Failed attaching wallpaper; clearing", e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mEngine = null;
            if (mWallpaperConnection == this) {
                Xlog.w(LOG_TAG, "Wallpaper service gone: " + name);
            }
        }

        public void attachEngine(IWallpaperEngine engine) {
            synchronized (this) {
                Xlog.v(LOG_TAG, "attachEngine start engine = " + engine);
                if (mConnected) {
                    mEngine = engine;
                    try {
                        Xlog.v(LOG_TAG, "attachEngine engine = " + engine);
                        engine.setVisibility(true);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        engine.destroy();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        public void engineShown(IWallpaperEngine engine) {

        }

        public ParcelFileDescriptor setWallpaper(String name) {
            return null;
        }
    }
}
