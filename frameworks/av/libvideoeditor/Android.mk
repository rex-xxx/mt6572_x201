ifneq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
ifeq ($(strip $(MTK_ENABLE_VIDEO_EDITOR)),yes)

LOCAL_PATH := $(call my-dir)

include $(call all-subdir-makefiles)

endif
endif
