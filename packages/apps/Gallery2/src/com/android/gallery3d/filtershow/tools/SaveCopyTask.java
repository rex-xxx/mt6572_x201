/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.filtershow.tools;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.presets.ImagePreset;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.XmpUtilHelper;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
/**
 * Asynchronous task for saving edited photo as a new copy.
 */
public class SaveCopyTask extends AsyncTask<ImagePreset, Void, Uri> {


    private static final String LOGTAG = "Gallery2/SaveCopyTask";
    private static final int DEFAULT_COMPRESS_QUALITY = 95;
    private static final String DEFAULT_SAVE_DIRECTORY = "EditedOnlinePhotos";
    ///M: failed type @{
    private static final int SPACE_FULL_FAILED_SAVE = -1;
    private static final int FAILED_SAVE = -2;
    private static final int SUCCEED_SAVE = 0;
    private static int mFailedType;
    ///}@

    /**
     * Saves the bitmap in the final destination
     */
    public static boolean saveBitmap(Bitmap bitmap, File destination, Object xmp) {
        OutputStream os = null;
        ///M:
        boolean result;
        try {
            os = new FileOutputStream(destination);
            result = bitmap.compress(CompressFormat.JPEG, DEFAULT_COMPRESS_QUALITY, os);
        } catch (FileNotFoundException e) {
            Log.v(LOGTAG,"Error in writing "+destination.getAbsolutePath());
            ///M:
            result = false;
        } finally {
            closeStream(os);
        }
        if (xmp != null) {
            XmpUtilHelper.writeXMPMeta(destination.getAbsolutePath(), xmp);
        }
        return result;
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Callback for the completed asynchronous task.
     */
    public interface Callback {

        void onComplete(Uri result, int failedType);
    }

    private interface ContentResolverQueryCallback {

        void onCursorResult(Cursor cursor);
    }

    private static final String TIME_STAMP_NAME = "'IMG'_yyyyMMdd_HHmmss";

    private final Context context;
    private final Uri sourceUri;
    private final Callback callback;
    private final String saveFileName;
    private final File destinationFile;

    public SaveCopyTask(Context context, Uri sourceUri, File destination, Callback callback) {
        this.context = context;
        this.sourceUri = sourceUri;
        this.callback = callback;

        if (destination == null) {
            this.destinationFile = getNewFile(context, sourceUri);
        } else {
            this.destinationFile = destination;
        }

        saveFileName = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(
                System.currentTimeMillis()));
    }

    public static File getFinalSaveDirectory(Context context, Uri sourceUri) {
        File saveDirectory = getSaveDirectory(context, sourceUri);
        if ((saveDirectory == null) || !saveDirectory.canWrite()) {
            saveDirectory = new File(Environment.getExternalStorageDirectory(),
                    DEFAULT_SAVE_DIRECTORY);
        }
        // Create the directory if it doesn't exist
        if (!saveDirectory.exists()) saveDirectory.mkdirs();
        return saveDirectory;
    }

    public static File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(
                System.currentTimeMillis()));
        return new File(saveDirectory, filename + ".JPG");
    }

    private Bitmap loadMutableBitmap(BitmapFactory.Options options) throws FileNotFoundException {
        // TODO: on <3.x we need a copy of the bitmap (inMutable doesn't
        // exist)
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(sourceUri, "r");
        FileDescriptor fd = null;
        Bitmap bitmap = null;
        if (pfd != null) {
            fd = pfd.getFileDescriptor();
        }
        try {
            if (fd != null) {
                bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
            }
        } catch (OutOfMemoryError e) {
            /// M: As there is a chance no enough dvm memory for decoded Bitmap,
            //Skia will return a null Bitmap. In this case, we have to
            //downscale the decoded Bitmap by increase the options.inSampleSize
            final int maxTryNum = 8;
            for (int i=0; i < maxTryNum; i++) {
                //we increase inSampleSize to expect a smaller Bitamp
                options.inSampleSize *= 2;
                Log.w(LOGTAG,"getCroppedImage:try for sample size::" + options.inSampleSize);
                try {
                    bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
                } catch (OutOfMemoryError e1) {
                    Log.w(LOGTAG,"  saveBitmap :out of memory when decoding:"+e1);
                    bitmap = null;
                }
                if (bitmap != null) break;
            }
        } finally {
            if (pfd != null) {
                Utils.closeSilently(pfd);
            }
            if (bitmap != null) {
                int orientation = ImageLoader.getOrientation(context, sourceUri);
                bitmap = ImageLoader.rotateToPortrait(bitmap, orientation);
            }
            if(bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
                return bitmap;
            } else {
                return bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
        }
    }

    private static final String[] COPY_EXIF_ATTRIBUTES = new String[] {
        ExifInterface.TAG_APERTURE,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_ISO,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_WHITE_BALANCE,
    };

    private static void copyExif(String sourcePath, String destPath) {
        try {
            ExifInterface source = new ExifInterface(sourcePath);
            ExifInterface dest = new ExifInterface(destPath);
            boolean needsSave = false;
            for (String tag : COPY_EXIF_ATTRIBUTES) {
                String value = source.getAttribute(tag);
                if (value != null) {
                    needsSave = true;
                    dest.setAttribute(tag, value);
                }
            }
            if (needsSave) {
                dest.saveAttributes();
            }
        } catch (IOException ex) {
            Log.w(LOGTAG, "Failed to copy exif metadata", ex);
        }
    }

    private void copyExif(Uri sourceUri, String destPath) {
        if (ContentResolver.SCHEME_FILE.equals(sourceUri.getScheme())) {
            copyExif(sourceUri.getPath(), destPath);
            return;
        }

        final String[] PROJECTION = new String[] {
                ImageColumns.DATA
        };
        try {
            Cursor c = context.getContentResolver().query(sourceUri, PROJECTION,
                    null, null, null);
            if (c.moveToFirst()) {
                String path = c.getString(0);
                if (new File(path).exists()) {
                    copyExif(path, destPath);
                }
            }
            c.close();
        } catch (Exception e) {
            Log.w(LOGTAG, "Failed to copy exif", e);
        }
    }

    /**
     * The task should be executed with one given bitmap to be saved.
     */

    /// M: add for recycling resources
    private Bitmap mOriBitmap = null;
    private Bitmap mNewBitmap = null;
    @Override
    protected Uri doInBackground(ImagePreset... params) {
        // TODO: Support larger dimensions for photo saving.
        if (params[0] == null) {
            return null;
        }

        ImagePreset preset = params[0];
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 1;
            options.inMutable = true;
            Bitmap mBitmap = loadMutableBitmap(options);
            Bitmap bitmap = null;
            /// M: Decode the crop region image from origin image, may cause out of memory issue.
            //  So we should decode the origin image again using large inSampleSize.
            try {
                bitmap = preset.apply(mBitmap);
            } catch (OutOfMemoryError e) {
                final int maxTryNum = 8;
                for (int i=0; i < maxTryNum; i++) {
                    mBitmap.recycle();
                    options.inSampleSize *= 2;
                    mBitmap = loadMutableBitmap(options);
                    //we increase inSampleSize to expect a smaller Bitamp
                    Log.w(LOGTAG,"doInBackground:getCroppedImage:try for sample size::" + options.inSampleSize);
                    try {
                        bitmap = preset.apply(mBitmap);
                    } catch (OutOfMemoryError e1) {
                        Log.w(LOGTAG,"  doInBackground:saveBitmap :out of memory when decoding:"+e1);
                        bitmap = null;
                    }
                    if (bitmap != null) break;
                }
            }//catch
            /// M:
            mOriBitmap = mBitmap;
            mNewBitmap = bitmap;
            Object xmp = null;
            InputStream is = null;
            if (preset.isPanoramaSafe()) {
                is = context.getContentResolver().openInputStream(sourceUri);
                xmp =  XmpUtilHelper.extractXMPMeta(is);
            }
            boolean result = saveBitmap(bitmap, this.destinationFile, xmp);
            ///M:save failed,space full, return null
            if(false == result) {
                mFailedType = SPACE_FULL_FAILED_SAVE;
                return null;
            }
            copyExif(sourceUri, destinationFile.getAbsolutePath());

            Uri uri = insertContent(context, sourceUri, this.destinationFile, saveFileName);
            /// M:
            // bitmap.recycle();
            return uri;

        } catch (FileNotFoundException ex) {
            Log.w(LOGTAG, "Failed to save image!", ex);
            ///M: save failed
            mFailedType = FAILED_SAVE;
            return null;
        } catch (Exception e) {
            Log.w(LOGTAG, "Failed to save image!", e);
            ///M: save failed
            mFailedType = FAILED_SAVE;
            return null;
        } finally {
            /// M:
            if (mOriBitmap != null) {
                mOriBitmap.recycle();
                mOriBitmap = null;
            }
            if (mNewBitmap != null) {
                mNewBitmap.recycle();
                mNewBitmap = null;
            }
        }
    }

    @Override
    protected void onPostExecute(Uri result) {
        if (callback != null) {
            callback.onComplete(result, mFailedType);
        }
    }

    private static void querySource(Context context, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null,
                    null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static File getSaveDirectory(Context context, Uri sourceUri) {
        final File[] dir = new File[1];
        querySource(context, sourceUri, new String[] {
                ImageColumns.DATA
        },
                new ContentResolverQueryCallback() {

                    @Override
                    public void onCursorResult(Cursor cursor) {
                        dir[0] = new File(cursor.getString(0)).getParentFile();
                    }
                });
        return dir[0];
    }

    /**
     * Insert the content (saved file) with proper source photo properties.
     */
    public static Uri insertContent(Context context, Uri sourceUri, File file, String saveFileName) {
        long now = System.currentTimeMillis() / 1000;

        final ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, saveFileName);
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATE_TAKEN, now);
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());
        /// M: insert ImageWidth and Height to DB to fix bug:
        /// the Width and Height of cropped picture are 0 in DB @{
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int imageLength = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
            int imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
            values.put(Images.Media.WIDTH, imageWidth);
            values.put(Images.Media.HEIGHT, imageLength);
        } catch (IOException ex) {
            Log.w(LOGTAG, "ExifInterface throws IOException", ex);
        }
        /// @}
        final String[] projection = new String[] {
                ImageColumns.DATE_TAKEN,
                ImageColumns.LATITUDE, ImageColumns.LONGITUDE,
        };
        querySource(context, sourceUri, projection,
                new ContentResolverQueryCallback() {

            @Override
            public void onCursorResult(Cursor cursor) {
                values.put(Images.Media.DATE_TAKEN, cursor.getLong(0));

                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                // TODO: Change || to && after the default location issue is
                // fixed.
                if ((latitude != 0f) || (longitude != 0f)) {
                    values.put(Images.Media.LATITUDE, latitude);
                    values.put(Images.Media.LONGITUDE, longitude);
                }
            }
        });

        return context.getContentResolver().insert(
                Images.Media.EXTERNAL_CONTENT_URI, values);
    }

}
