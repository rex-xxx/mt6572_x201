/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /*******************************************************************************

  This header file contains the inline defintion for the porting of Open NFC.

*******************************************************************************/

#ifndef __NAL_PORTING_INLINE_H
#define __NAL_PORTING_INLINE_H

#include <stdarg.h>

/* Inline implementation of the memory functions  */

#include <stdlib.h>

#define CNALMemoryAlloc					malloc
#define CNALMemoryFree					free

#include <string.h>
#define CNALMemoryCopy					memcpy
#define CNALMemoryFill					memset
#define CNALMemoryCompare				memcmp
#define CNALMemoryMove					memmove

#ifdef P_NAL_DEBUG_ACTIVE
#	include <assert.h>
#  define CNALDebugAssert( cond ) assert( cond )
#else
#  define CNALDebugAssert( cond ) ((void)0)
#endif /* P_NAL_DEBUG_ACTIVE */

#define P_NAL_INLINE  					inline

/* The critical section type */

#include <pthread.h>
#define P_NAL_SYNC_CS    			pthread_mutex_t

/* Inline definition */
#define CNALSyncCreateCriticalSection(phCriticalSection) \
            pthread_mutex_init(phCriticalSection, NULL)

/* Inline definition */
#define CNALSyncEnterCriticalSection(phCriticalSection) \
            pthread_mutex_lock(phCriticalSection)

/* Inline definition */
#define CNALSyncLeaveCriticalSection(phCriticalSection) \
            pthread_mutex_unlock(phCriticalSection)

/* Inline definition */
#define CNALSyncDestroyCriticalSection(phCriticalSection) \
            pthread_mutex_destroy(phCriticalSection)

#endif /* __NAL_PORTING_INLINE_H */
