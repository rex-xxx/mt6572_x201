/*
 * Copyright (c) 2007-2010 Inside Contactless, All Rights Reserved.
 *
 * Company Confidential Document
 *
 * Picopass, Open NFC, the Open NFC logo, Wave-Me and the Wave-Me logo are trademarks
 * or registered trademarks of Inside Contactless.
 *
 * Other brand, product and company names mentioned herein may be trademarks,
 * registered trademarks or trade names of their respective owners.
 *
 * The information and source code contained herein is the exclusive
 * property of Inside Contactless and may not be disclosed, examined
 * or reproduced in whole or in part without explicit written authorization
 * from the company.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. INSIDE CONTACTLESS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL
 * INSIDE CONTACTLESS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF INSIDE CONTACTLESS
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

/*******************************************************************************

  This header file contains the functions to implement to port
  Microread NFC HAL on the device OS. For the porting, refer the documentation
  on the function detailed description and the porting process.

*******************************************************************************/

#ifndef __NAL_PORTING_OS_H
#define __NAL_PORTING_OS_H

#include "nal_porting_config.h"
#include "nal_porting_types.h"
#include "nal_porting_inline.h"

/*******************************************************************************
  Memory Functions
*******************************************************************************/

#ifndef CNALMemoryAlloc
void* CNALMemoryAlloc( uint32_t nSize );
#endif /* CNALMemoryAlloc */

#ifndef CNALMemoryFree
void CNALMemoryFree( void* pBuffer );
#endif /* CNALMemoryFree */

#ifndef CNALMemoryCopy
void* CNALMemoryCopy( void* pDestination, const void* pSource, uint32_t nLength );
#endif /* CNALMemoryCopy */

#ifndef CNALMemoryMove
void* CNALMemoryMove( void* pDestination, const void* pSource, uint32_t nLength );
#endif /* CNALMemoryMove */

#ifndef CNALMemoryFill
void CNALMemoryFill( void* pBuffer, uint8_t nValue, uint32_t nLength );
#endif /* CNALMemoryFill */

#ifndef CNALMemoryCompare
int32_t CNALMemoryCompare( const void* pBuffer1, const void* pBuffer2, uint32_t nLength );
#endif /* CNALMemoryCompare */

/*******************************************************************************
  Debug Functions
*******************************************************************************/

#ifndef CNALDebugAssert
#define CNALDebugAssert( cond ) \
   if(!(cond)) CNALDebugAssertFailed(#cond)
#endif /* CNALDebugAssert */

static P_NAL_INLINE void PNALDebugTraceNone(const char* pMessage, ...) {}

#ifndef P_NAL_TRACE_ACTIVE
#define PNALDebugTrace while(0) PNALDebugTraceNone
#define PNALDebugLog while(0) PNALDebugTraceNone
#define PNALDebugWarning while(0) PNALDebugTraceNone
#define PNALDebugError while(0) PNALDebugTraceNone
#define PNALDebugTraceBuffer(pBuffer, nLength) while(0) {}
#define PNALDebugLogBuffer(pBuffer, nLength) while(0) {}
#define PNALDebugWarningBuffer(pBuffer, nLength) while(0) {}
#define PNALDebugErrorBuffer(pBuffer, nLength) while(0) {}
#else /* P_TRACE_ACTIVE */

/* The trace levels */
#define P_TRACE_TRACE      1
#define P_TRACE_LOG        2
#define P_TRACE_WARNING    3
#define P_TRACE_ERROR      4
#define P_TRACE_NONE       5

/* See Functional Specifications Document */
void CNALDebugPrintTrace(
            const char* pTag,
            uint32_t nTraceLevel,
            const char* pMessage,
            va_list list);

/* See Functional Specifications Document */
void CNALDebugPrintTraceBuffer(
            const char* pTag,
            uint32_t nTraceLevel,
            const uint8_t* pBuffer,
            uint32_t nLength);

#ifdef P_MODULE
#  define P_MODULE_DEC(X) P_TRACE_LEVEL_##X
#  if P_MODULE == P_TRACE_TRACE
#     define P_TRACE_LEVEL P_TRACE_TRACE
#  elif P_MODULE == P_TRACE_LOG
#     define P_TRACE_LEVEL P_TRACE_LOG
#  elif P_MODULE == P_TRACE_WARNING
#     define P_TRACE_LEVEL P_TRACE_WARNING
#  elif P_MODULE == P_TRACE_ERROR
#     define P_TRACE_LEVEL P_TRACE_ERROR
#  elif P_MODULE == P_TRACE_NONE
#     define P_TRACE_LEVEL P_TRACE_NONE
#  else
#     define P_TRACE_LEVEL P_TRACE_LEVEL_DEFAULT
#  endif
#  undef P_MODULE_DEC
#  define P_MODULE_DEC(X) #X
#  define P_TRACE_TAG P_MODULE
#else /* P_MODULE */

#  define P_TRACE_TAG  "?????"
#  define P_TRACE_LEVEL P_TRACE_LEVEL_DEFAULT
#endif /* P_MODULE */

#if P_NAL_TRACE_LEVEL <= P_TRACE_TRACE

#ifdef __GNUC__
static void PNALDebugTrace( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PNALDebugTrace( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CNALDebugPrintTrace(P_TRACE_TAG, P_TRACE_TRACE, pMessage, list );
   va_end( list );
}
static P_NAL_INLINE void PNALDebugTraceBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CNALDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_TRACE, pBuffer, nLength );
}
#else
#define PNALDebugTrace while(0) PNALDebugTraceNone
#define PNALDebugTraceBuffer(pBuffer, nLength) while(0) {}
#endif

#if P_TRACE_LEVEL <= P_TRACE_LOG

#ifdef __GNUC__
static void PNALDebugLog( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PNALDebugLog( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CNALDebugPrintTrace(P_TRACE_TAG, P_TRACE_LOG, pMessage, list );
   va_end( list );
}
static P_NAL_INLINE void PNALDebugLogBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CNALDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_LOG, pBuffer, nLength );
}
#else
#define PNALDebugLog while(0) PNALDebugTraceNone
#define PNALDebugLogBuffer(pBuffer, nLength) while(0) {}
#endif

#if P_TRACE_LEVEL <= P_TRACE_WARNING

#ifdef __GNUC__
static void PNALDebugWarning( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PNALDebugWarning( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CNALDebugPrintTrace(P_TRACE_TAG, P_TRACE_WARNING, pMessage, list );
   va_end( list );
}
static P_NAL_INLINE void PNALDebugWarningBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CNALDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_WARNING, pBuffer, nLength );
}
#else
#define PNALDebugWarning while(0) PNALDebugTraceNone
#define PNALDebugWarningBuffer(pBuffer, nLength) while(0) {}
#endif

#if P_TRACE_LEVEL <= P_TRACE_ERROR

#ifdef __GNUC__
static void PNALDebugError( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PNALDebugError( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CNALDebugPrintTrace(P_TRACE_TAG, P_TRACE_ERROR, pMessage, list );
   va_end( list );
}
static P_NAL_INLINE void PNALDebugErrorBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CNALDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_ERROR, pBuffer, nLength );
}
#else
#define PNALDebugError while(0) PNALDebugTraceNone
#define PNALDebugErrorBuffer(pBuffer, nLength) while(0) {}
#endif

#undef P_MODULE_DEC
#undef P_TRACE_TAG

#endif /* P_TRACE_ACTIVE */

/*******************************************************************************
  Critical Section Functions
*******************************************************************************/

#ifndef CNALSyncCreateCriticalSection
void CNALSyncCreateCriticalSection(
            P_NAL_SYNC_CS* phCriticalSection);
#endif /* #ifndef CNALSyncCreateCriticalSection */

#ifndef CNALSyncEnterCriticalSection
void CNALSyncEnterCriticalSection(
            P_NAL_SYNC_CS* phCriticalSection );
#endif /* #ifndef CNALSyncEnterCriticalSection */

#ifndef CNALSyncLeaveCriticalSection
void CNALSyncLeaveCriticalSection(
            P_NAL_SYNC_CS* phCriticalSection );
#endif /* #ifndef CNALSyncLeaveCriticalSection */

#ifndef CNALSyncDestroyCriticalSection
void CNALSyncDestroyCriticalSection(
            P_NAL_SYNC_CS* phCriticalSection );
#endif /* #ifndef CNALSyncDestroyCriticalSection */

#endif /* __NAL_PORTING_OS_H */
