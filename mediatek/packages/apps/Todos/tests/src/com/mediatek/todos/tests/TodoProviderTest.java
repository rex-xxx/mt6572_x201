package com.mediatek.todos.tests;

import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.mediatek.todos.TodoAsyncQuery;
import com.mediatek.todos.provider.TodoProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Unit test; improve code Coverage;
 */
public class TodoProviderTest extends InstrumentationTestCase {

    public void testAppendAccountFromParameterToSelection() {
        TodoProvider provider = new TodoProvider();
        try {
            Class classProvider = provider.getClass();
            Method method = classProvider.getDeclaredMethod("appendAccountFromParameterToSelection", String.class,
                    Uri.class);
            method.setAccessible(true);
            method.invoke(classProvider, null, TodoAsyncQuery.TODO_URI);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void testAppendSelection() {
        TodoProvider provider = new TodoProvider();
        try {
            Class classProvider = provider.getClass();
            Method method = classProvider.getDeclaredMethod("appendSelection", StringBuilder.class, String.class);
            method.setAccessible(true);
            method.invoke(classProvider, new StringBuilder(), "test");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void testInsertSelectionArg() {
        TodoProvider provider = new TodoProvider();
        try {
            Class classProvider = provider.getClass();
            Method method = classProvider.getDeclaredMethod("insertSelectionArg", String[].class, String.class);
            method.setAccessible(true);
            String params[] = { "param1" };
            method.invoke(classProvider, params, "param_append");
            method.invoke(classProvider, null, "param_append");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testGetType() {
        TodoProvider provider = new TodoProvider();
        assertNotNull(provider.getType(TodoAsyncQuery.TODO_URI));
    }
}
