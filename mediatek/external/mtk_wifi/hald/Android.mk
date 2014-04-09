
ifeq ($(MTK_WLAN_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := libsysutils libcutils libwpa_client libnetutils

#MTK CONFIG

#Chip Reset Manager for Whole-Chip Reset Support
LOCAL_CFLAGS += -DCFG_ENABLE_RESET_MGR

#Auto Enable RFKILL Interface after Loading P2P Driver
#LOCAL_CFLAGS += -DCFG_ENABLE_RFKILL_IF_FOR_CFG80211

#Check NvRAM Status before Loading Driver
LOCAL_CFLAGS += -DCFG_ENABLE_NVRAM_CHECK

#Insert/remove by system property
LOCAL_CFLAGS += -DHANDLE_MODULE_BY_PROP

MODULE_POSTFIX := _$(shell echo $(strip $(MTK_WLAN_CHIP)) | tr A-Z a-z)

#LOCAL_CFLAGS += -DWIFI_DRIVER_MODULE_NAME=\"wlan$(MODULE_POSTFIX)\"
#LOCAL_CFLAGS += -DWIFI_HOTSPOT_DRIVER_MODULE_NAME=\"p2p$(MODULE_POSTFIX)\"
#LOCAL_CFLAGS += -DWIFI_P2P_DRIVER_MODULE_NAME=\"p2p$(MODULE_POSTFIX)\"    

#CONFIG_END

#NVRAM SUPPORT
#ifdef MTK_SDIORETRY_SUPPORT
LOCAL_SHARED_LIBRARIES += libnvram libcustom_nvram
LOCAL_C_INCLUDES:= $(MTK_PATH_SOURCE)/external/nvram/libnvram \
	$(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
	$(MTK_PATH_CUSTOM)/cgen/cfgdefault \
        $(MTK_PATH_CUSTOM)/cgen/inc \
#endif

LOCAL_SRC_FILES:=                                      \
                  main.cpp                             \
                  CommandListener.cpp                  \
                  HaldCommand.cpp                      \
                  RfkillCtrl.cpp                          \
                  ModuleCtrl.cpp                       \
                  DriverCtrl.cpp                       \
                  HaldController.cpp                   \
                  ResetHandler.cpp                     \
                  ResetManager.cpp                     \
                  ResetListener.cpp

LOCAL_MODULE:= hald
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:=          \
                  hdc.c \

LOCAL_MODULE:= hdc
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES := $(KERNEL_HEADERS)

LOCAL_CFLAGS :=

LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_EXECUTABLE)

endif

