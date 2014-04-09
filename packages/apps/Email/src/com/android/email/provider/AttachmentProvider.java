/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.email.provider;

import com.android.email.R;

import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.AttachmentColumns;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.emailcommon.utility.AttachmentUtilities.Columns;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/*
 * A simple ContentProvider that allows file access to Email's attachments.
 *
 * The URI scheme is as follows.  For raw file access:
 *   content://com.android.email.attachmentprovider/acct#/attach#/RAW
 *
 * And for access to thumbnails:
 *   content://com.android.email.attachmentprovider/acct#/attach#/THUMBNAIL/width#/height#
 *
 * The on-disk (storage) schema is as follows.
 *
 * Attachments are stored at:  <database-path>/account#.db_att/item#
 * Thumbnails are stored at:   <cache-path>/thmb_account#_item#
 *
 * Using the standard application context, account #10 and attachment # 20, this would be:
 *      /data/data/com.android.email/databases/10.db_att/20
 *      /data/data/com.android.email/cache/thmb_10_20
 */
public class AttachmentProvider extends ContentProvider {

    private static final String[] MIME_TYPE_PROJECTION = new String[] {
            AttachmentColumns.MIME_TYPE, AttachmentColumns.FILENAME };
    private static final int MIME_TYPE_COLUMN_MIME_TYPE = 0;
    private static final int MIME_TYPE_COLUMN_FILENAME = 1;
    private static final String TAG = "AttachmentProvider";
    private static final String[] PROJECTION_QUERY = new String[] { AttachmentColumns.FILENAME,
            AttachmentColumns.SIZE, AttachmentColumns.CONTENT_URI };

    public static final String JPS_MIME_TYPE = "image/x-jps";
    //this mime type is mainly to support un-common mime-types imposed by
    //other source
    public static final String JPS_MIME_TYPE2 = "image/jps";

    @Override
    public boolean onCreate() {
        /*
         * We use the cache dir as a temporary directory (since Android doesn't give us one) so
         * on startup we'll clean up any .tmp files from the last run.
         */
        File[] files = getContext().getCacheDir().listFiles();
        for (File file : files) {
            String filename = file.getName();
            if (filename.endsWith(".tmp") || filename.startsWith("thmb_")) {
                file.delete();
            }
        }
        return true;
    }

    /**
     * Returns the mime type for a given attachment.  There are three possible results:
     *  - If thumbnail Uri, always returns "image/png" (even if there's no attachment)
     *  - If the attachment does not exist, returns null
     *  - Returns the mime type of the attachment
     */
    @Override
    public String getType(Uri uri) {
        long callingId = Binder.clearCallingIdentity();
        try {
            List<String> segments = uri.getPathSegments();
            String id = segments.get(1);
            String format = segments.get(2);
            if (AttachmentUtilities.FORMAT_THUMBNAIL.equals(format)) {
                return "image/png";
            } else {
                uri = ContentUris.withAppendedId(Attachment.CONTENT_URI, Long.parseLong(id));
                Cursor c = getContext().getContentResolver().query(uri, MIME_TYPE_PROJECTION, null,
                        null, null);
                try {
                    if (c.moveToFirst()) {
                        String mimeType = c.getString(MIME_TYPE_COLUMN_MIME_TYPE);
                        String fileName = c.getString(MIME_TYPE_COLUMN_FILENAME);
                        mimeType = AttachmentUtilities.inferMimeType(fileName, mimeType);
                        return mimeType;
                    }
                } finally {
                    c.close();
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /**
     * Open an attachment file.  There are two "formats" - "raw", which returns an actual file,
     * and "thumbnail", which attempts to generate a thumbnail image.
     *
     * Thumbnails are cached for easy space recovery and cleanup.
     *
     * TODO:  The thumbnail format returns null for its failure cases, instead of throwing
     * FileNotFoundException, and should be fixed for consistency.
     *
     *  @throws FileNotFoundException
     */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        // If this is a write, the caller must have the EmailProvider permission, which is
        // based on signature only
        if (mode.equals("w")) {
            Context context = getContext();
            if (context.checkCallingPermission(EmailContent.PROVIDER_PERMISSION)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new FileNotFoundException();
            }
            List<String> segments = uri.getPathSegments();
            String accountId = segments.get(0);
            String id = segments.get(1);
            File saveIn =
                AttachmentUtilities.getAttachmentDirectory(context, Long.parseLong(accountId));
            if (!saveIn.exists()) {
                saveIn.mkdirs();
            }
            File newFile = new File(saveIn, id);
            if (newFile.exists()) {
                Logging.d(
                        "Delete the incomplete attachment temp file before the brand new writing");
                if (!newFile.delete()) {
                    Logging.d("Delete the attachment temp file failed");
                }
            }
            return ParcelFileDescriptor.open(
                    newFile, ParcelFileDescriptor.MODE_READ_WRITE |
                        ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            List<String> segments = uri.getPathSegments();
            String accountId = segments.get(0);
            String id = segments.get(1);
            String format = segments.get(2);
            if (AttachmentUtilities.FORMAT_THUMBNAIL.equals(format)) {
                int width = Integer.parseInt(segments.get(3));
                int height = Integer.parseInt(segments.get(4));
                String filename = "thmb_" + accountId + "_" + id;
                File dir = getContext().getCacheDir();
                File file = new File(dir, filename);
                if (!file.exists()) {
                    Uri attachmentUri = AttachmentUtilities.
                        getAttachmentUri(Long.parseLong(accountId), Long.parseLong(id));
                    Cursor c = query(attachmentUri,
                            new String[] { Columns.DATA }, null, null, null);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                attachmentUri = Uri.parse(c.getString(0));
                            } else {
                                Logging.d(TAG, "openFile/thumbnail failed with"
                                        + " attachmentUri could not be found.");
                                return null;
                            }
                        } finally {
                            c.close();
                        }
                    }
                    String type = getContext().getContentResolver().getType(attachmentUri);
                    try {
                        InputStream in =
                            getContext().getContentResolver().openInputStream(attachmentUri);
                        Bitmap thumbnail = createThumbnail(attachmentUri, type, in, width, height);
                        if (thumbnail == null) {
                            return null;
                        }
                        thumbnail = Bitmap.createScaledBitmap(thumbnail, width, height, true);
                        FileOutputStream out = new FileOutputStream(file);
                        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.close();
                        in.close();
                    } catch (IOException ioe) {
                        Log.d(Logging.LOG_TAG, "openFile/thumbnail failed with " +
                                ioe.getMessage());
                        return null;
                    } catch (OutOfMemoryError oome) {
                        Log.d(Logging.LOG_TAG, "openFile/thumbnail failed with " +
                                oome.getMessage());
                        return null;
                    }
                }
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
            else {
                return ParcelFileDescriptor.open(
                        new File(getContext().getDatabasePath(accountId + ".db_att"), id),
                        ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    /**
     * Returns a cursor based on the data in the attachments table, or null if the attachment
     * is not recorded in the table.
     *
     * Supports REST Uri only, for a single row - selection, selection args, and sortOrder are
     * ignored (non-null values should probably throw an exception....)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
            Logging.d(TAG, "query uri = " + uri + " selection = " + selection);
        long callingId = Binder.clearCallingIdentity();
        try {
            if (projection == null) {
                projection =
                    new String[] {
                        Columns._ID,
                        Columns.DATA,
                        Columns.DISPLAY_NAME,
                        Columns.SIZE,
                };
            }

            List<String> segments = uri.getPathSegments();
            String accountId = segments.get(0);
            String id = segments.get(1);
            String format = segments.get(2);
            String name = null;
            int size = -1;
            String contentUri = null;

            uri = ContentUris.withAppendedId(Attachment.CONTENT_URI, Long.parseLong(id));
            Cursor c = getContext().getContentResolver().query(uri, PROJECTION_QUERY,
                    null, null, null);
            try {
                if (c.moveToFirst()) {
                    name = c.getString(0);
                    size = c.getInt(1);
                    contentUri = c.getString(2);
                } else {
                    return null;
                }
            } finally {
                c.close();
            }

            MatrixCursor ret = new MatrixCursor(projection);
            Object[] values = new Object[projection.length];
            for (int i = 0, count = projection.length; i < count; i++) {
                String column = projection[i];
                if (Columns._ID.equals(column)) {
                    values[i] = id;
                    Logging.d(TAG, "Set Columns._ID = " + id);
                } else if (Columns.DATA.equals(column)) {
                    values[i] = contentUri;
                    Logging.d(TAG, "Set Columns.DATA = " + contentUri);
                } else if (Columns.DISPLAY_NAME.equals(column)) {
                    values[i] = name;
                    Logging.d(TAG, "Set Columns.DISPLAY_NAME = " + name);
                } else if (Columns.SIZE.equals(column)) {
                    values[i] = size;
                    Logging.d(TAG, "Set Columns.SIZE = " + size);
                }
            }
            ret.addRow(values);
            return ret;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private Bitmap createThumbnail(Uri uri, String type, InputStream data,
            int width, int height) {
        if(MimeUtility.mimeTypeMatches(type, "image/*")) {
            Options opts = new Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(data, null, opts);
            InputStream in = null;
            try {
                in = getContext().getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (JPS_MIME_TYPE.equals(type) || JPS_MIME_TYPE2.equals(type)) {
                return create3DImageThumbnail(in, opts.outWidth, opts.outHeight , width, height);
            }
            return createImageThumbnail(in, opts.outWidth, opts.outHeight , width, height);
        }
        return null;
    }

    /**
     * M: Create thumbnail from a 3D image
     * @param data the 3D image input stream
     * @param sourceWidth the width of the 3D image
     * @param sourceHeight the height of the 3D image
     * @param decodeWidth the destination width of the thumbnail
     * @param decodeHeight the destination height of the thumbnail
     * @return the thumbnail of the 3D image
     */
    private Bitmap create3DImageThumbnail(InputStream data, int sourceWidth, int sourceHeight,
            int decodeWidth, int decodeHeight) {
        Logging.d("create3DImageThumbnail with sourceWidth=" + sourceWidth + ", sourceHeight="
                + sourceHeight + "decodeWidth=" + decodeWidth + ", decodeHeight=" + decodeHeight);
        try {
            // 3D image contains two internal images which layout from left to right 
            // or layout from top to bottom. We just assume the 3D image has a left
            // to right layout, and we fetch its left part to generate the thumbnail.
            sourceWidth = sourceWidth/2;

            // TODO: Actually, there is a "layout" tag in the specified JPS specification,
            // and we should parse the original file to get this layout, and decide
            // which part of this image is dedicated firstFrame, and return its Rect. 

            Rect rect = new Rect(0, 0, sourceWidth, sourceHeight);

            int withSample = sourceWidth/decodeWidth;
            int heightSample = sourceHeight/decodeHeight;
            int downSample = withSample > heightSample ? heightSample : withSample;
            Options opts = new Options();
            opts.inSampleSize = downSample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            BitmapRegionDecoder regionDecoder = BitmapRegionDecoder.newInstance(data, false);
            Bitmap bitmap = regionDecoder.decodeRegion(rect, opts);

            int regionWidth = bitmap.getWidth();
            int regionHeight = bitmap.getHeight();
            int cropX = 0;
            int cropY = 0;
            int cropWidth = regionWidth > decodeWidth ? decodeWidth : regionWidth;
            int cropHeight = regionHeight > decodeHeight ? decodeHeight : regionHeight;
            if (regionWidth > decodeWidth) {
                cropX = (regionWidth - decodeWidth) / 2;
            }
            if (regionHeight > decodeHeight) {
                cropY = (regionHeight - decodeHeight) / 2;
            }

            bitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight);
            bitmap = draw3DImageTypeOverlay(getContext(), bitmap);
            return bitmap;
        } catch (OutOfMemoryError oome) {
            /*
             * Improperly downloaded images, corrupt bitmaps and the like can commonly
             * cause OOME due to invalid allocation sizes. We're happy with a null bitmap in
             * that case. If the system is really out of memory we'll know about it soon
             * enough.
             */
            Logging.d(TAG, "openFile/create3DImageThumbnail failed with OutOfMemoryError "
                    + oome.getMessage());
            return null;
        } catch (Exception e) {
            Logging.d(TAG, "openFile/create3DImageThumbnail failed with Exception "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * M: overlay a 3D icon on the Bitmap
     * @param context the context used to read resources
     * @param bitmap the original bitmap
     * @return the bitmap overlaid with a 3D icon
     */
    public static Bitmap draw3DImageTypeOverlay(Context context, Bitmap bitmap) {
        Drawable sStereoOverlay = context.getResources().getDrawable(R.drawable.ic_stereo_overlay);
        int width = sStereoOverlay.getIntrinsicWidth();
        int height = sStereoOverlay.getIntrinsicHeight();
        Logging.d(TAG, "original stereo overlay w=" + width + ", h=" + height);
        float aspectRatio = (float) width / (float) height;
        final int overlayRadio = 3;
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        Logging.d(TAG, "scaled stereo overlay bmpWidth=" + bmpWidth + ", bmpHeight=" + bmpHeight);
        boolean heightSmaller = (bmpHeight < bmpWidth);
        int scaleResult = (heightSmaller ? bmpHeight : bmpWidth) / overlayRadio;
        if (heightSmaller) {
            height = scaleResult;
            width = (int)((float) scaleResult * aspectRatio);
        } else {
            width = scaleResult;
            height = (int)((float) width / aspectRatio);
        }

        int left = 0;
        int bottom = bmpHeight;
        int top = bottom - height;
        int right = width + left;
        Logging.d(TAG, "stereo overlay drawing dimension=(" + left + ", " + top + ", " + right
                + ", " + bottom + ")");
        sStereoOverlay.setBounds(left, top, right, bottom);
        if (bitmap.isMutable() == false) {
            bitmap = bitmap.copy(bitmap.getConfig(), true);
        }
        Canvas tmpCanvas = new Canvas(bitmap);
        sStereoOverlay.draw(tmpCanvas);
        return bitmap;
    }

    private Bitmap createImageThumbnail(InputStream data, int sourceWidth, int sourceHeight,
            int decodeWidth, int decodeHeight) {
        try {
            // reduce decode memory consumption.
            int downSampleBaseValue = sourceWidth > sourceHeight ? sourceWidth: sourceHeight;
            int decodeValue = decodeWidth > decodeHeight ? decodeWidth: decodeHeight;
            int downSample = downSampleBaseValue / decodeValue;
            Options opts = new Options();
            opts.inSampleSize = downSample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeStream(data, null, opts);
            return bitmap;
        } catch (OutOfMemoryError oome) {
            /*
             * Improperly downloaded images, corrupt bitmaps and the like can commonly
             * cause OOME due to invalid allocation sizes. We're happy with a null bitmap in
             * that case. If the system is really out of memory we'll know about it soon
             * enough.
             */
            Logging.d(TAG, "openFile/createImageThumbnail failed with OutOfMemoryError "
                    + oome.getMessage());
            return null;
        } catch (Exception e) {
            Logging.d(TAG, "openFile/createImageThumbnail failed with Exception " + e.getMessage());
            return null;
        }
    }

    /**
     * Need this to suppress warning in unit tests.
     */
    @Override
    public void shutdown() {
        // Don't call super.shutdown(), which emits a warning...
    }
}
