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

#include <linux/leds-mt65xx.h>
#include <linux/sched.h>
#include <linux/vmalloc.h>
#include "slt.h"
#include <linux/io.h>
#include <asm/pgtable.h>
#if 1
extern int fp5_adv_simd_start(void);

static int g_iCPU0_PassFail, g_iCPU1_PassFail;
static int g_iSimdLoopCount;

static struct device_driver slt_cpu0_simd_drv =
{
    .name = "slt_cpu0_simd",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

static struct device_driver slt_cpu1_simd_drv =
{
    .name = "slt_cpu1_simd",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

static struct device_driver slt_simd_loop_count_drv =
{
    .name = "slt_simd_loop_count",
    .bus = &platform_bus_type,
    .owner = THIS_MODULE,
};

#define DEFINE_SLT_CPU_SIMD_SHOW(_N)    \
static ssize_t slt_cpu##_N##_simd_show(struct device_driver *driver, char *buf) \
{   \
    if(g_iCPU##_N##_PassFail == -1) \
        return snprintf(buf, PAGE_SIZE, "CPU%d SIMD - CPU%d is powered off\n",_N,_N); \
    else    \
        return snprintf(buf, PAGE_SIZE, "CPU%d SIMD - %s(loop_count = %d)\n", _N, g_iCPU##_N##_PassFail != g_iSimdLoopCount ? "FAIL" : "PASS", g_iCPU##_N##_PassFail);  \
}

DEFINE_SLT_CPU_SIMD_SHOW(0)
DEFINE_SLT_CPU_SIMD_SHOW(1)

static ssize_t slt_simd_loop_count_show(struct device_driver *driver, char *buf)
{
    return snprintf(buf, PAGE_SIZE, "SIMD Test Loop Count = %d\n", g_iSimdLoopCount);
}

#define DEFINE_SLT_CPU_SIMD_STORE(_N)    \
static ssize_t slt_cpu##_N##_simd_store(struct device_driver *driver, const char *buf, size_t count)    \
{   \
    unsigned int i, ret;    \
    unsigned long mask; \
    int retry=0;    \
    DEFINE_SPINLOCK(cpu##_N##_lock);    \
    unsigned long cpu##_N##_flags;     \
    \
    g_iCPU##_N##_PassFail = 0;  \
    \
    mask = (1 << _N); /* processor _N */ \
    while(sched_setaffinity(0, (struct cpumask*) &mask) < 0)    \
    {   \
        printk("Could not set cpu%d affinity for current process(%d).\n", _N, retry);  \
        g_iCPU##_N##_PassFail = -1; \
        retry++;    \
        if(retry > 100) \
        {   \
            return count;   \
        }   \
    }   \
    \
    printk("\n>> CPU%d simd test start (cpu id = %d) <<\n\n", _N, raw_smp_processor_id());  \
    \
    for (i = 0, g_iCPU##_N##_PassFail = 0; i < g_iSimdLoopCount; i++) {    \
        spin_lock_irqsave(&cpu##_N##_lock, cpu##_N##_flags);    \
        ret = fp5_adv_simd_start();    /* 1: PASS, 0:Fail, -1: target CPU power off */  \
        spin_unlock_irqrestore(&cpu##_N##_lock, cpu##_N##_flags);   \
        if(ret != -1)   \
        {   \
             g_iCPU##_N##_PassFail += ret;  \
        }   \
        else    \
        {   \
             g_iCPU##_N##_PassFail = -1;    \
             break; \
        }   \
    }   \
    \
    if (g_iCPU##_N##_PassFail == g_iSimdLoopCount) {    \
        printk("\n>> CPU%d simd test pass <<\n\n", _N); \
    }else { \
        printk("\n>> CPU%d simd test fail (loop count = %d)<<\n\n", _N, g_iCPU##_N##_PassFail);  \
    }   \
    \
    return count;   \
}

DEFINE_SLT_CPU_SIMD_STORE(0)
DEFINE_SLT_CPU_SIMD_STORE(1)

static ssize_t slt_simd_loop_count_store(struct device_driver *driver, const char *buf, size_t count)
{
    int result;

    if ((result = sscanf(buf, "%d", &g_iSimdLoopCount)) == 1)
    {
        printk("set SLT simd test loop count = %d successfully\n", g_iSimdLoopCount);
    }
    else
    {
        printk("bad argument!!\n");
        return -EINVAL;
    }

    return count;
}

DRIVER_ATTR(slt_cpu0_simd, 0644, slt_cpu0_simd_show, slt_cpu0_simd_store);
DRIVER_ATTR(slt_cpu1_simd, 0644, slt_cpu1_simd_show, slt_cpu1_simd_store);
DRIVER_ATTR(slt_simd_loop_count, 0644, slt_simd_loop_count_show, slt_simd_loop_count_store);

#define DEFINE_SLT_CPU_SIMD_INIT(_N)    \
int __init slt_cpu##_N##_simd_init(void) \
{   \
    int ret;    \
    \
    ret = driver_register(&slt_cpu##_N##_simd_drv);  \
    if (ret) {  \
        printk("fail to create SLT CPU%d simd driver\n",_N);    \
    }   \
    else    \
    {   \
        printk("success to create SLT CPU%d simd driver\n",_N); \
    }   \
    \
    ret = driver_create_file(&slt_cpu##_N##_simd_drv, &driver_attr_slt_cpu##_N##_simd);   \
    if (ret) {  \
        printk("fail to create SLT CPU%d simd sysfs files\n",_N);   \
    }   \
    else    \
    {   \
        printk("success to create SLT CPU%d simd sysfs files\n",_N);    \
    }   \
    \
    return 0;   \
}

DEFINE_SLT_CPU_SIMD_INIT(0)
DEFINE_SLT_CPU_SIMD_INIT(1)

int __init slt_simd_loop_count_init(void)
{
    int ret;

    ret = driver_register(&slt_simd_loop_count_drv);
    if (ret) {
        printk("fail to create simd loop count driver\n");
    }
    else
    {
        printk("success to create simd loop count driver\n");
    }


    ret = driver_create_file(&slt_simd_loop_count_drv, &driver_attr_slt_simd_loop_count);
    if (ret) {
        printk("fail to create simd loop count sysfs files\n");
    }
    else
    {
        printk("success to create simd loop count sysfs files\n");
    }

    g_iSimdLoopCount = SLT_LOOP_CNT;

    return 0;
}
arch_initcall(slt_cpu0_simd_init);
arch_initcall(slt_cpu1_simd_init);
arch_initcall(slt_simd_loop_count_init);
#endif
