LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#ifeq ($(BOARD_HAVE_BLUETOOTH),true)
  include $(all-subdir-makefiles)
#endif

#ifeq ($(MTK_BT_SUPPORT),true)
  include $(LOCAL_PATH)/data/Android.mk
#endif
