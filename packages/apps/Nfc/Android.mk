ifeq ($(MTK_NFC_SUPPORT), yes)
LOCAL_PATH:= $(call my-dir)

########################################
# MTK Single-Load NFC Configuration
########################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_SRC_FILES += \
        $(call all-java-files-under, mtk-nfc/src)

LOCAL_PACKAGE_NAME := Nfc
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := NfcLogTags

LOCAL_JNI_SHARED_LIBRARIES := libnfc_msr3110_jni libnfc_mt6605_jni libmtknfc_dynamic_load_jni

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

########################################
# NXP Configuration
########################################
#include $(CLEAR_VARS)
#
#LOCAL_MODULE_TAGS := optional
#
#LOCAL_SRC_FILES := \
#        $(call all-java-files-under, src)
#
#LOCAL_SRC_FILES += \
#        $(call all-java-files-under, nxp)
#
#LOCAL_PACKAGE_NAME := Nfc
#LOCAL_CERTIFICATE := platform
#
#LOCAL_STATIC_JAVA_LIBRARIES := NfcLogTags
#
#LOCAL_JNI_SHARED_LIBRARIES := libnfc_jni
#
#LOCAL_PROGUARD_ENABLED := disabled
#
#include $(BUILD_PACKAGE)

########################################
# NCI Configuration
########################################
#include $(CLEAR_VARS)
#
#LOCAL_MODULE_TAGS := optional
#
#LOCAL_SRC_FILES := \
#        $(call all-java-files-under, src)
#
#LOCAL_SRC_FILES += \
#        $(call all-java-files-under, nci)
#
#LOCAL_PACKAGE_NAME := NfcNci
#LOCAL_OVERRIDES_PACKAGES := Nfc
#LOCAL_CERTIFICATE := platform
#
#LOCAL_STATIC_JAVA_LIBRARIES := NfcLogTags
#
#LOCAL_JNI_SHARED_LIBRARIES := libnfc_nci_jni
#
#LOCAL_PROGUARD_ENABLED := disabled
#
#include $(BUILD_PACKAGE)

#####
# static lib for the log tags
#####
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := src/com/android/nfc/EventLogTags.logtags

LOCAL_MODULE:= NfcLogTags

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH)/mtk-nfc)

endif
