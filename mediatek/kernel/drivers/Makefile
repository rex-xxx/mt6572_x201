include $(MTK_PATH_BUILD)/common.mk
#$(call all-subdir-src-or-makefile)

obj-y   +=  aee/
obj-y   +=  ccci/
obj-y   +=  dual_ccci/
obj-y   +=  fmradio/
obj-y   +=  gps/
obj-y   +=  hwmon/
obj-y   +=  m4u/
obj-y   +=  mhl/
obj-y   +=  mtprof/

ifneq ($(MTK_BATTERY_NO_HAL),yes)
obj-y   += power/
endif

obj-y   +=  sched/
obj-y   +=  usb11/
obj-y   +=  almk/
obj-y   +=  video/
ifneq ($(MTK_PROJECT), mt6589fpga_ca7_ldvt mt6582fpga_ldvt)

endif
obj-y   +=  bthid/
obj-y   +=  ccmni/
 
obj-y   +=  emd_ctl/

# gator-driver is not custom-release and will be removed
obj-y	+= $(call all-subdirs-with-makefile,gator-driver)
obj-y	+= $(call all-subdirs-with-makefile,met)

#obj-y   +=  hdmitx/
obj-y   +=  leds/
obj-y   +=  mmp/
obj-y		+=  ram_console/
obj-y   +=  net/
obj-y   +=  thermal/
obj-y   +=  videocodec/
obj-y   +=  xlog/
obj-y   +=  wdk/
obj-y   +=  usb20/
obj-y   +=  evdo_support/

ifeq ($(CONFIG_MTK_COMBO),m)
	ifneq ($(filter MT6620E3,$(MTK_COMBO_CHIP)),)
		obj-y   +=  combo/
	endif
	ifneq ($(filter MT6628,$(MTK_COMBO_CHIP)),)
		obj-y   +=  combo/
	endif
	ifneq ($(filter MT6572_CONSYS,$(MTK_COMBO_CHIP)),)
		obj-y   +=  conn_soc/
	endif
endif

obj-$(CONFIG_MTK_BTIF)   +=  btif/

obj-y   +=  ion_drv/
ifeq ($(MTK_NFC_SUPPORT),yes)
obj-y	+= nfc/
endif

#ifeq ($(CONFIG_MTK_WIFI),m)
#obj-y   +=  conn_soc/
#endif

obj-$(CONFIG_ZRAM) +=  mlog/

