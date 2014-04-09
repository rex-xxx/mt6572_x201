# Copyright (C) 2011 The Android Open Source Project
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

LOCAL_PATH := vendor/nvidia/wingray

# NVIDIA blob necessary for wingray hardware
PRODUCT_COPY_FILES := \
    $(LOCAL_PATH)/proprietary/nvmm_aacdec.axf:system/etc/firmware/nvmm_aacdec.axf \
    $(LOCAL_PATH)/proprietary/nvddk_audiofx_core.axf:system/etc/firmware/nvddk_audiofx_core.axf \
    $(LOCAL_PATH)/proprietary/nvddk_audiofx_transport.axf:system/etc/firmware/nvddk_audiofx_transport.axf \
    $(LOCAL_PATH)/proprietary/nvmm_adtsdec.axf:system/etc/firmware/nvmm_adtsdec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_audiomixer.axf:system/etc/firmware/nvmm_audiomixer.axf \
    $(LOCAL_PATH)/proprietary/nvmm_h264dec.axf:system/etc/firmware/nvmm_h264dec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_jpegdec.axf:system/etc/firmware/nvmm_jpegdec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_jpegenc.axf:system/etc/firmware/nvmm_jpegenc.axf \
    $(LOCAL_PATH)/proprietary/nvmm_manager.axf:system/etc/firmware/nvmm_manager.axf \
    $(LOCAL_PATH)/proprietary/nvmm_mp2dec.axf:system/etc/firmware/nvmm_mp2dec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_mp3dec.axf:system/etc/firmware/nvmm_mp3dec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_mpeg4dec.axf:system/etc/firmware/nvmm_mpeg4dec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_reference.axf:system/etc/firmware/nvmm_reference.axf \
    $(LOCAL_PATH)/proprietary/nvmm_service.axf:system/etc/firmware/nvmm_service.axf \
    $(LOCAL_PATH)/proprietary/nvmm_sorensondec.axf:system/etc/firmware/nvmm_sorensondec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_sw_mp3dec.axf:system/etc/firmware/nvmm_sw_mp3dec.axf \
    $(LOCAL_PATH)/proprietary/nvmm_wavdec.axf:system/etc/firmware/nvmm_wavdec.axf \
    $(LOCAL_PATH)/proprietary/nvrm_avp.bin:system/etc/firmware/nvrm_avp.bin \
    $(LOCAL_PATH)/proprietary/libEGL_tegra.so:system/lib/egl/libEGL_tegra.so \
    $(LOCAL_PATH)/proprietary/libGLESv1_CM_tegra.so:system/lib/egl/libGLESv1_CM_tegra.so \
    $(LOCAL_PATH)/proprietary/libGLESv2_tegra.so:system/lib/egl/libGLESv2_tegra.so \
    $(LOCAL_PATH)/proprietary/gralloc.tegra.so:system/lib/hw/gralloc.tegra.so \
    $(LOCAL_PATH)/proprietary/hwcomposer.tegra.so:system/lib/hw/hwcomposer.tegra.so \
    $(LOCAL_PATH)/proprietary/libcgdrv.so:system/lib/libcgdrv.so \
    $(LOCAL_PATH)/proprietary/libnvddk_2d.so:system/lib/libnvddk_2d.so \
    $(LOCAL_PATH)/proprietary/libnvddk_2d_v2.so:system/lib/libnvddk_2d_v2.so \
    $(LOCAL_PATH)/proprietary/libnvddk_audiofx.so:system/lib/libnvddk_audiofx.so \
    $(LOCAL_PATH)/proprietary/libnvdispatch_helper.so:system/lib/libnvdispatch_helper.so \
    $(LOCAL_PATH)/proprietary/libnvdispmgr_d.so:system/lib/libnvdispmgr_d.so \
    $(LOCAL_PATH)/proprietary/libnvmm.so:system/lib/libnvmm.so \
    $(LOCAL_PATH)/proprietary/libnvmm_camera.so:system/lib/libnvmm_camera.so \
    $(LOCAL_PATH)/proprietary/libnvmm_contentpipe.so:system/lib/libnvmm_contentpipe.so \
    $(LOCAL_PATH)/proprietary/libnvmm_image.so:system/lib/libnvmm_image.so \
    $(LOCAL_PATH)/proprietary/libnvmm_manager.so:system/lib/libnvmm_manager.so \
    $(LOCAL_PATH)/proprietary/libnvmm_service.so:system/lib/libnvmm_service.so \
    $(LOCAL_PATH)/proprietary/libnvmm_tracklist.so:system/lib/libnvmm_tracklist.so \
    $(LOCAL_PATH)/proprietary/libnvmm_utils.so:system/lib/libnvmm_utils.so \
    $(LOCAL_PATH)/proprietary/libnvmm_video.so:system/lib/libnvmm_video.so \
    $(LOCAL_PATH)/proprietary/libnvomx.so:system/lib/libnvomx.so \
    $(LOCAL_PATH)/proprietary/libnvomxilclient.so:system/lib/libnvomxilclient.so \
    $(LOCAL_PATH)/proprietary/libnvos.so:system/lib/libnvos.so \
    $(LOCAL_PATH)/proprietary/libnvrm.so:system/lib/libnvrm.so \
    $(LOCAL_PATH)/proprietary/libnvrm_channel.so:system/lib/libnvrm_channel.so \
    $(LOCAL_PATH)/proprietary/libnvrm_graphics.so:system/lib/libnvrm_graphics.so \
    $(LOCAL_PATH)/proprietary/libnvsm.so:system/lib/libnvsm.so \
    $(LOCAL_PATH)/proprietary/libnvwsi.so:system/lib/libnvwsi.so \
    $(LOCAL_PATH)/proprietary/libstagefrighthw.so:system/lib/libstagefrighthw.so

PRODUCT_PACKAGES := \
    libpkip
