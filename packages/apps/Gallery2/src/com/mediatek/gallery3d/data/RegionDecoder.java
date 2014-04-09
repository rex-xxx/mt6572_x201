package com.mediatek.gallery3d.data;

import android.graphics.BitmapRegionDecoder;
import android.util.Log;

public class RegionDecoder {
    private static final String TAG = "Gallery2/RegionDecoder";

    public RegionDecoder() {}

    public void release() {
        jpegBuffer = null;
        regionDecoder = null;
    }

    public void showInfo() {
        showInfo(TAG);
    }

    public int getWidth() {
        if (null != regionDecoder) {
            return regionDecoder.getWidth();
        } else {
            return 0;
        }
    }

    public int getHeight() {
        if (null != regionDecoder) {
            return regionDecoder.getHeight();
        } else {
            return 0;
        }
    }

    public void showInfo(String tag) {
        Log.i(tag, "showInfo:jpegBuffer=" + jpegBuffer);
        Log.d(tag, "showInfo:regionDecoder=" + regionDecoder);
        if (null != regionDecoder) {
            Log.v(tag, "showInfo:regionDecoder[" + regionDecoder.getWidth()
                       + "x" + regionDecoder.getHeight() + "]");
        }
    }

    public byte[] jpegBuffer;
    public BitmapRegionDecoder regionDecoder;
}


