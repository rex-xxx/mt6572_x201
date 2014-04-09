LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(MTK_HWC_SUPPORT), yes)

LOCAL_SRC_FILES := \
	hwc.cpp

LOCAL_CFLAGS := \
	-DLOG_TAG=\"hwcomposer\"

ifeq ($(MTK_HDMI_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_EXTERNAL_SUPPORT
endif

ifeq ($(MTK_WFD_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_VIRTUAL_SUPPORT
endif

ifeq ($(MTK_PQ_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_ENHAHCE_SUPPORT
endif

ifneq ($(MTK_TABLET_HARDWARE), )
MTK_HWC_CHIP = $(shell echo $(MTK_TABLET_HARDWARE) | tr A-Z a-z )
else
MTK_HWC_CHIP = $(shell echo $(MTK_PLATFORM) | tr A-Z a-z )
endif

ifeq ($(MTK_HWC_VERSION), 1.2)
LOCAL_CFLAGS += -DMTK_HWC_VER_1_2
endif

LOCAL_STATIC_LIBRARIES += hwcomposer.$(MTK_HWC_CHIP).$(MTK_HWC_VERSION)

LOCAL_SHARED_LIBRARIES := \
	libEGL \
	libGLESv1_CM \
	libui \
	libutils \
	libcutils \
	libsync \
	libm4u \
	libdpframework \
    libaed

# HAL module implemenation stored in
# hw/<OVERLAY_HARDWARE_MODULE_ID>.<ro.product.board>.so
LOCAL_MODULE := hwcomposer.$(MTK_HWC_CHIP)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
include $(BUILD_SHARED_LIBRARY)

endif # MTK_HWC_SUPPORT
