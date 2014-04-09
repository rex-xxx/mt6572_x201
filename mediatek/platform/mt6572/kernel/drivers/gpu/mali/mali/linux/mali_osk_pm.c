/**
 * Copyright (C) 2010-2012 ARM Limited. All rights reserved.
 * 
 * This program is free software and is provided to you under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation, and any use by you of this program is subject to the terms of such GNU licence.
 * 
 * A copy of the licence is included with the program, and can also be obtained from Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * @file mali_osk_pm.c
 * Implementation of the callback functions from common power management
 */

#include <linux/sched.h>

#ifdef CONFIG_PM_RUNTIME
#include <linux/pm_runtime.h>
#endif /* CONFIG_PM_RUNTIME */
#include <linux/platform_device.h>
#include <linux/version.h>
#include "mali_osk.h"
#include "mali_kernel_common.h"
#include "mali_kernel_linux.h"
#include "mali_pm.h"
#include "platform_pmm.h"

static _mali_osk_timer_t* pm_timer;
static int mali_suspend_called;
static _mali_osk_lock_t* pm_lock;

/// For MFG sub-system clock control API
#include <mach/mt_clkmgr.h> 
#include <linux/spinlock.h>

static _mali_osk_atomic_t mali_pm_ref_count;

_mali_osk_errcode_t _mali_osk_pm_delete_callback_timer(void)
{
   MALI_DEBUG_PRINT(2, ("_mali_osk_pm_delete_callback_timer (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));
   _mali_osk_timer_del(pm_timer);
}

void _mali_pm_callback(void *arg)
{   
   MALI_DEBUG_PRINT(2, ("_mali_pm_callback (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));
    
   _mali_osk_lock_wait(pm_lock, _MALI_OSK_LOCKMODE_RW);   	
	
	if((_mali_osk_atomic_read(&mali_pm_ref_count) == 0) &&
	   (_mali_osk_atomic_read(&mali_suspend_called) == 0))
	{
	   mali_pm_runtime_suspend();
	   mali_suspend_called++;
	   mali_platform_power_mode_change(MALI_POWER_MODE_DEEP_SLEEP);	   	   
	}
	
	_mali_osk_lock_signal(pm_lock, _MALI_OSK_LOCKMODE_RW);
}


void _mali_osk_pm_dev_enable(void) /* @@@@ todo: change to init of some kind.. or change the way or where atomics are initialized? */
{
	_mali_osk_atomic_init(&mali_pm_ref_count, 0);
	pm_timer = _mali_osk_timer_init();
	_mali_osk_timer_setcallback(pm_timer, _mali_pm_callback, NULL);	
	mali_suspend_called = 0;
	
	pm_lock = _mali_osk_lock_init(_MALI_OSK_LOCKFLAG_SPINLOCK_IRQ | _MALI_OSK_LOCKFLAG_NONINTERRUPTABLE, 0, 0);
}

void _mali_osk_pm_dev_disable(void) /* @@@@ todo: change to term of some kind */
{
	_mali_osk_atomic_term(&mali_pm_ref_count);
	_mali_osk_timer_term(pm_timer);
	_mali_osk_lock_term(pm_lock);
}


/* Can NOT run in atomic context */
_mali_osk_errcode_t _mali_osk_pm_dev_ref_add(void)
{  
#ifdef CONFIG_PM_RUNTIME

	int err;
	MALI_DEBUG_ASSERT_POINTER(mali_platform_device);
	err = pm_runtime_get_sync(&(mali_platform_device->dev));	
#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,37))   
	pm_runtime_mark_last_busy(&(mali_platform_device->dev));
#endif   
	if (0 > err)
	{
		MALI_PRINT_ERROR(("Mali OSK PM: pm_runtime_get_sync() returned error code %d\n", err));
		return _MALI_OSK_ERR_FAULT;
	}
	_mali_osk_atomic_inc(&mali_pm_ref_count);
	MALI_DEBUG_PRINT(4, ("Mali OSK PM: Power ref taken (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));	
#else /// CONFIG_PM_RUNTIME  
				
   _mali_osk_timer_del(pm_timer);	
	
	_mali_osk_lock_wait(pm_lock, _MALI_OSK_LOCKMODE_RW);
	
   mali_platform_power_mode_change(MALI_POWER_MODE_ON);
   	
   if(_mali_osk_atomic_read(&mali_suspend_called))
   {	      		
		mali_pm_runtime_resume();
		mali_suspend_called--;
	}
	
	_mali_osk_atomic_inc(&mali_pm_ref_count);       
   
   _mali_osk_lock_signal(pm_lock, _MALI_OSK_LOCKMODE_RW);
   
   MALI_DEBUG_PRINT(4, ("Mali OSK PM: Power ref taken (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));		  

#endif

	return _MALI_OSK_ERR_OK;
}

/* Can run in atomic context */
void _mali_osk_pm_dev_ref_dec(void)
{
#ifdef CONFIG_PM_RUNTIME
	MALI_DEBUG_ASSERT_POINTER(mali_platform_device);
	_mali_osk_atomic_dec(&mali_pm_ref_count);
#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,37))
	pm_runtime_mark_last_busy(&(mali_platform_device->dev));
	pm_runtime_put_autosuspend(&(mali_platform_device->dev));
#else
	pm_runtime_put(&(mali_platform_device->dev));
#endif
	MALI_DEBUG_PRINT(4, ("Mali OSK PM: Power ref released (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));

#else /// CONFIG_PM_RUNTIME
	u32 ref;
	ref = _mali_osk_atomic_dec_return(&mali_pm_ref_count);     
      	
	if((ref == 0) && 
	   (!_mali_osk_timer_pending(pm_timer)))
	{
		_mali_osk_timer_mod(pm_timer, _mali_osk_time_mstoticks(3000));
	}

	MALI_DEBUG_PRINT(4, ("Mali OSK PM: Power ref released (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));
#endif   
   
}

/* Can run in atomic context */
mali_bool _mali_osk_pm_dev_ref_add_no_power_on(void)
{
#ifdef CONFIG_PM_RUNTIME
	u32 ref;
	MALI_DEBUG_ASSERT_POINTER(mali_platform_device);
	pm_runtime_get_noresume(&(mali_platform_device->dev));
	ref = _mali_osk_atomic_read(&mali_pm_ref_count);
	MALI_DEBUG_PRINT(4, ("Mali OSK PM: No-power ref taken (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));
	return ref > 0 ? MALI_TRUE : MALI_FALSE;
#else
	return MALI_TRUE;
#endif
}

/* Can run in atomic context */
void _mali_osk_pm_dev_ref_dec_no_power_on(void)
{
#ifdef CONFIG_PM_RUNTIME
	MALI_DEBUG_ASSERT_POINTER(mali_platform_device);
#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,37))
	pm_runtime_put_autosuspend(&(mali_platform_device->dev));
#else
	pm_runtime_put(&(mali_platform_device->dev));
#endif
	MALI_DEBUG_PRINT(4, ("Mali OSK PM: No-power ref released (%u)\n", _mali_osk_atomic_read(&mali_pm_ref_count)));
#endif
}
