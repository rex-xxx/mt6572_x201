obj-$(CONFIG_THERMAL) += mtk_thermal_monitor.o
obj-$(CONFIG_THERMAL) += mtk_cooler_shutdown.o
obj-$(CONFIG_THERMAL) += mtk_cooler_backlight.o
obj-$(CONFIG_THERMAL) += mtk_cooler_kshutdown.o
obj-$(CONFIG_THERMAL) += mtk_ts_pa_thput.o
ifneq ($(CONFIG_MTK_COMBO_WIFI),)
    ifeq ($(MTK_COMBO_CHIP),MT6572_CONSYS)
        obj-$(CONFIG_THERMAL) += mtk_ts_wmt.o
    endif
endif
#obj-$(CONFIG_THERMAL) += mtk_ts_skin.o
#obj-$(CONFIG_THERMAL) += mtk_cooler_spkrvol.o
#obj-$(CONFIG_THERMAL) += mtk_cooler_dropcall.o
