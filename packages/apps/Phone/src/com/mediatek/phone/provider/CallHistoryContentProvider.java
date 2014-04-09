/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License
 */

package com.mediatek.phone.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.mediatek.phone.provider.CallHistory.Calls;
import com.mediatek.phone.provider.CallHistoryDatabaseHelper.Tables;

import java.util.HashMap;

public class CallHistoryContentProvider extends ContentProvider {

    private static final int CALLS = 1;
    private static final int COUNTRY_ISOS = 2;

    private static final UriMatcher URIMATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URIMATCHER.addURI(CallHistory.AUTHORITY, "calls", CALLS);
        URIMATCHER.addURI(CallHistory.AUTHORITY, "country_isos", COUNTRY_ISOS);
    }

    private static final HashMap<String, String> CALLSPROJECTIONMAP;
    static {
        // Calls projection map
        CALLSPROJECTIONMAP = new HashMap<String, String>();
        CALLSPROJECTIONMAP.put(Calls._ID, Calls._ID);
        CALLSPROJECTIONMAP.put(Calls.NUMBER, Calls.NUMBER);
        CALLSPROJECTIONMAP.put(Calls.DATE, Calls.DATE);
        CALLSPROJECTIONMAP.put(Calls.COUNTRY_ISO, Calls.COUNTRY_ISO);
        CALLSPROJECTIONMAP.put(Calls.AREA_CODE, Calls.AREA_CODE);
        CALLSPROJECTIONMAP.put(Calls.CONFIRM, Calls.CONFIRM);
    }

    private CallHistoryDatabaseHelper mDbHelper;

    protected CallHistoryDatabaseHelper getDatabaseHelperInstance(Context context) {
        CallHistoryDatabaseHelper dbHelper = CallHistoryDatabaseHelper.getInstance(context);
        return dbHelper;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mDbHelper = getDatabaseHelperInstance(context);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);

        boolean distinct = false;
        int match = URIMATCHER.match(uri);
        switch (match) {
            case CALLS:
                break;
            case COUNTRY_ISOS:
                distinct = true;
                break;

            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = db.query(distinct, Tables.CALLS, projection, selectionBuilder.build(),
                            selectionArgs, null, null, sortOrder, null);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        int match = URIMATCHER.match(uri);
        switch (match) {
            case CALLS:
            case COUNTRY_ISOS:
                return Calls.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(Tables.CALLS, null, values);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int matchedUriId = URIMATCHER.match(uri);
        switch (matchedUriId) {
            case CALLS:
                break;

            default:
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
        }

        return db.update(Tables.CALLS, values, selectionBuilder.build(), selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int matchedUriId = URIMATCHER.match(uri);
        switch (matchedUriId) {
            case CALLS:
                return db.delete(Tables.CALLS, selectionBuilder.build(), selectionArgs);
            default:
                throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
        }
    }
}
