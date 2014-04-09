LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

$(call make-private-dependency,\
  $(BOARD_CONFIG_DIR)/configs/StageFright.mk \
)
include frameworks/av/media/libstagefright/codecs/common/Config.mk

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE

else

#ifeq ($(strip $(MTK_USES_VR_DYNAMIC_QUALITY_MECHANISM)),yes)
#LOCAL_CFLAGS += -DMTK_USES_VR_DYNAMIC_QUALITY_MECHANISM
#endif

endif

LOCAL_MTK_PATH:=../../../../mediatek/frameworks-ext/av/media/libstagefright

LOCAL_SRC_FILES:=                         \
        ACodec.cpp                        \
        AACExtractor.cpp                  \
        AACWriter.cpp                     \
        AMRExtractor.cpp                  \
        AMRWriter.cpp                     \
        AudioPlayer.cpp                   \
        AudioSource.cpp                   \
        AwesomePlayer.cpp                 \
        CameraSource.cpp                  \
        CameraSourceTimeLapse.cpp         \
        DataSource.cpp                    \
        DRMExtractor.cpp                  \
        ESDS.cpp                          \
        FileSource.cpp                    \
        FragmentedMP4Extractor.cpp        \
        HTTPBase.cpp                      \
        JPEGSource.cpp                    \
        MP3Extractor.cpp                  \
        MPEG2TSWriter.cpp                 \
        MPEG4Extractor.cpp                \
        MPEG4Writer.cpp                   \
        MediaBuffer.cpp                   \
        MediaBufferGroup.cpp              \
        MediaCodec.cpp                    \
        MediaCodecList.cpp                \
        MediaDefs.cpp                     \
        MediaExtractor.cpp                \
        MediaSource.cpp                   \
        MetaData.cpp                      \
        NuCachedSource2.cpp               \
        NuMediaExtractor.cpp              \
        OMXClient.cpp                     \
        OMXCodec.cpp                      \
        OggExtractor.cpp                  \
        SampleIterator.cpp                \
        SampleTable.cpp                   \
        SkipCutBuffer.cpp                 \
        StagefrightMediaScanner.cpp       \
        StagefrightMetadataRetriever.cpp  \
        SurfaceMediaSource.cpp            \
        ThrottledSource.cpp               \
        TimeSource.cpp                    \
        TimedEventQueue.cpp               \
        Utils.cpp                         \
        VBRISeeker.cpp                    \
        WAVExtractor.cpp                  \
        WVMExtractor.cpp                  \
        XINGSeeker.cpp                    \
        avc_utils.cpp                     \
        mp4/FragmentedMP4Parser.cpp       \
        mp4/TrackFragment.cpp             \
    	$(LOCAL_MTK_PATH)/OggWriter.cpp   \
	$(LOCAL_MTK_PATH)/PCMWriter.cpp   \

ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)

ifeq ($(strip $(MTK_S3D_SUPPORT)),yes)
LOCAL_CFLAGS += -DMTK_S3D_SUPPORT
endif

LOCAL_SRC_FILES += \
        $(LOCAL_MTK_PATH)/MtkFLACExtractor.cpp

ifeq ($(MULTI_CH_PLAYBACK_SUPPORT),yes)
  LOCAL_CFLAGS += -DFLAC_MULTI_CH_SUPPORT
endif

ifeq ($(MTK_AUDIO_APE_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_AUDIO_APE_SUPPORT

LOCAL_SRC_FILES += \
        $(LOCAL_MTK_PATH)/APEExtractor.cpp \
        $(LOCAL_MTK_PATH)/apetag.cpp

endif
else
LOCAL_SRC_FILES += \
        FLACExtractor.cpp
endif


ifeq ($(MTK_SWIP_VORBIS),yes)
  LOCAL_CFLAGS += -DUSE_MTK_DECODER
ifeq ($(MULTI_CH_PLAYBACK_SUPPORT),yes)
  LOCAL_CFLAGS += -DVORBIS_MULTI_CH_SUPPORT
endif
endif

ifeq ($(MTK_AUDIO_ADPCM_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_AUDIO_ADPCM_SUPPORT
endif

LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/include/media/stagefright/timedtext \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/media/libstagefright/include \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/include \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/include \
        $(TOP)/frameworks/native/include/media/hardware \
        $(TOP)/external/tremolo \
        $(MTK_PATH_SOURCE)/frameworks/av/media/libstagefright/include \
        $(TOP)/mediatek/frameworks/av/media/libstagefright/include/omx_core \
        $(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright \
        $(TOP)/external/openssl/include \
        $(TOP)/frameworks/av/media/libstagefright/include/omx_core \
        $(TOP)/frameworks/av/media/libstagefright/include \
        $(TOP)/mediatek/frameworks-ext/av/media/libstagefright/include \
        $(MTK_PATH_SOURCE)/kernel/include \
        $(TOP)/external/skia/include/images \
        $(TOP)/external/skia/include/core \

ifeq ($(TARGET_ARCH), arm)         
LOCAL_C_INCLUDES += $(TOP)/mediatek/external/flacdec/include
else
LOCAL_C_INCLUDES += $(TOP)/external/flac/include
endif

LOCAL_SHARED_LIBRARIES := \
        libbinder \
        libcamera_client \
        libcrypto \
        libcutils \
        libdl \
        libdrmframework \
        libexpat \
        libgui \
        libicui18n \
        libicuuc \
        liblog \
        libmedia \
        libmedia_native \
        libsonivox \
        libssl \
        libstagefright_omx \
        libstagefright_yuv \
        libsync \
        libui \
        libutils \
        libvorbisidec \
        libz \

LOCAL_STATIC_LIBRARIES := \
        libstagefright_color_conversion \
        libstagefright_aacenc \
        libstagefright_matroska \
        libstagefright_timedtext \
        libvpx \
        libstagefright_mpeg2ts \
        libstagefright_httplive \
        libstagefright_id3 \

ifeq ($(TARGET_ARCH), arm)         
LOCAL_STATIC_LIBRARIES += libflacdec_mtk
else
LOCAL_STATIC_LIBRARIES += libFLAC
endif

LOCAL_SRC_FILES += \
        chromium_http_stub.cpp
ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_SRC_FILES += \
	$(LOCAL_MTK_PATH)/TableOfContentThread.cpp \
	$(LOCAL_MTK_PATH)/MtkAACExtractor.cpp \
	$(LOCAL_MTK_PATH)/MMReadIOThread.cpp
endif
LOCAL_SHARED_LIBRARIES += libskia
LOCAL_CPPFLAGS += -DCHROMIUM_AVAILABLE=1

LOCAL_SHARED_LIBRARIES += libstlport
include external/stlport/libstlport.mk

LOCAL_SHARED_LIBRARIES += \
        libstagefright_enc_common \
        libstagefright_avc_common \
        libstagefright_foundation \
        libdl

ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)        
#ifeq ($(MTK_PLATFORM),$(filter $(MTK_PLATFORM),MT6575 MT6577))
#LOCAL_SHARED_LIBRARIES += \
#    libmhal
#endif  
#ifeq ($(MTK_PLATFORM),$(filter $(MTK_PLATFORM),MT6589))
LOCAL_SHARED_LIBRARIES += \
    libdpframework
#endif  
endif
ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)      
ifeq ($(strip $(HAVE_ADPCMENCODE_FEATURE)),yes)
    LOCAL_CFLAGS += -DHAVE_ADPCMENCODE_FEATURE
    LOCAL_SRC_FILES += \
        $(LOCAL_MTK_PATH)/ADPCMWriter.cpp 
endif
endif

ifeq ($(strip $(HAVE_AWBENCODE_FEATURE)),yes)
    LOCAL_CFLAGS += -DHAVE_AWBENCODE
endif

ifeq ($(strip $(HAVE_AACENCODE_FEATURE)),yes)
    LOCAL_CFLAGS += -DHAVE_AACENCODE
endif

###
# 	ANDROID_DEFAULT_HTTP_STREAM is used to check android default http streaming
# 	how to: LOCAL_CFLAGS += ANDROID_DEFAULT_HTTP_STREAM 
# 	notice: if ANDROID_DEFAULT_CODE define, ANDROID_DEFAULT_HTTP_STREAM must be defined
ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
LOCAL_CFLAGS += -DANDROID_DEFAULT_HTTP_STREAM
else
LOCAL_STATIC_LIBRARIES += libstagefright_rtsp
#LOCAL_CFLAGS += -DANDROID_DEFAULT_HTTP_STREAM
ifeq ($(strip $(MTK_S3D_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_S3D_SUPPORT
endif
ifeq ($(strip $(MTK_FLV_PLAYBACK_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_FLV_PLAYBACK_SUPPORT
endif
ifeq ($(strip $(MTK_RMVB_PLAYBACK_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_RMVB_PLAYBACK_SUPPORT
endif
ifeq ($(strip $(MTK_ASF_PLAYBACK_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_ASF_PLAYBACK_SUPPORT
endif
ifeq ($(strip $(MTK_ASF_PLAYBACK_SUPPORT)), no)
	LOCAL_CFLAGS += -DMTK_REMOVE_WMA_COMPONENT
endif
ifeq ($(strip $(MTK_AVI_PLAYBACK_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_AVI_PLAYBACK_SUPPORT
	LOCAL_SRC_FILES += $(LOCAL_MTK_PATH)/MtkAVIExtractor.cpp
endif
ifeq ($(strip $(MTK_OGM_PLAYBACK_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_OGM_PLAYBACK_SUPPORT
	LOCAL_SRC_FILES += $(LOCAL_MTK_PATH)/OgmExtractor.cpp
endif
ifeq ($(strip $(HAVE_XLOG_FEATURE)),yes)
	LOCAL_CFLAGS += -DMTK_STAGEFRIGHT_USE_XLOG
endif
ifeq ($(strip $(MTK_MTKPS_PLAYBACK_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_MTKPS_PLAYBACK_SUPPORT
endif
ifeq ($(strip $(MTK_AUDIO_HD_REC_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_AUDIO_HD_REC_SUPPORT
endif

endif
ifeq ($(strip $(MTK_HIGH_QUALITY_THUMBNAIL)),yes)
LOCAL_CFLAGS += -DMTK_HIGH_QUALITY_THUMBNAIL
endif
ifneq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
LOCAL_SHARED_LIBRARIES += libstagefright_memutil
endif
ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_C_INCLUDES += \
	$(TOP)/external/aac/libAACdec/include \
	$(TOP)/external/aac/libPCMutils/include \
	$(TOP)/external/aac/libFDK/include \
	$(TOP)/external/aac/libMpegTPDec/include \
	$(TOP)/external/aac/libSBRdec/include \
	$(TOP)/external/aac/libSYS/include

LOCAL_STATIC_LIBRARIES += libFraunhoferAAC
LOCAL_CFLAGS += -DUSE_FRAUNHOFER_AAC
endif

LOCAL_CFLAGS += -Wno-multichar

ifeq ($(strip $(MTK_HIGH_QUALITY_THUMBNAIL)),yes)
LOCAL_CFLAGS += -DMTK_HIGH_QUALITY_THUMBNAIL
endif

ifeq ($(strip $(MTK_DRM_APP)),yes)
    LOCAL_CFLAGS += -DMTK_DRM_APP
    LOCAL_C_INCLUDES += \
        $(TOP)/mediatek/frameworks/av/include
    LOCAL_SHARED_LIBRARIES += \
        libdrmmtkutil
endif

ifeq ($(HAVE_CMMB_FEATURE), yes)
  LOCAL_CFLAGS += -DMTK_CMMB_ENABLE
  LOCAL_C_INCLUDES += \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/include/cmmb \
        $(TOP)/mediatek/frameworks/base/cmmb/include \
        $(TOP)/mediatek/frameworks/base/cmmb/inc
  LOCAL_STATIC_LIBRARIES += \
        libcmmbsource
  LOCAL_SHARED_LIBRARIES += \
        libcmmbsp
endif

ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
ifeq ($(strip $(MTK_USES_VR_DYNAMIC_QUALITY_MECHANISM)),yes)
LOCAL_CFLAGS += -DMTK_USES_VR_DYNAMIC_QUALITY_MECHANISM
LOCAL_C_INCLUDES += \
			$(TOP)/$(MTK_PATH_CUSTOM)/native/vr
endif


#add for mmprofile code        
ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)

LOCAL_C_INCLUDES += \
	 $(MTK_PATH_SOURCE)/kernel/include \
        $(TOPDIR)/kernel/include \
        $(TOPDIR)/system/core/include \

LOCAL_SHARED_LIBRARIES += \
           libmmprofile
           
           
LOCAL_CFLAGS += -DMMPROFILE_HTTP

endif   

ifeq ($(HAVE_AEE_FEATURE),yes)
LOCAL_SHARED_LIBRARIES += libaed
LOCAL_C_INCLUDES += $(MTK_ROOT)/external/aee/binary/inc
LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif

endif

LOCAL_MODULE:= libstagefright

#ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),no)
#BYPASS_NONNDK_BUILD := no
#include $(MTK_PATH_SOURCE)external/nonNDK/nonndk.mk
#endif

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
