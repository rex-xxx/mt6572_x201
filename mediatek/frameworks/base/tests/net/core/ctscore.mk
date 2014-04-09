# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# First we build an apk without the core-tests resource
cts_ORIGINAL_PACKAGE_NAME := $(LOCAL_PACKAGE_NAME)

LOCAL_PACKAGE_NAME := $(LOCAL_PACKAGE_NAME).no-core-tests-res
# Make sure this apk won't get installed
LOCAL_UNINSTALLABLE_MODULE := true

LOCAL_JAVA_LIBRARIES := android.test.runner bouncycastle
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)
# Vars set by $(BUILD_PACKAGE) and needed by the below module definition.
cts_no-core-tests-res_BUILT_MODULE := $(LOCAL_BUILT_MODULE)
cts_no-core-tests-res_private_key := $(private_key)
cts_no-core-tests-res_certificate := $(certificate)

##################################
# Now the rules to build the apk with core-tests resource
include $(CLEAR_VARS)

LOCAL_MODULE := $(cts_ORIGINAL_PACKAGE_NAME)
LOCAL_MODULE_CLASS := APPS
# don't include these packages in any target
LOCAL_MODULE_TAGS := optional
# and when built explicitly put them in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := package.apk

include $(BUILD_SYSTEM)/base_rules.mk

CORETESTS_INTERMEDIATES := $(call intermediates-dir-for,JAVA_LIBRARIES,core-tests,,COMMON)

$(LOCAL_BUILT_MODULE): PRIVATE_INTERMEDIATES_COMMON := $(intermediates.COMMON)
$(LOCAL_BUILT_MODULE): PRIVATE_CORETESTS_INTERMEDIATES_COMMON := $(CORETESTS_INTERMEDIATES)
$(LOCAL_BUILT_MODULE): PRIVATE_PRIVATE_KEY := $(cts_no-core-tests-res_private_key)
$(LOCAL_BUILT_MODULE): PRIVATE_CERTIFICATE := $(cts_no-core-tests-res_certificate)
$(LOCAL_BUILT_MODULE): $(cts_no-core-tests-res_BUILT_MODULE) $(CORETESTS_INTERMEDIATES)/javalib.jar
	@echo "Add resources to package ($@)"
	$(hide) mkdir -p $(dir $@) $(PRIVATE_INTERMEDIATES_COMMON)
	$(hide) rm -rf $(PRIVATE_INTERMEDIATES_COMMON)/ctsclasses
	# javalib.jar should only contain .dex files, but the harmony tests also include
	# some .class files, so get rid of them
	$(hide) unzip -qo $(PRIVATE_CORETESTS_INTERMEDIATES_COMMON)/javalib.jar \
		-d $(PRIVATE_INTERMEDIATES_COMMON)/ctsclasses
	$(hide) find $(PRIVATE_INTERMEDIATES_COMMON)/ctsclasses -type f -name "*.class" -delete
	$(hide) rm -f $(PRIVATE_INTERMEDIATES_COMMON)/ctsclasses/classes.dex
	$(hide) cp $< $@
	$(hide) jar uf $@ -C $(PRIVATE_INTERMEDIATES_COMMON)/ctsclasses .
	$(sign-package)
	$(align-package)

# some global vars set in $(BUILD_PACKAGE), not sure if we really need here
PACKAGES.$(cts_ORIGINAL_PACKAGE_NAME).PRIVATE_KEY := $(cts_no-core-tests-res_private_key)
PACKAGES.$(cts_ORIGINAL_PACKAGE_NAME).CERTIFICATE := $(cts_no-core-tests-res_certificate)
PACKAGES := $(PACKAGES) $(cts_ORIGINAL_PACKAGE_NAME)
