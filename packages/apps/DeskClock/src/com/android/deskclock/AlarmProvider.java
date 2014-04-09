/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.deskclock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class AlarmProvider extends ContentProvider {
    private AlarmDatabaseHelper mOpenHelper;

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("com.android.deskclock", "alarm", ALARMS);
        sURLMatcher.addURI("com.android.deskclock", "alarm/#", ALARMS_ID);
    }

    public AlarmProvider() {
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new AlarmDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String table = "alarms";
        if(Alarms.SUPPORT_SDSWAP){
            table = "alarms_view";
            projectionIn = Alarm.Columns.ALARM_FULL_QUERY_COLUMNS;
        }
        // Generate the body of the query
        int match = sURLMatcher.match(url);
        switch (match) {
            case ALARMS:
                qb.setTables(table);
                break;
            case ALARMS_ID:
                qb.setTables(table);
                qb.appendWhere("_id=");
                qb.appendWhere(url.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = null;
        try {
            ret = qb.query(db, projectionIn, selection, selectionArgs,
                    null, null, sort);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ret == null) {
            if (Log.LOGV) Log.v("Alarms.query: failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), url);
        }

        return ret;
    }

    @Override
    public String getType(Uri url) {
        int match = sURLMatcher.match(url);
        switch (match) {
            case ALARMS:
                return "vnd.android.cursor.dir/alarms";
            case ALARMS_ID:
                return "vnd.android.cursor.item/alarms";
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        long rowId = 0;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
            case ALARMS_ID: {
                String segment = url.getPathSegments().get(1);
                rowId = Long.parseLong(segment);
                String path = (String) values.get(Alarm.Columns.PATH);
                values.remove(Alarm.Columns.PATH);
                Cursor c = null;
                try {
                    count = db.update("alarms", values, "_id=" + rowId, null);
                    if (Alarms.SUPPORT_SDSWAP) {
                        c = db.query("ringtone", new String[] { "alarm_id" },
                                "alarm_id=" + rowId, null, null, null, null);
                        values = new ContentValues();
                        values.put(Alarm.Columns.PATH, path == null ? "" : path);
                        values.put("alarm_id", rowId);
                        if (c != null && c.moveToFirst()) {
                            Log.v("update alarm ringtone path = " + path
                                    + "  alarm id = " + rowId);
                            db.update("ringtone", values, "alarm_id=" + rowId,
                                    null);
                        } else {
                            db.insert("ringtone", null, values);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException(
                        "Cannot update URL: " + url);
            }
        }
        if (Log.LOGV) Log.v("*** notifyChange() rowId: " + rowId + " url " + url);
        getContext().getContentResolver().notifyChange(url, null);
        return count;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        if (sURLMatcher.match(url) != ALARMS) {
            throw new IllegalArgumentException("Cannot insert into URL: " + url);
        }
        Uri newUrl = null;
        try {
            newUrl = mOpenHelper.commonInsert(initialValues);
            getContext().getContentResolver().notifyChange(newUrl, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newUrl;
    }

    public int delete(Uri url, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        long rowId = 0;
        try {
            switch (sURLMatcher.match(url)) {
                case ALARMS:
                    if (Alarms.SUPPORT_SDSWAP) {
                        if (TextUtils.isEmpty(where)) {
                            db.delete("ringtone", null, whereArgs);
                        } else {
                            String[] columns = new String[] { "_id" };
                            Cursor c = null;
                            try {
                                c = db.query("alarms", columns, where, whereArgs,
                                        null, null, null);
                                if (c != null && c.moveToFirst()) {
                                    while (c.moveToNext()) {
                                        db.delete("ringtone",
                                                "alarm_id=" + c.getInt(0), null);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    }
                    count = db.delete("alarms", where, whereArgs);
                    break;
                case ALARMS_ID:
                    String segment = url.getPathSegments().get(1);
                    rowId = Long.parseLong(segment);
                    if (TextUtils.isEmpty(where)) {
                        where = "_id=" + segment;
                    } else {
                        where = "_id=" + segment + " AND (" + where + ")";
                    }
                    if (Alarms.SUPPORT_SDSWAP) {
                        db.delete("ringtone", "alarm_id=" + rowId, null);
                    }
                    count = db.delete("alarms", where, whereArgs);
                    break;
                default:
                    throw new IllegalArgumentException("Cannot delete from URL: " + url);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        getContext().getContentResolver().notifyChange(url, null);
        return count;
    }
}
