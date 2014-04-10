LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_mediatek_nfcdemo_NativeNfcManager.cpp \
    com_mediatek_nfcdemo.cpp 
    

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
    com_mediatek_nfcdemo.h 

LOCAL_SHARED_LIBRARIES := \
    libnativehelper \
    libcutils \
    libutils \

#LOCAL_CFLAGS += -O0 -g

LOCAL_MODULE := libnfcdemo
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

