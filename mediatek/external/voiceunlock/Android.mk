ifeq ($(strip $(MTK_VOICE_UNLOCK_SUPPORT)),yes)
LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := libvoiceunlock.a
include $(BUILD_MULTI_PREBUILT)
endif