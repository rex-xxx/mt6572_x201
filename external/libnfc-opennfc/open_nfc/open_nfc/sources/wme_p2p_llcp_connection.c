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
   Contains the implementation of the LLCP connection manager
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_LLC )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Initializes LLCP connection component

 * @param[in]   pLLCPCONNInstance The CONN instance
 *
 * @return
 *         - W_SUCCESS
 *
 **/

W_ERROR PLLCPCreateCONN(tLLCPCONNInstance * pLLCPCONNInstance)
{
   PDebugTrace("PLLCPCreateCONN");

   CMemoryFill(pLLCPCONNInstance, 0, sizeof(tLLCPCONNInstance));

   return (W_SUCCESS);
}

W_ERROR PLLCPDestroyCONN(tLLCPCONNInstance * pLLCPCONNInstance)
{
   PDebugTrace("PLLCPDestroyCONN");

   CMemoryFill(pLLCPCONNInstance, 0, sizeof(tLLCPCONNInstance));

   return (W_SUCCESS);
}

/* See header */
tLLCPConnection * PLLCPConnectionAllocContext(
   tContext    * pContext,
   uint8_t       nLocalSAP,
   uint8_t       nRemoteSAP,
   tP2PSocket * pP2PSocket
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPCONNInstance * pLLCPCONNInstance = & pP2PInstance->sLLCPInstance.sCONNInstance;
   tLLCPConnection * pConnection;

   pConnection = (tLLCPConnection *) CMemoryAlloc(sizeof(tLLCPConnection));

   if (pConnection != null)
   {
      /* initializes connection context */

      CMemoryFill(pConnection, 0, sizeof(tLLCPConnection));

      pConnection->pNext = null;
      pConnection->nLocalSAP  = nLocalSAP;
      pConnection->nRemoteSAP = nRemoteSAP;
      pConnection->nMIUL = pP2PSocket->sConfig.nLocalMIU;
      pConnection->nRWL = pP2PSocket->sConfig.nLocalRW;

      pConnection->nState = LLCP_CONNECTION_STATE_IDLE;
      pConnection->pP2PSocket = pP2PSocket;

      /* insert the connection in the head of the connection list */

      pConnection->pNext = pLLCPCONNInstance->pConnectionList;
      pLLCPCONNInstance->pConnectionList = pConnection;
   }

   PDebugTrace("PLLCPConnectionAllocContext %p", pConnection);


   return (pConnection);
}


/* See header */
W_ERROR PLLCPConnectionFreeContext(
   tContext        * pContext,
   tLLCPConnection * pConnection
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPCONNInstance * pLLCPONNInstance = & pP2PInstance->sLLCPInstance.sCONNInstance;
   tP2PSocket * pP2PSocket;

   tLLCPConnection * pPriorConnection;
   tLLCPConnection * pCurrentConnection;

   PDebugTrace("PLLCPConnectionFreeContext %p", pConnection);

   if (pConnection == null)
   {
      return W_SUCCESS;
   }

   pP2PSocket = pP2PInstance->pFirstSocket;

   while (pP2PSocket != null)
   {
      if ((pP2PSocket->sConnection.pConnection == pConnection) || (pP2PSocket->sConnection.pPendingConnection == pConnection))
      {
         PDebugError("PLLCPConnectionFreeContext : cleaning pConnection %p STILL IN USE in socket %p : TROUBLE AHEAD", pConnection, pP2PSocket);
      }

      pP2PSocket = pP2PSocket->pNext;
   }

   pPriorConnection = null;
   pCurrentConnection  = pLLCPONNInstance->pConnectionList;

   while (pCurrentConnection != null)
   {
      if (pCurrentConnection == pConnection)
      {
         /* found the corresponding connection object */

         if (pPriorConnection != null)
         {
            pPriorConnection->pNext = pConnection->pNext;
         }
         else
         {
            pLLCPONNInstance->pConnectionList = pConnection->pNext;
         }

            /* free the pConnection object */
         CMemoryFree(pConnection);
         return (W_SUCCESS);
      }

      pPriorConnection = pCurrentConnection;
      pCurrentConnection = pCurrentConnection->pNext;
   }

   PDebugError("PLLCPConnectionFreeContext %p : THIS CONNECTION NO LONGER EXISTS", pConnection);
   return (W_ERROR_BAD_PARAMETER);
}

/**
 * Accesses a LLCP connection from local and remote SAP
 *
 * @param[in]   pContext    The context.
 * @param[in]   nLocalSAP   The local SAP.
 * @param[in]   nRemoteSAP  The remote SAP.
 *
 * @return  the LLCP connection if found, else null
 **/

tLLCPConnection * PLLCPConnectionAccessContext(
   tContext * pContext,
   uint8_t nLocalSAP,
   uint8_t nRemoteSAP
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPCONNInstance * pLLCPONNInstance = & pP2PInstance->sLLCPInstance.sCONNInstance;
   tLLCPConnection * pConnection = null;

   CDebugAssert(pLLCPONNInstance != null);

   for (pConnection = pLLCPONNInstance->pConnectionList; pConnection != null; pConnection = pConnection->pNext)
   {
      if (pConnection->nLocalSAP == nLocalSAP)
      {
         /* in LLCP_CONNECTION_STATE_CONNECT_SENT, we ignore the source SAP since it may differ from the initial DSAP (SDP indirection) */
         /* otherwise, we check both local and remote SAP */

         if ((pConnection->nState == LLCP_CONNECTION_STATE_CONNECT_SENT) ||
             (pConnection->nRemoteSAP == nRemoteSAP))
         {
            break;
         }
      }
   }

   PDebugTrace("PLLCPConnectionAccessContext local SAP %d - remote SAP %d : %p", nLocalSAP, nRemoteSAP, pConnection);
   return (pConnection);
}

static void static_PLLCPConnectCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   tLLCPConnection * pConnection = pCallbackParameter;

   PDebugTrace("static_PLLCPConnectCompleted : pConnection %p", pConnection);
   pConnection->pConnectPDU = null;
}

/* See header */

W_ERROR PLLCPConnect(
   tContext * pContext,
   tLLCPConnection * pConnection,
   uint8_t * pURI,
   uint32_t   nURILength,
   uint8_t nSAP
   )
{
   tLLCPPDUHeader * pPDU;
   tP2PInstance   * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance  * pLLCPInstance = &pP2PInstance->sLLCPInstance;

   PDebugTrace("PLLCPConnect : connection %p", pConnection);

   if (pConnection->nState == LLCP_CONNECTION_STATE_IDLE)
   {
      pPDU = PLLCPAllocatePDU(pContext);

      if (pPDU == null)
      {
         return (W_ERROR_OUT_OF_RESOURCE);
      }

      pConnection->pConnectPDU = pPDU;

      PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

      pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_CONNECT;
      pLLCPInstance->sXmitPDUDescriptor.nSSAP = pConnection->nLocalSAP;

      if (nURILength != 0)
      {
         /* an URI is specified, use a connect 'by name' */
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = LLCP_SDP_SAP;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSN.bIsPresent = W_TRUE;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSN.pSN = pURI;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSN.nSNLength = (uint8_t) nURILength;
      }
      else
      {
         /* a URI is not specified, use a connect 'by SAP' */
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = nSAP;
      }

      if (pConnection->nRWL != LLCP_PARAM_RW_DEFAULT)
      {
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sRW.bIsPresent = W_TRUE;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sRW.nRW = pConnection->nRWL;
      }

      if (pConnection->nMIUL != LLCP_DEFAULT_MIU)
      {
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sMIUX.bIsPresent = W_TRUE;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sMIUX.nMIUX = pConnection->nMIUL - LLCP_DEFAULT_MIU;
      }

      PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pPDU->nLength, pPDU->aPayload);

      PLLCPFramerEnterXmitPacketWithAck(pContext, pPDU, static_PLLCPConnectCompleted, pConnection);
      pConnection->nState = LLCP_CONNECTION_STATE_CONNECT_SENT;
   }

   return (W_SUCCESS);
}


static void static_PLLCPDisconnectCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   tLLCPConnection * pConnection = pCallbackParameter;

   PDebugTrace("static_PLLCPDisconnectCompleted pConnection %p %s", pConnection, PUtilTraceError(nError));

   if (nError == W_SUCCESS)
   {
      pConnection->nState = LLCP_CONNECTION_STATE_DISC_SENT;
      pConnection->pDiscPDU = null;
   }
}


/**
 * Initiates an outgoing connection disconnection
 *
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 *
 *
 * @return W_SUCCESS on success
 *         W_ERROR_OUT_OF_RESOURCE
 *         W_ERROR_BAD_STATE
 **/

void PLLCPDisconnect(
   tContext * pContext,
   tLLCPConnection * pConnection)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pXmitPDU;

   CDebugAssert(pConnection != null);

   if ((pLLCPInstance->nRole != LLCP_ROLE_NONE) && (pConnection->nState == LLCP_CONNECTION_STATE_ESTABLISHED))
   {
      /* nominal case */
      pConnection->bShutdownRequested = W_TRUE;

      pXmitPDU = PLLCPAllocatePDU(pContext);

      if (pXmitPDU == null)
      {
         /* no memory to build the DISC PDU */
         static_PLLCPDisconnectCompleted(pContext, pConnection, W_ERROR_OUT_OF_RESOURCE);
         return;
      }

      pConnection->pDiscPDU = pXmitPDU;

      PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

      pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DISC;
      pLLCPInstance->sXmitPDUDescriptor.nSSAP = pConnection->nLocalSAP;
      pLLCPInstance->sXmitPDUDescriptor.nDSAP = pConnection->nRemoteSAP;

      PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
      PLLCPFramerEnterXmitPacketWithAck(pContext, pXmitPDU, static_PLLCPDisconnectCompleted, pConnection);
   }
   else
   {
      static_PLLCPDisconnectCompleted(pContext, pConnection, W_ERROR_BAD_STATE);
   }
}

/**
 * Start transmission of SDU in the LLCP connection xmit fifo
 *
 * @param[in] pContext        The context
 * @param[in] pConnection     The LLCP connection
 *
 * @return - W_TRUE if a PDU has been sent, otherwise W_FALSE
 *
 */

bool_t PLLCPConnectionStartXmitSDU(
   tContext          * pContext,
   tLLCPConnection   * pConnection
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tP2PSocket * pP2PSocket;

   tLLCPPDUHeader * pXmitPDU;
   bool_t             bPDUSent;

   CDebugAssert(pLLCPInstance != null);
   CDebugAssert(pConnection != null);

   pP2PSocket = pConnection->pP2PSocket;

   if (pP2PSocket->pXmitBuffer == null)
   {
      /* no SDU waiting for transmission */
      return (W_FALSE);
   }

   if ((pConnection->bIsPeerBusy != W_FALSE) && (pConnection->bShutdownRequested == W_FALSE))
   {
      /* do not send data if remote peer is busy and we are not in shutdown sequence.

         During shutdown sequence, allow to send data even if the peer is busy to avoid
         infinite shutdown duration if peer does not exit from congestion.
       */

      return (W_FALSE);
   }

   bPDUSent = W_FALSE;

   while ( (pP2PSocket->pXmitBuffer != null) &&
           (pConnection->nVS != ((pConnection->nVSA + pConnection->nRWR) % 16)))
   {
      pXmitPDU = PLLCPAllocatePDU(pContext);

      if (pXmitPDU == null)
      {
         /* no resource */
         return (bPDUSent);
      }

      PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

      pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_I,
      pLLCPInstance->sXmitPDUDescriptor.nSSAP = pConnection->nLocalSAP;
      pLLCPInstance->sXmitPDUDescriptor.nDSAP = pConnection->nRemoteSAP;
      pLLCPInstance->sXmitPDUDescriptor.nNS = pConnection->nVS;
      pLLCPInstance->sXmitPDUDescriptor.nNR = pConnection->nVR;

      pLLCPInstance->sXmitPDUDescriptor.sPayload.sSDU.pInformation = pP2PSocket->pXmitBuffer;
      pLLCPInstance->sXmitPDUDescriptor.sPayload.sSDU.nInformationLength = (uint16_t) pP2PSocket->nXmitBufferLength;

      PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
      PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);
      pConnection->nVRA = pConnection->nVR;

      IncMod16(pConnection->nVS);


      bPDUSent = W_TRUE;

      PP2PCallSocketWriteCallback(pContext, pConnection->pP2PSocket, W_SUCCESS);
   }


   return (bPDUSent);
}

/**
 * Start reception of SDU
 *
 * @param[in] pContext        The context
 * @param[in] pConnection     The LLCP connection
 */

void PLLCPConnectionStartRecvSDU(
   tContext          * pContext,
   tLLCPConnection   * pConnection
   )
{
   tP2PSocket     * pP2PSocket;
   tLLCPPDUHeader * pRecvPDU;
   uint16_t         nPayloadSize;
   W_ERROR          nError;

   CDebugAssert(pConnection != null);
   pP2PSocket = pConnection->pP2PSocket;

   if (pP2PSocket->pRecvBuffer == null)
   {
      /* no user buffer to store incoming SDU */
      return;
   }

   if (pP2PSocket->pFirstRecvPDU == null)
   {
      /* no received buffer waiting for user delivery */
      return;
   }

   /* process the received PDU until all pending received PDUs have been processed
      or the receive buffer is full */

   pRecvPDU = pP2PSocket->pFirstRecvPDU;
   nPayloadSize = pRecvPDU->nLength;

   if (nPayloadSize <= pP2PSocket->nRecvBufferLength)
   {
      /* the payload fits entirely in the received buffer */
      CMemoryCopy(pP2PSocket->pRecvBuffer, & pRecvPDU->aPayload[pRecvPDU->nDataOffset], nPayloadSize);

      /* the received PDU payload has been copied in the user buffer,
         remove it from the receive FIFO and free it */

      pP2PSocket->nRecvPDU--;
      pP2PSocket->pFirstRecvPDU = pRecvPDU->pNext;

      if (pP2PSocket->pFirstRecvPDU == null)
      {
         pP2PSocket->pLastRecvPDU = null;
      }

      if (pP2PSocket->nRecvPDU < pP2PSocket->sConfig.nLocalRW)
      {
         if ((pConnection->bIsBusy) && (pConnection->nState == LLCP_CONNECTION_STATE_ESTABLISHED))
         {
            /* reuse the pRecvPDU to send the RR */
            tDecodedPDU sDecodedPDU;

            PLLCPResetDescriptor(&sDecodedPDU);

            sDecodedPDU.nType = LLCP_PDU_RR;
            sDecodedPDU.nSSAP = pConnection->nLocalSAP;
            sDecodedPDU.nDSAP = pConnection->nRemoteSAP;
            sDecodedPDU.nNR   = pConnection->nVR;
            sDecodedPDU.nNS   = 0;

            PLLCPBuildPDU(&sDecodedPDU, &pRecvPDU->nLength, pRecvPDU->aPayload);
            PLLCPFramerEnterXmitPacket(pContext, pRecvPDU);
            pConnection->nVRA = pConnection->nVR;

            pConnection->bIsBusy = W_FALSE;
            pRecvPDU = null;
         }
      }

      if (pRecvPDU)
      {
         PLLCPFreePDU(pContext, pRecvPDU);
      }

      nError = W_SUCCESS;
   }
   else
   {
      /* The entire PDU does not fit in the receive buffer,
          return W_ERROR_BUFFER_TOO_SHORT */

      nError = W_ERROR_BUFFER_TOO_SHORT;
      nPayloadSize = 0;
   }

   /* Call the callback function */
   PP2PCallSocketReadCallback(pContext, pP2PSocket, nPayloadSize, nError, 0);
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */


/* EOF */
