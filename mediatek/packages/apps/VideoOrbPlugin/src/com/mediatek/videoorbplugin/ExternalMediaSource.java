package com.mediatek.videoorbplugin;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Media;
import com.mediatek.ngin3d.Video;
import android.util.Log;

public class ExternalMediaSource implements IMediaSource {
    private final static String TAG = "vo.ext";
    private static final String [] PROJECTION =
            new String [] { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_TAKEN,  Media.TAGS};
    public static final int MAX_VIDEO_CONTENT = 8;

    private final ContentResolver mCr;
    private Cursor mCursor;
    private int mCounts;

    public ExternalMediaSource(ContentResolver cr) {
        mCr = cr;
    }

    private static String selection() {
        String query = MediaStore.Video.Media.TAGS + " IS NULL OR " + MediaStore.Video.Media.TAGS + " <>? ";
        Log.v(TAG, "selection() : " + query);
        return query;
    }

    private static String[] selectionArg() {
        Log.v(TAG, "selectionArg() : " + TranscodedMediaSource.TRANSCODED_TAG_ID);
        return new String [] { TranscodedMediaSource.TRANSCODED_TAG_ID };
    }

//    private static String selection() {
//        String query = "(" +
//                       MediaStore.Video.Media.TAGS + " IS NULL OR " +
//                       MediaStore.Video.Media.TAGS + " <>? ) AND (" +
//                       Media.MIME_TYPE + " =? OR " +
//                       Media.MIME_TYPE + " =? OR " +
//                       Media.MIME_TYPE + " =? OR " +
//                       Media.MIME_TYPE + " =?)";
//        Log.v(TAG, "selection() : " + query);
//        return query;
//    }
//
//    private static String[] selectionArg() {
//        Log.v(TAG, "selectionArg() : " + TranscodedMediaSource.TRANSCODED_TAG_ID);
//        return new String [] {
//                    TranscodedMediaSource.TRANSCODED_TAG_ID,
//                    "video/mp4",
//                    "video/3gpp",
//                    "video/webm",
//                    "video/x-matroska"};
//    }

    private static String sortOrder8() {
        return Media.DATE_TAKEN + " DESC LIMIT 8";
    }

    private static String sortOrder16() {
        return Media.DATE_TAKEN + " DESC LIMIT 16";
    }

    private Cursor query() {
        if (mCursor == null) {
            mCursor = mCr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, selection(), selectionArg(), sortOrder16());
            Log.v(TAG, "query() : " + mCursor);
        }
        return mCursor;
    }

    public int getMediaCount() {
        query();
        if (mCursor != null) {
            mCounts = mCursor.getCount();
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
            return clip;
        }
        return null;
    }

    public void close() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public Uri getMediaUri(Context ctx, int index) {
        if (index >= mCounts) {
            return null;
        }
        query();
        if (mCursor == null || !mCursor.moveToPosition(index)) {
            return null;
        }

        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(0));
    }

    public static Cursor queryLatest8(ContentResolver cr) {
        Cursor c = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                PROJECTION, selection(), selectionArg(), sortOrder8());
        Log.v(TAG, "query() count : " + c.getCount());
        return c;
    }

    public static Cursor query(ContentResolver cr, Uri uri, String[] projection) {
        return cr.query(uri, projection, null, null, null);
    }
}
