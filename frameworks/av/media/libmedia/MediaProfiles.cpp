/*
**
** Copyright 2010, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


//#define LOG_NDEBUG 0
#define LOG_TAG "MediaProfiles"

#include <stdlib.h>
#include <utils/Log.h>
#include <utils/Vector.h>
#include <cutils/properties.h>
#include <libexpat/expat.h>
#include <media/MediaProfiles.h>
#include <media/stagefright/foundation/ADebug.h>
#include <OMX_Video.h>

#include <sys/sysconf.h>
#include <asm/page.h>

#if !defined(ANDROID_DEFAULT_PROFILE)
#include "venc_drv_if.h"
#include "val_types.h"
#endif

namespace android {

Mutex MediaProfiles::sLock;
bool MediaProfiles::sIsInitialized = false;
MediaProfiles *MediaProfiles::sInstance = NULL;

const MediaProfiles::NameToTagMap MediaProfiles::sVideoEncoderNameMap[] = {
    {"h263", VIDEO_ENCODER_H263},
    {"h264", VIDEO_ENCODER_H264},
    {"m4v",  VIDEO_ENCODER_MPEG_4_SP}
};

const MediaProfiles::NameToTagMap MediaProfiles::sAudioEncoderNameMap[] = {
    {"amrnb",  AUDIO_ENCODER_AMR_NB},
    {"amrwb",  AUDIO_ENCODER_AMR_WB},
    {"aac",    AUDIO_ENCODER_AAC},
    {"heaac",  AUDIO_ENCODER_HE_AAC},
    {"aaceld", AUDIO_ENCODER_AAC_ELD}
};

const MediaProfiles::NameToTagMap MediaProfiles::sFileFormatMap[] = {
    {"3gp", OUTPUT_FORMAT_THREE_GPP},
    {"mp4", OUTPUT_FORMAT_MPEG_4}
};

const MediaProfiles::NameToTagMap MediaProfiles::sVideoDecoderNameMap[] = {
    {"wmv", VIDEO_DECODER_WMV}
};

const MediaProfiles::NameToTagMap MediaProfiles::sAudioDecoderNameMap[] = {
    {"wma", AUDIO_DECODER_WMA}
};

const MediaProfiles::NameToTagMap MediaProfiles::sCamcorderQualityNameMap[] = {
    {"low", CAMCORDER_QUALITY_LOW},
    {"high", CAMCORDER_QUALITY_HIGH},
    {"qcif", CAMCORDER_QUALITY_QCIF},
    {"cif", CAMCORDER_QUALITY_CIF},
    {"480p", CAMCORDER_QUALITY_480P},
    {"720p", CAMCORDER_QUALITY_720P},
    {"1080p", CAMCORDER_QUALITY_1080P},
    {"qvga", CAMCORDER_QUALITY_QVGA},

    {"timelapselow",  CAMCORDER_QUALITY_TIME_LAPSE_LOW},
    {"timelapsehigh", CAMCORDER_QUALITY_TIME_LAPSE_HIGH},
    {"timelapseqcif", CAMCORDER_QUALITY_TIME_LAPSE_QCIF},
    {"timelapsecif", CAMCORDER_QUALITY_TIME_LAPSE_CIF},
    {"timelapse480p", CAMCORDER_QUALITY_TIME_LAPSE_480P},
    {"timelapse720p", CAMCORDER_QUALITY_TIME_LAPSE_720P},
    {"timelapse1080p", CAMCORDER_QUALITY_TIME_LAPSE_1080P},
    {"timelapseqvga", CAMCORDER_QUALITY_TIME_LAPSE_QVGA},
};

/*static*/ void
MediaProfiles::logVideoCodec(const MediaProfiles::VideoCodec& codec)
{
    ALOGV("video codec:");
    ALOGV("codec = %d", codec.mCodec);
    ALOGV("bit rate: %d", codec.mBitRate);
    ALOGV("frame width: %d", codec.mFrameWidth);
    ALOGV("frame height: %d", codec.mFrameHeight);
    ALOGV("frame rate: %d", codec.mFrameRate);
}

/*static*/ void
MediaProfiles::logAudioCodec(const MediaProfiles::AudioCodec& codec)
{
    ALOGV("audio codec:");
    ALOGV("codec = %d", codec.mCodec);
    ALOGV("bit rate: %d", codec.mBitRate);
    ALOGV("sample rate: %d", codec.mSampleRate);
    ALOGV("number of channels: %d", codec.mChannels);
}

/*static*/ void
MediaProfiles::logVideoEncoderCap(const MediaProfiles::VideoEncoderCap& cap)
{
    ALOGV("video encoder cap:");
    ALOGV("codec = %d", cap.mCodec);
    ALOGV("bit rate: min = %d and max = %d", cap.mMinBitRate, cap.mMaxBitRate);
    ALOGV("frame width: min = %d and max = %d", cap.mMinFrameWidth, cap.mMaxFrameWidth);
    ALOGV("frame height: min = %d and max = %d", cap.mMinFrameHeight, cap.mMaxFrameHeight);
    ALOGV("frame rate: min = %d and max = %d", cap.mMinFrameRate, cap.mMaxFrameRate);
}

/*static*/ void
MediaProfiles::logAudioEncoderCap(const MediaProfiles::AudioEncoderCap& cap)
{
    ALOGV("audio encoder cap:");
    ALOGV("codec = %d", cap.mCodec);
    ALOGV("bit rate: min = %d and max = %d", cap.mMinBitRate, cap.mMaxBitRate);
    ALOGV("sample rate: min = %d and max = %d", cap.mMinSampleRate, cap.mMaxSampleRate);
    ALOGV("number of channels: min = %d and max = %d", cap.mMinChannels, cap.mMaxChannels);
}

/*static*/ void
MediaProfiles::logVideoDecoderCap(const MediaProfiles::VideoDecoderCap& cap)
{
    ALOGV("video decoder cap:");
    ALOGV("codec = %d", cap.mCodec);
}

/*static*/ void
MediaProfiles::logAudioDecoderCap(const MediaProfiles::AudioDecoderCap& cap)
{
    ALOGV("audio codec cap:");
    ALOGV("codec = %d", cap.mCodec);
}

/*static*/ void
MediaProfiles::logVideoEditorCap(const MediaProfiles::VideoEditorCap& cap)
{
    ALOGV("videoeditor cap:");
    ALOGV("mMaxInputFrameWidth = %d", cap.mMaxInputFrameWidth);
    ALOGV("mMaxInputFrameHeight = %d", cap.mMaxInputFrameHeight);
    ALOGV("mMaxOutputFrameWidth = %d", cap.mMaxOutputFrameWidth);
    ALOGV("mMaxOutputFrameHeight = %d", cap.mMaxOutputFrameHeight);
}

/*static*/ int
MediaProfiles::findTagForName(const MediaProfiles::NameToTagMap *map, size_t nMappings, const char *name)
{
    int tag = -1;
    for (size_t i = 0; i < nMappings; ++i) {
        if (!strcmp(map[i].name, name)) {
            tag = map[i].tag;
            break;
        }
    }
    return tag;
}

/*static*/ MediaProfiles::VideoCodec*
MediaProfiles::createVideoCodec(const char **atts, MediaProfiles *profiles)
{
    CHECK(!strcmp("codec",     atts[0]) &&
          !strcmp("bitRate",   atts[2]) &&
          !strcmp("width",     atts[4]) &&
          !strcmp("height",    atts[6]) &&
          !strcmp("frameRate", atts[8]));

    const size_t nMappings = sizeof(sVideoEncoderNameMap)/sizeof(sVideoEncoderNameMap[0]);
    const int codec = findTagForName(sVideoEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(static_cast<video_encoder>(codec),
            atoi(atts[3]), atoi(atts[5]), atoi(atts[7]), atoi(atts[9]));
    logVideoCodec(*videoCodec);

    size_t nCamcorderProfiles;
    CHECK((nCamcorderProfiles = profiles->mCamcorderProfiles.size()) >= 1);
    profiles->mCamcorderProfiles[nCamcorderProfiles - 1]->mVideoCodec = videoCodec;
    return videoCodec;
}

/*static*/ MediaProfiles::AudioCodec*
MediaProfiles::createAudioCodec(const char **atts, MediaProfiles *profiles)
{
    CHECK(!strcmp("codec",      atts[0]) &&
          !strcmp("bitRate",    atts[2]) &&
          !strcmp("sampleRate", atts[4]) &&
          !strcmp("channels",   atts[6]));
    const size_t nMappings = sizeof(sAudioEncoderNameMap)/sizeof(sAudioEncoderNameMap[0]);
    const int codec = findTagForName(sAudioEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::AudioCodec *audioCodec =
        new MediaProfiles::AudioCodec(static_cast<audio_encoder>(codec),
            atoi(atts[3]), atoi(atts[5]), atoi(atts[7]));
    logAudioCodec(*audioCodec);

    size_t nCamcorderProfiles;
    CHECK((nCamcorderProfiles = profiles->mCamcorderProfiles.size()) >= 1);
    profiles->mCamcorderProfiles[nCamcorderProfiles - 1]->mAudioCodec = audioCodec;
    return audioCodec;
}
/*static*/ MediaProfiles::AudioDecoderCap*
MediaProfiles::createAudioDecoderCap(const char **atts)
{
    CHECK(!strcmp("name",    atts[0]) &&
          !strcmp("enabled", atts[2]));

    const size_t nMappings = sizeof(sAudioDecoderNameMap)/sizeof(sAudioDecoderNameMap[0]);
    const int codec = findTagForName(sAudioDecoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::AudioDecoderCap *cap =
        new MediaProfiles::AudioDecoderCap(static_cast<audio_decoder>(codec));
    logAudioDecoderCap(*cap);
    return cap;
}

/*static*/ MediaProfiles::VideoDecoderCap*
MediaProfiles::createVideoDecoderCap(const char **atts)
{
    CHECK(!strcmp("name",    atts[0]) &&
          !strcmp("enabled", atts[2]));

    const size_t nMappings = sizeof(sVideoDecoderNameMap)/sizeof(sVideoDecoderNameMap[0]);
    const int codec = findTagForName(sVideoDecoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::VideoDecoderCap *cap =
        new MediaProfiles::VideoDecoderCap(static_cast<video_decoder>(codec));
    logVideoDecoderCap(*cap);
    return cap;
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createVideoEncoderCap(const char **atts)
{
    CHECK(!strcmp("name",           atts[0])  &&
          !strcmp("enabled",        atts[2])  &&
          !strcmp("minBitRate",     atts[4])  &&
          !strcmp("maxBitRate",     atts[6])  &&
          !strcmp("minFrameWidth",  atts[8])  &&
          !strcmp("maxFrameWidth",  atts[10]) &&
          !strcmp("minFrameHeight", atts[12]) &&
          !strcmp("maxFrameHeight", atts[14]) &&
          !strcmp("minFrameRate",   atts[16]) &&
          !strcmp("maxFrameRate",   atts[18]));

    const size_t nMappings = sizeof(sVideoEncoderNameMap)/sizeof(sVideoEncoderNameMap[0]);
    const int codec = findTagForName(sVideoEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::VideoEncoderCap *cap =
        new MediaProfiles::VideoEncoderCap(static_cast<video_encoder>(codec),
            atoi(atts[5]), atoi(atts[7]), atoi(atts[9]), atoi(atts[11]), atoi(atts[13]),
            atoi(atts[15]), atoi(atts[17]), atoi(atts[19]));
    logVideoEncoderCap(*cap);
    return cap;
}

/*static*/ MediaProfiles::AudioEncoderCap*
MediaProfiles::createAudioEncoderCap(const char **atts)
{
    CHECK(!strcmp("name",          atts[0])  &&
          !strcmp("enabled",       atts[2])  &&
          !strcmp("minBitRate",    atts[4])  &&
          !strcmp("maxBitRate",    atts[6])  &&
          !strcmp("minSampleRate", atts[8])  &&
          !strcmp("maxSampleRate", atts[10]) &&
          !strcmp("minChannels",   atts[12]) &&
          !strcmp("maxChannels",   atts[14]));

    const size_t nMappings = sizeof(sAudioEncoderNameMap)/sizeof(sAudioEncoderNameMap[0]);
    const int codec = findTagForName(sAudioEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::AudioEncoderCap *cap =
        new MediaProfiles::AudioEncoderCap(static_cast<audio_encoder>(codec), atoi(atts[5]), atoi(atts[7]),
            atoi(atts[9]), atoi(atts[11]), atoi(atts[13]),
            atoi(atts[15]));
    logAudioEncoderCap(*cap);
    return cap;
}

/*static*/ output_format
MediaProfiles::createEncoderOutputFileFormat(const char **atts)
{
    CHECK(!strcmp("name", atts[0]));

    const size_t nMappings =sizeof(sFileFormatMap)/sizeof(sFileFormatMap[0]);
    const int format = findTagForName(sFileFormatMap, nMappings, atts[1]);
    CHECK(format != -1);

    return static_cast<output_format>(format);
}

static bool isCameraIdFound(int cameraId, const Vector<int>& cameraIds) {
    for (int i = 0, n = cameraIds.size(); i < n; ++i) {
        if (cameraId == cameraIds[i]) {
            return true;
        }
    }
    return false;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createCamcorderProfile(int cameraId, const char **atts, Vector<int>& cameraIds)
{
    CHECK(!strcmp("quality",    atts[0]) &&
          !strcmp("fileFormat", atts[2]) &&
          !strcmp("duration",   atts[4]));

    const size_t nProfileMappings = sizeof(sCamcorderQualityNameMap)/sizeof(sCamcorderQualityNameMap[0]);
    const int quality = findTagForName(sCamcorderQualityNameMap, nProfileMappings, atts[1]);
    CHECK(quality != -1);

    const size_t nFormatMappings = sizeof(sFileFormatMap)/sizeof(sFileFormatMap[0]);
    const int fileFormat = findTagForName(sFileFormatMap, nFormatMappings, atts[3]);
    CHECK(fileFormat != -1);

    MediaProfiles::CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = cameraId;
    if (!isCameraIdFound(cameraId, cameraIds)) {
        cameraIds.add(cameraId);
    }
    profile->mFileFormat = static_cast<output_format>(fileFormat);
    profile->mQuality = static_cast<camcorder_quality>(quality);
    profile->mDuration = atoi(atts[5]);
    return profile;
}

MediaProfiles::ImageEncodingQualityLevels*
MediaProfiles::findImageEncodingQualityLevels(int cameraId) const
{
    int n = mImageEncodingQualityLevels.size();
    for (int i = 0; i < n; i++) {
        ImageEncodingQualityLevels *levels = mImageEncodingQualityLevels[i];
        if (levels->mCameraId == cameraId) {
            return levels;
        }
    }
    return NULL;
}

void MediaProfiles::addImageEncodingQualityLevel(int cameraId, const char** atts)
{
    CHECK(!strcmp("quality", atts[0]));
    int quality = atoi(atts[1]);
    ALOGV("%s: cameraId=%d, quality=%d", __func__, cameraId, quality);
    ImageEncodingQualityLevels *levels = findImageEncodingQualityLevels(cameraId);

    if (levels == NULL) {
        levels = new ImageEncodingQualityLevels();
        levels->mCameraId = cameraId;
        mImageEncodingQualityLevels.add(levels);
    }

    levels->mLevels.add(quality);
}

/*static*/ int
MediaProfiles::getCameraId(const char** atts)
{
    if (!atts[0]) return 0;  // default cameraId = 0
    CHECK(!strcmp("cameraId", atts[0]));
    return atoi(atts[1]);
}

void MediaProfiles::addStartTimeOffset(int cameraId, const char** atts)
{
    int offsetTimeMs = 1000;
    if (atts[2]) {
        CHECK(!strcmp("startOffsetMs", atts[2]));
        offsetTimeMs = atoi(atts[3]);
    }

    ALOGV("%s: cameraId=%d, offset=%d ms", __func__, cameraId, offsetTimeMs);
    mStartTimeOffsets.replaceValueFor(cameraId, offsetTimeMs);
}
/*static*/ MediaProfiles::ExportVideoProfile*
MediaProfiles::createExportVideoProfile(const char **atts)
{
    CHECK(!strcmp("name", atts[0]) &&
          !strcmp("profile", atts[2]) &&
          !strcmp("level", atts[4]));

    const size_t nMappings =
        sizeof(sVideoEncoderNameMap)/sizeof(sVideoEncoderNameMap[0]);
    const int codec = findTagForName(sVideoEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::ExportVideoProfile *profile =
        new MediaProfiles::ExportVideoProfile(
            codec, atoi(atts[3]), atoi(atts[5]));

    return profile;
}
/*static*/ MediaProfiles::VideoEditorCap*
MediaProfiles::createVideoEditorCap(const char **atts, MediaProfiles *profiles)
{
    CHECK(!strcmp("maxInputFrameWidth", atts[0]) &&
          !strcmp("maxInputFrameHeight", atts[2])  &&
          !strcmp("maxOutputFrameWidth", atts[4]) &&
          !strcmp("maxOutputFrameHeight", atts[6]) &&
          !strcmp("maxPrefetchYUVFrames", atts[8]));

    MediaProfiles::VideoEditorCap *pVideoEditorCap =
        new MediaProfiles::VideoEditorCap(atoi(atts[1]), atoi(atts[3]),
                atoi(atts[5]), atoi(atts[7]), atoi(atts[9]));

    logVideoEditorCap(*pVideoEditorCap);
    profiles->mVideoEditorCap = pVideoEditorCap;

    return pVideoEditorCap;
}

/*static*/ void
MediaProfiles::startElementHandler(void *userData, const char *name, const char **atts)
{
    MediaProfiles *profiles = (MediaProfiles *) userData;
    if (strcmp("Video", name) == 0) {
        createVideoCodec(atts, profiles);
    } else if (strcmp("Audio", name) == 0) {
        createAudioCodec(atts, profiles);
    } else if (strcmp("VideoEncoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mVideoEncoders.add(createVideoEncoderCap(atts));
    } else if (strcmp("AudioEncoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mAudioEncoders.add(createAudioEncoderCap(atts));
    } else if (strcmp("VideoDecoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mVideoDecoders.add(createVideoDecoderCap(atts));
    } else if (strcmp("AudioDecoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mAudioDecoders.add(createAudioDecoderCap(atts));
    } else if (strcmp("EncoderOutputFileFormat", name) == 0) {
        profiles->mEncoderOutputFileFormats.add(createEncoderOutputFileFormat(atts));
    } else if (strcmp("CamcorderProfiles", name) == 0) {
        profiles->mCurrentCameraId = getCameraId(atts);
        profiles->addStartTimeOffset(profiles->mCurrentCameraId, atts);
    } else if (strcmp("EncoderProfile", name) == 0) {
        profiles->mCamcorderProfiles.add(
            createCamcorderProfile(profiles->mCurrentCameraId, atts, profiles->mCameraIds));
    } else if (strcmp("ImageEncoding", name) == 0) {
        profiles->addImageEncodingQualityLevel(profiles->mCurrentCameraId, atts);
    } else if (strcmp("VideoEditorCap", name) == 0) {
        createVideoEditorCap(atts, profiles);
    } else if (strcmp("ExportVideoProfile", name) == 0) {
        profiles->mVideoEditorExportProfiles.add(createExportVideoProfile(atts));
    }
}

static bool isCamcorderProfile(camcorder_quality quality) {
    return quality >= CAMCORDER_QUALITY_LIST_START &&
           quality <= CAMCORDER_QUALITY_LIST_END;
}

static bool isTimelapseProfile(camcorder_quality quality) {
    return quality >= CAMCORDER_QUALITY_TIME_LAPSE_LIST_START &&
           quality <= CAMCORDER_QUALITY_TIME_LAPSE_LIST_END;
}

void MediaProfiles::initRequiredProfileRefs(const Vector<int>& cameraIds) {
    ALOGV("Number of camera ids: %d", cameraIds.size());
    CHECK(cameraIds.size() > 0);
    mRequiredProfileRefs = new RequiredProfiles[cameraIds.size()];
    for (size_t i = 0, n = cameraIds.size(); i < n; ++i) {
        mRequiredProfileRefs[i].mCameraId = cameraIds[i];
        for (size_t j = 0; j < kNumRequiredProfiles; ++j) {
            mRequiredProfileRefs[i].mRefs[j].mHasRefProfile = false;
            mRequiredProfileRefs[i].mRefs[j].mRefProfileIndex = -1;
            if ((j & 1) == 0) {  // low resolution
                mRequiredProfileRefs[i].mRefs[j].mResolutionProduct = 0x7FFFFFFF;
            } else {             // high resolution
                mRequiredProfileRefs[i].mRefs[j].mResolutionProduct = 0;
            }
        }
    }
}

int MediaProfiles::getRequiredProfileRefIndex(int cameraId) {
    for (size_t i = 0, n = mCameraIds.size(); i < n; ++i) {
        if (mCameraIds[i] == cameraId) {
            return i;
        }
    }
    return -1;
}

void MediaProfiles::checkAndAddRequiredProfilesIfNecessary() {
    if (sIsInitialized) {
        return;
    }

    initRequiredProfileRefs(mCameraIds);

    for (size_t i = 0; i < mCamcorderProfiles.size(); ++i) {
        int product = mCamcorderProfiles[i]->mVideoCodec->mFrameWidth *
                      mCamcorderProfiles[i]->mVideoCodec->mFrameHeight;

        camcorder_quality quality = mCamcorderProfiles[i]->mQuality;
        int cameraId = mCamcorderProfiles[i]->mCameraId;
        int index = -1;
        int refIndex = getRequiredProfileRefIndex(cameraId);
        CHECK(refIndex != -1);
        RequiredProfileRefInfo *info;
        camcorder_quality refQuality;
        VideoCodec *codec = NULL;

        // Check high and low from either camcorder profile or timelapse profile
        // but not both. Default, check camcorder profile
        size_t j = 0;
        size_t o = 2;
        if (isTimelapseProfile(quality)) {
            // Check timelapse profile instead.
            j = 2;
            o = kNumRequiredProfiles;
        } else {
            // Must be camcorder profile.
            CHECK(isCamcorderProfile(quality));
        }
        for (; j < o; ++j) {
            info = &(mRequiredProfileRefs[refIndex].mRefs[j]);
            if ((j % 2 == 0 && product > info->mResolutionProduct) ||  // low
                (j % 2 != 0 && product < info->mResolutionProduct)) {  // high
                continue;
            }
            switch (j) {
                case 0:
                   refQuality = CAMCORDER_QUALITY_LOW;
                   break;
                case 1:
                   refQuality = CAMCORDER_QUALITY_HIGH;
                   break;
                case 2:
                   refQuality = CAMCORDER_QUALITY_TIME_LAPSE_LOW;
                   break;
                case 3:
                   refQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
                   break;
                default:
                    CHECK(!"Should never reach here");
            }

            if (!info->mHasRefProfile) {
                index = getCamcorderProfileIndex(cameraId, refQuality);
            }
            if (index == -1) {
                // New high or low quality profile is found.
                // Update its reference.
                info->mHasRefProfile = true;
                info->mRefProfileIndex = i;
                info->mResolutionProduct = product;
            }
        }
    }

    for (size_t cameraId = 0; cameraId < mCameraIds.size(); ++cameraId) {
        for (size_t j = 0; j < kNumRequiredProfiles; ++j) {
            int refIndex = getRequiredProfileRefIndex(cameraId);
            CHECK(refIndex != -1);
            RequiredProfileRefInfo *info =
                    &mRequiredProfileRefs[refIndex].mRefs[j];

            if (info->mHasRefProfile) {

                CamcorderProfile *profile =
                    new CamcorderProfile(
                            *mCamcorderProfiles[info->mRefProfileIndex]);

                // Overwrite the quality
                switch (j % kNumRequiredProfiles) {
                    case 0:
                        profile->mQuality = CAMCORDER_QUALITY_LOW;
                        break;
                    case 1:
                        profile->mQuality = CAMCORDER_QUALITY_HIGH;
                        break;
                    case 2:
                        profile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_LOW;
                        break;
                    case 3:
                        profile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
                        break;
                    default:
                        CHECK(!"Should never come here");
                }

                int index = getCamcorderProfileIndex(cameraId, profile->mQuality);
                if (index != -1) {
                    ALOGV("Profile quality %d for camera %d already exists",
                        profile->mQuality, cameraId);
                    CHECK(index == refIndex);
                    continue;
                }

                // Insert the new profile
                ALOGV("Add a profile: quality %d=>%d for camera %d",
                        mCamcorderProfiles[info->mRefProfileIndex]->mQuality,
                        profile->mQuality, cameraId);

                mCamcorderProfiles.add(profile);
            }
        }
    }
}

/*static*/ MediaProfiles*
MediaProfiles::getInstance()
{
    ALOGV("getInstance");
    Mutex::Autolock lock(sLock);
    if (!sIsInitialized) {
        char value[PROPERTY_VALUE_MAX];
        if (property_get("media.settings.xml", value, NULL) <= 0) {
            const char *defaultXmlFile = "/etc/media_profiles.xml";
            FILE *fp = fopen(defaultXmlFile, "r");
            if (fp == NULL) {
                ALOGW("could not find media config xml file");
                sInstance = createDefaultInstance();
            } else {
                fclose(fp);  // close the file first.
                sInstance = createInstanceFromXmlFile(defaultXmlFile);
            }
        } else {
            sInstance = createInstanceFromXmlFile(value);
        }
        CHECK(sInstance != NULL);
        sInstance->checkAndAddRequiredProfilesIfNecessary();
        sIsInitialized = true;
    }

    return sInstance;
}

#if !defined(ANDROID_DEFAULT_PROFILE)
static int getVideoCapability(int i4VideoFormat,unsigned int *pu4Width, unsigned int *pu4Height, unsigned int *pu4BitRatem, unsigned int *pu4FrameRate);
#endif

static unsigned long getChipName(void);

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultH263VideoEncoderCap()
{
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
    return new MediaProfiles::VideoEncoderCap(
        VIDEO_ENCODER_H263, 375*1000, 10000*1000, 176, 704, 144, 576, 15, 30);
#else
    return new MediaProfiles::VideoEncoderCap(
        VIDEO_ENCODER_H263, 192000, 420000, 176, 352, 144, 288, 1, 20);
#endif
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultM4vVideoEncoderCap()
{
#if !defined(ANDROID_DEFAULT_PROFILE)
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;
    if(getVideoCapability(VIDEO_ENCODER_MPEG_4_SP, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0){     
        ALOGI("[ %s ], support maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",__FUNCTION__,u4Width,u4Height, u4BitRate, u4FrameRate);
        return new MediaProfiles::VideoEncoderCap(
            VIDEO_ENCODER_MPEG_4_SP, 75*1000, u4BitRate, 96, 
            u4Width, 96, u4Height, 
            15, 30);
    }
    else{
        return new MediaProfiles::VideoEncoderCap(
            VIDEO_ENCODER_MPEG_4_SP, 75*1000, 12500*1000, 96, 
            1280, 96, 720, 
            15, 30);
    }
#else
    return new MediaProfiles::VideoEncoderCap(
        VIDEO_ENCODER_MPEG_4_SP, 192000, 420000, 176, 352, 144, 288, 1, 20);
#endif  
}
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultH264VideoEncoderCap()
{
	unsigned long eChipName = getChipName();
	switch(eChipName)
	{
		#if !defined(ANDROID_DEFAULT_PROFILE)
		case VAL_CHIP_NAME_MT6572:
			{
				return new MediaProfiles::VideoEncoderCap(VIDEO_ENCODER_H264, /*250000*/75000, 2500*1000, 96,
					640, 96, 480, 15, 30);
			}
			break;
		#endif
		default:
			{
				return new MediaProfiles::VideoEncoderCap(
	        		VIDEO_ENCODER_H264, /*250000*/75000, 9000*1000, 96, 
	        			1280, 96, 720, 15, 30);
			}
			break;
	}
}
#endif
/*static*/ void
MediaProfiles::createDefaultVideoEncoders(MediaProfiles *profiles)
{
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
   profiles->mVideoEncoders.add(createDefaultH264VideoEncoderCap());
#endif
    profiles->mVideoEncoders.add(createDefaultH263VideoEncoderCap());
    profiles->mVideoEncoders.add(createDefaultM4vVideoEncoderCap());
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderTimeLapseQcifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 1000000, 176, 144, 20);

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderTimeLapse480pProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 6000*1000, 640, 480, 20);//for front cam not support 720x480 in BSP case
#else
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 20000000, 720, 480, 20);
#endif
    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ void
MediaProfiles::createDefaultCamcorderTimeLapseLowProfiles(
        MediaProfiles::CamcorderProfile **lowTimeLapseProfile,
        MediaProfiles::CamcorderProfile **lowSpecificTimeLapseProfile) {
    *lowTimeLapseProfile = createDefaultCamcorderTimeLapseQcifProfile(CAMCORDER_QUALITY_TIME_LAPSE_LOW);
    *lowSpecificTimeLapseProfile = createDefaultCamcorderTimeLapseQcifProfile(CAMCORDER_QUALITY_TIME_LAPSE_QCIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderTimeLapseHighProfiles(
        MediaProfiles::CamcorderProfile **highTimeLapseProfile,
        MediaProfiles::CamcorderProfile **highSpecificTimeLapseProfile) {
    *highTimeLapseProfile = createDefaultCamcorderTimeLapse480pProfile(CAMCORDER_QUALITY_TIME_LAPSE_HIGH);
    *highSpecificTimeLapseProfile = createDefaultCamcorderTimeLapse480pProfile(CAMCORDER_QUALITY_TIME_LAPSE_480P);
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderQcifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 192000, 176, 144, 20);

    MediaProfiles::AudioCodec *audioCodec =
        new MediaProfiles::AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);

    MediaProfiles::CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 30;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderCifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 360000, 352, 288, 20);

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ void
MediaProfiles::createDefaultCamcorderLowProfiles(
        MediaProfiles::CamcorderProfile **lowProfile,
        MediaProfiles::CamcorderProfile **lowSpecificProfile) {
    *lowProfile = createDefaultCamcorderQcifProfile(CAMCORDER_QUALITY_LOW);
    *lowSpecificProfile = createDefaultCamcorderQcifProfile(CAMCORDER_QUALITY_QCIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderHighProfiles(
        MediaProfiles::CamcorderProfile **highProfile,
        MediaProfiles::CamcorderProfile **highSpecificProfile) {
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
    *highProfile = createMTKCamcorderProfile(CAMCORDER_QUALITY_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
    *highSpecificProfile = createMTKCamcorderProfile(CAMCORDER_QUALITY_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);   
#else
    *highProfile = createDefaultCamcorderCifProfile(CAMCORDER_QUALITY_HIGH);
    *highSpecificProfile = createDefaultCamcorderCifProfile(CAMCORDER_QUALITY_CIF);
#endif
}

#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderFrontQcifProfile(camcorder_quality quality)
{
	MediaProfiles::VideoCodec *videoCodec =
		new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 192000, 176, 144, 20);

    MediaProfiles::AudioCodec *audioCodec =
        new MediaProfiles::AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);

    MediaProfiles::CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 1;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 30;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderFrontCifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 360000, 352, 288, 20);
    
    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 1;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderFrontTimeLapseQcifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 1000000, 176, 144, 20);

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 1;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderFrontTimeLapse480pProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)        
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 6000*1000, 640, 480, 20);//for front cam not support 720x480 in BSP case
#else
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 20000000, 720, 480, 20);
#endif

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);  
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 1;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 30;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ void
MediaProfiles::createDefaultCamcorderFrontLowProfiles(
        MediaProfiles::CamcorderProfile **lowProfile,
        MediaProfiles::CamcorderProfile **lowSpecificProfile) {       
    *lowProfile = createDefaultCamcorderFrontQcifProfile(CAMCORDER_QUALITY_LOW);
    *lowSpecificProfile = createDefaultCamcorderFrontQcifProfile(CAMCORDER_QUALITY_QCIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderFrontHighProfiles(
        MediaProfiles::CamcorderProfile **highProfile,
        MediaProfiles::CamcorderProfile **highSpecificProfile) {
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
    *highProfile = createMTKCamcorderProfile(CAMCORDER_QUALITY_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    *highSpecificProfile = createMTKCamcorderProfile(CAMCORDER_QUALITY_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);   
#else
    *highProfile = createDefaultCamcorderFrontCifProfile(CAMCORDER_QUALITY_HIGH);
    *highSpecificProfile = createDefaultCamcorderFrontCifProfile(CAMCORDER_QUALITY_CIF);
#endif
}
/*static*/ void
MediaProfiles::createDefaultCamcorderFrontTimeLapseLowProfiles(
        MediaProfiles::CamcorderProfile **lowTimeLapseProfile,
        MediaProfiles::CamcorderProfile **lowSpecificTimeLapseProfile) {
    *lowTimeLapseProfile = createDefaultCamcorderFrontTimeLapseQcifProfile(CAMCORDER_QUALITY_TIME_LAPSE_LOW);
    *lowSpecificTimeLapseProfile = createDefaultCamcorderFrontTimeLapseQcifProfile(CAMCORDER_QUALITY_TIME_LAPSE_QCIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderFrontTimeLapseHighProfiles(
        MediaProfiles::CamcorderProfile **highTimeLapseProfile,
        MediaProfiles::CamcorderProfile **highSpecificTimeLapseProfile) {
    *highTimeLapseProfile = createDefaultCamcorderFrontTimeLapse480pProfile(CAMCORDER_QUALITY_TIME_LAPSE_HIGH);
    *highSpecificTimeLapseProfile = createDefaultCamcorderFrontTimeLapse480pProfile(CAMCORDER_QUALITY_TIME_LAPSE_480P);
}

#if !defined(ANDROID_DEFAULT_PROFILE)
static int getVideoCapability(int i4VideoFormat,unsigned int *pu4Width, unsigned int *pu4Height, unsigned int *pu4BitRatem, unsigned int *pu4FrameRate)
{
    int i4RetValue = 1;
    VENC_DRV_QUERY_VIDEO_FORMAT_T qinfo;
    VENC_DRV_QUERY_VIDEO_FORMAT_T outinfo;
    VENC_DRV_MRESULT_T ret;
    
    if((NULL == pu4Width) || (NULL == pu4Height) || (NULL == pu4BitRatem) || (NULL == pu4FrameRate)){
        return -1;
    }
    
    memset(&qinfo,0,sizeof(VENC_DRV_QUERY_VIDEO_FORMAT_T));
    memset(&outinfo,0,sizeof(VENC_DRV_QUERY_VIDEO_FORMAT_T));
    switch (i4VideoFormat)
    {
        /*
        case VIDEO_ENCODER_H263 :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_H263;
        break;*/
        case VIDEO_ENCODER_H264 :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_H264;
            ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_VIDEO_FORMAT, &qinfo, &outinfo);
            if(ret ==  VENC_DRV_MRESULT_OK){
                (*pu4Width)= outinfo.u4Width;
                (*pu4Height) = outinfo.u4Height;
                (*pu4BitRatem) = outinfo.u4Bitrate;
                (*pu4FrameRate) = outinfo.u4FrameRate;
                ALOGI("checkVideoCapability, format=%d,support maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",i4VideoFormat,outinfo.u4Width,outinfo.u4Height, outinfo.u4Bitrate, outinfo.u4FrameRate);
            }
            else{
                i4RetValue = -1;
            }
        break;
        case VIDEO_ENCODER_MPEG_4_SP :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_MPEG4;
            ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_VIDEO_FORMAT, &qinfo, &outinfo);
            if(ret ==  VENC_DRV_MRESULT_OK){
                (*pu4Width)= outinfo.u4Width;
                (*pu4Height) = outinfo.u4Height;
                (*pu4BitRatem) = outinfo.u4Bitrate;
                (*pu4FrameRate) = outinfo.u4FrameRate;
                ALOGI("checkVideoCapability, format=%d,support maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",i4VideoFormat,outinfo.u4Width,outinfo.u4Height, outinfo.u4Bitrate, outinfo.u4FrameRate);
            }
            else{
                i4RetValue = -1;
            }
        break;
        default:
            i4RetValue = -1;
            break;
    }
     return i4RetValue;
}

#endif

static unsigned long getChipName(void)
{
#if !defined(ANDROID_DEFAULT_PROFILE)
	static bool fGetChipName = false;
	static VAL_UINT32_T outChipName = VAL_CHIP_NAME_MAX;
	 
	if (!fGetChipName)
	{
		VENC_DRV_MRESULT_T ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_CHIP_NAME, NULL, &outChipName);
		if(ret ==  VENC_DRV_MRESULT_OK)
		{
			ALOGI("getChipName success = %d", outChipName);		
		}
		fGetChipName = true;
	}
	
	return (unsigned long)outChipName;
#else
	return 0xFFFFFFFF;
#endif
}


/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createMTKCamcorderProfile(camcorder_quality quality, camcorder_mode CamMode, camera_id CamId)
{
    MediaProfiles::VideoCodec *videoCodec = NULL;
    MediaProfiles::AudioCodec *audioCodec = NULL;
    int64_t memory_size_byte = (int64_t)sysconf(_SC_PHYS_PAGES) * PAGE_SIZE;
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;
	unsigned int MEMORY_SIZE_IS_LARGE = 0;
	if (memory_size_byte > 256*1024*1024) 
		MEMORY_SIZE_IS_LARGE = 1;
	else 
		MEMORY_SIZE_IS_LARGE = 0;

	// Get Chip Name from video driver
	unsigned long eChipName = getChipName();
		
	// Setting for VIDEO Profile
	switch(quality)
	{
		case CAMCORDER_QUALITY_MTK_LOW:
		case CAMCORDER_QUALITY_MTK_NIGHT_LOW:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW:
			switch(eChipName)
			{
				#if !defined(ANDROID_DEFAULT_PROFILE)
				case VAL_CHIP_NAME_MT6572:
					{
						if (CamId == BACK_CAMERA){
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 750*1000/CamMode, 176, 144, 30/CamMode);                        
						}
						else{                            
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 150*1000/CamMode, 176, 144, 30/CamMode);                        
						}							
					}
					break;
				#endif
				default:
					{
						if (CamId == BACK_CAMERA){
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 750*1000/CamMode, 176, 144, 30/CamMode);
						}
						else{
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 150*1000/CamMode, 176, 144, 30/CamMode);
						}
					}
					break;
			}
			break;
		case CAMCORDER_QUALITY_MTK_MEDIUM:
		case CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM:
#ifdef MTK_CAMCORDER_PROFILE_MID_MP4
			if (CamId == BACK_CAMERA){
				videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 4000*1000/CamMode, 480, 320, 30/CamMode);
			}
			else{
				videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 2000*1000/CamMode, 480, 320, 30/CamMode);
			 }
#else
			switch(eChipName)
			{
				#if !defined(ANDROID_DEFAULT_PROFILE)
				case VAL_CHIP_NAME_MT6572:
					{
						videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 2500*1000/CamMode, 480, 320, 30/CamMode);
					}
					break;
				#endif
				default:
					{
						videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, (CamMode == CAMCORDER_DAY_MODE) ? 3000*1000 : 1250*1000, 640, 480, 30/CamMode);
					}
					break;
			}
#endif
			break;
		case CAMCORDER_QUALITY_MTK_HIGH:
		case CAMCORDER_QUALITY_MTK_NIGHT_HIGH:	
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH:
			switch(eChipName)
			{
				#if !defined(ANDROID_DEFAULT_PROFILE)
				case VAL_CHIP_NAME_MT6572:
					{
						if (CamId == BACK_CAMERA){                            
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 6000*1000/CamMode, 640, 480, 30/CamMode);                        
						}                        
						else{                            
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
						}									
					}
					break;
				#endif
				default:
					{
						if (CamId == BACK_CAMERA){
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
						}
						else{
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
						}
					}
					break;
			}

			break;
		case CAMCORDER_QUALITY_480P:   // just for CTS
		case CAMCORDER_QUALITY_HIGH:   // just for CTS
                        if (CamId == BACK_CAMERA){
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 6000*1000/CamMode, 640, 480, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                        }
			break;
		case CAMCORDER_QUALITY_MTK_FINE:
		case CAMCORDER_QUALITY_MTK_NIGHT_FINE:	
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE:
			switch(eChipName)
			{
				#if !defined(ANDROID_DEFAULT_PROFILE)
				case VAL_CHIP_NAME_MT6572:
					{
						videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 12500*1000/CamMode, 1280, 720, 30/CamMode);
					}
					break;
				#endif
				default:
					{
						if (MEMORY_SIZE_IS_LARGE){
							#if !defined(ANDROID_DEFAULT_PROFILE)
							if(getVideoCapability(VIDEO_ENCODER_MPEG_4_SP, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0){
								if((u4Width >= 1920) && (u4Height >= 1088)){
									videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 26000*1000/CamMode, 1920, 1088, 30/CamMode);
								}
								else{
									videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, (u4BitRate)/CamMode, u4Width, u4Height, u4FrameRate/CamMode);
								}
							}
							else
							#endif
							{
								ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
								videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 26000*1000/CamMode, 1920, 1080, 30/CamMode);
							}
						}
						else{
							videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 12500*1000/CamMode, 1280, 720, 30/CamMode);
						}									
					}
					break;
			}
			break;
		case CAMCORDER_QUALITY_MTK_LIVE_EFFECT:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT:
                        if (CamId == BACK_CAMERA){
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 2500*1000/CamMode, 480, 320, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 2500*1000/CamMode, 480, 320, 30/CamMode);
                        }
                        break;
		case CAMCORDER_QUALITY_MTK_H264_HIGH:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_H264_HIGH:
                        if (CamId == BACK_CAMERA){
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 4000*1000/CamMode, 640, 480, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 4000*1000/CamMode, 640, 480, 30/CamMode);
                        }
                        break;
		case CAMCORDER_QUALITY_MTK_MPEG4_1080P:
		case CAMCORDER_QUALITY_MTK_TIME_LAPSE_MPEG4_1080P:
                        if (CamId == BACK_CAMERA){
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 14000*1000/CamMode, 1920, 1088, 15/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 14000*1000/CamMode, 1920, 1088, 15/CamMode);
                        }
                        break;
		default:
			videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 75*1000/CamMode, 96, 96, 30/CamMode);
			ALOGE("The given quality %d is not found", quality);
			break;
	}

    // Setting for AUDIO Profile
	switch(quality)
	{
        case CAMCORDER_QUALITY_MTK_LOW:
        case CAMCORDER_QUALITY_MTK_NIGHT_LOW:
        case CAMCORDER_QUALITY_TIME_LAPSE_LOW:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW:
#ifdef HAVE_AACENCODE           
            audioCodec = new AudioCodec(AUDIO_ENCODER_AAC, 64000, 48000, 2);            
#else
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
#endif
            break;

        //Fine quality AAC HE
        case CAMCORDER_QUALITY_MTK_FINE:
#ifdef HAVE_AACENCODE
            audioCodec = new AudioCodec(AUDIO_ENCODER_HE_AAC, 192000, 48000, 2);
#else
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
#endif

            break;

        default:
#ifdef HAVE_AACENCODE
            audioCodec = new AudioCodec(AUDIO_ENCODER_AAC, 128000, 48000, 2);
#else
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
#endif
            break;
	}

    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = CamId;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 30;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}
#endif
/*static*/ void
MediaProfiles::createDefaultCamcorderProfiles(MediaProfiles *profiles)
{
    // low camcorder profiles.
    MediaProfiles::CamcorderProfile *lowProfile, *lowSpecificProfile;
    createDefaultCamcorderLowProfiles(&lowProfile, &lowSpecificProfile);
    profiles->mCamcorderProfiles.add(lowProfile);
    profiles->mCamcorderProfiles.add(lowSpecificProfile);

    // high camcorder profiles.
    MediaProfiles::CamcorderProfile* highProfile, *highSpecificProfile;
    createDefaultCamcorderHighProfiles(&highProfile, &highSpecificProfile);
    profiles->mCamcorderProfiles.add(highProfile);
    profiles->mCamcorderProfiles.add(highSpecificProfile);

    // low camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *lowTimeLapseProfile, *lowSpecificTimeLapseProfile;
    createDefaultCamcorderTimeLapseLowProfiles(&lowTimeLapseProfile, &lowSpecificTimeLapseProfile);
    profiles->mCamcorderProfiles.add(lowTimeLapseProfile);
    profiles->mCamcorderProfiles.add(lowSpecificTimeLapseProfile);

    // high camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *highTimeLapseProfile, *highSpecificTimeLapseProfile;
    createDefaultCamcorderTimeLapseHighProfiles(&highTimeLapseProfile, &highSpecificTimeLapseProfile);
    profiles->mCamcorderProfiles.add(highTimeLapseProfile);
    profiles->mCamcorderProfiles.add(highSpecificTimeLapseProfile);

#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)

    // low camcorder profiles.
    MediaProfiles::CamcorderProfile *frontlowProfile, *frontlowSpecificProfile;
    createDefaultCamcorderFrontLowProfiles(&frontlowProfile, &frontlowSpecificProfile);
    profiles->mCamcorderProfiles.add(frontlowProfile);
    profiles->mCamcorderProfiles.add(frontlowSpecificProfile);

    // high camcorder profiles.
    MediaProfiles::CamcorderProfile* fronthighProfile, *fronthighSpecificProfile;
    createDefaultCamcorderFrontHighProfiles(&fronthighProfile, &fronthighSpecificProfile);
    profiles->mCamcorderProfiles.add(fronthighProfile);
    profiles->mCamcorderProfiles.add(fronthighSpecificProfile);

    // low camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *frontlowTimeLapseProfile, *frontlowSpecificTimeLapseProfile;
    createDefaultCamcorderFrontTimeLapseLowProfiles(&frontlowTimeLapseProfile, &frontlowSpecificTimeLapseProfile);
    profiles->mCamcorderProfiles.add(frontlowTimeLapseProfile);
    profiles->mCamcorderProfiles.add(frontlowSpecificTimeLapseProfile);

    // high camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *fronthighTimeLapseProfile, *fronthighSpecificTimeLapseProfile;
    createDefaultCamcorderFrontTimeLapseHighProfiles(&fronthighTimeLapseProfile, &fronthighSpecificTimeLapseProfile);
    profiles->mCamcorderProfiles.add(fronthighTimeLapseProfile);
    profiles->mCamcorderProfiles.add(fronthighSpecificTimeLapseProfile);


    // mtk low camcorder profiles.
    MediaProfiles::CamcorderProfile *LowProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *LowSpecificProfile =
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(LowProfile);
    profiles->mCamcorderProfiles.add(LowSpecificProfile);

	// mtk medium camcorder profiles.
	MediaProfiles::CamcorderProfile *MediumProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_MEDIUM, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *MediumSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_MEDIUM, CAMCORDER_DAY_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(MediumProfile);
	profiles->mCamcorderProfiles.add(MediumSpecificProfile);

    // mtk high camcorder profiles.
    MediaProfiles::CamcorderProfile *HighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *HighSpecificProfile =
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(HighProfile);
    profiles->mCamcorderProfiles.add(HighSpecificProfile);
    
    // 480p camcorder profiles.
    MediaProfiles::CamcorderProfile *Back480pProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *Back480pSpecificProfile =
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(Back480pProfile);
    profiles->mCamcorderProfiles.add(Back480pSpecificProfile);
    
    // mtk fine camcorder profiles.
    MediaProfiles::CamcorderProfile *FineProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_FINE, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *FineSpecificProfile =
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_FINE, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(FineProfile);
    profiles->mCamcorderProfiles.add(FineSpecificProfile);

    // mtk h264 high camcorder profiles.
    //MediaProfiles::CamcorderProfile *H264HighProfile =
            //createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_H264_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
	//MediaProfiles::CamcorderProfile *H264HighSpecificProfile =
		    //createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_H264_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
    //profiles->mCamcorderProfiles.add(H264HighProfile);
    //profiles->mCamcorderProfiles.add(H264HighSpecificProfile);

	// front low camcorder profiles.
	MediaProfiles::CamcorderProfile *FrontLowProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontLowSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontLowProfile);
	profiles->mCamcorderProfiles.add(FrontLowSpecificProfile);

	// front high camcorder profiles.
	MediaProfiles::CamcorderProfile *FrontHighProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontHighSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontHighProfile);
	profiles->mCamcorderProfiles.add(FrontHighSpecificProfile);
    
    // front 480p camcorder profiles.
	MediaProfiles::CamcorderProfile *Front480pProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *Front480pSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(Front480pProfile);
	profiles->mCamcorderProfiles.add(Front480pSpecificProfile);
    
	// night low camcorder profiles.
	MediaProfiles::CamcorderProfile *NightLowProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_LOW, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightLowSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_LOW, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightLowProfile);
	profiles->mCamcorderProfiles.add(NightLowSpecificProfile);

	// night medium camcorder profiles.
	MediaProfiles::CamcorderProfile *NightMediumProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightMediumSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightMediumProfile);
	profiles->mCamcorderProfiles.add(NightMediumSpecificProfile);

	// night high camcorder profiles.
	MediaProfiles::CamcorderProfile *NightHighProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightHighSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightHighProfile);
	profiles->mCamcorderProfiles.add(NightHighProfile);	

	// night fine camcorder profiles.
	MediaProfiles::CamcorderProfile *NightFineProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_FINE, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightFineSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_FINE, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightFineProfile);
	profiles->mCamcorderProfiles.add(NightFineProfile);	


	// front night low camcorder profiles.
	MediaProfiles::CamcorderProfile *FrontNightLowProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_LOW, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontNightLowSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_LOW, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontNightLowProfile);
	profiles->mCamcorderProfiles.add(FrontNightLowSpecificProfile);	

	// front night high camcorder profiles.
	MediaProfiles::CamcorderProfile *FrontNightHighProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontNightHighSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontNightHighProfile);
	profiles->mCamcorderProfiles.add(FrontNightHighSpecificProfile);	

// LIVE EFFECT Profiles
// mtk live effect camcorder profiles.
MediaProfiles::CamcorderProfile *LiveEffectProfile =
        createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LIVE_EFFECT, CAMCORDER_DAY_MODE, BACK_CAMERA);
MediaProfiles::CamcorderProfile *LiveEffectSpecificProfile =
        createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LIVE_EFFECT, CAMCORDER_DAY_MODE, BACK_CAMERA);
profiles->mCamcorderProfiles.add(LiveEffectProfile);
profiles->mCamcorderProfiles.add(LiveEffectSpecificProfile);


// front night high camcorder profiles.
MediaProfiles::CamcorderProfile *LiveEffectFrontProfile =
        createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LIVE_EFFECT, CAMCORDER_DAY_MODE, FRONT_CAMERA);
MediaProfiles::CamcorderProfile *LiveEffectFrontSpecificProfile =
        createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LIVE_EFFECT, CAMCORDER_DAY_MODE, FRONT_CAMERA);
profiles->mCamcorderProfiles.add(LiveEffectFrontProfile);
profiles->mCamcorderProfiles.add(LiveEffectFrontSpecificProfile);    


// TIME LAPSE Profiles

    // mtk low camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *LowTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *LowTimeLapseSpecificProfile =
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(LowTimeLapseProfile);
    profiles->mCamcorderProfiles.add(LowTimeLapseSpecificProfile);

	// mtk medium camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *MediumTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *MediumTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM, CAMCORDER_DAY_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(MediumTimeLapseProfile);
	profiles->mCamcorderProfiles.add(MediumTimeLapseSpecificProfile);

    // mtk high camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *HighTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *HighTimeLapseSpecificProfile = 
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);;
    profiles->mCamcorderProfiles.add(HighTimeLapseProfile);
    profiles->mCamcorderProfiles.add(HighTimeLapseSpecificProfile);

    // mtk fine camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *FineTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *FineTimeLapseSpecificProfile = 
		    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE, CAMCORDER_DAY_MODE, BACK_CAMERA);;
    profiles->mCamcorderProfiles.add(FineTimeLapseProfile);
    profiles->mCamcorderProfiles.add(FineTimeLapseSpecificProfile);

    // mtk h264 high camcorder time lapse profiles.
    //MediaProfiles::CamcorderProfile *H264HighTimeLapseProfile =
            //createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_H264_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
	//MediaProfiles::CamcorderProfile *H264HighTimeLapseSpecificProfile = 
		    //createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_H264_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);;
    //profiles->mCamcorderProfiles.add(H264HighTimeLapseProfile);
    //profiles->mCamcorderProfiles.add(H264HighTimeLapseSpecificProfile);

	// front low camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *FrontLowTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontLowTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontLowTimeLapseProfile);
	profiles->mCamcorderProfiles.add(FrontLowTimeLapseSpecificProfile);

	// front high camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *FrontHighTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontHighTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontHighTimeLapseProfile);
	profiles->mCamcorderProfiles.add(FrontHighTimeLapseSpecificProfile);

	// night low camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *NightLowTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightLowTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightLowTimeLapseProfile);
	profiles->mCamcorderProfiles.add(NightLowTimeLapseSpecificProfile);

	// night medium camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *NightMediumTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightMediumTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightMediumTimeLapseProfile);
	profiles->mCamcorderProfiles.add(NightMediumTimeLapseSpecificProfile);

	// night high camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *NightHighTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightHighTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightHighTimeLapseProfile);
	profiles->mCamcorderProfiles.add(NightHighTimeLapseSpecificProfile);

	// night fine camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *NightFineTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *NightFineTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(NightFineTimeLapseProfile);
	profiles->mCamcorderProfiles.add(NightFineTimeLapseSpecificProfile);

        // live effect camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *LiveEffectTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT, CAMCORDER_DAY_MODE, BACK_CAMERA);
	MediaProfiles::CamcorderProfile *LiveEffectTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT, CAMCORDER_DAY_MODE, BACK_CAMERA);
	profiles->mCamcorderProfiles.add(LiveEffectTimeLapseProfile);
	profiles->mCamcorderProfiles.add(LiveEffectTimeLapseSpecificProfile);
    

        // live effect camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *LiveEffectTimeLapseFrontProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *LiveEffectTimeLapseSpecificFrontProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT, CAMCORDER_DAY_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(LiveEffectTimeLapseFrontProfile);
	profiles->mCamcorderProfiles.add(LiveEffectTimeLapseSpecificFrontProfile);


	// front night low camcorder profiles.
	MediaProfiles::CamcorderProfile *FrontNightLowTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontNightLowTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontNightLowTimeLapseProfile);
	profiles->mCamcorderProfiles.add(FrontNightLowTimeLapseSpecificProfile); 

	// front night high camcorder time lapse profiles.
	MediaProfiles::CamcorderProfile *FrontNightHighTimeLapseProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	MediaProfiles::CamcorderProfile *FrontNightHighTimeLapseSpecificProfile =
			createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
	profiles->mCamcorderProfiles.add(FrontNightHighTimeLapseProfile);
	profiles->mCamcorderProfiles.add(FrontNightHighTimeLapseSpecificProfile);
#endif
    // For emulator and other legacy devices which does not have a
    // media_profiles.xml file, We assume that the default camera id
    // is 0 and that is the only camera available.
    profiles->mCameraIds.push(0);
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_PROFILE)
    profiles->mCameraIds.push(1);
#endif
}

/*static*/ void
MediaProfiles::createDefaultAudioEncoders(MediaProfiles *profiles)
{
    profiles->mAudioEncoders.add(createDefaultAmrNBEncoderCap());
//MTK80721 2011-12-14 
#ifndef ANDROID_DEFAULT_CODE
    MediaProfiles::AudioEncoderCap* mAwbCap = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_AMR_WB, 6600, 28500, 16000, 16000, 1, 1);
    profiles->mAudioEncoders.add(mAwbCap);
	
    MediaProfiles::AudioEncoderCap* mAacCap = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_AAC, 4000, 160000, 8000, 48000, 1, 2);
    profiles->mAudioEncoders.add(mAacCap);
    
    //2012-10-09
    MediaProfiles::AudioEncoderCap* mAacCapHE = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_HE_AAC, 4000, 320000, 8000, 48000, 1, 2);
    profiles->mAudioEncoders.add(mAacCapHE);
    
    MediaProfiles::AudioEncoderCap* mAacCapELD = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_AAC_ELD, 4000, 320000, 8000, 48000, 1, 2);
    profiles->mAudioEncoders.add(mAacCapELD);
    //
    MediaProfiles::AudioEncoderCap* mVorbisCap = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_VORBIS, 31980, 202960, 8000, 48000,  1, 2);
    profiles->mAudioEncoders.add(mVorbisCap);
#endif
//
}

/*static*/ void
MediaProfiles::createDefaultVideoDecoders(MediaProfiles *profiles)
{
    MediaProfiles::VideoDecoderCap *cap =
        new MediaProfiles::VideoDecoderCap(VIDEO_DECODER_WMV);

    profiles->mVideoDecoders.add(cap);
}

/*static*/ void
MediaProfiles::createDefaultAudioDecoders(MediaProfiles *profiles)
{
    MediaProfiles::AudioDecoderCap *cap =
        new MediaProfiles::AudioDecoderCap(AUDIO_DECODER_WMA);

    profiles->mAudioDecoders.add(cap);
}

/*static*/ void
MediaProfiles::createDefaultEncoderOutputFileFormats(MediaProfiles *profiles)
{
    profiles->mEncoderOutputFileFormats.add(OUTPUT_FORMAT_THREE_GPP);
    profiles->mEncoderOutputFileFormats.add(OUTPUT_FORMAT_MPEG_4);
}

/*static*/ MediaProfiles::AudioEncoderCap*
MediaProfiles::createDefaultAmrNBEncoderCap()
{
    return new MediaProfiles::AudioEncoderCap(
        AUDIO_ENCODER_AMR_NB, 5525, 12200, 8000, 8000, 1, 1);
}

/*static*/ void
MediaProfiles::createDefaultImageEncodingQualityLevels(MediaProfiles *profiles)
{
    ImageEncodingQualityLevels *levels = new ImageEncodingQualityLevels();
    levels->mCameraId = 0;
    levels->mLevels.add(70);
    levels->mLevels.add(80);
    levels->mLevels.add(90);
    profiles->mImageEncodingQualityLevels.add(levels);
    
#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_CODE)
    ALOGE("FrontCameraLevels Setting\n");
    ImageEncodingQualityLevels *FrontCameraLevels = new ImageEncodingQualityLevels();
    FrontCameraLevels->mCameraId = 1;
    FrontCameraLevels->mLevels.add(70);
    FrontCameraLevels->mLevels.add(80);
    FrontCameraLevels->mLevels.add(90);
    profiles->mImageEncodingQualityLevels.add(FrontCameraLevels);
#endif
}

/*static*/ void
MediaProfiles::createDefaultVideoEditorCap(MediaProfiles *profiles)
{
    profiles->mVideoEditorCap =
        new MediaProfiles::VideoEditorCap(
                VIDEOEDITOR_DEFAULT_MAX_INPUT_FRAME_WIDTH,
                VIDEOEDITOR_DEFUALT_MAX_INPUT_FRAME_HEIGHT,
                VIDEOEDITOR_DEFAULT_MAX_OUTPUT_FRAME_WIDTH,
                VIDEOEDITOR_DEFUALT_MAX_OUTPUT_FRAME_HEIGHT,
                VIDEOEDITOR_DEFAULT_MAX_PREFETCH_YUV_FRAMES);
}
/*static*/ void
MediaProfiles::createDefaultExportVideoProfiles(MediaProfiles *profiles)
{
    // Create default video export profiles
    profiles->mVideoEditorExportProfiles.add(
        new ExportVideoProfile(VIDEO_ENCODER_H263,
            OMX_VIDEO_H263ProfileBaseline, OMX_VIDEO_H263Level10));
    profiles->mVideoEditorExportProfiles.add(
        new ExportVideoProfile(VIDEO_ENCODER_MPEG_4_SP,
            OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level1));
    profiles->mVideoEditorExportProfiles.add(
        new ExportVideoProfile(VIDEO_ENCODER_H264,
            OMX_VIDEO_AVCProfileBaseline, OMX_VIDEO_AVCLevel13));
}

/*static*/ MediaProfiles*
MediaProfiles::createDefaultInstance()
{
    MediaProfiles *profiles = new MediaProfiles;
    createDefaultCamcorderProfiles(profiles);
    createDefaultVideoEncoders(profiles);
    createDefaultAudioEncoders(profiles);
    createDefaultVideoDecoders(profiles);
    createDefaultAudioDecoders(profiles);
    createDefaultEncoderOutputFileFormats(profiles);
    createDefaultImageEncodingQualityLevels(profiles);
    createDefaultVideoEditorCap(profiles);
    createDefaultExportVideoProfiles(profiles);
    return profiles;
}

/*static*/ MediaProfiles*
MediaProfiles::createInstanceFromXmlFile(const char *xml)
{
    FILE *fp = NULL;
    CHECK((fp = fopen(xml, "r")));

    XML_Parser parser = ::XML_ParserCreate(NULL);
    CHECK(parser != NULL);

    MediaProfiles *profiles = new MediaProfiles();
    ::XML_SetUserData(parser, profiles);
    ::XML_SetElementHandler(parser, startElementHandler, NULL);

    /*
      FIXME:
      expat is not compiled with -DXML_DTD. We don't have DTD parsing support.

      if (!::XML_SetParamEntityParsing(parser, XML_PARAM_ENTITY_PARSING_ALWAYS)) {
          ALOGE("failed to enable DTD support in the xml file");
          return UNKNOWN_ERROR;
      }

    */

    const int BUFF_SIZE = 512;
    for (;;) {
        void *buff = ::XML_GetBuffer(parser, BUFF_SIZE);
        if (buff == NULL) {
            ALOGE("failed to in call to XML_GetBuffer()");
            delete profiles;
            profiles = NULL;
            goto exit;
        }

        int bytes_read = ::fread(buff, 1, BUFF_SIZE, fp);
        if (bytes_read < 0) {
            ALOGE("failed in call to read");
            delete profiles;
            profiles = NULL;
            goto exit;
        }

        CHECK(::XML_ParseBuffer(parser, bytes_read, bytes_read == 0));

        if (bytes_read == 0) break;  // done parsing the xml file
    }

exit:
    ::XML_ParserFree(parser);
    ::fclose(fp);
    return profiles;
}

Vector<output_format> MediaProfiles::getOutputFileFormats() const
{
    return mEncoderOutputFileFormats;  // copy out
}

Vector<video_encoder> MediaProfiles::getVideoEncoders() const
{
    Vector<video_encoder> encoders;
    for (size_t i = 0; i < mVideoEncoders.size(); ++i) {
        encoders.add(mVideoEncoders[i]->mCodec);
    }
    return encoders;  // copy out
}

int MediaProfiles::getVideoEncoderParamByName(const char *name, video_encoder codec) const
{
    ALOGV("getVideoEncoderParamByName: %s for codec %d", name, codec);
    int index = -1;
    for (size_t i = 0, n = mVideoEncoders.size(); i < n; ++i) {
        if (mVideoEncoders[i]->mCodec == codec) {
            index = i;
            break;
        }
    }
    if (index == -1) {
        ALOGE("The given video encoder %d is not found", codec);
        return -1;
    }

    if (!strcmp("enc.vid.width.min", name)) return mVideoEncoders[index]->mMinFrameWidth;
    if (!strcmp("enc.vid.width.max", name)) return mVideoEncoders[index]->mMaxFrameWidth;
    if (!strcmp("enc.vid.height.min", name)) return mVideoEncoders[index]->mMinFrameHeight;
    if (!strcmp("enc.vid.height.max", name)) return mVideoEncoders[index]->mMaxFrameHeight;
    if (!strcmp("enc.vid.bps.min", name)) return mVideoEncoders[index]->mMinBitRate;
    if (!strcmp("enc.vid.bps.max", name)) return mVideoEncoders[index]->mMaxBitRate;
    if (!strcmp("enc.vid.fps.min", name)) return mVideoEncoders[index]->mMinFrameRate;
    if (!strcmp("enc.vid.fps.max", name)) return mVideoEncoders[index]->mMaxFrameRate;

    ALOGE("The given video encoder param name %s is not found", name);
    return -1;
}
int MediaProfiles::getVideoEditorExportParamByName(
    const char *name, int codec) const
{
    ALOGV("getVideoEditorExportParamByName: name %s codec %d", name, codec);
    ExportVideoProfile *exportProfile = NULL;
    int index = -1;
    for (size_t i =0; i < mVideoEditorExportProfiles.size(); i++) {
        exportProfile = mVideoEditorExportProfiles[i];
        if (exportProfile->mCodec == codec) {
            index = i;
            break;
        }
    }
    if (index == -1) {
        ALOGE("The given video decoder %d is not found", codec);
        return -1;
    }
    if (!strcmp("videoeditor.export.profile", name))
        return exportProfile->mProfile;
    if (!strcmp("videoeditor.export.level", name))
        return exportProfile->mLevel;

    ALOGE("The given video editor export param name %s is not found", name);
    return -1;
}
int MediaProfiles::getVideoEditorCapParamByName(const char *name) const
{
    ALOGV("getVideoEditorCapParamByName: %s", name);

    if (mVideoEditorCap == NULL) {
        ALOGE("The mVideoEditorCap is not created, then create default cap.");
        createDefaultVideoEditorCap(sInstance);
    }

    if (!strcmp("videoeditor.input.width.max", name))
        return mVideoEditorCap->mMaxInputFrameWidth;
    if (!strcmp("videoeditor.input.height.max", name))
        return mVideoEditorCap->mMaxInputFrameHeight;
    if (!strcmp("videoeditor.output.width.max", name))
        return mVideoEditorCap->mMaxOutputFrameWidth;
    if (!strcmp("videoeditor.output.height.max", name))
        return mVideoEditorCap->mMaxOutputFrameHeight;
    if (!strcmp("maxPrefetchYUVFrames", name))
        return mVideoEditorCap->mMaxPrefetchYUVFrames;

    ALOGE("The given video editor param name %s is not found", name);
    return -1;
}

Vector<audio_encoder> MediaProfiles::getAudioEncoders() const
{
    Vector<audio_encoder> encoders;
    for (size_t i = 0; i < mAudioEncoders.size(); ++i) {
        encoders.add(mAudioEncoders[i]->mCodec);
    }
    return encoders;  // copy out
}

int MediaProfiles::getAudioEncoderParamByName(const char *name, audio_encoder codec) const
{
    ALOGV("getAudioEncoderParamByName: %s for codec %d", name, codec);
    int index = -1;
    for (size_t i = 0, n = mAudioEncoders.size(); i < n; ++i) {
        if (mAudioEncoders[i]->mCodec == codec) {
            index = i;
            break;
        }
    }
    if (index == -1) {
        ALOGE("The given audio encoder %d is not found", codec);
        return -1;
    }

    if (!strcmp("enc.aud.ch.min", name)) return mAudioEncoders[index]->mMinChannels;
    if (!strcmp("enc.aud.ch.max", name)) return mAudioEncoders[index]->mMaxChannels;
    if (!strcmp("enc.aud.bps.min", name)) return mAudioEncoders[index]->mMinBitRate;
    if (!strcmp("enc.aud.bps.max", name)) return mAudioEncoders[index]->mMaxBitRate;
    if (!strcmp("enc.aud.hz.min", name)) return mAudioEncoders[index]->mMinSampleRate;
    if (!strcmp("enc.aud.hz.max", name)) return mAudioEncoders[index]->mMaxSampleRate;

    ALOGE("The given audio encoder param name %s is not found", name);
    return -1;
}

Vector<video_decoder> MediaProfiles::getVideoDecoders() const
{
    Vector<video_decoder> decoders;
    for (size_t i = 0; i < mVideoDecoders.size(); ++i) {
        decoders.add(mVideoDecoders[i]->mCodec);
    }
    return decoders;  // copy out
}

Vector<audio_decoder> MediaProfiles::getAudioDecoders() const
{
    Vector<audio_decoder> decoders;
    for (size_t i = 0; i < mAudioDecoders.size(); ++i) {
        decoders.add(mAudioDecoders[i]->mCodec);
    }
    return decoders;  // copy out
}

#if defined(MTK_VIDEO_PROFILE) || !defined(ANDROID_DEFAULT_CODE)
size_t  MediaProfiles::getCamcorderProfilesNum(int id)
{
	return mCamcorderProfiles.size();
}

String8  MediaProfiles::getCamcorderProfilesCaps(int id)
{
	char buff[256];
	memset(buff,0,256);

	for (size_t i = 0; i < mCamcorderProfiles.size();  ++i) 
	{
        if (id == mCamcorderProfiles[i]->mCameraId) 
        {   
            char temp[10];
            memset(temp,0,10);
	   	    sprintf(temp,"%d,",mCamcorderProfiles[i]->mQuality);
            strcat(buff,temp);
        }
	}

    ALOGD("[getCamcorderProfilesCaps] mCameraId = %d, buff = %s", id, buff);

    return String8(buff);
}
#endif

int MediaProfiles::getCamcorderProfileIndex(int cameraId, camcorder_quality quality) const
{
    int index = -1;
    for (size_t i = 0, n = mCamcorderProfiles.size(); i < n; ++i) {
        if (mCamcorderProfiles[i]->mCameraId == cameraId &&
            mCamcorderProfiles[i]->mQuality == quality) {
            index = i;
            break;
        }
    }
    return index;
}

int MediaProfiles::getCamcorderProfileParamByName(const char *name,
                                                  int cameraId,
                                                  camcorder_quality quality) const
{
    ALOGV("getCamcorderProfileParamByName: %s for camera %d, quality %d",
        name, cameraId, quality);

    int index = getCamcorderProfileIndex(cameraId, quality);
    if (index == -1) {
        ALOGE("The given camcorder profile camera %d quality %d is not found",
            cameraId, quality);
        return -1;
    }

    if (!strcmp("duration", name)) return mCamcorderProfiles[index]->mDuration;
    if (!strcmp("file.format", name)) return mCamcorderProfiles[index]->mFileFormat;
    if (!strcmp("vid.codec", name)) return mCamcorderProfiles[index]->mVideoCodec->mCodec;
    if (!strcmp("vid.width", name)) return mCamcorderProfiles[index]->mVideoCodec->mFrameWidth;
    if (!strcmp("vid.height", name)) return mCamcorderProfiles[index]->mVideoCodec->mFrameHeight;
    if (!strcmp("vid.bps", name)) return mCamcorderProfiles[index]->mVideoCodec->mBitRate;
    if (!strcmp("vid.fps", name)) return mCamcorderProfiles[index]->mVideoCodec->mFrameRate;
    if (!strcmp("aud.codec", name)) return mCamcorderProfiles[index]->mAudioCodec->mCodec;
    if (!strcmp("aud.bps", name)) return mCamcorderProfiles[index]->mAudioCodec->mBitRate;
    if (!strcmp("aud.ch", name)) return mCamcorderProfiles[index]->mAudioCodec->mChannels;
    if (!strcmp("aud.hz", name)) return mCamcorderProfiles[index]->mAudioCodec->mSampleRate;

    ALOGE("The given camcorder profile param id %d name %s is not found", cameraId, name);
    return -1;
}

bool MediaProfiles::hasCamcorderProfile(int cameraId, camcorder_quality quality) const
{
    return (getCamcorderProfileIndex(cameraId, quality) != -1);
}

Vector<int> MediaProfiles::getImageEncodingQualityLevels(int cameraId) const
{
    Vector<int> result;
    ImageEncodingQualityLevels *levels = findImageEncodingQualityLevels(cameraId);
    if (levels != NULL) {
        result = levels->mLevels;  // copy out
    }
    return result;
}

int MediaProfiles::getStartTimeOffsetMs(int cameraId) const {
    int offsetTimeMs = -1;
    ssize_t index = mStartTimeOffsets.indexOfKey(cameraId);
    if (index >= 0) {
        offsetTimeMs = mStartTimeOffsets.valueFor(cameraId);
    }
    ALOGV("offsetTime=%d ms and cameraId=%d", offsetTimeMs, cameraId);
    return offsetTimeMs;
}

MediaProfiles::~MediaProfiles()
{
    CHECK("destructor should never be called" == 0);
#if 0
    for (size_t i = 0; i < mAudioEncoders.size(); ++i) {
        delete mAudioEncoders[i];
    }
    mAudioEncoders.clear();

    for (size_t i = 0; i < mVideoEncoders.size(); ++i) {
        delete mVideoEncoders[i];
    }
    mVideoEncoders.clear();

    for (size_t i = 0; i < mVideoDecoders.size(); ++i) {
        delete mVideoDecoders[i];
    }
    mVideoDecoders.clear();

    for (size_t i = 0; i < mAudioDecoders.size(); ++i) {
        delete mAudioDecoders[i];
    }
    mAudioDecoders.clear();

    for (size_t i = 0; i < mCamcorderProfiles.size(); ++i) {
        delete mCamcorderProfiles[i];
    }
    mCamcorderProfiles.clear();
#endif
}
} // namespace android
