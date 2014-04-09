LOCAL_PATH:= $(call my-dir)

# Static library with some common classes for the phone apps.
# To use it add this line in your Android.mk
#  LOCAL_STATIC_JAVA_LIBRARIES := com.android.phone.common

include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := CellConnUtil

LOCAL_JAVA_LIBRARIES += mediatek-framework

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    ../src/com/android/phone/CallLogAsync.java \
    ../src/com/android/phone/HapticFeedback.java \
    ../src/com/android/phone/Constants.java \

LOCAL_MODULE := com.android.phone.common
include $(BUILD_STATIC_JAVA_LIBRARY)