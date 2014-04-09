/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.beam;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.mediatek.beam.BeamShareTask.BeamShareTaskMetaData;
import com.mediatek.xlog.Xlog;

import java.util.HashMap;

public class BeamShareProvider extends ContentProvider {

    private static final String TAG = "BeamShareProvider";

    public static final String AUTHORITY = "com.android.settings.provider.beam.share";

    public static final String DATABASE_NAME = "share.db";

    public static final int DATABASE_VERSION = 1;

    // Column projection map
    private static HashMap<String, String> sProjectionMap = new HashMap<String, String>();
    static {
        sProjectionMap
                .put(BeamShareTaskMetaData._ID, BeamShareTaskMetaData._ID);
        sProjectionMap.put(BeamShareTaskMetaData.TASK_TYPE,
                BeamShareTaskMetaData.TASK_TYPE);
        sProjectionMap.put(BeamShareTaskMetaData.TASK_STATE,
                BeamShareTaskMetaData.TASK_STATE);

        sProjectionMap.put(BeamShareTaskMetaData.TASK_OBJECT_FILE,
                BeamShareTaskMetaData.TASK_OBJECT_FILE);
        sProjectionMap.put(BeamShareTaskMetaData.TASK_MIMETYPE,
                BeamShareTaskMetaData.TASK_MIMETYPE);

        sProjectionMap.put(BeamShareTaskMetaData.TASK_TOTAL_BYTES,
                BeamShareTaskMetaData.TASK_TOTAL_BYTES);
        sProjectionMap.put(BeamShareTaskMetaData.TASK_DONE_BYTES,
                BeamShareTaskMetaData.TASK_DONE_BYTES);

        sProjectionMap.put(BeamShareTaskMetaData.TASK_MODIFIED_DATE,
                BeamShareTaskMetaData.TASK_MODIFIED_DATE);
    }

    // Uri Matcher
    private static final UriMatcher URI_MATCHER;

    private static final int MULTIPLE_TASK_URI = 1;

    private static final int SINGLE_TASK_URI = 2;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BeamShareTaskMetaData.TABLE_NAME,
                MULTIPLE_TASK_URI);
        URI_MATCHER.addURI(AUTHORITY, BeamShareTaskMetaData.TABLE_NAME + "/#",
                SINGLE_TASK_URI);
    }

    private DatabaseHelper mDbHelper;

    private SQLiteDatabase mDb;

    @Override
    public boolean onCreate() {

        mDbHelper = new DatabaseHelper(getContext());
        mDb = mDbHelper.getWritableDatabase();
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        checkWritePermissions();

        mDb = mDbHelper.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(uri)) {
        case MULTIPLE_TASK_URI:
            count = mDb.delete(BeamShareTaskMetaData.TABLE_NAME, selection,
                    selectionArgs);
            break;
        case SINGLE_TASK_URI:
            String rowId = uri.getPathSegments().get(1);
            count = mDb.delete(
                    BeamShareTaskMetaData.TABLE_NAME,
                    BeamShareTaskMetaData._ID
                            + "="
                            + rowId
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // getContext().getContentResolver().notifyChange(uri, null);
        sendNotify(uri);
        return count;
    }

    @Override
    public String getType(Uri uri) {

        switch (URI_MATCHER.match(uri)) {
        case MULTIPLE_TASK_URI:
            return BeamShareTaskMetaData.CONTENT_TYPE;
        case SINGLE_TASK_URI:
            return BeamShareTaskMetaData.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Make sure the caller has permission to write this data.
     */
    private void checkWritePermissions() {
        if (getContext().checkCallingOrSelfPermission(
                android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    String.format(
                            "Permission denial: writing to secure settings requires %1$s",
                            android.Manifest.permission.WRITE_SECURE_SETTINGS));
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {

        if (URI_MATCHER.match(url) != MULTIPLE_TASK_URI) {
            throw new IllegalArgumentException("Invalid URI: " + url);
        }

        checkWritePermissions();

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final long rowId = db.insert(BeamShareTaskMetaData.TABLE_NAME, null,
                initialValues);
        if (rowId <= 0) {
            return null;
        }

        Xlog.v(TAG, BeamShareTaskMetaData.TABLE_NAME + " <- " + initialValues);
        url = ContentUris.withAppendedId(BeamShareTaskMetaData.CONTENT_URI,
                rowId);
        getContext().getContentResolver().notifyChange(BeamShareTaskMetaData.CONTENT_URI, null);
        sendNotify(url);
        return url;
    }

    /**
     * Send a notification when a particular content URI changes. 
     * @param uri
     *            to send notifications for
     */
    private void sendNotify(Uri uri) {
        // Now send the notification through the content framework.
        String notify = uri.getQueryParameter("notify");
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
            Xlog.v(TAG, "notifying: " + uri);
        } else {
            Xlog.v(TAG, "notification suppressed: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        mDb = mDbHelper.getReadableDatabase();
        try {
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(BeamShareTaskMetaData.TABLE_NAME);
            qb.setProjectionMap(sProjectionMap);
            if (URI_MATCHER.match(uri) == SINGLE_TASK_URI) {
                qb.appendWhere(BeamShareTaskMetaData._ID + "="
                        + uri.getPathSegments().get(1));
            }

            String orderBy = sortOrder;
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = BeamShareTaskMetaData.DEFAULT_SORT_ORDER;
            }

            Cursor c = qb.query(mDb, projection, selection, selectionArgs,
                    null, null, orderBy);
            if (c != null) {
                c.setNotificationUri(getContext().getContentResolver(), uri);
            }
            return c;
        } catch (SQLiteDiskIOException e) {
            Xlog.e(TAG, e.toString());
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {

        checkWritePermissions();

        // update database
        mDb = mDbHelper.getWritableDatabase();

        int count;
        switch (URI_MATCHER.match(uri)) {
        case MULTIPLE_TASK_URI:
            count = mDb.update(BeamShareTaskMetaData.TABLE_NAME, values,
                    selection, selectionArgs);
            break;
        case SINGLE_TASK_URI:
            String rowId = uri.getPathSegments().get(1);
            count = mDb.update(
                    BeamShareTaskMetaData.TABLE_NAME,
                    values,
                    BeamShareTaskMetaData._ID
                            + "="
                            + rowId
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // getContext().getContentResolver().notifyChange(uri, null);
        sendNotify(uri);
        return count;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {

            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE " + BeamShareTaskMetaData.TABLE_NAME
                    + " (" + BeamShareTaskMetaData._ID
                    + " INTEGER PRIMARY KEY," + BeamShareTaskMetaData.TASK_TYPE
                    + " INTEGER," + BeamShareTaskMetaData.TASK_STATE
                    + " INTEGER," + BeamShareTaskMetaData.TASK_OBJECT_FILE
                    + " TEXT," + BeamShareTaskMetaData.TASK_MIMETYPE + " TEXT,"
                    + BeamShareTaskMetaData.TASK_TOTAL_BYTES + " INTEGER,"
                    + BeamShareTaskMetaData.TASK_DONE_BYTES + " INTEGER,"
                    + BeamShareTaskMetaData.TASK_MODIFIED_DATE + " INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Xlog.d(TAG, "Upgrading database from version " + oldVersion
                    + " to " + newVersion + " (will destroy all old data)!");
            db.execSQL("DROP TABLE IF EXISTS "
                    + BeamShareTaskMetaData.TABLE_NAME);
            onCreate(db);
        }
    }
}
