#if !defined (__MRDUMP_H__)
#define __MRDUMP_H__

#include <sys/types.h>

#define RAM_CONSOLE_ADDR 0x01000000

#define RAM_CONSOLE_SIG (0x43474244) /* DBGC */

#define RC_CPU_COUNT 2
#define TASK_COMM_LEN 16

struct ram_console_buffer {
	uint32_t    sig;
	uint32_t    start;
	uint32_t    size;

	uint8_t     hw_status;
	uint8_t	    fiq_step;
	uint8_t     reboot_mode;
	uint8_t     __pad2;
	uint8_t     __pad3;

	uint32_t    bin_log_count;

	uint32_t    last_irq_enter[RC_CPU_COUNT];
	uint64_t    jiffies_last_irq_enter[RC_CPU_COUNT];

	uint32_t    last_irq_exit[RC_CPU_COUNT];
	uint64_t    jiffies_last_irq_exit[RC_CPU_COUNT];

	uint64_t    jiffies_last_sched[RC_CPU_COUNT];
	char        last_sched_comm[RC_CPU_COUNT][TASK_COMM_LEN];

	uint8_t     hotplug_data1[RC_CPU_COUNT];
	uint8_t     hotplug_data2[RC_CPU_COUNT];

	void        *kparams;

 	uint8_t     data[0];
};


int aee_kdump_detection(void);

#endif
