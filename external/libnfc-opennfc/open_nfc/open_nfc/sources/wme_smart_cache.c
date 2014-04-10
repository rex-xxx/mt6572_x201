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

/*******************************************************************************
   Contains the implementation of the smart cache functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( CACHE )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_SMART_CACHE_DATA_INDEX               0x01
#define P_SMART_CACHE_INFO_INDEX               0x10

/* The operation types */
#define P_SMART_CACHE_OPERATION_NONE   0
#define P_SMART_CACHE_OPERATION_READ   1
#define P_SMART_CACHE_OPERATION_WRITE  2

static bool_t static_PSmartCacheReadStateMachine(
               tContext* pContext,
               tSmartCache* pSmartCache);

static bool_t static_PSmartCacheWriteStateMachine(
               tContext* pContext,
               tSmartCache* pSmartCache);


#define PSmartCacheDivideN(n)     static uint32_t static_PSmartCacheDivide##n(uint32_t nValue)    { return (nValue)/(n); }
#define PSmartCacheMultiplyN(n)   static uint32_t static_PSmartCacheMultiply##n(uint32_t nValue)  { return (nValue)*(n); }
#define PSmartCacheModuloN(n)     static uint32_t static_PSmartCacheModulo##n(uint32_t nValue)    { return (nValue)%(n); }


#define PSmartCacheSectorN(n)                      \
   PSmartCacheDivideN(n)                           \
   PSmartCacheMultiplyN(n)                         \
   PSmartCacheModuloN(n)                           \
                                                   \
   tSmartCacheSectorSize g_sSectorSize##n =        \
   {                                               \
      n,                                           \
      static_PSmartCacheDivide##n,                 \
      static_PSmartCacheMultiply##n,               \
      static_PSmartCacheModulo##n                  \
   };

PSmartCacheSectorN(1)
PSmartCacheSectorN(2)
PSmartCacheSectorN(3)
PSmartCacheSectorN(4)
PSmartCacheSectorN(5)
PSmartCacheSectorN(6)
PSmartCacheSectorN(7)
PSmartCacheSectorN(8)
PSmartCacheSectorN(9)
PSmartCacheSectorN(10)
PSmartCacheSectorN(11)
PSmartCacheSectorN(12)
PSmartCacheSectorN(13)
PSmartCacheSectorN(14)
PSmartCacheSectorN(15)
PSmartCacheSectorN(16)
PSmartCacheSectorN(17)
PSmartCacheSectorN(18)
PSmartCacheSectorN(19)
PSmartCacheSectorN(20)
PSmartCacheSectorN(21)
PSmartCacheSectorN(22)
PSmartCacheSectorN(23)
PSmartCacheSectorN(24)
PSmartCacheSectorN(25)
PSmartCacheSectorN(26)
PSmartCacheSectorN(27)
PSmartCacheSectorN(28)
PSmartCacheSectorN(29)
PSmartCacheSectorN(30)
PSmartCacheSectorN(31)
PSmartCacheSectorN(32)

/**
 * Resets the operation parameters
 *
 * @param[in]  pSmartCache  The smart cache.
 **/
static void static_PSmartCacheResetOperation(
               tSmartCache* pSmartCache)
{
   pSmartCache->nOffset = 0;
   pSmartCache->nLength = 0;
   pSmartCache->pBuffer = null;
   pSmartCache->nCurrentOperation = P_SMART_CACHE_OPERATION_NONE;
   pSmartCache->nOperationOffset = 0;
   pSmartCache->nOperationLength = 0;
   pSmartCache->nWriteBackupOffset = 0;
   pSmartCache->nWriteBackupLength = 0;
}

/* See header file */
W_ERROR PSmartCacheCreateCache(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nSectorNumber,
               tSmartCacheDescriptor* pDescriptor,
               void* pConnection)
{
   uint8_t* pInfoBuffer;
   uint8_t* pDataBuffer;
   uint32_t nInfoLength, nDataLength;

   CMemoryFill(pSmartCache, 0, sizeof(tSmartCache));

   pInfoBuffer = (uint8_t*)PCacheConnectionGetBuffer(
                                        pContext,
                                        pDescriptor->nIdentifier,
                                        P_SMART_CACHE_INFO_INDEX,
                                        &nInfoLength);

   pDataBuffer = (uint8_t*)PCacheConnectionGetBuffer(
                                        pContext,
                                        pDescriptor->nIdentifier,
                                        P_SMART_CACHE_DATA_INDEX,
                                        &nDataLength);

   if((pInfoBuffer == null) || (pDataBuffer == null)
   || (nInfoLength != nSectorNumber + 1)
   || (nDataLength != pDescriptor->pSectorSize->pMultiply(nSectorNumber))
   || (pInfoBuffer[nSectorNumber] != 1))
   {
      PCacheConnectionRemoveBlock( pContext, pDescriptor->nIdentifier );

      /* Get the cache Connection buffer for info */
      pInfoBuffer = (uint8_t*)PCacheConnectionCreateBlock(
           pContext,
           pDescriptor->nIdentifier,
           P_SMART_CACHE_INFO_INDEX,
           nSectorNumber + 1 );

      /* Create the cache Connection buffer for tag data */
      pDataBuffer = (uint8_t*)PCacheConnectionCreateBlock(
           pContext,
           pDescriptor->nIdentifier,
           P_SMART_CACHE_DATA_INDEX,
           pDescriptor->pSectorSize->pMultiply(nSectorNumber) );

      if((pInfoBuffer == null) || (pDataBuffer == null))
      {
         PDebugError("PSmartCacheCreateCache: cannot create cache buffer");
         PCacheConnectionRemoveBlock( pContext, pDescriptor->nIdentifier );
         return W_ERROR_OUT_OF_RESOURCE;
      }
      else
      {
         pInfoBuffer[nSectorNumber] = 1;
         PCacheConnectionUpdateBuffer(pContext,
            pDescriptor->nIdentifier, P_SMART_CACHE_INFO_INDEX);
      }
   }

   pSmartCache->pInfoBuffer = pInfoBuffer;
   pSmartCache->pDataBuffer = pDataBuffer;
   pSmartCache->pDescriptor = pDescriptor;
   pSmartCache->pConnection = pConnection;
   pSmartCache->nSectorNumber = nSectorNumber;

   static_PSmartCacheResetOperation(pSmartCache);

   return W_SUCCESS;
}

/* See header file */
void PSmartCacheDestroyCache(
               tContext* pContext,
               tSmartCache* pSmartCache )
{
   if(pSmartCache->pDescriptor != null)
   {
      /* Flush the cache used for this connection */
      PCacheConnectionRemoveBlock(pContext, pSmartCache->pDescriptor->nIdentifier);
   }

   CMemoryFill(pSmartCache, 0, sizeof(tSmartCache));
}

/* See tPBasicGenericCallbackFunction */
static void static_PSmartCacheReadCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nError)
{
   tSmartCache* pSmartCache = (tSmartCache*)pCallbackParameter;

   PDebugTrace("static_PSmartCacheReadCompleted()");

   CDebugAssert(pSmartCache->nCurrentOperation != P_SMART_CACHE_OPERATION_NONE);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSmartCacheReadCompleted: receiving %s", PUtilTraceError(nError));
   }
   else
   {
      uint32_t nSectorIndex, nSectorNumber;

      /* Update the cache */
      CDebugAssert(pSmartCache->nOperationLength != 0);
      CDebugAssert(pSmartCache->pDescriptor->pSectorSize->pModulo(pSmartCache->nOperationLength) == 0);
      CDebugAssert(pSmartCache->pDescriptor->pSectorSize->pModulo(pSmartCache->nOperationOffset) == 0);

      CMemoryCopy(
         &pSmartCache->pDataBuffer[pSmartCache->nOperationOffset],
         pSmartCache->aCommandDataBuffer,
         pSmartCache->nOperationLength);
      PCacheConnectionUpdateBuffer( pContext,
         pSmartCache->pDescriptor->nIdentifier, P_SMART_CACHE_DATA_INDEX );

      nSectorIndex  = pSmartCache->pDescriptor->pSectorSize->pDivide(pSmartCache->nOperationOffset);
      nSectorNumber = pSmartCache->pDescriptor->pSectorSize->pDivide(pSmartCache->nOperationLength);

      while(nSectorNumber > 0)
      {
         pSmartCache->pInfoBuffer[nSectorIndex++] = 1;
         nSectorNumber--;
      }
      PCacheConnectionUpdateBuffer( pContext,
         pSmartCache->pDescriptor->nIdentifier, P_SMART_CACHE_INFO_INDEX );

      /* restart the read state machine if needed */
      if(static_PSmartCacheReadStateMachine(pContext, pSmartCache) != W_FALSE)
      {
         return;
      }

      if(pSmartCache->nCurrentOperation == P_SMART_CACHE_OPERATION_READ)
      {
         if(pSmartCache->pBuffer != null)
         {
            CMemoryCopy(
               pSmartCache->pBuffer,
               &pSmartCache->pDataBuffer[pSmartCache->nOffset],
               pSmartCache->nLength);
         }
      }
      else
      {
          CDebugAssert(pSmartCache->nCurrentOperation == P_SMART_CACHE_OPERATION_WRITE);

         /* restart the write state machine if needed */
         if(static_PSmartCacheWriteStateMachine(pContext, pSmartCache) != W_FALSE)
         {
            return;
         }
      }
   }

   static_PSmartCacheResetOperation(pSmartCache);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSmartCacheReadCompleted: returning %s", PUtilTraceError(nError));
   }

   /* Send the result */
   PDFCPostContext2(
      &pSmartCache->sCallbackContext,
      nError );
}

/* See tPBasicGenericCallbackFunction */
static void static_PSmartCacheWriteCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nError)
{
   tSmartCache* pSmartCache = (tSmartCache*)pCallbackParameter;

   PDebugTrace("static_PSmartCacheWriteCompleted()");

   CDebugAssert(pSmartCache->nCurrentOperation == P_SMART_CACHE_OPERATION_WRITE);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSmartCacheWriteCompleted: receiving %s", PUtilTraceError(nError));
   }
   else
   {
      /* Update the cache */
      CDebugAssert(pSmartCache->nOperationLength != 0);
      CDebugAssert(pSmartCache->pDescriptor->pSectorSize->pModulo(pSmartCache->nOperationLength) == 0);
      CDebugAssert(pSmartCache->pDescriptor->pSectorSize->pModulo(pSmartCache->nOperationOffset) == 0);

      CMemoryCopy(
         &pSmartCache->pDataBuffer[pSmartCache->nOperationOffset],
         pSmartCache->aCommandDataBuffer,
         pSmartCache->nOperationLength);
      PCacheConnectionUpdateBuffer( pContext,
         pSmartCache->pDescriptor->nIdentifier, P_SMART_CACHE_DATA_INDEX );

      /* restart the write state machine if needed */
      if(static_PSmartCacheWriteStateMachine(pContext, pSmartCache) != W_FALSE)
      {
         return;
      }
   }

   static_PSmartCacheResetOperation(pSmartCache);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSmartCacheWriteCompleted: returning %s", PUtilTraceError(nError));
   }

   /* Send the result */
   PDFCPostContext2(
      &pSmartCache->sCallbackContext,
      nError );
}

/**
 * Executes a step in the read state machine.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache instance.
 *
 * @return  W_TRUE if an operation is pending, W_FALSE if the read operation is completed.
 **/
static bool_t static_PSmartCacheReadStateMachine(
               tContext* pContext,
               tSmartCache* pSmartCache)
{
   uint32_t nSectorStart = pSmartCache->pDescriptor->pSectorSize->pDivide(pSmartCache->nOffset);
   uint32_t nSectorEnd = pSmartCache->pDescriptor->pSectorSize->pDivide(pSmartCache->nLength + pSmartCache->nOffset - 1);
   uint32_t aScore[P_SMART_CACHE_READ_FUNCTION_NUMBER];
   uint32_t aSectorOffset[P_SMART_CACHE_READ_FUNCTION_NUMBER];
   uint32_t nFunctionIndex, nBestFunctionIndex;
   uint32_t nScore;
   tSmartCacheReadDescriptor* pReadDescriptor;

   CMemoryFill(aSectorOffset, 0, P_SMART_CACHE_READ_FUNCTION_NUMBER);
   CMemoryFill(aScore, 0, P_SMART_CACHE_READ_FUNCTION_NUMBER);

   PDebugTrace(
      "static_PSmartCacheReadStateMachine() [ %d, %d ]",
      pSmartCache->nOffset, pSmartCache->nOffset + pSmartCache->nLength - 1 );

   /* Remove the sectors at the beginning of the area */
   while(nSectorStart <= nSectorEnd)
   {
      if(pSmartCache->pInfoBuffer[nSectorStart] == 0)
      {
         break;
      }
      nSectorStart++;
   }

   if(nSectorStart == nSectorEnd + 1)
   {
      PDebugTrace("static_PSmartCacheReadStateMachine: nothing to read");
      return W_FALSE;
   }

   /* Remove the sectors at the end of the area */
   while(nSectorStart <= nSectorEnd)
   {
      if(pSmartCache->pInfoBuffer[nSectorEnd] == 0)
      {
         break;
      }
      nSectorEnd--;
   }

   CDebugAssert(nSectorStart != nSectorEnd + 1);

   /* Select the best function to call */
   for(nFunctionIndex = 0; nFunctionIndex < P_SMART_CACHE_READ_FUNCTION_NUMBER; nFunctionIndex++)
   {
      uint32_t nSectorIndex;
      nScore = 0;
      pReadDescriptor = &pSmartCache->pDescriptor->aReadFunctionArray[nFunctionIndex];

      if((pReadDescriptor->pReadFunction != null)
      && (pReadDescriptor->nSectorNumber <= pSmartCache->nSectorNumber))
      {
         /* Compute the offset */
         aSectorOffset[nFunctionIndex] = nSectorStart - (nSectorStart % pReadDescriptor->nAlign);
         if((aSectorOffset[nFunctionIndex] + pReadDescriptor->nSectorNumber) > pSmartCache->nSectorNumber)
         {
            aSectorOffset[nFunctionIndex] = pSmartCache->nSectorNumber - pReadDescriptor->nSectorNumber;
            aSectorOffset[nFunctionIndex] = aSectorOffset[nFunctionIndex] - (aSectorOffset[nFunctionIndex] % pReadDescriptor->nAlign);
         }
         CDebugAssert((aSectorOffset[nFunctionIndex] % pReadDescriptor->nAlign) == 0);

         for(nSectorIndex = aSectorOffset[nFunctionIndex];
            nSectorIndex < aSectorOffset[nFunctionIndex] + pReadDescriptor->nSectorNumber;
            /* optimize the cache loading */
            /* use "nSectorIndex < nSectorEnd" to minimize the command length */
            nSectorIndex++)
         {
            if(pSmartCache->pInfoBuffer[nSectorIndex] == 0)
            {
               nScore++;
            }
         }
      }

      aScore[nFunctionIndex] = nScore;
   }

   nScore = 0;
   nBestFunctionIndex = (uint32_t)-1;
   for(nFunctionIndex = 0; nFunctionIndex < P_SMART_CACHE_READ_FUNCTION_NUMBER; nFunctionIndex++)
   {
      if(aScore[nFunctionIndex] > nScore)
      {
         nBestFunctionIndex = nFunctionIndex;
         nScore = aScore[nFunctionIndex];
      }
      else if(aScore[nFunctionIndex] == nScore)
      {
         if((nBestFunctionIndex < P_SMART_CACHE_READ_FUNCTION_NUMBER) &&
            (pSmartCache->pDescriptor->aReadFunctionArray[nBestFunctionIndex].nSectorNumber >
             pSmartCache->pDescriptor->aReadFunctionArray[nFunctionIndex].nSectorNumber))
         {
            nBestFunctionIndex = nFunctionIndex;
         }
      }
   }

   CDebugAssert(nBestFunctionIndex != (uint32_t)-1);

   pReadDescriptor = &pSmartCache->pDescriptor->aReadFunctionArray[nBestFunctionIndex];

   pSmartCache->nOperationLength = pSmartCache->pDescriptor->pSectorSize->pMultiply(pReadDescriptor->nSectorNumber);
   pSmartCache->nOperationOffset = pSmartCache->pDescriptor->pSectorSize->pMultiply(aSectorOffset[nBestFunctionIndex]);

   PDebugTrace("static_PSmartCacheReadStateMachine: calling READ-%d [ %d, %d ]",
         pReadDescriptor->nSectorNumber,
         pSmartCache->nOperationOffset,
         pSmartCache->nOperationOffset + pSmartCache->nOperationLength - 1);

   pReadDescriptor->pReadFunction(pContext,
      pSmartCache->pConnection,
      aSectorOffset[nBestFunctionIndex],
      pReadDescriptor->nSectorNumber,
      pSmartCache->aCommandDataBuffer,
      static_PSmartCacheReadCompleted, pSmartCache);

   return W_TRUE;
}

/**
 * Executes a step in the write state machine.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSmartCache  The smart cache instance.
 *
 * @return  W_TRUE if an operation is pending, W_FALSE if the write operation is completed.
 **/
static bool_t static_PSmartCacheWriteStateMachine(
               tContext* pContext,
               tSmartCache* pSmartCache)
{
   uint8_t* pBuffer = pSmartCache->pBuffer;
   uint32_t nOffset = pSmartCache->nOffset;
   uint32_t nLength = pSmartCache->nLength;
   uint32_t nFunctionIndex, nBestFunctionIndex;
   uint32_t nScore;
   tSmartCacheWriteDescriptor* pWriteDescriptor;
   uint32_t nByteIndex, nLimit;
   uint32_t aByteCommandOffset[P_SMART_CACHE_WRITE_FUNCTION_NUMBER];
   uint32_t aScore[P_SMART_CACHE_WRITE_FUNCTION_NUMBER];

   CMemoryFill(aByteCommandOffset, 0, P_SMART_CACHE_WRITE_FUNCTION_NUMBER);
   CMemoryFill(aScore, 0, P_SMART_CACHE_WRITE_FUNCTION_NUMBER);

   /* Restore the backup if needed */
   if(pSmartCache->nWriteBackupLength != 0)
   {
      pSmartCache->nOffset = pSmartCache->nWriteBackupOffset;
      pSmartCache->nLength = pSmartCache->nWriteBackupLength;
      pSmartCache->nWriteBackupOffset = 0;
      pSmartCache->nWriteBackupLength = 0;
   }

   pBuffer = pSmartCache->pBuffer;
   nOffset = pSmartCache->nOffset;
   nLength = pSmartCache->nLength;

   PDebugTrace(
      "static_PSmartCacheWriteStateMachine() [ %d, %d ]",
      nOffset, nOffset + nLength - 1 );

   while(nLength > 0)
   {
      if(pSmartCache->pDataBuffer[nOffset] == *pBuffer)
      {
         nOffset++;
         pBuffer++;
         nLength--;
      }
      else
      {
         break;
      }
   }

   if(nLength == 0)
   {
      PDebugTrace("static_PSmartCacheWriteStateMachine: nothing to write");
      return W_FALSE;
   }

   while(nLength > 0)
   {
      if(pSmartCache->pDataBuffer[nOffset + nLength - 1] == pBuffer[nLength - 1])
      {
         nLength--;
      }
      else
      {
         break;
      }
   }

   CDebugAssert(nLength != 0);

   pSmartCache->pBuffer = pBuffer;
   pSmartCache->nOffset = nOffset;
   pSmartCache->nLength = nLength;

   /* Select the best function to call */
   for(nFunctionIndex = 0; nFunctionIndex < P_SMART_CACHE_WRITE_FUNCTION_NUMBER; nFunctionIndex++)
   {
      nScore = 0;
      pWriteDescriptor = &pSmartCache->pDescriptor->aWriteFunctionArray[nFunctionIndex];

      if(pWriteDescriptor->pWriteFunction != null)
      {
         uint32_t nByteCommandAlign, nByteCommandLength, nByteCommandOffset;
         nByteCommandAlign = pSmartCache->pDescriptor->pSectorSize->pMultiply(pWriteDescriptor->nAlign);
         nByteCommandLength = pSmartCache->pDescriptor->pSectorSize->pMultiply(pWriteDescriptor->nSectorNumber);

         /* Compute the offset */
         nByteCommandOffset = nOffset - (nOffset % pSmartCache->pDescriptor->pSectorSize->pMultiply(pWriteDescriptor->nAlign));
         if((nByteCommandOffset + nByteCommandLength) >
            pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber))
         {
            nByteCommandOffset = pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber) - nByteCommandLength;
         }

         if( (nByteCommandOffset % nByteCommandAlign) != 0)
         {
            PDebugError("static_PSmartCacheWriteStateMachine : Should not arrive");
            CDebugAssert((nByteCommandOffset % nByteCommandAlign) == 0);
         }


         /* Check if all the data is already in the cache */
         pSmartCache->nWriteBackupOffset = pSmartCache->nOffset;
         pSmartCache->nWriteBackupLength = pSmartCache->nLength;
         pSmartCache->nOffset = nByteCommandOffset;
         pSmartCache->nLength = nByteCommandLength;
         if(static_PSmartCacheReadStateMachine(pContext, pSmartCache) != W_FALSE)
         {
            return W_TRUE;
         }
         pSmartCache->nOffset = pSmartCache->nWriteBackupOffset;
         pSmartCache->nWriteBackupOffset = 0;
         pSmartCache->nLength = pSmartCache->nWriteBackupLength;
         pSmartCache->nWriteBackupLength = 0;

         nLimit = nByteCommandOffset + nByteCommandLength;
         if(nLimit > (nOffset + nLength))
         {
            nLimit = nOffset + nLength;
         }
         for(nByteIndex = nOffset; nByteIndex < nLimit; nByteIndex++)
         {
            if(pSmartCache->pDataBuffer[nByteIndex] != pBuffer[nByteIndex-nOffset])
            {
               nScore++;
            }
         }

         aByteCommandOffset[nFunctionIndex] = nByteCommandOffset;
      }

      aScore[nFunctionIndex] = nScore;
   }

   nScore = 0;
   nBestFunctionIndex = (uint32_t)-1;
   for(nFunctionIndex = 0; nFunctionIndex < P_SMART_CACHE_WRITE_FUNCTION_NUMBER; nFunctionIndex++)
   {
      if(aScore[nFunctionIndex] > nScore)
      {
         nBestFunctionIndex = nFunctionIndex;
         nScore = aScore[nFunctionIndex];
      }
      else if(aScore[nFunctionIndex] == nScore)
      {
         if(pSmartCache->pDescriptor->aWriteFunctionArray[nBestFunctionIndex].nSectorNumber >
            pSmartCache->pDescriptor->aWriteFunctionArray[nFunctionIndex].nSectorNumber)
         {
            nBestFunctionIndex = nFunctionIndex;
         }
      }
   }

   CDebugAssert(nBestFunctionIndex != (uint32_t)-1);
   pWriteDescriptor = &pSmartCache->pDescriptor->aWriteFunctionArray[nBestFunctionIndex];

   pSmartCache->nOperationOffset = aByteCommandOffset[nBestFunctionIndex];
   pSmartCache->nOperationLength = pSmartCache->pDescriptor->pSectorSize->pMultiply(pWriteDescriptor->nSectorNumber);

   /* Compute the content of the command array */
   nLimit = pSmartCache->nOperationOffset + pSmartCache->nOperationLength;
   for(nByteIndex = pSmartCache->nOperationOffset;
       nByteIndex < nLimit;
       nByteIndex++)
   {
      uint8_t nByte;

      if((nByteIndex < nOffset)
      || (nByteIndex >= nOffset + nLength))
      {
         nByte = pSmartCache->pDataBuffer[nByteIndex];
      }
      else
      {
         nByte = pBuffer[nByteIndex - nOffset];
      }

      pSmartCache->aCommandDataBuffer[nByteIndex - pSmartCache->nOperationOffset] = nByte;
   }

   PDebugTrace("static_PSmartCacheWriteStateMachine: calling WRITE-%d [ %d, %d ]",
      pWriteDescriptor->nSectorNumber,
      pSmartCache->nOperationOffset,
      pSmartCache->nOperationOffset + pSmartCache->nOperationLength - 1);

   pWriteDescriptor->pWriteFunction(pContext,
      pSmartCache->pConnection,
      pSmartCache->pDescriptor->pSectorSize->pDivide(aByteCommandOffset[nBestFunctionIndex]),
      pWriteDescriptor->nSectorNumber,
      pSmartCache->aCommandDataBuffer,
      static_PSmartCacheWriteCompleted, pSmartCache);

   return W_TRUE;
}

/* See header file */
void PSmartCacheRead(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("PSmartCacheRead( nOffset=%d nLength=%d )", nOffset, nLength);

   PDFCFillCallbackContext(
       pContext,
       (tDFCCallback*)pCallback,
       pCallbackParameter,
       &sCallbackContext );

   if(pSmartCache->nCurrentOperation != P_SMART_CACHE_OPERATION_NONE)
   {
      PDebugError("PSmartCacheRead: Operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   if(nOffset + nLength >
      pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber))
   {
      PDebugError("PSmartCacheRead: Bad parameters");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   if(nLength == 0)
   {
      goto return_function;
   }

   pSmartCache->nCurrentOperation = P_SMART_CACHE_OPERATION_READ;
   pSmartCache->nOffset = nOffset;
   pSmartCache->nLength = nLength;
   pSmartCache->pBuffer = pBuffer;
   pSmartCache->sCallbackContext = sCallbackContext;

   if(static_PSmartCacheReadStateMachine(pContext, pSmartCache) != W_FALSE)
   {
      return;
   }

   if(pBuffer != null)
   {
      CMemoryCopy(pBuffer, &pSmartCache->pDataBuffer[nOffset], nLength);
   }

   static_PSmartCacheResetOperation(pSmartCache);

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("PSmartCacheRead: returning %s", PUtilTraceError(nError));
   }

   /* Send the result */
   PDFCPostContext2(
      &sCallbackContext,
      nError );
}

/* See header file */
uint8_t* PSmartCacheGetBuffer(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength)
{
   uint32_t nBlockStart, nBlockEnd;

   if((nOffset + nLength) >
      pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber))
   {
      PDebugError("PSmartCacheGetBuffer: bad parameter");
      return null;
   }

   nBlockStart = pSmartCache->pDescriptor->pSectorSize->pDivide(nOffset);
   nBlockEnd = pSmartCache->pDescriptor->pSectorSize->pDivide(nOffset + nLength - 1);

   while(nBlockStart <= nBlockEnd)
   {
      if(pSmartCache->pInfoBuffer[nBlockStart++] == 0)
      {
         PDebugError("PSmartCacheGetBuffer: part of the buffer is not in the cache");
         return null;
      }
   }

   return &pSmartCache->pDataBuffer[nOffset];
}

/* See header file */
void PSmartCacheWrite(
               tContext* pContext,
               tSmartCache* pSmartCache,
               uint32_t nOffset,
               uint32_t nLength,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("PSmartCacheWrite( nOffset=%d nLength=%d )", nOffset, nLength);

   PDFCFillCallbackContext(
       pContext,
       (tDFCCallback*)pCallback,
       pCallbackParameter,
       &sCallbackContext );

   if(pSmartCache->nCurrentOperation != P_SMART_CACHE_OPERATION_NONE)
   {
      PDebugError("PSmartCacheWrite: Operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   if((nOffset + nLength >
      pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber))
   || (pBuffer == null))
   {
      PDebugError("PSmartCacheWrite: Bad parameters");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   if(nLength == 0)
   {
      goto return_function;
   }

   pSmartCache->nCurrentOperation = P_SMART_CACHE_OPERATION_WRITE;
   pSmartCache->nOffset = nOffset;
   pSmartCache->nLength = nLength;
   pSmartCache->pBuffer = (uint8_t*)pBuffer;
   pSmartCache->sCallbackContext = sCallbackContext;

   /* First read the data in the cache if needed */
   if(static_PSmartCacheReadStateMachine(pContext, pSmartCache) != W_FALSE)
   {
      return;
   }

   /* then write the data if needed */
   if(static_PSmartCacheWriteStateMachine(pContext, pSmartCache) != W_FALSE)
   {
      return;
   }

   static_PSmartCacheResetOperation(pSmartCache);

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("PSmartCacheWrite: returning %s", PUtilTraceError(nError));
   }

   /* Send the result */
   PDFCPostContext2(
      &sCallbackContext,
      nError );
}

/* See header */
void PSmartCacheInvalidateCache(
   tContext* pContext,
   tSmartCache* pSmartCache,
   uint32_t     nOffset,
   uint32_t     nLength)
{
   uint32_t nBlockStart, nBlockEnd;

   if(pSmartCache->pDescriptor != null)
   {
      nBlockStart = pSmartCache->pDescriptor->pSectorSize->pDivide(nOffset);
      nBlockEnd = pSmartCache->pDescriptor->pSectorSize->pDivide(nOffset + nLength - 1);

      if ( (nBlockStart > pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber)) ||
           (nBlockEnd > pSmartCache->pDescriptor->pSectorSize->pMultiply(pSmartCache->nSectorNumber)))
      {
         PDebugError("PSmartCacheInvalidateCache: Bad parameters");
         return;
      }

      while (nBlockStart <= nBlockEnd)
      {
         pSmartCache->pInfoBuffer[nBlockStart++] = 0;
      }

      PCacheConnectionUpdateBuffer( pContext,
         pSmartCache->pDescriptor->nIdentifier, P_SMART_CACHE_INFO_INDEX );
   }
}

#endif /* P_CONFIG_P_CONFIG_USER || P_CONFIG_MONOLITHIC */

