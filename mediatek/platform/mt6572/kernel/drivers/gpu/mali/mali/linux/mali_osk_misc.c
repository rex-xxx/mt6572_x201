/*
 * Copyright (C) 2010-2012 ARM Limited. All rights reserved.
 * 
 * This program is free software and is provided to you under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation, and any use by you of this program is subject to the terms of such GNU licence.
 * 
 * A copy of the licence is included with the program, and can also be obtained from Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * @file mali_osk_misc.c
 * Implementation of the OS abstraction layer for the kernel device driver
 */
#include <linux/kernel.h>
#include <asm/uaccess.h>
#include <asm/cacheflush.h>
#include <linux/sched.h>
#include <linux/module.h>
#include "mali_osk.h"
#include "mali_kernel_common.h"

void _mali_osk_dbgmsg( const char *fmt, ... )
{
    va_list args;
    va_start(args, fmt);
    vprintk(fmt, args);
	va_end(args);
}

u32 _mali_osk_snprintf( char *buf, u32 size, const char *fmt, ... )
{
	int res;
	va_list args;
	va_start(args, fmt);

	res = vscnprintf(buf, (size_t)size, fmt, args);

	va_end(args);
	return res;
}

void _mali_osk_abort(void)
{
	/* make a simple fault by dereferencing a NULL pointer */
	dump_stack();
	*(int *)0 = 0;
}

void _mali_osk_break(void)
{
   unsigned long value0 = 0;
   unsigned long value1 = 0;
   unsigned long value2 = 0;
   unsigned long value3 = 0;
   unsigned long value4 = 0;
   unsigned long value5 = 0;
   unsigned long value6 = 0;
   ///
   MALI_DEBUG_PRINT(1, ("_mali_osk_break (%u)\n", 0));

   value0 = (*(volatile unsigned long*)(0xF0000020));
   value1 = (*(volatile unsigned long*)(0xF0000024));
   value2 = (*(volatile unsigned long*)(0xF0006214));
   value3 = (*(volatile unsigned long*)(0xF000623C));
   value4 = (*(volatile unsigned long*)(0xF000660c));
   value5 = (*(volatile unsigned long*)(0xF0006610));
   value6 = (*(volatile unsigned long*)(0xF3000000));

   MALI_DEBUG_PRINT(1, ("clk reg(0xF0000020) = %x\n", value0));
   MALI_DEBUG_PRINT(1, ("clk reg(0xF0000024) = %x\n", value1));
   MALI_DEBUG_PRINT(1, ("clk reg(0xF0006214) = %x\n", value2));
   MALI_DEBUG_PRINT(1, ("clk reg(0xF000623C) = %x\n", value3));
   MALI_DEBUG_PRINT(1, ("clk reg(0xF000660c) = %x\n", value4));
   MALI_DEBUG_PRINT(1, ("clk reg(0xF0006610) = %x\n", value5));
   MALI_DEBUG_PRINT(1, ("clk reg(0xF3000000) = %x\n", value6));

   _mali_osk_abort();
}

u32 _mali_osk_get_pid(void)
{
	/* Thread group ID is the process ID on Linux */
	return (u32)current->tgid;
}

u32 _mali_osk_get_tid(void)
{
	/* pid is actually identifying the thread on Linux */
	return (u32)current->pid;
}
