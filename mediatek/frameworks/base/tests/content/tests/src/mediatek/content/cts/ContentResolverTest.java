/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mediatek.content.cts;



import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;
import android.content.ContentProviderClient;
import android.content.SyncInfo;
import android.content.PeriodicSync;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.RemoteException;
import android.content.OperationApplicationException;
import java.util.ArrayList;

@TestTargetClass(ContentResolver.class)
public class ContentResolverTest extends AndroidTestCase {
    private final static String COLUMN_ID_NAME = "_id";
    private final static String COLUMN_KEY_NAME = "key";
    private final static String COLUMN_VALUE_NAME = "value";

    private static final String AUTHORITY = "mediatekcontentctstest";
    private static final Uri TABLE1_URI = Uri.parse("content://" + AUTHORITY + "/testtable1/");
    private static final Uri TABLE2_URI = Uri.parse("content://" + AUTHORITY + "/testtable2/");

    private static final Account ACCOUNT = new Account("cts", "cts");

    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String KEY3 = "key3";
    private static final int VALUE1 = 1;
    private static final int VALUE2 = 2;
    private static final int VALUE3 = 3;

    private static final String TEST_PACKAGE_NAME = "com.mediatek.test.stub";

    private Context mContext;
    private ContentResolver mContentResolver;
    private Cursor mCursor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();
        mContentResolver = mContext.getContentResolver();

        // add three rows to database when every test case start.
        ContentValues values = new ContentValues();

        values.put(COLUMN_KEY_NAME, KEY1);
        values.put(COLUMN_VALUE_NAME, VALUE1);
        mContentResolver.insert(TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY2);
        values.put(COLUMN_VALUE_NAME, VALUE2);
        mContentResolver.insert(TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY3);
        values.put(COLUMN_VALUE_NAME, VALUE3);
        mContentResolver.insert(TABLE1_URI, values);
    }

    @Override
    protected void tearDown() throws Exception {
        mContentResolver.delete(TABLE1_URI, null, null);
        if ( null != mCursor && !mCursor.isClosed() ) {
            mCursor.close();
        }
        super.tearDown();
    }

    public void testAddToQueryHistory() {
         
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "ContentResolver",
        args = {android.content.Context.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete")
    public void testConstructor() {
        assertNotNull(mContentResolver);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getType",
        args = {android.net.Uri.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "should add @throws clause into javadoc of " +
            "ContentResolver#getType(Uri) when the input Uri is null")
    public void testGetType() {
        String type1 = mContentResolver.getType(TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        String type2 = mContentResolver.getType(TABLE2_URI);
        assertTrue(type2.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        Uri invalidUri = Uri.parse("abc");
        assertNull(mContentResolver.getType(invalidUri));

        try {
            mContentResolver.getType(null);
            fail("did not throw NullPointerException when Uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "query",
        args = {android.net.Uri.class, java.lang.String[].class, java.lang.String.class,
                java.lang.String[].class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "should add @throws clause into javadoc of " +
            "ContentResolver#query(Uri, String[], String, String[], String) when the input " +
            "param Uri is null")
    public void testQuery() {
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);

        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToPrevious();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY2, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        String selection = COLUMN_ID_NAME + "=1";
        mCursor = mContentResolver.query(TABLE1_URI, null, selection, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY1, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        selection = COLUMN_KEY_NAME + "=\"" + KEY3 + "\"";
        mCursor = mContentResolver.query(TABLE1_URI, null, selection, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToFirst();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.query(null, null, null, null, null);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }


 

  

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "insert",
        args = {android.net.Uri.class, android.content.ContentValues.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "should add @throws clause into javadoc of " +
            "ContentResolver#insert(Uri, ContentValues) when the input Uri are null")
    public void testInsert() {
        String key4 = "key4";
        String key5 = "key5";
        int value4 = 4;
        int value5 = 5;
        String key4Selection = COLUMN_KEY_NAME + "=\"" + key4 + "\"";

        mCursor = mContentResolver.query(TABLE1_URI, null, key4Selection, null, null);
        assertEquals(0, mCursor.getCount());
        mCursor.close();

        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY_NAME, key4);
        values.put(COLUMN_VALUE_NAME, value4);
        Uri uri = mContentResolver.insert(TABLE1_URI, values);
        assertNotNull(uri);

        mCursor = mContentResolver.query(TABLE1_URI, null, key4Selection, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key4, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        values.put(COLUMN_KEY_NAME, key5);
        values.put(COLUMN_VALUE_NAME, value5);
        uri = mContentResolver.insert(TABLE1_URI, values);
        assertNotNull(uri);

        // check returned uri
        mCursor = mContentResolver.query(uri, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());

        mCursor.moveToLast();
        assertEquals(5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key5, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.insert(null, values);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "bulkInsert",
        args = {android.net.Uri.class, android.content.ContentValues[].class}
    )
    @ToBeFixed(bug = "1695243", explanation = "should add @throws clause into javadoc of " +
            "ContentResolver#bulkInsert(Uri, ContentValues[]) when the input Uri are null")
    public void testBulkInsert() {
        String key4 = "key4";
        String key5 = "key5";
        int value4 = 4;
        int value5 = 5;

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        mCursor.close();

        ContentValues[] cvs = new ContentValues[2];
        cvs[0] = new ContentValues();
        cvs[0].put(COLUMN_KEY_NAME, key4);
        cvs[0].put(COLUMN_VALUE_NAME, value4);

        cvs[1] = new ContentValues();
        cvs[1].put(COLUMN_KEY_NAME, key5);
        cvs[1].put(COLUMN_VALUE_NAME, value5);

        assertEquals(2, mContentResolver.bulkInsert(TABLE1_URI, cvs));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(5, mCursor.getCount());

        mCursor.moveToLast();
        assertEquals(5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key5, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToPrevious();
        assertEquals(4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key4, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.bulkInsert(null, cvs);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "delete",
        args = {android.net.Uri.class, java.lang.String.class, java.lang.String[].class}
    )
    @ToBeFixed(bug = "1695243", explanation = "should add @throws clause into javadoc of " +
            "ContentResolver#delete(Uri, String, String[]) when the input Uri are null")
    public void testDelete() {
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        mCursor.close();

        assertEquals(3, mContentResolver.delete(TABLE1_URI, null, null));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(0, mCursor.getCount());
        mCursor.close();

        // add three rows to database.
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY_NAME, KEY1);
        values.put(COLUMN_VALUE_NAME, VALUE1);
        mContentResolver.insert(TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY2);
        values.put(COLUMN_VALUE_NAME, VALUE2);
        mContentResolver.insert(TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY3);
        values.put(COLUMN_VALUE_NAME, VALUE3);
        mContentResolver.insert(TABLE1_URI, values);

        // test delete row using selection
        String selection = COLUMN_ID_NAME + "=2";
        assertEquals(1, mContentResolver.delete(TABLE1_URI, selection, null));

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(2, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY1, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToNext();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        selection = COLUMN_VALUE_NAME + "=3";
        assertEquals(1, mContentResolver.delete(TABLE1_URI, selection, null));

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY1, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        selection = COLUMN_KEY_NAME + "=\"" + KEY1 + "\"";
        assertEquals(1, mContentResolver.delete(TABLE1_URI, selection, null));

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(0, mCursor.getCount());
        mCursor.close();

        try {
            mContentResolver.delete(null, null, null);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "update",
        args = {android.net.Uri.class, android.content.ContentValues.class,
                java.lang.String.class, java.lang.String[].class}
    )
    @ToBeFixed(bug = "1695243", explanation = "The javadoc says \"return the URL of the " +
            "newly created row\", but actually it return an integer.")
    public void testUpdate() {
        ContentValues values = new ContentValues();
        String key10 = "key10";
        String key20 = "key20";
        int value10 = 10;
        int value20 = 20;

        values.put(COLUMN_KEY_NAME, key10);
        values.put(COLUMN_VALUE_NAME, value10);

        // test update all the rows.
        assertEquals(3, mContentResolver.update(TABLE1_URI, values, null, null));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToNext();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        // test update one row using selection.
        String selection = COLUMN_ID_NAME + "=1";
        values.put(COLUMN_KEY_NAME, key20);
        values.put(COLUMN_VALUE_NAME, value20);

        assertEquals(1, mContentResolver.update(TABLE1_URI, values, selection, null));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key20, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value20, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToNext();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.update(null, values, null, null);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }

        // javadoc says it will throw NullPointerException when values are null,
        // but actually, it throws IllegalArgumentException here.
        try {
            mContentResolver.update(TABLE1_URI, null, null, null);
            fail("did not throw IllegalArgumentException when values are null.");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

}
