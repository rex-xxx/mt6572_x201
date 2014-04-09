ifneq ($(TARGET_SIMULATOR),true)
ifeq ($(TARGET_ARCH),arm)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

CORE_SRC_FILES := \
	src/tst_main.c

LOCAL_SRC_FILES := \
	$(CORE_SRC_FILES)

LOCAL_C_INCLUDES:= \
	$(LOCAL_PATH)/inc \
	$(MTK_PATH_PLATFORM)/external/meta/ft \
	mediatek/external/dfo/featured \
	$(TARGET_OUT_HEADERS)/dfo

LOCAL_STATIC_LIBRARIES:=libc libcutils
LOCAL_SHARED_LIBRARIES := libft

ifeq ($(TELEPHONY_DFOSET),yes)
LOCAL_SHARED_LIBRARIES += libdfo
endif  

LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_MODULE:=meta_tst

LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT_SBIN)
LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_SBIN_UNSTRIPPED)

include $(BUILD_EXECUTABLE)

endif   # TARGET_ARCH == arm
endif	# !TARGET_SIMULATOR
