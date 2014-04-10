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
 File auto-generated with the autogen.exe tool - Do not modify manually
 The autogen.exe binary tool, the generation scripts and the files used
 for the source of the generation are available under Apache License, Version 2.0
 ******************************************************************************/

#ifdef P_TRACE_ACTIVE
#include <stdarg.h> /* Needed for the traces */
#endif /* P_TRACE_ACTIVE */
#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER)
extern tContext* g_pContext;

void W14Part3ExchangeRawBits(W_HANDLE hConnection, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t nReaderToCardBufferLastByteBitNumber, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, uint8_t nExpectedBits, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   P14Part3ExchangeRawBits( pContext, hConnection, null, &dfc2, pReaderToCardBuffer, nReaderToCardBufferLength, nReaderToCardBufferLastByteBitNumber, pCardToReaderBuffer, nCardToReaderBufferMaxLength, nExpectedBits, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR W14Part3GetConnectionInfo(W_HANDLE hConnection, tW14Part3ConnectionInfo * p14Part3ConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part3GetConnectionInfo( pContext, hConnection, p14Part3ConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR W14Part3GetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part3GetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR W14Part3ListenToCardDetectionTypeB(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, const uint8_t * pHigherLayerDataBuffer, uint8_t nHigherLayerDataLength, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part3ListenToCardDetectionTypeB( pContext, null, &dfc1, nPriority, nAFI, bUseCID, nCID, nBaudRate, pHigherLayerDataBuffer, nHigherLayerDataLength, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W14Part3SetTimeout(W_HANDLE hConnection, uint32_t nTimeout)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part3SetTimeout( pContext, hConnection, nTimeout );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W14Part4GetConnectionInfo(W_HANDLE hConnection, tW14Part4ConnectionInfo * p14Part4ConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part4GetConnectionInfo( pContext, hConnection, p14Part4ConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR W14Part4GetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part4GetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR W14Part4ListenToCardDetectionTypeA(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part4ListenToCardDetectionTypeA( pContext, null, &dfc1, nPriority, bUseCID, nCID, nBaudRate, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W14Part4ListenToCardDetectionTypeB(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, const uint8_t * pHigherLayerDataBuffer, uint8_t nHigherLayerDataLength, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part4ListenToCardDetectionTypeB( pContext, null, &dfc1, nPriority, nAFI, bUseCID, nCID, nBaudRate, pHigherLayerDataBuffer, nHigherLayerDataLength, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W14Part4SetNAD(W_HANDLE hConnection, uint8_t nNAD)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P14Part4SetNAD( pContext, hConnection, nNAD );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W15GetConnectionInfo(W_HANDLE hConnection, tW15ConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P15GetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR W15GetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P15GetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR W15IsWritable(W_HANDLE hConnection, uint8_t nSectorIndex)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P15IsWritable( pContext, hConnection, nSectorIndex );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W15ListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P15ListenToCardDetection( pContext, null, &dfc1, nPriority, nAFI, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

void W15Read(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   P15Read( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, phOperation );
   PContextReleaseLock(pContext);
}

void W15SetAttribute(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nActions, uint8_t nAFI, uint8_t nDSFID, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   P15SetAttribute( pContext, hConnection, null, &dfc2, nActions, nAFI, nDSFID, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR W15SetTagSize(W_HANDLE hConnection, uint16_t nSectorNumber, uint8_t nSectorSize)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P15SetTagSize( pContext, hConnection, nSectorNumber, nSectorSize );
   PContextReleaseLock(pContext);
   return ret;
}

void W15Write(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   P15Write( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, bLockSectors, phOperation );
   PContextReleaseLock(pContext);
}

void W7816ExchangeAPDU(W_HANDLE hChannel, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendAPDUBuffer, uint32_t nSendAPDUBufferLength, uint8_t * pReceivedAPDUBuffer, uint32_t nReceivedAPDUBufferMaxLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   P7816ExchangeAPDU( pContext, hChannel, null, &dfc2, pSendAPDUBuffer, nSendAPDUBufferLength, pReceivedAPDUBuffer, nReceivedAPDUBufferMaxLength, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR W7816GetAid(W_HANDLE hChannel, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P7816GetAid( pContext, hChannel, pBuffer, nBufferMaxLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W7816GetATR(W_HANDLE hConnection, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P7816GetATR( pContext, hConnection, pBuffer, nBufferMaxLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W7816GetATRSize(W_HANDLE hConnection, uint32_t * pnSize)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P7816GetATRSize( pContext, hConnection, pnSize );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR W7816GetResponseAPDUData(W_HANDLE hChannel, uint8_t * pReceivedAPDUBuffer, uint32_t nReceivedAPDUBufferMaxLength, uint32_t * pnReceivedAPDUActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = P7816GetResponseAPDUData( pContext, hChannel, pReceivedAPDUBuffer, nReceivedAPDUBufferMaxLength, pnReceivedAPDUActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

void W7816OpenChannel(W_HANDLE hConnection, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nType, const uint8_t * pAID, uint32_t nAIDLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   P7816OpenChannel( pContext, hConnection, null, &dfc2, nType, pAID, nAIDLength, phOperation );
   PContextReleaseLock(pContext);
}

void WBasicCancelOperation(W_HANDLE hOperation)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PBasicCancelOperation( pContext, hOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WBasicCheckConnectionProperty(W_HANDLE hConnection, uint8_t nPropertyIdentifier)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBasicCheckConnectionProperty( pContext, hConnection, nPropertyIdentifier );
   PContextReleaseLock(pContext);
   return ret;
}

void WBasicCloseHandle(W_HANDLE hHandle)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PBasicCloseHandle( pContext, hHandle );
   PContextReleaseLock(pContext);
}

void WBasicCloseHandleSafe(W_HANDLE hHandle, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PBasicCloseHandleSafe( pContext, hHandle, null, &dfc2 );
   PContextReleaseLock(pContext);
}

void WBasicExecuteEventLoop(void)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }
   PBasicExecuteEventLoop( pContext );
}

bool_t WBasicGenericSyncPrepare(void * param)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PBasicGenericSyncPrepare( pContext, param );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WBasicGetConnectionProperties(W_HANDLE hConnection, uint8_t * pPropertyArray, uint32_t nArrayLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBasicGetConnectionProperties( pContext, hConnection, pPropertyArray, nArrayLength );
   PContextReleaseLock(pContext);
   return ret;
}

const char * WBasicGetConnectionPropertyName(uint8_t nPropertyIdentifier)
{
   const char * ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (const char *)0;
   }

   PContextLock(pContext);
   ret = PBasicGetConnectionPropertyName( pContext, nPropertyIdentifier );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WBasicGetConnectionPropertyNumber(W_HANDLE hConnection, uint32_t * pnPropertyNumber)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBasicGetConnectionPropertyNumber( pContext, hConnection, pnPropertyNumber );
   PContextReleaseLock(pContext);
   return ret;
}

const char * WBasicGetErrorString(W_ERROR nError)
{
   const char * ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (const char *)0;
   }

   PContextLock(pContext);
   ret = PBasicGetErrorString( pContext, nError );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WBasicInit(const char16_t * pVersionString)
{
   tContext* pContext;

   pContext = g_pContext;
   return PBasicInit( pContext, pVersionString );
}

W_ERROR WBasicPumpEvent(bool_t bWait)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }
   return PBasicPumpEvent( pContext, bWait );
}

void WBasicStopEventLoop(void)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }
   PBasicStopEventLoop( pContext );
}

void WBasicTerminate(void)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }
   PBasicTerminate( pContext );
}

void WBasicTestExchangeMessage(tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendMessageBuffer, uint32_t nSendMessageBufferLength, uint8_t * pReceivedMessageBuffer)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PBasicTestExchangeMessage( pContext, null, &dfc1, pSendMessageBuffer, nSendMessageBufferLength, pReceivedMessageBuffer );
   PContextReleaseLock(pContext);
}

W_ERROR WBPrimeGetConnectionInfo(W_HANDLE hConnection, tWBPrimeConnectionInfo * pBPrimeConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBPrimeGetConnectionInfo( pContext, hConnection, pBPrimeConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WBPrimeGetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBPrimeGetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WBPrimeListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, const uint8_t * pAPGENBuffer, uint32_t nAPGENLength, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBPrimeListenToCardDetection( pContext, null, &dfc1, pAPGENBuffer, nAPGENLength, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WBPrimeSetTimeout(W_HANDLE hConnection, uint32_t nTimeout)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PBPrimeSetTimeout( pContext, hConnection, nTimeout );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
bool_t WDFCPumpJNICallback(uint32_t * pArgs, uint32_t nArgsSize)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PDFCPumpJNICallback( pContext, pArgs, nArgsSize );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WEmulClose(W_HANDLE hHandle, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PEmulClose( pContext, hHandle, null, &dfc2 );
   PContextReleaseLock(pContext);
}

W_ERROR WEmulGetMessageData(W_HANDLE hHandle, uint8_t * pDataBuffer, uint32_t nDataLength, uint32_t * pnActualDataLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PEmulGetMessageData( pContext, hHandle, pDataBuffer, nDataLength, pnActualDataLength );
   PContextReleaseLock(pContext);
   return ret;
}

bool_t WEmulIsPropertySupported(uint8_t nPropertyIdentifier)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PEmulIsPropertySupported( pContext, nPropertyIdentifier );
   PContextReleaseLock(pContext);
   return ret;
}

void WEmulOpenConnectionDriver1(tWBasicGenericCallbackFunction * pOpenCallback, void * pOpenCallbackParameter, const tWEmulConnectionInfo * pEmulConnectionInfo, uint32_t nSize, W_HANDLE * phHandle)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PEmulOpenConnectionDriver1( pContext, (tPBasicGenericCallbackFunction *)pOpenCallback, pOpenCallbackParameter, pEmulConnectionInfo, nSize, phHandle );
   PContextReleaseLock(pContext);
}

void WEmulOpenConnectionDriver2(W_HANDLE hHandle, tWEmulDriverEventReceived * pEventCallback, void * pEventCallbackParameter)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PEmulOpenConnectionDriver2( pContext, hHandle, (tPEmulDriverEventReceived *)pEventCallback, pEventCallbackParameter );
   PContextReleaseLock(pContext);
}

void WEmulOpenConnectionDriver3(W_HANDLE hHandle, tWEmulDriverCommandReceived * pCommandCallback, void * pCommandCallbackParameter)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PEmulOpenConnectionDriver3( pContext, hHandle, (tPEmulDriverCommandReceived *)pCommandCallback, pCommandCallbackParameter );
   PContextReleaseLock(pContext);
}

W_ERROR WEmulSendAnswer(W_HANDLE hDriverConnection, const uint8_t * pDataBuffer, uint32_t nDataLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PEmulSendAnswer( pContext, hDriverConnection, pDataBuffer, nDataLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WFeliCaGetCardList(W_HANDLE hConnection, tWFeliCaConnectionInfo * aFeliCaConnectionInfos, const uint32_t nArraySize)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PFeliCaGetCardList( pContext, hConnection, aFeliCaConnectionInfos, nArraySize );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WFeliCaGetCardListBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PFeliCaGetCardListBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WFeliCaGetCardNumber(W_HANDLE hConnection, uint32_t * pnCardNumber)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PFeliCaGetCardNumber( pContext, hConnection, pnCardNumber );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WFeliCaGetCardNumberBuffer(W_HANDLE hConnection, uint32_t * pCardNumber)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PFeliCaGetCardNumberBuffer( pContext, hConnection, pCardNumber );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
W_ERROR WFeliCaGetConnectionInfo(W_HANDLE hConnection, tWFeliCaConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PFeliCaGetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

W_ERROR WFeliCaListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint16_t nSystemCode, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PFeliCaListenToCardDetection( pContext, null, &dfc1, nPriority, nSystemCode, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

void WFeliCaRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nLength, uint8_t nNumberOfService, const uint16_t * pServiceCodeList, uint8_t nNumberOfBlocks, const uint8_t * pBlockList, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PFeliCaRead( pContext, hConnection, null, &dfc2, pBuffer, nLength, nNumberOfService, pServiceCodeList, nNumberOfBlocks, pBlockList, phOperation );
   PContextReleaseLock(pContext);
}


#ifdef P_INCLUDE_JAVA_API
void WFeliCaReadSimple(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nLength, uint32_t nNumberOfService, uint32_t * pServiceCodeList, uint32_t nNumberOfBlocks, const uint8_t * pBlockList)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PFeliCaReadSimple( pContext, hConnection, null, &dfc2, pBuffer, nLength, nNumberOfService, pServiceCodeList, nNumberOfBlocks, pBlockList );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WFeliCaSelectCard(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const tWFeliCaConnectionInfo * pFeliCaConnectionInfo)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PFeliCaSelectCard( pContext, hConnection, null, &dfc2, pFeliCaConnectionInfo );
   PContextReleaseLock(pContext);
}

void WFeliCaSelectSystem(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t nIndexSubSystem)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PFeliCaSelectSystem( pContext, hConnection, null, &dfc2, nIndexSubSystem );
   PContextReleaseLock(pContext);
}

void WFeliCaWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength, uint8_t nNumberOfService, const uint16_t * pServiceCodeList, uint8_t nNumberOfBlocks, const uint8_t * pBlockList, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PFeliCaWrite( pContext, hConnection, null, &dfc2, pBuffer, nLength, nNumberOfService, pServiceCodeList, nNumberOfBlocks, pBlockList, phOperation );
   PContextReleaseLock(pContext);
}


#ifdef P_INCLUDE_JAVA_API
void WFeliCaWriteSimple(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength, uint32_t nNumberOfService, uint32_t * pServiceCodeList, uint32_t nNumberOfBlocks, const uint8_t * pBlockList)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PFeliCaWriteSimple( pContext, hConnection, null, &dfc2, pBuffer, nLength, nNumberOfService, pServiceCodeList, nNumberOfBlocks, pBlockList );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WHandoverAddBluetoothCarrier(W_HANDLE hConnectionHandover, tWBTPairingInfo * pBluetoothInfo, uint8_t nCarrierPowerState)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverAddBluetoothCarrier( pContext, hConnectionHandover, pBluetoothInfo, nCarrierPowerState );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WHandoverAddWiFiCarrier(W_HANDLE hConnectionHandover, tWWiFiPairingInfo * pWiFiInfo, uint8_t nCarrierPowerState)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverAddWiFiCarrier( pContext, hConnectionHandover, pWiFiInfo, nCarrierPowerState );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WHandoverCreate(W_HANDLE * phMessage)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverCreate( pContext, phMessage );
   PContextReleaseLock(pContext);
   return ret;
}

void WHandoverFormatTag(W_HANDLE hConnectionHandover, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nActionMask, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PHandoverFormatTag( pContext, hConnectionHandover, null, &dfc2, nActionMask, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WHandoverGetBluetoothInfo(W_HANDLE hConnectionHandover, tWBTPairingInfo * pRemoteInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverGetBluetoothInfo( pContext, hConnectionHandover, pRemoteInfo );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WHandoverGetPairingInfo(W_HANDLE hConnectionHandover, tWHandoverPairingInfo * pPairingInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverGetPairingInfo( pContext, hConnectionHandover, pPairingInfo );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WHandoverGetPairingInfoLength(W_HANDLE hConnectionHandover, uint32_t * pnLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverGetPairingInfoLength( pContext, hConnectionHandover, pnLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WHandoverGetWiFiInfo(W_HANDLE hOperation, tWWiFiPairingInfo * pWiFiInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverGetWiFiInfo( pContext, hOperation, pWiFiInfo );
   PContextReleaseLock(pContext);
   return ret;
}

void WHandoverPairingCompletion(W_HANDLE hHandoverConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PHandoverPairingCompletion( pContext, hHandoverConnection, null, &dfc2, phOperation );
   PContextReleaseLock(pContext);
}

void WHandoverPairingStart(W_HANDLE hConnectionHandover, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nMode, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PHandoverPairingStart( pContext, hConnectionHandover, null, &dfc2, nMode, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WHandoverRemoveAllCarrier(W_HANDLE hHandoverConnection)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PHandoverRemoveAllCarrier( pContext, hHandoverConnection );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_HANDLE WJavaCreateByteBuffer(uint8_t * pJavaBuffer, uint32_t nOffset, uint32_t nLength)
{
   W_HANDLE ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (W_HANDLE)0;
   }

   PContextLock(pContext);
   ret = PJavaCreateByteBuffer( pContext, pJavaBuffer, nOffset, nLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint32_t WJavaGetByteBufferLength(W_HANDLE hBufferReference)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PJavaGetByteBufferLength( pContext, hBufferReference );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint32_t WJavaGetByteBufferOffset(W_HANDLE hBufferReference)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PJavaGetByteBufferOffset( pContext, hBufferReference );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint8_t * WJavaGetByteBufferPointer(W_HANDLE hBufferReference)
{
   uint8_t * ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint8_t *)0;
   }

   PContextLock(pContext);
   ret = PJavaGetByteBufferPointer( pContext, hBufferReference );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaGetConnectionPropertyNameBuffer(uint8_t nPropertyIdentifier, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaGetConnectionPropertyNameBuffer( pContext, nPropertyIdentifier, pBuffer, nBufferMaxLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaHandoverAddBluetoothCarrierBuffer(W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength, uint8_t nCarrierPowerState)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaHandoverAddBluetoothCarrierBuffer( pContext, hConnectionHandover, pHandoverPairingInfoBuffer, pHandoverPairingInfoBufferLength, nCarrierPowerState );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaHandoverAddWiFiCarrierBuffer(W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength, uint8_t nCarrierPowerState)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaHandoverAddWiFiCarrierBuffer( pContext, hConnectionHandover, pHandoverPairingInfoBuffer, pHandoverPairingInfoBufferLength, nCarrierPowerState );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaHandoverGetBluetoothInfoBuffer(W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaHandoverGetBluetoothInfoBuffer( pContext, hConnectionHandover, pHandoverPairingInfoBuffer, pHandoverPairingInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaHandoverGetPairingInfoBuffer(W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaHandoverGetPairingInfoBuffer( pContext, hConnectionHandover, pHandoverPairingInfoBuffer, pHandoverPairingInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaHandoverGetWiFiInfoBuffer(W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaHandoverGetWiFiInfoBuffer( pContext, hConnectionHandover, pHandoverPairingInfoBuffer, pHandoverPairingInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint32_t WJavaNDEFGetMessageContent(W_HANDLE hMessage, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PJavaNDEFGetMessageContent( pContext, hMessage, pMessageBuffer, nMessageBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void WJavaNdefSendMessage(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PJavaNdefSendMessage( pContext, null, &dfc1, pMessageBuffer, nMessageBufferLength, phOperation );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void WJavaNDEFWriteMessage(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t nActionMask, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PJavaNDEFWriteMessage( pContext, hConnection, null, &dfc2, pMessageBuffer, nMessageBufferLength, nActionMask, phOperation );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void WJavaNDEFWriteMessageOnAnyTag(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t nActionMask, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PJavaNDEFWriteMessageOnAnyTag( pContext, null, &dfc1, nPriority, pMessageBuffer, nMessageBufferLength, nActionMask, phOperation );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
bool_t WJavaNFCControllerGetBooleanProperty(uint8_t nPropertyIdentifier)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PJavaNFCControllerGetBooleanProperty( pContext, nPropertyIdentifier );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaP2PGetConfigurationBuffer(uint8_t * pConfigurationBuffer, uint32_t nConfigurationBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaP2PGetConfigurationBuffer( pContext, pConfigurationBuffer, nConfigurationBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaP2PGetLinkPropertiesBuffer(W_HANDLE hLink, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaP2PGetLinkPropertiesBuffer( pContext, hLink, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR WJavaP2PSetConfigurationBuffer(uint8_t * pConfigurationBuffer, uint32_t nConfigurationBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PJavaP2PSetConfigurationBuffer( pContext, pConfigurationBuffer, nConfigurationBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void WJavaReleaseByteBuffer(W_HANDLE hBufferReference, uint8_t * pJavaBuffer, uint32_t nJavaBufferLength)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PJavaReleaseByteBuffer( pContext, hBufferReference, pJavaBuffer, nJavaBufferLength );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WMifareClassicAuthenticate(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nSectorNumber, bool_t bWithKeyA, const uint8_t * pKey, uint8_t nKeyLength)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareClassicAuthenticate( pContext, hConnection, null, &dfc2, nSectorNumber, bWithKeyA, pKey, nKeyLength );
   PContextReleaseLock(pContext);
}

W_ERROR WMifareClassicGetConnectionInfo(W_HANDLE hConnection, tWMifareClassicConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareClassicGetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WMifareClassicGetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareClassicGetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WMifareClassicGetSectorInfo(W_HANDLE hConnection, uint16_t nSectorNumber, tWMifareClassicSectorInfo * pSectorInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareClassicGetSectorInfo( pContext, hConnection, nSectorNumber, pSectorInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WMifareClassicGetSectorInfoBuffer(W_HANDLE hConnection, uint16_t nSectorNumber, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareClassicGetSectorInfoBuffer( pContext, hConnection, nSectorNumber, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WMifareClassicReadBlock(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nBlockNumber, uint8_t * pBuffer, uint8_t nBufferLength)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareClassicReadBlock( pContext, hConnection, null, &dfc2, nBlockNumber, pBuffer, nBufferLength );
   PContextReleaseLock(pContext);
}

void WMifareClassicWriteBlock(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nBlockNumber, const uint8_t * pBuffer, uint8_t nBufferLength)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareClassicWriteBlock( pContext, hConnection, null, &dfc2, nBlockNumber, pBuffer, nBufferLength );
   PContextReleaseLock(pContext);
}

W_ERROR WMifareGetConnectionInfo(W_HANDLE hConnection, tWMifareConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareGetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WMifareGetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareGetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WMifareRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareRead( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, phOperation );
   PContextReleaseLock(pContext);
}

void WMifareULCAuthenticate(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pKey, uint32_t nKeyLength)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareULCAuthenticate( pContext, hConnection, null, &dfc2, pKey, nKeyLength );
   PContextReleaseLock(pContext);
}

void WMifareULCSetAccessRights(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pKey, uint32_t nKeyLength, uint8_t nThreshold, uint32_t nRights, bool_t bLockConfiguration)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareULCSetAccessRights( pContext, hConnection, null, &dfc2, pKey, nKeyLength, nThreshold, nRights, bLockConfiguration );
   PContextReleaseLock(pContext);
}

W_ERROR WMifareULForceULC(W_HANDLE hConnection)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareULForceULC( pContext, hConnection );
   PContextReleaseLock(pContext);
   return ret;
}

void WMifareULFreezeDataLockConfiguration(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareULFreezeDataLockConfiguration( pContext, hConnection, null, &dfc2 );
   PContextReleaseLock(pContext);
}

W_ERROR WMifareULGetAccessRights(W_HANDLE hConnection, uint32_t nOffset, uint32_t nLength, uint32_t * pnRights)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMifareULGetAccessRights( pContext, hConnection, nOffset, nLength, pnRights );
   PContextReleaseLock(pContext);
   return ret;
}

void WMifareULRetrieveAccessRights(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareULRetrieveAccessRights( pContext, hConnection, null, &dfc2 );
   PContextReleaseLock(pContext);
}

void WMifareWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMifareWrite( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, bLockSectors, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WMyDGetConnectionInfo(W_HANDLE hConnection, tWMyDConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMyDGetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WMyDGetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PMyDGetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WMyDMoveAuthenticate(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nPassword)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMyDMoveAuthenticate( pContext, hConnection, null, &dfc2, nPassword );
   PContextReleaseLock(pContext);
}

void WMyDMoveFreezeDataLockConfiguration(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMyDMoveFreezeDataLockConfiguration( pContext, hConnection, null, &dfc2 );
   PContextReleaseLock(pContext);
}

void WMyDMoveGetConfiguration(W_HANDLE hConnection, tWMyDMoveGetConfigurationCompleted * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMyDMoveGetConfiguration( pContext, hConnection, null, &dfc2 );
   PContextReleaseLock(pContext);
}

void WMyDMoveSetConfiguration(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nStatusByte, uint8_t nPasswordRetryCounter, uint32_t nPassword, bool_t bLockConfiguration)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMyDMoveSetConfiguration( pContext, hConnection, null, &dfc2, nStatusByte, nPasswordRetryCounter, nPassword, bLockConfiguration );
   PContextReleaseLock(pContext);
}

void WMyDRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMyDRead( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, phOperation );
   PContextReleaseLock(pContext);
}

void WMyDWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PMyDWrite( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, bLockSectors, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WNDEFAppendRecord(W_HANDLE hMessage, W_HANDLE hRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFAppendRecord( pContext, hMessage, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFBuildMessage(const uint8_t * pBuffer, uint32_t nBufferLength, W_HANDLE * phMessage)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFBuildMessage( pContext, pBuffer, nBufferLength, phMessage );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFBuildRecord(const uint8_t * pBuffer, uint32_t nBufferLength, W_HANDLE * phRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFBuildRecord( pContext, pBuffer, nBufferLength, phRecord );
   PContextReleaseLock(pContext);
   return ret;
}

bool_t WNDEFCheckIdentifier(W_HANDLE hRecord, const char16_t * pIdentifierString)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PNDEFCheckIdentifier( pContext, hRecord, pIdentifierString );
   PContextReleaseLock(pContext);
   return ret;
}

bool_t WNDEFCheckType(W_HANDLE hRecord, uint8_t nTNF, const char16_t * pTypeString)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PNDEFCheckType( pContext, hRecord, nTNF, pTypeString );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFCreateNestedMessageRecord(uint8_t nTNF, const char16_t * pTypeString, W_HANDLE hNestedMessage, W_HANDLE * phRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFCreateNestedMessageRecord( pContext, nTNF, pTypeString, hNestedMessage, phRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFCreateNewMessage(W_HANDLE * phMessage)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFCreateNewMessage( pContext, phMessage );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFCreateRecord(uint8_t nTNF, const char16_t * pTypeString, const uint8_t * pPayloadBuffer, uint32_t nPayloadLength, W_HANDLE * phRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFCreateRecord( pContext, nTNF, pTypeString, pPayloadBuffer, nPayloadLength, phRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetEnclosedMessage(W_HANDLE hRecord, W_HANDLE * phMessage)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetEnclosedMessage( pContext, hRecord, phMessage );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetIdentifierString(W_HANDLE hRecord, char16_t * pIdentifierStringBuffer, uint32_t nIdentifierStringBufferLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetIdentifierString( pContext, hRecord, pIdentifierStringBuffer, nIdentifierStringBufferLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetMessageContent(W_HANDLE hMessage, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetMessageContent( pContext, hMessage, pMessageBuffer, nMessageBufferLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

uint32_t WNDEFGetMessageLength(W_HANDLE hMessage)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PNDEFGetMessageLength( pContext, hMessage );
   PContextReleaseLock(pContext);
   return ret;
}

W_HANDLE WNDEFGetNextMessage(W_HANDLE hMessage)
{
   W_HANDLE ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (W_HANDLE)0;
   }

   PContextLock(pContext);
   ret = PNDEFGetNextMessage( pContext, hMessage );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetPayloadPointer(W_HANDLE hRecord, uint8_t ** ppBuffer)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetPayloadPointer( pContext, hRecord, ppBuffer );
   PContextReleaseLock(pContext);
   return ret;
}

W_HANDLE WNDEFGetRecord(W_HANDLE hMessage, uint32_t nIndex)
{
   W_HANDLE ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (W_HANDLE)0;
   }

   PContextLock(pContext);
   ret = PNDEFGetRecord( pContext, hMessage, nIndex );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetRecordInfo(W_HANDLE hRecord, uint32_t nInfoType, uint32_t * pnValue)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetRecordInfo( pContext, hRecord, nInfoType, pnValue );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetRecordInfoBuffer(W_HANDLE hRecord, uint32_t nInfoType, uint8_t * pBuffer, uint32_t nBufferLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetRecordInfoBuffer( pContext, hRecord, nInfoType, pBuffer, nBufferLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

uint32_t WNDEFGetRecordNumber(W_HANDLE hMessage)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PNDEFGetRecordNumber( pContext, hMessage );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFGetTagInfo(W_HANDLE hConnection, tNDEFTagInfo * pTagInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetTagInfo( pContext, hConnection, pTagInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WNDEFGetTagInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetTagInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WNDEFGetTypeString(W_HANDLE hRecord, char16_t * pTypeStringBuffer, uint32_t nTypeStringBufferLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFGetTypeString( pContext, hRecord, pTypeStringBuffer, nTypeStringBufferLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

void WNDEFHandlerWorkPerformed(bool_t bGiveToNextListener, bool_t bMessageMatch)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNDEFHandlerWorkPerformed( pContext, bGiveToNextListener, bMessageMatch );
   PContextReleaseLock(pContext);
}

W_ERROR WNDEFInsertRecord(W_HANDLE hMessage, uint32_t nIndex, W_HANDLE hRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFInsertRecord( pContext, hMessage, nIndex, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

void WNDEFReadMessage(W_HANDLE hConnection, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNDEFReadMessage( pContext, hConnection, null, &dfc2, nTNF, pTypeString, phOperation );
   PContextReleaseLock(pContext);
}


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
void WNDEFReadMessageOnAnyTag(tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phRegistry)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNDEFReadMessageOnAnyTag( pContext, null, &dfc1, nPriority, nTNF, pTypeString, phRegistry );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

W_ERROR WNDEFRegisterMessageHandler(tWBasicGenericHandleCallbackFunction * pHandler, void * pHandlerParameter, const uint8_t * pPropertyArray, uint32_t nPropertyNumber, uint8_t nPriority, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFRegisterMessageHandler( pContext, null, &dfc1, pPropertyArray, nPropertyNumber, nPriority, nTNF, pTypeString, phRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFRemoveRecord(W_HANDLE hMessage, uint32_t nIndex)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFRemoveRecord( pContext, hMessage, nIndex );
   PContextReleaseLock(pContext);
   return ret;
}

void WNDEFSendMessage(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pPropertyArray, uint32_t nPropertyNumber, W_HANDLE hMessage, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNDEFSendMessage( pContext, null, &dfc1, pPropertyArray, nPropertyNumber, hMessage, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WNDEFSetIdentifierString(W_HANDLE hRecord, const char16_t * pIdentifierString)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFSetIdentifierString( pContext, hRecord, pIdentifierString );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFSetRecord(W_HANDLE hMessage, uint32_t nIndex, W_HANDLE hRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFSetRecord( pContext, hMessage, nIndex, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNDEFSetRecordInfo(W_HANDLE hRecord, uint32_t nInfoType, const uint8_t * pBuffer, uint32_t nBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNDEFSetRecordInfo( pContext, hRecord, nInfoType, pBuffer, nBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

void WNDEFWriteMessage(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, W_HANDLE hMessage, uint32_t nActionMask, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNDEFWriteMessage( pContext, hConnection, null, &dfc2, hMessage, nActionMask, phOperation );
   PContextReleaseLock(pContext);
}

void WNDEFWriteMessageOnAnyTag(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, W_HANDLE hMessage, uint32_t nActionMask, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNDEFWriteMessageOnAnyTag( pContext, null, &dfc1, nPriority, hMessage, nActionMask, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WNFCControllerActivateSwpLine(uint32_t nSlotIdentifier)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerActivateSwpLine( pContext, nSlotIdentifier );
   PContextReleaseLock(pContext);
   return ret;
}

void WNFCControllerFirmwareUpdate(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pUpdateBuffer, uint32_t nUpdateBufferLength, uint32_t nMode)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerFirmwareUpdate( pContext, null, &dfc1, pUpdateBuffer, nUpdateBufferLength, nMode );
   PContextReleaseLock(pContext);
}

uint32_t WNFCControllerFirmwareUpdateState(void)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PNFCControllerFirmwareUpdateState( pContext );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNFCControllerGetBooleanProperty(uint8_t nPropertyIdentifier, bool_t * pbValue)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerGetBooleanProperty( pContext, nPropertyIdentifier, pbValue );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNFCControllerGetIntegerProperty(uint8_t nPropertyIdentifier, uint32_t * pnValue)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerGetIntegerProperty( pContext, nPropertyIdentifier, pnValue );
   PContextReleaseLock(pContext);
   return ret;
}

uint32_t WNFCControllerGetMode(void)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PNFCControllerGetMode( pContext );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNFCControllerGetProperty(uint8_t nPropertyIdentifier, char16_t * pValueBuffer, uint32_t nBufferLength, uint32_t * pnValueLength)
{
   tContext* pContext;

   pContext = g_pContext;
   return PNFCControllerGetProperty( pContext, nPropertyIdentifier, pValueBuffer, nBufferLength, pnValueLength );
}

W_ERROR WNFCControllerGetRawMessageData(uint8_t * pBuffer, uint32_t nBufferLength, uint32_t * pnActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerGetRawMessageData( pContext, pBuffer, nBufferLength, pnActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

void WNFCControllerGetRFActivity(uint8_t * pnReaderState, uint8_t * pnCardState, uint8_t * pnP2PState)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerGetRFActivity( pContext, pnReaderState, pnCardState, pnP2PState );
   PContextReleaseLock(pContext);
}

void WNFCControllerGetRFLock(uint32_t nLockSet, bool_t * pbReaderLock, bool_t * pbCardLock)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerGetRFLock( pContext, nLockSet, pbReaderLock, pbCardLock );
   PContextReleaseLock(pContext);
}

W_ERROR WNFCControllerMonitorException(tWBasicGenericEventHandler * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerMonitorException( pContext, (tPBasicGenericEventHandler *)pHandler, pHandlerParameter, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WNFCControllerMonitorFieldEvents(tWBasicGenericEventHandler * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerMonitorFieldEvents( pContext, (tPBasicGenericEventHandler *)pHandler, pHandlerParameter, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

void WNFCControllerProductionTest(const uint8_t * pParameterBuffer, uint32_t nParameterBufferLength, uint8_t * pResultBuffer, uint32_t nResultBufferLength, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc5;
   dfc5.pFunction = (tDFCCallback*)pCallback;
   dfc5.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerProductionTest( pContext, pParameterBuffer, nParameterBufferLength, pResultBuffer, nResultBufferLength, null, &dfc5 );
   PContextReleaseLock(pContext);
}

W_ERROR WNFCControllerRegisterRawListener(tWBasicGenericDataCallbackFunction * pReceiveMessageEventHandler, void * pHandlerParameter)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerRegisterRawListener( pContext, (tPBasicGenericDataCallbackFunction *)pReceiveMessageEventHandler, pHandlerParameter );
   PContextReleaseLock(pContext);
   return ret;
}

void WNFCControllerReset(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nMode)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerReset( pContext, null, &dfc1, nMode );
   PContextReleaseLock(pContext);
}

void WNFCControllerSelfTest(tWNFCControllerSelfTestCompleted * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerSelfTest( pContext, null, &dfc1 );
   PContextReleaseLock(pContext);
}

void WNFCControllerSetRFLock(uint32_t nLockSet, bool_t bReaderLock, bool_t bCardLock, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc4;
   dfc4.pFunction = (tDFCCallback*)pCallback;
   dfc4.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerSetRFLock( pContext, nLockSet, bReaderLock, bCardLock, null, &dfc4 );
   PContextReleaseLock(pContext);
}

W_ERROR WNFCControllerSwitchStandbyMode(bool_t bStandbyOn)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PNFCControllerSwitchStandbyMode( pContext, bStandbyOn );
   PContextReleaseLock(pContext);
   return ret;
}

void WNFCControllerSwitchToRawMode(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerSwitchToRawMode( pContext, null, &dfc1 );
   PContextReleaseLock(pContext);
}

void WNFCControllerWriteRawMessage(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PNFCControllerWriteRawMessage( pContext, null, &dfc1, pBuffer, nLength );
   PContextReleaseLock(pContext);
}

void WP2PConnect(W_HANDLE hSocket, W_HANDLE hLink, tWBasicGenericCallbackFunction * pEstablishmentCallback, void * pEstablishmentCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc3;
   dfc3.pFunction = (tDFCCallback*)pEstablishmentCallback;
   dfc3.pParameter = pEstablishmentCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PConnect( pContext, hSocket, hLink, null, &dfc3 );
   PContextReleaseLock(pContext);
}

W_ERROR WP2PCreateSocket(uint8_t nType, const char16_t * pServiceURI, uint8_t nSAP, W_HANDLE * phSocket)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PP2PCreateSocket( pContext, nType, pServiceURI, nSAP, phSocket );
   PContextReleaseLock(pContext);
   return ret;
}

W_HANDLE WP2PEstablishLinkDriver1(tWBasicGenericHandleCallbackFunction * pEstablishmentCallback, void * pEstablishmentCallbackParameter)
{
   W_HANDLE ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (W_HANDLE)0;
   }

   PContextLock(pContext);
   ret = PP2PEstablishLinkDriver1( pContext, (tPBasicGenericHandleCallbackFunction *)pEstablishmentCallback, pEstablishmentCallbackParameter );
   PContextReleaseLock(pContext);
   return ret;
}

void WP2PEstablishLinkDriver2(W_HANDLE hLink, tWBasicGenericCallbackFunction * pReleaseCallback, void * pReleaseCallbackParameter, W_HANDLE * phOperation)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PEstablishLinkDriver2( pContext, hLink, (tPBasicGenericCallbackFunction *)pReleaseCallback, pReleaseCallbackParameter, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WP2PGetConfiguration(tWP2PConfiguration * pConfiguration)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PP2PGetConfiguration( pContext, pConfiguration );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WP2PGetLinkProperties(W_HANDLE hLink, tWP2PLinkProperties * pProperties)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PP2PGetLinkProperties( pContext, hLink, pProperties );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WP2PGetSocketParameter(W_HANDLE hSocket, uint32_t nParameter, uint32_t * pnValue)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PP2PGetSocketParameter( pContext, hSocket, nParameter, pnValue );
   PContextReleaseLock(pContext);
   return ret;
}

void WP2PRead(W_HANDLE hSocket, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pReceptionBuffer, uint32_t nReceptionBufferLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PRead( pContext, hSocket, null, &dfc2, pReceptionBuffer, nReceptionBufferLength, phOperation );
   PContextReleaseLock(pContext);
}

void WP2PRecvFrom(W_HANDLE hSocket, tWP2PRecvFromCompleted * pCallback, void * pCallbackParameter, uint8_t * pReceptionBuffer, uint32_t nReceptionBufferLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PRecvFrom( pContext, hSocket, null, &dfc2, pReceptionBuffer, nReceptionBufferLength, phOperation );
   PContextReleaseLock(pContext);
}

void WP2PSendTo(W_HANDLE hSocket, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nSAP, const uint8_t * pSendBuffer, uint32_t nSendBufferLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PSendTo( pContext, hSocket, null, &dfc2, nSAP, pSendBuffer, nSendBufferLength, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WP2PSetConfiguration(const tWP2PConfiguration * pConfiguration)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PP2PSetConfiguration( pContext, pConfiguration );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WP2PSetSocketParameter(W_HANDLE hSocket, uint32_t nParameter, uint32_t nValue)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PP2PSetSocketParameter( pContext, hSocket, nParameter, nValue );
   PContextReleaseLock(pContext);
   return ret;
}

void WP2PShutdown(W_HANDLE hSocket, tWBasicGenericCallbackFunction * pReleaseCallback, void * pReleaseCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pReleaseCallback;
   dfc2.pParameter = pReleaseCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PShutdown( pContext, hSocket, null, &dfc2 );
   PContextReleaseLock(pContext);
}

void WP2PURILookup(W_HANDLE hLink, tWP2PURILookupCompleted * pCallback, void * pCallbackParameter, const char16_t * pServiceURI)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PURILookup( pContext, hLink, null, &dfc2, pServiceURI );
   PContextReleaseLock(pContext);
}

void WP2PWrite(W_HANDLE hSocket, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendBuffer, uint32_t nSendBufferLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PP2PWrite( pContext, hSocket, null, &dfc2, pSendBuffer, nSendBufferLength, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WPicoGetConnectionInfo(W_HANDLE hConnection, tWPicoConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PPicoGetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WPicoGetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PPicoGetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WPicoIsWritable(W_HANDLE hConnection)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PPicoIsWritable( pContext, hConnection );
   PContextReleaseLock(pContext);
   return ret;
}

void WPicoRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PPicoRead( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, phOperation );
   PContextReleaseLock(pContext);
}

void WPicoWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockCard, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PPicoWrite( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, bLockCard, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WReaderErrorEventRegister(tWBasicGenericEventHandler * pHandler, void * pHandlerParameter, uint8_t nEventType, bool_t bCardDetectionRequested, W_HANDLE * phRegistryHandle)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PReaderErrorEventRegister( pContext, (tPBasicGenericEventHandler *)pHandler, pHandlerParameter, nEventType, bCardDetectionRequested, phRegistryHandle );
   PContextReleaseLock(pContext);
   return ret;
}

void WReaderExchangeDataEx(W_HANDLE hConnection, uint8_t nPropertyIdentifier, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc3;
   dfc3.pFunction = (tDFCCallback*)pCallback;
   dfc3.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PReaderExchangeDataEx( pContext, hConnection, nPropertyIdentifier, null, &dfc3, pReaderToCardBuffer, nReaderToCardBufferLength, pCardToReaderBuffer, nCardToReaderBufferMaxLength, phOperation );
   PContextReleaseLock(pContext);
}

W_ERROR WReaderGetIdentifier(W_HANDLE hConnection, uint8_t * pIdentifierBuffer, uint32_t nIdentifierBufferMaxLength, uint32_t * pnIdentifierActualLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PReaderGetIdentifier( pContext, hConnection, pIdentifierBuffer, nIdentifierBufferMaxLength, pnIdentifierActualLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WReaderGetPulsePeriod(uint32_t * pnTimeout)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PReaderGetPulsePeriod( pContext, pnTimeout );
   PContextReleaseLock(pContext);
   return ret;
}

void WReaderHandlerWorkPerformed(W_HANDLE hConnection, bool_t bGiveToNextListener, bool_t bCardApplicationMatch)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PReaderHandlerWorkPerformed( pContext, hConnection, bGiveToNextListener, bCardApplicationMatch );
   PContextReleaseLock(pContext);
}

bool_t WReaderIsPropertySupported(uint8_t nPropertyIdentifier)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PReaderIsPropertySupported( pContext, nPropertyIdentifier );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WReaderListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, const uint8_t * pConnectionPropertyArray, uint32_t nPropertyNumber, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pHandler;
   dfc1.pParameter = pHandlerParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PReaderListenToCardDetection( pContext, null, &dfc1, nPriority, pConnectionPropertyArray, nPropertyNumber, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WReaderListenToCardRemovalDetection(W_HANDLE hConnection, tWBasicGenericEventHandler * pEventHandler, void * pCallbackParameter, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pEventHandler;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PReaderListenToCardRemovalDetection( pContext, hConnection, null, &dfc2, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

bool_t WReaderPreviousApplicationMatch(W_HANDLE hConnection)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PReaderPreviousApplicationMatch( pContext, hConnection );
   PContextReleaseLock(pContext);
   return ret;
}

void WReaderSetPulsePeriod(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nPulsePeriod)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PReaderSetPulsePeriod( pContext, null, &dfc1, nPulsePeriod );
   PContextReleaseLock(pContext);
}

void WRoutingTableApply(W_HANDLE hRoutingTable, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PRoutingTableApply( pContext, hRoutingTable, null, &dfc2 );
   PContextReleaseLock(pContext);
}

W_ERROR WRoutingTableCreate(W_HANDLE * phRoutingTable)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRoutingTableCreate( pContext, phRoutingTable );
   PContextReleaseLock(pContext);
   return ret;
}

void WRoutingTableEnable(bool_t bIsEnabled, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PRoutingTableEnable( pContext, bIsEnabled, null, &dfc2 );
   PContextReleaseLock(pContext);
}

W_ERROR WRoutingTableGetEntry(W_HANDLE hRoutingTable, uint16_t nEntryIndex, tWRoutingTableEntry * pRoutingTableEntry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRoutingTableGetEntry( pContext, hRoutingTable, nEntryIndex, pRoutingTableEntry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRoutingTableGetEntryCount(W_HANDLE hRoutingTable, uint16_t * pnEntryCount)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRoutingTableGetEntryCount( pContext, hRoutingTable, pnEntryCount );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRoutingTableIsEnabled(bool_t * pbIsEnabled)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRoutingTableIsEnabled( pContext, pbIsEnabled );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRoutingTableModify(W_HANDLE hRoutingTable, uint32_t nOperation, uint16_t nEntryIndex, const tWRoutingTableEntry * pRoutingTableEntry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRoutingTableModify( pContext, hRoutingTable, nOperation, nEntryIndex, pRoutingTableEntry );
   PContextReleaseLock(pContext);
   return ret;
}

void WRoutingTableRead(tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PRoutingTableRead( pContext, null, &dfc1 );
   PContextReleaseLock(pContext);
}

bool_t WRTDIsTextRecord(W_HANDLE hRecord)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PRTDIsTextRecord( pContext, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

bool_t WRTDIsURIRecord(W_HANDLE hRecord)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PRTDIsURIRecord( pContext, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDTextAddRecord(W_HANDLE hMessage, const char16_t * pLanguage, bool_t bUseUtf8, const char16_t * pText, uint32_t nTextLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDTextAddRecord( pContext, hMessage, pLanguage, bUseUtf8, pText, nTextLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDTextCreateRecord(const char16_t * pLanguage, bool_t bUseUtf8, const char16_t * pText, uint32_t nTextLength, W_HANDLE * phRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDTextCreateRecord( pContext, pLanguage, bUseUtf8, pText, nTextLength, phRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDTextFind(W_HANDLE hMessage, const char16_t * pLanguage1, const char16_t * pLanguage2, W_HANDLE * phRecord, uint8_t * pnMatch)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDTextFind( pContext, hMessage, pLanguage1, pLanguage2, phRecord, pnMatch );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDTextGetLanguage(W_HANDLE hRecord, char16_t * pLanguageBuffer, uint32_t nBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDTextGetLanguage( pContext, hRecord, pLanguageBuffer, nBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

uint32_t WRTDTextGetLength(W_HANDLE hRecord)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PRTDTextGetLength( pContext, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDTextGetValue(W_HANDLE hRecord, char16_t * pBuffer, uint32_t nBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDTextGetValue( pContext, hRecord, pBuffer, nBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

uint8_t WRTDTextLanguageMatch(W_HANDLE hRecord, const char16_t * pLanguage1, const char16_t * pLanguage2)
{
   uint8_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint8_t)0;
   }

   PContextLock(pContext);
   ret = PRTDTextLanguageMatch( pContext, hRecord, pLanguage1, pLanguage2 );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDURIAddRecord(W_HANDLE hMessage, const char16_t * pURI)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDURIAddRecord( pContext, hMessage, pURI );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDURICreateRecord(const char16_t * pURI, W_HANDLE * phRecord)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDURICreateRecord( pContext, pURI, phRecord );
   PContextReleaseLock(pContext);
   return ret;
}

uint32_t WRTDURIGetLength(W_HANDLE hRecord)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PRTDURIGetLength( pContext, hRecord );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WRTDURIGetValue(W_HANDLE hRecord, char16_t * pBuffer, uint32_t nBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PRTDURIGetValue( pContext, hRecord, pBuffer, nBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSECheckAIDAccess(uint32_t nSlotIdentifier, const uint8_t * pAIDBuffer, uint32_t nAIDLength, const uint8_t * pImpersonationDataBuffer, uint32_t nImpersonationDataBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSECheckAIDAccess( pContext, nSlotIdentifier, pAIDBuffer, nAIDLength, pImpersonationDataBuffer, nImpersonationDataBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSecurityAuthenticate(const uint8_t * pApplicationDataBuffer, uint32_t nApplicationDataBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSecurityAuthenticate( pContext, pApplicationDataBuffer, nApplicationDataBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSEGetConnectivityEventParameter(uint32_t nSlotIdentifier, uint8_t * pDataBuffer, uint32_t nBufferLength, uint32_t * pnActualDataLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSEGetConnectivityEventParameter( pContext, nSlotIdentifier, pDataBuffer, nBufferLength, pnActualDataLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSEGetInfoEx(uint32_t nSlotIdentifier, tWSEInfoEx * pSEInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSEGetInfoEx( pContext, nSlotIdentifier, pSEInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WSEGetInfoExBuffer(uint32_t nSlotIdentifier, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSEGetInfoExBuffer( pContext, nSlotIdentifier, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

void WSEGetStatus(uint32_t nSlotIdentifier, tWSEGetStatusCompleted * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PSEGetStatus( pContext, nSlotIdentifier, null, &dfc2 );
   PContextReleaseLock(pContext);
}

uint32_t WSEGetTransactionAID(uint32_t nSlotIdentifier, uint8_t * pBuffer, uint32_t nBufferLength)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PSEGetTransactionAID( pContext, nSlotIdentifier, pBuffer, nBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSEMonitorConnectivityEvent(uint32_t nSlotIdentifier, tWBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSEMonitorConnectivityEvent( pContext, nSlotIdentifier, (tPBasicGenericEventHandler2 *)pHandler, pHandlerParameter, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSEMonitorEndOfTransaction(uint32_t nSlotIdentifier, tWBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSEMonitorEndOfTransaction( pContext, nSlotIdentifier, (tPBasicGenericEventHandler2 *)pHandler, pHandlerParameter, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

W_ERROR WSEMonitorHotPlugEvents(uint32_t nSlotIdentifier, tWBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PSEMonitorHotPlugEvents( pContext, nSlotIdentifier, (tPBasicGenericEventHandler2 *)pHandler, pHandlerParameter, phEventRegistry );
   PContextReleaseLock(pContext);
   return ret;
}

void WSEOpenConnection(uint32_t nSlotIdentifier, bool_t bForce, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc3;
   dfc3.pFunction = (tDFCCallback*)pCallback;
   dfc3.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PSEOpenConnection( pContext, nSlotIdentifier, bForce, null, &dfc3 );
   PContextReleaseLock(pContext);
}

void WSESetPolicy(uint32_t nSlotIdentifier, uint32_t nStorageType, uint32_t nProtocols, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc4;
   dfc4.pFunction = (tDFCCallback*)pCallback;
   dfc4.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PSESetPolicy( pContext, nSlotIdentifier, nStorageType, nProtocols, null, &dfc4 );
   PContextReleaseLock(pContext);
}


#ifdef P_INCLUDE_TEST_ENGINE
void * WTestAlloc(uint32_t nSize)
{
   void * ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (void *)0;
   }

   PContextLock(pContext);
   ret = PTestAlloc( pContext, nSize );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestExecuteRemoteFunction(const char * pFunctionIdentifier, uint32_t nParameter, const uint8_t * pParameterBuffer, uint32_t nParameterBufferLength, uint8_t * pResultBuffer, uint32_t nResultBufferLength, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc7;
   dfc7.pFunction = (tDFCCallback*)pCallback;
   dfc7.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestExecuteRemoteFunction( pContext, pFunctionIdentifier, nParameter, pParameterBuffer, nParameterBufferLength, pResultBuffer, nResultBufferLength, null, &dfc7 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestFree(void * pBuffer)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestFree( pContext, pBuffer );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
const void * WTestGetConstAddress(const void * pConstData)
{
   const void * ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (const void *)0;
   }

   PContextLock(pContext);
   ret = PTestGetConstAddress( pContext, pConstData );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
uint32_t WTestGetCurrentTime(void)
{
   uint32_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (uint32_t)0;
   }

   PContextLock(pContext);
   ret = PTestGetCurrentTime( pContext );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
tTestExecuteContext * WTestGetExecuteContext(void)
{
   tTestExecuteContext * ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (tTestExecuteContext *)0;
   }

   PContextLock(pContext);
   ret = PTestGetExecuteContext( pContext );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
bool_t WTestIsInAutomaticMode(void)
{
   bool_t ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return (bool_t)0;
   }

   PContextLock(pContext);
   ret = PTestIsInAutomaticMode( pContext );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestMessageBox(uint32_t nFlags, const char * pMessage, uint32_t nAutomaticResult, tWTestMessageBoxCompleted * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc4;
   dfc4.pFunction = (tDFCCallback*)pCallback;
   dfc4.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestMessageBox( pContext, nFlags, pMessage, nAutomaticResult, null, &dfc4 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestNotifyEnd(void)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestNotifyEnd( pContext );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestPresentObject(const char * pObjectName, const char * pOperatorMessage, uint32_t nDistance, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc4;
   dfc4.pFunction = (tDFCCallback*)pCallback;
   dfc4.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestPresentObject( pContext, pObjectName, pOperatorMessage, nDistance, null, &dfc4 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestRemoveObject(const char * pOperatorMessage, bool_t bSaveState, bool_t bCheckUnmodifiedState, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc4;
   dfc4.pFunction = (tDFCCallback*)pCallback;
   dfc4.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestRemoveObject( pContext, pOperatorMessage, bSaveState, bCheckUnmodifiedState, null, &dfc4 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestSetErrorResult(uint32_t nResult, const char * pMessage)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestSetErrorResult( pContext, nResult, pMessage );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestSetResult(uint32_t nResult, const void * pResultData, uint32_t nResultDataLength)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestSetResult( pContext, nResult, pResultData, nResultDataLength );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestSetTimer(uint32_t nTimeout, tWBasicGenericCompletionFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestSetTimer( pContext, nTimeout, null, &dfc2 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestTraceBuffer(const uint8_t * pBuffer, uint32_t nLength)
{
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PTestTraceBuffer( pContext, pBuffer, nLength );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestTraceError(const char * pMessage, ...)
{
#ifdef P_TRACE_ACTIVE
   va_list args;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   va_start(args, pMessage);

   PContextLock(pContext);
   PTestTraceError( pContext, pMessage, args );
   PContextReleaseLock(pContext);
   va_end(args);
#endif /* P_TRACE_ACTIVE */
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestTraceInfo(const char * pMessage, ...)
{
#ifdef P_TRACE_ACTIVE
   va_list args;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   va_start(args, pMessage);

   PContextLock(pContext);
   PTestTraceInfo( pContext, pMessage, args );
   PContextReleaseLock(pContext);
   va_end(args);
#endif /* P_TRACE_ACTIVE */
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void WTestTraceWarning(const char * pMessage, ...)
{
#ifdef P_TRACE_ACTIVE
   va_list args;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   va_start(args, pMessage);

   PContextLock(pContext);
   PTestTraceWarning( pContext, pMessage, args );
   PContextReleaseLock(pContext);
   va_end(args);
#endif /* P_TRACE_ACTIVE */
}

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */

W_ERROR WType1ChipGetConnectionInfo(W_HANDLE hConnection, tWType1ChipConnectionInfo * pConnectionInfo)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PType1ChipGetConnectionInfo( pContext, hConnection, pConnectionInfo );
   PContextReleaseLock(pContext);
   return ret;
}


#ifdef P_INCLUDE_JAVA_API
W_ERROR WType1ChipGetConnectionInfoBuffer(W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PType1ChipGetConnectionInfoBuffer( pContext, hConnection, pInfoBuffer, nInfoBufferLength );
   PContextReleaseLock(pContext);
   return ret;
}

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR WType1ChipIsWritable(W_HANDLE hConnection)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PType1ChipIsWritable( pContext, hConnection );
   PContextReleaseLock(pContext);
   return ret;
}

void WType1ChipRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PType1ChipRead( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, phOperation );
   PContextReleaseLock(pContext);
}

void WType1ChipWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockBlocks, W_HANDLE * phOperation)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PType1ChipWrite( pContext, hConnection, null, &dfc2, pBuffer, nOffset, nLength, bLockBlocks, phOperation );
   PContextReleaseLock(pContext);
}


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
void WUICCActivateSWPLine(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PUICCActivateSWPLine( pContext, null, &dfc1 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
void WUICCGetSlotInfo(tWUICCGetSlotInfoCompleted * pCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc1;
   dfc1.pFunction = (tDFCCallback*)pCallback;
   dfc1.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PUICCGetSlotInfo( pContext, null, &dfc1 );
   PContextReleaseLock(pContext);
}

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

W_ERROR WVirtualTagCreate(uint8_t nTagType, const uint8_t * pIdentifier, uint32_t nIdentifierLength, uint32_t nMaximumMessageLength, W_HANDLE * phVirtualTag)
{
   W_ERROR ret;
   tContext* pContext;

   if((pContext = g_pContext) == null)
   {
      return W_ERROR_BAD_STATE;
   }

   PContextLock(pContext);
   ret = PVirtualTagCreate( pContext, nTagType, pIdentifier, nIdentifierLength, nMaximumMessageLength, phVirtualTag );
   PContextReleaseLock(pContext);
   return ret;
}

void WVirtualTagStart(W_HANDLE hVirtualTag, tWBasicGenericCallbackFunction * pCompletionCallback, void * pCompletionCallbackParameter, tWBasicGenericEventHandler * pEventCallback, void * pEventCallbackParameter, bool_t bReadOnly)
{
   tContext* pContext;
   tDFCExternal dfc2;
   tDFCExternal dfc4;
   dfc2.pFunction = (tDFCCallback*)pCompletionCallback;
   dfc2.pParameter = pCompletionCallbackParameter;
   dfc4.pFunction = (tDFCCallback*)pEventCallback;
   dfc4.pParameter = pEventCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PVirtualTagStart( pContext, hVirtualTag, null, &dfc2, null, &dfc4, bReadOnly );
   PContextReleaseLock(pContext);
}

void WVirtualTagStop(W_HANDLE hVirtualTag, tWBasicGenericCallbackFunction * pCompletionCallback, void * pCallbackParameter)
{
   tContext* pContext;
   tDFCExternal dfc2;
   dfc2.pFunction = (tDFCCallback*)pCompletionCallback;
   dfc2.pParameter = pCallbackParameter;

   if((pContext = g_pContext) == null)
   {
      return;
   }

   PContextLock(pContext);
   PVirtualTagStop( pContext, hVirtualTag, null, &dfc2 );
   PContextReleaseLock(pContext);
}

#endif /* #if (P_BUILD_CONFIG == P_CONFIG_USER) */

/* End of file */
