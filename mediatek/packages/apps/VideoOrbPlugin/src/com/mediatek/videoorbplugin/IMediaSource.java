package com.mediatek.videoorbplugin;

import android.content.Context;
import android.net.Uri;

import com.mediatek.ngin3d.Video;

public interface IMediaSource {
    int getMediaCount();
    Video getMedia(Context ctx, int index, int width, int height);
    Uri getMediaUri(Context ctx, int index);
    void close();
}