ifeq ($(strip $(MTK_AUTO_TEST)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_CERTIFICATE := shared

LOCAL_JAVA_LIBRARIES := android.test.runner \
			robotium

LOCAL_STATIC_JAVA_LIBRARIES := libjunitreport-for-contact-tests

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := ContactsTest

LOCAL_INSTRUMENTATION_FOR := Contacts

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjunitreport-for-contact-tests:libs/android-junit-report-dev.jar

include $(BUILD_MULTI_PREBUILT)

endif