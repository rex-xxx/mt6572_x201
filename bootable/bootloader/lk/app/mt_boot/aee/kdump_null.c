#include <string.h>
#include <malloc.h>
#include <mt_partition.h>
#include <stdint.h>
#include <string.h>
#include <video.h>
#include <platform/mtk_key.h>
#include <platform/mtk_wdt.h>
#include <target/cust_key.h>

#include "aee.h"
#include "kdump.h"
#include "kdump_elf.h"

static int null_write_cb(void *handle, void *buf, int size)
{
    return size;
}

int kdump_null_output(uint32_t total_dump_size)
{
    uint32_t start_address = DRAM_PHY_ADDR;

    const struct kdump_params *kparams = aee_sram_get_kdump_params();

    voprintf_info("%s: Start dumping(address %x, size:%dM)...\n", __func__, start_address, total_dump_size / 0x100000UL);
    mtk_wdt_restart();

    bool ok = true;
    void *bufp = kdump_core_header_init(kparams, start_address, total_dump_size);
    if (bufp != NULL) {
        mtk_wdt_restart();
        struct kzip_file *zf = kzip_open(NULL, null_write_cb);
        if (zf != NULL) {
            struct kzip_memlist memlist[3];
            memlist[0].address = bufp;
            memlist[0].size = KDUMP_CORE_SIZE;
            memlist[1].address = start_address;
            memlist[1].size = total_dump_size;
            memlist[2].address = NULL;
            memlist[2].size = 0;
            kzip_add_file(zf, memlist, "SYS_COREDUMP");
            kzip_close(zf);
            zf = NULL;
            }
        else {
            ok = false;
        }
        free(bufp);
    }
    
    mtk_wdt_restart();
    if (ok) {
        voprintf_info("%s: dump finished, dumped.\n", __func__);
    }

    return 0;
}
