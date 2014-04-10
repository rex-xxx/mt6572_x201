/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
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

  This header file contains the functions to implement to port Open NFC
  on the device OS. For the porting, refer the documentation on the
  function detailed description and the porting process.

*******************************************************************************/

#ifndef __PORTING_OS_H
#define __PORTING_OS_H

#ifndef  P_CONFIG_DRIVER
/* Define the build configuration for the monolithic porting */
#  define P_CONFIG_MONOLITHIC  1
/* Define the build configuration for the driver porting */
#  define P_CONFIG_DRIVER  2
/* Define the build configuration for the user porting */
#  define P_CONFIG_USER    3
/* Define the build configuration for the client porting */
#  define P_CONFIG_CLIENT    4
/* Define the build configuration for the server porting */
#  define P_CONFIG_SERVER    5
#endif /* #ifndef  P_CONFIG_DRIVER */

#include "porting_config.h"
#include "porting_types.h"
#include "porting_inline.h"

/*******************************************************************************
  Memory Functions
*******************************************************************************/

#ifndef CMemoryAlloc
void* CMemoryAlloc( uint32_t nSize );
#endif /* CMemoryAlloc */

#ifndef CMemoryFree
void CMemoryFree( void* pBuffer );
#endif /* CMemoryFree */

#ifndef CMemoryGetStatistics
void CMemoryGetStatistics(
               uint32_t* pnCurrentAllocation,
               uint32_t* pnPeakAllocation );
#endif /* CMemoryGetStatistics */

#ifndef CMemoryResetStatistics
void CMemoryResetStatistics( void );
#endif /* CMemoryResetStatistics */

#ifndef CMemoryCopy
void* CMemoryCopy( void* pDestination, const void* pSource, uint32_t nLength );
#endif /* CMemoryCopy */

#ifndef CMemoryMove
void* CMemoryMove( void* pDestination, const void* pSource, uint32_t nLength );
#endif /* CMemoryMove */

#ifndef CMemoryFill
void CMemoryFill( void* pBuffer, uint8_t nValue, uint32_t nLength );
#endif /* CMemoryFill */

#ifndef CMemoryCompare
int32_t CMemoryCompare( const void* pBuffer1, const void* pBuffer2, uint32_t nLength );
#endif /* CMemoryCompare */

/*******************************************************************************
  Debug Functions
*******************************************************************************/

#ifndef CDebugAssert
#define CDebugAssert( cond ) \
   if(!(cond)) CDebugAssertFaild(#cond)
#endif /* CDebugAssert */

static P_INLINE void PDebugTraceNone(const char* pMessage, ...) {}

#ifndef P_TRACE_ACTIVE
#define PDebugTrace while(0) PDebugTraceNone
#define PDebugLog while(0) PDebugTraceNone
#define PDebugWarning while(0) PDebugTraceNone
#define PDebugError while(0) PDebugTraceNone
#define PDebugTraceBuffer(pBuffer, nLength) while(0) {}
#define PDebugLogBuffer(pBuffer, nLength) while(0) {}
#define PDebugWarningBuffer(pBuffer, nLength) while(0) {}
#define PDebugErrorBuffer(pBuffer, nLength) while(0) {}
#else /* P_TRACE_ACTIVE */

/* The trace levels */
#define P_TRACE_TRACE      1
#define P_TRACE_LOG        2
#define P_TRACE_WARNING    3
#define P_TRACE_ERROR      4
#define P_TRACE_NONE       5

/* See Functional Specifications Document */
void CDebugPrintTrace(
            const char* pTag,
            uint32_t nTraceLevel,
            const char* pMessage,
            va_list list);

/* See Functional Specifications Document */
void CDebugPrintTraceBuffer(
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

#if P_TRACE_LEVEL <= P_TRACE_TRACE

#ifdef __GNUC__
static void PDebugTrace( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PDebugTrace( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CDebugPrintTrace(P_TRACE_TAG, P_TRACE_TRACE, pMessage, list );
   va_end( list );
}
static P_INLINE void PDebugTraceBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_TRACE, pBuffer, nLength );
}
#else
#define PDebugTrace while(0) PDebugTraceNone
#define PDebugTraceBuffer(pBuffer, nLength) while(0) {}
#endif

#if P_TRACE_LEVEL <= P_TRACE_LOG

#ifdef __GNUC__
static void PDebugLog( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PDebugLog( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CDebugPrintTrace(P_TRACE_TAG, P_TRACE_LOG, pMessage, list );
   va_end( list );
}
static P_INLINE void PDebugLogBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_LOG, pBuffer, nLength );
}
#else
#define PDebugLog while(0) PDebugTraceNone
#define PDebugLogBuffer(pBuffer, nLength) while(0) {}
#endif

#if P_TRACE_LEVEL <= P_TRACE_WARNING

#ifdef __GNUC__
static void PDebugWarning( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PDebugWarning( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CDebugPrintTrace(P_TRACE_TAG, P_TRACE_WARNING, pMessage, list );
   va_end( list );
}
static P_INLINE void PDebugWarningBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_WARNING, pBuffer, nLength );
}
#else
#define PDebugWarning while(0) PDebugTraceNone
#define PDebugWarningBuffer(pBuffer, nLength) while(0) {}
#endif

#if P_TRACE_LEVEL <= P_TRACE_ERROR

#ifdef __GNUC__
static void PDebugError( const char* pMessage, ... ) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused)) ;
#endif

static void PDebugError( const char* pMessage, ... )
{
   va_list list;
   va_start( list, pMessage );
   CDebugPrintTrace(P_TRACE_TAG, P_TRACE_ERROR, pMessage, list );
   va_end( list );
}
static P_INLINE void PDebugErrorBuffer(const uint8_t* pBuffer, uint32_t nLength)
{
   CDebugPrintTraceBuffer(P_TRACE_TAG, P_TRACE_ERROR, pBuffer, nLength );
}
#else
#define PDebugError while(0) PDebugTraceNone
#define PDebugErrorBuffer(pBuffer, nLength) while(0) {}
#endif

#undef P_MODULE_DEC
#undef P_TRACE_TAG

#endif /* P_TRACE_ACTIVE */

/*******************************************************************************
  Critical Section Functions
*******************************************************************************/

#ifndef CSyncCreateCriticalSection
void CSyncCreateCriticalSection(
            P_SYNC_CS* phCriticalSection);
#endif /* #ifndef CSyncCreateCriticalSection */

#ifndef CSyncEnterCriticalSection
void CSyncEnterCriticalSection(
            P_SYNC_CS* phCriticalSection );
#endif /* #ifndef CSyncEnterCriticalSection */

#ifndef CSyncLeaveCriticalSection
void CSyncLeaveCriticalSection(
            P_SYNC_CS* phCriticalSection );
#endif /* #ifndef CSyncLeaveCriticalSection */

#ifndef CSyncDestroyCriticalSection
void CSyncDestroyCriticalSection(
            P_SYNC_CS* phCriticalSection );
#endif /* #ifndef CSyncDestroyCriticalSection */

/*******************************************************************************
  Wait Object Functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifndef CSyncCreateWaitObject
bool_t CSyncCreateWaitObject(
            P_SYNC_WAIT_OBJECT* phWaitObject);
#endif /* #ifndef CSyncCreateWaitObject */

#ifndef CSyncWaitForObject
void CSyncWaitForObject(
            P_SYNC_WAIT_OBJECT* phWaitObject );
#endif /* #ifndef CSyncWaitForObject */

#ifndef CSyncSignalWaitObject
void CSyncSignalWaitObject(
            P_SYNC_WAIT_OBJECT* phWaitObject );
#endif /* #ifndef CSyncSignalWaitObject */

#ifndef CSyncDestroyWaitObject
void CSyncDestroyWaitObject(
            P_SYNC_WAIT_OBJECT* phWaitObject );
#endif /* #ifndef CSyncDestroyWaitObject */


uintptr_t CSyncGetCurrentTaskIdentifier(void);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

/*******************************************************************************
  Semaphore Functions
*******************************************************************************/

#if P_BUILD_CONFIG == P_CONFIG_DRIVER

#ifndef CSyncCreateSemaphore
void CSyncCreateSemaphore(
               P_SYNC_SEMAPHORE* phSemaphore );
#endif /* #ifndef CSyncCreateSemaphore */

#ifndef CSyncDestroySemaphore
void CSyncDestroySemaphore(
               P_SYNC_SEMAPHORE* phSemaphore );
#endif /* #ifndef CSyncDestroySemaphore */

#ifndef CSyncIncrementSemaphore
void CSyncIncrementSemaphore(
               P_SYNC_SEMAPHORE* phSemaphore );
#endif /* #ifndef CSyncIncrementSemaphore */

#ifndef CSyncWaitSemaphore
bool_t CSyncWaitSemaphore(
               P_SYNC_SEMAPHORE* phSemaphore );
#endif /* #ifndef CSyncWaitSemaphore */

#endif /* P_CONFIG_DRIVER */

/*******************************************************************************
  Memory Mapping Functions
*******************************************************************************/

#if P_BUILD_CONFIG == P_CONFIG_DRIVER

void* CSyncMapUserBuffer(
               P_SYNC_BUFFER* phBuffer,
               void* pUserBuffer,
               uint32_t nLength,
               uint32_t nType);

void CSyncCopyToUserBuffer(
               P_SYNC_BUFFER* phBuffer,
               void* pUserBuffer,
               void* pKernelBuffer,
               uint32_t nBufferLength);

void CSyncUnmapUserBuffer(
               P_SYNC_BUFFER* phBuffer,
               void* pUserBuffer,
               void* pKernelBuffer,
               uint32_t nBufferLength);

#endif /* P_CONFIG_DRIVER */

/*******************************************************************************
  Trigger Event Pump Function
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

void CSyncTriggerEventPump(
            void* pPortingConfig);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/*******************************************************************************
  Security Functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tUserIdentity tUserIdentity;

typedef struct __tCSecurityDefaultPrincipal
{
   const uint8_t* pDefaultPrincipalBuffer;
   uint32_t nDefaultPrincipalBufferLength;
} tCSecurityDefaultPrincipal;

#ifdef P_INCLUDE_SE_SECURITY



bool_t CSecurityCreateAuthenticationData(
            const tUserIdentity* pUserIdentity,
            const uint8_t* pApplicationDataBuffer,
            uint32_t nApplicationDataBufferLength,
            const uint8_t** ppAuthenticationData,
            uint32_t* pnAuthenticationDataLength);

void CSecurityDestroyAuthenticationData(
            const uint8_t* pAuthenticationData,
            uint32_t nAuthenticationDataLength);

bool_t CSecurityCheckIdentity(
            uint32_t nSlotIdentifier,
            const tUserIdentity* pUserIdentity,
            const uint8_t* pAuthenticationData,
            uint32_t nAuthenticationDataLength,
            const uint8_t* pPrincipalBuffer,
            uint32_t nPrincipalBufferLength);

bool_t CSecurityCheckImpersonatedIdentity(
            uint32_t nSlotIdentifier,
            const tUserIdentity* pUserIdentity,
            const uint8_t* pAuthenticationData,
            uint32_t nAuthenticationDataLength,
            const uint8_t* pPrincipalBuffer,
            uint32_t nPrincipalBufferLength,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength);

bool_t CSecurityGetConfiguration(
            uint32_t nSlotIdentifier,
            const tCSecurityDefaultPrincipal** ppDefaultPrincipalList,
            uint32_t* pnDefaultPrincipalNumber);

bool_t CSecurityGetIdentityData(
            const tUserIdentity* pUserIdentity,
            uint8_t* pUserIdentityBuffer,
            uint32_t nUserIdentityBufferLength,
            uint32_t* pnActualLength);

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/*******************************************************************************
  Secure Element Functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define C_SE_SLOT_ID_STANDALONE_1   ((uint32_t)101)
#define C_SE_SLOT_ID_STANDALONE_2   ((uint32_t)102)
#define C_SE_SLOT_ID_STANDALONE_3   ((uint32_t)103)
#define C_SE_SLOT_ID_STANDALONE_4   ((uint32_t)104)
#define C_SE_SLOT_ID_PROPRIETARY_1  ((uint32_t)201)
#define C_SE_SLOT_ID_PROPRIETARY_2  ((uint32_t)202)
#define C_SE_SLOT_ID_PROPRIETARY_3  ((uint32_t)203)
#define C_SE_SLOT_ID_PROPRIETARY_4  ((uint32_t)204)
#define C_SE_SLOT_ID_SWP_1          ((uint32_t)301)
#define C_SE_SLOT_ID_SWP_2          ((uint32_t)302)
#define C_SE_SLOT_ID_SWP_3          ((uint32_t)303)
#define C_SE_SLOT_ID_SWP_4          ((uint32_t)304)

#define C_SE_FLAG_REMOVABLE         0x0001
#define C_SE_FLAG_HOT_PLUG          0x0002
#define C_SE_FLAG_UICC              0x0004
#define C_SE_FLAG_COMM_SUPPORT      0x0008
#define C_SE_FLAG_COMM_SWP_SUPPORT  0x0010
#define C_SE_FLAG_STK_REFRESH_SUPPORT 0x0020

#ifdef P_INCLUDE_SE_SECURITY

struct __tCSePorting;

typedef struct __tCSePorting tCSePorting;

#define C_SE_OPERATION_GET_INFO        ((uint32_t)0)
#define C_SE_OPERATION_OPEN            ((uint32_t)1)
#define C_SE_OPERATION_EXCHANGE        ((uint32_t)2)
#define C_SE_OPERATION_CLOSE           ((uint32_t)3)
#define C_SE_NOTIFY_HOT_PLUG           ((uint32_t)4)
#define C_SE_NOTIFY_STK_ACTIVATE_SWP   ((uint32_t)5)
#define C_SE_NOTIFY_STK_REFRESH        ((uint32_t)6)

typedef void tCSeCallback(
         void* pCallbackParameter,
         uint32_t nSlotIdentifier,
         uint32_t nOperation,
         bool_t bSuccess,
         uint32_t nParam1,
         uint32_t nParam2);

tCSePorting* CSeCreate(
         tCSeCallback* pCallback,
         void* pCallbackParameter,
         uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength);

void CSeDestroy(
         tCSePorting* pSePorting );

bool_t CSeGetStaticInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t* pnFlags,
         uint32_t* pnSwpTimeout,
         uint8_t* pNameBuffer,
         uint32_t nNameBufferLength,
         uint32_t* pnActualNameLength );

void CSeGetInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint8_t* pAtrBuffer,
         uint32_t nAtrBufferLength );

void CSeOpenChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nType,
         const uint8_t* pAidBuffer,
         uint32_t nAidLength );

void CSeExchangeApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         const uint8_t* pApduBuffer,
         uint32_t nApduLength,
         uint8_t* pResponseApduBuffer,
         uint32_t pResponseApduBufferLength);

void CSeGetResponseApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength,
         uint32_t * pnResponseApduActualSize);

void CSeCloseChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier);

void CSeTriggerStkPolling(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier);

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* __PORTING_OS_H */
