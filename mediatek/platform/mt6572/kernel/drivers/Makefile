include $(MTK_PATH_BUILD)/common.mk
#Fix-me: modified for early porting
#$(call all-subdir-src-or-makefile)
obj-y   += uart/
ifneq	($(MTK_GPU_SUPPORT),no)
obj-y   += gpu/
endif
obj-y   += btcvsd/
obj-y   += pmic_wrap/
obj-y   += power/
obj-y   += m4u/
obj-y   += smi/
obj-y   += usb20/
obj-y   += usb11/
obj-y   += mmc-host/
obj-y   += sound/
obj-y   += dum-char/
ifneq ($(MTK_EMMC_SUPPORT),yes)
obj-y   += nand/
endif
obj-y   += keypad/
obj-y   += video/
ifneq ($(MTK_PROJECT), mt6572td_evb1_ldvt)
endif
ifneq ($(CONFIG_EARLY_LINUX_PORTING),y)
obj-y   += touchpanel/
obj-y   += spi/
obj-y   += ldvt/
obj-y   += mmc-host/
obj-y   += dum-char/

endif
obj-y   += rtc/

obj-m   += videocodec/
obj-y   += i2c/
ifneq ($(MTK_PROJECT), mt6572fpga_ldvt)
obj-y   += dispsys/
endif
obj-y   += dual_ccci/
obj-y   += accdet/
obj-y   += vibrator/
obj-y   += wdt/
obj-m	+= devinfo/
obj-m	+= masp/
obj-m	+= devapc/
obj-$(CONFIG_THERMAL) += thermal/
obj-$(CONFIG_MTK_ETM) += mt_etm/
obj-y   += cpu_stress/
obj-y   += btif/

ifeq ($(HAVE_CMMB_FEATURE),yes)
obj-m += cmmbdrv/
endif
obj-y   += pmt/
