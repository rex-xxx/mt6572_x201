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
   Contains the implementation of the P2P target functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PP2PTargetDetectionStarted (tContext * pContext, void * pCallbackParameter, W_ERROR nError);
static void static_PP2PTargetEventReceived (tContext * pContext, void * pCallbackParameter, uint32_t nEventCode);
static void static_PP2PTargetCommandReceived (tContext * pContext, void * pCallbackParameter, uint32_t nDataLength);
static void static_PP2PTargetStopDetectionCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);

static const uint8_t g_aSYMM[2] = { 0x00, 0x00 };

/**
 *   Starts P2P target detection
 */

void PP2PTargetStartDetection(
   tContext                * pContext
   )
{
   tP2PInstance            * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance           * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tWEmulConnectionInfo     sEmulConnectionInfo;
   tUserInstance          * pUserInstance;

   PDebugTrace("PP2PTargetStartDetection");

   sEmulConnectionInfo.nCardType = W_PROP_P2P_TARGET;

   sEmulConnectionInfo.sCardInfo.sP2PTarget.nLLCPParameterSize = PLLCPBuildConfigurationBuffer(pContext, sEmulConnectionInfo.sCardInfo.sP2PTarget.aLLCPParameter);

   sEmulConnectionInfo.sCardInfo.sP2PTarget.bAllowTypeATargetProtocol = pLLCPInstance->bAllowTypeATargetProtocol;
   sEmulConnectionInfo.sCardInfo.sP2PTarget.bAllowActiveMode = pLLCPInstance->bAllowActiveMode;

   sEmulConnectionInfo.sCardInfo.sP2PTarget.nTimeOutRTX = 0x0D; /* pP2PInstance->sLLCPInstance.nLocalTimeoutMs; */

   /* Temporary set current user to null, since
        the handle must not be associated to any user */

   pUserInstance = PContextGetCurrentUserInstance(pContext);
   PContextSetCurrentUserInstance(pContext, null);

   PEmulOpenConnectionHelper(
      pContext,
      static_PP2PTargetDetectionStarted,
      null,
      static_PP2PTargetEventReceived,
      null,
      static_PP2PTargetCommandReceived,
      null,
      &sEmulConnectionInfo,
      &pP2PInstance->hP2PTargetConnection
   );

   /* restore user */
   PContextSetCurrentUserInstance(pContext, pUserInstance);

   PDebugTrace("pP2PInstance->hP2PTargetConnection %08x", pP2PInstance->hP2PTargetConnection);
}

/**
 * P2P Target emulation open callback
 */

static void static_PP2PTargetDetectionStarted (
      tContext * pContext,
      void     * pCallbackParameter,
      W_ERROR    nError )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   PDebugTrace("static_PP2PTargetDetectionStarted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PP2PTargetDetectionStarted : nError %s", PUtilTraceError(nError));
      pP2PInstance->nState = P_LLCP_STATE_ERROR;
      PP2PLinkGeneralFailure(pContext, nError);
   }
   else
   {
      pP2PInstance->nState = P_LLCP_STATE_STARTED;
   }

   if (pP2PInstance->nTargetState == P_LLCP_STATE_STOPPED)
   {
      PP2PInitiatorStopDetection(pContext);
   }
}

/**
 * Stop P2P Target emulation detection
 */

void PP2PTargetStopDetection(
   tContext                * pContext
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tUserInstance * pUserInstance;

   PDebugTrace("PP2PTargetStopDetection");

   if (pP2PInstance->hP2PTargetConnection != W_NULL_HANDLE)
   {
      /* Temporary set current user to null, since
        the handle is not be associated to any user */

      pUserInstance = PContextGetCurrentUserInstance(pContext);
      PContextSetCurrentUserInstance(pContext, null);

      PEmulCloseDriver(pContext, pP2PInstance->hP2PTargetConnection, static_PP2PTargetStopDetectionCompleted, null);
      pP2PInstance->hP2PTargetConnection = W_NULL_HANDLE;

      /* restore user */
      PContextSetCurrentUserInstance(pContext, pUserInstance);
   }
   else
   {
      static_PP2PTargetStopDetectionCompleted(pContext, null, W_SUCCESS);
   }
}

/**
 * Stop P2P Target emulation completion callback
 */

static void static_PP2PTargetStopDetectionCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   PDebugTrace("static_PP2PTargetStopDetectionCompleted");

   PP2PInitiatorStopConnection(pContext);
}

/*
 *  P2P target event notification callback
 */

static void static_PP2PTargetEventReceived (
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t nEventCode
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   W_ERROR        nError;
   uint32_t       nActualDataLength;

   PDebugTrace("static_PP2PTargetEventReceived");

   switch (nEventCode)
   {
      case W_EMUL_EVENT_SELECTION :

         PDebugTrace("static_PP2PTargetEventReceived : W_EMUL_EVENT_SELECTION");

         /* Ok, we've detected a P2P initiator, retrieve the peer parameters */

         if ((nError = PEmulGetMessageData(pContext, pP2PInstance->hP2PTargetConnection, pP2PInstance->aBuffer, sizeof (pP2PInstance->aBuffer), &nActualDataLength)) == W_SUCCESS)
         {
            /* Process link activation */
            nError = PLLCPProcessLinkActivation(pContext, LLCP_ROLE_TARGET, pP2PInstance->aBuffer, (uint16_t) nActualDataLength,
                  PP2PLinkDeactivationCallback, pContext);
            PDebugTrace("static_PP2PTargetEventReceived[jim] : PLLCPProcessLinkActivation, nError= %d", nError);
            /* call the link establishment callbacks */
            PP2PCallAllLinkEstablishmentCallbacks(pContext, nError);
            if (nError == W_SUCCESS)
            {
               /* start timer to check the peer is still alive */
               PMultiTimerSet( pContext,
                  TIMER_T9_P2P,
                  2 * pP2PInstance->sLLCPInstance.nRemoteTimeoutMs,      /* *2 for more robusteness */
                  &PP2PRemoteTimeoutExpiry,
                  null );
            }
#if 0			
			else  //jim add
			{
	            /* Unable to retrieve the mandatory peer LLCP parameters, the connection must be aborted */

	            PDebugError("static_PP2PTargetEventReceived[jim] : PEmulGetMessageData nError= %d", nError);

	            /* call the link establishment callbacks */
	            PP2PLinkGeneralFailure(pContext, W_ERROR_TIMEOUT);
	            goto error;			
			}
#endif			
         }
         else
         {
            /* Unable to retrieve the mandatory peer LLCP parameters, the connection must be aborted */

            PDebugError("static_PP2PTargetEventReceived : PEmulGetMessageData failed %d", nError);

            /* call the link establishment callbacks */
            PP2PLinkGeneralFailure(pContext, W_ERROR_TIMEOUT);
            goto error;
         }
         break;

      case W_EMUL_EVENT_DEACTIVATE :

         PDebugTrace("static_PP2PTargetEventReceived : W_EMUL_EVENT_DEACTIVATE");

         /* There's no longer any field */

         pP2PInstance->sLLCPInstance.nRole = LLCP_ROLE_NONE;
         PP2PLinkGeneralFailure(pContext, W_ERROR_TIMEOUT);
         break;

      default :
         PDebugError("static_PP2PTargetEventReceived : unknown event %d", nEventCode);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   return;

error:

   /* something went wrong, abort the connection */

   PDebugTrace("static_PP2PTargetEventReceived : cleanup");

   pP2PInstance->sLLCPInstance.nRole = LLCP_ROLE_NONE;
   PP2PLinkGeneralFailure(pContext, nError);
}

/*
 *  P2P target Emulation command receive callback
 *  Called when a command from P2P initiator has been received
 */

static void static_PP2PTargetCommandReceived (
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t nDataLength
)
{
   tP2PInstance   * pP2PInstance = PContextGetP2PInstance(pContext);
   uint32_t         nActualDataLength;
   tLLCPPDUHeader * pPDU;
   W_ERROR          nError;

   PDebugTrace("static_PP2PTargetCommandReceived");

   /* we've received a PDU from the peer, stop the timer */
   PMultiTimerCancel(pContext, TIMER_T9_P2P);

   /* Processes incoming PDU  */
   pPDU = PLLCPAllocatePDU(pContext);

   if (pPDU == null)
   {
      /* unable to allocate PDU to retrieve incoming data */
      PDebugError("static_PP2PTargetCommandReceived : PLLCPAllocatePDU failed");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   if ((nError = PEmulGetMessageData(pContext, pP2PInstance->hP2PTargetConnection, pPDU->aPayload,
                  LLCP_NUMBERED_PDU_DATA_OFFSET + pP2PInstance->sLLCPInstance.nLocalLinkMIU, &nActualDataLength)) == W_SUCCESS)
   {
      pPDU->nLength = (uint16_t) nActualDataLength;

         /* process the incoming PDU */
      PLLCPProcessIncomingPDU(pContext, pPDU);
   }
   else
   {
      PDebugError("static_PP2PTargetCommandReceived PEmulGetMessageData failed %d\n", nError);
      PLLCPFreePDU(pContext, pPDU);
      goto error;
   }

   /* sends the answer */
   pPDU = PLLCPFramerGetNextXmitPDU(pContext, W_TRUE);

   if (pPDU != null)
   {
      nError = PEmulSendAnswer(pContext, pP2PInstance->hP2PTargetConnection, pPDU->aPayload, pPDU->nLength);

      PLLCPFreePDU(pContext, pPDU);
   }
   else
   {
      PEmulSendAnswer(pContext, pP2PInstance->hP2PTargetConnection, g_aSYMM, sizeof(g_aSYMM));
   }

   /* restart timer to check the peer is still alive */

   PMultiTimerSet( pContext, TIMER_T9_P2P, 2 * pP2PInstance->sLLCPInstance.nRemoteTimeoutMs,      /* *2 for more robusteness */
                     PP2PRemoteTimeoutExpiry, null);
   return;

error :

   PEmulSendAnswer(pContext, pP2PInstance->hP2PTargetConnection, g_aSYMM, sizeof(g_aSYMM));

   PP2PLinkGeneralFailure(pContext, nError);
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

