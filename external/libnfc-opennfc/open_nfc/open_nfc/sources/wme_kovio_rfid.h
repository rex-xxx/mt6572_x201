/*
 * Copyright (c) 2011-2012 Inside Secure, All Rights Reserved.
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

#ifndef __WME_KOVIO_RFID_H
#define __WME_KOVIO_RFID_H

/*******************************************************************************
   Contains the declaration of the Kovio RFID functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The Kovio RFID move is organised in 64 blocks of 4 bytes */
#define P_KOVIO_RFID_BLOCK_NUMBER            64
#define P_KOVIO_RFID_LOCK_BYTE_NUMBER        8

/* Kovio RFID block info */
#define P_KOVIO_RFID_FIRST_DATA_BLOCK        (P_NDEF2GEN_STATIC_LOCK_BLOCK + 1)
#define P_KOVIO_RFID_LAST_DATA_BLOCK         61
#define P_KOVIO_RFID_LOCK_BLOCK              62
#define P_KOVIO_RFID_LOCK_LENGTH             6



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
void PKovioRFIDCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/** See tPReaderUserRemoveSecondaryConnection */
void PKovioRFIDRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection );

/**
 * @brief   Checks if a Kovio RFID card can be formatted as a Type 2 Tag.
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
W_ERROR PKovioRFIDCheckType2(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumTagSize,
            uint8_t* pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable );


/**
 * @brief Invalidate a part of the Kovio RFID SmartCache
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
W_ERROR PKovioRFIDInvalidateSmartCacheNDEF(
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

void PKovioRFIDPoll(
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
W_ERROR PKovioRFIDNDEF2Lock(tContext * pContext,
                      W_HANDLE hConnection);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_MY_D_H */
