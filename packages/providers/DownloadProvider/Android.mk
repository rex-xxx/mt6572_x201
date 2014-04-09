LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)


LOCAL_PACKAGE_NAME := DownloadProvider
LOCAL_CERTIFICATE := media
LOCAL_STATIC_JAVA_LIBRARIES := guava\
                  com.mediatek.downloadmanager.plugin
LOCAL_JAVA_LIBRARIES += mediatek-framework

# M: add for emma
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/downloadprovider-emma-filter.txt

include $(BUILD_PACKAGE)

# build UI + tests
include $(call all-makefiles-under,$(LOCAL_PATH))
