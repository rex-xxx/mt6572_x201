# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng
LOCAL_ARM_MODE := arm

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, ./sources) \
						 $(call all-c-files-under, ./porting)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/interfaces	\
						$(LOCAL_PATH)/porting	\
						$(LOCAL_PATH)/../../../../../open_nfc/interfaces \
						$(LOCAL_PATH)/../../../../../open_nfc/porting/common \
						$(LOCAL_PATH)/../../../../../open_nfc/porting/server \
						$(LOCAL_PATH)/../../include	\
						$(LOCAL_PATH)/driver
						
LOCAL_MODULE := nfc_hal_microread

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/driver/open_nfc_driver.ko:system/lib/open_nfc_driver.ko

include $(BUILD_SHARED_LIBRARY)
