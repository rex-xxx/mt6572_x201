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

import java.util.HashMap;
import java.util.LinkedHashMap;

import android.util.Log;

import com.mediatek.gallery3d.gif.GifHelper;
import com.mediatek.gallery3d.gif.GifRequest;
import com.mediatek.gallery3d.jps.JpsHelper;
import com.mediatek.gallery3d.jps.JpsRequest;
import com.mediatek.gallery3d.mpo.MpoHelper;
import com.mediatek.gallery3d.mpo.MpoRequest;

public class RequestManager {

    private static final String TAG = "Gallery2/RequestManager";

    private static HashMap<String, IMediaRequest> mRequestMap =
            new LinkedHashMap<String, IMediaRequest>();
    private static IMediaRequest mDefaultImageRequest = new ImageRequest();

    static {
        mRequestMap.put(MpoHelper.MIME_TYPE, new MpoRequest());
        mRequestMap.put(JpsHelper.MIME_TYPE, new JpsRequest());
        mRequestMap.put(GifHelper.MIME_TYPE, new GifRequest());
    }

    public static IMediaRequest getMediaRequest(String mimeType) {
        return getMediaRequest(mimeType, true);
    }

    public static IMediaRequest getMediaRequest(String mimeType,
                                    boolean allowDefault) {
        Log.v(TAG, "getMediaRequest(mimeType="+mimeType+")");
        if (null == mimeType) return null;

        IMediaRequest request = mRequestMap.get(mimeType);
        if (null != request) return request;

        //return a default request object in the future.
        if (allowDefault) return mDefaultImageRequest;

        return null;
    }
}
