LOCAL_PATH := $(my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES:= \
    $(MTK_PATH_SOURCE)/external/nvram/libnvram \
    $(TOP)/mediatek/custom/$(MTK_PROJECT)/cgen/inc

LOCAL_SRC_FILES := \
    AudioCustParam.cpp

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libnvram

# Audio HD Record
ifeq ($(MTK_AUDIO_HD_REC_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_AUDIO_HD_REC_SUPPORT
endif
# Audio HD Record

ifeq ($(strip $(MTK_PLATFORM)),MT6582)
  LOCAL_CFLAGS += -DMT6582
endif
   
LOCAL_MODULE := libaudiocustparam

LOCAL_PRELINK_MODULE := false

LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)
