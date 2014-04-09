LOCAL_PATH := $(call my-dir)
$(call config-custom-folder,custom:hal/bluetooth)

###########################################################################
# MTK BT DRIVER SOLUTION
###########################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  bt_drv.c

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/custom \
  $(LOCAL_PATH)/../inc

LOCAL_MODULE := libbluetoothdrv
LOCAL_SHARED_LIBRARIES := liblog libdl libhardware_legacy
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)


############# MTK BT init library #############
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  mtk.c \
  radiomgr.cpp \
  radiomod.c

LOCAL_C_INCLUDES := \
  $(MTK_PATH_SOURCE)/external/nvram/libnvram \
  $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
  $(MTK_PATH_CUSTOM)/cgen/cfgdefault \
  $(LOCAL_PATH)/custom


ifeq ($(TARGET_BUILD_VARIANT), eng)
LOCAL_CFLAGS += -DDEV_VERSION
endif

LOCAL_MODULE := libbluetooth_mtk
LOCAL_SHARED_LIBRARIES := libnvram liblog libcutils
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)

###########################################################################
# INSTALL BT FIRMWARE
###########################################################################
ifeq ($(MTK_BT_CHIP), )
$(error Should define MTK_BT_CHIP)
endif

ifeq ($(MTK_BT_CHIP), MTK_MT6622)
include $(CLEAR_VARS)
LOCAL_MODULE := $(MTK_BT_CHIP)_E2_Patch.nb0
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif

ifeq ($(MTK_BT_CHIP), MTK_MT6626)
include $(CLEAR_VARS)
LOCAL_MODULE := $(MTK_BT_CHIP)_E2_Patch.nb0
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif

############# INSTALL BT EXT FIRMWARE #############


###########################################################################
# BT ENGINEER MODE
###########################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := bt_em.c

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/custom \
  $(LOCAL_PATH)/../inc

LOCAL_MODULE :=libbluetoothem_mtk
LOCAL_SHARED_LIBRARIES := liblog libdl
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)


################ BT RELAYER ##################
BUILD_BT_RELAYER := true
ifeq ($(BUILD_BT_RELAYER), true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := bt_relayer.c

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/../inc

LOCAL_MODULE := libbluetooth_relayer
LOCAL_SHARED_LIBRARIES := liblog libbluetoothem_mtk
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)
endif
