ifeq ($(strip $(HAVE_CMMB_FEATURE)), yes)
LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_PACKAGE_NAME := CMMBPlayer

LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.mbbms.protocol

# switch for CMMB update apk build ,yes:build  no: no build
CMMB_UPDATE_APK = no 
ifeq ($(strip $(CMMB_UPDATE_APK)), yes)
# location of JNI shared libraries, cp libcmmbdrv.so untile it is used.
GEN := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/libcmmbdrv.so

$(GEN): $(TARGET_OUT)/lib/modules/cmmbdrv.ko
        $(hide) if [ ! -d $(dir $@) ]; then mkdir -p $(dir $@); fi
	$(hide) cp -f $< $@

LOCAL_JNI_SHARED_LIBRARIES :=libcmmbsp \
                             libcmmb_jni \
                             libcmmbdrv
$(LOCAL_BUILT_MODULE): $(GEN)
endif

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
