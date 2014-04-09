package com.mediatek.gallery3d.jps;

import java.io.FileDescriptor;
import java.io.FileInputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.data.IMediaRequest;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class JpsRequest implements IMediaRequest {
    private static final String TAG = "Gallery2/JpsRequest";

    public JpsRequest() {
    }

    public DataBundle request(JobContext jc, Params params, String filePath) {
        Log.i(TAG, "request(jc, parmas, filePath="+filePath+")");
        if (null == params || null == filePath) {
            Log.e(TAG,"request:invalid parameters");
            return null;
        }

        FileInputStream fis = null;
        FileDescriptor fd = null;
        try {
            fis = new FileInputStream(filePath);
            fd = fis.getFD();
            BitmapRegionDecoder regionDecoder =
                DecodeUtils.createBitmapRegionDecoder(null, fd, false);
            //assume that the jps is left-right layout
            int layout = StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
            return request(jc, layout, params, regionDecoder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Utils.closeSilently(fis);
        }
    }

    public DataBundle request(JobContext jc, Params params, byte[] data, 
                              int offset,int length) {
        Log.e(TAG, "request:no support for buffer!");
        return null;
    }

    public DataBundle request(JobContext jc, Params params,
                              ContentResolver cr, Uri uri) {
        Log.i(TAG, "request(jc, parmas, cr, uri="+uri+")");
        if (null == params || null == cr || null == uri) {
            Log.e(TAG,"request:invalid parameters");
            return null;
        }

        ParcelFileDescriptor pfd = null;
        FileDescriptor fd = null;
        try {
            pfd = cr.openFileDescriptor(uri, "r");
            fd = pfd.getFileDescriptor();
            BitmapRegionDecoder regionDecoder =
                DecodeUtils.createBitmapRegionDecoder(null, fd, false);
            //assume that the jps is left-right layout
            int layout = StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
            return request(jc, layout, params, regionDecoder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Utils.closeSilently(pfd);
        }
    }

    public DataBundle request(JobContext jc, int layout, Params params,
                              BitmapRegionDecoder regionDecoder) {
        if (null == params || null == regionDecoder) {
            Log.w(TAG, "request:got null params or decoder!");
            return null;
        }
        if (null != jc && jc.isCancelled()) return null;

        params.info(TAG);

        DataBundle dataBundle = new DataBundle();

        if (params.inOriginalFrame || params.inFirstFrame ||
            params.inSecondFrame) {
            retrieveThumbData(jc, layout, params, dataBundle, regionDecoder);
        }

        if (params.inOriginalFullFrame || params.inFirstFullFrame ||
            params.inSecondFullFrame) {
            retrieveLargeData(jc, layout, params, dataBundle, regionDecoder);
        }

        if (params.inGifDecoder) {
            Log.w(TAG, "request: no GifDecoder can be generated from jps");
        }

        dataBundle.info(TAG);

        return dataBundle;
    }

    private void retrieveThumbData(JobContext jc, int layout, Params params,
                     DataBundle dataBundle, BitmapRegionDecoder regionDecoder) {
        if (null == params || null == regionDecoder) {
            Log.e(TAG,"retrieveThumbData:invalid parameters");
            return;
        }

        if (null != jc && jc.isCancelled()) return;

        Rect imageRect = new Rect(0, 0, regionDecoder.getWidth(), 
                                        regionDecoder.getHeight());
        JpsHelper.adjustRect(layout, true, imageRect);

        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSizeByType(
                                   imageRect.right - imageRect.left, 
                                   imageRect.bottom - imageRect.top, 
                                   params.inType, params.inOriginalTargetSize);
        options.inPostProc = params.inPQEnhance;

        if (params.inOriginalFrame) {
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, true, imageRect);
            dataBundle.originalFrame = DecodeHelper.safeDecodeImageRegion(
                                           jc, regionDecoder, imageRect, options);
            dataBundle.originalFrame = DecodeHelper.postScaleDown(
                dataBundle.originalFrame, params.inType,
                params.inOriginalTargetSize);
        }

        if (params.inFirstFrame || params.inSecondFrame) {
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, false, imageRect);
            dataBundle.secondFrame = DecodeHelper.safeDecodeImageRegion(
                                           jc, regionDecoder, imageRect, options);
            dataBundle.secondFrame = DecodeHelper.postScaleDown(
                dataBundle.secondFrame, params.inType,
                params.inOriginalTargetSize);
        }
    }

    private void retrieveLargeData(JobContext jc, int layout, Params params,
                     DataBundle dataBundle, BitmapRegionDecoder regionDecoder) {
        if (null == params || null == regionDecoder) {
            Log.e(TAG,"retrieveLargeData:invalid parameters");
            return;
        }

        if (null != jc && jc.isCancelled()) return;

        Rect imageRect = new Rect(0, 0, regionDecoder.getWidth(), 
                                        regionDecoder.getHeight());
        JpsHelper.adjustRect(layout, true, imageRect);

        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSize(
                                   DecodeHelper.MAX_BITMAP_BYTE_COUNT, -1,
                                   imageRect.right - imageRect.left,
                                   imageRect.bottom - imageRect.top);
        //as we decode buffer for region decoder, we close PQ enhance option
        //to prevent double enhancement.

        //decode original full frame if needed
        if (params.inOriginalFullFrame) {
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, true, imageRect);
            Bitmap bitmap = DecodeHelper.safeDecodeImageRegion(
                                           jc, regionDecoder, imageRect, options);
            if (null != bitmap) {
                if (null != jc && jc.isCancelled()) {
                    bitmap.recycle();
                    bitmap = null;
                } else {
                    dataBundle.originalFullFrame =
                        DecodeHelper.getRegionDecoder(jc, bitmap, true);
                }
            }
        }

        //decode second full frame if needed
        if (params.inFirstFullFrame || params.inSecondFullFrame) {
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, false, imageRect);
            Bitmap bitmap = DecodeHelper.safeDecodeImageRegion(
                                           jc, regionDecoder, imageRect, options);
            if (null != bitmap) {
                if (null != jc && jc.isCancelled()) {
                    bitmap.recycle();
                    bitmap = null;
                } else {
                    dataBundle.secondFullFrame =
                        DecodeHelper.getRegionDecoder(jc, bitmap, true);
                }
            }
        }
    }

    public void setMavListener(MavListener listener) {
        // TODO Auto-generated method stub
    }
}

