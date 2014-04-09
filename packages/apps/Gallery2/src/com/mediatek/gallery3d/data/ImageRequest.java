package com.mediatek.gallery3d.data;

import java.io.FileDescriptor;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class ImageRequest implements IMediaRequest {
    private static final String TAG = "Gallery2/ImageRequest";

    public DataBundle request(JobContext jc, Params params, String filePath) {
        if (null == params || null == filePath) {
            Log.w(TAG, "request:got null params or filePath!");
            return null;
        }
        if (null != jc && jc.isCancelled()) return null;

        params.info(TAG);

        DataBundle dataBundle = new DataBundle();
        Options options = new Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap originThumb = null;
        Bitmap largeFrame = null;

        if (params.inOriginalFrame || params.inFirstFrame ||
            params.inSecondFrame) {
            // M: for picture quality enhancement
            MediatekFeature.enablePictureQualityEnhance(
                                options, params.inPQEnhance);

            originThumb = DecodeUtils.decodeThumbnail(
                jc, filePath, options, 
                params.inOriginalTargetSize, params.inType);
        }

        if (params.inOriginalFullFrame) {
            BitmapRegionDecoder bitmapRegionDecoder =
                DecodeUtils.createBitmapRegionDecoder(jc, filePath, false);
            if (null != bitmapRegionDecoder) {
                RegionDecoder regionDecoder = new RegionDecoder();
                regionDecoder.regionDecoder = bitmapRegionDecoder;
                dataBundle.originalFullFrame = regionDecoder;
            }
        }

        if (params.inFirstFullFrame || params.inSecondFullFrame) {
            largeFrame = DecodeHelper.decodeLargeBitmap(jc, null, filePath);
        }

        request(jc, params, dataBundle, originThumb, largeFrame);

        dataBundle.info(TAG);

        return dataBundle;
    }

    public DataBundle request(JobContext jc, Params params, byte[] data, 
                              int offset,int length) {
        if (null == params || null == data) {
            Log.w(TAG, "request:got null params or data!");
            return null;
        }
        if (null != jc && jc.isCancelled()) return null;

        params.info(TAG);

        DataBundle dataBundle = new DataBundle();
        Options options = new Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap originThumb = null;
        Bitmap largeFrame = null;

        if (params.inOriginalFrame || params.inFirstFrame ||
            params.inSecondFrame) {
            // M: for picture quality enhancement
            MediatekFeature.enablePictureQualityEnhance(
                                options, params.inPQEnhance);

            originThumb = DecodeHelper.decodeThumbnail(jc, data, 
                             offset, length, options,
                             params.inOriginalTargetSize, params.inType);
        }

        if (params.inOriginalFullFrame) {
            BitmapRegionDecoder bitmapRegionDecoder =
                DecodeUtils.createBitmapRegionDecoder(
                                jc, data, offset, length, false);
            if (null != bitmapRegionDecoder) {
                RegionDecoder regionDecoder = new RegionDecoder();
                regionDecoder.regionDecoder = bitmapRegionDecoder;
                dataBundle.originalFullFrame = regionDecoder;
            }
        }

        if (params.inFirstFullFrame || params.inSecondFullFrame) {
            largeFrame = 
                DecodeHelper.decodeLargeBitmap(
                                 jc, null, data, offset, length);
        }

        request(jc, params, dataBundle, originThumb, largeFrame);

        dataBundle.info(TAG);

        return dataBundle;
    }

    public DataBundle request(JobContext jc, Params params,
                              ContentResolver cr, Uri uri) {
        if (null == params || null == cr || null == uri) {
            Log.w(TAG, "request:got null params or cr or uri!");
            return null;
        }
        if (null != jc && jc.isCancelled()) return null;

        params.info(TAG);

        DataBundle dataBundle = new DataBundle();
        Options options = new Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap originThumb = null;
        Bitmap largeFrame = null;

        ParcelFileDescriptor pfd = null;
        FileDescriptor fd = null;
        try {
            pfd = cr.openFileDescriptor(uri, "r");
            fd = pfd.getFileDescriptor();

            if (params.inOriginalFrame || params.inFirstFrame ||
                params.inSecondFrame) {
                // M: for picture quality enhancement
                MediatekFeature.enablePictureQualityEnhance(
                                    options, params.inPQEnhance);
    
                originThumb = DecodeUtils.decodeThumbnail(jc, fd, options,
                        params.inOriginalTargetSize, params.inType);
            }

            if (params.inOriginalFullFrame) {
                BitmapRegionDecoder bitmapRegionDecoder =
                    DecodeUtils.createBitmapRegionDecoder(null, fd, false);
                if (null != bitmapRegionDecoder) {
                    RegionDecoder regionDecoder = new RegionDecoder();
                    regionDecoder.regionDecoder = bitmapRegionDecoder;
                    dataBundle.originalFullFrame = regionDecoder;
                }
            }

            if (params.inFirstFullFrame || params.inSecondFullFrame) {
                largeFrame = 
                    DecodeHelper.decodeLargeBitmap(jc, null, fd);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Utils.closeSilently(pfd);
        }


        request(jc, params, dataBundle, originThumb, largeFrame);

        dataBundle.info(TAG);

        return dataBundle;
    }


    private void request(JobContext jc, Params params,DataBundle dataBundle, 
                         Bitmap originThumb, Bitmap largeFrame) {

        if (params.inOriginalFrame || params.inFirstFrame ||
            params.inSecondFrame) {
            //first, we resize down the original bitmap
            if (null != originThumb) {
                originThumb = DecodeHelper.postScaleDown(
                    originThumb, params.inType, 960);
            }

            //check if we should retrieve original thumbnail
            if (params.inOriginalFrame) {
                dataBundle.originalFrame = originThumb;
            }
            //check if we need to generate stereo pair
            if (params.inFirstFrame || params.inSecondFrame) {
                if (null != originThumb) {
                    //generate the second image
                    Params special = new Params();
                    special.inFirstFrame = params.inFirstFrame;
                    special.inSecondFrame = params.inSecondFrame;
                    DataBundle temp = 
                        StereoHelper.generateSecondImage(jc, originThumb,
                                         special, !params.inOriginalFrame);
                    if (null != temp) {
                        dataBundle.firstFrame = temp.firstFrame;
                        dataBundle.secondFrame = temp.secondFrame;
                    }
                }
            }
            if (null != dataBundle.originalFrame) {
                dataBundle.originalFrame = DecodeHelper.postScaleDown(
                    dataBundle.originalFrame, params.inType,
                    params.inOriginalTargetSize);
            }
            if (null != dataBundle.firstFrame) {
                dataBundle.firstFrame = DecodeHelper.postScaleDown(
                    dataBundle.firstFrame, params.inType,
                    params.inOriginalTargetSize);
            }
            if (null != dataBundle.secondFrame) {
                dataBundle.secondFrame = DecodeHelper.postScaleDown(
                    dataBundle.secondFrame, params.inType,
                    params.inOriginalTargetSize);
            }
        }

        if (params.inFirstFullFrame || params.inSecondFullFrame) {
            if (null != largeFrame) {
                if (null != jc && jc.isCancelled()) {
                    largeFrame.recycle();
                    return;
                }
                Params special = new Params();
                special.inFirstFullFrame = params.inFirstFullFrame;
                special.inSecondFullFrame = params.inSecondFullFrame;
                DataBundle temp = 
                    StereoHelper.generateSecondImage(
                                     jc, largeFrame, special, true);
                if (null != temp) {
                    dataBundle.firstFullFrame = temp.firstFullFrame;
                    dataBundle.secondFullFrame = temp.secondFullFrame;
                }
            }
        }
    }

    public void setMavListener(MavListener listener) {}
}
