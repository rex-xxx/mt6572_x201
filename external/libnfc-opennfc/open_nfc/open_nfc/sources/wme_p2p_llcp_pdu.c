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
   Contains the implementation of LLCP PDU related functions
 *******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_LLC )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PLLCPProcessIncomingPDU(tContext * pContext);
static void static_PLLCPProcessSignallingPDU(tContext * pContext);
static void static_PLLCPProcessServiceDiscoveryProtocolPDU(tContext * pContext);
static void static_PLLCPProcessPDU(tContext * pContext);

static bool_t static_PLLCPDecodePDU(uint8_t * pPDU, uint16_t nPDULength, tDecodedPDU * pDecodedPDU);
static uint16_t static_PComputeParameterSize (tDecodedPDU * pPDUDescriptor);
static void static_PBuildPDUHeader(uint8_t * pPDU, uint8_t nType, uint8_t nDSAP, uint8_t nSSAP);
static void static_PBuildParameterTLV(uint8_t * pPDU, tDecodedPDU * pPDUDescriptor);

const char * aPDUNames[] =
{  "SYMM", "PAX", "AGF", "UI", "CONNECT", "DISC", "CC", "DM", "FRMR",
   "SNL", "res_1010", "res_1011", "I", "RR", "RNR", "res_1111"
};

/* See header */
tLLCPPDUHeader * PLLCPAllocatePDU(tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   tLLCPPDUHeader * pPDU;
   uint16_t         nPDULength;

   nPDULength = (uint16_t) (uintptr_t)((tLLCPPDUHeader *)0)->aPayload + (uint16_t)LLCP_NUMBERED_PDU_DATA_OFFSET + LLCP_DEFAULT_MIU + LLCP_MAX_LOCAL_MIUX;

   pPDU = CMemoryAlloc(nPDULength);

   if (pPDU != null)
   {
      CMemoryFill(pPDU, 0, nPDULength);
      pLLCPInstance->nAllocatedPDU++;
   }

   return (pPDU);
}


/* See header */
void PLLCPFreePDU(
   tContext  * pContext,
   tLLCPPDUHeader * pPDU
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   if (pPDU != null)
   {
      pLLCPInstance->nAllocatedPDU--;
      CMemoryFree(pPDU);
   }
}

/* See header */
void PLLCPProcessIncomingPDU(
   tContext        * pContext,
   tLLCPPDUHeader  * pRcvPDU
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   PDebugTrace("PLLCPProcessIncomingPDU");

   if (pLLCPInstance->nRole ==LLCP_ROLE_NONE)
   {
      PLLCPFreePDU(pContext, pRcvPDU);
      return;
   }

   /* Check the PDU correctness */

   if (static_PLLCPDecodePDU(pRcvPDU->aPayload, pRcvPDU->nLength, &pLLCPInstance->sRcvPDUDescriptor) == W_FALSE)
   {
      /* the PDU format was invalid */
      PLLCPFreePDU(pContext, pRcvPDU);
      return;
   }

   if (pLLCPInstance->sRcvPDUDescriptor.nType != LLCP_PDU_AGF)
   {
      pLLCPInstance->pRcvPDU = pRcvPDU;
      static_PLLCPProcessIncomingPDU(pContext);
   }
   else
   {
      /* we received an AGF PDU - split in atomic PDU and process it */

      tLLCPPDUHeader * pTempPDU;
      uint8_t  * pTLV;
      uint16_t   nTLVLength;
      uint16_t   nProcessedLength;

         /* skip the AGF header */

      pTLV = pRcvPDU->aPayload + LLCP_UNUMBERED_PDU_DATA_OFFSET;
      nProcessedLength = LLCP_UNUMBERED_PDU_DATA_OFFSET;

      while (nProcessedLength < pRcvPDU->nLength)
      {
         nTLVLength = (pTLV[0] << 8) + pTLV[1] + 2;

         /* copy the current TLV contents in a temporary PDU */

         pTempPDU = PLLCPAllocatePDU(pContext);

         if (pTempPDU == null)
         {
            PLLCPFreePDU(pContext, pRcvPDU);
            PP2PLinkGeneralFailure(pContext, W_ERROR_OUT_OF_RESOURCE);
            return;
         }

         pTempPDU->nLength = nTLVLength-2;
         CMemoryCopy(pTempPDU->aPayload, &pTLV[2], nTLVLength-2);

         if (static_PLLCPDecodePDU(pTempPDU->aPayload, nTLVLength-2, &pLLCPInstance->sRcvPDUDescriptor) != W_FALSE)
         {
            pLLCPInstance->pRcvPDU = pTempPDU;
            static_PLLCPProcessIncomingPDU(pContext);
         }
         else
         {
            PLLCPFreePDU(pContext, pTempPDU);
         }

         /* Go to next TLV */

         pTLV += nTLVLength;
         nProcessedLength += nTLVLength;
      }

      /* Free the AGF PDU */
      PLLCPFreePDU(pContext, pRcvPDU);
   }
}


static void static_PLLCPProcessIncomingPDU(
   tContext        * pContext
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   switch (pLLCPInstance->sRcvPDUDescriptor.nDSAP)
   {
      case LLCP_SIGNALLING_SAP :
         static_PLLCPProcessSignallingPDU(pContext);
         break;

      case LLCP_SDP_SAP :
         static_PLLCPProcessServiceDiscoveryProtocolPDU(pContext);
         break;

      default :
         static_PLLCPProcessPDU(pContext);
         break;
   }

   if (pLLCPInstance->pRcvPDU != null)
   {
      PLLCPFreePDU(pContext, pLLCPInstance->pRcvPDU);
      pLLCPInstance->pRcvPDU = null;
   }
}


/* See header */
bool_t PLLCPDecodeParameters(
   uint8_t * pParameters,
   uint16_t nTotalParamLength,
   tDecodedPDU * pDecodedPDU
   )
{
   uint8_t * pCurrentParameter;
   uint8_t   nCurrentParameterType;
   uint8_t   nCurrentParameterLength;

   PDebugTrace("PLLCPDecodeParameters");

   CDebugAssert(pParameters != null);
   CDebugAssert(pDecodedPDU != null);

      /* parse the TLV array */

   for (pCurrentParameter = pParameters; nTotalParamLength > 0; )
   {
      if (nTotalParamLength < 2)
      {
         /* not enough remaining bytes for a complete parameter : TLV encoding problem ?*/

         PDebugError("PLLCPDecodeParameters : invalid TLV format");
         return (W_FALSE);
      }

         /* all initial requirements are met, parse the parameter */

      nCurrentParameterType   = pCurrentParameter[LLCP_PARAM_TYPE_OFFSET];
      nCurrentParameterLength = pCurrentParameter[LLCP_PARAM_LENGTH_OFFSET];

      if (nTotalParamLength < (2 + nCurrentParameterLength))
      {
         PDebugError("PLLCPDecodeParameters : invalid TLV format");
         return (W_FALSE);
      }

      switch (nCurrentParameterType)
      {
         case LLCP_PARAM_VERSION_ID :

            if (nCurrentParameterLength == LLCP_PARAM_VERSION_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sVersion.bIsPresent = W_TRUE;

               pDecodedPDU->sPayload.sParams.sVersion.nMajor = (pCurrentParameter[LLCP_PARAM_DATA_OFFSET] >> 4) & 0x0F;
               pDecodedPDU->sPayload.sParams.sVersion.nMinor = pCurrentParameter[LLCP_PARAM_DATA_OFFSET] & 0x0F;

               PDebugTrace("PLLCPDecodeParameters : VERSION %d.%d", pDecodedPDU->sPayload.sParams.sVersion.nMajor, pDecodedPDU->sPayload.sParams.sVersion.nMinor);
            }
            else
            {
                  /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid VERSION parameter length");
            }
            break;

         case LLCP_PARAM_MIUX_ID  :

            if (nCurrentParameterLength == LLCP_PARAM_MIUX_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sMIUX.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sMIUX.nMIUX = ((pCurrentParameter[LLCP_PARAM_DATA_OFFSET] & 0x07) << 8) | pCurrentParameter[LLCP_PARAM_DATA_OFFSET+1];

               PDebugTrace("PLLCPDecodeParameters : MIUX %d", pDecodedPDU->sPayload.sParams.sMIUX.nMIUX);
            }
            else
            {
               /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid MIUX parameter length");
            }
            break;

         case LLCP_PARAM_WKS_ID :

            if (nCurrentParameterLength == LLCP_PARAM_WKS_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sWKS.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sWKS.nWKS = (pCurrentParameter[LLCP_PARAM_DATA_OFFSET] << 8)| pCurrentParameter[LLCP_PARAM_DATA_OFFSET+1];

               PDebugTrace("PLLCPDecodeParameters : WKS %04x", pDecodedPDU->sPayload.sParams.sWKS.nWKS);
            }
            else
            {
                  /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid MKS parameter length");
            }
            break;

         case LLCP_PARAM_LTO_ID :

            if (nCurrentParameterLength == LLCP_PARAM_LTO_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sLTO.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sLTO.nLTO = pCurrentParameter[LLCP_PARAM_DATA_OFFSET];

               PDebugTrace("PLLCPDecodeParameters : LTO %d", pDecodedPDU->sPayload.sParams.sLTO.nLTO);
            }
            else
            {
                  /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid LTO parameter length");
            }
            break;

         case LLCP_PARAM_RW_ID :

            if (nCurrentParameterLength == LLCP_PARAM_RW_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sRW.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sRW.nRW = pCurrentParameter[LLCP_PARAM_DATA_OFFSET] & 0x0F;

               PDebugTrace("PLLCPDecodeParameters : RW %d", pDecodedPDU->sPayload.sParams.sRW.nRW);
            }
            else
            {
                    /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid RW parameter length");
            }
            break;

         case LLCP_PARAM_SN_ID :

            if (nCurrentParameterLength >= LLCP_PARAM_SN_MIN_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sSN.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sSN.nSNLength = nCurrentParameterLength;
               pDecodedPDU->sPayload.sParams.sSN.pSN = pCurrentParameter + LLCP_PARAM_DATA_OFFSET;

               PDebugTrace("PLLCPDecodeParameters : SN :");
               PDebugTraceBuffer(pDecodedPDU->sPayload.sParams.sSN.pSN, pDecodedPDU->sPayload.sParams.sSN.nSNLength);
            }
            else
            {
                    /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid SN parameter length");
            }
            break;

         case LLCP_PARAM_OPT_ID :

            if (nCurrentParameterLength == LLCP_PARAM_OPT_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sOPT.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sOPT.nLSC = pCurrentParameter[LLCP_PARAM_DATA_OFFSET] & 0x03;

               PDebugTrace("PLLCPDecodeParameters : OPT %d :", pDecodedPDU->sPayload.sParams.sOPT.nLSC);
            }
            else
            {
               PDebugError("PLLCPDecodeParameters : invalid OPT parameter length");
            }
            break;

         case LLCP_PARAM_SDREQ_ID :

            if (nCurrentParameterLength >= LLCP_PARAM_SDREQ_MIN_LENGTH + 1 )
            {
               pDecodedPDU->sPayload.sParams.sSDREQ.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sSDREQ.nTID = pCurrentParameter[LLCP_PARAM_DATA_OFFSET];
               pDecodedPDU->sPayload.sParams.sSDREQ.nSNLength = nCurrentParameterLength - 1;
               pDecodedPDU->sPayload.sParams.sSDREQ.pSN = pCurrentParameter + LLCP_PARAM_DATA_OFFSET + 1;

               PDebugTrace("PLLCPDecodeParameters : SDREQ (TID %d - length %d):",pDecodedPDU->sPayload.sParams.sSDREQ.nTID,  pDecodedPDU->sPayload.sParams.sSDREQ.nSNLength );
               PDebugTraceBuffer(pDecodedPDU->sPayload.sParams.sSDREQ.pSN, pDecodedPDU->sPayload.sParams.sSDREQ.nSNLength);
            }
            else
            {
                    /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid SDREQ parameter length");
            }
            break;

         case LLCP_PARAM_SDRES_ID:

            if (nCurrentParameterLength == LLCP_PARAM_SDRES_LENGTH)
            {
               pDecodedPDU->sPayload.sParams.sSDRES.bIsPresent = W_TRUE;
               pDecodedPDU->sPayload.sParams.sSDRES.nTID = pCurrentParameter[LLCP_PARAM_DATA_OFFSET];
               pDecodedPDU->sPayload.sParams.sSDRES.nSAP = pCurrentParameter[LLCP_PARAM_DATA_OFFSET + 1];

               PDebugTrace("PLLCPDecodeParameters : SDRES TID %d - SAP %d:",pDecodedPDU->sPayload.sParams.sSDRES.nTID, pDecodedPDU->sPayload.sParams.sSDRES.nSAP );
            }
            else
            {
                   /* invalid parameter length */
               PDebugError("PLLCPDecodeParameters : invalid SDRES parameter length");
            }
            break;

         default :
            /* invalid parameter */
            PDebugError("PLLCPDecodeParameters : invalid OPT parameter length");
            break;
      }

      pCurrentParameter  += (2 + nCurrentParameterLength);
      nTotalParamLength  -= (2 + nCurrentParameterLength);
   }

      /* filter the parameters according to the PDU type */

   switch (pDecodedPDU->nType)
   {
      case LLCP_PDU_PAX :  /* not supported, but used for negociated parameters parsing */

            /* mark unsupported parameters as not present */
         pDecodedPDU->sPayload.sParams.sRW.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSN.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDREQ.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDRES.bIsPresent = W_FALSE;

         break;

      case LLCP_PDU_CONNECT :

            /* mark unsupported parameters as not present */

         pDecodedPDU->sPayload.sParams.sLTO.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sOPT.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sVersion.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sWKS.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDREQ.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDRES.bIsPresent = W_FALSE;
         break;

      case LLCP_PDU_CC :

            /* mark unsupported parameters as not present */

         pDecodedPDU->sPayload.sParams.sLTO.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sOPT.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSN.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sVersion.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sWKS.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDREQ.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDRES.bIsPresent = W_FALSE;
         break;

      case LLCP_PDU_SNL :

            /* mark unsupported parameters as not present */
         pDecodedPDU->sPayload.sParams.sLTO.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sMIUX.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sOPT.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sRW.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSN.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sVersion.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sWKS.bIsPresent = W_FALSE;
         break;

      default :

            /* mark unsupported parameters as not present */
         pDecodedPDU->sPayload.sParams.sLTO.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sMIUX.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sOPT.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sRW.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSN.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sVersion.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sWKS.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDREQ.bIsPresent = W_FALSE;
         pDecodedPDU->sPayload.sParams.sSDRES.bIsPresent = W_FALSE;
         break;
   }

      /* the parameters have been parsed successfully */
   return (W_TRUE);
}

/* See header */
void PLLCPBuildPDU(
   tDecodedPDU * pPDUDescriptor,
   uint16_t    * pPDULength,
   uint8_t     * pPDU
   )
{
   uint16_t   nPDULength;

   CDebugAssert(pPDULength != null);
   CDebugAssert(pPDUDescriptor!= null);
   CDebugAssert(pPDU != null);

   PDebugTrace("===> %12s (NS:%d/NR:%d) (SSAP %2d:DSAP %2d)", aPDUNames[pPDUDescriptor->nType],
      pPDUDescriptor->nNS, pPDUDescriptor->nNR,
      pPDUDescriptor->nSSAP, pPDUDescriptor->nDSAP);

      /* build the PDU header common to all PDU */

   static_PBuildPDUHeader(pPDU, pPDUDescriptor->nType, pPDUDescriptor->nDSAP, pPDUDescriptor->nSSAP);

   switch (pPDUDescriptor->nType)
   {
      /* ------ unumbered PDU without payload ------ */

      case LLCP_PDU_SYMM :
      case LLCP_PDU_DISC :

         nPDULength = LLCP_UNUMBERED_PDU_MIN_LENGTH;

         break;

         /* -------  unumbered PDU with payload  ------- */

      case LLCP_PDU_PAX :

         /* not supported when using 18092 MAC layer */
         nPDULength = 0;

         CDebugAssert(pPDUDescriptor->nType != LLCP_PDU_PAX);
         break;

      case LLCP_PDU_AGF :

         /* not used */
         nPDULength = 0;
         CDebugAssert(pPDUDescriptor->nType != LLCP_PDU_AGF);
         break;

      case LLCP_PDU_UI :

         nPDULength = LLCP_UNUMBERED_PDU_DATA_OFFSET + pPDUDescriptor->sPayload.sSDU.nInformationLength;

         CMemoryCopy(pPDU + LLCP_UNUMBERED_PDU_DATA_OFFSET, pPDUDescriptor->sPayload.sSDU.pInformation, pPDUDescriptor->sPayload.sSDU.nInformationLength);

         break;

      case LLCP_PDU_CONNECT :
      case LLCP_PDU_CC :
      case LLCP_PDU_SNL :

         nPDULength = LLCP_UNUMBERED_PDU_DATA_OFFSET + static_PComputeParameterSize(pPDUDescriptor);

         static_PBuildParameterTLV(pPDU + LLCP_UNUMBERED_PDU_DATA_OFFSET , pPDUDescriptor);
         break;

      case LLCP_PDU_DM :

         nPDULength = LLCP_DM_PDU_LENGTH;

         pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET] = pPDUDescriptor->sPayload.sDM.nReason;
         break;

      case LLCP_PDU_FRMR :

         nPDULength = LLCP_FRMR_PDU_LENGTH;

         pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET+0] = (pPDUDescriptor->sPayload.sFRMR.nFlag << 4) |(pPDUDescriptor->sPayload.sFRMR.nType);
         pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET+1] = (pPDUDescriptor->sPayload.sFRMR.nNS << 4) |(pPDUDescriptor->sPayload.sFRMR.nNR);
         pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET+2] = (pPDUDescriptor->sPayload.sFRMR.nVS << 4) |(pPDUDescriptor->sPayload.sFRMR.nVR);
         pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET+3] = (pPDUDescriptor->sPayload.sFRMR.nVSA << 4) |(pPDUDescriptor->sPayload.sFRMR.nVRA);

         break;

      case LLCP_PDU_RR :
      case LLCP_PDU_RNR :

         nPDULength = LLCP_NUMBERED_PDU_MIN_LENGTH;

         pPDU[LLCP_NUMBERED_PDU_SEQUENCE_OFFSET] = pPDUDescriptor->nNR;
         break;

         /* -------   Numbered PDU with payload  ------- */
      case LLCP_PDU_I :

         nPDULength = LLCP_NUMBERED_PDU_DATA_OFFSET + pPDUDescriptor->sPayload.sSDU.nInformationLength;

         pPDU[LLCP_NUMBERED_PDU_SEQUENCE_OFFSET] = (pPDUDescriptor->nNS << 4) | (pPDUDescriptor->nNR);
         CMemoryCopy(pPDU + LLCP_NUMBERED_PDU_DATA_OFFSET, pPDUDescriptor->sPayload.sSDU.pInformation, pPDUDescriptor->sPayload.sSDU.nInformationLength);
         break;

      default :

         /* should not occur */
         CDebugAssert(W_FALSE);

         nPDULength = 0;
         break;

   }

   * pPDULength = nPDULength;
}

void PLLCPResetDescriptor(tDecodedPDU * pDecodedPDU)
{
   CMemoryFill(pDecodedPDU, 0, sizeof(*pDecodedPDU));
}

/**
 * Decodes the received PDU
 *
 * @param[in]  pPDU         The start address of the PDU.
 * @param[in]  nPDULength   The length of the PDU.
 * @param[in]  pDecodedPDU  The structure that will contain the decoded PDU
 *
 * @return  W_TRUE, W_FALSE
 *
 **/

static bool_t static_PLLCPDecodePDU(uint8_t * pPDU, uint16_t nPDULength, tDecodedPDU * pDecodedPDU)
{
   bool_t bValid = W_FALSE;    /* the bValue will be set to W_TRUE once all checks passed */

   PDebugTrace("static_PLLCPDecodePDU");

   CDebugAssert(pPDU != null);
   CDebugAssert(pDecodedPDU != null);

      /* reset the decoded PDU structure */
   PLLCPResetDescriptor(pDecodedPDU);

   if (nPDULength < LLCP_UNUMBERED_PDU_MIN_LENGTH) {

      /* The PDU is too short, we can not even decode its type, SSAP and DSAP, simply drop it */
      return (W_FALSE);
   }

   pDecodedPDU->nDSAP = (pPDU[0] >> 2) & 0x3F;
   pDecodedPDU->nType = ((pPDU[0] & 0x3) << 2) | ((pPDU[1] >> 6) & 0x3);
   pDecodedPDU->nSSAP = pPDU[1] & 0x3F;

   switch (pDecodedPDU->nType)
   {
      case LLCP_PDU_SYMM :

         if ((nPDULength == LLCP_UNUMBERED_PDU_MIN_LENGTH) &&
             (pDecodedPDU->nDSAP == LLCP_SIGNALLING_SAP)   &&
             (pDecodedPDU->nSSAP == LLCP_SIGNALLING_SAP))
         {
             bValid = W_TRUE;
         }
         break;

      case LLCP_PDU_PAX :

         bValid = W_FALSE;      /* PAX PDU is not supported when using the 18092 binding */
         break;

      case LLCP_PDU_AGF :

         if ((pDecodedPDU->nDSAP == LLCP_SIGNALLING_SAP) && (pDecodedPDU->nSSAP == LLCP_SIGNALLING_SAP))
         {
            /* payload content will be checked once splitted */
            bValid = W_TRUE;
         }
         break;

      case LLCP_PDU_UI :

         pDecodedPDU->sPayload.sSDU.pInformation = pPDU + LLCP_UNUMBERED_PDU_DATA_OFFSET;
         pDecodedPDU->sPayload.sSDU.nInformationLength = nPDULength - LLCP_UNUMBERED_PDU_DATA_OFFSET;

         bValid = W_TRUE;
         break;

      case LLCP_PDU_DISC :

         bValid = (nPDULength == LLCP_UNUMBERED_PDU_MIN_LENGTH) ? W_TRUE : W_FALSE;
         /* Work around for Nexus DISC PDU)  */
         bValid = (nPDULength >= LLCP_UNUMBERED_PDU_MIN_LENGTH) ? W_TRUE : W_FALSE;

         break;

      case LLCP_PDU_CONNECT :
      case LLCP_PDU_CC :

         if (nPDULength >= LLCP_UNUMBERED_PDU_MIN_LENGTH) {

               /* parse the parameters */
            bValid = PLLCPDecodeParameters(pPDU + LLCP_UNUMBERED_PDU_DATA_OFFSET, nPDULength - LLCP_UNUMBERED_PDU_DATA_OFFSET, pDecodedPDU);
         }

         break;

      case LLCP_PDU_DM :

         if (nPDULength == LLCP_DM_PDU_LENGTH)
         {
            /* check the DM reason */

            switch (pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET])
            {
               case LLCP_DM_REASON_DISC_CONFIRMED :
               case LLCP_DM_REASON_NO_ACTIVE_CONNECTION :
               case LLCP_DM_REASON_NO_SERVICE :
               case LLCP_DM_REASON_REJECTED :
               case LLCP_DM_REASON_PERMANENT_DSAP_REJECT :
               case LLCP_DM_REASON_PERMANENT_REJECT :
               case LLCP_DM_REASON_TEMPORARY_DSAP_REJECT :
               case LLCP_DM_REASON_TEMPORARY_REJECT :

                  /* all this disconnected mode reasons are valid */
                  pDecodedPDU->sPayload.sDM.nReason = pPDU[LLCP_UNUMBERED_PDU_DATA_OFFSET];
                  bValid = W_TRUE;
                  break;

               default :

                  PDebugError("static_PLLCPDecodePDU : invalid DISC reason %d", pPDU[2]);

                  /* an invalid DM reason should be interpreted as DM_REASON_DISC_CONFIRMED */
                  pDecodedPDU->sPayload.sDM.nReason = LLCP_DM_REASON_DISC_CONFIRMED;

                  bValid = W_TRUE;
                  break;
            }
         }

         break;

      case LLCP_PDU_FRMR :

         if (nPDULength == LLCP_FRMR_PDU_LENGTH)
         {
            bValid = W_TRUE;
         }
         break;

      case LLCP_PDU_I :

         if (nPDULength >= LLCP_NUMBERED_PDU_MIN_LENGTH)
         {
            pDecodedPDU->nNS = (pPDU[LLCP_NUMBERED_PDU_SEQUENCE_OFFSET] >> 4) & 0x0F;
            pDecodedPDU->nNR = pPDU[LLCP_NUMBERED_PDU_SEQUENCE_OFFSET] & 0x0F;
            pDecodedPDU->sPayload.sSDU.pInformation = pPDU + LLCP_NUMBERED_PDU_DATA_OFFSET;
            pDecodedPDU->sPayload.sSDU.nInformationLength = nPDULength - LLCP_NUMBERED_PDU_DATA_OFFSET;

            bValid = W_TRUE;
         }
         break;


      case LLCP_PDU_RR  :
      case LLCP_PDU_RNR :

         if (nPDULength == LLCP_NUMBERED_PDU_MIN_LENGTH)
         {
            pDecodedPDU->nNS = (pPDU[LLCP_NUMBERED_PDU_SEQUENCE_OFFSET] >> 4) & 0x0F;
            pDecodedPDU->nNR = pPDU[LLCP_NUMBERED_PDU_SEQUENCE_OFFSET] & 0x0F;

            if (pDecodedPDU->nNS != 0)
            {
               PDebugError("static_PLLCPDecodePDU : invalid NS %d", pDecodedPDU->nNS);
               pDecodedPDU->nNS = 0;
            }

            bValid = W_TRUE;
         }
         break;

      case LLCP_PDU_SNL:

         if (nPDULength >= LLCP_UNUMBERED_PDU_MIN_LENGTH) {

               /* parse the parameters */
            bValid = PLLCPDecodeParameters(pPDU + LLCP_UNUMBERED_PDU_DATA_OFFSET, nPDULength - LLCP_UNUMBERED_PDU_DATA_OFFSET, pDecodedPDU);
         }

         break;

      default :

            /* Well formered error */
         pDecodedPDU->sPayload.sFRMR.nFlag |= LLCP_FRMR_FLAG_W;
         break;
   }

   PDebugTrace("<=== %12s (NS:%d/NR:%d) (SSAP %2d:DSAP %2d)", aPDUNames[pDecodedPDU->nType],
      pDecodedPDU->nNS, pDecodedPDU->nNR,
      pDecodedPDU->nSSAP, pDecodedPDU->nDSAP);

   return (bValid);
}

/**
 * Computes the required size to store parameters
 *
 * @param[in]  pPDUDescriptor  The PDU descriptor.
 *
 * @return  The size needed.
 *
 **/

static uint16_t static_PComputeParameterSize (
   tDecodedPDU * pPDUDescriptor
)
{
   uint16_t nLength = 0;

   PDebugTrace("static_PComputeParameterSize");

   CDebugAssert(pPDUDescriptor != null);

   if (pPDUDescriptor->sPayload.sParams.sVersion.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_VERSION_LENGTH;
   }

   if (pPDUDescriptor->sPayload.sParams.sMIUX.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_MIUX_LENGTH;
   }

   if (pPDUDescriptor->sPayload.sParams.sWKS.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_WKS_LENGTH;
   }

   if (pPDUDescriptor->sPayload.sParams.sLTO.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_LTO_LENGTH;
   }

   if (pPDUDescriptor->sPayload.sParams.sRW.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_RW_LENGTH;
   }

   if (pPDUDescriptor->sPayload.sParams.sSN.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET;
      nLength += pPDUDescriptor->sPayload.sParams.sSN.nSNLength;
   }

   if (pPDUDescriptor->sPayload.sParams.sOPT.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_OPT_LENGTH;
   }

   if (pPDUDescriptor->sPayload.sParams.sSDREQ.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + 1;
      nLength += pPDUDescriptor->sPayload.sParams.sSDREQ.nSNLength;
   }

   if (pPDUDescriptor->sPayload.sParams.sSDRES.bIsPresent != W_FALSE)
   {
      nLength += LLCP_PARAM_DATA_OFFSET + LLCP_PARAM_SDRES_LENGTH;
   }
   return (nLength);
}


/**
 * Builds LLCP PDU header
 *
 * param[in] pPDU Start address of the memory area where TLV will be built
 *
 * param[in] nType Type of PDU
 *
 * param[in] nDSAP Destination SAP
 *
 * param[in] nSSAP Source SAP
 *
 * @return  void
 *
 **/

static void static_PBuildPDUHeader(
      uint8_t * pPDU,
      uint8_t nType,
      uint8_t nDSAP,
      uint8_t nSSAP
)
{
   PDebugTrace("static_PBuildPDUHeader");

   CDebugAssert(pPDU != null);

   pPDU[0] = (nDSAP << 2) | (nType >> 2);
   pPDU[1] = ((nType & 0x03) << 6) |nSSAP;
}

/**
  * Builds PDU parameter TLV
  *
  * param[in] pPDU Start address of the memory area where TLV will be built
  *
  * param[in] pPDUDescriptor Address of PDU descriptor
  *
  */

static void static_PBuildParameterTLV(
   uint8_t * pPDU,
   tDecodedPDU * pPDUDescriptor
)
{
   PDebugTrace("static_PBuildParameterTLV");

   CDebugAssert(pPDU != null);
   CDebugAssert(pPDUDescriptor != null);

   if (pPDUDescriptor->sPayload.sParams.sVersion.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_VERSION_ID;
      * pPDU++ = LLCP_PARAM_VERSION_LENGTH;
      * pPDU++ = (pPDUDescriptor->sPayload.sParams.sVersion.nMajor << 4) | pPDUDescriptor->sPayload.sParams.sVersion.nMinor;
   }

   if (pPDUDescriptor->sPayload.sParams.sMIUX.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_MIUX_ID;
      * pPDU++ = LLCP_PARAM_MIUX_LENGTH;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sMIUX.nMIUX >> 8;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sMIUX.nMIUX & 0xFF;
   }

   if (pPDUDescriptor->sPayload.sParams.sWKS.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_WKS_ID;
      * pPDU++ = LLCP_PARAM_WKS_LENGTH;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sWKS.nWKS >> 8;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sWKS.nWKS & 0xFF;
   }

   if (pPDUDescriptor->sPayload.sParams.sLTO.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_LTO_ID;
      * pPDU++ = LLCP_PARAM_LTO_LENGTH;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sLTO.nLTO;
   }

   if (pPDUDescriptor->sPayload.sParams.sRW.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_RW_ID;
      * pPDU++ = LLCP_PARAM_RW_LENGTH;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sRW.nRW;
   }

   if (pPDUDescriptor->sPayload.sParams.sSN.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_SN_ID;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sSN.nSNLength;

      CMemoryCopy(pPDU, pPDUDescriptor->sPayload.sParams.sSN.pSN, pPDUDescriptor->sPayload.sParams.sSN.nSNLength);
   }

   if (pPDUDescriptor->sPayload.sParams.sOPT.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_OPT_ID;
      * pPDU++ = LLCP_PARAM_OPT_LENGTH;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sOPT.nLSC;
   }

   if (pPDUDescriptor->sPayload.sParams.sSDREQ.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_SDREQ_ID;
      * pPDU++ = 1 + pPDUDescriptor->sPayload.sParams.sSDREQ.nSNLength;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sSDREQ.nTID;

      CMemoryCopy(pPDU, pPDUDescriptor->sPayload.sParams.sSDREQ.pSN, pPDUDescriptor->sPayload.sParams.sSDREQ.nSNLength);
   }

   if (pPDUDescriptor->sPayload.sParams.sSDRES.bIsPresent != W_FALSE)
   {
      * pPDU++ = LLCP_PARAM_SDRES_ID;
      * pPDU++ = LLCP_PARAM_SDRES_LENGTH;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sSDRES.nTID;
      * pPDU++ = pPDUDescriptor->sPayload.sParams.sSDRES.nSAP;
   }
}

/**
  * Processes incoming PDU receveived on Signalling SAP (0)
  *
  * param[in] pLLPInstance The LLCP Instance
  *
  */

void static_PLLCPProcessSignallingPDU(
   tContext        * pContext
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   tLLCPPDUHeader * pXmitPDU;

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_SYMM :
      case LLCP_PDU_PAX :
      case LLCP_PDU_AGF :

         /* no processing for these PDU */
         break;

      case LLCP_PDU_UI :

         /* Unnumbered Information on Signalling service is not allowed */
         /* simply ignore it */
         break;

      case LLCP_PDU_DISC :

         if (pLLCPInstance->sRcvPDUDescriptor.nSSAP == LLCP_SIGNALLING_SAP)
         {
            /* LLCP link deactivation procedure */
            PLLCPProcessLinkDeactivation(pContext);
            break;
         }

         /* fall though the error case */

      default :

         /* Signalling SAP should never be used for connection */
         PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

         pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
         pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
         pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_NO_ACTIVE_CONNECTION;

         pXmitPDU = pLLCPInstance->pRcvPDU;
         pLLCPInstance->pRcvPDU = null;      /* is now pXmitPDU */

         PLLCPBuildPDU(& pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
         PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

         break;
   }
}

/**
  * Processes incoming PDU receveived on Service Discovery Protocol SAP
  *
  * param[in] pLLPInstance The LLCP Instance
  *
  */

void static_PLLCPProcessServiceDiscoveryProtocolPDU(
   tContext        * pContext
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   tLLCPPDUHeader * pXmitPDU;
   uint8_t nSAP;

   PDebugTrace("static_PLLCPProcessServiceDiscoveryProtocolPDU");

   switch (pLLCPInstance->sRcvPDUDescriptor.nType)
   {
      case LLCP_PDU_CONNECT :

         PDebugTrace("static_PLLCPProcessServiceDiscoveryProtocolPDU : CONNECT");

         if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSN.bIsPresent != W_FALSE)
         {
            /* We received a CONNECT with a specified Service Name,
               check if this service is known in the SDP */

            nSAP = PLLCPSDPLookupServiceFromURI(
                        &pLLCPInstance->sSDPInstance,
                        pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSN.pSN,
                        pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSN.nSNLength);

            if (nSAP != 0)
            {
               /* OK, the requested service has been registered,
                  update the DSAP in the packet and go on with the standard automaton */

               pLLCPInstance->sRcvPDUDescriptor.nDSAP = nSAP;
               static_PLLCPProcessPDU(pContext);
               break;
            }
         }

         /* If we reach this point, the CONNECT has been rejected */

         PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

         pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
         pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_NO_SERVICE;

         pXmitPDU = pLLCPInstance->pRcvPDU;
         pLLCPInstance->pRcvPDU = null;

         PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
         PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

         break;


      case LLCP_PDU_SNL :

         PDebugTrace("static_PLLCPProcessServiceDiscoveryProtocolPDU : SNL");

         if ((pLLCPInstance->nAgreedVersionMajor == LLCP_VERSION_11_MAJOR) &&
             (pLLCPInstance->nAgreedVersionMinor >= LLCP_VERSION_11_MINOR))
         {
            /* Ok, we aggreed on LLCP 1.1, must process the PDU */

            /* Process SDREQ if any */

            if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDREQ.bIsPresent != W_FALSE)
            {
               PDebugTrace("static_PLLCPProcessServiceDiscoveryProtocolPDU : %p %d", pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDREQ.pSN, pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDREQ.nSNLength);

               nSAP = PLLCPSDPLookupServiceFromURI(
                        &pLLCPInstance->sSDPInstance,
                        pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDREQ.pSN,
                        pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDREQ.nSNLength);

               PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

               pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_SNL;
               pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
               pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;

               pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSDRES.bIsPresent = W_TRUE;
               pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSDRES.nTID = pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDREQ.nTID;
               pLLCPInstance->sXmitPDUDescriptor.sPayload.sParams.sSDRES.nSAP = nSAP;

               pXmitPDU = pLLCPInstance->pRcvPDU;
               pLLCPInstance->pRcvPDU = null;

               PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
               PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);
            }


            /* Process SDRES if any */

            if (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDRES.bIsPresent != W_FALSE)
            {
               /* process a name resolution response */

               if ( (pLLCPInstance->bURILookupInProgress != W_FALSE)
                    && (pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDRES.nTID == pLLCPInstance->nCurrentTID))
               {
                  PDFCDriverPostCC3(pLLCPInstance->pURILookupCC, (uint32_t) pLLCPInstance->sRcvPDUDescriptor.sPayload.sParams.sSDRES.nSAP, W_SUCCESS);

                  pLLCPInstance->bURILookupInProgress = W_FALSE;
                  pLLCPInstance->nCurrentTID++;
               }
            }
         }
         else
         {
            PDebugError("Received a SNL PDU on a LLCP 1.0 connection !!!");
         }

         break;

      case LLCP_PDU_UI :

         /* Unnumbered Information on SDP service ? */
         /* FIXME what it is supposed to mean ? for now, simply ignore it */

         break;


      default :

         /* We never establish a direct connection on SDP SAP, so we should not receive connection related PDUs */
         PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

         pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
         pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
         pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
         pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_NO_ACTIVE_CONNECTION;

         pXmitPDU = pLLCPInstance->pRcvPDU;
         pLLCPInstance->pRcvPDU = null;

         PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
         PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

         break;
   }
}

void static_PLLCPProcessPDU(
   tContext        * pContext
)
{
   tP2PInstance  * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tP2PSocket    * pP2PSocket;
   tLLCPPDUHeader  * pXmitPDU;

   if (pLLCPInstance->sRcvPDUDescriptor.nType == LLCP_PDU_UI)
   {
      /* UI PDU is not related to any connection */
      uint32_t      nBytes = pLLCPInstance->sRcvPDUDescriptor.sPayload.sSDU.nInformationLength;
      uint8_t       nSap = pLLCPInstance->sRcvPDUDescriptor.nDSAP;

      /* search a matching connection-less socket */

      for (pP2PSocket = pP2PInstance->pFirstSocket; pP2PSocket != null; pP2PSocket = pP2PSocket->pNext)
      {
         if ((pP2PSocket->sConfig.nType == W_P2P_TYPE_CONNECTIONLESS) &&
             (pP2PSocket->sConfig.nLocalSap == nSap))
         {
            if (pP2PSocket->pRecvBuffer != null)
            {
               if (pP2PSocket->nRecvBufferLength >= nBytes)
               {
                  CMemoryCopy(pP2PSocket->pRecvBuffer, pLLCPInstance->sRcvPDUDescriptor.sPayload.sSDU.pInformation, pLLCPInstance->sRcvPDUDescriptor.sPayload.sSDU.nInformationLength);

                  PP2PCallSocketReadCallback(pContext, pP2PSocket, nBytes, W_SUCCESS, pLLCPInstance->sRcvPDUDescriptor.nSSAP);
               }
               else
               {
                  PP2PCallSocketReadCallback(pContext, pP2PSocket, nBytes, W_ERROR_BUFFER_TOO_SHORT, 0);
               }
            }
            else
            {
               /* there's no recv operation on the socket, store the PDU for later processing
                  the pending PDU are periodically flushed to avoid resource consumption */

               if (pP2PSocket->pLastRecvPDU != null)
               {
                  pP2PSocket->pLastRecvPDU->pNext = pLLCPInstance->pRcvPDU;
                  pP2PSocket->pLastRecvPDU = pLLCPInstance->pRcvPDU;
               }
               else
               {
                  pP2PSocket->pFirstRecvPDU = pP2PSocket->pLastRecvPDU = pLLCPInstance->pRcvPDU;
               }

               pLLCPInstance->pRcvPDU = null;
            }

            break;
         }
      }

      if (pP2PSocket == null)
      {
         PDebugError("static_PLLCPProcessPDU : no connectionless socket matching this UI. Drop it");
      }
   }
   else
   {
      tLLCPConnection * pConnection;

      /* Other PDU types are associated to a connection */

      pConnection = PLLCPConnectionAccessContext(pContext,
                                                  pLLCPInstance->sRcvPDUDescriptor.nDSAP,
                                                  pLLCPInstance->sRcvPDUDescriptor.nSSAP);

      if (pConnection == null)
      {

         /* if we received a CONNECT, check if a service is listening on this SAP.
            if so, allocate a new connection */

         if (pLLCPInstance->sRcvPDUDescriptor.nType == LLCP_PDU_CONNECT)
         {
            /* if PDU is CONNECT, check if a service is registered on the DSAP */

            for (pP2PSocket = pP2PInstance->pFirstSocket; pP2PSocket != null; pP2PSocket = pP2PSocket->pNext)
            {
               if ( (pP2PSocket->sConnection.pConnection == null) &&
                    (pP2PSocket->sConfig.nLocalSap == pLLCPInstance->sRcvPDUDescriptor.nDSAP) &&
                    ((pP2PSocket->sConfig.nType == W_P2P_TYPE_SERVER) || (pP2PSocket->sConfig.nType == W_P2P_TYPE_CLIENT_SERVER))
                    )
               {
                  break;
               }
            }

            if (pP2PSocket != null)
            {
               /* found a service listening, create a connection  */

               pConnection = PLLCPConnectionAllocContext(pContext, pLLCPInstance->sRcvPDUDescriptor.nDSAP, pLLCPInstance->sRcvPDUDescriptor.nSSAP, pP2PSocket);

               if (pConnection == null)
               {
                  /* Could not allocate association context */
                  PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

                  pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
                  pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
                  pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
                  pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_TEMPORARY_DSAP_REJECT;

                  pXmitPDU = pLLCPInstance->pRcvPDU;
                  pLLCPInstance->pRcvPDU = null;

                  PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
                  PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

                  return;
               }
            }
            else
            {
               /* no one is listening, reject the incoming connection */
               PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

               pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
               pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
               pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
               pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_NO_SERVICE;

               pXmitPDU = pLLCPInstance->pRcvPDU;
               pLLCPInstance->pRcvPDU = null;

               PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
               PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);

               return;
            }
         }
      }

      if (pConnection != null)
      {
         /* found a matching connection, call the connection state machine */

         PLLCPConnectionStateMachine(pContext, pConnection);
      }
      else
      {
         if (pLLCPInstance->sRcvPDUDescriptor.nType != LLCP_PDU_DM)
         {

            /* received a connection related PDU without corresponding connection */
            PLLCPResetDescriptor(&pLLCPInstance->sXmitPDUDescriptor);

            pLLCPInstance->sXmitPDUDescriptor.nType = LLCP_PDU_DM;
            pLLCPInstance->sXmitPDUDescriptor.nSSAP = pLLCPInstance->sRcvPDUDescriptor.nDSAP;
            pLLCPInstance->sXmitPDUDescriptor.nDSAP = pLLCPInstance->sRcvPDUDescriptor.nSSAP;
            pLLCPInstance->sXmitPDUDescriptor.sPayload.sDM.nReason = LLCP_DM_REASON_NO_ACTIVE_CONNECTION;

            pXmitPDU = pLLCPInstance->pRcvPDU;
            pLLCPInstance->pRcvPDU = null;

            PLLCPBuildPDU(&pLLCPInstance->sXmitPDUDescriptor, &pXmitPDU->nLength, pXmitPDU->aPayload);
            PLLCPFramerEnterXmitPacket(pContext, pXmitPDU);
         }
      }
   }
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

/* EOF */

