package com.mediatek.encapsulation.com.google.android.mms;

import com.google.android.mms.ContentType;
import com.mediatek.encapsulation.EncapsulationConstant;
import java.util.ArrayList;

public class EncapsulatedContentType {

    public static final String MMS_MESSAGE       = ContentType.MMS_MESSAGE;
    // The phony content type for generic PDUs (e.g. ReadOrig.ind,
    // Notification.ind, Delivery.ind).
    public static final String MMS_GENERIC       = ContentType.MMS_GENERIC;
    public static final String MULTIPART_MIXED   = ContentType.MULTIPART_MIXED;
    public static final String MULTIPART_RELATED = ContentType.MULTIPART_RELATED;
    public static final String MULTIPART_ALTERNATIVE = ContentType.MULTIPART_ALTERNATIVE;

    public static final String TEXT_PLAIN        = ContentType.TEXT_PLAIN;
    public static final String TEXT_HTML         = ContentType.TEXT_HTML;
    public static final String TEXT_VCALENDAR    = ContentType.TEXT_VCALENDAR;
    public static final String TEXT_VCARD        = ContentType.TEXT_VCARD;

    public static final String IMAGE_UNSPECIFIED = ContentType.IMAGE_UNSPECIFIED;
    public static final String IMAGE_JPEG        = ContentType.IMAGE_JPEG;
    public static final String IMAGE_JPG         = ContentType.IMAGE_JPG;
    public static final String IMAGE_GIF         = ContentType.IMAGE_GIF;
    public static final String IMAGE_WBMP        = ContentType.IMAGE_WBMP;
    public static final String IMAGE_PNG         = ContentType.IMAGE_PNG;
    public static final String IMAGE_X_MS_BMP    = ContentType.IMAGE_X_MS_BMP;

    public static final String AUDIO_UNSPECIFIED = ContentType.AUDIO_UNSPECIFIED;
    public static final String AUDIO_AAC         = ContentType.AUDIO_AAC;
    public static final String AUDIO_AMR         = ContentType.AUDIO_AMR;
    public static final String AUDIO_IMELODY     = ContentType.AUDIO_IMELODY;
    public static final String AUDIO_MID         = ContentType.AUDIO_MID;
    public static final String AUDIO_MIDI        = ContentType.AUDIO_MIDI;
    public static final String AUDIO_MP3         = ContentType.AUDIO_MP3;
    public static final String AUDIO_MPEG3       = ContentType.AUDIO_MPEG3;
    public static final String AUDIO_MPEG        = ContentType.AUDIO_MPEG;
    public static final String AUDIO_MPG         = ContentType.AUDIO_MPG;
    public static final String AUDIO_MP4         = ContentType.AUDIO_MP4;
    public static final String AUDIO_X_MID       = ContentType.AUDIO_X_MID;
    public static final String AUDIO_X_MIDI      = ContentType.AUDIO_X_MIDI;
    public static final String AUDIO_X_MP3       = ContentType.AUDIO_X_MP3;
    public static final String AUDIO_X_MPEG3     = ContentType.AUDIO_X_MPEG3;
    public static final String AUDIO_X_MPEG      = ContentType.AUDIO_X_MPEG;
    public static final String AUDIO_X_MPG       = ContentType.AUDIO_X_MPG;
    public static final String AUDIO_3GPP        = ContentType.AUDIO_3GPP;
    public static final String AUDIO_X_WAV       = ContentType.AUDIO_X_WAV;
    public static final String AUDIO_OGG         = ContentType.AUDIO_OGG;

    public static final String VIDEO_UNSPECIFIED = ContentType.VIDEO_UNSPECIFIED;
    public static final String VIDEO_3GPP        = ContentType.VIDEO_3GPP;
    public static final String VIDEO_3G2         = ContentType.VIDEO_3G2;
    public static final String VIDEO_H263        = ContentType.VIDEO_H263;
    public static final String VIDEO_MP4         = ContentType.VIDEO_MP4;

    public static final String APP_SMIL          = ContentType.APP_SMIL;
    public static final String APP_WAP_XHTML     = ContentType.APP_WAP_XHTML;
    public static final String APP_XHTML         = ContentType.APP_XHTML;

    public static final String APP_DRM_CONTENT   = ContentType.APP_DRM_CONTENT;
    public static final String APP_DRM_MESSAGE   = ContentType.APP_DRM_MESSAGE;

    public static final String APP_OCET_STREAM   = "application/octet-stream";
    /** M: support some new ContentTypes */
    public static final String TEXT_TS           = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                       ContentType.TEXT_TS : "text/texmacs";

    public static final String IMAGE_BMP         = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.IMAGE_BMP : "image/x-ms-bmp";
    public static final String IMAGE_XBMP        = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.IMAGE_XBMP : "image/bmp";

    public static final String AUDIO_WAV         = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.AUDIO_WAV : "audio/x-wav";
    public static final String AUDIO_AWB         = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.AUDIO_AWB : "audio/amr-wb";
    public static final String AUDIO_WMA         = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.AUDIO_WMA : "audio/x-ms-wma";
    public static final String AUDIO_VORBIS      = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.AUDIO_VORBIS : "audio/vorbis";

    public static final String VIDEO_TS          = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        ContentType.VIDEO_TS : "video/mp2ts";

    private static final ArrayList<String> sSupportedContentTypes = new ArrayList<String>();
    private static final ArrayList<String> sSupportedImageTypes = new ArrayList<String>();
    private static final ArrayList<String> sSupportedAudioTypes = new ArrayList<String>();
    private static final ArrayList<String> sSupportedVideoTypes = new ArrayList<String>();
    /** M: support Restricted mode */
    private static final ArrayList<String> sRestrictedContentTypes = new ArrayList<String>();

    static {
        sSupportedContentTypes.add(TEXT_TS);
        sSupportedContentTypes.add(IMAGE_BMP);
        sSupportedContentTypes.add(IMAGE_XBMP);
        sSupportedContentTypes.add(AUDIO_WAV);
        sSupportedContentTypes.add(AUDIO_AWB);
        sSupportedContentTypes.add(AUDIO_WMA);
        sSupportedContentTypes.add(AUDIO_VORBIS);
        sSupportedContentTypes.add(VIDEO_TS);

        //restricted type
        sRestrictedContentTypes.add(TEXT_TS);
        sRestrictedContentTypes.add(AUDIO_VORBIS);
        sRestrictedContentTypes.add(VIDEO_TS);

        // add supported image types
        sSupportedImageTypes.add(IMAGE_BMP);
        sSupportedImageTypes.add(IMAGE_XBMP);

        // add supported audio types
        sSupportedAudioTypes.add(AUDIO_WAV);
        sSupportedAudioTypes.add(AUDIO_AWB);
        sSupportedAudioTypes.add(AUDIO_WMA);
        sSupportedAudioTypes.add(AUDIO_VORBIS);

        // add supported video types
        sSupportedVideoTypes.add(VIDEO_TS);
        sSupportedVideoTypes.add(TEXT_TS);
        //add for attachment enhance by feng
        //add supported ocet_stream type
        sSupportedContentTypes.add(APP_OCET_STREAM);
    }

    public static boolean isSupportedType(String contentType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.isSupportedType(contentType);
        } else {
            return (null != contentType) && (ContentType.isSupportedType(contentType)
                    || sSupportedContentTypes.contains(contentType));
        }
    }

    /** M: support Restricted mode */
    public static boolean isRestrictedType(String contentType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.isRestrictedType(contentType);
        } else {
            return (null != contentType) && (ContentType.isRestrictedType(contentType)
                    || sRestrictedContentTypes.contains(contentType));
        }
    }

    public static boolean isSupportedImageType(String contentType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.isSupportedImageType(contentType);
        } else {
            return (null != contentType) && (ContentType.isSupportedImageType(contentType)
                    || (isImageType(contentType) && isSupportedType(contentType)));
        }
    }

    public static boolean isSupportedAudioType(String contentType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.isSupportedAudioType(contentType);
        } else {
            return (null != contentType) && (ContentType.isSupportedAudioType(contentType)
                    || (isAudioType(contentType) && isSupportedType(contentType)));
        }
    }

    public static boolean isSupportedVideoType(String contentType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.isSupportedVideoType(contentType);
        } else {
            return (null != contentType) && (ContentType.isSupportedVideoType(contentType)
                    || (isVideoType(contentType) && isSupportedType(contentType)));
        }
    }

    public static boolean isTextType(String contentType) {
        return ContentType.isTextType(contentType);
    }

    public static boolean isImageType(String contentType) {
        return ContentType.isImageType(contentType);
    }

    public static boolean isAudioType(String contentType) {
        return ContentType.isAudioType(contentType);
    }

    public static boolean isVideoType(String contentType) {
        return ContentType.isVideoType(contentType);
    }

    public static boolean isDrmType(String contentType) {
        return ContentType.isDrmType(contentType);
    }

    public static boolean isUnspecified(String contentType) {
        return ContentType.isUnspecified(contentType);
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String> getImageTypes() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.getImageTypes();
        } else {
            ArrayList<String> imageTypes = (ArrayList<String>) sSupportedImageTypes.clone();
            imageTypes.addAll(ContentType.getImageTypes());
            return imageTypes;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String> getAudioTypes() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.getAudioTypes();
        } else {
            ArrayList<String> audioTypes = (ArrayList<String>) sSupportedAudioTypes.clone();
            audioTypes.addAll(ContentType.getAudioTypes());
            return audioTypes;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String> getVideoTypes() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.getVideoTypes();
        } else {
            ArrayList<String> videoTypes = (ArrayList<String>) sSupportedVideoTypes.clone();
            videoTypes.addAll(ContentType.getVideoTypes());
            return videoTypes;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String> getSupportedTypes() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return ContentType.getSupportedTypes();
        } else {
            ArrayList<String> supportedTypes = (ArrayList<String>) sSupportedContentTypes.clone();
            supportedTypes.addAll(ContentType.getSupportedTypes());
            return supportedTypes;
        }
    }
}
