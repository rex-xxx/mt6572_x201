
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Module name should match lib name
LOCAL_MODULE := libcocosdenshionocem.so
LOCAL_MODULE_TAGS := optioanl
LOCAL_MODULE_CLASS := SHARED_LIBARARY
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib

include $(BUILD_PREBUILT)

