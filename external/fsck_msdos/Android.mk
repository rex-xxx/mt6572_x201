LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := mediatek_enhancement.c boot.c check.c dir.c fat.c main.c

LOCAL_C_INCLUDES := external/fsck_msdos/

LOCAL_CFLAGS := -O2 -g -W -Wall -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64

LOCAL_MODULE := fsck_msdos
LOCAL_MODULE_TAGS :=
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := libcutils
include $(BUILD_EXECUTABLE)
