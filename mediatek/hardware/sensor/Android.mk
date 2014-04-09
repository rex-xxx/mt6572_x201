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

ifeq ($(MTK_SENSOR_SUPPORT),yes)
# Copyright 2007 The Android Open Source Project
#
ifeq ($(CUSTOM_KERNEL_MAGNETOMETER), )
$(shell rm -f $(PRODUCT_OUT)/system/etc/permissions/android.hardware.sensor.compass.xml)
endif
ifeq ($(CUSTOM_KERNEL_ACCELEROMETER), )
$(shell rm -f $(PRODUCT_OUT)/system/etc/permissions/android.hardware.sensor.accelerometer.xml)
endif
ifeq ($(CUSTOM_KERNEL_ALSPS), )
$(shell rm -f $(PRODUCT_OUT)/system/etc/permissions/android.hardware.sensor.light.xml)
$(shell rm -f $(PRODUCT_OUT)/system/etc/permissions/android.hardware.sensor.proximity.xml)
endif
ifeq ($(CUSTOM_KERNEL_GYROSCOPE), )
$(shell rm -f $(PRODUCT_OUT)/system/etc/permissions/android.hardware.sensor.gyroscope.xml)
endif
LOCAL_DIR := $(call my-dir)
include $(LOCAL_DIR)/hwmsen/Android.mk

endif
