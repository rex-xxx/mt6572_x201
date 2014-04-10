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
   Contains the declaration of the LLCP SAP component
  *******************************************************************************/

#ifndef __WME_P2P_SAP_H
#define __WME_P2P_SAP_H

#define  LLCP_MIN_SAP                              0x02
#define  LLCP_MAX_SAP                              0x3F

#define  LLCP_SIGNALLING_SAP                       0x00
#define  LLCP_SDP_SAP                              0x01
#define  LLCP_DYNAMIC_SERVER_MIN_SAP               0x10
#define  LLCP_DYNAMIC_SERVER_MAX_SAP               0x1F
#define  LLCP_DYNAMIC_CLIENT_MIN_SAP               0x20
#define  LLCP_DYNAMIC_CLIENT_MAX_SAP               0x3F

#define  LLCP_MAX_SAP                              0x3F

#define  SAP_FIXED_VALUE                           1
#define  SAP_DYNAMIC_SERVER                        2
#define  SAP_DYNAMIC_CLIENT                        3

typedef struct __tLLCPSAPInstance
{
      /* Array used as a bitfield for SAP allocation */

   uint8_t aAllocatedSap[(LLCP_MAX_SAP+1)/8+1];

} tLLCPSAPInstance;




/**
 * Initialises a LLCP SAP instance
 *
 * @param[in]  pLLCPSAPInstance  The SAP instance
 *
 * @return  W_SUCCESS
 *
 **/

W_ERROR PLLCPCreateSAP(
   tLLCPSAPInstance * pLLCPSAPInstance
   );

/**
 * Destroys a LLCP SAP instance
 *
 * @param[in]  pLLCPSAPInstance  The SAP instance
 *
 * @return  W_SUCCESS
 *
 **/

W_ERROR PLLCPDestroySAP(
   tLLCPSAPInstance * pLLCPSAPInstance
   );

/**
 * Allocates a new SAP
 *
 * @param[in] pLLCPSAPInstance The SAP instance.
 *
 * @param[in] nSapType The type of SAP to be allocated.
 *                     Allowed values are : SAP_FIXED_VALUE, SAP_DYNAMIC_SERVER or SAP_DYNAMIC_CLIENT
 *
 * @param[in] nSapValue  The value of SAP to be allocated, fow SAP_FIXED_VALUE only
 *
 * @return The allocated SAP
 */

uint8_t PLLCPSAPAllocSap(
   tLLCPSAPInstance * pLLCPSAPInstance,
   uint8_t nSapType,
   uint8_t nSapValue
   );


/**
 * Frees a SAP
 *
 * @param[in] pLLCPSAPInstance The SAP instance.
 *
 * @param[in] nSap The SAP value to be freed
 *
 * @return void
 */

void PLLCPSAPFreeSap(
   tLLCPSAPInstance * pLLCPSAPInstance,
   uint8_t nSap
   );

#endif /* __WME_P2P_SAP_H */
