LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(strip $(BOARD_USES_GENERIC_AUDIO)),true)
  LOCAL_CFLAGS += -DGENERIC_AUDIO
endif

LOCAL_SRC_FILES := \
   AudioMeta.cpp

LOCAL_C_INCLUDES := \
   $(MTK_PATH_PLATFORM)/hardware/audio/aud_drv \
   $(MTK_PATH_PLATFORM)/hardware/audio/LAD \
   $(MTK_PATH_PLATFORM)/hardware/audio \
   $(MTK_PATH_SOURCE)/external/audiocustparam \
   $(MTK_PATH_SOURCE)/frameworks/base/include/media \
   $(MTK_PATH_SOURCE)/external/meta/common/inc \
   $(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
   $(MTK_PATH_SOURCE)/external/HeadphoneCompensationFilter \
   frameworks/base/include/media \
   system/core/include/cutils \
   $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
   $(MTK_PATH_CUSTOM)/cgen/cfgdefault \
   $(MTK_PATH_CUSTOM)/cgen/inc \
   $(MTK_PATH_CUSTOM)/hal/audioflinger/audio

ifeq ($(MTK_PLATFORM),MT6575)
  LOCAL_CFLAGS += -DMT6575
endif


LOCAL_MODULE := libmeta_audio

LOCAL_SHARED_LIBRARIES := \
    libft

LOCAL_PRELINK_MODULE := false

include $(BUILD_STATIC_LIBRARY)

###############################################################################
# SELF TEST
###############################################################################
BUILD_SELF_TEST := false
# BUILD_SELF_TEST := true

ifeq ($(BUILD_SELF_TEST), true)
include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  AudioMeta.cpp

LOCAL_C_INCLUDES := $(MTK_PATH_SOURCE)/external/meta/common/inc
LOCAL_MODULE := AudioMeta_Test
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_SHARED_LIBRARIES := libmeta_audio
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_SBIN_UNSTRIPPED)
include $(BUILD_EXECUTABLE)
endif

