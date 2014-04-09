#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/device.h>
#include <linux/platform_device.h>
#include <linux/kallsyms.h>
#include "mach/sync_write.h"
#include "mach/mt_reg_dump.h"


int mt_reg_dump(char *buf)
{
	int i;
	char *ptr = buf;
	unsigned int pc_value;
	unsigned int fp_value;
	unsigned int sp_value;
	int size = 0;
	int offset = 0;
	char str[KSYM_SYMBOL_LEN];

	/* Get PC, FP, SP and save to buf */
	for (i = 0; i < CORE_CNT; i++) {
	    pc_value = readl(CORE0_PC + (i << 4));
	    fp_value = readl(CORE0_FP + (i << 4));
	    sp_value = readl(CORE0_SP + (i << 4));
	    if(pc_value != 0x0)  pc_value -= 8;
	    kallsyms_lookup((unsigned long)pc_value, &size, &offset, NULL, str);	  
	    ptr += sprintf(ptr, "CORE_%d PC = 0x%x(%s + 0x%x), FP = 0x%x, SP = 0x%x\n", i, pc_value, str, offset, fp_value, sp_value);
	}

	return 0;
}

static struct mt_reg_dump_driver {
	struct device_driver driver;
	const struct platform_device_id *id_table;
};

static struct mt_reg_dump_driver mt_reg_dump_drv = {
	.driver = {
		   .name = "mt_reg_dump",
		   .bus = &platform_bus_type,
		   .owner = THIS_MODULE,
		   },
	.id_table = NULL,
};

static ssize_t last_pc_dump_show(struct device_driver *driver, char *buf)
{
	int ret = mt_reg_dump(buf);
	if (ret == -1)
		printk(KERN_CRIT "Dump error in %s, %d\n", __func__, __LINE__);
	
	return strlen(buf);;
}

static ssize_t last_pc_dump_store(struct device_driver * driver, const char *buf, size_t count)
{
	return count;
}

DRIVER_ATTR(last_pc_dump, 0664, last_pc_dump_show, last_pc_dump_store);

/*
 * mt_reg_dump_init: initialize driver.
 * Always return 0.
 */
static int __init mt_reg_dump_init(void)
{
	int ret;
	
	ret = driver_register(&mt_reg_dump_drv.driver);
	if (ret) {
		pr_err("Fail to register mt_reg_dump_drv");
	}

	ret = driver_create_file(&mt_reg_dump_drv.driver, &driver_attr_last_pc_dump);
	if (ret) {
		pr_err("Fail to create mt_reg_dump_drv sysfs files");
	}

	return 0;
}

arch_initcall(mt_reg_dump_init);
