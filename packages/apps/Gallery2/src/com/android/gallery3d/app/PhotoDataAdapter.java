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

package com.android.gallery3d.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.view.Display;
import android.os.SystemClock;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.BitmapPool;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.BitmapScreenNail;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TileImageViewAdapter;
import com.android.gallery3d.ui.TiledScreenNail;
import com.android.gallery3d.ui.TiledTexture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Log;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.LocalVideo;

// M: mtk import
import com.mediatek.gifdecoder.GifDecoder;
import android.os.SystemClock;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.content.Context;
import android.app.Activity;

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.gif.GifDecoderWrapper;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.ui.MtkBitmapScreenNail;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekMMProfile;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.gallery3d.util.MtkLog;

import com.android.gallery3d.data.ComboAlbum;
import com.android.gallery3d.data.FilterDeleteSet;

public class PhotoDataAdapter implements PhotoPage.Model {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/PhotoDataAdapter";

    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private static final int MSG_RUN_OBJECT = 3;
    private static final int MSG_UPDATE_IMAGE_REQUESTS = 4;

    private static final int MIN_LOAD_COUNT = 16;
    private static final int DATA_CACHE_SIZE = 256;
    private static final int SCREEN_NAIL_MAX = PhotoView.SCREEN_NAIL_MAX;
    private static final int IMAGE_CACHE_SIZE = 2 * SCREEN_NAIL_MAX + 1;

    private static final int BIT_SCREEN_NAIL = 1;
    private static final int BIT_FULL_IMAGE = 2;
    
    // M: added to support mav palyback
    private static final int BIT_MAV_PLAYBACK = 1 << 28;
    //added to support Stereo Display
    private static final int BIT_SECOND_SCREEN_NAIL = 1 << 29;
    //added to support DRM micro-thumb
    private static final int BIT_DRM_SCREEN_NAIL = 1 << 30;
    // M: added to support GIF animation
    private static final int BIT_GIF_ANIMATION = 1 << 31;

    private static final boolean mIsGifAnimationSupported = 
                               MediatekFeature.isGifAnimationSupported();
    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsStereoDisplaySupported = 
                        MediatekFeature.isStereoDisplaySupported();
    private static final boolean mIsMavSupported = 
                        MediatekFeature.isMAVSupported();

    private final AbstractGalleryActivity mActivity;
    private Path mConsumedItemPath;
    private boolean mTimeIntervalDRM;
    //because Gallery cached thumbnail as JPEG, and JPEG usually loses image
    //quality. For those image format whose does not has BitmapRegionDecoder
    //this will results in poor image quality, expecially for those man-made
    //image which is used to test image quality.
    //So we will decode from original image to improve image quality if there
    //is no regiondecoder for that image
    private final boolean mReDecodeToImproveImageQuality = true;


    // sImageFetchSeq is the fetching sequence for images.
    // We want to fetch the current screennail first (offset = 0), the next
    // screennail (offset = +1), then the previous screennail (offset = -1) etc.
    // After all the screennail are fetched, we fetch the full images (only some
    // of them because of we don't want to use too much memory).
    private static ImageFetch[] sImageFetchSeq;

    private static class ImageFetch {
        int indexOffset;
        int imageBit;
        public ImageFetch(int offset, int bit) {
            indexOffset = offset;
            imageBit = bit;
        }
    }

    static {
        int gifRequestCount = mIsGifAnimationSupported ? 1 : 0;
        int drmRequestCount = mIsDrmSupported ? 1 : 0;
        int stereoRequestCount = mIsStereoDisplaySupported ? 1 : 0;
        int mpoRequestCount = mIsMavSupported ? 1 : 0;
        
        int k = 0;
        sImageFetchSeq = new ImageFetch[1 + //drmRequestCount +
                                (IMAGE_CACHE_SIZE - 1) * (2)// + 2 * drmRequestCount) 
                                        + 3 + gifRequestCount - 2 /*remove 2 full image requests to improve pan performance*/
                                        + stereoRequestCount
                                        + mpoRequestCount];
        sImageFetchSeq[k++] = new ImageFetch(0, BIT_SCREEN_NAIL);

        //add to retrieve the second frame of stereo photo
        if (mIsStereoDisplaySupported) {
            sImageFetchSeq[k++] = new ImageFetch(0, BIT_SECOND_SCREEN_NAIL);
        }

        for (int i = 1; i < IMAGE_CACHE_SIZE; ++i) {
            sImageFetchSeq[k++] = new ImageFetch(i, BIT_SCREEN_NAIL);
            sImageFetchSeq[k++] = new ImageFetch(-i, BIT_SCREEN_NAIL);
        }

        sImageFetchSeq[k++] = new ImageFetch(0, BIT_FULL_IMAGE);
//        sImageFetchSeq[k++] = new ImageFetch(1, BIT_FULL_IMAGE);
//        sImageFetchSeq[k++] = new ImageFetch(-1, BIT_FULL_IMAGE);

        if (mIsGifAnimationSupported) {
            sImageFetchSeq[k++] = new ImageFetch(0, BIT_GIF_ANIMATION);
        }
        
        /// M: added for supporting mav palyback integrated in PhotoPage
        if (mIsMavSupported) {
            sImageFetchSeq[k++] = new ImageFetch(0, BIT_MAV_PLAYBACK);
        }

    }

    private final TileImageViewAdapter mTileProvider = new TileImageViewAdapter();

    // PhotoDataAdapter caches MediaItems (data) and ImageEntries (image).
    //
    // The MediaItems are stored in the mData array, which has DATA_CACHE_SIZE
    // entries. The valid index range are [mContentStart, mContentEnd). We keep
    // mContentEnd - mContentStart <= DATA_CACHE_SIZE, so we can use
    // (i % DATA_CACHE_SIZE) as index to the array.
    //
    // The valid MediaItem window size (mContentEnd - mContentStart) may be
    // smaller than DATA_CACHE_SIZE because we only update the window and reload
    // the MediaItems when there are significant changes to the window position
    // (>= MIN_LOAD_COUNT).
    private final MediaItem mData[] = new MediaItem[DATA_CACHE_SIZE];
    private int mContentStart = 0;
    private int mContentEnd = 0;

    // The ImageCache is a Path-to-ImageEntry map. It only holds the
    // ImageEntries in the range of [mActiveStart, mActiveEnd).  We also keep
    // mActiveEnd - mActiveStart <= IMAGE_CACHE_SIZE.  Besides, the
    // [mActiveStart, mActiveEnd) range must be contained within
    // the [mContentStart, mContentEnd) range.
    private HashMap<Path, ImageEntry> mImageCache =
            new HashMap<Path, ImageEntry>();
    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    // mCurrentIndex is the "center" image the user is viewing. The change of
    // mCurrentIndex triggers the data loading and image loading.
    private int mCurrentIndex;

    // mChanges keeps the version number (of MediaItem) about the images. If any
    // of the version number changes, we notify the view. This is used after a
    // database reload or mCurrentIndex changes.
    private final long mChanges[] = new long[IMAGE_CACHE_SIZE];
    // mPaths keeps the corresponding Path (of MediaItem) for the images. This
    // is used to determine the item movement.
    private final Path mPaths[] = new Path[IMAGE_CACHE_SIZE];

    private final Handler mMainHandler;
    private final ThreadPool mThreadPool;

    private final PhotoView mPhotoView;
    private final MediaSet mSource;
    private ReloadTask mReloadTask;

    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;
    private int mSize = 0;
    private Path mItemPath;
    private int mCameraIndex;
    private boolean mIsPanorama;
    private boolean mIsStaticCamera;
    private boolean mIsActive;
    private boolean mNeedFullImage;
    private int mFocusHintDirection = FOCUS_HINT_NEXT;
    private Path mFocusHintPath = null;

    public interface DataListener extends LoadingListener {
        public void onPhotoChanged(int index, Path item);
    }

    private DataListener mDataListener;

    private final SourceListener mSourceListener = new SourceListener();
    private final TiledTexture.Uploader mUploader;
    
    public interface MavListener {
        public void setSeekBar(int max, int progress);
        public void setStatus(boolean isEnable);
        public void setProgress(int progress);
    }
    private MavListener mMavListener;
    private boolean mIsMavLoadingFinished;

    // M: PhotoDataAdapter will start Reload task twice after photopage resume, first load data and 
    // calculate contentStart and contentEnd, second submit job decode images and get screennail
    // because Camera screennail has ready before reload task second run,
    // in order to improve camera launch performance, we need first submit job and get CameraScreennail
    private boolean reloadCameraItem = false;

    // The path of the current viewing item will be stored in mItemPath.
    // If mItemPath is not null, mCurrentIndex is only a hint for where we
    // can find the item. If mItemPath is null, then we use the mCurrentIndex to
    // find the image being viewed. cameraIndex is the index of the camera
    // preview. If cameraIndex < 0, there is no camera preview.
    public PhotoDataAdapter(AbstractGalleryActivity activity, PhotoView view,
            MediaSet mediaSet, Path itemPath, int indexHint, int cameraIndex,
            boolean isPanorama, boolean isStaticCamera) {
        mSource = Utils.checkNotNull(mediaSet);
        mPhotoView = Utils.checkNotNull(view);
        mItemPath = Utils.checkNotNull(itemPath);
        mCurrentIndex = indexHint;
        mCameraIndex = cameraIndex;
        mIsPanorama = isPanorama;
        mIsStaticCamera = isStaticCamera;
        mThreadPool = activity.getThreadPool();
        mNeedFullImage = true;

        // M: camera launch performance improve 
        reloadCameraItem = (mCameraIndex == 0);
        MtkLog.i(TAG, "PhotoDataAdapter reloadCameraItem " + reloadCameraItem);
        //hold activity istance for DRM feature
        mActivity = activity;

        Arrays.fill(mChanges, MediaObject.INVALID_DATA_VERSION);

        mUploader = new TiledTexture.Uploader(activity.getGLRoot());

        mMainHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @SuppressWarnings("unchecked")
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_RUN_OBJECT:
                        ((Runnable) message.obj).run();
                        return;
                    case MSG_LOAD_START: {
                        if (mDataListener != null) {
                            mDataListener.onLoadingStarted();
                        }
                        return;
                    }
                    case MSG_LOAD_FINISH: {
                        if (mDataListener != null) {
                            mDataListener.onLoadingFinished(false);
                        }
                        return;
                    }
                    case MSG_UPDATE_IMAGE_REQUESTS: {
                        updateImageRequests();
                        return;
                    }
                    default: throw new AssertionError();
                }
            }
        };

        updateSlidingWindow();
    }

    private MediaItem getItemInternal(int index) {
        if (index < 0 || index >= mSize) return null;
        if (index >= mContentStart && index < mContentEnd) {
            return mData[index % DATA_CACHE_SIZE];
        }
        return null;
    }

    private long getVersion(int index) {
        MediaItem item = getItemInternal(index);
        if (item == null) return MediaObject.INVALID_DATA_VERSION;
        return item.getDataVersion();
    }

    private Path getPath(int index) {
        MediaItem item = getItemInternal(index);
        if (item == null) return null;
        return item.getPath();
    }

    private void fireDataChange() {
        MediatekMMProfile.startProfilePhotoPageFireDataChange();
        // First check if data actually changed.
        boolean changed = false;
        for (int i = -SCREEN_NAIL_MAX; i <= SCREEN_NAIL_MAX; ++i) {
            long newVersion = getVersion(mCurrentIndex + i);
            if (mChanges[i + SCREEN_NAIL_MAX] != newVersion) {
                mChanges[i + SCREEN_NAIL_MAX] = newVersion;
                changed = true;
            }
        }

        if (!changed) {
            MediatekMMProfile.stopProfilePhotoPageFireDataChange();
            return;
        }

        // Now calculate the fromIndex array. fromIndex represents the item
        // movement. It records the index where the picture come from. The
        // special value Integer.MAX_VALUE means it's a new picture.
        final int N = IMAGE_CACHE_SIZE;
        int fromIndex[] = new int[N];

        // Remember the old path array.
        Path oldPaths[] = new Path[N];
        System.arraycopy(mPaths, 0, oldPaths, 0, N);

        // Update the mPaths array.
        for (int i = 0; i < N; ++i) {
            mPaths[i] = getPath(mCurrentIndex + i - SCREEN_NAIL_MAX);
        }

        // Calculate the fromIndex array.
        for (int i = 0; i < N; i++) {
            Path p = mPaths[i];
            if (p == null) {
                fromIndex[i] = Integer.MAX_VALUE;
                continue;
            }

            // Try to find the same path in the old array
            int j;
            for (j = 0; j < N; j++) {
                if (oldPaths[j] == p) {
                    break;
                }
            }
            fromIndex[i] = (j < N) ? j - SCREEN_NAIL_MAX : Integer.MAX_VALUE;
        }

        mPhotoView.notifyDataChange(fromIndex, -mCurrentIndex,
                mSize - 1 - mCurrentIndex);
        
        // M: if we're at camera preview now, make sure lights are out
        if (isCamera(0)) {
            Log.e(TAG, "fireDataChange: enterCameraPreview");
            mPhotoView.enterCameraPreview();
        }
        MediatekMMProfile.stopProfilePhotoPageFireDataChange();
    }

    public void setDataListener(DataListener listener) {
        mDataListener = listener;
    }

    private void updateScreenNail(Path path, Future<ScreenNail> future) {
        MediatekMMProfile.startProfilePhotoPageDecodeScreenNailListener();
        ImageEntry entry = mImageCache.get(path);
        ScreenNail screenNail = future.get();

        if (entry == null || entry.screenNailTask != future) {
            if (screenNail != null) screenNail.recycle();
            return;
        }

        entry.screenNailTask = null;
        
        // M: whether load has failed
        // should be decided by the screennail in decode result
        boolean loadFailed = (screenNail == null);

        // Combine the ScreenNails if we already have a BitmapScreenNail
        // M: change to BitmapTexture from TiledTexture
        if (entry.screenNail instanceof BitmapScreenNail) {
            BitmapScreenNail original = (BitmapScreenNail) entry.screenNail;
            screenNail = original.combine(screenNail);
        }

        if (screenNail == null) {
            entry.failToLoad = loadFailed;
        } else {
            entry.failToLoad = loadFailed;
            entry.screenNail = screenNail;
        }

        for (int i = -SCREEN_NAIL_MAX; i <= SCREEN_NAIL_MAX; ++i) {
            if (path == getPath(mCurrentIndex + i)) {
                if (i == 0) updateTileProvider(entry);
                mPhotoView.notifyImageChange(i);
                break;
            }
        }
        updateImageRequests();
        // M: change to BitmapTexture from TiledTexture
        // updateScreenNailUploadQueue();
        MediatekMMProfile.stopProfilePhotoPageDecodeScreenNailListener();
    }

    private void updateFullImage(Path path, Future<BitmapRegionDecoder> future) {
        ImageEntry entry = mImageCache.get(path);
        if (entry == null || entry.fullImageTask != future) {
            BitmapRegionDecoder fullImage = future.get();
            if (fullImage != null) fullImage.recycle();
            return;
        }

        if(future != null) {
            BitmapRegionDecoder fullImage = future.get();
            MediaItem item = (MediaItem)mActivity.getDataManager().getMediaObject(path);
            String mimeType = item.getMimeType();
            if(fullImage != null
                    && entry.failToLoad
                    && MediatekFeature.isOutOfLimitation(mimeType, fullImage.getWidth(), fullImage.getHeight())) {
                MtkLog.d(TAG, String.format("out of limitation: %s [mime type: %s, width: %d, height: %d]", 
                        path, mimeType, fullImage.getWidth(), fullImage.getHeight()));
                fullImage.recycle();
                return;
            }
        }

        entry.fullImageTask = null;
        entry.fullImage = future.get();
        if (entry.fullImage != null) {
            if (path == getPath(mCurrentIndex)) {
                updateTileProvider(entry);
                mPhotoView.notifyImageChange(0);
            }
        }
        updateImageRequests();
    }

    @Override
    public void resume() {
        mIsActive = true;
        TiledTexture.prepareResources();

        mSource.addContentListener(mSourceListener);
        updateImageCache();
        updateImageRequests();

        mReloadTask = new ReloadTask();
        ///M: set thread name
        mReloadTask.setName("reloadtask-PDA");
        mReloadTask.start();

        fireDataChange();
    }

    @Override
    public void pause() {
        mIsActive = false;

        mReloadTask.terminate();
        mReloadTask = null;

        mSource.removeContentListener(mSourceListener);

        //added for drm consume behavior
        if (mIsDrmSupported) {
            saveDrmConsumeStatus();
        }

        for (ImageEntry entry : mImageCache.values()) {
            if (entry.fullImageTask != null) entry.fullImageTask.cancel();
            if (entry.screenNailTask != null) entry.screenNailTask.cancel();
            if (entry.screenNail != null) entry.screenNail.recycle();
            
            // M: added to cancel Gif decoder task
            if (MediatekFeature.isGifSupported() &&
                entry.gifDecoderTask != null) {
                entry.gifDecoderTask.cancel();
            }

            //added to cancel second screen nail for stereo diaplay
            if (mIsStereoDisplaySupported &&
                entry.secondScreenNailTask != null) {
                entry.secondScreenNailTask.cancel();
                if (null != entry.firstScreenNail) {
                    entry.firstScreenNail.recycle();
                }
                if (null != entry.firstScreenNail) {
                    entry.secondScreenNail.recycle();
                }
            }
            
            /// M: added to release mav resource
            MediaItem currentItem = getMediaItem(0);
            if (currentItem != null && mIsMavSupported
                    && currentItem.getSubType() == MediaObject.SUBTYPE_MPO_MAV) {
                // cancel mpo decoder task of decoder has not finished
                if(entry.mpoDecoderTask != null) {
                    MtkLog.d("TAG", "cancel decoder task when pause");
                    entry.mpoDecoderTask.cancel();
                    entry.mpoDecoderTask = null;
                }
                // notify mav render thread if current item is mav file
                synchronized (mRenderLock) {
                    mRenderRequested = false;
                    mRenderLock.notifyAll();
                }
            }
        }
        mImageCache.clear();
        mTileProvider.clear();

        mUploader.clear();
        TiledTexture.freeResources();
    }

    private MediaItem getItem(int index) {
        if (index < 0 || index >= mSize || !mIsActive) return null;
        Utils.assertTrue(index >= mActiveStart && index < mActiveEnd);

        if (index >= mContentStart && index < mContentEnd) {
            return mData[index % DATA_CACHE_SIZE];
        }
        return null;
    }

    private void updateCurrentIndex(int index) {
        Log.d(TAG, "updateCurrentIndex: " + mCurrentIndex + " => " + index);
        if (mCurrentIndex == index) return;
        /// M: notify mav thread
        synchronized (mRenderLock) {
            mRenderRequested = false;
            mRenderLock.notifyAll();
        }

        int prevIndex = mCurrentIndex;
        mCurrentIndex = index;
        updateSlidingWindow();

        MediaItem item = mData[index % DATA_CACHE_SIZE];
        mItemPath = item == null ? null : item.getPath();

        if (mIsStereoDisplaySupported) {
            mPhotoView.setStereoMode(false);
            //mPhotoView.notifyImageChange(0);
        }

        updateImageCache();

        //update drm cache
        updateDrmScreenNail();

        updateImageRequests();
        updateTileProvider();

        if (mDataListener != null) {
            mDataListener.onPhotoChanged(index, mItemPath);
        }

        fireDataChange();
        
        // M: if we're entering camera preview, make sure "the lights are out";
        // and if previous item is camera preview,
        // make sure the orientation is unlocked.
        if (isCamera(0)) {
            mPhotoView.enterCameraPreview();
            mPhotoView.setWantPictureCenterCallbacks(true);
        } else if (isCamera(prevIndex - mCurrentIndex)) {
            mPhotoView.leaveCameraPreview();
        }
        
    }

    private void uploadScreenNail(int offset) {
        int index = mCurrentIndex + offset;
        if (index < mActiveStart || index >= mActiveEnd) return;

        MediaItem item = getItem(index);
        if (item == null) return;

        ImageEntry e = mImageCache.get(item.getPath());
        if (e == null) return;

        ScreenNail s = e.screenNail;
        if (s instanceof TiledScreenNail) {
            TiledTexture t = ((TiledScreenNail) s).getTexture();
            if (t != null && !t.isReady()) mUploader.addTexture(t);
        }
    }

    private void updateScreenNailUploadQueue() {
        mUploader.clear();
        uploadScreenNail(0);
        for (int i = 1; i < IMAGE_CACHE_SIZE; ++i) {
            uploadScreenNail(i);
            uploadScreenNail(-i);
        }
    }

    @Override
    public void moveTo(int index) {
        updateCurrentIndex(index);
    }

    @Override
    public ScreenNail getScreenNail(int offset) {
        int index = mCurrentIndex + offset;
        if (index < 0 || index >= mSize || !mIsActive) return null;
        Utils.assertTrue(index >= mActiveStart && index < mActiveEnd);

        MediaItem item = getItem(index);
        if (item == null) return null;

        ImageEntry entry = mImageCache.get(item.getPath());
        if (entry == null) return null;

        // M: as there are gif frame for gif animation or original screennail
        // for improving image quality of bmp format, we have to decide
        // which screen nail is to be used.
        ScreenNail targetScreenNail = getTargetScreenNail(entry);
        if (targetScreenNail != null) return targetScreenNail;

        // Create a default ScreenNail if the real one is not available yet,
        // except for camera that a black screen is better than a gray tile.
        if (entry.screenNail == null && !isCamera(offset)) {
            entry.screenNail = newPlaceholderScreenNail(item);
            if (offset == 0) updateTileProvider(entry);
        }

        return entry.screenNail;
    }

    @Override
    public void getImageSize(int offset, PhotoView.Size size) {
        MediaItem item = getItem(mCurrentIndex + offset);
        if (item == null) {
            size.width = 0;
            size.height = 0;
        } else {
            size.width = item.getWidth();
            size.height = item.getHeight();
        }
    }

    @Override
    public int getImageRotation(int offset) {
        MediaItem item = getItem(mCurrentIndex + offset);
        return (item == null) ? 0 : item.getFullImageRotation();
    }

    @Override
    public void setNeedFullImage(boolean enabled) {
        mNeedFullImage = enabled;
        mMainHandler.sendEmptyMessage(MSG_UPDATE_IMAGE_REQUESTS);
    }

    @Override
    public boolean isCamera(int offset) {
        return mCurrentIndex + offset == mCameraIndex;
    }

    @Override
    public boolean isPanorama(int offset) {
        return isCamera(offset) && mIsPanorama;
    }

    @Override
    public boolean isStaticCamera(int offset) {
        return isCamera(offset) && mIsStaticCamera;
    }

    @Override
    public boolean isVideo(int offset) {
        MediaItem item = getItem(mCurrentIndex + offset);
        return (item == null)
                ? false
                : item.getMediaType() == MediaItem.MEDIA_TYPE_VIDEO;
    }

    @Override
    public boolean isDeletable(int offset) {
        MediaItem item = getItem(mCurrentIndex + offset);
        return (item == null)
                ? false
                : (item.getSupportedOperations() & MediaItem.SUPPORT_DELETE) != 0;
    }

    @Override
    public int getLoadingState(int offset) {
        ImageEntry entry = mImageCache.get(getPath(mCurrentIndex + offset));
        if (entry == null) return LOADING_INIT;
        if (entry.failToLoad) return LOADING_FAIL;
        if (entry.screenNail != null) return LOADING_COMPLETE;
        return LOADING_INIT;
    }

    @Override
    public ScreenNail getScreenNail() {
        return getScreenNail(0);
    }

    @Override
    public int getImageHeight() {
        return mTileProvider.getImageHeight();
    }

    @Override
    public int getImageWidth() {
        return mTileProvider.getImageWidth();
    }

    @Override
    public int getLevelCount() {
        return mTileProvider.getLevelCount();
    }

    @Override
    public Bitmap getTile(int level, int x, int y, int tileSize,
            int borderSize, BitmapPool pool) {
        return mTileProvider.getTile(level, x, y, tileSize, borderSize, pool);
    }

    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    @Override
    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    @Override
    public MediaItem getMediaItem(int offset) {
        int index = mCurrentIndex + offset;
        if (index >= mContentStart && index < mContentEnd) {
            return mData[index % DATA_CACHE_SIZE];
        }
        return null;
    }

    @Override
    public void setCurrentPhoto(Path path, int indexHint) {
        if (mItemPath == path) return;
        mItemPath = path;
        mCurrentIndex = indexHint;
        updateSlidingWindow();
        updateImageCache();

        //update drm cache
        updateDrmScreenNail();

        fireDataChange();

        // We need to reload content if the path doesn't match.
        MediaItem item = getMediaItem(0);
        if (item != null && item.getPath() != path) {
            if (mReloadTask != null) mReloadTask.notifyDirty();
        }
    }

    @Override
    public void setFocusHintDirection(int direction) {
        mFocusHintDirection = direction;
    }

    @Override
    public void setFocusHintPath(Path path) {
        mFocusHintPath = path;
    }

    private void updateTileProvider() {
        ImageEntry entry = mImageCache.get(getPath(mCurrentIndex));
        if (entry == null) { // in loading
            mTileProvider.clear();
        } else {
            updateTileProvider(entry);
        }
    }

    private void updateTileProvider(ImageEntry entry) {
        MediatekMMProfile.startProfilePhotoPageUpdateTileProvider();
        ScreenNail screenNail = entry.screenNail;
        BitmapRegionDecoder fullImage = entry.fullImage;
        if (screenNail != null) {
            //added for mediatek features
            //let image data of mediate feature to take action.
            if (updateTileProviderEx(entry)) {
                Log.d(TAG,"updateTileProvider:we return!");
                MediatekMMProfile.stopProfilePhotoPageUpdateTileProvider();
                return;
            }            

            if (fullImage != null) {
                mTileProvider.setScreenNail(screenNail,
                        fullImage.getWidth(), fullImage.getHeight());
                mTileProvider.setRegionDecoder(fullImage);
            } else {
                int width = screenNail.getWidth();
                int height = screenNail.getHeight();
                mTileProvider.setScreenNail(screenNail, width, height);
            }
        } else {
            mTileProvider.clear();
        }
        MediatekMMProfile.stopProfilePhotoPageUpdateTileProvider();
    }

    private void updateSlidingWindow() {
        MediatekMMProfile.startProfilePhotoPageUpdateSlidingWindow();
        // 1. Update the image window
        int start = Utils.clamp(mCurrentIndex - IMAGE_CACHE_SIZE / 2,
                0, Math.max(0, mSize - IMAGE_CACHE_SIZE));
        int end = Math.min(mSize, start + IMAGE_CACHE_SIZE);

        if (mActiveStart == start && mActiveEnd == end) {
            MediatekMMProfile.stopProfilePhotoPageUpdateSlidingWindow();
            return;
        }

        mActiveStart = start;
        mActiveEnd = end;

        // 2. Update the data window
        start = Utils.clamp(mCurrentIndex - DATA_CACHE_SIZE / 2,
                0, Math.max(0, mSize - DATA_CACHE_SIZE));
        end = Math.min(mSize, start + DATA_CACHE_SIZE);
        if (mContentStart > mActiveStart || mContentEnd < mActiveEnd
                || Math.abs(start - mContentStart) > MIN_LOAD_COUNT) {
            for (int i = mContentStart; i < mContentEnd; ++i) {
                if (i < start || i >= end) {
                    mData[i % DATA_CACHE_SIZE] = null;
                }
            }
            mContentStart = start;
            mContentEnd = end;
            if (mReloadTask != null) mReloadTask.notifyDirty();
        }
        MediatekMMProfile.stopProfilePhotoPageUpdateSlidingWindow();
    }

    private void updateImageRequests() {
        if (!mIsActive) return;

        int currentIndex = mCurrentIndex;
        MediaItem item = mData[currentIndex % DATA_CACHE_SIZE];
        if (item == null || item.getPath() != mItemPath) {
            // current item mismatch - don't request image
            return;
        }

        MediatekMMProfile.startProfilePhotoPageUpdateImageRequest();
        // 1. Find the most wanted request and start it (if not already started).
        Future<?> task = null;
        for (int i = 0; i < sImageFetchSeq.length; i++) {
            int offset = sImageFetchSeq[i].indexOffset;
            int bit = sImageFetchSeq[i].imageBit;
            if (bit == BIT_FULL_IMAGE && !mNeedFullImage) continue;
            task = startTaskIfNeeded(currentIndex + offset, bit);
            if (task != null) break;
        }

        // 2. Cancel everything else.
        for (ImageEntry entry : mImageCache.values()) {
            if (entry.screenNailTask != null && entry.screenNailTask != task) {
                entry.screenNailTask.cancel();
                entry.screenNailTask = null;
                entry.requestedScreenNail = MediaObject.INVALID_DATA_VERSION;
            }
            if (entry.fullImageTask != null && entry.fullImageTask != task) {
                entry.fullImageTask.cancel();
                entry.fullImageTask = null;
                entry.requestedFullImage = MediaObject.INVALID_DATA_VERSION;
            }

            //cancel gif animation task if needed
            if (mIsGifAnimationSupported &&
                entry.gifDecoderTask != null && entry.gifDecoderTask != task) {
                entry.gifDecoderTask.cancel();
                entry.gifDecoderTask = null;
                entry.requestedGif = MediaObject.INVALID_DATA_VERSION;
            }

            //cancel stereo task if needed
            if (mIsStereoDisplaySupported &&
                entry.secondScreenNailTask != null && 
                entry.secondScreenNailTask != task) {
                entry.secondScreenNailTask.cancel();
                entry.secondScreenNailTask = null;
                entry.requestedSecondScreenNail = MediaObject.INVALID_DATA_VERSION;
            }
            //cancel decode original bitmap task
            if (mReDecodeToImproveImageQuality &&
                entry.originScreenNailTask != null &&
                entry.originScreenNailTask != task) {
                entry.originScreenNailTask.cancel();
                entry.originScreenNailTask = null;
                entry.requestedOriginScreenNail = MediaObject.INVALID_DATA_VERSION;
            }
            
            //cancel mav playback task if needed
            if (mIsMavSupported &&
                entry.mpoDecoderTask != null && entry.mpoDecoderTask != task) {
                entry.mpoDecoderTask.cancel();
                entry.mpoDecoderTask = null;
                entry.requestedMav = MediaObject.INVALID_DATA_VERSION;
            }
        }
        MediatekMMProfile.stopProfilePhotoPageUpdateImageRequest();
    }

    private class ScreenNailJob implements Job<ScreenNail> {
        private MediaItem mItem;

        public ScreenNailJob(MediaItem item) {
            mItem = item;
        }

        @Override
        public ScreenNail run(JobContext jc) {
            // We try to get a ScreenNail first, if it fails, we fallback to get
            // a Bitmap and then wrap it in a BitmapScreenNail instead.
            ScreenNail s = mItem.getScreenNail();
            if (s != null) return s;

            // If this is a temporary item, don't try to get its bitmap because
            // it won't be available. We will get its bitmap after a data reload.
            if (isTemporaryItem(mItem)) {
                return newPlaceholderScreenNail(mItem);
            }
            MediatekMMProfile.startProfilePhotoPageDecodeScreenNailJob();
            Bitmap bitmap = mItem.requestImage(MediaItem.TYPE_THUMBNAIL).run(jc);
            if (jc.isCancelled()) return null;
            if (bitmap != null) {
                bitmap = BitmapUtils.rotateBitmap(bitmap,
                    mItem.getRotation() - mItem.getFullImageRotation(), true);
            }

            ScreenNail screenNail = MediatekFeature.getMtkScreenNail(mItem, bitmap);
            MediatekMMProfile.stopProfilePhotoPageDecodeScreenNailJob();
            if (null != screenNail) return screenNail;

            // M: change to BitmapTexture from TiledTexture
            return bitmap == null ? null : new BitmapScreenNail(bitmap);
        }
    }

    private class FullImageJob implements Job<BitmapRegionDecoder> {
        private MediaItem mItem;

        public FullImageJob(MediaItem item) {
            mItem = item;
        }

        @Override
        public BitmapRegionDecoder run(JobContext jc) {
            if (isTemporaryItem(mItem)) {
                return null;
            }
            // M: if decode thumbnail fail, there is no need to decode full image
            ImageEntry entry = mImageCache.get(mItem.getPath());
            if (entry != null && entry.failToLoad == true) {
                Log.i(TAG, "<FullImageJob.run> decode thumbnail fail,"
                        + "no need to decode full image, return null");
                return null;
            }
            return mItem.requestLargeImage().run(jc);
        }
    }

    // Returns true if we think this is a temporary item created by Camera. A
    // temporary item is an image or a video whose data is still being
    // processed, but an incomplete entry is created first in MediaProvider, so
    // we can display them (in grey tile) even if they are not saved to disk
    // yet. When the image or video data is actually saved, we will get
    // notification from MediaProvider, reload data, and show the actual image
    // or video data.
    private boolean isTemporaryItem(MediaItem mediaItem) {
        // Must have camera to create a temporary item.
        if (mCameraIndex < 0) return false;
        // Must be an item in camera roll.
        if (!(mediaItem instanceof LocalMediaItem)) return false;
        LocalMediaItem item = (LocalMediaItem) mediaItem;
        if (item.getBucketId() != MediaSetUtils.CAMERA_BUCKET_ID) return false;
        // Must have no size, but must have width and height information
        if (item.getSize() != 0) return false;
        if (item.getWidth() == 0) return false;
        if (item.getHeight() == 0) return false;
        // Must be created in the last 10 seconds.
        if (item.getDateInMs() - System.currentTimeMillis() > 10000) return false;
        return true;
    }

    // Create a default ScreenNail when a ScreenNail is needed, but we don't yet
    // have one available (because the image data is still being saved, or the
    // Bitmap is still being loaded.
    private ScreenNail newPlaceholderScreenNail(MediaItem item) {
        //added for drm feature
        ScreenNail screenNail = MediatekFeature.getMtkScreenNail(item);
        if (null != screenNail) return screenNail;

        int width = item.getWidth();
        int height = item.getHeight();
        // M: change to BitmapTexture from TiledTexture
        return new BitmapScreenNail(width, height);
    }

    // Returns the task if we started the task or the task is already started.
    private Future<?> startTaskIfNeeded(int index, int which) {
        if (index < mActiveStart || index >= mActiveEnd) return null;

        ImageEntry entry = mImageCache.get(getPath(index));
        if (entry == null) return null;
        MediaItem item = mData[index % DATA_CACHE_SIZE];
        Utils.assertTrue(item != null);
        long version = item.getDataVersion();

        if (which == BIT_SCREEN_NAIL && entry.screenNailTask != null
                && entry.requestedScreenNail == version) {
            return entry.screenNailTask;
        } else if (which == BIT_FULL_IMAGE && entry.fullImageTask != null
                && entry.requestedFullImage == version) {
            return entry.fullImageTask;
        } else if (which == BIT_GIF_ANIMATION && entry.gifDecoderTask != null
                && MediatekFeature.isGifSupported() && entry.requestedGif == version) {
            return entry.gifDecoderTask;
        } else if (which == BIT_SECOND_SCREEN_NAIL && 
                   entry.secondScreenNailTask != null &&
                   entry.requestedSecondScreenNail == version &&
                   mIsStereoDisplaySupported) {
            return entry.secondScreenNailTask;
        } else if (which == BIT_FULL_IMAGE &&
                   entry.originScreenNailTask != null &&
                   entry.requestedOriginScreenNail == version &&
                   mReDecodeToImproveImageQuality) {
            return entry.originScreenNailTask;
        } 
        // TODO maybe need to remove it, because we should reload mpo frames  when resumed
        else if (which == BIT_MAV_PLAYBACK && entry.mpoDecoderTask != null
                   && MediatekFeature.isMAVSupported() && entry.requestedMav == version) {
                   MtkLog.d(TAG, "startTaskIfNeed: return existed mpoDecoderTask");
            return entry.mpoDecoderTask;
        }

        if (which == BIT_SCREEN_NAIL && entry.requestedScreenNail != version) {
            /// M: @{ a shot cut for loading camera screen nail. As CameraScreennail is
            // already there, there is no need to start a job in thread
            if (isCamera(0) && index == mCameraIndex) {
                ScreenNail s = item.getScreenNail();
                if (s != null) {
                    MediatekMMProfile.triggerPhotoPageDecodeScreenNail();
                    entry.requestedScreenNail = version;
                    entry.failToLoad = false;
                    entry.screenNail = s;
                    updateTileProvider(entry);
                    mPhotoView.notifyImageChange(0);
                    return null;
                }
            }
            // @}

            entry.requestedScreenNail = version;
            MediatekMMProfile.triggerPhotoPageDecodeScreenNail();
            entry.screenNailTask = mThreadPool.submit(
                    new ScreenNailJob(item),
                    new ScreenNailListener(item));
            // request screen nail
            return entry.screenNailTask;
        }
        if (which == BIT_FULL_IMAGE && entry.requestedFullImage != version
                && (item.getSupportedOperations()
                & MediaItem.SUPPORT_FULL_IMAGE) != 0) {
            entry.requestedFullImage = version;
            entry.fullImageTask = mThreadPool.submit(
                    new FullImageJob(item),
                    new FullImageListener(item));
            // request full image
            return entry.fullImageTask;
        }

        if (mReDecodeToImproveImageQuality && which == BIT_FULL_IMAGE
                && entry.requestedOriginScreenNail != version
                && (item.getSupportedOperations()
                & MediaItem.SUPPORT_FULL_IMAGE) == 0) {
            entry.requestedOriginScreenNail = version;

            Params params = new Params();
            params.inOriginalFrame = true;
            params.inOriginalTargetSize = Params.THUMBNAIL_TARGET_SIZE_LARGER;
            MediatekFeature.enablePictureQualityEnhance(params, true);

            entry.originScreenNailTask = mThreadPool.submit(
                    new OriginScreenNailJob(item, params),
                    new OriginScreenNailListener(item));
            // request original screen nail
            return entry.originScreenNailTask;
        }

        if (mIsGifAnimationSupported) {
            if (which == BIT_GIF_ANIMATION
                    && (entry.requestedGif != version)
                    && (item.getSupportedOperations() & 
                        MediaItem.SUPPORT_GIF_ANIMATION) != 0
                    ) {
                entry.requestedGif = version;

                //create mediatek parameters
                Params params = new Params();
                params.inGifDecoder = true;//we want gif decoder

                Log.i(TAG,"startTaskIfNeeded:start GifDecoder task");
                entry.gifDecoderTask = mThreadPool.submit(
                        item.requestImage(MediaItem.TYPE_THUMBNAIL, params),
                        new GifDecoderListener(item.getPath()));
                // request gif decoder
                return entry.gifDecoderTask;
            }
        }

        if (mIsStereoDisplaySupported) {
            if (which == BIT_SECOND_SCREEN_NAIL
                    && entry.requestedSecondScreenNail != version
                    && (item.getSupportedOperations() & 
                        MediaItem.SUPPORT_STEREO_DISPLAY) != 0
                    ) {
                //we should get the layout of stereo image first
                entry.stereoLayout = item.getStereoLayout();
                //we create a decode job
                entry.requestedSecondScreenNail = version;

                //create mediatek parameters
                Params params = new Params();
                params.inFirstFrame = true;//we decode the first frame if possible
                params.inSecondFrame = true;
                MediatekFeature.enablePictureQualityEnhance(params, true);

                entry.secondScreenNailTask = mThreadPool.submit(
                    new SecondScreenNailJob(item, params),
                    new SecondScreenNailListener(item));
                // request second screen nail for stereo display
                return entry.secondScreenNailTask;
            }
        }
        
        // only to exeute the task at non-film mode
        if (mIsMavSupported
                && which == BIT_MAV_PLAYBACK
                && !mPhotoView.getFilmMode()
                && (entry.requestedMav != version)
                && (item.getSupportedOperations() & MediaItem.SUPPORT_MAV_PLAYBACK) != 0) {
            MtkLog.d(TAG, "create mav decoder task");
            entry.requestedMav = version;

            // create mediatek parameters
            // only set inMpoTotalCount as true
            // first get mpo's total count
            // because we need set it to MavSeekBar for update progress 
            Params params = new Params();
            params.inMpoTotalCount = true;
            
            MtkLog.d(TAG, "get mav total count");
            mIsMavLoadingFinished = false;
            mPhotoView.setMavLoadingFinished(mIsMavLoadingFinished);
            
            entry.mpoDecoderTask = mThreadPool.submit(item.requestImage(
                    MediaItem.TYPE_THUMBNAIL, params), new MavDecoderListener(
                    item.getPath(), item, TYPE_LOAD_TOTAL_COUNT));
            return entry.mpoDecoderTask;
        }
        return null;
    }

    private void updateImageCache() {
        MediatekMMProfile.startProfilePhotoPageUpdateImageCache();
        HashSet<Path> toBeRemoved = new HashSet<Path>(mImageCache.keySet());
        for (int i = mActiveStart; i < mActiveEnd; ++i) {
            MediaItem item = mData[i % DATA_CACHE_SIZE];
            if (item == null) continue;
            Path path = item.getPath();
            ImageEntry entry = mImageCache.get(path);
            toBeRemoved.remove(path);
            if (entry != null) {
                if (Math.abs(i - mCurrentIndex) > 1) {
                    if (entry.fullImageTask != null) {
                        entry.fullImageTask.cancel();
                        entry.fullImageTask = null;
                    }
                    entry.fullImage = null;
                    entry.requestedFullImage = MediaObject.INVALID_DATA_VERSION;
                }
                if (entry.requestedScreenNail != item.getDataVersion()) {
                    // This ScreenNail is outdated, we want to update it if it's
                    // still a placeholder.
                    // M: change to BitmapTexture from TiledTexture
                    if (entry.screenNail instanceof BitmapScreenNail) {
                        BitmapScreenNail s = (BitmapScreenNail) entry.screenNail;
                        s.updatePlaceholderSize(
                                item.getWidth(), item.getHeight());
                    }
                }

                // M: added for cancel Gif animation tasks
                if (mIsGifAnimationSupported &&
                    Math.abs(i - mCurrentIndex) > 0) {
                    if (entry.gifDecoderTask != null) {
                        entry.gifDecoderTask.cancel();
                        entry.gifDecoderTask = null;
                    }
                    entry.gifDecoder = null;
                    entry.requestedGif = MediaItem.INVALID_DATA_VERSION;
                    if (null != entry.currentGifFrame) {
                        //recycle cached gif frame
                        entry.currentGifFrame.recycle();
                        entry.currentGifFrame = null;
                    }
                }

                //added to decode original bitmap (not from cache, to 
                //improve image quality
                if (mReDecodeToImproveImageQuality && 
                    Math.abs(i - mCurrentIndex) > 0) {
                    if (entry.originScreenNailTask != null) {
                        entry.originScreenNailTask.cancel();
                        entry.originScreenNailTask = null;
                    }
                    if (null != entry.originScreenNail) {
                        entry.originScreenNail.recycle();
                        entry.originScreenNail = null;
                    }
                    entry.requestedOriginScreenNail = MediaObject.INVALID_DATA_VERSION;
                }

                //added for cancel second frame screen nail task
                if (mIsStereoDisplaySupported &&
                    Math.abs(i - mCurrentIndex) > 0) {
                    if (entry.secondScreenNailTask != null) {
                        entry.secondScreenNailTask.cancel();
                        entry.secondScreenNailTask = null;
                    }
                    entry.requestedSecondScreenNail = MediaObject.INVALID_DATA_VERSION;
                    if (null != entry.secondScreenNail) {
                        //recycle second screen nail
                        entry.secondScreenNail.recycle();
                        entry.secondScreenNail = null;
                    }
                    if (null != entry.firstScreenNail) {
                        //recycle first screen nail
                        entry.firstScreenNail.recycle();
                        entry.firstScreenNail = null;
                    }
                }
                
                // M: added for cancel mav tasks
                // maybe we can cache next and previous mav 
                if (mIsMavSupported &&
                    Math.abs(i - mCurrentIndex) > 0) {
                    MtkLog.d(TAG, "updateImageCache: release mav");
                    if (entry.mpoDecoderTask != null) {
                        entry.mpoDecoderTask.cancel();
                        entry.mpoDecoderTask = null;
                    }
                    entry.mpoTotalCount = 0;
                    entry.requestedMav = MediaItem.INVALID_DATA_VERSION;
                    entry.isMpoFrameRecyled = true;
                    //recycle cached mpo frame
                    if (null != entry.currentMpoFrame) {
                        entry.currentMpoFrame.recycle();
                        entry.currentMpoFrame = null;
                    }
                    if(mIsStereoDisplaySupported) {
                        if(null != entry.firstMpoFrame) {
                            entry.firstMpoFrame.recycle();
                            entry.firstMpoFrame = null;
                        }
                        if(null != entry.secondMpoFrame) {
                            entry.secondMpoFrame.recycle();
                            entry.secondMpoFrame = null;
                        }
                    }
                    // may be need to release all mpo frames
                    if(entry.mpoFrames != null) {
                        int length = entry.mpoFrames.length;
                        for(int idx = 0; idx < length; idx++) {
                            if(entry.mpoFrames[idx] != null) {
                                entry.mpoFrames[idx].recycle();
                                entry.mpoFrames[idx] = null;
                            }
                        }
                        entry.mpoFrames = null;
                    }
                }
            } else {
                entry = new ImageEntry();
                mImageCache.put(path, entry);
            }
        }

        // Clear the data and requests for ImageEntries outside the new window.
        for (Path path : toBeRemoved) {
            ImageEntry entry = mImageCache.remove(path);
            if (entry.fullImageTask != null) entry.fullImageTask.cancel();
            if (entry.screenNailTask != null) entry.screenNailTask.cancel();
            if (entry.screenNail != null) entry.screenNail.recycle();
            
            //added for gif animation: cancel gifDecoder task and recycle frame
            if (mIsGifAnimationSupported) {
                if (entry.gifDecoderTask != null) entry.gifDecoderTask.cancel();
                if (null != entry.currentGifFrame) {
                    //recycle cached gif frame
                    entry.currentGifFrame.recycle();
                    entry.currentGifFrame = null;
                }
            }
            
            // added for mav playback: cancel mpoDecoder task and recycle frame
            if (mIsMavSupported) {
                MtkLog.d(TAG, "updateImageCache: release mav");
                if (entry.mpoDecoderTask != null) {
                    entry.mpoDecoderTask.cancel();
                }
                entry.mpoTotalCount = 0;
                entry.isMpoFrameRecyled = true;
                //recycle cached mpo frame
                if (null != entry.currentMpoFrame) {
                    entry.currentMpoFrame.recycle();
                    entry.currentMpoFrame = null;
                }
                if(mIsStereoDisplaySupported) {
                    if(null != entry.firstMpoFrame) {
                        entry.firstMpoFrame.recycle();
                        entry.firstMpoFrame = null;
                    }
                    if(null != entry.secondMpoFrame) {
                        entry.secondMpoFrame.recycle();
                        entry.secondMpoFrame = null;
                    }
                }
                // may be need to release all mpo frames
                if(entry.mpoFrames != null) {
                    int length = entry.mpoFrames.length;
                    for(int idx = 0; idx < length; idx++) {
                        if(entry.mpoFrames[idx] != null) {
                            entry.mpoFrames[idx].recycle();
                            entry.mpoFrames[idx] = null;
                        }
                    }
                    entry.mpoFrames = null;
                }
            }

            //added to decode original bitmap (not from cache, to 
            //improve image quality
            if (mReDecodeToImproveImageQuality &&
                entry.originScreenNailTask != null) {
                entry.originScreenNailTask.cancel();
                if (null != entry.originScreenNail) {
                    entry.originScreenNail.recycle();
                    entry.originScreenNail = null;
                }
            }
            if (mIsStereoDisplaySupported) {
                if (entry.secondScreenNailTask != null) {
                    entry.secondScreenNailTask.cancel();
                }
                if (entry.secondScreenNail != null) {
                    //recycle second screen nail for stereo photo
                    entry.secondScreenNail.recycle();
                    entry.secondScreenNail = null;
                }
                if (entry.firstScreenNail != null) {
                    //recycle first screen nail for stereo photo
                    entry.firstScreenNail.recycle();
                    entry.firstScreenNail = null;
                }
            }
        }
        MediatekMMProfile.stopProfilePhotoPageUpdateImageCache();
    }

    private class FullImageListener
            implements Runnable, FutureListener<BitmapRegionDecoder> {
        private final Path mPath;
        private Future<BitmapRegionDecoder> mFuture;

        public FullImageListener(MediaItem item) {
            mPath = item.getPath();
        }

        @Override
        public void onFutureDone(Future<BitmapRegionDecoder> future) {
            mFuture = future;
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
        }

        @Override
        public void run() {
            updateFullImage(mPath, mFuture);
        }
    }

    private class ScreenNailListener
            implements Runnable, FutureListener<ScreenNail> {
        private final Path mPath;
        private Future<ScreenNail> mFuture;

        public ScreenNailListener(MediaItem item) {
            mPath = item.getPath();
        }

        @Override
        public void onFutureDone(Future<ScreenNail> future) {
            mFuture = future;
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
        }

        @Override
        public void run() {
            updateScreenNail(mPath, mFuture);
        }
    }

    private static class ImageEntry {
        public BitmapRegionDecoder fullImage;
        public ScreenNail screenNail;
        public Future<ScreenNail> screenNailTask;
        public Future<BitmapRegionDecoder> fullImageTask;
        public long requestedScreenNail = MediaObject.INVALID_DATA_VERSION;
        public long requestedFullImage = MediaObject.INVALID_DATA_VERSION;
        public boolean failToLoad = false;
        
        // M: added for gif
        public GifDecoderWrapper gifDecoder;
        public Future<DataBundle> gifDecoderTask;
        public ScreenNail currentGifFrame;
        public long requestedGif = MediaObject.INVALID_DATA_VERSION;
        
        //added to support DRM
        public boolean isDrm;
        public boolean enteredConsumeMode;
        //added to gain better image quality
        public Future<ScreenNail> originScreenNailTask;
        public ScreenNail originScreenNail;
        //added to support stereo display
        public Future<DataBundle> secondScreenNailTask;
        public ScreenNail firstScreenNail;
        public ScreenNail secondScreenNail;
        public int stereoLayout;

        public long requestedOriginScreenNail = MediaObject.INVALID_DATA_VERSION;
        public long requestedSecondScreenNail = MediaObject.INVALID_DATA_VERSION;
        
        /// M: added for mav playback
        // don't need mpoDecoder, because we'll get all mpo frames one time
//        public MpoDecoderWrapper mpoDecoder;
        public Future<DataBundle> mpoDecoderTask;
        public int mpoTotalCount;
        public ScreenNail currentMpoFrame;
        public ScreenNail firstMpoFrame;
        public ScreenNail secondMpoFrame;
//        public Bitmap[] mpoFrames;
        public ScreenNail[] mpoFrames;
        public long requestedMav = MediaObject.INVALID_DATA_VERSION;
        public boolean isMpoFrameRecyled = false;
    }

    private class SourceListener implements ContentListener {
        @Override
        public void onContentDirty() {
            if (mReloadTask != null) mReloadTask.notifyDirty();
        }
    }

    private <T> T executeAndWait(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<T>(callable);
        mMainHandler.sendMessage(
                mMainHandler.obtainMessage(MSG_RUN_OBJECT, task));
        try {
            return task.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class UpdateInfo {
        public long version;
        public boolean reloadContent;
        public Path target;
        public int indexHint;
        public int contentStart;
        public int contentEnd;

        public int size;
        public ArrayList<MediaItem> items;
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {

        private boolean needContentReload() {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                if (mData[i % DATA_CACHE_SIZE] == null) return true;
            }
            MediaItem current = mData[mCurrentIndex % DATA_CACHE_SIZE];
            return current == null || current.getPath() != mItemPath;
        }

        @Override
        public UpdateInfo call() throws Exception {
            MediatekMMProfile.startProfilePhotoPageGetUpdateInfo();
            // TODO: Try to load some data in first update
            UpdateInfo info = new UpdateInfo();
            info.version = mSourceVersion;
            info.reloadContent = needContentReload();
            info.target = mItemPath;
            info.indexHint = mCurrentIndex;
            info.contentStart = mContentStart;
            info.contentEnd = mContentEnd;
            info.size = mSize;
            MediatekMMProfile.stopProfilePhotoPageGetUpdateInfo();
            return info;
        }
    }

    private class UpdateContent implements Callable<Void> {
        UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo updateInfo) {
            mUpdateInfo = updateInfo;
        }

        @Override
        public Void call() throws Exception {
            MediatekMMProfile.startProfilePhotoPageUpdateContent();
            UpdateInfo info = mUpdateInfo;
            mSourceVersion = info.version;

            if (info.size != mSize) {
                mSize = info.size;
                if (mContentEnd > mSize) mContentEnd = mSize;
                if (mActiveEnd > mSize) mActiveEnd = mSize;
            }
            
            //M: indexHint needs to be limited to [0, mSize)
            if(mSize>0 && info.indexHint >= mSize){
                info.indexHint = mSize-1;
            }
            
            mCurrentIndex = info.indexHint;
            updateSlidingWindow();

            if (info.items != null) {
                int start = Math.max(info.contentStart, mContentStart);
                int end = Math.min(info.contentStart + info.items.size(), mContentEnd);
                int dataIndex = start % DATA_CACHE_SIZE;
                for (int i = start; i < end; ++i) {
                    mData[dataIndex] = info.items.get(i - info.contentStart);
                    if (++dataIndex == DATA_CACHE_SIZE) dataIndex = 0;
                }
            }

            // update mItemPath
            MediaItem current = mData[mCurrentIndex % DATA_CACHE_SIZE];
            mItemPath = current == null ? null : current.getPath();

            //added for drm consume behavior
            if (mIsDrmSupported) {
                restoreDrmConsumeStatus();
            }

            updateImageCache();
            updateTileProvider();
            updateImageRequests();

            if (mDataListener != null) {
                mDataListener.onPhotoChanged(mCurrentIndex, mItemPath);
            }

            fireDataChange();
            MediatekMMProfile.stopProfilePhotoPageUpdateContent();
            return null;
        }
    }

    private class ReloadTask extends Thread {
        private volatile boolean mActive = true;
        private volatile boolean mDirty = true;

        private boolean mIsLoading = false;

        private void updateLoading(boolean loading) {
            if (mIsLoading == loading) return;
            mIsLoading = loading;
            mMainHandler.sendEmptyMessage(loading ? MSG_LOAD_START : MSG_LOAD_FINISH);
        }

        @Override
        public void run() {
            while (mActive) {
                synchronized (this) {
                    if (!mDirty && mActive) {
                        updateLoading(false);
                        Utils.waitWithoutInterrupt(this);
                        continue;
                    }
                }
                MediatekMMProfile.startProfilePhotoPageReloadData();
                mDirty = false;
                UpdateInfo info = executeAndWait(new GetUpdateInfo());
                updateLoading(true);
                long version = mSource.reload();
                if (info.version != version) {
                    info.reloadContent = true;
                    info.size = mSource.getMediaItemCount();

                // M: for debug purpose only
                MtkLog.i(TAG, "[ReloadTask] run: set=" + mSource + 
                        ", name=" + mSource.getName() + ", item count=" + info.size);
                    if (mSource instanceof FilterDeleteSet) {
                        MediaSet underlyingSet = ((FilterDeleteSet) mSource).getUnderlyingSet();
                        if (underlyingSet != null && underlyingSet instanceof ComboAlbum) {
                            MtkLog.i(TAG, "[ReloadTask] run: combo info=" + 
                                    ((ComboAlbum) underlyingSet).getComboInfo());
                        }
                    }
                }
                if (!info.reloadContent) {
                    MediatekMMProfile.stopProfilePhotoPageReloadData();
                    continue;
                }
                // M: camera launch performance improve
                if (reloadCameraItem) {
                    reloadCameraItem = false;
                    info.items = mSource.getMediaItem(0, 1);
                    MtkLog.i(TAG, "[ReloadTask] reloadCameraItem info.items " + info.items.size() + " " + info.items.get(0));
                } else {
                    info.items = mSource.getMediaItem(
                                    info.contentStart, info.contentEnd);
                }
                int index = MediaSet.INDEX_NOT_FOUND;

                // First try to focus on the given hint path if there is one.
                if (mFocusHintPath != null) {
                    index = findIndexOfPathInCache(info, mFocusHintPath);
                    mFocusHintPath = null;
                }

                // Otherwise try to see if the currently focused item can be found.
                if (index == MediaSet.INDEX_NOT_FOUND) {
                    MediaItem item = findCurrentMediaItem(info);
                    if (item != null && item.getPath() == info.target) {
                        index = info.indexHint;
                    } else {
                        index = findIndexOfTarget(info);
                    }
                }

                // The image has been deleted. Focus on the next image (keep
                // mCurrentIndex unchanged) or the previous image (decrease
                // mCurrentIndex by 1). In page mode we want to see the next
                // image, so we focus on the next one. In film mode we want the
                // later images to shift left to fill the empty space, so we
                // focus on the previous image (so it will not move). In any
                // case the index needs to be limited to [0, mSize).
                if (index == MediaSet.INDEX_NOT_FOUND) {
                    index = info.indexHint;
                    int focusHintDirection = mFocusHintDirection;
                    if (index == (mCameraIndex + 1)) {
                        focusHintDirection = FOCUS_HINT_NEXT;
                    }
                    if (focusHintDirection == FOCUS_HINT_PREVIOUS
                            && index > 0) {
                        index--;
                    }
                }

                // Don't change index if mSize == 0
                if (mSize > 0) {
                    if (index >= mSize) index = mSize - 1;
                }

                info.indexHint = index;

                executeAndWait(new UpdateContent(info));
                MediatekMMProfile.stopProfilePhotoPageReloadData();
            }
        }

        public synchronized void notifyDirty() {
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }

        private MediaItem findCurrentMediaItem(UpdateInfo info) {
            ArrayList<MediaItem> items = info.items;
            int index = info.indexHint - info.contentStart;
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        private int findIndexOfTarget(UpdateInfo info) {
            if (info.target == null) return info.indexHint;
            ArrayList<MediaItem> items = info.items;

            // First, try to find the item in the data just loaded
            if (items != null) {
                int i = findIndexOfPathInCache(info, info.target);
                if (i != MediaSet.INDEX_NOT_FOUND) return i;
            }

            // Not found, find it in mSource.
            return mSource.getIndexOfItem(info.target, info.indexHint);
        }

        private int findIndexOfPathInCache(UpdateInfo info, Path path) {
            ArrayList<MediaItem> items = info.items;
            for (int i = 0, n = items.size(); i < n; ++i) {
                MediaItem item = items.get(i);
                if (item != null && item.getPath() == path) {
                    return i + info.contentStart;
                }
            }
            return MediaSet.INDEX_NOT_FOUND;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////

    // Mediatek added feature.
    private boolean updateTileProviderEx(ImageEntry entry) {
        Utils.assertTrue(null != entry && null != entry.screenNail);

        //for "drm" feature and "display image at its original size" feature,
        //we use MtkBitmapScreenNail to pass all info needed to display image
        //correctly, so for the two feature, we do following operation
        int subType = MediatekFeature.getScreenNailSubType(entry.screenNail);

        if (MediatekFeature.showDrmMicroThumb(subType)) {
            return false;
        }

        ScreenNail targetScreenNail = getTargetScreenNail(entry);
        if (null == targetScreenNail) {
            return false;
        }
        MediatekFeature.syncSubType(targetScreenNail, entry.screenNail);

        PhotoView.Size size = MediatekFeature.getSizeForSubtype(targetScreenNail);
        int w, h;
        if (size != null) {
            w = size.width;
            h = size.height;
        } else {
            w = targetScreenNail.getWidth();
            h = targetScreenNail.getHeight();
        }
        mTileProvider.setScreenNail(targetScreenNail, w, h);

        if (mIsStereoDisplaySupported) {
            //update stereo relate texture
            mTileProvider.setStereoScreenNail(1, entry.firstScreenNail);
            mTileProvider.setStereoScreenNail(2, entry.secondScreenNail);
        }

        if (null != entry.fullImage) {
            //adjust full image dimesion if needed
            int fullWidth = StereoHelper.adjustDim(true, entry.stereoLayout,
                                               entry.fullImage.getWidth());
            int fullHeight = StereoHelper.adjustDim(false, entry.stereoLayout,
                                               entry.fullImage.getHeight());
            mTileProvider.setRegionDecoder(entry.fullImage, targetScreenNail, 
                                    fullWidth, fullHeight);
        }
        return true;
    }

    private ScreenNail getTargetScreenNail(ImageEntry entry) {
        if (null == entry) return null;
        // M: gif frame is first priority screen nail
        if (null != entry.currentGifFrame) return entry.currentGifFrame;
        // M: mpo frame is second priority screen nail
        if (null != entry.currentMpoFrame) {
            MtkLog.d(TAG, "return current MpoFrame");
            return entry.currentMpoFrame;
        }
        // M: origin frame is third priority screen nail
        if (null != entry.originScreenNail) return entry.originScreenNail;
        // M: default screen nail is last priority
        return entry.screenNail;
    }

    private void updateDrmScreenNail() {
        if (!mIsDrmSupported) return;
        for (int i = mActiveStart; i < mActiveEnd; ++i) {
            MediaItem item = getItemInternal(i);
            if (null == item) continue;
            ImageEntry entry = mImageCache.get(item.getPath());
            if (entry != null) {
                MtkBitmapScreenNail mtkScreeNail =
                    MediatekFeature.toMtkScreenNail(entry.screenNail);
                if (null == mtkScreeNail) continue;

                int index = i - mCurrentIndex;
                if (index < -SCREEN_NAIL_MAX || 
                    index > SCREEN_NAIL_MAX) {
                    continue;
                }
                if (index != 0) {
                    //reset drm consume mode
                    entry.enteredConsumeMode = false;
                }
                // we need to check drm status for each screen nail
                if (updateDrmScreenNail(mtkScreeNail, item, entry)) {
                    //notify photo view to invalidate
                    mChanges[index + SCREEN_NAIL_MAX] = 
                                MediaObject.INVALID_DATA_VERSION;
                }
            }
        }
    }

    private boolean updateDrmScreenNail(MtkBitmapScreenNail mtkScreenNail,
                                 int index) {
        Path path = getPath(index);
        if (null == path) return false;
        ImageEntry entry = mImageCache.get(path);
        MediaItem item = getItem(index);
        return updateDrmScreenNail(mtkScreenNail, item, entry);
    }

    private boolean updateDrmScreenNail(MtkBitmapScreenNail mtkScreenNail,
                                 MediaItem item, ImageEntry entry) {
        Utils.assertTrue(null != mtkScreenNail &&
                         null != item && null != entry);
        int origSubType = item.getSubType();
        int drmSubMask = MediaObject.SUBTYPE_DRM_NO_RIGHT |
                         MediaObject.SUBTYPE_DRM_HAS_RIGHT;

        int subType = mtkScreenNail.getSubType();
        if (entry.enteredConsumeMode && 0 != (subType & drmSubMask) ||
            !entry.enteredConsumeMode && 0 == (subType & drmSubMask)) {
            //we should change the ui
            if (entry.enteredConsumeMode) {
                subType &= ~ drmSubMask;
                mtkScreenNail.setSubType(subType);
            } else {
                //reset drm related subtype
                mtkScreenNail.setSubType(origSubType);
            }
            return true;
        } else {
            return false;
        }
    }

    public void enterConsumeMode() {
        if (!mIsDrmSupported) return;
        // update mImagecache to avoid entry is null
        updateImageCache();
        ImageEntry entry = mImageCache.get(getPath(mCurrentIndex));

        if (entry == null) {
            Log.i(TAG, "enter is null");
            return;
        }
        if (entry.enteredConsumeMode) {
            //we should ignore it?
            Log.w(TAG,"enterConsumeMode:we already in consumed mode!");
            return;
        }

        //enter consumed mode
        entry.enteredConsumeMode = true;

        //record drm time interval type
        MediaItem item = getItemInternal(mCurrentIndex);
        if (item.isTimeInterval()) {
            mTimeIntervalDRM = true;
        }

        // If current media item is consume, DRM rights status other
        // of media item may be affected, so we need to notify PhotoView
        // to reload screen nails.
        // Note: there can only be drm file that looses its rights,
        // but never a drm file gains drm rights via consume mode.
        updateDrmScreenNail();
        //as TileProvider does not own screen nails loaded by PDA
        //we may not need to re-update tile provider.
        updateTileProvider();
        // told photo view to reload screen nails
        fireDataChange();
        //play gif animation if possible
        startGifAnimation(getPath(mCurrentIndex));
    }

    public boolean enteredConsumeMode() {
        ImageEntry entry = mImageCache.get(getPath(mCurrentIndex));
        return entry == null ? false : entry.enteredConsumeMode;
    }

    private void saveDrmConsumeStatus() {
        resetDrmConsumeStatus();
        ImageEntry entry = mImageCache.get(getPath(mCurrentIndex));
        if (null != entry && entry.enteredConsumeMode) {
            MediaItem current = mData[mCurrentIndex % DATA_CACHE_SIZE];
            mConsumedItemPath = current == null ? null : current.getPath();
        }
    }

    private void restoreDrmConsumeStatus() {
        if (null == mConsumedItemPath) return;
        ImageEntry entry = mImageCache.get(getPath(mCurrentIndex));

        //for time interval kind drm media, check if we still have
        //rights to display full image
        if (mTimeIntervalDRM) {
            Log.d(TAG,"restoreDrmConsumeStatus:for time interval media...");
            if (mCurrentIndex < 0){
                return;
            }
            MediaItem item = mData[mCurrentIndex % DATA_CACHE_SIZE];
            if (item == null){
                return;
            }
            if (!item.hasDrmRights()) {
                Log.d(TAG,"restoreDrmConsumeStatus:we have no rights ");
                resetDrmConsumeStatus();
                return;
            }
        }

        if (null != entry) {
            if (mItemPath == mConsumedItemPath) {
                if (entry.enteredConsumeMode) {
                    return;
                }
                //restore consumed mode
                enterConsumeMode();
            }
            resetDrmConsumeStatus();
        }
    }

    private void resetDrmConsumeStatus() {
        mConsumedItemPath = null;
    }

    private static class OriginScreenNailJob 
                             implements Job<ScreenNail> {
        private MediaItem mItem;
        private Params mParams;

        public OriginScreenNailJob(MediaItem item, Params params) {
            mItem = item;
            mParams = params;
        }

        @Override
        public ScreenNail run(JobContext jc) {
            /*
            DataBundle dataBundle
                    = mItem.requestImage(MediaItem.TYPE_THUMBNAIL,
                                                     mParams).run(jc);
            */
            Job<DataBundle> imageRequest = mItem.requestImage(
                    MediaItem.TYPE_THUMBNAIL, mParams);
            if (imageRequest == null) {
                return null;
            }
            DataBundle dataBundle = imageRequest.run(jc);
            if (jc.isCancelled() || null == dataBundle) {
                return null;
            }
            ScreenNail originScreenNail =
                MediatekFeature.getMtkScreenNail(mItem, dataBundle.originalFrame);

            if (null != originScreenNail) {
                return originScreenNail;
            }

            return null == dataBundle.originalFrame ?
                   null : new BitmapScreenNail(dataBundle.originalFrame);
        }
    }

    private class OriginScreenNailListener
            implements Runnable, FutureListener<ScreenNail> {
        private final Path mPath;
        private Future<ScreenNail> mFuture;

        public OriginScreenNailListener(MediaItem item) {
            mPath = item.getPath();
        }

        @Override
        public void onFutureDone(Future<ScreenNail> future) {
            mFuture = future;
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
        }

        @Override
        public void run() {
            updateOriginScreenNail(mPath, mFuture);
        }
    }

    private void updateOriginScreenNail(Path path, 
                      Future<ScreenNail> future) {
        ImageEntry entry = mImageCache.get(path);
        if (entry == null || entry.originScreenNailTask != future) {
            ScreenNail screenNail = future.get();
            if (null != screenNail) {
                if (screenNail != null) screenNail.recycle();
            }
            return;
        }

        entry.originScreenNailTask = null;
        entry.originScreenNail = future.get();

        if (entry.originScreenNail != null) {
            if (path == getPath(mCurrentIndex)) {
                updateTileProvider(entry);
                mPhotoView.notifyImageChange(0);
            }
        }
        updateImageRequests();
    }

    private class SecondScreenNailJob implements Job<DataBundle> {
        private MediaItem mItem;
        private Params mParams;

        public SecondScreenNailJob(MediaItem item, Params params) {
            mItem = item;
            mParams = params;
        }

        @Override
        public DataBundle run(JobContext jc) {
            // got the second frame of target stereo photo or video
            /*
            DataBundle dataBundle
                    = mItem.requestImage(MediaItem.TYPE_THUMBNAIL,
                                                     mParams).run(jc);
            */
            Job<DataBundle> imageRequest = mItem.requestImage(
                    MediaItem.TYPE_THUMBNAIL, mParams);
            if (imageRequest == null) {
                return null;
            }
            DataBundle dataBundle = imageRequest.run(jc);
            if (jc.isCancelled()) {
                return null;
            }
            return dataBundle;
        }
    }

    private class SecondScreenNailListener
            implements Runnable, FutureListener<DataBundle> {
        private final Path mPath;
        private Future<DataBundle> mFuture;

        public SecondScreenNailListener(MediaItem item) {
            mPath = item.getPath();
        }

        @Override
        public void onFutureDone(Future<DataBundle> future) {
            mFuture = future;
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
        }

        @Override
        public void run() {
            updateSecondScreenNail(mPath, mFuture);
        }
    }

    private void updateSecondScreenNail(Path path, 
                           Future<DataBundle> future) {
        ImageEntry entry = mImageCache.get(path);
        if (entry == null || entry.secondScreenNailTask != future) {
            DataBundle dataBundle = future.get();
            if (dataBundle != null) dataBundle.recycle();
            return;
        }

        entry.secondScreenNailTask = null;
        DataBundle dataBundle = future.get();

        if (null != dataBundle && dataBundle.secondFrame != null) {
            entry.firstScreenNail = dataBundle.firstFrame == null ?
                    null : new BitmapScreenNail(dataBundle.firstFrame);
            entry.secondScreenNail = new BitmapScreenNail(dataBundle.secondFrame);

            if (path == getPath(mCurrentIndex)) {
                Log.v(TAG,"updateSecondScreenNail:update tileProvider");
                updateTileProvider(entry);
                mPhotoView.notifyImageChange(0);
                MediaItem item = mData[mCurrentIndex % DATA_CACHE_SIZE];
                //the 2d to 3d picture should not enter stereo mode.
                //Enter stereo mode only when stereo photo is encountered

                if ((item.getSupportedOperations() & 
                     MediaObject.SUPPORT_CONVERT_TO_3D) == 0) {
                    //stereo photo
                    //after loaded second image, all enter stereo mode
                    mPhotoView.allowStereoMode(true);
                    mPhotoView.setStereoMode(true);
                } else {
                    //2d to 3d kind
                    //after loaded second image, all exit stereo mode
                    mPhotoView.allowStereoMode(false);
                    mPhotoView.setStereoMode(false);
                }
            }
        }
        updateImageRequests();
    }

    public ScreenNail getStereoScreenNail(int stereoIndex) {
        return mTileProvider.getStereoScreenNail(stereoIndex);
    }

    // M: following code are added for Gif decoder    
    private class GifDecoderListener
            implements Runnable, FutureListener<DataBundle> {
        private final Path mPath;
        private Future<DataBundle> mFuture;

        public GifDecoderListener(Path path) {
            mPath = path;
        }

        @Override
        public void onFutureDone(Future<DataBundle> future) {
            Log.i(TAG, "GifDecoderListener.onFutureDone: future=" + future.get());
            mFuture = future;
            if (MediatekFeature.isGifSupported() && null != mFuture.get()) {
                mMainHandler.sendMessage(
                        mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
            }
        }

        @Override
        public void run() {
            updateGifDecoder(mPath, mFuture);
        }
    }

    private void updateGifDecoder(Path path, 
                     Future<DataBundle> future) {
        Log.d(TAG, ">> updateGifDecoder");
        ImageEntry entry = mImageCache.get(path);
        if (entry == null || entry.gifDecoderTask != future) {
            Log.e(TAG,"updateGifDecoder:invalid entry or future");
            return;
        }

        entry.gifDecoderTask = null;
        entry.gifDecoder = future.get().gifDecoder;

        //if this is a GIF that decoded from DRM file, check rights first.
        MtkBitmapScreenNail mtkScreenNail = 
            MediatekFeature.toMtkScreenNail(entry.screenNail);
        if (null != mtkScreenNail && DrmHelper.showDrmMicroThumb(
                                         mtkScreenNail.getSubType())) {
            Log.d(TAG, "updateGifDecoder:no animation for non-consume drm");
        } else {
            startGifAnimation(path);
        }
        updateImageRequests();
        Log.d(TAG, "<< updateGifDecoder");
    }

    private void startGifAnimation(Path path) {
        ImageEntry entry = mImageCache.get(path);
        if (null == entry) return;
        if (entry.gifDecoder != null && 
            entry.gifDecoder.getTotalFrameCount() != 
            GifDecoderWrapper.INVALID_VALUE) {
            //we also have to check if width & height got from GifDecoder
            //is valid.
            //Note: in Bitmap object, the max Bitmap buffer size max be
            //within a 32bit integer. w * h * 4 < 2^32. 4 for ARGB.
            if (entry.gifDecoder.getWidth() <= 0 ||
                    entry.gifDecoder.getHeight() <= 0 ||
                    (long)entry.gifDecoder.getWidth() *
                    (long)entry.gifDecoder.getHeight() * 4
                    >= (long)65536*(long)65536) {
                Log.e(TAG,"startGifAnimation:illegal gif frame dimension");
                return;
            }

            int currentIndex = mCurrentIndex;
            if (path == getPath(currentIndex)) {
                //MediaItem item = mData[currentIndex % DATA_CACHE_SIZE];

                GifAnimation gifAnimation = new GifAnimation();
                gifAnimation.gifDecoder = entry.gifDecoder;
                gifAnimation.animatedIndex = currentIndex;
                gifAnimation.entry = entry;

                mMainHandler.sendMessage(
                        mMainHandler.obtainMessage(MSG_RUN_OBJECT, 
                                  new GifAnimationRunnable(path, gifAnimation)));
            }
        }
    }

    private class GifAnimationRunnable implements Runnable {

        private GifAnimation mGifAnimation;
        private Path mPath;

        private void releaseGifResource() {
            if (null != mGifAnimation) {
                mGifAnimation.gifDecoder = null;
                //mGifAnimation.mediaItem = null;
                mGifAnimation = null;
            }
        }

        public GifAnimationRunnable(Path path, GifAnimation gifAnimation) {
            mPath = path;
            mGifAnimation = gifAnimation;
            if (null == mGifAnimation || null == mGifAnimation.gifDecoder) {
                Log.e(TAG,"GifAnimationRunnable:invalid GifDecoder");
                releaseGifResource();
                return;
            }
            boolean imageChanged = mGifAnimation.animatedIndex != mCurrentIndex;
            MediaItem item = getMediaItem(0);
            Path currentPath = (item != null ? item.getPath() : null);
            imageChanged |= path != currentPath;
            if (imageChanged) {
                Log.i(TAG,"GifAnimationRunnable:image changed");
                releaseGifResource();
                return;
            }
            //prepare Gif animation state
            mGifAnimation.currentFrame = 0;
            mGifAnimation.totalFrameCount =
                                   mGifAnimation.gifDecoder.getTotalFrameCount();
            if (mGifAnimation.totalFrameCount <= 1) {
                Log.w(TAG,
                       "GifAnimationRunnable:invalid frame count, NO animation!");
                releaseGifResource();
                return;
            }
        }

        @Override
        public void run() {
            //Log.d(TAG, ">> GifAnimationRunnable.run");
            if (!mIsActive) {
                Log.i(TAG,"GifAnimationRunnable:run:already paused");
                releaseGifResource();
                return;
            }

            if (null == mGifAnimation || null == mGifAnimation.gifDecoder) {
                Log.e(TAG,"GifAnimationRunnable:run:invalid GifDecoder");
                releaseGifResource();
                return;
            }

            boolean imageChanged = mGifAnimation.animatedIndex != mCurrentIndex;
            MediaItem item = getMediaItem(0);
            Path currentPath = (item != null ? item.getPath() : null);
            imageChanged |= mPath != currentPath;
            if (imageChanged) {
                Log.i(TAG," GifAnimationRunnable:run:image changed");
                releaseGifResource();
                return;
            }

            //assign decoded bitmap to CurrentGifFrame
            Bitmap curBitmap = mGifAnimation.gifDecoder.getFrameBitmap(
                                                  mGifAnimation.currentFrame);
            if (null == curBitmap) {
                Log.e(TAG,"GifAnimationRunnable:run:got null frame!");
                releaseGifResource();
                return;
            }
            //as max texture size is 2048, we have to make sure that decoded
            //gif frame does not exceeds that size
            //TODO: 2048 should be replaced with meaningful constant variable
            curBitmap = BitmapUtils.resizeDownBySideLength(curBitmap, 2048, true);
            curBitmap = MediatekFeature.replaceGifBackground(curBitmap);
            Log.i(TAG,"GifAnimationRunnable:run:update frame["+
                                                (mGifAnimation.currentFrame)+"]");

            //get curent frame duration
            long curDuration = (long) mGifAnimation.gifDecoder.getFrameDuration(
                                                        mGifAnimation.currentFrame);
            //calculate next frame index
            mGifAnimation.currentFrame = (mGifAnimation.currentFrame + 1) %
                                                    mGifAnimation.totalFrameCount;

            ScreenNail gifFrame = null;
            ScreenNail screenNail = 
                   MediatekFeature.getMtkScreenNail(getMediaItem(0), curBitmap);
            if (null == screenNail) {
                screenNail = new BitmapScreenNail(curBitmap);
            }
            gifFrame = screenNail;

            if (mGifAnimation.entry.currentGifFrame != null) {
                mGifAnimation.entry.currentGifFrame.recycle();
                mGifAnimation.entry.currentGifFrame = null;
            }

            mGifAnimation.entry.currentGifFrame = gifFrame;

            updateTileProvider(mGifAnimation.entry);
            //mPhotoView.notifyImageInvalidated(0);
            mPhotoView.notifyImageChange(0);

            mMainHandler.sendMessageDelayed(
                mMainHandler.obtainMessage(MSG_RUN_OBJECT, this), curDuration);
            //Log.d(TAG, "<< GifAnimationRunnable.run");
        }
    }

    private static class GifAnimation {
        public ImageEntry entry;
        public GifDecoderWrapper gifDecoder;
        public int animatedIndex;
        public int currentFrame;
        public int totalFrameCount;
    }
    
    // M: added for MAV
    @Override
    public boolean isMav(int offset) {
        MediaItem item = getItem(mCurrentIndex + offset);
        if (item == null) {
            return false;
        }
        boolean isMavType = (item.getSubType() & MediaObject.SUBTYPE_MPO_MAV) != 0;
        return isMavType;
    }
    
    // M: added for modifying image data for MTK features
    private void updateScreenNailSize(ScreenNail screenNail, MediaItem item) {
        if (MediatekFeature.getImageOptions().shouldUseOriginalSize()) {
        }
    }

    public static final int TYPE_LOAD_TOTAL_COUNT = 0; 
    public static final int TYPE_LOAD_FRAME = 1; 
    
    // M: following code are added for mav decoder    
    private class MavDecoderListener
            implements Runnable, FutureListener<DataBundle> {
        private final Path mPath;
        private Future<DataBundle> mFuture;
        private int mType;
        private MediaItem mItem;

        public MavDecoderListener(Path path, MediaItem item, int type) {
            mPath = path;
            mType = type;
            mItem = item;
        }

        @Override
        public void onFutureDone(Future<DataBundle> future) {
            Log.i(TAG, "MavDecoderListener.onFutureDone: future=" + future.get());
            mFuture = future;
            if (MediatekFeature.isMAVSupported() && null != mFuture.get()) {
                mMainHandler.sendMessage(
                        mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
            }
        }

        @Override
        public void run() {
            updateMavDecoder(mPath, mFuture, mItem, mType);
        }
    }
    
    private void updateMavDecoder(Path path, Future<DataBundle> future, MediaItem item, int type) {
        Log.d(TAG, ">> updateMavDecoder, type: " + type);
        ImageEntry entry = mImageCache.get(path);
        if (entry == null || entry.mpoDecoderTask != future) {
           Log.e(TAG,"updateMpoDecoder:invalid entry or future");
           return;
        }
        if(type == TYPE_LOAD_TOTAL_COUNT) {
            entry.mpoDecoderTask = null;
            entry.mpoTotalCount = future.get().mpoTotalCount;
            MtkLog.d(TAG, "the mav total count is " + entry.mpoTotalCount);
            
            // update MavSeekbar's max value and current progress
            if(mMavListener != null) {
                mMavListener.setSeekBar(entry.mpoTotalCount - 1, 0);
            }
            
            // submit task to decode frames
            Params params = new Params();
            params.inMpoFrames = true;//we want mpo frames
            // get windows size
            Display defaultDisplay = ((Activity)mActivity).getWindowManager().getDefaultDisplay();
            long availableMemory = MediatekFeature.availableMemoryForMavPlayback(mActivity);
            if(availableMemory > MediatekFeature.MEMORY_THRESHOLD_MAV_L1) {
                params.inTargetDisplayHeight = defaultDisplay.getHeight();
                params.inTargetDisplayWidth = defaultDisplay.getWidth();
            } else if(availableMemory <= MediatekFeature.MEMORY_THRESHOLD_MAV_L1 && availableMemory > MediatekFeature.MEMORY_THRESHOLD_MAV_L2){
                MtkLog.d(TAG, "no enough memory, degrade sample rate to 1/4");
                params.inTargetDisplayHeight = defaultDisplay.getHeight() / 2;
                params.inTargetDisplayWidth = defaultDisplay.getWidth() / 2;
            } else if(availableMemory <= MediatekFeature.MEMORY_THRESHOLD_MAV_L2){
                MtkLog.d(TAG, "no enough memory, degrade sample rate to 1/9");
                params.inTargetDisplayHeight = (int)(defaultDisplay.getHeight() / 3);
                params.inTargetDisplayWidth = (int)(defaultDisplay.getWidth() / 3);
            }
            params.inPQEnhance = true;
            MtkLog.d(TAG, "display width: " + defaultDisplay.getWidth() + ", height: " + defaultDisplay.getHeight());
            
            // set MavListener, it's used to update MavSeekBar's progress by decode progress
            item.setMavListener(mMavListener);
            MtkLog.d(TAG, "start load all mav frames");
            entry.mpoDecoderTask = mThreadPool.submit(
                    item.requestImage(MediaItem.TYPE_THUMBNAIL, params),
                    new MavDecoderListener(item.getPath(), item, TYPE_LOAD_FRAME));
        } else if(type == TYPE_LOAD_FRAME) {
            entry.mpoDecoderTask = null;
            entry.mpoFrames = getScreenNails(future.get().mpoFrames);
            MtkLog.d(TAG, "mpo frame width: " + entry.mpoFrames[0].getWidth() + ", height: " + entry.mpoFrames[0].getHeight());
            entry.isMpoFrameRecyled = false;
            
//            MtkLog.d(TAG, "get mpoFrames: ");
//            for(int i = 0; i < entry.mpoFrames.length; i++) {
//                MtkLog.d(TAG, "mpoFrames[" + i + "]: " + entry.mpoFrames[i] + " w,h: " + entry.mpoFrames[i].getWidth() + ", " + entry.mpoFrames[i].getHeight());
//            }
            // set the flag as true, it's used to hide mav icon
            mIsMavLoadingFinished = true;
            mPhotoView.setMavLoadingFinished(mIsMavLoadingFinished);
            startMavPlayback(path);
            updateImageRequests();
        }
        Log.d(TAG, "<< updateMavDecoder");
    }
    
    private ScreenNail[] getScreenNails(Bitmap[] bmps) {
        if(bmps == null) {
            return null;
        }
        int len = bmps.length;
        ScreenNail[] screenNails = new ScreenNail[len];
        for(int i = 0; i < len; i++) {
            if(bmps[i] == null) {
                MtkLog.d(TAG, "bmps[" + i + "] is null");
            }
            ScreenNail screenNail = MediatekFeature.getMtkScreenNail(getMediaItem(0), bmps[i]);
            if(screenNail == null) {
                screenNail = new BitmapScreenNail(bmps[i]);
            }
            screenNails[i] = screenNail;
        }
        return screenNails;
    }
    
    private void startMavPlayback(Path path) {
        MtkLog.d(TAG, "startMavPlayback");
        ImageEntry entry = mImageCache.get(path);
        if (null == entry) {
            return;
        }
        int totalCount = entry.mpoTotalCount;
        if (totalCount >  0) {
            // TODO check mpo frame size if has limitation
            int currentIndex = mCurrentIndex;
            if (path == getPath(currentIndex)) {
                // set max value and middle position 
                int middleFrame = (int) (totalCount / 2);
                MtkLog.d(TAG, "the middle frame is " + middleFrame);
                if(mMavListener != null) {
                    mMavListener.setSeekBar(totalCount - 1, middleFrame);
                    mMavListener.setStatus(true);
                }
                
                // start mav render thread
                new MavRenderThread(path).start();
            } else {
                MtkLog.e(TAG, "incorrect path: " + path.toString());
            }
        } else {
            MtkLog.e(TAG, "mpoTotalCount <= 0");
        }
    }
    
    private Object mRenderLock = new Object();
    public boolean mRenderRequested = false;
    
    private void requestRender() {
        MtkLog.d(TAG, "requestRender");
        long time = SystemClock.uptimeMillis();
        synchronized (mRenderLock) {
            mRenderRequested = true;
            mRenderLock.notifyAll();
        }
        time = SystemClock.uptimeMillis() - time;
        Log.i(TAG, "request render consumed " + time + "ms");
    }
    
    
    private ScreenNail mCurrentScreenNail;
    private ScreenNail mFirstScreenNail;
    private ScreenNail mSecondScreenNail;
    private ScreenNail mOldCurrentScreenNail;
    private ScreenNail mOldFirstScreenNail;
    private ScreenNail mOldSecondScreenNail;
    
    private boolean mIsMavStereoMode = false;
    // render thread for drawing continuous
    private class MavRenderThread extends Thread {
        
        private Path mPath;
        private ImageEntry mMavEntry;
        
        public MavRenderThread(Path path) {
            this.mPath = path;
            mMavEntry = mImageCache.get(path);
            mIsMavStereoMode = mIsStereoDisplaySupported;
        }
        
        private void renderCurrentFrame() {
            if(mIsMavStereoMode) {
                // TODO uncomment if support stereo3d in jb2
                
                /*if(mCurrentMpoIndex < 0 || mMavEntry.mpoFrames == null || mCurrentMpoIndex > mMavEntry.mpoFrames.length) {
                    MtkLog.d(TAG, "[renderCurrentFrame]mCurrentMpoIndex[" + mCurrentMpoIndex + "] out of bounds");
                    return;
                }
                if(mNextMpoIndex < 0 || mMavEntry.mpoFrames == null || mNextMpoIndex > mMavEntry.mpoFrames.length) {
                    MtkLog.d(TAG, "[renderCurrentFrame]mCurrentMpoIndex[" + mCurrentMpoIndex + "] out of bounds");
                    return;
                }
                
                // TODO the index will be out of bounds
                mFirstScreenNail = mMavEntry.mpoFrames[mCurrentMpoIndex];
                mSecondScreenNail = mMavEntry.mpoFrames[mNextMpoIndex];
                
                mMavEntry.stereoLayout = StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
                mMavEntry.firstMpoFrame = mFirstScreenNail;
                mMavEntry.secondMpoFrame = mSecondScreenNail;
                updateTileProvider(mMavEntry);
                mPhotoView.notifyImageChange(0);
               
                mPhotoView.setStereoLayout(StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT);
                mPhotoView.setStereoMode(true, true);*/
            } else {
                if (mCurrentMpoIndex < 0 || mMavEntry.mpoFrames == null
                        || mCurrentMpoIndex >= mMavEntry.mpoFrames.length) {
                    MtkLog.d(TAG, "[renderCurrentFrame]mCurrentMpoIndex[" + mCurrentMpoIndex
                            + "] out of bounds");
                    return;
                }
                // as max texture size is 2048, we have to make sure that decoded
                // mav frame does not exceeds that size
                // TODO: 2048 should be replaced with meaningful constant variable
                // curBitmap = BitmapUtils.resizeDownBySideLength(curBitmap, 2048, true);
                mCurrentScreenNail = mMavEntry.mpoFrames[mCurrentMpoIndex];

                // M: when mpo frame has been recyle, not render
                if (mMavEntry.isMpoFrameRecyled) {
                    MtkLog.d(TAG, "[renderCurrentFrame] mpo frame has been recyled, return");
                    return;
                }

                mMavEntry.currentMpoFrame = mCurrentScreenNail;

                updateTileProvider(mMavEntry);
                mPhotoView.notifyImageChange(0);
            }
        }
        
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
            while (true) {
                if (!mIsActive) {
                    MtkLog.v(TAG, "MavRenderThread:run: exit MavRenderThread");
                    return;
                }
                // need conditions to trigger abort thread action
                MediaItem item = getMediaItem(0);
                Path currentPath = (item != null ? item.getPath() : null);
                boolean imageChanged = mPath != currentPath;
                // if current image has changed or paused activity
                // release resource and abort thread
                if (imageChanged) {
                    MtkLog.v(TAG, "MavRenderThread:run: image has changed, exit MavRenderThread");
                    return;
                }

                synchronized (mRenderLock) {
                    if (mRenderRequested) {
                        mRenderRequested = false;
                    } else {
                        try {
                            mRenderLock.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
                renderCurrentFrame();
            }
        }
    }

    private int mCurrentMpoIndex = -1;
    private int mNextMpoIndex = -1;
    public void setImageBitmap(int index) {
        // get mpo frames from ImageCache to avoid conflict
        MediaItem currentItem = getMediaItem(0);
        Path currentPath = (currentItem != null ? currentItem.getPath() : null);
        if(currentPath == null) {
            MtkLog.d(TAG, "setImageBitmap: the currentPath is null");
        }
        ImageEntry mavEntry = mImageCache.get(currentPath);
        if(mavEntry == null) {
            MtkLog.v(TAG, "setImageBitmap: the mavEntry is null");
            return;
        }
        if (mavEntry.mpoFrames == null) {
            MtkLog.v(TAG, "setImageBitmap: the mpoFrames of current entry is null");
            return;
        }
        int nextIndex = 0;
        int arrayLen = 0;
        
        arrayLen = mavEntry.mpoFrames.length;
        if (index >= 0 && index < arrayLen) {
            mCurrentMpoIndex = index;
            MtkLog.d(TAG, "get current mpo frame, index: " + index);
        }
            
        if (mIsMavStereoMode) {
            if (arrayLen > 1) {
                nextIndex = index + 1;
                if (nextIndex < 0) {
                    nextIndex = index;
                } else if (nextIndex > arrayLen - 1) {
                    nextIndex = arrayLen - 1;
                }
            }
        } else {
            nextIndex = index;
        }
        mNextMpoIndex = nextIndex;
        MtkLog.d(TAG, "get next mpo frame, index: " + nextIndex);
        
        requestRender();
    }

    public void setMavListener(MavListener listener) {
        mMavListener = listener;
    }

    public int getTotalFrameCount() {
        ImageEntry entry = null;
        MediaItem item = getMediaItem(0);
        Path currentPath = (item != null ? item.getPath() : null);
        if(currentPath != null) {
            entry = mImageCache.get(currentPath);
        }
        if(entry != null) {
            return entry.mpoTotalCount;
        }
        return 0;
    }

    public void updateMavStereoMode(boolean isMavStereoMode) {
        mIsMavStereoMode = isMavStereoMode;
    }

    public boolean isMavLoadingFinished() {
        return mIsMavLoadingFinished;
    }
    
    public void cancelCurrentMavDecodeTask() {
        ImageEntry entry = mImageCache.get(getPath(getCurrentIndex()));
        if (mIsMavSupported && entry != null) {
            if (entry.mpoDecoderTask != null) {
                entry.mpoDecoderTask.cancel();
                entry.mpoDecoderTask = null;
            }
            entry.mpoTotalCount = 0;
            entry.requestedMav = MediaItem.INVALID_DATA_VERSION;
        }
    }
}
