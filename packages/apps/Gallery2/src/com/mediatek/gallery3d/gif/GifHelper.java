/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.mediatek.gallery3d.gif;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.util.Log;

import com.mediatek.gifdecoder.GifDecoder;

import com.android.gallery3d.util.ThreadPool.JobContext;

public class GifHelper {
	
    private static final String TAG = "Gallery2/GifHelper";

    public static final String FILE_EXTENSION = "gif";

    public static final String MIME_TYPE = "image/gif";


    public static GifDecoder createGifDecoder(JobContext jc, String filePath) {
        try {
            InputStream is = new FileInputStream(filePath);
            GifDecoder gifDecoder = createGifDecoderInner(is);
            is.close();
            return gifDecoder;
        } catch (Throwable t) {
            Log.w(TAG, t);
            return null;
        }
    }

    public static GifDecoder createGifDecoder(JobContext jc, byte[] data, 
                              int offset,int length) {
        if (null == data) {
            Log.e(TAG,"createGifDecoder:find null buffer!");
            return null;
        }
        GifDecoder gifDecoder = new GifDecoder(data, offset, length);
        if (gifDecoder.getTotalFrameCount() == GifDecoder.INVALID_VALUE) {
            Log.e(TAG,"createGifDecoder:got invalid GifDecoder");
            gifDecoder = null;
        }
        return gifDecoder;
    }

    public static GifDecoder createGifDecoder(JobContext jc, InputStream is) {
        try {
            return createGifDecoderInner(is);
        } catch (Throwable t)  {
            Log.w(TAG, t);
            return null;
        }
    }

    public static GifDecoder createGifDecoder(JobContext jc, FileDescriptor fd) {
        try {
            InputStream is = new FileInputStream(fd);
            GifDecoder gifDecoder = createGifDecoderInner(is);
            is.close();
            return gifDecoder;
        } catch (Throwable t)  {
            Log.w(TAG, t);
            return null;
        }
    }
    
    private static GifDecoder createGifDecoderInner(InputStream is) {
        if (null == is) {
            Log.e(TAG,"createGifDecoder:find null InputStream!");
            return null;
        }
        GifDecoder gifDecoder = new GifDecoder(is);
        if (gifDecoder.getTotalFrameCount() == GifDecoder.INVALID_VALUE) {
            Log.e(TAG,"createGifDecoder:got invalid GifDecoder");
            gifDecoder = null;
        }
        return gifDecoder;
    }

}
