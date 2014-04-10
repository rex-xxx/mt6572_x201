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

#ifndef __WME_FELICA_H
#define __WME_FELICA_H

/*******************************************************************************
   Contains the declaration of the type1 functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_FELICA_BLOCK_SIZE                    0x10

/**
 * @brief   Create the FeliCa connection.
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
void PFeliCaUserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength );

/**
 * @brief   Checks if a card is a FeliCa card.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The connection handle.
 **/
W_ERROR PFeliCaUserCheckType(
            tContext* pContext,
            W_HANDLE hUserConnection);

/**
 * @brief   Read data using Felica protocol. PReaderNotifyExchange is not used here.
 *          This method must be called from another connection, not directly by user.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter The callback parameter.
 *
 * @param[in]  pBuffer  A pointer on the buffer receiving the data read from the card.
 *
 * @param[in]  nLength  The size of the buffer.
 *
 * @param[in]  nNumberOfService  The number of services.
 *
 * @param[in]  pServiceCodeList  The list of service code.
 *
 * @param[in]  nNumberOfBlocks  The number of blocks.
 *
 * @param[in]  pBlockList  The list of the block list element.
 **/
void PFeliCaReadInternal(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList);

/**
 * @brief   Write data using Felica protocol. PReaderNotifyExchange is not used here.
 *          This method must be called from another connection, not directly by user.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter The callback parameter.
 *
 * @param[in]  pBuffer  A pointer on the buffer containing the data to write into the card.
 *
 * @param[in]  nLength  The size of the buffer.
 *
 * @param[in]  nNumberOfService  The number of services.
 *
 * @param[in]  pServiceCodeList  The list of service code.
 *
 * @param[in]  nNumberOfBlocks  The number of blocks.
 *
 * @param[in]  pBlockList  The list of the block list element.
 **/
void PFeliCaWriteInternal(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nLength,
            uint8_t  nNumberOfService,
            const uint16_t* pServiceCodeList,
            uint8_t  nNumberOfBlocks,
            const uint8_t* pBlockList);


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* #ifndef __WME_FELICA_H */
