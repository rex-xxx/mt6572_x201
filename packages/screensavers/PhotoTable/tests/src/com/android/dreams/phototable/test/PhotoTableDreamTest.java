package com.android.dreams.phototable.test;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamService;
import android.test.ServiceTestCase;
import android.util.Log;

import com.android.dreams.phototable.PhotoTableDream;

public class PhotoTableDreamTest extends ServiceTestCase<PhotoTableDream> {

    private static final String TAG = PhotoTableDreamTest.class.getSimpleName();
    private Context mContext;

    public PhotoTableDreamTest() {
        super(PhotoTableDream.class);
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
        intent.setClass(mContext, PhotoTableDream.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            IDreamService binder = IDreamService.Stub
                    .asInterface(bindService(intent));
            assertNotNull(binder);
            PhotoTableDream tableService = getService();
            assertNotNull(tableService);
            Binder token = new Binder();
            binder.attach(token);
            Thread.sleep(500);
            tableService.onDreamingStarted();
            Thread.sleep(500);
            tableService.onDreamingStopped();
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
        intent.setClass(mContext, PhotoTableDream.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startService(intent);
        try {
            Thread.sleep(500);
            PhotoTableDream tableService = getService();
            assertNotNull(tableService);
            shutdownService();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

    }

}
