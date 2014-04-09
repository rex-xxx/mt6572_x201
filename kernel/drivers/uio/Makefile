ifeq ($(MTK_USE_RESERVED_EXT_MEM), yes)
KBUILD_CFLAGS += -DONLY_MSPACES
KBUILD_CFLAGS += -DUSE_LOCKS
KBUILD_CFLAGS += -DNO_MALLOC_STATS
KBUILD_CFLAGS += -DLACKS_SCHED_H
KBUILD_CFLAGS += -DLACKS_TIME_H
KBUILD_CFLAGS += -DLACKS_SYS_TYPES_H
KBUILD_CFLAGS += -DNO_MALLINFO

obj-y += uio.o
obj-$(CONFIG_UIO_CIF)	+= uio_cif.o
obj-$(CONFIG_UIO_PDRV)	+= uio_pdrv.o
obj-$(CONFIG_UIO_PDRV_GENIRQ)	+= uio_pdrv_genirq.o
obj-$(CONFIG_UIO_AEC)	+= uio_aec.o
obj-$(CONFIG_UIO_SERCOS3)	+= uio_sercos3.o
obj-$(CONFIG_UIO_PCI_GENERIC)	+= uio_pci_generic.o
obj-$(CONFIG_UIO_NETX)	+= uio_netx.o
obj-$(CONFIG_UIO_PRUSS)         += uio_pruss.o
obj-y += uio_dlmalloc.o

else

obj-$(CONFIG_UIO)	+= uio.o
obj-$(CONFIG_UIO_CIF)	+= uio_cif.o
obj-$(CONFIG_UIO_PDRV)	+= uio_pdrv.o
obj-$(CONFIG_UIO_PDRV_GENIRQ)	+= uio_pdrv_genirq.o
obj-$(CONFIG_UIO_AEC)	+= uio_aec.o
obj-$(CONFIG_UIO_SERCOS3)	+= uio_sercos3.o
obj-$(CONFIG_UIO_PCI_GENERIC)	+= uio_pci_generic.o
obj-$(CONFIG_UIO_NETX)	+= uio_netx.o
obj-$(CONFIG_UIO_PRUSS)         += uio_pruss.o
endif
