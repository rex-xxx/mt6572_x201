LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := TelephonyProvider
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += telephony-common mms-common
LOCAL_STATIC_JAVA_LIBRARIES += android-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
