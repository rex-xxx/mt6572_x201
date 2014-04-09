#include <linux/kernel.h>
#include <linux/interrupt.h>
#include <linux/wakelock.h>
#include <linux/module.h>
#include <asm/delay.h>
#include <linux/device.h>
#include <linux/platform_device.h>
#include <linux/gfp.h>
#include <asm/io.h>
#include <asm/memory.h>
#include <asm/outercache.h>
#include <linux/spinlock.h>
#include <linux/slab.h>

#include <linux/leds-mt65xx.h>
#include <linux/sched.h>
#include <linux/vmalloc.h>
#include "slt.h"
#include <linux/io.h>
#include <asm/pgtable.h>
#include <linux/delay.h>

#include <linux/kthread.h>
#include <linux/err.h>
#include <asm/cacheflush.h>
#include <linux/irq.h>

//#include <mach/mt_wdt.h>
//#include <wd_kicker.h>

//extern int mtk_wdt_enable(enum wk_wdt_en en);


typedef enum {
	SCU_STATE_IDEL = 0,
	SCU_STATE_START,
    SCU_STATE_EXECUTE,
    SCU_STATE_EXEEND,
} scu_state_t;

extern int fp6_scu_start(unsigned long);
static int g_iCPU_PassFail;
static int g_iScuLoopCount;
static int g_iSCU_PassFail[NR_CPUS];
static volatile int g_iSCU_State[NR_CPUS];
volatile unsigned int scu_error_flag;
volatile unsigned int scu_test_flag;
static spinlock_t scu_store_lock;
static spinlock_t scu_wait_sync_lock;
static spinlock_t scu_thread_irq_lock[NR_CPUS];

static struct device_driver slt_cpu_scu_drv =
{
    .name = "slt_cpu_scu",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

static struct device_driver slt_scu_loop_count_drv =
{
    .name = "slt_scu_loop_count",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

static ssize_t slt_cpu_scu_show(struct device_driver *driver, char *buf)
{
    return snprintf(buf, PAGE_SIZE, "CPU SCU - %s(loop_count = %d)\n", g_iCPU_PassFail != g_iScuLoopCount ? "FAIL" : "PASS", g_iCPU_PassFail);
}


static ssize_t slt_scu_loop_count_show(struct device_driver *driver, char *buf)
{
    return snprintf(buf, PAGE_SIZE, "SCU Test Loop Count = %d\n", g_iScuLoopCount);
}

static int slt_scu_print_state(void)
{
    printk("state cpu0: %d, cpu1:%d\n", g_iSCU_State[0], g_iSCU_State[1]);

}

static int slt_scu_write_state(int cpu, unsigned long state)
{
    g_iSCU_State[cpu] = state;
    return 0;
}


static int is_slt_scu_state_sync(unsigned long state)
{
    int ret = 1;
    int cpu;

    __cpuc_flush_dcache_area(g_iSCU_State, 2*sizeof(int));

    for(cpu = 0; cpu < NR_CPUS; cpu++)
    {
        if(g_iSCU_State[cpu] != state)
        {
            ret = 0;
            break;
        }
    }

    return ret;
}

/*
 * function    : wait_slt_scu_state_sync
 * input       : state: waitting state
 *               wait = 0 will wait forever
 * Description : This function only can be called in
 *               slt_scu_test_func for cpu sync
 */

static int wait_slt_scu_state_sync(unsigned long state, int wait)
{

    int ret = 0, i;
    unsigned long retry = 0;
    static volatile int get_exit = 0;
    static volatile int cpu_num = 0;
    unsigned long all_cpu_mask = 0;

    int cpu = raw_smp_processor_id();

    //printk("wait_slt_scu_state_sync, cpu%d wait state=%d\n", cpu, state);

    if(cpu_num & (0x1 << cpu))
    {
        //printk(KERN_ERR, "cpu%d already waitting\n", cpu);
        return 0;
    }

    while(cpu_num && get_exit)
    {
        //printk(KERN_INFO, "wait other cpu to finish waiting loop\n");
        mdelay(10);
    }

    spin_lock(&scu_wait_sync_lock);
    cpu_num  |= 0x1 << cpu;
    get_exit = 0;
    __cpuc_flush_dcache_area(&get_exit, sizeof(int));
    __cpuc_flush_dcache_area(&cpu_num, sizeof(int));
    spin_unlock(&scu_wait_sync_lock);

    for(i = 0; i < NR_CPUS; i++)
    {
        all_cpu_mask |= (0x1 << i);
    }

    /* wait all cpu in sync loop */
    while(cpu_num != all_cpu_mask)
    {
        retry++;

        if(retry > 0x10000)
        {
            //printk(KERN_INFO, "scu wait sync state (%d) timeout\n", state);
            goto wait_sync_out;
        }

        if(get_exit)
            break;

        //printk(KERN_INFO, "\n\nretry=0x%08x wait state = %d\n", retry, state);
        //slt_scu_print_state();
        mdelay(1);
    }

    spin_lock(&scu_wait_sync_lock);
    get_exit |= 0x1 << cpu;
    __cpuc_flush_dcache_area(&get_exit, sizeof(int));
    spin_unlock(&scu_wait_sync_lock);


    ret = is_slt_scu_state_sync(state);

    /* make sure all cpu exit wait sync loop
     * check cpu_num is for the case retry timeout
     */
    while(1)
    {
        //printk(KERN_INFO, "wait exit retry\n");
        if(!get_exit ||
           get_exit == all_cpu_mask ||
           cpu_num != all_cpu_mask)
        {
            break;
        }
        mdelay(1);
    }

wait_sync_out:
    spin_lock(&scu_wait_sync_lock);
    cpu_num &= ~(0x01 << cpu);
    __cpuc_flush_dcache_area(&cpu_num, sizeof(int));
    spin_unlock(&scu_wait_sync_lock);

    //printk("cpu%d exit fun, ret=%s\n", cpu, ret ? "pass" : "fail");
    return ret;
}


static int slt_scu_test_func(void *data)
{
    int ret = 0, loop, pass;
    int cpu = raw_smp_processor_id();
    unsigned long irq_flag;
    int cpu_cnt;

    unsigned long buf;
    unsigned long *mem_buf = (unsigned long *)data;
    unsigned long retry;

    //spin_lock(&scu_thread_lock[cpu]);
    //local_irq_save(irq_flag);
#if 0
    if(cpu == 0)
    {
        mtk_wdt_enable(WK_WDT_DIS);
    }
#endif

    if(!mem_buf)
    {
        printk(KERN_ERR, "allocate memory fail for cpu scu test\n");
        g_iCPU_PassFail = -1;
        goto scu_thread_out;
    }

    printk("\n>>slt_scu_test_func -- cpu id = %d, mem_buf = 0x%08x <<\n", cpu, mem_buf);

    msleep(50);

    if(!wait_slt_scu_state_sync(SCU_STATE_START, 1))
    {
        printk("cpu%d wait SCU_STATE_START timeout\n", cpu);

        goto scu_thread_out;
    }
    g_iCPU_PassFail = 0;
    g_iSCU_PassFail[cpu] = 1;

    for (loop = 0; loop < g_iScuLoopCount; loop++) {

        slt_scu_write_state(cpu, SCU_STATE_EXECUTE);
        spin_lock_irqsave(&scu_thread_irq_lock[cpu], irq_flag);
        if(!wait_slt_scu_state_sync(SCU_STATE_EXECUTE, 1))
        {
            spin_unlock_irqrestore(&scu_thread_irq_lock[cpu], irq_flag);
            printk("cpu%d wait SCU_STATE_EXECUTE timeout\n", cpu);
            goto scu_thread_out;
        }

        g_iSCU_PassFail[cpu] = fp6_scu_start(mem_buf);
        spin_unlock_irqrestore(&scu_thread_irq_lock[cpu], irq_flag);

        __cpuc_flush_dcache_area(g_iSCU_PassFail, 2*sizeof(int));

        printk("\n>>cpu%d scu : fp6_scu_start %s ret=0x%x<<\n", cpu, g_iSCU_PassFail[cpu] != 0xA? "fail" : "pass", g_iSCU_PassFail[cpu]);

        slt_scu_write_state(cpu, SCU_STATE_EXEEND);

        if(!wait_slt_scu_state_sync(SCU_STATE_EXEEND, 1))
        {
            printk("cpu%d wait SCU_STATE_EXEEND timeout\n", cpu);
            goto scu_thread_out;

        }

        if(cpu == 0)
        {
            pass = 1;
            for(cpu_cnt = 0; cpu_cnt < NR_CPUS; cpu_cnt++)
            {
                if(g_iSCU_PassFail[cpu_cnt] != 0xA)
                {
                    pass = 0;
                }
            }

            if(pass)
            {
                g_iCPU_PassFail += 1;
            }
        }
    }

scu_thread_out:

    slt_scu_write_state(cpu, SCU_STATE_IDEL);

    if(cpu == 0)
    {
        if (g_iCPU_PassFail == g_iScuLoopCount) {
            printk("\n>> CPU scu test pass <<\n\n");
        }else {
            printk("\n>> CPU scu test fail (loop count = %d)<<\n\n", g_iCPU_PassFail);
        }
        //mtk_wdt_enable(WK_WDT_EN);
    }

    wait_slt_scu_state_sync(SCU_STATE_IDEL, 1);

    printk("cpu%d scu thread out\n", cpu);

    //local_irq_restore(irq_flag);

    //spin_unlock(&scu_thread_lock[cpu]);
    return 0;

}

static ssize_t slt_cpu_scu_store(struct device_driver *driver, const char *buf, size_t count)
{
    int cpu;
    unsigned int i, pass;
    int ret, err;
    struct task_struct *scu_thread[NR_CPUS];
    unsigned char name[20] = {'\0'};
    int loop;

    unsigned long *mem_buf = NULL;

    //spin_lock(&scu_store_lock);
    printk("\n\nStart: slt_cpu_scu_store\n\n");
    mem_buf = (unsigned long *)kmalloc(0x20000, GFP_KERNEL);

    g_iCPU_PassFail = 0;

    if(!mem_buf)
    {
        g_iCPU_PassFail = -1;
        return count;
    }

    for(cpu = 0; cpu < NR_CPUS; cpu++)
    {

        sprintf(name, "scu-test-%d", cpu);
        scu_thread[cpu] = kthread_create(slt_scu_test_func, mem_buf, name);

        if (IS_ERR(scu_thread[cpu])) {
            err = PTR_ERR(scu_thread[cpu]);
            scu_thread[cpu] = NULL;
            printk(KERN_ERR "[%s]: kthread_create %s fail(%d)\n", __func__, name, err);
            goto scu_out;
        }

        kthread_bind(scu_thread[cpu], cpu);
        slt_scu_write_state(cpu, SCU_STATE_START);
        wake_up_process(scu_thread[cpu]);
    }


    while(!is_slt_scu_state_sync(SCU_STATE_IDEL))
    {
        slt_scu_print_state();
        msleep(100);
    }

scu_out:

    printk("End of slt_cpu_scu_store\n");

    kfree(mem_buf);


    //spin_unlock(&scu_store_lock);
    return count;
}

static ssize_t slt_scu_loop_count_store(struct device_driver *driver, const char *buf, size_t count)
{
    int result;

    if ((result = sscanf(buf, "%d", &g_iScuLoopCount)) == 1)
    {
        printk("set SLT scu test loop count = %d successfully\n", g_iScuLoopCount);
    }
    else
    {
        printk("bad argument!!\n");
        return -EINVAL;
    }

    return count;
}

DRIVER_ATTR(slt_cpu_scu, 0644, slt_cpu_scu_show, slt_cpu_scu_store);
DRIVER_ATTR(slt_scu_loop_count, 0644, slt_scu_loop_count_show, slt_scu_loop_count_store);

int __init slt_cpu_scu_init(void)
{
    int ret;
    int cpu;

    ret = driver_register(&slt_cpu_scu_drv);
    if (ret) {
        printk("fail to create SLT CPU scu driver\n");
    }
    else
    {
        printk("success to create SLT CPU scu driver\n");
    }

    ret = driver_create_file(&slt_cpu_scu_drv, &driver_attr_slt_cpu_scu);
    if (ret) {
        printk("fail to create SLT CPU scu sysfs files\n");
    }
    else
    {
        printk("success to create SLT CPU scu sysfs files\n");
    }


    for (cpu = 0; cpu < NR_CPUS; cpu++) {

        //spin_lock_init(&scu_thread_lock[cpu]);
        spin_lock_init(&scu_thread_irq_lock[cpu]);

        g_iSCU_State[cpu] = SCU_STATE_IDEL;
    }

    spin_lock_init(&scu_store_lock);
    spin_lock_init(&scu_wait_sync_lock);

    printk("slt_cpu_scu_init done!\n");

    return 0;
}

int __init slt_scu_loop_count_init(void)
{
    int ret, err;

    ret = driver_register(&slt_scu_loop_count_drv);
    if (ret) {
        printk("fail to create scu loop count driver\n");
    }
    else
    {
        printk("success to create scu loop count driver\n");
    }


    ret = driver_create_file(&slt_scu_loop_count_drv, &driver_attr_slt_scu_loop_count);
    if (ret) {
        printk("fail to create scu loop count sysfs files\n");
    }
    else
    {
        printk("success to create scu loop count sysfs files\n");
    }

    g_iScuLoopCount = SLT_LOOP_CNT;



    return 0;
}
arch_initcall(slt_cpu_scu_init);
arch_initcall(slt_scu_loop_count_init);

