ifeq ($(TARGET_ARCH),arm)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := recovery_ui.cpp masterclear_bp.c
LOCAL_C_INCLUDES += bootable/recovery
# should match TARGET_RECOVERY_UI_LIB set in BoardConfig.mk
LOCAL_MODULE := librecovery_ui_stingray
include $(BUILD_STATIC_LIBRARY)

endif   # TARGET_ARCH == arm
