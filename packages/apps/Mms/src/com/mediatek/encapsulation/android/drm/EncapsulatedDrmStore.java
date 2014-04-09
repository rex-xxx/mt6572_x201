package com.mediatek.encapsulation.android.drm;

import com.mediatek.drm.OmaDrmStore;

public class EncapsulatedDrmStore extends OmaDrmStore {

    /**
     * Defines actions that can be performed on rights-protected content.
     */
    public static class EncapsulatedAction extends Action {

        // M: these 2 types are added for OMA DRM v1.0 implementation.
        /**
         * @hide
         */
        public static final int PRINT     = 0x08;
        /**
         * @hide
         */
        public static final int WALLPAPER = 0x09; // for FL only

        // M: add two case "PRINT" and "WALLPAPER"
        /* package */ static boolean isValid(int action) {
            boolean isValid = false;

            switch (action) {
                case DEFAULT:
                case PLAY:
                case RINGTONE:
                case TRANSFER:
                case OUTPUT:
                case PREVIEW:
                case EXECUTE:
                case DISPLAY:
                case PRINT:
                case WALLPAPER:
                    isValid = true;
            }
            return isValid;
        }

    }

    /**
     *  M: Defines status notifications for digital rights.
     */
    public static class EncapsulatedRightsStatus extends RightsStatus {
        // M:
        // this is added for OMA DRM v1.0 implementation
        /**
         * M: Constant field signifies that the secure timer is invalid
         * @hide
         */
        public static final int SECURE_TIMER_INVALID = 0x04;
    }

    // M: the following classes are added for OMA DRM v1.0 implementation.
    /**
     * Defines media mime type prefix
     * @hide
     */
    public static class MimePrefix {
        /**
         * M: Constant field signifies that image prefix
         */
        public static final String IMAGE = "image/";
        /**
         * M: Constant field signifies that audio prefix
         */
        public static final String AUDIO = "audio/";
        /**
         * M: Constant field signifies that video prefix
         */
        public static final String VIDEO = "video/";
    }

    /**
     * M: defines the string constants for retrieving metadata from dcf file.
     * @hide
     */
    public static class MetadataKey {
        public static final String META_KEY_IS_DRM = "is_drm";
        public static final String META_KEY_CONTENT_URI = "drm_content_uri";
        public static final String META_KEY_OFFSET = "drm_offset";
        public static final String META_KEY_DATALEN = "drm_dataLen";
        public static final String META_KEY_RIGHTS_ISSUER = "drm_rights_issuer";
        public static final String META_KEY_CONTENT_NAME = "drm_content_name";
        public static final String META_KEY_CONTENT_DESCRIPTION =
                "drm_content_description";
        public static final String META_KEY_CONTENT_VENDOR =
                "drm_content_vendor";
        public static final String META_KEY_ICON_URI = "drm_icon_uri";
        public static final String META_KEY_METHOD = "drm_method";
        public static final String META_KEY_MIME = "drm_mime_type";
    }

    /**
     * M: defines the string for Drm object mime type.
     * @hide
     */
    public static class DrmObjectMime {
        public static final String MIME_RIGHTS_XML =
                "application/vnd.oma.drm.rights+xml";
        public static final String MIME_RIGHTS_WBXML =
                "application/vnd.oma.drm.rights+wbxml";
        public static final String MIME_DRM_CONTENT =
                "application/vnd.oma.drm.content";
        public static final String MIME_DRM_MESSAGE =
                "application/vnd.oma.drm.message";
    }

    /**
     * M: defines the string for Drm Object file's suffix.
     * @hide
     */
    public static class DrmFileExt {
        public static final String EXT_RIGHTS_XML = ".dr";
        public static final String EXT_RIGHTS_WBXML = ".drc";
        public static final String EXT_DRM_CONTENT = ".dcf";
        public static final String EXT_DRM_MESSAGE = ".dm";
    }

    /**
     * M: defines the string for drm method.
     * Don't change without DrmDef.h
     * @hide
     */
    public static class DrmMethod {
        public static final int METHOD_NONE = 0;
        public static final int METHOD_FL = 1;
        public static final int METHOD_CD = 2;
        public static final int METHOD_SD = 4;
        public static final int METHOD_FLDCF = 8;
    }

    /**
     * M: defines the drm extra key & value.
     * @hide
     */
    public static class DrmExtra {
        public static final String EXTRA_DRM_LEVEL =
                "android.intent.extra.drm_level";
        public static final int DRM_LEVEL_FL = 1;
        public static final int DRM_LEVEL_SD = 2;
        public static final int DRM_LEVEL_ALL = 4;
    }

    /**
     * M: defines result code for NTP time synchronization
     * @hide
     */
    public static class NTPResult {
        public static final int NTP_SYNC_SUCCESS = 0;
        public static final int NTP_SYNC_NETWORK_TIMEOUT = -1;
        public static final int NTP_SYNC_SERVER_TIMEOUT = -2;
        public static final int NTP_SYNC_NETWORK_ERROR = -3;
    }

    /**
     * M: defines extra info type telling drm service to finish coresponding job.
     * will be checked in DrmInfoRequest.java using isValidType()
     * @hide
     */
    public static class DrmInfoType {
        public static final String KEY_DRM_UPDATE_CLOCK = "updateClock";
        public static final String KEY_DRM_CHECK_CLOCK = "checkClock";
        public static final String KEY_DRM_SAVE_DEVICE_ID = "saveDeviceId";

        public static final int TYPE_DRM_UPDATE_CLOCK = 2001;
        public static final int TYPE_DRM_UPDATE_TIME_BASE = 2002;
        public static final int TYPE_DRM_UPDATE_OFFSET = 2003;
        public static final int TYPE_DRM_LOAD_CLOCK = 2004;
        public static final int TYPE_DRM_SAVE_CLOCK = 2005;
        public static final int TYPE_DRM_CHECK_CLOCK = 2006;
        public static final int TYPE_DRM_LOAD_DEVICE_ID = 2007;
        public static final int TYPE_DRM_SAVE_DEVICE_ID = 2008;
    }

}
