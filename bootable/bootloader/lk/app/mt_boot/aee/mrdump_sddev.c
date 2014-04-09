#include <stddef.h>
#include <stdint.h>
#include <mt_partition.h>
#include <mmc_common_inter.h>

#include "aee.h"
#include "kdump.h"
#include "kdump_sdhc.h"

#define DEVICE_SECTOR_BYTES 512

static bool mrdump_dev_emmc_read(struct mrdump_dev *dev, uint32_t sector_addr, uint8_t *pdBuf, int32_t blockLen)
{
    part_t *fatpart = dev->handle;
    return card_dump_read(fatpart, pdBuf, sector_addr * DEVICE_SECTOR_BYTES, blockLen * DEVICE_SECTOR_BYTES);
}

static bool mrdump_dev_emmc_write(struct mrdump_dev *dev, uint32_t sector_addr, uint8_t *pdBuf, int32_t blockLen)
{
    part_t *fatpart = dev->handle;
    return card_dump_write(fatpart, pdBuf, sector_addr * DEVICE_SECTOR_BYTES, blockLen * DEVICE_SECTOR_BYTES);
}

struct mrdump_dev *mrdump_dev_emmc(void)
{
#if defined(PART_FAT)
    struct mrdump_dev *dev = malloc(sizeof(struct mrdump_dev));

    part_t *fatpart = card_dump_init(0, PART_FAT);
    if (fatpart == NULL) {
        return NULL;
    }
    dev->handle = fatpart;
    dev->read = mrdump_dev_emmc_read;
    dev->write = mrdump_dev_emmc_write;
    return dev;
#else
    voprintf_info("%s: No FAT partition to write, NAND system?", __func__);
    return NULL;
#endif
}

static bool mrdump_dev_sdcard_read(struct mrdump_dev *dev, uint32_t sector_addr, uint8_t *pdBuf, int32_t blockLen)
{
        mmc_wrap_bread(1, sector_addr, blockLen, pdBuf) == 1;
}

static bool mrdump_dev_sdcard_write(struct mrdump_dev *dev, uint32_t sector_addr, uint8_t *pdBuf, int32_t blockLen)
{
        mmc_wrap_bwrite(1, sector_addr, blockLen, pdBuf) == 1;
}

struct mrdump_dev *mrdump_dev_sdcard(void)
{
    struct mrdump_dev *dev = malloc(sizeof(struct mrdump_dev));
    dev->handle = NULL;
    dev->read = mrdump_dev_sdcard_read;
    dev->write = mrdump_dev_sdcard_write;

    mmc_legacy_init(2);
    return dev;
}

