ifneq ($(BUILD_TINY_ANDROID),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    AudioPolicyManager.cpp

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils

LOCAL_STATIC_LIBRARIES := \
    libmedia_helper

LOCAL_WHOLE_STATIC_LIBRARIES := \
    libaudiopolicy_legacy

LOCAL_MODULE := audio_policy.stingray
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE := audio.primary.stingray
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libhardware_legacy

LOCAL_SHARED_LIBRARIES += libdl

LOCAL_SRC_FILES += \
    AudioHardware.cpp

LOCAL_C_INCLUDES += \
    $(call include-path-for, audio-effects)

LOCAL_CFLAGS += -fno-short-enums

LOCAL_STATIC_LIBRARIES := \
    libmedia_helper

LOCAL_WHOLE_STATIC_LIBRARIES := \
    libaudiohw_legacy

ifeq ($(USE_PROPRIETARY_AUDIO_EXTENSIONS),true)
LOCAL_SRC_FILES += AudioPostProcessor.cpp
LOCAL_STATIC_LIBRARIES += \
    libEverest_motomm-r \
    libCortexA9_aie-r \
    libCortexA9_sas-r \
    libCortexA9_se-r \
    libCortexA9_motovoice-r \
    libCortexA9_ecns-r \
    libsamplerateconverter \
    libCortexA9_anm-r

LOCAL_CFLAGS += -DUSE_PROPRIETARY_AUDIO_EXTENSIONS
LOCAL_C_INCLUDES += vendor/moto/stingray/motomm/ghdr
LOCAL_C_INCLUDES += vendor/moto/stingray/motomm/rate_conv
endif

include $(BUILD_SHARED_LIBRARY)

endif # not BUILD_TINY_ANDROID

