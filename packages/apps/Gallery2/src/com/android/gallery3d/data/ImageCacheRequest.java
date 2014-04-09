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

package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.BytesBufferPool.BytesBuffer;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

abstract class ImageCacheRequest implements Job<Bitmap> {
    private static final String TAG = "Gallery2/ImageCacheRequest";

    protected GalleryApp mApplication;
//    private Path mPath;
    protected Path mPath;
    private int mType;
    private int mTargetSize;
    private long mDateModifiedInSec;

    public ImageCacheRequest(GalleryApp application,
            Path path, int type, int targetSize, long dateModifiedInSec) {
        mApplication = application;
        mPath = path;
        mType = type;
        mTargetSize = targetSize;
        mDateModifiedInSec = dateModifiedInSec;
    }

    private String debugTag() {
        return mPath + "," +
                 ((mType == MediaItem.TYPE_THUMBNAIL) ? "THUMB" :
                 (mType == MediaItem.TYPE_MICROTHUMBNAIL) ? "MICROTHUMB" : "?");
    }
    
    @Override
    public Bitmap run(JobContext jc) {
        ImageCacheService cacheService = mApplication.getImageCacheService();

        BytesBuffer buffer = MediaItem.getBytesBufferPool().get();
        try {
            //boolean found = cacheService.getImageData(mPath, mType, buffer);
            boolean found = cacheService.getImageData(mPath, mType, buffer, mDateModifiedInSec);
            // if support picture quality tuning, we decode bitmap from origin image
            // in order to apply picture quality every time
            if (MtkLog.SUPPORT_PQ) found = false;
            if (jc.isCancelled()) return null;
            if (found) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap;
                if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                    bitmap = DecodeUtils.decode(jc,
                            buffer.data, buffer.offset, buffer.length, options,
                            MediaItem.getMicroThumbPool());
                } else {
                    bitmap = DecodeUtils.decode(jc,
                            buffer.data, buffer.offset, buffer.length, options,
                            MediaItem.getThumbPool());
                }
                if (bitmap == null && !jc.isCancelled()) {
                    Log.w(TAG, "decode cached failed " + debugTag());
                }
                /// M: dump Skia decoded cache Bitmap for debug @{
                if (MtkLog.DBG) {
                    long dumpStart = System.currentTimeMillis();
                    String fileType;
                    if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                        fileType = "MicroTNail";
                    } else {
                        fileType = "TNail";
                    }
                    MediaItem item = (MediaItem) mPath.getObject();
                    if (item != null) {
                        String string = item.getName() + "_dumpcacheBitmap" + fileType;
                        Log.i(TAG, "string " + string);
                        MtkUtils.dumpBitmap(bitmap, string);
                        Log.i(TAG, " Dump cached Bitmap time " + (System.currentTimeMillis() - dumpStart));
                    }
                }
                /// @}
                return bitmap;
            }
        } finally {
            MediaItem.getBytesBufferPool().recycle(buffer);
        }
        Bitmap bitmap = onDecodeOriginal(jc, mType);
        if (jc.isCancelled()) return null;

        if (bitmap == null) {
            Log.w(TAG, "decode orig failed " + debugTag());
            return null;
        }
        /// M: dump Skia decoded origin Bitmap for debug @{
        if (MtkLog.DBG) {
            long dumpStart = System.currentTimeMillis();
            String fileType;
            if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                fileType = "MicroTNail";
            } else {
                fileType = "TNail";
            }
            MediaItem item = (MediaItem) mPath.getObject();
            if (item != null) {
                String string = item.getName() + "_dumpOriginBitmap" + fileType;
                Log.i(TAG, "string " + string);
                MtkUtils.dumpBitmap(bitmap, string);
                Log.i(TAG, " Dump Origin Bitmap time " + (System.currentTimeMillis() - dumpStart));
            }
        }
        /// @}
        if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
            bitmap = BitmapUtils.resizeAndCropCenter(bitmap, mTargetSize, true);
        } else {
            bitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
        }
        if (jc.isCancelled()) return null;

        byte[] array = BitmapUtils.compressToBytes(bitmap);
        if (jc.isCancelled()) return null;

        //cacheService.putImageData(mPath, mType, array);
        // if support picture quality tuning, we don't write data to cache in order to improve performance
        if (!MtkLog.SUPPORT_PQ) {
            cacheService.putImageData(mPath, mType, array, mDateModifiedInSec);
        }
        return bitmap;
    }

    public abstract Bitmap onDecodeOriginal(JobContext jc, int targetSize);
}
