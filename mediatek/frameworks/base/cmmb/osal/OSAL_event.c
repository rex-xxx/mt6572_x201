/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include <pthread.h>
#include <string.h>
#include <sys/time.h>
#include"OSAL_event.h"



unsigned int OSAL_EventCreate ( OSAL_Event * pEvent)
{
	if (pEvent)
	{
		// init mutex for event
		pthread_mutex_init(&pEvent->mutex, NULL);
		// init condition variable
		pthread_cond_init(&pEvent->cond, NULL);
		pEvent->set = 0;
		return 0;
	}
	
	return 1;
}


unsigned int OSAL_EventDelete ( OSAL_Event * pEvent)
{
	if (pEvent)
	{
		pthread_mutex_destroy(&pEvent->mutex);
		pthread_cond_destroy(&pEvent->cond);
		return 0;
	}

	return 1;
}


unsigned int OSAL_EventSet ( OSAL_Event * pEvent)
{
	int result = 0;

	result = pthread_mutex_lock ( &pEvent->mutex );
	if ( result != 0 )
	{
		result = pthread_mutex_unlock (  &pEvent->mutex );
		return result;
	}
	// setflag for reolve problem of wait-signal time seqence.
	pEvent->set = 1;
	result = pthread_cond_signal ( &pEvent->cond );
	if ( result != 0 )
	{
		pthread_mutex_unlock(&pEvent->mutex);
		return result;
	}


	result = pthread_mutex_unlock (  &pEvent->mutex );
	if ( result != 0 )
	{
		return result;
	}

	return result;
}


unsigned int OSAL_EventClear ( OSAL_Event * pEvent)
{
	int result = 0;

	result = pthread_mutex_lock ( &pEvent->mutex );
	if ( result != 0 )
	{
		result = pthread_mutex_unlock (  &pEvent->mutex );
		return result;
	}

	// clear "even set" flag
	pEvent->set = 0;

	result = pthread_mutex_unlock (  &pEvent->mutex );
	if ( result != 0 )
	{
		return result;
	}

	return 0;
}


unsigned int OSAL_EventWait ( OSAL_Event * pEvent, unsigned int Timeout )
{
	int result = 0;
	unsigned int Rc = 0;

	// critical section
	pthread_mutex_lock ( &pEvent->mutex );

	// check "event set" flag
	if ( pEvent->set )
	{
		// if already set reset and return immidiately
		pEvent->set = 0;
		pthread_mutex_unlock ( &pEvent->mutex );
		return 0;
	}

	if(0xFFFFFFFF == Timeout)
	{
		// wait forever
		pthread_cond_wait ( &pEvent->cond, &pEvent->mutex );
		pEvent->set = 0;
	}
	else
	{
		struct timeval now;
		struct timespec timeout;
		gettimeofday(&now, NULL);

		
		timeout.tv_sec = now.tv_sec + (Timeout/1000);
		timeout.tv_nsec = (now.tv_usec*1000) + ((Timeout % 1000) * 1000000);
		if (timeout.tv_nsec >= 1000000000)
		{
			timeout.tv_nsec -= 1000000000;
			timeout.tv_sec++;			
		}

		result = pthread_cond_timedwait ( &pEvent->cond, &pEvent->mutex, &timeout);
		if(!result)
		{
			pEvent->set = 0;		
		}
		else
		{
			Rc = result;
			if ( result == ETIMEDOUT )
			{
				Rc = OSAL_MAX_WAIT;
			}
		}
	}

	// exit critical section
	pthread_mutex_unlock (  &pEvent->mutex );
	return Rc;
}



unsigned int OSAL_MutexCreate (pthread_mutex_t * mutex )
{
	pthread_mutexattr_t attr;
	
	pthread_mutexattr_init(&attr);
	pthread_mutexattr_settype((pthread_mutexattr_t *)&attr, (int)PTHREAD_MUTEX_RECURSIVE_NP);


	int result = pthread_mutex_init ( mutex, &attr );
	
	return result;
}


unsigned int OSAL_MutexDelete ( pthread_mutex_t * mutex )
{
	return pthread_mutex_destroy ( mutex );
}


unsigned int OSAL_MutexPut ( pthread_mutex_t * mutex )
{
	return pthread_mutex_unlock ( mutex );
}


unsigned int OSAL_MutexGet ( pthread_mutex_t * mutex )
{
	return pthread_mutex_lock ( mutex );
}


