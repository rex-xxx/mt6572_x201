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
   Contains the declaration of the LLCP Service Discovery Protocol component
*******************************************************************************/

#ifndef __WME_P2P_SDP_H
#define __WME_P2P_SDP_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tLLCPSdpServiceDescriptor tLLCPSdpServiceDescriptor;

struct __tLLCPSdpServiceDescriptor
{
   tLLCPSdpServiceDescriptor * pNext;         /**< pointer to the next descriptor */

  uint8_t    nSap;                           /**< SAP associated to the service */
  uint8_t  * pUTF8ServiceURI;                /**< URI associated to the service */
  uint32_t   nServiceURILength;              /**< Length in bytes of the service URI */

};


typedef struct __tLLCPSDPInstance
{
   tLLCPSdpServiceDescriptor   * pDescriptorList;    /**< List of registered services */

} tLLCPSDPInstance;

/**
 * Initialises a LLCP Service Discovery Protocol instance
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @return W_SUCCESS
 **/


W_ERROR PLLCPCreateSDP(
   tLLCPSDPInstance * pLLCPSDPInstance
);


 /**
 * Destroys a LLCP Service Discovery Protocol instance
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @return W_SUCCESS
 **/

W_ERROR PLLCPDestroySDP(
   tLLCPSDPInstance * pLLCPSDPInstance
);


/**
 * Registers a service in the SDP
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @param[in]  nServiceSap The SAP associated to the service.
 *
 * @param[in]  pUTF8ServiceURI The URI associated to the service.
 *
 * @param[in]  nServiceURILength The length of URI in bytes
 *
 * @return
 *         - W_SUCCESS
 *         - W_ERROR_BAD_PARAMETER
 *         - W_ERROR_OUT_OF_RESOURCE
 *
 **/

W_ERROR PLLCPSDPRegisterService(
   tLLCPSDPInstance * pLLCPSDPInstance,
   uint8_t            nServiceSap,
   const uint8_t *    pUTF8ServiceURI,
   uint32_t           nServiceURILength
);

/**
 * Unregisters a service from the SDP
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @param[in]  nServiceSap The SAP associated to the service.
 *
 * @return
 *         - W_SUCCESS
 *         - W_ERROR_ITEM_NOT_FOUND
 *
 **/

W_ERROR PLLCPSDPUnregisterService(
   tLLCPSDPInstance *   pLLCPSDPInstance,
   uint8_t              nServiceSap
);



/**
 * Looks up for a service from service URI
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @param[in]  pUTF8ServiceURI The service URI in Utf-8
 *
 * @param[in]  nServiceURILength The length of URI in bytes
 *
 * return The corresponding SAP if found, else 0
 *
 **/
uint8_t PLLCPSDPLookupServiceFromURI(
   tLLCPSDPInstance *   pLLCPSDPInstance,
   const uint8_t *      pUTF8ServiceURI,
   uint32_t             nServiceURILength

);

/**
 * Looks up for a service from service SAP
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @param[in]  nServiceSAP       The service SAP value
 *
 * return The corresponding SAP if found, else 0
 *
 **/

uint8_t PLLCPSDPLookupServiceFromSAP(
   tLLCPSDPInstance *      pLLCPSDPInstance,
   uint8_t                 nServiceSAP
);


/**
 * Checks if a specified service is a well-known service
 *
 * @param[in]  pLLCPSDPInstance  The SDP instance
 *
 * @param[in]  pUTF8ServiceURI   The URI describing the service.
*
 * @param[in]  nServiceURILength The length of URI in bytes
 *
 * @return     The service SAP if found, else 0
 **/

uint8_t PLLCPSDPGetWellKnownServiceSAP(
   tLLCPSDPInstance *      pLLCPSDPInstance,
   const uint8_t *         pUTF8ServiceURI,
   uint32_t                nServiceURILength
);

bool_t PLLCPSDPCheckURIAndSAPConsistency(
   tLLCPSDPInstance *      pLLCPSDPInstance,
   const uint8_t *         pUTF8ServiceURI,
   uint32_t                nServiceURILength,
   uint8_t                 nSap
);

uint32_t PLLCPSDPGetWellKnownServiceURI(
   tLLCPSDPInstance *                  pLLCPSDPInstance,
   uint8_t                             nSap,
   uint8_t *                           pUTF8ServiceURI
   );

#endif   /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

#endif /* __WME_P2P_SDP_H */
