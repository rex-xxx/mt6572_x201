package com.mediatek.thememanager;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.mediatek.xlog.Xlog;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provider to manager information of themes in current system in database.
 */
public class ThemeProvider extends ContentProvider {
    private static final String TAG = "ThemeProvider";
    
    private static final String DATABASE_NAME = "themes.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "theme";

    private SQLiteDatabase mDatabase;
    private DatabaseHelper mDatabaseHelper;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;
        private final Collator mCollator = Collator.getInstance();

        private final Comparator<PackageInfo> mThemeNameComparator = new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo a, PackageInfo b) {
                String aThemeName = ThemeManager.getThemeName(mContext, a.packageName, a.themeNameId);
                String bThemeName = ThemeManager.getThemeName(mContext, b.packageName, b.themeNameId);
                return mCollator.compare(aThemeName, bThemeName);
            }
        };

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Xlog.d(TAG, "Enter DatabaseHelper.onCreate()");
            db.execSQL("Create table " + TABLE_NAME + "( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "package_name TEXT," + "theme_path TEXT," + "theme_name_id INTEGER);");
            initDatabase(db);
            Xlog.d(TAG, "Leave DatabaseHelper.onCreate()");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Xlog.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
            onCreate(db);
        }

        public void initDatabase(SQLiteDatabase db) {
            PackageManager pmg = mContext.getPackageManager();
            List<PackageInfo> allApps = pmg.getInstalledPackages(0);
            List<PackageInfo> selectedApps = new ArrayList<PackageInfo>();
            PackageInfo defaultApp = null;
            
            for (PackageInfo app : allApps) {
                Xlog.d(TAG, "initDatabase: packageName = " + app.packageName + ",isThemePackage = "
                        + app.isThemePackage + ",themeNameId = " + app.themeNameId);
                if (app.packageName.equals(ThemeManager.DEFAULT_THEME_PACKAGE_NAME)) {
                    defaultApp = app;                
                }
                if (ThemeReceiver.THEME_PACKAGE_LOGO == app.isThemePackage) {
                     selectedApps.add(app);
                }
            }
            Collections.sort(selectedApps, mThemeNameComparator);
            if (defaultApp != null) {
                selectedApps.add(0, defaultApp);
            }
            ContentValues values;
            for (PackageInfo item : selectedApps) {
                values = new ContentValues();
                values.put(Themes.PACKAGE_NAME, item.packageName);
                values.put(Themes.THEME_PATH, item.applicationInfo.sourceDir);
                values.put(Themes.THEME_NAME_ID, item.themeNameId);
                db.insert(TABLE_NAME, null, values);
            }

            allApps.clear();
            selectedApps.clear();
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri url, String selection, String[] selectionArgs) {
        Xlog.d(TAG, "Enter delete()");
        mDatabase = mDatabaseHelper.getWritableDatabase();
        return mDatabase.delete(TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public String getType(Uri url) {
        return null;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        Xlog.d(TAG, "Enter insert()");
        mDatabase = mDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues(initialValues);
        long rowId = mDatabase.insert(TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri rowUri = ContentUris.appendId(Themes.CONTENT_URI.buildUpon(), rowId).build();
            Xlog.d(TAG, "Leave insert()");
            return rowUri;
        }
        throw new SQLException("Failed to insert row into " + url);
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Xlog.d(TAG, "Enter query()");
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        qb.setTables(TABLE_NAME);
        Xlog.d(TAG, "query(): uri: " + url.toString());

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), url);
        }
        Xlog.d(TAG, "Leave query()");
        return c;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        return 0;
    }
}
