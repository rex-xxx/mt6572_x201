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
#ifndef __WME_CONNECTION_HANDOVER_H
#define __WME_CONNECTION_HANDOVER_H

/*******************************************************************************
   Contains the declaration of the Handover Connection functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_HANDOVER_MAX_PAYLOAD_LENGTH                 255



typedef W_ERROR tPHandoverParserFunction(void * pParserData , uint8_t * pDataReceived, uint32_t nLength);

typedef tWHandoverError tPHandoverError;

typedef struct __tPHandoverCarrierType
{
   /* Record Name */
   uint8_t              nLength;
   uint8_t              * pName;
}tPHandoverCarrierType;


typedef struct __tPHandoverConfigurationData
{
   /* Data */
   uint8_t     nLength;
   uint8_t     pData[P_HANDOVER_MAX_PAYLOAD_LENGTH];
}tPHandoverConfigurationData;


W_ERROR PHandoverCreate(
      tContext * pContext,
      W_HANDLE* phMessage);

W_ERROR PHandoverAddCarrier(
      tContext * pContext,
      W_HANDLE hHandoverConnection,
      const tPHandoverCarrierType * pCarrierType,
      const uint8_t * pConfigurationData,
      uint32_t nConfigurationDataLength,
      uint8_t  nCarrierPowerState);

W_ERROR PHandoverRemoveAllCarrier(
      tContext *  pContext,
      W_HANDLE hHandoverConnection);

void PHandoverPairingStart(
      tContext *  pContext,
      W_HANDLE hHandoverConnection,
      tPBasicGenericCallbackFunction* pCallback,
      void * pCallbackParameter,
      uint32_t nMode,
      W_HANDLE* phRegistry);

void PHandoverFormatTag(
      tContext *  pContext,
      W_HANDLE hHandoverConnection,
      tPBasicGenericCallbackFunction* pCallback,
      void * pCallbackParameter,
      uint32_t nActionMask,
      W_HANDLE* phRegistry);

W_ERROR PHandoverGetPairingInfo(
      tContext *  pContext,
      W_HANDLE hConnection,
      tWHandoverPairingInfo* pPairingInfo);

W_ERROR PHandoverGetPairingInfoLength(
      tContext *  pContext,
      W_HANDLE hConnection,
      uint32_t * pnLength);

W_ERROR PHandoverGetCarrierInfo(
      tContext *  pContext,
      W_HANDLE hConnection,
      tPHandoverCarrierType * pCarrierType,
      tPHandoverParserFunction * pParsingFunction,
      void * pData);

void PHandoverTest(tContext * pContext);

#endif
#endif

