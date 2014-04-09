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

#include <time.h>
#include <stdlib.h>
#include "OSAL_task.h"

typedef void *(*PthreadTaskFunc)(void*);

/*!  create a task
	\param[in]	TaskName: name of the Task
	\param[in]	TaskPriority: priority of the task
	\param[in]	TaskStackSize: stack size of the Task
	\param[in]	TaskFunction: the function that this task will run
	\param[in]	TaskFunctionParams: parameters to be passed to the task function
	\return		handle to the task.
*/
void* OSAL_TaskCreate ( const char *TaskName,
					   unsigned int TaskPriority,
					   unsigned int  TaskStackSize,
					   TaskFunc TaskFunction,
					   void *TaskFunctionParams )
{
	pthread_t *TaskHandle;
	pthread_attr_t attr;

	// allocate memory for task structure
	TaskHandle = (pthread_t*)malloc(sizeof(pthread_t));

	pthread_attr_init(&attr);
	pthread_attr_setstacksize (&attr, TaskStackSize);
	
	// create linux thread
	pthread_create ( TaskHandle, &attr,
					 (PthreadTaskFunc)TaskFunction,
					 TaskFunctionParams );
	if ( *TaskHandle == 0 )
	{
		free(TaskHandle);
		return NULL;
	}

	return (void*)TaskHandle;
}

/*!  cancel a task
	\param[in]	pTask: handle to the task.
*/
void OSAL_TaskCleanup ( void* pTask )
{
	// free task structure
	free((pthread_t*)pTask);
}

/*!  sleep
	\param[in]	TaskSleepPeriod: time to sleep.
*/
void OSAL_TaskSleep ( unsigned int  TaskSleepPeriod )
{
	struct timespec ts, tr;
	
	// calculate wakeup time
	ts.tv_sec = TaskSleepPeriod / 1000;
	ts.tv_nsec = ( TaskSleepPeriod % 1000 ) * 1000000;
	// sleep
	nanosleep ( &ts, &tr );
	
}


