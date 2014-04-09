# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

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

ifneq ($(TARGET_PRODUCT),generic)
ifneq ($(TARGET_SIMULATOR),true)
ifeq ($(TARGET_ARCH),arm)
ifneq ($(MTK_EMULATOR_SUPPORT),yes)

# factory program
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

GENERIC_CUSTOM_PATH := mediatek/custom/generic/factory
HAVE_CUST_FOLDER := $(shell test -d mediatek/custom/$(TARGET_PRODUCT)/factory && echo yes)

ifeq ($(HAVE_CUST_FOLDER),yes)
CUSTOM_PATH := mediatek/custom/out/$(FULL_PROJECT)/factory
else
CUSTOM_PATH := $(GENERIC_CUSTOM_PATH)
endif

commands_factory_local_path := $(LOCAL_PATH)

CORE_SRC_FILES := \
	src/factory.c \
	src/util/at_command.c \
	src/util/utils.c \
	src/util/uart_op.c
TEST_SRC_FILES := \
	src/test/ftm.c\
	src/test/ftm_mods.c\
	src/test/ftm_strobe.cpp\
 	src/test/ftm_keys.c\
 	src/test/ftm_lcd.c\
 	src/test/ftm_led.c\
 	src/test/ftm_memcard.c\
	src/test/ftm_rtc.cpp\
	src/test/ftm_gsensor.c\
	src/test/ftm_gs_cali.c\
 	src/test/ftm_msensor.c\
 	src/test/ftm_touch.c\
 	src/test/ftm_touch_auto.c\
	src/test/ftm_vibrator.c\
 	src/test/ftm_headset.cpp\
 	src/test/ftm_usb.cpp\
 	src/test/ftm_otg.cpp\
	src/test/ftm_idle.c \
 	src/test/ftm_jogball.c \
 	src/test/ftm_ofn.c \
	src/test/ftm_alsps.c \
	src/test/ftm_barometer.c \
 	src/test/ftm_gyroscope.c \
 	src/test/ftm_gyro_cali.c	\
	src/test/ftm_sim.c \
	src/test/ftm_signaltest.c \
	src/test/ftm_speaker.cpp	\
	src/test/ftm_emi.c \
	src/test/ftm_camera.cpp

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
TEST_SRC_FILES += \
	src/test/ftm_audio.cpp\
	src/test/ftm_audio_debug.cpp\
	src/test/ftm_audio_Common.cpp
endif

ifeq ($(MTK_WLAN_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_wifi.c \
	src/test/ftm_wifi_op.c
endif

ifeq ($(HAVE_CMMB_FEATURE),yes)
TEST_SRC_FILES += \
	src/test/ftm_cmmb_impl.cpp \
	src/test/ftm_cmmb.c
endif

ifeq ($(MTK_TVOUT_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_tvout.c
endif
ifeq ($(MTK_GPS_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_gps.c
endif

ifeq ($(MTK_NFC_SUPPORT),yes)
ifeq ($(MTK_NFC_MT6605), yes)
TEST_SRC_FILES += \
	src/test/ftm_nfc.c
else
ifeq ($(MTK_NFC_MSR3110), yes)
TEST_SRC_FILES += \
        src/test/ftm_nfc_msr3110.c
endif
endif
endif

ifeq ($(MTK_FM_SUPPORT), yes)
ifeq ($(MTK_FM_RX_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_fm.c
endif
ifeq ($(MTK_FM_TX_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_fmtx.c
endif
endif

ifeq ($(HAVE_MATV_FEATURE),yes)

ifeq ($(MTK_MATV_ANALOG_SUPPORT),yes)
LOCAL_CFLAGS += \
  -DANALOG_AUDIO
endif

TEST_SRC_FILES += \
  src/test/ftm_matv_auto.cpp \
  src/test/ftm_matv_preview.cpp \
  src/test/ftm_matv_common.cpp

# copy resources to rootfs/res for non-factory image mode
DIRS := $(TARGET_OUT)/res/matv
$(DIRS):
	echo Directory: $@
	mkdir -p $@
ALL_PREBUILT += $(DIRS)

copy_to := \
  res/matv/matv_pattern.jpg

copy_from := \
  factory/

copy_to := $(addprefix $(TARGET_OUT)/,$(copy_to))
copy_from_custom := $(addprefix $(MTK_ROOT_CUSTOM_OUT)/,$(copy_from))

$(copy_to) : $(TARGET_OUT)/% : $(copy_from_custom)/% | $(ACP)
	@if [ ! -h $(TARGET_ROOT_OUT)/res ]; then mkdir -p $(TARGET_ROOT_OUT); ln -s /system/res $(TARGET_ROOT_OUT)/res || echo "Makelink failed !!" ;fi
	$(transform-prebuilt-to-target)
ALL_PREBUILT += $(copy_to)

endif

ifeq ($(MTK_BT_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_bt.c\
	src/test/ftm_bt_op.c
endif

ifeq ($(MTK_EMMC_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_emmc.c
else
TEST_SRC_FILES += \
	src/test/ftm_flash.c
endif

ifeq ($(MTK_NCP1851_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_battery_ncp1851.c
else
  ifeq ($(MTK_BQ24196_SUPPORT),yes)
  TEST_SRC_FILES += \
	  src/test/ftm_battery_bq24196.c
else
TEST_SRC_FILES += \
  src/test/ftm_battery.c
endif
endif

ifeq ($(MTK_HDMI_SUPPORT), yes)
TEST_SRC_FILES += \
  src/test/ftm_hdmi.c
endif

HAVE_CUST_INC_PATH := $(shell test -d mediatek/custom/$(TARGET_PRODUCT)/factory/inc && echo yes)

ifeq ($(HAVE_CUST_INC_PATH),yes)
  $(info Apply factory custom include path for $(TARGET_DEVICE))
else
  $(info No factory custom include path for $(TARGET_DEVICE))
endif

ifeq ($(HAVE_CUST_INC_PATH),yes)
	LOCAL_CUST_INC_PATH := $(CUSTOM_PATH)/inc
else
	LOCAL_CUST_INC_PATH := $(GENERIC_CUSTOM_PATH)/inc
endif

ifeq ($(MTK_SENSOR_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_SUPPORT

ifeq ($(MTK_SENSOR_MAGNETOMETER),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_MAGNETOMETER
endif

ifeq ($(MTK_SENSOR_ACCELEROMETER),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_ACCELEROMETER
endif

ifeq ($(MTK_SENSOR_ALSPS),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_ALSPS
endif

ifeq ($(MTK_SENSOR_GYROSCOPE),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_GYROSCOPE
endif

endif

ifeq ($(MTK_TB_WIFI_3G_MODE),3GDATA_ONLY)
LOCAL_CFLAGS += \
    -DMTK_TB_WIFI_3G_MODE_3GDATA_ONLY
endif

ifeq ($(MTK_TB_WIFI_3G_MODE),3GDATA_SMS)
LOCAL_CFLAGS += \
    -DMTK_TB_WIFI_3G_MODE_3GDATA_SMS
endif

ifeq ($(MTK_TB_WIFI_3G_MODE),WIFI_ONLY)
LOCAL_CFLAGS += \
    -DMTK_TB_WIFI_ONLY
endif


ifeq ($(MTK_NFC_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_NFC_SUPPORT_FM
endif

ifeq (yes,$(GEMINI))
    LOCAL_CFLAGS += -DGEMINI
endif

ifeq ($(MTK_DIGITAL_MIC_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_DIGITAL_MIC_SUPPORT
endif

ifeq ($(MTK_VIBSPK_SUPPORT),yes)
   LOCAL_CFLAGS += -DMTK_VIBSPK_SUPPORT
endif
#MTKBEGIN   [mtk80625][DualTalk]
ifeq ($(MTK_DT_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_DT_SUPPORT
endif
#MTKEND   [mtk80625][DualTalk]

ifeq ($(MTK_HDMI_SUPPORT), yes)
	LOCAL_CFLAGS += -DMTK_HDMI_SUPPORT
endif

include $(LOCAL_PATH)/src/miniui/font.mk

LOCAL_SRC_FILES := \
	$(CORE_SRC_FILES)\
	$(TEST_SRC_FILES)

LOCAL_C_INCLUDES:= \
	$(LOCAL_PATH)/inc/ \
	mediatek/custom/common/factory/inc \
	mediatek/frameworks/av/media/libs \
	$(LOCAL_CUST_INC_PATH) \
	$(MTK_PATH_SOURCE)/external/mhal/src/custom/inc \
	$(MTK_PATH_SOURCE)/external/mhal/inc \
	frameworks/av/include/media \
  ./$(MTK_PATH_SOURCE)/platform/common/hardware/audio/include \
  ./$(MTK_PATH_PLATFORM)/hardware/audio/include \
	$(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio/aud_drv \
	$(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio/include \
	$(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio \
	$(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio/aud_drv \
	$(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio/include \
	$(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio \
	$(call include-path-for, audio-utils) \
	$(TOP)/$(MTK_PATH_SOURCE)/platform/common/hardware/audio/speech_driver \
	$(TOP)/$(MTK_PATH_PLATFORM)/hardware/audio/speech_driver \
    $(call include-path-for, audio-effects) \
	$(MTK_PATH_SOURCE)/external/audiocustparam \
	$(MTK_PATH_SOURCE)/frameworks-ext/av/include/media \
	$(MTK_PATH_SOURCE)/frameworks-ext/av/include \
	$(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
	$(MTK_PATH_CUSTOM)/cgen/cfgdefault \
	$(MTK_PATH_CUSTOM)/kernel/imgsensor/inc \
	$(MTK_PATH_CUSTOM)/cgen/inc \
	$(MTK_PATH_CUSTOM)/cgen/inc \
	$(MTK_PATH_CUSTOM)/audioflinger/audio \
	$(MTK_PATH_SOURCE)/external/nvram/libnvram \
	$(MTK_PATH_SOURCE)/external/AudioSpeechEnhancement/inc \
	$(MTK_PATH_SOURCE)/external/matvctrl \
	$(MTK_PATH_SOURCE)/external/fft \
	$(MTK_PATH_SOURCE)/external/sensor-tools \
	$(MTK_PATH_SOURCE)/external/aee/binary/inc \
	$(MTK_PATH_SOURCE)/kernel/drivers/video \
	$(MTK_PATH_SOURCE)/kernel/drivers/hdmitx \
	$(MTK_PATH_CUSTOM)/hal/inc \
	$(MTK_PATH_SOURCE)/external/audiodcremoveflt \
	$(MTK_ROOT_CUSTOM_OUT)/kernel/dct \
	system/extras/ext4_utils \
	$(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
	$(MTK_PATH_SOURCE)/external/cvsd_plc_codec \
	$(TOP)/$(MTK_PATH_PLATFORM)/hardware/camera/core/featureio/drv/inc \
	mediatek/external/dfo/featured \
	$(TARGET_OUT_HEADERS)/dfo

LOCAL_MODULE := factory

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_LIBRARIES :=
LOCAL_STATIC_LIBRARIES += libminzip libunz libmtdutil libmincrypt libm
LOCAL_STATIC_LIBRARIES += libminiui libpixelflinger_static libpng libz libcutils
LOCAL_STATIC_LIBRARIES += libstdc++ libc
LOCAL_STATIC_LIBRARIES += libfft

ifeq ($(MTK_PLATFORM),MT6573)
# FIXME {
LOCAL_SHARED_LIBRARIES:= libc libcutils libmedia libhardware_legacy libhwm libacdk libcameracustom libnvram libdl libaudiocustparam libaed

LOCAL_C_INCLUDES += \
   mediatek/source/hardware/mtk/audio/mt657x_drv \
   $(MTK_PATH_SOURCE)/external/mhal/src/core/scenario/6573/cameradebug/inc \
   $(MTK_PATH_SOURCE)/external/mhal/src/lib/inc/acdk

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
LOCAL_SHARED_LIBRARIES += libaudio.primary.default
endif

LOCAL_CFLAGS += -DMT6573

# FIXME }
endif

## ==> HAVE_CMMB_FEATURE
ifeq ($(HAVE_CMMB_FEATURE),yes)

ifeq ($(MTK_PLATFORM),MT6577)
LOCAL_C_INCLUDES += \
   $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/include \
   $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb

ifneq (,$(findstring Innofidei,$(MTK_CMMB_CHIP)))
LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/innofidei
LOCAL_CFLAGS += -DCMMB_CHIP_INNO
# siano chip used
else
LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/siano \
        $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/siano/hostlib \
        $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/siano/osw/include \
        $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/siano/osw/linux \
        $(MTK_PATH_SOURCE)/platform/mt6577/external/meta/cmmb/siano/siano_appdriver_new
endif
endif      # MT6577

ifeq ($(MTK_PLATFORM),MT6589)
LOCAL_C_INCLUDES += \
   $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/include \
   $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb

ifneq (,$(findstring Innofidei,$(MTK_CMMB_CHIP)))
LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/innofidei
LOCAL_CFLAGS += -DCMMB_CHIP_INNO
# siano chip used
else
LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/siano \
        $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/siano/hostlib \
        $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/siano/osw/include \
        $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/siano/osw/linux \
        $(MTK_PATH_SOURCE)/platform/mt6589/external/meta/cmmb/siano/siano_appdriver_new
endif
endif      # MT6589

ifeq ($(MTK_PLATFORM),MT6572)
LOCAL_C_INCLUDES += \
   $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/include \
   $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb

ifneq (,$(findstring Innofidei,$(MTK_CMMB_CHIP)))
LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/innofidei
LOCAL_CFLAGS += -DCMMB_CHIP_INNO
# siano chip used
else
LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/siano \
        $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/siano/hostlib \
        $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/siano/osw/include \
        $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/siano/osw/linux \
        $(MTK_PATH_SOURCE)/platform/mt6572/external/meta/cmmb/siano/siano_appdriver_new
endif
endif      # MT6572

endif
## <== HAVE_CMMB_FEATURE

ifeq ($(MTK_PLATFORM),MT6575)
# FIXME {

LOCAL_SHARED_LIBRARIES:= libc libcutils libnvram libdl libhwm libmhal libaudiocustparam libext4_utils
LOCAL_SHARED_LIBRARIES += libacdk libcameracustom

LOCAL_C_INCLUDES += \
   mediatek/source/hardware/mtk/audio/mt657x_drv \
   $(TOP)/$(MTK_PATH_PLATFORM)/hardware/camera/core/cameradebug/inc

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
LOCAL_SHARED_LIBRARIES += libaudio.primary.default
endif

LOCAL_CFLAGS += -DMT6575
endif

ifeq ($(MTK_PLATFORM),MT6577)
# FIXME {
LOCAL_SHARED_LIBRARIES:= libc libcutils libnvram libdl libhwm libmhal libaudiocustparam libext4_utils
LOCAL_SHARED_LIBRARIES += libacdk libcameracustom

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
LOCAL_SHARED_LIBRARIES += libaudio.primary.default
endif

LOCAL_C_INCLUDES += \
   $(TOP)/$(MTK_PATH_PLATFORM)/hardware/camera/core/cameradebug/inc

LOCAL_CFLAGS += -DMT6577
# FIXME }
endif

ifeq ($(MTK_PLATFORM),MT6589)
# FIXME {
LOCAL_SHARED_LIBRARIES:= libc libcutils libnvram libdl libhwm libaudiocustparam libext4_utils liblog libdpframework libdpframework_plat libdpframework_os libutils libmmfactoryutil

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
LOCAL_SHARED_LIBRARIES += libaudio.primary.default
endif

LOCAL_CFLAGS += -DMT6589
# FIXME }
endif

ifeq ($(MTK_PLATFORM),MT6582)
# FIXME {
LOCAL_SHARED_LIBRARIES:= libc libcutils libnvram libdl libhwm libaudiocustparam libext4_utils

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
LOCAL_SHARED_LIBRARIES += libaudio.primary.default
endif

LOCAL_CFLAGS += -DMT6582
# FIXME }
endif

ifeq ($(MTK_PLATFORM),MT6572)
# FIXME {
LOCAL_SHARED_LIBRARIES:= libc libcutils libnvram libdl libhwm libaudiocustparam libext4_utils libfeatureio

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
LOCAL_SHARED_LIBRARIES += libaudio.primary.default
endif

LOCAL_CFLAGS += -DMT6572
# FIXME }
endif

#camera{

LOCAL_SHARED_LIBRARIES += libacdk

LOCAL_C_INCLUDES += \
   $(MTK_PATH_SOURCE)/hardware/camera/inc/acdk \

#}camera
ifeq ($(TELEPHONY_DFOSET),yes)
LOCAL_SHARED_LIBRARIES += libdfo
endif
LOCAL_SHARED_LIBRARIES += libutils
ifeq ($(HAVE_MATV_FEATURE),yes)
LOCAL_STATIC_LIBRARIES +=
LOCAL_SHARED_LIBRARIES += libmatv_cust
LOCAL_CFLAGS += -DHAVE_MATV_FEATURE

LOCAL_SHARED_LIBRARIES += libjpeg

LOCAL_C_INCLUDES += \
  external/jpeg

endif
LOCAL_SHARED_LIBRARIES += libmtkplayer

ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
LOCAL_CFLAG += -DMTK_DUAL_MIC_SUPPORT
LOCAL_CFLAGS += -DFEATURE_FTM_ACSLB
endif

ifeq ($(CUSTOM_KERNEL_SOUND),amp_6329pmic_spk)
LOCAL_CFLAGS += -DFEATURE_FTM_SPK_OC
endif

ifeq ($(HAVE_CMMB_FEATURE),yes)
LOCAL_CFLAGS += -DHAVE_CMMB_FEATURE
endif

ifneq ($(RECEIVER_HEADSET_AUTOTEST),no)
LOCAL_CFLAGS += -DRECEIVER_HEADSET_AUTOTEST
endif
#ifeq ($(TARGET_NO_FACTORYIMAGE),true)
#  LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT_SBIN)
#endif

#ifeq ($(MTK_WLAN_SUPPORT),yes)
#LOCAL_CFLAGS += \
#    -DWIFI_DRIVER_MODULE_NAME=\"wlan_$(shell echo $(strip $(MTK_WLAN_CHIP)) | tr A-Z a-z)\"
#endif

ifneq ($(DISABLE_EARPIECE),yes)
LOCAL_CFLAGS += -DFEATURE_FTM_RECEIVER
endif

include $(BUILD_EXECUTABLE)

# copy etc/init.rc to rootfs/factory_init.rc for non-factory image mode
#$(shell cp $(CUSTOM_PATH)/init.rc $(LOCAL_PATH)/etc/init.rc)
#$(shell chmod 777 $(LOCAL_PATH)/etc/init.rc)

#ifeq ($(TARGET_NO_FACTORYIMAGE),true)
#$(shell echo -e "\nservice factory /system/bin/logwrapper /system/bin/factory\n    oneshot" >> $(LOCAL_PATH)/etc/init.rc)
#include $(CLEAR_VARS)
#LOCAL_MODULE := init.factory.rc
#LOCAL_MODULE_TAGS := optional

#LOCAL_MODULE_CLASS := ETC
#OCAL_SRC_FILES := etc/init.rc
#LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)
#include $(BUILD_PREBUILT)
#else
#$(shell echo -e "\nservice factory /system/bin/logwrapper /sbin/factory\n    oneshot" >> $(LOCAL_PATH)/etc/init.rc)
#endif


include $(commands_factory_local_path)/src/mtdutil/Android.mk
include $(commands_factory_local_path)/src/miniui/Android.mk
include $(commands_factory_local_path)/src/mmutil/Android.mk

# audio resource

LOCAL_PATH :=  $(CUSTOM_PATH)
include $(CLEAR_VARS)

# copy resources to rootfs/res for test pattern
DIRS := $(TARGET_OUT)/res/sound
$(DIRS):
	echo Directory: $@
	mkdir -p $@
ALL_PREBUILT += $(DIRS)

copy_from := \
	res/sound/testpattern1.wav \
	res/sound/ringtone.wav

copy_to := $(addprefix $(TARGET_OUT)/,$(copy_from))
copy_from := $(addprefix $(LOCAL_PATH)/,$(copy_from))
$(copy_to) : $(TARGET_OUT)/% : $(LOCAL_PATH)/% | $(ACP)
	@if [ ! -h $(TARGET_ROOT_OUT)/res ]; then mkdir -p $(TARGET_ROOT_OUT); ln -s /system/res $(TARGET_ROOT_OUT)/res || echo "Makelink failed !!" ;fi
	$(transform-prebuilt-to-target)
ALL_PREBUILT += $(copy_to)


#################################################################
LOCAL_PATH :=  $(CUSTOM_PATH)

include $(CLEAR_VARS)
ifeq ($(TARGET_NO_FACTORYIMAGE),true)

# copy resources to rootfs/res for non-factory image mode
DIRS := $(TARGET_OUT)/res/images
$(DIRS):
	echo Directory: $@
	mkdir -p $@
ALL_PREBUILT += $(DIRS)

copy_from := \
	res/images/lcd_test_00.png \
	res/images/lcd_test_01.png \
	res/images/lcd_test_02.png \
	res/images/lcd_test_03.png \
	res/images/lcd_test_04.png  \
	res/images/lcd_test_05.png \
	res/images/lcd_test_06.png \
	res/images/lcd_test_07.png

copy_to := $(addprefix $(TARGET_OUT)/,$(copy_from))
copy_from := $(addprefix $(LOCAL_PATH)/,$(copy_from))
$(copy_to) : $(TARGET_OUT)/% : $(LOCAL_PATH)/% | $(ACP)
	@if [ ! -h $(TARGET_ROOT_OUT)/res ]; then mkdir -p $(TARGET_ROOT_OUT); ln -s /system/res $(TARGET_ROOT_OUT)/res || echo "Makelink failed !!" ;fi
	$(transform-prebuilt-to-target)
ALL_PREBUILT += $(copy_to)
else
$(shell if [ -h $(TARGET_ROOT_OUT)/res ] ;then rm -f $(TARGET_ROOT_OUT)/res ;  fi  )
endif
include $(CLEAR_VARS)
include $(MTK_PATH_SOURCE)/factory/src/miniui/font.mk
LOCAL_MODULE := factory.ini
LOCAL_MODULE_TAGS := optional

ifeq ($(MTK_FACTORY_MODE_IN_GB2312),yes)
	LOCAL_SRC_FILES := factory.chn.ini
else
	LOCAL_SRC_FILES := factory.ini
endif

LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH += $(PRODUCT_OUT)
LOCAL_MODULE_PATH += $(TARGET_OUT_ETC)
include $(BUILD_PREBUILT)
##################################################################
endif   # TARGET_ARCH == arm
endif	# !TARGET_SIMULATOR
endif
endif
