package com.mediatek.videoorbplugin;

import android.content.Context;
import android.net.Uri;

import android.util.Log;
import com.mediatek.ngin3d.Video;

public class InternalMediaSource implements IMediaSource {
    private final static String TAG = "vo.internal";
    private static final String RESOURCE_URI =
            "android.resource://com.mediatek.videoorbplugin/";

    private static final int sDemos[] = new int[] {
            R.raw.gg_hyoyeon, R.raw.gg_jessica, R.raw.gg_seohyun, R.raw.gg_sunny,
            R.raw.gg_taeyeon, R.raw.gg_tiffany, R.raw.gg_yoona, R.raw.gg_yuri };

    public int getMediaCount() {
        Log.v(TAG, "media count : " + sDemos.length);
        return sDemos.length;
    }

    public Video getMedia(Context cts, int index, int width, int height) {
        if (index >= getMediaCount()) {
            return null;
        }
        Log.v(TAG, "getMedia : " + sDemos[index]);
        Video video = Video.createFromVideo(
                cts, Uri.parse(RESOURCE_URI + sDemos[index]), width, height);
        video.setDoubleSided(true);
        return video;
    }

    public Uri getMediaUri(Context ctx, int index) {
        if (index >= getMediaCount()) {
            return null;
        }
        return Uri.parse(RESOURCE_URI + sDemos[index]);
    }

    public void close() {
    }
}