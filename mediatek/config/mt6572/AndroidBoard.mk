LOCAL_PATH := $(call my-dir)

PRODUCT_COPY_FILES += $(LOCAL_PATH)/ProjectConfig.mk:system/data/misc/ProjectConfig.mk

include $(CLEAR_VARS)

TARGET_PROVIDES_INIT_RC := true

PRODUCT_COPY_FILES += $(LOCAL_PATH)/mtk-kpd.kl:system/usr/keylayout/mtk-kpd.kl \
                      $(LOCAL_PATH)/init.rc:root/init.rc \
                      $(LOCAL_PATH)/init.fon.rc:root/init.fon.rc \
                      $(LOCAL_PATH)/init.modem.rc:root/init.modem.rc \
                      $(LOCAL_PATH)/init.usb.rc:root/init.usb.rc \
                      $(LOCAL_PATH)/init.xlog.rc:root/init.xlog.rc \
                      $(LOCAL_PATH)/vold.fstab:system/etc/vold.fstab \
                      $(LOCAL_PATH)/vold.fstab.nand:system/etc/vold.fstab.nand \
                      $(LOCAL_PATH)/vold.fstab.fat.nand:system/etc/vold.fstab.fat.nand \
                      $(LOCAL_PATH)/player.cfg:system/etc/player.cfg \
			    $(LOCAL_PATH)/media_codecs.xml:system/etc/media_codecs.xml \
                      $(LOCAL_PATH)/mtk_omx_core.cfg:system/etc/mtk_omx_core.cfg \
                      $(LOCAL_PATH)/advanced_meta_init.rc:root/advanced_meta_init.rc \
                      $(LOCAL_PATH)/meta_init.rc:root/meta_init.rc \
                      $(LOCAL_PATH)/meta_init.modem.rc:root/meta_init.modem.rc \
                      $(LOCAL_PATH)/factory_init.rc:root/factory_init.rc \
                      $(LOCAL_PATH)/audio_policy.conf:system/etc/audio_policy.conf \
                      $(LOCAL_PATH)/init.protect.rc:root/init.protect.rc \
                      $(LOCAL_PATH)/ACCDET.kl:system/usr/keylayout/ACCDET.kl \
                      $(LOCAL_PATH)/partition_permission.sh:system/etc/partition_permission.sh \
                      $(LOCAL_PATH)/fstab:root/fstab

ifeq ($(MTK_KERNEL_POWER_OFF_CHARGING),yes)
PRODUCT_COPY_FILES += $(LOCAL_PATH)/init.charging.rc:root/init.charging.rc 
endif

_init_project_rc := $(MTK_ROOT_CONFIG_OUT)/init.project.rc
ifneq ($(wildcard $(_init_project_rc)),)
PRODUCT_COPY_FILES += $(_init_project_rc):root/init.project.rc
endif

_meta_init_project_rc := $(MTK_ROOT_CONFIG_OUT)/meta_init.project.rc
ifneq ($(wildcard $(_meta_init_project_rc)),)
PRODUCT_COPY_FILES += $(_meta_init_project_rc):root/meta_init.project.rc
endif

_advanced_meta_init_project_rc := $(MTK_ROOT_CONFIG_OUT)/advanced_meta_init.project.rc
ifneq ($(wildcard $(_advanced_meta_init_project_rc)),)
PRODUCT_COPY_FILES += $(_advanced_meta_init_project_rc):root/advanced_meta_init.project.rc
endif

_factory_init_project_rc := $(MTK_ROOT_CONFIG_OUT)/factory_init.project.rc
ifneq ($(wildcard $(_factory_init_project_rc)),)
PRODUCT_COPY_FILES += $(_factory_init_project_rc):root/factory_init.project.rc
endif

PRODUCT_COPY_FILES += $(strip \
                        $(foreach file,$(call wildcard2, $(LOCAL_PATH)/*.xml), \
                          $(addprefix $(LOCAL_PATH)/$(notdir $(file)):system/etc/permissions/,$(notdir $(file))) \
                         ) \
                       )


ifeq ($(HAVE_AEE_FEATURE),yes)
ifeq ($(PARTIAL_BUILD),true)
PRODUCT_COPY_FILES += $(LOCAL_PATH)/init.aee.customer.rc:root/init.aee.customer.rc
else
PRODUCT_COPY_FILES += $(LOCAL_PATH)/init.aee.mtk.rc:root/init.aee.mtk.rc
endif
endif

ifeq ($(strip $(HAVE_SRSAUDIOEFFECT_FEATURE)),yes)
  PRODUCT_COPY_FILES += $(LOCAL_PATH)/srs_processing.cfg:system/data/srs_processing.cfg
endif

ifeq ($(MTK_SHARED_SDCARD),yes)
ifeq ($(MTK_2SDCARD_SWAP),yes)
  PRODUCT_COPY_FILES += $(LOCAL_PATH)/init.ssd_nomuser.rc:root/init.ssd_nomuser.rc
else
  PRODUCT_COPY_FILES += $(LOCAL_PATH)/init.ssd.rc:root/init.ssd.rc
endif
else
  PRODUCT_COPY_FILES += $(LOCAL_PATH)/init.no_ssd.rc:root/init.no_ssd.rc
endif

include $(CLEAR_VARS)
LOCAL_SRC_FILES := mtk-kpd.kcm
include $(BUILD_KEY_CHAR_MAP)

##################################
$(call config-custom-folder,modem:modem)

##### INSTALL MODEM FIRMWARE #####
ifeq ($(strip $(MTK_ENABLE_MD1)),yes)

MD1_IMAGE_2G := $(wildcard $(LOCAL_PATH)/modem/modem_1_2g_n.img)
MD1_IMAGE_WG := $(wildcard $(LOCAL_PATH)/modem/modem_1_wg_n.img)
MD1_IMAGE_TG := $(wildcard $(LOCAL_PATH)/modem/modem_1_tg_n.img)

ifneq ($(strip $(MD1_IMAGE_2G)),)
include $(CLEAR_VARS)
LOCAL_MODULE := modem_1_2g_n.img
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

ifeq ($(MTK_MDLOGGER_SUPPORT),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := catcher_filter_1_2g_n.bin
LOCAL_MODULE_CLASS := ETC 
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
endif # MD1_IMAGE_2G

ifneq ($(strip $(MD1_IMAGE_WG)),)
include $(CLEAR_VARS)
LOCAL_MODULE := modem_1_wg_n.img
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

ifeq ($(MTK_MDLOGGER_SUPPORT),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := catcher_filter_1_wg_n.bin
LOCAL_MODULE_CLASS := ETC 
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
endif # MD1_IMAGE_WG

ifneq ($(strip $(MD1_IMAGE_TG)),)
include $(CLEAR_VARS)
LOCAL_MODULE := modem_1_tg_n.img
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

ifeq ($(MTK_MDLOGGER_SUPPORT),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := catcher_filter_1_tg_n.bin
LOCAL_MODULE_CLASS := ETC 
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
endif # MD1_IMAGE_TG

########INSTALL MODEM_DATABASE########
# for Modem database
ifeq ($(strip $(MTK_INCLUDE_MODEM_DB_IN_IMAGE)), yes)
  ifeq ($(filter generic banyan_addon banyan_addon_x86,$(PROJECT)),)
    MD1_DATABASE_FILE1 := $(foreach m,$(CUSTOM_MODEM),$(if $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_1_2g_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_1_2g_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomApp_*_1_2g_*)))
    $(info 2G database file of md1 = $(MD1_DATABASE_FILE1))
    MD1_DATABASE_FILENAME1 := $(notdir $(MD1_DATABASE_FILE1))
    ifneq ($(strip $(MD1_IMAGE_2G)),)
        $(TARGET_OUT_ETC)/firmware/modem_1_2g_n.img:$(PRODUCT_OUT)/system/etc/mddb/$(MD1_DATABASE_FILENAME1)
        $(eval $(call copy-one-file,$(LOCAL_PATH)/modem/$(MD1_DATABASE_FILENAME1),$(PRODUCT_OUT)/system/etc/mddb/$(MD1_DATABASE_FILENAME1)))
    endif

    MD1_DATABASE_FILE2 := $(foreach m,$(CUSTOM_MODEM),$(if $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_1_wg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_1_wg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomApp_*_1_wg_*)))
    $(info WG database file of md1 = $(MD1_DATABASE_FILE2)) 
    MD1_DATABASE_FILENAME2 := $(notdir $(MD1_DATABASE_FILE2))
    ifneq ($(strip $(MD1_IMAGE_WG)),)
        $(TARGET_OUT_ETC)/firmware/modem_1_wg_n.img:$(PRODUCT_OUT)/system/etc/mddb/$(MD1_DATABASE_FILENAME2)
        $(eval $(call copy-one-file,$(LOCAL_PATH)/modem/$(MD1_DATABASE_FILENAME2),$(PRODUCT_OUT)/system/etc/mddb/$(MD1_DATABASE_FILENAME2)))
    endif

    MD1_DATABASE_FILE3 := $(foreach m,$(CUSTOM_MODEM),$(if $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_1_tg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_1_tg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomApp_*_1_tg_*)))
    $(info TG database file of md1 = $(MD1_DATABASE_FILE3)) 
    MD1_DATABASE_FILENAME3 := $(notdir $(MD1_DATABASE_FILE3))
    ifneq ($(strip $(MD1_IMAGE_TG)),)
        $(TARGET_OUT_ETC)/firmware/modem_1_tg_n.img:$(PRODUCT_OUT)/system/etc/mddb/$(MD1_DATABASE_FILENAME3)
        $(eval $(call copy-one-file,$(LOCAL_PATH)/modem/$(MD1_DATABASE_FILENAME3),$(PRODUCT_OUT)/system/etc/mddb/$(MD1_DATABASE_FILENAME3)))
    endif
  endif
endif

endif # MTK_ENABLE_MD1=yes

ifeq ($(strip $(MTK_ENABLE_MD2)),yes)

MD2_IMAGE_2G := $(wildcard $(LOCAL_PATH)/modem/modem_2_2g_n.img)
MD2_IMAGE_WG := $(wildcard $(LOCAL_PATH)/modem/modem_2_wg_n.img)
MD2_IMAGE_TG := $(wildcard $(LOCAL_PATH)/modem/modem_2_tg_n.img)

ifneq ($(strip $(MD2_IMAGE_2G)),)
include $(CLEAR_VARS)
LOCAL_MODULE := modem_2_2g_n.img
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

ifeq ($(MTK_MDLOGGER_SUPPORT),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := catcher_filter_2_2g_n.bin
LOCAL_MODULE_CLASS := ETC 
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
endif # MD2_IMAGE_2G

ifneq ($(strip $(MD2_IMAGE_WG)),)
include $(CLEAR_VARS)
LOCAL_MODULE := modem_2_wg_n.img
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

ifeq ($(MTK_MDLOGGER_SUPPORT),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := catcher_filter_2_wg_n.bin
LOCAL_MODULE_CLASS := ETC 
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
endif # MD2_IMAGE_WG

ifneq ($(strip $(MD2_IMAGE_TG)),)
include $(CLEAR_VARS)
LOCAL_MODULE := modem_2_tg_n.img
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

ifeq ($(MTK_MDLOGGER_SUPPORT),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := catcher_filter_2_tg_n.bin
LOCAL_MODULE_CLASS := ETC 
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
LOCAL_SRC_FILES := modem/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
endif # MD2_IMAGE_TG

########INSTALL MODEM_DATABASE########
# for Modem database
ifeq ($(strip $(MTK_INCLUDE_MODEM_DB_IN_IMAGE)), yes)
  ifeq ($(filter generic banyan_addon banyan_addon_x86,$(PROJECT)),)
    MD2_DATABASE_FILE1 := $(foreach m,$(CUSTOM_MODEM),$(if $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_2_2g_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_2_2g_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomApp_*_2_2g_*)))
    $(info 2G database file of md2 = $(MD2_DATABASE_FILE1))
    MD2_DATABASE_FILENAME1 := $(notdir $(MD2_DATABASE_FILE1))
    ifneq ($(strip $(MD2_IMAGE_2G)),)
        $(TARGET_OUT_ETC)/firmware/modem_2_2g_n.img:$(PRODUCT_OUT)/system/etc/mddb/$(MD2_DATABASE_FILENAME1)
        $(eval $(call copy-one-file,$(LOCAL_PATH)/modem/$(MD2_DATABASE_FILENAME1),$(PRODUCT_OUT)/system/etc/mddb/$(MD2_DATABASE_FILENAME1)))
    endif

    MD2_DATABASE_FILE2 := $(foreach m,$(CUSTOM_MODEM),$(if $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_2_wg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_2_wg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomApp_*_2_wg_*)))
    $(info WG database file of md2 = $(MD2_DATABASE_FILE2)) 
    MD2_DATABASE_FILENAME2 := $(notdir $(MD2_DATABASE_FILE2))
    ifneq ($(strip $(MD2_IMAGE_WG)),)
        $(TARGET_OUT_ETC)/firmware/modem_2_wg_n.img:$(PRODUCT_OUT)/system/etc/mddb/$(MD2_DATABASE_FILENAME2)
        $(eval $(call copy-one-file,$(LOCAL_PATH)/modem/$(MD2_DATABASE_FILENAME2),$(PRODUCT_OUT)/system/etc/mddb/$(MD2_DATABASE_FILENAME2)))
    endif

    MD2_DATABASE_FILE3 := $(foreach m,$(CUSTOM_MODEM),$(if $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_2_tg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomAppSrcP_*_2_tg_*), \
       $(wildcard mediatek/custom/common/modem/$(strip $(m))/BPLGUInfoCustomApp_*_2_tg_*)))
    $(info TG database file of md2 = $(MD2_DATABASE_FILE3)) 
    MD2_DATABASE_FILENAME3 := $(notdir $(MD2_DATABASE_FILE3))
    ifneq ($(strip $(MD2_IMAGE_TG)),)
        $(TARGET_OUT_ETC)/firmware/modem_2_tg_n.img:$(PRODUCT_OUT)/system/etc/mddb/$(MD2_DATABASE_FILENAME3)
        $(eval $(call copy-one-file,$(LOCAL_PATH)/modem/$(MD2_DATABASE_FILENAME3),$(PRODUCT_OUT)/system/etc/mddb/$(MD2_DATABASE_FILENAME3)))
    endif
  endif
endif
#############################################

endif # MTK_ENABLE_MD2=yes

##### INSTALL ht120.mtc ##########

_ht120_mtc := $(MTK_ROOT_CONFIG_OUT)/configs/ht120.mtc
ifneq ($(wildcard $(_ht120_mtc)),)
PRODUCT_COPY_FILES += $(_ht120_mtc):system/etc/.tp/.ht120.mtc
endif

##################################

##### INSTALL thermal.conf ##########

_thermal_conf := $(MTK_ROOT_CONFIG_OUT)/configs/thermal.conf
ifneq ($(wildcard $(_thermal_conf)),)
PRODUCT_COPY_FILES += $(_thermal_conf):system/etc/.tp/thermal.conf
endif

##################################

##### INSTALL thermal.off.conf ##########

_thermal_off_conf := $(MTK_ROOT_CONFIG_OUT)/configs/thermal.off.conf
ifneq ($(wildcard $(_thermal_off_conf)),)
PRODUCT_COPY_FILES += $(_thermal_off_conf):system/etc/.tp/thermal.off.conf
endif

##################################

##### INSTALL throttle.sh ##########

_throttle_sh := $(MTK_ROOT_CONFIG_OUT)/configs/throttle.sh
ifneq ($(wildcard $(_throttle_sh)),)
PRODUCT_COPY_FILES += $(_throttle_sh):system/etc/throttle.sh
endif

##################################

_notimeout := $(MTK_ROOT_CONFIG_OUT)/configs/notimeout.sh
ifneq ($(wildcard $(_notimeout)),)
PRODUCT_COPY_FILES += $(_notimeout):system/etc/notimeout.sh
endif

