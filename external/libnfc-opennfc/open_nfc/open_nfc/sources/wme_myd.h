/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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

#ifndef __WME_MY_D_H
#define __WME_MY_D_H

/*******************************************************************************
   Contains the declaration of the My-d functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief   Create the connection at 14443-3 A level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nProtocol  The connection property.
 **/
void PMyDCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/** See tPReaderUserRemoveSecondaryConnection */
void PMyDRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection );

/**
 * @brief   Checks if a My-d card can be formatted as a Type 2 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pnMaximumTagSize  The maximum tag size.
 *
 * @param[in]  pnSectorSize  The sector size.
 *
 * @param[in]  pbIsLocked  The card is locked or not.
 *
 * @param[in]  pbIsLockable  The card is lockable or not.
 *
 * @param[in] pbIsFormattable The card is formattable or not
 **/
W_ERROR PMyDCheckType2(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumTagSize,
            uint8_t* pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable );


/**
 * @brief Invalidate a part of the myD NFC SmartCache
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  pointer on the myD connection Handle object header
 *
 * @param[in]  nOffset  offset where the invalidate begins
 *
 * @param[in]  nLength  length of data invalidated
 *
 **/
W_ERROR PMyDNFCInvalidateSmartCacheNDEF(
            tContext * pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength);

/**
 * @brief   Send a request response to the current card and verify if it answers
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  pointer on the current object
 *
 * @param[in]  pCallback  Callback trigerred when the card answers
 *
 * @param[in]  pCallbackParameter   param of callback
 **/

void PMyDNFCPoll(
      tContext * pContext,
      tHandleObjectHeader * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);


/**
 * @brief   Update the My-d data when a NDEF Type 2 tag has been locked.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 **/
W_ERROR PMyDNDEF2Lock(tContext * pContext,
                      W_HANDLE hConnection);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_MY_D_H */
