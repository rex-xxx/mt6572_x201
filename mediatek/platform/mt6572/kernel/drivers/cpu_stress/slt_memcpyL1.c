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

extern int fp2_memcpyL1_start(unsigned long);

static int g_iCPU0_PassFail, g_iCPU1_PassFail;
static int g_iMemcpyL1LoopCount;

static struct device_driver slt_cpu0_memcpyL1_drv =
{
    .name = "slt_cpu0_memcpyL1",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

static struct device_driver slt_cpu1_memcpyL1_drv =
{
    .name = "slt_cpu1_memcpyL1",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

static struct device_driver slt_memcpyL1_loop_count_drv =
{
    .name = "slt_memcpyL1_loop_count",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

#define DEFINE_SLT_CPU_MEMCPYL1_SHOW(_N)    \
static ssize_t slt_cpu##_N##_memcpyL1_show(struct device_driver *driver, char *buf) \
{   \
    if(g_iCPU##_N##_PassFail == -1) \
        return snprintf(buf, PAGE_SIZE, "CPU%d MemcpyL1 - CPU%d is powered off\n", _N, _N); \
    else    \
        return snprintf(buf, PAGE_SIZE, "CPU%d MemcpyL1 - %s(loop_count = %d)\n", _N, g_iCPU##_N##_PassFail != g_iMemcpyL1LoopCount ? "FAIL" : "PASS", g_iCPU##_N##_PassFail);  \
}

DEFINE_SLT_CPU_MEMCPYL1_SHOW(0)
DEFINE_SLT_CPU_MEMCPYL1_SHOW(1)

static ssize_t slt_memcpyL1_loop_count_show(struct device_driver *driver, char *buf)
{
    return snprintf(buf, PAGE_SIZE, "MemcpyL1 Loop Count = %d\n", g_iMemcpyL1LoopCount);
}

#define DEFINE_SLT_CPU_MEMCPYL1_STORE(_N)                               \
static ssize_t slt_cpu##_N##_memcpyL1_store(struct device_driver *driver, const char *buf, size_t count) \
{                                                                       \
    unsigned int i, ret;                                                \
    unsigned long mask;                                                 \
    int retry=0;                                                        \
    DEFINE_SPINLOCK(cpu##_N##_lock);                                    \
    unsigned long cpu##_N##_flags;                                      \
    unsigned char *mem_buf = (unsigned long *)kmalloc(0x10000, GFP_KERNEL); \
                                                                        \
    g_iCPU##_N##_PassFail = 0;                                          \
    if(!mem_buf)                                                        \
    {                                                                   \
        printk("allocate memory for cpu%d speed test fail\n", _N);      \
        return 0;                                                       \
    }                                                                   \
                                                                        \
    printk("allocate memory for cpu%d = 0x%08x\n", _N, mem_buf);        \
                                                                        \
    mask = (1 << _N); /* processor _N */                                \
    while(sched_setaffinity(0, (struct cpumask*) &mask) < 0)            \
    {                                                                   \
        printk("Could not set cpu%d affinity for current process(%d).\n", _N, retry); \
        g_iCPU##_N##_PassFail = -1;                                     \
        retry++;                                                        \
        if(retry > 100)                                                 \
        {                                                               \
            kfree(mem_buf);                                             \
            return count;                                               \
        }                                                               \
    }                                                                   \
                                                                        \
    printk("\n>> CPU%d MemcpyL1 test start (cpu id = %d) <<\n\n", _N, raw_smp_processor_id()); \
                                                                        \
    for (i = 0, g_iCPU##_N##_PassFail = 0; i < g_iMemcpyL1LoopCount; i++) { \
        spin_lock_irqsave(&cpu##_N##_lock, cpu##_N##_flags);            \
        ret = fp2_memcpyL1_start(mem_buf);    /* 1: PASS, 0:Fail, -1: target CPU power off */ \
        spin_unlock_irqrestore(&cpu##_N##_lock, cpu##_N##_flags);       \
        if(ret != -1)                                                   \
        {                                                               \
            g_iCPU##_N##_PassFail += ret;                               \
        }                                                               \
        else                                                            \
        {                                                               \
            g_iCPU##_N##_PassFail = -1;                                 \
            break;                                                      \
        }                                                               \
    }                                                                   \
                                                                        \
    if (g_iCPU##_N##_PassFail == g_iMemcpyL1LoopCount) {                \
        printk("\n>> CPU%d memcpyL1 test pass <<\n\n", _N);             \
    }else {                                                             \
        printk("\n>> CPU%d memcpyL1 test fail (loop count = %d)<<\n\n", _N, g_iCPU##_N##_PassFail); \
    }                                                                   \
                                                                        \
    kfree(mem_buf);                                                     \
    return count;                                                       \
}

DEFINE_SLT_CPU_MEMCPYL1_STORE(0)
DEFINE_SLT_CPU_MEMCPYL1_STORE(1)

static ssize_t slt_memcpyL1_loop_count_store(struct device_driver *driver, const char *buf, size_t count)
{
    int result;

    if ((result = sscanf(buf, "%d", &g_iMemcpyL1LoopCount)) == 1)
    {
        printk("set SLT MemcpyL1 test loop count = %d successfully\n", g_iMemcpyL1LoopCount);
    }
    else
    {
        printk("bad argument!!\n");
        return -EINVAL;
    }

    return count;
}

DRIVER_ATTR(slt_cpu0_memcpyL1, 0644, slt_cpu0_memcpyL1_show, slt_cpu0_memcpyL1_store);
DRIVER_ATTR(slt_cpu1_memcpyL1, 0644, slt_cpu1_memcpyL1_show, slt_cpu1_memcpyL1_store);
DRIVER_ATTR(slt_memcpyL1_loop_count, 0644, slt_memcpyL1_loop_count_show, slt_memcpyL1_loop_count_store);

#define DEFINE_SLT_CPU_MEMCPYL1_INIT(_N)    \
int __init slt_cpu##_N##_memcpyL1_init(void) \
{   \
    int ret;    \
    \
    ret = driver_register(&slt_cpu##_N##_memcpyL1_drv);  \
    if (ret) {  \
        printk("fail to create SLT CPU%d MemcpyL1 driver\n", _N);    \
    }   \
    else    \
    {   \
        printk("success to create SLT CPU%d MemcpyL1 driver\n", _N); \
    }   \
    \
    ret = driver_create_file(&slt_cpu##_N##_memcpyL1_drv, &driver_attr_slt_cpu##_N##_memcpyL1);   \
    if (ret) {  \
        printk("fail to create SLT CPU%d MemcpyL1 sysfs files\n", _N);   \
    }   \
    else    \
    {   \
        printk("success to create SLT CPU%d MemcpyL1 sysfs files\n", _N);    \
    }   \
    \
    return 0;   \
}

DEFINE_SLT_CPU_MEMCPYL1_INIT(0)
DEFINE_SLT_CPU_MEMCPYL1_INIT(1)

int __init slt_memcpyL1_loop_count_init(void)
{
    int ret;

    ret = driver_register(&slt_memcpyL1_loop_count_drv);
    if (ret) {
        printk("fail to create MemcpyL1 loop count driver\n");
    }
    else
    {
        printk("success to create MemcpyL1 loop count driver\n");
    }


    ret = driver_create_file(&slt_memcpyL1_loop_count_drv, &driver_attr_slt_memcpyL1_loop_count);
    if (ret) {
        printk("fail to create MemcpyL1 loop count sysfs files\n");
    }
    else
    {
        printk("success to create MemcpyL1 loop count sysfs files\n");
    }

    g_iMemcpyL1LoopCount = SLT_LOOP_CNT;

    return 0;
}
arch_initcall(slt_cpu0_memcpyL1_init);
arch_initcall(slt_cpu1_memcpyL1_init);
arch_initcall(slt_memcpyL1_loop_count_init);
