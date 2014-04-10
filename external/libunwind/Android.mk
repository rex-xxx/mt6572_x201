LOCAL_PATH := $(call my-dir)

commonCflags	:= -DHAVE_CONFIG_H -DUNW_LOCAL_ONLY -DHAVE_ANDROID_LOG_H -DHAVE_LINUX_ASHMEM_H 
#-DDEBUG

commonIncludes	:= \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/src \
	$(KERNEL_HEADERS)

commonSrcFiles	:= \
	src/mi/init.c \
	src/mi/dyn-info-list.c \
	src/mi/flush_cache.c \
	src/mi/mempool.c \
	src/mi/strerror.c \
	src/mi/Ldyn-extract.c \
	src/mi/Ldyn-remote.c \
	src/mi/Lfind_dynamic_proc_info.c \
	src/mi/Lget_accessors.c \
	src/mi/Lget_proc_info_by_ip.c \
	src/mi/Lget_proc_name.c \
	src/mi/Lput_dynamic_unwind_info.c \
	src/mi/Ldestroy_addr_space.c \
	src/mi/Lget_reg.c \
	src/mi/Lset_reg.c \
	src/mi/Lget_fpreg.c \
	src/mi/Lset_fpreg.c \
	src/mi/Lset_caching_policy.c \
	src/os-linux.c \
	src/dl-iterate-phdr.c \
	src/dwarf/global.c \
	src/dwarf/Lexpr.c \
	src/dwarf/Lfde.c \
	src/dwarf/Lparser.c \
	src/dwarf/Lstep.c \
	src/dwarf/Lfind_proc_info-lsb.c \
	src/dwarf/Lpe.c \
	src/ptrace/_UPT_elf.c \
	src/ptrace/_UPT_accessors.c \
	src/ptrace/_UPT_access_fpreg.c \
	src/ptrace/_UPT_access_mem.c \
	src/ptrace/_UPT_access_reg.c \
	src/ptrace/_UPT_create.c \
	src/ptrace/_UPT_destroy.c \
	src/ptrace/_UPT_find_proc_info.c \
	src/ptrace/_UPT_get_dyn_info_list_addr.c \
	src/ptrace/_UPT_put_unwind_info.c \
	src/ptrace/_UPT_get_proc_name.c \
	src/ptrace/_UPT_reg_offset.c \
	src/ptrace/_UPT_resume.c

ifeq ($(TARGET_ARCH),arm)
Cflags := $(commonCflags)

Includes := $(commonIncludes) \
	$(LOCAL_PATH)/include/tdep-arm

SrcFiles := $(commonSrcFiles) \
	src/arm/is_fpreg.c \
	src/arm/regname.c \
	src/arm/Lcreate_addr_space.c \
	src/arm/Lget_proc_info.c \
	src/arm/Lget_save_loc.c \
	src/arm/Lglobal.c \
	src/arm/Linit.c \
	src/arm/Linit_local.c \
	src/arm/Linit_remote.c \
	src/arm/Lis_signal_frame.c \
	src/arm/Lregs.c \
	src/arm/Lresume.c \
	src/arm/Lstep.c \
	src/arm/Lex_tables.c
endif # arm

# build static library
include $(CLEAR_VARS)

# mtk04376: Force to ARM build to speed up
LOCAL_ARM_MODE := arm

LOCAL_CFLAGS += $(Cflags)
LOCAL_C_INCLUDES := $(Includes)
LOCAL_SRC_FILES	:= $(SrcFiles)
LOCAL_SHARED_LIBRARIES := libc
LOCAL_MODULE := libunwind
LOCAL_MODULE_TAGS := optional
include $(BUILD_STATIC_LIBRARY)
