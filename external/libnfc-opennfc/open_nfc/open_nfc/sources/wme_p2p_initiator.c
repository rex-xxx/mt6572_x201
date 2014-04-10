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
   Contains the implementation of the P2P initiator functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


void static_PP2PInitiatorDataReceived(tContext* pContext, void* pCallbackParameter, const uint8_t* pBuffer, uint32_t nLength);

typedef struct __tP2PInitiatorDriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   tDFCCallbackContext        sCallbackContext;

   /*  response buffer */
   uint8_t*                   pCardToReaderBuffer;

   /*  response buffer maximum length */
   uint32_t                   nCardToReaderBufferMaxLength;

   /* Buffer for the payload of a NFC HAL command (LLCP) */
   uint8_t                    aReaderToCardBufferNAL[ LLCP_NUMBERED_PDU_DATA_OFFSET +  LLCP_DEFAULT_MIU + LLCP_MAX_LOCAL_MIUX];

   /* Service Context */
   uint8_t                    nServiceIdentifier;

   /* Service Operation */
   tNALServiceOperation       sServiceOperation;


} tP2PInitiatorDriverConnection;


/* Function prototypes */
static uint32_t static_PP2PDriverDestroyConnection(tContext* pContext, void* pObject );
static void static_PP2PInitiatorLocalTimeoutExpiry(tContext * pContext, void * pCallbackParameter);


tHandleType g_sP2PInitiatorDriverConnection = { static_PP2PDriverDestroyConnection, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_P2P_INITIATORDRIVER_CONNECTION (&g_sP2PInitiatorDriverConnection)


typedef void tP2PInitiatorDriverSendDataCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);



/**
 * Creates the Initiator P2P connection object.
 *
 * @return  W_SUCCESS if the connection was successfully created, an error otherwise.
 **/

static W_ERROR static_PP2PInitiatorDriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   tP2PInitiatorDriverConnection * pConnection;
   W_HANDLE                        hConnection;
   W_ERROR                         nError;

   PDebugTrace("static_PP2PInitiatorCreateConnection");

   if (phDriverConnection == null)
   {
      return (W_ERROR_BAD_PARAMETER);
   }

   pConnection = CMemoryAlloc(sizeof(tP2PInitiatorDriverConnection));

   if (pConnection != null)
   {

      /* initialises the connection */

      CMemoryFill(pConnection, 0, sizeof(tP2PInitiatorDriverConnection));
      pConnection->nServiceIdentifier = nServiceIdentifier;

      nError = PHandleRegister(pContext, pConnection, P_HANDLE_TYPE_P2P_INITIATORDRIVER_CONNECTION, &hConnection);

      if (nError == W_SUCCESS)
      {
         * phDriverConnection = hConnection;
      }
      else
      {
         * phDriverConnection = W_NULL_HANDLE;
      }
   }
   else
   {
      *phDriverConnection = W_NULL_HANDLE;

      nError = W_ERROR_OUT_OF_RESOURCE;
   }



   if (nError == W_SUCCESS)
   {
      PReaderDriverRegisterP2PDataIndicationCallback(pContext, (tDFCCallback *) static_PP2PInitiatorDataReceived, pConnection);
   }

   return (nError);
}

/**
 * Destroys an existing Initiator P2P connection
 *
 **/
static uint32_t static_PP2PDriverDestroyConnection(
      tContext * pContext,
      void     * pObject
)
{
   tP2PInitiatorDriverConnection * pP2PDriverConnection = (tP2PInitiatorDriverConnection*)pObject;

   PDebugTrace("static_PP2PDriverDestroyConnection");

   PDFCFlushCall(&pP2PDriverConnection->sCallbackContext);

   PReaderDriverUnregisterP2PDataIndicationCallback(pContext);

   /* Free the P2P initiator connection structure */
   CMemoryFree( pP2PDriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * Creates a NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS
 *
 * @return  W_SUCCESS if the parameters are valid, an error otherwise.
 **/

static W_ERROR static_PP2PInitiatorDriverSetDetectionConfiguration(
            tContext* pContext,
            uint8_t* pCommandBuffer,
            uint32_t* pnCommandBufferLength,
            const uint8_t* pDetectionConfigurationBuffer,
            uint32_t nDetectionConfigurationLength )
{
   PDebugTrace("static_PP2PInitiatorDriverSetDetectionConfiguration");

   if ((pCommandBuffer == null) || (pnCommandBufferLength == null))
   {
      PDebugError("static_PP2PInitiatorDriverSetDetectionConfiguration: bad Parameters");
      return (W_ERROR_BAD_PARAMETER);
   }

   if ((pDetectionConfigurationBuffer == null) || (nDetectionConfigurationLength == 0))
   {
      PDebugError("static_PP2PInitiatorDriverSetDetectionConfiguration: bad Parameters");
      return (W_ERROR_BAD_PARAMETER);
   }

   CMemoryCopy(pCommandBuffer, pDetectionConfigurationBuffer, nDetectionConfigurationLength);

   * pnCommandBufferLength = nDetectionConfigurationLength;

   return (W_SUCCESS);
}

/**
 * Parses NAL_EVT_P2P_TARGET_DISCOVERED parameters contents
 *
 * @return  W_SUCCESS if the parameters are valid, an error otherwise.
 **/

static W_ERROR static_PP2PInitiatorDriverParseDetectionMessage(
            tContext* pContext,
            const uint8_t* pBuffer,
            uint32_t nLength,
            tPReaderDriverCardInfo* pCardInfo )
{
   PDebugTrace("static_PP2PInitiatorDriverParseDetectionMessage");

   pCardInfo->nUIDLength = (uint8_t)nLength;
   CMemoryCopy(pCardInfo->aUID, pBuffer, nLength);
   pCardInfo->nAFI = 0;

   return (W_SUCCESS);
}

/**
 * Callback called when a NFC HAL Service Send Event operation has been completed
 *
 * @param[in] pContext The context
 *
 * @param[in] pCallbackParameter The callback parameter
 *
 * @param[in] nError The result of the operation
 *
 * @param[in] nReceprtionCounter
 **/

static void static_PP2PInitiatorDriverSendEventCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError,
         uint32_t nReceptionCounter )
{
   tP2PInitiatorDriverConnection * pP2PDriverConnection = (tP2PInitiatorDriverConnection *) pCallbackParameter;

   PDebugTrace("static_PP2PInitiatorDriverSendEventCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PP2PInitiatorDriverSendEventCompleted : %s",PUtilTraceError(nError));
   }

   /* Send the result of the operation */
   PDFCPostContext2(& pP2PDriverConnection->sCallbackContext,  nError );

   /* Decrement the reference count */
   PHandleDecrementReferenceCount(pContext, pP2PDriverConnection);
}

/** The protocol information structure */

tPRegistryDriverReaderProtocolConstant g_sP2PInitiatorReaderProtocolConstant = {
      W_NFCC_PROTOCOL_READER_P2P_INITIATOR,
      NAL_SERVICE_P2P_INITIATOR,
      static_PP2PInitiatorDriverCreateConnection,
      static_PP2PInitiatorDriverSetDetectionConfiguration,
      static_PP2PInitiatorDriverParseDetectionMessage,
      null
};

static const uint8_t g_aSYMM[2] = { 0x00, 0x00 };

/**
  * Sends data to the peer device
  *
  */

static void static_PP2PInitiatorDriverSendData(
         tContext                                  * pContext,
         W_HANDLE                                    hConnection,
         tP2PInitiatorDriverSendDataCompleted      * pCallback,
         void                                      * pCallbackParameter,
         const uint8_t                             * pReaderToCardBuffer,
         uint32_t                                    nReaderToCardBufferLength)
{
   tP2PInitiatorDriverConnection * pP2PDriverConnection;
   tDFCCallbackContext             sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PP2PInitiatorDriverSendData");

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);

   nError = PReaderDriverGetConnectionObject( pContext, hConnection, P_HANDLE_TYPE_P2P_INITIATORDRIVER_CONNECTION, (void**)&pP2PDriverConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PP2PInitiatorDriverSendData: could not get pP2PDriverConnection");

      /* Send the error */
      PDFCPostContext2( &sCallbackContext,  nError );
      return;
   }

   /* Store the callback context */
   pP2PDriverConnection->sCallbackContext = sCallbackContext;

   /* Check the parameters */
   if (  ( (pReaderToCardBuffer == null) && (nReaderToCardBufferLength != 0) )
      || ( nReaderToCardBufferLength > LLCP_NUMBERED_PDU_DATA_OFFSET + LLCP_DEFAULT_MIU + LLCP_MAX_LOCAL_MIUX )
      )
   {
      PDebugError("PP2PInitiatorDriverSendData : W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;

      /* Send the error */
      PDFCPostContext2( &pP2PDriverConnection->sCallbackContext, nError );
      return;
   }

   CMemoryCopy(
      &pP2PDriverConnection->aReaderToCardBufferNAL[0],
      pReaderToCardBuffer,
      nReaderToCardBufferLength );

   /* Increment the reference count to keep the connection object alive
      during the operation.
      The reference count is decreased in static_PP2PInitiatorDriverSendEventCompleted
      when the NFC HAL operation is completed */
   PHandleIncrementReferenceCount(pP2PDriverConnection);

   /* Send the NAL_EVT_P2P_SEND_DATA */

   PNALServiceSendEvent(
      pContext,
      pP2PDriverConnection->nServiceIdentifier,
      &pP2PDriverConnection->sServiceOperation,
      NAL_EVT_P2P_SEND_DATA,
      pP2PDriverConnection->aReaderToCardBufferNAL,
      nReaderToCardBufferLength,
      static_PP2PInitiatorDriverSendEventCompleted,
      pP2PDriverConnection);
}

/**
 * Called when send data operation has been completed
 *
 */

static void static_PP2PInitiatorSendDataCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError )
{
   PDebugTrace("PP2PInitiatorSendDataCompleted");

   if (nError != W_SUCCESS)
   {
      PP2PLinkGeneralFailure(pContext, nError);
   }
}


/**
 * Callback called when a EVT_P2P_SEND_DATA has been received
 *
 * @param[in] pContext The context
 *
 * @param[in] pCallbackParameter The callback parameter, e.g the P2P connection
 *
 * @param[in] pBuffer Pointer to buffer that contains the data received
 *
 * @param[in] nLength The amount of data received
 */

void static_PP2PInitiatorDataReceived(
         tContext* pContext,
         void* pCallbackParameter,
         const uint8_t* pBuffer,
         uint32_t nLength)
{
   tP2PInstance   * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPPDUHeader * pPDU;
   W_ERROR          nError;

   PDebugTrace("PP2PInitiatorProcessIncomingData");

   /* Set anti replay reference to avoid card redetection at the end of P2P session */
   PReaderDriverSetAntiReplayReference(pContext);

   /* we've received a PDU from the peer, stop the timer */

   PMultiTimerCancel(pContext, TIMER_T9_P2P);

   /* process the incoming PDU */

   pPDU = PLLCPAllocatePDU(pContext);

   if (pPDU == null)
   {
      /* unable to allocate PDU to retrieve incoming data */
      PDebugError("PP2PInitiatorProcessIncomingData : PLLCPAllocatePDU failed");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   /* Copy the received data in the PDU area */

   if (nLength > LLCP_NUMBERED_PDU_DATA_OFFSET + LLCP_DEFAULT_MIU + LLCP_MAX_LOCAL_MIUX)
   {
      /* the data received is too big ! */
      PDebugError("PP2PInitiatorProcessIncomingData : received data too big");
      nError = W_ERROR_BUFFER_TOO_LARGE;
      PLLCPFreePDU(pContext, pPDU);
      goto error;
   }

   CMemoryCopy(pPDU->aPayload, pBuffer, nLength);
   pPDU->nLength = (uint16_t) nLength;

   /* process the incoming PDU */
   PLLCPProcessIncomingPDU(pContext, pPDU);

   /* send the answer */

   if ((pPDU = PLLCPFramerGetNextXmitPDU(pContext, W_TRUE)) != null)
   {
      PDebugTrace("PP2PInitiatorProcessIncomingData[jim] pPDU != null");
      static_PP2PInitiatorDriverSendData(
         pContext,
         pP2PInstance->hP2PInitiatorConnection,
         static_PP2PInitiatorSendDataCompleted,
         null,
         pPDU->aPayload,
         pPDU->nLength);

      PLLCPFreePDU(pContext, pPDU);

      /* restart timer to check the peer is still alive */

      PMultiTimerSet( pContext, TIMER_T9_P2P, 2 * pP2PInstance->sLLCPInstance.nRemoteTimeoutMs,      /* *2 for more robusteness */
                        &PP2PRemoteTimeoutExpiry, null);
   }
   else
   {
      /* no PDU to transmit, send a SYMM */

      //PMultiTimerSet( pContext, TIMER_T9_P2P, pP2PInstance->sLLCPInstance.nLocalTimeoutMs / 2,      /* *2 for more robusteness */
      //            &static_PP2PInitiatorLocalTimeoutExpiry, null);
      //jim test
      PDebugTrace("PP2PInitiatorProcessIncomingData[jim] pPDU is null");
      PMultiTimerSet( pContext, TIMER_T9_P2P, 5,      /* timeout set 5 ms */
                  &static_PP2PInitiatorLocalTimeoutExpiry, null);

   }

   return;

error:
   PP2PLinkGeneralFailure(pContext, nError);
}


/*
 * Delayed activation timer expiry.
 * This is due to overcome an issue when peer controller is a PN544 chip
 * (100 ms delay)
 */

static void static_PP2PInitiatorTargetDetectedDelayedProcessing(
   tContext * pContext,
   void     * pCallbackParameter
   )
{
   tP2PInstance   * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPPDUHeader * pPDU;

   pPDU = PLLCPFramerGetNextXmitPDU(pContext, W_TRUE);

   if (pPDU != null)
   {
      static_PP2PInitiatorDriverSendData(
         pContext,
         pP2PInstance->hP2PInitiatorConnection,
         static_PP2PInitiatorSendDataCompleted,
         null,
         pPDU->aPayload,
         pPDU->nLength);

      PLLCPFreePDU(pContext, pPDU);
   }
   else
   {
      /* no PDU to transmit, send a SYMM */

      static_PP2PInitiatorDriverSendData(
         pContext,
         pP2PInstance->hP2PInitiatorConnection,
         static_PP2PInitiatorSendDataCompleted,
         null,
         g_aSYMM,
         sizeof(g_aSYMM));
   }

   PMultiTimerSet( pContext, TIMER_T9_P2P, 2 * pP2PInstance->sLLCPInstance.nRemoteTimeoutMs,      /* *2 for more robusteness */
                     &PP2PRemoteTimeoutExpiry, null);
}

/**
 * P2P Initiator card detection callback
 */

static void static_PP2PInitiatorTargetDetected(
      tContext* pContext,
      void* pCallbackParameter,
      uint32_t nDriverProtocol,
      W_HANDLE hDriverConnection,
      uint32_t nLength,
      bool_t bCardApplicationMatch )
{
   tP2PInstance   * pP2PInstance = PContextGetP2PInstance(pContext);
   W_ERROR          nError;

   PDebugTrace("static_PP2PInitiatorTargetDetected");

   pP2PInstance->hP2PInitiatorConnection = hDriverConnection;
   /* The P2P target has been detected */
   nError = PLLCPProcessLinkActivation(pContext, LLCP_ROLE_INITIATOR, pP2PInstance->aBuffer, (uint16_t) nLength,
                                          (tLinkDeactivationCallback *) PP2PLinkDeactivationCallback, pContext);

   /* Call the establishment request callbacks */
   PP2PCallAllLinkEstablishmentCallbacks(pContext, nError);

   if (nError == W_SUCCESS)
   {
      PMultiTimerSet( pContext, TIMER_T9_P2P, 100,  &static_PP2PInitiatorTargetDetectedDelayedProcessing, null);
   }
   else
   {
      PDebugTrace("static_PP2PTargetEmulationEventReceived  : PLLCPProcessLinkActivation failed");

      PP2PLinkGeneralFailure(pContext, nError);
   }
}

/**
 *  P2P initiator local timeout expiry
 *  Send a SYMM or a pending PDU
 */

void static_PP2PInitiatorLocalTimeoutExpiry(
   tContext * pContext,
   void     * pCallbackParameter
   )
{
   tP2PInstance   * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPPDUHeader * pPDU;

   PDebugTrace("static_PP2PInitiatorLocalTimeoutExpiry");

   pPDU = PLLCPFramerGetNextXmitPDU(pContext, W_TRUE);

   if (pPDU != null)
   {
      PDebugTrace("static_PP2PInitiatorLocalTimeoutExpiry[jim] Send Info PDU");
      static_PP2PInitiatorDriverSendData(
         pContext,
         pP2PInstance->hP2PInitiatorConnection,
         static_PP2PInitiatorSendDataCompleted,
         null,
         pPDU->aPayload,
         pPDU->nLength);

      PLLCPFreePDU(pContext, pPDU);
   }
   else
   {
      /* no PDU to transmit, send a SYMM */
      PDebugTrace("static_PP2PInitiatorLocalTimeoutExpiry[jim] Send SYMM PDU");
      static_PP2PInitiatorDriverSendData(
         pContext,
         pP2PInstance->hP2PInitiatorConnection,
         static_PP2PInitiatorSendDataCompleted,
         null,
         g_aSYMM,
         sizeof(g_aSYMM));
   }

   PMultiTimerSet( pContext, TIMER_T9_P2P, 2 * pP2PInstance->sLLCPInstance.nRemoteTimeoutMs,      /* *2 for more robusteness */
                     &PP2PRemoteTimeoutExpiry, null);
}


/**
 *   Start P2P initiator detection
 */

void PP2PInitiatorStartDetection(
   tContext                * pContext
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   uint8_t nLLCPParametersSize;
   tUserInstance * pUserInstance;
   W_ERROR nError;

   PDebugTrace("static_PP2PInitiatorStartDetection");

   pP2PInstance->nState = P_LLCP_STATE_STARTING;

   /* start the P2P initiator if it has been requested*/
   if (pP2PInstance->sLLCPInstance.bAllowInitiatorMode)
   {
      /* set the LLCP magic parameters */
      nLLCPParametersSize = PLLCPBuildConfigurationBuffer(pContext, pP2PInstance->aBuffer);

      /* Temporary set current user to null, since
        the handle must not be associated to any user */

      pUserInstance = PContextGetCurrentUserInstance(pContext);
      PContextSetCurrentUserInstance(pContext, null);

      nError = PReaderDriverRegisterInternal(
                  pContext,
                  static_PP2PInitiatorTargetDetected,
                  pP2PInstance,
                  W_PRIORITY_EXCLUSIVE,
                  W_NFCC_PROTOCOL_READER_P2P_INITIATOR,
                  nLLCPParametersSize,
                  pP2PInstance->aBuffer,
                  sizeof (pP2PInstance->aBuffer),
                  &pP2PInstance->hP2PInitiatorListener,
                  W_FALSE);

      /* restore user */
      PContextSetCurrentUserInstance(pContext, pUserInstance);
   }
   else
   {
      pP2PInstance->hP2PInitiatorListener = W_NULL_HANDLE;

      nError = W_SUCCESS;
   }

   /* start the P2P target */
   if (nError == W_SUCCESS)
   {
      PP2PTargetStartDetection(pContext);
   }
   else
   {
      PDebugError("static_PP2PInitiatorStartDetection : nError %s", PUtilTraceError(nError));
      pP2PInstance->nState = P_LLCP_STATE_ERROR;
      PP2PLinkGeneralFailure(pContext, nError);
   }
}

/**
 *   Stop P2P initiator detection
 */

void PP2PInitiatorStopDetection(
   tContext                * pContext
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tUserInstance * pUserInstance;
   PDebugTrace("PP2PInitiatorStopDetection");

   pP2PInstance->nState = P_LLCP_STATE_STOPPING;

   if (pP2PInstance->hP2PInitiatorListener != W_NULL_HANDLE)
   {
      PDebugTrace("PP2PInitiatorStopDetection : PHandleClose");

      /* Temporary set current user to null, since
        the handle is not be associated to any user */

      pUserInstance = PContextGetCurrentUserInstance(pContext);
      PContextSetCurrentUserInstance(pContext, null);

      PHandleClose(pContext, pP2PInstance->hP2PInitiatorListener);
      pP2PInstance->hP2PInitiatorListener = W_NULL_HANDLE;

      /* restore current user */
      PContextSetCurrentUserInstance(pContext, pUserInstance);
   }

   PP2PTargetStopDetection(pContext);
}


void PP2PInitiatorStopConnection(
   tContext                * pContext
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   PDebugTrace("PP2PInitiatorStopConnection");

   if (pP2PInstance->hP2PInitiatorConnection != W_NULL_HANDLE)
   {
      PDebugTrace("PP2PInitiatorStopConnection : PReaderDriverWorkPerformed");
      PReaderDriverWorkPerformed(pContext, pP2PInstance->hP2PInitiatorConnection, W_FALSE, W_TRUE);
      pP2PInstance->hP2PInitiatorConnection  = W_NULL_HANDLE;
   }

   pP2PInstance->nState = P_LLCP_STATE_STOPPED;

   pP2PInstance->sLLCPInstance.nRole = LLCP_ROLE_NONE;

   /* Call the pending URI lookup callback (if any) */
   PP2PCallURILookUpCallback(pContext, W_ERROR_CANCEL);

   /* Call the pending read/write callbacks (if any) */
   PP2PCallAllSocketReadAndWriteCallbacks(pContext, W_ERROR_CANCEL);

   /* Call the pending connect callbacks (if any) */
   PP2PCallAllSocketConnectCallbacks(pContext, W_ERROR_CANCEL);

   /* Mark all the established connections as broken (if any) */
   PP2PCallAllDisconnectIndicationCallback(pContext);

   /* call all link release callbacks (if any) */
   PP2PCallAllLinkReleaseCallbacks(pContext, pP2PInstance->nLinkReleaseCause);


   if (pP2PInstance->nTargetState == P_LLCP_STATE_STARTED)
   {
      PP2PInitiatorStartDetection(pContext);
   }
}
#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */
