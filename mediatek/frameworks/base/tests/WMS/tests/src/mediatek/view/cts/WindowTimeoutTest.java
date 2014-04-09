package com.mediatek.cts.window;

import com.mediatek.cts.window.stub.R;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Surface;
import android.view.Window;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

@TestTargetClass(Window.class)
public class WindowTimeoutTest extends ActivityInstrumentationTestCase2<WindowTimeoutStubActivity> {
    private final static String TAG = "WindowTimeoutTest";
    private WindowTimeoutStubActivity mActivity;
    private ContentResolver mResolver;
    private int APP_TRANSITION_TIMEOUT = 5000;
    private int APP_FREEZE_TIMEOUT = 5000;
    private int BUFFER_TIME = 1000;
    private Object mLock = new Object();
    private Handler mH = new H();


    public WindowTimeoutTest() {
        super("com.android.cts.stub", WindowTimeoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();
        // Initialize the orientation to portrait
        Settings.System.putInt(mResolver, Settings.System.USER_ROTATION, Surface.ROTATION_0);
        Log.d(TAG, "setup");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Log.d(TAG, "tearDown");
    }

    // Test if the debug mechanism for app transition timeout has been checked in
    public void testWindowTimeoutForAppTransition() throws Exception {
        Log.d(TAG, "testWindowTimeoutForAppTransition begin");
        new ProcessBuilder("/system/bin/am", "start", "-n", "com.android.launcher/com.android.launcher2.Launcher").start();
        Thread.sleep(BUFFER_TIME);
        mActivity.mStartTesting = true;
        new ProcessBuilder("/system/bin/am", "start", "-n", "com.mediatek.cts.window.stub/com.mediatek.cts.window.WindowTimeoutStubActivity").start();
        Thread.sleep(APP_TRANSITION_TIMEOUT + BUFFER_TIME);
        String[] pattern = { "*** APP TRANSITION TIMEOUT", "MSG HISTORY" };
        assertTrue(canParseLogPattern(pattern));
        mActivity.mStartTesting = false;
        Log.d(TAG, "testAppTransitionTimeout end");
    }

    // Test if the debug mechanism for app freeze timeout has been checked in
    public void testWindowTimeoutForAppFreeze() throws Exception {
        Log.d(TAG, "testAppFreezeTimeout begin");
        mActivity.mStartTesting = true;
        Thread.sleep(BUFFER_TIME);
        // Set the rotation degree and change the ACCELEROMETER_ROTATION settings to trigger rotation
        Settings.System.putInt(mResolver, Settings.System.USER_ROTATION, Surface.ROTATION_90);
        Settings.System.putInt(mResolver, Settings.System.ACCELEROMETER_ROTATION, 0);

        Thread.sleep(APP_FREEZE_TIMEOUT + BUFFER_TIME);
        String[] pattern = { "App freeze timeout expired.", "MSG HISTORY" };
        assertTrue(canParseLogPattern(pattern));

        // Reset the rotation settings
        Settings.System.putInt(mResolver, Settings.System.USER_ROTATION, Surface.ROTATION_0);
        Settings.System.putInt(mResolver, Settings.System.ACCELEROMETER_ROTATION, 1);
        mActivity.mStartTesting = false;
        Log.d(TAG, "testAppFreezeTimeout end");
    }

    // Check if the solution patch for JB MR1 Google issue (Wrong window order) has been checked in
    public void testWindowOrderAdjustment() throws Exception {
        Log.d(TAG, "testWindowOrderAdjustment begin");
        synchronized(mLock) {
            new ProcessBuilder("/system/bin/input", "touchscreen", "tap", "265", "139").start();
            mH.sendMessageDelayed(mH.obtainMessage(H.DO_DELAY_NOTIFY), 2000);
            mLock.wait();
            new ProcessBuilder("/system/bin/input", "touchscreen", "tap", "265", "222").start();
            mH.sendMessageDelayed(mH.obtainMessage(H.DO_DELAY_NOTIFY), 2000);
            mLock.wait();
            assertTrue(checkWindowOrderInToken());
        }
        Thread.sleep(BUFFER_TIME);
        Log.d(TAG, "testWindowOrderAdjustment end");
    }

    final class H extends Handler {
        public static final int DO_DELAY_NOTIFY = 1;
        public H(){}

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DO_DELAY_NOTIFY: {
                    synchronized(mLock) {
                        mLock.notify();
                    }
                }break;
            }
        }
    }

    //  The log pattern should be as following
    //  ************************************************************************************************************************************************
    //  Application tokens in Z order:
    //  App #11 AppWindowToken{44137878 token=Token{43ce1148 ActivityRecord{44136ba0 u0 com.mediatek.cts.window.stub/com.mediatek.cts.window.WindowTimeo
    //    windows=[Window{43ae8238 u0 ProgressDialog}, Window{439a8530 u0 com.mediatek.cts.window.stub/com.mediatek.cts.window.WindowTimeoutStubActivity}]
    //  ************************************************************************************************************************************************
    //  The implementation is to parse the "windows" content and identify if the order is right, WindowTimeoutStubActivity window should not be moved.
    private boolean checkWindowOrderInToken() {
        boolean isSuccessful = false;
        try {
            java.lang.Process dumpsys = new ProcessBuilder("/system/bin/dumpsys", "window", "t").redirectErrorStream(true).start();
            InputStreamReader isr = new InputStreamReader(dumpsys.getInputStream());
            BufferedReader buf = new BufferedReader(isr);
            String s;
            Log.d(TAG, "start reading & parsing");
            while ((s = buf.readLine()) != null) {
                if (s.contains("Application tokens in Z order:")) {
                    Log.d(TAG, "Catch!! ");
                    Log.d(TAG, buf.readLine());
                    String target = buf.readLine();
                    String[] windowlist = target.split("=");
                    String[] tokens = windowlist[1].split(",");
                    Log.d(TAG, tokens[0]);
                    Log.d(TAG, tokens[1]);
                    if (tokens[0].contains("WindowTimeoutStubActivity"))
                        isSuccessful = true;
                }
            }
            Log.d(TAG, "finish parsing");
            isr.close();
        } catch (Exception e) {
        }
        return isSuccessful;
    }

    private boolean canParseLogPattern(String[] pattern) {
        boolean isSuccessful = false;
        try {
            int lines = 1024;
            java.lang.Process logcat = new ProcessBuilder("/system/bin/logcat", "-v", "time", "-t", String.valueOf(lines)).redirectErrorStream(true).start();
            InputStreamReader isr = new InputStreamReader(logcat.getInputStream());
            BufferedReader buf = new BufferedReader(isr);
            String s;
            int checkTimes = 0;
            Log.d(TAG, "start reading & parsing");
            while ((s = buf.readLine()) != null) {
                if (s.contains(pattern[checkTimes])) {
                    Log.d(TAG, "Successfully parsing the "+ pattern[checkTimes] +" pattern!!");
                    Log.d(TAG, s);
                    checkTimes++;
                    if (checkTimes == pattern.length) {
                        isSuccessful = true;
                        break;
                    }
                }
            }
            Log.d(TAG, "finish parsing");
            isr.close();
        } catch (Exception e) {
        }
        return isSuccessful;
    }
}
