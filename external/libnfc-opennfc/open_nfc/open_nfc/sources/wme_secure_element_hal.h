/*
 * Copyright (c) 2010-2012 Inside Secure, All Rights Reserved.
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

#ifndef __WME_SECURE_ELEMENT_HAL_H
#define __WME_SECURE_ELEMENT_HAL_H

/* The maximum number of standalone slots */
#define P_SE_HAL_MAXIMUM_NUMBER_STANDALONE_SE   4

/* The maximum number of SWP slots */
#define P_SE_HAL_MAXIMUM_NUMBER_SWP_SE   NAL_MAXIMUM_SE_SWP_LINK_NUMBER

/* The maximum number of proprietary slots */
#define P_SE_HAL_MAXIMUM_NUMBER_PROPRIETARY_SE   NAL_MAXIMUM_SE_PROPRIETARY_LINK_NUMBER

/* The maximum number of Secure Elements */
#define P_SE_HAL_MAXIMUM_SE_NUMBER (P_SE_HAL_MAXIMUM_NUMBER_PROPRIETARY_SE + P_SE_HAL_MAXIMUM_NUMBER_SWP_SE + P_SE_HAL_MAXIMUM_NUMBER_STANDALONE_SE)

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The Get info callback function */
typedef void tPSeHalGetInfoCallbackFunction(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nHalSlotIdentifier,
         uint32_t nHalSessionIdentifier,
         uint32_t nActualAtrLength,
         W_ERROR nResult );

/* Slot registry */
typedef struct __tPSeHalSlot
{
   uint32_t nHalSlotIdentifier;
   uint32_t nSlotIdentifier;

   /* Get Info operation */
   tPSeHalGetInfoCallbackFunction* pGetInfoCallback;
   void* pGetInfoCallbackParameter;
   uint32_t nGetInfoAtrBufferLength;

   /* Exchange operation */
   tPBasicGenericDataCallbackFunction* pExchangeCallback;
   void* pExchangeCallbackParameter;
   bool_t bExchangeFromUser;
} tPSeHalSlot;

/* Declare a reader driver registry */
typedef struct __tPSeHalInstance
{
#ifdef P_INCLUDE_SE_SECURITY
   tCSePorting* pSePorting;
#endif /* P_INCLUDE_SE_SECURITY */

   bool_t bIsInitialized;

   tPSeHalSlot aSlotArray[P_SE_HAL_MAXIMUM_SE_NUMBER];

   uint8_t aRefreshFileList[256];

} tPSeHalInstance;

/**
 * Returns the string corresponding to a SE HAL identifier.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 *
 * @return The string value.
 **/
const char* PSeHalTraceIdentifier(
         uint32_t nHalSlotIdentifier);

/**
 * Checks if the specified slot is a proprietary slot.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier
 *
 * @return  The result of the check.
 **/
bool_t PSeHalIsProprietarySlot(
         uint32_t nHalSlotIdentifier);

/**
 * Checks if the specified slot is a SWP slot.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier
 *
 * @return  The result of the check.
 **/
bool_t PSeHalIsSwpSlot(
         uint32_t nHalSlotIdentifier);

/**
 * Checks if the specified slot is a standalone slot.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier
 *
 * @return  The result of the check.
 **/
bool_t PSeHalIsStandaloneSlot(
         uint32_t nHalSlotIdentifier);

/**
 * @brief   Creates the Secure Element HAL instance.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSeHalInstance The instance to initialize.
 **/
void PSeHalCreate(
         tContext* pContext,
         tPSeHalInstance* pSeHalInstance );

/**
 * @brief   Destroyes a Secure Element HAL instance.
 *
 * @post  PSeHalDestroy() does not return any error. The caller should always
 *        assume that the registry instance is destroyed after this call.
 *
 * @post  The caller should never re-use the registry instance value.
 *
 * @param[in]  pSeHalInstance  The instance to destroy.
 **/
void PSeHalDestroy(
         tPSeHalInstance* pSeHalInstance );

/* See CSeGetStaticInfo() */
bool_t PSeHalGetStaticInfo(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         uint32_t nHalSlotIdentifier,
         uint32_t* pnCapabilities,
         uint32_t* pnSwpTimeout,
         uint8_t* pNameBuffer,
         uint32_t nNameBufferLength );

#ifdef P_INCLUDE_SE_SECURITY
/* See CSeGetInfo() */
void PSeHalGetInfo(
         tContext* pContext,
         tPSeHalGetInfoCallbackFunction* pCallback,
         void* pCallbackParameter,
         uint32_t nHalSlotIdentifier,
         uint8_t* pAtrBuffer,
         uint32_t nAtrBufferLength );

/**
 * Exchanges an APDU with the Secure Element.
 *
 * The implementation may implement the automatic handling of the '6C XX' - GET RESPONSE sequence. If it is
 * not done the NFC stack will do it.
 *
 * The implementation of this function shall return immediately.
 * The implementation shall perform the operation asynchronously and return the result with the callback later on.
 *
 * The implementation shall always call the callback function to notify the result of the operation.
 * The parameters of the callback should be as follows:
 *  - nError  set to W_SUCCESS if the operation is successful, W_ERROR_TIMEOUT if an error occurs.
 *  - nLength is the actual length in bytes of the response APDU stored in pResponseApduBuffer. The value is zero in case of failure.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback function parameter
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 *
 * @param[in]  nSessionIdentifier  The card session identifier. This value is returned byt PSeHalGetInfo().
 *
 * @param[in]  pApduBuffer  A pointer on a buffer containing the APDU.
 *
 * @param[in]  nApduLength  The length in bytes of the APDU.
 *
 * @param[out] pResponseApduBuffer  A pointer on a buffer receiving the response APDU.
 *
 * @param[in]  pResponseApduBufferLength  The length in bytes of the response APDU buffer.
 *
 * @param[in]  bCreateOperation  The create operation flag.
 *
 * @return  The opration handle if requested.
 */
W_HANDLE PSeHalExchangeApdu(
         tContext* pContext,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         uint32_t nHalSlotIdentifier,
         uint32_t nHalSessionIdentifier,
         const uint8_t* pApduBuffer,
         uint32_t nApduLength,
         uint8_t* pResponseApduBuffer,
         uint32_t pResponseApduBufferLength,
         bool_t bCreateOperation);

/* See CSeTriggerStkPolling() */
void PSeHalTriggerStkPolling(
         tContext* pContext,
         uint32_t nHalSlotIdentifier);
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER ||P_CONFIG_MONOLITHIC */

#endif /* __WME_SECURE_ELEMENT_HAL_H */
