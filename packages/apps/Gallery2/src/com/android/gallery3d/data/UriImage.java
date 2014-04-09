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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.PanoramaMetadataSupport;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool.CancelListener;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.app.PhotoDataAdapter.MavListener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

// M: mediatek import
import java.io.FileDescriptor;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.mediatek.mpo.MpoDecoder;

import com.mediatek.gallery3d.data.RequestHelper;
import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.mpo.MpoHelper;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class UriImage extends MediaItem {
    private static final String TAG = "Gallery2/UriImage";

    private static final int STATE_INIT = 0;
    private static final int STATE_DOWNLOADING = 1;
    private static final int STATE_DOWNLOADED = 2;
    private static final int STATE_ERROR = -1;

    //added to support Mediatek features
    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsMpoSupported = 
                                          MediatekFeature.isMpoSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();
    private static final boolean mIsDisplay2dAs3dSupported = 
            MediatekFeature.isDisplay2dAs3dSupported();

    private int mMpoSubType = -1;

    private final Uri mUri;
    private final String mContentType;

    private DownloadCache.Entry mCacheEntry;
    private ParcelFileDescriptor mFileDescriptor;
    private int mState = STATE_INIT;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private PanoramaMetadataSupport mPanoramaMetadata = new PanoramaMetadataSupport(this);

    private GalleryApp mApplication;

    //added for mav feature, about mav progress bar
    public MavListener mMavListener;
    public UriImage(GalleryApp application, Path path, Uri uri, String contentType) {
        super(path, nextVersionNumber());
        mUri = uri;
        mApplication = Utils.checkNotNull(application);
        mContentType = contentType;
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
        return new BitmapJob(type);
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
        return new RegionDecoderJob();
    }

    private void openFileOrDownloadTempFile(JobContext jc) {
        int state = openOrDownloadInner(jc);
        synchronized (this) {
            mState = state;
            if (mState != STATE_DOWNLOADED) {
                if (mFileDescriptor != null) {
                    Utils.closeSilently(mFileDescriptor);
                    mFileDescriptor = null;
                }
            }
            notifyAll();
        }
    }

    private int openOrDownloadInner(JobContext jc) {
        String scheme = mUri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                || ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                if (MIME_TYPE_JPEG.equalsIgnoreCase(mContentType)) {
                    InputStream is = mApplication.getContentResolver()
                            .openInputStream(mUri);
                    mRotation = Exif.getOrientation(is);
                    Utils.closeSilently(is);
                }
                mFileDescriptor = mApplication.getContentResolver()
                        .openFileDescriptor(mUri, "r");
                if (jc.isCancelled()) return STATE_INIT;
                return STATE_DOWNLOADED;
            } catch (FileNotFoundException e) {
                Log.w(TAG, "fail to open: " + mUri, e);
                return STATE_ERROR;
            }
        } else {
            try {
                URL url = new URI(mUri.toString()).toURL();
                mCacheEntry = mApplication.getDownloadCache().download(jc, url);
                if (jc.isCancelled()) return STATE_INIT;
                if (mCacheEntry == null) {
                    Log.w(TAG, "download failed " + url);
                    return STATE_ERROR;
                }
                if (MIME_TYPE_JPEG.equalsIgnoreCase(mContentType)) {
                    InputStream is = new FileInputStream(mCacheEntry.cacheFile);
                    mRotation = Exif.getOrientation(is);
                    Utils.closeSilently(is);
                }
                mFileDescriptor = ParcelFileDescriptor.open(
                        mCacheEntry.cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
                return STATE_DOWNLOADED;
            } catch (Throwable t) {
                Log.w(TAG, "download error", t);
                return STATE_ERROR;
            }
        }
    }

    private boolean prepareInputFile(JobContext jc) {
        jc.setCancelListener(new CancelListener() {
            @Override
            public void onCancel() {
                synchronized (this) {
                    notifyAll();
                }
            }
        });

        while (true) {
            synchronized (this) {
                if (jc.isCancelled()) return false;
                if (mState == STATE_INIT) {
                    mState = STATE_DOWNLOADING;
                    // Then leave the synchronized block and continue.
                } else if (mState == STATE_ERROR) {
                    return false;
                } else if (mState == STATE_DOWNLOADED) {
                    return true;
                } else /* if (mState == STATE_DOWNLOADING) */ {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignored.
                    }
                    continue;
                }
            }
            // This is only reached for STATE_INIT->STATE_DOWNLOADING
            openFileOrDownloadTempFile(jc);
        }
    }

    private class RegionDecoderJob implements Job<BitmapRegionDecoder> {
        @Override
        public BitmapRegionDecoder run(JobContext jc) {
            if (!prepareInputFile(jc)) return null;
            BitmapRegionDecoder decoder = DecodeUtils.createBitmapRegionDecoder(
                    jc, mFileDescriptor.getFileDescriptor(), false);
            mWidth = decoder.getWidth();
            mHeight = decoder.getHeight();
            return decoder;
        }
    }

    private class BitmapJob implements Job<Bitmap> {
        private int mType;

        protected BitmapJob(int type) {
            mType = type;
        }

        @Override
        public Bitmap run(JobContext jc) {
            //reset mState each time we want to prepare
            //Is is a very bad solution for image not updated
            //issue ?
            mState = STATE_INIT;

            if (!prepareInputFile(jc)) return null;
            int targetSize = MediaItem.getTargetSize(mType);
            Options options = new Options();
            options.inPreferredConfig = Config.ARGB_8888;

            // M: for picture quality enhancement
            MediatekFeature.enablePictureQualityEnhance(options, true);

            //check if we need to extract Origin image dimensions
            extractImageInfo(jc);

            Bitmap bitmap = decodeBitmapEx(jc, mApplication, mUri, mContentType, 
                    mType, options, targetSize);
            if (null != bitmap) return bitmap;

            bitmap = DecodeUtils.decodeThumbnail(jc,
                mFileDescriptor.getFileDescriptor(), options, targetSize, mType);

            if (jc.isCancelled() || bitmap == null) {
                return null;
            }

            if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                bitmap = BitmapUtils.resizeAndCropCenter(bitmap, targetSize, true);
            } else {
                bitmap = BitmapUtils.resizeDownBySideLength(bitmap, targetSize, true);
            }
            
            // M: replace gif background and ensure GL compatible bitmap
            if (MediatekFeature.isGifSupported() &&
                0 != (getSupportedOperations() & SUPPORT_GIF_ANIMATION)) {
                //if needed, replace gif background
                bitmap = MediatekFeature.replaceGifBackground(bitmap);
            }
            return DecodeUtils.ensureGLCompatibleBitmap(bitmap);
            //return bitmap;
        }
    }

    @Override
    public int getSupportedOperations() {
        int supported = SUPPORT_EDIT | SUPPORT_SETAS;
        if (isSharable()) supported |= SUPPORT_SHARE;
        if (BitmapUtils.isSupportedByRegionDecoder(mContentType)) {
            supported |= SUPPORT_FULL_IMAGE;
            if(MediatekFeature.MIMETYPE_MPO.equals(mContentType) && 
                    getSubType() == MediaObject.SUBTYPE_MPO_MAV) {
                MtkLog.v(TAG, "current type is mpo_mav, don't support full image, path: " + mPath);
                supported &= ~SUPPORT_FULL_IMAGE; 
            }
        }

        supported = getModifiedOperations(supported);

        return supported;
    }
    
    @Override
    public void getPanoramaSupport(PanoramaSupportCallback callback) {
        mPanoramaMetadata.getPanoramaSupport(mApplication, callback);
    }

    @Override
    public void clearCachedPanoramaSupport() {
        mPanoramaMetadata.clearCachedValues();
    }

    private boolean isSharable() {
        // We cannot grant read permission to the receiver since we put
        // the data URI in EXTRA_STREAM instead of the data part of an intent
        // And there are issues in MediaUploader and Bluetooth file sender to
        // share a general image data. So, we only share for local file.
        return ContentResolver.SCHEME_FILE.equals(mUri.getScheme());
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_IMAGE;
    }

    @Override
    public Uri getContentUri() {
        return mUri;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        if (mWidth != 0 && mHeight != 0) {
            details.addDetail(MediaDetails.INDEX_WIDTH, mWidth);
            details.addDetail(MediaDetails.INDEX_HEIGHT, mHeight);
        }
        if (mContentType != null) {
            details.addDetail(MediaDetails.INDEX_MIMETYPE, mContentType);
        }
        if (ContentResolver.SCHEME_FILE.equals(mUri.getScheme())) {
            String filePath = mUri.getPath();
            details.addDetail(MediaDetails.INDEX_PATH, filePath);
            MediaDetails.extractExifInfo(details, filePath);
        }
        return details;
    }

    @Override
    public String getMimeType() {
        return mContentType;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mFileDescriptor != null) {
                Utils.closeSilently(mFileDescriptor);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public int getWidth() {
        return mWidth;//0;
    }

    @Override
    public int getHeight() {
        return mHeight;//0;
    }

    @Override
    public int getRotation() {
        return mRotation;
    }


    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public int getSubType() {
        int subType = 0;
        if (mIsMpoSupported && 
            MpoHelper.MPO_MIME_TYPE.equalsIgnoreCase(mContentType)) {
            initMpoSubType();
            if (MpoDecoder.MTK_TYPE_MAV == mMpoSubType) {
                subType |= MediaObject.SUBTYPE_MPO_MAV;
            } //else if
        }
        //display image at its origin size, MAV is not regarded as
        //normal image
        if (MediatekFeature.preferDisplayOriginalSize() &&
            0 == (subType & SUBTYPE_MPO_MAV)) {
            subType |= SUBTYPE_ORIGIN_SIZE;
        }

        return subType;
    }

    private void initMpoSubType() {
        if (-1 == mMpoSubType) {
            ContentResolver resolver = mApplication.getContentResolver();
            MpoDecoder mpoDecoder = MpoDecoder.decodeUri(resolver, mUri);
            if (null != mpoDecoder) {
                mMpoSubType = mpoDecoder.suggestMtkMpoType();
                mpoDecoder.close();
                Log.d(TAG, "initMpoSubType:mMpoSubType="+mMpoSubType);
            }
        }
    }

    // M: added for all mediatek added feature
    private int getModifiedOperations(int supported) {
        //modify supported operation according to image capacity
        supported = modifyForImageType(supported);

        //modify supported operation according to drm protection
        //operation = modifyForDRM(operation);

        return supported;
    }

    private int modifyForImageType(int supported) {
        //added for GIF animation
        if (MediatekFeature.isGifSupported() &&
                MediatekFeature.MIMETYPE_GIF.equalsIgnoreCase(mContentType)) {
            supported |= SUPPORT_GIF_ANIMATION;
        }
        //added for stereo display
    	// JPS stereo image has to be treated specially
        if (StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mContentType)) {
            if (mIsStereoDisplaySupported) {
                supported |= SUPPORT_STEREO_DISPLAY;
            }
        }
        // Whether a mpo file can be stereoly display depends on its
        // subtype. This will introduce IO operation and decoding, which
        // is very risky for ANR
        if (mIsMpoSupported &&
            MpoHelper.MPO_MIME_TYPE.equalsIgnoreCase(mContentType)) {
            initMpoSubType();
            if ((MpoDecoder.MTK_TYPE_Stereo == mMpoSubType ||
                 MpoDecoder.MTK_TYPE_3DPan == mMpoSubType)) {
                if (mIsStereoDisplaySupported) {
                    supported |= SUPPORT_STEREO_DISPLAY;
                }
            } else if(MediaObject.SUBTYPE_MPO_MAV == getSubType()) {
                supported |= SUPPORT_MAV_PLAYBACK;
            } else {
                supported |= SUPPORT_PLAY;
            }
        }
        
        //for normal image, support stereo display if possible
        //GIF animation is not supposed to be displayed as stereo
        if (mIsDisplay2dAs3dSupported &&
            0 == (supported & SUPPORT_PLAY) &&
            0 == (supported & SUPPORT_STEREO_DISPLAY) &&
            0 == (supported & SUPPORT_GIF_ANIMATION)) {
            supported |= SUPPORT_STEREO_DISPLAY;
            supported |= SUPPORT_CONVERT_TO_3D;
        }

        return supported;
    }
    
    private InputStream openUriInputStream(Uri uri) {
        if (null == uri) return null;
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme) || 
            ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme) || 
            ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                return mApplication.getContentResolver()
                            .openInputStream(uri);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "openUriInputStream:fail to open: " + uri, e);
                return null;
            }
        }
        Log.w(TAG,"openUriInputStream:encountered unknow scheme!");
        return null;
    }

    private void extractImageInfo(JobContext jc) {
        if (!MediatekFeature.preferDisplayOriginalSize()) return;

        Options options = new Options();
        options.inJustDecodeBounds = true;

        if (mIsDrmSupported && null != mUri && null != mUri.getPath() && 
            ContentResolver.SCHEME_FILE.equals(mUri.getScheme()) &&
            mUri.getPath().toLowerCase().endsWith(".dcf")) {
            //when drm file, decode it.
            //Note: currently, only DRM files on sdcard can be decoded
            ContentResolver resolver = mApplication.getContentResolver();
            Uri drmUri = Uri.parse("file:///" + mUri.getPath());
            DrmHelper.forceDecodeDrmUri(resolver, drmUri, null, false);
        } else {
            BitmapFactory.decodeFileDescriptor(
                mFileDescriptor.getFileDescriptor(), null, options);
        }
        if (0 != options.outWidth && 0 != options.outHeight) {
            mWidth = options.outWidth;
            mHeight = options.outHeight;
        }
        Log.d(TAG,"extractImageInfo:[" + mWidth + "x" + mHeight + "]");
        //for stereo feature, adjust dimension
        if (mIsStereoDisplaySupported && 
            StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mContentType)) {
            //we assume left and right stereo layout here.
            Log.w(TAG, "extractImageInfo:for JPS, assume left/right layout");
            mWidth = mWidth / 2;
        }
    }

    private FileDescriptor getFileDescriptor(JobContext jc) {
        //reset mState each time we want to prepare
        //Is is a very bad solution for image not updated
        //issue ?
//        mState = STATE_INIT;

        if (!prepareInputFile(jc)) return null;
        return mFileDescriptor.getFileDescriptor();
    }

    private static Bitmap decodeBitmapEx(JobContext jc, GalleryApp application,
                      Uri uri, String mimeType, int type,
                      BitmapFactory.Options options, int targetSize) {
        Params params = new Params();
        params.inOriginalFrame = true;
        params.inType = type;
        params.inOriginalTargetSize = targetSize;
        params.inPQEnhance = options.inPostProc;
        DataBundle dataBundle = RequestHelper.requestDataBundle(jc, params,
                                    (Context)application, uri, mimeType, false);
        Bitmap bitmap = dataBundle != null ? dataBundle.originalFrame : null;
        return DecodeUtils.ensureGLCompatibleBitmap(bitmap);
    }

    @Override
    public Job<MediatekFeature.DataBundle> requestImage(int type, Params params) {
        return new UriImageRequest(type, params);
    }

    public class UriImageRequest implements Job<MediatekFeature.DataBundle> {
        private int mType;
        private Params mParams;

        UriImageRequest(int type, Params params) {
            mType = type;
            mParams = params;
        }

        public MediatekFeature.DataBundle run(JobContext jc) {
            if (null == mUri || null == mParams) {
                Log.w(TAG,"UriImageRequest:got null mUri or mParams");
                return null;
            }
            if((getSubType() == MediaObject.SUBTYPE_MPO_MAV) && mMavListener != null) {
                return RequestHelper.requestDataBundle(jc, mParams, 
                        (Context)mApplication, mUri, mContentType, mMavListener);
            } else {
                return RequestHelper.requestDataBundle(jc, mParams, 
                        (Context)mApplication, mUri, mContentType);
            }
        }
    }

    //added for Stereo Display
    public int getStereoLayout() {
        if (MpoHelper.MPO_MIME_TYPE.equalsIgnoreCase(mContentType)) {
            return StereoHelper.STEREO_LAYOUT_FULL_FRAME;
        } else if (StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mContentType)) {
            //now we ignore the possibility that the image is top and bottom layout
            return StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
        } else {
            return StereoHelper.STEREO_LAYOUT_NONE;
        }
    }

    @Override
    public void setMavListener(MavListener listener) {
        this.mMavListener = listener;
    }
}
