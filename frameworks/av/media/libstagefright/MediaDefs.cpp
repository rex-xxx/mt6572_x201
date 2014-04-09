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

#include <media/stagefright/MediaDefs.h>

namespace android {

const char *MEDIA_MIMETYPE_IMAGE_JPEG = "image/jpeg";

const char *MEDIA_MIMETYPE_VIDEO_VPX = "video/x-vnd.on2.vp8";
const char *MEDIA_MIMETYPE_VIDEO_AVC = "video/avc";
const char *MEDIA_MIMETYPE_VIDEO_MPEG4 = "video/mp4v-es";
const char *MEDIA_MIMETYPE_VIDEO_H263 = "video/3gpp";
const char *MEDIA_MIMETYPE_VIDEO_MPEG2 = "video/mpeg2";
const char *MEDIA_MIMETYPE_VIDEO_RAW = "video/raw";

const char *MEDIA_MIMETYPE_VIDEO_DIVX = "video/divx";
const char *MEDIA_MIMETYPE_VIDEO_DIVX3 = "video/divx3";
const char *MEDIA_MIMETYPE_VIDEO_XVID = "video/xvid";
const char *MEDIA_MIMETYPE_AUDIO_AMR_NB = "audio/3gpp";
const char *MEDIA_MIMETYPE_AUDIO_AMR_WB = "audio/amr-wb";
const char *MEDIA_MIMETYPE_AUDIO_MPEG = "audio/mpeg";
const char *MEDIA_MIMETYPE_AUDIO_MPEG_LAYER_I = "audio/mpeg-L1";
const char *MEDIA_MIMETYPE_AUDIO_MPEG_LAYER_II = "audio/mpeg-L2";
const char *MEDIA_MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
const char *MEDIA_MIMETYPE_AUDIO_QCELP = "audio/qcelp";
const char *MEDIA_MIMETYPE_AUDIO_VORBIS = "audio/vorbis";
const char *MEDIA_MIMETYPE_AUDIO_G711_ALAW = "audio/g711-alaw";
const char *MEDIA_MIMETYPE_AUDIO_G711_MLAW = "audio/g711-mlaw";
const char *MEDIA_MIMETYPE_AUDIO_RAW = "audio/raw";
const char *MEDIA_MIMETYPE_AUDIO_FLAC = "audio/flac";
const char *MEDIA_MIMETYPE_AUDIO_AAC_ADTS = "audio/aac-adts";

const char *MEDIA_MIMETYPE_CONTAINER_MPEG4 = "video/mp4";
const char *MEDIA_MIMETYPE_CONTAINER_WAV = "audio/x-wav";
const char *MEDIA_MIMETYPE_CONTAINER_OGG = "application/ogg";
const char *MEDIA_MIMETYPE_CONTAINER_MATROSKA = "video/x-matroska";
const char *MEDIA_MIMETYPE_CONTAINER_MPEG2TS = "video/mp2ts";
const char *MEDIA_MIMETYPE_CONTAINER_AVI = "video/avi";
const char *MEDIA_MIMETYPE_CONTAINER_MPEG2PS = "video/mp2p";

const char *MEDIA_MIMETYPE_CONTAINER_WVM = "video/wvm";

const char *MEDIA_MIMETYPE_TEXT_3GPP = "text/3gpp-tt";
const char *MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";

#ifndef ANDROID_DEFAULT_CODE  
const char *MEDIA_MIMETYPE_APPLICATION_SDP = "application/sdp";
  
#ifdef MTK_ASF_PLAYBACK_SUPPORT
const char *MEDIA_MIMETYPE_CONTAINER_ASF = "video/asfff";
#endif

const char *MEDIA_MIMETYPE_VIDEO_MJPEG = "video/x-motion-jpeg";
const char *MEDIA_MIMETYPE_VIDEO_WMV = "video/x-ms-wmv";
const char *MEDIA_MIMETYPE_AUDIO_WMA = "audio/x-ms-wma";  //maybe other contains with this codec

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_AUDIO_APE_SUPPORT
const char *MEDIA_MIMETYPE_AUDIO_APE = "audio/ape";
#endif
#endif // #ifndef ANDROID_DEFAULT_CODE

#ifdef MTK_FLV_PLAYBACK_SUPPORT
const char *MEDIA_MIMETYPE_CONTAINER_FLV = "video/x-flv";
#endif //#ifdef MTK_FLV_PLAYBACK_SUPPORT

#ifdef MTK_OGM_PLAYBACK_SUPPORT
const char *MEDIA_MIMETYPE_CONTAINER_OGM = "video/ogm";
#endif //#ifdef MTK_OGM_PLAYBACK_SUPPORT 

const char *MEDIA_MIMETYPE_VIDEO_FLV = "video/x-flv";
const char *MEDIA_MIMETYPE_AUDIO_FLV = "audio/x-flv"; //maybe other contains with this codec
#ifndef ANDROID_DEFAULT_CODE
#if defined(MTK_AUDIO_ADPCM_SUPPORT) || defined(HAVE_ADPCMENCODE_FEATURE)  
const char *MEDIA_MIMETYPE_AUDIO_MS_ADPCM = "audio/x-adpcm-ms";
const char *MEDIA_MIMETYPE_AUDIO_DVI_IMA_ADPCM = "audio/x-adpcm-dvi-ima";
#endif
#endif

#endif // #ifndef ANDROID_DEFAULT_CODE
}  // namespace android
