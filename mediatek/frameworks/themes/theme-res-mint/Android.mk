
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_PACKAGE_NAME := theme-res-mint
LOCAL_CERTIFICATE := platform

LOCAL_AAPT_FLAGS := -x9

LOCAL_MODULE_TAGS := optional

# Install this alongside the libraries.
LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)

# Create package-export.apk, which other packages can use to get
# PRODUCT-agnostic resource data like IDs and type definitions.
LOCAL_EXPORT_PACKAGE_RESOURCES := true

$(call intermediates-dir-for,APPS,theme-res-mint,,COMMON)/package-export.apk:$(call intermediates-dir-for,APPS,framework-res,,COMMON)/src/R.stamp
$(call intermediates-dir-for,APPS,theme-res-mint,,COMMON)/package-export.apk:$(call intermediates-dir-for,APPS,mediatek-res,,COMMON)/src/R.stamp

$(shell perl $(LOCAL_PATH)/copy_res.pl $(LOCAL_PATH))
include $(BUILD_PACKAGE)

# define a global intermediate target that other module may depend on.
.PHONY: mtk-framework-res-package-target
mtk-framework-res-package-target: $(LOCAL_BUILT_MODULE)
