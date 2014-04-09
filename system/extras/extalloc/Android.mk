# Copyright (C) 2008 The Android Open Source Project
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
               
ifeq ($(MTK_INTERNAL), yes)
ifeq ($(MTK_USE_RESERVED_EXT_MEM), yes)
#LOCAL_CFLAGS += -DMTK_USE_RESERVED_EXT_MEM
endif                
endif
               
LOCAL_SRC_FILES := extalloc.c
LOCAL_SHARED_LIBRARIES += libcutils libutils
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libextalloc

include $(BUILD_SHARED_LIBRARY)
