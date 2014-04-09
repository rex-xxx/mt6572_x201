/*
 * Copyright (C) 2009 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "StagefrightMediaScanner"
#include <utils/Log.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <media/stagefright/StagefrightMediaScanner.h>

#include <media/mediametadataretriever.h>
#include <private/media/VideoFrame.h>

// Sonivox includes
#include <libsonivox/eas.h>

// OMA DRM v1 implementation
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
#include <drm/DrmManagerClient.h>
#include <drm/DrmMetadata.h>
#include <drm/DrmMtkDef.h>
#include <drm/DrmMtkUtil.h>
#endif
#endif // ANDROID_DEFAULT_CODE

namespace android {

StagefrightMediaScanner::StagefrightMediaScanner() {
#ifndef ANDROID_DEFAULT_CODE
		ALOGI("StagefrightMediaScanner Constructor");
#endif
	}

StagefrightMediaScanner::~StagefrightMediaScanner() {
#ifndef ANDROID_DEFAULT_CODE
		ALOGI("StagefrightMediaScanner destructor");
#endif
}

static bool FileHasAcceptableExtension(const char *extension) {
    static const char *kValidExtensions[] = {
        ".mp3", ".mp4", ".m4a", ".3gp", ".3gpp", ".3g2", ".3gpp2",
        ".mpeg", ".ogg", ".mid", ".smf", ".imy", ".wma", ".aac",
        ".wav", ".amr", ".midi", ".xmf", ".rtttl", ".rtx", ".ota",
        ".mkv", ".mka", ".webm", ".ts", ".fl", ".flac", ".mxmf"
#ifdef ANDROID_DEFAULT_CODE
        ,".wma"
#endif
#ifndef ANDROID_DEFAULT_CODE
        ,".mpga"
#endif
#ifndef ANDROID_DEFAULT_CODE
        ,".m4v"
#endif
#ifndef ANDROID_DEFAULT_CODE
        ,".oga"
#endif
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_AUDIO_APE_SUPPORT
        , ".ape"
#endif
#endif
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
        , ".dcf"
#endif
#endif
#ifndef ANDROID_DEFAULT_CODE
        , ".awb",".mp2", ".mov", ".qt"
#ifdef MTK_ASF_PLAYBACK_SUPPORT
        , ".asf",".wmv",".wma"
#endif

#ifdef MTK_FLV_PLAYBACK_SUPPORT
        ,".flv",".f4v"
#endif//#ifdef MTK_FLV_PLAYBACK_SUPPORT

#ifdef MTK_OGM_PLAYBACK_SUPPORT
        ,".ogv",".ogm"
#endif//#ifdef MTK_OGM_PLAYBACK_SUPPORT
		,".m2ts"
#endif
       , ".avi", ".mpeg", ".mpg"
    };
    static const size_t kNumValidExtensions =
        sizeof(kValidExtensions) / sizeof(kValidExtensions[0]);

    for (size_t i = 0; i < kNumValidExtensions; ++i) {
        if (!strcasecmp(extension, kValidExtensions[i])) {
            return true;
        }
    }

    return false;
}

static MediaScanResult HandleMIDI(
        const char *filename, MediaScannerClient *client) {
    // get the library configuration and do sanity check
    const S_EAS_LIB_CONFIG* pLibConfig = EAS_Config();
    if ((pLibConfig == NULL) || (LIB_VERSION != pLibConfig->libVersion)) {
        ALOGE("EAS library/header mismatch\n");
        return MEDIA_SCAN_RESULT_ERROR;
    }
    EAS_I32 temp;

    // spin up a new EAS engine
    EAS_DATA_HANDLE easData = NULL;
    EAS_HANDLE easHandle = NULL;
    EAS_RESULT result = EAS_Init(&easData);
    if (result == EAS_SUCCESS) {
        EAS_FILE file;
        file.path = filename;
        file.fd = 0;
        file.offset = 0;
        file.length = 0;
        result = EAS_OpenFile(easData, &file, &easHandle);
    }
    if (result == EAS_SUCCESS) {
        result = EAS_Prepare(easData, easHandle);
    }
    if (result == EAS_SUCCESS) {
        result = EAS_ParseMetaData(easData, easHandle, &temp);
    }
    if (easHandle) {
        EAS_CloseFile(easData, easHandle);
    }
    if (easData) {
        EAS_Shutdown(easData);
    }

    if (result != EAS_SUCCESS) {
#ifndef ANDROID_DEFAULT_CODE
        client->setMimeType("bad mime type");
#endif // ANDROID_DEFAULT_CODE
        return MEDIA_SCAN_RESULT_SKIPPED;
    }

    char buffer[20];
    sprintf(buffer, "%ld", temp);
    status_t status = client->addStringTag("duration", buffer);
    if (status != OK) {
        return MEDIA_SCAN_RESULT_ERROR;
    }
#ifndef ANDROID_DEFAULT_CODE
    status_t status_res = client->addStringTag("isAccurateDuration", "1");
    if (status_res) {
    	return MEDIA_SCAN_RESULT_ERROR;
    }
#endif // ANDROID_DEFAULT_CODE

    return MEDIA_SCAN_RESULT_OK;
}

MediaScanResult StagefrightMediaScanner::processFile(
        const char *path, const char *mimeType,
        MediaScannerClient &client) {
#ifndef ANDROID_DEFAULT_CODE
    ALOGI("processFile '%s'.", path);
#else
    ALOGV("processFile '%s'.", path);
#endif // #ifndef ANDROID_DEFAULT_CODE

    client.setLocale(locale());
    client.beginFile();
    MediaScanResult result = processFileInternal(path, mimeType, client);
    client.endFile();
    return result;
}

MediaScanResult StagefrightMediaScanner::processFileInternal(
        const char *path, const char *mimeType,
        MediaScannerClient &client) {
    const char *extension = strrchr(path, '.');

    if (!extension) {
        return MEDIA_SCAN_RESULT_SKIPPED;
    }

    if (!FileHasAcceptableExtension(extension)) {
        return MEDIA_SCAN_RESULT_SKIPPED;
    }

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
    // add dcf meta data for dcf file
    // check extension first
    ALOGV("processFileInternal() : the extension: %s", extension);
    bool isOMADrmDcf = false;
    if (0 == strcasecmp(extension, ".dcf")) {
        String8 tmp(path);
        DrmManagerClient* drmManagerClient = new DrmManagerClient();
        DrmMetadata* dcfMetadata = drmManagerClient->getMetadata(&tmp);

        if (dcfMetadata == NULL) {
            ALOGW("scan: OMA DRM v1: failed to get drm metadata, not scanned into db.");
            delete drmManagerClient;
            client.setMimeType("bad mime type");
            return MEDIA_SCAN_RESULT_SKIPPED;
        }

        struct Map {
            const char* from;
            int to;
        };
        static const Map kMap[] = {
            {DrmMetaKey::META_KEY_IS_DRM,
                METADATA_KEY_IS_DRM}, // "is_drm"
            {DrmMetaKey::META_KEY_CONTENT_URI, 
                METADATA_KEY_DRM_CONTENT_URI},
            {DrmMetaKey::META_KEY_OFFSET,
                METADATA_KEY_DRM_OFFSET},
            {DrmMetaKey::META_KEY_DATALEN,
                METADATA_KEY_DRM_DATALEN},
            {DrmMetaKey::META_KEY_RIGHTS_ISSUER,
                METADATA_KEY_DRM_RIGHTS_ISSUER},
            {DrmMetaKey::META_KEY_CONTENT_NAME,
                METADATA_KEY_DRM_CONTENT_NAME},
            {DrmMetaKey::META_KEY_CONTENT_DESCRIPTION,
                METADATA_KEY_DRM_CONTENT_DES},
            {DrmMetaKey::META_KEY_CONTENT_VENDOR,
                METADATA_KEY_DRM_CONTENT_VENDOR},
            {DrmMetaKey::META_KEY_ICON_URI,
                METADATA_KEY_DRM_ICON_URI} ,
            {DrmMetaKey::META_KEY_METHOD,
                METADATA_KEY_DRM_METHOD},
            {DrmMetaKey::META_KEY_MIME,
                METADATA_KEY_DRM_MIME}
        };
        static const size_t kNumMapEntries = sizeof(kMap) / sizeof(kMap[0]);

        int action = Action::PLAY;
        String8 type;
        for (size_t i = 0; i < kNumMapEntries; ++i) {
            String8 value = dcfMetadata->get(String8(kMap[i].from));
            if (value.length() != 0)
            {
                if (kMap[i].to == METADATA_KEY_DRM_MIME) {
                    value = DrmMtkUtil::toCommonMime(value.string());
                    // not audio/video/image -> not scan into db
                    type.setTo(value.string(), 6);
                    if (0 != strcasecmp(type.string(), "audio/")
                        && 0 != strcasecmp(type.string(), "video/")
                        && 0 != strcasecmp(type.string(), "image/")) {
                        ALOGW("scan: OMA DRM v1: invalid drm media file mime type[%s], not added into db.",
                                value.string());
                        delete dcfMetadata;
                        delete drmManagerClient;
                        client.setMimeType("bad mime type");
                        return MEDIA_SCAN_RESULT_SKIPPED;
                    }

                    client.setMimeType(value.string());
                    ALOGD("scan: OMA DRM v1: drm original mime type[%s].",
                            value.string());

                    // determine the Action it shall used.
                    if ((0 == strcasecmp(type.string(), "audio/"))
                        || (0 == strcasecmp(type.string(), "video/"))) {
                        action = Action::PLAY;
                    } else if ((0 == strcasecmp(type.string(), "image/"))) {
                        action = Action::DISPLAY;
                    }
                }

                if (kMap[i].to == METADATA_KEY_IS_DRM) {
                    isOMADrmDcf = (value == String8("1"));
                }

                client.addStringTag(kMap[i].from, value.string());
                ALOGD("scan: OMA DRM v1: client.addString tag[%s] value[%s].",
                        kMap[i].from, value.string());
            }
        }

        // if there's no valid rights for this file currently, just return OK
        // to make sure it can be scanned into db.
        if (isOMADrmDcf
            && RightsStatus::RIGHTS_VALID != drmManagerClient->checkRightsStatus(tmp, action)) {
            ALOGD("scan: OMA DRM v1: current no valid rights, return OK so that it can be added into db.");
            delete dcfMetadata;
            delete drmManagerClient;
            return MEDIA_SCAN_RESULT_OK;
        }

        // when there's valid rights, should contine to add extra metadata
        ALOGD("scan: OMA DRM v1: current valid rights, continue to add extra info.");
        delete dcfMetadata; dcfMetadata = NULL;
        delete drmManagerClient; drmManagerClient = NULL;

        // if picture then we need not to scan with extractors.
        if (isOMADrmDcf && 0 == strcasecmp(type.string(), "image/")) {
            ALOGD("scan: OMA DRM v1: for DRM image we do not sniff with extractors.");
            return MEDIA_SCAN_RESULT_OK;
        }
    }
#endif
#endif // ANDROID_DEFAULT_CODE

    if (!strcasecmp(extension, ".mid")
            || !strcasecmp(extension, ".smf")
            || !strcasecmp(extension, ".imy")
            || !strcasecmp(extension, ".midi")
            || !strcasecmp(extension, ".xmf")
            || !strcasecmp(extension, ".rtttl")
            || !strcasecmp(extension, ".rtx")
            || !strcasecmp(extension, ".ota")
            || !strcasecmp(extension, ".mxmf")) {
        return HandleMIDI(path, &client);
    }

    sp<MediaMetadataRetriever> mRetriever(new MediaMetadataRetriever);

    int fd = open(path, O_RDONLY | O_LARGEFILE);
    status_t status;
    if (fd < 0) {
        // couldn't open it locally, maybe the media server can?
        status = mRetriever->setDataSource(path);
    } else {
        status = mRetriever->setDataSource(fd, 0, 0x7ffffffffffffffL);
        //why close fd so fast???--haizhen
        close(fd);
    }

    if (status) {
#ifndef ANDROID_DEFAULT_CODE
         //if this still need on ICS
        // set mime type to "bad mime type" for unsupported format, otherwise the file will be included in Gallery/Music
        ALOGE("processFile '%s' - not supported", path);
        client.setMimeType("bad mime type");
        return MEDIA_SCAN_RESULT_SKIPPED;
#else
        return MEDIA_SCAN_RESULT_ERROR;
#endif
    }

    const char *value;
    if ((value = mRetriever->extractMetadata(
                    METADATA_KEY_MIMETYPE)) != NULL) {
        status = client.setMimeType(value);
        if (status) {
            return MEDIA_SCAN_RESULT_ERROR;
        }
    }

    struct KeyMap {
        const char *tag;
        int key;
    };
    static const KeyMap kKeyMap[] = {
        { "tracknumber", METADATA_KEY_CD_TRACK_NUMBER },
        { "discnumber", METADATA_KEY_DISC_NUMBER },
        { "album", METADATA_KEY_ALBUM },
        { "artist", METADATA_KEY_ARTIST },
        { "albumartist", METADATA_KEY_ALBUMARTIST },
        { "composer", METADATA_KEY_COMPOSER },
        { "genre", METADATA_KEY_GENRE },
        { "title", METADATA_KEY_TITLE },
        { "year", METADATA_KEY_YEAR },
        { "duration", METADATA_KEY_DURATION },
        { "writer", METADATA_KEY_WRITER },
        { "compilation", METADATA_KEY_COMPILATION },
        { "isdrm", METADATA_KEY_IS_DRM },
        { "width", METADATA_KEY_VIDEO_WIDTH },
        { "height", METADATA_KEY_VIDEO_HEIGHT },
#ifdef MTK_S3D_SUPPORT
        {"stereotype",METADATA_KEY_STEREO_3D}, //stereo 3d info
#endif
    };
    static const size_t kNumEntries = sizeof(kKeyMap) / sizeof(kKeyMap[0]);

    for (size_t i = 0; i < kNumEntries; ++i) {
        const char *value;
        if ((value = mRetriever->extractMetadata(kKeyMap[i].key)) != NULL) {
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
            if (kKeyMap[i].key == METADATA_KEY_IS_DRM) {
                if (isOMADrmDcf) {
                    ALOGD("set METADATA_KEY_IS_DRM to 1 for OMA DRM v1.");
                    value = "1";
                }
            }
#endif
#endif // ANDROID_DEFAULT_CODE
            status = client.addStringTag(kKeyMap[i].tag, value);
            ALOGD("processFileInternal() : client.addString tag[%s] value[%s]",
                        kKeyMap[i].tag, value);
            if (status != OK) {
                return MEDIA_SCAN_RESULT_ERROR;
            }
#ifndef ANDROID_DEFAULT_CODE
            if (kKeyMap[i].key == METADATA_KEY_DURATION) {
                if (!strcasecmp(extension, ".mp3")
                        || !strcasecmp(extension, ".aac")
                        || !strcasecmp(extension, ".amr")
                        || !strcasecmp(extension, ".awb")) {
                    client.addStringTag("isAccurateDuration", "0");
                } else if (!strcasecmp(extension, ".wav")
                        || !strcasecmp(extension, ".ogg")
                        || !strcasecmp(extension, ".oga")) {
                    client.addStringTag("isAccurateDuration", "1");
                }
        }
#endif // ANDROID_DEFAULT_CODE
    }
    }
#ifndef ANDROID_DEFAULT_CODE
	ALOGI("processFileInternal '%s' - return OK", path);
#endif
    return MEDIA_SCAN_RESULT_OK;
}

char *StagefrightMediaScanner::extractAlbumArt(int fd) {
#ifndef ANDROID_DEFAULT_CODE
		ALOGI("extractAlbumArt %d", fd);
#else
    ALOGV("extractAlbumArt %d", fd);
#endif

    off64_t size = lseek64(fd, 0, SEEK_END);
    if (size < 0) {
        return NULL;
    }
    lseek64(fd, 0, SEEK_SET);

    sp<MediaMetadataRetriever> mRetriever(new MediaMetadataRetriever);
    if (mRetriever->setDataSource(fd, 0, size) == OK) {
        sp<IMemory> mem = mRetriever->extractAlbumArt();

        if (mem != NULL) {
            MediaAlbumArt *art = static_cast<MediaAlbumArt *>(mem->pointer());

            char *data = (char *)malloc(art->mSize + 4);
			if(data==NULL){
				ALOGE("MediaAlbumArt malloc failed! art->mSize=%u",art->mSize);
				return NULL;
			}
            *(int32_t *)data = art->mSize;
            memcpy(&data[4], &art[1], art->mSize);

            return data;
        }
    }

    return NULL;
}

}  // namespace android
