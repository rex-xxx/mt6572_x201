package com.mediatek.gallery3d.gif;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.data.IMediaRequest;
import com.mediatek.gallery3d.data.ImageRequest;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class GifRequest extends ImageRequest {
    private static final String TAG = "Gallery2/GifRequest";

    public GifRequest() {}

    public DataBundle request(JobContext jc, Params params, String filePath) {
        Log.d(TAG, "request(jc,parmas,filePath="+filePath+")");
        if (null == params || null == filePath) return null;

        boolean inGifDecoder = params.inGifDecoder;
        params.inGifDecoder = false;
        DataBundle dataBundle = super.request(jc, params, filePath);

        if (inGifDecoder) {
            if (null == dataBundle) dataBundle = new DataBundle();
            dataBundle.gifDecoder =
                GifDecoderWrapper.createGifDecoderWrapper(filePath);
        }

        return dataBundle;
    }

    public DataBundle request(JobContext jc, Params params, byte[] data, 
                              int offset,int length) {
        Log.d(TAG, "request(jc, params, data, ...)");
        if (null == params || null == data || length <= 0) return null;

        boolean inGifDecoder = params.inGifDecoder;
        params.inGifDecoder = false;
        DataBundle dataBundle =
            super.request(jc, params, data, offset, length);

        if (inGifDecoder) {
            if (null == dataBundle) dataBundle = new DataBundle();
            dataBundle.gifDecoder =
                GifDecoderWrapper.createGifDecoderWrapper(
                                      data, offset, length);
        }

        return dataBundle;
    }

    public DataBundle request(JobContext jc, Params params,
                              ContentResolver cr, Uri uri) {
        Log.d(TAG, "request(jc, parmas, cr, uri="+uri+")");
        if (null == params || null == cr || null == uri) return null;

        boolean inGifDecoder = params.inGifDecoder;
        params.inGifDecoder = false;
        DataBundle dataBundle =
            super.request(jc, params, cr, uri);

        if (inGifDecoder) {
            if (null == dataBundle) dataBundle = new DataBundle();
            dataBundle.gifDecoder =
                GifDecoderWrapper.createGifDecoderWrapper(
                    DecodeHelper.openUriInputStream(cr, uri));
        }

        return dataBundle;
    }

}

