LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng
# ??
LOCAL_ARM_MODE := arm

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, ./sources) \
                   $(call all-c-files-under, ./porting)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/interfaces \
                    $(LOCAL_PATH)/porting \
		    $(LOCAL_PATH)/sources \
                    $(LOCAL_PATH)/../../../../../open_nfc/interfaces \
                    $(LOCAL_PATH)/../../../../../open_nfc/porting/common \
                    $(LOCAL_PATH)/../../../../../open_nfc/porting/server \
                    $(LOCAL_PATH)/../../include \

LOCAL_CFLAGS += -DDO_NOT_USE_LWRAP_UNICODE

LOCAL_MODULE := libnfc_hal_msr3110

include $(BUILD_SHARED_LIBRARY)


