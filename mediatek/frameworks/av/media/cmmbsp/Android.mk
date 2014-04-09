ifeq ($(HAVE_CMMB_FEATURE), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	main_cmmbsp.cpp 

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libbinder

LOCAL_CFLAGS += -DMTK_CMMBSP_SUPPORT
LOCAL_SHARED_LIBRARIES += libdl

#base := $(LOCAL_PATH)/../..

LOCAL_C_INCLUDES := \
    $(TOP)/$(MTK_PATH_SOURCE)/frameworks/base/include \
	$(TOP)/mediatek/frameworks/base/cmmb/include  

LOCAL_MODULE:= cmmbsp

include $(BUILD_EXECUTABLE)

endif