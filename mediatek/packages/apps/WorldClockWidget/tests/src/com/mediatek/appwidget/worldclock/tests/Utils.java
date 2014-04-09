package com.mediatek.appwidget.worldclock.tests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
import android.util.Log;

public class Utils {
    private final static String TAG = "WorldClockWidgetTest";

    public static Object getFiledValue(Class<?> cls, Activity instance,
            String filed) {
        try {
            Field field = cls.getDeclaredField(filed);
            field.setAccessible(true);
            Object result = field.get(instance);
            Log.v(TAG, "getFiledValue filed = " + filed + ", result = "
                    + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void invoke(Class<?> cls, Activity instance, String func,
            String param) {
        try {
            // Class c = Class.forName(s);
            Class params[] = new Class[1];
            params[0] = Class.forName("java.lang.String");
            Method m = cls.getMethod(func, params);
            Object args[] = new Object[1];
            if (param != null) {
                args[0] = param;
            }
            m.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}