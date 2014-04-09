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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mediatek.phone.provider.CallHistory.Calls;

public class CallHistoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "CallHistoryDatabaseHelper";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "phone.db";

    private static CallHistoryDatabaseHelper sSingleton = null;

    public interface Tables {
        static final String CALLS = "calls";

        static final String[] SEQUENCE_TABLES = new String[] {
                CALLS
        };
    }

    public static synchronized CallHistoryDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new CallHistoryDatabaseHelper(context, DATABASE_NAME, true);
        }
        return sSingleton;
    }

    protected CallHistoryDatabaseHelper(Context context, String databaseName,
                                        boolean optimizationEnabled) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    public void initDatabase() {
        getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Bootstrapping database version: " + DATABASE_VERSION);

        db.execSQL("CREATE TABLE " + Tables.CALLS + " (" +
                Calls._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Calls.NUMBER + " TEXT," +
                Calls.DATE + " INTEGER," +
                Calls.COUNTRY_ISO + " TEXT," +
                Calls.AREA_CODE + " TEXT DEFAULT NULL," +
                Calls.CONFIRM + " INTEGER NOT NULL DEFAULT 0" +
        ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Returns a new instance for unit tests.
     */
    static CallHistoryDatabaseHelper getNewInstanceForTest(Context context) {
        return new CallHistoryDatabaseHelper(context, null, false);
    }
}