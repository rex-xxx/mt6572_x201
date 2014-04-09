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
# Copyright (C) 2007 The Android Open Source Project
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

PRODUCT_BRAND := generic
PRODUCT_DEVICE := generic
PRODUCT_NAME := core

PRODUCT_PROPERTY_OVERRIDES := \
    ro.config.notification_sound=Proxima.ogg \
    ro.config.alarm_alert=Alarm_Classic.ogg \
    ro.config.ringtone=Backroad.ogg

PRODUCT_PACKAGES += \
    ApplicationsProvider \
    BackupRestoreConfirmation \
    Browser \
    Contacts \
    ContactsProvider \
    DefaultContainerService \
    DownloadProvider \
    DownloadProviderUi \
    HTMLViewer \
    Home \
    KeyChain \
    MediaProvider \
    PackageInstaller \
    PicoTts \
    SettingsProvider \
    SharedStorageBackup \
    TelephonyProvider \
    UserDictionaryProvider \
    VpnDialogs \
    abcc \
    apache-xml \
    atrace \
    bouncycastle \
    bu \
    cacerts \
    com.android.location.provider \
    com.android.location.provider.xml \
    core \
    core-junit \
    dalvikvm \
    dexdeps \
    dexdump \
    dexlist \
    dexopt \
    dmtracedump \
    drmserver \
    dx \
    ext \
    framework-res \
    hprof-conv \
    icu.dat \
    installd \
    ip \
    ip-up-vpn \
    ip6tables \
    iptables \
    keystore \
    keystore.default \
    libandroidfw \
    libOpenMAXAL \
    libOpenSLES \
    libaudiopreprocessing \
    libaudioutils \
    libbcc \
    libcrypto \
    libdownmix \
    libdvm \
    libdrmframework \
    libdrmframework_jni \
    libexpat \
    libfilterfw \
    libfilterpack_imageproc \
    libgabi++ \
    libicui18n \
    libicuuc \
    libjavacore \
    libkeystore \
    libmdnssd \
    libnativehelper \
    libnfc_ndef \
    libportable \
    libpowermanager \
    libspeexresampler \
    libsqlite_jni \
    libssl \
    libstagefright \
    libstagefright_chromium_http \
    libstagefright_soft_aacdec \
    libstagefright_soft_aacenc \
    libstagefright_soft_amrdec \
    libstagefright_soft_amrnbenc \
    libstagefright_soft_amrwbenc \
    libstagefright_soft_flacenc \
    libstagefright_soft_g711dec \
    libstagefright_soft_h264dec \
    libstagefright_soft_h264enc \
    libstagefright_soft_mp3dec \
    libstagefright_soft_mpeg4dec \
    libstagefright_soft_mpeg4enc \
    libstagefright_soft_vorbisdec \
    libstagefright_soft_vpxdec \
    libstagefright_soft_rawdec \
    libvariablespeed \
    libwebrtc_audio_preprocessing \
    libwilhelm \
    libz \
    make_ext4fs \
    mdnsd \
    requestsync \
    screencap \
    jpe_tool \
    libnativecheck-jni \
    sensorservice \
    lint \
    uiautomator \
    telephony-common \
    mms-common \
    zoneinfo.dat \
    zoneinfo.idx \
    zoneinfo.version \
    wpa_supplicant \
    wpa_supplicant.conf \
    p2p_supplicant.conf \
    hald \
    hdc \
    mediatek-res \
    mediatek-common \
    mediatek-framework \
    mediatek-op \
    CustomProperties \
    mediatek-telephony-common

ifneq ($(strip $(MTK_LCA_RAM_OPTIMIZE)), yes)
  PRODUCT_PACKAGES += BasicDreams \
    PhotoTable
endif

ifeq ($(strip $(MTK_AGPS_APP)), yes)
  PRODUCT_PACKAGES += mtk_agpsd \
    libssladp \
    libagpssupl
endif    

#ifneq ($(filter secondary-framework,$(ALL_MODULES)),)
  PRODUCT_PACKAGES += secondary-framework
#endif

PRODUCT_COPY_FILES += \
    system/core/rootdir/init.trace.rc:root/init.trace.rc \
#    system/core/rootdir/init.usb.rc:root/init.usb.rc \

# host-only dependencies
ifeq ($(WITH_HOST_DALVIK),true)
    PRODUCT_PACKAGES += \
        apache-xml-hostdex \
        bouncycastle-hostdex \
        core-hostdex \
        libcrypto \
        libexpat \
        libicui18n \
        libicuuc \
        libjavacore \
        libssl \
        libz-host \
        dalvik \
        zoneinfo-host.dat \
        zoneinfo-host.idx \
        zoneinfo-host.version
endif

ifeq ($(strip $(MTK_FM_SUPPORT)), yes)
  PRODUCT_PACKAGES += FMRadio
endif

ifeq ($(HAVE_SELINUX),true)
    PRODUCT_PACKAGES += \
        sepolicy \
        file_contexts \
        seapp_contexts \
        property_contexts \
        mac_permissions.xml
endif

ifeq (MT6620_FM,$(strip $(MTK_FM_CHIP)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.chip=1
endif

$(call inherit-product, $(SRC_TARGET_DIR)/product/base.mk)

ifeq (MT6626_FM,$(strip $(MTK_FM_CHIP)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.chip=2
endif

ifeq (MT6628_FM,$(strip $(MTK_FM_CHIP)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.chip=3
endif
