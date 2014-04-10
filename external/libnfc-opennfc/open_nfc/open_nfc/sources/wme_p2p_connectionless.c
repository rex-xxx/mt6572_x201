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
   Contains the implementation of the LLCP connection manager
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_LLC )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PP2PSendToCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nResult)
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) pCallbackParameter;

   PDFCPostContext2(pCallbackContext, nResult);
   CMemoryFree(pCallbackContext);
}
/** See Client API Specifications */
void PP2PSendTo(
   tContext * pContext,
   W_HANDLE hSocket,
   tPBasicGenericCallbackFunction* pCallback,
   void* pCallbackParameter,
   uint8_t nSAP,
   const uint8_t* pSendBuffer,
   uint32_t nSendBufferLength,
   W_HANDLE* phOperation)
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   tDFCCallbackContext   sCallbackContext;
   W_ERROR nError;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   if ( ((pSendBuffer == null) && (nSendBufferLength != 0)) ||
        ((pSendBuffer != null) && (nSendBufferLength == 0)) )
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (pCallbackContext == null) {

      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);
   PP2PSendToDriver(pContext, hSocket, static_PP2PSendToCompleted, pCallbackContext, nSAP, pSendBuffer, nSendBufferLength, phOperation);

   nError = PContextGetLastIoctlError(pContext);
   if (nError != W_SUCCESS)
   {
      goto return_error;
   }

   return;

return_error:

   if(pCallbackContext != null)
   {
      CMemoryFree(pCallbackContext);
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, nError);
}

/** See Client API Specifications */
W_ERROR WP2PSendToSync(
     W_HANDLE hSocket,
     uint8_t nSAP,
     const uint8_t* pSendBuffer,
     uint32_t nSendBufferLength)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WP2PSendTo(
         hSocket,
         PBasicGenericSyncCompletion, &param, nSAP,
         pSendBuffer, nSendBufferLength,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}


typedef struct _tPP2PRecvFromSyncParameters
{
   tPBasicGenericSyncParameters common;

   uint32_t nLength;
   uint8_t nSAP;

} tPP2PRecvFromSyncParameters;


static void static_PP2PRecvFromCompleted(
   tContext* pContext,
   void * pCallbackParameter,
   uint32_t nDataLength,
   W_ERROR nError,
   uint8_t nSAP )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) pCallbackParameter;

   PDFCPostContext4(pCallbackContext, nDataLength, nError, nSAP);
   CMemoryFree(pCallbackContext);
}

void PP2PRecvFrom(
   tContext* pContext,
   W_HANDLE hSocket,
   tPP2PRecvFromCompleted * pCallback,
   void * pCallbackParameter,
   uint8_t * pReceptionBuffer,
   uint32_t nReceptionBufferLength,
   W_HANDLE * phOperation )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   tDFCCallbackContext   sCallbackContext;
   W_ERROR nError;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   if ( ((pReceptionBuffer == null) && (nReceptionBufferLength != 0)) ||
        ((pReceptionBuffer != null) && (nReceptionBufferLength == 0)) )
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (pCallbackContext == null) {

      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);
   PP2PRecvFromDriver(pContext, hSocket, static_PP2PRecvFromCompleted, pCallbackContext, pReceptionBuffer, nReceptionBufferLength, phOperation);

   nError = PContextGetLastIoctlError(pContext);
   if (nError != W_SUCCESS)
   {
      goto return_error;
   }
   return;

return_error:

   if(pCallbackContext != null)
   {
      CMemoryFree(pCallbackContext);
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext4(&sCallbackContext, 0, nError, 0);

}

/**
  * WP2PRecvFrom completion callback used for WP2PRecvFromSync implementation
  */
static void static_WP2PRecvFromCompleted(
  void* pCallbackParameter,
  uint32_t nDataLength,
  W_ERROR nError,
  uint8_t nSAP )
{
   tPP2PRecvFromSyncParameters * pParams = pCallbackParameter;

   pParams->common.nResult = nError;
   pParams->nLength = nDataLength;
   pParams->nSAP = nSAP;

   CSyncSignalWaitObject(&pParams->common.hWaitObject);
}

/** See Client API Specifications */
W_ERROR WP2PRecvFromSync (
   W_HANDLE hSocket,
   uint8_t* pReceptionBuffer,
   uint32_t nReceptionBufferLength,
   uint8_t * pnSAP,
   uint32_t *pnDataLength)
{
   tPP2PRecvFromSyncParameters sParam;

   if(WBasicGenericSyncPrepare(&sParam.common) == W_FALSE)
   {
      PDebugError("WP2PRecvFromSync: WBasicGenericSyncPrepare failed");
      return sParam.common.nResult;
   }

   WP2PRecvFrom(hSocket, static_WP2PRecvFromCompleted, &sParam, pReceptionBuffer, nReceptionBufferLength, null);

   CSyncWaitForObject(&sParam.common.hWaitObject);
   CSyncDestroyWaitObject(&sParam.common.hWaitObject);

   * pnSAP = sParam.nSAP;
   * pnDataLength = sParam.nLength;
   return sParam.common.nResult;
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */


#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


static void static_PP2PSendToDriverInternalCompleted( tContext* pContext, void * pCallbackParameter, W_ERROR nError )
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) pCallbackParameter;

   PDFCDriverPostCC2(* ppDriverCC, nError);
   CMemoryFree(ppDriverCC);
}


void PP2PSendToDriver(
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      uint8_t nSAP,
      const uint8_t* pSendBuffer,
      uint32_t nSendBufferLength,
      W_HANDLE* phOperation)
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) CMemoryAlloc(sizeof(tDFCDriverCCReference));

   if (ppDriverCC != null)
   {
      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, ppDriverCC);
      PP2PSendToDriverInternal(pContext, hSocket, static_PP2PSendToDriverInternalCompleted, ppDriverCC, nSAP, pSendBuffer, nSendBufferLength, phOperation);
   }
   else
   {
      tDFCDriverCCReference pDriverCC;

      if (phOperation != null)
      {
         * phOperation = W_NULL_HANDLE;
      }

      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pDriverCC);
      PDFCDriverPostCC2(pDriverCC, W_ERROR_OUT_OF_RESOURCE);
   }
}

/**
  * completion callback called when the packet has been sent to the NFC controller
  *
  * This is used to call the user WP2PSendTo completion callback
  */

static void static_PP2PSendToDriverCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   tP2PSocket * pP2PSocket = (tP2PSocket *) pCallbackParameter;

   pP2PSocket->bXmitInProgress = W_FALSE;
   PDFCPostContext2(&pP2PSocket->sXmitCC, nError);
}


/** See WP2PSendTo */
void PP2PSendToDriverInternal(
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      uint8_t nSAP,
      const uint8_t* pSendBuffer,
      uint32_t nSendBufferLength,
      W_HANDLE* phOperation)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PSocket   * pP2PSocket;
   tDFCCallbackContext sCallbackContext;
   tDecodedPDU    sPDUDescriptor;
   tLLCPPDUHeader * pPDU;
   W_ERROR        nError;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);

   nError = PP2PGetSocketObject(pContext, hSocket, &pP2PSocket);

   if (nError != W_SUCCESS)
   {
      goto error;
   }

   if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
   {
      PDebugError("PP2PSendTo is only supported on connectionless sockets");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto error;
   }

   if (pP2PInstance->sLLCPInstance.nRole == LLCP_ROLE_NONE)
   {
      PDebugError("PP2PSendTo is only supported when the link is established");
      nError = W_ERROR_BAD_STATE;
      goto error;
   }

   if (nSAP > 63)
   {
      PDebugError("PP2PSendTo : invalid nSap value - must be in [0-63]");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   if (nSendBufferLength > pP2PInstance->sLLCPInstance.nRemoteLinkMIU)
   {
      PDebugError("PP2PSendTo : buffer too large");
      nError = W_ERROR_BUFFER_TOO_LARGE;
      goto error;
   }

   if  ( ((nSendBufferLength == 0) && (pSendBuffer != null)) ||
         ((nSendBufferLength != 0) && (pSendBuffer == null)))
   {
      PDebugError("P2PSendTo : pSendBuffer/nSendBufferLength inconsistency");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   if (pP2PSocket->bXmitInProgress != W_FALSE)
   {
      PDebugError("PP2PSendTo : a send operation is already in progress");
      nError = W_ERROR_RETRY;
      goto error;
   }

   pPDU = PLLCPAllocatePDU(pContext);

   if (pPDU == null)
   {
      PDebugError("PP2PSendTo : no more memory available");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   PLLCPResetDescriptor(&sPDUDescriptor);

   sPDUDescriptor.nType = LLCP_PDU_UI;
   sPDUDescriptor.nSSAP = pP2PSocket->sConfig.nLocalSap;
   sPDUDescriptor.nDSAP = nSAP;
   sPDUDescriptor.sPayload.sSDU.pInformation = (uint8_t *) pSendBuffer;
   sPDUDescriptor.sPayload.sSDU.nInformationLength = (uint16_t) nSendBufferLength;

   PLLCPBuildPDU(&sPDUDescriptor, &pPDU->nLength, pPDU->aPayload);

   pP2PSocket->sXmitCC = sCallbackContext;
   pP2PSocket->bXmitInProgress = W_TRUE;

   PLLCPFramerEnterXmitPacketWithAck(pContext, pPDU, static_PP2PSendToDriverCompleted, pP2PSocket);

   return;

error:
   PDFCPostContext2(&sCallbackContext, nError);
}

/*
 * P2PRecvFrom cancellation callback
 */
static void static_PP2PRecvFromCancelOperation(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tP2PSocket * pP2PSocket = (tP2PSocket *) pCancelParameter;

   if (bIsClosing == W_FALSE)
   {
      PP2PCallSocketReadCallback(pContext, pP2PSocket, 0, W_ERROR_CANCEL, 0);
   }
   else
   {
      /* The user closed the operation handle */
      pP2PSocket->hRecvOperation = W_NULL_HANDLE;
   }
}


static void static_PP2PRecvFromDriverInternalCompleted( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError, uint8_t nSAP )
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) pCallbackParameter;

   PDFCDriverPostCC4(* ppDriverCC, nDataLength, nError, nSAP);
   CMemoryFree(ppDriverCC);
}


void PP2PRecvFromDriver (
   tContext * pContext,
      W_HANDLE hSocket,
      tPP2PRecvFromCompleted* pCallback,
      void* pCallbackParameter,
      uint8_t * pReceptionBuffer,
      uint32_t nReceptionBufferLength,
      W_HANDLE* phOperation)
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) CMemoryAlloc(sizeof(tDFCDriverCCReference));

   if (ppDriverCC != null)
   {
      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, ppDriverCC);
      PP2PRecvFromDriverInternal(pContext, hSocket, static_PP2PRecvFromDriverInternalCompleted, ppDriverCC, pReceptionBuffer, nReceptionBufferLength, phOperation);
   }
   else
   {
      tDFCDriverCCReference pDriverCC;

      if (phOperation != null)
      {
         * phOperation = W_NULL_HANDLE;
      }

      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pDriverCC);
      PDFCDriverPostCC3(pDriverCC, 0, W_ERROR_OUT_OF_RESOURCE);
   }
}


/** See WP2PRecvFrom */
void PP2PRecvFromDriverInternal(
      tContext * pContext,
      W_HANDLE hSocket,
      tPP2PRecvFromCompleted* pCallback,
      void* pCallbackParameter,
      uint8_t * pReceptionBuffer,
      uint32_t nReceptionBufferLength,
      W_HANDLE* phOperation)
{
   tDFCCallbackContext sCallbackContext;
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PSocket * pP2PSocket;
   W_ERROR      nError;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);

   if (PP2PCheckP2PSupport(pContext) == W_FALSE)
   {
      nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
      goto error;
   }

   if ((nError = PP2PGetSocketObject(pContext, hSocket, &pP2PSocket)) != W_SUCCESS)
   {
      goto error;
   }

   PDebugTrace("PP2PRecvFrom : %08x %p", pP2PSocket->sConfig.hSocket, pP2PSocket);

   if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
   {
      PDebugError("PP2PRecvFrom is only supported on connectionless sockets");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto error;
   }

   if (pP2PInstance->sLLCPInstance.nRole == LLCP_ROLE_NONE)
   {
      PDebugError("PP2PRecvFrom is only supported when the link is established");
      nError = W_ERROR_BAD_STATE;
      goto error;
   }

   if  ( ((nReceptionBufferLength == 0) && (pReceptionBuffer != null)) ||
         ((nReceptionBufferLength != 0) && (pReceptionBuffer == null)))
   {
      PDebugError("PP2PRecvFrom : nReceptionBufferLength/nReceptionBufferLength inconsistency");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   if (pP2PSocket->bRecvInProgress != W_FALSE)
   {
      PDebugError("PP2PRecvFrom : a recv operation is already in progress");
      nError = W_ERROR_RETRY;
      goto error;
   }

   /* check if some data is waiting for reception */

   if (pP2PSocket->pFirstRecvPDU != null)
   {
      tLLCPPDUHeader * pPDU = pP2PSocket->pFirstRecvPDU;

      PDebugTrace("PP2PRecvFrom : process pending data");

      if( ((uint32_t)(pPDU->nLength - LLCP_UNUMBERED_PDU_DATA_OFFSET)) <= nReceptionBufferLength)
      {
         CMemoryCopy(pReceptionBuffer, pPDU->aPayload + pPDU->nDataOffset + LLCP_UNUMBERED_PDU_DATA_OFFSET, pPDU->nLength - LLCP_UNUMBERED_PDU_DATA_OFFSET);
         PDFCPostContext4(&sCallbackContext, (uint32_t) pPDU->nLength - LLCP_UNUMBERED_PDU_DATA_OFFSET, W_SUCCESS, pPDU->aPayload[1] & 0x3F);

         pP2PSocket->pFirstRecvPDU = pP2PSocket->pFirstRecvPDU->pNext;
      }
      else
      {
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto error;
      }
   }
   else
   {
      pP2PSocket->pRecvBuffer = pReceptionBuffer;
      pP2PSocket->nRecvBufferLength = nReceptionBufferLength;
      pP2PSocket->bRecvInProgress = W_TRUE;
      pP2PSocket->sRecvCC = sCallbackContext;

      if (phOperation)
      {
         pP2PSocket->hRecvOperation = * phOperation = PBasicCreateOperation(pContext, static_PP2PRecvFromCancelOperation, pP2PSocket);
      }
   }

   return;

error:
   PDFCPostContext4(&sCallbackContext, 0, nError, 0);
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */


/* EOF */
