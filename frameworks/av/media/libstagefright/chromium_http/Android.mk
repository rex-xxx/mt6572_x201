LOCAL_PATH:= $(call my-dir)

ifneq ($(TARGET_BUILD_PDK), true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=       \
        DataUriSource.cpp \
        ChromiumHTTPDataSource.cpp \
        support.cpp \
        chromium_http_stub.cpp

LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        external/chromium \
        external/chromium/android \
		$(MTK_PATH_CUSTOM)/native

LOCAL_CFLAGS += -Wno-multichar


MTK_CUSTOM_UASTRING_FROM_PROPERTY := yes
ifeq ($(strip $(MTK_BSP_PACKAGE)),yes)
	MTK_CUSTOM_UASTRING_FROM_PROPERTY := no
endif
ifeq ($(strip $(MTK_CUSTOM_UASTRING_FROM_PROPERTY)), yes)
LOCAL_CFLAGS += -DCUSTOM_UASTRING_FROM_PROPERTY
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)frameworks/base/custom/inc
LOCAL_WHOLE_STATIC_LIBRARIES += libcustom_prop
endif


LOCAL_SHARED_LIBRARIES += \
        libstlport \
        libchromium_net \
        libutils \
        libcutils \
        libstagefright_foundation \
        libstagefright \
        libdrmframework

include external/stlport/libstlport.mk

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
LOCAL_CFLAGS += -DANDROID_DEFAULT_HTTP_STREAM

else
#LOCAL_CFLAGS += -DHTTP_STREAM_SUPPORT_PROXY
endif

LOCAL_MODULE:= libstagefright_chromium_http

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
endif
