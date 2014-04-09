package com.android.providers.media;

import com.mediatek.xlog.Xlog;

/**
 * Adapter for Mediatek log system.
 */
public final class MtkLog {
    private static final boolean LOCAL_LOGV = true;
    private static final boolean LOCAL_LOGD = true;
    private static final boolean LOCAL_LOGI = true;
    private static final int DEFAULT = 0;
    
    private MtkLog() {
    }

    /**
     * Sends a {@link LOG#VERBOSE} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @return
     */
    public static int v(String tag, String msg) {
        if (LOCAL_LOGV) {
            return Xlog.v(tag, msg);
        } else {
            return DEFAULT;
        }
    }

    /**
     * Sends a {@link LOG#VERBOSE} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @param tr the exception to be logged.
     * @return
     */
    public static int v(String tag, String msg, Throwable tr) {
        if (LOCAL_LOGV) {
            return Xlog.v(tag, msg, tr);
        } else {
            return DEFAULT;
        }
    }

    /**
     * Sends a {@link LOG#DEBUG} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @return
     */
    public static int d(String tag, String msg) {
        if (LOCAL_LOGD) {
            return Xlog.d(tag, msg);
        } else {
            return DEFAULT;
        }
    }

    /**
     * Sends a {@link LOG#DEBUG} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @param tr the exception to be logged.
     * @return
     */
    public static int d(String tag, String msg, Throwable tr) {
        if (LOCAL_LOGD) {
            return Xlog.d(tag, msg, tr);
        } else {
            return DEFAULT;
        }
    }

    /**
     * Sends a {@link LOG#INFO} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @return
     */
    public static int i(String tag, String msg) {
        if (LOCAL_LOGI) {
            return Xlog.i(tag, msg);
        } else {
            return DEFAULT;
        }
    }

    /**
     * Sends a {@link LOG#INFO} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @param tr the exception to be logged.
     * @return
     */
    public static int i(String tag, String msg, Throwable tr) {
        if (LOCAL_LOGI) {
            return Xlog.i(tag, msg, tr);
        } else {
            return DEFAULT;
        }
    }

    /**
     * Sends a {@link LOG#WARN} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @return
     */
    public static int w(String tag, String msg) {
        return Xlog.w(tag, msg);
    }

    /**
     * Sends a {@link LOG#WARN} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @param tr the exception to be logged.
     * @return
     */
    public static int w(String tag, String msg, Throwable tr) {
        return Xlog.w(tag, msg, tr);
    }

    /**
     * Sends a {@link LOG#ERROR} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @return
     */
    public static int e(String tag, String msg) {
        return Xlog.e(tag, msg);
    }

    /**
     * Sends a {@link LOG#ERROR} log message.
     * 
     * @param tag the tag used to identify the source of a log message.
     * @param msg the message to be logged.
     * @param tr the exception to be logged.
     * @return
     */
    public static int e(String tag, String msg, Throwable tr) {
        return Xlog.e(tag, msg, tr);
    }
}