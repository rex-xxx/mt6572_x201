package com.mediatek.contacts.util;

import android.util.Log;
import com.mediatek.xlog.Xlog;

public final class LogUtils {

    private static StringBuilder sBuilder = new StringBuilder();
    private static final int INIT_STACK_DEEPTH = 4;
    
    private static void printStack(String tag, String msg, int deepth, int initStackDeepth) {
        final int firstStack = Math.max(initStackDeepth, INIT_STACK_DEEPTH);
        sBuilder.setLength(0);
        sBuilder.append(msg).append(':');
        for (int i = 0; i < deepth; i++) {
            StackTraceElement traceElement = Thread.currentThread().getStackTrace()[firstStack + i];
            sBuilder.append("\n\t")
                    .append(shortenClassName(traceElement.getClassName()))
                    .append(':')
                    .append(traceElement.getMethodName() + "()" )
                    .append('#')
                    .append(traceElement.getLineNumber());
        }
        d(tag, sBuilder.toString());
    }
    
    public static void printCaller(String tag) {
        printStack(tag, "Caller", 1, INIT_STACK_DEEPTH + 1);
    }
    
    private static String shortenClassName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public static void e(String tag, String msg) {
        Xlog.e(tag, msg);
    }

    public static void d(String tag, String msg) {
        Xlog.d(tag, msg);
    }
    
    public static void w(String tag, String msg) {
        Xlog.w(tag, msg);
    }
    
    public static void trace(String tag, String msg) {
        final int firstStack = INIT_STACK_DEEPTH - 1;
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[firstStack];
        sBuilder.setLength(0);
        sBuilder.append("[").append(traceElement.getMethodName()).append("] ").append(msg);
        Xlog.d(tag, sBuilder.toString());
    }
}
