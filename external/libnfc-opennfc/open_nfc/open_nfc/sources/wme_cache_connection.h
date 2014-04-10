/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
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

#ifndef __WME_CONNECTION_CACHE_H
#define __WME_CONNECTION_CACHE_H

/*******************************************************************************
   Contains the declaration of the connection cache functions
*******************************************************************************/

#define ZERO_IDENTIFIER_LEVEL             0x00000000

#define P_CACHE_CONNECTION_SIZE           0x2800

/**
 * Describes cache connection  parameters
 * ****************************
 * (!) Do not use directly (!)
 ***/
typedef struct __tCacheConnectionInstance
{
   /* length of part of the cache already reserved */
   uint32_t nTotalLengthInUse;
   /* cache connection Memory */
   uint32_t aCacheMemory[P_CACHE_CONNECTION_SIZE/4];
   /* signal write operation in cache connection */
   bool_t bIsCacheModified;

}tCacheConnectionInstance;

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief  Creates a block(header + buffer) in cache connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nIdentifier  The module identifier.
 *
 * @param[in]  nIndex   The buffer index in cache.
 *
 * @param[in]  nLength   The buffer length in bytes.
 *
 * @return  The pointer on the new buffer.
 **/
void* PCacheConnectionCreateBlock(
            tContext* pContext,
            uint32_t nIdentifier,
            uint32_t nIndex,
            uint32_t nLength );

/**
 * @brief   Gets the specified buffer in cache connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nIdentifier  The module identifier.
 *
 * @param[in]  nIndex   The buffer index in cache.
 *
 * @param[out] pnLength   A pointer on a variable valued with the buffer nLength.
 *
 * @return  The pointer on the buffer.
 **/
void* PCacheConnectionGetBuffer(
               tContext* pContext,
               uint32_t nIdentifier,
               uint32_t nIndex,
               uint32_t* pnLength );

/**
 * @brief   Updates buffer in cache connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nIdentifier  The module identifier.
 *
 * @param[in]  nIndex   The buffer index in cache.
 **/
void PCacheConnectionUpdateBuffer(
               tContext* pContext,
               uint32_t nIdentifier,
               uint32_t nIndex );

/**
 * @brief   Removes block(s) from the cache connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nIdentifier  The module identifier of the blocks to remove.
 **/
void PCacheConnectionRemoveBlock(
               tContext* pContext,
               uint32_t nIdentifier );

/**
 * @brief   Commits the connection cache updated with the user copy.
 *
 * @param[in]  pContext  The context.
 **/
void PCacheConnectionUserCommit(
               tContext* pContext );

/**
 * @brief   Updates the user connection cache updated with the driver copy.
 *
 * @param[in]  pContext  The context.
 **/
void PCacheConnectionUserUpdate(
               tContext* pContext );

#endif /* P_CONFIG_P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief   Resets the cache connection.
 *
 * @param[in]  pContext  The context.
 **/
void PCacheConnectionReset(
               tContext* pContext);

#endif /* P_CONFIG_P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* __WME_CONNECTION_CACHE_H */
