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

#ifndef __WME_NDEF_TYPE2_GENERIC_H
#define __WME_NDEF_TYPE2_GENERIC_H

/*******************************************************************************
   Contains the declaration of the Mifare functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_NDEF2GEN_BLOCK_SIZE                      4

#define P_NDEF2GEN_STATIC_BLOCK_NUMBER             16
#define P_NDEF2GEN_STATIC_BYTE_LENGTH              (P_NDEF2GEN_STATIC_BLOCK_NUMBER * P_NDEF2GEN_BLOCK_SIZE)

#define P_NDEF2GEN_STATIC_LOCK_BLOCK               2
#define P_NDEF2GEN_STATIC_LOCK_BYTE_OFFSET         2
#define P_NDEF2GEN_STATIC_LOCK_BYTE_ADDRESS        (P_NDEF2GEN_STATIC_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE + P_NDEF2GEN_STATIC_LOCK_BYTE_OFFSET)
#define P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH         2
#define P_NDEF2GEN_STATIC_BLOCK_NUMBER             16


/**
 * @brief   Checks if a card is compliant with type 2.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 **/
W_ERROR PNDEF2GenCheckType2(
            tContext* pContext,
            W_HANDLE hConnection );

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
void PNDEF2GenCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/** See tPReaderUserRemoveSecondaryConnection */
void PNDEF2GenRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection );

/**
 * @brief   Creates the smart cache for the dynamic area (>64 bytes).
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  nSectorNumber  The number of sectors in the tag (static + dynamic area) (must be greater than 64).
 **/
W_ERROR PNDEF2GenCreateSmartCacheDynamic(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nSectorNumber);

/**
 * @brief   Invalidates a smart cache part (it could be in static or dynamic area).
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  nOffset  the offset of the first byte to invalidate.
 *
 * @param[in]  nLength  The number of bytes to invalidate.
 **/
W_ERROR PNDEF2GenInvalidateCache(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength);

/**
 * @brief   Reads some bytes in a generic NDEF type 2 tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  pBuffer  The pointer to receive the read data.
 *
 * @param[in]  nOffset  the offset of the first byte to read.
 *
 * @param[in]  nLength  The number of bytes to read.
 **/
void PNDEF2GenRead(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength );

/**
 * @brief   Writes some bytes in a generic NDEF type 2 tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  pBuffer  The pointer to the data to write.
 *
 * @param[in]  nOffset  the offset of the first byte to write.
 *
 * @param[in]  nLength  The number of bytes to write.
 **/
void PNDEF2GenWrite(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength );

/* Write directly without using a smart cache since
   smart cache performs a read prior a write and some data are not readable */
void PNDEF2GenDirectWrite(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nSectorOffset );


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_NDEF_TYPE2_GENERIC_H */
