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
   Contains the declaration of the LLCP PDU encoding / decoding functions
  ****************************************************************************** */

#ifndef __WME_P2P_LLCP_PDU_H
#define __WME_P2P_LLCP_PDU_H

typedef struct __tLLCPPDUHeader tLLCPPDUHeader;

struct __tLLCPPDUHeader
{
   tLLCPPDUHeader     * pNext;
   uint16_t             nLength;
   uint16_t             nDataOffset;
   bool_t                 bAck;
   tDFCCallbackContext  sCallbackContext;
   bool_t                 bIsDiscarded;
   uint8_t              aPayload[1];
};

void PLLCPResetDescriptor(tDecodedPDU * pDecodedPDU);

/**
 * Decodes the received LLCP parameters
 *
 * @param[in]  pParameters         The start address of the parameters
 * @param[in]  nTotalParamLength   The length of the parameters
 * @param[in]  pDecodedPDU          The structure that will contain decoded parameters
 *
 * @return  W_TRUE, W_FALSE
 *
 **/

bool_t PLLCPDecodeParameters(
   uint8_t * pParameters,
   uint16_t nTotalParamLength,
   tDecodedPDU * pDecodedPDU
   );


/**
 * Allocates a PDU
 *
 * @param[in] pContext The context
 *
 * @return  address of the PDU or null
 **/

tLLCPPDUHeader * PLLCPAllocatePDU(
   tContext * pContext
);

/**
 * Frees a PDU
 *
 * @param[in] pContext     The context
 * @param[in] pPDU         The PDU to be freed
 *
 **/

void PLLCPFreePDU(
   tContext       * pContext,
   tLLCPPDUHeader * pPDU
);

/**
 * Builds LLCP PDU
 *
 * @param[in]   pPDUDescriptor The description of PDU to be generated
 * @param[out]  pPDULength     The length of the PDU generated
 * @param[out]  pPDU           The output buffer
 *
 * @return      void
 *
 **/

void PLLCPBuildPDU(
   tDecodedPDU * pPDUDescriptor,
   uint16_t    * pPDULength,
   uint8_t     * pPDU
   );

#endif /* __WME_P2P_LLCP_PDU_H */

/* EOF*/

