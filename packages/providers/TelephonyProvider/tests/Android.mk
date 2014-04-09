ifeq ($(strip $(MTK_AUTO_TEST)), yes)
	LOCAL_PATH:= $(call my-dir)
	include $(CLEAR_VARS)
	
	# We only want this apk build for tests.
	LOCAL_MODULE_TAGS := tests
	
	# Include all test java files.
        # LOCAL_STATIC_JAVA_LIBRARIES := libjunitreprot-for-telephonyprovider-tests
	LOCAL_SRC_FILES := $(call all-java-files-under, src)
	
	LOCAL_PACKAGE_NAME :=  TelephonyProviderTests
	
LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_JAVA_LIBRARIES += telephony-common mms-common

	LOCAL_CERTIFICATE := platform
	LOCAL_INSTRUMENTATION_FOR := TelephonyProvider
	
	include $(BUILD_PACKAGE)

###############
# Junit Test option
#include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjunitreprot-for-telephonyprovider-tests:android-junit-report-1.2.6.jar
#include $(BUILD_MULTI_PREBUILT)
###############

endif
