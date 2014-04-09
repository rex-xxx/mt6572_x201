/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.graphics;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import android.util.Log;

import android.graphics.Bitmap;

public class Movie {
    private final int mNativeMovie;
    
    private Movie(int nativeMovie) {
        if (nativeMovie == 0) {
            throw new RuntimeException("native movie creation failed");
        }
        mNativeMovie = nativeMovie;
    }

    public native int width();
    public native int height();
    public native boolean isOpaque();
    public native int duration();

    public native boolean setTime(int relativeMilliseconds);    

    public native void draw(Canvas canvas, float x, float y, Paint paint);
   
//for add gif begin
    //the following 4 methods are intented for no one but GifDecoder to use.
    //please see GifDecoder for information
    /**
     * This method get frame duration of specified GIF frame
     * @hide
     */
    public native int gifFrameDuration(int frameIndex);

    /**
     * This method get total frame count of GIF file
     * @hide
     */
    public native int gifTotalFrameCount();

    /**
     * This method get Bitmap of specified GIF frame
     * @hide
     */
    public native Bitmap gifFrameBitmap(int frameIndex);

    /**
     * This method release all the Info stored for GIF.
     * After this method is call, Movie Object should no longer be used.
     * eg. mMovie.closeGif();
     *     mMovie = null;
     * @hide
     */
    public native void closeGif();
//for add gif end

    public void draw(Canvas canvas, float x, float y) {
        draw(canvas, x, y, null);
    }

//modify original public method to make decode work properly.

    public static Movie decodeStream(InputStream is) {
        // we need mark/reset to work properly

        if (!is.markSupported()) {
            //the size of Buffer is aligned with BufferedInputStream
            // used in BitmapFactory of Android default version.
            is = new BufferedInputStream(is, 8*1024);
        }

        // so we can call reset() if a given codec gives up after reading up to
        // this many bytes. FIXME: need to find out from the codecs what this
        // value should be.
        is.mark(1024);
 
        return decodeMarkedStream(is);
    }

//    public static native Movie decodeStream(InputStream is);
    private static native Movie decodeMarkedStream(InputStream is);
//modify original public method to make decode work properly end.

    public static native Movie decodeByteArray(byte[] data, int offset,
                                               int length);

    private static native void nativeDestructor(int nativeMovie);

    public static Movie decodeFile(String pathName) {
        InputStream is;
        try {
            is = new FileInputStream(pathName);
        }
        catch (java.io.FileNotFoundException e) {
            return null;
        }
        return decodeTempStream(is);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            nativeDestructor(mNativeMovie);
        } finally {
            super.finalize();
        }
    }

    private static Movie decodeTempStream(InputStream is) {
        Movie moov = null;
        try {
            moov = decodeStream(is);
            is.close();
        }
        catch (java.io.IOException e) {
            /*  do nothing.
                If the exception happened on open, moov will be null.
                If it happened on close, moov is still valid.
            */
        }
        return moov;
    }
}
