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

#ifndef __WME_15693_H
#define __WME_15693_H

/*******************************************************************************
   Contains the declaration of the ISO 15693 functions
*******************************************************************************/
/* Philips tag's size */
#define P_15693_PHILIPS_SL2ICS20_SIZE     112
#define P_15693_PHILIPS_SL2ICS53_SIZE     160
#define P_15693_PHILIPS_SL2ICS50_SIZE     32

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief   Creates the connection at ISO 15693 level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  hDriverConnection  The driver connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter The callback parameter.
 *
 * @param[in]  nProtocol  The protocol property.
 *
 * @param[in]  pBuffer  The buffer containing the activate result.
 *
 * @param[in]  nLength  The length of the buffer.
 **/
void P15P3UserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength );

/**
 * @brief   Checks if a card is a Type 6 Tag.
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
W_ERROR P15P3UserCheckType6(
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
W_ERROR P15InvalidateCache(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength);

/**
 * @brief   Create the connection for ICODE, LRI and TAG IT chips
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
void P15P3UserCreateSecondaryConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/** See tPReaderUserRemoveSecondaryConnection */
void P15P3UserRemoveSecondaryConnection(
            tContext* pContext,
            W_HANDLE hUserConnection );


/*********** End Of Keep for futur usage **************/

/**
 * @brief   Set supported commands in 15693 structure
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  bIsReadMultipleSupported  Indicates if ReadMultiple is supported
 *
 * @param[in]  bIsWriteMultipleSupported Indicates if WriteMultiple is supported
 **/
W_ERROR P15SetSupportedCommands(
      tContext* pContext,
      W_HANDLE hConnection,
      bool_t bIsReadMultipleSupported,
      bool_t bIsWriteMultipleSupported );

/**
 * @brief   Read data using 15693 protocol. PReaderNotifyExchange is not used here.
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
 **/
void P15ReadInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength);

/**
 * @brief   Write data using 15693 protocol. PReaderNotifyExchange is not used here.
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
 * @param[in]  bLockSectors  If set to W_TRUE the sectors containing the data written will be locked after the write operation.
 **/
void P15WriteInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockSectors);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_15693_H */
