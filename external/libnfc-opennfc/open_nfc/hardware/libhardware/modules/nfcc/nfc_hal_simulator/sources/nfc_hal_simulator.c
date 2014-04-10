/*
 * Copyright (c) 2007-2010 Inside Contactless, All Rights Reserved.
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

  NFC HAL Binding for the NFCCSimulator

*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SIM)

#include "nal_porting_os.h"
#include "nal_porting_hal.h"

#include "nfc_hal.h"
#include "linux_nal_porting_hal.h"

/* for the definition of constant values */
#include "open_nfc.h"

typedef struct __tNALSimulatorInstance
{
   P_NAL_SYNC_CS  hCriticalSection;

   tNALInstance * pNALInstance;

   /* the reception parameters */
   uint8_t* pReceptionBuffer;
   uint32_t nReceptionBufferLength;
   tNALBindingReadCompleted* pReadCallbackFunction;
   void* pReadCallbackParameter;

   /* the connection parameters */
   tNALBindingConnectCompleted* pConnectCallbackFunction;
   void* pConnectCallbackParameter;

   /* The write parameters */
   tNALBindingWriteCompleted* pWriteCallbackFunction;
   void* pWriteCallbackParameter;

   /* The reception counter */
   uint32_t nReceptionCounter;

   /* The connection flag */
   bool_t bIsConnected;

   tNALBindingTimerHandler* pTimerHandlerFunction;

   tNALComInstance* pComPort;
   tNALTimerInstance* pTimer;

   void* pPortingConfig;
   void* pCallbackContext;

   uint32_t nMode;

} tNALSimulatorInstance;


/* See NFC HAL Binding header file */
static void static_NALBindingReset(
         tNALVoidContext* pNALContext)
{
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;

   CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

   pInstance->pConnectCallbackFunction = null;
   pInstance->pConnectCallbackParameter = null;

   pInstance->nReceptionCounter = 0;

   pInstance->bIsConnected = W_FALSE;

   CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

/* See NFC HAL Binding header file */
static void static_NALBindingConnect(
         tNALVoidContext* pNALContext,
         uint32_t nType,
         tNALBindingConnectCompleted* pCallbackFunction,
         void* pCallbackParameter)
{
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;

   CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

	pInstance->pConnectCallbackFunction = pCallbackFunction;
	pInstance->pConnectCallbackParameter = pCallbackParameter;
	CNALResetNFCController(pInstance->pPortingConfig, nType);

	CNALSyncTriggerEventPump(pInstance->pPortingConfig);

   CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

/* See NFC HAL Binding header file */
static void static_NALBindingWrite(
         tNALVoidContext* pNALContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         tNALBindingWriteCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;

   CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

	CNALDebugAssert(pInstance->bIsConnected != W_FALSE);
	CNALDebugAssert(pInstance->pWriteCallbackFunction == null);

	CNALComWriteBytes(pInstance->pComPort,pBuffer, nLength);

	pInstance->pWriteCallbackFunction = pCallbackFunction;
	pInstance->pWriteCallbackParameter = pCallbackParameter;

	CNALSyncTriggerEventPump(pInstance->pPortingConfig);

	CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

/* See NFC HAL Binding header file */
static void static_NALBindingPoll(
         tNALVoidContext* pNALContext)
{
  tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;

  CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

   if(CNALTimerIsTimerElapsed( pInstance->pTimer ))
   {
      CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
      pInstance->pTimerHandlerFunction( pInstance->pCallbackContext );
      CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
   }

   if(pInstance->bIsConnected != W_FALSE)
   {
      int32_t nResult;

      if(pInstance->pComPort->pNFCCConnection != null)
      {
         if(pInstance->pWriteCallbackFunction != null)
         {
            tNALBindingWriteCompleted * pWriteCallback;

            pWriteCallback = pInstance->pWriteCallbackFunction;
            pInstance->pWriteCallbackFunction = null;

            CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);

            pWriteCallback(
               pInstance->pCallbackContext,
               pInstance->pWriteCallbackParameter,
               pInstance->nReceptionCounter++);

            CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
         }


			nResult = CNALComReadBytes(pInstance->pComPort, pInstance->pReceptionBuffer, pInstance->nReceptionBufferLength);

#if 0

         /* No more DATA => Restart Overlapped Reception */
         nResult = CCClientReceiveData(
                  pInstance->pComPort->pNFCCConnection,
                  pInstance->pComPort->aRXBuffer, P_MAX_RX_BUFFER_SIZE,
                  &pInstance->pComPort->pRXData);

         if( nResult < 0 )
         {
            PNALDebugError("An error occured during the read operation on the communication port");
            CNALComDestroy(pInstance->pComPort);
         }
         else if( nResult > 0 )
         {
            if((uint32_t)nResult > pInstance->nReceptionBufferLength)
            {
               PNALDebugError("Message received from the communication port is too long");
               nResult = (int32_t)pInstance->nReceptionBufferLength;
            }
            CNALMemoryCopy(pInstance->pReceptionBuffer, pInstance->pComPort->pRXData, (uint32_t)nResult);

            CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);


				if (nResult > 0)
				{
					pInstance->pReadCallbackFunction(
						pInstance->pCallbackContext,
						pInstance->pReadCallbackParameter,
						(uint32_t)nResult,
						pInstance->nReceptionCounter++);

				}
            CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
         }
 #endif

			if( nResult > 0 )
			{
				CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);

				pInstance->pReadCallbackFunction(
									pInstance->pCallbackContext,
									pInstance->pReadCallbackParameter,
									(uint32_t)nResult,
									pInstance->nReceptionCounter++);


				CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
			}
		}
   }
   else
   {
      if(pInstance->pConnectCallbackFunction != null)
      {
         pInstance->pConnectCallbackFunction(
            pInstance->pCallbackContext, pInstance->pConnectCallbackParameter, 0);

         pInstance->pConnectCallbackFunction = null;
         pInstance->pConnectCallbackParameter = null;

         pInstance->bIsConnected = W_TRUE;
      }
   }

   CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

/* See NFC HAL Binding header file */
static void static_NALBindingDestroy(
         tNALVoidContext* pNALContext)
{
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;

   if(pInstance != null)
   {

      CNALPreDestroy(pInstance->pNALInstance);

      CNALComDestroy(pInstance->pComPort);

      CNALTimerDestroy(pInstance->pTimer);

      CNALSyncDestroyCriticalSection(&pInstance->hCriticalSection);

      CNALPostDestroy(pInstance->pNALInstance);

      CNALMemoryFree(pInstance);

   }
}

/* See NFC HAL Binding header file */
static tNALVoidContext* static_NALBindingCreate(
         void* pPortingConfig,
         void* pCallbackContext,
         uint8_t* pReceptionBuffer,
         uint32_t nReceptionBufferLength,
         tNALBindingReadCompleted* pReadCallbackFunction,
         void* pCallbackParameter,
         uint32_t nAutoStandbyTimeout,
         uint32_t nStandbyTimeout,
         tNALBindingTimerHandler* pTimerHandlerFunction,
         tNALBindingAntropySourceHandler* pAntropySourceHandlerFunction)
{
   uint32_t nComType;
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)CNALMemoryAlloc(
      sizeof(tNALSimulatorInstance));

   CNALMemoryFill(pInstance, 0x00, sizeof(tNALSimulatorInstance));

   CNALSyncCreateCriticalSection(&pInstance->hCriticalSection);

   pInstance->pReceptionBuffer = pReceptionBuffer;
   pInstance->nReceptionBufferLength = nReceptionBufferLength;
   pInstance->pReadCallbackFunction = pReadCallbackFunction;
   pInstance->pReadCallbackParameter = pCallbackParameter;

   pInstance->pTimerHandlerFunction = pTimerHandlerFunction;

   pInstance->pPortingConfig = pPortingConfig;
   pInstance->pCallbackContext = pCallbackContext;

   pInstance->pConnectCallbackFunction = null;
   pInstance->pConnectCallbackParameter = null;

   pInstance->nReceptionCounter = 0;

   pInstance->bIsConnected = W_FALSE;

   if ((pInstance->pNALInstance = CNALPreCreate(pPortingConfig)) == null)
   {
      PNALDebugError("static_NALBindingCreate: CNALPreCreate failed");
      goto return_error;
   }

   if((pInstance->pComPort = CNALComCreate(pPortingConfig, &nComType)) == null)
   {
      PNALDebugError("static_NALBindingCreateFunction: Cannot create the com port");
      goto return_error;
   }

   if((pInstance->pTimer = CNALTimerCreate(pPortingConfig)) == null)
   {
      PNALDebugError("static_NALBindingCreate: Cannot create the timer");
      goto return_error;
   }

   if (CNALPostCreate(pInstance->pNALInstance, pInstance) == W_FALSE)
   {
      PNALDebugError("CNALPostCreate failed");
      goto return_error;
   }

   return (tNALVoidContext*)pInstance;

return_error:

   static_NALBindingDestroy((tNALVoidContext*)pInstance);

   return null;
}

static uint32_t static_NALBindingGetVariable(
         tNALVoidContext* pNALContext,
         uint32_t nType)
{
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;
   uint32_t nValue = 0;

   CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

   switch(nType)
   {
   case NAL_PARAM_SUB_MODE:
      nValue = W_NFCC_MODE_ACTIVE;  /* The sub mode is not simulated */
      break;
   case NAL_PARAM_MODE:
      nValue = pInstance->nMode;
      break;
   case NAL_PARAM_FIRMWARE_UPDATE:
      nValue = 0; /* The update progression is not simulated */
      break;
   case NAL_PARAM_CURRENT_TIME:
      nValue = CNALTimerGetCurrentTime(pInstance->pTimer);
      break;
   }

   CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);

   return nValue;
}

static void static_NALBindingSetVariable(
         tNALVoidContext* pNALContext,
         uint32_t nType,
         uint32_t nValue)
{
   tNALSimulatorInstance* pInstance = (tNALSimulatorInstance*)pNALContext;

   CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

   switch(nType)
   {
   case NAL_PARAM_MODE:
      pInstance->nMode = nValue;
      break;
   case NAL_PARAM_STATISTICS:
      /* Reset the protocol statistics */
      break;
   case NAL_PARAM_CURRENT_TIMER:
      if(nValue == 0)
      {
         CNALTimerCancel(pInstance->pTimer);
      }
      else
      {
         CNALTimerSet( pInstance->pTimer, nValue);
      }
      break;
   }

   CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

static void static_NALBindingGetStatistics(
         tNALVoidContext* pNALContext,
         tNALProtocolStatistics* pStatistics)
{
   CNALMemoryFill(pStatistics, 0x00, sizeof(tNALProtocolStatistics));
}

static const tNALBinding g_sSimulatorNALBinding =
{
   NAL_BINDING_MAGIC_WORD,

   static_NALBindingCreate,
   static_NALBindingDestroy,
   static_NALBindingReset,
   static_NALBindingConnect,
   static_NALBindingWrite,
   static_NALBindingPoll,
   static_NALBindingGetVariable,
   static_NALBindingSetVariable,
   static_NALBindingGetStatistics
};

const tNALBinding* const g_pNALBinding = &g_sSimulatorNALBinding;

