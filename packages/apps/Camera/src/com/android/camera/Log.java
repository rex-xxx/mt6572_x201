package com.android.camera;

import android.os.Environment;
import android.os.Build;

import com.mediatek.xlog.Xlog;

import java.io.File;

public class Log {
    private static final String TAG = "Camera_Debug_V";
    private static final String DEBUG_FILE_V = "/Camera_Debug_V_off";
    
    public static final boolean LOGV = isLogV();
    
    private static boolean isLogV() {
        boolean debug = Build.IS_DEBUGGABLE;
        boolean log = android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
        //boolean file = new File(Environment.getExternalStorageDirectory() + DEBUG_FILE_V).exists();
        android.util.Log.i(TAG, "isLogV() debug=" + debug + ", log=" + log);// + ", file=" + file);
        return  log || debug;// M: this statement should be deleted before MP.
    }
    
    private Log() {
    }
    
    public static int v(String tag, String msg) {
        return Xlog.v(tag, msg);
    }
    public static int v(String tag, String msg, Throwable tr) {
        return Xlog.v(tag, msg, tr);
    }
    public static int d(String tag, String msg) {
        return Xlog.d(tag, msg);
    }
    public static int d(String tag, String msg, Throwable tr) {
        return Xlog.d(tag, msg, tr);
    }
    public static int i(String tag, String msg) {
        return Xlog.i(tag, msg);
    }
    public static int i(String tag, String msg, Throwable tr) {
        return Xlog.i(tag, msg, tr);
    }
    public static int w(String tag, String msg) {
        return Xlog.w(tag, msg);
    }
    public static int w(String tag, String msg, Throwable tr) {
        return Xlog.w(tag, msg, tr);
    }
    public static int e(String tag, String msg) {
        return Xlog.e(tag, msg);
    }
    public static int e(String tag, String msg, Throwable tr) {
        return Xlog.e(tag, msg, tr);
    }
}
