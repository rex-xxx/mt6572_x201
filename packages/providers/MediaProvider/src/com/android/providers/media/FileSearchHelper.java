package com.android.providers.media;

import android.app.SearchManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.MediaFile;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;

/**
 * Helper class used to computes file attributes, such as file_type, file_name,
 * is_ringtone, is_notification, is_alarm, is_music and is_podcast.
 *
 */
public class FileSearchHelper {
    private static final String TAG = "FileSearchHelper";
    private static String[] sSearchFileCols = new String[] {
            android.provider.BaseColumns._ID,
            "(CASE WHEN media_type=1 THEN " + R.drawable.ic_search_category_image +
            " ELSE CASE WHEN media_type=2 THEN " + R.drawable.ic_search_category_audio +
            " ELSE CASE WHEN media_type=3 THEN " + R.drawable.ic_search_category_video +
            " ELSE CASE WHEN file_type=4 THEN " + R.drawable.ic_search_category_text +
            " ELSE CASE WHEN file_type=5 THEN " + R.drawable.ic_search_category_zip +
            " ELSE CASE WHEN file_type=6 THEN " + R.drawable.ic_search_category_apk +
            " ELSE CASE WHEN format=12289 THEN " + R.drawable.ic_search_category_folder +
            " ELSE " + R.drawable.ic_search_category_others + " END END END END END END END" +
            ") AS " + SearchManager.SUGGEST_COLUMN_ICON_1,
            FileColumns.FILE_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
            "_data AS " + SearchManager.SUGGEST_COLUMN_TEXT_2,
            "_data AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            "_id AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
    };

    /**
     * Constants for file type indicating the file is not an image, audio, video, text, zip or apk. 
     */
    public static final int FILE_TYPE_NONE = 0;
    /**
     * Constants for file type indicating the file is an image.
     */
    public static final int FILE_TYPE_IMAGE = 1;
    /**
     * Constants for file type indicating the file is an audio.
     */
    public static final int FILE_TYPE_AUDIO = 2;
    /**
     * Constants for file type indicating the file is a video.
     */
    public static final int FILE_TYPE_VIDEO = 3;
    /**
     * Constants for file type indicating the file is a text.
     */
    public static final int FILE_TYPE_TEXT = 4;
    /**
     * Constants for file type indicating the file is a zip.
     */
    public static final int FILE_TYPE_ZIP = 5;
    /**
     * Constants for file type indicating the file is an apk.
     */
    public static final int FILE_TYPE_APK = 6;

    /**
     * Searches files whose name contain some specific string.
     * 
     * @param db the SQLiteDatabase instance used to query database.
     * @param qb the SQLiteQueryBuilder instance used to build sql query.
     * @param uri the uri with the search string at its last path.
     * @param limit the limit of rows returned by this query.
     * @return
     */
    public static Cursor doFileSearch(SQLiteDatabase db, SQLiteQueryBuilder qb, Uri uri, String limit) {
        if (db == null || qb == null || uri == null) {
            MtkLog.e(TAG, "doFileSearch: Param error!");
            return null;
        }

        String searchString = uri.getPath().endsWith("/") ? "" : uri.getLastPathSegment();
        searchString = Uri.decode(searchString).trim();
        if (TextUtils.isEmpty(searchString)) {
            return null;
        }

        searchString = searchString.replace("\\", "\\\\");
        searchString = searchString.replace("%", "\\%");
        searchString = searchString.replace("'", "\\'");
        searchString = "%" + searchString + "%";
        String where = FileColumns.FILE_NAME + " LIKE ? ESCAPE '\\'";
        String[] whereArgs = new String[] { searchString };
        qb.setTables("files");
        return qb.query(db, sSearchFileCols, where, whereArgs, null, null, null, limit);
    }

    /**
     * Updates file_name and file_type.
     * 
     * @param db the SQLiteDatabase instance used to update database.
     * @param tableName the name of table to be updated.
     */
    public static void updateFileNameAndType(SQLiteDatabase db, String tableName) {
        if (db == null || tableName == null) {
            MtkLog.e(TAG, "updateFileName: Param error!");
            return;
        }
        db.beginTransaction();
        try {
            String[] columns = {BaseColumns._ID, MediaColumns.DATA, FileColumns.FILE_NAME};
            Cursor cursor = db.query(tableName, columns, null, null, null, null, null);
            try {
                if (cursor != null) {
                    final int idColumnIndex = cursor.getColumnIndex(BaseColumns._ID);
                    final int dataColumnIndex = cursor.getColumnIndex(MediaColumns.DATA);
                    final int fileNameIndex = cursor.getColumnIndex(FileColumns.FILE_NAME);
                    ContentValues values = new ContentValues();
                    while (cursor.moveToNext()) {
                        String fileName = cursor.getString(fileNameIndex);
                        if (fileName == null) {
                            String data = cursor.getString(dataColumnIndex);
                            values.clear();
                            computeFileName(data, values);
                            computeFileType(data, values);
                            int rowId = cursor.getInt(idColumnIndex);
                            db.update(tableName, values, "_id=" + rowId, null);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Computes file name from file path.
     * 
     * @param path the file path.
     * @param values ContentValues instance to hold file name.
     */
    public static void computeFileName(String path, ContentValues values) {
        if (path == null || values == null) {
            MtkLog.e(TAG, "computeFileName: Param error!");
            return;
        }

        int idx = path.lastIndexOf('/');
        if (idx >= 0) {
            path = path.substring(idx + 1);
        }
        values.put(FileColumns.FILE_NAME, path);
    }

    /**
     * Computes file type from file path.
     * 
     * @param path the file path.
     * @param values ContentValues instance to hold file name.
     */
    public static void computeFileType(String path, ContentValues values) {
        if (path == null || values == null) {
            MtkLog.e(TAG, "computeFileType: Param error!");
            return;
        }
        
        String mimeType = MediaFile.getMimeTypeForFile(path);
        if (mimeType == null) {
            return;
        }

        mimeType = mimeType.toLowerCase();
        if (mimeType.startsWith("image/")) {
            values.put(FileColumns.FILE_TYPE, FILE_TYPE_IMAGE);
            return;
        }
        if (mimeType.startsWith("audio/") || mimeType.startsWith("application/ogg")) {
            values.put(FileColumns.FILE_TYPE, FILE_TYPE_AUDIO);
            return;
        }
        if (mimeType.startsWith("video/")) {
            values.put(FileColumns.FILE_TYPE, FILE_TYPE_VIDEO);
            return;
        }
        if (mimeType.startsWith("text/")) {
            values.put(FileColumns.FILE_TYPE, FILE_TYPE_TEXT);
            return;
        }
        if (mimeType.equals("application/zip")) {
            values.put(FileColumns.FILE_TYPE, FILE_TYPE_ZIP);
            return;
        }
        if (mimeType.equals("application/vnd.android.package-archive")) {
            values.put(FileColumns.FILE_TYPE, FILE_TYPE_APK);
            return;
        }
    }

    private static final String RINGTONES_DIR = "/ringtones/";
    private static final String NOTIFICATIONS_DIR = "/notifications/";
    private static final String ALARMS_DIR = "/alarms/";
    private static final String MUSIC_DIR = "/music/";
    private static final String PODCAST_DIR = "/podcasts/";

    /**
     * Computes is_ringtone, is_notification, is_alarm, is_music and is_podcast based on file path.
     * 
     * @param path file path.
     * @param values ContentValues instance to hold values.
     */
    public static void computeRingtoneAttributes(String path, ContentValues values) {
        if (path == null || values == null) {
            MtkLog.e(TAG, "computeRingtoneAttributes: Param error!");
            return;
        }
        String lowpath = path.toLowerCase();
        boolean ringtones = (lowpath.indexOf(RINGTONES_DIR) > 0);
        boolean notifications = (lowpath.indexOf(NOTIFICATIONS_DIR) > 0);
        boolean alarms = (lowpath.indexOf(ALARMS_DIR) > 0);
        boolean podcasts = (lowpath.indexOf(PODCAST_DIR) > 0);
        boolean music = (lowpath.indexOf(MUSIC_DIR) > 0) ||
            (!ringtones && !notifications && !alarms && !podcasts);
        values.put(Audio.Media.IS_RINGTONE, ringtones);
        values.put(Audio.Media.IS_NOTIFICATION, notifications);
        values.put(Audio.Media.IS_ALARM, alarms);
        values.put(Audio.Media.IS_MUSIC, music);
        values.put(Audio.Media.IS_PODCAST, podcasts);
    }

    /**
     * Handles shortcut search.
     * 
     * @param db the SQLiteDatabase instance used to query database.
     * @param qb the SQLiteQueryBuilder instance used to build sql query.
     * @param uri the uri to be queried.
     * @param limit the limit of rows returned by this query.
     * @return
     */
    public static Cursor doShortcutSearch(SQLiteDatabase db, SQLiteQueryBuilder qb, Uri uri, String limit) {
        if (db == null || qb == null || uri == null) {
            MtkLog.e(TAG, "doShortcutSearch: Param error!");
            return null;
        }

        String searchString = uri.getLastPathSegment();
        searchString = Uri.decode(searchString).trim();
        if (TextUtils.isEmpty(searchString)) {
            MtkLog.e(TAG, "doShortcutSearch: Null id!");
            return null;
        }

        String where = FileColumns._ID + "=?";
        String[] whereArgs = new String[] { searchString };
        qb.setTables("files");
        return qb.query(db, sSearchFileCols, where, whereArgs, null, null, null, limit);
    }
}
