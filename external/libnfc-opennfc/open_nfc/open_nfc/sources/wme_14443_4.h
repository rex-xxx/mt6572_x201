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

#ifndef __WME_14443_4_H
#define __WME_14443_4_H

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
   Contains the declaration of the 14443-4 functions
*******************************************************************************/

#ifdef P_READER_14P4_STANDALONE_SUPPORT

/**
 * @brief   Create the connection at 14443-4 level.
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
 * @param[in]  nProtocol  The protocol type.
 *
 * @param[in]  pBuffer  The buffer containing the activate result.
 *
 * @param[in]  nLength  The length of the buffer.
 **/
void P14P4UserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength );

/**
 * @brief   Gets the 14443-3 infos from a 14443-4 connection
 *
 * see P14Part3GetConnectionInfo
 **/

W_ERROR P14Part4GetConnectionInfoPart3( tContext* pContext, W_HANDLE hConnection, tW14Part3ConnectionInfo * p14Part3ConnectionInfo );

#else
/**
 * @brief   Create the connection at 14443-4 level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nProtocolType  The protocol type.
 *
 * @param[in]  phConnection  The connection handle.
 **/
void P14P4CreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocolType );

#endif /* P_READER_14P4_STANDALONE_SUPPORT */

/**
 * @brief   Checks if a card is compliant with Mifare.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pUID  The Mifare UID.
 *
 * @param[in]  pnUIDLength  The Mifare UID length.
 *
 * @param[in]  nType  The Mifare type (UL, 1K, 4K, Desfire).
 **/
W_ERROR P14P4UserCheckMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType );


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

W_HANDLE P14P4DriverExchangeDataInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            bool_t     bSendNAD,
            uint8_t  nNAD,
            bool_t     bCreateOperation,
            bool_t     bFromUser);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* __WME_14443_4_H */
