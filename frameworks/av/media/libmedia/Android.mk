LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
  LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
endif

LOCAL_SRC_FILES:= \
    AudioParameter.cpp
LOCAL_MODULE:= libmedia_helper
LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MTK_PATH:=../../../../mediatek/frameworks-ext/av/media/libmedia

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
  LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
endif

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
  LOCAL_CFLAGS += -DMTK_AUDIO
else
  LOCAL_CFLAGS += -DGENERIC_AUDIO
endif

LOCAL_CFLAGS += -DVOLUME_NEWMAP

LOCAL_SRC_FILES:= \
    AudioTrack.cpp \
    IAudioFlinger.cpp \
    IAudioFlingerClient.cpp \
    IAudioTrack.cpp \
    IAudioRecord.cpp \
    ICrypto.cpp \
    IHDCP.cpp \
    AudioRecord.cpp \
    AudioSystem.cpp \
    mediaplayer.cpp \
    IMediaPlayerService.cpp \
    IMediaPlayerClient.cpp \
    IMediaRecorderClient.cpp \
    IMediaPlayer.cpp \
    IMediaRecorder.cpp \
    IRemoteDisplay.cpp \
    IRemoteDisplayClient.cpp \
    IStreamSource.cpp \
    Metadata.cpp \
    mediarecorder.cpp \
    IMediaMetadataRetriever.cpp \
    mediametadataretriever.cpp \
    ToneGenerator.cpp \
    JetPlayer.cpp \
    IOMX.cpp \
    IAudioPolicyService.cpp \
    MediaScanner.cpp \
    MediaScannerClient.cpp \
    autodetect.cpp \
    IMediaDeathNotifier.cpp \
    MediaProfiles.cpp \
    IEffect.cpp \
    IEffectClient.cpp \
    AudioEffect.cpp \
    Visualizer.cpp \
    MemoryLeakTrackUtil.cpp \
    SoundPool.cpp \
    SoundPoolThread.cpp    

LOCAL_SRC_FILES+= \
    $(LOCAL_MTK_PATH)/AudioPCMxWay.cpp \
    $(LOCAL_MTK_PATH)/ATVCtrl.cpp \
    $(LOCAL_MTK_PATH)/IATVCtrlClient.cpp \
    $(LOCAL_MTK_PATH)/IATVCtrlService.cpp

LOCAL_SHARED_LIBRARIES := \
	libui libcutils libutils libbinder libsonivox libicuuc libexpat \
        libcamera_client libstagefright_foundation \
        libgui libdl libaudioutils libmedia_native

LOCAL_STATIC_LIBRARIES += \
        libmedia_helper

ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_SHARED_LIBRARIES += \
        libvcodecdrv     
else
    LOCAL_CFLAGS += -DANDROID_DEFAULT_PROFILE    
endif
        
LOCAL_WHOLE_STATIC_LIBRARY := libmedia_helper

LOCAL_MODULE:= libmedia

LOCAL_C_INCLUDES := \
    $(call include-path-for, graphics corecg) \
    $(TOP)/frameworks/native/include/media/openmax \
    $(TOP)/mediatek/external/mhal/src/core/drv/inc \
    $(TOP)/mediatek/kernel/include/linux \
    $(TOP)/mediatek/kernel/include/linux/vcodec \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/vcodec/inc \
    $(TOP)/$(MTK_PATH_SOURCE)/kernel/include \
    external/icu4c/common \
    $(call include-path-for, audio-effects) \
    $(call include-path-for, audio-utils) \
    $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/include \
    $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/include \

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
  LOCAL_CFLAGS += -DMTK_AUDIO
  LOCAL_C_INCLUDES+= \
   $(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio/aud_drv \
   $(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio/include \
   $(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio \
   $(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio/aud_drv \
   $(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio/include \
   $(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio
endif    

ifeq ($(strip $(MTK_AUDIO_GAIN_TABLE_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_AUDIO_GAIN_TABLE
endif

ifeq ($(strip $(HAVE_AACENCODE_FEATURE)),yes)
    LOCAL_CFLAGS += -DHAVE_AACENCODE
endif

ifeq ($(strip $(MTK_AUDIO_HD_REC_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_AUDIO_HD_REC_SUPPORT
endif

ifeq ($(HAVE_CMMB_FEATURE),yes)
  LOCAL_CFLAGS += -DMTK_CMMB_ENABLE
endif

include $(BUILD_SHARED_LIBRARY)

