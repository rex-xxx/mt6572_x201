package com.android.providers.media;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;

import com.android.providers.media.MediaProvider.DatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class used to update audio's title_key, artist_key and album_key.
 * The title_key, artist_key and album_key are used for full text search. And those keys were generated
 * when the audio was inserted to database under certain Locale. Those keys generated under different Locale
 * are different. As a result, nothing would be searched if the Locale was changed to one that different to
 * the Locale under which those keys were generated. 
 * 
 */
public class UpdateKeyRequest {
    private static final String TAG = "UpdateKeyRequest";

    private Object mLock = new Object();
    private HashMap<Integer, Integer> mAlbumIdHash = new HashMap<Integer, Integer>();
    private HashMap<String, List<Integer>> mAlbumNameIds = new HashMap<String, List<Integer>>();
    private HashMap<String, List<Integer>> mArtistNameIds = new HashMap<String, List<Integer>>();

    /**
     * M: The last finish update key request locale
     */
    public static final String LAST_FINISHED_UPDATE_LOCALE = "last_finished_update_locale";

    private boolean mInterrupted;
    private DatabaseHelper mHelper;
    private SQLiteDatabase mDb;
    private Context mContext;
    private String mLocaleString;
    /**
     * Constructor.
     * 
     * @param helper the DatabaseHelper instance.
     * @param db the SQLiteDatabase instance.
     * @param context Context use to edit share preference
     * @param newLocaleString new locale
     */
    public UpdateKeyRequest(DatabaseHelper helper, SQLiteDatabase db, Context context, String newLocaleString) {
        mHelper = helper;
        mDb = db;
        mContext = context;
        mLocaleString = newLocaleString;
    }

    /**
     * Starts to update audio's title_key, artist_key and album_key.
     */
    public void execute() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                MtkLog.v(TAG, "execute: subthread started.");
                updateKey(mHelper, mDb);
                /// M: Save locale to sharepreference when update this locale finished
                saveUpdateLocaleToPrefs();
                MtkLog.v(TAG, "execute: subthread end.");
            }
        });
        t.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        t.start();
    }

    /**
     * Interrupts current update.
     */
    public void interrupt() {
        synchronized (mLock) {
            MtkLog.w(TAG, "interrupting...");
            mInterrupted = true;
        }
    }

    private boolean isInterrupted() {
        synchronized (mLock) {
            return mInterrupted;
        }
    }

    private void updateKey(DatabaseHelper helper, SQLiteDatabase db) {
        MtkLog.v(TAG, "updateKey: started.");
        final String[] tables = new String[] { 
                "audio_meta", // 0
                "artists", // 1
                "albums" // 2
        };

        final String[][] projections = new String[][] {
            new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            },
            new String[] {
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Artists.ARTIST
            },
            new String[] {
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Albums.ALBUM
            }
        };
        
        final String[] whereClauses = new String[] {
                MediaStore.Audio.Media._ID + "=? and " + MediaStore.Audio.Media.TITLE + "=? ",
                MediaStore.Audio.Media.ARTIST_ID + "=? and " + MediaStore.Audio.Artists.ARTIST + "=? ",
                MediaStore.Audio.Media.ALBUM_ID + "=? and " + MediaStore.Audio.Albums.ALBUM + "=? ",
            
        };
        
        final String[] keys = new String[] {
                MediaStore.Audio.Media.TITLE_KEY,
                MediaStore.Audio.Artists.ARTIST_KEY,
                MediaStore.Audio.Albums.ALBUM_KEY
        };
        
        final String[] whereArgs = new String[2];
        final int columnId = 0;
        final int columnKey = 1;
        final int length = tables.length;
        Cursor cursor = null;
        ContentValues values = new ContentValues(1);
        try {
            // get audio info
            getAudioInfos(helper, db);
            db.beginTransaction();
            uniqueArtist(helper, db, mArtistNameIds);
            uniqueAlbum(helper, db, mAlbumNameIds, mAlbumIdHash);
            for (int i = 0; i < length; i++) {
                helper.mNumQueries++;
                cursor = db.query(tables[i], projections[i], null, null, null, null, null);
                // Since audio_meta is a view in ICS, and view can not be updated. So to update files table.
                String updateTable = tables[i];
                if (i == 0) {
                    updateTable = "files";
                }

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        if (isInterrupted()) {
                            MtkLog.w(TAG, "updateKey: Interrupted!");
                            return;
                        }
                        values.clear();
                        int id = cursor.getInt(columnId);
                        whereArgs[0] = String.valueOf(id);
                        String name = cursor.getString(columnKey);
                        if (name != null) {
                            if (i == 2) {
                                // ablum, should add album hash
                                values.put(keys[i], MediaStore.Audio.keyFor(name) + mAlbumIdHash.get(id));
                            } else {
                                values.put(keys[i], MediaStore.Audio.keyFor(name));
                            }
                            whereArgs[1] = name;
                            helper.mNumUpdates++;
                            db.update(updateTable, values, whereClauses[i], whereArgs);
                        } else {
                            MtkLog.e(TAG, "updateKeyIfNeed: Null name! table=" + tables[i] + ",id=" + id);
                        }
                    }
                    cursor.close();
                    cursor = null;
                }
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            MtkLog.e(TAG, "updateKey: id=" + whereArgs[0] + ",values=" + values, e);
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
            clear();
        }
        MtkLog.v(TAG, "updateKey: finished.");
    }

    private void getAudioInfos(DatabaseHelper helper, SQLiteDatabase db) {
        clear();
        final String table = "audio";
        final String[] projections = new String[]{
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST
        };
        
        helper.mNumQueries++;
        final String groupby = MediaStore.Audio.Media.ARTIST_ID + "," + MediaStore.Audio.Media.ALBUM_ID;
        Cursor cursor = db.query(table, projections, null, null, groupby, null, null);
        try {
            if (cursor != null) {
                final int dataIndex = 0;
                final int albumIdIndex = 1;
                final int albumIndex = 2;
                final int artistIdIndex = 3;
                final int artistIndex = 4;
                final int ablumArtistIndex = 5;

                while (cursor.moveToNext()) {
                    String path = cursor.getString(dataIndex);
                    int albumId = cursor.getInt(albumIdIndex);
                    String album = cursor.getString(albumIndex);
                    int artistId = cursor.getInt(artistIdIndex);
                    String artist = cursor.getString(artistIndex);
                    String albumArtist = cursor.getString(ablumArtistIndex);

                    int albumHash = 0;
                    boolean isUnknown = MediaStore.UNKNOWN_STRING.equals(albumArtist);
                    if (albumArtist != null && !isUnknown) {
                        albumHash = albumArtist.hashCode();
                    } else if (path != null && !"".equals(path)) {
                        int lastSlashIndex = path.lastIndexOf('/');
                        if (lastSlashIndex >= 0) {
                            albumHash = path.substring(0, lastSlashIndex).hashCode();
                        } else {
                            MtkLog.e(TAG, "getAudioInfos: Invalid path=" + path);
                        }
                    } else {
                        MtkLog.e(TAG, "getAudioInfos: path=" + path);
                    }
                    mAlbumIdHash.put(albumId, albumHash);

                    if (album != null) {
                        if (mAlbumNameIds.containsKey(album)) {
                            List<Integer> ids = mAlbumNameIds.get(album);
                            if (!ids.contains(albumId)) {
                                ids.add(albumId);
                            }
                        } else {
                            List<Integer> ids = new ArrayList<Integer>();
                            ids.add(albumId);
                            mAlbumNameIds.put(album, ids);
                        }
                    } else {
                        MtkLog.e(TAG, "getAudioInfos: Null album with id=" + albumId);
                    }

                    if (artist != null) {
                        if (mArtistNameIds.containsKey(artist)) {
                            List<Integer> ids = mArtistNameIds.get(artist);
                            if (!ids.contains(artistId)) {
                                ids.add(artistId);
                            }
                        } else {
                            List<Integer> ids = new ArrayList<Integer>();
                            ids.add(artistId);
                            mArtistNameIds.put(artist, ids);
                        }
                    } else {
                        MtkLog.e(TAG, "getAudioInfos: Null artist with id=" + artistId);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void uniqueArtist(DatabaseHelper helper, SQLiteDatabase db, HashMap<String, List<Integer>> nameIdsMap) {
        if (helper == null || db == null || nameIdsMap == null) {
            MtkLog.e(TAG, "uniqueArtist<<<Parameter error! helper=" + helper + ",db=" + db + ",nameIdsMap=" + nameIdsMap);
            return;
        }

        Set<Entry<String, List<Integer>>> entrySet = nameIdsMap.entrySet();
        Iterator<Entry<String, List<Integer>>> iter = entrySet.iterator();
        while (iter.hasNext()) {
            Entry<String, List<Integer>> entry = iter.next();
            List<Integer> ids = entry.getValue();
            if (ids.size() > 1) {
                MtkLog.v(TAG, "uniqueArtist: artist=" + entry.getKey());
                uniqueForeignKey(helper, db, "files", MediaStore.Audio.Media.ARTIST_ID, ids);
            }
        }

        deleteNotReferencedKey(helper, db, "artists", MediaStore.Audio.Media.ARTIST_ID, "audio",
                MediaStore.Audio.Media.ARTIST_ID);
    }

    private void uniqueAlbum(DatabaseHelper helper, SQLiteDatabase db, HashMap<String, List<Integer>> nameIdsMap,
            HashMap<Integer, Integer> idHashMap) {
        if (helper == null || db == null || nameIdsMap == null || idHashMap == null) {
            MtkLog.e(TAG, "uniqueAlbum<<<Parameter error! helper=" + helper + ",db=" + db + ",nameIdsMap=" + nameIdsMap);
            return;
        }

        Set<Entry<String, List<Integer>>> nameIdsEntrySet = nameIdsMap.entrySet();
        Iterator<Entry<String, List<Integer>>> nameIdsIter = nameIdsEntrySet.iterator();
        HashMap<Integer, List<Integer>> albumHashIds = new HashMap<Integer, List<Integer>>();

        while (nameIdsIter.hasNext()) {
            albumHashIds.clear();
            Entry<String, List<Integer>> nameIdsEntry = nameIdsIter.next();
            List<Integer> albumIds = nameIdsEntry.getValue();
            for (Integer id : albumIds) {
                Integer albumHash = idHashMap.get(id);
                if (albumHashIds.containsKey(albumHash)) {
                    List<Integer> ids = albumHashIds.get(albumHash);
                    ids.add(id);
                } else {
                    List<Integer> ids = new ArrayList<Integer>();
                    ids.add(id);
                    albumHashIds.put(albumHash, ids);
                }
            }

            Set<Entry<Integer, List<Integer>>> hashIdsEntrySet = albumHashIds.entrySet();
            Iterator<Entry<Integer, List<Integer>>> hashIdsIter = hashIdsEntrySet.iterator();
            while (hashIdsIter.hasNext()) {
                Entry<Integer, List<Integer>> hashIdsEntry = hashIdsIter.next();
                int hash = hashIdsEntry.getKey();
                List<Integer> ids = hashIdsEntry.getValue();
                if (hash != 0 && ids.size() > 1) {
                    MtkLog.v(TAG, "uniqueAlbum: album=" + nameIdsEntry.getKey() + ",hash=" + hash);
                    uniqueForeignKey(helper, db, "files", MediaStore.Audio.Media.ALBUM_ID, ids);
                }
            }
        }

        deleteNotReferencedKey(helper, db, "albums", MediaStore.Audio.Media.ALBUM_ID, "audio",
                MediaStore.Audio.Media.ALBUM_ID);
    }

    private int uniqueForeignKey(DatabaseHelper helper, SQLiteDatabase db, String foreignTable, String foreignKey,
            List<Integer> ids) {
        MtkLog.v(TAG, "uniqueForeignKey>>>table=" + foreignTable);
        int size = (ids == null ? 0 : ids.size());
        if (size < 2) {
            MtkLog.v(TAG, "uniqueForeignKey<<<Not needed. size=" + size);
            return 0;
        }

        if (helper == null || db == null || foreignTable == null || foreignKey == null) {
            MtkLog.e(TAG, "uniqueForeignKey<<<Parameter error!helper=" + helper + ",db=" + db + ",table=" + foreignTable
                    + ",key=" + foreignKey);
            return -1;
        }

        int maxId = 0;
        int current = 0;
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(FileColumns.MEDIA_TYPE);
        whereClause.append("=");
        whereClause.append(FileColumns.MEDIA_TYPE_AUDIO);
        whereClause.append(" and ");
        whereClause.append(foreignKey);
        whereClause.append(" in (");
        Iterator<Integer> iter = ids.iterator();
        while (iter.hasNext()) {
            current = iter.next();
            if (maxId < current) {
                if (maxId != 0) {
                    whereClause.append(maxId).append(",");
                }
                maxId = current;
            } else if (maxId > current) {
                whereClause.append(current).append(",");
            }
        }
        whereClause.deleteCharAt(whereClause.length() - 1);
        whereClause.append(")");
        String where = whereClause.toString();
        MtkLog.v(TAG, "uniqueForeignKey: where=" + where);

        ContentValues values = new ContentValues();
        values.put(foreignKey, Integer.toString(maxId));
        int updated = db.update(foreignTable, values, where, null);
        helper.mNumUpdates += updated;
        MtkLog.v(TAG, "uniqueForeignKey<<<maxId=" + maxId + ",updated=" + updated);
        return updated;
    }

    private int deleteNotReferencedKey(DatabaseHelper helper, SQLiteDatabase db, String table, String key,
            String foreignTable, String foreignKey) {
        MtkLog.v(TAG, "deleteNotReferencedKey>>>table=" + table);
        if (helper == null || db == null || table == null || key == null || foreignTable == null || foreignKey == null) {
            MtkLog.e(TAG, "deleteNotReferencedKey<<<Parameter error! helper=" + helper + ",db=" + db + ",table=" + table
                    + ",key=" + key + ",fTable=" + foreignTable + ",fKey=" + foreignKey);
            return -1;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(key)
            .append(" not in(")
            .append("select distinct ")
            .append(foreignKey)
            .append(" from ")
            .append(foreignTable)
            .append(")");
        String whereClause = sb.toString();
        int deleted = db.delete(table, whereClause, null);
        helper.mNumDeletes += deleted;
        MtkLog.v(TAG, "deleteNotReferencedKey<<<delete=" + deleted + ",where=" + whereClause);
        return deleted;
    }

    private void clear() {
        mAlbumIdHash.clear();
        mAlbumNameIds.clear();
        mArtistNameIds.clear();
    }

    /**
     * M: Save update key request new locale to share preference to avoid process kill before finished update,
     * so that mediaprovider can check at oncreate again to update this locale again then.
     */
    private void saveUpdateLocaleToPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_FINISHED_UPDATE_LOCALE, mLocaleString);
        editor.commit();
        MtkLog.d(TAG, "saveUpdateLocaleToPrefs at update request with new locale " + mLocaleString);
    }
}
