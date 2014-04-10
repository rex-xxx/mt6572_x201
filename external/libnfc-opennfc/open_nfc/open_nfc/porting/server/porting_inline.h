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

#ifndef __PORTING_INLINE_H
#define __PORTING_INLINE_H


#include <stdarg.h>  /* Needed for the traces */

/* Inline implementation of the memory functions  */

#include <stdlib.h>

#define CMemoryAlloc(nSize ) 		malloc(nSize)
#define CMemoryFree( pBuffer )	free(pBuffer)

#include <memory.h>

#define CMemoryCopy( pDestination, pSource, nLength ) \
		memcpy( (pDestination), (pSource), (size_t)(nLength) )

#define CMemoryMove( pDestination, pSource, nLength ) \
		memmove( (pDestination), (pSource), (size_t)(nLength) )

#define CMemoryFill( pBuffer, nValue, nLength ) \
		memset( (pBuffer), (int)(nValue), (size_t)(nLength) )

#define CMemoryCompare( pBuffer1, pBuffer2, nLength ) \
		memcmp( (pBuffer1), (pBuffer2), (size_t)(nLength) )

#define CMemoryResetStatistics()
#define CMemoryGetStatistics(pnCurrentAllocation, pnPeakAllocation) ( * pnCurrentAllocation = 0, *pnPeakAllocation = 0 )

#ifdef P_DEBUG_ACTIVE
#include <assert.h>
#  define CDebugAssert( cond ) assert( cond )
#else
#  define CDebugAssert( cond ) ((void)0)
#endif /* P_DEBUG_ACTIVE */

#define P_INLINE  __inline

/* The synchronization object type */
#include <semaphore.h>
#define P_SYNC_WAIT_OBJECT  sem_t

/* The critical section type */
#include <pthread.h>
#define P_SYNC_CS    pthread_mutex_t

/* Inline definition */
#define CSyncCreateCriticalSection(phCriticalSection) \
            pthread_mutex_init(phCriticalSection, NULL)

/* Inline definition */
#define CSyncEnterCriticalSection(phCriticalSection) \
            pthread_mutex_lock(phCriticalSection)

/* Inline definition */
#define CSyncLeaveCriticalSection(phCriticalSection) \
            pthread_mutex_unlock(phCriticalSection)

/* Inline definition */
#define CSyncDestroyCriticalSection(phCriticalSection) \
            pthread_mutex_destroy(phCriticalSection)


/* The semaphore type */
/* The semaphore type */
typedef struct __P_SYNC_SEMAPHORE
{
	int 	sockets[2];			  /* a socket pair, used for synchronization between processes */

} P_SYNC_SEMAPHORE;


typedef struct __P_SYNC_BUFFER
{
   uint32_t nType;

   void     *pKernelBuffer;
   void     *pUserBuffer;     /* for double-checking upon unmap() */
   uint32_t  nBufferLength;

} P_SYNC_BUFFER;


#endif /* __PORTING_INLINE_H */
