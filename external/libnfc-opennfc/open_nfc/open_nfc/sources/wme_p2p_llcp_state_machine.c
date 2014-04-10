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
   Contains the implementation of the LLCP state machine
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_LLC )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

   /* Local Function Prototypes */

void static_PLLCPConnectionStateMachineIdle(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   );

void static_PLLCPConnectionStateMachineConnecting(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   );

void static_PLLCPConnectionStateMachineConnected(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   );

void static_PLLCPConnectionStateMachineDisconnecting(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   );

void static_PLLCPConnectionStateMachineDisconnected(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   );

uint8_t static_PLLCPCheckNRNS(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   );


/* See header */
void PLLCPConnectionStateMachine(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   )
{
   switch (pConnection->nState)
   {
      case LLCP_CONNECTION_STATE_IDLE :
         static_PLLCPConnectionStateMachineIdle(pContext, pConnection);
         break;

      case LLCP_CONNECTION_STATE_CONNECT_SENT :
         static_PLLCPConnectionStateMachineConnecting(pContext, pConnection);
         break;

      case LLCP_CONNECTION_STATE_ESTABLISHED :
         static_PLLCPConnectionStateMachineConnected(pContext, pConnection);
         break;

      case LLCP_CONNECTION_STATE_DISC_SENT :
         static_PLLCPConnectionStateMachineDisconnecting(pContext, pConnection);
         break;

      case LLCP_CONNECTION_STATE_DISCONNECTED:
         static_PLLCPConnectionStateMachineDisconnected(pContext, pConnection);
         break;
   }
}

static void static_PLLCPDMSent(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   tLLCPConnection  * pConnection = (tLLCPConnection  *) pCallbackParameter;

   PDebugTrace("static_PLLCPDMSent");

   pConnection->nState = LLCP_CONNECTION_STATE_DISCONNECTED;
   PP2PDisconnectIndicationCallback(pContext, pConnection);
}


/**
 * Implements the IDLE state machine
 *
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 *
 * @return  void
 *
 **/


void static_PLLCPConnectionStateMachineIdle(
   tContext * pContext,
   tLLCPConnection  * pConnection
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pXmitPDU;

   /* this correspond only to incoming CONNECT processing */

   PDebugTrace("static_PLLCPConnectionStateMachineIdle %p", pConnection);

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_CONNECT :

         PDebugTrace("static_PLLCPConnectionStateMachineIdle : received CONNECT");

         pConnection->nMIUL = pConnection->pP2PSocket->sConfig.nLocalMIU;
         pConnection->nRWL = pConnection->pP2PSocket->sConfig.nLocalRW;

         if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sMIUX.bIsPresent != W_FALSE)
         {
            pConnection->nMIUR = LLCP_DEFAULT_MIU + pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sMIUX.nMIUX;
         }
         else
         {
            pConnection->nMIUR = LLCP_DEFAULT_MIU;
         }

         if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sRW.bIsPresent != W_FALSE)
         {
            pConnection->nRWR = pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sRW.nRW;
         }
         else
         {
            pConnection->nRWR = LLCP_PARAM_RW_DEFAULT;
         }

         pConnection->nVS = 0;
         pConnection->nVR = 0;
         pConnection->nVSA = 0;
         pConnection->nVRA = 0;

         /* send CC */

         PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

         pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_CC;
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
         pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;

         if (pConnection->pP2PSocket->sConfig.nLocalMIU != LLCP_DEFAULT_MIU)
         {
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sMIUX.bIsPresent = W_TRUE;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sMIUX.nMIUX = pConnection->pP2PSocket->sConfig.nLocalMIU - LLCP_DEFAULT_MIU;
         }

         if (pConnection->pP2PSocket->sConfig.nLocalRW != LLCP_PARAM_RW_DEFAULT)
         {
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sRW.bIsPresent = W_TRUE;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sRW.nRW = pConnection->pP2PSocket->sConfig.nLocalRW;
         }

         pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSN.bIsPresent = W_FALSE;

         pXmitPDU = pLLCPInstance->pRcvPDU;
         pLLCPInstance->pRcvPDU = null;

         PLLCPBuildPDU(& pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
         PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

         pConnection->nState = LLCP_CONNECTION_STATE_ESTABLISHED;

            /* call the callback */
         PP2PConnectIndicationCallback(pContext, pConnection);

      break;


      default :
         /* other case should not occur */
         PDebugTrace("static_PLLCPConnectionStateMachineIdle : unexpected PDU %d", pLLCPInstance->sRcvPDUDescriptor.nType);
         break;
   }
}

/**
 * Implements the CONNECTING state machine
 *
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 *
 * @return  void
 *
 **/

void static_PLLCPConnectionStateMachineConnecting(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;

   PDebugTrace("static_PLLCPConnectionStateMachineConnecting %p", pConnection);

   if (pConnection->pConnectPDU != null)
   {
      if (pLLCPInstance->sRcvPDUDescriptor.nType == LLCP_PDU_CONNECT)
      {
         PDebugTrace("static_PLLCPConnectionStateMachineConnecting : connect collision detected");
         /* the connect PDU has not been sent */
         pConnection->pConnectPDU->bIsDiscarded = W_TRUE;
         PLLCPFramerRemoveDiscardedPDU(pContext);
         pConnection->pConnectPDU = null;

         pConnection->nState = LLCP_CONNECTION_STATE_IDLE;
         pConnection->nRemoteSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
         static_PLLCPConnectionStateMachineIdle(pContext, pConnection);
         return;
      }
      else
      {
         /* Consider other PDUs from previous connections */
         PDebugTrace("static_PLLCPConnectionStateMachineConnecting : trailing PDU %d", pLLCPInstance->sRcvPDUDescriptor.nType);
         return;
      }
   }

   /* this correspond to incoming CC or DM processing */

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_CC :

         PDebugTrace("static_PLLCPConnectionStateMachineConnecting : received CC");

         /* outgoing connection has been accepted by peer */

         if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sMIUX.bIsPresent != W_FALSE)
         {
            pConnection->nMIUR = LLCP_DEFAULT_MIU + pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sMIUX.nMIUX;
         }
         else
         {
            pConnection->nMIUR = LLCP_DEFAULT_MIU;
         }

         if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sRW.bIsPresent != W_FALSE)
         {
            pConnection->nRWR = pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sRW.nRW;
         }
         else
         {
            pConnection->nRWR = LLCP_PARAM_RW_DEFAULT;
         }

            /* update the remote SAP value in the connection connect */
         pConnection->nRemoteSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
         pConnection->nState = LLCP_CONNECTION_STATE_ESTABLISHED;

         /* call the callback */
         PP2PConnectCompleteCallback(pContext, pConnection, W_SUCCESS);
         break;

      case LLCP_PDU_DM :

         PDebugTrace("static_PLLCPConnectionStateMachineConnecting : received DM");

         /* outgoing connection has been rejected by peer */

         PP2PConnectCompleteCallback(pContext, pConnection, W_ERROR_P2P_CLIENT_REJECTED);
         break;

      default :

         PDebugTrace("static_PLLCPConnectionStateMachineConnecting : unexpected PDU %d", pLLCPInstance->sRcvPDUDescriptor.nType);
         break;
   }
}

/**
 * Checks the NS and NR fields of the received PDU
 *
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 *
 *
 * @return 0 if OK, else the error flag value to be put in the FRMR PDU
 *
 **/

uint8_t static_PLLCPCheckNRNS(
   tContext         * pContext,
   tLLCPConnection  * pConnection
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;


   uint8_t nErrorFlag = 0;
   /* uint8_t nNR; */
   uint8_t  nDistance;

   PDebugTrace("static_PLLCPCheckNRNS");

   CDebugAssert(pLLCPInstance != null);
   CDebugAssert(pConnection != null);

   /* See LLCP $4.3.9 */

#if 0 /* @todo */

      /* numbered PDU with N(R) equal to V(SA) */

   /* @todo  :

      - implementing this check leads to the connection to be broken at first packet reception

      - N(R) equal to V(SA) is a valid behaviour (it occurs each time two I PDUs are sent
        without any I PDU received between them

        so we removed this check
   */

   if (pLLCPInstance->sRcvPDUDescriptor.nNR == pConnection->nVSA)
   {
      nErrorFlag |= LLCP_FRMR_FLAG_R;
   }

#endif /* 0 */

#if 0 /* @todo */
      /* numbered PDU with N(R) equal to V(S) */

   /* @todo  :

      - implementing this check leads to the connection to be broken at first packet reception

      reception of a PDU with N(R) means the received PDU acknowledged all packets sent
      with N(S) value up to N(R) - 1.

      received N(R) equal to V(S) means the received PDU acknowledge all packets sent up to sequence
      number V(S)-1, which is perfectly correct since V(S) is the sequence number of the next paquet to
      be transmitted !

   */

   if (pConnection->bAtLeastOneIPDUSent != W_FALSE)
   {
      if (pLLCPInstance->sRcvPDUDescriptor.nNR == pConnection->nVS)
      {
         PDebugError("static_PLLCPCheckNRNS NR(%d) == VS(%d)",  pLLCPInstance->sRcvPDUDescriptor.nNR, pConnection->nVS);
         nErrorFlag |= LLCP_FRMR_FLAG_R;
      }
   }

#endif /* 0 */

#if 0 /* @todo */
   if (pConnection->bAtLeastOneIPDUReceived != W_FALSE)
   {
      /* repeatidly incrementing N(R) becomes equal to V(SA) prior V(S) */

      for (nNR = pLLCPInstance->sRcvPDUDescriptor.nNR ; nNR != pConnection->nVS; IncMod16(nNR))
      {
         if (nNR == pConnection->nVSA)
         {
            PDebugError("static_PLLCPCheckNRNS N(R) %d becomes equal to V(SA) %d prior V(S) %d",  pLLCPInstance->sRcvPDUDescriptor.nNR, pConnection->nVSA, pConnection->nVS);
            nErrorFlag |= LLCP_FRMR_FLAG_R;
            break;
         }
      }
   }
#endif /* 0 */
   if (pConnection->bAtLeastOneIPDUReceived != W_FALSE)
   {
      if (pLLCPInstance->sRcvPDUDescriptor.nType == LLCP_PDU_I) {

         /* N(S) >= V(RA)+RW(L), mod 16 */

         if (pLLCPInstance->sRcvPDUDescriptor.nNS >= pConnection->nVRA)
         {
            nDistance = pLLCPInstance->sRcvPDUDescriptor.nNS - pConnection->nVRA;
         }
         else
         {
            nDistance = 16 + pLLCPInstance->sRcvPDUDescriptor.nNS - pConnection->nVRA;
         }

         if (nDistance > pConnection->nRWL)
         {
            PDebugError("static_PLLCPCheckNRNS N(S)%d >= V(RA) %d + RW(L) %d",  pLLCPInstance->sRcvPDUDescriptor.nNS, pConnection->nVRA, pConnection->nRWL);
            nErrorFlag |= LLCP_FRMR_FLAG_S;
         }

            /* N(S) is not an increment by 1 of the last received N(S)
               we do not check RR and RNR since NS is always 0 in theses PDU */

         if (pLLCPInstance->sRcvPDUDescriptor.nNS != pConnection->nVR)
         {
            PDebugError("static_PLLCPCheckNRNS N(S) %d != V(R) %d",  pLLCPInstance->sRcvPDUDescriptor.nNS, pConnection->nVR);

            nErrorFlag |= LLCP_FRMR_FLAG_S;
         }
      }
   }
         /* Set the error flag in the decoded PDU */

   return (nErrorFlag);
}


/**
 * Implements the CONNECTED state machine
 *
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 *
 * @return  void
 *
 **/

void static_PLLCPConnectionStateMachineConnected(

   tContext         * pContext,
   tLLCPConnection  * pConnection
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pXmitPDU;
   uint8_t nErrorFlag;

   PDebugTrace("static_PLLCPConnectionStateMachineConnected %p", pConnection);

      /* For numbered PDU, check the validity of the NS/NR fields */

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_I :
      case LLCP_PDU_RR :
      case LLCP_PDU_RNR :

         if ((nErrorFlag = static_PLLCPCheckNRNS(pContext, pConnection)) != 0)
         {
            /* the receive PDU did not passed the check */

            PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

            pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_FRMR;
            pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
            pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nFlag = nErrorFlag;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nType = pLLCPInstance->sRcvPDUDescriptor.nType;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nNR = pLLCPInstance->sRcvPDUDescriptor.nNR;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nNS = pLLCPInstance->sRcvPDUDescriptor.nNS;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nVS = pConnection->nVS;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nVR = pConnection->nVR;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nVSA = pConnection->nVSA;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sFRMR.nVRA = pConnection->nVRA;

            pXmitPDU = pLLCPInstance->pRcvPDU;
            pLLCPInstance->pRcvPDU = null;

            PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
            PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

            /* inform the upper layer of the release of the connection */
            pConnection->nState = LLCP_CONNECTION_STATE_DISCONNECTED;
            PP2PDisconnectIndicationCallback(pContext, pConnection);
            return;
         }

      break;

      default :
         /* other PDU are not numbered */
         break;
   }


   /* The different checks passed */

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_I :       /* data traffic */

         PDebugTrace("static_PLLCPConnectionStateMachineConnected : received I");

         pConnection->bAtLeastOneIPDUReceived = W_TRUE;

         pConnection->nVSA = pLLCPInstance->sRcvPDUDescriptor.nNR;
         IncMod16(pConnection->nVR);

         /* skip the first bytes of the payload since it must niot be provided to user */

         pLLCPInstance->pRcvPDU->nDataOffset = LLCP_NUMBERED_PDU_DATA_OFFSET;
         pLLCPInstance->pRcvPDU->nLength -= LLCP_NUMBERED_PDU_DATA_OFFSET;

         /* enqueue the received PDU in the LLCP receive FIFO */

         if (pConnection->pP2PSocket->pLastRecvPDU == null)
         {
            pConnection->pP2PSocket->pFirstRecvPDU = pConnection->pP2PSocket->pLastRecvPDU =  pLLCPInstance->pRcvPDU;
         }
         else
         {
            pConnection->pP2PSocket->pLastRecvPDU->pNext = pLLCPInstance->pRcvPDU;
            pConnection->pP2PSocket->pLastRecvPDU = pLLCPInstance->pRcvPDU;
         }

         pConnection->pP2PSocket->nRecvPDU++;

         pLLCPInstance->pRcvPDU = null;

         PLLCPConnectionStartRecvSDU(pContext, pConnection);

         /* try to acknowledge the incoming I PDU by sending an I PDU if available */
         if (PLLCPConnectionStartXmitSDU(pContext, pConnection) == W_FALSE)
         {
            /* no PDU sent, send a RR / RNR using the received PDU */

            pXmitPDU = PLLCPAllocatePDU(pContext);

            PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

            if (pXmitPDU != null)
            {
               if (pConnection->pP2PSocket->nRecvPDU < pConnection->pP2PSocket->sConfig.nLocalRW)
               {
                  pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_RR;
                  pConnection->bIsBusy = W_FALSE;
               }
               else
               {
                  pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_RNR;
                  pConnection->bIsBusy = W_TRUE;
               }
               pLLCPInstance->sXmitPDUDescriptor.nSSAP = pConnection->nLocalSAP;
               pLLCPInstance->sXmitPDUDescriptor.nDSAP = pConnection->nRemoteSAP;
               pLLCPInstance->sXmitPDUDescriptor.nNR = pConnection->nVR;
               pLLCPInstance->sXmitPDUDescriptor.nNS = 0;

               PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
               PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);
               pConnection->nVRA = pConnection->nVR;

            }
         }

         if (pConnection->pDiscPDU != null)
         {
            pConnection->pDiscPDU->bIsDiscarded = W_TRUE;
            PLLCPFramerRemoveDiscardedPDU(pContext);
            pConnection->pDiscPDU = null;
            PLLCPDisconnect(pContext, pConnection);
         }

         break;

      case LLCP_PDU_RR :      /* data acknowlegde */

         PDebugTrace("static_PLLCPConnectionStateMachineConnected : received RR");
         pConnection->nVSA = pLLCPInstance->sRcvPDUDescriptor.nNR;
         pConnection->bIsPeerBusy = W_FALSE;

         PLLCPConnectionStartXmitSDU(pContext, pConnection);
         break;

      case LLCP_PDU_RNR :     /* peer busy */

         PDebugTrace("static_PLLCPConnectionStateMachineConnected : received RNR");
         pConnection->nVSA = pLLCPInstance->sRcvPDUDescriptor.nNR;
         pConnection->bIsPeerBusy = W_TRUE;
         break;


      case LLCP_PDU_DISC :    /* disconnection request */

         PDebugTrace("static_PLLCPConnectionStateMachineConnected : received DISC");
         /* send a DM using the received PDU */

         pXmitPDU = pLLCPInstance->pRcvPDU;
         pLLCPInstance->pRcvPDU = null;

         PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);
         pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
         pLLCPInstance->sXmitPDUDescriptor.nSSAP = pConnection->nLocalSAP;
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = pConnection->nRemoteSAP;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_DISC_CONFIRMED;
         PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);

         PLLCPFramerEnterXmitPacketWithAck(pContext, pXmitPDU, static_PLLCPDMSent, pConnection);
         break;

      case LLCP_PDU_FRMR :

         PDebugTrace("static_PLLCPConnectionStateMachineConnected : received FRMR");

         /* call Release Indication callback */
         pConnection->nState = LLCP_CONNECTION_STATE_DISCONNECTED;
         PP2PDisconnectIndicationCallback(pContext, pConnection);
         break;

      case LLCP_PDU_DM :
         PDebugTrace("static_PLLCPConnectionStateMachineConnected : received DM");

         /* call Release Indication callback */
         pConnection->nState = LLCP_CONNECTION_STATE_DISCONNECTED;
         PP2PDisconnectIndicationCallback(pContext, pConnection);
         break;

      default :

         PDebugTrace("static_PLLCPConnectionStateMachineConnected : unexpected PDU %d",  pLLCPInstance->sRcvPDUDescriptor.nType);
         /* error cases */
         break;
   }
}


/**
 * Implements the DISCONNECTING state machine
 *
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 *
 * @return  void
 *
 **/

void static_PLLCPConnectionStateMachineDisconnecting(
   tContext         * pContext,
   tLLCPConnection  * pConnection
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;


   /* See LLCP,  5.6.6 */

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_DM :

         PDebugTrace("static_PLLCPConnectionStateMachineDisconnecting : received DM");

         /* call Release Indication callback */
         pConnection->nState = LLCP_CONNECTION_STATE_DISCONNECTED;
         PP2PDisconnectIndicationCallback(pContext, pConnection);
         break;


      default :

         /* all others PDU must be discarded */
         PDebugTrace("static_PLLCPConnectionStateMachineDisconnecting : unexpected PDU %d", pLLCPInstance->sRcvPDUDescriptor.nType);
         break;
   }
}


void static_PLLCPConnectionStateMachineDisconnected(
   tContext         * pContext,
   tLLCPConnection  * pConnection
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pXmitPDU;

   PDebugTrace("static_PLLCPConnectionStateMachineDisconnected");

   if (pLLCPInstance->sRcvPDUDescriptor.nType != LLCP_PDU_DM)
   {
      pXmitPDU = pLLCPInstance->pRcvPDU;
      pLLCPInstance->pRcvPDU = null;
      PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

      pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
      pLLCPInstance->sXmitPDUDescriptor.nSSAP = pConnection->nLocalSAP;
      pLLCPInstance->sXmitPDUDescriptor.nDSAP = pConnection->nRemoteSAP;

      if (pLLCPInstance->sRcvPDUDescriptor.nType == LLCP_PDU_CONNECT)
      {
         /* once disconnected, do not accept further connection until the previous connection has been destroyed */
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_TEMPORARY_DSAP_REJECT;
      }
      else
      {
         /* no active connection */
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_NO_ACTIVE_CONNECTION;
      }

      PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
      PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);
   }
}
#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */


/* EOF */
