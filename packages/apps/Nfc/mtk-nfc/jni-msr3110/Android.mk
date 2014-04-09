ifeq ($(MTK_NFC_SUPPORT), yes)
#ifeq ($(MTK_NFC_INSIDE), yes)
#MTK_NFC_OPENNFC := true
#endif

#ifeq ($(MTK_NFC_MSR3110), yes)
#MTK_NFC_OPENNFC := true
#endif

#ifeq ($(MTK_NFC_OPENNFC), true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_android_nfc_NativeLlcpConnectionlessSocket.cpp \
    com_android_nfc_NativeLlcpServiceSocket.cpp \
    com_android_nfc_NativeLlcpSocket.cpp \
    com_android_nfc_NativeNfcManager.cpp \
    com_android_nfc_NativeNfcTag.cpp \
    com_android_nfc_NativeP2pDevice.cpp \
    com_android_nfc_NativeNfcSecureElement.cpp \
    com_android_nfc_list.cpp \
    com_android_nfc.cpp \
    utilities.c

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
    external/libnfc-opennfc/open_nfc/open_nfc/interfaces \
    external/libnfc-opennfc/open_nfc/open_nfc/porting/common

LOCAL_SHARED_LIBRARIES := \
    libnativehelper \
    libcutils \
    libutils 
    
ifdef OPENNFC_MOCK_TEST
$(info "Build OPENNFC_MOCK_TEST")
LOCAL_C_INCLUDES += external/libnfc-opennfc-mock/opennfc_mock
LOCAL_CFLAGS += -DOPENNFC_MOCK_TEST
LOCAL_SHARED_LIBRARIES +=  libopennfc_mock_server
else
LOCAL_SHARED_LIBRARIES += libopen_nfc_client_jni 
endif


#LOCAL_CFLAGS += -O0 -g

LOCAL_MODULE := libnfc_msr3110_jni
LOCAL_MODULE_TAGS := optional eng

include $(BUILD_SHARED_LIBRARY)

#endif
endif

