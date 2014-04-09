# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2011. All rights reserved.
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

internal_docs_FRAMEWORK_AIDL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,framework,,COMMON)/src
internal_docs_TEL_COMMON_AIDL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,telephony-common,,COMMON)/src
internal_docs_MTK_COMMON_AIDL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,mediatek-common,,COMMON)/src

INTERNAL_API_SRC_FILES := \
    ../../../external/apache-http/src/org/apache/http/params/HttpConnectionParams.java \
    ../../../external/libphonenumber/java/src/com/android/i18n/phonenumbers/Phonenumber.java \
    ../../../frameworks/base/core/java/android/app/AlarmManager.java \
    ../../../frameworks/base/core/java/android/app/DownloadManager.java \
    ../../../frameworks/base/core/java/android/app/StatusBarManager.java \
    ../../../frameworks/base/core/java/android/content/pm/PackageInfo.java \
    ../../../frameworks/base/core/java/android/content/res/Configuration.java \
    ../../../frameworks/base/core/java/android/content/res/Resources.java \
    ../../../frameworks/base/core/java/android/database/DatabaseUtils.java \
    ../../../frameworks/base/core/java/android/hardware/Camera.java \
    ../../../frameworks/base/core/java/android/net/ConnectivityManager.java \
    ../../../frameworks/base/core/java/android/net/NetworkPolicy.java \
    ../../../frameworks/base/core/java/android/net/NetworkUtils.java \
    ../../../frameworks/base/core/java/android/net/http/AndroidHttpClient.java \
    ../../../frameworks/base/core/java/android/nfc/NfcAdapter.java \
    ../../../frameworks/base/core/java/android/provider/CallLog.java \
    ../../../frameworks/base/core/java/android/provider/MediaStore.java \
    ../../../frameworks/base/core/java/android/provider/Settings.java \
    ../../../frameworks/base/core/java/android/view/SurfaceView.java \
    ../../../frameworks/base/core/java/android/webkit/WebView.java \
    ../../../frameworks/base/core/java/android/webkit/WebViewClassic.java \
    ../../../frameworks/base/core/java/android/widget/ListView.java \
    ../../../frameworks/base/core/java/android/widget/Spinner.java \
    ../../../frameworks/base/core/java/com/android/internal/widget/LockPatternUtils.java \
    ../../../frameworks/base/core/java/com/android/internal/widget/multiwaveview/GlowPadView.java \
    ../../../frameworks/base/media/java/android/media/AudioManager.java \
    ../../../frameworks/base/media/java/android/media/AudioSystem.java \
    ../../../frameworks/base/media/java/android/media/MediaRecorder.java \
    ../../../frameworks/base/media/java/android/media/RingtoneManager.java \
    ../../../frameworks/base/media/java/android/media/videoeditor/VideoEditorImpl.java \
    ../../../frameworks/base/media/java/android/mtp/MtpServer.java \
    ../../../frameworks/base/services/java/com/android/server/NetworkManagementService.java \
    ../../../frameworks/base/wifi/java/android/net/wifi/WifiConfiguration.java \
    ../../../frameworks/base/wifi/java/android/net/wifi/WifiManager.java \
    ../../../frameworks/opt/telephony/src/java/android/provider/Telephony.java \
    ../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/AdnRecord.java \
    ../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/CatResponseMessage.java \
    ../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/Input.java \
    ../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/Menu.java \
    ../../../mediatek/frameworks-ext/base/core/java/android/webkit/NotificationPermissions.java \
    ../../../mediatek/frameworks-ext/base/core/java/android/widget/BounceCoverFlow.java \
    ../../../mediatek/frameworks-ext/base/core/java/android/widget/BounceGallery.java \
    ../../../mediatek/frameworks-ext/base/wifi/java/android/net/wifi/HotspotClient.java \
    ../../../mediatek/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BluetoothAdapterEx.java \
    ../../../mediatek/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BluetoothUuidEx.java \
    ../../../mediatek/frameworks/base/notification/java/com/mediatek/NotificationManagerPlus.java \
    ../../../mediatek/frameworks/base/notification/java/com/mediatek/NotificationPlus.java \
    ../../../mediatek/frameworks/base/storage/java/com/mediatek/storage/StorageManagerEx.java \
    ../../../mediatek/frameworks/base/telephony/java/com/mediatek/telephony/PhoneNumberUtilsEx.java \
    ../../../mediatek/frameworks/base/text/java/com/mediatek/text/style/BackgroundImageSpan.java \
    ../../../mediatek/frameworks/common/src/com/mediatek/common/audioprofile/AudioProfileListener.java \
    ../../../mediatek/frameworks/common/src/com/mediatek/common/search/ISearchEngineManager.java \
    ../../../mediatek/frameworks/common/src/com/mediatek/common/search/SearchEngineInfo.java

# Specify directory of intermediate source files (e.g. AIDL) here.  
INTERNAL_API_ADDITIONAL_SRC_DIR := \
#    $(internal_docs_TEL_COMMON_AIDL_JAVA_DIR)/src/java/com/android/internal/telephony \
#    $(internal_docs_MTK_COMMON_AIDL_JAVA_DIR)/src/com/mediatek/common/dm
