LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
else
#ifeq ($(MTK_PLATFORM),$(filter $(MTK_PLATFORM),MT6589))
LOCAL_CFLAGS += -DMTK_USEDPFRMWK
#endif
endif

ifeq "$(strip $(MTK_75DISPLAY_ENHANCEMENT_SUPPORT))" "yes"
  LOCAL_CFLAGS += -DMTK_75DISPLAY_ENHANCEMENT_SUPPORT
endif

LOCAL_SRC_FILES:=                     \
        ColorConverter.cpp            \
        SoftwareRenderer.cpp

LOCAL_C_INCLUDES := \
	$(TOP)/mediatek/frameworks/av/media/libstagefright/include/omx_core \
	$(TOP)/frameworks/native/include/media/openmax \
	$(TOP)/mediatek/hardware/dpframework/inc \
	$(TOP)/mediatek/external/mhal/inc

LOCAL_MODULE:= libstagefright_color_conversion

include $(BUILD_STATIC_LIBRARY)
