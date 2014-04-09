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
#define LOG_TAG "MediaExtractor"
#include <utils/Log.h>

#include "include/AMRExtractor.h"
#include "include/MP3Extractor.h"
#include "include/MPEG4Extractor.h"
#include "include/FragmentedMP4Extractor.h"
#include "include/WAVExtractor.h"
#include "include/OggExtractor.h"
#include "include/MPEG2PSExtractor.h"
#include "include/MPEG2TSExtractor.h"
#include "include/DRMExtractor.h"
#include "include/WVMExtractor.h"
#include "include/FLACExtractor.h"
#include "include/AACExtractor.h"
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_AUDIO_APE_SUPPORT
#include "include/APEExtractor.h"
#endif
#include "MtkAACExtractor.h"
#include <MtkRTSPController.h>
#endif //#ifndef ANDROID_DEFAULT_CODE
 
#include "matroska/MatroskaExtractor.h"

#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MetaData.h>
#include <utils/String8.h>
#ifndef ANDROID_DEFAULT_CODE 
#include <dlfcn.h>
#ifdef MTK_AVI_PLAYBACK_SUPPORT
#include <MtkAVIExtractor.h>
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
#include <drm/drm_framework_common.h>
#include <drm/DrmManagerClient.h>
#endif

#ifdef MTK_OGM_PLAYBACK_SUPPORT
#include <OgmExtractor.h>
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

namespace android {
#ifndef ANDROID_DEFAULT_CODE  

static bool isKnownType(const char* mime) {
	bool b;
	if (mime == NULL)
		return false;
	b = (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG4)) 
		||(!strcasecmp(mime, "audio/mp4")) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_MPEG)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AMR_NB)) 
        ||(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AMR_WB)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_FLAC)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_WAV)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_OGG)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MATROSKA)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG2TS)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG2PS)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_AVI)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_WVM)) 
		||(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AAC)) 
#ifdef MTK_ASF_PLAYBACK_SUPPORT
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_ASF)) 
#endif
#ifdef MTK_FLV_PLAYBACK_SUPPORT
    	||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_FLV)) 
#endif//#ifdef MTK_FLV_PLAYBACK_SUPPORT
#ifdef MTK_OGM_PLAYBACK_SUPPORT
		||(!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_OGM))
#endif//#ifdef MTK_OGM_PLAYBACK_SUPPORT
		||(!strcasecmp(mime, MEDIA_MIMETYPE_APPLICATION_SDP));

	return b;
}
#ifdef MTK_ASF_PLAYBACK_SUPPORT
#define MTK_ASF_EXTRACTOR_LIB_NAME			"libasfextractor.so"
#define MTK_ASF_EXTRACTOR_FACTORY_NAME		"mtk_asf_extractor_create_instance"
typedef sp<MediaExtractor> AsfFactory_Ptr(const sp<DataSource> &source);
sp<MediaExtractor> ASFExtractorCreateInstance(const sp<DataSource> &source)
{
	bool ret = false;
	void* pAsfLib = NULL;

	pAsfLib = dlopen(MTK_ASF_EXTRACTOR_LIB_NAME, RTLD_NOW);
	if (NULL == pAsfLib) {
		ALOGE ("%s", dlerror());
		return NULL;
	}

	AsfFactory_Ptr* asf_extractor_create_instance = (AsfFactory_Ptr*) dlsym(pAsfLib, MTK_ASF_EXTRACTOR_FACTORY_NAME);
	if (NULL == asf_extractor_create_instance) {
		ALOGE ("%s", dlerror());
		dlclose(pAsfLib);
		return NULL;
	}

	return asf_extractor_create_instance(source);
}
#endif

#ifdef MTK_FLV_PLAYBACK_SUPPORT

#define MTK_FLV_EXTRACTOR_LIB_NAME			"libflvextractor.so"
#define MTK_FLV_EXTRACTOR_FACTORY_NAME		"mtk_flv_extractor_create_instance"
typedef sp<MediaExtractor> FlvFactory_Ptr(const sp<DataSource> &source);
sp<MediaExtractor> FLVExtractorCreateInstance(const sp<DataSource> &source)
{
	bool ret = false;
	void* pFlvLib = NULL;

	pFlvLib = dlopen(MTK_FLV_EXTRACTOR_LIB_NAME, RTLD_NOW);
	if (NULL == pFlvLib) {
		ALOGE ("%s", dlerror());
		return NULL;
	}

	FlvFactory_Ptr* flv_extractor_create_instance = (FlvFactory_Ptr*) dlsym(pFlvLib, MTK_FLV_EXTRACTOR_FACTORY_NAME);
	if (NULL == flv_extractor_create_instance) {
		ALOGE ("%s", dlerror());
		dlclose(pFlvLib);
		return NULL;
	}

	return flv_extractor_create_instance(source);
}

#endif //#ifdef MTK_FLV_PLAYBACK_SUPPORT
#endif // #ifndef ANDROID_DEFAULT_CODE

sp<MetaData> MediaExtractor::getMetaData() {
    return new MetaData;
}

uint32_t MediaExtractor::flags() const {
    return CAN_SEEK_BACKWARD | CAN_SEEK_FORWARD | CAN_PAUSE | CAN_SEEK;
}

// static
sp<MediaExtractor> MediaExtractor::Create(
        const sp<DataSource> &source, const char *mime) {
    sp<AMessage> meta;
    ALOGD("JB +MediaExtractor::Create");
    String8 tmp;
    if (mime == NULL) {
        float confidence;
        if (!source->sniff(&tmp, &confidence, &meta)) {
            ALOGV("FAILED to autodetect media content.");

            return NULL;
        }

        mime = tmp.string();
        ALOGD("Autodetected media content as '%s' with confidence %.2f",
             mime, confidence);
    }

    bool isDrm = false;
    // DRM MIME type syntax is "drm+type+original" where
    // type is "es_based" or "container_based" and
    // original is the content's cleartext MIME type
    if (!strncmp(mime, "drm+", 4)) {
        const char *originalMime = strchr(mime+4, '+');
        if (originalMime == NULL) {
            // second + not found
            return NULL;
        }
        ++originalMime;
        if (!strncmp(mime, "drm+es_based+", 13)) {
            // DRMExtractor sets container metadata kKeyIsDRM to 1
            return new DRMExtractor(source, originalMime);
        } else if (!strncmp(mime, "drm+container_based+", 20)) {
            mime = originalMime;
            isDrm = true;
        } else {
            return NULL;
        }
    }
    // M: add for OMA DRM v1.0 implementation
    // explanation:
    // for an OMA DRM v1.0 file, if it can be sniffed successfully, the mime type
    // would be replaced by the actual type (e.g. image/jpeg) instead of drm+containder_based+<original_mime>
    // Thus, the following code checks for decrypt handle and set isDrm to true
    // so that it's handled correctly in AwesomePlayer.
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
    DrmManagerClient *drmManagerClient = NULL;
    sp<DecryptHandle> decryptHandle;
    source->getDrmInfo(decryptHandle, &drmManagerClient);
    // mark isDrm as true for OMA DRM v1.0 file
    // the same judgement as in FileSource::flags()
    if (decryptHandle.get() != NULL
            && DecryptApiType::CONTAINER_BASED == decryptHandle->decryptApiType) {
        isDrm = true;
    }
#endif
#endif

#ifndef ANDROID_DEFAULT_CODE
	sp<MediaExtractor> ret;
#else
	MediaExtractor *ret = NULL;
#endif
    if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG4)
            || !strcasecmp(mime, "audio/mp4")) {
        int fragmented = 0;
        if (meta != NULL && meta->findInt32("fragmented", &fragmented) && fragmented) {
            ret = new FragmentedMP4Extractor(source);
        } else {
        ret = new MPEG4Extractor(source);
        }
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_MPEG)) {
        ret = new MP3Extractor(source, meta);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AMR_NB)
            || !strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AMR_WB)) {
        ret = new AMRExtractor(source);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_FLAC)) {
        ret = new FLACExtractor(source);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_WAV)) {
        ret = new WAVExtractor(source);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_OGG)) {
        ret = new OggExtractor(source);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MATROSKA)) {
        ret = new MatroskaExtractor(source);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG2TS)) {
        ret = new MPEG2TSExtractor(source);
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG2PS)) {
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_MTKPS_PLAYBACK_SUPPORT
        ret = new MPEG2PSExtractor(source);
#else //MTK_MTKPS_PLAYBACK_SUPPORT
        ALOGV(" MediaExtractor::is PS file, not support playing now");
        ret =NULL;
#endif //MTK_MTKPS_PLAYBACK_SUPPORT
#else  //#ifndef ANDROID_DEFAULT_CODE
        ret = new MPEG2PSExtractor(source);
#endif  //#ifndef ANDROID_DEFAULT_CODE
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_AVI)) {
#ifdef MTK_AVI_PLAYBACK_SUPPORT
        ret = new MtkAVIExtractor(source);
#endif
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_WVM)) {
        // Return now.  WVExtractor should not have the DrmFlag set in the block below.
        return new WVMExtractor(source);
    } 
#ifndef ANDROID_DEFAULT_CODE
	else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AAC)) {
		ret = new MtkAACExtractor(source,meta);
	}
#else
    else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AAC_ADTS)) {
        ret = new AACExtractor(source, meta);
    } 
#endif    
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_ASF_PLAYBACK_SUPPORT
    else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_ASF)) { 
		   ret  = ASFExtractorCreateInstance(source);
    }
#endif
#ifdef MTK_FLV_PLAYBACK_SUPPORT
    else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_FLV)) {
         ret  =  FLVExtractorCreateInstance(source);
    }
#endif//#ifdef MTK_FLV_PLAYBACK_SUPPORT
#ifdef MTK_AUDIO_APE_SUPPORT
	else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_APE)) {
		ret = new APEExtractor(source, meta); 
    }
#endif
    else if (!strcasecmp(mime, MEDIA_MIMETYPE_APPLICATION_SDP)) {
        ret = new MtkSDPExtractor(source);
    }
#ifdef MTK_OGM_PLAYBACK_SUPPORT
    else if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_OGM)) {
        ret = new OgmExtractor(source);
    }
#endif//#ifdef MTK_OGM_PLAYBACK_SUPPORT 
#endif
    if (ret != NULL) {
       if (isDrm) {
           ret->setDrmFlag(true);
       } else {
           ret->setDrmFlag(false);
       }
    }
    ALOGD("JB -MediaExtractor::Create");
    return ret;
}

}  // namespace android
