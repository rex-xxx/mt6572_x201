package com.android.launcher2;

import com.mediatek.xlog.Xlog;

public final class LauncherLog {
    static final boolean DEBUG = true;
    static final boolean DEBUG_DRAW = false;
    static final boolean DEBUG_DRAG = false;
    static final boolean DEBUG_KEY = true;
    static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_LOADER = true;
    static final boolean DEBUG_MOTION = false;
    static final boolean DEBUG_PERFORMANCE = true;
    static final boolean DEBUG_SURFACEWIDGET = true;
    static final boolean DEBUG_UNREAD = false;
    public static final boolean DEBUG_AUTOTESTCASE = true;

    private static final String MODULE_NAME = "Launcher";
    private static final LauncherLog INSTANCE = new LauncherLog();

    /**
     * private constructor here, It is a singleton class.
     */
    private LauncherLog() {
    }

    /**
     * The FileManagerLog is a singleton class, this static method can be used
     * to obtain the unique instance of this class.
     * 
     * @return The global unique instance of FileManagerLog.
     */
    public static LauncherLog getInstance() {
        return INSTANCE;
    }

    /**
     * The method prints the log, level error.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void e(String tag, String msg) {
        Xlog.e(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level error.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void e(String tag, String msg, Throwable t) {
        Xlog.e(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level warning.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void w(String tag, String msg) {
        Xlog.w(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level warning.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void w(String tag, String msg, Throwable t) {
        Xlog.w(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void i(String tag, String msg) {
        Xlog.i(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void i(String tag, String msg, Throwable t) {
        Xlog.i(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void d(String tag, String msg) {
        Xlog.d(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t An exception to log.
     */
    public static void d(String tag, String msg, Throwable t) {
        Xlog.d(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void v(String tag, String msg) {
        Xlog.v(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t An exception to log.
     */
    public static void v(String tag, String msg, Throwable t) {
        Xlog.v(MODULE_NAME, tag + ", " + msg, t);
    }
}
