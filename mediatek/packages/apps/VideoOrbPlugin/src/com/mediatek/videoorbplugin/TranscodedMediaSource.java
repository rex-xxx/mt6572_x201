package com.mediatek.videoorbplugin;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import com.mediatek.ngin3d.Video;

import java.io.File;
import java.util.ArrayList;

public class TranscodedMediaSource implements IMediaSource {
    private final static String TAG = "vo.transcoded";
    private static final String [] PROJECTION =
            new String [] { MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATE_TAKEN };
    public static final int MAX_VIDEO_CONTENT = 8;

    private final ContentResolver mCr;
    private Cursor mCursor;
    private int mCounts;

    public TranscodedMediaSource(ContentResolver cr) {
        mCr = cr;
    }

    private Cursor query() {
        if (mCursor == null) {
            mCursor = mCr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, selectionAll(), null, sortOrder());
            Log.v(TAG, "query() : " + mCursor);
        }
        return mCursor;
    }

    public int getMediaCount() {
        query();
        if (mCursor != null) {
            mCounts = mCursor.getCount();
            Log.v(TAG, "media count : " + mCounts);
            return mCounts > MAX_VIDEO_CONTENT ?
                    (mCounts = MAX_VIDEO_CONTENT) : mCounts;
        }
        return 0;
    }

    public Video getMedia(Context cts, int index, int width, int height) {
        if (index >= mCounts) {
            return null;
        }
        query();
        if (mCursor == null) {
            return null;
        }

        if (mCursor.moveToPosition(index)) {
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(0)); //_ID
            Video clip = Video.createFromVideo(cts, uri, width, height);
            clip.setDoubleSided(true);
            clip.setVolume(0, 0);
            return clip;
        }
        return null;
    }

    public void close() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public static final String TRANSCODED_TAG_ID = "VOTranscoded";
    public static final String INCLUDED_TAG_ID = "VOLink";
    private static String selectionAll() {
        String query = MediaStore.Video.Media.TAGS + "='" + TRANSCODED_TAG_ID
                + "' OR " + MediaStore.Video.Media.TAGS + "='" + INCLUDED_TAG_ID + "'";
        Log.v(TAG, "selectionAll() : " + query);
        return query;
    }

    private static String selectionNotCurVer(int ver) {
        String query = MediaStore.Video.Media.TAGS + " = '" + TRANSCODED_TAG_ID + "' AND " +
                       MediaStore.Video.Media.CATEGORY + " <> " + ver;
        Log.v(TAG, "selectionNotCurVer() : " + query);
        return query;
    }

    private static String selectionCurVer(int ver) {
        String query = MediaStore.Video.Media.TAGS + " = '" + TRANSCODED_TAG_ID + "' AND " +
                MediaStore.Video.Media.CATEGORY + " = " + ver;
        Log.v(TAG, "selectionNotCurVer() : " + query);
        return query;
    }

    private static String sortOrder() {
        return MediaStore.Video.Media.DATE_TAKEN + " DESC LIMIT 8";
    }

    public Uri getMediaUri(Context ctx, int index) {
        if (index >= mCounts) {
            return null;
        }
        query();
        if (mCursor == null || !mCursor.moveToPosition(index)) {
            return null;
        }

        return ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(0));
    }

    public static boolean hasTranscoded(ContentResolver cr, int ver) {
        if (cr == null)
            return false;

        // Query transcoded
        Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, selectionCurVer(ver), null, sortOrder());

        if (cursor.getCount() == 0) {
            cursor.close();
            Log.v(TAG, "hasTrascoded : no transcoded");
            return false;
        }

        // Get first transcoded taken date.
        ArrayList<Long> transcodeTime = new ArrayList<Long>();
        cursor.moveToFirst();
        do {
            String transcodedDateAdded = cursor.getString(1); // DateAdded
            transcodeTime.add(Long.valueOf(transcodedDateAdded));
            Log.v(TAG, "hasTrascoded : transcoded dateModified : " + transcodedDateAdded);
        } while (cursor.moveToNext());
        cursor.close();

        // Query external except transcoded
        cursor = ExternalMediaSource.queryLatest8(cr);
        if (cursor.getCount() == 0) {
            cursor.close();
            Log.v(TAG, "hasTrascoded : no external");
            // if there is no external sources,
            // which means we should re-config the transcoded videos.
            return false;
        }

        ArrayList<Long> externalTime = new ArrayList<Long>();
        cursor.moveToFirst();
        do {
            String externalDateTaken = cursor.getString(1); // DateTaken
            externalTime.add(Long.valueOf(externalDateTaken));
            Log.v(TAG, "hasTrascoded : external dateTaken : " + externalDateTaken);
        } while(cursor.moveToNext());
        cursor.close();

        boolean needTranscode = false;
        if (transcodeTime.size() != externalTime.size()) {
            needTranscode = true;
        } else {
            for (int i = 0; i < transcodeTime.size(); ++i) {
                if (!transcodeTime.get(i).equals(externalTime.get(i)))
                    needTranscode = true;
            }
        }

        Log.v(TAG, "Transcode count : " + transcodeTime.size() +
                ", external count : " + externalTime.size() + ", hasTranscoded : " + !needTranscode);
        return !needTranscode;
    }

    // Remove all invalid sources.
    public static void removeNonCurVersionData(ContentResolver cr, int ver) {
        ArrayList<Uri> invalidate = new ArrayList<Uri>();
        Cursor c = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, selectionNotCurVer(ver), null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            do {
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(0)); //_ID
                String path = TranscoderActivity.getPathFromUri(cr, uri);
                Log.v(TAG, "Validate : ver : " + ver +  "uri : " + uri + ", path : " + path);
                File f = new File(path);
                if (!f.exists()) {
                    Log.w(TAG, "Validate path doesn't exist : " + path);
                    invalidate.add(uri);
                }
            } while (c.moveToNext());
        }
        c.close();

        for(Uri uri : invalidate) {
            int deletedRow = cr.delete(uri, null, null);
            Log.v(TAG, "Validate : ver " + ver +  ", remove uri (deleted): " + uri + ", row : " + deletedRow);
        }
    }
}