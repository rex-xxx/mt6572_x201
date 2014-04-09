#
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

$(call inherit-product, device/moto/wingray/device_base.mk)

PRODUCT_COPY_FILES += \
    device/moto/stingray/oem-iptables-init.sh:system/bin/oem-iptables-init.sh \
    device/moto/stingray/ril/tty2ttyd:system/bin/tty2ttyd \
    device/moto/stingray/ril/base64:system/bin/base64 \
    device/moto/stingray/ril/libb64.so:system/lib/libb64.so \
    device/moto/stingray/ril/extract-embedded-files:system/bin/extract-embedded-files \
    device/moto/stingray/ril/vril-dump:system/bin/vril-dump \
    device/moto/stingray/ril/logcatd:system/bin/logcatd \
    device/moto/stingray/ril/logcatd-blan:system/bin/logcatd-blan \
    device/sample/etc/apns-conf_verizon.xml:system/etc/apns-conf.xml

ifneq ($(AP_RIL_BLDSRC),1)
PRODUCT_COPY_FILES += \
    device/moto/stingray/ril/libmoto_cdma_ril.so:system/lib/libmoto_cdma_ril.so \
    device/moto/stingray/ril/libmoto_rds_ril.so:system/lib/libmoto_rds_ril.so \
    device/moto/stingray/ril/libmoto_qmi_ril.so:system/lib/libmoto_qmi_ril.so \
    device/moto/stingray/ril/libmoto_nwif_ril.so:system/lib/libmoto_nwif_ril.so \
    device/moto/stingray/ril/libmoto_intfutil_ril.so:system/lib/libmoto_intfutil_ril.so \
    device/moto/stingray/ril/libmoto_db_ril.so:system/lib/libmoto_db_ril.so \
    device/moto/stingray/ril/libmoto_mm_ril.so:system/lib/libmoto_mm_ril.so \
    device/moto/stingray/ril/libmoto_mdmctrl.so:system/lib/libmoto_mdmctrl.so \
    device/moto/stingray/ril/libbabysit.so:system/lib/libbabysit.so \
    device/moto/stingray/ril/mm-wrigley-qc-dump.sh:system/bin/mm-wrigley-qc-dump.sh \
    device/moto/stingray/ril/wrigley-dump.sh:system/bin/wrigley-dump.sh \
    device/moto/stingray/ril/wrigley-diag.sh:system/bin/wrigley-diag.sh \
    device/moto/stingray/ril/wrigley-iptables.sh:system/bin/wrigley-iptables.sh \
    device/moto/stingray/ril/wrigley-fetch-mpr.sh:system/bin/wrigley-fetch-mpr.sh
ifneq ($(TARGET_BUILD_VARIANT),user)
PRODUCT_COPY_FILES += \
    device/moto/stingray/ril/qbp-dump.sh:system/bin/qbp-dump.sh \
    device/moto/stingray/ril/qbp-apr-dump.sh:system/bin/qbp-apr-dump.sh \
    device/moto/stingray/ril/qbpfs:system/bin/qbpfs
endif
endif

PRODUCT_PACKAGES += \
    nc \
    tty2ttyd \
    base64 \
    libb64 \
    extract-embedded-files \
    libmoto_cdma_ril \
    libmoto_rds_ril \
    libmoto_qmi_ril \
    libmoto_nwif_ril \
    libmoto_intfutil_ril.so \
    libmoto_lte_ril \
    libmoto_db_ril \
    libmoto_mm_ril \
    libmoto_mdmctrl \
    libbabysit \
    logcatd \
    logcatd-blan \
    mm-wrigley-qc-dump \
    wrigley-dump \
    wrigley-diag \
    wrigley-iptables \
    wrigley-fetch-mpr

# Overrides
DEVICE_PACKAGE_OVERLAYS := \
    device/moto/stingray/overlay device/moto/wingray/overlay
