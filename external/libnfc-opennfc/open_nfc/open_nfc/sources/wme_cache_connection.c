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

/*******************************************************************************
   Contains the implementation of the connection cach functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( CACHE )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define ALIGN(size, align) (((size) + ((align) - 1)) & ~((align) - 1))


/**
 * Describes cache connection block parameters
 **/
struct __tCacheBlock;

typedef struct __tCacheBlock
{
   uint32_t                          nIdentifier;
   uint32_t                          nIndex;
   uint32_t                          nBufferLength;
}tCacheBlock;

/**
 * Returns the following block.
 *
 * @param[in]  pCacheBlock  The current block.
 *
 * @return  The address of the following block.
 **/
static tCacheBlock* static_PCacheGetNextBlock(
            tCacheBlock* pCacheBlock)
{
   pCacheBlock = (tCacheBlock*)(((uint8_t*)pCacheBlock) + sizeof(tCacheBlock) + pCacheBlock->nBufferLength);
   return (tCacheBlock*)PUtilConvertUintToPointer(ALIGN(PUtilConvertPointerToUint(pCacheBlock), 4));
}

/* See header file */
void* PCacheConnectionCreateBlock(
            tContext* pContext,
            uint32_t nIdentifier,
            uint32_t nIndex,
            uint32_t nLength )
{
 /*
 +------------+----------+-----+--------+----------+--------+------+---------------+
 | block(x,i) | (x, i+1) | ... | (y, i) | (y, i+1) | (z, i) | ...  | block(z, i+2) |
 +------------+----------+-----+--------+----------+--------+------+---------------+
                                      cache memory representation
 */
   tCacheBlock* pCacheBlock;
   void* pNewBlock;
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);
   uint8_t* pCacheMemory = (uint8_t*)pCacheConnection->aCacheMemory;
   uintptr_t nLengthInUse;


   if((pCacheConnection->nTotalLengthInUse + sizeof(tCacheBlock) + nLength) > P_CACHE_CONNECTION_SIZE)
   {
      PDebugError("PCacheConnectionCreateBlock: not enought memory to create buffer of %d bytes index #%d identifier #%d\n",
                  nLength, nIndex, nIdentifier);
      return null;
   }

   /* get the last block of cache connection: nIndexLastBlock is aligned, do not need to align */
   pCacheBlock = (tCacheBlock*)&pCacheMemory[pCacheConnection->nTotalLengthInUse];

   /*set new conection cache block params */
   pCacheBlock->nIdentifier = nIdentifier;
   pCacheBlock->nIndex  = nIndex;
   pCacheBlock->nBufferLength = nLength;
   /* update the block linked list*/
   nLengthInUse = (uint8_t*)static_PCacheGetNextBlock(pCacheBlock) - pCacheMemory;
   CDebugAssert(nLengthInUse <= (uintptr_t)((uint32_t)-1));
   pCacheConnection->nTotalLengthInUse = (uint32_t)nLengthInUse;

   pNewBlock = (void*)(((uint8_t*)pCacheBlock) + sizeof(tCacheBlock));

   CMemoryFill(pNewBlock, 0, nLength);

   return pNewBlock;
}

/* See header file */
void* PCacheConnectionGetBuffer(
               tContext* pContext,
               uint32_t nIdentifier,
               uint32_t nIndex,
               uint32_t* pnLength )
{
   tCacheBlock* pCacheBlock;
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);
   uint8_t* pCacheMemory = (uint8_t*)pCacheConnection->aCacheMemory;

   pCacheBlock = (tCacheBlock *)pCacheMemory;

   while(((uint8_t*)pCacheBlock) < (pCacheMemory + pCacheConnection->nTotalLengthInUse))
   {
      if((pCacheBlock->nIdentifier == nIdentifier) &&
         (pCacheBlock->nIndex == nIndex))
      {
         *pnLength = pCacheBlock->nBufferLength;
         return  ((uint8_t*)pCacheBlock) + sizeof(tCacheBlock);
      }
      /* get the next block*/
      pCacheBlock = static_PCacheGetNextBlock(pCacheBlock);
   }
   return null;
}

/* See header file */
void PCacheConnectionUpdateBuffer(
                   tContext* pContext,
                   uint32_t nIdentifier,
                   uint32_t nIndex )
{
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);

   pCacheConnection->bIsCacheModified = W_TRUE;
}

/* See header file */
void PCacheConnectionUserCommit(
               tContext* pContext )
{
#if (P_BUILD_CONFIG == P_CONFIG_USER)
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);
   W_ERROR nError;
   if(pCacheConnection->bIsCacheModified != W_FALSE)
   {
      nError = PCacheConnectionDriverWrite(
                           pContext,
                           pCacheConnection,
                           sizeof(tCacheConnectionInstance));

      if (nError != W_SUCCESS)
      {
         /*@todo What can we do here if the IOCTL failed ? */
         PDebugError("PCacheConnectionUserCommit : PCacheConnectionDriverWrite failed %d\n", nError);
      }
      else
      {
         pCacheConnection->bIsCacheModified = W_FALSE;
      }
   }
#endif /* P_CONFIG_P_CONFIG_USER */
}

/* See header file */
void PCacheConnectionUserUpdate(
               tContext* pContext )
{
#if (P_BUILD_CONFIG == P_CONFIG_USER)
   /* Update the User Connection Cache*/
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);
   W_ERROR nError;

   nError = PCacheConnectionDriverRead(
                         pContext,
                         pCacheConnection,
                         sizeof(tCacheConnectionInstance));

   if (nError != W_SUCCESS)
   {
      /* @todo : what can we do here if the IOCTL failed */
      PDebugError("PCacheConnectionUserUpdate : PCacheConnectionDriverRead failed %d\n", nError);
   }

   pCacheConnection->bIsCacheModified = W_FALSE;
#endif /* P_CONFIG_P_CONFIG_USER */
}

/* See header file */
void PCacheConnectionRemoveBlock(
            tContext* pContext,
            uint32_t nIdentifier)
{
   tCacheBlock* pCacheBlock;
   tCacheBlock* pNextCacheBlock;
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);
   uint8_t* pCacheMemory = (uint8_t*)pCacheConnection->aCacheMemory;

   pCacheBlock = (tCacheBlock *)pCacheMemory;

   while(((uint8_t*)pCacheBlock) < (pCacheMemory + pCacheConnection->nTotalLengthInUse))
   {
      /* get the next block*/
      pNextCacheBlock = static_PCacheGetNextBlock(pCacheBlock);

      if(pCacheBlock->nIdentifier != nIdentifier)
      {
         /* get the next block*/
         pCacheBlock = pNextCacheBlock;
      }
      else
      {
         uintptr_t nLength;
         CDebugAssert(((uint8_t*)pNextCacheBlock) <= (pCacheMemory + pCacheConnection->nTotalLengthInUse));

         nLength = (pCacheMemory + pCacheConnection->nTotalLengthInUse) - ((uint8_t*)pNextCacheBlock);
         if(nLength != 0)
         {
            CDebugAssert(nLength <= ((uint32_t)-1));
            CMemoryMove(pCacheBlock, pNextCacheBlock, (uint32_t)nLength);
         }
         nLength = (uint8_t*)pNextCacheBlock - (uint8_t*)pCacheBlock;
         CDebugAssert(nLength <= (uintptr_t)((uint32_t)-1));
         pCacheConnection->nTotalLengthInUse -= (uint32_t)nLength;
      }
   }
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
/* See header file */
void PCacheConnectionReset(
            tContext* pContext)
{
   tCacheConnectionInstance* pCacheConnection = PContextGetCacheConnectionInstance(pContext);
   /* reset Cache params*/
   pCacheConnection->nTotalLengthInUse = 0;
   pCacheConnection->bIsCacheModified = W_FALSE;
}

#endif /* P_CONFIG_DIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/* see driver.h header file */
W_ERROR PCacheConnectionDriverRead(
            tContext* pContext,
            tCacheConnectionInstance *pCacheConnection,
            uint32_t nSize )
{
   tCacheConnectionInstance* pDriverCacheConnection = PContextGetCacheConnectionInstance(pContext);
   if(nSize != sizeof(tCacheConnectionInstance))
   {
      PDebugError("PCacheConnectionDriverRead: unexpected size value nSize (%d) != P_CACHE_CONNECTION_SIZE", nSize);
      return W_ERROR_BAD_PARAMETER;
   }
   else
   {
      pCacheConnection->nTotalLengthInUse = pDriverCacheConnection->nTotalLengthInUse;
      pCacheConnection->bIsCacheModified = W_FALSE;
      CDebugAssert(pDriverCacheConnection->nTotalLengthInUse <= P_CACHE_CONNECTION_SIZE);
      CMemoryCopy(
         pCacheConnection->aCacheMemory,
         pDriverCacheConnection->aCacheMemory,
         pDriverCacheConnection->nTotalLengthInUse);

      return W_SUCCESS;
   }
}

/* see driver.h header file */
W_ERROR PCacheConnectionDriverWrite(
            tContext* pContext,
            const tCacheConnectionInstance *pCacheConnection,
            uint32_t nSize )
{
   tCacheConnectionInstance* pDriverCacheConnection = PContextGetCacheConnectionInstance(pContext);
   if(nSize != sizeof(tCacheConnectionInstance))
   {
      PDebugError("PCacheConnectionDriverWrite: unexpected size value nSize (%d) != P_CACHE_CONNECTION_SIZE", nSize);
      return W_ERROR_BAD_PARAMETER;
   }
   else
   {
      if(pCacheConnection->nTotalLengthInUse > P_CACHE_CONNECTION_SIZE)
      {
         PDebugError("PCacheConnectionDriverWrite: wrong data lentgh");
         return W_ERROR_BAD_PARAMETER;
      }
      else
      {
         pDriverCacheConnection->nTotalLengthInUse = pCacheConnection->nTotalLengthInUse;
         CMemoryCopy(
            pDriverCacheConnection->aCacheMemory,
            pCacheConnection->aCacheMemory,
            pCacheConnection->nTotalLengthInUse);

         return W_SUCCESS;
      }
   }
}

#endif /* P_CONFIG_DRIVER  */
