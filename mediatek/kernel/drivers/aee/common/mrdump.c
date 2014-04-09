#include <linux/delay.h>
#include <linux/module.h>
#include <linux/mm.h>
#include <linux/aee.h>
#include <linux/elf.h>
#include <linux/elfcore.h>
#include <linux/kallsyms.h>
#include <linux/miscdevice.h>
#include <linux/mtk_ram_console.h>
#include <linux/reboot.h>
#include <linux/stacktrace.h>
#include <asm/pgtable.h>
#include <mach/fiq_smp_call.h>

#include "../../../../kernel/drivers/staging/android/logger.h"

extern void __inner_flush_dcache_all(void);
extern void __inner_flush_dcache_L1(void);
extern void __inner_flush_dcache_L2(void);

struct kdump_alog {
	unsigned char *buf;
	int size;
	size_t *woff;
	size_t *head;
};

struct mrdump_crash_record {
	char msg[128];
	char backtrace[512];
	
	uint32_t fault_cpu;
	elf_gregset_t cpu_regs[NR_CPUS];
};

struct mrdump_machdesc {
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

	struct krdump_crash_record *crash_record;

	char *log_buf;
	int log_buf_len;
	unsigned int *log_end;

	struct kdump_alog android_main_log;
	struct kdump_alog android_system_log;
	struct kdump_alog android_radio_log;
};

static struct mrdump_crash_record mrdump_crash_record;
static struct mrdump_machdesc mrdump_machdesc;

static void save_current_task(void)
{
	struct stack_trace trace;
	unsigned long stack_entries[16];
	int i, plen;
	struct task_struct *tsk;
	
	
	tsk = current_thread_info()->task;
    
	/* Grab kernel task stack trace */
	trace.nr_entries	= 0;
	trace.max_entries	= sizeof(stack_entries)/sizeof(stack_entries[0]);
	trace.entries		= stack_entries;
	trace.skip		= 1;
	save_stack_trace_tsk(tsk, &trace);

	for (i = 0; i < trace.nr_entries; i++) {
		int off = strlen(mrdump_crash_record.backtrace);
		int plen = sizeof(mrdump_crash_record.backtrace) - off;
		if (plen > 16) {
			snprintf(mrdump_crash_record.backtrace + off, plen, "[<%p>] %pS\n",
				 (void *)stack_entries[i], (void *)stack_entries[i]);
		}
	}
}

static void aee_kdump_cpu_stop(void *arg, void *regs, void *svc_sp)
{
	int cpu = 0;
	register int sp asm("sp");
	struct pt_regs *ptregs = (struct pt_regs *)regs;
	
	asm volatile("mov %0, %1\n\t"
		     "mov fp, %2\n\t"
		     : "=r" (sp)
		     : "r" (svc_sp), "r" (ptregs->ARM_fp)
		);
	
	asm volatile("MRC p15,0,%0,c0,c0,5\n"
		     "AND %0,%0,#0xf\n"
		     : "+r" (cpu)
		     :
		     : "cc");
	
	elf_core_copy_regs(&mrdump_crash_record.cpu_regs[cpu], ptregs);

	set_cpu_online(cpu, false);
	local_fiq_disable();
	local_irq_disable();
	
	__inner_flush_dcache_L1();
	while (1)
		cpu_relax();
}

void aee_kdump_reboot(AEE_REBOOT_MODE reboot_mode, const char *msg, ...)
{
	struct pt_regs regs;
	struct cpumask mask;
	va_list ap;
	int timeout, cpu;

	asm volatile("stmia %1, {r0 - r15}\n\t"
		     "mrs %0, cpsr\n"
		     : "=r"(regs.uregs[16])
		     : "r" (&regs)
		     : "memory");

	local_irq_disable();

	cpu = 0;
	asm volatile("MRC p15,0,%0,c0,c0,5\n"
		     "AND %0,%0,#0xf\n"
		     : "+r" (cpu)
		     :
		     : "cc");
	elf_core_copy_regs(&mrdump_crash_record.cpu_regs[cpu], &regs);

	cpumask_copy(&mask, cpu_online_mask);

	cpumask_clear_cpu(cpu, &mask);

	fiq_smp_call_function(aee_kdump_cpu_stop, NULL, 0);

	mrdump_crash_record.fault_cpu = cpu;

	// Wait up to one second for other CPUs to stop 
	timeout = USEC_PER_SEC;
	while (num_online_cpus() > 1 && timeout--)
		udelay(1);

	va_start(ap, msg);
	vsnprintf(mrdump_crash_record.msg, sizeof(mrdump_crash_record.msg), msg, ap);
	va_end(ap);

	save_current_task();

	/* FIXME: Check reboot_mode is valid */
	aee_rr_rec_reboot_mode(reboot_mode);
	__inner_flush_dcache_all();
	emergency_restart();
}

static int __init aee_kdump_hw_init(void)
{
	char **log_buf_sym;
	int *log_buf_len_sym;
	unsigned *log_end_sym;
#if 0
	struct logger_log *logger_sym;
#endif

	memset(&mrdump_machdesc, 0, sizeof(struct mrdump_machdesc));

	log_buf_sym = (char **)kallsyms_lookup_name("log_buf");
	log_buf_len_sym = (int *)kallsyms_lookup_name("log_buf_len");
	log_end_sym = (unsigned *)kallsyms_lookup_name("log_end");

	memcpy(&mrdump_machdesc.sig, "MRDUMP00", 8);
	mrdump_machdesc.nr_cpus = NR_CPUS;
	mrdump_machdesc.page_offset = PAGE_OFFSET;
	mrdump_machdesc.high_memory = high_memory;

        mrdump_machdesc.vmalloc_start = VMALLOC_START;
        mrdump_machdesc.vmalloc_end = VMALLOC_END;

        mrdump_machdesc.modules_start = MODULES_VADDR;
        mrdump_machdesc.modules_end = MODULES_END;

	mrdump_machdesc.phys_offset = PHYS_OFFSET;
	mrdump_machdesc.master_page_table = swapper_pg_dir;
	mrdump_machdesc.crash_record = __pa(&mrdump_crash_record);
	
	mrdump_machdesc.log_buf = (log_buf_sym != NULL ? *log_buf_sym : 0);
	mrdump_machdesc.log_buf_len = (log_buf_len_sym != NULL ? *log_buf_len_sym : 0);
	mrdump_machdesc.log_end = (log_end_sym != NULL ? __pa(log_end_sym) : 0);

#if 0
	logger_sym = (struct logger_log *) kallsyms_lookup_name("log_main");
	mrdump_machdesc.android_main_log.buf = logger_sym->buffer;
	mrdump_machdesc.android_main_log.size = logger_sym->size;
	mrdump_machdesc.android_main_log.woff = &logger_sym->w_off;
	mrdump_machdesc.android_main_log.head = &logger_sym->head;

	logger_sym = (struct logger_log *) kallsyms_lookup_name("log_system");
	mrdump_machdesc.android_system_log.buf = logger_sym->buffer;
	mrdump_machdesc.android_system_log.size = logger_sym->size;
	mrdump_machdesc.android_system_log.woff = &logger_sym->w_off;
	mrdump_machdesc.android_system_log.head = &logger_sym->head;

	logger_sym = (struct logger_log *) kallsyms_lookup_name("log_radio");
	mrdump_machdesc.android_radio_log.buf = logger_sym->buffer;
	mrdump_machdesc.android_radio_log.size = logger_sym->size;
	mrdump_machdesc.android_radio_log.woff = &logger_sym->w_off;
	mrdump_machdesc.android_radio_log.head = &logger_sym->head;
#endif

	aee_rr_rec_kdump_params(__pa(&mrdump_machdesc));
	
	return 0;
}

late_initcall(aee_kdump_hw_init);

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("MediaTek MRDUMP module");
MODULE_AUTHOR("MediaTek Inc.");
