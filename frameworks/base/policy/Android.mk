LOCAL_PATH:= $(call my-dir)

# the library
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

MTK_SERVICES_JAVA_PATH := ../../../mediatek/frameworks-ext/base/policy/java

LOCAL_SRC_FILES += $(call all-java-files-under,$(MTK_SERVICES_JAVA_PATH))
            
LOCAL_MODULE := android.policy

LOCAL_JAVA_LIBRARIES += mediatek-common mms-common mediatek-framework

LOCAL_STATIC_JAVA_LIBRARIES := CellConnUtil

include $(BUILD_JAVA_LIBRARY)

# additionally, build unit tests in a separate .apk
include $(call all-makefiles-under,$(LOCAL_PATH))
