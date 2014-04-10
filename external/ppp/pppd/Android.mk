
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

local_src_files:= \
	main.c \
	magic.c \
	fsm.c \
	lcp.c \
	ipcp.c \
	upap.c \
	chap-new.c \
	ccp.c \
	ecp.c \
	auth.c \
	options.c \
	sys-linux.c \
	chap_ms.c \
	demand.c \
	utils.c \
	tty.c \
	eap.c \
	chap-md5.c \
	pppcrypt.c \
	openssl-hash.c \
	pppox.c


#######################################
# pure pppd binary

LOCAL_SRC_FILES += $(local_src_files)

ifeq ($(MTK_BT_PROFILE_DUN),yes)	
	LOCAL_SRC_FILES += pppbt.c
endif

LOCAL_SHARED_LIBRARIES := \
	libcutils libcrypto

ifeq ($(MTK_BT_PROFILE_DUN),yes)
LOCAL_SHARED_LIBRARIES += libpppbtdun
endif

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include

LOCAL_CFLAGS := -DANDROID_CHANGES -DCHAPMS=1 -DMPPE=1 -Iexternal/openssl/include

ifeq ($(MTK_BT_PROFILE_DUN),yes)
LOCAL_CFLAGS += -D__BTMTK__ -D__BT_DUN_PROFILE__
endif


LOCAL_CFLAGS += -DUSE_NEG_ADDR


ifeq ($(EVDO_DT_VIA_SUPPORT),yes)
LOCAL_CFLAGS += -D__VIA_PPPD_DOWN__
endif

LOCAL_MODULE:= pppd

include $(BUILD_EXECUTABLE)

#######################################
# mtk dual talk pppd binary
include $(CLEAR_VARS)

LOCAL_SRC_FILES += $(local_src_files)

LOCAL_SHARED_LIBRARIES := \
	libcutils libcrypto
	
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include

LOCAL_CFLAGS := -DANDROID_CHANGES -DCHAPMS=1 -DMPPE=1 -Iexternal/openssl/include

LOCAL_MODULE:= pppd_dt

include $(BUILD_EXECUTABLE)

#######################################

