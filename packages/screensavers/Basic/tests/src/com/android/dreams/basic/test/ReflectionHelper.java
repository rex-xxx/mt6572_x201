package com.android.dreams.basic.test;

import java.lang.reflect.Field;

public class ReflectionHelper {

    public static Class getNonPublicInnerClass(Class targetCls, String innerClsName) {
        Class innerCls = null;
        Class[] innerClasses = targetCls.getDeclaredClasses();
        for (Class cls : innerClasses) {
            if (cls.toString().contains(innerClsName)) {
                innerCls = cls;
                break;
            }
        }
        return innerCls;
    }

    public static Field getNonPublicField(Class cls, String fieldName) {
        Field field = null;
        try {
            field = cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return field;
    }

    public static Object getObjectValue(Field field, Object targetObject) {
        field.setAccessible(true);
        Object result = null;
        try {
            result = field.get(targetObject);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean getBooleanValue(Field field, Object tarObject) {
        field.setAccessible(true);
        boolean result = false;
        try {
            result = field.getBoolean(tarObject);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Object getObjectValue(Class cls, String fieldName, Object targetObject) {
        Field field = getNonPublicField(cls, fieldName);
        return getObjectValue(field, targetObject);
    }

    public static boolean getBooleanValue(Class cls, String fieldName, Object tarObject) {
        Field field = getNonPublicField(cls, fieldName);
        return getBooleanValue(field, tarObject);
    }

}
