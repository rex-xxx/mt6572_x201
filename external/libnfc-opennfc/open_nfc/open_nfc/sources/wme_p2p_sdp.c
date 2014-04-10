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
   Contains the implementation of the LLCP Service Discovery Protocol component
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_SDP )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


typedef struct __tLLCPWellKnownServiceDescriptor
{
   uint32_t        nServiceLength;   /**< URI length in bytes*/
   const uint8_t * pServiceURI;      /**< URI associated to the service */

   uint8_t   nSap;               /**< SAP associated to this service */

} tLLCPWellKnownServiceDescriptor;


const uint8_t g_aWellKnownSDP[]  = "urn:nfc:sn:sdp";
const uint8_t g_aWellKnownIP[]   = "urn:nfc:sn:ip";
const uint8_t g_aWellKnownOBEX[] = "urn:nfc:sn:obex";
const uint8_t g_aWellKnownSNEP[] = "urn:nfc:sn:snep";

static const tLLCPWellKnownServiceDescriptor g_aWellKnownServiceTable[] =
{
   { sizeof(g_aWellKnownSDP),   g_aWellKnownSDP,    1  },
   { sizeof(g_aWellKnownIP),    g_aWellKnownIP,     2  },
   { sizeof(g_aWellKnownOBEX),  g_aWellKnownOBEX,   3  },
   { sizeof(g_aWellKnownSNEP),   g_aWellKnownSNEP,   4  }
};


static tLLCPSdpServiceDescriptor * static_PLLCPSDPAllocServiceDescriptor(void);
static void static_PLLCPSDPFreeServiceDescriptor(tLLCPSdpServiceDescriptor * pDescriptor);

/* See header */
W_ERROR PLLCPCreateSDP(tLLCPSDPInstance * pSDPInstance)
{
   pSDPInstance->pDescriptorList = null;

   return (W_SUCCESS);
}

/* See header */
W_ERROR PLLCPDestroySDP(tLLCPSDPInstance * pSDPInstance)
{
   CMemoryFill(pSDPInstance, 0, sizeof(tLLCPSDPInstance));

   return (W_SUCCESS);
}

/* See header */
W_ERROR PLLCPSDPRegisterService(
   tLLCPSDPInstance *                  pSDPInstance,
   uint8_t                             nServiceSap,
   const uint8_t *                     pUTF8ServiceURI,
   uint32_t                            nServiceURILength
)
{
   tLLCPSdpServiceDescriptor * pDescriptor;

   PDebugTrace("PLLCPSDPRegisterService");

   /* check parameters consistency */

   CDebugAssert(pSDPInstance != null);
   CDebugAssert(pUTF8ServiceURI != null);
   CDebugAssert((nServiceSap != LLCP_SIGNALLING_SAP) && (nServiceSap != LLCP_SDP_SAP) && (nServiceSap <= LLCP_MAX_SAP));


#if 0    /* @todo useless since we perfom the test before */

   /* check if the service is not already registered */
   for (pDescriptor = pSDPInstance->pDescriptorList; pDescriptor != null; pDescriptor = pDescriptor->pNext)
   {
      if (pDescriptor->nSap == nServiceSap)
      {
         /* the SAP is already in use ! */
         PDebugError("PLLCPSDPRegisterService : nServiceSap %d already in use", pDescriptor->nSap);
         return (W_ERROR_BAD_PARAMETER);
      }

      if (PUtilStringCompare(pDescriptor->pServiceURI, pServiceURI) == 0)
      {
         /* the URI is already registered ! */
         PDebugError("PLLCPSDPRegisterService : pServiceURI already in use (sap %d)", pDescriptor->nSap);
         return (W_ERROR_BAD_PARAMETER);
      }
   }

#endif /* 0 */

   /* allocate a new service descriptor */

   pDescriptor = static_PLLCPSDPAllocServiceDescriptor();

   if (pDescriptor == null)
   {
      PDebugError("PLLCPSDPRegisterService : static_PLLCPSDPAllocServiceDescriptor returned null");
      return (W_ERROR_OUT_OF_RESOURCE);
   }

      /* store the Service URI value in the descriptor. */

   pDescriptor->pUTF8ServiceURI = CMemoryAlloc(nServiceURILength);

   if (pDescriptor->pUTF8ServiceURI == null) {

      PDebugError("PLLCPSDPRegisterService : CMemoryAlloc returned null");
      CMemoryFree(pDescriptor);

      return (W_ERROR_OUT_OF_RESOURCE);
   }

   /* copy the service URI in the descriptor */

   CMemoryCopy(pDescriptor->pUTF8ServiceURI, pUTF8ServiceURI, nServiceURILength);
   pDescriptor->nServiceURILength = nServiceURILength;

   /* set the service SAP */

   pDescriptor->nSap = nServiceSap;

   /* insert the descriptor in the list descriptors */

   pDescriptor->pNext = pSDPInstance->pDescriptorList;
   pSDPInstance->pDescriptorList = pDescriptor;

   return (W_SUCCESS);
}

/* See header */
W_ERROR PLLCPSDPUnregisterService(
   tLLCPSDPInstance *                  pLLCPSDPInstance,
   uint8_t                             nServiceSap
)
{
   tLLCPSdpServiceDescriptor * pDescriptor;
   tLLCPSdpServiceDescriptor * pPriorDescriptor;

   PDebugTrace("PLLCPSDPUnregisterService");

   /* check parameters consistency */

   CDebugAssert(pLLCPSDPInstance != null);
   CDebugAssert((nServiceSap != LLCP_SIGNALLING_SAP) && (nServiceSap != LLCP_SDP_SAP) && (nServiceSap <= LLCP_MAX_SAP));

   /* walk through the registered services list */

   for (pPriorDescriptor = null, pDescriptor = pLLCPSDPInstance->pDescriptorList; pDescriptor != null;)
   {
      if (pDescriptor->nSap == nServiceSap)
      {
            /* the service has been found */

         if (pPriorDescriptor != null)
         {
            pPriorDescriptor->pNext = pDescriptor->pNext;
         } else
         {
            pLLCPSDPInstance->pDescriptorList = pDescriptor->pNext;
         }

         static_PLLCPSDPFreeServiceDescriptor(pDescriptor);
         return (W_SUCCESS);
      }

      pPriorDescriptor = pDescriptor;
      pDescriptor = pDescriptor->pNext;
   }
      /* if we reach this point, the service has not been found ! */
   return (W_ERROR_ITEM_NOT_FOUND);
}

/* See header */
uint8_t PLLCPSDPLookupServiceFromURI(
   tLLCPSDPInstance *                  pLLCPSDPInstance,
   const uint8_t *                     pUTF8ServiceURI,
   uint32_t                            nServiceURILength
)
{
   tLLCPSdpServiceDescriptor * pDescriptor;

      /* check parameters consistency */

   CDebugAssert(pLLCPSDPInstance != null);
   CDebugAssert(pUTF8ServiceURI != null);

   /* Specific case for SDP */
   if ((nServiceURILength == 14) && (CMemoryCompare(pUTF8ServiceURI, "urn:nfc:sn:sdp", nServiceURILength) == 0))
   {
      return 1;
   }

      /* walk through the registered services list */

   for (pDescriptor = pLLCPSDPInstance->pDescriptorList; pDescriptor != null; pDescriptor = pDescriptor->pNext)
   {
      if ((pDescriptor->nServiceURILength == nServiceURILength)         ||
          (pDescriptor->nServiceURILength == nServiceURILength + 1))          /* to handle the case provided Utf-8 is not zero terminated */
      {

            /* compare only the first nServiceURILength bytes */
         if (CMemoryCompare(pDescriptor->pUTF8ServiceURI, pUTF8ServiceURI, nServiceURILength) == 0)
         {
            return (pDescriptor->nSap);
         }
      }
   }
      /* not found */
   return (0);
}


/* See header */
uint8_t PLLCPSDPLookupServiceFromSAP(
   tLLCPSDPInstance *                  pLLCPSDPInstance,
   uint8_t                             nServiceSAP
)
{
   tLLCPSdpServiceDescriptor * pDescriptor;

   CDebugAssert(pLLCPSDPInstance != null);

      /* walk through the registered services list */

   for (pDescriptor = pLLCPSDPInstance->pDescriptorList; pDescriptor != null; pDescriptor = pDescriptor->pNext)
   {
      if (pDescriptor->nSap == nServiceSAP)
      {
         return (nServiceSAP);
      }
   }
      /* not found */
   return (0);
}

/* See header */
uint8_t PLLCPSDPGetWellKnownServiceSAP(
   tLLCPSDPInstance *                  pLLCPSDPInstance,
   const uint8_t *                     pUTF8ServiceURI,
   uint32_t                            nServiceURILength)
{
   uint8_t  i;

   /* check parameters consistency */

   CDebugAssert(pLLCPSDPInstance != null);
   CDebugAssert(pUTF8ServiceURI != null);

   for (i=0; i<sizeof(g_aWellKnownServiceTable) / sizeof(g_aWellKnownServiceTable[0]); i++)
   {
      if (g_aWellKnownServiceTable[i].nServiceLength == nServiceURILength)
      {
         if (CMemoryCompare(g_aWellKnownServiceTable[i].pServiceURI, pUTF8ServiceURI, nServiceURILength) == 0)
         {
            return (g_aWellKnownServiceTable[i].nSap);
         }
      }
   }

   return (0);
}


bool_t PLLCPSDPCheckURIAndSAPConsistency(
   tLLCPSDPInstance *      pLLCPSDPInstance,
   const uint8_t *         pUTF8ServiceURI,
   uint32_t                nServiceURILength,
   uint8_t                 nSap
)
{
   uint8_t i;
   bool_t    bSapMatch;
   bool_t    bUriMatch;

   if ((pUTF8ServiceURI == null) || (nSap == 0))
   {
      return W_TRUE;
   }

   for (i=0; i<sizeof(g_aWellKnownServiceTable) / sizeof(g_aWellKnownServiceTable[0]); i++)
   {
      if (g_aWellKnownServiceTable[i].nSap == nSap)
      {
         bSapMatch = W_TRUE;
      }
      else
      {
         bSapMatch = W_FALSE;
      }

      if ( (g_aWellKnownServiceTable[i].nServiceLength == nServiceURILength) &&
           (CMemoryCompare(g_aWellKnownServiceTable[i].pServiceURI, pUTF8ServiceURI, nServiceURILength) == 0))
      {
         bUriMatch = W_TRUE;
      }
      else
      {
         bUriMatch = W_FALSE;
      }

      if (bUriMatch != bSapMatch)
      {
         return W_FALSE;
      }
   }

   return W_TRUE;
}

uint32_t PLLCPSDPGetWellKnownServiceURI(
   tLLCPSDPInstance *                  pLLCPSDPInstance,
   uint8_t                             nSap,
   uint8_t *                           pUTF8ServiceURI
   )
{
   uint8_t i;

   CDebugAssert(pLLCPSDPInstance != null);

   /* check that WKS and SAP values are consistent */

   for (i=0; i<sizeof(g_aWellKnownServiceTable) / sizeof(g_aWellKnownServiceTable[0]); i++)
   {
      if (g_aWellKnownServiceTable[i].nSap == nSap)
      {
         if (pUTF8ServiceURI != null)
         {
            CMemoryCopy(pUTF8ServiceURI, g_aWellKnownServiceTable[i].pServiceURI, g_aWellKnownServiceTable[i].nServiceLength - 1);
         }

         return g_aWellKnownServiceTable[i].nServiceLength - 1;
      }
   }

   return 0;
}


/**
 * Allocates a SDP service descriptor
 *
 * @return The newly allocated SDP service descriptor or null
 **/

static tLLCPSdpServiceDescriptor * static_PLLCPSDPAllocServiceDescriptor(void)
{
   tLLCPSdpServiceDescriptor * pDescr;

      /* allocates the descriptor */

   pDescr =  CMemoryAlloc( sizeof(tLLCPSdpServiceDescriptor) );

   if (pDescr != null)
   {
      CMemoryFill(pDescr, 0, sizeof(* pDescr));
   }

   return (pDescr);
}

/**
 * Frees a SDP service descriptor
 *
 * @param[in]  pDescriptor : Address ot the descriptor to be freed
 *
 * @return     void
 **/

static void static_PLLCPSDPFreeServiceDescriptor(tLLCPSdpServiceDescriptor * pDescriptor)
{
   CDebugAssert(pDescriptor != null);

   if (pDescriptor != null)
   {
      /* free the allocated fields of the descriptor prior to free the descriptor */

      if (pDescriptor->pUTF8ServiceURI != null)
      {
         CMemoryFree(pDescriptor->pUTF8ServiceURI);
      }

      CMemoryFree(pDescriptor);
   }
}


#endif   /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

