package com.mediatek.gallery3d.mpo;

import android.content.ContentResolver;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.data.IMediaRequest;
import com.mediatek.gallery3d.data.ImageRequest;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

import com.mediatek.common.mpodecoder.IMpoDecoder;

public class MpoRequest extends ImageRequest {
    private static final String TAG = "Gallery2/MpoRequest";
    private MavListener mMavlistener;

    public MpoRequest() {}

    public DataBundle request(JobContext jc, Params params, String filePath) {
        Log.i(TAG, "request(jc,parmas,filePath="+filePath+")");
        if (null == params) return null;

        DataBundle dataBundle = null;
        MpoDecoderWrapper mpoDecoderWrapper =
            MpoDecoderWrapper.createMpoDecoderWrapper(filePath);
        try {
            // retrieve data bundel
            dataBundle = request(jc, params, mpoDecoderWrapper);
        } finally {
            // we must close mpo wrapper manually.
            if (null != mpoDecoderWrapper) {
                mpoDecoderWrapper.close();
            }
        }
        return dataBundle;
    }

    public DataBundle request(JobContext jc, Params params, byte[] data, 
                              int offset,int length) {
        Log.e(TAG, "request:no support for buffer!");
        return null;
    }

    public DataBundle request(JobContext jc, Params params,
                              ContentResolver cr, Uri uri) {
        Log.i(TAG, "request(jc, parmas, cr, uri="+uri+")");
        DataBundle dataBundle = null;
        MpoDecoderWrapper mpoDecoderWrapper =
            MpoDecoderWrapper.createMpoDecoderWrapper(cr, uri);
        try {
            // retrieve data bundel
            dataBundle = request(jc, params, mpoDecoderWrapper);
        } finally {
            // we must close mpo wrapper manually.
            if (null != mpoDecoderWrapper) {
                mpoDecoderWrapper.close();
            }
        }
        return dataBundle;
    }

    public DataBundle request(JobContext jc, Params params,
                              MpoDecoderWrapper mpoDecoderWrapper) {
        if (null == params || null == mpoDecoderWrapper) {
            Log.w(TAG, "request:got null params or decoder!");
            return null;
        }
        if (mpoDecoderWrapper.frameCount() < 1) {
            Log.w(TAG,"request:invalid frame count:"+
                      mpoDecoderWrapper.frameCount());
            return null;
        }
        if (null != jc && jc.isCancelled()) return null;

        params.info(TAG);

        // now, we should got a valid mpo decoder wrapper,
        // then try to get component
        DataBundle dataBundle = new DataBundle();

        if (params.inOriginalFrame || params.inFirstFrame ||
            params.inSecondFrame) {
            retrieveThumbData(jc, params, dataBundle, mpoDecoderWrapper);
        }

        if (params.inOriginalFullFrame || params.inFirstFullFrame ||
            params.inSecondFullFrame) {
            retrieveLargeData(jc, params, dataBundle, mpoDecoderWrapper);
        }

        if (params.inGifDecoder) {
            Log.w(TAG, "request: no GifDecoder can be generated from mpo");
        }

        if(params.inMpoTotalCount) {
            dataBundle.mpoTotalCount = mpoDecoderWrapper.frameCount();
        }

        if (params.inMpoFrames) {
            //decode all mpo frames, and store it in the data bundle
            retrieveMpoFrames(jc, params, dataBundle, mpoDecoderWrapper);
        }

        dataBundle.info(TAG);

        return dataBundle;
    }

    private void retrieveThumbData(JobContext jc, Params params, 
                     DataBundle dataBundle, MpoDecoderWrapper mpoDecoderWrapper) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveThumbData:job cancelled");
            return;
        }
        boolean isMav = false;
        if(mpoDecoderWrapper.getMtkMpoType() == IMpoDecoder.MTK_TYPE_MAV) {
            isMav = true;
            MtkLog.d(TAG, "retrieveThumbData, isMav: " + isMav);
        }
        
        int frameCount = mpoDecoderWrapper.frameCount();
        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSizeByType(
                    mpoDecoderWrapper.width(), mpoDecoderWrapper.height(),
                    params.inType, params.inOriginalTargetSize);
        options.inPostProc = params.inPQEnhance;

        if (params.inOriginalFrame) {
            int frameIndex = StereoHelper.getMpoFrameIndex(true, frameCount, isMav);
            dataBundle.originalFrame = DecodeHelper.decodeFrameSafe(jc, 
                                           mpoDecoderWrapper, frameIndex, options);
            dataBundle.originalFrame = DecodeHelper.postScaleDown(
                dataBundle.originalFrame, params.inType,
                params.inOriginalTargetSize);
        }

        if (params.inFirstFrame || params.inSecondFrame) {
            //for mpo, we only get second frame. 
            //first frame will never be retrieved.
            int frameIndex = StereoHelper.getMpoFrameIndex(false, frameCount, isMav);
            dataBundle.secondFrame = DecodeHelper.decodeFrameSafe(jc,
                                         mpoDecoderWrapper, frameIndex, options);
            dataBundle.secondFrame = DecodeHelper.postScaleDown(
                dataBundle.secondFrame, params.inType,
                params.inOriginalTargetSize);
        }
    }

    private void retrieveLargeData(JobContext jc, Params params, 
                     DataBundle dataBundle, MpoDecoderWrapper mpoDecoderWrapper) {
        //decode original full frame if needed
        if (params.inOriginalFullFrame) {
            int frameCount = mpoDecoderWrapper.frameCount();
            int frameIndex = StereoHelper.getMpoFrameIndex(true, frameCount, false);
            dataBundle.originalFullFrame = DecodeHelper.getRegionDecoder(jc,
                                             mpoDecoderWrapper, frameIndex);
        }
        //decode second full frame if needed
        if (params.inFirstFullFrame || params.inSecondFullFrame) {
            int frameCount = mpoDecoderWrapper.frameCount();
            int frameIndex = StereoHelper.getMpoFrameIndex(false, frameCount, false);
            dataBundle.secondFullFrame = DecodeHelper.getRegionDecoder(jc,
                                             mpoDecoderWrapper, frameIndex);
        }
    }
    
    private void retrieveMpoFrames(JobContext jc, Params params, 
            DataBundle dataBundle, MpoDecoderWrapper mpoDecoderWrapper) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveMpoFrames:job cancelled");
            return;
        }

        dataBundle.mpoFrames =
            DecodeHelper.decodeMpoFrames(jc, params, mpoDecoderWrapper, mMavlistener);
    }
    
    @Override
    public void setMavListener(MavListener listener) {
        this.mMavlistener = listener;
    }

}

