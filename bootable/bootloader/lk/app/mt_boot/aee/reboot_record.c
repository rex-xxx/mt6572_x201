#include <string.h>
#include "aee.h"
#include "kdump.h"
#include <platform/mrdump.h>

static int reboot_mode;
static struct kdump_params hw_reboot_params;
static struct kdump_crash_record hw_reboot_crash_record;

static bool sram_record_filled;

static void mrdump_query_bootinfo(void)
{
    if (!sram_record_filled) {
        reboot_mode = AEE_REBOOT_MODE_NORMAL;
        struct ram_console_buffer *bufp = (struct ram_console_buffer *) RAM_CONSOLE_ADDR;
	if (bufp->sig == RAM_CONSOLE_SIG) {
            reboot_mode = bufp->reboot_mode;
            bufp->reboot_mode = 0;
            memcpy(&hw_reboot_params, (const struct kdump_params *)bufp->kparams, sizeof(struct kdump_params));
	    if (memcmp(hw_reboot_params.sig, "MRDUMP00", 8) == 0) {
                memcpy(&hw_reboot_crash_record, hw_reboot_params.crash_record, sizeof(struct kdump_crash_record));
                hw_reboot_params.crash_record = &hw_reboot_crash_record;
            }
        }

        sram_record_filled = true;
    }
}

uint8_t aee_mrdump_get_reboot_mode(void)
{
    mrdump_query_bootinfo();
    return reboot_mode;
}

const struct kdump_params *aee_mrdump_get_params(void)
{
    mrdump_query_bootinfo();
    return &hw_reboot_params;
}

void aee_mrdump_wdt_handle()
{
    mrdump_query_bootinfo();
    reboot_mode = AEE_REBOOT_MODE_WDT;

    memset(&hw_reboot_crash_record, 0, sizeof(struct kdump_crash_record));
    strcpy(hw_reboot_crash_record.msg, "HW_REBOOT");

    aee_kdump_detection();
}

