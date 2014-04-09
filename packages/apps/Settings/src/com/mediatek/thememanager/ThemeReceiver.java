package com.mediatek.thememanager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;

import com.mediatek.xlog.Xlog;

/**
 * Receive package add and removed broadcast, and add a record
 * to database when the added package is a theme package, or delete
 * the related record in database when a theme package is removed.
 */
public class ThemeReceiver extends BroadcastReceiver {
    private static final String TAG = "ThemeReceiver";
    private PackageManager mPm;
    static final int THEME_PACKAGE_LOGO = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        mPm = context.getPackageManager();
        String packageName = intent.getData().getSchemeSpecificPart();
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            // When a package added to system, judge whether this package is a theme package.
            try {
                PackageInfo info = mPm.getPackageInfo(packageName, 0);
                Xlog.d(TAG, "insert theme: " + info.isThemePackage + " " + info.themeNameId);
                if (THEME_PACKAGE_LOGO == info.isThemePackage) {
                    ContentValues values = new ContentValues();
                    values.put(Themes.PACKAGE_NAME, packageName);
                    values.put(Themes.THEME_PATH, info.applicationInfo.sourceDir);
                    values.put(Themes.THEME_NAME_ID, info.themeNameId);
                    context.getContentResolver().insert(Themes.CONTENT_URI, values);
                }
            } catch (NameNotFoundException e) {
                Xlog.d(TAG, "Intent.ACTION_PACKAGE_ADDED can not find name:packageName = " + packageName);
            }
        } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            // When a package removed from system, judge whether this package is a theme package.
            Xlog.d(TAG, "delete theme: " + packageName);
            Cursor c = null;
            /// M: try finally the database operation to avoid exception
            try {
                c = context.getContentResolver().query(Themes.CONTENT_URI, null,
                    Themes.PACKAGE_NAME + " = ?", new String[] { packageName }, null);
                if (c != null) {
                    context.getContentResolver().delete(Themes.CONTENT_URI,
                        Themes.PACKAGE_NAME + " = ?", new String[] { packageName });
                }
            } finally {
                /// M: finally close the cursor to avoid leak
                if (c != null) {
                    c.close();
                }
            }
        }
    }
}
