package com.android.dreams.basic.test;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamService;
import android.test.ServiceTestCase;
import android.util.Log;

import com.android.dreams.basic.Colors;

public class DreamColorsTest extends ServiceTestCase<Colors> {

    private static final String TAG = DreamColorsTest.class.getSimpleName();
    private Context mContext;

    public DreamColorsTest() {
        super(Colors.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getSystemContext();
    }

    @Override
    protected void tearDown() throws Exception {
        mContext = null;
        super.tearDown();
    }

    public void testCase01bind() {
        Intent intent = new Intent(DreamService.SERVICE_INTERFACE);
        intent.setClass(mContext, Colors.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            IDreamService binder = IDreamService.Stub
                    .asInterface(bindService(intent));
            assertNotNull(binder);
            Colors colorsService = getService();
            assertNotNull(colorsService);
            assertNotNull(ReflectionHelper.getNonPublicField(Colors.class,
                    "mTextureView"));
            Binder token = new Binder();
            binder.attach(token);
            Thread.sleep(1000);
            assertNotNull(ReflectionHelper.getNonPublicField(Colors.class,
                    "mRenderer"));
            // colorsService.onAttachedToWindow();
            // colorsService.onSurfaceTextureAvailable(null, 480, 800);
            colorsService.onSurfaceTextureSizeChanged(null, 100, 100);
            colorsService.onSurfaceTextureDestroyed(null);
            colorsService.onSurfaceTextureUpdated(null);
            binder.detach();
            Thread.sleep(1000);
            shutdownService();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void testCase02start() {
        Intent intent = new Intent(DreamService.SERVICE_INTERFACE);
        intent.setClass(mContext, Colors.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startService(intent);
        try {
            Thread.sleep(1000);
            Colors colorsService = getService();
            assertNotNull(colorsService);
            assertNotNull(ReflectionHelper.getNonPublicField(Colors.class,
                    "mTextureView"));
            shutdownService();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

    }

}
