package com.mediatek.videoorbplugin;

import android.content.ContentResolver;
import android.os.SystemProperties;
import android.util.Log;

public class MediaSourceFactory {
    private static final String INTERNAL_SOURCE_PROP = "vo.internalsource";
    private static final String TAG = "vo.factory";

    public static IMediaSource getInstance(ContentResolver cr) {
        boolean isInternal = SystemProperties.getBoolean(INTERNAL_SOURCE_PROP, false);
        Log.v(TAG, "Source configuration : " + INTERNAL_SOURCE_PROP + ", value : " + isInternal);
        return isInternal ? new InternalMediaSource() : new TranscodedMediaSource(cr);
    }

    public static IMediaSource getTranscodeSource(ContentResolver cr) {
        return new ExternalMediaSource(cr);
    }
}