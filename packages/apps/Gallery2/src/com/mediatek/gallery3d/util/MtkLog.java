package com.mediatek.gallery3d.util;

import android.os.Environment;

import com.mediatek.xlog.Xlog;

import java.io.File;

/**
 * Adapter for log system.
 */
public final class MtkLog {
    
    // on/off switch to control large amount of logs
    public static final boolean DBG;
    public static final boolean SUPPORT_PQ;
    public static final boolean SUPPORT_PQ_ADV;
    public static final boolean DBG_TILE;
    public static final boolean DBG_PERFORMANCE;

    static {
        File cfg = new File(Environment.getExternalStorageDirectory(), "DEBUG_GALLERY2");
        if (cfg.exists()) {
            DBG = true;
        } else {
            DBG = false;
        }
        cfg = new File(Environment.getExternalStorageDirectory(), "DEBUG_TILE");
        if (cfg.exists()) {
            DBG_TILE = true;
        } else {
            DBG_TILE = false;
        }
        Xlog.v("MtkLog", "Gallery2 debug mode " + (DBG ? "ON" : "OFF"));
        File pqCfg = new File(Environment.getExternalStorageDirectory(), "SUPPORT_PQ");
        if (pqCfg.exists()) {
            SUPPORT_PQ = true;
        } else {
            SUPPORT_PQ = false;
        }
        File pqADVModeCfg = new File(Environment.getExternalStorageDirectory(), "SUPPORT_PQ_ADV");
        if (pqADVModeCfg.exists()) {
            SUPPORT_PQ_ADV = true;
        } else {
            SUPPORT_PQ_ADV = false;
        }
        Xlog.v("MtkLog", "Gallery2 support PQ " + (SUPPORT_PQ ? "ON" : "OFF") +" Gallery2 support PQ ADV :" + (SUPPORT_PQ_ADV ? "ON" : "OFF"));
        File PMCfg = new File(Environment.getExternalStorageDirectory(), "DEBUG_PERFORMANCE");
        if (PMCfg.exists()) {
            DBG_PERFORMANCE = true;
        } else {
            DBG_PERFORMANCE = false;
        }
        Xlog.v("MtkLog", "Gallery2 debug performance " + (DBG_PERFORMANCE ? "ON" : "OFF"));
    }
    
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
    
    public static boolean isDebugTile() {
        return DBG_TILE;
    }
    
}
