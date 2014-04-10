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

#ifndef __WME_PICO_H
#define __WME_PICO_H

/*******************************************************************************
   Contains the declaration of the Picopass functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_PICO_BLOCK_SIZE                    0x08

/**
 * @brief   Create the Picopass connection at level 15693-2 or 14443-3.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter   The callback parameter.
 *
 * @param[in]  nProperty  The connection property.
 **/
void PPicoCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/** See tPReaderUserRemoveSecondaryConnection */
void PPicoRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection );

/**
 * @brief   Checks if a card is a Type 5 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pnMaximumTagSize  The maximum tag size.
 *
 * @param[in]  pbIsLocked  The card is locked or not.
 *
 * @param[in]  pbIsLockable  The card is lockable or not.
 **/
W_ERROR PPicoCheckType5(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumTagSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable );


/**
 * @brief   Invalidates the cache associated to the current connection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The connection handle.
 */
W_ERROR PPicoInvalidateCache(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength);

/**
 * @brief   Read data using Picopass protocol. PReaderNotifyExchange is not used here.
 *          This method must be called from another connection, not directly by user.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The user connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter The callback parameter.
 *
 * @param[in]  pBuffer  A pointer on the buffer receiving the data read from the card.
 *
 * @param[in]  nOffset  The offset in bytes from where the reading should start.
 *
 * @param[in]  nLength  The number of bytes to read.
 *
 * @param[in]  phOperation  A pointer on a variable valued with the handle of the operation.
 **/
void PPicoReadInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation);

/**
 * @brief   Write data using Picopass protocol. PReaderNotifyExchange is not used here.
 *          This method must be called from another connection, not directly by user.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The user connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter The callback parameter.
 *
 * @param[in]  pBuffer  A pointer on the buffer containing the data to write into the card.
 *
 * @param[in]  nOffset  The offset in bytes from where the writing should start.
 *
 * @param[in]  nLength  The number of bytes to write.
 *
 * @param[in]  bLockCard  If set to W_TRUE the card will be locked in read-only mode after the write operation.
 *
 * @param[in]  phOperation  A pointer on a variable valued with the handle of the operation.
 **/
void PPicoWriteInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockCard,
            W_HANDLE *phOperation);


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_PICO_H */
