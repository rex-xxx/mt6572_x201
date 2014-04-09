package com.android.dreams.phototable.test;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamService;
import android.test.ServiceTestCase;
import android.util.Log;

import com.android.dreams.phototable.FlipperDream;

public class FlipperDreamTest extends ServiceTestCase<FlipperDream> {

    private static final String TAG = FlipperDreamTest.class.getSimpleName();
    private Context mContext;

    public FlipperDreamTest() {
        super(FlipperDream.class);
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
        intent.setClass(mContext, FlipperDream.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            IDreamService binder = IDreamService.Stub
                    .asInterface(bindService(intent));
            assertNotNull(binder);
            FlipperDream flipService = getService();
            assertNotNull(flipService);
            Binder token = new Binder();
            binder.attach(token);
            Thread.sleep(500);
            flipService.onDreamingStarted();
            Thread.sleep(500);
            flipService.onDreamingStopped();
            Thread.sleep(500);
            binder.detach();
            Thread.sleep(500);
            shutdownService();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void testCase02start() {
        Intent intent = new Intent(DreamService.SERVICE_INTERFACE);
        intent.setClass(mContext, FlipperDream.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startService(intent);
        try {
            Thread.sleep(500);
            FlipperDream flipService = getService();
            assertNotNull(flipService);
            shutdownService();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

    }

}
