ifeq ($(MTK_MEMORY_COMPRESSION_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := dmlog.c
LOCAL_MODULE := dmlog

include $(BUILD_EXECUTABLE)

endif
