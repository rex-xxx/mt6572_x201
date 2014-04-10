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
   Contains the implementation of the NFC HAL Service functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( NFC_HAL )

#include "wme_context.h"

#define P_DFC_TYPE_NFC_HAL_SERVICE  ((void*)(uintptr_t)26)

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
  NFC HAL Trace Functions
*******************************************************************************/

void PNALFrameLogBuffer(
         tContext* pContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         bool_t bFromNFCC);

/*******************************************************************************
   Instance States
*******************************************************************************/
#define P_NFC_HAL_STATE_DISCONNECTED       0x00
#define P_NFC_HAL_STATE_CONNECTED_PENDING  0x01
#define P_NFC_HAL_STATE_CONNECTED          0x02


/*******************************************************************************
   Operation States
*******************************************************************************/
#define P_NFC_HAL_OPERATION_STATE_UNUSED        0x00  /* Initial value, must be 0 */
#define P_NFC_HAL_OPERATION_STATE_SCHEDULED     0x01
#define P_NFC_HAL_OPERATION_STATE_READ_PENDING  0x02
#define P_NFC_HAL_OPERATION_STATE_WRITE_PENDING 0x03

/*******************************************************************************
   Static Functions
*******************************************************************************/

static void static_PNALServiceCancelOperation(
         tContext* pContext,
         tNALServiceInstance* pInstance,
         tNALServiceOperation* pOperation,
         bool_t bGracefull);

static void static_PNALServiceConnectCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nResultCode)
{
   tNALServiceInstance* pInstance = (tNALServiceInstance*)pCallbackParameter;
   W_ERROR nError;
   tDFCCallbackContext sCallbackContext;

   CDebugAssert(pInstance->nState == P_NFC_HAL_STATE_CONNECTED_PENDING);

   pInstance->nState = P_NFC_HAL_STATE_CONNECTED;

   switch(nResultCode)
   {
   case 0:
      nError = W_SUCCESS;
      break;
   case 1:
      nError = W_ERROR_DURING_FIRMWARE_BOOT;
      break;
   case 2:
   default:
      nError = W_ERROR_DURING_HARDWARE_BOOT;
      break;
   }

   /* Post Application callback */
   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pInstance->pCallbackFunction, pInstance->pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, nError);
}

static void static_PNALServiceConnectCompletedStub(
         void* pCallbackContext,
         void* pCallbackParameter,
         uint32_t nResultCode)
{
   tContext * pContext = pCallbackContext;
   tDFCCallbackContext sCallbackContext;

   PContextLock (pContext);

   PDFCFillCallbackContextType(pContext, (tDFCCallback*) static_PNALServiceConnectCompleted, pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, nResultCode);
   PContextReleaseLock(pContext);
}

#ifdef P_DEBUG_ACTIVE
static void static_PNALServiceCheckOperation(
         tContext* pContext,
         tNALServiceOperation* pOperation)
{
   CDebugAssert(pOperation->pNext != pOperation);
   CDebugAssert(pOperation->nServiceIdentifier < NAL_SERVICE_NUMBER);

   if(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED)
   {
      CDebugAssert(pOperation->pNext == null);
   }
   else
   {
      tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
      tNALServiceOperation* pOtherOperation;

      pOtherOperation = pInstance->aOperationListHeadArray[pOperation->nServiceIdentifier];

      while(pOtherOperation != null)
      {
         if(pOtherOperation == pOperation)
         {
            return;
         }
         pOtherOperation = pOtherOperation->pNext;
      }
      CDebugAssert(W_FALSE);
   }
}
#else /* #ifdef P_DEBUG_ACTIVE */
#define static_PNALServiceCheckOperation(X, Y)
#endif /* #ifdef P_DEBUG_ACTIVE */

static void static_PNALServiceResetInstance(
         tContext* pContext,
         tNALServiceInstance* pInstance )
{
   tNALVoidContext* pNALContext = pInstance->pNALContext;
   tNALBinding* pNALBinding = pInstance->pNALBinding;

   if(pContext != null)
   {
      uint32_t nServiceIdentifier;

      for(nServiceIdentifier = 0; nServiceIdentifier < NAL_SERVICE_NUMBER; nServiceIdentifier++)
      {
         tNALServiceOperation* pOperation = pInstance->aOperationListHeadArray[nServiceIdentifier];

         while(pOperation != null)
         {
            tNALServiceOperation* pNextOperation = pOperation->pNext;

            static_PNALServiceCancelOperation(pContext, pInstance, pOperation, W_FALSE);

            pOperation = pNextOperation;
         }
      }

      pNALBinding->pResetFunction(pNALContext);
   }

   CMemoryFill(pInstance, 0, sizeof(tNALServiceInstance));
   pInstance->nState = P_NFC_HAL_STATE_DISCONNECTED;
   pInstance->bWritePending = W_FALSE;

   pInstance->pNALBinding = pNALBinding;
   pInstance->pNALContext = pNALContext;
}

static void static_PNALServiceExecuteNextOperation(
         tContext* pContext)
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
   uint32_t nServiceIdentifier;

   if ((pInstance->bWritePending == W_FALSE ) && (pInstance->bInNALCallback != W_FALSE))
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContextType(pContext, (tDFCCallback*) static_PNALServiceExecuteNextOperation, null, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
      PDFCPostContext0(&sCallbackContext);   /* With PDFCPostContext0, the callback parameter is not used */
      return;
   }

   if ((pInstance->bWritePending == W_FALSE ) && (pInstance->bInNALCallback == W_FALSE))
   {
      for(nServiceIdentifier = 0; nServiceIdentifier < NAL_SERVICE_NUMBER; nServiceIdentifier++)
      {
         if(pInstance->aOperationLockedArray[nServiceIdentifier] == W_FALSE)
         {
            tNALServiceOperation* pOperation = pInstance->aOperationListHeadArray[nServiceIdentifier];

            while(pOperation != null)
            {
               if(pOperation->nState == P_NFC_HAL_OPERATION_STATE_SCHEDULED)
               {
                  pInstance->aOperationLockedArray[nServiceIdentifier] = W_TRUE;

                  /* Start the operation */
                  pOperation->pType->pStartFunction( pContext, pOperation );
                  CDebugAssert( pOperation->nState != P_NFC_HAL_OPERATION_STATE_SCHEDULED );
                  CDebugAssert( pInstance->bWritePending != W_FALSE );
                  return;
               }

               pOperation = pOperation->pNext;
            }
         }
      }
   }
}

static void static_PNALServicePushOperation(
         tContext* pContext,
         tNALServiceOperation* pOperation)
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
   tNALServiceOperation** ppOtherOperation;

   for(ppOtherOperation = &pInstance->aOperationListHeadArray[pOperation->nServiceIdentifier];
       *ppOtherOperation != null; ppOtherOperation = &(*ppOtherOperation)->pNext) {}

   pOperation->pNext = null;
   *ppOtherOperation = pOperation;

   static_PNALServiceCheckOperation(pContext, pOperation);

   static_PNALServiceExecuteNextOperation( pContext );
}

/* Remove from the operation list and set the state to completed */
static void static_PNALServiceSetOperationCompleted(
         tContext* pContext,
         tNALServiceOperation* pOperation )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
   tNALServiceOperation** ppCurrentOperation;

   static_PNALServiceCheckOperation(pContext, pOperation);

   if(pOperation->nState != P_NFC_HAL_OPERATION_STATE_UNUSED)
   {
      /* Remove the operation from the operation list */
      for(ppCurrentOperation = &pInstance->aOperationListHeadArray[pOperation->nServiceIdentifier];
         *ppCurrentOperation != pOperation;
         ppCurrentOperation = &((*ppCurrentOperation)->pNext))
      {
         CDebugAssert(*ppCurrentOperation != null);
      }
      *ppCurrentOperation = pOperation->pNext;
      CDebugAssert(pOperation != pOperation->pNext);
      if(*ppCurrentOperation != null)
      {
         CDebugAssert(*ppCurrentOperation != (*ppCurrentOperation)->pNext);
      }

      CMemoryFill(pOperation, 0, sizeof(tNALServiceOperation));
   }

   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);
}

static void static_PNALServiceUnlockOperation(
                  tContext* pContext,
                  uint8_t nServiceIdentifier)
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);
   CDebugAssert(pInstance->aOperationLockedArray[nServiceIdentifier] != W_FALSE);

   pInstance->aOperationLockedArray[nServiceIdentifier] = W_FALSE;
}

static void static_PNALServiceWriteCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   /* Check write flag */
   CDebugAssert(pInstance->bWritePending != W_FALSE);

   /* Reset write flag */
   pInstance->bWritePending = W_FALSE;

   /* Call callback */
   CDebugAssert(pInstance->pWriteCallbackFunction != null);
   pInstance->pWriteCallbackFunction(pContext, pCallbackParameter, nReceptionCounter);

   /* Process the next pending operation, if any */
   static_PNALServiceExecuteNextOperation(pContext);
}

static void static_PNALServiceWriteCompletedStub(
         void* pCallbackContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tContext * pContext = pCallbackContext;
   tNALServiceInstance* pInstance;

   PContextLock (pContext);

   pInstance = PContextGetNALServiceInstance( pContext );
   pInstance->bInNALCallback = W_TRUE;

   static_PNALServiceWriteCompleted(pContext, pCallbackParameter, nReceptionCounter);

   pInstance->bInNALCallback = W_FALSE;
   PContextReleaseLock ((tContext*) pCallbackContext);
}

static void static_PNALServiceStartWrite(
                  tContext* pContext,
                  uint8_t nServiceIdentifier,
                  uint8_t nMessageCode,
                  const uint8_t* pDataBuffer,
                  uint32_t nDataBufferLength,
                  tNALServiceStartWriteCompleted* pCallbackFunction,
                  void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);

   /* Check write flag */
   CDebugAssert(pInstance->bWritePending == W_FALSE);

   if(nDataBufferLength + 2 > NAL_MESSAGE_MAX_LENGTH)
   {
      PDebugError("static_PNALServiceStartWrite: Message too long");
      CDebugAssert(W_FALSE);
      nDataBufferLength = NAL_MESSAGE_MAX_LENGTH - 2;
   }

   pInstance->aSendBuffer[0] = nServiceIdentifier;
   pInstance->aSendBuffer[1] = nMessageCode;

   CMemoryCopy(&pInstance->aSendBuffer[2], pDataBuffer, nDataBufferLength);

   pInstance->pWriteCallbackFunction = pCallbackFunction;

   pInstance->bWritePending = W_TRUE;

#ifdef P_TRACE_ACTIVE
#ifdef P_NFC_HAL_TRACE

   PNALFrameLogBuffer(pContext, pInstance->aSendBuffer, nDataBufferLength + 2, W_FALSE);

#endif /* #ifdef P_NFC_HAL_TRACE */
#endif /* #ifdef P_TRACE_ACTIVE */

   pInstance->pNALBinding->pWriteFunction(
         pInstance->pNALContext,
         pInstance->aSendBuffer, nDataBufferLength + 2,
         static_PNALServiceWriteCompletedStub,
         pCallbackParameter );
}

static void static_PNALServiceStartWrite2(
                  tContext* pContext,
                  uint8_t nServiceIdentifier,
                  uint8_t nMessageCode,
                  uint8_t nMessageParam,
                  const uint8_t* pDataBuffer,
                  uint32_t nDataBufferLength,
                  tNALServiceStartWriteCompleted* pCallbackFunction,
                  void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);

   /* Check write flag */
   CDebugAssert(pInstance->bWritePending == W_FALSE);

   if(nDataBufferLength + 3 > NAL_MESSAGE_MAX_LENGTH)
   {
      PDebugError("static_PNALServiceStartWrite2: Message too long");
      CDebugAssert(W_FALSE);
      nDataBufferLength = NAL_MESSAGE_MAX_LENGTH - 3;
   }

   pInstance->aSendBuffer[0] = nServiceIdentifier;
   pInstance->aSendBuffer[1] = nMessageCode;
   pInstance->aSendBuffer[2] = nMessageParam;

   CMemoryCopy(&pInstance->aSendBuffer[3], pDataBuffer, nDataBufferLength);

   pInstance->pWriteCallbackFunction = pCallbackFunction;

   pInstance->bWritePending = W_TRUE;

#ifdef P_TRACE_ACTIVE
#ifdef P_NFC_HAL_TRACE

   PNALFrameLogBuffer(pContext, pInstance->aSendBuffer, nDataBufferLength + 3, W_FALSE);

#endif /* #ifdef P_NFC_HAL_TRACE */
#endif /* #ifdef P_TRACE_ACTIVE */

   pInstance->pNALBinding->pWriteFunction(
         pInstance->pNALContext,
         pInstance->aSendBuffer, nDataBufferLength + 3,
         static_PNALServiceWriteCompletedStub,
         pCallbackParameter );
}

/*******************************************************************************
   Receive Event Operation Functions
*******************************************************************************/

static bool_t static_PNALServiceRecvEventReadCompleted(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nNALMessageReceptionCounter)
{
   uint8_t nEventIdentifier;

   static_PNALServiceCheckOperation(pContext, pOperation);

   if(nLength < 2)
   {
      PDebugError("static_PNALServiceRecvEventReadCompleted: Wrong event message length");
      return W_FALSE;
   }

   nEventIdentifier = pBuffer[1];

   if((pOperation->op.s_recv_event.nEventFilter != P_NFC_HAL_EVENT_FILTER_ANY)
   && (pOperation->op.s_recv_event.nEventFilter != nEventIdentifier))
   {
      return W_FALSE;
   }

   /* Special case for the event reception: direct call
      because the data is not copied in a recption the buffer */
   pOperation->op.s_recv_event.pCallbackFunction(
         pContext,
         pOperation->op.s_recv_event.pCallbackParameter,
         nEventIdentifier,
         pBuffer + 2, nLength - 2, nNALMessageReceptionCounter);

   return W_TRUE;
}

static tNALServiceOperationType P_NAL_SERVICE_RECV_EVENT_OPERATION =
{
   null,
   null,
   static_PNALServiceRecvEventReadCompleted,
};


/*******************************************************************************
   Send Event Operation Functions
*******************************************************************************/
static void static_PNALServiceSendEventCancel(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  bool_t bGracefull )
{
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_send_event.pCallbackFunction, pOperation->op.s_send_event.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext3(&sCallbackContext, W_ERROR_CANCEL, 0);
}

static void static_PNALServiceSendEventWriteCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tNALServiceOperation* pOperation = (tNALServiceOperation*)pCallbackParameter;

   static_PNALServiceCheckOperation(pContext, pOperation);

   if ( pOperation->nState != P_NFC_HAL_OPERATION_STATE_UNUSED )
   {
      tDFCCallbackContext sCallbackContext;

      CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_WRITE_PENDING );

      /* Send the result */
      PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_send_event.pCallbackFunction, pOperation->op.s_send_event.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, W_SUCCESS, nReceptionCounter);

      /* Unlock the service for the next operation */
      static_PNALServiceUnlockOperation(pContext, pOperation->nServiceIdentifier);

      /* Remove from the operation list and set the state to completed */
      static_PNALServiceSetOperationCompleted(pContext, pOperation);
   }
}

static void static_PNALServiceSendEventStart(
                  tContext* pContext,
                  tNALServiceOperation* pOperation )
{
   static_PNALServiceCheckOperation(pContext, pOperation);

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_WRITE_PENDING;


   static_PNALServiceStartWrite(pContext, pOperation->nServiceIdentifier,
      pOperation->op.s_send_event.nEventCode,
      pOperation->op.s_send_event.pEventDataBuffer,
      pOperation->op.s_send_event.nEventDataBufferLength,
      static_PNALServiceSendEventWriteCompleted, pOperation );
}

static tNALServiceOperationType P_NAL_SERVICE_SEND_EVENT_OPERATION =
{
   static_PNALServiceSendEventStart,
   static_PNALServiceSendEventCancel,
   null,
};


/*******************************************************************************
   Execute Operation
*******************************************************************************/

static void static_PNALServiceExecuteCancel(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  bool_t bGracefull )
{
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_execute.pCallbackFunction, pOperation->op.s_execute.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext4(&sCallbackContext, 0, W_ERROR_CANCEL, 0);
}

static void static_PNALServiceExecuteWriteCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tNALServiceOperation* pOperation = (tNALServiceOperation*)pCallbackParameter;

   static_PNALServiceCheckOperation(pContext, pOperation);

   CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_WRITE_PENDING );

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_READ_PENDING;
}

static void static_PNALServiceExecuteStart(
                  tContext* pContext,
                  tNALServiceOperation* pOperation )
{
   static_PNALServiceCheckOperation(pContext, pOperation);

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_WRITE_PENDING;

   static_PNALServiceStartWrite(pContext, pOperation->nServiceIdentifier,
      pOperation->op.s_execute.nCommandCode,
      pOperation->op.s_execute.pInputBuffer,
      pOperation->op.s_execute.nInputBufferLength,
      static_PNALServiceExecuteWriteCompleted, pOperation );
}

static bool_t static_PNALServiceExecuteReadCompleted(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nNALMessageReceptionCounter)
{
   W_ERROR nError = W_SUCCESS;
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
   uint8_t nResult;
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_READ_PENDING );
   CDebugAssert( nLength >= 2);

   nResult = pBuffer[1];

   if(nResult != NAL_RES_OK)
   {
      if (nResult == NAL_RES_BAD_DATA)
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error in the command data, %d bytes lost", nLength );
         nError = W_ERROR_BAD_PARAMETER;
      }
      else if(nResult == NAL_RES_UNKNOWN_COMMAND)
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error unknown command, %d bytes lost", nLength );
         nError = W_ERROR_FUNCTION_NOT_SUPPORTED;
      }
      else if(nResult == NAL_RES_TIMEOUT)
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error timeout, %d bytes lost", nLength );
         nError = W_ERROR_TIMEOUT;
      }
      else if(nResult == NAL_RES_BAD_STATE)
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error bad state, %d bytes lost", nLength );
         nError = W_ERROR_BAD_STATE;
      }
      else if(nResult == NAL_RES_PROTOCOL_ERROR)
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error RF protocol, %d bytes lost", nLength );
         nError = W_ERROR_RF_COMMUNICATION;
      }
      else if (nResult == NAL_RES_BAD_VERSION)
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error BAD firmware version, %d bytes lost", nLength );
         nError = W_ERROR_BAD_FIRMWARE_VERSION;
      }
      else
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: Error protocol 0x%02X returned by NFC Controller, %d bytes lost", nResult, nLength );
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
      }
   }

   if(nError == W_SUCCESS)
   {
      nLength -= 2;
      if(nLength <= pOperation->op.s_execute.nOutputBufferLengthMax)
      {
         CMemoryCopy(pOperation->op.s_execute.pOutputBuffer, &pBuffer[2], nLength);
      }
      else
      {
         PDebugWarning("static_PNALServiceExecuteReadCompleted: The response buffer is too short");
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   if(nError != W_SUCCESS)
   {
      PDebugWarning("static_PNALServiceExecuteReadCompleted: return error %s",
         PUtilTraceError(nError));
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
      nLength = 0;
   }

   /* Unlock the service for the next operation */
   static_PNALServiceUnlockOperation(pContext, pOperation->nServiceIdentifier);

   /* Send the result */
   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_execute.pCallbackFunction, pOperation->op.s_execute.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext4(&sCallbackContext, nLength, nError, nNALMessageReceptionCounter);

   /* Remove from the operation list and set the state to completed */
   static_PNALServiceSetOperationCompleted(pContext, pOperation);

   return W_TRUE;
}

static tNALServiceOperationType P_NAL_SERVICE_EXECUTE_OPERATION =
{
   static_PNALServiceExecuteStart,
   static_PNALServiceExecuteCancel,
   static_PNALServiceExecuteReadCompleted,
};


/*******************************************************************************
   Get Parameter Operation
*******************************************************************************/

static void static_PNALServiceGetParameterCancel(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  bool_t bGracefull )
{
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_get_parameter.pCallbackFunction, pOperation->op.s_get_parameter.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext3(&sCallbackContext, 0, W_ERROR_CANCEL);
}

static void static_PNALServiceGetParameterWriteCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tNALServiceOperation* pOperation = (tNALServiceOperation*)pCallbackParameter;

   static_PNALServiceCheckOperation(pContext, pOperation);

   CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_WRITE_PENDING );

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_READ_PENDING;
}

static void static_PNALServiceGetParameterStart(
                  tContext* pContext,
                  tNALServiceOperation* pOperation )
{
   static_PNALServiceCheckOperation(pContext, pOperation);

   static_PNALServiceCheckOperation(pContext, pOperation);

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_WRITE_PENDING;

   static_PNALServiceStartWrite2(pContext, pOperation->nServiceIdentifier,
      NAL_CMD_GET_PARAMETER,
      pOperation->op.s_get_parameter.nParameterCode,
      null,
      0,
      static_PNALServiceGetParameterWriteCompleted, pOperation );
}

static bool_t static_PNALServiceGetParameterReadCompleted(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nNALMessageReceptionCounter)
{
   W_ERROR nError = W_SUCCESS;
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
   uint8_t nResult;
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_READ_PENDING );
   CDebugAssert( nLength >= 2);

   nResult = pBuffer[1];

   if(nResult != NAL_RES_OK)
   {
      if ( nResult == NAL_RES_UNKNOWN_PARAM )
      {
         PDebugWarning("static_PNALServiceGetParameterReadCompleted: Error unknown parameter, %d bytes lost", nLength );
         nError = W_ERROR_ITEM_NOT_FOUND;
      }
      else if(nResult == NAL_RES_UNKNOWN_COMMAND)
      {
         PDebugWarning("static_PNALServiceGetParameterReadCompleted: Error unknown command, %d bytes lost", nLength );
         nError = W_ERROR_FUNCTION_NOT_SUPPORTED;
      }
      else if(nResult == NAL_RES_TIMEOUT)
      {
         PDebugWarning("static_PNALServiceGetParameterReadCompleted: Error timeout, %d bytes lost", nLength );
         nError = W_ERROR_TIMEOUT;
      }
      else if(nResult == NAL_RES_PROTOCOL_ERROR)
      {
         PDebugWarning("static_PNALServiceGetParameterReadCompleted: Error RF protocol, %d bytes lost", nLength );
         nError = W_ERROR_RF_COMMUNICATION;
      }
      else
      {
         PDebugWarning("static_PNALServiceGetParameterReadCompleted: Error protocol 0x%02X returned by NFC Controller, %d bytes lost", nResult, nLength );
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
      }
   }

   if(nError == W_SUCCESS)
   {
      nLength -= 2;
      if(nLength <= pOperation->op.s_get_parameter.nValueBufferLengthMax)
      {
         CMemoryCopy(pOperation->op.s_get_parameter.pValueBuffer, &pBuffer[2], nLength);
      }
      else
      {
         PDebugWarning("static_PNALServiceGetParameterReadCompleted: The parameter buffer is too short");
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   if(nError != W_SUCCESS)
   {
      PDebugWarning("static_PNALServiceGetParameterReadCompleted: return error %s",
         PUtilTraceError(nError));
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
      nLength = 0;
   }

   /* Unlock the service for the next operation */
   static_PNALServiceUnlockOperation(pContext, pOperation->nServiceIdentifier);

   /* Send the result */
   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_get_parameter.pCallbackFunction, pOperation->op.s_get_parameter.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext3(&sCallbackContext, nLength, nError);

   /* Remove from the operation list and set the state to completed */
   static_PNALServiceSetOperationCompleted(pContext, pOperation);

   return W_TRUE;
}

static tNALServiceOperationType P_NAL_SERVICE_GET_PARAMETER_OPERATION =
{
   static_PNALServiceGetParameterStart,
   static_PNALServiceGetParameterCancel,
   static_PNALServiceGetParameterReadCompleted,
};


/*******************************************************************************
   Set Parameter Operation
*******************************************************************************/

static void static_PNALServiceSetParameterCancel(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  bool_t bGracefull )
{
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_set_parameter.pCallbackFunction, pOperation->op.s_set_parameter.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, W_ERROR_CANCEL);
}

static void static_PNALServiceSetParameterWriteCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tNALServiceOperation* pOperation = (tNALServiceOperation*)pCallbackParameter;
   static_PNALServiceCheckOperation(pContext, pOperation);

   CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_WRITE_PENDING );

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_READ_PENDING;
}

static void static_PNALServiceSetParameterStart(
                  tContext* pContext,
                  tNALServiceOperation* pOperation )
{
   static_PNALServiceCheckOperation(pContext, pOperation);

   pOperation->nState = P_NFC_HAL_OPERATION_STATE_WRITE_PENDING;

   static_PNALServiceStartWrite2(pContext, pOperation->nServiceIdentifier,
      NAL_CMD_SET_PARAMETER,
      pOperation->op.s_set_parameter.nParameterCode,
      pOperation->op.s_set_parameter.pValueBuffer,
      pOperation->op.s_set_parameter.nValueBufferLength,
      static_PNALServiceSetParameterWriteCompleted, pOperation );
}

static bool_t static_PNALServiceSetParameterReadCompleted(
                  tContext* pContext,
                  tNALServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nNALMessageReceptionCounter)
{
   W_ERROR nError = W_SUCCESS;
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );
   uint8_t nResult;
   tDFCCallbackContext sCallbackContext;

   static_PNALServiceCheckOperation(pContext, pOperation);

   CDebugAssert( pOperation->nState == P_NFC_HAL_OPERATION_STATE_READ_PENDING );
   CDebugAssert( nLength == 2);

   nResult = pBuffer[1];

   if(nResult != NAL_RES_OK)
   {
      if ( nResult == NAL_RES_UNKNOWN_PARAM )
      {
         PDebugWarning("static_PNALServiceSetParameterReadCompleted: Error unknown parameter, %d bytes lost", nLength );
         nError = W_ERROR_ITEM_NOT_FOUND;
      }
      else if(nResult == NAL_RES_UNKNOWN_COMMAND)
      {
         PDebugWarning("static_PNALServiceSetParameterReadCompleted: Error unknown command, %d bytes lost", nLength );
         nError = W_ERROR_FUNCTION_NOT_SUPPORTED;
      }
      else if(nResult == NAL_RES_TIMEOUT)
      {
         PDebugWarning("static_PNALServiceSetParameterReadCompleted: Error timeout, %d bytes lost", nLength );
         nError = W_ERROR_TIMEOUT;
      }
      else if(nResult == NAL_RES_PROTOCOL_ERROR)
      {
         PDebugWarning("static_PNALServiceSetParameterReadCompleted: Error RF protocol, %d bytes lost", nLength );
         nError = W_ERROR_RF_COMMUNICATION;
      }
      else if(nResult == NAL_RES_FEATURE_NOT_SUPPORTED)
      {
         PDebugWarning("static_PNALServiceSetParameterReadCompleted: Feature not supported" );
         nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      }
      else
      {
         PDebugWarning("static_PNALServiceSetParameterReadCompleted: Error protocol 0x%02X returned by NFC Controller, %d bytes lost", nResult, nLength );
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
      }
   }

   if(nError != W_SUCCESS)
   {
      PDebugWarning("static_PNALServiceSetParameterReadCompleted: return error %s",
         PUtilTraceError(nError));
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
   }

   /* Unlock the service for the next operation */
   static_PNALServiceUnlockOperation(pContext, pOperation->nServiceIdentifier);

   /* Send the result */
   PDFCFillCallbackContextType(pContext, (tDFCCallback*) pOperation->op.s_set_parameter.pCallbackFunction, pOperation->op.s_set_parameter.pCallbackParameter, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, nError);

   /* Remove from the operation list and set the state to completed */
   static_PNALServiceSetOperationCompleted(pContext, pOperation);

   return W_TRUE;
}

static tNALServiceOperationType P_NAL_SERVICE_SET_PARAMETER_OPERATION =
{
   static_PNALServiceSetParameterStart,
   static_PNALServiceSetParameterCancel,
   static_PNALServiceSetParameterReadCompleted,
};

/*******************************************************************************
   Static Functions
*******************************************************************************/

static void static_PNALServiceCancelOperation(
         tContext* pContext,
         tNALServiceInstance* pInstance,
         tNALServiceOperation* pOperation,
         bool_t bGracefull)
{
   tNALServiceOperation** ppCurrentOperation;

   static_PNALServiceCheckOperation(pContext, pOperation);

   if(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED)
   {
      return;
   }

   if(bGracefull != W_FALSE)
   {
      if(pOperation->nState != P_NFC_HAL_OPERATION_STATE_SCHEDULED)
      {
         /* Special case for the receive event operation */
         if((pOperation->nState != P_NFC_HAL_OPERATION_STATE_READ_PENDING)
         || (pOperation->pType != &P_NAL_SERVICE_RECV_EVENT_OPERATION))
         {
            return;
         }
      }
   }

   if(pOperation->pType->pCancelFunction != null)
   {
      pOperation->pType->pCancelFunction(pContext, pOperation, bGracefull);
   }

   /* Remove the operation from the operation list */
   for(ppCurrentOperation = &pInstance->aOperationListHeadArray[pOperation->nServiceIdentifier];
      *ppCurrentOperation != pOperation;
      ppCurrentOperation = &((*ppCurrentOperation)->pNext))
   {
      CDebugAssert(*ppCurrentOperation != null);
   }
   *ppCurrentOperation = pOperation->pNext;
   CDebugAssert(pOperation != pOperation->pNext);
   if(*ppCurrentOperation != null)
   {
      CDebugAssert(*ppCurrentOperation != (*ppCurrentOperation)->pNext);
   }
   pOperation->pNext = null;

   CMemoryFill(pOperation, 0, sizeof(tNALServiceOperation));
   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);
}

static void static_PNALServiceReadCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         uint32_t nReceptionCounter)
{
   tNALServiceInstance* pInstance = (tNALServiceInstance*)pCallbackParameter;
   uint8_t nServiceIdentifier;
   uint8_t nMessageType;
   tNALServiceOperation* pOperation;

#ifdef P_TRACE_ACTIVE
#ifdef P_NFC_HAL_TRACE

   PNALFrameLogBuffer(pContext, pInstance->aReceptionBuffer, nLength, W_TRUE);

#endif /* #ifdef P_NFC_HAL_TRACE */
#endif /* #ifdef P_TRACE_ACTIVE */

   /* Dispatch the message */
   if(nLength < 2)
   {
      PDebugError("static_PNALServiceReadCompleted: wrong message length");
      goto return_message_lost;
   }
   nServiceIdentifier = pInstance->aReceptionBuffer[0];
   if(nServiceIdentifier >= NAL_SERVICE_NUMBER)
   {
      PDebugError("static_PNALServiceReadCompleted: wrong service number");
      goto return_message_lost;
   }

   pOperation = pInstance->aOperationListHeadArray[nServiceIdentifier];

   nMessageType = pInstance->aReceptionBuffer[1] & 0xC0;

   while(pOperation != null)
   {
      if(pOperation->nState == P_NFC_HAL_OPERATION_STATE_READ_PENDING)
      {
         switch(nMessageType)
         {
            case NAL_MESSAGE_TYPE_EVENT:
               if(pOperation->pType == &P_NAL_SERVICE_RECV_EVENT_OPERATION)
               {
                  if(pOperation->pType->pReadFunction(pContext, pOperation,
                     pInstance->aReceptionBuffer, nLength,
                     nReceptionCounter) != W_FALSE)
                  {
                     /* Process the next pending operation, if any */
                     static_PNALServiceExecuteNextOperation(pContext);
                     return;
                  }
               }
               break;

            case NAL_MESSAGE_TYPE_ANSWER:
               if((pOperation->pType == &P_NAL_SERVICE_EXECUTE_OPERATION)
               || (pOperation->pType == &P_NAL_SERVICE_GET_PARAMETER_OPERATION)
               || (pOperation->pType == &P_NAL_SERVICE_SET_PARAMETER_OPERATION))
               {
                  if(pOperation->pType->pReadFunction(pContext, pOperation,
                        pInstance->aReceptionBuffer, nLength,
                        nReceptionCounter) != W_FALSE)
                  {
                     /* Process the next pending operation, if any */
                     static_PNALServiceExecuteNextOperation(pContext);
                     return;
                  }
               }
               break;

            case NAL_MESSAGE_TYPE_COMMAND:
            default:
               PDebugError("static_PNALServiceReadCompleted: wrong message type");
               goto return_message_lost;
         }
      }

      pOperation = pOperation->pNext;
   }

   return;

return_message_lost:

   PDebugWarning("static_PNALServiceReadCompleted: message lost");
   pInstance->nReadMessageLostCount++;
   pInstance->nReadBytesLost += nLength;
}

static void static_PNALServiceTimerHandler(
         tContext* pContext,
         void* pCallbackParameter)
{
   PMultiTimerPoll(pContext, W_TRUE);
}

static void static_PNALServiceTimerHandlerStub(
         void* pCallbackContext)
{
   tContext * pContext = (tContext *) pCallbackContext;
   tDFCCallbackContext sCallbackContext;

   PContextLock(pContext);

   PDFCFillCallbackContextType(pContext, (tDFCCallback*) static_PNALServiceTimerHandler, null, P_DFC_TYPE_NFC_HAL_SERVICE, &sCallbackContext);
   PDFCPostContext1(&sCallbackContext);

   PContextReleaseLock(pContext);
}

static void static_PNALServiceAntropySourceHandlerStub(
         void* pCallbackContext,
         uint32_t nValue)
{
   tContext * pContext = (tContext *) pCallbackContext;

   PContextLock(pContext);
   PContextDriverGenerateEntropy(pContext, nValue);
   PContextReleaseLock(pContext);
}

static void static_PNALServiceReadCompletedStub(
         void* pCallbackContext,
         void* pCallbackParameter,
         uint32_t nLength,
         uint32_t nReceptionCounter)
{

   tContext * pContext = pCallbackContext;
   tNALServiceInstance* pInstance;

   PContextLock (pContext);
   pInstance = PContextGetNALServiceInstance( pContext );

   pInstance->bInNALCallback = W_TRUE;
   static_PNALServiceReadCompleted(pContext, pCallbackParameter, nLength, nReceptionCounter);

   CMemoryFill(pInstance->aReceptionBuffer, 0xCA, NAL_MESSAGE_MAX_LENGTH);
   pInstance->bInNALCallback = W_FALSE;

   PContextReleaseLock(pContext);
}

/*******************************************************************************
   Functions
*******************************************************************************/

/* See header file */
W_ERROR PNALServiceCreate(
         tNALServiceInstance* pInstance,
         tNALBinding* pNALBinding,
         void* pPortingConfig,
         tContext* pContext )
{
   W_ERROR nError;
   CMemoryFill(pInstance, 0, sizeof(tNALServiceInstance));

   pInstance->pNALBinding = pNALBinding;
   pInstance->pNALContext = pNALBinding->pCreateFunction(
      pPortingConfig,
      pContext,
      pInstance->aReceptionBuffer, NAL_MESSAGE_MAX_LENGTH,
      static_PNALServiceReadCompletedStub, pInstance,
      P_CHIP_AUTO_STANDBY_TIMEOUT, P_CHIP_STANDBY_TIMEOUT,
      static_PNALServiceTimerHandlerStub,
      static_PNALServiceAntropySourceHandlerStub);

   if(pInstance->pNALContext == null)
   {
      PDebugError("PNALServiceCreate: Error returned by the initialization of the NFC HAL");
      nError = W_ERROR_DRIVER;
   }
   else
   {
      static_PNALServiceResetInstance(null, pInstance);
      nError = W_SUCCESS;
   }

   return nError;
}

/* See header file */
void PNALServiceDestroy(
         tNALServiceInstance* pInstance )
{
   static_PNALServiceResetInstance(null, pInstance);

   pInstance->pNALBinding->pDestroyFunction(pInstance->pNALContext);

   CMemoryFill(pInstance, 0, sizeof(tNALServiceInstance));
}

/* See header file */
void PNALServicePreReset(
         tContext* pContext )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   /* Reset the instance context */
   static_PNALServiceResetInstance(pContext, pInstance);

   /* Flush DFC Queue */
   PDFCFlush( pContext, P_DFC_TYPE_NFC_HAL_SERVICE );
}

/* See header file */
void PNALServiceConnect(
         tContext* pContext,
         uint32_t nType,
         tPNALServiceConnectCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   static_PNALServiceResetInstance(pContext, pInstance);

   pInstance->nState = P_NFC_HAL_STATE_CONNECTED_PENDING;

   pInstance->pCallbackFunction  = pCallbackFunction;
   pInstance->pCallbackParameter = pCallbackParameter;

   pInstance->pNALBinding->pConnectFunction(
      pInstance->pNALContext,
      nType,
      &static_PNALServiceConnectCompletedStub, pInstance );
}

/* See header file */
void PNALServiceExecuteCommand(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nCommandCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         uint8_t* pOutputBuffer,
         uint32_t nOutputBufferLength,
         tPNALServiceExecuteCommandCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   CDebugAssert(((pInputBuffer != null) || (nInputBufferLength == 0))
   && ((pInputBuffer == null) || (nInputBufferLength != 0)));
   CDebugAssert(((pOutputBuffer != null) || (nOutputBufferLength == 0))
   && ((pOutputBuffer == null) || (nOutputBufferLength != 0)));

   /* Check NFC HAL state is not connected */
   if(pInstance->nState != P_NFC_HAL_STATE_CONNECTED)
   {
      PDebugError("PNALServiceExecuteCommand: The instance is not open");
      return;
   }

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);
   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);

   pOperation->pNext = null;
   pOperation->pType = &P_NAL_SERVICE_EXECUTE_OPERATION;
   pOperation->nServiceIdentifier = nServiceIdentifier;
   pOperation->nState = P_NFC_HAL_OPERATION_STATE_SCHEDULED;

   pOperation->op.s_execute.pCallbackFunction = pCallbackFunction;
   pOperation->op.s_execute.pCallbackParameter = pCallbackParameter;
   pOperation->op.s_execute.nCommandCode = nCommandCode;
   pOperation->op.s_execute.pInputBuffer = pInputBuffer;
   pOperation->op.s_execute.nInputBufferLength = nInputBufferLength;
   pOperation->op.s_execute.pOutputBuffer = pOutputBuffer;
   pOperation->op.s_execute.nOutputBufferLengthMax = nOutputBufferLength;

   static_PNALServicePushOperation( pContext, pOperation );
}

/* See header file */
void PNALServiceGetParameter(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nParameterCode,
         uint8_t* pValueBuffer,
         uint32_t nValueBufferLengthMax,
         tPNALServiceGetParameterCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   CDebugAssert((pValueBuffer != null) && (nValueBufferLengthMax != 0));

   /* Check NFC HAL state is not connected */
   if(pInstance->nState != P_NFC_HAL_STATE_CONNECTED)
   {
      PDebugError("PNALServiceGetParameter: The instance is not open");
      return;
   }

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);
   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);

   pOperation->pNext = null;
   pOperation->pType = &P_NAL_SERVICE_GET_PARAMETER_OPERATION;
   pOperation->nServiceIdentifier = nServiceIdentifier;
   pOperation->nState = P_NFC_HAL_OPERATION_STATE_SCHEDULED;

   pOperation->op.s_get_parameter.pCallbackFunction = pCallbackFunction;
   pOperation->op.s_get_parameter.pCallbackParameter = pCallbackParameter;
   pOperation->op.s_get_parameter.nParameterCode = nParameterCode;
   pOperation->op.s_get_parameter.pValueBuffer = pValueBuffer;
   pOperation->op.s_get_parameter.nValueBufferLengthMax = nValueBufferLengthMax;

   static_PNALServicePushOperation( pContext, pOperation );
}

/* See header file */
void PNALServiceSetParameter(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nParameterCode,
         const uint8_t* pValueBuffer,
         uint32_t nValueBufferLength,
         tPNALServiceSetParameterCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   CDebugAssert(((pValueBuffer != null) || (nValueBufferLength == 0))
   && ((pValueBuffer == null) || (nValueBufferLength != 0)));

   /* Check NFC HAL state is not connected */
   if(pInstance->nState != P_NFC_HAL_STATE_CONNECTED)
   {
      PDebugError("PNALServiceSetParameter: The instance is not open");
      return;
   }

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);
   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);

   pOperation->pNext = null;
   pOperation->pType = &P_NAL_SERVICE_SET_PARAMETER_OPERATION;
   pOperation->nServiceIdentifier = nServiceIdentifier;
   pOperation->nState = P_NFC_HAL_OPERATION_STATE_SCHEDULED;

   pOperation->op.s_set_parameter.pCallbackFunction = pCallbackFunction;
   pOperation->op.s_set_parameter.pCallbackParameter = pCallbackParameter;
   pOperation->op.s_set_parameter.nParameterCode = nParameterCode;
   pOperation->op.s_set_parameter.pValueBuffer = pValueBuffer;
   pOperation->op.s_set_parameter.nValueBufferLength = nValueBufferLength;

   static_PNALServicePushOperation( pContext, pOperation );
}

/* See header file */
void PNALServiceRegisterForEvent(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         uint8_t nEventFilter,
         tNALServiceOperation* pOperation,
         tPNALServiceEventReceived* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   /* Check NFC HAL state is not connected */
   if(pInstance->nState != P_NFC_HAL_STATE_CONNECTED)
   {
      PDebugError("PNALServiceRegisterForEvent: The instance is not open");
      return;
   }

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);
   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);

   pOperation->pNext = null;
   pOperation->pType = &P_NAL_SERVICE_RECV_EVENT_OPERATION;
   pOperation->nServiceIdentifier = nServiceIdentifier;
   pOperation->nState = P_NFC_HAL_OPERATION_STATE_READ_PENDING;

   pOperation->op.s_recv_event.pCallbackFunction = pCallbackFunction;
   pOperation->op.s_recv_event.pCallbackParameter = pCallbackParameter;
   pOperation->op.s_recv_event.nEventFilter = nEventFilter;

   static_PNALServicePushOperation(pContext, pOperation);
}

/* See header file */
void PNALServiceCancelOperation(
         tContext* pContext,
         tNALServiceOperation* pOperation)
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   /* Check NFC HAL state is not connected */
   if(pInstance->nState != P_NFC_HAL_STATE_CONNECTED)
   {
      PDebugError("PNALServiceCancelOperation: The instance is not open");
      return;
   }

   static_PNALServiceCancelOperation(pContext, pInstance, pOperation, W_TRUE);
}

/* See header file */
void PNALServiceSendEvent(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nEventCode,
         const uint8_t* pEventDataBuffer,
         uint32_t nEventDataBufferLength,
         tPNALServiceSendEventCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   /* Check NFC HAL state is not connected */
   if(pInstance->nState != P_NFC_HAL_STATE_CONNECTED)
   {
      PDebugError("PNALServiceSendEvent: The instance is not open");
      return;
   }

   CDebugAssert(nServiceIdentifier < NAL_SERVICE_NUMBER);
   CDebugAssert(pOperation->nState == P_NFC_HAL_OPERATION_STATE_UNUSED);

   pOperation->pNext = null;
   pOperation->pType = &P_NAL_SERVICE_SEND_EVENT_OPERATION;
   pOperation->nServiceIdentifier = nServiceIdentifier;
   pOperation->nState = P_NFC_HAL_OPERATION_STATE_SCHEDULED;

   pOperation->op.s_send_event.pCallbackFunction = pCallbackFunction;
   pOperation->op.s_send_event.pCallbackParameter = pCallbackParameter;
   pOperation->op.s_send_event.nEventCode = nEventCode;
   pOperation->op.s_send_event.pEventDataBuffer = pEventDataBuffer;
   pOperation->op.s_send_event.nEventDataBufferLength = nEventDataBufferLength;

   static_PNALServicePushOperation(pContext, pOperation);
}

/* See header file */
void PNALServiceGetStatistics(
         tContext* pContext,
         uint32_t* pnNALReadMessageLostCount,
         uint32_t* pnNALReadBytesLost )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   *pnNALReadBytesLost        = pInstance->nReadBytesLost;
   *pnNALReadMessageLostCount = pInstance->nReadMessageLostCount;
}

/* See header file */
void PNALServiceResetStatistics(
         tContext* pContext)
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   pInstance->nReadBytesLost    = 0;
   pInstance->nReadMessageLostCount = 0;
}

/* See header file */
uint32_t PNALServiceGetVariable(
         tContext* pContext,
         uint32_t nType )
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   return pInstance->pNALBinding->pGetVariableFunction(
      pInstance->pNALContext, nType);
}

/* See header file */
void PNALServiceSetVariable(
         tContext* pContext,
         uint32_t nType,
         uint32_t nValue)
{
   tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

   pInstance->pNALBinding->pSetVariableFunction(
      pInstance->pNALContext, nType, nValue);
}

/* -----------------------------------------------------------------------------
      Time Function
----------------------------------------------------------------------------- */

uint32_t PNALServiceDriverGetCurrentTime(
            tContext* pContext)
{
   return PNALServiceGetVariable(pContext, NAL_PARAM_CURRENT_TIME);
}

/* -----------------------------------------------------------------------------
      Protocol Statistics
----------------------------------------------------------------------------- */

void PNALServiceDriverResetProtocolStatistics(
            tContext* pContext)
{
   PNALServiceSetVariable(pContext, NAL_PARAM_STATISTICS, 0);
}

void PNALServiceDriverGetProtocolStatistics(
            tContext* pContext,
            tNALProtocolStatistics* pStatistics,
            uint32_t nSize )
{
   if((pStatistics != null) && (nSize == sizeof(tNALProtocolStatistics)))
   {
      tNALServiceInstance* pInstance = PContextGetNALServiceInstance( pContext );

      pInstance->pNALBinding->pGetStatisticsFunction(
         pInstance->pNALContext, pStatistics);
   }
}

#define P_NAL_PROTOCOL_CARD_ALL ( \
      NAL_PROTOCOL_CARD_ISO_14443_4_A | \
      NAL_PROTOCOL_CARD_ISO_14443_4_B | \
      NAL_PROTOCOL_CARD_ISO_14443_3_A | \
      NAL_PROTOCOL_CARD_ISO_14443_3_B | \
      NAL_PROTOCOL_CARD_ISO_15693_3 | \
      NAL_PROTOCOL_CARD_ISO_15693_2 | \
      NAL_PROTOCOL_CARD_FELICA | \
      NAL_PROTOCOL_CARD_P2P_TARGET | \
      NAL_PROTOCOL_CARD_TYPE_1_CHIP | \
      NAL_PROTOCOL_CARD_MIFARE_CLASSIC | \
      NAL_PROTOCOL_CARD_BPRIME |\
      NAL_PROTOCOL_CARD_KOVIO |\
      NAL_PROTOCOL_CARD_MIFARE_PLUS)

/* Define a mask with all the reader protocols */
#define P_NAL_PROTOCOL_READER_ALL ( \
NAL_PROTOCOL_CARD_ISO_14443_4_A | \
      NAL_PROTOCOL_READER_ISO_14443_4_B | \
      NAL_PROTOCOL_READER_ISO_14443_3_A | \
      NAL_PROTOCOL_READER_ISO_14443_3_B | \
      NAL_PROTOCOL_READER_ISO_15693_3 | \
      NAL_PROTOCOL_READER_ISO_15693_2 | \
      NAL_PROTOCOL_READER_FELICA | \
      NAL_PROTOCOL_READER_P2P_INITIATOR | \
      NAL_PROTOCOL_READER_TYPE_1_CHIP | \
      NAL_PROTOCOL_READER_MIFARE_CLASSIC | \
      NAL_PROTOCOL_READER_BPRIME |\
      NAL_PROTOCOL_READER_KOVIO |\
      NAL_PROTOCOL_READER_MIFARE_PLUS)

#define PNALCONV(X) \
   if((nProtocols & W_NFCC_PROTOCOL_##X) != 0) { \
      nNALProtocols |= NAL_PROTOCOL_##X; }

/* See header file */
void PNALWriteCardProtocols(
            uint32_t nProtocols,
            uint8_t* pBuffer)
{
   uint16_t nNALProtocols = 0;

   CDebugAssert((nProtocols | W_NFCC_PROTOCOL_CARD_ALL | W_NFCC_PROTOCOL_READER_ALL)
      == (W_NFCC_PROTOCOL_CARD_ALL | W_NFCC_PROTOCOL_READER_ALL));

   PNALCONV(CARD_ISO_14443_4_A)
   PNALCONV(CARD_ISO_14443_4_B)
   PNALCONV(CARD_ISO_14443_3_A)
   PNALCONV(CARD_ISO_14443_3_B)
   PNALCONV(CARD_ISO_15693_3)
   PNALCONV(CARD_ISO_15693_2)
   PNALCONV(CARD_FELICA)
   PNALCONV(CARD_P2P_TARGET)
   PNALCONV(CARD_TYPE_1_CHIP)
   PNALCONV(CARD_MIFARE_CLASSIC)
   PNALCONV(CARD_BPRIME)
   PNALCONV(CARD_KOVIO)
   PNALCONV(CARD_MIFARE_PLUS)

   PNALWriteUint16ToBuffer(nNALProtocols, pBuffer);
}

/* See header file */
void PNALWriteReaderProtocols(
            uint32_t nProtocols,
            uint8_t* pBuffer)
{
   uint16_t nNALProtocols = 0;

   CDebugAssert((nProtocols | W_NFCC_PROTOCOL_CARD_ALL | W_NFCC_PROTOCOL_READER_ALL)
      == (W_NFCC_PROTOCOL_CARD_ALL | W_NFCC_PROTOCOL_READER_ALL));

   PNALCONV(READER_ISO_14443_4_A)
   PNALCONV(READER_ISO_14443_4_B)
   PNALCONV(READER_ISO_14443_3_A)
   PNALCONV(READER_ISO_14443_3_B)
   PNALCONV(READER_ISO_15693_3)
   PNALCONV(READER_ISO_15693_2)
   PNALCONV(READER_FELICA)
   PNALCONV(READER_P2P_INITIATOR)
   PNALCONV(READER_TYPE_1_CHIP)
   PNALCONV(READER_MIFARE_CLASSIC)
   PNALCONV(READER_BPRIME)
   PNALCONV(READER_KOVIO)
   PNALCONV(READER_MIFARE_PLUS)

   PNALWriteUint16ToBuffer(nNALProtocols, pBuffer);
}

#undef PNALCONV

#define PNALCONV(X) \
   if((nNALProtocols & NAL_PROTOCOL_##X) != 0) { \
      nWProtocols |= W_NFCC_PROTOCOL_##X; }

/* See header file */
uint32_t PNALReadCardProtocols(
            const uint8_t* pBuffer)
{
   uint16_t nNALProtocols = PNALReadUint16FromBuffer(pBuffer);
   uint32_t nWProtocols = 0;

   CDebugAssert((nNALProtocols | P_NAL_PROTOCOL_CARD_ALL) == P_NAL_PROTOCOL_CARD_ALL);

   PNALCONV(CARD_ISO_14443_4_A)
   PNALCONV(CARD_ISO_14443_4_B)
   PNALCONV(CARD_ISO_14443_3_A)
   PNALCONV(CARD_ISO_14443_3_B)
   PNALCONV(CARD_ISO_15693_3)
   PNALCONV(CARD_ISO_15693_2)
   PNALCONV(CARD_FELICA)
   PNALCONV(CARD_P2P_TARGET)
   PNALCONV(CARD_TYPE_1_CHIP)
   PNALCONV(CARD_MIFARE_CLASSIC)
   PNALCONV(CARD_BPRIME)
   PNALCONV(CARD_KOVIO)
   PNALCONV(CARD_MIFARE_PLUS)

   return nWProtocols;
}

/* See header file */
uint32_t PNALReadReaderProtocols(
            const uint8_t* pBuffer)
{
   uint16_t nNALProtocols = PNALReadUint16FromBuffer(pBuffer);
   uint32_t nWProtocols = 0;

   CDebugAssert((nNALProtocols | P_NAL_PROTOCOL_READER_ALL) == P_NAL_PROTOCOL_READER_ALL);

   PNALCONV(READER_ISO_14443_4_A)
   PNALCONV(READER_ISO_14443_4_B)
   PNALCONV(READER_ISO_14443_3_A)
   PNALCONV(READER_ISO_14443_3_B)
   PNALCONV(READER_ISO_15693_3)
   PNALCONV(READER_ISO_15693_2)
   PNALCONV(READER_FELICA)
   PNALCONV(READER_P2P_INITIATOR)
   PNALCONV(READER_TYPE_1_CHIP)
   PNALCONV(READER_MIFARE_CLASSIC)
   PNALCONV(READER_BPRIME)
   PNALCONV(READER_KOVIO)
   PNALCONV(READER_MIFARE_PLUS)

   return nWProtocols;
}

#undef PNALCONV

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
