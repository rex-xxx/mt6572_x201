#include "aee.h"
#include "kdump.h"

part_t *card_dump_init(int dev, const char *part_name)
{
    part_t *part = mt_part_get_partition(part_name);
    if (part == NULL) {
	voprintf_error("No FAT partition found");
	return 0;
    }
    voprintf_info("%s offset: %lu, size: %lu Mb\n", part->name, part->startblk, (part->blknum * BLK_SIZE) / 0x100000UL);
    
    return part;
}

int card_dump_read(part_t *part, unsigned char* buf, uint64_t offset, uint32_t len)
{
    if ((offset / BLK_SIZE + len / BLK_SIZE) >= part->blknum) {
      voprintf_error("Read %s partition overflow, size%lu, block %lu\n", part->name, offset / BLK_SIZE, part->blknum);
        return 0;
    }
    if (len % BLK_SIZE != 0) {
        voprintf_error("Read partition size/offset not align, start %ld offset %lld elen %lu\n", part->startblk, offset, len);
        return 0;
    }
    return emmc_read((part->startblk * BLK_SIZE) + offset, buf, len) == len;
}

bool card_dump_write(part_t *part, unsigned char* buf, uint64_t offset, uint32_t len)
{
    if ((offset / BLK_SIZE + len / BLK_SIZE) >= part->blknum) {
        voprintf_error("Write to %s partition overflow, size %lu,  block %lu\n", part-> name, offset / BLK_SIZE, part->blknum);
        return 0;
    }
    if (len % BLK_SIZE != 0) {
        voprintf_error("Write to partition size/offset not align, offset %lld elen %lu\n", offset, len);
        return 0;
    }
    bool retval = emmc_write((part->startblk * BLK_SIZE) + offset, buf, len) == len;
    if (!retval) {
        voprintf_error("EMMC write failed %d\n", len);
    }
    return retval;
}
