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

package com.mediatek.gallery3d.data;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.media.MediaFile;
import android.net.Uri;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.mpo.MpoRequest;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class RequestHelper {
    private static final String TAG = "Gallery2/RequestHelper";

    public static DataBundle requestDataBundle(JobContext jc, Params params,
                      Context context, String filePath) {
        return requestDataBundle(jc, params, context, filePath, true);
    }

    public static DataBundle requestDataBundle(JobContext jc, Params params,
                      Context context, String filePath, boolean allowDefault) {
        if (null == context || null == filePath) {
            Log.w(TAG,"requestOriginalBitmap:got null parameters");
            return null;
        }

        String mimeType = null;
        IMediaRequest mediaRequest = null;
        //check if this file is drm and can get decrypted buffer
        byte[] buffer = DrmHelper.forceDecryptFile(filePath, false);

        if (null != buffer) {
            //for drm, we have to retrieve its mime type first.
            mimeType = DrmHelper.getOriginalMimeType(context, filePath);
Log.i(TAG, "requestOriginalBitmap:mimeType="+mimeType);
            mediaRequest = RequestManager.getMediaRequest(mimeType, true);
        } else {
            // mimeType = MediaFile.getMimeTypeBySuffix(filePath);
            mimeType = MediaFile.getMimeTypeForFile(filePath);
            mediaRequest = RequestManager.getMediaRequest(mimeType, allowDefault);
        }

        Log.d(TAG,"run:mediaRequest="+mediaRequest);
        if (null == mediaRequest) return null;

        if (null == buffer) {
            return mediaRequest.request(jc, params, filePath);
        } else {
            return mediaRequest.request(jc, params, buffer, 0, buffer.length);
        }
    }

    public static DataBundle requestDataBundle(JobContext jc, Params params,
            String filePath, String mimeType) {
        return requestDataBundle(jc, params, filePath, mimeType, true, null);
    }
    
    public static DataBundle requestDataBundle(JobContext jc, Params params,
            String filePath, String mimeType, MavListener listener) {
        return requestDataBundle(jc, params, filePath, mimeType, true, listener);
    }

    public static DataBundle requestDataBundle(JobContext jc, Params params,
            String filePath, String mimeType, boolean allowDefault, MavListener listener) {
        if (null == filePath || null == params || null == mimeType) {
            Log.w(TAG,"requestDataBundle:got null parameters");
            return null;
        }
        //check if this file is drm and can get decrypted buffer
        byte[] buffer = DrmHelper.forceDecryptFile(filePath, false);

        IMediaRequest mediaRequest = null;
        if (null == buffer) {
            mediaRequest = RequestManager.getMediaRequest(mimeType, allowDefault);
            if (null == mediaRequest) return null;
            if(mediaRequest instanceof MpoRequest && listener != null) {
                mediaRequest.setMavListener(listener);
            }
            return mediaRequest.request(jc, params, filePath);
        } else {
            mediaRequest = RequestManager.getMediaRequest(mimeType, true);
            if (null == mediaRequest) return null;

            return mediaRequest.request(jc, params, buffer, 0, buffer.length);
        }
    }

    public static DataBundle requestDataBundle(JobContext jc, Params params,
            Context context, Uri uri, String mimeType) {
        return requestDataBundle(jc, params, context, uri, mimeType, true, null);
    }
    
    public static DataBundle requestDataBundle(JobContext jc, Params params,
            Context context, Uri uri, String mimeType, MavListener listener) {
        return requestDataBundle(jc, params, context, uri, mimeType, true, listener);
    }
    
    public static DataBundle requestDataBundle(JobContext jc, Params params,
            Context context, Uri uri, String mimeType, boolean allowDefault) {
        return requestDataBundle(jc, params, context, uri, mimeType, allowDefault, null);
    }

    public static DataBundle requestDataBundle(JobContext jc, Params params,
            Context context, Uri uri, String mimeType, boolean allowDefault, MavListener listener) {
        if (null == uri || null == params || null == mimeType) {
            Log.w(TAG,"requestDataBundle:got null parameters");
            return null;
        }
        //temporaly, there is no need to decrypt uri to check if it is drm,
        //as we assume there is no drm in uri format
        //NOTE: there is indeed a chance that content://media/external or
        //file://mnt/sdcard or content://download...
        IMediaRequest mediaRequest = 
            RequestManager.getMediaRequest(mimeType, allowDefault);
        Log.d(TAG,"run:mediaRequest="+mediaRequest);
        if (null == mediaRequest) return null;
        if(mediaRequest instanceof MpoRequest && listener != null) {
            mediaRequest.setMavListener(listener);
        }
        return mediaRequest.request(jc, params, 
                                    context.getContentResolver(), uri);
    }

}
