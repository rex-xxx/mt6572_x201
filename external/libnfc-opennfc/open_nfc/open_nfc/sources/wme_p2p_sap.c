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
   Contains the implementation of the LLCP SAP component
  *******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_SAP )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*
 * See header
 */

W_ERROR PLLCPCreateSAP(tLLCPSAPInstance* pLLCPSAPInstance)
{
   CMemoryFill(pLLCPSAPInstance->aAllocatedSap, 0, sizeof(pLLCPSAPInstance->aAllocatedSap));

   return (W_SUCCESS);
}

/*
 * See header
 */

W_ERROR PLLCPDestroySAP(tLLCPSAPInstance* pLLCPSAPInstance)
{
   CMemoryFill(pLLCPSAPInstance, 0, sizeof(pLLCPSAPInstance));

   return (W_SUCCESS);
}

/*
 * See header
 */

uint8_t PLLCPSAPAllocSap(tLLCPSAPInstance * pLLCPSAPInstance, uint8_t nSapType, uint8_t nSapValue)
{
   uint8_t nSap, nMinSap, nMaxSap, nSapByte, nSapBit;

   PDebugTrace("PLLCPSAPAllocSap");

   /* checks parameter consistency */

   CDebugAssert(pLLCPSAPInstance != null);

   switch (nSapType)
   {
      case SAP_FIXED_VALUE :

         if ((nSapValue < LLCP_MIN_SAP) || (nSapValue > LLCP_MAX_SAP))
         {
            return (0);
         }

         nMinSap = nMaxSap = nSapValue;
         break;

      case SAP_DYNAMIC_SERVER :

         nMinSap = LLCP_DYNAMIC_SERVER_MIN_SAP;
         nMaxSap = LLCP_DYNAMIC_SERVER_MAX_SAP;
         break;

      case SAP_DYNAMIC_CLIENT :

         nMinSap = LLCP_DYNAMIC_CLIENT_MIN_SAP;
         nMaxSap = LLCP_DYNAMIC_CLIENT_MAX_SAP;
         break;

      default :

         PDebugError("PLLCPSAPAllocSap : Unknown nSapType");
         CDebugAssert(W_FALSE);
         return (0);
   }

      /* look for an available SAP in the corresponding range */

   for (nSap = nMinSap; nSap<=nMaxSap; nSap++)
   {
      nSapByte = nSap / 8;
      nSapBit  = nSap - nSapByte * 8;

      if ( (pLLCPSAPInstance->aAllocatedSap[nSapByte] & (1 << nSapBit)) == 0)
      {
         /* this SAP is available, mark it as allocated */
          pLLCPSAPInstance->aAllocatedSap[nSapByte] |= (1 << nSapBit);

          return (nSap);
      }
   }

      /* if we reach this point, we could not allocate a sap */
   return (0);
}

/*
 * See header
 */

void PLLCPSAPFreeSap(tLLCPSAPInstance * pLLCPSAPInstance, uint8_t nSap)
{
   uint8_t nSapByte, nSapBit;

   PDebugTrace("PLLCPSAPFreeSap");

   /* checks parameter consistency */

   CDebugAssert(pLLCPSAPInstance != null);
   CDebugAssert(nSap > LLCP_SDP_SAP);
   CDebugAssert(nSap <= LLCP_MAX_SAP);

   nSapByte = nSap / 8;
   nSapBit  = nSap - nSapByte * 8;

   if ( (pLLCPSAPInstance->aAllocatedSap[nSapByte] & (1 << nSapBit)) != 0)
   {
      /* ok, the SAP was allocated, free it */
      pLLCPSAPInstance->aAllocatedSap[nSapByte] &= ~(1 << nSapBit);
   }
}

#endif   /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

