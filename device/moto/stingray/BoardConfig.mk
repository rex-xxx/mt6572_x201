# Copyright (C) 2010 The Android Open Source Project
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

#
# This file sets variables that control the way modules are built
# thorughout the system. It should not be used to conditionally
# disable makefiles (the proper mechanism to control what gets
# included in a build is to use PRODUCT_PACKAGES in a product
# definition file).
#

ifeq ($(filter trygon trygon_l10n calgon full_stingray tyranid stingray,$(TARGET_PRODUCT)),)
$(warning The stingray device can only be used)
$(warning with the following TARGET_PRODUCT:)
$(warning trygon trygon_l10n calgon full_stingray tyranid stingray)
$(warning and you are using $(TARGET_PRODUCT).)
$(warning If that's correct, you need to modify)
$(warning the following files:)
$(warning device/moto/stingray/BoardConfig.mk)
$(warning device/moto/wingray/device_base.mk)
$(warning and (if it comes from git))
$(warning vendor/moto/stingray/stingray-vendor.mk)
$(error unknown TARGET_PRODUCT for stingray)
endif

include device/moto/wingray/BoardConfig.mk
BOARD_KERNEL_CMDLINE :=

