/*
 * Copyright (c) 2008-2012 Inside Secure, All Rights Reserved.
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
   Contains the Wi-Fi pairing implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( WIFI_PAIR )

#include "wme_context.h"

#if( (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_PAIRING)

/**
 * @brief Carrier Type Name reserved for handover Wi-Fi connection
 **/
static const char aHandoverWifiCarrierTypeName[] = {  'a','p','p','l','i','c','a','t','i','o','n',
                                                            '/',
                                                            'v','n','d',
                                                            '.',
                                                            'w','f','a',
                                                            '.',
                                                            'w','s','c'};

W_ERROR PHandoverAddWiFiCarrier(
   tContext *  pContext,
   W_HANDLE    hConnectionHandover,
   tWWiFiPairingInfo *   pWifiInfo,
   uint8_t     nCarrierPowerState)
{
   W_ERROR nError = W_SUCCESS;
   tPHandoverCarrierType g_sWifiCarrierType;
   g_sWifiCarrierType.nLength = sizeof(aHandoverWifiCarrierTypeName);
   g_sWifiCarrierType.pName = (uint8_t *)aHandoverWifiCarrierTypeName;

   PDebugTrace("PHandoverAddWifiCarrier");

   if(nError != W_SUCCESS)
   {
      PDebugTrace("PHandoverAddWifiCarrier => Error");
      return nError;
   }
   else
   {
      /*** Add Carrier ***/
      PHandoverAddCarrier(
            pContext,
            hConnectionHandover,
            &g_sWifiCarrierType,
            pWifiInfo->pData,
            pWifiInfo->nLength,
            nCarrierPowerState
       );
   }

   return W_SUCCESS;
}

static W_ERROR static_tPHandoverWiFiParser(void * pParserData , uint8_t * pDataReceived, uint32_t nLength)
{
   tWWiFiPairingInfo * pWifiInfo = (tWWiFiPairingInfo *) pParserData;

   CDebugAssert(nLength <= W_WIFI_PAIRING_MAX_INFO_LENTGH);

   /* must be ok */
   if(nLength >  W_WIFI_PAIRING_MAX_INFO_LENTGH)
   {
      PDebugError("static_tPHandoverWifiParser out of resource");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pWifiInfo->nLength = nLength;
   CMemoryCopy( pWifiInfo->pData,
                pDataReceived,
                nLength);

   return W_SUCCESS;
}

W_ERROR PHandoverGetWiFiInfo(
      tContext * PContext,
      W_HANDLE hConnectionHandover,
      tWWiFiPairingInfo * pWiFiInfo
   )
{
   tPHandoverCarrierType sCarrierType;
   sCarrierType.nLength = sizeof(aHandoverWifiCarrierTypeName);
   sCarrierType.pName = (uint8_t *)aHandoverWifiCarrierTypeName;

   PDebugError("PBTPairingGetRemoteDeviceInfo");
   if(pWiFiInfo == null)
   {
      PDebugError("PBTPairingGetRemoteDeviceInfo : Error Bad Parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   return PHandoverGetCarrierInfo(
      PContext,
      hConnectionHandover,
      &sCarrierType,
      static_tPHandoverWiFiParser,
      pWiFiInfo);

}

/*********************        End Handover            **********************/

#endif /* ( (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_PAIRING) */


#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (! defined P_INCLUDE_PAIRING)

/* See client API */
W_ERROR PHandoverAddWiFiCarrier(
   tContext *  pContext,
   W_HANDLE    hConnectionHandover,
   tWWiFiPairingInfo *   pWifiInfo,
   uint8_t     nCarrierPowerState)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See client API */
W_ERROR PHandoverGetWiFiInfo(
      tContext * PContext,
      W_HANDLE hConnectionHandover,
      tWWiFiPairingInfo * pWiFiInfo
   )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#endif /*((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (! defined P_INCLUDE_PAIRING) */

