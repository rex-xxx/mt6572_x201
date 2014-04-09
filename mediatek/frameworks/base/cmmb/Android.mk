ifeq ($(HAVE_CMMB_FEATURE),yes)
LOCAL_PATH:= $(call my-dir)
CMMB_TOP := $(LOCAL_PATH)
include $(CLEAR_VARS)

#include $(CMMB_TOP)/osw/linux/Android.mk
include $(CMMB_TOP)/osal/Android.mk
#include $(CMMB_TOP)/inno_appdriver/Android.mk
#include $(CMMB_TOP)/siano_appdriver_new/Android.mk
#include $(CMMB_TOP)/siano_hostlib/Android.mk
#include $(CMMB_TOP)/uam/Android.mk
#include $(CMMB_TOP)/serviceprovider/Android.mk
#include $(CMMB_TOP)/jni/Android.mk
#include $(CMMB_TOP)/unittest/Android.mk
endif
