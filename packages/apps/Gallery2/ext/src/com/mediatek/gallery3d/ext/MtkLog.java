package com.mediatek.gallery3d.ext;

import com.mediatek.xlog.Xlog;

/**
 * Adapter for log system.
 */
public final class MtkLog {
    private MtkLog() {
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