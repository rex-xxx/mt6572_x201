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

package com.mediatek.gallery3d.mpo;

import java.io.FileDescriptor;
import java.io.InputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;

import com.mediatek.common.mpodecoder.IMpoDecoder;

public class MpoDecoderWrapper {
	
    private static final String TAG = "Gallery2/MpoDecoderWrapper";
    public static final int INVALID_VALUE = 0;

    private IMpoDecoder mMpoDecoder;

    private MpoDecoderWrapper(IMpoDecoder mpoDecoder) {
        mMpoDecoder = mpoDecoder;
    }

    public static MpoDecoderWrapper 
        createMpoDecoderWrapper(String filePath) {
        IMpoDecoder mpoDecoder = MpoHelper.createMpoDecoder(null, filePath);
        if (null == mpoDecoder) return null;
        return new MpoDecoderWrapper(mpoDecoder);
    }

    public static MpoDecoderWrapper 
        createMpoDecoderWrapper(ContentResolver cr, Uri uri) {
        IMpoDecoder mpoDecoder = MpoHelper.createMpoDecoder(null, cr, uri);
        if (null == mpoDecoder) return null;
        return new MpoDecoderWrapper(mpoDecoder);
    }

    public int width() {
        if (null == mMpoDecoder) return INVALID_VALUE;
        return mMpoDecoder.width();
    }

    public int height() {
        if (null == mMpoDecoder) return INVALID_VALUE;
        return mMpoDecoder.height();
    }

    public int frameCount() {
        if (null == mMpoDecoder) return INVALID_VALUE;
        return mMpoDecoder.frameCount();
    }

    public int getMtkMpoType() {
        if (null == mMpoDecoder) return INVALID_VALUE;
        return mMpoDecoder.getMtkMpoType();
    }

    public int suggestMtkMpoType() {
        if (null == mMpoDecoder) return INVALID_VALUE;
        return mMpoDecoder.suggestMtkMpoType();
    }

    public Bitmap frameBitmap(int frameIndex, Options options) {
        if (null == mMpoDecoder) return null;
        return mMpoDecoder.frameBitmap(frameIndex, options);
    }

    public void close() {
        if (null == mMpoDecoder) return;
        mMpoDecoder.close();
    }

}
