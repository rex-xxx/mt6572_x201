# Copyright 2006 The Android Open Source Project

LOCAL_PATH := $(call my-dir)
include $(BUILD_PREBUILD)

############################
include $(CLEAR_VARS)

LOCAL_CFLAGS := -fshort-enums

LOCAL_SRC_FILES := SA_Phys_Linux.c Whisper_AccyMain.c SHA_Comm.c SHA_CommInterfaceTemplate.c SHA_CommMarshalling.c SHA_TimeUtilsLoop.c

LOCAL_C_INCLUDES := \
	hardware/libhardware_legacy/include

LOCAL_SHARED_LIBRARIES := liblog libcutils libhardware_legacy
LOCAL_MODULE_PATH := $(TARGET_OUT)/bin
LOCAL_MODULE := whisperd
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

#########################
