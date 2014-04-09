
ifeq ($(strip $(MTK_AUTO_TEST)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := tests
#LOCAL_CERTIFICATE := platform
# We only want this apk build for tests.
#LOCAL_MODULE_TAGS := ReceiveAndReplyMMS_GEMINI

LOCAL_JAVA_LIBRARIES := android.test.runner\
			robotium

LOCAL_STATIC_JAVA_LIBRARIES := libjunitreport-for-sms-tests

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := smstest

LOCAL_INSTRUMENTATION_FOR := Mms

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjunitreport-for-sms-tests:libs/android-junit-report-dev.jar

include $(BUILD_MULTI_PREBUILT)

endif
