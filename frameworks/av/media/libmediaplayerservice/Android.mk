LOCAL_PATH:= $(call my-dir)

#
# libmediaplayerservice
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=               \
    ActivityManager.cpp         \
    Crypto.cpp                  \
    HDCP.cpp                    \
    MediaPlayerFactory.cpp      \
    MediaPlayerService.cpp      \
    MediaRecorderClient.cpp     \
    MetadataRetrieverClient.cpp \
    MidiFile.cpp                \
    MidiMetadataRetriever.cpp   \
    RemoteDisplay.cpp           \
    StagefrightPlayer.cpp       \
    StagefrightRecorder.cpp     \
    TestPlayerStub.cpp          \

LOCAL_SHARED_LIBRARIES :=       \
    libbinder                   \
    libcamera_client            \
    libcutils                   \
    libdl                       \
    libgui                      \
    libmedia                    \
    libmedia_native             \
    libsonivox                  \
    libstagefright              \
    libstagefright_foundation   \
    libstagefright_omx          \
    libstagefright_wfd          \
    libutils                    \
    libvorbisidec               \

LOCAL_STATIC_LIBRARIES := \
        libstagefright_nuplayer                 \
        libstagefright_rtsp                     \


LOCAL_C_INCLUDES :=                                               \
	$(call include-path-for, graphics corecg)                       \
	$(TOP)/frameworks/av/media/libstagefright/include               \
	$(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/include             \
	$(TOP)/frameworks/av/media/libstagefright/rtsp                  \
	$(TOP)/frameworks/native/include/media/openmax                  \
	$(TOP)/external/tremolo/Tremolo                                 


ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_C_INCLUDES+= \
	$(TOP)/frameworks/av/media/libstagefright/wifi-display
else
LOCAL_C_INCLUDES+= \
	$(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/wifi-display-mediatek
endif


ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
else
LOCAL_C_INCLUDES+= \
	$(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libmediaplayerservice  \
  $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/media/libs                       \
  $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/include                          \
  $(TOP)/$(MTK_PATH_SOURCE)/kernel/include                                 \
  $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/include  

LOCAL_SHARED_LIBRARIES += libhardware_legacy
#ifeq ($(USE_NOTIFYSENDER),yes)  
LOCAL_MTK_PATH:=../../../../mediatek/frameworks-ext/av/media/libmediaplayerservice
LOCAL_SRC_FILES+= \
  $(LOCAL_MTK_PATH)/NotifySender.cpp
  LOCAL_CFLAGS += -DNOTIFYSENDER_ENABLE
#endif

ifeq ($(HAVE_MATV_FEATURE),yes)
  LOCAL_CFLAGS += -DMTK_MATV_ENABLE
endif	
ifeq ($(MTK_FM_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_FM_ENABLE
endif

ifeq ($(HAVE_ADPCMENCODE_FEATURE),yes)
  LOCAL_CFLAGS += -DHAVE_ADPCMENCODE_FEATURE
endif
ifneq ($(strip $(HAVE_MATV_FEATURE))_$(strip $(MTK_FM_SUPPORT)), no_no)
  LOCAL_SHARED_LIBRARIES += libmtkplayer
ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)  
  LOCAL_C_INCLUDES+= \
    $(TOP)/frameworks/av/include
endif
endif
	
ifeq ($(strip $(MTK_AUDIO_HD_REC_SUPPORT)), yes)
	LOCAL_CFLAGS += -DMTK_AUDIO_HD_REC_SUPPORT
endif

ifeq ($(HAVE_CMMB_FEATURE),yes)
  LOCAL_CFLAGS += -DMTK_CMMB_ENABLE
endif

ifeq ($(strip $(MTK_DRM_APP)),yes)
    LOCAL_CFLAGS += -DMTK_DRM_APP
    LOCAL_C_INCLUDES += \
        $(TOP)/mediatek/frameworks/av/include
    LOCAL_SHARED_LIBRARIES += \
        libdrmmtkutil
endif

ifeq ($(strip $(MTK_USES_VR_DYNAMIC_QUALITY_MECHANISM)),yes)
LOCAL_CFLAGS += -DMTK_USES_VR_DYNAMIC_QUALITY_MECHANISM
endif

ifeq ($(HAVE_AEE_FEATURE),yes)
LOCAL_SHARED_LIBRARIES += libaed
LOCAL_C_INCLUDES += $(MTK_ROOT)/external/aee/binary/inc
LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif

LOCAL_C_INCLUDES += $(TOP)/mediatek/kernel/include/linux/vcodec 
LOCAL_SHARED_LIBRARIES += \
        libvcodecdrv
endif
   
LOCAL_MODULE:= libmediaplayerservice

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
