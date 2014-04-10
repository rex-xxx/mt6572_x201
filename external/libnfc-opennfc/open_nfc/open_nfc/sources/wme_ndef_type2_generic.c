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
   Contains the implementation of the generic NDEF type2 functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( NDEFA2G )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Smart cache read/write block number and align */
#define P_NDEF2GEN_READ_BLOCK_NUMBER               4
#define P_NDEF2GEN_READ_BLOCK_ALIGN                1
#define P_NDEF2GEN_WRITE_BLOCK_NUMBER              1
#define P_NDEF2GEN_WRITE_BLOCK_ALIGN               1

/* Smart caches identifiers */
#define P_NDEF2GEN_IDENTIFIER_LEVEL                ZERO_IDENTIFIER_LEVEL
#define P_NDEF2GEN_IDENTIFIER_LEVEL_DYNAMIC        ZERO_IDENTIFIER_LEVEL + 1

/* Size of a sector in blocks (256 blocks = 1024 bytes) */
#define P_NDEF2GEN_SECTOR_SIZE                     256

/* State of SelectSectorReadWrite automaton */
#define P_NDEF2GEN_STATE_SELECT_SECTOR_1           1
#define P_NDEF2GEN_STATE_SELECT_SECTOR_2           2
#define P_NDEF2GEN_STATE_BEGIN_READ_WRITE          3
#define P_NDEF2GEN_STATE_READ_WRITE                4

/* Queued operation type */
#define P_NDEF2GEN_QUEUED_NONE                     0
#define P_NDEF2GEN_QUEUED_READ                     1
#define P_NDEF2GEN_QUEUED_WRITE                    2
#define P_NDEF2GEN_QUEUED_DIRECT_WRITE             3

/* Declare a generic NDEF Type2 exchange data structure */
typedef struct __tNDEF2GenConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* Connection handle */
   W_HANDLE                   hConnection;

   /* Tag info */
#if 0
   uint16_t                   ATQA;                /* RFU */
   uint8_t                    nSAK;                /* RFU */
#endif /* 0 */
   uint8_t                    nUIDLength;
   uint8_t                    aUID[7];

   uint32_t                   nSectorNumber;

   /* smart cache */
   tSmartCache                sSmartCacheStatic;   /* static area (64 bytes) */
   tSmartCache                sSmartCacheDynamic;  /* dynamic area (>64 bytes) */
   tSmartCacheSectorSize*     pSectorSize;

   /* to read / write in dynamic area */
   uint32_t                   nDynamicLength;
   uint8_t*                   pDynamicBuffer;

   /* Command buffer */
   uint8_t                    aReaderToCardBuffer[NAL_MESSAGE_MAX_LENGTH];

   /* Save the current sector number selected with SECTOR SELECT */
   uint32_t                   nCurrentSector;

   /* Used in SelectSectorReadWrite automaton */
   uint32_t                   nCurrentOperationState;
   bool_t                       bWrite;
   uint32_t                   nSectorToSelect;
   uint32_t                   nOffset;
   uint32_t                   nSize;
   uint8_t*                   pBuffer;

   /* Used in to Invalidate MyDNFC Data */

   /* Callback context */
   tDFCCallbackContext        sCallbackContext;
   tDFCCallbackContext        sCacheCallbackContext;

} tNDEF2GenConnection;

/**
 * @brief   Destroyes a generic NDEF Type2 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PNDEF2GenDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pObject;

   PDebugTrace("static_PNDEF2GenDestroyConnection");

   PDFCFlushCall(&pNDEF2GenConnection->sCallbackContext);

   /* Free the generic NDEF Type2 connection structure */
   CMemoryFree( pNDEF2GenConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the generic NDEF Type2 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 **/

static uint32_t static_PNDEF2GenGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/**
 * @brief   Gets the generic NDEF Type2 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PNDEF2GenGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   pPropertyArray[0] = W_PROP_NFC_TAG_TYPE_2_GENERIC;
   return W_TRUE;
}

/**
 * @brief   Checkes the generic NDEF Type2 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PNDEF2GenCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   return  (W_PROP_NFC_TAG_TYPE_2_GENERIC != nPropertyValue) ? W_FALSE : W_TRUE;
}

/* Send polling command */
static void static_PNDEF2GenPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Handle registry generic NDEF Type2 connection type */
tHandleType g_sNDEF2GenConnection = { static_PNDEF2GenDestroyConnection,
                                      null,
                                      static_PNDEF2GenGetPropertyNumber,
                                      static_PNDEF2GenGetProperties,
                                      static_PNDEF2GenCheckProperties,
                                      null, null, null,
                                      static_PNDEF2GenPoll };

#define P_HANDLE_TYPE_NDEF2GEN_CONNECTION (&g_sNDEF2GenConnection)

static void static_PNDEF2GenSelectSectorReadWriteAutomaton(
      tContext * pContext,
      void     * pCallbackParameter,
      uint32_t   nDataLength,
      W_ERROR    nError);


/**
 * @brief   Sends a SECTOR SELECT packet 1 command to the card.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEF2GenConnection  The generic NDEF Type2 connection.
 **/
static void static_PNDEF2GenSelectSectorPart1(
               tContext* pContext,
               tNDEF2GenConnection* pNDEF2GenConnection)
{
   static const uint8_t pSelectSector1[] = { 0xC2, 0xFF };

   PDebugTrace("static_PNDEF2GenSelectSectorPart1");

   /* Prepare the command */
   CMemoryCopy(
      pNDEF2GenConnection->aReaderToCardBuffer,
      pSelectSector1, sizeof(pSelectSector1) );

   /* Send the command */
   P14P3UserExchangeData(
      pContext,
      pNDEF2GenConnection->hConnection,
      static_PNDEF2GenSelectSectorReadWriteAutomaton, pNDEF2GenConnection,
      pNDEF2GenConnection->aReaderToCardBuffer, sizeof(pSelectSector1),
      null, 0, /* no answer data */
      null,
      W_FALSE, /* no answer crc */
      W_TRUE /* ack or nack */);
}

/**
 * @brief   Sends a SECTOR SELECT packet 2 command to the card.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEF2GenConnection  The generic NDEF Type2 connection.
 *
 * @param[in]  nSector  The sector number.
 **/
static void static_PNDEF2GenSelectSectorPart2(
               tContext* pContext,
               tNDEF2GenConnection* pNDEF2GenConnection,
               uint32_t nSector)
{
   static const uint8_t pSelectSector2[] = { 0x00, 0x00, 0x00, 0x00 };

   PDebugTrace("static_PNDEF2GenSelectSectorPart2: %d", nSector);

   /* Prepare the command */
   CMemoryCopy(
      pNDEF2GenConnection->aReaderToCardBuffer,
      pSelectSector2, sizeof(pSelectSector2) );

   /* copy the sector number */
   pNDEF2GenConnection->aReaderToCardBuffer[0] = (uint8_t)nSector;

   /* To answer this command, NDEF Type 2 tag uses passive ACK. No answer (timeout) within 1ms = ACK.
      Set Timeout value 2 = 1.21ms */
   P14Part3SetTimeout(pContext, pNDEF2GenConnection->hConnection, 2);

   /* Send the command */
   P14P3UserExchangeData(
      pContext,
      pNDEF2GenConnection->hConnection,
      static_PNDEF2GenSelectSectorReadWriteAutomaton, pNDEF2GenConnection,
      pNDEF2GenConnection->aReaderToCardBuffer, sizeof(pSelectSector2),
      null, 0, /* no answer data */
      null,
      W_FALSE,   /* no answer crc */
      W_FALSE);
}

/**
 * @brief   Sends a WRITE command to the card.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEF2GenConnection  The generic NDEF Type2 connection.
 *
 * @param[in]  nSectorOffset  the first sector number to write.
 *
 * @param[in]  nSectorNumber  The number of sectors to write (must be one).
 *
 * @param[in]  pBuffer  The pointer to the data to write.
 **/
static void static_PNDEF2GenWriteRaw(
               tContext* pContext,
               tNDEF2GenConnection* pNDEF2GenConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t* pBuffer)
{
   static const uint8_t pWrite[] = { 0xA2, 0x00 /* Block number */ };

   PDebugTrace("static_PNDEF2GenWriteRaw() WRITE %d", nSectorNumber);

   /* Prepare the command */
   CMemoryCopy(
      pNDEF2GenConnection->aReaderToCardBuffer,
      pWrite, sizeof(pWrite) );

   /* Copy the block number */
   pNDEF2GenConnection->aReaderToCardBuffer[1] = (uint8_t)nSectorOffset;

   /* Copy data to write */
   CMemoryCopy(
      &pNDEF2GenConnection->aReaderToCardBuffer[2],
      pBuffer, pNDEF2GenConnection->pSectorSize->pMultiply(nSectorNumber) );

   /* Send the command */
   P14P3UserExchangeData(
      pContext,
      pNDEF2GenConnection->hConnection,
      static_PNDEF2GenSelectSectorReadWriteAutomaton, pNDEF2GenConnection,
      pNDEF2GenConnection->aReaderToCardBuffer,
      sizeof(pWrite) + pNDEF2GenConnection->pSectorSize->nValue /* Data */,
      null, 0, /* no answer data */
      null,
      W_FALSE,   /* no answer crc */
      W_TRUE);   /* ack or nack answer */
}

/**
 * @brief   Sends a READ command to the card.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEF2GenConnection  The generic NDEF Type2 connection.
 *
 * @param[in]  nSectorOffset  the first sector number to read.
 *
 * @param[in]  nSectorNumber  The number of sectors to read (must be four).
 *
 * @param[in]  pBuffer  The pointer to receive the read data.
 **/
static void static_PNDEF2GenReadRaw(
               tContext* pContext,
               tNDEF2GenConnection* pNDEF2GenConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer)
{
   static const uint8_t pRead[] = { 0x30, 0x00 /* Block number */ };

   PDebugTrace("static_PNDEF2GenReadRaw() READ %d", nSectorNumber);

   /* Prepare the command */
   CMemoryCopy(
      pNDEF2GenConnection->aReaderToCardBuffer,
      pRead, sizeof(pRead) );

   /* Copy the block number */
   pNDEF2GenConnection->aReaderToCardBuffer[1] = (uint8_t)nSectorOffset;

   /* Send the command */
   P14P3UserExchangeData(
      pContext,
      pNDEF2GenConnection->hConnection,
      static_PNDEF2GenSelectSectorReadWriteAutomaton, pNDEF2GenConnection,
      pNDEF2GenConnection->aReaderToCardBuffer, sizeof(pRead),
      pBuffer, pNDEF2GenConnection->pSectorSize->pMultiply(P_NDEF2GEN_READ_BLOCK_NUMBER),
      null,
      W_TRUE, W_FALSE);
}

/* read/write automaton including sector select */
static void static_PNDEF2GenSelectSectorReadWriteAutomaton(
      tContext * pContext,
      void     * pCallbackParameter,
      uint32_t   nDataLength,
      W_ERROR    nError)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEF2GenSelectSectorReadWriteAutomaton: step %d", pNDEF2GenConnection->nCurrentOperationState);

   if (P_NDEF2GEN_STATE_SELECT_SECTOR_2 == pNDEF2GenConnection->nCurrentOperationState)
   {
      /* A special management of answer is needed for SECTOR SELECT packet 2 */
      if (W_ERROR_TIMEOUT == nError)
      {
         /* No answer = passive ACK */
         nError = W_SUCCESS;
      }
      else if (W_SUCCESS == nError)
      {
         /* Any answer = NACK */
         nError = W_ERROR_RF_COMMUNICATION;
      }

      /* Restore the default timeout value */
      P14Part3SetTimeout(pContext, pNDEF2GenConnection->hConnection, P_14443_3_A_DEFAULT_TIMEOUT);
   }

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PNDEF2GenSelectSectorReadWriteAutomaton : nError %d", nError);
      PDFCPostContext2(&pNDEF2GenConnection->sCacheCallbackContext, nError);
      return;
   }

   switch (pNDEF2GenConnection->nCurrentOperationState)
   {
      case P_NDEF2GEN_STATE_SELECT_SECTOR_1 :
         /* SECTOR SELECT packet 1 is done */
         pNDEF2GenConnection->nCurrentOperationState = P_NDEF2GEN_STATE_SELECT_SECTOR_2;
         static_PNDEF2GenSelectSectorPart2(
               pContext,
               pNDEF2GenConnection,
               pNDEF2GenConnection->nSectorToSelect);
         break;

      case P_NDEF2GEN_STATE_SELECT_SECTOR_2 :
         /* SECTOR SELECT packet2 is done */
         pNDEF2GenConnection->nCurrentOperationState = P_NDEF2GEN_STATE_BEGIN_READ_WRITE;
         /* Save the selected sector */
         pNDEF2GenConnection->nCurrentSector = pNDEF2GenConnection->nSectorToSelect;
         /* No break. Go directly to the next step */

      case P_NDEF2GEN_STATE_BEGIN_READ_WRITE:
         /* start to read/write */
         pNDEF2GenConnection->nCurrentOperationState = P_NDEF2GEN_STATE_READ_WRITE;
         if (pNDEF2GenConnection->bWrite != W_FALSE)
         {
            static_PNDEF2GenWriteRaw(
               pContext,
               pNDEF2GenConnection,
               pNDEF2GenConnection->nOffset % P_NDEF2GEN_SECTOR_SIZE,
               pNDEF2GenConnection->nSize,
               pNDEF2GenConnection->pBuffer);
         }
         else
         {
            static_PNDEF2GenReadRaw(
               pContext,
               pNDEF2GenConnection,
               pNDEF2GenConnection->nOffset % P_NDEF2GEN_SECTOR_SIZE,
               pNDEF2GenConnection->nSize,
               pNDEF2GenConnection->pBuffer);
         }
         break;

      case P_NDEF2GEN_STATE_READ_WRITE :
         /* Read/Write is done */
         PDFCPostContext2(&pNDEF2GenConnection->sCacheCallbackContext, nError);
         break;
   }
}

/**
 * @brief   Common function to read or write in the tag. Sends a SECTOR SELECT before read/write if needed.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEF2GenConnection  The generic NDEF Type2 connection.
 *
 * @param[in]  nSectorOffset  the first sector number to read/write.
 *
 * @param[in]  nSectorNumber  The number of sectors to read/write.
 *
 * @param[in]  pBuffer  The pointer to the data.
 *
 * @param[in]  bWrite  W_TRUE = write. W_FALSE = read.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 **/
static void static_PNDEF2GenSelectSectorReadWrite(
               tContext* pContext,
               tNDEF2GenConnection* pNDEF2GenConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer,
               bool_t bWrite,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   PDebugTrace("static_PNDEF2GenSelectSectorReadWrite");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pNDEF2GenConnection->sCacheCallbackContext);

   /* Compute the sector to select */
   pNDEF2GenConnection->nSectorToSelect = nSectorOffset / P_NDEF2GEN_SECTOR_SIZE;

   /* Save parameters for read/write */
   pNDEF2GenConnection->nOffset = nSectorOffset;
   pNDEF2GenConnection->nSize = nSectorNumber;
   pNDEF2GenConnection->pBuffer = pBuffer;
   pNDEF2GenConnection->bWrite = bWrite;

   /* Check if the sector to select is not the currently selected one. */
   if (pNDEF2GenConnection->nSectorToSelect != pNDEF2GenConnection->nCurrentSector)
   {
      /* Select Sector */
      pNDEF2GenConnection->nCurrentOperationState = P_NDEF2GEN_STATE_SELECT_SECTOR_1;
      static_PNDEF2GenSelectSectorPart1(pContext, pNDEF2GenConnection);
   }
   else
   {
      /* Skip sector selection */
      pNDEF2GenConnection->nCurrentOperationState = P_NDEF2GEN_STATE_BEGIN_READ_WRITE;
      static_PNDEF2GenSelectSectorReadWriteAutomaton(pContext, pNDEF2GenConnection, 0, W_SUCCESS);
   }
}

/* See tSmartCacheWriteSector */
static void static_PNDEF2GenWrite(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pConnection;

   PDebugTrace(
      "static_PNDEF2GenWrite() WRITE-%d [ %d, %d ]",
      nSectorNumber,
      pNDEF2GenConnection->pSectorSize->pMultiply(nSectorOffset),
      pNDEF2GenConnection->pSectorSize->pMultiply(nSectorOffset + nSectorNumber) - 1 );

   static_PNDEF2GenSelectSectorReadWrite(
      pContext,
      pNDEF2GenConnection,
      nSectorOffset,
      nSectorNumber,
      (uint8_t*)pBuffer,
      W_TRUE,
      pCallback,
      pCallbackParameter);
}

/* See tSmartCacheReadSector */
static void static_PNDEF2GenRead(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pConnection;

   PDebugTrace(
      "static_PNDEF2GenRead() READ-%d [ %d, %d ]",
      nSectorNumber,
      pNDEF2GenConnection->pSectorSize->pMultiply(nSectorOffset),
      pNDEF2GenConnection->pSectorSize->pMultiply(nSectorOffset + nSectorNumber) - 1 );

   static_PNDEF2GenSelectSectorReadWrite(
      pContext,
      pNDEF2GenConnection,
      nSectorOffset,
      nSectorNumber,
      pBuffer,
      W_FALSE,
      pCallback,
      pCallbackParameter);
}

/* See tSmartCacheWriteSector */
static void static_PNDEF2GenWriteDynamic(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   static_PNDEF2GenWrite(
      pContext,
      pConnection,
      nSectorOffset + P_NDEF2GEN_STATIC_BLOCK_NUMBER,
      nSectorNumber,
      pBuffer,
      pCallback,
      pCallbackParameter);
}

/* See tSmartCacheReadSector */
static void static_PNDEF2GenReadDynamic(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{

   PDebugTrace("static_PNDEF2GenReadDynamic");
   static_PNDEF2GenRead(
      pContext,
      pConnection,
      nSectorOffset + P_NDEF2GEN_STATIC_BLOCK_NUMBER,
      nSectorNumber,
      pBuffer,
      pCallback,
      pCallbackParameter);
}

/* data for smart caches */
extern tSmartCacheSectorSize g_sSectorSize4;

static tSmartCacheDescriptor g_sDescriptorNDEF2GenStatic = {
   P_NDEF2GEN_IDENTIFIER_LEVEL, &g_sSectorSize4,
   {
      { P_NDEF2GEN_READ_BLOCK_ALIGN, P_NDEF2GEN_READ_BLOCK_NUMBER, static_PNDEF2GenRead },
      { 0, 0, null }
   },
   {
      { P_NDEF2GEN_WRITE_BLOCK_ALIGN, P_NDEF2GEN_WRITE_BLOCK_NUMBER, static_PNDEF2GenWrite },
      { 0, 0, null }
   },
};

static tSmartCacheDescriptor g_sDescriptorNDEF2GenDynamic = {
   P_NDEF2GEN_IDENTIFIER_LEVEL_DYNAMIC, &g_sSectorSize4,
   {
      { P_NDEF2GEN_READ_BLOCK_ALIGN, P_NDEF2GEN_READ_BLOCK_NUMBER, static_PNDEF2GenReadDynamic },
      { 0, 0, null }
   },
   {
      { P_NDEF2GEN_WRITE_BLOCK_ALIGN, P_NDEF2GEN_WRITE_BLOCK_NUMBER, static_PNDEF2GenWriteDynamic },
      { 0, 0, null }
   },
};

/**
 * @brief   Sends the result.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pNDEF2GenConnection  The generic NDEF Type2 connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEF2GenSendResult(
            tContext* pContext,
            tNDEF2GenConnection* pNDEF2GenConnection,
            W_ERROR nError )
{
   PDebugTrace("static_PNDEF2GenSendResult");

   /* Send the error */
   PDFCPostContext2(&pNDEF2GenConnection->sCallbackContext, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pNDEF2GenConnection);
}

/* See Header file */
W_ERROR PNDEF2GenCheckType2(
            tContext* pContext,
            W_HANDLE hConnection )
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   W_ERROR nError;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF2GEN_CONNECTION, (void**)&pNDEF2GenConnection);
   if (W_SUCCESS != nError)
   {
      PDebugError("PNDEF2GenCheckType2: could not get pNDEF2GenConnection buffer");
      return nError;
   }

   return W_SUCCESS;
}

/* See Header file */
void PNDEF2GenCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PNDEF2GenCreateConnection");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the connection buffer */
   pNDEF2GenConnection = (tNDEF2GenConnection*)CMemoryAlloc( sizeof(tNDEF2GenConnection) );
   if ( pNDEF2GenConnection == null )
   {
      PDebugError("PNDEF2GenCreateConnection: pNDEF2GenConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pNDEF2GenConnection, 0, sizeof(tNDEF2GenConnection));

   /* Check the 14443-3 information level */
   if ( ( nError = P14P3UserCheckNDEF2Gen(
                     pContext,
                     hConnection,
                     pNDEF2GenConnection->aUID,
                     &pNDEF2GenConnection->nUIDLength) ) != W_SUCCESS )
   {
      PDebugLog("PNDEF2GenCreateConnection: not a generic NDEF Type2 card");
      goto return_error;
   }

   /* Create the smart cache for the static area (64 bytes = 16 blocks) */
   pNDEF2GenConnection->nSectorNumber = P_NDEF2GEN_STATIC_BLOCK_NUMBER;

   if( (nError = PSmartCacheCreateCache(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheStatic,
                  pNDEF2GenConnection->nSectorNumber,
                  &g_sDescriptorNDEF2GenStatic,
                  pNDEF2GenConnection) ) != W_SUCCESS)
   {
      PDebugError("PNDEF2GenCreateConnection: error creating the smart cache for the static area");
      goto return_error;
   }

   /* Store a pointer to pSectorSize (convenient to use helpers: multiply...) */
   pNDEF2GenConnection->pSectorSize = g_sDescriptorNDEF2GenStatic.pSectorSize;

   /* Add the generic NDEF Type2 connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hConnection,
                     pNDEF2GenConnection,
                     P_HANDLE_TYPE_NDEF2GEN_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PNDEF2GenCreateConnection: error returned by PHandleAddHeir()");
      goto return_error;
   }

   /* Store the connection information */
   pNDEF2GenConnection->hConnection = hConnection;

   PDFCPostContext2(&sCallbackContext, W_SUCCESS);

   return;

return_error:

   if (nError != W_ERROR_CONNECTION_COMPATIBILITY)
   {
      PDebugError("PNDEF2GenCreateConnection: return error %s", PUtilTraceError(nError));
   }

   if (pNDEF2GenConnection != null)
   {
      if (pNDEF2GenConnection->sSmartCacheStatic.pDescriptor != null)
      {
         PSmartCacheDestroyCache(pContext, &pNDEF2GenConnection->sSmartCacheStatic);
      }

      CMemoryFree(pNDEF2GenConnection);
   }

   PDFCPostContext2(&sCallbackContext, nError);
}

/** See tPReaderUserRemoveSecondaryConnection */
void PNDEF2GenRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection )
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)PHandleRemoveLastHeir(
            pContext, hUserConnection,
            P_HANDLE_TYPE_NDEF2GEN_CONNECTION);

   PDebugTrace("PNDEF2GenRemoveConnection");

   if (pNDEF2GenConnection != null)
   {
      if (pNDEF2GenConnection->sSmartCacheDynamic.pDescriptor != null)
      {
         PSmartCacheDestroyCache(pContext, &pNDEF2GenConnection->sSmartCacheDynamic);
      }

      if (pNDEF2GenConnection->sSmartCacheStatic.pDescriptor != null)
      {
         PSmartCacheDestroyCache(pContext, &pNDEF2GenConnection->sSmartCacheStatic);
      }

      CMemoryFree(pNDEF2GenConnection);
   }
}

/* See Header file */
W_ERROR PNDEF2GenCreateSmartCacheDynamic(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nSectorNumber)
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   W_ERROR nError;

   PDebugTrace("PNDEF2GenCreateSmartCacheDynamic");

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF2GEN_CONNECTION, (void**)&pNDEF2GenConnection);
   if ( nError != W_SUCCESS )
   {
      PDebugError("PNDEF2GenCreateSmartCacheDynamic: Bad handle");
      return nError;
   }

   /* Check if a dynamic smart cache already exists for this connection */
   if(pNDEF2GenConnection->sSmartCacheDynamic.pDescriptor != null)
   {
      PDebugTrace("PNDEF2GenCreateSmartCacheDynamic: the smart cache for the dynamic area already exists");
   }
   else
   {
      /* Check parameter */
      if (nSectorNumber <= pNDEF2GenConnection->nSectorNumber)
      {
         PDebugError("PNDEF2GenCreateSmartCacheDynamic: nSectorNumber = %d is smaller than the static smart cache size", nSectorNumber);
         return W_ERROR_BAD_PARAMETER;
      }

      /* Create the smart cache for the dynamic area */
      if((nError = PSmartCacheCreateCache(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheDynamic,
                  nSectorNumber - pNDEF2GenConnection->nSectorNumber,
                  &g_sDescriptorNDEF2GenDynamic,
                  pNDEF2GenConnection)) != W_SUCCESS)
      {
         PDebugError("PNDEF2GenCreateSmartCacheDynamic: error creating the smart cache");
         return nError;
      }

      /* Save the new size of tag (static + dynamic) */
      pNDEF2GenConnection->nSectorNumber = nSectorNumber;
   }

   return W_SUCCESS;
}

/* See header file */
W_ERROR PNDEF2GenInvalidateCache(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength)
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   W_ERROR nError;

   PDebugTrace("PNDEF2GenInvalidateCache");

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF2GEN_CONNECTION, (void**)&pNDEF2GenConnection);
   if ( nError != W_SUCCESS )
   {
      PDebugError("PNDEF2GenInvalidateCache: Bad handle");
      return nError;
   }

   /* Check the parameters */
   if (0 == nLength)
   {
      PDebugError("PNDEF2GenInvalidateCache: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nOffset + nLength) > pNDEF2GenConnection->pSectorSize->pMultiply(pNDEF2GenConnection->nSectorNumber) )
   {
      PDebugError("PNDEF2GenInvalidateCache: the data to invalidate is too large");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nOffset < P_NDEF2GEN_STATIC_BYTE_LENGTH)
   {
      if ((nOffset + nLength) > P_NDEF2GEN_STATIC_BYTE_LENGTH)
      {
         /* length in dynamic smart cache */
         pNDEF2GenConnection->nDynamicLength = nOffset + nLength - P_NDEF2GEN_STATIC_BYTE_LENGTH;
         /* length in static smart cache */
         nLength = P_NDEF2GEN_STATIC_BYTE_LENGTH - nOffset;
      }
      else
      {
         /* Only static smart cache is concerned */
         pNDEF2GenConnection->nDynamicLength = 0;
      }

      /* Invalidate static smart cache */
      PSmartCacheInvalidateCache(pContext, &pNDEF2GenConnection->sSmartCacheStatic, nOffset, nLength);

      /* offset in dynamic smart cache */
      nOffset = 0;
   }
   else
   {
      /* length in dynamic smart cache */
      pNDEF2GenConnection->nDynamicLength = nLength;
      /* offset in dynamic smart cache */
      nOffset -= P_NDEF2GEN_STATIC_BYTE_LENGTH;
   }

   /* Invalidate dynamic smart cache */
   if (pNDEF2GenConnection->nDynamicLength > 0)
   {
      PSmartCacheInvalidateCache(pContext, &pNDEF2GenConnection->sSmartCacheDynamic, nOffset, pNDEF2GenConnection->nDynamicLength);
   }

   return W_SUCCESS;
}

static void static_PNDEF2GenSmartCacheReadCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEF2GenSmartCacheReadCompleted");

   if (W_SUCCESS == nError && pNDEF2GenConnection->nDynamicLength > 0)
   {
      PSmartCacheRead(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheDynamic,
                  0, pNDEF2GenConnection->nDynamicLength,
                  pNDEF2GenConnection->pDynamicBuffer,
                  static_PNDEF2GenSmartCacheReadCompleted,
                  pNDEF2GenConnection);

      pNDEF2GenConnection->nDynamicLength = 0;
   }
   else
   {
      pNDEF2GenConnection->nDynamicLength = 0;
      pNDEF2GenConnection->pDynamicBuffer = null;
      static_PNDEF2GenSendResult(pContext, pNDEF2GenConnection, nError);
   }
}

static void static_PNDEF2GenSmartCacheWriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEF2GenSmartCacheWriteCompleted");

   if (W_SUCCESS == nError && pNDEF2GenConnection->nDynamicLength > 0)
   {
      PSmartCacheWrite(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheDynamic,
                  0, pNDEF2GenConnection->nDynamicLength,
                  pNDEF2GenConnection->pDynamicBuffer,
                  static_PNDEF2GenSmartCacheWriteCompleted,
                  pNDEF2GenConnection);

      pNDEF2GenConnection->nDynamicLength = 0;
   }
   else
   {
      pNDEF2GenConnection->nDynamicLength = 0;
      pNDEF2GenConnection->pDynamicBuffer = null;
      static_PNDEF2GenSendResult(pContext, pNDEF2GenConnection, nError);
   }
}

static void static_PNDEF2GenDirectWriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEF2GenDirectWriteCompleted");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNDEF2GenDirectWriteCompleted: returning %s", PUtilTraceError(nError));
   }

   static_PNDEF2GenSendResult(pContext, pNDEF2GenConnection, nError);
}


/* See Client API Specifications */
void PNDEF2GenRead(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength )
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PNDEF2GenRead");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF2GEN_CONNECTION, (void**)&pNDEF2GenConnection);
   if (pNDEF2GenConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PNDEF2GenRead: Bad handle");
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("PNDEF2GenRead: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nOffset + nLength) > pNDEF2GenConnection->pSectorSize->pMultiply(pNDEF2GenConnection->nSectorNumber) )
   {
      PDebugError("PNDEF2GenRead: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Increment the reference count to keep the connection object alive during the operation.
      The reference count is decreased in static_PNDEF2GenSendResult() when the operation is completed */
   PHandleIncrementReferenceCount(pNDEF2GenConnection);

   /* Store the callback context */
   pNDEF2GenConnection->sCallbackContext = sCallbackContext;

   /* Read */
   pNDEF2GenConnection->nDynamicLength = 0;
   pNDEF2GenConnection->pDynamicBuffer = null;

   if (nOffset < P_NDEF2GEN_STATIC_BYTE_LENGTH)
   {
      if ((nOffset + nLength) > P_NDEF2GEN_STATIC_BYTE_LENGTH)
      {
         pNDEF2GenConnection->nDynamicLength = nOffset + nLength - P_NDEF2GEN_STATIC_BYTE_LENGTH;
         nLength = P_NDEF2GEN_STATIC_BYTE_LENGTH - nOffset;
         pNDEF2GenConnection->pDynamicBuffer = pBuffer + nLength;
      }

      PSmartCacheRead(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheStatic,
                  nOffset, nLength,
                  pBuffer,
                  static_PNDEF2GenSmartCacheReadCompleted,
                  pNDEF2GenConnection);
   }
   else
   {
      PSmartCacheRead(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheDynamic,
                  nOffset - P_NDEF2GEN_STATIC_BYTE_LENGTH, nLength,
                  pBuffer,
                  static_PNDEF2GenSmartCacheReadCompleted,
                  pNDEF2GenConnection);
   }

   return;

return_error:
   PDebugError("PNDEF2GenRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See Client API Specifications */
void PNDEF2GenWrite(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength )
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PNDEF2GenWrite");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF2GEN_CONNECTION, (void**)&pNDEF2GenConnection);
   if (pNDEF2GenConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PNDEF2GenWrite: Bad handle");
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("PNDEF2GenWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the parameters */
   if ( (nOffset + nLength) > pNDEF2GenConnection->pSectorSize->pMultiply(pNDEF2GenConnection->nSectorNumber) )
   {
      PDebugError("PNDEF2GenWrite: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Increment the reference count to keep the connection object alive during the operation.
      The reference count is decreased in static_PNDEF2GenSendResult() when the operation is completed */
   PHandleIncrementReferenceCount(pNDEF2GenConnection);

   /* store the callback context */
   pNDEF2GenConnection->sCallbackContext = sCallbackContext;

   /* Write */
   pNDEF2GenConnection->nDynamicLength = 0;
   pNDEF2GenConnection->pDynamicBuffer = null;

   if (nOffset < P_NDEF2GEN_STATIC_BYTE_LENGTH)
   {
      if ((nOffset + nLength) > P_NDEF2GEN_STATIC_BYTE_LENGTH)
      {
         pNDEF2GenConnection->nDynamicLength = nOffset + nLength - P_NDEF2GEN_STATIC_BYTE_LENGTH;
         nLength = P_NDEF2GEN_STATIC_BYTE_LENGTH - nOffset;
         pNDEF2GenConnection->pDynamicBuffer = (uint8_t*)pBuffer + nLength;
      }

      PSmartCacheWrite(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheStatic,
                  nOffset, nLength,
                  pBuffer,
                  static_PNDEF2GenSmartCacheWriteCompleted,
                  pNDEF2GenConnection);

      if(PHandleCheckProperty(pContext, hConnection, W_PROP_MY_D_NFC) == W_SUCCESS)
      {
         PMyDNFCInvalidateSmartCacheNDEF(
                              pContext,
                              hConnection,
                              g_sSectorSize4.pMultiply(P_NDEF2GEN_SECTOR_SIZE * pNDEF2GenConnection->nCurrentSector) + nOffset,
                              nLength);
      }

   }
   else
   {
      PSmartCacheWrite(
                  pContext,
                  &pNDEF2GenConnection->sSmartCacheDynamic,
                  nOffset - P_NDEF2GEN_STATIC_BYTE_LENGTH, nLength,
                  pBuffer,
                  static_PNDEF2GenSmartCacheWriteCompleted,
                  pNDEF2GenConnection);

      if(PHandleCheckProperty(pContext, hConnection, W_PROP_MY_D_NFC) == W_SUCCESS)
      {
         PMyDNFCInvalidateSmartCacheNDEF(
                              pContext,
                              hConnection,
                              g_sSectorSize4.pMultiply(P_NDEF2GEN_SECTOR_SIZE * pNDEF2GenConnection->nCurrentSector) + nOffset,
                              nLength);
      }
   }

   return;

return_error:
   PDebugError("PNDEF2GenWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* Write directly without using a smart cache since
   smart cache performs a read prior a write and some data are not readable */
void PNDEF2GenDirectWrite(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nSectorOffset )
{
   tNDEF2GenConnection* pNDEF2GenConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PNDEF2GenDirectWrite");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF2GEN_CONNECTION, (void**)&pNDEF2GenConnection);
   if (pNDEF2GenConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PNDEF2GenDirectWrite: Bad handle");
      goto return_error;
   }

   /* Check the parameters */
   if (null == pBuffer)
   {
      PDebugError("PNDEF2GenDirectWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the parameters */
   if ( (nSectorOffset + P_NDEF2GEN_WRITE_BLOCK_ALIGN) > pNDEF2GenConnection->pSectorSize->pMultiply(pNDEF2GenConnection->nSectorNumber) )
   {
      PDebugError("PNDEF2GenDirectWrite: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Increment the reference count to keep the connection object alive during the operation.
      The reference count is decreased in static_PNDEF2GenSendResult() when the operation is completed */
   PHandleIncrementReferenceCount(pNDEF2GenConnection);

   /* Save the callback context */
   pNDEF2GenConnection->sCallbackContext = sCallbackContext;

   static_PNDEF2GenWrite(
               pContext,
               pNDEF2GenConnection,
               nSectorOffset,
               P_NDEF2GEN_WRITE_BLOCK_ALIGN,
               pBuffer,
               static_PNDEF2GenDirectWriteCompleted,
               pNDEF2GenConnection);

   return;

return_error:
   PDebugError("PNDEF2GenDirectWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* Polling command's callback */
static void static_PNDEF2GenPollCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      uint32_t nLength,
      W_ERROR nError)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection *) pCallbackParameter;

   PDebugTrace("static_PNDEF2GenPollCompleted");

   /* Send the error */
   PDFCPostContext2(&pNDEF2GenConnection->sCallbackContext, nError);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, pNDEF2GenConnection);
}

/* Send polling command */
static void static_PNDEF2GenPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tNDEF2GenConnection* pNDEF2GenConnection = (tNDEF2GenConnection *) pObject;

   PDebugTrace("static_PNDEF2GenPoll");

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PNDEF2GenPollCompleted callback  */
   PHandleIncrementReferenceCount(pNDEF2GenConnection);

   /* store the callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pNDEF2GenConnection->sCallbackContext);

   /* Send the command */
   /* Copy the block number */
   pNDEF2GenConnection->aReaderToCardBuffer[0] = 0x30;
   pNDEF2GenConnection->aReaderToCardBuffer[1] = 0x00;

   /* We used the ReaderToCardBuffer to avoid a useless allocation.
      We don't use the read data */
   pNDEF2GenConnection->pBuffer = pNDEF2GenConnection->aReaderToCardBuffer;

   /* Send the command */
   P14P3UserExchangeData(
      pContext,
      pNDEF2GenConnection->hConnection,
      static_PNDEF2GenPollCompleted, pNDEF2GenConnection,
      pNDEF2GenConnection->aReaderToCardBuffer, 0x02,
      pNDEF2GenConnection->pBuffer, pNDEF2GenConnection->pSectorSize->pMultiply(P_NDEF2GEN_READ_BLOCK_NUMBER),
      W_NULL_HANDLE,
      W_TRUE, W_FALSE);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
