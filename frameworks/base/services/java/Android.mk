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


LOCAL_PATH:= $(call my-dir)

# the library
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
            $(call all-subdir-java-files) \
	    com/android/server/EventLogTags.logtags \
	    com/android/server/am/EventLogTags.logtags

MTK_SERVICES_JAVA_PATH := ../../../../mediatek/frameworks-ext/base/services/java

LOCAL_SRC_FILES += $(call all-java-files-under,$(MTK_SERVICES_JAVA_PATH))

LOCAL_MODULE:= services

ifneq ($(PARTIAL_BUILD),true)
LOCAL_PROGUARD_ENABLED := custom
LOCAL_PROGUARD_FLAG_FILES := ../../../../mediatek/frameworks-ext/base/services/java/proguard.flags
LOCAL_PROGUARD_SOURCE := javaclassfile
LOCAL_EXCLUDED_JAVA_CLASSES := com/android/server/am/ANRManager*.class
endif

LOCAL_JAVA_LIBRARIES := android.policy telephony-common emma

ifeq ($(strip $(SERVICE_EMMA_ENABLE)),yes)
LOCAL_NO_EMMA_INSTRUMENT := false
LOCAL_NO_EMMA_COMPILE := false
else
LOCAL_NO_EMMA_INSTRUMENT := true
LOCAL_NO_EMMA_COMPILE := true
endif

ifeq ($(strip $(SYSTEM_SERVER_WM)),yes)
EMMA_INSTRUMENT := true
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/wms_filter_method.txt
endif

ifeq ($(strip $(SYSTEM_SERVER_AM)),yes)
EMMA_INSTRUMENT := true
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/ams_filter_method.txt
endif

ifeq ($(strip $(SYSTEM_SERVER_PM)),yes)
EMMA_INSTRUMENT := true
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/pms_filter_method.txt
endif

ifeq ($(strip $(SYSTEM_SERVER)),yes)
EMMA_INSTRUMENT := true
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/systemserver_filter_method.txt
endif

include $(BUILD_JAVA_LIBRARY)

include $(BUILD_DROIDDOC)
