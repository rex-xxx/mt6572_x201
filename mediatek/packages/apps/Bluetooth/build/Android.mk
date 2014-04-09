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

#############################################
# Build Java Package
#############################################
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES += mediatek-framework

$(info [BlueAngel][PKG] LOCAL_PATH=$(LOCAL_PATH))

### define build path
MY_BUILD_PATH := ${LOCAL_PATH}
MY_BUILD_PREFIX := ..

### clean first
$(shell rm -rf $(LOCAL_PATH)/AndroidManifest.xml)
$(shell rm -rf $(LOCAL_PATH)/res)
$(shell mkdir $(LOCAL_PATH)/res)

### generate AndroidManifest.xml
#"mediatek/config/" + $(FULL_PROJECT) + "/ProjectConfig.mk"
#$(info executing blueangel.py: project[$(FULL_PROJECT)], PYTHONPATH[$(PYTHONPATH)])
PY_RES := $(shell python $(LOCAL_PATH)/blueangel.py)

LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/emma_filter.txt,--$(LOCAL_PATH)/emma_filter_method.txt

### include modules' mk file
ifeq ($(MTK_BT_SUPPORT), yes)
include $(MY_MODULE_PATH)/common/bt40/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_OPP), yes)
include $(MY_MODULE_PATH)/profiles/opp/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_PRXM), yes)
include $(MY_MODULE_PATH)/profiles/prxm/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_PRXR), yes)
include $(MY_MODULE_PATH)/profiles/prxr/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_SIMAP), yes)
include $(MY_MODULE_PATH)/profiles/simap/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_HIDH), yes)
include $(MY_MODULE_PATH)/profiles/hid/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_FTP), yes)
include $(MY_MODULE_PATH)/profiles/ftp/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_PBAP), yes)
include $(MY_MODULE_PATH)/profiles/pbap/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_BPP), yes)
include $(MY_MODULE_PATH)/profiles/bpp/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_BIP), yes)
include $(MY_MODULE_PATH)/profiles/bip/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_DUN), yes)
include $(MY_MODULE_PATH)/profiles/dun/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_AVRCP), yes)
include $(MY_MODULE_PATH)/profiles/avrcp/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_PAN), yes)
include $(MY_MODULE_PATH)/profiles/pan/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_MAPS), yes)
include $(MY_MODULE_PATH)/profiles/map/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_TIMES), yes)
include $(MY_MODULE_PATH)/profiles/times/Android.mk
endif
ifeq ($(MTK_BT_PROFILE_TIMEC), yes)
include $(MY_MODULE_PATH)/profiles/timec/Android.mk
endif
### config package and build

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := MtkBt
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags
include $(BUILD_PACKAGE)

#############################################
# End of file
#############################################
