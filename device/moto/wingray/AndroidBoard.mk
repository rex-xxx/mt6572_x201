LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(TARGET_PREBUILT_KERNEL),)
TARGET_PREBUILT_KERNEL := $(LOCAL_PATH)/kernel
endif

file := $(INSTALLED_KERNEL_TARGET)
ALL_PREBUILT += $(file)
$(file): $(TARGET_PREBUILT_KERNEL) | $(ACP)
	$(transform-prebuilt-to-target)

include $(CLEAR_VARS)

COMMON_DIR := vendor/nvidia/common/

ifeq ($(wildcard $(COMMON_DIR)/TegraBoard.mk),$(COMMON_DIR)/TegraBoard.mk)
include $(COMMON_DIR)/TegraBoard.mk
endif

subdir_makefiles:= \
	$(LOCAL_PATH)/libaudio/Android.mk \
	$(LOCAL_PATH)/taudio/Android.mk

include $(subdir_makefiles)

-include vendor/moto/stingray/AndroidBoardVendor.mk
