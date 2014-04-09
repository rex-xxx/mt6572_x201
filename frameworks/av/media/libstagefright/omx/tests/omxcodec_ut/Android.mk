LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES =       \
	OMXCodec_ut.cpp  

LOCAL_SHARED_LIBRARIES := \
	libstagefright libstagefright_foundation libbinder libmedia libutils libstlport libui libgui

LOCAL_STATIC_LIBRARIES := libgtest

LOCAL_C_INCLUDES:= \
	$(JNI_H_INCLUDE) \
	frameworks/base/media/libstagefright \
	$(TOP)/frameworks/native/include/media/openmax \
	$(TOP)/frameworks/av/include \
	$(TOP)/frameworks/native/include/gui/ \
	$(TOP)/frameworks/native/include/ \
	bionic \
	external/stlport/stlport \
	external/gtest/include

ifeq ($(strip $(MTK_SUPPORT_MJPEG)), yes)
LOCAL_CFLAGS += -DMTK_SUPPORT_MJPEG
endif

LOCAL_MODULE:= omxcodec_ut

include $(BUILD_EXECUTABLE)
