#
# Makefile for the linux kernel.
#
ifeq ($(MTK_INTERNAL), yes)
ifeq ($(MTK_USE_RESERVED_EXT_MEM), yes)
KBUILD_CFLAGS += -DMTK_USE_RESERVED_EXT_MEM
endif
endif

obj-y := core.o irq.o system.o timer.o clock.o mt_dbg.o
obj-y += mt_devs.o mt_gpt.o mt_boot.o board.o mt_pm_init.o mt_fixup.o
obj-y += ca7_timer.o
obj-y += mt_ahb_abt.o
obj-y += mt_gpio_debug.o mt_gpio_ext.o mt_gpio_affix.o
obj-y += mt_gpio_core.o
ifeq ($(CONFIG_MT6572_FPGA),y)
obj-y += mt_gpio_fpga.o
else
obj-y += mt_gpio_base.o
endif
ifeq ($(CONFIG_SMP),y)
obj-y += mt-smp.o hotplug.o mt-headsmp.o mt_hotplug.o mt_hotplug_mechanism.o
endif
obj-y += mt_eint.o mt_freqhopping.o
obj-y += mt_device_apc.o
obj-$(CONFIG_MT65XX_TRACER) += pmu_v7.o mon_interface.o mt_mon.o mt_emi_bm.o
obj-y += emi_bwl.o emi_mpu.o
obj-y += mt_auxadc.o
obj-y += camera_isp.o
obj-y += camera_pipe_mgr.o
#obj-y += camera_eis.o
#obj-y += camera_fdvt.o
ifneq ($(MTK_PROJECT), mt6572td_evb1_ldvt)
endif
obj-y += mt_pm_ldo.o
obj-y += mt_cache_v7.o innercache.o mt_cache_dump.o

# MT6572 GDMA is controlled by CONNSYS
#obj-y += mt_dma.o

ccflags-y += -I$(MTK_PATH_PLATFORM)/drivers/power \
             -I$(MTK_PATH_SOURCE)/drivers/wdk
obj-y += mt_spm.o mt_spm_pcm.o mt_spm_suspend.o mt_spm_dpidle.o mt_spm_mcdi.o mt_spm_mtcmos.o mt_spm_mcdi_ldvt.o mt_spm_wdt.o spm_fs.o
obj-y += mt_sleep.o
obj-y += mt_cpufreq.o
obj-y += mt_gpufreq.o
obj-y += mt_cpu_ss.o
obj-y += mt_dormant.o cpu_dormant.o
obj-y += mt_dcm.o mt_idle.o
obj-y += mt_clkmgr.o
obj-y += mt_pwm.o
obj-y += mt_golden_setting.o
obj-y += mt_ptp.o
obj-y += sbchk_base.o
obj-y += sec_osal.o
obj-$(CONFIG_MTK_CIRQ) += mt_cirq.o
obj-$(CONFIG_MTK_MCI) += mt_mci.o
obj-y += mtk_ccci_helper.o
obj-$(CONFIG_SMP) += fiq_smp_call.o
obj-y += mssv.o
obj-$(CONFIG_MTK_MEMCFG) += mtk_memcfg.o

ifneq ($(CONFIG_MTK_CCCI_DEVICES),y)
ccflags-y  += -D__USING_DUMMY_CCCI_API__
endif

ccflags-y += -I$(MTK_PATH_PLATFORM)/drivers/power \
             -I$(MTK_PATH_PLATFORM)/drivers/gpu/mali/mali/include \
             -I$(MTK_PATH_PLATFORM)/drivers/gpu/mali/mali/platform \
             -I$(MTK_PATH_PLATFORM)/drivers/devinfo \
             -I$(MTK_PATH_SOURCE)/drivers/wdk
             
ifeq ($(CONFIG_THERMAL),y)
	obj-y += mtk_ts_cpu.o
	obj-y += mtk_ts_battery.o
	obj-y += mtk_ts_pmic.o
	obj-y += mtk_ts_pa.o
  ifeq ($(MTK_PCB_BATTERY_SENSOR), yes)
       obj-y += mtk_ts_battery2.o
  endif	
endif

# Hibernation
AFLAGS_swsusp.o	:= -DTEXT_OFFSET=$(TEXT_OFFSET)
obj-$(CONFIG_HIBERNATION)  += cpu_hibernate.o swsusp.o
obj-$(CONFIG_MTK_HIBERNATION) += mtk_hibernate_dpm.o mtk_hibernate_core.o

ifneq ($(MTK_TABLET_HARDWARE), )
ccflags-y += -DMTK_TABLET_PLATFORM=\"$(shell echo $(MTK_TABLET_HARDWARE) | tr a-z A-Z )\"
endif


# SLT Cases
#obj-y += slt_maxpower.o slt_fp0_maxpower_asm.o slt_memcpyL2.o slt_fp3_memcpyL2_asm.o slt_vfp_func.o slt_fp4_vfp_func_asm.o slt_fdct.o slt_fp7_fdct_asm.o slt_imdct.o slt_fp8_imdct_asm.o
#

#KBUILD_AFLAGS	:=$(KBUILD_AFLAGS:-msoft-float=-Wa,-mfpu=neon)
#KT: modify for slt vfp test
KBUILD_AFLAGS	:=$(KBUILD_AFLAGS:-msoft-float=-Wa,-mfpu=neon-vfpv4) 
