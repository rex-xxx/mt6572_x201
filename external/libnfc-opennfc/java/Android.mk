ifeq ($(MTK_NFC_INSIDE), yes)
# flag to enable OpenNFCExtensions
EXTENDED_OPEN_NFC := true
LOCAL_PATH := $(call my-dir)

############################## JNI for OpenNFCExtensions

$(info "Build JNI for OpenNFCExtensions")

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE	:=	false

LOCAL_MODULE := libnfc_ext_jni
LOCAL_MODULE_TAGS := optional eng

$(info "LOCAL_PATH=$(LOCAL_PATH)")

LOCAL_SRC_FILES		:=	jni/com_opennfc_extension_engine_OpenNFCExtService.c \
						jni/com_opennfc_extension_engine_SecureElementAdapter.c \
						jni/com_opennfc_extension_engine_FirmwareUpdateAdapter.c \
						jni/com_opennfc_extension_engine_CardEmulationAdapter.c \
						jni/com_opennfc_extension_engine_VirtualTagAdapter.c \
						jni/com_opennfc_extension_engine_RoutingTableAdapter.c \
						../../../packages/apps/Nfc/jni_inside/utilities.c
						
LOCAL_C_INCLUDES		:=	$(JNI_H_INCLUDE) \
							$(LOCAL_PATH)/../inc \
							$(LOCAL_PATH)/../ndef \
							$(LOCAL_PATH)/../open_nfc/open_nfc/interfaces \
							$(LOCAL_PATH)/../open_nfc/open_nfc/porting/common \
							$(LOCAL_PATH)/../../../packages/apps/Nfc/jni_inside

LOCAL_SHARED_LIBRARIES	:=	libnativehelper		\
							libcutils			\
							libopen_nfc_client_jni 
#							libutils			

LOCAL_CFLAGS += -O0 -g

include $(BUILD_SHARED_LIBRARY)


############################## OpenNFCExtensions Service
$(info "Building OpenNFCExtensionsService")

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_SUB_PATH			:= src/com/opennfc/extension/engine

#LOCAL_CLASSPATH += $(LOCAL_PATH)/java/src

LOCAL_SRC_FILES := \
        $(call all-java-files-under, $(LOCAL_SUB_PATH))
        
LOCAL_JAVA_LIBRARIES := NfcExt

$(info "LOCAL_SRC_FILES=$(LOCAL_SRC_FILES)")
LOCAL_AIDL_INCLUDES := external/libnfc-opennfc/java/src

LOCAL_PACKAGE_NAME := OpenNfcExtService

LOCAL_JNI_SHARED_LIBRARIES := libnfc_ext_jni
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

endif
