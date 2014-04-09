package com.mediatek.gallery3d.videothumbnail;

import com.mediatek.common.featureoption.FeatureOption;

public class VideoThumbnailFeatureOption {
    // constant section start
    // --------------------------------------------------------------------
    static final int OPTION_RENDER_TYPE_REGARDLESS_OF_ASPECT_RATIO = 0;
    static final int OPTION_RENDER_TYPE_KEEP_ASPECT_RATIO = 1;
    static final int OPTION_RENDER_TYPE_CROP_CENTER = 2;
    // --------------------------------------------------------------------
    // constant section end

    // configuration section start
    // --------------------------------------------------------------------
    // whether we enable video thumbnail play feature
    public static final boolean OPTION_ENABLE_THIS_FEATRUE
        = FeatureOption.MTK_VIDEO_THUMBNAIL_PLAY_SUPPORT; // true;
    // how to render video frames in a slot
    // value should be one of OPTION_RENDER_TYPE_* in constant section above
    static final int OPTION_RENDER_TYPE = OPTION_RENDER_TYPE_CROP_CENTER;
    // DO NOT enable this feature at present
    static final boolean OPTION_MONITOR_LOADING = false;      // please DO NOT modify
    // for test purpose (test playing without transcoding), set the below to be false
    static final boolean OPTION_TRANSCODE_BEFORE_PLAY = true; // only for debugging
    static final boolean OPTION_PREPARE_ASYNC = false;        // useless at present
    // --------------------------------------------------------------------
    // configuration section end
}
