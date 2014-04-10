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

#ifndef __WME_SMART_CACHE_H
#define __WME_SMART_CACHE_H

/*******************************************************************************
   Contains the declaration of the smart cache functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Maximum number of read function */
#define P_SMART_CACHE_READ_FUNCTION_NUMBER  2

/* Maximum number of write function */
#define P_SMART_CACHE_WRITE_FUNCTION_NUMBER  2

/* The maximum size in bytes of a read/write data buffer */
#define P_SMART_CACHE_MAX_READ_WRITE_BUFFER  132

/**
 * Type of a read function.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pConnection  The pointer on the connection.
 *
 * @param[in]  nSectorOffset  The zero-based index of the first sector to read.
 *
 * @param[in]  nSectorNumber  The number of sector to read.
 *
 * @param[out] pBuffer  The pointer on the buffer where to store the read data.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the callback function.
 **/
typedef void tSmartCacheReadSector(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter);

/**
 * Type of a write function.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pConnection  The pointer on the connection.
 *
 * @param[in]  nSectorOffset  The zero-based index of the first sector to write.
 *
 * @param[in]  nSectorNumber  The number of sector to write.
 *
 * @param[in]  pBuffer  The pointer on the buffer with the data to write.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the callback function.
 **/
typedef void tSmartCacheWriteSector(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter);

/* The structure describing a read function */
typedef const struct __tSmartCacheReadDescriptor
{
   uint32_t nAlign;
   uint32_t nSectorNumber;
   tSmartCacheReadSector* pReadFunction;
} tSmartCacheReadDescriptor;

/* The structure describing a write function */
typedef const struct __tSmartCacheWriteDescriptor
{
   uint32_t nAlign;
   uint32_t nSectorNumber;
   tSmartCacheWriteSector* pWriteFunction;
} tSmartCacheWriteDescriptor;

/* Generic operation */
typedef uint32_t tSmartCacheGenericOperation( uint32_t nValue );

/* The sector size value */
typedef const struct __tSmartCacheSectorSize
{
   uint32_t nValue;
   tSmartCacheGenericOperation* pDivide;
   tSmartCacheGenericOperation* pMultiply;
   tSmartCacheGenericOperation* pModulo;

} tSmartCacheSectorSize;

/* The constant structure describing a cache descriptor */
typedef const struct __tSmartCacheDescriptor
{
   uint32_t nIdentifier;
   tSmartCacheSectorSize* pSectorSize;
   tSmartCacheReadDescriptor aReadFunctionArray[P_SMART_CACHE_READ_FUNCTION_NUMBER];
   tSmartCacheWriteDescriptor aWriteFunctionArray[P_SMART_CACHE_WRITE_FUNCTION_NUMBER];
} tSmartCacheDescriptor;

/* The structure describing the smart cache */
typedef struct __tSmartCache
{
   tSmartCacheDescriptor* pDescriptor;
   void* pConnection;

   uint32_t nSectorNumber;

   /* Cache information */
   uint8_t* pInfoBuffer;
   uint8_t* pDataBuffer;

   /* Command informtion */
   uint32_t nOffset;
   uint32_t nLength;
   uint8_t* pBuffer;
   uint32_t nCurrentOperation;
   tDFCCallbackContext sCallbackContext;
   uint32_t nOperationOffset;
   uint32_t nOperationLength;
   uint32_t nWriteBackupOffset;
   uint32_t nWriteBackupLength;

   /* Intermediate buffer for the command data */
   uint8_t aCommandDataBuffer[P_SMART_CACHE_MAX_READ_WRITE_BUFFER];

} tSmartCache;

/**
 * Creates a smart cache.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache structure to initialize.
 *
 * @param[in]  nSectorNumber  The number of sectors.
 *
 * @param[in]  pDescriptor  The cache descriptor.
 *
 * @param[in]  pConnection  The connection.
 *
 * @return  The result code.
 **/
W_ERROR PSmartCacheCreateCache(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nSectorNumber,
               tSmartCacheDescriptor* pDescriptor,
               void* pConnection);

/**
 * Destroyes a smart cache.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache structure to destroy.
 **/
void PSmartCacheDestroyCache(
               tContext* pContext,
               tSmartCache* pSmartCache );

/**
 * Reads data in the cache.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache.
 *
 * @param[in]  nOffset  The offset in bytes of the buffer to read.
 *
 * @param[in]  nLength  The length in bytes of the buffer to read.
 *
 * @param[in]  pBuffer  The buffer where to write the read buffer.
 *             If set to null, the data is just read in the cache.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the callback function.
 **/
void PSmartCacheRead(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter);

/**
 * Gets a pointer in the data cache.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache.
 *
 * @param[in]  nOffset  The offset in bytes of the buffer.
 *
 * @param[in]  nLength  The length in bytes of the buffer.
 *
 * @return  The buffer pointer or null if an error occured.
 **/
uint8_t* PSmartCacheGetBuffer(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength);

/**
 * Writes data in the card.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache.
 *
 * @param[in]  nOffset  The offset in bytes of the buffer to write.
 *
 * @param[in]  nLength  The length in bytes of the buffer to write.
 *
 * @param[in]  pBuffer  The buffer containing the data to write.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the callback function.
 **/
void PSmartCacheWrite(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter);

/**
 * Invalidates smart cache
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache.
 *
 **/
void PSmartCacheInvalidateCache(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength
               );

#endif /* P_CONFIG_P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_SMART_CACHE_H */
