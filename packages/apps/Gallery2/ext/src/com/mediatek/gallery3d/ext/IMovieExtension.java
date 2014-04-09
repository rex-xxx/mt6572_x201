package com.mediatek.gallery3d.ext;

import java.util.List;

/**
 * MoviePlayer extension finder class.
 */
public interface IMovieExtension {
    /**
     * Support stopping video in video playbck interface.
     * This is common feature for video, so plugin app doesn't need to add this hooker.
     * @Deprecated for common feature
     */
    int FEATURE_ENABLE_STOP = 1;
    /**
     * Support notifying final user in fullscreen mode.
     */
    int FEATURE_ENABLE_NOTIFICATION_PLUS = 2;
    /**
     * Support streaming setting, input URL and streaming video details displayed.
     */
    int FEATURE_ENABLE_STREAMING = 3;
    /**
     * Support bookmarking video URL and related operations: add, edit, delete.
     */
    int FEATURE_ENABLE_BOOKMARK = 4;
    /**
     * Support previous/next menu. Video list will be filled by starting activity's intent extra.
     * If starting activity doesn't set any extra, Gallery2's default order will be used.
     * Please see MovieListLoader for more details.
     */
    int FEATURE_ENABLE_VIDEO_LIST = 5;
    /**
     * Support stereo audio feature.
     */
    int FEATURE_ENABLE_STEREO_AUDIO = 6;
    /**
     * Support stereo audio feature.
     */
    int FEATURE_ENABLE_SETTINGS = 7;
    /**
     * Host app will get feature list from plugin app when being started.
     * Plugin app can return supported feature list to Host app.
     * Note: feature list order will define feature display order.
     * @return
     */
    List<Integer> getFeatureList();
    /**
     * Some small features swticher.
     * @return
     */
    IMovieStrategy getMovieStrategy();
    /**
     * @return return hooker extension which will be used by host app.
     */
    IActivityHooker getHooker();
}
