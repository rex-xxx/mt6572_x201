#include <malloc.h>
#include <mt_partition.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <video.h>
#include <platform/mtk_key.h>
#include <target/cust_key.h>

#include "aee.h"
#include "kdump.h"

static void voprintf(char type, const char *msg, va_list ap)
{
    char msgbuf[128];
    msgbuf[0] = type;
    msgbuf[1] = ':';
    vsnprintf(msgbuf + 2, sizeof(msgbuf) - 2, msg, ap);
    // video_printf("%s", msgbuf);
    printf("%s", msgbuf);
}

void voprintf_verbose(const char *msg, ...)
{
    va_list ap;
    va_start(ap, msg);
    voprintf('V', msg, ap);
    va_end(ap);
}

void voprintf_debug(const char *msg, ...)
{
    va_list ap;
    va_start(ap, msg);
    voprintf('D', msg, ap);
    va_end(ap);
}

void voprintf_info(const char *msg, ...)
{
    va_list ap;
    va_start(ap, msg);
    voprintf('I', msg, ap);
    va_end(ap);
}

void voprintf_warning(const char *msg, ...)
{
    va_list ap;
    va_start(ap, msg);
    voprintf('W', msg, ap);
    va_end(ap);
}

void voprintf_error(const char *msg, ...)
{
    va_list ap;
    va_start(ap, msg);
    voprintf('E', msg, ap);
    va_end(ap);
}


u64 value_round_up(u64 value, int align)
{
    return ((value + align - 1) / align) * align;
}

void *kernel_pa(struct kdump_params *kparams, void *vaddr)
{
    return (vaddr - kparams->page_offset) + kparams->phys_offset;
}

uint32_t g_aee_mode = AEE_MODE_MTK_ENG;

const const char *mode2string(uint8_t mode)
{
  switch (mode) {
  case AEE_REBOOT_MODE_NORMAL:
    return "Normal reboot";

  case AEE_REBOOT_MODE_KERNEL_PANIC:
    return "Kernel panic";

  case AEE_REBOOT_MODE_NESTED_EXCEPTION:
    return "Nested CPU exception";

  case AEE_REBOOT_MODE_WDT:
    return "Hardware watch dog triggered";

  case AEE_REBOOT_MODE_EXCEPTION_KDUMP:
    return "Kernel exception dump";

  default:
    return "Unknown reboot mode";
  }
}

#define KERNEL_LOG_BUF_SIZE 32768
static bool kdump_write_ipanic_kernel_log(const part_t *expdb, const struct kdump_params *kparams, u64 offset_pos)
{
    char *log_buf = kernel_pa(kparams, kparams->log_buf);
    unsigned int log_begin = 0, log_end = *kparams->log_end;
    if (log_end > kparams->log_buf_len) {
        log_begin = log_end - kparams->log_buf_len;
    }

    char *kernel_log = malloc(KERNEL_LOG_BUF_SIZE);
    memset(kernel_log, 0, KERNEL_LOG_BUF_SIZE);

    int curr = 0;
    while (curr < kparams->log_buf_len) {
        int buf_index = 0;
        while ((buf_index < KERNEL_LOG_BUF_SIZE) && (curr < kparams->log_buf_len)) {
            kernel_log[buf_index] = log_buf[(log_begin + curr) % kparams->log_buf_len];
            curr++;
            buf_index++;
        }
        buf_index = value_round_up(buf_index, BLK_SIZE);
        if (!card_dump_write(expdb, kernel_log, offset_pos, buf_index)) {
            free(kernel_log);
            return false;
        }
        offset_pos += buf_index;
    }

    free(kernel_log);
    return true;
}

static bool kdump_write_log(const part_t *expdb, const struct kdump_params *kparams, u64 offset_pos, const struct kdump_alog *alog)
{
    char *log_buf = kernel_pa(kparams, alog->buf);
    size_t log_head = *((size_t *) kernel_pa(kparams, alog->head)), 
	log_woff = *((size_t *) kernel_pa(kparams, alog->woff));

    int blknum = value_round_up(alog->size, BLK_SIZE);

    voprintf_debug("%s buf %x size %d\n    head %d woff %d\n", __func__, log_buf, blknum,
                   log_head, log_woff);
    char *new_buf = malloc(blknum);
    memset(new_buf, 0, sizeof(blknum));

    if (log_woff > log_head) {
	memcpy(new_buf, log_buf + log_head, log_head - log_woff);
    }
    else {
	memcpy(new_buf, log_buf + log_head, alog->size - log_head);
	memcpy(new_buf + alog->size - log_head, log_buf, log_woff);
    }

    bool retval = card_dump_write(expdb, new_buf, offset_pos, blknum);
    free(new_buf);
    return retval;
}

static bool kdump_write_ipanic_oops_header(const part_t *expdb, const struct kdump_params *kparams, u64 offset_pos)
{
    int header_size = value_round_up(sizeof(struct ipanic_oops_header) + BLK_SIZE, BLK_SIZE);
    struct ipanic_oops_header *oops_header = malloc(header_size);
    memset(oops_header, 0, oops_header);
    strcpy(oops_header->process_path, "<unknown>");
    strcpy(oops_header->backtrace, kparams->crash_record->backtrace);
    bool retval = true;
    if (!card_dump_write(expdb, oops_header, offset_pos, header_size)) {
	retval = false;
    }
    free(oops_header);
    return retval;
}

static int kdump_write_ipanic_record(void)
{
    part_t *expdb = card_dump_init(0, PART_APANIC);
    if (expdb == NULL) {
	voprintf_error("No expdb partition found");
	return 0;
    }

    const struct kdump_params *kparams = aee_sram_get_kdump_params();
    voprintf_info("CPU #%d page offset %x phys offset %x\n", kparams->nr_cpus, kparams->page_offset, kparams->phys_offset);
    voprintf_info("kernel %x len %d\n", kparams->log_buf, kparams->log_buf_len);
    voprintf_info("Android main %x len %d\n", kparams->android_main_log.buf, kparams->android_main_log.size);
    voprintf_info("Android system %x len %d\n", kparams->android_system_log.buf, kparams->android_system_log.size);
    voprintf_info("Android radio %x len %d\n", kparams->android_radio_log.buf, kparams->android_radio_log.size);
    
    char *emmc_blk = malloc(BLK_SIZE);

    memset(emmc_blk, 0, BLK_SIZE);
    /* Clear old record  */
    if (!card_dump_write(expdb, emmc_blk, 0, BLK_SIZE)) {
        return 0;
    }

    struct ipanic_header *panic = (struct ipanic_header *)emmc_blk;
    memset(panic, 0, sizeof(struct ipanic_header));
    panic->magic = AEE_IPANIC_MAGIC;
    panic->version = AEE_IPANIC_PHDR_VERSION;

    int current_offset = BLK_SIZE;
    panic->oops_header_offset = current_offset;
    panic->oops_header_length = sizeof(struct ipanic_oops_header);

    current_offset = value_round_up(panic->oops_header_offset + panic->oops_header_length, BLK_SIZE);
    panic->console_offset = current_offset;
    panic->console_length = kparams->log_buf_len;

    current_offset = value_round_up(panic->console_offset + panic->console_length, BLK_SIZE);
    panic->android_main_offset = current_offset;
    panic->android_main_length = kparams->android_main_log.size;

    current_offset = value_round_up(panic->android_main_offset + panic->android_main_length, BLK_SIZE);
    panic->android_system_offset = current_offset;
    panic->android_system_length = kparams->android_system_log.size;

    current_offset = value_round_up(panic->android_system_offset + panic->android_system_length, BLK_SIZE);
    panic->android_radio_offset = current_offset;
    panic->android_radio_length = kparams->android_radio_log.size;

    if (kdump_write_ipanic_oops_header(expdb, kparams, panic->oops_header_offset)) {
        voprintf_info("Write oops header ok\n");
        if (kdump_write_ipanic_kernel_log(expdb, kparams, panic->console_offset)) {
            voprintf_info("Write kernel log ok\n");
            if (kdump_write_log(expdb, kparams, panic->android_main_offset, &kparams->android_main_log)) {
                voprintf_info("Write android main log ok\n");
                if (kdump_write_log(expdb, kparams, panic->android_system_offset, &kparams->android_system_log)) {
                    voprintf_info("Write android system log ok\n");
                    if (kdump_write_log(expdb, kparams, panic->android_radio_offset, &kparams->android_radio_log)) {
                        voprintf_info("Write android radio log ok\n");
                        if (card_dump_write(expdb, emmc_blk, 0, BLK_SIZE)) {
                            voprintf_info("Write ipanic header ok\n");
                        }
                    }
                }
            }
        }
    }

    free(emmc_blk);

    return 1;
}

static void kdump_ui(uint8_t mode)
{
    video_clean_screen();
    voprintf_info("Kdump triggerd by '%s'\n", mode2string(mode));
  
#if 0
    kdump_write_ipanic_record();
#endif

    uint32_t total_dump_size = memory_size();
    // total_dump_size = 64 * 1024 * 1024;

    // kdump_null_output(total_dump_size);
    kdump_sdcard_output(total_dump_size);

    voprintf_info("Press[VOL DOWN] to continue\n");
    while(1) {
        // VOL_DOWN
        if (mtk_detect_key(MT65XX_MENU_OK_KEY)) {
            break;
        }
    }
    voprintf_info("Prepare to boot...", mode2string(mode));
    mtk_arch_reset(1);
}

int aee_kdump_detection(void)
{
#ifdef USER_BUILD
    return 0;
#else
    uint8_t reboot_mode = aee_mrdump_get_reboot_mode();

#if 0
    voprintf_info("sram record with mode %d\n", reboot_mode);
#endif
    switch (reboot_mode) {
    case AEE_REBOOT_MODE_NORMAL:
      return 0;
            
    case AEE_REBOOT_MODE_KERNEL_PANIC:
    case AEE_REBOOT_MODE_NESTED_EXCEPTION:
      return 0;

    case AEE_REBOOT_MODE_WDT:
      kdump_ui(reboot_mode);
      break;

    case AEE_REBOOT_MODE_EXCEPTION_KDUMP:
      kdump_ui(reboot_mode);
      break;
    }
    return 0;
#endif
}

