# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.


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

# Configuration
BUILD_PATCH  := false
BUILD_WMT_CFG_L2   := false
BUILD_WMT_CFG_L1   := false
BUILD_MT6620 := false
BUILD_MT6628 := false
BUILD_MT6582 :=false


ifeq ($(MTK_COMBO_SUPPORT), yes)

LOCAL_PATH := $(call my-dir)

BUILD_PATCH := true
BUILD_WMT_CFG_L2 := true

ifeq ($(BUILD_WMT_CFG_L2), true)
    $(call config-custom-folder, cfg_folder:hal/ant)
    $(info cfg_folder:$(cfg_folder))
endif

ifeq ($(BUILD_PATCH), true)
    $(call config-custom-folder, patch_folder:hal/combo)
    $(info $(patch_folder))
endif

ifneq ($(filter MT6620E3,$(MTK_COMBO_CHIP)),)
    BUILD_MT6620 := true
    BUILD_WMT_CFG_L1 := true
endif

ifneq ($(filter MT6620,$(MTK_COMBO_CHIP)),)
    BUILD_MT6620 := true
    BUILD_WMT_CFG_L1 := true
endif

ifneq ($(filter MT6628,$(MTK_COMBO_CHIP)),)
    BUILD_MT6628 := true
    BUILD_WMT_CFG_L1 := true
endif

ifneq ($(filter MT6572_CONSYS,$(MTK_COMBO_CHIP)),)
    BUILD_MT6582 := true
endif


##### INSTALL WMT.CFG FOR COMBO CONFIG #####
ifeq ($(BUILD_WMT_CFG_L1), true)
include $(CLEAR_VARS)
LOCAL_MODULE := WMT.cfg
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif

ifeq ($(BUILD_MT6620), true)
$(warning building MT6620)
ifneq ($(filter mt6620_ant_m1,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m1)
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m1.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT) 
endif

ifneq ($(filter mt6620_ant_m2,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m2)    
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m2.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif

ifneq ($(filter mt6620_ant_m3,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m3)     
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m3.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT) 
endif

ifneq ($(filter mt6620_ant_m4,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m4)     
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m4.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)  
endif

ifneq ($(filter mt6620_ant_m5,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m5)      
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m5.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif

ifneq ($(filter mt6620_ant_m6,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m6)         
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m6.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif

ifneq ($(filter mt6620_ant_m7,$(CUSTOM_HAL_ANT)),)
$(warning building mt6620_ant_m7)         
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_ant_m7.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif 

$(warning building mt6620_patch_e3_0_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_patch_e3_0_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)

$(warning building mt6620_patch_e3_1_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_patch_e3_1_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)

$(warning building mt6620_patch_e3_2_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_patch_e3_2_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)

$(warning building mt6620_patch_e3_3_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6620_patch_e3_3_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)

$(warning building MT6620 finished)
endif


ifeq ($(BUILD_MT6628), true)
$(warning building MT6628)

ifneq ($(filter mt6628_ant_m1,$(CUSTOM_HAL_ANT)),)
$(warning building mt6628_ant_m1)
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_ant_m1.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif  

ifneq ($(filter mt6628_ant_m2,$(CUSTOM_HAL_ANT)),)
$(warning building mt6628_ant_m2)    
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_ant_m2.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif

ifneq ($(filter mt6628_ant_m3,$(CUSTOM_HAL_ANT)),)
$(warning building mt6628_ant_m3)       
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_ant_m3.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT) 
endif

ifneq ($(filter mt6628_ant_m4,$(CUSTOM_HAL_ANT)),)
$(warning building mt6628_ant_m4)        
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_ant_m4.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)  
endif

$(warning building mt6628_patch_e1_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_patch_e1_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
    
$(warning building mt6628_patch_e2_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_patch_e2_0_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)

    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6628_patch_e2_1_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
    
$(warning building MT6628 finished)
endif

ifeq ($(BUILD_MT6582), true)
$(warning building MT6582)

ifneq ($(filter mt6582_ant_m1,$(CUSTOM_HAL_ANT)),)
$(warning building mt6582_ant_m1)
    include $(CLEAR_VARS)
    LOCAL_MODULE := WMT_SOC.cfg
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := cfg_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
endif  

$(warning building mt6572_82_patch_e1_hdr) 
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6572_82_patch_e1_0_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
    
    include $(CLEAR_VARS)
    LOCAL_MODULE := mt6572_82_patch_e1_1_hdr.bin
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE_CLASS := ETC
    LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
    LOCAL_SRC_FILES := patch_folder/$(LOCAL_MODULE)
    include $(BUILD_PREBUILT)
    
$(warning building MT6582 finished)
endif

include $(call all-makefiles-under,$(LOCAL_PATH))

endif
