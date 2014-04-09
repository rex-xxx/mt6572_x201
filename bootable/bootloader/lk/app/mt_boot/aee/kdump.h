#if !defined(__KDUMP_H__)
#define __KDUMP_H__

#include <stdint.h>
#include <mt_partition.h>
#include <platform/mrdump.h>
#include "kdump_elf.h"

#define KDUMP_ON_PANIC_NAME "kdump_on_panic"

#define KDUMP_CONFIG_NONE              0
#define KDUMP_CONFIG_USB               1
#define KDUMP_CONFIG_MSDC_FORMAT_WRITE 2
#define KDUMP_CONFIG_MSDC_WRITE        6

#define KDUMP_EXTERNAL_SDCARD
#define KDUMP_INTENRAL_EMMC

#define KZIP_ENTRY_MAX 8
#define LOCALHEADERMAGIC 0x04034b50UL
#define CENTRALHEADERMAGIC 0x02014b50UL
#define ENDOFCENTRALDIRMAGIC 0x06054b50UL

struct kzip_entry {
    char *filename;
    int level;
    uint64_t localheader_offset;
    uint32_t comp_size;
    uint32_t uncomp_size;
    uint32_t crc32;
};

struct kzip_file {
    uint32_t reported_size;
    uint32_t wdk_kick_size;
    uint32_t current_size;
  
    uint32_t entries_num;
    struct kzip_entry zentries[KZIP_ENTRY_MAX];
    void *handle;

    int (*write_cb)(void *handle, void *buf, int size);
};

struct kzip_memlist {
    void *address;
    uint32_t size;
};

struct kdump_alog {
    unsigned char *buf;
    int size;
    size_t *woff;
    size_t *head;
};

struct kdump_crash_record {
    char msg[128];
    char backtrace[512];
    
    uint32_t fault_cpu;
    elf_gregset_t cpu_regs[RC_CPU_COUNT];
};

struct kdump_params {
    char sig[8];

    uint32_t crc;

    uint32_t nr_cpus;

    void *page_offset;
    void *high_memory;

    void *vmalloc_start;
    void *vmalloc_end;

    void *modules_start;
    void *modules_end;

    void *phys_offset;
    void *master_page_table;

    struct kdump_crash_record *crash_record;

    char *log_buf;
    int log_buf_len;
    unsigned int *log_end;

    struct kdump_alog android_main_log;
    struct kdump_alog android_system_log;
    struct kdump_alog android_radio_log;
    
};

struct kzip_file *kzip_open(void *handle, int (*write_cb)(void *handle, void *p, int size));
bool kzip_add_file(struct kzip_file *zf, const struct kzip_memlist *memlist, const char *zfilename);
bool kzip_close(struct kzip_file *zf);

int kdump_sdcard_output(uint32_t total_dump_size);
int kdump_null_output(uint32_t total_dump_size);

part_t *card_dump_init(int dev, const char *name);
int card_dump_read(part_t *part, unsigned char* buf, uint64_t offset, uint32_t len);
bool card_dump_write(part_t *part, unsigned char* buf, uint64_t offset, uint32_t len);

void *kdump_core_header_init(const struct kdump_params *kparams, uint32_t kmem_address, uint32_t kmem_size);

#endif
