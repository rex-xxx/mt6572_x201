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

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.util.UpdateHelper;

// M: mtk import
import com.mediatek.drm.OmaDrmStore;
import java.io.File;

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;

// LocalVideo represents a video in the local storage.
public class LocalVideo extends LocalMediaItem {
    private static final String TAG = "Gallery2/LocalVideo";
    static final Path ITEM_PATH = Path.fromString("/local/video/item");

    public static Path getItemPath(int mtkInclusion) {
        if (0 != mtkInclusion) {
            // M: retrieve path with respect to mtk inclusion
            return Path.fromString("/local/video/item", mtkInclusion);
        } else {
            return ITEM_PATH;
        }
    }

    //added to support Mediatek features
    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();

    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_DURATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_RESOLUTION = 12;
    //added to support DRM
    private static final int INDEX_IS_DRM = 13;
    private static final int INDEX_DRM_METHOD = 14;
    //added to support Stereo display
    private static final int INDEX_STEREO_TYPE = 15;

    static final String[] PROJECTION = new String[] {
            VideoColumns._ID,
            VideoColumns.TITLE,
            VideoColumns.MIME_TYPE,
            VideoColumns.LATITUDE,
            VideoColumns.LONGITUDE,
            VideoColumns.DATE_TAKEN,
            VideoColumns.DATE_ADDED,
            VideoColumns.DATE_MODIFIED,
            VideoColumns.DATA,
            VideoColumns.DURATION,
            VideoColumns.BUCKET_ID,
            VideoColumns.SIZE,
            VideoColumns.RESOLUTION,
            //added to support DRM
            VideoColumns.IS_DRM,
            VideoColumns.DRM_METHOD,
            //added to support stereo display
            Video.Media.STEREO_TYPE,
    };

    private final GalleryApp mApplication;

    public int durationInSec;

    public LocalVideo(Path path, GalleryApp application, Cursor cursor) {
        super(path, nextVersionNumber());
        mApplication = application;
        loadFromCursor(cursor);
    }

    public LocalVideo(Path path, GalleryApp context, int id) {
        super(path, nextVersionNumber());
        mApplication = context;
        ContentResolver resolver = mApplication.getContentResolver();
        Uri uri = Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = LocalAlbum.getItemCursor(resolver, uri, PROJECTION, id);
        if (cursor == null) {
            throw new RuntimeException("cannot get cursor for: " + path);
        }
        try {
            if (cursor.moveToNext()) {
                loadFromCursor(cursor);
            } else {
                throw new RuntimeException("cannot find data for: " + path);
            }
        } finally {
            cursor.close();
        }
    }

    private void loadFromCursor(Cursor cursor) {
        id = cursor.getInt(INDEX_ID);
        caption = cursor.getString(INDEX_CAPTION);
        mimeType = cursor.getString(INDEX_MIME_TYPE);
        latitude = cursor.getDouble(INDEX_LATITUDE);
        longitude = cursor.getDouble(INDEX_LONGITUDE);
        dateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        //added to avoid fake data changed judgement in
        //updateFromCursor function-begin
        dateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        dateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        //end
        filePath = cursor.getString(INDEX_DATA);
        durationInSec = cursor.getInt(INDEX_DURATION) / 1000;
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        parseResolution(cursor.getString(INDEX_RESOLUTION));
        //added to support drm feature
        if (mIsDrmSupported) {
            is_drm = cursor.getInt(INDEX_IS_DRM);
            drm_method = cursor.getInt(INDEX_DRM_METHOD);
        }
        if (mIsStereoDisplaySupported) {
            stereoType = cursor.getInt(INDEX_STEREO_TYPE);
        }
    }

    private void parseResolution(String resolution) {
        if (resolution == null) return;
        int m = resolution.indexOf('x');
        if (m == -1) return;
        try {
            int w = Integer.parseInt(resolution.substring(0, m));
            int h = Integer.parseInt(resolution.substring(m + 1));
            width = w;
            height = h;
        } catch (Throwable t) {
            Log.w(TAG, t);
        }
    }

    @Override
    protected boolean updateFromCursor(Cursor cursor) {
        UpdateHelper uh = new UpdateHelper();
        id = uh.update(id, cursor.getInt(INDEX_ID));
        caption = uh.update(caption, cursor.getString(INDEX_CAPTION));
        mimeType = uh.update(mimeType, cursor.getString(INDEX_MIME_TYPE));
        latitude = uh.update(latitude, cursor.getDouble(INDEX_LATITUDE));
        longitude = uh.update(longitude, cursor.getDouble(INDEX_LONGITUDE));
        dateTakenInMs = uh.update(
                dateTakenInMs, cursor.getLong(INDEX_DATE_TAKEN));
        dateAddedInSec = uh.update(
                dateAddedInSec, cursor.getLong(INDEX_DATE_ADDED));
        dateModifiedInSec = uh.update(
                dateModifiedInSec, cursor.getLong(INDEX_DATE_MODIFIED));
        filePath = uh.update(filePath, cursor.getString(INDEX_DATA));
        durationInSec = uh.update(
                durationInSec, cursor.getInt(INDEX_DURATION) / 1000);
        bucketId = uh.update(bucketId, cursor.getInt(INDEX_BUCKET_ID));
        fileSize = uh.update(fileSize, cursor.getLong(INDEX_SIZE));
        //added to support drm feature
        if (mIsDrmSupported) {
            is_drm = uh.update(is_drm,cursor.getInt(INDEX_IS_DRM));
            drm_method = uh.update(drm_method,cursor.getInt(INDEX_DRM_METHOD));
        }
        if (mIsStereoDisplaySupported) {
            stereoType = uh.update(stereoType,cursor.getInt(INDEX_STEREO_TYPE));
        }
        return uh.isUpdated();
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
        return new LocalVideoRequest(mApplication, getPath(), type, filePath,
                                     dateModifiedInSec);
    }

    public static class LocalVideoRequest extends ImageCacheRequest {
        private String mLocalFilePath;

        LocalVideoRequest(GalleryApp application, Path path, int type,
                String localFilePath, long dateModifiedInSec) {
            super(application, path, type, MediaItem.getTargetSize(type),
                                                        dateModifiedInSec);
            mLocalFilePath = localFilePath;
        }

        @Override
        public Bitmap onDecodeOriginal(JobContext jc, int type) {
            long logTimeBefore;
            logTimeBefore = System.currentTimeMillis();
            Log.i(TAG, "create video thumb begins at" + logTimeBefore);
            Bitmap bitmap = BitmapUtils.createVideoThumbnail(mLocalFilePath);
            Log.i(TAG, "create video thumb costs "
                    + (System.currentTimeMillis() - logTimeBefore));
            if (bitmap == null || jc.isCancelled()) return null;
            if (mIsStereoDisplaySupported) {
                DataManager manager = mApplication.getDataManager();
                LocalMediaItem item = (LocalMediaItem) manager.getMediaObject(mPath);
                int stereoType = item.stereoType;
                if (Video.Media.STEREO_TYPE_2D != stereoType) {
                    Bitmap temp = StereoHelper.getStereoVideoImage(jc, bitmap, 
                                                             true, stereoType);
                    bitmap.recycle();
                    bitmap = temp;
                }
            }
            return bitmap;
        }
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
        throw new UnsupportedOperationException("Cannot regquest a large image"
                + " to a local video!");
    }

    @Override
    public Job<MediatekFeature.DataBundle> 
        requestImage(int type, MediatekFeature.Params params) {
        return new LocalImageRequest(mApplication, getPath(), type, filePath);
    }

    public static class LocalImageRequest implements 
                                       Job<MediatekFeature.DataBundle> {
        private GalleryApp mApplication;
        private Path mPath;
        private int mType;
        private int mTargetSize;
        private String mLocalFilePath;

        LocalImageRequest(GalleryApp application, Path path, int type,
                String localFilePath) {
            mApplication = application;
            mPath = path;
            mType = type;
            //mTargetSize = getTargetSize(type);
            mLocalFilePath = localFilePath;
        }

        public MediatekFeature.DataBundle run(JobContext jc) {

            if (!mIsStereoDisplaySupported) {
                Log.e(TAG,"LocalSecondImageRequest:Stereo is not supported!");
                return null;
            }

            if (null == mLocalFilePath) {
                Log.w(TAG,"LocalSecondImageRequest:got null mLocalFilePath");
                return null;
            }

            Bitmap bitmap = BitmapUtils.createVideoThumbnail(mLocalFilePath);
            if (bitmap == null || jc.isCancelled()) return null;
            Log.i(TAG,"LocalSecondImageRequest:bitmap.getWidth()="+bitmap.getWidth());
            Log.d(TAG,"LocalSecondImageRequest:bitmap.getHeight()="+bitmap.getHeight());
            DataManager manager = mApplication.getDataManager();
            LocalMediaItem item = (LocalMediaItem) manager.getMediaObject(mPath);
            int stereoType = item.stereoType;
            if (Video.Media.STEREO_TYPE_2D != stereoType) {
                Bitmap temp = StereoHelper.getStereoVideoImage(jc, bitmap, false,
                                                               stereoType);
                bitmap.recycle();
                bitmap = temp;
            }

            MediatekFeature.DataBundle dataBundle = 
                                     new MediatekFeature.DataBundle();
            dataBundle.secondFrame = bitmap;
            return dataBundle;
        }
    }



    @Override
    public int getSupportedOperations() {
        int operation = SUPPORT_DELETE | SUPPORT_SHARE | 
                        SUPPORT_PLAY | SUPPORT_INFO | SUPPORT_TRIM;
        if (isDrm()) {
            //add drm protection info
            operation |= SUPPORT_DRM_INFO;
            if (OmaDrmStore.RightsStatus.RIGHTS_VALID !=
                drmRights(OmaDrmStore.Action.TRANSFER)) {
                    //remove share operation if forbids.
                    operation &= ~SUPPORT_SHARE;
            }
        }
    	// thumbnail of stereo video can be displayed as a stereo photo
        if (mIsStereoDisplaySupported && Video.Media.STEREO_TYPE_2D != stereoType) {
                    operation |= SUPPORT_STEREO_DISPLAY;
        }
        return operation;
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
        mApplication.getContentResolver().delete(baseUri, "_id=?",
                new String[]{String.valueOf(id)});
        mApplication.getDataManager().broadcastLocalDeletion();
    }

    @Override
    public void rotate(int degrees) {
        // TODO
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public Uri getPlayUri() {
        return getContentUri();
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_VIDEO;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        int s = durationInSec;
        if (s > 0) {
            details.addDetail(MediaDetails.INDEX_DURATION, GalleryUtils.formatDuration(
                    mApplication.getAndroidContext(), durationInSec));
        }
        return details;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean hasDrmRights() {
         return OmaDrmStore.RightsStatus.RIGHTS_VALID == 
                DrmHelper.checkRightsStatus(mApplication.getAndroidContext(), 
                                            filePath, OmaDrmStore.Action.PLAY);
    }

    @Override
    public int drmRights(int action) {
         return DrmHelper.checkRightsStatus(mApplication.getAndroidContext(), 
                                            filePath, action);
    }

    @Override
    public boolean isTimeInterval() {
         return DrmHelper.isTimeIntervalMedia(mApplication.getAndroidContext(), 
                                            filePath, OmaDrmStore.Action.PLAY);
    }

    public int getSubType() {
        int subType = 0;
    	// VIDEO STEREO
        if (0 != stereoType) {
            if (mIsStereoDisplaySupported) {
                subType |= MediaObject.SUBTYPE_STEREO_VIDEO;
            }
        }

        //when show drm media whose type is not FL, show extra lock
        if (mIsDrmSupported && isDrm() &&
            OmaDrmStore.DrmMethod.METHOD_FL != drm_method) {
            if (!hasDrmRights()) {
                subType |= SUBTYPE_DRM_NO_RIGHT;
            } else {
                subType |= SUBTYPE_DRM_HAS_RIGHT;
            }
        }
        return subType;
    }

    /// M: Video thumbnail play @{    
    public interface OnDeleteListener {
        public void onDelete(LocalVideo video);
    }
    
    public static final int THUMBNAIL_STATE_STATIC = 0;
    public static final int THUMBNAIL_STATE_DYNAMIC_GENERATING = 1;
    public static final int THUMBNAIL_STATE_DYNAMIC_GENERATED_SUCCESS = 2;
    public static final int THUMBNAIL_STATE_DYNAMIC_GENERATED_FAIL = 3;
    // only used in GL thread (so may move it to Entry)
    public volatile int thumbNailState = THUMBNAIL_STATE_STATIC;
    // only used in GL thread (so may move it to Entry)
    public String dynamicThumbnailPath;
    /// @}
}
