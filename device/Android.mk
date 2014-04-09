# dummy Android.mk to disable htc & samsung codes developed by google to be built in MediaTek's solution
# if you want to place your codes under this folder, please include your "Android.mk" here

ifeq ($(MTK_EMULATOR_SUPPORT),yes)
hardware_modules := generic
include $(call all-named-subdir-makefiles,generic)
endif