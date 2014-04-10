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

#define P_MODULE  P_MODULE_DEC( P2P_LLCP )

#include "wme_context.h"
#include "wme_reader_driver_registry.h"
extern tGerenalByte pGerenalByte;


#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

const uint8_t g_aLLCPMagic[] = { 0x46, 0x66, 0x6D };

   /* Function prototypes */

static bool_t static_PLLCPCheckMagic(
   uint8_t * pRemoteParameters,
   uint16_t nRemoteParametersLength
);

static bool_t static_PLLCPPerformVersionAgreement(
   tLLCPInstance  * pLLCPInstance,
   tDecodedPDU    * pRcvDecodedPDU
);

static void static_PLLCPPerformLinkMIUDetermination(
   tLLCPInstance  * pLLCPInstance,
   tDecodedPDU    * pRcvDecodedPDU
);

/*
 * See header
 */

void PLLCPCreate(tLLCPInstance* pLLCPInstance)
{
   CDebugAssert(pLLCPInstance != null);

   CMemoryFill(pLLCPInstance, 0, sizeof(tLLCPInstance));

   pLLCPInstance->nLocalLinkMIU  = LLCP_DEFAULT_MIU + LLCP_MAX_LOCAL_MIUX;
   pLLCPInstance->nLocalTimeoutMs = 100;  /* Default timeout is 100 ms according to LLCP */
   pLLCPInstance->bAllowInitiatorMode = W_TRUE;
   pLLCPInstance->bAllowActiveMode = W_TRUE;
   pLLCPInstance->bAllowTypeATargetProtocol = W_TRUE;
   pLLCPInstance->nLocalWKS = 0x03;

      /* Initialise the LLCP SAP component */
   PLLCPCreateSAP(& pLLCPInstance->sSAPInstance);

      /* Initialize the LLCP SDP component */
   PLLCPCreateSDP(& pLLCPInstance->sSDPInstance);

      /* Initialize the LLCP Connection component */
   PLLCPCreateCONN(&pLLCPInstance->sCONNInstance);
}


/* See header */
uint8_t PLLCPBuildConfigurationBuffer(
   tContext * pContext,
   uint8_t * pBuffer)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   uint8_t nLength = 0;

   CDebugAssert(pLLCPInstance != null);
   CDebugAssert(pBuffer != null);

      /* LLCP forum magic number */
   pBuffer[nLength++] = 0x46;
   pBuffer[nLength++] = 0x66;
   pBuffer[nLength++] = 0x6D;

      /* LLCP version */
   pBuffer[nLength++] = LLCP_PARAM_VERSION_ID;
   pBuffer[nLength++] = LLCP_PARAM_VERSION_LENGTH;
   pBuffer[nLength++] = (LLCP_VERSION_MAJOR << 4) | (LLCP_VERSION_MINOR);

      /* MIUX */
   pBuffer[nLength++] = LLCP_PARAM_MIUX_ID;
   pBuffer[nLength++] = LLCP_PARAM_MIUX_LENGTH;
   pBuffer[nLength++] = (pLLCPInstance->nLocalLinkMIU - 128) >> 8;
   pBuffer[nLength++] = (pLLCPInstance->nLocalLinkMIU - 128) & 0xFF;

      /* WKS */
   pBuffer[nLength++] = LLCP_PARAM_WKS_ID;
   pBuffer[nLength++] = LLCP_PARAM_WKS_LENGTH;
   pBuffer[nLength++] = (uint8_t) ((pLLCPInstance->nLocalWKS >> 8) & 0xFF);
   pBuffer[nLength++] = (uint8_t) (pLLCPInstance->nLocalWKS & 0xFF);

      /* LTO */
   pBuffer[nLength++] = LLCP_PARAM_LTO_ID;
   pBuffer[nLength++] = LLCP_PARAM_LTO_LENGTH;
   pBuffer[nLength++] = (uint8_t) (pLLCPInstance->nLocalTimeoutMs / 10);

      /* OPT */
   pBuffer[nLength++] = LLCP_PARAM_OPT_ID;
   pBuffer[nLength++] = LLCP_PARAM_OPT_LENGTH;
   pBuffer[nLength++] = 0x03;      /* connectionless and connection oriented transport services */

   return nLength;
}

/*
 * See header
 */

void PLLCPDestroy(
      tLLCPInstance* pLLCPInstance )
{

   CDebugAssert(pLLCPInstance != null);

      /* Destroy the LLCP SAP component */
   PLLCPDestroySAP(& pLLCPInstance->sSAPInstance);

      /* Destroy the LLCP SDP component */
   PLLCPDestroySDP(& pLLCPInstance->sSDPInstance);

      /* Destroy the LLCP CONN component */
   PLLCPDestroyCONN(& pLLCPInstance->sCONNInstance);
}

/*
 * See header
 */

W_ERROR PLLCPProcessLinkActivation (

  tContext                  * pContext,
  uint8_t                      nRole,
  uint8_t                   * pRemoteParameters,
  uint16_t                    nRemoteParametersLength,
  tLinkDeactivationCallback * pLinkDeactivationCallback,
  void *                      pCallbackParameter
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tDecodedPDU sRcvDecodedPDU;

   PDebugTrace("PLLCPProcessLinkActivation");

   CDebugAssert(pLLCPInstance != null);
   CDebugAssert((nRole == LLCP_ROLE_INITIATOR) || (nRole == LLCP_ROLE_TARGET));

   PLLCPResetDescriptor(&sRcvDecodedPDU);

   pLLCPInstance->nRole = LLCP_ROLE_NONE;

   PDebugTrace("PLLCPProcessLinkActivation[jim], nRemoteParametersLength= %d", nRemoteParametersLength);
   if ((nRemoteParametersLength < 4) && (pGerenalByte.nGerenalByteLength > 4))
   {
       PDebugTrace("PLLCPProcessLinkActivation[jim], Assign GB, len=%d", pGerenalByte.nGerenalByteLength);
	   nRemoteParametersLength = pGerenalByte.nGerenalByteLength;
	   pRemoteParameters = &pGerenalByte.aGerenalByte;
   }
   if (nRemoteParametersLength < 4)
   {
      PDebugTrace("PLLCPProcessLinkActivation[jim] retrun W_ERROR_CONNECTION_COMPATIBILITY");      
	  return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   pLLCPInstance->nBaudRate = PUtilReadUint32FromBigEndianBuffer(pRemoteParameters);
   pRemoteParameters += 4;
   nRemoteParametersLength -= 4;

   /* Check PLLCP magic number */

   if (static_PLLCPCheckMagic(pRemoteParameters, nRemoteParametersLength) == W_FALSE)
   {
      PDebugError("PLLCPProcessLinkActivation : bad magic number");
      return (W_ERROR_CONNECTION_COMPATIBILITY);
   }

   /*
    * magic verification passed, decode remote parameters,
    * faking they have been received in a PAX PDU to allow parameter filtering in the decoding function
    */

   sRcvDecodedPDU.nType = LLCP_PDU_PAX;

   if (PLLCPDecodeParameters(pRemoteParameters + sizeof (g_aLLCPMagic),nRemoteParametersLength - sizeof (g_aLLCPMagic), &sRcvDecodedPDU) == W_FALSE)
   {
      /* Parameter decoding failed */
      PDebugError("PLLCPProcessLinkActivation : remote parameter decoding failed");
      return (W_ERROR_CONNECTION_COMPATIBILITY);
   }

   /*
    * Perform Version Number Agreement Procedure
    */

   if (static_PLLCPPerformVersionAgreement(pLLCPInstance, &sRcvDecodedPDU) == W_FALSE)
   {
      PDebugError("PLLCPProcessLinkActivation : version number agreement failed");
      /* Version Number Agreement Procedure failed */
      return (W_ERROR_CONNECTION_COMPATIBILITY);
   }

   /*
    * Perform Link MIU Determination Procedure
    */

   static_PLLCPPerformLinkMIUDetermination(pLLCPInstance, &sRcvDecodedPDU);

   /*
    * Retrieve LLC parameters
    */

   if (sRcvDecodedPDU.sPayload.sParams.sLTO.bIsPresent != W_FALSE)
   {
      pLLCPInstance->nRemoteTimeoutMs = sRcvDecodedPDU.sPayload.sParams.sLTO.nLTO * 10;    /* parameter value is expressed in 10 ms units */
   }
   else
   {
      pLLCPInstance->nRemoteTimeoutMs = LLCP_PARAM_LTO_DEFAULT_VALUE;
   }

   if (sRcvDecodedPDU.sPayload.sParams.sWKS.bIsPresent != W_FALSE)
   {
      pLLCPInstance->nRemoteWKS = sRcvDecodedPDU.sPayload.sParams.sWKS.nWKS;
   }
   else
   {
      /* if not present, consider the only service is LLC Link Management (SAP 0) */
      pLLCPInstance->nRemoteWKS = 0x0001;
   }

      /* Set local LLC role */
   pLLCPInstance->nRole = nRole;

   pLLCPInstance->pLinkDeactivationCallback = pLinkDeactivationCallback;
   pLLCPInstance->pCallbackParameter = pCallbackParameter;

   return (W_SUCCESS);
}

/* see header */
void PLLCPRequestLinkDeactivation (
   tContext        * pContext,
   tPBasicGenericCallbackFunction * pCallback,
   void * pCallbackParameter)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pPDU;
   tDFCCallbackContext sCallback;


   PDebugTrace("PLLCPRequestLinkDeactivation");

   if (pLLCPInstance->nRole != LLCP_ROLE_NONE)
   {
      pPDU = PLLCPAllocatePDU(pContext);

      if (pPDU != null)
      {
         PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

         pLLCPInstance->sXmitPDUDescriptor.nSSAP = LLCP_SIGNALLING_SAP;
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = LLCP_SIGNALLING_SAP;
         pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DISC;

         PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pPDU->nLength, pPDU->aPayload);
         PLLCPFramerEnterXmitPacketWithAck(pContext, pPDU, pCallback, pCallbackParameter);
      }
      else
      {
         /* unable to allocate PDU to send DM */
         PDebugError("PLLCPRequestLinkDeactivation : PLLCPAllocatePDU failed");

         PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallback);
         PDFCPostContext2(& sCallback, W_ERROR_OUT_OF_RESOURCE);
      }
   }
}
/*
 * See header
 */

W_ERROR PLLCPProcessLinkDeactivation (
   tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tDFCCallbackContext sCallback;

   tLLCPPDUHeader * pPDU;

   PDebugTrace("PLLCPProcessLinkDeactivation");

   pLLCPInstance->nRole = LLCP_ROLE_NONE;

      /* Free all remaining PDUs */

   while ((pPDU = PLLCPFramerGetNextXmitPDU(pContext, W_FALSE)) != null)
   {
      PLLCPFreePDU(pContext, pPDU);
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pLLCPInstance->pLinkDeactivationCallback, pLLCPInstance->pCallbackParameter, &sCallback);
   PDFCPostContext1(&sCallback);

   return (W_SUCCESS);
}


/*
 * See header
 */

void PLLCPFramerEnterXmitPacket(
   tContext        * pContext,
   tLLCPPDUHeader  * pXmitPDU
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;

   pXmitPDU->bAck = W_FALSE;
   pXmitPDU->pNext = null;

   if (pLLCPInstance->pLastXmitPDU == null)
   {
      pLLCPInstance->pFirstXmitPDU = pLLCPInstance->pLastXmitPDU = pXmitPDU;
   }
   else
   {
      pLLCPInstance->pLastXmitPDU->pNext = pXmitPDU;
      pLLCPInstance->pLastXmitPDU = pXmitPDU;
   }

   pLLCPInstance->nXmitPDU++;
}

/*
 * See header
 */

void PLLCPFramerEnterXmitPacketWithAck(
   tContext        * pContext,
   tLLCPPDUHeader  * pXmitPDU,
   tPBasicGenericCallbackFunction * pCallback,
   void * pCallbackParameter)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;

   pXmitPDU->bAck = W_TRUE;
   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pXmitPDU->sCallbackContext);

   pXmitPDU->pNext = null;

   if (pLLCPInstance->pLastXmitPDU == null)
   {
      pLLCPInstance->pFirstXmitPDU = pLLCPInstance->pLastXmitPDU = pXmitPDU;
   }
   else
   {
      pLLCPInstance->pLastXmitPDU->pNext = pXmitPDU;
      pLLCPInstance->pLastXmitPDU = pXmitPDU;
   }

   pLLCPInstance->nXmitPDU++;
}



void PLLCPFramerRemoveDiscardedPDU(
   tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pCurrentPDU, * pFirstPDU, * pLastPDU;

   PDebugTrace("PLLCPFramerRemoveDiscardedPDU");

   PDebugTrace("pLLCPInstance->nXmitPDU %d", pLLCPInstance->nXmitPDU);

   pFirstPDU = pLastPDU = null;

   while ((pCurrentPDU = pLLCPInstance->pFirstXmitPDU) != null)
   {
      if (pCurrentPDU->bIsDiscarded == W_FALSE)
      {
         if (pFirstPDU == null)
         {
            pFirstPDU = pLastPDU = pCurrentPDU;
         }
         else
         {
            pLastPDU->pNext = pCurrentPDU;
            pLastPDU = pCurrentPDU;
         }

         pLLCPInstance->pFirstXmitPDU = pCurrentPDU->pNext;
      }
      else
      {
         pLLCPInstance->nXmitPDU--;
         pLLCPInstance->pFirstXmitPDU = pCurrentPDU->pNext;
         PLLCPFreePDU(pContext, pCurrentPDU);
      }
   }

   pLLCPInstance->pFirstXmitPDU = pFirstPDU;
   pLLCPInstance->pLastXmitPDU = pLastPDU;

   PDebugTrace("pLLCPInstance->nXmitPDU %d", pLLCPInstance->nXmitPDU);
}

tLLCPPDUHeader * PLLCPFramerGetNextXmitPDU(
   tContext * pContext,
   bool_t       bPostCallback
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tLLCPPDUHeader * pPDUHeader;
   uint16_t         nAggregatedPDU;

#ifndef LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT
   tLLCPPDUHeader * pPDU;
   uint16_t         nTotalLength;
   uint16_t         nTLVLength;
   uint8_t *        pNextTLV;

#endif /* ifndef LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT */

   /* check parameters */

   CDebugAssert(pLLCPInstance != null);

   if (pLLCPInstance->nXmitPDU == 0)
   {

      return (null);
   }

   /* compute the number of PDU to be aggregated */

#ifdef LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT
   nAggregatedPDU = 1;
#else /* LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT */

   nAggregatedPDU = 0;
   nTotalLength = 0;

   pPDUHeader = pLLCPInstance->pFirstXmitPDU;

   while (pPDUHeader != null)
   {
      nTLVLength = 2 + pPDUHeader->nLength;

      if ((nTotalLength + nTLVLength) > pLLCPInstance->nRemoteLinkMIU)
      {
         /* we've gone beyond the remote link MIU */
         break;
      }

      nTotalLength += nTLVLength;
      nAggregatedPDU++;
      pPDUHeader = pPDUHeader->pNext;
   }

   if (nAggregatedPDU == 0)
   {
      /* this can occur when the first PDU put in an AGF frame does not fit in the remote MIU
         in this case, it will be sent non aggregated */

      nAggregatedPDU = 1;
   }

#endif /* LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT */

#ifndef LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT
   if (nAggregatedPDU == 1)
#endif /* LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT */
   {
         /* only ONE PDU to send */

      pPDUHeader = pLLCPInstance->pFirstXmitPDU;

      pLLCPInstance->pFirstXmitPDU = pPDUHeader->pNext;

      if (pLLCPInstance->pFirstXmitPDU == null)
      {
         pLLCPInstance->pLastXmitPDU = null;
      }

      pLLCPInstance->nXmitPDU--;

      pPDUHeader->pNext = null;

      if (pPDUHeader->bAck)
      {
         PDFCPostContext2(&pPDUHeader->sCallbackContext, W_SUCCESS);
      }

      return pPDUHeader;
   }
#ifndef LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT
   else
   {
      pPDU = PLLCPAllocatePDU(pContext);

      if (pPDU == null)
      {
         return (null);
      }


      /* Build AGF header */

      pPDU->aPayload[0] = 0x00;
      pPDU->aPayload[1] = 0x80;
      nTotalLength += LLCP_UNUMBERED_PDU_DATA_OFFSET;

      pNextTLV = pPDU->aPayload + LLCP_UNUMBERED_PDU_DATA_OFFSET;

      while (nAggregatedPDU--)
      {
         pPDUHeader = pLLCPInstance->pFirstXmitPDU;

         nTLVLength = pPDUHeader->nLength + 2;

         pNextTLV[0] = (pPDUHeader->nLength >> 8);
         pNextTLV[1] = (pPDUHeader->nLength & 0xFF);

         CMemoryCopy(&pNextTLV[2], pPDUHeader->aPayload, pPDUHeader->nLength);

         pNextTLV += nTLVLength;

         /* remove PDU header from the head of the xmit queue */

         pLLCPInstance->pFirstXmitPDU = pPDUHeader->pNext;

         if (pLLCPInstance->pFirstXmitPDU == null)
         {
            pLLCPInstance->pLastXmitPDU = null;
         }

         pLLCPInstance->nXmitPDU--;

         if (pPDUHeader->bAck)
         {
            PDFCPostContext2(&pPDUHeader->sCallbackContext, W_SUCCESS);
         }
         /* free the PDU */
         PLLCPFreePDU(pContext, pPDUHeader);
      }

      pPDU->pNext = null;
      pPDU->nLength = nTotalLength;

      return (pPDU);
   }

#endif /* LLCP_DISABLE_XMIT_AGF_PDU_SUPPORT */
}



/*
 * Performs LLCP Magic Verification
 * See LLCP 6.2.3
 *
 * param[in] - pRemoteParameters address of negociated parameters during Link activation procedure
 *
 * param[in] - nRemoteParametersLength  length of negociated parameters
 *
 * @return  - W_TRUE if Magic verification succeeded.
 *
 */

static bool_t static_PLLCPCheckMagic(uint8_t * pRemoteParameters, uint16_t nRemoteParametersLength)
{
   uint8_t i;

   PDebugTrace("static_PLLCPCheckMagic");

   /*
    * Check if Magic code is present
    */

   if (nRemoteParametersLength < sizeof (g_aLLCPMagic))
   {
      PDebugError("static_PLLCPCheckMagic : Remote Parameters length TOO short");
      return (W_FALSE);
   }

   for (i=0; i<sizeof(g_aLLCPMagic); i++) {

      if (pRemoteParameters[i] != g_aLLCPMagic[i])
      {
         PDebugError("static_PLLCPCheckMagic : Invalid Magic number");
         return (W_FALSE);
      }
   }

   return (W_TRUE);
}

/*
 * Performs LLCP Version Number Agreement Procedure
 * See LLCP 5.2.2
 *
 * @param[in] pLLCPInstance  LLCP instance
 * @param[in] pRcvDecodedPDU Decoded PDU that contains the exchanged parameters
 *
 * @return W_TRUE if Version Agreement Procedure succeeded
 */

static bool_t static_PLLCPPerformVersionAgreement(
      tLLCPInstance  * pLLCPInstance,
      tDecodedPDU    * pRcvDecodedPDU
)
{
   PDebugTrace("static_PLLCPPerformVersionAgreement");

   if (pRcvDecodedPDU->sPayload.sParams.sVersion.bIsPresent == W_FALSE)
   {
      /* The version parameter MUST be presend !*/
      PDebugError("static_PLLCPPerformVersionAgreement : NO VERSION");
      return (W_FALSE);
   }

   if (LLCP_VERSION_MAJOR == pRcvDecodedPDU->sPayload.sParams.sVersion.nMajor)
   {
         /* same Version major, agree on the min version */

      pLLCPInstance->nAgreedVersionMajor = LLCP_VERSION_MAJOR;

#if   LLCP_VERSION_MINOR > 0

      if (LLCP_VERSION_MINOR <= pRcvDecodedPDU->sPayload.sParams.sVersion.nMinor)
      {
         pLLCPInstance->nAgreedVersionMinor = LLCP_VERSION_MINOR;
      }
      else
      {
         pLLCPInstance->nAgreedVersionMinor = pRcvDecodedPDU->sPayload.sParams.sVersion.nMinor;
      }
#else /* LLCP_VERSION_MINOR > 0 */

      pLLCPInstance->nAgreedVersionMinor = LLCP_VERSION_MINOR;

#endif /* LLCP_VERSION_MINOR > 0 */

   }
   else if (LLCP_VERSION_MAJOR < pRcvDecodedPDU->sPayload.sParams.sVersion.nMajor)
   {
      /* Our version major is smaller, let the peer perform version agreement
         for us, the agreed version will be our version (e.g the smallest) */

      pLLCPInstance->nAgreedVersionMajor = LLCP_VERSION_MAJOR;
      pLLCPInstance->nAgreedVersionMinor = LLCP_VERSION_MINOR;

   }
   else
   {
      /* Our version major is greater, we have to decide if we accept the version */
      /* @todo : for now, just reject (only one major version of LLCP at the moment) */

      return (W_FALSE);
   }

   pLLCPInstance->nRemoteVersionMajor = pRcvDecodedPDU->sPayload.sParams.sVersion.nMajor;
   pLLCPInstance->nRemoteVersionMinor = pRcvDecodedPDU->sPayload.sParams.sVersion.nMinor;

   return (W_TRUE);
}


/*
 * Performs Link MIU Determination Procedure
 * See LLCP 5.2.3
 *
 * @param[in] pLLCPInstance  LLCP instance
 *
 * @param[in] pRcvDecodedPDU Decoded PDU that contains the exchanged parameters
 *
 * @return void
 */

static void static_PLLCPPerformLinkMIUDetermination(
   tLLCPInstance  * pLLCPInstance,
   tDecodedPDU    * pRcvDecodedPDU
)
{
   PDebugTrace("static_PLLCPPerformLinkMIUDetermination");

   if (pRcvDecodedPDU->sPayload.sParams.sMIUX.bIsPresent != W_FALSE)
   {
      /* LLCP 4.5.2 */

      /* We only support sending packet up to LLCP_DEFAULT_MIU + LLCP_MAX_REMOTE_MIUX,
         so we troncate the advertized remote MIUX to LLCP_MAX_REMOTE_MIUX */

      if (pRcvDecodedPDU->sPayload.sParams.sMIUX.nMIUX < LLCP_MAX_REMOTE_MIUX)
      {
         pLLCPInstance->nRemoteLinkMIU = LLCP_DEFAULT_MIU + + pRcvDecodedPDU->sPayload.sParams.sMIUX.nMIUX;
      }
      else
      {
         pLLCPInstance->nRemoteLinkMIU = LLCP_DEFAULT_MIU + LLCP_MAX_REMOTE_MIUX;
      }

   }
   else
   {
      pLLCPInstance->nRemoteLinkMIU = LLCP_DEFAULT_MIU;     /* default remote link MIU */
   }
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */
